package org.openl.util.formatters;

import org.openl.util.print.DefaultFormat;

/**
 * 
 * Wrapper to adapt {@link DefaultFormat} functionality to {@link IFormatter}.
 * Supports only format operation.
 * 
 * @author DLiauchuk
 * 
 */
public class FormatterAdapter implements IFormatter {

    public String format(Object obj) {
        StringBuilder buf = new StringBuilder();
        return DefaultFormat.format(obj, buf).toString();
    }

    public Object parse(String value) {
        throw new UnsupportedOperationException("Should not be called, this is only to implement IFormatter interface.");
    }

}
