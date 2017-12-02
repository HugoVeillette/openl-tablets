/*
 * Created on May 19, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding;

import org.openl.exception.OpenLRuntimeException;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.vm.IRuntimeEnv;

/**
 * @author snshor
 */
public interface IBoundNode {

    void assign(Object value, IRuntimeEnv env) throws OpenLRuntimeException;

    Object evaluate(IRuntimeEnv env) throws OpenLRuntimeException;

    IBoundNode[] getChildren();

    ISyntaxNode getSyntaxNode();

    IBoundNode getTargetNode();

    IOpenClass getType();

    // Lvalue operations
    boolean isLvalue();
    
    /**
     * Static target will accept only static methods; vice-versa is not necessarily true, but should produce at least a warning
     * @return
     */
    boolean isStaticTarget();

    void updateAssignFieldDependency(BindingDependencies dependencies);

    void updateDependency(BindingDependencies dependencies);

}
