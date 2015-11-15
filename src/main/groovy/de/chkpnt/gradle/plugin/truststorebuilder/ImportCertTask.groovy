package de.chkpnt.gradle.plugin.truststorebuilder

import java.nio.file.Path
import java.nio.file.Files
import java.nio.file.Path

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException

class ImportCertTask extends DefaultTask {
	Path keytool
	Path file
	String alias
	Path keystore
	String password

	@TaskAction
	def importCert() {
		checkTaskConfiguration()
		prepareOutputDir(keystore.getParent())

		project.exec {
			executable keytool
			args '-importcert'
			args '-noprompt'
			args '-alias', alias
			args '-file', file
			args '-keystore', keystore
			args '-storepass', password
		}
	}

	private def prepareOutputDir(Path outputDir) {
		if (outputDir && Files.notExists(outputDir)) {
			Files.createDirectories(outputDir)
		}
	}

	private def checkTaskConfiguration() {
		def listOfUnconfiguredProperties = []
		if (!keytool) listOfUnconfiguredProperties << 'keytool'
		if (!file) listOfUnconfiguredProperties << 'file'
		if (!alias) listOfUnconfiguredProperties << 'alias'
		if (!keystore) listOfUnconfiguredProperties << 'keystore'
		if (!password) listOfUnconfiguredProperties << 'password'

		if (listOfUnconfiguredProperties.any()) {
			def unconfiguredProperties = String.join(", ", listOfUnconfiguredProperties)
			throw new TaskExecutionException(this, new IllegalArgumentException("The following properties has to be configured: ${unconfiguredProperties}"))
		}
	}
}