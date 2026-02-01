package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerializationConfig;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.Indexed;

@Tag("new-test")
public class IndexedType100Test extends TestBase {

    private IndexedType<String> indexedType;
    private CharacterWriter characterWriter;

    @Mock
    private JsonXmlSerializationConfig<?> config;

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
        assertTrue(indexedType.isParameterizedType());
    }

    @Test
    public void testStringOf() {
        assertNull(indexedType.stringOf(null));

    }

    @Test
    public void testValueOf() {
        assertNull(indexedType.valueOf(null));
        assertNull(indexedType.valueOf(""));

    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();

        indexedType.appendTo(writer, null);
        assertEquals("null", writer.toString());

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
        indexedType.writeCharacter(characterWriter, null, config);

        Indexed<String> indexed = Indexed.of("value", 10);
        indexedType.writeCharacter(characterWriter, indexed, config);
    }

    @Test
    public void testGetTypeName() {
        String typeName = IndexedType.getTypeName("String", true);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Indexed"));
        assertTrue(typeName.contains("String"));

        typeName = IndexedType.getTypeName("String", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Indexed"));
        assertTrue(typeName.contains("String"));
    }
}
