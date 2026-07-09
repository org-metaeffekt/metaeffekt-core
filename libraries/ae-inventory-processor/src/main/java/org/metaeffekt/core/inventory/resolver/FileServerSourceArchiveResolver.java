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

import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.report.model.types.AeaaInventoryAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Resolver that downloads source archives from URLs specified in artifact attributes,
 * supporting placeholder resolution.
 */
@Slf4j
@Setter
public class FileServerSourceArchiveResolver implements SourceArchiveResolver {

    private RemoteUriResolver uriResolver;
    private Properties properties;
    private String propertyFilePath;
    private List<String> sourceUrls = new ArrayList<>();

    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\[([^\\]]+)\\]");

    /**
     * Resolve the source archive for a given artifact and provide a File instance that points to the file.
     *
     * @param artifact  The {@link Artifact} for which the source is to be resolved.
     * @param targetDir The proposed target directory.
     * @return The {@link SourceArchiveResolverResult}.
     */
    @Override
    public SourceArchiveResolverResult resolveArtifactSourceArchive(Artifact artifact, File targetDir) {
        final SourceArchiveResolverResult result = new SourceArchiveResolverResult();

        final Properties effectiveProperties = new Properties();
        if (properties != null) {
            effectiveProperties.putAll(properties);
        }
        loadPropertiesFromFile(effectiveProperties);

        // attempt to resolve using artifact attributes
        String url = artifact.get(Artifact.Attribute.SOURCE_ARTIFACT_URL);
        if (url == null) {
            url = artifact.get(Artifact.Attribute.SOURCE_ARCHIVE_URL);
        }

        if (url != null) {
            if (resolveUrl(url, artifact, targetDir, result, effectiveProperties)) {
                return result;
            }
        }

        // iterate through sourceUrls as fallback
        if (sourceUrls != null && !sourceUrls.isEmpty()) {
            for (String urlPattern : sourceUrls) {
                if (resolveUrl(urlPattern, artifact, targetDir, result, effectiveProperties)) {
                    return result;
                }
            }
        }

        return result;
    }

    protected boolean resolveUrl(String url, Artifact artifact, File targetDir, SourceArchiveResolverResult result, Properties effectiveProperties) {
        PatternResolver resolver = new PatternResolver();

        resolver.addHandler(new PatternResolver.PropertyPlaceholderHandler(effectiveProperties));
        resolver.addHandler(new PatternResolver.ArtifactAttributeHandler(artifact));

        String resolvedUrl = url;
        if (resolvedUrl != null) {
            StringBuilder sb = new StringBuilder();
            Matcher matcher = PROPERTY_PATTERN.matcher(resolvedUrl);
            int lastEnd = 0;

            while (matcher.find()) {
                sb.append(resolvedUrl, lastEnd, matcher.start());
                String pattern = matcher.group(1);
                String resolvedValue = resolver.resolve(pattern);

                // if the resolver returns the exact pattern name, it failed to resolve it.
                // in that case, we keep the original placeholder syntax.
                if (resolvedValue != null && !resolvedValue.equals(pattern)) {
                    sb.append(resolvedValue);
                } else {
                    sb.append(matcher.group(0));
                }
                lastEnd = matcher.end();
            }
            sb.append(resolvedUrl.substring(lastEnd));
            resolvedUrl = sb.toString();

            if (PROPERTY_PATTERN.matcher(resolvedUrl).find()) {
                log.debug("URL still contains unresolved placeholders, skipping: {}", resolvedUrl);
                result.addAttemptedResourceLocation(resolvedUrl);
                return false;
            }

            return downloadFile(resolvedUrl, targetDir, result);
        }
        return false;
    }

    private boolean downloadFile(String url, File targetDir, SourceArchiveResolverResult result) {
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf("?"));
        }

        final File destinationFile = new File(targetDir, fileName);

        try {
            final File downloadedFile = uriResolver.resolve(url, destinationFile);

            if (downloadedFile != null && downloadedFile.exists()) {
                result.addFile(downloadedFile, url);
                return true;
            } else {
                result.addAttemptedResourceLocation(url);
            }
        } catch (Exception e) {
            log.debug("Failed to download source from {}: {}", url, e.getMessage());
            result.addAttemptedResourceLocation(url);
        }
        return false;
    }

    private void loadPropertiesFromFile(Properties properties) {
        if (propertyFilePath != null) {
            File file = new File(propertyFilePath);
            if (file.exists() && file.isFile()) {
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    properties.load(inputStream);
                } catch (IOException e) {
                    log.error("Failed to load properties from file {}: {}", propertyFilePath, e.getMessage());
                }
            }
        }
    }
}