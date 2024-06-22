/*******************************************************************************
 * Copyright © 2018-2024. Cloud Software Group, Inc.
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

import org.apache.maven.plugin.testing.MojoRule;
import org.apache.maven.plugin.testing.resources.TestResources;
import org.junit.After;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * Node lifecycle - start and stop
 */
public class NodeLifecycleTest extends BetterAbstractMojoTestCase {

    private static final Logger LOGGER = LoggerFactory.getLogger(NodeLifecycleTest.class);
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
    SimulatedLog simulatedLog = new SimulatedLog(false);

    /**
     * Clean up after a test.
     * @throws Exception on error
     */
    @After
    public void postTest() throws Exception {

        File stop = new File("target/projects/lifecycle", "stop.xml");
        Assert.assertNotNull(stop);
        Assert.assertTrue(stop.exists());

        LOGGER.info("   Stop nodes");
        StopNodesMojo stopNodes = (StopNodesMojo) lookupConfiguredMojo(stop, "stop-nodes");
        Assert.assertNotNull(stopNodes);
        stopNodes.environment = new String[]{"BUILD_ID=" + System.getenv("BUILD_ID")};
        simulatedLog.reset();
        stopNodes.setLog(simulatedLog);
        stopNodes.execute();
        assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
        assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
        assertTrue(simulatedLog.getInfoLog(), simulatedLog.getInfoLog().contains("Node removed"));
    }

    /**
     * Install product - do this first
     *
     * @throws Exception on error
     */
    @Test
    public void testNodeLifecycle() throws Exception {
        LOGGER.info("Install Product");

        File pom = new File("target/projects", "pom.xml");
        Assert.assertNotNull(pom);
        Assert.assertTrue(pom.exists());

        InstallProductMojo installProduct = (InstallProductMojo) lookupConfiguredMojo(pom, "install-product");
        Assert.assertNotNull(installProduct);
        installProduct.setLog(simulatedLog);
        installProduct.execute();
        assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
        assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());

        LOGGER.info("Test lifecycle");
        simulatedLog = new SimulatedLog(false);

        pom = new File("target/projects/lifecycle", "pom.xml");
        Assert.assertNotNull(pom);
        Assert.assertTrue(pom.exists());

        File status = new File("target/projects/lifecycle", "status.xml");
        Assert.assertNotNull(status);
        Assert.assertTrue(status.exists());

        File status2 = new File("target/projects/lifecycle", "status2.xml");
        Assert.assertNotNull(status);
        Assert.assertTrue(status.exists());

        File status3 = new File("target/projects/lifecycle", "status3.xml");
        Assert.assertNotNull(status);
        Assert.assertTrue(status.exists());

        LOGGER.info("   Start nodes");
        StartNodesMojo startNodes = (StartNodesMojo) lookupConfiguredMojo(pom, "start-nodes");
        Assert.assertNotNull(startNodes);
        startNodes.environment = new String[]{"BUILD_ID=" + System.getenv("BUILD_ID")};
        simulatedLog.reset();
        startNodes.setLog(simulatedLog);
        startNodes.execute();
        assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
        assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
        assertTrue(simulatedLog.getInfoLog(), simulatedLog.getInfoLog().contains("Node started"));

        LOGGER.info("   Display nodes");
        AdministrationMojo administrationMojo = (AdministrationMojo) lookupConfiguredMojo(status, "administer-nodes");
        Assert.assertNotNull(administrationMojo);
        administrationMojo.environment = new String[]{"BUILD_ID=" + System.getenv("BUILD_ID")};
        simulatedLog.reset();
        administrationMojo.setLog(simulatedLog);
        administrationMojo.execute();
        assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
        assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
        assertTrue(simulatedLog.getInfoLog(), simulatedLog.getInfoLog().contains("Started"));

        LOGGER.info("   Display nodes2");
        administrationMojo = (AdministrationMojo) lookupConfiguredMojo(status2, "administer-nodes");
        Assert.assertNotNull(administrationMojo);
        administrationMojo.environment = new String[]{"BUILD_ID=" + System.getenv("BUILD_ID")};
        simulatedLog.reset();
        administrationMojo.setLog(simulatedLog);
        administrationMojo.execute();
        assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
        assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
        assertTrue(simulatedLog.getInfoLog(), simulatedLog.getInfoLog().contains("Started"));

        LOGGER.info("   Browse services");
        administrationMojo = (AdministrationMojo) lookupConfiguredMojo(status3, "administer-nodes");
        Assert.assertNotNull(administrationMojo);
        administrationMojo.environment = new String[]{"BUILD_ID=" + System.getenv("BUILD_ID")};
        simulatedLog.reset();
        administrationMojo.setLog(simulatedLog);
        administrationMojo.execute();
        assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
        assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());
    }
}
