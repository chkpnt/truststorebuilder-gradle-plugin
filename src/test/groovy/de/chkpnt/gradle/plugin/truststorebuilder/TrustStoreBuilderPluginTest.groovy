package de.chkpnt.gradle.plugin.truststorebuilder;

import static org.gradle.testkit.runner.TaskOutcome.*

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Specification

class TrustStoreBuilderPluginTest extends Specification {

	@Rule
	final TemporaryFolder testProjectDir = new TemporaryFolder();

	private File buildFile

	private List<File> pluginClasspath

	private static String FINGERPRINT_CACERT_ROOT_CA="13:5C:EC:36:F4:9C:B8:E9:3B:1A:B2:70:CD:80:88:46:76:CE:8F:33"

	private static String FINGERPRINT_LETSENCRYPT_ROOT_CA="CA:BD:2A:79:A1:07:6A:31:F2:1D:25:36:35:CB:03:9D:43:29:A5:E8"

	def setup() {
		initProjectDir();

		def pluginClasspathResource = getClass().classLoader.findResource("plugin-classpath.txt")
		if (pluginClasspathResource == null) {
			throw new IllegalStateException("Did not find plugin classpath resource, run `testClasses` build task.")
		}

		pluginClasspath = pluginClasspathResource.readLines().collect { new File(it) }
	}

	private def initProjectDir() {
		copyCertsToProjectDir()
		buildFile = testProjectDir.newFile('build.gradle')
	}

	private def copyCertsToProjectDir() {
		def dest = testProjectDir.newFolder('certs').toPath();

		def certsFolder = getClass().getClassLoader().getResource("certs")
		def source = Paths.get(certsFolder.toURI())

		new AntBuilder().copy(toDir: dest) { fileset(dir: source) }
	}

	def "buildTrustStore task is included in task-list"() {
		given:
		buildFile << """
            plugins {
                id 'de.chkpnt.truststorebuilder'
            }
        """

		when:
		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments("tasks", "--all")
				.withPluginClasspath(pluginClasspath)
				.build()

		then:
		result.output.contains("buildTrustStore - Adds all certificates found")
	}

	def "buildTrustStore task builds a TrustStore"() {
		given:
		buildFile << """
            plugins {
                id 'de.chkpnt.truststorebuilder'
            }
        """

		when:
		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments("buildTrustStore")
				.withPluginClasspath(pluginClasspath)
				.build()

		then:
		result.task(":buildTrustStore").outcome == SUCCESS
		Path keystore = Paths.get(testProjectDir.root.getPath(), "build/cacerts.jks")
		Files.exists(keystore)
		assertKeystoreContainsTestCerts(keystore)
	}

	private def void assertKeystoreContainsTestCerts(Path keystore) {
		def keytoolCmd = "keytool -list -keystore $keystore -storepass changeit"
		def process = keytoolCmd.execute()

		assert process.waitFor() == 0

		def output = process.getText()

		assert output.contains(FINGERPRINT_CACERT_ROOT_CA)
		assert output.contains(FINGERPRINT_LETSENCRYPT_ROOT_CA)
	}

	def "missing config file for certificate"() {
		given:
		Files.delete(testProjectDir.getRoot().toPath().resolve("certs/CAcert/root.crt.config"))
		buildFile << """
            plugins {
                id 'de.chkpnt.truststorebuilder'
            }
        """

		when:
		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments("buildTrustStore")
				.withPluginClasspath(pluginClasspath)
				.buildAndFail()

		then:
		result.output.contains('Configuration of ImportCertTasks failed: "certs\\CAcert\\root.crt.config" missing')
	}

	def "buildTrustStore task is included in task-list with pre Gradle 2.8"() {
		given:
		def classpathString = pluginClasspath
				.collect { it.absolutePath.replace('\\', '\\\\') } // escape backslashes in Windows paths
				.collect { "'$it'" }
				.join(", ")

		buildFile << """
            buildscript {
                dependencies {
                    classpath files($classpathString)
                }
            }
            apply plugin: "de.chkpnt.truststorebuilder"
        """

		when:
		def result = GradleRunner.create()
				.withProjectDir(testProjectDir.root)
				.withArguments('tasks', '--all')
				.withGradleVersion("2.7")
				.build()

		then:
		result.output.contains('buildTrustStore - Adds all certificates found')
	}
}
