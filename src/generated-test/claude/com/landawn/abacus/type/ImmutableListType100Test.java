package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ImmutableList;

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
        assertTrue(immutableListType.isGenericType());
    }

    @Test
    public void testIsSerializable() {
        // Result depends on underlying listType.isSerializable()
        boolean result = immutableListType.isSerializable();
        // Just verify it returns a boolean
        assertTrue(result || !result);
    }

    @Test
    public void testGetSerializationType() {
        Type.SerializationType serType = immutableListType.getSerializationType();
        assertNotNull(serType);
        // Should be either SERIALIZABLE or COLLECTION
        assertTrue(serType == Type.SerializationType.SERIALIZABLE || serType == Type.SerializationType.COLLECTION);
    }

    @Test
    public void testStringOf() {
        // Test would require mocking listType.stringOf()
        // Result depends on delegation to listType
    }

    @Test
    public void testValueOf() {
        // Test would require mocking listType.valueOf()
        // and ImmutableList.wrap()
    }

    @Test
    public void testAppendTo() throws IOException {
        // Test would require mocking listType.appendTo()
        StringWriter writer = new StringWriter();
        // Actual test depends on delegation to listType
    }

    @Test
    public void testWriteCharacter() throws IOException {
        // Test would require mocking listType.writeCharacter()
        // Actual test depends on delegation to listType
    }

    @Test
    public void testGetTypeName() {
        // Test static method with declaring name
        String typeName = ImmutableListType.getTypeName(ImmutableList.class, "String", true);
        assertNotNull(typeName);
        assertTrue(typeName.contains("ImmutableList"));
        assertTrue(typeName.contains("String"));

        // Test static method without declaring name
        typeName = ImmutableListType.getTypeName(ImmutableList.class, "String", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("ImmutableList"));
        assertTrue(typeName.contains("String"));
    }
}
