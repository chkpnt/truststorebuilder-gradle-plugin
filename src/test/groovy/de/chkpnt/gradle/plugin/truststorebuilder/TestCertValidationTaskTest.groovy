package de.chkpnt.gradle.plugin.truststorebuilder

import java.nio.file.FileSystem;

import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.api.tasks.StopExecutionException;
import org.gradle.api.tasks.TaskExecutionException;
import org.gradle.testfixtures.ProjectBuilder

import com.google.common.jimfs.Jimfs;

import spock.lang.Specification

class TestCertValidationTaskTest extends Specification {

	private TestCertValidationTask classUnderTest

	private CertificateService certificateServiceMock = Mock()

	private FileSystem fs
	
	private Project project

	def setup() {
		fs = Jimfs.newFileSystem()
		fs.getPath("cacert.pem").write(CertificateProvider.CACERT_ROOT_CA)
		fs.getPath("letsencrypt.pem").write(CertificateProvider.LETSENCRYPT_ROOT_CA)
		fs.getPath("corrupt.pem").write(CertificateProvider.CORRUPT)
		fs.getPath("notACert.txt").write(CertificateProvider.NOT_A_CERT)
		
		project = ProjectBuilder.builder().build()
		classUnderTest = project.task('testCertValidation', type: TestCertValidationTask)
		
		classUnderTest.certificateService = certificateServiceMock
	}
	
	def "loading a corrupt certificate"() {
		given:
		classUnderTest.atLeastValidDays = 30
		classUnderTest.file fs.getPath("corrupt.pem")
		
		when:
		classUnderTest.execute()
		
		then:
		def e = thrown(TaskExecutionException)
		e.cause instanceof TestCertValidationAssertionError
		e.cause.message == "Could not load certificate: corrupt.pem"
	}
	
	def "loading a non-certificate"() {
		given:
		classUnderTest.atLeastValidDays = 30
		classUnderTest.file fs.getPath("notACert.txt")
		
		when:
		classUnderTest.execute()
		
		then:
		def e = thrown(TaskExecutionException)
		e.cause instanceof TestCertValidationAssertionError
		e.cause.message == "Could not load certificate: notACert.txt"
	}
	
	def "certificate letsencrypt.pem is invalid"() {
		given:
		classUnderTest.atLeastValidDays = 30
		classUnderTest.file fs.getPath("cacert.pem")
		classUnderTest.file fs.getPath("letsencrypt.pem")
		
		and:
		certificateServiceMock.isCertificateValidInFuture(_, _) >>> [true, false]
		
		when:
		classUnderTest.execute()
		
		then:
		def e = thrown(TaskExecutionException)
		e.cause instanceof TestCertValidationAssertionError
		e.cause.message == "Certificate is already or becomes invalid within the next 30 days: letsencrypt.pem"
	}
	
	def "when all certificates are valid nothing happens"() {
		given:
		classUnderTest.atLeastValidDays = 30
		classUnderTest.file fs.getPath("cacert.pem")
		classUnderTest.file fs.getPath("letsencrypt.pem")
		
		and:
		certificateServiceMock.isCertificateValidInFuture(_, _) >> true
		
		when:
		classUnderTest.execute()
		
		then:
		true
	}
}
