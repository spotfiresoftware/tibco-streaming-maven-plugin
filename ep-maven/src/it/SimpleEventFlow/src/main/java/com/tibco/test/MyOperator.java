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

package com.tibco.test;

import com.streambase.sb.*;
import com.streambase.sb.operator.*;

/**
 * A simple operator used in the flow
 */
public class MyOperator extends Operator implements Parameterizable {

    public static final long serialVersionUID = 1603804847032L;
    private static final String DISPLAY_NAME = "MyOperator";

    private static final int NB_INPUT_PORTS = 1;
    private static final int INPUT_PORT_INDEX = 0;

    private static final int NB_OUTPUT_PORTS = 1;
    private static final int OUTPUT_PORT_INDEX = 0;

    private Schema outputSchema;

    /**
     * Operator constructor
     */
    public MyOperator() {
        super();
        setPortHints(NB_INPUT_PORTS, NB_OUTPUT_PORTS);
        setDisplayName(DISPLAY_NAME);
        setShortDisplayName(this.getClass().getSimpleName());
    }

    @Override
    public void typecheck() throws TypecheckException {
        requireInputPortCount(NB_INPUT_PORTS);
        setOutputSchema(INPUT_PORT_INDEX, getInputSchema(OUTPUT_PORT_INDEX));
    }

    @Override
    public void processTuple(int inputPort, Tuple tuple) throws StreamBaseException {
        if (getLogger().isInfoEnabled()) {
            getLogger().info("operator processing a tuple at input port" + inputPort);
        }

        if (inputPort > 0) {
            getLogger().info("operator skipping tuple at input port" + inputPort);
            return;
        }

        // Create the tuple for the output schema and intialize it.
        //
        Tuple out = outputSchema.createTuple();
        for (int i = 0; i < out.getSchema().getFieldCount(); ++i) {
            out.setField(i, tuple.getField(i));
        }

        //  Send the tuple out.
        //
        sendOutput(OUTPUT_PORT_INDEX, out);
    }

    @Override
    public void init() throws StreamBaseException {
        super.init();
        outputSchema = getRuntimeOutputSchema(OUTPUT_PORT_INDEX);
    }
}