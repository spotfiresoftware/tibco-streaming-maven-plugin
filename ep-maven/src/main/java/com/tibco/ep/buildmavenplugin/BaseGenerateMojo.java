/*
 * Copyright (C) 2020-2025 Cloud Software Group, Inc.
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
 */

package com.tibco.ep.buildmavenplugin;

import com.tibco.ep.sb.services.build.BuildExceptionDetails;
import com.tibco.ep.sb.services.build.BuildParameters;
import com.tibco.ep.sb.services.build.BuildResult;
import com.tibco.ep.sb.services.build.BuildTarget;
import com.tibco.ep.sb.services.build.IBuildNotifier;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for code generation MOJO
 */
public abstract class BaseGenerateMojo extends BaseMojo {

    private static final String COMPILER_PROPERTIES_EQUALS = "=";
    private static final String ENGINE_DATA_AREA = "com.tibco.ep.dtm.engine.data.area";

    private final BuildTarget target;
    private final List<String> failedBuilds = new ArrayList<>();

    //  Maven parameters
    //
    /**
     * <p>The compiler properties used during EventFlow typecheck/build</p>
     *
     * <p>Example use in pom.xml:</p>
     * <pre>
     * &lt;configuration&gt;
     *   ...
     *   &lt;compilerProperties&gt;
     *      &lt;compilerProperty&gt;streambase.runtime.stream-queue-timeout=1500&lt;/compilerProperties&gt;
     *      &lt;compilerProperty&gt;streambase.runtime.stream-queue-timeout-report-threshold=10&lt;/compilerProperties&gt;
     *   &lt;/compilerProperties&gt;
     *   ...
     * &lt;/configuration&gt;
     * </pre>
     *
     * <p>Example use on commandline:</p>
     * <pre>
     *     mvn install -DcompilerProperties=streambase.runtime.stream-queue-timeout=1500,streambase.runtime.stream-queue-timeout-report-threshold=10
     * </pre>
     *
     * @since 2.0.0
     */
    @Parameter(property = "compilerProperties", required = false)
    String[] compilerPropertiesEntries;
    Map<String, String> compilerProperties;


    /**
     * <p>The "main" EventFlow directories</p>
     *
     * <p>Default value is: ${project.basedir}/src/main/eventflow</p>
     *
     * <p>Example use in pom.xml:</p>
     * <pre>
     * &lt;configuration&gt;
     *   ...
     *   &lt;eventflowDirectories&gt;
     *      &lt;eventflowDirectory&gt;${project.basedir}/src/main/eventflow&lt;/eventflowDirectory&gt;
     *      &lt;eventflowDirectory&gt;${project.basedir}/src/main/other&lt;/eventflowDirectory&gt;
     *   &lt;/eventflowDirectories&gt;
     *   ...
     * &lt;/configuration&gt;
     * </pre>
     *
     * <p>Example use on commandline:</p>
     * <pre>
     *     mvn install -DeventflowDirectories=src/main/eventflow,src/main/other
     * </pre>
     *
     * @since 2.0.0
     */
    @Parameter(required = false, property = "eventflowDirectories", defaultValue = "${project.basedir}/src/main/eventflow")
    File[] eventflowDirectories;

    /**
     * <p>The "test" EventFlow directories</p>
     *
     * <p>Default value is: src/test/eventflow</p>
     *
     * <p>Example use in pom.xml:</p>
     * <pre>
     * &lt;configuration&gt;
     *   ...
     *   &lt;testEventflowDirectories&gt;
     *      &lt;testEventflowDirectory&gt;${project.basedir}/src/test/eventflow&lt;/testEventflowDirectory&gt;
     *      &lt;testEventflowDirectory&gt;${project.basedir}/src/test/other&lt;/testEventflowDirectory&gt;
     *   &lt;/testEventflowDirectories&gt;
     *   ...
     * &lt;/configuration&gt;
     * </pre>
     *
     * <p>Example use on commandline:</p>
     * <pre>
     *     mvn install -testEventflowDirectories=src/test/eventflow,src/test/other
     * </pre>
     *
     * @since 2.0.0
     */
    @Parameter(required = false, property = "testEventflowDirectories", defaultValue = "${project.basedir}/src/test/eventflow")
    File[] testEventflowDirectories;

