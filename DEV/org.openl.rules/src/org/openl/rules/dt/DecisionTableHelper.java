package org.openl.rules.dt;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toList;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.Predicate;
import java.util.function.ToLongFunction;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.mutable.MutableBoolean;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openl.base.INamedThing;
import org.openl.binding.IBindingContext;
import org.openl.binding.impl.cast.IOpenCast;
import org.openl.exception.OpenLCompilationException;
import org.openl.message.OpenLMessagesUtils;
import org.openl.rules.binding.RuleRowHelper;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.convertor.IString2DataConvertor;
import org.openl.rules.convertor.String2DataConvertorFactory;
import org.openl.rules.fuzzy.OpenLFuzzyUtils;
import org.openl.rules.fuzzy.OpenLFuzzyUtils.FuzzyResult;
import org.openl.rules.fuzzy.Token;
import org.openl.rules.helpers.*;
import org.openl.rules.lang.xls.IXlsTableNames;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.lang.xls.XlsWorkbookSourceCodeModule;
import org.openl.rules.lang.xls.binding.DTColumnsDefinition;
import org.openl.rules.lang.xls.binding.XlsDefinitions;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.load.SimpleSheetLoader;
import org.openl.rules.lang.xls.load.SimpleWorkbookLoader;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.types.meta.DecisionTableMetaInfoReader;
import org.openl.rules.lang.xls.types.meta.MetaInfoReader;
import org.openl.rules.table.*;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.source.impl.StringSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.impl.ISyntaxConstants;
import org.openl.syntax.impl.IdentifierNode;
import org.openl.syntax.impl.Tokenizer;
import org.openl.types.*;
import org.openl.types.impl.AOpenClass;
import org.openl.types.java.JavaOpenClass;
import org.openl.util.StringTool;
import org.openl.util.text.TextInfo;

public final class DecisionTableHelper {

    private static final String RET1_COLUMN_NAME = DecisionTableColumnHeaders.RETURN.getHeaderKey() + "1";
    private static final String CRET1_COLUMN_NAME = DecisionTableColumnHeaders.COLLECT_RETURN.getHeaderKey() + "1";
    private static final List<Class<?>> INT_TYPES = Arrays.asList(byte.class,
        short.class,
        int.class,
        java.lang.Byte.class,
        java.lang.Short.class,
        org.openl.meta.ByteValue.class,
        org.openl.meta.ShortValue.class,
        org.openl.meta.IntValue.class,
        java.math.BigInteger.class,
        org.openl.meta.BigIntegerValue.class,
        java.lang.Integer.class);
    private static final List<Class<?>> DOUBLE_TYPES = Arrays.asList(long.class,
        float.class,
        double.class,
        java.lang.Long.class,
        java.lang.Float.class,
        java.lang.Double.class,
        org.openl.meta.LongValue.class,
        org.openl.meta.FloatValue.class,
        org.openl.meta.DoubleValue.class,
        java.math.BigDecimal.class,
        org.openl.meta.BigDecimalValue.class);
    private static final List<Class<?>> CHAR_TYPES = Arrays.asList(char.class, java.lang.Character.class);
    private static final List<Class<?>> STRINGS_TYPES = Arrays.asList(java.lang.String.class,
        org.openl.meta.StringValue.class);
    private static final List<Class<?>> DATE_TYPES = Collections.singletonList(Date.class);
    private static final List<Class<?>> RANGES_TYPES = Arrays
        .asList(IntRange.class, DoubleRange.class, CharRange.class, StringRange.class, DateRange.class);

    private static final List<Class<?>> IGNORED_CLASSES_FOR_COMPOUND_TYPE = Arrays.asList(null,
        byte.class,
        short.class,
        int.class,
        long.class,
        float.class,
        double.class,
        char.class,
        void.class,
        java.lang.Byte.class,
        java.lang.Short.class,
        java.lang.Integer.class,
        java.lang.Long.class,
        java.lang.Float.class,
        java.lang.Double.class,
        java.lang.Character.class,
        java.lang.String.class,
        java.math.BigInteger.class,
        java.math.BigDecimal.class,
        Date.class,
        IntRange.class,
        DoubleRange.class,
        CharRange.class,
        StringRange.class,
        DateRange.class,
        org.openl.meta.ByteValue.class,
        org.openl.meta.ShortValue.class,
        org.openl.meta.IntValue.class,
        org.openl.meta.LongValue.class,
        org.openl.meta.FloatValue.class,
        org.openl.meta.DoubleValue.class,
        org.openl.meta.BigIntegerValue.class,
        org.openl.meta.BigDecimalValue.class,
        org.openl.meta.StringValue.class,
        Object.class,
        Map.class,
        SortedMap.class,
        Set.class,
        SortedSet.class,
        List.class,
        Collections.class,
        ArrayList.class,
        LinkedList.class,
        HashSet.class,
        LinkedHashSet.class,
        HashMap.class,
        TreeSet.class,
        TreeMap.class,
        LinkedHashMap.class);

    private DecisionTableHelper() {
    }

    /**
     * Check if table is vertical.<br>
     * Vertical table is when conditions are represented from left to right, table is reading from top to bottom.</br>
     * Example of vertical table:
     *
     * <table cellspacing="2">
     * <tr>
     * <td align="center" bgcolor="#ccffff"><b>Rule</b></td>
     * <td align="center" bgcolor="#ccffff"><b>C1</b></td>
     * <td align="center" bgcolor="#ccffff"><b>C2</b></td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#ccffff"></td>
     * <td align="center" bgcolor="#ccffff">paramLocal1==paramInc</td>
     * <td align="center" bgcolor="#ccffff">paramLocal2==paramInc</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#ccffff"></td>
     * <td align="center" bgcolor="#ccffff">String paramLocal1</td>
     * <td align="center" bgcolor="#ccffff">String paramLocal2</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#8FCB52">Rule</td>
     * <td align="center" bgcolor="#ffff99">Local Param 1</td>
     * <td align="center" bgcolor="#ffff99">Local Param 2</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#8FCB52">Rule1</td>
     * <td align="center" bgcolor="#ffff99">value11</td>
     * <td align="center" bgcolor="#ffff99">value21</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#8FCB52">Rule2</td>
     * <td align="center" bgcolor="#ffff99">value12</td>
     * <td align="center" bgcolor="#ffff99">value22</td>
     * </tr>
     * <tr>
     * <td align="center" bgcolor="#8FCB52">Rule3</td>
     * <td align="center" bgcolor="#ffff99">value13</td>
     * <td align="center" bgcolor="#ffff99">value23</td>
     * </tr>
     * </table>
     *
     * @param table checked table
     * @return <code>TRUE</code> if table is vertical.
     */
    static boolean looksLikeVertical(ILogicalTable table) {

        if (table.getWidth() < IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            return true;
        }

        if (table.getHeight() < IDecisionTableConstants.SERVICE_COLUMNS_NUMBER) {
            return false;
        }

        int cnt1 = countConditionsAndActions(table);
        int cnt2 = countConditionsAndActions(table.transpose());

        if (cnt1 != cnt2) {
            return cnt1 > cnt2;
        }

        return table.getWidth() <= IDecisionTableConstants.SERVICE_COLUMNS_NUMBER;
    }

    static boolean isValidConditionHeader(String s) {
        return s.length() >= 2 && s.charAt(0) == DecisionTableColumnHeaders.CONDITION.getHeaderKey()
            .charAt(0) && Character.isDigit(s.charAt(1));
    }

    static boolean isValidHConditionHeader(String headerStr) {
        return headerStr.startsWith(
            DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey()) && headerStr.length() > 2 && Character
                .isDigit(headerStr.charAt(2));
    }

    static boolean isValidMergedConditionHeader(String headerStr) {
        return headerStr.startsWith(
            DecisionTableColumnHeaders.MERGED_CONDITION.getHeaderKey()) && headerStr.length() > 2 && Character
                .isDigit(headerStr.charAt(2));
    }

    static boolean isValidActionHeader(String s) {
        return s.length() >= 2 && s.charAt(0) == DecisionTableColumnHeaders.ACTION.getHeaderKey().charAt(0) && Character
            .isDigit(s.charAt(1));
    }

    static boolean isValidRetHeader(String s) {
        return s.length() >= 3 && s.startsWith(
            DecisionTableColumnHeaders.RETURN.getHeaderKey()) && (s.length() == 3 || Character.isDigit(s.charAt(3)));
    }

    static boolean isValidKeyHeader(String s) {
        return s.length() >= 3 && s.startsWith(
            DecisionTableColumnHeaders.KEY.getHeaderKey()) && (s.length() == 3 || Character.isDigit(s.charAt(3)));
    }

    static boolean isValidCRetHeader(String s) {
        return s.length() >= 4 && s.startsWith(DecisionTableColumnHeaders.COLLECT_RETURN
            .getHeaderKey()) && (s.length() == 4 || Character.isDigit(s.charAt(4)));
    }

    static boolean isValidRuleHeader(String s) {
        return s.equals(DecisionTableColumnHeaders.RULE.getHeaderKey());
    }

    static boolean isConditionHeader(String s) {
        return isValidConditionHeader(s) || isValidHConditionHeader(s) || isValidMergedConditionHeader(s);
    }

    private static int countConditionsAndActions(ILogicalTable table) {

        int width = table.getWidth();
        int count = 0;

        for (int i = 0; i < width; i++) {

            String value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();

            if (value != null) {
                value = value.toUpperCase();
                count += isValidConditionHeader(value) || isValidActionHeader(value) || isValidRetHeader(
                    value) || isValidCRetHeader(value) || isValidKeyHeader(value) ? 1 : 0;
            }
        }

        return count;
    }

    /**
     * Creates virtual headers for condition and return columns to load simple Decision Table as an usual Decision Table
     *
     * @param decisionTable method description for simple Decision Table.
     * @param originalTable The original body of simple Decision Table.
     * @param numberOfHcondition The number of horizontal conditions. In SimpleRules it == 0 in SimpleLookups > 0
     * @return prepared usual Decision Table.
     */
    static ILogicalTable preprocessDecisionTableWithoutHeaders(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            ILogicalTable originalTable,
            IBindingContext bindingContext) throws OpenLCompilationException {
        IWritableGrid virtualGrid = createVirtualGrid();
        writeVirtualHeaders(tableSyntaxNode, decisionTable, originalTable, virtualGrid, bindingContext);

        // If the new table header size bigger than the size of the old table we
        // use the new table size
        int sizeOfVirtualGridTable = virtualGrid.getMaxColumnIndex(0) < originalTable.getSource()
            .getWidth() ? originalTable.getSource().getWidth() - 1 : virtualGrid.getMaxColumnIndex(0) - 1;
        GridTable virtualGridTable = new GridTable(0,
            0,
            IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1,
            sizeOfVirtualGridTable/* originalTable.getSource().getWidth() - 1 */,
            virtualGrid);

        IGrid grid = new CompositeGrid(new IGridTable[] { virtualGridTable, originalTable.getSource() }, true);

        // If the new table header size bigger than the size of the old table we
        // use the new table size
        int sizeofGrid = virtualGridTable.getWidth() < originalTable.getSource().getWidth() ? originalTable.getSource()
            .getWidth() - 1 : virtualGridTable.getWidth() - 1;

        return LogicalTableHelper.logicalTable(new GridTable(0,
            0,
            originalTable.getSource().getHeight() + IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1,
            sizeofGrid /* originalTable.getSource().getWidth() - 1 */,
            grid));
    }

    private static FuzzyContext buildFuzzyContext(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            int numberOfHcondition,
            IBindingContext bindingContext) {
        final ParameterTokens parameterTokens = buildParameterTokens(decisionTable);
        Map<Token, IOpenMethod[][]> returnTypeFuzzyTokens = null;
        Token[] returnTokens = null;
        if (numberOfHcondition == 0) {
            IOpenClass returnType = getCompoundReturnType(tableSyntaxNode, decisionTable, bindingContext);
            if (isCompoundReturnType(returnType)) {
                returnTypeFuzzyTokens = OpenLFuzzyUtils
                    .tokensMapToOpenClassSetterMethodsRecursively(returnType, returnType.getName(), 0);
                returnTokens = returnTypeFuzzyTokens.keySet().toArray(new Token[] {});
                return new FuzzyContext(parameterTokens, returnTokens, returnTypeFuzzyTokens, returnType);
            }
        }

        return new FuzzyContext(parameterTokens);
    }

    private static void writeVirtualHeaders(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            ILogicalTable originalTable,
            IWritableGrid grid,
            IBindingContext bindingContext) throws OpenLCompilationException {
        int numberOfHcondition = isLookup(tableSyntaxNode) ? getNumberOfHConditions(originalTable) : 0;
        int firstColumnHeight = originalTable.getSource().getCell(0, 0).getHeight();

        final FuzzyContext fuzzyContext = buildFuzzyContext(tableSyntaxNode,
            decisionTable,
            numberOfHcondition,
            bindingContext);

        final NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter = new NumberOfColumnsUnderTitleCounter(
            originalTable,
            firstColumnHeight);

        List<DTHeader> dtHeaders = getDTHeaders(tableSyntaxNode,
            decisionTable,
            originalTable,
            fuzzyContext,
            numberOfColumnsUnderTitleCounter,
            numberOfHcondition,
            firstColumnHeight,
            bindingContext);

        writeConditions(decisionTable,
            originalTable,
            grid,
            numberOfColumnsUnderTitleCounter,
            dtHeaders,
            numberOfHcondition,
            firstColumnHeight,
            bindingContext);

        writeActions(decisionTable, originalTable, grid, dtHeaders, bindingContext);

        writeReturns(tableSyntaxNode, decisionTable, originalTable, grid, fuzzyContext, dtHeaders, bindingContext);
    }

