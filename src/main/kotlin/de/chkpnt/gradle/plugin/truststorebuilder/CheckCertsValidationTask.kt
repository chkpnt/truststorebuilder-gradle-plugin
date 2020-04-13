/*
 * Copyright 2016 - 2020 Gregor Dschung
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
import java.nio.file.Files
import java.nio.file.Path
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.time.Duration

open class CheckCertsValidationTask() : DefaultTask() {

    @InputDirectory
    val inputDir: Property<Path> = project.objects.property(Path::class.java)
    @Input
    val acceptedFileEndings: ListProperty<String> = project.objects.listProperty(String::class.java)
    @Input
    val atLeastValidDays: Property<Integer> = project.objects.property(Integer::class.java)

    @Internal
    var certificateService: CertificateService = DefaultCertificateService()
    @Internal
    var certificateFactory = CertificateFactory.getInstance("X.509")

    init {
        // After updating to Gradle 5: https://github.com/gradle/gradle/issues/6108
        acceptedFileEndings.set(emptyList())
    }

    private val INVALID_REASON: String
        get() = "Certificate is already or becomes invalid within the next ${atLeastValid.toDays()} days"

    val atLeastValid: Duration
        @Input
        get() = Duration.ofDays(atLeastValidDays.get().toLong())

    @TaskAction
    fun testValidation() {
        val certFiles = PathScanner.scanForFilesWithFileEnding(inputDir.get(), acceptedFileEndings.get())
        for (certFile in certFiles) {
            checkValidation(certFile)
        }
    }

    private fun checkValidation(file: Path) {
        val cert = loadX509Certificate(file)

        if (!certificateService.isCertificateValidInFuture(cert, atLeastValid)) {
            throw CheckCertValidationError(file, INVALID_REASON)
        }
    }

    private fun loadX509Certificate(file: Path): X509Certificate {
        Files.newInputStream(file).use { inputStream ->
            try {
                return certificateFactory.generateCertificate(inputStream) as X509Certificate
            } catch (e: CertificateException) {
                throw CheckCertValidationError(file, "Could not load certificate")
            }
        }
    }
}

data class CheckCertValidationError(val file: Path, val reason: String) : GradleException() {

    override val message: String? = "$reason: $file"
}