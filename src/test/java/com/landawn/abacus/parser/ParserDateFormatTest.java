package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;
import static org.junit.jupiter.api.Assertions.*;

public class ParserDateFormatTest extends TestBase {
    private final JsonParser parser = ParserFactory.createJsonParser();
    private final ObjectMapper strictJson = new ObjectMapper();
    private static final String DATE_PATTERN = "yyyy-MM-dd'\"\\\n\u96ea'HH:mm:ss.SSS";

    @Test
    public void legacyDateLiteralsRoundTripAtMillisecondBoundaries() throws Exception {
        SimpleDateFormat format = new SimpleDateFormat(DATE_PATTERN);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        for (long millis : new long[] { -1, 0, 1, 1700000000123L }) {
            Legacy bean = new Legacy();
            bean.date = new Date(millis);
            String json = parser.serialize(bean);
            assertEquals(format.format(bean.date), strictJson.readTree(json).get("date").textValue());
            assertEquals(bean.date, parser.deserialize(json, Legacy.class).date);
            assertEquals(bean.date, parser.deserialize(new StringReader(json), Legacy.class).date);
            StringWriter output = new StringWriter();
            parser.serialize(bean, output);
            assertEquals(json, output.toString());
        }
    }

    @Test
    public void modernDateAndTimeLiteralsRoundTrip() throws Exception {
        Modern bean = new Modern();
        bean.date = LocalDate.of(2000, 2, 29);
        bean.time = LocalTime.of(23, 59, 59);
        bean.dateTime = LocalDateTime.of(bean.date, bean.time);
        String json = parser.serialize(bean);
        assertEquals("2000\"02\\29", strictJson.readTree(json).get("date").textValue());
        Modern result = parser.deserialize(json, Modern.class);
        assertEquals(bean.date, result.date);
        assertEquals(bean.time, result.time);
        assertEquals(bean.dateTime, result.dateTime);
    }

    @Test
    public void calendarAndSqlTypesUseEscapedText() throws Exception {
        LegacyTypes bean = new LegacyTypes();
        bean.calendar = java.util.Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        bean.calendar.setTimeInMillis(0);
        bean.timestamp = new java.sql.Timestamp(0);
        bean.date = new java.sql.Date(0);
        bean.time = new java.sql.Time(0);
        String json = parser.serialize(bean);
        strictJson.readTree(json);
        LegacyTypes result = parser.deserialize(json, LegacyTypes.class);
        assertEquals(0, result.calendar.getTimeInMillis());
        assertEquals(bean.timestamp, result.timestamp);
        assertEquals(bean.date, result.date);
        assertEquals(bean.time, result.time);
    }

    @Test
    public void formattedStringPreservesEmptyNullUnicodeAndLongText() throws Exception {
        Text bean = new Text();
        for (String value : new String[] { "", " ", "\"\\\n\r\t\b\f\u0001\u96ea\ud83d\ude00", "x".repeat(9000) + "\"\\" }) {
            bean.value = value;
            String json = parser.serialize(bean);
            assertEquals(value, strictJson.readTree(json).get("value").textValue());
            assertEquals(value, parser.deserialize(json, Text.class).value);
        }
        bean.value = null;
        assertNull(parser.deserialize(parser.serialize(bean), Text.class).value);
        assertNull(parser.deserialize(parser.serialize(new Legacy()), Legacy.class).date);
    }

    @Test
    public void epochFormatPreservesQuotationAndLongBoundaries() throws Exception {
        Epoch bean = new Epoch();
        for (long millis : new long[] { Long.MIN_VALUE, -1, 0, 1, Long.MAX_VALUE }) {
            bean.date = new Date(millis);
            String json = parser.serialize(bean);
            assertEquals(Long.toString(millis), strictJson.readTree(json).get("date").textValue());
            assertEquals(bean.date, parser.deserialize(json, Epoch.class).date);
            String raw = parser.serialize(bean, JsonSerConfig.create().setStringQuotation((char) 0));
            assertEquals(millis, strictJson.readTree(raw).get("date").longValue());
        }
    }

    @Test
    public void quotationModesAndXmlKeepTheirConventions() {
        Text bean = new Text();
        bean.value = "a'b\\c\"\u96ea";
        String single = parser.serialize(bean, JsonSerConfig.create().setStringQuotation('\''));
        assertEquals(bean.value, parser.deserialize(single, Text.class).value);
        String raw = parser.serialize(bean, JsonSerConfig.create().setStringQuotation((char) 0));
        assertTrue(raw.contains(bean.value));
        XmlParser xml = ParserFactory.createXmlParser();
        assertEquals(bean.value, xml.deserialize(xml.serialize(bean), Text.class).value);
    }

