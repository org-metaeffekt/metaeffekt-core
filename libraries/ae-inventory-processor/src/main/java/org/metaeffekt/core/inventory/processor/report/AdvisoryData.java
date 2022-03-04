package org.metaeffekt.core.inventory.processor.report;

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
                '}';
    }

    public static List<AdvisoryData> fromCertFr(String certFrJson) {
        final List<AdvisoryData> advisoryDataList = new ArrayList<>();

        JSONArray jsonArray = new JSONArray(certFrJson);

        for (int i = 0; i < jsonArray.length(); i++) {
            final JSONObject entry = jsonArray.getJSONObject(i);

            AdvisoryData advisoryData = new AdvisoryData();
            advisoryData.id = entry.optString("certfr");
            advisoryData.type = advisoryData.id == null ? "info" : CertFrUtils.getType(advisoryData.id);
            if (advisoryData.id != null) {
                advisoryData.url = CertFrUtils.toURL(advisoryData.id);
            }
            advisoryData.source = "CERT-FR";
            advisoryData.overview = formatString(entry.optString("topic"));

            JSONArray furtherDetails = entry.getJSONArray("furtherDetails");

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

            advisoryDataList.add(advisoryData);
        }

        return advisoryDataList;
    }

    public static List<AdvisoryData> fromMsrc(String certMsrc, VulnerabilityMetaData vmd) {
        final List<AdvisoryData> advisoryDataList = new ArrayList<>();

        if (certMsrc.startsWith("[")) {
            JSONArray jsonArray = new JSONArray(certMsrc);
            for (int i = 0; i < jsonArray.length(); i++) {
                final JSONObject entry = jsonArray.getJSONObject(i);

            }
        } else {
            JSONObject entry = new JSONObject(certMsrc);

            AdvisoryData advisoryData = new AdvisoryData();
            String id = vmd.get(VulnerabilityMetaData.Attribute.NAME);

            advisoryData.url = "https://msrc.microsoft.com/update-guide/en-US/vulnerability/" + id;
            advisoryData.id = "MSRC-" + id;

            advisoryData.overview = entry.optString("title");

            JSONArray notes = entry.getJSONArray("notes");

            for (int j = 0; j < notes.length(); j++) {

                final JSONObject detail = notes.getJSONObject(j);

                if (detail.has("type")) {
                    String type = detail.getString("type");
                    String content = detail.optString("content");

                    if ("Description".equalsIgnoreCase(type)) {
                        advisoryData.description = content;
                        continue;
                    }
                }
            }

            advisoryDataList.add(advisoryData);

        }

        return advisoryDataList;
    }

    public static List<AdvisoryData> fromCertSei(String certSei) {
        final List<AdvisoryData> advisoryDataList = new ArrayList<>();

        final JSONArray jsonArray = new JSONArray(certSei);

        for (int i = 0; i < jsonArray.length(); i++) {
            final JSONObject entry = jsonArray.getJSONObject(i);

            AdvisoryData advisoryData = new AdvisoryData();
            String id = entry.optString("id");

            // FIXME: clarify id handling
            advisoryData.id = "VU#" + id;
            advisoryData.url = "https://kb.cert.org/vuls/id/" + id;

            advisoryData.description = entry.optString("name");

            // FIXME: in the current log4j inventory the filed shows mixed content as it seems to be markdown formatted
            advisoryData.overview = entry.optString("overview");

            advisoryDataList.add(advisoryData);
        }

        return advisoryDataList;
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
