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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.eol.export;

import org.metaeffekt.core.util.ColorScheme;

public enum CycleScenarioRating {
    RATING_1(1, ColorScheme.STRONG_DARK_GREEN),
    RATING_2(2, ColorScheme.STRONG_LIGHT_GREEN),
    RATING_3(3, ColorScheme.STRONG_YELLOW),
    RATING_4(4, ColorScheme.STRONG_DARK_ORANGE),
    RATING_5(5, ColorScheme.STRONG_RED);

    private final int rating;
    private final ColorScheme color;

    CycleScenarioRating(int rating, ColorScheme color) {
        this.rating = rating;
        this.color = color;
    }

    public int getRating() {
        return rating;
    }

    public ColorScheme getColor() {
        return color;
    }
}
