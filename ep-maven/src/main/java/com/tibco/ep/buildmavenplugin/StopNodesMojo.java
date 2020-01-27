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
import java.util.Properties;

import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;


/**
 * <p>Stop and remove nodes.</p>
 * 
 * <p>When executed in clean phase, any errors found will be logged but won't 
 * cause the build to fail.  When executed in a phase other than clean, a stop 
 * failure will fail the build.</p>
 */
@Mojo(name = "stop-nodes", threadSafe = true)
public class StopNodesMojo extends BaseExecuteMojo {

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
     * <p>Set this to 'true' to skip running tests, but still compile them.</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/start-nodes-skipTests.svg" alt="pom">
     * 
     * <p>Example use on commandline:</p>
     * <img src="uml/start-nodes-skipTests-commandline.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter(property = "skipTests")
    boolean skipTests;
    
    /**
     * <p>Set this to 'true' to skip removing test nodes</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/stop-nodes-skipRemove.svg" alt="pom">
     * 
     * <p>Example use on commandline:</p>
     * <img src="uml/stop-nodes-skipRemove-commandline.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter(property = "skipRemove", defaultValue="false")
    boolean skipRemove;

    /**
     * <p>Set this to 'true' to only install nodes</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/start-nodes-installOnly.svg" alt="pom">
     * 
     * <p>Example use on commandline:</p>
     * <img src="uml/start-nodes-installOnly-commandline.svg" alt="pom">
     * 
     * @since 1.1.0
     */
    @Parameter(property = "installOnly")
    boolean installOnly;
    
    /**
     * Set this to 'false' to ignore errors on node stop
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/stop-nodes-failOnStopError.svg" alt="pom">
     * 
     * <p>Example use on commandline:</p>
     * <img src="uml/stop-nodes-failOnStopError-commandline.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter(property = "failOnStopError", defaultValue="true")
    boolean failOnStopError;
    
    /**
     * <p>List of node names to start ready for test cases to run.  If not set, a
     * single node <b>A</b> is used.</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/start-nodes-nodes.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter
    String[] nodes = new String[] {"A"};

    /**
     * Installation path - overrides servicename, nodes are ignored
     *
     * @since 1.6.0
     */
    @Parameter
    String installPath;
    
    public void execute() throws MojoExecutionException {
        getLog().debug("Stop nodes");

        prechecks();

        Properties modelProperties = project.getModel().getProperties();
        boolean testCasesFound = true;
        if (modelProperties.getProperty(TESTCASESFOUND_PROPERTY) != null && modelProperties.getProperty(TESTCASESFOUND_PROPERTY).equals("false")) {
            testCasesFound = false;
        }
        
        boolean clean = false;
        if (execution.getLifecyclePhase() != null && (execution.getLifecyclePhase().equals("clean") || execution.getLifecyclePhase().equals("pre-clean"))) {
            clean = true;
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
        
        if (!clean && (
                (hasExecutions && mojoExecution.getExecutionId().startsWith("default-")) || skipTests || installOnly || !testCasesFound)) {
            getLog().info("Stop and remove nodes is skipped (skipTests="+skipTests+",hasExecutions="+hasExecutions+",testCasesFound="+testCasesFound+",installOnly="+installOnly+",phase="+execution.getLifecyclePhase()+")");
            return;
        }

        boolean failOnError = failOnStopError;
        if (clean) {
            failOnError = false;
        }

        initializeAdministration(failOnError);

        // shut down
        //
        if (skipStop) {
            getLog().info("Stop nodes is skipped");
        } else {
            for (int i=0; i<nodes.length; i++) {
                String nodeName = nodes[i]+"."+clusterName;
                if (new File(nodeDirectory, nodeName).exists()) {
                    stopNodes(nodeName, userName, password, failOnError);
                }
            }
        }

        if (skipRemove) {
            getLog().info("Remove nodes is skipped");
            return;
        }

        // remove nodes
        //
        if (installPath != null) {
            removeNode(installPath, failOnError);
        } else {
            for (int i=0; i<nodes.length; i++) {
                String nodeName = nodes[i]+"."+clusterName;
                if (new File(nodeDirectory, nodeName).exists()) {
                    try {
                        removeNodes(nodeName, userName, password, failOnError);
                    } catch (MojoExecutionException e) {
                        removeNode(new File(nodeDirectory, nodeName).getAbsolutePath(), failOnError);
                    }
                }
            }
        }

    }
}
