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
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static org.hamcrest.Matchers.containsInAnyOrder
import static spock.util.matcher.HamcrestSupport.that

class TrustStoreBuilderExtensionTest extends Specification {

    Project project

    TrustStoreBuilderExtension classUnderTest

    def setup() {
        project = ProjectBuilder.builder().build()
        project.pluginManager
                .apply(BasePlugin)
        project.file('src/main/certs').mkdirs()
        classUnderTest = new TrustStoreBuilderExtension(project)
    }

    def "register checkCertificates task"() {
        when:
        classUnderTest.checkCertificates {
            it.exclude("src/main/certs/exclude/**")
        }

        then:
        def task = project.tasks.getByName("checkCertificates") as CheckCertsValidationTask
        that task.includes, containsInAnyOrder(*[
            "**/*.crt",
            "**/*.cer",
            "**/*.pem"
        ])
        that task.excludes, containsInAnyOrder(*["src/main/certs/exclude/**"])
        task.atLeastValidDays.get() == 90
    }

    def "register buildTrustStore task"() {
        when:
        classUnderTest.trustStore {
            it.password("changeit123")
        }

        then:
        def task = project.tasks.getByName("buildTrustStore") as BuildTrustStoreTask
        task.trustStorePath.get() == project.layout.buildDirectory.file("cacerts.jks").get().asFile.toPath()
        task.trustStorePassword.get() == "changeit123"
        task.source.get() == project.layout.projectDirectory.dir("src/main/certs").asFile.toPath()
        that task.includes.get(), containsInAnyOrder(*[
            "**/*.crt",
            "**/*.cer",
            "**/*.pem"
        ])
    }

    def "throwing exception if password is not set"() {
        when:
        classUnderTest.trustStore {
            it.password("")
        }

        then:
        def e = thrown(ProjectConfigurationException)
        e.message == "The following properties have to be configured appropriately: password"
    }


    def "throwing exception if include is not set appropriately"() {
        when:
        classUnderTest.trustStore {
            it.include("", " ")
        }

        then:
        def e = thrown(ProjectConfigurationException)
        e.message == "The following properties have to be configured appropriately: include"
    }

    def "throwing exception if not supported KeyStoreType is used"() {
        when:
        classUnderTest.trustStore("name") {
            it.path("build/cacerts.bin")
        }

        then:
        def e = thrown(ProjectConfigurationException)
        e.message == "The following properties have to be configured appropriately: path"
    }
}
