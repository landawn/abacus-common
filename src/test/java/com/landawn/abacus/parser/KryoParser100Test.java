package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Strings;

@Tag("new-test")
public class KryoParser100Test extends TestBase {

    private KryoParser parser;

    @TempDir
    Path tempDir;

    @BeforeEach
    public void setUp() {
        parser = new KryoParser();
    }

    public static class TestObject implements Serializable {
        private String name;
        private int value;

        public TestObject() {
        }

        public TestObject(String name, int value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getValue() {
            return value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            TestObject that = (TestObject) o;
            return value == that.value && Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, value);
        }
    }

    public static class CustomSerializer extends Serializer<TestObject> {
        @Override
        public void write(Kryo kryo, Output output, TestObject object) {
            output.writeString(object.getName());
            output.writeInt(object.getValue());
        }

        @Override
        public TestObject read(Kryo kryo, Input input, Class<? extends TestObject> type) {
            String name = input.readString();
            int value = input.readInt();
            return new TestObject(name, value);
        }
    }

    @Test
    public void testSerializeToString() {
        TestObject obj = new TestObject("test", 123);
        String result = parser.serialize(obj, (KryoSerializationConfig) null);
        assertNotNull(result);
        assertTrue(Strings.base64Decode(result).length > 0);
    }

    @Test
    public void testSerializeNull() {
        assertThrows(IllegalArgumentException.class, () -> parser.serialize(null, (KryoSerializationConfig) null));
    }

    @Test
    public void testSerializeToFile() throws IOException {
        TestObject obj = new TestObject("test", 123);
        File file = tempDir.resolve("test.kryo").toFile();

        parser.serialize(obj, null, file);

        assertTrue(file.exists());
        assertTrue(file.length() > 0);
    }

    @Test
    public void testSerializeToOutputStream() throws IOException {
        TestObject obj = new TestObject("test", 123);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        parser.serialize(obj, null, baos);

        byte[] bytes = baos.toByteArray();
        assertTrue(bytes.length > 0);
    }

    @Test
    public void testSerializeToWriter() throws IOException {
        TestObject obj = new TestObject("test", 123);
        StringWriter writer = new StringWriter();

        parser.serialize(obj, null, writer);

        String result = writer.toString();
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void testDeserializeFromString() {
        TestObject original = new TestObject("test", 123);
        String serialized = parser.serialize(original, (KryoSerializationConfig) null);

        TestObject result = parser.deserialize(serialized, null, TestObject.class);

        assertEquals(original, result);
    }

    @Test
    public void testDeserializeNullString() {
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize((String) null, null, TestObject.class));
    }

    @Test
    public void testDeserializeFromFile() throws IOException {
        TestObject original = new TestObject("test", 123);
        File file = tempDir.resolve("test.kryo").toFile();
        parser.serialize(original, null, file);

        TestObject result = parser.deserialize(file, null, TestObject.class);

        assertEquals(original, result);
    }

