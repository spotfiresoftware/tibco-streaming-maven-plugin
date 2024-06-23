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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import static org.apache.maven.plugins.annotations.LifecyclePhase.TEST;

import java.util.Properties;

/**
 * <p>Check for test cases.</p>
 * 
 * <p>If no test cases are found, <b>testCasesFound</b> property is set to true so that nodes are
 * not started or stopped.</p>
 */
@Mojo(name = "check-testcases", defaultPhase = TEST, threadSafe = true)
public class CheckTestCasesMojo extends BaseTestMojo {

    /**
     * <p>Set this to 'true' to skip running tests, but still compile them.</p>
     * 
     * <p>Example use in pom.xml:</p>
     * <img src="uml/start-nodes-skipTests.svg" alt="pom">
     * 
     * <p>Example use on commandline:</p>
     * <img src="uml/start-nodes-skipTests-commandline.svg" alt="pom">
     * 
     * @since 1.0.0
     */
    @Parameter(property = "skipTests", defaultValue = "false")
    boolean skipTests;
    
    public void execute() throws MojoExecutionException {

        getLog().debug( "Checking for test cases");

        if (skipTests || installOnly) {
            getLog().info("Tests are skipped (skipTests="+skipTests+",installOnly="+installOnly+")");
            return;
        }
        
        Properties modelProperties = project.getModel().getProperties();
        
        String[] testClasses = scanForTests();
        if (testClasses == null || testClasses.length == 0) {
            getLog().info("No test cases found, setting testCasesFound=false to skip redundant start and stop nodes");
            modelProperties.setProperty(TESTCASESFOUND_PROPERTY, "false");
        } else {
            modelProperties.setProperty(TESTCASESFOUND_PROPERTY, "true");
        }
        
        project.getModel().setProperties(modelProperties);

    }

}
