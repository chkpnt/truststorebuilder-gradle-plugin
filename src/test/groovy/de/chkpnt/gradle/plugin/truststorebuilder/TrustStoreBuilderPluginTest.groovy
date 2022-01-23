/*
 * Copyright 2016 - 2022 Gregor Dschung
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.chkpnt.gradle.plugin.truststorebuilder

import org.gradle.testkit.runner.GradleRunner
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import static de.chkpnt.gradle.plugin.truststorebuilder.KeystoreAssertions.assertFingerprintOfKeystoreEntry
import static de.chkpnt.gradle.plugin.truststorebuilder.KeystoreAssertions.assertNumberOfEntriesInKeystore
import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class TrustStoreBuilderPluginTest extends Specification {

    @TempDir
    private Path testProjectDir

    private File buildFile

    def setup() {
        initProjectDir()
    }

    private def initProjectDir() {
        copyCertsToProjectDir()
        buildFile = testProjectDir.resolve('build.gradle').toFile()
        buildFile << """
            plugins {
                id 'de.chkpnt.truststorebuilder'
            }

            trustStoreBuilder {
                trustStore {
                }
                checkCertificates {
                }
            }
        """
        def settingsFile = testProjectDir.resolve('settings.gradle').toFile()
        settingsFile << ""
    }

    private def copyCertsToProjectDir() {
        def dest = testProjectDir.resolve("src/main/certs")

        def certsFolder = getClass().getClassLoader().getResource("certs")
        def source = Paths.get(certsFolder.toURI())

        new AntBuilder().copy(toDir: dest) { fileset(dir: source) }
    }

    def "buildTrustStore and checkCertificates tasks are included in task-list"() {
        when:
        def result = buildGradleRunner("tasks", "--all").build()

        then:
        println(result.output)
        result.output.contains("buildTrustStore - Adds all certificates found under 'src/main/certs' to the TrustStore.\n")
        result.output.contains("checkCertificates - Checks the validation of certificates.\n")
    }

    def "buildTrustStore and checkCertificates tasks are included in lifecycle"() {
        given:
        buildFile.text = """
            plugins {
                id 'de.chkpnt.truststorebuilder'
            }

            trustStoreBuilder {
                trustStore {
                }
                checkCertificates {
                    exclude("Mozilla/cacert.pem") // contains expired certs
                }
            }
        """

        when:
        def result = buildGradleRunner("build").build()

        then:
        result.output.contains("Task :buildTrustStore")
        result.output.contains("Task :checkCertificates")
    }


    def "buildTrustStore and checkCertificates tasks can be excluded from lifecycle"() {
        given:
        buildFile.text = """
            plugins {
                id 'de.chkpnt.truststorebuilder'
            }

            trustStoreBuilder {
                trustStore {
                    buildEnabled.set(false)
                }
                checkCertificates {
                    checkEnabled.set(false)
                }
            }
        """

        when:
        def result = buildGradleRunner("build").build()

        then:
        result.output.contains("Task :buildTrustStore") == false
        result.output.contains("Task :checkCertificates") == false
    }

    def "buildTrustStore task builds a TrustStore"() {
        when:
        def result = buildGradleRunner("buildTrustStore").build()

        then:
        result.task(":buildTrustStore").outcome == SUCCESS
        Path trustStore = getDefaultTrustStorePath()

        assertNumberOfEntriesInKeystore(trustStore, "changeit", 131) // 130 from Mozilla (already contains Let's encrypt) + CAcert
        assertFingerprintOfKeystoreEntry(trustStore, "changeit", CertificateProvider.LETSENCRYPT_ROOT_CA_ALIAS, CertificateProvider.LETSENCRYPT_ROOT_CA_FINGERPRINT_SHA1)
        assertFingerprintOfKeystoreEntry(trustStore, "changeit", CertificateProvider.CACERT_ROOT_CA_ALIAS, CertificateProvider.CACERT_ROOT_CA_FINGERPRINT_SHA1)
    }

    def "buildTrustStore task respects configuration (inputDir)"() {
        given:
        def origPath = testProjectDir.resolve("src/main/certs")
        def newPath = testProjectDir.resolve("src/main/x509")
        Files.move(origPath, newPath)

        and:
        buildFile.text = """
            plugins {
                id 'de.chkpnt.truststorebuilder'
            }

            trustStoreBuilder {
                trustStore {
                    source('src/main/x509')
                }
            }
        """

        when:
        def result = buildGradleRunner("buildTrustStore").build()

        then:
        result.task(":buildTrustStore").outcome == SUCCESS
        Path trustStore = getDefaultTrustStorePath()

        assertNumberOfEntriesInKeystore(trustStore, "changeit", 131)
        assertFingerprintOfKeystoreEntry(trustStore, "changeit", CertificateProvider.LETSENCRYPT_ROOT_CA_ALIAS, CertificateProvider.LETSENCRYPT_ROOT_CA_FINGERPRINT_SHA1)
        assertFingerprintOfKeystoreEntry(trustStore, "changeit", CertificateProvider.CACERT_ROOT_CA_ALIAS, CertificateProvider.CACERT_ROOT_CA_FINGERPRINT_SHA1)
    }

    def "buildTrustStore task respects configuration (include)"() {
        // Textfixtures contains 'isrgrootx1.pem' and 'root.crt'
        given:
        buildFile.text = """
            plugins {
                id 'de.chkpnt.truststorebuilder'
            }

            trustStoreBuilder {
                trustStore {
                    include('**/*.crt')
                }
            }
        """

        when:
        def result = buildGradleRunner("buildTrustStore").build()

        then:
        result.task(":buildTrustStore").outcome == SUCCESS
        assertNumberOfEntriesInKeystore(getDefaultTrustStorePath(), "changeit", 1)
    }

    def "buildTrustStore task respects configuration (trustStore)"() {
        // Textfixtures contains 'isrgrootx1.pem' and 'root.crt'
        given:
        buildFile.text = """
            plugins {
                id 'de.chkpnt.truststorebuilder'
            }

            trustStoreBuilder {
                trustStore {
                    path('trustStores/cacerts.jks')
                    password('changeit')
                }
            }
        """

        when:
        def result = buildGradleRunner("buildTrustStore").build()

        then:
        result.task(":buildTrustStore").outcome == SUCCESS
        assertNumberOfEntriesInKeystore(testProjectDir.resolve("trustStores/cacerts.jks"), "changeit", 131)
    }

    def "custom BuildTrustStoreTask"() {
        given:
        buildFile.text = """            
            import de.chkpnt.gradle.plugin.truststorebuilder.*

            plugins {
                id 'de.chkpnt.truststorebuilder'
            }
            
            trustStoreBuilder {
                trustStore("forAppX") {
                    path('build/truststore-x.jks')
                    password('changeit')
                    source('src/certs/AppX')
                }
            }
        """

        when:
        def result = buildGradleRunner("tasks", "--all").build()

        then:
        result.output.contains("buildTrustStoreForAppX - Adds all certificates found under 'src/certs/AppX' to the TrustStore.\n")
    }

    private Path getDefaultTrustStorePath() {
        Path keystore = testProjectDir.resolve("build/cacerts.jks")
        assert Files.exists(keystore)
        return keystore
    }

    private def GradleRunner buildGradleRunner(String... tasks) {
        GradleRunner.create()
                .withDebug(true)
                .withGradleVersion("7.0")
                .withProjectDir(testProjectDir.toFile())
                .withArguments(tasks)
                .withPluginClasspath()
    }
}
