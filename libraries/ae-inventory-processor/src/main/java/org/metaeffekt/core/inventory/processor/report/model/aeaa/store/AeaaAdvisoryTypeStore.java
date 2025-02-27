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
package org.metaeffekt.core.inventory.processor.report.model.aeaa.store;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONObject;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.advisory.*;
import org.metaeffekt.core.inventory.processor.report.model.aeaa.csaf.AeaaCsafAdvisoryEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Mirrors structure of <code>com.metaeffekt.mirror.contents.advisory.OsvAdvisorEntry</code>
 * until separation of inventory report generation from ae core inventory processor.
 */
public class AeaaAdvisoryTypeStore extends AeaaContentIdentifierStore<AeaaAdvisoryTypeIdentifier<?>> {

    private static final Logger LOG = LoggerFactory.getLogger(Inventory.class);

    public final static AeaaAdvisoryTypeIdentifier<AeaaCertFrAdvisorEntry> CERT_FR = new AeaaAdvisoryTypeIdentifier<>(
            "CERT_FR", "CERT-FR", "",
            Pattern.compile("((?:CERTFR|CERTA)-\\d+-(?:ACT|AVI|ALE|INF)-\\d+(?:-\\d+)?)", Pattern.CASE_INSENSITIVE),
            AeaaCertFrAdvisorEntry.class, AeaaCertFrAdvisorEntry::new);
    public final static AeaaAdvisoryTypeIdentifier<AeaaCertSeiAdvisorEntry> CERT_SEI = new AeaaAdvisoryTypeIdentifier<>(
            "CERT_SEI", "CERT-SEI", "",
            Pattern.compile("(VU#(\\d+))", Pattern.CASE_INSENSITIVE),
            AeaaCertSeiAdvisorEntry.class, AeaaCertSeiAdvisorEntry::new);
    public final static AeaaAdvisoryTypeIdentifier<AeaaCertEuAdvisorEntry> CERT_EU = new AeaaAdvisoryTypeIdentifier<>(
            "CERT_EU", "CERT-EU", "",
            Pattern.compile("(CERT-EU-(\\d+))", Pattern.CASE_INSENSITIVE),
            AeaaCertEuAdvisorEntry.class, AeaaCertEuAdvisorEntry::new);
    public final static AeaaAdvisoryTypeIdentifier<AeaaMsrcAdvisorEntry> MSRC = new AeaaAdvisoryTypeIdentifier<>(
            "MSRC", "MSRC", "",
            Pattern.compile("(MSRC-(?:CVE|CAN)-([0-9]{4})-([0-9]{4,})|ADV(\\d+))", Pattern.CASE_INSENSITIVE),
            AeaaMsrcAdvisorEntry.class, AeaaMsrcAdvisorEntry::new);
    /**
     * <a href="https://github.com/github/advisory-database">Pattern source</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaGhsaAdvisorEntry> GHSA = new AeaaAdvisoryTypeIdentifier<>(
            "GHSA", "GHSA", "",
            Pattern.compile("GHSA(-[23456789cfghjmpqrvwx]{4}){3}"),
            AeaaGhsaAdvisorEntry.class, AeaaGhsaAdvisorEntry::new);

    // OSV DATA SOURCES

    /**
     * Generic OSV-Type Identifier will be used if no other known Identifier matches is matched.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_GENERIC_IDENTIFIER =
            createOsvIdentifier("OSV", "OSV",
                    Pattern.compile("UNKNOWN", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Android Security Bulletins</b>.
     * Find details about Android security vulnerabilities and patches.
     * <a href="https://source.android.com/docs/security/bulletin">Android Security Bulletins</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_ASB_A =
            createOsvIdentifier("ASB-A", "Android Security Bulletin",
                    Pattern.compile("ASB-A-\\d+", Pattern.CASE_INSENSITIVE));
    /**
     * Advisory IDs from the <b>Android Security Bulletins</b>.
     * Find details about Android security vulnerabilities and patches.
     * <a href="https://source.android.com/docs/security/bulletin">Android Security Bulletins</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_PUB_A =
            createOsvIdentifier("PUB-A", "Android Security Bulletin",
                    Pattern.compile("PUB-A-\\d+", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>AlmaLinux Security Advisories</b>.
     * Provides information on security updates and vulnerabilities for AlmaLinux.
     * <a href="https://errata.almalinux.org/">AlmaLinux Vulnerability Advisory Database Homepage</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_ALSA =
            createOsvIdentifier("ALSA", "AlmaLinux Security Advisory",
                    Pattern.compile("ALSA-\\d{4}:\\d{4}", Pattern.CASE_INSENSITIVE));
    /**
     * Advisory IDs from the <b>AlmaLinux Bug Advisories</b>.
     * Details on bug fixes and updates for AlmaLinux.
     * <a href="https://errata.almalinux.org/">AlmaLinux Bug Advisory Database</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_ALBA =
            createOsvIdentifier("ALBA", "AlmaLinux Bug Advisory",
                    Pattern.compile("ALBA-\\d{4}:\\d{4}", Pattern.CASE_INSENSITIVE));
    /**
     * Advisory IDs from the <b>AlmaLinux Enhancement Advisories</b>.
     * Information on enhancements and feature updates for AlmaLinux.
     * <a href="https://errata.almalinux.org/">AlmaLinux Enhancement Advisory Database</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_ALEA =
            createOsvIdentifier("ALEA", "AlmaLinux Enhancement Advisory",
                    Pattern.compile("ALEA-\\d{4}:\\d{4}", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Bitnami Vulnerability Database</b>.
     * Contains security advisories for Bitnami packages.
     * <a href="https://github.com/bitnami/vulndb">Bitnami Vulnerability Database on GitHub</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_BIT =
            createOsvIdentifier("BIT", "Bitnami Vulnerability Database",
                    Pattern.compile("BIT-[a-zA-Z0-9\\-_]+-\\d{4}-\\d+", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>National Vulnerability Database (NVD)</b>.
     * Official U.S. government repository of standards-based vulnerability management data.
     * <a href="https://nvd.nist.gov/">National Vulnerability Database (NVD)</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_CVE =
            createOsvIdentifier("OSV-CVE", "National Vulnerability Database",
                    Pattern.compile("(OSV-)?CVE-\\d{4}-\\d{4,}", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Debian Security Advisories</b>.
     * Provides information on security vulnerabilities in Debian packages.
     * <a href="https://www.debian.org/security/">Debian Security Advisories</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_DSA =
            createOsvIdentifier("DSA", "Debian Security Advisory",
                    Pattern.compile("DSA-\\d+(-\\d)?", Pattern.CASE_INSENSITIVE));
    /**
     * Advisory IDs from the <b>Debian Long Term Support Advisories</b>.
     * Security updates and advisories for Debian LTS.
     * <a href="https://www.debian.org/lts/security/">Debian LTS Security Advisories</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_DLA =
            createOsvIdentifier("DLA", "Debian LTS Advisory",
                    Pattern.compile("DLA-\\d+-\\d", Pattern.CASE_INSENSITIVE));
    /**
     * Advisory IDs from the <b>Debian Temporary Security Advisories</b>.
     * Temporary security advisories for Debian.
     * <a href="https://www.debian.org/security/">Debian Security Advisories</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_DTSA =
            createOsvIdentifier("DTSA", "Debian Temporary Security Advisory",
                    Pattern.compile("DTSA-\\d+-\\d", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>GitHub Advisory Database</b>.
     * Security advisories curated by GitHub.
     * <a href="https://github.com/github/advisory-database">GitHub Advisory Database</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_GHSA =
            createOsvIdentifier("GHSA", "GitHub Security Advisory",
                    Pattern.compile("GHSA(-[23456789cfghjmpqrvwx]{4}){3}", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Go Vulnerability Database</b>.
     * Official database of vulnerabilities for Go packages.
     * <a href="https://pkg.go.dev/vuln/">Go Vulnerability Database</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_GO =
            createOsvIdentifier("GO", "Go Vulnerability Database",
                    Pattern.compile("GO-\\d{4}-\\d{4}", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Global Security Database (GSD)</b>.
     * Community-driven security database.
     * <a href="https://github.com/cloudsecurityalliance/gsd-database">Global Security Database (GSD)</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_GSD =
            createOsvIdentifier("GSD", "Global Security Database",
                    Pattern.compile("GSD-\\d{4}-\\d{7}", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Haskell Security Advisory Database</b>.
     * Security advisories for Haskell packages.
     * <a href="https://github.com/haskell/security-advisories">Haskell Security Advisory Database</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_HSEC =
            createOsvIdentifier("HSEC", "Haskell Security Advisory Database",
                    Pattern.compile("HSEC-\\d{4}-\\d{4}", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Malicious Packages Repository</b>.
     * Database of known malicious packages.
     * <a href="https://github.com/ossf/malicious-packages/tree/main/osv/">Malicious Packages Repository</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_MAL =
            createOsvIdentifier("MAL", "Malicious Packages Repository",
                    Pattern.compile("MAL-\\d{4}-\\d+", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Open Source Vulnerabilities (OSV) List</b>.
     * Database of open-source vulnerabilities collected by OSV.
     * <a href="https://osv.dev/list">Open Source Vulnerabilities (OSV) List</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_OSV =
            createOsvIdentifier("OSV", "Open Source Vulnerabilities",
                    Pattern.compile("OSV-\\d{4}-\\d+", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Python Software Foundation Advisory Database</b>.
     * Security advisories for Python packages maintained by the PSF.
     * <a href="https://github.com/psf/advisory-database">Python Software Foundation Advisory Database</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_PSF =
            createOsvIdentifier("PSF", "Python Software Foundation Vulnerability Database",
                    Pattern.compile("PSF-\\d{4}-\\d+", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Python Software Foundation CVE Database</b>.
     * CVE entries assigned by the PSF.
     * <a href="https://github.com/psf/advisory-database">Python Software Foundation CVE Database</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_PSF_CVE =
            createOsvIdentifier("PSF-CVE", "Python Software Foundation CVE Database",
                    Pattern.compile("PSF-CVE-\\d{4}-\\d+", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>PyPI Vulnerability Database</b>.
     * Security advisories for packages on the Python Package Index.
     * <a href="https://github.com/pypa/advisory-db">PyPI Vulnerability Database</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_PYSEC =
            createOsvIdentifier("PYSEC", "PyPI Vulnerability Database",
                    Pattern.compile("PYSEC-\\d{4}-\\d+", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>R Consortium Advisory Database</b>.
     * Security advisories for R packages.
     * <a href="https://github.com/RConsortium/r-advisory-database">R Consortium Advisory Database</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_RSEC =
            createOsvIdentifier("RSEC", "R Consortium Advisory Database",
                    Pattern.compile("RSEC-\\d{4}-\\d+", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Rocky Linux Security Advisories</b>.
     * Security updates and vulnerabilities for Rocky Linux.
     * <a href="https://errata.rockylinux.org/">Rocky Linux Security Advisory Database Homepage</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_RLSA =
            createOsvIdentifier("RLSA", "Rocky Linux Security Advisory",
                    Pattern.compile("RLSA-\\d{4}:\\d{4}", Pattern.CASE_INSENSITIVE));
    /**
     * Advisory IDs from the <b>Rocky Linux Extra Security Advisories</b>.
     * Additional security advisories for Rocky Linux.
     * <a href="https://errata.rockylinux.org/">Rocky Linux Security Advisory Database Homepage</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_RXSA =
            createOsvIdentifier("RXSA", "Rocky Linux Extra Security Advisory",
                    Pattern.compile("RXSA-\\d{4}:\\d{4}", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>RustSec Advisory Database</b>.
     * Security advisories for Rust packages and crates.
     * <a href="https://github.com/rustsec/advisory-db">RustSec Advisory Database</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_RUSTSEC =
            createOsvIdentifier("RUSTSEC", "RustSec Advisory Database",
                    Pattern.compile("RUSTSEC-\\d{4}-\\d{4}", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Ubuntu Security Notices</b>.
     * Official security notices for Ubuntu packages.
     * <a href="https://ubuntu.com/security/notices">Ubuntu Security Notices</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_USN =
            createOsvIdentifier("USN", "Ubuntu Security Notices",
                    Pattern.compile("((USN)|(UBUNTU-CVE))-\\d{4}-\\d+", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Chainguard Security Notices</b>.
     * Security advisories for Chainguard packages.
     * <a href="https://packages.cgr.dev/chainguard/osv/all.json">Chainguard Security Notices</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_CGA =
            createOsvIdentifier("CGA", "Chainguard Security Notices",
                    Pattern.compile("CGA(-[a-zA-Z0-9]{4}){3}", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>curl Security Advisories</b>.
     * Official security advisories for curl.
     * <a href="https://curl.se/docs/security.html">curl Security Advisories</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_CURL_CVE =
            createOsvIdentifier("CURL-CVE", "curl CVEs",
                    Pattern.compile("CURL-CVE-\\d{4}-\\d+", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Unified Vulnerability Identifier Database</b>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_UVI =
            createOsvIdentifier("UVI", "Unified Vulnerability Identifier Database",
                    Pattern.compile("UVI-\\d{4}-\\d{7}", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Chinese National Vulnerability Database</b> (possibly).
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_CAN =
            createOsvIdentifier("CAN", "Chinese National Vulnerability Database",
                    Pattern.compile("CAN-\\d{4}-\\d+", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>SUSE Security Update</b>.
     * Official security advisories for curl.
     * <a href="https://www.suse.com/support/security/">SUSE Security Landing Page</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_SUSE_SU =
            createOsvIdentifier("SUSE-SU", "SUSE Security Update",
                    Pattern.compile("SUSE-SU-\\d{3,4}([:/]\\d{3,5}-\\d)?( Forbidden-1)?", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>SUSE Risk Update</b>.
     * Official security advisories for curl.
     * <a href="https://www.suse.com/support/security/">SUSE Security Landing Page</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_SUSE_RU =
            createOsvIdentifier("SUSE-RU", "SUSE Risk Update",
                    Pattern.compile("SUSE-RU-\\d{4}[:/]\\d{4,5}-\\d", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>SUSE Feature Update</b>.
     * Official security advisories for curl.
     * <a href="https://www.suse.com/support/security/">SUSE Security Landing Page</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_SUSE_FU =
            createOsvIdentifier("SUSE-FU", "SUSE Feature Update",
                    Pattern.compile("SUSE-FU-\\d{4}[:/]\\d{4}-\\d", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>SUSE Other Update</b>.
     * Official security advisories for curl.
     * <a href="https://www.suse.com/support/security/">SUSE Security Landing Page</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_SUSE_OU =
            createOsvIdentifier("SUSE-OU", "SUSE Other Update",
                    Pattern.compile("SUSE-OU-\\d{4}[:/]\\d{4}-\\d", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>openSUSE Security Update</b>.
     * Official security advisories for curl.
     * <a href="https://www.suse.com/support/security/">SUSE Security Landing Page</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_OPENSUSE_SU =
            createOsvIdentifier("openSUSE-SU", "openSUSE Security Update",
                    Pattern.compile("openSUSE-SU-\\d{4}[:/]\\d{4,5}-\\d", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>red Hat Security Advisory</b>.
     * Official security advisories for Red Hat Security Advisories.
     * <a href="https://access.redhat.com/security/">Red Hat Security Landing Page</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_RHSA =
            createOsvIdentifier("RHSA", "Red Hat Security Advisory",
                    Pattern.compile("RHSA-\\d{4}:\\d{3,5}", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Red Hat Bug Advisory</b>.
     * Official security advisories for Red Hat Bug Advisories.
     * <a href="https://access.redhat.com/security/">Red Hat Security Landing Page</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_RHBA =
            createOsvIdentifier("RHBA", "Red Hat Bug Advisory",
                    Pattern.compile("RHBA-\\d{4}:\\d{3,5}", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Red Hat Enhancement Advisory</b>.
     * Official security advisories for Red Hat Enhancement Advisories.
     * <a href="https://access.redhat.com/security/">Red Hat Security Landing Page</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_RHEA =
            createOsvIdentifier("RHEA", "Red Hat Enhancement Advisory",
                    Pattern.compile("RHEA-\\d{4}:\\d{3,5}", Pattern.CASE_INSENSITIVE));

    /**
     * Advisory IDs from the <b>Mageia Security Advisory</b>.
     * Official security advisories for Mageia Linux.
     * <a href="https://advisories.mageia.org/">Mageia Security Advisories</a>.
     */
    public final static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> OSV_MGASA =
            createOsvIdentifier("MGASA", "Mageia Security Advisory",
                    Pattern.compile("(MGASA|MGAA)-\\d{4}:\\d{4}", Pattern.CASE_INSENSITIVE));
    
