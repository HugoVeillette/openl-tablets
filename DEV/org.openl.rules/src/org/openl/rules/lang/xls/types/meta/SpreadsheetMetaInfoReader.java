package org.openl.rules.lang.xls.types.meta;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.openl.binding.impl.NodeType;
import org.openl.binding.impl.NodeUsage;
import org.openl.binding.impl.SimpleNodeUsage;
import org.openl.engine.OpenLCellExpressionsCompiler;
import org.openl.rules.calc.SpreadsheetBoundNode;
import org.openl.rules.calc.element.SpreadsheetCell;
import org.openl.rules.lang.xls.types.CellMetaInfo;
import org.openl.rules.table.CellKey;
import org.openl.rules.table.ICell;
import org.openl.types.IOpenClass;
import org.openl.types.impl.CompositeMethod;
import org.openl.types.java.JavaOpenClass;

public class SpreadsheetMetaInfoReader extends AMethodMetaInfoReader<SpreadsheetBoundNode> {
    // Typically too few header cells have any meta info. Header may not contain any cell at all.
    // It's more convenient to store existing header meta info in the Map.
    // Remove it in the future: retrieve header meta info from compiled data.
    private Map<CellKey, CellMetaInfo> headerMetaInfo = new HashMap<>();

    public SpreadsheetMetaInfoReader(SpreadsheetBoundNode boundNode) {
        super(boundNode);
    }

    @Override
    public CellMetaInfo getBodyMetaInfo(int row, int col) {
        SpreadsheetCell[][] cells = getBoundNode().getSpreadsheet().getCells();
        ICell firstCell = cells[0][0].getSourceCell();
        int r = row - firstCell.getAbsoluteRow();
        int c = col - firstCell.getAbsoluteColumn();

        if (r < 0 || c < 0) {
            return headerMetaInfo.get(CellKey.CellKeyFactory.getCellKey(col, row));
        }

        SpreadsheetCell spreadsheetCell = cells[r][c];
        ICell sourceCell = spreadsheetCell.getSourceCell();
        IOpenClass type = spreadsheetCell.getType();

        String stringValue = sourceCell.getStringValue();
        if (stringValue != null) {
            if (stringValue.startsWith("=") || stringValue.startsWith("{") && stringValue.endsWith("}")) {
                List<NodeUsage> nodeUsages = new ArrayList<>();
                int from = stringValue.indexOf('=');
                if (from >= 0) {
                    String description = "Cell type: " + type.getDisplayName(0);
                    nodeUsages.add(new SimpleNodeUsage(from, from, description, null, NodeType.OTHER));
                }

                List<NodeUsage> parsedNodeUsages = OpenLCellExpressionsCompiler.getNodeUsages(
                        (CompositeMethod) spreadsheetCell.getMethod(),
                        stringValue, from + 1
                );
                nodeUsages.addAll(parsedNodeUsages);

                return new CellMetaInfo(JavaOpenClass.STRING, false, nodeUsages);
            }
        }


        return null;
    }

    /**
     * Is invoked from binder
     */
    public void addHeaderMetaInfo(int row, int col, CellMetaInfo metaInfo) {
        headerMetaInfo.put(CellKey.CellKeyFactory.getCellKey(col, row), metaInfo);
    }
}
