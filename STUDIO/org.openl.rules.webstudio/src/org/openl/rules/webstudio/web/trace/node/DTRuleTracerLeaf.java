package org.openl.rules.webstudio.web.trace.node;

import org.openl.rules.dtx.IDecisionTable;
import org.openl.rules.table.ILogicalTable;

/**
 * Tracer leaf for the Decision Table Rule.
 *
 * @author Yury Molchan
 */
public class DTRuleTracerLeaf extends ATableTracerNode {

    private int ruleIndex;

    DTRuleTracerLeaf(int ruleIdx) {
        super("rule", null, null, null);
        this.ruleIndex = ruleIdx;
    }

    public String getRuleName() {
        return ((IDecisionTable) getTraceObject()).getRuleName(ruleIndex);
    }

    public ILogicalTable getRuleTable() {
        return ((IDecisionTable) getTraceObject()).getRuleTable(ruleIndex);
    }
}
