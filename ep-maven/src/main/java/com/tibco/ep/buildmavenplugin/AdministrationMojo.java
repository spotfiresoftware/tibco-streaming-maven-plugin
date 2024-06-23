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

import java.util.Map;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * <p>Administer nodes</p>
 * <p>Example use in pom.xml:</p>
 * <img src="uml/administer-nodes.svg" alt="pom">
 * 
 */
@Mojo(name = "administer-nodes", threadSafe = true)
public class AdministrationMojo extends BaseExecuteMojo {

    /**
     * <p>Administration command to execute</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/administer-nodes-command.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter(required=true)
    String command;

    /**
     * <p>Administration target to execute</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/administer-nodes-target.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter(required=true)
    String target;

    /**
     * <p>Administration port - overrides servicename</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/administer-nodes-adminport.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter
    Integer adminport;

    /**
     * <p>Hostname - default is localhost.  Only applies when adminport is set</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/administer-nodes-hostname.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter
    String hostname;
    
    /**
     * <p>Administration command arguments</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/administer-nodes-arguments.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter
    Map<String, String> arguments;
    
    /**
     * <p>Servicename to select which nodes to run the administration command on.</p>
     * 
     * <p>If not set, test cases will be run on all the nodes in the cluster.</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/administer-nodes-serviceName.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter(defaultValue="${project.artifactId}")
    String serviceName;
    
    /**
     * <p>Skip executing the administration command</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/administer-nodes-skip.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter(defaultValue="false")
    boolean skip;
    
    public void execute() throws MojoExecutionException {
        getLog().debug("Administer nodes");

        prechecks();

        if (skip) {
            getLog().info("Administrator command is skipped");
            return;
        }
        
        initializeService(PlatformService.ADMINISTRATION, ErrorHandling.FAIL);

        AdminCommand adminCommand = null;
        if (adminport == null) {
            adminCommand = newCommand(serviceName);
        } else {
            adminCommand = newCommand(adminport).hostname(hostname);
        }

        adminCommand
            .commandAndTarget(command, target)
            .parameters(arguments)
            .run();
    }

}
