/*
 * Copyright 2009-2022 the original author or authors.
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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.logging.Log;
import org.metaeffekt.core.inventory.resolver.ComponentSourceArchiveResolver;
import org.metaeffekt.core.inventory.resolver.Mapping;
import org.metaeffekt.core.inventory.resolver.RemoteUriResolver;

import java.util.List;
import java.util.Properties;

public class ComponentMirror extends AbstractMirror {

    private List<String> mappings;

    public ComponentSourceArchiveResolver createResolver(Properties properties) {
        ComponentSourceArchiveResolver resolver = new ComponentSourceArchiveResolver();
        resolver.setMirrorBaseUrls(getMirrorUrls());
        resolver.setUriResolver(new RemoteUriResolver(properties));

        if (mappings != null) {
            for (String mapping : mappings) {
                String[] split = mapping.split(":");
                resolver.addMapping(new Mapping(extractPattern(0, split), extractPattern(1, split)));
            }
        }

        return resolver;
    }

    public void dumpConfig(Log log, String prefix) {
        super.dumpConfig(log, prefix);
        log.debug(prefix + "  mappings: " + mappings);
    }

}
