package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.specific.SpecificRecord;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.entity.User;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Strings;

public class AvroParserTest extends TestBase {

    static final AvroParser avroParser = ParserFactory.createAvroParser();

    private AvroParser parser;
    private Schema testSchema;
    private Schema arraySchema;
    private Schema nullableSchema;

    @TempDir
    File tempDir;

    @BeforeEach
    public void setUp() {
        parser = new AvroParser();
        testSchema = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"TestRecord\",\"fields\":[" + "{\"name\":\"name\",\"type\":\"string\"},"
                + "{\"name\":\"age\",\"type\":\"int\"}" + "]}");
        arraySchema = new Schema.Parser().parse("{\"type\":\"array\",\"items\":\"string\"}");
        nullableSchema = new Schema.Parser()
                .parse("{\"type\":\"record\",\"name\":\"NullableRecord\",\"fields\":[" + "{\"name\":\"name\",\"type\":[\"null\",\"string\"],\"default\":null},"
                        + "{\"name\":\"age\",\"type\":[\"null\",\"int\"],\"default\":null}" + "]}");
    }

    public static class TestBean {
        private String name;
        private int age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }
    }

    public static class MockSpecificRecord implements SpecificRecord {
        public static final Schema SCHEMA$ = new Schema.Parser()
                .parse("{\"type\":\"record\",\"name\":\"com.landawn.abacus.parser.AvroParserTest.MockSpecificRecord\",\"fields\":["
                        + "{\"name\":\"id\",\"type\":\"string\"}]}");

        private String id;

        public MockSpecificRecord() {
        }

        public MockSpecificRecord(String id) {
            this.id = id;
        }

        @Override
        public Schema getSchema() {
            return SCHEMA$;
        }

        @Override
        public Object get(int field) {
            if (field == 0) {
                return id;
            }
            throw new IndexOutOfBoundsException();
        }

        @Override
        public void put(int field, Object value) {
            if (field == 0) {
                id = (String) value;
            } else {
                throw new IndexOutOfBoundsException();
            }
        }

        public String getId() {
            return id;
        }
    }

    private static final class CloseTrackingOutputStream extends ByteArrayOutputStream {
        private boolean closed = false;

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        boolean isClosed() {
            return closed;
        }
    }

    private static final class CloseTrackingInputStream extends ByteArrayInputStream {
        private boolean closed = false;

        CloseTrackingInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        boolean isClosed() {
            return closed;
        }
    }

    static final Schema schema;

    static {
        try {
            schema = new Schema.Parser().parse(new File("./src/test/resources/user.avsc"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private GenericRecord record(final String name, final int age) {
        GenericRecord record = new GenericData.Record(testSchema);
        record.put("name", name);
        record.put("age", age);
        return record;
    }

    private AvroSerConfig serConfig() {
        return AvroSerConfig.create().setSchema(testSchema);
    }

    private AvroDeserConfig deserConfig() {
        return AvroDeserConfig.create().setSchema(testSchema);
    }

    @Test
    public void testSerializeWithSchema() {
        List<Map<String, Object>> list = new ArrayList<>();
        list.add(N.asMap("name", "Item1", "age", 10));
        list.add(N.asMap("name", "Item2", "age", 20));
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        parser.serialize(list, serConfig(), os);
        assertTrue(os.toByteArray().length > 0);

        Map<String, Object> map = N.asMap("name", "MapUser", "age", 40);
        assertTrue(parser.serialize(map, serConfig()).length() > 0);
        assertTrue(parser.serialize(Arrays.asList("one", "two", "three"), AvroSerConfig.create().setSchema(arraySchema)).length() > 0);
        assertTrue(
                parser.serialize(N.toList(1, 2, 3, 4, 5), AvroSerConfig.create().setSchema(new Schema.Parser().parse("{\"type\":\"array\",\"items\":\"int\"}")))
                        .length() > 0);

        TestBean bean = new TestBean();
        bean.setName("BeanUser");
        bean.setAge(40);
        assertTrue(parser.serialize(bean, serConfig()).length() > 0);

        String empty = parser.serialize(new ArrayList<>(), AvroSerConfig.create().setSchema(arraySchema));
        List<String> restored = parser.deserialize(empty, AvroDeserConfig.create().setSchema(arraySchema).setElementType(String.class), List.class);
        assertNotNull(restored);
        assertTrue(restored.isEmpty());
    }

    @Test
    public void testSerializeUserRoundTrip() throws Exception {
        User user1 = new User();
        user1.setName("Alyssa");
        user1.setFavoriteNumber(256);

        String str = avroParser.serialize(user1);
        assertTrue(str.length() > 0);
        GenericRecord record = avroParser.deserialize(str, User.class);
        assertEquals("Alyssa", record.get("name").toString());
        assertEquals(256, record.get("favorite_number"));

        Map<String, Object> m = avroParser.deserialize(str, AvroDeserConfig.create().setSchema(schema), Map.class);
        assertEquals("Alyssa", m.get("name").toString());
        assertEquals(256, m.get("favorite_number"));

        User user2 = new User("Ben", 7, "red");
        User user3 = User.newBuilder().setName("Charlie").setFavoriteColor("blue").setFavoriteNumber(null).build();
        str = avroParser.serialize(N.toList(user1, user2, user3));
        List<User> users = avroParser.deserialize(str, AvroDeserConfig.create().setSchema(schema).setElementType(User.class), List.class);
        assertEquals(3, users.size());
        users = avroParser.deserialize(str, AvroDeserConfig.create().setElementType(User.class), List.class);
        assertEquals(3, users.size());
    }

    @Test
    public void testSerializeUserToFile() throws Exception {
        User user1 = new User();
        user1.setName("Alyssa");
        user1.setFavoriteNumber(256);
        File file = new File("./src/test/resources/test.avsc");
        avroParser.serialize(user1, file);
        assertTrue(IOUtil.readAllToString(file).length() > 0);
        GenericRecord record = avroParser.deserialize(file, User.class);
        assertEquals("Alyssa", record.get("name").toString());

        User user2 = new User("Ben", 7, "red");
        User user3 = User.newBuilder().setName("Charlie").setFavoriteColor("blue").setFavoriteNumber(null).build();
        avroParser.serialize(N.toList(user1, user2, user3), file);
        List<User> users = avroParser.deserialize(file, AvroDeserConfig.create().setSchema(schema).setElementType(User.class), List.class);
        assertEquals(3, users.size());
        users = avroParser.deserialize(file, AvroDeserConfig.create().setElementType(User.class), List.class);
        assertEquals(3, users.size());
        IOUtil.deleteIfExists(file);
    }

    @Test
    public void testSerializeGenericRecordAndMap() {
        GenericRecord user1 = new GenericData.Record(schema);
        user1.put("name", "Alyssa");
        user1.put("favorite_number", 256);
        String str = avroParser.serialize(user1, AvroSerConfig.create().setSchema(schema));
        assertTrue(str.length() > 0);

        Map<String, Object> user2 = new HashMap<>();
        user2.put("name", "Ben");
        user2.put("favorite_number", 7);
        user2.put("favorite_color", "red");
        str = avroParser.serialize(user2, AvroSerConfig.create().setSchema(schema));
        AvroDeserConfig ds = AvroDeserConfig.create().setSchema(schema);
        GenericRecord record = avroParser.deserialize(str, ds, GenericRecord.class);
        assertEquals("Ben", record.get("name").toString());
        Map<String, Object> m = avroParser.deserialize(str, ds, Map.class);
        assertEquals("Ben", m.get("name").toString());
        assertEquals(7, m.get("favorite_number"));
    }

    @Test
    public void testSerializeToDestinations() throws IOException {
        GenericRecord record = record("John", 30);
        String encoded = parser.serialize(record, serConfig());
        assertTrue(encoded.length() > 0);
        assertDoesNotThrow(() -> Strings.base64Decode(encoded));

        assertThrows(IllegalArgumentException.class, () -> parser.serialize(N.asMap("name", "John", "age", 30), new AvroSerConfig()));
        assertEquals("", parser.serialize(null, (AvroSerConfig) null));
        assertThrows(IllegalArgumentException.class, () -> parser.serialize(new StringBuilder("test"), serConfig()));
        assertThrows(UnsupportedOperationException.class, () -> parser.serialize(record("Alice", 28), serConfig(), new StringWriter()));

        File outputFile = new File(tempDir, "test.avro");
        parser.serialize(record("Jane", 25), serConfig(), outputFile);
        assertTrue(outputFile.exists());
        assertTrue(outputFile.length() > 0);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        parser.serialize(record("Bob", 35), serConfig(), os);
        assertTrue(os.toByteArray().length > 0);

        os.reset();
        parser.serialize(List.of(N.asMap("name", "User1", "age", 20), N.asMap("name", "User2", "age", 25)), serConfig(), os);
        assertTrue(os.toByteArray().length > 0);

        CloseTrackingOutputStream tracking = new CloseTrackingOutputStream();
        parser.serialize(N.asMap("name", "NoClose", "age", 26), serConfig(), tracking);
        assertFalse(tracking.isClosed());
        tracking.write(1);

        assertThrows(IllegalArgumentException.class, () -> parser.serialize(N.asMap("test", "value"), null, new ByteArrayOutputStream()));
        assertThrows(IllegalArgumentException.class, () -> parser.serialize(record("Test", 30), serConfig(), tempDir));
    }

    @Test
    public void testSerializeToFile_EdgeCase() throws IOException {
        File unwritable = new File(tempDir, "unwritable.avro");
        assertTrue(unwritable.createNewFile());
        assertTrue(unwritable.setReadOnly());
        Assumptions.assumeTrue(!unwritable.canWrite(), "this platform/user can write a read-only file; nothing to provoke");
        try {
            assertThrows(UncheckedIOException.class, () -> parser.serialize(record("Test", 30), serConfig(), unwritable));
        } finally {
            unwritable.setWritable(true);
        }

        File file = new File(tempDir, "existing.avro");
        Files.writeString(file.toPath(), "preserve-me", StandardCharsets.UTF_8);
        assertThrows(IllegalArgumentException.class, () -> parser.serialize(N.asMap("name", "No schema"), null, file));
        assertEquals("preserve-me", Files.readString(file.toPath(), StandardCharsets.UTF_8));
        assertThrows(IllegalArgumentException.class, () -> parser.serialize(Arrays.asList(new MockSpecificRecord("id"), null), null, file));
        assertEquals("preserve-me", Files.readString(file.toPath(), StandardCharsets.UTF_8));
    }

    @Test
    public void testSerializeSpecificAndGenericRecords() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(new MockSpecificRecord("test-id-123"), null, baos);
        assertTrue(baos.toByteArray().length > 0);

        baos.reset();
        parser.serialize(Arrays.asList(new MockSpecificRecord("id1"), new MockSpecificRecord("id2"), new MockSpecificRecord("id3")), null, baos);
        assertTrue(baos.toByteArray().length > 0);

        baos.reset();
        parser.serialize(Arrays.asList(record("Generic1", 10), record("Generic2", 20)), serConfig(), baos);
        assertTrue(baos.toByteArray().length > 0);
    }

    @Test
    public void testToGenericRecord_CollectionFieldsByPosition() throws Exception {
        Method method = AvroParser.class.getDeclaredMethod("toGenericRecord", Object.class, Schema.class, boolean.class);
        method.setAccessible(true);
        // Projection changes named-field validation, not positional record conversion.
        for (boolean ignoreUnknownFields : new boolean[] { false, true }) {
            GenericRecord result = (GenericRecord) method.invoke(parser, Arrays.asList("ListUser", 29), testSchema, ignoreUnknownFields);
            assertEquals("ListUser", result.get("name").toString());
            assertEquals(29, result.get("age"));
        }
    }

    @Test
    public void testDeserialize() throws IOException {
        String serialized = parser.serialize(record("TestUser", 50), serConfig());
        GenericRecord result = parser.deserialize(serialized, deserConfig(), GenericRecord.class);
        assertEquals("TestUser", result.get("name").toString());
        assertEquals(50, result.get("age"));

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        parser.serialize(N.asMap("name", "InputNoClose", "age", 41), serConfig(), os);
        CloseTrackingInputStream is = new CloseTrackingInputStream(os.toByteArray());
        result = parser.deserialize(is, deserConfig(), GenericRecord.class);
        assertEquals("InputNoClose", result.get("name").toString());
        assertFalse(is.isClosed());

        os.reset();
        parser.serialize(record("Alice", 30), serConfig(), os);
        result = parser.deserialize(new ByteArrayInputStream(os.toByteArray()), deserConfig(), GenericRecord.class);
        assertEquals("Alice", result.get("name").toString());
        Map<String, Object> asMap = parser.deserialize(new ByteArrayInputStream(os.toByteArray()), deserConfig(), Map.class);
        assertEquals("Alice", asMap.get("name").toString());
        assertEquals(30, asMap.get("age"));

        File file = new File(tempDir, "deserialize_test.avro");
        parser.serialize(record("FileUser", 45), serConfig(), file);
        result = parser.deserialize(file, deserConfig(), GenericRecord.class);
        assertEquals("FileUser", result.get("name").toString());
        assertEquals(45, result.get("age"));

        os.reset();
        parser.serialize(record("StreamUser", 55), serConfig(), os);
        result = parser.deserialize(new ByteArrayInputStream(os.toByteArray()), deserConfig(), GenericRecord.class);
        assertEquals("StreamUser", result.get("name").toString());

        os.reset();
        parser.serialize(record("MapDeserializeUser", 60), serConfig(), os);
        Map<String, Object> map = parser.deserialize(new ByteArrayInputStream(os.toByteArray()), deserConfig(), HashMap.class);
        assertEquals("MapDeserializeUser", map.get("name").toString());
        assertEquals(60, map.get("age"));

        os.reset();
        parser.serialize(record("BeanDeserializeUser", 65), serConfig(), os);
        TestBean bean = parser.deserialize(new ByteArrayInputStream(os.toByteArray()), deserConfig(), TestBean.class);
        assertEquals("BeanDeserializeUser", bean.getName().toString());
        assertEquals(65, bean.getAge());

        assertThrows(UnsupportedOperationException.class, () -> parser.deserialize(new java.io.StringReader("test"), deserConfig(), GenericRecord.class));
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize(Strings.base64Encode("dummy data".getBytes()), new AvroDeserConfig(), Map.class));
        assertThrows(UncheckedIOException.class, () -> parser.deserialize(new ByteArrayInputStream(new byte[0]), deserConfig(), StringBuilder.class));
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize(new ByteArrayInputStream(new byte[0]), null, Map.class));
    }

    @Test
    public void testDeserializeToMapPreservesNullFields() {
        GenericRecord record = new GenericData.Record(nullableSchema);
        record.put("name", null);
        record.put("age", 12);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        parser.serialize(record, AvroSerConfig.create().setSchema(nullableSchema), os);
        Map<String, Object> result = parser.deserialize(new ByteArrayInputStream(os.toByteArray()), AvroDeserConfig.create().setSchema(nullableSchema),
                HashMap.class);
        assertTrue(result.containsKey("name"));
        assertEquals(null, result.get("name"));
        assertEquals(12, result.get("age"));
    }

    @Test
    public void testDeserializeCollection() throws IOException {
        Schema stringArraySchema = new Schema.Parser().parse("{\"type\":\"array\",\"items\":\"string\"}");
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(Arrays.asList("alpha", "beta", "gamma"), AvroSerConfig.create().setSchema(stringArraySchema), baos);
        List<String> result = parser.deserialize(new ByteArrayInputStream(baos.toByteArray()), AvroDeserConfig.create().setSchema(stringArraySchema),
                List.class);
        assertEquals(3, result.size());

        baos.reset();
        parser.serialize(List.of(N.asMap("name", "Element1", "age", 15), N.asMap("name", "Element2", "age", 25)), serConfig(), baos);
        List<HashMap> maps = parser.deserialize(new ByteArrayInputStream(baos.toByteArray()),
                AvroDeserConfig.create().setSchema(testSchema).setElementType(HashMap.class), List.class);
        assertEquals(2, maps.size());
        assertEquals("Element1", maps.get(0).get("name").toString());
        assertEquals(15, maps.get(0).get("age"));
    }

    @Test
    public void testDeserializeSpecificRecord_EdgeCase() throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(new MockSpecificRecord("deserialize-test"), null, baos);
        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        assertThrows(ClassCastException.class, () -> {
            MockSpecificRecord result = parser.deserialize(bais, null, MockSpecificRecord.class);
            assertNotNull(result);
            assertEquals("deserialize-test", result.getId());
        });

        baos.reset();
        parser.serialize(Arrays.asList(new MockSpecificRecord("col-id1"), new MockSpecificRecord("col-id2")), null, baos);
        ByteArrayInputStream colIn = new ByteArrayInputStream(baos.toByteArray());
        AvroDeserConfig config = new AvroDeserConfig();
        config.setElementType(MockSpecificRecord.class);
        assertThrows(ClassCastException.class, () -> {
            List<MockSpecificRecord> result = parser.deserialize(colIn, config, List.class);
            assertNotNull(result);
            assertEquals(2, result.size());
            assertEquals("col-id1", result.get(0).getId());
            assertEquals("col-id2", result.get(1).getId());
        });
    }

    @Test
    public void testRecordSequenceToTypedMapCollection() throws Exception {
        String encoded = parser.serialize(record("ListUser", 29), serConfig());
        List<Map<String, String>> result = parser.deserialize(encoded, deserConfig(), Type.of("List<Map<String,String>>"));
        assertEquals(List.of(Map.of("name", "ListUser", "age", "29")), result);

        GenericRecord nullable = new GenericData.Record(nullableSchema);
        nullable.put("name", null);
        nullable.put("age", 12);
        encoded = parser.serialize(nullable, AvroSerConfig.create().setSchema(nullableSchema));
        List<Map<String, Object>> withNull = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(nullableSchema),
                Type.of("List<Map<String,Object>>"));
        assertEquals(1, withNull.size());
        assertTrue(withNull.get(0).containsKey("name"));
        assertEquals(null, withNull.get(0).get("name"));
        assertEquals(12, withNull.get(0).get("age"));
    }

    // ---------------------------------------------------------------------------------------------
    // reviewFixes20260906: P6-01 (write-side range checks), P6-05 (enum fields), P6-06 (unknown
    // properties/fields), P6-07 (empty and null String sources).
    // ---------------------------------------------------------------------------------------------

    public static class LongAgeBean {
        private String name;
        private long age;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public long getAge() {
            return age;
        }

        public void setAge(long age) {
            this.age = age;
        }
    }

    public static class ExtraPropertyBean extends TestBean {
        private String extra = "not-in-schema";

        public String getExtra() {
            return extra;
        }

        public void setExtra(String extra) {
            this.extra = extra;
        }
    }

    public enum Color {
        RED, GREEN
    }

    public static class Painted {
        private Color color;
        private String name;

        public Color getColor() {
            return color;
        }

        public void setColor(Color color) {
            this.color = color;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    private static final Schema LONG_ID_SCHEMA = new Schema.Parser()
            .parse("{\"type\":\"record\",\"name\":\"LongRecord\",\"fields\":[{\"name\":\"id\",\"type\":\"long\"}]}");

    private static final Schema INT_ARRAY_SCHEMA = new Schema.Parser().parse("{\"type\":\"array\",\"items\":\"int\"}");

    private static final Schema INT_MAP_FIELD_SCHEMA = new Schema.Parser()
            .parse("{\"type\":\"record\",\"name\":\"MapRecord\",\"fields\":[{\"name\":\"counts\",\"type\":{\"type\":\"map\",\"values\":\"int\"}}]}");

    private static final Schema FLOAT_SCHEMA = new Schema.Parser()
            .parse("{\"type\":\"record\",\"name\":\"FloatRecord\",\"fields\":[{\"name\":\"ratio\",\"type\":\"float\"}]}");

    private static final Schema ENUM_SCHEMA = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Painted\",\"fields\":["
            + "{\"name\":\"color\",\"type\":{\"type\":\"enum\",\"name\":\"Color\",\"symbols\":[\"RED\",\"GREEN\"]}},"
            + "{\"name\":\"name\",\"type\":\"string\"}]}");

    private static final Schema NULLABLE_ENUM_SCHEMA = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"PaintedN\",\"fields\":["
            + "{\"name\":\"color\",\"type\":[\"null\",{\"type\":\"enum\",\"name\":\"ColorN\",\"symbols\":[\"RED\",\"GREEN\"]}],\"default\":null},"
            + "{\"name\":\"name\",\"type\":\"string\"}]}");

    private static final Schema NAME_AGE_ZIP_SCHEMA = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Evolved\",\"fields\":["
            + "{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"age\",\"type\":\"int\"},{\"name\":\"zip\",\"type\":\"string\"}]}");

    private Map<String, Object> nameAge(final Object age) {
        final Map<String, Object> map = new HashMap<>();
        map.put("name", "a");
        map.put("age", age);
        return map;
    }

    private GenericRecord writeAndReadRecord(final Object obj, final Schema schema) {
        final String encoded = parser.serialize(obj, AvroSerConfig.create().setSchema(schema));
        return parser.deserialize(encoded, AvroDeserConfig.create().setSchema(schema), GenericRecord.class);
    }

    @Test
    public void reviewFixes20260906_serializeRejectsNumbersThatDoNotFitIntField() {
        final AvroSerConfig config = AvroSerConfig.create().setSchema(testSchema);

        assertThrows(ArithmeticException.class, () -> parser.serialize(nameAge(5_000_000_000L), config));
        assertThrows(ArithmeticException.class, () -> parser.serialize(nameAge(Long.MAX_VALUE), config));
        assertThrows(ArithmeticException.class, () -> parser.serialize(nameAge(Long.MIN_VALUE), config));
        assertThrows(ArithmeticException.class, () -> parser.serialize(nameAge(Integer.MAX_VALUE + 1L), config));
        assertThrows(NumberFormatException.class, () -> parser.serialize(nameAge(3.7d), config));
        assertThrows(NumberFormatException.class, () -> parser.serialize(nameAge(new BigDecimal("1.5")), config));

        final LongAgeBean bean = new LongAgeBean();
        bean.setName("a");
        bean.setAge(5_000_000_000L);
        assertThrows(ArithmeticException.class, () -> parser.serialize(bean, config));
        bean.setAge(Long.MAX_VALUE);
        assertThrows(ArithmeticException.class, () -> parser.serialize(bean, config, new ByteArrayOutputStream()));

        // In-range values of a wider or narrower wrapper type still write and read back unchanged.
        assertEquals(42, writeAndReadRecord(nameAge(42L), testSchema).get("age"));
        assertEquals(Integer.MAX_VALUE, writeAndReadRecord(nameAge((long) Integer.MAX_VALUE), testSchema).get("age"));
        assertEquals(Integer.MIN_VALUE, writeAndReadRecord(nameAge((long) Integer.MIN_VALUE), testSchema).get("age"));
        assertEquals(7, writeAndReadRecord(nameAge((short) 7), testSchema).get("age"));
        assertEquals(3, writeAndReadRecord(nameAge((byte) 3), testSchema).get("age"));
        assertEquals(15, writeAndReadRecord(nameAge(new BigDecimal("15")), testSchema).get("age"));
        bean.setAge(Integer.MAX_VALUE);
        assertEquals(Integer.MAX_VALUE, writeAndReadRecord(bean, testSchema).get("age"));

        final LongAgeBean readBack = parser.deserialize(parser.serialize(bean, config), AvroDeserConfig.create().setSchema(testSchema), LongAgeBean.class);
        assertEquals(Integer.MAX_VALUE, readBack.getAge());
        assertEquals("a", readBack.getName());

        // Only Numbers are intercepted: a String for an int field is still rejected by Avro itself.
        assertThrows(DataFileWriter.AppendWriteException.class, () -> parser.serialize(nameAge("5"), config));
    }

    @Test
    public void reviewFixes20260906_serializeRejectsNumbersThatDoNotFitLongField() {
        final AvroSerConfig config = AvroSerConfig.create().setSchema(LONG_ID_SCHEMA);

        assertThrows(ArithmeticException.class, () -> parser.serialize(Map.of("id", new BigInteger("100000000000000000000")), config));
        assertThrows(NumberFormatException.class, () -> parser.serialize(Map.of("id", 1e300d), config));
        assertThrows(NumberFormatException.class, () -> parser.serialize(Map.of("id", 2.5d), config));

        assertEquals(42L, writeAndReadRecord(Map.of("id", 42), LONG_ID_SCHEMA).get("id"));
        assertEquals(7L, writeAndReadRecord(Map.of("id", (short) 7), LONG_ID_SCHEMA).get("id"));
        assertEquals(Long.MAX_VALUE, writeAndReadRecord(Map.of("id", Long.MAX_VALUE), LONG_ID_SCHEMA).get("id"));
        assertEquals(Long.MAX_VALUE, writeAndReadRecord(Map.of("id", BigInteger.valueOf(Long.MAX_VALUE)), LONG_ID_SCHEMA).get("id"));
    }

    @Test
    public void reviewFixes20260906_serializeRangeChecksArrayItemsAndMapValues() {
        final AvroSerConfig arrayConfig = AvroSerConfig.create().setSchema(INT_ARRAY_SCHEMA);
        assertThrows(ArithmeticException.class, () -> parser.serialize(Arrays.asList(1, 5_000_000_000L), arrayConfig));

        final List<Integer> items = parser.deserialize(parser.serialize(Arrays.asList(1L, 2L), arrayConfig),
                AvroDeserConfig.create().setSchema(INT_ARRAY_SCHEMA).setElementType(Integer.class), List.class);
        assertEquals(List.of(1, 2), items);

        final AvroSerConfig mapConfig = AvroSerConfig.create().setSchema(INT_MAP_FIELD_SCHEMA);
        assertThrows(ArithmeticException.class, () -> parser.serialize(Map.of("counts", Map.of("k", 5_000_000_000L)), mapConfig));

        final GenericRecord record = writeAndReadRecord(Map.of("counts", Map.of("k", 9L)), INT_MAP_FIELD_SCHEMA);
        final Map<?, ?> counts = (Map<?, ?>) record.get("counts");
        assertEquals(1, counts.size());
        assertEquals(9, counts.values().iterator().next());
    }

    @Test
    public void reviewFixes20260906_serializeNullableIntUnionAppliesRangeCheck() {
        final AvroSerConfig config = AvroSerConfig.create().setSchema(nullableSchema);

        assertThrows(ArithmeticException.class, () -> parser.serialize(nameAge(5_000_000_000L), config));
        assertEquals(42, writeAndReadRecord(nameAge(42L), nullableSchema).get("age"));
        assertEquals(42, writeAndReadRecord(nameAge(42), nullableSchema).get("age"));

        // The null branch is untouched.
        final GenericRecord nullAge = writeAndReadRecord(nameAge(null), nullableSchema);
        assertTrue(nullAge.hasField("age"));
        assertEquals(null, nullAge.get("age"));
    }

    @Test
    public void reviewFixes20260906_doubleToFloatNarrowsWithoutRangeCheck() {
        assertEquals(0.1f, writeAndReadRecord(Map.of("ratio", 0.1d), FLOAT_SCHEMA).get("ratio"));
        assertEquals(1.5f, writeAndReadRecord(Map.of("ratio", 1.5f), FLOAT_SCHEMA).get("ratio"));
        assertEquals(Float.POSITIVE_INFINITY, writeAndReadRecord(Map.of("ratio", 1e300d), FLOAT_SCHEMA).get("ratio"));
    }

    @Test
    public void reviewFixes20260906_enumPropertyWritesAndReadsEnumSchema() {
        final Painted painted = new Painted();
        painted.setColor(Color.GREEN);
        painted.setName("n");

        final AvroSerConfig serConfig = AvroSerConfig.create().setSchema(ENUM_SCHEMA);
        final AvroDeserConfig deserConfig = AvroDeserConfig.create().setSchema(ENUM_SCHEMA);

        final Painted readBack = parser.deserialize(parser.serialize(painted, serConfig), deserConfig, Painted.class);
        assertEquals(Color.GREEN, readBack.getColor());
        assertEquals("n", readBack.getName());

        final GenericRecord record = writeAndReadRecord(painted, ENUM_SCHEMA);
        assertTrue(record.get("color") instanceof GenericData.EnumSymbol);
        assertEquals("GREEN", record.get("color").toString());

        // A symbol name in a Map and an existing EnumSymbol are accepted as well.
        assertEquals("RED", writeAndReadRecord(Map.of("color", "RED", "name", "m"), ENUM_SCHEMA).get("color").toString());
        final GenericData.EnumSymbol symbol = new GenericData.EnumSymbol(ENUM_SCHEMA.getField("color").schema(), "RED");
        assertEquals("RED", writeAndReadRecord(Map.of("color", symbol, "name", "m"), ENUM_SCHEMA).get("color").toString());

        // Unicode in the sibling string field survives next to the enum.
        assertEquals("名前", writeAndReadRecord(Map.of("color", Color.RED, "name", "名前"), ENUM_SCHEMA).get("name").toString());

        final IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class,
                () -> parser.serialize(Map.of("color", "BLUE", "name", "m"), serConfig));
        assertTrue(thrown.getMessage().contains("BLUE"), thrown.getMessage());

        // Nullable union enum: null stays null, a constant resolves to the enum branch.
        final Painted unpainted = new Painted();
        unpainted.setName("u");
        final AvroDeserConfig nullableDeser = AvroDeserConfig.create().setSchema(NULLABLE_ENUM_SCHEMA);
        final Painted readUnpainted = parser.deserialize(parser.serialize(unpainted, AvroSerConfig.create().setSchema(NULLABLE_ENUM_SCHEMA)), nullableDeser,
                Painted.class);
        assertEquals(null, readUnpainted.getColor());
        assertEquals("u", readUnpainted.getName());

        unpainted.setColor(Color.RED);
        assertEquals(Color.RED,
                parser.deserialize(parser.serialize(unpainted, AvroSerConfig.create().setSchema(NULLABLE_ENUM_SCHEMA)), nullableDeser, Painted.class)
                        .getColor());
    }

    @Test
    public void reviewFixes20260906_unknownPropertiesRequireExplicitProjection() {
        final ExtraPropertyBean bean = new ExtraPropertyBean();
        bean.setName("x");
        bean.setAge(9);
        final Map<String, Object> map = nameAge(10);
        map.put("zzz", "ignored");
        for (Object source : new Object[] { bean, map }) {
            assertThrows(IllegalArgumentException.class, () -> parser.serialize(source, AvroSerConfig.create().setSchema(testSchema)));
            final String projected = parser.serialize(source, AvroSerConfig.create().setSchema(testSchema).setIgnoreUnknownFields(true));
            assertTrue(!projected.isEmpty());
        }
        assertThrows(RuntimeException.class, () -> parser.serialize(Map.of("name", "only"), AvroSerConfig.create().setSchema(testSchema)));
    }

    @Test
    public void reviewFixes20260906_unmatchedSchemaFieldHonoursIgnoreUnmatchedProperty() {
        final Map<String, Object> evolved = nameAge(33);
        evolved.put("zip", "94000");
        final String encoded = parser.serialize(evolved, AvroSerConfig.create().setSchema(NAME_AGE_ZIP_SCHEMA));

        // Default: unmatched schema fields are skipped (the schema-evolution case).
        final TestBean bean = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(NAME_AGE_ZIP_SCHEMA), TestBean.class);
        assertEquals("a", bean.getName());
        assertEquals(33, bean.getAge());

        final TestBean viaType = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(NAME_AGE_ZIP_SCHEMA), Type.of(TestBean.class));
        assertEquals(33, viaType.getAge());

        final List<TestBean> beans = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(NAME_AGE_ZIP_SCHEMA).setElementType(TestBean.class),
                List.class);
        assertEquals(1, beans.size());
        assertEquals("a", beans.get(0).getName());

        // Disabled: the unmatched field is a ParsingException, like the JSON parser's unknown-property rule.
        final AvroDeserConfig strict = AvroDeserConfig.create().setSchema(NAME_AGE_ZIP_SCHEMA).setIgnoreUnmatchedProperty(false);
        final ParsingException thrown = assertThrows(ParsingException.class, () -> parser.deserialize(encoded, strict, TestBean.class));
        assertTrue(thrown.getMessage().contains("zip"), thrown.getMessage());
        assertThrows(ParsingException.class, () -> parser.deserialize(encoded, strict.copy().setElementType(TestBean.class), List.class));

        // A Map target has no properties to match, so the setting does not apply.
        final Map<String, Object> asMap = parser.deserialize(encoded, strict, Map.class);
        assertEquals("94000", asMap.get("zip").toString());

        // A schema with exactly the bean's properties is unaffected by the strict setting.
        final String exact = parser.serialize(nameAge(1), AvroSerConfig.create().setSchema(testSchema));
        assertEquals(1, parser.deserialize(exact, AvroDeserConfig.create().setSchema(testSchema).setIgnoreUnmatchedProperty(false), TestBean.class).getAge());
    }

    @Test
    public void reviewFixes20260906_emptyAndNullStringSources() {
        final AvroDeserConfig config = AvroDeserConfig.create().setSchema(testSchema);

        assertEquals("", parser.serialize(null, AvroSerConfig.create().setSchema(testSchema)));
        assertEquals(null, parser.deserialize(parser.serialize(null, AvroSerConfig.create().setSchema(testSchema)), config, TestBean.class));

        assertEquals(null, parser.deserialize("", config, TestBean.class));
        assertEquals(null, parser.deserialize("", config, Type.of(TestBean.class)));
        assertEquals(null, parser.deserialize("", config, List.class));
        assertEquals(null, parser.deserialize("", config, Map.class));
        assertEquals(null, parser.deserialize("", config, GenericRecord.class));
        assertEquals(null, parser.deserialize("", null, TestBean.class));
        assertEquals(0, (int) parser.deserialize("", config, int.class));
        assertEquals(0, (int) parser.deserialize("", config, Type.of(int.class)));

        assertThrows(IllegalArgumentException.class, () -> parser.deserialize((String) null, config, TestBean.class));
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize((String) null, config, Type.of(TestBean.class)));

        // A blank-but-not-empty string is not a valid Base64 Avro container.
        assertThrows(RuntimeException.class, () -> parser.deserialize(" ", config, TestBean.class));

        // Stream sources have no such shortcut: an empty stream is not an Avro container.
        assertThrows(UncheckedIOException.class, () -> parser.deserialize(new ByteArrayInputStream(new byte[0]), config, TestBean.class));
    }

    @Test
    public void testDeserializeRootMapSchemaIntoMapTarget() {
        final AvroParser avroParser = new AvroParser();
        final Schema mapSchema = Schema.createMap(Schema.create(Schema.Type.INT));
        final Map<String, Object> source = new java.util.LinkedHashMap<>();
        source.put("a", 1);
        source.put("b", 2);

        final String encoded = avroParser.serialize(source, new AvroSerConfig().setSchema(mapSchema));
        final AvroDeserConfig config = AvroDeserConfig.create().setSchema(mapSchema);

        final Map<String, Integer> typed = avroParser.deserialize(encoded, config, Type.of("Map<String, Integer>"));
        assertEquals(source, typed);

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        avroParser.serialize(source, new AvroSerConfig().setSchema(mapSchema), os);
        final Map<?, ?> untyped = avroParser.deserialize(new ByteArrayInputStream(os.toByteArray()), config, HashMap.class);
        assertEquals(HashMap.class, untyped.getClass());
        assertEquals(2, untyped.size());
        final Map<String, Object> byKeyText = new HashMap<>();
        untyped.forEach((k, v) -> byKeyText.put(k.toString(), v));
        assertEquals(source, byKeyText);
    }

    @Test
    public void testDeserializeNonRecordDatumIntoBeanOrMapTargetThrowsIllegalArgument() {
        final AvroParser avroParser = new AvroParser();
        final Schema mapSchema = Schema.createMap(Schema.create(Schema.Type.INT));
        final String mapEncoded = avroParser.serialize(Map.of("a", 1), new AvroSerConfig().setSchema(mapSchema));
        assertThrows(IllegalArgumentException.class,
                () -> avroParser.deserialize(mapEncoded, AvroDeserConfig.create().setSchema(mapSchema), AvroParserTest.TestBean.class));

        final Schema arraySchema = Schema.createArray(Schema.create(Schema.Type.STRING));
        final String arrayEncoded = avroParser.serialize(List.of("x", "y"), new AvroSerConfig().setSchema(arraySchema));
        assertThrows(IllegalArgumentException.class, () -> avroParser.deserialize(arrayEncoded, AvroDeserConfig.create().setSchema(arraySchema), Map.class));
    }

    @Test
    public void testGenericRecordRequiresSchemaInConfigForSerializeAndDeserialize() {
        final AvroParser avroParser = new AvroParser();
        final Schema recordSchema = new Schema.Parser()
                .parse("{\"type\":\"record\",\"name\":\"SchemaRequired\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}");
        final GenericRecord record = new GenericData.Record(recordSchema);
        record.put("name", "x");

        assertThrows(IllegalArgumentException.class, () -> avroParser.serialize(record, null, new ByteArrayOutputStream()));

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        avroParser.serialize(record, new AvroSerConfig().setSchema(recordSchema), os);
        assertThrows(IllegalArgumentException.class, () -> avroParser.deserialize(new ByteArrayInputStream(os.toByteArray()), null, GenericRecord.class));

        final GenericRecord read = avroParser.deserialize(new ByteArrayInputStream(os.toByteArray()), AvroDeserConfig.create().setSchema(recordSchema),
                GenericRecord.class);
        assertEquals("x", read.get("name").toString());
    }

    // ---- deep review 2026-09-25 G006 begin ----
    public static class G006BytesBean {
        private String name;
        private byte[] data;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public byte[] getData() {
            return data;
        }

        public void setData(byte[] data) {
            this.data = data;
        }
    }

    private static Schema g006BytesSchema() {
        return new Schema.Parser().parse(
                "{\"type\":\"record\",\"name\":\"G006Bytes\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"data\",\"type\":\"bytes\"}]}");
    }

    // G006-01: a byte[] bean property failed the whole write with a ClassCastException for a bytes field.
    @Test
    public void testSerialize_byteArrayPropertyToBytesField() {
        final Schema schema = g006BytesSchema();
        final G006BytesBean bean = new G006BytesBean();
        bean.setName("n");
        bean.setData(new byte[] { 1, 2, 3 });

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        parser.serialize(bean, new AvroSerConfig().setSchema(schema), os);

        final GenericRecord read = parser.deserialize(new ByteArrayInputStream(os.toByteArray()), AvroDeserConfig.create().setSchema(schema),
                GenericRecord.class);
        final java.nio.ByteBuffer buffer = (java.nio.ByteBuffer) read.get("data");
        final byte[] bytes = new byte[buffer.remaining()];
        buffer.get(bytes);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { 1, 2, 3 }, bytes);
    }

    // G006-01: a bytes datum was converted through ByteBuffer.toString() and silently set the byte[] property to null.
    @Test
    public void testDeserialize_bytesFieldIntoByteArrayProperty() {
        final Schema schema = g006BytesSchema();
        final Map<String, Object> source = new HashMap<>();
        source.put("name", "n");
        source.put("data", java.nio.ByteBuffer.wrap(new byte[] { 4, 5, 6 }));

        final String encoded = parser.serialize(source, new AvroSerConfig().setSchema(schema));
        final G006BytesBean bean = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(schema), G006BytesBean.class);

        assertEquals("n", bean.getName());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { 4, 5, 6 }, bean.getData());
    }

    private static Schema g006FixedSchema() {
        return new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"G006Fixed\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},"
                + "{\"name\":\"data\",\"type\":{\"type\":\"fixed\",\"name\":\"G006Two\",\"size\":2}}]}");
    }

    // G006-04: a byte[] bean property failed the write with a ClassCastException for a fixed field.
    @Test
    public void testSerialize_byteArrayPropertyToFixedField() {
        final Schema schema = g006FixedSchema();
        final G006BytesBean bean = new G006BytesBean();
        bean.setName("n");
        bean.setData(new byte[] { 7, 8 });

        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        parser.serialize(bean, new AvroSerConfig().setSchema(schema), os);

        final GenericRecord read = parser.deserialize(new ByteArrayInputStream(os.toByteArray()), AvroDeserConfig.create().setSchema(schema),
                GenericRecord.class);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { 7, 8 }, ((org.apache.avro.generic.GenericFixed) read.get("data")).bytes());
    }

    // G006-04: a byte[] whose length differs from the fixed size is an argument error, not an Avro write failure.
    @Test
    public void testSerialize_byteArrayOfWrongLengthToFixedFieldThrowsIAE() {
        final G006BytesBean bean = new G006BytesBean();
        bean.setName("n");
        bean.setData(new byte[] { 1, 2, 3 });

        assertThrows(IllegalArgumentException.class,
                () -> parser.serialize(bean, new AvroSerConfig().setSchema(g006FixedSchema()), new ByteArrayOutputStream()));
    }

    // G006-04: a fixed datum read into a byte[] property failed: Type.valueOf(Object) treated GenericData.Fixed as a bean.
    @Test
    public void testDeserialize_fixedFieldIntoByteArrayProperty() {
        final Schema schema = g006FixedSchema();
        final Map<String, Object> source = new HashMap<>();
        source.put("name", "n");
        source.put("data", new GenericData.Fixed(schema.getField("data").schema(), new byte[] { 5, 6 }));

        final String encoded = parser.serialize(source, new AvroSerConfig().setSchema(schema));
        final G006BytesBean bean = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(schema), G006BytesBean.class);

        assertEquals("n", bean.getName());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { 5, 6 }, bean.getData());
    }
    // ---- deep review 2026-09-25 G006 end ----

    // ---- deep review 2026-10-16 G128 begin ----
    public static class G128ArrayBean {
        private String name;
        private String[] tags;
        private int[] nums;
        private String[][] grid;
        private List<int[]> rows;
        private G128Item[] items;
        private Map<String, long[]> byKey;
        private List<String> plain;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String[] getTags() {
            return tags;
        }

        public void setTags(String[] tags) {
            this.tags = tags;
        }

        public int[] getNums() {
            return nums;
        }

        public void setNums(int[] nums) {
            this.nums = nums;
        }

        public String[][] getGrid() {
            return grid;
        }

        public void setGrid(String[][] grid) {
            this.grid = grid;
        }

        public List<int[]> getRows() {
            return rows;
        }

        public void setRows(List<int[]> rows) {
            this.rows = rows;
        }

        public G128Item[] getItems() {
            return items;
        }

        public void setItems(G128Item[] items) {
            this.items = items;
        }

        public Map<String, long[]> getByKey() {
            return byKey;
        }

        public void setByKey(Map<String, long[]> byKey) {
            this.byKey = byKey;
        }

        public List<String> getPlain() {
            return plain;
        }

        public void setPlain(List<String> plain) {
            this.plain = plain;
        }
    }

    public static class G128Item {
        private int x;

        public int getX() {
            return x;
        }

        public void setX(int x) {
            this.x = x;
        }
    }

    private static Schema g128ArraySchema() {
        return new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"G128Arrays\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},"
                + "{\"name\":\"tags\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"string\"}]},"
                + "{\"name\":\"nums\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"int\"}]},"
                + "{\"name\":\"grid\",\"type\":[\"null\",{\"type\":\"array\",\"items\":{\"type\":\"array\",\"items\":\"string\"}}]},"
                + "{\"name\":\"rows\",\"type\":[\"null\",{\"type\":\"array\",\"items\":{\"type\":\"array\",\"items\":\"int\"}}]},"
                + "{\"name\":\"items\",\"type\":[\"null\",{\"type\":\"array\",\"items\":{\"type\":\"record\",\"name\":\"G128Item\","
                + "\"fields\":[{\"name\":\"x\",\"type\":\"int\"}]}}]},"
                + "{\"name\":\"byKey\",\"type\":[\"null\",{\"type\":\"map\",\"values\":{\"type\":\"array\",\"items\":\"long\"}}]},"
                + "{\"name\":\"plain\",\"type\":[\"null\",{\"type\":\"array\",\"items\":\"string\"}]}]}");
    }

    private static Schema g128ValueSchema(final String fieldSchema) {
        return new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"G128Value\",\"fields\":[{\"name\":\"v\",\"type\":" + fieldSchema + "}]}");
    }

    private static G128ArrayBean g128FilledBean() {
        final G128Item item = new G128Item();
        item.setX(7);
        final Map<String, long[]> byKey = new java.util.LinkedHashMap<>();
        byKey.put("k", new long[] { 8L, 5000000000L });

        final G128ArrayBean bean = new G128ArrayBean();
        bean.setName("n");
        bean.setTags(new String[] { "a", "b" });
        bean.setNums(new int[] { 1, 2 });
        bean.setGrid(new String[][] { { "g" }, {} });
        bean.setRows(List.of(new int[] { 3 }, new int[] { 4, 5 }));
        bean.setItems(new G128Item[] { item });
        bean.setByKey(byKey);
        bean.setPlain(List.of("p"));
        return bean;
    }

    // G128 (G008-02): object/primitive array bean properties failed the write with DataFileWriter$AppendWriteException (ClassCastException).
    @Test
    public void testSerialize_arrayBeanPropertiesToArrayFields() {
        final Schema schema = g128ArraySchema();
        final String encoded = parser.serialize(g128FilledBean(), new AvroSerConfig().setSchema(schema));

        final GenericRecord read = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(schema), GenericRecord.class);
        assertEquals("[a, b]", read.get("tags").toString());
        assertEquals("[1, 2]", read.get("nums").toString());
        assertEquals("[[g], []]", read.get("grid").toString());
        assertEquals("[[3], [4, 5]]", read.get("rows").toString());
        assertEquals("[{\"x\": 7}]", read.get("items").toString());
        assertEquals("{k=[8, 5000000000]}", read.get("byKey").toString());
        assertEquals("[p]", read.get("plain").toString());
    }

    // G128 (G008-02): an array<string> datum read into a String[] property failed with ParsingException "Unsupported class: Utf8".
    @Test
    public void testDeserialize_arrayFieldsIntoArrayBeanProperties() {
        final Schema schema = g128ArraySchema();
        final Map<String, Object> source = new HashMap<>();
        source.put("name", "n");
        source.put("tags", List.of("a", "b"));
        source.put("nums", List.of(1, 2));
        source.put("grid", List.of(List.of("g"), List.of()));
        source.put("rows", List.of(List.of(3), List.of(4, 5)));
        source.put("items", List.of(Map.of("x", 7)));
        source.put("byKey", Map.of("k", List.of(8L)));
        source.put("plain", List.of("p"));
        final String encoded = parser.serialize(source, new AvroSerConfig().setSchema(schema));

        final G128ArrayBean bean = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(schema), G128ArrayBean.class);

        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[] { "a", "b" }, bean.getTags());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 1, 2 }, bean.getNums());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[][] { { "g" }, {} }, bean.getGrid());
        assertEquals(2, bean.getRows().size());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 3 }, bean.getRows().get(0));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 4, 5 }, bean.getRows().get(1));
        assertEquals(1, bean.getItems().length);
        assertEquals(7, bean.getItems()[0].getX());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new long[] { 8L }, bean.getByKey().get("k"));
        assertEquals(List.of("p"), bean.getPlain());
    }

    // G128 (G008-02): a bean with array properties now round-trips.
    @Test
    public void testSerialize_arrayBeanRoundTrip() {
        final Schema schema = g128ArraySchema();
        final String encoded = parser.serialize(g128FilledBean(), new AvroSerConfig().setSchema(schema));
        final G128ArrayBean bean = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(schema), G128ArrayBean.class);

        assertEquals("n", bean.getName());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[] { "a", "b" }, bean.getTags());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 1, 2 }, bean.getNums());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[][] { { "g" }, {} }, bean.getGrid());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 4, 5 }, bean.getRows().get(1));
        assertEquals(7, bean.getItems()[0].getX());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new long[] { 8L, 5000000000L }, bean.getByKey().get("k"));
    }

    // G128 (G008-02): array map values (object, primitive and byte[] for an array<int> schema) were passed to Avro unconverted.
    @Test
    public void testSerialize_arrayMapValuesToArrayFields() {
        final Schema schema = g128ValueSchema("{\"type\":\"array\",\"items\":\"int\"}");

        for (final Object value : new Object[] { new int[] { 1, 2 }, new Integer[] { 1, 2 }, new long[] { 1L, 2L }, new short[] { 1, 2 },
                new byte[] { 1, 2 } }) {
            final Map<String, Object> source = new HashMap<>();
            source.put("v", value);
            final String encoded = parser.serialize(source, new AvroSerConfig().setSchema(schema));
            final GenericRecord read = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(schema), GenericRecord.class);
            assertEquals("[1, 2]", read.get("v").toString(), value.getClass().getSimpleName());
        }

        final Map<String, Object> enums = new HashMap<>();
        enums.put("v", new String[] { "GREEN" });
        final Schema enumSchema = g128ValueSchema("{\"type\":\"array\",\"items\":{\"type\":\"enum\",\"name\":\"G128Color\",\"symbols\":[\"RED\",\"GREEN\"]}}");
        final GenericRecord read = parser.deserialize(parser.serialize(enums, new AvroSerConfig().setSchema(enumSchema)),
                AvroDeserConfig.create().setSchema(enumSchema), GenericRecord.class);
        assertEquals("[GREEN]", read.get("v").toString());
    }

    // G128 (G008-02): array items get the same range checks as collection items instead of a ClassCastException from Avro.
    @Test
    public void testSerialize_arrayItemOverflowThrowsArithmeticException() {
        final Schema schema = g128ValueSchema("{\"type\":\"array\",\"items\":\"int\"}");
        final Map<String, Object> source = new HashMap<>();
        source.put("v", new long[] { 5000000000L });

        assertThrows(ArithmeticException.class, () -> parser.serialize(source, new AvroSerConfig().setSchema(schema)));

        source.put("v", List.of(5000000000L));
        assertThrows(ArithmeticException.class, () -> parser.serialize(source, new AvroSerConfig().setSchema(schema)));
    }

    // G128 (G008-02): Map targets with array value types and collection targets with array element types read item by item.
    @Test
    public void testDeserialize_arrayDatumIntoArrayMapValueAndCollectionElement() {
        final Schema schema = g128ValueSchema("{\"type\":\"array\",\"items\":{\"type\":\"array\",\"items\":\"string\"}}");
        final Map<String, Object> source = new HashMap<>();
        source.put("v", List.of(List.of("a"), List.of("b", "c")));
        final String encoded = parser.serialize(source, new AvroSerConfig().setSchema(schema));

        final Map<String, String[][]> map = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(schema), Type.ofMap(String.class, String[][].class));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[][] { { "a" }, { "b", "c" } }, map.get("v"));

        final Map<String, List<String[]>> listMap = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(schema),
                Type.of("Map<String, List<String[]>>"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[] { "b", "c" }, listMap.get("v").get(1));

        final Schema arraySchemaOfArrays = new Schema.Parser().parse("{\"type\":\"array\",\"items\":{\"type\":\"array\",\"items\":\"string\"}}");
        final String top = parser.serialize(List.of(List.of("x", "y")), new AvroSerConfig().setSchema(arraySchemaOfArrays));
        final List<String[]> list = parser.deserialize(top, AvroDeserConfig.create().setSchema(arraySchemaOfArrays).setElementType(String[].class), List.class);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[] { "x", "y" }, list.get(0));
    }

    // G128 (G008-02): an array<bytes> datum read into byte[][] silently produced null items.
    @Test
    public void testDeserialize_bytesArrayIntoByteArrayArray() {
        final Schema schema = g128ValueSchema("{\"type\":\"array\",\"items\":\"bytes\"}");
        final Map<String, Object> source = new HashMap<>();
        source.put("v", new byte[][] { { 1 }, { 2, 3 } });
        final String encoded = parser.serialize(source, new AvroSerConfig().setSchema(schema));

        final Map<String, byte[][]> map = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(schema), Type.ofMap(String.class, byte[][].class));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[][] { { 1 }, { 2, 3 } }, map.get("v"));
    }

    // G128 neighbour: numeric array datums were already readable into primitive arrays; a null item keeps the primitive default
    // and integral overflow still fails - unchanged.
    @Test
    public void testDeserialize_numericArrayIntoPrimitiveArrayUnchanged() {
        final Schema schema = g128ValueSchema("{\"type\":\"array\",\"items\":[\"null\",\"long\"]}");
        final Map<String, Object> source = new HashMap<>();
        source.put("v", Arrays.asList(1L, null, 3L));
        final String encoded = parser.serialize(source, new AvroSerConfig().setSchema(schema));

        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 1, 0, 3 },
                parser.<Map<String, int[]>> deserialize(encoded, AvroDeserConfig.create().setSchema(schema), Type.ofMap(String.class, int[].class)).get("v"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new Long[] { 1L, null, 3L },
                parser.<Map<String, Long[]>> deserialize(encoded, AvroDeserConfig.create().setSchema(schema), Type.ofMap(String.class, Long[].class)).get("v"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[] { 1.0, 0.0, 3.0 },
                parser.<Map<String, double[]>> deserialize(encoded, AvroDeserConfig.create().setSchema(schema), Type.ofMap(String.class, double[].class))
                        .get("v"));

        source.put("v", List.of(5000000000L));
        final String big = parser.serialize(source, new AvroSerConfig().setSchema(schema));
        assertThrows(ArithmeticException.class,
                () -> parser.deserialize(big, AvroDeserConfig.create().setSchema(schema), Type.ofMap(String.class, int[].class)));
    }

    // G128 neighbour: collections, untyped values, byte[] for a bytes field and the root-level array rejection are unchanged.
    @Test
    public void testArrayNeighboursUnchanged() {
        final Schema schema = g128ValueSchema("{\"type\":\"array\",\"items\":\"string\"}");
        final Map<String, Object> source = new HashMap<>();
        source.put("v", List.of("a", "b"));
        final String encoded = parser.serialize(source, new AvroSerConfig().setSchema(schema));

        assertEquals(List.of("a", "b"),
                parser.<Map<String, List<String>>> deserialize(encoded, AvroDeserConfig.create().setSchema(schema), Type.of("Map<String, List<String>>"))
                        .get("v"));
        final Object untyped = parser.<Map<String, Object>> deserialize(encoded, AvroDeserConfig.create().setSchema(schema), Type.of("Map<String, Object>"))
                .get("v");
        assertTrue(untyped instanceof GenericData.Array);

        final G006BytesBean bytesBean = new G006BytesBean();
        bytesBean.setName("n");
        bytesBean.setData(new byte[] { 1, 2 });
        final GenericRecord bytesRead = parser.deserialize(parser.serialize(bytesBean, new AvroSerConfig().setSchema(g006BytesSchema())),
                AvroDeserConfig.create().setSchema(g006BytesSchema()), GenericRecord.class);
        assertTrue(bytesRead.get("data") instanceof java.nio.ByteBuffer);

        assertThrows(IllegalArgumentException.class, () -> parser.serialize(new String[] { "a" }, new AvroSerConfig().setSchema(arraySchema)));
        final String top = parser.serialize(List.of("a"), new AvroSerConfig().setSchema(arraySchema));
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize(top, AvroDeserConfig.create().setSchema(arraySchema), String[].class));
    }
    // ---- deep review 2026-10-16 G128 end ----

    // ---- deep review 2026-10-16 verify G133 begin ----
    public enum G133Color {
        RED, GREEN
    }

    public static class G133ArrayKinds {
        private long[] longs;
        private double[] doubles;
        private boolean[] flags;
        private Integer[] boxed;
        private Object[] objects;
        private G133Color[] colors;
        private G128Item[] items;
        private byte[][] blobs;
        private float[] floats;
        private char[] chars;

        public long[] getLongs() {
            return longs;
        }

        public void setLongs(long[] longs) {
            this.longs = longs;
        }

        public double[] getDoubles() {
            return doubles;
        }

        public void setDoubles(double[] doubles) {
            this.doubles = doubles;
        }

        public boolean[] getFlags() {
            return flags;
        }

        public void setFlags(boolean[] flags) {
            this.flags = flags;
        }

        public Integer[] getBoxed() {
            return boxed;
        }

        public void setBoxed(Integer[] boxed) {
            this.boxed = boxed;
        }

        public Object[] getObjects() {
            return objects;
        }

        public void setObjects(Object[] objects) {
            this.objects = objects;
        }

        public G133Color[] getColors() {
            return colors;
        }

        public void setColors(G133Color[] colors) {
            this.colors = colors;
        }

        public G128Item[] getItems() {
            return items;
        }

        public void setItems(G128Item[] items) {
            this.items = items;
        }

        public byte[][] getBlobs() {
            return blobs;
        }

        public void setBlobs(byte[][] blobs) {
            this.blobs = blobs;
        }

        public float[] getFloats() {
            return floats;
        }

        public void setFloats(float[] floats) {
            this.floats = floats;
        }

        public char[] getChars() {
            return chars;
        }

        public void setChars(char[] chars) {
            this.chars = chars;
        }
    }

    public static class G133RoundTripBean {
        private String[] strings;
        private int[] ints;
        private long[] longs;
        private double[] doubles;
        private boolean[] flags;
        private Integer[] boxed;
        private String[][] grid;

        public String[] getStrings() {
            return strings;
        }

        public void setStrings(String[] strings) {
            this.strings = strings;
        }

        public int[] getInts() {
            return ints;
        }

        public void setInts(int[] ints) {
            this.ints = ints;
        }

        public long[] getLongs() {
            return longs;
        }

        public void setLongs(long[] longs) {
            this.longs = longs;
        }

        public double[] getDoubles() {
            return doubles;
        }

        public void setDoubles(double[] doubles) {
            this.doubles = doubles;
        }

        public boolean[] getFlags() {
            return flags;
        }

        public void setFlags(boolean[] flags) {
            this.flags = flags;
        }

        public Integer[] getBoxed() {
            return boxed;
        }

        public void setBoxed(Integer[] boxed) {
            this.boxed = boxed;
        }

        public String[][] getGrid() {
            return grid;
        }

        public void setGrid(String[][] grid) {
            this.grid = grid;
        }
    }

    private static String g133NullableArray(final String items) {
        return "[\"null\",{\"type\":\"array\",\"items\":" + items + "}]";
    }

    private static Schema g133ArrayKindsSchema() {
        return new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"G133ArrayKinds\",\"fields\":[" + "{\"name\":\"longs\",\"type\":"
                + g133NullableArray("\"long\"") + "}," + "{\"name\":\"doubles\",\"type\":" + g133NullableArray("\"double\"") + "},"
                + "{\"name\":\"flags\",\"type\":" + g133NullableArray("\"boolean\"") + "}," + "{\"name\":\"boxed\",\"type\":"
                + g133NullableArray("[\"null\",\"int\"]") + "}," + "{\"name\":\"objects\",\"type\":" + g133NullableArray("\"string\"") + "},"
                + "{\"name\":\"colors\",\"type\":" + g133NullableArray("{\"type\":\"enum\",\"name\":\"G133Color\",\"symbols\":[\"RED\",\"GREEN\"]}") + "},"
                + "{\"name\":\"items\",\"type\":"
                + g133NullableArray("{\"type\":\"record\",\"name\":\"G133Item\",\"fields\":[{\"name\":\"x\",\"type\":\"int\"}]}") + "},"
                + "{\"name\":\"blobs\",\"type\":" + g133NullableArray("\"bytes\"") + "}," + "{\"name\":\"floats\",\"type\":" + g133NullableArray("\"float\"")
                + "}," + "{\"name\":\"chars\",\"type\":" + g133NullableArray("\"string\"") + "}]}");
    }

    private static Schema g133RoundTripSchema() {
        return new Schema.Parser().parse(
                "{\"type\":\"record\",\"name\":\"G133RoundTrip\",\"fields\":[" + "{\"name\":\"strings\",\"type\":" + g133NullableArray("\"string\"") + "},"
                        + "{\"name\":\"ints\",\"type\":" + g133NullableArray("\"int\"") + "}," + "{\"name\":\"longs\",\"type\":" + g133NullableArray("\"long\"")
                        + "}," + "{\"name\":\"doubles\",\"type\":" + g133NullableArray("\"double\"") + "}," + "{\"name\":\"flags\",\"type\":"
                        + g133NullableArray("\"boolean\"") + "}," + "{\"name\":\"boxed\",\"type\":" + g133NullableArray("[\"null\",\"int\"]") + "},"
                        + "{\"name\":\"grid\",\"type\":" + g133NullableArray("{\"type\":\"array\",\"items\":\"string\"}") + "}]}");
    }

    private static byte[] g133WriteGeneric(final Schema schema, final GenericRecord record) throws IOException {
        final ByteArrayOutputStream os = new ByteArrayOutputStream();
        try (DataFileWriter<GenericRecord> writer = new DataFileWriter<>(new org.apache.avro.generic.GenericDatumWriter<GenericRecord>(schema))) {
            writer.create(schema, os);
            writer.append(record);
        }
        return os.toByteArray();
    }

    // G133 (G128 read side): Avro-written array datums are read into every kind of array bean property.
    @Test
    public void testDeserialize_genericArrayDatumsIntoAllArrayPropertyKinds() throws IOException {
        final Schema schema = g133ArrayKindsSchema();
        final GenericRecord record = new GenericData.Record(schema);
        record.put("longs", Arrays.asList(5000000000L, -1L));
        record.put("doubles", Arrays.asList(1.5, -0.25));
        record.put("flags", Arrays.asList(true, false));
        record.put("boxed", Arrays.asList(1, null));
        record.put("objects", Arrays.asList("p", "q"));
        final Schema colorSchema = schema.getField("colors").schema().getTypes().get(1).getElementType();
        record.put("colors", Arrays.asList(new GenericData.EnumSymbol(colorSchema, "GREEN"), new GenericData.EnumSymbol(colorSchema, "RED")));
        final GenericRecord item = new GenericData.Record(schema.getField("items").schema().getTypes().get(1).getElementType());
        item.put("x", 9);
        record.put("items", Arrays.asList(item));
        record.put("blobs", Arrays.asList(java.nio.ByteBuffer.wrap(new byte[] { 1, 2 }), java.nio.ByteBuffer.wrap(new byte[0])));
        record.put("floats", Arrays.asList(1.25f));
        record.put("chars", Arrays.asList("x", "y"));

        final G133ArrayKinds bean = parser.deserialize(new ByteArrayInputStream(g133WriteGeneric(schema, record)), AvroDeserConfig.create().setSchema(schema),
                G133ArrayKinds.class);

        org.junit.jupiter.api.Assertions.assertArrayEquals(new long[] { 5000000000L, -1L }, bean.getLongs());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[] { 1.5, -0.25 }, bean.getDoubles());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new boolean[] { true, false }, bean.getFlags());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new Integer[] { 1, null }, bean.getBoxed());
        // An Object component keeps the Avro item representation (Utf8), exactly like a List<Object> element.
        assertEquals(2, bean.getObjects().length);
        assertTrue(bean.getObjects()[0] instanceof CharSequence);
        assertEquals("p", bean.getObjects()[0].toString());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new G133Color[] { G133Color.GREEN, G133Color.RED }, bean.getColors());
        assertEquals(1, bean.getItems().length);
        assertEquals(9, bean.getItems()[0].getX());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[][] { { 1, 2 }, {} }, bean.getBlobs());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[] { 1.25f }, bean.getFloats());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new char[] { 'x', 'y' }, bean.getChars());
    }

    // G133 (G128 read side): reader-schema array defaults (schema evolution) are read into array properties and array map values.
    @Test
    public void testDeserialize_arrayFieldDefaultsFromSchemaEvolution() throws IOException {
        final Schema writerSchema = new Schema.Parser()
                .parse("{\"type\":\"record\",\"name\":\"G133Evol\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}");
        final Schema readerSchema = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"G133Evol\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},"
                + "{\"name\":\"tags\",\"type\":{\"type\":\"array\",\"items\":\"string\"},\"default\":[\"x\",\"y\"]},"
                + "{\"name\":\"nums\",\"type\":{\"type\":\"array\",\"items\":\"int\"},\"default\":[3,4]}]}");
        final GenericRecord record = new GenericData.Record(writerSchema);
        record.put("name", "n");
        final byte[] bytes = g133WriteGeneric(writerSchema, record);

        final G128ArrayBean bean = parser.deserialize(new ByteArrayInputStream(bytes), AvroDeserConfig.create().setSchema(readerSchema), G128ArrayBean.class);
        assertEquals("n", bean.getName());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[] { "x", "y" }, bean.getTags());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 3, 4 }, bean.getNums());

        final Map<String, String[]> map = parser.deserialize(new ByteArrayInputStream(bytes), AvroDeserConfig.create().setSchema(readerSchema),
                Type.of("Map<String, String[]>"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[] { "x", "y" }, map.get("tags"));
    }

    // G133 (G128 write side): null arrays under a nullable union stay null and empty arrays of every kind round-trip as empty.
    @Test
    public void testSerialize_nullAndEmptyArrayProperties() {
        final Schema schema = g133RoundTripSchema();
        final G133RoundTripBean nulls = parser.deserialize(parser.serialize(new G133RoundTripBean(), new AvroSerConfig().setSchema(schema)),
                AvroDeserConfig.create().setSchema(schema), G133RoundTripBean.class);
        assertEquals(null, nulls.getStrings());
        assertEquals(null, nulls.getInts());
        assertEquals(null, nulls.getGrid());

        final G133RoundTripBean empty = new G133RoundTripBean();
        empty.setStrings(new String[0]);
        empty.setInts(new int[0]);
        empty.setLongs(new long[0]);
        empty.setDoubles(new double[0]);
        empty.setFlags(new boolean[0]);
        empty.setBoxed(new Integer[0]);
        empty.setGrid(new String[][] { {} });
        final G133RoundTripBean read = parser.deserialize(parser.serialize(empty, new AvroSerConfig().setSchema(schema)),
                AvroDeserConfig.create().setSchema(schema), G133RoundTripBean.class);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[0], read.getStrings());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[0], read.getInts());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new long[0], read.getLongs());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[0], read.getDoubles());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new boolean[0], read.getFlags());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new Integer[0], read.getBoxed());
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[][] { {} }, read.getGrid());
    }

    // G133 (G128): property test - random arrays (incl. empty, null items, extreme values) written from a bean read back equal.
    @Test
    public void testSerialize_randomArrayBeanRoundTripProperty() {
        final Schema schema = g133RoundTripSchema();
        final java.util.Random random = new java.util.Random(133L);

        for (int round = 0; round < 200; round++) {
            final int length = random.nextInt(6);
            final G133RoundTripBean bean = new G133RoundTripBean();
            final String[] strings = new String[length];
            final int[] ints = new int[length];
            final long[] longs = new long[length];
            final double[] doubles = new double[length];
            final boolean[] flags = new boolean[length];
            final Integer[] boxed = new Integer[length];
            final String[][] grid = new String[length][];

            for (int i = 0; i < length; i++) {
                strings[i] = Integer.toString(random.nextInt(), 36);
                ints[i] = random.nextBoolean() ? random.nextInt() : (random.nextBoolean() ? Integer.MIN_VALUE : Integer.MAX_VALUE);
                longs[i] = random.nextBoolean() ? random.nextLong() : Long.MIN_VALUE;
                doubles[i] = random.nextBoolean() ? random.nextGaussian() * 1e300 : -0.0;
                flags[i] = random.nextBoolean();
                boxed[i] = random.nextInt(3) == 0 ? null : random.nextInt();
                grid[i] = new String[random.nextInt(3)];
                for (int j = 0; j < grid[i].length; j++) {
                    grid[i][j] = "g" + random.nextInt(100);
                }
            }

            bean.setStrings(strings);
            bean.setInts(ints);
            bean.setLongs(longs);
            bean.setDoubles(doubles);
            bean.setFlags(flags);
            bean.setBoxed(boxed);
            bean.setGrid(grid);

            final G133RoundTripBean read = parser.deserialize(parser.serialize(bean, new AvroSerConfig().setSchema(schema)),
                    AvroDeserConfig.create().setSchema(schema), G133RoundTripBean.class);
            final String message = "round " + round;
            org.junit.jupiter.api.Assertions.assertArrayEquals(strings, read.getStrings(), message);
            org.junit.jupiter.api.Assertions.assertArrayEquals(ints, read.getInts(), message);
            org.junit.jupiter.api.Assertions.assertArrayEquals(longs, read.getLongs(), message);
            org.junit.jupiter.api.Assertions.assertArrayEquals(doubles, read.getDoubles(), message);
            org.junit.jupiter.api.Assertions.assertArrayEquals(flags, read.getFlags(), message);
            org.junit.jupiter.api.Assertions.assertArrayEquals(boxed, read.getBoxed(), message);
            org.junit.jupiter.api.Assertions.assertArrayEquals(grid, read.getGrid(), message);
        }
    }

    // G133 (G128 write side): wrong array element types fail exactly like the equivalent List (Avro's ClassCastException in the append).
    @Test
    public void testSerialize_arrayWrongElementTypeFailsLikeList() {
        final Schema schema = g128ValueSchema("{\"type\":\"array\",\"items\":\"int\"}");
        final Map<String, Object> source = new HashMap<>();
        source.put("v", new String[] { "x" });
        final RuntimeException fromArray = assertThrows(RuntimeException.class, () -> parser.serialize(source, new AvroSerConfig().setSchema(schema)));

        source.put("v", List.of("x"));
        final RuntimeException fromList = assertThrows(RuntimeException.class, () -> parser.serialize(source, new AvroSerConfig().setSchema(schema)));

        assertEquals(fromList.getClass(), fromArray.getClass());
        assertTrue(fromList.getCause() instanceof ClassCastException);
        assertTrue(fromArray.getCause() instanceof ClassCastException);
        // The array is now unpacked, so Avro reports the offending ITEM, as it does for the List.
        assertTrue(fromArray.getCause().getMessage().contains("java.lang.String"), fromArray.getCause().getMessage());
        assertFalse(fromArray.getCause().getMessage().contains("[Ljava.lang.String;"), fromArray.getCause().getMessage());
    }

    // G133-01: a byte[]-typed SCALAR target (Bytes/Base64Encoded) keeps the pre-existing Base64 conversion of an array datum
    // instead of failing inside the element-wise array conversion ("argument type mismatch").
    @Test
    public void testDeserialize_arrayDatumIntoBase64ScalarTypeUnchanged() {
        final Schema schema = g128ValueSchema("{\"type\":\"array\",\"items\":\"int\"}");
        final Map<String, Object> source = new HashMap<>();
        source.put("v", List.of(1, 2));
        final String encoded = parser.serialize(source, new AvroSerConfig().setSchema(schema));

        for (final String typeName : new String[] { "Map<String, Base64Encoded>", "Map<String, Bytes>" }) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> parser.deserialize(encoded, AvroDeserConfig.create().setSchema(schema), Type.of(typeName)), typeName);
            assertTrue(String.valueOf(e.getMessage()).toLowerCase().contains("base64"), typeName + ": " + e.getMessage());
        }

        // byte[] itself is an array type: element-wise, as for a List<Byte>.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { 1, 2 },
                parser.<Map<String, byte[]>> deserialize(encoded, AvroDeserConfig.create().setSchema(schema), Type.ofMap(String.class, byte[].class)).get("v"));
    }

    // G133-02: an Avro bytes item read into a numeric array component fails (as before the array conversion existed) instead of
    // silently becoming 0/null; byte[][], ByteBuffer[], Object[] and List targets keep their results.
    @Test
    public void testDeserialize_bytesItemsIntoNumericArrayComponentThrow() {
        final Schema schema = g128ValueSchema("{\"type\":\"array\",\"items\":[\"null\",\"bytes\"]}");
        final Map<String, Object> source = new HashMap<>();
        source.put("v", Arrays.asList(new byte[] { 1, 2 }, null));
        final String encoded = parser.serialize(source, new AvroSerConfig().setSchema(schema));

        for (final Class<?> arrayClass : new Class<?>[] { byte[].class, Byte[].class, short[].class, int[].class, Integer[].class, long[].class, Long[].class,
                float[].class, double[].class, Double[].class }) {
            assertThrows(NumberFormatException.class,
                    () -> parser.deserialize(encoded, AvroDeserConfig.create().setSchema(schema), Type.ofMap(String.class, arrayClass)),
                    arrayClass.getSimpleName());
        }

        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[][] { { 1, 2 }, null },
                parser.<Map<String, byte[][]>> deserialize(encoded, AvroDeserConfig.create().setSchema(schema), Type.ofMap(String.class, byte[][].class))
                        .get("v"));
        final java.nio.ByteBuffer[] buffers = parser.<Map<String, java.nio.ByteBuffer[]>> deserialize(encoded, AvroDeserConfig.create().setSchema(schema),
                Type.ofMap(String.class, java.nio.ByteBuffer[].class)).get("v");
        assertEquals(java.nio.ByteBuffer.wrap(new byte[] { 1, 2 }), buffers[0]);
        assertEquals(null, buffers[1]);
        final Object[] objects = parser
                .<Map<String, Object[]>> deserialize(encoded, AvroDeserConfig.create().setSchema(schema), Type.ofMap(String.class, Object[].class))
                .get("v");
        assertEquals(java.nio.ByteBuffer.wrap(new byte[] { 1, 2 }), objects[0]);
        final List<byte[]> byteList = parser
                .<Map<String, List<byte[]>>> deserialize(encoded, AvroDeserConfig.create().setSchema(schema), Type.of("Map<String, List<byte[]>>"))
                .get("v");
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { 1, 2 }, byteList.get(0));
        // List targets are out of scope and unchanged: a bytes item still converts to null there.
        assertEquals(Arrays.asList(null, null),
                parser.<Map<String, List<Integer>>> deserialize(encoded, AvroDeserConfig.create().setSchema(schema), Type.of("Map<String, List<Integer>>"))
                        .get("v"));
    }
    // ---- deep review 2026-10-16 verify G133 end ----

    // ---- deep review 2026-10-16 G152 begin ----
    private Object g152RoundTripValue(final String fieldSchema, final Object value, final String targetTypeName) {
        final Schema schema = g128ValueSchema(fieldSchema);
        final Map<String, Object> source = new HashMap<>();
        source.put("v", value);
        final String encoded = parser.serialize(source, new AvroSerConfig().setSchema(schema));
        final Map<String, Object> read = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(schema),
                Type.of("Map<String, " + targetTypeName + ">"));
        return read.get("v");
    }

    private String g152WrittenDatum(final String fieldSchema, final Object value) {
        final Schema schema = g128ValueSchema(fieldSchema);
        final Map<String, Object> source = new HashMap<>();
        source.put("v", value);
        final String encoded = parser.serialize(source, new AvroSerConfig().setSchema(schema));
        return String.valueOf(parser.<GenericRecord> deserialize(encoded, AvroDeserConfig.create().setSchema(schema), GenericRecord.class).get("v"));
    }

    // G152 (G128 coverage): a byte[][] for an array<fixed> field writes each item as a fixed datum (with the size check)
    // and reads back as byte[][]; an Object[] component keeps the Avro fixed datum like a List<Object> element.
    @Test
    public void testSerialize_fixedItemArrayRoundTrip() {
        final String fixedItems = "{\"type\":\"array\",\"items\":{\"type\":\"fixed\",\"name\":\"G152Fixed\",\"size\":2}}";

        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[][] { { 1, 2 }, { 3, 4 } },
                (byte[][]) g152RoundTripValue(fixedItems, new byte[][] { { 1, 2 }, { 3, 4 } }, "byte[][]"));
        final Object[] objects = (Object[]) g152RoundTripValue(fixedItems, new byte[][] { { 1, 2 } }, "Object[]");
        assertTrue(objects[0] instanceof org.apache.avro.generic.GenericFixed);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { 1, 2 }, ((org.apache.avro.generic.GenericFixed) objects[0]).bytes());
        assertThrows(IllegalArgumentException.class, () -> g152WrittenDatum(fixedItems, new byte[][] { { 1 } }));
    }

    // G152 (G128 coverage): a Map[] for an array<map> field is written item by item and read into a typed Map[] component,
    // converting the map values to the component's value type.
    @Test
    public void testSerialize_mapItemArrayRoundTrip() {
        final String mapItems = "{\"type\":\"array\",\"items\":{\"type\":\"map\",\"values\":\"int\"}}";
        final Map<String, Integer> item = new java.util.LinkedHashMap<>();
        item.put("a", 1);

        assertEquals("[{a=1}]", g152WrittenDatum(mapItems, new Map[] { item }));
        final Map<?, ?>[] read = (Map<?, ?>[]) g152RoundTripValue(mapItems, new Map[] { item }, "Map<String, Long>[]");
        assertEquals(1, read.length);
        assertEquals(Long.valueOf(1L), read[0].get("a"));
    }

    // G152 (G128 coverage): a Java enum array for an array<enum> field is written as symbols and read back into the enum
    // array or a String[]; an undefined symbol still fails with IllegalArgumentException.
    @Test
    public void testSerialize_javaEnumArrayToEnumItems() {
        final String enumItems = "{\"type\":\"array\",\"items\":{\"type\":\"enum\",\"name\":\"G152Color\",\"symbols\":[\"RED\",\"GREEN\"]}}";

        assertEquals("[GREEN, RED]", g152WrittenDatum(enumItems, new G133Color[] { G133Color.GREEN, G133Color.RED }));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new G133Color[] { G133Color.GREEN },
                (G133Color[]) g152RoundTripValue(enumItems, new G133Color[] { G133Color.GREEN }, G133Color.class.getName() + "[]"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[] { "GREEN" },
                (String[]) g152RoundTripValue(enumItems, new G133Color[] { G133Color.GREEN }, "String[]"));
        assertThrows(IllegalArgumentException.class, () -> g152WrittenDatum(enumItems, new String[] { "BLUE" }));
    }

    // G152 (G128 coverage): null items of an object array are written for nullable items and read back as null; a null item
    // for non-nullable items fails exactly like the equivalent List (Avro's NullPointerException inside the append).
    @Test
    public void testSerialize_objectArrayWithNullItems() {
        final String nullableItems = "{\"type\":\"array\",\"items\":[\"null\",\"string\"]}";

        assertEquals("[a, null]", g152WrittenDatum(nullableItems, new String[] { "a", null }));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new String[] { "a", null },
                (String[]) g152RoundTripValue(nullableItems, new String[] { "a", null }, "String[]"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new char[] { 'a', '\0' },
                (char[]) g152RoundTripValue(nullableItems, new String[] { "a", null }, "char[]"));

        final String strictItems = "{\"type\":\"array\",\"items\":\"string\"}";
        final RuntimeException fromArray = assertThrows(RuntimeException.class, () -> g152WrittenDatum(strictItems, new String[] { "a", null }));
        final RuntimeException fromList = assertThrows(RuntimeException.class, () -> g152WrittenDatum(strictItems, Arrays.asList("a", null)));
        assertEquals(fromList.getClass(), fromArray.getClass());
        assertTrue(fromArray.getCause() instanceof NullPointerException, String.valueOf(fromArray.getCause()));
    }

    // G152 (G128 coverage): primitive arrays of the remaining primitive types round-trip (NaN / -0.0 / MIN kept); a char[]
    // fails for a string schema exactly like a List<Character>; integral overflow on read still throws ArithmeticException.
    @Test
    public void testSerialize_primitiveArraysOfEveryTypeRoundTrip() {
        org.junit.jupiter.api.Assertions.assertArrayEquals(new short[] { 1, -2, Short.MIN_VALUE },
                (short[]) g152RoundTripValue("{\"type\":\"array\",\"items\":\"int\"}", new short[] { 1, -2, Short.MIN_VALUE }, "short[]"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { 1, -2, Byte.MAX_VALUE },
                (byte[]) g152RoundTripValue("{\"type\":\"array\",\"items\":\"int\"}", new byte[] { 1, -2, Byte.MAX_VALUE }, "byte[]"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new long[] { Long.MIN_VALUE, Long.MAX_VALUE },
                (long[]) g152RoundTripValue("{\"type\":\"array\",\"items\":\"long\"}", new long[] { Long.MIN_VALUE, Long.MAX_VALUE }, "long[]"));
        final float[] floats = (float[]) g152RoundTripValue("{\"type\":\"array\",\"items\":\"float\"}", new float[] { Float.NaN, -0.0f, 1.5f }, "float[]");
        assertTrue(Float.isNaN(floats[0]));
        assertEquals(Float.floatToIntBits(-0.0f), Float.floatToIntBits(floats[1]));
        assertEquals(1.5f, floats[2]);
        org.junit.jupiter.api.Assertions.assertArrayEquals(new double[] { 1.5 },
                (double[]) g152RoundTripValue("{\"type\":\"array\",\"items\":\"float\"}", new float[] { 1.5f }, "double[]"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new boolean[] { true, false },
                (boolean[]) g152RoundTripValue("{\"type\":\"array\",\"items\":\"boolean\"}", new boolean[] { true, false }, "boolean[]"));

        final RuntimeException fromChars = assertThrows(RuntimeException.class,
                () -> g152WrittenDatum("{\"type\":\"array\",\"items\":\"string\"}", new char[] { 'a' }));
        final RuntimeException fromList = assertThrows(RuntimeException.class,
                () -> g152WrittenDatum("{\"type\":\"array\",\"items\":\"string\"}", List.of('a')));
        assertEquals(fromList.getClass(), fromChars.getClass());

        assertThrows(ArithmeticException.class, () -> g152RoundTripValue("{\"type\":\"array\",\"items\":\"int\"}", new int[] { 70000 }, "short[]"));
        assertThrows(ArithmeticException.class, () -> g152RoundTripValue("{\"type\":\"array\",\"items\":\"int\"}", new int[] { 300 }, "byte[]"));
    }

    // G152 (G128 coverage): nested primitive arrays with a null inner array under nullable items round-trip as int[][]
    // (the null inner array stays null) and as List<int[]>.
    @Test
    public void testSerialize_nestedPrimitiveArrayWithNullInnerArray() {
        final String nestedItems = "{\"type\":\"array\",\"items\":[\"null\",{\"type\":\"array\",\"items\":\"int\"}]}";

        assertEquals("[[1, 2], null]", g152WrittenDatum(nestedItems, new int[][] { { 1, 2 }, null }));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[][] { { 1, 2 }, null },
                (int[][]) g152RoundTripValue(nestedItems, new int[][] { { 1, 2 }, null }, "int[][]"));
        final List<?> list = (List<?>) g152RoundTripValue(nestedItems, new int[][] { { 1, 2 }, null }, "List<int[]>");
        org.junit.jupiter.api.Assertions.assertArrayEquals(new int[] { 1, 2 }, (int[]) list.get(0));
        assertEquals(null, list.get(1));
    }

    // G152 (owner decision): a null item read into a primitive array of every primitive type keeps the primitive default,
    // and a wrapper array keeps the null. The numeric/boolean rows read the same before the array conversion existed;
    // the char[] row from string items failed before (ParsingException "Unsupported class: Utf8").
    @Test
    public void testDeserialize_nullItemsIntoEveryPrimitiveArrayKeepDefault() {
        final String nullableInts = "{\"type\":\"array\",\"items\":[\"null\",\"int\"]}";
        final List<Integer> withNull = Arrays.asList(1, null, 3);

        org.junit.jupiter.api.Assertions.assertArrayEquals(new byte[] { 1, 0, 3 }, (byte[]) g152RoundTripValue(nullableInts, withNull, "byte[]"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new short[] { 1, 0, 3 }, (short[]) g152RoundTripValue(nullableInts, withNull, "short[]"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new long[] { 1, 0, 3 }, (long[]) g152RoundTripValue(nullableInts, withNull, "long[]"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new float[] { 1, 0, 3 }, (float[]) g152RoundTripValue(nullableInts, withNull, "float[]"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new Short[] { 1, null, 3 }, (Short[]) g152RoundTripValue(nullableInts, withNull, "Short[]"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new Float[] { 1f, null, 3f }, (Float[]) g152RoundTripValue(nullableInts, withNull, "Float[]"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new boolean[] { true, false },
                (boolean[]) g152RoundTripValue("{\"type\":\"array\",\"items\":[\"null\",\"boolean\"]}", Arrays.asList(true, null), "boolean[]"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new Boolean[] { true, null },
                (Boolean[]) g152RoundTripValue("{\"type\":\"array\",\"items\":[\"null\",\"boolean\"]}", Arrays.asList(true, null), "Boolean[]"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new char[] { 'x', '\0' },
                (char[]) g152RoundTripValue("{\"type\":\"array\",\"items\":[\"null\",\"string\"]}", Arrays.asList("x", null), "char[]"));
    }
    // ---- deep review 2026-10-16 G152 end ----

    // ---- deep review 2026-10-16 G159 begin ----

    // G159 (owner: keep the stricter behaviour): an Avro array item is converted like a List element or a single field, not
    // re-parsed from the array's text form as before the array conversion existed. r89 read array<long> [1, 0] into boolean[]
    // as [true, false] and array<double> [1.5] into BigInteger[] as [1]; both now fail exactly like the List and scalar targets.
    @Test
    public void testDeserialize_arrayItemCoercionMatchesListElementAndScalarField() {
        final String longs = "{\"type\":\"array\",\"items\":\"long\"}";
        final String doubles = "{\"type\":\"array\",\"items\":\"double\"}";
        final String floats = "{\"type\":\"array\",\"items\":\"float\"}";

        assertThrows(IllegalArgumentException.class, () -> g152RoundTripValue(longs, List.of(1L, 0L), "boolean[]"));
        assertThrows(IllegalArgumentException.class, () -> g152RoundTripValue(longs, List.of(1L, 0L), "Boolean[]"));
        assertThrows(IllegalArgumentException.class, () -> g152RoundTripValue(longs, List.of(1L, 0L), "List<Boolean>"));
        assertThrows(IllegalArgumentException.class, () -> g152RoundTripValue("\"long\"", 1L, "Boolean"));

        assertThrows(NumberFormatException.class, () -> g152RoundTripValue(doubles, List.of(1.5d), "BigInteger[]"));
        assertThrows(NumberFormatException.class, () -> g152RoundTripValue(floats, List.of(1.5f), "BigInteger[]"));
        assertThrows(NumberFormatException.class, () -> g152RoundTripValue(doubles, List.of(1.5d), "List<BigInteger>"));
        assertThrows(NumberFormatException.class, () -> g152RoundTripValue("\"double\"", 1.5d, "BigInteger"));

        // Neighbours that convert the same way item by item and as text: int 1/0 is a boolean, integral doubles and longs are BigIntegers.
        org.junit.jupiter.api.Assertions.assertArrayEquals(new boolean[] { true, false },
                (boolean[]) g152RoundTripValue("{\"type\":\"array\",\"items\":\"int\"}", List.of(1, 0), "boolean[]"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new BigInteger[] { BigInteger.ONE, BigInteger.valueOf(-3) },
                (BigInteger[]) g152RoundTripValue(longs, List.of(1L, -3L), "BigInteger[]"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new long[] { 1L, 0L }, (long[]) g152RoundTripValue(longs, List.of(1L, 0L), "long[]"));
    }

    // ---- deep review 2026-10-16 G159 end ----

    // ---- deep review 2026-10-26 G008 begin ----
    // G008-01: a Map key that is not a String (Avro Utf8, enum constant, Integer) failed the record write with a ClassCastException
    // instead of naming its field by its text (as Avro's own map writer does) or being rejected/discarded as an unknown field.
    @Test
    public void testSerialize_mapWithNonStringKeysForRecordSchema() {
        final Map<Object, Object> source = new java.util.LinkedHashMap<>();
        source.put(new org.apache.avro.util.Utf8("name"), "John");
        source.put(java.time.temporal.ChronoUnit.DAYS, null);
        source.put("age", 30);
        source.put(7, "x");

        final IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
                () -> parser.serialize(source, AvroSerConfig.create().setSchema(testSchema)));
        assertTrue(unknown.getMessage().contains("'Days'"), unknown.getMessage());

        final String projected = parser.serialize(source, AvroSerConfig.create().setSchema(testSchema).setIgnoreUnknownFields(true));
        final Map<String, Object> read = parser.deserialize(projected, AvroDeserConfig.create().setSchema(testSchema), Map.class);
        assertEquals(2, read.size());
        assertEquals("John", read.get("name").toString());
        assertEquals(30, read.get("age"));

        // An enum constant naming a field writes that field.
        final Map<java.time.DayOfWeek, Object> byEnum = new java.util.EnumMap<>(java.time.DayOfWeek.class);
        byEnum.put(java.time.DayOfWeek.MONDAY, "x");
        final Schema enumKeyed = new Schema.Parser()
                .parse("{\"type\":\"record\",\"name\":\"EnumKeyed\",\"fields\":[{\"name\":\"MONDAY\",\"type\":\"string\"}]}");
        final Map<String, Object> enumRead = parser.deserialize(parser.serialize(byEnum, AvroSerConfig.create().setSchema(enumKeyed)),
                AvroDeserConfig.create().setSchema(enumKeyed), Map.class);
        assertEquals("x", enumRead.get("MONDAY").toString());
    }
    // ---- deep review 2026-10-26 G008 end ----

    // ---- deep review 2026-10-26 verify G119 begin ----

    // G008-01: a map datum read back from Avro has Utf8 keys; written again under a record schema, directly or as a nested record
    // value, those keys name the fields (it failed with a ClassCastException).
    @Test
    public void testSerialize_recordFromMapDatumReadBackWithUtf8Keys() {
        final Schema mapSchema = new Schema.Parser().parse("{\"type\":\"map\",\"values\":\"string\"}");
        final Map<String, Object> source = new java.util.LinkedHashMap<>();
        source.put("name", "John");
        final Map<Object, Object> readBack = parser.deserialize(parser.serialize(source, AvroSerConfig.create().setSchema(mapSchema)),
                AvroDeserConfig.create().setSchema(mapSchema), Map.class);
        assertEquals(org.apache.avro.util.Utf8.class, readBack.keySet().iterator().next().getClass());
        readBack.put("age", 30);

        final Map<String, Object> record = parser.deserialize(parser.serialize(readBack, AvroSerConfig.create().setSchema(testSchema)),
                AvroDeserConfig.create().setSchema(testSchema), Map.class);
        assertEquals("John", record.get("name").toString());
        assertEquals(30, record.get("age"));

        final Schema outer = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Outer\",\"fields\":[{\"name\":\"inner\",\"type\":"
                + "{\"type\":\"record\",\"name\":\"Inner\",\"fields\":[{\"name\":\"x\",\"type\":\"int\"}]}}]}");
        final Map<Object, Object> inner = new java.util.LinkedHashMap<>();
        inner.put(new org.apache.avro.util.Utf8("x"), 3);
        final Map<String, Object> nested = parser.deserialize(parser.serialize(Map.of("inner", inner), AvroSerConfig.create().setSchema(outer)),
                AvroDeserConfig.create().setSchema(outer), Map.class);
        assertEquals(3, ((GenericRecord) nested.get("inner")).get("x"));
    }

    // G008-01: keys with the same text write the field once with the value of the last key iterated (as documented); a null key
    // is still rejected as an unknown field by default and discarded under projection.
    @Test
    public void testSerialize_mapKeysWithSameTextAndNullKeyForRecordSchema() {
        final Map<Object, Object> source = new java.util.LinkedHashMap<>();
        source.put("name", "A");
        source.put(new org.apache.avro.util.Utf8("name"), "B");
        source.put("age", 1);
        Map<String, Object> read = parser.deserialize(parser.serialize(source, AvroSerConfig.create().setSchema(testSchema)),
                AvroDeserConfig.create().setSchema(testSchema), Map.class);
        assertEquals("B", read.get("name").toString());

        source.clear();
        source.put(new org.apache.avro.util.Utf8("name"), "B");
        source.put("name", "A");
        source.put("age", 1);
        read = parser.deserialize(parser.serialize(source, AvroSerConfig.create().setSchema(testSchema)), AvroDeserConfig.create().setSchema(testSchema),
                Map.class);
        assertEquals("A", read.get("name").toString());

        final Map<String, Object> withNullKey = new java.util.LinkedHashMap<>();
        withNullKey.put("name", "n");
        withNullKey.put("age", 2);
        withNullKey.put(null, "x");
        final IllegalArgumentException unknown = assertThrows(IllegalArgumentException.class,
                () -> parser.serialize(withNullKey, AvroSerConfig.create().setSchema(testSchema)));
        assertTrue(unknown.getMessage().contains("'null'"), unknown.getMessage());

        read = parser.deserialize(parser.serialize(withNullKey, AvroSerConfig.create().setSchema(testSchema).setIgnoreUnknownFields(true)),
                AvroDeserConfig.create().setSchema(testSchema), Map.class);
        assertEquals(2, read.size());
        assertEquals("n", read.get("name").toString());
    }

    // ---- deep review 2026-10-26 verify G119 end ----
}
