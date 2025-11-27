package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;

import org.apache.commons.lang3.time.FastDateFormat;
import org.joda.time.DateTime;
import org.joda.time.MutableDateTime;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.type.Type;

@Tag("old-test")
public class DateTimeTest extends AbstractTest {

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

    @Test
    public void test_DateTime() {
        Type<DateTime> type = CommonUtil.typeOf(DateTime.class);
        DateTime now = DateTime.now();
        String str = type.stringOf(now);
        N.println(str);

        str = type.stringOf(type.valueOf(str));
        N.println(str);

        assertEquals(now, type.valueOf(str));
    }

    @Test
    public void test_MutableDateTime() {
        Type<MutableDateTime> type = CommonUtil.typeOf(MutableDateTime.class);
        MutableDateTime now = MutableDateTime.now();
        String str = type.stringOf(now);
        N.println(str);

        str = type.stringOf(type.valueOf(str));
        N.println(str);

        assertEquals(now, type.valueOf(str));
    }
}
