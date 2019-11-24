package de.chkpnt.gradle.plugin.truststorebuilder

import java.nio.file.Path

import org.gradle.api.Project

open class TrustStoreBuilderExtension(project: Project) {

    private val project: Project = project

    /**
     * The password used for the TrustStore. Defaults to 'changeit'.
     */
    var password: String = "changeit"

    /**
     * Path pointing to the TrustStore being built. Defaults to '$buildDir/cacerts.jks'.
     */
    var trustStore: Any? = null
    val trustStorePath: Path
        get() {
            val _trustStore = trustStore
            return if (_trustStore != null) {
                project.file(_trustStore).toPath()
            } else {
                project.buildDir.toPath().resolve("cacerts.jks")
            }
        }

    /**
     * The directory which is scanned for certificates. Defaults to '$projectDir/src/main/certs'.
     */
    var inputDir: Any? = null
    val inputDirPath: Path
        get() {
            val _inputDir = inputDir
            return if (_inputDir != null) {
                project.file(_inputDir).toPath()
            } else {
                project.file("src/main/certs").toPath()
            }
        }

    /**
     * A file being processed as a certificate has to have a file ending from this list. Defaults to  ['crt', 'cer', 'pem'].
     */
    var acceptedFileEndings: List<String> = mutableListOf("crt", "cer", "pem")

    /**
     * Number of days the certificates have to be at least valid. Defaults to 90 days.
     */
    var atLeastValidDays: Int = 90
}