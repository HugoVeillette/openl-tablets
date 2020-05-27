package org.openl.rules.ruleservice.publish.lazy;

import java.util.Collections;
import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.exception.OpenLCompilationException;
import org.openl.rules.lang.xls.prebind.IPrebindHandler;
import org.openl.rules.lang.xls.prebind.XlsLazyModuleOpenClass;
import org.openl.rules.project.dependencies.ProjectExternalDependenciesHelper;
import org.openl.rules.project.instantiation.RulesInstantiationStrategy;
import org.openl.rules.project.instantiation.RulesInstantiationStrategyFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.ruleservice.core.MaxThreadsForCompileSemaphore;
import org.openl.rules.ruleservice.core.RuleServiceDependencyManager;
import org.openl.types.IOpenMember;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Lazy IOpenMember that contains info about module where it was declared. When we try to do some operations with lazy
 * member it will compile module and wrap the compiled member.
 *
 * @author Marat Kamalov
 */
public abstract class LazyMember<T extends IOpenMember> {
    private final Logger log = LoggerFactory.getLogger(LazyMember.class);

    private final RuleServiceDependencyManager dependencyManager;

    /**
     * ClassLoader used in "lazy" compilation. It should be reused because it contains generated classes for
     * datatypes.(If we use different ClassLoaders we can get ClassCastException because generated classes for datatypes
     * have been loaded by different ClassLoaders).
     */
    private final ClassLoader classLoader;
    private volatile T cachedMember;

    LazyMember(RuleServiceDependencyManager dependencyManager,
               ClassLoader classLoader) {
        this.dependencyManager = dependencyManager;
        this.classLoader = classLoader;
    }

    protected abstract T initMember();

    public T getMember() {
        if (cachedMember != null) {
            return cachedMember;
        }
        cachedMember = initMember();
        return cachedMember;
    }

    void clearCachedMember() {
        cachedMember = null;
    }

    CompiledOpenClass getCompiledOpenClassWithThrowErrorExceptionsIfAny() throws Exception {
        CompiledOpenClass compiledOpenClass = getCompiledOpenClass();
        if (compiledOpenClass.hasErrors()) {
            compiledOpenClass.throwErrorExceptionsIfAny();
        }
        return compiledOpenClass;
    }

    private CompiledOpenClass getCompiledOpenClass() throws Exception {
        final Module module = getModule();
        DeploymentDescription deployment = getDeployment();
        CompiledOpenClass compiledOpenClass = CompiledOpenClassCache.getInstance().get(deployment, module.getName());
        if (compiledOpenClass != null) {
            return compiledOpenClass;
        }

        synchronized (getXlsLazyModuleOpenClass()) {
            compiledOpenClass = CompiledOpenClassCache.getInstance().get(deployment, module.getName());
            if (compiledOpenClass != null) {
                return compiledOpenClass;
            }
            try {
                return MaxThreadsForCompileSemaphore.getInstance().run(() -> {
                    CompiledOpenClass compiledOpenClass1 = null;
                    IPrebindHandler prebindHandler = LazyBinderMethodHandler.getPrebindHandler();
                    try {
                        LazyBinderMethodHandler.removePrebindHandler();
                        RulesInstantiationStrategy rulesInstantiationStrategy = RulesInstantiationStrategyFactory
                            .getStrategy(module, true, dependencyManager, classLoader);
                        rulesInstantiationStrategy.setServiceClass(EmptyInterface.class);// Prevent
                        Map<String, Object> parameters = ProjectExternalDependenciesHelper
                            .getExternalParamsWithProjectDependencies(dependencyManager.getExternalParameters(),
                                Collections.singleton(module));
                        rulesInstantiationStrategy.setExternalParameters(parameters);
                        compiledOpenClass1 = rulesInstantiationStrategy.compile();
                        CompiledOpenClassCache.getInstance()
                            .putToCache(deployment, module.getName(), compiledOpenClass1);
                        if (log.isDebugEnabled()) {
                            log.debug(
                                "CompiledOpenClass for deploymentName='{}', deploymentVersion='{}', dependencyName='{}' was stored to cache.",
                                deployment.getName(),
                                deployment.getVersion().getVersionName(),
                                module.getName());
                        }
                        return compiledOpenClass1;
                    } catch (Exception ex) {
                        log.error("Failed to load dependency '{}'.", module.getName(), ex);
                        return compiledOpenClass1;
                    } finally {
                        LazyBinderMethodHandler.setPrebindHandler(prebindHandler);
                    }
                });
            } catch (OpenLCompilationException e) {
                throw e;
            } catch (InterruptedException e) {
                throw new OpenLCompilationException("Interrupted exception.", e);
            } catch (Exception e) {
                throw new OpenLCompilationException("Failed to compile.", e);
            }
        }
    }

    public abstract XlsLazyModuleOpenClass getXlsLazyModuleOpenClass();

    /**
     * @return Module containing current member.
     */
    public abstract Module getModule();

    /**
     * @return Deployment containing current module.
     */
    public abstract DeploymentDescription getDeployment();

    interface EmptyInterface {
    }
}
