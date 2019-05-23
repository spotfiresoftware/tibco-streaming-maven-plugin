/*******************************************************************************
 * Copyright (C) 2018, TIBCO Software Inc.
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
 ******************************************************************************/
package com.tibco.ep.dtm.management;

import java.util.List;
import java.util.Map;

import javax.imageio.IIOException;

/**
 * Stub DtmDeployFragmentCommand
 */
public final class DtmDeployFragmentCommand extends DtmCommand
{
    
    private boolean fail = false;
    
    public enum FragmentType
    {
        JAVA,
        STREAMBASE,
        LIVEVIEW
    }
    
    public DtmDeployFragmentCommand(
        FragmentType fragmentType,
        String target,
        DtmDestination destination)
    {
        super("deploy", target, destination);

    }

    @Override
    public void execute(
        final Map<String, String> parameters,
        IDtmProgress progress) 
        throws IllegalStateException
    {
        System.out.println("[STUB] DtmDeployFragmentCommand: execute("+parameters+")");

        if (fail) {
            throw new IllegalStateException("Invalid parameters");
        }
        if (command.equals("deploy") && target.equals("com.tibco.ep.buildmavenplugin.surefire.Runner")) {
            DtmResults results = new DtmResults();
            results.plainText = "Failures: 0, Errors: 0, Skipped: 0";
            progress.results(results);
        }
        if (command.equals("deploy") && target.equals("com.tibco.ep.buildmavenplugin.DummyTest")) {
            DtmResults results = new DtmResults();
            results.plainText = "Failures: 0, Errors: 0, Skipped: 0";
            progress.results(results);
        }
    }
    
    @Override
    public int waitForCompletion()
    {
        System.out.println("[STUB] DtmDeployFragmentCommand: waitForCompletion()");
        return 0;
    }
    
    @Override
    public final void cancel()
    {
        System.out.println("[STUB] DtmDeployFragmentCommand: cancel()");
    }
    
    public void setExecutionOptions(final List<String> options)
    {
        System.out.println("[STUB] DtmDeployFragmentCommand: setExecutionOptions("+options+")");
        long count=0;
        for (String s : options) {
            if (s.startsWith("ignoreoptionsfile")) {
               count++;
            }
        }
        this.fail=false;
        if (count > 1) {
            this.fail = true;
        }
    }
    
    public void clearExecutionOptions()
    {
    }
    
    public void setApplicationArguments(final List<String> arguments)
    {
        System.out.println("[STUB] DtmDeployFragmentCommand: setApplicationArguments("+arguments+")");
    }

}
