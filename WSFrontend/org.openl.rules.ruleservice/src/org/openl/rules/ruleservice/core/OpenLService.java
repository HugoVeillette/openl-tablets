package org.openl.rules.ruleservice.core;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.openl.classloader.ClassLoaderUtils;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.project.model.Module;
import org.openl.types.IOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * Class designed for storing settings for service configuration and compiled service bean.
 * RuleServiceOpenLServiceInstantiationFactory is designed for build OpenLService instances.
 * 
 * @author Marat Kamalov
 * 
 */
public final class OpenLService {
    /**
     * Unique for service.
     */
    private String name;
    private String url;
    private String serviceClassName;
    private String rmiServiceClassName;
    private Class<?> serviceClass;
    private Class<?> rmiServiceClass;
    private Object serviceBean;
    private IOpenClass openClass;
    private boolean provideRuntimeContext = false;
    private boolean provideVariations = false;
    private Collection<Module> modules;
    private Set<String> publishers;
    private ClassLoader classLoader;

    /**
     * Not full constructor, by default variations is not supported.
     * 
     * @param name service name
     * @param url url
     * @param serviceClassName class name for service
     * @param provideRuntimeContext define is runtime context should be used
     * @param modules a list of modules for load
     */
    OpenLService(String name,
            String url,
            String serviceClassName,
            String rmiServiceClassName,
            boolean provideRuntimeContext,
            Set<String> publishers,
            Collection<Module> modules,
            ClassLoader classLoader) {
        this(name,
            url,
            serviceClassName,
            rmiServiceClassName,
            provideRuntimeContext,
            false,
            publishers,
            modules,
            classLoader);
    }

    OpenLService(String name,
            String url,
            String serviceClassName,
            String rmiServiceClassName,
            boolean provideRuntimeContext,
            Collection<Module> modules,
            ClassLoader classLoader) {
        this(name,
            url,
            serviceClassName,
            rmiServiceClassName,
            provideRuntimeContext,
            false,
            null,
            modules,
            classLoader);
    }

    OpenLService(String name,
            String url,
            String serviceClassName,
            String rmiServiceClassName,
            boolean provideRuntimeContext,
            boolean provideVariations,
            Collection<Module> modules,
            ClassLoader classLoader) {
        this(name,
            url,
            serviceClassName,
            rmiServiceClassName,
            provideRuntimeContext,
            provideVariations,
            null,
            modules,
            classLoader);
    }

    /**
     * Returns service classloader
     * 
     * @return classLoader
     */
    public ClassLoader getClassLoader() {
        return classLoader;
    }

    /**
     * Main constructor.
     * 
     * @param name service name
     * @param url url
     * @param serviceClassName class name for service
     * @param provideRuntimeContext define is runtime context should be used
     * @param provideVariations define is variations should be supported
     * @param modules a list of modules for load
     */
    OpenLService(String name,
            String url,
            String serviceClassName,
            String rmiServiceClassName,
            boolean provideRuntimeContext,
            boolean provideVariations,
            Set<String> publishers,
            Collection<Module> modules,
            ClassLoader classLoader) {
        if (name == null) {
            throw new IllegalArgumentException("name arg must not be null.");
        }
        this.name = name;
        this.url = url;
        if (modules != null) {
            this.modules = Collections.unmodifiableCollection(modules);
        } else {
            this.modules = Collections.emptyList();
        }
        this.serviceClassName = serviceClassName;
        this.rmiServiceClassName = rmiServiceClassName;
        this.provideRuntimeContext = provideRuntimeContext;
        this.provideVariations = provideVariations;
        if (publishers != null) {
            this.publishers = Collections.unmodifiableSet(publishers);
        } else {
            this.publishers = Collections.emptySet();
        }
        this.classLoader = classLoader;
    }

    private OpenLService(OpenLServiceBuilder builder) {
        this(builder.name,
            builder.url,
            builder.serviceClassName,
            builder.rmiServiceClassName,
            builder.provideRuntimeContext,
            builder.provideVariations,
            builder.publishers,
            builder.modules,
            builder.classLoader);
    }

    /**
     * Returns service name.
     * 
     * @return service name
     */
    public String getName() {
        return name;
    }

    /**
     * Returns service URL.
     * 
     * @return service URL
     */
    public String getUrl() {
        return url;
    }

    /**
     * Returns service publishers.
     * 
     * @return service publishers
     */
    public Collection<String> getPublishers() {
        if (publishers == null)
            return Collections.emptyList();
        return publishers;
    }

    /**
     * Returns unmodifiable collection of modules.
     * 
     * @return a collection of modules
     */
    public Collection<Module> getModules() {
        if (modules == null)
            return Collections.emptyList();
        return modules;
    }

    /**
     * Returns a class name for service.
     * 
     * @return
     */
    public String getServiceClassName() {
        return serviceClassName;
    }

    void setServiceClassName(String serviceClassName) {
        this.serviceClassName = serviceClassName;
    }

    /**
     * Returns a rmi class name for service.
     * 
     * @return
     */
    public String getRmiServiceClassName() {
        return rmiServiceClassName;
    }

    void setRmiServiceClassName(String rmiServiceClassName) {
        this.rmiServiceClassName = rmiServiceClassName;
    }

    /**
     * Return provideRuntimeContext value. This value is define that service methods first argument is
     * IRulesRuntimeContext.
     * 
     * @return isProvideRuntimeContext
     */
    public boolean isProvideRuntimeContext() {
        return provideRuntimeContext;
    }

