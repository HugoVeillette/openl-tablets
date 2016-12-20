package org.openl.rules.repository.jcr;

import org.openl.rules.common.ProjectException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.repository.*;
import org.openl.rules.repository.api.FolderAPI;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class JcrProductionRepository extends BaseJcrRepository implements RProductionRepository {
    private final Logger log = LoggerFactory.getLogger(JcrProductionRepository.class);

    final static String PROPERTY_NOTIFICATION = "deploymentReady";
    public static final String DEPLOY_ROOT = "/deploy";

    private Node deployLocation;
    private List<RDeploymentListener> listeners = new CopyOnWriteArrayList<RDeploymentListener>();

    public JcrProductionRepository(Session session, Node deployLocation) throws RepositoryException {
        super(session);
        this.deployLocation = deployLocation;

        session.getWorkspace().getObservationManager().addEventListener(this, Event.PROPERTY_ADDED | Event.PROPERTY_CHANGED | Event.PROPERTY_REMOVED, DEPLOY_ROOT, false,
                null, null, false);
    }

    public void addListener(RDeploymentListener listener) {
        listeners.add(listener);
    }

    public boolean hasDeploymentProject(String name) throws RRepositoryException {
        try {
            return deployLocation.hasNode(name);
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to check project {0}", e, name);
        }
    }

    /**
     * Checks whether project with given name exists in the repository.
     *
     * @return <code>true</code> if project with such name exists
     * @throws org.openl.rules.repository.exceptions.RRepositoryException
     */
    public boolean hasProject(String name) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public void onEvent(EventIterator eventIterator) {
        boolean activate = false;
        while (eventIterator.hasNext()) {
            Event event = eventIterator.nextEvent();
            try {
                if (event.getPath().equals(DEPLOY_ROOT + "/" + PROPERTY_NOTIFICATION)) {
                    activate = true;
                    break;
                }
            } catch (RepositoryException e) {
                log.debug("onEvent-1", e);
            }
        }

        if (activate) {
            for (RDeploymentListener listener : listeners) {
                try {
                    listener.onEvent();
                } catch (Exception e) {
                    log.error("onEvent-2", e);
                }
            }

        }
    }

    public boolean removeListener(RDeploymentListener listener) {
        return listeners.remove(listener);
    }

    public FolderAPI createDeploymentProject(String name) throws RRepositoryException {
        try {
            String path = "deploy/" + name;
            Node parent = checkFolder(path.substring(0, path.lastIndexOf("/")));
            Node node = NodeUtil.createNode(parent, name.substring(name.lastIndexOf("/") + 1), JcrNT.NT_APROJECT, true);
            deployLocation.save();
            node.checkin();
            return new JcrFolderAPI(node, new ArtefactPathImpl(new String[]{name}));
        } catch (RepositoryException e) {
            throw new RRepositoryException("", e);
        } catch (ProjectException e) {
            throw new RRepositoryException("", e);
        }
    }

    //FIXME
    private static final Object lock = new Object();

    public void notifyChanges() throws RRepositoryException {
        synchronized (lock) {
            try {
                deployLocation.setProperty(JcrProductionRepository.PROPERTY_NOTIFICATION, System.currentTimeMillis());
                deployLocation.save();
            } catch (RepositoryException e) {
                throw new RRepositoryException("Failed to notify changes", e);
            }
        }
    }

    public FolderAPI createRulesProject(String name) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public FolderAPI getDeploymentProject(String name) throws RRepositoryException {
        Node node;
        try {
            node = deployLocation.getNode(name);
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to get node", e);
        }

        try {
            return new JcrFolderAPI(node, new ArtefactPathImpl(new String[]{name}));
        } catch (RepositoryException e) {
            throw new RRepositoryException("failed to wrap JCR node", e);
        }
    }

    public FolderAPI getRulesProject(String name) throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public List<FolderAPI> getRulesProjects() throws RRepositoryException {
        throw new UnsupportedOperationException();
    }

    public void addRepositoryListener(RRepositoryListener listener) {
        throw new UnsupportedOperationException();
    }

    public void removeRepositoryListener(RRepositoryListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    protected boolean isBaseNode(Node node) throws RepositoryException {
        return node.getPath().equals(deployLocation.getPath());
    }
}
