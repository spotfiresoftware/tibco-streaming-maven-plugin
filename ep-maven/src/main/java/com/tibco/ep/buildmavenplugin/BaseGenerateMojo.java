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
import com.tibco.ep.sb.services.build.BuildTarget;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class BaseGenerateMojo extends BaseMojo {

    private final BuildTarget target;

    //  Maven parameters

    @Parameter(defaultValue = "false")
    Boolean ignoreUnboundCaptures;

    @Parameter
    Map<String, String> compilerProperties;

    @Parameter(required = false, property = "eventflowDirectories")
    File[] eventflowDirectories;

    @Parameter(required = false, property = "testEventflowDirectories")
    File[] testEventflowDirectories;

    private final List<String> failedBuilds = new ArrayList<>();

    protected BaseGenerateMojo(BuildTarget target) {
        this.target = target;
    }

    private File[] initializeAndCheck(File[] originalValue, String defaultDir) {

        if (originalValue == null ||originalValue.length == 0) {

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

        initializeService(PlatformService.CODE_GENERATION, ErrorHandling.FAIL);

        eventflowDirectories = initializeAndCheck(eventflowDirectories, "/src/main/eventflow");
        testEventflowDirectories = initializeAndCheck(testEventflowDirectories, "/src/test/eventflow");

        //  Construct the build parameters and trigger the build.
        //
        BuildParameters buildParameters = new BuildParameters();

        try {
            buildParameters
                .withCompilerProperties(compilerProperties == null
                    ? new HashMap<>() : compilerProperties)
                .withSourcePaths(toPaths(eventflowDirectories))
                .withTestSourcePaths(toPaths(testEventflowDirectories))
                .withCompileClassPath(toPaths(project.getCompileClasspathElements()))
                .withTestClassPath(toPaths(project.getCompileClasspathElements()))
                .withBuildDirectory(toPath(project.getBuild().getDirectory()));

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
        getBuildService().build(project.getName(), target, buildParameters,
            (entityName, entityPath, optionalException) -> {

                if (!optionalException.isPresent()) {
                    getLog().info("Module " + entityName + ": code generation SUCCESS");
                    return;
                }

                //  We have a failure.
                //
                failedBuilds.add(entityName);
                Exception error = optionalException.get();
                getLog().error(
                    "Module " + entityName + ": code generation FAILURE: " + error.getMessage());

                //  We don't want Maven to display a stack trace just because a build fail, so
                //  we push the log with the exception as "DEBUG". Users can still get it with -X.
                //
                getLog().debug("Exception for above failure", error);
            });

        if (!failedBuilds.isEmpty()) {

            throw new MojoExecutionException("Code generations failed: " + failedBuilds);
        }

        //  Add the generated source directory
        //
        addCompileSourceRoot();
    }

    private void addCompileSourceRoot() {

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

    private Path toPath(String uri) throws MojoExecutionException {
        try {
            assert !uri.startsWith("file://") : uri;
            URI uriObject = new URI("file://" + uri);

            return Paths.get(uriObject);
        } catch (URISyntaxException e) {
            throw new MojoExecutionException("Cannot parse URI: " + uri, e);
        }
    }

    private List<Path> toPaths(File[] files) {
        return Stream.of(files).map(File::toPath).collect(Collectors.toList());
    }
}
