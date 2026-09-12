package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

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

public class uOrTest extends uTestSupport {
    @Test
    public void testOrElseThrow() {
        u.Optional<String> present = u.Optional.of("val");
        u.Optional<String> empty = u.Optional.empty();

        assertEquals("val", present.orElseThrow());
        assertThrows(NoSuchElementException.class, empty::orElseThrow);

        assertEquals("val", present.orElseThrow(RuntimeException::new));
        assertThrows(RuntimeException.class, () -> empty.orElseThrow(RuntimeException::new));

        Exception e = assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("error msg %s", "param"));
        assertEquals("error msg param", e.getMessage());
    }

    @Test
    public void testOr() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        u.OptionalBoolean result = optional.or(() -> u.OptionalBoolean.of(false));
        assertTrue(result.isPresent());
        assertTrue(result.get());

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        result = empty.or(() -> u.OptionalBoolean.of(false));
        assertTrue(result.isPresent());
        assertFalse(result.get());
    }

    @Test
    public void testOrElse() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        assertTrue(optional.orElse(false));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        assertFalse(empty.orElse(false));
        assertTrue(empty.orElse(true));
    }

    @Test
    public void testOrElseGet() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        assertTrue(optional.orElseGet(() -> false));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        assertFalse(empty.orElseGet(() -> false));
        assertTrue(empty.orElseGet(() -> true));
    }

    @Test
    public void testOrElseThrowWithMessage() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        assertTrue(optional.orElseThrow("Error"));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Custom error"));
        assertEquals("Custom error", ex.getMessage());
    }

    @Test
    public void testOrElseThrowWithMessageAndParam() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        assertTrue(optional.orElseThrow("Error %s", "param"));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s", "param"));
    }

    @Test
    public void testOrElseThrowWithMessageAndTwoParams() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        assertTrue(optional.orElseThrow("Error %s %s", "p1", "p2"));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", "p1", "p2"));
    }

    @Test
    public void testOrElseThrowWithMessageAndThreeParams() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        assertTrue(optional.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
    }

    @Test
    public void testOrElseThrowWithMessageAndParamsArray() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        assertTrue(optional.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));
    }

    @Test
    public void testOrElseThrowWithSupplier() {
        u.OptionalBoolean optional = u.OptionalBoolean.of(true);
        assertTrue(optional.orElseThrow(() -> new IllegalStateException()));

        u.OptionalBoolean empty = u.OptionalBoolean.empty();
        assertThrows(IllegalStateException.class, () -> empty.orElseThrow(() -> new IllegalStateException("Custom")));
    }

    @Test
    public void testOrElseZero() {
        u.OptionalChar optional = u.OptionalChar.of('A');
        assertEquals('A', optional.orElseZero());

        u.OptionalChar empty = u.OptionalChar.empty();
        assertEquals((char) 0, empty.orElseZero());
    }

    @Test
    public void testOrElseThrowWithMessageAndParams() {
        u.OptionalChar optional = u.OptionalChar.of('A');
        assertEquals('A', optional.orElseThrow("Error %s", "param"));
        assertEquals('A', optional.orElseThrow("Error %s %s", "p1", "p2"));
        assertEquals('A', optional.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
        assertEquals('A', optional.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));

        u.OptionalChar empty = u.OptionalChar.empty();
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s", "param"));
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", "p1", "p2"));
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s %s", "p1", "p2", "p3"));
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrow("Error %s %s", new Object[] { "p1", "p2" }));
    }

    @Test
    @DisplayName("Test orIfNull()")
    public void testOrIfNull() {
        u.Nullable<String> nonNull = u.Nullable.of("first");
        u.Nullable<String> result = nonNull.orIfNull(() -> u.Nullable.of("second"));
        assertTrue(result.isPresent());
        assertEquals("first", result.get());

        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        u.Nullable<String> result2 = nullValue.orIfNull(() -> u.Nullable.of("second"));
        assertTrue(result2.isPresent());
        assertEquals("second", result2.get());

        u.Nullable<String> empty = u.Nullable.empty();
        u.Nullable<String> result3 = empty.orIfNull(() -> u.Nullable.of("second"));
        assertTrue(result3.isPresent());
        assertEquals("second", result3.get());

        assertThrows(IllegalArgumentException.class, () -> nullValue.orIfNull(null));
    }

    @Test
    @DisplayName("Test orElseNull()")
    public void testOrElseNull() {
        u.Nullable<String> present = u.Nullable.of("test");
        assertEquals("test", present.orElseNull());

        u.Nullable<String> nullPresent = u.Nullable.of((String) null);
        assertNull(nullPresent.orElseNull());

        u.Nullable<String> empty = u.Nullable.empty();
        assertNull(empty.orElseNull());
    }

    @Test
    @DisplayName("Test orElseIfNull()")
    public void testOrDefaultIfNull() {
        u.Nullable<String> nonNull = u.Nullable.of("test");
        assertEquals("test", nonNull.orElseIfNull("default"));

        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        assertEquals("default", nullValue.orElseIfNull("default"));

        u.Nullable<String> empty = u.Nullable.empty();
        assertEquals("default", empty.orElseIfNull("default"));
    }

    @Test
    @DisplayName("Test orElseGetIfNull()")
    public void testOrElseGetIfNull() {
        u.Nullable<String> nonNull = u.Nullable.of("test");
        assertEquals("test", nonNull.orElseGetIfNull(() -> "default"));

        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        assertEquals("default", nullValue.orElseGetIfNull(() -> "default"));

        u.Nullable<String> empty = u.Nullable.empty();
        assertEquals("default", empty.orElseGetIfNull(() -> "default"));

        assertThrows(IllegalArgumentException.class, () -> nullValue.orElseGetIfNull(null));
    }

    @Test
    @DisplayName("Test orElseThrow(Supplier)")
    public void testOrElseThrowSupplier() {
        u.Nullable<String> present = u.Nullable.of("test");
        assertEquals("test", present.orElseThrow(() -> new RuntimeException()));

        u.Nullable<String> empty = u.Nullable.empty();
        RuntimeException ex = assertThrows(RuntimeException.class, () -> empty.orElseThrow(() -> new RuntimeException("Custom")));
        assertEquals("Custom", ex.getMessage());

        assertThrows(IllegalArgumentException.class, () -> empty.orElseThrow((Supplier<RuntimeException>) null));
    }

    @Test
    @DisplayName("Test orElseThrowIfNull()")
    public void testOrElseThrowIfNull() {
        u.Nullable<String> nonNull = u.Nullable.of("test");
        assertEquals("test", nonNull.orElseThrowIfNull());

        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        assertThrows(NoSuchElementException.class, () -> nullValue.orElseThrowIfNull());

        u.Nullable<String> empty = u.Nullable.empty();
        assertThrows(NoSuchElementException.class, () -> empty.orElseThrowIfNull());
    }

    @Test
    @DisplayName("Test orElseThrowIfNull() with message variations")
    public void testOrElseThrowIfNullWithMessage() {
        u.Nullable<String> nullValue = u.Nullable.of((String) null);

        NoSuchElementException ex1 = assertThrows(NoSuchElementException.class, () -> nullValue.orElseThrowIfNull("Custom error"));
        assertEquals("Custom error", ex1.getMessage());

        NoSuchElementException ex2 = assertThrows(NoSuchElementException.class, () -> nullValue.orElseThrowIfNull("Error: %s", "param1"));
        assertTrue(ex2.getMessage().contains("param1"));

        NoSuchElementException ex3 = assertThrows(NoSuchElementException.class, () -> nullValue.orElseThrowIfNull("Error: %s %s", "param1", "param2"));
        assertTrue(ex3.getMessage().contains("param1"));
        assertTrue(ex3.getMessage().contains("param2"));

        NoSuchElementException ex4 = assertThrows(NoSuchElementException.class, () -> nullValue.orElseThrowIfNull("Error: %s %s %s", "p1", "p2", "p3"));
        assertTrue(ex4.getMessage().contains("p1"));
        assertTrue(ex4.getMessage().contains("p2"));
        assertTrue(ex4.getMessage().contains("p3"));

        NoSuchElementException ex5 = assertThrows(NoSuchElementException.class,
                () -> nullValue.orElseThrowIfNull("Error: %s %s %s %s", "p1", "p2", "p3", "p4"));
        assertTrue(ex5.getMessage().contains("p1"));
        assertTrue(ex5.getMessage().contains("p4"));
    }

    @Test
    @DisplayName("Test orElseThrowIfNull(Supplier)")
    public void testOrElseThrowIfNullSupplier() {
        u.Nullable<String> nonNull = u.Nullable.of("test");
        assertEquals("test", nonNull.orElseThrowIfNull(() -> new RuntimeException()));

        u.Nullable<String> nullValue = u.Nullable.of((String) null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> nullValue.orElseThrowIfNull(() -> new RuntimeException("Custom")));
        assertEquals("Custom", ex.getMessage());

        assertThrows(IllegalArgumentException.class, () -> nullValue.orElseThrowIfNull((Supplier<RuntimeException>) null));
    }

    @Test
    public void testOrElseMethods() {
        assertEquals(42.5f, OptionalFloat.of(42.5f).orElseZero());
        assertEquals(0.0f, OptionalFloat.empty().orElseZero());

        assertEquals(42.5f, OptionalFloat.of(42.5f).orElse(100.0f));
        assertEquals(100.0f, OptionalFloat.empty().orElse(100.0f));

        assertEquals(42.5f, OptionalFloat.of(42.5f).orElseGet(() -> 100.0f));
        assertEquals(100.0f, OptionalFloat.empty().orElseGet(() -> 100.0f));
    }

    @Test
    @DisplayName("orElseGet supplier MUST NOT be invoked when value is present")
    public void testOrElseGet_NotInvokedIfPresent() {
        AtomicBoolean called = new AtomicBoolean(false);
        Supplier<String> s = () -> {
            called.set(true);
            return "fallback";
        };

        Optional.of("v").orElseGet(s);
        assertFalse(called.get(), "Optional.orElseGet supplier invoked while value present");

        Nullable.of("v").orElseGet(s);
        assertFalse(called.get(), "Nullable.orElseGet supplier invoked while value present");

        AtomicBoolean intCalled = new AtomicBoolean(false);
        OptionalInt.of(42).orElseGet(() -> {
            intCalled.set(true);
            return 0;
        });
        assertFalse(intCalled.get(), "OptionalInt.orElseGet supplier invoked while value present");

        AtomicBoolean longCalled = new AtomicBoolean(false);
        OptionalLong.of(42L).orElseGet(() -> {
            longCalled.set(true);
            return 0L;
        });
        assertFalse(longCalled.get(), "OptionalLong.orElseGet supplier invoked while value present");

        AtomicBoolean dblCalled = new AtomicBoolean(false);
        OptionalDouble.of(1.0).orElseGet(() -> {
            dblCalled.set(true);
            return 0.0;
        });
        assertFalse(dblCalled.get(), "OptionalDouble.orElseGet supplier invoked while value present");
    }

    @Test
    @DisplayName("orElseThrow(supplier) MUST NOT invoke supplier when value is present")
    public void testOrElseThrowSupplier_NotInvokedIfPresent() {
        AtomicBoolean called = new AtomicBoolean(false);
        Supplier<RuntimeException> exSup = () -> {
            called.set(true);
            return new RuntimeException();
        };

        Optional.of("v").orElseThrow(exSup);
        assertFalse(called.get());
        OptionalInt.of(1).orElseThrow(exSup);
        assertFalse(called.get());
        OptionalLong.of(1L).orElseThrow(exSup);
        assertFalse(called.get());
        OptionalDouble.of(1.0).orElseThrow(exSup);
        assertFalse(called.get());
    }

    @Test
    @DisplayName("or(supplier) MUST NOT invoke supplier when value is present and returns this")
    public void testOr_NotInvokedIfPresent() {
        AtomicBoolean called = new AtomicBoolean(false);
        Optional<String> opt = Optional.of("v");
        Optional<String> result = opt.or(() -> {
            called.set(true);
            return Optional.of("other");
        });
        assertFalse(called.get());
        assertSame(opt, result);

        AtomicBoolean intCalled = new AtomicBoolean(false);
        OptionalInt iopt = OptionalInt.of(7);
        OptionalInt iresult = iopt.or(() -> {
            intCalled.set(true);
            return OptionalInt.of(99);
        });
        assertFalse(intCalled.get());
        assertSame(iopt, iresult);
    }

    @Test
    @DisplayName("or supplier returning null throws IllegalArgumentException")
    public void testOr_NullResult() {
        assertThrows(IllegalArgumentException.class, () -> Optional.empty().or(() -> null));
        assertThrows(IllegalArgumentException.class, () -> OptionalInt.empty().or(() -> null));
        assertThrows(IllegalArgumentException.class, () -> OptionalLong.empty().or(() -> null));
        assertThrows(IllegalArgumentException.class, () -> OptionalFloat.empty().or(() -> null));
        assertThrows(IllegalArgumentException.class, () -> OptionalDouble.empty().or(() -> null));
        assertThrows(IllegalArgumentException.class, () -> Nullable.empty().or(() -> null));

        // A null supplier is rejected eagerly with IllegalArgumentException.
        assertThrows(IllegalArgumentException.class, () -> Optional.empty().or(null));
        assertThrows(IllegalArgumentException.class, () -> Nullable.empty().or(null));
    }

    @Test
    @DisplayName("or rejects a null supplier eagerly")
    public void testOr_DoesNotEvaluateNullSupplierForPresentValue() {
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> OptionalBoolean.of(true).or(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> OptionalChar.of('a').or(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> OptionalByte.of((byte) 1).or(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> OptionalShort.of((short) 1).or(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> OptionalInt.of(1).or(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> OptionalLong.of(1L).or(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> OptionalFloat.of(1F).or(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> OptionalDouble.of(1D).or(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Optional.of("value").or(null));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> Nullable.of("value").or(null));
    }
}
