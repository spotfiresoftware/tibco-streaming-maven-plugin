/*
 * Copyright (C) 2020-2024 Cloud Software Group, Inc.
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

package com.tibco.ep.buildmavenplugin.admin;

import com.tibco.ep.buildmavenplugin.ErrorHandling;
import com.tibco.ep.sb.services.management.INotifier;
import org.apache.maven.plugin.logging.Log;

import java.util.List;
import java.util.Optional;

/**
 * The command notifier implementation
 */
public class RuntimeCommandNotifier implements INotifier {

    private final Log log;
    private final String command;
    private final String header;
    private boolean running;
    private Optional<StringBuilder> results;
    private ErrorHandling errorHandling;

    /**
     * @param log The Maven logger
     * @param command The command description string
     * @param targetLocation The target location for reporting
     */
    public RuntimeCommandNotifier(Log log, String command, String targetLocation) {
        this.log = log;
        this.command = command;
        this.header = "[" + targetLocation + "] ";

        //  FIX THIS (FL): for some reason, the DtmDeploy command does not call start.
        //
        this.running = true;

        errorHandling(ErrorHandling.FAIL);
        recordOutput(false);
    }

    /**
     * @param errorHandling Error handling (to drive logging severity)
     * @return This
     */
    public RuntimeCommandNotifier errorHandling(ErrorHandling errorHandling) {
        this.errorHandling = errorHandling;
        return this;
    }

    /**
     * @param recordOutput True if the full output should be available in {@link #getOutput()}
     * @return This
     */
    public RuntimeCommandNotifier recordOutput(Boolean recordOutput) {
        this.results = recordOutput ? Optional.of(new StringBuilder()) : Optional.empty();
        return this;
    }

    /**
     * @return The command output
     */
    public String getOutput() {
        assert results.isPresent();
        return results.get().toString();
    }

    @Override
    public void start() {
        logInfo("Running \"" + command + "\"");
        running = true;
    }

    @Override
    public void cancel() {
        logError("cancelled");
    }

    @Override
    public void failed(int returnCode) {
        if (!running) {
            return;
        }

        logErrorIfNeeded("return code " + returnCode);
        results.ifPresent(sb -> sb.append(returnCode).append("\n"));
    }

    @Override
    public void complete() {
        if (!running) {
            return;
        }
        logInfo("Finished \"" + command + "\"");
        running = false;
    }

    @Override
    public void results(List<String> resultLines) {
        if (!running) {
            log.info("Received logs while not running: " + resultLines.size());
            return;
        }

        resultLines.forEach(this::logInfo);
    }

    @Override
    public void info(String source, String message) {
        if (!running) {
            return;
        }
        log.info("[" + source + "] " + message);
        results.ifPresent(sb -> sb.append(message).append("\n"));
    }

    @Override
    public void error(String source, String message) {
        if (!running) {
            return;
        }
        log.error("[" + source + "] " + message);
        results.ifPresent(sb -> sb.append(message).append("\n"));
    }

    private void logErrorIfNeeded(String message) {
        if (errorHandling == ErrorHandling.FAIL) {
            logError(message);
        } else {
            logInfo(message + " [ignored]");
        }
    }

    private void logInfo(String message) {
        log.info(header + message);
    }

    private void logError(String message) {
        log.error(header + message);
    }
}
