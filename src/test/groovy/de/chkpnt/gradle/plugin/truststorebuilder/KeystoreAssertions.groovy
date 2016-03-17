package de.chkpnt.gradle.plugin.truststorebuilder

import java.nio.file.Path
import java.security.KeyStore;

class KeystoreAssertions {
	
	private static CertificateService certificateService = new CertificateService()
	
	static void assertFingerprintOfKeystoreEntry(Path keystore, String password, String alias, String expectedFingerprint) {
		def ks = certificateService.loadKeystore(keystore, password)
		assertFingerprintOfKeystoreEntry(ks, alias, expectedFingerprint) 
	}
	
	static void assertFingerprintOfKeystoreEntry(KeyStore ks, String alias, String expectedFingerprint) {
		def cert = certificateService.getCertificateFromKeystore(ks, alias)
		def fingerprint = certificateService.fingerprintSha1(cert)

		assert fingerprint == expectedFingerprint
	}
}
