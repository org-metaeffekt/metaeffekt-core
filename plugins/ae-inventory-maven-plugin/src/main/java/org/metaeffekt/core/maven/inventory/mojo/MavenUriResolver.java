package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.DefaultArtifact;
import org.apache.maven.artifact.factory.DefaultArtifactFactory;
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
