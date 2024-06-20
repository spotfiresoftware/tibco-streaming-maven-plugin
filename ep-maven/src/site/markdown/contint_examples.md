# Continuous integration examples

* [Jenkins](#jenkins)
    * [Project type](#project-type)
    * [Set environment](#set-environment)
    * [Build step](#build-step)
    * [Other configurations](#other-configurations)

<a name="jenkins"></a>

# Jenkins

The [Jenkins continuous integration server](https://jenkins-ci.org/) contains
a [maven project plugin](https://wiki.jenkins-ci.org/display/JENKINS/Maven+Project+Plugin)
that can easily be configured to build, test and deploy
projects with the event processing maven plugin.

<a name="project-type"></a>

## Project type

  When creating a job, use the **Maven project** type :

![Jenkins new project](images/jenkins-new-project.png)

<a name="set-environment"></a>

## Set environment

The environment variable **TIBCO_EP_HOME** must be defined - setting it to the 
jenkins workspace is recommended :

![Jenkins environment](images/jenkins-env.png)

<a name="build-step"></a>

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

<a name="other-configurations"></a>

## Other configurations

1. The source code management has to be defined so that Jenkins
can find the source.

2. Suitable build nodes have to be defined

3. Additional Maven goals such as `site` and `sonar` can be defined.

4. [Email notifications](https://wiki.jenkins-ci.org/display/JENKINS/Email-ext+plugin) 
to the development team

5. Centralized [Maven settings](https://wiki.jenkins-ci.org/display/JENKINS/Config+File+Provider+Plugin)
