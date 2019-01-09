/**
 * Copyright 2009-2018 the original author or authors.
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

import org.apache.commons.lang.StringEscapeUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstract implementation of a {@link SourceArchiveResolver}. This implementation is based on a list of
 * base URLs of a archive mirror.
 */
public abstract class AbstractMirrorSourceArchiveResolver implements SourceArchiveResolver {

    private List<String> mirrorBaseUrls;

    private LocalFileUriResolver localFileUriResolver = new LocalFileUriResolver();

    private UriResolver uriResolver;

    @Override
    public SourceArchiveResolverResult resolveArtifactSourceArchive(Artifact artifact, File targetDir) {
        List<String> sourceIds = matchAndReplace(artifact);

        if (sourceIds == null && !sourceIds.isEmpty()) {
            return null;
        }

        SourceArchiveResolverResult result = new SourceArchiveResolverResult();

        String hitMirrorBaseUrl = null;

        for (String mirrorUrl : mirrorBaseUrls) {
            for (String sourceId : sourceIds) {
                String sourceArchiveUrl = mirrorUrl + sourceId;
                sourceArchiveUrl = encodeUrl(sourceArchiveUrl);

                File file = new File(targetDir, sourceId);

                if (mirrorUrl.toLowerCase().startsWith("file")) {
                    file = localFileUriResolver.resolve(sourceArchiveUrl, file);
                } else {
                    if (uriResolver == null) {
                        throw new IllegalStateException(String.format(
                                "Cannot resolve source archive at '%s'. No uriResolver configured.", sourceArchiveUrl));
                    }
                    file = uriResolver.resolve(sourceArchiveUrl, file);
                }

                if (file != null && file.exists()) {
                    result.addFile(file, sourceArchiveUrl);
                    hitMirrorBaseUrl = mirrorUrl;
                    break;
                } else {
                    result.addAttemptedResourceLocation(sourceArchiveUrl);
                }
            }
        }

        // if we have a hit...
        if (!result.isEmpty()) {
            // we reprioritize the mirrorBaseUrl by moving it at first position
            mirrorBaseUrls.remove(hitMirrorBaseUrl);
            mirrorBaseUrls.add(0, hitMirrorBaseUrl);
        }

        return result;
    }

    private String encodeUrl(String sourceArchiveUrl) {
        return sourceArchiveUrl.replace(" ", "%20");
    }

    protected abstract List<String> matchAndReplace(Artifact artifact);

    public List<String> getMirrorBaseUrls() {
        return mirrorBaseUrls;
    }

    public void setMirrorBaseUrls(List<String> mirrorBaseUrls) {
        this.mirrorBaseUrls = mirrorBaseUrls;
    }

    public UriResolver getUriResolver() {
        return uriResolver;
    }

    public void setUriResolver(UriResolver uriResolver) {
        this.uriResolver = uriResolver;
    }

    public LocalFileUriResolver getLocalFileUriResolver() {
        return localFileUriResolver;
    }

    public void setLocalFileUriResolver(LocalFileUriResolver localFileUriResolver) {
        this.localFileUriResolver = localFileUriResolver;
    }

}
