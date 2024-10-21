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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.*;

import static org.metaeffekt.core.inventory.processor.patterns.contributors.util.bdb.RpmConstants.REGION_TAG_COUNT;

public class RPMDBUtils {

    public static List<IndexEntry> headerImport(byte[] data) throws IOException {
        HdrBlob blob = hdrblobInit(data);
        return hdrblobImport(blob, data);
    }

    public static HdrBlob hdrblobInit(byte[] data) throws IOException {
        HdrBlob blob = new HdrBlob();
        ByteArrayInputStream input = new ByteArrayInputStream(data);
        DataInputStream dataInput = new DataInputStream(input);

        int sizeOfInt = Integer.BYTES;

        blob.setIl(readInt(dataInput, ByteOrder.BIG_ENDIAN));
        blob.setDl(readInt(dataInput, ByteOrder.BIG_ENDIAN));
        blob.setDataStart(sizeOfInt + sizeOfInt + blob.getIl() * EntryInfo.BYTE_SIZE);
        blob.setPvlen(blob.getDataStart() + blob.getDl());
        blob.setDataEnd(blob.getDataStart() + blob.getDl());

        if (blob.getIl() < 1) {
            throw new IOException("region no tags error");
        }

        for (int i = 0; i < blob.getIl(); i++) {
            EntryInfo entryInfo = readEntryInfo(dataInput, ByteOrder.LITTLE_ENDIAN);
            blob.getPeList().add(entryInfo);
        }

        if (blob.getPvlen() >= RpmConstants.HEADER_MAX_BYTES) {
            throw new IOException("blob size BAD, 8 + 16 * il(" + blob.getIl() + ") + dl(" + blob.getDl() + ")");
        }

        hdrblobVerifyRegion(blob, data);
        try {
            hdrblobVerifyInfo(blob, data);
        } catch (IOException e) {
            throw new IOException("hdrblobVerifyInfo failed", e);
        }

        return blob;
    }

    public static List<IndexEntry> hdrblobImport(HdrBlob blob, byte[] data) throws IOException {
        List<IndexEntry> indexEntries;
        List<IndexEntry> dribbleIndexEntries;

        EntryInfo entry = ei2h(blob.getPeList().get(0));
        int rdlen;

        if (entry.getTag() >= RpmConstants.RPMTAG_HEADERI18NTABLE) {
            RdlenWrapper dl = new RdlenWrapper();
            dl.value = 0;
            indexEntries = regionSwab(data, blob.getPeList(), dl, blob.getDataStart(), blob.getDataEnd());
            rdlen = dl.value;
        } else {
            int ril = blob.getRil();
            if (entry.getOffset() == 0) {
                ril = blob.getIl();
            }

            RdlenWrapper dl = new RdlenWrapper();
            dl.value = 0;
            indexEntries = regionSwab(data, blob.getPeList().subList(1, ril), dl, blob.getDataStart(), blob.getDataEnd());
            rdlen = dl.value;
            if (rdlen < 0) {
                throw new IOException("invalid region length");
            }

            if (blob.getRil() < blob.getPeList().size() - 1) {
                dribbleIndexEntries = regionSwab(data, blob.getPeList().subList(ril, blob.getPeList().size()), dl, blob.getDataStart(), blob.getDataEnd());
                Map<Integer, IndexEntry> uniqueTagMap = new HashMap<>();
                for (IndexEntry indexEntry : indexEntries) {
                    uniqueTagMap.put(indexEntry.getInfo().getTag(), indexEntry);
                }
                for (IndexEntry indexEntry : dribbleIndexEntries) {
                    uniqueTagMap.put(indexEntry.getInfo().getTag(), indexEntry);
                }
                indexEntries = new ArrayList<>(uniqueTagMap.values());
                rdlen = dl.value;
            }
            rdlen += REGION_TAG_COUNT;
        }

        if (rdlen != blob.getDl()) {
            throw new IOException("the calculated length (" + rdlen + ") is different from the data length (" + blob.getDl() + ")");
        }

        return indexEntries;
    }

