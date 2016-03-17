package de.chkpnt.gradle.plugin.truststorebuilder

import static KeystoreAssertions.*

import java.nio.file.FileSystem
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore

import com.google.common.jimfs.Configuration;

import org.gradle.api.Project
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.process.ExecSpec
import org.gradle.process.internal.DefaultExecAction
import org.gradle.process.internal.ExecHandle
import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Specification

import com.google.common.jimfs.Jimfs

class ImportCertsTaskTest extends Specification {

	private ImportCertsTask classUnderTest

	private FileSystem fs
	
	private ProjectInternal projectMock = Mock()

	private Project project
	
	private CertificateService certificateService = new CertificateService()
	
	private FileAdapter fileAdapter = new FileAdapter() {
		@Override
		File toFile(Path path) {
			def temp = Files.isDirectory(path) ? Files.createTempDirectory(null)
											   : Files.createTempFile(null, null)
			def file = temp.toFile()
			file.deleteOnExit()
			return file
		}
	}

	def setup() {
		fs = Jimfs.newFileSystem(Configuration.unix())
		Files.createDirectory(fs.getPath("certs"))
		fs.getPath("certs/letsencrypt.pem").write(CertificateProvider.LETSENCRYPT_ROOT_CA)
		fs.getPath("certs/cacert.pem").write(CertificateProvider.CACERT_ROOT_CA)

		project = ProjectBuilder.builder().build()
		classUnderTest = project.task('importCert', type: ImportCertsTask)

		classUnderTest.project = projectMock
		classUnderTest.fileAdapter = fileAdapter
	}

	def "ImportCertsTask works awesome"() {
		given:
		classUnderTest.keystore = fs.getPath("truststore.jks")
		classUnderTest.password = "changeit"
		classUnderTest.inputDir = fs.getPath("certs")
		classUnderTest.importCert(fs.getPath("certs/letsencrypt.pem"), "Let's Encrypt Root CA")
		classUnderTest.importCert(fs.getPath("certs/cacert.pem"), "CAcert Root CA")

		when:
		classUnderTest.execute()

		then:
		def ks = certificateService.loadKeystore(fs.getPath("truststore.jks"), "changeit")
		assertFingerprintOfKeystoreEntry(ks, "Let's Encrypt Root CA", CertificateProvider.LETSENCRYPT_ROOT_CA_FINGERPRINT_SHA1)
		assertFingerprintOfKeystoreEntry(ks, "cacert root ca", CertificateProvider.CACERT_ROOT_CA_FINGERPRINT_SHA1)
	}
	

	def "output folder is generated"() {
		given:
		def outputdir = fs.getPath("foo", "bar")
		classUnderTest.keystore = fs.getPath("foo", "bar", "truststore.jks")
		assert Files.notExists(outputdir)

		and:
		classUnderTest.password = "changeit"
		classUnderTest.inputDir = fs.getPath("certs")

		when:
		classUnderTest.execute()

		then:
		Files.exists(outputdir)
	}

	def "throwing exception some properties are not set"() {
		given:
		classUnderTest.inputDir = fs.getPath("certs")
		classUnderTest.keystore = fs.getPath("truststore.jks")

		when:
		classUnderTest.execute()

		then:
		TaskExecutionException e = thrown()
		IllegalArgumentException rootCause = e.cause.cause
		rootCause.message == "The following properties has to be configured: password"
	}
}
