package org.openl.rules.maven;

import static org.openl.rules.testmethod.TestUnitResultComparator.TestStatus.TR_NEQ;
import static org.openl.rules.testmethod.TestUnitResultComparator.TestStatus.TR_OK;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.openl.CompiledOpenClass;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
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

    /**
     * Thread count to run test cases. The parameter is as follows:
     * <ul>
     *     <li>4 - Runs tests with 4 threads</li>
     *     <li>1.5C - 1.5 thread per cpu core</li>
     *     <li>none - Run tests sequentially (don't create threads to run tests)</li>
     *     <li>auto - Threads count will be configured automatically</li>
     * </ul>
     * Default value is "auto".
     */
    @Parameter(defaultValue = "auto")
    private String threadCount;

    /**
     * Compile the project in Single module mode.
     * If true each module will be compiled in sequence and tests from that module will be run. Needed for big projects.
     * If false all modules will be compiled at once and all tests from all modules will be run.
     * By default false.
     */
    @Parameter(defaultValue = "false")
    private boolean singleModuleMode;

    @Parameter(defaultValue = "${project.testClasspathElements}", readonly = true, required = true)
    private List<String> classpath;

    @Override
    public void execute(String sourcePath) throws Exception {
        Summary summary = singleModuleMode ? executeModuleByModule(sourcePath) : executeAllAtOnce(sourcePath);

        info("");
        info("Results:");
        if (summary.getFailedTests() > 0) {
            info("");
            info("Failed Tests:");
            for (String failure : summary.getSummaryFailures()) {
                info("  ", failure);
            }
        }
        if (summary.getErrors() > 0) {
            info("");
            info("Tests in error:");
            for (String error : summary.getSummaryErrors()) {
                info("  ", error);
            }
        }

        info("");
        info("Total tests run: ", summary.getRunTests(), ", Failures: ", summary.getFailedTests(), ", Errors: ", summary.getErrors());
        info("");
        if (summary.getFailedTests() > 0 || summary.getErrors() > 0) {
            throw new MojoFailureException("There are errors in the OpenL tests");
        } else if (summary.isHasCompilationErrors()) {
            throw new MojoFailureException("There are compilation errors in the OpenL tests ");
        }
    }

    private Summary executeAllAtOnce(String sourcePath) throws
                                                        MalformedURLException,
                                                        RulesInstantiationException,
                                                        ProjectResolvingException,
                                                        ClassNotFoundException {
        URL[] urls = toURLs(classpath);
        ClassLoader classLoader = new URLClassLoader(urls, SimpleProjectEngineFactory.class.getClassLoader());

        SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object>();
        SimpleProjectEngineFactory<?> factory = builder.setProject(sourcePath)
                .setClassLoader(classLoader)
                .setExecutionMode(false)
                .build();

        CompiledOpenClass openLRules = factory.getCompiledOpenClass();
        return executeTests(openLRules);
    }

    private Summary executeModuleByModule(String sourcePath) throws
                                                             MalformedURLException,
                                                             RulesInstantiationException,
                                                             ProjectResolvingException,
                                                             ClassNotFoundException {
        ProjectDescriptor pd = ProjectResolver.instance().resolve(new File(sourcePath));
        if (pd == null) {
            throw new ProjectResolvingException("Failed to resolve project. Defined location is not an OpenL project.");
        }

        int runTests = 0;
        int failedTests = 0;
        int errors = 0;

        List<String> summaryFailures = new ArrayList<>();
        List<String> summaryErrors = new ArrayList<>();
        boolean hasCompilationErrors = false;

        List<Module> modules = pd.getModules();

        for (Module module : modules) {
            URL[] urls = toURLs(classpath);
            ClassLoader classLoader = new URLClassLoader(urls, SimpleProjectEngineFactory.class.getClassLoader());

            SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object>();
            SimpleProjectEngineFactory<?> factory = builder.setProject(sourcePath)
                    .setClassLoader(classLoader)
                    .setExecutionMode(false)
                    .setModule(module.getName())
                    .build();

            CompiledOpenClass openLRules = factory.getCompiledOpenClass();
            Summary summary = executeTests(openLRules);

            runTests += summary.getRunTests();
            failedTests += summary.getFailedTests();
            errors += summary.getErrors();
            summaryFailures.addAll(summary.getSummaryFailures());
            summaryErrors.addAll(summary.getSummaryErrors());
            hasCompilationErrors |= summary.isHasCompilationErrors();
        }

        return new Summary(runTests, failedTests, errors, summaryFailures, summaryErrors, hasCompilationErrors);
    }

    private Summary executeTests(CompiledOpenClass openLRules) {
        IOpenClass openClass = openLRules.getOpenClassWithErrors();

        int runTests = 0;
        int failedTests = 0;
        int errors = 0;

        List<String> summaryFailures = new ArrayList<>();
        List<String> summaryErrors = new ArrayList<>();

        TestSuiteExecutor testSuiteExecutor = getTestSuiteExecutor();

        TestSuiteMethod[] tests = ProjectHelper.allTesters(openClass);
        for (TestSuiteMethod test : tests) {
            String moduleName = test.getModuleName();
            try {
                info("");
                String moduleInfo = moduleName == null ? "" : " from the module " + moduleName;
                info("Running ", test.getName(), moduleInfo);
                TestUnitsResults result;
                if (testSuiteExecutor == null) {
                    result = new TestSuite(test).invokeSequentially(openClass, 1L);
                } else {
                    result = new TestSuite(test).invokeParallel(testSuiteExecutor, openClass, 1L);
                }

                int suitTests = result.getNumberOfTestUnits();
                int suitFailures = result.getNumberOfAssertionFailures();
                int suitErrors = result.getNumberOfErrors();

                info("Tests run: ", suitTests,
                        ", Failures: ", suitFailures,
                        ", Errors: ", suitErrors,
                        ". Time elapsed: ", formatTime(result.getExecutionTime()), " sec.",
                        result.getNumberOfFailures() > 0 ? " " + FAILURE : "");

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
                Throwable cause = ExceptionUtils.getRootCause(e);
                if (cause == null) {
                    cause = e;
                }
                summaryErrors.add(modulePrefix + test.getName() + cause.getClass().getName());
            }
        }

        return new Summary(runTests, failedTests, errors, summaryFailures, summaryErrors, openLRules.hasErrors());
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
                        ". Time elapsed: ", formatTime(testUnit.getExecutionTime()), " sec. ",
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
                    Throwable cause = ExceptionUtils.getRootCause(error);
                    if (cause == null) {
                        cause = error;
                    }
                    summaryErrors.add(modulePrefix + test.getName() + "#" + num + " " + cause.getClass().getName());
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

    private String formatTime(long nanoseconds) {
        double time = (double) nanoseconds / 1000_000_000;
        DecimalFormat df = (DecimalFormat) NumberFormat.getNumberInstance(Locale.US);
        df.applyPattern("#.###");
        return time < 0.001 ? "< 0.001" : df.format(time);
    }

    private TestSuiteExecutor getTestSuiteExecutor() {
        int threads;

        switch (threadCount) {
            case "none":
                return null;
            case "auto":
                // Can be changed in the future
                threads = Runtime.getRuntime().availableProcessors() + 2;
                break;
            default:
                if (threadCount.matches("\\d+")) {
                    threads = Integer.parseInt(threadCount);
                } else if (threadCount.matches("\\d[\\d.]*[cC]")) {
                    float multiplier = Float.parseFloat(threadCount.substring(0, threadCount.length() - 1));
                    threads = (int) (multiplier * Runtime.getRuntime().availableProcessors());
                } else {
                    throw new IllegalArgumentException("Incorrect thread count '" + threadCount + "'");
                }
                break;
        }
        info("Run tests using ", threads, " threads.");

        return new TestSuiteExecutor(threads);
    }

    private static class Summary {
        private final int runTests;
        private final int failedTests;
        private final int errors;
        private final List<String> summaryFailures;
        private final List<String> summaryErrors;
        private final boolean hasCompilationErrors;

        public Summary(int runTests,
                int failedTests,
                int errors,
                List<String> summaryFailures,
                List<String> summaryErrors, boolean hasCompilationErrors) {
            this.runTests = runTests;
            this.failedTests = failedTests;
            this.errors = errors;
            this.summaryFailures = Collections.unmodifiableList(summaryFailures);
            this.summaryErrors = Collections.unmodifiableList(summaryErrors);
            this.hasCompilationErrors = hasCompilationErrors;
        }

        public int getRunTests() {
            return runTests;
        }

        public int getFailedTests() {
            return failedTests;
        }

        public int getErrors() {
            return errors;
        }

        public List<String> getSummaryFailures() {
            return summaryFailures;
        }

        public List<String> getSummaryErrors() {
            return summaryErrors;
        }

        public boolean isHasCompilationErrors() {
            return hasCompilationErrors;
        }
    }
}
