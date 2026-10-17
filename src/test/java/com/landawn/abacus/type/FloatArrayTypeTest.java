package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.io.StringWriter;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;

public class FloatArrayTypeTest extends TestBase {

    private final FloatArrayType type = new FloatArrayType();

    @Test
    public void test_stringOf() {
        Float[] arr = new Float[] { 1.1f, 2.2f };
        String result = type.stringOf(arr);
        assertNotNull(result);

        assertNull(type.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        Float[] result = type.valueOf("[1.1, 2.2]");
        assertNotNull(result);

        assertNull(type.valueOf((String) null));
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        Float[] arr = new Float[] { 1.1f, 2.2f };
        type.appendTo(sw, arr);
        assertNotNull(sw.toString());

        sw = new StringWriter();
        type.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_serializeTo() throws IOException {
        CharacterWriter writer = mock(BufferedJsonWriter.class);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        Float[] arr = new Float[] { 1.1f, 2.2f };
        type.serializeTo(writer, arr, config);
        verify(writer, atLeastOnce()).write(any(String.class));

        reset(writer);
        type.serializeTo(writer, null, config);
        verify(writer).write(NULL_CHAR_ARRAY);
    }

    @Test
    public void test_clazz() {
        assertEquals(Float[].class, type.javaType());
    }

    // ---- deep review 2026-10-16 G151 begin ----

    // G151 (owner: consistency, reverses G143): quoted elements are read like unquoted ones, as Long[], the primitive arrays
    // and N.fromJson do; r89 threw NumberFormatException for ["1", 2].
    @Test
    public void testValueOf_quotedElementsReadLikeUnquoted() {
        final com.landawn.abacus.type.Type<Float[]> type = com.landawn.abacus.type.TypeFactory.getType(Float[].class);

        org.junit.jupiter.api.Assertions.assertArrayEquals(new Float[] { 1.5f, null, -3f }, type.valueOf("[\"1.5\", null, \"-3\"]"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new Float[] { 1f, 2f }, type.valueOf("['1', 2]"));
        org.junit.jupiter.api.Assertions.assertArrayEquals(new Float[] { 1f }, type.valueOf("[\"\\u0031\"]")); // JSON escape
        org.junit.jupiter.api.Assertions.assertArrayEquals(com.landawn.abacus.util.N.fromJson("[\"1\", 2]", Float[].class), type.valueOf("[\"1\", 2]"));

        // null handling is unchanged: only the bare literal is a null element; empty or quoted null still fail, as for Long[]
        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> type.valueOf("[\"\"]"));
        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> type.valueOf("[\"null\"]"));
        org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> type.valueOf("[\"1']"));
    }

    // ---- deep review 2026-10-16 G151 end ----
}
