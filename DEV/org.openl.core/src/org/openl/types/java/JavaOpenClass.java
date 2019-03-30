/*
 * Created on May 20, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.*;

import org.openl.base.INamedThing;
import org.openl.gen.JavaInterfaceImplBuilder;
import org.openl.types.IAggregateInfo;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.AOpenClass;
import org.openl.types.impl.ArrayIndex;
import org.openl.types.impl.ArrayLengthOpenField;
import org.openl.types.impl.MethodKey;
import org.openl.util.ClassUtils;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
public class JavaOpenClass extends AOpenClass {

    public static final JavaOpenClass INT = new JavaPrimitiveClass(int.class, Integer.class, 0);
    public static final JavaOpenClass LONG = new JavaPrimitiveClass(long.class, Long.class, 0L);
    public static final JavaOpenClass DOUBLE = new JavaPrimitiveClass(double.class, Double.class, 0.0);
    public static final JavaOpenClass FLOAT = new JavaPrimitiveClass(float.class, Float.class, 0.0f);
    public static final JavaOpenClass SHORT = new JavaPrimitiveClass(short.class, Short.class, (short) 0);
    public static final JavaOpenClass CHAR = new JavaPrimitiveClass(char.class, Character.class, '\0');
    public static final JavaOpenClass BYTE = new JavaPrimitiveClass(byte.class, Byte.class, (byte) 0);
    public static final JavaOpenClass BOOLEAN = new JavaPrimitiveClass(boolean.class, Boolean.class, Boolean.FALSE);
    public static final JavaOpenClass VOID = new JavaPrimitiveClass(void.class, Void.class, null);
    public static final JavaOpenClass STRING = new JavaOpenClass(String.class, true);
    public static final JavaOpenClass OBJECT = new JavaOpenClass(Object.class, false);
    public static final JavaOpenClass CLASS = new JavaOpenClass(Class.class, true);

    protected Class<?> instanceClass;

    private final boolean simple;

    private volatile IAggregateInfo aggregateInfo;

    protected volatile Map<String, IOpenField> fields;

    private volatile List<IOpenClass> superClasses;

    public JavaOpenClass(Class<?> instanceClass) {
        this(instanceClass, false);
    }

    protected JavaOpenClass(Class<?> instanceClass, boolean simple) {
        this.instanceClass = instanceClass;
        this.simple = simple;
    }

    public static JavaOpenClass getOpenClass(Class<?> c) {
        JavaOpenClass res = JavaOpenClassCache.getInstance().get(c);
        if (res == null) {
            if (c.isInterface()) {
                res = new JavaOpenInterface(c);
            } else if (c.isEnum()) {
                res = new JavaOpenEnum(c);
            } else {
                CustomJavaOpenClass annotation = c.getAnnotation(CustomJavaOpenClass.class);
                if (annotation != null) {
                    res = createOpenClass(c, annotation);
                } else {
                    res = new JavaOpenClass(c);
                }
            }
            return JavaOpenClassCache.getInstance().put(c, res);
        }

        return res;
    }

    private static JavaOpenClass createOpenClass(Class<?> c, CustomJavaOpenClass annotation) {
        Class<? extends JavaOpenClass> type = annotation.type();
        try {
            return type.getConstructor(Class.class).newInstance(c);
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException(String.format(
                "Cannot find constructor with signature 'public MyCustomJavaOpenClass(Class<?> c)' in type %s",
                type.getCanonicalName()), e);
        } catch (InstantiationException e) {
            throw new IllegalStateException(
                String.format("Error while creating a custom JavaOpenClass of type '%s'", type.getCanonicalName()),
                e);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(
                String.format("Constructor of a custom JavaOpenClass of type '%s' is inaccessible",
                    type.getCanonicalName()),
                e);
        } catch (InvocationTargetException e) {
            throw new IllegalStateException(
                String.format("Constructor of a class '%s' threw and exception", type.getCanonicalName()),
                e);
        }
    }

    public static IOpenClass[] getOpenClasses(Class<?>[] cc) {
        if (cc.length == 0) {
            return IOpenClass.EMPTY;
        }

        IOpenClass[] ary = new IOpenClass[cc.length];

        for (int i = 0; i < cc.length; i++) {
            ary[i] = getOpenClass(cc[i]);
        }

        return ary;

    }

    public static Class<?> makeArrayClass(Class<?> c) {
        return Array.newInstance(c, 0).getClass();
    }

    public static ArrayIndex makeArrayIndex(IOpenClass arrayType) {
        return new ArrayIndex(getOpenClass(arrayType.getInstanceClass().getComponentType()));
    }

    public static boolean isVoid(IOpenClass clazz) {
        return JavaOpenClass.VOID.equals(clazz);
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof JavaOpenClass)) {
            return false;
        }
        return instanceClass == ((JavaOpenClass) obj).instanceClass;
    }

    @Override
    protected Map<String, IOpenField> fieldMap() {
        if (fields == null) {
            synchronized (this) {
                if (fields == null) {
                    fields = initalizeFields();
                }
            }
        }
        return fields;
    }

    private Map<String, IOpenField> initalizeFields() {
        Map<String, IOpenField> fields = new HashMap<>();
        Field[] ff = instanceClass.getDeclaredFields();

        if (isPublic(instanceClass)) {
            for (int i = 0; i < ff.length; i++) {
                if (isPublic(ff[i])) {
                    fields.put(ff[i].getName(), new JavaOpenField(ff[i]));
                }
            }
        }
        if (instanceClass.isArray()) {
            fields.put("length", new JavaArrayLengthField());
        }

        fields.put("class", new JavaClassClassField(instanceClass));
        BeanOpenField.collectFields(fields, instanceClass, getGetters(), getSetters());
        return fields;
    }

    protected Map<Method, BeanOpenField> getGetters() {
        return null;
    }

    protected Map<Method, BeanOpenField> getSetters() {
        return null;
    }

    public IAggregateInfo getAggregateInfo() {
        if (aggregateInfo != null) {
            return aggregateInfo;
        }

        synchronized (this) {
            if (aggregateInfo == null) {
                Class<?> instanceClass = getInstanceClass();
                if (List.class.isAssignableFrom(instanceClass)) {
                    aggregateInfo = JavaListAggregateInfo.LIST_AGGREGATE;
                } else if (Map.class.isAssignableFrom(instanceClass)) {
                    aggregateInfo = JavaMapAggregateInfo.MAP_AGGREGATE;
                } else if (Collection.class.isAssignableFrom(instanceClass)) {
                    aggregateInfo = JavaCollectionAggregateInfo.COLLECTION_AGGREGATE;
                } else {
                    aggregateInfo = JavaArrayAggregateInfo.ARRAY_AGGREGATE;
                }
            }
        }
        return aggregateInfo;
    }

    public String getDisplayName(int mode) {
        String name = getName();
        switch (mode) {
            case INamedThing.SHORT:
            case INamedThing.REGULAR:
            default:
                return name.substring(name.lastIndexOf('.') + 1);
            case INamedThing.LONG:
                return name;
        }
    }

    public Class<?> getInstanceClass() {
        return instanceClass;
    }

    String name;

    public String getName() {
        if (name == null)
            name = instanceClass.getCanonicalName();
        return name;
    }

    @Override
    public String getJavaName() {
        return instanceClass.getName();
    }

    @Override
    public String getPackageName() {
        return instanceClass.getPackage().getName();
    }

    public String getSimpleName() {
        return getDisplayName(INamedThing.SHORT);
    }

    @Override
    public int hashCode() {
        return instanceClass.hashCode();
    }

    @Override
    public boolean isAbstract() {
        return Modifier.isAbstract(instanceClass.getModifiers());
    }

    public boolean isAssignableFrom(Class<?> c) {
        return instanceClass.isAssignableFrom(c);
    }

    public boolean isAssignableFrom(IOpenClass ioc) {
        return instanceClass.isAssignableFrom(ioc.getInstanceClass());
    }

    public boolean isInstance(Object instance) {
        return instanceClass.isInstance(instance);
    }

    protected boolean isPublic(Class<?> declaringClass) {
        return Modifier.isPublic(declaringClass.getModifiers());
    }

    protected boolean isPublic(Member member) {
        return Modifier.isPublic(member.getModifiers());
    }

    @Override
    public boolean isSimple() {
        return simple;
    }

    @Override
    protected Map<MethodKey, IOpenMethod> initMethodMap() {
        Map<MethodKey, IOpenMethod> methods = new HashMap<>();
        Method[] mm = instanceClass.getDeclaredMethods();
        if (isPublic(instanceClass)) {
            for (int i = 0; i < mm.length; i++) {
                if (isPublic(mm[i])) {
                    JavaOpenMethod om = new JavaOpenMethod(mm[i]);
                    methods.put(new MethodKey(om), om);
                }
            }
        }

        if (methods.isEmpty()) {
            return Collections.emptyMap();
        }
        return Collections.unmodifiableMap(methods);
    }

    @Override
    protected Map<MethodKey, IOpenMethod> initConstructorMap() {
        Map<MethodKey, IOpenMethod> constructors = new HashMap<>();

        Constructor<?>[] cc = instanceClass.getDeclaredConstructors();
        for (int i = 0; i < cc.length; i++) {
            if (isPublic(cc[i])) {
                IOpenMethod om = new JavaOpenConstructor(cc[i]);
                constructors.put(new MethodKey(om), om);
            }
        }
        if (constructors.isEmpty()) {
            return Collections.emptyMap();
        }

        return Collections.unmodifiableMap(constructors);
    }

    public Object newInstance(IRuntimeEnv env) {
        try {
            return getInstanceClass().newInstance();
        } catch (Exception e) {
            throw RuntimeExceptionWrapper.wrap(e);
        }
    }

    @Override
    public Object nullObject() {
        return null;
    }

    @Override
    public IOpenClass getComponentClass() {
        return getAggregateInfo().getComponentType(this);
    }

    public Iterable<IOpenClass> superClasses() {
        if (superClasses == null) {
            synchronized (this) {
                if (superClasses == null) {
                    Class<?>[] interfaces = instanceClass.getInterfaces();
                    Class<?> superClass = instanceClass.getSuperclass();
                    List<IOpenClass> superClasses = new ArrayList<IOpenClass>(interfaces.length + 1);
                    if (superClass != null) {
                        superClasses.add(getOpenClass(superClass));
                    }
                    for (Class<?> interf : interfaces) {
                        superClasses.add(getOpenClass(interf));
                    }
                    this.superClasses = superClasses;
                }
            }
        }

        return superClasses;
    }

    @Override
    public Map<String, IOpenField> getFields() {
        Map<String, IOpenField> fields = new HashMap<>(fieldMap());
        for (IOpenClass superClass : superClasses()) {
            if (superClass.isInterface() && !isAbstract()) {
                // no need to add fields from interface if current instance is not abstract class
                continue;
            }
            Map<String, IOpenField> superClassFields = superClass.getFields();
            for (Map.Entry<String, IOpenField> entry : superClassFields.entrySet()) {
                final IOpenField candidateField = entry.getValue();
                if (candidateField.getType() == JavaOpenClass.CLASS) {
                    continue;
                }
                final String name = entry.getKey();
                final IOpenField origField = fields.get(name);
                if (origField == null) {
                    fields.put(name, candidateField);
                } else {
                    if (origField.getType().equals(candidateField.getType())) {
                        // we assume that IOpenField always have read or write method
                        if (!origField.isWritable() && candidateField.isWritable()) {
                            fields.put(name, new OpenFieldCombiner(origField, candidateField));
                        } else if (!origField.isReadable() && candidateField.isReadable()) {
                            fields.put(name, new OpenFieldCombiner(candidateField, origField));
                        }
                    }
                }
            }
        }
        return fields;
    }

    private static class JavaArrayLengthField extends ArrayLengthOpenField {
        @Override
        public int getLength(Object target) {
            if (target == null) {
                return 0;
            }
            return Array.getLength(target);
        }
    }

    private static class JavaClassClassField implements IOpenField {
        private Class<?> instanceClass;

        public JavaClassClassField(Class<?> instanceClass) {
            this.instanceClass = instanceClass;
        }

        public Object get(Object target, IRuntimeEnv env) {
            return instanceClass;
        }

        public IOpenClass getDeclaringClass() {
            return null;
        }

        public String getDisplayName(int mode) {
            return "class";
        }

        public IMemberMetaInfo getInfo() {
            return null;
        }

        public String getName() {
            return "class";
        }

        public IOpenClass getType() {
            return JavaOpenClass.CLASS;
        }

        public boolean isConst() {
            return true;
        }

        public boolean isReadable() {
            return true;
        }

        public boolean isStatic() {
            return true;
        }

        public boolean isWritable() {
            return false;
        }

        public void set(Object target, Object value, IRuntimeEnv env) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toString() {
            return getName();
        }
    }

    private static class JavaPrimitiveClass extends JavaOpenClass {
        private Class<?> wrapperClass;

        private Object nullObject;

        public JavaPrimitiveClass(Class<?> instanceClass, Class<?> wrapperClass, Object nullObject) {
            super(instanceClass, true);
            this.wrapperClass = wrapperClass;
            this.nullObject = nullObject;
        }

        @Override
        public Object newInstance(IRuntimeEnv env) {
            return nullObject;
        }

        @Override
        public Object nullObject() {
            return nullObject;
        }
    }

    private static class JavaOpenInterface extends JavaOpenClass {

        private Map<Method, BeanOpenField> getters = new HashMap<>();
        private Map<Method, BeanOpenField> setters = new HashMap<>();

        @SuppressWarnings("unused")
        private Class<?> proxyClass;

        private volatile Class<?> generatedImplClass;

        @Override
        protected Map<MethodKey, IOpenMethod> initMethodMap() {
            Map<MethodKey, IOpenMethod> methodMap = new HashMap<>();
            methodMap.putAll(super.initMethodMap());

            for (IOpenMethod om : JavaOpenClass.OBJECT.getMethods()) { // Any interface has Object methods. For example:
                                                                       // toString()
                methodMap.put(new MethodKey(om), om);
            }

            return Collections.unmodifiableMap(methodMap);
        }

        protected JavaOpenInterface(Class<?> instanceClass) {
            super(instanceClass);
            proxyClass = Proxy.getProxyClass(instanceClass.getClassLoader(), instanceClass);

        }

        @Override
        protected Map<Method, BeanOpenField> getGetters() {
            return getters;
        }

        @Override
        protected Map<Method, BeanOpenField> getSetters() {
            return setters;
        }

        @Override
        public Object newInstance(IRuntimeEnv env) {
            try {
                Object res = createCollectionInstance();
                if (res != null) {
                    return res;
                }

                if (generatedImplClass == null) {
                    synchronized (this) {
                        if (generatedImplClass == null) {
                            JavaInterfaceImplBuilder builder = new JavaInterfaceImplBuilder(instanceClass);
                            generatedImplClass = ClassUtils.defineClass(builder.getBeanName(),
                                builder.byteCode(),
                                Thread.currentThread().getContextClassLoader());
                        }
                    }
                }
                return generatedImplClass.newInstance();
            } catch (Exception e) {
                throw RuntimeExceptionWrapper.wrap(e);
            }
        }

        private Object createCollectionInstance() {
            if (List.class.isAssignableFrom(instanceClass)) {
                return new ArrayList<>();
            }
            if (Set.class.isAssignableFrom(instanceClass)) {
                return new HashSet<>();
            }
            if (SortedMap.class.isAssignableFrom(instanceClass)) {
                return new TreeMap<>();
            }
            if (Map.class.isAssignableFrom(instanceClass)) {
                return new HashMap<>();
            }
            if (Collection.class.isAssignableFrom(instanceClass)) {
                return new ArrayList<>();
            }
            return null;
        }

        @Override
        public boolean isInterface() {
            return true;
        }

    }

}