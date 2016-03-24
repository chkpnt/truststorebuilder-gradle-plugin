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

package de.chkpnt.gradle.plugin.truststorebuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.ProjectConfigurationException;
import org.gradle.api.Task;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.api.plugins.JavaBasePlugin;

public class TrustStoreBuilderPlugin implements Plugin<Project> {

	private static final String TRUSTSTOREBUILDER_EXTENSION_NAME = "trustStoreBuilder";

	private static final String BUILD_TRUSTSTORE_TASK_NAME = "buildTrustStore";

	private static final String CHECK_CERTS_TASK_NAME = "checkCertificates";

	@Override
	public void apply(Project project) {
		project.getPluginManager()
			.apply(BasePlugin.class);

		TrustStoreBuilderConfiguration configuration = project.getExtensions()
			.create(TRUSTSTOREBUILDER_EXTENSION_NAME, TrustStoreBuilderConfiguration.class, project);

		configuration.validate();

		try {
			configureTasks(project, configuration);
		} catch (IOException e) {
			throw new ProjectConfigurationException("Configuration of ImportCertTasks failed", e);
		}
	}

	private static void configureTasks(Project project, TrustStoreBuilderConfiguration configuration) throws IOException {
		CheckCertsValidationTask checkCertsValidationTask = createCheckCertsValidationTask(project, configuration);
		ImportCertsTask importCertsTask = createImportCertsTask(project, configuration);

		configureDependency(project, JavaBasePlugin.CHECK_TASK_NAME, checkCertsValidationTask);
		configureDependency(project, JavaBasePlugin.BUILD_TASK_NAME, importCertsTask);

		List<Path> certs = scanForCertsToImport(configuration.getInputDir(), configuration.getPathMatcherForAcceptedFileEndings(), importCertsTask);

		certs.forEach(cert -> {
			Path filename = cert.getFileName();
			String alias = getCertConfig(cert).map(config -> config.getProperty("alias"))
				.orElseGet(() -> filename.toString());
			importCertsTask.importCert(cert, alias);
			checkCertsValidationTask.file(cert);
		});
	}

	private static void configureDependency(Project project, String taskName, Task dependsOn) {
		project.getTasks()
			.getByName(taskName)
			.dependsOn(dependsOn);
	}

	private static ImportCertsTask createImportCertsTask(Project project, TrustStoreBuilderConfiguration configuration) {
		ImportCertsTask task = project.getTasks()
			.create(BUILD_TRUSTSTORE_TASK_NAME, ImportCertsTask.class);
		task.setGroup(BasePlugin.BUILD_GROUP);
		task.setDescription(String.format("Adds all certificates found under '%s' to the TrustStore.", configuration.getInputDirName()));
		task.setKeystore(configuration.getTrustStore());
		task.setPassword(configuration.getPassword());
		task.setInputDir(configuration.getInputDir());
		return task;
	}

	private static CheckCertsValidationTask createCheckCertsValidationTask(Project project, TrustStoreBuilderConfiguration configuration) {
		CheckCertsValidationTask task = project.getTasks()
			.create(CHECK_CERTS_TASK_NAME, CheckCertsValidationTask.class);
		task.setGroup(JavaBasePlugin.VERIFICATION_GROUP);
		task.setDescription("Checks the validation of the certificates to import.");
		task.setAtLeastValidDays(configuration.getAtLeastValidDays());
		return task;
	}

	private static List<Path> scanForCertsToImport(Path path, PathMatcher acceptedFileEndings, ImportCertsTask importCertsTask) throws IOException {
		List<Path> certs = new ArrayList<>();

		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				super.visitFile(file, attrs);

				Path filename = file.getFileName();
				if (acceptedFileEndings.matches(filename)) {
					certs.add(file);
				}

				return FileVisitResult.CONTINUE;
			}
		});

		return certs;
	}

	private static Optional<Properties> getCertConfig(Path certFile) {
		Optional<Path> configFile = getConfigFileForCertificate(certFile);
		if (!configFile.isPresent()) {
			return Optional.empty();
		}

		Properties properties = new Properties();

		try {
			InputStream inputStream = Files.newInputStream(configFile.get());
			properties.load(inputStream);
		} catch (IOException e) {
			new UncheckedIOException(e);
		}

		return Optional.of(properties);
	}

	private static Optional<Path> getConfigFileForCertificate(Path certFile) {
		String certFilename = certFile.getFileName()
			.toString();
		Path configFile = certFile.resolveSibling(certFilename + ".config");

		if (!Files.exists(configFile)) {
			return Optional.empty();
		}

		return Optional.of(configFile);
	}
}
