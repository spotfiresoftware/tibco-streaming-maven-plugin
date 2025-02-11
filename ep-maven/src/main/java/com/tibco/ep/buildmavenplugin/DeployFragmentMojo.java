/*******************************************************************************
 * Copyright (C) 2018-2025 Cloud Software Group, Inc.
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

import com.tibco.ep.sb.services.management.FragmentType;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import static org.apache.maven.plugins.annotations.LifecyclePhase.TEST;

/**
 * <p>Deploy an application fragment</p>
 *
 * <p>Example use in pom.xml:</p>
 * <img src="uml/deploy-fragment1.svg" alt="pom">
 *
 * <p>A LiveView fragment requires fragmentType, target and project set:</p>
 *
 * <img src="uml/deploy-fragment2.svg" alt="pom">
 */
@Mojo(name = "deploy-fragment", defaultPhase = TEST, threadSafe = true)
public class DeployFragmentMojo extends BaseTestMojo {

    /**
     * <p>Eventflow source directories</p>
     *
     * <p>If no eventflowDirectories is specified, a single directory of
     * ${project.basedir}/src/main/eventflow is used.</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/eventflowDirectories.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter(required = false, property = "eventflowDirectories", defaultValue = "${project.basedir}/src/main/eventflow")
    File[] eventflowDirectories;

    /**
     * <p>Liveview source directory</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/liveviewDirectory.svg" alt="pom">
     *
     * <p>Example use on commandline:</p>
     * <img src="uml/liveviewDirectory-commandline.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/liveview", required = true, property = "liveviewDirectory")
    File liveviewDirectory;

    /**
     * <p>Fragment parameters</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/fragment-arguments.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter
    String[] arguments;

    /**
     * <p>Target to deploy</p>
     *
     * <p>
     * Target can a main class name, a JAR with a main class name in the manifest, a
     * fully scoped EventFlow module name, an EventFlow sbapp or ssql file, or the
     * keyword "liveview"</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/fragment-target.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter
    String target;
    /**
     * <p>Fragment type</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/fragment-fragmentType.svg" alt="pom">
     *
     * <p>One of JAVA, STREAMBASE or LIVEVIEW</p>
     */
    @Parameter(defaultValue = "JAVA")
    FRAGMENT fragmentType;

    public void execute() throws MojoExecutionException {
        getLog().debug("Deploy");


        // determine if we have executions steps in the pom - if so we can skip id this
        // run is a default one
        //
        boolean hasExecutions = false;
        if (mojoExecution.getPlugin() != null && mojoExecution.getPlugin()
            .getExecutions() != null) {
            for (PluginExecution p : mojoExecution.getPlugin().getExecutions()) {
                if (!p.getId().startsWith("default-")) {
                    hasExecutions = true;
                }
            }
        }

        if ((hasExecutions && mojoExecution.getExecutionId().startsWith("default-")) || skipTests) {
            getLog()
                .info("Deploy is skipped (skipTests=" + skipTests + ",hasExecutions=" + hasExecutions + ")");
            return;
        }

        prechecks();

        initializeService(PlatformService.ADMINISTRATION, ErrorHandling.FAIL);

        List<String> applicationArguments = new ArrayList<>();

        if ((arguments != null) && (arguments.length != 0)) {
            applicationArguments.addAll(Arrays.asList(arguments));
        }

        String thisServiceName = serviceName;
        if (serviceName == null || serviceName.length() == 0) {
            thisServiceName = clusterName;
        }

        // form list of java options - set in pom + command line
        //
        List<String> fullJavaOptions = new ArrayList<>(toNonNullList(javaOptions));
        fullJavaOptions.addAll(toNonNullList(optionsProperty));

        if (systemPropertyVariables != null && systemPropertyVariables.size() > 0) {
            for (Entry<String, String> entry : systemPropertyVariables.entrySet()) {
                fullJavaOptions.add("-D" + entry.getKey() + "=" + entry.getValue());
            }
        }

        newDeployment(FragmentType.fromParameter(fragmentType.toString()), target)
            .withServiceName(thisServiceName)
            .withJavaOptions(fullJavaOptions)
            .withApplicationArguments(applicationArguments)
            .withEventFlowDirectories(toNonNullList(eventflowDirectories))
            .withLiveViewDirectory(liveviewDirectory)
            .withWait(false)
            .run();
    }

    /**
     * Fragment type
     */
    public enum FRAGMENT {
        /**
         * Java
         */
        JAVA,
        /**
         * StreamBase
         */
        STREAMBASE,
        /**
         * Liveview data mart
         */
        LIVEVIEW
    }

}
