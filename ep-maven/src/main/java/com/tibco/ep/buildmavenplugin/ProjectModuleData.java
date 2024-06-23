/*
 * Copyright (C) 2021-2024 Cloud Software Group, Inc.
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

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Paths;

/**
 * An object representing the module data: the list of interfaces and modules.
 */
public class ProjectModuleData {

    //  The file containing the list of modules.
    //
    private static final String MODULES_FILE = "modules";

    private static final String SBAPP_EXTENSION = ".sbapp";
    private static final String SBINT_EXTENSION = ".sbint";

    private final StringBuilder modules = new StringBuilder();
    private final StringBuilder interfaces = new StringBuilder();
    private boolean modulesIsEmpty = true;
    private boolean interfacesIsEmpty = true;

    /**
     * @param project The project
     * @return The module data, read from the file in target
     * @throws MojoExecutionException Couldn't read the file
     */
    static ProjectModuleData read(MavenProject project) throws MojoExecutionException {
        File modulesFile = Paths.get(project.getBuild().getDirectory(), MODULES_FILE).toFile();

        ProjectModuleData result = new ProjectModuleData();
        try (BufferedReader reader = new BufferedReader(new FileReader(modulesFile))) {
            result.modules.append(reader.readLine());
            result.interfaces.append(reader.readLine());

        } catch (IOException exception) {
            throw new MojoExecutionException("Couldn't read from file: " + modulesFile, exception);
        }

        return result;
    }

    /**
     * Add a module
     *
     * @param entityTypeName The module name
     * @param extension      The extension
     */
    void addModule(String entityTypeName, String extension) {

        if (SBINT_EXTENSION.equals(extension)) {

            if (!interfacesIsEmpty) {
                interfaces.append(" ");
            }
            interfaces.append(entityTypeName);
            interfacesIsEmpty = false;
            return;
        }

        assert SBAPP_EXTENSION.equals(extension) : extension + " for " + entityTypeName;
        if (!modulesIsEmpty) {
            modules.append(" ");
        }
        modules.append(entityTypeName);
        modulesIsEmpty = false;
    }

    /**
     * Write the data to the filesystem.
     *
     * @param project The project
     * @throws MojoExecutionException Could not write the file
     */
    void write(MavenProject project) throws MojoExecutionException {
        File modulesFile = Paths.get(project.getBuild().getDirectory(), MODULES_FILE).toFile();

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(modulesFile))) {

            writer.write(modules + "\n");
            writer.write(interfaces + "\n");

        } catch (IOException exception) {
            throw new MojoExecutionException("Couldn't write to file: " + modulesFile, exception);
        }
    }

    /**
     * @return The space separated module list
     */
    public String getModules() {
        return modules.toString();
    }

    /**
     * @return The space separated interface list
     */
    public String getInterfaces() {
        return interfaces.toString();
    }
}
