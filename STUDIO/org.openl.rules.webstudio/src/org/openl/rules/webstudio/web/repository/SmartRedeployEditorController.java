package org.openl.rules.webstudio.web.repository;

import org.openl.rules.common.ProjectException;
import org.openl.rules.project.abstraction.AProject;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.annotation.SessionScope;

/**
 * @author Aleh Bykhavets
 */
@Controller
@SessionScope
public class SmartRedeployEditorController extends AbstractSmartRedeployController {

    public void setProject(String projectName) {
        try {
            reset();
            currentProject = userWorkspace.getProject(projectName);
        } catch (ProjectException e) {
            log.warn("Failed to retrieve the project", e);
        }
    }

    public void reset() {
        setRepositoryConfigName(null);
        items = null;
        currentProject = null;
    }

    @Override
    public AProject getSelectedProject() {
        return currentProject;
    }
}
