/*
 * Copyright 2009-2024 the original author or authors.
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
package org.metaeffekt.core.inventory.resolver;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class SourceArchiveResolverResult {

    /**
     * The resolved files.
     */
    private List<File> files = new ArrayList<>();

    /**
     * Attempted resource locations.
     */
    private List<String> attemptedResourceLocations = new ArrayList<>();

    public SourceArchiveResolverResult() {
    }

    public void addFile(File file, String resourceLocation) {
        this.files.add(file);
        this.attemptedResourceLocations.add(resourceLocation);
    }

    public boolean isEmpty() {
        return files.isEmpty();
    }

    public List<File> getFiles() {
        return files;
    }

    public List<String> getAttemptedResourceLocations() {
        return attemptedResourceLocations;
    }

    public void addAttemptedResourceLocation(String sourceArchiveUrl) {
        attemptedResourceLocations.add(sourceArchiveUrl);
    }
}
