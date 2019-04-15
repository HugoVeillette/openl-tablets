/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.data;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.openl.OpenL;
import org.openl.types.IOpenClass;

/**
 * @author snshor
 *
 */
public class OpenlBasedDataTableModel implements ITableModel {

    private String name;
    private IOpenClass type;
    private OpenL openl;
    private ColumnDescriptor[] columnDescriptors;
    private boolean hasColumnTitleRow;

    public OpenlBasedDataTableModel(String name,
            IOpenClass type,
            OpenL openl,
            ColumnDescriptor[] columnDescriptor,
            boolean hasColumnTitleRow) {
        this.name = name;
        this.type = type;
        this.openl = openl;
        this.columnDescriptors = initializeDescriptors(columnDescriptor);
        this.hasColumnTitleRow = hasColumnTitleRow;
    }

    private static ColumnDescriptor[] initializeDescriptors(ColumnDescriptor[] descriptors) {
        //group descriptors by KEY and put PK columns in first position of each group
        int cntDescriptors = 0;
        Map<ColumnDescriptor.ColumnGroupKey, List<ColumnDescriptor>> descriptorGroups = new TreeMap<>();
        for (ColumnDescriptor descriptor : descriptors) {
            if (descriptor == null) {
                continue;
            }
            cntDescriptors++;
            ColumnDescriptor.ColumnGroupKey key = descriptor.buildGroupKey();
            List<ColumnDescriptor> descriptorsByKey = descriptorGroups.computeIfAbsent(key, k -> new LinkedList<>());
            if (descriptor.isPrimaryKey()) {
                descriptorsByKey.add(0, descriptor);
            } else {
                descriptorsByKey.add(descriptor);
            }
        }

        //transform map to flat array and keep order
        ColumnDescriptor[] res = new ColumnDescriptor[cntDescriptors];
        int i = 0;
        for (Map.Entry<ColumnDescriptor.ColumnGroupKey, List<ColumnDescriptor>> e : descriptorGroups.entrySet()) {
            ColumnDescriptor.ColumnGroupKey key = e.getKey();
            for (ColumnDescriptor descriptor : e.getValue()) {
                descriptor.setGroupKey(key);
                res[i] = descriptor;
                i++;
            }
        }
        return res;
    }

    @Override
    public boolean hasColumnTitleRow() {
        return hasColumnTitleRow;
    }

    @Override
    public ColumnDescriptor[] getDescriptors() {
        return columnDescriptors;
    }

    @Override
    public Class<?> getInstanceClass() {
        return type.getInstanceClass();
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public IOpenClass getType() {
        return type;
    }

    @Override
    public Object newInstance() {
        return type.newInstance(openl.getVm().getRuntimeEnv());
    }

    @Override
    public ColumnDescriptor getDescriptor(int idx) {
        for (ColumnDescriptor descriptor : columnDescriptors) {
            if (descriptor.getColumnIdx() == idx) {
                return descriptor;
            }
        }
        return null;
    }
}
