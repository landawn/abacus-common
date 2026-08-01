package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Strings;

public class KryoParserTest extends TestBase {

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

    public static class LateRegisteredObject implements Serializable {
        private String name;

        public LateRegisteredObject() {
        }

        public LateRegisteredObject(String name) {
            this.name = name;
        }
    }

    public static class LateRegisteredObjectSerializer extends Serializer<LateRegisteredObject> {
        @Override
        public void write(Kryo kryo, Output output, LateRegisteredObject object) {
            output.writeString(object.name + "-registered");
        }

        @Override
        public LateRegisteredObject read(Kryo kryo, Input input, Class<? extends LateRegisteredObject> type) {
            return new LateRegisteredObject(input.readString());
        }
    }

    public static class GlobalExplicitIdTarget {
    }

    public static class InstanceExplicitIdTarget {
    }

    public static class ReverseInstanceExplicitIdTarget {
    }

    public static class ReverseGlobalExplicitIdTarget {
    }

    public static class BuiltInIdInstanceTarget {
    }

    public static class BuiltInIdGlobalTarget {
    }

    public static class ImplicitInstanceRegistrationTarget {
    }

    public static class ExplicitInstanceRegistrationTarget {
    }

    public static class ImplicitInstanceSerializerTarget {
    }

    public static class ExplicitInstanceSerializerTarget {
    }

    private static final class EmptySerializer<T> extends Serializer<T> {
        @Override
        public void write(final Kryo kryo, final Output output, final T object) {
        }

        @Override
        public T read(final Kryo kryo, final Input input, final Class<? extends T> type) {
            return null;
        }
    }

    public static class GlobalInstanceOverrideTarget {
        String value;
    }

    public static class GlobalImplicitInstanceExplicitIdOverrideTarget {
    }

    public static class GlobalInstanceOverrideSerializer extends Serializer<GlobalInstanceOverrideTarget> {
        @Override
        public void write(final Kryo kryo, final Output output, final GlobalInstanceOverrideTarget object) {
            output.writeString(object.value);
        }

        @Override
        public GlobalInstanceOverrideTarget read(final Kryo kryo, final Input input, final Class<? extends GlobalInstanceOverrideTarget> type) {
            final GlobalInstanceOverrideTarget result = new GlobalInstanceOverrideTarget();
            result.value = input.readString();
            return result;
        }
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
    public void testSerializeToString() {
        TestObject obj = new TestObject("test", 123);
        String result = parser.serialize(obj, (KryoSerConfig) null);
        assertNotNull(result);
        assertTrue(Strings.base64Decode(result).length > 0);
    }

    @Test
    public void testSerializationWithConfig() {
        TestObject obj = new TestObject("test", 123);
        KryoSerConfig config = new KryoSerConfig();
        config.setWriteClass(true);

        String result = parser.serialize(obj, config);
        assertNotNull(result);
    }

    @Test
    public void testGlobalRegistrationInvalidatesExistingParserPool() {
        try {
            LateRegisteredObject warmup = new LateRegisteredObject("warmup");
            LateRegisteredObject warmed = parser.deserialize(parser.serialize(warmup, (KryoSerConfig) null), null, LateRegisteredObject.class);
            assertEquals("warmup", warmed.name);

            ParserFactory.registerKryo(LateRegisteredObject.class, new LateRegisteredObjectSerializer());

            LateRegisteredObject original = new LateRegisteredObject("late");
            LateRegisteredObject result = parser.deserialize(parser.serialize(original, (KryoSerConfig) null), null, LateRegisteredObject.class);
            assertEquals("late-registered", result.name);
        } finally {
            unregisterKryoForTest(LateRegisteredObject.class);
        }
    }

    @Test
    public void testDeserializationWithConfig() {
        TestObject original = new TestObject("test", 123);
        KryoSerConfig serConfig = new KryoSerConfig();
        serConfig.setWriteClass(true);
        String serialized = parser.serialize(original, serConfig);

        KryoDeserConfig deserConfig = new KryoDeserConfig();
        TestObject result = parser.deserialize(serialized, deserConfig, (Class<TestObject>) null);

        assertEquals(original, result);
    }

    @Test
    public void testSerializeCollection() {
        List<String> list = Arrays.asList("one", "two", "three");
        String serialized = parser.serialize(list, (KryoSerConfig) null);
        assertNotNull(serialized);

        List<String> deserialized = parser.deserialize(serialized, null, ArrayList.class);
        assertEquals(list, deserialized);
    }

    @Test
    public void testSerializeMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("one", 1);
        map.put("two", 2);

        String serialized = parser.serialize(map, (KryoSerConfig) null);
        assertNotNull(serialized);

