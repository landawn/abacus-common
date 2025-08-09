package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.Multimap;
import com.landawn.abacus.TestBase;

public class GuavaMultimapType100Test extends TestBase {

    private GuavaMultimapType<String, Integer, Multimap<String, Integer>> multimapType;

    @BeforeEach
    public void setUp() {
        multimapType = (GuavaMultimapType<String, Integer, Multimap<String, Integer>>) createType("com.google.common.collect.Multimap<String, Integer>");
    }

    @Test
    public void testDeclaringName() {
        String declaringName = multimapType.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Multimap"));
        assertTrue(declaringName.contains("String"));
        assertTrue(declaringName.contains("Integer"));
    }

    @Test
    public void testClazz() {
        assertEquals(Multimap.class, multimapType.clazz());
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = multimapType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(2, paramTypes.length);
    }

    @Test
    public void testIsGenericType() {
        assertTrue(multimapType.isGenericType());
    }

    @Test
    public void testIsSerializable() {
        assertTrue(multimapType.isSerializable());
    }

    @Test
    public void testStringOf() {
        // Test with null
        assertNull(multimapType.stringOf(null));

        // Test with actual Multimap would require creating a multimap instance
        // and mocking Utils.jsonParser
    }

    @Test
    public void testValueOf() {
        // Test with null and empty string
        assertNull(multimapType.valueOf(null));
        assertNull(multimapType.valueOf(""));

        // Test with JSON string would require mocking Utils.jsonParser
        // and testing the multimap creation logic
    }

    @Test
    public void testGetTypeName() {
        // Test static method with declaring name
        String typeName = GuavaMultimapType.getTypeName(Multimap.class, "String", "Integer", true);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Multimap"));

        // Test static method without declaring name
        typeName = GuavaMultimapType.getTypeName(Multimap.class, "String", "Integer", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Multimap"));
    }
}
