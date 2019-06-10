package org.openl.rules.ruleservice.simple;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.Lock;

import org.apache.commons.beanutils.MethodUtils;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceInstantiationException;
import org.openl.rules.ruleservice.core.RuleServiceRedeployLock;
import org.openl.rules.ruleservice.core.RuleServiceWrapperException;
import org.openl.util.ClassUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple implementation of IRulesFrontend interface.
 *
 * @author Marat Kamalov
 */
public class RulesFrontendImpl extends AbstractRulesFrontend {
    private final Logger log = LoggerFactory.getLogger(RulesFrontendImpl.class);

    private Map<String, OpenLService> runningServices = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     */
    @Override
    public void registerService(OpenLService service) {
        if (service == null) {
            throw new IllegalArgumentException("service argument must not be null!");
        }
        OpenLService replacedService = runningServices.put(service.getName(), service);
        if (replacedService != null) {
            log.warn("Service '{}' has already been registered. Replaced with new service bean.", service.getName());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void unregisterService(String serviceName) {
        Objects.requireNonNull(serviceName, "serviceName argument can't be null!");
        runningServices.remove(serviceName);
    }

    // for internal usage
    Collection<OpenLService> getServices() {
        return new ArrayList<>(runningServices.values());
    }

    @Override
    public java.util.Collection<String> getServiceNames() {
        return new ArrayList<>(runningServices.keySet());
    }

    @Override
    public OpenLService findServiceByName(String serviceName) {
        Objects.requireNonNull(serviceName, "serviceName argument can't be null!");
        return getService(serviceName);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object execute(String serviceName,
            String ruleName,
            Class<?>[] inputParamsTypes,
            Object[] params) throws MethodInvocationException {
        Objects.requireNonNull(serviceName, "serviceName argument can't be null!");
        Objects.requireNonNull(ruleName, "ruleName argument can't be null!");
        OpenLService service = getService(serviceName);
        if (service == null) {
            throw new MethodInvocationException("Service '" + serviceName + "' hasn't been found.");
        }
        try {
            if (service.getServiceBean() != null) {
                Method serviceMethod = null;
                serviceMethod = MethodUtils
                    .getMatchingAccessibleMethod(service.getServiceBean().getClass(), ruleName, inputParamsTypes);
                if (serviceMethod == null) {
                    StringBuilder sb = new StringBuilder();
                    boolean f = true;
                    for (Class<?> param : inputParamsTypes) {
                        if (!f) {
                            sb.append(",");
                        } else {
                            f = false;
                        }
                        sb.append(param.getCanonicalName());
                    }

                    throw new MethodInvocationException("Method '" + ruleName + "(" + sb
                        .toString() + ")' hasn't been found in service '" + serviceName + "'!");
                }
                try {
                    return serviceMethod.invoke(service.getServiceBean(), params);
                } catch (IllegalAccessException e) {
                    throw new InternalError(e.toString());
                } catch (InvocationTargetException e) {
                    Throwable t = e.getCause();
                    throw new MethodInvocationException(t.toString(), t);
                }
            } else {
                throw new MethodInvocationException("Service initialization '" + serviceName + "' has been failed.");
            }
        } catch (RuleServiceInstantiationException e) {
            throw new MethodInvocationException("Service initialization '" + serviceName + "' has been failed.", e);
        }
    }

    private OpenLService getService(String serviceName) {
        Lock lock = RuleServiceRedeployLock.getInstance().getReadLock();
        OpenLService service = null;
        try {
            lock.lock();
            service = runningServices.get(serviceName);
        } finally {
            lock.unlock();
        }
        return service;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object execute(String serviceName, String ruleName, Object... params) throws MethodInvocationException {
        Objects.requireNonNull(serviceName, "serviceName argument can't be null!");
        Objects.requireNonNull(ruleName, "ruleName argument can't be null!");

        log.debug("Executing rule from service with name='{}', ruleName='{}'.", serviceName, ruleName);

        Class<?>[] paramTypes = new Class<?>[params.length];
        for (int i = 0; i < params.length; i++) {
            if (params[i] == null) {
                throw new MethodInvocationException(
                    "One parameter is null. Please, use 'execute(String serviceName, String ruleName, Class<?>[] inputParamsTypes, Object[] params)' method! This method doesn't supports null params!");
            }
            paramTypes[i] = params[i].getClass();
        }
        return execute(serviceName, ruleName, paramTypes, params);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue(String serviceName, String fieldName) throws MethodInvocationException {
        Objects.requireNonNull(serviceName, "serviceName argument can't be null!");
        Objects.requireNonNull(fieldName, "ieldName argument can't be null!!");

        log.debug("Getting value from service with name='{}', fieldName='{}'.", serviceName, fieldName);

        Object result = null;

        OpenLService service = getService(serviceName);
        if (service != null) {
            try {
                Method serviceMethod = MethodUtils.getMatchingAccessibleMethod(service.getServiceBean().getClass(),
                    ClassUtils.getter(fieldName),
                    new Class<?>[] {});
                result = serviceMethod.invoke(service.getServiceBean(), new Object[] {});
            } catch (Exception e) {
                if (e.getCause() instanceof RuleServiceWrapperException) {
                    throw new MethodInvocationException(e.getCause());
                } else {
                    log.warn("Error on reading field '{}' from the service '{}'.", fieldName, serviceName, e);
                }
            }
        } else {
            throw new MethodInvocationException("Service '" + serviceName + "' hasn't been found.");
        }

        return result;
    }

}
