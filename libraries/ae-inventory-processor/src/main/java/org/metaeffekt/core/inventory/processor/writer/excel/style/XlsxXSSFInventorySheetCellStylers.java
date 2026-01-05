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
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.metaeffekt.core.inventory.processor.model.AdvisoryMetaData;
import org.metaeffekt.core.inventory.processor.model.InventorySerializationContext;
import org.metaeffekt.core.inventory.processor.model.VulnerabilityMetaData;
import org.metaeffekt.core.util.ParsingUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * If this file is changed, the according changes should also be applied to the other stylers.
 */
public class XlsxXSSFInventorySheetCellStylers {

    /**
     * Contains self-managed palette of colors. XSSF api is strange.
     */
    private final Map<String, XSSFColor> colorPalette = new HashMap<>();

    private final DefaultIndexedColorMap indexedColorMap = new DefaultIndexedColorMap();

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

    public XlsxXSSFInventorySheetCellStylers(SXSSFWorkbook workbook) {
        final CellStyle defaultHeaderStyle = createDefaultHeaderStyle(workbook);
        final CellStyle assetSourceHeaderStyle = createAssetSourceHeaderStyle(workbook);
        final CellStyle assetConfigHeaderStyle = createAssetConfigHeaderStyle(workbook);
        final CellStyle assetHeaderStyle = createAssetHeaderStyle(workbook);
        final CellStyle binaryArtifactHeaderStyle = createBinaryArtifactHeaderStyle(workbook);
        final CellStyle descriptorHeaderStyle = createDescriptorHeaderStyle(workbook);
        final CellStyle sourceArtifactHeaderStyle = createSourceArtifactHeaderStyle(workbook);
        final CellStyle sourceArchiveHeaderStyle = createSourceArchiveHeaderStyle(workbook);
        final CellStyle centeredStyle = createCenteredStyle(workbook);
        final CellStyle warnHeaderStyle = createWarnHeaderStyle(workbook);
        final CellStyle errorHeaderStyle = createErrorHeaderStyle(workbook);

        final CellStyle termsMarkerHeaderStyle = createMarkerHeaderStyle(workbook);
        final CellStyle termsClassificationStyle = createClassificationHeaderStyle(workbook);

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

    protected CellStyle createDefaultHeaderStyle(SXSSFWorkbook workbook) {
        final XSSFColor headerColor = resolveColor(workbook, "153,204,255");

        final Font headerFont = workbook.createFont();
        headerFont.setColor(Font.COLOR_NORMAL);

        final CellStyle headerStyle = workbook.createCellStyle();
        headerStyle.setFillForegroundColor(headerColor);
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setFont(headerFont);
        headerStyle.setWrapText(true);
        return headerStyle;
    }

    protected CellStyle createHeaderStyle(SXSSFWorkbook workbook) {
        return createDefaultHeaderStyle(workbook);
    }

    protected CellStyle createAssetSourceHeaderStyle(SXSSFWorkbook workbook) {
        return createRotatedCellStyle(workbook, resolveColor(workbook, "155,192,0"));
    }

    protected CellStyle createAssetConfigHeaderStyle(SXSSFWorkbook workbook) {
        return createRotatedCellStyle(workbook, resolveColor(workbook, "219,219,219"));
    }

    protected CellStyle createAssetHeaderStyle(SXSSFWorkbook workbook) {
        return createRotatedCellStyle(workbook, resolveColor(workbook, "255,192,0"));
    }

    protected CellStyle createRotatedCellStyle(SXSSFWorkbook workbook, XSSFColor headerColor) {
        final CellStyle cellStyle = createDefaultHeaderStyle(workbook);
        cellStyle.setFillForegroundColor(headerColor);
        cellStyle.setRotation((short) -90);
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.TOP);
        cellStyle.setWrapText(false);
        return cellStyle;
    }

    protected CellStyle createCenteredStyle(SXSSFWorkbook workbook) {
        final CellStyle cellStyle = workbook.createCellStyle();
        cellStyle.setAlignment(HorizontalAlignment.CENTER);
        cellStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        return cellStyle;
    }

