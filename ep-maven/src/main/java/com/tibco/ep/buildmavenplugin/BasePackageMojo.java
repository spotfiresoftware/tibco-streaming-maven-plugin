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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import java.util.stream.Stream;

import org.apache.maven.archiver.ManifestSection;
import org.apache.maven.archiver.MavenArchiveConfiguration;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.assembly.archive.AssemblyArchiver;
import org.apache.maven.plugins.assembly.model.Assembly;
import org.apache.maven.plugins.assembly.model.FileItem;
import org.apache.maven.plugins.assembly.model.FileSet;

/**
 * Package base
 */
abstract class BasePackageMojo extends BaseMojo {

    /**
     * Assembly archiver
     */
    @Component
    AssemblyArchiver assemblyArchiver;

    /**
     * The archive configuration to use. Any headers or entries here are in addition to the ones already placed by ep-maven-plugin. See <a href=https://maven.apache.org/shared/maven-archiver/index.html>Maven Archiver Reference</a>.
     * 
     * @since 2.3.0
     */
    @Parameter(required = false)
    MavenArchiveConfiguration archive = new MavenArchiveConfiguration();

    /**
     * @param groupId    The group Id
     * @param artifactId The artifact Id
     * @return The POM path in the archive
     */
    static String getPOMPathInArchive(String groupId, String artifactId) {
        return "META-INF/maven/" + groupId + "/" + artifactId + "/pom.xml";
    }

    /**
     * @return A new archive generator
     */
    ArchiveGenerator newArchiveGenerator() {
        return new ArchiveGenerator();
    }


    /**
     * The archive generator class
     */
    class ArchiveGenerator {
        private final Assembly assembly;
        private final String productVersion;
        private final List<Consumer<Assembly>> steps = new ArrayList<>();
        private final Map<String, String> additionalManifestEntries = new HashMap<>();
        private File temporaryManifestFile;
        private String mainClass;

        private ArchiveGenerator() {
            assembly = new Assembly();
            assembly.setId(project.getPackaging());
            assembly.setIncludeBaseDirectory(false);
            assembly.setBaseDirectory(project.getBasedir().getAbsolutePath());

            productVersion = getProductVersion();
        }

        /**
         * @param step The step to add in the assembly construction
         * @return This
         */
        ArchiveGenerator withAssemblyStep(Consumer<Assembly> step) {
            this.steps.add(step);
            return this;
        }

        /**
         * @param name The name of the additional MANIFEST entry
         * @param value The value of the additional MANIFEST entry
         * @return This
         */
        ArchiveGenerator withManifestEntry(String name, String value) {
            additionalManifestEntries.put(name, value);
            return this;
        }

        /**
         * @param mainClass The main class
         * @return This
         */
        public ArchiveGenerator withMainClass(String mainClass) {
            this.mainClass = mainClass;
            return this;
        }

