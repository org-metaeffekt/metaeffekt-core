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
package org.metaeffekt.core.maven.inventory.extractor.windows;

import org.metaeffekt.core.inventory.processor.model.ArtifactType;

/**
 * Enum that lists all PNPClass values and their corresponding ClassGuid values.<br>
 * These values are mapped to the most fitting hardware category.
 */
public enum WindowsPnpClassGuid {
    // https://www.lifewire.com/device-class-guids-for-most-common-types-of-hardware-2619208
    // https://learn.microsoft.com/en-us/windows-hardware/drivers/install/system-defined-device-setup-classes-available-to-vendors
    BATTERY("Battery", "72631e54-78a4-11d0-bcf7-00aa00b7b32a", ArtifactType.POWER_SUPPLY, ArtifactType.POWER_DRIVER), // UPSs and other battery devices
    BIOMETRIC("Biometric", "53d29ef7-377c-4d14-864b-eb3a85769359", ArtifactType.SCANNER, ArtifactType.SECURITY_DEVICE_DRIVER), // Biometric-based devices
    BLUETOOTH("Bluetooth", "e0cbf06c-cd8b-4647-bb8a-263b43f0f974", ArtifactType.EXTENSION_MODULE, ArtifactType.BLUETOOTH_DRIVER), // Bluetooth devices
    CAMERA("Camera", "ca3e7ab9-b4c3-4ae6-8251-579ef933890f", ArtifactType.IMAGING_HARDWARE, ArtifactType.IMAGING_DRIVER), // Camera devices
    CDROM("CDROM", "4d36e965-e325-11ce-bfc1-08002be10318", ArtifactType.DATA_STORAGE, ArtifactType.AUDIO_DRIVER), // CD-ROM drives
    DISKDRIVE("DiskDrive", "4d36e967-e325-11ce-bfc1-08002be10318", ArtifactType.DATA_STORAGE, ArtifactType.STORAGE_DRIVER), // Hard drives
    DISPLAY("Display", "4d36e968-e325-11ce-bfc1-08002be10318", ArtifactType.DISPLAY_DRIVER, ArtifactType.DISPLAY_DRIVER), // Video adapters; display drivers and video miniport drivers
    EXTENSION("Extension", "e2f84ce7-8efa-411c-aa69-97454ca4cb57", ArtifactType.EXTENSION_MODULE, ArtifactType.SOFTWARE_DEVICE_DRIVER), // Devices requiring customizations
    FDC("FDC", "4d36e969-e325-11ce-bfc1-08002be10318", ArtifactType.STORAGE_CONTROLLER, ArtifactType.STORAGE_DRIVER), // floppy disk drive controllers
    FLOPPYDISK("FloppyDisk", "4d36e980-e325-11ce-bfc1-08002be10318", ArtifactType.DATA_STORAGE, ArtifactType.STORAGE_DRIVER), // Floppy drives
    HDC("HDC", "4d36e96a-e325-11ce-bfc1-08002be10318", ArtifactType.STORAGE_CONTROLLER, ArtifactType.STORAGE_DRIVER), // hard disk controllers, including ATA/ATAPI controllers but not SCSI and RAID disk controllers
    HIDCLASS("HIDClass", "745a17a0-74d3-11d0-b6fe-00a0c90f57da", ArtifactType.INPUT_DEVICE, ArtifactType.INPUT_DEVICE_DRIVER), // interactive input devices that are operated by the system-supplied HID class driver. This includes USB devices that comply with the USB HID Standard and non-USB devices that use a HID minidriver. For more information, see HIDClass Device Setup Class. (See also the Keyboard or Mouse classes later in this list.)
    IEEE1394("1394", "6bdd1fc1-810f-11d0-bec7-08002be2092f", ArtifactType.CONTROLLER, ArtifactType.DRIVER), // IEEE 1394 host controller
    IMAGE("Image", "6bdd1fc6-810f-11d0-bec7-08002be2092f", ArtifactType.IMAGING_HARDWARE, ArtifactType.IMAGING_DRIVER), // Cameras and scanners
    INFRARED("Infrared", "6bdd1fc5-810f-11d0-bec7-08002be2092f", ArtifactType.NETWORKING_HARDWARE, ArtifactType.DRIVER), // Infrared devices, Serial-IR and Fast-IR NDIS miniports
    KEYBOARD("Keyboard", "4d36e96b-e325-11ce-bfc1-08002be10318", ArtifactType.KEYBOARD, ArtifactType.KEYBOARD_DRIVER), // Keyboards
    MEDIUMCHANGER("MediumChanger", "ce5939ae-ebde-11d0-b181-0000f8753ec4", ArtifactType.STORAGE_CONTROLLER, ArtifactType.STORAGE_DRIVER), // SCSI media changer devices
    MTD("MTD", "4d36e970-e325-11ce-bfc1-08002be10318", ArtifactType.DATA_STORAGE, ArtifactType.STORAGE_DRIVER), // Memory devices (e.g., flash memory cards)
    MODEM("Modem", "4d36e96d-e325-11ce-bfc1-08002be10318", ArtifactType.NETWORKING_HARDWARE, ArtifactType.NETWORK_DRIVER), // Modems
    MONITOR("Monitor", "4d36e96e-e325-11ce-bfc1-08002be10318", ArtifactType.DISPLAY, ArtifactType.DISPLAY_DRIVER), // Monitors
    MOUSE("Mouse", "4d36e96f-e325-11ce-bfc1-08002be10318", ArtifactType.MOUSE, ArtifactType.MOUSE_DRIVER), // Mice and pointing devices
    MULTIFUNCTION("Multifunction", "4d36e971-e325-11ce-bfc1-08002be10318", ArtifactType.APPLIANCE, ArtifactType.DRIVER), // Combo cards (e.g., PCMCIA modem)
    MEDIA("Media", "4d36e96c-e325-11ce-bfc1-08002be10318", ArtifactType.MULTIMEDIA_OUTPUT_DEVICE, ArtifactType.MULTIMEDIA_DRIVER), // Audio and video devices
    MULTIPORTSERIAL("MultiportSerial", "50906cb8-ba12-11d1-bf5d-0000f805f530", ArtifactType.EXTENSION_MODULE, ArtifactType.CARD_DRIVER), // multiport serial cards, not peripheral devices that connect to its ports
    NET("Net", "4d36e972-e325-11ce-bfc1-08002be10318", ArtifactType.NETWORK_DRIVER, ArtifactType.NETWORK_DRIVER), // network adapter drivers
    NETCLIENT("NetClient", "4d36e973-e325-11ce-bfc1-08002be10318", ArtifactType.NETWORKING_HARDWARE, ArtifactType.NETWORK_DRIVER), // network and/or print providers
    NETSERVICE("NetService", "4d36e974-e325-11ce-bfc1-08002be10318", ArtifactType.NETWORKING_HARDWARE, ArtifactType.NETWORK_DRIVER), // network services, such as redirectors and servers
    PORTS("Ports", "4d36e978-e325-11ce-bfc1-08002be10318", ArtifactType.DEVICE_CONNECTOR, ArtifactType.PORT_DRIVER), // Serial and parallel ports
    PRINTER("Printer", "4d36e979-e325-11ce-bfc1-08002be10318", ArtifactType.PRINTER, ArtifactType.PRINTER_DRIVER), // Printers
    PPNPPRINTERS("PNPPrinters", "4658ee7e-f050-11d1-b6bd-00c04fa372a7", ArtifactType.PRINTER_DRIVER, ArtifactType.PRINTER_DRIVER), // SCSI/1394-enumerated printers, but also Bus-specific class drivers, I will map this to printer drivers, as this is what I mostly found on the internet.
    PROCESSOR("Processor", "50127dc3-0f36-415e-a6cc-4cb3be910b65", ArtifactType.PROCESSING_CORE, ArtifactType.PROCESSOR_DRIVER), // Processor types
    SCS_ADAPTER("SCSIAdapter", "4d36e97b-e325-11ce-bfc1-08002be10318", ArtifactType.STORAGE_CONTROLLER, ArtifactType.DISK_DRIVES_DRIVER), // SCSI and RAID controllers
    SECURITYDEVICES("Securitydevices", "d94ee5d8-d189-4994-83d2-f68d7d41b0e6", ArtifactType.SECURITY_HARDWARE, ArtifactType.SECURITY_DEVICE_DRIVER), // Trusted Platform Module chips; crypto-processor that helps with generating, storing, and limiting use of cryptographic keys
    SENSOR("Sensor", "5175d334-c371-4806-b3ba-71fd53c9258d", ArtifactType.SENSOR, ArtifactType.INPUT_DEVICE_DRIVER), // Sensor and location devices
    SMARTCARDREADER("SmartCardReader", "50dd5230-ba8a-11d1-bf5d-0000f805f530", ArtifactType.SECURITY_HARDWARE, ArtifactType.SECURITY_DEVICE_DRIVER), // Smart card readers
    VOLUME("Volume", "71a27cdd-812a-11d0-bec7-08002be2092f", ArtifactType.DATA_STORAGE, ArtifactType.STORAGE_DRIVER), // Storage volumes
    SYSTEM("System", "4d36e97d-e325-11ce-bfc1-08002be10318", ArtifactType.BOARD, ArtifactType.SYSTEM_DEVICE_DRIVER), // This class includes HALs, system buses, system bridges, the system ACPI driver, and the system volume manager driver.
    TAPEDRIVE("TapeDrive", "6d807884-7d21-11cf-801c-08002be10318", ArtifactType.DATA_STORAGE, ArtifactType.STORAGE_DRIVER), // Tape drives
    USB("USB", "36fc9e60-c465-11cf-8056-444553540000", ArtifactType.STORAGE_CONTROLLER, ArtifactType.USB_DRIVER), // USB host controllers and USB hubs, but not USB peripherals
    USBDEVICE("USBDevice", "88bae032-5a81-49f0-bc3d-a4ff138216d6", ArtifactType.DATA_STORAGE, ArtifactType.USB_DRIVER), // USB devices that don't belong to another class
    WPD("WPD", "eec5ad98-8080-425f-922a-dabf3de3f69a", ArtifactType.APPLIANCE, ArtifactType.COMPUTER_DRIVER), // Windows Portable Devices

