package de.chkpnt.gradle.plugin.truststorebuilder

import java.nio.file.Path
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.time.Duration

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import java.security.cert.X509Certificate

open class CheckCertsValidationTask() : DefaultTask() {

    val inputDir: Property<Path> = project.objects.property(Path::class.java)
    val acceptedFileEndings: ListProperty<String> = project.objects.listProperty(String::class.java)
    val atLeastValidDays: Property<Integer> = project.objects.property(Integer::class.java)

    var certificateService: CertificateService = DefaultCertificateService()
    var certificateFactory = CertificateFactory.getInstance("X.509")

    init {
        // After updating to Gradle 5: https://github.com/gradle/gradle/issues/6108
        acceptedFileEndings.set(emptyList())
    }

    private val INVALID_REASON: String
        get() = "Certificate is already or becomes invalid within the next ${atLeastValid.toDays()} days"

    val atLeastValid: Duration
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

        if (! certificateService.isCertificateValidInFuture(cert, atLeastValid)) {
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

data class CheckCertValidationError(val file: Path, val reason: String): GradleException() {

    override val message: String? = "$reason: $file"

}