/*
 * Copyright (C) 2020, TIBCO Software Inc.
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

import com.tibco.ep.sb.services.build.BuildParameters;
import com.tibco.ep.sb.services.build.BuildResult;
import com.tibco.ep.sb.services.build.BuildTarget;
import com.tibco.ep.sb.services.build.IBuildNotifier;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Base class for code generation MOJO
 */
public abstract class BaseGenerateMojo extends BaseMojo {

    public static final Logger LOGGER = LoggerFactory.getLogger(BaseGenerateMojo.class);
    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
    private final BuildTarget target;

    //  Maven parameters
    private final List<String> failedBuilds = new ArrayList<>();
    @Parameter(defaultValue = "false")
    Boolean ignoreUnboundCaptures;
    @Parameter
    Map<String, String> compilerProperties;
    @Parameter(required = false, property = "eventflowDirectories")
    File[] eventflowDirectories;
    @Parameter(required = false, property = "testEventflowDirectories")
    File[] testEventflowDirectories;
    @Parameter(required = false, property = "failFast", defaultValue = "false")
    Boolean failFast;
    @Parameter(required = false, property = "skipGenerateSources", defaultValue = "false")
    Boolean skipGenerateSources;


    /**
     * <p>Additional resources directory for HOCON configurations</p>
     *
     * <p>This is added to the list of resource directories</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/package-configurationDirectory.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/configurations", required = true)
    File configurationDirectory;

    /**
     * <p>Additional resources directory for test HOCON configurations</p>
     *
     * <p>This is added to the list of test resource directories</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/package-testConfigurationDirectory.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "${project.basedir}/src/test/configurations", required = true)
    File testConfigurationDirectory;

    /**
     * @param target The build target
     */
    protected BaseGenerateMojo(BuildTarget target) {
        this.target = target;
    }

