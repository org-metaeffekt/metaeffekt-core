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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.store;

import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;

public class AeaaOtherTypeStore extends AeaaContentIdentifierStore<AeaaOtherTypeIdentifier> {

    public final static AeaaOtherTypeIdentifier CWE = new AeaaOtherTypeIdentifier("CWE", "CWE", "",
            Pattern.compile("(CWE-\\d+)", Pattern.CASE_INSENSITIVE));
    public final static AeaaOtherTypeIdentifier CPE = new AeaaOtherTypeIdentifier("CPE", "CPE", "",
            Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE));
    public final static AeaaOtherTypeIdentifier NVD = new AeaaOtherTypeIdentifier("NVD", "NVD", "",
            Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE));
    public final static AeaaOtherTypeIdentifier ASSESSMENT_STATUS = new AeaaOtherTypeIdentifier("ASSESSMENT_STATUS", "Assessment Status", "",
            Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE));
    public final static AeaaOtherTypeIdentifier EOL = new AeaaOtherTypeIdentifier("EOL", "endoflife.date", "",
            Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE));
    public final static AeaaOtherTypeIdentifier EPSS = new AeaaOtherTypeIdentifier("EPSS", "EPSS", "",
            Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE));
    public final static AeaaOtherTypeIdentifier KEV = new AeaaOtherTypeIdentifier("KEV", "KEV", "",
            Pattern.compile("UNDEFINED", Pattern.CASE_INSENSITIVE));

    private final static AeaaOtherTypeStore INSTANCE = new AeaaOtherTypeStore();

    public static AeaaOtherTypeStore get() {
        return INSTANCE;
    }

    protected AeaaOtherTypeStore() {
        super(AeaaOtherTypeIdentifier.class);
    }

    @Override
    protected AeaaOtherTypeIdentifier createIdentifier(String name, String implementation) {
        return new AeaaOtherTypeIdentifier(name, AeaaContentIdentifierStore.AeaaContentIdentifier.deriveWellFormedName(name), implementation, Pattern.compile("UNKNOWN", Pattern.CASE_INSENSITIVE));
    }

}
