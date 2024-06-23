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

package com.tibco.ep.sb.services.stubs.build;

import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;

import com.tibco.ep.sb.services.build.BuildExceptionDetails;
import com.tibco.ep.sb.services.build.BuildParameters;
import com.tibco.ep.sb.services.build.BuildResult;
import com.tibco.ep.sb.services.build.BuildTarget;
import com.tibco.ep.sb.services.build.IBuildNotifier;
import com.tibco.ep.sb.services.build.IRuntimeBuildService;

/**
 * A test {@link IRuntimeBuildService}
 */
public class RuntimeBuildService implements IRuntimeBuildService {

    @Override
    public void build(String name, BuildTarget buildTarget, BuildParameters parameters, IBuildNotifier notifier) {

        System.out.println("[STUB] build: " + name + " " + buildTarget + " " + parameters);

        notifier.onBuildStarted(2);

        notifier.onSkipped("Skipped", ".sbapp");

        notifier.onStarted("MyModule", ".sbapp");
        notifier.onCompleted(new BuildResult()
            .withElapsedTimeMillis(1234)
            .withEntityName("MyModule.sbapp")
            .withEntityPath(Paths.get("com", "tibco", "Module.sbapp"))
            .withException(new RuntimeException("here")));

        notifier.onBuildCompleted();
    }

    @Override
    public List<BuildExceptionDetails> getDetails(Exception exception) {
        return Collections.singletonList(new BuildExceptionDetails()
            .withShortMessage(exception.getMessage())
            .withLocation("location"));
    }
}
