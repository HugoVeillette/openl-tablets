package org.openl.binding.impl;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;

/**
 * This binder is used for both: if-then-else statement and ternary q-mark statement.
 * 
 * @author Yury Molchan
 */
public class IfNodeBinder extends ANodeBinder {

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {
        IBoundNode conditionNode = bindChildNode(node.getChild(0), bindingContext);

        IBoundNode checkConditionNode = BindHelper.checkConditionBoundNode(conditionNode, bindingContext);

        if (checkConditionNode != conditionNode)
            return checkConditionNode;

        IBoundNode thenNode = bindChildNode(node.getChild(1), bindingContext);
        IOpenClass type = thenNode.getType();

        IBoundNode elseNode = null;

        if (node.getNumberOfChildren() == 3) {
            // else branch
            elseNode = bindChildNode(node.getChild(2), bindingContext);
            IOpenClass elseType = elseNode.getType();

            CastToWiderType castToWiderType = CastToWiderType.create(bindingContext, type, elseType);

            type = castToWiderType.getWiderType();
            IOpenCast cast1 = castToWiderType.getCast1();
            if (cast1 != null) {
                thenNode = new CastNode(null, thenNode, cast1, type);
            }
            IOpenCast cast2 = castToWiderType.getCast2();
            if (cast2 != null) {
                elseNode = new CastNode(null, elseNode, cast2, type);
            }
        }

        return new IfNode(node, conditionNode, thenNode, elseNode, type);
    }

}
