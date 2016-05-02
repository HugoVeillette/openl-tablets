/*
 * Created on Oct 7, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.impl;

import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IParameterDeclaration;
import org.openl.util.StringTool;

/**
 * @author snshor
 *
 */
public class MethodSignature implements IMethodSignature {

    IParameterDeclaration[] parameters;

    /**
     * Copy constructor makes deep copy of ims
     *
     * @param ims
     */
    public MethodSignature(IMethodSignature ims) {
        IOpenClass[] parTypes = ims.getParameterTypes();
        int len = parTypes.length;
        parameters = new IParameterDeclaration[len];
        for (int i = 0; i < len; i++) {
            parameters[i] = new ParameterDeclaration(parTypes[i], ims.getParameterName(i));
        }
    }

    public MethodSignature(IOpenClass[] parTypes) {
        int len = parTypes.length;
        parameters = new IParameterDeclaration[len];
        for (int i = 0; i < len; i++) {
            parameters[i] = new ParameterDeclaration(parTypes[i], "p" + i);
        }
    }

    public MethodSignature(IOpenClass[] parTypes, String[] names) {
        int len = parTypes.length;
        parameters = new IParameterDeclaration[len];
        for (int i = 0; i < len; i++) {
            parameters[i] = new ParameterDeclaration(parTypes[i], names[i]);
        }
    }

    public MethodSignature(IParameterDeclaration[] parameters) {
        this.parameters = parameters;
    }

    public int getNumberOfParameters() {
        return parameters.length;
    }

    public String getParameterName(int i) {
        return parameters[i].getName();
    }
    
    public IOpenClass getParameterType(int i) {
        return parameters[i].getType();
    }

    public IOpenClass[] getParameterTypes() {
        IOpenClass[] parameterTypes = new IOpenClass[parameters.length];

        for (int i = 0; i < parameterTypes.length; i++) {
            parameterTypes[i] = parameters[i].getType();
        }
        return parameterTypes;
    }

    public MethodSignature merge(IParameterDeclaration[] extraParams) {
        return new MethodSignature(merge(parameters, extraParams));
    }
    
    @Override
    public String toString() {
        return StringTool.arrayToStringThroughSymbol(parameters, ",");
    }

    private static IParameterDeclaration[] merge(IParameterDeclaration[] array1, IParameterDeclaration[] array2) {
        if (array1 == null) {
            return array2;
        }
        if (array2 == null || array2.length == 0) {
            return array1;
        }
        int newSize = array1.length + array2.length;

        IParameterDeclaration[] newArray = new IParameterDeclaration[newSize];
        System.arraycopy(array1, 0, newArray, 0, array1.length);
        System.arraycopy(array2, 0, newArray, array1.length, array2.length);
        return newArray;
    }

}
