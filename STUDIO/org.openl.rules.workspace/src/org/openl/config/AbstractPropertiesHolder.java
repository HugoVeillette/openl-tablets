package org.openl.config;

import org.openl.rules.repository.config.PassCoder;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.PropertyResolver;

public abstract class AbstractPropertiesHolder implements PropertiesHolder {
    private static final String REPO_PASS_KEY = "repository.encode.decode.key";
    protected final PropertyResolver propertyResolver;
    private final Logger log = LoggerFactory.getLogger(InMemoryProperties.class);

    AbstractPropertiesHolder(PropertyResolver propertyResolver) {
        this.propertyResolver = propertyResolver;
    }

    @Override
    public PropertyResolver getPropertyResolver() {
        return propertyResolver;
    }

    @Override
    public String getPassword(String key) {
        try {
            String repoPassKey = getRepoPassKey();
            String pass = getProperty(key);
            return StringUtils.isEmpty(repoPassKey) ? pass : PassCoder.decode(pass, repoPassKey);
        } catch (Exception e) {
            log.error("Error when getting password property: {}", key, e);
            return "";
        }
    }

    @Override
    public void setPassword(String key, String pass) {
        try {
            String repoPassKey = getRepoPassKey();
            setProperty(key, StringUtils.isEmpty(repoPassKey) ? pass : PassCoder.encode(pass, repoPassKey));
        } catch (Exception e) {
            Logger log = LoggerFactory.getLogger(AbstractPropertiesHolder.class);
            log.error("Error when setting password property: {}", key, e);
        }
    }

    private String getRepoPassKey() {
        String passKey = getProperty(REPO_PASS_KEY);
        return passKey != null ? StringUtils.trimToEmpty(passKey) : "";
    }

    @Override
    public void revertProperties(String... keys) {
        for (String key : keys) {
            revertProperty(key);
        }
    }
}