    public static void hdrblobVerifyInfo(HdrBlob blob, byte[] data) throws IOException {
        int end = 0;
        int peOffset = blob.getRegionTag() != 0 ? 1 : 0;

        for (EntryInfo pe : blob.getPeList().subList(peOffset, blob.getPeList().size())) {
            EntryInfo info = ei2h(pe);

            if (end > info.getOffset()) {
                throw new IOException("invalid offset info: " + info);
            }

            if (hdrchkTag(info.getTag())) {
                throw new IOException("invalid tag info: " + info);
            }

            if (hdrchkType(info.getType())) {
                throw new IOException("invalid type info: " + info);
            }

            if (hdrchkAlign(info.getType(), info.getOffset())) {
                throw new IOException("invalid align info: " + info);
            }

            if (hdrchkRange(blob.getDl(), info.getOffset())) {
                throw new IOException("invalid range info: " + info);
            }

            int length = dataLength(data, info.getType(), info.getCount(), blob.getDataStart() + info.getOffset(), blob.getDataEnd());
            end = info.getOffset() + length;

            if (hdrchkRange(blob.getDl(), end) || length <= 0) {
                throw new IOException("invalid data length info: " + info);
            }
        }
    }

    public static void hdrblobVerifyRegion(HdrBlob blob, byte[] data) throws IOException {
        EntryInfo einfo = ei2h(blob.getPeList().get(0));
        int regionTag = 0;

        if (einfo.getTag() == RpmConstants.RPMTAG_HEADERIMAGE ||
                einfo.getTag() == RpmConstants.RPMTAG_HEADERSIGNATURES ||
                einfo.getTag() == RpmConstants.RPMTAG_HEADERIMMUTABLE) {
            regionTag = einfo.getTag();
        }

        if (einfo.getTag() != regionTag) {
            return;
        }

        if (!(einfo.getType() == RpmConstants.REGION_TAG_TYPE && einfo.getCount() == REGION_TAG_COUNT)) {
            throw new IOException("invalid region tag");
        }

        if (hdrchkRange(blob.getDl(), einfo.getOffset() + REGION_TAG_COUNT)) {
            throw new IOException("invalid region offset");
        }

        int regionEnd = blob.getDataStart() + einfo.getOffset();
        if (regionEnd > data.length || regionEnd + REGION_TAG_COUNT > data.length) {
            throw new IOException("invalid region offset");
        }

        byte[] subArr = Arrays.copyOfRange(data, regionEnd, regionEnd + REGION_TAG_COUNT);

        EntryInfo trailer = readEntryInfo(new DataInputStream(new ByteArrayInputStream(subArr)), ByteOrder.LITTLE_ENDIAN);
        blob.setRdl(regionEnd + REGION_TAG_COUNT - blob.getDataStart());

        if (regionTag == RpmConstants.RPMTAG_HEADERSIGNATURES && einfo.getTag() == RpmConstants.RPMTAG_HEADERIMAGE) {
            einfo.setTag(RpmConstants.RPMTAG_HEADERSIGNATURES);
        }

        if (!(einfo.getTag() == regionTag && einfo.getType() == RpmConstants.REGION_TAG_TYPE && einfo.getCount() == REGION_TAG_COUNT)) {
            throw new IOException("invalid region trailer");
        }

        einfo = ei2h(trailer);
        einfo.setOffset(-einfo.getOffset());
        int newRil = einfo.getOffset() / EntryInfo.BYTE_SIZE;
        blob.setRil(newRil);
        if (einfo.getOffset() % REGION_TAG_COUNT != 0 || hdrchkRange(blob.getDl(), blob.getRil()) || hdrchkRange(blob.getDl(), blob.getRdl())) {
            throw new IOException(String.format("invalid region size, region %d", regionTag));
        }
        blob.setRegionTag(regionTag);
    }