    // CSAF
    public final static AeaaAdvisoryTypeIdentifier<AeaaCsafAdvisoryEntry> CSAF_GENERIC_IDENTIFIER = new AeaaAdvisoryTypeIdentifier<>(
            "CSAF", "CSAF", "CSAF",
            Pattern.compile("UNKNOWN", Pattern.CASE_INSENSITIVE),
            AeaaCsafAdvisoryEntry.class, () -> new AeaaCsafAdvisoryEntry(AeaaAdvisoryTypeStore.get().fromNameAndImplementation("CSAF", "CSAF")));

    public final static AeaaAdvisoryTypeIdentifier<AeaaCsafAdvisoryEntry> CSAF_BSI = new AeaaAdvisoryTypeIdentifier<>(
            "https://www.bsi.bund.de", "BSI", "CSAF",
            Pattern.compile("((?:WID-SEC-W)|(?:BSI))-\\d{4}-\\d{4}", Pattern.CASE_INSENSITIVE),
            AeaaCsafAdvisoryEntry.class, () -> new AeaaCsafAdvisoryEntry(AeaaAdvisoryTypeStore.get().fromNameAndImplementation("BSI", "CSAF")));

    public final static AeaaAdvisoryTypeIdentifier<AeaaCsafAdvisoryEntry> CSAF_REDHAT = new AeaaAdvisoryTypeIdentifier<>(
            "https://www.redhat.com", "Red Hat", "CSAF",
            Pattern.compile("(?:CVE-\\d{4}-\\d{4,5})|RHSA-\\d{4}:\\d{3,5}", Pattern.CASE_INSENSITIVE),
            AeaaCsafAdvisoryEntry.class, () -> new AeaaCsafAdvisoryEntry(AeaaAdvisoryTypeStore.get().fromNameAndImplementation("REDHAT", "CSAF")));

