/**
 * Copyright 2009-2016 the original author or authors.
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

import org.dom4j.DocumentException;
import org.metaeffekt.core.inventory.processor.model.Inventory;
import org.metaeffekt.core.inventory.processor.model.LicenseMetaData;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class InventoryUpdateExecution {

    private static Map<String, String> DEFAULT_LICENSE_NAME_MAP = new HashMap<String, String>();

    private static Map<String, String> DEFAULT_COMPONENT_NAME_MAP = new HashMap<String, String>();

    static {
        // the inventory should use the names on the right
        DEFAULT_LICENSE_NAME_MAP.put("Jdom License", "JDOM License");
        DEFAULT_LICENSE_NAME_MAP.put("Jtidy License", "JTidy License");
        DEFAULT_LICENSE_NAME_MAP.put("dom4j License (BSD 2.0 +)", "DOM4J License");
        DEFAULT_LICENSE_NAME_MAP.put("dom4j License (BSD 2.0 +)", "DOM4J License");
        DEFAULT_LICENSE_NAME_MAP.put("dom4j License (BSD 2.0 +)", "DOM4J License");
        DEFAULT_LICENSE_NAME_MAP.put("Tanuki Java Service Wrapper Development Software License",
                "Tanuki Development Software License Agreement 1.1");
        DEFAULT_LICENSE_NAME_MAP.put("Tanuki Java Service Wrapper Development Software License",
                "Tanuki Development Software License Agreement 1.1");
        DEFAULT_LICENSE_NAME_MAP.put("COMMON DEVELOPMENT AND DISTRIBUTION LICENSE (CDDL) Version 1.0",
                "Common Development and Distribution License (CDDL) 1.0");
        DEFAULT_LICENSE_NAME_MAP.put("Common Development and Distribution (CDDL) 1.1 License",
                "Common Development and Distribution License (CDDL) 1.1");

        // the inventory must use the names on the left
        mapComponent("Apache Aries", "Apache Blueprint Aries");
        mapComponent("Tanuki Java Service Wrapper", "Java Service Wrapper");
        mapComponent("Open eHealth Foundation Integration Platform", "IPF");
        mapComponent("Spring Framework", "Spring Framework - springframework");
        mapComponent("Databene", "databene benerator - benerator");
        mapComponent("Restlet", "Noelios Restlet Engine");
        mapComponent("Eclipse BIRT",
                "Eclipse BIRT Project - Business Intelligence and Reporting Tools");
        mapComponent("Ehcache", "ehcache - ehcache");
        mapComponent("Ehcache Spring Annotations", "ehcache-spring-annotations");
        mapComponent("JBoss jBPM", "jBpm.org - java Business Process Mgmt - f) jBPM jPDL 3");
        mapComponent("DD Roundies", "dd_roundies");

        mapComponent("Bootstrap", "bootstrap");

        mapComponent("jQuery", "JQuery");
        mapComponent("jQuery", "JQuery - jquery");
        mapComponent("jQuery", "jquery");
        mapComponent("jQuery Autocomplete", "jquery-autocomplete");
        mapComponent("jQuery BGIFrame", "jquery.bgiframe");
        mapComponent("jQuery Cookie", "jquery-cookie");
        mapComponent("jQuery Cookie", "jquery.cookie");
        mapComponent("jQuery Cookie Plugin", "JQuery Cookie Plugin");
        mapComponent("jQuery Dimensions", "jquery.dimensions");
        mapComponent("jQuery Format", "jquery-format");
        mapComponent("jQuery Hashchange", "jquery-hashchange");
        mapComponent("jQuery JS", "jqueryjs");

        mapComponent("jQuery UI", "jquery-ui");

        mapComponent("jQuery Outside-Events", "jquery-outside-events");
        mapComponent("jQuery ScrollTo Plugin", "jQuery Plugins - ScrollTo");

        mapComponent("jQuery Timepicker-Addon", "jQuery-Timepicker-Addon");

        mapComponent("jQuery Date Format", "jquery-dateFormat");
        mapComponent("jQuery Date Format", "jquery-dateFormat");
        mapComponent("jQuery BGIFrame Plugin", "jQuery Plugin: bgiframe");

        mapComponent("jQuery FCKEditor Plugin", "jquery-fckeditor-plugin");
        mapComponent("jQuery Hijack", "jquery-hijack");
        mapComponent("jQuery Tiny Scrollbar", "jquery-tinyscrollbar");
    }

    private static void mapComponent(String newName, String obsoleteName) {
        DEFAULT_COMPONENT_NAME_MAP.put(obsoleteName, newName);
    }

    public static void main(String[] args) throws IOException, DocumentException {
        File folder = new File("C:/dev/workspace/artifact-inventory-trunk/thirdparty/src/main/resources/META-INF");
        File targetFolder = new File("target/inventory-update-execution");
        File inputFolder = new File("C:/dev/workspace/artifact-inventory-trunk/thirdparty/input");

        File globalFile = new File(folder, "artifact-inventory-thirdparty-2015-Q1.xls");
        File targetFile = new File(targetFolder, "artifact-inventory-thirdparty-Q3-2013.xls");
        File protexFile = new File(inputFolder, "globalInventoryReport-2015-01-01_KKL.xls");

        Properties properties = new Properties();
        properties.setProperty(MavenCentralUpdateProcessor.GROUPID_EXCLUDE_PATTERNS, "-nothing-");
        properties.setProperty(MavenCentralUpdateProcessor.ARTIFACTID_EXCLUDE_PATTERNS, "ae-.*,ipf-.*,cdm-.*,pxs-.*,epr-.*,bas-.*");
//        properties.setProperty(MavenCentralUpdateProcessor.PROXY_HOST, "host");
//        properties.setProperty(MavenCentralUpdateProcessor.PROXY_PORT, "port");
        properties.setProperty(MergeProtexInventoryProcessor.PROTEX_INVENTORY_PATH, protexFile.getAbsolutePath());

        InventoryUpdate inventoryUpdate = new InventoryUpdate();
        inventoryUpdate.setSourceInventoryFile(globalFile);
        inventoryUpdate.setTargetInventoryFile(targetFile);

        inventoryUpdate.setComponentNameMap(DEFAULT_COMPONENT_NAME_MAP);
        inventoryUpdate.setLicenseNameMap(DEFAULT_LICENSE_NAME_MAP);

        List<InventoryProcessor> inventoryProcessors = new ArrayList<InventoryProcessor>();
//        inventoryProcessors.add(new MavenCentralUpdateProcessor(properties));
        inventoryProcessors.add(new MergeProtexInventoryProcessor(properties));
        inventoryProcessors.add(new CleanupInventoryProcessor(properties));
        inventoryProcessors.add(new UpdateVersionRecommendationProcessor(properties));

        inventoryUpdate.setInventoryProcessors(inventoryProcessors);

        Inventory inventory = inventoryUpdate.process();

        // TODO: move to a processor
        createLicenseFolders(inventory);
    }

    private static void createLicenseFolders(Inventory inventory) {
        File licensePath = new File("C:/dev/workspace/artifact-inventory-trunk/thirdparty/src/main/resources/META-INF/licenses");

        List<String> licenses = inventory.evaluateLicenses(true);
        for (String license : licenses) {
            String touchedLicense = LicenseMetaData.deriveLicenseFolderName(license);

            File file = new File(licensePath, touchedLicense);
            file.mkdirs();

            if (!file.exists()) {
                System.err.println("Cannot create folder for: " + file);
            }

            // and now the derived licenses
            Set<String> derivedLicenses = inventory.
                    listDerivedLicenses(license, touchedLicense);

            for (String derivedPath : derivedLicenses) {
                File derivedLicense = new File(licensePath, derivedPath);
                if (!derivedLicense.exists()) {
                    ;
                    System.err.println("No derived license file in folder '" + derivedPath + "'");
                }
            }
        }
    }

}
