package org.openl.rules.ruleservice.logging.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.openl.rules.ruleservice.logging.Convertor;

@Retention(RetentionPolicy.RUNTIME)
@Target(value = { ElementType.FIELD, ElementType.METHOD })
public @interface KafkaMessageHeader {
    String value();

    Type type() default Type.CONSUMER_RECORD;

    Class<? extends Convertor<byte[], ?>> convertor() default ByteArrayToStringConvertor.class;

    public enum Type {
        PRODUCER_RECORD,
        CONSUMER_RECORD
    }
}
