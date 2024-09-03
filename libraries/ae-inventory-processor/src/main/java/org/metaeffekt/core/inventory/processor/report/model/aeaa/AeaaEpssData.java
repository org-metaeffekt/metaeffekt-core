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
package org.metaeffekt.core.inventory.processor.report.model.aeaa;

import lombok.NoArgsConstructor;
import org.json.JSONObject;


@NoArgsConstructor
public class AeaaEpssData {

    private String vulnerability;
    private float epssScore;
    private float percentile;

    public AeaaEpssData(String vulnerability, double epssScore, double percentile) {
        this.vulnerability = vulnerability;
        this.epssScore = (float) epssScore;
        this.percentile = (float) percentile;
    }

    public static AeaaEpssData fromJson(JSONObject json) {
        if (json == null) {
            return null;
        }
        final AeaaEpssData epssData = new AeaaEpssData();
        epssData.setVulnerability((String) json.get("vulnerability"));
        epssData.setEpssScore(parseFloat(json.get("epssScore")));
        epssData.setPercentile(parseFloat(json.get("percentile")));
        return epssData;
    }

    public JSONObject toJson() {
        final JSONObject json = new JSONObject();
        json.put("vulnerability", vulnerability);
        json.put("epssScore", epssScore);
        json.put("percentile", percentile);
        return json;
    }

    private static float parseFloat(Object value) {
        if (value == null) {
            return 0;
        } else if (value instanceof Number) {
            return ((Number) value).floatValue();
        } else if (!(value instanceof String)) {
            return 0;
        }

        final String stringValue = (String) value;

        // check for both "," and "." as decimal separator
        try {
            return Float.parseFloat(stringValue);
        } catch (NumberFormatException e) {
            return Float.parseFloat(stringValue.replace(",", "."));
        }
    }

    public void setVulnerability(String vulnerability) {
        this.vulnerability = vulnerability;
    }

    public void setEpssScore(float epssScore) {
        this.epssScore = epssScore;
    }

    public void setPercentile(float percentile) {
        this.percentile = percentile;
    }

    public String getVulnerability() {
        return vulnerability;
    }

    public float getEpssScore() {
        return epssScore;
    }

    public float getPercentile() {
        return percentile;
    }

    public String getEpssScoreAsPercentage() {
        return String.format("%.2f", epssScore * 100) + "%";
    }

    public String getTopRatedPercentileAsPercentage() {
        return String.format("%.2f", 100 - percentile * 100) + "%";
    }
}
