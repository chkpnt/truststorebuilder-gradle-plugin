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

package de.chkpnt.gradle.plugin.truststorebuilder;

import static de.chkpnt.gradle.plugin.truststorebuilder.KeystoreAssertions.*
import static org.gradle.testkit.runner.TaskOutcome.*

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.gradle.testkit.runner.GradleRunner
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Specification

class TrustStoreBuilderPluginTest extends Specification {

	@Rule
	final TemporaryFolder testProjectDir = new TemporaryFolder();

	private File buildFile

	private List<File> pluginClasspath

	def setup() {
		initProjectDir();

		// Was needed when I wrote the tests with Gradle 2. Still needed?
		def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
		if (pluginClasspathResource == null) {
			throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
		}

		pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }
	}

	private def initProjectDir() {
		copyCertsToProjectDir()
		buildFile = testProjectDir.newFile('build.gradle')
		buildFile << """
			plugins {
				id 'de.chkpnt.truststorebuilder'
			}

			trustStoreBuilder {
				//acceptedFileEndings = ['bla']
			}
		"""
	}

	private def copyCertsToProjectDir() {
		def dest = testProjectDir.newFolder('src', 'main', 'certs').toPath();

		def certsFolder = getClass().getClassLoader().getResource("certs")
		def source = Paths.get(certsFolder.toURI())

		new AntBuilder().copy(toDir: dest) { fileset(dir: source) }
	}

	def "buildTrustStore and checkCertificates tasks are included in task-list"() {
		when:
		def result = buildGradleRunner("tasks", "--all").build()

		then:
		result.output.contains("buildTrustStore - Adds all certificates found")
		result.output.contains("checkCertificates - Checks the validation of the certificates")
	}

	def "buildTrustStore task builds a TrustStore"() {
		when:
		def result = buildGradleRunner("buildTrustStore").build()

		then:
		result.task(":buildTrustStore").outcome == SUCCESS
		Path trustStore = getDefaultTrustStorePath()

		assertNumberOfEntriesInKeystore(trustStore, "changeit", 2)
		assertFingerprintOfKeystoreEntry(trustStore, "changeit", "Let's Encrypt Root CA", CertificateProvider.LETSENCRYPT_ROOT_CA_FINGERPRINT_SHA1)
		assertFingerprintOfKeystoreEntry(trustStore, "changeit", "CAcert Root CA", CertificateProvider.CACERT_ROOT_CA_FINGERPRINT_SHA1)
	}

	def "buildTrustStore task respects configuration (inputDir)"() {
		given:
		def origPath = Paths.get(testProjectDir.getRoot().getPath(), "src/main/certs")
		def newPath = Paths.get(testProjectDir.getRoot().getPath(), "src/main/x509")
		Files.move(origPath, newPath)

		and:
		buildFile.text = """
			plugins {
				id 'de.chkpnt.truststorebuilder'
			}

			trustStoreBuilder {
				inputDir = 'src/main/x509'
			}
		"""

		when:
		def result = buildGradleRunner("buildTrustStore").build()

		then:
		result.task(":buildTrustStore").outcome == SUCCESS
		Path trustStore = getDefaultTrustStorePath()

		assertNumberOfEntriesInKeystore(trustStore, "changeit", 2)
		assertFingerprintOfKeystoreEntry(trustStore, "changeit", "Let's Encrypt Root CA", CertificateProvider.LETSENCRYPT_ROOT_CA_FINGERPRINT_SHA1)
		assertFingerprintOfKeystoreEntry(trustStore, "changeit", "CAcert Root CA", CertificateProvider.CACERT_ROOT_CA_FINGERPRINT_SHA1)
	}

	def "buildTrustStore task respects configuration (acceptedFileEndings)"() {
		// Textfixtures contains 'isrgrootx1.pem' and 'root.crt'
		given:
		buildFile.text = """
			plugins {
				id 'de.chkpnt.truststorebuilder'
			}

			trustStoreBuilder {
				acceptedFileEndings = ['crt']
			}
		"""

		when:
		def result = buildGradleRunner("buildTrustStore").build()

		then:
		result.task(":buildTrustStore").outcome == SUCCESS
		assertNumberOfEntriesInKeystore(getDefaultTrustStorePath(), "changeit", 1)
	}

	def "alias for certificate is filename if config file is missing"() {
		given:
		def configfile = Paths.get("src/main/certs/CAcert/root.crt.config")
		Files.delete(testProjectDir.getRoot().toPath().resolve(configfile))

		when:
		def result = buildGradleRunner("buildTrustStore").build()

		then:
		result.task(":buildTrustStore").outcome == SUCCESS
		assertFingerprintOfKeystoreEntry(getDefaultTrustStorePath(), "changeit", "root.crt", CertificateProvider.CACERT_ROOT_CA_FINGERPRINT_SHA1)
	}

	def "alias for certificate is filename if config file contains no alias"() {
		given:
		def configfile = Paths.get("src/main/certs/CAcert/root.crt.config")
		def file = testProjectDir.getRoot().toPath().resolve(configfile)
		file.write("foobar")

		when:
		def result = buildGradleRunner("buildTrustStore").build()

		then:
		result.task(":buildTrustStore").outcome == SUCCESS
		assertFingerprintOfKeystoreEntry(getDefaultTrustStorePath(), "changeit", "root.crt", CertificateProvider.CACERT_ROOT_CA_FINGERPRINT_SHA1)
	}

	private Path getDefaultTrustStorePath() {
		Path keystore = Paths.get(testProjectDir.root.getPath(), "build/cacerts.jks")
		assert Files.exists(keystore)
		return keystore
	}

	private def GradleRunner buildGradleRunner(String... tasks) {
		GradleRunner.create()
				.withDebug(true)
				.withProjectDir(testProjectDir.root)
				.withArguments(tasks)
				.withPluginClasspath(pluginClasspath)
	}
}
