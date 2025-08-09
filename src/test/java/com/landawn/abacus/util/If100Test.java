package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class If100Test extends TestBase {

    @Test
    public void testIs() {
        AtomicBoolean executed = new AtomicBoolean(false);
        
        If.is(true).then(() -> executed.set(true));
        Assertions.assertTrue(executed.get());
        
        executed.set(false);
        If.is(false).then(() -> executed.set(true));
        Assertions.assertFalse(executed.get());
    }

    @Test
    public void testNot() {
        AtomicBoolean executed = new AtomicBoolean(false);
        
        If.not(false).then(() -> executed.set(true));
        Assertions.assertTrue(executed.get());
        
        executed.set(false);
        If.not(true).then(() -> executed.set(true));
        Assertions.assertFalse(executed.get());
    }

    @Test
    public void testExists() {
        AtomicBoolean executed = new AtomicBoolean(false);
        
        If.exists(5).then(() -> executed.set(true));
        Assertions.assertTrue(executed.get());
        
        executed.set(false);
        If.exists(-1).then(() -> executed.set(true));
        Assertions.assertFalse(executed.get());
        
        executed.set(false);
        If.exists(0).then(() -> executed.set(true));
        Assertions.assertTrue(executed.get());
    }

    @Test
    public void testIsNull() {
        AtomicBoolean executed = new AtomicBoolean(false);
        
        If.isNull(null).then(() -> executed.set(true));
        Assertions.assertTrue(executed.get());
        
        executed.set(false);
        If.isNull("not null").then(() -> executed.set(true));
        Assertions.assertFalse(executed.get());
    }

    @Test
    public void testIsEmptyCharSequence() {
        AtomicBoolean executed = new AtomicBoolean(false);
        
        If.isEmpty((CharSequence) null).then(() -> executed.set(true));
        Assertions.assertTrue(executed.get());
        
        executed.set(false);
        If.isEmpty("").then(() -> executed.set(true));
        Assertions.assertTrue(executed.get());
        
        executed.set(false);
        If.isEmpty("not empty").then(() -> executed.set(true));
        Assertions.assertFalse(executed.get());
    }

    @Test
    public void testIsEmptyPrimitiveArrays() {
        // boolean array
        Assertions.assertTrue(If.isEmpty((boolean[]) null).b);
        Assertions.assertTrue(If.isEmpty(new boolean[0]).b);
        Assertions.assertFalse(If.isEmpty(new boolean[]{true}).b);
        
        // char array
        Assertions.assertTrue(If.isEmpty((char[]) null).b);
        Assertions.assertTrue(If.isEmpty(new char[0]).b);
        Assertions.assertFalse(If.isEmpty(new char[]{'a'}).b);
        
        // byte array
        Assertions.assertTrue(If.isEmpty((byte[]) null).b);
        Assertions.assertTrue(If.isEmpty(new byte[0]).b);
        Assertions.assertFalse(If.isEmpty(new byte[]{1}).b);
        
        // short array
        Assertions.assertTrue(If.isEmpty((short[]) null).b);
        Assertions.assertTrue(If.isEmpty(new short[0]).b);
        Assertions.assertFalse(If.isEmpty(new short[]{1}).b);
        
        // int array
        Assertions.assertTrue(If.isEmpty((int[]) null).b);
        Assertions.assertTrue(If.isEmpty(new int[0]).b);
        Assertions.assertFalse(If.isEmpty(new int[]{1}).b);
        
        // long array
        Assertions.assertTrue(If.isEmpty((long[]) null).b);
        Assertions.assertTrue(If.isEmpty(new long[0]).b);
        Assertions.assertFalse(If.isEmpty(new long[]{1L}).b);
        
        // float array
        Assertions.assertTrue(If.isEmpty((float[]) null).b);
        Assertions.assertTrue(If.isEmpty(new float[0]).b);
        Assertions.assertFalse(If.isEmpty(new float[]{1.0f}).b);
        
        // double array
        Assertions.assertTrue(If.isEmpty((double[]) null).b);
        Assertions.assertTrue(If.isEmpty(new double[0]).b);
        Assertions.assertFalse(If.isEmpty(new double[]{1.0}).b);
    }

    @Test
    public void testIsEmptyObjectArray() {
        Assertions.assertTrue(If.isEmpty((Object[]) null).b);
        Assertions.assertTrue(If.isEmpty(new Object[0]).b);
        Assertions.assertFalse(If.isEmpty(new Object[]{"a"}).b);
    }

    @Test
    public void testIsEmptyCollection() {
        Assertions.assertTrue(If.isEmpty((Collection<?>) null).b);
        Assertions.assertTrue(If.isEmpty(new ArrayList<>()).b);
        Assertions.assertFalse(If.isEmpty(Arrays.asList("a", "b")).b);
    }

    @Test
    public void testIsEmptyMap() {
        Assertions.assertTrue(If.isEmpty((Map<?, ?>) null).b);
        Assertions.assertTrue(If.isEmpty(new HashMap<>()).b);
        
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Assertions.assertFalse(If.isEmpty(map).b);
    }

    @Test
    public void testIsEmptyPrimitiveList() {
        IntList intList = new IntList();
        Assertions.assertTrue(If.isEmpty(intList).b);
        
        intList.add(1);
        Assertions.assertFalse(If.isEmpty(intList).b);
        
        Assertions.assertTrue(If.isEmpty((IntList) null).b);
    }

    @Test
    public void testIsEmptyMultiset() {
        Multiset<String> multiset = new Multiset<>();
        Assertions.assertTrue(If.isEmpty(multiset).b);
        
        multiset.add("a");
        Assertions.assertFalse(If.isEmpty(multiset).b);
        
        Assertions.assertTrue(If.isEmpty((Multiset<?>) null).b);
    }

    @Test
    public void testIsEmptyMultimap() {
        ListMultimap<String, Integer> multimap = N.newListMultimap();
        Assertions.assertTrue(If.isEmpty(multimap).b);
        
        multimap.put("a", 1);
        Assertions.assertFalse(If.isEmpty(multimap).b);
        
        Assertions.assertTrue(If.isEmpty((Multimap<?, ?, ?>) null).b);
    }

    @Test
    public void testIsBlank() {
        Assertions.assertTrue(If.isBlank(null).b);
        Assertions.assertTrue(If.isBlank("").b);
        Assertions.assertTrue(If.isBlank("  ").b);
        Assertions.assertTrue(If.isBlank("\t\n").b);
        Assertions.assertFalse(If.isBlank("a").b);
        Assertions.assertFalse(If.isBlank(" a ").b);
    }

    @Test
    public void testNotNull() {
        AtomicBoolean executed = new AtomicBoolean(false);
        
        If.notNull("not null").then(() -> executed.set(true));
        Assertions.assertTrue(executed.get());
        
        executed.set(false);
        If.notNull(null).then(() -> executed.set(true));
        Assertions.assertFalse(executed.get());
    }

    @Test
    public void testNotEmptyCharSequence() {
        Assertions.assertTrue(If.notEmpty("not empty").b);
        Assertions.assertFalse(If.notEmpty("").b);
        Assertions.assertFalse(If.notEmpty((CharSequence) null).b);
    }

    @Test
    public void testNotEmptyPrimitiveArrays() {
        // Test all primitive array types
        Assertions.assertTrue(If.notEmpty(new boolean[]{true}).b);
        Assertions.assertFalse(If.notEmpty(new boolean[0]).b);
        
        Assertions.assertTrue(If.notEmpty(new char[]{'a'}).b);
        Assertions.assertFalse(If.notEmpty(new char[0]).b);
        
        Assertions.assertTrue(If.notEmpty(new byte[]{1}).b);
        Assertions.assertFalse(If.notEmpty(new byte[0]).b);
        
        Assertions.assertTrue(If.notEmpty(new short[]{1}).b);
        Assertions.assertFalse(If.notEmpty(new short[0]).b);
        
        Assertions.assertTrue(If.notEmpty(new int[]{1}).b);
        Assertions.assertFalse(If.notEmpty(new int[0]).b);
        
        Assertions.assertTrue(If.notEmpty(new long[]{1L}).b);
        Assertions.assertFalse(If.notEmpty(new long[0]).b);
        
        Assertions.assertTrue(If.notEmpty(new float[]{1.0f}).b);
        Assertions.assertFalse(If.notEmpty(new float[0]).b);
        
        Assertions.assertTrue(If.notEmpty(new double[]{1.0}).b);
        Assertions.assertFalse(If.notEmpty(new double[0]).b);
    }

    @Test
    public void testNotEmptyObjectArray() {
        Assertions.assertTrue(If.notEmpty(new Object[]{"a"}).b);
        Assertions.assertFalse(If.notEmpty(new Object[0]).b);
        Assertions.assertFalse(If.notEmpty((Object[]) null).b);
    }

    @Test
    public void testNotEmptyCollection() {
        Assertions.assertTrue(If.notEmpty(Arrays.asList("a")).b);
        Assertions.assertFalse(If.notEmpty(new ArrayList<>()).b);
        Assertions.assertFalse(If.notEmpty((Collection<?>) null).b);
    }

    @Test
    public void testNotEmptyMap() {
        Map<String, Integer> map = new HashMap<>();
        map.put("a", 1);
        Assertions.assertTrue(If.notEmpty(map).b);
        
        Assertions.assertFalse(If.notEmpty(new HashMap<>()).b);
        Assertions.assertFalse(If.notEmpty((Map<?, ?>) null).b);
    }

    @Test
    public void testNotBlank() {
        Assertions.assertTrue(If.notBlank("a").b);
        Assertions.assertTrue(If.notBlank(" a ").b);
        Assertions.assertFalse(If.notBlank("").b);
        Assertions.assertFalse(If.notBlank("  ").b);
        Assertions.assertFalse(If.notBlank(null).b);
    }

    @Test
    public void testThenDoNothing() {
        If.is(true).thenDoNothing().orElse(() -> Assertions.fail("Should not execute"));
        
        AtomicBoolean executed = new AtomicBoolean(false);
        If.is(false).thenDoNothing().orElse(() -> executed.set(true));
        Assertions.assertTrue(executed.get());
    }

    @Test
    public void testThenWithRunnable() {
        AtomicInteger counter = new AtomicInteger(0);
        
        If.is(true).then(() -> counter.incrementAndGet());
        Assertions.assertEquals(1, counter.get());
        
        If.is(false).then(() -> counter.incrementAndGet());
        Assertions.assertEquals(1, counter.get()); // Should not increment
    }

    @Test
    public void testThenWithConsumer() {
        AtomicInteger result = new AtomicInteger(0);
        
        If.is(true).then(10, result::set);
        Assertions.assertEquals(10, result.get());
        
        If.is(false).then(20, result::set);
        Assertions.assertEquals(10, result.get()); // Should not change
    }

    @Test
    public void testThenThrow() {
        Assertions.assertThrows(IllegalStateException.class, 
            () -> If.is(true).thenThrow(() -> new IllegalStateException("Expected")));
        
        // Should not throw when condition is false
        If.is(false).thenThrow(() -> new IllegalStateException("Should not throw"));
    }

    @Test
    public void testOrElse() {
        AtomicInteger counter = new AtomicInteger(0);
        
        If.is(true)
            .then(() -> counter.set(1))
            .orElse(() -> counter.set(2));
        Assertions.assertEquals(1, counter.get());
        
        If.is(false)
            .then(() -> counter.set(3))
            .orElse(() -> counter.set(4));
        Assertions.assertEquals(4, counter.get());
    }

    @Test
    public void testOrElseWithConsumer() {
        AtomicInteger result = new AtomicInteger(0);
        
        If.is(false)
            .then(() -> result.set(10))
            .orElse(20, result::set);
        Assertions.assertEquals(20, result.get());
        
        If.is(true)
            .then(() -> result.set(30))
            .orElse(40, result::set);
        Assertions.assertEquals(30, result.get());
    }

    @Test
    public void testOrElseThrow() {
        // Should not throw when condition is true
        If.is(true)
            .then(() -> {})
            .orElseThrow(() -> new IllegalStateException("Should not throw"));
        
        // Should throw when condition is false
        Assertions.assertThrows(IllegalStateException.class,
            () -> If.is(false)
                    .then(() -> {})
                    .orElseThrow(() -> new IllegalStateException("Expected")));
    }

    @Test
    public void testComplexChaining() {
        List<String> results = new ArrayList<>();
        
        If.notEmpty(Arrays.asList("a", "b", "c"))
            .then(() -> results.add("list not empty"))
            .orElse(() -> results.add("list empty"));
        
        If.isBlank("  ")
            .then(() -> results.add("string is blank"))
            .orElse(() -> results.add("string not blank"));
        
        If.exists(Arrays.asList("x", "y", "z").indexOf("y"))
            .then(() -> results.add("element found"))
            .orElse(() -> results.add("element not found"));
        
        Assertions.assertEquals(Arrays.asList("list not empty", "string is blank", "element found"), results);
    }

    @Test
    public void testNullArguments() {
        Assertions.assertThrows(IllegalArgumentException.class, 
            () -> If.is(true).then((Throwables.Runnable<?>) null));
        
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> If.is(true).then("value", null));
        
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> If.is(true).thenThrow(null));
        
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> If.is(true).then(() -> {}).orElse((Throwables.Runnable<?>) null));
        
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> If.is(true).then(() -> {}).orElse("value", null));
        
        Assertions.assertThrows(IllegalArgumentException.class,
            () -> If.is(true).then(() -> {}).orElseThrow(null));
    }

    @Test
    public void testOrElseDoNothing() {
        // This method is implicitly called when no orElse is chained
        If.is(false).then(() -> Assertions.fail("Should not execute"));
        // orElseDoNothing() is called implicitly
    }
}