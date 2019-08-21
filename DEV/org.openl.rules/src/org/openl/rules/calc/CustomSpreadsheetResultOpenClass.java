package org.openl.rules.calc;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.datatype.gen.JavaBeanClassBuilder;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.table.Point;
import org.openl.types.IAggregateInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.ADynamicClass;
import org.openl.types.impl.DynamicArrayAggregateInfo;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;
import org.openl.vm.IRuntimeEnv;

public class CustomSpreadsheetResultOpenClass extends ADynamicClass {

    private String[] rowNames;
    private String[] columnNames;
    private String[] rowNamesMarkedWithStar;
    private String[] columnNamesMarkedWithStar;
    private List<Pair<String[], String[]>> rowAndColumnNamesMarkedWithStarHistory;
    private String[] rowTitles;
    private String[] columnTitles;
    private Map<String, Point> fieldsCoordinates;
    private XlsModuleOpenClass module;
    private volatile Class<?> beanClass;
    private volatile SpreadsheetResultValueSetter[] spreadsheetResultValueSetters;
    private boolean simpleRefBeanByRow;
    private boolean simpleRefBeanByColumn;
    private long columnsWithStarCount;
    private long rowsWithStarCount;

    public CustomSpreadsheetResultOpenClass(String name,
            String[] rowNames,
            String[] columnNames,
            String[] rowNamesMarkedWithStar,
            String[] columnNamesMarkedWithStar,
            String[] rowTitles,
            String[] columnTitles,
            XlsModuleOpenClass module) {
        super(name, SpreadsheetResult.class);
        Objects.requireNonNull(rowNames);
        Objects.requireNonNull(columnNames);
        Objects.requireNonNull(rowNamesMarkedWithStar);
        Objects.requireNonNull(columnNamesMarkedWithStar);
        Objects.requireNonNull(rowTitles);
        Objects.requireNonNull(columnTitles);
        Objects.requireNonNull(module);
        this.rowNames = rowNames.clone();
        this.columnNames = columnNames.clone();
        this.rowNamesMarkedWithStar = rowNamesMarkedWithStar.clone();
        this.columnNamesMarkedWithStar = columnNamesMarkedWithStar.clone();

        this.columnsWithStarCount = Arrays.stream(columnNamesMarkedWithStar).filter(Objects::nonNull).count();
        this.rowsWithStarCount = Arrays.stream(rowNamesMarkedWithStar).filter(Objects::nonNull).count();

        this.simpleRefBeanByRow = columnsWithStarCount == 1 && rowsWithStarCount > 1;
        this.simpleRefBeanByColumn = rowsWithStarCount == 1 && columnsWithStarCount > 1;

        this.rowAndColumnNamesMarkedWithStarHistory = new ArrayList<>();
        this.rowAndColumnNamesMarkedWithStarHistory
            .add(Pair.of(this.columnNamesMarkedWithStar, this.rowNamesMarkedWithStar));

        this.rowTitles = rowTitles.clone();
        this.columnTitles = columnTitles.clone();
        this.fieldsCoordinates = SpreadsheetResult.buildFieldsCoordinates(this.columnNames, this.rowNames);
        this.module = module;
    }

    public CustomSpreadsheetResultOpenClass(String name) {
        super(name, SpreadsheetResult.class);
    }

    private Iterable<IOpenClass> superClasses = null;

    @Override
    public IAggregateInfo getAggregateInfo() {
        return DynamicArrayAggregateInfo.aggregateInfo;
    }

    public boolean isEmptyBean() {
        return getBeanClass().getDeclaredFields().length == 0;
    }

    @Override
    public synchronized Iterable<IOpenClass> superClasses() {
        if (superClasses == null) {
            Class<?>[] interfaces = SpreadsheetResult.class.getInterfaces();
            List<IOpenClass> superClasses = new ArrayList<>(interfaces.length + 1);
            for (Class<?> interf : interfaces) {
                superClasses.add(JavaOpenClass.getOpenClass(interf));
            }
            this.superClasses = superClasses;
        }
        return superClasses;
    }

