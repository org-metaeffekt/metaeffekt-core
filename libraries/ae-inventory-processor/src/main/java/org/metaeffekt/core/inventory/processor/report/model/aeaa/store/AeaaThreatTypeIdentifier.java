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

import lombok.Getter;
import lombok.Setter;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.threat.AeaaThreatReference;

import java.util.function.Supplier;
import java.util.regex.Pattern;

@Getter
@Setter
public class AeaaThreatTypeIdentifier<TR extends AeaaThreatReference> extends AeaaContentIdentifierStore.AeaaContentIdentifier {

    private final Class<TR> threatReferenceClass;
    private final Supplier<TR> threatReferenceFactory;

    public AeaaThreatTypeIdentifier(String name, String wellFormedName, String implementation, Pattern idPattern, Class<TR> threatReferenceClass, Supplier<TR> threatReferenceFactory) {
        super(name, wellFormedName, implementation, idPattern);
        this.threatReferenceClass = threatReferenceClass;
        this.threatReferenceFactory = threatReferenceFactory;
    }

    @Override
    public String toString() {
        return super.toString();
    }

    public boolean isWeakness() {
        return getImplementation().equals(ThreatCategory.WEAKNESS_IMPLEMENTATION.getKey());
    }

    public boolean isAttackPattern() {
        return getImplementation().equals(ThreatCategory.ATTACK_PATTERN_IMPLEMENTATION.getKey());
    }

    public boolean isThreat() {
        return getImplementation().equals(ThreatCategory.THREAT_IMPLEMENTATION.getKey());
    }
}
