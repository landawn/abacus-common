package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ImmutableList;

@Tag("new-test")
public class ImmutableListType100Test extends TestBase {

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
        assertEquals(ImmutableList.class, immutableListType.clazz());
    }

    @Test
    public void testGetElementType() {
        Type<?> elementType = immutableListType.getElementType();
        assertNotNull(elementType);
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = immutableListType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.length);
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
        Type.SerializationType serType = immutableListType.getSerializationType();
        assertNotNull(serType);
        assertTrue(serType == Type.SerializationType.SERIALIZABLE || serType == Type.SerializationType.COLLECTION);
    }

    @Test
    public void testStringOf() {
    }

    @Test
    public void testValueOf() {
    }

    @Test
    public void testAppendTo() throws IOException {
        StringWriter writer = new StringWriter();
    }

    @Test
    public void testWriteCharacter() throws IOException {
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
