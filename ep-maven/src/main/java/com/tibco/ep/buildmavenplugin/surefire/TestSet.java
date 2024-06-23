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

import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.testset.AbstractTestSet;
import org.apache.maven.surefire.testset.TestSetFailedException;
import org.junit.runner.notification.RunListener;
import org.junit.runner.notification.RunNotifier;
import org.junit.runner.Request;

/**
 * Simple test set for JUnit-compatible tests.
 */
class TestSet extends AbstractTestSet {
    /**
     * Constructor
     * @param testClass Test class
     */
    TestSet(Class<?> testClass) {
        super(testClass);
        m_testClass = testClass;
    }

    /**
     * Execute this test set via the JUnit test runner.
     * 
     * @param reportManager Report manager
     * @param loader Class loader
     */
    public void execute(ReporterManager reportManager, ClassLoader loader) 
    	throws TestSetFailedException {
        org.junit.runner.Runner testRunner = Request.aClass(m_testClass).getRunner();
        RunListener runListener = new TestListener(this, reportManager);
        RunNotifier runNotifier = new RunNotifier();

        runNotifier.addListener(runListener);

        try {
            testRunner.run(runNotifier);
        } finally {
            runNotifier.removeListener(runListener);
        }
    }

    /**
     * Test set class to pass to the JUnit runner.
     */
    private Class<?> m_testClass;
}
