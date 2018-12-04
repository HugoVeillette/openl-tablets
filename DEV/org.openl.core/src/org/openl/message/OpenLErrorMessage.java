package org.openl.message;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.openl.exception.OpenLCompilationException;
import org.openl.exception.OpenLException;
import org.openl.main.SourceCodeURLConstants;
import org.openl.main.SourceCodeURLTool;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.syntax.exception.SyntaxNodeException;
import org.openl.util.StringUtils;

/**
 * Class defines error OpenL message abstraction. <code>OpenLErrorMessage</code> encapsulates {@link IOpenLError} object
 * as source of message.
 * 
 */
public class OpenLErrorMessage extends OpenLMessage {

    private OpenLException error;

    public OpenLErrorMessage(String summary) {
        super(summary, Severity.ERROR);
    }

    public OpenLErrorMessage(OpenLException error) {
        super(getOpenLExceptionMessage(error), Severity.ERROR);
        if (error == null) {
            throw new NullPointerException();
        }
        this.error = error;
    }

    public OpenLException getError() {
        return error;
    }

    @Override
    public String toString() {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        printWriter.println(super.toString());

        if (getError() != null) {
            String url = SourceCodeURLTool.makeSourceLocationURL(getError().getLocation(),
                getError().getSourceModule());

            if (StringUtils.isNotEmpty(url)) {
                printWriter.println(SourceCodeURLConstants.AT_PREFIX + url);
            }
            if (getError().getCause() != null && getError().getLocation() == null) {
                getError().getCause().printStackTrace(printWriter);
            }
        }

        printWriter.close();

        return stringWriter.toString();
    }

    @Override
    public String getSourceLocation() {
        return SourceCodeURLTool.makeSourceLocationURL(error.getLocation(), error.getSourceModule());
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + ((error == null) ? 0
                                                   : ((error.getMessage() == null) ? 0
                                                                                   : error.getMessage().hashCode()));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        OpenLErrorMessage other = (OpenLErrorMessage) obj;
        if (error == null) {
            if (other.error != null)
                return false;
        } else if (other.error == null) {
            if (error != null)
                return false;
        }

        if (!StringUtils.equals(error.getMessage(), other.error.getMessage())) {
            return false;
        }

        if (error instanceof OpenLCompilationException && other.error instanceof OpenLCompilationException) {
            String location = ((OpenLCompilationException) error).getSourceLocation();
            String otherLocation = ((OpenLCompilationException) other.error).getSourceLocation();
            if (!StringUtils.equals(location, otherLocation)) {
                return false;
            }

        }
        
        return true;
    }

    private static String getOpenLExceptionMessage(OpenLException ex) {

        if (!(ex instanceof Throwable)) {
            return null;
        }

        Throwable t = (Throwable) ex;

        OpenLException cause = ex;
        while (t != null) {
            if (t instanceof OpenLException) {
                cause = (OpenLException) t;
            }
            t = t.getCause();
        }

        return cause.getMessage();
    }
}
