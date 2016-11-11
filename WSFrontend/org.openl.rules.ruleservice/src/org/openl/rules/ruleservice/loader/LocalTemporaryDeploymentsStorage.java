package org.openl.rules.ruleservice.loader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.Deployment;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.ruleservice.core.RuleServiceRuntimeException;
import org.openl.rules.workspace.lw.impl.FolderHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Local temporary file system storage for deployments. Clears all data on first
 * initialization. Thread safe implementation.
 *
 * @author Marat Kamalov
 */
public class LocalTemporaryDeploymentsStorage {

    private final Logger log = LoggerFactory.getLogger(LocalTemporaryDeploymentsStorage.class);

    private String directoryToLoadDeploymentsIn;

    private Map<String, Deployment> cacheForGetDeployment = new HashMap<String, Deployment>();
    private Repository repository;

    /**
     * Construct a new LocalTemporaryDeploymentsStorage for bean usage.
     */
    public LocalTemporaryDeploymentsStorage(String directoryToLoadDeploymentsIn) {
        if (directoryToLoadDeploymentsIn == null) {
            throw new IllegalArgumentException("directoryToLoadDeploymentsIn argument can't be null");
        }
        this.directoryToLoadDeploymentsIn = directoryToLoadDeploymentsIn;
        File folderToLoadDeploymentsIn = new File(directoryToLoadDeploymentsIn);
        folderToLoadDeploymentsIn.mkdirs();
        if (!FolderHelper.clearFolder(folderToLoadDeploymentsIn)) {
            log.error("Failed to clear a folder \"{}\"!", folderToLoadDeploymentsIn.getAbsolutePath());
        } else {
            log.info("Local temporary folder for downloading deployments was cleared.");
        }
        log.info("Local temporary folder location is: {}", directoryToLoadDeploymentsIn);
        repository = new LocalRepository(folderToLoadDeploymentsIn);
    }

    /**
     * Gets path to folder to load deployments in directory. Extract the value
     * with key <i>ruleservice.deployment.storage.dir</i> from configuration
     * file. If such a key is missing returns default value
     * <tt>/tmp/rules-deploy</tt>.
     *
     * @return path to load in directory
     */
    public String getDirectoryToLoadDeploymentsIn() {
        return directoryToLoadDeploymentsIn;
    }

    /**
     * Sets a path to local temporary storage. Spring bean configuration
     * property.
     *
     * @param directoryToLoadDeploymentsIn
     */
    /**
     * Generates folder name for deployment by given deployment name and common
     * version.
     *
     * @return folder name
     */
    private String getDeploymentFolderName(String deploymentName, CommonVersion version) {
        return new StringBuilder(deploymentName).append("_v").append(version.getVersionName()).toString();
    }

    /**
     * Gets deployment from storage. If deployment doesn't exists in storage
     * returns null.
     *
     * @return deployment from storage or null if doens't exists
     */
    Deployment getDeployment(String deploymentName, CommonVersion version) {
        log.debug("Getting deployment with name=\"{}\" and version=\"{}\"", deploymentName, version.getVersionName());
        String deploymentFolderName = getDeploymentFolderName(deploymentName, version);
        Deployment deployment = cacheForGetDeployment.get(deploymentFolderName);
        return deployment;
    }

    /**
     * Loads deployment to local file system from repository.
     *
     * @param deployment
     * @return loaded deployment
     */
    Deployment loadDeployment(Deployment deployment) {
        if (deployment == null) {
            throw new IllegalArgumentException("deployment argument can't be null");
        }

        String deploymentName = deployment.getDeploymentName();
        CommonVersion version = deployment.getCommonVersion();
        String versionName = deployment.getVersion().getVersionName();
        log.debug("Loading deployement with name=\"{}\" and version=\"{}\"", deploymentName, versionName);

        String deploymentFolderName = getDeploymentFolderName(deploymentName, version);
        Deployment loadedDeployment = new Deployment(repository, deploymentFolderName, deploymentName, version);
        try {
            loadedDeployment.update(deployment, null);
            loadedDeployment.refresh();
        } catch (ProjectException e) {
            log.warn("Exception occurs on loading deployment with name=\"{}\" and version=\"{}\" from data source",
                deploymentName,
                versionName,
                e);
            throw new RuleServiceRuntimeException(e);
        }

        cacheForGetDeployment.put(deploymentFolderName, loadedDeployment);

        log.debug("Deployment with name=\"{}\" and version=\"{}\" has been made on local storage and putted to cache.",
            deploymentName,
            versionName);
        return loadedDeployment;
    }

    /**
     * Check to existing deployment in local temporary folder.
     *
     * @return true if and only if the deployment exists; false otherwise
     */
    boolean containsDeployment(String deploymentName, CommonVersion version) {
        return cacheForGetDeployment.containsKey(getDeploymentFolderName(deploymentName, version));
    }
}
