package org.openl.rules.ruleservice.publish;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cxf.databinding.DataBinding;
import org.apache.cxf.endpoint.Server;
import org.apache.cxf.feature.Feature;
import org.apache.cxf.frontend.ServerFactoryBean;
import org.openl.rules.ruleservice.core.OpenLService;
import org.openl.rules.ruleservice.core.RuleServiceDeployException;
import org.openl.rules.ruleservice.core.RuleServiceUndeployException;
import org.openl.rules.ruleservice.logging.CollectOpenLServiceIntercepror;
import org.openl.rules.ruleservice.logging.CollectOperationResourceInfoInterceptor;
import org.openl.rules.ruleservice.logging.CollectPublisherTypeInterceptor;
import org.openl.rules.ruleservice.publish.jaxws.JAXWSInterfaceEnhancerHelper;
import org.openl.rules.ruleservice.publish.jaxws.JAXWSInvocationHandler;
import org.openl.rules.ruleservice.servlet.AvailableServicesGroup;
import org.openl.rules.ruleservice.servlet.ServiceInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectFactory;

/**
 * DeploymentAdmin to expose services via HTTP.
 * 
 * @author PUdalau, Marat Kamalov
 */
public class JAXWSRuleServicePublisher extends AbstractRuleServicePublisher implements AvailableServicesGroup {

    private final Logger log = LoggerFactory.getLogger(JAXWSRuleServicePublisher.class);

    private ObjectFactory<?> serverFactory;
    private Map<OpenLService, ServiceServer> runningServices = new HashMap<OpenLService, ServiceServer>();
    private String baseAddress;
    private List<ServiceInfo> availableServices = new ArrayList<ServiceInfo>();
    private boolean loggingStoreEnable = false;

    private ObjectFactory<? extends Feature> storeLoggingFeatureFactoryBean;

    public ObjectFactory<? extends Feature> getStoreLoggingFeatureFactoryBean() {
        return storeLoggingFeatureFactoryBean;
    }

    public void setStoreLoggingFeatureFactoryBean(ObjectFactory<? extends Feature> storeLoggingFeatureFactoryBean) {
        this.storeLoggingFeatureFactoryBean = storeLoggingFeatureFactoryBean;
    }

    public void setLoggingStoreEnable(boolean loggingStoreEnable) {
        this.loggingStoreEnable = loggingStoreEnable;
    }

    public boolean isLoggingStoreEnable() {
        return loggingStoreEnable;
    }

    public String getBaseAddress() {
        return baseAddress;
    }

    public void setBaseAddress(String address) {
        this.baseAddress = address;
    }

    public ObjectFactory<?> getServerFactory() {
        return serverFactory;
    }

    public void setServerFactory(ObjectFactory<?> serverFactory) {
        this.serverFactory = serverFactory;
    }

    /* internal for test */ServerFactoryBean getServerFactoryBean() {
        if (serverFactory != null) {
            return (ServerFactoryBean) serverFactory.getObject();
        }
        return new ServerFactoryBean();
    }

    /* internal for test */Feature getStoreLoggingFeatureBean() {
        if (storeLoggingFeatureFactoryBean != null) {
            return storeLoggingFeatureFactoryBean.getObject();
        }
        throw new IllegalArgumentException("loggingInfoStoringService isn't defined.");
    }

    protected Class<?> enhanceServiceClassWithJAXWSAnnotations(Class<?> serviceClass,
            OpenLService service) throws Exception {
        return JAXWSInterfaceEnhancerHelper.decorateInterface(serviceClass, service);
    }

    protected Object createWrappedBean(Object serviceBean, OpenLService service) {
        return Proxy.newProxyInstance(service.getClassLoader(), new Class<?>[] {service.getServiceClass()}, new JAXWSInvocationHandler(serviceBean));
    }