    AUDIO_PROCESSING_OBJECT("AudioProcessingObject", "5989fce8-9cd0-467d-8a6a-5419e31529d4", ArtifactType.AUDIO_HARDWARE, ArtifactType.AUDIO_DRIVER), // audio processing objects (APOs) https://learn.microsoft.com/en-us/windows-hardware/drivers/audio/windows-audio-processing-objects
    DOT4("Dot4", "48721b56-6795-11d2-b1a8-0080c72e74a2", ArtifactType.CONTROLLER, ArtifactType.INPUT_DEVICE_DRIVER), // input devices that control multifunction IEEE 1284.4 peripheral devices
    DOT4PRINT("Dot4Print", "49ce6ac8-6f86-11d2-b1e5-0080c72e74a2", ArtifactType.PRINTER, ArtifactType.INPUT_DEVICE_DRIVER), // Dot4 print functions; function on a Dot4 device and has a single child device
    AVC("AVC", "c06ff265-ae09-48f0-812c-16753d7cba83", ArtifactType.CONTROLLER, ArtifactType.DRIVER), // IEEE 1394 devices that support the AVC protocol device class
    SBP2("SBP2", "d48179be-ec20-11d1-b6b8-00c04fa372a7", ArtifactType.DATA_STORAGE, ArtifactType.STORAGE_DRIVER), // IEEE 1394 devices that support the SBP2 protocol device class
    NET_TRANS("NetTrans", "4d36e975-e325-11ce-bfc1-08002be10318", ArtifactType.NETWORKING_HARDWARE, ArtifactType.NETWORK_DRIVER), // NDIS protocols CoNDIS stand-alone call managers, and CoNDIS clients, in addition to higher level drivers in transport stacks
    SECURITY_ACCELERATOR("SecurityAccelerator", "268c95a1-edfe-11d3-95c3-0010dc4050a5", ArtifactType.SECURITY_TOKEN, ArtifactType.SECURITY_DEVICE_DRIVER), // devices that accelerate secure socket layer (SSL) cryptographic processing
    PCMCIA("PCMCIA", "4d36e977-e325-11ce-bfc1-08002be10318", ArtifactType.CONTROLLER, ArtifactType.CARD_DRIVER), // PCMCIA and CardBus host controllers, but not PCMCIA or CardBus peripherals
    WCEUSBS("WCEUSBS", "25dbce51-6c8f-4a72-8a6d-b54c2b4fc835", ArtifactType.APPLIANCE, ArtifactType.DRIVER), // Windows CE ActiveSync devices
    IEC61883("61883", "7ebefbc0-3200-11d2-b4c2-00a0c9697d07", ArtifactType.DEVICE_CONNECTOR, ArtifactType.DRIVER), // IEEE 1394 devices that support the IEC 61883 protocol device class; speed computer data-transfer interface

