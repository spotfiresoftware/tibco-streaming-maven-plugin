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

import static org.apache.maven.plugins.annotations.LifecyclePhase.PROCESS_RESOURCES;

import java.io.File;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.codehaus.plexus.archiver.ArchiverException;
import org.codehaus.plexus.archiver.UnArchiver;
import org.codehaus.plexus.archiver.manager.ArchiverManager;
import org.codehaus.plexus.archiver.manager.NoSuchArchiverException;

/**
 * <p>Unpack any nar (native archive) archives listed in dependences.</p>
 * 
 * <p>Nar archives are extracted into the build nar directory and are included
 * on the java library path when running unit tests and native libraries are
 * included in the fragment.</p>
 */
@Mojo(name = "unpack-nar", defaultPhase = PROCESS_RESOURCES, threadSafe = true)
public class UnpackNarMojo extends BaseMojo {
    
    /**
     * Archive manager
     */
    @Component
    private ArchiverManager archiverManager;
    
    public void execute() throws MojoExecutionException {
        getLog().debug( "Unpack nar" );
        
        prechecks();
        
        for (Artifact artifact : getProjectDependencies()) {
            if (artifact.getType() != null && artifact.getType().equals("nar")) {
                try {
                    File destDir = new File(project.getBuild().getDirectory()+File.separator+"nar");
                    if (!destDir.exists() && !destDir.mkdirs()) {
                        throw new MojoExecutionException("Unable to create directory "+destDir.getAbsolutePath());
                    }

                    File source = new File(getArtifactPath(artifact));
                    UnArchiver unArchiver = archiverManager.getUnArchiver( source );
                    unArchiver.setSourceFile( source );
                    unArchiver.setDestDirectory( destDir );
                    unArchiver.extract();

                } catch (ArchiverException e) {
                    throw new MojoExecutionException("Unable to unpack nar: " + e.getMessage(), e);
                } catch (NoSuchArchiverException e) {
                    throw new MojoExecutionException("Unable to unpack nar: " + e.getMessage(), e);
                }
            }
        }
    }

}
