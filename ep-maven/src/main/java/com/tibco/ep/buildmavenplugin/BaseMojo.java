/*******************************************************************************
 * Copyright (C) 2018 - 2019, TIBCO Software Inc.
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.jar.JarOutputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.artifact.resolver.filter.ArtifactFilter;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.apache.maven.project.ProjectBuildingRequest;
import org.apache.maven.repository.RepositorySystem;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilder;
import org.apache.maven.shared.dependency.graph.DependencyGraphBuilderException;
import org.apache.maven.shared.dependency.graph.DependencyNode;
import org.apache.maven.shared.dependency.graph.traversal.DependencyNodeVisitor;
import org.sonatype.plexus.build.incremental.BuildContext;

/**
 * Base type
 *
 */
abstract class BaseMojo extends AbstractMojo {

    // maven components
    //

    /**
     * Maven resolver
     */
    @Component
    ArtifactResolver artifactResolver;

    /**
     * Maven repository system
     */
    @Component
    RepositorySystem repositorySystem;

    /**
     * Build context
     */
    @Component
    BuildContext buildContext;

    /**
     * Project helper
     */
    @Component
    MavenProjectHelper helper;

    @Component( hint = "default" )
    private DependencyGraphBuilder dependencyGraphBuilder;
    
    @Parameter( defaultValue = "${reactorProjects}", readonly = true, required = true )
    private List<MavenProject> reactorProjects;
    
    /**
     * Maven execution
     */
    @Parameter( defaultValue="${mojo}", readonly=true )
    MojoExecution execution;

    // maven read-only parameters
    //

    /**
     * Map of plugin artifacts.
     */
    @Parameter(property="plugin.artifactMap", readonly = true, required = true)
    Map<String,Artifact> pluginArtifactMap;

    /**
     * The current build session instance.
     */
    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    MavenSession session;  

    /**
     * Maven project
     */
    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    MavenProject project;

    /** 
     * Mojo execution 
     */ 
    @Parameter(defaultValue = "${mojoExecution}", readonly = true, required = true)
    MojoExecution mojoExecution; 

    /**
     * The plugin remote repositories declared in the pom.
     */
    @Parameter(property="project.pluginArtifactRepositories", readonly = true, required = true)
    List<ArtifactRepository> remoteRepositories;

    /**
     * Maven local repository
     */
    @Parameter( defaultValue = "${localRepository}", required = true, readonly = true )
    ArtifactRepository localRepository;

    /**
     * <p>Product home location.  This path is resolved in the following way :</p>
     * 
     * <ul>
     * <li>If property com.tibco.ep.ep-maven.product is set, use that, else</li>
     * <li>If environment variable TIBCO_EP_HOME is set, use that, else</li>
     * <li>Use localrepository/../product-group/product-artifact/product-version (so default is ~/.m2/product-group/product-artifact/product-version)</li>
     * </ul>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/productHome.svg" alt="pom">
     * 
     * <p>Example use on commandline:</p>
     * <img src="uml/productHome-commandline.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter(property = "com.tibco.ep.ep-maven.product")
    File productHome;

    /**
     * <p>List of class names to ignore in leak detection.  This is processed to a CSV value to include in
     * unit testing and in jar manifest file.</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/ignoreLeaks.svg" alt="pom">
     * 
     * @since 1.3.0
     */
    @Parameter
    String[] ignoreLeaks;
    
    // non-annotations
    //

    /**
     * maven property to use to skip start/stop/tests if no tests exist
     */
    protected static String TESTCASESFOUND_PROPERTY = "testCasesFound";
    
    /**
     * Application packaging and type
     */
    protected static String APPLICATION_TYPE = "ep-application";

    /**
     * Java packaging and type
     */
    protected static String JAVA_TYPE = "ep-java-fragment";

    /**
     * EventFlow packaging and type
     */
    protected static String EVENTFLOW_TYPE = "ep-eventflow-fragment";

    /**
     * LiveView packaging and type
     */
    protected static String LIVEVIEW_TYPE = "ep-liveview-fragment";

    /**
     * TIBCO Cloud Streaming packaging and type
     */
    protected static String TCS_TYPE = "ep-tcs-fragment";

    /**
     * system property for reports
     */
    protected static String REPORTS_DIRECTORY = "com.tibco.reportsdirectory";

    /**
     * DTM administration context
     */
    protected Object dtmContext = null;

    /**
     * Directory used to import other Eventflow fragments
     */
    protected static String IMPORT_DIRECTORY = "eventflow";

    // Artifact names
    //
    private static String DTM_GROUP_IDENTIFIER = "com.tibco.ep.dtm";
    private static String SB_GROUP_IDENTIFIER = "com.tibco.ep.sb";
    private static String SB_RT_GROUP_IDENTIFIER = "com.tibco.ep.sb.rt";
    private static String DTM_MANAGEMENT_ARTIFACT_IDENTIFIER = "management";
    private static String DTM_SDK_ARTIFACT_IDENTIFIER = "sdk";
    private static String SB_PRODUCT_ARTIFACT_PREFIX = "platform_";
    private static String SB_SUPPORT_ARTIFACT_PREFIX = "support_platform_";

