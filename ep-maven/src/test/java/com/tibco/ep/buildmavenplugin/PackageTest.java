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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tibco.ep.buildmavenplugin.InstallProductMojo;
import com.tibco.ep.buildmavenplugin.PackageApplicationMojo;
import com.tibco.ep.buildmavenplugin.PackageJavaFragmentMojo;
import com.tibco.ep.buildmavenplugin.PackageLiveViewFragmentMojo;
import com.tibco.ep.buildmavenplugin.PackageEventFlowFragmentMojo;

/**
 * Packaging tests
 *
 */
public class PackageTest extends BetterAbstractMojoTestCase  {
    private Logger logger = LoggerFactory.getLogger(PackageTest.class);

    /**
     * rule
     */
    @Rule
    public MojoRule rule = new MojoRule();

    /**
     * resources
     */
    @Rule
    public TestResources resources = new TestResources();

    /**
     * Install product - do this first
     * 
     * @throws Exception on error
     */
    @Test
    public void testPackaging() throws Exception  {    
        logger.info("Install Product");
        SimulatedLog simulatedLog = new SimulatedLog(false);

        File pom = new File( "target/projects", "pom.xml" );
        Assert.assertNotNull( pom );
        Assert.assertTrue( pom.exists());

        InstallProductMojo installProduct = (InstallProductMojo) lookupConfiguredMojo(pom, "install-product");
        Assert.assertNotNull( installProduct );
        installProduct.setLog(simulatedLog);     
        installProduct.execute();
        assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
        assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());

        logger.info("SetResources");
        SetResources setResources = (SetResources) lookupConfiguredMojo(pom, "set-resources");
        Assert.assertNotNull(setResources);  
        simulatedLog.reset();
        setResources.setLog(simulatedLog);
        setResources.execute();
        assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
        assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
        
        logger.info("Java packaging");
        simulatedLog = new SimulatedLog(false);

        // simulate some classes and binaries
        //
        new File("target/projects/java/target/classes").mkdirs();
        new File("target/projects/java/target/classes/x.class").createNewFile();
        new File("target/projects/java/target/nar").mkdirs();
        new File("target/projects/java/target/nar/bin-x86_64-MacOSX").createNewFile();
        new File("target/projects/java/target/nar/bin-amd64-Linux").createNewFile();
        new File("target/projects/java/target/nar/bin-x86.Windows").createNewFile();

        pom = new File( "target/projects/java", "pom.xml" );
        Assert.assertNotNull(pom);
        Assert.assertTrue(pom.exists());

