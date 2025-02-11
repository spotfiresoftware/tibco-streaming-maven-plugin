/*******************************************************************************
 * Copyright © 2018-2025 Cloud Software Group, Inc.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ******************************************************************************/
package com.tibco.ep.buildmavenplugin;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.DefaultArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.util.DirectoryScanner;

import com.tibco.ep.buildmavenplugin.admin.RuntimeCommandRunner;
import com.tibco.ep.buildmavenplugin.surefire.Runner;
import com.tibco.ep.sb.services.management.AbstractDeployFragmentCommandBuilder;
import com.tibco.ep.sb.services.management.FragmentType;
import com.tibco.ep.sb.services.management.IDestination;

/**
 * Base test
 */
abstract class BaseTestMojo extends BaseExecuteMojo {

    // maven read-only parameters
    //

    /**
     * <p>Servicename to determine which nodes to execute on.</p>
     *
     * <p>If not set, execution will be run on all the nodes in the cluster.</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/administer-nodes-serviceName.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter
    String serviceName;
    /**
     * <p>Set this to 'true' to skip running tests, but still compile them.</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/skipTests.svg" alt="pom">
     *
     * <p>Example use on commandline:</p>
     * <img src="uml/skipTests-commandline.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter(property = "skipTests", defaultValue = "false")
    boolean skipTests;
    /**
     * <p>Set this to 'true' to only install nodes</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/installOnly.svg" alt="pom">
     *
     * <p>Example use on commandline:</p>
     * <img src="uml/installOnly-commandline.svg" alt="pom">
     *
     * @since 1.1.0
     */
    @Parameter(property = "installOnly")
    boolean installOnly;

    // maven user parameters
    //
    /**
     * <p>Specify this parameter to run individual tests by file name, overriding
     * the <code>includes</code>/<code>excludes</code> parameter.</p>
     *
     * <p>Each pattern you specify here will be used to create an include pattern
     * formatted like <code>**&#47;${test}.java</code>, so you can just type
     * "-Dtest=MyTest" to run a single test called "foo/MyTest.java".</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/test.svg" alt="pom">
     *
     * <p>Example use on commandline:</p>
     * <img src="uml/test-commandline.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter(property = "test")
    String test;
    /**
     * <p>List of patterns used to specify the tests that should be included in
     * testing. </p>
     *
     * <p>When not specified and when the <code>test</code> parameter is
     * not specified, the default includes will be
     * <code>**&#47;Test*.java   **&#47;*Test.java   **&#47;*TestCase.java  **&#47;*TestSuite*.java</code></p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/includes.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter
    List<String> includes;
    /**
     * <p>List of patterns used to specify the tests that should be excluded in
     * testing.</p>
     *
     * <p>When not specified and when the <code>test</code> parameter is
     * not specified, the default excludes will be <code>**&#47;*$*</code>
     * (which excludes all inner classes).</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/excludes.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter
    List<String> excludes;
    /**
     * <p>Java options to pass to the execution environment</p>
     * <br />
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/javaOptions.svg" alt="pom">
     *
     * <p>Example use on commandline:</p>
     * <img src="uml/javaOptions-commandline.svg" alt="pom">
     *
     * <p><b>User property is:</b> <tt>options</tt>.</p>
     *
     * @since 1.0.0
     */
    @Parameter
    String[] javaOptions;
    /**
     * <p>Java system properties to pass to the execution environment</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/systemPropertyVariables.svg" alt="pom">
     *
     * @since 1.2.0
     */
    @Parameter
    Map<String, String> systemPropertyVariables;
    /**
     * Additional javaOptions to append
     *
     * @since 1.1.0
     */
    @Parameter(property = "options", readonly = true)
    String[] optionsProperty;
    /**
     * <p>Node options to pass to the execution environment.  See the deployment
     * tool documentation for details</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/nodeOptions.svg" alt="pom">
     *
     * <p>Example use on commandline:</p>
     * <img src="uml/nodeOptions-commandline.svg" alt="pom">
     *
     * <p><b>User property is:</b> <tt>nodeOptions</tt>.</p>
     *
     * @since 1.0.0
     */
    @Parameter
    Map<String, String> nodeOptions;
    /**
     * <p>Additional nodeOptions to append</p>
     *
     * @since 1.1.0
     */
    @Parameter(property = "nodeOptions", readonly = true)
    String[] nodeOptionsProperty;
    /**
     * <p>Location of the junit reports</p>
     *
     * <p>Example use on commandline:</p>
     * <img src="uml/nodeOptions-commandline.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "${project.build.directory}/surefire-reports")
    File reportsDirectory;
    /**
     * <p>
     * Test main class
     * </p>
     *
     * <p>
     * This main class is deployed to the whole cluster and run in the
     * background before test cases are executed
     * </p>
     *
     * <p>
     * Note that javaOptions is not applied to testMain
     * </p>
     *
     * <p>Example use on commandline:</p>
     * <img src="uml/testMain2.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter
    String testMain;
    /**
     * <p>
     * Set this to 'true' to have the unit test use System.exit() to terminate the test.
     * </p>
     *
     * <p>Example use on commandline:</p>
     * <img src="uml/useSystemExit.svg" alt="pom">
     *
     * @since 1.3.0
     */
    @Parameter(defaultValue = "false")
    boolean useSystemExit;
    /**
     * <p>Set this to 'true' to skip stopping test nodes</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/start-nodes-skipStop.svg" alt="pom">
     *
     * <p>Example use on commandline:</p>
     * <img src="uml/start-nodes-skipStop-commandline.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter(property = "skipStop")
    boolean skipStop;
    /**
     * The classpath elements of the project being tested.
     *
     * @since 1.0.0
     */
    @Parameter(property = "project.testClasspathElements", readonly = true, required = true)
    private List<String> classpathElements;
    /**
     * The directory to search for generated test classes of the project being
     * tested.
     *
     * @since 1.0.0
     */
    @Parameter(property = "project.build.testOutputDirectory", readonly = true, required = true)
    private File testOutputDirectory;
    /**
     * The directory to search for generated classes of the project being
     * tested.
     *
     * @since 1.0.0
     */
    @Parameter(property = "project.build.outputDirectory", readonly = true, required = true)
    private File classesDirectory;
    /**
     * <p>Additional classpath elements</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/additionalClasspathElements.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter
    private List<String> additionalClasspathElements;
    private PluginClassloader coverageClassLoader = null;

    /**
     * Build a String array of include/exclude patterns. Convert the list of
     * Java files to a String array of class files. FIX THIS: undocumented code
     * originally from Surefire.
     *
     * @param list
     * @return
     */
    private static String[] processIncludesExcludes(List<String> list) {
        String[] incs = new String[list.size()];

        for (int i = 0; i < incs.length; i++) {
            incs[i] = ((String) list.get(i)).replace(".java", ".class");
        }
        return incs;
    }

