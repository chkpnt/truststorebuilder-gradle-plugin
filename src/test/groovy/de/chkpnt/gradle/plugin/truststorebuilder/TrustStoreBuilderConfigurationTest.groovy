package de.chkpnt.gradle.plugin.truststorebuilder

import static org.hamcrest.Matchers.containsInAnyOrder
import static spock.util.matcher.HamcrestSupport.that

import java.nio.file.Paths

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Specification

class TrustStoreBuilderConfigurationTest extends Specification  {
	Project project

	TrustStoreBuilderConfiguration configuration

	def setup() {
		project = ProjectBuilder.builder().build()
		configuration = new TrustStoreBuilderConfiguration(project)
	}

	def "check default values"() {
		expect:
		configuration.password == 'changeit'
		configuration.trustStoreFileName == 'cacerts.jks'
		configuration.trustStore == project.file('build/cacerts.jks').toPath()
		configuration.outputDir == project.file('build').toPath()
		configuration.inputDir == project.file('certs').toPath()
		that configuration.acceptedFileEndings, containsInAnyOrder(*['cer', 'crt', 'pem'])
	}

	def "TrustStore relative to build dir"() {
		when:
		project.buildDir = 'mybuild'
		configuration.trustStoreFileName = 'truststore.jks'

		then:
		configuration.outputDir == project.file('mybuild').toPath()
		configuration.trustStore == project.file('mybuild/truststore.jks').toPath()
	}

	def "TrustStore with explicit input and output dir"() {
		when:
		configuration.trustStoreFileName = 'truststore.jks'
		configuration.outputDirName = 'src/main/resources'
		configuration.inputDirName = 'src/main/resources/certs'

		then:
		configuration.outputDir == project.file('src/main/resources').toPath()
		configuration.inputDir == project.file('src/main/resources/certs').toPath()
		configuration.trustStore == project.file('src/main/resources/truststore.jks').toPath()
	}

	def "accept additional file ending"() {
		when:
		configuration.acceptedFileEndings << 'der'

		then:
		that configuration.acceptedFileEndings, containsInAnyOrder(*['cer', 'crt', 'pem', 'der'])
	}

	def "change accepted file endings"() {
		when:
		configuration.acceptedFileEndings = ['txt']

		then:
		configuration.acceptedFileEndings.size() == 1
		configuration.acceptedFileEndings.first() == 'txt'
	}

	def "test PathMatcher for accepted file endings"() {
		expect:
		configuration.pathMatcherForAcceptedFileEndings.matches(Paths.get('foo.cer'))
		configuration.pathMatcherForAcceptedFileEndings.matches(Paths.get('foo.crt'))
		configuration.pathMatcherForAcceptedFileEndings.matches(Paths.get('foo.pem'))
		! configuration.pathMatcherForAcceptedFileEndings.matches(Paths.get('foo.txt'))
	}
}
