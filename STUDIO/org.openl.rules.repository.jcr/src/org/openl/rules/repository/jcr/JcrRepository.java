package org.openl.rules.repository.jcr;

import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.repository.RRepositoryListener;
import org.openl.rules.repository.RRepositoryListener.RRepositoryEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;

/**
 * Implementation for JCR Repository. One JCR Repository instance per user.
 *
 * @author Aleh Bykhavets
 */
public class JcrRepository extends BaseJcrRepository {
    private final Logger log = LoggerFactory.getLogger(JcrRepository.class);

    private Node defRulesLocation;
    private Node defDeploymentConfigLocation;

    private RRepositoryListener listeners;

    public JcrRepository(Session session,
                         Node defRulesLocation,
                         Node defDeploymentConfigLocation)
            throws RepositoryException {
        super(session);
        this.defRulesLocation = defRulesLocation;
        this.defDeploymentConfigLocation = defDeploymentConfigLocation;

        session.getWorkspace()
                .getObservationManager()
                .addEventListener(this, Event.PROPERTY_CHANGED | Event.NODE_REMOVED, session.getRootNode().getPath(),
                        true, null, null, false);

    }

    // ------ protected methods ------

    private static final String CHECKED_OUT_PROPERTY = "jcr:isCheckedOut";

    private String extractProjectName(String relativePath) {
        return new ArtefactPathImpl(relativePath).segment(0);
    }

    private boolean isProjectDeletedEvent(Event event, String relativePath) {
        ArtefactPathImpl path = new ArtefactPathImpl(relativePath);
        return path.segmentCount() == 1 && event.getType() == Event.NODE_REMOVED;
    }

    private boolean isProjectModifiedEvent(Event event, String relativePath) {
        return relativePath.contains(CHECKED_OUT_PROPERTY);
    }

    public void onEvent(EventIterator eventIterator) {
        while (listeners != null && eventIterator.hasNext()) {
            Event event = eventIterator.nextEvent();
            try {
                String path = event.getPath();
                if (path.startsWith(defRulesLocation.getPath() + "/")) {
                    String relativePath = path.substring(defRulesLocation.getPath().length() + 1);
                    if (isProjectDeletedEvent(event, relativePath) || isProjectModifiedEvent(event, relativePath)) {
                        listeners.onEventInRulesProjects(new RRepositoryEvent(extractProjectName(relativePath)));
                    }
                } else if (path.startsWith(defDeploymentConfigLocation.getPath() + "/")) {
                    String relativePath = path.substring(defDeploymentConfigLocation.getPath().length() + 1);
                    if (isProjectDeletedEvent(event, relativePath) || isProjectModifiedEvent(event, relativePath)) {
                        listeners.onEventInDeploymentProjects(new RRepositoryEvent(extractProjectName(relativePath)));
                    }
                }
            } catch (RepositoryException e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    public void setListener(RRepositoryListener listener) {
        listeners = listener;
    }

    @Override
    protected boolean isBaseNode(Node node) throws RepositoryException {
        String path = node.getPath();
        return path.equals(defRulesLocation.getPath()) || path.equals(defDeploymentConfigLocation.getPath());
    }
}
