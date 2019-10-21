package org.openl.rules.ruleservice.databinding;

import java.util.Objects;

import org.springframework.util.Assert;

public class ServiceConfigurationStringFactoryBean extends ServiceConfigurationFactoryBean<String> {

    private String propertyName;

    public String getPropertyName() {
        return propertyName;
    }

    public void setPropertyName(String propertyName) {
        this.propertyName = Objects.requireNonNull(propertyName, "propertyName cannot be null");
    }

    @Override
    protected String createInstance() throws Exception {
        String ret = getDefaultValue();
        Object value = getValue(getPropertyName().trim());
        if (value instanceof String) {
            return (String) value;
        } else {
            if (value != null) {
                throw new ServiceConfigurationException(
                    String.format("Expected string for '%s' in the configuration for service '%s'.",
                        getPropertyName(),
                        getServiceDescription().getName()));
            }
        }
        return ret;
    }

    @Override
    public Class<?> getObjectType() {
        return String.class;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Assert.notNull(this.propertyName, "propertyName must be set!");
    }

}
