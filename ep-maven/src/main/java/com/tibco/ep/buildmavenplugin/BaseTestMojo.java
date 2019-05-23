/*******************************************************************************
 * Copyright (C) 2018, TIBCO Software Inc.
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
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
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

import com.tibco.ep.buildmavenplugin.surefire.Runner;

/**
 * Base test
 *
 */
abstract class BaseTestMojo extends BaseExecuteMojo {

    // maven read-only parameters
    //

    /**
     * The classpath elements of the project being tested.
     * 
     * @since 1.0.0
     */
    @Parameter(property="project.testClasspathElements", readonly = true, required = true)
    private List<String> classpathElements;

    /**
     * The directory to search for generated test classes of the project being
     * tested.
     * 
     * @since 1.0.0
     */
    @Parameter(property="project.build.testOutputDirectory", readonly = true, required = true)
    private File testOutputDirectory;

    /**
     * The directory to search for generated classes of the project being
     * tested.
     * 
     * @since 1.0.0
     */
    @Parameter(property="project.build.outputDirectory", readonly = true, required = true)
    private File classesDirectory;

    // maven user parameters
    //

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
    @Parameter(property="test")
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
    @Parameter( property = "options", readonly=true )
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
     * @since 1.0.0
     */
    @Parameter
    Map<String, String> nodeOptions;

    /**
     * <p>Additional nodeOptions to append</p>
     * 
     * @since 1.1.0
     */
    @Parameter( property = "nodeOptions", readonly=true )
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
     * Run junit based test cases
     * 
     * @param failOnError if true, fail on error
     * @param eventFlowDirectories eventflow source directories
     * @param liveviewDirectory liveview source directory
     * 
     * @throws MojoExecutionException node install failed
     */
    void runJunitTests(final boolean failOnError, File[] eventFlowDirectories, File liveviewDirectory) throws MojoExecutionException {

        // Set some application fragment parameters
        //
        List<String> applArguments = new ArrayList<>();
        String[] testClasses = scanForTests();
        if (testClasses.length == 0) {
            getLog().info("No test cases found");
            return;
        }

        applArguments.add(""+this.useSystemExit);
        applArguments.addAll(Arrays.asList(testClasses));

        // set jenkins property so it can find the reports when run as a maven job
        //
        Properties modelProperties = project.getModel().getProperties();
        String propertyName = "jenkins."+mojoExecution.getExecutionId()+".reportsDirectory";
        modelProperties.setProperty(propertyName, reportsDirectory.getAbsolutePath());
        project.getModel().setProperties(modelProperties);

        if (testMain != null) {
            ArrayList<String> fullJavaOptions = new ArrayList<String>();
            if (systemPropertyVariables != null && systemPropertyVariables.size() >0) {
                for (Entry<String, String> entry : systemPropertyVariables.entrySet()) {
                    fullJavaOptions.add("-D"+entry.getKey() + "=" + entry.getValue());
                }
            }
            deploy(failOnError, "JAVA", testMain, clusterName, userName, password, fullJavaOptions.toArray(new String[fullJavaOptions.size()]), applArguments, eventFlowDirectories, liveviewDirectory, false);
        }

        String thisServiceName=serviceName;
        if (serviceName == null ||serviceName.length() == 0) {
            thisServiceName = clusterName;
        }
        
        // form list of java options - set in pom + command line
        //
        ArrayList<String> fullJavaOptions = new ArrayList<String>();
        if (javaOptions != null && javaOptions.length > 0) {
            fullJavaOptions.addAll(Arrays.asList(javaOptions));
        }
        if (optionsProperty != null && optionsProperty.length > 0) {
            fullJavaOptions.addAll(Arrays.asList(optionsProperty));
        }
        if (systemPropertyVariables != null && systemPropertyVariables.size() >0) {
            for (Entry<String, String> entry : systemPropertyVariables.entrySet()) {
                fullJavaOptions.add("-D"+entry.getKey() + "=" + entry.getValue());
            }
        }
        
        if (this.ignoreLeaks != null && this.ignoreLeaks.length >0) {
            String ignoreLeaks = String.join(",", this.ignoreLeaks);
            fullJavaOptions.add("-DIgnoreLeaks="+ignoreLeaks);
        }
        
        deploy(failOnError, "JAVA", Runner.class.getName(), thisServiceName, userName, password, fullJavaOptions.toArray(new String[fullJavaOptions.size()]), applArguments, eventFlowDirectories, liveviewDirectory, true);
    }

