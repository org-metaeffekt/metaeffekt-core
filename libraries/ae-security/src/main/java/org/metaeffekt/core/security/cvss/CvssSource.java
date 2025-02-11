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

import org.json.JSONArray;
import org.json.JSONObject;
import org.metaeffekt.core.security.cvss.processor.LruLinkedHashMap;

import java.net.URL;
import java.util.*;

/**
 * <h2>Background</h2>
 * <p>
 * The new vulnerabilities and security advisories management system requires allowing any number of CVSS vectors to be
 * stored for vulnerabilities and security advisories. These vectors are aggregated by different enrichment steps, each
 * with different sources and potential conditions attached.
 * </p>
 * <p>
 * The reason behind this deferred effective CVSS vector selection process is simple: during the enrichment process, one
 * does not yet know what vectors will be found in further steps. It is therefore impossible to decide during the
 * process, what CVSS vectors should be used as effective vectors for a vulnerability while the process is still running.
 * Merging them all into a single vector, as done previously, is not only incorrect, but also dangerous, since simply
 * merging all vectors on top of each other does not allow for a conscious decision to be made by either automated rules
 * or a human being.
 * </p>
 * <p>
 * In the new system, only when a CVSS score needs to be calculated will the selection and merging actually take place
 * using a ruleset that allows for specifying an order of preference and merging methods for certain sources. An example
 * of this will be shown below, after the individual components have been explained.
 * </p>
 *
 * <hr>
 *
 * <h2>CVSS Sources</h2>
 * <p>A common format for storing sources of CVSS vectors is therefore needed. This format looks like this:</p>
 * <pre>
 * CvssVersion HostingEntity-IssuerRole-IssuingEntity
 * CvssVersion HostingEntity-IssuingEntity
 * CvssVersion HostingEntity
 *
 * // examples:
 * CVSS:3.1 Microsoft Corporation
 * CVSS:2.0 Assessment-all
 * CVSS:2.0 NVD-CNA-NVD
 * CVSS:3.1 NVD-CNA-Microsoft Corporation
 * CVSS:4.0 Assessment-lower
 * </pre>
 * <p>Where (CvssVersion, HostingEntity) are mandatory and (IssuerRole, IssuingEntity) are optional.</p>
 * <ul>
 *     <li>
 *         <strong>HostingEntity:</strong> This represents the platform, or more commonly the provider type of the CVSS
 *     vector.</li>
 *     <li><strong>IssuingEntity:</strong> Is the entity that provided the CVSS vector on the HostingEntity platform.
 *     In some cases, this is the same as the HostingEntity and can be left away, but if it differs, it must be listed.</li>
 *     <li><strong>IssuerRole:</strong> The role of the IssuingEntity on the HostingEntity platform. Must not be filled
 *     if no IssuingEntity is provided. Currently, the only role in use is the CNA role from the NVD.</li>
 * </ul>
 * <p>Combining sources: Merged sources (more than one source) are not meant to be written back to the vulnerability
 * inventory sheet, but are allowed in the security advisories. If multiple sources are combined, they are split using
 * <code> + </code>:</p>
 * <pre>
 * CvssVersion HostingEntity1-IssuerRole1-IssuingEntity1 + HostingEntity2-IssuerRole2-IssuingEntity2 + ...
 * </pre>
 */
public class CvssSource {

    private final CvssEntity hostingEntity;
    private final CvssEntity issuingEntity;
    private final CvssIssuingEntityRole issuingEntityRole;

    private final Class<CvssVector> vectorClass;

    public CvssSource(CvssEntity hostingEntity, CvssIssuingEntityRole issuingEntityRole, CvssEntity issuingEntity, Class<? extends CvssVector> vectorClass) {
        this.hostingEntity = hostingEntity;
        this.issuingEntityRole = issuingEntityRole;
        this.issuingEntity = issuingEntity;
        this.vectorClass = (Class<CvssVector>) vectorClass;
    }

