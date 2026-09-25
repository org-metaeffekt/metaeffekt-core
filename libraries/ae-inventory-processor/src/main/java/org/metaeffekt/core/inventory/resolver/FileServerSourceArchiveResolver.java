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
    private List<ServerCredential> credentials = new ArrayList<>();

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
                
                String placeholderName = pattern;
                String defaultValue = null;
                
                int colonIndex = pattern.indexOf(':');
                if (colonIndex != -1) {
                    placeholderName = pattern.substring(0, colonIndex);
                    defaultValue = pattern.substring(colonIndex + 1);
                }

                String resolvedValue = resolver.resolve(placeholderName);

                boolean isMissing = (resolvedValue == null || resolvedValue.equals(placeholderName) || resolvedValue.trim().isEmpty());

                if (isMissing) {
                    if (defaultValue != null) {
                        sb.append(defaultValue);
                    } else {
                        log.debug("URL placeholder '{}' is missing and has no default, skipping: {}", placeholderName, url);
                        return false;
                    }
                } else {
                    sb.append(resolvedValue);
                }
                lastEnd = matcher.end();
            }
            sb.append(resolvedUrl.substring(lastEnd));
            resolvedUrl = sb.toString();

            if (PROPERTY_PATTERN.matcher(resolvedUrl).find()) {
                log.debug("URL still contains unresolved placeholders, skipping: {}", resolvedUrl);
                return false;
            }

            return downloadFile(resolvedUrl, targetDir, result);
        }
        return false;
    }

    protected boolean downloadFile(String url, File targetDir, SourceArchiveResolverResult result) {
        String fileName = url.substring(url.lastIndexOf('/') + 1);
        if (fileName.contains("?")) {
            fileName = fileName.substring(0, fileName.indexOf("?"));
        }

        File existingFile = findFile(targetDir, fileName);
        if (existingFile != null) {
            result.addFile(existingFile, url);
            return true;
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
            log.debug("Failed to download source from [{}]: [{}]", url, e.getMessage());
            result.addAttemptedResourceLocation(url);
        }
        return false;
    }

    private File findFile(File dir, String fileName) {
        if (dir == null || !dir.exists() || !dir.isDirectory()) {
            return null;
        }
        
        File directFile = new File(dir, fileName);
        if (directFile.exists() && directFile.isFile()) {
            return directFile;
        }

        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    File found = findFile(file, fileName);
                    if (found != null) {
                        return found;
                    }
                }
            }
        }
        return null;
    }

    private void loadPropertiesFromFile(Properties properties) {
        if (propertyFilePath != null) {
            File file = new File(propertyFilePath);
            if (file.exists() && file.isFile()) {
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    properties.load(inputStream);
                } catch (IOException e) {
                    log.error("Failed to load properties from file [{}]: [{}]", propertyFilePath, e.getMessage());
                }
            }
        }
    }
}