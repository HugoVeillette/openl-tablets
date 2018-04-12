package org.openl.rules.project.dependencies;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.message.Severity;
import org.openl.rules.project.instantiation.ProjectEngineFactory;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder;

/**
 * Check the case when main project includes 2 dependencies. Both dependencies
 * are containing the identical datatypes. Test that error about datatype
 * duplication will be thrown.
 * 
 * @author DLiauchuk
 *
 */
public class DuplicateDatatypesTest {

    @Test
    public void test() throws Exception {
        ProjectEngineFactory<?> factory = new SimpleProjectEngineFactoryBuilder()
            .setProject("test-resources/dependencies/testDuplicateDatatypes").build();
        CompiledOpenClass compiledOpenClass = factory.getCompiledOpenClass();
        assertTrue("Should be an error message, as there is datatype duplication", compiledOpenClass.hasErrors());
        OpenLMessage message = compiledOpenClass.getOpenLMessages().getMessages().iterator().next();
        assertEquals("Should be an error message", Severity.ERROR, message.getSeverity());
        assertEquals("The type TestType2 has been already defined.", message.getSummary());
    }
}
