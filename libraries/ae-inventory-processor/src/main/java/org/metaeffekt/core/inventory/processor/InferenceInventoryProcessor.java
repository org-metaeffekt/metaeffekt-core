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
import org.metaeffekt.core.inventory.extractor.InventoryExtractor;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseMetaData;
import org.metaeffekt.core.inventory.processor.reader.InventoryReader;
import static org.springframework.util.StringUtils.*;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;


public class InferenceInventoryProcessor extends AbstractInputInventoryBasedProcessor {

    public InferenceInventoryProcessor() {
        super();
    }

    public InferenceInventoryProcessor(Properties properties) {
        super(properties);
    }

    @Override
    public void process(Inventory inventory) {
        final Inventory inputInventory = loadInputInventory();

        adoptPackageSpecifiedLicense(inventory);

        // TODO: infer component name?

        inferLicenseMetaData(inventory);
    }

    private void inferLicenseMetaData(Inventory inventory) {
        for (Artifact artifact : inventory.getArtifacts()) {
            if (!isEmpty(artifact.getLicense()) &&
                    !isEmpty(artifact.getComponent()) &&
                    !isEmpty(artifact.getVersion())) {
                List<String> licenses = InventoryUtils.tokenizeLicense(artifact.getLicense(), true, true);
                if (licenses.size() > 1) {
                    LicenseMetaData licenseMetaData = inventory.findMatchingLicenseMetaData(artifact);
                    if (licenseMetaData == null) {
                        licenseMetaData = new LicenseMetaData();
                        licenseMetaData.setComponent(artifact.getComponent());
                        licenseMetaData.setVersion(artifact.getVersion());
                        licenseMetaData.setLicense(artifact.getLicense());
                        licenseMetaData.setLicenseInEffect(InventoryUtils.joinEffectiveLicenses(licenses));
                        licenseMetaData.setNotice("TODO: validate and explain licenses in effect; classify component");
                        inventory.getLicenseMetaData().add(licenseMetaData);
                    }
                }
            }
        }
    }

    private void adoptPackageSpecifiedLicense(Inventory inventory) {
        for (Artifact artifact : inventory.getArtifacts()) {
            if (isEmpty(artifact.getLicense())) {
                String derivedPackageLicense = artifact.get(InventoryExtractor.KEY_DERIVED_LICENSE_PACKAGE);
                if (isEmpty(derivedPackageLicense)) {
                    artifact.setLicense(derivedPackageLicense);
                }
            }
        }
    }

}
