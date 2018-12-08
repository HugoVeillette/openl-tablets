package org.openl.classloader;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public abstract class OpenLBundleClassLoader extends OpenLClassLoader {

    protected Set<ClassLoader> bundleClassLoaders = new LinkedHashSet<ClassLoader>();
    
    OpenLBundleClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void addClassLoader(ClassLoader classLoader) {
        
        if (classLoader == null) {
            throw new IllegalArgumentException("Bundle class loader cannot be null");
        }
        
        if (classLoader == this) {
            throw new IllegalArgumentException("Bundle class loader cannot register himself");
        }

        if (classLoader instanceof OpenLBundleClassLoader && ((OpenLBundleClassLoader) classLoader).containsClassLoader(this)) {
            throw new IllegalArgumentException("Bundle class loader cannot register class loader containing himself");
        }
        
        bundleClassLoaders.add(classLoader);
    }
    
    protected Set<ClassLoader> getBundleClassLoaders() {
        return Collections.unmodifiableSet(bundleClassLoaders);
    }

    public boolean containsClassLoader(ClassLoader classLoader) {
        if (bundleClassLoaders.contains(classLoader)) {
            return true;
        }

        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            if (bundleClassLoader instanceof OpenLBundleClassLoader) {
                if (((OpenLBundleClassLoader) bundleClassLoader).containsClassLoader(classLoader)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public void close() throws IOException {
        super.close();
        for (ClassLoader classLoader : bundleClassLoaders) {
            ClassLoaderUtils.close(classLoader);
        }
    }
}
