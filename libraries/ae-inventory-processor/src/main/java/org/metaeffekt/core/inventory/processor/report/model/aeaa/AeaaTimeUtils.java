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
package org.metaeffekt.core.inventory.processor.report.model.aeaa;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Mirrors structure of <code>com.metaeffekt.artifact.analysis.utils.TimeUtils</code>
 * until separation of inventory report generation from ae core inventory processor.
 */
public abstract class AeaaTimeUtils {

    public static final DateTimeFormatter HUMAN_READABLE_TIMESTAMP_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd@HH'h'mm'm'ss's'");

    public static long parseHumanFormatTime(String time) {
        if (time == null) return -1;
        if (time.isEmpty() || time.equals("now")) {
            return AeaaTimeUtils.utcNow();
        }

        String[] parts = time.replace("ago", "").replace("last", "").split("(,|and)");
        long value = 0;
        boolean hadValidPart = false;

        for (String part : parts) {
            String[] split = part.trim().split(" ");
            if (split.length == 2) {
                long l = parseHumanFormatTime(split[0], split[1]);
                if (l > 0) {
                    value += l;
                    hadValidPart = true;
                }
            } else if (split.length == 1) {
                long l = parseHumanFormatTime("1", split[0]);
                if (l > 0) {
                    value += l;
                    hadValidPart = true;
                }
            }
        }

        if (!hadValidPart) {
            return -1;
        }
        return value;
    }

    private static long parseHumanFormatTime(String time, String unit) {
        time = time.replaceAll("[^0-9]", "");
        if (time.isEmpty()) {
            return -1;
        }
        int parsedTime = Integer.parseInt(time);

        if (unit.contains("year")) {
            return (long) parsedTime * 365 * 24 * 60 * 60 * 1000;
        } else if (unit.contains("month")) {
            return (long) parsedTime * 30 * 24 * 60 * 60 * 1000;
        } else if (unit.contains("week")) {
            return (long) parsedTime * 7 * 24 * 60 * 60 * 1000;
        } else if (unit.contains("day")) {
            return (long) parsedTime * 24 * 60 * 60 * 1000;
        } else if (unit.contains("hour")) {
            return (long) parsedTime * 60 * 60 * 1000;
        } else if (unit.contains("minute")) {
            return (long) parsedTime * 60 * 1000;
        } else if (unit.contains("second")) {
            return (long) parsedTime * 1000;
        }

        return -1;
    }

    public static long parseTimeFromInput(String input) {
        if (input == null) return 0;

        if (input.matches("[0-9]+")) {
            return Long.parseLong(input);
        }

        if (input.equals("now")) {
            return AeaaTimeUtils.utcNow();
        }

        long humanTime = AeaaTimeUtils.parseHumanFormatTime(input);
        if (humanTime != -1) {
            return AeaaTimeUtils.utcNow() - humanTime;
        }

        return AeaaTimeUtils.timestampFromNormalizedDate(AeaaTimeUtils.normalizeDate(input));
    }

    public static long timestampFromNormalizedDate(String date) {
        if (!date.contains("-")) {
            return 0;
        }

        final String[] dateTimeParts = date.split("[T ]");
        String datePart = dateTimeParts[0];
        final String timePart = dateTimeParts.length > 1 ? dateTimeParts[1] : "00:00:00";

        if (datePart.contains(" ")) {
            datePart = datePart.substring(0, datePart.indexOf(" "));
        }

        final String[] dateParts = datePart.split("-");
        final String[] timeParts = timePart.split(":");

        final GregorianCalendar calendar = new GregorianCalendar(TimeZone.getTimeZone("UTC"));
        calendar.set(
                Integer.parseInt(dateParts[0]),
                Integer.parseInt(dateParts[1]) - 1,
                Integer.parseInt(dateParts[2]),
                Integer.parseInt(timeParts[0]),
                Integer.parseInt(timeParts[1]),
                Integer.parseInt(timeParts[2])
        );

        calendar.set(Calendar.MILLISECOND, 0);

        return calendar.getTimeInMillis();
    }

    private final static List<SimpleDateFormat> DATE_FORMATS = Arrays.asList(
            nonLenientUtcSimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
            createUtcSimpleDateFormat("dd MMMMM yyyy", Locale.GERMAN), // cert-fr
            createUtcSimpleDateFormat("dd MMMMM yyyy", Locale.FRANCE),
            createUtcSimpleDateFormat("dd MMMMM yyyy", Locale.ENGLISH),
            nonLenientUtcSimpleDateFormat("dd MMMMM yyyy"),
            nonLenientUtcSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),

            nonLenientUtcSimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
            nonLenientUtcSimpleDateFormat("yyyy-MM-dd'T'HH:mm"),
            nonLenientUtcSimpleDateFormat("yyyy-MM-dd HH:mm"),

            nonLenientUtcSimpleDateFormat("dd-MM-yyyy'T'HH:mm:ss"),
            nonLenientUtcSimpleDateFormat("dd-MM-yyyy'T'HH:mm"),
            nonLenientUtcSimpleDateFormat("dd-MM-yyyy HH:mm:ss"),
            nonLenientUtcSimpleDateFormat("dd-MM-yyyy HH:mm"),

            nonLenientUtcSimpleDateFormat("MM-dd-yyyy'T'HH:mm:ss"),
            nonLenientUtcSimpleDateFormat("MM-dd-yyyy'T'HH:mm"),
            nonLenientUtcSimpleDateFormat("MM-dd-yyyy HH:mm:ss"),
            nonLenientUtcSimpleDateFormat("MM-dd-yyyy HH:mm"),

            nonLenientUtcSimpleDateFormat("MM/dd/yyyy'T'HH:mm:ss"),
            nonLenientUtcSimpleDateFormat("MM/dd/yyyy'T'HH:mm"),
            nonLenientUtcSimpleDateFormat("MM/dd/yyyy HH:mm:ss"),
            nonLenientUtcSimpleDateFormat("MM/dd/yyyy HH:mm"),

            nonLenientUtcSimpleDateFormat("dd/MM/yyyy'T'HH:mm:ss"),
            nonLenientUtcSimpleDateFormat("dd/MM/yyyy'T'HH:mm"),
            nonLenientUtcSimpleDateFormat("dd/MM/yyyy HH:mm:ss"),
            nonLenientUtcSimpleDateFormat("dd/MM/yyyy HH:mm"),

            nonLenientUtcSimpleDateFormat("yyyy/MM/dd'T'HH:mm:ss"),
            nonLenientUtcSimpleDateFormat("yyyy/MM/dd'T'HH:mm"),
            nonLenientUtcSimpleDateFormat("yyyy/MM/dd HH:mm:ss"),
            nonLenientUtcSimpleDateFormat("yyyy/MM/dd HH:mm"),

            nonLenientUtcSimpleDateFormat("yyyy-MM-dd"),
            nonLenientUtcSimpleDateFormat("yyyy-MM"),

            nonLenientUtcSimpleDateFormat("dd-MM-yyyy"),
            nonLenientUtcSimpleDateFormat("MM-dd-yyyy"),

            nonLenientUtcSimpleDateFormat("MM/dd/yyyy"),
            nonLenientUtcSimpleDateFormat("dd/MM/yyyy"),
            createUtcSimpleDateFormat("yyyy/MM/dd"),

            nonLenientUtcSimpleDateFormat("yyyy.MM.dd"),
            nonLenientUtcSimpleDateFormat("dd.MM.yyyy"),
            nonLenientUtcSimpleDateFormat("MM.dd.yyyy"),

            createUtcSimpleDateFormat("dd.MM.yyyy'T'HH:mm:ss"),
            createUtcSimpleDateFormat("dd.MM.yyyy'T'HH:mm"),
            createUtcSimpleDateFormat("dd.MM.yyyy HH:mm:ss"),
            createUtcSimpleDateFormat("dd.MM.yyyy HH:mm"),