    public static List<IndexEntry> regionSwab(byte[] data, List<EntryInfo> peList, RdlenWrapper dl, int dataStart, int dataEnd) throws IOException {
        List<IndexEntry> indexEntries = new ArrayList<>();
        int rdlen = dl.value;

        for (int i = 0; i < peList.size(); i++) {
            EntryInfo pe = peList.get(i);
            IndexEntry indexEntry = new IndexEntry();
            indexEntry.setInfo(ei2h(pe));
            EntryInfo info = indexEntry.getInfo();

            int start = dataStart + info.getOffset();
            if (start >= dataEnd) {
                throw new IOException("invalid data offset");
            }

            if (i < peList.size() - 1 && RpmConstants.TYPE_SIZES[(int) info.getType()] == -1) {
                indexEntry.setLength(htonl(peList.get(i + 1).getOffset()) - info.getOffset());
            } else {
                indexEntry.setLength(dataLength(data, info.getType(), info.getCount(), start, dataEnd));
            }

            if (indexEntry.getLength() < 0) {
                throw new IOException("invalid data length");
            }

            int end = start + indexEntry.getLength();
            if (start > data.length || end > data.length) {
                throw new IOException("invalid data length");
            }

            byte[] entryData = Arrays.copyOfRange(data, start, end);
            indexEntry.setData(entryData);

            rdlen += indexEntry.getLength() + alignDiff(info.getType(), Integer.toUnsignedLong(rdlen));
            indexEntry.setRdlen(indexEntry.getLength() + alignDiff(info.getType(), Integer.toUnsignedLong(rdlen)));
            dl.value = rdlen;
            indexEntries.add(indexEntry);
        }
        return indexEntries;
    }

    private static int readInt(DataInputStream input, ByteOrder order) throws IOException {
        byte[] buffer = new byte[Integer.BYTES];
        input.readFully(buffer);
        return ByteBuffer.wrap(buffer).order(order).getInt();
    }

    private static EntryInfo readEntryInfo(DataInputStream input, ByteOrder order) throws IOException {
        int tag = readInt(input, order);
        long type = readInt(input, order);
        int offset = readInt(input, order);
        long count = readInt(input, order);
        return new EntryInfo(tag, type, offset, count);
    }

    private static EntryInfo ei2h(EntryInfo pe) {
        return new EntryInfo(htonl(pe.getTag()), htonlU(pe.getType()), htonl(pe.getOffset()), htonlU(pe.getCount()));
    }


