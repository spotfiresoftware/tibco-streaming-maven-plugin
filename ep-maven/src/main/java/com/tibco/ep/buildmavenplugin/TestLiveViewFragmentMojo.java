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

import org.apache.maven.model.PluginExecution;
import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static org.apache.maven.plugins.annotations.LifecyclePhase.TEST;

import java.io.File;
import java.util.Properties;

/**
 * <p>Test a LiveView fragment using sbunit.</p>
 *
 * <p>A java runner for surefire is created and deployed to the test nodes -
 * this runner invokes surefire and hence junit to run the test cases.</p>
 *
 * <p>The java runner takes the configured reportsDirectory property and
 * appends the node name - the node specific test report files are created in
 * this directory.  When all the test have finished, these node specific test
 * reports are renamed into the configured reportsDirectory directory.</p>
 *
 * <p>The maven property jenkins.executionId.reportsDirectory is set so that
 * jenkins can locate the test reports.</p>
 *
 * <p>Java assertions are always enabled.  Options file is always ignored.</p>
 *
 * <p>The following system properties are set :</p>
 * <ul>
 * <li><b>com.tibco.ep.dtm.fragment.version</b> - fragment version</li>
 * <li><b>com.tibco.ep.dtm.fragment.identifier</b> - fragment identifier</li>
 * </ul>
 *
 * <p>Nodes are terminated on unit test failure, unless skipStop is set.</p>
 */
@Mojo(name = "test-liveview-fragment", defaultPhase = TEST, threadSafe = true)
public class TestLiveViewFragmentMojo extends BaseTestMojo {

    /**
     * <p>Eventflow source directories</p>
     *
     * <p>If no eventflowDirectories is specified, a single directory of
     * ${project.basedir}/src/main/eventflow is used.</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/eventflowDirectories.svg" alt="pom">
     *
     * <p>Example use on commandline:</p>
     * <img src="uml/eventflowDirectories-commandline.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter( required = false, property = "eventflowDirectories", defaultValue = "${project.basedir}/src/main/eventflow" )
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
    @Parameter( defaultValue = "${project.basedir}/src/main/liveview", required = true, property = "liveviewDirectory" )
    File liveviewDirectory;

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
    @Parameter( defaultValue = "${project.basedir}/src/main/configurations", required = true )
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
    @Parameter( defaultValue = "${project.basedir}/src/test/configurations", required = true )
    File testConfigurationDirectory;

    public void execute() throws MojoExecutionException {
        getLog().debug( "Testing live data mart fragment" );

        Properties modelProperties = project.getModel().getProperties();
        boolean testCasesFound = true;
        if (modelProperties.getProperty(TESTCASESFOUND_PROPERTY) != null && modelProperties.getProperty(TESTCASESFOUND_PROPERTY).equals("false")) {
            testCasesFound = false;
        }

        // determine if we have executions steps in the pom - if so we can skip id this
        // run is a default one
        //
        boolean hasExecutions = false;
        if (mojoExecution.getPlugin() != null && mojoExecution.getPlugin().getExecutions() != null) {
            for (PluginExecution p : mojoExecution.getPlugin().getExecutions()) {
                if (!p.getId().startsWith("default-")) {
                    hasExecutions = true;
                }
            }
        }

        if ((hasExecutions && mojoExecution.getExecutionId().startsWith("default-")) || skipTests || installOnly || !testCasesFound) {
            getLog().info("Tests are skipped (skipTests="+skipTests+",hasExecutions="+hasExecutions+",installOnly="+installOnly+",testCasesFound="+testCasesFound+")");
            return;
        }

        prechecks();

        Resource resource = new Resource();
        resource.setDirectory(configurationDirectory.getAbsolutePath());
        resource.setTargetPath("configurations");
        project.getBuild().addResource(resource);
        resource = new Resource();
        resource.setDirectory(testConfigurationDirectory.getAbsolutePath());
        resource.setTargetPath("test-configuration");
        project.getBuild().addTestResource(resource);

        initializeService(PlatformService.ADMINISTRATION, ErrorHandling.FAIL);

        // run test cases
        //
        newJunitTest()
            .withEventFlowDirectories(eventflowDirectories)
            .withLiveViewDirectory(liveviewDirectory)
            .run();
    }

}
