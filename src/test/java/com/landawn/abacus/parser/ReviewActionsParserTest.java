package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.InputSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.http.HARUtil;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.XmlUtil;
import com.landawn.abacus.util.u;

/** Regression coverage for the requested HAR, XML and Avro review actions. */
@org.junit.jupiter.api.Tag("unit")
public class ReviewActionsParserTest extends TestBase {
    @Test
    public void malformedHarFailsBeforeAnyEntryIsSelectedOrReplayed() {
        final Map<String, Object> valid = Map.of("request", Map.of("url", "http://example.invalid/first", "method", "GET"));
        final Object[][] malformed = { { null, "log.entries[1]" }, { Map.of(), "log.entries[1].request" },
                { Map.of("request", "bad"), "log.entries[1].request" }, { Map.of("request", Map.of("url", 4)), "log.entries[1].request.url" },
                { Map.of("request", Map.of("url", "x", "headers", Map.of())), "log.entries[1].request.headers" },
                { Map.of("request", Map.of("url", "x", "headers", List.of("bad"))), "log.entries[1].request.headers[0]" },
                { Map.of("request", Map.of("url", "x", "postData", Map.of("params", "bad"))), "log.entries[1].request.postData.params" },
                { Map.of("request", Map.of("url", "x", "postData", Map.of("params", List.of(Map.of("value", "x"))))),
                        "log.entries[1].request.postData.params[0].name" } };
        for (Object[] bad : malformed) {
            List<Object> entries = new ArrayList<>();
            entries.add(valid);
            entries.add(bad[0]);
            String har = N.toJson(Map.of("log", Map.of("entries", entries)));
            AtomicInteger selections = new AtomicInteger();
            java.util.function.Predicate<String> filter = url -> {
                selections.incrementAndGet();
                return true;
            };
            List<Runnable> calls = List.of(() -> HARUtil.findRequestEntry(har, filter), () -> HARUtil.sendRequest(har, filter),
                    () -> HARUtil.sendRequests(har, filter), () -> HARUtil.streamRequests(har, filter).toList());
            for (Runnable call : calls) {
                assertTrue(assertThrows(IllegalArgumentException.class, call::run).getMessage().contains((String) bad[1]));
            }
            assertEquals(0, selections.get());
        }
        for (String har : List.of("[]", "{\"log\":4}", "{\"log\":{\"entries\":{}}}")) {
            assertTrue(assertThrows(IllegalArgumentException.class, () -> HARUtil.findRequestEntry(har, url -> false)).getMessage().contains("HAR at"));
        }
    }

