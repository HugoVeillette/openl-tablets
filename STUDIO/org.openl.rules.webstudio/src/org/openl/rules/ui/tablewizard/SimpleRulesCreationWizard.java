package org.openl.rules.ui.tablewizard;

import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.faces.model.SelectItemGroup;
import javax.faces.validator.ValidatorException;
import javax.validation.constraints.Pattern;

import org.hibernate.validator.constraints.NotBlank;
import org.openl.base.INamedThing;
import org.openl.commons.web.jsf.FacesUtils;
import org.openl.meta.BigDecimalValue;
import org.openl.meta.BigIntegerValue;
import org.openl.meta.ByteValue;
import org.openl.meta.DoubleValue;
import org.openl.meta.FloatValue;
import org.openl.meta.IntValue;
import org.openl.meta.LongValue;
import org.openl.meta.ShortValue;
import org.openl.rules.helpers.DoubleRange;
import org.openl.rules.helpers.IntRange;
import org.openl.rules.lang.xls.XlsSheetSourceCodeModule;
import org.openl.rules.table.properties.def.TablePropertyDefinition;
import org.openl.rules.table.properties.def.TablePropertyDefinitionUtils;
import org.openl.rules.table.properties.inherit.InheritanceLevel;
import org.openl.rules.table.xls.XlsSheetGridModel;
import org.openl.rules.table.xls.builder.CreateTableException;
import org.openl.rules.table.xls.builder.DataTableBuilder;
import org.openl.rules.table.xls.builder.SimpleRulesTableBuilder;
import org.openl.rules.table.xls.builder.TableBuilder;
import org.openl.rules.ui.tablewizard.util.CellStyleManager;
import org.openl.rules.ui.tablewizard.util.JSONHolder;
import org.openl.types.IOpenClass;
import org.openl.types.impl.DomainOpenClass;
import org.openl.util.IntegerValuesUtils;
import org.openl.util.StringUtils;
import org.richfaces.json.JSONException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SimpleRulesCreationWizard extends TableCreationWizard {
    private static final String TABLE_TYPE = "xls.dt";
    private static final String RESTORE_TABLE_FUNCTION = "tableModel.restoreTableFromJSONString";

    private final Logger log = LoggerFactory.getLogger(SimpleRulesCreationWizard.class);

    @NotBlank(message = "Can not be empty")
    @Pattern(regexp = "([a-zA-Z_][a-zA-Z_0-9]*)?", message = INVALID_NAME_MESSAGE)
    private String tableName;
    private SelectItem[] domainTypes;
    private String jsonTable;
    private JSONHolder table;
    private String restoreTable;

    private List<DomainTypeHolder> typesList;

    private String returnValueType;

    private List<TypeNamePair> parameters = new ArrayList<>();

    @Override
    protected void onCancel() {
        reset();
    }

    @Override
    protected void onStart() {
        reset();

        initDomainType();
        initWorkbooks();
    }

    private void initDomainType() {
        List<IOpenClass> types = new ArrayList<>(WizardUtils.getProjectOpenClass().getTypes());
        Collection<IOpenClass> importedClasses = WizardUtils.getImportedClasses();
        types.addAll(importedClasses);

        List<String> datatypes = new ArrayList<>(types.size());
        datatypes.add("");
        for (IOpenClass datatype : types) {
            if (Modifier.isFinal(datatype.getInstanceClass().getModifiers())) {
                // cannot inherit from final class
                continue;
            }

            if (!(datatype instanceof DomainOpenClass)) {
                datatypes.add(datatype.getDisplayName(INamedThing.SHORT));
            }
        }

        Collection<String> allClasses = DomainTree.buildTree(WizardUtils.getProjectOpenClass()).getAllClasses();
        for (IOpenClass type : importedClasses) {
            allClasses.add(type.getDisplayName(INamedThing.SHORT));
        }

        domainTypes = FacesUtils.createSelectItems(allClasses);

        Collection<IOpenClass> classTypes = DomainTree.buildTree(WizardUtils.getProjectOpenClass()).getAllOpenClasses();
        this.typesList = new ArrayList<>();

        for (IOpenClass oc : classTypes) {
            typesList.add(new DomainTypeHolder(oc.getDisplayName(INamedThing.SHORT), oc, false));
        }
    }

    public int getColumnSize() {
        int size = 0;
        size += this.parameters.size();
        size++;

        return size;
    }

    public List<SelectItem> getPropertyList() {
        List<SelectItem> propertyNames = new ArrayList<>();
        TablePropertyDefinition[] propDefinitions = TablePropertyDefinitionUtils
            .getDefaultDefinitionsForTable(TABLE_TYPE, InheritanceLevel.TABLE, true);

        SelectItem selectItem = new SelectItem("");
        selectItem.setLabel("");
        propertyNames.add(selectItem);

        Map<String, List<TablePropertyDefinition>> propGroups = TablePropertyDefinitionUtils
            .groupProperties(propDefinitions);
        for (Map.Entry<String, List<TablePropertyDefinition>> entry : propGroups.entrySet()) {
            List<SelectItem> items = new ArrayList<>();

            for (TablePropertyDefinition propDefinition : entry.getValue()) {
                String propName = propDefinition.getName();
                if (propDefinition.getDeprecation() == null) {
                    items.add(new SelectItem(propName, propDefinition.getDisplayName()));
                }
            }

            if (!items.isEmpty()) {
                SelectItemGroup itemGroup = new SelectItemGroup(entry.getKey());
                itemGroup.setSelectItems(items.toArray(new SelectItem[items.size()]));
                propertyNames.add(itemGroup);
            }
        }

        return propertyNames;
    }

    public DomainTypeHolder getReturnValue() {
        return getTypedParameterByName(this.returnValueType);
    }

    private DomainTypeHolder getTypedParameterByName(String name) {
        DomainTypeHolder dth = null;

        for (DomainTypeHolder type : typesList) {
            if (type.name.equals(name)) {
                dth = type.clone();
                break;
            }
        }

        if (dth == null) {
            dth = new DomainTypeHolder(name, "STRING", false);
        }

        return dth;
    }

    public List<DomainTypeHolder> getTypedParameters() {
        List<DomainTypeHolder> typedParameters = new ArrayList<>();

        for (TypeNamePair tnp : this.parameters) {
            DomainTypeHolder gth = getTypedParameterByName(tnp.getType());
            gth.setTypeName(tnp.getName());
            gth.setIterable(tnp.isIterable());

            typedParameters.add(gth);
        }

        return typedParameters;
    }

    @Override
    protected void onFinish() throws Exception {
        XlsSheetSourceCodeModule sheetSourceModule = getDestinationSheet();
        String newTableUri = buildTable(sheetSourceModule);
        setNewTableId(newTableUri);
        getModifiedWorkbooks().add(sheetSourceModule.getWorkbookSource());
        super.onFinish();
    }

    protected String buildTable(XlsSheetSourceCodeModule sourceCodeModule) throws CreateTableException {
        XlsSheetGridModel gridModel = new XlsSheetGridModel(sourceCodeModule);
        SimpleRulesTableBuilder builder = new SimpleRulesTableBuilder(gridModel);

        CellStyleManager styleManager = new CellStyleManager(gridModel, table);

        Map<String, Object> properties = buildProperties();
        properties.putAll(table.getProperties());

        int width = DataTableBuilder.MIN_WIDTH;
        if (!properties.isEmpty()) {
            width = TableBuilder.PROPERTIES_MIN_WIDTH;
        }

        List<List<Map<String, Object>>> rows = table.getDataRows(styleManager);
        width = Math.max(table.getFieldsCount(), width);
        int height = TableBuilder.HEADER_HEIGHT + properties.size() + rows.size();

        builder.beginTable(width, height);
        builder.writeHeader(table.getHeaderStr(), styleManager.getHeaderStyle());

        builder.writeProperties(properties, styleManager.getPropertyStyles());

        for (List<Map<String, Object>> row : rows) {
            builder.writeTableBodyRow(row);
        }

        String uri = gridModel.getRangeUri(builder.getTableRegion());

        builder.endTable();

        return uri;
    }

    public void addParameter() {
        parameters.add(new TypeNamePair());
    }

    public void removeParameter(TypeNamePair parameter) {
        parameters.remove(parameter);
    }

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public SelectItem[] getDomainTypes() {
        return domainTypes;
    }

    public void setDomainTypes(SelectItem[] domainTypes) {
        this.domainTypes = domainTypes;
    }

    public String getReturnValueType() {
        return returnValueType;
    }

    public void setReturnValueType(String returnValueType) {
        this.returnValueType = returnValueType;
    }

    public List<TypeNamePair> getParameters() {
        return parameters;
    }

    public void setParameters(List<TypeNamePair> parameters) {
        this.parameters = parameters;
    }

    public List<DomainTypeHolder> getTypesList() {
        return typesList;
    }

    public void setTypesList(List<DomainTypeHolder> typesList) {
        this.typesList = typesList;
    }

    public final class DomainTypeHolder {
        private String name;
        private String type;
        private boolean iterable;
        private String typeName;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        DomainTypeHolder(String name, IOpenClass openClass, boolean iterable) {
            this.name = name;
            this.iterable = iterable;

            if (openClass != null) {
                if (openClass.isArray()) {
                    this.type = "ARRAY";
                } else if (openClass.toString().equals(Date.class.getCanonicalName())) {
                    this.type = "DATE";
                } else if (openClass.toString().equals(boolean.class.getCanonicalName()) || openClass.toString()
                    .equals(Boolean.class.getCanonicalName())) {
                    this.type = "BOOLEAN";
                } else if (IntegerValuesUtils.isIntegerValue(openClass.getInstanceClass())) {
                    this.type = "INT";
                } else if (openClass.toString().equals(BigDecimal.class.getCanonicalName()) || openClass.toString()
                    .equals(BigDecimalValue.class.getCanonicalName()) || openClass.toString()
                        .equals(DoubleValue.class.getCanonicalName()) || openClass.toString()
                            .equals(Double.class.getCanonicalName()) || openClass.toString()
                                .equals(FloatValue.class.getCanonicalName()) || openClass.toString()
                                    .equals(Float.class.getCanonicalName()) || openClass.toString()
                                        .equals(double.class.getCanonicalName()) || openClass.toString()
                                            .equals(float.class.getCanonicalName())) {
                    this.type = "FLOAT";
                } else if (openClass.toString().equals(IntRange.class.getCanonicalName()) || openClass.toString()
                    .equals(DoubleRange.class.getCanonicalName())) {
                    this.type = "RANGE";
                } else {
                    this.type = "STRING";
                }
            }
        }

        DomainTypeHolder(String name, String type, boolean iterable) {
            this.name = name;
            this.iterable = iterable;
            this.type = type;
        }

        public DomainTypeHolder clone() {
            return new DomainTypeHolder(this.name, this.type, this.iterable);
        }

        public String getTypeName() {
            return typeName;
        }

        public void setTypeName(String typeName) {
            this.typeName = typeName;
        }

        public boolean isIterable() {
            return iterable;
        }

        public void setIterable(boolean iterable) {
            this.iterable = iterable;
        }
    }

    public String getJsonTable() {
        return jsonTable;
    }

    public void setJsonTable(String jsonTable) {
        this.jsonTable = jsonTable;

        try {
            this.table = new JSONHolder(jsonTable);
            this.restoreTable = getTableInitFunction(jsonTable);
        } catch (JSONException e) {
            log.error(e.getMessage(), e);
        }
    }

    private String getTableInitFunction(String jsonStr) {
        return RESTORE_TABLE_FUNCTION + "(" + jsonStr + ")";
    }

    public String getRestoreTable() {
        return restoreTable;
    }

    public void setRestoreTable(String restoreTable) {
        this.restoreTable = restoreTable;
    }

    @Override
    protected void reset() {
        jsonTable = null;
        table = null;
        restoreTable = null;

        super.reset();
    }

    /**
     * Validation for properties name
     */
    public void validatePropsName(FacesContext context, UIComponent toValidate, Object value) {
        String name = ((String) value);
        int paramId = this.getParamId(toValidate.getClientId());
        checkParameterName(name);

        for (int i = 0; i < parameters.size(); i++) {
            TypeNamePair param = parameters.get(i);
            if (paramId != i && param.getName() != null && param.getName().equals(name)) {
                throw new ValidatorException(new FacesMessage("Parameter with such name already exists"));
            }
        }
    }

    /**
     * Validation for table name
     */
    public void validateTableName(FacesContext context, UIComponent toValidate, Object value) {
        String name = ((String) value);
        checkParameterName(name);
    }

    public void pNameListener(ValueChangeEvent valueChangeEvent) {
        int paramId = this.getParamId(valueChangeEvent.getComponent().getClientId());

        this.parameters.get(paramId).setName(valueChangeEvent.getNewValue().toString());
    }

    public void pTypeListener(ValueChangeEvent valueChangeEvent) {
        int paramId = this.getParamId(valueChangeEvent.getComponent().getClientId());

        this.parameters.get(paramId).setType(valueChangeEvent.getNewValue().toString());
    }

    public void pIterableListener(ValueChangeEvent valueChangeEvent) {
        int paramId = this.getParamId(valueChangeEvent.getComponent().getClientId());

        this.parameters.get(paramId).setIterable(Boolean.getBoolean(valueChangeEvent.getNewValue().toString()));
    }

    private int getParamId(String componentId) {
        if (componentId != null) {
            String[] elements = componentId.split(":");

            if (elements.length > 3) {
                return Integer.parseInt(elements[2]);
            }
        }

        return 0;
    }

    private void checkParameterName(String name) {
        if (StringUtils.isEmpty(name)) {
            throw new ValidatorException(new FacesMessage("Can not be empty"));
        }

        if (!name.matches("([a-zA-Z_][a-zA-Z_0-9]*)?")) {
            throw new ValidatorException(new FacesMessage(INVALID_NAME_MESSAGE));
        }
    }
}
