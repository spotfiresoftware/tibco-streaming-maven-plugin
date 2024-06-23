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
package com.tibco.ep.buildmavenplugin.surefire;

import java.util.ResourceBundle;
import org.apache.maven.surefire.Surefire;
import org.apache.maven.surefire.report.PojoStackTraceWriter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterManager;
import org.junit.runner.Description;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

/**
 * JUnit run listener that republishes JUnit events to the report manager.
 */
public class TestListener extends RunListener {
    /**
     * Constructor.
     *
     * @param testSet       the specific test set that this will report on as it is
     *                      executed
     * @param reportManager the report manager to log testing events to
     */
    TestListener(TestSet testSet, ReporterManager reportManager) {
        m_testSet = testSet;
        m_reportMgr = reportManager;
        m_testFailed = false;
    }

    /**
     * Test run started
     * @param description Test description
     * @throws ReporterException Reporting error
     */
    @Override
    public void testRunStarted(Description description) throws ReporterException {
        ReportEntry report = new ReportEntry(
                m_testSet.getName(),
                m_testSet.getName(),
                bundle.getString("testSetStarting"));

        m_reportMgr.testSetStarting(report);
    }

    /**
     * Test run finished
     * @param result Test result
     */
    @Override
    public void testRunFinished(Result result) {
        ReportEntry report = new ReportEntry(
                m_testSet.getName(),
                m_testSet.getName(),
                bundle.getString("testSetCompletedNormally"));

        this.m_reportMgr.testSetCompleted( report );
        this.m_reportMgr.reset();
    }

    /**
     * Test ignored
     * @param description Test description
     */
    @Override
    public void testIgnored(Description description) {
        ReportEntry report = new ReportEntry(
                m_testSet.getClass().getName(),
                description.getDisplayName(),
                bundle.getString("testSkipped") );

        m_reportMgr.testSkipped(report);
    }

    /**
     * Test started
     * @param description Test description
     * @throws Exception Test initialization faiulre
     */
    @Override
    public void testStarted(Description description)
        throws Exception {
        ReportEntry report = new ReportEntry(
                m_testSet.getClass().getName(),
                description.getDisplayName(),
                bundle.getString("testStarting"));

        m_reportMgr.testStarting(report);
        m_testFailed = false;
    }

    /**
     * Test failed
     * @param failure Failure reason
     */
    @Override
    public void testFailure(Failure failure) {
        ReportEntry report = new ReportEntry(
                m_testSet.getClass().getName(),
                failure.getTestHeader(),
                bundle.getString( "executeException" ),
                new PojoStackTraceWriter( 
                    m_testSet.getClass().getName(),
                    m_testSet.getName(),
                    failure.getException()));

        if (failure.getException() instanceof AssertionError) {
            m_reportMgr.testFailed( report );
        } else {
            m_reportMgr.testError( report );
        }

        m_testFailed = true;
    }

    /**
     * Test finished
     * @param description Test description
     */
    @Override
    public void testFinished(Description description) {
        if ( m_testFailed == false ) {
            ReportEntry report = new ReportEntry(
                    m_testSet.getName(),
                    description.getDisplayName(),
                    bundle.getString( "testSuccessful" ));

            this.m_reportMgr.testSucceeded( report );
        }
    }

    private static final ResourceBundle bundle
            = ResourceBundle.getBundle(Surefire.SUREFIRE_BUNDLE_NAME);
    private final TestSet m_testSet;
    private final ReporterManager m_reportMgr;

    /**
     * This flag is set after a failure has occurred so that a
     * <code>testSucceeded</code> event is not fired. This is necessary because
     * JUnit4 always fires a <code>testRunFinished</code> event-- even if
     * there was a failure.
     */
    private boolean m_testFailed;
}
