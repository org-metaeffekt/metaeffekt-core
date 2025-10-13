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
package org.metaeffekt.core.inventory.processor.model;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

import static org.metaeffekt.core.inventory.processor.model.Constants.ASTERISK;

/**
 * {@link LicenseMetaData} contains relevant meta data to evaluate notices with regards to the used licenses in a
 * component.
 *
 * @author Karsten Klein
 */
public class LicenseMetaData extends AbstractModelBase {

    // Maximize compatibility with serialized inventories
    private static final long serialVersionUID = 1L;

    /**
     * @deprecated Use retained instead
     */
    @Deprecated
    public static final String SOURCE_CATEGORY_ADDITIONAL = "additional";

    /**
     * @deprecated Use annex instead
     */
    @Deprecated
    public static final String SOURCE_CATEGORY_EXTENDED = "extended";

    public static final String SOURCE_CATEGORY_RETAINED = "retained";
    public static final String SOURCE_CATEGORY_ANNEX = "annex";


    /**
     * Default constructor.
     */
    public LicenseMetaData() {
        super();
    }

    public LicenseMetaData(LicenseMetaData licenseMetaData) {
        super(licenseMetaData);
    }

    public static String deriveComponentFolderName(String componentName) {
        if (componentName == null)
            return null;
        return normalizeId(componentName);
    }

    public static String deriveComponentFolderName(String componentName, String version) {
        if (componentName == null)
            return null;
        if (StringUtils.isEmpty(version))
            return deriveComponentFolderName(componentName);
        return normalizeId(componentName + "-" + version);
    }

    public static String deriveLicenseFolderName(String license) {
        if (license == null)
            return null;
        return normalizeId(license);
    }

    // FIXME: unify with Inventory.removeSpecialCharacters and deriveLicenseId
    public static String normalizeId(String string) {
        String result = string.replace(" ", "-");
        result = result.replace("+", "");
        result = result.replace("!", "");
        result = result.replace("&", "and");
        result = result.replace(",", "-");
        result = result.replace(":", "_");
        result = result.replace(";", "_");
        result = result.replace("/", "_");
        result = result.replace("\\", "_");

        int length = -1;
        while (length != result.length()) {
            length = result.length();
            result = result.replace("__", "_");
            result = result.replace("--", "-");
            result = result.replace("-_", "-");
            result = result.replace("_-", "-");
            result = result.replace("_(", "(");
            result = result.replace("-)", ")");
        }
        return result;
    }

    public String createCompareStringRepresentation() {
        return toString();
    }

    public String deriveLicenseInEffect() {
        String licenseInEffect = getLicenseInEffect();
        if (StringUtils.isEmpty(licenseInEffect)) {
            licenseInEffect = getLicense();
        }
        return licenseInEffect;
    }

    public String deriveQualifier() {
        StringBuilder sb = new StringBuilder(getComponent()).append("-").append(getLicense());
        if (getVersion() != null) {
            if (!ASTERISK.equalsIgnoreCase(getVersion().trim())) {
                sb.append("-").append(getVersion());
            }
        }
        return sb.toString();
    }

    public String get(Attribute attribute, String defaultValue) {
        return get(attribute.getKey(), defaultValue);
    }

    public String get(Attribute attribute) {
        return get(attribute.getKey());
    }

    public String getComment() {
        return get(Attribute.COMMENT);
    }

    public void setComment(String comment) {
        set(Attribute.COMMENT, comment);
    }

    @Deprecated
    public String getCompleteNotice() {
        String notice = get(Attribute.NOTICE);
        if (notice == null) return null;
        StringBuilder sb = new StringBuilder(notice);
        int index = 1;
        while (get("Notice (split-" + index + ")") != null) {
            String s = get("Notice (split-" + index + ")");
            sb.append(s);
            index++;
        }
        return sb.toString();
    }

    public String getComponent() {
        return get(Attribute.COMPONENT);
    }

    public void setComponent(String component) {
        set(Attribute.COMPONENT, component);
    }

    public String getLicense() {
        return get(Attribute.LICENSE);
    }

    public void setLicense(String license) {
        set(Attribute.LICENSE, license);
    }

