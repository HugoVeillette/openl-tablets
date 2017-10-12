package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.util.*;

import org.openl.rules.common.ArtefactPath;
import org.openl.rules.common.CommonUser;
import org.openl.rules.common.LockInfo;
import org.openl.rules.common.ProjectException;
import org.openl.rules.common.ProjectVersion;
import org.openl.rules.common.PropertiesContainer;
import org.openl.rules.common.Property;
import org.openl.rules.common.PropertyException;
import org.openl.rules.common.impl.ArtefactPathImpl;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.common.impl.RepositoryVersionInfoImpl;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.dtr.impl.LockInfoImpl;
import org.openl.util.RuntimeExceptionWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// TODO: Remove PropertiesContainer interface from the class
public class AProjectArtefact implements PropertiesContainer {
    private final Logger log = LoggerFactory.getLogger(AProjectArtefact.class);

    private AProject project;
    private Repository repository;
    private FileData fileData;
    private String versionComment;

    private Date modifiedTime;

    public AProjectArtefact(AProject project, Repository repository, FileData fileData) {
        this.project = project;
        this.repository = repository;
        this.fileData = fileData;
        this.modifiedTime = fileData == null ? null : fileData.getModifiedAt();
    }

    public AProject getProject() {
        return project;
    }

    public FileData getFileData() {
        return fileData;
    }

    public void setFileData(FileData fileData) {
        this.fileData = fileData;
    }

    public Repository getRepository() {
        return repository;
    }

    public void setRepository(Repository repository) {
        this.repository = repository;
    }

    public void addProperty(Property property) throws PropertyException {
        throw new UnsupportedOperationException();
    }

    public Collection<Property> getProperties() {
        return Collections.emptyList();
    }

    public Property getProperty(String name) throws PropertyException {
        return null;
    }

    public boolean hasProperty(String name) {
        return false;
    }

    public Property removeProperty(String name) throws PropertyException {
        throw new UnsupportedOperationException();
    }

    public void delete() throws ProjectException {
        FileData fileData = getFileData();
        if (!getRepository().delete(fileData)) {
            throw new ProjectException("Resource is absent or can't be deleted");
        }
    }

    public ArtefactPath getArtefactPath() {
        return new ArtefactPathImpl(getFileData().getName());
    }

    public String getInternalPath() {
        String projectPath = getProject().getFileData().getName();
        return getFileData().getName().substring(projectPath.length() + 1);
    }

    public String getName() {
        String name = getFileData().getName();
        return name.substring(name.lastIndexOf("/") + 1);
    }

    public boolean isFolder() {
        return false;
    }

    // current version
    public ProjectVersion getVersion() {
        return createProjectVersion(getFileData());
    }

    public ProjectVersion getLastVersion() {
        List<FileData> fileDatas = null;
        try {
            fileDatas = getRepository().listHistory(getFileData().getName());
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
        return fileDatas.isEmpty() ?
               createProjectVersion(null) :
               createProjectVersion(fileDatas.get(fileDatas.size() - 1));
    }

    public ProjectVersion getFirstVersion() {
        int versionsCount = getVersionsCount();
        if (versionsCount == 0) {
            return new RepositoryProjectVersionImpl("0", null);
        }

        if (versionsCount == 1) {
            try {
                return getVersion(0);
            } catch (RRepositoryException e1) {
                return new RepositoryProjectVersionImpl("0", null);
            }
        }

        try {
            return getVersion(getFirstRevisionIndex());
        } catch (Exception e) {
            return new RepositoryProjectVersionImpl("0", null);
        }
    }

    public int getFirstRevisionIndex() {
        return 0;
    }

    public List<ProjectVersion> getVersions() {
        if (getFileData() == null) {
            return Collections.emptyList();
        }
        Collection<FileData> fileDatas = null;
        try {
            fileDatas = getRepository().listHistory(getFileData().getName());
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
        List<ProjectVersion> versions = new ArrayList<ProjectVersion>();
        for (FileData data : fileDatas) {
            versions.add(createProjectVersion(data));
        }
        return versions;
    }

    public boolean hasModifications() {
        return !getFirstVersion().equals(getLastVersion());
    }

    public int getVersionsCount() {
        try {
            return getFileData() == null ? 0 : getRepository().listHistory(getFileData().getName()).size();
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
    }

    protected ProjectVersion getVersion(int index) throws RRepositoryException {
        List<FileData> fileDatas = null;
        try {
            fileDatas = getRepository().listHistory(getFileData().getName());
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
        return fileDatas.isEmpty() ? null : createProjectVersion(fileDatas.get(index));
    }

    protected ProjectVersion createProjectVersion(FileData fileData) {
        if (fileData == null) {
            return new RepositoryProjectVersionImpl("0", null);
        }
        RepositoryVersionInfoImpl rvii = new RepositoryVersionInfoImpl(fileData.getModifiedAt(), fileData.getAuthor());
        String version = fileData.getVersion();
        RepositoryProjectVersionImpl projectVersion = new RepositoryProjectVersionImpl(version == null ? "0" : version, rvii);
        projectVersion.setVersionComment(fileData.getComment());
        return projectVersion;
    }

    public void update(AProjectArtefact artefact, CommonUser user) throws ProjectException {
        refresh();
    }

    public void refresh() {
        // TODO
    }

    public void lock() throws ProjectException {
        // Do  nothing
    }

    public void unlock() throws ProjectException {
        // Do  nothing
    }

    public boolean isLocked() {
        return getLockInfo().isLocked();
    }

    public boolean isLockedByUser(CommonUser user) {
        return isLockedByUser(getLockInfo(), user);
    }

    protected boolean isLockedByUser(LockInfo lockInfo, CommonUser user) {
        if (lockInfo.isLocked()) {
            CommonUser lockedBy = lockInfo.getLockedBy();
            if (lockedBy.getUserName().equals(user.getUserName())) {
                return true;
            }

            if (isLockedByDefaultUser(lockedBy, user)) {
                return true;
            }
        }
        return false;
    }

    public LockInfo getLockInfo() {
        return LockInfoImpl.NO_LOCK;
    }

    public boolean isModified(){
        FileData fileData = getFileData();
        if (fileData == null) {
            return false;
        }
        if (modifiedTime == null) {
            return true;
        }
        return !modifiedTime.equals(fileData.getModifiedAt());
    }

    public void setVersionComment(String versionComment) {
        FileData fileData = getFileData();
        if (fileData != null) {
            fileData.setComment(versionComment);
        } else {
            this.versionComment = versionComment;
        }
    }

    public String getVersionComment() {
        FileData fileData = getFileData();
        return fileData == null ? versionComment : fileData.getComment();
    }

    /**
     * For backward compatibility. Earlier user name in the single user mode analog was "LOCAL".
     * Checks that lockedUser is LOCAL and current user is DEFAULT
     * 
     * @param lockedUser - owner of the lock
     * @param currentUser - current user trying to unlock
     * @return true if owner of the lock is "LOCAL" and current user is "DEFAULT"
     */
    private boolean isLockedByDefaultUser(CommonUser lockedUser, CommonUser currentUser) {
        return "LOCAL".equals(lockedUser.getUserName()) && "DEFAULT".equals(currentUser.getUserName());
    }

    public boolean isHistoric() {
        return getFileData().getVersion() != null;
    }

}
