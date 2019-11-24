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

import java.nio.file.Files
import java.nio.file.Path

class TestFileAdapter implements FileAdapter {

    @Override
    File toFile(Path path) {
        def temp = Files.isDirectory(path)
                ? Files.createTempDirectory(null)
                : Files.createTempFile(null, null)
        def file = temp.toFile()
        file.deleteOnExit()
        return file
    }
}