    // administration class names
    //
    private static String DTMCONTEXT_CLASSNAME = "com.tibco.ep.dtm.management.DtmContext";
    private static String IDTMPROGRESS_CLASSNAME = "com.tibco.ep.dtm.management.IDtmProgress";
    private static String DTMNODE_CLASSNAME = "com.tibco.ep.dtm.management.DtmNode";
    private static String DTMINSTALLNODECOMMAND_CLASSNAME = "com.tibco.ep.dtm.management.DtmInstallNodeCommand";
    private static String DTMCOMMAND_CLASSNAME = "com.tibco.ep.dtm.management.DtmCommand";
    private static String DTMDESTINATION_CLASSNAME = "com.tibco.ep.dtm.management.DtmDestination";
    private static String DTMRESULTS_CLASSNAME = "com.tibco.ep.dtm.management.DtmResults";
    private static String DTMRESULTSET_CLASSNAME = "com.tibco.ep.dtm.management.DtmResultSet";
    private static String DTMROW_CLASSNAME = "com.tibco.ep.dtm.management.DtmRow";
    private static String DTMDEPLOYFRAGMENTCOMMAND_CLASSNAME = "com.tibco.ep.dtm.management.DtmDeployFragmentCommand";
    private static String FRAGMENTTYPE_CLASSNAME = "com.tibco.ep.dtm.management.DtmDeployFragmentCommand$FragmentType";
    private static String DTMFREEFORMCOMMAND_CLASSNAME = "com.tibco.ep.dtm.management.DtmFreeFormCommand";

    // administration classes
    //
    private Class<?> dtmNodeClass = null;
    private Class<?> dtmContextClass = null;
    private Class<?> dtmInstallNodeCommandClass = null;
    private Class<?> dtmCommandClass = null;
    private Class<?> dtmDestinationClass = null;
    private Class<?> dtmResultsClass = null;
    private Class<?> dtmResultsSetClass = null;
    private Class<?> dtmRowClass = null;
    private Class<?> dtmDeployFragmentCommandClass = null;
    private Class<?> dtmFreeFormCommandClass = null;
    private Class<?> iDtmProgressClass = null;
    
    /**
     * Fragment type Class
     */
    protected Class<?> fragmentTypeClass = null;

    // administration constructors
    //
    /** DtmNode constructor */ protected Constructor<?> dtmNodeConstructor = null;
    /** DtmNode username/password constructor */ protected Constructor<?> dtmNodeConstructorUsernamePassword = null;
    /** DtmInstallNode constructor */ protected Constructor<?> dtmInstallNodeConstructor = null;
    /** DtmCommand constructor */ protected Constructor<?> dtmCommandConstructor = null;
    /** DtmDestination constructor */ protected Constructor<?> dtmDestinationConstructor = null;
    /** DtmDestination username/password constructor */ protected Constructor<?> dtmDestinationConstructorUsernamePassword = null;
    /** DtmDeployFragment constructor */ protected Constructor<?> dtmDeployFragmentCommandConstructor = null;
    /** DtmFreeFormCommand constructor */ protected Constructor<?> dtmFreeFormCommandConstructor = null;

    // administration methods
    //
    /** DtmInstallNode execute method */ protected Method dtmInstallNodeCommand_execute = null;
    /** DtmInstallNode waitForCompletion method */ protected Method dtmInstallNodeCommand_waitForCompletion = null;
    /** DtmInstallNode cancel method */ protected Method dtmInstallNodeCommand_cancel = null;
    /** DtmCommand execute method */ protected Method dtmCommand_execute = null;
    /** DtmCommand waitForCompletion execute method */ protected Method dtmCommand_waitForCompletion = null;
    /** DtmDestination addDiscoveryHost execute method */ protected Method dtmDestination_addDiscoveryHost = null;
    /** DtmDestination setDiscoveryPort method */ protected Method dtmDestination_setDiscoveryPort = null;
    /** DtmDeployFragmentCommand execute method */ protected Method dtmDeployFragmentCommand_execute = null;
    /** DtmDeployFragmentCommand waitForCompletion method */ protected Method dtmDeployFragmentCommand_waitForCompletion = null;
    /** DtmDeployFragmentCommand setApplicationArguments method */ protected Method dtmDeployFragmentCommand_setApplicationArguments = null;
    /** DtmDeployFragmentCommand setExecutionOptions method */ protected Method dtmDeployFragmentCommand_setExecutionOptions = null;
    /** DtmDeployFragmentCommand cancel method */ protected Method dtmDeployFragmentCommand_cancel = null;
    /** DtmNode setAdministrationPort method */ protected Method dtmNode_setAdministrationPort = null;
    /** DtmNode setHostName method */ protected Method dtmNode_setHostName = null;
    /** DtmContext enableTracing method */ protected Method dtmContext_enableTracing = null;
    /** DtmResults getHeaders method */ protected Method dtmResults_getHeaders = null;
    /** DtmRow getColumns method */ protected Method dtmRow_getColumns = null;
    /** DtmResultsSet getRows method */ protected Method dtmResultsSet_getRows = null;
    /** DtmResults getResultSet method */ protected Method dtmResults_getResultSet = null;
    /** DtmFreeFormCommand execute method */ protected Method dtmFreeFormCommand_execute = null;
    /** DtmFreeFormCommand waitForCompletion method */ protected Method dtmFreeFormCommand_waitForCompletion = null;
    /** DtmContext setEnvironment method */ protected Method dtmContext_setEnvironment = null;
    /** DtmContext clearEnvironemnt method */ protected Method dtmContext_clearEnvironment = null;

    // Background processes
    ///
    private static ArrayList<Process> backgroundProcesses = new ArrayList<Process>();

    // local classloader to be used to load admin jars
    //
    public class MyClassloader extends URLClassLoader {

        public MyClassloader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        public void addURL(URL url) {
            super.addURL(url);
        }
    }
    private final MyClassloader classLoader = new MyClassloader(new URL[0], this.getClass().getClassLoader());

