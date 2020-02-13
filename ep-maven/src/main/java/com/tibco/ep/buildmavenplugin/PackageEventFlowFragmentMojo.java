/*******************************************************************************
 * Copyright (C) 2018 - 2019, TIBCO Software Inc.
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

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.FileItem;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.manager.ArchiverManager;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Build a EventFlow fragment zip archive.</p>
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
 *              <li>Product-Version: product version</li>
 *      </ul>
 * <li>pom.xml copied to /META-INF/maven/groupId/artifactId/pom.xml</li>
 * <li>pom.properties created in /META-INF/maven/groupId/artifactId/pom.properties</li>
 * <li>The project's sbapp/ssql files are copied into /modules, maintaining any sub directories</li>
 * <li>The project's java classes and jar dependencies are copied into /java-resources</li>
 * <li>The project's resources files are copied into /</li>
 * <li>Application files are compiled if possible</li>
 * </ol>
 * 
 * <p>The plexus archiver is used to create the archive via the maven assembly
 * plugin.</p>
 * 
 * <p>Native archives (nar) are included - in this case a mapping is included
 * from the nar AOL values (http://maven-nar.github.io/aol.html) to internal
 * values.</p>
 * 
 * <p>The generated filename is &lt;artifactId&gt;-&lt;version&gt;-ep-eventflow-fragment.zip</p>
 */
@Mojo(name = "package-eventflow-fragment", defaultPhase = PACKAGE, threadSafe = true)
public class PackageEventFlowFragmentMojo extends BasePackageMojo {

    /**
     * Archive manager
     */
    @Component
    private ArchiverManager archiverManager;

    // maven user parameters
    //

    /**
     * <p>Eventflow source directories</p>
     * 
     * <p>If no eventflowDirectories is specified, a single directory of
     * ${project.basedir}/src/main/eventflow is used.</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/eventflowDirectories.svg" alt="pom">
     * 
     * <p>Example use on commandline:</p>
     * <img src="uml/eventflowDirectories-commandline.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter( required = false, property = "eventflowDirectories" )
    File[] eventflowDirectories;

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

    public void execute() throws MojoExecutionException {
        getLog().debug( "Creating eventflow fragment" );

        if (eventflowDirectories == null || eventflowDirectories.length == 0) {
            eventflowDirectories = new File[] { new File(project.getBasedir(), "/src/main/eventflow") };
        }

        prechecks();

        // updated format which is really jar
        //

        Assembly assembly = createAssembly();

        // add runtime dependencies
        //
        // we add them one-by-one instead of using a dependencyset so we can
        // set the classpath in the manifest file
        //
        String classPath = "";
        String nativeClassPath = "";
        String fragmentList = "";

        List<FileItem> deps = new ArrayList<FileItem>();
        for (Artifact dependency : getCompileProjectDependencies()) {
            String type = dependency.getType();
            String destName = dependency.getGroupId()+"-"+dependency.getArtifactId()+"-"+dependency.getBaseVersion();

            if (type != null && (type.equals(JAVA_TYPE) || type.equals(EVENTFLOW_TYPE) || type.equals(TCS_TYPE) || type.equals(LIVEVIEW_TYPE))) {
                destName += "-"+type+".zip";
            } else {
                if (dependency.hasClassifier()) {
                    destName += "-"+dependency.getClassifier();
                }
                destName += "."+dependency.getType();
            }

            FileItem dependencyFile = new FileItem();
            dependencyFile.setOutputDirectory("");
            dependencyFile.setSource(getArtifactPath(dependency));
            dependencyFile.setDestName(destName);
            deps.add(dependencyFile);

            if (dependency.getType().equals("nar")) {
                nativeClassPath += destName+" ";
            } else if (type != null && (type.equals(JAVA_TYPE) || type.equals(EVENTFLOW_TYPE) || type.equals(TCS_TYPE) || type.equals(LIVEVIEW_TYPE))) {
                fragmentList += destName+" ";
            } else {
                classPath += destName+" ";
            }
        }

        // add files in our source directories
        //
        for (File eventflowDirectory : eventflowDirectories) {
            FileSet fileSet = new FileSet();
            fileSet.setDirectory(eventflowDirectory.getAbsolutePath());
            fileSet.setOutputDirectory("");
            assembly.addFileSet(fileSet);
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
        FileSet fileSet = new FileSet();
        fileSet.setDirectory(project.getBuild().getOutputDirectory());
        fileSet.setOutputDirectory("");
        assembly.addFileSet(fileSet);

        // write assembly
        //
        writeAssembly(assembly);
        if (!manifest.delete()) {
            getLog().debug("Unable to delete "+manifest.getAbsolutePath());
        }

    }

}
