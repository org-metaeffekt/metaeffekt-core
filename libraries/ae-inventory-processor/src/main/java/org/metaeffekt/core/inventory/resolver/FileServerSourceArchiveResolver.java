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
public class FileServerSourceArchiveResolver implements SourceArchiveResolver {

    private static final Logger LOG = LoggerFactory.getLogger(FileServerSourceArchiveResolver.class);

    private RemoteUriResolver uriResolver;
    private Properties properties;
    private String propertyFilePath;
    private List<String> sourceUrls = new ArrayList<>();

    private static final Pattern PROPERTY_PATTERN = Pattern.compile("\\$\\[([^}]+)\\]");

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

        // Prepare effective properties
        final Properties effectiveProperties = new Properties();
        if (properties != null) {
            effectiveProperties.putAll(properties);
        }
        loadPropertiesFromFile(effectiveProperties);

        // Attempt to resolve using artifact attributes
        String url = artifact.get(AeaaInventoryAttribute.SOURCE_ARTIFACT_URL.getKey());
        if (url == null) {
            url = artifact.get(AeaaInventoryAttribute.SOURCE_ARCHIVE_URL.getKey());
        }

        if (url != null) {
            final String resolvedUrl = resolvePlaceholders(url, effectiveProperties);
            if (downloadFile(resolvedUrl, targetDir, result)) {
                return result;
            }
        }

        // Iterate through sourceUrls as fallback
        if (sourceUrls != null && !sourceUrls.isEmpty()) {
            for (String urlPattern : sourceUrls) {
                // Placeholder resolution only uses provided properties
                final String resolvedFallbackUrl = resolvePlaceholders(urlPattern, effectiveProperties);

                // Call placeholder method for custom resolution logic
                if (resolveFromFallbackUrl(resolvedFallbackUrl, artifact, targetDir, result)) {
                    return result;
                }
            }
        }

        return result;
    }

    protected boolean resolveFromFallbackUrl(String url, Artifact artifact, File targetDir, SourceArchiveResolverResult result) {
        System.out.println("fallback logic activated");
        return downloadFile(url, targetDir, result);
    }

    private boolean downloadFile(String url, File targetDir, SourceArchiveResolverResult result) {
        // Determine file name from the URL
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
            LOG.debug("Failed to download source from {}: {}", url, e.getMessage());
            result.addAttemptedResourceLocation(url);
        }
        return false;
    }

    private void loadPropertiesFromFile(Properties props) {
        if (propertyFilePath != null) {
            File file = new File(propertyFilePath);
            if (file.exists() && file.isFile()) {
                try (FileInputStream inputStream = new FileInputStream(file)) {
                    props.load(inputStream);
                } catch (IOException e) {
                    LOG.error("Failed to load properties from file {}: {}", propertyFilePath, e.getMessage());
                }
            }
        }
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

    public void setPropertyFilePath(String propertyFilePath) {
        this.propertyFilePath = propertyFilePath;
    }

    public void setSourceUrls(List<String> sourceUrls) {
        this.sourceUrls = sourceUrls;
    }
}