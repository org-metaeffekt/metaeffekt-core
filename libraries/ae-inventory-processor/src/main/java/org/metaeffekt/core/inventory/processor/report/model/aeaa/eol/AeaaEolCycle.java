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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.eol;

import lombok.Getter;
import lombok.Setter;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.AeaaTimeUtils;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.eol.state.*;
import org.apache.commons.lang3.StringUtils;

import java.util.Comparator;
import java.util.Date;
import java.util.Objects;

import static org.apache.commons.lang3.ObjectUtils.firstNonNull;

/**
 * Represents an end-of-life (EOL) cycle for a product.
 * <p>
 * This class stores information about a product's EOL cycle, including its release date, EOL date, latest version,
 * LTS (Long-Term Support) status, and other related information.
 */
@Getter
public class AeaaEolCycle implements Comparable<AeaaEolCycle> {

    @Setter
    private String product;
    private final String cycle;
    private final String releaseDate;
    private final String latest;
    private final String latestReleaseDate;
    private final String link;
    /**
     * <p><code>string&lt;date&gt;</code> or <code>boolean</code></p>
     * <ul>
     * <li>if boolean: <code>true</code> means it has reached the end of life state, without a specific date and <code>false</code> means that the end
     * of life state has not been set yet</li>
     * <li>if date: the release reaches the end of life state on this date, which can be a date in the past or in the future</li>
     * </ul>
     */
    private final String eol;
    /**
     * <p><code>string&lt;date&gt;</code> or <code>boolean</code></p>
     * <p>Similar to <code>eol</code>, but for when/if long term support is available for the version.</p>
     * <ul>
     * <li>if boolean: <code>true</code> means it has long term support and <code>false</code> means that lts is most likely never going to be
     * available for this version</li>
     * <li>if date: the cycle enters long term support on this date, which can be a date in the past or in the future</li>
     * </ul>
     */
    private final String lts;
    /**
     * <p><code>string&lt;date&gt;</code> or <code>boolean</code></p>
     * <p>Similar to <code>eol</code>, but for when/if the version is discontinued.</p>
     * <ul>
     * <li>if boolean: <code>true</code> means it has been discontinued and <code>false</code> means that the version is not discontinued</li>
     * <li>if date: the cycle is discontinued on this date, which can be a date in the past or in the future</li>
     * </ul>
     */
    private final String discontinued;
    /**
     * <p><code>string&lt;date&gt;</code> or <code>boolean</code> or <code>null</code></p>
     * <p>Similar to <code>eol</code>, but for when/if the version still provides support by the vendor.</p>
     * <ul>
     * <li>if boolean: <code>true</code> means it is still supported and <code>false</code> means that the version is not supported anymore</li>
     * <li>if date: the cycle is still supported up till this date, which can be a date in the past or in the future</li>
     * <li>if <code>null</code>: inverted <code>eol</code> field, meaning that if the product is not end of life, it is still supported (see rabbitmq
     * below for example)</li>
     * </ul>
     * <pre><code class="lang-json">
     * {
     *   <span class="hljs-attr">"product"</span>: <span class="hljs-string">"rabbitmq"</span>,
     *   <span class="hljs-attr">"eol"</span>: <span class="hljs-string">"2023-12-31"</span>,
     *   <span class="hljs-attr">"extendedSupport"</span>: <span class="hljs-string">"2024-07-31"</span>,
     * }
     * </code></pre>
     * <p>Note that the extended support may differ from the support date, and that there is no support date specified. This means
     * that the product ends support on the <code>2023-12-31</code> and that extended support is available until <code>2024-07-31</code>.</p>
     */
    private final String support;
    /**
     * <p><code>string&lt;date&gt;</code> or <code>boolean</code> or <code>null</code></p>
     * <p>Similar to <code>support</code>, but for when/if the version still provides extended support by the vendor.</p>
     * <ul>
     * <li>if boolean: <code>true</code> means it is still supported and <code>false</code> means that the version is not supported anymore</li>
     * <li>if date: the cycle is still supported up till this date, which can be a date in the past or in the future</li>
     * <li>if <code>null</code>: see below</li>
     * </ul>
     * <p><a href="https://endoflife.date/almalinux">https://endoflife.date/almalinux</a>:</p>
     * <pre><code class="lang-json">{
     *   <span class="hljs-attr">"product"</span>: <span class="hljs-string">"almalinux"</span>,
     *   <span class="hljs-attr">"cycle"</span>: <span class="hljs-string">"9"</span>,
     *   <span class="hljs-attr">"eol"</span>: <span class="hljs-string">"2032-05-31"</span>,
     *   <span class="hljs-attr">"support"</span>: <span class="hljs-string">"2027-05-31"</span>
     * }
     * </code></pre>
     * <p>Meaning that security support ends on 31 May 2032 (<code>eol</code>), the same date as the end of life for AlmaLinux 9. The end of
     * the regular support period is 31 May 2027 (<code>support</code>).</p>
     * <p>There is another case, however:</p>
     * <p><a href="https://endoflife.date/samsung-mobile">https://endoflife.date/samsung-mobile</a></p>
     * <pre><code class="lang-json">{
     *   <span class="hljs-attr">"product"</span>: <span class="hljs-string">"samsung-mobile"</span>,
     *   <span class="hljs-attr">"cycle"</span>: <span class="hljs-string">"Galaxy A8s"</span>,
     *   <span class="hljs-attr">"eol"</span>: <span class="hljs-string">"false"</span>,
     *   <span class="hljs-attr">"support"</span>: <span class="hljs-string">"false"</span>
     * }
     * </code></pre>
     * <p>Which the EOL Date page interprets as <code>Yes</code>. This is because the <code>eol</code> and <code>support</code> fields are <code>false</code>, which means
     * that the product is not supported by regular support, but the EOL has not been reached yet, so the extended support is
     * still available.</p>
     * <p>On the same product, there is another example where the <code>eol</code> field is set to a date and the support is <code>false</code>. In this
     * case, too, the extended support uses the <code>eol</code> date:</p>
     * <pre><code class="lang-json">{
     *   <span class="hljs-attr">"product"</span>: <span class="hljs-string">"samsung-mobile"</span>,
     *   <span class="hljs-attr">"cycle"</span>: <span class="hljs-string">"Galaxy A2 Core"</span>,
     *   <span class="hljs-attr">"eol"</span>: <span class="hljs-string">"2021-10-01"</span>,
     *   <span class="hljs-attr">"support"</span>: <span class="hljs-string">"false"</span>
     * }
     * </code></pre>
     */
    private final String extendedSupport;
    /**
     * <p><code>string&lt;date&gt;</code></p>
     * <p>The date on which technical guidance ends for the product. This is not the same as the end of life date, but the date
     * on which the vendor stops providing technical guidance for the product.</p>
     */
    private final String technicalGuidance;
    /**
     * <p>As of writing this document, the <code>supportedPhpVersions</code> field is only used by the <code>magento</code> and <code>laravel</code> products. It
     * is a csv list of versions, with each version entry either being a single version or version range:</p>
     * <p><code>7.3 - 8.1, 7.3, 7.4</code></p>
     * <p>This field is currently unused, but logic for checking if a version is included is present.</p>
     */
    private final String supportedPhpVersions;
    /**
     * <p>This is the version of the Java Development Kit (JDK) for the given product cycle that has to be used to run the
     * product.</p>
     * <p>Example: <a href="https://endoflife.date/azul-zulu">https://endoflife.date/azul-zulu</a></p>
     */
    private final String latestJdkVersion;
    /**
     * <p>Currently, only the <a href="https://endoflife.date/amazon-neptune">amazon-neptune</a> product uses this field. In this one case,
     * it is a string containing the version that the product will automatically be upgraded to during a maintenance window.</p>
     */
    private final String upgradeVersion;
    /**
     * <p>Contains a formatted name for the release. May contain HTML content and the same expression evaluator as the <code>link</code> key.
     * Examples:</p>
     * <ul>
     * <li><code>AMI 2016.03</code></li>
     * <li><code>Core __RELEASE_CYCLE__</code></li>
     * <li><code>OS X __RELEASE_CYCLE__ (__CODENAME__)</code></li>
     * <li><code>9 (&lt;abbr title=&#39;Short Term Support&#39;&gt;STS&lt;\/abbr&gt;)</code></li>
     * </ul>
     */
    private final String releaseLabel;
    /**
     * @see #supportedPhpVersions
     */
    private final String supportedKubernetesVersions;
    /**
     * @see #supportedPhpVersions
     */
    private final String supportedJavaVersions;
    private final String minRubyVersion;
    /**
     * <p>An alternate (project internal) name for the version. Example is android with their dessert names:</p>
     * <pre><code class="lang-json">
     * {
     *   <span class="hljs-attr">"product"</span>: <span class="hljs-string">"android"</span>,
     *   <span class="hljs-attr">"codename"</span>: <span class="hljs-string">"Queen Cake"</span>,
     *   <span class="hljs-attr">"cycle"</span>: <span class="hljs-string">"10"</span>
     * }
     * </code></pre>
     */
    private final String codename;
    private final String minJavaVersion;

