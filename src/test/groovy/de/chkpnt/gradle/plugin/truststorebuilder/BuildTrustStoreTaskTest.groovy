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
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.TempDir

import java.nio.file.Files
import java.nio.file.Path

import static de.chkpnt.gradle.plugin.truststorebuilder.KeystoreAssertions.assertFingerprintOfKeystoreEntry
import static org.hamcrest.Matchers.containsInAnyOrder
import static spock.util.matcher.HamcrestSupport.that

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
        testProjectDir.resolve("certs/cacert.pem").text = CertificateProvider.CACERT_ROOT_CA

        and:
        classUnderTest.trustStorePath.set(testProjectDir.resolve("truststore.jks"))
        classUnderTest.trustStorePassword.set("changeit")
        classUnderTest.source.set(testProjectDir.resolve("certs"))
        classUnderTest.includes.set(["**/*.pem"])

        when:
        classUnderTest.buildTrustStore()

        then:
        def ks = certificateService.loadKeystore(testProjectDir.resolve("truststore.jks"), "changeit")
        that Collections.list(ks.aliases()), containsInAnyOrder(*[
            CertificateProvider.LETSENCRYPT_ROOT_CA_ALIAS.toLowerCase(),
            CertificateProvider.CACERT_ROOT_CA_ALIAS.toLowerCase()
        ])
        assertFingerprintOfKeystoreEntry(ks, CertificateProvider.LETSENCRYPT_ROOT_CA_ALIAS, CertificateProvider.LETSENCRYPT_ROOT_CA_FINGERPRINT_SHA1)
        assertFingerprintOfKeystoreEntry(ks, CertificateProvider.CACERT_ROOT_CA_ALIAS, CertificateProvider.CACERT_ROOT_CA_FINGERPRINT_SHA1)
    }


    def "output folder is generated"() {
        given:
        def outputdir = testProjectDir.resolve("foo/bar")
        classUnderTest.trustStorePath.set(testProjectDir.resolve("foo/bar/truststore.jks"))
        classUnderTest.trustStorePassword.set("changeit")
        assert Files.notExists(outputdir)

        and:
        classUnderTest.source.set(testProjectDir.resolve("certs"))
        classUnderTest.includes.set(["**/*.pem"])

        when:
        classUnderTest.buildTrustStore()

        then:
        Files.exists(outputdir)
    }
}
