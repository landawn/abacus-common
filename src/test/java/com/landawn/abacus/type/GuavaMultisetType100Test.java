package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Multiset;
import com.landawn.abacus.TestBase;

@Tag("new-test")
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
        assertTrue(multisetType.isParameterizedType());
    }

    @Test
    public void testIsSerializable() {
        assertTrue(multisetType.isSerializable());
    }

    @Test
    public void testStringOf() {
        assertNull(multisetType.stringOf(null));

    }

    @Test
    public void testValueOf() {
        assertNull(multisetType.valueOf(null));
        assertNull(multisetType.valueOf(""));

    }

    @Test
    public void testGetTypeName() {
        String typeName = GuavaMultisetType.getTypeName(Multiset.class, "String", true);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Multiset"));

        typeName = GuavaMultisetType.getTypeName(Multiset.class, "String", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Multiset"));
    }
}
