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

package org.metaeffekt.core.inventory.processor.patterns.contributors.util.bdb;

public class RpmConstants {
    // Reference constants for tag values
    public static final int RPMTAG_HEADERI18NTABLE = 100;
    public static final int RPMTAG_HEADERIMAGE = 61;
    public static final int RPMTAG_HEADERSIGNATURES = 62;
    public static final int RPMTAG_HEADERIMMUTABLE = 63;
    public static final int RPM_MIN_TYPE = 0;
    public static final int RPM_NULL_TYPE = 0;
    public static final int RPM_CHAR_TYPE = 1;
    public static final int RPM_INT8_TYPE = 2;
    public static final int RPM_INT16_TYPE = 3;
    public static final int RPM_INT32_TYPE = 4;
    public static final int RPM_STRING_TYPE = 6;
    public static final int RPM_MAX_TYPE = 9;
    public static final int RPM_STRING_ARRAY_TYPE = 8;
    public static final int RPM_I18NSTRING_TYPE = 9;
    public static final int RPM_BIN_TYPE = 7;
    public static final int RPMTAG_DIRINDEXES = 1116;
    public static final int RPMTAG_BASENAMES = 1117;
    public static final int RPMTAG_DIRNAMES = 1118;
    public static final int RPMTAG_PGP = 259;
    public static final int RPMTAG_SIGMD5 = 261;
    public static final int RPMTAG_NAME = 1000;
    public static final int RPMTAG_VERSION = 1001;
    public static final int RPMTAG_RELEASE = 1002;
    public static final int RPMTAG_EPOCH = 1003;
    public static final int RPMTAG_SUMMARY = 1004;
    public static final int RPMTAG_INSTALLTIME = 1008;
    public static final int RPMTAG_SIZE = 1009;
    public static final int RPMTAG_VENDOR = 1011;
    public static final int RPMTAG_LICENSE = 1014;
    public static final int RPMTAG_ARCH = 1022;
    public static final int RPMTAG_FILESIZES = 1028;
    public static final int RPMTAG_FILEMODES = 1030;
    public static final int RPMTAG_FILEDIGESTS = 1035;
    public static final int RPMTAG_FILEFLAGS = 1037;
    public static final int RPMTAG_FILEUSERNAME = 1039;
    public static final int RPMTAG_FILEGROUPNAME = 1040;
    public static final int RPMTAG_SOURCERPM = 1044;
    public static final int RPMTAG_PROVIDENAME = 1047;
    public static final int RPMTAG_REQUIRENAME = 1049;
    public static final int RPMTAG_FILEDIGESTALGO = 5011;
    public static final int RPMTAG_MODULARITYLABEL = 5096;


    // Array of type sizes
    public static final int[] TYPE_SIZES = {
            0,  /*!< RPM_NULL_TYPE */
            1,  /*!< RPM_CHAR_TYPE */
            1,  /*!< RPM_INT8_TYPE */
            2,  /*!< RPM_INT16_TYPE */
            4,  /*!< RPM_INT32_TYPE */
            8,  /*!< RPM_INT64_TYPE */
            -1, /*!< RPM_STRING_TYPE */
            1,  /*!< RPM_BIN_TYPE */
            -1, /*!< RPM_STRING_ARRAY_TYPE */
            -1, /*!< RPM_I18NSTRING_TYPE */
            0,
            0,
            0,
            0,
            0,
            0
    };

    // Array of type alignments
    public static final long[] TYPE_ALIGN = {
            1, /*!< RPM_NULL_TYPE */
            1, /*!< RPM_CHAR_TYPE */
            1, /*!< RPM_INT8_TYPE */
            2, /*!< RPM_INT16_TYPE */
            4, /*!< RPM_INT32_TYPE */
            8, /*!< RPM_INT64_TYPE */
            1, /*!< RPM_STRING_TYPE */
            1, /*!< RPM_BIN_TYPE */
            1, /*!< RPM_STRING_ARRAY_TYPE */
            1, /*!< RPM_I18NSTRING_TYPE */
            0,
            0,
            0,
            0,
            0,
            0
    };

    // Constants for region tag
    public static final int REGION_TAG_COUNT = Integer.BYTES * 4;
    public static final int REGION_TAG_TYPE = RPM_BIN_TYPE;

    // Maximum bytes for header
    public static final int HEADER_MAX_BYTES = 256 * 1024 * 1024;
}

