/*
 * Copyright 2009-2026 the original author or authors.
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
package org.metaeffekt.core.inventory.processor.writer.excel.style;

import org.apache.commons.lang3.StringUtils;
import org.apache.poi.common.usermodel.HyperlinkType;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFPalette;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.*;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.model.InventorySerializationContext;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.util.ParsingUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * If this file is changed, the according changes should also be applied to the other stylers.
 */
public class XlsHSSFInventorySheetCellStylers {

    /**
     * Contains self-managed palette of colors. HSSF api is strange.
     */
    private final Map<String, HSSFColor> colorPalette = new HashMap<>();

    // HEADER CELLS
    public final InventorySheetCellStyler headerStyleColumnNameAssetId;
    public final InventorySheetCellStyler headerStyleColumnNameIncompleteMatch;
    public final InventorySheetCellStyler headerStyleColumnNameErrors;
    public final InventorySheetCellStyler headerStyleBinaryArtifact;
    public final InventorySheetCellStyler headerStyleSourceArtifact;
    public final InventorySheetCellStyler headerStyleSourceArchive;
    public final InventorySheetCellStyler headerStyleDescriptor;
    public final InventorySheetCellStyler headerStyleDefault;
    public final InventorySheetCellStyler headerStyleColumnNameSrcAssetSource;
    public final InventorySheetCellStyler headerStyleColumnNameAssetConfig;
    public final InventorySheetCellStyler headerStyleColumnNameMarker;
    public final InventorySheetCellStyler headerStyleColumnNameClassification;

    // DATA CELLS
    public final InventorySheetCellStyler contentStyleColumnNameAssetId;
    public final InventorySheetCellStyler contentStyleColumnNameIncompleteMatch;
    public final InventorySheetCellStyler contentStyleCvssScoresDoubleValue;
    public final InventorySheetCellStyler contentStyleUrlValue;
    public final InventorySheetCellStyler contentStyleColumnNameSrcCentered;
    public final InventorySheetCellStyler contentStyleColumnNameMarkerCentered;

