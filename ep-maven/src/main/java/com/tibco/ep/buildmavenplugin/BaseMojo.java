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

import com.tibco.ep.buildmavenplugin.admin.RuntimeCommandRunner;
import com.tibco.ep.sb.services.RuntimeServices;
import com.tibco.ep.sb.services.build.IRuntimeBuildService;
import com.tibco.ep.sb.services.management.AbstractCommandBuilder;
import com.tibco.ep.sb.services.management.IContext;
import com.tibco.ep.sb.services.management.IRuntimeAdminService;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;

/**
 * Base type
 */
abstract class BaseMojo extends AbstractMojo {

    // Artifact names
    //
    private static final String SB_PRODUCT_ARTIFACT_PREFIX = "platform_";
    private static final String DTM_GROUP_IDENTIFIER = "com.tibco.ep.dtm";
    private static final String DTM_MANAGEMENT_ARTIFACT_IDENTIFIER = "management";
    private static final String DTM_SDK_ARTIFACT_IDENTIFIER = "sdk";
    private static final String SB_GROUP_IDENTIFIER = "com.tibco.ep.sb";
    private static final String SB_RT_GROUP_IDENTIFIER = "com.tibco.ep.sb.rt";
    private static final String SB_SERVER_ARTIFACT_IDENTIFIER = "server";
    private static final String SB_SUPPORT_ARTIFACT_PREFIX = "support_platform_";
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
     * Directory used to import other Eventflow fragments
     */
    protected static String IMPORT_DIRECTORY = "eventflow";

    /**
     * TIBCO EP Fragment List manifest entry name
     */
    static final String MANIFEST_TIBCO_EP_FRAGMENT_LIST = "TIBCO-EP-Fragment-List";

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
    /**
     * Maven execution
     */
    @Parameter(defaultValue = "${mojo}", readonly = true)
    MojoExecution execution;
    /**
     * Map of plugin artifacts.
     */
    @Parameter(property = "plugin.artifactMap", readonly = true, required = true)
    Map<String, Artifact> pluginArtifactMap;
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
    @Parameter(property = "project.pluginArtifactRepositories", readonly = true, required = true)
    List<ArtifactRepository> remoteRepositories;
    /**
     * Maven local repository
     */
    @Parameter(defaultValue = "${localRepository}", required = true, readonly = true)
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

    @Component(hint = "default")
    private DependencyGraphBuilder dependencyGraphBuilder;

    @Parameter(defaultValue = "${reactorProjects}", readonly = true, required = true)
    private List<MavenProject> reactorProjects;

    private PluginClassloader classLoader = null;
    private IRuntimeAdminService adminService;
    private IRuntimeBuildService buildService;

    //  Runtime admin context.
    //
    private IContext context;

    /**
     * Determine if the given artifact belongs to an EP platform.
     * <p>
     * This doesn't check for the artifact type, since there might be other
     * attachments for that platform (sources, etc.). What it does mean
     * is that if you've got a <code>zip</code> artifact for which this
     * function returns <code>true</code>, it <em>can</em> be installed.
     * By convention, that product-installable artifact has no classifier.
     *
     * @param artifact the artifact, with any relation to a platform
     * @return whether the artifact belongs to an EP platform
     */
    static boolean isPlatformArtifact(Artifact artifact) {
        final String artifactId = artifact.getArtifactId();
        return artifactId.startsWith(SB_PRODUCT_ARTIFACT_PREFIX)
            || artifactId.startsWith(SB_SUPPORT_ARTIFACT_PREFIX);
    }

    /**
     * @param currentValue The current value
     * @param path         The path of the default directory
     * @return The current value if it contains a value, or an array pointing to the default path
     */
    File[] getOrDefault(File[] currentValue, String path) {
        if (currentValue != null && currentValue.length > 0) {
            return currentValue;
        }

        return new File[]{new File(project.getBasedir(), path)};
    }

