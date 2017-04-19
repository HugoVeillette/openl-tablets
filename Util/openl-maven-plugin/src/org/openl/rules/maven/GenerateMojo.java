package org.openl.rules.maven;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.rules.maven.gen.GenerateInterface;
import org.openl.rules.maven.gen.SimpleBeanJavaGenerator;
import org.openl.rules.project.instantiation.SimpleProjectEngineFactory;
import org.openl.types.IOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;

import com.helger.jcodemodel.EClassType;
import com.helger.jcodemodel.JClassAlreadyExistsException;
import com.helger.jcodemodel.JCodeModel;
import com.helger.jcodemodel.JDefinedClass;
import com.helger.jcodemodel.JMethod;
import com.helger.jcodemodel.JMod;

import net.sf.cglib.beans.BeanGenerator;
import net.sf.cglib.core.NamingPolicy;
import net.sf.cglib.core.Predicate;

/**
 * Generate OpenL interface, domain classes, project descriptor and unit tests
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateMojo extends BaseOpenLMojo {

    @Parameter(defaultValue = "${project}", readonly = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.compileClasspathElements}", readonly = true, required = true)
    private List<String> classpath;

    @Parameter(defaultValue = "${project.compileSourceRoots}", readonly = true, required = true)
    private List<String> sourceRoots;

    /**
     * An output directory of generated Java beans and OpenL java interface.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/openl")
    private File outputDirectory;

    /**
     * Comma-separated list of interfaces which are used for extending of the generated interface.
     */
    @Parameter
    private String superInterface;

    /**
     * A generated Java interface from an OpenL project. If it is empty then
     * generation will be skipped.
     */
    @Parameter
    private String interfaceClass;

    /**
     * Add IRulesRuntimeContext arguments to the generated interface.
     */
    @Parameter
    private boolean isProvideRuntimeContext;

    /**
     * Add additional methods to the generated interface to support variations.
     */
    @Parameter
    private boolean isProvideVariations;

    /**
     * Tasks that will generate classes or data type.
     * <p>
     * <b>Object Properties</b>
     * <table border="1">
     * <tr>
     * <th>Name</th>
     * <th>Type</th>
     * <th>Required</th>
     * <th>Description</th>
     * </tr>
     * <tr>
     * <td>srcFile</td>
     * <td>String</td>
     * <td>false</td>
     * <td>*Reference to the Excel file for which an interface class must be
     * generated.</td>
     * </tr>
     * <tr>
     * <td>targetClass</td>
     * <td>String</td>
     * <td>false</td>
     * <td>*Full name of the interface class to be generated. Optional; if
     * missed interface not generated. OpenL Tablets WebStudio recognizes
     * modules in projects by interface classes and uses their names in the user
     * interface. If there are multiple wrappers with identical names, only one
     * of them is recognized as a module in OpenL Tablets WebStudio.</td>
     * </tr>
     * <tr>
     * <td>isUsedRuleXmlForGenerate</td>
     * <td>boolean (true/false)</td>
     * <td>false</td>
     * <td>*Should system generate class and datatypes from rules.xml. If yes
     * srcFile ignored; targetClass is required.</td>
     * </tr>
     * <tr>
     * <td>displayName</td>
     * <td>String</td>
     * <td>false</td>
     * <td>*End user oriented title of the file that appears in OpenL Tablets
     * WebStudio. Default value is Excel file name without extension.</td>
     * </tr>
     * <tr>
     * <td>targetSrcDir</td>
     * <td>String</td>
     * <td>false</td>
     * <td>*Folder where the generated interface class must be placed. For
     * example: "src/main/java". Default value is:
     * "${project.build.sourceDirectory}"</td>
     * </tr>
     * <tr>
     * <td>openlName</td>
     * <td>String</td>
     * <td>false</td>
     * <td>*OpenL configuration to be used. For OpenL Tablets, the following
     * value must always be used: org.openl.xls. Default value is:
     * "org.openl.xls"</td>
     * </tr>
     * <tr>
     * <td>userHome</td>
     * <td>String</td>
     * <td>false</td>
     * <td>*Location of user-defined resources relative to the current OpenL
     * Tablets project. Default value is: "."</td>
     * </tr>
     * <tr>
     * <td>userClassPath</td>
     * <td>String</td>
     * <td>false</td>
     * <td>*Reference to the folder with additional compiled classes imported by
     * the module when the interface is generated. Default value is: null.</td>
     * </tr>
     * <tr>
     * <td>ignoreTestMethods</td>
     * <td>boolean</td>
     * <td>false</td>
     * <td>*If true, test methods will not be added to interface class. Used
     * only in GenerateInterface. Default value is: true.</td>
     * </tr>
     * <tr>
     * <td>generateUnitTests</td>
     * <td>boolean</td>
     * <td>false</td>
     * <td>*Overwrites base {@link #generateUnitTests} value</td>
     * </tr>
     * <tr>
     * <td>unitTestTemplatePath</td>
     * <td>String</td>
     * <td>false</td>
     * <td>*Overwrites base {@link #unitTestTemplatePath} value</td>
     * </tr>
     * <tr>
     * <td>overwriteUnitTests</td>
     * <td>boolean</td>
     * <td>false</td>
     * <td>*Overwrites base {@link #overwriteUnitTests} value</td>
     * </tr>
     * <tr>
     * <td>generateDataType</td>
     * <td>boolean</td>
     * <td>false</td>
     * <td>*Generate or not dataType for current task.</td>
     * </tr>
     * </table>
     * <p>
     *
     * @deprecated Obsolete. Replaced with the smart generator. Use interfaceClass instead.
     */
    @Parameter
    @Deprecated
    private GenerateInterface[] generateInterfaces;

    /**
     * If true, rules.xml will be generated if it doesn't exist. If false,
     * rules.xml will not be generated. Default value is "true".
     * 
     * @see #overwriteProjectDescriptor
     * @deprecated Obsolete. No needs to generate rules.xml from Maven.
     */
    @Parameter(defaultValue = "true")
    @Deprecated
    private boolean createProjectDescriptor;

    /**
     * If true, rules.xml will be overwritten on each run. If false, rules.xml
     * generation will be skipped if it exists. Makes sense only if
     * {@link #createProjectDescriptor} == true. Default value is "true".
     * 
     * @see #createProjectDescriptor
     * @deprecated Obsolete. No needs to generate rules.xml from Maven.
     */
    @Parameter(defaultValue = "true")
    @Deprecated
    private boolean overwriteProjectDescriptor;

    /**
     * Default project name in rules.xml. If omitted, the name of the first
     * module in the project is used. Used only if createProjectDescriptor ==
     * true.
     * @deprecated Obsolete. No needs to generate rules.xml from Maven.
     */
    @Parameter
    @Deprecated
    private String projectName;

    /**
     * Default classpath entries in rules.xml. Default value is {"."} Used only
     * if createProjectDescriptor == true.
     * @deprecated Obsolete. No needs to generate rules.xml from Maven.
     */
    @Parameter
    @Deprecated
    private String[] classpaths = { "." };

    /**
     * If true, JUnit tests for OpenL Tablets Test tables will be generated.
     * Default value is "false"
     * @deprecated Obsolete. Use openl:test goal to run OpenL tests.
     */
    @Parameter(defaultValue = "false")
    @Deprecated
    private Boolean generateUnitTests;

    /**
     * Path to Velocity template for generated unit tests. If omitted, default
     * template will be used. Available in template variables:
     * <table border="1">
     * <tr>
     * <th>Name</th>
     * <th>Description</th>
     * </tr>
     * <tr>
     * <td>openlInterfacePackage</td>
     * <td>Package of generated interface class</td>
     * </tr>
     * <tr>
     * <td>openlInterfaceClass</td>
     * <td>Generated interface class name</td>
     * </tr>
     * <tr>
     * <td>testMethodNames</td>
     * <td>Available test method names</td>
     * </tr>
     * <tr>
     * <td>projectRoot</td>
     * <td>Root directory of OpenL project</td>
     * </tr>
     * <tr>
     * <td>srcFile</td>
     * <td>Reference to the Excel file for which an interface class must be
     * generated.</td>
     * </tr>
     * <tr>
     * <td>StringUtils</td>
     * <td>Apache commons utility class</td>
     * </tr>
     * </table>
     * @deprecated Obsolete. Use openl:test goal to run OpenL tests.
     */
    @Parameter(defaultValue = "org/openl/rules/maven/JUnitTestTemplate.vm")
    @Deprecated
    private String unitTestTemplatePath;

    /**
     * If true, existing JUnit tests will be overwritten. If false, only absent
     * tests will be generated, others will be skipped.
     * @deprecated Obsolete. Use openl:test goal to run OpenL tests.
     */
    @Parameter(defaultValue = "false")
    @Deprecated
    private Boolean overwriteUnitTests;

    @Override
    @Deprecated
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (generateInterfaces != null) {
            useGenerateInterface();
        } else {
            super.execute();
        }
    }

    @Override
    public void execute(String sourcePath) throws Exception {
        ClassLoader classLoader = composeClassLoader();

        SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<?> builder = new SimpleProjectEngineFactory.SimpleProjectEngineFactoryBuilder<Object>();
        SimpleProjectEngineFactory<?> factory = builder.setProject(sourcePath)
            .setClassLoader(classLoader)
            .setProvideRuntimeContext(isProvideRuntimeContext)
            .setProvideVariations(isProvideVariations)
            .setExecutionMode(true)
            .build();

        CompiledOpenClass openLRules = factory.getCompiledOpenClass();

        // Generate Java beans from OpenL dataTypes
        writeJavaBeans(openLRules.getTypes());

        // Generate interface is optional.
        if (interfaceClass != null) {
            Class<?> interfaceClass = factory.getInterfaceClass();
            writeInterface(interfaceClass);
        }

        project.addCompileSourceRoot(outputDirectory.getPath());
    }

    private ClassLoader composeClassLoader() throws Exception {
        info("Composing the classloader for the folloving sources:");
        for (String dir : sourceRoots) {
            info("  # source roots > ", dir);
        }
        URL[] urls = toURLs(classpath);
        return new URLClassLoader(urls, this.getClass().getClassLoader()) {
            @Override
            public Class<?> findClass(String name) throws ClassNotFoundException {
                String file = name.replace('.', '/').concat(".java");
                for (String dir : sourceRoots) {
                    if (new File(dir, file).isFile()) {
                        debug("  # FOUND > ", dir, "/", file);
                        BeanGenerator builder = new BeanGenerator();
                        builder.setClassLoader(this);
                        builder.setNamingPolicy(new ClassNaming(name));
                        return builder.create().getClass();
                    }
                }
                debug("  > ", file);
                return super.findClass(name);
            }
        };
    }

    @Override
    String getHeader() {
        return "OPENL JAVA SOURCES GENERATION";
    }

    @Deprecated
    private void useGenerateInterface() throws MojoExecutionException {
        if (getLog().isInfoEnabled()) {
            getLog().info("Running OpenL GenerateMojo...");
        }
        boolean isUsedRuleXmlForGenerate = false;
        for (GenerateInterface task : generateInterfaces) {
            if (task.isUsedRuleXmlForGenerate()) {
                isUsedRuleXmlForGenerate = true;
                break;
            }
        }
        for (GenerateInterface task : generateInterfaces) {
            if (getLog().isInfoEnabled()) {
                getLog().info(String.format("Generating classes for module '%s'...", task.getDisplayName()));
            }
            initDefaultValues(task, isUsedRuleXmlForGenerate);
            try {
                task.execute();
            } catch (Exception e) {
                throw new MojoExecutionException("Exception during generation: ", e);
            }
        }
        project.addCompileSourceRoot(outputDirectory.getPath());
    }

    private void initDefaultValues(GenerateInterface task, boolean isUsedRuleXmlForGenerate) {
        if (StringUtils.isBlank(task.getResourcesPath())) {
            task.setResourcesPath(getSourceDirectory());
        }
        if (!task.isUsedRuleXmlForGenerate() && isUsedRuleXmlForGenerate) {
            task.setGenerateDataType(false);
        }
        if (task.getOpenlName() == null) {
            task.setOpenlName(OpenL.OPENL_JAVA_RULE_NAME);
        }
        if (task.getTargetSrcDir() == null) {
            task.setTargetSrcDir(outputDirectory.getPath());
        }

        if (task.getDisplayName() == null) {
            task.setDisplayName(FileUtils.getBaseName(task.getSrcFile()));
        }

        if (task.getSrcFile() != null) {
            initResourcePath(task);
        }

        initCreateProjectDescriptorState(task);
        task.setDefaultProjectName(projectName);
        task.setDefaultClasspaths(classpaths);

        task.setLog(getLog());
        task.setTestSourceDirectory(project.getBuild().getTestSourceDirectory());
        if (task.getGenerateUnitTests() == null) {
            task.setGenerateUnitTests(generateUnitTests);
        }
        if (task.getUnitTestTemplatePath() == null) {
            task.setUnitTestTemplatePath(unitTestTemplatePath);
        }
        if (task.getOverwriteUnitTests() == null) {
            task.setOverwriteUnitTests(overwriteUnitTests);
        }
    }

    private void initCreateProjectDescriptorState(GenerateInterface task) {
        if (createProjectDescriptor) {
            if (new File(task.getResourcesPath(), "rules.xml").exists()) {
                task.setCreateProjectDescriptor(overwriteProjectDescriptor);
                return;
            }
        }
        task.setCreateProjectDescriptor(createProjectDescriptor);
    }

    private void initResourcePath(GenerateInterface task) {
        String srcFile = task.getSrcFile().replace("\\", "/");
        String baseDir = project.getBasedir().getAbsolutePath();

        String directory = getSubDirectory(baseDir, getSourceDirectory()).replace("\\", "/");
        if (srcFile.startsWith(directory)) {
            srcFile = getSubDirectory(directory, srcFile);
            task.setResourcesPath(directory);
            task.setSrcFile(srcFile);
            return;
        }

        @SuppressWarnings("unchecked")
        List<Resource> resources = (List<Resource>) project.getResources();
        for (Resource resource : resources) {
            String resourceDirectory = resource.getDirectory();
            resourceDirectory = getSubDirectory(baseDir, resourceDirectory).replace("\\", "/");

            if (srcFile.startsWith(resourceDirectory)) {
                srcFile = getSubDirectory(resourceDirectory, srcFile);
                task.setResourcesPath(resourceDirectory);
                task.setSrcFile(srcFile);
                break;
            }
        }
    }

    private String getSubDirectory(String baseDir, String resourceDirectory) {
        if (resourceDirectory.startsWith(baseDir)) {
            resourceDirectory = resourceDirectory.substring(resourceDirectory.lastIndexOf(baseDir) + baseDir.length());
            resourceDirectory = removeSlashFromBeginning(resourceDirectory);
        }
        return resourceDirectory;
    }

    private String removeSlashFromBeginning(String resourceDirectory) {
        if (resourceDirectory.startsWith("/") || resourceDirectory.startsWith("\\")) {
            resourceDirectory = resourceDirectory.substring(1);
        }
        return resourceDirectory;
    }

    private void writeJavaBeans(Collection<IOpenClass> types) throws Exception {
        if (CollectionUtils.isNotEmpty(types)) {
            for (IOpenClass datatypeOpenClass : types) {

                // Skip java code generation for types what is defined
                // thru DomainOpenClass (skip java code generation for alias
                // types).
                //
                if (datatypeOpenClass instanceof DatatypeOpenClass) {
                    Class<?> datatypeClass = datatypeOpenClass.getInstanceClass();
                    String dataType = datatypeClass.getName();
                    info("Java Bean: " + dataType);
                    SimpleBeanJavaGenerator beanJavaGenerator = new SimpleBeanJavaGenerator(datatypeClass);
                    String generatedSource = beanJavaGenerator.generateJavaClass();
                    writeClassToFile(dataType, generatedSource);
                }
            }
        }
    }

    private void writeInterface(Class<?> clazz) throws IOException, JClassAlreadyExistsException {
        info("Interface: " + interfaceClass);
        JCodeModel model = new JCodeModel();
        CodeHelper helper = new CodeHelper();

        // Generate a class body
        JDefinedClass java = model._class(interfaceClass, EClassType.INTERFACE);

        // Add super interfaces
        String[] interfaces = StringUtils.split(superInterface, ',');
        if (CollectionUtils.isNotEmpty(interfaces)) {
            for (String s : interfaces) {
                java._extends(helper.get(s));
            }
        }

        // Generate methods
        Method[] methods = clazz.getMethods();

        for (Method method : methods) {
            String name = method.getName();
            Class<?> returnType = method.getReturnType();
            JMethod jm = java.method(JMod.NONE, helper.get(returnType), name);
            Class<?>[] argTypes = method.getParameterTypes();
            for (int i = 0; i < argTypes.length; i++) {
                jm.param(helper.get(argTypes[i]), "arg" + i);
            }
        }

        // Write the generated source code
        model.build(outputDirectory, (PrintStream) null);
    }

    private void writeClassToFile(String clazz, String source) throws IOException {
        File file = new File(outputDirectory, clazz.replace('.', '/') + ".java");
        FileWriter fw = null;
        try {
            if (file.exists()) {
                if (getLog().isInfoEnabled()) {
                    getLog().info(String.format("File '%s' exists already. It has been overwritten.", file));
                }
            }
            File folder = file.getParentFile();
            if (folder != null && !folder.mkdirs() && !folder.isDirectory()) {
                throw new IOException("Failed to create folder " + folder.getAbsolutePath());
            }
            fw = new FileWriter(file);
            fw.write(source);
        } finally {
            IOUtils.closeQuietly(fw);
        }
    }

    private static class ClassNaming implements NamingPolicy {
        private final String className;

        private ClassNaming(String className) {
            this.className = className;
        }

        @Override
        public String getClassName(String s, String s1, Object o, Predicate predicate) {
            return className;
        }
    };

    /**
     * A utility class to convert Java classes in CodeModel class descriptors.
     * It is required for managing generated beans because of they have not a classloader.
     */
    private class CodeHelper {
        JCodeModel model = new JCodeModel();

        JDefinedClass get(Class<?> clazz) throws JClassAlreadyExistsException {
            String clazzName = clazz.getName();
            EClassType eClassType = clazz.isInterface() ? EClassType.INTERFACE : EClassType.CLASS;
            return get(clazzName, eClassType);
        }

        JDefinedClass get(String clazzName) throws JClassAlreadyExistsException {
            return get(clazzName, EClassType.INTERFACE);
        }

        private JDefinedClass get(String clazzName, EClassType eClassType) throws JClassAlreadyExistsException {
            JDefinedClass jArgType = model._getClass(clazzName);
            if (jArgType == null) {
                jArgType = model._class(clazzName, eClassType);
            }
            return jArgType;
        }
    }
}
