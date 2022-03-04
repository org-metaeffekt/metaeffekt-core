package org.metaeffekt.core.inventory.processor.report;

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
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
        advisoryData.type = advisoryData.id == null ? "info" : CertFrUtils.getType(advisoryData.id);
        if (advisoryData.id != null) {
            advisoryData.url = CertFrUtils.toURL(advisoryData.id);
        }
        advisoryData.source = "CERT-FR";
        advisoryData.overview = formatString(entry.optString("topic"));

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

    private static AdvisoryData extractAdvisoryDataFromMsrc(JSONObject entry, VulnerabilityMetaData vmd) {
        AdvisoryData advisoryData = new AdvisoryData();
        String id = vmd.get(VulnerabilityMetaData.Attribute.NAME);

        advisoryData.url = "https://msrc.microsoft.com/update-guide/en-US/vulnerability/" + id;
        advisoryData.id = "MSRC-" + id;
        advisoryData.source = "MSRC";

        // FIXME: check whether this is correct and only advisories are provided
        advisoryData.type = "advisory";

        advisoryData.recommendations = null;
        advisoryData.acknowledgements = null;
        advisoryData.threat = null;

        // FIXME: currently the content is unstructured (apart from html headings)
        advisoryData.overview = entry.optString("title");

        JSONArray notes = entry.getJSONArray("notes");

        for (int j = 0; j < notes.length(); j++) {

            final JSONObject detail = notes.getJSONObject(j);

            if (detail.has("type")) {
                String type = detail.getString("type");
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

        advisoryData.description = entry.optString("name");

        // FIXME: overview shows mixed content as it seems to be markdown formatted; where to split?
        advisoryData.overview = entry.optString("overview");

        return advisoryData;
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