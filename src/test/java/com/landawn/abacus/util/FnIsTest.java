package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.function.Predicate;

public class FnIsTest extends FnTestSupport {

    @TempDir
    public File tempDir;

    @Test
    public void testIsNull() {
        assertTrue(Fn.isNull().test(null));
        assertFalse(Fn.isNull().test("x"));
        final Predicate<Map.Entry<String, String>> byValue = Fn.isNull(Map.Entry::getValue);
        assertTrue(byValue.test(new AbstractMap.SimpleEntry<>("k", null)));
        assertFalse(byValue.test(new AbstractMap.SimpleEntry<>("k", "v")));
        assertThrows(IllegalArgumentException.class, () -> Fn.isNull(null));
    }

    @Test
    public void testIsEmpty() {
        assertTrue(Fn.isEmpty().test(""));
        assertTrue(Fn.isEmpty().test(null));
        assertFalse(Fn.isEmpty().test(" "));
        assertFalse(Fn.isEmpty().test("x"));
        final Predicate<Map.Entry<String, String>> byValue = Fn.isEmpty(Map.Entry::getValue);
        assertTrue(byValue.test(new AbstractMap.SimpleEntry<>("k", "")));
        assertTrue(byValue.test(new AbstractMap.SimpleEntry<>("k", null)));
        assertFalse(byValue.test(new AbstractMap.SimpleEntry<>("k", "x")));
        assertThrows(IllegalArgumentException.class, () -> Fn.isEmpty(null));
    }

    @Test
    public void testIsBlank() {
        assertTrue(Fn.isBlank().test(""));
        assertTrue(Fn.isBlank().test("   "));
        assertTrue(Fn.isBlank().test(null));
        assertFalse(Fn.isBlank().test("x"));
        final Predicate<Map.Entry<String, String>> byValue = Fn.isBlank(Map.Entry::getValue);
        assertTrue(byValue.test(new AbstractMap.SimpleEntry<>("k", "  ")));
        assertFalse(byValue.test(new AbstractMap.SimpleEntry<>("k", "hello")));
        assertFalse(byValue.test(Map.entry("k", "\u00A0")));
        assertTrue(byValue.test(Map.entry("k", "\u3000")));
        assertThrows(IllegalArgumentException.class, () -> Fn.isBlank(null));
    }

    @Test
    public void testIsEmptyArray() {
        assertTrue(Fn.isEmptyArray().test(null));
        assertTrue(Fn.isEmptyArray().test(new String[0]));
        assertFalse(Fn.isEmptyArray().test(new String[] { "a" }));
    }

    @Test
    public void testIsEmptyCollection() {
        assertTrue(Fn.<List<String>> isEmptyCollection().test(null));
        assertTrue(Fn.isEmptyCollection().test(Collections.emptyList()));
        assertFalse(Fn.isEmptyCollection().test(List.of("a")));
    }

    @Test
    public void testIsEmptyMap() {
        assertTrue(Fn.<Map<String, String>> isEmptyMap().test(null));
        assertTrue(Fn.isEmptyMap().test(Collections.emptyMap()));
        assertFalse(Fn.isEmptyMap().test(Map.of("a", "b")));
    }

    @Test
    public void testIsFile() throws Exception {
        assertFalse(Fn.isFile().test(null));
        assertFalse(Fn.isFile().test(tempDir));
        final File file = new File(tempDir, "f.txt");
        assertTrue(file.createNewFile());
        assertTrue(Fn.isFile().test(file));
        assertFalse(Fn.isFile().test(new File(tempDir, "missing")));
    }

    @Test
    public void testIsDirectory() {
        assertFalse(Fn.isDirectory().test(null));
        assertTrue(Fn.isDirectory().test(tempDir));
        assertFalse(Fn.isDirectory().test(new File(tempDir, "missing")));
    }

    @Test
    public void testIsPresent() {
        assertTrue(Fn.IS_PRESENT_BOOLEAN.test(OptionalBoolean.of(true)));
        assertFalse(Fn.IS_PRESENT_BOOLEAN.test(OptionalBoolean.empty()));
        assertTrue(Fn.IS_PRESENT_INT.test(OptionalInt.of(1)));
        assertFalse(Fn.IS_PRESENT_INT.test(OptionalInt.empty()));
        assertTrue(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.of(1)));
        assertFalse(Fn.IS_PRESENT_INT_JDK.test(java.util.OptionalInt.empty()));
        assertTrue(Fn.<String> isPresent().test(com.landawn.abacus.util.u.Optional.of("x")));
        assertFalse(Fn.<String> isPresent().test(com.landawn.abacus.util.u.Optional.empty()));
        assertTrue(Fn.<String> isPresentJdk().test(java.util.Optional.of("x")));
        assertFalse(Fn.<String> isPresentJdk().test(java.util.Optional.empty()));
    }
}