    @Test
    public void harAbsentDataAndValidDuplicateFormFieldsRemainSupported() {
        assertTrue(HARUtil.sendRequests("{\"log\":{}}", url -> true).isEmpty());
        assertNull(HARUtil.getBodyAndMimeTypeByRequestEntry(Map.of())._1);
        assertTrue(HARUtil.getHeadersByRequestEntry(Map.of()).isEmpty());
        Map<String, Object> request = Map.of("postData",
                Map.of("params", List.of(Map.of("name", "a", "value", "two words"), Map.of("name", "a", "value", "&"))));
        assertEquals("a=two+words&a=%26", HARUtil.getBodyAndMimeTypeByRequestEntry(request)._1);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> HARUtil.getBodyAndMimeTypeByRequestEntry(Map.of("postData", List.of()))).getMessage()
                .contains("request.postData"));
        assertTrue(assertThrows(IllegalArgumentException.class,
                () -> HARUtil.getBodyAndMimeTypeByRequestEntry(Map.of("postData", Map.of("params", List.of("bad"))))).getMessage().contains("params[0]"));
    }

    private static List<AbstractXmlParser> parsers(Set<Class<?>> approved) {
        List<AbstractXmlParser> parsers = new ArrayList<>();
        for (XmlParserType backend : XmlParserType.values()) {
            parsers.add(new AbacusXmlParserImpl(backend, null, null, approved));
            if (backend != XmlParserType.SAX)
                parsers.add(new XmlParserImpl(backend, null, null, approved));
        }
        return parsers;
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    public void directHarHeaderExtractionValidatesBeforeCallingItsFilter(final boolean acceptHeaders) {
        final AtomicInteger filterCalls = new AtomicInteger();
        HARUtil.setThreadLocalHeaderFilter((name, value) -> {
            filterCalls.incrementAndGet();
            return acceptHeaders;
        });
        try {
            final Map<String, Object> valid = Map.of("name", "X-Valid", "value", "ok");
            final Object[][] malformed = { { Map.of("value", "missing name"), "name" }, { Map.of("name", List.of("X-Bad"), "value", "x"), "name" },
                    { Map.of("name", "X-Bad", "value", Map.of()), "value" } };
            for (final Object[] bad : malformed) {
                // Even a filter that rejects everything must not mask malformed headers or see a partial list.
                final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class,
                        () -> HARUtil.getHeadersByRequestEntry(Map.of("headers", List.of(valid, bad[0]))));
                assertTrue(failure.getMessage().contains("request.headers[1]." + bad[1]), failure.getMessage());
                assertEquals(0, filterCalls.get());
            }
            assertEquals(!acceptHeaders, HARUtil.getHeadersByRequestEntry(Map.of("headers", List.of(valid))).isEmpty());
            assertEquals(1, filterCalls.get());
        } finally {
            HARUtil.resetThreadLocalHeaderFilter();
        }
    }

    @Test
    public void xmlTypeApprovalIsLocalImmutableAndIndependentOfGlobalState() throws Exception {
        Set<Class<?>> classes = new HashSet<>(Set.of(PolicyBean.class));
        List<AbstractXmlParser> approved = parsers(classes);
        classes.clear();
        Type.of(PolicyBean.class);
        String name = PolicyBean.class.getCanonicalName();
        String xml = "<policyBean type=\"" + name + "\"><value>7</value></policyBean>";
        String property = AbstractXmlParser.XML_TYPE_CLASS_FOR_NAME_PROPERTY;
        String before = System.getProperty(property);
        try {
            for (String setting : List.of("false", "true")) {
                System.setProperty(property, setting);
                for (AbstractXmlParser parser : approved) {
                    assertEquals(7, parser.deserialize(xml, PolicyBean.class).getValue());
                    assertEquals(PolicyBean.class, parser.resolveTypeAttribute(name).javaType());
                    assertEquals(List.class, parser.resolveTypeAttribute("List<" + name + ">").javaType());
                    assertEquals(PolicyBean[].class, parser.resolveTypeAttribute(name + "[]").javaType());
                    final var node = XmlUtil.createDOMParser(false, false).parse(new InputSource(new StringReader(xml))).getDocumentElement();
                    assertSame(String.class, parser.getConcreteClass(node, String.class));
                }
                // Also exercises recycling SAX handlers between parsers with opposite policies.
                for (AbstractXmlParser parser : parsers(Set.of())) {
                    assertNull(parser.resolveTypeAttribute(name));
                    assertThrows(ParsingException.class, () -> parser.deserialize(xml, PolicyBean.class));
                }
            }
        } finally {
            if (before == null)
                System.clearProperty(property);
            else
                System.setProperty(property, before);
        }
        assertEquals(7, ParserFactory.createXmlParser(null, null, Set.of(PolicyBean.class)).deserialize(xml, PolicyBean.class).getValue());
        assertEquals(7, ParserFactory.createAbacusXmlParser(null, null, Set.of(PolicyBean.class)).deserialize(xml, PolicyBean.class).getValue());
    }

    @Test
    public void xmlRejectsUnrepresentableValuesAndStillWritesEmptyBeans() {
        for (AbstractXmlParser parser : parsers(Set.of(PolicyBean.class))) {
            for (Object value : List.of(Character.valueOf((char) 0), u.Nullable.of(null), Map.of("v", Character.valueOf((char) 0)),
                    Map.of("v", u.Nullable.of(null)), u.Optional.of(u.Nullable.of(null)))) {
                assertThrows(ParsingException.class, () -> parser.serialize(value), parser.getClass() + ": " + value);
            }
            assertEquals("<object></object>", parser.serialize(new Object(), XmlSerConfig.create().setFailOnEmptyBean(false)));
            String xml = parser.serialize(new PolicyBean(),
                    XmlSerConfig.create().setFailOnEmptyBean(false).setIgnoredPropNames(PolicyBean.class, Set.of("value")));
            assertFalse(xml.isEmpty());
            assertDoesNotThrow(() -> XmlUtil.createDOMParser(false, false).parse(new InputSource(new StringReader(xml))));
            assertDoesNotThrow(() -> parser.serialize(Map.of("v", u.Nullable.empty())));
            assertDoesNotThrow(() -> parser.serialize(Map.of("v", u.Nullable.of("a<&"))));
        }
    }

    @Test
    public void avroUnknownFieldsRequireExplicitProjectionIncludingNestedRecords() throws Exception {
        Schema schema = new Schema.Parser().parse(
                "{\"type\":\"record\",\"name\":\"Outer\",\"fields\":[{\"name\":\"child\",\"type\":{\"type\":\"record\",\"name\":\"Child\",\"fields\":[{\"name\":\"value\",\"type\":\"int\"}]}}]}");
        AvroParser parser = new AvroParser();
        AvroSerConfig strict = AvroSerConfig.create().setSchema(schema);
        Map<String, Object> source = Map.of("child", Map.of("value", 7, "typo", 8));
        IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parser.serialize(source, strict));
        assertTrue(error.getMessage().contains("typo") && error.getMessage().contains("Child"));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        AvroSerConfig projection = AvroSerConfig.create().setSchema(schema).setIgnoreUnknownFields(true);
        parser.serialize(source, projection, output);
        try (DataFileStream<GenericRecord> records = new DataFileStream<>(new ByteArrayInputStream(output.toByteArray()), new GenericDatumReader<>())) {
            assertEquals(7, ((GenericRecord) records.next().get("child")).get("value"));
        }
        assertNotEquals(strict, projection);
        assertEquals(projection, projection.copy());
        assertTrue(projection.toString().contains("ignoreUnknownFields=true"));
    }

    @Test
    public void avroUnknownNullBeanPropertiesRequireProjectionAtRootAndInNestedRecords() throws Exception {
        final Schema schema = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"OnlyValue\",\"fields\":[{\"name\":\"value\",\"type\":\"int\"}]}");
        final Schema outer = new Schema.Parser()
                .parse("{\"type\":\"record\",\"name\":\"OuterNullExtra\",\"fields\":[{\"name\":\"child\",\"type\":" + schema + "}]}");
        final AvroParser parser = new AvroParser();
        final Map<String, Object> map = new java.util.LinkedHashMap<>();
        map.put("value", 7);
        map.put("typo", null);
        for (final Object source : List.of(new NullExtraBean(), map)) {
            for (final boolean nested : new boolean[] { false, true }) {
                final Object value = nested ? Map.of("child", source) : source;
                final AvroSerConfig config = AvroSerConfig.create().setSchema(nested ? outer : schema);
                final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> parser.serialize(value, config));
                assertTrue(error.getMessage().contains("typo") && error.getMessage().contains("OnlyValue"));
                final ByteArrayOutputStream output = new ByteArrayOutputStream();
                parser.serialize(value, config.setIgnoreUnknownFields(true), output);
                try (DataFileStream<GenericRecord> records = new DataFileStream<>(new ByteArrayInputStream(output.toByteArray()), new GenericDatumReader<>())) {
                    final GenericRecord record = records.next();
                    assertEquals(7, (nested ? (GenericRecord) record.get("child") : record).get("value"));
                    assertFalse(records.hasNext());
                }
            }
        }
        // A declared nullable property is valid under strict mode and retains its null value.
        final Schema nullable = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"WithNullable\",\"fields\":[{\"name\":\"value\",\"type\":\"int\"},"
                + "{\"name\":\"typo\",\"type\":[\"null\",\"string\"]}]}");
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        parser.serialize(new NullExtraBean(), AvroSerConfig.create().setSchema(nullable), output);
        try (DataFileStream<GenericRecord> records = new DataFileStream<>(new ByteArrayInputStream(output.toByteArray()), new GenericDatumReader<>())) {
            final GenericRecord record = records.next();
            assertEquals(7, record.get("value"));
            assertNull(record.get("typo"));
        }
    }

    @ParameterizedTest
    @ValueSource(strings = { "array", "map", "nullable", "array-map-nullable" })
    public void avroProjectionPolicySurvivesContainersAndStillValidatesDeclaredFields(final String container) throws Exception {
        final String childSchema = "{\"type\":\"record\",\"name\":\"ProjectedChild\",\"fields\":[{\"name\":\"value\",\"type\":\"int\"}]}";
        final String payloadSchema = switch (container) {
            case "array" -> "{\"type\":\"array\",\"items\":" + childSchema + "}";
            case "map" -> "{\"type\":\"map\",\"values\":" + childSchema + "}";
            case "nullable" -> "[\"null\"," + childSchema + "]";
            case "array-map-nullable" -> "{\"type\":\"array\",\"items\":{\"type\":\"map\",\"values\":[\"null\"," + childSchema + "]}}";
            default -> throw new AssertionError(container);
        };
        final Schema schema = new Schema.Parser()
                .parse("{\"type\":\"record\",\"name\":\"ProjectionContainer\",\"fields\":[{\"name\":\"payload\",\"type\":" + payloadSchema + "}]}");
        final AvroParser parser = new AvroParser();
        for (final boolean nullExtra : new boolean[] { true, false }) {
            final NullExtraBean bean = new NullExtraBean();
            bean.setTypo(nullExtra ? null : "discard me");
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("value", 7);
            map.put("typo", bean.getTypo());
            for (final Object child : List.of(bean, map)) {
                final Map<String, Object> source = Map.of("payload", wrapAvroChild(container, child));
                final AvroSerConfig config = AvroSerConfig.create().setSchema(schema);
                final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> parser.serialize(source, config));
                assertTrue(failure.getMessage().contains("typo") && failure.getMessage().contains("ProjectedChild"));

                final ByteArrayOutputStream output = new ByteArrayOutputStream();
                parser.serialize(source, config.setIgnoreUnknownFields(true), output);
                try (DataFileStream<GenericRecord> records = new DataFileStream<>(new ByteArrayInputStream(output.toByteArray()), new GenericDatumReader<>())) {
                    Object decoded = records.next().get("payload");
                    if (container.startsWith("array")) {
                        final List<?> items = assertInstanceOf(List.class, decoded);
                        assertEquals(1, items.size());
                        decoded = items.get(0);
                    }
                    if (container.contains("map")) {
                        final Map<?, ?> entries = assertInstanceOf(Map.class, decoded);
                        assertEquals(1, entries.size());
                        assertEquals("child", entries.keySet().iterator().next().toString());
                        decoded = entries.values().iterator().next();
                    }
                    final GenericRecord record = assertInstanceOf(GenericRecord.class, decoded);
                    assertEquals(7, record.get("value"));
                    assertEquals(1, record.getSchema().getFields().size());
                    assertFalse(records.hasNext());
                }
            }
        }
        // Projection discards unknown fields, but must never bypass conversion of a declared field.
        final Map<String, Object> overflow = Map.of("value", (long) Integer.MAX_VALUE + 1, "typo", "discard me");
        assertThrows(ArithmeticException.class, () -> parser.serialize(Map.of("payload", wrapAvroChild(container, overflow)),
                AvroSerConfig.create().setSchema(schema).setIgnoreUnknownFields(true)));
        if (container.equals("nullable")) {
            final Map<String, Object> source = new LinkedHashMap<>();
            source.put("payload", null);
            final ByteArrayOutputStream output = new ByteArrayOutputStream();
            parser.serialize(source, AvroSerConfig.create().setSchema(schema), output);
            try (DataFileStream<GenericRecord> records = new DataFileStream<>(new ByteArrayInputStream(output.toByteArray()), new GenericDatumReader<>())) {
                assertNull(records.next().get("payload"));
                assertFalse(records.hasNext());
            }
        }
    }

    private static Object wrapAvroChild(final String container, final Object child) {
        return switch (container) {
            case "array" -> List.of(child);
            case "map" -> Map.of("child", child);
            case "nullable" -> child;
            case "array-map-nullable" -> List.of(Map.of("child", child));
            default -> throw new AssertionError(container);
        };
    }

    @ParameterizedTest
    @ValueSource(strings = { "batch", "positional" })
    public void avroProjectionPolicyReachesLaterRecordsAndNestedPositionalFields(final String shape) throws Exception {
        final String itemSchema = "{\"type\":\"record\",\"name\":\"ProjectedItem\",\"fields\":[{\"name\":\"value\",\"type\":\"int\"}]}";
        final boolean batch = shape.equals("batch");
        final Schema schema = new Schema.Parser().parse(batch ? itemSchema
                : "{\"type\":\"record\",\"name\":\"PositionalRoot\",\"fields\":[{\"name\":\"positional\",\"type\":{"
                        + "\"type\":\"record\",\"name\":\"PositionalFields\",\"fields\":[{\"name\":\"child\",\"type\":" + itemSchema + "}]}}]}");
        final AvroParser parser = new AvroParser();
        for (final boolean nullExtra : new boolean[] { true, false }) {
            final NullExtraBean bean = new NullExtraBean();
            bean.setTypo(nullExtra ? null : "discard me");
            final Map<String, Object> map = new LinkedHashMap<>();
            map.put("value", 7);
            map.put("typo", bean.getTypo());
            for (final Object child : List.of(bean, map)) {
                // The first batch record is valid. A later record (or a nested positional field) must still be checked.
                final Object source = batch ? List.of(Map.of("value", 1), child) : Map.of("positional", List.of(child));
                final AvroSerConfig config = AvroSerConfig.create().setSchema(schema);
                final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> parser.serialize(source, config));
                assertTrue(failure.getMessage().contains("typo") && failure.getMessage().contains("ProjectedItem"), failure.getMessage());
                final ByteArrayOutputStream output = new ByteArrayOutputStream();
                parser.serialize(source, config.setIgnoreUnknownFields(true), output);
                try (DataFileStream<GenericRecord> records = new DataFileStream<>(new ByteArrayInputStream(output.toByteArray()), new GenericDatumReader<>())) {
                    if (batch) {
                        assertEquals(1, records.next().get("value"));
                    }
                    final GenericRecord record = records.next();
                    final GenericRecord item = batch ? record : (GenericRecord) ((GenericRecord) record.get("positional")).get("child");
                    assertEquals(7, item.get("value"));
                    assertEquals(1, item.getSchema().getFields().size());
                    assertFalse(records.hasNext());
                }
                // Reusing the parser/config must not make projection sticky after the caller restores strict mode.
                config.setIgnoreUnknownFields(false);
                assertThrows(IllegalArgumentException.class, () -> parser.serialize(source, config));
            }
        }
        final Object overflow = Map.of("value", (long) Integer.MAX_VALUE + 1, "typo", "discard me");
        final Object invalidSource = batch ? List.of(Map.of("value", 1), overflow) : Map.of("positional", List.of(overflow));
        assertThrows(ArithmeticException.class, () -> parser.serialize(invalidSource, AvroSerConfig.create().setSchema(schema).setIgnoreUnknownFields(true)));
    }

    public static class NullExtraBean {
        private int value = 7;
        private String typo;

        public int getValue() {
            return value;
        }

        public void setValue(final int value) {
            this.value = value;
        }

        public String getTypo() {
            return typo;
        }

        public void setTypo(final String typo) {
            this.typo = typo;
        }
    }

    public static class PolicyBean {
        private int value;

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }
    }
}
