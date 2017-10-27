package org.openl.rules.method;

import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.types.Invokable;
import org.openl.vm.IRuntimeEnv;

/**
 * Default implementation for invokers supporting tracing.
 *
 * @author Yury Molchan
 */
public abstract class RulesMethodInvoker<T extends ExecutableRulesMethod> implements Invokable {

    private T invokableMethod;

    protected RulesMethodInvoker(T invokableMethod) {
        this.invokableMethod = invokableMethod;
    }

    public final Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        // check if the object can be invoked
        if (!canInvoke()) {
            // object can`t be invoked, inform user about the problem.
            TableSyntaxNode syntaxNode = getInvokableMethod().getSyntaxNode();
            if (syntaxNode != null) {
                throw new OpenLRuntimeException(syntaxNode.getErrors()[0]);
            } else {
                throw new OpenLRuntimeException("Method can't be invoked");
            }
        } else {
            // simple run invoke
            return invokeSimple(target, params, env);
        }
    }

    public T getInvokableMethod() {
        return invokableMethod;
    }

    /**
     * Checks if it is possible to invoke invokable object.
     */
    abstract protected boolean canInvoke();

    /**
     * Invoke for simple run operation.
     */
    abstract protected Object invokeSimple(Object target, Object[] params, IRuntimeEnv env);
}
