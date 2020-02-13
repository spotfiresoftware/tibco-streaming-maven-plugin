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

import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.assembly.archive.AssemblyArchiver;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.FileItem;

/**
 * Package base 
 *
 */
abstract class BasePackageMojo extends BaseMojo {

    // maven components
    //

    /**
     * Assembly archiver
     */
    @Component
    AssemblyArchiver assemblyArchiver;

    // maven read-only parameters
    //

    // maven user parameters
    //

    /**
     * <p>List of artifact names to exclude from the zip archive.</p>  
     * 
     * <p>Format of each artifact is groupId:artifactId:type[:classifier]:version.  Wildcards are
     * supported.</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/artifactExcludes.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter
    String[] artifactExcludes;
    
    /**
     * Create a manifest, pom.xml and pom.properties files and add to assembly
     * 
     * @param assembly assembly to add manifest
     * @param mainclass man java class, null if none
     * @param extras map of any additional manifest entries
     * @return manifest file
     * @throws MojoExecutionException on error
     */
    File packageManifest(Assembly assembly, String mainclass, Map<String, String> extras) throws MojoExecutionException {
        
        // make sure the build directory is created
        //
        File dir = new File(project.getBuild().getDirectory());
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                throw new MojoExecutionException("Unable to create: " + dir.getAbsolutePath());
            }
        }
        
        FileOutputStream os = null;
        FileOutputStream pos = null;
        
        try {

            File tempFile = File.createTempFile("MAN", "MF", new File(project.getBuild().getDirectory()));
          
            String tempManifestPath = tempFile.getAbsolutePath();
            Manifest manifest = new Manifest();
            Attributes atts = manifest.getMainAttributes();
            atts.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            atts.put(new Attributes.Name("Archiver-Version"), "Plexus Archiver");
            atts.put(new Attributes.Name("Built-By"), System.getProperty("user.name"));
            atts.put(new Attributes.Name("Build-Jdk"), System.getProperty("java.version"));
            atts.put(new Attributes.Name("Package-Title"), project.getName());

            String buildNumber = (String) project.getProperties().get("buildNumber");
            if (buildNumber != null) {
                atts.put(new Attributes.Name("Package-Version"), project.getVersion()+" "+buildNumber);
            } else {
                atts.put(new Attributes.Name("Package-Version"), project.getVersion());
            }

            if (project.getOrganization() != null) {
                atts.put(new Attributes.Name("Package-Vendor"), project.getOrganization().getName());
            }

            if (mainclass != null && mainclass.length() > 0) {
                atts.put(Attributes.Name.MAIN_CLASS, mainclass);
            }

            atts.put(new Attributes.Name("TIBCO-EP-Fragment-Type"), project.getPackaging());
            atts.put(new Attributes.Name("TIBCO-EP-Fragment-Identifier"), project.getGroupId()+"."+project.getArtifactId());

            String productVersion = getProductVersion();
            if (productVersion != null && productVersion.length() > 0) {
                atts.put(new Attributes.Name("Product-Version"), productVersion);
            }
            
            if (extras != null) {
                for (Map.Entry<String, String> entry : extras.entrySet()) {
                    atts.put(new Attributes.Name(entry.getKey()), entry.getValue());
                }
            }
            os = new FileOutputStream(new File(tempManifestPath));
            manifest.write(os);
            FileItem manifestfile = new FileItem();
            manifestfile.setSource(tempManifestPath);
            manifestfile.setDestName("META-INF/MANIFEST.MF");
            assembly.addFile(manifestfile);

            // Add in pom.xml ( in the same way as maven archiver )
            //
            FileItem pom = new FileItem();
            pom.setSource(project.getFile().getAbsolutePath());
            pom.setDestName("META-INF/maven/" + project.getGroupId() + "/" + project.getArtifactId() + "/pom.xml");
            assembly.addFile(pom);

            // Add in pom.properties ( in the same was as maven archiver )
            //
            File tempPropertiesFile = File.createTempFile("pom", "properties", new File(project.getBuild().getDirectory()));
            tempPropertiesFile.deleteOnExit();
            String tempPropertiesPath = tempPropertiesFile.getAbsolutePath();
            pos = new FileOutputStream(new File(tempPropertiesPath));
            OutputStreamWriter osw = new OutputStreamWriter(pos);
            osw.write("# Created by TIBCO Streaming Maven Plugin\n");
            osw.write("version="+project.getVersion()+"\n");
            osw.write("groupId="+project.getGroupId()+"\n");
            osw.write("artifactId="+project.getArtifactId()+"\n");
            if (productVersion != null && productVersion.length() > 0) {
                osw.write("productVersion="+productVersion+"\n");
            }
            osw.close();
            FileItem properties = new FileItem();
            properties.setSource(tempPropertiesPath);
            properties.setDestName("META-INF/maven/" + project.getGroupId() + "/" + project.getArtifactId() + "/pom.properties");
            assembly.addFile(properties);
            
            return tempFile;
        } catch (FileNotFoundException e) {
            throw new MojoExecutionException("Failed to create manifest: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new MojoExecutionException("Failed to create manifest: " + e.getMessage(), e);
        } finally {
            if (os != null) {
                try {
                    os.close();
                } catch (IOException e) {
                }
            }
            if (pos != null) {
                try {
                    pos.close();
                } catch (IOException e) {
                }
            }
        }
    }

    /**
     * Create an assembly
     * 
     * @return assembly
     */
    Assembly createAssembly() {
        Assembly assembly = new Assembly();
        assembly.setId(project.getPackaging());
        assembly.setIncludeBaseDirectory(false);
        assembly.setBaseDirectory(project.getBasedir().getAbsolutePath());
        return assembly;
    }

    /**
     * Write an assembly file and attach to build
     * 
     * @param assembly assembly
     * @throws MojoExecutionException on error
     */
    void writeAssembly(Assembly assembly) throws MojoExecutionException {
        File assemblyFile;

        ConfigurationSource configSource = new ConfigurationSource(project, localRepository, session);
        try {
            assemblyFile = assemblyArchiver.createArchive(assembly, project.getArtifactId()+"-"+project.getVersion()+"-"+project.getPackaging(), "zip", configSource, false, "merge");
        } catch (Exception e) {
            throw new MojoExecutionException("Failed to create plugin bundle: " + e.getMessage(), e.getCause());
        } finally {
            // clean temp directories
            //
            Stream<Path> s = null;
            try {
                s = Files.walk(configSource.getWorkingDirectory().toPath());
                s.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException e) {
            } finally {
                if (s != null) {
                    s.close();
                }
            }
            s = null;
            try {
                s = Files.walk(configSource.getTemporaryRootDirectory().toPath());
                s.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException e) {
            } finally {
                if (s != null) {
                    s.close();
                }
            }
        }
        

        // Attach bundle assembly to the project
        project.getArtifact().setFile( assemblyFile );

        // Notify m2e
        //
        buildContext.refresh(assemblyFile);
    }
}