    // https://learn.microsoft.com/en-us/windows-hardware/drivers/install/system-defined-device-setup-classes-reserved-for-system-use
    // Obsolete or reserved for system use; may not require direct categorization
    ADAPTER("Adapter", "4d36e964-e325-11ce-bfc1-08002be10318", ArtifactType.EXTENSION_MODULE, ArtifactType.DRIVER), // class is obsolete
    APM_SUPPORT("APMSupport", "d45b1c18-c8fa-11d1-9f77-0000f805f530", ArtifactType.CONTROLLER, ArtifactType.POWER_DRIVER), // reserved for system use; Advanced Power Management
    DECODER("Decoder", "6bdd1fc2-810f-11d0-bec7-08002be2092f", ArtifactType.INPUT_DEVICE, ArtifactType.INPUT_DEVICE_DRIVER), // reserved for future use; multimedia decoders
    DEBUG1394("1394Debug", "66f250d6-7801-4a64-b139-eea80a450b24", ArtifactType.DEVICE_CONNECTOR, ArtifactType.DRIVER), // reserved for system use; host-side IEEE 1394 Kernel Debugger Support
    ENUM1394("Enum1394", "c459df55-db08-11d1-b009-00a0c9081ff6", ArtifactType.NETWORKING_HARDWARE, ArtifactType.DRIVER), // reserved for system use; IEEE 1394 IP Network Enumerator
    NO_DRIVER("NoDriver", "4d36e976-e325-11ce-bfc1-08002be10318", ArtifactType.CATEGORY_HARDWARE, ArtifactType.DRIVER), // class is obsolete; devices that do not require a driver
    LEGACY_DRIVER("LegacyDriver", "8ecc055d-047f-11d1-a537-0000f8753ed1", ArtifactType.DRIVER, ArtifactType.DRIVER), // reserved for system use; legacy drivers (Non-Plug and Play Drivers)
    UNKNOWN("Unknown", "4d36e97e-e325-11ce-bfc1-08002be10318", ArtifactType.UNKNOWN, ArtifactType.UNKNOWN), // reserved for system use; devices that could not be identified as a specific class
    PRINTER_UPGRADE("PrinterUpgrade", "4d36e97a-e325-11ce-bfc1-08002be10318", ArtifactType.PRINTER, ArtifactType.PRINTER_DRIVER), // reserved for system use; printer upgrades
    SOUND("Sound", "4d36e97c-e325-11ce-bfc1-08002be10318", ArtifactType.AUDIO_HARDWARE, ArtifactType.AUDIO_DRIVER), // class is obsolete; sound devices
    VOLUME_SNAPSHOT("VolumeSnapshot", "533c5b84-ec70-11d2-9505-00c04f79deaf", ArtifactType.CONTROLLER, ArtifactType.DRIVER), // reserved for system use; Volume Shadow Copy Service (VSS) volume snapshot providers

