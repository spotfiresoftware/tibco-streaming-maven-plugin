/*******************************************************************************
 * Copyright (C) 2018-2024 Cloud Software Group, Inc.
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
 ******************************************************************************/
package com.tibco.ep.buildmavenplugin;

import com.tibco.ep.buildmavenplugin.admin.RuntimeCommandNotifier;
import com.tibco.ep.sb.services.management.AbstractBrowseServicesCommandBuilder;
import com.tibco.ep.sb.services.management.AbstractCommandBuilder;
import com.tibco.ep.sb.services.management.AbstractDestinationBuilder;
import com.tibco.ep.sb.services.management.AbstractInstallNodeCommandBuilder;
import com.tibco.ep.sb.services.management.AbstractNodeBuilder;
import com.tibco.ep.sb.services.management.IBrowseServicesCommand;
import com.tibco.ep.sb.services.management.IDestination;
import com.tibco.ep.sb.services.management.INode;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

/**
 * Base type
 */
abstract class BaseExecuteMojo extends BaseMojo {

    // maven user parameters
    //

    /**
     * For random port numbers
     */
    private static final Random RANDOM = new Random();

    /**
     * <p>List of host names for the client discovery.</p>
     *
     * <p>This is used on each administration client invocation.</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/discoveryHosts.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter
    String[] discoveryHosts;
    /**
     * <p>Port number for discovery.  If not set a random free port is selected and
     * persisted to a file</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/discoveryPort.svg" alt="pom">
     *
     * <p>Example use on commandline:</p>
     * <img src="uml/discoveryPort-commandline.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter(property = "discoveryPort")
    Integer discoveryPort;
    /**
     * <p>User name.  If not set authentication is by platform credentials</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/userName.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter
    String userName;
    /**
     * <p>Password</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/password.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter
    String password;
    /**
     * <p>Filename to be used to store generated discovery port</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/discoveryPortFile.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "${project.build.directory}/discovery.port")
    File discoveryPortFile;
    /**
     * <p>cluster name to append to the node names.</p>
     *
     * <p>Nodes are started with a service name obtained by concatenating the node names and the cluster
     * name.  For example with a node name of <b>A</b> plus a cluster name of <b>test</b>
     * results in a service name of <b>A.test</b>.</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/clusterName.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "${project.artifactId}")
    String clusterName;
    /**
     * <p>Base directory of test nodes.</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/nodeDirectory.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "${project.build.directory}/test-nodes")
    File nodeDirectory;
    /**
     * <p>Build type - DEVELOPMENT or PRODUCTION</p>
     *
     * <p>Determines build type to use when installing nodes and deploying
     * applications.<p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/buildtype.svg" alt="pom">
     *
     * <p>Example use on commandline:</p>
     * <img src="uml/buildtype-commandline.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter(property = "build")
    BuldType buildtype;
    /**
     * <p>Environment variables - these environment variables are passed through to
     * created processes.</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/environmentVariables.svg" alt="pom">
     *
     * <p>Example use on commandline:</p>
     * <img src="uml/environmentVariables-commandline.svg" alt="pom">
     *
     * <p><b>User property is:</b> <tt>environmentVariables</tt>.</p>
     *
     * @since 1.0.0
     */
    @Parameter
    Map<String, String> environmentVariables;
    /**
     * Additional parameters supported by install node, to support command-line
     *
     * @since 1.0.0
     */
    @Parameter(property = "environmentVariables", readonly = true)
    String[] environment;

