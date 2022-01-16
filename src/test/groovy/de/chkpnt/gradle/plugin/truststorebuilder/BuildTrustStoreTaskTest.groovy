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

import org.gradle.api.Project
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

import static de.chkpnt.gradle.plugin.truststorebuilder.KeystoreAssertions.assertFingerprintOfKeystoreEntry

class BuildTrustStoreTaskTest extends Specification {

    private BuildTrustStoreTask classUnderTest

    @TempDir
    private Path testProjectDir

    private Project project

    private DefaultCertificateService certificateService = new DefaultCertificateService()

    def setup() {
        Files.createDirectory(testProjectDir.resolve("certs"))
        project = ProjectBuilder.builder()
                .withProjectDir(testProjectDir.toFile())
                .build()
        classUnderTest = project.task('buildTrustStore', type: BuildTrustStoreTask)
    }

    def "BuildTrustStoreTask works awesome"() {
        given:
        testProjectDir.resolve("certs/letsencrypt.pem").text = CertificateProvider.LETSENCRYPT_ROOT_CA
        testProjectDir.resolve("certs/letsencrypt.pem.config").text = "alias=Let's Encrypt Root CA"
        testProjectDir.resolve("certs/cacert.pem").text = CertificateProvider.CACERT_ROOT_CA
        testProjectDir.resolve("certs/cacert.pem.config").text = "alias=CACert Root CA"

        and:
        classUnderTest.trustStore({
            it.path(testProjectDir.resolve("truststore.jks"))
            it.password("changeit")
        })
        classUnderTest.source(testProjectDir.resolve("certs"))
        classUnderTest.include(["**/*.pem"])

        when:
        classUnderTest.importCerts()

        then:
        def ks = certificateService.loadKeystore(testProjectDir.resolve("truststore.jks"), "changeit")
        assertFingerprintOfKeystoreEntry(ks, "Let's Encrypt Root CA", CertificateProvider.LETSENCRYPT_ROOT_CA_FINGERPRINT_SHA1)
        assertFingerprintOfKeystoreEntry(ks, "cacert root ca", CertificateProvider.CACERT_ROOT_CA_FINGERPRINT_SHA1)
    }

    def "alias is derived from filename if not available through config file"() {
        given:
        testProjectDir.resolve("certs/letsencrypt.pem").text = CertificateProvider.LETSENCRYPT_ROOT_CA
        testProjectDir.resolve("certs/letsencrypt.pem.config").text = "bla=no_alias"
        testProjectDir.resolve("certs/cacert.pem").text = CertificateProvider.CACERT_ROOT_CA

        and:
        classUnderTest.trustStore({
            it.path("truststore.jks")
            it.password("changeit")
        })
        classUnderTest.source(testProjectDir.resolve("certs"))
        classUnderTest.include("**/*.pem")

        when:
        classUnderTest.importCerts()

        then:
        def ks = certificateService.loadKeystore(testProjectDir.resolve("truststore.jks"), "changeit")
        assertFingerprintOfKeystoreEntry(ks, "letsencrypt.pem", CertificateProvider.LETSENCRYPT_ROOT_CA_FINGERPRINT_SHA1)
        assertFingerprintOfKeystoreEntry(ks, "cacert.pem", CertificateProvider.CACERT_ROOT_CA_FINGERPRINT_SHA1)
    }


    def "output folder is generated"() {
        given:
        def outputdir = testProjectDir.resolve("foo/bar")
        classUnderTest.trustStore({
            it.path("foo/bar/truststore.jks")
            it.password("changeit")
        })
        assert Files.notExists(outputdir)

        and:
        classUnderTest.source("certs")
        classUnderTest.include("**/*.pem")

        when:
        classUnderTest.importCerts()

        then:
        Files.exists(outputdir)
    }

    def "throwing exception if password is not set"() {
        given:
        classUnderTest.trustStore({
            it.path(testProjectDir.resolve("foo/bar/truststore.jks"))
            it.password("")
        })
        classUnderTest.source("certs")
        classUnderTest.include("**/*.pem")

        when:
        classUnderTest.importCerts()

        then:
        def e = thrown(TaskExecutionException)
        IllegalArgumentException rootCause = e.cause
        rootCause.message == "The following properties have to be configured appropriately: password"
    }

    def "throwing exception if include is not set appropriately"() {
        given:
        classUnderTest.trustStore({
            it.path("foo/bar/truststore.jks")
            it.password("changeit")
        })
        classUnderTest.source("certs")

        and:
        // PropertyList<String> can't contain null values, therefore testing is not needed
        classUnderTest.include("", " ")

        when:
        classUnderTest.importCerts()

        then:
        def e = thrown(TaskExecutionException)
        IllegalArgumentException rootCause = e.cause
        rootCause.message == "The following properties have to be configured appropriately: include"
    }
}
