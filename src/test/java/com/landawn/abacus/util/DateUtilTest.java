package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.Dates.DTF;

public class DateUtilTest extends AbstractTest {

    @Test
    @Tag("slow-test")
    public void test_perf() {
        final Date date = new Date();

        N.println(Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT));
        N.println(Dates.format(date, Dates.ISO_8601_TIMESTAMP_FORMAT));

        final String dateTimeText = ISO8601Util.format(date);
        final String timestampText = ISO8601Util.format(date, true);
        N.println(dateTimeText);
        N.println(timestampText);

        assertEquals(Dates.parseJUDate(dateTimeText, Dates.ISO_8601_DATE_TIME_FORMAT), ISO8601Util.parse(dateTimeText));
        assertEquals(Dates.parseJUDate(timestampText, Dates.ISO_8601_TIMESTAMP_FORMAT), ISO8601Util.parse(timestampText));

        final int threadNumber = 6;
        final int loopNum = 10;
        Profiler.run(threadNumber, loopNum, 3, "DateUtil_format_DATE_TIME", () -> Dates.format(date, Dates.ISO_8601_DATE_TIME_FORMAT)).printResult();
        Profiler.run(threadNumber, loopNum, 3, "DateUtil_format_TIMESTAMP", () -> Dates.format(date, Dates.ISO_8601_TIMESTAMP_FORMAT)).printResult();

        Profiler.run(threadNumber, loopNum, 3, "ISO8601Util_format_DATE_TIME", () -> ISO8601Util.format(date)).printResult();
        Profiler.run(threadNumber, loopNum, 3, "ISO8601Util_format_TIMESTAMP", () -> ISO8601Util.format(date, true)).printResult();

        Profiler.run(threadNumber, loopNum, 3, "DateUtil_parse_DATE_TIME", () -> Dates.parseJUDate(dateTimeText, Dates.ISO_8601_DATE_TIME_FORMAT))
                .printResult();
        Profiler.run(threadNumber, loopNum, 3, "DateUtil_parse_TIMESTAMP", () -> Dates.parseJUDate(timestampText, Dates.ISO_8601_TIMESTAMP_FORMAT))
                .printResult();

