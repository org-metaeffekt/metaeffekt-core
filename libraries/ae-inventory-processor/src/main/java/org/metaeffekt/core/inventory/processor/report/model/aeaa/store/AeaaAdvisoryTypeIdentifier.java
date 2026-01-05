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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.store;

import org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.AeaaAdvisoryEntry;

import java.util.function.Supplier;
import java.util.regex.Pattern;

public class AeaaAdvisoryTypeIdentifier<ADV extends AeaaAdvisoryEntry> extends AeaaContentIdentifierStore.AeaaContentIdentifier {

    private final Class<ADV> advisoryClass;
    private final Supplier<ADV> advisoryFactory;

    public AeaaAdvisoryTypeIdentifier(String name, String wellFormedName, String implementation, Pattern idPattern,
                                      Class<ADV> advisoryClass, Supplier<ADV> advisoryFactory
    ) {
        super(name, wellFormedName, implementation, idPattern);
        this.advisoryClass = advisoryClass;
        this.advisoryFactory = advisoryFactory;
    }

    public Class<ADV> getAdvisoryClass() {
        return advisoryClass;
    }

    public Supplier<ADV> getAdvisoryFactory() {
        return advisoryFactory;
    }

    @Override
    public String toString() {
        return super.toString();
    }
}
