/*
 * Created on Jul 10, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.types.java;

import java.util.*;

import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenIndex;

public class JavaMapAggregateInfo implements IAggregateInfo {

    static class MapIndex implements IOpenIndex {
        public IOpenClass getElementType() {
            return JavaOpenClass.OBJECT;
        }

        public IOpenClass getIndexType() {
            return JavaOpenClass.OBJECT;
        }

        @Override
        public Collection getIndexes(Object container) {
            return ((Map) container).keySet();
        }

        @SuppressWarnings("unchecked")
        public Object getValue(Object container, Object index) {
            return ((Map<Object, Object>) container).get(index);
        }

        public boolean isWritable() {
            return true;
        }

        @SuppressWarnings("unchecked")
        public void setValue(Object container, Object index, Object value) {
            ((Map<Object, Object>) container).put(index, value);
        }
    }

    static final IAggregateInfo MAP_AGGREGATE = new JavaMapAggregateInfo();

    public IOpenClass getComponentType(IOpenClass aggregateType) {
        return JavaOpenClass.getOpenClass(Map.Entry.class);
    }

    @Override
    public IOpenIndex getIndex(IOpenClass aggregateType) {
        return getIndex(aggregateType, JavaOpenClass.OBJECT);
    }

    public IOpenIndex getIndex(IOpenClass aggregateType, IOpenClass indexType) {
        if (!isAggregate(aggregateType)) {
            return null;
        }

        return new MapIndex();
    }

    @SuppressWarnings("unchecked")
    public Iterator<Object> getIterator(Object aggregate) {
        return ((Map) aggregate).entrySet().iterator();
    }

    public boolean isAggregate(IOpenClass type) {
        return Map.class.isAssignableFrom(type.getInstanceClass());
    }

    @Override
    public IOpenClass getIndexedAggregateType(IOpenClass componentType, int dims) {
        return JavaOpenClass.getOpenClass(Map.class);
    }

    @Override
    public Object makeIndexedAggregate(IOpenClass componentType, int[] dimValues) {
        if (dimValues.length > 1) {
            throw new UnsupportedOperationException("Only one dimensional Java Maps are supported.");
        }

        return new HashMap(dimValues[0]);
    }
}
