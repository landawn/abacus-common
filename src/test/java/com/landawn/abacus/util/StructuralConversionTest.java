package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

import testfixtures.ConversionReviewFixtures.AliasList;
import testfixtures.ConversionReviewFixtures.PrefilledList;
import testfixtures.ConversionReviewFixtures.ReusedList;
import testfixtures.ConversionReviewFixtures.SameList;
import testfixtures.ConversionReviewFixtures.Supplied;
import testfixtures.ConversionReviewFixtures.Wrong;

public class StructuralConversionTest extends TestBase {
    private static final Type<List<String>> STRINGS = new TypeReference<List<String>>() {
    }.type();
    private static final Type<List<Integer>> INTS = new TypeReference<List<Integer>>() {
    }.type();

    @Test
    void allElementsAndNestedContainersAreConvertedWithoutMutatingSources() {
        List<Object> mixed = new ArrayList<>(Arrays.asList(null, "\u4e2d\ud83d\ude00", 2));
        List<String> result = CommonUtil.convert(mixed, STRINGS);
        assertEquals(Arrays.asList(null, "\u4e2d\ud83d\ude00", "2"), result);
        assertNotSame(mixed, result);
        result.set(2, "changed");
        assertEquals(2, mixed.get(2));
        assertEquals("2", CommonUtil.convert(new LinkedHashSet<>(mixed), STRINGS).get(2));
        assertEquals("2", CommonUtil.convert(mixed, new TypeReference<LinkedList<String>>() {
        }.type()).get(2));
        List<List<String>> nested = CommonUtil.convert(List.of(Arrays.asList("a", 2)), new TypeReference<List<List<String>>>() {
        }.type());
        assertEquals(List.of(List.of("a", "2")), nested);
        Map<Object, Object> source = new LinkedHashMap<>();
        source.put(null, null);
        source.put("1", Arrays.asList(null, "2", 3));
        Map<Integer, List<Integer>> converted = CommonUtil.convert(source, new TypeReference<Map<Integer, List<Integer>>>() {
        }.type());
        assertNull(converted.get(null));
        assertTrue(converted.containsKey(null));
        assertEquals(Arrays.asList(null, 2, 3), converted.get(1));
        assertTrue(source.containsKey("1"));
        assertThrows(NumberFormatException.class, () -> CommonUtil.convert(Arrays.asList("1", "bad"), INTS));
        assertNull(CommonUtil.convert(null, INTS));
        assertTrue(CommonUtil.convert(List.of(), INTS).isEmpty());
    }

    @Test
    void arraysIncludePrimitiveDefaultsGenericArraysAndNestedGenericComponents() {
        assertEquals(List.of("1", "2"), CommonUtil.convert(new int[] { 1, 2 }, STRINGS));
        assertEquals(Arrays.asList(null, "a", "2"), CommonUtil.convert(new Object[] { null, "a", 2 }, STRINGS));
        assertArrayEquals(new int[] { 1, 0, 3 }, CommonUtil.convert(Arrays.asList("1", null, "3"), int[].class));
        assertArrayEquals(new String[] { "a", "2" }, CommonUtil.convert(Arrays.asList("a", 2), String[].class));
        List<?>[] input = { Arrays.asList("a", 2) };
        Type<List<String>[]> arrayType = new TypeReference<List<String>[]>() {
        }.type();
        List<String>[] normalized = CommonUtil.convert(input, arrayType);
        assertNotSame(input, normalized);
        assertEquals(List.of("a", "2"), normalized[0]);
        assertEquals(2, input[0].get(1));
        assertEquals(List.of("a", "2"), CommonUtil.convert(List.of(Arrays.asList("a", 2)), arrayType)[0]);
        Type<Map<String, List<String>[]>[]> deepType = new TypeReference<Map<String, List<String>[]>[]>() {
        }.type();
        Map<String, List<String>[]>[] deep = CommonUtil.convert(List.of(Map.of("x", input)), deepType);
        assertEquals("2", deep[0].get("x")[0].get(1));
    }

