package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.binding.BindingDependencies;
import org.openl.rules.binding.RulesBindingDependencies;
import org.openl.rules.calc.Spreadsheet;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetStructureBuilder;
import org.openl.rules.data.ColumnDescriptor;
import org.openl.rules.data.DataTableBindHelper;
import org.openl.rules.data.FieldChain;
import org.openl.rules.data.PrecisionFieldChain;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.method.ExecutableRulesMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.IOpenMethodHeader;
import org.openl.types.impl.DatatypeArrayElementField;
import org.openl.types.impl.DynamicObject;
import org.openl.types.impl.ThisField;
import org.openl.types.java.JavaOpenClass;
import org.openl.vm.IRuntimeEnv;

public class TestSuiteMethod extends ExecutableRulesMethod {

    private final static String PRECISION_PARAM = "precision";
    private IOpenMethod testedMethod;
    private TestDescription[] tests;
    private Map<String, Integer> indeces;
    private final boolean runmethod;
    private DynamicObject[] testObjects;
    private ColumnDescriptor[] descriptors;

    public TestSuiteMethod(IOpenMethod testedMethod, IOpenMethodHeader header,
            TestMethodBoundNode boundNode) {
        super(header, boundNode);

        this.testedMethod = testedMethod;
        initProperties(getSyntaxNode().getTableProperties());
        runmethod = XlsNodeTypes.XLS_RUN_METHOD.toString().equals(getSyntaxNode().getType());
    }

    public TestSuiteMethod(IOpenMethod testedMethod, TestSuiteMethod copy) {
        super(copy.getHeader(), copy.getBoundNode());

        this.testedMethod = testedMethod;
        initProperties(copy.getMethodProperties());
        this.runmethod = copy.isRunmethod();
        this.testObjects = copy.getTestObjects();
        this.descriptors = copy.getDescriptors();
        this.setTableUri(copy.getTableUri());
    }

    private TestDescription[] initTestsAndIndexes() {
        DynamicObject[] testObjects = getTestObjects();
        TestDescription[] tests = new TestDescription[testObjects.length];
        indeces = new HashMap<>(tests.length);
        Map<String, Object> properties = getProperties();
        Integer precision = null;
        if (properties != null && properties.containsKey(PRECISION_PARAM)) {
            precision = Integer.parseInt(properties.get(PRECISION_PARAM).toString());
        }
        IOpenMethod testedMethod = getTestedMethod();
        ColumnDescriptor[] descriptors = getDescriptors();
        List<IOpenField> fields = createFieldsToTest(testedMethod, descriptors, precision);

        for (int i = 0; i < tests.length; i++) {
            tests[i] = new TestDescription(testedMethod, testObjects[i], fields, descriptors);
            tests[i].setIndex(i);
            indeces.put(tests[i].getId(), i);
        }
        return tests;
    }

    public synchronized int[] getIndices(String ids) {
        if (tests == null){
            initTestsAndIndexes();
        }
        TreeSet<Integer> result = new TreeSet<Integer>();

        String ranges[] = ids.trim().split(",");
        for(String range: ranges) {
            if (range.isEmpty() && indeces.containsKey(",")) {
                result.add(indeces.get(","));
                continue;
            }
            String v = range.trim();
            if (indeces.containsKey(v)) {
                result.add(indeces.get(v));
                continue;
            }
            String edges[] = v.split("-");
            if (edges.length > 2 || edges[edges.length - 1].trim().isEmpty()) {
                edges = v.split("\\s[-]\\s");
            }
            if (edges.length == 0) {
                if (indeces.containsKey("-")) {
                    result.add(indeces.get("-"));
                } 
            } else {
                String startIdValue = edges[0].trim();
                String endIdValue = edges[edges.length - 1].trim();
    
                int startIndex = indeces.get(startIdValue);
                int endIndex = indeces.get(endIdValue);
    
                for (int i = startIndex; i<=endIndex; i++) {
                    result.add(i);
                }
            }
        }
        Integer[] indices = new Integer[result.size()];
        return ArrayUtils.toPrimitive(result.toArray(indices));
    }

    @Override
    public TestMethodBoundNode getBoundNode() {
        return (TestMethodBoundNode) super.getBoundNode();
    }

    public BindingDependencies getDependencies() {
        BindingDependencies bindingDependencies = new RulesBindingDependencies();

        updateDependency(bindingDependencies);

        return bindingDependencies;
    }

    private void updateDependency(BindingDependencies bindingDependencies) {
        IOpenMethod testedMethod = getTestedMethod();
        if (testedMethod instanceof ExecutableRulesMethod || testedMethod instanceof OpenMethodDispatcher) {
            bindingDependencies.addMethodDependency(testedMethod, getBoundNode());
        }
    }