    public final static AeaaAdvisoryTypeIdentifier<AeaaCsafAdvisoryEntry> CSAF_SIEMENS = new AeaaAdvisoryTypeIdentifier<>(
            "https://www.siemens.com", "Siemens", "CSAF",
            Pattern.compile("((?:WID-SEC-W)|(?:BSI))-\\d{4}-\\d{4}", Pattern.CASE_INSENSITIVE),
            AeaaCsafAdvisoryEntry.class, () -> new AeaaCsafAdvisoryEntry(AeaaAdvisoryTypeStore.get().fromNameAndImplementation("SIEMENS", "CSAF")));

    public final static AeaaAdvisoryTypeIdentifier<AeaaCsafAdvisoryEntry> CSAF_CISA = new AeaaAdvisoryTypeIdentifier<>(
            "https://www.cisa.gov/", "CISA", "CSAF",
            Pattern.compile("ICSM?A-\\d{2}-\\d{3}-\\d{2}", Pattern.CASE_INSENSITIVE),
            AeaaCsafAdvisoryEntry.class, () -> new AeaaCsafAdvisoryEntry(AeaaAdvisoryTypeStore.get().fromNameAndImplementation("CISA", "CSAF")));

    public final static AeaaAdvisoryTypeIdentifier<AeaaCsafAdvisoryEntry> CSAF_INTEVATION = new AeaaAdvisoryTypeIdentifier<>(
            "https://intevation.de", "Intevation", "CSAF",
            Pattern.compile("intevation-os-\\d{4}-\\d{4}", Pattern.CASE_INSENSITIVE),
            AeaaCsafAdvisoryEntry.class, () -> new AeaaCsafAdvisoryEntry(AeaaAdvisoryTypeStore.get().fromNameAndImplementation("INTEVATION", "CSAF")));