    public XlsHSSFInventorySheetCellStylers(HSSFWorkbook workbook) {
        final HSSFCellStyle defaultHeaderStyle = createDefaultHeaderStyle(workbook);
        final HSSFCellStyle assetSourceHeaderStyle = createAssetSourceHeaderStyle(workbook);
        final HSSFCellStyle assetConfigHeaderStyle = createAssetConfigHeaderStyle(workbook);
        final HSSFCellStyle assetHeaderStyle = createAssetHeaderStyle(workbook);
        final HSSFCellStyle binaryArtifactHeaderStyle = createBinaryArtifactHeaderStyle(workbook);
        final HSSFCellStyle descriptorHeaderStyle = createDescriptorHeaderStyle(workbook);
        final HSSFCellStyle sourceArtifactHeaderStyle = createSourceArtifactHeaderStyle(workbook);
        final HSSFCellStyle sourceArchiveHeaderStyle = createSourceArchiveHeaderStyle(workbook);
        final HSSFCellStyle centeredStyle = createCenteredStyle(workbook);
        final HSSFCellStyle warnHeaderStyle = createWarnHeaderStyle(workbook);
        final HSSFCellStyle errorHeaderStyle = createErrorHeaderStyle(workbook);

        final HSSFCellStyle termsMarkerHeaderStyle = createMarkerHeaderStyle(workbook);
        final HSSFCellStyle termsClassificationStyle = createClassificationHeaderStyle(workbook);

        // HEADER CELLS
        this.headerStyleColumnNameAssetId = InventorySheetCellStyler.createStyler(
                context -> isAssetId(context.getFullColumnHeader()),
                context -> {
                    context.getCell().setCellStyle(assetHeaderStyle);
                    // number of pixel times magic number
                    context.getSheet().setColumnWidth(context.getColumnIndex(), 20 * 42);
                    context.getRow().setHeight((short) (170 * 20));
                });

        this.headerStyleColumnNameIncompleteMatch = InventorySheetCellStyler.createStyler(
                context -> context.getFullColumnHeader().equalsIgnoreCase("Incomplete Match"),
                context -> {
                    context.getCell().setCellStyle(warnHeaderStyle);
                    // number of pixel times magic number
                    context.getSheet().setColumnWidth(context.getColumnIndex(), 20 * 42);
                    context.getRow().setHeight((short) (170 * 20));
                });

        this.headerStyleBinaryArtifact = InventorySheetCellStyler.createStyler(
                context -> context.getFullColumnHeader().startsWith("Binary Artifact - "),
                context -> context.getCell().setCellStyle(binaryArtifactHeaderStyle));

        this.headerStyleSourceArtifact = InventorySheetCellStyler.createStyler(
                context -> context.getFullColumnHeader().startsWith("Source Artifact - "),
                context -> context.getCell().setCellStyle(sourceArtifactHeaderStyle));

        this.headerStyleSourceArchive = InventorySheetCellStyler.createStyler(
                context -> context.getFullColumnHeader().startsWith("Source Archive - "),
                context -> context.getCell().setCellStyle(sourceArchiveHeaderStyle));

        this.headerStyleDescriptor = InventorySheetCellStyler.createStyler(
                context -> context.getFullColumnHeader().startsWith("Descriptor - "),
                context -> context.getCell().setCellStyle(descriptorHeaderStyle));

        this.headerStyleColumnNameErrors = InventorySheetCellStyler.createStyler(
                context -> {
                    return context.getFullColumnHeader().equalsIgnoreCase("Errors");
                }, context -> {
                    context.getCell().setCellStyle(errorHeaderStyle);
                });

        this.headerStyleDefault = InventorySheetCellStyler.createStyler(
                context -> true, context -> {
                    context.getCell().setCellStyle(defaultHeaderStyle);
                });

        this.headerStyleColumnNameSrcAssetSource = InventorySheetCellStyler.createStyler(
                context -> {
                    return context.getFullColumnHeader().startsWith("SRC-");
                }, context -> {
                    context.getCell().setCellStyle(assetSourceHeaderStyle);
                    // number of pixel times magic number
                    context.getSheet().setColumnWidth(context.getColumnIndex(), 20 * 42);
                    context.getRow().setHeight((short) (170 * 20));
                });

        this.headerStyleColumnNameAssetConfig = InventorySheetCellStyler.createStyler(
                context -> {
                    final String fullColumnHeader = context.getFullColumnHeader();
                    return fullColumnHeader.startsWith("config_") ||
                            fullColumnHeader.startsWith("prop_") ||
                            fullColumnHeader.startsWith("meta_");
                }, context -> {
                    context.getCell().setCellStyle(assetConfigHeaderStyle);
                    // number of pixel times magic number
                    context.getSheet().setColumnWidth(context.getColumnIndex(), 40 * 42);
                });

        this.headerStyleColumnNameMarker = InventorySheetCellStyler.createStyler(
                context -> {
                    return context.getFullColumnHeader().startsWith("M-");
                }, context -> {
                    context.getCell().setCellStyle(termsMarkerHeaderStyle);
                    // number of pixel times magic number
                    context.getSheet().setColumnWidth(context.getColumnIndex(), 20 * 42);
                    context.getRow().setHeight((short) (170 * 20));
                });

        this.headerStyleColumnNameClassification = InventorySheetCellStyler.createStyler(
                context -> {
                    return context.getFullColumnHeader().startsWith("C-");
                }, context -> {
                    context.getCell().setCellStyle(termsClassificationStyle);
                    // number of pixel times magic number
                    context.getSheet().setColumnWidth(context.getColumnIndex(), 20 * 42);
                    context.getRow().setHeight((short) (170 * 20));
                });

        // DATA CELLS
        this.contentStyleColumnNameAssetId = InventorySheetCellStyler.createStyler(
                context -> {
                    return isAssetId(context.getFullColumnHeader());
                }, context -> {
                    context.getCell().setCellStyle(centeredStyle);
                });

        this.contentStyleColumnNameIncompleteMatch = InventorySheetCellStyler.createStyler(
                context -> {
                    return context.getFullColumnHeader().equalsIgnoreCase("Incomplete Match");
                }, context -> {
                    context.getCell().setCellStyle(centeredStyle);
                });

        this.contentStyleCvssScoresDoubleValue = InventorySheetCellStyler.createStyler(
                context -> {
                    final boolean isCorrectHeader = context.isHeaderEither(
                            VulnerabilityMetaData.Attribute.SCORE_CONTEXT_OVERALL,
                            VulnerabilityMetaData.Attribute.SCORE_INITIAL_OVERALL,
                            VulnerabilityMetaData.Attribute.SCORE_BASE,
                            VulnerabilityMetaData.Attribute.SCORE_EXPLOITABILITY,
                            VulnerabilityMetaData.Attribute.SCORE_IMPACT
                    );
                    if (!isCorrectHeader) {
                        return false;
                    }
                    return !context.isSplitColumn() && StringUtils.isNotEmpty(context.getFullCellContent());
                }, context -> {
                    try {
                        context.getCell().setCellValue(ParsingUtils.parseCvssScore(context.getFullCellContent()));
                    } catch (NumberFormatException e) {
                        context.getCell().setCellValue(context.getFullCellContent());
                    }
                });

        this.contentStyleUrlValue = InventorySheetCellStyler.createStyler(
                context -> {
                    final boolean isCorrectHeader = context.isHeaderEither(VulnerabilityMetaData.Attribute.URL, AdvisoryMetaData.Attribute.URL);
                    if (!isCorrectHeader) {
                        return false;
                    }
                    return !context.isSplitColumn() && StringUtils.isNotEmpty(context.getFullCellContent());
                }, context -> {
                    final Hyperlink link = workbook.getCreationHelper().createHyperlink(HyperlinkType.URL);
                    link.setAddress(context.getFullCellContent());
                    context.getCell().setHyperlink(link);
                });

        this.contentStyleColumnNameSrcCentered = InventorySheetCellStyler.createStyler(
                context -> {
                    return context.getFullColumnHeader().startsWith("SRC-");
                }, context -> {
                    context.getCell().setCellStyle(centeredStyle);
                });

        this.contentStyleColumnNameMarkerCentered = InventorySheetCellStyler.createStyler(
                context -> {
                    return context.getFullColumnHeader().startsWith("M-") ||
                            context.getFullColumnHeader().startsWith("C-")  ;
                }, context -> {
                    context.getCell().setCellStyle(centeredStyle);
                });
    }

