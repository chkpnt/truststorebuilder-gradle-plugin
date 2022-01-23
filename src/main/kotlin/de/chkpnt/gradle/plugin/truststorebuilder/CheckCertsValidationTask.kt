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

import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.SourceTask
import org.gradle.api.tasks.TaskAction
import org.gradle.language.base.plugins.LifecycleBasePlugin
import java.nio.file.Path
import java.security.cert.X509Certificate
import java.time.Duration

abstract class CheckCertsValidationTask() : SourceTask() {

    @get:Input
    abstract val atLeastValidDays: Property<Int>

    @Internal
    var certificateService: CertificateService = DefaultCertificateService()

    private val atLeastValid: Duration
        get() = Duration.ofDays(atLeastValidDays.get().toLong())

    init {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Checks the validation of certificates."
    }

    @TaskAction
    fun testValidation() {
        for (file in source.files) {
            val certFile = file.toPath()
            certificateService.loadCertificates(certFile)
                .forEach { checkValidation(it, certFile) }
        }
    }

    private fun checkValidation(cert: X509Certificate, path: Path) {
        if (!certificateService.isCertificateValidInFuture(cert, atLeastValid)) {
            val relativePath = project.projectDir
                .toPath()
                .relativize(path)
            val alias = certificateService.deriveAlias(cert)
            throw TrustStoreBuilderError(relativePath, "Certificate \"$alias\" is already or becomes invalid within the next ${atLeastValid.toDays()} days")
        }
    }
}
