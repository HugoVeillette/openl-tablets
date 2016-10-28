package org.openl.rules.workspace.deploy;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;

public final class DeployUtils {

    public static final String DEPLOY_PATH = "deploy/";

    private DeployUtils() {
    }

    public static Collection<FileData> getLastDeploymentProjects(Repository repository) throws RRepositoryException {

        Map<String, FileData> latestDeployments = new HashMap<String, FileData>();
        Map<String, String> versionsList = new HashMap<String, String>();

        Collection<FileData> fileDatas = repository.list(DEPLOY_PATH);
        for (FileData fileData : fileDatas) {
            String path = fileData.getName();
            String deploymentName = path.substring(path.lastIndexOf("/") + 1);
            String versionNum = "0";

            if (deploymentName.contains("#")) {
                String versionStr;

                if (deploymentName.indexOf('#') > deploymentName.lastIndexOf('.')) {
                    versionStr = deploymentName.substring(deploymentName.indexOf('#') + 1);
                } else {
                    versionStr = deploymentName.substring(deploymentName.lastIndexOf('.') + 1);
                }

                deploymentName = deploymentName.substring(0, deploymentName.indexOf('#'));

                if (!versionStr.isEmpty()) {
                    versionNum = versionStr;
                }
            }

            if (versionsList.containsKey(deploymentName)) {
                if (versionNum.compareTo(versionsList.get(deploymentName)) > 0) {
                    versionsList.put(deploymentName, versionNum);
                    latestDeployments.put(deploymentName, fileData);
                }
            } else {
                versionsList.put(deploymentName, versionNum);
                latestDeployments.put(deploymentName, fileData);
            }
        }

        return latestDeployments.values();
    }
}
