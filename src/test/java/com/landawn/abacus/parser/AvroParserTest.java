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
        Method method = AvroParser.class.getDeclaredMethod("toGenericRecord", Object.class, Schema.class);
        method.setAccessible(true);
        GenericRecord result = (GenericRecord) method.invoke(parser, Arrays.asList("ListUser", 29), testSchema);
        assertEquals("ListUser", result.get("name").toString());
        assertEquals(29, result.get("age"));
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
            + "{\"name\":\"color\",\"type\":{\"type\":\"enum\",\"name\":\"Color\",\"symbols\":[\"RED\",\"GREEN\"]}}," + "{\"name\":\"name\",\"type\":\"string\"}]}");

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
                parser.deserialize(parser.serialize(unpainted, AvroSerConfig.create().setSchema(NULLABLE_ENUM_SCHEMA)), nullableDeser, Painted.class).getColor());
    }

    @Test
    public void reviewFixes20260906_unknownPropertiesAreSkippedOnWrite() {
        final ExtraPropertyBean bean = new ExtraPropertyBean();
        bean.setName("x");
        bean.setAge(9);

        final GenericRecord fromBean = writeAndReadRecord(bean, testSchema);
        assertEquals("x", fromBean.get("name").toString());
        assertEquals(9, fromBean.get("age"));
        assertEquals(2, fromBean.getSchema().getFields().size());

        final Map<String, Object> map = nameAge(10);
        map.put("zzz", "ignored");
        map.put("", "ignored-too");
        final GenericRecord fromMap = writeAndReadRecord(map, testSchema);
        assertEquals("a", fromMap.get("name").toString());
        assertEquals(10, fromMap.get("age"));

        // The record still needs its own fields: a missing non-nullable field is rejected by Avro as before.
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
}
