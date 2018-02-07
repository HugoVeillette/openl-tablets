package org.openl.rules.webstudio.web.repository;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectDescriptor;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.project.IRulesDeploySerializer;
import org.openl.rules.project.abstraction.ADeploymentProject;
import org.openl.rules.project.abstraction.AProject;
import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.xml.XmlRulesDeploySerializer;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.WorkspaceException;
import org.openl.rules.workspace.deploy.DeployID;
import org.openl.rules.workspace.deploy.DeployUtils;
import org.openl.rules.workspace.deploy.DeploymentException;
import org.openl.rules.workspace.dtr.DesignTimeRepository;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;

/**
 * Deployment manager
 *
 * @author Andrey Naumenko
 */
public class DeploymentManager implements InitializingBean {
    private final Logger log = LoggerFactory.getLogger(DeploymentManager.class);

    private String[] initialProductionRepositoryConfigNames;
    private DesignTimeRepository designRepository;
    private IRulesDeploySerializer rulesDeploySerializer = new XmlRulesDeploySerializer();

    private Set<String> deployers = new HashSet<String>();

    public void addRepository(String repositoryConfigName) {
        deployers.add(repositoryConfigName);
    }

    public void removeRepository(String repositoryConfigName) throws RRepositoryException {
        deployers.remove(repositoryConfigName);
        repositoryFactoryProxy.releaseRepository(repositoryConfigName);
    }

    public Collection<String> getRepositoryConfigNames() {
        return deployers;
    }

    public DeployID deploy(ADeploymentProject project, String repositoryConfigName) throws WorkspaceException,
                                                                                    ProjectException {
        if (!deployers.contains(repositoryConfigName)) {
            throw new IllegalArgumentException("No such repository '" + repositoryConfigName + "'");
        }

        String userName = WebStudioUtils.getRulesUserSession(FacesUtils.getSession()).getUserName();

        @SuppressWarnings("rawtypes")
        Collection<ProjectDescriptor> projectDescriptors = project.getProjectDescriptors();

        try {
            Repository deployRepo = repositoryFactoryProxy.getRepositoryInstance(repositoryConfigName);
            StringBuilder sb = new StringBuilder(project.getName());
            ProjectVersion projectVersion = project.getVersion();
            boolean includeVersionInDeploymentName = repositoryFactoryProxy.isIncludeVersionInDeploymentName( repositoryConfigName);
            if (projectVersion != null) {
                if (includeVersionInDeploymentName) {
                    int version = DeployUtils.getNextDeploymentVersion(deployRepo, project.getName());
                    sb.append(DeployUtils.SEPARATOR).append(version);
                } else {
                    String apiVersion = getApiVersion(project);
                    if (apiVersion != null) {
                        sb.append(DeployUtils.API_VERSION_SEPARATOR).append(apiVersion);
                    }
                }
            }
            DeployID id = new DeployID(sb.toString());

            String deploymentPath = DeployUtils.DEPLOY_PATH + id.getName();

            List<FileData> existingProjects = deployRepo.list(deploymentPath + "/");
            List<FileData> projectsToDelete = findProjectsToDelete(existingProjects, projectDescriptors);
            for (FileData fileData : projectsToDelete) {
                deployRepo.delete(fileData);
            }

            Repository designRepo = designRepository.getRepository();
            for (ProjectDescriptor<?> pd : projectDescriptors) {
                InputStream stream = null;
                try {
                    String version = pd.getProjectVersion().getVersionName();
                    String projectName = pd.getProjectName();
                    FileItem srcPrj = designRepo.readHistory("DESIGN/rules/" + projectName, version);
                    stream = srcPrj.getStream();
                    FileData dest = new FileData();
                    dest.setName(deploymentPath + "/" + projectName);
                    dest.setAuthor(userName);
                    dest.setComment(srcPrj.getData().getComment());
                    dest.setSize(srcPrj.getData().getSize());
                    deployRepo.save(dest, stream);
                } finally {
                    IOUtils.closeQuietly(stream);
                }
            }

            // TODO: Some analogue of notifyChanges() possibly will be needed
            // rRepository.notifyChanges();
            return id;
        } catch (Exception e) {
            throw new DeploymentException("Failed to deploy: " + e.getMessage(), e);
        }
    }

    private List<FileData> findProjectsToDelete(List<FileData> existingProjects, Collection<ProjectDescriptor> projectsToDeploy) {
        List<FileData> projectsToDelete = new ArrayList<>(existingProjects);
        // Filter out projects that will be replaced with a new version
        for (ProjectDescriptor projectToDeploy : projectsToDeploy) {
            for (Iterator<FileData> it = projectsToDelete.iterator(); it.hasNext(); ) {
                String folderPath = it.next().getName();
                String projectName = folderPath.substring(folderPath.lastIndexOf("/") + 1);
                if (projectName.equals(projectToDeploy.getProjectName())) {
                    // This project will be replaced with a new version. No need to delete it
                    it.remove();
                    break;
                }
            }
        }
        return projectsToDelete;
    }

    private String getApiVersion(ADeploymentProject deploymentConfiguration) {
        Repository designRepo = designRepository.getRepository();

        for (ProjectDescriptor pd : deploymentConfiguration.getProjectDescriptors()) {
            try {
                InputStream content = null;
                try {
                    String projectVersion = pd.getProjectVersion().getVersionName();
                    String projectName = pd.getProjectName();
                    AProject project = new AProject(designRepo, "DESIGN/rules/" + projectName, projectVersion, false);

                    AProjectArtefact artifact = project.getArtefact(DeployUtils.RULES_DEPLOY_XML);
                    if (artifact instanceof AProjectResource) {
                        AProjectResource resource = (AProjectResource) artifact;
                        content = resource.getContent();
                        RulesDeploy rulesDeploy = rulesDeploySerializer.deserialize(content);
                        String apiVersion = rulesDeploy.getVersion();
                        if (StringUtils.isNotBlank(apiVersion)) {
                            return apiVersion;
                        }
                    }
                } catch (ProjectException ignored) {
                } finally {
                    if (content != null) {
                        try {
                            content.close();
                        } catch (IOException e) {
                            log.error(e.getMessage(), e);
                        }
                    }
                }
            } catch (Throwable e) {
                log.error(
                        "Project loading from repository was failed! Project with name \"{}\" in deployment configuration \"{}\" was skipped!",
                        pd.getProjectName(),
                        deploymentConfiguration.getName(),
                        e);
            }
        }

        return null;
    }

    private ProductionRepositoryFactoryProxy repositoryFactoryProxy;

    public void setRepositoryFactoryProxy(ProductionRepositoryFactoryProxy repositoryFactoryProxy) {
        this.repositoryFactoryProxy = repositoryFactoryProxy;
    }

    public void setInitialProductionRepositoryConfigNames(String[] initialProductionRepositoryConfigNames) {
        this.initialProductionRepositoryConfigNames = initialProductionRepositoryConfigNames;
    }

    public void setDesignRepository(DesignTimeRepository designRepository) {
        this.designRepository = designRepository;
    }

    @Override
    public void afterPropertiesSet() {
        if (initialProductionRepositoryConfigNames != null) {
            for (String repositoryConfigName : initialProductionRepositoryConfigNames) {
                addRepository(repositoryConfigName);
            }
        }
    }
}
