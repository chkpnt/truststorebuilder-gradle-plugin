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

import org.gradle.api.Project
import java.nio.file.Path

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

    /**
     * Should the `check`-task depend on `checkCertificates`? Defaults to true.
     */
    var checkEnabled: Boolean = true

    /**
     * Should the `build`-task depend on `buildTrustStore`? Defaults to true.
     */
    var buildEnabled: Boolean = true
}
