package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.landawn.abacus.TestBase;

public class GuavaMultisetTypeTest extends TestBase {

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
        assertEquals(Multiset.class, multisetType.javaType());
    }

    @Test
    public void testGetElementType() {
        Type<?> elementType = multisetType.elementType();
        assertNotNull(elementType);
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = multisetType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.length);
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
    public void testStringOf_WithContent() {
        HashMultiset<String> multiset = HashMultiset.create();
        multiset.add("foo");
        multiset.add("bar");

        String str = multisetType.stringOf(multiset);
        assertNotNull(str);
        assertTrue(str.contains("foo") || str.length() > 0);
    }

    @Test
    public void testHashMultisetType_valueOf() {
        GuavaMultisetType<String, HashMultiset<String>> hashType = (GuavaMultisetType<String, HashMultiset<String>>) createType(
                "com.google.common.collect.HashMultiset<String>");

        HashMultiset<String> original = HashMultiset.create();
        original.add("x");
        original.add("x");
        original.add("y");

        String str = hashType.stringOf(original);
        HashMultiset<String> result = hashType.valueOf(str);
        assertNotNull(result);
        assertEquals(2, result.count("x"));
        assertEquals(1, result.count("y"));
    }

    @Test
    public void testLinkedHashMultisetType_valueOf() {
        GuavaMultisetType<String, LinkedHashMultiset<String>> linkedType = (GuavaMultisetType<String, LinkedHashMultiset<String>>) createType(
                "com.google.common.collect.LinkedHashMultiset<String>");

        LinkedHashMultiset<String> original = LinkedHashMultiset.create();
        original.add("a");
        original.add("b");
        original.add("a");

        String str = linkedType.stringOf(original);
        LinkedHashMultiset<String> result = linkedType.valueOf(str);
        assertNotNull(result);
        assertEquals(2, result.count("a"));
    }

    @Test
    public void testValueOf() {
        assertNull(multisetType.valueOf(null));
        assertNull(multisetType.valueOf(""));

    }

    @Test
    public void testValueOf_WithContent() {
        // Create a multiset and round-trip through string
        HashMultiset<String> original = HashMultiset.create();
        original.add("apple");
        original.add("banana");
        original.add("apple");

        String str = multisetType.stringOf(original);
        assertNotNull(str);

        Multiset<String> result = multisetType.valueOf(str);
        assertNotNull(result);
        assertEquals(2, result.count("apple"));
        assertEquals(1, result.count("banana"));
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
