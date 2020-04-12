/*
 * Copyright 2016 - 2020 Gregor Dschung
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

import static de.chkpnt.gradle.plugin.truststorebuilder.KeystoreAssertions.*

import java.nio.file.FileSystem
import java.nio.file.Files

import org.gradle.api.Project
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.testfixtures.ProjectBuilder

import com.google.common.jimfs.Configuration
import com.google.common.jimfs.Jimfs

import spock.lang.Specification

class ImportCertsTaskTest extends Specification {

    private ImportCertsTask classUnderTest

    private FileSystem fs

    private Project project

    private DefaultCertificateService certificateService = new DefaultCertificateService()

    def setup() {
        fs = Jimfs.newFileSystem(Configuration.unix())
        Files.createDirectory(fs.getPath("certs"))

        project = ProjectBuilder.builder().build()
        classUnderTest = project.task('importCert', type: ImportCertsTask)
        classUnderTest.fileAdapter = new TestFileAdapter()
    }

    def "ImportCertsTask works awesome"() {
        given:
        fs.getPath("certs/letsencrypt.pem").text = CertificateProvider.LETSENCRYPT_ROOT_CA
        fs.getPath("certs/letsencrypt.pem.config").text = "alias=Let's Encrypt Root CA"
        fs.getPath("certs/cacert.pem").text = CertificateProvider.CACERT_ROOT_CA
        fs.getPath("certs/cacert.pem.config").text = "alias=CACert Root CA"

        and:
        classUnderTest.keystore = fs.getPath("truststore.jks")
        classUnderTest.password = "changeit"
        classUnderTest.inputDir = fs.getPath("certs")
        classUnderTest.acceptedFileEndings = ["pem"]

        when:
        classUnderTest.importCerts()

        then:
        def ks = certificateService.loadKeystore(fs.getPath("truststore.jks"), "changeit")
        assertFingerprintOfKeystoreEntry(ks, "Let's Encrypt Root CA", CertificateProvider.LETSENCRYPT_ROOT_CA_FINGERPRINT_SHA1)
        assertFingerprintOfKeystoreEntry(ks, "cacert root ca", CertificateProvider.CACERT_ROOT_CA_FINGERPRINT_SHA1)
    }

    def "alias is derived from filename if not available through config file"() {
        given:
        fs.getPath("certs/letsencrypt.pem").text = CertificateProvider.LETSENCRYPT_ROOT_CA
        fs.getPath("certs/letsencrypt.pem.config").text = "bla=no_alias"
        fs.getPath("certs/cacert.pem").text = CertificateProvider.CACERT_ROOT_CA

        and:
        classUnderTest.keystore = fs.getPath("truststore.jks")
        classUnderTest.password = "changeit"
        classUnderTest.inputDir = fs.getPath("certs")
        classUnderTest.acceptedFileEndings = ["pem"]

        when:
        classUnderTest.importCerts()

        then:
        def ks = certificateService.loadKeystore(fs.getPath("truststore.jks"), "changeit")
        assertFingerprintOfKeystoreEntry(ks, "letsencrypt.pem", CertificateProvider.LETSENCRYPT_ROOT_CA_FINGERPRINT_SHA1)
        assertFingerprintOfKeystoreEntry(ks, "cacert.pem", CertificateProvider.CACERT_ROOT_CA_FINGERPRINT_SHA1)
    }


    def "output folder is generated"() {
        given:
        def outputdir = fs.getPath("foo", "bar")
        classUnderTest.keystore = fs.getPath("foo", "bar", "truststore.jks")
        assert Files.notExists(outputdir)

        and:
        classUnderTest.password = "changeit"
        classUnderTest.inputDir = fs.getPath("certs")
        classUnderTest.acceptedFileEndings = ["pem"]

        when:
        classUnderTest.importCerts()

        then:
        Files.exists(outputdir)
    }

    def "throwing exception if password is not set"() {
        given:
        classUnderTest.inputDir = fs.getPath("certs")
        classUnderTest.keystore = fs.getPath("truststore.jks")
        classUnderTest.acceptedFileEndings = ["pem"]

        when:
        classUnderTest.importCerts()

        then:
        def e = thrown(TaskExecutionException)
        IllegalArgumentException rootCause = e.cause
        rootCause.message == "The following properties have to be configured appropriately: password"
    }

    def "throwing exception if acceptedFileEndings is not set appropriately"() {
        given:
        classUnderTest.inputDir = fs.getPath("certs")
        classUnderTest.keystore = fs.getPath("truststore.jks")
        classUnderTest.password = "changeit"

        and:
        // PropertyList<String> can't contain null values, therefore testing is not needed
        classUnderTest.acceptedFileEndings = ["", " "]

        when:
        classUnderTest.importCerts()

        then:
        def e = thrown(TaskExecutionException)
        IllegalArgumentException rootCause = e.cause
        rootCause.message == "The following properties have to be configured appropriately: acceptedFileEndings"
    }
}
