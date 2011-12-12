/*******************************************************************************
 *  Copyright (c) 2007, 2011 IBM Corporation and others.
 *  All rights reserved. This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License v1.0
 *  which accompanies this distribution, and is available at
 *  http://www.eclipse.org/legal/epl-v10.html
 * 
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.full;

import junit.framework.*;

/**
 * Performs all automated full end-to-end install/update/rollback tests.
 */
public class AllTests extends TestCase {

	public static Test suite() {
		TestSuite suite = new TestSuite(AllTests.class.getName());

		suite.addTestSuite(End2EndTest35.class);
		suite.addTestSuite(End2EndTest36.class);
		suite.addTestSuite(End2EndTest37.class);

		//suite.addTest(From35to36.suite());
		suite.addTest(From36to37.suite());
		//suite.addTest(From37to38.suite());

		//suite.addTest(Install36from35.suite());
		suite.addTest(Install37from36.suite());
		//suite.addTest(Install38from37.suite());
		return suite;
	}

}
