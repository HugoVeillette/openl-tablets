package org.openl.rules.data;

import java.lang.reflect.Array;
import java.util.*;

import org.openl.binding.IBindingContext;
import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLRuntimeException;
import org.openl.rules.OpenlToolAdaptor;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.IGridTable;
import org.openl.rules.table.ILogicalTable;
import org.openl.rules.table.openl.GridCellSourceCodeModule;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IOpenClass;
import org.openl.util.BiMap;
import org.openl.vm.IRuntimeEnv;

public class Table implements ITable {

    private ILogicalTable logicalTable;
    private ITableModel dataModel;

    private String tableName;
    private TableSyntaxNode tableSyntaxNode;

    private Object dataArray;

    private BiMap<Integer, Object> rowIndexMap;
    private BiMap<Integer, String> primaryIndexMap;

    public Table(ITableModel dataModel, ILogicalTable data) {
        this.dataModel = dataModel;
        this.logicalTable = data;
    }

    public Table(String tableName, TableSyntaxNode tsn) {
        this.tableName = tableName;
        this.tableSyntaxNode = tsn;
    }

    @Override
    public void setData(ILogicalTable dataWithHeader) {
        logicalTable = dataWithHeader;
    }

    @Override
    public ILogicalTable getData() {
        return logicalTable;
    }

    @Override
    public void setModel(ITableModel dataModel) {
        this.dataModel = dataModel;
    }

    @Override
    public String getColumnDisplay(int n) {
        return dataModel.getDescriptor(n).getDisplayName();
    }

    @Override
    public int getColumnIndex(String columnName) {
        ColumnDescriptor[] descriptors = dataModel.getDescriptors();

        for (int i = 0; i < descriptors.length; i++) {
            ColumnDescriptor descriptor = descriptors[i];
            if (descriptor == null) {
                continue;
            }
            if (descriptor.getName().equals(columnName)) {
                return descriptor.getColumnIdx();
            }
        }

        return -1;
    }

    @Override
    public String getColumnName(int n) {
        ColumnDescriptor columnDescriptor = dataModel.getDescriptor(n);
        return columnDescriptor != null ? columnDescriptor.getName() : null;
    }

    @Override
    public IOpenClass getColumnType(int n) {

        ColumnDescriptor descriptor = dataModel.getDescriptor(n);

        if (!descriptor.isConstructor()) {
            return descriptor.getType();
        }

        return null;
    }

    @Override
    public Object getData(int row) {
        return Array.get(dataArray, row);
    }

    @Override
    public Object getDataArray() {
        return dataArray;
    }

    @Override
    public ITableModel getDataModel() {
        return dataModel;
    }

    @Override
    public IGridTable getHeaderTable() {
        return logicalTable.getRow(0).getSource();
    }

    @Override
    public String getName() {
        return tableName;
    }

    @Override
    public int getNumberOfColumns() {
        return dataModel.getDescriptors().length;
    }

    @Override
    public ColumnDescriptor getColumnDescriptor(int i) {
        return dataModel.getDescriptor(i);
    }

    @Override
    public int getNumberOfRows() {
        return logicalTable.getHeight() - 1;
    }

    @Override
    public synchronized String getPrimaryIndexKey(int row) {
        if (primaryIndexMap == null) {
            return null;
        }
        return primaryIndexMap.get(row);
    }

    @Override
    public Integer getRowIndex(Object target) {
        return rowIndexMap.getKey(target);
    }

    @Override
    public IGridTable getRowTable(int row) {
        return logicalTable.getRow(row + 1).getSource();
    }

    @Override
    public int getSize() {
        return Array.getLength(dataArray);
    }

    @Override
    public TableSyntaxNode getTableSyntaxNode() {
        return tableSyntaxNode;
    }

    @Override
    public Map<String, Integer> getUniqueIndex(int columnIndex) throws SyntaxNodeException {

        ColumnDescriptor descriptor = dataModel.getDescriptor(columnIndex);

        return descriptor.getUniqueIndex(this, columnIndex);
    }

    @Override
    public Object getValue(int col, int row) {

        Object rowObject = Array.get(getDataArray(), row);

        return dataModel.getDescriptor(col).getColumnValue(rowObject);
    }

