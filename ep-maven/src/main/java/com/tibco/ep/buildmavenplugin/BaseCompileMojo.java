/*
 * Copyright (C) 2023-2025 Cloud Software Group, Inc.
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
package com.tibco.ep.buildmavenplugin;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;

import org.apache.maven.model.Resource;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.shared.filtering.MavenFilteringException;
import org.apache.maven.shared.filtering.MavenResourcesExecution;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;

/**
 * Mojo used to "compile" sources (i.e. copy from source to target).
 */
abstract class BaseCompileMojo extends BaseMojo {

    @Component
    private MavenResourcesFiltering filtering;

    /**
     * Copies resources from {@code sourceDirectories} to {@code targetDirectories}. The {@code sourceDirectories}
     * should be <i>root</i> directories, like {@code src/main/eventflow}. This ensures that the directory/package
     * structure is kept when the files are placed in {@code outputDirectory}.
     * 
     * @param sourceDirectories directories to copy resources from
     * @param targetDirectory output directory
     * @param includes file extension glob patterns to include
     * @throws MavenFilteringException if filtering fails for any reason
     */
    protected final void execute(File[] sourceDirectories, File targetDirectory, String... includes) throws MavenFilteringException {
        prechecks();
        final String encoding = project.getProperties().getOrDefault("project.build.sourceEncoding", "").toString();
        for (File directory : sourceDirectories) {
            if (directory.exists()) {
                Resource resource = new Resource();
                resource.setDirectory(directory.getAbsolutePath());
                Arrays.stream(includes).forEach(resource::addInclude);
                final MavenResourcesExecution exec = new MavenResourcesExecution(Collections.singletonList(resource), targetDirectory, encoding,
                                                                                 Collections.emptyList(), directory, Collections.emptyList());
                filtering.filterResources(exec);
            }
        }
    }

}
