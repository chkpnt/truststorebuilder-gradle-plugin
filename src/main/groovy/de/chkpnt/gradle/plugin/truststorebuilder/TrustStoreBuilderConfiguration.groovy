package de.chkpnt.gradle.plugin.truststorebuilder

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.Paths
import java.time.Duration

import org.gradle.api.Project
import org.gradle.api.internal.file.FileLookup
import org.gradle.api.internal.project.ProjectInternal

import groovy.transform.PackageScope;

@PackageScope
class TrustStoreBuilderConfiguration {
	
	private ProjectInternal project
	
	TrustStoreBuilderConfiguration(Project project) {
		this.project = project
	}

	/**
	 * The password used for the TrustStore. Defaults to 'changeit'.
	 */
	String password = 'changeit'

	/**
	 * The directory where the TrustStore is built, relative to the project. If this property is not
	 * set, the TrustStore is built in the project's build directory.
	 */
	String outputDirName = null
	
	/**
	 * Returns a path pointing to the directory where the TrustStore is built.
	 */
	Path getOutputDir() {
		if (outputDirName != null) {
			return project.file(outputDirName).toPath()
		}

		project.buildDir.toPath()
	}

	/**
	 * The file name of the TrustStore to build. Defaults to 'cacerts.jks'.
	 */
	String trustStoreFileName = 'cacerts.jks'
	
	/**
	 * Returns a path pointing to the TrustStore being built.
	 */
	Path getTrustStore() {
		project.services.get(FileLookup).getFileResolver(outputDir.toFile()).resolve(trustStoreFileName).toPath()
	}

	/**
	 * The directory which is scanned for certificates. It is relative to the project and defaults to 'certs'.
	 */
	String inputDirName = 'certs'
	
	/**
	 * Returns a path pointing to the directory which is scanned for certificates.
	 */
	Path getInputDir() {
		project.file(inputDirName).toPath()
	}

	/**
	 * A file being processed as a certificate has to have a file ending from this list. Defaults to  ['crt', 'cer', 'pem'].
	 */
	List<String> acceptedFileEndings = ['crt', 'cer', 'pem']
	
	PathMatcher getPathMatcherForAcceptedFileEndings() {
		def extensions = String.join(',', acceptedFileEndings)
		return FileSystems.getDefault().getPathMatcher("glob:*.{$extensions}");
	}
	
	/**
	 * Number of days the certificates have to be at least valid.
	 */
	int atLeastValidDays = 90
	
	Duration getAtLeastValidDays() {
		Duration.ofDays(atLeastValidDays)
	}

}
