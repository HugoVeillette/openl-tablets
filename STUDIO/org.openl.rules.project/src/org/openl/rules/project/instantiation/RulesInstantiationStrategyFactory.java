package org.openl.rules.project.instantiation;

import org.openl.dependency.IDependencyManager;
import org.openl.rules.extension.instantiation.ExtensionInstantiationStrategyFactory;
import org.openl.rules.project.model.Extension;
import org.openl.rules.project.model.Module;

public final class RulesInstantiationStrategyFactory {
    private RulesInstantiationStrategyFactory() {
    }

    /**
     * @return {@link SingleModuleInstantiationStrategy} instance that will compile
     *         {@link Module}
     */
    public static SingleModuleInstantiationStrategy getStrategy(Module moduleInfo, boolean executionMode) {
        return getStrategy(moduleInfo, executionMode, null);
    }

    public static SingleModuleInstantiationStrategy getStrategy(Module moduleInfo, boolean executionMode, IDependencyManager dependencyManager) {
        return getStrategy(moduleInfo, executionMode, dependencyManager, null);
    }

    public static SingleModuleInstantiationStrategy getStrategy(Module moduleInfo, boolean executionMode, IDependencyManager dependencyManager, ClassLoader classLoader) {
        Extension extension = moduleInfo.getExtension();
        if (extension != null) {
            return ExtensionInstantiationStrategyFactory.getInstantiationStrategy(extension, moduleInfo, executionMode, dependencyManager, classLoader);
        }

        return new ApiBasedInstantiationStrategy(moduleInfo, executionMode, dependencyManager, classLoader);
    }
}