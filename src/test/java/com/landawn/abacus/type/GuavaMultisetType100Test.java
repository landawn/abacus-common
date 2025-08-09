package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Multiset;
import com.landawn.abacus.TestBase;

public class GuavaMultisetType100Test extends TestBase {

    private GuavaMultisetType<String, Multiset<String>> multisetType;

    @BeforeEach
    public void setUp() {
        multisetType = (GuavaMultisetType<String, Multiset<String>>) createType("com.google.common.collect.Multiset<String>");
    }

    @Test
    public void testDeclaringName() {
        String declaringName = multisetType.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Multiset"));
        assertTrue(declaringName.contains("String"));
    }

    @Test
    public void testClazz() {
        assertEquals(Multiset.class, multisetType.clazz());
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = multisetType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.length);
    }

    @Test
    public void testGetElementType() {
        Type<?> elementType = multisetType.getElementType();
        assertNotNull(elementType);
    }

    @Test
    public void testIsGenericType() {
        assertTrue(multisetType.isGenericType());
    }

    @Test
    public void testIsSerializable() {
        assertTrue(multisetType.isSerializable());
    }

    @Test
    public void testStringOf() {
        // Test with null
        assertNull(multisetType.stringOf(null));

        // Test with actual Multiset would require creating a multiset instance
        // and mocking Utils.jsonParser
    }

    @Test
    public void testValueOf() {
        // Test with null and empty string
        assertNull(multisetType.valueOf(null));
        assertNull(multisetType.valueOf(""));

        // Test with JSON string would require mocking Utils.jsonParser
        // and testing the multiset creation logic
    }

    @Test
    public void testGetTypeName() {
        // Test static method with declaring name
        String typeName = GuavaMultisetType.getTypeName(Multiset.class, "String", true);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Multiset"));

        // Test static method without declaring name
        typeName = GuavaMultisetType.getTypeName(Multiset.class, "String", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Multiset"));
    }
}
