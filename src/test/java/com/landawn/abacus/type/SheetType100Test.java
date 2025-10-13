package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Sheet;

@Tag("new-test")
public class SheetType100Test extends TestBase {

    private SheetType<String, String, Integer> sheetType;

    @BeforeEach
    public void setUp() {
        sheetType = new SheetType<>("String", "String", "Integer");
    }

    @Test
    public void testDeclaringName() {
        assertNotNull(sheetType.declaringName());
        assertTrue(sheetType.declaringName().contains("Sheet"));
    }

    @Test
    public void testClazz() {
        assertEquals(Sheet.class, sheetType.clazz());
    }

    @Test
    public void testGetParameterTypes() {
        Type<?>[] paramTypes = sheetType.getParameterTypes();
        assertNotNull(paramTypes);
        assertEquals(3, paramTypes.length);
    }

    @Test
    public void testIsGenericType() {
        assertTrue(sheetType.isGenericType());
    }

    @Test
    public void testIsSerializable() {
        assertFalse(sheetType.isSerializable());
    }

    @Test
    public void testGetSerializationType() {
        assertEquals(Type.SerializationType.SHEET, sheetType.getSerializationType());
    }

    @Test
    public void testStringOf() {
        assertNull(sheetType.stringOf(null));

        Sheet<String, String, Object> sheet = Sheet.rows(N.asList("r1", "r2", "r3"), N.asList("c1", "c2"),
                new Object[][] { { 1, "a" }, { null, "b" }, { 5, "c" } });

        String result = sheetType.stringOf(sheet);
        assertNotNull(result);
    }

    @Test
    public void testValueOf() {
        assertNull(sheetType.valueOf(null));
        assertNull(sheetType.valueOf(""));
        assertNull(sheetType.valueOf(" "));
    }

    @Test
    public void testGetTypeName() {
        String typeName = SheetType.getTypeName(Sheet.class, "String", "String", "Integer", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Sheet"));

        String declaringName = SheetType.getTypeName(Sheet.class, "String", "String", "Integer", true);
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Sheet"));
    }
}