    public String getLicenseInEffect() {
        return get(Attribute.LICENSE_IN_EFFECT);
    }

    public void setLicenseInEffect(String licenseInEffect) {
        set(Attribute.LICENSE_IN_EFFECT, licenseInEffect);
    }

    @Deprecated
    public String getName() {
        return get(Attribute.LICENSE);
    }

    @Deprecated
    public void setName(String license) {
        set(Attribute.LICENSE, license);
    }

    public String getNotice() {
        return get(Attribute.NOTICE);
    }

    public void setNotice(String notice) {
        set(Attribute.NOTICE, notice);
    }

    public String getSourceCategory() {
        return get(Attribute.SOURCE_CATEGORY);
    }

    public void setSourceCategory(String sourceCategory) {
        set(Attribute.SOURCE_CATEGORY, sourceCategory);
    }

    public String getVersion() {
        return get(Attribute.VERSION);
    }

    public void setVersion(String version) {
        set(Attribute.VERSION, version);
    }

    public boolean isValid() {
        if (StringUtils.isEmpty(getComponent())) return false;
        return !StringUtils.isEmpty(getLicense());
    }

    public void set(Attribute attribute, String value) {
        set(attribute.getKey(), value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder(get(Attribute.LICENSE));
        sb.append('/').append(get(Attribute.COMPONENT));
        sb.append('/').append(get(Attribute.VERSION));
        sb.append('/').append(get(Attribute.LICENSE_IN_EFFECT));
        sb.append('/').append(get(Attribute.NOTICE));
        if (StringUtils.isNotBlank(get(Attribute.SOURCE_CATEGORY))) {
            sb.append('/').append(get(Attribute.SOURCE_CATEGORY));
        }
        if (StringUtils.isNotBlank(get(Attribute.COMMENT))) {
            sb.append('/').append(get(Attribute.COMMENT));
        }
        return sb.toString();
    }

    /**
     * Defines the minimum set of attributes for serialization. The order is not relevant.
     */
    public static ArrayList<String> MIN_ATTRIBUTES = new ArrayList<>();
    static {
        // fix selection and order
        MIN_ATTRIBUTES.add(Attribute.COMPONENT.getKey());
        MIN_ATTRIBUTES.add(Attribute.VERSION.getKey());
        MIN_ATTRIBUTES.add(Attribute.LICENSE.getKey());
        MIN_ATTRIBUTES.add(Attribute.LICENSE_IN_EFFECT.getKey());
        MIN_ATTRIBUTES.add(Attribute.NOTICE.getKey());
    }

    /**
     * Defines the core attributes. Used for logging and ordering.
     */
    public static ArrayList<String> CORE_ATTRIBUTES = new ArrayList<>();
    static {
        // fix selection and order
        CORE_ATTRIBUTES.add(Attribute.COMPONENT.getKey());
        CORE_ATTRIBUTES.add(Attribute.VERSION.getKey());
        CORE_ATTRIBUTES.add(Attribute.LICENSE.getKey());
        CORE_ATTRIBUTES.add(Attribute.LICENSE_IN_EFFECT.getKey());
        CORE_ATTRIBUTES.add(Attribute.SOURCE_CATEGORY.getKey());
        CORE_ATTRIBUTES.add(Attribute.NOTICE.getKey());
        CORE_ATTRIBUTES.add(Attribute.COMMENT.getKey());
    }

    public enum Attribute implements AbstractModelBase.Attribute {
        COMPONENT("Component"),
        VERSION("Version"),
        LICENSE("License"),
        LICENSE_IN_EFFECT("License in Effect"),
        NOTICE("License Notice"),
        COMMENT("Comment"),

        /**
         * The sourceCategory specifies whether source code for the artifacts associated with this component must be included
         * either in the 'software distribution annex' or the 'retained sources' archive. To include the source code in the
         * distribution annex specify the value 'annex'; to include the source code in the additional sources specify
         * 'retained'. Other values are currently not supported. If no information is provided no source code is to
         * be inlcuded in any archive.
         */
        SOURCE_CATEGORY("Source Category");

        private final String key;

        Attribute(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }
}
