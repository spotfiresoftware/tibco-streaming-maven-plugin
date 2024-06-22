/*******************************************************************************
 * Copyright (C) 2018-2024. Cloud Software Group, Inc.
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
package com.tibco.ep.buildmavenplugin.surefire;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.maven.surefire.Surefire;
import org.apache.maven.surefire.report.BriefConsoleReporter;
import org.apache.maven.surefire.report.BriefFileReporter;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.XMLReporter;
import org.apache.maven.surefire.testset.TestSetFailedException;

/**
 * Test runner invoked on the server by the Deployment plugin.
 */
public class Runner
{
    
    protected final static String SYS_PROP_PREFIX = "com.tibco.";
    protected final static String SYS_PROP_PREFIX_JUNIT_RESULTS_REPORTER = SYS_PROP_PREFIX + "junit.results.";
    
    /**
     * system property for reports
     */
    protected final static String REPORTS_DIRECTORY = "com.tibco.reportsdirectory";
    
    /**
     * system property for node name - defined in the runtime but this would be an adanced definition
     */
    protected final static String NODE_NAME = "com.kabira.platform.node.name";
    
    /**
     * Main method to run a set of unit tests on the node.
     * 
     * @param args
     *            a series of unit test class names
     * @throws ReporterException Reporting error
     * @throws TestSetFailedException  Test failure
     */
    public static void main(String[] args) throws ReporterException, TestSetFailedException {

        // parse argumnets
        //
        // arg[0] "true" to use system exit on success, false otherwise
        // args[1]+ contains junit test cases
        //
        boolean useSystemExit = Boolean.parseBoolean(args[0]);

        List<Object[]> reportDefinition = new ArrayList<Object[]>();
        List<Object> testSuites = new ArrayList<>();
        Surefire surefire = new Surefire();

        reportDefinition.add(new Object[] { BriefConsoleReporter.class.getName(), new Object[] { Boolean.TRUE } });
        reportDefinition.add(new Object[] { XMLReporter.class.getName(), new Object[] { new File(System.getProperty(REPORTS_DIRECTORY)+File.separator+System.getProperty(NODE_NAME)), false } });
        reportDefinition.add(new Object[] { BriefFileReporter.class.getName(), new Object[] { new File(System.getProperty(REPORTS_DIRECTORY)+File.separator+System.getProperty(NODE_NAME)), false } });

        String[] testFileNames = null;
        if (args.length > 1) {
            testFileNames = Arrays.copyOfRange(args, 1, args.length);
        }

        // ================ (Optionally) Set up the Reporter that sends test results to the Studio-side JUnit view ================
        //
        // If the "SYS_PROP_PREFIX_JUNIT_RESULTS_REPORTER + port" System property is assigned a valid port number,
        // then that means an EclipseJUnitViewSocketReporter instance should be created,
        // for the sake of sending Surefire test results to the JUnit view in Studio.
        //
        // For the sake of development-level testing, the constructor accepts two debug-output System property booleans.
        // When true, "SYS_PROP_PREFIX_JUNIT_RESULTS_REPORTER + tracecalls" prints to stdout whenever a Reporter interface call is made.
        // When true, "SYS_PROP_PREFIX_JUNIT_RESULTS_REPORTER + tracesocketmessages" prints to stdout a copy of the messages sent to the
        // JUnit client via the Socket connection.
        //
        Integer testResultsPort = 0;
        String testResultsPortString = System.getProperty(SYS_PROP_PREFIX_JUNIT_RESULTS_REPORTER + "port");
        if (testResultsPortString != null) {
            try {
                testResultsPort = Integer.parseInt(testResultsPortString);
            } catch (NumberFormatException e) {
                // fall-through
            }
        }
        if (testResultsPort.intValue() != 0) {
            Boolean traceCalls = Boolean
                    .parseBoolean(System.getProperty(SYS_PROP_PREFIX_JUNIT_RESULTS_REPORTER + "tracecalls"));
            Boolean traceSocketMessages = Boolean
                    .parseBoolean(System.getProperty(SYS_PROP_PREFIX_JUNIT_RESULTS_REPORTER + "tracesocketmessages"));
            reportDefinition.add(new Object[] { EclipseJUnitViewSocketReporter.class.getName(),
                    new Object[] { testResultsPort, traceCalls, traceSocketMessages, testFileNames } });
        }
        // ========================================================================================================================

        if (args.length == 1) {
            // FIX THIS - this shouldn't happen if called correctly from maven
            //
            String[] tclasses = new String[1];
            testSuites.add(new Object[] { TestSuite.class.getName(), new Object[] { tclasses } });
        } else {
            testSuites.add(new Object[] { TestSuite.class.getName(), new Object[] { Arrays.copyOfRange(args, 1, args.length) } });
        }

        // FIX THIS: does not respect "failIfNoTests" parameter

        ClassLoader.getSystemClassLoader().setDefaultAssertionStatus(true);

        // Surefire....sigh. The run() method is not documented, but zero
        // indicates SUCCESS. (The const is private so you'll have to trust me)
        int retval = surefire.run(reportDefinition, testSuites, ClassLoader.getSystemClassLoader(),
                ClassLoader.getSystemClassLoader(), Boolean.TRUE);

        if (useSystemExit || retval != 0) {
            System.exit(retval);
        }
    }
}
