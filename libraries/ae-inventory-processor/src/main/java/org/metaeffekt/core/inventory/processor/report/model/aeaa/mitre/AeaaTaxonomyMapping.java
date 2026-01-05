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

package org.metaeffekt.core.inventory.processor.report.model.aeaa.mitre;

import lombok.Getter;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


/**
 * Taxonomy mappings involve aligning or correlating terms, concepts, or classifications from one taxonomy
 * to another. This process ensures consistency and interoperability between different systems or datasets
 * that use distinct taxonomies.
 */
@Getter
public class AeaaTaxonomyMapping {

    private final String source;
    private final String id;
    private final String name;
    private String url;

    private static final Map<String, String> urlMapping = new HashMap<>();

    static {
        urlMapping.put("ATTACK", "https://attack.mitre.org/techniques/T%s");
        urlMapping.put("OWASP Attacks", "https://owasp.org/www-community/attacks/%s");
        urlMapping.put("OWASP Top Ten 2007", "https://cwe.mitre.org/data/definitions/629.html");
        urlMapping.put("OWASP Top Ten 2004", "https://cwe.mitre.org/data/definitions/711.html");
        urlMapping.put("WASC", "http://projects.webappsec.org/%s");
        urlMapping.put("CERT C Secure Coding", "https://wiki.sei.cmu.edu/confluence/display/c/%s");
        urlMapping.put("SEI CERT Perl Coding Standard", "https://wiki.sei.cmu.edu/confluence/display/perl/%s");
        urlMapping.put("SEI CERT Oracle Coding Standard for Java", "https://wiki.sei.cmu.edu/confluence/display/java/%s");
        urlMapping.put("The CERT Oracle Secure Coding Standard for Java (2011)", "https://wiki.sei.cmu.edu/confluence/display/java/%s");
        urlMapping.put("PLOVER", "https://cwe.mitre.org/documents/sources/PLOVER.pdf");
        urlMapping.put("OMG ASCPEM", "https://www.omg.org/spec/ASCPEM/");
        urlMapping.put("OMG ASCMM", "https://www.omg.org/spec/ASCMM/");
        urlMapping.put("OMG ASCSM", "https://www.omg.org/spec/ASCSM/");
        urlMapping.put("OMG ASCRM", "https://www.omg.org/spec/ASCRM/");
        urlMapping.put("7 Pernicious Kingdoms", "https://cwe.mitre.org/data/definitions/700.html");
    }

    public AeaaTaxonomyMapping(String source, String id, String name) {
        this.source = source;
        this.id = id;
        this.name = name;

        switch (source) {
            case "ATTACK":
                this.url = String.format(urlMapping.get(source), id.replaceAll("\\.", "/"));
                break;
            case "OWASP Attacks":
                this.url = String.format(urlMapping.get(source), name.replaceAll("\\s", "_"));
                break;
            case "WASC":
                this.url = String.format(urlMapping.get(source), name.replaceAll("\\s", "-"));
                break;
            // fall-through switch case, first time use
            case "SEI CERT Perl Coding Standard":
            case "SEI CERT Oracle Coding Standard for Java":
            case "The CERT Oracle Secure Coding Standard for Java (2011)":
            case "CERT C Secure Coding":
                this.url = String.format(urlMapping.get(source), encodeString(id + ". " + name));
                break;
            case "PLOVER":
            case "7 Pernicious Kingdoms":
            case "OMG ASCMM":
            case "OMG ASCPEM":
            case "OMG ASCSM":
            case "OMG ASCRM":
            case "OWASP Top Ten 2004":
            case "OWASP Top Ten 2007":
                this.url = urlMapping.get(source);
                break;
        }
    }

    public static AeaaTaxonomyMapping fromJson(JSONObject json) {
        return new AeaaTaxonomyMapping(json.optString("source"), json.optString("id"), json.optString("name"));
    }

    public JSONObject toJson() {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("source", source);
        jsonObject.put("id", id);
        jsonObject.put("name", name);
        jsonObject.put("url", url);
        return jsonObject;
    }

    private static String encodeString(String uri) {
        try {
            return URLEncoder.encode(uri, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return uri;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AeaaTaxonomyMapping that = (AeaaTaxonomyMapping) o;
        return Objects.equals(source, that.source) && Objects.equals(id, that.id) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(source, id, name);
    }
}
