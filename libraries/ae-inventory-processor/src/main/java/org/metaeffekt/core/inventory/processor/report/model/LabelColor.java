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
package org.metaeffekt.core.inventory.processor.report.model;

import java.awt.*;

public class LabelColor {

    private final String background;
    private final String foreground;

    public LabelColor(String background, String foreground) {
        this.background = background;
        this.foreground = foreground;
    }

    public String getBackground() {
        return background;
    }

    public String getForeground() {
        return foreground;
    }

    /**
     * Converts a HEX color into a label color by evaluating the background brightness and setting the foreground to
     * either black or white based on that.
     *
     * @param color The background color to use.
     * @return A LabelColor containing background and foreground colors.
     */
    public static LabelColor getLabelColorForBackgroundColor(String color) {
        if (color != null) {
            int rgb = Integer.parseInt(color.substring(1), 16);
            int r = (rgb >> 16) & 0xff;
            int g = (rgb >> 8) & 0xff;
            int b = (rgb) & 0xff;
            double luma = 0.2126 * r + 0.7152 * g + 0.0722 * b;
            String backgroundColor = luma < 120 ? "white" : "black";
            return new LabelColor(color, backgroundColor);
        }
        return new LabelColor("#cfcfc4", "black");
    }

    public static LabelColor getLabelColorForBackgroundColor(int r, int g, int b) {
        return getLabelColorForBackgroundColor(String.format("#%02x%02x%02x", r, g, b));
    }

    public static LabelColor getLabelColorForBackgroundColor(Color color) {
        return getLabelColorForBackgroundColor(color.getRed(), color.getGreen(), color.getBlue());
    }
}