    // found via system scanning
    FIRMWARE("Firmware", "f2e7dd72-6468-4e36-b6f1-6488f42c1b52", ArtifactType.EXTENSION_MODULE, ArtifactType.FIRMWARE_DRIVER), // firmware devices
    SOFTWARE_DEVICE("SoftwareDevice", "62f9c741-b25a-46ce-b54c-9bccce08b6f2", ArtifactType.APPLIANCE, ArtifactType.SOFTWARE_DEVICE_DRIVER), // software-based devices
    AUDIO_ENDPOINT("AudioEndpoint", "c166523c-fe0c-4a94-a586-f1a80cfbbf3e", ArtifactType.AUDIO_HARDWARE, ArtifactType.AUDIO_DRIVER), // audio devices
    PRINT_QUEUE("PrintQueue", "1ed2bbf9-11f0-4084-b21f-ad83a8e6dcdc", ArtifactType.PRINTER, ArtifactType.PRINT_QUEUES_DRIVER), // print queue devices
    SD_HOST("SDHost", "a0a588a4-c46f-4b37-b7ea-c82fe89870c6", ArtifactType.STORAGE_CONTROLLER, ArtifactType.STORAGE_DRIVER), // SD host controllers
    COMPUTER("Computer", "4d36e966-e325-11ce-bfc1-08002be10318", ArtifactType.APPLIANCE, ArtifactType.COMPUTER_DRIVER), // computer systems
    SOFTWARE_COMPONENT("SoftwareComponent", "5c4c3332-344d-483c-8739-259e934c9cc8", ArtifactType.APPLIANCE, ArtifactType.SOFTWARE_DEVICE_DRIVER), // software components
    SMART_CARD_FILTER("SmartCardFilter", "db4f6ddd-9c0e-45e4-9597-78dbbad0f412", ArtifactType.SECURITY_TOKEN, ArtifactType.SECURITY_DEVICE_DRIVER), // smart card filters
    ;

