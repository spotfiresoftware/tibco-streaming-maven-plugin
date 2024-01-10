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
package com.tibco.sb.sample.bestbidsandasks;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.streambase.sb.unittest.Expecter;
import com.streambase.sb.unittest.JSONSingleQuotesTupleMaker;
import com.streambase.sb.unittest.SBServerManager;
import com.streambase.sb.unittest.ServerManagerFactory;

/**
 * Example test case
 */
public class TestCase {

    private static SBServerManager server;

    /**
     * Set up the server
     * @throws Exception Initialization failed
     */
    @BeforeClass
    public static void setupServer() throws Exception {
        // create a StreamBase server and load applications once for all tests in this class
        server = ServerManagerFactory.getEmbeddedServer();
        server.startServer();
        server.loadApp("com.tibco.sb.sample.bestbidsandasks.BestBidsAsks");
    }

    /**
     * Stop the server
     * @throws Exception Shutdown failed
     */
    @AfterClass
    public static void stopServer() throws Exception {
        if (server != null) {
            server.shutdownServer();
            server = null;
        }
    }

    /**
     * Start the containers
     * @throws Exception Container startup error
     */
    @Before
    public void startContainers() throws Exception {
        // before each test, startup fresh container instances
        server.startContainers();
    }

    /**
     * test cases
     * @throws Exception Test failure
     */
	@Test
	public void test1() throws Exception {
		/*
		 * Example enqueuer using a TupleMaker that converts JSON strings to Tuples.
		 * JSONSingleQuotesTupleMaker is useful when typing JSON strings,
		 * by allowing you to use single quotes instead of escaping double quotes.
		 */
        server.getEnqueuer("NYSE_Feed").enqueue(
                JSONSingleQuotesTupleMaker.MAKER,
                "{'time_int':34306,'symbol':'JNY','bid_price':31.1,'bid_size':21,'ask_price':31.18,'ask_size':7,'sequence':20030}"
        );
		/*
		 * Example dequeuer using an alternate TupleMaker that converts Java Objects to
		 * Tuples. ObjectArrayTupleMaker maps Java Objects to Tuple field values
		 */
        new Expecter(server.getDequeuer("BestAsks")).expect(
                JSONSingleQuotesTupleMaker.MAKER,
                "{'time_int':34306,'symbol':'JNY','best_ask':31.18}"
            );
	}
	@Test
	public void test2() throws Exception {
        server.getEnqueuer("NYSE_Feed").enqueue(
                JSONSingleQuotesTupleMaker.MAKER,
                "{'time_int':34306,'symbol':'GEA','bid_price':26.23,'bid_size':3,'ask_price':26.33,'ask_size':4,'sequence':19961}"
        );
        new Expecter(server.getDequeuer("BestBids")).expect(
                JSONSingleQuotesTupleMaker.MAKER,
                "{'time_int':34306,'symbol':'GEA','best_bid':26.23}"
            );        
	}	

    /**
     * Stop containers
     * @throws Exception Container stop failed
     */
    @After
    public void stopContainers() throws Exception {
        // after each test, dispose of the container instances
        server.stopContainers();
    }
}
