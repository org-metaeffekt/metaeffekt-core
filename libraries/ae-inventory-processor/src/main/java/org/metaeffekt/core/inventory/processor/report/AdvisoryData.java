/*
 * Copyright 2009-2021 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.report;

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class AdvisoryData {

    public static final String EMPTY_STRING = "";

    private String id = EMPTY_STRING;
    private String source = EMPTY_STRING;
    private String url = EMPTY_STRING;
    private String overview = EMPTY_STRING;
    private String description = EMPTY_STRING;
    private String threat = EMPTY_STRING;
    private String recommendations = EMPTY_STRING;
    private String workarounds = EMPTY_STRING;
    private String acknowledgements = EMPTY_STRING;
    private String type = EMPTY_STRING;

    private String createDate;
    private String updateDate;

    @Override
    public String toString() {
        return "AdvisoryData{" +
                "id='" + id + '\'' +
                ", source='" + source + '\'' +
                ", url='" + url + '\'' +
                ", overview='" + overview + '\'' +
                ", description='" + description + '\'' +
                ", threat='" + threat + '\'' +
                ", recommendations='" + recommendations + '\'' +
                ", workarounds='" + workarounds + '\'' +
                ", acknowledgements='" + acknowledgements + '\'' +
                ", type='" + type + '\'' +
                ", createDate='" + createDate + '\'' +
                ", updateDate='" + updateDate + '\'' +
                '}';
    }

    public static List<AdvisoryData> fromCertFr(String certFrJson) {
        final List<AdvisoryData> advisoryDataList = new ArrayList<>();
        final JSONArray jsonArray = new JSONArray(certFrJson);

        for (int i = 0; i < jsonArray.length(); i++) {
            final JSONObject entry = jsonArray.getJSONObject(i);
            advisoryDataList.add(extractAdvisoryDataFromCertFr(entry));
        }

        return advisoryDataList;
    }

    public static List<AdvisoryData> fromCertSei(String certSei) {
        final List<AdvisoryData> advisoryDataList = new ArrayList<>();
        final JSONArray jsonArray = new JSONArray(certSei);

        for (int i = 0; i < jsonArray.length(); i++) {
            final JSONObject entry = jsonArray.getJSONObject(i);
            advisoryDataList.add(extractAdvisoryDataFromCertSei(entry));
        }

        return advisoryDataList;
    }

    public static List<AdvisoryData> fromMsrc(String certMsrc, VulnerabilityMetaData vmd) {
        final List<AdvisoryData> advisoryDataList = new ArrayList<>();

        if (certMsrc.startsWith("[")) {
            JSONArray jsonArray = new JSONArray(certMsrc);
            for (int i = 0; i < jsonArray.length(); i++) {
                final JSONObject entry = jsonArray.getJSONObject(i);
                advisoryDataList.add(extractAdvisoryDataFromMsrc(entry, vmd));
            }
        } else {
            JSONObject entry = new JSONObject(certMsrc);
            advisoryDataList.add(extractAdvisoryDataFromMsrc(entry, vmd));
        }

        return advisoryDataList;
    }

    private static AdvisoryData extractAdvisoryDataFromCertFr(final JSONObject entry) {
        final AdvisoryData advisoryData = new AdvisoryData();

        advisoryData.id = entry.optString("certfr");
        advisoryData.type = normalizeType(advisoryData.id == null ? "info" : CertFrUtils.getType(advisoryData.id));
        if (advisoryData.id != null) {
            advisoryData.url = CertFrUtils.toURL(advisoryData.id);
        }
        advisoryData.source = "CERT-FR";
        advisoryData.overview = formatString(entry.optString("topic"));

        // FIXME: all entries should have a create and update date
        advisoryData.createDate = parseDate(null);
        advisoryData.updateDate = parseDate(null);

        final JSONArray furtherDetails = entry.getJSONArray("furtherDetails");

        for (int j = 0; j < furtherDetails.length(); j++) {
            final JSONObject detail = furtherDetails.getJSONObject(j);

            if (detail.has("title")) {
                String title = detail.optString("title");
                if (title != null) {
                    String content = formatString(detail.optString(("content")));

                    if ("Summary".equalsIgnoreCase(title)) {
                        advisoryData.description = content;
                    }
                    if ("Risk".equalsIgnoreCase(title)) {
                        advisoryData.threat = content;
                    }
                    if ("Solution".equalsIgnoreCase(title)) {
                        advisoryData.recommendations = content;
                    }
                }
            }
        }
        return advisoryData;
    }

    private static String normalizeType(String type) {
        type = type.toLowerCase().trim();
        if (type.equalsIgnoreCase("notice")) {
            return "notice";
        }
        if (type.equalsIgnoreCase("alert")) {
            return "alert";
        }
        if (type.equalsIgnoreCase("news")) {
            return "news";
        }
        if (type.equalsIgnoreCase("info")) {
            return "notice";
        }
        if (type.equalsIgnoreCase("advisory")) {
            return "alert";
        }
        if (type.equalsIgnoreCase("cna")) {
            return "alert";
        }
        if (type.equalsIgnoreCase("compromise indicators")) {
            return "alert";
        }
        if (type.equalsIgnoreCase("hardening and recommendations")) {
            return "alert";
        }
        if (type.equalsIgnoreCase("threats and incidents")) {
            return "alert";
        }
        if (type.equalsIgnoreCase("Description")) {
            return "notice";
        }
        if (type.equalsIgnoreCase("tag")) {
            return "notice";
        }

        /*
            Notice: AVI
            Alert: ALE
            Compromise Indicators: IOC
            Hardening and Recommendations: DUR
            News: ACT
            Threats and Incidents: CTI
            Information: INF/REC

            if (certfr.contains("AVI"))
                return "Notice";
            else if (certfr.contains("ALE"))
                return "Alert";
            else if (certfr.contains("IOC"))
                return "Compromise Indicators";
            else if (certfr.contains("DUR"))
                return "Hardening and Recommendations";
            else if (certfr.contains("ACT"))
                return "News";
            else if (certfr.contains("CTI"))
                return "Threats and Incidents";
         */

        return "notice";
    }

    private static AdvisoryData extractAdvisoryDataFromMsrc(JSONObject entry, VulnerabilityMetaData vmd) {
        AdvisoryData advisoryData = new AdvisoryData();
        String id = vmd.get(VulnerabilityMetaData.Attribute.NAME);

        advisoryData.url = "https://msrc.microsoft.com/update-guide/en-US/vulnerability/" + id;
        advisoryData.id = "MSRC-" + id;
        advisoryData.source = "MSRC";

        // FIXME: check whether this is correct and only advisories are provided
        advisoryData.type = normalizeType("advisory");

        advisoryData.recommendations = null;
        advisoryData.acknowledgements = null;
        advisoryData.threat = null;

        // FIXME: all entries should have a create and update date
        advisoryData.createDate = parseDate(null);
        advisoryData.updateDate = parseDate(null);

        advisoryData.overview = entry.optString("title");

        JSONArray notes = entry.getJSONArray("notes");

        for (int j = 0; j < notes.length(); j++) {

            final JSONObject detail = notes.getJSONObject(j);

            if (detail.has("type")) {
                String type = normalizeType(detail.getString("type"));
                String content = detail.optString("content");

                // FIXME: currently the content is unstructured (apart from html headings)
                if ("Description".equalsIgnoreCase(type)) {
                    advisoryData.description = content;
                    continue;
                }
            }
        }
        return advisoryData;
    }

    private static AdvisoryData extractAdvisoryDataFromCertSei(JSONObject entry) {
        AdvisoryData advisoryData = new AdvisoryData();
        String id = entry.optString("id");

        // FIXME: clarify id handling; insert id into json
        advisoryData.id = "VU#" + id;
        advisoryData.url = "https://kb.cert.org/vuls/id/" + id;
        advisoryData.source = "CERT-SEI";

        advisoryData.description = entry.optString("overview");

        // FIXME: check whether this is correct and only advisories are provided
        advisoryData.type = normalizeType("advisory");

        // FIXME: all entries should have a create and update date
        advisoryData.createDate = parseDate(null);
        advisoryData.updateDate = parseDate(entry.optString("dateupdated"));

        // FIXME: overview shows mixed content as it seems to be markdown formatted; where to split?
        advisoryData.overview = entry.optString("name");

        return advisoryData;
    }

    private static String parseDate(String string) {
        if (string == null) return "n.a.";
        try {
            return LocalDate.parse(string).format(DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (Exception e) {
            if (string.contains("T")) {
                return string.substring(0, string.indexOf("T"));
            }
            return string;
        }
    }

    public String getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public String getUrl() {
        return url;
    }

    public String getOverview() {
        return overview;
    }

    public String getDescription() {
        return description;
    }

    public String getThreat() {
        return threat;
    }

    public String getRecommendations() {
        return recommendations;
    }

    public String getWorkarounds() {
        return workarounds;
    }

    public String getAcknowledgements() {
        return acknowledgements;
    }

    public String getType() {
        return type;
    }

    public String getCreateDate() {
        return createDate;
    }

    public String getUpdateDate() {
        return updateDate;
    }

    private static String formatString(String string) {
        if (!StringUtils.hasText(string)) return EMPTY_STRING;
        string = string.replace("\r\n", " ");
        string = string.replace("\n", " ");

        string = string.replace("(cid:160)", " ");

        string = string.replace("Note:", "-PSTART-Note:-PEND-");
        string = string.replace("Note :", "-PSTART-Note:-PEND-");

        string = string.replace("Pour rappel:", "-PSTART-Pour rappel:-PEND-");
        string = string.replace("Pour rappel :", "-PSTART-Pour rappel:-PEND-");

        string = string.replace(" : ", ":-NEWLINE-");
        string = string.replace(" ; ", ";-NEWLINE-");

        return string;
    }

}
