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

import org.apache.commons.io.FileUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Properties;

public class RemoteUriResolver extends AbstractRemoteAccess implements UriResolver {

    private static final Logger LOG = LoggerFactory.getLogger(RemoteUriResolver.class);

    public RemoteUriResolver(Properties properties) {
        super(properties);
    }

    @Override
    public File resolve(String uri, File destinationFile) {
        if (!destinationFile.exists()) {
            download(uri, destinationFile);
        }
        return destinationFile.exists() ? destinationFile : null;
    }

    private void download(String uri, File destinationFile) {
        try {
            CloseableHttpClient client = createHttpClient();
            final HttpGet get = new HttpGet(uri);
            try (final CloseableHttpResponse response = client.execute(get)) {
                if (response.getStatusLine().getStatusCode() == 200) {
                    FileUtils.copyInputStreamToFile(response.getEntity().getContent(), destinationFile);
                }
            } catch(Exception e) {
                LOG.debug("Cannot download uri. Request timed out.", e);
            }
        } catch (Exception e) {
            LOG.debug("Cannot remote uri {}.", uri, e);
        }
    }

}