    public final static AeaaAdvisoryTypeIdentifier<AeaaCsafAdvisoryEntry> CSAF_NOZOMI = new AeaaAdvisoryTypeIdentifier<>(
            "https://security.nozominetworks.com/psirt", "Nozomi Networks", "CSAF",
            Pattern.compile("NN-\\d{4}:\\d{1,2}-\\d{2}", Pattern.CASE_INSENSITIVE),
            AeaaCsafAdvisoryEntry.class, () -> new AeaaCsafAdvisoryEntry(AeaaAdvisoryTypeStore.get().fromNameAndImplementation("NOZOMI", "CSAF")));

    public final static AeaaAdvisoryTypeIdentifier<AeaaCsafAdvisoryEntry> CSAF_SICK = new AeaaAdvisoryTypeIdentifier<>(
            "https://sick.com/psirt", "SICK", "CSAF",
            Pattern.compile("SCA-\\d{4}-\\d{4}", Pattern.CASE_INSENSITIVE),
            AeaaCsafAdvisoryEntry.class, () -> new AeaaCsafAdvisoryEntry(AeaaAdvisoryTypeStore.get().fromNameAndImplementation("SICK", "CSAF")));

    public final static AeaaAdvisoryTypeIdentifier<AeaaCsafAdvisoryEntry> CSAF_OPEN_XCHANGE = new AeaaAdvisoryTypeIdentifier<>(
            "https://open-xchange.com/", "Open-Exchange", "CSAF",
            Pattern.compile("OXAS-ADV-\\d{4}-\\d{4}", Pattern.CASE_INSENSITIVE),
            AeaaCsafAdvisoryEntry.class, () -> new AeaaCsafAdvisoryEntry(AeaaAdvisoryTypeStore.get().fromNameAndImplementation("OPEN_XCHANGE", "CSAF")));

