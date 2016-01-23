package de.chkpnt.gradle.plugin.truststorebuilder

import java.nio.file.Path
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate
import java.time.Duration;
import java.util.stream.Collectors;

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction;
import org.gradle.api.tasks.TaskExecutionException

class TestCertValidationTask extends DefaultTask {
	
	List<Path> files = new ArrayList<>()
	
	Duration atLeastValidDays
	
	CertificateService certificateService = new CertificateService()
	
	CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
	
	private def INVALID_REASON = { -> "Certificate is already or becomes invalid within the next ${atLeastValidDays.toDays()} days" }
	
	@TaskAction
	def testValidation() {
		for (Path file : files) {
			checkValidation file
		}
	}
	
	private def checkValidation(Path file) {
		def cert = loadX509Certificate file
		
		if (! certificateService.isCertificateValidInFuture(cert, atLeastValidDays)) {
			throw new TestCertValidationAssertionError(file, INVALID_REASON())
		}
	}
				
	private def loadX509Certificate(Path file) {
		def certStream = file.newInputStream()

		try {
			return certificateFactory.generateCertificate(certStream)
		} catch (CertificateException e) {
			throw new TestCertValidationAssertionError(file, "Could not load certificate")
		}
	}

	def file(Path file) {
		files.add(file)
	}
	
	def setAtLeastValidDays(int days) {
		atLeastValidDays = Duration.ofDays days
	}
	
}
