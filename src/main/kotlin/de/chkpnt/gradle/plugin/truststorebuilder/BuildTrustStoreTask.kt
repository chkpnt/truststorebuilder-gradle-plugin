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
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Console
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import java.util.Properties

interface FileAdapter {
    fun toFile(path: Path): File
}

class DefaultFileAdapter : FileAdapter {
    override fun toFile(path: Path): File = path.toFile()
}

open class BuildTrustStoreTask() : DefaultTask() {

    @OutputFile
    val trustStore: Property<Path> = project.objects.property(Path::class.java)
    @Input
    val password: Property<String> = project.objects.property(String::class.java)
    @InputDirectory
    val inputDir: Property<Path> = project.objects.property(Path::class.java)
    @Input
    val acceptedFileEndings: ListProperty<String> = project.objects.listProperty(String::class.java)

    @Internal
    var fileAdapter: FileAdapter = DefaultFileAdapter()
    @Internal
    var certificateService: CertificateService = DefaultCertificateService()

    @InputDirectory
    fun getInput(): File {
        return fileAdapter.toFile(inputDir.get())
    }

    @OutputFile
    fun getOutput(): File {
        return fileAdapter.toFile(trustStore.get())
    }

    @Console
    override fun getDescription(): String {
        val inputDirName = project.projectDir
            .toPath()
            .relativize(inputDir.get())
            .toString()
        return "Adds all certificates found under '$inputDirName' to the TrustStore."
    }

    @TaskAction
    fun importCerts() {
        checkTaskConfiguration()
        prepareOutputDir(trustStore.get().parent)

        val jks = certificateService.newKeystore()

        val certFiles = PathScanner.scanForFilesWithFileEnding(inputDir.get(), acceptedFileEndings.get())
        for (certFile in certFiles) {
            val alias = getCertAlias(certFile)
            val cert = certificateService.loadCertificate(certFile)
            certificateService.addCertificateToKeystore(jks, cert, alias)
        }

        certificateService.storeKeystore(jks, trustStore.get(), password.get())
    }

    private fun prepareOutputDir(outputDir: Path?) {
        if (outputDir != null && Files.notExists(outputDir)) {
            Files.createDirectories(outputDir)
        }
    }

    private fun checkTaskConfiguration() {
        val listOfImproperConfiguredProperties = mutableListOf<String>()
        if (!trustStore.isPresent) listOfImproperConfiguredProperties.add("keystore")
        if (!password.isPresent) listOfImproperConfiguredProperties.add("password")
        if (acceptedFileEndings.getOrElse(emptyList()).filterNot { it.isBlank() }.isEmpty()) listOfImproperConfiguredProperties.add("acceptedFileEndings")

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

internal object PathScanner {

    fun scanForFilesWithFileEnding(path: Path, endsWith: List<String>): List<Path> {
        val extensions = endsWith.joinToString(",")
        val endsWithMatcher = FileSystems.getDefault()
            .getPathMatcher("glob:*.{$extensions}")

        val certs = ArrayList<Path>()

        Files.walkFileTree(
            path,
            object : SimpleFileVisitor<Path>() {

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    super.visitFile(file, attrs)

                    val filename = file.fileName
                    if (endsWithMatcher.matches(filename)) {
                        certs.add(file)
                    }

                    return FileVisitResult.CONTINUE
                }
            }
        )

        return certs
    }
}
