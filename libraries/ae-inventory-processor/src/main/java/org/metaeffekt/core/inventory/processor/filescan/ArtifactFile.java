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

import lombok.Getter;
import lombok.Setter;
import org.metaeffekt.core.inventory.processor.model.FilePatternQualifierMapper;

import java.io.File;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
public class ArtifactFile {
    private File file;
    private Set<FilePatternQualifierMapper> owningComponents; // components this artifact belongs to

    public ArtifactFile(File file) {
        this.file = file;
        this.owningComponents = new HashSet<>();
    }

    public void addOwningComponent(FilePatternQualifierMapper component) {
        owningComponents.add(component);
    }

    public boolean isShared() {
        return owningComponents.size() > 1;
    }
}
