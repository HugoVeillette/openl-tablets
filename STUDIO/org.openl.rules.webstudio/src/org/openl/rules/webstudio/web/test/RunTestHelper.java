package org.openl.rules.webstudio.web.test;

import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;

import org.openl.commons.web.jsf.FacesUtils;
import org.openl.rules.data.IDataBase;
import org.openl.rules.table.IOpenLTable;
import org.openl.rules.testmethod.TestDescription;
import org.openl.rules.testmethod.TestSuite;
import org.openl.rules.testmethod.TestSuiteMethod;
import org.openl.rules.types.OpenMethodDispatcher;
import org.openl.rules.ui.ProjectModel;
import org.openl.rules.ui.TraceHelper;
import org.openl.rules.webstudio.web.trace.TreeBuildTracer;
import org.openl.rules.webstudio.web.trace.node.ITracerObject;
import org.openl.rules.webstudio.web.util.Constants;
import org.openl.rules.webstudio.web.util.WebStudioUtils;
import org.openl.types.IOpenMethod;

@ManagedBean
@SessionScoped
public final class RunTestHelper {

    // FIXME last parameters of the test suite should have temporary
    // location(such as Flash scope)
    // but now it placed to session bean due to WebStudio navigation specific
    // TODO move this object to the correct place
    private Object[] params = new Object[0];

    public ITracerObject getTraceObject() {
        catchParams();
        TestSuite testSuite = getTestSuite();
        ProjectModel model = WebStudioUtils.getProjectModel();
        ITracerObject t;
        try {
            t = TreeBuildTracer.initialize();
            model.traceElement(testSuite);
        } finally {
            TreeBuildTracer.destroy();
        }

        return t;
    }

    public void initTrace() {
        ITracerObject root = getTraceObject();

        TraceHelper traceHelper = WebStudioUtils.getTraceHelper();
        traceHelper.cacheTraceTree(root);// Register
    }

    public void catchParams() {
        this.params = ((InputArgsBean) FacesUtils.getBackingBean("inputArgsBean")).getParams();
    }

    public void catchParamsToDownload() {
        catchParams();
        Utils.saveTestToSession(FacesUtils.getSession(), getTestSuite());
    }

    public TestSuite getTestSuite() {
        String id = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_ID);

        ProjectModel model = WebStudioUtils.getProjectModel();
        IOpenLTable table = model.getTableById(id);
        String uri = table.getUri();
        IOpenMethod method = model.getMethod(uri);
        if (method instanceof OpenMethodDispatcher) {
            method = model.getCurrentDispatcherMethod(method, uri);
        }

        IDataBase db = Utils.getDb(model);

        TestSuite testSuite;
        if (method instanceof TestSuiteMethod) {
            TestSuiteMethod testSuiteMethod = (TestSuiteMethod) method;

            String testRanges = FacesUtils.getRequestParameter(Constants.REQUEST_PARAM_TEST_RANGES);
            if (testRanges == null) {
                // Run all test cases of selected test suite
                testSuite = new TestSuiteWithPreview(db, testSuiteMethod);
            } else {
                // Run only selected test cases of selected test suite
                int[] indices = testSuiteMethod.getIndices(testRanges);
                testSuite = new TestSuiteWithPreview(db, testSuiteMethod, indices);
            }
        } else {
            if (method.getSignature().getNumberOfParameters() > params.length) {
                // View expired
                return null;
            }
            TestDescription testDescription = new TestDescription(method, params);
            testSuite = new TestSuiteWithPreview(db, testDescription);
        }

        params = new Object[0]; // Reset caught params
        return testSuite;
    }
}
