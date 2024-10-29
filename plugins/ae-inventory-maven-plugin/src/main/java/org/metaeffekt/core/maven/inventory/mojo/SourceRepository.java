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

import org.apache.maven.artifact.repository.ArtifactRepository;
import org.apache.maven.artifact.resolver.ArtifactResolver;
import org.apache.maven.plugin.logging.Log;
import org.metaeffekt.core.inventory.resolver.ArtifactPattern;
import org.metaeffekt.core.inventory.resolver.ArtifactSourceRepository;

import java.util.List;
import java.util.Properties;

/**
 * Maven configurable delegate for {@link org.metaeffekt.core.inventory.resolver.ArtifactSourceRepository}.
 */
public class SourceRepository extends IdentifiableComponent {

    private String targetFolder;

    private EclipseMirror eclipseMirror;

    private ComponentMirror componentMirror;

    private MavenMirror mavenMirror;

    private List<String> patterns;

    private boolean ignoreMatches;

    public org.metaeffekt.core.inventory.resolver.ArtifactSourceRepository constructDelegate(
            ArtifactResolver artifactResolver, ArtifactRepository localRepository, List<ArtifactRepository> remoteRepositories) {

        ArtifactSourceRepository artifactSourceRepository = new ArtifactSourceRepository();
        artifactSourceRepository.setId(getId());
        artifactSourceRepository.setTargetFolder(targetFolder);
        artifactSourceRepository.setIgnoreMatches(ignoreMatches);

        if (getPatterns() != null) {
            for (String pattern : patterns) {
                String[] split = pattern.split(":");
                artifactSourceRepository.register(new ArtifactPattern(extractPattern(0, split),
                        extractPattern(1, split), extractPattern(2, split), extractPattern(3, split)));
            }
        }

        int mirrorCount = 0;
        mirrorCount += eclipseMirror == null ? 0 : 1;
        mirrorCount += componentMirror == null ? 0 : 1;
        mirrorCount += mavenMirror == null ? 0 : 1;

        if (mirrorCount > 1) {
            throw new IllegalStateException(String.format(
                    "Cannot configure source repository with id %s. Only one mirror can be configured.", getId()));
        }

        final Properties p = new Properties();
        if (getProperties() != null) {
            p.putAll(getProperties());
        }

        if (eclipseMirror != null) {
            artifactSourceRepository.setSourceArchiveResolver(eclipseMirror.createResolver(p));
        }

        if (componentMirror != null) {
            artifactSourceRepository.setSourceArchiveResolver(componentMirror.createResolver(p));
        }

        if (mavenMirror != null) {
            artifactSourceRepository.setSourceArchiveResolver(mavenMirror.createResolver(artifactResolver, localRepository, remoteRepositories));
        }

        return artifactSourceRepository;
    }

    public String getTargetFolder() {
        return targetFolder;
    }

    public List<String> getPatterns() {
        return patterns;
    }

    public void dumpConfig(Log log, String prefix) {
        super.dumpConfig(log, prefix);
        log.debug(prefix + "  targetFolder: " + getTargetFolder());
        log.debug(prefix + "  patterns: " + getPatterns());

        if (eclipseMirror != null) eclipseMirror.dumpConfig(log, prefix + "  ");
        if (componentMirror != null) componentMirror.dumpConfig(log, prefix + "  ");
        if (mavenMirror != null) mavenMirror.dumpConfig(log, prefix + "  ");
    }

}
