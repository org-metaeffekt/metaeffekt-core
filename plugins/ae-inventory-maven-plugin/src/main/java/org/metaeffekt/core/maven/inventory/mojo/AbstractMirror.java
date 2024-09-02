/*
 * Copyright 2009-2024 the original author or authors.
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

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractMirror extends IdentifiableComponent {
    private List<String> mirrorUrls = new ArrayList<>();

    public List<String> getMirrorUrls() {
        return mirrorUrls;
    }

    public void setMirrorUrls(List<String> mirrorUrls) {
        this.mirrorUrls = mirrorUrls;
    }

    public void dumpConfig(Log log, String prefix) {
        super.dumpConfig(log, prefix);
        log.debug(prefix + "  mirrorUrls: " + getMirrorUrls());
    }

}
