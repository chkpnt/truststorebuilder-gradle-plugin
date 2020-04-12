/*
 * Copyright 2016 - 2020 Gregor Dschung
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

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Specification

class TrustStoreBuilderExtensionTest extends Specification  {

    Project project

    TrustStoreBuilderExtension classUnderTest

    def setup() {
        project = ProjectBuilder.builder().build()
        project.file('src/main/certs').mkdirs()
        classUnderTest = new TrustStoreBuilderExtension(project)
    }

    def "check default values"() {
        expect:
        classUnderTest.password == 'changeit'
        classUnderTest.trustStorePath == project.file('build/cacerts.jks').toPath()
        classUnderTest.inputDirPath == project.file('src/main/certs').toPath()
        that classUnderTest.acceptedFileEndings, containsInAnyOrder(*['cer', 'crt', 'pem'])
    }

    def "Default TrustStore is in build dir"() {
        when:
        project.buildDir = 'mybuild'

        then:
        classUnderTest.trustStorePath == project.file('mybuild/cacerts.jks').toPath()
    }

    def "TrustStore with explicit input and output"() {
        given:
        project.file('src/x509/certs').mkdirs()

        when:
        classUnderTest.trustStore = 'öäü/truststore.jks'
        classUnderTest.inputDir = 'src/x509/certs'

        then:
        classUnderTest.inputDirPath == project.file('src/x509/certs').toPath()
        classUnderTest.trustStorePath == project.file('öäü/truststore.jks').toPath()
    }

    def "accept additional file ending"() {
        when:
        classUnderTest.acceptedFileEndings << 'der'

        then:
        that classUnderTest.acceptedFileEndings, containsInAnyOrder(*['cer', 'crt', 'pem', 'der'])
    }

    def "change accepted file endings"() {
        when:
        classUnderTest.acceptedFileEndings = ['txt']

        then:
        classUnderTest.acceptedFileEndings.size() == 1
        classUnderTest.acceptedFileEndings.first() == 'txt'
    }
}