        /**
         * Write the archive
         *
         * @throws MojoExecutionException Something went wrong
         */
        public void generate() throws MojoExecutionException {

            Set<Artifact> artifacts = getCompileProjectDependencies();

            // add runtime dependencies
            //
            // we add them one-by-one instead of using a dependencyset so we can
            // set the classpath in the manifest file
            //
            List<String> classPathItems = new ArrayList<>();
            List<String> nativeClassPathItems = new ArrayList<>();
            List<String> fragmentList = new ArrayList<>();

            List<FileItem> deps = new ArrayList<>();

            for (Artifact dependency : artifacts) {
                String type = dependency.getType();

                String destName = dependency.getGroupId()
                    + "-" + dependency.getArtifactId()
                    + "-" + dependency.getBaseVersion();

                if (isFragment(dependency)) {
                    destName += "-" + type + ".zip";
                } else {
                    if (dependency.hasClassifier()) {
                        destName += "-" + dependency.getClassifier();
                    }
                    destName += "." + dependency.getType();
                }

                String artifactPath = getArtifactPath(dependency);
                if (!new File(artifactPath).exists()) {

                    // normally maven dependency checks catch this, but m2e seems to bypass it
                    //
                    throw new MojoExecutionException("Artifact " + destName + " wasn't found at " + artifactPath);
                }

                FileItem dependencyFile = new FileItem();
                dependencyFile.setOutputDirectory("");
                dependencyFile.setSource(getArtifactPath(dependency));
                dependencyFile.setDestName(destName);
                deps.add(dependencyFile);

                //  Based on dependency type, we add them to the corresponding section in the manifest.
                //
                if (dependency.getType().equals("nar")) {
                    nativeClassPathItems.add(destName);
                } else if (isFragment(dependency)) {
                    fragmentList.add(destName);
                } else {
                    classPathItems.add(destName);
                }
            }

            //  Build a manifest file and add as first file to the zip.
            //
            Map<String, String> extras = new HashMap<>();
            extras.put("TIBCO-EP-Fragment-Format-Version", "2");
            if (!classPathItems.isEmpty()) {
                extras.put("Class-Path", String.join(" ", classPathItems));
            }
            if (!nativeClassPathItems.isEmpty()) {
                extras.put("TIBCO-EP-Native-Class-Path", String.join(" ", nativeClassPathItems));
            }
            if (!fragmentList.isEmpty()) {
                extras.put(MANIFEST_TIBCO_EP_FRAGMENT_LIST, String.join(" ", fragmentList));
            }

            packageArchive(assembly, extras);

            //  Then, include the dependencies.
            //
            for (FileItem dependencyFile : deps) {
                assembly.addFile(dependencyFile);
            }

            //  Additional steps.
            //
            steps.forEach(step -> step.accept(assembly));

            //  Add the output directory.
            //
            File outputDirectory = new File(project.getBuild().getOutputDirectory());
            if (!outputDirectory.exists() && !outputDirectory.mkdirs()) {
                throw new MojoExecutionException("Output directory "
                    + outputDirectory.getAbsolutePath() + " could not be created");
            }

            FileSet fileSet = new FileSet();
            fileSet.setDirectory(project.getBuild().getOutputDirectory());
            fileSet.setOutputDirectory("");
            assembly.addFileSet(fileSet);

            //  Write the assembly.
            //
            writeAssembly(assembly);
            if (!temporaryManifestFile.delete()) {
                getLog().debug("Unable to delete " + temporaryManifestFile.getAbsolutePath());
            }
        }

        /**
         * Create a manifest, pom.xml and pom.properties files and add to assembly
         *
         * @param assembly assembly to add manifest
         * @param extras   map of any additional manifest entries
         * @throws MojoExecutionException on error
         */
        private void packageArchive(Assembly assembly, Map<String, String> extras) throws MojoExecutionException {

            // make sure the build directory is created
            //
            File dir = new File(project.getBuild().getDirectory());
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    throw new MojoExecutionException("Unable to create: " + dir.getAbsolutePath());
                }
            }

