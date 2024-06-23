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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.apache.maven.surefire.report.AbstractReporter;
import org.apache.maven.surefire.report.ReportEntry;
import org.apache.maven.surefire.report.ReporterException;
import org.apache.maven.surefire.report.StackTraceWriter;


/**
 * <p>
 * A Reporter that reports results via a socket connection to Eclipse (or
 * StreamBase Studio) and its JUnit view, using a syntax and protocol specific
 * to that view.
 * </p>
 * <p>
 * For the sake of development-level testing, the constructor accepts two
 * debug-output booleans. When true, "traceCalls" prints to stdout whenever a
 * Reporter interface call is made. When true, "traceSocketMessages" prints to
 * stdout a copy of the messages sent to the JUnit client via the Socket
 * connection.
 * </p>
 */
public class EclipseJUnitViewSocketReporter extends AbstractReporter {

    // --- members ---

    /** Port to connect to. */
    protected final int fPort;

    /** Is tracing of calls enabled? */
    protected final boolean fTraceCalls;

    /** Is tracing of socket messages enabled? */
    protected final boolean fTraceSocketMessages;

    /** The client socket. */
    protected Socket fClientSocket;

    /** The test classes (fully qualified) */
    protected final String[] fTestClasses;

    /** Print writer for sending messages */
    protected PrintWriter fWriter;

    /**
     * The start time for all tests; used to calculate the overall duration of all
     * tests at the end
     */
    protected long fTestStartTime;

    /**
     * JUnit wants unique integers assigned to tests; this map provides that mapping
     */
    protected HashMap<String, Integer> fTestNameToIdMap = new HashMap<>();

    /** Tracks the next available unique ID number used for "fTestNameToIdMap" */
    protected int fNextAvailableId = 1;

    /**
     * Constructor
     * 
     * @param testResultsPort test results port number
     * @param traceCalls true to trace calls
     * @param traceSocketMessages true to trace socket messages
     * @param testClasses test case classes
     */
    public EclipseJUnitViewSocketReporter(Integer testResultsPort, Boolean traceCalls, Boolean traceSocketMessages,
            String[] testClasses) {
        super(false);
        fPort = testResultsPort;
        fTraceCalls = traceCalls;
        fTraceSocketMessages = traceSocketMessages;
        fTestClasses = Arrays.copyOf(testClasses, testClasses.length);

        if (fTraceCalls) {
            System.out.println("new SocketReporter(port = " + fPort + ", traceCalls = " + traceCalls
                    + ", traceSocketMessages = " + traceSocketMessages + ")");
        }
        connectToClient();
    }

    // --- methods ---

    /** Forms the Socket connection to the JUnit client in Eclipse/Studio */
    protected void connectToClient() {
        if (fTraceCalls) {
            System.out.println("RemoteTestRunner: trying to connect to port " + fPort); //$NON-NLS-1$ //$NON-NLS-2$
        }
        Exception exception = null;
        // Try forming the Socket connection up to 40 times, waiting a second between
        // each try:
        for (int i = 1; i < 40; i++) {
            try {
                fClientSocket = new Socket((String) null, fPort);
                fWriter = new PrintWriter(
                        new BufferedWriter(new OutputStreamWriter(fClientSocket.getOutputStream(), StandardCharsets.UTF_8)),
                        false);

                // See SB-43820:
                // If we support re-running tests in the future, then this Reader will be useful for getting re-run requests from the client.
                // See "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner" for more details.
                /*
                try {
                    fReader= new BufferedReader(new InputStreamReader(fClientSocket.getInputStream(), "UTF-8")); //$NON-NLS-1$
                } catch (UnsupportedEncodingException e1) {
                    fReader= new BufferedReader(new InputStreamReader(fClientSocket.getInputStream()));
                }
                fReaderThread= new ReaderThread();
                fReaderThread.start();
                */
                if (fTraceCalls) {
                    System.out.println("RemoteTestRunner: successfully connected to port " + fPort); //$NON-NLS-1$
                }
                return; // We didn't throw an Exception, so: success !!
            } catch (IOException e) {
                exception = e;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
            }
        }
        if (fTraceCalls) {
            System.out.println("RemoteTestRunner: failed to connect to port " + fPort); //$NON-NLS-1$
        }
        runFailed("Could not connect to port " + fPort, exception);
    }

    /**
     * Run failed
     * 
     * @param message failure message
     * @param exception exception
     */
    public void runFailed(String message, Exception exception) {
        System.err.println(message);
        if (exception != null) {
            exception.printStackTrace(System.err);
        }
    }