    // ANY IDENTIFIER
    public final static AeaaAdvisoryTypeIdentifier<AeaaGeneralAdvisorEntry> ANY_ADVISORY_FILTER_WILDCARD = new AeaaAdvisoryTypeIdentifier<>(
            "any", "any", "any",
            Pattern.compile("(any|all)", Pattern.CASE_INSENSITIVE),
            AeaaGeneralAdvisorEntry.class, AeaaGeneralAdvisorEntry::new);

    private final static Pattern HTTP_MATCHING_PATTERN = Pattern.compile("^(?:https?://)?(?:www\\.)?([^:/\\.\\n]+)\\.(?:(?:[^:/\\.\\n]+)\\.?)+(/(?:[^:/\\.\\n]+)?)?$");

    private final static AeaaAdvisoryTypeStore INSTANCE = new AeaaAdvisoryTypeStore();

    public static AeaaAdvisoryTypeStore get() {
        return INSTANCE;
    }

    protected AeaaAdvisoryTypeStore() {
    }

    @Override
    protected AeaaAdvisoryTypeIdentifier<?> createIdentifier(String name, String implementation) {
        if (implementation.equals("CSAF")) {
            final Matcher m = HTTP_MATCHING_PATTERN.matcher(name);
            final String finalNameCsaf = m.matches() ? m.group(1) : name;
            return new AeaaAdvisoryTypeIdentifier<>(
                    name, finalNameCsaf,
                    implementation,
                    Pattern.compile("UNKNOWN", Pattern.CASE_INSENSITIVE),
                    AeaaCsafAdvisoryEntry.class,
                    () -> new AeaaCsafAdvisoryEntry(AeaaAdvisoryTypeStore.get().fromNameAndImplementation(finalNameCsaf, implementation)));

        } else if (implementation.equals("OSV")) {
            return new AeaaAdvisoryTypeIdentifier<>(
                    name, AeaaContentIdentifier.deriveWellFormedName(name),
                    implementation,
                    Pattern.compile("UNKNOWN", Pattern.CASE_INSENSITIVE),
                    AeaaOsvAdvisorEntry.class,
                    () -> new AeaaOsvAdvisorEntry(AeaaAdvisoryTypeStore.get().fromNameAndImplementation(name, implementation)));
        }

        if (name.equalsIgnoreCase("any") || implementation.equalsIgnoreCase("all")) {
            return AeaaAdvisoryTypeStore.ANY_ADVISORY_FILTER_WILDCARD;
        }

        return new AeaaAdvisoryTypeIdentifier<>(
                name, AeaaContentIdentifier.deriveWellFormedName(name),
                implementation,
                Pattern.compile("UNKNOWN", Pattern.CASE_INSENSITIVE),
                AeaaGeneralAdvisorEntry.class,
                () -> new AeaaGeneralAdvisorEntry(AeaaAdvisoryTypeStore.get().fromNameAndImplementation(name, implementation)));
    }