    /**
     * This flag defines whether variations will be supported or not.
     * 
     * @return <code>true</code> if variations should be injected in service class, and <code>false</code> otherwise.
     */
    public boolean isProvideVariations() {
        return provideVariations;
    }

    /**
     * Returns service class.
     * 
     * @return
     */
    public Class<?> getServiceClass() {
        return serviceClass;
    }

    /**
     * Returns rmi service class.
     * 
     * @return
     */
    public Class<?> getRmiServiceClass() {
        return rmiServiceClass;
    }

    void setClassLoader(ClassLoader classLoader) {
        this.classLoader = classLoader;
    }

    void setServiceClass(Class<?> serviceClass) {
        this.serviceClass = serviceClass;
    }

    void setRmiServiceClass(Class<?> rmiServiceClass) {
        this.rmiServiceClass = rmiServiceClass;
    }

    public Object getServiceBean() {
        return serviceBean;
    }

    void setServiceBean(Object serviceBean) {
        this.serviceBean = serviceBean;
    }

    public void setOpenClass(IOpenClass openClass) {
        this.openClass = openClass;
    }

    public IOpenClass getOpenClass() {
        return openClass;
    }

    /** {@inheritDoc} */
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    /** {@inheritDoc} */
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        OpenLService other = (OpenLService) obj;
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    /**
     * Unregister ClassLoaders of this service.
     */
    public void destroy() {
        if (getClassLoader() != null) {
            ClassLoader classLoader = getClassLoader();
            while (classLoader instanceof SimpleBundleClassLoader) {
                JavaOpenClass.resetClassloader(classLoader);
                String2DataConvertorFactory.unregisterClassLoader(classLoader);
                ClassLoaderUtils.close(classLoader);
                classLoader = classLoader.getParent();
            }
        }
    }

    /**
     * OpenLService builder.
     * 
     * @author Marat Kamalov
     * 
     */
    public static class OpenLServiceBuilder {
        private String name;
        private String url;
        private String serviceClassName;
        private String rmiServiceClassName;
        private boolean provideRuntimeContext = false;
        private boolean provideVariations = false;
        private Collection<Module> modules;
        private Set<String> publishers;
        private ClassLoader classLoader;

        public OpenLServiceBuilder setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public OpenLServiceBuilder setPublishers(Set<String> publishers) {
            if (publishers == null) {
                this.publishers = new HashSet<String>(0);
            } else {
                this.publishers = publishers;
            }
            return this;
        }

        public OpenLServiceBuilder addPublishers(Set<String> publishers) {
            if (this.publishers == null) {
                this.publishers = new HashSet<String>();
            }
            if (publishers != null) {
                this.publishers.addAll(publishers);
            }
            return this;
        }

        public OpenLServiceBuilder addPublisher(String publisher) {
            if (this.publishers == null) {
                this.publishers = new HashSet<String>();
            }
            if (publisher != null) {
                this.publishers.add(publisher);
            }
            return this;
        }

        /**
         * Sets name to the builder.
         * 
         * @param name
         * @return
         */
        public OpenLServiceBuilder setName(String name) {
            if (name == null) {
                throw new IllegalArgumentException("name arg must not be null.");
            }
            this.name = name;
            return this;
        }

        /**
         * Sets class name to the builder.
         * 
         * @param serviceClassName
         * @return
         */
        public OpenLServiceBuilder setServiceClassName(String serviceClassName) {
            this.serviceClassName = serviceClassName;
            return this;
        }

        /**
         * Sets RMI class name to the builder.
         * 
         * @param serviceClassName
         * @return
         */
        public OpenLServiceBuilder setRmiServiceClassName(String rmiServiceClassName) {
            this.rmiServiceClassName = rmiServiceClassName;
            return this;
        }

        /**
         * Sets provideRuntimeContext to the builder.
         * 
         * @param provideRuntimeContext
         * @return
         */
        public OpenLServiceBuilder setProvideRuntimeContext(boolean provideRuntimeContext) {
            this.provideRuntimeContext = provideRuntimeContext;
            return this;
        }

        /**
         * Sets provideVariations flag to the builder. (Optional)
         * 
         * @param provideVariations
         * @return
         */
        public OpenLServiceBuilder setProvideVariations(boolean provideVariations) {
            this.provideVariations = provideVariations;
            return this;
        }

        /**
         * Sets a new set of modules to the builder.
         * 
         * @param modules
         * @return
         */
        public OpenLServiceBuilder setModules(Collection<Module> modules) {
            if (modules == null) {
                this.modules = new ArrayList<Module>(0);
            } else {
                this.modules = new ArrayList<Module>(modules);
            }
            return this;
        }

        /**
         * Add modules to the builder.
         * 
         * @param modules
         * @return
         */
        public OpenLServiceBuilder addModules(Collection<Module> modules) {
            if (this.modules == null) {
                this.modules = new ArrayList<Module>();
            }
            this.modules.addAll(modules);
            return this;
        }

        /**
         * Adds module to the builder.
         * 
         * @param module
         * @return
         */
        public OpenLServiceBuilder addModule(Module module) {
            if (this.modules == null) {
                this.modules = new ArrayList<Module>();
            }
            if (module != null) {
                this.modules.add(module);
            }
            return this;
        }

        /**
         * Sets url to the builder.
         * 
         * @param url
         * @return
         */
        public OpenLServiceBuilder setUrl(String url) {
            this.url = url;
            return this;
        }

        /**
         * Builds OpenLService.
         * 
         * @return
         */
        public OpenLService build() {
            if (name == null) {
                throw new IllegalStateException("Field 'name' is required for building ServiceDescription.");
            }
            return new OpenLService(this);
        }
    }
}
