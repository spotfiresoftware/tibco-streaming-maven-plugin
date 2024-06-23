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
 * Details about a build exception
 */
public class BuildExceptionDetails {

    private String shortMessage;
    private String longDescription;
    private String location;
    private String fileLocation;

    /**
     * @return Short message
     */
    public String getShortMessage() {
        return shortMessage;
    }

    /**
     * @param shortMessage Short message
     * @return This
     */
    public BuildExceptionDetails withShortMessage(String shortMessage) {
        this.shortMessage = shortMessage;
        return this;
    }

    /**
     * @return Long description
     */
    public String getLongDescription() {
        return longDescription;
    }

    /**
     * @param longDescription Long description
     * @return This
     */
    public BuildExceptionDetails withLongDescription(String longDescription) {
        this.longDescription = longDescription;
        return this;
    }

    /**
     * @return Location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location Location
     * @return This
     */
    public BuildExceptionDetails withLocation(String location) {
        this.location = location;
        return this;
    }

    /**
     * @return File location
     */
    public String getFileLocation() {
        return fileLocation;
    }

    /**
     * @param fileLocation File location
     * @return This
     */
    public BuildExceptionDetails withFileLocation(String fileLocation) {
        this.fileLocation = fileLocation;
        return this;
    }
}
