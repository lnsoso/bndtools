package org.bndtools.builder.jobs.newproject;

import java.util.ArrayList;
import java.util.List;

import org.bndtools.api.ILogger;
import org.bndtools.api.Logger;
import org.bndtools.builder.classpath.BndContainerInitializer;
import org.bndtools.utils.workspace.WorkspaceUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.resources.WorkspaceJob;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

import aQute.bnd.build.Project;
import bndtools.central.Central;

class AdjustClasspathsForNewProjectJob extends WorkspaceJob {
    private static final ILogger logger = Logger.getLogger(AdjustClasspathsForNewProjectJob.class);

    private final IProject addedProject;

    AdjustClasspathsForNewProjectJob(IProject addedProject) {
        super("Adjusting classpaths for new project: " + addedProject.getName());
        this.addedProject = addedProject;
    }

    @Override
    public IStatus runInWorkspace(IProgressMonitor monitor) {
        List<Project> projects;
        SubMonitor progress;
        try {
            projects = new ArrayList<Project>(Central.getWorkspace().getAllProjects());
            progress = SubMonitor.convert(monitor, projects.size());
        } catch (Exception e) {
            return Status.CANCEL_STATUS;
        }

        IWorkspaceRoot wsroot = ResourcesPlugin.getWorkspace().getRoot();
        while (!projects.isEmpty()) {
            Project project = projects.remove(0);
            IProject eclipseProject = WorkspaceUtils.findOpenProject(wsroot, project);
            if (eclipseProject != null && !eclipseProject.equals(addedProject)) {
                try {
                    project.propertiesChanged();
                    IJavaProject javaProject = JavaCore.create(eclipseProject);
                    if (javaProject != null) {
                        BndContainerInitializer.requestClasspathContainerUpdate(javaProject);
                    }
                } catch (CoreException e) {
                    logger.logStatus(e.getStatus());
                    return Status.CANCEL_STATUS;
                }
                progress.worked(1);
            }
            if (progress.isCanceled())
                return Status.CANCEL_STATUS;
        }
        return Status.OK_STATUS;
    }

}
