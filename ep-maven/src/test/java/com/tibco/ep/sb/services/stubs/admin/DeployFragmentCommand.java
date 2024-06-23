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

import com.tibco.ep.sb.services.management.AbstractDeployFragmentCommandBuilder;
import com.tibco.ep.sb.services.management.FragmentType;
import com.tibco.ep.sb.services.management.IDeployFragmentCommand;
import com.tibco.ep.sb.services.management.IDestination;
import com.tibco.ep.sb.services.management.INotifier;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * The deploy fragment stub implementation
 */
public class DeployFragmentCommand extends Command implements IDeployFragmentCommand {

    private final boolean fail;

    /**
     * @param builder The builder
     */
    public DeployFragmentCommand(AbstractDeployFragmentCommandBuilder builder) {
        super(builder);

        List<String> options = builder.getExecutionOptions();

        long count = 0;
        for (String s : options) {
            if (s.startsWith("ignoreoptionsfile")) {
                count++;
            }
        }

        this.fail = (count > 1);

        //  Setup test completion.
        //
        for (String target : Arrays.asList(
            "com.tibco.ep.buildmavenplugin.surefire.Runner",
            "com.tibco.ep.buildmavenplugin.DummyTest")) {

            upon("deploy", target,
                (params, notifier) -> {
                    notifier.info("", "Failures: 0, Errors: 0, Skipped: 0");
                });
        }
    }

    @Override
    public void execute(Map<String, String> parameters, INotifier notifier) {
        if (fail) {
            throw new IllegalStateException("Invalid parameters");
        }
        super.execute(parameters, notifier);
    }

    /**
     * The builder class
     */
    static class Builder extends AbstractDeployFragmentCommandBuilder {

        /**
         * @param destination  The destination
         * @param fragmentType The fragment type
         * @param target       The target
         */
        public Builder(IDestination destination, FragmentType fragmentType, String target) {
            super(destination, fragmentType, target);
        }

        @Override
        public IDeployFragmentCommand build() {
            return new DeployFragmentCommand(this);
        }
    }
}