            nonLenientUtcSimpleDateFormat("yyyy")
    );

    private static final List<SimpleDateFormat> FORMATS_WITH_T = new ArrayList<>();
    private static final List<SimpleDateFormat> FORMATS_WITH_SLASH = new ArrayList<>();
    private static final List<SimpleDateFormat> FORMATS_WITH_DASH = new ArrayList<>();
    private static final List<SimpleDateFormat> FORMATS_TEXT = new ArrayList<>();
    private static final List<SimpleDateFormat> FORMATS_GENERIC = new ArrayList<>();

    static {
        for (SimpleDateFormat sdf : DATE_FORMATS) {
            String pattern = sdf.toPattern();
            if (pattern.contains("'T'")) {
                FORMATS_WITH_T.add(sdf);
            } else if (pattern.contains("/")) {
                FORMATS_WITH_SLASH.add(sdf);
            } else if (pattern.contains("-")) {
                FORMATS_WITH_DASH.add(sdf);
            } else if (pattern.contains("MMM") || pattern.contains("EEE")) {
                FORMATS_TEXT.add(sdf);
            } else {
                FORMATS_GENERIC.add(sdf);
            }
        }
        sortFormatsByPriority(FORMATS_WITH_T);
        sortFormatsByPriority(FORMATS_WITH_SLASH);
        sortFormatsByPriority(FORMATS_WITH_DASH);
        sortFormatsByPriority(FORMATS_TEXT);
        sortFormatsByPriority(FORMATS_GENERIC);
    }

    private static void sortFormatsByPriority(List<SimpleDateFormat> formats) {
        formats.sort((a, b) -> Integer.compare(b.toPattern().length(), a.toPattern().length()));
    }

    public static Date tryParse(String dateString) {
        if (dateString == null) return null;

        if (dateString.matches("\\d{5,}")) {
            return new Date(Long.parseLong(dateString));
        }

        List<SimpleDateFormat> candidates = new ArrayList<>(8);

        if (dateString.contains("T")) {
            candidates.addAll(FORMATS_WITH_T);
        } else if (dateString.contains("/")) {
            candidates.addAll(FORMATS_WITH_SLASH);
        } else if (dateString.contains("-")) {
            candidates.addAll(FORMATS_WITH_DASH);
        } else if (dateString.matches(".*[a-zA-Z].*")) {
            candidates.addAll(FORMATS_TEXT);
        } else {
            candidates.addAll(FORMATS_GENERIC);
        }

        synchronized (DATE_FORMATS) {
            for (SimpleDateFormat formatter : candidates) {
                try {
                    return formatter.parse(dateString);
                } catch (ParseException ignored) {
                }
            }
        }

        return null;
    }

    private static SimpleDateFormat nonLenientSimpleDateFormat(String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setLenient(false);
        return sdf;
    }

    public static String normalizeDate(String string) {
        if (string == null) return "n.a.";

        Date parsedDate = AeaaTimeUtils.tryParse(string);

        if (parsedDate != null) {
            return formatNormalizedDateEn(parsedDate);
        } else if (string.contains("T")) {
            return string.substring(0, string.indexOf("T"));
        }

        return string;
    }

    private final static SimpleDateFormat NORMALIZED_DATE_EN_PATTERN = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    private final static SimpleDateFormat NORMALIZED_DATE_DE_PATTERN = new SimpleDateFormat("dd-MM-yyyy HH:mm:ss");
    private final static SimpleDateFormat NORMALIZED_DATE_ONLY_DATE_PATTERN = new SimpleDateFormat("yyyy-MM-dd");

    static {
        NORMALIZED_DATE_EN_PATTERN.setTimeZone(TimeZone.getTimeZone("UTC"));
        NORMALIZED_DATE_DE_PATTERN.setTimeZone(TimeZone.getTimeZone("UTC"));
        NORMALIZED_DATE_ONLY_DATE_PATTERN.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public static String formatNormalizedDateEn(Date date) {
        if (date == null) return "n.a.";
        return NORMALIZED_DATE_EN_PATTERN
                .format(date)
                .replace(" 00:00:00", "")
                .replaceAll(":00$", "");
    }

    public static String formatNormalizedDateDe(Date date) {
        if (date == null) return "n.a.";
        return NORMALIZED_DATE_DE_PATTERN
                .format(date)
                .replace(" 00:00:00", "")
                .replaceAll(":00$", "");
    }

    public static String formatNormalizedDateOnlyDate(Date date) {
        return NORMALIZED_DATE_ONLY_DATE_PATTERN
                .format(date);
    }

    public static String formatTimeDiff(long diff) {
        if (diff < 1000) {
            return diff + "ms";
        } else if (diff < 60 * 1000) {
            long s = diff / 1000;
            long ms = diff % 1000;
            return s + "s" + (ms == 0 ? "" : " " + ms + "ms");
        } else if (diff < 60 * 60 * 1000) {
            long m = diff / 1000 / 60;
            long s = diff / 1000 % 60;
            return m + "m" + (s == 0 ? "" : " " + s + "s");
        } else if (diff < 24 * 60 * 60 * 1000) {
            long h = diff / 1000 / 60 / 60;
            long m = diff / 1000 / 60 % 60;
            return h + "h" + (m == 0 ? "" : " " + m + "m");
        } else {
            long d = diff / 1000 / 60 / 60 / 24;
            long h = diff / 1000 / 60 / 60 % 24;
            return d + "d" + (h == 0 ? "" : " " + h + "h");
        }
    }

    public static Date tryParse(Object date) {
        if (date instanceof Date) {
            return (Date) date;
        } else if (date instanceof Number) {
            return new Date(((Number) date).longValue());
        } else if (date instanceof String) {
            return AeaaTimeUtils.tryParse((String) date);
        } else {
            return null;
        }
    }

    public static long utcNow() {
        return Instant.now().toEpochMilli();
    }


    public static String formatTimeUntilOrAgoDefault(long timeUntil) {
        return AeaaTimeUtils.formatTimeUntilOrAgo(timeUntil, "", "in", "ago", "", " and ", "no date provided");
    }


    public static String formatTimeNoSuffixPrefix(long timeUntil) {
        return AeaaTimeUtils.formatTimeUntilOrAgo(timeUntil, "", "", "", "", " and ", "no date provided");
    }

    public static String formatTimeUntilOrAgo(
            long timeUntil,
            String prefixBefore,
            String prefixAfter,
            String suffixBefore,
            String suffixAfter,
            String joinWord,
            String neverWord
    ) {
        String prefixUsed = timeUntil < 0 ? prefixBefore : prefixAfter;
        String suffixUsed = timeUntil < 0 ? suffixBefore : suffixAfter;

        prefixUsed = prefixUsed.isEmpty() ? "" : prefixUsed + " ";
        suffixUsed = suffixUsed.isEmpty() ? "" : " " + suffixUsed;

        final long absTime = Math.abs(timeUntil);
        if (timeUntil == Long.MAX_VALUE) {
            return neverWord;
        } else if (absTime < 1000 * 10) {
            return "now";
        }

        final String format = prefixUsed + "%d %s" + joinWord + "%d %s" + suffixUsed;
        final String formatZero = prefixUsed + "%d %s" + suffixUsed;

        final long years = absTime / (1000L * 60 * 60 * 24 * 365);
        final long months = absTime / (1000L * 60 * 60 * 24 * 30);
        final long weeks = absTime / (1000L * 60 * 60 * 24 * 7);
        final long days = absTime / (1000L * 60 * 60 * 24);
        final long hours = absTime / (1000L * 60 * 60);
        final long minutes = absTime / (1000L * 60);
        final long seconds = absTime / (1000L);

        if (years > 0) {
            return months % 12 == 0 ?
                    String.format(formatZero, years, (years > 1 ? "years" : "year")) :
                    String.format(format, years, (years > 1 ? "years" : "year"), months % 12, (months % 12 == 1 ? "month" : "months"));
        } else if (months > 0) {
            return weeks % 4 == 0 ?
                    String.format(formatZero, months, (months > 1 ? "months" : "month")) :
                    String.format(format, months, (months > 1 ? "months" : "month"), weeks % 4, (weeks % 4 == 1 ? "week" : "weeks"));
        } else if (weeks > 0) {
            return days % 7 == 0 ?
                    String.format(formatZero, weeks, (weeks > 1 ? "weeks" : "week")) :
                    String.format(format, weeks, (weeks > 1 ? "weeks" : "week"), days % 7, (days % 7 == 1 ? "day" : "days"));
        } else if (days > 0) {
            return hours % 24 == 0 ?
                    String.format(formatZero, days, (days > 1 ? "days" : "day")) :
                    String.format(format, days, (days > 1 ? "days" : "day"), hours % 24, (hours % 24 == 1 ? "hour" : "hours"));
        } else if (hours > 0) {
            return minutes % 60 == 0 ?
                    String.format(formatZero, hours, (hours > 1 ? "hours" : "hour")) :
                    String.format(format, hours, (hours > 1 ? "hours" : "hour"), minutes % 60, (minutes % 60 == 1 ? "minute" : "minutes"));
        } else if (minutes > 0) {
            return seconds % 60 == 0 ?
                    String.format(formatZero, minutes, (minutes > 1 ? "minutes" : "minute")) :
                    String.format(format, minutes, (minutes > 1 ? "minutes" : "minute"), seconds % 60, (seconds % 60 == 1 ? "second" : "seconds"));
        } else {
            return String.format(formatZero, seconds, "seconds");
        }
    }

    public static String createHumanReadableTimestamp() {
        return ZonedDateTime.now(ZoneOffset.UTC).format(HUMAN_READABLE_TIMESTAMP_FORMATTER);
    }

    private static SimpleDateFormat createUtcSimpleDateFormat(String pattern) {
        final SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    }

    private static SimpleDateFormat createUtcSimpleDateFormat(String pattern, Locale locale) {
        final SimpleDateFormat sdf = new SimpleDateFormat(pattern, locale);
        sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
        return sdf;
    }

    private static SimpleDateFormat nonLenientUtcSimpleDateFormat(String pattern) {
        final SimpleDateFormat sdf = createUtcSimpleDateFormat(pattern);
        sdf.setLenient(false);
        return sdf;
    }
}
