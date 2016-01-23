package de.chkpnt.gradle.plugin.truststorebuilder

import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.Clock
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset;

import org.apache.tools.ant.filters.StringInputStream

import spock.lang.Specification

class CertificateServiceTest extends Specification {

	private CertificateService classUnderTest

	private X509Certificate demoCertificate

	def setup() {
		classUnderTest = new CertificateService()

		def cf = CertificateFactory.getInstance("X.509");
		demoCertificate = cf.generateCertificate(new StringInputStream(CertificateProvider.CACERT_ROOT_CA))
	}

	def "Certificate is valid if now+thresholds is before notAfter-Date"(String now, int daysInFuture, int hoursInFuture, boolean isValidExpected) {
		given:
		def nowClock = Clock.fixed(Instant.parse(now), ZoneOffset.UTC)
		classUnderTest.setClock(nowClock)
		
		def thresholdDuration = Duration.ofDays(daysInFuture).plusHours(hoursInFuture)
		
		when:
		// CAcert, Not After : Mar 29 12:29:49 2033 GMT
		def isValid = classUnderTest.isCertificateValidInFuture(demoCertificate, thresholdDuration)
		
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
}
