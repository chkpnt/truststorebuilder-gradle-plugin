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
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import java.nio.file.Path

class CheckCertsSpec(private val project: Project) {

    internal val source: Property<Path> = project.objects.property(Path::class.java)
        .convention(project.layout.projectDirectory.dir("src/main/certs").asFile.toPath())
    internal val includes: ListProperty<String> = project.objects.listProperty(String::class.java)
        .convention(listOf("**/*.crt", "**/*.cer", "**/*.pem"))
    internal val excludes: ListProperty<String> = project.objects.listProperty(String::class.java)

    /**
     * Number of days the certificates have to be at least valid. Defaults to 90 days.
     */
    var atLeastValidDays: Property<Int> = project.objects.property(Int::class.java)
        .convention(90)

    /**
     * Should the `check`-task depend on `checkCertificates<Name>`? Defaults to true.
     */
    var checkEnabled: Property<Boolean> = project.objects.property(Boolean::class.java)
        .convention(true)

    /**
     * The directory which is scanned for certificates and bundles, which is resolved using `project.file(...)`.
     *
     * Defaults to '$projectDir/src/main/certs'.
     *
     * {@see https://docs.gradle.org/current/dsl/org.gradle.api.Project.html#org.gradle.api.Project:file(java.lang.Object)}
     */
    fun source(directory: Any) {
        source.set(project.file(directory).toPath())
    }

    /**
     * Filter for the source directory.
     * Defaults to ['**&#47;*.crt', '**&#47;*.cer', '**&#47;*.pem'].
     */
    fun include(vararg patterns: String) {
        includes.addAll(patterns.toList())
    }

    /**
     * Exclusions for the source directory.
     * Defaults to [].
     */
    fun exclude(vararg patterns: String) {
        excludes.addAll(patterns.toList())
    }
}
