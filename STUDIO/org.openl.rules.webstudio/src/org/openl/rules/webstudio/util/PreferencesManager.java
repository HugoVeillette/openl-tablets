package org.openl.rules.webstudio.util;

import java.io.File;
import java.io.IOException;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.openl.rules.webstudio.web.servlet.StartupListener;
import org.openl.spring.env.DynamicPropertySource;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum PreferencesManager {

    INSTANCE;

    public static final String WEBSTUDIO_WORKING_DIR_KEY = DynamicPropertySource.OPENL_HOME;
    private static final String WEBSTUDIO_MODE_KEY = "webstudio.mode";
    private static final String WEBSTUDIO_MODE_MAIN = "webstudio";
    private static final String WEBSTUDIO_MODE_INSTALLER = "installer";

    private final Logger log = LoggerFactory.getLogger(StartupListener.class);

    public void initWebStudioMode(String appName) {
        boolean configured = isAppConfigured(appName);
        writeValue(appName, WEBSTUDIO_MODE_KEY, configured ? WEBSTUDIO_MODE_MAIN : WEBSTUDIO_MODE_INSTALLER);
    }

    public void setInstallerMode(String appName) {
        writeValue(appName, WEBSTUDIO_MODE_KEY, WEBSTUDIO_MODE_INSTALLER);
    }

    public boolean isAppConfigured(String appName) {
        String configured = System.getProperty("webstudio.configured");
        if (configured != null) {
            return Boolean.parseBoolean(configured);
        }
        String homePath = readValue(appName, WEBSTUDIO_WORKING_DIR_KEY);
        return getMarkerFile(homePath).exists();
    }

    public void setWebStudioHomeDir(String appName, String workingDir) {
        writeValue(appName, WEBSTUDIO_WORKING_DIR_KEY, workingDir);
        setWebStudioHomeNotConfigured(appName, workingDir);
    }

    private void setWebStudioHomeNotConfigured(String appName, String homePath) {
        File configuredMarker = getMarkerFile(homePath);
        if (configuredMarker.exists()) {
            if (!configuredMarker.delete() && configuredMarker.exists()) {
                log.warn("Can't delete the file {}", configuredMarker.getPath());
            }
        }
        writeValue(appName, WEBSTUDIO_MODE_KEY, WEBSTUDIO_MODE_INSTALLER);
    }

    public void webStudioConfigured(String appName) {
        String homePath = readValue(appName, WEBSTUDIO_WORKING_DIR_KEY);
        File configuredMarker = getMarkerFile(homePath);
        try {
            if (!configuredMarker.exists()) {
                if (!configuredMarker.createNewFile()) {
                    log.debug("File {} exists already.", configuredMarker.getPath());
                }
            }
        } catch (IOException e) {
            log.error("cannot create configured file", e);
        }
        writeValue(appName, WEBSTUDIO_MODE_KEY, WEBSTUDIO_MODE_MAIN);
    }

    private String readValue(String appName, String key) {
        String applicationNodePath = getApplicationNode(appName);
        Preferences node = Preferences.userRoot().node(applicationNodePath);
        return node.get(key, null);
    }

    private void writeValue(String appName, String key, String value) {
        String applicationNodePath = getApplicationNode(appName);
        Preferences node = Preferences.userRoot().node(applicationNodePath);
        node.put(key, value);
        try {
            // guard against loss in case of abnormal termination of the VM
            // in case of normal VM termination, the flush method is not required
            node.flush();
        } catch (BackingStoreException e) {
            log.error("cannot save preferences value", e);
        }
    }

    private String getApplicationNode(String appName) {
        return StringUtils.isEmpty(appName) ? "openl" : "openl/" + appName;
    }

    private static File getMarkerFile(String homePath) {
        return new File(homePath, "webStudioConfigured.txt");
    }
}
