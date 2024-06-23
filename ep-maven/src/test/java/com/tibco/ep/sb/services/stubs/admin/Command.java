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

package com.tibco.ep.sb.services.stubs.admin;

import com.tibco.ep.sb.services.management.AbstractCommandBuilder;
import com.tibco.ep.sb.services.management.ICommand;
import com.tibco.ep.sb.services.management.IDestination;
import com.tibco.ep.sb.services.management.INotifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * The command stub implementation
 */
public class Command extends Stub implements ICommand {

    private final IDestination destination;
    private final AbstractCommandBuilder builder;
    private final List<Trigger> triggers = new ArrayList<>();

    /**
     * @param builder The builder
     */
    public Command(AbstractCommandBuilder builder) {
        this(builder, builder.getDestination());
    }

    /**
     * @param builder     The builder
     * @param destination The destination
     */
    Command(AbstractCommandBuilder builder, IDestination destination) {
        logMethod("constructor", builder, destination);
        this.destination = destination;
        this.builder = builder;

        upon("remove", "node",
            (params, notifier) -> notifier.info("", "Node removed"));
        upon("display", "node",
            (params, notifier) -> notifier.info("", "Started"));
    }

    @Override
    public IDestination getDestination() {
        return destination;
    }

    /**
     * Add a trigger upon a command/target couple
     *
     * @param command The command
     * @param target  The target
     * @param action  The action to perform
     */
    void upon(String command, String target, BiConsumer<Map<String, String>, INotifier> action) {
        triggers.add(new Trigger(command, target, action));
    }

    @Override
    public void execute(Map<String, String> parameters, INotifier notifier) {
        assert notifier != null;

        logMethod("executeAndWaitForCompletion", builder, parameters);

        notifier.start();
        notifier.info("command",
            "Processing command '" + builder.getCommand() + " " + builder.getTarget() + "'");

        triggers.stream()
            .filter(trigger -> trigger.matches(builder.getCommand(), builder.getTarget()))
            .forEach(trigger -> trigger.process(parameters, notifier));

        notifier.complete();
    }

    @Override
    public int waitForCompletion() {
        logMethod("waitForCompletion");
        return 0;
    }

    @Override
    public void cancel() {
        logMethod("cancel");
    }

    /**
     * The builder class
     */
    static class Builder extends AbstractCommandBuilder {

        @Override
        public ICommand build() {
            return new Command(this);
        }
    }

    private static class Trigger {
        private final String command;
        private final String target;
        private final BiConsumer<Map<String, String>, INotifier> action;

        private Trigger(String command, String target, BiConsumer<Map<String, String>, INotifier> action) {
            this.command = command;
            this.target = target;
            this.action = action;
        }

        private boolean matches(String command, String target) {
            return this.command.equals(command) && this.target.equals(target);
        }

        private void process(Map<String, String> parameters, INotifier notifier) {
            //  The command and target match. Trigger the notification.
            //
            action.accept(parameters, notifier);
        }
    }
}
