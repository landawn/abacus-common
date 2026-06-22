package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Objectory;

public class MapEntryTypeTest extends TestBase {

    private MapEntryType<String, Integer> mapEntryType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        mapEntryType = (MapEntryType<String, Integer>) createType("Map.Entry<String, Integer>");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testDeclaringName() {
        String declaringName = mapEntryType.declaringName();
        Assertions.assertNotNull(declaringName);
        Assertions.assertTrue(declaringName.contains("Map.Entry"));
    }

    @Test
    public void testClazz() {
        Class<Map.Entry<String, Integer>> clazz = mapEntryType.javaType();
        Assertions.assertNotNull(clazz);
        assertEquals(Map.Entry.class, clazz);
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = mapEntryType.parameterTypes();
        Assertions.assertNotNull(paramTypes);
        assertEquals(2, paramTypes.size());
    }

    @Test
    public void testIsGenericType() {
        boolean isGeneric = mapEntryType.isParameterizedType();
        Assertions.assertTrue(isGeneric);
    }

    @Test
    public void testStringOfNull() {
        String result = mapEntryType.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOfNonNull() {
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
        String result = mapEntryType.stringOf(entry);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("key"));
        Assertions.assertTrue(result.contains("123"));
    }

    @Test
    public void testValueOfNull() {
        Map.Entry<String, Integer> result = mapEntryType.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        Map.Entry<String, Integer> result = mapEntryType.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyObject() {
        Map.Entry<String, Integer> result = mapEntryType.valueOf("{}");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfValidJson() {
        Map.Entry<String, Integer> result = mapEntryType.valueOf("{\"key\":123}");
        Assertions.assertNotNull(result);
        assertEquals("key", result.getKey());
        assertEquals(123, result.getValue());
    }

    @Test
    public void testValueOfWithMultipleEntriesThrowsException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> mapEntryType.valueOf("{\"a\":1,\"b\":2}"));
    }

    @Test
    public void testAppendToWithNull() throws IOException {
        StringWriter writer = new StringWriter();
        mapEntryType.appendTo(writer, null);
        assertEquals("null", writer.toString());
    }

    @Test
    public void testAppendToWithNonNull() throws IOException {
        StringWriter writer = new StringWriter();
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
        mapEntryType.appendTo(writer, entry);
        String result = writer.toString();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("key"));
        Assertions.assertTrue(result.contains("123"));
    }

    @Test
    public void testAppendToWithStringBuilder() throws IOException {
        StringBuilder sb = new StringBuilder();
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
        mapEntryType.appendTo(sb, entry);
        String result = sb.toString();
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("key"));
        Assertions.assertTrue(result.contains("123"));
    }

    @Test
    public void testAppendToPropagatesWriterIOException() {
        Writer writer = newFailingWriter();
        Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
        assertThrows(IOException.class, () -> mapEntryType.appendTo(writer, entry));
    }

    @Test
    public void testSerializeToWithNull() throws IOException {
        assertDoesNotThrow(() -> {
            mapEntryType.serializeTo(characterWriter, null, null);
        });
    }

    @Test
    public void testSerializeToWithNonNull() throws IOException {
        assertDoesNotThrow(() -> {
            Map.Entry<String, Integer> entry = new AbstractMap.SimpleEntry<>("key", 123);
            JsonXmlSerConfig<?> config = null;
            mapEntryType.serializeTo(characterWriter, entry, config);
        });
    }

    @Test
    public void testSerializeToQuotesNumericMapKeyByDefault() throws IOException {
        MapEntryType<Integer, String> type = (MapEntryType<Integer, String>) createType("Map.Entry<Integer, String>");
        BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();

        try {
            type.serializeTo(writer, new AbstractMap.SimpleEntry<>(1, "a"), JsonSerConfig.create());
            assertEquals("{\"1\":\"a\"}", writer.toString());
        } finally {
            Objectory.recycle(writer);
        }
    }

    private static Writer newFailingWriter() {
        return new Writer() {
            @Override
            public void write(final char[] cbuf, final int off, final int len) throws IOException {
                throw new IOException("boom");
            }

            @Override
            public void flush() throws IOException {
                throw new IOException("boom");
            }

            @Override
            public void close() throws IOException {
                throw new IOException("boom");
            }
        };
    }
}
