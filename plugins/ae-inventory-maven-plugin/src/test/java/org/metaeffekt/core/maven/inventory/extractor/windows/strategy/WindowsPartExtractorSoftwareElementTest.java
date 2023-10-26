package org.metaeffekt.core.maven.inventory.extractor.windows.strategy;

import org.junit.Test;

import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WindowsPartExtractorSoftwareElementTest {

    private final WindowsPartExtractorSoftwareElement partExtractor = new WindowsPartExtractorSoftwareElement();

    @Test
    public void extractPropertiesFromMappingStringTest() {
        final String mappingString = "IdentifyingNumber=\"{ad8a2fa1-06e7-4b0d-927d-6e54b3d31028}\",Name=\"VC_Redist\",ProductName=\"Microsoft Visual C++ 2005 Redistributable (x64)\",Version=\"8.0.61000\"";
        final Map<String, String> properties = partExtractor.extractPropertiesFromMappingString(mappingString);

        assertEquals("{ad8a2fa1-06e7-4b0d-927d-6e54b3d31028}", properties.get("IdentifyingNumber"));
        assertEquals("VC_Redist", properties.get("Name"));
        assertEquals("Microsoft Visual C++ 2005 Redistributable (x64)", properties.get("ProductName"));
        assertEquals("8.0.61000", properties.get("Version"));
    }

    @Test
    public void extractPropertiesFromMappingStringWithEscapedQuotesTest() {
        final String mappingString = "IdentifyingNumber=\"{ad8a2fa1-06e7-4b0d-927d-6e54b3d31028}\",Name=\"V\\\"C_Redist\",ProductName=\"Microsoft Visual C++ 2005 Redistributable (x64)\",Version=\"8.0.61000\"";
        final Map<String, String> properties = partExtractor.extractPropertiesFromMappingString(mappingString);

        assertEquals("{ad8a2fa1-06e7-4b0d-927d-6e54b3d31028}", properties.get("IdentifyingNumber"));
        assertEquals("V\"C_Redist", properties.get("Name"));
        assertEquals("Microsoft Visual C++ 2005 Redistributable (x64)", properties.get("ProductName"));
        assertEquals("8.0.61000", properties.get("Version"));
    }

    @Test
    public void extractPropertiesFromMappingStringWithCommasInValuesTest() {
        final String mappingString = "IdentifyingNumber=\"{ad8a2fa1-06e7-4b0d-927d-6e54b3d31028}\",Name=\"VC,Redist\",ProductName=\"Microsoft Visual C++ 2005 Redistributable (x64)\",Version=\"8,0.61000\"";
        final Map<String, String> properties = partExtractor.extractPropertiesFromMappingString(mappingString);

        assertEquals("{ad8a2fa1-06e7-4b0d-927d-6e54b3d31028}", properties.get("IdentifyingNumber"));
        assertEquals("VC,Redist", properties.get("Name"));
        assertEquals("Microsoft Visual C++ 2005 Redistributable (x64)", properties.get("ProductName"));
        assertEquals("8,0.61000", properties.get("Version"));
    }

    @Test
    public void extractPropertiesFromMappingStringWithOnePropertyTest() {
        final String mappingString = "Key=\"Value\"";
        final Map<String, String> properties = partExtractor.extractPropertiesFromMappingString(mappingString);

        assertEquals("Value", properties.get("Key"));
    }

    @Test
    public void extractPropertiesFromMappingStringWithTrailingCommasTest() {
        final String mappingString = "Key1=\"Value1\",Key2=\"Value2\",,Key3=\"Value3\",,";
        final Map<String, String> properties = partExtractor.extractPropertiesFromMappingString(mappingString);

        assertEquals("Value1", properties.get("Key1"));
        assertEquals("Value2", properties.get("Key2"));
        assertEquals("Value3", properties.get("Key3"));
    }

    @Test
    public void extractPropertiesFromMappingStringWithEmptyPairsTest() {
        final String mappingString = "Key1=\"\",=\"Value2\",Key3=\"\"";
        final Map<String, String> properties = partExtractor.extractPropertiesFromMappingString(mappingString);

        assertEquals("", properties.get("Key1"));
        assertEquals("Value2", properties.get(""));
        assertEquals("", properties.get("Key3"));
    }

    @Test
    public void extractPropertiesFromMappingStringWithEscapedSequenceInKeyValuesTest() {
        final String mappingString = "Ke\\\"y=\"Val\\\"ue\",Ke\\,y=\"Val\\,ue\",Ke\\\\y=\"Val\\\\ue\"";
        final Map<String, String> properties = partExtractor.extractPropertiesFromMappingString(mappingString);

        assertEquals("Val\"ue", properties.get("Ke\"y"));
        assertEquals("Val,ue", properties.get("Ke,y"));
        assertEquals("Val\\ue", properties.get("Ke\\y"));
    }

    @Test
    public void extractPropertiesFromMappingStringWithEmptyInputTest() {
        final Map<String, String> properties = partExtractor.extractPropertiesFromMappingString("");
        assertTrue(properties.isEmpty());
    }

    @Test
    public void extractPropertiesFromMappingStringWithNullInputTest() {
        final Map<String, String> properties = partExtractor.extractPropertiesFromMappingString(null);
        assertTrue(properties.isEmpty());
    }
}
