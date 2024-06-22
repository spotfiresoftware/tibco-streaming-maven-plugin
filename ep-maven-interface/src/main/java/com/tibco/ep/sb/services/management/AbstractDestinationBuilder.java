/*
 * Copyright © 2020-2024. Cloud Software Group, Inc.
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
 * The base class for destination builder
 */
public abstract class AbstractDestinationBuilder extends AbstractBaseBuilder {

    private final IContext context;
    private final List<String> discoveryHosts = new ArrayList<>();
    private String name;
    private String userName;
    private String password;

    /**
     * @param adminService The admin service
     * @param context      The context
     */
    public AbstractDestinationBuilder(IRuntimeAdminService adminService, IContext context) {
        super(adminService);
        this.context = context;
    }

    /**
     * @return The destination name
     */
    public String getName() {
        return name;
    }

    /**
     * @return The user name
     */
    public String getUserName() {
        return userName;
    }

    /**
     * @return The password
     */
    public String getPassword() {
        return password;
    }

    /**
     * @return The context
     */
    public IContext getContext() {
        return context;
    }

    /**
     * @return The discovery hosts
     */
    public List<String> getDiscoveryHosts() {
        return discoveryHosts;
    }

    /**
     * @param name The destination name
     * @return This
     */
    public AbstractDestinationBuilder withName(String name) {
        this.name = name;
        return this;
    }

    /**
     * @param userName The user name
     * @return This
     */
    public AbstractDestinationBuilder withUserName(String userName) {
        this.userName = userName;
        return this;
    }

    /**
     * @param password The password
     * @return This
     */
    public AbstractDestinationBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    /**
     * Add a destination host
     *
     * @param host The host to add
     * @return This
     */
    public AbstractDestinationBuilder withAdditionalDiscoveryHost(String host) {
        this.discoveryHosts.add(host);
        return this;
    }

    /**
     * Build the destination.
     * @return a new destination
     */
    public abstract IDestination build();

    @Override
    public String toString() {
        return "{" +
            "context=" + context +
            ", name='" + name + '\'' +
            ", userName='" + userName + '\'' +
            ", password='" + password + '\'' +
            ", discoveryHosts=" + discoveryHosts +
            "}";
    }
}
