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

import org.json.JSONObject;


public class AeaaEpssData {

    private String vulnerability;
    private float epssScore;
    private float percentile;


    public static AeaaEpssData fromJson(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        final AeaaEpssData AeaaEpssData = new AeaaEpssData();
        AeaaEpssData.setVulnerability((String) jsonObject.get("vulnerability"));
        AeaaEpssData.setEpssScore(Float.parseFloat((String) jsonObject.get("epssScore")));
        AeaaEpssData.setPercentile(Float.parseFloat((String) jsonObject.get("percentile")));
        return AeaaEpssData;
    }

    public JSONObject toJson() {
        final JSONObject jsonObject = new JSONObject();
        jsonObject.put("vulnerability", vulnerability);
        jsonObject.put("epssScore", String.format("%f", epssScore));
        jsonObject.put("percentile", String.format("%f", percentile));
        return jsonObject;
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

    public String getVulnerability(){
        return vulnerability;
    }

    public float getEpssScore(){
        return epssScore;
    }

    public float getPercentile() {
        return percentile;
    }
}
