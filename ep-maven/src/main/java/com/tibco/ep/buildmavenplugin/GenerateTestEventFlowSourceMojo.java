/*
 * Copyright (C) 2021-2024 Cloud Software Group, Inc.
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

package com.tibco.ep.buildmavenplugin;

import com.tibco.ep.sb.services.build.BuildTarget;
import org.apache.maven.plugins.annotations.Mojo;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_TEST_SOURCES;


/**
 * <p>Typecheck EventFlows and generate java sources</p>
 *
 * <p>The plugin will scan the configured EventFlow source and test directories and typecheck/build
 * them in the dependency order, reporting any typecheck failure.</p>
 *
 * <p>It does not build the generated java sources, this is done using a standard maven compiler
 * plugin execution.</p>
 *
 * <p>See {@link GenerateEventFlowSourceMojo} for more information (only differs from the list of
 * scanned directories).</p>
 *
 */
@Mojo(name = "generate-test-eventflow", defaultPhase = GENERATE_TEST_SOURCES, threadSafe = false)
public class GenerateTestEventFlowSourceMojo extends BaseGenerateMojo {

    /**
     * Public constructor
     */
    public GenerateTestEventFlowSourceMojo() {
        super(BuildTarget.TEST);
    }
}
