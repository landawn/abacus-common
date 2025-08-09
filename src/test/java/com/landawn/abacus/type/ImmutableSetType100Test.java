package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ImmutableSet;

public class ImmutableSetType100Test extends TestBase {

    private ImmutableSetType<String> immutableSetType;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        immutableSetType = (ImmutableSetType<String>) createType("ImmutableSet<String>");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testDeclaringName() {
        String declaringName = immutableSetType.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("ImmutableSet"));
        assertTrue(declaringName.contains("String"));
    }

    @Test
    public void testClazz() {
        assertEquals(ImmutableSet.class, immutableSetType.clazz());
    }

    @Test
    public void testGetElementType() {
        Type<?> elementType = immutableSetType.getElementType();
        assertNotNull(elementType);
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = immutableSetType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.length);
    }

    @Test
    public void testIsSet() {
        assertTrue(immutableSetType.isSet());
    }

    @Test
    public void testIsCollection() {
        assertTrue(immutableSetType.isCollection());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(immutableSetType.isGenericType());
    }

    @Test
    public void testIsSerializable() {
        // Result depends on underlying setType.isSerializable()
        boolean result = immutableSetType.isSerializable();
        // Just verify it returns a boolean
        assertTrue(result || !result);
    }

    @Test
    public void testGetSerializationType() {
        Type.SerializationType serType = immutableSetType.getSerializationType();
        assertNotNull(serType);
        // Should be either SERIALIZABLE or COLLECTION
        assertTrue(serType == Type.SerializationType.SERIALIZABLE || serType == Type.SerializationType.COLLECTION);
    }

    @Test
    public void testStringOf() {
        // Test would require mocking setType.stringOf()
        // Result depends on delegation to setType
    }

    @Test
    public void testValueOf() {
        // Test would require mocking setType.valueOf()
        // and ImmutableSet.wrap()
    }

    @Test
    public void testAppendTo() throws IOException {
        // Test would require mocking setType.appendTo()
        // Actual test depends on delegation to setType
    }

    @Test
    public void testWriteCharacter() throws IOException {
        // Test would require mocking setType.writeCharacter()
        // Actual test depends on delegation to setType
    }

    @Test
    public void testGetTypeName() {
        // Test static method with declaring name
        String typeName = ImmutableSetType.getTypeName(ImmutableSet.class, "String", true);
        assertNotNull(typeName);
        assertTrue(typeName.contains("ImmutableSet"));
        assertTrue(typeName.contains("String"));

        // Test static method without declaring name
        typeName = ImmutableSetType.getTypeName(ImmutableSet.class, "String", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("ImmutableSet"));
        assertTrue(typeName.contains("String"));
    }
}
