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

package com.tibco.ep.sb.services.stubs.build;

import com.tibco.ep.sb.services.build.BuildParameters;
import com.tibco.ep.sb.services.build.BuildTarget;
import com.tibco.ep.sb.services.build.IBuildResultHandler;
import com.tibco.ep.sb.services.build.IRuntimeBuildService;

import java.nio.file.Paths;
import java.util.Optional;

/**
 * A test {@link IRuntimeBuildService}
 */
public class RuntimeBuildService implements IRuntimeBuildService {

    @Override
    public void build(String name, BuildTarget buildTarget, BuildParameters parameters, IBuildResultHandler buildResultHandler) {

        System.out.println("[STUB] build: " + name + " " + buildTarget + " " + parameters);

        buildResultHandler.onBuildResult(
            "com.tibco.Module",
            Paths.get("com", "tibco", "Module.sbapp"),
            Optional.empty());
    }
}
