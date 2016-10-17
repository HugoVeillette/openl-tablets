package org.openl.rules.ruleservice.simple;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.annotation.Required;

import javax.annotation.Resource;

public class OpenLServiceFactoryBean<T> implements FactoryBean<T> {
    private Class<T> proxyInterface;
    private String serviceName;
    private RulesFrontend rulesFrontend;

    @Override
    public T getObject() throws Exception {
        return rulesFrontend.buildServiceProxy(serviceName, proxyInterface);
    }

    @Override
    public Class<?> getObjectType() {
        return proxyInterface;
    }

    @Override
    public boolean isSingleton() {
        return Boolean.TRUE;
    }

    @Resource(name = "frontend")
    public void setRulesFrontend(RulesFrontend rulesFrontend) {
        this.rulesFrontend = rulesFrontend;
    }

    @Required
    public void setProxyInterface(Class<T> proxyInterface) {
        this.proxyInterface = proxyInterface;
    }

    @Required
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }
}
