package org.openl.rules.project.abstraction;

import static org.openl.rules.workspace.deploy.DeployUtils.DEPLOY_PATH;

import java.io.File;
import java.util.*;

import org.openl.rules.common.CommonUser;
import org.openl.rules.common.CommonVersion;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.impl.CommonVersionImpl;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.common.impl.RepositoryVersionInfoImpl;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.workspace.deploy.DeployUtils;

/**
 * Class representing deployment from ProductionRepository. Deployment is set of
 * logically grouped rules projects.
 * 
 * @author PUdalau
 */
public class Deployment extends AProjectFolder {
	private Map<String, AProject> projects;

	private String deploymentName;
	private CommonVersion commonVersion;

	public static List<Deployment> getDeployments(Repository repository, Collection<FileData> fileDatas) {
		Set<String> deploymentFolderNames = new HashSet<String>();
		for (FileData fileData : fileDatas) {
			String deploymentName = fileData.getName().substring(DEPLOY_PATH.length()).split("/")[0];
			deploymentFolderNames.add(deploymentName);
		}

		List<Deployment> deployments = new ArrayList<Deployment>();
		for (String deploymentFolderName : deploymentFolderNames) {
			int separatorPosition = deploymentFolderName.lastIndexOf(DeployUtils.SEPARATOR);

			if (separatorPosition >= 0) {
				String deploymentName = deploymentFolderName.substring(0, separatorPosition);
				String version = deploymentFolderName.substring(separatorPosition + 1);
				CommonVersionImpl commonVersion = new CommonVersionImpl(version);
				deployments.add(new Deployment(repository, DEPLOY_PATH + deploymentFolderName, deploymentName, commonVersion));
			} else {
				deployments.add(new Deployment(repository, DEPLOY_PATH + deploymentFolderName, deploymentFolderName, null));
			}
		}

		return deployments;
	}

	public Deployment(Repository repository, String folderName, String deploymentName, CommonVersion commonVersion) {
		super(null, repository, folderName, commonVersion == null ? null : commonVersion.getVersionName());
		init();
		this.commonVersion = commonVersion;
		this.deploymentName = deploymentName;
	}

	public CommonVersion getCommonVersion() {
		if (commonVersion == null) return this.getVersion();
		return commonVersion;
	}

	public String getDeploymentName() {
		if (deploymentName == null) return this.getName();
		return deploymentName;
	}
	
	@Override
	public void refresh() {
		init();
	}

	private void init() {
		super.refresh();
		projects = new HashMap<String, AProject>();

		for (AProjectArtefact artefact : getArtefactsInternal().values()) {
			String projectPath = artefact.getArtefactPath().getStringValue();
			projects.put(artefact.getName(), new AProject(getRepository(), projectPath));
		}
	}

	public Collection<AProject> getProjects() {
		return projects.values();
	}

	public AProject getProject(String name) {
		return projects.get(name);
	}

	@Override
	public ProjectVersion getVersion() {
		RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(null, null);
		return new RepositoryProjectVersionImpl(commonVersion, rvii);
	}

	@Override
	protected Map<String, AProjectArtefact> createInternalArtefacts() {
		if (getRepository() instanceof LocalRepository) {
			LocalRepository repository = (LocalRepository) getRepository();
			File[] files = new File(repository.getLocation(), getFolderPath()).listFiles();
			Map<String, AProjectArtefact> result = new HashMap<String, AProjectArtefact>();
			if (files != null) {
				for (File file : files) {
					result.put(file.getName(), new AProject(repository, getFolderPath() + "/" + file.getName()));
				}
			}
			return result;
		} else {
			return super.createInternalArtefacts();
		}
	}

	@Override
	public boolean isHistoric() {
		return false;
	}

	@Override
	public void update(AProjectArtefact newFolder, CommonUser user) throws ProjectException {
		Deployment other = (Deployment) newFolder;
		// add new
		for (AProject otherProject : other.getProjects()) {
			String name = otherProject.getName();
			if (!hasArtefact(name)) {
				AProject newProject = new AProject(getRepository(), getFolderPath() + "/" + name);
				newProject.update(otherProject, user);
				projects.put(newProject.getName(), newProject);
			}
		}
	}
}
