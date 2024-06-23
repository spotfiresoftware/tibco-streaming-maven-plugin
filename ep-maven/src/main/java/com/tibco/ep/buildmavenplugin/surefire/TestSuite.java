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

import java.util.Map;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.ReporterManager;
import org.apache.maven.surefire.testset.SurefireTestSet;
import org.apache.maven.surefire.testset.TestSetFailedException;

import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.ResourceBundle;
import org.apache.maven.surefire.Surefire;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.suite.SurefireTestSuite;

/**
 * Test suite to run an explicit set of named unit test classes
 */
public class TestSuite implements SurefireTestSuite {
    /**
     * SureFire resource bundle
     */
    protected final static ResourceBundle bundle = ResourceBundle.getBundle(Surefire.SUREFIRE_BUNDLE_NAME);

    /**
     * Array of test class names (in Java binary format)
     */
    private final String[] m_testClasses;

    /**
     * The set of known tests sets, indexed by test set name.
     */
    private HashMap<String, SurefireTestSet> m_testMap;

    /**
     * Create a test suite runner for a series of test classes.
     * 
     * @param testClasses
     *            array of test class names
     */
    public TestSuite(String[] testClasses) {
        m_testClasses = Arrays.copyOf(testClasses, testClasses.length);
    }

    /**
     * Execute test suite
     * 
     * @param reporterManager
     *            Report manager
     * @param classLoader
     *            class loader
     * @throws ReporterException Reporting exception
     * @throws TestSetFailedException Test failure exception
     */
    @Override
    public void execute(ReporterManager reporterManager, ClassLoader classLoader)
            throws ReporterException, TestSetFailedException {
        for (SurefireTestSet testSet : m_testMap.values()) {
            executeTestSet(testSet, reporterManager, classLoader);
            reporterManager.reset();
        }
    }

    /**
     * Execute test test
     * 
     * @param testSetName
     *            Name of test set
     * @param reporterManager
     *            Report manager
     * @param classLoader
     *            Class loader
     * @throws ReporterException Reporting exception
     * @throws TestSetFailedException Test failure exception
     */
    @Override
    public void execute(String testSetName, ReporterManager reporterManager, ClassLoader classLoader)
            throws ReporterException, TestSetFailedException {
        SurefireTestSet testSet = m_testMap.get(testSetName);
        if (testSet == null) {
            // FIX THIS: is this even possible?
            throw new TestSetFailedException("Test set '" + testSetName + "' does not exist.");
        }
        executeTestSet(testSet, reporterManager, classLoader);
    }

    /**
     * Number of test cases
     * 
     * @return Number of test cases
     */
    @Override
    public int getNumTests() {
        return m_testClasses.length;
    }

    /**
     * Locate test sets
     * 
     * @param classLoader
     *            Class loader
     * @return Map of test sets
     * @throws TestSetFailedException Test failure exception
     */
    @Override
    public Map<String, SurefireTestSet> locateTestSets(ClassLoader classLoader)
    	throws TestSetFailedException {
        m_testMap = new HashMap<>();

        for (String className : m_testClasses) {
            Class<?> testClass;
            try {
                testClass = classLoader.loadClass(className);
            } catch (ClassNotFoundException e) {
                throw new TestSetFailedException("Unable to load test class '" + className + "'", e);
            }

            if (!Modifier.isAbstract(testClass.getModifiers())) {
                m_testMap.put(className, new TestSet(testClass));
            }
        }
        return Collections.unmodifiableMap(m_testMap);
    }

    private void executeTestSet(SurefireTestSet testSet, ReporterManager reporterManager, ClassLoader classLoader)
            throws ReporterException, TestSetFailedException {
        reporterManager.testSetStarting(
                new ReportEntry(this.getClass().getName(), testSet.getName(), bundle.getString("testSetStarting")));

        testSet.execute(reporterManager, classLoader);

        reporterManager.testSetCompleted(new ReportEntry(this.getClass().getName(), testSet.getName(),
                bundle.getString("testSetCompletedNormally")));

        reporterManager.reset();
    }
}
