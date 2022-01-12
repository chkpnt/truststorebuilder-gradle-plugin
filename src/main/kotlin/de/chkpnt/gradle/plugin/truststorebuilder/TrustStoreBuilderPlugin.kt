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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.BasePlugin
import org.gradle.language.base.plugins.LifecycleBasePlugin

class TrustStoreBuilderPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.pluginManager
            .apply(BasePlugin::class.java)

        val extension = project.extensions
            .create(TRUSTSTOREBUILDER_EXTENSION_NAME, TrustStoreBuilderExtension::class.java, project)

        project.tasks
            .register(CHECK_CERTS_TASK_NAME, CheckCertsValidationTask::class.java) { task ->
                task.group = LifecycleBasePlugin.VERIFICATION_GROUP
                task.description = "Checks the validation of the certificates to import."

                task.inputDir.set(extension.inputDirPath)
                task.acceptedFileEndings.set(extension.acceptedFileEndings)
                task.atLeastValidDays.set(extension.atLeastValidDays)
            }

        project.tasks
            .named(LifecycleBasePlugin.CHECK_TASK_NAME).configure { task ->
                if (extension.checkEnabled) {
                    task.dependsOn(CHECK_CERTS_TASK_NAME)
                }
            }

        project.tasks
            .register(BUILD_TRUSTSTORE_TASK_NAME, ImportCertsTask::class.java) { task ->
                task.group = BasePlugin.BUILD_GROUP

                task.keystore.set(extension.trustStorePath)
                task.password.set(extension.password)
                task.inputDir.set(extension.inputDirPath)
                task.acceptedFileEndings.set(extension.acceptedFileEndings)
            }

        project.tasks
            .named(LifecycleBasePlugin.BUILD_TASK_NAME) { task ->
                if (extension.buildEnabled) {
                    task.dependsOn(BUILD_TRUSTSTORE_TASK_NAME)
                }
            }
    }

    companion object {

        private val TRUSTSTOREBUILDER_EXTENSION_NAME = "trustStoreBuilder"
        private val BUILD_TRUSTSTORE_TASK_NAME = "buildTrustStore"
        private val CHECK_CERTS_TASK_NAME = "checkCertificates"
    }
}
