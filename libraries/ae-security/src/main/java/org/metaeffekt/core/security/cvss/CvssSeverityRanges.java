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
package org.metaeffekt.core.security.cvss;

import org.apache.commons.lang3.StringUtils;
import org.metaeffekt.core.util.ColorScheme;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * In order to map CVSS scores to a human-readable severity category name, the {@link CvssSeverityRanges} class allows
 * for the definition of multiple {@link SeverityRange}s that define a lower and upper score boundary, together with the
 * category name and a color.
 * <p>
 * In most cases, the {@link CvssSeverityRanges#CVSS_3_SEVERITY_RANGES} should be used, which is also the default value
 * for all operations.
 */
public class CvssSeverityRanges {

    private static final Logger LOG = LoggerFactory.getLogger(CvssSeverityRanges.class);

    private final SeverityRange[] ranges;

    public CvssSeverityRanges() {
        this("cvss3");
    }

    public CvssSeverityRanges(String input) {
        if (StringUtils.isEmpty(input)) {
            LOG.warn("No severity ranges defined. Using default (v3) ranges.");
            this.ranges = CVSS_3_SEVERITY_RANGES.ranges;
            return;
        }

        if (input.equals("cvss3") || input.equals("cvss4")) {
            this.ranges = CVSS_3_SEVERITY_RANGES.ranges;

        } else if (input.equals("cvss2")) {
            this.ranges = CVSS_2_SEVERITY_RANGES.ranges;

        } else {
            final String[] splitRangesInputs = input.split(", ?");
            this.ranges = new SeverityRange[splitRangesInputs.length];

            for (int i = 0; i < splitRangesInputs.length; i++) {
                this.ranges[i] = new SeverityRange(splitRangesInputs[i], i);
            }
        }
    }

    public SeverityRange getRange(double score) {
        for (SeverityRange range : ranges) {
            if (range.matches(score)) {
                return range;
            }
        }
        return UNDEFINED_SEVERITY_RANGE;
    }

    public SeverityRange getRangeByName(String name) {
        for (SeverityRange range : ranges) {
            if (range.getName().equals(name)) {
                return range;
            }
        }
        return UNDEFINED_SEVERITY_RANGE;
    }

    public SeverityRange[] getRanges() {
        return ranges;
    }

    public static class SeverityRange implements Comparable<SeverityRange> {
        private final String name;
        private final ColorScheme color;
        private final double floor, ceil;
        private final int index;

        private SeverityRange(String input, int index) {
            Matcher matcher = RANGE_PATTERN.matcher(input);
            if (!matcher.matches()) {
                throw new IllegalArgumentException("Range pattern does not match format [NAME:COLOR:FLOOR:CEIL] in " + input);
            }

            this.name = matcher.group(1).trim();
            this.color = ColorScheme.getColor(matcher.group(2));
            if (this.color == null) {
                throw new IllegalArgumentException("Range color unknown in [" + input + "]. available colors are [" + getAvailableColors() + "]");
            }

            final String floorStr = matcher.group(3).trim();
            final String ceilStr = matcher.group(4).trim();
            this.floor = StringUtils.isEmpty(floorStr) ? -Double.MAX_VALUE : Double.parseDouble(floorStr);
            this.ceil = StringUtils.isEmpty(ceilStr) ? Double.MAX_VALUE : Double.parseDouble(ceilStr);

            if (floorStr.isEmpty() && ceilStr.isEmpty()) {
                throw new IllegalArgumentException("Both floor and ceil cannot be empty in [" + input + "]");
            }
            if (this.floor > this.ceil) {
                throw new IllegalArgumentException("Range floor [" + this.floor + "] must be smaller than range ceil [" + this.ceil + "] in [" + input + "]");
            }

            this.index = index;
        }

        public boolean matches(double score) {
            return score >= floor && score <= ceil;
        }

        public String getName() {
            return name;
        }

        public ColorScheme getColor() {
            return color;
        }

        public double getFloor() {
            return floor;
        }

        public double getCeil() {
            return ceil;
        }

        public int getIndex() {
            return index;
        }

        private final static Pattern RANGE_PATTERN = Pattern.compile("([^:]+):([^:]+):([^:]*):([^:]*)");

        @Override
        public int compareTo(SeverityRange o) {
            return Double.compare(floor, o.floor);
        }

        @Override
        public String toString() {
            return String.format("%s:%s:%s:%s", name, color.getCssRootName(), floor, ceil);
        }
    }

    @Override
    public String toString() {
        return Arrays.stream(ranges).map(SeverityRange::toString).collect(Collectors.joining(","));
    }

    public final static SeverityRange UNDEFINED_SEVERITY_RANGE = new SeverityRange("Undefined:strong-gray:-100.0:100.0", -1);

    public static final CvssSeverityRanges CVSS_2_SEVERITY_RANGES = new CvssSeverityRanges(
            "Low:strong-yellow::3.9," +
                    "Medium:strong-light-orange:4.0:6.9," +
                    "High:strong-red:7.0:"
    );
    public static final CvssSeverityRanges CVSS_3_SEVERITY_RANGES = new CvssSeverityRanges(
            "None:pastel-gray::0.0," +
                    "Low:strong-yellow:0.1:3.9," +
                    "Medium:strong-light-orange:4.0:6.9," +
                    "High:strong-dark-orange:7.0:8.9," +
                    "Critical:strong-red:9.0:"
    );
    public static final CvssSeverityRanges PRIORITY_SCORE_SEVERITY_RANGES = new CvssSeverityRanges(
            "escalate:strong-red:9.0:," +
                    "due:strong-dark-orange:7.0:8.9," +
                    "elevated:strong-light-orange::6.9"
    );


    private static String getAvailableColors() {
        StringJoiner colors = new StringJoiner(", ");
        for (ColorScheme value : ColorScheme.values()) {
            colors.add(value.getCssRootName());
        }
        return colors.toString();
    }
}
