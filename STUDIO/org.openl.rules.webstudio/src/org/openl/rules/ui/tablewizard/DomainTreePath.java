package org.openl.rules.ui.tablewizard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.util.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.web.context.annotation.RequestScope;

/**
 * @author Aliaksandr Antonik.
 */
@Controller
@RequestScope
public class DomainTreePath {

    private String dotExpression;

    @Value("#{tableCreatorWizardManager.wizard.domainTree}")
    private DomainTree domainTree;

    @Value("#{tableCreatorWizardManager.wizard.parameters}")
    private List<TypeNamePair> parameters;

    private DomainTreeContext context;
    private Collection<String> rootObjects;

    private boolean checkDotExpression(String dotExpression) {
        String typename = domainTree.getTypename(context, dotExpression);
        return typename != null;
    }

    public String getDotExpression() {
        return dotExpression;
    }

    public String getNewDotPart() {
        return StringUtils.EMPTY;
    }

    public Collection<String> getSubExpressions() {
        String typename = domainTree.getTypename(context, dotExpression);
        if (typename == null) { // invalid expression
            return rootObjects;
        }

        return domainTree.getClassProperties(typename);
    }

    public void setDomainTree(DomainTree domainTree) {
        this.domainTree = domainTree;
    }

    public void setDotExpression(String dotExpression) {
        this.dotExpression = dotExpression;
    }

    public void setNewDotPart(String newDotPart) {
        if (StringUtils.isNotBlank(newDotPart)) {
            if (StringUtils.isBlank(dotExpression)) {
                dotExpression = newDotPart;
            } else {
                dotExpression += "." + newDotPart;
            }
        }

        if (!checkDotExpression(dotExpression)) {
            dotExpression = StringUtils.EMPTY;
        }
    }

    public void setParameters(List<TypeNamePair> parameters) {
        context = new DomainTreeContext();
        rootObjects = new ArrayList<>(parameters.size());
        for (TypeNamePair pair : parameters) {
            context.setObjectType(pair.getName(), pair.getType());
            rootObjects.add(pair.getName());
        }
    }
}
