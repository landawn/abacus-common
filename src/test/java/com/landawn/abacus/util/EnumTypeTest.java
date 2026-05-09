package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class EnumTypeTest extends TestBase {

    @Test
    public void testValuesContainsExpectedConstants() {
        EnumType[] values = EnumType.values();
        assertEquals(3, values.length);
        assertSame(EnumType.NAME, values[0]);
        assertSame(EnumType.ORDINAL, values[1]);
        assertSame(EnumType.CODE, values[2]);
    }

    @Test
    public void testValueOf_byName() {
        assertSame(EnumType.NAME, EnumType.valueOf("NAME"));
        assertSame(EnumType.ORDINAL, EnumType.valueOf("ORDINAL"));
        assertSame(EnumType.CODE, EnumType.valueOf("CODE"));
    }

    @Test
    public void testValueOf_unknownNameThrows() {
        assertThrows(IllegalArgumentException.class, () -> EnumType.valueOf("DOES_NOT_EXIST"));
    }

    @Test
    public void testValueOf_nullThrows() {
        assertThrows(NullPointerException.class, () -> EnumType.valueOf(null));
    }

    @Test
    public void testOrdinalAndNameContractStable() {
        // The order of these constants is part of the public contract because
        // ORDINAL persistence keys off of the declaration order.
        assertEquals(0, EnumType.NAME.ordinal());
        assertEquals(1, EnumType.ORDINAL.ordinal());
        assertEquals(2, EnumType.CODE.ordinal());

        assertEquals("NAME", EnumType.NAME.name());
        assertEquals("ORDINAL", EnumType.ORDINAL.name());
        assertEquals("CODE", EnumType.CODE.name());
    }

    @Test
    public void testToStringMatchesName() {
        // toString() defaults to name() for enums; persistence by NAME relies on this.
        assertEquals("NAME", EnumType.NAME.toString());
        assertEquals("ORDINAL", EnumType.ORDINAL.toString());
        assertEquals("CODE", EnumType.CODE.toString());
    }

    @Test
    public void testEqualityAndHashCodeIdentitySemantics() {
        // Enums are singletons – equals is identity, hashCode comes from Object.
        assertTrue(EnumType.NAME == EnumType.valueOf("NAME"));
        assertEquals(EnumType.NAME, EnumType.valueOf("NAME"));
        assertEquals(EnumType.NAME.hashCode(), EnumType.valueOf("NAME").hashCode());
    }

    @Test
    public void testCompareToFollowsDeclarationOrder() {
        assertTrue(EnumType.NAME.compareTo(EnumType.ORDINAL) < 0);
        assertTrue(EnumType.ORDINAL.compareTo(EnumType.CODE) < 0);
        assertTrue(EnumType.CODE.compareTo(EnumType.NAME) > 0);
        assertEquals(0, EnumType.NAME.compareTo(EnumType.NAME));
    }

    @Test
    public void testGetDeclaringClass() {
        assertNotNull(EnumType.NAME.getDeclaringClass());
        assertSame(EnumType.class, EnumType.NAME.getDeclaringClass());
    }
}
