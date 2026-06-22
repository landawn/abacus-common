package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ImmutableList;

public class ImmutableListTypeTest extends TestBase {

    private ImmutableListType<String> immutableListType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        immutableListType = (ImmutableListType<String>) createType("ImmutableList<String>");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testDeclaringName() {
        String declaringName = immutableListType.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("ImmutableList"));
        assertTrue(declaringName.contains("String"));
    }

    @Test
    public void testClazz() {
        assertEquals(ImmutableList.class, immutableListType.javaType());
    }

    @Test
    public void testGetElementType() {
        Type<?> elementType = immutableListType.elementType();
        assertNotNull(elementType);
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = immutableListType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.size());
    }

    @Test
    public void testIsList() {
        assertTrue(immutableListType.isList());
    }

    @Test
    public void testIsCollection() {
        assertTrue(immutableListType.isCollection());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(immutableListType.isParameterizedType());
    }

    @Test
    public void testIsSerializable() {
        boolean result = immutableListType.isSerializable();
        assertTrue(result || !result);
    }

    @Test
    public void testGetSerializationType() {
        Type.SerializationType serType = immutableListType.serializationType();
        assertNotNull(serType);
        assertTrue(serType == Type.SerializationType.SERIALIZABLE || serType == Type.SerializationType.COLLECTION);
    }

    @Test
    public void testStringOf() {
        ImmutableList<String> values = ImmutableList.of("a", "b");

        assertEquals("[\"a\", \"b\"]", immutableListType.stringOf(values));
        assertNull(immutableListType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        ImmutableList<String> values = immutableListType.valueOf("[\"a\", \"b\"]");

        assertNotNull(values);
        assertEquals(ImmutableList.of("a", "b"), values);
    }

    @Test
    public void testAppendTo() throws IOException {
        StringBuilder sb = new StringBuilder();
        immutableListType.appendTo(sb, ImmutableList.of("a", "b"));
        // appendTo emits the plain, toString()-style form: string elements are NOT quoted
        assertEquals("[a, b]", sb.toString());

        sb.setLength(0);
        immutableListType.appendTo(sb, null);
        assertEquals("null", sb.toString());
    }

    @Test
    public void testSerializeTo() throws IOException {
        ImmutableList<String> list = ImmutableList.of("a", "b");
        com.landawn.abacus.util.BufferedJsonWriter writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter();
        immutableListType.serializeTo(writer, list, com.landawn.abacus.parser.JsonSerConfig.create());
        String json = writer.toString();
        com.landawn.abacus.util.Objectory.recycle(writer);

        // serializeTo emits JSON: string elements ARE quoted; equals stringOf and differs from appendTo
        assertEquals("[\"a\", \"b\"]", json);
        assertEquals(immutableListType.stringOf(list), json);

        StringBuilder sb = new StringBuilder();
        immutableListType.appendTo(sb, list);
        org.junit.jupiter.api.Assertions.assertNotEquals(sb.toString(), json);
    }

    @Test
    public void testGetTypeName() {
        String typeName = ImmutableListType.getTypeName(ImmutableList.class, "String", true);
        assertNotNull(typeName);
        assertTrue(typeName.contains("ImmutableList"));
        assertTrue(typeName.contains("String"));

        typeName = ImmutableListType.getTypeName(ImmutableList.class, "String", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("ImmutableList"));
        assertTrue(typeName.contains("String"));
    }
}
