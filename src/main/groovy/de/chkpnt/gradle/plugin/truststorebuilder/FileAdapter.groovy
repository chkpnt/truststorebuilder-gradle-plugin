package de.chkpnt.gradle.plugin.truststorebuilder

import java.io.File;
import java.nio.file.Path

interface FileAdapter {
	File toFile(Path path)
}

class DefaultFileAdapter implements FileAdapter {

	@Override
	public File toFile(Path path) {
		return path.toFile();
	}
	
}