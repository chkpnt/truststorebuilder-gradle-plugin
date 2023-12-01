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

class TrustStoreBuilderPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.pluginManager
            .apply(BasePlugin::class.java)

        project.extensions
            .create(TRUSTSTOREBUILDER_EXTENSION_NAME, TrustStoreBuilderExtension::class.java, project)
    }

    companion object {
        private const val TRUSTSTOREBUILDER_EXTENSION_NAME = "trustStoreBuilder"
    }
}