    public AeaaEolCycle(String product, String cycle, String releaseDate, String eol, String latest, String link, String lts, String support, String discontinued, String latestReleaseDate, String supportedPhpVersions, String latestJdkVersion, String extendedSupport, String upgradeVersion, String releaseLabel, String supportedKubernetesVersions, String minRubyVersion, String codename, String technicalGuidance, String supportedJavaVersions, String minJavaVersion) {
        this.product = product;
        this.cycle = cycle;
        this.releaseDate = releaseDate;
        this.eol = eol;
        this.latest = latest;
        this.lts = lts;
        this.support = support;
        this.discontinued = discontinued;
        this.latestReleaseDate = latestReleaseDate;
        this.supportedPhpVersions = supportedPhpVersions;
        this.latestJdkVersion = latestJdkVersion;
        this.extendedSupport = extendedSupport;
        this.upgradeVersion = upgradeVersion;
        this.supportedKubernetesVersions = supportedKubernetesVersions;
        this.minRubyVersion = minRubyVersion;
        this.codename = codename;
        this.technicalGuidance = technicalGuidance;
        this.supportedJavaVersions = supportedJavaVersions;
        this.minJavaVersion = minJavaVersion;

        this.link = AeaaEolStringFormatter.format(link, this);
        this.releaseLabel = AeaaEolStringFormatter.format(releaseLabel, this);
    }