    public int getNumberOfTests() {
        return getTests().length;
    }

    public String getSourceUrl() {
        return getSyntaxNode().getUri();
    }

    public DynamicObject[] getTestObjects() {
        initializeTestData();
        return testObjects;
    }

    public synchronized TestDescription[] getTests() {
        if (tests == null) {
            this.tests = initTestsAndIndexes();
        }
        return tests;
    }

    public TestDescription getTest(int numberOfTest) {
        return getTests()[numberOfTest];
    }
    
    public void setTestedMethod(IOpenMethod testedMethod) {
        this.testedMethod = testedMethod;
    }

    public String getColumnDisplayName(String columnTechnicalName) {
        int columnIndex = getColumnIndex(columnTechnicalName);
        return getColumnDisplayName(columnIndex);
    }

    public int getColumnIndex(String columnName) {
        ColumnDescriptor[] descriptors = getDescriptors();
        for (int i = 0; i < descriptors.length; i++) {
            if (descriptors[i] == null) {
                continue;
            }
            if (descriptors[i].getName().equals(columnName)) {
                return i;
            }
        }

        return -1;
    }


    public String getColumnName(int index) {
        if (index >= 0) {
            ColumnDescriptor[] descriptors = getDescriptors();
            return descriptors[index] == null ? null : descriptors[index].getName();
        } else {
            return null;
        }
    }

    public String getColumnDisplayName(int index) {
        if (index >= 0) {
            ColumnDescriptor[] descriptors = getDescriptors();
            return descriptors[index] == null ? null : descriptors[index].getDisplayName();
        } else {
            return null;
        }
    }

    public int getColumnsCount() {
        return getDescriptors().length;
    }

    public IOpenMethod getTestedMethod() {
        return testedMethod;
    }

    @Override
    protected boolean isMethodCacheable() {
        return false;
    }

    protected TestUnitsResults innerInvoke(Object target, Object[] params, IRuntimeEnv env) {
        return new TestSuite(this).invoke(target, env);
    }

    public boolean isRunmethod() {
        return runmethod;
    }

    /**
     * Indicates if test method has any row rules for testing target table.
     * Finds it by field that contains
     * {@link TestMethodHelper#EXPECTED_RESULT_NAME} or
     * {@link TestMethodHelper#EXPECTED_ERROR}
     * 
     * @return true if method expects some return result or some error.
     * 
     *         TODO: rename it. it is difficult to understand what is it doing
     */
    public boolean isRunmethodTestable() {
        for (int i = 0; i < getNumberOfTests(); i++) {
            if (getTest(i).isExpectedResultDefined() || getTest(i).isExpectedErrorDefined()
                    || containsFieldsForSprCellTests(getTest(i).getTestObject().getFieldValues().keySet())
                    || (testedMethod instanceof Spreadsheet)) {
                return true;
            }
        }

        return false;
    }

