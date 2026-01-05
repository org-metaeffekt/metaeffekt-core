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
package org.metaeffekt.core.maven.inventory.mojo;

import org.apache.maven.plugin.logging.Log;
import org.metaeffekt.core.inventory.resolver.EclipseMirrorSourceArchiveResolver;
import org.metaeffekt.core.inventory.resolver.RemoteUriResolver;

import java.util.Properties;

public class EclipseMirror extends AbstractMirror {

    private String passThrough = "[^_]\\.source_.*";

    private String select = "([^_]*)(_)(.*)";

    private String replacement = "$1.source_$3";

    public EclipseMirrorSourceArchiveResolver createResolver(Properties properties) {
        EclipseMirrorSourceArchiveResolver resolver = new EclipseMirrorSourceArchiveResolver();
        resolver.setUriResolver(new RemoteUriResolver(properties));

        // FIXME: missing passThrough

        resolver.setReplacement(replacement);
        resolver.setSelect(select);
        resolver.setMirrorBaseUrls(getMirrorUrls());
        return resolver;
    }

    public void dumpConfig(Log log, String prefix) {
        super.dumpConfig(log, prefix);
        log.debug(prefix + "  passThrough: " + passThrough);
        log.debug(prefix + "  select: " + select);
        log.debug(prefix + "  replacement: " + replacement);
    }

}
