/*
 * Created on Oct 23, 2003
 *
 * Developed by Intelligent ChoicePoint Inc. 2003
 */

package org.openl.rules.lang.xls.binding;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.binding.IBindingContext;
import org.openl.binding.exception.DuplicatedFieldException;
import org.openl.binding.exception.DuplicatedMethodException;
import org.openl.binding.impl.module.ModuleOpenClass;
import org.openl.classloader.OpenLBundleClassLoader;
import org.openl.dependency.CompiledDependency;
import org.openl.engine.ExtendableModuleOpenClass;
import org.openl.engine.OpenLSystemProperties;
import org.openl.exception.OpenlNotCheckedException;
import org.openl.rules.binding.RulesModuleBindingContext;
import org.openl.rules.calc.CustomSpreadsheetResultOpenClass;
import org.openl.rules.calc.SpreadsheetBoundNode;
import org.openl.rules.constants.ConstantOpenField;
import org.openl.rules.data.IDataBase;
import org.openl.rules.data.ITable;
import org.openl.rules.lang.xls.XlsNodeTypes;
import org.openl.rules.lang.xls.binding.wrapper.IOpenMethodWrapper;
import org.openl.rules.lang.xls.binding.wrapper.WrapperLogic;
import org.openl.rules.lang.xls.prebind.ILazyMember;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.lang.xls.syntax.XlsModuleSyntaxNode;
import org.openl.rules.table.OpenLArgumentsCloner;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.rules.table.properties.PropertiesHelper;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.types.UriMemberHelper;
import org.openl.rules.types.impl.MatchingOpenMethodDispatcher;
import org.openl.rules.types.impl.OverloadedMethodsDispatcherTable;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.code.IParsedCode;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeExceptionUtils;
import org.openl.types.IMemberMetaInfo;
import org.openl.types.IModuleInfo;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenField;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.AMethod;
import org.openl.util.ClassUtils;
import org.openl.util.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.rits.cloning.Cloner;

/**
 * @author snshor
 *
 */
public class XlsModuleOpenClass extends ModuleOpenClass implements ExtendableModuleOpenClass {

    private final Logger log = LoggerFactory.getLogger(XlsModuleOpenClass.class);

    private IDataBase dataBase;

    /**
     * Whether DecisionTable should be used as a dispatcher for overloaded tables. By default(this flag equals false)
     * dispatching logic will be performed in Java code.
     */
    private boolean useDecisionTableDispatcher;

    private boolean dispatchingValidationEnabled;

    private Collection<String> imports = new HashSet<>();

    private ClassLoader classLoader;

    private OpenLBundleClassLoader classGenerationClassLoader;

    private RulesModuleBindingContext rulesModuleBindingContext;

    private XlsDefinitions xlsDefinitions = new XlsDefinitions();

    private String csrBeansPackage;

    public RulesModuleBindingContext getRulesModuleBindingContext() {
        return rulesModuleBindingContext;
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
            IBindingContext bindingContext) {
        super(name, openl);

        this.dataBase = dbase;
        this.metaInfo = metaInfo;
        this.useDecisionTableDispatcher = OpenLSystemProperties.isDTDispatchingMode(bindingContext.getExternalParams());
        this.dispatchingValidationEnabled = OpenLSystemProperties
            .isDispatchingValidationEnabled(bindingContext.getExternalParams());
        this.classLoader = classLoader;

        this.classGenerationClassLoader = new OpenLBundleClassLoader(null);
        this.classGenerationClassLoader.addClassLoader(classLoader);

        this.csrBeansPackage = getCsrBeansPackage(bindingContext);

        this.rulesModuleBindingContext = new RulesModuleBindingContext(bindingContext, this);

        if (usingModules != null) {
            setDependencies(usingModules);
            initDependencies();
        }
        initImports(metaInfo.getXlsModuleNode());
    }

    private String getCsrBeansPackage(IBindingContext bindingContext) {
        if (bindingContext.getExternalParams().get(SpreadsheetBoundNode.CSR_BEANS_PACKAGE) instanceof String) {
            String packageName = (String) bindingContext.getExternalParams()
                .get(SpreadsheetBoundNode.CSR_BEANS_PACKAGE);
            if (ClassUtils.isValidPackageName(packageName)) {
                return packageName;
            } else if (log.isWarnEnabled()) {
                log.warn(
                    "Invalid package name '{}' is defined for generated custom spreadsheet result beans for module '{}'. Default value 'org.openl.generated.csr' is used.",
                    packageName,
                    getName());
            }
        }
        return "org.openl.generated.csr";
    }

    public String getCsrBeansPackage() {
        return csrBeansPackage;
    }

    public boolean isUseDecisionTableDispatcher() {
        return useDecisionTableDispatcher;
    }

    public ClassLoader getClassLoader() {
        return classLoader;
    }

    public OpenLBundleClassLoader getClassGenerationClassLoader() {
        return classGenerationClassLoader;
    }