    /**
     * <p>DEVELOPMENT ONLY, DO NOT USE.</p>
     */
    @Parameter(required = false, property = "failFast", defaultValue = "false")
    Boolean failFast;

    /**
     * <p>DEVELOPMENT ONLY, DO NOT USE.</p>
     */
    @Parameter(required = false, property = "skipGenerateSources", defaultValue = "false")
    Boolean skipGenerateSources;

    /**
     * <p>Additional resources directory for HOCON configurations</p>
     *
     * <p>This is added to the list of resource directories</p>
     *
     * <p>Example use in pom.xml:</p>
     * <pre>
     * &lt;configuration&gt;
     *   ...
     *   &lt;configurationDirectory&gt;${project.basedir}/src/main/confs&lt;/configurationDirectory&gt;
     *   ...
     * &lt;/configuration&gt;
     * </pre>
     *
     * @since 2.0.0
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/configurations", required = true)
    File configurationDirectory;

    /**
     * <p>Additional resources directory for test HOCON configurations</p>
     *
     * <p>This is added to the list of test resource directories</p>
     *
     * <p>Example use in pom.xml:</p>
     * <pre>
     * &lt;configuration&gt;
     *   ...
     *   &lt;testConfigurationDirectory&gt;${project.basedir}/src/main/confs&lt;/testConfigurationDirectory&gt;
     *   ...
     * &lt;/configuration&gt;
     * </pre>
     *
     * @since 2.0.0
     */
    @Parameter(defaultValue = "${project.basedir}/src/test/configurations", required = true)
    File testConfigurationDirectory;

    /**
     * @param target The build target
     */
    protected BaseGenerateMojo(BuildTarget target) {
        this.target = target;
    }

    private static List<Path> toUniqueAndExistingPaths(List<String> pathElements) throws MojoExecutionException {
        LinkedHashSet<Path> paths = new LinkedHashSet<>(toPaths(pathElements));
        return paths.stream()
            .filter(p -> p.toFile().exists())
            .collect(Collectors.toList());

    }

    private static List<Path> toPaths(List<String> stringPaths) throws MojoExecutionException {
        List<Path> list = new ArrayList<>();
        for (String p : stringPaths) {
            list.add(toPath(p));
        }
        return list;
    }

    private static Path toPath(String file) throws MojoExecutionException {
        return new File(file).toPath();
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skipGenerateSources) {
            getLog().warn("Skipping code generation entirely");
            return;
        }

        //  Transform the compiler properties into a map.
        //
        compilerProperties = new HashMap<>();
        for (String prop : compilerPropertiesEntries) {
            String[] propSplit = prop.split(COMPILER_PROPERTIES_EQUALS);
            if (propSplit.length != 2) {
                throw new MojoExecutionException("Illegal compiler property (not 'key=value'): "
                    + prop);
            }

            compilerProperties.put(propSplit[0], propSplit[1]);
        }

        prechecks();
        initializeService(PlatformService.CODE_GENERATION, ErrorHandling.FAIL);

        setupEngineDataArea();

        //  Construct the build parameters and trigger the build.
        //
        BuildParameters buildParameters = new BuildParameters();

