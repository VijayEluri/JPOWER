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

package edu.cornell.pserc.jpower.tdouble.test;

import static edu.cornell.pserc.jpower.tdouble.test.TestGlobals.t_quiet;
import static edu.cornell.pserc.jpower.tdouble.test.TestGlobals.t_num_of_tests;
import static edu.cornell.pserc.jpower.tdouble.test.TestGlobals.t_counter;
import static edu.cornell.pserc.jpower.tdouble.test.TestGlobals.t_ok_cnt;
import static edu.cornell.pserc.jpower.tdouble.test.TestGlobals.t_not_ok_cnt;
import static edu.cornell.pserc.jpower.tdouble.test.TestGlobals.t_skip_cnt;
import static edu.cornell.pserc.jpower.tdouble.test.TestGlobals.t_clock;

/**
 * Begin running tests.
 *
 * @author Ray Zimmerman
 * @author Richard Lincoln
 *
 */
public class Djp_t_begin {

	public static void t_begin(int num_of_tests) {
		t_begin(num_of_tests, false);
	}

	/**
	 * Initializes the global test counters,
	 * setting everything up to execute NUM_OF_TESTS tests using T_OK
	 * and T_IS. If QUIET is true, it will not print anything for the
	 * individual tests, only a summary when T_END is called.
	 *
	 * @param quiet
	 */
	public static void t_begin(int num_of_tests, boolean quiet) {
		t_quiet = quiet;
		t_num_of_tests = num_of_tests;
		t_counter = 1;
		t_ok_cnt = 0;
		t_not_ok_cnt = 0;
		t_skip_cnt = 0;
		t_clock = System.currentTimeMillis();

		if (t_quiet == false)
			System.out.printf("1..%d\n", num_of_tests);
	}

}