    private void initImports(XlsModuleSyntaxNode xlsModuleSyntaxNode) {
        imports.addAll(xlsModuleSyntaxNode.getImports());
    }

    // TODO: should be placed to ModuleOpenClass
    public IDataBase getDataBase() {
        return dataBase;
    }

    protected void addXlsDefinitions(CompiledDependency dependency) {
        IOpenClass openClass = dependency.getCompiledOpenClass().getOpenClassWithErrors();
        if (openClass instanceof XlsModuleOpenClass) {
            XlsModuleOpenClass xlsModuleOpenClass = (XlsModuleOpenClass) openClass;
            this.xlsDefinitions.addAll(xlsModuleOpenClass.getXlsDefinitions());
        }
    }

    public XlsDefinitions getXlsDefinitions() {
        return xlsDefinitions;
    }

    @Override
    protected IOpenClass processDependencyTypeBeforeAdding(IOpenClass type) {
        if (type instanceof CustomSpreadsheetResultOpenClass) {
            IOpenClass existingType = findType(type.getName());
            if (existingType instanceof CustomSpreadsheetResultOpenClass) {
                CustomSpreadsheetResultOpenClass existingCustomSpreadsheetResultOpenClass = (CustomSpreadsheetResultOpenClass) existingType;
                existingCustomSpreadsheetResultOpenClass.extendWith((CustomSpreadsheetResultOpenClass) type);
                return existingCustomSpreadsheetResultOpenClass;
            } else {
                return ((CustomSpreadsheetResultOpenClass) type).makeCopyForModule(this);
            }
        }
        return super.processDependencyTypeBeforeAdding(type);
    }

    /**
     * Populate current module fields with data from dependent modules.
     */
    @Override
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

            addXlsDefinitions(dependency);

