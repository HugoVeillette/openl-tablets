package org.openl.rules.repository.factories;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import javax.jcr.Value;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathFactory;

import org.openl.config.ConfigPropertyBoolean;
import org.openl.config.ConfigPropertyString;
import org.openl.config.ConfigSet;
import org.openl.rules.repository.RRepository;
import org.openl.rules.repository.RRepositoryFactory;
import org.openl.rules.repository.RTransactionManager;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.repository.jcr.JcrNT;
import org.openl.rules.repository.jcr.JcrProductionRepository;
import org.openl.rules.repository.jcr.JcrRepository;
import org.xml.sax.InputSource;

/**
 * This is Abstract class with common code for Local and RMI methods of
 * accessing any JCR-170 compliant instance.
 * <p>
 * It performs basic insanity checks. For example, it verifies that OpenL node
 * types are registered in using JCR.
 *
 * @author Aleh Bykhavets
 *
 */
public abstract class AbstractJcrRepositoryFactory implements RRepositoryFactory {

    protected static final String DEFAULT_NODETYPE_FILE = "/org/openl/rules/repository/openl_nodetypes.xml";

    /** Default path where new project should be created */
    protected final ConfigPropertyString confRulesProjectsLocation = new ConfigPropertyString(
            "design-repository.rules.path", "/rules");
    protected final ConfigPropertyString confDeploymentProjectsLocation = new ConfigPropertyString(
            "design-repository.deployments.path", "/deployments");

    protected Repository repository;
//    protected String repositoryName;
    private RRepository rulesRepository;

    protected ConfigPropertyString login;
    protected ConfigPropertyString password;
    protected ConfigPropertyString uri;
    boolean designRepositoryMode = false;


    /**
     * Checks whether the JCR instance is prepared for OpenL. If it is the first
     * time, then there are no openL node types, yet.
     *
     * @throws RepositoryException if failed
     */
    protected void checkOnStart() throws RepositoryException {
        Session systemSession = null;
        try {
            // FIXME: do not hardcode system credentials
            systemSession = createSession();
            NodeTypeManager ntm = systemSession.getWorkspace().getNodeTypeManager();

            try {
                // Does JCR know anything about OpenL?
                ntm.getNodeType(JcrNT.NT_REPOSITORY);
            } catch (Exception e) {
                // No, it doesn't.
                initNodeTypes(ntm);
            }
        } finally {
            if (systemSession != null) {
                systemSession.logout();
            }
        }
    }

    /**
     * Creates JCR Session.
     *
     * @return new JCR session
     * @throws RepositoryException if fails or user credentials are not correct
     */
    protected Session createSession() throws RepositoryException {
        String loginValue = login.getValue();
        String passwordValue = password.getValue();
        if (loginValue == null) {
            loginValue = "";
        }
        if (passwordValue == null) {
            passwordValue = "";
        }
        Credentials credencials = new SimpleCredentials(loginValue, passwordValue.toCharArray());
        return repository.login(credencials);
    }

    /** {@inheritDoc} */
    public RRepository getRepositoryInstance() throws RRepositoryException {
        if(rulesRepository == null){
            rulesRepository = createRepository();
        }
        return rulesRepository;
    }

    public RRepository createRepository() throws RRepositoryException {
        Session session = null;
        try {
            session = createSession();

            RRepository theRepository;
            if (designRepositoryMode) {
                theRepository = new JcrRepository(session,
                        confRulesProjectsLocation.getValue(), confDeploymentProjectsLocation.getValue());
            } else {
                theRepository = new JcrProductionRepository(session);
            }
            return theRepository;
        } catch (RepositoryException e) {
            if (session != null){
                session.logout();
            }
            throw new RRepositoryException("Failed to get Repository Instance", e);
        } 
    }

    /** {@inheritDoc} */
    public void initialize(ConfigSet confSet) throws RRepositoryException {
        confSet.updateProperty(confRulesProjectsLocation);
        confSet.updateProperty(confDeploymentProjectsLocation);

        ConfigPropertyBoolean dessignModeProperty = new ConfigPropertyBoolean("dessign-mode", false);
        confSet.updateProperty(dessignModeProperty);
        designRepositoryMode = dessignModeProperty.getValue();

        String type = designRepositoryMode ? "design" : "production";
        login = new ConfigPropertyString(type + "-repository.login", null);
        password = new ConfigPropertyString(type + "-repository.password", null);
        uri = new ConfigPropertyString(type + "-repository.uri", null);

        confSet.updateProperty(login);
        confSet.updatePasswordProperty(password);
        confSet.updateProperty(uri);

        // TODO: add default path support
        // 1. check path -- create if absent
        // 2. pass as parameter or property to JcrRepository
    }

    /**
     * Registers OpenL node types in JCR.
     * <p>
     * Usually it can be done on local JCR instance.
     * <p>
     * This operation may not be supported via RMI.
     *
     * @param ntm node type manager
     * @throws RepositoryException if failed
     */
    protected abstract void initNodeTypes(NodeTypeManager ntm) throws RepositoryException;

    public void release() throws RRepositoryException {
        // If rulesRepository is not created, we don't need to create it and then release it
        if (rulesRepository != null) {
            rulesRepository.release();
            rulesRepository = null;
        }
    }

    /**
     * Sets repository reference. Must be called before invoking
     * {@link #getRepositoryInstance()} method.
     *
     * @param rep implementation specific repository
     * @throws RepositoryException if fails to check first start
     */
    protected void setRepository(Repository rep) throws RepositoryException {
        repository = rep;

        checkOnStart();
    }

}