    private static boolean isCompoundReturnType(IOpenClass compoundType) {
        if (IGNORED_CLASSES_FOR_COMPOUND_TYPE.contains(compoundType.getInstanceClass())) {
            return false;
        }

        if (compoundType.getConstructor(IOpenClass.EMPTY) == null) {
            return false;
        }

        int count = 0;
        for (IOpenMethod method : compoundType.getMethods()) {
            if (OpenLFuzzyUtils.isSetterMethod(method)) {
                count++;
            }
        }
        return count > 0;
    }

    private static boolean isCompoundInputType(IOpenClass type) {
        if (IGNORED_CLASSES_FOR_COMPOUND_TYPE.contains(type.getInstanceClass())) {
            return false;
        }

        int count = 0;
        for (IOpenMethod method : type.getMethods()) {
            if (OpenLFuzzyUtils.isGetterMethod(method)) {
                count++;
            }
        }
        return count > 0;
    }

    private static void validateCompoundReturnType(IOpenClass compoundType) throws OpenLCompilationException {
        try {
            compoundType.getInstanceClass().getConstructor();
        } catch (Exception e) {
            throw new OpenLCompilationException(
                String.format("Invalid compound return type: There is no default constructor found in return type '%s'",
                    compoundType.getDisplayName(0)));
        }
    }

    private static void writeReturnMetaInfo(TableSyntaxNode tableSyntaxNode,
            ICell cell,
            String description,
            String uri) {
        MetaInfoReader metaReader = tableSyntaxNode.getMetaInfoReader();
        if (metaReader instanceof DecisionTableMetaInfoReader) {
            DecisionTableMetaInfoReader metaInfoReader = (DecisionTableMetaInfoReader) metaReader;
            metaInfoReader.addSimpleRulesReturn(cell.getAbsoluteRow(), cell.getAbsoluteColumn(), description, uri);
        }
    }