    /**
     * Converts a signed 32-bit integer from host to network byte order (big-endian).
     *
     * @param val the signed 32-bit integer to convert
     * @return the converted integer in network byte order
     */
    public static int htonl(int val) {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt(val);
            buf.write(byteBuffer.array());

            ByteArrayInputStream input = new ByteArrayInputStream(buf.toByteArray());
            ByteBuffer resultBuffer = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.BIG_ENDIAN);
            byte[] temp = new byte[Integer.BYTES];
            if (input.read(temp) != Integer.BYTES) {
                throw new IOException("Failed to read integer");
            }
            resultBuffer.put(temp);
            ((java.nio.Buffer) resultBuffer).flip();
            return resultBuffer.getInt();
        } catch (IOException e) {
            return 0;
        }
    }

    /**
     * Converts an unsigned 32-bit integer from host to network byte order (big-endian).
     * The result is represented as a long to accommodate the unsigned 32-bit integer.
     *
     * @param val the unsigned 32-bit integer to convert, represented as a signed int
     * @return the converted integer in network byte order, represented as a long
     */
    public static long htonlU(long val) {
        try {
            ByteArrayOutputStream buf = new ByteArrayOutputStream();
            ByteBuffer byteBuffer = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.LITTLE_ENDIAN).putInt((int) val);
            buf.write(byteBuffer.array());

            ByteArrayInputStream input = new ByteArrayInputStream(buf.toByteArray());
            ByteBuffer resultBuffer = ByteBuffer.allocate(Integer.BYTES).order(ByteOrder.BIG_ENDIAN);
            byte[] temp = new byte[Integer.BYTES];
            if (input.read(temp) != Integer.BYTES) {
                throw new IOException("Failed to read integer");
            }
            resultBuffer.put(temp);
            ((java.nio.Buffer) resultBuffer).flip();
            return Integer.toUnsignedLong(resultBuffer.getInt());
        } catch (IOException e) {
            return 0L;
        }
    }

    private static boolean hdrchkTag(int tag) {
        return tag < RpmConstants.RPMTAG_HEADERI18NTABLE;
    }

    private static boolean hdrchkType(long type) {
        return type < RpmConstants.RPM_MIN_TYPE || type > RpmConstants.RPM_MAX_TYPE;
    }

    private static boolean hdrchkAlign(long type, int offset) {
        return (offset & (RpmConstants.TYPE_ALIGN[(int) type] - 1)) != 0;
    }

    private static boolean hdrchkRange(int dl, int offset) {
        return offset < 0 || offset > dl;
    }

    private static int dataLength(byte[] data, long type, long count, int start, int dataEnd) {
        int length;

        switch ((int) type) {
            case RpmConstants.RPM_STRING_TYPE:
                if (count != 1) return -1;
                length = strtaglen(data, 1, start, dataEnd);
                break;
            case RpmConstants.RPM_STRING_ARRAY_TYPE:
            case RpmConstants.RPM_I18NSTRING_TYPE:
                length = strtaglen(data, count, start, dataEnd);
                break;
            default:
                if (RpmConstants.TYPE_SIZES[(int) type] == -1) return -1;
                length = RpmConstants.TYPE_SIZES[(int) type & 0xf] * (int) count;
                if (length < 0 || start + length > dataEnd) return -1;
                break;
        }
        return length;
    }

    private static int alignDiff(long type, long alignSize) {
        int typeSize = RpmConstants.TYPE_SIZES[(int) type];
        if (typeSize > 1) {
            int diff = typeSize - ((int) alignSize % typeSize);
            if (diff != typeSize) return diff;
        }
        return 0;
    }

    private static int strtaglen(byte[] data, long count, int start, int dataEnd) {
        int length = 0;
        if (start >= dataEnd || dataEnd > data.length) return -1;
        for (long c = count; c > 0; c--) {
            int offset = start + length;
            if (offset > data.length) return -1;
            int index = indexOf(data, offset, dataEnd, (byte) 0x00);
            if (index == -1) {
                return -1;
            }
            length += (index - offset) + 1;
        }
        return length;
    }

    /**
     * Returns the index of the first occurrence of the specified target byte in the data array.
     * @param data the data array to search
     * @param start the start index of the search
     * @param end the end index of the search
     * @param target the target byte to search for
     * @return the index of the first occurrence of the target byte, or -1 if the target byte is not found
     */
    private static int indexOf(byte[] data, int start, int end, byte target) {
        for (int i = start; i < end; i++) {
            if (data[i] == target) {
                return i;
            }
        }
        return -1;
    }

    public static List<Integer> parseInt32Array(byte[] data, int arraySize) throws IOException {
        int length = arraySize / Integer.BYTES;
        List<Integer> values = new ArrayList<>(length);
        ByteArrayInputStream reader = new ByteArrayInputStream(data);
        byte[] buffer = new byte[Integer.BYTES];

        for (int i = 0; i < length; i++) {
            if (reader.read(buffer) != Integer.BYTES) {
                throw new IOException("Failed to read binary data");
            }
            values.add(ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN).getInt());
        }

        return values;
    }

    public static int parseInt32(byte[] data) throws IOException {
        if (data.length != Integer.BYTES) {
            throw new IOException("Invalid data length for an int32");
        }
        ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN);
        return buffer.getInt();
    }

    public static List<Short> uint16Array(byte[] data, int arraySize) throws IOException {
        int length = arraySize / Short.BYTES;
        List<Short> values = new ArrayList<>(length);
        ByteArrayInputStream reader = new ByteArrayInputStream(data);
        byte[] buffer = new byte[Short.BYTES];

        for (int i = 0; i < length; i++) {
            if (reader.read(buffer) != Short.BYTES) {
                throw new IOException("Failed to read binary data");
            }
            values.add(ByteBuffer.wrap(buffer).order(ByteOrder.BIG_ENDIAN).getShort());
        }

        return values;
    }

    public static List<String> parseStringArray(byte[] data) {
        String rawString = new String(data).trim();
        String[] splitStrings = rawString.split("\u0000");
        List<String> stringList = new ArrayList<>();
        for (String s : splitStrings) {
            if (!s.isEmpty()) {
                stringList.add(s);
            }
        }
        return stringList;
    }

    public static PackageInfo getNEVRA(List<IndexEntry> indexEntries) throws IOException {
        PackageInfo pkgInfo = new PackageInfo();

        for (IndexEntry ie : indexEntries) {
            switch (ie.getInfo().getTag()) {
                case RpmConstants.RPMTAG_DIRINDEXES:
                    if (ie.getInfo().getType() != RpmConstants.RPM_INT32_TYPE) {
                        throw new IOException("invalid tag dir indexes");
                    }
                    pkgInfo.setDirIndexes(parseInt32Array(ie.getData(), ie.getLength()));
                    break;

                case RpmConstants.RPMTAG_DIRNAMES:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_ARRAY_TYPE) {
                        throw new IOException("invalid tag dir names");
                    }
                    pkgInfo.setDirNames(parseStringArray(ie.getData()));
                    break;

                case RpmConstants.RPMTAG_BASENAMES:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_ARRAY_TYPE) {
                        throw new IOException("invalid tag base names");
                    }
                    pkgInfo.setBaseNames(parseStringArray(ie.getData()));
                    break;

                case RpmConstants.RPMTAG_MODULARITYLABEL:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag modularitylabel");
                    }
                    pkgInfo.setModularityLabel(new String(ie.getData()).trim());
                    break;

                case RpmConstants.RPMTAG_NAME:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag name");
                    }
                    pkgInfo.setName(new String(ie.getData()).trim());
                    break;

                case RpmConstants.RPMTAG_EPOCH:
                    if (ie.getInfo().getType() != RpmConstants.RPM_INT32_TYPE) {
                        throw new IOException("invalid tag epoch");
                    }
                    if (ie.getData() != null) {
                        pkgInfo.setEpoch(parseInt32(ie.getData()));
                    }
                    break;

                case RpmConstants.RPMTAG_VERSION:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag version");
                    }
                    pkgInfo.setVersion(new String(ie.getData()).trim());
                    break;

                case RpmConstants.RPMTAG_RELEASE:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag release");
                    }
                    pkgInfo.setRelease(new String(ie.getData()).trim());
                    break;

                case RpmConstants.RPMTAG_ARCH:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag arch");
                    }
                    pkgInfo.setArch(new String(ie.getData()).trim());
                    break;

                case RpmConstants.RPMTAG_SOURCERPM:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag sourcerpm");
                    }
                    String sourceRpm = new String(ie.getData()).trim();
                    pkgInfo.setSourceRpm("(none)".equals(sourceRpm) ? "" : sourceRpm);
                    break;

                case RpmConstants.RPMTAG_PROVIDENAME:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_ARRAY_TYPE) {
                        throw new IOException("invalid tag providename");
                    }
                    pkgInfo.setProvides(parseStringArray(ie.getData()));
                    break;

                case RpmConstants.RPMTAG_REQUIRENAME:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_ARRAY_TYPE) {
                        throw new IOException("invalid tag requirename");
                    }
                    pkgInfo.setRequires(parseStringArray(ie.getData()));
                    break;

                case RpmConstants.RPMTAG_LICENSE:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag license");
                    }
                    String license = new String(ie.getData()).trim();
                    pkgInfo.setLicense("(none)".equals(license) ? "" : license);
                    break;

                case RpmConstants.RPMTAG_VENDOR:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag vendor");
                    }
                    String vendor = new String(ie.getData()).trim();
                    pkgInfo.setVendor("(none)".equals(vendor) ? "" : vendor);
                    break;

                case RpmConstants.RPMTAG_SIZE:
                    if (ie.getInfo().getType() != RpmConstants.RPM_INT32_TYPE) {
                        throw new IOException("invalid tag size");
                    }
                    pkgInfo.setSize(parseInt32(ie.getData()));
                    break;

                case RpmConstants.RPMTAG_FILEDIGESTALGO:
                    if (ie.getInfo().getType() != RpmConstants.RPM_INT32_TYPE) {
                        throw new IOException("invalid tag digest algo");
                    }
                    pkgInfo.setDigestAlgorithm(DigestAlgorithm.fromValue(parseInt32(ie.getData())));
                    break;

                case RpmConstants.RPMTAG_FILESIZES:
                    if (ie.getInfo().getType() != RpmConstants.RPM_INT32_TYPE) {
                        throw new IOException("invalid tag file-sizes");
                    }
                    pkgInfo.setFileSizes(parseInt32Array(ie.getData(), ie.getLength()));
                    break;

                case RpmConstants.RPMTAG_FILEDIGESTS:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_ARRAY_TYPE) {
                        throw new IOException("invalid tag file-digests");
                    }
                    pkgInfo.setFileDigests(parseStringArray(ie.getData()));
                    break;

                case RpmConstants.RPMTAG_FILEMODES:
                    if (ie.getInfo().getType() != RpmConstants.RPM_INT16_TYPE) {
                        throw new IOException("invalid tag file-modes");
                    }
                    pkgInfo.setFileModes(uint16Array(ie.getData(), ie.getLength()));
                    break;

                case RpmConstants.RPMTAG_FILEFLAGS:
                    if (ie.getInfo().getType() != RpmConstants.RPM_INT32_TYPE) {
                        throw new IOException("invalid tag file-flags");
                    }
                    pkgInfo.setFileFlags(parseInt32Array(ie.getData(), ie.getLength()));
                    break;

                case RpmConstants.RPMTAG_FILEUSERNAME:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_ARRAY_TYPE) {
                        throw new IOException("invalid tag usernames");
                    }
                    pkgInfo.setUserNames(parseStringArray(ie.getData()));
                    break;

                case RpmConstants.RPMTAG_FILEGROUPNAME:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_ARRAY_TYPE) {
                        throw new IOException("invalid tag groupnames");
                    }
                    pkgInfo.setGroupNames(parseStringArray(ie.getData()));
                    break;

                case RpmConstants.RPMTAG_SUMMARY:
                    if (ie.getInfo().getType() != RpmConstants.RPM_I18NSTRING_TYPE && ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag summary");
                    }
                    pkgInfo.setSummary(new String(ie.getData()).split("\0")[0]);
                    break;

                case RpmConstants.RPMTAG_INSTALLTIME:
                    if (ie.getInfo().getType() != RpmConstants.RPM_INT32_TYPE) {
                        throw new IOException("invalid tag installtime");
                    }
                    pkgInfo.setInstallTime(parseInt32(ie.getData()));
                    break;

                case RpmConstants.RPMTAG_SIGMD5:
                    pkgInfo.setSigMD5(bytesToHex(ie.getData()));
                    break;

                case RpmConstants.RPMTAG_PGP:
                    pkgInfo.setPgp(parsePGPSignature(ie.getData()));
                    break;

                case RpmConstants.RPMTAG_URL:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag url");
                    }
                    String url = new String(ie.getData()).trim();
                    pkgInfo.setUrl("(none)".equals(url) ? "" : url);
                    break;

                case RpmConstants.RPMTAG_OS:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag os");
                    }
                    String os = new String(ie.getData()).trim();
                    pkgInfo.setOs("(none)".equals(os) ? "" : os);
                    break;

                case RpmConstants.RPMTAG_GROUP:
                    if (ie.getInfo().getType() != RpmConstants.RPM_I18NSTRING_TYPE && ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag group");
                    }
                    String group = new String(ie.getData()).trim();
                    pkgInfo.setGroup("(none)".equals(group) ? "" : group);
                    break;

                case RpmConstants.RPMTAG_DISTRIBUTION:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag distribution");
                    }
                    String distribution = new String(ie.getData()).trim();
                    pkgInfo.setDistribution("(none)".equals(distribution) ? "" : distribution);
                    break;

                case RpmConstants.RPMTAG_DISTTAG:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag disttag");
                    }
                    String disttag = new String(ie.getData()).trim();
                    pkgInfo.setDistTag("(none)".equals(disttag) ? "" : disttag);
                    break;

                case RpmConstants.RPMTAG_DISTURL:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag disturl");
                    }
                    String disturl = new String(ie.getData()).trim();
                    pkgInfo.setDistUrl("(none)".equals(disturl) ? "" : disturl);
                    break;

                case RpmConstants.RPMTAG_PLATFORM:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag platform");
                    }
                    String platform = new String(ie.getData()).trim();
                    pkgInfo.setPlatform("(none)".equals(platform) ? "" : platform);
                    break;

                case RpmConstants.RPMTAG_NEVRA:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag nevra");
                    }
                    pkgInfo.setNevra(new String(ie.getData()).trim());
                    break;

                case RpmConstants.RPMTAG_RPMVERSION:
                    if (ie.getInfo().getType() != RpmConstants.RPM_STRING_TYPE) {
                        throw new IOException("invalid tag rpmversion");
                    }
                    pkgInfo.setRpmVersion(new String(ie.getData()).trim());
                    break;
            }
        }

        return pkgInfo;
    }

    private static String parsePGPSignature(byte[] data) throws IOException {
        DataInputStream r = new DataInputStream(new ByteArrayInputStream(data));
        byte[] buffer = new byte[1];

        r.readFully(buffer);
        // int tag = buffer[0] & 0xFF;
        r.readFully(buffer);
        int signatureType = buffer[0] & 0xFF;
        r.readFully(buffer);
        int version = buffer[0] & 0xFF;

        String pubKeyAlgo;
        String hashAlgo;
        long date;
        byte[] keyId = new byte[8];

        if (signatureType == 0x01) {
            if (version == 0x1c) {
                r.skipBytes(2);
                r.readFully(buffer);
                pubKeyAlgo = getPubKeyAlgo(buffer[0] & 0xFF);
                r.readFully(buffer);
                hashAlgo = getHashAlgo(buffer[0] & 0xFF);
                r.skipBytes(4);
                date = readInt(r, ByteOrder.BIG_ENDIAN);
                r.readFully(keyId);
            } else {
                r.skipBytes(3);
                r.readFully(buffer);
                pubKeyAlgo = getPubKeyAlgo(buffer[0] & 0xFF);
                r.readFully(buffer);
                hashAlgo = getHashAlgo(buffer[0] & 0xFF);
                date = readInt(r, ByteOrder.BIG_ENDIAN);
                r.readFully(keyId);
            }
        } else if (signatureType == 0x02 && version == 0x33) {
            r.skipBytes(2);
            r.readFully(buffer);
            pubKeyAlgo = getPubKeyAlgo(buffer[0] & 0xFF);
            r.readFully(buffer);
            hashAlgo = getHashAlgo(buffer[0] & 0xFF);
            r.skipBytes(17);
            r.readFully(keyId);
            r.skipBytes(2);
            date = readInt(r, ByteOrder.BIG_ENDIAN);
        } else {
            r.skipBytes(3);
            r.readFully(buffer);
            pubKeyAlgo = getPubKeyAlgo(buffer[0] & 0xFF);
            r.readFully(buffer);
            hashAlgo = getHashAlgo(buffer[0] & 0xFF);
            date = readInt(r, ByteOrder.BIG_ENDIAN);
            r.readFully(keyId);
        }

        return String.format("%s/%s, %s, Key ID %s", pubKeyAlgo, hashAlgo, formatDate(date), bytesToHex(keyId));
    }

    private static String getPubKeyAlgo(int value) {
        switch (value) {
            case 0x01:
                return "RSA";
            default:
                return "Unknown";
        }
    }

    private static String getHashAlgo(int value) {
        switch (value) {
            case 0x02:
                return "SHA1";
            case 0x08:
                return "SHA256";
            default:
                return "Unknown";
        }
    }

    private static String formatDate(long epochSeconds) {
        return java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME
                .format(java.time.Instant.ofEpochSecond(epochSeconds)
                        .atZone(java.time.ZoneId.of("UTC")));
    }

    private static String bytesToHex(byte[] bytes) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : bytes) {
            String hex = Integer.toHexString(0xFF & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    static class RdlenWrapper {
        public int value;
    }
}

