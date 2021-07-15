/*
 * Copyright 2009-2021 the original author or authors.
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

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

/**
 * This implementation of the {@link SourceArchiveResolver} interface uses common rules to derive the resolve source
 * archives of eclipse bundles.
 */
public class EclipseMirrorSourceArchiveResolver extends AbstractMirrorSourceArchiveResolver {

    private String select = "([^_]*)(_)(.*)";
    private String replacement = "$1.source_$3";

    private File resolveFileUrl(String mirrorUrl) {
        try {
            ResourceLoader loader = new FileSystemResourceLoader();
            Resource resource = loader.getResource(mirrorUrl);
            return resource.getFile();
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Cannot resolve file url %s.", mirrorUrl));
        }
    }

    @Override
    protected List<String> matchAndReplace(Artifact artifact) {
        String artifactId = artifact.getId();
        String sourceId = artifactId;
        if (artifactId.matches(select)) {
            sourceId = artifactId.replaceAll(select, replacement);
        }
        return Collections.singletonList(sourceId);
    }

    public String getSelect() {
        return select;
    }

    public void setSelect(String select) {
        this.select = select;
    }

    public String getReplacement() {
        return replacement;
    }

    public void setReplacement(String replacement) {
        this.replacement = replacement;
    }
}
