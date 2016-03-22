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

import static org.hamcrest.Matchers.containsInAnyOrder
import static spock.util.matcher.HamcrestSupport.that

import java.nio.file.Paths

import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Specification

class TrustStoreBuilderConfigurationTest extends Specification  {

	Project project

	TrustStoreBuilderConfiguration configuration

	def setup() {
		project = ProjectBuilder.builder().build()
		project.file('certs').mkdir()
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

	def "validation check 1"() {
		given:
		project.file('certs').delete()

		when:
		configuration.validate()

		then:
		ProjectConfigurationException e = thrown()
		e.message == "The directory 'certs', which is scanned for certificates, does not exist"
	}

	def "validation check 2"() {
		given:
		configuration.atLeastValidDays = -3

		when:
		configuration.validate()

		then:
		ProjectConfigurationException e = thrown()
		e.message == "The setting 'atLeastValidDays' has to be positive (currently -3)"
	}

	def "validation check 3"(List<String> fileEndings) {
		given:
		configuration.acceptedFileEndings = fileEndings

		when:
		configuration.validate()

		then:
		ProjectConfigurationException e = thrown()
		e.message == "The setting 'acceptedFileEndings' has to contain at least one entry"

		where:
		_ | fileEndings
		_ | null
		_ | []
		_ | ["", " ", null]
	}

	def "validation check 4"(String filename) {
		given:
		configuration.trustStoreFileName = filename

		when:
		configuration.validate()

		then:
		ProjectConfigurationException e = thrown()
		e.message == "The setting 'trustStoreFileName' is not set"

		where:
		filename | _
		""       | _
		null     | _
	}
}
