/*******************************************************************************
 * Copyright © 2018-2024. Cloud Software Group, Inc.
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

/**
 * Use this as you would {@link AbstractMojoTestCase}, where you want more of the
 * standard Maven defaults to be set (and where the {@link AbstractMojoTestCase}
 * leaves them as null or empty). This includes:
 * <ul>
 * <li> Local repository, repository sessions and managers configured
 * <li> Maven default remote repositories installed <b>N.B.:</b> this does
 * not use your {@code ~/.m2} local settings.
 * <li> System properties are copies
 * </ul>
 * 
 */ 
public abstract class BetterAbstractMojoTestCase extends AbstractMojoTestCase {

    protected File regressionRepository;

    /**
     * Get the regression repository, which should hold the other stub and test
     * artifacts needed for tests.
     * <p>
     * Normally, this should be the local repository of the current build. It is OK
     * to mix test-only artifacts into the local repository because the Maven build
     * arranges for such artifacts to <b>not</b> be deployed.
     * <p>
     * This throws a runtime error if a suitable local repository cannot be
     * determined.
     * @return a local repository path
     */
    private File findRegressionRepository() {
        // Can we figure out the current local repository form settings here?
        // For now, set from the Surefire configuration based on the
        // surrounding build's current Maven settings.
        // So this means such tests cannot be run in IDE unless you set
        // TIBCO_EP_HOME before/around its launcher, and that location is
        // right for the local repository you intend to use.
        final String regressionRepositoryFromExternal = System
                .getProperty("REGRESSION_REPOSITORY");
        if (null != regressionRepositoryFromExternal) {
            return new File(regressionRepositoryFromExternal);
        }

        // Guesses based on older EP conventions
        final String tibcoEPHome = System.getenv("TIBCO_EP_HOME");
        if (null != tibcoEPHome) {
            File regressionRepository = new File(
                    tibcoEPHome + "/BUILD/repository");
            if (regressionRepository.exists()) {
                return regressionRepository;
            }

            regressionRepository = new File(tibcoEPHome + "/.repository");
            if (regressionRepository.exists()) {
                return regressionRepository;
            }
        }

        // Error situations fall through to here.
        throw new RuntimeException(
                "Cannot determine Maven local repository from REGRESSION_REPOSITORY nor TIBCO_EP_HOME.");
    }

    /**
     * Get the version of test-only projects.
     * <p>
     * This does <b>not</b> include stub projects.
     * @return the Maven version of such projects
     */
    protected String getTestProjectsVersion() {
        return "0.0.1-SNAPSHOT";
    }
    
    protected MavenSession newMavenSession() {
        try {
            MavenExecutionRequest request = new DefaultMavenExecutionRequest();
            MavenExecutionResult result = new DefaultMavenExecutionResult();
            
            regressionRepository = findRegressionRepository();
            request.setLocalRepositoryPath(regressionRepository);

            // Populate sensible defaults, including repository basedir and remote repos
            MavenExecutionRequestPopulator populator;
            populator = getContainer().lookup( MavenExecutionRequestPopulator.class );
            populator.populateDefaults( request );

            // this is needed to allow java profiles to get resolved; i.e. avoid during project builds:
            // [ERROR] Failed to determine Java version for profile java-1.5-detected @ org.apache.commons:commons-parent:22, /Users/alex/.m2/repository/org/apache/commons/commons-parent/22/commons-parent-22.pom, line 909, column 14
            Properties properties = System.getProperties();
            String tibcoEPHome = System.getenv("TIBCO_EP_HOME");
            if (null == tibcoEPHome) {
                throw new RuntimeException("TIBCO_EP_HOME must be set for mojo tests.");
                // Probably should force in the mojo configurations, since there
                // is a parameter for the install location, instead of always needed
                // TIBCO_EP_HOME.

            } else {
                properties.put("env.TIBCO_EP_HOME", tibcoEPHome);
                request.setSystemProperties( properties );
            }
            
            // And this is needed so that the repo session in the maven session 
            // has a repository manager, and it points at the local repository.
            // (cf. MavenRepositorySystemUtils.newSession() which is what is otherwise done)
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
            if (e instanceof RuntimeException) {
                throw (RuntimeException)e; // we want the best backtrace 
            }
            
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