    public AeaaEolCycle(String product, String cycle) {
        this(product, cycle,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, null);
    }

    public AeaaLtsState getLtsState() {
        return new AeaaStateMapping<>(AeaaLtsState.NOT_LTS, AeaaLtsState.LTS,
                AeaaLtsState.LTS_DATE_REACHED, AeaaLtsState.UPCOMING_LTS_DATE,
                false, "lts")
                .getState(lts, 0);
    }

    public AeaaDiscontinuedState getDiscontinuedState() {
        return new AeaaStateMapping<>(AeaaDiscontinuedState.NOT_DISCONTINUED, AeaaDiscontinuedState.DISCONTINUED,
                AeaaDiscontinuedState.DISCONTINUED_DATE_REACHED, AeaaDiscontinuedState.UPCOMING_DISCONTINUED_DATE,
                false, "discontinued")
                .getState(discontinued, 0);
    }

    public AeaaEolState getEolState() {
        return getEolState(0);
    }

    public AeaaEolState getEolState(long millisUntilEolWarning) {
        return new AeaaStateMapping<>(AeaaEolState.NOT_EOL, AeaaEolState.EOL,
                AeaaEolState.EOL_DATE_REACHED, AeaaEolState.DISTANT_EOL_DATE, AeaaEolState.UPCOMING_EOL_DATE,
                false, "eol")
                .getState(eol, millisUntilEolWarning);
    }

