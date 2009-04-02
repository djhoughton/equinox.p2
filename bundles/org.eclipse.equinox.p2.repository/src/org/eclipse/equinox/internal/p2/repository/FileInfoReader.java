/*******************************************************************************
 * Copyright (c) 2009, IBM Corporation, and others.
 * The code, documentation and other materials contained herein have been
 * licensed under the Eclipse Public License - v 1.0 by the copyright holder
 * listed above, as the Initial Contributor under such license. The text of
 * such license is available at www.eclipse.org.
 * Contributors:
 * 	IBM Corporation - initial implementation
 * 	Cloudsmith Inc - modified API, and implementation
 ******************************************************************************/
package org.eclipse.equinox.internal.p2.repository;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.ecf.core.*;
import org.eclipse.ecf.core.security.IConnectContext;
import org.eclipse.ecf.filetransfer.*;
import org.eclipse.ecf.filetransfer.events.IRemoteFileSystemBrowseEvent;
import org.eclipse.ecf.filetransfer.events.IRemoteFileSystemEvent;
import org.eclipse.ecf.filetransfer.identity.*;
import org.eclipse.equinox.internal.p2.core.helpers.LogHelper;
import org.eclipse.osgi.util.NLS;

/**
 * The FileInfoReader is a {@link Job} similar to {@link FileReader}, but without the support
 * from ECF (there is currently no way to wait on a BrowseRequest job, as this is internal to
 * ECF). If such support is added, this class is easily modified.
 * 
 * @author henrik.lindberg@cloudsmith.com 
 */
public class FileInfoReader extends Job implements IRemoteFileSystemListener {
	private Exception exception;
	private IProgressMonitor theMonitor;
	private final int connectionRetryCount;
	private final long connectionRetryDelay;
	private final IConnectContext connectContext;
	final Boolean[] barrier = new Boolean[1];
	private IRemoteFile[] remoteFiles;

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	protected IStatus run(IProgressMonitor monitor) {
		synchronized (barrier) {
			while (barrier[0] == null) {
				try {
					barrier.wait();
				} catch (InterruptedException e) {
					//ignore
				}
			}
		}
		return Status.OK_STATUS;
	}

	/**
	 * Waits until request is processed (barrier[0] is non null).
	 * This is a bit of a hack, as it would be better if the ECFBrowser worked in similar fashion to
	 * file transfer were a custom job can be supplied.
	 * TODO: log an issue for ECF.
	 */
	private void waitOnSelf() {
		schedule();
		while (barrier[0] == null) {
			boolean logged = false;
			try {
				join();
			} catch (InterruptedException e) {
				if (!logged)
					LogHelper.log(new Status(IStatus.WARNING, Activator.ID, "Unexpected interrupt while waiting on ECF browse request", e)); //$NON-NLS-1$
			}
		}
	}

	/**
	 * Create a new FileInfoReader that will retry failed connection attempts and sleep some amount of time between each
	 * attempt.
	 */
	public FileInfoReader(IConnectContext aConnectContext) {
		super(Messages.repo_loading); // job label - TODO: this is a bad label
		barrier[0] = null;
		// Hide this job.
		setSystem(true);
		setUser(false);
		connectionRetryCount = RepositoryPreferences.getConnectionRetryCount();
		connectionRetryDelay = RepositoryPreferences.getConnectionMsRetryDelay();
		connectContext = aConnectContext;
	}

	/**
	 * Get the requested information.
	 * @return IRemoteFile[] or null if there was an error.
	 * @throws CoreException 
	 * @throws FileNotFoundException 
	 * @throws AuthenticationFailedException 
	 */
	public IRemoteFile[] getRemoteFiles(URI location, IProgressMonitor monitor) throws AuthenticationFailedException, FileNotFoundException, CoreException {
		if (monitor != null)
			monitor.beginTask(location.toString(), 1);
		try {
			sendBrowseRequest(location, monitor);
			waitOnSelf();
			return remoteFiles;
		} finally {
			if (monitor != null) {
				monitor.done();
			}
		}

	}