        Profiler.run(threadNumber, loopNum, 3, "ISO8601Util_parse_DATE_TIME", () -> ISO8601Util.parse(dateTimeText)).printResult();
        Profiler.run(threadNumber, loopNum, 3, "ISO8601Util_parse_TIMESTAMP", () -> ISO8601Util.parse(timestampText)).printResult();
    }

    @Test
    public void test_format() {
        N.println(Dates.format(Dates.currentCalendar()));
        N.println(DateTimeFormatter.ISO_ZONED_DATE_TIME.toString());
        N.println(LocalDateTime.now());
        N.println(DTF.LOCAL_DATE.format(LocalDateTime.now()));
        N.println(DTF.LOCAL_TIME.format(LocalDateTime.now()));
        N.println(DTF.LOCAL_DATE_TIME.format(LocalDateTime.now()));
        N.println(DTF.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now()));

        N.println(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now()));
        N.println(DTF.ISO_ZONED_DATE_TIME.format(ZonedDateTime.now()));
        N.println(DateTimeFormatter.ISO_ZONED_DATE_TIME.format(ZonedDateTime.now()));
        N.println(DTF.ISO_8601_DATE_TIME.format(LocalDateTime.now()));
        N.println(DTF.ISO_8601_TIMESTAMP.format(LocalDateTime.now()));
        N.println(DTF.RFC_1123_DATE_TIME.format(ZonedDateTime.now()));
        N.println(Dates.format(Dates.currentDate(), Dates.RFC_1123_DATE_TIME_FORMAT));
        N.println(Dates.formatLocalDate());
        N.println(Dates.formatLocalDateTime());

        N.println(DTF.ISO_8601_TIMESTAMP.parseToLocalDate(DTF.ISO_8601_TIMESTAMP.format(OffsetDateTime.now())));
        N.println(DTF.ISO_8601_TIMESTAMP.parseToLocalTime(DTF.ISO_8601_TIMESTAMP.format(OffsetDateTime.now())));
        N.println(DTF.ISO_8601_TIMESTAMP.parseToLocalDateTime(DTF.ISO_8601_TIMESTAMP.format(OffsetDateTime.now())));
        N.println(DTF.ISO_8601_TIMESTAMP.parseToLocalDate(DTF.ISO_8601_TIMESTAMP.format(ZonedDateTime.now())));
        N.println(DTF.ISO_8601_TIMESTAMP.parseToLocalTime(DTF.ISO_8601_TIMESTAMP.format(ZonedDateTime.now())));
        N.println(DTF.ISO_8601_TIMESTAMP.parseToLocalDateTime(DTF.ISO_8601_TIMESTAMP.format(ZonedDateTime.now())));

        N.println(DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime(DTF.ISO_ZONED_DATE_TIME.format(ZonedDateTime.now())));
        N.println(DTF.ISO_OFFSET_DATE_TIME.parseToZonedDateTime(DTF.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now())));
        N.println(DTF.ISO_8601_TIMESTAMP.parseToZonedDateTime(DTF.ISO_8601_TIMESTAMP.format(OffsetDateTime.now())));
        N.println(DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime(DTF.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now())));
        N.println(DTF.ISO_ZONED_DATE_TIME.parseToOffsetDateTime(DTF.ISO_ZONED_DATE_TIME.format(ZonedDateTime.now())));
        N.println(DTF.ISO_8601_TIMESTAMP.parseToOffsetDateTime(DTF.ISO_8601_TIMESTAMP.format(ZonedDateTime.now())));
        N.println(DTF.ISO_ZONED_DATE_TIME.parseToInstant(DTF.ISO_ZONED_DATE_TIME.format(ZonedDateTime.now())));

        N.println(Dates.format(Dates.currentDate(), Dates.LOCAL_DATE_TIME_FORMAT));

        N.println(Strings.repeat("=", 80));

        String format = Dates.LOCAL_DATE_FORMAT;
        String str = Dates.format(Dates.currentJUDate(), format);
        N.println(format + " -> " + format.length() + ", " + str + " -> " + str.length());
        format = Dates.LOCAL_TIME_FORMAT;
        str = Dates.format(Dates.currentJUDate(), format);
        N.println(format + " -> " + format.length() + ", " + str + " -> " + str.length());
        format = Dates.LOCAL_DATE_TIME_FORMAT;
        str = Dates.format(Dates.currentJUDate(), format);
        N.println(format + " -> " + format.length() + ", " + str + " -> " + str.length());
        format = Dates.ISO_LOCAL_DATE_TIME_FORMAT;
        str = Dates.format(Dates.currentJUDate(), format);
        N.println(format + " -> " + format.length() + ", " + str + " -> " + str.length());
        format = Dates.ISO_OFFSET_DATE_TIME_FORMAT;
        str = Dates.format(Dates.currentJUDate(), format);
        N.println(format + " -> " + format.length() + ", " + str + " -> " + str.length());
        format = Dates.ISO_8601_DATE_TIME_FORMAT;
        str = Dates.format(Dates.currentJUDate(), format);
        N.println(format + " -> " + format.length() + ", " + str + " -> " + str.length());
        format = Dates.ISO_8601_TIMESTAMP_FORMAT;
        str = Dates.format(Dates.currentJUDate(), format);
        N.println(format + " -> " + format.length() + ", " + str + " -> " + str.length());
        format = Dates.RFC_1123_DATE_TIME_FORMAT;
        str = Dates.format(Dates.currentJUDate(), format);
        N.println(format + " -> " + format.length() + ", " + str + " -> " + str.length());

        //    Stream.of(DateTimeFormatter.class.getDeclaredFields())
        //            .filterE(it -> Modifier.isStatic(it.getModifiers()) && DateTimeFormatter.class.isAssignableFrom(it.get(null).getClass()))
        //            .mapE(it -> it.get(null))
        //            .map(Object::toString)
        //            .forEach(Fn.println());

        N.println(Dates.parseDate("2024-07-31T23:42:38-07:00"));
        N.println(Dates.parseTime("2024-07-31T23:42:38-07:00"));
        N.println(Dates.parseTimestamp("2024-07-31T23:42:38-07:00"));
        N.println(Dates.parseJUDate("2024-07-31T23:42:38-07:00"));
        N.println(DTF.ISO_OFFSET_DATE_TIME.parseToLocalDateTime("2024-07-31T23:42:38-07:00"));
        N.println(DTF.ISO_OFFSET_DATE_TIME.parseToOffsetDateTime("2024-07-31T23:42:38-07:00"));
        N.println(DTF.ISO_ZONED_DATE_TIME.parseToZonedDateTime("2024-07-31T23:42:38-07:00[America/Los_Angeles]"));
        N.println(ZonedDateTime.parse(ZonedDateTime.now().toString()));

        N.println(Dates.format(Dates.parseTimestamp("Wed, 01 Jul 2024 23:53:48 PDT"), Dates.RFC_1123_DATE_TIME_FORMAT));

    }

    @Test
    public void test_isLastDateOfMonth() {
        assertTrue(Dates.isLastDateOfMonth(Dates.parseDate("2023-02-28", "yyyy-MM-dd")));
        assertTrue(Dates.isLastDateOfMonth(Dates.parseDate("2023-03-31", "yyyy-MM-dd")));
        assertTrue(Dates.isLastDateOfMonth(Dates.parseDate("2023-06-30", "yyyy-MM-dd")));
        assertTrue(Dates.isLastDateOfYear(Dates.parseDate("2023-12-31", "yyyy-MM-dd")));

        assertFalse(Dates.isLastDateOfMonth(Dates.parseDate("2023-02-01", "yyyy-MM-dd")));
        assertFalse(Dates.isLastDateOfMonth(Dates.parseDate("2023-02-29", "yyyy-MM-dd")));
        assertFalse(Dates.isLastDateOfMonth(Dates.parseDate("2023-03-30", "yyyy-MM-dd")));
        assertFalse(Dates.isLastDateOfMonth(Dates.parseDate("2023-03-32", "yyyy-MM-dd")));
        assertFalse(Dates.isLastDateOfYear(Dates.parseDate("2023-03-32", "yyyy-MM-dd")));

        assertTrue(Dates.isSameYear(Dates.parseDate("2023-06-30", "yyyy-MM-dd"), Dates.parseDate("2023-01-30", "yyyy-MM-dd")));
        assertTrue(Dates.isSameMonth(Dates.parseDate("2023-06-30", "yyyy-MM-dd"), Dates.parseDate("2023-06-01", "yyyy-MM-dd")));

        assertFalse(Dates.isSameYear(Dates.parseDate("2023-06-30", "yyyy-MM-dd"), Dates.parseDate("2025-01-30", "yyyy-MM-dd")));
        assertFalse(Dates.isSameMonth(Dates.parseDate("2023-06-30", "yyyy-MM-dd"), Dates.parseDate("2023-05-01", "yyyy-MM-dd")));

        assertEquals(366, Dates.getLastDateOfYear(Dates.parseDate("2020-02-32", "yyyy-MM-dd")));
        assertEquals(365, Dates.getLastDateOfYear(Dates.parseDate("2023-03-32", "yyyy-MM-dd")));
    }

}
