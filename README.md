# Build your Java TrustStore with Gradle

This Gradle plugin for Gradle 7.0 and newer can build a Java TrustStore from existing certificates and bundles.
TrustStores can be built in the JKS format or as PKCS12-containers.
Additionally, a validation check for the certificates is provided by this plugin, too.

## Status

[![Gradle plugin](https://img.shields.io/badge/plugins.gradle.org-de.chkpnt.truststorebuilder-blue.svg)](https://plugins.gradle.org/plugin/de.chkpnt.truststorebuilder)
[![License](https://img.shields.io/github/license/chkpnt/truststorebuilder-gradle-plugin.svg?label=License)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))  
[![GitHub Workflow Status](https://img.shields.io/github/workflow/status/chkpnt/truststorebuilder-gradle-plugin/Run%20tests?logo=github)](https://github.com/chkpnt/truststorebuilder-gradle-plugin/actions/workflows/run-tests.yml)
[![Windows Build Status](https://ci.appveyor.com/api/projects/status/c5cu6n9ngma600y9?svg=true)](https://ci.appveyor.com/project/chkpnt/truststorebuilder-gradle-plugin/branch/main)
[![KDoc](https://img.shields.io/badge/Docs-KDoc-lightgrey.svg)](https://chkpnt.github.io/truststorebuilder-gradle-plugin/kdoc/index.html)
[![SonarQube](https://img.shields.io/badge/SonarQube-sonar.chkpnt.de-blue.svg)](https://sonar.chkpnt.de/dashboard?id=de.chkpnt%3Atruststorebuilder-gradle-plugin)
[![Tests](https://img.shields.io/sonar/tests/de.chkpnt:truststorebuilder-gradle-plugin?label=Tests&server=https%3A%2F%2Fsonar.chkpnt.de&sonarVersion=8.9)](https://sonar.chkpnt.de/component_measures?id=de.chkpnt%3Atruststorebuilder-gradle-plugin&metric=tests)
[![Codecov](https://img.shields.io/badge/Other%20CI%20tool-codecov.io-blue.svg)](https://codecov.io/)
[![Test coverage by codecov.io](https://codecov.io/gh/chkpnt/truststorebuilder-gradle-plugin/branch/main/graph/badge.svg)](https://codecov.io/github/chkpnt/truststorebuilder-gradle-plugin?branch=main)

## Configuration

The tasks provided by this plugin are configured via the extension `trustStoreBuilder`.
The following example registers two tasks `buildTrustStore` and `checkCertificates`, which are both included in the `build` and `check` phases:

```groovy
plugins {
    id "de.chkpnt.truststorebuilder" version "<version>"
}

// minimal configuration:
trustStoreBuilder {
    trustStore {
    }
    checkCertificates {
    }
}

// which is the same as
trustStoreBuilder {
    trustStore {
        path("$buildDir/cacerts.jks")
        password("changeit")
        source("src/main/certs")
        include("**/*.crt", "**/*.cer", "**/*.pem")
        buildEnabled.set(true)
    }
    checkCertificates {
        source("src/main/certs")
        include("**/*.crt", "**/*.cer", "**/*.pem")
        exclude()
        atLeastValidDays.set(90)
        checkEnabled.set(true)
    }
}
```

The function `trustStore` takes a `TrustStoreSpec` and can be called multiple times,
if multiple TrustStores are to be built.
In such a case, the TrustStores need to be named:

```groovy
trustStoreBuilder {
    trustStore("jks") {
        path("$buildDir/cacerts.jks")
    }
    trustStore("pkcs12") {
        path("$buildDir/cacerts.p12")
    }
}
```

A `TrustStoreSpec` consists the following settings:

| Setting                          | Description                                                                                                                                | Default                              | Type                |
|----------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------|--------------------------------------|---------------------|
| path(value: Any)                 | The file of the TrustStore to build. The type of the TrustStore is derived from the file extension. Supported are _jks_, _p12_, and _pfx_. | $buildDir/cacerts.jks                | function            |
| password(value: String)          | The password used for the TrustStore.                                                                                                      | changeit                             | function            |
| source(directory: Any*)          | The directory which is scanned for certificates and bundles.                                                                               | $projectDir/src/main/certs           | function            |
| include(vararg patterns: String) | Filter for the source directory.                                                                                                           | ['**/*.crt', '**/*.cer', '**/*.pem'] | function            |
| buildEnabled                     | Should the `build`-task depend on `buildTrustStore<Name>`?                                                                                 | true                                 | Property\<Boolean\> |

The function `checkCertificates` takes a `CheckCertsSpec`, consisting of the following settings:

| Setting                          | Description                                                        | Default                              | Type                |
|----------------------------------|--------------------------------------------------------------------|--------------------------------------|---------------------|
| source(directory: Any*)          | The directory which is scanned for certificates and bundles.       | $projectDir/src/main/certs           | function            |
| include(vararg patterns: String) | Filter for the source directory, can be called multiple times.     | ['**/*.crt', '**/*.cer', '**/*.pem'] | function            |
| exclude(vararg patterns: String) | Exclusions for the source directory, can be called multiple times. | []                                   | function            |
| atLeastValidDays                 | Number of days the certificates have to be at least valid.         | 90                                   | Property\<Int\>     |
| checkEnabled                     | Should the `check`-task depend on `checkCertificates`?             | true                                 | Property\<Boolean\> |

_\* Anything, that can be handled by [project.file(...)](https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:file%28java.lang.Object%29)._

## Example

A demonstration of this plugin can be found in [this repository](https://github.com/chkpnt/truststorebuilder-gradle-plugin-demo).


<!--[![Tech dept by SonarQube](https://img.shields.io/sonar/https/sonar.chkpnt.de/de.chkpnt:truststorebuilder-gradle-plugin/tech_debt.svg?label=Tech dept)](https://sonar.chkpnt.de/overview/debt?id=de.chkpnt%3Atruststorebuilder-gradle-plugin)
[![Test coverage by SonarQube](https://img.shields.io/sonar/https/sonar.chkpnt.de/de.chkpnt:truststorebuilder-gradle-plugin/coverage.svg?label=Code coverage)](https://sonar.chkpnt.de/drilldown/measures?id=de.chkpnt%3Atruststorebuilder-gradle-plugin&metric=lines_to_cover)      
[![Average component dependency according to John Lakos](https://img.shields.io/sonar/https/sonar.chkpnt.de/de.chkpnt:truststorebuilder-gradle-plugin/sg_i.CORE_ACD.svg?label=ACD)](https://sonar.chkpnt.de/dashboard/index/19?did=5)
[![Cumulative component dependency according to John Lakos](https://img.shields.io/sonar/https/sonar.chkpnt.de/de.chkpnt:truststorebuilder-gradle-plugin/sg_i.CORE_CCD.svg?label=CCD)](https://sonar.chkpnt.de/dashboard/index/19?did=5)
[![Normalized cumulative component dependency according to John Lakos](https://img.shields.io/sonar/https/sonar.chkpnt.de/de.chkpnt:truststorebuilder-gradle-plugin/sg_i.CORE_NCCD.svg?label=NCCD)](https://sonar.chkpnt.de/dashboard/index/19?did=5)
[![Sonargraph by hello2morrow](https://img.shields.io/badge/Static%20code%20analyzer-Sonargraph-blue.svg)](https://www.hello2morrow.com/products/sonargraph)
[![Sonargraph report](https://img.shields.io/badge/Report-chkpnt.github.io-lightgrey.svg)](https://chkpnt.github.io/truststorebuilder-gradle-plugin/reports/sonargraph.html)  -->