    @Override
    protected Collection<AeaaAdvisoryTypeIdentifier<?>> createDefaultIdentifiers() {
        return Arrays.asList(
                CERT_FR, CERT_SEI, CERT_EU, MSRC, GHSA,
                // OSV DATA SOURCES
                OSV_GENERIC_IDENTIFIER,
                OSV_CVE, OSV_MAL, OSV_PYSEC, OSV_GHSA, OSV_OSV, OSV_ALSA, OSV_ALEA, OSV_ALBA, OSV_GO, OSV_DSA, OSV_DLA,
                OSV_DTSA, OSV_RUSTSEC, OSV_CGA, OSV_BIT, OSV_UVI, OSV_CAN, OSV_RSEC, OSV_GSD, OSV_USN, OSV_PUB_A, OSV_ASB_A,
                OSV_HSEC, OSV_RLSA, OSV_RXSA, OSV_CURL_CVE, OSV_PSF, OSV_PSF_CVE,
                OSV_SUSE_SU, OSV_SUSE_RU, OSV_SUSE_FU, OSV_SUSE_OU, OSV_OPENSUSE_SU,
                OSV_RHBA, OSV_RHEA, OSV_RHSA, OSV_MGASA,
                // CSAF
                CSAF_GENERIC_IDENTIFIER, CSAF_BSI, CSAF_REDHAT, CSAF_SIEMENS, CSAF_CISA, CSAF_INTEVATION, CSAF_NOZOMI,
                CSAF_SICK, CSAF_OPEN_XCHANGE,
                // OTHER
                ANY_ADVISORY_FILTER_WILDCARD
        );
    }