    public CvssSource(CvssEntity hostingEntity, CvssEntity issuingEntity, Class<? extends CvssVector> vectorClass) {
        this(hostingEntity, null, issuingEntity, vectorClass);
    }

    public CvssSource(CvssEntity hostingEntity, Class<? extends CvssVector> vectorClass) {
        this(hostingEntity, null, null, vectorClass);
    }

    public CvssSource(CvssEntity hostingEntity, CvssIssuingEntityRole issuingEntityRole, CvssEntity issuingEntity, CvssVector vectorInstance) {
        this.hostingEntity = hostingEntity;
        this.issuingEntityRole = issuingEntityRole;
        this.issuingEntity = issuingEntity;
        this.vectorClass = (Class<CvssVector>) vectorInstance.getClass();
    }

    public CvssSource(CvssEntity hostingEntity, CvssEntity issuingEntity, CvssVector vectorInstance) {
        this(hostingEntity, null, issuingEntity, vectorInstance);
    }

    public CvssSource(CvssEntity hostingEntity, CvssVector vectorInstance) {
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

    public Class<CvssVector> getVectorClass() {
        return vectorClass;
    }

    public CvssVector parseVector(String vector) {
        if (vectorClass == null) {
            throw new IllegalStateException("No vector class specified for CVSS source: " + this);
        }

        try {
            return vectorClass.getConstructor(String.class).newInstance(vector);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse vector [" + vector + "] for CVSS source: " + this, e);
        }
    }

    public CvssSource deriveSource(Class<? extends CvssVector> vectorClass) {
        return new CvssSource(hostingEntity, issuingEntityRole, issuingEntity, vectorClass);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CvssSource that = (CvssSource) o;
        return Objects.equals(hostingEntity, that.hostingEntity) && Objects.equals(issuingEntity, that.issuingEntity)
                && Objects.equals(issuingEntityRole, that.issuingEntityRole) && Objects.equals(vectorClass, that.vectorClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(hostingEntity, issuingEntity, issuingEntityRole, vectorClass);
    }

    public static class CvssEntity implements EntityNameProvider {
        private final String name; // from partnerName
        private final String email; // from steps.filter(link.startsWith("mailto:")).map(link.substring("mailto:".length())).findFirst()
        private final URL url; // from Security Advisories or fallback steps.filter(link.startsWith("http")).findFirst()
        private final URL cveOrgDetailsLink; // from cveOrgDetailsLink

        private final String description; // from Scope
        private final String country; // from Country
        private final String role; // from Program Role
        private final List<String> organizationTypes; // from Organization Type

        private final CvssEntity topLevelRootPartner; // from Top-Level Root
        private final CvssEntity rootPartner; // from Root

        private final List<ReportStep> reportSteps; // from steps

        public CvssEntity(String name, String email, URL url, URL cveOrgDetailsLink, String description, String country, String role, List<String> organizationTypes, CvssEntity topLevelRootPartner, CvssEntity rootPartner, List<ReportStep> reportSteps) {
            this.name = name;
            this.email = email;
            this.url = url;
            this.cveOrgDetailsLink = cveOrgDetailsLink;
            this.description = description;
            this.country = country;
            this.role = role;
            this.organizationTypes = organizationTypes;
            this.topLevelRootPartner = topLevelRootPartner;
            this.rootPartner = rootPartner;
            this.reportSteps = reportSteps;
        }

        public CvssEntity(String name) {
            this(name, null, null, null, null, null, null, Collections.emptyList(), null, null, Collections.emptyList());
        }

        public String getEmail() {
            return email;
        }

        public URL getUrl() {
            return url;
        }

        public URL getCveOrgDetailsLink() {
            return cveOrgDetailsLink;
        }

        public String getDescription() {
            return description;
        }

        public String getCountry() {
            return country;
        }

        public String getRole() {
            return role;
        }

        public List<String> getOrganizationTypes() {
            return organizationTypes;
        }

        public CvssEntity getTopLevelRootPartner() {
            return topLevelRootPartner;
        }

        public CvssEntity getRootPartner() {
            return rootPartner;
        }

        public List<ReportStep> getReportSteps() {
            return reportSteps;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return "CvssEntity{" + name
                    + (email != null ? ", " + email : "")
                    + (url != null ? " (" + url + ")" : "") + "}";
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CvssEntity that = (CvssEntity) o;
            return Objects.equals(name, that.name) && Objects.equals(email, that.email) && Objects.equals(url, that.url)
                    && Objects.equals(cveOrgDetailsLink, that.cveOrgDetailsLink) && Objects.equals(description, that.description)
                    && Objects.equals(country, that.country) && Objects.equals(role, that.role) && Objects.equals(organizationTypes, that.organizationTypes)
                    && Objects.equals(topLevelRootPartner, that.topLevelRootPartner) && Objects.equals(rootPartner, that.rootPartner)
                    && Objects.equals(reportSteps, that.reportSteps);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, email, url, cveOrgDetailsLink, description, country, role, organizationTypes,
                    topLevelRootPartner, rootPartner, reportSteps);
        }

        public CvssSource deriveSource(Class<? extends CvssVector> vectorClass) {
            return new CvssSource(this, vectorClass);
        }
    }

    public static class ReportStep {
        private final int stepIndex;
        private final String title;
        private final String link;

        public ReportStep(int stepIndex, String title, String link) {
            this.stepIndex = stepIndex;
            this.title = title;
            this.link = link;
        }

        public int getStepIndex() {
            return stepIndex;
        }

        public String getTitle() {
            return title;
        }

        public String getLink() {
            return link;
        }

        public static ReportStep fromJson(JSONObject json) {
            return new ReportStep(
                    json.getInt("stepIndex"),
                    json.getString("title"),
                    json.getString("link")
            );
        }

        public static List<ReportStep> fromJson(JSONArray json) {
            if (json == null) return Collections.emptyList();
            final List<ReportStep> result = new ArrayList<>();
            for (int i = 0; i < json.length(); i++) {
                result.add(fromJson(json.getJSONObject(i)));
            }
            return result;
        }

        @Override
        public String toString() {
            return "StepInfo{" +
                    "stepIndex=" + stepIndex +
                    ", title='" + title + '\'' +
                    ", link='" + link + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            ReportStep that = (ReportStep) o;
            return stepIndex == that.stepIndex && Objects.equals(title, that.title) && Objects.equals(link, that.link);
        }

        @Override
        public int hashCode() {
            return Objects.hash(stepIndex, title, link);
        }
    }

    public static class CvssIssuingEntityRole implements EntityNameProvider {
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

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            CvssIssuingEntityRole that = (CvssIssuingEntityRole) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    public interface EntityNameProvider {
        String getName();


        default String getEscapedName() {
            return getName()
                    .replace("_", "\\_")
                    .replace("-", "_");
        }

        static String unescapeEntityName(String name) {
            final StringBuilder result = new StringBuilder();

            boolean escapeNextChar = false;
            for (int i = 0; i < name.length(); i++) {
                final char c = name.charAt(i);

                if (c == '\\') {
                    escapeNextChar = !escapeNextChar;
                } else if (!escapeNextChar) {
                    if (c == '_') {
                        result.append('-');
                    } else {
                        result.append(c);
                    }
                } else {
                    result.append(c);
                    escapeNextChar = false;
                }
            }

            return result.toString();
        }
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

        return vectorVersion + " " + hostingEntity.getEscapedName() + (issuingEntityRole != null ? "-" + issuingEntityRole.getEscapedName() : "") + (issuingEntity != null ? "-" + issuingEntity.getEscapedName() : "");
    }

    public static String toCombinedColumnHeaderString(Collection<CvssSource> sources) {
        return toCombinedColumnHeaderString(sources, true);
    }

    public static String toCombinedColumnHeaderString(Collection<CvssSource> sources, boolean includeVersion) {
        if (sources.isEmpty()) return "";

        final String vectorVersion;
        if (includeVersion) {
            final CvssSource firstSource = sources.iterator().next();
            try {
                vectorVersion = CvssVector.getVersionName(firstSource.getVectorClass()) + " ";
            } catch (Exception e) {
                throw new IllegalArgumentException("Unsupported CVSS version [" + firstSource.getVectorClass() + "] in CVSS source: " + firstSource);
            }
        } else {
            vectorVersion = "";
        }

        final StringJoiner joiner = new StringJoiner(" + ", vectorVersion, "");
        for (CvssSource source : sources) {
            joiner.add(source.getHostingEntity().getEscapedName() + (source.getIssuingEntityRole() != null ? "-" + source.getIssuingEntityRole().getEscapedName() : "") + (source.getIssuingEntity() != null ? "-" + source.getIssuingEntity().getEscapedName() : ""));
        }
        return joiner.toString();
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

    public static CvssSource fromColumnHeaderString(String header) {
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
            hostingEntity = KnownCvssEntities.findByNameOrMailOrCreateNew(EntityNameProvider.unescapeEntityName(parts[0]));

            if (parts.length == 2) {
                issuingEntityRole = null;
                issuingEntity = KnownCvssEntities.findByNameOrMailOrCreateNew(EntityNameProvider.unescapeEntityName(parts[1]));
            } else if (parts.length == 3) {
                issuingEntityRole = CvssIssuingEntityRole.findOrCreateNew(EntityNameProvider.unescapeEntityName(parts[1]));
                issuingEntity = KnownCvssEntities.findByNameOrMailOrCreateNew(EntityNameProvider.unescapeEntityName(parts[2]));
            } else if (parts.length > 3) {
                throw new IllegalArgumentException("Invalid CVSS source, requires at most hosting entity, issuing entity role and issuing entity: " + header);
            } else { // parts.length == 1
                issuingEntityRole = null;
                issuingEntity = null;
            }
        } else {
            throw new IllegalArgumentException("Invalid CVSS source, requires at least hosting entity: " + header);
        }

        return new CvssSource(hostingEntity, issuingEntityRole, issuingEntity, cvssVectorClass);
    }

    public static Map<String, CvssSource> fromMultipleColumnHeaderStrings(Set<String> headers) {
        final Map<String, CvssSource> result = new LinkedHashMap<>();

        for (String header : headers) {
            if (!header.startsWith("CVSS")) continue;

            if (header.equalsIgnoreCase("CVSS Initial Overall")
                    || header.equalsIgnoreCase("CVSS Context Overall")
                    || header.equalsIgnoreCase("CVSS Initial Base")
                    || header.equalsIgnoreCase("CVSS Initial Exploitability")
                    || header.equalsIgnoreCase("CVSS Initial Impact")) {
                continue;
            }

            try {
                result.put(header, fromColumnHeaderString(header));
            } catch (Exception ignored) {
            }
        }

        return result;
    }

    public static Map<String, CvssSource> fromMultipleColumnHeaderStringsThrowing(Set<String> headers) {
        final Map<String, CvssSource> result = new LinkedHashMap<>();

        for (String header : headers) {
            if (!header.startsWith("CVSS")) continue;

            if (header.equalsIgnoreCase("CVSS Initial Overall")
                    || header.equalsIgnoreCase("CVSS Context Overall")
                    || header.equalsIgnoreCase("CVSS Initial Base")
                    || header.equalsIgnoreCase("CVSS Initial Exploitability")
                    || header.equalsIgnoreCase("CVSS Initial Impact")) {
                continue;
            }

            result.put(header, fromColumnHeaderString(header));
        }

        return result;
    }
}