    /**
     * @param eventFlowDirectories The current event flow directories value
     * @return The current value or the default if needed
     */
    File[] getOrDefaultEventFlowDirectories(File[] eventFlowDirectories) {
        return getOrDefault(eventFlowDirectories, "/src/main/eventflow");
    }


    /**
     * @return The administration service
     */
    public IRuntimeAdminService getAdminService() {
        return adminService;
    }

    /**
     * @return The build service
     */
    public IRuntimeBuildService getBuildService() {
        return buildService;
    }

    /**
     * @return The Runtime Administration context
     */
    public IContext getContext() {
        return context;
    }

    /**
     * Initialize administration or build services
     * <p>
     * Admin & build jars are loaded
     *
     * @param service       The service to initialize
     * @param errorHandling error handling
     * @throws MojoExecutionException if initialize fails
     */
    void initializeService(PlatformService service, ErrorHandling errorHandling) throws MojoExecutionException {

        if (classLoader == null) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    classLoader = new PluginClassloader(new URL[0], this.getClass()
                        .getClassLoader());
                    return null;
                }
            });
        }

        if (adminService == null && service == PlatformService.ADMINISTRATION) {

            loadJarForService(PlatformService.ADMINISTRATION);

            adminService = doGetService(
                () -> RuntimeServices.getAdminService(classLoader),
                IRuntimeAdminService.class,
                errorHandling);

            if (adminService != null) {

                //  Only create a context if we have a service.
                //
                context = adminService.newContext(productHome.toPath());
                if (getLog().isDebugEnabled()) {
                    context.withTracingEnabled();
                }
            }
        }

        if (buildService == null && service == PlatformService.CODE_GENERATION) {

            loadJarForService(PlatformService.CODE_GENERATION);

            buildService = doGetService(
                () -> RuntimeServices.getBuildService(classLoader),
                IRuntimeBuildService.class,
                errorHandling);
        }
    }

    private <T> T doGetService(Supplier<T> getter, Class<T> serviceClass, ErrorHandling errorHandling) throws MojoExecutionException {

        T service = getter.get();
        if (service == null) {
            String message = "Cannot get service " + serviceClass;

            if (errorHandling == ErrorHandling.FAIL) {
                throw new MojoExecutionException(message);
            } else {
                getLog().warn(message + " [ignored]");
            }
        }

        return service;
    }

    /**
     * to avoid maven dependency hell, we need to load the management jar and all its dependencies at
     * runtime.
     *
     * @throws MojoExecutionException if jar loading fails
     */
    private void loadJarForService(PlatformService service) throws MojoExecutionException {

        // Needed for testing, since test stub API version is different from production version.
        String productVersion = getProductVersion();

        if (productVersion.isEmpty()) {
            throw new MojoExecutionException("Unable to determine the product version - ensure that either the management or sdk jar is set as a dependency in your pom");
        }

        if (service == PlatformService.ADMINISTRATION) {
            loadArtifact(DTM_GROUP_IDENTIFIER, DTM_MANAGEMENT_ARTIFACT_IDENTIFIER, productVersion);
        } else {
            assert service == PlatformService.CODE_GENERATION;
            loadArtifact(SB_GROUP_IDENTIFIER, SB_SERVER_ARTIFACT_IDENTIFIER, productVersion);
        }
    }

    private void loadArtifact(String groupIdentifier, String artifactIdentifier, String productVersion) throws MojoExecutionException {

        String jarCoordinates = groupIdentifier + ":" + artifactIdentifier + ":" + productVersion;
        getLog().debug("Loading: " + jarCoordinates);

        Artifact managementAPIArtifact = repositorySystem
            .createArtifact(groupIdentifier, artifactIdentifier, productVersion, "", "jar");
        Set<Artifact> managementAPIDependencies = getProjectDependencies(
            new HashSet<>(Collections.singletonList(managementAPIArtifact)));

        for (final Artifact artifact : managementAPIDependencies) {

            ArtifactResolutionRequest request = new ArtifactResolutionRequest();
            request.setArtifact(artifact);
            request.setRemoteRepositories(remoteRepositories);
            request.setLocalRepository(localRepository);
            repositorySystem.resolve(request);

            String artifactPath = getArtifactPath(artifact);
            if (artifact.getGroupId().equals(groupIdentifier)
                && artifact.getArtifactId().equals(artifactIdentifier)) {
                getLog().debug("Loading: " + jarCoordinates
                    + " from " + artifactPath + " (and dependencies)");
            }

            try {

                File file = new File(artifactPath);
                if (!file.exists()) {
                    throw new FileNotFoundException("Failed to find:" + artifactPath);
                }

                classLoader.addURL(file.toURI().toURL());

            } catch (Exception e) {
                throw new MojoExecutionException(
                    "Unable to locate "
                        + artifact.getGroupId()
                        + "."
                        + artifact.getArtifactId()
                        + "-"
                        + artifact.getVersion()
                        + " (expected at "
                        + artifactPath
                        + ") - ensure that repository is on-line", e);
            }
        }
    }

    /**
     * Return the absolute path to the given artifact.
     *
     * @param artifact Maven artifact to be resolved.
     * @return Absolute path to the resolved artifact.
     */
    String getArtifactPath(Artifact artifact) {
        assert (artifact != null);

        if (artifact.getFile() != null) {

            // work-around for m2e getting the resolved path wrong
            //
            if (artifact.getFile().getName().equals("classes")) {
                String type = artifact.getType();
                if (type != null && (type.equals(JAVA_TYPE) || type.equals(EVENTFLOW_TYPE) || type
                    .equals(TCS_TYPE) || type.equals(LIVEVIEW_TYPE))) {
                    File fixedPath = new File(artifact.getFile().getParentFile(), artifact
                        .getArtifactId() + "-" + artifact.getVersion() + "-" + artifact
                        .getType() + ".zip");
                    artifact.setFile(fixedPath);
                }

                if (type != null && (type.equals("nar"))) {
                    File fixedPath = new File(artifact.getFile().getParentFile(), artifact
                        .getArtifactId() + "-" + artifact.getVersion() + "-" + artifact
                        .getClassifier() + ".nar");
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
     * <p>
     * groupid-artifactid-type-version
     *
     * @param artifact Maven artifact
     * @return name
     */
    String getArtifactName(Artifact artifact) {
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
     * get the project dependencies as artifacts
     *
     * @return set of artifacts
     */
    Set<Artifact> getProjectDependencies() {

        // initial deps
        //
        Set<Artifact> artifacts = new LinkedHashSet<>();
        if (project.getDependencies() != null) {
            for (Dependency d : (List<Dependency>) project.getDependencies()) {
                artifacts.add(repositorySystem.createDependencyArtifact(d));
            }
        }

        return getProjectDependencies(artifacts);
    }

    /**
     * get the project dependencies as artifacts
     *
     * @return set of artifacts
     * @throws MojoExecutionException on error
     */
    Set<Artifact> getCompileProjectDependencies() throws MojoExecutionException {
        return getFilteredProjectDependencies(artifact -> {
                String scope = artifact.getScope();
                if (scope.equals(Artifact.SCOPE_COMPILE) || scope.equals(Artifact.SCOPE_RUNTIME)) {
                    return VisitorDecision.KEEP;
                } else if (scope.equals(Artifact.SCOPE_PROVIDED) || scope.equals(Artifact.SCOPE_TEST)) {
                    return VisitorDecision.SKIP_STOP;
                }

                return VisitorDecision.SKIP_VISIT_DEPENDENCIES;
            },
            FragmentDependencyMode.SKIP_NESTED_FRAGMENT);
    }

    /**
     * get the project dependencies as artifacts
     *
     * @param visitorFilter The visitor filter
     * @param mode          The fragment nested dependency mode
     * @return set of artifacts
     * @throws MojoExecutionException on error
     */
    Set<Artifact> getFilteredProjectDependencies(Function<Artifact, VisitorDecision> visitorFilter, FragmentDependencyMode mode) throws MojoExecutionException {

        DependencyVisitor dependencyVisitor = new DependencyVisitor(visitorFilter, mode);

        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session
            .getProjectBuildingRequest());
        buildingRequest.setProject(project);

        try {
            DependencyNode rootNode = dependencyGraphBuilder
                .buildDependencyGraph(buildingRequest, null, reactorProjects);
            rootNode.accept(dependencyVisitor);
        } catch (DependencyGraphBuilderException e) {
            throw new MojoExecutionException(e.getMessage(), e);
        }

        return dependencyVisitor.getGatheredDependencies();
    }

    /**
     * get the project dependencies as artifacts
     *
     * @param type only look for this type
     * @return set of artifacts
     */
    Set<Artifact> getProjectDependencies(String type) {

        // initial deps
        //
        Set<Artifact> artifacts = new LinkedHashSet<>();
        if (project.getDependencies() != null) {
            for (Dependency d : project.getDependencies()) {
                artifacts.add(repositorySystem.createDependencyArtifact(d));
            }
        }

        // and filter
        //
        Set<Artifact> projectArtifacts = new LinkedHashSet<>();
        for (Artifact a : getProjectDependencies(artifacts)) {
            if (a.getType().equals(type)) {
                projectArtifacts.add(a);
            }
        }

        return projectArtifacts;
    }

    /**
     * get the project dependencies as artifacts
     *
     * @param groupId    only look for this groupId
     * @param artifactId only look for this artifactId
     * @return set of artifacts
     */
    private Set<Artifact> getProjectDependencies(String groupId, String artifactId) {

        // initial deps
        //
        Set<Artifact> artifacts = new LinkedHashSet<>();
        if (project.getDependencies() != null) {
            for (Dependency d : project.getDependencies()) {
                artifacts.add(repositorySystem.createDependencyArtifact(d));
            }
        }

        // and filter
        //
        Set<Artifact> projectArtifacts = new LinkedHashSet<>();
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
     * @return set of artifacts
     */
    private Set<Artifact> getProjectDependencies(Set<Artifact> artifacts) {

        final ArtifactFilter filter = artifact -> true;

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

        return artifactResolver.resolve(request).getArtifacts();
    }

    /**
     * Determine if this artifact is a DTM product artifact
     *
     * @param artifact artifact
     * @return true if its a product, false otherwise
     */
    boolean isDTMProduct(Artifact artifact) {
        return artifact.getGroupId().equals(DTM_GROUP_IDENTIFIER) &&
            artifact.getArtifactId().startsWith(SB_PRODUCT_ARTIFACT_PREFIX);
    }

    /**
     * Determine if this artifact is a SB product artifact
     *
     * @param artifact artifact
     * @return true if its a product, false otherwise
     */
    boolean isSBProduct(Artifact artifact) {
        return artifact.getGroupId().equals(SB_RT_GROUP_IDENTIFIER) &&
            artifact.getArtifactId().startsWith(SB_PRODUCT_ARTIFACT_PREFIX);
    }

    /**
     * Determine if this artifact is a DTM support artifact
     *
     * @param artifact artifact
     * @return true if its a product, false otherwise
     */
    boolean isDTMSupport(Artifact artifact) {
        return artifact.getGroupId().equals(DTM_GROUP_IDENTIFIER) &&
            artifact.getArtifactId().startsWith(SB_SUPPORT_ARTIFACT_PREFIX);
    }

    /**
     * Get product version based on dependencies
     *
     * @return product version
     */
    String getProductVersion() {

        // FIX THIS - DJS: We should assert these are all the same

        // FIX THIS - DJS: We should use transitive dependencies for the SDK artifact
        for (Artifact artifact : getProjectDependencies(DTM_GROUP_IDENTIFIER, DTM_SDK_ARTIFACT_IDENTIFIER)) {
            getLog().debug("Found product version " + artifact.getBaseVersion()
                + " from " + artifact);
            return artifact.getBaseVersion();
        }

        for (Artifact artifact : getProjectDependencies(DTM_GROUP_IDENTIFIER, DTM_MANAGEMENT_ARTIFACT_IDENTIFIER)) {
            getLog()
                .debug("Found product version " + artifact.getBaseVersion()
                    + " from " + artifact.toString());
            return artifact.getBaseVersion();
        }

        for (Artifact artifact : getProjectDependencies("zip")) {
            if (artifact.getGroupId().equals(DTM_GROUP_IDENTIFIER) && artifact.getArtifactId()
                .startsWith(SB_PRODUCT_ARTIFACT_PREFIX)) {
                getLog().debug("Found product version " + artifact
                    .getBaseVersion() + " from " + artifact.toString());
                return artifact.getBaseVersion();
            }
        }

        return "";
    }

    /**
     * Check environment before running mojo
     */
    public void prechecks() {

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
                        baseSubDirectory = artifact.getGroupId() + File.separator + artifact
                            .getArtifactId() + File.separator + artifact.getBaseVersion();
                        break;
                    }
                    if (isDTMProduct(artifact)) {
                        baseSubDirectory = artifact.getGroupId() + File.separator + artifact
                            .getArtifactId() + File.separator + artifact.getBaseVersion();
                        break;
                    }
                }
                productHome = new File(base, baseSubDirectory);

            }
        }
        getLog().debug("Product home set to " + productHome);
    }

    RuntimeCommandRunner newCommandRunner(AbstractCommandBuilder builder, String shortLocation, String longLocation) {
        return new RuntimeCommandRunner(getLog(), builder, shortLocation, longLocation);
    }

    <T> List<T> toNonNullList(T[] array) {
        if (array == null) {
            return new ArrayList<>();
        }
        return Arrays.asList(array);
    }

    /**
     * @param artifact The artifact
     * @return True if the artifact is a fragment
     */
    boolean isFragment(Artifact artifact) {
        String type = artifact.getType();
        return type != null &&
            (type.equals(JAVA_TYPE)
                || type.equals(EVENTFLOW_TYPE)
                || type.equals(TCS_TYPE)
                || type.equals(LIVEVIEW_TYPE));
    }

    enum VisitorDecision {
        KEEP,
        SKIP_STOP,
        SKIP_VISIT_DEPENDENCIES;
    }

    /**
     * The type of platform service to initialize
     */
    enum PlatformService {
        /**
         * Administration
         */
        ADMINISTRATION,
        /**
         * Build
         */
        CODE_GENERATION
    }

    /**
     * Flag to indicate if nested dependency should be returend or not.
     */
    enum FragmentDependencyMode {
        SKIP_NESTED_FRAGMENT,
        INCLUDE_FULL_DEPENDENCIES
    }

    /**
     * local classloader to be used to load admin jars
     */
    public static class PluginClassloader extends URLClassLoader {

        /**
         * Constructor
         *
         * @param urls   urls
         * @param parent parent
         */
        public PluginClassloader(URL[] urls, ClassLoader parent) {
            super(urls, parent);
        }

        /**
         * Add a URL to the class loader
         *
         * @param url The URL
         */
        public void addURL(URL url) {
            super.addURL(url);
        }
    }

    /**
     * Process the output of a process and write logs to maven getLog()
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
                InputStreamReader inpStrd = new InputStreamReader(inpStr, StandardCharsets.UTF_8);
                BufferedReader buffRd = new BufferedReader(inpStrd);
                String line = null;
                while ((line = buffRd.readLine()) != null) {
                    if (warn) {
                        getLog().warn("[" + strType + "] " + line);
                    } else {
                        getLog().info("[" + strType + "] " + line);
                    }
                    log += line;
                }
                buffRd.close();

            } catch (Exception e) {
                getLog().warn(e.getMessage());
            }
        }
    }

    private class DependencyVisitor implements DependencyNodeVisitor {

        private final Function<Artifact, VisitorDecision> visitorFilter;
        private final FragmentDependencyMode mode;
        private final Set<Artifact> gatheredDependencies = new LinkedHashSet<>();

        //  Dependencies of type "fragment" and their artifact name.
        //
        private final Map<String, String> fragmentDependencies = new HashMap<>();
        private int depth = 0;

        private DependencyVisitor(Function<Artifact, VisitorDecision> visitorFilter, FragmentDependencyMode mode) {
            this.visitorFilter = visitorFilter;
            this.mode = mode;
        }

        /**
         * @return The gathered dependencies after dependency computing/filtering
         */
        private Set<Artifact> getGatheredDependencies() {
            LinkedHashSet<Artifact> artifacts = new LinkedHashSet<>(gatheredDependencies);

            if (mode == FragmentDependencyMode.SKIP_NESTED_FRAGMENT) {

                //  Remove fragments nested in other fragments.
                //  FIX THIS (FL): remove nesting.
                //
                for (Iterator<Artifact> iterator = artifacts.iterator(); iterator.hasNext(); ) {

                    Artifact artifact = iterator.next();
                    String destName = artifact.getGroupId() + "-" + artifact
                        .getArtifactId() + "-" + artifact.getBaseVersion() + "-" + artifact
                        .getType() + ".zip";

                    if (fragmentDependencies.containsKey(destName)) {
                        getLog()
                            .debug("Skipping " + artifact + " since it is already included by "
                                + fragmentDependencies.get(destName));
                        iterator.remove();
                    }
                }
            }

            return artifacts;
        }

        private String indent() {
            StringBuilder t = new StringBuilder();
            for (int i = 0; i < depth; i++) {
                t.append("  ");
            }
            return t.toString();
        }

        @Override
        public boolean visit(DependencyNode node) {
            if (node.getParent() == null) {
                // root node - skip
                return true;
            }

            String indent = indent();
            depth = depth + 1;
            Artifact artifact = node.getArtifact();

            VisitorDecision decision = visitorFilter.apply(artifact);

            boolean visitDependencies;
            switch (decision) {

                case KEEP:

                    // add to list
                    //
                    gatheredDependencies.add(artifact);

                    if (isFragment(artifact) && mode == FragmentDependencyMode.SKIP_NESTED_FRAGMENT) {

                        getLog().debug(indent + node.getArtifact()
                            + " [adding (skip nested dependencies)]");

                        // fragments don't descend into dependencies
                        //
                        updateFragmentDependencies(node, artifact);
                        visitDependencies = false;

                    } else {

                        getLog().debug(indent + node.getArtifact() + " [adding]");

                        // keep going to check for dependencies of this artifact
                        //
                        visitDependencies = true;
                    }
                    break;

                case SKIP_STOP:

                    getLog().debug(indent + node.getArtifact()
                        + " [skipping (and skipping dependencies)]");
                    visitDependencies = false;
                    break;

                case SKIP_VISIT_DEPENDENCIES:

                    getLog().debug(indent + node.getArtifact()
                        + "[skipping]");
                    visitDependencies = true;
                    break;

                default:
                    throw new IllegalArgumentException("Decision: " + decision);
            }

            return visitDependencies;
        }

        private void updateFragmentDependencies(DependencyNode node, Artifact artifact) {
            // Read manifest so we can later see if we have duplicates
            //
            JarInputStream jarStream;
            try {
                jarStream = new JarInputStream(new FileInputStream(getArtifactPath(artifact)));
                Manifest mf = jarStream.getManifest();
                String fragmentList = mf.getMainAttributes()
                    .getValue(MANIFEST_TIBCO_EP_FRAGMENT_LIST);
                if (fragmentList != null && !fragmentList.isEmpty()) {
                    for (String fragmentDep : fragmentList.split(" ")) {
                        fragmentDependencies
                            .put(fragmentDep, node.getArtifact().toString());
                    }
                }
            } catch (IOException e) {
                // no manifest, ignore
                getLog().warn("No manifest could be read for: " + node.getArtifact()
                    .getFile(), e);
            }
        }

        @Override
        public boolean endVisit(DependencyNode node) {
            if (node.getParent() != null) {
                depth = depth - 1;
            }

            return true;
        }
    }
}
