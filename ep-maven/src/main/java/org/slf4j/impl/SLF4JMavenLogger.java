/*
 * Copyright (C) 2020, TIBCO Software Inc.
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

package org.slf4j.impl;

import org.apache.maven.plugin.logging.Log;
import org.slf4j.event.Level;
import org.slf4j.helpers.MarkerIgnoringBase;
import org.slf4j.helpers.MessageFormatter;

import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;

/**
 * SLF4J logger, calling into a MOJO's Log.
 */
public class SLF4JMavenLogger extends MarkerIgnoringBase {

    private static final AtomicReference<Optional<Log>> MAVEN_LOGGER =
        new AtomicReference<>(Optional.empty());

    /**
     * Configure hte SLF4JMavenLoggers to use a specific Log handle as the backend
     *
     * @param logger The logger to use
     */
    public static void setMavenLogger(Log logger) {
        MAVEN_LOGGER.set(Optional.of(logger));
    }

    private boolean isEnabled(Level level) {
        Optional<Log> optionalLog = MAVEN_LOGGER.get();
        if (optionalLog.isPresent()) {
            Log log = optionalLog.get();

            switch (level) {

                case ERROR:
                    return log.isErrorEnabled();
                case WARN:
                    return log.isWarnEnabled();
                case INFO:
                    return log.isInfoEnabled();
                case DEBUG:
                case TRACE:
                    //  Maven has no "trace" concept.
                    //
                    return log.isDebugEnabled();
                default:
                    throw new IllegalArgumentException("Unknown level: " + level);
            }
        }

        //  No optional Maven log, log to STDOUT INFO and higher.
        //
        return level == Level.ERROR || level == Level.WARN || level == Level.INFO;
    }

    private void logFormattedMessage(Level level, Supplier<String> supplier) {
        logFormattedMessage(level, supplier, null);
    }

    private void logFormattedMessage(Level level, Supplier<String> supplier, Throwable throwable) {
        Optional<Log> optionalLog = MAVEN_LOGGER.get();
        if (optionalLog.isPresent()) {

            Log log = optionalLog.get();
            switch (level) {

                case ERROR:
                    log.error(supplier.get(), throwable);
                    break;
                case WARN:
                    log.warn(supplier.get(), throwable);
                    break;
                case INFO:
                    log.info(supplier.get(), throwable);
                    break;
                case DEBUG:
                case TRACE:
                    //  Maven has no "trace" concept
                    //
                    log.debug(supplier.get(), throwable);
                    break;
            }

        } else {

            //  To STDOUT.
            //
            System.out.println("[" + level + "] " + supplier.get());
            if (throwable != null) {
                throwable.printStackTrace();
            }
        }
    }

    @Override
    public boolean isTraceEnabled() {
        return isEnabled(Level.TRACE);
    }

    @Override
    public void trace(String msg) {
        if (isTraceEnabled()) {
            logFormattedMessage(Level.TRACE, () -> msg);
        }
    }

    @Override
    public void trace(String format, Object arg) {
        if (isTraceEnabled()) {
            logFormattedMessage(Level.TRACE,
                () -> MessageFormatter.format(format, arg).getMessage());
        }
    }

    @Override
    public void trace(String format, Object arg1, Object arg2) {
        if (isTraceEnabled()) {
            logFormattedMessage(Level.TRACE,
                () -> MessageFormatter.format(format, arg1, arg2).getMessage());
        }
    }

    @Override
    public void trace(String format, Object... arguments) {
        if (isTraceEnabled()) {
            logFormattedMessage(Level.TRACE,
                () -> MessageFormatter.arrayFormat(format, arguments).getMessage());
        }
    }

    @Override
    public void trace(String msg, Throwable t) {
        if (isTraceEnabled()) {
            logFormattedMessage(Level.TRACE, () -> msg, t);
        }
    }

    @Override
    public boolean isDebugEnabled() {
        return isEnabled(Level.DEBUG);
    }

    @Override
    public void debug(String msg) {
        if (isDebugEnabled()) {
            logFormattedMessage(Level.DEBUG, () -> msg);
        }
    }

