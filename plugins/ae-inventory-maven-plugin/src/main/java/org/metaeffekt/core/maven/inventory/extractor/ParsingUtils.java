package org.metaeffekt.core.maven.inventory.extractor;

import org.metaeffekt.core.inventory.processor.model.Constants;
import org.springframework.util.StringUtils;

import java.util.List;

public class ParsingUtils {

    protected static int getIndex(List<String> lines, String key) {
        int index = -1;
        for (String line : lines) {
            index ++;
            if (line.startsWith(key)) {
                return index;
            }
        }
        return -1;
    }

    protected static String getValue(List<String> lines, String key) {
        int index = getIndex(lines, key);
        if (index < 0) return null;
        if (lines.size() < index) return null;
        String line = lines.get(index);
        int colonIndex = line.indexOf(Constants.DELIMITER_COLON);
        if (colonIndex < 0) return null;
        StringBuilder sb = new StringBuilder(line.substring(colonIndex + 1).trim());
        int lineIndex = index + 1;
        while (lineIndex < lines.size() && !lines.get(lineIndex).contains(""+Constants.DELIMITER_COLON)) {
            if (StringUtils.hasText(sb)) sb.append(Constants.DELIMITER_NEWLINE);
            line = lines.get(lineIndex).trim();

            // filter the debian specific lines with '.'
            if (!".".equalsIgnoreCase(line)) {
                sb.append(line);
            }
            lineIndex++;
        }
        return sb.toString().trim();

        // NOTE: potential flaw:
        //  - multiline values are read until the next line with a colon in detected.
    }

}
