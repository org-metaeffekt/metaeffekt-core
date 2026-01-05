/*
 * Copyright 2009-2026 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.filescan;

import org.metaeffekt.core.util.FileUtils;

import java.io.File;

/**
 * Reference for file managing its file and normalized path representation.
 *
 * The implementation does explicitly not canonicalize the path.
 */
public class FileRef {

    /**
     * The file of the references.
     */
    private final File file;

    /**
     * The normalized path.
     */
    private final String path;

    /**
     * Constructor based on {@link File}. The path is taken from the file and implicitly normalized.
     *
     * @param file The file to reference.
     */
    public FileRef(File file) {
        this.file = file;
        this.path = FileUtils.normalizePathToLinux(file.getPath());
    }

    /**
     * Constructor based on a path.
     *
     * @param path The path to use.
     */
    public FileRef(String path) {
        this(new File(path));
    }

    public String getPath() {
        return path;
    }

    public File getFile() {
        return file;
    }

}
