package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

public class CommonUtilDefaultTest extends CommonUtilTestSupport {

    @Test
    public void testDefaultIfNull_primitives() {
        assertFalse(CommonUtil.defaultIfNull((Boolean) null));
        assertTrue(CommonUtil.defaultIfNull(true));
        assertFalse(CommonUtil.defaultIfNull(false));
        assertTrue(CommonUtil.defaultIfNull((Boolean) null, true));
        assertFalse(CommonUtil.defaultIfNull((Boolean) null, false));
        assertTrue(CommonUtil.defaultIfNull(Boolean.TRUE, false));
        assertFalse(CommonUtil.defaultIfNull(Boolean.FALSE, true));

        assertEquals('\u0000', CommonUtil.defaultIfNull((Character) null));
        assertEquals('a', CommonUtil.defaultIfNull('a'));
        assertEquals('x', CommonUtil.defaultIfNull((Character) null, 'x'));
        assertEquals('a', CommonUtil.defaultIfNull(Character.valueOf('a'), 'b'));

        assertEquals((byte) 0, CommonUtil.defaultIfNull((Byte) null));
        assertEquals((byte) 5, CommonUtil.defaultIfNull((byte) 5));
        assertEquals((byte) 10, CommonUtil.defaultIfNull((Byte) null, (byte) 10));
        assertEquals((byte) 5, CommonUtil.defaultIfNull(Byte.valueOf((byte) 5), (byte) 10));

        assertEquals((short) 0, CommonUtil.defaultIfNull((Short) null));
        assertEquals((short) 5, CommonUtil.defaultIfNull((short) 5));
        assertEquals((short) 10, CommonUtil.defaultIfNull((Short) null, (short) 10));
        assertEquals((short) 5, CommonUtil.defaultIfNull(Short.valueOf((short) 5), (short) 10));

        assertEquals(0, CommonUtil.defaultIfNull((Integer) null));
        assertEquals(5, CommonUtil.defaultIfNull(5));
        assertEquals(10, CommonUtil.defaultIfNull((Integer) null, 10));
        assertEquals(5, CommonUtil.defaultIfNull(Integer.valueOf(5), 10));

        assertEquals(0L, CommonUtil.defaultIfNull((Long) null));
        assertEquals(5L, CommonUtil.defaultIfNull(5L));
        assertEquals(10L, CommonUtil.defaultIfNull((Long) null, 10L));
        assertEquals(5L, CommonUtil.defaultIfNull(Long.valueOf(5L), 10L));

        assertEquals(0.0f, CommonUtil.defaultIfNull((Float) null));
        assertEquals(5.0f, CommonUtil.defaultIfNull(5.0f));
        assertEquals(10.0f, CommonUtil.defaultIfNull((Float) null, 10.0f));
        assertEquals(5f, CommonUtil.defaultIfNull(Float.valueOf(5f), 10f), 0.0f);

        assertEquals(0.0, CommonUtil.defaultIfNull((Double) null));
        assertEquals(5.0, CommonUtil.defaultIfNull(5.0));
        assertEquals(10.0, CommonUtil.defaultIfNull((Double) null, 10.0));
        assertEquals(5d, CommonUtil.defaultIfNull(Double.valueOf(5d), 10d), 0.0);
    }

    @Test
    public void testDefaultIfNull_objectAndSupplier() {
        assertEquals("default", CommonUtil.defaultIfNull((String) null, "default"));
        assertEquals("test", CommonUtil.defaultIfNull("test", "default"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfNull(null, (String) null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfNull("any", (String) null));

        Supplier<String> supplier = () -> "default";
        assertEquals("default", CommonUtil.defaultIfNull((String) null, supplier));
        assertEquals("test", CommonUtil.defaultIfNull("test", supplier));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfNull(null, (Supplier<String>) () -> null));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfNull("test", (Supplier<String>) null));
        assertEquals("default", CommonUtil.defaultIfNull((String) null, Fn.s(() -> "default")));
    }

