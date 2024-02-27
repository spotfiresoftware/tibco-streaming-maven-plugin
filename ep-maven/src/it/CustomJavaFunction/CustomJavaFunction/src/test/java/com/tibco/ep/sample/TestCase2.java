package com.tibco.ep.sample;

/*
 * Copyright (C) 2024, Cloud Software Group, Inc.
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
 */

import static org.junit.Assert.assertNotNull;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.streambase.sb.StreamBaseException;
import com.streambase.sb.unittest.Expecter;
import com.streambase.sb.unittest.JSONSingleQuotesTupleMaker;
import com.streambase.sb.unittest.SBServerManager;
import com.streambase.sb.unittest.ServerManagerFactory;

import com.tibco.ep.testing.framework.Configuration;
import com.tibco.ep.testing.framework.ConfigurationException;
import com.tibco.ep.testing.framework.TransactionalDeadlockDetectedException;
import com.tibco.ep.testing.framework.TransactionalMemoryLeakException;
import com.tibco.ep.testing.framework.UnitTest;

/**
 * Example test case
 */
public class TestCase2 extends UnitTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(TestCase2.class);

    private static SBServerManager server;

    /**
     * Set up the server
     *
     * @throws StreamBaseException on start server error
     * @throws ConfigurationException on configuration failure
     * @throws InterruptedException on start server error
     */
    @BeforeClass
    public static void setupServer() throws StreamBaseException, ConfigurationException, InterruptedException {
        // Example configuration load
        Configuration.forFile("sbengine.conf").load().activate();

        // create a StreamBase server and load modules once for all tests in this class
        server = ServerManagerFactory.getEmbeddedServer();
        server.startServer();
        server.loadApp("com.tibco.ep.sample.custom-test-functions");
    }

    /**
     * Stop the server
     *
     * @throws StreamBaseException on shutdown failure
     * @throws InterruptedException on shutdown failure
     */
    @AfterClass
    public static void stopServer() throws InterruptedException, StreamBaseException {
        try {
            assertNotNull(server);
            server.shutdownServer();
            server = null;
        } finally {
            Configuration.deactiveAndRemoveAll();
        }
    }

    /**
     * Start the containers
     *
     * @throws StreamBaseException on start container error
     */
    @Before
    public void startContainers() throws StreamBaseException {
        // before each test, startup fresh container instances
        server.startContainers();

        // Setup test framework before running tests
        this.initialize();
    }

    /**
     * Test cases
     */
    @Test
    public void testCase1() throws Exception {
        LOGGER.info("\n\nRunning Test Case 1...\n");
        server.getEnqueuer("TriangleSidesIn").enqueue(JSONSingleQuotesTupleMaker.MAKER, "{'x':24.0,'y':36.0}");
        new Expecter(server.getDequeuer("TriangleDimOut")).expect(JSONSingleQuotesTupleMaker.MAKER,
            "{'x':24.0,'y':36.0,'customHypot':43.266615305567875,'customCalchyp':43.266615305567875,"
            + "'directMathHypot':43.266615305567875,'aliasedMathHypot':43.266615305567875,'aliasedMax':36.0}"
            );
    }

    @Test
    public void testCase2() throws Exception {
        LOGGER.info("\n\nRunning Test Case 2...\n");
        server.getEnqueuer("VarArgsIsIn").enqueue(JSONSingleQuotesTupleMaker.MAKER,
            "{'needle':'blue','haystack1':'red','haystack2':'purple'}",
            "{'needle':'red','haystack1':'blue','haystack2':'reddish'}");
        new Expecter(server.getDequeuer("NeedleInHaystackOut")).expect(JSONSingleQuotesTupleMaker.MAKER,
            "{'needle':'blue','haystack1':'red','haystack2':'purple','IsNeedleInHaystack':false}",
            "{'needle':'red','haystack1':'blue','haystack2':'reddish','IsNeedleInHaystack':true}");
    }

    @Test
    public void testCase3() throws Exception {
        LOGGER.info("\n\nRunning Test Case 3...\n");
        server.getEnqueuer("VarArgsSums").enqueue(JSONSingleQuotesTupleMaker.MAKER,
            "{'value1':12.0,'value2':36.0,'value3':92.0}");
        new Expecter(server.getDequeuer("SumAllOut")).expect(JSONSingleQuotesTupleMaker.MAKER,
            "{'value1':12.0,'value2':36.0,'value3':92.0,'sum':140.0}");
    }

    /**
     * Stop containers
     *
     * @throws StreamBaseException on stop container error
     * @throws TransactionalMemoryLeakException Leak detected
     * @throws TransactionalDeadlockDetectedException Deadlock detected
     */
    @After
    public void stopContainers() throws StreamBaseException, TransactionalMemoryLeakException, TransactionalDeadlockDetectedException {
        // Complete test framework and check for any errors
        this.complete();

        // after each test, dispose of the container instances
        server.stopContainers();
    }
}
