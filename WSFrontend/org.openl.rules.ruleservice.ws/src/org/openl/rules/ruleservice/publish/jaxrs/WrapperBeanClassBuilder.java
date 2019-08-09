package org.openl.rules.ruleservice.publish.jaxrs;

import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;

class WrapperBeanClassBuilder extends JavaBeanClassBuilder {

    private String methodName;

    public WrapperBeanClassBuilder(String beanName, String methodName) {
        super(beanName);
        Objects.requireNonNull(methodName);
        if (StringUtils.isEmpty(methodName)) {
            throw new IllegalArgumentException("Method name can't be empty.");
        }
        this.methodName = methodName;
    }

    @Override
    public byte[] byteCode() {
        return new WrapperBeanClassGenerator(getBeanName(),
            getFields(),
            getParentClass(),
            getParentFields(),
            methodName).byteCode();
    }
}
