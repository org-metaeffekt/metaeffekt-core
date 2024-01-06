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

    public boolean isDriver() {
        return isOrHasParent(DRIVER);
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
    public static final ArtifactType KEYBOARD = new ArtifactType("keyboard", "keyboards");
    public static final ArtifactType MOUSE = new ArtifactType("mouse", "mice, trackballs, touchpads");
    public static final ArtifactType HUMAN_INTERFACE = new ArtifactType("human interface", "keyboards, mice, gamepads, joysticks", KEYBOARD, MOUSE);
    public static final ArtifactType SCANNER = new ArtifactType("scanner", "barcode scanners, fingerprint scanners, biometric scanners");
    public static final ArtifactType INPUT_DEVICE = new ArtifactType("input device", "top-level category for input devices", HUMAN_INTERFACE, SCANNER);
    // OUTPUT_DEVICE
    public static final ArtifactType AUDIO_HARDWARE = new ArtifactType("audio hardware", "dacs, audio interfaces, speakers");
    public static final ArtifactType IMAGING_HARDWARE = new ArtifactType("imaging hardware", "cameras, video capture cards");
    public static final ArtifactType PRINTER = new ArtifactType("printer", null);
    public static final ArtifactType DISPLAY = new ArtifactType("display", null);
    public static final ArtifactType PROJECTOR = new ArtifactType("projector", null);
    public static final ArtifactType MULTIMEDIA_OUTPUT_DEVICE = new ArtifactType("multimedia output device", "devices that contain both sound and imaging hardware");
    public static final ArtifactType OUTPUT_DEVICE = new ArtifactType("output device", "top-level category for output devices", AUDIO_HARDWARE, IMAGING_HARDWARE, PRINTER, DISPLAY, PROJECTOR, MULTIMEDIA_OUTPUT_DEVICE);
    // other hardware
    public static final ArtifactType DATA_STORAGE = new ArtifactType("data storage", "ssds, hard drives, usb drives");
    public static final ArtifactType DEVICE_CONNECTOR = new ArtifactType("device connector", "cables, connectors");
    public static final ArtifactType SECURITY_TOKEN = new ArtifactType("security token", "nfc tokens");
    public static final ArtifactType SECURITY_HARDWARE = new ArtifactType("security hardware", "top-level category for security hardware. does not include elements from category 'scanner'", SECURITY_TOKEN);
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
            DATA_STORAGE, DEVICE_CONNECTOR, SECURITY_HARDWARE, SENSOR, PROCESSING_CORE, EXTENSION_MODULE, POWER_SUPPLY,
            TEMPERATURE_CONTROL, AESTHETIC_HARDWARE, TRACKING_HARDWARE, WEARABLE
    );

    public static final ArtifactType COMPUTER_DRIVER = new ArtifactType("computer driver", "System devices, base system hardware");
    public static final ArtifactType FIRMWARE_DRIVER = new ArtifactType("firmware driver", "System firmware, BIOS, UEFI");
    public static final ArtifactType SOFTWARE_DEVICE_DRIVER = new ArtifactType("software device driver", "Software-emulated devices, Software-based system components");
    public static final ArtifactType SYSTEM_DEVICE_DRIVER = new ArtifactType("system device driver", "Base system devices, chipset");
    public static final ArtifactType IMAGING_DRIVER = new ArtifactType("imaging driver", "Cameras, scanners");
    public static final ArtifactType KEYBOARD_DRIVER = new ArtifactType("keyboard driver", "Keyboard devices");
    public static final ArtifactType MOUSE_DRIVER = new ArtifactType("mouse driver", "Mouse devices, trackpads, pointing devices");
    public static final ArtifactType INPUT_DEVICE_DRIVER = new ArtifactType("input device driver", "Keyboard, mouse, touchpad, touchscreen, gamepad, joystick", KEYBOARD_DRIVER, MOUSE_DRIVER);
    public static final ArtifactType AUDIO_DRIVER = new ArtifactType("audio driver", "Audio devices, microphones, speakers");
    public static final ArtifactType MULTIMEDIA_DRIVER = new ArtifactType("multimedia driver", "Audio, video, sound, imaging devices");
    public static final ArtifactType DISPLAY_ADAPTERS_DRIVER = new ArtifactType("display adapters driver", "Graphics hardware, display output");
    public static final ArtifactType DISPLAY_DRIVER = new ArtifactType("display driver", "Display monitors, screens");
    public static final ArtifactType NETWORK_DRIVER = new ArtifactType("network driver", "Ethernet adapters, Wi-Fi cards");
    public static final ArtifactType PORT_DRIVER = new ArtifactType("port driver", "Serial and parallel ports");
    public static final ArtifactType PRINT_QUEUES_DRIVER = new ArtifactType("print queue driver", "Print job management");
    public static final ArtifactType PRINTER_DRIVER = new ArtifactType("printer driver", "Printer devices");
    public static final ArtifactType PROCESSOR_DRIVER = new ArtifactType("processor driver", "CPU devices");
    public static final ArtifactType SECURITY_DEVICE_DRIVER = new ArtifactType("security device driver", "Encryption, biometric devices");
    public static final ArtifactType SOUND_VIDEO_DRIVER = new ArtifactType("sound, video driver", "Sound cards, video cards, game controllers");
    public static final ArtifactType BLUETOOTH_DRIVER = new ArtifactType("Bluetooth driver", "Bluetooth devices, data transfer, pairing");
    public static final ArtifactType DISK_DRIVES_DRIVER = new ArtifactType("disk drives driver", "Hard drives, SSDs");
    public static final ArtifactType USB_DRIVER = new ArtifactType("Universal Serial Bus controller driver", "USB controllers, hubs, devices");
    public static final ArtifactType STORAGE_DRIVER = new ArtifactType("storage driver", "Disk controllers, storage controllers, usb", DISK_DRIVES_DRIVER, USB_DRIVER);
    public static final ArtifactType POWER_DRIVER = new ArtifactType("power driver", "Power management, battery management");
    public static final ArtifactType CARD_DRIVER = new ArtifactType("card driver", "Expansion cards, PCI, PCIe, AGP, ISA, PCMCIA, ExpressCard, Thunderbolt");

    public static final ArtifactType DRIVER = new ArtifactType("driver", "top-level category for drivers",
            COMPUTER_DRIVER, FIRMWARE_DRIVER, SOFTWARE_DEVICE_DRIVER, SYSTEM_DEVICE_DRIVER, IMAGING_DRIVER, INPUT_DEVICE_DRIVER,
            AUDIO_DRIVER, DISPLAY_ADAPTERS_DRIVER, DISPLAY_DRIVER, NETWORK_DRIVER, PORT_DRIVER, PRINT_QUEUES_DRIVER, PRINTER_DRIVER,
            PROCESSOR_DRIVER, SECURITY_DEVICE_DRIVER, SOUND_VIDEO_DRIVER, BLUETOOTH_DRIVER, STORAGE_DRIVER, POWER_DRIVER
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

    // {4d36e97e-e325-11ce-bfc1-08002be10318} Unknown: Other Devices
    public static final ArtifactType UNKNOWN = new ArtifactType("unknown", "types that are not covered by the other categories or could not be identified");

    public static final List<ArtifactType> ARTIFACT_TYPES = Collections.unmodifiableList(
            Stream.of(
                            CATEGORY_HARDWARE,
                            CATEGORY_SOFTWARE_LIBRARY,
                            OPERATING_SYSTEM,
                            DRIVER,
                            BIOS,
                            FILE
                    ).flatMap(ArtifactType::flattenArtifactType)
                    .collect(Collectors.toList())
    );

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

    public static String toMarkdownString() {
        final StringBuilder sb = new StringBuilder();
        final Set<ArtifactType> coveredTypes = new HashSet<>();
        for (ArtifactType artifactType : ARTIFACT_TYPES) {
            recurseMarkdownString(artifactType, sb, 0, coveredTypes);
        }
        return sb.toString();
    }

    public static void recurseMarkdownString(ArtifactType artifactType, StringBuilder sb, int level, Set<ArtifactType> coveredTypes) {
        if (coveredTypes.contains(artifactType)) {
            return;
        } else {
            coveredTypes.add(artifactType);
        }
        final int indentation = Math.max(0, level);
        for (int i = 0; i < indentation; i++) {
            sb.append("  ");
        }
        sb.append("-").append(" **").append(artifactType.getCategory()).append("**");
        if (artifactType.getDescription() != null) {
            sb.append(" (").append(artifactType.getDescription()).append(")");
        }
        sb.append("\n");
        for (ArtifactType child : artifactType.getSubcategories()) {
            recurseMarkdownString(child, sb, level + 1, coveredTypes);
        }
    }

}
