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

import java.io.File;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.filtering.MavenFilteringException;

/**
 * <p>
 * Compile EventFlow modules.
 * </p>
 * 
 * <p>
 * Compilation includes copying the source files to the output directory.
 * </p>
 */
@Mojo(name = "compile-eventflow-fragment", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class CompileEventFlow extends BaseCompileMojo {

    /**
     * <p>
     * EventFlow source directories
     * </p>
     * 
     * <p>
     * If no eventflowDirectories is specified, a single directory of ${project.basedir}/src/main/eventflow is used.
     * </p>
     * 
     * <p>
     * Example use in pom.xml:
     * </p>
     * <img src="uml/eventflowDirectories.svg" alt="pom">
     * 
     * <p>
     * Example use on commandline:
     * </p>
     * <img src="uml/eventflowDirectories-commandline.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter(required = false, property = "eventflowDirectories", defaultValue = "${project.basedir}/src/main/eventflow")
    File[] eventflowDirectories;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().debug("Compiling EventFlow fragment");
        final File outputDirectory = new File(project.getBuild().getOutputDirectory());
        try {
            execute(eventflowDirectories, outputDirectory, "**/*.sbapp", "**/*.sbint", "**/*.ssql");
        } catch (MavenFilteringException e) {
            throw new MojoExecutionException("Failed to copy EventFlow modules", e);
        }
    }

}
