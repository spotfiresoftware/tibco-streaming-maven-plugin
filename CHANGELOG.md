# Change Log
All notable changes to this project will be documented in this file.

## [1.6.0] - Pending

### Added

- [SB-48296](https://jira.tibco.com/browse/SB-48296) EP-MAVEN: detect duplicate dependencies

  fragment dependencies are skipped if already included via other dependencies

- [SB-48476](https://jira.tibco.com/browse/SB-48476) EP-MAVEN: stamp product version into archive and fragment archive manifests

  added product version to manfest and pom.properties files

### Changed

- [SB-48274](https://jira.tibco.com/browse/SB-48274) EP-MAVEN: unpack-fragment goal is a bit chatty.

  Unpacking info lines have been reduced to debug

### Fixed

- [SB-48215](https://jira.tibco.com/browse/SB-48215) EP-MAVEN: Unable to create report directory
- [SB-41668](https://jira.tibco.com/browse/SB-41668) EP-MAVEN: Warning when using ep-maven-plugin with mvn -T option

  Improvements to thread safe

- [SBGPP-83](https://jira.tibco.com/browse/SBGPP-83) TEST: StreamBaseException: Error loading resource

  Added missing target/eventflow to the test classpath

- [SB-48556](https://jira.tibco.com/browse/SB-48556) EP-MAVEN: confusing messages running against read-only product installation

  Improve error message if product directory is not found

## [1.5.0] - Sep 19 2019

Initial github release

