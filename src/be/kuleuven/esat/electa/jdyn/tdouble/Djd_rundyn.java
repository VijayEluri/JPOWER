/*
 * Copyright (C) 2009 Stijn Cole
 * Copyright (C) 2010-2011 Richard Lincoln
 *
 * JPOWER is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published
 * by the Free Software Foundation, either version 3 of the License,
 * or (at your option) any later version.
 *
 * JPOWER is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with JPOWER. If not, see <http://www.gnu.org/licenses/>.
 *
 */

package be.kuleuven.esat.electa.jdyn.tdouble;

import java.util.Map;

import be.kuleuven.esat.electa.jdyn.tdouble.jdc.Djd_buschange;
import be.kuleuven.esat.electa.jdyn.tdouble.jdc.Djd_event;
import be.kuleuven.esat.electa.jdyn.tdouble.jdc.Djd_exc;
import be.kuleuven.esat.electa.jdyn.tdouble.jdc.Djd_gen;
import be.kuleuven.esat.electa.jdyn.tdouble.jdc.Djd_gov;
import be.kuleuven.esat.electa.jdyn.tdouble.jdc.Djd_linechange;
import be.kuleuven.esat.electa.jdyn.tdouble.models.exciters.Djp_ExciterInit;
import be.kuleuven.esat.electa.jdyn.tdouble.models.generators.Djd_GeneratorInit;
import be.kuleuven.esat.electa.jdyn.tdouble.models.governors.Djd_GovernorInit;

import cern.colt.matrix.tdcomplex.DComplexMatrix1D;
import cern.colt.matrix.tdcomplex.algo.decomposition.SparseDComplexLUDecomposition;
import cern.colt.matrix.tdouble.DoubleFactory1D;
import cern.colt.matrix.tdouble.DoubleMatrix1D;
import cern.colt.matrix.tdouble.DoubleMatrix2D;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.util.tdouble.Djp_util;
import cern.jet.math.tdcomplex.DComplexFunctions;
import cern.jet.math.tdouble.DoubleFunctions;
import cern.jet.math.tint.IntFunctions;
import edu.cornell.pserc.jpower.tdouble.Djp_jpoption;
import edu.cornell.pserc.jpower.tdouble.jpc.Djp_branch;
import edu.cornell.pserc.jpower.tdouble.jpc.Djp_bus;
import edu.cornell.pserc.jpower.tdouble.jpc.Djp_gen;
import edu.cornell.pserc.jpower.tdouble.jpc.Djp_jpc;
import edu.cornell.pserc.jpower.tdouble.pf.Djp_runpf;

/**
 * Runs dynamic simulation.
 *
 * @author Stijn Cole
 * @author Richard Lincoln
 *
 */
public class Djd_rundyn {

	private static final IntFunctions ifunc = IntFunctions.intFunctions;
	private static final DoubleFunctions dfunc = DoubleFunctions.functions;
	private static final DComplexFunctions cfunc = DComplexFunctions.functions;

	public static Object[] jd_rundyn(String casefile_pf, String casefile_dyn, String casefile_ev) {
		return jd_rundyn(casefile_pf, casefile_dyn, casefile_ev, Djd_Jdoption.jd_Jdoption());
	}

