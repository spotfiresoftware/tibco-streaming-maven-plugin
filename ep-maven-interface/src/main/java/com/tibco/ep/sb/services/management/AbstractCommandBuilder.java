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

package com.tibco.ep.sb.services.management;

import java.util.HashMap;
import java.util.Map;

/**
 * The base class for command builders
 */
public abstract class AbstractCommandBuilder {

    private IDestination destination;
    private String command;
    private String target;
    private String userName;
    private String password;
    private String serviceName;
    private Integer discoveryPort;
    private Map<String, String> environment = new HashMap<>();

    /**
     * No parameter constructor
     */
    protected AbstractCommandBuilder() {
    }

    /**
     * @return The command
     */
    public String getCommand() {
        return command;
    }

    /**
     * @return The target
     */
    public String getTarget() {
        return target;
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
     * @return The service name
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * @return The discovery port
     */
    public Integer getDiscoveryPort() {
        return discoveryPort;
    }

    /**
     * @return The environment
     */
    public Map<String, String> getEnvironment() {
        return environment;
    }

    /**
     * @return the destination
     */
    public IDestination getDestination() {
        return destination;
    }

    /**
     * @param destination The destination
     * @return This
     */
    public AbstractCommandBuilder withDestination(IDestination destination) {
        this.destination = destination;
        return this;
    }

    /**
     * @param command The command
     * @return This
     */
    public AbstractCommandBuilder withCommand(String command) {
        this.command = command;
        return this;
    }

    /**
     * @param target The target
     * @return This
     */
    public AbstractCommandBuilder withTarget(String target) {
        this.target = target;
        return this;
    }

    /**
     * @param environment The environment
     * @return This
     */
    public AbstractCommandBuilder withEnvironment(Map<String, String> environment) {
        this.environment = environment;
        return this;
    }

    /**
     * @param discoveryPort The discovery port
     * @return This
     */
    public AbstractCommandBuilder withDiscoveryPort(int discoveryPort) {
        this.discoveryPort = discoveryPort;
        return this;
    }

    /**
     * @param userName The user name
     * @return This
     */
    public AbstractCommandBuilder withUserName(String userName) {
        this.userName = userName;
        return this;
    }

    /**
     * @param password The password
     * @return This
     */
    public AbstractCommandBuilder withPassword(String password) {
        this.password = password;
        return this;
    }

    /**
     * @param serviceName The service name
     * @return This
     */
    public AbstractCommandBuilder withServiceName(String serviceName) {
        this.serviceName = serviceName;
        return this;
    }

    /**
     * @return A new command
     */
    public abstract ICommand build();

    @Override
    public String toString() {
        return "{" +
            "destination=" + destination +
            ", command='" + command + '\'' +
            ", target='" + target + '\'' +
            ", userName='" + userName + '\'' +
            ", password='" + password + '\'' +
            ", serviceName='" + serviceName + '\'' +
            ", discoveryPort=" + discoveryPort +
            ", environment=" + environment +
            "}{class=" + getClass().getSimpleName() + "}";
    }
}
