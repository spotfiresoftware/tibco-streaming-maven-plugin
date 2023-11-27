//
// Copyright (c) 2023 Cloud Software Group, Inc.
// All Rights Reserved. Confidential & Proprietary.
//
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
