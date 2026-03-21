package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ExclusionTest extends TestBase {

    @Test
    public void test_values() {
        Exclusion[] values = Exclusion.values();
        assertNotNull(values);
        assertEquals(3, values.length);
    }

    @Test
    public void test_valueOf_NULL() {
        Exclusion exclusion = Exclusion.valueOf("NULL");
        assertEquals(Exclusion.NULL, exclusion);
    }

    @Test
    public void test_valueOf_DEFAULT() {
        Exclusion exclusion = Exclusion.valueOf("DEFAULT");
        assertEquals(Exclusion.DEFAULT, exclusion);
    }

    @Test
    public void test_valueOf_NONE() {
        Exclusion exclusion = Exclusion.valueOf("NONE");
        assertEquals(Exclusion.NONE, exclusion);
    }

    @Test
    public void test_NULL_value() {
        assertEquals("NULL", Exclusion.NULL.toString());
        assertEquals(Exclusion.NULL, Exclusion.values()[0]);
    }

    @Test
    public void test_DEFAULT_value() {
        assertEquals("DEFAULT", Exclusion.DEFAULT.toString());
        assertEquals(Exclusion.DEFAULT, Exclusion.values()[1]);
    }

    @Test
    public void test_NONE_value() {
        assertEquals("NONE", Exclusion.NONE.toString());
        assertEquals(Exclusion.NONE, Exclusion.values()[2]);
    }

    @Test
    public void test_ordinal_NULL() {
        assertEquals(0, Exclusion.NULL.ordinal());
    }

    @Test
    public void test_ordinal_DEFAULT() {
        assertEquals(1, Exclusion.DEFAULT.ordinal());
    }

    @Test
    public void test_ordinal_NONE() {
        assertEquals(2, Exclusion.NONE.ordinal());
    }

    @Test
    public void test_name_NULL() {
        assertEquals("NULL", Exclusion.NULL.name());
    }

    @Test
    public void test_name_DEFAULT() {
        assertEquals("DEFAULT", Exclusion.DEFAULT.name());
    }

    @Test
    public void test_name_NONE() {
        assertEquals("NONE", Exclusion.NONE.name());
    }

    @Test
    public void testEnumValues() {
        Exclusion[] values = Exclusion.values();
        Assertions.assertEquals(3, values.length);

        Assertions.assertEquals(Exclusion.NULL, values[0]);
        Assertions.assertEquals(Exclusion.DEFAULT, values[1]);
        Assertions.assertEquals(Exclusion.NONE, values[2]);
    }

    @Test
    public void testValueOf() {
        Assertions.assertEquals(Exclusion.NULL, Exclusion.valueOf("NULL"));
        Assertions.assertEquals(Exclusion.DEFAULT, Exclusion.valueOf("DEFAULT"));
        Assertions.assertEquals(Exclusion.NONE, Exclusion.valueOf("NONE"));

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            Exclusion.valueOf("INVALID");
        });
    }

    @Test
    public void testEnumName() {
        Assertions.assertEquals("NULL", Exclusion.NULL.name());
        Assertions.assertEquals("DEFAULT", Exclusion.DEFAULT.name());
        Assertions.assertEquals("NONE", Exclusion.NONE.name());
    }

    @Test
    public void testEnumOrdinal() {
        Assertions.assertEquals(0, Exclusion.NULL.ordinal());
        Assertions.assertEquals(1, Exclusion.DEFAULT.ordinal());
        Assertions.assertEquals(2, Exclusion.NONE.ordinal());
    }

    @Test
    public void testEnumToString() {
        Assertions.assertEquals("NULL", Exclusion.NULL.toString());
        Assertions.assertEquals("DEFAULT", Exclusion.DEFAULT.toString());
        Assertions.assertEquals("NONE", Exclusion.NONE.toString());
    }

}
