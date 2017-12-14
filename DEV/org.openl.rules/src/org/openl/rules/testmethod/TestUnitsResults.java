/**
 * Created Jan 5, 2007
 */
package org.openl.rules.testmethod;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.openl.base.INamedThing;
import org.openl.rules.calc.SpreadsheetResult;
import org.openl.rules.calc.SpreadsheetResultOpenClass;
import org.openl.rules.data.ColumnDescriptor;
import org.openl.rules.data.DataTableBindHelper;
import org.openl.rules.data.FieldChain;
import org.openl.rules.data.PrecisionFieldChain;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.types.IMethodSignature;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.impl.DatatypeArrayElementField;
import org.openl.types.impl.ThisField;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.ClassUtils;

/**
 * Test units results for the test table. Consist of the test suit method
 * itself. And a number of test units that were represented in test table.
 * 
 */
public class TestUnitsResults implements INamedThing {

    private TestSuite testSuite;
    private ArrayList<TestUnit> testUnits = new ArrayList<TestUnit>();

    public TestUnitsResults(TestSuite testSuite) {
        this.testSuite = testSuite;
    }

    public TestSuite getTestSuite() {
        return testSuite;
    }

    public String getName() {
        return testSuite.getDisplayName(INamedThing.SHORT);
    }

    public String getDisplayName(int mode) {
        return testSuite.getDisplayName(mode);
    }

    public ArrayList<TestUnit> getTestUnits() {
        return testUnits;
    }

    public List<TestUnit> getFilteredTestUnits(boolean failuresOnly, int size) {
        if (testUnits != null && failuresOnly) {
            List<TestUnit> failedUnits = new ArrayList<TestUnit>();
            for (TestUnit testUnit : testUnits) {
                if (testUnit.compareResult() != TestStatus.TR_OK // Failed unit
                        && (failedUnits.size() < size || size == -1)) {
                    failedUnits.add(testUnit);
                }
            }
            return failedUnits;
        }

        return testUnits;
    }

    public long getExecutionTime() {
        long executionTime = 0;
        if (testUnits != null) {
            for (TestUnit testUnit : testUnits) {
                executionTime += testUnit.getExecutionTime();
            }
        }

        return executionTime;
    }

    void addTestUnit(TestUnit testUnit) {
        if (!testSuite.isVirtualTestSuite()) {
            testUnits.add(updateTestUnit(testUnit));
        } else {
            testUnits.add(testUnit);
        }
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

    /**
     * Creates the list of test unit results.
     * 
     * @param testUnit test unit
     * @return list of test unit results
     * 
     *         FIXME it should be moved to compile phase and all info about bean
     *         comparator should be located in {@link TestDescription}
     */
    private TestUnit updateTestUnit(TestUnit testUnit) {
        List<IOpenField> fieldsToTest = new ArrayList<>();

        IOpenClass resultType = testSuite.getTestedMethod().getType();
        TestDescription test = testUnit.getTest();
        Integer testTablePrecision = test.getTestTablePrecision();
        for (ColumnDescriptor columnDescriptor : testSuite.getTestSuiteMethod().getDescriptors()) {
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

        testUnit.setFieldsToTest(fieldsToTest);
        return testUnit;
    }

    public int getNumberOfFailures() {
        int cnt = 0;
        for (int i = 0; i < getNumberOfTestUnits(); i++) {
            if (testUnits.get(i).compareResult() != TestStatus.TR_OK) {
                ++cnt;
            }
        }
        return cnt;
    }

    public int getNumberOfErrors() {
        int cnt = 0;
        for (int i = 0; i < getNumberOfTestUnits(); i++) {
            if (testUnits.get(i).compareResult() == TestStatus.TR_EXCEPTION) {
                ++cnt;
            }
        }
        return cnt;
    }

    public int getNumberOfAssertionFailures() {
        int cnt = 0;
        for (int i = 0; i < getNumberOfTestUnits(); i++) {
            if (testUnits.get(i).compareResult() == TestStatus.TR_NEQ) {
                ++cnt;
            }
        }
        return cnt;
    }

    public int getNumberOfTestUnits() {
        return testUnits.size();
    }

    public boolean hasDescription() {
        for (TestUnit testUnit : testUnits) {
            if (testUnit.getTest().getDescription() != null) {
                return true;
            }
        }
        return false;
    }

    public boolean hasContext() {
        for (TestUnit testUnit : testUnits) {
            if (testUnit.getTest().isRuntimeContextDefined()) {
                return true;
            }
        }
        return false;
    }

    public boolean isSpreadsheetResultTester() {
        return ClassUtils.isAssignable(testSuite.getTestedMethod().getType().getInstanceClass(),
            SpreadsheetResult.class);
    }

    public boolean isRunmethod() {
        return testSuite.getTestSuiteMethod().isRunmethod();
    }

    @Deprecated
    public Object getUnitResult(int i) {
        return testUnits.get(i).getActualResult();
    }

    @Deprecated
    public Object getUnitDescription(int i) {
        return testUnits.get(i).getDescription();
    }

    public String[] getTestDataColumnDisplayNames() {
        String[] columnTechnicalNames = getTestDataColumnHeaders();
        String[] columnDisplayNames = new String[columnTechnicalNames.length];
        for (int i = 0; i < columnDisplayNames.length; i++) {
            String displayName = testSuite.getTestSuiteMethod().getColumnDisplayName(columnTechnicalNames[i]);
            if (displayName != null){
                columnDisplayNames[i] = displayName;
            }else{
                columnDisplayNames[i] = columnTechnicalNames[i];
            }
        }
        return columnDisplayNames;
    }

    private String[] getColumnDisplayNames(String type) {
        List<String> displayNames = new ArrayList<String>();
        TestSuiteMethod test = testSuite.getTestSuiteMethod();
        for (int i = 0; i < test.getColumnsCount(); i++) {
            String columnName = test.getColumnName(i);
            if (columnName != null && columnName.startsWith(type)) {
                displayNames.add(test.getColumnDisplayName(columnName));
            }
        }
        return displayNames.toArray(new String[displayNames.size()]);
    }

    public String[] getContextColumnDisplayNames() {
        return getColumnDisplayNames(TestMethodHelper.CONTEXT_NAME);
    }

    public String[] getTestResultColumnDisplayNames() {
        return getColumnDisplayNames(TestMethodHelper.EXPECTED_RESULT_NAME);
    }

    public String[] getTestDataColumnHeaders() {
        IMethodSignature testMethodSignature = testSuite.getTestedMethod().getSignature();

        int len = testMethodSignature.getParameterTypes().length;

        String[] res = new String[len];
        for (int i = 0; i < len; i++) {
            res[i] = testMethodSignature.getParameterName(i);
        }
        return res;
    }
}