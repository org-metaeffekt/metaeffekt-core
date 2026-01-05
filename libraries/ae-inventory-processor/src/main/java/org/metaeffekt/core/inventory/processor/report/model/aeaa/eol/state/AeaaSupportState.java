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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.eol.state;

import org.metaeffekt.core.util.ColorScheme;

public enum AeaaSupportState {
    NO_SUPPORT("No support", ColorScheme.STRONG_RED),
    SUPPORT("Support available", ColorScheme.STRONG_DARK_GREEN),
    SUPPORT_END_DATE_REACHED("Support end reached", ColorScheme.STRONG_RED),
    DISTANT_SUPPORT_END_DATE("Upcoming support end", ColorScheme.STRONG_DARK_GREEN),
    UPCOMING_SUPPORT_END_DATE("Upcoming support end", ColorScheme.STRONG_YELLOW);

    private final String shortDescription;
    private final ColorScheme color;

    AeaaSupportState(String shortDescription, ColorScheme color) {
        this.shortDescription = shortDescription;
        this.color = color;
    }

    public String getShortDescription() {
        return shortDescription;
    }

    public static AeaaSupportState fromEolState(AeaaEolState eolState) {
        switch (eolState) {
            case NOT_EOL:
                return SUPPORT;
            case EOL:
                return NO_SUPPORT;
            case EOL_DATE_REACHED:
                return SUPPORT_END_DATE_REACHED;
            case DISTANT_EOL_DATE:
                return DISTANT_SUPPORT_END_DATE;
            case UPCOMING_EOL_DATE:
                return UPCOMING_SUPPORT_END_DATE;
            default:
                throw new IllegalStateException("Unexpected value: " + eolState);
        }
    }

    public ColorScheme getColor() {
        return color;
    }
}
