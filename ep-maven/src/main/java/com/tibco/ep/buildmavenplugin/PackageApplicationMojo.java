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

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.FileItem;
import org.apache.maven.plugins.assembly.model.FileSet;

/**
 * <p>Build an application zip archive.</p>
 *
 * <p>The packaging rules are as follows :-</p>
 * <ol>
 * <li>A mainifest file is created at /META-INF/MANIFEST.MF containing :-
 *      <ul>
 *              <li>Archiver-Version: Plexus Archiver</li>
 *              <li>Built-By: build user</li>
 *              <li>Build-Jdk: jdk version</li>
 *              <li>Package-Title: project groupId and name</li>
 *              <li>Package-Version: project version and buildNumber</li>
 *              <li>Package-Vendor: project organization name (if set)</li>
 *              <li>TIBCO-EP-Application-Format-Version: 1</li>
 *      </ul>
 * <li>Discovered application definition file is copied into /META-INF/application.conf</li>
 * <li>The project's resourcedirectory/artifactId/ files are copied to /app-config/fragmentname/<li>
 * <li>The project's resourcedirectory/groupId-artifact/ files are copied to /app-config/fragmentname/<li>
 * <li>The project's resourcedirectory/groupId-artifact-version/ files are copied to /app-config/fragmentname/<li>
 * <li>Remaining resource files are copied into /</li>
 * <li>Dependent fragments are copied into /</li>
 * </ol>
 * 
 * <p>The plexus archiver is used to create the archive via the maven assembly
 * plugin.</p>
 * 
 * <p>The generated filename is &lt;artifactId&gt;-&lt;version&gt;-ep-application.zip</p>
 */
@Mojo(name = "package-application", defaultPhase = PACKAGE)
public class PackageApplicationMojo extends BasePackageMojo {

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
     * @since 1.0.0
     */
    @Parameter( defaultValue = "${project.basedir}/src/test/configurations", required = true )
    File testConfigurationDirectory;

    public void execute() throws MojoExecutionException {
        getLog().debug( "Creating application zip" );

        prechecks();

        // updated format which is really jar
        //

        Assembly assembly = createAssembly();

        Set<Artifact> artifacts = getCompileProjectDependencies();

        // check we have at least on fragment
        //
        long fragmentCount = 0;
        for (Artifact a : artifacts) {
            String type = a.getType();
            if (type != null && (type.equals(JAVA_TYPE) || type.equals(EVENTFLOW_TYPE) || type.equals(LIVEVIEW_TYPE))) {
                fragmentCount++;
            }
        }
        if (fragmentCount == 0) {
            throw new MojoExecutionException("Application archive must contain at least one fragment");
        }
        
        // add runtime dependencies
        //
        // we add them one-by-one instead of using a dependencyset so we can
        // set the classpath in the manifest file
        //
        String classPath = "";
        String nativeClassPath = "";
        String fragmentList = "";

        List<FileItem> deps = new ArrayList<FileItem>();

        for (Artifact dependency : artifacts) {
            String type = dependency.getType();

            String destName = dependency.getGroupId()+"-"+dependency.getArtifactId()+"-"+dependency.getBaseVersion()+"."+dependency.getType();
            if (type != null && (type.equals(JAVA_TYPE) || type.equals(EVENTFLOW_TYPE) || type.equals(LIVEVIEW_TYPE))) {
                destName = dependency.getGroupId()+"-"+dependency.getArtifactId()+"-"+dependency.getBaseVersion()+"-"+dependency.getType()+".zip";
            }
            
            String artifactPath = getArtifactPath(dependency);
            if (! new File(artifactPath).exists()) {
                // normally maven dependency checks catch this, but m2e seems to bypass it
                //
                throw new MojoExecutionException("Artifact "+destName+" wasn't found at "+artifactPath);
            }

            FileItem dependencyFile = new FileItem();
            dependencyFile.setOutputDirectory("");
            dependencyFile.setSource(getArtifactPath(dependency));
            dependencyFile.setDestName(destName);
            deps.add(dependencyFile);

            if (dependency.getType().equals("nar")) {
                nativeClassPath += destName+" ";
            } else if (type != null && (type.equals(JAVA_TYPE) || type.equals(EVENTFLOW_TYPE) || type.equals(LIVEVIEW_TYPE))) {
                fragmentList += destName+" ";
            } else {
                classPath += destName+" ";
            }
        }

        // build a manifest file and add as first file to the zip
        //
        Map<String, String> extras = new HashMap<String, String>();
        extras.put("TIBCO-EP-Fragment-Format-Version", "2");
        if (!classPath.isEmpty()) {
            extras.put("Class-Path", classPath);
        }
        if (!nativeClassPath.isEmpty()) {
            extras.put("TIBCO-EP-Native-Class-Path", nativeClassPath);
        }
        if (!fragmentList.isEmpty()) {
            extras.put("TIBCO-EP-Fragment-List", fragmentList);
        }
        File manifest = packageManifest(assembly, null, extras);

        for (FileItem dependencyFile : deps) {
            assembly.addFile(dependencyFile);
        }

        // add output directory
        //
        // this should include resources ( already copied here ) and
        // eventflow files
        //
        if (!new File(project.getBuild().getOutputDirectory()).exists()) {
            new File(project.getBuild().getOutputDirectory()).mkdirs();
        }

        FileSet fileSet = new FileSet();
        fileSet.setDirectory(project.getBuild().getOutputDirectory());
        fileSet.setOutputDirectory("");
        assembly.addFileSet(fileSet);

        // write assembly
        //
        writeAssembly(assembly);
        manifest.delete();
    }

}