    private final String pnpClass;
    private final String classGuid;
    private final ArtifactType pnpEntityType;
    private final ArtifactType pnpDriverType;

    WindowsPnpClassGuid(String pnpClass, String classGuid, ArtifactType pnpEntityType, ArtifactType pnpDriverType) {
        this.pnpClass = pnpClass;
        this.classGuid = classGuid;
        this.pnpEntityType = pnpEntityType;
        this.pnpDriverType = pnpDriverType;
    }

    public String getPnpClass() {
        return pnpClass;
    }

    public String getClassGuid() {
        return classGuid;
    }

    public ArtifactType getPnpEntityType() {
        return pnpEntityType;
    }

    public ArtifactType getPnpDriverType() {
        return pnpDriverType;
    }

    public static WindowsPnpClassGuid fromPnpClass(String pnpClass) {
        if (pnpClass == null) return null;
        for (WindowsPnpClassGuid value : values()) {
            if (value.getPnpClass().equalsIgnoreCase(pnpClass)) {
                return value;
            }
        }
        return null;
    }

    public static WindowsPnpClassGuid fromClassGuid(String classGuid) {
        if (classGuid == null) return null;
        for (WindowsPnpClassGuid value : values()) {
            if (value.getClassGuid().equalsIgnoreCase(classGuid)) {
                return value;
            }
        }
        // check if classGuid from parameter is surrounded with { }
        // like: {4d36e96b-e325-11ce-bfc1-08002be10318} for the Keyboard class
        if (classGuid.startsWith("{") && classGuid.endsWith("}")) {
            return fromClassGuid(classGuid.substring(1, classGuid.length() - 1));
        }
        return null;
    }
}