    @Override
    public Map<String, Integer> makeUniqueIndex(int colIdx) throws SyntaxNodeException {

        Map<String, Integer> index = new HashMap<>();

        int rows = logicalTable.getHeight();

        for (int i = 1; i < rows; i++) {

            IGridTable gridTable = logicalTable.getSubtable(colIdx, i, 1, 1).getSource();
            String key = gridTable.getCell(0, 0).getStringValue();

            if (key == null) {
                throw SyntaxNodeExceptionUtils.createError("Empty key in an unique index",
                    new GridCellSourceCodeModule(gridTable));
            }

            key = key.trim();

            if (index.containsKey(key)) {
                throw SyntaxNodeExceptionUtils.createError("Duplicated key in an unique index: " + key,
                    new GridCellSourceCodeModule(gridTable));
            }

            index.put(key, i - 1);
        }

        return index;
    }

    @Override
    public List<Object> getUniqueValues(int colIdx) throws SyntaxNodeException {

        List<Object> values = new ArrayList<>();

        int rows = logicalTable.getHeight();

        for (int i = 1; i < rows; i++) {

            IGridTable gridTable = logicalTable.getSubtable(colIdx, i, 1, 1).getSource();
            Object value = gridTable.getCell(0, 0).getObjectValue();

            if (value == null) {
                throw SyntaxNodeExceptionUtils.createError("Empty key in an unique index",
                    new GridCellSourceCodeModule(gridTable));
            }

            if (values.contains(value)) {
                throw SyntaxNodeExceptionUtils.createError("Duplicated key in an unique index: " + value,
                    new GridCellSourceCodeModule(gridTable));
            }

            values.add(value);
        }

        return values;
    }

    @Override
    public void populate(IDataBase dataBase, IBindingContext bindingContext) throws Exception {

        int rows = logicalTable.getHeight();
        int columns = logicalTable.getWidth();

        int startRow = 1;

        Collection<SyntaxNodeException> errorSyntaxNodeExceptions = new ArrayList<>(0);

        for (int i = startRow; i < rows; i++) {

            Object target = Array.get(dataArray, i - startRow);

            for (int j = 0; j < columns; j++) {

                ColumnDescriptor descriptor = dataModel.getDescriptor(j);

                if (descriptor instanceof ForeignKeyColumnDescriptor) {
                    ForeignKeyColumnDescriptor fkDescriptor = (ForeignKeyColumnDescriptor) descriptor;

                    if (fkDescriptor.isReference()) {
                        try {
                            if (descriptor.isConstructor()) {
                                target = fkDescriptor.getLiteralByForeignKey(dataModel.getType(),
                                    logicalTable.getSubtable(j, i, 1, 1),
                                    dataBase,
                                    bindingContext);
                            } else {
                                fkDescriptor.populateLiteralByForeignKey(target,
                                    logicalTable.getSubtable(j, i, 1, 1),
                                    dataBase,
                                    bindingContext);
                            }
                        } catch (SyntaxNodeException e) {
                            boolean found = false;
                            for (SyntaxNodeException syntaxNodeException : errorSyntaxNodeExceptions) {
                                if (syntaxNodeException.getMessage()
                                    .equals(
                                        e.getMessage()) && syntaxNodeException.getSyntaxNode() == e.getSyntaxNode()) {
                                    found = true;
                                }
                            }
                            if (!found) {
                                errorSyntaxNodeExceptions.add(e);
                            }
                        }
                    }
                }
            }
        }
        if (!errorSyntaxNodeExceptions.isEmpty()) {
            throw new CompositeSyntaxNodeException("Parsing Error:",
                errorSyntaxNodeExceptions.toArray(new SyntaxNodeException[0]));
        }
    }

    @Override
    public void preLoad(OpenlToolAdaptor openlAdapter) throws Exception {
        int rows = logicalTable.getHeight();
        int startRow = getStartRowForData();

        dataArray = Array.newInstance(dataModel.getInstanceClass(), rows - startRow);

        for (int rowNum = startRow; rowNum < rows; rowNum++) {
            processRow(openlAdapter, startRow, rowNum);
        }
    }

