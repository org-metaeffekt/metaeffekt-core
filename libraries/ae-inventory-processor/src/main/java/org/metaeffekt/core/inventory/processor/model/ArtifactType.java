/*
 * Copyright 2009-2022 the original author or authors.
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

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ArtifactType {
    private final String category;
    private final String description;
    private final ArtifactType[] subcategories;
    private ArtifactType parent;

    public ArtifactType(String category, String description, ArtifactType... subcategories) {
        this.category = category;
        this.description = description;
        this.subcategories = subcategories;
        for (ArtifactType subcategory : subcategories) {
            subcategory.parent = this;
        }
    }

    public String getCategory() {
        return category;
    }

    public String getDescription() {
        return description;
    }

    public ArtifactType[] getSubcategories() {
        return subcategories;
    }

    public ArtifactType getParent() {
        return parent;
    }

    private boolean isRoot() {
        return parent == null;
    }

    public ArtifactType getRoot() {
        if (isRoot()) {
            return this;
        } else {
            return parent.getRoot();
        }
    }

    public boolean isHardware() {
        return isOrHasParent(CATEGORY_HARDWARE);
    }

    public boolean isOrHasParent(ArtifactType artifactType) {
        if (this == artifactType) {
            return true;
        } else if (isRoot()) {
            return false;
        } else {
            return parent.isOrHasParent(artifactType);
        }
    }

    // CATEGORY_HARDWARE
    // APPLIANCE
    public static final ArtifactType NETWORKING_HARDWARE = new ArtifactType("networking hardware", "routers, switches, modems");
    public static final ArtifactType BOARD = new ArtifactType("board", "motherboards, development boards");
    public static final ArtifactType APPLIANCE = new ArtifactType("appliance", "desktops, laptops, servers", NETWORKING_HARDWARE, BOARD);
    // CONTROLLER
    public static final ArtifactType EMBEDDED_SYSTEM = new ArtifactType("embedded system", "microcontrollers, iot devices, programmable logic controller");
    public static final ArtifactType STORAGE_CONTROLLER = new ArtifactType("storage controller", "RAID controllers");
    public static final ArtifactType CONTROLLER = new ArtifactType("controller", "top-level category for controllers", EMBEDDED_SYSTEM, STORAGE_CONTROLLER);
    // INPUT_DEVICE
    public static final ArtifactType HUMAN_INTERFACE = new ArtifactType("human interface", "keyboards, mice");
    public static final ArtifactType SCANNER = new ArtifactType("scanner", "barcode scanners, fingerprint scanners, biometric scanners");
    public static final ArtifactType INPUT_DEVICE = new ArtifactType("input device", "top-level category for input devices", HUMAN_INTERFACE, SCANNER);
    // OUTPUT_DEVICE
    public static final ArtifactType SOUND_HARDWARE = new ArtifactType("sound hardware", "dacs, audio interfaces, speakers");
    public static final ArtifactType IMAGING_HARDWARE = new ArtifactType("imaging hardware", "cameras, video capture cards");
    public static final ArtifactType PRINTER = new ArtifactType("printer", null);
    public static final ArtifactType DISPLAY = new ArtifactType("display", null);
    public static final ArtifactType PROJECTOR = new ArtifactType("projector", null);
    public static final ArtifactType OUTPUT_DEVICE = new ArtifactType("output device", "top-level category for output devices", SOUND_HARDWARE, IMAGING_HARDWARE, PRINTER, DISPLAY, PROJECTOR);
    // other hardware
    public static final ArtifactType DATA_STORAGE = new ArtifactType("data storage", "ssds, hard drives, usb drives");
    public static final ArtifactType DEVICE_CONNECTOR = new ArtifactType("device connector", "cables, connectors");
    public static final ArtifactType SECURITY_TOKEN = new ArtifactType("security token", "nfc tokens");
    public static final ArtifactType SENSOR = new ArtifactType("sensor", "measurement devices, temperature sensors, motion sensors, anemometers");
    public static final ArtifactType PROCESSING_CORE = new ArtifactType("processing core", "CPUs, GPUs, TPUs");
    public static final ArtifactType EXTENSION_MODULE = new ArtifactType("extension module", "sound cards, wi-fi cards, bluetooth cards, RAM");
    public static final ArtifactType POWER_SUPPLY = new ArtifactType("power supply", "batteries, adapters, solar chargers, uninterruptible power supplies, portable power");
    public static final ArtifactType TEMPERATURE_CONTROL = new ArtifactType("temperature control", "fans, liquid cooling systems, heat sinks");
    public static final ArtifactType AESTHETIC_HARDWARE = new ArtifactType("aesthetic hardware", "lights, RGB mouse-pads, custom case panels, LED fans");
    public static final ArtifactType TRACKING_HARDWARE = new ArtifactType("tracking hardware", "GPS modules, bluetooth trackers");
    public static final ArtifactType WEARABLE = new ArtifactType("wearable", "smart watches, fitness trackers, smart glasses, smart rings, smart clothing");

    public static final ArtifactType CATEGORY_HARDWARE = new ArtifactType("hardware", "top-level category for hardware; unclassified or specialized hardware",
            // categories
            APPLIANCE, CONTROLLER, INPUT_DEVICE, OUTPUT_DEVICE,
            // other
            DATA_STORAGE, DEVICE_CONNECTOR, SECURITY_TOKEN, SENSOR, PROCESSING_CORE, EXTENSION_MODULE, POWER_SUPPLY,
            TEMPERATURE_CONTROL, AESTHETIC_HARDWARE, TRACKING_HARDWARE, WEARABLE
    );

    // CATEGORY_SOFTWARE_LIBRARY
    public static final ArtifactType LINUX_PACKAGE = new ArtifactType("package", "linux package");
    public static final ArtifactType PYTHON_MODULE = new ArtifactType("python-module", "python module");
    public static final ArtifactType NODEJS_MODULE = new ArtifactType("nodejs-module", "nodejs module");

    public static final ArtifactType CATEGORY_SOFTWARE_LIBRARY = new ArtifactType("software library", null,
            LINUX_PACKAGE, PYTHON_MODULE, NODEJS_MODULE
    );

    public static final ArtifactType OPERATING_SYSTEM = new ArtifactType("operating system", null);
    public static final ArtifactType BIOS = new ArtifactType("bios", null);
    public static final ArtifactType FILE = new ArtifactType("file", null);

    public static final List<ArtifactType> ARTIFACT_TYPES = Collections.unmodifiableList(
            Stream.of(
                            CATEGORY_HARDWARE,
                            CATEGORY_SOFTWARE_LIBRARY,
                            OPERATING_SYSTEM,
                            BIOS,
                            FILE
                    ).flatMap(ArtifactType::flattenArtifactType)
                    .collect(Collectors.toList())
    );

    public static String toMarkdownString() {
        final StringJoiner joiner = new StringJoiner("\n");
        for (ArtifactType artifactType : ARTIFACT_TYPES) {
            joiner.add((artifactType.getParent() != null ? (artifactType.getParent() != null && artifactType.getParent().getParent() != null ? "    - " : "  - ") : "- ") + "**" + artifactType.getCategory() + "**" + (artifactType.getDescription() != null ? " (" + artifactType.getDescription() + ")" : ""));
        }
        return joiner.toString();
    }

    private static Stream<ArtifactType> flattenArtifactType(ArtifactType artifactType) {
        return Stream.concat(
                Stream.of(artifactType),
                Arrays.stream(artifactType.getSubcategories()).flatMap(ArtifactType::flattenArtifactType)
        );
    }

    public static Optional<ArtifactType> findType(String category) {
        return ARTIFACT_TYPES.stream()
                .filter(artifactType -> artifactType.getCategory().equals(category))
                .findFirst();
    }
}
