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

package de.chkpnt.gradle.plugin.truststorebuilder

import java.nio.file.FileSystems
import java.nio.file.Path
import java.nio.file.PathMatcher

import org.gradle.api.PathValidation
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.internal.project.ProjectInternal

class TrustStoreBuilderExtension {

	private ProjectInternal project

	TrustStoreBuilderExtension(Project project) {
		this.project = project
	}

	/**
	 * The password used for the TrustStore. Defaults to 'changeit'.
	 */
	String password = 'changeit'

	/**
	 * Path pointing to the TrustStore being built. Defaults to '$buildDir/cacerts.jks'.
	 */
	Path trustStore

	void setTrustStore(Object file) {
		trustStore = project.file(file).toPath()
	}

	Path getTrustStore() {
		if (trustStore == null) {
			return project.buildDir.toPath().resolve('cacerts.jks')
		}
		return trustStore
	}

	/**
	 * The directory which is scanned for certificates. Defaults to '$projectDir/src/main/certs'.
	 */
	Path inputDir

	void setInputDir(Object dir) {
		inputDir = project.file(dir, PathValidation.DIRECTORY).toPath()
	}

	Path getInputDir() {
		if (inputDir == null) {
			return project.file('src/main/certs', PathValidation.DIRECTORY).toPath()
		}
		return inputDir
	}

	/**
	 * A file being processed as a certificate has to have a file ending from this list. Defaults to  ['crt', 'cer', 'pem'].
	 */
	List<String> acceptedFileEndings = ['crt', 'cer', 'pem']

	PathMatcher getPathMatcherForAcceptedFileEndings() {
		def extensions = String.join(',', acceptedFileEndings)
		return FileSystems.getDefault().getPathMatcher("glob:*.{$extensions}")
	}

	/**
	 * Number of days the certificates have to be at least valid. Defaults to 90 days.
	 */
	int atLeastValidDays = 90


	void validate() {
		if (atLeastValidDays < 1) {
			throw new ProjectConfigurationException("The setting 'atLeastValidDays' has to be positive (currently $atLeastValidDays)", null)
		}
		if (!acceptedFileEndings || acceptedFileEndings.empty
		|| acceptedFileEndings.findAll { it?.trim() }.empty ) {
			throw new ProjectConfigurationException("The setting 'acceptedFileEndings' has to contain at least one entry", null)
		}
	}
}
