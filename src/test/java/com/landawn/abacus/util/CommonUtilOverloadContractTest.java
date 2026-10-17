package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

public class CommonUtilOverloadContractTest extends TestBase {
    record Sorter(String name, Consumer<String[]> sort, boolean reversed) {
        @Override
        public String toString() {
            return name;
        }
    }

    static List<Sorter> sorters() {
        return List.of(new Sorter("sortBy", a -> N.sortBy(a, Integer::valueOf), false), new Sorter("sortByInt", a -> N.sortByInt(a, Integer::parseInt), false),
                new Sorter("sortByLong", a -> N.sortByLong(a, Long::parseLong), false),
                new Sorter("sortByFloat", a -> N.sortByFloat(a, Float::parseFloat), false),
                new Sorter("sortByDouble", a -> N.sortByDouble(a, Double::parseDouble), false),
                new Sorter("parallelSortBy", a -> N.parallelSortBy(a, Integer::valueOf), false),
                new Sorter("parallelSortByInt", a -> N.parallelSortByInt(a, Integer::parseInt), false),
                new Sorter("parallelSortByLong", a -> N.parallelSortByLong(a, Long::parseLong), false),
                new Sorter("parallelSortByFloat", a -> N.parallelSortByFloat(a, Float::parseFloat), false),
                new Sorter("parallelSortByDouble", a -> N.parallelSortByDouble(a, Double::parseDouble), false),
                new Sorter("reverseSortBy", a -> N.reverseSortBy(a, Integer::valueOf), true),
                new Sorter("reverseSortByInt", a -> N.reverseSortByInt(a, Integer::parseInt), true),
                new Sorter("reverseSortByLong", a -> N.reverseSortByLong(a, Long::parseLong), true),
                new Sorter("reverseSortByFloat", a -> N.reverseSortByFloat(a, Float::parseFloat), true),
                new Sorter("reverseSortByDouble", a -> N.reverseSortByDouble(a, Double::parseDouble), true));
    }

    @ParameterizedTest
    @MethodSource("sorters")
    void arraySortOverloadsUseTheExtractedKey(Sorter sorter) {
        String[] values = { "30", "4", "12", "2" };
        sorter.sort.accept(values);
        assertArrayEquals(sorter.reversed ? new String[] { "30", "12", "4", "2" } : new String[] { "2", "4", "12", "30" }, values);
        String[] single = { "4" };
        sorter.sort.accept(single);
        assertArrayEquals(new String[] { "4" }, single);
        String[] empty = {};
        sorter.sort.accept(empty);
        assertEquals(0, empty.length);
    }

    static java.util.stream.Stream<Method> checks() {
        return Arrays.stream(CommonUtil.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("checkArgument") || m.getName().equals("checkState"))
                .filter(m -> java.lang.reflect.Modifier.isPublic(m.getModifiers()))
                .filter(m -> Arrays.stream(m.getParameterTypes())
                        .allMatch(t -> t.isPrimitive() || t == String.class || t == Object.class || t == Object[].class))
                .sorted(java.util.Comparator.comparing(Method::toGenericString));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("checks")
    void validationOverloadsFormatValuesAndRejectOnlyFalseConditions(Method method) throws Throwable {
        Class<?>[] types = method.getParameterTypes();
        Object[] args = new Object[types.length];
        args[0] = true;
        List<String> expectedValues = new ArrayList<>();
        for (int i = 2; i < types.length; i++) {
            if (types[i] == char.class)
                args[i] = 'x';
            else if (types[i] == int.class)
                args[i] = 7;
            else if (types[i] == long.class)
                args[i] = 8L;
            else if (types[i] == double.class)
                args[i] = 9.5D;
            else if (types[i] == Object.class)
                args[i] = "value";
            else if (types[i] == Object[].class)
                args[i] = new Object[] { "a", "b", "c", "d" };
            else
                throw new AssertionError(types[i]);
            if (args[i] instanceof Object[] array)
                for (Object value : array)
                    expectedValues.add(value.toString());
            else
                expectedValues.add(args[i].toString());
        }
        if (args.length > 1)
            args[1] = expectedValues.isEmpty() ? "invalid" : "invalid " + String.join(" ", java.util.Collections.nCopies(expectedValues.size(), "{}"));
        invoke(method, args);
        args[0] = false;
        Class<? extends RuntimeException> failureType = method.getName().equals("checkState") ? IllegalStateException.class : IllegalArgumentException.class;
        RuntimeException failure = assertThrows(failureType, () -> invoke(method, args));
        if (args.length > 1)
            assertEquals(expectedValues.isEmpty() ? "invalid" : "invalid " + String.join(" ", expectedValues), failure.getMessage());
    }

    static List<Object> primitiveArrays() {
        return List.of(new boolean[] { true, false }, new char[] { 'a', 'b' }, new byte[] { 1, 2 }, new short[] { 1, 2 }, new int[] { 1, 2 },
                new long[] { 1, 2 }, new float[] { 1, 2 }, new double[] { 1, 2 });
    }

    @ParameterizedTest
    @MethodSource("primitiveArrays")
    void primitiveArrayConversionsPreserveValuesAndDoNotAliasTheSource(Object array) {
        List<Object> expected = new ArrayList<>();
        for (int i = 0; i < java.lang.reflect.Array.getLength(array); i++)
            expected.add(java.lang.reflect.Array.get(array, i));
        List<?> converted = N.convert(array, List.class);
        assertEquals(expected, converted);
        Object roundTrip = N.convert(converted, array.getClass());
        assertNotSame(array, roundTrip);
        for (int i = 0; i < expected.size(); i++)
            assertEquals(expected.get(i), java.lang.reflect.Array.get(roundTrip, i));
        converted.clear();
        assertEquals(2, java.lang.reflect.Array.getLength(array));
        assertEquals(expected.get(0), java.lang.reflect.Array.get(array, 0));
    }

    static java.util.stream.Stream<Method> nullToEmptyArrays() {
        return Arrays.stream(CommonUtil.class.getDeclaredMethods())
                .filter(m -> m.getName().equals("nullToEmpty"))
                .filter(m -> m.getParameterCount() == 1 && m.getParameterTypes()[0].isArray());
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("nullToEmptyArrays")
    void nullToEmptyOverloadsPreserveExistingArrayIdentity(Method method) throws Throwable {
        Object empty = invoke(method, new Object[] { null });
        assertEquals(0, java.lang.reflect.Array.getLength(empty));
        Object array = java.lang.reflect.Array.newInstance(method.getParameterTypes()[0].getComponentType(), 2);
        assertSame(array, invoke(method, new Object[] { array }));
    }

    private static Object invoke(Method method, Object[] args) throws Throwable {
        try {
            return method.invoke(null, args);
        } catch (InvocationTargetException e) {
            throw e.getCause();
        }
    }
}
