/*
 * Copyright 2016 - 2022 Gregor Dschung
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

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternSet
import java.nio.file.Files
import java.nio.file.Path
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.Duration

abstract class CheckCertsValidationTask() : DefaultTask() {

    @get:InputDirectory
    internal abstract val source: Property<Path>
    @get:Input
    internal abstract val includes: ListProperty<String>
    @get:Input
    internal abstract val atLeastValidDays: Property<Int>

    @Internal
    var certificateService: CertificateService = DefaultCertificateService()
    @Internal
    var certificateFactory = CertificateFactory.getInstance("X.509")

    private val INVALID_REASON: String
        get() = "Certificate is already or becomes invalid within the next ${atLeastValid.toDays()} days"

    private val atLeastValid: Duration
        get() = Duration.ofDays(atLeastValidDays.get().toLong())

    /**
     * The directory which is scanned for certificates. Defaults to '$projectDir/src/main/certs'.
     */
    fun source(directory: Any) {
        source.set(project.file(directory).toPath())
    }

    /**
     * Filter for the source directory.
     * Defaults to ['**&#47;*.crt', '**&#47;*.cer', '**&#47;*.pem'].
     */
    fun include(vararg patterns: String) {
        includes.set(patterns.toList())
    }

    /**
     * Filter for the source directory.
     * Defaults to ['**&#47;*.crt', '**&#47;*.cer', '**&#47;*.pem'].
     */
    fun include(patterns: Iterable<String>) {
        includes.set(patterns)
    }

    /**
     * Number of days the certificates have to be at least valid. Defaults to 90 days.
     */
    fun atLeastValidDays(numberOfDays: Int) {
        atLeastValidDays.set(numberOfDays)
    }

    @TaskAction
    fun testValidation() {
        val patterns = PatternSet().include(includes.get())
        project.fileTree(source.get()).matching(patterns).forEach {
            val certFile = it.toPath()
            checkValidation(certFile)
        }
    }

    private fun checkValidation(file: Path) {
        val cert = loadX509Certificate(file)

        if (!certificateService.isCertificateValidInFuture(cert, atLeastValid)) {
            val relativePath = project.projectDir
                .toPath()
                .relativize(file)
            throw CheckCertValidationError(relativePath, INVALID_REASON)
        }
    }

    private fun loadX509Certificate(file: Path): X509Certificate {
        Files.newInputStream(file).use { inputStream ->
            try {
                return certificateFactory.generateCertificate(inputStream) as X509Certificate
            } catch (e: CertificateException) {
                val relativePath = project.projectDir
                    .toPath()
                    .relativize(file)
                throw CheckCertValidationError(relativePath, "Could not load certificate")
            }
        }
    }
}

data class CheckCertValidationError(val file: Path, val reason: String) : GradleException() {

    override val message: String? = "$reason: $file"
}