    private void terminateNodes(String deployServiceName) {
        try {
            newCommand(deployServiceName)
                .commandAndTarget("terminate", "node")
                .errorHandling(ErrorHandling.IGNORE)
                .run();
        } catch (MojoExecutionException e) {
            getLog().debug("Could not terminate nodes", e);
        }
    }

    private void removeNodes(String deployServiceName) {
        try {
            removeNodes(deployServiceName, ErrorHandling.IGNORE);
        } catch (MojoExecutionException e) {
            getLog().debug("Could not remove nodes", e);
        }
    }

    private void stopNodes(String deployServiceName) {
        try {
            stopNodes(deployServiceName, ErrorHandling.IGNORE);
        } catch (MojoExecutionException e) {
            getLog().debug("Could not stop nodes", e);
        }
    }

    private String getFullNativePath() {
        // if there are any native libraries, set library path
        //
        String osName = System.getProperty("os.name").replaceAll("\\s", "");
        if (osName.startsWith("Windows")) {
            osName = "Windows";
        }
        StringBuilder fullNativePath = new StringBuilder();
        for (String suffix : new String[]{"gpp", "msvc"}) {
            for (String type : new String[]{"jni", "shared"}) {
                File nativePath = new File(project.getBuild()
                    .getDirectory() + File.separator + "nar" +
                    File.separator + "lib" +
                    File.separator + System.getProperty("os.arch") + "-" + osName + "-" + suffix +
                    File.separator + type);
                if (nativePath.isDirectory()) {
                    if (fullNativePath.length() > 0) {
                        fullNativePath.append(File.pathSeparator);
                    }
                    fullNativePath.append(nativePath.getAbsolutePath());
                }
            }
        }
        return fullNativePath.toString();
    }