    protected CellStyle createWarnHeaderStyle(SXSSFWorkbook workbook) {
        final XSSFColor headerColor = resolveColor(workbook, "244,176,132");
        return createRotatedCellStyle(workbook, headerColor);
    }

    protected CellStyle createMarkerHeaderStyle(SXSSFWorkbook workbook) {
        final XSSFColor headerColor = resolveColor(workbook, "250,234,173");
        return createRotatedCellStyle(workbook, headerColor);
    }

    protected CellStyle createClassificationHeaderStyle(SXSSFWorkbook workbook) {
        final XSSFColor headerColor = resolveColor(workbook, "248,214,128");
        return createRotatedCellStyle(workbook, headerColor);
    }

    protected CellStyle createErrorHeaderStyle(SXSSFWorkbook workbook) {
        final XSSFColor headerColor = resolveColor(workbook, "244,176,132");
        final CellStyle cellStyle = createDefaultHeaderStyle(workbook);
        cellStyle.setFillForegroundColor(headerColor);
        cellStyle.setWrapText(false);
        return cellStyle;
    }

    protected CellStyle createBinaryArtifactHeaderStyle(SXSSFWorkbook workbook) {
        final XSSFColor headerColor = resolveColor(workbook, "198,224,180");
        final CellStyle cellStyle = createDefaultHeaderStyle(workbook);
        cellStyle.setFillForegroundColor(headerColor);
        cellStyle.setWrapText(false);
        return cellStyle;
    }

    protected CellStyle createSourceArtifactHeaderStyle(SXSSFWorkbook workbook) {
        final XSSFColor headerColor = resolveColor(workbook, "248,203,173");
        final CellStyle cellStyle = createDefaultHeaderStyle(workbook);
        cellStyle.setFillForegroundColor(headerColor);
        cellStyle.setWrapText(false);
        return cellStyle;
    }

    protected CellStyle createSourceArchiveHeaderStyle(SXSSFWorkbook workbook) {
        final XSSFColor headerColor = resolveColor(workbook, "241,187,144");
        final CellStyle cellStyle = createDefaultHeaderStyle(workbook);
        cellStyle.setFillForegroundColor(headerColor);
        cellStyle.setWrapText(false);
        return cellStyle;
    }

    protected CellStyle createDescriptorHeaderStyle(SXSSFWorkbook workbook) {
        final XSSFColor headerColor = resolveColor(workbook, "255,230,153");
        final CellStyle cellStyle = createDefaultHeaderStyle(workbook);
        cellStyle.setFillForegroundColor(headerColor);
        cellStyle.setWrapText(false);
        return cellStyle;
    }

    protected XSSFColor resolveColor(SXSSFWorkbook workbook, String rgb) {
        final String[] rgbSplit = rgb.trim().split(", ?");
        final int red = Short.parseShort(rgbSplit[0]);
        final int green = Short.parseShort(rgbSplit[1]);
        final int blue = Short.parseShort(rgbSplit[2]);

        XSSFColor color = colorPalette.get(rgb);
        if (color == null) {
            // create color and register in map
            color = new XSSFColor(new java.awt.Color(red, green, blue), indexedColorMap);

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
                final CellStyle customHeaderStyle = createHeaderStyle((SXSSFWorkbook) context.getWorkbook());
                final XSSFColor headerColor = resolveColor((SXSSFWorkbook) context.getWorkbook(), customHeaderColor);
                customHeaderStyle.setFillForegroundColor(headerColor);

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
                final CellStyle centeredCellStyle = this.createCenteredStyle((SXSSFWorkbook) context.getWorkbook());
                context.getCell().setCellStyle(centeredCellStyle);
            });
    }

    public InventorySheetCellStyler createArtifactsCellStyler(InventorySerializationContext serializationContext) {
        return InventorySheetCellStyler.createStyler(
                context -> {
                    final Boolean centered = serializationContext.get("artifact.column.[" + context.getFullColumnHeader() + "].centered");
                    return centered != null && centered;
                }, context -> {
                    final CellStyle centeredCellStyle = this.createCenteredStyle((SXSSFWorkbook) context.getWorkbook());
                    context.getCell().setCellStyle(centeredCellStyle);
                });
    }
}
