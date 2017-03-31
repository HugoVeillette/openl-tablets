package org.openl.types.java;

import java.util.Iterator;

import org.openl.domain.FixedSizeDomain;
import org.openl.domain.IType;
import org.openl.util.OpenIterator;

public class JavaEnumDomain extends FixedSizeDomain<Object> {
    
    private JavaOpenEnum enumClass;
    
    public JavaEnumDomain(JavaOpenEnum enumClass) {
        this.enumClass = enumClass;
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    public Iterator iterator() {
        return OpenIterator.fromArray(enumClass.getInstanceClass().getEnumConstants());
    }

    public int size() {
        return enumClass.getInstanceClass().getEnumConstants().length;
    }

    public IType getElementType() {
        return enumClass;
    }

    public boolean selectObject(Object obj) {
        return enumClass.getInstanceClass().isInstance(obj);
    }

    public boolean selectType(IType type) {
        return type == enumClass;
    }

    public Object getValue(int index) {
        return enumClass.getInstanceClass().getEnumConstants()[index];
    }

    public JavaOpenEnum getEnumClass() {
        return enumClass;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        @SuppressWarnings("rawtypes")
        Iterator itr = iterator();
        boolean f = false;
        while (itr.hasNext()) {
            Object v = itr.next();
            if (f) {
                sb.append(", ");
            } else {
                f = true;
            }
            sb.append(v.toString());
        }
        return "[" + sb.toString() + "]";
    }

}
