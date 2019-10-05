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

package de.chkpnt.gradle.plugin.truststorebuilder;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.BasePlugin;
import org.gradle.language.base.plugins.LifecycleBasePlugin;

public class TrustStoreBuilderPlugin implements Plugin<Project> {

	private static final String TRUSTSTOREBUILDER_EXTENSION_NAME = "trustStoreBuilder";
	private static final String BUILD_TRUSTSTORE_TASK_NAME = "buildTrustStore";
	private static final String CHECK_CERTS_TASK_NAME = "checkCertificates";

	@Override
	public void apply(Project project) {
		project.getPluginManager()
			.apply(BasePlugin.class);

		TrustStoreBuilderExtension extension = project.getExtensions()
			.create(TRUSTSTOREBUILDER_EXTENSION_NAME, TrustStoreBuilderExtension.class, project);

		project.getTasks()
			.register(CHECK_CERTS_TASK_NAME, CheckCertsValidationTask.class, task -> {
				task.setGroup(LifecycleBasePlugin.VERIFICATION_GROUP);
				task.setDescription("Checks the validation of the certificates to import.");

				task.getInputDir()
					.set(extension.getInputDir());
				task.getAcceptedFileEndings()
					.set(extension.getAcceptedFileEndings());
				task.getAtLeastValidDays()
					.set(extension.getAtLeastValidDays());
			});
		project.getTasks()
			.getByName(LifecycleBasePlugin.CHECK_TASK_NAME)
			.dependsOn(CHECK_CERTS_TASK_NAME);

		project.getTasks()
			.register(BUILD_TRUSTSTORE_TASK_NAME, ImportCertsTask.class, task -> {
				task.setGroup(BasePlugin.BUILD_GROUP);

				task.getKeystore()
					.set(extension.getTrustStore());
				task.getPassword()
					.set(extension.getPassword());
				task.getInputDir()
					.set(extension.getInputDir());
				task.getAcceptedFileEndings()
					.set(extension.getAcceptedFileEndings());
			});

		configureTaskDependencies(project);
	}

	private void configureTaskDependencies(Project project) {
		project.getTasks()
			.getByName(LifecycleBasePlugin.CHECK_TASK_NAME)
			.dependsOn(CHECK_CERTS_TASK_NAME);

		project.getTasks()
			.getByName(LifecycleBasePlugin.BUILD_TASK_NAME)
			.dependsOn(BUILD_TRUSTSTORE_TASK_NAME);
	}

}
