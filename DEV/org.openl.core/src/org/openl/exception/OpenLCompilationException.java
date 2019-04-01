package org.openl.exception;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.openl.main.SourceCodeURLTool;
import org.openl.source.IOpenSourceCodeModule;
import org.openl.syntax.exception.CompositeSyntaxNodeException;
import org.openl.util.text.ILocation;

public class OpenLCompilationException extends Exception implements OpenLException {

    private static final long serialVersionUID = -8075090606797764194L;

    private Throwable insideCause;
    private ILocation location;
    private IOpenSourceCodeModule source;

    public OpenLCompilationException(String message,
            Throwable insideCause,
            ILocation location,
            IOpenSourceCodeModule source) {
        super(message);
        this.insideCause = insideCause;
        this.location = location;
        this.source = source;
    }

    public OpenLCompilationException(String message, Throwable cause, ILocation location) {
        this(message, cause, location, null);
    }

    public OpenLCompilationException(String message, Throwable cause) {
        this(message, cause, null, null);
    }

    public OpenLCompilationException(String message) {
        this(message, null, null, null);
    }

    public Throwable getOriginalCause() {

        Throwable rootCause = ExceptionUtils.getRootCause(insideCause);

        if (rootCause != null) {
            return rootCause;
        }

        return insideCause;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.exception.OpenLException#getLocation()
     */
    @Override
    public ILocation getLocation() {
        return location;
    }

    /*
     * (non-Javadoc)
     *
     * @see org.openl.exception.OpenLException#getSourceModule()
     */
    @Override
    public IOpenSourceCodeModule getSourceModule() {
        return source;
    }

    @Override
    public Throwable getCause() {
        return getOriginalCause();
    }

    public String getSourceLocation() {
        return SourceCodeURLTool.makeSourceLocationURL(getLocation(), getSourceModule());
    }

    @Override
    public String toString() {

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);

        printError(this, printWriter);
        SourceCodeURLTool.printSourceLocation(getLocation(), getSourceModule(), printWriter);
        printWriter.close();

        return stringWriter.toString();
    }

    private static void printError(OpenLException error, PrintWriter writer) {

        Throwable cause = error.getCause();

        String message;

        if (cause instanceof CompositeSyntaxNodeException) {

            CompositeSyntaxNodeException syntaxErrorException = (CompositeSyntaxNodeException) cause;

            for (int i = 0; i < syntaxErrorException.getErrors().length; i++) {
                printError(syntaxErrorException.getErrors()[i], writer);
            }

            return;

        } else {
            message = error.getMessage();
        }

        writer.println("Error: " + message);

        SourceCodeURLTool.printCodeAndError(error.getLocation(), error.getSourceModule(), writer);
        SourceCodeURLTool.printSourceLocation(error.getLocation(), error.getSourceModule(), writer);

        if (error.getCause() != null) {
            error.getCause().printStackTrace(writer);
        }
    }
}
