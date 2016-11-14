package org.openl.rules.repository.file;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.openl.rules.repository.api.FileData;
import org.openl.rules.repository.api.FileItem;
import org.openl.rules.repository.api.Listener;
import org.openl.rules.repository.api.Repository;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simple implementation of a file-system-based repository. This repository
 * does not support versioning.
 *
 * @author Yury Molchan
 */
public class FileRepository implements Repository {
    private final Logger log = LoggerFactory.getLogger(FileRepository.class);

    private File root;
    private int rootPathLength;

    public FileRepository(File root) {
        this.root = root;
    }

    public void initialize() throws IOException {
        root.mkdirs();
        if (!root.exists() || !root.isDirectory()) {
            throw new IOException("Failed to initialize the root directory: [" + root + "]");
        }
        String rootPath = root.getCanonicalPath();
        rootPathLength = rootPath.length() + 1;
    }

    @Override
    public List<FileData> list(String path) throws IOException {
        LinkedList<FileData> files = new LinkedList<FileData>();
        File directory = new File(root, path);
        listFiles(files, directory);
        return files;
    }

    @Override
    public FileData check(String name) throws IOException {
        File file = new File(root, name);
        if (file.exists()) {
            return getFileData(file);
        }
        return null;
    }

    @Override
    public FileItem read(String name) throws IOException {
        File file = new File(root, name);
        if (file.exists()) {
            FileData data = getFileData(file);
            FileInputStream stream = new FileInputStream(file);
            return new FileItem(data, stream);
        }
        return null;
    }

    @Override
    public FileData save(FileData data, InputStream stream) throws IOException {
        String name = data.getName();
        File file = new File(root, name);
        file.getParentFile().mkdirs();
        FileOutputStream output = new FileOutputStream(file);
        IOUtils.copyAndClose(stream, output);
        return getFileData(file);
    }

    @Override
    public boolean delete(String name) {
        File file = new File(root, name);
        boolean deleted = file.delete();
        // Delete empty parent folders
        while (!(file = file.getParentFile()).equals(root) && file.delete());
        return deleted;
    }

    @Override
    public FileData copy(String srcName, String destName) throws IOException {
        File srcFile = new File(root, srcName);
        File destFile = new File(root, destName);
        FileUtils.copy(srcFile, destFile);
        return getFileData(destFile);
    }

    @Override
    public FileData rename(String srcName, String destName) throws IOException {
        File srcFile = new File(root, srcName);
        File destFile = new File(root, destName);
        boolean renamed = srcFile.renameTo(destFile);
        if (!renamed) {
            throw new IOException("Impossible to rename the file from [" + srcFile + "] to [" + destFile + "]");
        }
        // Delete empty parent folders
        while (!(srcFile = srcFile.getParentFile()).equals(root) && srcFile.delete());
        return getFileData(destFile);
    }

    @Override
    public void setListener(Listener callback) {

    }

    @Override
    public List<FileData> listHistory(String name) throws IOException {
        File file = new File(root, name);
        try {
            if (file.exists()) {
                FileData data = getFileData(file);
                return Collections.singletonList(data);
            }
        } catch (Exception ex) {
            log.warn("The file cannot be resolved: [{}]", file, ex);
        }
        return Collections.emptyList();
    }

    @Override
    public FileData checkHistory(String name, String version) throws IOException {
        if (version == null) {
            return check(name);
        }
        return null;
    }

    @Override
    public FileItem readHistory(String name, String version) throws IOException {
        if (version == null) {
            return read(name);
        }
        return null;
    }

    @Override
    public boolean deleteHistory(String name, String version) {
        return version == null && delete(name);
    }

    @Override
    public FileData copyHistory(String srcName, String destName, String version) throws IOException {
        if (version == null) {
            return copy(srcName, destName);
        }
        throw new FileNotFoundException("File versions is not supported.");
    }

    private void listFiles(Collection<FileData> files, File directory) {
        File[] found = directory.listFiles();

        if (found != null) {
            for (File file : found) {
                if (file.isDirectory()) {
                    listFiles(files, file);
                } else {
                    try {
                        FileData data = getFileData(file);
                        files.add(data);
                    } catch (Exception ex) {
                        log.warn("The file cannot be resolved in the directory {}.", directory, ex);
                    }
                }
            }
        }
    }

    private FileData getFileData(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("File [" + file + "] does not exist.");
        }
        if (!file.isFile()) {
            throw new FileNotFoundException("File [" + file + "] is not a file.");
        }
        FileData data = new FileData();
        String canonicalPath = file.getCanonicalPath();
        String relativePath = canonicalPath.substring(rootPathLength);
        String converted = relativePath.replace('\\', '/');
        data.setName(converted);
        long timestamp = file.lastModified();
        Date date = new Date(timestamp);
        data.setModifiedAt(date);
        long size = file.length();
        data.setSize(size);
        return data;
    }
}