	public IRemoteFile getRemoteFile(URI location, IProgressMonitor monitor) throws AuthenticationFailedException, FileNotFoundException, CoreException {

		getRemoteFiles(location, monitor);
		return remoteFiles != null && remoteFiles.length > 0 ? remoteFiles[0] : null;
	}

	public long getLastModified(URI location, IProgressMonitor monitor) throws AuthenticationFailedException, FileNotFoundException, CoreException {
		IRemoteFile file = getRemoteFile(location, monitor);
		if (file == null)
			throw new FileNotFoundException(location.toString());
		return file.getInfo().getLastModified();
	}

	public void handleRemoteFileEvent(IRemoteFileSystemEvent event) {
		exception = event.getException();
		if (exception != null) {
			synchronized (barrier) {
				barrier[0] = Boolean.TRUE;
				barrier.notify();
			}
		} else if (event instanceof IRemoteFileSystemBrowseEvent) {
			IRemoteFileSystemBrowseEvent fsbe = (IRemoteFileSystemBrowseEvent) event;
			remoteFiles = fsbe.getRemoteFiles();
			if (theMonitor != null)
				theMonitor.worked(1);
			synchronized (barrier) {
				barrier[0] = Boolean.TRUE;
				barrier.notify();
			}
		} else {
			synchronized (barrier) {
				barrier[0] = Boolean.FALSE; // ended by unknown reason
				barrier.notify();
			}
		}
	}

	protected void sendBrowseRequest(URI uri, IProgressMonitor monitor) throws CoreException, FileNotFoundException, AuthenticationFailedException {
		IContainer container;
		try {
			container = ContainerFactory.getDefault().createContainer();
		} catch (ContainerCreateException e) {
			throw RepositoryStatusHelper.fromMessage(Messages.ecf_configuration_error);
		}

		IRemoteFileSystemBrowserContainerAdapter adapter = (IRemoteFileSystemBrowserContainerAdapter) container.getAdapter(IRemoteFileSystemBrowserContainerAdapter.class);
		if (adapter == null) {
			throw RepositoryStatusHelper.fromMessage(Messages.ecf_configuration_error);
		}

		adapter.setConnectContextForAuthentication(connectContext);

		this.exception = null;
		this.theMonitor = monitor;
		for (int retryCount = 0;;) {
			if (monitor != null && monitor.isCanceled())
				throw new OperationCanceledException();

			try {
				IFileID fileID = FileIDFactory.getDefault().createFileID(adapter.getBrowseNamespace(), uri.toString());
				adapter.sendBrowseRequest(fileID, this);
			} catch (RemoteFileSystemException e) {
				exception = e;
			} catch (FileCreateException e) {
				exception = e;
			}

			// note that 'exception' could have been captured in a callback
			if (exception != null) {
				// if this is a authentication failure - it is not meaningful to continue
				RepositoryStatusHelper.checkPermissionDenied(exception);

				Throwable t = RepositoryStatusHelper.unwind(exception);
				if (t instanceof CoreException)
					throw RepositoryStatusHelper.unwindCoreException((CoreException) t);

				if (t instanceof FileNotFoundException)
					//
					// Connection succeeded but the target doesn't exist
					//
					throw (FileNotFoundException) t;

				if (t instanceof IOException && retryCount < connectionRetryCount) {
					// TODO: Retry only certain exceptions or filter out
					// some exceptions not worth retrying
					//
					++retryCount;
					exception = null;
					try {
						LogHelper.log(new Status(IStatus.WARNING, Activator.ID, NLS.bind(Messages.connection_to_0_failed_on_1_retry_attempt_2, new String[] {uri.toString(), t.getMessage(), String.valueOf(retryCount)}), t));

						Thread.sleep(connectionRetryDelay);
						continue;
					} catch (InterruptedException e) {
						/* ignore */
					}
				}
				throw RepositoryStatusHelper.wrap(exception);
			}
			break;
		}
	}

	protected Exception getException() {
		return exception;
	}

}