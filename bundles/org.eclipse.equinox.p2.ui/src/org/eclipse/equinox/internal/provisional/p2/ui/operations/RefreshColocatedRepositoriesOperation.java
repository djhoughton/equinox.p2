/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************/

package org.eclipse.equinox.internal.provisional.p2.ui.operations;

import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.equinox.internal.provisional.p2.core.ProvisionException;
import org.eclipse.equinox.internal.provisional.p2.ui.ProvUI;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * @since 3.5
 */
public class RefreshColocatedRepositoriesOperation extends RepositoryOperation {

	/**
	 * @param label
	 * @param locations
	 */
	public RefreshColocatedRepositoriesOperation(String label, URI[] locations) {
		super(label, locations);
	}

	public RefreshColocatedRepositoriesOperation(String label, int flags) {
		super(label, new URI[0]);
		try {
			// We use the list of metadata repositories since this is what drives
			// what the user sees, but we will refresh both the artifact and metadata
			// repositories.
			this.locations = ProvisioningUtil.getMetadataRepositories(flags);
		} catch (ProvisionException e) {
			ProvUI.handleException(e, null, StatusManager.LOG);
		}
	}

	protected IStatus doBatchedExecute(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		// Clear the not found cache so that repos not found are reported again.
		ProvUI.clearRepositoriesNotFound();
		ProvisioningUtil.refreshMetadataRepositories(locations, monitor);
		ProvisioningUtil.refreshArtifactRepositories(locations, monitor);
		return Status.OK_STATUS;
	}

	protected IStatus doBatchedUndo(IProgressMonitor monitor, IAdaptable uiInfo) throws ProvisionException {
		// Should never happen
		return Status.CANCEL_STATUS;
	}

	public boolean canUndo() {
		return false;
	}

}
