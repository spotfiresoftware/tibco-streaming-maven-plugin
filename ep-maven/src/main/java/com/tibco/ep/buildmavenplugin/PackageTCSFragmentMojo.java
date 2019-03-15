/*******************************************************************************
 *
 * COPYRIGHT
 *      Copyright 2019 TIBCO Software Inc. ALL RIGHTS RESERVED.
 *      TIBCO Software Inc. Confidential Information
 *      
 *******************************************************************************/
package com.tibco.ep.buildmavenplugin;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.FileItem;
import org.apache.maven.plugins.assembly.model.FileSet;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * <p>Build a TCS fragment</p>
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
 *      </ul>
 * <li>The project's files are copied into /</li>
 * <li>The project's java classes and jar dependencies are copied into /java-resources</li>
 * <li>The project's resources files are copied into /</li>
 * </ol>
 * 
 * <p>The plexus archiver is used to create the archive via the maven assembly
 * plugin.</p>
 * 
 * <p>Native archives (nar) are included - in this case a mapping is included
 * from the nar AOL values (http://maven-nar.github.io/aol.html) to internal
 * values.</p>
 * 
 * <p>The generated filename is &lt;artifactId&gt;-&lt;version&gt;-ep-tcs-fragment.zip</p>
 * 
 */
@Mojo(name = "package-tcs-fragment", defaultPhase = PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME)
public class PackageTCSFragmentMojo extends BasePackageMojo {

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
        getLog().debug( "Creating TCS fragment" );

        prechecks();

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
        manifest.delete();
    }
}
