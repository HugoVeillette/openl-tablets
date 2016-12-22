package org.openl.rules.repository;

import org.openl.config.ConfigurationManager;
import org.openl.config.ConfigurationManagerFactory;
import org.openl.rules.repository.api.Repository;
import org.openl.rules.repository.exceptions.RRepositoryException;
import org.openl.util.IOUtils;

import java.io.Closeable;
import java.util.HashMap;
import java.util.Map;

/**
 * Repository Factory Proxy.
 * <p/>
 * Takes actual factory description from <i>rules-production.properties</i>
 * file.
 */
public class ProductionRepositoryFactoryProxy {

    private ConfigurationManagerFactory configManagerFactory;

    private Map<String, Repository> factories = new HashMap<String, Repository>();

    public Repository getRepositoryInstance(String propertiesFileName) throws RRepositoryException {
        if (!factories.containsKey(propertiesFileName)) {
            synchronized (this) {
                if (!factories.containsKey(propertiesFileName)) {
                    factories.put(propertiesFileName, createFactory(propertiesFileName));
                }
            }
        }

        return factories.get(propertiesFileName);
    }

    public void releaseRepository(String propertiesFileName) throws RRepositoryException {
        synchronized (this) {
            Repository repository = factories.get(propertiesFileName);
            if (repository != null) {
                if (repository instanceof Closeable) {
                    // Close repo connection after validation
                    IOUtils.closeQuietly((Closeable) repository);
                }
                factories.remove(propertiesFileName);
            }
        }
    }

    public void destroy() throws RRepositoryException {
        synchronized (this) {
            for (Repository repository : factories.values()) {
                if (repository instanceof Closeable) {
                    // Close repo connection after validation
                    IOUtils.closeQuietly((Closeable) repository);
                }
            }
            factories.clear();
        }
    }

    public void setConfigManagerFactory(ConfigurationManagerFactory configManagerFactory) {
        this.configManagerFactory = configManagerFactory;
    }

    private Repository createFactory(String propertiesFileName) throws RRepositoryException {
        ConfigurationManager configurationManager = configManagerFactory.getConfigurationManager(propertiesFileName);
        Map<String, Object> properties = configurationManager.getProperties();

        return RepositoryFactoryInstatiator.newFactory(properties, false);
    }

}