    public XlsModuleOpenClass getModule() {
        return module;
    }

    public void extendSpreadsheetResult(String[] rowNames,
            String[] columnNames,
            String[] rowNamesMarkedWithStar,
            String[] columnNamesMarkedWithStar,
            String[] rowTitles,
            String[] columnTitles,
            Collection<IOpenField> fields) {
        if (beanClass != null) {
            throw new IllegalStateException(
                "Bean class for custom spreadsheet result has already been generated. Spreasheet result can't be extended.");
        }

        List<String> nRowNames = new ArrayList<>(Arrays.asList(this.rowNames));
        List<String> nRowNamesMarkedWithStar = new ArrayList<>(Arrays.asList(this.rowNamesMarkedWithStar));
        Set<String> existedRowNamesSet = new HashSet<>(Arrays.asList(this.rowNames));
        List<String> nColumnNames = new ArrayList<>(Arrays.asList(this.columnNames));
        List<String> nColumnNamesMarkedWithStar = new ArrayList<>(Arrays.asList(this.columnNamesMarkedWithStar));
        Set<String> existedColumnNamesSet = new HashSet<>(Arrays.asList(this.columnNames));

        List<String> nRowTitles = new ArrayList<>(Arrays.asList(this.rowTitles));
        List<String> nColumnTitles = new ArrayList<>(Arrays.asList(this.columnTitles));

        boolean fieldCoordinatesRequresUpdate = false;
        boolean rowColumnsWithStartRequiresUpdate = false;

        for (int i = 0; i < rowNames.length; i++) {
            if (!existedRowNamesSet.contains(rowNames[i])) {
                nRowNames.add(rowNames[i]);
                nRowNamesMarkedWithStar.add(rowNamesMarkedWithStar[i]);
                nRowTitles.add(rowTitles[i]);
                fieldCoordinatesRequresUpdate = true;
                rowColumnsWithStartRequiresUpdate = true;
            } else if (rowNamesMarkedWithStar[i] != null) {
                int k = nRowNames.indexOf(rowNames[i]);
                nRowNamesMarkedWithStar.set(k, rowNamesMarkedWithStar[i]);
                rowColumnsWithStartRequiresUpdate = true;
            }
        }

        for (int i = 0; i < columnNames.length; i++) {
            if (!existedColumnNamesSet.contains(columnNames[i])) {
                nColumnNames.add(columnNames[i]);
                nColumnNamesMarkedWithStar.add(columnNamesMarkedWithStar[i]);
                nColumnTitles.add(columnTitles[i]);
                fieldCoordinatesRequresUpdate = true;
                rowColumnsWithStartRequiresUpdate = true;
            } else if (columnNamesMarkedWithStar[i] != null) {
                int k = nColumnNames.indexOf(columnNames[i]);
                nColumnNamesMarkedWithStar.set(k, columnNamesMarkedWithStar[i]);
                rowColumnsWithStartRequiresUpdate = true;
            }
        }

        if (fieldCoordinatesRequresUpdate) {
            this.rowNames = nRowNames.toArray(new String[] {});
            this.rowTitles = nRowTitles.toArray(new String[] {});

            this.columnNames = nColumnNames.toArray(new String[] {});
            this.columnTitles = nColumnTitles.toArray(new String[] {});

            this.fieldsCoordinates = Collections
                .unmodifiableMap(SpreadsheetResult.buildFieldsCoordinates(this.columnNames, this.rowNames));
        }

        if (rowColumnsWithStartRequiresUpdate) {
            long cols = Arrays.stream(columnNamesMarkedWithStar).filter(Objects::nonNull).count();
            long rows = Arrays.stream(rowNamesMarkedWithStar).filter(Objects::nonNull).count();

            if (this.simpleRefBeanByColumn) {
                this.simpleRefBeanByColumn = rows == 1 && this.rowsWithStarCount == 1;
            }
            if (this.simpleRefBeanByRow) {
                this.simpleRefBeanByRow = cols == 1 && this.columnsWithStarCount == 1;
            }

            this.rowAndColumnNamesMarkedWithStarHistory.add(Pair.of(columnNamesMarkedWithStar, rowNamesMarkedWithStar));

            this.rowNamesMarkedWithStar = nRowNamesMarkedWithStar.toArray(new String[] {});
            this.columnNamesMarkedWithStar = nColumnNamesMarkedWithStar.toArray(new String[] {});
            this.columnsWithStarCount = Arrays.stream(columnNamesMarkedWithStar).filter(Objects::nonNull).count();
            this.rowsWithStarCount = Arrays.stream(rowNamesMarkedWithStar).filter(Objects::nonNull).count();
        }

        for (IOpenField field : fields) {
            if (getField(field.getName()) == null) {
                super.addField(field);
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

    public void extendWith(CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass) {
        if (beanClass != null) {
            throw new IllegalStateException("Bean class is loaded. Custom spreadsheet result can't be extended.");
        }
        this.extendSpreadsheetResult(customSpreadsheetResultOpenClass.getRowNames(),
            customSpreadsheetResultOpenClass.getColumnNames(),
            customSpreadsheetResultOpenClass.getRowNamesMarkedWithStar(),
            customSpreadsheetResultOpenClass.getColumnNamesMarkedWithStar(),
            customSpreadsheetResultOpenClass.getRowTitles(),
            customSpreadsheetResultOpenClass.getColumnTitles(),
            customSpreadsheetResultOpenClass.getFields().values());
        validate(this, customSpreadsheetResultOpenClass.getFields().values());
    }

    private void validate(CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass,
            Collection<IOpenField> fields) {
        List<String> errorMessages = new ArrayList<>();
        for (IOpenField field : fields) {
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

    public Map<String, Point> getFieldsCoordinates() {
        return fieldsCoordinates;
    }

    public String[] getRowNamesMarkedWithStar() {
        return rowNamesMarkedWithStar;
    }

    public String[] getColumnNamesMarkedWithStar() {
        return columnNamesMarkedWithStar;
    }

    public CustomSpreadsheetResultOpenClass makeCopyForModule(XlsModuleOpenClass module) {
        CustomSpreadsheetResultOpenClass type = new CustomSpreadsheetResultOpenClass(getName(),
            getRowNames(),
            getColumnNames(),
            getRowNamesMarkedWithStar(),
            getColumnNamesMarkedWithStar(),
            getRowTitles(),
            getColumnTitles(),
            module);
        for (IOpenField field : getFields().values()) {
            type.addField(field);
        }
        type.setMetaInfo(getMetaInfo());
        return type;
    }

    @Override
    public Object newInstance(IRuntimeEnv env) {
        Object[][] result = new Object[rowNames.length][columnNames.length];
        return new SpreadsheetResult(result, rowNames.clone(), columnNames.clone(), fieldsCoordinates);
    }

    public Object createBean(SpreadsheetResult spreadsheetResult) throws IllegalAccessException,
                                                                  InstantiationException {
        if (!this.getName().equals(spreadsheetResult.getCustomSpreadsheetResultOpenClass().getName())) {
            throw new IllegalArgumentException("Invalid spreadsheet result structure.");
        }
        Class<?> clazz = getBeanClass();
        Object target = clazz.newInstance();
        for (SpreadsheetResultValueSetter spreadsheetResultValueSetter : spreadsheetResultValueSetters) {
            spreadsheetResultValueSetter.set(spreadsheetResult, target);
        }
        return target;
    }

    public boolean isBeanClassInitialized() {
        return beanClass != null;
    }

    public Class<?> getBeanClass() {
        if (beanClass == null) {
            synchronized (this) {
                if (beanClass == null) {
                    final String beanClassName = getBeanClassName(this);
                    JavaBeanClassBuilder beanClassBuilder = new JavaBeanClassBuilder(beanClassName);
                    Set<String> usedFields = new HashSet<>();
                    boolean[][] used = new boolean[rowNames.length][columnNames.length];
                    for (int i = 0; i < used.length; i++) {
                        Arrays.fill(used[i], false);
                    }
                    Map<String, IOpenField> beanFieldsMap = new HashMap<>();
                    Map<String, IOpenClass> types = new HashMap<>();
                    addFieldsToJavaClassBuilder(beanClassBuilder,
                        columnsWithStarCount,
                        rowsWithStarCount,
                        used,
                        usedFields,
                        true,
                        beanFieldsMap,
                        types);
                    addFieldsToJavaClassBuilder(beanClassBuilder,
                        columnsWithStarCount,
                        rowsWithStarCount,
                        used,
                        usedFields,
                        false,
                        beanFieldsMap,
                        types);
                    byte[] byteCode = beanClassBuilder.byteCode();
                    try {
                        beanClass = ClassUtils
                            .defineClass(beanClassName, byteCode, module.getClassGenerationClassLoader());
                        List<SpreadsheetResultValueSetter> srValueSetters = new ArrayList<>();
                        for (Field field : beanClass.getDeclaredFields()) {
                            IOpenField openField = beanFieldsMap.get(field.getName());
                            SpreadsheetResultValueSetter spreadsheetResultValueSetter = new SpreadsheetResultValueSetter(
                                module,
                                field,
                                openField,
                                types.get(field.getName()));
                            srValueSetters.add(spreadsheetResultValueSetter);
                        }
                        this.spreadsheetResultValueSetters = srValueSetters
                            .toArray(new SpreadsheetResultValueSetter[] {});
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
        return beanClass;
    }

    private void addFieldsToJavaClassBuilder(JavaBeanClassBuilder beanClassBuilder,
            long columnsWithStarCount,
            long rowsWithStarCount,
            boolean[][] used,
            Set<String> usedFields,
            boolean addFieldNameWithCollisions,
            Map<String, IOpenField> beanFieldsMap,
            Map<String, IOpenClass> types) {
        for (Entry<String, IOpenField> entry : getFields().entrySet()) {
            Point point = getFieldsCoordinates().get(entry.getKey());
            if (point != null && rowNamesMarkedWithStar[point.getRow()] != null && columnNamesMarkedWithStar[point
                .getColumn()] != null && !used[point.getRow()][point.getColumn()]) {
                String fieldName;

                if (simpleRefBeanByRow && !simpleRefBeanByColumn) {
                    fieldName = rowNamesMarkedWithStar[point.getRow()];
                } else if (simpleRefBeanByColumn && !simpleRefBeanByRow) {
                    fieldName = columnNamesMarkedWithStar[point.getColumn()];
                } else {
                    boolean found = false;
                    for (Pair<String[], String[]> p : rowAndColumnNamesMarkedWithStarHistory) {
                        for (String col : p.getLeft()) {
                            for (String row : p.getRight()) {
                                if (!found && Objects.equals(columnNamesMarkedWithStar[point.getColumn()],
                                    col) && Objects.equals(rowNamesMarkedWithStar[point.getRow()], row)) {
                                    found = true;
                                }
                            }
                        }
                    }
                    if (!found) {
                        continue;
                    }
                    fieldName = columnNamesMarkedWithStar[point.getColumn()] + "_" + rowNamesMarkedWithStar[point
                        .getRow()];
                }
                Class<?> type;
                IOpenClass t = entry.getValue().getType();
                int dim = 0;
                while (t.isArray()) {
                    dim++;
                    t = t.getComponentClass();
                }
                if (t instanceof CustomSpreadsheetResultOpenClass) {
                    CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) t;
                    CustomSpreadsheetResultOpenClass csroc = (CustomSpreadsheetResultOpenClass) this.getModule()
                        .findType(customSpreadsheetResultOpenClass.getName());
                    Class<?> beanType = csroc.getBeanClass();
                    if (dim > 0) {
                        type = Array.newInstance(beanType, new int[dim]).getClass();
                    } else {
                        type = beanType;
                    }
                } else if (t instanceof SpreadsheetResultOpenClass) {
                    if (dim > 0) {
                        type = Array.newInstance(Map.class, new int[dim]).getClass();
                    } else {
                        type = Map.class;
                    }
                } else if (JavaOpenClass.VOID.equals(t) || JavaOpenClass.CLS_VOID.equals(t)) {
                    continue; // IGNORE VOID FIELDS
                } else {
                    type = entry.getValue().getType().getInstanceClass();
                }
                if (!usedFields.contains(fieldName)) {
                    usedFields.add(fieldName);
                    fillUsed(used, point);
                    beanClassBuilder.addField(fieldName, type.getName());
                    beanFieldsMap.put(fieldName, entry.getValue());
                    types.put(fieldName, t);
                } else {
                    if (addFieldNameWithCollisions) {
                        String newFieldName = fieldName;
                        int i = 1;
                        while (usedFields.contains(newFieldName)) {
                            newFieldName = fieldName + i;
                            i++;
                        }
                        usedFields.add(newFieldName);
                        fillUsed(used, point);
                        beanClassBuilder.addField(newFieldName, type.getName());
                        beanFieldsMap.put(newFieldName, entry.getValue());
                        types.put(fieldName, t);
                    }
                }
            }
        }
    }

    public void fillUsed(boolean[][] used, Point point) {
        if (simpleRefBeanByRow && !simpleRefBeanByColumn) {
            for (int w = 0; w < used[point.getRow()].length; w++) {
                used[point.getRow()][w] = true;
            }
        } else if (simpleRefBeanByColumn && !simpleRefBeanByRow) {
            for (int w = 0; w < used.length; w++) {
                used[w][point.getColumn()] = true;
            }
        } else {
            used[point.getRow()][point.getColumn()] = true;
        }
    }

    private static synchronized String getBeanClassName(
            CustomSpreadsheetResultOpenClass customSpreadsheetResultOpenClass) {
        String name = customSpreadsheetResultOpenClass.getName()
            .substring(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX.length());
        String firstLetterUppercasedName = Character
            .toUpperCase(name.charAt(0)) + (name.length() > 1 ? name.substring(1) : "");
        if (customSpreadsheetResultOpenClass.getModule()
            .findType(Spreadsheet.SPREADSHEETRESULT_TYPE_PREFIX + firstLetterUppercasedName) == null) {
            name = firstLetterUppercasedName;
        }

        String csrPackage = "csr";
        final String beanName = CustomSpreadsheetResultOpenClass.class.getPackage()
            .getName() + "." + csrPackage + "." + name;
        try {
            customSpreadsheetResultOpenClass.getModule().getClassGenerationClassLoader().loadClass(beanName);
            throw new IllegalStateException("This shouldn't happen.");
        } catch (ClassNotFoundException e) {
            return beanName;
        }
    }

    private static class SpreadsheetResultValueSetter {
        private Field field;
        private IOpenField openField;
        private IOpenClass type;
        private XlsModuleOpenClass module;

        private SpreadsheetResultValueSetter(XlsModuleOpenClass module,
                Field field,
                IOpenField openField,
                IOpenClass type) {
            this.field = field;
            this.openField = openField;
            this.type = type;
            this.module = module;
            this.field.setAccessible(true);
        }

        private Object convert(Object value, Class<?> t) throws IllegalAccessException, InstantiationException {
            if (value == null) {
                return null;
            }
            if (value.getClass().isArray()) {
                int len = Array.getLength(value);
                Object target = Array.newInstance(t.getComponentType(), len);
                for (int i = 0; i < len; i++) {
                    Object v = convert(Array.get(value, i), t.getComponentType());
                    Array.set(target, i, v);
                }
                return target;
            } else {
                SpreadsheetResult spr = ((SpreadsheetResult) value);
                return spr.toPlain(module);
            }
        }

        public void set(SpreadsheetResult spreadsheetResult, Object target) throws IllegalAccessException,
                                                                            InstantiationException {
            Object v = openField.get(spreadsheetResult, null);

            if (v == null) {
                field.set(target, null);
                return;
            }
            Class<?> t = v.getClass();
            while (t.isArray()) {
                t = t.getComponentType();
            }
            if (SpreadsheetResult.class.isAssignableFrom(t)) {
                field.set(target, convert(v, field.getType()));
            } else {
                field.set(target, v);
            }
        }
    }
}
