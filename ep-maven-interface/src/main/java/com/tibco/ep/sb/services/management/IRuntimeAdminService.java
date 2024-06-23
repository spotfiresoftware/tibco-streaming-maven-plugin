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

import java.nio.file.Path;

/**
 * The main entry point for admin service
 */
public interface IRuntimeAdminService {

    /**
     * Create a new context
     *
     * @param installation The installation path
     * @return A new context
     */
    IContext newContext(Path installation);

    /**
     * Create a new destination builder. Use the more fluent method {@link IContext#newDestination()}
     * instead.
     *
     * @param context The context
     * @return A new destination builder
     */
    AbstractDestinationBuilder newDestinationBuilder(IContext context);

    /**
     * Create a new node builder. Use the more fluent method {@link IContext#newNode(String)}
     * instead.
     *
     * @param context  The context
     * @param nodeName The node name
     * @return a new node builder
     */
    AbstractNodeBuilder newNodeBuilder(IContext context, String nodeName);

    /**
     * Create a new command builder
     *
     * @return A new command builder
     */
    AbstractCommandBuilder newCommandBuilder();

    /**
     * Create a new browse services command builder. Use the more fluent method
     * {@link IContext#newBrowseServicesCommand()} instead.
     *
     * @param context The context
     * @return A new browse service command builder
     */
    AbstractBrowseServicesCommandBuilder newBrowseServicesCommandBuilder(IContext context);

    /**
     * Create a new install node command builder. Use the more fluent method
     * {@link INode#newInstallNodeCommand()} instead.
     * @param node The node
     * @return A new install node command builder
     */
    AbstractInstallNodeCommandBuilder newInstallNodeCommandBuilder(INode node);

    /**
     * Create a new deploy command builder. Use the more fluent method
     * {@link IDestination#newDeployFragmentCommand(FragmentType, String)} instead.
     * @param destination The destination
     * @param fragmentType The fragment type
     * @param target The target
     * @return A new deploy command builder
     */
    AbstractDeployFragmentCommandBuilder newDeployCommandBuilder(IDestination destination, FragmentType fragmentType, String target);
}
