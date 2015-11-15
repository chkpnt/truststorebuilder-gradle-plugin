package de.chkpnt.gradle.plugin.truststorebuilder

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher
import java.nio.file.Paths

import org.gradle.api.Project
import org.gradle.api.internal.file.FileLookup
import org.gradle.api.internal.project.ProjectInternal


class TrustStoreBuilderConfiguration {
	private ProjectInternal project

	/**
	 * Path to the keytool binary. Defaults to 'keytool', which works as long as 'keytool' is available
	 * through the PATH environment variable. <br />
	 * A useful setting is {@code new File(environment['JAVA_HOME'], 'bin/keytool').canonicalPath}
	 */
	String keytool = 'keytool'

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
	 * The file name of the TrustStore to build. Defaults to 'cacerts.jks'.
	 */
	String trustStoreFileName = 'cacerts.jks'

	/**
	 * The directory which is scanned for certificates. It is relative to the project and defaults to 'certs'.
	 */
	String inputDirName = 'certs'

	/**
	 * A file being processed as a certificate has to have a file ending from this list. Defaults to  ['crt', 'cer', 'pem'].
	 */
	List<String> acceptedFileEndings = ['crt', 'cer', 'pem']

	TrustStoreBuilderConfiguration(Project project) {
		this.project = project
	}

	PathMatcher getPathMatcherForAcceptedFileEndings() {
		def extensions = String.join(',', acceptedFileEndings)
		return FileSystems.getDefault().getPathMatcher("glob:*.{$extensions}");
	}

	/**
	 * Returns a path pointing to the keytool binary used to build the TrustStore.
	 */
	Path getKeytool() {
		Paths.get(keytool)
	}

	/**
	 * Returns a path pointing to the TrustStore being built.
	 */
	Path getTrustStore() {
		project.services.get(FileLookup).getFileResolver(outputDir.toFile()).resolve(trustStoreFileName).toPath()
	}

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
	 * Returns a path pointing to the directory which is scanned for certificates.
	 */
	Path getInputDir() {
		project.file(inputDirName).toPath()
	}
}
