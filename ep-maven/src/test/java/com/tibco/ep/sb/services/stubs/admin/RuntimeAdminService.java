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

import com.tibco.ep.sb.services.management.AbstractBrowseServicesCommandBuilder;
import com.tibco.ep.sb.services.management.AbstractCommandBuilder;
import com.tibco.ep.sb.services.management.AbstractDeployFragmentCommandBuilder;
import com.tibco.ep.sb.services.management.AbstractDestinationBuilder;
import com.tibco.ep.sb.services.management.AbstractInstallNodeCommandBuilder;
import com.tibco.ep.sb.services.management.AbstractNodeBuilder;
import com.tibco.ep.sb.services.management.FragmentType;
import com.tibco.ep.sb.services.management.IRuntimeAdminService;
import com.tibco.ep.sb.services.management.IContext;
import com.tibco.ep.sb.services.management.IDestination;
import com.tibco.ep.sb.services.management.INode;

import java.nio.file.Path;

/**
 * The admin service stub implementation
 */
public class RuntimeAdminService implements IRuntimeAdminService {
    @Override
    public Context newContext(Path installation) {
        return new Context(this, installation);
    }

    @Override
    public AbstractDestinationBuilder newDestinationBuilder(IContext context) {
        return new Destination.Builder(this, context);
    }

    @Override
    public AbstractNodeBuilder newNodeBuilder(IContext context, String name) {
        return new Node.Builder(this, context, name);
    }

    @Override
    public AbstractCommandBuilder newCommandBuilder() {
        return new Command.Builder();
    }

    @Override
    public AbstractBrowseServicesCommandBuilder newBrowseServicesCommandBuilder(IContext context) {
        return new BrowseServicesCommand.Builder(context);
    }

    @Override
    public AbstractInstallNodeCommandBuilder newInstallNodeCommandBuilder(INode node) {
        return new InstallNodeCommand.Builder(node);
    }

    @Override
    public AbstractDeployFragmentCommandBuilder newDeployCommandBuilder(IDestination destination, FragmentType fragmentType, String target) {
        return new DeployFragmentCommand.Builder(destination, fragmentType, target);
    }
}
