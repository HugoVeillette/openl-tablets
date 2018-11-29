package org.openl.itest.serviceclass.internal;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAroundAdvice;

import java.lang.reflect.Method;

public class LongMethodAroundInterceptor implements ServiceMethodAroundAdvice<Response> {

    @Override
    public Response around(Method interfaceMethod, Method proxyMethod, Object proxy, Object... args) throws Throwable {
        Long res = (Long) proxyMethod.invoke(proxy, args);
        return new Response("SUCCESS", res.intValue());
    }
}
