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
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.api.tasks.util.PatternSet
import java.nio.file.Files
import java.nio.file.Path
import java.util.Properties

// I do not inherit from SourceTask (which would provide source and includes for free),
// as I'd like to print the source directory in the task's description. With SourceTask,
// I'd only have access to the FileTree, which doesn't have a root directory.

abstract class BuildTrustStoreTask() : DefaultTask() {

    private var trustStore: TrustStoreSpec = TrustStoreSpec(project)

    @get:OutputFile
    internal val trustStorePath: Property<Path>
        get() = trustStore.path

    @get:Input
    internal val trustStorePassword: Property<String>
        get() = trustStore.password

    @get:InputDirectory
    internal abstract val source: Property<Path>

    @get:Input
    internal abstract val includes: ListProperty<String>

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

    fun trustStore(action: Action<TrustStoreSpec>) {
        action.execute(trustStore)
    }

    internal fun trustStore(spec: TrustStoreSpec) {
        trustStore = spec
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
     * Filter for the source directory.
     * Defaults to ['**&#47;*.crt', '**&#47;*.cer', '**&#47;*.pem'].
     */
    fun include(patterns: Iterable<String>) {
        includes.set(patterns)
    }

    @TaskAction
    fun importCerts() {
        checkTaskConfiguration()
        prepareOutputDir(trustStore.path.get().parent)

        val jks = certificateService.newKeystore()

        val patterns = PatternSet().include(includes.get())
        project.fileTree(source.get()).matching(patterns).forEach {
            val certFile = it.toPath()
            val alias = getCertAlias(certFile)
            val cert = certificateService.loadCertificate(certFile)
            certificateService.addCertificateToKeystore(jks, cert, alias)
        }

        certificateService.storeKeystore(jks, trustStore.path.get(), trustStore.password.get())
    }

    private fun prepareOutputDir(outputDir: Path?) {
        if (outputDir != null && Files.notExists(outputDir)) {
            Files.createDirectories(outputDir)
        }
    }

    private fun checkTaskConfiguration() {
        val listOfImproperConfiguredProperties = mutableListOf<String>()
        if (trustStorePassword.getOrElse("").isBlank()) listOfImproperConfiguredProperties.add("password")
        if (includes.getOrElse(emptyList()).filterNot { it.isBlank() }.isEmpty()) listOfImproperConfiguredProperties.add("include")

        if (listOfImproperConfiguredProperties.any()) {
            val improperConfiguredProperties = listOfImproperConfiguredProperties.joinToString(separator = ", ")
            throw TaskExecutionException(this, IllegalArgumentException("The following properties have to be configured appropriately: $improperConfiguredProperties"))
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
