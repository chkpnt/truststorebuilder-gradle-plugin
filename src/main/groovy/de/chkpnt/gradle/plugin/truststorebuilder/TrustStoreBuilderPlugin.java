package de.chkpnt.gradle.plugin.truststorebuilder;

import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.ProjectConfigurationException;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.plugins.BasePlugin;

public class TrustStoreBuilderPlugin implements Plugin<Project> {

	private static final String TRUSTSTOREBUILDER_EXTENSION_NAME = "trustStoreBuilder";
	private static final String BUILD_TRUSTSTORE_TASK_NAME = "buildTrustStore";
	private static final String IMPORT_CERTS_TASK_NAME = "importCerts";

	private Path projectDir;

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(BasePlugin.class);

		projectDir = project.getProjectDir().toPath();

		TrustStoreBuilderConfiguration configuration = project.getExtensions().create(TRUSTSTOREBUILDER_EXTENSION_NAME, TrustStoreBuilderConfiguration.class, project);

		project.afterEvaluate(p -> {
			BuildTrustStoreTask buildTruststoreTask = p.getTasks().create(BUILD_TRUSTSTORE_TASK_NAME, BuildTrustStoreTask.class);
			buildTruststoreTask.setGroup(BasePlugin.BUILD_GROUP);
			buildTruststoreTask.setDescription(String.format("Adds all certificates found under '%s' to the TrustStore.", configuration.getInputDirName()));
			buildTruststoreTask.setTruststore(configuration.getTrustStore().toFile());
			buildTruststoreTask.setInputDir(configuration.getInputDir().toFile());

			ImportCertsTask importCertsTask = project.getTasks().create(IMPORT_CERTS_TASK_NAME, ImportCertsTask.class);
			importCertsTask.setGroup("TrustStore");
			importCertsTask.setDescription(String.format("Adds all certificates found under '%s' to the TrustStore.", configuration.getInputDirName()));
			importCertsTask.setKeytool(configuration.getKeytool());
			importCertsTask.setKeystore(configuration.getTrustStore());
			importCertsTask.setPassword(configuration.getPassword());
			
			buildTruststoreTask.dependsOn(importCertsTask);
			
			try {
				configureImportCertsTask(importCertsTask, configuration);
			} catch (IOException e) {
				throw new ProjectConfigurationException("Configuration of ImportCertTasks failed", e);
			}

		});
	}

	private void configureImportCertsTask(ImportCertsTask importCertsTask, TrustStoreBuilderConfiguration configuration) throws IOException {
		Files.walkFileTree(configuration.getInputDir(), new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				super.visitFile(file, attrs);

				Path filename = file.getFileName();
				if (configuration.getPathMatcherForAcceptedFileEndings().matches(filename)) {
					Path configFile = getConfigFileForCertificate(file);
					ConfigObject configObject = parseCertConfigFile(configFile);

					importCertsTask.importCert(filename, (String) configObject.get("alias"));
				}

				return FileVisitResult.CONTINUE;
			}
		});
	}

	private Path getConfigFileForCertificate(Path certFile) {
		String certFilename = certFile.getFileName().toString();
		Path configFile = certFile.resolveSibling(certFilename + ".config");

		if (!Files.exists(configFile)) {
			String message = String.format("Configuration of ImportCertTasks failed: \"%s\" missing", projectDir.relativize(configFile));
			throw new ProjectConfigurationException(message, null);
		}

		return configFile;
	}

	private ConfigObject parseCertConfigFile(Path configFile) throws MalformedURLException {
		ConfigSlurper configSlurper = new ConfigSlurper();
		ConfigObject configObject = configSlurper.parse(configFile.toUri().toURL());

		if (!configObject.isSet("alias")) {
			String message = String.format("Configuration of ImportCertTasks failed: \"alias\" missing in file \"%s\"", projectDir.relativize(configFile));
			throw new ProjectConfigurationException(message, null);
		}

		return configObject;
	}
}