    @Test
    public void testDeserializeFromInputStream() throws IOException {
        TestObject original = new TestObject("test", 123);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(original, null, baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        TestObject result = parser.deserialize(bais, null, TestObject.class);

        assertEquals(original, result);
    }

    @Test
    public void testDeserializeFromReader() throws IOException {
        TestObject original = new TestObject("test", 123);
        StringWriter writer = new StringWriter();
        parser.serialize(original, null, writer);

        StringReader reader = new StringReader(writer.toString());
        TestObject result = parser.deserialize(reader, null, TestObject.class);

        assertEquals(original, result);
    }

    @Test
    public void testCopy() {
        TestObject original = new TestObject("test", 123);
        TestObject copy = parser.copy(original);

        assertEquals(original, copy);
        assertNotSame(original, copy);
    }

    @Test
    public void testClone() {
        TestObject original = new TestObject("test", 123);
        TestObject cloned = parser.clone(original);

        assertEquals(original, cloned);
        assertNotSame(original, cloned);
    }

    @Test
    public void testEncode() {
        TestObject obj = new TestObject("test", 123);
        byte[] encoded = parser.encode(obj);

        assertNotNull(encoded);
        assertTrue(encoded.length > 0);
    }

    @Test
    public void testDecode() {
        TestObject original = new TestObject("test", 123);
        byte[] encoded = parser.encode(original);

        TestObject decoded = parser.decode(encoded);

        assertEquals(original, decoded);
    }

    @Test
    public void testRegisterClass() {
        assertDoesNotThrow(() -> parser.register(TestObject.class));
    }

    @Test
    public void testRegisterClassWithId() {
        assertDoesNotThrow(() -> parser.register(TestObject.class, 100));
    }

    @Test
    public void testRegisterClassWithSerializer() {
        assertDoesNotThrow(() -> parser.register(TestObject.class, new CustomSerializer()));
    }

    @Test
    public void testRegisterClassWithSerializerAndId() {
        assertDoesNotThrow(() -> parser.register(TestObject.class, new CustomSerializer(), 200));
    }

    @Test
    public void testRegisterNullClass() {
        assertThrows(IllegalArgumentException.class, () -> parser.register(null));
    }

    @Test
    public void testRegisterNullSerializer() {
        assertThrows(IllegalArgumentException.class, () -> parser.register(TestObject.class, null));
    }

    @Test
    public void testSerializationWithConfig() {
        TestObject obj = new TestObject("test", 123);
        KryoSerializationConfig config = new KryoSerializationConfig();
        config.writeClass(true);

        String result = parser.serialize(obj, config);
        assertNotNull(result);
    }

    @Test
    public void testDeserializationWithConfig() {
        TestObject original = new TestObject("test", 123);
        KryoSerializationConfig serConfig = new KryoSerializationConfig();
        serConfig.writeClass(true);
        String serialized = parser.serialize(original, serConfig);

        KryoDeserializationConfig deserConfig = new KryoDeserializationConfig();
        TestObject result = parser.deserialize(serialized, deserConfig, null);

        assertEquals(original, result);
    }

    @Test
    public void testSerializeCollection() {
        List<String> list = Arrays.asList("one", "two", "three");
        String serialized = parser.serialize(list, (KryoSerializationConfig) null);
        assertNotNull(serialized);

        List<String> deserialized = parser.deserialize(serialized, null, ArrayList.class);
        assertEquals(list, deserialized);
    }

    @Test
    public void testSerializeMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);

        String serialized = parser.serialize(map, (KryoSerializationConfig) null);
        assertNotNull(serialized);

        Map<String, Integer> deserialized = parser.deserialize(serialized, null, HashMap.class);
        assertEquals(map, deserialized);
    }

    @Test
    public void testSerializeArray() {
        int[] array = { 1, 2, 3, 4, 5 };
        String serialized = parser.serialize(array, (KryoSerializationConfig) null);
        assertNotNull(serialized);

        int[] deserialized = parser.deserialize(serialized, null, int[].class);
        assertArrayEquals(array, deserialized);
    }

    @Test
    public void testSerializePrimitiveTypes() {
        assertEquals(123, (int) parser.decode(parser.encode(123)));
        assertEquals(123L, (long) parser.decode(parser.encode(123L)));
        assertEquals(123.45f, (float) parser.decode(parser.encode(123.45f)), 0.001);
        assertEquals(123.45, (double) parser.decode(parser.encode(123.45)), 0.001);
        assertEquals(true, parser.decode(parser.encode(true)));
        assertEquals('A', (char) parser.decode(parser.encode('A')));
        assertEquals("test", parser.decode(parser.encode("test")));
    }

    @Test
    public void testSerializeBigNumbers() {
        BigInteger bigInt = new BigInteger("12345678901234567890");
        BigDecimal bigDec = new BigDecimal("123456789.0123456789");

        BigInteger decodedInt = parser.decode(parser.encode(bigInt));
        BigDecimal decodedDec = parser.decode(parser.encode(bigDec));

        assertEquals(bigInt, decodedInt);
        assertEquals(bigDec, decodedDec);
    }

    @Test
    public void testComplexNestedObject() {
        Map<String, List<TestObject>> complex = new HashMap<>();
        complex.put("list1", Arrays.asList(new TestObject("a", 1), new TestObject("b", 2)));
        complex.put("list2", Arrays.asList(new TestObject("c", 3), new TestObject("d", 4)));

        byte[] encoded = parser.encode(complex);
        Map<String, List<TestObject>> decoded = parser.decode(encoded);

        assertEquals(complex, decoded);
    }

    @Test
    public void testEmptyCollections() {
        List<String> emptyList = new ArrayList<>();
        Map<String, String> emptyMap = new HashMap<>();
        Set<String> emptySet = new HashSet<>();

        assertEquals(emptyList, parser.decode(parser.encode(emptyList)));
        assertEquals(emptyMap, parser.decode(parser.encode(emptyMap)));
        assertEquals(emptySet, parser.decode(parser.encode(emptySet)));
    }
}
