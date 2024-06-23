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

package com.tibco.ep.sb.services.build;

/**
 * Interface that is called back when a module/interface is built
 */
public interface IBuildNotifier {

    /**
     * Called first during the build
     *
     * @param nbModules number of modules
     */
    default void onBuildStarted(int nbModules) {
        //  Do nothing.
    }

    /**
     * Called last during the build.
     */
    default void onBuildCompleted() {
        //  Do nothing
    }

    /**
     * Called when a module/interface doesn't need a build
     *
     * @param entityName      The entity name
     * @param entityExtension The entity extension (.sbint or .sbapp)
     */
    default void onSkipped(String entityName, String entityExtension) {
        //  Do nothing
    }

    /**
     * Called when a module/interface build is started
     *
     * @param entityName      The entity name
     * @param entityExtension The entity extension (.sbint or .sbapp)
     */
    default void onStarted(String entityName, String entityExtension) {
        //  Do nothing
    }

    /**
     * Called to report an App warning
     *
     * @param entityName The entity name
     * @param warning    The warning
     */
    default void onWarning(String entityName, String warning) {
        //  Do nothing
    }

    /**
     * Called when a module/interface build is complete
     *
     * @param result The build result
     */
    void onCompleted(BuildResult result);
}
