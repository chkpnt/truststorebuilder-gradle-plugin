package de.chkpnt.gradle.plugin.truststorebuilder

import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.Path

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException

class ImportCertsTask extends DefaultTask {
	Path keytool
	Path keystore
	String password
	List<ImportCertConfig> importCertConfigs = new ArrayList<>()

	@TaskAction
	def importCert() {
		checkTaskConfiguration()
		prepareOutputDir(keystore.getParent())

		for (def importCertConfig : importCertConfigs) {
			project.exec {
				executable keytool
				args '-importcert'
				args '-noprompt'
				args '-alias', importCertConfig.alias
				args '-file', importCertConfig.file
				args '-keystore', keystore
				args '-storepass', password
			}
		}
	}
	
	def importCert(Path file, String alias) {
		importCertConfigs.add(new ImportCertConfig(file, alias))
	}

	private def prepareOutputDir(Path outputDir) {
		if (outputDir && Files.notExists(outputDir)) {
			Files.createDirectories(outputDir)
		}
	}

	private def checkTaskConfiguration() {
		def listOfUnconfiguredProperties = []
		if (!keytool) listOfUnconfiguredProperties << 'keytool'
		if (!keystore) listOfUnconfiguredProperties << 'keystore'
		if (!password) listOfUnconfiguredProperties << 'password'

		if (listOfUnconfiguredProperties.any()) {
			def unconfiguredProperties = String.join(", ", listOfUnconfiguredProperties)
			throw new TaskExecutionException(this, new IllegalArgumentException("The following properties has to be configured: ${unconfiguredProperties}"))
		}
	}
	
	private static class ImportCertConfig {
		
		final Path file
		
		final String alias
	
		ImportCertConfig(Path file, String alias) {
			this.file = file
			this.alias = alias
		}
	
	}
}