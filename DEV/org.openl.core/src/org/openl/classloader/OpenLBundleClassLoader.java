package org.openl.classloader;

import java.io.InputStream;
import java.net.URL;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.openl.util.ClassUtils;

/**
 * ClassLoader that have bundle classLoaders. When loading any class, at first tries to find it in bundle classLoaders
 * if can`t tries to find it in his parent.
 */
public class OpenLBundleClassLoader extends OpenLClassLoader {

    private final Set<ClassLoader> bundleClassLoaders = new LinkedHashSet<>();

    private final Map<String, byte[]> generatedClasses = new ConcurrentHashMap<>();

    public OpenLBundleClassLoader(ClassLoader parent) {
        super(new URL[0], parent);
    }

    public OpenLBundleClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public void addClassLoader(ClassLoader classLoader) {
        Objects.requireNonNull(classLoader, "Bundle class loader cannot be null");

        if (classLoader == this) {
            throw new IllegalArgumentException("Bundle class loader cannot register himself");
        }

        if (classLoader instanceof OpenLBundleClassLoader && ((OpenLBundleClassLoader) classLoader)
            .containsClassLoader(this)) {
            throw new IllegalArgumentException("Bundle class loader cannot register class loader containing himself");
        }

        bundleClassLoaders.add(classLoader);
    }

    public void addGeneratedClass(String name, byte[] byteCode) {
        if (generatedClasses.putIfAbsent(name, byteCode) != null) {
            throw new OpenLGeneratedClassAlreadyDefinedException(
                String.format("Byte code for class '%s' is already defined.", name));
        }

    }

    public boolean containsClassLoader(ClassLoader classLoader) {
        if (bundleClassLoaders.contains(classLoader)) {
            return true;
        }

        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            if (bundleClassLoader instanceof OpenLBundleClassLoader && ((OpenLBundleClassLoader) bundleClassLoader)
                .containsClassLoader(classLoader)) {
                return true;
            }
        }

        return false;
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        Set<ClassLoader> c = Collections.newSetFromMap(new IdentityHashMap<>());
        c.add(this);
        return loadClass(name, c);
    }

    protected Class<?> loadClass(String name, Set<ClassLoader> c) throws ClassNotFoundException {
        Class<?> clazz = findClassInBundles(name, c);

        if (clazz != null) {
            return clazz;
        }
        try {
            return super.loadClass(name);
        } catch (ClassNotFoundException e) {
            byte[] byteCode = generatedClasses.get(name);
            if (byteCode != null) {
                try {
                    return ClassUtils.defineClass(name, byteCode, this);
                } catch (Exception e1) {
                    throw e;
                }
            }
            throw e;
        }
    }

    private Class<?> findClassInBundles(String name, Set<ClassLoader> c) {

        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            if (c.contains(bundleClassLoader)) {
                continue;
            }
            c.add(bundleClassLoader);
            try {
                // if current class loader contains appropriate class - it will
                // be returned as a result
                //
                Class<?> clazz;
                if (bundleClassLoader instanceof OpenLBundleClassLoader && bundleClassLoader.getParent() == this) {
                    OpenLBundleClassLoader sbc = (OpenLBundleClassLoader) bundleClassLoader;
                    clazz = sbc.findLoadedClass(name);
                    if (clazz == null) {
                        clazz = sbc.findClassInBundles(name, c);
                    }
                } else {
                    if (bundleClassLoader instanceof OpenLBundleClassLoader) {
                        clazz = ((OpenLBundleClassLoader) bundleClassLoader).loadClass(name, c);
                    } else {
                        clazz = bundleClassLoader.loadClass(name);
                    }
                }
                if (clazz != null) {
                    return clazz;
                }
            } catch (ClassNotFoundException ignored) {
            }
        }

        return null;
    }

    private URL findResourceInBundleClassLoader(String name) {
        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            URL url;
            if (bundleClassLoader instanceof OpenLBundleClassLoader && bundleClassLoader.getParent() == this) {
                OpenLBundleClassLoader sbcl = (OpenLBundleClassLoader) bundleClassLoader;
                url = sbcl.findResourceInBundleClassLoader(name);
            } else {
                url = bundleClassLoader.getResource(name);
            }
            if (url != null) {
                return url;
            }
        }
        return null;
    }

    private InputStream findResourceAsStreamInBundleClassLoader(String name) {
        for (ClassLoader bundleClassLoader : bundleClassLoaders) {
            InputStream inputStream = null;
            if (bundleClassLoader instanceof OpenLBundleClassLoader && bundleClassLoader.getParent() == this) {
                OpenLBundleClassLoader sbcl = (OpenLBundleClassLoader) bundleClassLoader;
                inputStream = sbcl.findResourceAsStreamInBundleClassLoader(name);
            } else {
                inputStream = bundleClassLoader.getResourceAsStream(name);
            }
            if (inputStream != null) {
                return inputStream;
            }
        }
        return null;
    }

    @Override
    public URL getResource(String name) {
        URL url = findResourceInBundleClassLoader(name);
        if (url != null) {
            return url;
        }
        return super.getResource(name);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        InputStream inputStream = findResourceAsStreamInBundleClassLoader(name);
        if (inputStream != null) {
            return inputStream;
        }
        return super.getResourceAsStream(name);
    }

}
