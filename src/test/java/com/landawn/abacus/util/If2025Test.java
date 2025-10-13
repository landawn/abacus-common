package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class If2025Test extends TestBase {

    @Test
    public void testIs() {
        assertTrue(If.is(true).b);
        assertFalse(If.is(false).b);

        assertTrue(If.is(true) == If.is(true));
        assertTrue(If.is(false) == If.is(false));
    }

    @Test
    public void testIsThen() {
        AtomicBoolean executed = new AtomicBoolean(false);

        If.is(true).then(() -> executed.set(true));
        assertTrue(executed.get());

        executed.set(false);
        If.is(false).then(() -> executed.set(true));
        assertFalse(executed.get());
    }

    @Test
    public void testNot() {
        assertFalse(If.not(true).b);
        assertTrue(If.not(false).b);

        assertTrue(If.not(true) == If.is(false));
        assertTrue(If.not(false) == If.is(true));
    }

    @Test
    public void testNotThen() {
        AtomicBoolean executed = new AtomicBoolean(false);

        If.not(false).then(() -> executed.set(true));
        assertTrue(executed.get());

        executed.set(false);
        If.not(true).then(() -> executed.set(true));
        assertFalse(executed.get());
    }

    @Test
    public void testExists() {
        assertTrue(If.exists(0).b);
        assertTrue(If.exists(1).b);
        assertTrue(If.exists(100).b);
        assertFalse(If.exists(-1).b);
        assertFalse(If.exists(-100).b);
    }

    @Test
    public void testExistsWithIndexOf() {
        List<String> list = Arrays.asList("a", "b", "c");

        AtomicBoolean executed = new AtomicBoolean(false);
        If.exists(list.indexOf("b")).then(() -> executed.set(true));
        assertTrue(executed.get());

        executed.set(false);
        If.exists(list.indexOf("d")).then(() -> executed.set(true));
        assertFalse(executed.get());
    }

    @Test
    public void testIsNull() {
        assertTrue(If.isNull(null).b);
        assertFalse(If.isNull("").b);
        assertFalse(If.isNull("abc").b);
        assertFalse(If.isNull(new Object()).b);
        assertFalse(If.isNull(0).b);
    }

    @Test
    public void testIsNullThen() {
        AtomicBoolean executed = new AtomicBoolean(false);

        If.isNull(null).then(() -> executed.set(true));
        assertTrue(executed.get());

        executed.set(false);
        If.isNull("value").then(() -> executed.set(true));
        assertFalse(executed.get());
    }

    @Test
    public void testIsEmptyCharSequence() {
        assertTrue(If.isEmpty((CharSequence) null).b);
        assertTrue(If.isEmpty("").b);
        assertFalse(If.isEmpty(" ").b);
        assertFalse(If.isEmpty("abc").b);
    }

    @Test
    public void testIsEmptyBooleanArray() {
        assertTrue(If.isEmpty((boolean[]) null).b);
        assertTrue(If.isEmpty(new boolean[0]).b);
        assertFalse(If.isEmpty(new boolean[] { true }).b);
    }

    @Test
    public void testIsEmptyCharArray() {
        assertTrue(If.isEmpty((char[]) null).b);
        assertTrue(If.isEmpty(new char[0]).b);
        assertFalse(If.isEmpty(new char[] { 'a' }).b);
    }

    @Test
    public void testIsEmptyByteArray() {
        assertTrue(If.isEmpty((byte[]) null).b);
        assertTrue(If.isEmpty(new byte[0]).b);
        assertFalse(If.isEmpty(new byte[] { 1 }).b);
    }

    @Test
    public void testIsEmptyShortArray() {
        assertTrue(If.isEmpty((short[]) null).b);
        assertTrue(If.isEmpty(new short[0]).b);
        assertFalse(If.isEmpty(new short[] { 1 }).b);
    }

    @Test
    public void testIsEmptyIntArray() {
        assertTrue(If.isEmpty((int[]) null).b);
        assertTrue(If.isEmpty(new int[0]).b);
        assertFalse(If.isEmpty(new int[] { 1 }).b);
    }

    @Test
    public void testIsEmptyLongArray() {
        assertTrue(If.isEmpty((long[]) null).b);
        assertTrue(If.isEmpty(new long[0]).b);
        assertFalse(If.isEmpty(new long[] { 1 }).b);
    }

    @Test
    public void testIsEmptyFloatArray() {
        assertTrue(If.isEmpty((float[]) null).b);
        assertTrue(If.isEmpty(new float[0]).b);
        assertFalse(If.isEmpty(new float[] { 1.0f }).b);
    }

    @Test
    public void testIsEmptyDoubleArray() {
        assertTrue(If.isEmpty((double[]) null).b);
        assertTrue(If.isEmpty(new double[0]).b);
        assertFalse(If.isEmpty(new double[] { 1.0 }).b);
    }

    @Test
    public void testIsEmptyObjectArray() {
        assertTrue(If.isEmpty((Object[]) null).b);
        assertTrue(If.isEmpty(new Object[0]).b);
        assertFalse(If.isEmpty(new Object[] { "a" }).b);
    }

    @Test
    public void testIsEmptyCollection() {
        assertTrue(If.isEmpty((Collection<?>) null).b);
        assertTrue(If.isEmpty(new ArrayList<>()).b);
        assertFalse(If.isEmpty(Arrays.asList("a")).b);
    }

    @Test
    public void testIsEmptyMap() {
        assertTrue(If.isEmpty((Map<?, ?>) null).b);
        assertTrue(If.isEmpty(new HashMap<>()).b);

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertFalse(If.isEmpty(map).b);
    }

    @Test
    public void testIsBlank() {
        assertTrue(If.isBlank(null).b);
        assertTrue(If.isBlank("").b);
        assertTrue(If.isBlank(" ").b);
        assertTrue(If.isBlank("  ").b);
        assertTrue(If.isBlank("\t").b);
        assertTrue(If.isBlank("\n").b);
        assertFalse(If.isBlank("a").b);
        assertFalse(If.isBlank(" a ").b);
    }

    @Test
    public void testNotNull() {
        assertFalse(If.notNull(null).b);
        assertTrue(If.notNull("").b);
        assertTrue(If.notNull("abc").b);
        assertTrue(If.notNull(new Object()).b);
        assertTrue(If.notNull(0).b);
    }

    @Test
    public void testNotNullThen() {
        AtomicBoolean executed = new AtomicBoolean(false);

        If.notNull("value").then(() -> executed.set(true));
        assertTrue(executed.get());

        executed.set(false);
        If.notNull(null).then(() -> executed.set(true));
        assertFalse(executed.get());
    }

    @Test
    public void testNotEmptyCharSequence() {
        assertFalse(If.notEmpty((CharSequence) null).b);
        assertFalse(If.notEmpty("").b);
        assertTrue(If.notEmpty(" ").b);
        assertTrue(If.notEmpty("abc").b);
    }

    @Test
    public void testNotEmptyBooleanArray() {
        assertFalse(If.notEmpty((boolean[]) null).b);
        assertFalse(If.notEmpty(new boolean[0]).b);
        assertTrue(If.notEmpty(new boolean[] { true }).b);
    }

    @Test
    public void testNotEmptyCharArray() {
        assertFalse(If.notEmpty((char[]) null).b);
        assertFalse(If.notEmpty(new char[0]).b);
        assertTrue(If.notEmpty(new char[] { 'a' }).b);
    }

    @Test
    public void testNotEmptyByteArray() {
        assertFalse(If.notEmpty((byte[]) null).b);
        assertFalse(If.notEmpty(new byte[0]).b);
        assertTrue(If.notEmpty(new byte[] { 1 }).b);
    }

    @Test
    public void testNotEmptyShortArray() {
        assertFalse(If.notEmpty((short[]) null).b);
        assertFalse(If.notEmpty(new short[0]).b);
        assertTrue(If.notEmpty(new short[] { 1 }).b);
    }

    @Test
    public void testNotEmptyIntArray() {
        assertFalse(If.notEmpty((int[]) null).b);
        assertFalse(If.notEmpty(new int[0]).b);
        assertTrue(If.notEmpty(new int[] { 1 }).b);
    }

    @Test
    public void testNotEmptyLongArray() {
        assertFalse(If.notEmpty((long[]) null).b);
        assertFalse(If.notEmpty(new long[0]).b);
        assertTrue(If.notEmpty(new long[] { 1 }).b);
    }

    @Test
    public void testNotEmptyFloatArray() {
        assertFalse(If.notEmpty((float[]) null).b);
        assertFalse(If.notEmpty(new float[0]).b);
        assertTrue(If.notEmpty(new float[] { 1.0f }).b);
    }

    @Test
    public void testNotEmptyDoubleArray() {
        assertFalse(If.notEmpty((double[]) null).b);
        assertFalse(If.notEmpty(new double[0]).b);
        assertTrue(If.notEmpty(new double[] { 1.0 }).b);
    }

    @Test
    public void testNotEmptyObjectArray() {
        assertFalse(If.notEmpty((Object[]) null).b);
        assertFalse(If.notEmpty(new Object[0]).b);
        assertTrue(If.notEmpty(new Object[] { "a" }).b);
    }

    @Test
    public void testNotEmptyCollection() {
        assertFalse(If.notEmpty((Collection<?>) null).b);
        assertFalse(If.notEmpty(new ArrayList<>()).b);
        assertTrue(If.notEmpty(Arrays.asList("a")).b);
    }

    @Test
    public void testNotEmptyMap() {
        assertFalse(If.notEmpty((Map<?, ?>) null).b);
        assertFalse(If.notEmpty(new HashMap<>()).b);

        Map<String, String> map = new HashMap<>();
        map.put("key", "value");
        assertTrue(If.notEmpty(map).b);
    }

    @Test
    public void testNotBlank() {
        assertFalse(If.notBlank(null).b);
        assertFalse(If.notBlank("").b);
        assertFalse(If.notBlank(" ").b);
        assertFalse(If.notBlank("  ").b);
        assertFalse(If.notBlank("\t").b);
        assertFalse(If.notBlank("\n").b);
        assertTrue(If.notBlank("a").b);
        assertTrue(If.notBlank(" a ").b);
    }

    @Test
    public void testThenDoNothing() {
        AtomicBoolean executed = new AtomicBoolean(false);

        If.is(true).thenDoNothing().orElse(() -> executed.set(true));
        assertFalse(executed.get());

        If.is(false).thenDoNothing().orElse(() -> executed.set(true));
        assertTrue(executed.get());
    }

    @Test
    public void testThenRunnable() {
        AtomicInteger counter = new AtomicInteger(0);

        If.is(true).then(() -> counter.incrementAndGet());
        assertEquals(1, counter.get());

        If.is(false).then(() -> counter.incrementAndGet());
        assertEquals(1, counter.get());
    }

    @Test
    public void testThenRunnableNull() {
        assertThrows(IllegalArgumentException.class, () -> If.is(true).then((Throwables.Runnable<Exception>) null));
    }

    @Test
    public void testThenWithInput() {
        AtomicInteger result = new AtomicInteger(0);

        If.is(true).then(5, result::set);
        assertEquals(5, result.get());

        If.is(false).then(10, result::set);
        assertEquals(5, result.get());
    }

    @Test
    public void testThenWithInputNull() {
        assertThrows(IllegalArgumentException.class, () -> If.is(true).then("value", null));
    }

    @Test
    public void testThenThrow() {
        assertThrows(RuntimeException.class, () -> If.is(true).thenThrow(() -> new RuntimeException("Error")));

        assertDoesNotThrow(() -> If.is(false).thenThrow(() -> new RuntimeException("Error")));
    }

    @Test
    public void testThenThrowNull() {
        assertThrows(IllegalArgumentException.class, () -> If.is(true).thenThrow(null));
    }

    @Test
    public void testOrElseRunnable() {
        AtomicInteger counter = new AtomicInteger(0);

        If.is(true).then(() -> counter.set(1)).orElse(() -> counter.set(2));
        assertEquals(1, counter.get());

        If.is(false).then(() -> counter.set(1)).orElse(() -> counter.set(2));
        assertEquals(2, counter.get());
    }

    @Test
    public void testOrElseRunnableNull() {
        assertThrows(IllegalArgumentException.class, () -> If.is(true).then(() -> {
        }).orElse((Throwables.Runnable<Exception>) null));
    }

    @Test
    public void testOrElseWithInput() {
        AtomicInteger result = new AtomicInteger(0);

        If.is(true).then(() -> result.set(1)).orElse(5, result::set);
        assertEquals(1, result.get());

        If.is(false).then(() -> result.set(1)).orElse(5, result::set);
        assertEquals(5, result.get());
    }

    @Test
    public void testOrElseWithInputNull() {
        assertThrows(IllegalArgumentException.class, () -> If.is(true).then(() -> {
        }).orElse("value", null));
    }

    @Test
    public void testOrElseThrow() {
        assertDoesNotThrow(() -> If.is(true).then(() -> {
        }).orElseThrow(() -> new RuntimeException("Error")));

        assertThrows(RuntimeException.class, () -> If.is(false).then(() -> {
        }).orElseThrow(() -> new RuntimeException("Error")));
    }

    @Test
    public void testOrElseThrowNull() {
        assertThrows(IllegalArgumentException.class, () -> If.is(true).then(() -> {
        }).orElseThrow(null));
    }

    @Test
    public void testCompleteIfElseChain() {
        String result = performAction(true);
        assertEquals("success", result);

        result = performAction(false);
        assertEquals("failure", result);
    }

    private String performAction(boolean condition) {
        AtomicReference<String> result = new AtomicReference<>();

        If.is(condition).then(() -> result.set("success")).orElse(() -> result.set("failure"));

        return result.get();
    }

    @Test
    public void testNestedConditions() {
        List<String> list = Arrays.asList("a", "b", "c");
        String searchItem = "b";
        AtomicBoolean found = new AtomicBoolean(false);

        If.notEmpty(list).then(() -> {
            If.exists(list.indexOf(searchItem)).then(() -> found.set(true));
        });

        assertTrue(found.get());
    }

    @Test
    public void testNullCheckWithAction() {
        String value = "test";
        AtomicReference<String> processed = new AtomicReference<>();

        If.notNull(value).then(value, v -> processed.set(v.toUpperCase())).orElse(() -> processed.set("NULL"));

        assertEquals("TEST", processed.get());

        processed.set(null);
        If.notNull((String) null).then(value, v -> processed.set(v.toUpperCase())).orElse(() -> processed.set("NULL"));

        assertEquals("NULL", processed.get());
    }

    @Test
    public void testCollectionProcessing() {
        List<Integer> numbers = Arrays.asList(1, 2, 3, 4, 5);
        AtomicInteger sum = new AtomicInteger(0);

        If.notEmpty(numbers).then(() -> numbers.forEach(sum::addAndGet)).orElseThrow(() -> new IllegalStateException("List is empty"));

        assertEquals(15, sum.get());
    }

    @Test
    public void testValidationScenario() {
        String input = "";

        assertThrows(IllegalArgumentException.class, () -> If.isBlank(input).thenThrow(() -> new IllegalArgumentException("Input cannot be blank")));

        String validInput = "valid";
        assertDoesNotThrow(() -> If.isBlank(validInput).thenThrow(() -> new IllegalArgumentException("Input cannot be blank")));
    }

    @Test
    public void testMultipleConditionTypes() {
        AtomicInteger step = new AtomicInteger(0);

        If.is(true).then(() -> step.set(1));
        assertEquals(1, step.get());

        If.not(false).then(() -> step.set(2));
        assertEquals(2, step.get());

        If.notNull("value").then(() -> step.set(3));
        assertEquals(3, step.get());

        If.notEmpty("value").then(() -> step.set(4));
        assertEquals(4, step.get());

        If.notBlank("value").then(() -> step.set(5));
        assertEquals(5, step.get());

        If.exists(0).then(() -> step.set(6));
        assertEquals(6, step.get());
    }

    @Test
    public void testIsEmptyPrimitiveList() {
        assertTrue(If.isEmpty((IntList) null).b);
        assertTrue(If.isEmpty(IntList.of()).b);
        assertFalse(If.isEmpty(IntList.of(1, 2, 3)).b);
    }

    @Test
    public void testIsEmptyMultiset() {
        assertTrue(If.isEmpty((Multiset<?>) null).b);
        assertTrue(If.isEmpty(Multiset.of()).b);
        assertFalse(If.isEmpty(Multiset.of("a", "b")).b);
    }

    @Test
    public void testIsEmptyMultimap() {
        assertTrue(If.isEmpty((ListMultimap<?, ?>) null).b);
        assertTrue(If.isEmpty(new ListMultimap<>()).b);

        ListMultimap<String, String> mm = new ListMultimap<>();
        mm.put("key", "value");
        assertFalse(If.isEmpty(mm).b);
    }

    @Test
    public void testNotEmptyPrimitiveList() {
        assertFalse(If.notEmpty((IntList) null).b);
        assertFalse(If.notEmpty(IntList.of()).b);
        assertTrue(If.notEmpty(IntList.of(1, 2, 3)).b);
    }

    @Test
    public void testNotEmptyMultiset() {
        assertFalse(If.notEmpty((Multiset<?>) null).b);
        assertFalse(If.notEmpty(Multiset.of()).b);
        assertTrue(If.notEmpty(Multiset.of("a", "b")).b);
    }

    @Test
    public void testNotEmptyMultimap() {
        assertFalse(If.notEmpty((ListMultimap<?, ?>) null).b);
        assertFalse(If.notEmpty(new ListMultimap<>()).b);

        ListMultimap<String, String> mm = new ListMultimap<>();
        mm.put("key", "value");
        assertTrue(If.notEmpty(mm).b);
    }
}
