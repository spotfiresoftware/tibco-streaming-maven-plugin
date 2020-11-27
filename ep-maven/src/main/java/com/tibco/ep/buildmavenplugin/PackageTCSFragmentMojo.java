/*******************************************************************************
 *
 * COPYRIGHT
 *      Copyright 2019 TIBCO Software Inc. ALL RIGHTS RESERVED.
 *      TIBCO Software Inc. Confidential Information
 *
 *******************************************************************************/
package com.tibco.ep.buildmavenplugin;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

import static org.apache.maven.plugins.annotations.LifecyclePhase.PACKAGE;

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
 *              <li>TIBCO-EP-Build-Product-Version: product version</li>
 *      </ul>
 * <li>pom.xml copied to /META-INF/maven/groupId/artifactId/pom.xml</li>
 * <li>pom.properties created in /META-INF/maven/groupId/artifactId/pom.properties</li>
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
 */
@Mojo(name = "package-tcs-fragment", defaultPhase = PACKAGE, requiresDependencyResolution = ResolutionScope.RUNTIME, threadSafe = true)
public class PackageTCSFragmentMojo extends BasePackageMojo {

    @Override
    public void execute() throws MojoExecutionException {
        getLog().debug("Creating TCS fragment");

        prechecks();

        newArchiveGenerator().generate();
    }
}