    public AeaaSupportState getSupportState() {
        return getSupportState(0);
    }

    public AeaaSupportState getSupportState(long millisUntilSupportEndWarning) {
        if (StringUtils.isEmpty(support)) {
            return AeaaSupportState.fromEolState(getEolState(millisUntilSupportEndWarning));
        }
        return new AeaaStateMapping<>(AeaaSupportState.NO_SUPPORT, AeaaSupportState.SUPPORT,
                AeaaSupportState.SUPPORT_END_DATE_REACHED, AeaaSupportState.DISTANT_SUPPORT_END_DATE, AeaaSupportState.UPCOMING_SUPPORT_END_DATE,
                false, "support")
                .getState(support, millisUntilSupportEndWarning);
    }

    public AeaaSupportState getExtendedSupportState() {
        return getExtendedSupportState(0);
    }

    public AeaaSupportState getExtendedSupportState(long millisUntilExtendedSupportEndWarning) {
        // must also check for false, see "angular" product where there are three columns
        if (StringUtils.isEmpty(extendedSupport) || extendedSupport.equals("false")) {
            final AeaaEolState eolState = getEolState(millisUntilExtendedSupportEndWarning);

            if (eolState == AeaaEolState.UPCOMING_EOL_DATE || eolState == AeaaEolState.EOL_DATE_REACHED || eolState == AeaaEolState.DISTANT_EOL_DATE) {
                if ("false".equals(support)) {
                    return AeaaSupportState.fromEolState(eolState);
                }

                final Date eolDate = AeaaTimeUtils.tryParse(eol);
                final Date supportDate = AeaaTimeUtils.tryParse(support);

                if (eolDate != null && supportDate != null) {
                    if (eolDate.after(supportDate)) {
                        return AeaaSupportState.fromEolState(eolState);
                    } else {
                        return AeaaSupportState.NO_SUPPORT;
                    }
                }
            } else if (eolState == AeaaEolState.NOT_EOL) {
                return AeaaSupportState.SUPPORT;
            }
        }

        return new AeaaStateMapping<>(AeaaSupportState.NO_SUPPORT, AeaaSupportState.SUPPORT,
                AeaaSupportState.SUPPORT_END_DATE_REACHED, AeaaSupportState.DISTANT_SUPPORT_END_DATE, AeaaSupportState.UPCOMING_SUPPORT_END_DATE,
                false, "support")
                .getState(extendedSupport, millisUntilExtendedSupportEndWarning);
    }

    public AeaaTechnicalGuidanceState getTechnicalGuidanceState() {
        return new AeaaStateMapping<>(AeaaTechnicalGuidanceState.NO_TECHNICAL_GUIDANCE, AeaaTechnicalGuidanceState.TECHNICAL_GUIDANCE,
                AeaaTechnicalGuidanceState.END_OF_TECHNICAL_GUIDANCE_DATE_REACHED, AeaaTechnicalGuidanceState.UPCOMING_END_OF_TECHNICAL_GUIDANCE_DATE,
                false, "technicalGuidance")
                .getState(technicalGuidance, 0);
    }

    public String getSupport() {
        if (StringUtils.isEmpty(support)) {
            final AeaaEolState eolState = getEolState();
            switch (eolState) {
                case EOL:
                    return "false";
                case NOT_EOL:
                    return "true";
                case EOL_DATE_REACHED:
                case UPCOMING_EOL_DATE:
                case DISTANT_EOL_DATE:
                default:
                    return eol;
            }
        } else {
            return support;
        }
    }

