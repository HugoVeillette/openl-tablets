package org.openl.rules.validation;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.BeforeClass;
import org.junit.Test;
import org.openl.engine.OpenLSystemProperties;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.OpenLWarnMessage;
import org.openl.message.Severity;
import org.openl.rules.BaseOpenlBuilderHelper;
import org.openl.rules.lang.xls.syntax.TableSyntaxNode;
import org.openl.syntax.ISyntaxNode;

public class DispatcherTableBuildingTest extends BaseOpenlBuilderHelper {
    private static final String SRC = "test/rules/overload/DispatcherTest.xlsx";

    public DispatcherTableBuildingTest() {
        super(SRC);
    }

    @BeforeClass
    public static void init() {
        System.setProperty(OpenLSystemProperties.DISPATCHING_MODE_PROPERTY, OpenLSystemProperties.DISPATCHING_MODE_JAVA);
    }

    private static List<OpenLMessage> getWarningsForTable(List<OpenLMessage> allMessages, TableSyntaxNode tsn) {
        List<OpenLMessage> warningMessages = OpenLMessagesUtils.filterMessagesBySeverity(allMessages, Severity.WARN);
        List<OpenLMessage> warningsForTable = new ArrayList<OpenLMessage>();
        for (OpenLMessage message : warningMessages) {
            if (message instanceof OpenLWarnMessage) {// there can be simple
                                                      // OpenLMessages with
                                                      // severity WARN
                OpenLWarnMessage warning = (OpenLWarnMessage) message;
                ISyntaxNode syntaxNode = warning.getSource();
                if (syntaxNode == tsn) {
                    warningsForTable.add(warning);
                }
            }
        }
        return warningsForTable;
    }

    @Test
    public void checkArraysInSignature() {
        TableSyntaxNode dispatcherTable = findDispatcherForMethod("arraysTest");
        assertNotNull(dispatcherTable);
        assertFalse(dispatcherTable.hasErrors());
        assertTrue(getWarningsForTable(getJavaWrapper().getCompiledClass().getMessages(), dispatcherTable).size() == 0);
    }

    @Test
    public void checkKeywordsInSignature() {
        TableSyntaxNode dispatcherTable = findDispatcherForMethod("keywordsTest");
        assertNotNull(dispatcherTable);
        assertFalse(dispatcherTable.hasErrors());
        assertTrue(getWarningsForTable(getJavaWrapper().getCompiledClass().getMessages(), dispatcherTable).size() == 0);
    }
}
