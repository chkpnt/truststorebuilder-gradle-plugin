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
import org.gradle.api.ProjectConfigurationException
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class TrustStoreBuilderExtensionTest extends Specification {

    Project project

    TrustStoreBuilderExtension classUnderTest

    def setup() {
        project = ProjectBuilder.builder().build()
        project.file('src/main/certs').mkdirs()
        classUnderTest = new TrustStoreBuilderExtension(project)
    }

    def "throwing exception if password is not set"() {
        when:
        classUnderTest.trustStore("") {
            it.password("")
        }

        then:
        def e = thrown(ProjectConfigurationException)
        e.message == "The following properties have to be configured appropriately: password"
    }


    def "throwing exception if include is not set appropriately"() {
        when:
        classUnderTest.trustStore("") {
            it.include("", " ")
        }

        then:
        def e = thrown(ProjectConfigurationException)
        e.message == "The following properties have to be configured appropriately: include"
    }
}
