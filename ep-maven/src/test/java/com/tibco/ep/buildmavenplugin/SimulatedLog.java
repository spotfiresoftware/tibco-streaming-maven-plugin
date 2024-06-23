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

import org.apache.maven.plugin.logging.Log;
import org.slf4j.event.Level;

/**
 * Simulated maven logger to catch the actual output
 */
public class SimulatedLog implements Log {
    private final boolean verbose;
    private StringBuilder debugText;
    private StringBuilder infoText;
    private StringBuilder errorText;
    private StringBuilder warnText;

    /**
     * Constructor
     *
     * @param verbose true to log all output
     */
    SimulatedLog(boolean verbose) {
        this.verbose = verbose;
        reset();
    }

    /**
     * Reset stored stings
     */
    public void reset() {
        debugText = new StringBuilder();
        infoText = new StringBuilder();
        errorText = new StringBuilder();
        warnText = new StringBuilder();
    }

    /**
     * Get stored debug log
     *
     * @return debug log
     */
    public String getDebugLog() {
        return debugText.toString();
    }

    /**
     * Get stored info log
     *
     * @return info log
     */
    public String getInfoLog() {
        return infoText.toString();
    }

    /**
     * Get stored error log
     *
     * @return error log
     */
    public String getErrorLog() {
        return errorText.toString();
    }

    /**
     * Get stored warn log
     *
     * @return warn log
     */
    public String getWarnLog() {
        return warnText.toString();
    }

    private void log(Level level, CharSequence msg, Throwable error) {

        if (verbose) {
            //  DO NOT use SLF4J here, this will cause recursion because of the SLF4J Maven binding.
            //
            System.out.println("Verbose SimulatedLog: " + level + ": " + msg
                + (error != null ? " error = " + error : ""));
        }

        StringBuilder builder = null;
        switch (level) {

            case ERROR:
                builder = errorText;
                break;
            case WARN:
                builder = warnText;
                break;
            case INFO:
                builder = infoText;
                break;
            case DEBUG:
                builder = debugText;
                break;
            case TRACE:
            default:
                throw new IllegalArgumentException("Bad level: " + level);
        }

        if (msg != null) {
            builder.append(msg).append("\n");
        }
        if (error != null) {
            builder.append(error).append("\n");
        }

    }

    @Override
    public void debug(CharSequence content) {
        log(Level.DEBUG, content, null);
    }

    @Override
    public void debug(Throwable error) {
        log(Level.DEBUG, null, error);
    }

    @Override
    public void debug(CharSequence content, Throwable error) {
        log(Level.DEBUG, content, error);
    }

    @Override
    public void info(CharSequence content) {
        log(Level.INFO, content, null);
    }

    @Override
    public void info(Throwable error) {
        log(Level.INFO, null, error);
    }

    @Override
    public void info(CharSequence content, Throwable error) {
        log(Level.INFO, content, error);
    }

    @Override
    public void warn(CharSequence content) {
        log(Level.WARN, content, null);
    }

    @Override
    public void warn(Throwable error) {
        log(Level.WARN, null, error);
    }

    @Override
    public void warn(CharSequence content, Throwable error) {
        log(Level.WARN, content, error);
    }

    @Override
    public void error(CharSequence content) {
        log(Level.ERROR, content, null);
    }

    @Override
    public void error(Throwable error) {
        log(Level.ERROR, null, error);
    }

    @Override
    public void error(CharSequence content, Throwable error) {
        log(Level.ERROR, content, error);
    }

    @Override
    public boolean isDebugEnabled() {
        return true;
    }

    @Override
    public boolean isErrorEnabled() {
        return true;
    }

    @Override
    public boolean isInfoEnabled() {
        return true;
    }

    @Override
    public boolean isWarnEnabled() {
        return true;
    }
}
