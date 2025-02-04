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

import org.apache.maven.model.Resource;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

import java.io.File;

/**
 * <p>Set resources</p>
 *
 */
@Mojo(name = "set-resources", defaultPhase = PACKAGE, threadSafe = true)
public class SetResources extends BasePackageMojo {

    // maven user parameters
    //

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
     * <p>Example use on commandline:</p>
     * <img src="uml/eventflowDirectories-commandline.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter( defaultValue = "${project.basedir}/src/test/configurations", required = true )
    File testConfigurationDirectory;

    /**
     * <p>Eventflow directories</p>
     * 
     * <p>This is added to the list of resource directories</p>
     * 
     * <p>Example use on commandline:</p>
     * <img src="uml/eventflowDirectories-commandline.svg" alt="pom">
     * 
     * @since 1.1.0
     */
    @Parameter( required = false, property = "eventflowDirectories", defaultValue = "${project.basedir}/src/main/eventflow" )
    File[] eventflowDirectories;

    /**
     * <p>Liveview source directory</p>
     * 
     * <p>This is added to the list of resource directories</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/liveviewDirectory.svg" alt="pom">
     * 
     * <p>Example use on commandline:</p>
     * <img src="uml/liveviewDirectory-commandline.svg" alt="pom">
     * 
     * @since 1.1.0
     */
    @Parameter( defaultValue = "${project.basedir}/src/main/liveview", required = true, property = "liveviewDirectory" )
    File liveviewDirectory;

    public void execute() throws MojoExecutionException {
        getLog().debug( "Set resources" );

        prechecks();

        if (configurationDirectory.exists()) {
            Resource resource = new Resource();
            resource.setDirectory(configurationDirectory.getAbsolutePath());
            //resource.setTargetPath(project.getBasedir().toPath().relativize(configurationDirectory.toPath()).toString());
            project.getBuild().addResource(resource);
        }
        
        if (testConfigurationDirectory.exists()) {
            Resource resource = new Resource();
            resource.setDirectory(testConfigurationDirectory.getAbsolutePath());
            //resource.setTargetPath(project.getBasedir().toPath().relativize(testConfigurationDirectory.toPath()).toString());
            project.getBuild().addTestResource(resource);
        }
        
        for (File ef : eventflowDirectories) {
            if (ef.exists()) {
                Resource resource = new Resource();
                resource.setDirectory(ef.getAbsolutePath());
                //resource.setTargetPath(project.getBasedir().toPath().relativize(ef.toPath()).toString());
                project.getBuild().addResource(resource);
            }
        }

        if (liveviewDirectory.exists()) {
            Resource resource = new Resource();
            resource.setDirectory(liveviewDirectory.getAbsolutePath());
            //resource.setTargetPath(project.getBasedir().toPath().relativize(liveviewDirectory.toPath()).toString());
            project.getBuild().addResource(resource);
        }
        
        if (this.ignoreLeaks != null && this.ignoreLeaks.length >0) {
            String ignoreLeaks = String.join(",", this.ignoreLeaks);
            project.getProperties().setProperty("ignoreLeaks", ignoreLeaks);
            getLog().debug("ignoreLeaks="+ignoreLeaks);
        }
        
        
    }

}
