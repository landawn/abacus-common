package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.AbstractMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class ImmutableMapEntryType100Test extends TestBase {

    private ImmutableMapEntryType<String, Integer> immutableMapEntryType;
    private CharacterWriter characterWriter;

    @Mock
    private JSONXMLSerializationConfig<?> config;

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
        assertEquals(AbstractMap.SimpleImmutableEntry.class, immutableMapEntryType.clazz());
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = immutableMapEntryType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(2, paramTypes.length);
    }

    @Test
    public void testIsGenericType() {
        assertTrue(immutableMapEntryType.isGenericType());
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
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();

        immutableMapEntryType.appendTo(writer, null);
        assertEquals("null", writer.toString());

    }

    @Test
    public void testWriteCharacter() throws IOException {
        immutableMapEntryType.writeCharacter(characterWriter, null, config);

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
}
