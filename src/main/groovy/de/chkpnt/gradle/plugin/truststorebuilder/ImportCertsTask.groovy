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

import java.nio.file.Files
import java.nio.file.Path
import java.security.cert.X509Certificate

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException

class ImportCertsTask extends DefaultTask {

	Path keystore

	String password

	Path inputDir

	List<ImportCertConfig> importCertConfigs = new ArrayList<>()

	FileAdapter fileAdapter = new DefaultFileAdapter()

	CertificateService certificateService = new CertificateService()

	@InputDirectory
	File getInputDir() {
		fileAdapter.toFile(inputDir)
	}

	@OutputFile
	File getOutputFile() {
		fileAdapter.toFile(keystore)
	}

	@TaskAction
	def importCert() {
		checkTaskConfiguration()
		prepareOutputDir(keystore.getParent())

		def ks = certificateService.newKeystore()
		for (def importCertConfig : importCertConfigs) {
			certificateService.addCertificateToKeystore(ks, importCertConfig.cert, importCertConfig.alias)
		}
		certificateService.storeKeystore(ks, keystore, password)
	}

	def importCert(Path file, String alias) {
		def cert = certificateService.loadCertificate(file)
		importCertConfigs.add(new ImportCertConfig(cert, alias))
	}

	private def prepareOutputDir(Path outputDir) {
		if (outputDir && Files.notExists(outputDir)) {
			Files.createDirectories(outputDir)
		}
	}

	private def checkTaskConfiguration() {
		def listOfUnconfiguredProperties = []
		if (!keystore) listOfUnconfiguredProperties << 'keystore'
		if (!password) listOfUnconfiguredProperties << 'password'

		if (listOfUnconfiguredProperties.any()) {
			def unconfiguredProperties = String.join(", ", listOfUnconfiguredProperties)
			throw new TaskExecutionException(this, new IllegalArgumentException("The following properties has to be configured: ${unconfiguredProperties}"))
		}
	}

	private static class ImportCertConfig {

		final X509Certificate cert

		final String alias

		ImportCertConfig(X509Certificate cert, String alias) {
			this.cert = cert
			this.alias = alias
		}
	}
}
