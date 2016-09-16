package org.openl.rules.maven;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.maven.plugin.logging.Log;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.resource.loader.StringResourceLoader;
import org.apache.velocity.runtime.resource.util.StringResourceRepository;
import org.openl.CompiledOpenClass;
import org.openl.classloader.SimpleBundleClassLoader;
import org.openl.conf.ClassLoaderFactory;
import org.openl.conf.UserContext;
import org.openl.dependency.IDependencyManager;
import org.openl.impl.OpenClassJavaWrapper;
import org.openl.main.OpenLProjectPropertiesLoader;
import org.openl.message.OpenLErrorMessage;
import org.openl.message.OpenLMessage;
import org.openl.message.OpenLMessagesUtils;
import org.openl.message.Severity;
import org.openl.meta.IVocabulary;
import org.openl.rules.project.ProjectDescriptorManager;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.rules.project.model.Module;
import org.openl.rules.project.model.PathEntry;
import org.openl.rules.project.model.ProjectDescriptor;
import org.openl.rules.project.resolving.ProjectDescriptorBasedResolvingStrategy;
import org.openl.rules.testmethod.ProjectHelper;
import org.openl.types.IOpenClass;
import org.openl.types.IOpenMethod;
import org.openl.types.impl.DomainOpenClass;
import org.openl.util.FileTool;
import org.openl.util.IOUtils;
import org.openl.util.StringTool;
import org.openl.util.StringUtils;
import org.openl.util.generation.SimpleBeanJavaGenerator;

public class GenerateInterface {
    public static final String GOAL_MAKE_WRAPPER = "make wrapper";
    public static final String GOAL_UPDATE_PROPERTIES = "update properties";
    public static final String GOAL_MAKE_WEBINF = "make WEB-INF";
    public static final String GOAL_ALL = "all";
    public static final String GOAL_GENERATE_DATATYPES = "generate datatypes";
    public static final int MSG_ERR = 0;
    public static final int MSG_WARN = 1;
    public static final int MSG_INFO = 2;
    public static final int MSG_VERBOSE = 3;
    public static final int MSG_DEBUG = 4;
    private static final String DEFAULT_CLASSPATH = "./bin";
    private Log log;
    private String testSourceDirectory;

    private Boolean generateUnitTests;
    private String unitTestTemplatePath;
    private Boolean overwriteUnitTests;
    private boolean ignoreTestMethods = true;
    private String defaultProjectName;
    private String[] defaultClasspaths = { GenerateInterface.DEFAULT_CLASSPATH };
    private boolean createProjectDescriptor = true;

    public GenerateInterface() {
        // TODO setGoal() should be refactored: now it's usage is inconvenient
        // and unclear.
        // For interface generation only "generate datatypes" goal is needed
        // Can be overridden in maven configuration
        setGoal(GOAL_GENERATE_DATATYPES);
        setIgnoreTestMethods(true);
    }

    public void setGenerateUnitTests(Boolean generateUnitTests) {
        this.generateUnitTests = generateUnitTests;
    }

    public Boolean getGenerateUnitTests() {
        return generateUnitTests;
    }

    public void setUnitTestTemplatePath(String unitTestTemplatePath) {
        this.unitTestTemplatePath = unitTestTemplatePath;
    }

    public String getUnitTestTemplatePath() {
        return unitTestTemplatePath;
    }

    public Boolean getOverwriteUnitTests() {
        return overwriteUnitTests;
    }

    public void setOverwriteUnitTests(Boolean overwriteUnitTests) {
        this.overwriteUnitTests = overwriteUnitTests;
    }

