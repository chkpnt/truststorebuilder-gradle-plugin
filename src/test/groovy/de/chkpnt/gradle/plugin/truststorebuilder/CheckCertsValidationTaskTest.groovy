/*
 * Copyright 2016 - 2019 Gregor Dschung
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

import java.nio.file.FileSystem
import java.nio.file.Files

import org.gradle.api.Project
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.testfixtures.ProjectBuilder

import com.google.common.jimfs.Jimfs

import spock.lang.Specification

class CheckCertsValidationTaskTest extends Specification {

    private CheckCertsValidationTask classUnderTest
    private CertificateService certificateServiceMock = Mock()
    private FileSystem fs
    private Project project

    def setup() {
        fs = Jimfs.newFileSystem()
        Files.createDirectory(fs.getPath("certs"))

        project = ProjectBuilder.builder().build()
        classUnderTest = project.task('testCertValidation', type: CheckCertsValidationTask)

        classUnderTest.certificateService = certificateServiceMock
        classUnderTest.atLeastValidDays = 30
        classUnderTest.inputDir = fs.getPath("certs")
        classUnderTest.acceptedFileEndings = ["pem"]
    }

    def "loading a corrupt certificate"() {
        given:
        fs.getPath("certs/corrupt.pem").text = CertificateProvider.CORRUPT

        when:
        classUnderTest.execute()

        then:
        def e = thrown(TaskExecutionException)
        e.cause instanceof CheckCertValidationError
        e.cause.message == "Could not load certificate: certs${fs.separator}corrupt.pem"
    }

    def "loading a non-certificate"() {
        given:
        fs.getPath("certs/notACert.txt").text = CertificateProvider.NOT_A_CERT

        and:
        classUnderTest.acceptedFileEndings = ["txt"]

        when:
        classUnderTest.execute()

        then:
        def e = thrown(TaskExecutionException)
        e.cause instanceof CheckCertValidationError
        e.cause.message == "Could not load certificate: certs${fs.separator}notACert.txt"
    }

    def "certificate letsencrypt.pem is invalid"() {
        given:
        fs.getPath("certs/cacert.pem").text = CertificateProvider.CACERT_ROOT_CA
        fs.getPath("certs/letsencrypt.pem").text = CertificateProvider.LETSENCRYPT_ROOT_CA

        and:
        certificateServiceMock.isCertificateValidInFuture(_, _) >>> [true, false]

        when:
        classUnderTest.execute()

        then:
        def e = thrown(TaskExecutionException)
        e.cause instanceof CheckCertValidationError
        e.cause.message == "Certificate is already or becomes invalid within the next 30 days: certs${fs.separator}letsencrypt.pem"
    }

    def "when all certificates are valid nothing happens"() {
        given:
        fs.getPath("certs/cacert.pem").text = CertificateProvider.CACERT_ROOT_CA
        fs.getPath("certs/letsencrypt.pem").text = CertificateProvider.LETSENCRYPT_ROOT_CA

        and:
        certificateServiceMock.isCertificateValidInFuture(_, _) >> true

        when:
        classUnderTest.execute()

        then:
        true
    }
}
