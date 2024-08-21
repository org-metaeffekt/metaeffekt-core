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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.eol.state;

public enum AeaaTechnicalGuidanceState {
    /**
     * Means, that there has never been any guidance.
     */
    NO_TECHNICAL_GUIDANCE,
    /**
     * Does not currently occur in the EOL Date data (as of 2023-07-04).
     */
    TECHNICAL_GUIDANCE,
    UPCOMING_END_OF_TECHNICAL_GUIDANCE_DATE,
    END_OF_TECHNICAL_GUIDANCE_DATE_REACHED;
}
