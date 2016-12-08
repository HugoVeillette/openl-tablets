package org.openl.rules.project.abstraction;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.rules.common.*;
import org.openl.rules.common.impl.RepositoryProjectVersionImpl;
import org.openl.rules.project.impl.local.LocalRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.rules.workspace.dtr.impl.LockInfoImpl;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.RuntimeExceptionWrapper;

public class RulesProject extends UserWorkspaceProject {
    private LocalRepository localRepository;
    private String localFolderName;
    private Repository designRepository;
    private String designFolderName;

    public RulesProject(UserWorkspace userWorkspace,
            LocalRepository localRepository,
            FileData localFileData,
            Repository designRepository, FileData designFileData) {
        super(userWorkspace.getUser(), localFileData != null ? localRepository : designRepository,
                localFileData != null ? localFileData : designFileData, localFileData != null);
        this.localRepository = localRepository;
        this.localFolderName = localFileData == null ? null : localFileData.getName();
        this.designRepository = designRepository;
        this.designFolderName = designFileData == null ? null : designFileData.getName();
    }

    @Override
    public void save(CommonUser user) throws ProjectException {
        clearModifyStatus();
        AProject designProject = new AProject(designRepository, designFolderName, false);
        AProject localProject = new AProject(localRepository, localFolderName, true);
        designProject.update(localProject, user);
        setHistoryVersion(designProject.getFileData().getVersion());
        unlock(user);
        refresh();
    }

    @Override
    public void delete(CommonUser user) throws ProjectException {
        if (isLocalOnly()) {
            erase();
        } else {
            super.delete(user);
        }
    }

    public void close(CommonUser user) throws ProjectException {
        try {
            if (localFolderName != null) {
                deleteFromLocalRepository();
            }
            if (isLockedByUser(user)) {
                unlock(user);
            }
            if (!isLocalOnly()) {
                setRepository(designRepository);
                setFolderPath(designFolderName);
                setHistoryVersion(null);
                setFolderStructure(false);
            }
        } finally {
            refresh();
        }
    }

    private void deleteFromLocalRepository() throws ProjectException {
        try {
            for (FileData fileData : localRepository.list(localFolderName)) {
                if (!localRepository.delete(fileData.getName())) {
                    throw new ProjectException("Can't close project because some resources are used");
                }
            }
        } catch (IOException e) {
            throw new ProjectException("Not possible to read the directory", e);
        }
    }

    @Override
    public void erase() throws ProjectException {
        try {
            if (designFolderName != null) {
                if (!designRepository.deleteHistory(designFolderName, null)) {
                    throw new ProjectException("Can't erase project because it is absent or can't be deleted");
                }
            } else {
                deleteFromLocalRepository();
            }
        } finally {
          refresh();
        }
    }

    @Override
    public void refresh() {
        super.refresh();
        setFileData(null);
    }

    public LockInfo getLockInfo() {
        return LockInfoImpl.NO_LOCK;
    }

    @Override
    public void lock(CommonUser user) throws ProjectException {
        // Do nothing
    }

    @Override
    public void unlock(CommonUser user) throws ProjectException {
        // Do nothing
    }

    public String getLockedUserName() {
        return getLockInfo().getLockedBy().getUserName();
    }

    public ProjectVersion getVersion() {
        String historyVersion = getHistoryVersion();
        if (historyVersion == null) {
            return getLastVersion();
        }
        return new RepositoryProjectVersionImpl(historyVersion, null);
    }

    @Override
    public ProjectVersion getLastVersion() {
        List<FileData> fileDatas = getHistoryFileDatas();
        return fileDatas.isEmpty() ? null : createProjectVersion(fileDatas.get(fileDatas.size() - 1));
    }

    @Override
    public List<ProjectVersion> getVersions() {
        Collection<FileData> fileDatas = getHistoryFileDatas();
        List<ProjectVersion> versions = new ArrayList<ProjectVersion>();
        for (FileData data : fileDatas) {
            versions.add(createProjectVersion(data));
        }
        return versions;
    }

    @Override
    public int getVersionsCount() {
        try {
            if (designFolderName != null) {
                return designRepository.listHistory(designFolderName).size();
            } else {
                // Local repository doesn't have versions
                return 0;
            }
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
    }

    @Override
    protected ProjectVersion getVersion(int index) throws RRepositoryException {
        List<FileData> fileDatas = getHistoryFileDatas();
        return fileDatas.isEmpty() ? null : createProjectVersion(fileDatas.get(index));
    }

    private List<FileData> getHistoryFileDatas() {
        List<FileData> fileDatas;
        try {
            if (designFolderName != null) {
                fileDatas = designRepository.listHistory(designFolderName);
            } else {
                fileDatas = localRepository.list(localFolderName);
            }
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
        return fileDatas;
    }

    public List<ProjectVersion> getArtefactVersions(ArtefactPath artefactPath) {
        String subPath = artefactPath.getStringValue();
        if (subPath.isEmpty() || subPath.equals("/")) {
            return getVersions();
        }
        if (!subPath.startsWith("/")) {
            subPath += "/";
        }
        String fullPath = getFolderPath() + subPath;
        Collection<FileData> fileDatas;
        try {
            fileDatas = getRepository().listHistory(fullPath);
        } catch (IOException ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
        List<ProjectVersion> versions = new ArrayList<ProjectVersion>();
        for (FileData data : fileDatas) {
            versions.add(createProjectVersion(data));
        }
        return versions;
    }

    public boolean isLocalOnly() {
        return designRepository == null;
    }

    public boolean isRepositoryOnly() {
        return localFolderName == null;
    }

    public boolean isOpened() {
        return getRepository() == localRepository;
    }

    public void openVersion(String version) throws ProjectException {
        AProject designProject = new AProject(designRepository, designFolderName, version, false);

        if (localFolderName == null) {
            localFolderName = designProject.getName();
        }
        new AProject(localRepository, localFolderName, true).update(designProject, getUser());
        setRepository(localRepository);
        setFolderPath(localFolderName);
        setFolderStructure(true);

        setHistoryVersion(version);

        refresh();

        localRepository.getProjectState(localFolderName).clearModifyStatus();
    }

    // Is Opened for Editing by me? -- in LW + locked by me
    public boolean isOpenedForEditing() {
        return !isLocalOnly() && isLockedByMe() && !isRepositoryOnly();
    }

    @Override
    public boolean isModified() {
        return !isRepositoryOnly() && localRepository.getProjectState(localFolderName).isModified();

    }

    private void clearModifyStatus() {
        if (!isRepositoryOnly()) {
            localRepository.getProjectState(localFolderName).clearModifyStatus();
        }
    }

    @Override
    public void setHistoryVersion(String historyVersion) {
        super.setHistoryVersion(historyVersion);
        if (isOpened()) {
            localRepository.getProjectState(localFolderName).setProjectVersion(historyVersion);
        }
    }
}
