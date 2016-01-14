package de.chkpnt.gradle.plugin.truststorebuilder

import java.security.cert.X509Certificate
import java.time.Clock;
import java.time.Duration
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;


class CertificateService {
	Clock clock = Clock.systemDefaultZone()

	boolean isCertificateValidInFuture(X509Certificate cert, Duration duration) {
		def notAfterInstant = cert.notAfter.toInstant()
		def notAfter = LocalDateTime.ofInstant(notAfterInstant, ZoneOffset.UTC)
		
		def thresholdDateTime = LocalDateTime.now(clock).plus(duration)
		
		thresholdDateTime.isBefore(notAfter)
	}
}