    private boolean containsFieldsForSprCellTests(Set<String> fieldNames) {
        for (String fieldName : fieldNames) {
            if (fieldName.startsWith(SpreadsheetStructureBuilder.DOLLAR_SIGN)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setBoundNode(ATableBoundNode node) {
        if (node == null) {
            // removeDebugInformation() is invoked.
            // Initialize data needed to run tests before removing debug info
            initializeTestData();
        }

        super.setBoundNode(node);
    }

    public ColumnDescriptor[] getDescriptors() {
        initializeTestData();
        return descriptors;
    }

    private void initializeTestData() {
        if (descriptors == null) {
            testObjects = (DynamicObject[]) getBoundNode().getField().getData();
            descriptors = getBoundNode().getTable().getDataModel().getDescriptor();
        }
    }

    private static List<IOpenField> createFieldsToTest(IOpenMethod testedMethod, ColumnDescriptor[] descriptors, Integer testTablePrecision) {
        IOpenClass resultType = testedMethod.getType();
        List<IOpenField> fieldsToTest = new ArrayList<>();
        for (ColumnDescriptor columnDescriptor : descriptors) {
            if (columnDescriptor != null) {
                IdentifierNode[] nodes = columnDescriptor.getFieldChainTokens();
                if (nodes.length == 0 || !nodes[0].getIdentifier().startsWith(TestMethodHelper.EXPECTED_RESULT_NAME)) {
                    // skip empty or non-'_res_' columns
                    continue;
                }
                Integer fieldPrecision = testTablePrecision;
                if (nodes.length > 1 && nodes[nodes.length - 1].getIdentifier().matches(DataTableBindHelper.PRECISION_PATTERN)) {
                    // set the precision of the field
                    fieldPrecision = DataTableBindHelper.getPrecisionValue(nodes[nodes.length - 1]);
                    nodes = ArrayUtils.remove(nodes, nodes.length - 1);
                }

                if (columnDescriptor.isReference()) {
                    if (resultType.isSimple()) {
                        fieldsToTest.add(new ThisField(resultType));
                    } else if (resultType.isArray()) {
                        fieldsToTest.add(new ThisField(resultType));
                    } else {
                        fieldsToTest.addAll(resultType.getFields().values());
                    }
                } else {
                    IOpenField[] fieldSequence;
                    boolean resIsArray = nodes[0].getIdentifier().matches(DataTableBindHelper.ARRAY_ACCESS_PATTERN);
                    int startIndex = 0;
                    IOpenClass currentType = resultType;

                    if (resIsArray) {
                        startIndex = 1;
                        fieldSequence = new IOpenField[nodes.length];
                        IOpenField arrayField = new ThisField(resultType);
                        int arrayIndex = getArrayIndex(nodes[0]);
                        IOpenField arrayAccessField = new DatatypeArrayElementField(arrayField, arrayIndex);
                        if (arrayAccessField.getType().isArray()) {
                            currentType = arrayAccessField.getType().getComponentClass();
                        } else {
                            currentType = arrayAccessField.getType();
                        }
                        fieldSequence[0] = arrayAccessField;
                    } else {
                        fieldSequence = new IOpenField[nodes.length - 1];
                    }

                    for (int i = startIndex; i < fieldSequence.length; i++) {
                        boolean isArray = nodes[i + 1 - startIndex].getIdentifier()
                                .matches(DataTableBindHelper.ARRAY_ACCESS_PATTERN);
                        if (isArray) {
                            IOpenField arrayField = currentType.getField(getArrayName(nodes[i + 1 - startIndex]));
                            // Try process field as SpreadsheetResult
                            if (arrayField == null && currentType.equals(JavaOpenClass.OBJECT) && nodes[i + 1 - startIndex].getIdentifier()
                                    .matches(DataTableBindHelper.SPREADSHEETRESULTFIELD_PATTERN)) {
                                SpreadsheetResultOpenClass spreadsheetResultOpenClass = new SpreadsheetResultOpenClass(SpreadsheetResult.class);
                                arrayField = spreadsheetResultOpenClass.getField(getArrayName(nodes[i + 1 - startIndex]));
                            }
                            int arrayIndex = getArrayIndex(nodes[i + 1 - startIndex]);
                            IOpenField arrayAccessField = new DatatypeArrayElementField(arrayField, arrayIndex);
                            fieldSequence[i] = arrayAccessField;
                        } else {
                            fieldSequence[i] = currentType.getField(nodes[i + 1 - startIndex].getIdentifier());
                            if (fieldSequence[i] == null) {
                                // Try process field as SpreadsheetResult
                                SpreadsheetResultOpenClass spreadsheetResultOpenClass = new SpreadsheetResultOpenClass(SpreadsheetResult.class);
                                IOpenField openField = spreadsheetResultOpenClass.getField(nodes[i + 1 - startIndex].getIdentifier());
                                if (openField != null) {
                                    fieldSequence[i] = openField;
                                }
                            }
                        }

                        if (fieldSequence[i].getType().isArray() && isArray) {
                            currentType = fieldSequence[i].getType().getComponentClass();
                        } else {
                            currentType = fieldSequence[i].getType();
                        }
                    }
                    if (fieldSequence.length == 0) {
                        fieldSequence = new IOpenField[] { new ThisField(resultType) };
                    }
                    if (fieldPrecision != null) {
                        fieldsToTest.add(new PrecisionFieldChain(currentType, fieldSequence, fieldPrecision));
                    } else if (fieldSequence.length > 1) {
                        fieldsToTest.add(new FieldChain(currentType, fieldSequence));
                    } else {
                        fieldsToTest.add(fieldSequence[0]);
                    }
                }
            }
        }
        return fieldsToTest;
    }

    private static int getArrayIndex(IdentifierNode fieldNameNode) {
        String fieldName = fieldNameNode.getIdentifier();
        String txtIndex = fieldName.substring(fieldName.indexOf("[") + 1, fieldName.indexOf("]"));

        return Integer.parseInt(txtIndex);
    }

    private static String getArrayName(IdentifierNode fieldNameNode) {
        String fieldName = fieldNameNode.getIdentifier();
        return fieldName.substring(0, fieldName.indexOf("["));
    }
}
