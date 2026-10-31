package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.avro.Schema;
import org.apache.avro.specific.SpecificRecord;
import org.apache.avro.util.ClassSecurityValidator;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.entity.User;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Suppliers;

class ParserBinaryPerformance20261030Test extends TestBase {
    @Test
    void failedCopiesReleaseBothIdleAndOwnershipReferences() throws Exception {
        for (final boolean shallow : new boolean[] { false, true }) {
            final KryoParser parser = new KryoParser();
            final NoDefaultConstructor source = new NoDefaultConstructor(1024 * 1024);
            assertThrows(KryoException.class, () -> {
                if (shallow) {
                    parser.shallowCopy(source);
                } else {
                    parser.deepCopy(source);
                }
            });
            assertTrue(((Collection<?>) field(parser, "kryoPool")).isEmpty());
            assertTrue(((Map<?, ?>) field(parser, "xPool")).isEmpty(), "Ownership must not retain a discarded Kryo and its failed source graph");
            assertEquals("next", parser.deepCopy("next"));
            assertEquals(1, ((Map<?, ?>) field(parser, "xPool")).size());
        }
    }

    @Test
    void nestedCopyErrorsAlsoDiscardTheFailedKryo() throws Exception {
        final KryoParser parser = new KryoParser();
        final AssertionError failure = new AssertionError("copy failure");
        parser.register(NoDefaultConstructor.class, new Serializer<NoDefaultConstructor>() {
            @Override
            public void write(final Kryo kryo, final Output output, final NoDefaultConstructor value) {
                throw new UnsupportedOperationException();
            }

            @Override
            public NoDefaultConstructor read(final Kryo kryo, final Input input, final Class<? extends NoDefaultConstructor> target) {
                throw new UnsupportedOperationException();
            }

            @Override
            public NoDefaultConstructor copy(final Kryo kryo, final NoDefaultConstructor source) {
                throw failure;
            }
        });
        final List<Object> source = new ArrayList<>();
        source.add(new NoDefaultConstructor(1024));
        assertSame(failure, assertThrows(AssertionError.class, () -> parser.deepCopy(source)));
        assertTrue(((Collection<?>) field(parser, "kryoPool")).isEmpty());
        assertTrue(((Map<?, ?>) field(parser, "xPool")).isEmpty());
    }

    @Test
    void successfulCopiesStillReuseThePoolAndPreserveCycles() throws Exception {
        final KryoParser parser = new KryoParser();
        final CopyNode source = new CopyNode();
        source.next = source;
        final CopyNode deep = parser.deepCopy(source);
        assertNotSame(source, deep);
        assertSame(deep, deep.next);
        final Object firstKryo = ((List<?>) field(parser, "kryoPool")).getFirst();
        final CopyNode shallow = parser.shallowCopy(source);
        assertNotSame(source, shallow);
        assertSame(source, shallow.next);
        assertSame(firstKryo, ((List<?>) field(parser, "kryoPool")).getFirst());
        assertEquals(1, ((Map<?, ?>) field(parser, "xPool")).size());
    }

