package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JSONXMLSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Indexed;

public class IndexedType100Test extends TestBase {

    private IndexedType<String> indexedType;
    private CharacterWriter characterWriter;

    @Mock
    private JSONXMLSerializationConfig<?> config;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        indexedType = (IndexedType<String>) createType("Indexed<String>");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testDeclaringName() {
        String declaringName = indexedType.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Indexed"));
        assertTrue(declaringName.contains("String"));
    }

    @Test
    public void testClazz() {
        assertEquals(Indexed.class, indexedType.clazz());
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = indexedType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.length);
    }

    @Test
    public void testIsGenericType() {
        assertTrue(indexedType.isGenericType());
    }

    @Test
    public void testStringOf() {
        // Test with null
        assertNull(indexedType.stringOf(null));

        // Test with Indexed would require mocking Utils.jsonParser
        // and N.asArray()
    }

    @Test
    public void testValueOf() {
        // Test with null and empty string
        assertNull(indexedType.valueOf(null));
        assertNull(indexedType.valueOf(""));

        // Test with JSON array string would require mocking Utils.jsonParser
        // and testing the parsing logic
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();

        // Test with null
        indexedType.appendTo(writer, null);
        assertEquals("null", writer.toString());

        // Test with Indexed
        writer = new StringWriter();
        Indexed<String> indexed = Indexed.of("value", 5);
        indexedType.appendTo(writer, indexed);
        String result = writer.toString();
        assertTrue(result.contains("["));
        assertTrue(result.contains("]"));
        assertTrue(result.contains("5"));
    }

    @Test
    public void testWriteCharacter() throws IOException {
        // Test with null
        indexedType.writeCharacter(characterWriter, null, config);

        // Test with Indexed
        Indexed<String> indexed = Indexed.of("value", 10);
        indexedType.writeCharacter(characterWriter, indexed, config);
    }

    @Test
    public void testGetTypeName() {
        // Test static method with declaring name
        String typeName = IndexedType.getTypeName("String", true);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Indexed"));
        assertTrue(typeName.contains("String"));

        // Test static method without declaring name
        typeName = IndexedType.getTypeName("String", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Indexed"));
        assertTrue(typeName.contains("String"));
    }
}
