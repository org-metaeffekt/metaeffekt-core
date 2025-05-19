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
package org.metaeffekt.core.inventory.processor.report.adapter;

import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.apache.maven.model.License;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseData;

import java.util.ArrayList;
import java.util.List;

@Getter
public class InventoryReportAdapter {

    private Inventory inventory;

    private List<LicenseData> licenseDataList;
    /**
     * List to cover artifacts without license.
     */
    private List<Artifact> artifactsWithoutLicense = new ArrayList<>();

    public InventoryReportAdapter(Inventory inventory) {
        this.inventory = inventory;
        this.licenseDataList = inventory.getLicenseData();

        evaluateArtifactsWithoutLicense();
    }

    private void evaluateArtifactsWithoutLicense() {
        // collect artifacts that are components or component parts and have no license
        for (Artifact artifact : inventory.getArtifacts()) {
            if (StringUtils.isEmpty(artifact.getLicense())) {
                // only components or component parts are integrated
                if (artifact.isComponentOrComponentPart()) {
                    artifactsWithoutLicense.add(artifact);
                }
            }
        }
    }

    /**
     * Complex type to enable different required terms categories to be differentiated.
     */
    @Getter
    public static class TermsCategorization {
        /**
         * The termsNoOptions include terms/licenses that do not have an immediate (in contrast to secondary licenses)
         * option (represent a choice).
         */
        List<LicenseData> termsNoOptions = new ArrayList<>();

        /**
         * The termsWithOptions include terms/licenses that do have an immediate option / choice.
         */
        List<LicenseData> termsWithOptions = new ArrayList<>();

        /**
         * The atomicTerms include all atomic terms/licenses. These may also cover atomic options, but no non-atomic
         * options. 'A + B' is not included, but the parts A and B. 'A (or any later version)' is included as option.
         */
        List<LicenseData> atomicTerms = new ArrayList<>();
    }

    /**
     * Categorizes the provided list of canonicalNames (license / terms ids) in determined
     * categories.
     *
     * @param canonicalNames List of canonical names identifying a license or generic terms.
     *
     * @return A {@link TermsCategorization} instance carrying the categorized data.
     */
    public TermsCategorization categorizeTerms(List<String> canonicalNames) {
        final TermsCategorization termsCategorization = new TermsCategorization();
        for (String canonicalName : canonicalNames) {
            LicenseData matchingLicenseData = inventory.findMatchingLicenseData(canonicalName);
            if (matchingLicenseData == null) {
                matchingLicenseData = new LicenseData();
                matchingLicenseData.set(LicenseData.Attribute.CANONICAL_NAME, canonicalName);
            }
            if (matchingLicenseData.isOption()) {
                termsCategorization.termsWithOptions.add(matchingLicenseData);
            } else {
                termsCategorization.termsNoOptions.add(matchingLicenseData);
            }
            if (matchingLicenseData.isAtomic()) {
                termsCategorization.atomicTerms.add(matchingLicenseData);
            }
        }
        return termsCategorization;
    }

    public boolean isRepresentedLicense(String licenseName) {
        for (LicenseData licenseData : licenseDataList) {
            if (licenseName.equals(licenseData.get(LicenseData.Attribute.REPRESENTED_AS))
                    && !licenseName.equals(licenseData.get(LicenseData.Attribute.CANONICAL_NAME))) {
                return true;
            }
        }
        return false;
    }
}
