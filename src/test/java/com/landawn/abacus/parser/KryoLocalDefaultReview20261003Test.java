package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Registration;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Tuple.Tuple2;

public class KryoLocalDefaultReview20261003Test extends TestBase {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void localDefaultReplacesGlobalSerializerAndRetainsId(boolean explicitGlobalId) {
        Serializer<Value> global = new ValueSerializer("-global");
        try (GlobalRegistration saved = new GlobalRegistration(Value.class)) {
            if (explicitGlobalId) {
                ParserFactory.registerKryo(Value.class, global, 1_931_001);
            } else {
                ParserFactory.registerKryo(Value.class, global);
            }
            KryoParser parser = ParserFactory.createKryoParser();
            int globalId = registrationId(parser, Value.class);
            assertEquals("data-global", roundTrip(parser).name);

            parser.register(Value.class);
            assertEquals(globalId, registrationId(parser, Value.class));
            assertEquals("data", roundTrip(parser).name);
            assertEquals("data-global", roundTrip(ParserFactory.createKryoParser()).name);
        }
    }

    @Test
    void localDefaultSurvivesGlobalRefreshAndReplacesLocalSerializer() {
        try (GlobalRegistration saved = new GlobalRegistration(Value.class)) {
            ParserFactory.registerKryo(Value.class, new ValueSerializer("-global"));
            KryoParser parser = ParserFactory.createKryoParser();
            parser.register(Value.class, new ValueSerializer("-local"));
            assertEquals("data-local", roundTrip(parser).name);

            parser.register(Value.class);
            assertEquals("data", roundTrip(parser).name);
            ParserFactory.registerKryo(Value.class, new ValueSerializer("-updated"));
            assertEquals("data", roundTrip(parser).name);
        }
    }

    @Test
    void localDefaultRestoresPrimitiveWrapperSerializer() {
        Serializer<Integer> global = new Serializer<>() {
            @Override
            public void write(Kryo kryo, Output output, Integer value) {
                output.writeInt(value + 100);
            }

            @Override
            public Integer read(Kryo kryo, Input input, Class<? extends Integer> type) {
                return input.readInt();
            }
        };
        try (GlobalRegistration saved = new GlobalRegistration(Integer.class)) {
            ParserFactory.registerKryo(Integer.class, global);
            KryoParser parser = ParserFactory.createKryoParser();
            assertEquals(123, (int) parser.decode(parser.encode(23)));
            int id = registrationId(parser, Integer.class);
            parser.register(Integer.class);
            assertEquals(id, registrationId(parser, Integer.class));
            assertEquals(23, (int) parser.decode(parser.encode(23)));
            assertEquals(23, (int) parser.deserialize(parser.serialize(23), int.class));
        }
    }

    @Test
    void localDefaultRestoresStringSerializer() {
        Serializer<String> global = new Serializer<>() {
            @Override
            public void write(Kryo kryo, Output output, String value) {
                output.writeString(value + "-global");
            }

            @Override
            public String read(Kryo kryo, Input input, Class<? extends String> type) {
                return input.readString();
            }
        };
        try (GlobalRegistration saved = new GlobalRegistration(String.class)) {
            ParserFactory.registerKryo(String.class, global);
            KryoParser parser = ParserFactory.createKryoParser();
            assertEquals("data-global", parser.decode(parser.encode("data")));
            parser.register(String.class);
            assertEquals("data", parser.decode(parser.encode("data")));
        }
    }

    @Test
    void automaticIdsAndOrdinaryPrimitiveRegistrationsStayStable() {
        KryoParser parser = ParserFactory.createKryoParser();
        Kryo initial = parser.createKryo();
        int nextId;
        try {
            nextId = initial.getNextRegistrationId();
        } finally {
            parser.recycle(initial);
        }
        parser.register(Value.class);
        assertEquals(nextId, registrationId(parser, Value.class));
        assertEquals("data", roundTrip(parser).name);

        for (Class<?> type : new Class<?>[] { boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class,
                Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class, String.class }) {
            int id = registrationId(parser, type);
            parser.register(type);
            assertEquals(id, registrationId(parser, type));
        }
        for (Object value : new Object[] { true, 'x', (byte) 7, (short) 8, 9, 10L, 11.5f, 12.5d, "text" }) {
            assertEquals(value, parser.decode(parser.encode(value)));
        }
    }

    @Test
    void explicitIdAndExplicitSerializersKeepTheirPrecedence() {
        Serializer<Value> global = new ValueSerializer("-global");
        Serializer<Value> local = new ValueSerializer("-local");
        try (GlobalRegistration saved = new GlobalRegistration(Value.class)) {
            ParserFactory.registerKryo(Value.class, global, 1_931_002);
            KryoParser parser = ParserFactory.createKryoParser();
            parser.register(Value.class, 1_931_003);
            assertEquals(1_931_003, registrationId(parser, Value.class));
            assertEquals("data", roundTrip(parser).name);
            parser.register(Value.class, local);
            assertEquals("data-local", roundTrip(parser).name);
            parser.register(Value.class, local, 1_931_004);
            assertEquals(1_931_004, registrationId(parser, Value.class));
            assertEquals("data-local", roundTrip(parser).name);
            Kryo kryo = parser.createKryo();
            try {
                Registration registration = kryo.getRegistration(Value.class);
                assertSame(local, registration.getSerializer());
                assertNotSame(global, registration.getSerializer());
            } finally {
                parser.recycle(kryo);
            }

            parser.register(Value.class);
            assertEquals(1_931_002, registrationId(parser, Value.class));
        }
    }

    private static int registrationId(KryoParser parser, Class<?> type) {
        Kryo kryo = parser.createKryo();
        try {
            return kryo.getRegistration(type).getId();
        } finally {
            parser.recycle(kryo);
        }
    }

    private static Value roundTrip(KryoParser parser) {
        return parser.decode(parser.encode(new Value("data")));
    }

    public static class Value {
        public String name;

        public Value() {
        }

        Value(String name) {
            this.name = name;
        }
    }

    private static class ValueSerializer extends Serializer<Value> {
        private final String suffix;

        ValueSerializer(String suffix) {
            this.suffix = suffix;
        }

        @Override
        public void write(Kryo kryo, Output output, Value value) {
            output.writeString(value.name + suffix);
        }

        @Override
        public Value read(Kryo kryo, Input input, Class<? extends Value> type) {
            return new Value(input.readString());
        }
    }

    private static class GlobalRegistration implements AutoCloseable {
        private final Class<?> type;
        private final boolean plain;
        private final Integer id;
        private final Serializer<?> serializer;
        private final Tuple2<Serializer<?>, Integer> serializerId;

        GlobalRegistration(Class<?> type) {
            this.type = type;
            synchronized (ParserFactory._kryoRegistrationLock) {
                plain = ParserFactory._kryoClassSet.contains(type);
                id = ParserFactory._kryoClassIdMap.get(type);
                serializer = ParserFactory._kryoClassSerializerMap.get(type);
                serializerId = ParserFactory._kryoClassSerializerIdMap.get(type);
            }
        }

        @Override
        public void close() {
            synchronized (ParserFactory._kryoRegistrationLock) {
                ParserFactory._kryoClassSet.remove(type);
                ParserFactory._kryoClassIdMap.remove(type);
                ParserFactory._kryoClassSerializerMap.remove(type);
                ParserFactory._kryoClassSerializerIdMap.remove(type);
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
                ParserFactory._kryoRegistrationVersion.incrementAndGet();
            }
        }
    }
}
