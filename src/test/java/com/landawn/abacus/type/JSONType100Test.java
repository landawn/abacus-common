package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class JSONType100Test extends TestBase {

    private JSONType<Map> jsonMapType;
    private JSONType<List> jsonListType;
    private JSONType<TestClass> jsonCustomType;

    @BeforeEach
    public void setUp() {
        jsonMapType = (JSONType<Map>) createType("JSON<Map>");
        jsonListType = (JSONType<List>) createType("JSON<List>");
        jsonCustomType = (JSONType<TestClass>) createType("JSON<com.landawn.abacus.type.JSONType100Test$TestClass>");
    }

    @Test
    public void testClazz_Map() {
        assertEquals(Map.class, jsonMapType.clazz());
    }

    @Test
    public void testClazz_List() {
        assertEquals(List.class, jsonListType.clazz());
    }

    @Test
    public void testDeclaringName() {
        assertNotNull(jsonMapType.declaringName());
        assertTrue(jsonMapType.declaringName().contains("JSON"));
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
}