    public String getExtendedSupport() {
        if (StringUtils.isEmpty(extendedSupport)) {
            if (Objects.equals(support, eol)) {
                return extendedSupport;
            }

            final AeaaEolState eolState = getEolState();

            if (eolState == AeaaEolState.UPCOMING_EOL_DATE || eolState == AeaaEolState.EOL_DATE_REACHED || eolState == AeaaEolState.DISTANT_EOL_DATE) {
                if ("false".equals(support)) {
                    return eol;
                }

                final Date eolDate = AeaaTimeUtils.tryParse(eol);
                final Date supportDate = AeaaTimeUtils.tryParse(support);

                if (eolDate != null && supportDate != null) {
                    if (eolDate.after(supportDate)) {
                        return eol;
                    } else {
                        return "false";
                    }
                }
            }
        }

        return extendedSupport;
    }

    public boolean isExtendedSupportGenerallyAvailable() {
        return !StringUtils.isEmpty(getExtendedSupport());
    }

    public long getTimeUntilSupportEnd() {
        return getTimeUntil(getSupport(), true);
    }

    /**
     * Formats the time until a specific date or the time ago from a specific date.
     *
     * @param timeUntil the time until a specific date in milliseconds
     * @return a formatted string representing the time until or ago from the specific date
     * @see AeaaTimeUtils#formatTimeUntilOrAgo(long, String, String, String, String, String, String)
     */
    public static String formatTimeUntilOrAgo(long timeUntil) {
        return AeaaTimeUtils.formatTimeUntilOrAgo(timeUntil, "", "in", "ago", "", " and ", "no date provided");
    }

    public long getTimeUntilExtendedSupportEnd() {
        return getTimeUntil(getExtendedSupport(), true);
    }

    public long getTimeUntilTechnicalGuidanceEnd() {
        return getTimeUntil(technicalGuidance, true);
    }

    public long getTimeUntilEol() {
        return getTimeUntil(eol, false);
    }

    public long getTimeUntilDiscontinued() {
        return getTimeUntil(discontinued, true);
    }

    public long getTimeUntilLtsStart() {
        return getTimeUntil(lts, false);
    }

    /**
     * Either string&lt;date&gt; or string&lt;boolean&gt;.<br>
     * <ul>
     *     <li><code>"true"</code> - Long.MAX_VALUE if trueIsMax is true, 0 otherwise</li>
     *     <li><code>"false"</code> - 0 if trueIsMax is true, Long.MAX_VALUE otherwise</li>
     *     <li><code>"YYYY-MM-DD"</code> - milliseconds until that date</li>
     * </ul>
     *
     * @param dateString string&lt;date&gt; or string&lt;boolean&gt;
     * @return milliseconds until that date, see above
     */
    private long getTimeUntil(String dateString, boolean trueIsMax) {
        if (StringUtils.isEmpty(dateString)) {
            return Long.MAX_VALUE;
        }

        if ("true".equals(dateString)) {
            return trueIsMax ? Long.MAX_VALUE : 0;
        } else if ("false".equals(dateString)) {
            return trueIsMax ? 0 : Long.MAX_VALUE;
        }

        final Date date = AeaaTimeUtils.tryParse(dateString);
        if (date == null) {
            return 0;
        }

        return date.getTime() - AeaaStateMapping.getNOW().getTime();
    }

