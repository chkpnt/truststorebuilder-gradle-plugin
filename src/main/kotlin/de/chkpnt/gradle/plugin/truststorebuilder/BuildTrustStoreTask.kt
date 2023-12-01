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
import java.security.KeyStore
import java.security.cert.X509Certificate

// I do not inherit from SourceTask (which would provide source and includes for free),
// as I'd like to print the source directory in the task's description. With SourceTask,
// I'd only have access to the FileTree, which doesn't have a root directory.
// TODO: Gregor, 2021-01-23: Rethink...

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
        val sourceDirName =
            project.projectDir
                .toPath()
                .relativize(source.get())
                .toString()
        return "Adds all certificates found under '$sourceDirName' to the TrustStore."
    }

    @TaskAction
    fun buildTrustStore() {
        prepareOutputDir(trustStorePath.get().parent)

        val type = checkNotNull(trustStorePath.get().keyStoreType()) // is ensured to be not null via TrustStoreSpec
        val keystore = certificateService.newKeyStore(type)

        val patterns = PatternSet().include(includes.get())
        for (file in project.fileTree(source.get()).matching(patterns)) {
            val certFile = file.toPath()
            val certificates = certificateService.loadCertificates(certFile)
            importCertificatesInto(keystore, certificates)
        }

        certificateService.storeKeystore(keystore, trustStorePath.get(), trustStorePassword.get())
    }

    private fun prepareOutputDir(outputDir: Path?) {
        if (outputDir != null && Files.notExists(outputDir)) {
            Files.createDirectories(outputDir)
        }
    }

    private fun importCertificatesInto(
        keystore: KeyStore,
        certs: List<X509Certificate>,
    ) {
        for (cert in certs) {
            val alias = getCertAlias(cert)
            if (certificateService.containsAlias(keystore, alias)) {
                project.logger.warn("Certificate $alias exists multiple times in source")
            }
            certificateService.addCertificateToKeystore(keystore, cert, alias)
        }
    }

    private fun getCertAlias(cert: X509Certificate): String {
        return certificateService.deriveAlias(cert)
    }
}
