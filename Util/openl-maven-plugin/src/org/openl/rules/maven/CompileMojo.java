package org.openl.rules.maven;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;

import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.openl.CompiledOpenClass;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.rules.project.instantiation.RulesInstantiationException;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectResolver;
import org.openl.rules.project.resolving.ProjectResolvingException;
import org.openl.types.IOpenClass;

/**
 * Compile and validate OpenL project
 * 
 * @author Yury Molchan
 */
@Mojo(name = "compile", defaultPhase = LifecyclePhase.COMPILE, requiresDependencyResolution = ResolutionScope.COMPILE)
public final class CompileMojo extends BaseOpenLMojo {
    /**
     * Compile the project in Single module mode.
     * If true each module will be compiled in sequence. Needed for big projects.
     * If false all modules will be compiled at once.
     * By default false.
     */
    @Parameter(defaultValue = "false")
    private boolean singleModuleMode;

    /**
     * If you want to override some parameters, define them here.
     */
    @Parameter
    private Map<String, Object> externalParameters;

    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    private List<String> classpath;

    @Override
    public void execute(String sourcePath, boolean hasDependencies) throws Exception {
        if (singleModuleMode) {
            executeModuleByModule(sourcePath, hasDependencies);
        } else {
            executeAllAtOnce(sourcePath, hasDependencies);
        }
    }

    private void executeAllAtOnce(String sourcePath, boolean hasDependencies) throws
                                                     MalformedURLException,
                                                     RulesInstantiationException,
                                                     ProjectResolvingException,
                                                     ClassNotFoundException {
        URL[] urls = toURLs(classpath);
        ClassLoader classLoader = new URLClassLoader(urls, SimpleProjectEngineFactory.class.getClassLoader());

        SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object>();
        if (hasDependencies) {
            builder.setWorkspace(workspaceFolder.getPath());
        }
        SimpleProjectEngineFactory<?> factory = builder.setProject(sourcePath)
                .setClassLoader(classLoader)
                .setExecutionMode(true)
                .setExternalParameters(externalParameters)
                .build();

        CompiledOpenClass openLRules = factory.getCompiledOpenClass();
        IOpenClass openClass = openLRules.getOpenClass();
        List<OpenLMessage> messages = openLRules.getMessages();
        List<OpenLMessage> warnings = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.WARN);
        info("Compilation has finished.");
        info("DataTypes: " + openClass.getTypes().size());
        info("Methods  : " + openClass.getMethods().size());
        info("Fields   : " + openClass.getFields().size());
        info("Warnings : " + warnings.size());
    }

    private void executeModuleByModule(String sourcePath, boolean hasDependencies) throws
                                                          ProjectResolvingException,
                                                          MalformedURLException,
                                                          ClassNotFoundException,
                                                          RulesInstantiationException {
        ProjectDescriptor pd = ProjectResolver.instance().resolve(new File(sourcePath));
        if (pd == null) {
            throw new ProjectResolvingException("Failed to resolve project. Defined location is not an OpenL project.");
        }

        List<Module> modules = pd.getModules();

        URL[] urls = toURLs(classpath);
        ClassLoader classLoader = new URLClassLoader(urls, SimpleProjectEngineFactory.class.getClassLoader());
        for (Module module : modules) {
            info("");
            info("Compiling the module '" + module.getName() + "'...");

            SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object>();
            if (hasDependencies) {
                builder.setWorkspace(workspaceFolder.getPath());
            }
            SimpleProjectEngineFactory<?> factory = builder.setProject(sourcePath)
                    .setClassLoader(classLoader)
                    .setExecutionMode(true)
                    .setModule(module.getName())
                    .setExternalParameters(externalParameters)
                    .build();

            CompiledOpenClass openLRules = factory.getCompiledOpenClass();
            IOpenClass openClass = openLRules.getOpenClass();
            List<OpenLMessage> messages = openLRules.getMessages();
            List<OpenLMessage> warnings = OpenLMessagesUtils.filterMessagesBySeverity(messages, Severity.WARN);
            info("Compilation of the module '" + module.getName() + "' has finished.");
            info("DataTypes: " + openClass.getTypes().size());
            info("Methods  : " + openClass.getMethods().size());
            info("Fields   : " + openClass.getFields().size());
            info("Warnings : " + warnings.size());
        }
    }

    @Override
    String getHeader() {
        return "OPENL COMPILATION";
    }
}
