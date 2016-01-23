package de.chkpnt.gradle.plugin.truststorebuilder

import java.nio.file.Path
import java.time.Duration

class TestCertValidationAssertionError extends AssertionError {
	
	Path file
	
	String reason
	
	TestCertValidationAssertionError(Path file, String reason) {
		this.file = file
		this.reason = reason
	}
	
	@Override
	public String getMessage() {
		return "${reason}: ${file}"
	}
}
