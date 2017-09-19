package org.openl.rules.table.ui;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.VerticalAlignment;

/**
 * @author snshor
 *
 */
public class CellStyle implements ICellStyle {

    private HorizontalAlignment horizontalAlignment = HorizontalAlignment.GENERAL;

    private VerticalAlignment verticalAlignment = VerticalAlignment.TOP;

    private short[] fillBackgroundColor;

    private short[] fillForegroundColor;

    private short fillBackgroundColorIndex;

    private short fillForegroundColorIndex;

    private FillPatternType fillPattern;

    private BorderStyle[] borderStyle;

    private short[][] borderRGB;
    private int ident;

    private boolean wrappedText;

    private int rotation;

    public CellStyle(ICellStyle cellStyle) {
        if (cellStyle == null) {
            return;
        }

        horizontalAlignment = cellStyle.getHorizontalAlignment();

        verticalAlignment = cellStyle.getVerticalAlignment();

        fillBackgroundColor = cellStyle.getFillBackgroundColor();
        fillForegroundColor = cellStyle.getFillForegroundColor();

        fillBackgroundColorIndex = cellStyle.getFillBackgroundColorIndex();
        fillForegroundColorIndex = cellStyle.getFillForegroundColorIndex();
        fillPattern = cellStyle.getFillPattern();

        borderStyle = cellStyle.getBorderStyle();
        borderRGB = cellStyle.getBorderRGB();

        ident = cellStyle.getIdent();

        wrappedText = cellStyle.isWrappedText();

        rotation = cellStyle.getRotation();
    }

    public short[][] getBorderRGB() {
        return borderRGB;
    }

    public BorderStyle[] getBorderStyle() {
        return borderStyle;
    }

    public short[] getFillBackgroundColor() {
        return fillBackgroundColor;
    }

    public short[] getFillForegroundColor() {
        return fillForegroundColor;
    }

    public HorizontalAlignment getHorizontalAlignment() {
        return horizontalAlignment;
    }

    public int getIdent() {
        return ident;
    }

    public int getRotation() {
        return rotation;
    }

    public VerticalAlignment getVerticalAlignment() {
        return verticalAlignment;
    }

    public boolean isWrappedText() {
        return wrappedText;
    }

    public void setBorderRGB(short[][] borderRGB) {
        this.borderRGB = borderRGB;
    }

    public void setBorderStyle(BorderStyle[] borderStyle) {
        this.borderStyle = borderStyle;
    }

    public void setFillBackgroundColor(short[] fillBackgroundColor) {
        this.fillBackgroundColor = fillBackgroundColor;
    }

    public void setFillForegroundColor(short[] fillForegroundColor) {
        this.fillForegroundColor = fillForegroundColor;
    }

    public short getFillBackgroundColorIndex() {
        return fillBackgroundColorIndex;
    }

    public short getFillForegroundColorIndex() {
        return fillForegroundColorIndex;
    }

    public FillPatternType getFillPattern() {
        return fillPattern;
    }

}