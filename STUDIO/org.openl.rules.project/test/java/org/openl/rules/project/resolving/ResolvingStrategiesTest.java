package org.openl.rules.project.resolving;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ModuleType;
import org.openl.rules.project.model.ProjectDescriptor;

public class ResolvingStrategiesTest {

    @Test
    public void testDescriptor() throws Exception{
        ProjectDescriptorBasedResolvingStrategy strategy = new ProjectDescriptorBasedResolvingStrategy();
        File projectFolder = new File("test/resources/descriptor");
        assertTrue(strategy.isRulesProject(projectFolder));

        ProjectDescriptor descriptor = strategy.resolveProject(projectFolder);
        assertNotNull(descriptor);

        assertEquals(1, descriptor.getModules().size());
        assertEquals(1, descriptor.getClasspath().size());

        File nonProjectFolder = new File("test/resources");
        assertFalse(strategy.isRulesProject(nonProjectFolder));
    }

    @Test
    public void testSimple() throws Exception{
        ResolvingStrategy resolvingStrategy = new SimpleXlsResolvingStrategy();
        File projectFolder = new File("test/resources/excel/");
        assertTrue(resolvingStrategy.isRulesProject(projectFolder));
        ProjectDescriptor descriptor = resolvingStrategy.resolveProject(projectFolder);
        assertEquals(projectFolder.getName(), descriptor.getName());
        assertEquals(projectFolder.getCanonicalPath(), descriptor.getProjectFolder().getCanonicalPath());
        assertEquals(2, descriptor.getModules().size());
        Module moduleFirst = descriptor.getModules().get(0);
        assertEquals("Rules", moduleFirst.getName());
        assertEquals(ModuleType.API, moduleFirst.getType());
        Module moduleSecond = descriptor.getModules().get(1);
        assertEquals("Rules2", moduleSecond.getName());
        assertEquals(ModuleType.API, moduleSecond.getType());
    }

}
