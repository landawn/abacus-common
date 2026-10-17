package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.landawn.abacus.util.Tuple.Tuple2;

@Tag("unit")
class KryoParserRegistrationTest {
    @ParameterizedTest
    @MethodSource("primitiveRegistrationCases")
    void localExplicitIdRestoresDefaultPrimitiveSerializer(Class<?> localType, boolean explicitGlobalId) {
        synchronized (ParserFactory._kryoRegistrationLock) {
            final List<SavedRegistration> saved = List.of(SavedRegistration.capture(int.class), SavedRegistration.capture(Integer.class));
            saved.forEach(entry -> clearGlobalRegistration(entry.type()));
            ParserFactory._kryoRegistrationVersion.incrementAndGet();

            try {
                Serializer<Integer> globalSerializer = new Serializer<Integer>() {
                    @Override
                    public void write(Kryo kryo, Output output, Integer value) {
                        output.writeInt(value + 100);
                    }

                    @Override
                    public Integer read(Kryo kryo, Input input, Class<? extends Integer> type) {
                        return input.readInt();
                    }
                };
                if (explicitGlobalId) {
                    ParserFactory.registerKryo(Integer.class, globalSerializer, 1_983_010);
                } else {
                    ParserFactory.registerKryo(Integer.class, globalSerializer);
                }

                KryoParser parser = ParserFactory.createKryoParser();
                assertEquals(123, (int) parser.decode(parser.encode(23)));

                int localId = 1_983_011;
                parser.register(localType, localId);
                assertEquals(23, (int) parser.decode(parser.encode(23)));
                assertEquals(23, (int) parser.deserialize(parser.serialize(23), int.class));

                Kryo configured = parser.createKryo();
                try {
                    assertEquals(localId, configured.getRegistration(localType).getId());
                    assertSame(configured.getRegistration(int.class), configured.getRegistration(Integer.class));
                } finally {
                    parser.recycle(configured);
                }

                KryoParser globalParser = ParserFactory.createKryoParser();
                assertEquals(123, (int) globalParser.decode(globalParser.encode(23)));
            } finally {
                saved.forEach(SavedRegistration::restore);
                ParserFactory._kryoRegistrationVersion.incrementAndGet();
            }
        }
    }

    @ParameterizedTest
    @MethodSource("primitiveAliasCases")
    void localIdOnlyRegistrationKeepsEveryPrimitiveAliasDefaultAfterPoolRefresh(final Class<?> primitive, final Class<?> wrapper, final Object value,
            final boolean globalPrimitive, final boolean localPrimitive, final boolean explicitGlobalId) {
        synchronized (ParserFactory._kryoRegistrationLock) {
            final List<SavedRegistration> saved = List.of(SavedRegistration.capture(primitive), SavedRegistration.capture(wrapper));
            saved.forEach(entry -> clearGlobalRegistration(entry.type()));
            ParserFactory._kryoRegistrationVersion.incrementAndGet();

            try {
                final Class<?> globalType = globalPrimitive ? primitive : wrapper;
                final Class<?> localType = localPrimitive ? primitive : wrapper;
                final Kryo defaults = new Kryo();
                final CountingSerializer global = new CountingSerializer(defaults.getSerializer(primitive));
                if (explicitGlobalId) {
                    ParserFactory.registerKryo(globalType, global, 1_983_020);
                } else {
                    ParserFactory.registerKryo(globalType, global);
                }

                final KryoParser parser = ParserFactory.createKryoParser();
                assertRoundTrips(parser, value, primitive, wrapper);
                assertTrue(global.writes > 0);
                assertTrue(global.reads > 0);
                global.reset();

                for (final int localId : new int[] { defaults.getRegistration(primitive).getId(), 1_983_021 }) {
                    parser.register(localType, localId);
                    assertRoundTrips(parser, value, primitive, wrapper);
                    assertDefaultAlias(parser, primitive, wrapper, localId, global);
                    assertEquals(0, global.writes);
                    assertEquals(0, global.reads);
                }

                // The parser has pooled an instance. A later global override invalidates that pool, but the local
                // ID-only registration must still restore the original serializer on its replacement instance.
                final CountingSerializer refreshedGlobal = new CountingSerializer(defaults.getSerializer(primitive));
                ParserFactory.registerKryo(globalType, refreshedGlobal, 1_983_020);
                assertRoundTrips(parser, value, primitive, wrapper);
                assertDefaultAlias(parser, primitive, wrapper, 1_983_021, refreshedGlobal);
                assertEquals(0, refreshedGlobal.writes);
                assertEquals(0, refreshedGlobal.reads);

                assertRoundTrips(ParserFactory.createKryoParser(), value, primitive, wrapper);
                assertTrue(refreshedGlobal.writes > 0);
                assertTrue(refreshedGlobal.reads > 0);

                // A subsequent explicit local serializer remains authoritative over the ID-only default.
                final CountingSerializer local = new CountingSerializer(defaults.getSerializer(primitive));
                parser.register(localType, local, 1_983_021);
                assertRoundTrips(parser, value, primitive, wrapper);
                assertTrue(local.writes > 0);
                assertTrue(local.reads > 0);
            } finally {
                saved.forEach(SavedRegistration::restore);
                ParserFactory._kryoRegistrationVersion.incrementAndGet();
            }
        }
    }

