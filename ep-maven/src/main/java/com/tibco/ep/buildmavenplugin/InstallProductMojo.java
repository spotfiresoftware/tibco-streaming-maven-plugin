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

import static org.apache.maven.plugins.annotations.LifecyclePhase.VALIDATE;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.codehaus.plexus.archiver.zip.ZipUnArchiver;

/**
 * <p>Install product zip and tgz artifacts if not already installed.</p>
 * 
 * <p>This goal scans through the project dependencies and if a product zip
 * or tgz artifact is not installed, it is installed into the $TIBCO_EP_HOME
 * directory.</p>
 * 
 * <p>Dependencies with scope <b>provided</b> are skipped.</p>
 * 
 * <p>A <b>markersDirectory</b> is used to track if the artifact has already
 * been installed - hence manually removing this directory will cause the
 * artifacts to be re-installed.  This directory is only created if there is
 * a need to install an artifact.</p>
 *
 * <p>To detect a manual installation of the product, a file specified by
 * <b>productValidationFile</b> is used.  If this file exists, then the
 * product installation is not attempted.</p>
 * 
 * <p>If the plugin does install a zip, then an md5 sum of the original zip
 * is saved - this allows installation of a newer version of the zip at a
 * later time.  This can happen with SNAPSHOTS.</p>
 */
@Mojo(name = "install-product", defaultPhase = VALIDATE, threadSafe = true)
public class InstallProductMojo extends BaseMojo {

    /**
     * <p>Directory to store flag files.</p>
     * 
     * <p>Relative to productHome.<p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/installproduct-markersDirectory.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter(defaultValue="dependency-maven-plugin-markers")
    String markersDirectory;

    /**
     * <p>File to check if DTM product has been installed externally - if this exists,
     * then product installation is not attempted.</p>
     * 
     * <p>Relative to productHome.<p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/installproduct-dtmProductValidationFile.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter(defaultValue="distrib/tibco/dtm/deploy/dtm.kds")
    String dtmProductValidationFile;

    /**
     * <p>File to check if SB product has been installed externally - if this exists,
     * then product installation is not attempted.</p>
     * 
     * <p>Relative to productHome.<p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/installproduct-sbProductValidationFile.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter(defaultValue="distrib/tibco/sb/deploy/sb.kds")
    String sbProductValidationFile;

    /**
     * <p>File to check if DTM support has been installed externally - if this exists,
     * then support installation is not attempted.</p>
     * 
     * <p>Relative to productHome.<p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/installproduct-dtmSupportValidationFile.svg" alt="pom">
     * 
     * @since 1.2.0
     */
    @Parameter(defaultValue="distrib/tibco/devbin/epadmin")
    String dtmSupportValidationFile;

