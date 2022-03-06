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

public class CertFrUtils {

    private final static String CERT_FR_BASE_URL = "https://www.cert.ssi.gouv.fr/";

    // FIXME: rather than duplicating code here, we could introduce a type in the CertFr JSON.
    /**
     * A Cert-Fr entry can be a lot of different things. This returns the type for a given Cert-Fr.
     *
     * @param certfr The Cert-Fr identifier to get the type from.
     * @return The type to the given Cert-Fr identifier.
     *
     *
     */
    public static String getType(String certfr) {
        if (certfr == null) return "Unknown type";
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
        else if (certfr.contains("REC"))
            return "Information";
        else if (certfr.contains("INF"))
            return "Information";
        else return "Unknown type";
    }

    // FIXME: rather than duplicating code here, we could introduce a type in the CertFr JSON.
    /**
     * Generates a url from a given certfr id.
     *
     * @param certfr The Cert-Fr identifier to generate an url to.
     * @return If the Cert-Fr identifier is valid: The generated URL, otherwise the Cert-Fr base url.
     */
    public static String toURL(String certfr) {
        if (certfr == null) return CERT_FR_BASE_URL;
        String onlineCertFr = certfr.replaceAll("((?:CERTFR|CERTA)-\\d+-(?:ACT|AVI|ALE|INF)-\\d+)(?:-\\d+)", "$1");
        if (certfr.contains("AVI"))
            return CERT_FR_BASE_URL + "avis/" + onlineCertFr;
        else if (certfr.contains("ALE"))
            return CERT_FR_BASE_URL + "alerte/" + onlineCertFr;
        else if (certfr.contains("IOC"))
            return CERT_FR_BASE_URL + "ioc/" + onlineCertFr;
        else if (certfr.contains("DUR"))
            return CERT_FR_BASE_URL + "dur/" + onlineCertFr;
        else if (certfr.contains("ACT"))
            return CERT_FR_BASE_URL + "actualite/" + onlineCertFr;
        else if (certfr.contains("CTI"))
            return CERT_FR_BASE_URL + "cti/" + onlineCertFr;
        else if (certfr.contains("REC"))
            return CERT_FR_BASE_URL + "information/" + onlineCertFr;
        else if (certfr.contains("INF"))
            return CERT_FR_BASE_URL + "information/" + onlineCertFr;
        else return CERT_FR_BASE_URL;
    }

}