    private String getClassPath(List<File> eventflowDirectories, File liveviewDirectory) throws MojoExecutionException {
        // Set classpath for the test run
        //
        StringBuilder classPath = new StringBuilder();
        classPath.append(constructClassPath());

        // add EventFlow directories
        //
        for (File eventFlowDirectory : eventflowDirectories) {
            classPath.append(File.pathSeparatorChar);
            classPath.append(eventFlowDirectory.getAbsolutePath());
        }

        if (liveviewDirectory != null) {
            classPath.append(File.pathSeparatorChar);
            classPath.append(liveviewDirectory.getAbsolutePath());
        }
        return classPath.toString();
    }

    /**
     * Scan for test classes in the same way that surefire does, and return a
     * list of class names (in Java binary notation).
     *
     * @return list of test classes to execute on the server.
     */
    String[] scanForTests() {

        // to tests to find
        //
        if (!testOutputDirectory.exists()) {
            return new String[0];
        }

        List<String> includeList;
        List<String> excludeList;

        // If "test" is set, this overrides the includes/excludes parameters
        //
        if (test != null) {
            // If we are running a single test, make it conform to the
            // "includes" format:
            // FooTest -> **/FooTest.java

            includeList = new ArrayList<>();
            excludeList = new ArrayList<>();

            for (String testRegex : test.split(",")) {
                // Strip ".java" off the end if it exists:
                if (testRegex.endsWith(".java")) {
                    testRegex = testRegex.substring(0, testRegex.length() - 5);
                }

                // Allow paths delimited by '.' or '/'
                testRegex = testRegex.replace('.', '/');
                includeList.add("**/" + testRegex + ".java");
            }
        } else {
            includeList = includes;
            excludeList = excludes;

            // Set default value for includes, excludes (not convenient with the
            // default-value parameter annotation):
            //
            if (includeList == null || includeList.isEmpty()) {
                includeList = new ArrayList<>();
                includeList.add("**/Test*.java");
                includeList.add("**/*Test.java");
                includeList.add("**/*TestCase.java");
                includeList.add("**/*TestSuite*.java");
            }

            if (excludeList == null || excludeList.isEmpty()) {
                excludeList = new ArrayList<>();
                excludeList.add("**/*$*");
            }
        }

        DirectoryScanner scanner = new DirectoryScanner();

        scanner.setBasedir(testOutputDirectory);

        scanner.setIncludes(processIncludesExcludes(includeList));
        scanner.setExcludes(processIncludesExcludes(excludeList));

        scanner.scan();

        String[] files = scanner.getIncludedFiles();
        for (int i = 0; i < files.length; i++) {
            String thisTest = files[i];

            // Remove trailing ".extension", and convert path characters to
            // dots.
            // So: foo/bar/MyTest.class becomes foo.bar.MyTest.
            //
            thisTest = thisTest.substring(0, thisTest.indexOf("."));
            files[i] = thisTest.replace(File.separatorChar, '.');

            getLog().debug("Adding test case: " + files[i]);
        }

        return files;
    }

