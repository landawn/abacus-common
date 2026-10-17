package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.u.Nullable;

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
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
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
            return new TestObject(input.readString(), input.readInt());
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
    public void testRecyclingBeyondCapacityReleasesDiscardedKryo() throws Exception {
        final Kryo reusable = parser.createKryo();
        parser.recycle(reusable);
        assertSame(reusable, parser.createKryo());

        final Field poolField = KryoParser.class.getDeclaredField("kryoPool");
        poolField.setAccessible(true);
        final List<Kryo> pool = (List<Kryo>) poolField.get(parser);
        final Field trackedField = KryoParser.class.getDeclaredField("xPool");
        trackedField.setAccessible(true);
        final Map<Kryo, Kryo> tracked = (Map<Kryo, Kryo>) trackedField.get(parser);

        pool.addAll(Collections.nCopies(AbstractParser.POOL_SIZE, new Kryo()));
        try {
            parser.recycle(reusable);
            assertEquals(AbstractParser.POOL_SIZE, pool.size());
            assertNull(tracked.get(reusable));
        } finally {
            pool.clear();
        }
    }

    @Test
    public void testSerializePrimitivesAndCollections() {
        assertEquals(123, (int) parser.decode(parser.encode(123)));
        assertEquals(123L, (long) parser.decode(parser.encode(123L)));
        assertEquals(123.45f, (float) parser.decode(parser.encode(123.45f)), 0.001);
        assertEquals(123.45, (double) parser.decode(parser.encode(123.45)), 0.001);
        assertEquals(true, parser.decode(parser.encode(true)));
        assertEquals('A', (char) parser.decode(parser.encode('A')));
        assertEquals("test", parser.decode(parser.encode("test")));
        assertEquals(new BigInteger("12345678901234567890"), parser.decode(parser.encode(new BigInteger("12345678901234567890"))));
        assertEquals(new BigDecimal("123456789.0123456789"), parser.decode(parser.encode(new BigDecimal("123456789.0123456789"))));

        List<String> list = Arrays.asList("one", "two", "three");
        assertEquals(list, parser.deserialize(parser.serialize(list, (KryoSerConfig) null), null, ArrayList.class));

        Map<String, Integer> map = N.asMap("one", 1, "two", 2);
        assertEquals(map, parser.deserialize(parser.serialize(map, (KryoSerConfig) null), null, HashMap.class));

        int[] array = { 1, 2, 3, 4, 5 };
        assertArrayEquals(array, parser.deserialize(parser.serialize(array, (KryoSerConfig) null), null, int[].class));

        assertEquals(new ArrayList<>(), parser.decode(parser.encode(new ArrayList<>())));
        assertEquals(new HashMap<>(), parser.decode(parser.encode(new HashMap<>())));
        assertEquals(new HashSet<>(), parser.decode(parser.encode(new HashSet<>())));
    }

    @Test
    public void testSerializeToStringAndConfig() {
        TestObject obj = new TestObject("test", 123);
        String result = parser.serialize(obj, (KryoSerConfig) null);
        assertTrue(Strings.base64Decode(result).length > 0);
        assertEquals(obj, parser.deserialize(result, null, TestObject.class));
        assertEquals(obj, parser.deserialize(result, null, com.landawn.abacus.type.Type.of(TestObject.class)));

        KryoSerConfig writeClass = new KryoSerConfig();
        writeClass.setWriteClass(true);
        String serialized = parser.serialize(obj, writeClass);
        assertNotNull(serialized);
        assertEquals(obj, parser.deserialize(serialized, new KryoDeserConfig(), (Class<TestObject>) null));

        TestObject original = new TestObject("roundtrip", 42);
        assertEquals(original, parser.deserialize(parser.serialize(original, (KryoSerConfig) null), null, TestObject.class));

        String hello = parser.serialize("hello", (KryoSerConfig) null);
        assertEquals("hello", parser.deserialize(hello, null, String.class));
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize((String) null, null, TestObject.class));
    }

    @Test
    public void testSerializeNullDoesNotThrow() {
        KryoSerConfig serConfig = KryoSerConfig.create().setWriteClass(true);
        assertDoesNotThrow(() -> {
            String serialized = parser.serialize(null, serConfig);
            assertNotNull(serialized);
            assertNull(parser.deserialize(serialized, null, (Class<Object>) null));
        });
    }

    @Test
    public void testSerializeToDestinations() throws IOException {
        TestObject obj = new TestObject("test", 123);
        File file = tempDir.resolve("test.kryo").toFile();
        parser.serialize(obj, null, file);
        assertTrue(file.exists());
        assertTrue(file.length() > 0);
        assertEquals(obj, parser.deserialize(file, null, TestObject.class));
        assertEquals(obj, parser.deserialize(file, null, com.landawn.abacus.type.Type.of(TestObject.class)));

        TestObject withConfig = new TestObject("fileConfig", 200);
        File configFile = tempDir.resolve("test-config.kryo").toFile();
        parser.serialize(withConfig, new KryoSerConfig(), configFile);
        assertEquals(withConfig, parser.deserialize(configFile, null, TestObject.class));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        parser.serialize(obj, null, baos);
        assertTrue(baos.toByteArray().length > 0);
        assertEquals(obj, parser.deserialize(new ByteArrayInputStream(baos.toByteArray()), null, TestObject.class));
        assertEquals(obj, parser.deserialize(new ByteArrayInputStream(baos.toByteArray()), null, com.landawn.abacus.type.Type.of(TestObject.class)));

        baos.reset();
        parser.serialize(new TestObject("osConfig", 300), new KryoSerConfig(), baos);
        assertTrue(baos.toByteArray().length > 0);

        StringWriter writer = new StringWriter();
        parser.serialize(obj, null, writer);
        assertTrue(writer.toString().length() > 0);
        assertEquals(obj, parser.deserialize(new StringReader(writer.toString()), null, TestObject.class));
        assertEquals(obj, parser.deserialize(new StringReader(writer.toString()), null, com.landawn.abacus.type.Type.of(TestObject.class)));

        writer = new StringWriter();
        parser.serialize(new TestObject("writerConfig", 400), new KryoSerConfig(), writer);
        assertTrue(writer.toString().length() > 0);
    }

    @Test
    public void testCopyAndEncode() {
        TestObject original = new TestObject("test", 123);
        TestObject shallow = parser.shallowCopy(original);
        assertEquals(original, shallow);
        assertNotSame(original, shallow);

        TestObject deep = parser.deepCopy(original);
        assertEquals(original, deep);
        assertNotSame(original, deep);

        byte[] encoded = parser.encode(original);
        assertTrue(encoded.length > 0);
        assertEquals(original, parser.decode(encoded));

        byte[] nullEncoded = parser.encode(null);
        assertEquals(0, nullEncoded[0]);

        Map<String, List<TestObject>> complex = new HashMap<>();
        complex.put("list1", Arrays.asList(new TestObject("a", 1), new TestObject("b", 2)));
        complex.put("list2", Arrays.asList(new TestObject("c", 3), new TestObject("d", 4)));
        assertEquals(complex, parser.decode(parser.encode(complex)));

        Pair<String, Integer> pair = parser.deepCopy(Pair.of("abc", 123));
        assertEquals(Pair.of("abc", 123), pair);
    }

    @Test
    public void testGlobalRegistrationInvalidatesExistingParserPool() {
        try {
            LateRegisteredObject warmed = parser.deserialize(parser.serialize(new LateRegisteredObject("warmup"), (KryoSerConfig) null), null,
                    LateRegisteredObject.class);
            assertEquals("warmup", warmed.name);

            ParserFactory.registerKryo(LateRegisteredObject.class, new LateRegisteredObjectSerializer());

            LateRegisteredObject result = parser.deserialize(parser.serialize(new LateRegisteredObject("late"), (KryoSerConfig) null), null,
                    LateRegisteredObject.class);
            assertEquals("late-registered", result.name);
        } finally {
            unregisterKryoForTest(LateRegisteredObject.class);
        }
    }

    @Test
    public void testRegisterClass() {
        parser.register(TestObject.class, new CustomSerializer(), 500);
        TestObject original = new TestObject("registered", 999);
        assertEquals(original, parser.decode(parser.encode(original)));

        assertDoesNotThrow(() -> parser.register(TestObject.class));
        assertDoesNotThrow(() -> parser.register(TestObject.class, 100));
        assertDoesNotThrow(() -> parser.register(TestObject.class, new CustomSerializer()));
        assertDoesNotThrow(() -> parser.register(TestObject.class, new CustomSerializer(), 200));
        assertThrows(IllegalArgumentException.class, () -> parser.register(TestObject.class, -1));
        assertThrows(IllegalArgumentException.class, () -> parser.register(TestObject.class, new CustomSerializer(), -1));
        assertThrows(IllegalArgumentException.class, () -> parser.register(null));
        assertThrows(IllegalArgumentException.class, () -> parser.register(TestObject.class, null));
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
        assertThrows(IllegalArgumentException.class, () -> parser.register(LateRegisteredObject.class, new LateRegisteredObjectSerializer(), occupiedId));

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
    public void testDeserializeWriteClassPayloadWithNullTarget() {
        final String encoded = parser.serialize("abc", KryoSerConfig.create().setWriteClass(true));
        assertEquals("abc", parser.deserialize(encoded, null, (Class<String>) null));
        assertEquals("abc", parser.deserialize(new ByteArrayInputStream(Strings.base64Decode(encoded)), null, (Class<String>) null));
        assertNull(parser.deserialize(parser.serialize(null, (KryoSerConfig) null), null, (Class<String>) null));
    }

    @Test
    public void testDeserializeTypedPreservesSerializerFailure() {
        final RuntimeException serializerFailure = new RuntimeException("serializer failure");
        parser.register(TestObject.class, new Serializer<TestObject>() {
            @Override
            public void write(final Kryo kryo, final Output output, final TestObject object) {
                output.writeByte(7);
            }

            @Override
            public TestObject read(final Kryo kryo, final Input input, final Class<? extends TestObject> type) {
                throw serializerFailure;
            }
        }, 501);

        final String encoded = parser.serialize(new TestObject("ignored", 1), KryoSerConfig.create());
        final RuntimeException thrown = assertThrows(RuntimeException.class, () -> parser.deserialize(encoded, null, TestObject.class));
        assertSame(serializerFailure, thrown);
        assertEquals(0, thrown.getSuppressed().length);
    }

    @Test
    public void testDeserializeStopsDrainingWhenSourceMakesNoProgress() {
        final byte[] encoded = Strings.base64Decode(parser.serialize("decoded"));
        final StalledAfterPayloadInput source = new StalledAfterPayloadInput(encoded);

        assertEquals("decoded", parser.deserialize(source, String.class));
        assertEquals(2, source.zeroReads);
        assertFalse(source.closed);
    }

    @Test
    public void testDeserializePreservesDecodeFailureWhenDrainMakesNoProgress() {
        final KryoException failure = new KryoException("decode failed");
        parser.register(TestObject.class, new Serializer<TestObject>() {
            @Override
            public void write(final Kryo kryo, final Output output, final TestObject value) {
                output.writeByte(1);
            }

            @Override
            public TestObject read(final Kryo kryo, final Input input, final Class<? extends TestObject> type) {
                input.readByte();
                throw failure;
            }
        });
        final StalledAfterPayloadInput source = new StalledAfterPayloadInput(Strings.base64Decode(parser.serialize(new TestObject())));

        assertSame(failure, assertThrows(KryoException.class, () -> parser.deserialize(source, TestObject.class)));
        assertEquals(2, source.zeroReads);
        assertFalse(source.closed);
    }

    private static final class StalledAfterPayloadInput extends ByteArrayInputStream {
        int zeroReads;
        boolean closed;

        StalledAfterPayloadInput(final byte[] payload) {
            super(payload);
        }

        @Override
        public synchronized int read(final byte[] bytes, final int offset, final int length) {
            if (length > 0 && available() == 0) {
                // Fail deterministically if the parser retries after IOUtil's two-read no-progress signal.
                assertTrue(++zeroReads <= 2, "Drain continued after two consecutive zero-length reads");
                return 0;
            }
            return super.read(bytes, offset, length);
        }

        @Override
        public void close() {
            closed = true;
        }
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
    // ---------------------------------------------------------------------------------------------
    // reviewFixes20260906: P6-02 (primitive-slot displacement), P6-03 (I/O failure -> UncheckedIOException),
    // P6-09/P6-10/P6-11 (documented contracts pinned).
    // ---------------------------------------------------------------------------------------------

    public static class ObjectHolder {
        public Object value;
        public int count;
        public String label;
    }

    public static class Node {
        public String name;
        public Node next;
    }

    public enum Color {
        RED, GREEN
    }

    private static final Class<?>[] PRIMITIVE_SLOT_TYPES = { int.class, String.class, float.class, boolean.class, byte.class, char.class, short.class,
            long.class, double.class };

    private static final Object[] WRAPPER_VALUES = { 123, "sé中", 2.5f, Boolean.TRUE, (byte) 3, 'c', (short) 7, 5L, 2.25d };

    private static void assertAllPrimitiveShapesRoundTrip(final KryoParser p) {
        for (final Object value : WRAPPER_VALUES) {
            assertEquals(value, p.decode(p.encode(value)), "class-and-object " + value.getClass().getSimpleName());
            assertEquals(value, p.deserialize(p.serialize(value, (KryoSerConfig) null), null, value.getClass()), "typed " + value.getClass().getSimpleName());
        }

        assertEquals(List.of(1, "s"), p.decode(p.encode(new ArrayList<>(List.of(1, "s")))));

        final ObjectHolder holder = new ObjectHolder();
        holder.value = 42;
        holder.count = 7;
        holder.label = "x";

        final ObjectHolder typed = p.deserialize(p.serialize(holder, (KryoSerConfig) null), null, ObjectHolder.class);
        assertEquals(42, typed.value);
        assertEquals(7, typed.count);
        assertEquals("x", typed.label);

        final ObjectHolder classBearing = p.deserialize(p.serialize(holder, KryoSerConfig.create().setWriteClass(true)), null, (Class<ObjectHolder>) null);
        assertEquals(42, classBearing.value);
        assertEquals(7, classBearing.count);
    }

    @Test
    public void reviewFixes20260906_registerAtEachPrimitiveSlotKeepsWrappersSerializable() {
        final Kryo baseline = parser.createKryo();
        final int baselineNextId;

        try {
            for (int id = 0; id < PRIMITIVE_SLOT_TYPES.length; id++) {
                assertSame(PRIMITIVE_SLOT_TYPES[id], baseline.getRegistration(id).getType(), "Kryo built-in slot " + id);
            }

            baselineNextId = baseline.getNextRegistrationId();
        } finally {
            parser.recycle(baseline);
        }

        for (int id = 0; id < PRIMITIVE_SLOT_TYPES.length; id++) {
            final KryoParser p = new KryoParser();
            p.register(BuiltInIdInstanceTarget.class, id);

            assertAllPrimitiveShapesRoundTrip(p);

            final Kryo kryo = p.createKryo();

            try {
                assertSame(BuiltInIdInstanceTarget.class, kryo.getRegistration(id).getType());
                assertEquals(id, kryo.getRegistration(BuiltInIdInstanceTarget.class).getId());

                final Class<?> displaced = PRIMITIVE_SLOT_TYPES[id];

                if (displaced.isPrimitive()) {
                    // The displaced primitive and its wrapper share one relocated, id-based registration
                    // (before the fix: both unregistered, the wrapper resolved to an implicit id -1).
                    final int relocatedId = kryo.getRegistration(displaced).getId();
                    assertEquals(baselineNextId, relocatedId, "relocated id of " + displaced);
                    assertEquals(relocatedId, kryo.getClassResolver().getRegistration(com.landawn.abacus.util.ClassUtil.wrap(displaced)).getId());
                    assertEquals(baselineNextId + 1, kryo.getNextRegistrationId());
                } else {
                    // String is not a primitive slot: it falls back to a name-based registration, nothing is relocated.
                    assertEquals(baselineNextId, kryo.getNextRegistrationId());
                }
            } finally {
                p.recycle(kryo);
            }
        }
    }

    @Test
    public void reviewFixes20260906_registerWithSerializerAtPrimitiveSlotRelocatesPrimitive() {
        parser.register(BuiltInIdInstanceTarget.class, new EmptySerializer<>(), 7);

        assertAllPrimitiveShapesRoundTrip(parser);

        final Kryo kryo = parser.createKryo();

        try {
            assertSame(BuiltInIdInstanceTarget.class, kryo.getRegistration(7).getType());
            assertTrue(kryo.getRegistration(long.class).getId() > 8);
            assertEquals(kryo.getRegistration(long.class).getId(), kryo.getClassResolver().getRegistration(Long.class).getId());
        } finally {
            parser.recycle(kryo);
        }
    }

    @Test
    public void reviewFixes20260906_globalRegisterAtPrimitiveSlotKeepsWrappersSerializable() {
        try {
            ParserFactory.registerKryo(BuiltInIdGlobalTarget.class, 0);

            final KryoParser fresh = new KryoParser();
            assertAllPrimitiveShapesRoundTrip(fresh);

            final Kryo kryo = fresh.createKryo();

            try {
                assertSame(BuiltInIdGlobalTarget.class, kryo.getRegistration(0).getType());
                assertTrue(kryo.getRegistration(int.class).getId() > 8);
            } finally {
                fresh.recycle(kryo);
            }
        } finally {
            unregisterKryoForTest(BuiltInIdGlobalTarget.class);
        }
    }

    @Test
    public void reviewFixes20260906_relocatedPrimitiveIdIsDisplaceableAgain() {
        parser.register(BuiltInIdInstanceTarget.class, 0);

        final int relocatedId;
        final Kryo first = parser.createKryo();

        try {
            relocatedId = first.getRegistration(int.class).getId();
        } finally {
            parser.recycle(first);
        }

        assertTrue(relocatedId > 8);

        // A later explicit registration at the id the primitive moved to must not be rejected as "assigned to int".
        parser.register(LateRegisteredObject.class, relocatedId);

        final Kryo second = assertDoesNotThrow(parser::createKryo);

        try {
            assertSame(LateRegisteredObject.class, second.getRegistration(relocatedId).getType());
            assertSame(BuiltInIdInstanceTarget.class, second.getRegistration(0).getType());

            final int cascadedId = second.getRegistration(int.class).getId();
            assertTrue(cascadedId > relocatedId, "int must be relocated once more, was " + cascadedId);
        } finally {
            parser.recycle(second);
        }

        assertAllPrimitiveShapesRoundTrip(parser);
    }

    @Test
    public void reviewFixes20260906_registerWrapperAtItsOwnPrimitiveSlotIsUnchanged() {
        parser.register(Integer.class, 0);

        assertEquals(123, (int) parser.decode(parser.encode(123)));
        assertEquals(123, (int) parser.deserialize(parser.serialize(123, (KryoSerConfig) null), null, Integer.class));

        final Kryo kryo = parser.createKryo();

        try {
            assertEquals(0, kryo.getRegistration(Integer.class).getId());
        } finally {
            parser.recycle(kryo);
        }
    }

    @Test
    public void reviewFixes20260906_nonPrimitiveBuiltInIdStillDisplacesWithoutRelocation() {
        final Kryo baseline = parser.createKryo();
        final int nullableId;
        final int baselineNextId;

        try {
            nullableId = baseline.getRegistration(Nullable.class).getId();
            baselineNextId = baseline.getNextRegistrationId();
        } finally {
            parser.recycle(baseline);
        }

        assertTrue(nullableId > 8);

        parser.register(BuiltInIdInstanceTarget.class, nullableId);

        assertEquals(Nullable.of(3), parser.decode(parser.encode(Nullable.of(3))));
        assertEquals(123, (int) parser.decode(parser.encode(123)));

        final Kryo kryo = parser.createKryo();

        try {
            assertSame(BuiltInIdInstanceTarget.class, kryo.getRegistration(nullableId).getType());
            assertEquals(baselineNextId, kryo.getNextRegistrationId());
            assertEquals(-1, kryo.getRegistration(Nullable.class).getId());
        } finally {
            parser.recycle(kryo);
        }
    }

    @Test
    public void testRegisterClassWithIdCanReplaceBuiltInRegistrationAndStillSerializesIntegers() {
        parser.register(BuiltInIdInstanceTarget.class, 0);

        assertEquals(123, (int) parser.decode(parser.encode(123)));
        assertEquals(List.of(1, "s"), parser.decode(parser.encode(new ArrayList<>(List.of(1, "s")))));
    }

    private static final class FailingOutputStream extends OutputStream {
        private final IOException failure;
        private final boolean failOnWrite;
        private final boolean failOnFlush;

        FailingOutputStream(final IOException failure, final boolean failOnWrite, final boolean failOnFlush) {
            this.failure = failure;
            this.failOnWrite = failOnWrite;
            this.failOnFlush = failOnFlush;
        }

        @Override
        public void write(final int b) throws IOException {
            if (failOnWrite) {
                throw failure;
            }
        }

        @Override
        public void flush() throws IOException {
            if (failOnFlush) {
                throw failure;
            }
        }
    }

    @Test
    public void reviewFixes20260906_serializeToFailingOutputStreamThrowsUncheckedIOException() throws IOException {
        final IOException disk = new IOException("disk full");
        final TestObject obj = new TestObject("io", 1);

        UncheckedIOException thrown = assertThrows(UncheckedIOException.class, () -> parser.serialize(obj, null, new FailingOutputStream(disk, true, true)));
        assertSame(disk, thrown.getCause());

        thrown = assertThrows(UncheckedIOException.class, () -> parser.serialize(obj, null, new FailingOutputStream(disk, false, true)));
        assertSame(disk, thrown.getCause());

        // A payload larger than Kryo's 8192-byte Output buffer fails inside require(), i.e. mid-write.
        final byte[] big = new byte[20_000];
        thrown = assertThrows(UncheckedIOException.class, () -> parser.serialize(big, null, new FailingOutputStream(disk, true, false)));
        assertSame(disk, thrown.getCause());

        thrown = assertThrows(UncheckedIOException.class, () -> parser.serialize(null, null, new FailingOutputStream(disk, true, true)));
        assertSame(disk, thrown.getCause());

        thrown = assertThrows(UncheckedIOException.class,
                () -> parser.serialize(obj, KryoSerConfig.create().setWriteClass(true), new FailingOutputStream(disk, true, true)));
        assertSame(disk, thrown.getCause());

        final File file = tempDir.resolve("closed.kryo").toFile();
        final FileOutputStream closed = new FileOutputStream(file);
        closed.close();
        thrown = assertThrows(UncheckedIOException.class, () -> parser.serialize(obj, null, closed));
        assertTrue(thrown.getCause() instanceof IOException);

        // The pooled Output must be clean afterwards: the next write is byte-identical to a fresh parser's.
        assertArrayEquals(new KryoParser().encode(obj), parser.encode(obj));
        assertEquals(obj, parser.deserialize(parser.serialize(obj, (KryoSerConfig) null), null, TestObject.class));
    }

    @Test
    public void reviewFixes20260906_serializerFailureWithoutIoCauseStaysKryoException() {
        final KryoException failure = new KryoException("custom serializer failure");

        parser.register(TestObject.class, new Serializer<TestObject>() {
            @Override
            public void write(final Kryo kryo, final Output output, final TestObject object) {
                throw failure;
            }

            @Override
            public TestObject read(final Kryo kryo, final Input input, final Class<? extends TestObject> type) {
                return null;
            }
        }, 502);

        final KryoException thrown = assertThrows(KryoException.class, () -> parser.serialize(new TestObject("k", 1), null, new ByteArrayOutputStream()));
        assertSame(failure, thrown);
        assertFalse(UncheckedIOException.class.isInstance(thrown));

        // A truncated payload is a KryoException subtype, not an I/O failure.
        final byte[] encoded = new KryoParser().encode(new TestObject("t", 2));
        assertThrows(KryoException.class, () -> new KryoParser().decode(Arrays.copyOf(encoded, encoded.length / 2)));
    }

    @Test
    public void reviewFixes20260906_copyAndEncodeRequireNoArgConstructor() {
        assertThrows(KryoException.class, () -> parser.deepCopy(ImmutableList.of(1, 2)));
        assertThrows(KryoException.class, () -> parser.deepCopy(Collections.unmodifiableList(new ArrayList<>(List.of(1, 2)))));
        assertThrows(KryoException.class, () -> parser.decode(parser.encode(ImmutableList.of(1, 2))));

        final EnumMap<Color, Integer> enumMap = new EnumMap<>(Color.class);
        enumMap.put(Color.RED, 1);
        assertThrows(KryoException.class, () -> parser.deepCopy(enumMap));

        final TreeMap<String, Integer> reversed = new TreeMap<>(Comparator.reverseOrder());
        reversed.put("a", 1);
        reversed.put("b", 2);
        // Opening java.util lets Kryo invoke the comparator singleton's private no-argument constructor.
        if (Object.class.getModule().isOpen("java.util", KryoParser.class.getModule())) {
            final TreeMap<String, Integer> decoded = parser.decode(parser.encode(reversed));
            assertEquals(reversed, decoded);
            assertEquals(List.of("b", "a"), new ArrayList<>(decoded.keySet()));
        } else {
            assertThrows(KryoException.class, () -> parser.decode(parser.encode(reversed)));
        }

        // Copying does not go through the wire format: the comparator singleton is shared with the copy.
        final TreeMap<String, Integer> copied = parser.deepCopy(reversed);
        assertEquals(reversed, copied);
        assertSame(reversed.comparator(), copied.comparator());
        assertEquals(List.of("b", "a"), new ArrayList<>(copied.keySet()));

        assertEquals(List.of(1, 2), parser.deepCopy(List.of(1, 2)));
        assertEquals(List.of(1, 2), parser.decode(parser.encode(List.of(1, 2))));
        assertEquals(Arrays.asList(1, 2), parser.deepCopy(Arrays.asList(1, 2)));
        assertEquals(Collections.emptyList(), parser.decode(parser.encode(Collections.emptyList())));
    }

    @Test
    public void reviewFixes20260906_serializeDoesNotTrackReferencesButCopyDoes() {
        final Node a = new Node();
        final Node b = new Node();
        a.name = "a";
        b.name = "b";
        a.next = b;
        b.next = a;

        assertThrows(KryoException.class, () -> parser.serialize(a, (KryoSerConfig) null));
        assertThrows(KryoException.class, () -> parser.encode(a));

        final Node copiedA = parser.deepCopy(a);
        assertNotSame(a, copiedA);
        assertEquals("b", copiedA.next.name);
        assertSame(copiedA, copiedA.next.next);

        final Node shared = new Node();
        shared.name = "shared";
        final List<Node> twice = new ArrayList<>(List.of(shared, shared));

        final List<Node> decoded = parser.decode(parser.encode(twice));
        assertEquals(2, decoded.size());
        assertEquals("shared", decoded.get(0).name);
        assertEquals("shared", decoded.get(1).name);
        assertNotSame(decoded.get(0), decoded.get(1));

        final List<Node> typed = parser.deserialize(parser.serialize(twice, (KryoSerConfig) null), null, ArrayList.class);
        assertNotSame(typed.get(0), typed.get(1));

        final List<Node> copied = parser.deepCopy(twice);
        assertNotSame(shared, copied.get(0));
        assertSame(copied.get(0), copied.get(1));
    }

    @Test
    public void reviewFixes20260906_emptyPayloadsFailWithKryoException() {
        assertThrows(KryoException.class, () -> parser.deserialize("", null, TestObject.class));
        assertThrows(KryoException.class, () -> parser.deserialize("", null, (Class<TestObject>) null));
        assertThrows(KryoException.class, () -> parser.deserialize(new ByteArrayInputStream(new byte[0]), null, TestObject.class));
        assertThrows(KryoException.class, () -> parser.deserialize(new StringReader(""), null, TestObject.class));
        assertThrows(KryoException.class, () -> parser.decode(new byte[0]));
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize((String) null, null, TestObject.class));
    }

    @Test
    public void testEncodeReverseOrderTreeMapDependsOnJavaUtilBeingOpened() {
        final KryoParser kryoParser = ParserFactory.createKryoParser();
        final TreeMap<String, Integer> reversed = new TreeMap<>(Comparator.reverseOrder());
        reversed.put("a", 1);
        reversed.put("b", 2);

        // Copying never needs to instantiate the comparator singleton.
        assertEquals(List.of("b", "a"), new ArrayList<>(kryoParser.deepCopy(reversed).keySet()));

        if (Object.class.getModule().isOpen("java.util", KryoParser.class.getModule())) {
            // --add-opens java.base/java.util: the singleton's private constructor is accessible to Kryo.
            final TreeMap<String, Integer> decoded = kryoParser.decode(kryoParser.encode(reversed));
            assertEquals(List.of("b", "a"), new ArrayList<>(decoded.keySet()));
        } else {
            assertThrows(KryoException.class, () -> kryoParser.decode(kryoParser.encode(reversed)));
        }
    }

    // ---- perf review 2026-09-26 G008 begin ----
    public static class G008IdTargetA {
        public int value;
    }

    public static class G008IdTargetB {
        public int value;
    }

    public static class G008IdTargetC {
        public int value;
    }

    // G008-01: the merged-ID check result is reused, but a later global conflict is still reported on every call until removed
    @Test
    public void testCreateKryo_mergedIdCheckReusedButLateGlobalConflictStillReported() {
        final int conflictingId = 1_926_008;
        final int otherId = 1_926_009;
        parser.register(G008IdTargetA.class, conflictingId);

        final Kryo first = parser.createKryo();
        parser.recycle(first);
        final Kryo second = parser.createKryo();
        parser.recycle(second);

        try {
            ParserFactory.registerKryo(G008IdTargetB.class, conflictingId);
            assertThrows(IllegalArgumentException.class, parser::createKryo);
            assertThrows(IllegalArgumentException.class, parser::createKryo);
            assertThrows(IllegalArgumentException.class, () -> parser.serialize(new G008IdTargetA(), (KryoSerConfig) null));
        } finally {
            unregisterKryoForTest(G008IdTargetB.class);
        }

        final Kryo afterRemoval = parser.createKryo();
        try {
            assertEquals(conflictingId, afterRemoval.getRegistration(G008IdTargetA.class).getId());
        } finally {
            parser.recycle(afterRemoval);
        }

        parser.register(G008IdTargetC.class, otherId);
        final Kryo afterInstanceRegistration = parser.createKryo();
        try {
            assertEquals(conflictingId, afterInstanceRegistration.getRegistration(G008IdTargetA.class).getId());
            assertEquals(otherId, afterInstanceRegistration.getRegistration(G008IdTargetC.class).getId());
        } finally {
            parser.recycle(afterInstanceRegistration);
        }

        assertThrows(IllegalArgumentException.class, () -> parser.register(G008IdTargetB.class, otherId));

        final G008IdTargetA source = new G008IdTargetA();
        source.value = 42;
        assertEquals(42, parser.deserialize(parser.serialize(source, (KryoSerConfig) null), null, G008IdTargetA.class).value);
    }

    // G008-02: a recycled Input must not keep the caller's payload array reachable from the static pool
    @Test
    public void testRecycleInput_releasesPayloadBuffer() {
        final byte[] encoded = parser.encode("g008-payload");
        assertEquals("g008-payload", parser.decode(encoded));

        final Input pooled = KryoParser.createInput();
        try {
            assertNotSame(encoded, pooled.getBuffer());
            assertEquals(0, pooled.limit());
            assertEquals(0, pooled.position());
            assertNull(pooled.getInputStream());
        } finally {
            KryoParser.recycle(pooled);
        }
    }

    // G008-02: pooled Input instances keep decoding correctly across small and large payloads
    @Test
    public void testRecycleInput_reuseAcrossPayloadSizes() {
        final String small = "abc";
        final String large = Strings.repeat('x', 20_000);

        for (int i = 0; i < 3; i++) {
            assertEquals(small, parser.decode(parser.encode(small)));
            assertEquals(large, parser.decode(parser.encode(large)));
            assertEquals(small, parser.deserialize(parser.serialize(small), null, String.class));
            assertEquals(large, parser.deserialize(new ByteArrayInputStream(parser.encode(large)), null, (Class<String>) null));
            assertEquals(Integer.valueOf(7), parser.deserialize(parser.serialize(7), null, Integer.class));
        }

        assertThrows(KryoException.class, () -> parser.decode(N.EMPTY_BYTE_ARRAY));
        assertEquals(small, parser.decode(parser.encode(small)));
    }
    // ---- perf review 2026-09-26 G008 end ----

    // ---- bug review 2026-10-02 G008 begin ----
    // G008-01: String re-registered at an explicit id fell back to Kryo's FieldSerializer (String internals) instead of StringSerializer
    @Test
    public void testRegister_stringAtExplicitIdKeepsStringSerializer() {
        final String baselinePayload = new KryoParser().serialize("abc");
        parser.register(String.class, 1_920_101);

        final Kryo kryo = parser.createKryo();

        try {
            assertEquals(1_920_101, kryo.getRegistration(String.class).getId());
            assertTrue(kryo.getRegistration(String.class).getSerializer() instanceof com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer);
        } finally {
            parser.recycle(kryo);
        }

        assertEquals(baselinePayload, parser.serialize("abc"));
        assertEquals("abc", parser.deserialize(parser.serialize("abc"), null, String.class));

        final String text = "copy me";
        assertSame(text, parser.deepCopy(text));
    }

    // G008-01: a class registered at String's built-in id 1 leaves String name-based; that registration fell back to FieldSerializer too
    @Test
    public void testRegister_classAtStringIdKeepsStringSerializerForNameBasedString() {
        final String baselinePayload = new KryoParser().serialize("abc");
        parser.register(BuiltInIdInstanceTarget.class, 1);

        assertEquals(baselinePayload, parser.serialize("abc"));
        assertEquals(List.of("x", "y"), parser.decode(parser.encode(new ArrayList<>(List.of("x", "y")))));

        final String text = "copy me";
        assertSame(text, parser.deepCopy(text));

        final Kryo kryo = parser.createKryo();

        try {
            assertSame(BuiltInIdInstanceTarget.class, kryo.getRegistration(1).getType());
            assertTrue(kryo.getRegistration(String.class).getSerializer() instanceof com.esotericsoftware.kryo.serializers.DefaultSerializers.StringSerializer);
        } finally {
            parser.recycle(kryo);
        }
    }
    // ---- bug review 2026-10-02 G008 end ----

    // ---- perf review 2026-10-07 G13 begin ----
    private static final class G13RecordingWriter extends java.io.Writer {
        // A StringBuilder, not a StringWriter: a StackOverflowError raised by an earlier cyclic-graph test can leave
        // java.lang.StringBuffer uninitializable for the rest of a shared test JVM.
        final StringBuilder text = new StringBuilder();
        final StringBuilder calls = new StringBuilder();
        final boolean fail;
        int flushes;
        int closes;

        G13RecordingWriter(final boolean fail) {
            this.fail = fail;
        }

        @Override
        public void write(final char[] cbuf, final int off, final int len) throws IOException {
            if (fail) {
                throw new IOException("g13 writer failure");
            }

            calls.append('a').append(len).append(' ');
            text.append(cbuf, off, len);
        }

        @Override
        public void write(final int c) throws IOException {
            if (fail) {
                throw new IOException("g13 writer failure");
            }

            calls.append("c ");
            text.append((char) c);
        }

        @Override
        public void write(final String str, final int off, final int len) throws IOException {
            if (fail) {
                throw new IOException("g13 writer failure");
            }

            calls.append('s').append(len).append(' ');
            text.append(str, off, off + len);
        }

        @Override
        public void flush() {
            flushes++;
        }

        @Override
        public void close() {
            closes++;
        }
    }

    private static byte[] g13Payload(final int size) {
        final byte[] bytes = new byte[size];

        for (int i = 0; i < size; i++) {
            bytes[i] = (byte) (i * 131 + (i >>> 5) + 7);
        }

        return bytes;
    }

    // G13-01: writeBase64 emits the JDK encoder's text, chunked as before: 8124-character chunks of complete groups, then the padded group char by char
    @Test
    public void testWriteBase64_textAndWriterCallsAcrossChunkBoundaries() throws IOException {
        final int[] sizes = { 0, 1, 2, 3, 100, 1023, 1024, 1025, 8189, 8190, 8191, 8192, 8193, 8194, 8195, 12186, 12187, 12188, 12189, 12190, 12191, 12192,
                12193, 12194, 16384, 16385, 16386, 24370, 24372, 24373, 24375, 24376, 24378, 65536, 65537, 65538, 100000, 100001, 100002 };

        for (final int size : sizes) {
            final byte[] bytes = g13Payload(size);
            final com.landawn.abacus.util.ByteArrayOutputStream source = new com.landawn.abacus.util.ByteArrayOutputStream(size + 17);
            source.write(bytes, 0, size);

            final String expected = java.util.Base64.getEncoder().encodeToString(bytes);
            final StringBuilder expectedCalls = new StringBuilder();

            if (size < 8192) {
                expectedCalls.append('s').append(expected.length()).append(' ');
            } else {
                int groups = size / 3;

                while (groups > 0) {
                    final int chunkGroups = Math.min(groups, 2031);
                    expectedCalls.append('a').append(chunkGroups * 4).append(' ');
                    groups -= chunkGroups;
                }

                if (size % 3 != 0) {
                    expectedCalls.append("c c c c ");
                }
            }

            final G13RecordingWriter output = new G13RecordingWriter(false);
            BinaryParserSupport.writeBase64(source, output);

            assertEquals(expected, output.text.toString(), "size " + size);
            assertEquals(expectedCalls.toString(), output.calls.toString(), "size " + size);
            assertEquals(0, output.flushes, "size " + size);
            assertEquals(0, output.closes, "size " + size);
            assertEquals(expected, BinaryParserSupport.encodeBase64(source), "size " + size);
        }
    }

    // G13-01: the Writer overload produces the String overload's text on both sides of the streaming threshold, flushes once and never closes
    @Test
    public void testSerializeWriter_matchesStringOverloadAroundStreamingThreshold() {
        final Object[] values = { new byte[0], g13Payload(100), g13Payload(8_000), g13Payload(8_189), g13Payload(8_190), g13Payload(8_191), g13Payload(65_536),
                Strings.repeat('w', 30_000), new TestObject("g13", 13) };

        for (final Object value : values) {
            final String expected = parser.serialize(value);
            final G13RecordingWriter output = new G13RecordingWriter(false);
            parser.serialize(value, null, output);

            assertEquals(expected, output.text.toString());
            assertEquals(1, output.flushes);
            assertEquals(0, output.closes);

            final Object restored = parser.deserialize(new StringReader(output.text.toString()), null, value.getClass());

            if (value instanceof byte[] bytes) {
                assertArrayEquals(bytes, (byte[]) restored);
            } else {
                assertEquals(value, restored);
            }
        }
    }

    // G13-01: a failing Writer still surfaces UncheckedIOException with the IOException cause, without a flush or close, on both paths
    @Test
    public void testSerializeWriter_failurePropagatesWithoutFlushOrClose() {
        for (final int size : new int[] { 10, 8_192, 100_000 }) {
            final byte[] value = g13Payload(size);
            final G13RecordingWriter output = new G13RecordingWriter(true);

            final UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> parser.serialize(value, null, output));

            assertTrue(e.getCause() instanceof IOException);
            assertEquals("g13 writer failure", e.getCause().getMessage());
            assertEquals(0, output.flushes);
            assertEquals(0, output.closes);
            assertEquals(0, output.text.length());
        }
    }
    // ---- perf review 2026-10-07 G13 end ----

    // ---- perf review 2026-10-07 round 2 R2C begin ----

    /** R2C-01: a Writer that fails on one write call and records every call made to it, before and after that failure. */
    private static final class R2FailOnceWriter extends java.io.Writer {
        final int failAtCall;
        int callCount;
        int callsAfterFailure;
        boolean failed;
        int flushes;
        int closes;

        R2FailOnceWriter(final int failAtCall) {
            this.failAtCall = failAtCall;
        }

        private void record() throws IOException {
            callCount++;

            if (failed) {
                callsAfterFailure++;
            } else if (callCount == failAtCall) {
                failed = true;
                throw new IOException("r2 writer failure at call " + callCount);
            }
        }

        @Override
        public void write(final char[] cbuf, final int off, final int len) throws IOException {
            record();
        }

        @Override
        public void write(final int c) throws IOException {
            record();
        }

        @Override
        public void write(final String str, final int off, final int len) throws IOException {
            record();
        }

        @Override
        public void flush() {
            flushes++;
        }

        @Override
        public void close() {
            closes++;
        }
    }

    // R2C-01: a Writer that fails part-way through a large Base64 payload (on a middle chunk, on the last chunk, or on the
    // first character of the padded tail) gets nothing written after the failing call, the failure is the exception the
    // caller sees, and no close failure is attached to it as a suppressed exception (the JDK streaming encoder used until
    // r61 finished its pending padded group in close(), after the Writer had already failed, and attached that second
    // failure). Chunks are 2,031 complete 3-byte groups = 8,124 characters; the tail is written character by character.
    @Test
    public void testSerializeWriter_failureMidWayWritesNothingMoreAndAddsNoSuppressed() {
        for (final int size : new int[] { 100_000, 99_999, 99_998 }) {
            final byte[] value = g13Payload(size);
            final int groups = size / 3;
            final int chunkWrites = (groups + 2030) / 2031;
            final int tailWrites = size % 3 == 0 ? 0 : 4;
            final int[] failAtCalls = tailWrites == 0 ? new int[] { 2, chunkWrites } : new int[] { 2, chunkWrites, chunkWrites + 1, chunkWrites + tailWrites };

            for (final int failAt : failAtCalls) {
                final R2FailOnceWriter output = new R2FailOnceWriter(failAt);

                final UncheckedIOException e = assertThrows(UncheckedIOException.class, () -> parser.serialize(value, null, output), size + "/" + failAt);

                assertEquals("r2 writer failure at call " + failAt, e.getCause().getMessage(), size + "/" + failAt);
                assertEquals(0, e.getSuppressed().length, size + "/" + failAt);
                assertEquals(0, e.getCause().getSuppressed().length, size + "/" + failAt);
                assertEquals(failAt, output.callCount, "no write after the failing call: " + size + "/" + failAt);
                assertEquals(0, output.callsAfterFailure, size + "/" + failAt);
                assertEquals(0, output.flushes, size + "/" + failAt);
                assertEquals(0, output.closes, size + "/" + failAt);
            }

            // control: the same payload through a Writer that never fails receives exactly chunkWrites + tailWrites calls
            final R2FailOnceWriter sink = new R2FailOnceWriter(Integer.MAX_VALUE);
            parser.serialize(value, null, sink);
            assertEquals(chunkWrites + tailWrites, sink.callCount, String.valueOf(size));
            assertEquals(1, sink.flushes, String.valueOf(size));
            assertEquals(0, sink.closes, String.valueOf(size));
        }
    }

    // ---- perf review 2026-10-07 round 2 R2C end ----
}
