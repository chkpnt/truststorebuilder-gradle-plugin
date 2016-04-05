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
[![Tech dept by SonarQube](https://img.shields.io/sonar/https/sonar.chkpnt.de/de.chkpnt:truststorebuilder-gradle-plugin/tech_debt.svg?label=SonarQube: tech dept)](https://sonar.chkpnt.de/overview/debt?id=de.chkpnt%3Atruststorebuilder-gradle-plugin)
[![Test coverage by SonarQube](https://img.shields.io/sonar/https/sonar.chkpnt.de/de.chkpnt:truststorebuilder-gradle-plugin/coverage.svg?label=SonarQube: coverage)](https://sonar.chkpnt.de/drilldown/measures?id=de.chkpnt%3Atruststorebuilder-gradle-plugin&metric=lines_to_cover)
[![Test coverage by codecov.io](https://img.shields.io/codecov/c/github/chkpnt/truststorebuilder-gradle-plugin/master.svg?label=Codecov: coverage)](https://codecov.io/github/chkpnt/truststorebuilder-gradle-plugin?branch=master)


## Configuration

The plugin registers an extension `trustStoreBuilder` which allows to configure the following settings:

| Setting             | Description                                                                       | Default                       | Type         |
|---------------------|-----------------------------------------------------------------------------------|-------------------------------|--------------|
| password            | The password used for the TrustStore.                                             | changeit                      | String       |
| trustStore          | The file of the TrustStore to build.                                              | $buildDir/cacerts.jks         | Object*      |
| inputDir            | The directory which is scanned for certificates.                                  | $projectDir/src/main/certs    | Object*      |
| acceptedFileEndings | A file being processed as a certificate has to have a file ending from this list. | ['crt', 'cer', 'pem']         | List<String> |
| atLeastValidDays    | Number of days the certificates have to be at least valid.                        | 90                            | int          |

_\* Anything, that can be handled by [project.file(...)](https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:file%28java.lang.Object%29)._

## Example

A demonstration of this plugin can be found in [this repository](https://github.com/chkpnt/truststorebuilder-gradle-plugin-demo).

## Development

I'm using _Eclipse Neon_ with the plugin _[GroovyEclipse](https://github.com/groovy/groovy-eclipse/wiki)_. This project requires the _Groovy Compiler 2.4 Feature_.
