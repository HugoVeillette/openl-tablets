/*
 * Created on Mar 9, 2004
 *
 * Developed by OpenRules Inc. 2003-2004
 */

package org.openl.types.impl;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenIndex;
import org.openl.util.IntegerValuesUtils;

public class ArrayFieldIndex implements IOpenIndex {
    private IOpenClass elementType;
    private IOpenField indexField;

    public ArrayFieldIndex(IOpenClass elementType, IOpenField indexField) {
        this.elementType = elementType;
        this.indexField = indexField;
    }

    public IOpenClass getElementType() {
        return elementType;
    }

    public IOpenClass getIndexType() {
        return indexField.getType();
    }

    @Override
    public Collection getIndexes(Object container) {
        int len = Array.getLength(container);
        List<Object> indexes = new ArrayList<>(len);

        for (int i = 0; i < len; i++) {
            indexes.add(indexField.get(Array.get(container, i), null));
        }

        return indexes;
    }

    public Object getValue(Object container, Object index) {
        if (index != null) {
            int len = Array.getLength(container);

            for (int i = 0; i < len; i++) {
                Object obj = Array.get(container, i);

                Object fieldValue = indexField.get(obj, null);
                
                // handles the case when index field of Datatype is of type int, and we try to get String index
                // e.g. person["12"], so we need to try cast String index value to Integer, and then compare them.
                // see DatatypeArrayTest
                if (index instanceof String &&
                        IntegerValuesUtils.isIntegerValue(fieldValue.getClass()) /*fieldValue instanceof Integer*/) {
                    index = IntegerValuesUtils.createNewObjectByType(fieldValue.getClass(),(String)index);//castStringToInteger((String)index);
                }
                
                if (fieldValue.equals(index)) {
                    return obj;
                }
            }
        }
        return null;
    }

//    private Object castStringToInteger(String index) {
//        try {
//            return Integer.valueOf(index);
//        } catch (NumberFormatException e) {
//            // we can`t cast, means there is no Integer value inside.
//        }
//        return index;
//    }

    public boolean isWritable() {
        return false;
    }

    public void setValue(Object container, Object index, Object value) {
        throw new UnsupportedOperationException();
    }

}