package org.openl.conf;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;
import java.util.Stack;

import org.openl.OpenL;
import org.openl.binding.impl.Binder;
import org.openl.impl.DefaultCompileContext;
import org.openl.syntax.impl.Parser;
import org.openl.util.Log;
import org.openl.util.RuntimeExceptionWrapper;
import org.openl.vm.SimpleVM;

public abstract class AOpenLBuilder extends BaseOpenLBuilder {

    static class UserContextStack extends ThreadLocal<Stack<IUserContext>> {

        @Override
        protected Stack<IUserContext> initialValue() {
            return new Stack<>();
        }

        public IUserContext pop() {
            return stack().pop();
        }

        public void push(IUserContext ucxt) {
            stack().push(ucxt);
        }

        protected Stack<IUserContext> stack() {
            return get();
        }

        public IUserContext top() {
            return stack().peek();
        }

    }

    public OpenL build(String openl) throws OpenConfigurationException {
        OpenL op = new OpenL();
        op.setName(openl);
        try {
            NoAntOpenLTask naot = getNoAntOpenLTask();

            naot.execute(getUserEnvironmentContext());

            IOpenLConfiguration conf = NoAntOpenLTask.retrieveConfiguration();

            op.setParser(new Parser(conf));

            op.setBinder(new Binder(conf, conf, conf, conf, conf, op));
            op.setVm(new SimpleVM());

            op.setCompileContext(new DefaultCompileContext());
        } catch (Exception ex) {
            throw RuntimeExceptionWrapper.wrap(ex);
        }
        return op;
    }

    public abstract NoAntOpenLTask getNoAntOpenLTask();

    protected Properties getProperties(String openl) {
        URL url = getResourceContext().findClassPathResource(openl.replace('.', '/') + '/' + openl + ".ant.properties");
        if (url == null) {
            return null;
        }
        InputStream is = null;
        try {
            is = url.openStream();
            Properties p = new Properties();
            p.load(is);
            return p;
        } catch (IOException e) {
            throw RuntimeExceptionWrapper.wrap(e);
        } finally {
            try {
                if (is != null) {
                    is.close();
                }
            } catch (Throwable t) {
                Log.error("Error closing stream", t);
            }
        }

    }

    private ClassLoader myClassLoader() {
        ClassLoader oldClassLoader = Thread.currentThread().getContextClassLoader();
        String myName = getClass().getName();
        try {
            oldClassLoader.loadClass(myName);
            return oldClassLoader;
        } catch (ClassNotFoundException e) {
            return getClass().getClassLoader();
        }

    }
}
