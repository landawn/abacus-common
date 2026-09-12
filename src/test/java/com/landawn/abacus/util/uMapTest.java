package com.landawn.abacus.util;

import static org.assertj.core.api.Assertions.fail;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

public class uMapTest extends uTestSupport {
    @Test
    public void testMap() throws Exception {
        u.OptionalInt present = u.OptionalInt.of(10);

        assertEquals(20, present.map(v -> v * 2).get());
        assertEquals(20L, present.mapToLong(v -> (long) v * 2).get());
        assertEquals("10", present.mapToObj(String::valueOf).get());
        assertFalse(u.OptionalInt.empty().map(v -> v * 2).isPresent());
    }

    @Test
    public void testMapAndFilter() throws Exception {
        u.Nullable<String> empty = u.Nullable.empty();
        u.Nullable<String> present = u.Nullable.of("val");
        u.Nullable<String> presentNull = u.Nullable.of(null);

        assertEquals("VAL", present.map(String::toUpperCase).get());
        assertEquals("not called", presentNull.map(v -> "not called").get());
        assertFalse(empty.map(v -> "not called").isPresent());

        assertEquals("VAL", present.mapIfNotNull(String::toUpperCase).get());
        assertFalse(presentNull.mapIfNotNull(v -> "not called").isPresent());
        assertFalse(empty.mapIfNotNull(v -> "not called").isPresent());
    }

    @Test
    public void testMapToChar() throws Exception {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);

