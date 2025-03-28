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
package org.metaeffekt.core.inventory.processor.patterns.contributors;

import org.metaeffekt.core.inventory.processor.linux.LinuxDistributionUtil;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.ComponentPatternData;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.util.FileUtils;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.metaeffekt.core.inventory.processor.model.ComponentPatternData.Attribute.*;
import static org.metaeffekt.core.inventory.processor.model.Constants.*;

public class LinuxDistributionAssetContributor extends ComponentPatternContributor {

    private static final List<String> SUFFIX_LIST = Arrays.stream(LinuxDistributionUtil.getContextPaths())
            .map(s->s.substring(1))
            .collect(Collectors.toList());

    @Override
    public boolean applies(String pathInContext) {
        return LinuxDistributionUtil.applies(pathInContext);
    }

    @Override
    public List<String> getSuffixes() {
        return SUFFIX_LIST;
    }

    @Override
    public int getExecutionPhase() {
        return 4;
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum) {
        return contribute(baseDir, relativeAnchorPath, anchorChecksum, new EvaluationContext());
    }

    @Override
    public List<ComponentPatternData> contribute(File baseDir, String relativeAnchorPath, String anchorChecksum, EvaluationContext context) {

        try {
            String virtualRootPath = modulateVirtualRootPath(baseDir, relativeAnchorPath, SUFFIX_LIST);

            final String contextSemaphore = getClass().getCanonicalName() + "-" + virtualRootPath ;

            // skip if context is already processed
            if (context.isProcessed(contextSemaphore)) return Collections.emptyList();

            final File distroBaseDir = ".".equals(virtualRootPath) ? baseDir : new File(baseDir, virtualRootPath);
            final LinuxDistributionUtil.LinuxDistro linuxDistro = LinuxDistributionUtil.parseDistro(distroBaseDir);

            if (linuxDistro != null) {

                final String contextRelativePath = FileUtils.asRelativePath(distroBaseDir, new File(baseDir, relativeAnchorPath));

                ComponentPatternData cpd = new ComponentPatternData();

                cpd.set(COMPONENT_NAME, linuxDistro.issue);
                cpd.set(COMPONENT_VERSION, linuxDistro.versionId);

                if (linuxDistro.versionId != null) {
                    cpd.set(COMPONENT_PART, linuxDistro.id + "-" + linuxDistro.versionId);
                } else {
                    // in case the distro instance does not convey sufficient information we fallback to the anchor name
                    cpd.set(COMPONENT_PART, new File(relativeAnchorPath).getName());
                }

                cpd.set(VERSION_ANCHOR, contextRelativePath);
                cpd.set(VERSION_ANCHOR_CHECKSUM, anchorChecksum);

                final String coveredFiled = getSuffixes().stream().collect(Collectors.joining(", "));
                cpd.set(INCLUDE_PATTERN, coveredFiled);

                // also include to shared includes
                cpd.set(SHARED_INCLUDE_PATTERN, coveredFiled);

                cpd.set(KEY_TYPE, ARTIFACT_TYPE_DISTRO);
                cpd.set(KEY_COMPONENT_SOURCE_TYPE, "linux-distro");

                cpd.setExpansionInventorySupplier(() -> createAssetInventory(baseDir, distroBaseDir, linuxDistro));

                context.registerProcessed(contextSemaphore);

                return Collections.singletonList(cpd);
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return Collections.emptyList();
    }

    private Inventory createAssetInventory(File baseDir, File distroBaseDir, LinuxDistributionUtil.LinuxDistro linuxDistro) {

        final String relativePathToDistroRoot = FileUtils.asRelativePath(baseDir, distroBaseDir);

        AssetMetaData assetMetaData = new AssetMetaData();
        assetMetaData.set(KEY_TYPE, "os");

        assetMetaData.set(AssetMetaData.Attribute.ASSET_ID, "OSID-" + relativePathToDistroRoot);
        assetMetaData.set(AssetMetaData.Attribute.NAME, linuxDistro.id);
        assetMetaData.set(AssetMetaData.Attribute.VERSION, linuxDistro.versionId);

        assetMetaData.set("Distro - Id", linuxDistro.id);
        assetMetaData.set("Distro - VersionId", linuxDistro.versionId);
        assetMetaData.set("Distro - CPE", linuxDistro.cpe);
        assetMetaData.set("Distro - Issue", linuxDistro.issue);
        assetMetaData.set("Distro - Version", linuxDistro.version);
        assetMetaData.set("Distro - URL", linuxDistro.url);

        assetMetaData.set(AssetMetaData.Attribute.ASSET_PATH.getKey(), relativePathToDistroRoot);

        Inventory inventory = new Inventory();
        inventory.getAssetMetaData().add(assetMetaData);

        return inventory;
    }

}