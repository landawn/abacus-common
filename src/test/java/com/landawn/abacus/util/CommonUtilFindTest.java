package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.OptionalInt;

public class CommonUtilFindTest extends CommonUtilTestSupport {

    @Test
    public void testFindFirst() {
        List<Integer> nums = Arrays.asList(1, 2, 3, 4, 5);
        assertEquals(Integer.valueOf(4), CommonUtil.findFirst(nums, x -> x > 3).get());

        String[] arr = { "apple", "banana", "cherry", "date" };
        Predicate<String> startsWithC = s -> s.startsWith("c");
        assertEquals("cherry", CommonUtil.findFirst(arr, startsWithC).get());
        assertFalse(CommonUtil.findFirst(arr, s -> s.startsWith("x")).isPresent());
        assertFalse(CommonUtil.findFirst(new String[] {}, startsWithC).isPresent());
        assertFalse(CommonUtil.findFirst((String[]) null, startsWithC).isPresent());

        List<String> list = Arrays.asList("apple", "banana", "cherry", "date");
        assertEquals("cherry", CommonUtil.findFirst(list, startsWithC).get());
        assertFalse(CommonUtil.findFirst(list, s -> s.startsWith("x")).isPresent());
        assertFalse(CommonUtil.findFirst(Collections.emptyList(), startsWithC).isPresent());
        assertFalse(CommonUtil.findFirst((Iterable<String>) null, startsWithC).isPresent());

        assertEquals("cherry", CommonUtil.findFirst(list.iterator(), startsWithC).get());
        assertFalse(CommonUtil.findFirst(list.iterator(), s -> s.startsWith("x")).isPresent());
        assertFalse(CommonUtil.findFirst(Collections.emptyIterator(), startsWithC).isPresent());
        assertFalse(CommonUtil.findFirst((Iterator<String>) null, startsWithC).isPresent());
    }

    @Test
    public void testFindLast() {
        assertEquals(Integer.valueOf(3), CommonUtil.findLast(Arrays.asList(1, 2, 3, 4, 5), x -> x < 4).get());

        String[] arr = { "apple", "banana", "cherry", "date", "cucumber" };
        Predicate<String> startsWithC = s -> s.startsWith("c");
        assertEquals("cucumber", CommonUtil.findLast(arr, startsWithC).get());
        assertFalse(CommonUtil.findLast(arr, s -> s.startsWith("x")).isPresent());
        assertFalse(CommonUtil.findLast(new String[] {}, startsWithC).isPresent());
        assertFalse(CommonUtil.findLast((String[]) null, startsWithC).isPresent());

        List<String> list = Arrays.asList("apple", "banana", "cherry", "date", "cucumber");
        assertEquals("cucumber", CommonUtil.findLast(list, startsWithC).get());
        assertFalse(CommonUtil.findLast(Collections.emptyList(), startsWithC).isPresent());
        assertFalse(CommonUtil.findLast((Iterable<String>) null, startsWithC).isPresent());
        assertEquals("cucumber", CommonUtil.findLast(new ArrayList<>(list), startsWithC).get());
        assertEquals("cucumber", CommonUtil.findLast(new LinkedHashSet<>(list), startsWithC).get());
        assertEquals("cucumber", CommonUtil.findLast(new ArrayDeque<>(list), startsWithC).get());

        ArrayDeque<Integer> deque = new ArrayDeque<>(Arrays.asList(1, 2, 3, 2, 1));
        Nullable<Integer> lastTwo = CommonUtil.findLast(deque, n -> n == 2);
        assertTrue(lastTwo.isPresent());
        assertEquals(2, lastTwo.get());
        assertFalse(CommonUtil.findLast(new ArrayList<String>(), s -> true).isPresent());
    }

