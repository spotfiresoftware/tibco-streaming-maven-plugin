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

package com.tibco.ep.sb.services;

import com.tibco.ep.sb.services.management.IAdminService;
import com.tibco.ep.sb.services.stubs.StubbedRuntimeServices;

import java.util.ServiceLoader;

/**
 * The main service interface
 */
public interface IRuntimeService {

    /**
     * Get a non-stubbed service implementation if possible, otherwise return the stub one.
     *
     * @param classLoader The class loader to use to load services
     * @return A runtime service implementation
     */
    static IRuntimeService getServiceImplementation(ClassLoader classLoader) {

        //  Get a non stubbed service implementation, if possible.
        //
        IRuntimeService service = null;

        for (IRuntimeService s : ServiceLoader.load(IRuntimeService.class, classLoader)) {

            if (service == null || service instanceof StubbedRuntimeServices) {
                service = s;
            }
        }

        assert service != null;
        return service;
    }

    /**
     * @return The admin service
     */
    IAdminService getAdminService();
}
