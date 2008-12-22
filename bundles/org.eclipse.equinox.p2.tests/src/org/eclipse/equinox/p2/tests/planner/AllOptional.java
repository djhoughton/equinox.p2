/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others. All rights reserved. This
 * program and the accompanying materials are made available under the terms of
 * the Eclipse Public License v1.0 which accompanies this distribution, and is
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors: IBM Corporation - initial API and implementation
 ******************************************************************************/
package org.eclipse.equinox.p2.tests.planner;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.equinox.internal.provisional.p2.director.*;
import org.eclipse.equinox.internal.provisional.p2.engine.IProfile;
import org.eclipse.equinox.internal.provisional.p2.metadata.*;
import org.eclipse.equinox.p2.tests.AbstractProvisioningTest;
import org.eclipse.equinox.internal.provisional.p2.core.VersionRange;
import org.eclipse.equinox.internal.provisional.p2.core.Version;

public class AllOptional extends AbstractProvisioningTest {
	private IInstallableUnit a1;
	private IInstallableUnit b1;
	private IInstallableUnit c1;
	private IInstallableUnit d1;
	private IInstallableUnit e1;
	private IProfile profile;
	private IPlanner planner;

	protected void setUp() throws Exception {
		super.setUp();
		RequiredCapability[] reqA = new RequiredCapability[] {MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "B", VersionRange.emptyRange, null, false, false, true)};
		a1 = createIU("A", new Version("1.0.0"), reqA);
		b1 = createIU("B", new Version("1.0.0"), true);

		RequiredCapability[] reqC = new RequiredCapability[2];
		reqC[0] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "D", VersionRange.emptyRange, null, true, false, true);
		reqC[1] = MetadataFactory.createRequiredCapability(IInstallableUnit.NAMESPACE_IU_ID, "E", VersionRange.emptyRange, null, true, false, true);
		c1 = createIU("C", new Version("1.0.0"), reqC);
		d1 = createIU("D", new Version("1.0.0"), true);
		e1 = createIU("E", new Version("1.0.0"), true);

		createTestMetdataRepository(new IInstallableUnit[] {a1, b1, c1, d1, e1});

		profile = createProfile("TestProfile." + getName());
		planner = createPlanner();
	}

	public void testInstallation() {
		ProfileChangeRequest req = new ProfileChangeRequest(profile);
		req.addInstallableUnits(new IInstallableUnit[] {a1, c1});
		ProvisioningPlan plan = planner.getProvisioningPlan(req, null, null);
		assertEquals(IStatus.OK, plan.getStatus().getSeverity());
		assertInstallOperand(plan, a1);
		assertInstallOperand(plan, b1);
		assertInstallOperand(plan, c1);
		assertInstallOperand(plan, d1);
		assertInstallOperand(plan, e1);
	}
}