    /**
     * Construct a ClassLoader instance whose classpath includes everything from
     * classpathElements and additionalClasspathElements.
     *
     * @return a new ClassLoader instance.
     * @throws MojoExecutionException If invalid classpath elements are given.
     */
    private String constructClassPath() throws MojoExecutionException {
        StringBuilder buf = new StringBuilder();
        Artifact plugin;

        assert (project != null);

        // add in local test and compile directories
        //
        // note that we rely on the default maven lifecycle to copy main/test
        // resources into the target directory
        //
        for (Object o : classpathElements) {
            buf.append(File.pathSeparatorChar);
            buf.append(o.toString());
        }
        if (additionalClasspathElements != null && additionalClasspathElements.size() > 0) {
            for (Object o : additionalClasspathElements) {
                buf.append(File.pathSeparatorChar);
                buf.append(o.toString());
            }
        }

        // add in EventFlow (if found) - needed by any dependent fragments)
        //
        File testEventflowDirectory = new File(project.getBuild().getDirectory(), "eventflow");
        if (testEventflowDirectory.exists()) {
            buf.append(File.pathSeparatorChar);
            buf.append(testEventflowDirectory.getAbsolutePath());
        }

        // include a classpath element for this plugin (we need the Runner):
        //
        assert (project.getPluginArtifactMap() != null);
        plugin = project.getPluginArtifactMap().get("com.tibco.ep:ep-maven-plugin");
        if (plugin == null) {
            throw new MojoExecutionException("Cannot locate an artifact for plugin "
                + "com.tibco.ep:ep-maven-plugin." + " Add this to the plugins section of your POM.");
        }
        buf.append(File.pathSeparatorChar);
        buf.append(getArtifactPath(plugin));

        // include a classpath element for the base surefire API:
        //
        plugin = (Artifact) pluginArtifactMap.get("org.apache.maven.surefire:surefire-api");
        if (plugin != null) {
            buf.append(File.pathSeparatorChar);
            buf.append(getArtifactPath(plugin));
        }

        // locate dependencies, filtering as needed, and add to classpath
        //
        final ArtifactFilter filter = artifact -> {
            // Skip binary archives
            //
            if (artifact.getType().equals("tgz") || artifact.getType().equals("zip")) {
                return false;
            }

            String scope = artifact.getScope();
            String groupId = artifact.getGroupId();
            String artifactId = artifact.getArtifactId();

            // skip specific streambase dependencies that are in the runtime
            //
            if (groupId.equals("com.tibco.ep.thirdparty")) {
                return !artifactId.equals("tibco-sb-sbtest-unit") &&
                    !artifactId.equals("tibco-sb-sbclient") &&
                    !artifactId.equals("tibco-sb-sbserver");
            }
            return true;
        };

        try {
            // Note that this is using a deprecated API ... however, everyone
            // else seems to use this as well !
            //
            DefaultArtifactResolver actualResolver = (DefaultArtifactResolver) artifactResolver;
            ArtifactResolutionResult result = actualResolver
                .resolveTransitively(getProjectDependencies(), project.getArtifact(),
                    project.getManagedVersionMap(), localRepository, project
                        .getRemoteArtifactRepositories(),
                    null, filter);
            for (Artifact artifact : (Set<Artifact>) result.getArtifacts()) {
                buf.append(File.pathSeparatorChar);
                buf.append(artifact.getFile());
            }
        } catch (final ArtifactResolutionException | ArtifactNotFoundException e) {
            getLog().warn(e);
        }

        getLog().debug("Full classpath: [" + buf.toString() + "]");

        return buf.toString();
    }

