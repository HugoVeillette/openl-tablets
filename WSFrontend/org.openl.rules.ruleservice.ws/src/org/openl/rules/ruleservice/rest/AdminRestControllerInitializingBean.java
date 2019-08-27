package org.openl.rules.ruleservice.rest;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.apache.cxf.jaxrs.JAXRSServerFactoryBean;
import org.apache.cxf.jaxrs.impl.WebApplicationExceptionMapper;
import org.openl.rules.ruleservice.publish.jaxrs.JAXRSExceptionMapper;
import org.springframework.beans.factory.InitializingBean;

public class AdminRestControllerInitializingBean implements InitializingBean {

    private AdminRestController adminRestController;
    private String baseAddress;

    public AdminRestControllerInitializingBean(AdminRestController adminRestController) {
        this.adminRestController = adminRestController;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        JAXRSServerFactoryBean factoryBean = new JAXRSServerFactoryBean();
        factoryBean.setProvider(new JacksonJsonProvider());
        factoryBean.setProvider(new JAXRSExceptionMapper());
        factoryBean.setProvider(new WebApplicationExceptionMapper());
        factoryBean.setServiceBean(adminRestController);
        factoryBean.setAddress(baseAddress);
        factoryBean.init();
    }

    public void setBaseAddress(String baseAddress) {
        this.baseAddress = baseAddress;
    }
}
