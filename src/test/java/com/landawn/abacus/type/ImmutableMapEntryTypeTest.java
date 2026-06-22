package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.AbstractMap;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Objectory;

public class ImmutableMapEntryTypeTest extends TestBase {

    private ImmutableMapEntryType<String, Integer> immutableMapEntryType;
    private CharacterWriter characterWriter;

    @Mock
    private JsonXmlSerConfig<?> config;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        immutableMapEntryType = (ImmutableMapEntryType<String, Integer>) createType("Map.ImmutableEntry<String, Integer>");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testDeclaringName() {
        String declaringName = immutableMapEntryType.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Map.ImmutableEntry"));
        assertTrue(declaringName.contains("String"));
        assertTrue(declaringName.contains("Integer"));
    }

    @Test
    public void testClazz() {
        assertEquals(AbstractMap.SimpleImmutableEntry.class, immutableMapEntryType.javaType());
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = immutableMapEntryType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(2, paramTypes.size());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(immutableMapEntryType.isParameterizedType());
    }

    @Test
    public void testStringOf() {
        assertNull(immutableMapEntryType.stringOf(null));

    }

    @Test
    public void testValueOf() {
        assertNull(immutableMapEntryType.valueOf(null));
        assertNull(immutableMapEntryType.valueOf(""));
        assertNull(immutableMapEntryType.valueOf("{}"));

    }

    @Test
    public void testValueOfWithMultipleEntriesThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> immutableMapEntryType.valueOf("{\"a\":1,\"b\":2}"));
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();

        immutableMapEntryType.appendTo(writer, null);
        assertEquals("null", writer.toString());

    }

    @Test
    public void testAppendToPropagatesWriterIOException() {
        Writer writer = newFailingWriter();
        AbstractMap.SimpleImmutableEntry<String, Integer> entry = new AbstractMap.SimpleImmutableEntry<>("key", 123);
        assertThrows(IOException.class, () -> immutableMapEntryType.appendTo(writer, entry));
    }

    @Test
    public void testSerializeTo() throws IOException {
        assertDoesNotThrow(() -> {
            immutableMapEntryType.serializeTo(characterWriter, null, config);
        });
    }

    @Test
    public void testSerializeToQuotesNumericMapKeyByDefault() throws IOException {
        ImmutableMapEntryType<Integer, String> type = (ImmutableMapEntryType<Integer, String>) createType("Map.ImmutableEntry<Integer, String>");
        BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();

        try {
            type.serializeTo(writer, new AbstractMap.SimpleImmutableEntry<>(1, "a"), JsonSerConfig.create());
            assertEquals("{\"1\":\"a\"}", writer.toString());
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Test
    public void testGetTypeName() {
        String typeName = ImmutableMapEntryType.getTypeName("String", "Integer", true);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Map.ImmutableEntry"));
        assertTrue(typeName.contains("String"));
        assertTrue(typeName.contains("Integer"));

        typeName = ImmutableMapEntryType.getTypeName("String", "Integer", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Map.ImmutableEntry"));
        assertTrue(typeName.contains("String"));
        assertTrue(typeName.contains("Integer"));
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
