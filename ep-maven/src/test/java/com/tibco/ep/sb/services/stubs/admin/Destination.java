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

import com.tibco.ep.sb.services.management.AbstractDestinationBuilder;
import com.tibco.ep.sb.services.management.IRuntimeAdminService;
import com.tibco.ep.sb.services.management.IContext;
import com.tibco.ep.sb.services.management.IDestination;

/**
 * The destination stub implementation
 */
public class Destination extends ServiceAwareStub implements IDestination {

    private final AbstractDestinationBuilder builder;
    private int discoveryPort = 0;

    /**
     * @param builder The builder
     */
    Destination(AbstractDestinationBuilder builder) {
        super(builder.getAdminService());
        this.builder = builder;

        //  Also convers logging node constructor.
        //
        this.logMethod("constructor", builder);
    }

    /**
     * A destination no-op constructor
     */
    Destination() {
        super(null);
        builder = null;
        this.logMethod("constructor");
    }

    /**
     * @return The destination name
     */
    String getName() {
        return builder.getName();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + (builder != null ? builder.toString() : "");
    }

    @Override
    public void setDiscoveryPort(int discoveryPort) {
        this.discoveryPort = discoveryPort;
    }

    /**
     * The builder
     */
    static class Builder extends AbstractDestinationBuilder {

        /**
         * @param service The service
         * @param context The context
         */
        Builder(IRuntimeAdminService service, IContext context) {
            super(service, context);
        }

        @Override
        public IDestination build() {
            return new Destination(this);
        }
    }
}