    /**
     * Initialize administration
     * 
     * Admin jars are loaded and classes discovered
     * @param failOnError true to fail on error, false otherwise
     * 
     * @throws MojoExecutionException  if initialize fails
     */
    void initializeAdministration(boolean failOnError) throws MojoExecutionException {

        if (dtmContext == null) {

            loadAdministrationJars();

            try {
                dtmContextClass = Class.forName(DTMCONTEXT_CLASSNAME, true, classLoader);
                dtmNodeClass = Class.forName(DTMNODE_CLASSNAME, true, classLoader);
                dtmInstallNodeCommandClass = Class.forName(DTMINSTALLNODECOMMAND_CLASSNAME, true, classLoader);
                dtmCommandClass = Class.forName(DTMCOMMAND_CLASSNAME, true, classLoader);
                dtmDestinationClass = Class.forName(DTMDESTINATION_CLASSNAME, true, classLoader);
                dtmResultsClass = Class.forName(DTMRESULTS_CLASSNAME, true, classLoader);
                dtmResultsSetClass = Class.forName(DTMRESULTSET_CLASSNAME, true, classLoader);
                dtmRowClass = Class.forName(DTMROW_CLASSNAME, true, classLoader);
                dtmDeployFragmentCommandClass = Class.forName(DTMDEPLOYFRAGMENTCOMMAND_CLASSNAME, true, classLoader);
                fragmentTypeClass = Class.forName(FRAGMENTTYPE_CLASSNAME, true, classLoader);
                dtmFreeFormCommandClass = Class.forName(DTMFREEFORMCOMMAND_CLASSNAME, true, classLoader);
                iDtmProgressClass = Class.forName(IDTMPROGRESS_CLASSNAME, true, classLoader);

                dtmNodeConstructor = dtmNodeClass.getConstructor(String.class, dtmContextClass);
                dtmNodeConstructorUsernamePassword = dtmNodeClass.getConstructor(String.class, dtmContextClass, String.class, String.class);
                dtmInstallNodeConstructor = dtmInstallNodeCommandClass.getConstructor(dtmNodeClass);
                dtmDestinationConstructor = dtmDestinationClass.getConstructor(String.class, dtmContextClass);
                dtmDestinationConstructorUsernamePassword = dtmDestinationClass.getConstructor(String.class, dtmContextClass, String.class, String.class);
                dtmCommandConstructor = dtmCommandClass.getConstructor(String.class, String.class, dtmDestinationClass);
                dtmDeployFragmentCommandConstructor = dtmDeployFragmentCommandClass.getConstructor(fragmentTypeClass, String.class, dtmDestinationClass);
                dtmFreeFormCommandConstructor = dtmFreeFormCommandClass.getConstructor(dtmContextClass);

                dtmNode_setAdministrationPort = dtmNodeClass.getMethod("setAdministrationPort", int.class);
                dtmNode_setHostName = dtmNodeClass.getMethod("setHostName", String.class);
                dtmContext_enableTracing = dtmContextClass.getMethod("enableTracing");
                dtmContext_setEnvironment = dtmContextClass.getMethod("setEnvironment", new Class[]{Map.class});
                dtmContext_clearEnvironment = dtmContextClass.getMethod("clearEnvironment");


                dtmResults_getHeaders = dtmResultsClass.getMethod("getHeaders");
                dtmRow_getColumns = dtmRowClass.getMethod("getColumns");
                dtmResultsSet_getRows = dtmResultsSetClass.getMethod("getRows");
                dtmResults_getResultSet = dtmResultsClass.getMethod("getResultSet");
                dtmFreeFormCommand_execute = dtmFreeFormCommandClass.getMethod("execute", String.class, iDtmProgressClass);
                dtmFreeFormCommand_waitForCompletion = dtmFreeFormCommandClass.getMethod("waitForCompletion");

                for (Method m : dtmInstallNodeCommandClass.getMethods()) {
                    switch (m.getName()) {
                    case "execute": 
                        dtmInstallNodeCommand_execute = m;
                        break;
                    case "cancel": 
                        dtmInstallNodeCommand_cancel = m;
                        break;
                    case "waitForCompletion": 
                        if (m.getParameterTypes().length == 0) {
                            dtmInstallNodeCommand_waitForCompletion = m;
                        }
                        break;
                    }
                }

                for (Method m : dtmDeployFragmentCommandClass.getMethods()) {
                    switch (m.getName()) {
                    case "execute": 
                        dtmDeployFragmentCommand_execute = m;
                        break;
                    case "waitForCompletion": 
                        if (m.getParameterTypes().length == 0) {
                            dtmDeployFragmentCommand_waitForCompletion = m;
                        }
                        break;
                    case "setApplicationArguments": 
                        Class<?> [] p = m.getParameterTypes();
                        if (p[0].getName().equals("java.util.List")) {
                            dtmDeployFragmentCommand_setApplicationArguments = m;
                        }
                        break;
                    case "setExecutionOptions": 
                        dtmDeployFragmentCommand_setExecutionOptions = m;
                        break;
                    case "cancel": 
                        dtmDeployFragmentCommand_cancel = m;
                        break;
                    }
                }

                for (Method m : dtmCommandClass.getMethods()) {
                    switch (m.getName()) {
                    case "execute": 
                        dtmCommand_execute = m;
                        break;
                    case "waitForCompletion": 
                        if (m.getParameterTypes().length == 0) {
                            dtmCommand_waitForCompletion = m;
                        }
                        break;
                    }
                }

                for (Method m : dtmDestinationClass.getMethods()) {
                    switch (m.getName()) {
                    case "addDiscoveryHost": 
                        dtmDestination_addDiscoveryHost = m;
                        break;
                    case "setDiscoveryPort": 
                        dtmDestination_setDiscoveryPort = m;
                        break;
                    }
                }

                Constructor<?> cs = dtmContextClass.getConstructor(Path.class);
                dtmContext = cs.newInstance(productHome.toPath());
                if (getLog().isDebugEnabled()) {
                    dtmContext_enableTracing.invoke(dtmContext);
                }

            } catch (NoSuchMethodException e) {
                if (failOnError) {
                    throw new MojoExecutionException(e.toString());
                } else {
                    getLog().warn(e.toString()+" [ignored]");
                }
            } catch (SecurityException e) {
                if (failOnError) {
                    throw new MojoExecutionException(e.toString());
                } else {
                    getLog().warn(e.toString()+" [ignored]");
                }
            } catch (ClassNotFoundException e) {
                if (failOnError) {
                    throw new MojoExecutionException(e.toString());
                } else {
                    getLog().warn(e.toString()+" [ignored]");
                }
            } catch (InstantiationException e) {
                if (failOnError) {
                    throw new MojoExecutionException(e.toString());
                } else {
                    getLog().warn(e.toString()+" [ignored]");
                }
            } catch (IllegalAccessException e) {
                if (failOnError) {
                    throw new MojoExecutionException(e.toString());
                } else {
                    getLog().warn(e.toString()+" [ignored]");
                }
            } catch (IllegalArgumentException e) {
                if (failOnError) {
                    throw new MojoExecutionException(e.toString());
                } else {
                    getLog().warn(e.toString()+" [ignored]");
                }
            } catch (InvocationTargetException e) {
                if (failOnError) {
                    throw new MojoExecutionException(e.getCause().getMessage());
                } else {
                    getLog().warn(e.getCause().getMessage()+" [ignored]");
                }
            }

        }
    }