            addMethods(dependency);
            // Populate current module fields with data from dependent modules.
            // Requered
            // for data tables inheriting from dependend modules.
            addDataTables(dependency.getCompiledOpenClass()); // Required for
            // data tables.
            addFields(dependency);
        }

        for (IOpenClass type : getTypes()) {
            if (type instanceof CustomSpreadsheetResultOpenClass) {
                ((CustomSpreadsheetResultOpenClass) type).fixCSRFields();
            }
        }
    }

    public Collection<String> getImports() {
        return imports;
    }

    @SuppressWarnings("unchecked")
    protected IOpenMethod extractNonLazyMethod(IOpenMethod method) {
        if (method instanceof ILazyMember) {
            return extractNonLazyMethod(((ILazyMember<IOpenMethod>) method).getOriginal());
        }
        return method;
    }

    @Override
    protected boolean isDependencyMethodInheritable(IOpenMethod openMethod) {
        IOpenMethod method = extractNonLazyMethod(openMethod);
        if (method instanceof TestSuiteMethod) {
            return false;
        }
        return super.isDependencyMethodInheritable(method);
    }

    @SuppressWarnings("unchecked")
    protected IOpenField extractNonLazyMember(IOpenField openField) {
        if (openField instanceof ILazyMember) {
            return extractNonLazyMember(((ILazyMember<IOpenField>) openField).getOriginal());
        }
        return openField;
    }

    @Override
    protected boolean isDependencyFieldInheritable(IOpenField openField) {
        IOpenField field = extractNonLazyMember(openField);
        if (field instanceof ConstantOpenField) {
            return true;
        }
        return super.isDependencyFieldInheritable(field);
    }

    @Override
    public void applyToDependentParsedCode(IParsedCode parsedCode) {
        Objects.requireNonNull(parsedCode, "parsedCode cannot be null");
        if (parsedCode.getTopNode() instanceof XlsModuleSyntaxNode) {
            XlsModuleSyntaxNode xlsModuleSyntaxNode = (XlsModuleSyntaxNode) parsedCode.getTopNode();
            for (String value : getImports()) {
                xlsModuleSyntaxNode.addImport(value);
            }
        }
    }

    private void addDataTables(CompiledOpenClass dependency) {
        IOpenClass openClass = dependency.getOpenClassWithErrors();

        if (openClass instanceof XlsModuleOpenClass) {
            XlsModuleOpenClass xlsModuleOpenClass = (XlsModuleOpenClass) openClass;
            if (xlsModuleOpenClass.getDataBase() != null) {
                for (ITable table : xlsModuleOpenClass.getDataBase().getTables()) {
                    if (XlsNodeTypes.XLS_DATA.toString().equals(table.getTableSyntaxNode().getType())) {
                        try {
                            getDataBase().registerTable(table);
                        } catch (DuplicatedTableException | OpenlNotCheckedException e) {
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

    @Override
    public void addField(IOpenField openField) {
        Map<String, IOpenField> fields = fieldMap();
        IOpenField field = extractNonLazyMember(openField);
        if (fields.containsKey(openField.getName())) {
            IOpenField existedField = extractNonLazyMember(fields.get(openField.getName()));
            if (field instanceof ConstantOpenField && existedField instanceof ConstantOpenField) { // Ignore
                // constants
                // with
                // the
                // same
                // values
                if (field.getType().equals(existedField.getType()) && Objects
                    .equals(((ConstantOpenField) field).getValue(), ((ConstantOpenField) existedField).getValue())) {
                    return;
                }

                throw new DuplicatedFieldException("", field.getName());
            }

            UriMemberHelper.validateFieldDuplication(openField, existedField);
        }
        fieldMap().put(openField.getName(), openField);
        if (field instanceof ConstantOpenField) {
            constantFields.put(openField.getName(), openField);
        }
        addFieldToLowerCaseMap(openField);
    }

    private Map<String, IOpenField> constantFields = new HashMap<>();

    public ConstantOpenField getConstantField(String fname) {
        IOpenField openField = constantFields.get(fname);
        return (ConstantOpenField) extractNonLazyMember(openField);
    }

    public Map<String, IOpenField> getConstantFields() {
        return Collections.unmodifiableMap(constantFields);
    }

    /**
     * Adds method to <code>XlsModuleOpenClass</code>.
     *
     * @param method method object
     */
    @Override
    public void addMethod(IOpenMethod method) {
        if (method instanceof OpenMethodDispatcher) {
            /*
             * Dispatcher method should be added by adding all candidates of the specified dispatcher to current
             * XlsModuleOpenClass(it will cause adding methods to dispatcher of current module or creating new
             * dispatcher in current module).
             *
             * Previously there was problems because dispatcher from dependency was either added to dispatcher of
             * current module(dispatcher as a candidate in another dispatcher) or added to current module and was
             * modified during the current module processing. FIXME
             */
            for (IOpenMethod candidate : ((OpenMethodDispatcher) method).getCandidates()) {
                addMethod(candidate);
            }
            return;
        }
        IOpenMethod m = decorateForMultimoduleDispatching(method);

        // Workaround needed to set the module name in the method while compile
        if (m instanceof AMethod && ((AMethod) m).getModuleName() == null) {
            XlsMetaInfo metaInfo = getXlsMetaInfo();
            if (metaInfo != null) {
                IOpenSourceCodeModule sourceCodeModule = metaInfo.getXlsModuleNode().getModule();
                if (sourceCodeModule instanceof IModuleInfo) {
                    ((AMethod) m).setModuleName(((IModuleInfo) sourceCodeModule).getModuleName());
                }
            }
        }

        // Checks that method already exists in the class. If it already
        // exists then "overload" it using decorator; otherwise - just add to
        // the class.
        //
        IOpenMethod existedMethod = getDeclaredMethod(m.getName(), m.getSignature().getParameterTypes());
        if (existedMethod != null) {

            if (!existedMethod.getType().equals(m.getType())) {
                String message = String.format(
                    "Method '%s' with return type '%s' is already defined with another return type ('%s')",
                    m.getName(),
                    m.getType().getDisplayName(0),
                    existedMethod.getType().getDisplayName(0));
                throw new DuplicatedMethodException(message, existedMethod, method);
            }

            if (!m.equals(existedMethod) && method instanceof TestSuiteMethod) {
                UriMemberHelper.validateMethodDuplication(method, existedMethod);
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
                    if (!m.equals(existedMethod)) {
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

    private OpenMethodDispatcher getOpenMethodDispatcher(IOpenMethod method) {
        OpenMethodDispatcher decorator;
        IOpenMethod decorated = undecorateForMultimoduleDispatching(method);
        if (useDecisionTableDispatcher) {
            decorator = new OverloadedMethodsDispatcherTable(decorated, this);
        } else {
            decorator = new MatchingOpenMethodDispatcher(decorated, this);
        }
        return decorator;
    }

    @Override
    public void clearOddDataForExecutionMode() {
        super.clearOddDataForExecutionMode();
        dataBase = null;
        rulesModuleBindingContext = null;
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

    private void validateType(IOpenClass type) {
        if (type instanceof CustomSpreadsheetResultOpenClass) {
            for (IOpenClass t : getTypes()) {
                if (t instanceof CustomSpreadsheetResultOpenClass) {
                    CustomSpreadsheetResultOpenClass csrType = (CustomSpreadsheetResultOpenClass) t;
                    if (Objects.equals(csrType.getName(), type.getName()) && csrType.isBeanClassInitialized()) {
                        throw new IllegalStateException(String.format(
                            "This module does not support adding '%s' custom spreadsheet result types. Bean class has already been initialized for this custom spreadsheet result type.",
                            csrType.getName()));
                    }
                }
            }
        }
    }

    @Override
    public void addType(IOpenClass type) {
        validateType(type);
        super.addType(type);
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
                        addError(e);
                    }
                }
            }
        }
    }

    private volatile OpenLArgumentsCloner cloner = null;

    public Cloner getCloner() {
        if (cloner == null) {
            synchronized (this) {
                if (cloner == null) {
                    cloner = new OpenLArgumentsCloner();
                }
            }
        }
        return cloner;
    }

}
