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
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.FileAttribute;

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugins.assembly.mojos.AbstractAssemblyMojo;
import org.apache.maven.project.MavenProject;

/**
 * Configuration source for assembly
 *
 */
public class ConfigurationSource extends AbstractAssemblyMojo {

    private final MavenProject project;
    private final ArtifactRepository localRepository;
    
    private final File outputDirectory;
    private final File siteDirectory;
    private File temporaryDirectory;
    private File workingDirectory;
    private final String finalName;
    private final MavenSession session;

    /**
     * Constructor
     * 
     * @param project project
     * @param localRepository local repository
     * @param session session
     */
    ConfigurationSource(MavenProject project, ArtifactRepository localRepository, MavenSession session) {
        this.project = project;
        this.localRepository = localRepository;
        this.outputDirectory = new File(project.getBuild().getDirectory());
        this.siteDirectory = new File(project.getModel().getReporting().getOutputDirectory());
        
        File buildDirectory = new File(project.getBuild().getDirectory());
        if (!buildDirectory.exists()) {
            if (!buildDirectory.mkdirs()) {
                getLog().warn("Unable to create directory "+buildDirectory.getAbsolutePath());
            }
        }
        try {
            this.temporaryDirectory = Files.createTempDirectory(buildDirectory.toPath(), "assemtmp", new FileAttribute[] {}).toFile();
            this.workingDirectory = Files.createTempDirectory(buildDirectory.toPath(), "assemwork", new FileAttribute[] {}).toFile();
        } catch (IOException e) {
            // should never happen
            e.printStackTrace();
        }
        
        this.finalName = project.getBuild().getFinalName();
        this.session = session;
    }

    @Override
    public File getBasedir() {
        return project.getBasedir();
    }

    @Override
    public String getFinalName() {
        return finalName;
    }

    @Override
    public File getOutputDirectory() {
        return outputDirectory;
    }

    @Override
    public MavenProject getProject() {
        return project;
    }

    @Override
    public File getSiteDirectory() {
        return siteDirectory;
    }

    @Override
    public File getTemporaryRootDirectory() {
        return temporaryDirectory;
    }

    @Override
    public File getWorkingDirectory() {
        return workingDirectory;
    }

    @Override
    public MavenSession getMavenSession() {
        return session;
    }
    
    
}
