package org.openl.rules.calc;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.binding.CustomDynamicOpenClass;
import org.openl.rules.table.Point;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class CustomSpreadsheetResultOpenClass extends ADynamicClass implements CustomDynamicOpenClass {

    private String[] rowNames;
    private String[] columnNames;
    private String[] rowTitles;
    private String[] columnTitles;
    private Map<String, Point> fieldCoordinates = new HashMap<String, Point>();

    public CustomSpreadsheetResultOpenClass(String name,
            String[] rowNames,
            String[] columnNames,
            String[] rowTitles,
            String[] columnTitles,
            Map<String, Point> fieldCoordinates) {
        super(name, SpreadsheetResult.class);
        if (fieldCoordinates == null) {
            throw new IllegalArgumentException();
        }
        if (rowNames == null) {
            throw new IllegalArgumentException();
        }
        if (columnNames == null) {
            throw new IllegalArgumentException();
        }
        if (rowTitles == null) {
            throw new IllegalArgumentException();
        }
        if (columnTitles == null) {
            throw new IllegalArgumentException();
        }
        this.fieldCoordinates = new HashMap<String, Point>(fieldCoordinates);
        this.rowNames = rowNames.clone();
        this.columnNames = columnNames.clone();
        this.columnTitles = columnTitles.clone();
        this.rowTitles = rowTitles.clone();
    }

    private Iterable<IOpenClass> superClasses = null;

    private static IOpenClass spreadsheetResultOpenClass = JavaOpenClass.createNewOpenClass(SpreadsheetResult.class);

    @Override
    public IAggregateInfo getAggregateInfo() {
        return DynamicArrayAggregateInfo.aggregateInfo;
    }

    public synchronized Iterable<IOpenClass> superClasses() {
        if (superClasses == null) {
            Class<?>[] interfaces = SpreadsheetResult.class.getInterfaces();
            Class<?> superClass = SpreadsheetResult.class;
            List<IOpenClass> superClasses = new ArrayList<IOpenClass>(interfaces.length + 1);
            if (superClass != null) {
                superClasses.add(spreadsheetResultOpenClass);
            }
            for (Class<?> interf : interfaces) {
                superClasses.add(JavaOpenClass.getOpenClass(interf));
            }
            this.superClasses = superClasses;
        }
        return superClasses;
    }

    public void extendSpreadsheetResult(String[] rowNames,
            String[] columnNames,
            String[] rowTitles,
            String[] columnTitles,
            Map<String, Point> fieldCoordinates,
            Collection<IOpenField> fields) {
        List<String> nRowNames = new ArrayList<String>(Arrays.asList(this.rowNames));
        Set<String> existedRowNamesSet = new HashSet<String>(Arrays.asList(this.rowNames));
        List<String> nColumnNames = new ArrayList<String>(Arrays.asList(this.columnNames));
        Set<String> existedColumnNamesSet = new HashSet<String>(Arrays.asList(this.columnNames));

        List<String> nRowTitles = new ArrayList<String>(Arrays.asList(this.rowTitles));
        List<String> nColumnTitles = new ArrayList<String>(Arrays.asList(this.columnTitles));

        boolean fieldCoordinatesRequresUpdate = false;

        for (int i = 0; i < rowNames.length; i++) {
            if (!existedRowNamesSet.contains(rowNames[i])) {
                nRowNames.add(rowNames[i]);
                nRowTitles.add(rowTitles[i]);
                fieldCoordinatesRequresUpdate = true;
            }
        }

        for (int i = 0; i < columnNames.length; i++) {
            if (!existedColumnNamesSet.contains(columnNames[i])) {
                nColumnNames.add(columnNames[i]);
                nColumnTitles.add(columnTitles[i]);
                fieldCoordinatesRequresUpdate = true;
            }
        }

        if (fieldCoordinatesRequresUpdate) {
            Set<String> newFieldNames = new HashSet<String>();
            for (int i = 0; i < nRowNames.size(); i++) {
                for (int j = this.columnNames.length; j < nColumnNames.size(); j++) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                        .append(nColumnNames.get(j))
                        .append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                        .append(nRowNames.get(i));
                    this.fieldCoordinates.put(sb.toString(), new Point(j, i));
                    newFieldNames.add(sb.toString());
                }
            }

            for (int i = this.rowNames.length; i < nRowNames.size(); i++) {
                for (int j = 0; j < nColumnNames.size(); j++) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                        .append(nColumnNames.get(j))
                        .append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                        .append(nRowNames.get(i));
                    this.fieldCoordinates.put(sb.toString(), new Point(j, i));
                    newFieldNames.add(sb.toString());
                }
            }

            for (int i = this.rowNames.length; i < nRowNames.size(); i++) {
                for (int j = this.columnNames.length; j < nColumnNames.size(); j++) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                        .append(nColumnNames.get(j))
                        .append(SpreadsheetStructureBuilder.DOLLAR_SIGN)
                        .append(nRowNames.get(i));
                    this.fieldCoordinates.put(sb.toString(), new Point(j, i));
                    newFieldNames.add(sb.toString());
                }
            }
            this.rowNames = nRowNames.toArray(new String[] {});
            this.columnNames = nColumnNames.toArray(new String[] {});
            this.rowTitles = nRowTitles.toArray(new String[] {});
            this.columnTitles = nColumnTitles.toArray(new String[] {});
            for (IOpenField field : fields) {
                if (newFieldNames.contains(field.getName())) {
                    addField(field);
                }
            }
        }
    }

    public String[] getRowNames() {
        return rowNames.clone();
    }

    public String[] getColumnNames() {
        return columnNames.clone();
    }

    public String[] getRowTitles() {
        return rowTitles;
    }

    public String[] getColumnTitles() {
        return columnTitles;
    }

    public Map<String, Point> getFieldCoordinates() {
        return Collections.unmodifiableMap(fieldCoordinates);
    }

    @Override
    public IOpenClass copy() {
        return copyCustomSpreadsheetResult();
    }

    @Override
    public void updateOpenClass(IOpenClass openClass) {
        CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) openClass;
        customSpreadsheetResultOpenClass.extendSpreadsheetResult(getRowNames(),
            getColumnNames(),
            getRowTitles(),
            getColumnTitles(),
            getFieldCoordinates(),
            getFields().values());
        validate(customSpreadsheetResultOpenClass, getFields().values());
    }

    private void validate(CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass,
            Collection<IOpenField> values) {
        List<String> errorMessages = new ArrayList<String>();
        for (IOpenField field : values) {
            IOpenField existedField = customSpreadsheetResultOpenClass.getField(field.getName());
            if (!existedField.getType().isAssignableFrom(field.getType())) {
                errorMessages.add(getName() + "." + field.getName() + "(expected: " + existedField.getType()
                    .getName() + ", found: " + field.getType().getName() + ")");
            }
        }
        if (!errorMessages.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String errorMessage : errorMessages) {
                if (!first) {
                    sb.append(", ");
                } else {
                    first = false;
                }
                sb.append(errorMessage);
            }
            throw new OpenlNotCheckedException("Incompatible type usage in spreadsheet fields: " + sb.toString());
        }
    }

    private CustomSpreadsheetResultOpenClass copyCustomSpreadsheetResult() {
        CustomSpreadsheetResultOpenClass type = new CustomSpreadsheetResultOpenClass(getName(),
            getRowNames(),
            getColumnNames(),
            getRowTitles(),
            getColumnTitles(),
            getFieldCoordinates());
        for (IOpenField field : getFields().values()) {
            type.addField(field);
        }
        return type;
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        Object[][] result = new Object[rowNames.length][columnNames.length];
        return new SpreadsheetResult(result, rowNames, columnNames, rowTitles, columnTitles, fieldCoordinates);
    }

}