    @Test
    public void testDefaultIfEmpty() {
        assertEquals("default", CommonUtil.defaultIfEmpty((String) null, "default"));
        assertEquals("default", CommonUtil.defaultIfEmpty("", "default"));
        assertEquals("test", CommonUtil.defaultIfEmpty("test", "default"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty("any", ""));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty("any", (String) null));

        StringBuilder empty = new StringBuilder();
        StringBuilder val = new StringBuilder("val");
        StringBuilder def = new StringBuilder("defaultSb");
        assertSame(def, CommonUtil.defaultIfEmpty((StringBuilder) null, def));
        assertSame(def, CommonUtil.defaultIfEmpty(empty, def));
        assertSame(val, CommonUtil.defaultIfEmpty(val, def));

        Supplier<String> emptyDefault = () -> "default";
        assertEquals("default", CommonUtil.defaultIfEmpty((String) null, emptyDefault));
        assertEquals("default", CommonUtil.defaultIfEmpty("", emptyDefault));
        assertEquals("actual", CommonUtil.defaultIfEmpty("actual", emptyDefault));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty("", (Supplier<String>) () -> ""));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty("", (Supplier<String>) () -> null));
        assertEquals("default", CommonUtil.defaultIfEmpty("", Fn.s(() -> "default")));

        List<String> list = Arrays.asList("a", "b");
        List<String> defaultList = Arrays.asList("x", "y");
        assertEquals(defaultList, CommonUtil.defaultIfEmpty((List<String>) null, defaultList));
        assertEquals(defaultList, CommonUtil.defaultIfEmpty(new ArrayList<>(), defaultList));
        assertEquals(list, CommonUtil.defaultIfEmpty(list, defaultList));

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        Map<String, String> defaultMap = new HashMap<>();
        defaultMap.put("default", "val");
        assertEquals(defaultMap, CommonUtil.defaultIfEmpty((Map<String, String>) null, defaultMap));
        assertEquals(defaultMap, CommonUtil.defaultIfEmpty(new HashMap<>(), defaultMap));
        assertEquals(map, CommonUtil.defaultIfEmpty(map, defaultMap));
    }

    @Test
    public void testDefaultIfBlank() {
        assertEquals("default", CommonUtil.defaultIfBlank((String) null, "default"));
        assertEquals("default", CommonUtil.defaultIfBlank("", "default"));
        assertEquals("default", CommonUtil.defaultIfBlank("  ", "default"));
        assertEquals("test", CommonUtil.defaultIfBlank("test", "default"));
        assertEquals(" test ", CommonUtil.defaultIfBlank(" test ", "default"));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank("any", ""));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank("any", "   "));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank("any", (String) null));

        Supplier<String> blankDefault = () -> "default";
        assertEquals("default", CommonUtil.defaultIfBlank((String) null, blankDefault));
        assertEquals("default", CommonUtil.defaultIfBlank("   ", blankDefault));
        assertEquals("actual", CommonUtil.defaultIfBlank("actual", blankDefault));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank("   ", (Supplier<String>) () -> "   "));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank("   ", (Supplier<String>) () -> null));
        assertEquals("default", CommonUtil.defaultIfBlank(" ", Fn.s(() -> "default")));
    }

    @Test
    public void testDefaultValueOf() {
        assertEquals(0, CommonUtil.defaultValueOf(int.class));
        assertEquals(0, (int) CommonUtil.defaultValueOf(Integer.class, true));
        assertNotNull(CommonUtil.defaultValueOf(Boolean.class, true));
        assertFalse(CommonUtil.defaultValueOf(Boolean.class, true));
        assertNull(CommonUtil.defaultValueOf(Integer.class, false));
        assertEquals('\0', CommonUtil.defaultValueOf(Character.class, true));
        assertNull(CommonUtil.defaultValueOf(Character.class, false));
        assertEquals((byte) 0, CommonUtil.defaultValueOf(Byte.class, true));
        assertNull(CommonUtil.defaultValueOf(Byte.class, false));
        assertEquals((short) 0, CommonUtil.defaultValueOf(Short.class, true));
        assertNull(CommonUtil.defaultValueOf(Short.class, false));
        assertEquals(0L, CommonUtil.defaultValueOf(Long.class, true));
        assertNull(CommonUtil.defaultValueOf(Long.class, false));
        assertEquals(0f, CommonUtil.defaultValueOf(Float.class, true), 0.0f);
        assertNull(CommonUtil.defaultValueOf(Float.class, false));
        assertEquals(0d, CommonUtil.defaultValueOf(Double.class, true), 0.0);
        assertNull(CommonUtil.defaultValueOf(Double.class, false));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultValueOf(null, true));
    }
    @Test
    public void testNotNullOrDefault_boxedPrimitiveDefaultIsNotTreatedAsDefault() {
        assertFalse(CommonUtil.notNullOrDefault(null));
        assertTrue(CommonUtil.notNullOrDefault(Integer.valueOf(0)));
        assertTrue(CommonUtil.notNullOrDefault(Boolean.FALSE));
        assertTrue(CommonUtil.notNullOrDefault(Double.valueOf(0d)));
        assertTrue(CommonUtil.notNullOrDefault(Character.valueOf((char) 0)));

        assertNull(CommonUtil.defaultValueOf(Integer.class));
        assertNull(CommonUtil.defaultValueOf(Boolean.class));
        assertEquals(Integer.valueOf(0), CommonUtil.defaultValueOf(int.class));

        assertFalse(CommonUtil.notNullOrDefault(u.Optional.empty()));
        assertFalse(CommonUtil.notNullOrDefault(u.Nullable.empty()));
        assertTrue(CommonUtil.notNullOrDefault(u.Optional.of("x")));
    }

    @Test
    public void testDefaultIf_rejectionMessageNamesTheRealParameter() {
        final IllegalArgumentException e1 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfNull("value", (String) null));
        assertTrue(e1.getMessage().contains("defaultForNull"), e1.getMessage());

        final IllegalArgumentException e2 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfEmpty("v", ""));
        assertTrue(e2.getMessage().contains("defaultForEmpty"), e2.getMessage());

        final IllegalArgumentException e3 = assertThrows(IllegalArgumentException.class, () -> CommonUtil.defaultIfBlank("v", "  "));
        assertTrue(e3.getMessage().contains("defaultForBlank"), e3.getMessage());

        final ArrayList<String> nonEmptyList = new ArrayList<>();
        nonEmptyList.add("a");
        final IllegalArgumentException e4 = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.defaultIfEmpty(nonEmptyList, new ArrayList<String>()));
        assertTrue(e4.getMessage().contains("defaultForEmpty"), e4.getMessage());

        final HashMap<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        final IllegalArgumentException e5 = assertThrows(IllegalArgumentException.class,
                () -> CommonUtil.defaultIfEmpty(nonEmptyMap, new HashMap<String, String>()));
        assertTrue(e5.getMessage().contains("defaultForEmpty"), e5.getMessage());
    }

}