    /**
     * Merge node specific test reports to a single directory
     */
    private void mergeTestReports() {

        if (!reportsDirectory.exists()) {
            // no test reports found
            return;
        }

        File[] reportsFiles = reportsDirectory.listFiles();
        if (reportsFiles != null) {
            for (File nodeDirectory : reportsFiles) {
                File[] nodeFiles = nodeDirectory.listFiles();
                if (nodeFiles != null) {
                    for (File testFile : nodeFiles) {
                        File target = new File(reportsDirectory, nodeDirectory
                            .getName() + "-" + testFile.getName());
                        try {
                            Files.move(testFile.toPath(), target
                                .toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            getLog().warn(e.getCause());
                        }
                    }
                }
            }
        }
    }

    /**
     * @return A new JUNIT test runner
     */
    public JunitTests newJunitTest() {
        return new JunitTests();
    }

    Deployment newDeployment(FragmentType fragmentType, String fragment) {
        return new Deployment(fragmentType, fragment);
    }

    /**
     * Class to run JUNIT base tests
     */
    class JunitTests {
        private List<File> eventFlowDirectories;
        private File liveviewDirectory;

        private JunitTests() {
            eventFlowDirectories = new ArrayList<>();
            liveviewDirectory = null;
        }

        /**
         * @param eventFlowDirectories The event flow directories
         * @return This
         */
        public JunitTests withEventFlowDirectories(File[] eventFlowDirectories) {
            this.eventFlowDirectories = toNonNullList(eventFlowDirectories);
            return this;
        }

        /**
         * @param liveViewDirectory The live view directory
         * @return This
         */
        public JunitTests withLiveViewDirectory(File liveViewDirectory) {
            this.liveviewDirectory = liveViewDirectory;
            return this;
        }

        /**
         * Run the tests
         *
         * @throws MojoExecutionException Wraps any execution failure
         */
        void run() throws MojoExecutionException {
            // Set some application fragment parameters
            //
            List<String> applicationArguments = new ArrayList<>();
            String[] testClasses = scanForTests();
            if (testClasses.length == 0) {
                getLog().info("No test cases found");
                return;
            }

            applicationArguments.add("" + BaseTestMojo.this.useSystemExit);
            applicationArguments.addAll(Arrays.asList(testClasses));

            // set jenkins property so it can find the reports when run as a maven job
            //
            Properties modelProperties = project.getModel().getProperties();
            String propertyName = "jenkins." + mojoExecution.getExecutionId() + ".reportsDirectory";
            modelProperties.setProperty(propertyName, reportsDirectory.getAbsolutePath());
            project.getModel().setProperties(modelProperties);

            if (testMain != null) {
                List<String> fullJavaOptions = new ArrayList<>();
                if (systemPropertyVariables != null && systemPropertyVariables.size() > 0) {
                    for (Entry<String, String> entry : systemPropertyVariables.entrySet()) {
                        fullJavaOptions.add("-D" + entry.getKey() + "=" + entry.getValue());
                    }
                }

                newDeployment(FragmentType.JAVA, testMain)
                    .withServiceName(clusterName)
                    .withJavaOptions(fullJavaOptions)
                    .withApplicationArguments(applicationArguments)
                    .withEventFlowDirectories(eventFlowDirectories)
                    .withLiveViewDirectory(liveviewDirectory)
                    .withWait(false)
                    .run();
            }

            String thisServiceName = serviceName;
            if (serviceName == null || serviceName.length() == 0) {
                thisServiceName = clusterName;
            }

            // form list of java options - set in pom + command line
            //
            List<String> fullJavaOptions = new ArrayList<>();
            if (javaOptions != null && javaOptions.length > 0) {
                fullJavaOptions.addAll(Arrays.asList(javaOptions));
            }
            if (optionsProperty != null && optionsProperty.length > 0) {
                fullJavaOptions.addAll(Arrays.asList(optionsProperty));
            }
            if (systemPropertyVariables != null && systemPropertyVariables.size() > 0) {
                for (Entry<String, String> entry : systemPropertyVariables.entrySet()) {
                    fullJavaOptions.add("-D" + entry.getKey() + "=" + entry.getValue());
                }
            }

            if (BaseTestMojo.this.ignoreLeaks != null && BaseTestMojo.this.ignoreLeaks.length > 0) {
                String ignoreLeaks = String.join(",", BaseTestMojo.this.ignoreLeaks);
                fullJavaOptions.add("-DIgnoreLeaks=" + ignoreLeaks);
            }

            newDeployment(FragmentType.JAVA, Runner.class.getName())
                .withServiceName(thisServiceName)
                .withJavaOptions(fullJavaOptions)
                .withApplicationArguments(applicationArguments)
                .withEventFlowDirectories(eventFlowDirectories)
                .withLiveViewDirectory(liveviewDirectory)
                .withWait(true)
                .run();
        }
    }

    class Deployment {
        private final FragmentType fragmentType;
        private final String fragment;
        private final List<String> javaOptions = new ArrayList<>();
        private final List<String> applicationArguments = new ArrayList<>();
        private final List<File> eventFlowDirectories = new ArrayList<>();
        private String deployServiceName;
        private File liveViewDirectory;
        private boolean wait;

        Deployment(FragmentType fragmentType, String fragment) {
            this.fragmentType = fragmentType;
            this.fragment = fragment;

            wait = true;
        }

        public Deployment withServiceName(String serviceName) {
            this.deployServiceName = serviceName;
            return this;
        }

        public Deployment withLiveViewDirectory(File liveViewDirectory) {
            this.liveViewDirectory = liveViewDirectory;
            return this;
        }

        public Deployment withWait(boolean wait) {
            this.wait = wait;
            return this;
        }

        public Deployment withJavaOptions(List<String> javaOptions) {
            this.javaOptions.addAll(javaOptions);
            return this;
        }

        public Deployment withApplicationArguments(List<String> applArguments) {
            this.applicationArguments.addAll(applArguments);
            return this;
        }

        public Deployment withEventFlowDirectories(List<File> eventFlowDirectories) {
            this.eventFlowDirectories.addAll(eventFlowDirectories);
            return this;
        }

        void run() throws MojoExecutionException {

            int actualDiscoveryPort = getDiscoveryPort();

            String classPath = getClassPath(eventFlowDirectories, liveViewDirectory);
            System.setProperty("java.class.path", classPath);

            doSetEnvironment();

            IDestination destination = newDestination(deployServiceName);
            destination.setDiscoveryPort(actualDiscoveryPort);

            AbstractDeployFragmentCommandBuilder deployCommandBuilder = destination
                .newDeployFragmentCommand(fragmentType, fragment)
                .withDestination(destination);

            //  Set deploy options.
            //
            List<String> exeParams = new ArrayList<>(javaOptions);


            //  Copy node options so we can create unique values
            //
            Map<String, String> finalNodeOptions = new HashMap<>();

            // ignore user's options file if it exists
            //
            finalNodeOptions.put("ignoreoptionsfile", "true");

            // build type
            //
            if (buildtype != null && buildtype != BuldType.ALL && buildtype != BuldType.TESTCOV) {
                finalNodeOptions.put("buildtype", buildtype.toString());
            }

            if (nodeOptions != null) {
                finalNodeOptions.putAll(nodeOptions);
            }

            if (nodeOptionsProperty != null) {
                for (String s : nodeOptionsProperty) {
                    if (s.startsWith("ignoreoptionsfile")) {
                        finalNodeOptions.remove("ignoreoptionsfile");
                    }
                }
            }

            for (Entry<String, String> entry : finalNodeOptions.entrySet()) {
                exeParams.add(entry.getKey() + "=" + entry.getValue());
            }

            // add on any command-line node options
            //
            if (nodeOptionsProperty != null) {
                exeParams.addAll(Arrays.asList(nodeOptionsProperty));
            }

            // enable assertions
            //
            exeParams.add("-enableassertions");

            // java classpath
            //
            exeParams.add("-Djava.class.path=" + classPath);

            // StreamBase test settings
            //
            List<String> directories = new ArrayList<>();
            for (File eventflowDirectory : eventFlowDirectories) {
                directories.add(eventflowDirectory.getAbsolutePath());
            }
            if (liveViewDirectory != null) {
                directories.add(liveViewDirectory.getAbsolutePath());
            }
            directories.addAll(project.getTestCompileSourceRoots());

            // add any sub directories
            //
            List<String> subDirectories = new ArrayList<>();
            for (String path : directories) {
                File streambaseSourceDirectory = new File(path);
                if (streambaseSourceDirectory.exists()) {
                    try {
                        Files.walkFileTree(streambaseSourceDirectory
                            .toPath(), new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                                if (dir.toFile().exists()) {
                                    subDirectories.add(dir.toString());
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        throw new MojoExecutionException("Failed to read resource directory: " + e
                            .getMessage(), e);
                    }
                }
            }
            directories.addAll(subDirectories);

            // pass through maven settings
            //
            exeParams.add("-D" + REPORTS_DIRECTORY + "=" + reportsDirectory.getAbsolutePath());
            exeParams.add("-Dcom.tibco.ep.dtm.fragment.version=" + project.getVersion());
            exeParams
                .add("-Dcom.tibco.ep.dtm.fragment.identifier=" + project
                    .getGroupId() + "." + project
                    .getArtifactId());
            exeParams.add("-Djava.library.path=" + getFullNativePath());
            getLog().debug("Execution options = " + exeParams);
            deployCommandBuilder.withExecutionOptions(exeParams);

            getLog().debug("Application options = " + applicationArguments);
            deployCommandBuilder.withApplicationArguments(applicationArguments);

            RuntimeCommandRunner deployRunner = newCommandRunner(deployCommandBuilder, deployServiceName, "node " + deployServiceName)
                .recordOutput(false)
                .wait(wait)

                .shutdownHook(cmd -> {
                    getLog().error("Test was aborted - attempting to clean up");
                    stopNodes(deployServiceName);
                    removeNodes(deployServiceName);
                })

                .onError((runner, resultCode) -> {
                    runner.removeShutdownHook();

                    // avoid process leak on unit test failure
                    //
                    if (!skipStop) {
                        stopNodes(deployServiceName);
                        terminateNodes(deployServiceName);
                    }

                    throw new MojoExecutionException("Launching junit test cases failed: node " + deployServiceName + " error code " + resultCode);
                });
            deployRunner.run();

            if (!wait) {
                try {
                    //  FIX THIS (FL): is this needed in any way ?
                    // 10 seconds from the old deploytimeout value
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                    getLog().warn("Interrupted", e);
                }
            }
            
            mergeTestReports();
        }
    }
}