        try {
            buildParameters
                .withProjectRootDirectory(project.getBasedir().toPath());

            buildParameters
                .withCompilerProperties(compilerProperties == null
                    ? new HashMap<>() : compilerProperties)
                .withSourcePaths(toPathsCheckExist(eventflowDirectories))
                .withTestSourcePaths(toPathsCheckExist(testEventflowDirectories));

            buildParameters
                .withProjectCompileClassPath(
                    toUniqueAndExistingPaths(project.getCompileClasspathElements()))
                .withProjectTestCompileClassPath(
                    toUniqueAndExistingPaths(project.getTestClasspathElements()));

            buildParameters
                .withDependenciesCompileClassPath(getDependencyClassPaths(BuildTarget.MAIN))
                .withDependenciesTestCompileClassPath(getDependencyClassPaths(BuildTarget.TEST));

            buildParameters
                .withConfigurationDirectory(configurationDirectory.toPath())
                .withTestConfigurationDirectory(testConfigurationDirectory.toPath())
                .withBuildDirectory(toPath(project.getBuild().getDirectory()))
                .withProductHome(productHome.toPath());

        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Cannot resolve dependency", e);
        }

        logPaths("Project Compile ClassPath", buildParameters.getProjectCompileClassPath());
        logPaths("Dependencies Compile ClassPath", buildParameters.getDependenciesCompileClassPath());
        logPaths("Project Test Compile ClassPath", buildParameters.getProjectTestCompileClassPath());
        logPaths("Dependencies Test Compile ClassPath", buildParameters.getDependenciesTestCompileClassPath());

        //  Now trigger the build and report errors.
        //
        BuildNotifier notifier = new BuildNotifier();
        try {

            getBuildService()
                .build(project.getName(), target, buildParameters, notifier);

        } catch (FailFastException ffe) {

            //  This is just used to get to failures quicker.
            //
            assert !failedBuilds.isEmpty();
        }

        if (!failedBuilds.isEmpty()) {
            throw new MojoExecutionException(
                "Code generation failed:\n(See above for stacks)\n"
                    + String.join("\n", failedBuilds));
        }

        //  Now update the list of generated modules.
        //  This list will be used to construct the manifest (so, we only do that for MAIN).
        //
        if (target == BuildTarget.MAIN) {
            notifier.moduleData.write(project);
        }

