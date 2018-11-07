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

import java.io.File;

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.tibco.ep.buildmavenplugin.AdministrationMojo;
import com.tibco.ep.buildmavenplugin.InstallProductMojo;
import com.tibco.ep.buildmavenplugin.StartNodesMojo;
import com.tibco.ep.buildmavenplugin.StopNodesMojo;

/**
 * Node lifecycle - start and stop
 *
 */
public class NodeLifecycleTest extends BetterAbstractMojoTestCase  {
    private Logger logger = LoggerFactory.getLogger(NodeLifecycleTest.class);

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
    public void testNodeLifecycle() throws Exception  {    
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

        logger.info("Test lifecycle");
        simulatedLog = new SimulatedLog(false);

        pom = new File( "target/projects/lifecycle", "pom.xml" );
        Assert.assertNotNull(pom);
        Assert.assertTrue(pom.exists());

        File status = new File( "target/projects/lifecycle", "status.xml" );
        Assert.assertNotNull(status);
        Assert.assertTrue(status.exists());

        File status2 = new File( "target/projects/lifecycle", "status2.xml" );
        Assert.assertNotNull(status);
        Assert.assertTrue(status.exists());
        
        File stop = new File( "target/projects/lifecycle", "stop.xml" );
        Assert.assertNotNull(status);
        Assert.assertTrue(status.exists());
        
        try {
            
            logger.info("   Start nodes");
            StartNodesMojo startNodes = (StartNodesMojo) lookupConfiguredMojo(pom, "start-nodes");
            Assert.assertNotNull(startNodes);
            startNodes.environment = new String[] { "BUILD_ID="+System.getenv("BUILD_ID") };
            simulatedLog.reset();
            startNodes.setLog(simulatedLog);
            startNodes.execute();
            assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
            assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
            assertTrue(simulatedLog.getInfoLog(), simulatedLog.getInfoLog().contains("Node started"));

            logger.info("   Display nodes");
            AdministrationMojo administrationMojo = (AdministrationMojo) lookupConfiguredMojo(status, "administer-nodes");
            Assert.assertNotNull(administrationMojo);
            administrationMojo.environment = new String[] { "BUILD_ID="+System.getenv("BUILD_ID") };
            simulatedLog.reset();
            administrationMojo.setLog(simulatedLog);
            administrationMojo.execute();
            assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
            assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
            assertTrue(simulatedLog.getInfoLog(), simulatedLog.getInfoLog().contains("Started"));

            logger.info("   Display nodes2");
            administrationMojo = (AdministrationMojo) lookupConfiguredMojo(status2, "administer-nodes");
            Assert.assertNotNull(administrationMojo);
            administrationMojo.environment = new String[] { "BUILD_ID="+System.getenv("BUILD_ID") };
            simulatedLog.reset();
            administrationMojo.setLog(simulatedLog);
            administrationMojo.execute();
            assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
            assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
            assertTrue(simulatedLog.getInfoLog(), simulatedLog.getInfoLog().contains("Started"));
            
        } finally {
            
            logger.info("   Stop nodes");
            StopNodesMojo stopNodes = (StopNodesMojo) lookupConfiguredMojo(stop, "stop-nodes");
            Assert.assertNotNull(stopNodes);
            stopNodes.environment = new String[] { "BUILD_ID="+System.getenv("BUILD_ID") };
            simulatedLog.reset();
            stopNodes.setLog(simulatedLog);
            stopNodes.execute();
            assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
            assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
            assertTrue(simulatedLog.getInfoLog(), simulatedLog.getInfoLog().contains("Node removed"));
            
        }
    }
}