    /**
     * Initialize streambase
     * 
     * Jars are loaded and reflection initilized
     * 
     * @throws MojoExecutionException on error
     */
    void initialiseStreambase() throws MojoExecutionException {
        loadStreamBaseJars();
    }

    /**
     * Create a monitor instance for returning the results of an admin command
     * 
     * Attempts are made to keep the results return in a consistent display
     * format.
     * 
     * @param command Command we are running
     * @param target Target we are running on
     * @param failOnError if false, demote errors/workings to info
     * @param recordOutput if true record output to return in topString()
     * @return Monitor instance
     * 
     * @throws MojoExecutionException
     *             On error
     */
    Object createMonitor(final String command, final String target, boolean failOnError, boolean recordOutput) throws MojoExecutionException {
        Object monitor = null;

        try {

            InvocationHandler handler = new java.lang.reflect.InvocationHandler() {

                public String command = "";
                public String target = "";
                boolean running = true;

                StringBuilder fullResults = new StringBuilder();

                @Override
                public Object invoke(Object proxy, java.lang.reflect.Method method, Object[] args) throws java.lang.Throwable {
                    String method_name = method.getName();
                    if (method_name.equals("start")) {
                        getLog().info("["+target+"] Running \""+command+"\"");
                        running = true;
                        // FIX THIS - DJS: Remove when all codelines updated
                        // to use new IDtmProgress signatures. FLUENCY-8132
                    } else if (method_name.equals("message")) {
                        if (running) {
                            getLog().info("["+(String)args[0]+"] "+(String)args[1]);
                            if (recordOutput) {
                                fullResults.append((String)args[1]).append("\n");
                            }
                        }
                    } else if (method_name.equals("loggingEvent")) {
                        if (running) {

                            final String message = getEventMessage(args[1]);

                            getLog().info("["+(String)args[0]+"] "+message);
                            if (recordOutput) {
                                fullResults.append(message).append("\n");
                            }
                        }
                    } else if (method_name.equals("status")) {
                        if (running) {
                            getLog().info("["+(String)args[0]+"] "+(String)args[1]);
                            if (recordOutput) {
                                fullResults.append((String)args[1]).append("\n");
                            }
                        }
                    } else if (method_name.equals("error")) {
                        if (running) {
                            getLog().error("["+(String)args[0]+"] "+(String)args[1]);
                            if (recordOutput) {
                                fullResults.append((String)args[1]).append("\n");
                            }
                        }
                    } else if (method_name.equals("failed")) {
                        if (running) {
                            if (failOnError) {
                                getLog().error("["+target+"] return code "+(Integer)args[0]);
                            } else {
                                getLog().info("["+target+"] return code "+(Integer)args[0]+" [ignored]");
                            }
                            if (recordOutput) {
                                fullResults.append((Integer)args[0]).append("\n");
                            }
                        }
                    } else if (method_name.equals("complete")) {
                        if (running) {
                            getLog().info("["+target+"] Finished \""+command+"\"");
                        }
                        running = false;
                    } else if (method_name.equals("results")) {
                        if (running) {
                            Method prettyPrint = dtmResultsClass.getMethod("prettyPrint");
                            String lines = (String) prettyPrint.invoke(args[0]);  
                            for (String line : lines.split("\\r?\\n")) {
                                getLog().info(line);
                            }
                        }
                    } else if (method_name.equals("toString")) {
                        return fullResults.toString();
                    } else if (method_name.equals("cancel")) {
                        getLog().error("["+target+"] return code "+(Integer)args[0]);
                    } else {
                        getLog().warn(method+" called on monitor - add implementation");
                    }
                    return null;
                }
            };

            Field field = handler.getClass().getField("command");
            field.setAccessible(true);
            field.set(handler, command);
            field = handler.getClass().getField("target");
            field.setAccessible(true);
            field.set(handler, target);

            monitor = Proxy.newProxyInstance(
                    classLoader,
                    new java.lang.Class[] { Class.forName(IDTMPROGRESS_CLASSNAME, true, classLoader) },
                    handler);

        } catch (IllegalArgumentException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (ClassNotFoundException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (NoSuchFieldException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (SecurityException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException(e.getMessage());
        }

        return monitor;
    }

    /**
     * Use reflection to extract and return the specified log event's message field.
     *
     * @param ev the event
     * @return its message field
     */
    private String getEventMessage(final Object ev) {

        try {

            Class<?> deployLogEventClass = Class.forName("com.tibco.ep.dtm.logging.DeployLogEvent", true, classLoader);
            Method getMessage = deployLogEventClass.getMethod("getMessage");

            return (String) getMessage.invoke(ev);

        } catch (ClassNotFoundException | NoSuchMethodException |SecurityException | IllegalAccessException | InvocationTargetException e) {
            return ev.toString();
        }
    }

    /**
     * to avoid maven dependency hell, we need to load the management jar and all its dependencies at
     * runtime.
     * 
     * @throws MojoExecutionException if jar loading fails
     */
    private void loadAdministrationJars() throws MojoExecutionException {

        // Needed for testing, since test stub API version is different from production version.
        String productVersion = getProductVersion();

        if (productVersion.isEmpty()) {
            throw new MojoExecutionException("Unable to determine the product version - ensure that either the management or sdk jar is set as a dependency in your pom");
        }

        Artifact managementAPIArtifact = repositorySystem.createArtifact(DTM_GROUP_IDENTIFIER, DTM_MANAGEMENT_ARTIFACT_IDENTIFIER, productVersion, "", "jar");
    
        Set<Artifact> baseArtifacts = new LinkedHashSet<Artifact>();

        baseArtifacts.add(managementAPIArtifact);

        Set<Artifact> managementAPIDependencies = getProjectDependencies(baseArtifacts);

        for (final Artifact artifact : managementAPIDependencies) {

            ArtifactResolutionRequest request = new ArtifactResolutionRequest();
            request.setArtifact(artifact);
            request.setRemoteRepositories(remoteRepositories);
            request.setLocalRepository(localRepository);
            repositorySystem.resolve(request);

            if (!loadJar(getArtifactPath(artifact))) {
                throw new MojoExecutionException(
                                                 "Unable to locate " 
                                                 + artifact.getGroupId()
                                                 + "."
                                                 + artifact.getArtifactId()
                                                 + "-"
                                                 + artifact.getVersion()
                                                 + " (expected at "
                                                 + getArtifactPath(artifact)
                                                 + ") - ensure that repository is on-line");
            }
        }
    }
    
    /**
     * to avoid maven dependency hell, we need to load the streambase jar at
     * runtime.  Jars are loaded via maven dependencies.
     * 
     * @throws MojoExecutionException if jar loading fails
     */
    protected void loadStreamBaseJars() throws MojoExecutionException {

        Set<Artifact> baseArtifacts = new LinkedHashSet<Artifact>();

        for (Artifact artifact : getProjectDependencies()) {
            if (artifact.getGroupId().equals(SB_GROUP_IDENTIFIER) || 
                    (artifact.getGroupId().equals(DTM_GROUP_IDENTIFIER) 
                            && artifact.getArtifactId().equals(DTM_SDK_ARTIFACT_IDENTIFIER))) {
                baseArtifacts.add(artifact);
            }
        }

        for (Artifact artifact : getProjectDependencies(baseArtifacts)) {
            loadJar(getArtifactPath(artifact));
        }
    }

    /**
     * Load a single jar and report success
     * 
     * @param fileName file to load
     * @return true if loaded successfully
     */
    private boolean loadJar(String fileName) {

        File file = new File(fileName);
        if (!file.exists()) {
            getLog().debug("Failed to find "+fileName+" file doesn't exist");
            return false;
        }

        try {
            URL url = file.toURI().toURL();
            classLoader.addURL(url);
        } catch (MalformedURLException e) {
            return false;
        }
        return true;
    }


    /**
     * Return the absolute path to the given artifact. 
     *
     * @param artifact
     *            Maven artifact to be resolved.
     * @return Absolute path to the resolved artifact.
     */
    public String getArtifactPath(Artifact artifact) {
        assert (artifact != null);

        if (artifact.getFile() != null) {

            // work-around for m2e getting the resolved path wrong
            //
            if (artifact.getFile().getName().equals("classes")) {
                String type = artifact.getType();
                if (type != null && (type.equals(JAVA_TYPE) || type.equals(EVENTFLOW_TYPE) || type.equals(TCS_TYPE) || type.equals(LIVEVIEW_TYPE))) {
                    File fixedPath = new File(artifact.getFile().getParentFile(), artifact.getArtifactId()+"-"+artifact.getVersion()+"-"+artifact.getType()+".zip");
                    artifact.setFile(fixedPath);
                }
                
                if (type != null && (type.equals("nar"))) {
                    File fixedPath = new File(artifact.getFile().getParentFile(), artifact.getArtifactId()+"-"+artifact.getVersion()+"-"+artifact.getClassifier()+".nar");
                    artifact.setFile(fixedPath);
                }
            }

            return artifact.getFile().getAbsolutePath();
        }

        // default to the local repository
        //
        String path = localRepository.getBasedir();
        path += File.separator;
        path += localRepository.pathOf(artifact);

        return path;
    }

    /**
     * Get the artifact name in the form of 
     * 
     *  groupid-artifactid-type-version
     *  
     * @param artifact  Maven artifact
     * @return name
     */
    public String getArtifactName(Artifact artifact) {
        String name = "";
        name += artifact.getGroupId();
        name += "-";
        name += artifact.getArtifactId();
        name += "-";
        name += artifact.getType();
        name += "-";
        name += artifact.getBaseVersion();
        return name;
    }

    /**
     * Get the artifact name in the form for assembly
     *  
     * @param artifact Maven artifact
     * @return name
     */
    public String getArtifactAssemblyName(Artifact artifact) {
        String name = "";
        name += artifact.getGroupId();
        name += ":";
        name += artifact.getArtifactId();
        name += ":";
        name += artifact.getType();
        if (artifact.getClassifier() != null) {
            name += ":";
            name += artifact.getClassifier();
        }
        name += ":";
        name += artifact.getBaseVersion();
        return name;
    }
    
    /**
     * get the project dependencies as artifacts
     * 
     * @return set of artifacts
     */
    public Set<Artifact> getProjectDependencies() {

        // initial deps
        //
        Set<Artifact> artifacts = new LinkedHashSet<Artifact>();
        if (project.getDependencies() != null) {
            for (Dependency d : (List<Dependency>)project.getDependencies()) {
                artifacts.add(repositorySystem.createDependencyArtifact(d));
            }
        }

        return getProjectDependencies(artifacts);
    }
    
    /**
     * get the project dependencies as artifacts
     * 
     * @return set of artifacts
     * @throws MojoExecutionException  on error
     */
    public Set<Artifact> getCompileProjectDependencies() throws MojoExecutionException {
        
        Set<Artifact> deps = new LinkedHashSet<Artifact>();
        
        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest( session.getProjectBuildingRequest() );
        buildingRequest.setProject( project );
        try {
            DependencyNode rootNode = dependencyGraphBuilder.buildDependencyGraph( buildingRequest, null, reactorProjects );
            
            DependencyNodeVisitor visitor = new DependencyNodeVisitor() {
                int depth = 0;
                private String indent() {
                    String t = "";
                    for (int i=0; i<depth; i++) {
                        t = t+"  ";
                    }
                    return t;
                }
                @Override
                public boolean visit(DependencyNode node) {
                    if (node.getParent() == null) {
                        // root node - skip
                        return true;
                    }
                    
                    String indent = indent();
                    depth=depth+1;
                    Artifact artifact = node.getArtifact();
                    String scope = artifact.getScope();
                    String type = artifact.getType();
                    if (scope == null || scope.equals(Artifact.SCOPE_COMPILE) || scope.equals(Artifact.SCOPE_RUNTIME)) {
                        // compile dependency - add to list
                        //
                        deps.add(artifact);
                        
                        // fragments don't descend into dependencies
                        //
                        if (type != null && (type.equals(JAVA_TYPE) || type.equals(EVENTFLOW_TYPE) || type.equals(TCS_TYPE) || type.equals(LIVEVIEW_TYPE))) {
                            getLog().debug(indent+node.getArtifact()+" [adding]");
                            return false;
                        } else {
                            getLog().debug(indent+node.getArtifact()+" [adding]");

                            // keep going to check for dependencies of this artifact
                            //
                            return true;
                        }
                    } else if (scope.equals(Artifact.SCOPE_PROVIDED) || scope.equals(Artifact.SCOPE_TEST)) {
                        // provided and test skip
                        //
                        getLog().debug(indent+node.getArtifact()+" [skipping]");
                        return false;
                    }
                    
                    getLog().debug(indent+node.getArtifact()+" [skipping]");

                    return true;
                }
                @Override
                public boolean endVisit(DependencyNode node) {
                    if (node.getParent() != null) {
                        depth=depth-1;
                    }
                    
                    return true;
                }
            };
            
            rootNode.accept(visitor);

            
        } catch (DependencyGraphBuilderException e) {
            throw new MojoExecutionException(e.getMessage());
        }
        
        return deps;
    }

    /**
     * get the project dependencies as artifacts
     * @param type only look for this type
     * 
     * @return set of artifacts
     */
    public Set<Artifact> getProjectDependencies(String type) {

        // initial deps
        //
        Set<Artifact> artifacts = new LinkedHashSet<Artifact>();
        if (project.getDependencies() != null) {
            for (Dependency d : (List<Dependency>)project.getDependencies()) {
                artifacts.add(repositorySystem.createDependencyArtifact(d));
            }
        }

        // and filter
        //
        Set<Artifact> projectArtifacts = new LinkedHashSet<Artifact>();
        for (Artifact a :getProjectDependencies(artifacts)) {
            if (a.getType().equals(type)) {
                projectArtifacts.add(a);
            }
        }

        return projectArtifacts;
    }

    /**
     * get the project dependencies as artifacts
     * @param groupId only look for this groupId
     * @param artifactId  only look for this artifactId
     * 
     * @return set of artifacts
     */
    public Set<Artifact> getProjectDependencies(String groupId, String artifactId) {

        // initial deps
        //
        Set<Artifact> artifacts = new LinkedHashSet<Artifact>();
        if (project.getDependencies() != null) {
            for (Dependency d : (List<Dependency>)project.getDependencies()) {
                artifacts.add(repositorySystem.createDependencyArtifact(d));
            }
        }
        
        // and filter
        //
        Set<Artifact> projectArtifacts = new LinkedHashSet<Artifact>();
        for (Artifact a : getProjectDependencies(artifacts)) {
            if (a.getGroupId().equals(groupId) && a.getArtifactId().equals(artifactId)) {
                projectArtifacts.add(a);
            }
        }

        return projectArtifacts;
    }

    /**
     * get the project dependencies as artifacts, but limit scope to a subset
     * 
     * @param artifacts starting artifacts
     * @return  set of artifacts
     */
    public Set<Artifact> getProjectDependencies(Set<Artifact> artifacts) {

        final ArtifactFilter filter = new ArtifactFilter() {
            @Override
            public boolean include(Artifact artifact) {
                return true;
            }
        };

        // note that resolving transitively here incorrectly brings in older dependencies
        //
        ArtifactResolutionRequest request = new ArtifactResolutionRequest();
        request.setLocalRepository(localRepository);
        request.setResolveRoot(false);
        request.setRemoteRepositories(project.getRemoteArtifactRepositories());
        request.setResolveTransitively(false);
        request.setArtifact(project.getArtifact());
        request.setArtifactDependencies(artifacts);
        request.setManagedVersionMap(project.getManagedVersionMap());
        request.setCollectionFilter(filter);
        request.setOffline(session.isOffline());
        request.setForceUpdate(session.getRequest().isUpdateSnapshots());
        request.setServers(session.getRequest().getServers());
        request.setMirrors(session.getRequest().getMirrors());
        request.setProxies(session.getRequest().getProxies());
        ArtifactResolutionResult r = artifactResolver.resolve(request);
        artifacts = r.getArtifacts();

        
        /** old deprecated API
            DefaultArtifactResolver actualResolver = (DefaultArtifactResolver)artifactResolver;
            ArtifactResolutionResult result = actualResolver.resolveTransitively(artifacts, project.getArtifact(),
                    project.getManagedVersionMap(), localRepository, project.getRemoteArtifactRepositories(),
                    null, filter );
            artifacts = (Set<Artifact>)result.getArtifacts();
         */

        return artifacts;
    }
    
    /**
     * Determine if this artifact is a DTM product artifact
     * 
     * @param artifact artifact 
     * @return true if its a product, false otherwise
     */
    boolean isDTMProduct(Artifact artifact) {
        if (artifact.getGroupId().equals(DTM_GROUP_IDENTIFIER) && 
                (artifact.getArtifactId().startsWith(SB_PRODUCT_ARTIFACT_PREFIX))) {
            return true;
        }
        return false;
    }

    /**
     * Determine if this artifact is a SB product artifact
     * 
     * @param artifact artifact 
     * @return true if its a product, false otherwise
     */
    boolean isSBProduct(Artifact artifact) {
        if (artifact.getGroupId().equals(SB_RT_GROUP_IDENTIFIER) && 
                artifact.getArtifactId().startsWith(SB_PRODUCT_ARTIFACT_PREFIX)) {
            return true;
        }
        return false;
    }

    /**
     * Determine if this artifact is a DTM support artifact
     * 
     * @param artifact artifact 
     * @return true if its a product, false otherwise
     */
    boolean isDTMSupport(Artifact artifact) {
        if (artifact.getGroupId().equals(DTM_GROUP_IDENTIFIER) && 
                (artifact.getArtifactId().startsWith(SB_SUPPORT_ARTIFACT_PREFIX))) {
            return true;
        }
        return false;
    }
    
    /**
     * Determine if the given artifact belongs to an EP platform.
     * <p>
     * This doesn't check for the artifact type, since there might be other
     * attachments for that platform (sources, etc.). What it does mean
     * is that if you've got a <code>zip</code> artifact for which this
     * function returns <code>true</code>, it <em>can</em> be installed.
     * By convention, that product-installable artifact has no classifier.
     * @param artifact the artifact, with any relation to a platform
     * @return whether the artifact belongs to an EP platform
     */
    static boolean isPlatformArtifact(Artifact artifact) {
        final String artifactId = artifact.getArtifactId();
        return artifactId.startsWith(SB_PRODUCT_ARTIFACT_PREFIX)
                || artifactId.startsWith(SB_SUPPORT_ARTIFACT_PREFIX);
    }
    
    /**
     * Compile a sbapp file to a jar
     * 
     * @param sbapp sbapp name, without path
     * @return jar file - null if failed
     */
    File compileSbapp(String sbapp) {

        // save properties we will overwrite
        //
        Properties oldProperties = System.getProperties();

        File jarFile = null;
        JarOutputStream target = null;
        
        try {

            loadStreamBaseJars();

            // avoid conflict with antlr loaded via maven plugins
            //
            System.setProperty("ANTLR_USE_DIRECT_CLASS_LOADING", "true");

            Class<?> serverManagerFactoryClass = ClassLoader.getSystemClassLoader().loadClass("com.streambase.sb.unittest.ServerManagerFactory");
            Method getEmbeddedServer = serverManagerFactoryClass.getMethod("getEmbeddedServer");
            Object server = getEmbeddedServer.invoke(null);
            Class<?> sbServerManagerClass = ClassLoader.getSystemClassLoader().loadClass("com.streambase.sb.unittest.SBServerManager");
            Method startServer = sbServerManagerClass.getMethod("startServer");
            startServer.invoke(server);
            Method loadApp = sbServerManagerClass.getMethod("loadApp", String.class);
            loadApp.invoke(server, sbapp);

            jarFile = File.createTempFile("compile", "jar");
            jarFile.deleteOnExit();

            target = new JarOutputStream(new FileOutputStream(jarFile));
            Class<?> binDirManagerClass = ClassLoader.getSystemClassLoader().loadClass("com.streambase.sb.appgen.BinDirManager");
            Method getManager = binDirManagerClass.getMethod("getManager");
            Method saveIntoJar = binDirManagerClass.getMethod("saveIntoJar", JarOutputStream.class);
            Object manager = getManager.invoke(null);
            saveIntoJar.invoke(manager, target);

        } catch (FileNotFoundException e) {
            getLog().debug("Compile failed "+e.getCause());
        } catch (IOException e) {
            getLog().debug("Compile failed "+e.getCause());
        } catch (ClassNotFoundException e) {
            getLog().debug("Compile failed "+e.getCause());
        } catch (NoSuchMethodException e) {
            getLog().debug("Compile failed "+e.getCause());
        } catch (SecurityException e) {
            getLog().debug("Compile failed "+e.getCause());
        } catch (IllegalAccessException e) {
            getLog().debug("Compile failed "+e.getCause());
        } catch (IllegalArgumentException e) {
            getLog().debug("Compile failed "+e.getCause());
        } catch (InvocationTargetException e) {
            getLog().debug("Compile failed "+e.getCause());
        } catch (MojoExecutionException e) {
            getLog().debug("Compile failed "+e.getCause());
        } finally {
            if (target != null) {
                try {
                    target.close();
                } catch (IOException e) {
                }
            }
            // restore properties
            //
            System.setProperties(oldProperties);
        }

        return jarFile;
    }

    /**
     * Run a simple external command
     * 
     * @param command command
     * @param args arguments
     * @param background run in the background
     * @return return error response
     * @throws MojoExecutionException I/O error
     */
    public String run(final String command, final String[] args, boolean background) throws MojoExecutionException {

        try {

            String debugMessage = command;
            String[] cmdarray;

            if (args == null || args.length == 0) {
                cmdarray = new String[1];
            } else {
                cmdarray = new String[args.length+1];
                for (int i=0; i<args.length; i++) {
                    cmdarray[i+1] = args[i];
                    debugMessage += " "+args[i];
                }
            }
            cmdarray[0] = command;

            getLog().debug(debugMessage);

            Process p = Runtime.getRuntime().exec(cmdarray);

            String fileName = new File(command).getName();

            ProcessHandler inputStream = new ProcessHandler(p.getInputStream(),fileName, false);
            ProcessHandler errorStream = new ProcessHandler(p.getErrorStream(),fileName, true);

            inputStream.start();
            errorStream.start();
            if (!background) {
                p.waitFor();
            } else {
                // save our process id for possible future kill
                //
                backgroundProcesses.add(p);

                // wait for *something* before backgrounding
                //
                while (true) {
                    if (inputStream.getOutput().length() > 0 || errorStream.getOutput().length() > 0) {
                        return errorStream.getOutput();
                    }
                    Thread.sleep(1000);
                }
            }

            return errorStream.getOutput();

        } catch (InterruptedException e) {
        } catch (IOException e) {
            throw new MojoExecutionException(e.getMessage());
        }

        return "";
    }
    
    /**
     * Get product version based on dependencies
     * 
     * @return product version
     */
    private String getProductVersion() {
        
        // FIX THIS - DJS: We should assert these are all the same

        // FIX THIS - DJS: We should use transitive dependencies for the SDK artifact
        for (Artifact artifact : getProjectDependencies(DTM_GROUP_IDENTIFIER, DTM_SDK_ARTIFACT_IDENTIFIER)) {
            getLog().debug("Found product version "+artifact.getBaseVersion()+" from "+artifact.toString());
            return artifact.getBaseVersion();
        }

        for (Artifact artifact : getProjectDependencies(DTM_GROUP_IDENTIFIER, DTM_MANAGEMENT_ARTIFACT_IDENTIFIER)) {
            getLog().debug("Found product version "+artifact.getBaseVersion()+" from "+artifact.toString());
            return artifact.getBaseVersion();
        }

        for (Artifact artifact : getProjectDependencies("zip")) {
            if (artifact.getGroupId().equals(DTM_GROUP_IDENTIFIER) && artifact.getArtifactId().startsWith(SB_PRODUCT_ARTIFACT_PREFIX)) {
                getLog().debug("Found product version "+artifact.getBaseVersion()+" from "+artifact.toString());
                return artifact.getBaseVersion();
            }
        }

        return "";
    }

    /**
     * Process the output of a process and write logs to maven getLog()
     *
     */
    private class ProcessHandler extends Thread {

        InputStream inpStr;
        String strType;
        boolean warn;

        private String log = "";

        public ProcessHandler(InputStream inpStr, String strType, boolean warn) {
            this.inpStr = inpStr;
            this.strType = strType;
            this.warn = warn;
        }

        public String getOutput() {
            return log;
        }

        @Override
        public void run() {
            try {
                InputStreamReader inpStrd = new InputStreamReader(inpStr);
                BufferedReader buffRd = new BufferedReader(inpStrd);
                String line = null;
                while((line = buffRd.readLine()) != null) {
                    if (warn) {
                        getLog().warn("["+strType+"] " + line);
                    } else {
                        getLog().info("["+strType+"] " + line);
                    }
                    log += line;
                }
                buffRd.close();

            } catch(Exception e) {
                getLog().warn(e.getMessage());
            }
        }
    }    

    /**
     * Kill background processes
     */
    public void kill() {
        for (Process p : backgroundProcesses) {
            if (p != null) {
                p.destroy();
            }
        }
        backgroundProcesses.clear();
    }

    /**
     * Check environment before running mojo
     * 
     * @throws MojoExecutionException check failed
     */
    public void prechecks() throws MojoExecutionException {
        
        if (productHome == null || productHome.getAbsolutePath().isEmpty()) {
            String epHome = System.getenv("TIBCO_EP_HOME");
            if (epHome != null) {
                productHome = new File(epHome);
            } else {
                
                File base = new File(session.getLocalRepository().getBasedir()).getParentFile();
                
                // locate product version
                //
                String baseSubDirectory = "";
                for (Artifact artifact : getProjectDependencies("zip")) {
                    
                    if (isSBProduct(artifact)) {
                        baseSubDirectory = artifact.getGroupId()+File.separator+artifact.getArtifactId()+File.separator+artifact.getBaseVersion();
                        break;
                    }
                    if (isDTMProduct(artifact)) {
                        baseSubDirectory = artifact.getGroupId()+File.separator+artifact.getArtifactId()+File.separator+artifact.getBaseVersion();
                        break;
                    }
                }
                productHome = new File (base, baseSubDirectory);

            }
        }
        getLog().debug("Product home set to "+productHome);
    }


}