    private boolean isAssetId(String key) {
        return key.startsWith("CID-") ||
                key.startsWith("AID-") ||
                key.startsWith("EID-") ||
                key.startsWith("IID-") ||
                key.startsWith("EAID-");
    }

    private HSSFCellStyle createDefaultHeaderStyle(HSSFWorkbook workbook) {
        final HSSFColor headerColor = resolveColor(workbook, "153,204,255");

        final Font headerFont = workbook.createFont();
        headerFont.setColor(Font.COLOR_NORMAL);

        final HSSFCellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(headerColor.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setFont(headerFont);
        headerStyle.setWrapText(true);
        return headerStyle;
    }

    private HSSFCellStyle createHeaderStyle(HSSFWorkbook workbook) {
        return createDefaultHeaderStyle(workbook);
    }

    private HSSFCellStyle createAssetSourceHeaderStyle(HSSFWorkbook workbook) {
        return createRotatedCellStyle(workbook, resolveColor(workbook, "155,192,0"));
    }

    private HSSFCellStyle createAssetConfigHeaderStyle(HSSFWorkbook workbook) {
        return createRotatedCellStyle(workbook, resolveColor(workbook, "219,219,219"));
    }

    private HSSFCellStyle createAssetHeaderStyle(HSSFWorkbook workbook) {
        return createRotatedCellStyle(workbook, resolveColor(workbook, "255,192,0"));
    }

    private HSSFCellStyle createRotatedCellStyle(HSSFWorkbook workbook, HSSFColor headerColor) {
        final HSSFCellStyle cellStyle = createDefaultHeaderStyle(workbook);
        cellStyle.setFillForegroundColor(headerColor.getIndex());
        cellStyle.setRotation((short) -90);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.TOP);
        cellStyle.setWrapText(false);
        return cellStyle;
    }

