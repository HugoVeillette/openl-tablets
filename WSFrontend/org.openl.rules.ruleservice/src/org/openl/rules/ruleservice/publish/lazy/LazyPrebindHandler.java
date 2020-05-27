package org.openl.rules.ruleservice.publish.lazy;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.lang.xls.prebind.XlsLazyModuleOpenClass;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.RuleServiceDependencyManager;
import org.openl.rules.ruleservice.core.RuleServiceOpenLCompilationException;
import org.openl.rules.ruleservice.publish.lazy.wrapper.LazyWrapperLogic;
import org.openl.rules.table.properties.DimensionPropertiesMethodKey;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMember;
import org.openl.types.IOpenMethod;
import org.openl.types.java.OpenClassHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class LazyPrebindHandler implements IPrebindHandler {

    private static final Logger LOG = LoggerFactory.getLogger(LazyPrebindHandler.class);
    private final Collection<Module> modules;
    private final RuleServiceDependencyManager dependencyManager;
    private final ClassLoader classLoader;
    private final DeploymentDescription deployment;

    LazyPrebindHandler(Collection<Module> modules,
            RuleServiceDependencyManager dependencyManager,
            ClassLoader classLoader,
            DeploymentDescription deployment) {
        this.modules = modules;
        this.dependencyManager = dependencyManager;
        this.classLoader = classLoader;
        this.deployment = deployment;
    }

    @Override
    public IOpenMethod processPrebindMethod(final IOpenMethod method) {
        final Module module = getModuleForMember(method, modules);
        Class<?>[] argTypes = new Class<?>[method.getSignature().getNumberOfParameters()];
        for (int i = 0; i < argTypes.length; i++) {
            argTypes[i] = method.getSignature().getParameterType(i).getInstanceClass();
        }
        final Map<String, Object> dimensionProperties = (method instanceof ITableProperties) ? PropertiesHelper
                .getTableProperties(method)
                .getAllDimensionalProperties() : null;
        final LazyMember<IOpenMethod> lazyMethod = new LazyMember<IOpenMethod>(dependencyManager, getClassLoader()) {


            protected IOpenMethod initMember() {
                IOpenMethod openMethod;
                try {
                    CompiledOpenClass compiledOpenClass = getCompiledOpenClassWithThrowErrorExceptionsIfAny();
                    openMethod = OpenClassHelper
                            .findRulesMethod(compiledOpenClass.getOpenClass(), method.getName(), argTypes);
                    if (openMethod instanceof OpenMethodDispatcher && dimensionProperties != null) {
                        OpenMethodDispatcher openMethodDispatcher = (OpenMethodDispatcher) openMethod;
                        for (IOpenMethod candidate : openMethodDispatcher.getCandidates()) {
                            if (candidate instanceof ITableProperties) {
                                Map<String, Object> candidateDimensionProperties = PropertiesHelper
                                        .getTableProperties(candidate)
                                        .getAllDimensionalProperties();
                                if (DimensionPropertiesMethodKey.compareMethodDimensionProperties(dimensionProperties,
                                        candidateDimensionProperties)) {
                                    openMethod = candidate;
                                    break;
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    throw new RuleServiceOpenLCompilationException("Failed to load lazy method.", e);
                }
                return openMethod;
            }

            @Override
            public DeploymentDescription getDeployment() {
                return deployment;
            }

            @Override
            public Module getModule() {
                return module;
            }

            @Override
            public XlsLazyModuleOpenClass getXlsLazyModuleOpenClass() {
                return (XlsLazyModuleOpenClass) method.getDeclaringClass();
            }
        };
        CompiledOpenClassCache.getInstance()
            .registerEvent(deployment, module.getName(), new LazyMemberEvent(lazyMethod));
        return LazyWrapperLogic.wrapMethod(lazyMethod, method);
    }

    @Override
    public IOpenField processPrebindField(final IOpenField field) {
        final Module module = getModuleForMember(field, modules);
        final LazyMember<IOpenField> lazyField = new LazyMember<IOpenField>(dependencyManager, getClassLoader()) {

            protected IOpenField initMember() {
                try {
                    CompiledOpenClass compiledOpenClass = getCompiledOpenClassWithThrowErrorExceptionsIfAny();
                    return compiledOpenClass.getOpenClass().getField(field.getName());
                } catch (Exception e) {
                    throw new RuleServiceOpenLCompilationException("Failed to load a lazy field.", e);
                }
            }

            @Override
            public DeploymentDescription getDeployment() {
                return deployment;
            }

            @Override
            public Module getModule() {
                return module;
            }

            @Override
            public XlsLazyModuleOpenClass getXlsLazyModuleOpenClass() {
                return (XlsLazyModuleOpenClass) field.getDeclaringClass();
            }
        };
        CompiledOpenClassCache.getInstance()
            .registerEvent(deployment, module.getName(), new LazyMemberEvent(lazyField));
        return LazyWrapperLogic.wrapField(lazyField, field);
    }

    private ClassLoader getClassLoader() {
        return classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader;
    }

    private static Module getModuleForMember(IOpenMember member, Collection<Module> modules) {
        String sourceUrl = member.getDeclaringClass().getMetaInfo().getSourceUrl();
        Module module = getModuleForSourceUrl(sourceUrl, modules);
        if (module != null) {
            return module;
        }
        throw new OpenlNotCheckedException("Module is not found. This shoud not happen.");
    }

    private static Module getModuleForSourceUrl(String sourceUrl, Collection<Module> modules) {
        if (modules.size() == 1) {
            return modules.iterator().next();
        }
        for (Module module : modules) {
            String modulePath = module.getRulesRootPath().getPath();
            try {
                if (Paths.get(sourceUrl)
                    .normalize()
                    .equals(Paths.get(new File(modulePath).getCanonicalFile().toURI().toURL().toExternalForm())
                        .normalize())) {
                    return module;
                }
            } catch (Exception e) {
                LOG.warn("Failed to build url for module '{}' with path: {}", module.getName(), modulePath, e);
            }
        }
        return null;
    }
}
