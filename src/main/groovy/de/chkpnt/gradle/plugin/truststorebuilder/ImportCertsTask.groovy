/*
 * Copyright 2016 Gregor Dschung
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

import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyStore
import java.security.cert.X509Certificate

import org.gradle.api.DefaultTask
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskExecutionException

class ImportCertsTask extends DefaultTask {

    final Property<Path> keystore
    final Property<String> password
    final Property<Path> inputDir
    final ListProperty<String> acceptedFileEndings

    FileAdapter fileAdapter = new DefaultFileAdapter()
    CertificateService certificateService = new CertificateService()

    public ImportCertsTask() {
        keystore = getProject().getObjects().property(Path)
        password = getProject().getObjects().property(String)
        inputDir = getProject().getObjects().property(Path)
        acceptedFileEndings = getProject().getObjects().listProperty(String)

        // After updating to Gradle 5: https://github.com/gradle/gradle/issues/6108
        acceptedFileEndings.set([])
    }

    @InputDirectory
    File getInput() {
        fileAdapter.toFile(inputDir.get())
    }

    @OutputFile
    File getOutput() {
        fileAdapter.toFile(keystore.get())
    }

    @Override
    public String getDescription() {
        def inputDirName = getProject().getProjectDir()
                .toPath()
                .relativize(inputDir.get())
                .toString();
        return "Adds all certificates found under '$inputDirName' to the TrustStore."
    }

    @TaskAction
    def importCerts() {
        checkTaskConfiguration()
        prepareOutputDir(keystore.get().getParent())

        KeyStore jks = certificateService.newKeystore()

        List<Path> certFiles = PathScanner.scanForFilesWithFileEnding(inputDir.get(), acceptedFileEndings.get())
        for (certFile in certFiles) {
            String alias = getCertAlias(certFile)
            X509Certificate cert = certificateService.loadCertificate(certFile)
            certificateService.addCertificateToKeystore(jks, cert, alias)
        }

        certificateService.storeKeystore(jks, keystore.get(), password.get())
    }

    private def prepareOutputDir(Path outputDir) {
        if (outputDir && Files.notExists(outputDir)) {
            Files.createDirectories(outputDir)
        }
    }

    private def checkTaskConfiguration() {
        def listOfImproperConfiguredProperties = []
        if (!keystore.getOrNull()) listOfImproperConfiguredProperties << 'keystore'
        if (!password.getOrNull()) listOfImproperConfiguredProperties << 'password'
        if (acceptedFileEndings.getOrElse([]).findAll { it?.trim() }.empty ) listOfImproperConfiguredProperties << 'acceptedFileEndings'

        if (listOfImproperConfiguredProperties.any()) {
            def improperConfiguredProperties = String.join(", ", listOfImproperConfiguredProperties)
            throw new TaskExecutionException(this, new IllegalArgumentException("The following properties have to be configured appropriately: ${improperConfiguredProperties}"))
        }
    }

    private static String getCertAlias(Path certFile) {
        def filename = certFile.fileName.toString()
        def configFile = certFile.resolveSibling("${filename}.config")

        if (!Files.exists(configFile)) {
            return filename
        }

        Properties properties = new Properties()
        try {
            InputStream inputStream = Files.newInputStream(configFile)
            properties.load(inputStream)
        } catch (IOException e) {
            throw new UncheckedIOException(e)
        }

        def alias = properties.getProperty("alias")
        return alias ?: filename.toString()
    }
}