    /**
     * Deploy fragment
     * 
     * @param failOnError if true, fail on error
     * @param fragmentType fragment type JAVA, STREAMBASE, LIVEVIEW or EVENT_PROCESSING
     * @param fragment fragment name
     * @param deployServiceName deploy service name
     * @param userName user name
     * @param password password
     * @param deployJavaOptions java options 
     * @param applArguments arguments
     * @param eventflowDirectories eventflow source directories
     * @param liveviewDirectory liveview source directory
     * @param wait if true wait for the command to complete
     * 
     * @throws MojoExecutionException node install failed
     */
    void deploy(final boolean failOnError, final String fragmentType, final String fragment, final String deployServiceName, final String userName, final String password, final String[] deployJavaOptions, final List<String> applArguments, final File[] eventflowDirectories, final File liveviewDirectory, final boolean wait) throws MojoExecutionException {

        // attempt to remove node if install failed
        //
        Thread hook = new Thread() {
            public void run() {
                getLog().error("Test was aborted - attempting to clean up");
                try {
                    stopNodes(deployServiceName, userName, password, false);
                } catch (MojoExecutionException e) {
                }
                try {
                    removeNodes(deployServiceName, userName, password, false);
                } catch (MojoExecutionException e) {
                }
            }
        };
        Runtime.getRuntime().addShutdownHook(hook);
        
        // if discovery port is set use it, otherwise use a unused & persistent value
        //
        int actualDiscoveryPort;
        if (discoveryPort == null) {
            actualDiscoveryPort = findFreeUDPPort(discoveryPortFile);
        } else {
            actualDiscoveryPort=discoveryPort;
        }

        // Set classpath for the test run
        //
        String classPath = constructClassPath();
        
        // add eventflow directories
        //
        if (eventflowDirectories != null) {
            for (File eventFlowDirectory : eventflowDirectories) {
                classPath=classPath+File.pathSeparatorChar+eventFlowDirectory.getAbsolutePath();
            }
        }
        if (liveviewDirectory != null) {
            classPath=classPath+File.pathSeparatorChar+liveviewDirectory.getAbsolutePath();
        }
        System.setProperty("java.class.path", classPath);

        Map<String, String> params = new HashMap<String, String>();

        try {

            setEnvironment();

            Object destination;
            if (userName != null && userName.length() > 0 ) {
                destination = dtmDestinationConstructorUsernamePassword.newInstance(deployServiceName, dtmContext, userName, password);
            } else {
                destination = dtmDestinationConstructor.newInstance(deployServiceName, dtmContext);
            }
            Method m = destination.getClass().getMethod("addDiscoveryHost", java.lang.String.class);
            if (discoveryHosts != null && discoveryHosts.length > 0) {
                for (String discoveryHost : discoveryHosts) {
                    m.invoke(destination, discoveryHost);
                }
            }
            
            m = destination.getClass().getMethod("setDiscoveryPort", int.class);
            m.invoke(destination, actualDiscoveryPort);

            @SuppressWarnings({ "unchecked", "rawtypes" })
            Object command = dtmDeployFragmentCommandConstructor.newInstance(Enum.valueOf((Class<Enum>) fragmentTypeClass, fragmentType), fragment, destination);

            // Set some execution options
            //
            List<String> exeParams = new ArrayList<>();

            if (deployJavaOptions != null && deployJavaOptions.length > 0) {
                exeParams.addAll(Arrays.asList(deployJavaOptions));
            }
            
            // copy node options so we can create unique values
            //
            HashMap<String, String> finalNodeOptions = new HashMap<String, String>();
            
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
                for (String s : nodeOptionsProperty) {
                    exeParams.add(s);
                }
            }
            
            // enable assertions
            //
            exeParams.add("-enableassertions");

            // java classpath
            //
            exeParams.add("-Djava.class.path="+classPath);

            // StreamBase test settings
            //
            List<String> directories = new ArrayList<>();
            if (eventflowDirectories != null) {
                for (File eventflowDirectory : eventflowDirectories) {
                    directories.add(eventflowDirectory.getAbsolutePath());
                }
            }
            if (liveviewDirectory != null) {
                directories.add(liveviewDirectory.getAbsolutePath());
            }
            directories.addAll(project.getTestCompileSourceRoots());

            // add any sub directories
            //
            List<String> subDirectories = new ArrayList<String>();
            for (String path : directories) {
                File streambaseSourceDirectory = new File(path);
                if (streambaseSourceDirectory.exists()) {
                    try {
                        Files.walkFileTree(streambaseSourceDirectory.toPath(), new SimpleFileVisitor<Path>() {
                            @Override
                            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {

                                if (dir.toFile().exists()) {
                                    subDirectories.add(dir.toString());
                                }
                                return FileVisitResult.CONTINUE;
                            }
                        });
                    } catch (IOException e) {
                        throw new MojoExecutionException("Failed to read resource directory: " + e.getMessage(), e);
                    }
                }
            }
            directories.addAll(subDirectories);

            // pass through maven settings
            //
            exeParams.add("-D"+REPORTS_DIRECTORY+"="+reportsDirectory.getAbsolutePath());
            exeParams.add("-Dcom.tibco.ep.dtm.fragment.version="+project.getVersion());
            exeParams.add("-Dcom.tibco.ep.dtm.fragment.identifier="+project.getGroupId()+"."+project.getArtifactId());

            // if there are any native libraries, set library path
            //
            String osName = System.getProperty("os.name").replaceAll("\\s","");
            if (osName.startsWith("Windows")) {
                osName = "Windows";
            }
            String fullNativePath="";
            for (String suffix : new String[] { "gpp", "msvc" }) {
                for (String type : new String[] { "jni", "shared" }) {
                    File nativePath = new File(project.getBuild().getDirectory()+File.separator+"nar"+
                            File.separator+"lib"+
                            File.separator+System.getProperty("os.arch")+"-"+osName+"-"+suffix+
                            File.separator+type);
                    if (nativePath.isDirectory()) {
                        if (!fullNativePath.isEmpty()) {
                            fullNativePath+=File.pathSeparator;
                        }
                        fullNativePath+=nativePath.getAbsolutePath();
                    }
                }
            }
            exeParams.add("-Djava.library.path="+fullNativePath);
            
            getLog().debug("Execution options = "+exeParams.toString());
            try {
                dtmDeployFragmentCommand_setExecutionOptions.invoke(command, exeParams);
            } catch (IllegalArgumentException e) {
                Runtime.getRuntime().removeShutdownHook(hook);
                throw new MojoExecutionException("Invalid arguments to management API "+dtmDeployFragmentCommand_setExecutionOptions.toString());
            }

            getLog().debug("Application options = "+applArguments.toString());
            try {
                dtmDeployFragmentCommand_setApplicationArguments.invoke(command, applArguments);
            } catch (IllegalArgumentException e) {
                Runtime.getRuntime().removeShutdownHook(hook);
                throw new MojoExecutionException("Invalid arguments to management API "+dtmDeployFragmentCommand_setApplicationArguments.toString());
            }
            
            try {
                dtmDeployFragmentCommand_execute.invoke(command, params, createMonitor("junit", deployServiceName, failOnError, false));
            } catch (IllegalArgumentException e) {
                Runtime.getRuntime().removeShutdownHook(hook);
                throw new MojoExecutionException("Invalid arguments to management API "+dtmDeployFragmentCommand_execute.toString());
            }
            
            if (wait) {
                int rc;
                try {
                    rc = (int)dtmDeployFragmentCommand_waitForCompletion.invoke(command);
                } catch (IllegalArgumentException e) {
                    Runtime.getRuntime().removeShutdownHook(hook);
                    throw new MojoExecutionException("Invalid arguments to management API "+dtmDeployFragmentCommand_waitForCompletion.toString());
                }
                if (failOnError && rc != 0) {
                    Runtime.getRuntime().removeShutdownHook(hook);
                    throw new MojoExecutionException("launching junit test cases failed failed: node " + deployServiceName + " error code " + rc);
                }
            } else {
                try {
                    // 10 seconds from the old deploytimeout value
                    Thread.sleep(10000);
                } catch (InterruptedException e) {
                }
            }

            mergeCobertura();
            mergeTestReports();

        } catch (InstantiationException e) {
            Runtime.getRuntime().removeShutdownHook(hook);
            throw new MojoExecutionException(e.getMessage());
        } catch (IllegalAccessException e) {
            Runtime.getRuntime().removeShutdownHook(hook);
            throw new MojoExecutionException(e.getMessage());
        } catch (InvocationTargetException e) {
            Runtime.getRuntime().removeShutdownHook(hook);
            throw new MojoExecutionException(e.getCause().getMessage());
        } catch (NoSuchMethodException e) {
            Runtime.getRuntime().removeShutdownHook(hook);
            throw new MojoExecutionException(e.getMessage());
        } catch (SecurityException e) {
            Runtime.getRuntime().removeShutdownHook(hook);
            throw new MojoExecutionException(e.getMessage());
        } 

