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

public class ShortArrayTypeTest extends TestBase {

    private final ShortArrayType shortArrayType = new ShortArrayType();

    @Test
    public void test_stringOf() {
        Short[] arr = new Short[] { (short) 1, (short) 2 };
        String result = shortArrayType.stringOf(arr);
        assertNotNull(result);

        assertNull(shortArrayType.stringOf(null));
    }

    @Test
    public void test_valueOf_String() {
        Short[] result = shortArrayType.valueOf("[1, 2]");
        assertNotNull(result);

        assertNull(shortArrayType.valueOf((String) null));
    }

    @Test
    public void test_appendTo() throws IOException {
        StringWriter sw = new StringWriter();

        Short[] arr = new Short[] { (short) 1, (short) 2 };
        shortArrayType.appendTo(sw, arr);
        assertNotNull(sw.toString());

        sw = new StringWriter();
        shortArrayType.appendTo(sw, null);
        assertEquals("null", sw.toString());
    }

    @Test
    public void test_writeCharacter() throws IOException {
        CharacterWriter writer = mock(BufferedJsonWriter.class);
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);

        Short[] arr = new Short[] { (short) 1, (short) 2 };
        shortArrayType.writeCharacter(writer, arr, config);
        verify(writer, atLeastOnce()).write(any(String.class));

        reset(writer);
        shortArrayType.writeCharacter(writer, null, config);
        verify(writer).write(NULL_CHAR_ARRAY);
    }

    @Test
    public void test_clazz() {
        assertEquals(Short[].class, shortArrayType.javaType());
    }

    @Test
    public void test_name() {
        assertNotNull(shortArrayType.name());
    }

}
