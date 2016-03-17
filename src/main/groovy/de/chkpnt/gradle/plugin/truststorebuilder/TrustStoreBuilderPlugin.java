package de.chkpnt.gradle.plugin.truststorebuilder;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.ProjectConfigurationException;
import org.gradle.api.plugins.BasePlugin;

public class TrustStoreBuilderPlugin implements Plugin<Project> {

	private static final String TRUSTSTOREBUILDER_EXTENSION_NAME = "trustStoreBuilder";
	
	private static final String BUILD_TRUSTSTORE_TASK_NAME = "buildTrustStore";

	@Override
	public void apply(Project project) {
		project.getPluginManager().apply(BasePlugin.class);

		TrustStoreBuilderConfiguration configuration = project.getExtensions().create(TRUSTSTOREBUILDER_EXTENSION_NAME, TrustStoreBuilderConfiguration.class, project);

		configureImportCertsTask(project, configuration);
	}
	
	private void configureImportCertsTask(Project project, TrustStoreBuilderConfiguration configuration) {
		ImportCertsTask importCertsTask = project.getTasks().create(BUILD_TRUSTSTORE_TASK_NAME, ImportCertsTask.class);
		importCertsTask.setGroup(BasePlugin.BUILD_GROUP);
		importCertsTask.setDescription(String.format("Adds all certificates found under '%s' to the TrustStore.", configuration.getInputDirName()));
		importCertsTask.setKeystore(configuration.getTrustStore());
		importCertsTask.setPassword(configuration.getPassword());
		importCertsTask.setInputDir(configuration.getInputDir());
		
		try {
			scanForCertsToImport(configuration.getInputDir(), configuration.getPathMatcherForAcceptedFileEndings(), importCertsTask);
		} catch (IOException e) {
			throw new ProjectConfigurationException("Configuration of ImportCertTasks failed", e);
		}
	}
	
	private void scanForCertsToImport(Path path, PathMatcher acceptedFileEndings, ImportCertsTask importCertsTask) throws IOException {
		Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				super.visitFile(file, attrs);

				Path filename = file.getFileName();
				if (acceptedFileEndings.matches(filename)) {
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