        // simulate process resources
        //
        Files.copy(new File("target/projects/java/src/main/resources/engine.conf").toPath(), new File("target/projects/java/target/classes/engine.conf").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(new File("target/projects/java/src/main/resources/my.properties").toPath(), new File("target/projects/java/target/classes/my.properties").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(new File("target/projects/java/src/main/resources/upgrade-plan.txt").toPath(), new File("target/projects/java/target/classes/upgrade-plan.txt").toPath(), StandardCopyOption.REPLACE_EXISTING);

        logger.info("   Package");
        PackageJavaFragmentMojo packageJava = (PackageJavaFragmentMojo) lookupConfiguredMojo(pom, "package-java-fragment");
        Assert.assertNotNull(packageJava);
        String pomVersion = packageJava.project.getVersion();
        simulatedLog.reset();
        packageJava.setLog(simulatedLog);
        packageJava.execute();
        assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
        assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
        // there is no output to check for this target
        assertTrue(new File("target/projects/java/target/java-"+pomVersion+"-ep-java-fragment.zip").exists());
        assertTrue(zipContains("target/projects/java/target/java-"+pomVersion+"-ep-java-fragment.zip", "upgrade-plan.txt"));
        assertTrue(zipContains("target/projects/java/target/java-"+pomVersion+"-ep-java-fragment.zip", "engine.conf"));
        assertTrue(zipContains("target/projects/java/target/java-"+pomVersion+"-ep-java-fragment.zip", "META-INF/MANIFEST.MF"));
        assertTrue(zipContains("target/projects/java/target/java-"+pomVersion+"-ep-java-fragment.zip", "META-INF/maven/com.tibco.ep.testmavenplugin/java/pom.xml"));
        assertTrue(zipContains("target/projects/java/target/java-"+pomVersion+"-ep-java-fragment.zip", "META-INF/maven/com.tibco.ep.testmavenplugin/java/pom.properties"));

        // simulate install
        //
        File javaDestDir = new File(this.regressionRepository, "com/tibco/ep/testmavenplugin/java/"+pomVersion);
        javaDestDir.mkdirs();
        Files.copy(new File("target/projects/java/target/java-"+pomVersion+"-ep-java-fragment.zip").toPath(), new File(javaDestDir, "java-"+pomVersion+".zip").toPath(), StandardCopyOption.REPLACE_EXISTING);

        logger.info("EventFlow packaging");
        simulatedLog = new SimulatedLog(false);

        pom = new File( "target/projects/eventflow", "pom.xml" );
        Assert.assertNotNull(pom);
        Assert.assertTrue(pom.exists());

        logger.info("   Compile");
        CompileEventFlow compileEventFlow = (CompileEventFlow) lookupConfiguredMojo(pom, "compile-eventflow-fragment");
        Assert.assertNotNull(compileEventFlow);  
        pomVersion = compileEventFlow.project.getVersion();
        simulatedLog.reset();
        compileEventFlow.setLog(simulatedLog);
        compileEventFlow.execute();
        assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
        assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
        
        // simulate process resources
        //
        new File("target/projects/eventflow/target/classes").mkdirs();
        Files.copy(new File("target/projects/eventflow/src/main/resources/deploy.conf").toPath(), new File("target/projects/eventflow/target/classes/deploy.conf").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(new File("target/projects/eventflow/src/main/resources/engine.conf").toPath(), new File("target/projects/eventflow/target/classes/engine.conf").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(new File("target/projects/eventflow/src/main/resources/upgrade-plan.txt").toPath(), new File("target/projects/eventflow/target/classes/upgrade-plan.txt").toPath(), StandardCopyOption.REPLACE_EXISTING);

        logger.info("   Package");
        PackageEventFlowFragmentMojo packageStreambase = (PackageEventFlowFragmentMojo) lookupConfiguredMojo(pom, "package-eventflow-fragment");
        Assert.assertNotNull(packageStreambase);  
        pomVersion = packageStreambase.project.getVersion();
        simulatedLog.reset();
        packageStreambase.setLog(simulatedLog);
        packageStreambase.execute();
        assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
        assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
        // there is no output to check for this target
        assertTrue(new File("target/projects/eventflow/target/eventflow-"+pomVersion+"-ep-eventflow-fragment.zip").exists());
        assertTrue(zipContains("target/projects/eventflow/target/eventflow-"+pomVersion+"-ep-eventflow-fragment.zip", "upgrade-plan.txt"));
        assertTrue(zipContains("target/projects/eventflow/target/eventflow-"+pomVersion+"-ep-eventflow-fragment.zip", "engine.conf"));
        assertTrue(zipContains("target/projects/eventflow/target/eventflow-"+pomVersion+"-ep-eventflow-fragment.zip", "my.sbapp"));
        assertTrue(zipContains("target/projects/eventflow/target/eventflow-"+pomVersion+"-ep-eventflow-fragment.zip", "mod/my.sbapp"));
        assertTrue(zipContains("target/projects/eventflow/target/eventflow-"+pomVersion+"-ep-eventflow-fragment.zip", "META-INF/maven/com.tibco.ep.testmavenplugin/eventflow/pom.xml"));
        assertTrue(zipContains("target/projects/eventflow/target/eventflow-"+pomVersion+"-ep-eventflow-fragment.zip", "META-INF/maven/com.tibco.ep.testmavenplugin/eventflow/pom.properties"));

        // simulate install
        //
        File eventflowDestDir = new File(this.regressionRepository, "com/tibco/ep/testmavenplugin/eventflow/"+pomVersion);
        eventflowDestDir.mkdirs();
        Files.copy(new File("target/projects/eventflow/target/eventflow-"+pomVersion+"-ep-eventflow-fragment.zip").toPath(), new File(eventflowDestDir, "eventflow-"+pomVersion+".zip").toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        // variation with eventflow deps
        //
        pom = new File( "target/projects/eventflow2", "pom.xml" );
        Assert.assertNotNull(pom);
        Assert.assertTrue(pom.exists());
        
        logger.info("   Unpack");
        UnpackFragmentMojo unpackFragment = (UnpackFragmentMojo) lookupConfiguredMojo(pom, "unpack-fragment");
        Assert.assertNotNull(unpackFragment);  
        simulatedLog.reset();
        unpackFragment.setLog(simulatedLog);
        unpackFragment.execute();
        
        logger.info("   Package with eventflow");
        packageStreambase = (PackageEventFlowFragmentMojo) lookupConfiguredMojo(pom, "package-eventflow-fragment");
        Assert.assertNotNull(packageStreambase);  
        pomVersion = packageStreambase.project.getVersion();
        simulatedLog.reset();
        packageStreambase.setLog(simulatedLog);
        packageStreambase.execute();
        assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
        assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
        // there is no output to check for this target
        assertTrue(new File("target/projects/eventflow2/target/eventflow2-"+pomVersion+"-ep-eventflow-fragment.zip").exists());
        assertTrue(zipContains("target/projects/eventflow2/target/eventflow2-"+pomVersion+"-ep-eventflow-fragment.zip", "com.tibco.ep.testmavenplugin-eventflow-"+pomVersion+"-ep-eventflow-fragment.zip"));
        // See https://jira.tibco.com/browse/SB-44404
        //
        assertFalse(zipContains("target/projects/eventflow2/target/eventflow2-"+pomVersion+"-ep-eventflow-fragment.zip", "engine.conf"));

       logger.info("TCS packaging");
       simulatedLog = new SimulatedLog(false);

       pom = new File( "target/projects/tcs", "pom.xml" );
       Assert.assertNotNull(pom);
       Assert.assertTrue(pom.exists());

       // simulate process resources
       //
       new File("target/projects/tcs/target/classes").mkdirs();
       Files.copy(new File("target/projects/tcs/src/main/configurations/engine.conf").toPath(), new File("target/projects/tcs/target/classes/engine.conf").toPath(), StandardCopyOption.REPLACE_EXISTING);
       Files.copy(new File("target/projects/tcs/src/main/configurations/flow.conf").toPath(), new File("target/projects/tcs/target/classes/flow.conf").toPath(), StandardCopyOption.REPLACE_EXISTING);
       Files.copy(new File("target/projects/tcs/src/main/resources/logback.xml").toPath(), new File("target/projects/tcs/target/classes/logback.xml").toPath(), StandardCopyOption.REPLACE_EXISTING);
        
       new File("target/projects/tcs/target/classes/com/tibco/ep/testmavenplugin").mkdirs();
       Files.copy(new File("target/projects/tcs/src/main/resources/com/tibco/ep/testmavenplugin/Test.schema").toPath(),
                  new File("target/projects/tcs/target/classes/com/tibco/ep/testmavenplugin/Test.schema").toPath(), StandardCopyOption.REPLACE_EXISTING);

       logger.info("   Package");
       PackageTCSFragmentMojo packageTCS = (PackageTCSFragmentMojo) lookupConfiguredMojo(pom, "package-tcs-fragment");
       Assert.assertNotNull(packageTCS);  
       pomVersion = packageTCS.project.getVersion();
       simulatedLog.reset();
       packageTCS.setLog(simulatedLog);
       packageTCS.execute();
       assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
       assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
       assertTrue(new File("target/projects/tcs/target/tcs-"+pomVersion+"-ep-tcs-fragment.zip").exists());
       assertTrue(zipContains("target/projects/tcs/target/tcs-"+pomVersion+"-ep-tcs-fragment.zip", "engine.conf"));
       assertTrue(zipContains("target/projects/tcs/target/tcs-"+pomVersion+"-ep-tcs-fragment.zip", "flow.conf"));
       assertTrue(zipContains("target/projects/tcs/target/tcs-"+pomVersion+"-ep-tcs-fragment.zip", "logback.xml"));
       assertTrue(zipContains("target/projects/tcs/target/tcs-"+pomVersion+"-ep-tcs-fragment.zip", "com/tibco/ep/testmavenplugin/Test.schema"));
       assertTrue(zipContains("target/projects/tcs/target/tcs-"+pomVersion+"-ep-tcs-fragment.zip", "META-INF/maven/com.tibco.ep.testmavenplugin/tcs/pom.xml"));
       assertTrue(zipContains("target/projects/tcs/target/tcs-"+pomVersion+"-ep-tcs-fragment.zip", "META-INF/maven/com.tibco.ep.testmavenplugin/tcs/pom.properties"));

       // simulate install
       //
       File tcsDestDir = new File(this.regressionRepository, "com/tibco/ep/testmavenplugin/tcs/"+pomVersion+"");
       tcsDestDir.mkdirs();
       Files.copy(new File("target/projects/tcs/target/tcs-"+pomVersion+"-ep-tcs-fragment.zip").toPath(), new File(tcsDestDir, "tcs-"+pomVersion+".zip").toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        logger.info("LiveView packaging");
        simulatedLog = new SimulatedLog(false);

        pom = new File( "target/projects/liveview", "pom.xml" );
        Assert.assertNotNull(pom);
        Assert.assertTrue(pom.exists());

        logger.info("   Compile");
        CompileLiveView compileLiveview = (CompileLiveView) lookupConfiguredMojo(pom, "compile-liveview-fragment");
        Assert.assertNotNull(compileLiveview);
        pomVersion = compileLiveview.project.getVersion();
        simulatedLog.reset();
        compileLiveview.setLog(simulatedLog);
        compileLiveview.execute();
        assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
        assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
        
        // simulate process resources
        //
        new File("target/projects/liveview/target/classes").mkdirs();
        Files.copy(new File("target/projects/liveview/src/main/resources/engine.conf").toPath(), new File("target/projects/liveview/target/classes/engine.conf").toPath(), StandardCopyOption.REPLACE_EXISTING);
        Files.copy(new File("target/projects/liveview/src/main/resources/upgrade-plan.txt").toPath(), new File("target/projects/liveview/target/classes/upgrade-plan.txt").toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        logger.info("   Package");
        PackageLiveViewFragmentMojo packageLivedatamart = (PackageLiveViewFragmentMojo) lookupConfiguredMojo(pom, "package-liveview-fragment");
        Assert.assertNotNull(packageLivedatamart);  
        pomVersion = packageLivedatamart.project.getVersion();
        simulatedLog.reset();
        packageLivedatamart.setLog(simulatedLog);
        packageLivedatamart.execute();
        assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
        assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
        // there is no output to check for this target
        assertTrue(new File("target/projects/liveview/target/liveview-"+pomVersion+"-ep-liveview-fragment.zip").exists());
        assertTrue(zipContains("target/projects/liveview/target/liveview-"+pomVersion+"-ep-liveview-fragment.zip", "upgrade-plan.txt"));
        assertTrue(zipContains("target/projects/liveview/target/liveview-"+pomVersion+"-ep-liveview-fragment.zip", "engine.conf"));
        assertTrue(zipContains("target/projects/liveview/target/liveview-"+pomVersion+"-ep-liveview-fragment.zip", "l.lvconf"));
        assertTrue(zipContains("target/projects/liveview/target/liveview-"+pomVersion+"-ep-liveview-fragment.zip", "l.sbapp"));
        assertTrue(zipContains("target/projects/liveview/target/liveview-"+pomVersion+"-ep-liveview-fragment.zip", "e.lvconf"));
        assertTrue(zipContains("target/projects/liveview/target/liveview-"+pomVersion+"-ep-liveview-fragment.zip", "e.sbapp"));
        assertTrue(zipContains("target/projects/liveview/target/liveview-"+pomVersion+"-ep-liveview-fragment.zip", "META-INF/maven/com.tibco.ep.testmavenplugin/liveview/pom.xml"));
        assertTrue(zipContains("target/projects/liveview/target/liveview-"+pomVersion+"-ep-liveview-fragment.zip", "META-INF/maven/com.tibco.ep.testmavenplugin/liveview/pom.properties"));

        // simulate install
        //
        File liveviewDestDir = new File(this.regressionRepository, "com/tibco/ep/testmavenplugin/liveview/"+pomVersion+"");
        liveviewDestDir.mkdirs();
        Files.copy(new File("target/projects/liveview/target/liveview-"+pomVersion+"-ep-liveview-fragment.zip").toPath(), new File(liveviewDestDir, "liveview-"+pomVersion+".zip").toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        
        logger.info("Application packaging");
        simulatedLog = new SimulatedLog(false);

        pom = new File( "target/projects/application", "pom.xml" );
        Assert.assertNotNull(pom);
        Assert.assertTrue(pom.exists());

        // simulate process resources
        //
        new File("target/projects/application/target/classes").mkdirs();
        Files.copy(new File("target/projects/application/src/main/resources/app.conf").toPath(), new File("target/projects/application/target/classes/app.conf").toPath(), StandardCopyOption.REPLACE_EXISTING);

        logger.info("   Package");
        PackageApplicationMojo packageApplication = (PackageApplicationMojo) lookupConfiguredMojo(pom, "package-application");
        Assert.assertNotNull(packageApplication);  
        pomVersion = packageApplication.project.getVersion();
        simulatedLog.reset();
        packageApplication.setLog(simulatedLog);
        packageApplication.execute();
        assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
        assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
        // there is no output to check for this target
        assertTrue(new File("target/projects/application/target/application-"+pomVersion+"-ep-application.zip").exists());
        assertTrue(zipContains("target/projects/application/target/application-"+pomVersion+"-ep-application.zip", "com.tibco.ep.testmavenplugin-java-"+pomVersion+"-ep-java-fragment.zip"));
        // tcs dep already includes the fragment so we shouldn't see it again
        assertFalse(zipContains("target/projects/application/target/application-"+pomVersion+"-ep-application.zip", "com.tibco.ep.testmavenplugin-eventflow-"+pomVersion+"-ep-eventflow-fragment.zip"));
        assertTrue(zipContains("target/projects/application/target/application-"+pomVersion+"-ep-application.zip", "com.tibco.ep.testmavenplugin-liveview-"+pomVersion+"-ep-liveview-fragment.zip"));
        assertTrue(zipContains("target/projects/application/target/application-"+pomVersion+"-ep-application.zip", "com.tibco.ep.testmavenplugin-tcs-"+pomVersion+"-ep-tcs-fragment.zip"));
        assertTrue(zipContains("target/projects/application/target/application-"+pomVersion+"-ep-application.zip", "org.slf4j-slf4j-api-1.7.26.jar"));
        assertTrue(zipContains("target/projects/application/target/application-"+pomVersion+"-ep-application.zip", "app.conf"));
        assertTrue(zipContains("target/projects/application/target/application-"+pomVersion+"-ep-application.zip", "META-INF/maven/com.tibco.ep.testmavenplugin/application/pom.xml"));
        assertTrue(zipContains("target/projects/application/target/application-"+pomVersion+"-ep-application.zip", "META-INF/maven/com.tibco.ep.testmavenplugin/application/pom.properties"));

        // simulate install
        //
        File applicationDestDir = new File(this.regressionRepository, "com/tibco/ep/testmavenplugin/application/"+pomVersion);
        applicationDestDir.mkdirs();
        Files.copy(new File("target/projects/application/target/application-"+pomVersion+"-ep-application.zip").toPath(), new File(applicationDestDir, "application-"+pomVersion+".zip").toPath(), StandardCopyOption.REPLACE_EXISTING);
        
        // application with no fragments - should fail
        //
        pom = new File( "target/projects/application3", "pom.xml" );
        Assert.assertNotNull(pom);
        Assert.assertTrue(pom.exists());

        logger.info("   Package with no fragment");
        packageApplication = (PackageApplicationMojo) lookupConfiguredMojo(pom, "package-application");
        Assert.assertNotNull(packageApplication);  
        pomVersion = packageApplication.project.getVersion();
        simulatedLog.reset();
        packageApplication.setLog(simulatedLog);
        boolean failed=false;
        try {
            packageApplication.execute();
        } catch (MojoExecutionException e) {
            failed=true;
        }
        Assert.assertTrue(failed);
    }

    /**
     * Check if the zip  contains a file
     * @param zip zip archive
     * @param filename filename
     * @return true if it does contain it
     */
    boolean zipContains(String zip, String filename) {
        boolean found=false;

        ZipFile zipFile;
        try {
            zipFile = new ZipFile(zip);

            Enumeration<? extends ZipEntry> zipEntries = zipFile.entries();
            String fname;
            while (zipEntries.hasMoreElements()) {
                fname = ((ZipEntry)zipEntries.nextElement()).getName();
                if (fname.equals(filename)) {
                    found = true;
                }
            }
            zipFile.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return found;
    }
    
    /**
     * Check if the file contains test
     * @param filename gile
     * @param text text to look for
     * @return true if it does contain it
     */
    boolean fileContains(String filename, String text) {
        boolean found = false;
        try {
            File file = new File(filename);
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String line = scanner.nextLine();
                if(line.contains(text)) {
                     found = true;
                }
            }
            scanner.close();
        } catch (FileNotFoundException e) {
        }
        return found;
    }
}