    private static IOpenClass getCompoundReturnType(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            IBindingContext bindingContext) {
        IOpenClass compoundType;
        if (isCollect(tableSyntaxNode)) {
            if (tableSyntaxNode.getHeader().getCollectParameters().length > 0) {
                compoundType = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE,
                    tableSyntaxNode.getHeader()
                        .getCollectParameters()[tableSyntaxNode.getHeader().getCollectParameters().length - 1]);
            } else {
                if (decisionTable.getType().isArray()) {
                    compoundType = decisionTable.getType().getComponentClass();
                } else {
                    compoundType = decisionTable.getType();
                }
            }
        } else {
            compoundType = decisionTable.getType();
        }
        return compoundType;
    }

    private static Pair<String, IOpenClass> buildStatementByMethodsChain(IOpenClass type, IOpenMethod[] methodsChain) {
        StringBuilder fieldChainSb = new StringBuilder();
        for (int i = 0; i < methodsChain.length; i++) {
            IOpenField openField = type.getField(methodsChain[i].getName().substring(3), false);
            fieldChainSb.append(openField.getDisplayName(0));
            if (i < methodsChain.length - 1) {
                fieldChainSb.append(".");
            }
            if (methodsChain[i].getSignature().getNumberOfParameters() == 0) {
                type = methodsChain[i].getType();
            } else {
                type = methodsChain[i].getSignature().getParameterType(0);
            }
        }
        return Pair.of(fieldChainSb.toString(), type);
    }

    private static void validateCollectSyntaxNode(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            IBindingContext bindingContext) throws OpenLCompilationException {
        int parametersCount = tableSyntaxNode.getHeader().getCollectParameters().length;
        IOpenClass type = decisionTable.getType();
        if ((type.isArray() || Collection.class.isAssignableFrom(type.getInstanceClass())) && parametersCount > 1) {
            throw new OpenLCompilationException(
                String.format("Error: Cannot bind node: '%s'. Found more than one parameter for '%s'.",
                    Tokenizer.firstToken(tableSyntaxNode.getHeader().getModule(), "").getIdentifier(),
                    type.getComponentClass().getDisplayName(0)));
        }
        if (Map.class.isAssignableFrom(type.getInstanceClass())) {
            if (parametersCount > 2) {
                throw new OpenLCompilationException(
                    String.format("Error: Cannot bind node: '%s'. Found more than two parameter for '%s'.",
                        Tokenizer.firstToken(tableSyntaxNode.getHeader().getModule(), "").getIdentifier(),
                        type.getDisplayName(0)));
            }
            if (parametersCount == 1) {
                throw new OpenLCompilationException(
                    String.format("Error: Cannot bind node: '%s'. Found only one parameter for '%s'.",
                        Tokenizer.firstToken(tableSyntaxNode.getHeader().getModule(), "").getIdentifier(),
                        type.getDisplayName(0)));
            }
        }
        for (String parameterType : tableSyntaxNode.getHeader().getCollectParameters()) {
            IOpenClass t = bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, parameterType);
            if (t == null) {
                throw new OpenLCompilationException(
                    String.format("Error: Cannot bind node: '%s'. Cannot find type: '%s'.",
                        Tokenizer.firstToken(tableSyntaxNode.getHeader().getModule(), "").getIdentifier(),
                        parameterType));
            } else {
                if (type.isArray() && bindingContext.getCast(t, type.getComponentClass()) == null) {
                    throw new OpenLCompilationException(
                        String.format("Error: Cannot bind node: '%s'. Incompatible types: '%s' and '%s'.",
                            Tokenizer.firstToken(tableSyntaxNode.getHeader().getModule(), "").getIdentifier(),
                            type.getComponentClass().getDisplayName(0),
                            t.getDisplayName(0)));
                }
            }
        }
    }

    private static void writeReturnWithReturnDtHeader(TableSyntaxNode tableSyntaxNode,
            ILogicalTable originalTable,
            IWritableGrid grid,
            DeclaredDTHeader declaredReturn,
            String header,
            IBindingContext bindingContext) {
        grid.setCellValue(declaredReturn.getColumn(), 0, header);
        grid.setCellValue(declaredReturn.getColumn(), 1, declaredReturn.getStatement());
        DTColumnsDefinition dtColumnsDefinition = declaredReturn.getMatchedDefinition().getDtColumnsDefinition();
        int c = declaredReturn.getColumn();
        while (c < originalTable.getSource().getWidth()) {
            ICell cell = originalTable.getSource().getCell(c, 0);
            String d = cell.getStringValue();
            d = OpenLFuzzyUtils.toTokenString(d);
            for (String title : dtColumnsDefinition.getTitles()) {
                if (Objects.equals(d, title)) {
                    List<IParameterDeclaration> localParameters = dtColumnsDefinition.getLocalParameters(title);
                    List<String> localParameterNames = new ArrayList<>();
                    List<IOpenClass> typeOfColumns = new ArrayList<>();
                    int column = c;
                    for (IParameterDeclaration param : localParameters) {
                        if (param != null) {
                            String paramName = declaredReturn.getMatchedDefinition()
                                .getLocalParameterName(param.getName());
                            localParameterNames.add(paramName);
                            String value = param.getType().getName() + (paramName != null ? " " + paramName : "");
                            grid.setCellValue(column, 2, value);
                            typeOfColumns.add(param.getType());
                        } else {
                            typeOfColumns.add(declaredReturn.getCompositeMethod().getType());
                        }

                        int h = originalTable.getSource().getCell(column, 0).getHeight();
                        int w1 = originalTable.getSource().getCell(column, h).getWidth();
                        if (w1 > 1) {
                            grid.addMergedRegion(new GridRegion(2, column, 2, column + w1 - 1));
                        }

                        column = column + w1;
                    }
                    if (!bindingContext.isExecutionMode()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("Return: ").append(header);
                        if (!StringUtils.isEmpty(declaredReturn.getStatement())) {
                            sb.append("\n")
                                .append("Expression: ")
                                .append(declaredReturn.getStatement().replaceAll("\n", StringUtils.SPACE));

                        }
                        DecisionTableMetaInfoReader.appendParameters(sb,
                            localParameterNames.toArray(new String[] {}),
                            typeOfColumns.toArray(new IOpenClass[] {}));
                        writeReturnMetaInfo(tableSyntaxNode,
                            cell,
                            sb.toString(),
                            declaredReturn.getMatchedDefinition().getDtColumnsDefinition().getUri());
                    }
                    break;
                }
            }
            c = c + cell.getWidth();
        }

        if (declaredReturn.getWidth() > 1) {
            for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1; row++) {
                grid.addMergedRegion(
                    new GridRegion(row, declaredReturn.getColumn(), row, originalTable.getSource().getWidth() - 1));
            }
        }
    }

    private static final String FUZZY_RET_VARIABLE_NAME = "$R$E$T$U$R$N";

    private static IOpenClass writeReturnStatement(IOpenClass type,
            IOpenMethod[] methodChain,
            Set<String> generatedNames,
            Map<String, Map<IOpenMethod, String>> variables,
            String insertStatement,
            StringBuilder sb) {
        if (methodChain == null) {
            return type;
        }
        String currentVariable = FUZZY_RET_VARIABLE_NAME;
        for (int j = 0; j < methodChain.length; j++) {
            String var = null;
            type = methodChain[j].getSignature().getParameterType(0);
            if (j < methodChain.length - 1) {
                Map<IOpenMethod, String> vm = variables.get(currentVariable);
                if (vm == null || vm.get(methodChain[j]) == null) {
                    var = RandomStringUtils.random(8, true, false);
                    while (generatedNames.contains(var)) { // Prevent
                        // variable
                        // duplication
                        var = RandomStringUtils.random(8, true, false);
                    }
                    generatedNames.add(var);
                    sb.append(type.getName())
                        .append(" ")
                        .append(var)
                        .append(" = new ")
                        .append(type.getName())
                        .append("();");
                    vm = variables.computeIfAbsent(currentVariable, e -> new HashMap<>());
                    vm.put(methodChain[j], var);

                    sb.append(currentVariable).append(".");
                    sb.append(methodChain[j].getName());
                    sb.append("(");
                    sb.append(var);
                    sb.append(");");
                } else {
                    var = vm.get(methodChain[j]);
                }
                currentVariable = var;
            } else {
                sb.append(currentVariable).append(".");
                sb.append(methodChain[j].getName());
                sb.append("(");
                sb.append(insertStatement);
                sb.append(");");
            }
        }
        return type;
    }

    private static void writeInputParametersToReturnMetaInfo(DecisionTable decisionTable,
            String statementInInputParameters,
            String statementInReturn) {
        MetaInfoReader metaReader = decisionTable.getSyntaxNode().getMetaInfoReader();
        if (metaReader instanceof DecisionTableMetaInfoReader) {
            DecisionTableMetaInfoReader metaInfoReader = (DecisionTableMetaInfoReader) metaReader;
            metaInfoReader.addInputParametersToReturn(statementInInputParameters, statementInReturn);
        }
    }

    private static void writeInputParametersToReturn(DecisionTable decisionTable,
            FuzzyContext fuzzyContext,
            List<DTHeader> dtHeaders,
            Set<String> generatedNames,
            Map<String, Map<IOpenMethod, String>> variables,
            StringBuilder sb,
            IBindingContext bindingContext) {
        List<FuzzyDTHeader> fuzzyReturns = dtHeaders.stream()
            .filter(e -> e instanceof FuzzyDTHeader)
            .map(e -> (FuzzyDTHeader) e)
            .filter(FuzzyDTHeader::isReturn)
            .collect(toList());
        Map<IOpenMethod[], List<Token>> m = new HashMap<>();
        for (Token token : fuzzyContext.getReturnTokens()) {
            IOpenMethod[][] returnTypeMethodChains = fuzzyContext.getMethodChainsForReturnToken(token);
            for (int i = 0; i < returnTypeMethodChains.length; i++) {
                boolean f = false;
                for (Entry<IOpenMethod[], List<Token>> entry : m.entrySet()) {
                    if (OpenLFuzzyUtils.isEqualsMethodChains(entry.getKey(), returnTypeMethodChains[i])) {
                        entry.getValue().add(token);
                        f = true;
                        break;
                    }
                }
                if (!f) {
                    List<Token> tokens = new ArrayList<>();
                    tokens.add(token);
                    m.put(returnTypeMethodChains[i], tokens);
                }
            }
        }
        for (Entry<IOpenMethod[], List<Token>> entry : m.entrySet()) {
            final IOpenMethod[] methodChain = entry.getKey();
            final boolean foundInReturns = fuzzyReturns.stream()
                .anyMatch(e -> OpenLFuzzyUtils.isEqualsMethodChains(e.getMethodsChain(), methodChain));
            if (foundInReturns) {
                continue;
            }
            FuzzyResult fuzzyResult = null;
            for (Token token : entry.getValue()) {
                List<FuzzyResult> fuzzyResults = OpenLFuzzyUtils
                    .openlFuzzyExtract(token.getValue(), fuzzyContext.getParameterTokens().getTokens(), false);
                if (fuzzyResult == null && fuzzyResults.size() == 1 || fuzzyResult != null && fuzzyResults
                    .size() == 1 && fuzzyResults.get(0).compareTo(fuzzyResult) < 0) {
                    fuzzyResult = fuzzyResults.get(0);
                }
            }
            if (fuzzyResult != null) {
                Token paramToken = fuzzyResult.getToken();
                final int paramIndex = fuzzyContext.getParameterTokens().getParameterIndex(paramToken);
                IOpenClass type = decisionTable.getSignature().getParameterType(paramIndex);
                final IOpenMethod[] paramMethodChain = fuzzyContext.getParameterTokens().getMethodsChain(paramToken);
                final String statement;
                if (paramMethodChain != null) {
                    Pair<String, IOpenClass> v = buildStatementByMethodsChain(type, paramMethodChain);
                    statement = decisionTable.getSignature().getParameterName(paramIndex) + "." + v.getKey();
                    type = v.getValue();
                } else {
                    statement = decisionTable.getSignature().getParameterName(paramIndex);
                }

                if (!isCompoundInputType(type)) {
                    Pair<String, IOpenClass> p = buildStatementByMethodsChain(fuzzyContext.getReturnType(),
                        methodChain);
                    IOpenCast cast = bindingContext.getCast(type, p.getValue());
                    if (cast != null && cast.isImplicit()) {
                        writeReturnStatement(fuzzyContext
                            .getReturnType(), methodChain, generatedNames, variables, statement, sb);
                        if (!bindingContext.isExecutionMode()) {
                            final String statementInReturn = fuzzyContext.getReturnType()
                                .getDisplayName(INamedThing.SHORT) + "." + buildStatementByMethodsChain(
                                    fuzzyContext.getReturnType(),
                                    methodChain).getKey();
                            writeInputParametersToReturnMetaInfo(decisionTable, statement, statementInReturn);
                        }
                    }
                }
            }
        }

    }

    private static void writeFuzzyReturns(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            ILogicalTable originalTable,
            IWritableGrid grid,
            FuzzyContext fuzzyContext,
            List<DTHeader> dtHeaders,
            IOpenClass compoundReturnType,
            String header,
            IBindingContext bindingContext) throws OpenLCompilationException {
        validateCompoundReturnType(compoundReturnType);

        final List<FuzzyDTHeader> fuzzyReturns = dtHeaders.stream()
            .filter(e -> (e instanceof FuzzyDTHeader) && e.isReturn())
            .map(e -> (FuzzyDTHeader) e)
            .collect(toList());

        assert (!fuzzyReturns.isEmpty());

        StringBuilder sb = new StringBuilder();
        sb.append(compoundReturnType.getName())
            .append(" ")
            .append(FUZZY_RET_VARIABLE_NAME)
            .append(" = new ")
            .append(compoundReturnType.getName())
            .append("();");

        Set<String> generatedNames = new HashSet<>();
        while (generatedNames.size() < fuzzyReturns.size()) {
            generatedNames.add(RandomStringUtils.random(8, true, false));
        }
        String[] compoundColumnParamNames = generatedNames.toArray(new String[] {});
        Map<String, Map<IOpenMethod, String>> variables = new HashMap<>();

        writeInputParametersToReturn(decisionTable,
            fuzzyContext,
            dtHeaders,
            generatedNames,
            variables,
            sb,
            bindingContext);

        int i = 0;
        for (FuzzyDTHeader fuzzyDTHeader : fuzzyReturns) {
            IOpenClass type = writeReturnStatement(compoundReturnType,
                fuzzyDTHeader.getMethodsChain(),
                generatedNames,
                variables,
                compoundColumnParamNames[i],
                sb);

            grid.setCellValue(fuzzyDTHeader.getColumn(), 2, type.getName() + " " + compoundColumnParamNames[i]);

            if (fuzzyDTHeader.getWidth() > 1) {
                grid.addMergedRegion(new GridRegion(2,
                    fuzzyDTHeader.getColumn(),
                    2,
                    fuzzyDTHeader.getColumn() + fuzzyDTHeader.getWidth() - 1));
            }

            if (!bindingContext.isExecutionMode()) {
                int lastRowInHeader = getLastRowHeader(originalTable,
                    fuzzyDTHeader.getColumn(),
                    originalTable.getCell(0, 0).getHeight());
                ICell cell = originalTable.getSource().getCell(fuzzyDTHeader.getColumn(), lastRowInHeader);
                String statement = buildStatementByMethodsChain(compoundReturnType, fuzzyDTHeader.getMethodsChain())
                    .getKey();
                StringBuilder sb1 = new StringBuilder();
                sb1.append("Return: ").append(header);

                if (!StringUtils.isEmpty(statement)) {
                    sb1.append("\n")
                        .append("Expression: value for return ")
                        .append(compoundReturnType.getDisplayName(INamedThing.SHORT))
                        .append(".")
                        .append(statement);
                }
                DecisionTableMetaInfoReader.appendParameters(sb1, null, new IOpenClass[] { type });

                writeReturnMetaInfo(tableSyntaxNode, cell, sb1.toString(), null);
            }
            i++;
        }
        sb.append(FUZZY_RET_VARIABLE_NAME).append(";");
        grid.setCellValue(fuzzyReturns.get(0).getColumn(), 0, header);
        grid.setCellValue(fuzzyReturns.get(0).getColumn(), 1, sb.toString());
        int j = fuzzyReturns.size() - 1;
        if (fuzzyReturns.get(j).getColumn() + fuzzyReturns.get(j).getWidth() - fuzzyReturns.get(0).getColumn() > 1) {
            for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1; row++) {
                grid.addMergedRegion(new GridRegion(row,
                    fuzzyReturns.get(0).getColumn(),
                    row,
                    fuzzyReturns.get(j).getColumn() + fuzzyReturns.get(j).getWidth() - 1));
            }
        }
    }

    private static void writeSimpleDTReturnHeader(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            ILogicalTable originalTable,
            IWritableGrid grid,
            SimpleReturnDTHeader simpleReturnDTHeader,
            String header,
            int collectParameterIndex,
            IBindingContext bindingContext) {
        grid.setCellValue(simpleReturnDTHeader.getColumn(), 0, header);

        if (tableSyntaxNode.getHeader().getCollectParameters().length > 0) {
            grid.setCellValue(simpleReturnDTHeader.getColumn(),
                2,
                tableSyntaxNode.getHeader().getCollectParameters()[collectParameterIndex]);
        }

        if (!bindingContext.isExecutionMode()) {
            StringBuilder sb = new StringBuilder();
            sb.append("Return: ").append(header);
            ICell cell = originalTable.getSource().getCell(simpleReturnDTHeader.getColumn(), 0);
            if (!StringUtils.isEmpty(simpleReturnDTHeader.getStatement())) {
                sb.append("\n").append("Expression: ").append(simpleReturnDTHeader.getStatement());
            }
            DecisionTableMetaInfoReader
                .appendParameters(sb, null, new IOpenClass[] { decisionTable.getHeader().getType() });
            writeReturnMetaInfo(tableSyntaxNode, cell, sb.toString(), null);
        }

        if (simpleReturnDTHeader.getWidth() > 1) {
            for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT; row++) {
                grid.addMergedRegion(new GridRegion(row,
                    simpleReturnDTHeader.getColumn(),
                    row,
                    simpleReturnDTHeader.getColumn() + simpleReturnDTHeader.getWidth() - 1));
            }
        }
    }

    private static void writeReturns(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            ILogicalTable originalTable,
            IWritableGrid grid,
            FuzzyContext fuzzyContext,
            List<DTHeader> dtHeaders,
            IBindingContext bindingContext) throws OpenLCompilationException {
        boolean isCollect = isCollect(tableSyntaxNode);

        if (isCollect) {
            validateCollectSyntaxNode(tableSyntaxNode, decisionTable, bindingContext);
        }

        if (isLookup(tableSyntaxNode)) {
            int firstReturnColumn = dtHeaders.stream()
                .filter(e -> e.isCondition() || e.isAction())
                .mapToInt(e -> e.getColumn() + e.getWidth())
                .max()
                .orElse(0);
            grid.setCellValue(firstReturnColumn, 0, isCollect ? CRET1_COLUMN_NAME : RET1_COLUMN_NAME);
            return;
        }

        if (dtHeaders.stream()
            .filter(DTHeader::isReturn)
            .anyMatch(e -> e.getColumn() + e.getWidth() - 1 >= originalTable.getSource().getWidth())) {
            throw new OpenLCompilationException("Wrong table structure: There is no column for return values");
        }

        int retNum = 1;
        int cretNum = 1;
        int i = 0;
        int collectParameterIndex = 0;
        int keyNum = 1;
        boolean skipFuzzyReturns = false;
        for (DTHeader dtHeader : dtHeaders) {
            if (dtHeader.isReturn()) {
                if (dtHeader instanceof DeclaredDTHeader) {
                    writeReturnWithReturnDtHeader(tableSyntaxNode,
                        originalTable,
                        grid,
                        (DeclaredDTHeader) dtHeader,
                        isCollect ? DecisionTableColumnHeaders.COLLECT_RETURN.getHeaderKey() + cretNum++
                                  : DecisionTableColumnHeaders.RETURN.getHeaderKey() + retNum++,
                        bindingContext);
                } else if (dtHeader instanceof SimpleReturnDTHeader) {
                    boolean isKey = false;
                    String header;
                    if (isCollect && tableSyntaxNode.getHeader()
                        .getCollectParameters().length > 1 && (i == 0) && Map.class
                            .isAssignableFrom(decisionTable.getType().getInstanceClass())) {
                        header = DecisionTableColumnHeaders.KEY.getHeaderKey() + keyNum++;
                        isKey = true;
                    } else {
                        header = isCollect ? DecisionTableColumnHeaders.COLLECT_RETURN.getHeaderKey() + cretNum++
                                           : DecisionTableColumnHeaders.RETURN.getHeaderKey() + retNum++;
                    }
                    writeSimpleDTReturnHeader(tableSyntaxNode,
                        decisionTable,
                        originalTable,
                        grid,
                        (SimpleReturnDTHeader) dtHeader,
                        header,
                        collectParameterIndex,
                        bindingContext);
                    i++;
                    if (isKey) {
                        collectParameterIndex++;
                    }
                } else if (dtHeader instanceof FuzzyDTHeader && !skipFuzzyReturns) {
                    IOpenClass compoundReturnType = getCompoundReturnType(tableSyntaxNode,
                        decisionTable,
                        bindingContext);

                    writeFuzzyReturns(tableSyntaxNode,
                        decisionTable,
                        originalTable,
                        grid,
                        fuzzyContext,
                        dtHeaders,
                        compoundReturnType,
                        isCollect ? DecisionTableColumnHeaders.COLLECT_RETURN.getHeaderKey() + retNum++
                                  : DecisionTableColumnHeaders.RETURN.getHeaderKey() + retNum++,
                        bindingContext);
                    skipFuzzyReturns = true;
                }
            }
        }
    }

    private static void writeDeclaredDtHeader(DecisionTable decisionTable,
            ILogicalTable originalTable,
            IWritableGrid grid,
            DeclaredDTHeader dtHeader,
            String header,
            IBindingContext bindingContext) {
        int column = dtHeader.getColumn();

        grid.setCellValue(column, 0, header);
        grid.setCellValue(column, 1, dtHeader.getStatement());

        int firstColumn = column;

        for (int j = 0; j < dtHeader.getColumnParameters().length; j++) {
            int firstTitleColumn = column;
            List<String> parameterNames = new ArrayList<>();
            List<IOpenClass> typeOfColumns = new ArrayList<>();
            for (int k = 0; k < dtHeader.getColumnParameters()[j].length; k++) {
                IParameterDeclaration param = dtHeader.getColumnParameters()[j][k];
                if (param != null) {
                    String paramName = dtHeader.getMatchedDefinition().getLocalParameterName(param.getName());
                    parameterNames.add(paramName);
                    grid.setCellValue(column,
                        2,
                        param.getType().getName() + (paramName != null ? " " + paramName : ""));
                    typeOfColumns.add(param.getType());
                } else {
                    parameterNames.add(null);
                    typeOfColumns.add(dtHeader.getCompositeMethod().getType());
                }
                int h = originalTable.getSource().getCell(column, 0).getHeight();
                int w1 = originalTable.getSource().getCell(column, h).getWidth();
                if (w1 > 1) {
                    grid.addMergedRegion(new GridRegion(2, column, 2, column + w1 - 1));
                }

                column = column + w1;
            }

            if (!bindingContext.isExecutionMode()) {
                if (dtHeader.isAction()) {
                    writeMetaInfoForAction(originalTable,
                        decisionTable,
                        firstTitleColumn,
                        header,
                        parameterNames.toArray(new String[] {}),
                        dtHeader.getStatement(),
                        typeOfColumns.toArray(new IOpenClass[] {}),
                        dtHeader.getMatchedDefinition().getDtColumnsDefinition().getUri());
                } else if (dtHeader.isCondition()) {
                    writeMetaInfoForVCondition(originalTable,
                        decisionTable,
                        firstTitleColumn,
                        header,
                        parameterNames.toArray(new String[] {}),
                        dtHeader.getStatement(),
                        typeOfColumns.toArray(new IOpenClass[] {}),
                        dtHeader.getMatchedDefinition().getDtColumnsDefinition().getUri(),
                        null);
                }
            }
        }
        // merge columns
        if (column - firstColumn > 1) {
            for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1; row++) {
                grid.addMergedRegion(new GridRegion(row, firstColumn, row, column - 1));
            }
        }
    }

    private static void writeActions(DecisionTable decisionTable,
            ILogicalTable originalTable,
            IWritableGrid grid,
            List<DTHeader> dtHeaders,
            IBindingContext bindingContext) throws OpenLCompilationException {
        List<DTHeader> actions = dtHeaders.stream()
            .filter(e -> e.isAction())
            .collect(collectingAndThen(toList(), Collections::unmodifiableList));

        int i = 0;
        for (DTHeader action : actions) {
            if (action.getColumn() >= originalTable.getSource().getWidth()) {
                String message = "Wrong table structure: Wrong number of action columns!";
                throw new OpenLCompilationException(message);
            }

            DeclaredDTHeader declaredAction = (DeclaredDTHeader) action;
            String header = (DecisionTableColumnHeaders.ACTION.getHeaderKey() + (i + 1)).intern();
            writeDeclaredDtHeader(decisionTable, originalTable, grid, declaredAction, header, bindingContext);
            i++;
        }
    }

    private static boolean isVCondition(DTHeader condition) {
        return condition.isCondition() && !condition.isHCondition();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private static boolean getMinMaxOrder(ILogicalTable originalTable,
            NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter,
            int firstColumnHeight,
            int column,
            IOpenClass type) {
        int h = firstColumnHeight;
        int height = originalTable.getSource().getHeight();
        int t1 = 0;
        int t2 = 0;
        IString2DataConvertor<?> string2DataConvertor = String2DataConvertorFactory
            .getConvertor(type.getInstanceClass());
        while (h < height) {
            ICell cell1 = originalTable.getSource().getCell(column, h);
            String s1 = cell1.getStringValue();
            Object o1 = string2DataConvertor.parse(s1, null);

            ICell cell2 = originalTable.getSource()
                .getCell(column + numberOfColumnsUnderTitleCounter.getWidth(column, 0), h);
            String s2 = cell2.getStringValue();
            Object o2 = string2DataConvertor.parse(s2, null);

            if (o1 instanceof Comparable && o2 instanceof Comparable) {
                if (((Comparable) o1).compareTo(o2) > 0) {
                    t1++;
                } else if (((Comparable) o1).compareTo(o2) < 0) {
                    t2++;
                }
            }

            h = h + cell1.getHeight();
        }
        return t1 <= t2;
    }

    private static final String[] MIN_MAX_ORDER = new String[] { "min", "max" };
    private static final String[] MAX_MIN_ORDER = new String[] { "max", "min" };

    private static void writeConditions(DecisionTable decisionTable,
            ILogicalTable originalTable,
            IWritableGrid grid,
            NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter,
            List<DTHeader> dtHeaders,
            int numberOfHcondition,
            int firstColumnHeight,
            IBindingContext bindingContext) throws OpenLCompilationException {

        List<DTHeader> conditions = dtHeaders.stream()
            .filter(DTHeader::isCondition)
            .collect(collectingAndThen(toList(), Collections::unmodifiableList));

        int numOfVCondition = 0;
        int numOfHCondition = 0;

        int firstColumnForHConditions = dtHeaders.stream()
            .filter(e -> e.isCondition() && !e.isHCondition() || e.isAction())
            .mapToInt(e -> e.getColumn() + e.getWidth())
            .max()
            .orElse(0);

        for (DTHeader condition : conditions) {
            int column = condition.getColumn();
            if (column > originalTable.getSource().getWidth()) {
                String message = "Wrong table structure: Columns count is less than parameters count";
                throw new OpenLCompilationException(message);
            }
            if (column == originalTable.getSource().getWidth()) {
                String message = "Wrong table structure: There is no column for return values";
                throw new OpenLCompilationException(message);
            }
            // write headers
            //
            String header;
            if (isVCondition(condition)) {
                // write vertical condition
                //
                numOfVCondition++;
                if (numOfVCondition == 1 && numberOfHcondition == 0 && conditions.size() < 2) {
                    header = (DecisionTableColumnHeaders.MERGED_CONDITION.getHeaderKey() + numOfVCondition).intern();
                } else {
                    header = (DecisionTableColumnHeaders.CONDITION.getHeaderKey() + numOfVCondition).intern();
                }
            } else {
                // write horizontal condition
                //
                numOfHCondition++;
                header = (DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey() + numOfHCondition).intern();
            }

            if (condition instanceof DeclaredDTHeader) {
                writeDeclaredDtHeader(decisionTable,
                    originalTable,
                    grid,
                    (DeclaredDTHeader) condition,
                    header,
                    bindingContext);
            } else {
                grid.setCellValue(column, 0, header);
                final int numberOfColumnsUnderTitle = numberOfColumnsUnderTitleCounter.get(column);
                IOpenClass type = getTypeForCondition(decisionTable, condition);
                if (condition instanceof FuzzyDTHeader && numberOfColumnsUnderTitle == 2 && (type.getInstanceClass()
                    .isPrimitive() || Comparable.class.isAssignableFrom(type.getInstanceClass()))) {
                    boolean minMaxOrder = getMinMaxOrder(originalTable,
                        numberOfColumnsUnderTitleCounter,
                        firstColumnHeight,
                        column,
                        type);
                    String statement;
                    if (minMaxOrder) {
                        statement = "min <= " + condition.getStatement() + " && " + condition.getStatement() + " < max";
                    } else {
                        statement = "max > " + condition.getStatement() + " && " + condition.getStatement() + " >= min";
                    }
                    grid.setCellValue(column, 1, statement);
                    grid.setCellValue(column,
                        2,
                        type.getDisplayName(INamedThing.SHORT) + " " + (minMaxOrder ? "min" : "max"));
                    int w1 = numberOfColumnsUnderTitleCounter.getWidth(column, 0);
                    if (w1 > 1) {
                        grid.addMergedRegion(new GridRegion(2, column, 2, column + w1 - 1));
                    }
                    grid.setCellValue(column + w1,
                        2,
                        type.getDisplayName(INamedThing.SHORT) + " " + (minMaxOrder ? "max" : "min"));
                    int w2 = numberOfColumnsUnderTitleCounter.getWidth(column, 1);
                    if (w2 > 1) {
                        grid.addMergedRegion(new GridRegion(2, column + w1, 2, column + w1 + w2 - 1));
                    }
                    if (isVCondition(condition)) {
                        if (!bindingContext.isExecutionMode()) {
                            writeMetaInfoForVCondition(originalTable,
                                decisionTable,
                                column,
                                header,
                                (minMaxOrder ? MIN_MAX_ORDER : MAX_MIN_ORDER),
                                statement,
                                new IOpenClass[] { type, type },
                                null,
                                null);
                        }
                        if (condition.getWidth() > 1) {
                            for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT - 1; row++) {
                                grid.addMergedRegion(
                                    new GridRegion(row, column, row, column + condition.getWidth() - 1));
                            }
                        }
                    }
                } else {
                    grid.setCellValue(column, 1, condition.getStatement());
                    // Set type of condition values(for Ranges and Array)
                    Pair<String, IOpenClass> typeOfValue = getTypeForConditionColumn(decisionTable,
                        originalTable,
                        condition,
                        numOfHCondition,
                        firstColumnForHConditions,
                        bindingContext);
                    grid.setCellValue(column, 2, typeOfValue.getLeft());
                    if (isVCondition(condition)) {
                        if (!bindingContext.isExecutionMode()) {
                            writeMetaInfoForVCondition(originalTable,
                                decisionTable,
                                column,
                                header,
                                null,
                                condition.getStatement(),
                                new IOpenClass[] { typeOfValue.getRight() },
                                null,
                                null);
                        }
                        if (condition.getWidth() > 1) {
                            for (int row = 0; row < IDecisionTableConstants.SIMPLE_DT_HEADERS_HEIGHT; row++) {
                                grid.addMergedRegion(
                                    new GridRegion(row, column, row, column + condition.getWidth() - 1));
                            }
                        }
                    }
                }
            }
        }

        if (!bindingContext.isExecutionMode()) {
            writeMetaInfoForHConditions(originalTable, decisionTable, conditions);
        }
    }

    private static void writeMetaInfoForVCondition(ILogicalTable originalTable,
            DecisionTable decisionTable,
            int column,
            String header,
            String[] parameterNames,
            String conditionStatement,
            IOpenClass[] typeOfColumns,
            String url,
            String additionalDetails) {
        assert (header != null);
        MetaInfoReader metaReader = decisionTable.getSyntaxNode().getMetaInfoReader();
        if (metaReader instanceof DecisionTableMetaInfoReader) {
            DecisionTableMetaInfoReader metaInfoReader = (DecisionTableMetaInfoReader) metaReader;
            ICell cell = originalTable.getSource().getCell(column, 0);
            metaInfoReader.addSimpleRulesCondition(cell.getAbsoluteRow(),
                cell.getAbsoluteColumn(),
                header,
                parameterNames,
                conditionStatement,
                typeOfColumns,
                url,
                additionalDetails);
        }
    }

    private static void writeMetaInfoForAction(ILogicalTable originalTable,
            DecisionTable decisionTable,
            int column,
            String header,
            String[] parameterNames,
            String conditionStatement,
            IOpenClass[] typeOfColumns,
            String url) {
        assert (header != null);
        MetaInfoReader metaReader = decisionTable.getSyntaxNode().getMetaInfoReader();
        if (metaReader instanceof DecisionTableMetaInfoReader) {
            DecisionTableMetaInfoReader metaInfoReader = (DecisionTableMetaInfoReader) metaReader;
            ICell cell = originalTable.getSource().getCell(column, 0);
            metaInfoReader.addSimpleRulesAction(cell.getAbsoluteRow(),
                cell.getAbsoluteColumn(),
                header,
                parameterNames,
                conditionStatement,
                typeOfColumns,
                url,
                null);
        }
    }

    private static void writeMetaInfoForHConditions(ILogicalTable originalTable,
            DecisionTable decisionTable,
            List<DTHeader> conditions) {
        MetaInfoReader metaInfoReader = decisionTable.getSyntaxNode().getMetaInfoReader();
        int j = 0;
        for (DTHeader condition : conditions) {
            if (isVCondition(condition)) {
                continue;
            }
            int column = condition.getColumn() - ((SimpleDTHeader) condition).getRow();
            while (column < originalTable.getSource().getWidth()) {
                ICell cell = originalTable.getSource().getCell(column, j);
                String cellValue = cell.getStringValue();
                if (cellValue != null && metaInfoReader instanceof DecisionTableMetaInfoReader) {
                    ((DecisionTableMetaInfoReader) metaInfoReader).addSimpleRulesCondition(cell.getAbsoluteRow(),
                        cell.getAbsoluteColumn(),
                        (DecisionTableColumnHeaders.HORIZONTAL_CONDITION.getHeaderKey() + (j + 1)).intern(),
                        null,
                        decisionTable.getSignature().getParameterName(condition.getMethodParameterIndex()),
                        new IOpenClass[] {
                                decisionTable.getSignature().getParameterType(condition.getMethodParameterIndex()) },
                        null,
                        null);
                }
                column = column + cell.getWidth();
            }
            j++;
        }
    }

    private static void parseRec(ISyntaxNode node,
            MutableBoolean chain,
            boolean inChain,
            List<IdentifierNode> identifierNodes) {
        for (int i = 0; i < node.getNumberOfChildren(); i++) {
            if ("identifier".equals(node.getChild(i).getType())) {
                if (!chain.booleanValue()) {
                    identifierNodes.add((IdentifierNode) node.getChild(i));
                    if (inChain) {
                        chain.setTrue();
                    }
                }
            } else if ("chain".equals(node.getChild(i).getType())) {
                boolean f = chain.booleanValue();
                parseRec(node.getChild(i), chain, true, identifierNodes);
                chain.setValue(f);
            } else if ("function".equals(node.getChild(i).getType())) {
                parseRec(node.getChild(i), new MutableBoolean(false), false, identifierNodes);
            } else {
                parseRec(node.getChild(i), chain, inChain, identifierNodes);
            }
        }
    }

    @SafeVarargs
    private static String replaceIdentifierNodeNamesInCode(String code,
            List<IdentifierNode> identifierNodes,
            Map<String, String>... namesMaps) {
        final TextInfo textInfo = new TextInfo(code);
        Collections.sort(identifierNodes,
            Comparator.<IdentifierNode> comparingInt(e -> e.getLocation().getStart().getAbsolutePosition(textInfo))
                .reversed());

        StringBuilder sb = new StringBuilder(code);
        for (IdentifierNode identifierNode : identifierNodes) {
            int start = identifierNode.getLocation().getStart().getAbsolutePosition(textInfo);
            int end = identifierNode.getLocation().getEnd().getAbsolutePosition(textInfo);
            for (Map<String, String> m : namesMaps) {
                if (m.containsKey(identifierNode.getIdentifier())) {
                    sb.replace(start, end + 1, m.get(identifierNode.getIdentifier()));
                }
            }
        }
        return sb.toString();
    }

    private static MatchedDefinition matchByDTColumnDefinition(DecisionTable decisionTable,
            DTColumnsDefinition definition,
            IBindingContext bindingContext) {
        IOpenMethodHeader header = decisionTable.getHeader();
        if (definition.isReturn()) {
            IOpenClass methodReturnType = header.getType();
            IOpenClass definitionType = definition.getCompositeMethod().getType();
            IOpenCast openCast = bindingContext.getCast(definitionType, methodReturnType);
            if (openCast == null || !openCast.isImplicit()) {
                return null;
            }
        }

        List<IdentifierNode> identifierNodes = new ArrayList<>();
        parseRec(definition.getCompositeMethod().getMethodBodyBoundNode().getSyntaxNode(),
            new MutableBoolean(false),
            false,
            identifierNodes);
        Set<String> methodParametersUsedInExpression = new HashSet<>();

        Map<String, IParameterDeclaration> localParameters = new HashMap<>();
        for (IParameterDeclaration localParameter : definition.getLocalParameters()) {
            localParameters.put(localParameter.getName(), localParameter);
        }

        for (IdentifierNode identifierNode : identifierNodes) {
            if (!localParameters.containsKey(identifierNode.getIdentifier())) {
                methodParametersUsedInExpression.add(identifierNode.getIdentifier());
            }
        }

        Map<String, String> methodParametersToRename = new HashMap<>();
        Set<Integer> usedMethodParameterIndexes = new HashSet<>();
        Iterator<String> itr = methodParametersUsedInExpression.iterator();
        MatchType matchType = MatchType.STRICT;
        Map<String, Integer> paramToIndex = new HashMap<>();
        while (itr.hasNext()) {
            String param = itr.next();
            int j = -1;
            for (int i = 0; i < definition.getHeader().getSignature().getNumberOfParameters(); i++) {
                if (param.equals(definition.getHeader().getSignature().getParameterName(i))) {
                    j = i;
                    break;
                }
            }
            if (j < 0) { // Constants, etc
                itr.remove();
                continue;
            }
            paramToIndex.put(param, j);
            IOpenClass type = definition.getHeader().getSignature().getParameterType(j);
            for (int i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
                if (param.equals(header.getSignature().getParameterName(i)) && type
                    .equals(header.getSignature().getParameterType(i))) {
                    usedMethodParameterIndexes.add(i);
                    methodParametersToRename.put(param, param);
                    break;
                }
            }
        }

        MatchType[] matchTypes = { MatchType.STRICT_CASTED,
                MatchType.METHOD_PARAMS_RENAMED,
                MatchType.METHOD_PARAMS_RENAMED_CASTED };

        for (MatchType mt : matchTypes) {
            itr = methodParametersUsedInExpression.iterator();
            while (itr.hasNext()) {
                String param = itr.next();
                if (methodParametersToRename.containsKey(param)) {
                    continue;
                }
                int j = paramToIndex.get(param);
                IOpenClass type = definition.getHeader().getSignature().getParameterType(j);
                boolean duplicatedMatch = false;
                for (int i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
                    boolean predicate = true;
                    IOpenCast openCast = bindingContext.getCast(header.getSignature().getParameterType(i), type);
                    switch (mt) {
                        case METHOD_PARAMS_RENAMED_CASTED:
                            predicate = openCast != null && openCast.isImplicit();
                            break;
                        case STRICT_CASTED:
                            predicate = openCast != null && openCast.isImplicit() && param
                                .equals(header.getSignature().getParameterName(i));
                            break;
                        case METHOD_PARAMS_RENAMED:
                            predicate = type.equals(header.getSignature().getParameterType(i));
                            break;
                        default:
                            throw new IllegalStateException();
                    }

                    if (!usedMethodParameterIndexes.contains(i) && predicate) {
                        if (duplicatedMatch) {
                            return null;
                        }
                        duplicatedMatch = true;
                        matchType = mt;
                        usedMethodParameterIndexes.add(i);
                        String newParam = null;
                        switch (mt) {
                            case STRICT_CASTED:
                            case METHOD_PARAMS_RENAMED_CASTED:
                                String typeName = type.getInstanceClass().getSimpleName();
                                if (bindingContext.findType(ISyntaxConstants.THIS_NAMESPACE, typeName) == null) {
                                    typeName = type.getJavaName();
                                }
                                newParam = "((" + typeName + ")" + header.getSignature().getParameterName(i) + ")";
                                break;
                            case METHOD_PARAMS_RENAMED:
                                newParam = header.getSignature().getParameterName(i);
                                break;
                            default:
                                throw new IllegalStateException();
                        }
                        methodParametersToRename.put(param, newParam);
                    }
                }
            }
        }

        if (usedMethodParameterIndexes.size() != methodParametersUsedInExpression.size()) {
            return null;
        }

        Set<String> methodParameterNames = new HashSet<>();
        for (int i = 0; i < header.getSignature().getNumberOfParameters(); i++) {
            methodParameterNames.add(header.getSignature().getParameterName(i));
        }

        Map<String, String> renamedLocalParameters = new HashMap<>();
        for (String paramName : methodParameterNames) {
            if (localParameters.containsKey(paramName)) {
                int k = 1;
                String newParamName = "_" + paramName;
                while (localParameters.containsKey(newParamName) || renamedLocalParameters
                    .containsValue(newParamName) || methodParameterNames.contains(newParamName)) {
                    newParamName = "_" + paramName + "_" + k;
                    k++;
                }
                renamedLocalParameters.put(paramName, newParamName);
            }
        }

        final String code = definition.getCompositeMethod()
            .getMethodBodyBoundNode()
            .getSyntaxNode()
            .getModule()
            .getCode();

        String newCode = replaceIdentifierNodeNamesInCode(code,
            identifierNodes,
            methodParametersToRename,
            renamedLocalParameters);

        int[] usedMethodParameterIndexesArray = ArrayUtils
            .toPrimitive(usedMethodParameterIndexes.toArray(new Integer[] {}));

        switch (matchType) {
            case STRICT:
                return new MatchedDefinition(definition,
                    newCode,
                    usedMethodParameterIndexesArray,
                    renamedLocalParameters,
                    renamedLocalParameters.isEmpty() ? MatchType.STRICT : MatchType.STRICT_LOCAL_PARAMS_RENAMED);
            case STRICT_CASTED:
                return new MatchedDefinition(definition,
                    newCode,
                    usedMethodParameterIndexesArray,
                    renamedLocalParameters,
                    renamedLocalParameters.isEmpty() ? MatchType.STRICT_CASTED
                                                     : MatchType.STRICT_CASTED_LOCAL_PARAMS_RENAMED);
            case METHOD_PARAMS_RENAMED:
                return new MatchedDefinition(definition,
                    newCode,
                    usedMethodParameterIndexesArray,
                    renamedLocalParameters,
                    renamedLocalParameters.isEmpty() ? MatchType.METHOD_PARAMS_RENAMED
                                                     : MatchType.METHOD_LOCAL_PARAMS_RENAMED);
            case METHOD_PARAMS_RENAMED_CASTED:
                return new MatchedDefinition(definition,
                    newCode,
                    usedMethodParameterIndexesArray,
                    renamedLocalParameters,
                    renamedLocalParameters.isEmpty() ? MatchType.METHOD_PARAMS_RENAMED_CASTED
                                                     : MatchType.METHOD_LOCAL_PARAMS_RENAMED_CASTED);
            default:
                return null;
        }
    }

    private static ParameterTokens buildParameterTokens(DecisionTable decisionTable) {
        int numberOfParameters = decisionTable.getSignature().getNumberOfParameters();
        Map<Token, Integer> tokenToParameterIndex = new HashMap<>();
        Map<Token, IOpenMethod[]> tokenToMethodsChain = new HashMap<>();
        Set<Token> tokens = new HashSet<>();
        Set<Token> tokensToIgnore = new HashSet<>();
        for (int i = 0; i < numberOfParameters; i++) {
            IOpenClass parameterType = decisionTable.getSignature().getParameterType(i);
            if (isCompoundInputType(parameterType) && !parameterType.isArray()) {
                Map<Token, IOpenMethod[][]> openClassFuzzyTokens = OpenLFuzzyUtils
                    .tokensMapToOpenClassGetterMethodsRecursively(parameterType,
                        decisionTable.getSignature().getParameterName(i),
                        1);
                for (Map.Entry<Token, IOpenMethod[][]> entry : openClassFuzzyTokens.entrySet()) {
                    if (entry.getValue().length == 1 && !tokensToIgnore.contains(entry.getKey())) {
                        if (!tokens.contains(entry.getKey())) {
                            tokens.add(entry.getKey());
                            tokenToParameterIndex.put(entry.getKey(), i);
                            tokenToMethodsChain.put(entry.getKey(), entry.getValue()[0]);
                        } else {
                            tokens.remove(entry.getKey());
                            tokenToParameterIndex.remove(entry.getKey());
                            tokenToMethodsChain.remove(entry.getKey());
                            tokensToIgnore.add(entry.getKey());
                        }
                    }
                }
            }
        }
        for (int i = 0; i < numberOfParameters; i++) {
            String tokenString = OpenLFuzzyUtils.toTokenString(decisionTable.getSignature().getParameterName(i));
            Token token = new Token(tokenString, 0);
            tokenToParameterIndex.put(token, i);
            tokens.add(token);
        }

        return new ParameterTokens(tokens.toArray(new Token[] {}), tokenToParameterIndex, tokenToMethodsChain);
    }

    private static void matchWithFuzzySearchRec(DecisionTable decisionTable,
            IGridTable gridTable,
            FuzzyContext fuzzyContext,
            List<DTHeader> dtHeaders,
            int numberOfHcondition,
            int firstColumnHeight,
            int columnWidth,
            int w,
            int h,
            StringBuilder sb,
            int sourceTableColumn,
            IBindingContext bindingContext,
            boolean onlyReturns) {
        String d = gridTable.getCell(w, h).getStringValue();
        int w0 = gridTable.getCell(w, h).getWidth();
        int h0 = gridTable.getCell(w, h).getHeight();
        int prev = sb.length();
        if (sb.length() == 0) {
            sb.append(d);
        } else {
            sb.append(StringUtils.SPACE);
            sb.append("/");
            sb.append(StringUtils.SPACE);
            sb.append(d);
        }
        if (h + h0 < firstColumnHeight) {
            int w2 = w;
            while (w2 < w + w0) {
                int w1 = gridTable.getCell(w2, h + h0).getWidth();
                matchWithFuzzySearchRec(decisionTable,
                    gridTable,
                    fuzzyContext,
                    dtHeaders,
                    numberOfHcondition,
                    firstColumnHeight,
                    columnWidth,
                    w2,
                    h + h0,
                    sb,
                    sourceTableColumn,
                    bindingContext,
                    onlyReturns);
                w2 = w2 + w1;
            }
        } else {
            String tokenizedTitleString = OpenLFuzzyUtils.toTokenString(sb.toString());
            if (fuzzyContext.isFuzzySupportsForReturnType()) {
                List<FuzzyResult> fuzzyResults = OpenLFuzzyUtils
                    .openlFuzzyExtract(sb.toString(), fuzzyContext.getReturnTokens(), true);
                for (FuzzyResult fuzzyResult : fuzzyResults) {
                    IOpenMethod[][] methodChains = fuzzyContext.getMethodChainsForReturnToken(fuzzyResult.getToken());
                    assert (methodChains != null);
                    for (int j = 0; j < methodChains.length; j++) {
                        assert (methodChains[j] != null);
                        dtHeaders.add(new FuzzyDTHeader(-1,
                            null,
                            sb.toString(),
                            methodChains[j],
                            sourceTableColumn + w,
                            w0,
                            fuzzyResult,
                            true));
                    }
                }
            }
            if (!onlyReturns) {
                List<FuzzyResult> fuzzyResults = OpenLFuzzyUtils
                    .openlFuzzyExtract(tokenizedTitleString, fuzzyContext.getParameterTokens().getTokens(), true);
                for (FuzzyResult fuzzyResult : fuzzyResults) {
                    int paramIndex = fuzzyContext.getParameterTokens().getParameterIndex(fuzzyResult.getToken());
                    IOpenMethod[] methodsChain = fuzzyContext.getParameterTokens()
                        .getMethodsChain(fuzzyResult.getToken());
                    StringBuilder conditionStatement = new StringBuilder(
                        decisionTable.getSignature().getParameterName(paramIndex));
                    if (methodsChain != null) {
                        Pair<String, IOpenClass> c = buildStatementByMethodsChain(
                            decisionTable.getSignature().getParameterType(paramIndex),
                            methodsChain);
                        String chainStatement = c.getLeft();
                        conditionStatement.append(".");
                        conditionStatement.append(chainStatement);
                    }

                    dtHeaders.add(new FuzzyDTHeader(paramIndex,
                        conditionStatement.toString(),
                        sb.toString(),
                        methodsChain,
                        sourceTableColumn + w,
                        w0,
                        fuzzyResult,
                        false));
                }
            }
        }
        sb.delete(prev, sb.length());
    }

    private static List<DTHeader> matchWithFuzzySearch(DecisionTable decisionTable,
            ILogicalTable originalTable,
            FuzzyContext fuzzyContext,
            int column,
            int numberOfHcondition,
            List<DTHeader> dtHeaders,
            int firstColumnHeight,
            IBindingContext bindingContext,
            boolean onlyReturns) {
        if (onlyReturns && !fuzzyContext.isFuzzySupportsForReturnType()) {
            return Collections.emptyList();
        }
        int w = originalTable.getSource().getCell(column, 0).getWidth();
        IGridTable gt = originalTable.getSource().getSubtable(column, 0, w, firstColumnHeight);
        List<DTHeader> newDtHeaders = new ArrayList<>();
        matchWithFuzzySearchRec(decisionTable,
            gt,
            fuzzyContext,
            newDtHeaders,
            numberOfHcondition,
            firstColumnHeight,
            w,
            0,
            0,
            new StringBuilder(),
            column,
            bindingContext,
            onlyReturns);
        dtHeaders.addAll(newDtHeaders);
        return Collections.unmodifiableList(newDtHeaders);
    }

    private static boolean isCompatibleHeaders(DTHeader a, DTHeader b) {
        int c1 = a.getColumn();
        int c2 = a.getColumn() + a.getWidth() - 1;
        int d1 = b.getColumn();
        int d2 = b.getColumn() + b.getWidth() - 1;

        if (c1 <= d1 && d1 <= c2 || c1 <= d2 && d2 <= c2 || d1 <= c2 && c2 <= d2 || d1 <= c1 && c1 <= d2) {
            return false;
        }

        if ((a.isCondition() && b.isAction() || a.isAction() && b.isReturn() || a.isCondition() && b
            .isReturn()) && c1 >= d1) {
            return false;
        }
        if ((b.isCondition() && a.isAction() || b.isAction() && a.isReturn() || b.isCondition() && a
            .isReturn()) && d1 >= c1) {
            return false;
        }

        if ((a instanceof FuzzyDTHeader) && b instanceof FuzzyDTHeader) {
            FuzzyDTHeader a1 = (FuzzyDTHeader) a;
            FuzzyDTHeader b1 = (FuzzyDTHeader) b;
            if (a1.isCondition() && b1
                .isCondition() && a1.getMethodParameterIndex() == b1.getMethodParameterIndex() && Arrays
                    .deepEquals(a1.getMethodsChain(), b1.getMethodsChain())) {
                return false;
            }

            if (a1.isReturn() && b1.isReturn() && methodsChainsIsCrossed(a1.getMethodsChain(), b1.getMethodsChain())) {
                return false;
            }
        }
        if ((a instanceof DeclaredDTHeader) && b instanceof DeclaredDTHeader) {
            DeclaredDTHeader a1 = (DeclaredDTHeader) a;
            DeclaredDTHeader b1 = (DeclaredDTHeader) b;
            if (a1.getMatchedDefinition()
                .getDtColumnsDefinition()
                .equals(b1.getMatchedDefinition().getDtColumnsDefinition())) {
                return false;
            }
        }
        return true;
    }

    private static final int FITS_MAX_LIMIT = 10000;
    private static final int MAX_NUMBER_OF_RETURNS = 3;

    private static void bruteForceHeaders(int column,
            int numberOfVConditionParameters,
            List<DTHeader> dtHeaders,
            boolean[][] matrix,
            Map<Integer, List<Integer>> columnToIndex,
            List<Integer> usedIndexes,
            Set<Integer> usedParameterIndexes,
            List<List<DTHeader>> fits,
            Set<Integer> failedToFit,
            int numberOfReturns,
            int fuzzyReturnsFlag) {
        if (fits.size() > FITS_MAX_LIMIT) {
            return;
        }
        List<Integer> indexes = columnToIndex.get(column);
        if (indexes == null || usedParameterIndexes.size() >= numberOfVConditionParameters) {
            List<DTHeader> fit = new ArrayList<>();
            for (Integer index : usedIndexes) {
                fit.add(dtHeaders.get(index));
            }
            fits.add(Collections.unmodifiableList(fit));
            if (indexes == null) {
                return;
            }
        }
        boolean last = true;
        for (Integer index : indexes) {
            boolean f = true;
            for (Integer usedIndex : usedIndexes) {
                if (!matrix[index][usedIndex]) {
                    f = false;
                    break;
                }
            }
            if (f) {
                DTHeader dtHeader = dtHeaders.get(index);
                boolean isFuzzyReturn = false;
                if (dtHeader instanceof FuzzyDTHeader) {
                    FuzzyDTHeader fuzzyDTHeader = (FuzzyDTHeader) dtHeader;
                    if (fuzzyDTHeader.isReturn()) {
                        isFuzzyReturn = true;
                    }
                }
                if (isFuzzyReturn && fuzzyReturnsFlag == 2) {
                    continue;
                }
                Set<Integer> usedParameterIndexesTo = new HashSet<>(usedParameterIndexes);
                for (int i : dtHeader.getMethodParameterIndexes()) {
                    usedParameterIndexesTo.add(i);
                }
                if (usedParameterIndexesTo.size() <= numberOfVConditionParameters) {
                    int numberOfReturns1 = dtHeader.isReturn() && !isFuzzyReturn ? numberOfReturns + 1
                                                                                 : numberOfReturns;
                    int fuzzyReturnsFlag1 = isFuzzyReturn && fuzzyReturnsFlag != 1 ? fuzzyReturnsFlag + 1
                                                                                   : fuzzyReturnsFlag;
                    if (numberOfReturns1 + (fuzzyReturnsFlag1 > 1 ? 1 : 0) <= MAX_NUMBER_OF_RETURNS) {
                        last = false;
                        usedIndexes.add(index);
                        bruteForceHeaders(column + dtHeader.getWidth(),
                            numberOfVConditionParameters,
                            dtHeaders,
                            matrix,
                            columnToIndex,
                            usedIndexes,
                            usedParameterIndexesTo,
                            fits,
                            failedToFit,
                            numberOfReturns1,
                            fuzzyReturnsFlag1);
                        usedIndexes.remove(usedIndexes.size() - 1);
                    }
                }

            }
        }
        if (indexes != null && !indexes.isEmpty() && last) {
            for (Integer index : indexes) {
                failedToFit.add(index);
            }
        }
    }

    private static List<List<DTHeader>> filterHeadersByMax(List<List<DTHeader>> fits,
            ToLongFunction<List<DTHeader>> function) {
        long max = Long.MIN_VALUE;
        List<List<DTHeader>> newFits = new ArrayList<>();
        for (List<DTHeader> fit : fits) {
            long current = function.applyAsLong(fit);
            if (current > max) {
                max = current;
                newFits.clear();
                newFits.add(fit);
            } else if (current == max) {
                newFits.add(fit);
            }
        }
        return newFits;
    }

    private static List<List<DTHeader>> filterHeadersByMin(List<List<DTHeader>> fits,
            ToLongFunction<List<DTHeader>> function) {
        long min = Long.MAX_VALUE;
        List<List<DTHeader>> newFits = new ArrayList<>();
        for (List<DTHeader> fit : fits) {
            long current = function.applyAsLong(fit);
            if (current < min) {
                min = current;
                newFits.clear();
                newFits.add(fit);
            } else if (current == min) {
                newFits.add(fit);
            }
        }
        return newFits;
    }

    private static List<List<DTHeader>> filterHeadersByMatchType(List<List<DTHeader>> fits) {
        MatchType[] matchTypes = MatchType.values();
        Arrays.sort(matchTypes, Comparator.comparingInt(MatchType::getPriority));
        for (MatchType type : matchTypes) {
            fits = filterHeadersByMax(fits,
                e -> e.stream()
                    .filter(x -> x instanceof DeclaredDTHeader)
                    .map(x -> (DeclaredDTHeader) x)
                    .filter(x -> type.equals(x.getMatchedDefinition().getMatchType()))
                    .mapToLong(x -> x.getMatchedDefinition().getDtColumnsDefinition().getNumberOfTitles())
                    .sum());
        }
        return fits;
    }

    private static boolean isLastDtColumnValid(DTHeader dtHeader, int maxColumn, int columnsForReturn) {
        if (dtHeader.isReturn()) {
            return dtHeader.getColumn() + dtHeader.getWidth() == maxColumn;
        }
        if (dtHeader.isCondition() || dtHeader.isAction()) {
            return dtHeader.getColumn() + dtHeader.getWidth() < maxColumn - columnsForReturn;
        }
        return true;
    }

    private static List<List<DTHeader>> filterWithWrongStructure(ILogicalTable originalTable,
            List<List<DTHeader>> fits,
            boolean twoColumnsInReturn) {
        int maxColumn = originalTable.getSource().getWidth();
        int w = 0;
        if (maxColumn > 0 && twoColumnsInReturn) {
            w = originalTable.getSource().getCell(maxColumn - 1, 0).getWidth();
            if (maxColumn - w > 0) {
                w = w + originalTable.getSource().getCell(maxColumn - 1 - w, 0).getWidth();
            }
        }
        final int w1 = w;

        return fits.stream()
            .filter(
                e -> e.isEmpty() || isLastDtColumnValid(e.get(e.size() - 1), maxColumn, twoColumnsInReturn ? w1 : 0))
            .collect(toList());
    }

    private static boolean methodsChainsIsCrossed(IOpenMethod[] m1, IOpenMethod[] m2) {
        if (m1 == null && m2 == null) {
            return true;
        }
        if (m1 != null && m2 != null) {
            int i = 0;
            while (i < m1.length && i < m2.length) {
                if (m1[i].equals(m2[i])) {
                    i++;
                } else {
                    break;
                }
            }
            if (i == m1.length || i == m2.length) {
                return true;
            }
        }
        return false;
    }

    private static boolean isAmbiguousFits(List<List<DTHeader>> fits, Predicate<DTHeader> predicate) {
        if (fits.size() <= 1) {
            return false;
        }
        DTHeader[] dtHeaders0 = fits.get(0).stream().filter(predicate::test).toArray(DTHeader[]::new);
        for (int i = 1; i < fits.size(); i++) {
            DTHeader[] dtHeaders1 = fits.get(i).stream().filter(predicate::test).toArray(DTHeader[]::new);
            if (!Arrays.equals(dtHeaders0, dtHeaders1)) {
                return true;
            }
        }
        return false;
    }

    private static boolean intersect(int b1, int e1, int b2, int e2) {
        return b2 <= b1 && b1 <= e2 || b2 <= e1 && e1 <= e2 || b1 <= b2 && b2 <= e1 || b1 <= e2 && e2 <= e1;
    }

    private static List<DTHeader> optimizeDtHeaders(List<DTHeader> dtHeaders) {
        // Remove headers that intersect with declared dt header if declared dt header is matched 100%
        boolean[] f = new boolean[dtHeaders.size()];
        Arrays.fill(f, false);
        for (int i = 0; i < dtHeaders.size() - 1; i++) {
            for (int j = i + 1; j < dtHeaders.size(); j++) {
                if (dtHeaders.get(i) instanceof DeclaredDTHeader && dtHeaders.get(j) instanceof DeclaredDTHeader) {
                    DeclaredDTHeader d1 = (DeclaredDTHeader) dtHeaders.get(i);
                    DeclaredDTHeader d2 = (DeclaredDTHeader) dtHeaders.get(j);
                    if (!(d1.getColumn() == d2.getColumn() && d1.getWidth() == d2.getWidth()) && intersect(
                        d1.getColumn(),
                        d1.getColumn() + d1.getWidth() - 1,
                        d2.getColumn(),
                        d2.getColumn() + d2.getWidth() - 1)) {
                        f[i] = true;
                        f[j] = true;
                    }
                }
            }
        }
        Set<Integer> indexes = new HashSet<>();
        for (int i = 0; i < dtHeaders.size(); i++) {
            if (dtHeaders.get(i) instanceof DeclaredDTHeader && !f[i]) {
                indexes.add(i);
            }
        }
        List<DTHeader> ret = new ArrayList<>(dtHeaders);
        Iterator<DTHeader> itr = ret.iterator();
        while (itr.hasNext()) {
            DTHeader dtHeader = itr.next();
            if (!(dtHeader instanceof DeclaredDTHeader)) {
                for (Integer index : indexes) {
                    DTHeader t = dtHeaders.get(index);
                    if (intersect(t.getColumn(),
                        t.getColumn() + t.getWidth() - 1,
                        dtHeader.getColumn(),
                        dtHeader.getColumn() + dtHeader.getWidth() - 1)) {
                        itr.remove();
                        break;
                    }
                }
            }
        }
        return ret;
    }

    private static List<DTHeader> fitDtHeaders(TableSyntaxNode tableSyntaxNode,
            ILogicalTable originalTable,
            List<DTHeader> dtHeaders,
            int numberOfParameters,
            int numberOfHcondition,
            boolean twoColumnsForReturn,
            int firstColumnHeight,
            IBindingContext bindingContext) throws OpenLCompilationException {
        dtHeaders = optimizeDtHeaders(dtHeaders);
        int numberOfParametersForVCondition = numberOfParameters - numberOfHcondition;
        boolean[][] matrix = new boolean[dtHeaders.size()][dtHeaders.size()];
        for (int i = 0; i < dtHeaders.size(); i++) {
            for (int j = 0; j < dtHeaders.size(); j++) {
                matrix[i][j] = true;
            }
        }
        Map<Integer, List<Integer>> columnToIndex = new HashMap<>();
        for (int i = 0; i < dtHeaders.size(); i++) {
            List<Integer> indexes = columnToIndex.computeIfAbsent(dtHeaders.get(i).getColumn(), e -> new ArrayList<>());
            indexes.add(i);
            for (int j = i; j < dtHeaders.size(); j++) {
                if (i == j || !isCompatibleHeaders(dtHeaders.get(i), dtHeaders.get(j))) {
                    matrix[i][j] = false;
                    matrix[j][i] = false;
                }
            }
        }
        List<List<DTHeader>> fits = new ArrayList<>();
        Set<Integer> failedToFit = new HashSet<>();
        bruteForceHeaders(0,
            numberOfParametersForVCondition,
            dtHeaders,
            matrix,
            columnToIndex,
            new ArrayList<>(),
            new HashSet<>(),
            fits,
            failedToFit,
            0,
            0);
        if (fits.size() > FITS_MAX_LIMIT) {
            bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(
                "Ambiguous matching of column titles to DT conditions. Too many options are found.",
                tableSyntaxNode));
        }

        fits = filterWithWrongStructure(originalTable, fits, twoColumnsForReturn);

        // Declared covered columns filter
        fits = filterHeadersByMax(fits,
            e -> e.stream()
                .filter(x -> x instanceof DeclaredDTHeader)
                .mapToLong(
                    x -> ((DeclaredDTHeader) x).getMatchedDefinition().getDtColumnsDefinition().getNumberOfTitles())
                .sum());
        fits = filterHeadersByMatchType(fits);
        if (numberOfHcondition != numberOfParameters) {
            fits = filterHeadersByMax(fits, e -> e.stream().anyMatch(DTHeader::isCondition) ? 1l : 0l); // Prefer
            // full
            // matches
            // with
            // first
            // condition
            // headers
        }

        if (numberOfHcondition == 0) {
            fits = fits.stream().filter(e -> e.stream().anyMatch(DTHeader::isReturn)).collect(toList());
            // Prefer full
            // matches
            // with
            // last
            // return
            // headers
        } else {
            fits = fits.stream().filter(e -> e.stream().filter(DTHeader::isReturn).count() == 0).collect(toList()); // Lookup
            // table
            // with
            // no
            // returns
            // columns
        }

        fits = filterHeadersByMax(fits,
            e -> e.stream().flatMapToInt(c -> Arrays.stream(c.getMethodParameterIndexes())).distinct().count());

        fits = filterHeadersByMin(fits, e -> e.stream().filter(x -> x instanceof SimpleReturnDTHeader).count());

        fits = filterHeadersByMax(fits,
            e -> e.stream()
                .filter(x -> x instanceof FuzzyDTHeader)
                .map(x -> (FuzzyDTHeader) x)
                .mapToInt(x -> x.getFuzzyResult().getMax())
                .sum());

        fits = filterHeadersByMin(fits,
            e -> e.stream()
                .filter(x -> x instanceof FuzzyDTHeader)
                .map(x -> (FuzzyDTHeader) x)
                .mapToInt(x -> x.getFuzzyResult().getMin())
                .sum());

        fits = filterHeadersByMin(fits,
            e -> e.stream()
                .filter(x -> x instanceof FuzzyDTHeader)
                .map(x -> (FuzzyDTHeader) x)
                .mapToInt(x -> x.getFuzzyResult().getToken().getDistance())
                .sum());

        if (numberOfHcondition == 0 && fits.isEmpty()) {
            final List<DTHeader> dths = dtHeaders;
            OptionalInt c = failedToFit.stream().mapToInt(e -> dths.get(e).getColumn()).max();
            StringBuilder message = new StringBuilder();
            message.append("Failed to compile decision table.");
            if (c.isPresent()) {
                int c0 = c.getAsInt();
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < firstColumnHeight; i++) {
                    if (i > 0) {
                        sb.append(StringUtils.SPACE);
                        sb.append("/");
                        sb.append(StringUtils.SPACE);
                    }
                    sb.append(originalTable.getCell(c0, i).getStringValue());
                }
                message.append(StringUtils.SPACE);
                message.append("There is no match for column '" + sb.toString() + "'.");
            }
            throw new OpenLCompilationException(message.toString());
        }

        if (!fits.isEmpty()) {
            if (fits.size() > 1) {
                if (isAmbiguousFits(fits, DTHeader::isCondition)) {
                    bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(
                        "Ambiguous matching of column titles to DT conditions. Use more appropriate titles for condition columns.",
                        tableSyntaxNode));
                }
                if (isAmbiguousFits(fits, DTHeader::isAction)) {
                    bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(
                        "Ambiguous matching of column titles to DT action columns. Use more appropriate titles for action columns.",
                        tableSyntaxNode));
                }
                if (isAmbiguousFits(fits, DTHeader::isReturn)) {
                    bindingContext.addMessage(OpenLMessagesUtils.newWarnMessage(
                        "Ambiguous matching of column titles to DT return columns. Use more appropriate titles for return columns.",
                        tableSyntaxNode));
                }
            }

            fits = filterHeadersByMin(fits, e -> e.stream().filter(DTHeader::isReturn).count()); // Select with min
            // returns
            fits = filterHeadersByMin(fits, e -> e.stream().filter(DTHeader::isAction).count()); // Select with min
            // returns
            fits = filterHeadersByMin(fits, e -> e.stream().filter(DTHeader::isCondition).count()); // Select with max
            // conditions

            return fits.get(0);
        }

        return Collections.emptyList();
    }

    private static int getFirstColumnForHCondition(ILogicalTable originalTable,
            int numberOfHcondition,
            int firstColumnHeight) {
        int w = originalTable.getSource().getWidth();
        int column = 0;
        int ret = -1;
        while (column < w) {
            int rowsCount = calculateRowsCount(originalTable, column, firstColumnHeight);
            if (rowsCount != numberOfHcondition) {
                ret = -1;
            }
            if (rowsCount > 1 && rowsCount == numberOfHcondition && ret < 0) {
                ret = column;
            }
            column = column + originalTable.getSource().getCell(column, 0).getWidth();
        }
        return ret;
    }

    private static boolean columnWithFormulas(ILogicalTable originalTable, int firstColumnHeight, int column) {
        int h = firstColumnHeight;
        int height = originalTable.getSource().getHeight();
        int c = 0;
        int t = 0;
        while (h < height) {
            ICell cell = originalTable.getSource().getCell(column, h);
            String s = cell.getStringValue();
            if (!StringUtils.isEmpty(s != null ? s.trim() : s) && !RuleRowHelper.isFormula(s)) {
                c++;
            }
            t++;
            h = h + cell.getHeight();
        }
        return c <= t / 2 + t % 2;
    }

    private static List<DTHeader> getDTHeaders(TableSyntaxNode tableSyntaxNode,
            DecisionTable decisionTable,
            ILogicalTable originalTable,
            FuzzyContext fuzzyContext,
            NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter,
            int numberOfHcondition,
            int firstColumnHeight,
            IBindingContext bindingContext) throws OpenLCompilationException {
        boolean isSmart = isSmart(tableSyntaxNode);

        int numberOfParameters = decisionTable.getSignature().getNumberOfParameters();
        boolean twoColumnsForReturn = isTwoColumnsForReturn(tableSyntaxNode, decisionTable);

        final XlsDefinitions xlsDefinitions = ((XlsModuleOpenClass) decisionTable.getDeclaringClass())
            .getXlsDefinitions();

        int lastColumn = originalTable.getSource().getWidth();
        if (numberOfHcondition != 0) {
            int firstColumnForHCondition = getFirstColumnForHCondition(originalTable,
                numberOfHcondition,
                firstColumnHeight);
            if (firstColumnForHCondition > 0) {
                lastColumn = firstColumnForHCondition;
            }
        }

        List<DTHeader> dtHeaders = new ArrayList<>();
        int i = 0;
        int column = 0;
        while (column < lastColumn) {
            int w = originalTable.getSource().getCell(column, 0).getWidth();
            if (isSmart) {
                matchWithDtColumnsDefinitions(decisionTable,
                    originalTable,
                    column,
                    xlsDefinitions,
                    numberOfColumnsUnderTitleCounter,
                    dtHeaders,
                    firstColumnHeight,
                    bindingContext);
                List<DTHeader> fuzzyHeaders = matchWithFuzzySearch(decisionTable,
                    originalTable,
                    fuzzyContext,
                    column,
                    numberOfHcondition,
                    dtHeaders,
                    firstColumnHeight,
                    bindingContext,
                    false);
                if (numberOfHcondition == 0) {
                    boolean f = false;
                    if (fuzzyContext.isFuzzySupportsForReturnType()) {
                        if (fuzzyHeaders.stream().noneMatch(DTHeader::isReturn) && numberOfColumnsUnderTitleCounter
                            .get(column) == 1 && columnWithFormulas(originalTable, firstColumnHeight, column)) {
                            f = true;
                        }
                    } else {
                        f = true;
                    }
                    if (f) {
                        String title = originalTable.getSource().getCell(column, 0).getStringValue();
                        int width = originalTable.getSource().getCell(column, 0).getWidth();
                        dtHeaders.add(new SimpleReturnDTHeader(null, title, column, width));
                    }
                }
            } else {
                if (numberOfHcondition == 0 && i >= numberOfParameters) {
                    matchWithFuzzySearch(decisionTable,
                        originalTable,
                        fuzzyContext,
                        column,
                        numberOfHcondition,
                        dtHeaders,
                        firstColumnHeight,
                        bindingContext,
                        true);
                }
                if (i < numberOfParameters - numberOfHcondition) {
                    SimpleDTHeader simpleDTHeader = new SimpleDTHeader(i,
                        decisionTable.getSignature().getParameterName(i),
                        null,
                        column,
                        w);
                    dtHeaders.add(simpleDTHeader);
                } else if (numberOfHcondition == 0) {
                    SimpleReturnDTHeader simpleReturnDTHeader = new SimpleReturnDTHeader(null, null, column, w);
                    dtHeaders.add(simpleReturnDTHeader);
                }
            }

            column = column + w;
            i++;
        }

        List<DTHeader> fit = fitDtHeaders(tableSyntaxNode,
            originalTable,
            dtHeaders,
            decisionTable.getSignature().getNumberOfParameters(),
            numberOfHcondition,
            twoColumnsForReturn,
            firstColumnHeight,
            bindingContext);

        if (numberOfHcondition > 0) {
            boolean[] parameterIsUsed = new boolean[numberOfParameters];
            Arrays.fill(parameterIsUsed, false);
            for (DTHeader dtHeader : fit) {
                for (int paramIndex : dtHeader.getMethodParameterIndexes()) {
                    parameterIsUsed[paramIndex] = true;
                }
            }

            int k = 0;
            i = numberOfParameters - 1;
            while (k < numberOfHcondition && i >= 0) {
                if (!parameterIsUsed[i]) {
                    k++;
                }
                i--;
            }

            if (k < numberOfHcondition) {
                throw new OpenLCompilationException("No input parameter found for horizontal condition!");
            }

            column = fit.stream()
                .filter(e -> e.isCondition() || e.isAction())
                .mapToInt(e -> e.getColumn() + e.getWidth())
                .max()
                .orElse(0);

            List<DTHeader> fit1 = new ArrayList<>(fit);
            int j = 0;
            for (int w = i + 1; w < numberOfParameters; w++) {
                if (!parameterIsUsed[w] && j < numberOfHcondition) {
                    fit1.add(new SimpleDTHeader(w, decisionTable.getSignature().getParameterName(w), column + j, j));
                    j++;
                }
            }
            return Collections.unmodifiableList(fit1);
        } else {
            return fit;
        }

    }

    private static int getNumberOfHConditions(ILogicalTable tableBody) {
        int w = tableBody.getSource().getWidth();
        int d = tableBody.getSource().getCell(0, 0).getHeight();
        int k = 0;
        int i = 0;
        while (i < d) {
            i = i + tableBody.getSource().getCell(w - 1, i).getHeight();
            k++;
        }
        return k;
    }

    private static boolean isTwoColumnsForReturn(TableSyntaxNode tableSyntaxNode, DecisionTable decisionTable) {
        boolean twoColumnsForReturn = false;
        if (isCollect(tableSyntaxNode) && Map.class.isAssignableFrom(decisionTable.getType().getInstanceClass())) {
            twoColumnsForReturn = true;
        }
        return twoColumnsForReturn;
    }

    private static void matchWithDtColumnsDefinitions(DecisionTable decisionTable,
            ILogicalTable originalTable,
            int column,
            XlsDefinitions definitions,
            NumberOfColumnsUnderTitleCounter numberOfColumnsUnderTitleCounter,
            List<DTHeader> dtHeaders,
            int firstColumnHeight,
            IBindingContext bindingContext) {
        if (firstColumnHeight != originalTable.getSource().getCell(column, 0).getHeight()) {
            return;
        }
        for (DTColumnsDefinition definition : definitions.getDtColumnsDefinitions()) {
            Set<String> titles = new HashSet<>(definition.getTitles());
            String title = originalTable.getSource().getCell(column, 0).getStringValue();
            title = OpenLFuzzyUtils.toTokenString(title);
            int numberOfColumnsUnderTitle = numberOfColumnsUnderTitleCounter.get(column);
            int i = 0;
            int x = column;
            IParameterDeclaration[][] columnParameters = new IParameterDeclaration[definition.getNumberOfTitles()][];
            while (titles.contains(title) && numberOfColumnsUnderTitle == definition.getLocalParameters(title)
                .size() && x < originalTable.getSource().getWidth()) {
                titles.remove(title);
                for (String s : definition.getTitles()) {
                    if (s.equals(title)) {
                        columnParameters[i] = definition.getLocalParameters(title)
                            .toArray(new IParameterDeclaration[] {});
                        break;
                    }
                }
                i = i + 1;
                x = x + originalTable.getSource().getCell(x, 0).getWidth();
                title = originalTable.getSource().getCell(x, 0).getStringValue();
                title = OpenLFuzzyUtils.toTokenString(title);
                numberOfColumnsUnderTitle = numberOfColumnsUnderTitleCounter.get(x);
            }
            if (titles.isEmpty()) {
                MatchedDefinition matchedDefinition = matchByDTColumnDefinition(decisionTable,
                    definition,
                    bindingContext);
                if (matchedDefinition != null) {
                    DeclaredDTHeader dtHeader = new DeclaredDTHeader(matchedDefinition.getUsedMethodParameterIndexes(),
                        definition.getCompositeMethod(),
                        columnParameters,
                        column,
                        x - column,
                        matchedDefinition);
                    dtHeaders.add(dtHeader);
                }
            }
        }
    }

    private static Pair<Boolean, String[]> parsableAsArray(String src, Class<?> clazz, IBindingContext bindingContext) {
        String[] values = StringTool.splitAndEscape(src,
            RuleRowHelper.ARRAY_ELEMENTS_SEPARATOR,
            RuleRowHelper.ARRAY_ELEMENTS_SEPARATOR_ESCAPER);
        try {
            for (String value : values) {
                String2DataConvertorFactory.parse(clazz, value, bindingContext);
            }
        } catch (Exception e) {
            return Pair.of(false, values);
        }
        return Pair.of(true, values);
    }

    private static int calculateRowsCount(ILogicalTable originalTable, int column, int height) {
        int h = 0;
        int k = 0;
        while (h < height && h < originalTable.getSource().getHeight()) {
            h = h + originalTable.getSource().getCell(column, h).getHeight();
            k++;
        }
        return k;
    }

    private static int getLastRowHeader(ILogicalTable originalTable, int column, int height) {
        int h = 0;
        int hLast = 0;
        while (h < height && h < originalTable.getSource().getHeight()) {
            hLast = h;
            h = h + originalTable.getSource().getCell(column, h).getHeight();
        }
        return hLast;
    }

    /**
     * Check type of condition values. If condition values are complex(Range, Array) then types of complex values will
     * be returned
     *
     * @param originalTable The original body of simple Decision Table.
     * @param column The number of a condition
     * @param type The type of an input parameter
     * @param isThatVCondition If condition is vertical value = true
     * @param vColumnCounter Counter of vertical conditions. Needed for calculating position of horizontal condition
     * @return type of condition values
     */
    private static Pair<String, IOpenClass> getTypeForConditionColumn(DecisionTable decisionTable,
            ILogicalTable originalTable,
            DTHeader condition,
            int numOfHCondition,
            int firstColumnForHConditions,
            IBindingContext bindingContext) {
        int column = condition.getColumn();

        IOpenClass type = getTypeForCondition(decisionTable, condition);

        ILogicalTable decisionValues;
        int width;
        int skip;
        if (isVCondition(condition)) {
            decisionValues = LogicalTableHelper.logicalTable(originalTable.getSource().getColumn(column));
            width = decisionValues.getHeight();
            int firstColumnHeight = originalTable.getSource().getCell(0, 0).getHeight();
            skip = calculateRowsCount(originalTable, column, firstColumnHeight);
        } else {
            decisionValues = LogicalTableHelper.logicalTable(originalTable.getSource().getRow(numOfHCondition - 1));
            width = decisionValues.getWidth();
            skip = firstColumnForHConditions;
        }

        boolean allRangePattern = true;
        boolean allCanBeNotRangePattern = true;
        boolean zeroStartedNumbersFound = false;

        for (int valueNum = skip; valueNum < width; valueNum++) {
            ILogicalTable cellValue;
            if (isVCondition(condition)) {
                cellValue = decisionValues.getRow(valueNum);
            } else {
                cellValue = decisionValues.getColumn(valueNum);
            }

            String value = cellValue.getSource().getCell(0, 0).getStringValue();

            if (value == null) {
                continue;
            }

            ConstantOpenField constantOpenField = RuleRowHelper.findConstantField(bindingContext, value);
            if (constantOpenField != null && RANGES_TYPES.contains(constantOpenField.getType().getInstanceClass())) {
                return Pair.of(constantOpenField.getType().getInstanceClass().getSimpleName(),
                    constantOpenField.getType());
            }

            /* try to create range by values **/
            try {
                if (INT_TYPES.contains(type.getInstanceClass())) {
                    boolean f = IntRangeParser.getInstance().parse(value) != null;
                    if (!f) {
                        allRangePattern = false;
                    }
                    Pair<Boolean, String[]> g = parsableAsArray(value, type.getInstanceClass(), bindingContext);
                    if (g.getKey() && !zeroStartedNumbersFound) { // If array element starts with 0 and can be range and
                        // array for all elements then use Range by default. But if
                        // no zero started elements then default String[]
                        zeroStartedNumbersFound = Arrays.stream(g.getRight())
                            .anyMatch(e -> e != null && e.length() > 1 && e.startsWith("0"));
                    }
                    if (f && !g.getKey()) {
                        return Pair.of(IntRange.class.getSimpleName(), JavaOpenClass.getOpenClass(IntRange.class));
                    }
                } else if (DOUBLE_TYPES.contains(type.getInstanceClass())) {
                    boolean f = DoubleRangeParser.getInstance().parse(value) != null;
                    if (!f) {
                        allRangePattern = false;
                    }
                    Pair<Boolean, String[]> g = parsableAsArray(value, type.getInstanceClass(), bindingContext);
                    if (g.getKey() && !zeroStartedNumbersFound) {
                        zeroStartedNumbersFound = Arrays.stream(g.getRight())
                            .anyMatch(e -> e != null && e.length() > 1 && e.startsWith("0"));
                    }
                    if (f && !g.getKey()) {
                        return Pair.of(DoubleRange.class.getSimpleName(),
                            JavaOpenClass.getOpenClass(DoubleRange.class));
                    }
                } else if (CHAR_TYPES.contains(type.getInstanceClass())) {
                    if (!parsableAsArray(value, type.getInstanceClass(), bindingContext).getKey()) {
                        return Pair.of(CharRange.class.getSimpleName(), JavaOpenClass.getOpenClass(CharRange.class));
                    }
                } else if (DATE_TYPES.contains(type.getInstanceClass())) {
                    Object o = cellValue.getSource().getCell(0, 0).getObjectValue();
                    if (!DateRangeParser.getInstance().isDateRange(value) && !(o instanceof Date)) {
                        allRangePattern = false;
                        break;
                    }
                    if (!DateRangeParser.getInstance().canBeNotDateRange(value) && !(o instanceof Date)) {
                        allCanBeNotRangePattern = false;
                    }
                } else if (STRINGS_TYPES.contains(type.getInstanceClass())) {
                    if (!StringRangeParser.getInstance().isStringRange(value)) {
                        allRangePattern = false;
                        break;
                    }
                    if (!StringRangeParser.getInstance().canBeNotStringRange(value)) {
                        allCanBeNotRangePattern = false;
                    }
                }
            } catch (Exception e) {
            }
        }

        if (INT_TYPES.contains(type.getInstanceClass()) && allRangePattern && zeroStartedNumbersFound) {
            return Pair.of(IntRange.class.getSimpleName(), JavaOpenClass.getOpenClass(IntRange.class));
        }

        if (DOUBLE_TYPES.contains(type.getInstanceClass()) && allRangePattern && zeroStartedNumbersFound) {
            return Pair.of(DoubleRange.class.getSimpleName(), JavaOpenClass.getOpenClass(DoubleRange.class));
        }

        if (DATE_TYPES.contains(type.getInstanceClass()) && allRangePattern && !allCanBeNotRangePattern) {
            return Pair.of(DateRange.class.getSimpleName(), JavaOpenClass.getOpenClass(DateRange.class));
        }

        if (STRINGS_TYPES.contains(type.getInstanceClass()) && allRangePattern && !allCanBeNotRangePattern) {
            return Pair.of(StringRange.class.getSimpleName(), JavaOpenClass.getOpenClass(StringRange.class));
        }

        if (!type.isArray()) {
            return Pair.of(type.getName() + "[]", AOpenClass.getArrayType(type, 1));
        } else {
            return Pair.of(type.getName(), type);
        }
    }

    private static IOpenClass getTypeForCondition(DecisionTable decisionTable, DTHeader condition) {
        IOpenClass type = decisionTable.getSignature().getParameterTypes()[condition.getMethodParameterIndex()];
        if (condition instanceof FuzzyDTHeader) {
            FuzzyDTHeader fuzzyCondition = (FuzzyDTHeader) condition;
            if (fuzzyCondition.getMethodsChain() != null) {
                type = fuzzyCondition.getMethodsChain()[fuzzyCondition.getMethodsChain().length - 1].getType();
            }
        }
        return type;
    }

    public static XlsSheetGridModel createVirtualGrid(String poiSheetName, int numberOfColumns) {
        // Pre-2007 excel sheets had a limitation of 256 columns.
        Workbook workbook = (numberOfColumns > 256) ? new XSSFWorkbook() : new HSSFWorkbook();
        final Sheet sheet = workbook.createSheet(poiSheetName);
        return createVirtualGrid(sheet);
    }

    static boolean isCollect(TableSyntaxNode tableSyntaxNode) {
        return tableSyntaxNode.getHeader().isCollect();
    }

    static boolean isSmart(TableSyntaxNode tableSyntaxNode) {
        return isSmartDecisionTable(tableSyntaxNode) || isSmartLookupTable(tableSyntaxNode);
    }

    static boolean isLookup(TableSyntaxNode tableSyntaxNode) {
        return isSimpleLookupTable(tableSyntaxNode) || isSmartLookupTable(tableSyntaxNode);
    }

    static boolean isSmartDecisionTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();

        return IXlsTableNames.SMART_DECISION_TABLE.equals(dtType);
    }

    static boolean isSimpleDecisionTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();

        return IXlsTableNames.SIMPLE_DECISION_TABLE.equals(dtType);
    }

    static boolean isSmartLookupTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();

        return IXlsTableNames.SMART_DECISION_LOOKUP.equals(dtType);
    }

    static boolean isSimpleLookupTable(TableSyntaxNode tableSyntaxNode) {
        String dtType = tableSyntaxNode.getHeader().getHeaderToken().getIdentifier();
        return IXlsTableNames.SIMPLE_DECISION_LOOKUP.equals(dtType) || isSmartLookupTable(tableSyntaxNode);
    }

    static int countHConditionsByHeaders(ILogicalTable table) {
        int width = table.getWidth();
        int cnt = 0;

        for (int i = 0; i < width; i++) {
            String value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();
            if (value != null) {
                value = value.toUpperCase();
                if (isValidHConditionHeader(value)) {
                    ++cnt;
                }
            }
        }
        return cnt;
    }

    static int countVConditionsByHeaders(ILogicalTable table) {
        int width = table.getWidth();
        int cnt = 0;
        for (int i = 0; i < width; i++) {
            String value = table.getColumn(i).getSource().getCell(0, 0).getStringValue();
            if (value != null) {
                value = value.toUpperCase();
                if (isValidConditionHeader(value) || isValidMergedConditionHeader(value)) {
                    ++cnt;
                }
            }
        }
        return cnt;
    }

    /**
     * Creates virtual {@link XlsSheetGridModel} with poi source sheet.
     */
    public static XlsSheetGridModel createVirtualGrid() {
        Sheet sheet = new HSSFWorkbook().createSheet();
        return createVirtualGrid(sheet);
    }

    /**
     * Creates virtual {@link XlsSheetGridModel} from poi source sheet.
     *
     * @param sheet poi sheet source
     * @return virtual grid that wraps sheet
     */
    private static XlsSheetGridModel createVirtualGrid(Sheet sheet) {
        final StringSourceCodeModule sourceCodeModule = new StringSourceCodeModule("", null);
        final SimpleWorkbookLoader workbookLoader = new SimpleWorkbookLoader(sheet.getWorkbook());
        XlsWorkbookSourceCodeModule mockWorkbookSource = new XlsWorkbookSourceCodeModule(sourceCodeModule,
            workbookLoader);
        XlsSheetSourceCodeModule mockSheetSource = new XlsSheetSourceCodeModule(new SimpleSheetLoader(sheet),
            mockWorkbookSource);

        return new XlsSheetGridModel(mockSheetSource);
    }

    private static final class ParameterTokens {
        Token[] tokens;
        Map<Token, Integer> tokensToParameterIndex;
        Map<Token, IOpenMethod[]> tokenToMethodsChain;

        public ParameterTokens(Token[] tokens,
                Map<Token, Integer> tokensToParameterIndex,
                Map<Token, IOpenMethod[]> tokenToMethodsChain) {
            this.tokens = tokens;
            this.tokensToParameterIndex = tokensToParameterIndex;
            this.tokenToMethodsChain = tokenToMethodsChain;
        }

        public IOpenMethod[] getMethodsChain(Token value) {
            return tokenToMethodsChain.get(value);
        }

        public int getParameterIndex(Token value) {
            return tokensToParameterIndex.get(value);
        }

        public Token[] getTokens() {
            return tokens;
        }
    }

    private static class NumberOfColumnsUnderTitleCounter {
        ILogicalTable logicalTable;
        int firstColumnHeight;
        Map<Integer, List<Integer>> numberOfColumnsMap = new HashMap<>();

        private List<Integer> init(int column) {
            int w = logicalTable.getSource().getCell(column, 0).getWidth();
            int i = 0;
            List<Integer> w1 = new ArrayList<>();
            while (i < w) {
                int w0 = logicalTable.getSource().getCell(column + i, firstColumnHeight).getWidth();
                i = i + w0;
                w1.add(w0);
            }
            return w1;
        }

        private int get(int column) {
            List<Integer> numberOfColumns = numberOfColumnsMap.computeIfAbsent(column, e -> init(column));
            return numberOfColumns.size();
        }

        private int getWidth(int column, int num) {
            List<Integer> numberOfColumns = numberOfColumnsMap.computeIfAbsent(column, e -> init(column));
            return numberOfColumns.get(num);
        }

        private NumberOfColumnsUnderTitleCounter(ILogicalTable logicalTable, int firstColumnHeight) {
            this.logicalTable = logicalTable;
            this.firstColumnHeight = firstColumnHeight;
        }
    }

    private static class FuzzyContext {
        ParameterTokens parameterTokens;
        Token[] returnTokens = null;
        Map<Token, IOpenMethod[][]> returnTypeFuzzyTokens = null;
        IOpenClass returnType;

        private FuzzyContext(ParameterTokens parameterTokens) {
            this.parameterTokens = parameterTokens;
        }

        private FuzzyContext(ParameterTokens parameterTokens,
                Token[] returnTokens,
                Map<Token, IOpenMethod[][]> returnTypeFuzzyTokens,
                IOpenClass returnType) {
            this(parameterTokens);
            this.returnTokens = returnTokens;
            this.returnTypeFuzzyTokens = returnTypeFuzzyTokens;
            this.returnType = returnType;
        }

        public ParameterTokens getParameterTokens() {
            return parameterTokens;
        }

        public Token[] getReturnTokens() {
            return returnTokens;
        }

        public IOpenMethod[][] getMethodChainsForReturnToken(Token token) {
            return returnTypeFuzzyTokens.get(token);
        }

        public boolean isFuzzySupportsForReturnType() {
            return returnTypeFuzzyTokens != null && returnTokens != null && returnType != null;
        }

        public IOpenClass getReturnType() {
            return returnType;
        }
    }
}