        Map<String, Integer> deserialized = parser.deserialize(serialized, null, HashMap.class);
        assertEquals(map, deserialized);
    }

    @Test
    public void testSerializeArray() {
        int[] array = { 1, 2, 3, 4, 5 };
        String serialized = parser.serialize(array, (KryoSerConfig) null);
        assertNotNull(serialized);

        int[] deserialized = parser.deserialize(serialized, null, int[].class);
        assertArrayEquals(array, deserialized);
    }

    @Test
    public void testSerializeWithWriteClassConfig() {
        TestObject obj = new TestObject("writeClass", 100);
        KryoSerConfig config = new KryoSerConfig();
        config.setWriteClass(true);

        String serialized = parser.serialize(obj, config);
        assertNotNull(serialized);

        KryoDeserConfig deserConfig = new KryoDeserConfig();
        TestObject result = parser.deserialize(serialized, deserConfig, (Class<TestObject>) null);
        assertEquals(obj, result);
    }

    @Test
    public void testSerializeNullString() {
        String serialized = parser.serialize("hello", (KryoSerConfig) null);
        assertNotNull(serialized);
        String result = parser.deserialize(serialized, null, String.class);
        assertEquals("hello", result);
    }

    /**
     * Bug fix: KryoParser.write(Object,KryoSerConfig,Output) previously called
     * kryo.writeObject(output, obj) even when obj was null.  Kryo's writeObject
     * does NOT accept null and throws NullPointerException.  The fix routes null
     * objects through kryo.writeClassAndObject(), which handles null safely.
     * Serializing null and then deserializing (with class info in stream) must
     * round-trip correctly instead of blowing up.
     */
    @Test
    public void testSerializeNullDoesNotThrow() {
        // Serializing null via writeClassAndObject should not throw.
        // Use writeClass=true (default behaviour after the null-routing fix) so the
        // reader can use readClassAndObject to get null back.
        KryoSerConfig serConfig = KryoSerConfig.create().setWriteClass(true);
        assertDoesNotThrow(() -> {
            String serialized = parser.serialize(null, serConfig);
            assertNotNull(serialized);
            // Deserialise back – target class null means readClassAndObject.
            Object result = parser.deserialize(serialized, null, (Class<Object>) null);
            assertNull(result);
        });
    }

    /**
     * Verify that a non-null object serialized without writeClass still round-trips
     * correctly after the null-routing fix.
     */
    @Test
    public void testSerializeNonNullWithoutWriteClass() {
        TestObject original = new TestObject("roundtrip", 42);
        String serialized = parser.serialize(original, (KryoSerConfig) null);
        assertNotNull(serialized);
        TestObject restored = parser.deserialize(serialized, null, TestObject.class);
        assertEquals(original, restored);
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
    public void testSerializeToFileWithConfig() throws IOException {
        TestObject obj = new TestObject("fileConfig", 200);
        KryoSerConfig config = new KryoSerConfig();
        File file = tempDir.resolve("test-config.kryo").toFile();

        parser.serialize(obj, config, file);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);

        TestObject result = parser.deserialize(file, null, TestObject.class);
        assertEquals(obj, result);
    }

    @Test
    public void testSerializeToOutputStreamWithConfig() throws IOException {
        TestObject obj = new TestObject("osConfig", 300);
        KryoSerConfig config = new KryoSerConfig();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        parser.serialize(obj, config, baos);
        byte[] bytes = baos.toByteArray();
        assertTrue(bytes.length > 0);
    }

    @Test
    public void testSerializeToWriterWithConfig() throws IOException {
        TestObject obj = new TestObject("writerConfig", 400);
        KryoSerConfig config = new KryoSerConfig();
        StringWriter writer = new StringWriter();

        parser.serialize(obj, config, writer);
        String result = writer.toString();
        assertNotNull(result);
        assertTrue(result.length() > 0);
    }

    @Test
    public void testDeserializeFromString() {
        TestObject original = new TestObject("test", 123);
        String serialized = parser.serialize(original, (KryoSerConfig) null);

        TestObject result = parser.deserialize(serialized, null, TestObject.class);

        assertEquals(original, result);
    }