	public static Object[] jd_rundyn(String casefile_pf, String casefile_dyn, String casefile_ev, DoubleMatrix1D jdopt) {

		/* Begin timing */
		long t0 = System.currentTimeMillis();

		/* Options */
		int method = (int) jdopt.get(0);
		double tol = jdopt.get(1);
		double minstepsize = jdopt.get(2);
		double maxstepsize = jdopt.get(3);
		boolean output = jdopt.get(4) == 0.0 ? false : true;
		boolean plots = jdopt.get(5) == 0.0 ? false : true;

		/* Load all data */
		if (output)
			System.out.println("Loading dynamic simulation data...");

		// Load dynamic simulation data
		double[] dyn = Djd_Loaddyn.jd_Loaddyn(casefile_dyn);
		double freq = dyn[0], stepsize = dyn[1], stoptime = dyn[2];

		// Load generator data
		Djd_gen Pgen0 = Djd_Loadgen.jd_Loadgen(casefile_dyn, output);

		// Load exciter data
		Djd_exc Pexc0 = Djd_Loadexc.jd_Loadexc(casefile_dyn);

		// Load governor data
		Djd_gov Pgov0 = Djd_Loadgov.jd_Loadgov(casefile_dyn);

		// Load event data
		Djd_event event = null;
		Djd_buschange buschange = null;
		Djd_linechange linechange = null;
		if (casefile_ev != "") {
			Object[] events = Djd_Loadevents.jd_Loadevents(casefile_ev);
			event = (Djd_event) events[0];
			buschange = (Djd_buschange) events[1];
			linechange = (Djd_linechange) events[2];
		}

		IntMatrix1D genmodel = Pgen0.genmodel;
		IntMatrix1D excmodel = Pgen0.excmodel;
		IntMatrix1D govmodel = Pgen0.govmodel;

		/* Initialization: Power Flow */

		// Power flow options
		Map<String, Double> jpopt = Djp_jpoption.jp_jpoption();
		jpopt.put("VERBOSE", 0.0);
		jpopt.put("OUT_ALL", 0.0);

		// Run power flow
		Djp_jpc jpc = Djp_runpf.jp_runpf(casefile_pf, jpopt);
		double baseMVA = jpc.baseMVA;
		Djp_bus bus = jpc.bus;
		Djp_gen gen = jpc.gen;
		Djp_branch branch = jpc.branch;

		if (!jpc.success) {
			System.err.println("Power flow did not converge. Exiting...");
		} else {
			if (output)
				System.out.println("Power flow converged");
		}

		DComplexMatrix1D U0 = Djp_util.polar(bus.Vm, bus.Va, false);
		DComplexMatrix1D U00 = U0.copy();
		// Get generator info
		int[] on = Djp_util.nonzero( gen.gen_status.copy().assign(ifunc.equals(1)) );	// which generators are on?
		int[] gbus = gen.gen_bus.toArray();		// what buses are they at?
		int ngen = gbus.length;
		int nbus = (int) U0.size();

		/* Construct augmented Ybus */
		if (output)
			System.out.println("Constructing augmented admittance matrix...");
		DoubleMatrix1D Pl = bus.Pd.copy().assign(dfunc.div(baseMVA));	// load power
		DoubleMatrix1D Ql = bus.Qd.copy().assign(dfunc.div(baseMVA));

		int[] type1 = genmodel.copy().assign(ifunc.equals(1)).toArray();
		int[] type2 = genmodel.copy().assign(ifunc.equals(2)).toArray();

		DoubleMatrix1D xd_tr = DoubleFactory1D.dense.make(ngen);
		xd_tr.viewSelection(type2).assign(Pgen0.xd_tr.viewSelection(type2));	// 4th order model: xd_tr column 7
		xd_tr.viewSelection(type1).assign(Pgen0.xp.viewSelection(type1));		// classical model: xd_tr column 6

		SparseDComplexLUDecomposition invYbus = Djd_AugYbus.jd_AugYbus(baseMVA, bus, branch, xd_tr, gbus, Pl, Ql, U0);

		/* Calculate Initial machine state */
		if (output)
			System.out.println("Calculating initial state...");

		Djd_gen[] gen0 = Djd_GeneratorInit.jd_GeneratorInit(Pgen0, U0.viewSelection(gbus), gen, baseMVA, genmodel);
		Djd_gen Efd0 = gen0[0], Xgen0 = gen0[1];

//		DoubleMatrix1D omega0 = Xgen0.viewColumn(1);

		DoubleMatrix1D[] I = Djd_MachineCurrents.jd_MachineCurrents(Xgen0, Pgen0, U0.viewSelection(gbus), genmodel);
		DoubleMatrix1D Id0 = I[0], Iq0 = I[1], Pe0 = I[2];

		/* Exciter initial conditions */
		DoubleMatrix1D Vexc0 = U0.viewSelection(gbus).assign(cfunc.abs).getRealPart();
		Djd_exc[] exe0 = Djp_ExciterInit.jp_ExciterInit(Efd0, Pexc0, Vexc0, excmodel);
		Djd_exc Xexc0 = exe0[0];
		Pexc0 = exe0[1];

		/* Governor initial conditions */
		DoubleMatrix1D Pm0 = Pe0.copy();
//		Djd_gov[] gov0 = Djd_GovernorInit.jd_GovernorInit(Pm0, Pgov0, omega0, govmodel);
//		Djd_gov Xgov0 = gov0[0];
//		Pgov0 = gov0[1];

		/* Check Steady-state */


		return null;
	}
}