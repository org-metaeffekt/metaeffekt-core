/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.artifact.resolver.ArtifactResolutionRequest;
import org.apache.maven.artifact.resolver.ArtifactResolutionResult;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.metaeffekt.core.inventory.resolver.UriResolver;

import java.io.File;

public class MavenUriResolver implements UriResolver {

    private final ArtifactResolver artifactResolver;

    public MavenUriResolver(ArtifactResolver artifactResolver) {
        this.artifactResolver = artifactResolver;
    }

    @Override
    public File resolve(String uri, File destinationFile) {
        // FIXME: do we need this?
        ArtifactResolutionRequest artifactResolutionRequest = new ArtifactResolutionRequest(null);
        ArtifactResolutionResult result = artifactResolver.resolve(artifactResolutionRequest);
        return null;
    }
}