    /**
     * Get the discovery port from plugin configuration or find a new one and persist it
     *
     * @return The discovery port
     */
    int getDiscoveryPort() {
        // if discovery port is set use it, otherwise use a unused & persistent value
        //
        if (discoveryPort != null) {
            return discoveryPort;
        }

        // If the save file exists, use it
        //
        if (discoveryPortFile.exists()) {

            try (InputStreamReader fileReader = new InputStreamReader(
                new FileInputStream(discoveryPortFile), StandardCharsets.UTF_8);
                 BufferedReader bufferedReader = new BufferedReader(fileReader)) {

                return Integer.parseInt(bufferedReader.readLine());

            } catch (NumberFormatException | IOException e) {
                getLog().debug("Caught error reading save file " + discoveryPortFile, e);
            }
        }

        if (!discoveryPortFile.getParentFile().exists()
            && !discoveryPortFile.getParentFile().mkdirs()) {

            getLog().warn("Unable to create save parent directory, will try port 0");
            return 0;
        }

        for (int count = 0; count < 10000; count++) {

            int port = RANDOM.nextInt(65536 - 49152) + 49152;
            try (DatagramSocket socket = new DatagramSocket(port)) {

                // save it to a file if possible
                //
                try (OutputStreamWriter fileWriter = new OutputStreamWriter(new FileOutputStream(discoveryPortFile), StandardCharsets.UTF_8);
                     BufferedWriter bufferedWriter = new BufferedWriter(fileWriter)) {

                    bufferedWriter.write(port + "\n");
                } catch (IOException e) {
                    getLog().warn("UDP port number could not be save to file "
                        + discoveryPortFile.getAbsolutePath(), e);
                }

                getLog().info("UDP port " + port + " selected for discovery");

                return port;

            } catch (SocketException ex) {
                // port not free, we'll keep trying
            }
        }
        getLog().warn("Free UDP port not found, will try 0");

        return 0;
    }

    /**
     * Install a single new node
     *
     * @param nodeDirectory node directory path
     * @param nodeName      node name
     * @param parameters    additional parameters
     * @return Full results of the command
     * @throws MojoExecutionException node install failed
     */
    String installNode(final String nodeDirectory, final String nodeName, Map<String, String> parameters) throws MojoExecutionException {

        removeNodeDirectory(nodeDirectory, nodeName);

        // if discovery port is set use it, otherwise use a unused & persistent value
        //
        int actualDiscoveryPort = getDiscoveryPort();

        assert parameters != null;
        Map<String, String> params = new HashMap<>(parameters);

        if (discoveryHosts != null && discoveryHosts.length > 0) {
            StringBuilder hosts = new StringBuilder();
            for (String discoveryHost : discoveryHosts) {
                if (hosts.length() > 0) {
                    hosts.append(",");
                }
                hosts.append(discoveryHost);
            }
            params.put("discoveryhosts", hosts.toString());
        }
        params.put("discoveryport", Integer.toString(actualDiscoveryPort));

        // build type
        //
        if (buildtype != null && buildtype != BuldType.ALL && buildtype != BuldType.TESTCOV) {
            params.put("buildtype", buildtype.toString());
        }

        doSetEnvironment();
        AbstractNodeBuilder nodeBuilder = getContext().newNode(nodeName);
        updateUserNameAndPassword(nodeBuilder);

        INode node = nodeBuilder.build();

        AbstractInstallNodeCommandBuilder installNodeCommandBuilder = node
            .newInstallNodeCommand();

        params.put("nodedirectory", nodeDirectory);
        getLog().debug("Parameters = " + params.toString());

        RuntimeCommandNotifier notifier = newCommandRunner(installNodeCommandBuilder, nodeName, "node " + nodeName)
            .errorHandling(ErrorHandling.FAIL)
            .recordOutput(true)
            .shutdownHook(command -> {

                //  Try to remove the node if install is cancelled.
                //
                getLog().error("Install node was aborted - attempting to clean up");
                command.cancel();

                try {

                    removeNodes(nodeName, ErrorHandling.IGNORE);

                } catch (MojoExecutionException e) {

                    //  Ignore failures.
                    //
                    getLog().debug("Remove node failed in shutdown hook", e);
                }
            })
            .run(params);

        return notifier.getOutput();
    }