    @Test
    public void testDeserializeWithTypeParameter() {
        TestObject original = new TestObject("typeTest", 42);
        String serialized = parser.serialize(original, (KryoSerConfig) null);

        com.landawn.abacus.type.Type<TestObject> type = com.landawn.abacus.type.Type.of(TestObject.class);
        TestObject result = parser.deserialize(serialized, null, type);
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
    public void testDeserializeFromFileWithTypeParameter() throws IOException {
        TestObject original = new TestObject("fileType", 99);
        File file = tempDir.resolve("test-type.kryo").toFile();
        parser.serialize(original, null, file);

        com.landawn.abacus.type.Type<TestObject> type = com.landawn.abacus.type.Type.of(TestObject.class);
        TestObject result = parser.deserialize(file, null, type);
        assertEquals(original, result);
    }

    @Test
    public void testDeserializeFromInputStreamWithTypeParameter() throws IOException {
        TestObject original = new TestObject("streamType", 77);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(original, null, baos);

        ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
        com.landawn.abacus.type.Type<TestObject> type = com.landawn.abacus.type.Type.of(TestObject.class);
        TestObject result = parser.deserialize(bais, null, type);
        assertEquals(original, result);
    }

    @Test
    public void testDeserializeFromReaderWithTypeParameter() throws IOException {
        TestObject original = new TestObject("readerType", 55);
        StringWriter writer = new StringWriter();
        parser.serialize(original, null, writer);

        StringReader reader = new StringReader(writer.toString());
        com.landawn.abacus.type.Type<TestObject> type = com.landawn.abacus.type.Type.of(TestObject.class);
        TestObject result = parser.deserialize(reader, null, type);
        assertEquals(original, result);
    }

    @Test
    public void testShallowCopy() {
        TestObject original = new TestObject("test", 123);
        TestObject copy = parser.shallowCopy(original);

        assertEquals(original, copy);
        assertNotSame(original, copy);
    }

    @Test
    public void testDeepCopy() {
        TestObject original = new TestObject("test", 123);
        TestObject cloned = parser.deepCopy(original);

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
    public void testComplexNestedObject() {
        Map<String, List<TestObject>> complex = new HashMap<>();
        complex.put("list1", Arrays.asList(new TestObject("a", 1), new TestObject("b", 2)));
        complex.put("list2", Arrays.asList(new TestObject("c", 3), new TestObject("d", 4)));

        byte[] encoded = parser.encode(complex);
        Map<String, List<TestObject>> decoded = parser.decode(encoded);

        assertEquals(complex, decoded);
    }

    @Test
    public void testEncodeNull() {
        byte[] result = parser.encode(null);
        // assertThrows(Exception.class, () -> parser.encode(null));
        assertEquals(0, result[0]);
    }

    @Test
    public void testDecode() {
        TestObject original = new TestObject("test", 123);
        byte[] encoded = parser.encode(original);

        TestObject decoded = parser.decode(encoded);

        assertEquals(original, decoded);
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

    @Test
    public void testRegisterClassWithSerializerRoundTrip() {
        parser.register(TestObject.class, new CustomSerializer(), 500);

        TestObject original = new TestObject("registered", 999);
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
    public void testRegisterClassRejectsNegativeIdImmediately() {
        assertThrows(IllegalArgumentException.class, () -> parser.register(TestObject.class, -1));
        assertThrows(IllegalArgumentException.class, () -> parser.register(TestObject.class, new CustomSerializer(), -1));
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
    public void testRegisterPrimitiveWrapperWithExplicitIdPreservesBuiltInSerializer() {
        final int explicitId = 1_910_013;
        parser.register(Integer.class, explicitId);

        assertEquals(123, (int) parser.decode(parser.encode(123)));

        final Kryo kryo = parser.createKryo();

        try {
            assertEquals(explicitId, kryo.getRegistration(Integer.class).getId());
        } finally {
            parser.recycle(kryo);
        }
    }

    @Test
    public void testRegisterClassWithIdRejectsDuplicateIdBeforeMutation() {
        final int occupiedId = 1_910_001;
        final int laterValidId = 1_910_002;

        parser.register(TestObject.class, occupiedId);
        parser.register(LateRegisteredObject.class, laterValidId);
        assertThrows(IllegalArgumentException.class, () -> parser.register(LateRegisteredObject.class, occupiedId));

        final Kryo kryo = parser.createKryo();

        try {
            assertEquals(occupiedId, kryo.getRegistration(TestObject.class).getId());
            assertEquals(laterValidId, kryo.getRegistration(LateRegisteredObject.class).getId());
        } finally {
            parser.recycle(kryo);
        }
    }

    @Test
    public void testRegisterClassWithSerializerAndIdRejectsDuplicateIdBeforeMutation() {
        final int occupiedId = 1_910_003;
        final int laterValidId = 1_910_004;
        final Serializer<LateRegisteredObject> retainedSerializer = new LateRegisteredObjectSerializer();

        parser.register(TestObject.class, new CustomSerializer(), occupiedId);
        parser.register(LateRegisteredObject.class, retainedSerializer, laterValidId);
        assertThrows(IllegalArgumentException.class,
                () -> parser.register(LateRegisteredObject.class, new LateRegisteredObjectSerializer(), occupiedId));

        final Kryo kryo = parser.createKryo();

        try {
            assertEquals(occupiedId, kryo.getRegistration(TestObject.class).getId());
            assertEquals(laterValidId, kryo.getRegistration(LateRegisteredObject.class).getId());
            assertSame(retainedSerializer, kryo.getRegistration(LateRegisteredObject.class).getSerializer());
        } finally {
            parser.recycle(kryo);
        }
    }

    @Test
    public void testCreateKryoRejectsInstanceExplicitIdAlreadyAssignedImplicitly() {
        parser.register(ImplicitInstanceRegistrationTarget.class);
        final Kryo initialKryo = parser.createKryo();
        final int implicitId;

        try {
            implicitId = initialKryo.getRegistration(ImplicitInstanceRegistrationTarget.class).getId();
        } finally {
            parser.recycle(initialKryo);
        }

        parser.register(ExplicitInstanceRegistrationTarget.class, implicitId);

        assertThrows(IllegalArgumentException.class, parser::createKryo);
    }

    @Test
    public void testCreateKryoRejectsInstanceExplicitSerializerIdAlreadyAssignedImplicitly() {
        parser.register(ImplicitInstanceSerializerTarget.class, new EmptySerializer<>());
        final Kryo initialKryo = parser.createKryo();
        final int implicitId;

        try {
            implicitId = initialKryo.getRegistration(ImplicitInstanceSerializerTarget.class).getId();
        } finally {
            parser.recycle(initialKryo);
        }

        parser.register(ExplicitInstanceSerializerTarget.class, new EmptySerializer<>(), implicitId);

        assertThrows(IllegalArgumentException.class, parser::createKryo);
    }

    @Test
    public void testRegisterClassWithIdCanReplaceBuiltInRegistration() {
        final Kryo baseline = parser.createKryo();
        final int builtInId;

        try {
            builtInId = baseline.getRegistration(int.class).getId();
        } finally {
            parser.recycle(baseline);
        }

        parser.register(BuiltInIdInstanceTarget.class, builtInId);

        final Kryo kryo = parser.createKryo();

        try {
            assertEquals(builtInId, kryo.getRegistration(BuiltInIdInstanceTarget.class).getId());
            assertSame(BuiltInIdInstanceTarget.class, kryo.getRegistration(builtInId).getType());
        } finally {
            parser.recycle(kryo);
        }
    }

    @Test
    public void testGlobalRegisterClassWithIdCanReplaceBuiltInRegistration() {
        final Kryo baseline = parser.createKryo();
        final int builtInId;

        try {
            builtInId = baseline.getRegistration(int.class).getId();
        } finally {
            parser.recycle(baseline);
        }

        try {
            ParserFactory.registerKryo(BuiltInIdGlobalTarget.class, builtInId);
            final Kryo kryo = parser.createKryo();

            try {
                assertEquals(builtInId, kryo.getRegistration(BuiltInIdGlobalTarget.class).getId());
                assertSame(BuiltInIdGlobalTarget.class, kryo.getRegistration(builtInId).getType());
            } finally {
                parser.recycle(kryo);
            }
        } finally {
            unregisterKryoForTest(BuiltInIdGlobalTarget.class);
        }
    }

    @Test
    public void testRegisterClassWithIdRejectsGlobalDuplicateIdBeforeMutation() {
        final int globallyOccupiedId = 1_910_005;
        final int laterValidId = 1_910_006;

        try {
            ParserFactory.registerKryo(GlobalExplicitIdTarget.class, globallyOccupiedId);
            assertThrows(IllegalArgumentException.class, () -> parser.register(InstanceExplicitIdTarget.class, globallyOccupiedId));

            parser.register(InstanceExplicitIdTarget.class, laterValidId);
            final Kryo kryo = parser.createKryo();

            try {
                assertEquals(globallyOccupiedId, kryo.getRegistration(GlobalExplicitIdTarget.class).getId());
                assertEquals(laterValidId, kryo.getRegistration(InstanceExplicitIdTarget.class).getId());
            } finally {
                parser.recycle(kryo);
            }
        } finally {
            unregisterKryoForTest(GlobalExplicitIdTarget.class);
        }
    }

    @Test
    public void testInstanceRegistrationOverridesGlobalRegistrationForSameClass() {
        final int globalId = 1_910_007;
        final int instanceId = 1_910_008;
        final Serializer<GlobalInstanceOverrideTarget> instanceSerializer = new GlobalInstanceOverrideSerializer();

        try {
            ParserFactory.registerKryo(GlobalInstanceOverrideTarget.class, globalId);
            parser.register(GlobalInstanceOverrideTarget.class, instanceSerializer, instanceId);
            final Kryo kryo = parser.createKryo();

            try {
                assertEquals(instanceId, kryo.getRegistration(GlobalInstanceOverrideTarget.class).getId());
                assertSame(instanceSerializer, kryo.getRegistration(GlobalInstanceOverrideTarget.class).getSerializer());
            } finally {
                parser.recycle(kryo);
            }
        } finally {
            unregisterKryoForTest(GlobalInstanceOverrideTarget.class);
        }
    }

    @Test
    public void testInstanceExplicitIdOverridesSameClassGlobalImplicitRegistration() {
        final int instanceId = 1_910_012;

        try {
            ParserFactory.registerKryo(GlobalImplicitInstanceExplicitIdOverrideTarget.class);
            parser.register(GlobalImplicitInstanceExplicitIdOverrideTarget.class, instanceId);
            final Kryo kryo = parser.createKryo();

            try {
                assertEquals(instanceId, kryo.getRegistration(GlobalImplicitInstanceExplicitIdOverrideTarget.class).getId());
            } finally {
                parser.recycle(kryo);
            }
        } finally {
            unregisterKryoForTest(GlobalImplicitInstanceExplicitIdOverrideTarget.class);
        }
    }

    @Test
    public void testCreateKryoRejectsGlobalIdRegisteredAfterInstanceConflict() {
        final int conflictingId = 1_910_009;

        parser.register(ReverseInstanceExplicitIdTarget.class, conflictingId);

        try {
            ParserFactory.registerKryo(ReverseGlobalExplicitIdTarget.class, conflictingId);

            assertThrows(IllegalArgumentException.class, parser::createKryo);
        } finally {
            unregisterKryoForTest(ReverseGlobalExplicitIdTarget.class);
        }
    }

    @Test
    public void testLatestRegistrationOverloadReplacesEarlierVariant() {
        final Serializer<TestObject> staleSerializer = new CustomSerializer();
        parser.register(TestObject.class, staleSerializer, 500);
        parser.register(TestObject.class, 501);

        final Kryo kryo = parser.createKryo();

        try {
            assertEquals(501, kryo.getRegistration(TestObject.class).getId());
            assertNotSame(staleSerializer, kryo.getRegistration(TestObject.class).getSerializer());
        } finally {
            parser.recycle(kryo);
        }
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
    public void testDeserializeTypedWriteClassPayload() {
        final String encoded = parser.serialize("abc", KryoSerConfig.create().setWriteClass(true));

        assertEquals("abc", parser.deserialize(encoded, null, String.class));
    }

    @Test
    public void testDeserializeTypedWriteClassInputStreamPayload() {
        final String encoded = parser.serialize("abc", KryoSerConfig.create().setWriteClass(true));

        assertEquals("abc", parser.deserialize(new ByteArrayInputStream(Strings.base64Decode(encoded)), null, String.class));
    }

    @Test
    public void testDeserializeTypedNullPayload() {
        final String encoded = parser.serialize(null, (KryoSerConfig) null);

        assertNull(parser.deserialize(encoded, null, String.class));
    }

    @Test
    public void testDeserializeTypedDoesNotSelfSuppressReusedSerializerFailure() {
        final RuntimeException serializerFailure = new RuntimeException("serializer failure");

        parser.register(TestObject.class, new Serializer<TestObject>() {
            @Override
            public void write(final Kryo kryo, final Output output, final TestObject object) {
                // The class header alone is sufficient to exercise both typed-read strategies.
            }

            @Override
            public TestObject read(final Kryo kryo, final Input input, final Class<? extends TestObject> type) {
                throw serializerFailure;
            }
        }, 501);

        final String encoded = parser.serialize(new TestObject("ignored", 1), KryoSerConfig.create().setWriteClass(true));
        final RuntimeException thrown = assertThrows(RuntimeException.class, () -> parser.deserialize(encoded, null, TestObject.class));

        assertSame(serializerFailure, thrown);
        assertEquals(0, thrown.getSuppressed().length);
    }

    private static void unregisterKryoForTest(final Class<?> type) {
        synchronized (ParserFactory._kryoRegistrationLock) {
            ParserFactory._kryoClassSet.remove(type);
            ParserFactory._kryoClassIdMap.remove(type);
            ParserFactory._kryoClassSerializerMap.remove(type);
            ParserFactory._kryoClassSerializerIdMap.remove(type);
            ParserFactory._kryoRegistrationVersion.incrementAndGet();
        }
    }
}