    public JSONObject toJson() {
        JSONObject json = new JSONObject();
        putIfNotNull(json, "product", product);
        putIfNotNull(json, "cycle", cycle);
        putIfNotNull(json, "releaseDate", releaseDate);
        putIfNotNull(json, "eol", eol);
        putIfNotNull(json, "latest", latest);
        putIfNotNull(json, "link", link);
        putIfNotNull(json, "lts", lts);
        putIfNotNull(json, "support", support);
        putIfNotNull(json, "discontinued", discontinued);
        putIfNotNull(json, "latestReleaseDate", latestReleaseDate);
        putIfNotNull(json, "supportedPhpVersions", supportedPhpVersions);
        putIfNotNull(json, "latestJdkVersion", latestJdkVersion);
        putIfNotNull(json, "extendedSupport", extendedSupport);
        putIfNotNull(json, "upgradeVersion", upgradeVersion);
        putIfNotNull(json, "releaseLabel", releaseLabel);
        putIfNotNull(json, "supportedKubernetesVersions", supportedKubernetesVersions);
        putIfNotNull(json, "minRubyVersion", minRubyVersion);
        putIfNotNull(json, "supportedPHPVersions", supportedPhpVersions);
        putIfNotNull(json, "codename", codename);
        putIfNotNull(json, "technicalGuidance", technicalGuidance);
        putIfNotNull(json, "supportedJavaVersions", supportedJavaVersions);
        putIfNotNull(json, "minJavaVersion", minJavaVersion);
        return json;
    }

    public static AeaaEolCycle fromJson(JSONObject json) {
        return new AeaaEolCycle(
                parseJson(json, "product"),
                parseJson(json, "cycle"),
                parseJson(json, "releaseDate"),
                parseJson(json, "eol"),
                parseJson(json, "latest"),
                parseJson(json, "link"),
                parseJson(json, "lts"),
                parseJson(json, "support"),
                parseJson(json, "discontinued"),
                parseJson(json, "latestReleaseDate"),
                firstNonNull(parseJson(json, "supportedPhpVersions"), parseJson(json, "supportedPHPVersions")),
                parseJson(json, "latestJdkVersion"),
                parseJson(json, "extendedSupport"),
                parseJson(json, "upgradeVersion"),
                parseJson(json, "releaseLabel"),
                parseJson(json, "supportedKubernetesVersions"),
                parseJson(json, "minRubyVersion"),
                parseJson(json, "codename"),
                parseJson(json, "technicalGuidance"),
                parseJson(json, "supportedJavaVersions"),
                parseJson(json, "minJavaVersion"));
    }

