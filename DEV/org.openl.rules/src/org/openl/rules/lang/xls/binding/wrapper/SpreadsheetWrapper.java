package org.openl.rules.lang.xls.binding.wrapper;

import java.util.Map;

import org.openl.binding.BindingDependencies;
import org.openl.rules.calc.*;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.calc.result.IResultBuilder;
import org.openl.rules.lang.xls.binding.ATableBoundNode;
import org.openl.rules.lang.xls.binding.XlsModuleOpenClass;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.rules.table.Point;
import org.openl.rules.table.properties.ITableProperties;
import org.openl.types.*;
import org.openl.vm.IRuntimeEnv;

public class SpreadsheetWrapper extends Spreadsheet implements IOpenMethodWrapper {
    Spreadsheet delegate;

    XlsModuleOpenClass xlsModuleOpenClass;
    ContextPropertiesInjector contextPropertiesInjector;

    public SpreadsheetWrapper(XlsModuleOpenClass xlsModuleOpenClass,
            Spreadsheet delegate,
            ContextPropertiesInjector contextPropertiesInjector) {
        super();
        this.delegate = delegate;
        this.xlsModuleOpenClass = xlsModuleOpenClass;
        this.contextPropertiesInjector = contextPropertiesInjector;
    }

    @Override
    public Object invoke(Object target, Object[] params, IRuntimeEnv env) {
        return WrapperLogic.invoke(this, target, params, env);
    }

    @Override
    public XlsModuleOpenClass getXlsModuleOpenClass() {
        return xlsModuleOpenClass;
    }

    @Override
    public IOpenMethod getDelegate() {
        return delegate;
    }

    @Override
    public String toString() {
        return delegate.toString();
    }

    @Override
    public IOpenClass getDeclaringClass() {
        return delegate.getDeclaringClass();
    }

    @Override
    public String getDisplayName(int mode) {
        return delegate.getDisplayName(mode);
    }

    @Override
    public IOpenMethodHeader getHeader() {
        return delegate.getHeader();
    }

    @Override
    public String getUri() {
        return delegate.getUri();
    }

    @Override
    public IOpenMethod getMethod() {
        return delegate.getMethod();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public IMethodSignature getSignature() {
        return delegate.getSignature();
    }

    @Override
    public boolean isStatic() {
        return delegate.isStatic();
    }

    @Override
    public IOpenClass getType() {
        return delegate.getType();
    }

    @Override
    public boolean isCustomSpreadsheet() {
        return delegate.isCustomSpreadsheet();
    }

    @Override
    public SpreadsheetCell[][] getCells() {
        return delegate.getCells();
    }

    @Override
    public BindingDependencies getDependencies() {
        return delegate.getDependencies();
    }

    @Override
    public IResultBuilder getResultBuilder() {
        return delegate.getResultBuilder();
    }

    @Override
    public String getSourceUrl() {
        return delegate.getSourceUrl();
    }

    @Override
    public SpreadsheetOpenClass getSpreadsheetType() {
        return delegate.getSpreadsheetType();
    }

    @Override
    public int getHeight() {
        return delegate.getHeight();
    }

    @Override
    public void setCells(SpreadsheetCell[][] cells) {
        delegate.setCells(cells);
    }

    @Override
    public void setBoundNode(ATableBoundNode node) {
        delegate.setBoundNode(node);
    }

    @Override
    public void setColumnNames(String[] colNames) {
        delegate.setColumnNames(colNames);
    }

    @Override
    public SpreadsheetBoundNode getBoundNode() {
        return delegate.getBoundNode();
    }

    @Override
    public void setResultBuilder(IResultBuilder resultBuilder) {
        delegate.setResultBuilder(resultBuilder);
    }

    @Override
    public Map<String, Object> getProperties() {
        return delegate.getProperties();
    }

    @Override
    public void setRowNames(String[] rowNames) {
        delegate.setRowNames(rowNames);
    }

    @Override
    public void setSpreadsheetType(SpreadsheetOpenClass spreadsheetType) {
        delegate.setSpreadsheetType(spreadsheetType);
    }

    @Override
    public ITableProperties getMethodProperties() {
        return delegate.getMethodProperties();
    }

    @Override
    public IMemberMetaInfo getInfo() {
        return delegate.getInfo();
    }

    @Override
    public int getWidth() {
        return delegate.getWidth();
    }

    @Override
    public String[] getRowNames() {
        return delegate.getRowNames();
    }

    @Override
    public String[] getColumnNames() {
        return delegate.getColumnNames();
    }

    @Override
    public TableSyntaxNode getSyntaxNode() {
        return delegate.getSyntaxNode();
    }

    @Override
    public void setInvoker(SpreadsheetInvoker invoker) {
        delegate.setInvoker(invoker);
    }

    @Override
    public String getModuleName() {
        return delegate.getModuleName();
    }

    @Override
    public void setModuleName(String dependencyName) {
        delegate.setModuleName(dependencyName);
    }

    @Override
    public boolean isConstructor() {
        return delegate.isConstructor();
    }

    @Override
    public void setCustomSpreadsheetResultType(CustomSpreadsheetResultOpenClass spreadsheetCustomResultType) {
        delegate.setCustomSpreadsheetResultType(spreadsheetCustomResultType);
    }

    @Override
    public void setRowTitles(String[] rowTitles) {
        delegate.setRowTitles(rowTitles);
    }

    @Override
    public String[] getRowNamesForResultModel() {
        return delegate.getRowNamesForResultModel();
    }

    @Override
    public void setRowNamesForResultModel(String[] rowNamesForResultModel) {
        delegate.setRowNamesForResultModel(rowNamesForResultModel);
    }

    @Override
    public String[] getColumnNamesForResultModel() {
        return delegate.getColumnNamesForResultModel();
    }

    @Override
    public void setColumnNamesForResultModel(String[] columnNamesForResultModel) {
        delegate.setColumnNamesForResultModel(columnNamesForResultModel);
    }

    @Override
    public String[] getRowTitles() {
        return delegate.getRowTitles();
    }

    @Override
    public void setColumnTitles(String[] columnTitles) {
        delegate.setColumnTitles(columnTitles);
    }

    @Override
    public String[] getColumnTitles() {
        return delegate.getColumnTitles();
    }

    @Override
    public boolean isDetailedPlainModel() {
        return delegate.isDetailedPlainModel();
    }

    @Override
    public void setDetailedPlainModel(boolean detailedPlainModel) {
        delegate.setDetailedPlainModel(detailedPlainModel);
    }

    @Override
    public Map<String, Point> getFieldsCoordinates() {
        return delegate.getFieldsCoordinates();
    }

    private TopClassOpenMethodWrapperCache topClassOpenMethodWrapperCache = new TopClassOpenMethodWrapperCache(this);

    @Override
    public IOpenMethod getTopOpenClassMethod(IOpenClass openClass) {
        return topClassOpenMethodWrapperCache.getTopOpenClassMethod(openClass);
    }

    @Override
    public ContextPropertiesInjector getContextPropertiesInjector() {
        return contextPropertiesInjector;
    }

}
