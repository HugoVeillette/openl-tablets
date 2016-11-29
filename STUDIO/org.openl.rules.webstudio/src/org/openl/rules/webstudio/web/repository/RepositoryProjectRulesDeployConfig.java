package org.openl.rules.webstudio.web.repository;

import com.thoughtworks.xstream.XStreamException;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProjectResource;
import org.openl.rules.project.abstraction.UserWorkspaceProject;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.project.model.RulesDeploy;
import org.openl.rules.project.xml.RulesDeploySerializerFactory;
import org.openl.rules.project.xml.SupportedVersion;
import org.openl.rules.ui.WebStudio;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.ManagedProperty;
import javax.faces.bean.ViewScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

@ManagedBean
@ViewScoped
public class RepositoryProjectRulesDeployConfig {
    private static final String RULES_DEPLOY_CONFIGURATION_FILE = "rules-deploy.xml";
    private final Logger log = LoggerFactory.getLogger(RepositoryProjectRulesDeployConfig.class);

    @ManagedProperty(value = "#{repositoryTreeState}")
    private RepositoryTreeState repositoryTreeState;
    @ManagedProperty(value = "#{rulesDeploySerializerFactory}")
    private RulesDeploySerializerFactory rulesDeploySerializerFactory;

    private WebStudio studio = WebStudioUtils.getWebStudio(true);

    private XmlRulesDeployGuiWrapperSerializer serializer;

    private RulesDeployGuiWrapper rulesDeploy;
    private UserWorkspaceProject lastProject;

    public RepositoryProjectRulesDeployConfig() {
    }

    public void setRepositoryTreeState(RepositoryTreeState repositoryTreeState) {
        this.repositoryTreeState = repositoryTreeState;
    }

    public void setRulesDeploySerializerFactory(RulesDeploySerializerFactory rulesDeploySerializerFactory) {
        this.rulesDeploySerializerFactory = rulesDeploySerializerFactory;
        serializer = new XmlRulesDeployGuiWrapperSerializer(rulesDeploySerializerFactory);
    }

    public RulesDeployGuiWrapper getRulesDeploy() {
        UserWorkspaceProject project = getProject();
        if (lastProject != project) {
            rulesDeploy = null;
            lastProject = project;
        }
        if (project == null) {
            return null;
        }
        if (rulesDeploy == null) {
            if (hasRulesDeploy(project)) {
                rulesDeploy = loadRulesDeploy(project);
            }
        }
        return rulesDeploy;
    }

    public void createRulesDeploy() {
        rulesDeploy = new RulesDeployGuiWrapper(new RulesDeploy(), getSupportedVersion());

        // default values
        rulesDeploy.setProvideRuntimeContext(true);
    }

    public void deleteRulesDeploy() {
        UserWorkspaceProject project = getProject();
        if (hasRulesDeploy(project)) {
            try {
                project.deleteArtefact(RULES_DEPLOY_CONFIGURATION_FILE);
                repositoryTreeState.refreshSelectedNode();
                studio.reset();
            } catch (ProjectException e) {
                FacesUtils.addErrorMessage("Cannot delete " + RULES_DEPLOY_CONFIGURATION_FILE + " file");
                log.error(e.getMessage(), e);
            }
        }

        rulesDeploy = null;
    }

    public void saveRulesDeploy() {
        try {
            UserWorkspaceProject project = getProject();

            InputStream inputStream = IOUtils.toInputStream(serializer.serialize(rulesDeploy, getSupportedVersion(project)));

            if (project.hasArtefact(RULES_DEPLOY_CONFIGURATION_FILE)) {
                AProjectResource artefact = (AProjectResource) project.getArtefact(RULES_DEPLOY_CONFIGURATION_FILE);
                artefact.setContent(inputStream);
            } else {
                project.addResource(RULES_DEPLOY_CONFIGURATION_FILE, inputStream);
                repositoryTreeState.refreshSelectedNode();
                studio.reset();
            }
        } catch (ProjectException e) {
            FacesUtils.addErrorMessage("Cannot save " + RULES_DEPLOY_CONFIGURATION_FILE + " file");
            log.error(e.getMessage(), e);
        }
    }

    private SupportedVersion getSupportedVersion() {
        return getSupportedVersion(getProject());
    }

    private SupportedVersion getSupportedVersion(UserWorkspaceProject project) {
        if (project.getRepository() instanceof LocalRepository) {
            File projectFolder = new File(((LocalRepository) project.getRepository()).getRoot(), project.getFolderPath());
            return rulesDeploySerializerFactory.getSupportedVersion(projectFolder);
        }
        return SupportedVersion.getLastVersion();
    }

    private UserWorkspaceProject getProject() {
        return repositoryTreeState.getSelectedProject();
    }

    private boolean hasRulesDeploy(UserWorkspaceProject project) {
        return project.hasArtefact(RULES_DEPLOY_CONFIGURATION_FILE);
    }

    private RulesDeployGuiWrapper loadRulesDeploy(UserWorkspaceProject project) {
        try {
            AProjectResource artefact = (AProjectResource) project.getArtefact(RULES_DEPLOY_CONFIGURATION_FILE);
            InputStream content = artefact.getContent();
            String sourceString = IOUtils.toStringAndClose(content);
            return serializer.deserialize(sourceString, getSupportedVersion(project));
        } catch (IOException e) {
            FacesUtils.addErrorMessage("Cannot read " + RULES_DEPLOY_CONFIGURATION_FILE + " file");
            log.error(e.getMessage(), e);
        } catch (ProjectException e) {
            FacesUtils.addErrorMessage("Cannot read " + RULES_DEPLOY_CONFIGURATION_FILE + " file");
            log.error(e.getMessage(), e);
        } catch (XStreamException e) {
            FacesUtils.addErrorMessage("Cannot parse " + RULES_DEPLOY_CONFIGURATION_FILE + " file");
            log.error(e.getMessage(), e);
        }

        return null;
    }

    public void validateServiceName(FacesContext context, UIComponent toValidate, Object value) {
        String name = (String) value;

        if (StringUtils.isNotBlank(name)) {
            String message = "Invalid service name: only latin letters, numbers and _ are allowed, name must begin with a letter";
            FacesUtils.validate(name.matches("[a-zA-Z][a-zA-Z_\\-\\d]*"), message);
        }
    }

    public void validateServiceClass(FacesContext context, UIComponent toValidate, Object value) {
        String className = (String) value;
        if (StringUtils.isNotBlank(className)) {
            FacesUtils.validate(className.matches("([\\w$]+\\.)*[\\w$]+"), "Invalid class name");
        }
    }

    public boolean isVersionSupported() {
        return getSupportedVersion().compareTo(SupportedVersion.V5_17) >= 0;
    }

    public boolean isPublishersSupported() {
        return getSupportedVersion().compareTo(SupportedVersion.V5_14) >= 0;
    }

    public boolean isAnnotationTemplateClassNameSupported() {
        return getSupportedVersion().compareTo(SupportedVersion.V5_16) >= 0;
    }

    public boolean isRmiServiceClassSupported() {
        return getSupportedVersion().compareTo(SupportedVersion.V5_16) >= 0;
    }

    public boolean isGroupsSupported() {
        return getSupportedVersion().compareTo(SupportedVersion.V5_17) >= 0;
    }
}
