# Release notes

# v0.6.0
## What's Changed
* Added support for certificate bundles (#64), so it is now very easy to import a bundle like the [CA certificates extracted from Mozilla](https://curl.se/docs/caextract.html)
* Added support for PKCS12 containers (#68)
* Added support to build multiple TrustStores
* Removed the feature to configure the certificate's alias for the TrustStore. The alias is now derived from the CN of the certificate and its fingerprint (sha1).
* Separated the configuration of the `buildTrustStore`-tasks and the `checkCertificates`-task
* Updated dependencies
* Plugin requires Gradle 7.0

**Full Changelog**: https://github.com/chkpnt/truststorebuilder-gradle-plugin/compare/0.5.1...0.6.0

## Migration from previous versions
The DSL provided by the `trustStoreBuilder`-extension changed a lot. Please have a look at the examples in the [README.md](https://github.com/chkpnt/truststorebuilder-gradle-plugin#readme) and the [demo project](https://github.com/chkpnt/truststorebuilder-gradle-plugin-demo).

# v0.5.1
* Remove runtime dependency to kotlin-stdlib

# v0.5.0
* Task `buildTrustStore` can be excluded from lifecycle task `build`
* Task `checkCertificates` can be excluded from lifecycle task `check`

# v0.4.0
* Changed the versioning scheme to be more similar to [semantic versioning](https://semver.org/)
* Updated several build dependencies
* Plugin requires users to use Gradle 5.0 or later

# v0.3.x
* Refactored the plugin to use the [Task Configuration Avoidance API](https://github.com/gradle/gradle/blob/v5.6.2/subprojects/docs/src/docs/userguide/task_configuration_avoidance.adoc)
* Plugin requires users to use Gradle 4.9 or later
* Fixes #1
  * Plugin now respects the user's configuration
* Builds: 94

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
* Builds: 34, 37, 41
