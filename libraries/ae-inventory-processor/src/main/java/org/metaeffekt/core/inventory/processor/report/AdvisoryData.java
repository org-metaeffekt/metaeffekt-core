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
package org.metaeffekt.core.inventory.processor.report;

import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.util.StringUtils;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class AdvisoryData {

    public static final String EMPTY_STRING = "";

    private String id = EMPTY_STRING;
    private String source = EMPTY_STRING;
    private String url = EMPTY_STRING;

    private String summary = EMPTY_STRING;
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
                ", summary='" + summary + '\'' +
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

    public static List<AdvisoryData> fromJson(String jsonString) {
        if (jsonString != null && jsonString.length() > 0) {
            if (jsonString.charAt(0) == '{') {
                return Collections.singletonList(fromJson(new JSONObject(jsonString)));
            } else if (jsonString.charAt(0) == '[') {
                return fromJson(new JSONArray(jsonString));
            }
        }

        return new ArrayList<>();
    }

    public static List<AdvisoryData> fromJson(JSONArray advisoryJson) {
        final List<AdvisoryData> advisoryDataList = new ArrayList<>();

        for (int i = 0; i < advisoryJson.length(); i++) {
            final JSONObject json = advisoryJson.optJSONObject(i);
            if (json != null) {
                advisoryDataList.add(fromJson(json));
            }
        }

        return advisoryDataList;
    }

    public static AdvisoryData fromJson(JSONObject advisoryJson) {
        String source = advisoryJson.optString("source");

        if (source != null) {
            switch (source.replaceAll("[-_ ]", "").toUpperCase()) {
                case "CERTFR":
                    return extractAdvisoryDataFromCertFr(advisoryJson);
                case "CERTSEI":
                    return extractAdvisoryDataFromCertSei(advisoryJson);
                case "MSRC":
                    return extractAdvisoryDataFromMsrc(advisoryJson);
            }
        }

        return null;
    }

    private static AdvisoryData extractAdvisoryDataFromCertFr(final JSONObject entry) {
        final AdvisoryData advisoryData = new AdvisoryData();

        advisoryData.id = entry.optString("id", EMPTY_STRING);
        advisoryData.url = extractUrl(entry.opt("url"));
        advisoryData.source = entry.optString("source", EMPTY_STRING);
        advisoryData.summary = formatString(entry.optString("summary", EMPTY_STRING));
        advisoryData.description = formatString(entry.optString("description", EMPTY_STRING));
        advisoryData.threat = formatString(entry.optString("threat", EMPTY_STRING));
        advisoryData.recommendations = formatString(entry.optString("recommendations", EMPTY_STRING));
        advisoryData.createDate = normalizeDate(entry.optString("createDate", EMPTY_STRING));
        advisoryData.updateDate = normalizeDate(entry.optString("updateDate", EMPTY_STRING));
        advisoryData.type = normalizeType(entry.optString("type", EMPTY_STRING));

        return advisoryData;
    }

    private static AdvisoryData extractAdvisoryDataFromMsrc(JSONObject entry) {
        AdvisoryData advisoryData = new AdvisoryData();

        advisoryData.id = entry.optString("id", EMPTY_STRING);
        advisoryData.url = extractUrl(entry.opt("url"));
        advisoryData.source = entry.optString("source", EMPTY_STRING);
        advisoryData.summary = formatString(entry.optString("summary", EMPTY_STRING));
        // FIXME: currently the content is unstructured (apart from html headings)
        advisoryData.description = entry.optString("description", EMPTY_STRING);

        advisoryData.threat = extractMultilineStringFromJsonArray(entry.optJSONArray("threat"));
        advisoryData.recommendations = extractMultilineStringFromJsonArray(entry.optJSONArray("recommendations"));

        advisoryData.acknowledgements = entry.optString("acknowledgements", EMPTY_STRING);
        advisoryData.createDate = normalizeDate(entry.optString("createDate", EMPTY_STRING));
        advisoryData.updateDate = normalizeDate(entry.optString("updateDate", EMPTY_STRING));
        advisoryData.type = normalizeType(entry.optString("type", "advisory"));

        return advisoryData;
    }

    private static AdvisoryData extractAdvisoryDataFromCertSei(JSONObject entry) {
        AdvisoryData advisoryData = new AdvisoryData();

        advisoryData.id = entry.optString("id", EMPTY_STRING);
        advisoryData.url = extractUrl(entry.opt("url"));
        advisoryData.source = entry.optString("source", EMPTY_STRING);
        advisoryData.summary = entry.optString("summary", EMPTY_STRING);
        advisoryData.description = entry.optString("description", EMPTY_STRING);
        advisoryData.threat = entry.optString("threat", EMPTY_STRING);
        advisoryData.recommendations = entry.optString("recommendations", EMPTY_STRING);
        advisoryData.workarounds = entry.optString("workarounds", EMPTY_STRING);
        advisoryData.acknowledgements = entry.optString("acknowledgements", EMPTY_STRING);
        advisoryData.createDate = normalizeDate(entry.optString("createDate", EMPTY_STRING));
        advisoryData.updateDate = normalizeDate(entry.optString("updateDate", EMPTY_STRING));
        advisoryData.type = normalizeType(entry.optString("type", "advisory"));

        return advisoryData;
    }

    private static String extractMultilineStringFromJsonArray(JSONArray array) {
        if (array == null || array.length() == 0) {
            return EMPTY_STRING;
        }

        StringJoiner lines = new StringJoiner("\n");
        for (int i = 0; i < array.length(); i++) {
            lines.add(array.optString(i));
        }

        return lines.toString();
    }

    private static String extractUrl(Object url) {
        if (url == null || url.equals("")) return EMPTY_STRING;
        if (url instanceof JSONObject) {
            return ((JSONObject) url).optString("url");
        } else if (url instanceof String) {
            return (String) url;
        } else {
            return EMPTY_STRING;
        }
    }

    private final static Map<String, List<String>> TYPE_NORMALIZATION_MAP = new HashMap<String, List<String>>() {{
        put("notice", Arrays.asList(
                "notice", "info", "Description", "tag",
                "avis", "ioc", "cti", "information" // CERT-FR
        ));
        put("alert", Arrays.asList(
                "alert", "advisory", "cna", "compromise indicators",
                "hardening and recommendations", "threats and incidents",
                "alerte" // CERT-FR
        ));
        put("news", Arrays.asList(
                "news",
                "actualite", "dur" // CERT-FR
        ));
    }};

    public static String normalizeType(String type) {
        if (org.apache.commons.lang3.StringUtils.isEmpty(type)) {
            return "notice";
        }

        final String inputNormalizedType = type.toLowerCase().trim();

        for (Map.Entry<String, List<String>> entry : TYPE_NORMALIZATION_MAP.entrySet()) {
            final String normalizedType = entry.getKey();
            final List<String> typeList = entry.getValue();

            for (String t : typeList) {
                if (inputNormalizedType.equalsIgnoreCase(t)) {
                    return normalizedType;
                }
            }
        }

        return "notice";
    }

    private final static List<SimpleDateFormat> DATE_FORMATS = Arrays.asList(
            new SimpleDateFormat("yyyy-MM-dd"), // target format
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
            new SimpleDateFormat("dd MMMMM yyyy", Locale.GERMAN),
            new SimpleDateFormat("dd MMMMM yyyy", Locale.FRANCE),
            new SimpleDateFormat("dd MMMMM yyyy", Locale.ENGLISH),
            new SimpleDateFormat("dd MMMMM yyyy"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ")
    );

    static String normalizeDate(String string) {
        if (string == null) return "n.a.";

        final Date parsedDate = tryParse(string);

        if (parsedDate != null) {
            return DATE_FORMATS.get(0).format(parsedDate);
        } else {
            if (string.contains("T")) {
                return string.substring(0, string.indexOf("T"));
            } else {
                // do nothing
            }
        }

        return string;
    }

    private static Date tryParse(String dateString) {
        if (dateString.matches("\\d+")) {
            try {
                return new Date(Long.parseLong(dateString));
            } catch (NumberFormatException ignored) {
            }
        }

        for (SimpleDateFormat formatter : DATE_FORMATS) {
            try {
                return formatter.parse(dateString);
            } catch (ParseException ignored) {
            }
        }
        return null;
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

    public String getSummary() {
        return summary;
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
