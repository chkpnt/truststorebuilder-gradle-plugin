package de.chkpnt.gradle.plugin.truststorebuilder

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile

class BuildTrustStoreTask extends DefaultTask {
	@OutputFile
	File truststore

	@InputDirectory
	File inputDir
}
