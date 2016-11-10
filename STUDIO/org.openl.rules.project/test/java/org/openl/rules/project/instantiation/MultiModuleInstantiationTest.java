package org.openl.rules.project.instantiation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.openl.meta.DoubleValue;
import org.openl.rules.context.IRulesRuntimeContext;
import org.openl.rules.context.RulesRuntimeContextFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.RulesProjectResolver;

public class MultiModuleInstantiationTest {

    @Test
    public void test1() throws Exception {

        File root = new File("test/resources/multi-module-support/test1");
        RulesProjectResolver projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();

        List<Module> modules = new ArrayList<Module>();

        projectResolver.setWorkspace(root.getAbsolutePath());
        List<ProjectDescriptor> projects = projectResolver.listOpenLProjects();

        for (ProjectDescriptor project : projects) {
            modules.addAll(project.getModules());
        }

        SimpleMultiModuleInstantiationStrategy strategy = new SimpleMultiModuleInstantiationStrategy(modules, true);

        RuntimeContextInstantiationStrategyEnhancer enhancer = new RuntimeContextInstantiationStrategyEnhancer(strategy);

        Class<?> serviceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate();

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setLob("lob3");
        Method method = serviceClass.getMethod("driverRiskPremium", new Class<?>[] { IRulesRuntimeContext.class,
                String.class });
        Object result = method.invoke(instance, new Object[] { context, "High Risk Driver" });

        assertEquals(new DoubleValue(400), result);
    }

    private List<Module> listModulesInFolder(File folder) {
        List<Module> modules = new ArrayList<Module>();
        RulesProjectResolver projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        projectResolver.setWorkspace(folder.getAbsolutePath());
        List<ProjectDescriptor> projects = projectResolver.listOpenLProjects();
        for (ProjectDescriptor project : projects) {
            for (Module module : project.getModules()) {
                modules.add(module);
            }
        }
        return modules;
    }

    @Test
    public void test2() throws Exception {

        File root = new File("test/resources/multi-module-support/test2");

        SimpleMultiModuleInstantiationStrategy strategy = new SimpleMultiModuleInstantiationStrategy(
                listModulesInFolder(root), null, true);

        Class<?> serviceClass = strategy.getInstanceClass();
        Object instance = strategy.instantiate();

        Method method = serviceClass.getMethod("worldHello", new Class<?>[] { int.class });
        Object result = method.invoke(instance, new Object[] { 10 });
        assertEquals("World, Good Morning!", result);

        method = serviceClass.getMethod("helloWorld", new Class<?>[] { int.class });
        result = method.invoke(instance, new Object[] { 10 });
        assertEquals("Good Morning, World!", result);

        method = serviceClass.getMethod("getData1", new Class<?>[] {});
        result = method.invoke(instance, new Object[] {});
        assertEquals(2, ((Object[]) result).length);

        method = serviceClass.getMethod("getData2", new Class<?>[] {});
        result = method.invoke(instance, new Object[] {});
        assertEquals(3, ((Object[]) result).length);
    }

    public static interface MultimoduleInterface {
        String worldHello(int hour);

        String helloWorld(int hour);
    }

    @Test
    public void testServiceClass() throws Exception {
        File root = new File("test/resources/multi-module-support/test2");

        SimpleMultiModuleInstantiationStrategy strategy = new SimpleMultiModuleInstantiationStrategy(
                listModulesInFolder(root), null, true);
        strategy.setServiceClass(MultimoduleInterface.class);
        Object instantiate = strategy.instantiate();
        assertNotNull(instantiate);
        assertTrue(instantiate instanceof MultimoduleInterface);
    }

    @Test
    public void test3() throws Exception {

        File root = new File("test/resources/multi-module-support/test3");

        RulesProjectResolver projectResolver = RulesProjectResolver.loadProjectResolverFromClassPath();
        List<Module> modules = new ArrayList<Module>();

        projectResolver.setWorkspace(root.getAbsolutePath());
        List<ProjectDescriptor> projects = projectResolver.listOpenLProjects();

        for (ProjectDescriptor project : projects) {
            modules.addAll(project.getModules());
        }

        SimpleMultiModuleInstantiationStrategy strategy = new SimpleMultiModuleInstantiationStrategy(modules, true);

        RuntimeContextInstantiationStrategyEnhancer enhancer = new RuntimeContextInstantiationStrategyEnhancer(strategy);

        Class<?> serviceClass = enhancer.getServiceClass();
        Object instance = enhancer.instantiate();

        IRulesRuntimeContext context = RulesRuntimeContextFactory.buildRulesRuntimeContext();
        context.setLob("lob2");

        Method method = serviceClass.getMethod("hello", new Class<?>[] { IRulesRuntimeContext.class, int.class });
        Object result = method.invoke(instance, new Object[] { context, 10 });
        assertEquals("Good Morning, World!", result);

        method = serviceClass.getMethod("getData1", new Class<?>[] { IRulesRuntimeContext.class });
        result = method.invoke(instance, new Object[] { context });
        assertEquals(2, ((Object[]) result).length);

        method = serviceClass.getMethod("getData2", new Class<?>[] { IRulesRuntimeContext.class });
        result = method.invoke(instance, new Object[] { context });
        assertEquals(3, ((Object[]) result).length);

        context.setLob("lob3");

        method = serviceClass.getMethod("hello", new Class<?>[] { IRulesRuntimeContext.class, int.class });
        result = method.invoke(instance, new Object[] { context, 10 });
        assertEquals("World, Good Morning!", result);

        method = serviceClass.getMethod("getData1", new Class<?>[] { IRulesRuntimeContext.class });
        result = method.invoke(instance, new Object[] { context });
        assertEquals(2, ((Object[]) result).length);

        method = serviceClass.getMethod("getData2", new Class<?>[] { IRulesRuntimeContext.class });
        result = method.invoke(instance, new Object[] { context });
        assertEquals(3, ((Object[]) result).length);

        context.setLob("lob1");

        method = serviceClass.getMethod("hello", new Class<?>[] { IRulesRuntimeContext.class, int.class });
        result = method.invoke(instance, new Object[] { context, 10 });
        assertEquals("Good Morning", result);

        method = serviceClass.getMethod("getData1", new Class<?>[] { IRulesRuntimeContext.class });
        result = method.invoke(instance, new Object[] { context });
        assertEquals(2, ((Object[]) result).length);

        method = serviceClass.getMethod("getData2", new Class<?>[] { IRulesRuntimeContext.class });
        result = method.invoke(instance, new Object[] { context });
        assertEquals(3, ((Object[]) result).length);
    }

}