        Runtime.getRuntime().removeShutdownHook(hook);
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
        if ( !testOutputDirectory.exists() ) {
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

            includeList = new ArrayList<String>();
            excludeList = new ArrayList<String>();

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

        if (includeList != null) {
            scanner.setIncludes(processIncludesExcludes(includeList));
        }
        if (excludeList != null) {
            scanner.setExcludes(processIncludesExcludes(excludeList));
        }

        scanner.scan();

        String[] files = scanner.getIncludedFiles();
        for (int i = 0; i < files.length; i++)
        {
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

    /**
     * Construct a ClassLoader instance whose classpath includes everything from
     * classpathElements and additionalClasspathElements.
     *
     * @return a new ClassLoader instance.
     * @throws MojoExecutionException
     *             If invalid classpath elements are given.
     */
    String constructClassPath() throws MojoExecutionException {
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

        // include a classpath element for this plugin (we need the Runner):
        //
        assert (project.getPluginArtifactMap() != null);
        plugin = (Artifact) project.getPluginArtifactMap().get("com.tibco.ep:ep-maven-plugin");
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
        final ArtifactFilter filter = new ArtifactFilter() {
            @Override
            public boolean include(Artifact artifact) {
                // Skip binary archives
                //
                if (artifact.getType().equals("tgz") || artifact.getType().equals("zip")) {
                    return false;
                }

                String scope = artifact.getScope();
                String groupId = artifact.getGroupId();
                String artifactId = artifact.getArtifactId();

                // skip provided
                //
                if (scope != null && scope.equals(Artifact.SCOPE_PROVIDED)) {

                    // cobertura exception
                    //
                    if (artifactId.equals("cobertura") && groupId.equals("net.sourceforge.cobertura")) {
                        for (String goal : session.getGoals()) {
                            if (goal.equals("cobertura:cobertura")) {               
                                return true;
                            }
                        }
                    }

                    // cobertura not required
                    //
                    return false;
                }

                // skip specific streambase depndencies that are in the runtime
                //
                if (groupId.equals("com.tibco.ep.thirdparty")) {
                    if (artifactId.equals("tibco-sb-sbtest-unit") ||
                            artifactId.equals("tibco-sb-sbclient") ||
                            artifactId.equals("tibco-sb-sbserver")) {
                        return false;
                    }
                }
                return true;
            }
        };

        try {       
            // Note that this is using a deprecated API ... however, everyone
            // else seems to use this as well !
            //     
            DefaultArtifactResolver actualResolver = (DefaultArtifactResolver)artifactResolver;
            ArtifactResolutionResult result = actualResolver.resolveTransitively(getProjectDependencies(), project.getArtifact(),
                    project.getManagedVersionMap(), localRepository, project.getRemoteArtifactRepositories(),
                    null, filter );
            for (Artifact artifact : (Set<Artifact>)result.getArtifacts()) {
                buf.append(File.pathSeparatorChar);
                buf.append(artifact.getFile());
            }
        }
        catch ( final ArtifactResolutionException e ) {
            e.printStackTrace();
        }
        catch ( final ArtifactNotFoundException e ) {
            e.printStackTrace();
        }

        getLog().debug("Full classpath: [" + buf.toString() + "]");

        return buf.toString();
    }

    /**
     * Merge node specific report to a cluster report in the expected location
     */
    void mergeCobertura() {

        // check to see if we've got anything to merge
        //
        File coberturaDirectory = new File(reportsDirectory,"cobertura");
        if (!coberturaDirectory.exists()) {
            return;
        }
        ArrayList<String> files = new ArrayList<String>();
        File[] coberturaFiles = coberturaDirectory.listFiles();
        if (coberturaFiles == null) {
            return;
        }
        for (File coberturaFile : coberturaFiles) {
            if (coberturaFile.isFile() && coberturaFile.getName().endsWith(".cobertura.ser")) {
                files.add(coberturaFile.getAbsolutePath());
            }
        }

        if (files.size() > 0) {
            final MyClassloader coverageClassLoader = new MyClassloader(new URL[0], this.getClass().getClassLoader());
            try {

                // here we simulate cobertura-merge.sh script
                //

                // locate the jars we need and load
                //
                // should this be easier ?
                //
                Set<Artifact> coverageDependencies = new HashSet<Artifact>();

                final ArtifactFilter filter = new ArtifactFilter() {
                    @Override
                    public boolean include(Artifact artifact) {
                        if (artifact.getArtifactId().equals("cobertura") && artifact.getGroupId().equals("net.sourceforge.cobertura")) {
                            coverageDependencies.add(artifact);
                            return true;
                        }
                        if (artifact.getArtifactId().equals("oro") && artifact.getGroupId().equals("oro")) {
                            coverageDependencies.add(artifact);
                            return true;
                        }
                        if (artifact.getArtifactId().equals("slf4j-api") && artifact.getGroupId().equals("org.slf4j")) {
                            coverageDependencies.add(artifact);
                            return true;
                        }
                        if (artifact.getArtifactId().equals("logback-core") && artifact.getGroupId().equals("ch.qos.logback")) {
                            coverageDependencies.add(artifact);
                            return true;
                        }
                        if (artifact.getArtifactId().equals("logback-classic") && artifact.getGroupId().equals("ch.qos.logback")) {
                            coverageDependencies.add(artifact);
                            return true;
                        }
                        return false;
                    }
                };
                try {        
                    DefaultArtifactResolver actualResolver = (DefaultArtifactResolver)artifactResolver;
                    actualResolver.resolveTransitively(getProjectDependencies(), project.getArtifact(),
                            project.getManagedVersionMap(), localRepository, project.getRemoteArtifactRepositories(),
                            null, filter );
                    for (Artifact artifact : coverageDependencies) {
                        URL url = new File(getArtifactPath(artifact)).toURI().toURL();
                        coverageClassLoader.addURL(url);
                    }
                }
                catch ( final ArtifactResolutionException e ) {
                }
                catch ( final ArtifactNotFoundException e ) {
                }


                // find cobertura class, constructors and methods
                //
                Class<?> argumentsBuilderClass = Class.forName("net.sourceforge.cobertura.dsl.ArgumentsBuilder", true, coverageClassLoader);
                Constructor<?> argumentsBuilderConstructor = argumentsBuilderClass.getConstructor();
                Method argumentsBuilder_addFileToMerge = argumentsBuilderClass.getMethod("addFileToMerge", String.class);
                Method argumentsBuilder_setDataFile = argumentsBuilderClass.getMethod("setDataFile", String.class);
                Method argumentsBuilder_build = argumentsBuilderClass.getMethod("build");
                Class<?> argumentsClass = Class.forName("net.sourceforge.cobertura.dsl.Arguments", true, coverageClassLoader);
                Class<?> coberturaClass = Class.forName("net.sourceforge.cobertura.dsl.Cobertura", true, coverageClassLoader);
                Constructor<?> coberturaClassConstructor = coberturaClass.getConstructor(argumentsClass);
                Method cobertura_merge = coberturaClass.getMethod("merge");
                Method cobertura_saveProjectData = coberturaClass.getMethod("saveProjectData");


                Object builder = argumentsBuilderConstructor.newInstance();    

                // add in source files
                //
                for (String sourceFile : files) {
                    getLog().info("Reading node coverage report "+sourceFile);
                    argumentsBuilder_addFileToMerge.invoke(builder, sourceFile);
                }

                // add in destination
                //
                String coverageOutput = project.getBuild().getDirectory()+File.separator+"cobertura"+File.separator+"cobertura.ser";
                argumentsBuilder_setDataFile.invoke(builder,  coverageOutput);

                getLog().info("Writing cluster coverage report "+coverageOutput);

                // and merge
                //
                cobertura_saveProjectData.invoke(cobertura_merge.invoke(coberturaClassConstructor.newInstance(argumentsBuilder_build.invoke(builder))));

            } catch (ClassNotFoundException e) {
                getLog().warn("Cannot find cobertura class to start merge "+e.getMessage());
                e.printStackTrace();
            } catch (NoSuchMethodException e) {
                getLog().warn("Cannot find cobertura class to start merge "+e.getMessage());
                e.printStackTrace();
            } catch (SecurityException e) {
                getLog().warn("Cannot find cobertura class to start merge "+e.getMessage());
                e.printStackTrace();
            } catch (IllegalAccessException e) {
                getLog().warn("Cannot find cobertura class to start merge "+e.getMessage());
                e.printStackTrace();
            } catch (IllegalArgumentException e) {
                getLog().warn("Cannot find cobertura class to start merge "+e.getMessage());
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                getLog().warn("Cannot find cobertura class to start merge "+e.getCause().getMessage());
                e.printStackTrace();
            } catch (MalformedURLException e) {
                getLog().warn("Cannot find cobertura class to start merge "+e.getMessage());
                e.printStackTrace();
            } catch (InstantiationException e) {
                getLog().warn("Cannot find cobertura class to start merge "+e.getMessage());
                e.printStackTrace();
            } finally {
                try {
                    coverageClassLoader.close();
                } catch (IOException e) {
                }
            }

        }
    }

    /**
     * Merge node specific test reports to a single directory
     */
    void mergeTestReports() {

        if (!reportsDirectory.exists() ) {
            // no test reports found
            return;
        }

        File[] reportsFiles = reportsDirectory.listFiles();
        if (reportsFiles != null) {
            for (File nodeDirectory : reportsFiles) {
                File[] nodeFiles = nodeDirectory.listFiles();
                if (nodeFiles != null) {
                    for (File testFile : nodeFiles) {
                        File target = new File(reportsDirectory, nodeDirectory.getName()+"-"+testFile.getName());
                        try {
                            Files.move(testFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
                        } catch (IOException e) {
                            getLog().warn(e.getCause());
                        }
                    }
                }
            }
        }
    }
}
