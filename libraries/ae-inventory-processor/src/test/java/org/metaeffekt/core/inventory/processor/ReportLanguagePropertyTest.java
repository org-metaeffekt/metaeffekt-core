package org.metaeffekt.core.inventory.processor;

import org.junit.Test;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.*;

import static org.assertj.core.api.Assertions.fail;

public class ReportLanguagePropertyTest {

    public static String[] propertyFiles = { "en.properties", "de.properties" };

    @Test
    public void testLangFilesHaveSameKeys() throws IOException {
        Map<String, Set<String>> fileKeys = new HashMap<>();
        Set<String> allKeys = new HashSet<>();

        for (String file : propertyFiles) {
            Properties props = loadProperties(file);
            Set<String> keys = props.stringPropertyNames();
            fileKeys.put(file, keys);
            allKeys.addAll(keys);
        }

        StringBuilder failureMessage = new StringBuilder();
        for (Map.Entry<String, Set<String>> entry : fileKeys.entrySet()) {
            Set<String> keys = entry.getValue();
            Set<String> missing = new HashSet<>(allKeys);
            missing.removeAll(keys);

            if (!missing.isEmpty()) {
                failureMessage.append("File '").append(entry.getKey())
                        .append("' is missing keys: ").append(missing).append("\n");
            }
        }

        if (failureMessage.length() > 0) {
            fail("Properties files do not have the same keys:\n" + failureMessage);
        }
    }

    private Properties loadProperties(String resourceName) throws IOException {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("META-INF/templates/lang/" + resourceName)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourceName);
            }
            props.load(is);
        }
        return props;
    }
}