    @Override
    protected void deployService(OpenLService service) throws RuleServiceDeployException {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(service.getClassLoader());

        try {
            ServerFactoryBean svrFactory = getServerFactoryBean();
            ClassLoader origClassLoader = svrFactory.getBus().getExtension(ClassLoader.class);
            try {
                String serviceAddress = getBaseAddress() + processURL(service.getUrl());
                svrFactory.setAddress(serviceAddress);
                
                Class<?> serviceClass = enhanceServiceClassWithJAXWSAnnotations(service.getServiceClass(), service);
                svrFactory.setServiceClass(serviceClass);

                Object target = createWrappedBean(service.getServiceBean(), service);

                svrFactory.setServiceBean(target);

                svrFactory.getBus().setExtension(service.getClassLoader(), ClassLoader.class);
                if (isLoggingStoreEnable()) {
                    svrFactory.getFeatures().add(getStoreLoggingFeatureBean());
                    svrFactory.getInInterceptors().add(new CollectOpenLServiceIntercepror(service));
                    svrFactory.getInInterceptors().add(new CollectPublisherTypeInterceptor(getPublisherType()));
                    svrFactory.getInInterceptors().add(new CollectOperationResourceInfoInterceptor());
                    svrFactory.getInFaultInterceptors().add(new CollectOpenLServiceIntercepror(service));
                    svrFactory.getInFaultInterceptors().add(new CollectPublisherTypeInterceptor(getPublisherType()));
                    svrFactory.getInFaultInterceptors().add(new CollectOperationResourceInfoInterceptor());
                }
                Server wsServer = svrFactory.create();

                ServiceServer serviceServer = new ServiceServer(wsServer, svrFactory.getDataBinding());
                runningServices.put(service, serviceServer);
                availableServices.add(createServiceInfo(service));
                log.info("Service '{}' has been exposed with URL '{}'.", service.getName(), serviceAddress);
            } finally {
                svrFactory.getBus().setExtension(origClassLoader, ClassLoader.class);
            }
        } catch (Exception t) {
            throw new RuleServiceDeployException(String.format("Failed to deploy service '%s'", service.getName()), t);
        } finally {
            Thread.currentThread().setContextClassLoader(oldClassLoader);
        }
    }

    public DataBinding getDataBinding(String serviceName) {
        OpenLService service = getServiceByName(serviceName);
        if (service == null) {
            return null;
        }
        return runningServices.get(service).getDatabinding();
    }

    public Collection<OpenLService> getServices() {
        return new ArrayList<OpenLService>(runningServices.keySet());
    }

    public OpenLService getServiceByName(String name) {
        for (OpenLService service : runningServices.keySet()) {
            if (service.getName().equals(name)) {
                return service;
            }
        }
        return null;
    }

    @Override
    protected void undeployService(String serviceName) throws RuleServiceUndeployException {
        OpenLService service = getServiceByName(serviceName);
        if (service == null) {
            throw new RuleServiceUndeployException(String.format("There is no running service '%s'", serviceName));
        }
        try {
            runningServices.get(service).getServer().destroy();
            runningServices.remove(service);
            removeServiceInfo(serviceName);
        } catch (Exception t) {
            throw new RuleServiceUndeployException(String.format("Failed to undeploy service '%s'.", serviceName), t);
        }
    }

    @Override
    public String getGroupName() {
        return "SOAP";
    }

    @Override
    public List<ServiceInfo> getAvailableServices() {
        List<ServiceInfo> services = new ArrayList<ServiceInfo>(availableServices);
        Collections.sort(services, new Comparator<ServiceInfo>() {
            @Override
            public int compare(ServiceInfo o1, ServiceInfo o2) {
                return o1.getName().compareToIgnoreCase(o2.getName());
            }
        });
        return services;
    }

    private ServiceInfo createServiceInfo(OpenLService service) {
        List<String> methodNames = new ArrayList<String>();
        for (Method method : service.getServiceClass().getMethods()) {
            methodNames.add(method.getName());
        }
        Collections.sort(methodNames, new Comparator<String>() {
            @Override
            public int compare(String o1, String o2) {
                return o1.compareToIgnoreCase(o2);
            }
        });
        String url = processURL(service.getUrl()) + "?wsdl";
        return new ServiceInfo(new Date(), service.getName(), methodNames, url, "WSDL");
    }

    private void removeServiceInfo(String serviceName) {
        for (Iterator<ServiceInfo> iterator = availableServices.iterator(); iterator.hasNext();) {
            ServiceInfo serviceInfo = iterator.next();
            if (serviceInfo.getName().equals(serviceName)) {
                iterator.remove();
                break;
            }
        }
    }

    private static class ServiceServer {
        private Server server;
        private DataBinding databinding;

        public ServiceServer(Server server, DataBinding dataBinding) {
            if (server == null) {
                throw new IllegalArgumentException("server arg must not be null!");
            }

            this.server = server;
            this.databinding = dataBinding;
        }

        public DataBinding getDatabinding() {
            return databinding;
        }

        public Server getServer() {
            return server;
        }
    }

    @Override
    public boolean isServiceDeployed(String name) {
        return getServiceByName(name) != null;
    }
}
