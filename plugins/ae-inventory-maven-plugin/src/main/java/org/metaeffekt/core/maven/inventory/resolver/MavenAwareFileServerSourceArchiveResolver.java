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
package org.metaeffekt.core.maven.inventory.resolver;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.spi.connector.transport.GetTask;
import org.eclipse.aether.spi.connector.transport.Transporter;
import org.eclipse.aether.spi.connector.transport.TransporterProvider;
import org.metaeffekt.core.inventory.resolver.FileServerSourceArchiveResolver;
import org.metaeffekt.core.inventory.resolver.SourceArchiveResolverResult;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.net.URI;
import java.nio.file.Files;
import java.util.List;

@Slf4j
public class MavenAwareFileServerSourceArchiveResolver extends FileServerSourceArchiveResolver {

    private final RepositorySystemSession repositorySystemSession;
    private final List<RemoteRepository> remoteProjectRepositories;
    private final TransporterProvider transporterProvider;

    public MavenAwareFileServerSourceArchiveResolver(RepositorySystemSession repositorySystemSession,
            List<RemoteRepository> remoteProjectRepositories, TransporterProvider transporterProvider) {

        this.repositorySystemSession = repositorySystemSession;
        this.remoteProjectRepositories = remoteProjectRepositories;
        this.transporterProvider = transporterProvider;
    }

    @Override
    protected boolean downloadFile(String url, File targetDir, SourceArchiveResolverResult result) {
        if (remoteProjectRepositories != null && repositorySystemSession != null && transporterProvider != null) {

            // NOTE: we match the url to be resolved to the maven repo urls; for this to
            //   work we may require to add the repo to the maven settings (with its credentials)

            // iterate repo and check whether url is a match
            for (RemoteRepository repo : remoteProjectRepositories) {

                String repoUrl = repo.getUrl();
                if (!repoUrl.endsWith("/")) {
                    repoUrl += "/";
                }

                if (url.startsWith(repoUrl)) {
                    String relativePath = url.substring(repoUrl.length());
                    String fileName = url.substring(url.lastIndexOf('/') + 1);
                    if (fileName.contains("?")) {
                        fileName = fileName.substring(0, fileName.indexOf("?"));
                    }

                    // check if file already exists; skip further processing in any case
                    final File destinationFile = new File(targetDir, fileName);
                    if (destinationFile.exists()) {
                        result.addFile(destinationFile, url);
                        return true;
                    }

                    try {
                        try (Transporter transporter = transporterProvider.newTransporter(repositorySystemSession, repo)) {
                            GetTask task = new GetTask(URI.create(relativePath));
                            task.setDataFile(destinationFile);
                            transporter.get(task);

                            // MD5 checksum validation
                            try {
                                GetTask md5Task = new GetTask(URI.create(relativePath + ".md5"));
                                File md5File = new File(targetDir, fileName + ".md5");
                                md5Task.setDataFile(md5File);
                                transporter.get(md5Task);

                                String expectedMd5 = new String(Files.readAllBytes(md5File.toPath())).trim();
                                // some md5 files contain the hash and the filename, e.g. "hash  filename"
                                if (expectedMd5.contains(" ")) {
                                    expectedMd5 = expectedMd5.split(" ")[0];
                                }

                                String actualMd5 = FileUtils.computeMD5Checksum(destinationFile);
                                if (!expectedMd5.equalsIgnoreCase(actualMd5)) {
                                    log.error("MD5 mismatch for [{}]. Expected [{}], got [{}]", url, expectedMd5, actualMd5);
                                    if (destinationFile.exists()) {
                                        FileUtils.deleteQuietly(destinationFile);
                                    }
                                    if (md5File.exists()) {
                                        FileUtils.deleteQuietly(md5File);
                                    }
                                    continue; // try next repo or fallback
                                }
                                log.debug("MD5 checksum validated successfully for [{}]", url);
                                if (md5File.exists()) {
                                    FileUtils.deleteQuietly(md5File);
                                }
                            } catch (Exception e) {
                                log.debug("Failed to retrieve or validate MD5 for [{}] from [{}]: [{}]. Proceeding without MD5 validation.", url, repoUrl, e.getMessage());
                            }

                            if (destinationFile.exists()) {
                                result.addFile(destinationFile, url);
                                return true;
                            }
                        }
                    } catch (Exception e) {
                        // NOTE: to understand the failures debug-level log must be enabled
                        log.debug("Failed to download [{}] from [{}]: [{}]", url, repoUrl, e.getMessage());
                        if (destinationFile.exists()) {
                            FileUtils.deleteQuietly(destinationFile);
                        }
                    }
                } else {
                    log.debug("Skipping repository with url [{}] not matching the download url [{}].", repoUrl, url);
                }
            }
        }

        // fallback to default mechanism
        return super.downloadFile(url, targetDir, result);
    }
}
