package org.openl.rules.project.instantiation;

import java.io.File;
import java.util.*;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.variation.VariationInstantiationStrategyEnhancer;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDependencyDescriptor;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleProjectEngineFactory<T> implements ProjectEngineFactory<T> {

    private final Logger log = LoggerFactory.getLogger(SimpleProjectEngineFactory.class);

    private final boolean singleModuleMode;
    private final Map<String, Object> externalParameters;
    private final boolean provideRuntimeContext;
    private final boolean provideVariations;
    private final boolean executionMode;
    private final String module;
    private final ClassLoader classLoader;
    private final File workspace;
    private final File project;
    private final Class<?> interfaceClass;
    // lazy initialization.
    private Class<?> generatedInterfaceClass;
    private ProjectDescriptor projectDescriptor;

    public static class SimpleProjectEngineFactoryBuilder<T> {
        private String project;
        private String workspace;
        private ClassLoader classLoader;
        private String module;
        private boolean provideRuntimeContext = false;
        private boolean provideVariations = false;
        private Class<T> interfaceClass = null;
        private Map<String, Object> externalParameters = Collections.emptyMap();
        private boolean executionMode = true;

        public SimpleProjectEngineFactoryBuilder<T> setProject(String project) {
            if (project == null || project.isEmpty()) {
                throw new IllegalArgumentException("project cannot be null or empty!");
            }
            this.project = project;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setInterfaceClass(Class<T> interfaceClass) {
            this.interfaceClass = interfaceClass;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setExternalParameters(Map<String, Object> externalParameters) {
            if (externalParameters != null) {
                this.externalParameters = externalParameters;
            } else {
                this.externalParameters = Collections.emptyMap();
            }
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setExecutionMode(boolean executionMode) {
            this.executionMode = executionMode;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setProvideRuntimeContext(boolean provideRuntimeContext) {
            this.provideRuntimeContext = provideRuntimeContext;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setProvideVariations(boolean provideVariations) {
            this.provideVariations = provideVariations;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setClassLoader(ClassLoader classLoader) {
            this.classLoader = classLoader;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setModule(String module) {
            if (module == null || module.isEmpty()) {
                throw new IllegalArgumentException("module cannot be null or empty!");
            }
            this.module = module;
            return this;
        }

        public SimpleProjectEngineFactoryBuilder<T> setWorkspace(String workspace) {
            if (workspace == null || workspace.isEmpty()) {
                throw new IllegalArgumentException("workspace cannot be null or empty!");
            }
            this.workspace = workspace;
            return this;
        }

        public SimpleProjectEngineFactory<T> build() {
            if (project == null || project.isEmpty()) {
                throw new IllegalArgumentException("project cannot be null or empty!");
            }
            File projectFile = new File(project);
            File workspaceFile = workspace == null ? null : new File(workspace);
            return new SimpleProjectEngineFactory<>(projectFile,
                workspaceFile,
                classLoader,
                module,
                interfaceClass,
                externalParameters,
                provideRuntimeContext,
                provideVariations,
                executionMode);
        }

    }

    private SimpleProjectEngineFactory(File project,
            File workspace,
            ClassLoader classLoader,
            String module,
            Class<T> interfaceClass,
            Map<String, Object> externalParameters,
            boolean provideRuntimeContext,
            boolean provideVariations,
            boolean executionMode) {
        this.project = Objects.requireNonNull(project, "project arg cannot be null");
        if (workspace != null && !workspace.isDirectory()) {
            throw new IllegalArgumentException("workspace is not a directory with projects.");
        }
        this.workspace = workspace;
        this.classLoader = classLoader;
        this.interfaceClass = interfaceClass;
        this.externalParameters = externalParameters;
        this.provideRuntimeContext = provideRuntimeContext;
        this.provideVariations = provideVariations;
        this.executionMode = executionMode;
        this.module = module;
        this.singleModuleMode = module != null;
    }

    private RulesInstantiationStrategy rulesInstantiationStrategy = null;

    protected RulesInstantiationStrategy getStrategy(Collection<Module> modules, IDependencyManager dependencyManager) {
        if (rulesInstantiationStrategy == null) {
            switch (modules.size()) {
                case 0:
                    throw new IllegalStateException("There are no modules to instantiate.");
                case 1:
                    rulesInstantiationStrategy = RulesInstantiationStrategyFactory
                        .getStrategy(modules.iterator().next(), isExecutionMode(), dependencyManager, classLoader);
                    break;
                default:
                    rulesInstantiationStrategy = new SimpleMultiModuleInstantiationStrategy(modules,
                        dependencyManager,
                        classLoader,
                        isExecutionMode());
            }
        }
        return rulesInstantiationStrategy;
    }

    private List<ProjectDescriptor> getDependentProjects(ProjectDescriptor project,
            Collection<ProjectDescriptor> projectsInWorkspace) {
        List<ProjectDescriptor> projectDescriptors = new ArrayList<>();
        addDependentProjects(projectDescriptors, project, projectsInWorkspace);
        return projectDescriptors;
    }

    private void addDependentProjects(List<ProjectDescriptor> projectDescriptors,
            ProjectDescriptor project,
            Collection<ProjectDescriptor> projectsInWorkspace) {
        if (project.getDependencies() != null) {
            for (ProjectDependencyDescriptor dependencyDescriptor : project.getDependencies()) {
                boolean found = false;
                for (ProjectDescriptor projectDescriptor : projectsInWorkspace) {
                    if (dependencyDescriptor.getName().equals(projectDescriptor.getName())) {
                        projectDescriptors.add(projectDescriptor);
                        addDependentProjects(projectDescriptors, projectDescriptor, projectsInWorkspace);
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    log.warn("Dependency '{}' for project '{}' is not found.",
                        dependencyDescriptor.getName(),
                        project.getName());
                }
            }
        }
    }

    protected IDependencyManager buildDependencyManager() throws ProjectResolvingException {
        Collection<ProjectDescriptor> projectDescriptors = new ArrayList<>();
        ProjectResolver projectResolver = ProjectResolver.instance();
        ProjectDescriptor projectDescriptor = getProjectDescriptor();
        if (workspace != null) {
            File[] files = workspace.listFiles();
            List<ProjectDescriptor> projects;
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            try {
                if (classLoader != null) {
                    Thread.currentThread().setContextClassLoader(classLoader);
                }
                projects = projectResolver.resolve(files);
            } finally {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
            List<ProjectDescriptor> dependentProjects = getDependentProjects(projectDescriptor, projects);
            projectDescriptors.addAll(dependentProjects);
        }
        projectDescriptors.add(projectDescriptor);
        SimpleDependencyManager dependencyManager = new SimpleDependencyManager(projectDescriptors,
            classLoader,
            isSingleModuleMode(),
            isExecutionMode());
        dependencyManager.setExternalParameters(getExternalParameters());
        return dependencyManager;
    }

    private IDependencyManager dependencyManager = null;

    protected synchronized final IDependencyManager getDependencyManager() throws ProjectResolvingException {
        if (dependencyManager == null) {
            dependencyManager = buildDependencyManager();
        }
        return dependencyManager;
    }

    public boolean isExecutionMode() {
        return executionMode;
    }

    @Override
    public boolean isSingleModuleMode() {
        return singleModuleMode;
    }

    @Override
    public boolean isProvideRuntimeContext() {
        return provideRuntimeContext;
    }

    public boolean isProvideVariations() {
        return provideVariations;
    }

    @Override
    public Class<?> getInterfaceClass() throws RulesInstantiationException,
                                        ProjectResolvingException,
                                        ClassNotFoundException {
        if (interfaceClass != null) {
            return interfaceClass;
        }
        if (generatedInterfaceClass != null) {
            return generatedInterfaceClass;
        }
        log.info("Interface class is undefined for factory. Generated interface is used.");
        generatedInterfaceClass = getRulesInstantiationStrategy().getInstanceClass();
        return generatedInterfaceClass;
    }

    @Override
    public Map<String, Object> getExternalParameters() {
        return externalParameters;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T newInstance() throws RulesInstantiationException, ProjectResolvingException, ClassNotFoundException {
        return (T) getRulesInstantiationStrategy().instantiate();
    }

    protected final synchronized ProjectDescriptor getProjectDescriptor() throws ProjectResolvingException {
        if (this.projectDescriptor == null) {
            ProjectResolver projectResolver = ProjectResolver.instance();
            ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
            ProjectDescriptor pd;
            try {
                if (classLoader != null) {
                    Thread.currentThread().setContextClassLoader(classLoader);
                }
                pd = projectResolver.resolve(project);
            } finally {
                Thread.currentThread().setContextClassLoader(oldClassLoader);
            }
            if (pd == null) {
                throw new ProjectResolvingException(
                    "Failed to resolve project. Defined location is not a OpenL project.");
            }
            this.projectDescriptor = pd;
        }
        return this.projectDescriptor;
    }

    protected RulesInstantiationStrategy instantiationStrategy;

    protected final synchronized RulesInstantiationStrategy getRulesInstantiationStrategy() throws RulesInstantiationException,
                                                                                            ProjectResolvingException {
        if (rulesInstantiationStrategy == null) {
            RulesInstantiationStrategy instantiationStrategy = null;
            if (!isSingleModuleMode()) {
                instantiationStrategy = getStrategy(getProjectDescriptor().getModules(), getDependencyManager());
            } else {
                for (Module module : getProjectDescriptor().getModules()) {
                    if (module.getName().equals(this.module)) {
                        Collection<Module> modules = new ArrayList<>();
                        modules.add(module);
                        instantiationStrategy = getStrategy(modules, getDependencyManager());
                        break;
                    }
                }
                if (instantiationStrategy == null) {
                    throw new RulesInstantiationException("Module has not been found in project!");
                }
            }

            if (isProvideVariations()) {
                instantiationStrategy = new VariationInstantiationStrategyEnhancer(instantiationStrategy);
            }

            if (isProvideRuntimeContext()) {
                instantiationStrategy = new RuntimeContextInstantiationStrategyEnhancer(instantiationStrategy);
            }

            Map<String, Object> parameters = new HashMap<>(externalParameters);
            if (!isSingleModuleMode()) {
                parameters = ProjectExternalDependenciesHelper
                    .getExternalParamsWithProjectDependencies(externalParameters, getProjectDescriptor().getModules());
            }
            instantiationStrategy.setExternalParameters(parameters);
            try {
                if (interfaceClass != null) {
                    instantiationStrategy.setServiceClass(interfaceClass);
                }
            } catch (Exception ex) {
                throw new RulesInstantiationException(ex);
            }
            rulesInstantiationStrategy = instantiationStrategy;
        }
        return rulesInstantiationStrategy;
    }

    @Override
    public CompiledOpenClass getCompiledOpenClass() throws RulesInstantiationException,
                                                    ProjectResolvingException,
                                                    ClassNotFoundException {
        return getRulesInstantiationStrategy().compile();
    }

}
