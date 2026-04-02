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
package org.metaeffekt.core.inventory.resolver;

import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaInventoryAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolver that downloads source archives from URLs specified in artifact attributes,
 * supporting placeholder resolution.
 */
public class FileServerSourceArchiveResolver implements SourceArchiveResolver {

    private static final Logger LOG = LoggerFactory.getLogger(FileServerSourceArchiveResolver.class);

    private RemoteUriResolver uriResolver;
    private Properties properties;

    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\{([^}]+)\\}");

    /**
     * Resolve the source archive for a given artifact and provide a File instance that points to the file.
     *
     * @param artifact The {@link Artifact} for which the source is to be resolved.
     * @param targetDir The proposed target directory.
     *
     * @return The {@link SourceArchiveResolverResult}.
     */
    @Override
    public SourceArchiveResolverResult resolveArtifactSourceArchive(Artifact artifact, File targetDir) {
        final SourceArchiveResolverResult result = new SourceArchiveResolverResult();

        String url = artifact.get(AeaaInventoryAttribute.SOURCE_ARTIFACT_URL.getKey());
        if (url == null) {
            url = artifact.get(AeaaInventoryAttribute.SOURCE_ARCHIVE_URL.getKey());
        }

        if (url != null) {
            // Resolve placeholders like ${internal.server} using the provided properties
            final String resolvedUrl = resolvePlaceholders(url, properties);

            // Determine file name from the resolved URL
            String fileName = resolvedUrl.substring(resolvedUrl.lastIndexOf('/') + 1);
            if (fileName.contains("?")) {
                fileName = fileName.substring(0, fileName.indexOf("?"));
            }

            final File destinationFile = new File(targetDir, fileName);

            try {
                final File downloadedFile = uriResolver.resolve(resolvedUrl, destinationFile);

                if (downloadedFile != null && downloadedFile.exists()) {
                    result.addFile(downloadedFile, resolvedUrl);
                } else {
                    result.addAttemptedResourceLocation(resolvedUrl);
                }
            } catch (Exception e) {
                LOG.debug("Failed to download source from {}: {}", resolvedUrl, e.getMessage());
                result.addAttemptedResourceLocation(resolvedUrl);
            }
        }

        return result;
    }

    private String resolvePlaceholders(String input, Properties props) {
        if (input == null || props == null) return input;
        StringBuilder sb = new StringBuilder();
        Matcher matcher = PROPERTY_PATTERN.matcher(input);
        int lastEnd = 0;
        while (matcher.find()) {
            sb.append(input, lastEnd, matcher.start());
            String propName = matcher.group(1);
            String propValue = props.getProperty(propName);
            sb.append(propValue != null ? propValue : matcher.group(0));
            lastEnd = matcher.end();
        }
        sb.append(input.substring(lastEnd));
        return sb.toString();
    }

    /**
     * Set {@link #uriResolver}.
     *
     * @param uriResolver the resolver to be set.
     */
    public void setUriResolver(RemoteUriResolver uriResolver) {
        this.uriResolver = uriResolver;
    }

    /**
     * Set {@link #properties}.
     *
     * @param properties the properties to be set.
     */
    public void setProperties(Properties properties) {
        this.properties = properties;
    }
}