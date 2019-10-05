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

import java.nio.file.Path
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.time.Duration

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.TaskAction

class CheckCertsValidationTask extends DefaultTask {

    final Property<Path> inputDir
    final ListProperty<String> acceptedFileEndings
    final Property<Integer> atLeastValidDays

    CertificateService certificateService = new CertificateService()
    CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");

    private def INVALID_REASON = { -> "Certificate is already or becomes invalid within the next ${atLeastValid.toDays()} days" }

    public CheckCertsValidationTask() {
        inputDir = getProject().getObjects().property(Path)
        acceptedFileEndings = getProject().getObjects().listProperty(String)
        atLeastValidDays = getProject().getObjects().property(Integer)

        // After updating to Gradle 5: https://github.com/gradle/gradle/issues/6108
        acceptedFileEndings.set([])
    }

    Duration getAtLeastValid() {
        return Duration.ofDays(atLeastValidDays.get())
    }

    @TaskAction
    def testValidation() {
        List<Path> certFiles = PathScanner.scanForFilesWithFileEnding(inputDir.get(), acceptedFileEndings.get())
        for (certFile in certFiles) {
            checkValidation certFile
        }
    }

    private def checkValidation(Path file) {
        def cert = loadX509Certificate file

        if (! certificateService.isCertificateValidInFuture(cert, atLeastValid)) {
            throw new CheckCertValidationError(file, INVALID_REASON())
        }
    }

    private def loadX509Certificate(Path file) {
        def certStream = file.newInputStream()

        try {
            return certificateFactory.generateCertificate(certStream)
        } catch (CertificateException e) {
            throw new CheckCertValidationError(file, "Could not load certificate")
        }
    }
}
