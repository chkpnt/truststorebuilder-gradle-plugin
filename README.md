# Build your Java TrustStore with Gradle

This Gradle plugin can build a Java TrustStore from existing certificates. The generated TrustStore uses the JKS format as the database format for the certificates. Additionally, the certificates are checked for validity.

```
...
cmd> gradlew build
...
```

## Status

[![Gradle plugin](https://img.shields.io/badge/plugins.gradle.org-de.chkpnt.truststorebuilder-blue.svg)](https://plugins.gradle.org/plugin/de.chkpnt.truststorebuilder)
[![JCenter artifact](https://img.shields.io/badge/JCenter-de.chkpnt%3Atrust%E2%80%A6--plugin-blue.svg)](https://bintray.com/chkpnt/maven/truststorebuilder-gradle-plugin/view)
[![SonarQube](https://img.shields.io/badge/SonarQube-sonar.chkpnt.de-blue.svg)](https://sonar.chkpnt.de/dashboard?id=de.chkpnt%3Atruststorebuilder-gradle-plugin&did=1)
[![License](https://img.shields.io/github/license/chkpnt/truststorebuilder-gradle-plugin.svg?label=License)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))  
[![Linux Build Status](https://img.shields.io/travis/chkpnt/truststorebuilder-gradle-plugin/master.svg?label=Linux build)](https://travis-ci.org/chkpnt/truststorebuilder-gradle-plugin)
[![Windows Build Status](https://img.shields.io/appveyor/ci/chkpnt/truststorebuilder-gradle-plugin/master.svg?label=Windows build)](https://ci.appveyor.com/project/chkpnt/truststorebuilder-gradle-plugin/branch/master)  
[![Tests](https://img.shields.io/sonar/https/sonar.chkpnt.de/de.chkpnt:truststorebuilder-gradle-plugin/tests.svg?label=SonarQube: tests)](https://sonar.chkpnt.de/drilldown/measures?id=de.chkpnt%3Atruststorebuilder-gradle-plugin&metric=tests)
[![Tech dept by SonarQube](https://img.shields.io/sonar/https/sonar.chkpnt.de/de.chkpnt:truststorebuilder-gradle-plugin/tech_debt.svg?label=SonarQube: tech dept)](https://codecov.io/github/chkpnt/truststorebuilder-gradle-plugin?branch=master)
[![Test coverage by SonarQube](https://img.shields.io/sonar/https/sonar.chkpnt.de/de.chkpnt:truststorebuilder-gradle-plugin/coverage.svg?label=SonarQube: coverage)](https://sonar.chkpnt.de/drilldown/measures?id=de.chkpnt%3Atruststorebuilder-gradle-plugin&metric=lines_to_cover)
[![Test coverage by codecov.io](https://img.shields.io/codecov/c/github/chkpnt/truststorebuilder-gradle-plugin/master.svg?label=Codecov: coverage)](https://codecov.io/github/chkpnt/truststorebuilder-gradle-plugin?branch=master)


## Configuration

The plugin registers an extension `trustStoreBuilder` which allows to configure the following settings:

| Setting             | Description                                                                       | Default                       | Type         |
|---------------------|-----------------------------------------------------------------------------------|-------------------------------|--------------|
| password            | The password used for the TrustStore.                                             | changeit                      | String       |
| outputDirName       | The directory where the TrustStore is built, relative to the project.             | the project's build directory | String       |
| trustStoreFileName  | The file name of the TrustStore to build.                                         | cacerts.jks                   | String       |
| inputDirName        | The directory which is scanned for certificates, relative to the project.         | certs                         | String       |
| acceptedFileEndings | A file being processed as a certificate has to have a file ending from this list. | ['crt', 'cer', 'pem']         | List<String> |
| atLeastValidDays    | Number of days the certificates have to be at least valid.                        | 90                            | int          |

## Example