    public void setTestSourceDirectory(String testSourceDirectory) {
        this.testSourceDirectory = testSourceDirectory;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    protected void writeSpecific() throws Exception {
        if (createProjectDescriptor && getSrcFile() != null) {
            writeRulesXML();
        }
        if (generateUnitTests && getTargetClass() != null) {
            generateTests();
        }
    }

    private void generateTests() throws Exception {
        if (getOpenClass() == null) {
            setOpenClass(makeOpenClass());
        }
        if (log.isInfoEnabled()) {
            log.info(String.format("Generating unit tests for module '%s'...", getDisplayName()));
        }
        VelocityEngine ve = new VelocityEngine();
        Template template;
        try {
            ve.setProperty("resource.loader", "string");
            ve.setProperty("string.resource.loader.class", StringResourceLoader.class.getName());
            ve.setProperty("string.resource.loader.repository.static", false);
            ve.init();
            if (!ve.resourceExists(unitTestTemplatePath)) {
                StringResourceRepository repo = (StringResourceRepository) ve.getApplicationAttribute(StringResourceLoader.REPOSITORY_NAME_DEFAULT);
                repo.putStringResource(unitTestTemplatePath, getTemplateFromResource(unitTestTemplatePath));
            }

            template = ve.getTemplate(unitTestTemplatePath);
        } catch (Exception e) {
            throw new IllegalStateException("Can't find template " + unitTestTemplatePath, e);
        }

        VelocityContext vc = new VelocityContext();

        vc.put("StringUtils", StringUtils.class);
        vc.put("openlInterfacePackage", getPackageName());
        vc.put("openlInterfaceClass", getClassName());
        vc.put("testMethodNames", getTestMethodNames());
        vc.put("projectRoot", StringEscapeUtils.escapeJava(StringUtils.removeEnd(getResourcesPath(), File.separator)));
        vc.put("srcFile", StringEscapeUtils.escapeJava(getDisplayName()));

        StringWriter writer = new StringWriter();

        try {
            template.merge(vc, writer);
            writeContentToFile(writer.toString(), getOutputFileName(), overwriteUnitTests);
        } catch (IOException e) {
            throw new IllegalStateException("Can't generate JUnit class for file " + getDisplayName(), e);
        }
    }

    private String getClassName() {
        String targetClass = getTargetClass();
        int idx = targetClass.lastIndexOf('.');
        return idx < 0 ? null : targetClass.substring(idx + 1);
    }

    private String getPackageName() {
        String targetClass = getTargetClass();
        int idx = targetClass.lastIndexOf('.');
        return idx < 0 ? null : targetClass.substring(0, idx);
    }

    private List<String> getTestMethodNames() {
        List<String> methodNames = new ArrayList<String>();
        for (IOpenMethod method : ProjectHelper.allTesters(getOpenClass())) {
            methodNames.add(method.getName());
        }
        return methodNames;
    }

    private String getOutputFileName() {
        return testSourceDirectory + "/" + getTargetClass().replace('.', '/') + "Test.java";
    }

    private void writeContentToFile(String content, String fileName, boolean override) throws IOException {
        FileWriter fw = null;
        try {
            if (new File(fileName).exists()) {
                if (override) {
                    if (log.isInfoEnabled()) {
                        log.info(String.format("File '%s' exists already. Overwrite it.", fileName));
                    }
                } else {
                    if (log.isInfoEnabled()) {
                        log.info(String.format("File '%s' exists already. Skip it.", fileName));
                        return;
                    }
                }
            }
            File folder = new File(fileName).getParentFile();
            if (!folder.mkdirs() && !folder.exists()) {
                throw new IOException("Can't create folder " + folder.getAbsolutePath());
            }
            fw = new FileWriter(fileName);
            fw.write(content);
        } finally {
            if (fw != null) {
                fw.close();
            }
        }
    }

    private String getTemplateFromResource(final String templatePath) throws IOException {
        InputStream inputStream;
        if (new File(templatePath).exists()) {
            inputStream = new FileInputStream(templatePath);
        } else {
            inputStream = getClass().getClassLoader().getResourceAsStream(templatePath);
        }
        return IOUtils.toStringAndClose(inputStream);
    }

    protected ProjectDescriptor createNewProject() {
        ProjectDescriptor project = new ProjectDescriptor();
        project.setName(defaultProjectName != null ? defaultProjectName : getDisplayName());

        List<PathEntry> classpath = new ArrayList<PathEntry>();
        for (String path : defaultClasspaths) {
            classpath.add(new PathEntry(path));
        }
        project.setClasspath(classpath);

        return project;
    }

    protected Module createNewModule() {
        Module module = new Module();

        module.setName(getDisplayName());
        module.setRulesRootPath(new PathEntry(getSrcFile()));
        return module;
    }

    // TODO extract the code that writes rules.xml, to another class
    protected void writeRulesXML() {
        File rulesDescriptor = new File(getResourcesPath() + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME);
        ProjectDescriptorManager manager = new ProjectDescriptorManager();

        ProjectDescriptor projectToWrite;
        List<Module> modulesToWrite = new ArrayList<Module>();
        long timeSinceModification = System.currentTimeMillis() - rulesDescriptor.lastModified();

        // FIXME: This is tricky to rely on the time since modification.
        // Consider that if the time since last modification is small enough it
        // will be the modification
        // made for previously created module by this ant task and we need to add one more module to the project
        // @author DLiauchuk
        if (rulesDescriptor.exists() && timeSinceModification < 2000) {
            // There is a previously created project descriptor, with modules in it.
            // The time was small enough to consider that it was modified/created by the generator.
            // Add current module to existed project.
            ProjectDescriptor existedDescriptor;
            try {
                existedDescriptor = manager.readOriginalDescriptor(rulesDescriptor);
                Module newModule = createNewModule();
                boolean exist = false;
                for (Module existedModule : existedDescriptor.getModules()) {
                    if (existedModule.getName().equals(newModule.getName())) {
                        modulesToWrite.add(newModule);
                        exist = true;
                    } else {
                        modulesToWrite.add(copyOf(existedModule));
                    }
                }
                if (!exist) {
                    modulesToWrite.add(newModule);
                }
                projectToWrite = existedDescriptor;
            } catch (Exception e) {
                log("Error while reading previously created project descriptor file " + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME, e, MSG_ERR);
                throw new IllegalStateException(e);
            }
        } else {
            // Create new project and add new module
            projectToWrite = createNewProject();
            modulesToWrite.add(createNewModule());
        }
        projectToWrite.setModules(modulesToWrite);

        FileOutputStream fous = null;
        try {
            fous = new FileOutputStream(rulesDescriptor);
            manager.writeDescriptor(projectToWrite, fous);
        } catch (Exception e) {
            log("Error while writing project descriptor file " + ProjectDescriptorBasedResolvingStrategy.PROJECT_DESCRIPTOR_FILE_NAME, e, MSG_ERR);
        } finally {
            IOUtils.closeQuietly(fous);
        }
    }

    /**
     * Copy the module without {@link Module#getProject()}, as it prevents
     * to Circular dependency.
     * @param module income module
     * @return copy of income module without project field
     */
    private Module copyOf(Module module) {
        Module copy = new Module();
        copy.setName(module.getName());
        copy.setProperties(module.getProperties());
        copy.setRulesRootPath(module.getRulesRootPath());
        return copy;
    }

    public boolean isIgnoreTestMethods() {
        return ignoreTestMethods;
    }

    public void setIgnoreTestMethods(boolean ignoreTestMethods) {
        this.ignoreTestMethods = ignoreTestMethods;
    }

    public String getDefaultProjectName() {
        return defaultProjectName;
    }

    public void setDefaultProjectName(String defaultProjectName) {
        this.defaultProjectName = defaultProjectName;
    }

    public String[] getDefaultClasspaths() {
        return defaultClasspaths;
    }

    public void setDefaultClasspaths(String[] defaultClasspaths) {
        this.defaultClasspaths = defaultClasspaths;
    }

    public boolean isCreateProjectDescriptor() {
        return createProjectDescriptor;
    }

    public void setCreateProjectDescriptor(boolean createProjectDescriptor) {
        this.createProjectDescriptor = createProjectDescriptor;
    }
    private IOpenClass openClass;

    private String goal = GenerateInterface.GOAL_ALL;
    private String web_inf_path;
    private String web_inf_exclude = ".*apache.ant.*|.*apache.tomcat.*|.*javacc.*";
    private String web_inf_include = "";

    private String classpathExclude = ".*apache.ant.*|.*apache.commons.*|.*apache.tomcat.*|.*javacc.*";
    private String vocabularyClass;

    private String projectHome = ".";
    private boolean ignoreNonJavaTypes = false;

    private String ignoreFields;
    private String ignoreMethods;

    private String userClassPath;
    private String userHome = ".";
    private String deplUserHome;

    /**
     * The root path where resources (such as rules.xml and rules) are located
     * For example in maven: "src/main/openl. Default is "".
     */
    private String resourcesPath = "";
    private String srcFile;
    private boolean usedRuleXmlForGenerate;
    private String deplSrcFile;

    private String srcModuleClass;
    private String openlName;
    private String targetSrcDir;

    private String targetClass;
    private String displayName;

    private String[] methods;
    private String[] fields;

    private String s_package;
    private String s_class;

    private String rulesFolder = "rules";

    private String extendsClass = null;

    private String dependencyManagerClass;
    private boolean generateDataType = true;

    /*
             * Full or relative path to directory where properties will be saved
             */
    private String classpathPropertiesOutputDir = ".";

    public void execute() throws Exception {
        if (getIgnoreFields() != null) {
            setFields(StringTool.tokenize(getIgnoreFields(), ", "));
        }

        if (getIgnoreMethods() != null) {
            setMethods(StringTool.tokenize(getIgnoreMethods(), ", "));
        }

        if (getGoal().equals(GenerateInterface.GOAL_ALL) || getGoal().contains(GenerateInterface.GOAL_UPDATE_PROPERTIES)) {
            saveProjectProperties();
        }

        writeSpecific();

        setOpenClass(makeOpenClass());

        // Generate interface is optional.
        if (targetClass != null) {
            JavaInterfaceGenerator javaGenerator = new JavaInterfaceGenerator.Builder(getOpenClass(), getTargetClass())
                .methodsToGenerate(getMethods())
                .fieldsToGenerate(getFields())
                .ignoreNonJavaTypes(isIgnoreNonJavaTypes())
                .ignoreTestMethods(isIgnoreTestMethods())
                .build();

            String content = javaGenerator.generateJava();
            String fileName = targetSrcDir + "/" + targetClass.replace('.', '/') + ".java";
            writeContentToFile(content, fileName, true);
        }

        if (getGoal().equals(GenerateInterface.GOAL_ALL) || getGoal().contains(GenerateInterface.GOAL_GENERATE_DATATYPES) && isGenerateDataType()) {
            writeDatatypeBeans(getOpenClass().getTypes());
        }

        if (getGoal().contains(GenerateInterface.GOAL_MAKE_WEBINF)) {
            if (getWeb_inf_path() == null) {
                throw new RuntimeException("web_inf_path is not set");
            }
        }

        if (getGoal().equals(GenerateInterface.GOAL_ALL) || getGoal().contains(GenerateInterface.GOAL_MAKE_WRAPPER)) {
            if (getWeb_inf_path() == null) {
                return;
            }

            makeWebInfPath();
        }
    }

    private String filterClassPath() throws IOException {
        String cp = System.getProperty("java.class.path");

        String[] tokens = StringTool.tokenize(cp, File.pathSeparator);

        StringBuilder buf = new StringBuilder(300);

        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].matches(classpathExclude)) {
                continue;
            }
            File f = FileTool.buildRelativePath(new File(projectHome), new File(tokens[i]));
            String relativePath = f.getPath().replace('\\', '/');
            buf.append(relativePath).append(File.pathSeparator);
        }
        return buf.toString();
    }

    public String getClasspathExclude() {
        return classpathExclude;
    }

    /*
     * Get full or relative path to directory where classpath properties will be
     * save
     */
    public String getClasspathPropertiesOutputDir() {
        return classpathPropertiesOutputDir;
    }

    public String getDeplSrcFile() {
        return deplSrcFile;
    }

    public String getDeplUserHome() {
        return deplUserHome;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getExtendsClass() {
        return extendsClass;
    }

    public String[] getFields() {
        return fields;
    }

    public String getGoal() {
        return goal;
    }

    public String getIgnoreFields() {
        return ignoreFields;
    }

    public String getIgnoreMethods() {
        return ignoreMethods;
    }

    public String[] getMethods() {
        return methods;
    }

    public String getOpenlName() {
        return openlName;
    }

    public String getRulesFolder() {
        return rulesFolder;
    }

    public String getS_class() {
        return s_class;
    }

    public String getS_package() {
        return s_package;
    }

    public String getResourcesPath() {
        return resourcesPath;
    }

    public String getSrcFile() {
        return srcFile;
    }

    public String getSrcModuleClass() {
        return srcModuleClass;
    }

    public String getTargetClass() {
        return targetClass;
    }

    public String getTargetSrcDir() {
        return targetSrcDir;
    }

    public String getUserClassPath() {
        return userClassPath;
    }

    public String getUserHome() {
        return userHome;
    }

    public String getVocabularyClass() {
        return vocabularyClass;
    }

    public String getWeb_inf_exclude() {
        return web_inf_exclude;
    }

    public String getWeb_inf_include() {
        return web_inf_include;
    }

    public String getWeb_inf_path() {
        return web_inf_path;
    }

    public boolean isIgnoreNonJavaTypes() {
        return ignoreNonJavaTypes;
    }

    public String getDependencyManager() {
        return dependencyManagerClass;
    }

    public void setDependencyManager(String dependencyManagerClass) {
        this.dependencyManagerClass = dependencyManagerClass;
    }

    protected void setOpenClass(IOpenClass openClass) {
        this.openClass = openClass;
    }

    public IOpenClass getOpenClass() {
        return openClass;
    }

    public IOpenClass makeOpenClass() throws Exception {
        CompiledOpenClass compiledOpenClass = null;
        IOpenClass result = null;

        if (usedRuleXmlForGenerate) {
            SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object> simpleProjectEngineFactoryBuilder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object>();
            SimpleProjectEngineFactory<Object> simpleProjectEngineFactory = simpleProjectEngineFactoryBuilder.setExecutionMode(false)
                    .setProvideRuntimeContext(false)
                    .setWorkspace(resourcesPath)
                    .setProject(resourcesPath)
                    .build();

            compiledOpenClass = simpleProjectEngineFactory.getCompiledOpenClass();
        } else {
            ClassLoader applicationClassLoader = getApplicationClassLoader();

            SimpleBundleClassLoader bundleClassLoader = new SimpleBundleClassLoader(applicationClassLoader);
            UserContext ucxt = new UserContext(bundleClassLoader, userHome);
            Thread.currentThread().setContextClassLoader(bundleClassLoader);

            long start = System.currentTimeMillis();
            try {
                IDependencyManager dependencyManager = instantiateDependencyManager();
                OpenClassJavaWrapper jwrapper = null;
                jwrapper = OpenClassJavaWrapper.createWrapper(openlName, ucxt, resourcesPath + srcFile, false, dependencyManager);
                compiledOpenClass = jwrapper.getCompiledClass();
            } finally {
                long end = System.currentTimeMillis();
                log("Loaded " + resourcesPath + srcFile + " in " + (end - start) + " ms");
            }
        }

        List<OpenLMessage> errorMessages = OpenLMessagesUtils.filterMessagesBySeverity(
                compiledOpenClass.getMessages(), Severity.ERROR);
        if (errorMessages != null && !errorMessages.isEmpty()) {
            String message = getErrorMessage(errorMessages);
            // throw new OpenLCompilationException(message);
            log(message, GenerateInterface.MSG_ERR);
        }

        return compiledOpenClass.getOpenClass();
        // }
    }

    private ClassLoader getApplicationClassLoader() throws Exception {
        ClassLoader cl = Thread.currentThread().getContextClassLoader();
        if (userClassPath != null) {
            cl = ClassLoaderFactory.createClassLoader(userClassPath, this.getClass().getClassLoader(), userHome);
        }
        return cl;
    }

    private IDependencyManager instantiateDependencyManager() {
        IDependencyManager dependecyManager = null;
        if (StringUtils.isNotBlank(dependencyManagerClass)) {
            try {
                Class<?> depManagerClass = Class.forName(dependencyManagerClass);
                Constructor<?> constructor = depManagerClass.getConstructor();
                dependecyManager = (IDependencyManager) constructor.newInstance();
            } catch (Exception e) {
                log(e, GenerateInterface.MSG_DEBUG);
            }
        }
        return dependecyManager;
    }

    private String getErrorMessage(List<OpenLMessage> errorMessages) {
        StringBuilder buf = new StringBuilder();
        buf.append("There are critical errors in wrapper:\n");
        for (int i = 0; i < errorMessages.size(); i++) {
            if (errorMessages.get(i) instanceof OpenLErrorMessage) {
                OpenLErrorMessage openlError = (OpenLErrorMessage) errorMessages.get(i);
                buf.append(openlError.getError().toString());
                buf.append("\n\n");
            } else {
                // shouldn`t happen
                buf.append(String.format("[%s] %s", i + 1, errorMessages.get(i).getSummary()));
                buf.append("\n");
            }
        }
        return buf.toString();
    }

    protected void makeWebInfPath() throws IOException {
        String targetFolder = web_inf_path;

        String classes_target = targetFolder + "/classes";
        String lib_target = targetFolder + "/lib";

        String cp = System.getProperty("java.class.path");

        String[] tokens = StringTool.tokenize(cp, File.pathSeparator);

        log("Making WEB-INF...");

        for (int i = 0; i < tokens.length; i++) {
            if (tokens[i].matches(web_inf_exclude) && !tokens[i].matches(web_inf_include)) {
                continue;
            }
            log(tokens[i]);

            File f = new File(tokens[i]);

            if (f.isDirectory()) {
                FileUtils.copyDirectory(f, new File(classes_target));
            } else {
                FileUtils.copyFileToDirectory(f, new File(lib_target));
            }
        }
    }

    protected void writeDatatypeBeans(Map<String, IOpenClass> types) throws Exception {
        if (types != null) {
            for (Map.Entry<String, IOpenClass> datatype : types.entrySet()) {

                // Skip java code generation for types what is defined
                // thru DomainOpenClass (skip java code generation for alias
                // types).
                //
                IOpenClass datatypeOpenClass = datatype.getValue();
                if (!(datatypeOpenClass instanceof DomainOpenClass)) {
                    Class<?> datatypeClass = datatypeOpenClass.getInstanceClass();
                    SimpleBeanJavaGenerator beanJavaGenerator = new SimpleBeanJavaGenerator(datatypeClass);
                    String javaClass = beanJavaGenerator.generateJavaClass();
                    String fileName = targetSrcDir + "/" + datatypeClass.getName().replace('.', '/') + ".java";
                    writeContentToFile(javaClass, fileName, true);
                }
            }
        }
    }

    /**
     * @throws IOException
     *
     */
    protected void saveProjectProperties() throws IOException {
        Properties p = new Properties();
        p.put(OpenLProjectPropertiesLoader.OPENL_CLASSPATH_PROPERTY, filterClassPath());
        p.put(OpenLProjectPropertiesLoader.OPENL_CLASSPATH_SEPARATOR_PROPERTY, File.pathSeparator);

        if (displayName != null) {
            p.put(targetClass + OpenLProjectPropertiesLoader.DISPLAY_NAME_SUFFIX, displayName);
        }

        if (vocabularyClass != null) {
            try {
                Class<?> c = Class.forName(vocabularyClass);
                c.newInstance();
                if (IVocabulary.class.isAssignableFrom(c)){
                    throw new ClassCastException(vocabularyClass + " doesn't implements IVocabulary.");
                }
            } catch (Throwable t) {
                log("Error occured while trying instantiate vocabulary class:" + vocabularyClass, t, GenerateInterface.MSG_ERR);
            }

            p.put(targetClass + OpenLProjectPropertiesLoader.VOCABULARY_CLASS_SUFFIX, vocabularyClass);
        }

        new OpenLProjectPropertiesLoader().saveProperties(classpathPropertiesOutputDir, p, false);
    }

    protected void log(String msg) {
        log(msg, GenerateInterface.MSG_INFO);
    }

    protected void log(Exception e, int msgDebug) {
        log(e.getMessage(), msgDebug);
    }

    protected void log(String msg, int msgLevel) {
        if (msgLevel <= GenerateInterface.MSG_INFO) {
            System.err.println(msg);
        }
    }

    protected void log(String s, Throwable t, int msgErr) {
        log(s, msgErr);
        log(t.getMessage(), msgErr);
    }

    public void setClasspathExclude(String classpathExclude) {
        this.classpathExclude = classpathExclude;
    }

    /*
     * Set full or relative path to directory where classpath properties will be
     * save
     */
    public void setClasspathPropertiesOutputDir(String classpathPropertiesOutputDir) {
        this.classpathPropertiesOutputDir = classpathPropertiesOutputDir;
    }

    public void setDeplSrcFile(String deplSrcFile) {
        this.deplSrcFile = deplSrcFile;
    }

    public void setDeplUserHome(String deplUserHome) {
        this.deplUserHome = deplUserHome;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public void setExtendsClass(String extendsClass) {
        this.extendsClass = extendsClass;
    }

    public void setFields(String[] fields) {
        this.fields = fields;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public void setIgnoreFields(String ignoreFields) {
        this.ignoreFields = ignoreFields;
    }

    public void setIgnoreMethods(String ignoreMethods) {
        this.ignoreMethods = ignoreMethods;
    }

    public void setIgnoreNonJavaTypes(boolean ignoreNonJavaTypes) {
        this.ignoreNonJavaTypes = ignoreNonJavaTypes;
    }

    public void setMethods(String[] methods) {
        this.methods = methods;
    }

    public void setOpenlName(String openlName) {
        this.openlName = openlName;
    }

    public void setRulesFolder(String rulesFolder) {
        this.rulesFolder = rulesFolder;
    }

    public void setS_class(String s_class) {
        this.s_class = s_class;
    }

    public void setS_package(String s_package) {
        this.s_package = s_package;
    }

    public void setResourcesPath(String resourcesPath) {
        this.resourcesPath = resourcesPath.isEmpty() || resourcesPath.endsWith(File.separator) ? resourcesPath
                                                                                               : resourcesPath + File.separator;
    }

    public void setSrcFile(String srcFile) {
        this.srcFile = srcFile;
    }

    public void setSrcModuleClass(String srcModuleClass) {
        this.srcModuleClass = srcModuleClass;
    }

    public void setTargetClass(String targetClass) {
        this.targetClass = targetClass;
    }

    public void setTargetSrcDir(String targetSrcDir) {
        this.targetSrcDir = targetSrcDir;
    }

    public void setUserClassPath(String userClassPath) {
        this.userClassPath = userClassPath;
    }

    public void setUserHome(String userHome) {
        this.userHome = userHome;
    }

    public void setVocabularyClass(String vocabularyClass) {
        this.vocabularyClass = vocabularyClass;
    }

    public void setWeb_inf_exclude(String web_inf_exclude) {
        this.web_inf_exclude = web_inf_exclude;
    }

    public void setWeb_inf_include(String web_inf_include) {
        this.web_inf_include = web_inf_include;
    }

    public void setWeb_inf_path(String web_inf_path) {
        this.web_inf_path = web_inf_path;
    }

    public boolean isUsedRuleXmlForGenerate() {
        return usedRuleXmlForGenerate;
    }

    public void setUsedRuleXmlForGenerate(boolean usedRuleXmlForGenerate) {
        this.usedRuleXmlForGenerate = usedRuleXmlForGenerate;
    }

    public boolean isGenerateDataType() {
        return generateDataType;
    }

    public void setGenerateDataType(boolean generateDataType) {
        this.generateDataType = generateDataType;
    }
}
