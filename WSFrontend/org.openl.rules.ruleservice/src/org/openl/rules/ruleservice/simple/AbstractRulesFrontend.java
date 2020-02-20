package org.openl.rules.ruleservice.simple;

import java.util.Objects;

import org.openl.runtime.ASMProxyFactory;

public abstract class AbstractRulesFrontend implements RulesFrontend {
    @Override
    public <T> T buildServiceProxy(String serviceName, Class<T> proxyInterface) {
        return buildServiceProxy(serviceName, proxyInterface, Thread.currentThread().getContextClassLoader());
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T buildServiceProxy(String serviceName, Class<T> proxyInterface, ClassLoader classLoader) {
        Objects.requireNonNull(serviceName, "serviceName cannot be null");
        Objects.requireNonNull(proxyInterface, "proxyInterface cannot be null");
        return ASMProxyFactory
            .newProxyInstance(classLoader, new RulesFrontendProxyMethodHandler(serviceName, this), proxyInterface);

    }
}