        u.OptionalChar mapped = optional.mapToChar(value -> value ? 'T' : 'F');
        assertTrue(mapped.isPresent());
        assertEquals('T', mapped.get());

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        mapped = empty.mapToChar(value -> 'X');
        assertFalse(mapped.isPresent());
    }

    @Test
    public void testMapToInt() throws Exception {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);

        u.OptionalInt mapped = optional.mapToInt(value -> value ? 1 : 0);
        assertTrue(mapped.isPresent());
        assertEquals(1, mapped.get());

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        mapped = empty.mapToInt(value -> 99);
        assertFalse(mapped.isPresent());
    }

    @Test
    public void testMapToLong() throws Exception {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);

        u.OptionalLong mapped = optional.mapToLong(value -> value ? 1L : 0L);
        assertTrue(mapped.isPresent());
        assertEquals(1L, mapped.get());

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        mapped = empty.mapToLong(value -> 99L);
        assertFalse(mapped.isPresent());
    }

    @Test
    public void testMapToDouble() throws Exception {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);

        u.OptionalDouble mapped = optional.mapToDouble(value -> value ? 1.0 : 0.0);
        assertTrue(mapped.isPresent());
        assertEquals(1.0, mapped.get());

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        mapped = empty.mapToDouble(value -> 99.0);
        assertFalse(mapped.isPresent());
    }

    @Test
    public void testMapToObj() throws Exception {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);

        Optional<String> mapped = optional.mapToObj(value -> value ? "TRUE" : "FALSE");
        assertTrue(mapped.isPresent());
        assertEquals("TRUE", mapped.get());

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        mapped = empty.mapToObj(value -> "X");
        assertFalse(mapped.isPresent());
    }

    @Test
    public void testMapToBoolean() throws Exception {
        u.OptionalChar optional = u.OptionalChar.of('A');

        u.OptionalBoolean mapped = optional.mapToBoolean(value -> value == 'A');
        assertTrue(mapped.isPresent());
        assertTrue(mapped.get());

        u.OptionalChar empty = u.OptionalChar.empty();
        mapped = empty.mapToBoolean(value -> true);
        assertFalse(mapped.isPresent());
    }

    @Test
    public void testMapToNonNull() throws Exception {
        u.OptionalChar optional = u.OptionalChar.of('A');

        Optional<String> mapped = optional.mapToObj(value -> String.valueOf(value));
        assertTrue(mapped.isPresent());
        assertEquals("A", mapped.get());

        u.OptionalChar empty = u.OptionalChar.empty();
        mapped = empty.mapToObj(value -> "X");
        assertFalse(mapped.isPresent());
    }

    @Test
    public void testMapToFloat() throws Exception {
        u.OptionalInt optional = u.OptionalInt.of(42);

        u.OptionalFloat mapped = optional.mapToFloat(value -> value / 2.0f);
        assertTrue(mapped.isPresent());
        assertEquals(21.0f, mapped.get());

        u.OptionalInt empty = u.OptionalInt.empty();
        mapped = empty.mapToFloat(value -> 99.0f);
        assertFalse(mapped.isPresent());
    }

    @Test
    @DisplayName("Test mapToByte()")
    public void testMapToByte() throws Exception {
        u.Optional<Integer> opt = u.Optional.of(100);
        OptionalByte mapped = opt.mapToByte(Integer::byteValue);
        assertTrue(mapped.isPresent());
        assertEquals((byte) 100, mapped.get());

        u.Optional<Integer> empty = u.Optional.empty();
        OptionalByte mappedEmpty = empty.mapToByte(i -> (byte) 0);
        assertFalse(mappedEmpty.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToByte(null));
    }

    @Test
    @DisplayName("Test mapToShort()")
    public void testMapToShort() throws Exception {
        u.Optional<Integer> opt = u.Optional.of(1000);
        OptionalShort mapped = opt.mapToShort(Integer::shortValue);
        assertTrue(mapped.isPresent());
        assertEquals((short) 1000, mapped.get());

        u.Optional<Integer> empty = u.Optional.empty();
        OptionalShort mappedEmpty = empty.mapToShort(i -> (short) 0);
        assertFalse(mappedEmpty.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToShort(null));
    }

    @Test
    @DisplayName("Test mapIfNotNull()")
    public void testMapIfNotNull() throws Exception {
        u.Nullable<String> opt = u.Nullable.of("test");
        u.Nullable<Integer> mapped = opt.mapIfNotNull(String::length);
        assertTrue(mapped.isPresent());
        assertEquals(4, mapped.get());

        u.Nullable<String> nullOpt = u.Nullable.of((String) null);
        u.Nullable<Integer> mappedNull = nullOpt.mapIfNotNull(String::length);
        assertFalse(mappedNull.isPresent());

        u.Nullable<String> empty = u.Nullable.empty();
        u.Nullable<Integer> mappedEmpty = empty.mapIfNotNull(String::length);
        assertFalse(mappedEmpty.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToNonNullIfNotNull()")
    public void testMapToNonNullIfNotNull() throws Exception {
        u.Nullable<String> opt = u.Nullable.of("test");
        u.Optional<Integer> mapped = opt.mapToNonNullIfNotNull(String::length);
        assertTrue(mapped.isPresent());
        assertEquals(4, mapped.get());

        u.Nullable<String> nullOpt = u.Nullable.of((String) null);
        u.Optional<Integer> mappedNull = nullOpt.mapToNonNullIfNotNull(String::length);
        assertFalse(mappedNull.isPresent());

        assertThrows(NullPointerException.class, () -> opt.mapToNonNullIfNotNull(s -> null));

        assertThrows(IllegalArgumentException.class, () -> opt.mapToNonNullIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToBooleanIfNotNull()")
    public void testMapToBooleanIfNotNull() throws Exception {
        u.Nullable<String> opt = u.Nullable.of("test");
        OptionalBoolean mapped = opt.mapToBooleanIfNotNull(s -> s.length() > 3);
        assertTrue(mapped.isPresent());
        assertTrue(mapped.get());

        u.Nullable<String> nullOpt = u.Nullable.of((String) null);
        OptionalBoolean mappedNull = nullOpt.mapToBooleanIfNotNull(s -> true);
        assertFalse(mappedNull.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToBooleanIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToCharIfNotNull()")
    public void testMapToCharIfNotNull() throws Exception {
        u.Nullable<String> opt = u.Nullable.of("test");
        OptionalChar mapped = opt.mapToCharIfNotNull(s -> s.charAt(0));
        assertTrue(mapped.isPresent());
        assertEquals('t', mapped.get());

        u.Nullable<String> nullOpt = u.Nullable.of((String) null);
        OptionalChar mappedNull = nullOpt.mapToCharIfNotNull(s -> 'a');
        assertFalse(mappedNull.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToCharIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToByteIfNotNull()")
    public void testMapToByteIfNotNull() throws Exception {
        u.Nullable<Integer> opt = u.Nullable.of(100);
        OptionalByte mapped = opt.mapToByteIfNotNull(Integer::byteValue);
        assertTrue(mapped.isPresent());
        assertEquals((byte) 100, mapped.get());

        u.Nullable<Integer> nullOpt = u.Nullable.of((Integer) null);
        OptionalByte mappedNull = nullOpt.mapToByteIfNotNull(i -> (byte) 0);
        assertFalse(mappedNull.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToByteIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToShortIfNotNull()")
    public void testMapToShortIfNotNull() throws Exception {
        u.Nullable<Integer> opt = u.Nullable.of(1000);
        OptionalShort mapped = opt.mapToShortIfNotNull(Integer::shortValue);
        assertTrue(mapped.isPresent());
        assertEquals((short) 1000, mapped.get());

        u.Nullable<Integer> nullOpt = u.Nullable.of((Integer) null);
        OptionalShort mappedNull = nullOpt.mapToShortIfNotNull(i -> (short) 0);
        assertFalse(mappedNull.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToShortIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToIntIfNotNull()")
    public void testMapToIntIfNotNull() throws Exception {
        u.Nullable<String> opt = u.Nullable.of("test");
        OptionalInt mapped = opt.mapToIntIfNotNull(String::length);
        assertTrue(mapped.isPresent());
        assertEquals(4, mapped.getAsInt());

        u.Nullable<String> nullOpt = u.Nullable.of((String) null);
        OptionalInt mappedNull = nullOpt.mapToIntIfNotNull(s -> 0);
        assertFalse(mappedNull.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToIntIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToLongIfNotNull()")
    public void testMapToLongIfNotNull() throws Exception {
        u.Nullable<String> opt = u.Nullable.of("test");
        OptionalLong mapped = opt.mapToLongIfNotNull(s -> (long) s.length());
        assertTrue(mapped.isPresent());
        assertEquals(4L, mapped.getAsLong());

        u.Nullable<String> nullOpt = u.Nullable.of((String) null);
        OptionalLong mappedNull = nullOpt.mapToLongIfNotNull(s -> 0L);
        assertFalse(mappedNull.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToLongIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToFloatIfNotNull()")
    public void testMapToFloatIfNotNull() throws Exception {
        u.Nullable<Integer> opt = u.Nullable.of(10);
        OptionalFloat mapped = opt.mapToFloatIfNotNull(i -> i / 2.0f);
        assertTrue(mapped.isPresent());
        assertEquals(5.0f, mapped.get());

        u.Nullable<Integer> nullOpt = u.Nullable.of((Integer) null);
        OptionalFloat mappedNull = nullOpt.mapToFloatIfNotNull(i -> 0.0f);
        assertFalse(mappedNull.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToFloatIfNotNull(null));
    }

    @Test
    @DisplayName("Test mapToDoubleIfNotNull()")
    public void testMapToDoubleIfNotNull() throws Exception {
        u.Nullable<Integer> opt = u.Nullable.of(10);
        OptionalDouble mapped = opt.mapToDoubleIfNotNull(i -> i / 2.0);
        assertTrue(mapped.isPresent());
        assertEquals(5.0, mapped.getAsDouble());

        u.Nullable<Integer> nullOpt = u.Nullable.of((Integer) null);
        OptionalDouble mappedNull = nullOpt.mapToDoubleIfNotNull(i -> 0.0);
        assertFalse(mappedNull.isPresent());

        assertThrows(IllegalArgumentException.class, () -> opt.mapToDoubleIfNotNull(null));
    }

    @Test
    public void testMapToPrimitives() throws Exception {
        Optional<String> opt = Optional.of("test");
        Optional<String> empty = Optional.empty();

        u.OptionalBoolean boolOpt = opt.mapToBoolean(s -> s.length() > 3);
        assertTrue(boolOpt.isPresent());
        assertTrue(boolOpt.get());
        assertFalse(empty.mapToBoolean(s -> true).isPresent());

        u.OptionalChar charOpt = opt.mapToChar(s -> s.charAt(0));
        assertTrue(charOpt.isPresent());
        assertEquals('t', charOpt.get());
        assertFalse(empty.mapToChar(s -> 'a').isPresent());

        u.OptionalByte byteOpt = opt.mapToByte(s -> (byte) s.length());
        assertTrue(byteOpt.isPresent());
        assertEquals((byte) 4, byteOpt.get());
        assertFalse(empty.mapToByte(s -> (byte) 1).isPresent());

        u.OptionalShort shortOpt = opt.mapToShort(s -> (short) s.length());
        assertTrue(shortOpt.isPresent());
        assertEquals((short) 4, shortOpt.get());
        assertFalse(empty.mapToShort(s -> (short) 1).isPresent());

        u.OptionalInt intOpt = opt.mapToInt(String::length);
        assertTrue(intOpt.isPresent());
        assertEquals(4, intOpt.get());
        assertFalse(empty.mapToInt(String::length).isPresent());

        u.OptionalLong longOpt = opt.mapToLong(s -> (long) s.length());
        assertTrue(longOpt.isPresent());
        assertEquals(4L, longOpt.get());
        assertFalse(empty.mapToLong(s -> 1L).isPresent());

        u.OptionalFloat floatOpt = opt.mapToFloat(s -> (float) s.length());
        assertTrue(floatOpt.isPresent());
        assertEquals(4.0f, floatOpt.get());
        assertFalse(empty.mapToFloat(s -> 1.0f).isPresent());

        u.OptionalDouble doubleOpt = opt.mapToDouble(s -> (double) s.length());
        assertTrue(doubleOpt.isPresent());
        assertEquals(4.0, doubleOpt.get());
        assertFalse(empty.mapToDouble(s -> 1.0).isPresent());
    }

    @Test
    public void testMapToPrimitivesIfNotNull() throws Exception {
        Nullable<String> nullable = Nullable.of("test");
        Nullable<String> nullValue = Nullable.of((String) null);

        assertTrue(nullable.mapToBooleanIfNotNull(s -> s.length() > 3).isPresent());
        assertTrue(nullable.mapToCharIfNotNull(s -> s.charAt(0)).isPresent());
        assertTrue(nullable.mapToByteIfNotNull(s -> (byte) s.length()).isPresent());
        assertTrue(nullable.mapToShortIfNotNull(s -> (short) s.length()).isPresent());
        assertTrue(nullable.mapToIntIfNotNull(String::length).isPresent());
        assertTrue(nullable.mapToLongIfNotNull(s -> (long) s.length()).isPresent());
        assertTrue(nullable.mapToFloatIfNotNull(s -> (float) s.length()).isPresent());
        assertTrue(nullable.mapToDoubleIfNotNull(s -> (double) s.length()).isPresent());

        assertFalse(nullValue.mapToBooleanIfNotNull(s -> {
            fail("Should not be called");
            return true;
        }).isPresent());
        assertFalse(nullValue.mapToCharIfNotNull(s -> {
            fail("Should not be called");
            return 'a';
        }).isPresent());
        assertFalse(nullValue.mapToByteIfNotNull(s -> {
            fail("Should not be called");
            return (byte) 1;
        }).isPresent());
        assertFalse(nullValue.mapToShortIfNotNull(s -> {
            fail("Should not be called");
            return (short) 1;
        }).isPresent());
        assertFalse(nullValue.mapToIntIfNotNull(s -> {
            fail("Should not be called");
            return 1;
        }).isPresent());
        assertFalse(nullValue.mapToLongIfNotNull(s -> {
            fail("Should not be called");
            return 1L;
        }).isPresent());
        assertFalse(nullValue.mapToFloatIfNotNull(s -> {
            fail("Should not be called");
            return 1.0f;
        }).isPresent());
        assertFalse(nullValue.mapToDoubleIfNotNull(s -> {
            fail("Should not be called");
            return 1.0;
        }).isPresent());
    }
}
