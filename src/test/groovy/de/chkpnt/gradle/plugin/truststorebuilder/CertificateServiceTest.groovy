/*
 * Copyright 2016 Gregor Dschung
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

import java.nio.file.Files
import java.nio.file.FileSystem
import java.security.KeyStore
import java.security.KeyStore.LoadStoreParameter;
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.Clock
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

import org.apache.commons.io.IOUtils;
import org.apache.tools.ant.filters.StringInputStream
import org.hamcrest.CoreMatchers;

import com.google.common.jimfs.Jimfs;

import spock.lang.Specification

class CertificateServiceTest extends Specification {

	private CertificateService classUnderTest

	private X509Certificate caCertCertificate

	private FileSystem fs;

	def setup() {
		classUnderTest = new CertificateService()

		fs = Jimfs.newFileSystem()

		def cf = CertificateFactory.getInstance("X.509");
		caCertCertificate = cf.generateCertificate(new StringInputStream(CertificateProvider.CACERT_ROOT_CA))
	}

	def "Certificate is valid if now+thresholds is before notAfter-Date"(String now, int daysInFuture, int hoursInFuture, boolean isValidExpected) {
		given:
		def nowClock = Clock.fixed(Instant.parse(now), ZoneOffset.UTC)
		classUnderTest.setClock(nowClock)

		def thresholdDuration = Duration.ofDays(daysInFuture).plusHours(hoursInFuture)

		when:
		// CAcert, Not After : Mar 29 12:29:49 2033 GMT
		def isValid = classUnderTest.isCertificateValidInFuture(caCertCertificate, thresholdDuration)

		then:
		isValid == isValidExpected

		where:
		now                       | daysInFuture | hoursInFuture | isValidExpected
		"2016-01-13T00:00:00.00Z" |           30 |             0 | true
		"2033-03-19T10:15:30.00Z" |           10 |             0 | true
		"2033-03-19T10:45:00.00Z" |           10 |             2 | false
		"2033-03-19T10:45:00.00Z" |           30 |             0 | false
		"2033-04-01T00:00:00.00Z" |           30 |             0 | false
	}

	def "Certificate can be stored in a keystore"() {
		given:
		def keystore = KeyStore.getInstance("JKS")
		keystore.load(null)

		when:
		classUnderTest.addCertificateToKeystore(keystore, caCertCertificate, "democert")

		then:
		def aliases = Collections.list keystore.aliases()
		aliases.size == 1
		aliases.get(0) == "democert"
	}

	def "Certificate can be loaded from Keystore by alias"(String aliasInKeystore, String expectedFingerprint) {
		given:
		prepareKeystore("keystore.jks")
		def file = fs.getPath("keystore.jks")
		def keystore = classUnderTest.loadKeystore(file, "changeit")

		when:
		def cert = classUnderTest.getCertificateFromKeystore(keystore, aliasInKeystore)

		then:
		def fingerprint = classUnderTest.fingerprintSha1(cert)
		fingerprint == expectedFingerprint

		where:
		aliasInKeystore | expectedFingerprint
		"cacert"        | CertificateProvider.CACERT_ROOT_CA_FINGERPRINT_SHA1
		"letsencrypt"   | CertificateProvider.LETSENCRYPT_ROOT_CA_FINGERPRINT_SHA1
	}

	def "Some entries can not be loaded from Keystore by alias"(String aliasInKeystore, Class<? extends Exception> expectedException) {
		given:
		prepareKeystore("keystore.jks")
		def file = fs.getPath("keystore.jks")
		def keystore = classUnderTest.loadKeystore(file, "changeit")

		when:
		def cert = classUnderTest.getCertificateFromKeystore(keystore, aliasInKeystore)

		then:
		thrown(expectedException)

		where:
		aliasInKeystore | expectedException
		"mysecretcert"  | UnsupportedOperationException
		"notInKeystore" | IllegalArgumentException

		// Just for the case I need it for a test at a later date:
		// the PrivateKeyEntry "mysecretcert" is protected with the password "secret"
	}

	def "Fingerprint is calculated correctly"() {
		when:
		def fingerprint = classUnderTest.fingerprintSha1(caCertCertificate)

		then:
		fingerprint == CertificateProvider.CACERT_ROOT_CA_FINGERPRINT_SHA1
	}

	def "Certificate can be loaded from filesystem"() {
		given:
		def file = fs.getPath("cacert.pem")
		file.write(CertificateProvider.CACERT_ROOT_CA)

		when:
		def cert = classUnderTest.loadCertificate(file)

		then:
		cert == caCertCertificate
	}

	def "New Keystore can be saved to the filesystem"() {
		given:
		def file = fs.getPath("keystore.jks")
		def ks = classUnderTest.newKeystore()
		assert Files.notExists(file)

		when:
		classUnderTest.storeKeystore(ks, file, "changeit")

		then:
		Files.exists(file)
	}

	private def prepareKeystore(String filename) {
		def file = fs.getPath(filename)
		file.withOutputStream { os ->
			def input = getClass().getResourceAsStream("/keystore.jks")
			IOUtils.copy(input, os)
		}
	}
}
