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

import org.gradle.api.DefaultTask
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternSet
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

// I do not inherit from SourceTask (which would provide source and includes for free),
// as I'd like to print the source directory in the task's description. With SourceTask,
// I'd only have access to the FileTree, which doesn't have a root directory.

abstract class BuildTrustStoreTask() : DefaultTask() {

    @get:OutputFile
    abstract val trustStorePath: Property<Path>

    @get:Input
    abstract val trustStorePassword: Property<String>

    @get:InputDirectory
    abstract val source: Property<Path>

    @get:Input
    abstract val includes: ListProperty<String>

    @Internal
    var certificateService: CertificateService = DefaultCertificateService()

    init {
        group = BasePlugin.BUILD_GROUP
    }

    @Console
    override fun getDescription(): String {
        val sourceDirName = project.projectDir
            .toPath()
            .relativize(source.get())
            .toString()
        return "Adds all certificates found under '$sourceDirName' to the TrustStore."
    }

    @TaskAction
    fun importCerts() {
        prepareOutputDir(trustStorePath.get().parent)

        val jks = certificateService.newKeystore()

        val patterns = PatternSet().include(includes.get())
        project.fileTree(source.get()).matching(patterns).forEach {
            val certFile = it.toPath()
            val alias = getCertAlias(certFile)
            val cert = certificateService.loadCertificate(certFile)
            certificateService.addCertificateToKeystore(jks, cert, alias)
        }

        certificateService.storeKeystore(jks, trustStorePath.get(), trustStorePassword.get())
    }

    private fun prepareOutputDir(outputDir: Path?) {
        if (outputDir != null && Files.notExists(outputDir)) {
            Files.createDirectories(outputDir)
        }
    }

    companion object {

        private fun getCertAlias(certFile: Path): String {
            val filename = certFile.fileName.toString()
            val configFile = certFile.resolveSibling("$filename.config")

            if (!Files.exists(configFile)) {
                return filename
            }

            val properties = Properties()
            val inputStream = Files.newInputStream(configFile)
            properties.load(inputStream)

            val alias = properties.getProperty("alias")
            return alias ?: filename.toString()
        }
    }
}