    private void processRow(OpenlToolAdaptor openlAdapter, int startRow, int rowNum) throws OpenLCompilationException {

        boolean constructor = isConstructor();
        Object literal = null;

        int rowIndex = rowNum - startRow;

        if (!constructor) {
            if (dataModel.getInstanceClass().isArray()) {
                int dim = 0;
                Class<?> type = dataModel.getInstanceClass();
                while (type.isArray()) {
                    type = type.getComponentType();
                    dim++;
                }
                literal = Array.newInstance(type, new int[dim]);
            } else {
                literal = dataModel.newInstance();
            }
            if (literal == null) {
                String errorMessage = String.format("Can`t create instance of %s", dataModel.getName());
                throw new OpenLCompilationException(errorMessage);
            }
            addToRowIndex(rowIndex, literal);
        }

        int columns = logicalTable.getWidth();

        IRuntimeEnv env = openlAdapter.getOpenl().getVm().getRuntimeEnv();
        env.pushLocalFrame(new Object[] { new DatatypeArrayMultiRowElementContext() });
        for (ColumnDescriptor columnDescriptor : dataModel.getDescriptors()) {
            literal = processColumn(columnDescriptor, openlAdapter, constructor, rowNum, literal, env);
        }
        env.popLocalFrame();
        if (literal == null) {
            literal = dataModel.getType().nullObject();
        }

        Array.set(dataArray, rowNum - startRow, literal);
    }

    private Object processColumn(ColumnDescriptor columnDescriptor, OpenlToolAdaptor openlAdapter,
            boolean constructor,
            int rowNum,
            Object literal,
            IRuntimeEnv env) throws SyntaxNodeException {

        if (columnDescriptor != null && !columnDescriptor.isReference()) {
            if (constructor) {
                literal = columnDescriptor
                    .getLiteral(dataModel.getType(), logicalTable.getSubtable(columnDescriptor.getColumnIdx(), rowNum, 1, 1), openlAdapter);
            } else {
                try {
                    ILogicalTable lTable = logicalTable.getSubtable(columnDescriptor.getColumnIdx(), rowNum, 1, 1);
                    if (!(lTable.getHeight() == 1 && lTable.getWidth() == 1) || lTable.getCell(0, 0)
                        .getStringValue() != null) { // EPBDS-6104. For empty values should be used data type default
                        // value.
                        return columnDescriptor.populateLiteral(literal, lTable, openlAdapter, env);
                    }
                } catch (SyntaxNodeException ex) {
                    tableSyntaxNode.addError(ex);
                    openlAdapter.getBindingContext().addError(ex);
                }
            }
        }

        return literal;
    }

    @Override
    public synchronized void setPrimaryIndexKey(int row, String value) {
        if (primaryIndexMap == null) {
            primaryIndexMap = new BiMap<>();
        }
        Integer oldRow = primaryIndexMap.getKey(value);
        if (oldRow != null && row != oldRow) {
            throw new OpenLRuntimeException("Duplicated key: " + value + " in rows " + oldRow + " and " + row);
        }
        primaryIndexMap.put(row, value);
    }

    @Override
    public Object findObject(int columnIndex, String skey, IBindingContext cxt) throws SyntaxNodeException {

        Map<String, Integer> index = getUniqueIndex(columnIndex);

        Integer found = index.get(skey);

        if (found == null) {
            return null;
        }

        return Array.get(dataArray, found);
    }

    private void addToRowIndex(int rowIndex, Object target) {
        if (rowIndexMap == null) {
            rowIndexMap = new BiMap<>();
        }
        rowIndexMap.put(rowIndex, target);
    }

    /**
     * @return Start row for data rows from Data_With_Titles rows. It depends on if table has or no column title row.
     */
    private int getStartRowForData() {

        if (dataModel.hasColumnTitleRow()) {
            return 1;
        }

        return 0;
    }

    private boolean isConstructor() {
        for (ColumnDescriptor columnDescriptor : dataModel.getDescriptors()) {
            if (columnDescriptor != null && columnDescriptor.isConstructor()) {
                return true;
            }
        }
        return false;
    }
}
