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
import com.tibco.ep.sb.services.management.AbstractCommandBuilder;
import com.tibco.ep.sb.services.management.IRuntimeAdminService;
import com.tibco.ep.sb.services.management.IContext;
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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public IRuntimeAdminService getAdminService() {
        return adminService;
    }

    /**
     * @return The Runtime Administration context
     */
    public IContext getContext() {
        return context;
    }

    /**
     * Initialize administration
     * <p>
     * Admin jars are loaded and classes discovered
     *
     * @param errorHandling error handling
     * @throws MojoExecutionException if initialize fails
     */
    void initializeAdministration(ErrorHandling errorHandling) throws MojoExecutionException {

        if (classLoader == null) {
            AccessController.doPrivileged(new PrivilegedAction<Void>() {
                public Void run() {
                    classLoader = new PluginClassloader(new URL[0], this.getClass()
                        .getClassLoader());
                    return null;
                }
            });
        }


        if (adminService == null) {
            loadAdministrationJars();

            adminService = RuntimeServices.getAdminService(classLoader);
            if (adminService == null) {
                String message = "Cannot get service " + IRuntimeAdminService.class;

                if (errorHandling == ErrorHandling.FAIL) {
                    throw new MojoExecutionException(message);
                } else {
                    getLog().warn(message + " [ignored]");
                }
            }

            context = adminService.newContext(productHome.toPath());
            if (getLog().isDebugEnabled()) {
                context.withTracingEnabled();
            }
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

        loadArtifact(DTM_GROUP_IDENTIFIER, DTM_MANAGEMENT_ARTIFACT_IDENTIFIER, productVersion);
        loadArtifact(SB_GROUP_IDENTIFIER, SB_SERVER_ARTIFACT_IDENTIFIER, productVersion);
    }

    private void loadArtifact(String groupIdentifier, String artifactIdentifier, String productVersion) throws MojoExecutionException {

        getLog().debug(
            "Loading: " + groupIdentifier + ":" + artifactIdentifier + ":" + productVersion);
        Artifact managementAPIArtifact = repositorySystem
            .createArtifact(groupIdentifier, artifactIdentifier, productVersion, "", "jar");

        Set<Artifact> baseArtifacts = new LinkedHashSet<>();

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
     * Load a single jar and report success
     *
     * @param fileName file to load
     * @return true if loaded successfully
     */
    private boolean loadJar(String fileName) {

        File file = new File(fileName);
        if (!file.exists()) {
            getLog().debug("Failed to find " + fileName + " file doesn't exist");
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

        Set<Artifact> deps = new LinkedHashSet<Artifact>();

        Map<String, String> fragmentDeps = new HashMap<>();

        ProjectBuildingRequest buildingRequest = new DefaultProjectBuildingRequest(session
            .getProjectBuildingRequest());
        buildingRequest.setProject(project);
        try {
            DependencyNode rootNode = dependencyGraphBuilder
                .buildDependencyGraph(buildingRequest, null, reactorProjects);

            DependencyNodeVisitor visitor = new DependencyNodeVisitor() {
                int depth = 0;

                private String indent() {
                    StringBuffer t = new StringBuffer();
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
                    String scope = artifact.getScope();
                    String type = artifact.getType();
                    if (scope == null || scope.equals(Artifact.SCOPE_COMPILE) || scope
                        .equals(Artifact.SCOPE_RUNTIME)) {
                        // compile dependency - add to list
                        //
                        deps.add(artifact);

                        // fragments don't descend into dependencies
                        //
                        if (type != null && (type.equals(JAVA_TYPE) || type
                            .equals(EVENTFLOW_TYPE) || type.equals(TCS_TYPE) || type
                            .equals(LIVEVIEW_TYPE))) {
                            getLog().debug(indent + node.getArtifact() + " [adding]");

                            // Read manifest so we can later see if we have duplicates
                            //
                            JarInputStream jarStream;
                            try {
                                jarStream = new JarInputStream(new FileInputStream(getArtifactPath(artifact)));
                                Manifest mf = jarStream.getManifest();
                                String fragemntList = mf.getMainAttributes()
                                    .getValue("TIBCO-EP-Fragment-List");
                                if (fragemntList != null && !fragemntList.isEmpty()) {
                                    for (String fragmentDep : fragemntList.split(" ")) {
                                        fragmentDeps
                                            .put(fragmentDep, node.getArtifact().toString());
                                    }
                                }
                            } catch (IOException e) {
                                // no manifest, ignore
                            }

                            return false;
                        } else {
                            getLog().debug(indent + node.getArtifact() + " [adding]");

                            // keep going to check for dependencies of this artifact
                            //
                            return true;
                        }
                    } else if (scope.equals(Artifact.SCOPE_PROVIDED) || scope
                        .equals(Artifact.SCOPE_TEST)) {
                        // provided and test skip
                        //
                        getLog().debug(indent + node.getArtifact() + " [skipping]");
                        return false;
                    }

                    getLog().debug(indent + node.getArtifact() + " [skipping]");

                    return true;
                }

                @Override
                public boolean endVisit(DependencyNode node) {
                    if (node.getParent() != null) {
                        depth = depth - 1;
                    }

                    return true;
                }
            };

            rootNode.accept(visitor);


        } catch (DependencyGraphBuilderException e) {
            throw new MojoExecutionException(e.getMessage());
        }

        // sort out any duplicates
        //
        for (Iterator<Artifact> iterator = deps.iterator(); iterator.hasNext(); ) {
            Artifact artifact = iterator.next();
            String destName = artifact.getGroupId() + "-" + artifact
                .getArtifactId() + "-" + artifact.getBaseVersion() + "-" + artifact
                .getType() + ".zip";
            if (fragmentDeps.containsKey(destName)) {
                getLog()
                    .debug("Skipping " + artifact + " since it is already included by " + fragmentDeps
                        .get(destName));
                iterator.remove();
            }
        }
        return deps;
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
            for (Dependency d : (List<Dependency>) project.getDependencies()) {
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
        ArtifactResolutionResult r = artifactResolver.resolve(request);
        artifacts = r.getArtifacts();

        return artifacts;
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

}
