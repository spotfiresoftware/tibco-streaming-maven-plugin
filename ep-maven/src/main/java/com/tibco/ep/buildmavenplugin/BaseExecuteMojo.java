/*******************************************************************************
 * Copyright (C) 2018, TIBCO Software Inc.
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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
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
import java.util.Random;

import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Base type
 *
 */
abstract class BaseExecuteMojo extends BaseMojo {

    // maven user parameters
    //

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
    @Parameter(property="discoveryPort")
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
     * <p>Directory to install test nodes.</p>
     *
     * <p>Example use in pom.xml:</p>
     * <img src="uml/nodeDirectory.svg" alt="pom">
     *
     * @since 1.0.0
     */
    @Parameter(defaultValue = "${project.build.directory}/test-nodes")
    File nodeDirectory;

    /**
     * Build type
     */
    public enum BuldType {
        /** Development mode */
        DEVELOPMENT,
        /** Production mode */
        PRODUCTION,
        /** ALL mode - ignored */
        ALL,
        /** test coverage mode - ignored */
        TESTCOV
    }

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
    @Parameter(property="build")
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
    @Parameter( property = "environmentVariables", readonly=true )
    String[] environment;

    /**
     * For random port numbers
     */
    private Random rand = new Random();

    private static boolean shuttingDown = false;

    /**
     * Find a free UDP port
     * @param saveFile Filename to save port number
     *
     * @return free port
     */
    int findFreeUDPPort(File saveFile) {

        // If the save file exists, use it
        //
        if (saveFile.exists()) {

            InputStreamReader fileReader = null;
            BufferedReader bufferedReader = null;
            try {
                fileReader = new InputStreamReader(new FileInputStream(saveFile), StandardCharsets.UTF_8);
                bufferedReader = new BufferedReader(fileReader);
                int port = Integer.parseInt(bufferedReader.readLine());
                return port;
            } catch (FileNotFoundException e) {
            } catch (NumberFormatException e) {
            } catch (IOException e) {
            } finally {
                if (bufferedReader != null) {
                    try {
                        bufferedReader.close();
                    } catch (IOException e) {
                    }
                }
                if (fileReader != null) {
                    try {
                        fileReader.close();
                    } catch (IOException e) {
                    }
                }
            }
        }
        if (!saveFile.getParentFile().exists() && !saveFile.getParentFile().mkdirs()) {
            getLog().warn("Unable to create save parent directory, will try port 0");
            return 0;
        }

        for (int count=0; count<10000; count++) {
            int port = rand.nextInt(65536-49152)+49152;
            try {
                DatagramSocket socket = new DatagramSocket(port);
                socket.close();

                // save it to a file if possible
                //
                OutputStreamWriter fileWriter = null;
                BufferedWriter bufferedWriter = null;
                try {
                    fileWriter = new OutputStreamWriter(new FileOutputStream(saveFile), StandardCharsets.UTF_8);
                    bufferedWriter = new BufferedWriter(fileWriter);
                    bufferedWriter.write(Integer.toString(port)+"\n");
                } catch (IOException e) {
                    getLog().warn("UDP port number could not be save to file "+saveFile.getAbsolutePath());
                } finally {
                    if (bufferedWriter != null) {
                        try {
                            bufferedWriter.close();
                        } catch (IOException e) {
                        }
                    }
                    if (fileWriter != null) {
                        try {
                            fileWriter.close();
                        } catch (IOException e) {
                        }
                    }
                }

                getLog().info("UDP port "+port+" selected for discovery");

                return port;
            }
            catch (SocketException ex) {
                // port not free, we'll keep trying
            }
        }
        getLog().warn("Free UDP port not found, will try 0");

        return 0;
    }

