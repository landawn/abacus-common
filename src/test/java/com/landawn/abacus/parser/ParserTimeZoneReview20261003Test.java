package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import java.util.Map;
import java.util.TimeZone;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlConfig;
import com.landawn.abacus.annotation.JsonXmlField;

@Tag("unit")
class ParserTimeZoneReview20261003Test extends TestBase {

    @Test
    void jacksonDefaultTimeZoneInheritsClassZoneWhenWriting() {
        final ClassZoneBean bean = new ClassZoneBean();
        bean.date = new Date(0);

        final JsonParser parser = ParserFactory.createJsonParser();
        assertEquals("1970-01-01 09:00", parser.deserialize(parser.serialize(bean), Map.class).get("date"));
    }

    @Test
    void jacksonDefaultTimeZoneInheritsClassZoneWhenReading() {
        final ClassZoneBean bean = ParserFactory.createJsonParser().deserialize("{\"date\":\"1970-01-01 09:00\"}", ClassZoneBean.class);

        assertEquals(new Date(0), bean.date);
    }

    @Test
    void jacksonDefaultWithoutClassZoneLeavesContextDefaultUnspecified() throws Exception {
        assertNull(ParserUtil.getTimeZone(UnconfiguredBean.class.getField("date"), null));
        assertEquals(TimeZone.getDefault(), ParserUtil.getBeanInfo(UnconfiguredBean.class).getPropInfo("date").timeZone);
    }

    @Test
    void explicitFieldZonesAndEmptyJacksonZoneKeepTheirPrecedence() throws Exception {
        final JsonXmlConfig config = OverrideBean.class.getAnnotation(JsonXmlConfig.class);

        assertEquals("GMT-04:00", ParserUtil.getTimeZone(OverrideBean.class.getField("jackson"), config));
        assertEquals("GMT+03:00", ParserUtil.getTimeZone(OverrideBean.class.getField("nativeAnnotation"), config));
        assertEquals("GMT+09:00", ParserUtil.getTimeZone(OverrideBean.class.getField("empty"), config));

        final OverrideBean bean = ParserFactory.createJsonParser()
                .deserialize("{\"jackson\":\"1969-12-31 20:00\",\"nativeAnnotation\":\"1970-01-01 03:00\",\"empty\":\"1970-01-01 09:00\"}", OverrideBean.class);
        assertEquals(new Date(0), bean.jackson);
        assertEquals(new Date(0), bean.nativeAnnotation);
        assertEquals(new Date(0), bean.empty);
    }

    @JsonXmlConfig(timeZone = "GMT+09:00")
    public static class ClassZoneBean {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
        public Date date;
    }

    public static class UnconfiguredBean {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
        public Date date;
    }

    @JsonXmlConfig(timeZone = "GMT+09:00")
    public static class OverrideBean {
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT-04:00")
        public Date jackson;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "GMT-04:00")
        @JsonXmlField(timeZone = "GMT+03:00")
        public Date nativeAnnotation;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm", timezone = "")
        public Date empty;
    }
}
