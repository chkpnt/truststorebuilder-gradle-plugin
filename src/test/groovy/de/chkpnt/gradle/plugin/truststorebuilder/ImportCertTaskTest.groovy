package de.chkpnt.gradle.plugin.truststorebuilder

import java.nio.file.FileSystem
import java.nio.file.Files

import org.gradle.api.Project
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.tasks.TaskExecutionException
import org.gradle.process.ExecSpec
import org.gradle.process.internal.DefaultExecAction
import org.gradle.process.internal.ExecHandle
import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Specification

import com.google.common.jimfs.Jimfs

class ImportCertTaskTest extends Specification {

	private ImportCertTask classUnderTest

	private FileSystem fs

	private ProjectInternal projectMock = Mock()

	private Project project

	def setup() {
		fs = Jimfs.newFileSystem()

		project = ProjectBuilder.builder().build()
		classUnderTest = project.task('importCert', type: ImportCertTask)

		classUnderTest.setProject(projectMock)
	}

	def "ExecHandle is correctly build"() {
		given:
		classUnderTest.keytool = fs.getPath("keytool")
		classUnderTest.file = fs.getPath("letsencrypt.pem")
		classUnderTest.alias = "Let's Encrypt Root CA"
		classUnderTest.keystore = fs.getPath("truststore.jks")
		classUnderTest.password = "changeit"

		and: 'mock for exec'
		ExecHandle expectedExecHandle
		projectMock.exec(_ as Closure) >> { Closure closure ->
			def action = new ClosureBackedAction<ExecSpec>(closure)
			def actionExec = new DefaultExecAction(ProjectBuilder.builder().build().fileResolver)

			action.execute(actionExec)
			expectedExecHandle = actionExec.build()

			return null
		}

		when:
		classUnderTest.execute()

		then:
		expectedExecHandle.command == "keytool"
		expectedExecHandle.arguments.first() == '-importcert'
		expectedExecHandle.arguments.contains('-noprompt')
		assertArgumentFollowedByValue(expectedExecHandle.arguments, '-alias', "Let's Encrypt Root CA")
		assertArgumentFollowedByValue(expectedExecHandle.arguments, '-file', 'letsencrypt.pem')
		assertArgumentFollowedByValue(expectedExecHandle.arguments, '-keystore', 'truststore.jks')
		assertArgumentFollowedByValue(expectedExecHandle.arguments, '-storepass', 'changeit')
	}

	private void assertArgumentFollowedByValue(List<String> arguments, String arg, String value) {
		def index = arguments.indexOf(arg)
		assert index != -1 : "argument '${arg}' missing"
		assert arguments.getAt(index + 1) == value : "argument '${arg}' is not followed by '${value}'"
	}

	def "output folder is generated"() {
		given:
		def outputdir = fs.getPath("foo", "bar")
		classUnderTest.keystore = fs.getPath("foo", "bar", "truststore.jks")
		assert Files.notExists(outputdir)

		and:
		classUnderTest.keytool = fs.getPath("keytool")
		classUnderTest.file = fs.getPath("letsencrypt.pem")
		classUnderTest.alias = "Let's Encrypt Root CA"
		classUnderTest.password = "changeit"

		when:
		classUnderTest.execute()

		then:
		Files.exists(outputdir)
	}

	def "throwing exception when not configured at all"() {
		when:
		classUnderTest.execute()

		then:
		TaskExecutionException e = thrown()
		IllegalArgumentException rootCause = e.cause.cause
		rootCause.message == "The following properties has to be configured: keytool, file, alias, keystore, password"
	}

	def "throwing exception some properties are not set"() {
		given:
		classUnderTest.keytool = fs.getPath("keytool")
		classUnderTest.alias = "Let's Encrypt Root CA"
		classUnderTest.keystore = fs.getPath("truststore.jks")

		when:
		classUnderTest.execute()

		then:
		TaskExecutionException e = thrown()
		IllegalArgumentException rootCause = e.cause.cause
		rootCause.message == "The following properties has to be configured: file, password"
	}
}
