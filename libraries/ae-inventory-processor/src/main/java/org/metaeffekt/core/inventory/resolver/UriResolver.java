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

import java.io.File;

/**
 * Abstraction for resolving files from an URI.
 */
public interface UriResolver {

    /**
     * Resolve and load the file located at the given uri and make it available as destination file.
     *
     * @param uri The uri.
     * @param destinationFile The file to download to. The file is only a proposal. The returned file may differ.
     *
     * @return Returns the file under which the resolved or downloaded artifact is available in the local file system.
     */
    File resolve(String uri, File destinationFile);

}
