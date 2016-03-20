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

import java.nio.file.Path

import org.gradle.api.GradleException

class CheckCertValidationError extends GradleException {

	Path file

	String reason

	CheckCertValidationError(Path file, String reason) {
		this.file = file
		this.reason = reason
	}

	@Override
	public String getMessage() {
		return "${reason}: ${file}"
	}
}
