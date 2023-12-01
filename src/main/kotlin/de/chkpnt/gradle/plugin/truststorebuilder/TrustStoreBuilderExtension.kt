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
     * Configures and registers the task `buildTrustStore`.
     */
    fun trustStore(action: Action<TrustStoreSpec>) {
        trustStore("", action)
    }

    /**
     * Configures and registers the task `buildTrustStore<Name>`.
     */
    fun trustStore(
        name: String,
        action: Action<TrustStoreSpec>,
    ) {
        val trustStoreSpec = TrustStoreSpec(project)
        action.execute(trustStoreSpec)
        trustStoreSpec.check()
        registerBuildTrustStoreTask(name, trustStoreSpec)
    }

    /**
     * Configures and registers the task `checkCertificates`.
     */
    fun checkCertificates(action: Action<CheckCertsSpec>) {
        val checkCertsSpec = CheckCertsSpec(project)
        action.execute(checkCertsSpec)
        registerCheckCertificatesTask(checkCertsSpec)
    }

    private fun registerBuildTrustStoreTask(
        name: String,
        spec: TrustStoreSpec,
    ) {
        val buildTaskName = "$BUILD_TRUSTSTORE_TASK_NAME_PREFIX${name.replaceFirstChar(Char::titlecase)}"
        val buildTask =
            project.tasks
                .register(buildTaskName, BuildTrustStoreTask::class.java) { task ->
                    task.trustStorePath.set(spec.path)
                    task.trustStorePassword.set(spec.password)
                    task.source.set(spec.source)
                    task.includes.set(spec.includes)
                }

        project.tasks
            .named(LifecycleBasePlugin.BUILD_TASK_NAME) { task ->
                if (spec.buildEnabled.get()) {
                    task.dependsOn(buildTask)
                }
            }
    }

    private fun registerCheckCertificatesTask(spec: CheckCertsSpec) {
        val checkTaskName = "$CHECK_CERTS_TASK_NAME"
        val checkTask =
            project.tasks
                .register(checkTaskName, CheckCertsValidationTask::class.java) { task ->
                    task.source(spec.source.get())
                    task.include(spec.includes.get())
                    task.exclude(spec.excludes.get())
                    task.atLeastValidDays.set(spec.atLeastValidDays)
                }

        project.tasks
            .named(LifecycleBasePlugin.CHECK_TASK_NAME).configure { task ->
                if (spec.checkEnabled.get()) {
                    task.dependsOn(checkTask)
                }
            }
    }

    companion object {
        private const val BUILD_TRUSTSTORE_TASK_NAME_PREFIX = "buildTrustStore"
        private const val CHECK_CERTS_TASK_NAME = "checkCertificates"
    }
}
