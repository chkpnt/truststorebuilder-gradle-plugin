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
import org.gradle.language.base.plugins.LifecycleBasePlugin

open class TrustStoreBuilderExtension(private val project: Project) {

    /**
     * Configures and registers the tasks `buildTrustStore` and `checkCertificates`.
     */
    fun trustStore(action: Action<TrustStoreSpec>) {
        trustStore("", action)
    }

    /**
     * Configures and registers the tasks `buildTrustStore<Name>` and `checkCertificates<Name>`.
     */
    fun trustStore(name: String, action: Action<TrustStoreSpec>) {
        val trustStoreSpec = TrustStoreSpec(project)
        action.execute(trustStoreSpec)
        trustStoreSpec.check()
        registerTasks(name, trustStoreSpec)
    }

    private fun registerTasks(name: String, trustStoreSpec: TrustStoreSpec) {
        val buildTaskName = "$BUILD_TRUSTSTORE_TASK_NAME_PREFIX${name.replaceFirstChar(Char::titlecase)}"
        val buildTask = project.tasks
            .register(buildTaskName, BuildTrustStoreTask::class.java) { task ->
                task.trustStorePath.set(trustStoreSpec.path)
                task.trustStorePassword.set(trustStoreSpec.password)
                task.source.set(trustStoreSpec.source)
                task.includes.set(trustStoreSpec.includes)
            }

        val checkTaskName = "$CHECK_CERTS_TASK_NAME_PREFIX${name.replaceFirstChar(Char::titlecase)}"
        val checkTask = project.tasks
            .register(checkTaskName, CheckCertsValidationTask::class.java) { task ->
                task.source.set(trustStoreSpec.source)
                task.includes.set(trustStoreSpec.includes)
                task.atLeastValidDays.set(trustStoreSpec.atLeastValidDays)
            }

        project.tasks
            .named(LifecycleBasePlugin.BUILD_TASK_NAME) { task ->
                if (trustStoreSpec.buildEnabled.get()) {
                    task.dependsOn(buildTask)
                }
            }

        project.tasks
            .named(LifecycleBasePlugin.CHECK_TASK_NAME).configure { task ->
                if (trustStoreSpec.checkEnabled.get()) {
                    task.dependsOn(checkTask)
                }
            }
    }

    companion object {

        private const val BUILD_TRUSTSTORE_TASK_NAME_PREFIX = "buildTrustStore"
        private const val CHECK_CERTS_TASK_NAME_PREFIX = "checkCertificates"
    }
}
