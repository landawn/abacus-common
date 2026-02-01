package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.N;

@Tag("new-test")
public class MultisetType100Test extends TestBase {

    private MultisetType<String> multisetType;

    @BeforeEach
    public void setUp() {
        multisetType = (MultisetType<String>) createType("Multiset<String>");
    }

    @Test
    public void testDeclaringName() {
        String declaringName = multisetType.declaringName();
        Assertions.assertNotNull(declaringName);
        Assertions.assertTrue(declaringName.contains("Multiset"));
    }

    @Test
    public void testClazz() {
        Class<Multiset<String>> clazz = multisetType.clazz();
        Assertions.assertNotNull(clazz);
        assertEquals(Multiset.class, clazz);
    }

    @Test
    public void testGetElementType() {
        Type<String> elementType = multisetType.getElementType();
        Assertions.assertNotNull(elementType);
    }

    @Test
    public void testGetParameterTypes() {
        Type<String>[] paramTypes = multisetType.getParameterTypes();
        Assertions.assertNotNull(paramTypes);
        assertEquals(1, paramTypes.length);
    }

    @Test
    public void testIsGenericType() {
        boolean isGeneric = multisetType.isParameterizedType();
        Assertions.assertTrue(isGeneric);
    }

    @Test
    public void testIsSerializable() {
        boolean isSerializable = multisetType.isSerializable();
        Assertions.assertTrue(isSerializable);
    }

    @Test
    public void testStringOfNull() {
        String result = multisetType.stringOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testStringOfNonNull() {
        Multiset<String> multiset = N.newMultiset();
        multiset.add("apple", 2);
        multiset.add("banana", 3);

        String result = multisetType.stringOf(multiset);
        Assertions.assertNotNull(result);
        Assertions.assertTrue(result.contains("apple"));
        Assertions.assertTrue(result.contains("banana"));
        Assertions.assertTrue(result.contains("2"));
        Assertions.assertTrue(result.contains("3"));
    }

    @Test
    public void testValueOfNull() {
        Multiset<String> result = multisetType.valueOf(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfEmptyString() {
        Multiset<String> result = multisetType.valueOf("");
        Assertions.assertNull(result);
    }

    @Test
    public void testValueOfValidJson() {
        Multiset<String> result = multisetType.valueOf("{\"apple\":2,\"banana\":3}");
        Assertions.assertNotNull(result);
        assertEquals(2, result.count("apple"));
        assertEquals(3, result.count("banana"));
    }

}