    @Test
    public void testFindFirstLastNonNull() {
        String[] values = { null, "a", "b" };
        assertEquals("a", CommonUtil.findFirstNonNull(values, s -> true).get());
        assertFalse(CommonUtil.findFirstNonNull(values, s -> s.equals("z")).isPresent());
        String[] arr = { null, "a", null, "b", "c" };
        assertEquals("b", CommonUtil.findFirstNonNull(arr, s -> s.equals("b")).get());
        assertFalse(CommonUtil.findFirstNonNull(new String[] { null, null }, s -> true).isPresent());
        assertFalse(CommonUtil.findFirstNonNull((String[]) null, s -> true).isPresent());

        List<String> list = Arrays.asList(null, "a", null, "b", "c");
        assertEquals("a", CommonUtil.findFirstNonNull(list, s -> true).get());
        assertEquals("b", CommonUtil.findFirstNonNull(list, s -> s.equals("b")).get());
        assertFalse(CommonUtil.findFirstNonNull((Iterable<String>) null, s -> true).isPresent());
        assertEquals("a", CommonUtil.findFirstNonNull(list.iterator(), s -> true).get());
        assertFalse(CommonUtil.findFirstNonNull((Iterator<String>) null, s -> true).isPresent());

        List<String> lastValues = Arrays.asList(null, "a", "b", null);
        assertEquals("b", CommonUtil.findLastNonNull(lastValues, s -> true).get());
        assertFalse(CommonUtil.findLastNonNull(lastValues, s -> s.equals("z")).isPresent());
        String[] lastArr = { null, "a", null, "b", "c", null };
        assertEquals("b", CommonUtil.findLastNonNull(lastArr, s -> s.startsWith("b")).orElse(null));
        assertEquals("c", CommonUtil.findLastNonNull(lastArr, s -> s.startsWith("c")).orElse(null));
        assertFalse(CommonUtil.findLastNonNull((String[]) null, s -> true).isPresent());
        assertFalse(CommonUtil.findLastNonNull(new String[] { null, null, null }, s -> true).isPresent());
        assertEquals("c", CommonUtil.findLastNonNull(Arrays.asList((String) null, "a", null, "b", "c", null), s -> s.startsWith("c")).orElse(null));
        assertFalse(CommonUtil.findLastNonNull((Iterable<String>) null, s -> true).isPresent());
    }

    @Test
    public void testFindFirstLastIndex() {
        assertEquals(3, CommonUtil.findFirstIndex(Arrays.asList(1, 2, 3, 4, 5), x -> x > 3).getAsInt());
        String[] arr = { "a", "b", "c", "d", "e" };
        assertEquals(2, CommonUtil.findFirstIndex(arr, s -> s.equals("c")).getAsInt());
        assertFalse(CommonUtil.findFirstIndex(arr, s -> s.equals("z")).isPresent());
        assertFalse(CommonUtil.findFirstIndex(new String[0], s -> true).isPresent());
        assertEquals(2, CommonUtil.findFirstIndex(arr, "c", (s, p) -> s.startsWith(p)).getAsInt());
        assertFalse(CommonUtil.findFirstIndex(new String[0], "c", (s, p) -> true).isPresent());

        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        assertEquals(2, CommonUtil.findFirstIndex(list, s -> s.equals("c")).getAsInt());
        assertFalse(CommonUtil.findFirstIndex(list, s -> s.equals("z")).isPresent());
        assertFalse(CommonUtil.findFirstIndex(new ArrayList<String>(), s -> true).isPresent());
        assertEquals(2, CommonUtil.findFirstIndex(list, "c", (s, p) -> s.startsWith(p)).getAsInt());

        assertEquals(3, CommonUtil.findLastIndex(Arrays.asList(1, 2, 3, 2, 1), x -> x == 2).getAsInt());
        LinkedList<String> linked = new LinkedList<>(Arrays.asList("a", "b", "c", "b", "a"));
        assertEquals(3, CommonUtil.findLastIndex(linked, s -> s.equals("b")).getAsInt());
        assertFalse(CommonUtil.findLastIndex(new LinkedList<>(Arrays.asList("a", "b", "c")), s -> s.equals("z")).isPresent());

        String[] lastArr = { "a", "b", "c", "d", "c" };
        assertEquals(4, CommonUtil.findLastIndex(lastArr, s -> s.equals("c")).getAsInt());
        assertFalse(CommonUtil.findLastIndex(lastArr, s -> s.equals("z")).isPresent());
        assertFalse(CommonUtil.findLastIndex(new String[0], s -> true).isPresent());
        assertEquals(4, CommonUtil.findLastIndex(new String[] { "a", "b", "c", "d", "ca" }, "c", (s, p) -> s.startsWith(p)).getAsInt());

        List<String> lastList = Arrays.asList("a", "b", "c", "d", "c");
        assertEquals(4, CommonUtil.findLastIndex(lastList, s -> s.equals("c")).getAsInt());
        assertEquals(4, CommonUtil.findLastIndex(new LinkedList<>(lastList), s -> s.equals("c")).getAsInt());
        assertEquals(4, CommonUtil.findLastIndex(Arrays.asList("a", "b", "c", "d", "ca"), "c", (s, p) -> s.startsWith(p)).getAsInt());

        LinkedHashSet<String> set = new LinkedHashSet<>(Arrays.asList("a", "b", "c", "d"));
        OptionalInt setIdx = CommonUtil.findLastIndex(set, s -> s.equals("c"));
        assertTrue(setIdx.isPresent());
        assertEquals(2, setIdx.getAsInt());
        assertFalse(CommonUtil.findLastIndex(new LinkedHashSet<>(Arrays.asList("a", "b", "c")), s -> s.equals("z")).isPresent());
    }
}