    private HSSFCellStyle createCenteredStyle(HSSFWorkbook workbook) {
        final HSSFCellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        return cellStyle;
    }

    private HSSFCellStyle createWarnHeaderStyle(HSSFWorkbook workbook) {
        final HSSFColor headerColor = resolveColor(workbook, "244,176,132");
        return createRotatedCellStyle(workbook, headerColor);
    }

    protected HSSFCellStyle createMarkerHeaderStyle(HSSFWorkbook workbook) {
        final HSSFColor headerColor = resolveColor(workbook, "250,234,173");
        return createRotatedCellStyle(workbook, headerColor);
    }

    protected HSSFCellStyle createClassificationHeaderStyle(HSSFWorkbook workbook) {
        final HSSFColor headerColor = resolveColor(workbook, "248,214,128");
        return createRotatedCellStyle(workbook, headerColor);
    }

    private HSSFCellStyle createErrorHeaderStyle(HSSFWorkbook workbook) {
        final HSSFColor headerColor = resolveColor(workbook, "244,176,132");
        final HSSFCellStyle cellStyle = createDefaultHeaderStyle(workbook);
        cellStyle.setFillForegroundColor(headerColor.getIndex());
        cellStyle.setWrapText(false);
        return cellStyle;
    }

    protected HSSFCellStyle createBinaryArtifactHeaderStyle(HSSFWorkbook workbook) {
        final HSSFColor headerColor = resolveColor(workbook, "198,224,180");
        final HSSFCellStyle cellStyle = createDefaultHeaderStyle(workbook);
        cellStyle.setFillForegroundColor(headerColor);
        cellStyle.setWrapText(false);
        return cellStyle;
    }

    protected HSSFCellStyle createSourceArtifactHeaderStyle(HSSFWorkbook workbook) {
        final HSSFColor headerColor = resolveColor(workbook, "248,203,173");
        final HSSFCellStyle cellStyle = createDefaultHeaderStyle(workbook);
        cellStyle.setFillForegroundColor(headerColor);
        cellStyle.setWrapText(false);
        return cellStyle;
    }

    protected HSSFCellStyle createSourceArchiveHeaderStyle(HSSFWorkbook workbook) {
        final HSSFColor headerColor = resolveColor(workbook, "241,187,144");
        final HSSFCellStyle cellStyle = createDefaultHeaderStyle(workbook);
        cellStyle.setFillForegroundColor(headerColor);
        cellStyle.setWrapText(false);
        return cellStyle;
    }

    protected HSSFCellStyle createDescriptorHeaderStyle(HSSFWorkbook workbook) {
        final HSSFColor headerColor = resolveColor(workbook, "255,230,153");
        final HSSFCellStyle cellStyle = createDefaultHeaderStyle(workbook);
        cellStyle.setFillForegroundColor(headerColor);
        cellStyle.setWrapText(false);
        return cellStyle;
    }


