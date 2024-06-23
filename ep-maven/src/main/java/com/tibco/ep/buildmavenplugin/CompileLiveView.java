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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.filtering.MavenFilteringException;

/**
 * <p>
 * Compile LiveView.
 * </p>
 * 
 * <p>
 * Compilation includes copying the source files to the output directory
 * </p>
 */
@Mojo(name = "compile-liveview-fragment", defaultPhase = LifecyclePhase.COMPILE, threadSafe = true)
public class CompileLiveView extends BaseCompileMojo {

    /**
     * <p>
     * LiveView source directory.
     * </p>
     * 
     * <p>
     * Example use in pom.xml:
     * </p>
     * <img src="uml/liveviewDirectory.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter(defaultValue = "${project.basedir}/src/main/liveview", required = true, property = "liveviewDirectory")
    File liveviewDirectory;

    @Override
    public void execute() throws MojoExecutionException {
        getLog().debug("Compiling LiveView fragment " + liveviewDirectory);
        try {
            execute(new File[] { liveviewDirectory }, new File(project.getBuild().getOutputDirectory()), "**/*.lvconf");
        } catch (MavenFilteringException e) {
            throw new MojoExecutionException("Failed to copy LiveView conf files", e);
        }
    }

}
