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

import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.nio.file.Path

open class TrustStoreBuilderExtension(private val project: Project) {

    internal val trustStore: TrustStoreSpec = TrustStoreSpec(project)

    internal val source: Property<Path> = project.objects.property(Path::class.java)
        .convention(project.layout.projectDirectory.dir("src/main/certs").asFile.toPath())
    internal val includes: ListProperty<String> = project.objects.listProperty(String::class.java)
        .convention(listOf("**/*.crt", "**/*.cer", "**/*.pem"))

    internal val atLeastValidDays: Property<Int> = project.objects.property(Int::class.java)
        .convention(90)
    internal val checkEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(true)
    internal val buildEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(true)

    fun trustStore(action: Action<TrustStoreSpec>) {
        action.execute(trustStore)
    }

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
     * Number of days the certificates have to be at least valid. Defaults to 90 days.
     */
    fun atLeastValidDays(value: Int) {
        atLeastValidDays.set(value)
    }

    /**
     * Should the `check`-task depend on `checkCertificates`? Defaults to true.
     */
    fun checkEnabled(value: Boolean) {
        checkEnabled.set(value)
    }

    /**
     * Should the `build`-task depend on `buildTrustStore`? Defaults to true.
     */
    fun buildEnabled(value: Boolean) {
        buildEnabled.set(value)
    }
}
