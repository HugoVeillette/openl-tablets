/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.MethodUtil;
import org.openl.binding.exception.DuplicatedFieldException;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.binding.exception.DuplicatedVarException;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.dependency.CompiledDependency;
import org.openl.engine.ExtendableModuleOpenClass;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.data.DataOpenField;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.binding.wrapper.IOpenMethodWrapper;
import org.openl.rules.lang.xls.binding.wrapper.WrapperLogic;
import org.openl.rules.lang.xls.prebind.ILazyMember;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.property.PropertiesOpenField;
import org.openl.rules.source.impl.VirtualSourceCodeModule;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.testmethod.TestMethodNodeBinder;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.IUriMember;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.types.UriMemberHelper;
import org.openl.rules.types.ValidationMessages;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.rules.types.impl.OverloadedMethodsDispatcherTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.ISyntaxNode;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IModuleInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.AMethod;
import org.openl.util.Log;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author snshor
 *
 */
public class XlsModuleOpenClass extends ModuleOpenClass implements ExtendableModuleOpenClass {
    private Logger log = LoggerFactory.getLogger(XlsModuleOpenClass.class);

    protected Set<String> duplicatedErrosUris = new HashSet<String>();
    private IDataBase dataBase = null;

    /**
     * Whether DecisionTable should be used as a dispatcher for overloaded tables. By default(this flag equals false)
     * dispatching logic will be performed in Java code.
     */
    private boolean useDescisionTableDispatcher;

    private boolean dispatchingValidationEnabled;

    private Collection<String> imports = new HashSet<String>();

    private ClassLoader classLoader;

    private RulesModuleBindingContext rulesModuleBindingContext;

    public RulesModuleBindingContext getRulesModuleBindingContext() {
        return rulesModuleBindingContext;
    }

    public void setRulesModuleBindingContext(RulesModuleBindingContext rulesModuleBindingContext) {
        this.rulesModuleBindingContext = rulesModuleBindingContext;
    }

    /**
     * Constructor for module with dependent modules
     *
     */
    public XlsModuleOpenClass(String name,
            XlsMetaInfo metaInfo,
            OpenL openl,
            IDataBase dbase,
            Set<CompiledDependency> usingModules,
            ClassLoader classLoader,
            boolean useDescisionTableDispatcher,
            boolean dispatchingValidationEnabled) {
        super(name, openl);
        this.dataBase = dbase;
        this.metaInfo = metaInfo;
        this.useDescisionTableDispatcher = useDescisionTableDispatcher;
        this.dispatchingValidationEnabled = dispatchingValidationEnabled;
        this.classLoader = classLoader;
        if (usingModules != null) {
            setDependencies(usingModules);
            initDependencies();
        }
        initImports(metaInfo.getXlsModuleNode());
    }

