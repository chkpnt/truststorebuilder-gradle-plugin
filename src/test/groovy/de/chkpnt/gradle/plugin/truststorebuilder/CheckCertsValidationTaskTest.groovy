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

class CheckCertsValidationTaskTest extends Specification {

    private CheckCertsValidationTask classUnderTest
    private CertificateService certificateServiceMock = Spy(DefaultCertificateService)
    @TempDir
    private Path testProjectDir
    private Project project

    def setup() {
        Files.createDirectory(testProjectDir.resolve("certs"))

        project = ProjectBuilder.builder()
                .withProjectDir(testProjectDir.toFile())
                .build()
        classUnderTest = project.task('testCertValidation', type: CheckCertsValidationTask)

        classUnderTest.certificateService = certificateServiceMock
        classUnderTest.atLeastValidDays.set(30)
        classUnderTest.source("certs")
        classUnderTest.include("**/*.pem")
    }

    def "loading a corrupt certificate"() {
        given:
        testProjectDir.resolve("certs/corrupt.pem").text = CertificateProvider.CORRUPT

        when:
        classUnderTest.testValidation()

        then:
        def e = thrown(TrustStoreBuilderError)
        e.message.startsWith( "Could not load certificate: ")
    }

    def "loading a non-certificate"() {
        given:
        testProjectDir.resolve("certs/notACert.txt").text = CertificateProvider.NOT_A_CERT

        and:
        classUnderTest.include("**/*.txt")

        when:
        classUnderTest.testValidation()

        then:
        def e = thrown(TrustStoreBuilderError)
        e.message.startsWith( "Could not load certificate: ")
    }

    def "certificate letsencrypt.pem is invalid"() {
        given:
        testProjectDir.resolve("certs/letsencrypt.pem").text = CertificateProvider.LETSENCRYPT_ROOT_CA

        and:
        certificateServiceMock.isCertificateValidInFuture(_, _) >> false

        when:
        classUnderTest.testValidation()

        then:
        def e = thrown(TrustStoreBuilderError)
        e.message == "Certificate \"ISRG Root X1 [CABD2A7]\" is already or becomes invalid within the next 30 days: certs${testProjectDir.fileSystem.separator}letsencrypt.pem"
    }

    def "when all certificates are valid nothing happens"() {
        given:
        testProjectDir.resolve("certs/cacert.pem").text = CertificateProvider.CACERT_ROOT_CA
        testProjectDir.resolve("certs/letsencrypt.pem").text = CertificateProvider.LETSENCRYPT_ROOT_CA

        and:
        certificateServiceMock.isCertificateValidInFuture(_, _) >> true

        when:
        classUnderTest.testValidation()

        then:
        true
    }

    def "certificate bundle is supported"() {
        given:
        testProjectDir.resolve("certs/bundle.pem").text = """
        $CertificateProvider.LETSENCRYPT_ROOT_CA
        $CertificateProvider.CACERT_ROOT_CA
        """

        and:
        certificateServiceMock.isCertificateValidInFuture(_, _) >> true

        when:
        classUnderTest.testValidation()

        then:
        2 * certificateServiceMock.isCertificateValidInFuture(_, _)
    }
}
