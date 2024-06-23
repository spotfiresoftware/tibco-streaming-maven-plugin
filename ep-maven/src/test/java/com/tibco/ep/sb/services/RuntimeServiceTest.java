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

package com.tibco.ep.sb.services;

import com.tibco.ep.sb.services.build.BuildParameters;
import com.tibco.ep.sb.services.build.BuildResult;
import com.tibco.ep.sb.services.build.IBuildNotifier;
import com.tibco.ep.sb.services.build.IRuntimeBuildService;
import com.tibco.ep.sb.services.build.BuildTarget;
import com.tibco.ep.sb.services.management.AbstractCommandBuilder;
import com.tibco.ep.sb.services.management.AbstractDeployFragmentCommandBuilder;
import com.tibco.ep.sb.services.management.AbstractDestinationBuilder;
import com.tibco.ep.sb.services.management.AbstractNodeBuilder;
import com.tibco.ep.sb.services.management.FragmentType;
import com.tibco.ep.sb.services.management.IBrowseServicesCommand;
import com.tibco.ep.sb.services.management.ICommand;
import com.tibco.ep.sb.services.management.IContext;
import com.tibco.ep.sb.services.management.IDeployFragmentCommand;
import com.tibco.ep.sb.services.management.IDestination;
import com.tibco.ep.sb.services.management.IInstallNodeCommand;
import com.tibco.ep.sb.services.management.INode;
import com.tibco.ep.sb.services.management.INotifier;
import com.tibco.ep.sb.services.management.IRuntimeAdminService;
import org.junit.Test;

import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Basic tests for stubbed services
 */
public class RuntimeServiceTest {

    /**
     * Test that the stubbed runtime services can be loaded
     */
    @Test
    public void testStubbedServiceLoad() {
        assertThat(RuntimeServices.getAdminService(getClass().getClassLoader())).isNotNull();
        assertThat(RuntimeServices.getBuildService(getClass().getClassLoader())).isNotNull();
    }

    /**
     * Tests for build service stubs
     */
    @Test
    public void testStubbedBuildServiceImplementation() {

        IRuntimeBuildService build = RuntimeServices.getBuildService(getClass().getClassLoader());
        assertThat(build).isNotNull();

        BuildParameters parameters = new BuildParameters();

        Map<String, String> properties = new HashMap<>();
        properties.put("MyProp", "MyVal");
        parameters
            .withBuildDirectory(Paths.get("buildDir"))
            .withProjectCompileClassPath(Arrays.asList(
                Paths.get("first.jar"),
                Paths.get("path", "to", "classes")))
            .withProjectTestCompileClassPath(Collections.singletonList(Paths.get("test.jar")))
            .withCompilerProperties(properties)
            .withConfigurationDirectory(Paths.get("src", "main", "configurations"))
            .withTestConfigurationDirectory(Paths.get("src", "test", "configurations"))
            .withSourcePaths(Collections.singletonList(Paths.get("src", "main")))
            .withTestSourcePaths(Collections.singletonList(Paths.get("src", "test")));

        BuildNotifier handler = new BuildNotifier();
        build.build("MyBuild", BuildTarget.MAIN, parameters, handler);

        assertThat(handler.getStartedEntity()).isNotEmpty();
        assertThat(handler.getResult().getElapsedTimeMillis()).isGreaterThan(0);
        assertThat(handler.getResult().getEntityName()).isNotEmpty();
        assertThat(handler.getResult().getEntityPath()).isNotNull();
        assertThat(handler.getResult().getException()).isNotNull();
    }

