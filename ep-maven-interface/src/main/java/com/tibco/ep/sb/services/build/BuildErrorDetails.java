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
 * Build error detail
 */
public class BuildErrorDetails {

    private Exception exception;
    private String shortExceptionMessage;
    private String longDescription;
    private String location;
    private String file;

    /**
     * @return The exception
     */
    public Exception getException() {
        return exception;
    }

    /**
     * @param exception The exception
     * @return This
     */
    public BuildErrorDetails withException(Exception exception) {
        this.exception = exception;
        return this;
    }

    /**
     * @return The short exception message (if any)
     */
    public String getShortExceptionMessage() {
        return shortExceptionMessage;
    }

    /**
     * @param shortExceptionMessage The short exception message
     * @return This
     */
    public BuildErrorDetails withShortExceptionMessage(String shortExceptionMessage) {
        this.shortExceptionMessage = shortExceptionMessage;
        return this;
    }

    /**
     * @return The long description for this error
     */
    public String getLongDescription() {
        return longDescription;
    }

    /**
     * @param longDescription The long description for this error
     * @return This
     */
    public BuildErrorDetails withLongDescription(String longDescription) {
        this.longDescription = longDescription;
        return this;
    }

    /**
     * @return The location
     */
    public String getLocation() {
        return location;
    }

    /**
     * @param location The location
     * @return This
     */
    public BuildErrorDetails withLocation(String location) {
        this.location = location;
        return this;
    }

    /**
     * @return The file
     */
    public String getFile() {
        return file;
    }

    /**
     * @param file The file
     * @return This
     */
    public BuildErrorDetails withFile(String file) {
        this.file = file;
        return this;
    }
}