    /**
     * <p>Set this to 'true' to skip running tests, but still compile them.</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/skipTests.svg" alt="pom">
     * 
     * <p>Example use on commandline:</p>
     * <img src="uml/skipTests-commandline.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter(property = "skipTests", defaultValue = "false")
    boolean skipTests;
    
    public void execute() throws MojoExecutionException {
        getLog().debug("Install product");

        prechecks();

        for (Artifact artifact : getProjectDependencies("zip")) {
            
            final String artifactAsString = artifact.toString();
            
            // skip provided
            if (artifact.getScope().equals(Artifact.SCOPE_PROVIDED)) {
                getLog().debug("Ignoring provided artifact " + artifactAsString);
                continue;
            }
            
            // Platform and main attachments only.
            if (!isPlatformArtifact(artifact)) {
                getLog().debug("Ignoring non-platform artifact " + artifactAsString);
                continue;
            }
            
            if (null != artifact.getClassifier()) {
                getLog().debug("Ignore classified artifact " + artifactAsString);
                continue;
            }

            getLog().debug(artifactAsString+" local path = "+getArtifactPath(artifact));

            File sourceFile = new File(getArtifactPath(artifact));
            
            boolean forceReplace = false;

            File markersFile = new File(productHome, markersDirectory+File.separator+getArtifactName(artifact)+".marker");

            if (isInstalled(markersFile, sourceFile, artifact)) {
                continue;
            }
            
            // now we need check for directories existing, write access and create markers directory
            
            productHome.mkdirs();
            if (!productHome.exists()) {
                throw new MojoExecutionException("The product directory "+productHome+" does not exists or cannot be created");
            }
            if (! productHome.canWrite()) {
                throw new MojoExecutionException("The product directory "+productHome+" cannot be written to for markers directory");
            }
            

            Locker locker = new Locker(productHome);
            try {
                

                File markersDir = new File(productHome, markersDirectory);

                // Create markers directory of it doesn't exist
                //
                if (!markersDir.exists() && !markersDir.mkdirs()) {
                    throw new MojoExecutionException("Unable to create markers directory "+markersDir.getAbsolutePath());
                }
                
                // we might have held the lock for a long time, so check again before doing the big work
                if (isInstalled(markersFile, sourceFile, artifact)) {
                    locker.release();
                    continue;
                }
                
                getLog().info("Installing "+artifactAsString+" to "+productHome);

                if (!productHome.exists() && !productHome.mkdirs()) {
                    locker.release();
                    throw new MojoExecutionException("Unable to create product directory "+productHome.getAbsolutePath());
                }

                // delete any old files - this avoid permission issues when re-extracting
                //
                ZipInputStream zis = null;
                try {
                    zis = new ZipInputStream(new FileInputStream(sourceFile));
                    ZipEntry zipEntry = zis.getNextEntry();
                    while(zipEntry != null){
                        File oldFile = new File(productHome, zipEntry.getName());
                        if (oldFile.exists() && oldFile.isFile()) {
                            getLog().debug("File "+oldFile+" already exists so deleting");
                            if (!oldFile.delete()) {
                                getLog().warn("File "+oldFile+" failed to be deleted");
                            }
                        }
                        zipEntry = zis.getNextEntry();
                    }
                    zis.closeEntry();
                } catch (IOException e) {
                } finally {
                    if (zis != null) {
                        try {
                            zis.close();
                        } catch (IOException e) {
                        }
                    }
                }

                // and extract
                //
                ZipUnArchiver ua = new ZipUnArchiver();
                ua.setSourceFile(sourceFile);
                ua.setDestDirectory(productHome);
                if (forceReplace) {
                    ua.setOverwrite(true);
                }
                ua.extract();

                // we installed this, so create a md5 of original zip
                //
                String md5 = this.md5(sourceFile);
                BufferedWriter output = null;
                try {
                    output = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(markersFile), StandardCharsets.UTF_8));
                    output.write(md5);
                } catch (IOException e) {
                    getLog().warn("Unable to save zip checksum - "+e.getMessage());
                } finally {
                    if (output != null) {
                        try {
                            output.close();
                        } catch (IOException e) {
                        }
                    }
                }

            } finally {
                locker.release();
            }

        }
    }
    
    // return true if already installed, false otherwise
    //
    private boolean isInstalled(File markersFile, File sourceFile, Artifact artifact) {
        boolean forceReplace = false;
        
        if (markersFile.exists()) {
            // if we have an md5 of the installation and its not the same as the zip we are installing, 
            // install anyway
            //
            // this can happen with SNAPSHOT builds
            //
            if (markersFile.length() > 0) {
                BufferedReader input = null;
                try {
                    input = new BufferedReader(new InputStreamReader(new FileInputStream(markersFile), StandardCharsets.UTF_8));
                    String oldmd5 = input.readLine();
                    if (oldmd5 == null) {
                        getLog().warn("Unable to verify zip checksum");
                    } else {
                        String md5 = this.md5(sourceFile);
                        if (!oldmd5.equals(md5)) {
                            getLog().warn("Previous installation at "+productHome+" is old - overwriting");
                            forceReplace = true;
                        }
                    }
                } catch (IOException e) {
                    getLog().warn("Unable to verify zip checksum - "+e.getMessage());
                } finally {
                    if (input != null) {
                        try {
                            input.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }

            if (!forceReplace) {
                getLog().info(artifact.toString()+" already installed by maven to "+productHome);
                return true;
            }
        }

        if (!forceReplace && isSBProduct(artifact) && new File(productHome, sbProductValidationFile).exists()) {
            getLog().info(artifact.toString()+" already installed manually to "+productHome);
            return true;
        }
        if (!forceReplace && isDTMProduct(artifact) && new File(productHome, dtmProductValidationFile).exists()) {
            getLog().info(artifact.toString()+" already installed manually to "+productHome);
            return true;
        }
        if (!forceReplace && isDTMSupport(artifact) && new File(productHome, dtmSupportValidationFile).exists()) {
            getLog().info(artifact.toString()+" already installed manually to "+productHome);
            return true;
        }

        if (!sourceFile.exists()) {
            getLog().debug(getArtifactPath(artifact)+" not downloaded yet ... skipping unpack");
            return true;
        }
        
        return false;
    }
    
    
    private String md5(final File sourceFile) {
        
        String md5 = "";
        InputStream input = null;
        try {
            MessageDigest md5Local = MessageDigest.getInstance("MD5");
            input = new FileInputStream(sourceFile.toString());
            byte[] buffer = new byte[4096];
            int n;
            while ((n = input.read(buffer)) != -1) {
                md5Local.update(buffer, 0, n);
            }
            byte[] digest = md5Local.digest();
            BigInteger bi = new BigInteger(1, digest);
            md5 = String.format("%0" + (digest.length << 1) + "X", bi);
        } catch (NoSuchAlgorithmException e) {
            getLog().warn("Unable to save zip checksum - "+e.getMessage());
        } catch (IOException e) {
            getLog().warn("Unable to save zip checksum - "+e.getMessage());
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                }
            }
        }
        
        return md5;
    }

}
