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

    private final GlobalSBDConfParameters globalSBDConfParameters = new GlobalSBDConfParameters();
    private final List<Path> compileClassPath = new ArrayList<>();
    private final List<Path> testClassPath = new ArrayList<>();
    private final List<Path> sourcePaths = new ArrayList<>();
    private final List<Path> testSourcePaths = new ArrayList<>();
    private final Map<String, String> compilerProperties = new HashMap<>();
    private Path buildDirectory = null;

    /**
     * Construct a new set of parameters
     */
    public BuildParameters() {
    }

    /**
     * Get the global SBDConf parameters
     *
     * @return This
     */
    public GlobalSBDConfParameters getGlobalSBDConfParameters() {
        return globalSBDConfParameters;
    }

    /**
     * @param classPath The compile classpath
     * @return This
     */
    public BuildParameters withCompileClassPath(List<Path> classPath) {
        this.compileClassPath.addAll(classPath);
        return this;
    }

    /**
     * @param classPath The test classpath
     * @return This
     */
    public BuildParameters withTestClassPath(List<Path> classPath) {
        this.testClassPath.addAll(classPath);
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
     * @return The compile classpath
     */
    public List<Path> getCompileClassPath() {
        return compileClassPath;
    }

    /**
     * @return The test classpath
     */
    public List<Path> getTestClassPath() {
        return testClassPath;
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


    @Override
    public String toString() {
        return "BuildParameters{" +
            ", globalSBDConfParameters=" + globalSBDConfParameters +
            ", classPath=" + compileClassPath +
            ", sourcePaths=" + sourcePaths +
            ", testSourcePaths=" + testSourcePaths +
            ", compilerProperties=" + compilerProperties +
            ", buildDirectory=" + buildDirectory +
            '}';
    }
}
