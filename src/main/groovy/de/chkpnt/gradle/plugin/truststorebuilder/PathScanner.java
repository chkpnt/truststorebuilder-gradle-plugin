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

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

class PathScanner {

    static List<Path> scanForFilesWithFileEnding(Path path, List<String> endsWith) throws IOException {
        String extensions = String.join(",", endsWith);
        PathMatcher endsWithMatcher = FileSystems.getDefault()
            .getPathMatcher("glob:*.{" + extensions + "}");

        List<Path> certs = new ArrayList<>();

        Files.walkFileTree(path, new SimpleFileVisitor<Path>() {

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                super.visitFile(file, attrs);

                Path filename = file.getFileName();
                if (endsWithMatcher.matches(filename)) {
                    certs.add(file);
                }

                return FileVisitResult.CONTINUE;
            }
        });

        return certs;
    }
}