    private static void assertRoundTrips(final KryoParser parser, final Object value, final Class<?> primitive, final Class<?> wrapper) {
        assertEquals(value, parser.decode(parser.encode(value)));
        final String serialized = parser.serialize(value);
        assertEquals(value, parser.deserialize(serialized, primitive));
        assertEquals(value, parser.deserialize(serialized, wrapper));
    }

    private static void assertDefaultAlias(final KryoParser parser, final Class<?> primitive, final Class<?> wrapper, final int id,
            final Serializer<?> override) {
        final Kryo kryo = parser.createKryo();
        try {
            assertEquals(id, kryo.getRegistration(primitive).getId());
            assertSame(kryo.getRegistration(primitive), kryo.getRegistration(wrapper));
            assertNotSame(override, kryo.getRegistration(primitive).getSerializer());
        } finally {
            parser.recycle(kryo);
        }
    }

    private static Stream<Arguments> primitiveAliasCases() {
        return Stream
                .of(new Object[] { boolean.class, Boolean.class, true }, new Object[] { char.class, Character.class, '\u03bb' },
                        new Object[] { byte.class, Byte.class, (byte) -7 }, new Object[] { short.class, Short.class, (short) -32000 },
                        new Object[] { int.class, Integer.class, -1234567 }, new Object[] { long.class, Long.class, 9876543210L },
                        new Object[] { float.class, Float.class, -1.25f }, new Object[] { double.class, Double.class, 3.5d })
                .flatMap(types -> Stream.of(false, true)
                        .flatMap(globalPrimitive -> Stream.of(false, true)
                                .flatMap(localPrimitive -> Stream.of(false, true)
                                        .map(explicitGlobalId -> Arguments.of(types[0], types[1], types[2], globalPrimitive, localPrimitive,
                                                explicitGlobalId)))));
    }

    private static void clearGlobalRegistration(final Class<?> type) {
        ParserFactory._kryoClassSet.remove(type);
        ParserFactory._kryoClassIdMap.remove(type);
        ParserFactory._kryoClassSerializerMap.remove(type);
        ParserFactory._kryoClassSerializerIdMap.remove(type);
    }

    private record SavedRegistration(Class<?> type, boolean plain, Integer id, Serializer<?> serializer, Tuple2<Serializer<?>, Integer> serializerId) {
        static SavedRegistration capture(final Class<?> type) {
            return new SavedRegistration(type, ParserFactory._kryoClassSet.contains(type), ParserFactory._kryoClassIdMap.get(type),
                    ParserFactory._kryoClassSerializerMap.get(type), ParserFactory._kryoClassSerializerIdMap.get(type));
        }

        void restore() {
            clearGlobalRegistration(type);
            if (plain) {
                ParserFactory._kryoClassSet.add(type);
            }
            if (id != null) {
                ParserFactory._kryoClassIdMap.put(type, id);
            }
            if (serializer != null) {
                ParserFactory._kryoClassSerializerMap.put(type, serializer);
            }
            if (serializerId != null) {
                ParserFactory._kryoClassSerializerIdMap.put(type, serializerId);
            }
        }
    }

    private static final class CountingSerializer extends Serializer<Object> {
        private final Serializer<Object> delegate;
        private int writes;
        private int reads;

        CountingSerializer(final Serializer<Object> delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(final Kryo kryo, final Output output, final Object value) {
            writes++;
            delegate.write(kryo, output, value);
        }

        @Override
        public Object read(final Kryo kryo, final Input input, final Class<?> type) {
            reads++;
            return delegate.read(kryo, input, type);
        }

        void reset() {
            writes = 0;
            reads = 0;
        }
    }

    private static Stream<Arguments> primitiveRegistrationCases() {
        return Stream.of(Arguments.of(int.class, false), Arguments.of(Integer.class, false), Arguments.of(int.class, true), Arguments.of(Integer.class, true));
    }
}