    /**
     * Tests for admin service stubs
     */
    @Test
    public void testStubbedAdminServiceImplementation() {

        IRuntimeAdminService admin = RuntimeServices.getAdminService(getClass().getClassLoader());
        assertThat(admin).isNotNull();

        //  Context.
        //
        IContext ctx = admin.newContext(Paths.get("/some/path"));
        ctx.withTracingEnabled();
        ctx.clearEnvironment();

        Map<String, String> environment = new HashMap<>();
        environment.put("key", "value");
        ctx.withEnvironment(environment);

        //  Node.
        //
        AbstractNodeBuilder nodeBuilder = ctx.newNode("MyNode")
            .withHostName("MyNodeHost")
            .withAdministrationPort(123)
            .withUserName("user")
            .withPassword("password")
            .withAdditionalDiscoveryHost("discovery_host");
        assertThat(nodeBuilder.getHostName()).isEqualTo("MyNodeHost");
        assertThat(nodeBuilder.getAdministrationPort()).isEqualTo(123);
        INode node = nodeBuilder.build();

        //  Destination.
        //
        AbstractDestinationBuilder destinationBuilder = ctx.newDestination()
            .withName("destination_name")
            .withUserName("username")
            .withPassword("password")
            .withAdditionalDiscoveryHost("discovery_host1")
            .withAdditionalDiscoveryHost("discovery_host2");
        assertThat(destinationBuilder.getName()).isEqualTo("destination_name");
        assertThat(destinationBuilder.getUserName()).isEqualTo("username");
        assertThat(destinationBuilder.getPassword()).isEqualTo("password");
        assertThat(destinationBuilder.getDiscoveryHosts())
            .contains("discovery_host1", "discovery_host2");
        assertThat(destinationBuilder.getContext()).isEqualTo(ctx);

        IDestination destination = destinationBuilder.build();
        destination.setDiscoveryPort(1234);
        AbstractCommandBuilder commandBuilder = destination.newCommand("command", "target")
            .withCommand("command")
            .withTarget("target")
            .withDestination(destination);
        assertThat(commandBuilder.getCommand()).isEqualTo("command");
        assertThat(commandBuilder.getTarget()).isEqualTo("target");
        assertThat(commandBuilder.getDestination()).isEqualTo(destination);

        //  Command.
        //
        ICommand commandWithDestination = commandBuilder.build();
        assertThat(commandWithDestination.getDestination()).isEqualTo(destination);
        DummyNotifier notifier = new DummyNotifier();
        commandWithDestination.execute(environment, notifier);
        assertThat(commandWithDestination.waitForCompletion()).isEqualTo(0);
        assertThat(notifier.started).isTrue();
        assertThat(notifier.complete).isTrue();
        assertThat(notifier.info).isNotEmpty();
        commandWithDestination.cancel();

        //  Deploy command.
        //
        AbstractDeployFragmentCommandBuilder deployCommandBuilder = destination
            .newDeployFragmentCommand(FragmentType.EVENTFLOW, "target")
            .withExecutionOptions(Arrays.asList("exec1", "exec2"))
            .withApplicationArguments(Arrays.asList("app1", "app2"));
        assertThat(deployCommandBuilder.getApplicationArguments()).containsExactly("app1", "app2");
        assertThat(deployCommandBuilder.getExecutionOptions()).containsExactly("exec1", "exec2");
        assertThat(deployCommandBuilder.getFragmentType()).isEqualTo(FragmentType.EVENTFLOW);
        IDeployFragmentCommand deployFragmentCommand = deployCommandBuilder.build();
        assertThat(deployFragmentCommand).isNotNull();

        //  Browse services command
        //
        IBrowseServicesCommand browseServicesCommand = ctx
            .newBrowseServicesCommand().build();
        assertThat(browseServicesCommand).isNotNull();

        //  Install node command
        //
        IInstallNodeCommand installNodeCommand = node
            .newInstallNodeCommand()
            .build();
        assertThat(installNodeCommand).isNotNull();
    }

    /**
     * Dummy notifier implementation
     */
    private static class DummyNotifier implements INotifier {

        private boolean started = false;
        private boolean complete = false;
        private String info;

        @Override
        public void start() {
            started = true;
        }

        @Override
        public void cancel() {
            //  Do nothing
        }

        @Override
        public void failed(int returnCode) {
            //  Do nothing
        }

        @Override
        public void complete() {
            complete = true;
        }

        @Override
        public void results(List<String> resultLines) {
            //  Do nothing
        }

        @Override
        public void info(String source, String message) {
            info = "source: " + source + ", message: " + message;
        }

        @Override
        public void error(String source, String message) {
            //  Do nothing
        }
    }

    private class BuildNotifier implements IBuildNotifier {
        private String startedEntity;
        private BuildResult result;

        public String getStartedEntity() {
            return startedEntity;
        }

        public BuildResult getResult() {
            return result;
        }

        @Override
        public void onStarted(String entityName, String entityExtension) {
            this.startedEntity = entityName;
        }

        @Override
        public void onCompleted(BuildResult result) {
            this.result = result;
        }
    }
}
