/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.apache.commons.lang3.time.FastDateFormat;
import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.type.Type;

public class DateTimeTest extends AbstractTest {

    //    @Test
    //    public void test_currentTimeMillisRolled() {
    //        N.println(DateUtil.currentTimeMillisRolled(100, TimeUnit.SECONDS));
    //        N.println(DateUtil.currentTimeMillisRolled(-100, TimeUnit.SECONDS));
    //        N.println(DateUtil.currentTimeMillisRolled(100, TimeUnit.SECONDS) - DateUtil.currentTimeMillisRolled(-100, TimeUnit.SECONDS));
    //
    //        assertTrue(DateUtil.currentTimeMillisRolled(100, TimeUnit.SECONDS)
    //                - DateUtil.currentTimeMillisRolled(-100, TimeUnit.SECONDS) <= TimeUnit.SECONDS.toMillis(100) * 2);
    //
    //    }

    @Test
    public void test_perf() {
        final String pattern = "yyyy-MM-dd HH:mm:ss";
        final Date date = new Date();
        final FastDateFormat fdf = FastDateFormat.getInstance(pattern);
        N.println(fdf.format(date));
        N.println(Dates.format(date, pattern));
        Profiler.run(1, 100, 1, "Commons Lang", () -> fdf.format(date)).printResult();
        Profiler.run(1, 100, 1, "abacus-common", () -> Dates.format(date, pattern)).printResult();
    }

    //    public void test_format_zonedDateTime() {
    //        String date = "2011-12-03T10:15:30+01:00";
    //        N.println(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(date));
    //        ZonedDateTime zonedDateTime = DateUtil.parseZonedDateTime(date);
    //        N.println(zonedDateTime);
    //
    //        N.println(DateUtil.format(zonedDateTime));
    //        N.println(DateUtil.format(zonedDateTime, "yyyy-MM/dd"));
    //    }

    @Test
    public void test_DateTime() {
        Type<DateTime> type = N.typeOf(DateTime.class);
        DateTime now = DateTime.now();
        String str = type.stringOf(now);
        N.println(str);

        str = type.stringOf(type.valueOf(str));
        N.println(str);

        assertEquals(now, type.valueOf(str));
    }

    @Test
    public void test_MutableDateTime() {
        Type<MutableDateTime> type = N.typeOf(MutableDateTime.class);
        MutableDateTime now = MutableDateTime.now();
        String str = type.stringOf(now);
        N.println(str);

        str = type.stringOf(type.valueOf(str));
        N.println(str);

        assertEquals(now, type.valueOf(str));
    }
}