            try {

                String productVersion = getProductVersion();
                addManifestToAssembly(extras, productVersion);

                addPomToAssembly();

                addPropertiesToAssembly();

            } catch (IOException e) {

                throw new MojoExecutionException("Failed to create manifest: " + e.getMessage(), e);
            }
        }

        private void addPropertiesToAssembly() throws IOException {

            //  Add the pom.properties file.
            //
            File tempPropertiesFile = File
                .createTempFile("pom", "properties", new File(project.getBuild()
                    .getDirectory()));
            tempPropertiesFile.deleteOnExit();
            String tempPropertiesPath = tempPropertiesFile.getAbsolutePath();

            try (FileOutputStream pos = new FileOutputStream(new File(tempPropertiesPath));
                 OutputStreamWriter osw = new OutputStreamWriter(pos)) {

                osw.write("# Created by TIBCO Streaming Maven Plugin\n");
                osw.write("version=" + project.getVersion() + "\n");
                osw.write("groupId=" + project.getGroupId() + "\n");
                osw.write("artifactId=" + project.getArtifactId() + "\n");
                if (productVersion != null && productVersion.length() > 0) {
                    osw.write("productVersion=" + productVersion + "\n");
                }
            }

            FileItem properties = new FileItem();
            properties.setSource(tempPropertiesPath);
            properties.setDestName("META-INF/maven/" + project.getGroupId() + "/" + project
                .getArtifactId() + "/pom.properties");
            assembly.addFile(properties);
        }

        private void addPomToAssembly() {
            //  Add the pom.xml
            //
            FileItem pom = new FileItem();
            pom.setSource(project.getFile().getAbsolutePath());
            pom.setDestName(getPOMPathInArchive(project.getGroupId(), project.getArtifactId()));
            assembly.addFile(pom);
        }

        private Attributes.Name name(String name) {
            return new Attributes.Name(name);
        }

        private void addManifestToAssembly(Map<String, String> extras, String productVersion) throws IOException {
            //  Create a temporary manifest file.
            //  Keep it as a class parameter for cleanup at the end of archive generation.
            //
            temporaryManifestFile = File.createTempFile("MAN", "MF",
                new File(project.getBuild().getDirectory()));

            String tempManifestPath = temporaryManifestFile.getAbsolutePath();
            Manifest manifest = new Manifest();
            Attributes attributes = manifest.getMainAttributes();
            attributes.put(Attributes.Name.MANIFEST_VERSION, "1.0");
            attributes.put(name("Archiver-Version"), "Plexus Archiver");
            attributes.put(name("Built-By"), System.getProperty("user.name"));
            attributes.put(name("Build-Jdk"), System.getProperty("java.version"));
            attributes.put(name("Package-Title"), project.getName());

            String buildNumber = (String) project.getProperties().get("buildNumber");
            if (buildNumber != null) {
                attributes.put(name("Package-Version"), project.getVersion() + " " + buildNumber);
            } else {
                attributes.put(name("Package-Version"), project.getVersion());
            }

            if (project.getOrganization() != null) {
                attributes.put(name("Package-Vendor"), project.getOrganization()
                    .getName());
            }

            if (mainClass != null && mainClass.length() > 0) {
                attributes.put(Attributes.Name.MAIN_CLASS, mainClass);
            }

            attributes.put(name("TIBCO-EP-Fragment-Type"), project.getPackaging());
            attributes.put(name("TIBCO-EP-Fragment-Identifier"),
                project.getGroupId() + "." + project.getArtifactId());

            if (productVersion != null && productVersion.length() > 0) {
                attributes.put(name("TIBCO-EP-Build-Product-Version"), productVersion);
            }

            if (extras != null) {
                for (Map.Entry<String, String> entry : extras.entrySet()) {
                    attributes.put(name(entry.getKey()), entry.getValue());
                }
            }

            //  Add the additional manifest entries, if any.
            //
            additionalManifestEntries.forEach((name, value) -> attributes.put(name(name), value));

            // Add additional manifest data from the plugin configuration
            archive.getManifestEntries().forEach((name, value) -> attributes.put(name(name), value));
            for (ManifestSection section : archive.getManifestSections()) {
                Attributes sectionAttributes = new Attributes();
                section.getManifestEntries().forEach((key, value) -> sectionAttributes.put(name(key), value));
                manifest.getEntries().put(section.getName(), sectionAttributes);
            }

            //  Write the manifest file
            try (FileOutputStream os = new FileOutputStream(new File(tempManifestPath))) {
                manifest.write(os);
            }

            //  Declare the final manifest file, based on the generated temporary one.
            //
            FileItem manifestFileItem = new FileItem();
            manifestFileItem.setSource(tempManifestPath);
            manifestFileItem.setDestName("META-INF/MANIFEST.MF");
            assembly.addFile(manifestFileItem);
        }

        /**
         * Write an assembly file and attach to build
         *
         * @param assembly assembly
         * @throws MojoExecutionException on error
         */
        private void writeAssembly(Assembly assembly) throws MojoExecutionException {
            File assemblyFile;

            ConfigurationSource configSource = new ConfigurationSource(project, localRepository, session);
            try {
                getLog().debug("Assembly: " + project.getVersion(), new RuntimeException(project
                    .getVersion()));
                assemblyFile = assemblyArchiver
                    .createArchive(assembly, project.getArtifactId() + "-" + project
                        .getVersion() + "-" + project
                        .getPackaging(), "zip",
                        configSource, FileTime.fromMillis(System.currentTimeMillis()));
            } catch (Exception e) {

                throw new MojoExecutionException(
                    "Failed to create plugin bundle: " + e.getMessage(), e.getCause());

            } finally {

                // clean temp directories
                //
                cleanup(configSource.getWorkingDirectory().toPath());
                cleanup(configSource.getTemporaryRootDirectory().toPath());
            }

            // Attach bundle assembly to the project
            project.getArtifact().setFile(assemblyFile);

            // Notify m2e
            //
            buildContext.refresh(assemblyFile);
        }

        private void cleanup(Path path) {
            try (Stream<Path> files = Files.walk(path)) {
                files.sorted(Comparator.reverseOrder()).map(Path::toFile).forEach(File::delete);
            } catch (IOException e) {
                getLog().debug("Could not cleanup", e);
            }
        }
    }
}
