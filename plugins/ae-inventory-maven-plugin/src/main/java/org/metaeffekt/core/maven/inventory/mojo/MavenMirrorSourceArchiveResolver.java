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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.artifact.handler.DefaultArtifactHandler;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
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

    private final RepositorySystem repositorySystem;
    private final RepositorySystemSession repositorySystemSession;
    private final List<RemoteRepository> remoteProjectRepositories;

    public MavenMirrorSourceArchiveResolver(RepositorySystem repositorySystem, RepositorySystemSession repositorySystemSession, List<RemoteRepository> remoteProjectRepositories) {
        this.repositorySystem = repositorySystem;
        this.repositorySystemSession = repositorySystemSession;
        this.remoteProjectRepositories = remoteProjectRepositories;
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

    private void attemptResolveMavenRepo(Artifact artifact, String classifier, SourceArchiveResolverResult result) {
        final DefaultArtifact toSearchFor = new DefaultArtifact(
                artifact.getGroupId(), artifact.getArtifactId(), classifier,
                "jar", artifact.getVersion());

        try {
            ArtifactResult artifactResult = repositorySystem.resolveArtifact(repositorySystemSession, new ArtifactRequest(toSearchFor, remoteProjectRepositories, null));
            if (artifactResult.isResolved() && !artifactResult.isMissing()) {
                result.addFile(artifactResult.getArtifact().getFile(), ((RemoteRepository) artifactResult.getRepository()).getUrl());
            }
        } catch (ArtifactResolutionException e) {
                result.addAttemptedResourceLocation(e.getResult().getRequest().toString());
        }

    }
}
