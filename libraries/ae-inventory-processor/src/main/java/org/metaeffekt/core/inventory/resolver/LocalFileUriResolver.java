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

import org.springframework.core.io.FileSystemResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import java.io.File;
import java.io.IOException;

/**
 * Resolves local files.
 */
public class LocalFileUriResolver implements UriResolver {

    @Override
    public File resolve(String uri, File destinationFile) {
        try {
            ResourceLoader loader = new FileSystemResourceLoader();
            Resource resource = loader.getResource(uri);
            return resource.getFile();
        } catch (IOException e) {
            return null;
        }
    }

}