    @Override
    public AeaaSingleContentIdentifierParseResult<AeaaAdvisoryTypeIdentifier<?>> fromJsonNameAndImplementation(JSONObject json) {
        final AeaaSingleContentIdentifierParseResult<?> superResult = super.fromJsonNameAndImplementation(json);

        if (superResult.getIdentifier() instanceof AeaaAdvisoryTypeIdentifier) {
            return (AeaaSingleContentIdentifierParseResult<AeaaAdvisoryTypeIdentifier<?>>) superResult;
        } else {
            throw new IllegalArgumentException("The provided JSON object does not represent an advisory type identifier, which is an impossible scenario since this class can by definition only support advisory type identifiers.");
        }
    }

    @Override
    public AeaaSingleContentIdentifierParseResult<AeaaAdvisoryTypeIdentifier<?>> fromMap(Map<String, Object> map) {
        final AeaaSingleContentIdentifierParseResult<?> superResult = super.fromMap(map);

        if (superResult.getIdentifier() instanceof AeaaAdvisoryTypeIdentifier) {
            return (AeaaSingleContentIdentifierParseResult<AeaaAdvisoryTypeIdentifier<?>>) superResult;
        } else {
            throw new IllegalArgumentException("The provided map does not represent an advisory type identifier, which is an impossible scenario since this class can by definition only support advisory type identifiers.");
        }
    }

