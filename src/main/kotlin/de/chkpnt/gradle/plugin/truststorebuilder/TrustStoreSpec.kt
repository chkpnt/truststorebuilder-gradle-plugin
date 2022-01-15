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
import org.gradle.api.provider.Property
import java.nio.file.Path

class TrustStoreSpec(private val project: Project) {

    internal val path: Property<Path> = project.objects.property(Path::class.java)
        .convention(project.layout.buildDirectory.file("cacerts.jks").map { it.asFile.toPath() })
    internal val password: Property<String> = project.objects.property(String::class.java)
        .convention("changeit")

    /**
     * Path pointing to the TrustStore being built.
     * Can be anything that can be handled by `project.file(...)`.
     * Defaults to '$buildDir/cacerts.jks'
     *
     * @see https://docs.gradle.org/current/javadoc/org/gradle/api/Project.html#file-java.lang.Object-
     */
    fun path(value: Any) {
        path.set(project.file(value).toPath())
    }

    /**
     * The password used for the TrustStore. Defaults to 'changeit'.
     */
    fun password(value: String) {
        password.set(value)
    }
}
