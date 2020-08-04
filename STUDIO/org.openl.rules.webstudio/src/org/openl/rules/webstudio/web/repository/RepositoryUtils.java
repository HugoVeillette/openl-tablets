package org.openl.rules.webstudio.web.repository;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Comparator;
import java.util.List;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.openl.rules.project.abstraction.AProjectArtefact;
import org.openl.rules.repository.api.BranchRepository;
import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.FolderRepository;
import org.openl.rules.webstudio.web.repository.deployment.DeploymentOutputStream;
import org.openl.rules.webstudio.web.servlet.RulesUserSession;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.rules.workspace.uw.UserWorkspace;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Repository Utilities
 *
 * @author Aleh Bykhavets
 */
public final class RepositoryUtils {
    public static final Comparator<AProjectArtefact> ARTEFACT_COMPARATOR = new Comparator<AProjectArtefact>() {
        @Override
        public int compare(AProjectArtefact o1, AProjectArtefact o2) {
            if (o1.isFolder() == o2.isFolder()) {
                return o1.getName().compareTo(o2.getName());
            } else {
                return o1.isFolder() ? -1 : 1;
            }
        }
    };

    private RepositoryUtils() {
    }

    /**
     * @deprecated
     */
    @Deprecated
    public static RulesUserSession getRulesUserSession() {
        return (RulesUserSession) WebStudioUtils.getExternalContext().getSessionMap().get(Constants.RULES_USER_SESSION);
    }

    /**
     * @return user's workspace or <code>null</code>
     * @deprecated
     */
    @Deprecated
    public static UserWorkspace getWorkspace() {
        final Logger log = LoggerFactory.getLogger(RepositoryUtils.class);
        try {
            return getRulesUserSession().getUserWorkspace();
        } catch (Exception e) {
            log.error("Error obtaining user workspace", e);
        }
        return null;
    }

    public static String getTreeNodeId(String name) {
        if (StringUtils.isNotBlank(name)) {
            // FIXME name.hashCode() can produce collisions. Not good for id.
            return String.valueOf(name.hashCode());
        }
        return null;
    }

    public static void archive(FolderRepository folderRepository,
                               String rulesPath,
                               String projectName,
                               String version,
                               OutputStream out,
                               Manifest manifest) throws IOException {
        ZipOutputStream zipOutputStream = null;
        try {
            zipOutputStream = new DeploymentOutputStream(out, manifest);

            String projectPath = rulesPath + projectName + "/";
            folderRepository = getRepositoryForVersion(folderRepository, rulesPath, projectName, version);
            List<FileData> files = folderRepository.listFiles(projectPath, version);

            for (FileData file : files) {
                String internalPath = file.getName().substring(projectPath.length());
                if (JarFile.MANIFEST_NAME.equals(internalPath)) {
                    //skip old manifest
                    continue;
                }
                zipOutputStream.putNextEntry(new ZipEntry(internalPath));

                FileItem fileItem = folderRepository.readHistory(file.getName(), file.getVersion());
                try (InputStream content = fileItem.getStream()) {
                    IOUtils.copy(content, zipOutputStream);
                }

                zipOutputStream.closeEntry();
            }
            zipOutputStream.finish();
        } finally {
            IOUtils.closeQuietly(zipOutputStream);
        }
    }

    /**
     * Includes generated manifest to the first position of deployed archive. The old manifest file will be skipped
     * @param in project input stream
     * @param out target output stream
     * @param manifest manifest file to include
     * @throws IOException
     */
    public static void includeManifestAndRepackArchive(InputStream in, OutputStream out, Manifest manifest) throws IOException {
        try (ZipInputStream zipIn = new ZipInputStream(in);
             ZipOutputStream zipOut = new DeploymentOutputStream(out, manifest)) {
            byte[] buffer = new byte[64 * 1024];
            ZipEntry entry = zipIn.getNextEntry();
            while ( entry != null) {
                if (!entry.isDirectory() && !JarFile.MANIFEST_NAME.equals(entry.getName())) {
                    zipOut.putNextEntry(entry);
                    IOUtils.copy(zipIn, zipOut, buffer);
                    zipOut.closeEntry();
                }
                entry = zipIn.getNextEntry();
            }
            zipOut.finish();
        }
    }

    static FolderRepository getRepositoryForVersion(FolderRepository folderRepo,
            String rulesPath,
            String projectName,
            String version) throws IOException {
        String srcProjectPath = rulesPath + projectName + "/";
        if (folderRepo.supports().branches()) {
            BranchRepository branchRepository = (BranchRepository) folderRepo;
            if (branchRepository.checkHistory(srcProjectPath, version) != null) {
                // Use main branch
                return folderRepo;
            } else {
                // Use secondary branch
                List<String> branches = branchRepository.getBranches(projectName);
                for (String branch : branches) {
                    BranchRepository secondaryBranch = branchRepository.forBranch(branch);
                    FileData fileData = secondaryBranch.checkHistory(srcProjectPath, version);
                    if (fileData != null) {
                        return (FolderRepository) secondaryBranch;
                    }
                }

                return folderRepo;
            }
        } else {
            return folderRepo;
        }
    }
}
