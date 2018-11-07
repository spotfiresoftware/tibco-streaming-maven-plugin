# Continuous integration examples

* [Jenkins](#jenkins)
    * [Project type](#project-type)
    * [Set environment](#set-environment)
    * [Build step](#build-step)
    * [Other configurations](#other-configurations)

# Jenkins

The [Jenkins continuous integration server](https://jenkins-ci.org/) contains
a [maven project plugin](https://wiki.jenkins-ci.org/display/JENKINS/Maven+Project+Plugin)
that can easily be configured to build, test and deploy
projects with the event processing maven plugin.

## Project type

  When creating a job, use the **Maven project** type :

![Jenkins new project](images/jenkins-new-project.png)

## Set environment

The environment variable **TIBCO_EP_HOME** must be defined - setting it to the 
jenkins workspace is recommended :

![Jenkins environment](images/jenkins-env.png)

## Build step

The maven goal has to be defined - typically this will be **deploy** so that
the full default lifecycle is executed.  Its also recommended to use a 
maven repository private to the workspace :

![Jenkins build](images/jenkins-build.png)

It is recommended to pass the environment variable BUILD_ID to created processes
so that jenkins can find and kill any left over processes :

``` shell
$ mvn -DenvironmentVariables=BUILD_ID=${BUILD_ID} ...
```

## Other configurations

1. The source code management has to be defined so that jenkins
can find the source

2. Suitable build nodes have to be defined

3. Additional maven goals such as site, cobertura and sonar can be defined

4. Post-build actions to publish [coverage reports](https://wiki.jenkins-ci.org/display/JENKINS/Cobertura+Plugin)
  results are very useful

5. [Email notifications](https://wiki.jenkins-ci.org/display/JENKINS/Email-ext+plugin) 
to the development team

6. Centralized [maven settings](https://wiki.jenkins-ci.org/display/JENKINS/Config+File+Provider+Plugin)
