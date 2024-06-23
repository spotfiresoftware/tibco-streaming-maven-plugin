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

package com.tibco.ep.buildmavenplugin;

import com.tibco.ep.sb.services.build.BuildTarget;
import org.apache.maven.plugins.annotations.Mojo;

import static org.apache.maven.plugins.annotations.LifecyclePhase.GENERATE_SOURCES;


/**
 * <p>Typecheck EventFlows and generate java sources</p>
 *
 * <p>The plugin will scan the configured EventFlow source directories and typecheck/build them in the
 * dependency order, reporting any typecheck failure.</p>
 *
 * <p>It does not build the generated java sources, this is done using a standard maven compiler
 * plugin execution.</p>
 *
 * <p>The plugin keeps track of individual SBAPP/SBINT dependencies and will trigger or skip a
 * a rebuild accordingly.</p>
 *
 * <p>SBAPPs and SBINTs have public interfaces and private implementation details. The public
 * interface is composed of anything that another SBAPP/SBINT can import. Schemas, Streams, QueryTable,
 * Constants are part of the public interface. Everything else (Module operators, etc) is private.</p>
 *
 * <p>If an EventFlow A depends on another EventFlow B and A and B are in the same fragment, then,
 * upon a change on B's public interface, the plugin will rebuild B, then A. Upon a change on B's
 * implementation only, the plugin will only rebuild B.</p>
 *
 * <p>If an EventFlow A depends on another EventFlow B and A and B are in different fragments, the
 * plugin will only trigger a rebuild of A if the rebuild of B has happened and is installed in the
 * maven repository (i.e. if the fragment containing B has been rebuilt successfully).</p>
 *
 * <p>EventFlow builds are shared between Studio and the Maven command line. A fragment built using
 * the command line will not get rebuilt by Studio and vice-versa. A Maven clean or the removal of
 * the default *target* directory will force a rebuild on the next call.</p>
 *
 */
@Mojo(name = "generate-main-eventflow", defaultPhase = GENERATE_SOURCES, threadSafe = false)
public class GenerateEventFlowSourceMojo extends BaseGenerateMojo {

    /**
     * Public constructor
     */
    public GenerateEventFlowSourceMojo() {
        super(BuildTarget.MAIN);
    }
}
