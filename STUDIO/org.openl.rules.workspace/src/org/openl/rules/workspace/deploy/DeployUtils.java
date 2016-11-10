package org.openl.rules.workspace.deploy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;

public final class DeployUtils {
    public static final String SEPARATOR = "#";

    public static final String DEPLOY_PATH = "deploy/";

    private DeployUtils() {
    }

    public static Collection<Deployment> getLastDeploymentProjects(Repository repository) throws RRepositoryException {

        Map<String, Deployment> latestDeployments = new HashMap<String, Deployment>();
        Map<String, Integer> versionsList = new HashMap<String, Integer>();

        Collection<FileData> fileDatas = repository.list(DEPLOY_PATH);
        for (FileData fileData : fileDatas) {
            String deploymentFolderName = fileData.getName().substring(DEPLOY_PATH.length()).split("/")[0];
            int separatorPosition = deploymentFolderName.lastIndexOf(SEPARATOR);

            String deploymentName = deploymentFolderName;
            Integer version = 0;
            CommonVersionImpl commonVersion = null;
            if (separatorPosition >= 0) {
                deploymentName = deploymentFolderName.substring(0, separatorPosition);
                version = Integer.valueOf(deploymentFolderName.substring(separatorPosition + 1));
                commonVersion = new CommonVersionImpl(version);
            }
            Integer previous = versionsList.put(deploymentName, version);
            if (previous != null && previous > version) {
                // rollback
                versionsList.put(deploymentName, previous);
            } else {
                // put the latest deployment
                Deployment deployment = new Deployment(repository,
                    DEPLOY_PATH + deploymentFolderName,
                    deploymentName,
                    commonVersion);
                latestDeployments.put(deploymentName, deployment);
            }
        }

        return latestDeployments.values();
    }

    public static int getNextDeploymentVersion(Repository repository,
            ADeploymentProject project) throws RRepositoryException {
        Collection<Deployment> lastDeploymentProjects = getLastDeploymentProjects(repository);
        int version = 0;
        String prefix = project.getName() + SEPARATOR;
        for (Deployment deployment : lastDeploymentProjects) {
            if (deployment.getName().startsWith(prefix)) {
                version = Integer.valueOf(deployment.getCommonVersion().getRevision()) + 1;
                break;
            }
        }
        return version;
    }

    public static Collection<Deployment> getDeployments(Repository repository) {
        Collection<FileData> fileDatas = repository.list(DEPLOY_PATH);
        ConcurrentMap<String, Deployment> deployments = new ConcurrentHashMap<String, Deployment>();
        for (FileData fileData : fileDatas) {
            String deploymentFolderName = fileData.getName().substring(DEPLOY_PATH.length()).split("/")[0];
            int separatorPosition = deploymentFolderName.lastIndexOf(SEPARATOR);

            String deploymentName = deploymentFolderName;
            CommonVersionImpl commonVersion = null;
            if (separatorPosition >= 0) {
                deploymentName = deploymentFolderName.substring(0, separatorPosition);
                int version = Integer.valueOf(deploymentFolderName.substring(separatorPosition + 1));
                commonVersion = new CommonVersionImpl(version);
            }
            Deployment deployment = new Deployment(repository,
                DEPLOY_PATH + deploymentFolderName,
                deploymentName,
                commonVersion);
            deployments.putIfAbsent(deploymentFolderName, deployment);
        }

        return deployments.values();
    }

    public static Deployment getDeployment(Repository repository,
            String deploymentName,
            CommonVersion deploymentVersion) {
        String name = deploymentName + SEPARATOR + deploymentVersion.getVersionName();
        return new Deployment(repository, DEPLOY_PATH + name, deploymentName, deploymentVersion);
    }
}
