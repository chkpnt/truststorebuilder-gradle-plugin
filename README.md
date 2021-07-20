# Build your Java TrustStore with Gradle

This Gradle plugin for Gradle 5.0 and newer can build a Java TrustStore from existing certificates. The generated TrustStore uses the JKS format as the database format for the certificates. Additionally, the certificates are checked for validity.

For example, _DST Root CA X3_ expires on September 30, 2021. So when this date draws near in relation to the system's local date, something like this happens: 
```
cmd> # Windows, with the locale set to de_DE:

cmd> date /T
10.07.2021

cmd> gradlew.bat build
:assemble UP-TO-DATE
:buildTrustStore UP-TO-DATE
:checkCertificates FAILED

FAILURE: Build failed with an exception.

* What went wrong:
Execution failed for task ':checkCertificates'.
> Certificate is already or becomes invalid within the next 90 days:
D:\truststorebuilder-gradle-plugin-demo\src\main\certs\Let's Encrypt\dstrootx3.pem

* Try:
Run with --stacktrace option to get the stack trace. Run with --info or --debug option to get more log output.

BUILD FAILED

Total time: 4.62 secs
```

## Status

[![Gradle plugin](https://img.shields.io/badge/plugins.gradle.org-de.chkpnt.truststorebuilder-blue.svg)](https://plugins.gradle.org/plugin/de.chkpnt.truststorebuilder)
[![License](https://img.shields.io/github/license/chkpnt/truststorebuilder-gradle-plugin.svg?label=License)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))  
[![Linux Build Status](https://travis-ci.com/chkpnt/truststorebuilder-gradle-plugin.svg?branch=master)](https://travis-ci.com/chkpnt/truststorebuilder-gradle-plugin)
[![Windows Build Status](https://ci.appveyor.com/api/projects/status/c5cu6n9ngma600y9?svg=true)](https://ci.appveyor.com/project/chkpnt/truststorebuilder-gradle-plugin/branch/master)
[![KDoc](https://img.shields.io/badge/Docs-KDoc-lightgrey.svg)](https://chkpnt.github.io/truststorebuilder-gradle-plugin/kdoc/truststorebuilder-gradle-plugin/index.html)
[![SonarQube](https://img.shields.io/badge/SonarQube-sonar.chkpnt.de-blue.svg)](https://sonar.chkpnt.de/dashboard?id=de.chkpnt%3Atruststorebuilder-gradle-plugin&did=1)
[![Tests](https://img.shields.io/sonar/https/sonar.chkpnt.de/de.chkpnt:truststorebuilder-gradle-plugin/tests.svg?label=Tests)](https://sonar.chkpnt.de/component_measures?id=de.chkpnt%3Atruststorebuilder-gradle-plugin&metric=tests)
[![Sonargraph by hello2morrow](https://img.shields.io/badge/Static%20code%20analyzer-Sonargraph-blue.svg)](https://www.hello2morrow.com/products/sonargraph)
[![Sonargraph report](https://img.shields.io/badge/Report-chkpnt.github.io-lightgrey.svg)](https://chkpnt.github.io/truststorebuilder-gradle-plugin/reports/sonargraph.html)  
[![Codecov](https://img.shields.io/badge/Other%20CI%20tool-codecov.io-blue.svg)](https://codecov.io/)
[![Test coverage by codecov.io](https://codecov.io/gh/chkpnt/truststorebuilder-gradle-plugin/branch/master/graph/badge.svg)](https://codecov.io/github/chkpnt/truststorebuilder-gradle-plugin?branch=master)
<!--[![Tech dept by SonarQube](https://img.shields.io/sonar/https/sonar.chkpnt.de/de.chkpnt:truststorebuilder-gradle-plugin/tech_debt.svg?label=Tech dept)](https://sonar.chkpnt.de/overview/debt?id=de.chkpnt%3Atruststorebuilder-gradle-plugin)
[![Test coverage by SonarQube](https://img.shields.io/sonar/https/sonar.chkpnt.de/de.chkpnt:truststorebuilder-gradle-plugin/coverage.svg?label=Code coverage)](https://sonar.chkpnt.de/drilldown/measures?id=de.chkpnt%3Atruststorebuilder-gradle-plugin&metric=lines_to_cover)      
[![Average component dependency according to John Lakos](https://img.shields.io/sonar/https/sonar.chkpnt.de/de.chkpnt:truststorebuilder-gradle-plugin/sg_i.CORE_ACD.svg?label=ACD)](https://sonar.chkpnt.de/dashboard/index/19?did=5)
[![Cumulative component dependency according to John Lakos](https://img.shields.io/sonar/https/sonar.chkpnt.de/de.chkpnt:truststorebuilder-gradle-plugin/sg_i.CORE_CCD.svg?label=CCD)](https://sonar.chkpnt.de/dashboard/index/19?did=5)
[![Normalized cumulative component dependency according to John Lakos](https://img.shields.io/sonar/https/sonar.chkpnt.de/de.chkpnt:truststorebuilder-gradle-plugin/sg_i.CORE_NCCD.svg?label=NCCD)](https://sonar.chkpnt.de/dashboard/index/19?did=5)-->

## Configuration

The plugin registers two tasks `checkCertificates` and `buildTrustStore` which are configured via
the extension `trustStoreBuilder`:

| Setting             | Description                                                                       | Default                       | Type           |
|---------------------|-----------------------------------------------------------------------------------|-------------------------------|----------------|
| password            | The password used for the TrustStore.                                             | changeit                      | String         |
| trustStore          | The file of the TrustStore to build.                                              | $buildDir/cacerts.jks         | Object*        |
| inputDir            | The directory which is scanned for certificates.                                  | $projectDir/src/main/certs    | Object*        |
| acceptedFileEndings | A file being processed as a certificate has to have a file ending from this list. | ['crt', 'cer', 'pem']         | List\<String\> |
| atLeastValidDays    | Number of days the certificates have to be at least valid.                        | 90                            | int            |
| checkEnabled        | Should the `check`-task depend on `checkCertificates`?                            | true                          | Boolean        |
| buildEnabled        | Should the `build`-task depend on `buildTrustStore`?                              | true                          | Boolean        |

_\* Anything, that can be handled by [project.file(...)](https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:file%28java.lang.Object%29)._

## Example

A demonstration of this plugin can be found in [this repository](https://github.com/chkpnt/truststorebuilder-gradle-plugin-demo).