    public static class Legacy {
        @JsonXmlField(dateFormat = DATE_PATTERN, timeZone = "UTC")
        public Date date;
    }

    public static class Modern {
        @JsonXmlField(dateFormat = "uuuu'\"'MM'\\'dd")
        public LocalDate date;
        @JsonXmlField(dateFormat = "HH'\"'mm'\\'ss")
        public LocalTime time;
        @JsonXmlField(dateFormat = "uuuu-MM-dd'\"\\'HH:mm:ss")
        public LocalDateTime dateTime;
    }

    public static class LegacyTypes {
        @JsonXmlField(dateFormat = DATE_PATTERN, timeZone = "UTC")
        public java.util.Calendar calendar;
        @JsonXmlField(dateFormat = DATE_PATTERN, timeZone = "UTC")
        public java.sql.Timestamp timestamp;
        @JsonXmlField(dateFormat = "yyyy'\"'MM'\\'dd", timeZone = "UTC")
        public java.sql.Date date;
        @JsonXmlField(dateFormat = "HH'\"'mm'\\'ss", timeZone = "UTC")
        public java.sql.Time time;
    }

    public static class Text {
        @JsonXmlField(dateFormat = "yyyy-MM-dd")
        public String value;
    }

    public static class Epoch {
        @JsonXmlField(dateFormat = "long", timeZone = "UTC")
        public Date date;
    }

    public static class NullMarkers {
        @JsonXmlField(dateFormat = "yyyy-MM-dd HH:mm", timeZone = "UTC")
        public Date legacy;
        @JsonXmlField(dateFormat = "yyyy-MM-dd HH:mm", timeZone = "UTC")
        public LocalDateTime dateTime;
        @JsonXmlField(dateFormat = "yyyy-MM-dd", timeZone = "UTC")
        public LocalDate date;
        @JsonXmlField(dateFormat = "HH:mm", timeZone = "UTC")
        public LocalTime time;
        @JsonXmlField(dateFormat = "yyyy-MM-dd HH:mm VV", timeZone = "UTC")
        public java.time.ZonedDateTime zoned;
        @JsonXmlField(dateFormat = "long", timeZone = "UTC")
        public LocalDateTime epoch;
        @JsonXmlField(dateFormat = "yyyy-MM-dd HH:mm", timeZone = "UTC")
        public org.joda.time.DateTime joda;
        @JsonXmlField(dateFormat = "yyyy-MM-dd HH:mm", timeZone = "UTC")
        public org.joda.time.MutableDateTime mutableJoda;
    }

    @Test
    public void reviewFixes20260906_formattedJavaTimeAndJodaReadersAcceptTheNullDateMarkers() {
        ParserUtil.BeanInfo beanInfo = ParserUtil.getBeanInfo(NullMarkers.class);

        for (String prop : NULL_MARKER_PROPS) {
            ParserUtil.PropInfo propInfo = beanInfo.getPropInfo(prop);

            // The same set the legacy java.util.Date reader and the no-format java.time types accept.
            for (String marker : new String[] { null, "", "null", "NULL", "Null" }) {
                assertNull(propInfo.readPropValue(marker), prop + " <- [" + marker + "]");
                assertEquals(beanInfo.getPropInfo("legacy").readPropValue(marker), propInfo.readPropValue(marker), prop + " <- [" + marker + "]");
            }

            // Blank and near-miss text stay errors in every reader (parity with the legacy readers).
            for (String garbage : new String[] { " ", "nul", "  null " }) {
                assertThrows(RuntimeException.class, () -> propInfo.readPropValue(garbage), prop + " <- [" + garbage + "]");
            }
        }
    }

    @Test
    public void reviewFixes20260906_emptyAndNullTextDeserializeToNullDateFields() {
        String json = "{\"legacy\": \"\", \"dateTime\": \"\", \"date\": \"null\", \"time\": \"NULL\", \"zoned\": \"\", \"epoch\": \"\", \"joda\": \"null\", \"mutableJoda\": \"\"}";
        NullMarkers fromJson = parser.deserialize(json, NullMarkers.class);
        assertNull(fromJson.legacy);
        assertNull(fromJson.dateTime);
        assertNull(fromJson.date);
        assertNull(fromJson.time);
        assertNull(fromJson.zoned);
        // "long" + "" used to fabricate epoch 0 (Numbers.toLong("") == 0) instead of null.
        assertNull(fromJson.epoch);
        assertNull(fromJson.joda);
        assertNull(fromJson.mutableJoda);

        NullMarkers template = realValues();
        for (XmlParser xmlParser : java.util.List.of(ParserFactory.createXmlParser(), ParserFactory.createAbacusXmlParser())) {
            String xml = xmlParser.serialize(template).replace("2024-01-02 03:04 UTC", "null").replace("2024-01-02 03:04", "null").replace("2024-01-02", "null")
                    .replace("03:04", "NULL").replace(">1704164640000<", ">null<");
            assertTrue(xml.contains("<dateTime>null</dateTime>"), xml);
            NullMarkers fromXml = xmlParser.deserialize(xml, NullMarkers.class);
            assertNull(fromXml.dateTime, xml);
            assertNull(fromXml.date, xml);
            assertNull(fromXml.time, xml);
            assertNull(fromXml.zoned, xml);
            assertNull(fromXml.epoch, xml);
            assertNull(fromXml.joda, xml);
            assertNull(fromXml.mutableJoda, xml);
        }
    }

