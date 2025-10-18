package com.landawn.abacus.parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class Exclusion100Test extends TestBase {

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
