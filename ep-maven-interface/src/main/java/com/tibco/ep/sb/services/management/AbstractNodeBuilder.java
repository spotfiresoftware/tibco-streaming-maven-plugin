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

package com.tibco.ep.sb.services.management;

/**
 * Base class for node builder
 */
public abstract class AbstractNodeBuilder extends AbstractDestinationBuilder  {

    private String hostName;
    private Integer administrationPort;

    /**
     * @param service The admin service
     * @param context The context
     * @param name The node name
     */
    public AbstractNodeBuilder(IRuntimeAdminService service, IContext context, String name) {
        super(service, context);
        withName(name);
    }

    /**
     * @return The host name
     */
    public String getHostName() {
        return hostName;
    }

    /**
     * @return The administration port
     */
    public Integer getAdministrationPort() {
        return administrationPort;
    }

    /**
     * @param hostName The host name
     * @return This
     */
    public AbstractNodeBuilder withHostName(String hostName) {
        this.hostName = hostName;
        return this;
    }

    /**
     * @param port The administration port
     * @return This
     */
    public AbstractNodeBuilder withAdministrationPort(int port) {
        this.administrationPort = port;
        return this;
    }

    @Override
    public AbstractNodeBuilder withName(String name) {
        return (AbstractNodeBuilder) super.withName(name);
    }

    @Override
    public AbstractNodeBuilder withUserName(String userName) {
        return (AbstractNodeBuilder) super.withUserName(userName);
    }

    @Override
    public AbstractNodeBuilder withPassword(String password) {
        return (AbstractNodeBuilder) super.withPassword(password);
    }

    @Override
    public AbstractNodeBuilder withAdditionalDiscoveryHost(String host) {
        return (AbstractNodeBuilder) super.withAdditionalDiscoveryHost(host);
    }

    /**
     * @return A new node
     */
    @Override
    public abstract INode build();

    @Override
    public String toString() {
        return "{" +
            "hostName='" + hostName + '\'' +
            ", administrationPort=" + administrationPort +
            "} " + super.toString();
    }
}
