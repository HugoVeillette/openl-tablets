/**
 * Created Dec 3, 2006
 */
package org.openl.vm;

import org.openl.types.Invokable;

/**
 * @author Yury Molchan
 */
public class Tracer {
    protected static Tracer instance = new Tracer();

    protected void doPut(Object source, String id, Object... args) {
        // Nothing
    }

    protected <T, E extends IRuntimeEnv, R> R doInvoke(Invokable<? super T, E> executor,
            T target,
            Object[] params,
            E env,
            Object source) {
        return executor.invoke(target, params, env);
    }

    protected <T> T doWrap(Object source, T target, Object[] args) {
        return target;
    }

    public static void put(Object source, String id, Object... args) {
        instance.doPut(source, id, args);
    }
    
    public boolean isOn() {
        return false;
    }
    
    public static boolean isEnabled() {
        return instance.isOn();
    }

    public static <T, E extends IRuntimeEnv, R> R invoke(Invokable<? super T, E> executor,
            T target,
            Object[] params,
            E env,
            Object source) {
        return instance.doInvoke(executor, target, params, env, source);
    }

    public static <T> T wrap(Object source, T target, Object... args) {
        return instance.doWrap(source, target, args);
    }
}
