# Release notes

The version number consists of 3 parts: MAJOR.MINOR.BUILD
* MAJOR and MINOR must get updated with any significant changes.
* BUILD must get updated whenever a release is built. The BUILD number is an ever-increasing number representing a point in time of the projects repository.

Currently, the BUILD number is equal to the number of commits referred to the commit to build. 

# v0.2.x
* Revised configuration:
  * Merging `outputDirName` and `trustStoreFileName` into `trustStore` with default value `$buildDir/cacerts.jks`
  * Renaming `inputDirName` to `inputDir` with new default value `src/main/certs/`
* Builds: 57

# v0.1.x
* Initial release for testing the CI setup:
  * Building on Travis-CI and AppVeyor
  * Publishing on [Bintray](https://bintray.com/chkpnt/maven/truststorebuilder-gradle-plugin/view)
  * Publishing on [plugins.gradle.org](https://plugins.gradle.org/plugin/de.chkpnt.truststorebuilder)
  * Showing code coverage on [Codecov](https://codecov.io/github/chkpnt/truststorebuilder-gradle-plugin)
  * Setup of [SonarQube](https://sonar.chkpnt.de/dashboard?id=de.chkpnt%3Atruststorebuilder-gradle-plugin)
  * Automation powered by [Gradle](build.gradle) 
* Builds: 34, 41
