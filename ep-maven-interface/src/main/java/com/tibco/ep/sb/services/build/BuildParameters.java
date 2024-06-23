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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The build parameters
 */
public class BuildParameters {

    private final List<Path> projectCompileClassPath = new ArrayList<>();
    private final List<Path> dependenciesCompileClassPath = new ArrayList<>();
    private final List<Path> projectTestCompileClassPath = new ArrayList<>();
    private final List<Path> dependenciesTestCompileClassPath = new ArrayList<>();
    private final List<Path> sourcePaths = new ArrayList<>();
    private final List<Path> testSourcePaths = new ArrayList<>();
    private final Map<String, String> compilerProperties = new HashMap<>();
    private Path projectRootDirectory = null;
    private Path buildDirectory = null;
    private Path configurationDirectory = null;
    private Path testConfigurationDirectory = null;
    private Path productHome = null;

    /**
     * Construct a new set of parameters
     */
    public BuildParameters() {
    }

    /**
     * @param projectRootDirectory The project root directory
     * @return This
     */
    public BuildParameters withProjectRootDirectory(Path projectRootDirectory) {
        this.projectRootDirectory = projectRootDirectory;
        return this;
    }

    /**
     * @return The project root directory
     */
    public Path getProjectRootDirectory() {
        return projectRootDirectory;
    }

    /**
     * @return The class path added by the project (typically target/classes)
     */
    public List<Path> getProjectCompileClassPath() {
        return projectCompileClassPath;
    }

    /**
     * @return The class path added by dependencies (from Maven dependency JARs,
     * or other projects target/classes in Studio)
     */
    public List<Path> getDependenciesCompileClassPath() {
        return dependenciesCompileClassPath;
    }

    /**
     * @return The class path for tests added by the project (typically target/test-classes)
     */
    public List<Path> getProjectTestCompileClassPath() {
        return projectTestCompileClassPath;
    }

    /**
     * @return The class path for tests from dependencies (from Maven test dependency JARs, ...)
     */
    public List<Path> getDependenciesTestCompileClassPath() {
        return dependenciesTestCompileClassPath;
    }

    /**
     * @param classPath The class path added by the project (typically target/classes)
     * @return This
     */
    public BuildParameters withProjectCompileClassPath(List<Path> classPath) {
        return updateCPList(projectCompileClassPath, classPath);
    }

    /**
     * @param classPath The class path added by dependencies (from Maven dependency JARs,
     *                  or other projects target/classes in Studio)
     * @return This
     */
    public BuildParameters withDependenciesCompileClassPath(List<Path> classPath) {
        return updateCPList(dependenciesCompileClassPath, classPath);
    }

    /**
     * @param classPath The class path for tests added by the project (typically target/test-classes)
     * @return This
     */
    public BuildParameters withProjectTestCompileClassPath(List<Path> classPath) {
        return updateCPList(projectTestCompileClassPath, classPath);
    }

    /**
     * @param classPath The class path for tests from dependencies (from Maven test dependency JARs, ...)
     * @return This
     */
    public BuildParameters withDependenciesTestCompileClassPath(List<Path> classPath) {
        return updateCPList(dependenciesCompileClassPath, classPath);
    }

    private BuildParameters updateCPList(List<Path> field, List<Path> source) {
        field.clear();
        field.addAll(source);
        return this;
    }

    /**
     * @param paths The source paths
     * @return This
     */
    public BuildParameters withSourcePaths(List<Path> paths) {
        this.sourcePaths.addAll(paths);
        return this;
    }

    /**
     * @param paths The test source paths
     * @return This
     */
    public BuildParameters withTestSourcePaths(List<Path> paths) {
        this.testSourcePaths.addAll(paths);
        return this;
    }

    /**
     * @param buildDirectory The build directory
     * @return This
     */
    public BuildParameters withBuildDirectory(Path buildDirectory) {
        this.buildDirectory = buildDirectory;
        return this;
    }

    /**
     * @param compilerProperties The compiler properties
     * @return This
     */
    public BuildParameters withCompilerProperties(Map<String, String> compilerProperties) {
        this.compilerProperties.putAll(compilerProperties);
        return this;
    }

    /**
     * @param configurationDirectory The configuration directory
     * @return This
     */
    public BuildParameters withConfigurationDirectory(Path configurationDirectory) {
        this.configurationDirectory = configurationDirectory;
        return this;
    }

    /**
     * @param testConfigurationDirectory The test configuration directory
     * @return This
     */
    public BuildParameters withTestConfigurationDirectory(Path testConfigurationDirectory) {
        this.testConfigurationDirectory = testConfigurationDirectory;
        return this;
    }

    /**
     * @param productHome The product home
     * @return This
     */
    public BuildParameters withProductHome(Path productHome) {
        this.productHome = productHome;
        return this;
    }

    /**
     * @return The source paths
     */
    public List<Path> getSourcePaths() {
        return sourcePaths;
    }

    /**
     * @return The test source paths
     */
    public List<Path> getTestSourcePaths() {
        return testSourcePaths;
    }

    /**
     * @return The compiler properties
     */
    public Map<String, String> getCompilerProperties() {
        return compilerProperties;
    }

    /**
     * @return The build properties
     */
    public Path getBuildDirectory() {
        return buildDirectory;
    }

    /**
     * @return The configuration directory
     */
    public Path getConfigurationDirectory() {
        return configurationDirectory;
    }

    /**
     * @return The test configuration directory
     */
    public Path getTestConfigurationDirectory() {
        return testConfigurationDirectory;
    }

    /**
     * @return The product home
     */
    public Path getProductHome() {
        return productHome;
    }

    @Override
    public String toString() {
        return "BuildParameters{" +
            ", sourcePaths=" + sourcePaths +
            ", testSourcePaths=" + testSourcePaths +
            ", compilerProperties=" + compilerProperties +
            ", buildDirectory=" + buildDirectory +
            ", configurationDirectory=" + configurationDirectory +
            ", testConfigurationDirectory=" + testConfigurationDirectory +
            '}';
    }
}
