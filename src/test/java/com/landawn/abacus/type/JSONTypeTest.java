package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class JSONTypeTest extends TestBase {

    private JSONType<Map> jsonMapType;
    private JSONType<List> jsonListType;
    private JSONType<TestClass> jsonCustomType;

    @BeforeEach
    public void setUp() {
        jsonMapType = (JSONType<Map>) createType("JSON<Map>");
        jsonListType = (JSONType<List>) createType("JSON<List>");
        jsonCustomType = (JSONType<TestClass>) createType("JSON<com.landawn.abacus.type.JSONTypeTest$TestClass>");
    }

    public static class TestClass {
        private String field1;
        private int field2;

        public String getField1() {
            return field1;
        }

        public void setField1(String field1) {
            this.field1 = field1;
        }

        public int getField2() {
            return field2;
        }

        public void setField2(int field2) {
            this.field2 = field2;
        }
    }

    @Test
    public void testDeclaringName() {
        assertNotNull(jsonMapType.declaringName());
        assertTrue(jsonMapType.declaringName().contains("JSON"));
    }

    @Test
    public void testClazz_Map() {
        assertEquals(Map.class, jsonMapType.javaType());
    }

    @Test
    public void testClazz_List() {
        assertEquals(List.class, jsonListType.javaType());
    }

    @Test
    public void testStringOf_Null() {
        assertNull(jsonMapType.stringOf(null));
        assertNull(jsonListType.stringOf(null));
        assertNull(jsonCustomType.stringOf(null));
    }

    @Test
    public void testValueOf_Null() {
        assertNull(jsonMapType.valueOf(null));
        assertNull(jsonListType.valueOf(null));
        assertNull(jsonCustomType.valueOf(null));
    }

    @Test
    public void testValueOf_EmptyString() {
        assertNull(jsonMapType.valueOf(""));
        assertNull(jsonListType.valueOf(""));
        assertNull(jsonCustomType.valueOf(""));
    }

    @Test
    public void testNestedGenericTargetIsPreserved() {
        JSONType<List<Long>> type = (JSONType<List<Long>>) createType("JSON<List<Long>>");

        List<Long> parsed = type.valueOf("[1, 2]");

        assertTrue(type.isParameterizedType());
        assertEquals("List<Long>", type.parameterTypes().get(0).declaringName());
        assertEquals(Long.class, parsed.get(0).getClass());
        assertEquals(List.of(1L, 2L), parsed);
    }

    // ---- review fixes 2026-09-06, T2-07: blank input is null, like XMLType and the JSON-backed siblings ----

    @Test
    public void reviewFixes20260906_blankStringIsNull() {
        // Before the fix "  " reached the parser and materialised an empty HashMap.
        assertNull(jsonMapType.valueOf("  "));
        assertNull(jsonMapType.valueOf("\t\n"));
        assertNull(jsonListType.valueOf("  "));
        assertNull(jsonCustomType.valueOf("   "));
        // Non-blank input is still parsed.
        assertEquals(1, jsonMapType.valueOf(" {\"a\": 1} ").size());
    }

    // ---- T2-12: the declaring name expands a raw Map argument ----

    @Test
    public void reviewFixes20260906_declaringNameExpandsRawMap() {
        assertEquals("JSON<Map<Object, Object>>", jsonMapType.declaringName());
        assertEquals("JSON<com.landawn.abacus.type.JSONTypeTest.TestClass>", jsonCustomType.declaringName());
    }
}
