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

public final class Constants {

    public static final String ASTERISK = "*";
    public static final String VERSION_PLACHOLDER_PREFIX = "${";
    public static final String VERSION_PLACHOLDER_SUFFIX = "}";
    public static final String DOT = ".";
    public static final String SLASH = "/";
    public static final String SPACE = " ";
    public static final String UNDERSCORE = "_";
    public static final String HYPHEN = "-";
    public static final String DOT_SLASH = DOT + SLASH;

    public static final String STRING_EMPTY = "";
    public static final String STRING_TRUE = Boolean.TRUE.toString();
    public static final String STRING_FALSE = Boolean.FALSE.toString();

    public static final char DELIMITER_COLON = ':';
    public static final char DELIMITER_DASH = '-';
    public static final char DELIMITER_PIPE = '|';
    public static final char DELIMITER_COMMA = ',';
    public static final String DELIMITER_NEWLINE = String.format("%n");

    /**
     * Support to mark artifacts as matched by wildcard. This is usually transient information. The wildcard information
     * is lost, when resolving the version. Therefore the fact that an artifact was matched using wildcards is held
     * using this key.
     */
    public static final String KEY_WILDCARD_MATCH = "WILDCARD-MATCH";

    public static final String KEY_DERIVED_LICENSE_PACKAGE = "Specified Package License";
    public static final String KEY_DOCUMENTATION_PATH_PACKAGE = "Package Documentation Path";
    public static final String KEY_LICENSE_PATH_PACKAGE = "Package License Path";
    public static final String KEY_GROUP_PACKAGE = "Package Group";
    public static final String KEY_STATUS_PACKAGE = "Package Status";

    public static final String KEY_HASH_SHA1 = "Hash (SHA-1)";
    public static final String KEY_HASH_SHA256 = "Hash (SHA-256)";

    /**
     * Organization key. We stick to the terminology of maven; in other context this is the vendor (CVE) or
     * supplier (CycloneDX).
     */
    public static final String KEY_ORGANIZATION = "Organization";

    /**
     * Organization URL key. Maven uses two distinct attributes for an organization.
     */
    public static final String KEY_ORGANIZATION_URL = "Organization URL";

    public static final String KEY_SUMMARY = "Summary";
    public static final String KEY_DESCRIPTION = "Description";
    public static final String KEY_ARCHITECTURE = "Architecture";
    public static final String KEY_TYPE = "Type";
    public static final String KEY_COMPONENT_SOURCE_TYPE = "Component Source Type";
    public static final String KEY_NO_MATCHING_FILE = "No Matching File";
    public static final String KEY_SOURCE_PROJECT = "Source Project";

    public static final String KEY_CONTAINER = "Container";
    public static final String KEY_ISSUE = "Issue";

    public static final String ARTIFACT_TYPE_PACKAGE = "package";
    public static final String ARTIFACT_TYPE_MODULE = "module";
    public static final String ARTIFACT_TYPE_FILE = "file";

    @Deprecated
    public static final String ARTIFACT_TYPE_NODEJS_MODULE = "nodejs-module";

    public static final String ARTIFACT_TYPE_WEB_MODULE = "web-module";

    public static final String KEY_PATH_IN_ASSET = "Path in Asset";

    public static final String KEY_CHECKSUM = "Checksum";

    public static final String MARKER_CROSS = "x";

    public static final String MARKER_CONTAINS = "c";

    public static final String OS_RELEASE = "os-release";

    public static final String LSB_RELEASE = "lsb-release";

    public static final String DEBIAN_VERSION = "debian-version";

    public static final String REDHAT_RELEASE = "redhat-release";

    public static final String CENTOS_RELEASE = "centos-release";

    public static final String SYSTEM_RELEASE = "system-release";

    public static final String PACKAGE_JSON = "package.json";

    public static final String PACKAGE_LOCK_JSON = "package-lock.json";

    public static final String COMPOSER_JSON = "composer.json";

    public static final String BOWER_JSON = "bower.json";

    public static final String DOT_BOWER_JSON = DOT + BOWER_JSON;

    public static final String DOT_PACKAGE_LOCK_JSON = DOT + PACKAGE_LOCK_JSON;

    protected Constants() {
    }

}