    /** Shuts down the connection to the remote test listener. */
    private void shutDown() {
        if (fTraceCalls) {
            System.out.println("shutdown()");
        }
        if (fWriter != null) {
            fWriter.close();
            fWriter = null;
        }
        // See SB-43820:
        // If we support re-running tests in the future, then this Reader will be useful for getting re-run requests from the client.
        // See "org.eclipse.jdt.internal.junit.runner.RemoteTestRunner" for more details.
        // try {
        //    if (fReaderThread != null)   {
        //        // interrupt reader thread so that we don't block on close
        //        // on a lock held by the BufferedReader
        //        // fix for bug: 38955
        //        fReaderThread.interrupt();
        //    }
        //    if (fReader != null) {
        //        fReader.close();
        //        fReader= null;
        //    }
        // }
        // catch (IOException e) {
        //    if (fDebugMode)
        //        e.printStackTrace();
        // }

        try {
            if (fClientSocket != null) {
                fClientSocket.close();
                fClientSocket = null;
            }
        } catch (IOException e) {
            if (fTraceCalls) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void runStarting(int testCount) {
        super.runStarting(testCount);
        if (fTraceCalls) {
            System.out.println("runStarting(" + testCount + ")"); //$NON-NLS-1$ //$NON-NLS-2$
        }
        fTestStartTime = System.currentTimeMillis();

        // The JUnit view wants a count of the number of test methods, but unfortunately
        // "testCount" is the number of test files. So we need to count the number of
        // methods,
        // using the test class list given to us in the constructor:
        int numberOfTestMethods = countNumberOfTestMethods();
        sendMessage(JUnitViewMessageIds.TEST_RUN_START + numberOfTestMethods + " " + "v2"); //$NON-NLS-1$ //$NON-NLS-2$
    }

    protected int countNumberOfTestMethods() {
        int result = 0;
        for (String testClass : fTestClasses) {
            result += countNumberOfTestMethodsIn(testClass);
        }
        return result;
    }

    protected int countNumberOfTestMethodsIn(String testClass) {
        List<Method> methods;
        try {
            methods = testMethodsIn(testClass);
        } catch (ClassNotFoundException e) {
            return 0;
        }
        return methods.size();
    }

    @Override
    public void runAborted(ReportEntry entry) {
        super.runAborted(entry);
        if (fTraceCalls) {
            System.out.println("runAborted(ReportEntry entry)"); //$NON-NLS-1$
            printReportEntry(entry);
        }
        shutDown();
    }

    @Override
    public void runCompleted() {
        super.runCompleted();
        if (fTraceCalls) {
            System.out.println("runCompleted()"); //$NON-NLS-1$
        }
        long elapsedTime = System.currentTimeMillis() - fTestStartTime;
        sendMessage(JUnitViewMessageIds.TEST_RUN_END + elapsedTime);
        shutDown();
    }

    @Override
    public void runStopped() {
        super.runStopped();
        if (fTraceCalls) {
            System.out.println("runStopped()"); //$NON-NLS-1$
        }
        long elapsedTime = System.currentTimeMillis() - fTestStartTime;
        sendMessage(JUnitViewMessageIds.TEST_RUN_END + elapsedTime);
        shutDown();
    }

    protected String getTestIdAndName(ReportEntry entry) {
        String testName = entry.getName();
        return "" + getTestId(testName) + ',' + escapeTestName(testName);
    }

    @Override
    public void testSetStarting(ReportEntry entry) throws ReporterException {
        super.testSetStarting(entry);
        if (fTraceCalls) {
            System.out.println("testSetStarting(ReportEntry entry)"); //$NON-NLS-1$
            printReportEntry(entry);
        }

        // The JUnit protocol wants a TEST_TREE line for the "test-set" (i.e. entire
        // Java file)
        // as well as a TEST_TREE line for each test method.
        //
        // Produce the line for the Java file first:
        boolean isSuite = true;
        List<Method> methods;
        try {
            methods = testMethodsIn(entry.getName());
        } catch (ClassNotFoundException e) {
            throw new ReporterException("Class not found: " + entry.getName(), e);
        }
        int testCount = methods.size();
        String treeEntry = getTestIdAndName(entry) + ',' + isSuite + ',' + testCount;
        sendMessage(JUnitViewMessageIds.TEST_TREE + treeEntry);

        // Now emit a TEST_TREE line for each test method:
        String fileName = entry.getName();
        for (Method methodName : methods) {
            // E.g. "2,test1(com.tibco.junit_step_through.TestCase),false,1"
            isSuite = false;
            String entryName = methodName.getName() + "(" + fileName + ")";
            int id = getTestId(entryName);
            String testMethodEntry = "" + id + "," + entryName + "," + isSuite + ",1";
            sendMessage(JUnitViewMessageIds.TEST_TREE + testMethodEntry);
        }
    }

    protected List<Method> testMethodsIn(String testName) throws ClassNotFoundException {
        Class<?> testClass = Class.forName(testName);
        ArrayList<Method> result = new ArrayList<>();
        Method[] methods = testClass.getMethods();
        for (Method m : methods) {
            if (isTestMethod(m)) {
                result.add(m);
            }
        }
        return result;
    }

    protected boolean isTestMethod(Method m) {
        Annotation testAnnotation = m.getAnnotation(org.junit.Test.class);
        return testAnnotation != null;
    }

    @Override
    public void testSetAborted(ReportEntry entry) {
        super.testSetAborted(entry);
        if (fTraceCalls) {
            System.out.println("testSetAborted(ReportEntry entry)"); //$NON-NLS-1$
            printReportEntry(entry);
        }
        // The JUnit view doesn't care about this
    }

    @Override
    public void testSetCompleted(ReportEntry entry) throws ReporterException {
        super.testSetCompleted(entry);
        if (fTraceCalls) {
            System.out.println("testSetCompleted(ReportEntry entry)"); //$NON-NLS-1$
            printReportEntry(entry);
        }
        // The JUnit view doesn't care about this
    }

    @Override
    public void testSkipped(ReportEntry entry) {
        super.testSkipped(entry);
        if (fTraceCalls) {
            System.out.println("testSkipped(ReportEntry entry)"); //$NON-NLS-1$
            printReportEntry(entry);
        }
        // The JUnit view doesn't care about this
    }

    @Override
    public void testStarting(ReportEntry entry) {
        super.testStarting(entry);
        if (fTraceCalls) {
            System.out.println("testStarting(ReportEntry entry)"); //$NON-NLS-1$
            printReportEntry(entry);
        }
        String testIdAndName = getTestIdAndName(entry);
        sendMessage(JUnitViewMessageIds.TEST_START + testIdAndName);
    }

    @Override
    public void testSucceeded(ReportEntry entry) {
        super.testSucceeded(entry);
        if (fTraceCalls) {
            System.out.println("testSucceeded(ReportEntry entry)"); //$NON-NLS-1$
            printReportEntry(entry);
        }
        sendMessage(JUnitViewMessageIds.TEST_END + getTestIdAndName(entry));
    }

    @Override
    public void testError(ReportEntry entry, String stdOut, String stdErr) {
        super.testError(entry, stdOut, stdErr);
        if (fTraceCalls) {
            System.out.println("testError(ReportEntry entry, \"" + stdOut + "\", \"" + stdErr + "\")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            printReportEntry(entry);
        }
        sendMessage(JUnitViewMessageIds.TEST_ERROR + getTestIdAndName(entry));
        sendStackTrace(entry);
    }

    @Override
    public void testFailed(ReportEntry entry, String stdOut, String stdErr) {
        super.testFailed(entry, stdOut, stdErr);
        if (fTraceCalls) {
            System.out.println("testFailed(ReportEntry entry, \"" + stdOut + "\", \"" + stdErr + "\")"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
            printReportEntry(entry);
        }
        sendMessage(JUnitViewMessageIds.TEST_FAILED + getTestIdAndName(entry));
        sendStackTrace(entry);
    }

    protected void printReportEntry(ReportEntry entry) {
        printReportEntry(entry, System.out);
    }

    protected void printReportEntry(ReportEntry entry, PrintStream out) {
        if (entry == null) {
            out.println("    entry == null"); //$NON-NLS-1$
            return;
        }
        out.println("    entry.getName():       " + quoteNonNullString(entry.getName())); //$NON-NLS-1$
        out.println("         .getMessage():    " + quoteNonNullString(entry.getMessage())); //$NON-NLS-1$
        if (entry.getStackTraceWriter() != null) {
            out.println("         .getStackTraceWriter(): NOT null"); //$NON-NLS-1$
        }
    }

    private String quoteNonNullString(String s) {
        return (s != null) ? ("\"" + s + "\"") : null; //$NON-NLS-1$ //$NON-NLS-2$
    }

    @Override
    public void writeFooter(String arg0) {
        // No-op; this class doesn't do anything with the footer
    }

    @Override
    public void writeMessage(String arg0) {
        // No-op; called by the superclass' "writeFooter", but this class doesn't do
        // anything with the footer
    }

    @Override
    public void reset() {
        super.reset();
        if (fTraceCalls) {
            System.out.println("reset()");
        }
    }

    protected void sendMessage(String msg) {
        if (fWriter == null) {
            return;
        }
        if (fTraceSocketMessages) {
            System.out.println(msg);
        }
        fWriter.println(msg);
        fWriter.flush();
    }

    private void sendStackTrace(ReportEntry entry) {
        StackTraceWriter stackTraceWriter = entry.getStackTraceWriter();
        if ((fWriter == null) || (stackTraceWriter == null)) {
            return;
        }
        sendMessage(JUnitViewMessageIds.TRACE_START);
        sendMessage(stackTraceWriter.writeTraceToString());
        sendMessage(JUnitViewMessageIds.TRACE_END);
    }

    protected int getTestId(String testName) {
        Integer id = fTestNameToIdMap.get(testName);
        if (id != null) {
            return id;
        }
        id = fNextAvailableId++;
        fTestNameToIdMap.put(testName, id);
        return id;
    }

    private static String escapeTestName(String s) {
        if ((s.indexOf(',') < 0) && (s.indexOf('\\') < 0) && (s.indexOf('\r') < 0) && (s.indexOf('\n') < 0))
            return s;
        StringBuffer sb = new StringBuffer(s.length() + 10);
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == ',') {
                sb.append("\\,"); //$NON-NLS-1$
            } else if (c == '\\') {
                sb.append("\\\\"); //$NON-NLS-1$
            } else if (c == '\r') {
                if (i + 1 < s.length() && s.charAt(i + 1) == '\n') {
                    i++;
                }
                sb.append(' ');
            } else if (c == '\n') {
                sb.append(' ');
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