    public HSSFColor resolveColor(HSSFWorkbook workbook, String rgb) {
        final HSSFPalette palette = workbook.getCustomPalette();
        final String[] rgbSplit = rgb.trim().split(", ?");
        final byte red = (byte) Short.parseShort(rgbSplit[0]);
        final byte green = (byte) Short.parseShort(rgbSplit[1]);
        final byte blue = (byte) Short.parseShort(rgbSplit[2]);

        HSSFColor color = colorPalette.get(rgb);
        if (color == null) {
            // the index needs to start with 7 (8 being an offset that is used by HSSFPalette
            final int index = colorPalette.size() + 8;

            try {
                // adjust color
                palette.setColorAtIndex((short) index, red, green, blue);

                // access color using index
                color = palette.getColor(index);

            } catch (RuntimeException e) {
                // fallback to similar color (since palette is limited)
                color = palette.findSimilarColor(red, green, blue);
            }

            // add to map
            colorPalette.put(rgb, color);
        }
        return color;
    }

    // CUSTOM DYNAMIC STYLERS

    public InventorySheetCellStyler createArtifactHeaderCellStyler(InventorySerializationContext serializationContext) {
        return createCustomHeaderStyler(serializationContext, "artifact.header");
    }

    public InventorySheetCellStyler createLicensesHeaderCellStyler(InventorySerializationContext serializationContext) {
        return createCustomHeaderStyler(serializationContext, "licensedata.header");
    }

    private InventorySheetCellStyler createCustomHeaderStyler(InventorySerializationContext serializationContext, String prefix) {
        return InventorySheetCellStyler.createStyler(
                context -> {
                    final String customHeaderColor = serializationContext.get(prefix + ".[" + context.getFullColumnHeader() + "].fg");
                    return StringUtils.isNotBlank(customHeaderColor);
                }, context -> {
                    final String customHeaderColor = serializationContext.get(prefix + ".[" + context.getFullColumnHeader() + "].fg");
                    final HSSFCellStyle customHeaderStyle = this.createHeaderStyle((HSSFWorkbook) context.getWorkbook());
                    final HSSFColor headerColor = resolveColor((HSSFWorkbook) context.getWorkbook(), customHeaderColor);
                    customHeaderStyle.setFillForegroundColor(headerColor.getIndex());

                    // FIXME: IS THIS THE CORRECT BEHAVIOR?
                    //        it seems that the orientation and the width properties are only applied if the foreground color is set.
                    final String customHeaderOrientation = serializationContext.get(prefix + ".[" + context.getFullColumnHeader() + "].orientation");
                    if ("up".equalsIgnoreCase(customHeaderOrientation)) {
                        customHeaderStyle.setRotation((short) -90);
                        context.getSheet().setColumnWidth(context.getColumnIndex(), 20 * 42);
                        context.getRow().setHeight((short) (170 * 20));
                        customHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
                        customHeaderStyle.setVerticalAlignment(VerticalAlignment.TOP);
                    }

                    final Integer customHeaderWidth = serializationContext.get(prefix + ".[" + context.getFullColumnHeader() + "].width");
                    if (customHeaderWidth != null) {
                        context.getSheet().setColumnWidth(context.getColumnIndex(), customHeaderWidth * 42);
                    }

                    context.getCell().setCellStyle(customHeaderStyle);
                });
    }

    public InventorySheetCellStyler createLicensesCellStyler(InventorySerializationContext serializationContext) {
        return InventorySheetCellStyler.createStyler(
                context -> {
                    final Boolean centered = serializationContext.get("licensedata.column.[" + context.getFullColumnHeader() + "].centered");
                    return centered != null && centered;
                }, context -> {
                    final HSSFCellStyle centeredCellStyle = this.createCenteredStyle((HSSFWorkbook) context.getWorkbook());
                    context.getCell().setCellStyle(centeredCellStyle);
                });
    }

    public InventorySheetCellStyler createArtifactsCellStyler(InventorySerializationContext serializationContext) {
        return InventorySheetCellStyler.createStyler(
                context -> {
                    final Boolean centered = serializationContext.get("artifact.column.[" + context.getFullColumnHeader() + "].centered");
                    return centered != null && centered;
                }, context -> {
                    final HSSFCellStyle centeredCellStyle = this.createCenteredStyle((HSSFWorkbook) context.getWorkbook());
                    context.getCell().setCellStyle(centeredCellStyle);
                });
    }

}
