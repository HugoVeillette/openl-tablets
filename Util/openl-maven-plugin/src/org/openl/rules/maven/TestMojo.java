package org.openl.rules.maven;

import static org.openl.rules.testmethod.TestUnitResultComparator.TestStatus.TR_NEQ;
import static org.openl.rules.testmethod.TestUnitResultComparator.TestStatus.TR_OK;

import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;

import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.openl.CompiledOpenClass;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.testmethod.*;
import org.openl.types.IOpenClass;

/**
 * Run OpenL tests
 *
 * @author Yury Molchan
 */
@Mojo(name = "test", defaultPhase = LifecyclePhase.TEST, requiresDependencyResolution = ResolutionScope.TEST)
public final class TestMojo extends BaseOpenLMojo {
    private static final String FAILURE = "<<< FAILURE!";
    private static final String ERROR = "<<< ERROR!";
    /**
     * Set this to 'true' to skip running OpenL tests.
     */
    @Parameter(property = "skipTests")
    private boolean skipTests;

    @Parameter(defaultValue = "${project.testClasspathElements}", readonly = true, required = true)
    private List<String> classpath;

    @Override
    public void execute(String sourcePath) throws Exception {
        URL[] urls = toURLs(classpath);
        ClassLoader classLoader = new URLClassLoader(urls, SimpleProjectEngineFactory.class.getClassLoader());

        SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object>();
        SimpleProjectEngineFactory<?> factory = builder.setProject(sourcePath)
            .setClassLoader(classLoader)
            .setExecutionMode(false)
            .build();

        CompiledOpenClass openLRules = factory.getCompiledOpenClass();
        IOpenClass openClass = openLRules.getOpenClassWithErrors();

        int runTests = 0;
        int failedTests = 0;
        int errors = 0;

        List<String> summaryFailures = new ArrayList<>();
        List<String> summaryErrors = new ArrayList<>();

        TestSuiteMethod[] tests = ProjectHelper.allTesters(openClass);
        for (TestSuiteMethod test : tests) {
            String moduleName = test.getModuleName();
            try {
                info("");
                String moduleInfo = moduleName == null ? "" : " from the module " + moduleName;
                info("Running ", test.getName(), moduleInfo);
                TestUnitsResults result = new TestSuite(test).invokeSequentially(openClass, 1L);

                int suitTests = result.getNumberOfTestUnits();
                int suitFailures = result.getNumberOfAssertionFailures();
                int suitErrors = result.getNumberOfErrors();
                long time = result.getExecutionTime();

                info("Tests run: ", suitTests,
                        ", Failures: ", suitFailures,
                        ", Errors: ", suitErrors,
                        ". Time elapsed: ", time + " nano sec. ",
                        result.getNumberOfFailures() > 0 ? FAILURE : "");

                if (result.getNumberOfFailures() > 0) {
                    showFailures(test, result, summaryFailures, summaryErrors);
                }

                runTests += suitTests;
                failedTests += suitFailures;
                errors += suitErrors;
            } catch (Exception e) {
                error(e);
                errors++;
                String modulePrefix = moduleName == null ? "" : moduleName + ".";
                summaryErrors.add(modulePrefix + test.getName() + e.getClass().getName());
            }
        }

        info("");
        info("Results:");
        if (failedTests > 0) {
            info("");
            info("Failed Tests:");
            for (String failure : summaryFailures) {
                info("  ", failure);
            }
        }
        if (errors > 0) {
            info("");
            info("Tests in error:");
            for (String error : summaryErrors) {
                info("  ", error);
            }
        }

        info("");
        info("Total tests run: ", runTests, ", Failures: ", failedTests, ", Errors: ", errors);
        info("");
        if (failedTests > 0 || errors > 0) {
            throw new MojoFailureException("There are errors in the OpenL tests");
        } else if (openLRules.hasErrors()) {
            throw new MojoFailureException("There are compilation errors in the OpenL tests ");
        }
    }

    private void showFailures(TestSuiteMethod test, TestUnitsResults result, List<String> summaryFailures, List<String> summaryErrors) {
        int num = 1;
        String moduleName = test.getModuleName();
        String modulePrefix = moduleName == null ? "" : moduleName + ".";

        for (TestUnit testUnit : result.getTestUnits()) {
            int status = testUnit.compareResult();
            if (status != TR_OK.getStatus()) {
                String failureType = status == TR_NEQ.getStatus() ? FAILURE : ERROR;
                String description = testUnit.getDescription();

                info("  Test case: #", num,
                        TestUnit.DEFAULT_DESCRIPTION.equals(description) ? "" : " (" + description + ")",
                        ". Time elapsed: ", testUnit.getExecutionTime(), " nano sec. ",
                        failureType);

                if (status == TR_NEQ.getStatus()) {
                    info("    Expected: <", testUnit.getExpectedResult(),
                            "> but was: <", testUnit.getActualResult() + ">");
                    summaryFailures.add(modulePrefix + test.getName() + "#" + num +
                            " expected: <" + testUnit.getExpectedResult() +
                            "> but was <" + testUnit.getActualResult() + ">");
                } else {
                    Throwable error = (Throwable) testUnit.getActualResult();
                    info("  Error: ", error, "\n");
                    summaryErrors.add(modulePrefix + test.getName() + "#" + num + " " +
                            error.getClass().getName());
                }
            }
            num++;
        }
    }

    @Override
    String getHeader() {
        return "OPENL TESTS";
    }

    @Override
    boolean isDisabled() {
        return skipTests;
    }

}
