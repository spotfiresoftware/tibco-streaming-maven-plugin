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

package com.tibco.ep.sb.services.management;

import java.util.ArrayList;
import java.util.List;

/**
 * The base class for deploy fragment command builder
 */
public abstract class AbstractDeployFragmentCommandBuilder extends AbstractCommandBuilder {

    private final FragmentType fragmentType;
    private final List<String> executionOptions = new ArrayList<>();
    private final List<String> applicationArguments = new ArrayList<>();

    /**
     * @param destination The destination
     * @param fragmentType The fragment type
     * @param target The target
     */
    public AbstractDeployFragmentCommandBuilder(IDestination destination, FragmentType fragmentType, String target) {
        this.fragmentType = fragmentType;
        withDestination(destination);
        withCommand("deploy").withTarget(target);
    }

    /**
     * @return The fragment type
     */
    public FragmentType getFragmentType() {
        return fragmentType;
    }

    /**
     * @return The execution options
     */
    public List<String> getExecutionOptions() {
        return executionOptions;
    }

    /**
     * @return The application arguments
     */
    public List<String> getApplicationArguments() {
        return applicationArguments;
    }

    /**
     * @param executionOptions The execution options
     * @return This
     */
    public AbstractDeployFragmentCommandBuilder withExecutionOptions(List<String> executionOptions) {
        this.executionOptions.clear();
        this.executionOptions.addAll(executionOptions);
        return this;
    }

    /**
     * @param applicationArguments The application arguments
     * @return This
     */
    public AbstractDeployFragmentCommandBuilder withApplicationArguments(List<String> applicationArguments) {
        this.applicationArguments.clear();
        this.applicationArguments.addAll(applicationArguments);
        return this;
    }

    @Override
    public AbstractDeployFragmentCommandBuilder withDestination(IDestination destination) {
        super.withDestination(destination);
        return this;
    }

    /**
     * @return A new deploy fragment command
     */
    @Override
    public abstract IDeployFragmentCommand build();

    @Override
    public String toString() {
        return "{" +
            "fragmentType=" + fragmentType +
            ", executionOptions=" + executionOptions +
            ", applicationArguments=" + applicationArguments +
            "} " + super.toString();
    }
}
