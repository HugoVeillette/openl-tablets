package org.openl.binding.impl.cast;

import org.openl.types.IOpenClass;

public class JavaUnboxingCast implements IOpenCast {

    public Object convert(Object from) {
        return from;
    }

    public int getDistance(IOpenClass from, IOpenClass to) {
        return CastFactory.JAVA_UNBOXING_CAST_DISTANCE;
    }

    public boolean isImplicit() {
        return true;
    }

}
