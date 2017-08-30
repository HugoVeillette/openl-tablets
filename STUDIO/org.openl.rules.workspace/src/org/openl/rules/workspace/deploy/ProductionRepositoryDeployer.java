package org.openl.rules.workspace.deploy;

import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.util.Map;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.openl.config.ConfigurationManagerFactory;
import org.openl.rules.repository.RepositoryFactoryInstatiator;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.ZipUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.InputSource;

/**
 * This class allows to deploy a zip-based project to a production repository.
 * By default configuration of destination repository is get from
 * "deployer.properties" file.
 *
 * @author Yury Molchan
 */
public class ProductionRepositoryDeployer {
    private final Logger log = LoggerFactory.getLogger(ProductionRepositoryDeployer.class);
    public static final String VERSION_IN_DEPLOYMENT_NAME = "version-in-deployment-name";

    /**
     * Deploys a new project to the production repository. If the project exists
     * then it will be skipped to deploy.
     *
     * @param zipFile the project to deploy
     * @param config the configuration file name
     */
    public void deploy(File zipFile, String config) throws Exception {
        if (config == null || config.isEmpty()) {
            config = "deployer.properties";
        }
        ConfigurationManagerFactory configManagerFactory = new ConfigurationManagerFactory(true, null, "");

        Map<String, Object> properties = configManagerFactory.getConfigurationManager(config).getProperties();
        deployInternal(zipFile, properties, true);
    }

    /**
     * Deploys a new or redeploys an existing project to the production
     * repository.
     *
     * @param zipFile the project to deploy
     * @param config the configuration file name
     */
    public void redeploy(File zipFile, String config) throws Exception {
        if (config == null || config.isEmpty()) {
            config = "deployer.properties";
        }
        ConfigurationManagerFactory configManagerFactory = new ConfigurationManagerFactory(true, null, "");

        Map<String, Object> properties = configManagerFactory.getConfigurationManager(config).getProperties();
        deployInternal(zipFile, properties, false);
    }

    public void deployInternal(File zipFile, Map<String, Object> properties, boolean skipExist) throws Exception {
        Repository deployRepo = null;
        try {
            // Initialize repo
            deployRepo = RepositoryFactoryInstatiator.newFactory(properties, false);
            String includeVersion = (String) properties.get(VERSION_IN_DEPLOYMENT_NAME);

            deployInternal(zipFile, deployRepo, skipExist, Boolean.valueOf(includeVersion));
        } finally {
            // Close repo
            if (deployRepo != null) {
                if (deployRepo instanceof Closeable) {
                    // Close repo connection after validation
                    IOUtils.closeQuietly((Closeable) deployRepo);
                }
            }
        }

    }
    public void deployInternal(File zipFile, Repository deployRepo, boolean skipExist, boolean includeVersionInDeploymentName) throws Exception {

        // Temp folders
        File zipFolder = Files.createTempDirectory("openl").toFile();

        FileInputStream stream = null;
        try {
            String name = FileUtils.getBaseName(zipFile.getName());

            // Unpack jar to a file system
            ZipUtils.extractAll(zipFile, zipFolder);

            // Renamed a project according to rules.xml
            File rules = new File(zipFolder, "rules.xml");
            if (rules.exists()) {
                String rulesName = getProjectName(rules);
                if (rulesName != null && !rulesName.isEmpty()) {
                    name = rulesName;
                }
            }
            String deploymentName;

            int version = 0;
            if (includeVersionInDeploymentName) {
                version = DeployUtils.getNextDeploymentVersion(deployRepo, name);
                deploymentName = name + DeployUtils.SEPARATOR + version;
            } else {
                deploymentName = name;
                File rulesDeploy = new File(zipFolder, "rules-deploy.xml");
                if (rulesDeploy.exists()) {
                    String apiVersion = getApiVersion(rulesDeploy);
                    if (apiVersion != null && !apiVersion.isEmpty()) {
                        deploymentName = name + DeployUtils.API_VERSION_SEPARATOR + apiVersion;
                    }
                }
            }
            if (skipExist) {
                if (includeVersionInDeploymentName) {
                    if (version > 1) {
                        log.info("Project [{}] exists. It has been skipped to deploy.", name);
                        return;
                    }
                } else {
                    if (!deployRepo.list(DeployUtils.DEPLOY_PATH + deploymentName + "/").isEmpty()) {
                        return;
                    }
                }
            }

            // Do deploy
            String target = new StringBuilder(DeployUtils.DEPLOY_PATH).append(deploymentName)
                .append('/')
                .append(name)
                .toString();
            FileData dest = new FileData();
            dest.setName(target);
            dest.setAuthor("OpenL_Deployer");
            dest.setSize(zipFile.length());
            stream = new FileInputStream(zipFile);
            deployRepo.save(dest, stream);
        } finally {
            IOUtils.closeQuietly(stream);
            /* Clean up */
            FileUtils.deleteQuietly(zipFolder);
        }
    }

    private String getProjectName(File file) {
        try {
            InputSource inputSource = new InputSource(new FileInputStream(file));
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            XPathExpression xPathExpression = xPath.compile("/project/name");
            return xPathExpression.evaluate(inputSource);
        } catch (FileNotFoundException e) {
            return null;
        } catch (XPathExpressionException e) {
            return null;
        }
    }

    private String getApiVersion(File file) {
        try {
            InputSource inputSource = new InputSource(new FileInputStream(file));
            XPathFactory factory = XPathFactory.newInstance();
            XPath xPath = factory.newXPath();
            XPathExpression xPathExpression = xPath.compile("/version");
            return xPathExpression.evaluate(inputSource);
        } catch (FileNotFoundException e) {
            return null;
        } catch (XPathExpressionException e) {
            return null;
        }
    }
}
