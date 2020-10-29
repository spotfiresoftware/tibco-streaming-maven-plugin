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

package com.tibco.ep.sb.services.stubs.admin;

import com.tibco.ep.sb.services.management.AbstractCommandBuilder;
import com.tibco.ep.sb.services.management.ICommand;
import com.tibco.ep.sb.services.management.IDestination;
import com.tibco.ep.sb.services.management.INotifier;

import java.util.Map;

/**
 * The command stub implementation
 */
public class Command extends Stub implements ICommand {

    private final IDestination destination;
    private final AbstractCommandBuilder builder;

    /**
     * @param builder The builder
     */
    public Command(AbstractCommandBuilder builder) {
        logMethod("constructor", builder);
        destination = builder.getDestination();
        this.builder = builder;
    }

    @Override
    public IDestination getDestination() {
        return destination;
    }

    @Override
    public int executeAndWaitForCompletion(Map<String, String> parameters, INotifier notifier) {
        assert notifier != null;
        notifier.start();
        notifier.info("command",
            "Processing command '" + builder.getCommand() + " " + builder.getTarget() +"'");
        logMethod("executeAndWaitForCompletion", builder, parameters);
        notifier.complete();
        return 0;
    }

    @Override
    public void cancel() {
        //  Nothing to do.
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
}
