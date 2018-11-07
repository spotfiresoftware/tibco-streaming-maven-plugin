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

import java.util.Map;

/**
 * Stub DtmCommand
 */
public class DtmCommand
{
    protected final String command;
    protected final String target;
    protected final DtmDestination destination;

    public DtmCommand(
        final String command, 
        final String target, 
        final DtmDestination destination)
    {
        System.out.println("[STUB] DtmCommand: constructor("+command+","+target+")");
        this.command = command;
        this.target = target;
        this.destination = destination;
    }
    
    public void execute(
        Map<String,String> parameters,
        IDtmProgress progress)
        throws IllegalStateException, IllegalArgumentException
    {
        System.out.println("[STUB] DtmCommand: execute("+parameters+") "+destination.name);
        
        if (command.equals("display") && target.equals("node")) {
            DtmResults results = new DtmResults();
            results.plainText = "Started";
            progress.results(results);
        }
        if (command.equals("remove") && target.equals("node")) {
            DtmResults results = new DtmResults();
            results.plainText = "Node removed";
            progress.results(results);
        }

    }
    
    public int waitForCompletion()
    {
        System.out.println("[STUB] DtmCommand: waitForCompletion()");
        return 0;
    }
    
    public void cancel()
    {
        System.out.println("[STUB] DtmCommand: cancel()");
    }
    
}
