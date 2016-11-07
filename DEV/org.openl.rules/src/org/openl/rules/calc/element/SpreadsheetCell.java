package org.openl.rules.calc.element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.ICell;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.Invokable;
import org.openl.types.NullOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.NumberUtils;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetCell implements Invokable {

    private int rowIndex;
    private int columnIndex;
    private ICell sourceCell;

    private SpreadsheetCellType kind = SpreadsheetCellType.EMPTY;
    private Object value;
    private IOpenClass type;

    private IOpenMethod method;

    public SpreadsheetCell(int rowIndex, int columnIndex, ICell sourceCell) {
        this.rowIndex = rowIndex;
        this.columnIndex = columnIndex;
        this.sourceCell = sourceCell;
    }

    public ICell getSourceCell() {
        return sourceCell;
    }

    public int getColumnIndex() {
        return columnIndex;
    }

    public SpreadsheetCellType getKind() {
        return kind;
    }

    public IOpenMethod getMethod() {
        return method;
    }

    public int getRowIndex() {
        return rowIndex;
    }

    public IOpenClass getType() {
        return type;
    }

    public Object getValue() {
        return value;
    }

    public boolean isEmpty() {
        return kind == SpreadsheetCellType.EMPTY;
    }

    public boolean isMethodCell() {
        return kind == SpreadsheetCellType.METHOD;
    }

    public boolean isValueCell() {
        return kind == SpreadsheetCellType.VALUE;
    }

    public void setKind(SpreadsheetCellType kind) {
        this.kind = kind;
    }

    public void setMethod(IOpenMethod method) {
        this.method = method;
    }

    public void setType(IOpenClass type) {
        if (type == null)
            return;
        if (type.equals(NullOpenClass.the)) {
            type = JavaOpenClass.OBJECT;
        } else if (type == JavaOpenClass.VOID)
            type = JavaOpenClass.getOpenClass(Void.class);
        else if (type.getInstanceClass().isPrimitive()) {
            Class<?> wrapper = NumberUtils.getWrapperType(type.getInstanceClass().getName());
            type = JavaOpenClass.getOpenClass(wrapper);
        }
        this.type = type;

        // Add cell type meta info
        if (sourceCell != null) {
            String formattedValue = sourceCell.getFormattedValue();

            if (formattedValue.startsWith("=")) {
                CellMetaInfo metaInfo = sourceCell.getMetaInfo();
                if (metaInfo == null) {
                    metaInfo = new CellMetaInfo(CellMetaInfo.Type.DT_CA_CODE,
                            null,
                            JavaOpenClass.STRING,
                            false,
                            Collections.<NodeUsage>emptyList());
                }

                List<NodeUsage> nodeUsages = new ArrayList<NodeUsage>();
                String description = "Cell type: " + type.getDisplayName(0);
                int from = formattedValue.indexOf('=');
                nodeUsages.add(new SimpleNodeUsage(from, from + 1, description, null, NodeType.OTHER));
                nodeUsages.addAll(metaInfo.getUsedNodes());

                metaInfo.setUsedNodes(nodeUsages);
                sourceCell.setMetaInfo(metaInfo);
            }
        }
    }

    public void setValue(Object value) {
        if (value == null) {
        } else if (value instanceof IOpenMethod) {
            this.method = (IOpenMethod) value;
        } else {
            this.value = value;
        }
    }

    public Object invoke(Object spreadsheetResult, Object[] params, IRuntimeEnv env) {
        if (isValueCell()) {
            Object value = getValue();
            return value;
        } else if (isMethodCell()) {
            return getMethod().invoke(spreadsheetResult, params, env);
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "R" + getRowIndex() + "C" + getColumnIndex();
    }
}
