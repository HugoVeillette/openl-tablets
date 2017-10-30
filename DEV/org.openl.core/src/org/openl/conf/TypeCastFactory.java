/*
 * Created on Jun 11, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.conf;

import java.util.ArrayList;
import java.util.Collection;

import org.openl.binding.ICastFactory;
import org.openl.binding.impl.StaticClassLibrary;
import org.openl.binding.impl.cast.CastFactory;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public class TypeCastFactory extends AConfigurationElement implements IConfigurationElement {

    public static class JavaCastComponent extends AConfigurationElement {
        String libraryClassName;
        String className;

        CastFactory factory = null;

        public synchronized CastFactory getCastFactory(IConfigurableResourceContext cxt) {
            if (factory == null) {
                Class<?> libClass = ClassFactory
                    .validateClassExistsAndPublic(libraryClassName, cxt.getClassLoader(), getUri());
                Class<?> implClass = ClassFactory
                    .validateClassExistsAndPublic(className, cxt.getClassLoader(), getUri());

                factory = (CastFactory) ClassFactory.newInstance(implClass, getUri()); // Strange
                                                                                       // reflection
                                                                                       // logic
                                                                                       // with
                                                                                       // implementation
                                                                                       // cast!
                factory.setMethodFactory(new StaticClassLibrary(JavaOpenClass.getOpenClass(libClass)));
            }
            return factory;
        }

        /**
         * @param string
         */
        public void setClassName(String string) {
            className = string;
        }

        /**
         * @param string
         */
        public void setLibraryClassName(String string) {
            libraryClassName = string;
        }

        /*
         * (non-Javadoc)
         *
         * @see
         * org.openl.newconf.IConfigurationElement#validate(org.openl.newconf.
         * IConfigurationContext)
         */
        public void validate(IConfigurableResourceContext cxt) throws OpenConfigurationException {
            ClassFactory.validateClassExistsAndPublic(libraryClassName, cxt.getClassLoader(), getUri());
            Class<?> implClass = ClassFactory.validateClassExistsAndPublic(className, cxt.getClassLoader(), getUri());

            ClassFactory.validateSuper(implClass, CastFactory.class, getUri());

            ClassFactory.validateHaveNewInstance(implClass, getUri());
        }

    }

    ArrayList<JavaCastComponent> components = new ArrayList<JavaCastComponent>();

    public void addJavaCast(JavaCastComponent cmp) {
        components.add(cmp);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.binding.ICastFactory#getCast(org.openl.types.IOpenClass,
     * org.openl.types.IOpenClass)
     */
    public IOpenCast getCast(IOpenClass from, IOpenClass to, IConfigurableResourceContext cxt) {
        for (JavaCastComponent component : components) {
            IOpenCast openCast = component.getCastFactory(cxt).getCast(from, to);
            if (openCast != null) {
                return openCast;
            }
        }
        return null;
    }

    public IOpenClass getImplicitCastableClass(IOpenClass openClass1,
            IOpenClass openClass2,
            final IConfigurableResourceContext cxt) {
        Collection<IOpenMethod> allMethods = new ArrayList<>();
        for (JavaCastComponent component : components) {
            CastFactory castFactory = component.getCastFactory(cxt);
            Iterable<IOpenMethod> methods = castFactory.getMethodFactory().methods(CastFactory.AUTO_CAST_METHOD_NAME);
            for (IOpenMethod method : methods) {
                allMethods.add(method);
            }
        }
        return CastFactory.getImplicitCastableClass(openClass1, openClass2, new ICastFactory() {
            @Override
            public IOpenClass getImplicitCastableClass(IOpenClass openClass1, IOpenClass openClass2) {
                throw new UnsupportedOperationException();
            }

            @Override
            public IOpenCast getCast(IOpenClass from, IOpenClass to) {
                return TypeCastFactory.this.getCast(from, to, cxt);
            }
        }, allMethods);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.newconf.IConfigurationElement#validate(org.openl.newconf.
     * IConfigurationContext)
     */
    public void validate(IConfigurableResourceContext cxt) throws OpenConfigurationException {
        for (JavaCastComponent component : components) {
            component.validate(cxt);
        }
    }

}
