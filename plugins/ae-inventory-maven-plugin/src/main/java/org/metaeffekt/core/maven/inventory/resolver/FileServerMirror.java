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

import lombok.Getter;
import lombok.Setter;
import org.apache.maven.plugins.annotations.Parameter;
import org.metaeffekt.core.inventory.resolver.FileServerSourceArchiveResolver;
import org.metaeffekt.core.inventory.resolver.RemoteUriResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

@Setter
@Getter
public class FileServerMirror extends AbstractMirror {

    @Parameter
    private List<String> sourceUrls = new ArrayList<>();

    public FileServerSourceArchiveResolver createResolver(Properties properties) {
        final FileServerSourceArchiveResolver resolver = new FileServerSourceArchiveResolver();

        // pass the plugin configuration properties
        resolver.setProperties(properties);
        
        resolver.setSourceUrls(sourceUrls);

        // initialize the URI resolver
        resolver.setUriResolver(new RemoteUriResolver(properties));

        return resolver;
    }

}