package org.openl.core;

import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.FileInputStream;
import java.util.Locale;
import java.util.TimeZone;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.rules.runtime.RulesEngineFactory;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.testmethod.TestUnitsResults;
import org.openl.rules.vm.SimpleRulesVM;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.vm.IRuntimeEnv;

public class AllExcelTest {
    public static final String DIR = "test-resources/functionality/";
    private Locale defaultLocale;
    private TimeZone defaultTimeZone;

    @Before
    public void setupLocale() {
        defaultLocale = Locale.getDefault();
        defaultTimeZone = TimeZone.getDefault();
        Locale.setDefault(Locale.US);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @After
    public void restoreLocale() {
        Locale.setDefault(defaultLocale);
        TimeZone.setDefault(defaultTimeZone);
    }

    @Test
    public void testAllExcelFiles() throws NoSuchMethodException {
        boolean hasErrors = false;
        final File sourceDir = new File(DIR);
        final String[] files = sourceDir.list();
        for (String sourceFile : files) {

            try {
                new FileInputStream(new File(sourceDir, sourceFile)).close();
            } catch (Exception ex) {
                System.out.println("!!! Cannot read file [" + sourceFile + "]. Because: " + ex.getMessage());
                hasErrors = true;
                continue;
            }

            RulesEngineFactory<?> engineFactory = new RulesEngineFactory<Object>(DIR + sourceFile);
            engineFactory.setExecutionMode(false);
            IRuntimeEnv env = new SimpleRulesVM().getRuntimeEnv();
            final CompiledOpenClass compiledOpenClass = engineFactory.getCompiledOpenClass();

            if (compiledOpenClass.hasErrors()) {
                System.out.println("!!! Compilation errors in [" + sourceFile + "].");
                System.out.println(compiledOpenClass.getMessages());
                hasErrors = true;
                continue;
            }

            IOpenClass openClass = compiledOpenClass.getOpenClass();
            Object target = openClass.newInstance(env);
            int errors = 0;
            for (IOpenMethod method : openClass.getDeclaredMethods()) {
                if (method instanceof TestSuiteMethod) {
                    TestUnitsResults res = (TestUnitsResults) method.invoke(target, new Object[0], env);
                    final int numberOfFailures = res.getNumberOfFailures();
                    errors += numberOfFailures;
                    if (numberOfFailures != 0) {
                        System.out.println("!!! Errors in [" + sourceFile + "]. Failed test: " + res
                            .getName() + "  Errors #:" + numberOfFailures);
                    }

                }
            }
            if (errors != 0) {
                hasErrors = true;
                System.out.println("!!! Errors in [" + sourceFile + "]. Total failures #: " + errors);
            } else {
                System.out.println("+++ OK in [" + sourceFile + "]. ");
            }
        }
        assertFalse("Failed test", hasErrors);
    }
}
