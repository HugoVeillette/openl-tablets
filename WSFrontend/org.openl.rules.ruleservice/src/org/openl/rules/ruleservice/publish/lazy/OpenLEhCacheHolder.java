package org.openl.rules.ruleservice.publish.lazy;

import java.io.IOException;

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;

import net.sf.ehcache.Cache;
import net.sf.ehcache.CacheManager;

public class OpenLEhCacheHolder {

    private static final String LAZY_CACHE_NAME = "lazyModulesCache";
    private static final String CACHE_NAME = "modulesCache";
    private static final String OPENL_EHCACHE_FILE_NAME = "openl-ehcache.xml";

    private volatile Cache lazyModulesCache = null;
    private volatile Cache modulesCache = null;

    private static class OpenLEhCacheHolderHolder {
        public static final OpenLEhCacheHolder INSTANCE = new OpenLEhCacheHolder();
    }

    /**
     * Returns singleton OpenLEhCacheHolder
     * 
     * @return
     */
    public static OpenLEhCacheHolder getInstance() {
        return OpenLEhCacheHolderHolder.INSTANCE;
    }

    public Cache getModulesCache() {
        if (modulesCache == null) {
            synchronized (this) {
                if (modulesCache == null) {
                    try {
                        CacheManager cacheManager = getCacheManager();
                        modulesCache = cacheManager.getCache(CACHE_NAME);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return modulesCache;
    }
    
    public Cache getLazyModulesCache() {
        if (lazyModulesCache == null) {
            synchronized (this) {
                if (lazyModulesCache == null) {
                    try {
                        CacheManager cacheManager = getCacheManager();
                        lazyModulesCache = cacheManager.getCache(LAZY_CACHE_NAME);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
        return lazyModulesCache;
    }

    private CacheManager cacheManager = null;

    private synchronized CacheManager getCacheManager() throws IOException {
        if (cacheManager == null) {
            PathMatchingResourcePatternResolver prpr = new PathMatchingResourcePatternResolver();
            Resource[] resources = prpr
                .getResources(PathMatchingResourcePatternResolver.CLASSPATH_URL_PREFIX + OPENL_EHCACHE_FILE_NAME);
            if (resources == null || resources.length == 0) {
                resources = prpr.getResources(
                    PathMatchingResourcePatternResolver.CLASSPATH_ALL_URL_PREFIX + OPENL_EHCACHE_FILE_NAME);
            }
            if (resources == null || resources.length == 0) {
                throw new IllegalStateException(OPENL_EHCACHE_FILE_NAME + " isn't found!");
            }
            if (resources != null && resources.length > 1) {
                throw new IllegalStateException("Multiple " + OPENL_EHCACHE_FILE_NAME + " found in classpath!");
            }
            cacheManager = new CacheManager(resources[0].getURL());
        }
        return cacheManager;
    }

}
