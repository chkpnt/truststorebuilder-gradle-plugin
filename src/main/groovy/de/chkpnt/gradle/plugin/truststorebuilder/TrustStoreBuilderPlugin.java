package de.chkpnt.gradle.plugin.truststorebuilder;

import groovy.util.ConfigObject;
import groovy.util.ConfigSlurper;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.Properties;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.ProjectConfigurationException;
import org.gradle.api.internal.AbstractTask;
import org.gradle.api.plugins.BasePlugin;

public class TrustStoreBuilderPlugin implements Plugin<Project> {

	private static final String TRUSTSTOREBUILDER_EXTENSION_NAME = "trustStoreBuilder";
	private static final String BUILD_TRUSTSTORE_TASK_NAME = "buildTrustStore";

	private Path projectDir;

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(BasePlugin.class);

		projectDir = project.getProjectDir().toPath();

		TrustStoreBuilderConfiguration configuration = project.getExtensions().create(TRUSTSTOREBUILDER_EXTENSION_NAME, TrustStoreBuilderConfiguration.class, project);

		project.afterEvaluate(p -> {
			ImportCertsTask importCertsTask = project.getTasks().create(BUILD_TRUSTSTORE_TASK_NAME, ImportCertsTask.class);
			importCertsTask.setGroup("TrustStore");
			importCertsTask.setDescription(String.format("Adds all certificates found under '%s' to the TrustStore.", configuration.getInputDirName()));
			importCertsTask.setKeytool(configuration.getKeytool());
			importCertsTask.setKeystore(configuration.getTrustStore());
			importCertsTask.setPassword(configuration.getPassword());
			importCertsTask.setInputDir(configuration.getInputDir());
			
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
					Optional<Properties> certConfig = getCertConfig(file);
					String alias = getAlias(certConfig, filename.toString());
                    
					importCertsTask.importCert(file, alias);
				}

				return FileVisitResult.CONTINUE;
			}
		});
	}
	
	private String getAlias(Optional<Properties> config, String defaultAlias) {
		if (!config.isPresent()) {
			return defaultAlias;
		}
		
		return config.get().getProperty("alias", defaultAlias);
	}



	private Optional<Properties> getCertConfig(Path certFile) throws IOException {
		Optional<Path> configFile = getConfigFileForCertificate(certFile);
		if (!configFile.isPresent()) {
			return Optional.empty();
		}
		
		InputStream inputStream = Files.newInputStream(configFile.get());
		Properties properties = new Properties();
		properties.load(inputStream);
		return Optional.of(properties);
	}
	
	private Optional<Path> getConfigFileForCertificate(Path certFile) {
		String certFilename = certFile.getFileName().toString();
		Path configFile = certFile.resolveSibling(certFilename + ".config");

		if (!Files.exists(configFile)) {
			return Optional.empty();
		}

		return Optional.of(configFile);
	}
}