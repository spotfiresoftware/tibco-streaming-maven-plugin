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
import com.tibco.ep.sb.services.management.AbstractCommandBuilder;
import com.tibco.ep.sb.services.management.ICommand;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * An administrative command runner
 */
public class RuntimeCommandRunner {

    private final Log log;
    private final AbstractCommandBuilder builder;
    private final String shortLocation;
    private final String longLocation;

    private boolean wait;
    private ErrorHandling errorHandling;
    private boolean recordOutput;
    private Optional<Consumer<ICommand>> shutdownHook;
    private ErrorHandler errorHandler;

    private boolean shuttingDown;
    private Thread shutdownHookThread;

    /**
     * @param log           The Maven log handle
     * @param builder       The command builder
     * @param shortLocation Short location for logging
     * @param longLocation  Long location for error reporting
     */
    public RuntimeCommandRunner(Log log, AbstractCommandBuilder builder, String shortLocation, String longLocation) {
        this.log = log;
        this.builder = builder;
        this.shortLocation = shortLocation;
        this.longLocation = longLocation;

        wait = true;
        errorHandling = ErrorHandling.FAIL;
        recordOutput = false;
        shutdownHook = Optional.empty();
        errorHandler = (runner, resultCode) -> {
            throw new MojoExecutionException(
                "Command: " + runner.builder.getCommand() + " " + runner.builder.getTarget()
                    + " failed: " + runner.longLocation + " error code " + resultCode);
        };

        shutdownHookThread = null;
        shuttingDown = false;
    }

    /**
     * @return The command description (command + target)
     */
    private String getDescription() {
        return builder.getCommand() + " " + builder.getTarget();
    }

    /**
     * Should the runner wait for command completion ?
     *
     * @param wait True if the runner needs to wait
     * @return This
     */
    public RuntimeCommandRunner wait(boolean wait) {
        this.wait = wait;
        return this;
    }

    /**
     * Should the runner fail upon error ?
     *
     * @param errorHandling The error handling for the command
     * @return This
     */
    public RuntimeCommandRunner errorHandling(ErrorHandling errorHandling) {
        this.errorHandling = errorHandling;
        return this;
    }

    /**
     * Should the runner record output
     *
     * @param recordOutput True if the runner should record output
     * @return This
     */
    public RuntimeCommandRunner recordOutput(boolean recordOutput) {
        this.recordOutput = recordOutput;
        return this;
    }

    /**
     * Configure a shutdown hook
     *
     * @param shutdownHook The shutdown hook
     * @return This
     */
    public RuntimeCommandRunner shutdownHook(Consumer<ICommand> shutdownHook) {
        this.shutdownHook = Optional.of(shutdownHook);
        return this;
    }

    /**
     * Configure an error hook
     *
     * @param errorHandler The error hook
     * @return This
     */
    public RuntimeCommandRunner onError(ErrorHandler errorHandler) {
        this.errorHandler = errorHandler;
        return this;
    }

    /**
     * Run the command
     *
     * @return This
     * @throws MojoExecutionException An execution failure happened
     */
    public RuntimeCommandNotifier run() throws MojoExecutionException {
        return run(new HashMap<>());
    }

    /**
     * Run the command with parameters
     *
     * @param parameters The parameters
     * @return This
     * @throws MojoExecutionException An execution failure happened
     */
    public RuntimeCommandNotifier run(Map<String, String> parameters) throws MojoExecutionException {

        try {
            ICommand command = builder.build();

            //  Install shutdown hook if needed.
            //
            if (shutdownHook.isPresent()) {

                //  We will always fail on error if we have a shutdown hook to cleanup failures.
                //
                assert errorHandling == ErrorHandling.FAIL;

                shutdownHookThread = new Thread(() -> {
                    shuttingDown = true;
                    shutdownHook.get().accept(command);
                });
                Runtime.getRuntime().addShutdownHook(shutdownHookThread);
            }

            RuntimeCommandNotifier notifier = new RuntimeCommandNotifier(log, getDescription(), shortLocation);
            notifier.recordOutput(recordOutput).errorHandling(errorHandling);

            command.execute(parameters, notifier);

            if (!wait) {
                return notifier;
            }

            int resultCode = command.waitForCompletion();

            if (shuttingDown) {
                assert shutdownHook.isPresent();
                throw new MojoExecutionException(
                    "command: " + getDescription() + " was aborted - attempting to clean up");
            }

            if (resultCode != 0 && errorHandling == ErrorHandling.FAIL) {
                errorHandler.onError(this, resultCode);
            }

            removeShutdownHook();

            return notifier;

        } catch (IllegalArgumentException iae) {

            removeShutdownHook();

            throw new MojoExecutionException(
                "Illegal argument while using administrative API, command: "
                    + getDescription() + " on " + longLocation, iae);
        }
    }

    /**
     * Remove the shutdown hook
     */
    public void removeShutdownHook() {
        if (shutdownHookThread != null) {
            Runtime.getRuntime().removeShutdownHook(shutdownHookThread);
        }
        shutdownHookThread = null;
    }
}
