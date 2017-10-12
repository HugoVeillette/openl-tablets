package org.openl.rules.ruleservice.publish.lazy;

import java.util.Map;

import org.openl.CompiledOpenClass;
import org.openl.dependency.IDependencyManager;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.lang.xls.prebind.LazyMethodWrapper;
import org.openl.rules.method.ITablePropertiesMethod;
import org.openl.rules.method.TableUriMethod;
import org.openl.rules.project.model.Module;
import org.openl.rules.ruleservice.core.DeploymentDescription;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.OpenClassHelper;
import org.openl.vm.IRuntimeEnv;

/**
 * Lazy method that will return real object from dependency manager. Dependency
 * Manager is responsible for loading/unloading modules.
 * 
 * @author Marat Kamalov
 */
public abstract class LazyMethod extends LazyMember<IOpenMethod> implements IOpenMethod, TableUriMethod, LazyMethodWrapper {
    private String methodName;

    private Class<?>[] argTypes;

    private LazyMethod(String methodName,
            Class<?>[] argTypes,
            IOpenMethod original,
            IDependencyManager dependencyManager,
            ClassLoader classLoader,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        super(dependencyManager, executionMode, classLoader, original, externalParameters);
        this.methodName = methodName;
        this.argTypes = argTypes;
    }

    public static final LazyMethod getLazyMethod(final DeploymentDescription deployment,
            final Module module,
            Class<?>[] argTypes,
            IOpenMethod original,
            IDependencyManager dependencyManager,
            ClassLoader classLoader,
            boolean executionMode,
            Map<String, Object> externalParameters) {
        LazyMethod lazyMethod = null;

        if (original instanceof ITablePropertiesMethod) {
            lazyMethod = new TablePropertiesLazyMethod(original
                .getName(), argTypes, original, dependencyManager, classLoader, executionMode, externalParameters) {
                @Override
                public DeploymentDescription getDeployment() {
                    return deployment;
                }

                @Override
                public Module getModule() {
                    return module;
                }
            };
        } else {
            lazyMethod = new LazyMethod(original
                .getName(), argTypes, original, dependencyManager, classLoader, executionMode, externalParameters) {
                @Override
                public DeploymentDescription getDeployment() {
                    return deployment;
                }

                @Override
                public Module getModule() {
                    return module;
                }
            };
        }
        CompiledOpenClassCache.getInstance()
            .registerEvent(deployment, module.getName(), new LazyMemberEvent(lazyMethod));
        return lazyMethod;
    }

    /**
     * Compiles method declaring the member and returns it.
     *
     * @return member in compiled module.
     */
    protected IOpenMethod getMember() {
        IOpenMethod cachedMember = getCachedMember();
        if (cachedMember != null) {
            return cachedMember;
        }
        try {
            CompiledOpenClass compiledOpenClass = getCompiledOpenClassWithThrowErrorExceptionsIfAny();
            IOpenClass[] argOpenTypes = OpenClassHelper.getOpenClasses(compiledOpenClass.getOpenClass(), argTypes);
            IOpenMethod openMethod = compiledOpenClass.getOpenClass().getMethod(methodName, argOpenTypes);
            setCachedMember(openMethod);
            return openMethod;
        } catch (Exception e) {
            throw new OpenlNotCheckedException("Failed to load lazy method.", e);
        }
    }

    public IMethodSignature getSignature() {
        return getOriginal().getSignature();
    }

    @Override
    public boolean isConstructor() {
        return getOriginal().isConstructor();
    }

    @Override
    public IOpenMethod getCompiledMethod(IRuntimeEnv env) {
        return getMember();
    }

    @Override
    public String getTableUri() {
        if (getOriginal() instanceof TableUriMethod) {
            return ((TableUriMethod) getOriginal()).getTableUri();
        } else {
            throw new IllegalStateException("Implementation doesn't support methods other than ExecutableRulesMethod!");
        }
    }

    public IOpenMethod getMethod() {
        return this;
    }

    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return getMember().invoke(target, params, env);
    }

    private static abstract class TablePropertiesLazyMethod extends LazyMethod implements ITablePropertiesMethod {

        private TablePropertiesLazyMethod(String methodName,
                Class<?>[] argTypes,
                IOpenMethod original,
                IDependencyManager dependencyManager,
                ClassLoader classLoader,
                boolean executionMode,
                Map<String, Object> externalParameters) {
            super(methodName, argTypes, original, dependencyManager, classLoader, executionMode, externalParameters);
        }

        @Override
        public Map<String, Object> getProperties() {
            if (getOriginal() instanceof ITablePropertiesMethod) {
                return ((ITablePropertiesMethod) getOriginal()).getProperties();
            }
            throw new IllegalStateException("Original method must be the instance of ITablePropertiesMethod.");
        }

        @Override
        public ITableProperties getMethodProperties() {
            if (getOriginal() instanceof ITablePropertiesMethod) {
                return ((ITablePropertiesMethod) getOriginal()).getMethodProperties();
            }
            throw new IllegalStateException("Original method must be the instance of ITablePropertiesMethod.");
        }
    }

}
