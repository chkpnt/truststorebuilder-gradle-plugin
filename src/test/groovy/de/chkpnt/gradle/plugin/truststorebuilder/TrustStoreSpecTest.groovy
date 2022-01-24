/*
 * Copyright 2016 - 2022 Gregor Dschung
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

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static org.hamcrest.Matchers.containsInAnyOrder
import static spock.util.matcher.HamcrestSupport.that

class TrustStoreSpecTest extends Specification {

    Project project

    TrustStoreSpec classUnderTest

    def setup() {
        project = ProjectBuilder.builder().build()
        project.file('src/main/certs').mkdirs()
        classUnderTest = new TrustStoreSpec(project)
    }

    def "check default values"() {
        expect:
        classUnderTest.password.get() == 'changeit'
        classUnderTest.path.get() == project.file('build/cacerts.jks').toPath()
        classUnderTest.source.get() == project.file('src/main/certs').toPath()
        that classUnderTest.includes.get(), containsInAnyOrder(*[
            "**/*.crt",
            "**/*.cer",
            "**/*.pem"
        ])
        classUnderTest.atLeastValidDays.get() == 90
        classUnderTest.buildEnabled.get() == true
    }

    def "Default TrustStore is in build dir"() {
        when:
        project.buildDir = 'mybuild'

        then:
        classUnderTest.path.get() == project.file('mybuild/cacerts.jks').toPath()
    }

    def "TrustStore with explicit input and output"() {
        when:
        classUnderTest.path('öäü/truststore.jks')
        classUnderTest.source('src/x509/certs')

        then:
        classUnderTest.path.get() == project.file('öäü/truststore.jks').toPath()
        classUnderTest.source.get() == project.file('src/x509/certs').toPath()
    }

    def "change accepted file endings"() {
        when:
        classUnderTest.include("**/*.txt", "**/*.pem")

        then:
        that classUnderTest.includes.get(), containsInAnyOrder(*["**/*.txt", "**/*.pem"])
    }
}