        //  Add the generated source directory
        //
        addGeneratedSourceRoot();

    }

    private void logPaths(String header, List<Path> paths) {

        if (paths.isEmpty()) {
            getLog().debug(header + ": (none)");
            return;
        }

        getLog().debug(header + ":");
        paths.stream()
            .map(Path::toString)
            .sorted()
            .forEach(p -> getLog().debug("  " + p));
    }

    private void setupEngineDataArea() throws MojoExecutionException {

        //  This is used by ResourcePathResolvers to copy data into.
        //
        Path tempDirectory = Paths.get(project.getBuild().getDirectory()).resolve("tmp");
        String tempDirectoryString = tempDirectory.toAbsolutePath().toFile().toString();
        System.setProperty(ENGINE_DATA_AREA, tempDirectoryString);

        //  Create the directory.
        //
        tempDirectory.toFile().mkdirs();
        if (!tempDirectory.toFile().exists()) {
            throw new MojoExecutionException("Could not create: " + tempDirectory);
        }
        getLog().debug(ENGINE_DATA_AREA + " set to " + tempDirectoryString);
    }

    private List<Path> getDependencyClassPaths(BuildTarget target) throws MojoExecutionException {
        Set<Path> classpath = new LinkedHashSet<>();

        getLog().debug("Visiting dependencies for target: " + target);
        visitDependencies((currentProjectArtefactDependency, indent, context) -> {

            if (this.target == BuildTarget.MAIN) {
                //  If we're building MAIN, we don't want TEST scope dependencies.
                //  But if we're building TEST, we want them all (no filtering).
                //
                if (currentProjectArtefactDependency.getScope().equals((Artifact.SCOPE_TEST))) {

                    getLog().debug(indent + currentProjectArtefactDependency
                        + " [skipping (and skipping dependencies): TEST scope]");
                    return false;
                }
            }

            getLog().debug(indent + currentProjectArtefactDependency + " [adding]");

            classpath.add(currentProjectArtefactDependency.getFile().toPath());

            return true;
        });

        return new ArrayList<>(classpath);
    }

    private void addGeneratedSourceRoot() {

        //  FIX THIS (FL) The generated source path is hardcoded here and sb-server.
        //  We should get this out of the build, possibly through the notifier.
        //
        String directory = project.getBuild().getDirectory() + "/generated-"
            + (target == BuildTarget.MAIN ? "" : "test-")
            + "sources/streaming";

        if (target == BuildTarget.MAIN) {
            project.addCompileSourceRoot(directory);
        } else {
            assert target == BuildTarget.TEST : target;
            project.addTestCompileSourceRoot(directory);
        }
    }

    private List<Path> toPathsCheckExist(File[] files) {
        return Stream.of(files)
            .filter(File::exists)
            .map(File::toPath).collect(Collectors.toList());
    }

    private class BuildNotifier implements IBuildNotifier {

        private final ProjectModuleData moduleData = new ProjectModuleData();

        @Override
        public void onBuildStarted(int nbModules) {
            getLog().info("Found " + nbModules + " module" + (nbModules == 1 ? "" : "s"));
        }

        @Override
        public void onBuildCompleted() {
            //  Do nothing
        }

        @Override
        public void onSkipped(String entityName, String extension) {
            getLog().debug("Module " + entityName + ": code generation SKIPPED");
            moduleData.addModule(entityName, extension);
        }

        @Override
        public void onStarted(String entityName, String extension) {
            getLog().debug("Module " + entityName + ": code generation STARTED");
            moduleData.addModule(entityName, extension);
        }

        @Override
        public void onWarning(String entityName, String warning) {
            IBuildNotifier.super.onWarning(entityName, warning);
            getLog().warn("Module " + entityName + ": " + warning);
        }

        @Override
        public void onCompleted(BuildResult result) {

            double seconds = result.getElapsedTimeMillis();
            seconds /= TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);

            String entityName = result.getEntityName();

            if (!result.getException().isPresent()) {
                getLog().debug("Module " + entityName
                    + ": code generation SUCCESS"
                    + " (in " + String.format("%.3f", seconds) + " seconds)");
                return;
            }

            //  We have a failure.
            //
            getLog().error("Module " + entityName
                + ": code generation FAILURE"
                + " (in " + String.format("%.3f", seconds) + " seconds)");

            Exception error = result.getException().get();
            List<BuildExceptionDetails> detailList = getBuildService().getDetails(error);

            assert !detailList.isEmpty() : entityName;
            failedBuilds.add(entityName + ": " + detailList.get(0).getShortMessage());

            int i = 0;
            for (BuildExceptionDetails details : detailList) {

                String header = "";
                if (i > 0) {
                    header += " [" + i + "] ";
                }

                if (details.getLocation() != null) {
                    getLog().error(header + "Location: " + details.getLocation());
                }

                getLog().error(header + "Error: " + details.getShortMessage());

                if (details.getLongDescription() != null) {
                    getLog().error(
                        header + "Detailed error: " + details.getLongDescription());
                }

                if (i == 0 && detailList.size() > 1) {
                    getLog().error("Nested causes:");
                }
                i++;
            }

            getLog().error("Exception stack:");
            List<String> causes = new ArrayList<>();
            Throwable current = error;
            while (current != null) {
                causes.add("   At " + current.getStackTrace()[0].toString() + ": " + current
                    .toString());
                current = current.getCause();
            }

            Collections.reverse(causes);
            causes.forEach(c -> getLog().error(c));

            //  We don't want Maven to display a stack trace just because a build fail, so
            //  we push the log with the exception as "DEBUG". Users can still get it with -X.
            //
            getLog().debug("Exception for above failure", error);

            //  Stop on first error if needed.
            //
            if (failFast) {
                throw new FailFastException();
            }
        }
    }
}
