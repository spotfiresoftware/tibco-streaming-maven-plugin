/*******************************************************************************
 * Copyright (C) 2018-2024 Cloud Software Group, Inc.
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.model.PluginExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * <p>
 * Install and start nodes.
 * </p>
 * <p>
 * Nodes are installed one at a time but started in parallel.
 * </p>
 * <p>
 * If a node directory already exists, stop and remove nodes is called first.
 * If the directory still exists, it is deleted.
 * </p>
 * <p>Example use in pom.xml:</p>
 * <img src="uml/start-nodes.svg" alt="pom">
 */
@Mojo(name = "start-nodes", threadSafe = true)
public class StartNodesMojo extends BaseExecuteMojo {

    /**
     * <p>Node deployment HOCON file.  Applicable for packaging type ep-application
     * only</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/start-nodes-nodeDeployFile.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter
    File nodeDeployFile;

    /**
     * <p>Additional parameters supported by install node, to support command-line</p>
     * 
     * @since 1.0.0
     */
    @Parameter( property = "installArguments", readonly=true)
    String[] nodeParameters;

    /**
     * <p>Additional parameters supported by install node.</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/start-nodes-installArguments.svg" alt="pom">
     * 
     * <p>Example use on commandline:</p>
     * <img src="uml/start-nodes-installArguments-commandline.svg" alt="pom">
     * 
     * <p><b>User property is:</b> <tt>installArguments</tt>.</p>
     * 
     * @since 1.0.0
     */
    @Parameter
    Map<String, String> installArguments;

    /**
     * <p>Additional parameters supported by start node.</p>
     * 
     * <p>Example use on commandline:</p>
     * <img src="uml/start-nodes-startArguments.svg" alt="pom">
     * 
     * @since 1.3.0
     */
    @Parameter
    Map<String, String> startArguments;
    
    /**
     * <p>Set this to 'true' to skip installing test nodes</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/start-nodes-skipInstall.svg" alt="pom">
     * 
     * <p>Example use on commandline:</p>
     * <img src="uml/start-nodes-skipInstall-commandline.svg" alt="pom">
     * 
     * @since 1.1.0
     */
    @Parameter(property = "skipInstall")
    boolean skipInstall;

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
     * <p>Set this to 'true' to skip starting test nodes</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/start-nodes-skipStart.svg" alt="pom">
     * 
     * <p>Example use on commandline:</p>
     * <img src="uml/start-nodes-skipStart-commandline.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter(property = "skipStart")
    boolean skipStart;

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

    public void execute() throws MojoExecutionException {
        getLog().debug("Start nodes");

        prechecks();

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
        
        Properties modelProperties = project.getModel().getProperties();
        boolean testCasesFound = true;
        if (modelProperties.getProperty(TESTCASESFOUND_PROPERTY) != null && modelProperties.getProperty(TESTCASESFOUND_PROPERTY).equals("false")) {
            testCasesFound = false;
        }
        
        if ((hasExecutions && mojoExecution.getExecutionId().startsWith("default-")) || skipTests || !testCasesFound) {
            getLog().info("Start and install nodes is skipped (skipTests="+skipTests+",hasExecutions="+hasExecutions+",testCasesFound="+testCasesFound+")");
            return;
        }

        initializeService(PlatformService.ADMINISTRATION, ErrorHandling.FAIL);

        boolean staticDiscovery = false;
        Map<String, Integer> adminPorts = new HashMap<>();

        if (skipInstall || skipStart) {
            getLog().info("Install nodes is skipped");
        } else {

            Map<String, String> defaultArguments =
                (installArguments != null ? new HashMap<>(installArguments) : new HashMap<>());

            if (nodeParameters != null) {
                for(String argument : nodeParameters) {
                    if (argument.contains("=")) {
                        String[] args = argument.split("=");
                        if (args.length == 2) {
                            defaultArguments.put(args[0], args[1]);
                        } else {
                            getLog().warn("Invalid name/value "+argument);
                        }
                    } else {
                        getLog().warn("Invalid name/value "+argument);
                    }
                }
            }

            // for application archives, add in node deploy file and path to zip
            //
            if (project.getArtifact().getType().equals(APPLICATION_TYPE) && project.getArtifact().getFile() != null) {
                defaultArguments.put("application", project.getArtifact().getFile().getAbsolutePath());
                if (nodeDeployFile != null) {
                    defaultArguments.put("nodedeploy", nodeDeployFile.getAbsolutePath());
                }
            }

            getLog().debug("Execution options = "+defaultArguments.toString());

            // start test nodes
            // 
            // filter the final results to see if we are using static discovery on
            // the cluster
            //
            for (String node : nodes) {
                String results = installNode(
                    nodeDirectory.getAbsolutePath(),
                    node + "." + clusterName,
                    defaultArguments);
                if (results.contains("Discovery Service: Disabled: disabled via install option")) {
                    staticDiscovery = true;
                    Matcher m = Pattern
                        .compile(".*Administration port is (\\d+)$", Pattern.MULTILINE)
                        .matcher(results);
                    if (m.find()) {
                        adminPorts.put(node, Integer.parseInt(m.group(1)));
                    }
                }
            }
        }

        if (!installOnly && !skipStart) {
            if (!staticDiscovery) {

                getLog().info("Static discovery disabled.");
                newCommand(clusterName)
                    .commandAndTarget("start", "node")
                    .parameters(startArguments)
                    .run();

            } else {

                getLog().info("Static discovery enabled. Node list: " + Arrays.toString(nodes));

                AtomicReference<MojoExecutionException> caughtException = new AtomicReference<>();

                Thread[] threads = new Thread[nodes.length];
                for (int i=0; i<nodes.length; i++) {

                    // run start nodes in the background
                    //
                    final int adminPort = adminPorts.get(nodes[i]);
                    int finalI = i;
                    threads[i] = new Thread(() -> {

                        try {

                            newCommand(adminPort)
                                .commandAndTarget("start", "node")
                                .parameters(startArguments)
                                .run();

                        } catch (MojoExecutionException e) {
                            //  Log the exception, keep it.
                            //
                            getLog().warn("Start node on " + nodes[finalI] + " failed", e);
                            caughtException.compareAndSet(null, e);
                        }
                    });
                    threads[i].start(); 
                }

                // wait for the threads to finish.
                //
                for (int i=0; i<nodes.length; i++) {
                    try {
                        threads[i].join();
                    } catch (InterruptedException e) {
                        // ignore
                    }
                }

                MojoExecutionException anException = caughtException.get();
                if (anException != null) {
                    throw new MojoExecutionException("Could not start all nodes", anException);
                }
            }
        } else {
            getLog().info("Start nodes is skipped");
        }
    }
}
