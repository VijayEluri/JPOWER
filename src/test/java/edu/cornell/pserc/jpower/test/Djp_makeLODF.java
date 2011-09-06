/*
 * Copyright (C) 1996-2010 Power System Engineering Research Center
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

package edu.cornell.pserc.jpower.test;

import static edu.cornell.pserc.jpower.test.Djp_t_auction_case.t_auction_case;

import static edu.cornell.pserc.jpower.test.Djp_t_begin.t_begin;
import static edu.cornell.pserc.jpower.test.Djp_t_end.t_end;
import static edu.cornell.pserc.jpower.test.Djp_t_is.t_is;
import static edu.cornell.pserc.jpower.test.Djp_t_ok.t_ok;

public class Djp_makeLODF {

	public static void t_makeLODF() {
		t_makeLODF(false);
	}

	public static void t_makeLODF(boolean quiet) {

		t_begin(31, quiet);

		t_end();

	}

}
