package org.openl.extension.xmlrules.binding;

import org.openl.binding.IBoundNode;
import org.openl.binding.impl.ATargetBoundNode;
import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

public class IfErrorFunctionBoundNode extends ATargetBoundNode {

    public IfErrorFunctionBoundNode(ISyntaxNode syntaxNode,
            IBoundNode[] child) {
        super(syntaxNode, child, null);
    }

    @Override
    protected Object evaluateRuntime(IRuntimeEnv env) {

        try {
            try {
                return children[0].evaluate(env);
            } catch (RuntimeException e) {
                return children[1].evaluate(env);
            }
        } catch (OpenLRuntimeException opex) {
            opex.pushMethodNode(this);
            throw opex;
        }

    }

    public IOpenClass getType() {
        return children[0].getType();
    }

}
