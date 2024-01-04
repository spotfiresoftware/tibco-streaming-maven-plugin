/*******************************************************************************
 * Copyright (C) 2018-2023 Cloud Software Group, Inc.
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

import com.tibco.ep.buildmavenplugin.InstallProductMojo;
import com.tibco.ep.buildmavenplugin.UnpackFragmentMojo;
import com.tibco.ep.buildmavenplugin.UnpackNarMojo;

/**
 * Unpack tests
 *
 */
public class UnpackTest extends BetterAbstractMojoTestCase  {
    private Logger logger = LoggerFactory.getLogger(UnpackTest.class);

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
    public void testUnpack() throws Exception  {    
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

        logger.info("unpack test");
        simulatedLog = new SimulatedLog(false);

        File testPom = new File( "target/projects/eventflow", "pom.xml" );
        Assert.assertNotNull(testPom);
        Assert.assertTrue(testPom.exists());

        // FIX THIS - we need to figure out how to add a dependency here so it gets extracted
        
        logger.info("   Unpack fragment");
        UnpackFragmentMojo unpackFragment = (UnpackFragmentMojo) lookupConfiguredMojo(testPom, "unpack-fragment");
        Assert.assertNotNull(unpackFragment);  
        simulatedLog.reset();
        unpackFragment.setLog(simulatedLog);
        unpackFragment.execute();
        assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
        assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());

        logger.info("   Unpack nar");
        UnpackNarMojo unpackNar = (UnpackNarMojo) lookupConfiguredMojo(testPom, "unpack-nar");
        Assert.assertNotNull(unpackNar);  
        simulatedLog.reset();
        unpackNar.setLog(simulatedLog);
        unpackNar.execute();
        assertEquals(simulatedLog.getErrorLog(), 0, simulatedLog.getErrorLog().length());
        assertEquals(simulatedLog.getWarnLog(), 0, simulatedLog.getWarnLog().length());

    }

}
