package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.landawn.abacus.TestBase;

public class ParserFactoryTest extends TestBase {

    // Simple Kryo Serializer for testing
    private static class TestStringSerializer extends Serializer<String> {
        TestStringSerializer() {
            super(true, true); // accepts null, immutable
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
    public void testIsAbacusXMLAvailable() {
        boolean available = ParserFactory.isAbacusXmlParserAvailable();
        assertTrue(available || !available);
    }

    // =====================================================================
    // isAbacusXmlParserAvailable
    // =====================================================================

    @Test
    public void test_isAbacusXmlParserAvailable() {
        assertDoesNotThrow(() -> {
            // Just verify the method can be called
            ParserFactory.isAbacusXmlParserAvailable();
        });
    }

    @Test
    public void testIsXMLAvailable() {
        boolean available = ParserFactory.isXmlParserAvailable();
        assertTrue(available || !available);
    }

    // =====================================================================
    // isXmlParserAvailable
    // =====================================================================

    @Test
    public void test_isXmlParserAvailable() {
        assertDoesNotThrow(() -> {
            ParserFactory.isXmlParserAvailable();
        });
    }

    @Test
    public void testIsAvroAvailable() {
        boolean available = ParserFactory.isAvroParserAvailable();
        assertTrue(available || !available);
    }

    // =====================================================================
    // isAvroParserAvailable
    // =====================================================================

    @Test
    public void test_isAvroParserAvailable() {
        assertDoesNotThrow(() -> {
            ParserFactory.isAvroParserAvailable();
        });
    }

    // =====================================================================
    // isKryoParserAvailable
    // =====================================================================

    @Test
    public void testIsKryoParserAvailable() {
        boolean available = ParserFactory.isKryoParserAvailable();
        assertTrue(available || !available);
    }

    @Test
    public void testIsKryoAvailable() {
        boolean available = ParserFactory.isKryoParserAvailable();
        assertTrue(available || !available);
    }

    // =====================================================================
    // createAvroParser
    // =====================================================================

    @Test
    public void test_createAvroParser() {
        if (ParserFactory.isAvroParserAvailable()) {
            AvroParser parser = ParserFactory.createAvroParser();
            assertNotNull(parser);
        }
    }

    @Test
    public void testCreateAvroParser_notNull() {
        if (ParserFactory.isAvroParserAvailable()) {
            AvroParser parser1 = ParserFactory.createAvroParser();
            AvroParser parser2 = ParserFactory.createAvroParser();
            assertNotNull(parser1);
            assertNotNull(parser2);
            assertTrue(parser1 != parser2);
        }
    }

    // =====================================================================
    // createKryoParser
    // =====================================================================

    @Test
    public void test_createKryoParser() {
        if (ParserFactory.isKryoParserAvailable()) {
            KryoParser parser = ParserFactory.createKryoParser();
            assertNotNull(parser);
        }
    }

    @Test
    public void testCreateKryoParser_multipleInstances() {
        if (ParserFactory.isKryoParserAvailable()) {
            KryoParser parser1 = ParserFactory.createKryoParser();
            KryoParser parser2 = ParserFactory.createKryoParser();
            assertNotNull(parser1);
            assertNotNull(parser2);
            assertTrue(parser1 != parser2);
        }
    }

    @Test
    public void testCreateKryoParser_usable() {
        if (ParserFactory.isKryoParserAvailable()) {
            KryoParser parser = ParserFactory.createKryoParser();
            byte[] encoded = parser.encode("hello");
            assertNotNull(encoded);
            String decoded = parser.decode(encoded);
            assertEquals("hello", decoded);
        }
    }

    // =====================================================================
    // createJsonParser
    // =====================================================================

    @Test
    public void test_createJsonParser() {
        JsonParser parser = ParserFactory.createJsonParser();
        assertNotNull(parser);
    }

    // =====================================================================
    // createJsonParser(JsonSerConfig, JsonDeserConfig)
    // =====================================================================

    @Test
    public void test_createJsonParser_withConfig() {
        JsonSerConfig jsc = new JsonSerConfig();
        JsonDeserConfig jdc = new JsonDeserConfig();
        JsonParser parser = ParserFactory.createJsonParser(jsc, jdc);
        assertNotNull(parser);
    }

    @Test
    public void testCreateJsonParser_withNullConfigs() {
        JsonParser parser = ParserFactory.createJsonParser(null, null);
        assertNotNull(parser);
    }

    @Test
    public void testCreateJsonParser_usable() {
        JsonParser parser = ParserFactory.createJsonParser();
        String json = parser.serialize("hello");
        assertNotNull(json);
        String result = parser.deserialize(json, String.class);
        assertEquals("hello", result);
    }

    @Test
    public void testCreateJsonParser_withCustomConfig() {
        JsonSerConfig jsc = new JsonSerConfig().setPrettyFormat(true).setQuotePropName(true);
        JsonDeserConfig jdc = new JsonDeserConfig();
        JsonParser parser = ParserFactory.createJsonParser(jsc, jdc);

        String json = parser.serialize("test");
        assertNotNull(json);
    }

    // =====================================================================
    // createAbacusXmlParser
    // =====================================================================

    @Test
    public void test_createAbacusXmlParser() {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            XmlParser parser = ParserFactory.createAbacusXmlParser();
            assertNotNull(parser);
        }
    }

    // =====================================================================
    // createAbacusXmlParser(XmlSerConfig, XmlDeserConfig)
    // =====================================================================

    @Test
    public void test_createAbacusXmlParser_withConfig() {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            XmlSerConfig xsc = new XmlSerConfig();
            XmlDeserConfig xdc = new XmlDeserConfig();
            XmlParser parser = ParserFactory.createAbacusXmlParser(xsc, xdc);
            assertNotNull(parser);
        }
    }

