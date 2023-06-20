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
package org.metaeffekt.core.inventory.processor.writer;

import static org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData.VULNERABILITY_ASSESSMENT_CONTEXT_DEFAULT;
import static org.metaeffekt.core.inventory.processor.writer.InventoryWriter.SINGLE_VULNERABILITY_ASSESSMENT_WORKSHEET;
import static org.metaeffekt.core.inventory.processor.writer.InventoryWriter.VULNERABILITY_ASSESSMENT_WORKSHEET_PREFIX;

public class AbstractInventoryWriter {

    public String assessmentContextToSheetName(String assessmentContext) {
        if (VULNERABILITY_ASSESSMENT_CONTEXT_DEFAULT.equals(assessmentContext)) {
            return SINGLE_VULNERABILITY_ASSESSMENT_WORKSHEET;
        } else {
            return VULNERABILITY_ASSESSMENT_WORKSHEET_PREFIX + assessmentContext;
        }
    }

}