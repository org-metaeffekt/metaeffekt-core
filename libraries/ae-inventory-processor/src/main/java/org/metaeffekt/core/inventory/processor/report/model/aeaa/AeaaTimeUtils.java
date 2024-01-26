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
package org.metaeffekt.core.inventory.processor.report.model.aeaa;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;

/**
 * Mirrors structure of <code>com.metaeffekt.artifact.analysis.utils.TimeUtils</code> 
 * until separation of inventory report generation from ae core inventory processor.
 */
public abstract class AeaaTimeUtils {

    public static long parseHumanFormatTime(String time) {
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

        long humanTime = AeaaTimeUtils.parseHumanFormatTime(input);
        if (humanTime != -1) {
            return System.currentTimeMillis() - humanTime;
        }

        return AeaaTimeUtils.timestampFromNormalizedDate(AeaaTimeUtils.normalizeDate(input));
    }

    public static long timestampFromNormalizedDate(String date) {
        if (!date.contains("-")) {
            return 0;
        }
        if (date.contains("T")) {
            date = date.substring(0, date.indexOf("T"));
        }
        if (date.contains(" ")) {
            date = date.substring(0, date.indexOf(" "));
        }
        final String[] dateParts = date.split("-");
        return new GregorianCalendar(Integer.parseInt(dateParts[0]), Integer.parseInt(dateParts[1]) - 1, Integer.parseInt(dateParts[2])).getTimeInMillis();
    }

    private final static List<SimpleDateFormat> DATE_FORMATS = Arrays.asList(
            new SimpleDateFormat("yyyy-MM-dd HH:mm:ss"),
            new SimpleDateFormat("dd MMMMM yyyy", Locale.GERMAN), // cert-fr
            new SimpleDateFormat("dd MMMMM yyyy", Locale.FRANCE),
            new SimpleDateFormat("dd MMMMM yyyy", Locale.ENGLISH),
            new SimpleDateFormat("dd MMMMM yyyy"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ"),
            new SimpleDateFormat("EEE MMM dd HH:mm:ss zzz yyyy"), // java Date#toString() format

            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss"),
            new SimpleDateFormat("yyyy-MM-dd'T'HH:mm"),
            new SimpleDateFormat("yyyy-MM-dd HH:mm"),

            new SimpleDateFormat("dd-MM-yyyy'T'HH:mm:ss"),
            new SimpleDateFormat("dd-MM-yyyy'T'HH:mm"),
            new SimpleDateFormat("dd-MM-yyyy HH:mm:ss"),
            new SimpleDateFormat("dd-MM-yyyy HH:mm"),

            new SimpleDateFormat("MM-dd-yyyy'T'HH:mm:ss"),
            new SimpleDateFormat("MM-dd-yyyy'T'HH:mm"),
            new SimpleDateFormat("MM-dd-yyyy HH:mm:ss"),
            new SimpleDateFormat("MM-dd-yyyy HH:mm"),

            new SimpleDateFormat("MM/dd/yyyy'T'HH:mm:ss"),
            new SimpleDateFormat("MM/dd/yyyy'T'HH:mm"),
            new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"),
            new SimpleDateFormat("MM/dd/yyyy HH:mm"),

            new SimpleDateFormat("dd/MM/yyyy'T'HH:mm:ss"),
            new SimpleDateFormat("dd/MM/yyyy'T'HH:mm"),
            new SimpleDateFormat("dd/MM/yyyy HH:mm:ss"),
            new SimpleDateFormat("dd/MM/yyyy HH:mm"),

            new SimpleDateFormat("yyyy/MM/dd'T'HH:mm:ss"),
            new SimpleDateFormat("yyyy/MM/dd'T'HH:mm"),
            new SimpleDateFormat("yyyy/MM/dd HH:mm:ss"),
            new SimpleDateFormat("yyyy/MM/dd HH:mm"),

            new SimpleDateFormat("yyyy-MM-dd"),
            new SimpleDateFormat("yyyy-MM"),

            new SimpleDateFormat("dd-MM-yyyy"),
            new SimpleDateFormat("MM-dd-yyyy"),

            new SimpleDateFormat("MM/dd/yyyy"),
            new SimpleDateFormat("dd/MM/yyyy"),
            new SimpleDateFormat("yyyy/MM/dd"),

            nonLenientSimpleDateFormat("yyyy.MM.dd"),
            nonLenientSimpleDateFormat("dd.MM.yyyy"),
            nonLenientSimpleDateFormat("MM.dd.yyyy"),

            new SimpleDateFormat("dd.MM.yyyy'T'HH:mm:ss"),
            new SimpleDateFormat("dd.MM.yyyy'T'HH:mm"),
            new SimpleDateFormat("dd.MM.yyyy HH:mm:ss"),
            new SimpleDateFormat("dd.MM.yyyy HH:mm"),

            new SimpleDateFormat("yyyy")
    );

    private static SimpleDateFormat nonLenientSimpleDateFormat(String pattern) {
        SimpleDateFormat sdf = new SimpleDateFormat(pattern);
        sdf.setLenient(false);
        return sdf;
    }

    public static String normalizeDate(String string) {
        if (string == null) return "n.a.";

        Date parsedDate = tryParse(string);

        if (parsedDate != null) {
            return formatNormalizedDate(parsedDate);
        } else if (string.contains("T")) {
            return string.substring(0, string.indexOf("T"));
        }

        return string;
    }

    public static String formatNormalizedDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
                .format(date)
                .replace(" 00:00:00", "")
                .replaceAll(":00$", "");
    }

    public static String formatNormalizedDateOnlyDate(Date date) {
        return new SimpleDateFormat("yyyy-MM-dd")
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

    public static Date tryParse(String dateString) {
        if (dateString == null) return null;

        if (dateString.matches("\\d+")) {
            return new Date(Long.parseLong(dateString));
        }

        synchronized (DATE_FORMATS) {
            for (SimpleDateFormat formatter : DATE_FORMATS) {
                try {
                    return formatter.parse(dateString);
                } catch (ParseException ignored) {
                }
            }
        }

        return null;
    }

    public static Date tryParse(Object date) {
        if (date instanceof Date) {
            return (Date) date;
        } else if (date instanceof Number) {
            return new Date(((Number) date).longValue());
        } else if (date instanceof String) {
            return tryParse((String) date);
        } else {
            return null;
        }
    }

    public static long utcNow() {
        return Instant.now().toEpochMilli();
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
}
