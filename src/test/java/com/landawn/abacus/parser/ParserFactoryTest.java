package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.landawn.abacus.TestBase;

public class ParserFactoryTest extends TestBase {

    private static final class ReplacementRegistrationTarget {
    }

    private static final class DuplicateIdRegistrationTargetA {
    }

    private static final class DuplicateIdRegistrationTargetB {
    }

    private static final class DuplicateIdRegistrationTargetC {
    }

    private static final class DuplicateIdRegistrationTargetD {
    }

    private static final class ImplicitGlobalRegistrationTarget {
    }

    private static final class ExplicitGlobalRegistrationTarget {
    }

    private static final class ImplicitGlobalSerializerTarget {
    }

    private static final class ExplicitGlobalSerializerTarget {
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

    private static class TestStringSerializer extends Serializer<String> {
        TestStringSerializer() {
            super(true, true);
        }

        @Override
        public void write(Kryo kryo, Output output, String object) {
            output.writeString(object);
        }

        @Override
        public String read(Kryo kryo, Input input, Class<? extends String> type) {
            return input.readString();
        }
    }

    @Test
    public void testIsParserAvailable() {
        assertDoesNotThrow(ParserFactory::isAbacusXmlParserAvailable);
        assertDoesNotThrow(ParserFactory::isXmlParserAvailable);
        assertDoesNotThrow(ParserFactory::isAvroParserAvailable);
        assertDoesNotThrow(ParserFactory::isKryoParserAvailable);

        boolean jaxb = ParserFactory.isJaxbParserAvailable();
        assertEquals(jaxb, ParserFactory.isJaxbParserAvailable());
        if (jaxb) {
            assertNotNull(ParserFactory.createJaxbParser());
        } else {
            assertThrows(Throwable.class, ParserFactory::createJaxbParser);
        }
    }

    @Test
    public void testCreateAvroParser() {
        if (!ParserFactory.isAvroParserAvailable()) {
            return;
        }
        AvroParser a1 = ParserFactory.createAvroParser();
        AvroParser a2 = ParserFactory.createAvroParser();
        assertNotNull(a1);
        assertNotSame(a1, a2);
    }

    @Test
    public void testCreateKryoParser() {
        if (!ParserFactory.isKryoParserAvailable()) {
            return;
        }
        KryoParser p1 = ParserFactory.createKryoParser();
        KryoParser p2 = ParserFactory.createKryoParser();
        assertNotNull(p1);
        assertNotSame(p1, p2);
        assertEquals("hello", p1.decode(p1.encode("hello")));
    }

    @Test
    public void testCreateJsonParser() {
        JsonParser parser = ParserFactory.createJsonParser();
        assertNotNull(parser);
        assertEquals("hello", parser.deserialize(parser.serialize("hello"), String.class));

        JsonSerConfig jsc = new JsonSerConfig().setPrettyFormat(true).setQuotePropName(true);
        JsonParser configured = ParserFactory.createJsonParser(jsc, new JsonDeserConfig());
        assertNotNull(configured.serialize("test"));
        assertNotNull(ParserFactory.createJsonParser(null, null));
    }

    @Test
    public void testCreateAbacusXmlParser() {
        if (!ParserFactory.isAbacusXmlParserAvailable()) {
            return;
        }
        assertNotNull(ParserFactory.createAbacusXmlParser());
        assertNotNull(ParserFactory.createAbacusXmlParser(new XmlSerConfig(), new XmlDeserConfig()));
        assertNotNull(ParserFactory.createAbacusXmlParser(null, null));
    }

    @Test
    public void testCreateXmlParser() {
        if (!ParserFactory.isXmlParserAvailable()) {
            return;
        }
        assertNotNull(ParserFactory.createXmlParser());
        assertNotNull(ParserFactory.createXmlParser(new XmlSerConfig(), new XmlDeserConfig()));
        assertNotNull(ParserFactory.createXmlParser(null, null));
    }

    @Test
    public void testCreateJaxbParser() {
        assertNotNull(ParserFactory.createJaxbParser());
        assertNotNull(ParserFactory.createJaxbParser(new XmlSerConfig(), new XmlDeserConfig()));
        assertNotNull(ParserFactory.createJaxbParser(null, null));
    }

    @Test
    public void testRegisterKryo() {
        try {
            assertDoesNotThrow(() -> {
                ParserFactory.registerKryo(String.class);
                ParserFactory.registerKryo(Integer.class, 100);
                ParserFactory.registerKryo(Double.class);
                ParserFactory.registerKryo(Long.class, 1001);
                ParserFactory.registerKryo(Float.class, new TestStringSerializer());
                ParserFactory.registerKryo(Short.class, new TestStringSerializer(), 300);
            });
        } finally {
            unregisterKryoForTest(String.class);
            unregisterKryoForTest(Integer.class);
            unregisterKryoForTest(Double.class);
            unregisterKryoForTest(Long.class);
            unregisterKryoForTest(Float.class);
            unregisterKryoForTest(Short.class);
        }
    }

