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
package org.metaeffekt.core.inventory.processor.report;

import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.metaeffekt.core.inventory.InventoryUtils;
import org.metaeffekt.core.inventory.processor.model.Artifact;
import org.metaeffekt.core.inventory.processor.model.AssetMetaData;
import org.metaeffekt.core.inventory.processor.model.Inventory;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

public class ReportUtils {

    @Setter
    private VelocityContext context;

    private Properties languageProperties;

    /**
     * It is recommended to use Locale.getLanguage().toString() as those abbreviations are used in the
     * getLanguagePropertyFile() method and as the property file names.
     */
    private String lang;

    public void setLang(Locale locale) {
        this.lang = locale.getLanguage();
        Properties props = new Properties();
        InputStream inputStream = this.getClass()
                .getClassLoader()
                .getResourceAsStream("META-INF/templates/lang/" + lang + ".properties");

        try {
            if (inputStream != null) {
                props.load(inputStream);
                languageProperties = props;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean isEmpty(String value) {
        return !StringUtils.isNotBlank(value);
    }

    public boolean notEmpty(String value) {
        return StringUtils.isNotBlank(value);
    }

    public String ratio(long part, long total) {
        return String.format("%.1f", ((double) part) / total);
    }

    public String percent(long part, long total) {
        if (total == 0) return "n/a";
        return String.format(Locale.GERMANY, "%.1f %%", (100d * part) / total);
    }

    // FIXME: move to new InventoryReportAdapter
    /**
     * Returns a list of assets for which the give artifacts are contained in. If there is a primary asset in the list,
     * only the primary is returned. If there is no primary, all related assets are returned.
     *
     * @param artifacts the artifacts for which to find the related assets
     * @param inventory the inventory containing the artifacts and assets
     *
     * @return the set of related assets
     */
    public Set<AssetMetaData> getAssetsForArtifacts(List<Artifact> artifacts, Inventory inventory) {
        final Set<AssetMetaData> assets = InventoryUtils.getAssetsForArtifacts(inventory, new HashSet<>(artifacts));

        if (!assets.isEmpty()) {
            final Set<AssetMetaData> primaryAssets = assets.stream()
                    .filter(a -> a != null && a.isPrimary())
                    .collect(Collectors.toSet());

            if (!primaryAssets.isEmpty()) {
                // return primary assets
                return primaryAssets;
            } else {
                // fallback to assets for artifact if no primary assets could be detected
                return assets;
            }
        }


        return Collections.emptySet();
    }

    public String getText(String key) {
        String value = languageProperties.get(key).toString();
        if (StringUtils.isNotBlank(value)) {
            return evaluate(value);
        }
        return "";
    }

    // TODO: Manually evaluate how expensive this operation is compared to previous report iterations.
    public String evaluate(String template) {
        StringWriter writer = new StringWriter();
        try {
            Velocity.evaluate(context, writer, "TemplateEval", new StringReader(template));
        } catch ( Exception e ) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }
}