    private File[] initializeAndCheck(File[] originalValue, String defaultDir) {

        if (originalValue == null || originalValue.length == 0) {

            //  Default value, if it exists.
            //
            File defaultValue = new File(project.getBasedir(), defaultDir);
            if (defaultValue.exists() && defaultValue.isDirectory()) {
                return new File[]{defaultValue};
            } else {
                return new File[]{};
            }
        }

        return originalValue;
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {

        if (skipGenerateSources) {
            getLog().warn("Skipping code generation entirely");
            return;
        }

        prechecks();
        initializeService(PlatformService.CODE_GENERATION, ErrorHandling.FAIL);

        eventflowDirectories = initializeAndCheck(eventflowDirectories, "/src/main/eventflow");
        testEventflowDirectories = initializeAndCheck(testEventflowDirectories, "/src/test/eventflow");

        setupJavaTemporaryDirectory();

        //  Construct the build parameters and trigger the build.
        //
        BuildParameters buildParameters = new BuildParameters();

        try {
            buildParameters
                .withCompilerProperties(compilerProperties == null
                    ? new HashMap<>() : compilerProperties)
                .withSourcePaths(toPathsCheckExist(eventflowDirectories))
                .withTestSourcePaths(toPathsCheckExist(testEventflowDirectories))
                .withCompileClassPath(getCompileClassPath())
                .withTestClassPath(getTestClassPath())
                .withConfigurationDirectory(configurationDirectory.toPath())
                .withTestConfigurationDirectory(testConfigurationDirectory.toPath())
                .withBuildDirectory(toPath(project.getBuild().getDirectory()))
                .withProductHome(productHome.toPath());

        } catch (DependencyResolutionRequiredException e) {
            throw new MojoExecutionException("Cannot resolve dependency", e);
        }

        buildParameters.getCompileClassPath().stream()
            .map(Path::toString)
            .sorted()
            .forEach(p -> getLog().debug("Compile ClassPath: " + p));
        buildParameters.getTestClassPath().stream()
            .map(Path::toString)
            .sorted()
            .forEach(p -> getLog().debug("Test ClassPath: " + p));

        //  Now trigger the build and report errors.
        //
        try {

            IBuildNotifier notifier = new IBuildNotifier() {

                @Override
                public void onBuildStarted(int nbModules) {
                    getLog().info("Found " + nbModules + " modules");
                }

                @Override
                public void onBuildCompleted() {
                    //  Do nothing
                }

                @Override
                public void onSkipped(String entityName) {
                    getLog().info("Module " + entityName + ": code generation SKIPPED");
                }

                @Override
                public void onStarted(String entityName) {
                    getLog().info("Module " + entityName + ": code generation STARTED");
                }

                @Override
                public void onCompleted(BuildResult result) {

                    double seconds = result.getElapsedTimeMillis();
                    seconds /= TimeUnit.MILLISECONDS.convert(1, TimeUnit.SECONDS);

                    if (!result.getException().isPresent()) {
                        getLog().info("Module " + result.getEntityName()
                            + ": code generation SUCCESS"
                            + " (in " + String.format("%.3f", seconds) + " seconds)");
                        return;
                    }

                    //  We have a failure.
                    //
                    Exception error = result.getException().get();
                    failedBuilds.add(result.getEntityName() + ": " + error.getMessage());
                    getLog().error("Module " + result.getEntityName()
                        + ": code generation FAILURE: " + error.getMessage()
                        + " (in " + String.format("%.3f", seconds) + " seconds)");

                    //  We don't want Maven to display a stack trace just because a build fail, so
                    //  we push the log with the exception as "DEBUG". Users can still get it with -X.
                    //
                    getLog().debug("Exception for above failure", error);

                    if (failFast) {
                        throw new FailFastException();
                    }
                }
            };

            getBuildService().build(project.getName(), target, buildParameters, notifier);

        } catch (FailFastException ffe) {

            //  This is just used to get to failures quicker.
            //
            assert !failedBuilds.isEmpty();
        }

        if (!failedBuilds.isEmpty()) {

            throw new MojoExecutionException("Code generation failed: " + failedBuilds);
        }

        //  Add the generated source directory
        //
        addGeneratedSourceRoot();
    }

    private void setupJavaTemporaryDirectory() throws MojoExecutionException {

        Path tempDirectory = Paths.get(project.getBuild().getDirectory()).resolve("tmp");
        String tempDirectoryString = tempDirectory.toAbsolutePath().toFile().toString();
        System.setProperty(JAVA_IO_TMPDIR, tempDirectoryString);

        //  Create the directory.
        //
        tempDirectory.toFile().mkdirs();
        if (!tempDirectory.toFile().exists()) {
            throw new MojoExecutionException("Could not create: " + tempDirectory);
        }
        getLog().debug(JAVA_IO_TMPDIR + " set to " + tempDirectoryString);
    }

    private List<Path> getTestClassPath() throws MojoExecutionException, DependencyResolutionRequiredException {
        //  FIX THIS (FL): not good, to be finalized when getCompileClassPath() is ok.
        return toPaths(project.getTestClasspathElements());
    }

    private List<Path> getCompileClassPath() throws MojoExecutionException, DependencyResolutionRequiredException {

        //  Filter out compile class path elements that do not exist.
        //
        LinkedHashSet<Path> classpath = toPaths(project.getCompileClasspathElements()).stream()
            .filter(p -> p.toFile().exists()).collect(Collectors.toCollection(LinkedHashSet::new));

        //  Then dependencies.
        //
        visitDependencies((currentProjectArtefactDependency, indent, context) -> {

            if (target == BuildTarget.TEST
                && currentProjectArtefactDependency.getScope().equals((Artifact.SCOPE_TEST))) {

                getLog().debug(indent + currentProjectArtefactDependency
                    + " [skipping (and skipping dependencies)]");
                return false;
            }

            getLog().debug(indent + currentProjectArtefactDependency + " [adding]");

            classpath.add(currentProjectArtefactDependency.getFile().toPath());

            return true;
        });

        return new ArrayList<>(classpath);
    }

    private void addGeneratedSourceRoot() {

        //  FIX THIS (FL) The generated source path is hardcoded here and sb-server.
        //  Should we make this configurable from the MOJO ?
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

    private List<Path> toPaths(List<String> stringPaths) throws MojoExecutionException {
        List<Path> list = new ArrayList<>();
        for (String p : stringPaths) {
            list.add(toPath(p));
        }
        return list;
    }

    private Path toPath(String file) throws MojoExecutionException {
        return new File(file).toPath();
    }

    private List<Path> toPathsCheckExist(File[] files) {
        return Stream.of(files)
            .filter(File::exists)
            .map(File::toPath).collect(Collectors.toList());
    }

    private List<Path> toPaths(File[] files) {
        return Stream.of(files).map(File::toPath).collect(Collectors.toList());
    }
}
