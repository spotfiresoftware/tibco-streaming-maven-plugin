/*******************************************************************************
 * Copyright (C) 2018-2023 Cloud Software Group, Inc.
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

import java.io.File;
import java.util.Arrays;
import java.util.Properties;

import org.apache.maven.DefaultMaven;
import org.apache.maven.Maven;
import org.apache.maven.execution.DefaultMavenExecutionRequest;
import org.apache.maven.execution.DefaultMavenExecutionResult;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.execution.MavenExecutionRequestPopulator;
import org.apache.maven.execution.MavenExecutionResult;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.Mojo;
import org.apache.maven.plugin.testing.AbstractMojoTestCase;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.ProjectBuilder;
import org.apache.maven.project.ProjectBuildingRequest;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.internal.impl.SimpleLocalRepositoryManagerFactory;
import org.eclipse.aether.repository.LocalRepository;

/** Use this as you would {@link AbstractMojoTestCase},
 * where you want more of the standard maven defaults to be set
 * (and where the {@link AbstractMojoTestCase} leaves them as null or empty).
 * This includes:
 * <ul>
 * <li> local repo, repo sessions and managers configured
 * <li> maven default remote repos installed (NB: this does not use your ~/.m2 local settings)
 * <li> system properties are copies
 * </ul>
 * 
 */ 
public abstract class BetterAbstractMojoTestCase extends AbstractMojoTestCase {

    File regressionRepository;
    
    protected MavenSession newMavenSession() {
        try {
            MavenExecutionRequest request = new DefaultMavenExecutionRequest();
            MavenExecutionResult result = new DefaultMavenExecutionResult();

            regressionRepository = new File(System.getenv("TIBCO_EP_HOME")+"/BUILD/repository");
            if (regressionRepository.exists()) {
                request.setLocalRepositoryPath(regressionRepository);
            } else {
                regressionRepository = new File(System.getenv("TIBCO_EP_HOME")+"/.repository");
                if (regressionRepository.exists()) {
                    request.setLocalRepositoryPath(regressionRepository);
                }
            }
            
            // populate sensible defaults, including repository basedir and remote repos
            MavenExecutionRequestPopulator populator;
            populator = getContainer().lookup( MavenExecutionRequestPopulator.class );
            populator.populateDefaults( request );

            // this is needed to allow java profiles to get resolved; i.e. avoid during project builds:
            // [ERROR] Failed to determine Java version for profile java-1.5-detected @ org.apache.commons:commons-parent:22, /Users/alex/.m2/repository/org/apache/commons/commons-parent/22/commons-parent-22.pom, line 909, column 14
            Properties properties = System.getProperties();
            properties.put("env.TIBCO_EP_HOME", System.getenv("TIBCO_EP_HOME"));
            request.setSystemProperties( properties );
            
            // and this is needed so that the repo session in the maven session 
            // has a repo manager, and it points at the local repo
            // (cf MavenRepositorySystemUtils.newSession() which is what is otherwise done)
            DefaultMaven maven = (DefaultMaven) getContainer().lookup( Maven.class );
            DefaultRepositorySystemSession repoSession =
                (DefaultRepositorySystemSession) maven.newRepositorySession( request );
            repoSession.setLocalRepositoryManager(
                new SimpleLocalRepositoryManagerFactory().newInstance(repoSession, 
                    new LocalRepository( request.getLocalRepository().getBasedir() ) ));

            @SuppressWarnings("deprecation")
            MavenSession session = new MavenSession( getContainer(), 
                repoSession,
                request, result );
            return session;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    /** Extends the super to use the new {@link #newMavenSession()} introduced here 
     * which sets the defaults one expects from maven; the standard test case leaves a lot of things blank */
    @Override
    protected MavenSession newMavenSession(MavenProject project) {
        MavenSession session = newMavenSession();
        session.setCurrentProject( project );
        session.setProjects( Arrays.asList( project ) );
        return session;        
    }

    /** As {@link #lookupConfiguredMojo(MavenProject, String)} but taking the pom file 
     * and creating the {@link MavenProject}. 
     * @param pom pom file
     * @param goal goal
     * @return mojo
     * @throws Exception on error
     * */
    protected Mojo lookupConfiguredMojo(File pom, String goal) throws Exception {
        assertNotNull( pom );
        assertTrue( pom.exists() );

        ProjectBuildingRequest buildingRequest = newMavenSession().getProjectBuildingRequest();
        ProjectBuilder projectBuilder = lookup(ProjectBuilder.class);
        MavenProject project = projectBuilder.build(pom, buildingRequest).getProject();

        return lookupConfiguredMojo(project, goal);
    }


}