    public AeaaSingleContentIdentifierParseResult<AeaaAdvisoryTypeIdentifier<?>> fromAdvisoryMetaData(AdvisoryMetaData amd) {
        final String source = ObjectUtils.firstNonNull(amd.get(AdvisoryMetaData.Attribute.SOURCE));
        final String implementation = ObjectUtils.firstNonNull(amd.get(AdvisoryMetaData.Attribute.SOURCE_IMPLEMENTATION));
        final String entryId = ObjectUtils.firstNonNull(amd.get(AdvisoryMetaData.Attribute.NAME));

        if (StringUtils.isEmpty(source)) {
            // this should never happen, but let's catch it anyway.
            // the only other option is to check if the name is somehow recognized by any of the patterns.
            final Optional<AeaaSingleContentIdentifierParseResult<AeaaAdvisoryTypeIdentifier<?>>> result = fromEntryIdentifier(entryId);
            if (result.isPresent()) {
                return result.get();
            }

            throw new IllegalArgumentException("The advisory meta data does not contain a source attribute, which is required to determine the advisory type.");
        }

        final AeaaAdvisoryTypeIdentifier<?> AeaaAdvisoryTypeIdentifier = this.fromNameAndImplementation(source, implementation);
        return new AeaaSingleContentIdentifierParseResult<>(AeaaAdvisoryTypeIdentifier, entryId);
    }

    public static <T extends AeaaContentIdentifier> Optional<AeaaSingleContentIdentifierParseResult<AeaaAdvisoryTypeIdentifier<?>>> fromEntryIdentifier(String entryId) {
        if (StringUtils.isEmpty(entryId)) {
            LOG.warn("The advisory does not contain a source or name attribute, which is required to determine the advisory type.");
            return Optional.empty();
        }

        for (AeaaAdvisoryTypeIdentifier<?> typeIdentifier : AeaaAdvisoryTypeStore.get().values()) {
            if (typeIdentifier.patternMatchesId(entryId)) {
                return Optional.of(new AeaaSingleContentIdentifierParseResult<>(typeIdentifier, entryId));
            }
        }

        return Optional.empty();
    }

    public void inferSourceIdentifierFromIdIfAbsent(AeaaAdvisoryEntry securityAdvisory) {
        if (securityAdvisory.getSourceIdentifier() == null) {
            this.fromId(securityAdvisory.getId()).ifPresent(inferred -> {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Inferred source identifier [{}] for advisory entry [{}]", inferred.toExtendedString(), securityAdvisory.getId());
                }
                securityAdvisory.setSourceIdentifier(inferred);
            });
        }
    }

    private static AeaaAdvisoryTypeIdentifier<AeaaOsvAdvisorEntry> createOsvIdentifier(String name, String wellFormedName, Pattern pattern) {
        return new AeaaAdvisoryTypeIdentifier<>(
                name, wellFormedName, "OSV",
                pattern,
                AeaaOsvAdvisorEntry.class,
                () -> new AeaaOsvAdvisorEntry(AeaaAdvisoryTypeStore.get().fromNameAndImplementation(name, "OSV")));
    }

    public List<AeaaAdvisoryTypeIdentifier<?>> osvValues() {
        return this.values().stream()
                .filter(c -> c.getImplementation().equals("OSV"))
                .collect(Collectors.toList());
    }
}