    @Test
    void avroCopyOnWriteArraysKeepClassOrderNullsAndNestedTypes() {
        final AvroParser parser = new AvroParser();
        final Schema array = Schema.createArray(Schema.createUnion(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.STRING)));
        final Type<CopyOnWriteArrayList<String>> target = Type.of("java.util.concurrent.CopyOnWriteArrayList<String>");
        final List<String> values = Arrays.asList("first", null, "first", "last");
        for (final List<String> input : List.of(values, List.<String> of())) {
            final String encoded = parser.serialize(input, AvroSerConfig.create().setSchema(array));
            final CopyOnWriteArrayList<String> result = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(array), target);
            assertSame(CopyOnWriteArrayList.class, result.getClass());
            assertEquals(input, result);
            result.add("after");
            assertEquals(input.size() + 1, result.size());
        }

        final Schema mapSchema = Schema.createMap(array);
        final String encodedMap = parser.serialize(Map.of("items", values), AvroSerConfig.create().setSchema(mapSchema));
        final Type<Map<String, CopyOnWriteArrayList<String>>> mapType = Type.of("Map<String, java.util.concurrent.CopyOnWriteArrayList<String>>");
        final Map<String, CopyOnWriteArrayList<String>> map = parser.deserialize(encodedMap, AvroDeserConfig.create().setSchema(mapSchema), mapType);
        assertSame(CopyOnWriteArrayList.class, map.get("items").getClass());
        assertEquals(values, map.get("items"));

        final Schema beanSchema = Schema.createRecord("CowBean", null, null, false);
        beanSchema.setFields(List.of(new Schema.Field("items", array)));
        final String encodedBean = parser.serialize(Map.of("items", values), AvroSerConfig.create().setSchema(beanSchema));
        final CowBean bean = parser.deserialize(encodedBean, AvroDeserConfig.create().setSchema(beanSchema), CowBean.class);
        assertSame(CopyOnWriteArrayList.class, bean.items.getClass());
        assertEquals(values, bean.items);
    }

    @Test
    void avroRecordSequencesAndSpecificRecordArraysFinishAsCopyOnWriteLists() {
        final AvroParser parser = new AvroParser();
        final Schema schema = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"Entry\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}");
        final Type<CopyOnWriteArrayList<Map<String, String>>> target = Type.of("java.util.concurrent.CopyOnWriteArrayList<Map<String, String>>");
        for (final List<Map<String, String>> values : List.of(List.of(Map.of("name", "one"), Map.of("name", "two")), List.<Map<String, String>> of())) {
            final String encoded = parser.serialize(values, AvroSerConfig.create().setSchema(schema));
            final CopyOnWriteArrayList<Map<String, String>> result = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(schema), target);
            assertSame(CopyOnWriteArrayList.class, result.getClass());
            assertEquals(values, result);
        }

        final User user = new User("name", 7, "blue");
        final Type<CopyOnWriteArrayList<User>> specificTarget = Type.of("java.util.concurrent.CopyOnWriteArrayList<" + User.class.getName() + ">");
        final CopyOnWriteArrayList<User> records = parser.deserialize(parser.serialize(List.of(user)), null, specificTarget);
        assertSame(CopyOnWriteArrayList.class, records.getClass());
        assertEquals(List.of(user), records);

        final String emptyRecords = parser.serialize(List.of(), AvroSerConfig.create().setSchema(User.getClassSchema()));
        final CopyOnWriteArrayList<User> empty = parser.deserialize(emptyRecords, null, specificTarget);
        assertSame(CopyOnWriteArrayList.class, empty.getClass());
        assertTrue(empty.isEmpty());

        final Schema array = Schema.createArray(Schema.createUnion(Schema.create(Schema.Type.NULL), User.getClassSchema()));
        final List<User> values = Arrays.asList(null, user, null);
        final CopyOnWriteArrayList<User> result = parser.deserialize(parser.serialize(values, AvroSerConfig.create().setSchema(array)), null, specificTarget);
        assertSame(CopyOnWriteArrayList.class, result.getClass());
        assertEquals(values, result);
    }

    @Test
    @SuppressWarnings({ "rawtypes", "unchecked" })
    void avroCopyOnWriteSubclassesKeepInsertionAndRegisteredFactories() throws Exception {
        final AvroParser parser = new AvroParser();
        final Schema schema = Schema.createArray(Schema.create(Schema.Type.STRING));
        final String encoded = parser.serialize(List.of("one", "two"), AvroSerConfig.create().setSchema(schema));
        final AvroDeserConfig config = AvroDeserConfig.create().setSchema(schema).setElementType(String.class);
        final CountingCow result = parser.deserialize(encoded, config, CountingCow.class);
        assertEquals(List.of("one", "two"), result);
        assertEquals(2, result.insertions);

        // Supplier registration requires a class outside the library's built-in namespace.
        final Class target = new FactoryLoader().collectionClass();
        final AtomicInteger calls = new AtomicInteger();
        assertTrue(Suppliers.registerForCollection(target, () -> {
            calls.incrementAndGet();
            try {
                return (Collection) target.getConstructor(String.class).newInstance("registered");
            } catch (final ReflectiveOperationException e) {
                throw new AssertionError(e);
            }
        }));
        final FactoryCow factoryResult = (FactoryCow) parser.deserialize(encoded, config, target);
        assertEquals("registered", factoryResult.marker);
        assertEquals(List.of("one", "two"), factoryResult);
        assertEquals(2, factoryResult.insertions);
        assertEquals(1, calls.get());
    }

    @Test
    @Tag("slow-test")
    void avroCopyOnWriteAllocationDoesNotGrowQuadratically() {
        final com.sun.management.ThreadMXBean allocation = allocationCounter();
        final AvroParser parser = new AvroParser();
        final Schema schema = Schema.createArray(Schema.create(Schema.Type.INT));
        final AvroDeserConfig config = AvroDeserConfig.create().setSchema(schema).setElementType(Integer.class);
        final List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 8_000; i++) {
            values.add(i);
        }
        final ByteArrayOutputStream out = new ByteArrayOutputStream();
        parser.serialize(values, AvroSerConfig.create().setSchema(schema), out);
        final byte[] bytes = out.toByteArray();
        parser.deserialize(new ByteArrayInputStream(bytes), config, CopyOnWriteArrayList.class);
        final long before = allocation.getCurrentThreadAllocatedBytes();
        final CopyOnWriteArrayList<?> result = parser.deserialize(new ByteArrayInputStream(bytes), config, CopyOnWriteArrayList.class);
        final long allocated = allocation.getCurrentThreadAllocatedBytes() - before;
        assertEquals(values, result);
        // Generous allocation budget, independent of elapsed time; repeated add previously allocated over 128 MB.
        assertTrue(allocated < 8_000_000, "Unexpected quadratic allocation: " + allocated);
    }

    @Test
    void avroTrustRespectsPreviousPredicateAndExternalReplacement() throws Exception {
        final ClassSecurityValidator.ClassSecurityPredicate saved = ClassSecurityValidator.getGlobal();
        final AtomicBoolean failPrevious = new AtomicBoolean();
        final IllegalStateException failure = new IllegalStateException("external policy failure");
        try {
            ClassSecurityValidator.setGlobal(cls -> {
                if (failPrevious.get()) {
                    throw failure;
                }
                return cls == String.class;
            });
            trust(User.class);
            trust(SecondSpecificRecord.class);
            assertTrue(ClassSecurityValidator.getGlobal().isTrusted(String.class));
            assertTrue(ClassSecurityValidator.getGlobal().isTrusted(User.class));
            assertTrue(ClassSecurityValidator.getGlobal().isTrusted(SecondSpecificRecord.class));
            failPrevious.set(true);
            assertSame(failure, assertThrows(IllegalStateException.class, () -> ClassSecurityValidator.getGlobal().isTrusted(User.class)));

            ClassSecurityValidator.setGlobal(cls -> cls == Integer.class);
            trust(User.class);
            assertTrue(ClassSecurityValidator.getGlobal().isTrusted(Integer.class));
            assertTrue(ClassSecurityValidator.getGlobal().isTrusted(User.class));
            assertFalse(ClassSecurityValidator.getGlobal().isTrusted(String.class));
            assertFalse(ClassSecurityValidator.getGlobal().isTrusted(SecondSpecificRecord.class), "An external replacement revokes the old additions");
        } finally {
            ClassSecurityValidator.setGlobal(saved);
        }
    }

    @Test
    @Tag("slow-test")
    @SuppressWarnings("deprecation")
    void avroTrustLookupAllocationStaysBoundedAsClassesAccumulate() throws Exception {
        final com.sun.management.ThreadMXBean allocation = allocationCounter();
        final ClassSecurityValidator.ClassSecurityPredicate saved = ClassSecurityValidator.getGlobal();
        try {
            ClassSecurityValidator.setGlobal(cls -> false);
            trust(User.class);
            for (int i = 0; i < 128; i++) {
                final Class<?> record = Proxy.getProxyClass(new ClassLoader(SpecificRecord.class.getClassLoader()) {
                }, SpecificRecord.class);
                trust(record);
            }
            final ClassSecurityValidator.ClassSecurityPredicate predicate = ClassSecurityValidator.getGlobal();
            for (int i = 0; i < 100; i++) {
                assertTrue(predicate.isTrusted(User.class));
            }
            final long before = allocation.getCurrentThreadAllocatedBytes();
            boolean trusted = true;
            for (int i = 0; i < 1_000; i++) {
                trusted &= predicate.isTrusted(User.class);
            }
            final long allocated = allocation.getCurrentThreadAllocatedBytes() - before;
            assertTrue(trusted);
            // The recursive composite chain allocated well over 10 MB for these 1,000 checks.
            assertTrue(allocated < 1_000_000, "Trust lookups still allocate in proportion to accumulated classes: " + allocated);
        } finally {
            ClassSecurityValidator.setGlobal(saved);
        }
    }

    private static Object field(final KryoParser parser, final String name) throws Exception {
        final Field field = KryoParser.class.getDeclaredField(name);
        field.setAccessible(true);
        return field.get(parser);
    }

    private static void trust(final Class<?> type) throws Exception {
        final Method method = AvroParser.class.getDeclaredMethod("trustSpecificRecordClass", Class.class);
        method.setAccessible(true);
        method.invoke(null, type);
    }

    private static com.sun.management.ThreadMXBean allocationCounter() {
        final var bean = ManagementFactory.getThreadMXBean();
        assumeTrue(bean instanceof com.sun.management.ThreadMXBean);
        final var allocation = (com.sun.management.ThreadMXBean) bean;
        assumeTrue(allocation.isThreadAllocatedMemorySupported() && allocation.isThreadAllocatedMemoryEnabled());
        return allocation;
    }

    public static final class NoDefaultConstructor {
        public final byte[] payload;

        public NoDefaultConstructor(final int size) {
            payload = new byte[size];
        }
    }

    public static final class CopyNode {
        public CopyNode next;
    }

    public static final class CowBean {
        public CopyOnWriteArrayList<String> items;
    }

    public static class CountingCow extends CopyOnWriteArrayList<Object> {
        public int insertions;

        @Override
        public boolean add(final Object value) {
            insertions++;
            return super.add(value);
        }
    }

    public static class FactoryCow extends CountingCow {
        public final String marker;

        public FactoryCow(final String marker) {
            this.marker = marker;
        }
    }

    public static final class SecondSpecificRecord implements SpecificRecord {
        @Override
        public Schema getSchema() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object get(final int field) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void put(final int field, final Object value) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FactoryLoader extends ClassLoader {
        Class<?> collectionClass() {
            final String name = "testfixtures/AvroFactoryCow20261030";
            final String parent = FactoryCow.class.getName().replace('.', '/');
            final ClassWriter writer = new ClassWriter(0);
            writer.visit(Opcodes.V21, Opcodes.ACC_PUBLIC, name, null, parent, null);
            final MethodVisitor constructor = writer.visitMethod(Opcodes.ACC_PUBLIC, "<init>", "(Ljava/lang/String;)V", null, null);
            constructor.visitCode();
            constructor.visitVarInsn(Opcodes.ALOAD, 0);
            constructor.visitVarInsn(Opcodes.ALOAD, 1);
            constructor.visitMethodInsn(Opcodes.INVOKESPECIAL, parent, "<init>", "(Ljava/lang/String;)V", false);
            constructor.visitInsn(Opcodes.RETURN);
            constructor.visitMaxs(2, 2);
            constructor.visitEnd();
            writer.visitEnd();
            final byte[] bytes = writer.toByteArray();
            return defineClass(name.replace('/', '.'), bytes, 0, bytes.length);
        }
    }
}
