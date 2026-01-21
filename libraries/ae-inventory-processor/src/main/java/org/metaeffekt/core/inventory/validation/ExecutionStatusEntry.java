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
package org.metaeffekt.core.inventory.validation;

import lombok.Getter;
import org.metaeffekt.core.inventory.processor.model.Artifact;

@Getter
public class ExecutionStatusEntry {

    public enum SEVERITY {
        ERROR,
        WARN,
        INFO
    }

    private final SEVERITY severity;

    private final String message;

    private final Artifact artifact;

    public ExecutionStatusEntry(SEVERITY severity, String message, Artifact artifact) {
        this.severity = severity;
        this.message = message;
        this.artifact = artifact;
    }

}