    @Test
    public void testRegisterKryo_EdgeCase() {
        assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(null));
        assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(null, 100));
        assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(Integer.class, -1));
        assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(Integer.class, new TestStringSerializer(), -1));
        assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(null, new TestStringSerializer()));
        assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(String.class, (Serializer<?>) null));
        assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(String.class, null, 200));
        assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(null, new TestStringSerializer(), 300));
        assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(null, null, 300));
        assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(String.class, (Serializer<?>) null, 400));
    }

    @Test
    public void testRegisterKryo_rejectsDuplicateExplicitIds() {
        final int classRegistrationId = 1_900_001;
        final int serializerRegistrationId = 1_900_002;
        final int retainedClassRegistrationId = 1_900_003;
        final int retainedSerializerRegistrationId = 1_900_004;
        final Serializer<DuplicateIdRegistrationTargetD> retainedSerializer = new Serializer<>() {
            @Override
            public void write(final Kryo kryo, final Output output, final DuplicateIdRegistrationTargetD object) {
            }

            @Override
            public DuplicateIdRegistrationTargetD read(final Kryo kryo, final Input input, final Class<? extends DuplicateIdRegistrationTargetD> type) {
                return new DuplicateIdRegistrationTargetD();
            }
        };

        try {
            ParserFactory.registerKryo(DuplicateIdRegistrationTargetA.class, classRegistrationId);
            ParserFactory.registerKryo(DuplicateIdRegistrationTargetB.class, retainedClassRegistrationId);
            assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(DuplicateIdRegistrationTargetB.class, classRegistrationId));
            assertThrows(IllegalArgumentException.class,
                    () -> ParserFactory.registerKryo(DuplicateIdRegistrationTargetB.class, new TestStringSerializer(), classRegistrationId));
            assertEquals(retainedClassRegistrationId, ParserFactory._kryoClassIdMap.get(DuplicateIdRegistrationTargetB.class).intValue());
            assertTrue(!ParserFactory._kryoClassSerializerIdMap.containsKey(DuplicateIdRegistrationTargetB.class));

            ParserFactory.registerKryo(DuplicateIdRegistrationTargetC.class, new TestStringSerializer(), serializerRegistrationId);
            ParserFactory.registerKryo(DuplicateIdRegistrationTargetD.class, retainedSerializer, retainedSerializerRegistrationId);
            assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(DuplicateIdRegistrationTargetD.class, serializerRegistrationId));
            assertThrows(IllegalArgumentException.class,
                    () -> ParserFactory.registerKryo(DuplicateIdRegistrationTargetD.class, new TestStringSerializer(), serializerRegistrationId));
            assertEquals(retainedSerializerRegistrationId, ParserFactory._kryoClassSerializerIdMap.get(DuplicateIdRegistrationTargetD.class)._2.intValue());
            assertSame(retainedSerializer, ParserFactory._kryoClassSerializerIdMap.get(DuplicateIdRegistrationTargetD.class)._1);
            assertTrue(!ParserFactory._kryoClassIdMap.containsKey(DuplicateIdRegistrationTargetD.class));
        } finally {
            unregisterKryoForTest(DuplicateIdRegistrationTargetA.class);
            unregisterKryoForTest(DuplicateIdRegistrationTargetB.class);
            unregisterKryoForTest(DuplicateIdRegistrationTargetC.class);
            unregisterKryoForTest(DuplicateIdRegistrationTargetD.class);
        }
    }

    @Test
    public void testCreateKryoRejectsGlobalExplicitIdAlreadyAssignedImplicitly() {
        final KryoParser parser = new KryoParser();
        try {
            ParserFactory.registerKryo(ImplicitGlobalRegistrationTarget.class);
            final Kryo initialKryo = parser.createKryo();
            final int implicitId;
            try {
                implicitId = initialKryo.getRegistration(ImplicitGlobalRegistrationTarget.class).getId();
            } finally {
                parser.recycle(initialKryo);
            }
            ParserFactory.registerKryo(ExplicitGlobalRegistrationTarget.class, implicitId);
            assertThrows(IllegalArgumentException.class, parser::createKryo);
        } finally {
            unregisterKryoForTest(ImplicitGlobalRegistrationTarget.class);
            unregisterKryoForTest(ExplicitGlobalRegistrationTarget.class);
        }
    }

    @Test
    public void testCreateKryoRejectsGlobalExplicitSerializerIdAlreadyAssignedImplicitly() {
        final KryoParser parser = new KryoParser();
        try {
            ParserFactory.registerKryo(ImplicitGlobalSerializerTarget.class, new EmptySerializer<>());
            final Kryo initialKryo = parser.createKryo();
            final int implicitId;
            try {
                implicitId = initialKryo.getRegistration(ImplicitGlobalSerializerTarget.class).getId();
            } finally {
                parser.recycle(initialKryo);
            }
            ParserFactory.registerKryo(ExplicitGlobalSerializerTarget.class, new EmptySerializer<>(), implicitId);
            assertThrows(IllegalArgumentException.class, parser::createKryo);
        } finally {
            unregisterKryoForTest(ImplicitGlobalSerializerTarget.class);
            unregisterKryoForTest(ExplicitGlobalSerializerTarget.class);
        }
    }

    @Test
    public void testLatestGlobalRegistrationOverloadReplacesEarlierVariant() {
        final Serializer<ReplacementRegistrationTarget> serializer = new Serializer<>() {
            @Override
            public void write(final Kryo kryo, final Output output, final ReplacementRegistrationTarget object) {
            }

            @Override
            public ReplacementRegistrationTarget read(final Kryo kryo, final Input input, final Class<? extends ReplacementRegistrationTarget> type) {
                return new ReplacementRegistrationTarget();
            }
        };

        try {
            ParserFactory.registerKryo(ReplacementRegistrationTarget.class, serializer, 990);
            ParserFactory.registerKryo(ReplacementRegistrationTarget.class, 991);
            assertEquals(991, ParserFactory._kryoClassIdMap.get(ReplacementRegistrationTarget.class).intValue());
            assertTrue(!ParserFactory._kryoClassSerializerIdMap.containsKey(ReplacementRegistrationTarget.class));
        } finally {
            unregisterKryoForTest(ReplacementRegistrationTarget.class);
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
}
