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
import java.net.URL;
import java.net.URLClassLoader;
import java.util.List;
import java.util.Map;

import net.sf.cglib.beans.BeanGenerator;
import net.sf.cglib.core.NamingPolicy;
import net.sf.cglib.core.Predicate;
import net.sf.cglib.proxy.InterfaceMaker;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import org.apache.maven.plugins.annotations.ResolutionScope;
import org.openl.CompiledOpenClass;
import org.openl.OpenL;
import org.openl.rules.lang.xls.types.DatatypeOpenClass;
import org.openl.types.IOpenClass;
import org.openl.util.CollectionUtils;
import org.openl.util.FileUtils;
import org.openl.util.IOUtils;
import org.openl.util.StringUtils;
import org.openl.rules.maven.gen.SimpleBeanJavaGenerator;

/**
 * Generate OpenL interface, domain classes, project descriptor and unit tests
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.GENERATE_SOURCES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateMojo extends BaseOpenLMojo {

    /**
     * An output directory of generated Java beans and OpenL java interface.
     */
    @Parameter(defaultValue = "${project.build.directory}/generated-sources/openl")
    private File outputDirectory;

    /**
     * A generated Java interface from an OpenL project. If it is empty then
     * generation will be skipped.
     */
    @Parameter
    private String interfaceClass;

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
     */
    @Deprecated
    private GenerateInterface[] generateInterfaces;

    /**
     * If true, rules.xml will be generated if it doesn't exist. If false,
     * rules.xml will not be generated. Default value is "true".
     * 
     * @see #overwriteProjectDescriptor
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
     */
    @Parameter(defaultValue = "true")
    @Deprecated
    private boolean overwriteProjectDescriptor;

    /**
     * Default project name in rules.xml. If omitted, the name of the first
     * module in the project is used. Used only if createProjectDescriptor ==
     * true.
     */
    @Parameter
    @Deprecated
    private String projectName;

    /**
     * Default classpath entries in rules.xml. Default value is {"."} Used only
     * if createProjectDescriptor == true.
     */
    @Parameter
    @Deprecated
    private String[] classpaths = { "." };

    /**
     * If true, JUnit tests for OpenL Tablets Test tables will be generated.
     * Default value is "false"
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
     */
    @Parameter(defaultValue = "org/openl/rules/maven/JUnitTestTemplate.vm")
    @Deprecated
    private String unitTestTemplatePath;

    /**
     * If true, existing JUnit tests will be overwritten. If false, only absent
     * tests will be generated, others will be skipped.
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
    String execute(CompiledOpenClass openLRules) throws Exception {
        // Generate Java beans from OpenL dataTypes
        Map<String, IOpenClass> dataTypes = openLRules.getTypes();
        writeJavaBeans(dataTypes);

        // Generate interface is optional.
        if (interfaceClass != null) {
            IOpenClass openClass = openLRules.getOpenClassWithErrors();
            writeInterface(openClass);
        }
        project.addCompileSourceRoot(outputDirectory.getPath());
        return null;
    }

    @Override
    ClassLoader composeClassLoader() throws Exception {
        final List<String> compileSourceRoots = project.getCompileSourceRoots();
        info("Composing the classloader for the folloving sources:");
        for (String dir : compileSourceRoots) {
            info("  # source roots > ", dir);
        }
        info("Using the following classpaths:");
        List<String> files = project.getCompileClasspathElements();
        URL[] urls = toURLs(files);
        return new URLClassLoader(urls, this.getClass().getClassLoader()) {
            @Override
            public Class<?> findClass(String name) throws ClassNotFoundException {
                String file = name.replace('.', '/').concat(".java");
                for (String dir : compileSourceRoots) {
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
            task.setResourcesPath(getSourceDirectory().getPath());
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

        String directory = getSubDirectory(baseDir, getSourceDirectory().getPath()).replace("\\", "/");
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

    private void writeJavaBeans(Map<String, IOpenClass> types) throws Exception {
        if (CollectionUtils.isNotEmpty(types)) {
            for (Map.Entry<String, IOpenClass> datatype : types.entrySet()) {

                // Skip java code generation for types what is defined
                // thru DomainOpenClass (skip java code generation for alias
                // types).
                //
                IOpenClass datatypeOpenClass = datatype.getValue();
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

    private void writeInterface(IOpenClass openClass) throws IOException {
        info("Interface: " + interfaceClass);
        JavaInterfaceGenerator javaGenerator = new JavaInterfaceGenerator.Builder(openClass, interfaceClass)
            .methodsToGenerate(null).fieldsToGenerate(null).ignoreNonJavaTypes(false).ignoreTestMethods(true).build();

        String generatedSource = javaGenerator.generateJava();
        writeClassToFile(interfaceClass, generatedSource);
    }

    private void writeClassToFile(String clazz, String source) throws IOException {
        File file = new File(outputDirectory, clazz.replace('.', '/') + ".java");
        FileWriter fw = null;
        try {
            if (file.exists()) {
                if (getLog().isInfoEnabled()) {
                    getLog().info(String.format("File '%s' exists already. Overwrite it.", file));
                }
            }
            File folder = file.getParentFile();
            if (folder != null && !folder.mkdirs() && !folder.isDirectory()) {
                throw new IOException("Can't create folder " + folder.getAbsolutePath());
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
}