    @Test
    void rawIdentityAndRepeatedReferencesRemainValidWhileCyclesAreRejectedWhenTraversed() {
        List<Object> cycle = new ArrayList<>();
        cycle.add(cycle);
        assertSame(cycle, CommonUtil.convert(cycle, List.class));
        assertSame(cycle, CommonUtil.convert(cycle, new TypeReference<List<Object>>() {
        }.type()));
        assertSame(cycle, CommonUtil.convert(cycle, Object.class));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(cycle, STRINGS));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(List.of(cycle), STRINGS));
        List<Object> tail = cycle;
        for (int i = 0; i < 10_000; i++) {
            tail = new ArrayList<>(Collections.singletonList(tail));
        }
        final List<Object> deepCycle = tail;
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(deepCycle, STRINGS));
        List<Object> shared = new ArrayList<>(List.of(1));
        Type<List<List<String>>> target = new TypeReference<List<List<String>>>() {
        }.type();
        assertEquals(List.of(List.of("1"), List.of("1")), CommonUtil.convert(Arrays.asList(shared, shared), target));
        int[] primitive = { 1 };
        assertSame(primitive, CommonUtil.convert(primitive, int[].class));
        String[] strings = { "a" };
        assertSame(strings, CommonUtil.convert(strings, String[].class));
    }

    @Test
    void convertedMapKeysFollowTargetEqualityAndCollisionsAreRejected() {
        Map<String, String> source = new LinkedHashMap<>();
        source.put("1", "a");
        source.put("01", "b");
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(source, new TypeReference<Map<Integer, String>>() {
        }.type()));
        assertEquals(2, source.size());
        Map<String, String> decimals = Map.of("1.0", "a", "1.00", "b");
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(decimals, new TypeReference<TreeMap<BigDecimal, String>>() {
        }.type()));
        IdentityHashMap<String, Object> identity = new IdentityHashMap<>();
        identity.put(new String("x"), "1");
        identity.put(new String("x"), 2);
        assertEquals(2, CommonUtil.convert(identity, new TypeReference<IdentityHashMap<String, Integer>>() {
        }.type()).size());
    }

    @Test
    void registeredConvertersRunOnceAndTheirOutputIsCheckedAndNormalized() {
        AtomicInteger calls = new AtomicInteger();
        CommonUtil.registerConverter(Supplied.class, (Supplied source, Class<?> target) -> {
            calls.incrementAndGet();
            return source.value;
        });
        assertEquals(List.of("1", "2"), CommonUtil.convert(new Supplied(Arrays.asList(1, "2")), STRINGS));
        assertEquals(1, calls.get());
        AtomicInteger selfCalls = new AtomicInteger();
        CommonUtil.registerConverter(SameList.class, (SameList source, Class<?> target) -> {
            selfCalls.incrementAndGet();
            return source;
        });
        SameList same = new SameList();
        same.add(2);
        assertEquals(List.of("2"), CommonUtil.convert(same, STRINGS));
        assertEquals(1, selfCalls.get());
        CommonUtil.registerConverter(Wrong.class, (Wrong source, Class<?> target) -> 42);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(new Wrong(), String.class));
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(new Wrong(), STRINGS));
    }

    @Test
    void factoriesMustReturnFreshEmptyContainersWithoutInputOrOutputAliasing() {
        AliasList<Object> input = new AliasList<>();
        IntFunctions.registerForCollection(AliasList.class, capacity -> input);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(input, new TypeReference<AliasList<String>>() {
        }.type()));
        PrefilledList<Object> prefilled = new PrefilledList<>();
        prefilled.add("existing");
        IntFunctions.registerForCollection(PrefilledList.class, capacity -> prefilled);
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(List.of(1), new TypeReference<PrefilledList<String>>() {
        }.type()));
        assertEquals(List.of("existing"), prefilled);
        ReusedList<Object> reused = new ReusedList<>();
        IntFunctions.registerForCollection(ReusedList.class, capacity -> reused);
        Type<List<ReusedList<String>>> nested = new TypeReference<List<ReusedList<String>>>() {
        }.type();
        assertThrows(IllegalArgumentException.class, () -> CommonUtil.convert(List.of(List.of(), List.of()), nested));
    }

    @Test
    void parserFallbackAndResourceLeafConversionRetainBehavior() {
        assertEquals(Arrays.asList(1, 2, null), CommonUtil.convert("[1,\"2\",null]", INTS));
        assertEquals(List.of(List.of("1", "2")), CommonUtil.convert("[[1,\"2\"]]", new TypeReference<List<List<String>>>() {
        }.type()));
        ClosingReader reader = new ClosingReader("\u4e2d\ud83d\ude00");
        assertEquals(List.of("\u4e2d\ud83d\ude00"), CommonUtil.convert(List.of(reader), STRINGS));
        assertEquals(1, reader.closes);
        ClosingReader chars = new ClosingReader("中🙂");
        assertArrayEquals("中🙂".toCharArray(), CommonUtil.convert(chars, char[].class));
        assertEquals(1, chars.closes);
        byte[] bytes = { 0, -1, 127 };
        assertArrayEquals(bytes, CommonUtil.convert(new java.io.ByteArrayInputStream(bytes), CommonUtil.typeOf(byte[].class)));
    }

    private static final class ClosingReader extends StringReader {
        int closes;

        ClosingReader(String value) {
            super(value);
        }

        @Override
        public void close() {
            closes++;
            super.close();
        }
    }
}