    /**
     * Run administration command by servicename
     *
     * @param serviceName service name
     * @param userName user name
     * @param password password
     * @param command administration command
     * @param target  administration target
     * @param parameters administration parameters
     * @param failOnError if true, fail on error
     *
     * @throws MojoExecutionException node install failed
     */
    void runAdministrationCommand(final String serviceName, final String userName, final String password, final String command, final String target, final Map<String,String> parameters, final boolean failOnError) throws MojoExecutionException {

        // if discovery port is set use it, otherwise use a unused & persistent value
        //
        int actualDiscoveryPort;
        if (discoveryPort == null) {
            actualDiscoveryPort = findFreeUDPPort(discoveryPortFile);
        } else {
            actualDiscoveryPort=discoveryPort;
        }

        try {
            Object destination;

            setEnvironment();

            if (userName != null && userName.length() > 0 ) {
                destination = dtmDestinationConstructorUsernamePassword.newInstance(serviceName, dtmContext, userName, password);
            } else {
                destination = dtmDestinationConstructor.newInstance(serviceName, dtmContext);
            }
            if (command.equals("browse") && target.equals("services")) {
                Object start = dtmBrowseServicesCommandConstructor.newInstance(dtmContext);

                try {
                    destination = dtmBrowseServicesCommand_getDestination.invoke(start);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Invalid arguments to management API "+dtmBrowseServicesCommand_getDestination.toString());
                }
                try {
                    dtmDestination_setDiscoveryPort.invoke(destination, actualDiscoveryPort);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Invalid arguments to management API "+dtmDestination_setDiscoveryPort.toString());
                }

                Object monitor = createMonitor(command+" "+target, serviceName, failOnError, false);
                try {
                    dtmBrowseServicesCommand_execute.invoke(start, parameters, monitor);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Invalid arguments to management API "+dtmBrowseServicesCommand_execute.toString()+" "+e.getMessage());
                }

                int rc;
                try {
                    rc = (int)dtmBrowseServicesCommand_waitForCompletion.invoke(start);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Invalid arguments to management API "+dtmBrowseServicesCommand_waitForCompletion.toString());
                }

                if (failOnError && rc != 0) {
                    throw new MojoExecutionException("command: "+command+" "+target+" failed: node " + serviceName + " error code " + rc);
                }
            } else {
                Object start = dtmCommandConstructor.newInstance(command, target, destination);
                if (discoveryHosts != null && discoveryHosts.length > 0) {
                    for (String discoveryHost : discoveryHosts) {
                        try {
                            dtmDestination_addDiscoveryHost.invoke(destination, discoveryHost);
                        } catch (IllegalArgumentException e) {
                            throw new MojoExecutionException("Invalid arguments to management API "+dtmDestination_addDiscoveryHost.toString());
                        }
                    }
                }
                try {
                    dtmDestination_setDiscoveryPort.invoke(destination, actualDiscoveryPort);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Invalid arguments to management API "+dtmDestination_setDiscoveryPort.toString());
                }

                Object monitor = createMonitor(command+" "+target, serviceName, failOnError, false);
                try {
                    dtmCommand_execute.invoke(start, parameters, monitor);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Invalid arguments to management API "+dtmCommand_execute.toString());
                }

                int rc;
                try {
                    rc = (int)dtmCommand_waitForCompletion.invoke(start);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Invalid arguments to management API "+dtmCommand_waitForCompletion.toString());
                }

                if (failOnError && rc != 0) {
                    throw new MojoExecutionException("command: "+command+" "+target+" failed: node " + serviceName + " error code " + rc);
                }
            }
        } catch (InstantiationException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException(e.getCause().getMessage());
        } catch (SecurityException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    /**
     * Run administration command by admin port
     *
     * @param adminPort admin port
     * @param hostname hostname
     * @param userName user name
     * @param password password
     * @param command administration command
     * @param target  administration target
     * @param parameters administration parameters
     * @param failOnError if true, fail on error
     *
     * @throws MojoExecutionException node install failed
     */
    void runAdministrationCommand(final int adminPort, final String hostname, final String userName, final String password, final String command, final String target, final Map<String,String> parameters, final boolean failOnError) throws MojoExecutionException {

        try {

            Object destination;

            setEnvironment();

            if (command.equals("browse") && target.equals("services")) {
                Object start = dtmBrowseServicesCommandConstructor.newInstance(dtmContext);

                try {
                    destination = dtmBrowseServicesCommand_getDestination.invoke(start);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Invalid arguments to management API "+dtmBrowseServicesCommand_getDestination.toString());
                }

                Object monitor = createMonitor(command+" "+target, ""+adminPort, failOnError, false);
                try {
                    dtmBrowseServicesCommand_execute.invoke(start, parameters, monitor);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Invalid arguments to management API "+dtmBrowseServicesCommand_execute.toString());
                }

                int rc;

                try {
                    rc = (int)dtmBrowseServicesCommand_waitForCompletion.invoke(start);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Invalid arguments to management API "+dtmBrowseServicesCommand_waitForCompletion.toString());
                }

                if (failOnError && rc != 0) {
                    throw new MojoExecutionException("command: "+command+" "+target+" failed: node " + adminPort + " error code " + rc);
                }
            } else {
                if (userName != null && userName.length() > 0 ) {
                    destination = dtmNodeConstructorUsernamePassword.newInstance("", dtmContext, userName, password);
                } else {
                    destination = dtmNodeConstructor.newInstance("", dtmContext);
                }

                Object start = dtmCommandConstructor.newInstance(command, target, destination);
                try {
                    dtmNode_setAdministrationPort.invoke(destination, adminPort);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Invalid arguments to management API "+dtmNode_setAdministrationPort.toString());
                }

                if (hostname != null) {
                    try {
                        dtmNode_setHostName.invoke(destination, hostname);
                    } catch (IllegalArgumentException e) {
                        throw new MojoExecutionException("Invalid arguments to management API "+dtmNode_setHostName.toString());
                    }
                }

                Object monitor = createMonitor(command+" "+target, ""+adminPort, failOnError, false);
                try {
                    dtmCommand_execute.invoke(start, parameters, monitor);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Invalid arguments to management API "+dtmCommand_execute.toString());
                }

                int rc;

                try {
                    rc = (int)dtmCommand_waitForCompletion.invoke(start);
                } catch (IllegalArgumentException e) {
                    throw new MojoExecutionException("Invalid arguments to management API "+dtmCommand_waitForCompletion.toString());
                }

                if (failOnError && rc != 0) {
                    throw new MojoExecutionException("command: "+command+" "+target+" failed: node " + adminPort + " error code " + rc);
                }
            }
        } catch (InstantiationException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException(e.getCause().getMessage());
        } catch (SecurityException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    /**
     * Install a single new node
     *
     * @param nodeDirectory node directory path
     * @param userName user name
     * @param password password
     * @param nodeName node name
     * @param parameters additional parameters
     * @return Full results of the command
     *
     * @throws MojoExecutionException node install failed
     */
    String installNode(final String nodeDirectory, String userName, String password, final String nodeName, Map<String, String> parameters) throws MojoExecutionException {

        // if discovery port is set use it, otherwise use a unused & persistent value
        //
        int actualDiscoveryPort;
        if (discoveryPort == null) {
            actualDiscoveryPort = findFreeUDPPort(discoveryPortFile);
        } else {
            actualDiscoveryPort=discoveryPort;
        }

        Map<String, String> params;
        if (parameters != null) {
            params = new HashMap<String, String>(parameters);
        } else {
            params = new HashMap<String, String>();
        }

        if (discoveryHosts != null && discoveryHosts.length > 0) {
            StringBuffer hosts = new StringBuffer();
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

        // remove node if it already existed
        //
        File fullPath = new File(nodeDirectory+File.separator+nodeName);
        if (fullPath.exists()) {
            stopNodes(nodeName, userName, password, false);
            removeNodes(nodeName, userName, password, false);

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
                    // ignore
                }
            }
        }

        try {

            setEnvironment();

            Object node;
            if (userName != null && userName.length() > 0 ) {
                node = dtmNodeConstructorUsernamePassword.newInstance(nodeName, dtmContext, userName, password);
            } else {
                node = dtmNodeConstructor.newInstance(nodeName, dtmContext);
            }
            Object install = dtmInstallNodeConstructor.newInstance(node);

            params.put("nodedirectory", nodeDirectory);

            getLog().debug("Parameters = "+params.toString());

            // attempt to remove node if install failed
            //
            Thread hook = new Thread() {
                public void run() {
                    shuttingDown = true;
                    getLog().error("Install node was aborted - attempting to clean up");
                    try {
                        dtmInstallNodeCommand_cancel.invoke(install);
                    } catch (IllegalAccessException e) {
                    } catch (IllegalArgumentException e) {
                    } catch (InvocationTargetException e) {
                    }
                    try {
                        removeNodes(nodeName, userName, password, false);
                    } catch (MojoExecutionException e) {
                    }
                }
            };
            Runtime.getRuntime().addShutdownHook(hook);

            Object monitor = createMonitor("install node", nodeName, true, true);
            try {
                dtmInstallNodeCommand_execute.invoke(install, params, monitor);
            } catch (IllegalArgumentException e) {
                throw new MojoExecutionException("Invalid arguments to management API "+dtmInstallNodeCommand_execute.toString());
            }

            int rc;

            try {
                rc = (int)dtmInstallNodeCommand_waitForCompletion.invoke(install);
            } catch (IllegalArgumentException e) {
                throw new MojoExecutionException("Invalid arguments to management API "+dtmInstallNodeCommand_waitForCompletion.toString());
            }

            if (shuttingDown) {
                throw new MojoExecutionException("command: install node was aborted - attempting to clean up");
            } else {
                if (rc != 0) {
                    throw new MojoExecutionException("command: install node failed: node " + nodeName + " error code " + rc);
                }
                Runtime.getRuntime().removeShutdownHook(hook);
            }

            return monitor.toString();

        } catch (InstantiationException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException(e.getCause().getMessage());
        } catch (SecurityException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    /**
     * Remove node
     *
     * @param installPath Installation path
     * @param failOnError if true, fail on error
     *
     * @throws MojoExecutionException node install failed
     */
    void removeNode(final String installPath, final boolean failOnError) throws MojoExecutionException {

        Map<String, String> parameters = new HashMap<String, String>();

        parameters.put("installpath", installPath);

        try {
            Object destination;

            setEnvironment();

            destination = dtmDestinationConstructorEmpty.newInstance(dtmContext);
            Object start = dtmCommandConstructor.newInstance("remove", "node", destination);

            Object monitor = createMonitor("remove node", installPath, failOnError, false);
            try {
                dtmCommand_execute.invoke(start, parameters, monitor);
            } catch (IllegalArgumentException e) {
                throw new MojoExecutionException("Invalid arguments to management API "+dtmCommand_execute.toString());
            }

            int rc;
            try {
                rc = (int)dtmCommand_waitForCompletion.invoke(start);
            } catch (IllegalArgumentException e) {
                throw new MojoExecutionException("Invalid arguments to management API "+dtmCommand_waitForCompletion.toString());
            }

            if (failOnError && rc != 0) {
                throw new MojoExecutionException("remove node installpath=" + installPath + " failed: error code " + rc);
            }

        } catch (InstantiationException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new MojoExecutionException(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new MojoExecutionException(e.getCause().getMessage());
        } catch (SecurityException e) {
            throw new MojoExecutionException(e.getMessage());
        }
    }

    /**
     * Stop node(s)
     *
     * @param serviceName service name
     * @param userName user name
     * @param password password
     * @param failOnError if true, fail on error
     *
     * @throws MojoExecutionException node install failed
     */
    void stopNodes(final String serviceName, final String userName, final String password, final boolean failOnError) throws MojoExecutionException {
        runAdministrationCommand(serviceName, userName, password, "stop", "node", null, failOnError);
    }

    /**
     * Terminate node(s)
     *
     * @param serviceName service name
     * @param userName user name
     * @param password password
     * @param failOnError if true, fail on error
     *
     * @throws MojoExecutionException node install failed
     */
    void terminateNodes(final String serviceName, final String userName, final String password, final boolean failOnError) throws MojoExecutionException {
        runAdministrationCommand(serviceName, userName, password, "terminate", "node", null, failOnError);
    }

    /**
     * Remove node(s)
     *
     * @param serviceName service name
     * @param userName user name
     * @param password password
     * @param failOnError if true, fail on error
     *
     * @throws MojoExecutionException node install failed
     */
    void removeNodes(final String serviceName, final String userName, final String password, final boolean failOnError) throws MojoExecutionException {
        runAdministrationCommand(serviceName, userName, password, "remove", "node", null, failOnError);
    }

    /**
     * Reset the environment for this context
     *
     * @throws IllegalAccessException reflection error
     * @throws IllegalArgumentException reflection error
     * @throws InvocationTargetException reflection error
     */
    void setEnvironment() throws IllegalAccessException, IllegalArgumentException, InvocationTargetException {

        dtmContext_clearEnvironment.invoke(dtmContext);

        Map<String, String> allEnvironment = new HashMap<String, String>();
        if (environmentVariables != null) {
            allEnvironment.putAll(environmentVariables);
        }
        if (environment != null) {
            for(String argument : environment) {
                if (argument.contains("=")) {
                    String[] args = argument.split("=");
                    if (args.length == 2) {
                        allEnvironment.put(args[0], args[1]);
                    } else {
                        getLog().warn("Invalid name/value "+argument);
                    }
                } else {
                    getLog().warn("Invalid name/value "+argument);
                }
            }
        }
        if (!allEnvironment.isEmpty()) {
            dtmContext_setEnvironment.invoke(dtmContext, allEnvironment);
        }
    }

}
