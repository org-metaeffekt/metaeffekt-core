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
package org.metaeffekt.core.security.cvss;

import org.json.JSONArray;
import org.json.JSONObject;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class CvssSource<T extends CvssVector> {

    private final CvssEntity hostingEntity;
    private final CvssEntity issuingEntity;
    private final CvssIssuingEntityRole issuingEntityRole;

    private final Class<T> vectorClass;

    public CvssSource(CvssEntity hostingEntity, CvssIssuingEntityRole issuingEntityRole, CvssEntity issuingEntity, Class<T> vectorClass) {
        this.hostingEntity = hostingEntity;
        this.issuingEntityRole = issuingEntityRole;
        this.issuingEntity = issuingEntity;
        this.vectorClass = vectorClass;
    }

    public CvssSource(CvssEntity hostingEntity, CvssEntity issuingEntity, Class<T> vectorClass) {
        this(hostingEntity, null, issuingEntity, vectorClass);
    }

    public CvssSource(CvssEntity hostingEntity, Class<T> vectorClass) {
        this(hostingEntity, null, null, vectorClass);
    }

    public CvssSource(CvssEntity hostingEntity, CvssIssuingEntityRole issuingEntityRole, CvssEntity issuingEntity, T vectorInstance) {
        this.hostingEntity = hostingEntity;
        this.issuingEntityRole = issuingEntityRole;
        this.issuingEntity = issuingEntity;
        this.vectorClass = (Class<T>) vectorInstance.getClass();
    }

    public CvssSource(CvssEntity hostingEntity, CvssEntity issuingEntity, T vectorInstance) {
        this(hostingEntity, null, issuingEntity, vectorInstance);
    }

    public CvssSource(CvssEntity hostingEntity, T vectorInstance) {
        this(hostingEntity, null, null, vectorInstance);
    }

    public CvssEntity getHostingEntity() {
        return hostingEntity;
    }

    public CvssIssuingEntityRole getIssuingEntityRole() {
        return issuingEntityRole;
    }

    public CvssEntity getIssuingEntity() {
        return issuingEntity;
    }

    public Class<T> getVectorClass() {
        return vectorClass;
    }

    public <NT extends CvssVector> CvssSource<NT> withCvssVersion(Class<NT> vectorClass) {
        return new CvssSource<>(hostingEntity, issuingEntityRole, issuingEntity, vectorClass);
    }

    public T parseVector(String vector) {
        if (vectorClass == null) {
            throw new IllegalStateException("No vector class specified for CVSS source: " + this);
        }

        try {
            return vectorClass.getConstructor(String.class).newInstance(vector);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse vector [" + vector + "] for CVSS source: " + this, e);
        }
    }

    public static class CvssEntity implements NameProvider {
        private final String name;
        private final String sourceName;
        private final URL url;

        public CvssEntity(String name) {
            this(name, null, null);
        }

        public CvssEntity(String name, String sourceName) {
            this(name, sourceName, null);
        }

        public CvssEntity(String name, String sourceName, String url) {
            this.name = name;
            this.sourceName = sourceName;

            if (url == null) {
                this.url = null;
            } else {
                try {
                    this.url = new URL(url);
                } catch (MalformedURLException e) {
                    throw new IllegalArgumentException("Invalid URL for cvss entity " + name + ": " + url, e);
                }
            }
        }

        public String getName() {
            return name;
        }

        public String getSourceName() {
            return sourceName;
        }

        public URL getUrl() {
            return url;
        }

        public final static CvssEntity OTHER = new CvssEntity("OTHER");

        public final static CvssEntity NVD = new CvssEntity("NVD", "nvd@nist.gov", "https://nvd.nist.gov");
        public final static CvssEntity GHSA = new CvssEntity("GitHub, Inc.", "security-advisories@github.com", "https://github.com/advisories");

        public final static CvssEntity ASSESSMENT = new CvssEntity("Assessment");
        public final static CvssEntity ASSESSMENT_LOWER = new CvssEntity("lower");
        public final static CvssEntity ASSESSMENT_HIGHER = new CvssEntity("higher");
        public final static CvssEntity ASSESSMENT_ALL = new CvssEntity("all");

        public final static List<CvssEntity> ALL = Arrays.asList(
                NVD, GHSA,
                ASSESSMENT, ASSESSMENT_LOWER, ASSESSMENT_HIGHER, ASSESSMENT_ALL
        );

        public static CvssEntity findOrCreateNewFromName(String name) {
            for (CvssEntity entity : ALL) {
                if (entity.getName().equalsIgnoreCase(name)) {
                    return entity;
                }
            }
            return new CvssEntity(name);
        }

        public static CvssEntity findOrCreateNewFromSourceNameOrElse(String sourceName, CvssEntity defaultEntity) {
            if (sourceName != null) {
                for (CvssEntity entity : ALL) {
                    if (entity.getSourceName() != null && entity.getSourceName().equalsIgnoreCase(sourceName)) {
                        return entity;
                    }
                }
                return new CvssEntity(sourceName);
            } else {
                return defaultEntity;
            }
        }
    }

    public static class CvssIssuingEntityRole implements NameProvider {
        private final String name;

        public CvssIssuingEntityRole(String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public final static CvssIssuingEntityRole CNA = new CvssIssuingEntityRole("CNA");

        public final static List<CvssIssuingEntityRole> ALL = Arrays.asList(
                CNA
        );

        public static CvssIssuingEntityRole findOrCreateNew(String name) {
            for (CvssIssuingEntityRole role : ALL) {
                if (role.getName().equalsIgnoreCase(name)) {
                    return role;
                }
            }
            return new CvssIssuingEntityRole(name);
        }
    }

    private interface NameProvider {
        String getName();
    }

    private static String escapeName(String name) {
        return name
                .replace("_", "\\_")
                .replace("-", "_");
    }

    private static String unescapeName(String name) {
        // make sure to only translate those "_" back into "-" that do not have a "\\" in front of them
        return name
                .replaceAll("(?<!\\\\)_", "-")
                .replace("\\_", "_");
    }

    public String toColumnHeaderString() {
        final String vectorVersion;
        try {
            vectorVersion = CvssVector.getVersionName(vectorClass);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported CVSS version [" + vectorClass + "] in CVSS source: " + this);
        }
        if (hostingEntity == null) {
            throw new IllegalStateException("No hosting entity specified for CVSS source: " + this);
        }

        return vectorVersion + " " + escapeName(hostingEntity.getName()) + (issuingEntityRole != null ? "-" + escapeName(issuingEntityRole.getName()) : "") + (issuingEntity != null ? "-" + escapeName(issuingEntity.getName()) : "");
    }

    @Override
    public String toString() {
        String vectorVersion;
        try {
            vectorVersion = CvssVector.getVersionName(vectorClass);
        } catch (Exception e) {
            vectorVersion = "CVSS:unknown";
        }
        return vectorVersion + " " + (hostingEntity != null ? hostingEntity.getName() : "unknown") + (issuingEntityRole != null ? "-" + issuingEntityRole.getName() : "") + (issuingEntity != null ? "-" + issuingEntity.getName() : "");
    }

    public static CvssSource<?> fromColumnHeaderString(String header) {
        final String[] versionRest = header.split(" ", 2);

        if (versionRest.length != 2) {
            throw new IllegalArgumentException("Invalid CVSS source, requires version and rest: " + header);
        }

        final String version = versionRest[0];
        final String rest = versionRest[1];

        final Class<? extends CvssVector> cvssVectorClass;
        try {
            cvssVectorClass = CvssVector.classFromVersionName(version);
        } catch (Exception e) {
            throw new IllegalArgumentException("Unsupported CVSS version [" + version + "] in CVSS source: " + header);
        }

        final String[] parts = rest.split("-");

        final CvssEntity hostingEntity;
        final CvssEntity issuingEntity;
        final CvssIssuingEntityRole issuingEntityRole;

        if (parts.length > 0) {
            hostingEntity = CvssEntity.findOrCreateNewFromName(unescapeName(parts[0]));

            if (parts.length == 2) {
                issuingEntityRole = null;
                issuingEntity = CvssEntity.findOrCreateNewFromName(unescapeName(parts[1]));
            } else if (parts.length == 3) {
                issuingEntityRole = CvssIssuingEntityRole.findOrCreateNew(unescapeName(parts[1]));
                issuingEntity = CvssEntity.findOrCreateNewFromName(unescapeName(parts[2]));
            } else if (parts.length > 3) {
                throw new IllegalArgumentException("Invalid CVSS source, requires at most hosting entity, issuing entity role and issuing entity: " + header);
            } else { // parts.length == 1
                issuingEntityRole = null;
                issuingEntity = null;
            }
        } else {
            throw new IllegalArgumentException("Invalid CVSS source, requires at least hosting entity: " + header);
        }

        return new CvssSource<>(hostingEntity, issuingEntityRole, issuingEntity, cvssVectorClass);
    }

    public static Map<String, CvssSource<? extends CvssVector>> fromMultipleColumnHeaderStrings(Set<String> headers) {
        final Map<String, CvssSource<?>> result = new LinkedHashMap<>();

        for (String header : headers) {
            if (!header.startsWith("CVSS")) continue;
            try {
                result.put(header, fromColumnHeaderString(header));
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    public static Map<String, CvssSource<? extends CvssVector>> fromMultipleColumnHeaderStringsThrowing(Set<String> headers) {
        final Map<String, CvssSource<?>> result = new LinkedHashMap<>();

        for (String header : headers) {
            if (!header.startsWith("CVSS")) continue;
            result.put(header, fromColumnHeaderString(header));
        }

        return result;
    }

    public static JSONArray toJson(Map<CvssSource<?>, CvssVector> vectorsMap) {
        final JSONArray result = new JSONArray();

        for (Map.Entry<CvssSource<?>, CvssVector> entry : vectorsMap.entrySet()) {
            final CvssSource<?> source = entry.getKey();
            final CvssVector vector = entry.getValue();

            final JSONObject jsonEntry = new JSONObject();
            jsonEntry.put("source", source.toColumnHeaderString());
            jsonEntry.put("vector", vector.toString());
            result.put(jsonEntry);
        }

        return result;
    }

    public static Map<CvssSource<?>, CvssVector> fromJson(JSONArray jsonArray) {
        final Map<CvssSource<?>, CvssVector> result = new LinkedHashMap<>();

        for (int i = 0; i < jsonArray.length(); i++) {
            final JSONObject jsonEntry = jsonArray.getJSONObject(i);
            final CvssSource<?> source = fromColumnHeaderString(jsonEntry.getString("source"));
            final CvssVector vector = source.parseVector(jsonEntry.getString("vector"));
            result.put(source, vector);
        }

        return result;
    }
}