    public boolean isUseDescisionTableDispatcher() {
        return useDescisionTableDispatcher;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    private void initImports(XlsModuleSyntaxNode xlsModuleSyntaxNode) {
        imports.addAll(xlsModuleSyntaxNode.getImports());
    }

    // TODO: should be placed to ModuleOpenClass
    public IDataBase getDataBase() {
        return dataBase;
    }

    /**
     * Populate current module fields with data from dependent modules.
     */
    protected void initDependencies() {// Reduce iterators over dependencies for
                                       // compilation issue with lazy loading
        for (CompiledDependency dependency : this.getDependencies()) {
            // commented as there is no need to add each datatype to upper
            // module.
            // as now it`s will be impossible to validate from which module the
            // datatype is.
            //
            // addTypes(dependency);
            addDependencyTypes(dependency);
            addMethods(dependency);
            // Populate current module fields with data from dependent modules.
            // Requered
            // for data tables inheriting from dependend modules.
            addDataTables(dependency.getCompiledOpenClass()); // Required for
                                                              // data tables.
            addFields(dependency);
        }
    }

    public Collection<String> getImports() {
        return imports;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected boolean isDependencyMethodIgnorable(IOpenMethod method) {
        if (method instanceof ILazyMember) {
            return isDependencyMethodIgnorable(((ILazyMember<IOpenMethod>) method).getMember());
        }
        if (method instanceof TestSuiteMethod) {
            return true;
        }
        return false;
    }

    @SuppressWarnings("unchecked")
    @Override
    protected boolean isDependencyFieldIgnorable(IOpenField openField) {
        if (openField instanceof ILazyMember) {
            return isDependencyFieldIgnorable(((ILazyMember<IOpenField>) openField).getMember());
        }
        if (openField instanceof ConstantOpenField) {
            return false;
        }
        return super.isDependencyFieldIgnorable(openField);
    }

    @Override
    public void applyToDependentParsedCode(IParsedCode parsedCode) {
        if (parsedCode == null) {
            throw new IllegalArgumentException("parsedCode argument can't be null!");
        }
        if (parsedCode.getTopNode() instanceof XlsModuleSyntaxNode) {
            XlsModuleSyntaxNode xlsModuleSyntaxNode = (XlsModuleSyntaxNode) parsedCode.getTopNode();
            for (String value : getImports()) {
                xlsModuleSyntaxNode.addImport(value);
            }
        }
    }

    private void addDataTables(CompiledOpenClass dependency) {
        IOpenClass openClass = dependency.getOpenClassWithErrors();

        Map<String, IOpenField> fieldsMap = openClass.getFields();

        Set<String> existingDataTablesUris = new HashSet<String>();
        Map<String, IOpenField> fields = getFields();
        for (IOpenField openField : fields.values()) {
            if (openField instanceof DataOpenField) {
                existingDataTablesUris.add(((DataOpenField) openField).getUri());
            }
        }
        for (String key : fieldsMap.keySet()) {
            IOpenField field = fieldsMap.get(key);
            if (field instanceof DataOpenField) {
                DataOpenField dataOpenField = (DataOpenField) field;
                try {
                    String uri = dataOpenField.getUri();
                    // Test tables are added both as methods and variables.
                    if (!existingDataTablesUris.contains(uri) && !duplicatedErrosUris.contains(uri)) {
                        boolean containsInDependency = false;
                        if (VirtualSourceCodeModule.SOURCE_URI.equals(metaInfo.getSourceUrl())) {
                            for (CompiledDependency d : getDependencies()) {
                                IOpenClass dependentModuleClass = d.getCompiledOpenClass().getOpenClassWithErrors();
                                if (dependentModuleClass instanceof XlsModuleOpenClass) {
                                    if (((XlsModuleOpenClass) dependentModuleClass).duplicatedErrosUris.contains(uri)) {
                                        containsInDependency = true;
                                        break;
                                    }
                                }
                            }
                        }
                        if (!containsInDependency) {
                            addField(field);
                            existingDataTablesUris.add(uri);
                        }
                    }
                } catch (OpenlNotCheckedException e) {
                    ITable table = dataOpenField.getTable();
                    SyntaxNodeException error = SyntaxNodeExceptionUtils
                        .createError(e.getMessage(), e, table == null ? null : table.getTableSyntaxNode());
                    addError(error);
                }
            }
        }

        if (openClass instanceof XlsModuleOpenClass) {
            XlsModuleOpenClass xlsModuleOpenClass = (XlsModuleOpenClass) openClass;
            if (xlsModuleOpenClass.getDataBase() != null) {
                for (ITable table : xlsModuleOpenClass.getDataBase().getTables()) {
                    if (XlsNodeTypes.XLS_DATA.toString().equals(table.getTableSyntaxNode().getType())) {
                        try {
                            getDataBase().registerTable(table);
                        } catch (DuplicatedTableException e) {
                            addError(e);
                        } catch (OpenlNotCheckedException e) {
                            addError(e);
                        }
                    }
                }
            }
        }
    }

    public XlsMetaInfo getXlsMetaInfo() {
        return (XlsMetaInfo) metaInfo;
    }

    protected IOpenMethod undecorateForMultimoduleDispatching(final IOpenMethod openMethod) { // Dispatching
        // fix
        // for
        // mul1ti-module
        if (openMethod instanceof IOpenMethodWrapper) {
            IOpenMethodWrapper dispatchWrapper = (IOpenMethodWrapper) openMethod;
            return dispatchWrapper.getDelegate();
        }
        return openMethod;
    }

    protected IOpenMethod decorateForMultimoduleDispatching(final IOpenMethod openMethod) { // Dispatching
                                                                                            // fix
                                                                                            // for
                                                                                            // mul1ti-module
        return WrapperLogic.wrapOpenMethod(openMethod, this);
    }

    public void addField(IOpenField field) {
        Map<String, IOpenField> fields = fieldMap();
        if (fields.containsKey(field.getName())) {
            IOpenField existedField = fields.get(field.getName());
            if (field instanceof ConstantOpenField && existedField instanceof ConstantOpenField) { // Ignore constants
                                                                                                   // with the same
                                                                                                   // values
                if (field.getType().equals(existedField.getType()) && Objects.equals(((ConstantOpenField) field).getValue(),
                    ((ConstantOpenField) existedField).getValue())) {
                    return;
                }
                
                throw new DuplicatedFieldException("", field.getName());
            }

            if (field instanceof IUriMember && existedField instanceof IUriMember) {
                if (!UriMemberHelper.isTheSame((IUriMember) field, (IUriMember) existedField)) {
                    throw new DuplicatedFieldException("", field.getName());
                }
            } else {
                if (existedField != field) {
                    throw new DuplicatedFieldException("", field.getName());
                } else {
                    return;
                }
            }
        }
        fieldMap().put(field.getName(), field);
        addFieldToLowerCaseMap(field);
    }

    /**
     * Adds method to <code>XlsModuleOpenClass</code>.
     *
     * @param method method object
     */
    @Override
    public void addMethod(IOpenMethod method) {
        if (method instanceof OpenMethodDispatcher) {
            addDispatcherMethod((OpenMethodDispatcher) method);
            return;
        }
        IOpenMethod m = decorateForMultimoduleDispatching(method);

        // Workaround needed to set the module name in the method while compile
        if (m instanceof AMethod) {
            if (((AMethod) m).getModuleName() == null) {
                XlsMetaInfo metaInfo = getXlsMetaInfo();
                if (metaInfo != null) {
                    IOpenSourceCodeModule sourceCodeModule = metaInfo.getXlsModuleNode().getModule();
                    if (sourceCodeModule instanceof IModuleInfo) {
                        ((AMethod) m).setModuleName(((IModuleInfo) sourceCodeModule).getModuleName());
                    }
                }
            }
        }

        // Checks that method already exists in the class. If it already
        // exists then "overload" it using decorator; otherwise - just add to
        // the class.
        //
        IOpenMethod existedMethod = getDeclaredMethod(method.getName(), method.getSignature().getParameterTypes());
        if (existedMethod != null) {

            if (!existedMethod.getType().equals(method.getType())) {
                String message = String.format(
                    "Method \"%s\" with return type \"%s\" has already been defined with another return type (\"%s\")",
                    method.getName(),
                    method.getType().getDisplayName(0),
                    existedMethod.getType().getDisplayName(0));
                addDuplicatedMethodError(message, method, existedMethod);
                return;
            }

            if (method != existedMethod && method instanceof TestSuiteMethod) {
                validateTestSuiteMethod(method, existedMethod);
                return;
            }

            // Checks the instance of existed method. If it's the
            // OpenMethodDecorator then just add the method-candidate to
            // decorator; otherwise - replace existed method with new instance
            // of OpenMethodDecorator for existed method and add new one.
            //
            try {
                if (existedMethod instanceof OpenMethodDispatcher) {
                    OpenMethodDispatcher decorator = (OpenMethodDispatcher) existedMethod;
                    decorator.addMethod(undecorateForMultimoduleDispatching(m));
                } else {
                    if (m != existedMethod) {
                        // Create decorator for existed method.
                        //
                        OpenMethodDispatcher dispatcher = getOpenMethodDispatcher(existedMethod);

                        IOpenMethod openMethod = decorateForMultimoduleDispatching(dispatcher);

                        overrideMethod(openMethod);

                        dispatcher.addMethod(undecorateForMultimoduleDispatching(m));
                    }
                }
            } catch (DuplicatedMethodException e) {
                SyntaxNodeException error = null;
                if (m instanceof IMemberMetaInfo) {
                    IMemberMetaInfo memberMetaInfo = (IMemberMetaInfo) m;
                    if (memberMetaInfo.getSyntaxNode() != null) {
                        if (memberMetaInfo.getSyntaxNode() instanceof TableSyntaxNode) {
                            error = SyntaxNodeExceptionUtils
                                .createError(e.getMessage(), e, memberMetaInfo.getSyntaxNode());
                            ((TableSyntaxNode) memberMetaInfo.getSyntaxNode()).addError(error);
                        }
                    }
                }
                boolean f = false;
                for (Throwable t : getErrors()) {
                    if (t.getMessage().equals(e.getMessage())) {
                        f = true;
                        break;
                    }
                }
                if (!f) {
                    if (error != null) {
                        addError(error);
                    } else {
                        addError(e);
                    }
                }
            }
        } else {
            // Just wrap original method with dispatcher functionality.
            //

            if (dispatchingValidationEnabled && !(m instanceof TestSuiteMethod) && dimensionalPropertyPresented(m)) {
                // Create dispatcher for existed method.
                //
                OpenMethodDispatcher dispatcher = getOpenMethodDispatcher(m);

                IOpenMethod openMethod = decorateForMultimoduleDispatching(dispatcher);

                super.addMethod(openMethod);

            } else {
                super.addMethod(m);
            }
        }
    }

    private void validateTestSuiteMethod(IOpenMethod method, IOpenMethod existedMethod) {
        if (method instanceof IUriMember && existedMethod instanceof IUriMember) {
            if (!UriMemberHelper.isTheSame((IUriMember) method, (IUriMember) existedMethod)) {
                String message = ValidationMessages.getDuplicatedMethodMessage(existedMethod, method);
                addDuplicatedMethodError(message, method, existedMethod);
            }
        } else {
            throw new IllegalStateException("Implementation supports only IUriMember!");
        }
    }

    private boolean dimensionalPropertyPresented(IOpenMethod m) {
        List<TablePropertyDefinition> dimensionalPropertiesDef = TablePropertyDefinitionUtils
            .getDimensionalTableProperties();
        ITableProperties propertiesFromMethod = PropertiesHelper.getTableProperties(m);
        for (TablePropertyDefinition dimensionProperty : dimensionalPropertiesDef) {
            String propertyValue = propertiesFromMethod.getPropertyValueAsString(dimensionProperty.getName());
            if (StringUtils.isNotEmpty(propertyValue)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Dispatcher method should be added by adding all candidates of the specified dispatcher to current
     * XlsModuleOpenClass(it will cause adding methods to dispatcher of current module or creating new dispatcher in
     * current module).
     *
     * Previously there was problems because dispatcher from dependency was either added to dispatcher of current
     * module(dispatcher as a candidate in another dispatcher) or added to current module and was modified during the
     * current module processing. FIXME
     *
     * @param dispatcher Dispatcher methods to add.
     */
    public void addDispatcherMethod(OpenMethodDispatcher dispatcher) {
        for (IOpenMethod candidate : dispatcher.getCandidates()) {
            addMethod(candidate);
        }
    }

    private OpenMethodDispatcher getOpenMethodDispatcher(IOpenMethod method) {
        OpenMethodDispatcher decorator;
        IOpenMethod decorated = undecorateForMultimoduleDispatching(method);
        if (useDescisionTableDispatcher) {
            decorator = new OverloadedMethodsDispatcherTable(decorated, this);
        } else {
            decorator = new MatchingOpenMethodDispatcher(decorated, this);
        }
        return decorator;
    }

    @Override
    public void clearOddDataForExecutionMode() {
        super.clearOddDataForExecutionMode();
        if (!TestMethodNodeBinder.isKeepTestsInExecutionMode()) {
            dataBase = null;
        }
        rulesModuleBindingContext = null;
    }

    @Override
    public void addError(Throwable error) {
        if (error instanceof DuplicatedMethodException || error instanceof DuplicatedVarException || error instanceof DuplicatedTableException || error instanceof DuplicatedFieldException || error instanceof SyntaxNodeException) {
            if (VirtualSourceCodeModule.SOURCE_URI.equals(metaInfo.getSourceUrl())) {
                // Avoid duplication of error messages. This error was defined
                // in dependent module already.
                for (CompiledDependency dependency : getDependencies()) {
                    List<OpenLMessage> errors = OpenLMessagesUtils
                        .filterMessagesBySeverity(dependency.getCompiledOpenClass().getMessages(), Severity.ERROR);
                    for (OpenLMessage message : errors) {
                        if (message.getSummary() != null && message.getSummary().equals(error.getMessage())) {
                            return;
                        }
                    }
                }
            }
        }

        super.addError(error);
    }

    public void completeOpenClassBuilding() {
        addTestSuiteMethodsFromDependencies(); // Test method from dependencies
                                               // should use methods from this
                                               // class.
    }

    private TestSuiteMethod createNewTestSuiteMethod(TestSuiteMethod testSuiteMethod) {
        IOpenMethod method = testSuiteMethod.getTestedMethod();
        IOpenMethod newTargetMethod = getDeclaredMethod(method.getName(), method.getSignature().getParameterTypes());
        TestSuiteMethod copy = new TestSuiteMethod(newTargetMethod, testSuiteMethod);
        copy.setModuleName(testSuiteMethod.getModuleName());
        return copy;
    }

    protected void addTestSuiteMethodsFromDependencies() {
        for (CompiledDependency dependency : this.getDependencies()) {
            for (IOpenMethod depMethod : dependency.getCompiledOpenClass().getOpenClassWithErrors().getMethods()) {
                if (depMethod instanceof TestSuiteMethod) {
                    TestSuiteMethod testSuiteMethod = (TestSuiteMethod) depMethod;
                    try {
                        // Workaround for set dependency names in method while
                        // compile
                        if (testSuiteMethod.getModuleName() == null) {
                            testSuiteMethod.setModuleName(dependency.getDependencyName());
                        }
                        TestSuiteMethod newTestSuiteMethod = createNewTestSuiteMethod(testSuiteMethod);
                        addMethod(newTestSuiteMethod);
                    } catch (OpenlNotCheckedException e) {
                        if (Log.isDebugEnabled()) {
                            Log.debug(e.getMessage(), e);
                        }
                        addError(e);
                    }
                }
            }
        }
    }

    private void addDuplicatedMethodError(String message, IOpenMethod method, IOpenMethod existedMethod) {
        if (method instanceof IUriMember) {
            String uri = ((IUriMember) method).getUri();
            if (duplicatedErrosUris.contains(uri)) {
                return;
            }
        }
        ISyntaxNode newMethodSyntaxNode = method.getInfo().getSyntaxNode();
        if (newMethodSyntaxNode instanceof TableSyntaxNode) {
            SyntaxNodeException error = SyntaxNodeExceptionUtils.createError(message, newMethodSyntaxNode);
            ((TableSyntaxNode) newMethodSyntaxNode).addError(error);

            try {
                TableSyntaxNode existedMethodSyntaxNode = (TableSyntaxNode) existedMethod.getInfo().getSyntaxNode();
                if (existedMethodSyntaxNode != null) {
                    existedMethodSyntaxNode
                        .addError(SyntaxNodeExceptionUtils.createError(message, existedMethodSyntaxNode));
                }
            } catch (Exception ex) {
                log.warn("Cannot get a syntax node for the method: {}",
                    MethodUtil.printMethod(existedMethod, new StringBuilder()),
                    ex);
            }

            addError(error);
        } else {
            addError(new DuplicatedMethodException(message, method));
        }
    }
    
}
