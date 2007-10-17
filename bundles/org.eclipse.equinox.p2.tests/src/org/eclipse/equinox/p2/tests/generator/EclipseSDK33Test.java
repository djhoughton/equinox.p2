/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.equinox.p2.tests.generator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Path;
import org.eclipse.equinox.p2.metadata.IInstallableUnit;
import org.eclipse.equinox.p2.metadata.ProvidedCapability;
import org.eclipse.equinox.p2.metadata.generator.Generator;
import org.eclipse.equinox.p2.metadata.generator.IGeneratorInfo;
import org.eclipse.equinox.p2.tests.*;
import org.osgi.framework.Bundle;

/**
 * Tests running the metadata generator against Eclipse 3.3 features.
 */
public class EclipseSDK33Test extends AbstractProvisioningTest {
	public static Test suite() {
		return new TestSuite(EclipseSDK33Test.class);
	}

	public EclipseSDK33Test() {
		super("");
	}

	public EclipseSDK33Test(String name) {
		super(name);
	}

	public void testGeneration() {
		IGeneratorInfo generatorInfo = createGeneratorInfo();
		Generator generator = new Generator(generatorInfo);
		generator.generate();

		TestMetadataRepository repo = (TestMetadataRepository) generatorInfo.getMetadataRepository();
		IInstallableUnit unit = repo.find("org.eclipse.cvs.source.featureIU", "1.0.0.v20070606-7C79_79EI99g_Y9e");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.rcp.featureIU", "3.3.0.v20070607-8y8eE8NEbsN3X_fjWS8HPNG");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.jdt.featureIU", "3.3.0.v20070606-0010-7o7jCHEFpPoqQYvnXqejeR");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.cvs.featureIU", "1.0.0.v20070606-7C79_79EI99g_Y9e");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.pde.source.featureIU", "3.3.0.v20070607-7N7M-DUUEF6Ez0H46IcCC");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.sdk.featureIU", "3.3.0.v20070607-7M7J-BIolz-OcxWxvWAPSfLPqevO");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.platform.featureIU", "3.3.0.v20070612-_19UEkLEzwsdF9jSqQ-G");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.platform.source.featureIU", "3.3.0.v20070612-_19UEkLEzwsdF9jSqQ-G");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.jdt.source.featureIU", "3.3.0.v20070606-0010-7o7jCHEFpPoqQYvnXqejeR");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.pde.featureIU", "3.3.0.v20070607-7N7M-DUUEF6Ez0H46IcCC");
		assertNotNull(unit);
		assertGroup(unit);
		unit = repo.find("org.eclipse.rcp.source.featureIU", "3.3.0.v20070607-8y8eE8NEbsN3X_fjWS8HPNG");
		assertNotNull(unit);
		assertGroup(unit);

	}

	/**
	 * Asserts that the given IU represents a group.
	 */
	private void assertGroup(IInstallableUnit unit) {
		ProvidedCapability[] capabilities = unit.getProvidedCapabilities();
		for (int i = 0; i < capabilities.length; i++) {
			if (capabilities[i].getNamespace().equals(IInstallableUnit.IU_KIND_NAMESPACE) && capabilities[i].getName().equals("group"))
				return;
		}
		fail("IU is not a group:" + unit);
	}

	private IGeneratorInfo createGeneratorInfo() {
		Bundle myBundle = TestActivator.getContext().getBundle();
		URL root = FileLocator.find(myBundle, new Path("testData/generator/eclipse3.3"), null);
		File rootFile = null;
		try {
			root = FileLocator.toFileURL(root);
			rootFile = new File(root.getPath());
		} catch (IOException e) {
			fail("4.99", e);
		}
		TestGeneratorInfo info = new TestGeneratorInfo(rootFile);
		return info;
	}

}