    private void removeNodeDirectory(String nodeDirectory, String nodeName) throws MojoExecutionException {
        // remove node if it already existed
        //
        File fullPath = new File(nodeDirectory + File.separator + nodeName);
        if (fullPath.exists()) {
            stopNodes(nodeName, ErrorHandling.IGNORE);
            removeNodes(nodeName, ErrorHandling.IGNORE);

            if (fullPath.exists()) {
                Path directory = fullPath.toPath();
                try {
                    Files.walkFileTree(directory, new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            Files.delete(file);
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
                            Files.delete(dir);
                            return FileVisitResult.CONTINUE;
                        }

                    });
                } catch (IOException e) {
                    getLog().warn("Error while removing directory" + directory, e);
                }
            }
        }
    }

    /**
     * Remove node
     *
     * @param installPath The installation path
     * @param errorHandling The error handling
     * @throws MojoExecutionException The node removal failed
     */
    void removeNode(String installPath, ErrorHandling errorHandling) throws MojoExecutionException {

        doSetEnvironment();

        IDestination destination = getAdminService()
            .newDestinationBuilder(getContext()).build();

        AbstractCommandBuilder builder = getAdminService().newCommandBuilder()
            .withCommand("remove").withTarget("node")
            .withDestination(destination);

        Map<String, String> parameters = new HashMap<>();
        parameters.put("installpath", installPath);

        newCommandRunner(builder, installPath, "node installpath=" + installPath)
            .recordOutput(false)
            .errorHandling(errorHandling)
            .run(parameters);
    }

    /**
     * Stop node(s)
     *
     * @param serviceName The service name
     * @param errorHandling The error handling
     * @throws MojoExecutionException The node installation failed
     */
    void stopNodes(String serviceName, ErrorHandling errorHandling) throws MojoExecutionException {
        newCommand(serviceName)
            .commandAndTarget("stop", "node")
            .errorHandling(errorHandling)
            .run();
    }

    /**
     * Remove node(s)
     *
     * @param serviceName The service name
     * @param errorHandling The error handling
     * @throws MojoExecutionException The node removal(s) failed
     */
    void removeNodes( String serviceName,  ErrorHandling errorHandling) throws MojoExecutionException {
        newCommand(serviceName)
            .commandAndTarget("remove", "node")
            .errorHandling(errorHandling)
            .run();
    }

    /**
     * Reset the environment for this context
     */
    void doSetEnvironment() {

        getContext().clearEnvironment();
        Map<String, String> allEnvironment = new HashMap<>();

        if (environmentVariables != null) {
            allEnvironment.putAll(environmentVariables);
        }

        if (environment != null) {
            for (String argument : environment) {
                if (argument.contains("=")) {
                    String[] args = argument.split("=");
                    if (args.length == 2) {
                        allEnvironment.put(args[0], args[1]);
                    } else {
                        getLog().warn("Invalid name/value " + argument);
                    }
                } else {
                    getLog().warn("Invalid name/value " + argument);
                }
            }
        }

        if (!allEnvironment.isEmpty()) {
            getContext().withEnvironment(allEnvironment);
        }
    }

    private void updateUserNameAndPassword(AbstractDestinationBuilder builder) {
        if (userName != null && !userName.isEmpty()) {
            builder.withUserName(userName).withPassword(password);
        }
    }

    /**
     * Create a new destination
     * @param serviceName service name
     * @return destination
     */
    IDestination newDestination(String serviceName) {

        AbstractDestinationBuilder builder = getContext().newDestination().withName(serviceName);
        updateUserNameAndPassword(builder);

        if (discoveryHosts != null && discoveryHosts.length > 0) {
            for (String discoveryHost : discoveryHosts) {

                builder.withAdditionalDiscoveryHost(discoveryHost);
            }
        }

        return builder.build();
    }

    private INode newNode(int adminPort, String hostName) {
        AbstractNodeBuilder builder = getContext()
            .newNode(hostName)
            .withAdministrationPort(adminPort);

        updateUserNameAndPassword(builder);
        return builder.build();
    }

    /**
     * Create a new administration command
     * @param serviceName service name
     * @return command
     */
    AdminCommand newCommand(String serviceName) {
        return new AdminCommand(serviceName);
    }

    /**
     * Create a new administration command
     * @param adminPort admin port
     * @return command
     */
    AdminCommand newCommand(int adminPort) {
        return new AdminCommand(adminPort);
    }

    /**
     * Build type
     */
    public enum BuldType {
        /**
         * Development mode
         */
        DEVELOPMENT,
        /**
         * Production mode
         */
        PRODUCTION,
        /**
         * ALL mode - ignored
         */
        ALL,
        /**
         * test coverage mode - ignored
         */
        TESTCOV
    }

    /**
     * Adminisration command
     */
    class AdminCommand {

        private Optional<String> serviceName;
        private Optional<Integer> adminPort;
        private Optional<String> hostname;

        private String command;
        private String target;
        private Map<String, String> parameters;
        private ErrorHandling errorHandling;

        private AdminCommand(String serviceName) {
            this();
            this.serviceName = Optional.of(serviceName);
        }

        private AdminCommand(int adminPort) {
            this();
            this.adminPort = Optional.of(adminPort);
        }

        private AdminCommand() {
            serviceName = Optional.empty();
            adminPort = Optional.empty();
            hostname = Optional.empty();

            errorHandling = ErrorHandling.FAIL;
        }

        /**
         * Set host name
         * @param hostname host name
         * @return this
         */
        public AdminCommand hostname(String hostname) {
            this.hostname = Optional.ofNullable(hostname);
            return this;
        }

        /**
         * Set command and target
         * @param command command
         * @param target target
         * @return this
         */
        public AdminCommand commandAndTarget(String command, String target) {
            this.command = command;
            this.target = target;
            return this;
        }

        /**
         * Set parameters
         * @param parameters parameter
         * @return this
         */
        public AdminCommand parameters(Map<String, String> parameters) {
            this.parameters = parameters;
            return this;
        }

        /**
         * Set error handling
         * @param errorHandling error handling
         * @return this
         */
        public AdminCommand errorHandling(ErrorHandling errorHandling) {
            this.errorHandling = errorHandling;
            return this;
        }

        /**
         * Run command
         * @throws MojoExecutionException execution error
         */
        void run() throws MojoExecutionException {

            doSetEnvironment();

            String shortDescription;
            String longDescription;
            AbstractCommandBuilder builder;

            //  FIX THIS (FL): clarify how browse commands are supposed to work wrt parameters.
            //

            if (serviceName.isPresent()) {

                assert !hostname.isPresent() : hostname;
                assert !adminPort.isPresent() : adminPort;

                shortDescription = serviceName.get();
                longDescription = "node " + serviceName.get();

                int actualDiscoveryPort = getDiscoveryPort();

                if (command.equals("browse") && target.equals("services")) {

                    AbstractBrowseServicesCommandBuilder browseBuilder = getContext()
                        .newBrowseServicesCommand();

                    IBrowseServicesCommand cmd = browseBuilder.build();
                    IDestination destination = cmd.getDestination();
                    destination.setDiscoveryPort(actualDiscoveryPort);

                    builder = browseBuilder;

                } else {

                    IDestination destination = newDestination(serviceName.get());
                    destination.setDiscoveryPort(actualDiscoveryPort);

                    builder = destination.newCommand(command, target);
                }

            } else {

                assert adminPort.isPresent();

                shortDescription = String.valueOf(adminPort.get());
                longDescription = "node port=" + adminPort.get()
                    + (hostname.map(s -> " hostname=" + s).orElse(""));


                if (command.equals("browse") && target.equals("services")) {

                    builder = getContext().newBrowseServicesCommand();

                } else {

                    builder = newNode(adminPort.get(), hostname.orElse(""))
                        .newCommand(command, target);
                }
            }

            newCommandRunner(builder, shortDescription, longDescription)
                .errorHandling(errorHandling)
                .run(parameters);
        }
    }
}
