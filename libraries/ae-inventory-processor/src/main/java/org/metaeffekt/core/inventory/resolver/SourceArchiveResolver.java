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

import java.io.File;

/**
 * Interface for resolving source archives.
 */
public interface SourceArchiveResolver {

    /**
     * Resolve the source archive for a given artifact and provide a File instance that points to the file.
     * @param artifact The {@link Artifact} for which the source is to be resolved.
     * @param targetDir The proposed target directory.
     * @return The {@link SourceArchiveResolverResult}.
     */
    SourceArchiveResolverResult resolveArtifactSourceArchive(Artifact artifact, File targetDir);

}
