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
package org.metaeffekt.core.util;

import java.awt.*;
import java.util.StringJoiner;

public enum ColorScheme {

    // general colors; color, darkModeColor
    STRONG_DARK_RED(new Color(142, 45, 45), new Color(255, 0, 0)),
    STRONG_RED(new Color(255, 23, 45)),
    STRONG_DARK_ORANGE(new Color(255, 90, 14)),
    STRONG_LIGHT_ORANGE(new Color(255, 155, 5)),
    STRONG_YELLOW(new Color(255, 204, 0)),
    STRONG_LIGHT_GREEN(new Color(87, 201, 10)),
    STRONG_DARK_GREEN(new Color(72, 166, 8), new Color(84, 190, 11)),
    STRONG_AQUA(new Color(0, 198, 187)),
    STRONG_BLUE(new Color(41, 144, 255)),
    STRONG_DARK_BLUE(new Color(13, 110, 253), new Color(46, 132, 255)),
    STRONG_PURPLE(new Color(212, 69, 255)),
    PASTEL_RED(new Color(255, 126, 119)),
    PASTEL_ORANGE(new Color(255, 204, 112)),
    PASTEL_YELLOW(new Color(255, 225, 106)),
    PASTEL_GREEN(new Color(119, 221, 119)),
    PASTEL_BLUE(new Color(132, 191, 255)),
    PASTEL_PURPLE(new Color(227, 178, 242)),
    STRONG_DARK(new Color(52, 58, 64)),
    STRONG_GRAY(new Color(99, 99, 99)),
    PASTEL_GRAY(new Color(202, 206, 207)),
    PASTEL_WHITE(new Color(241, 243, 245)),
    // usage-specific colors
    CVSS_2(new Color(255, 79, 116)),
    CVSS_3(new Color(45, 156, 231)),
    CVSS_4(new Color(255, 197, 62)),
    // document colors
    TEXT_COLOR_WHITE(new Color(236, 234, 231)),
    TEXT_COLOR_BLACK(new Color(33, 37, 41)),
    TEXT_COLOR(TEXT_COLOR_BLACK.color, TEXT_COLOR_WHITE.color),
    TEXT_COLOR_INVERTED(TEXT_COLOR_WHITE.color, TEXT_COLOR_BLACK.color),
    BACKGROUND_COLOR(new Color(255, 255, 255), new Color(27, 31, 41));

    private final Color color;
    private final Color darkModeColor;

    ColorScheme(Color color, Color darkModeColor) {
        this.color = color;
        this.darkModeColor = darkModeColor;
    }

    ColorScheme(Color color) {
        this(color, color);
    }

    public Color getColor() {
        return color;
    }

    public Color getDarkModeColor() {
        return darkModeColor;
    }

    public String toHex() {
        return toHex(this.color);
    }

    public String toDarkModeHex() {
        return toHex(this.darkModeColor);
    }

    public static String toHex(Color color) {
        if (color == null) return toHex(STRONG_PURPLE.color);
        return "#" + Integer.toHexString(color.getRGB()).substring(2);
    }

    public String toRgb() {
        return toRgb(this.color);
    }

    public String toDarkModeRgb() {
        return toRgb(this.darkModeColor);
    }

    public static String toRgb(Color color) {
        if (color == null) return toRgb(STRONG_PURPLE.color);
        return color.getRed() + "," + color.getGreen() + "," + color.getBlue();
    }

    public static Color setOpacity(Color color, float opacity) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), a(opacity));
    }

    private static int a(float a) {
        return (int) (a * 255.0);
    }

    public String getCssRootName() {
        return name().toLowerCase().replace("_", "-");
    }

    public Color getTextColor() {
        final double lightness = (0.2126 * color.getRed() + 0.7152 * color.getGreen() + 0.0722 * color.getBlue()) / 255;
        return lightness < 0.5 ? TEXT_COLOR_WHITE.color : TEXT_COLOR_BLACK.color;
    }

    public String getHexTextColor() {
        return toHex(getTextColor());
    }

    public static String cssRoot() {
        final StringJoiner rootContent = new StringJoiner(";--");
        for (ColorScheme value : values()) {
            rootContent.add(value.getCssRootName() + ":" + value.toHex());
            rootContent.add(value.getCssRootName() + "-rgb" + ":" + value.toRgb());
        }
        final StringJoiner darkRootContent = new StringJoiner(";--");
        for (ColorScheme value : values()) {
            darkRootContent.add(value.getCssRootName() + ":" + value.toDarkModeHex());
            darkRootContent.add(value.getCssRootName() + "-rgb" + ":" + value.toDarkModeRgb());
        }
        return ":root, :root.ae-light{--" + rootContent + ";}" +
                "@media (prefers-color-scheme: dark){:root{--" + darkRootContent + ";}}" +
                ":root.ae-dark{--" + darkRootContent + ";}";
    }

    public static ColorScheme getColor(String identifier) {
        if (identifier == null) return null;
        identifier = identifier.trim();
        for (ColorScheme value : values()) {
            if (value.name().equals(identifier) || value.getCssRootName().replace("-", "").equals(identifier.toLowerCase().replace("_", "").replace("-", ""))) {
                return value;
            }
        }
        return null;
    }

    public static Color deriveColor(Color color, int alpha) {
        return new Color(color.getRed(), color.getGreen(), color.getBlue(), alpha);
    }
}
