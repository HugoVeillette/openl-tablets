/*
 * Created on Jun 16, 2003 Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.binding.impl;

import java.util.HashSet;
import java.util.Set;

import org.openl.binding.IBindingContext;
import org.openl.binding.IBoundNode;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.syntax.ISyntaxNode;
import org.openl.types.IOpenClass;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;

/**
 * @author snshor
 *
 */
public class QMarkNodeBinder extends ANodeBinder {

    public IBoundNode bind(ISyntaxNode node, IBindingContext bindingContext) throws Exception {

        IBoundNode[] children = new IBoundNode[3];
        children[0] = bindChildNode(node.getChild(0), bindingContext);

        IBoundNode conditionNode = children[0];

        IBoundNode checkConditionNode = BindHelper.checkConditionBoundNode(conditionNode, bindingContext);

        if (checkConditionNode != conditionNode)
            return checkConditionNode;

        children[1] = bindChildNode(node.getChild(1), bindingContext);
        children[2] = bindChildNode(node.getChild(2), bindingContext);
        IOpenClass type1 = children[1].getType();
        IOpenClass type2 = children[2].getType();

        IOpenClass type = type1;
        if (NullOpenClass.the.equals(type1)) {
            type = type2;
        } else {
            IOpenCast cast1To2 = bindingContext.getCast(type1, type2);
            IOpenCast cast2To1 = bindingContext.getCast(type2, type1);

            if (cast1To2 == null && cast2To1 == null) {
                // Find parent class for cast both nodes
                IOpenClass parentClass = findParentClass(type1, type2);
                if (parentClass != null) {
                    type = parentClass;
                    IOpenCast castToParent1 = bindingContext.getCast(type1, parentClass);
                    children[1] = new CastNode(null, children[1], castToParent1, parentClass);
                    IOpenCast castToParent2 = bindingContext.getCast(type2, parentClass);
                    children[2] = new CastNode(null, children[2], castToParent2, parentClass);
                }
            } else {
                if ((cast1To2 == null || !cast1To2.isImplicit()) && cast2To1 != null && cast2To1.isImplicit()) {
                    children[2] = new CastNode(null, children[2], cast2To1, type1);
                } else {
                    if ((cast2To1 == null || !cast2To1.isImplicit()) && cast1To2 != null && cast1To2.isImplicit()) {
                        children[1] = new CastNode(null, children[1], cast1To2, type2);
                        type = type2;
                    } else {
                        if (cast1To2 != null && cast2To1 != null && cast1To2.isImplicit() && cast2To1.isImplicit()) {
                            if (cast1To2.getDistance(type1, type2) < cast2To1
                                .getDistance(type2, type1)) {
                                children[1] = new CastNode(null, children[1], cast1To2, type2);
                                type = type2;
                            } else {
                                children[2] = new CastNode(null, children[2], cast2To1, type1);
                            }
                        }
                    }
                }
            }
        }

        return new QMarkNode(node, children, type);
    }

    private IOpenClass findParentClass(IOpenClass child1Type, IOpenClass child2Type) {
        Set<IOpenClass> superClasses = new HashSet<IOpenClass>();
        IOpenClass openClass = child1Type;
        while (!openClass.equals(JavaOpenClass.OBJECT)) {
            Iterable<IOpenClass> itr = openClass.superClasses();
            boolean f = false;
            for (IOpenClass superClass : itr) {
                if (!superClass.getInstanceClass().isInterface()) {
                    superClasses.add(superClass);
                    openClass = superClass;
                    f = true;
                    break;
                }
            }
            if (!f){
                break;
            }
        }
        openClass = child2Type;
        while (!openClass.equals(JavaOpenClass.OBJECT)) {
            Iterable<IOpenClass> itr = openClass.superClasses();
            boolean f = false;
            for (IOpenClass superClass : itr) {
                if (!superClass.getInstanceClass().isInterface()) {
                    if (superClasses.contains(superClass)) {
                        return superClass;
                    }
                    openClass = superClass;
                    f = true;
                    break;
                }
            }
            if (!f){
                break;
            }
        }
        return null;
    }

}
