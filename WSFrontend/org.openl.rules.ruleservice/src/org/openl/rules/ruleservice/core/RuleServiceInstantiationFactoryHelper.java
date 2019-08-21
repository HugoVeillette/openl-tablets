package org.openl.rules.ruleservice.core;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.openl.binding.MethodUtil;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.ruleservice.core.annotations.ServiceExtraMethod;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAfterAdvice;
import org.openl.rules.ruleservice.core.interceptors.ServiceMethodAroundAdvice;
import org.openl.rules.ruleservice.core.interceptors.annotations.NotConvertor;
import org.openl.rules.ruleservice.core.interceptors.annotations.UseOpenMethodReturnType;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAfterInterceptors;
import org.openl.rules.ruleservice.core.interceptors.annotations.ServiceCallAroundInterceptor;
import org.openl.rules.ruleservice.core.interceptors.annotations.TypeResolver;
import org.openl.rules.ruleservice.core.interceptors.converters.SPRToPlainConvertorAdvice;
import org.openl.rules.variation.VariationsResult;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMember;
import org.openl.util.ClassUtils;
import org.openl.util.generation.InterfaceTransformer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class RuleServiceInstantiationFactoryHelper {
    private RuleServiceInstantiationFactoryHelper() {
        // Hidden constructor
    }

    /**
     * Special ClassVistor to generate interface with {@link Object} as the return type for methods that have "after
     * interceptors".
     *
     * @author PUdalau
     */
    private static class RuleserviceInterceptorsSupportClassVisitor extends ClassVisitor {
        private Map<Method, Pair<Class<?>, Boolean>> methodsWithReturnTypeNeedsChange;
        private Collection<Method> methodsToRemove;

        /**
         * Constructs instance with delegated {@link ClassVisitor} and set of methods.
         *
         * @param visitor delegated {@link ClassVisitor}.
         * @param methods Methods where to change return type.
         */
        private RuleserviceInterceptorsSupportClassVisitor(ClassVisitor visitor,
                Map<Method, Pair<Class<?>, Boolean>> methodsWithReturnTypeNeedsChange,
                Collection<Method> methodsToRemove) {
            super(Opcodes.ASM5, visitor);
            this.methodsWithReturnTypeNeedsChange = methodsWithReturnTypeNeedsChange;
            this.methodsToRemove = methodsToRemove;
        }

        @Override
        public MethodVisitor visitMethod(int arg0, String arg1, String arg2, String arg3, String[] arg4) {
            for (Method method : methodsToRemove) {
                if (arg1.equals(method.getName()) && arg2.equals(Type.getMethodDescriptor(method))) {
                    return null;
                }
            }

            for (Entry<Method, Pair<Class<?>, Boolean>> entry : methodsWithReturnTypeNeedsChange.entrySet()) {
                Method method = entry.getKey();
                if (arg1.equals(method.getName()) && arg2.equals(Type.getMethodDescriptor(method))) {
                    MethodVisitor mv = super.visitMethod(arg0,
                        arg1,
                        convertReturnType(arg2, entry.getValue().getKey()),
                        arg3,
                        arg4);
                    if (!entry.getValue().getValue()) {
                        AnnotationVisitor av = mv.visitAnnotation(Type.getDescriptor(ServiceCallAfterInterceptor.class),
                            true);
                        AnnotationVisitor av1 = av.visitArray("value");
                        av1.visit("value", Type.getType(SPRToPlainConvertorAdvice.class));
                        av1.visitEnd();
                        av.visitEnd();
                    }
                    return mv;
                }
            }

            return super.visitMethod(arg0, arg1, arg2, arg3, arg4);
        }

        private String convertReturnType(String arg2, Class<?> newType) {
            int index = arg2.lastIndexOf(')');
            return arg2.substring(0, index + 1) + Type.getDescriptor(newType);
        }
    }

    private static final String UNDECORATED_CLASS_NAME_SUFFIX = "$Original";

    /**
     * Returns service class for instantiation strategy according to after interceptors of methods in service class of
     * service specified as the argument.
     *
     * @param instantiationStrategy instantiation strategy where returned interfaces will be set.
     * @param serviceClass Interface for service, which will be used for service class creation.
     * @return Service class for instantiation strategy based on service class for service.
     * @throws Exception
     */
    public static Class<?> getInterfaceForInstantiationStrategy(Class<?> serviceClass, ClassLoader classLoader) {
        return processInterface(null, serviceClass, null, true, false, classLoader);
    }

    public static Class<?> getInterfaceForService(IOpenClass openClass,
            Class<?> serviceClass,
            Object serviceTarget,
            ClassLoader classLoader) {
        return processInterface(openClass, serviceClass, serviceTarget, false, true, classLoader);
    }

    public static Class<?> processInterface(IOpenClass openClass,
            Class<?> serviceClass,
            Object serviceTarget,
            boolean removeServiceExtraMethods,
            boolean toServiceClass,
            ClassLoader classLoader) {
        if (toServiceClass) {
            Objects.requireNonNull(serviceTarget);
        }

        Map<Method, Pair<Class<?>, Boolean>> methodsWithReturnTypeNeedsChange = getMethodsWithReturnTypeNeedsChange(
            openClass,
            serviceClass,
            serviceTarget,
            toServiceClass);

        Set<Method> methodsToRemove = removeServiceExtraMethods ? getMethodsWithServiceExtraMethodAnnotation(
            serviceClass) : Collections.emptySet();

        if (methodsWithReturnTypeNeedsChange.isEmpty() && methodsToRemove.isEmpty()) {
            return serviceClass;
        } else {
            Logger log = LoggerFactory.getLogger(RuleServiceInstantiationFactoryHelper.class);
            for (Entry<Method, Pair<Class<?>, Boolean>> entry : methodsWithReturnTypeNeedsChange.entrySet()) {
                Method method = entry.getKey();
                if (method.getReturnType().isAssignableFrom(entry.getValue().getKey()) && log.isWarnEnabled()) {
                    log.warn(
                        "Wrong return type for '{}' method in '{}' class is used. Return type is replaced to '{}'.",
                        MethodUtil.printMethod(method.getName(), method.getParameterTypes()),
                        serviceClass.getName(),
                        entry.getValue().getKey().getSimpleName());
                }
            }

            ClassWriter classWriter = new ClassWriter(0);
            ClassVisitor classVisitor = new RuleserviceInterceptorsSupportClassVisitor(classWriter,
                methodsWithReturnTypeNeedsChange,
                methodsToRemove);
            String className = serviceClass.getName() + UNDECORATED_CLASS_NAME_SUFFIX;
            InterfaceTransformer transformer = new InterfaceTransformer(serviceClass, className);
            transformer.accept(classVisitor);
            classWriter.visitEnd();
            try {
                // Create class object.
                //
                return ClassUtils.defineClass(className, classWriter.toByteArray(), classLoader);
            } catch (Exception e) {
                throw new OpenlNotCheckedException(e);
            }
        }
    }

    private static Class<?> getGenericType(Class<?> clazz) {
        if (clazz
            .getGenericSuperclass() instanceof ParameterizedType && ((ParameterizedType) clazz.getGenericSuperclass())
                .getActualTypeArguments()[0] instanceof Class) {
            return (Class<?>) ((ParameterizedType) clazz.getGenericSuperclass()).getActualTypeArguments()[0];
        }
        return Object.class;
    }

    private static Class<? extends ServiceMethodAfterAdvice<?>> getLastServiceMethodAfterAdvice(
            ServiceCallAfterInterceptor serviceCallAfterInterceptor) {
        Class<? extends ServiceMethodAfterAdvice<?>>[] interceptors = serviceCallAfterInterceptor.value();
        int i = interceptors.length - 1;
        while (i >= 0) {
            Class<? extends ServiceMethodAfterAdvice<?>> serviceMethodAfterAdvice = interceptors[i];
            if (!serviceMethodAfterAdvice.isAnnotationPresent(NotConvertor.class)) {
                return serviceMethodAfterAdvice;
            }
            i--;
        }
        return null;
    }

    private static Class<?> notNullIfNewMethodReturnTypeWithAnnotations(Method method,
            Object serviceTarget,
            boolean toServiceClass) {

        Class<? extends ServiceMethodAfterAdvice<?>> lastServiceMethodAfterAdvice = null;
        if (method.getAnnotation(ServiceCallAfterInterceptor.class) != null && !method.getReturnType()
            .equals(VariationsResult.class)) {
            ServiceCallAfterInterceptor serviceCallAfterInterceptor = method
                .getAnnotation(ServiceCallAfterInterceptor.class);
            lastServiceMethodAfterAdvice = getLastServiceMethodAfterAdvice(serviceCallAfterInterceptor);
        }

        ServiceCallAfterInterceptors serviceCallAfterInterceptors = method
            .getAnnotation(ServiceCallAfterInterceptors.class);
        if (serviceCallAfterInterceptors != null && serviceCallAfterInterceptors.value().length > 0 && !method
            .getReturnType()
            .equals(VariationsResult.class)) {
            for (ServiceCallAfterInterceptor serviceCallAfterInterceptor : serviceCallAfterInterceptors.value()) {
                Class<? extends ServiceMethodAfterAdvice<?>> x = getLastServiceMethodAfterAdvice(
                    serviceCallAfterInterceptor);
                if (x != null) {
                    lastServiceMethodAfterAdvice = x;
                }
            }
        }

        if (lastServiceMethodAfterAdvice != null) {
            if (toServiceClass) {
                if (lastServiceMethodAfterAdvice.isAnnotationPresent(UseOpenMethodReturnType.class)) {
                    Class<?> t = extractOpenMethodReturnType(method,
                        serviceTarget,
                        lastServiceMethodAfterAdvice,
                        method.getAnnotation(UseOpenMethodReturnType.class).value());
                    if (t != null) {
                        return t;
                    }
                }
                return getGenericType(lastServiceMethodAfterAdvice);
            }
            return Object.class;
        }

        if (method.getAnnotation(ServiceCallAroundInterceptor.class) != null && !method.getReturnType()
            .equals(VariationsResult.class)) {
            ServiceCallAroundInterceptor serviceCallAroundInterceptor = method
                .getAnnotation(ServiceCallAroundInterceptor.class);
            Class<? extends ServiceMethodAroundAdvice<?>> serviceMethodAroundAdvice = serviceCallAroundInterceptor
                .value();
            if (toServiceClass && serviceMethodAroundAdvice != null && !serviceMethodAroundAdvice
                .isAnnotationPresent(NotConvertor.class)) {
                Class<?> t = extractOpenMethodReturnType(method,
                    serviceTarget,
                    serviceMethodAroundAdvice,
                    method.getAnnotation(UseOpenMethodReturnType.class).value());
                if (t != null) {
                    return t;
                }
                return getGenericType(serviceMethodAroundAdvice);
            }
            return Object.class;
        }

        return null;
    }

    private static Class<?> extractOpenMethodReturnType(Method method,
            Object serviceTarget,
            Class<?> interceptorClass,
            TypeResolver typeResolver) {
        Method m = null;
        try {
            m = serviceTarget.getClass().getMethod(method.getName(), method.getParameterTypes());
        } catch (NoSuchMethodException e) {
            logWarn(method, interceptorClass);
            return null;
        }
        if (m != null) {
            IOpenMember openMember = RuleServiceOpenLServiceInstantiationHelper.getOpenMember(m, serviceTarget);
            if (openMember == null) {
                logWarn(method, interceptorClass);
            }
            Class<?> retType = null;
            IOpenClass returnType = RuleServiceOpenLServiceInstantiationHelper.getOpenMember(m, serviceTarget)
                .getType();
            switch (typeResolver) {
                case ORIGINAL:
                    retType = returnType.getInstanceClass();
                    break;
                case IF_CSR_TO_PLAIN:
                    if (returnType instanceof CustomSpreadsheetResultOpenClass) {
                        retType = returnType.getInstanceClass();
                    } else {
                        retType = ((CustomSpreadsheetResultOpenClass) returnType).getBeanClass();
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }

            if (retType == null) {
                throw new IllegalStateException("Wrong instance class.");
            }
            return retType;
        }
        return null;
    }

    private static void logWarn(Method method, Class<?> interceptorClass) {
        Logger log = LoggerFactory.getLogger(RuleServiceOpenLServiceInstantiationFactoryImpl.class);

        if (log.isWarnEnabled()) {
            log.warn(
                "Method return type is not found for '{}.{}'. Please, make sure that @OpenMethodReturnType is used correctly in '{}' interceptor class.",
                method.getClass().getSimpleName(),
                MethodUtil.printMethod(method.getName(), method.getParameterTypes()),
                interceptorClass.getSimpleName());
        }
    }

    private static boolean isMethodWithServiceExtraMethodAnnotation(Method method) {
        return method.getAnnotation(ServiceExtraMethod.class) != null;
    }

    private static boolean isTypeChangingAnnotationPresent(Method method) {
        return method.isAnnotationPresent(ServiceCallAfterInterceptor.class) || method.isAnnotationPresent(
            ServiceCallAfterInterceptors.class) || method.isAnnotationPresent(ServiceCallAroundInterceptor.class);
    }

    /**
     * Look through all methods of the specified class in order to find all methods annotated by
     * {@link ServiceCallAfterInterceptor}.
     *
     * @param serviceClass Class to be analyzed.
     * @return Methods which have after interceptors.
     */
    public static Map<Method, Pair<Class<?>, Boolean>> getMethodsWithReturnTypeNeedsChange(IOpenClass openClass,
            Class<?> serviceClass,
            Object serviceTarget,
            boolean toServiceClass) {
        Map<Method, Pair<Class<?>, Boolean>> ret = new HashMap<>();
        for (Method method : serviceClass.getMethods()) {
            Class<?> newReturnType = notNullIfNewMethodReturnTypeWithAnnotations(method, serviceTarget, toServiceClass);
            if (newReturnType != null) {
                ret.put(method, Pair.of(newReturnType, Boolean.TRUE));
            } else if (toServiceClass && !isTypeChangingAnnotationPresent(method) && SpreadsheetResult.class
                .isAssignableFrom(method.getReturnType())) {
                IOpenMember openMember = RuleServiceOpenLServiceInstantiationHelper.getOpenMember(method,
                    serviceTarget);
                if (openMember.getType() instanceof CustomSpreadsheetResultOpenClass) {
                    CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) openMember
                        .getType();
                    XlsModuleOpenClass module = (XlsModuleOpenClass) openClass;
                    CustomSpreadsheetResultOpenClass csrt = (CustomSpreadsheetResultOpenClass) module
                        .findType(customSpreadsheetResultOpenClass.getName());
                    if (!csrt.isEmptyBean()) {
                        ret.put(method, Pair.of(csrt.getBeanClass(), Boolean.FALSE));
                    }
                }
            }
        }
        return ret;
    }

    /**
     * Look through all methods of the specified class in order to find all methods annotated by
     * {@link ServiceExtraMethod}.
     *
     * @param serviceClass Class to be analyzed.
     * @return Methods which have after interceptors.
     */
    public static Set<Method> getMethodsWithServiceExtraMethodAnnotation(Class<?> serviceClass) {
        Set<Method> ret = new HashSet<>();
        for (Method method : serviceClass.getMethods()) {
            if (isMethodWithServiceExtraMethodAnnotation(method)) {
                ret.add(method);
            }
        }
        return ret;
    }

}
