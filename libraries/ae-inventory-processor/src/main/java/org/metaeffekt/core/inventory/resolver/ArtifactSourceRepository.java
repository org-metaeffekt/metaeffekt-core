/**
 * Copyright 2009-2020 the original author or authors.
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

import java.io.File;

/**
 * An artifacts source repository aggregates information where source archives can be resolved. In addition the
 * {@link ArtifactSourceRepository} has a {@link SourceArchiveResolver} that abstracts the resolution of the source
 * archives.
 */
public class ArtifactSourceRepository extends ArtifactPatternMatcher {

    private String id;

    private String targetFolder;

    private SourceArchiveResolver sourceArchiveResolver;

    private boolean ignoreMatches;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public SourceArchiveResolverResult resolveSourceArchive(Artifact artifact, File targetDir) {
        if (sourceArchiveResolver != null) {
            File effectiveTargetDir = new File(targetDir, targetFolder);
            return sourceArchiveResolver.resolveArtifactSourceArchive(artifact, effectiveTargetDir);
        }
        return new SourceArchiveResolverResult();
    }

    public SourceArchiveResolver getSourceArchiveResolver() {
        return sourceArchiveResolver;
    }

    public void setSourceArchiveResolver(SourceArchiveResolver sourceArchiveResolver) {
        this.sourceArchiveResolver = sourceArchiveResolver;
    }

    public String getTargetFolder() {
        return targetFolder;
    }

    public void setTargetFolder(String targetFolder) {
        this.targetFolder = targetFolder;
    }

    public boolean isIgnoreMatches() {
        return ignoreMatches;
    }

    public void setIgnoreMatches(boolean ignoreMatches) {
        this.ignoreMatches = ignoreMatches;
    }
}