    @Override
    public void debug(String format, Object arg) {
        if (isDebugEnabled()) {
            logFormattedMessage(Level.DEBUG,
                () -> MessageFormatter.format(format, arg).getMessage());
        }
    }

    @Override
    public void debug(String format, Object arg1, Object arg2) {
        if (isDebugEnabled()) {
            logFormattedMessage(Level.DEBUG,
                () -> MessageFormatter.format(format, arg1, arg2).getMessage());
        }
    }

    @Override
    public void debug(String format, Object... arguments) {
        if (isDebugEnabled()) {
            logFormattedMessage(Level.DEBUG,
                () -> MessageFormatter.arrayFormat(format, arguments).getMessage());
        }
    }

    @Override
    public void debug(String msg, Throwable t) {
        if (isDebugEnabled()) {
            logFormattedMessage(Level.DEBUG, () -> msg, t);
        }
    }

    @Override
    public boolean isInfoEnabled() {
        return isEnabled(Level.INFO);
    }

    @Override
    public void info(String msg) {
        if (isInfoEnabled()) {
            logFormattedMessage(Level.INFO, () -> msg);
        }
    }

    @Override
    public void info(String format, Object arg) {
        if (isInfoEnabled()) {
            logFormattedMessage(Level.INFO,
                () -> MessageFormatter.format(format, arg).getMessage());
        }
    }

    @Override
    public void info(String format, Object arg1, Object arg2) {
        if (isInfoEnabled()) {
            logFormattedMessage(Level.INFO,
                () -> MessageFormatter.format(format, arg1, arg2).getMessage());
        }
    }

    @Override
    public void info(String format, Object... arguments) {
        if (isInfoEnabled()) {
            logFormattedMessage(Level.INFO,
                () -> MessageFormatter.arrayFormat(format, arguments).getMessage());
        }
    }

    @Override
    public void info(String msg, Throwable t) {
        if (isInfoEnabled()) {
            logFormattedMessage(Level.INFO, () -> msg, t);
        }
    }

    @Override
    public boolean isWarnEnabled() {
        return isEnabled(Level.WARN);
    }

    @Override
    public void warn(String msg) {
        if (isWarnEnabled()) {
            logFormattedMessage(Level.WARN, () -> msg);
        }
    }

    @Override
    public void warn(String format, Object arg) {
        if (isWarnEnabled()) {
            logFormattedMessage(Level.WARN,
                () -> MessageFormatter.format(format, arg).getMessage());
        }
    }

    @Override
    public void warn(String format, Object... arguments) {
        if (isWarnEnabled()) {
            logFormattedMessage(Level.WARN,
                () -> MessageFormatter.arrayFormat(format, arguments).getMessage());
        }
    }

    @Override
    public void warn(String format, Object arg1, Object arg2) {
        if (isWarnEnabled()) {
            logFormattedMessage(Level.WARN,
                () -> MessageFormatter.format(format, arg1, arg2).getMessage());
        }
    }

    @Override
    public void warn(String msg, Throwable t) {
        if (isWarnEnabled()) {
            logFormattedMessage(Level.WARN, () -> msg, t);
        }
    }

    @Override
    public boolean isErrorEnabled() {
        return isEnabled(Level.ERROR);
    }

    @Override
    public void error(String msg) {
        if (isErrorEnabled()) {
            logFormattedMessage(Level.ERROR, () -> msg);
        }
    }

    @Override
    public void error(String format, Object arg) {
        if (isErrorEnabled()) {
            logFormattedMessage(Level.ERROR,
                () -> MessageFormatter.format(format, arg).getMessage());
        }
    }

    @Override
    public void error(String format, Object arg1, Object arg2) {
        if (isErrorEnabled()) {
            logFormattedMessage(Level.ERROR,
                () -> MessageFormatter.format(format, arg1, arg2).getMessage());
        }
    }

    @Override
    public void error(String format, Object... arguments) {
        if (isErrorEnabled()) {
            logFormattedMessage(Level.ERROR,
                () -> MessageFormatter.arrayFormat(format, arguments).getMessage());
        }
    }

    @Override
    public void error(String msg, Throwable t) {
        if (isErrorEnabled()) {
            logFormattedMessage(Level.ERROR, () -> msg, t);
        }
    }
}
