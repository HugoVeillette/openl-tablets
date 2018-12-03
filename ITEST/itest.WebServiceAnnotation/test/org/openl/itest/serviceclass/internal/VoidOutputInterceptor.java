package org.openl.itest.serviceclass.internal;

import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAfterAdvice;

import java.lang.reflect.Method;

public class VoidOutputInterceptor implements ServiceMethodAfterAdvice<Response> {

    @Override
    public Response afterReturning(Method interfaceMethod, Object result, Object... args) throws Exception {
        return new Response("PASSED", 0);
    }

    @Override
    public Response afterThrowing(Method interfaceMethod, Exception t, Object... args) throws Exception {
        return new Response("ERROR", -1);
    }
}
