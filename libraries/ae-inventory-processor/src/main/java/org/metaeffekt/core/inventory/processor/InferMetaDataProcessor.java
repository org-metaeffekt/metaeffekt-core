/**
 * Copyright 2009-2020 the original author or authors.
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
package org.metaeffekt.core.inventory.processor;

import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Constants;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseMetaData;
import org.springframework.util.Assert;

import java.util.List;
import java.util.Properties;

import static org.springframework.util.StringUtils.isEmpty;


public class InferMetaDataProcessor extends AbstractInputInventoryBasedProcessor {

    public InferMetaDataProcessor() {
        super();
    }

    public InferMetaDataProcessor(Properties properties) {
        super(properties);
    }

    @Override
    public void process(Inventory inventory) {
        // final Inventory inputInventory = loadInputInventory();

        adoptPackageSpecifiedLicense(inventory);

        // TODO: infer component name?

        inferLicenseMetaData(inventory);
    }

    private void inferLicenseMetaData(Inventory inventory) {
        for (Artifact artifact : inventory.getArtifacts()) {
            String license = artifact.getLicense();
            if (!isEmpty(license) &&
                    !isEmpty(artifact.getComponent()) &&
                    !isEmpty(artifact.getVersion())) {

                String normalizedLicense = license.replaceAll(" AND ", ", ");
                normalizedLicense = normalizedLicense.replaceAll(" and ", ", ");
                // archlinux uses two spaces as separator
                normalizedLicense = normalizedLicense.replaceAll("  ", ", ");


                List<String> licenses = InventoryUtils.tokenizeLicense(normalizedLicense, true, true);
                if (licenses.size() > 1) {
                    LicenseMetaData licenseMetaData = inventory.findMatchingLicenseMetaData(artifact);
                    if (licenseMetaData == null) {
                        licenseMetaData = new LicenseMetaData();
                        licenseMetaData.setComponent(artifact.getComponent());
                        licenseMetaData.setVersion(artifact.getVersion());
                        licenseMetaData.setLicense(license);
                        licenseMetaData.setLicenseInEffect(InventoryUtils.joinEffectiveLicenses(licenses));
                        licenseMetaData.setNotice("TODO: validate and explain licenses in effect; classify component");
                        inventory.getLicenseMetaData().add(licenseMetaData);
                    }

                    Assert.notNull(inventory.findMatchingLicenseMetaData(artifact), "cannot find just added meta data");
                } else if (licenses.size() == 1 && licenses.get(0).contains(" OR ")) {
                    LicenseMetaData licenseMetaData = inventory.findMatchingLicenseMetaData(artifact);
                    if (licenseMetaData == null) {
                        licenseMetaData = new LicenseMetaData();
                        licenseMetaData.setComponent(artifact.getComponent());
                        licenseMetaData.setVersion(artifact.getVersion());
                        licenseMetaData.setLicense(license);
                        licenseMetaData.setLicenseInEffect(license);
                        licenseMetaData.setNotice("TODO: choose and explain licenses in effect; classify component");
                        inventory.getLicenseMetaData().add(licenseMetaData);
                    }

                }
            }
        }
    }

    private void adoptPackageSpecifiedLicense(Inventory inventory) {
        for (Artifact artifact : inventory.getArtifacts()) {
            if (isEmpty(artifact.getLicense())) {
                String derivedPackageLicense = artifact.get(Constants.KEY_DERIVED_LICENSE_PACKAGE);
                if (!isEmpty(derivedPackageLicense)) {
                    artifact.setLicense(derivedPackageLicense);
                }
            }
        }
    }

}