    /**
     * The default comparator for EolCycle objects.
     * <p>
     * This comparator defines the default sorting order for EolCycle objects. It first compares
     * the products using their natural order. If the products are equal, it then compares the
     * cycles using the VersionComparator.INSTANCE.compare() method. Next, it compares the
     * release dates, followed by the EOL dates. If the EOL dates are equal, it compares the latest
     * versions using the VersionComparator.INSTANCE.compare() method. Finally, it compares the
     * links and LTS values.
     * <p>
     * Usually, the cycle is already a unique value within a product, but just to be sure, other attributes are used.
     *
     * @see AeaaEolCycle
     */
    private final static Comparator<AeaaEolCycle> DEFAULT_COMPARATOR = Comparator.comparing(AeaaEolCycle::getProduct, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing((o1, o2) -> {
                if (o1.getCycle() == null || o2.getCycle() == null) {
                    return 0;
                }
                return o1.getCycle().compareTo(o2.getCycle());
            })
            .thenComparing(AeaaEolCycle::getReleaseDate, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(AeaaEolCycle::getEol, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing((o1, o2) -> {
                if (o1.getLatest() == null || o2.getLatest() == null) {
                    return 0;
                }
                return o1.getLatest().compareTo(o2.getLatest());
            })
            .thenComparing(AeaaEolCycle::getLink, Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(AeaaEolCycle::getLts, Comparator.nullsLast(Comparator.naturalOrder()));

    @Override
    public int compareTo(AeaaEolCycle o) {
        return DEFAULT_COMPARATOR.compare(this, o);
    }

    private static String parseJson(JSONObject json, String key) {
        Object obj = json.opt(key);
        if (obj instanceof Boolean) {
            return Boolean.toString((Boolean) obj);
        } else if (obj != null && !obj.toString().equals("null")) {
            return obj.toString();
        } else {
            return null;
        }
    }

    private void putIfNotNull(JSONObject json, String key, String value) {
        if (value != null && !value.equals("null")) {
            json.put(key, value);
        }
    }

    @Override
    public String toString() {
        return toJson().toString();
    }

    /**
     * @param version The version to check for
     * @return -1 if the version is not in the cycle, the higher a positive number is, the more the version matches.
     */
    public int matchVersion(String version) {
        if (version == null) {
            return -1;
        }

        if (version.equals(latest)) {
            return Integer.MAX_VALUE;
        }

        final String normalizedVersion = normalizeVersionForComparison(version);
        final String normalizedCycle = normalizeVersionForComparison(cycle);
        final String normalizedReleaseLabel = normalizeVersionForComparison(releaseLabel);

        final boolean cycleDefined = normalizedCycle != null;
        final boolean releaseLabelDefined = normalizedReleaseLabel != null;

        if (cycleDefined && normalizedVersion.startsWith(normalizedCycle)) {
            return normalizedCycle.length();
        } else if (releaseLabelDefined && normalizedVersion.startsWith(normalizedReleaseLabel)) {
            return normalizedReleaseLabel.length();
        }

        if (cycleDefined && normalizedCycle.startsWith(normalizedVersion)) {
            return normalizedVersion.length();
        } else if (releaseLabelDefined && normalizedReleaseLabel.startsWith(normalizedVersion)) {
            return normalizedVersion.length();
        }

        return -1;
    }

    private String normalizeVersionForComparison(String version) {
        if (version == null) {
            return null;
        }

        if (version.startsWith("v")) {
            version = version.substring(1);
        }

        version = version.toLowerCase();
        version = version.replaceAll("[^a-z0-9.]", "");

        return version;
    }

    /**
     * Checks if the provided PHP version is contained within the supported PHP versions.<br>
     * Example: 7.3 is contained in 7.3 - 8.1, 7.3, 7.4
     *
     * @param version The PHP version to check.
     * @return True if the PHP version is contained within the supported PHP versions, false otherwise.
     */
    public boolean containsPhpVersion(String version) {
        return containsSupportedVersion(version, supportedPhpVersions);
    }

    public boolean containsKubernetesVersion(String version) {
        return containsSupportedVersion(version, supportedKubernetesVersions);
    }

    public boolean containsJavaVersion(String version) {
        return containsSupportedVersion(version, supportedJavaVersions);
    }

    /**
     * Checks if the provided version is contained within the supported versions.<br>
     * Example: 7.3 is contained in 7.3 - 8.1, 7.3, 7.4
     *
     * @param version           The version to check.
     * @param supportedVersions A string containing the supported versions separated by commas.
     * @return True if the version is contained within the supported versions, false otherwise.
     */
    private boolean containsSupportedVersion(String version, String supportedVersions) {
        if (version == null) {
            return false;
        } else if (supportedVersions == null) {
            return false;
        }

        final String[] versions = supportedVersions.split(", ?");
        for (String ver : versions) {
            if (checkIfSingleVersionIsContained(ver, version)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Checks if a single version is contained within a given part.<br>
     * Example: 7.3 is contained in "7.3 - 8.1" and "7.3"
     *
     * @param part    The part to check, which can be a single version or a range of versions.
     * @param version The PHP version to check against the part.
     * @return True if the PHP version is contained within the part, false otherwise.
     */
    private boolean checkIfSingleVersionIsContained(String part, String version) {
        if (part.contains(" - ")) {
            final String[] range = part.split(" - ");
            if (range.length != 2) {
                return false;
            }

            // return VersionComparator.INSTANCE.compare(range[0], version) <= 0 && VersionComparator.INSTANCE.compare(range[1], version) >= 0;
            return version.compareTo(range[0]) >= 0 && version.compareTo(range[1]) <= 0;
        } else {
            return version.startsWith(part);
        }
    }
}
