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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactNotFoundException;
import org.apache.maven.artifact.resolver.ArtifactResolutionException;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.resolver.SourceArchiveResolver;
import org.metaeffekt.core.inventory.resolver.SourceArchiveResolverResult;

import java.io.File;
import java.util.List;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

/**
 * This implementation of the {@link SourceArchiveResolver} interface uses Maven directly.
 */
public class MavenMirrorSourceArchiveResolver implements SourceArchiveResolver {

    private final ArtifactResolver artifactResolver;
    private final ArtifactRepository localRepository;
    private final List<ArtifactRepository> remoteRepositories;

    public MavenMirrorSourceArchiveResolver(ArtifactResolver artifactResolver, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories) {
        this.artifactResolver = artifactResolver;
        this.localRepository = localRepository;
        this.remoteRepositories = remoteRepositories;
    }

    @Override
    public SourceArchiveResolverResult resolveArtifactSourceArchive(Artifact artifact, File targetDir) {

        final SourceArchiveResolverResult result = new SourceArchiveResolverResult();

        // in case a groupId and version is given, we attempt to download via maven
        if (isNotEmpty(artifact.getGroupId()) && isNotEmpty(artifact.getVersion())) {

            // ensure the artifactId is computed
            artifact.deriveArtifactId();

            attemptResolveMavenRepo(artifact, "sources", result);
            attemptResolveMavenRepo(artifact, "source", result);
        }

        return result;
    }

    private final static DefaultArtifactHandler artifactHandler = new DefaultArtifactHandler() {
        @Override public String getExtension() { return "jar"; }
    };

    private void attemptResolveMavenRepo(Artifact artifact, String classifier, SourceArchiveResolverResult result) {
        final DefaultArtifact toSearchFor = new DefaultArtifact(
                artifact.getGroupId(), artifact.getArtifactId(), artifact.getVersion(),
                null, "jar", classifier, artifactHandler
        );

        // had some weird issue with ArtifactResolutionRequest so using this signature for now
        try {
            artifactResolver.resolve(toSearchFor, remoteRepositories, localRepository);
            if (toSearchFor.getFile() != null && toSearchFor.getFile().exists()) {
                result.addFile(toSearchFor.getFile(), toSearchFor.getDownloadUrl());
            }
        } catch (ArtifactResolutionException | ArtifactNotFoundException e) {
            if (toSearchFor.getDownloadUrl() != null) {
                result.addAttemptedResourceLocation(toSearchFor.getDownloadUrl());
            }
        }

    }
}