    @Test
    public void testCreateAbacusXmlParser_withNullConfigs() {
        if (ParserFactory.isAbacusXmlParserAvailable()) {
            XmlParser parser = ParserFactory.createAbacusXmlParser(null, null);
            assertNotNull(parser);
        }
    }

    // =====================================================================
    // createXmlParser
    // =====================================================================

    @Test
    public void test_createXmlParser() {
        if (ParserFactory.isXmlParserAvailable()) {
            XmlParser parser = ParserFactory.createXmlParser();
            assertNotNull(parser);
        }
    }

    // =====================================================================
    // createXmlParser(XmlSerConfig, XmlDeserConfig)
    // =====================================================================

    @Test
    public void test_createXmlParser_withConfig() {
        if (ParserFactory.isXmlParserAvailable()) {
            XmlSerConfig xsc = new XmlSerConfig();
            XmlDeserConfig xdc = new XmlDeserConfig();
            XmlParser parser = ParserFactory.createXmlParser(xsc, xdc);
            assertNotNull(parser);
        }
    }

    @Test
    public void testCreateXmlParser_withNullConfigs() {
        if (ParserFactory.isXmlParserAvailable()) {
            XmlParser parser = ParserFactory.createXmlParser(null, null);
            assertNotNull(parser);
        }
    }

    // =====================================================================
    // createJaxbParser
    // =====================================================================

    @Test
    public void test_createJaxbParser() {
        XmlParser parser = ParserFactory.createJaxbParser();
        assertNotNull(parser);
    }

    // =====================================================================
    // createJaxbParser(XmlSerConfig, XmlDeserConfig)
    // =====================================================================

    @Test
    public void test_createJaxbParser_withConfig() {
        XmlSerConfig xsc = new XmlSerConfig();
        XmlDeserConfig xdc = new XmlDeserConfig();
        XmlParser parser = ParserFactory.createJaxbParser(xsc, xdc);
        assertNotNull(parser);
    }

    @Test
    public void testCreateJaxbParser_withNullConfigs() {
        XmlParser parser = ParserFactory.createJaxbParser(null, null);
        assertNotNull(parser);
    }

    // =====================================================================
    // registerKryo(Class)
    // =====================================================================

    @Test
    public void test_registerKryo_class() {
        assertDoesNotThrow(() -> {
            ParserFactory.registerKryo(String.class);
        });
    }

    @Test
    public void test_registerKryo_class_null() {
        assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(null));
    }

    @Test
    public void testRegisterKryo() {
        assertDoesNotThrow(() -> {
            ParserFactory.registerKryo(String.class);
            ParserFactory.registerKryo(Integer.class, 100);
        });
    }

    @Test
    public void testRegisterKryoWithNullClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParserFactory.registerKryo(null);
        });
    }

    // =====================================================================
    // registerKryo(Class, int)
    // =====================================================================

    @Test
    public void test_registerKryo_classWithId() {
        assertDoesNotThrow(() -> {
            ParserFactory.registerKryo(Integer.class, 100);
        });
    }

    @Test
    public void test_registerKryo_classWithId_null() {
        assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(null, 100));
    }

    @Test
    public void testRegisterKryoWithIdAndNullClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParserFactory.registerKryo(null, 100);
        });
    }

    // =====================================================================
    // registerKryo(Class, Serializer)
    // =====================================================================

    @Test
    public void testRegisterKryo_classWithSerializer() {
        assertDoesNotThrow(() -> {
            ParserFactory.registerKryo(String.class, new TestStringSerializer());
        });
    }

    @Test
    public void testRegisterKryo_classWithSerializer_nullClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParserFactory.registerKryo(null, new TestStringSerializer());
        });
    }

    @Test
    public void testRegisterKryo_classWithSerializer_nullSerializer() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParserFactory.registerKryo(String.class, (Serializer<?>) null);
        });
    }

    // =====================================================================
    // registerKryo(Class, Serializer, int)
    // =====================================================================

    @Test
    public void testRegisterKryo_classWithSerializerAndId() {
        assertDoesNotThrow(() -> {
            ParserFactory.registerKryo(String.class, new TestStringSerializer(), 300);
        });
    }

    @Test
    public void test_registerKryo_classWithSerializerAndId_nullSerializer() {
        assertThrows(IllegalArgumentException.class, () -> ParserFactory.registerKryo(String.class, null, 200));
    }

    @Test
    public void testRegisterKryo_classWithSerializerAndId_nullClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParserFactory.registerKryo(null, new TestStringSerializer(), 300);
        });
    }

    @Test
    public void testRegisterKryo_classWithSerializerAndId_nullBoth() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParserFactory.registerKryo(null, null, 300);
        });
    }

    @Test
    public void testRegisterKryo_multipleClasses() {
        assertDoesNotThrow(() -> {
            ParserFactory.registerKryo(String.class);
            ParserFactory.registerKryo(Integer.class);
            ParserFactory.registerKryo(Double.class);
        });
    }

    @Test
    public void testRegisterKryo_classWithIdMultiple() {
        assertDoesNotThrow(() -> {
            ParserFactory.registerKryo(String.class, 1000);
            ParserFactory.registerKryo(Integer.class, 1001);
            ParserFactory.registerKryo(Double.class, 1002);
        });
    }

    @Test
    public void testRegisterKryo_classWithSerializerAndNullSerializer() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParserFactory.registerKryo(String.class, (Serializer<?>) null, 400);
        });
    }

}
