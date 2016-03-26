# Build your Java TrustStore with Gradle

This Gradle plugin can build a Java TrustStore from existing certificates. The generated TrustStore uses the JKS format as the database format for the certificates. Additionally, the certificates are checked for validity.

```
...
cmd> gradlew build
...
```

## Status

[![Linux Build Status](https://img.shields.io/travis/chkpnt/truststorebuilder-gradle-plugin/master.svg?label=Linux build)](https://travis-ci.org/chkpnt/truststorebuilder-gradle-plugin)
[![Windows Build Status](https://img.shields.io/appveyor/ci/chkpnt/truststorebuilder-gradle-plugin/master.svg?label=Windows build)](https://ci.appveyor.com/project/chkpnt/truststorebuilder-gradle-plugin/branch/master)
[![Code Coverage](https://img.shields.io/codecov/c/github/chkpnt/truststorebuilder-gradle-plugin/master.svg?label=Code coverage)](https://codecov.io/github/chkpnt/truststorebuilder-gradle-plugin?branch=master)
[![License](https://img.shields.io/github/license/chkpnt/truststorebuilder-gradle-plugin.svg?label=License)](https://tldrlegal.com/license/apache-license-2.0-(apache-2.0))


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
