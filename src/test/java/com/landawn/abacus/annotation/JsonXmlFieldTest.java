package com.landawn.abacus.annotation;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField.Direction;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.ParserFactory;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.PropInfo;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.EnumType;
import com.landawn.abacus.util.Objectory;

public class JsonXmlFieldTest extends TestBase {
    public static class TestClass {
        @JsonXmlField
        private String field1;

        @JsonXmlField(name = "custom_name", aliases = { "alt1",
                "alt2" }, type = "String", enumerated = EnumType.ORDINAL, dateFormat = "yyyy-MM-dd", timeZone = "UTC", numberFormat = "#.##", ignore = true, isJsonRawValue = true, direction = Direction.SERIALIZE_ONLY)
        private String field2;
    }

    public static class DocumentedDateTimeExample {
        @JsonXmlField(dateFormat = "uuuu-MM-dd'T'HH:mm:ss.SSSXXX", timeZone = "UTC")
        private ZonedDateTime timestamp;

        public ZonedDateTime getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(final ZonedDateTime timestamp) {
            this.timestamp = timestamp;
        }
    }

    @Test
    public void testDefaultValues() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("field1");
        JsonXmlField annotation = field.getAnnotation(JsonXmlField.class);
        assertNotNull(annotation);
        assertEquals("", annotation.name());
        assertArrayEquals(new String[] {}, annotation.aliases());
        assertEquals("", annotation.type());
        assertEquals(EnumType.NAME, annotation.enumerated());
        assertEquals("", annotation.dateFormat());
        assertEquals("", annotation.timeZone());
        assertEquals("", annotation.numberFormat());
        assertFalse(annotation.ignore());
        assertFalse(annotation.isJsonRawValue());
        assertEquals(Direction.BOTH, annotation.direction());
    }

    @Test
    public void testCustomValues() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("field2");
        JsonXmlField annotation = field.getAnnotation(JsonXmlField.class);
        assertNotNull(annotation);
        assertEquals("custom_name", annotation.name());
        assertArrayEquals(new String[] { "alt1", "alt2" }, annotation.aliases());
        assertEquals("String", annotation.type());
        assertEquals(EnumType.ORDINAL, annotation.enumerated());
        assertEquals("yyyy-MM-dd", annotation.dateFormat());
        assertEquals("UTC", annotation.timeZone());
        assertEquals("#.##", annotation.numberFormat());
        assertTrue(annotation.ignore());
        assertTrue(annotation.isJsonRawValue());
        assertEquals(Direction.SERIALIZE_ONLY, annotation.direction());
    }

    @Test
    public void testFieldAnnotation() throws NoSuchFieldException {
        Field field = TestClass.class.getDeclaredField("field1");
        assertTrue(field.isAnnotationPresent(JsonXmlField.class));
    }

    @Test
    public void testRetentionPolicy() {
        Retention retention = JsonXmlField.class.getAnnotation(Retention.class);
        assertNotNull(retention);
        assertEquals(RetentionPolicy.RUNTIME, retention.value());
    }

    @Test
    public void testTargetElements() {
        Target target = JsonXmlField.class.getAnnotation(Target.class);
        assertNotNull(target);
        assertArrayEquals(new ElementType[] { ElementType.FIELD }, target.value());
    }

    @Test
    public void testIsAnnotation() {
        assertTrue(JsonXmlField.class.isAnnotation());
    }

    @Test
    public void testExposeEnum() {
        assertEquals(3, Direction.values().length);
        assertTrue(Arrays.asList(Direction.values()).contains(Direction.BOTH));
        assertTrue(Arrays.asList(Direction.values()).contains(Direction.SERIALIZE_ONLY));
        assertTrue(Arrays.asList(Direction.values()).contains(Direction.DESERIALIZE_ONLY));
    }

    @Test
    public void testDocumentedJavaTimeFormatExample() throws Exception {
        final PropInfo propInfo = ParserUtil.getBeanInfo(DocumentedDateTimeExample.class).getPropInfo("timestamp");
        final String value = "2023-12-25T10:30:45.123Z";
        final ZonedDateTime parsed = (ZonedDateTime) propInfo.readPropValue(value);

        assertEquals(Instant.parse(value), parsed.toInstant());

        final CharacterWriter writer = Objectory.createBufferedJsonWriter();

        try {
            propInfo.writePropValue(writer, parsed, JsonSerConfig.create());
            assertEquals('"' + value + '"', writer.toString());
        } finally {
            Objectory.recycle(writer);
        }
    }

    // a13 F-2: a field-level enumerated = NAME is indistinguishable from the default and loses to a class-level
    // ORDINAL; @Type(enumerated = NAME) is not consulted for JSON/XML either; the type = "...(NAME)" spelling wins.
    public enum Color {
        RED, GREEN, BLUE
    }

    @JsonXmlConfig(enumerated = EnumType.ORDINAL)
    public static class OrdinalClass {
        @JsonXmlField(enumerated = EnumType.NAME)
        private Color viaField = Color.BLUE;

        @Type(enumerated = EnumType.NAME)
        private Color viaType = Color.BLUE;

        @JsonXmlField(type = "com.landawn.abacus.annotation.JsonXmlFieldTest.Color(NAME)")
        private Color viaTypeString = Color.BLUE;

        private Color plain = Color.BLUE;

        public Color getViaField() {
            return viaField;
        }

        public void setViaField(final Color viaField) {
            this.viaField = viaField;
        }

        public Color getViaType() {
            return viaType;
        }

        public void setViaType(final Color viaType) {
            this.viaType = viaType;
        }

        public Color getViaTypeString() {
            return viaTypeString;
        }

        public void setViaTypeString(final Color viaTypeString) {
            this.viaTypeString = viaTypeString;
        }

        public Color getPlain() {
            return plain;
        }

        public void setPlain(final Color plain) {
            this.plain = plain;
        }
    }

    @Test
    public void testFieldLevelNameDoesNotOverrideClassLevelOrdinal_json() {
        final var parser = ParserFactory.createJsonParser();
        final String out = parser.serialize(new OrdinalClass());
        assertTrue(out.contains("\"viaField\": 2"), out);
        assertTrue(out.contains("\"viaType\": 2"), out);
        assertTrue(out.contains("\"plain\": 2"), out);
        assertTrue(out.contains("\"viaTypeString\": \"BLUE\""), out);

        final OrdinalClass back = parser.deserialize(out, OrdinalClass.class);
        assertEquals(Color.BLUE, back.getViaField());
        assertEquals(Color.BLUE, back.getViaType());
        assertEquals(Color.BLUE, back.getViaTypeString());
        assertEquals(Color.BLUE, back.getPlain());

        // Both spellings are accepted on input regardless of the output form.
        final OrdinalClass mixed = parser.deserialize("{\"viaField\":\"RED\",\"viaType\":0,\"viaTypeString\":1,\"plain\":\"GREEN\"}", OrdinalClass.class);
        assertEquals(Color.RED, mixed.getViaField());
        assertEquals(Color.RED, mixed.getViaType());
        assertEquals(Color.GREEN, mixed.getViaTypeString());
        assertEquals(Color.GREEN, mixed.getPlain());
    }

    @Test
    public void testFieldLevelNameDoesNotOverrideClassLevelOrdinal_xml() {
        final var parser = ParserFactory.createXmlParser();
        final String out = parser.serialize(new OrdinalClass());
        assertTrue(out.contains("<viaField>2</viaField>"), out);
        assertTrue(out.contains("<viaType>2</viaType>"), out);
        assertTrue(out.contains("<plain>2</plain>"), out);
        assertTrue(out.contains("<viaTypeString>BLUE</viaTypeString>"), out);

        final OrdinalClass back = parser.deserialize(out, OrdinalClass.class);
        assertEquals(Color.BLUE, back.getViaField());
        assertEquals(Color.BLUE, back.getViaTypeString());
    }

    // a13 F-3: an unrecognised time zone ID silently resolves to GMT.
    public static class ZoneExample {
        @JsonXmlField(dateFormat = "yyyy-MM-dd HH:mm:ss", timeZone = "Not/AZone")
        private Date unknownZone;

        @JsonXmlField(dateFormat = "yyyy-MM-dd HH:mm:ss", timeZone = "America/New York")
        private Date typo;

        @JsonXmlField(dateFormat = "yyyy-MM-dd HH:mm:ss", timeZone = "GMT")
        private Date gmt;

        @JsonXmlField(dateFormat = "yyyy-MM-dd HH:mm:ss", timeZone = "UTC")
        private Date utc;

        @JsonXmlField(dateFormat = "yyyy-MM-dd HH:mm:ss", timeZone = "GMT+02:00")
        private Date offset;

        @JsonXmlField(dateFormat = "yyyy-MM-dd HH:mm:ss", timeZone = " Europe/Paris ")
        private Date padded;

        @JsonXmlField(dateFormat = "yyyy-MM-dd HH:mm:ss", timeZone = "")
        private Date empty;

        public Date getUnknownZone() {
            return unknownZone;
        }

        public void setUnknownZone(final Date unknownZone) {
            this.unknownZone = unknownZone;
        }

        public Date getTypo() {
            return typo;
        }

        public void setTypo(final Date typo) {
            this.typo = typo;
        }

        public Date getGmt() {
            return gmt;
        }

        public void setGmt(final Date gmt) {
            this.gmt = gmt;
        }

        public Date getUtc() {
            return utc;
        }

        public void setUtc(final Date utc) {
            this.utc = utc;
        }

        public Date getOffset() {
            return offset;
        }

        public void setOffset(final Date offset) {
            this.offset = offset;
        }

        public Date getPadded() {
            return padded;
        }

        public void setPadded(final Date padded) {
            this.padded = padded;
        }

        public Date getEmpty() {
            return empty;
        }

        public void setEmpty(final Date empty) {
            this.empty = empty;
        }
    }

    private static String writeEpoch(final String propName) throws Exception {
        final PropInfo propInfo = ParserUtil.getBeanInfo(ZoneExample.class).getPropInfo(propName);
        final CharacterWriter writer = Objectory.createBufferedJsonWriter();

        try {
            propInfo.writePropValue(writer, new Date(0), JsonSerConfig.create());
            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Test
    public void testUnknownTimeZoneIdResolvesToGmt() throws Exception {
        assertEquals("GMT", TimeZone.getTimeZone("Not/AZone").getID());
        assertEquals("\"1970-01-01 00:00:00\"", writeEpoch("unknownZone"));
        assertEquals("\"1970-01-01 00:00:00\"", writeEpoch("typo"));
        assertEquals("\"1970-01-01 00:00:00\"", writeEpoch("gmt"));
        assertEquals("\"1970-01-01 00:00:00\"", writeEpoch("utc"));
        assertEquals("\"1970-01-01 02:00:00\"", writeEpoch("offset"));
        // Surrounding whitespace is trimmed before resolution: Paris was UTC+1 on 1970-01-01.
        assertEquals("\"1970-01-01 01:00:00\"", writeEpoch("padded"));
        // Empty selects the JVM default zone.
        final java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        sdf.setTimeZone(TimeZone.getDefault());
        assertEquals('"' + sdf.format(new Date(0)) + '"', writeEpoch("empty"));
    }
}