    @Test
    public void reviewFixes20260906_realFormattedValuesStillParseAfterTheNullMarkerGuard() {
        NullMarkers bean = realValues();
        String json = parser.serialize(bean);
        NullMarkers result = parser.deserialize(json, NullMarkers.class);
        assertEquals(bean.legacy, result.legacy, json);
        assertEquals(bean.dateTime, result.dateTime, json);
        assertEquals(bean.date, result.date, json);
        assertEquals(bean.time, result.time, json);
        assertEquals(bean.zoned, result.zoned, json);
        assertEquals(bean.epoch, result.epoch, json);
        assertEquals(bean.joda, result.joda, json);
        assertEquals(bean.mutableJoda, result.mutableJoda, json);
    }

    private static final String[] NULL_MARKER_PROPS = { "legacy", "dateTime", "date", "time", "zoned", "epoch", "joda", "mutableJoda" };

    private static NullMarkers realValues() {
        NullMarkers bean = new NullMarkers();
        bean.legacy = new Date(1704164640000L); // 2024-01-02T03:04:00Z
        bean.dateTime = LocalDateTime.of(2024, 1, 2, 3, 4);
        bean.date = LocalDate.of(2024, 1, 2);
        bean.time = LocalTime.of(3, 4);
        bean.zoned = java.time.ZonedDateTime.of(2024, 1, 2, 3, 4, 0, 0, java.time.ZoneId.of("UTC"));
        bean.epoch = LocalDateTime.of(2024, 1, 2, 3, 4);
        bean.joda = new org.joda.time.DateTime(2024, 1, 2, 3, 4, org.joda.time.DateTimeZone.UTC);
        bean.mutableJoda = new org.joda.time.MutableDateTime(2024, 1, 2, 3, 4, 0, 0, org.joda.time.DateTimeZone.UTC);
        return bean;
    }

    public static class EpochLongs {
        @JsonXmlField(dateFormat = "long")
        private Long boxed;
        @JsonXmlField(dateFormat = "long")
        private long primitive;

        public Long getBoxed() {
            return boxed;
        }

        public void setBoxed(Long boxed) {
            this.boxed = boxed;
        }

        public long getPrimitive() {
            return primitive;
        }

        public void setPrimitive(long primitive) {
            this.primitive = primitive;
        }
    }

    // G08-15: ten of the twelve dateFormat readers treat "" and the literal "null" as absent; the long/Long
    // entries kept a null-only check, so "" fabricated the 1970 epoch (Numbers.toLong("") is 0) and the
    // literal "null" threw NumberFormatException where every sibling reader answered null.
    @Test
    public void fixG08_F15_longEpochReadersAcceptTheSameNullMarkersAsTheirSiblings() {
        for (String text : new String[] { "\"\"", "\"null\"", "\"NULL\"", "null" }) {
            String json = "{\"boxed\":" + text + ",\"primitive\":" + text + "}";
            EpochLongs bean = parser.deserialize(json, EpochLongs.class);
            assertNull(bean.getBoxed(), json);
            assertEquals(0L, bean.getPrimitive(), json);
            EpochLongs fromReader = parser.deserialize(new StringReader(json), EpochLongs.class);
            assertNull(fromReader.getBoxed(), json);
            assertEquals(0L, fromReader.getPrimitive(), json);
        }

        EpochLongs real = parser.deserialize("{\"boxed\":123,\"primitive\":456}", EpochLongs.class);
        assertEquals(Long.valueOf(123L), real.getBoxed());
        assertEquals(456L, real.getPrimitive());

        // malformed text is still rejected rather than being swallowed as a null marker
        assertThrows(NumberFormatException.class, () -> parser.deserialize("{\"boxed\":\"abc\"}", EpochLongs.class));
    }

}
