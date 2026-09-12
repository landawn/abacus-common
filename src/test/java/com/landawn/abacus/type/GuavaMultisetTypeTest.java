package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.LinkedHashMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.SortedMultiset;
import com.google.common.collect.TreeMultiset;
import com.landawn.abacus.TestBase;

public class GuavaMultisetTypeTest extends TestBase {

    private GuavaMultisetType<String, Multiset<String>> multisetType;

    @BeforeEach
    public void setUp() {
        multisetType = (GuavaMultisetType<String, Multiset<String>>) createType("com.google.common.collect.Multiset<String>");
    }

    @Test
    public void testDeclaringName() {
        String declaringName = multisetType.declaringName();
        assertNotNull(declaringName);
        assertTrue(declaringName.contains("Multiset"));
        assertTrue(declaringName.contains("String"));
    }

    @Test
    public void testClazz() {
        assertEquals(Multiset.class, multisetType.javaType());
    }

    @Test
    public void testGetElementType() {
        Type<?> elementType = multisetType.elementType();
        assertNotNull(elementType);
    }

    @Test
    public void testGetParameterTypes() {
        List<Type<?>> paramTypes = multisetType.parameterTypes();
        assertNotNull(paramTypes);
        assertEquals(1, paramTypes.size());
    }

    @Test
    public void testIsGenericType() {
        assertTrue(multisetType.isParameterizedType());
    }

    @Test
    public void testIsSerializable() {
        assertTrue(multisetType.isSerializable());
    }

    @Test
    public void testStringOf() {
        assertNull(multisetType.stringOf(null));

    }

    @Test
    public void testStringOf_WithContent() {
        HashMultiset<String> multiset = HashMultiset.create();
        multiset.add("foo");
        multiset.add("bar");

        String str = multisetType.stringOf(multiset);
        assertNotNull(str);
        assertTrue(str.contains("foo") || str.length() > 0);
    }

    @Test
    public void testStringOfSizesTemporaryMapByDistinctElements() {
        final Multiset<String> multiset = org.mockito.Mockito.mock(Multiset.class);
        org.mockito.Mockito.when(multiset.size()).thenThrow(new AssertionError("total occurrence count must not size the temporary map"));
        org.mockito.Mockito.when(multiset.elementSet()).thenReturn(Set.of("x"));
        org.mockito.Mockito.when(multiset.count("x")).thenReturn(Integer.MAX_VALUE);

        final String str = multisetType.stringOf(multiset);

        assertTrue(str.contains(String.valueOf(Integer.MAX_VALUE)));
    }

    @Test
    public void testSortedMultisetSerializationPreservesSortOrder() {
        final Type<SortedMultiset<String>> sortedType = TypeFactory.getType("com.google.common.collect.SortedMultiset<String>");
        final SortedMultiset<String> multiset = TreeMultiset.create();
        multiset.add("d");
        multiset.add("a");

        final String str = sortedType.stringOf(multiset);

        assertTrue(str.indexOf("\"a\"") < str.indexOf("\"d\""));
    }

    @Test
    public void testHashMultisetType_valueOf() {
        GuavaMultisetType<String, HashMultiset<String>> hashType = (GuavaMultisetType<String, HashMultiset<String>>) createType(
                "com.google.common.collect.HashMultiset<String>");

        HashMultiset<String> original = HashMultiset.create();
        original.add("x");
        original.add("x");
        original.add("y");

        String str = hashType.stringOf(original);
        HashMultiset<String> result = hashType.valueOf(str);
        assertNotNull(result);
        assertEquals(2, result.count("x"));
        assertEquals(1, result.count("y"));
    }

    @Test
    public void testLinkedHashMultisetType_valueOf() {
        GuavaMultisetType<String, LinkedHashMultiset<String>> linkedType = (GuavaMultisetType<String, LinkedHashMultiset<String>>) createType(
                "com.google.common.collect.LinkedHashMultiset<String>");

        LinkedHashMultiset<String> original = LinkedHashMultiset.create();
        original.add("a");
        original.add("b");
        original.add("a");

        String str = linkedType.stringOf(original);
        LinkedHashMultiset<String> result = linkedType.valueOf(str);
        assertNotNull(result);
        assertEquals(2, result.count("a"));
    }

    @Test
    public void testValueOf() {
        assertNull(multisetType.valueOf(null));
        assertNull(multisetType.valueOf(""));

    }

    @Test
    public void testValueOf_WithContent() {
        // Create a multiset and round-trip through string
        HashMultiset<String> original = HashMultiset.create();
        original.add("apple");
        original.add("banana");
        original.add("apple");

        String str = multisetType.stringOf(original);
        assertNotNull(str);

        Multiset<String> result = multisetType.valueOf(str);
        assertNotNull(result);
        assertEquals(2, result.count("apple"));
        assertEquals(1, result.count("banana"));
    }

    @Test
    public void testGetTypeName() {
        String typeName = GuavaMultisetType.getTypeName(Multiset.class, "String", true);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Multiset"));

        typeName = GuavaMultisetType.getTypeName(Multiset.class, "String", false);
        assertNotNull(typeName);
        assertTrue(typeName.contains("Multiset"));
    }

    // --- regression tests for 2026-06-10 deep-review fixes ---

    @Test
    public void testValueOfImmutableMultisetTarget() {
        // regression: immutable targets are abstract, so valueOf returned a mutable HashMultiset
        // -> ClassCastException when assigned to the declared ImmutableMultiset
        final Type<com.google.common.collect.ImmutableMultiset<String>> t = TypeFactory.getType("com.google.common.collect.ImmutableMultiset<String>");
        final com.google.common.collect.ImmutableMultiset<String> ms = t.valueOf("{\"apple\":2,\"pear\":1}");

        org.junit.jupiter.api.Assertions.assertEquals(2, ms.count("apple"));
        org.junit.jupiter.api.Assertions.assertEquals(1, ms.count("pear"));
    }

    // T8-02: ImmutableSortedMultiset.copyOf(Iterable) expanded every occurrence (OutOfMemoryError for a large count).
    @Test
    public void reviewFixes20260906_immutableSortedMultisetIsBuiltEntryByEntry() {
        final Type<com.google.common.collect.ImmutableSortedMultiset<String>> t = TypeFactory
                .getType("com.google.common.collect.ImmutableSortedMultiset<String>");

        // Integer.MAX_VALUE, not a merely large count: the old copyOf(Iterable) asked for an
        // Object[Integer.MAX_VALUE] up front, which fails with "Requested array size exceeds VM limit" on any heap in
        // a few milliseconds. A count such as 200,000,000 only takes about 2.5 s on a 2 GB heap, so a timeout budget
        // around it makes the pin depend on the machine (it passed against the unfixed code in 1 of 3 runs here).
        // The error must be caught here: JUnit treats an escaping OutOfMemoryError as unrecoverable and aborts the
        // whole run instead of failing this test. Nothing is actually allocated on this path.
        final long startNano = System.nanoTime();
        com.google.common.collect.ImmutableSortedMultiset<String> big = null;

        try {
            big = t.valueOf("{\"b\": 1, \"a\": 2147483647}");
        } catch (final OutOfMemoryError e) {
            fail("valueOf must copy the entry counts, not expand every occurrence: " + e);
        }

        final long elapsedMillis = (System.nanoTime() - startNano) / 1_000_000;
        assertTrue(elapsedMillis < 5_000, "an entry-by-entry copy is immediate; took " + elapsedMillis + " ms");

        assertEquals(Integer.MAX_VALUE, big.count("a"));
        assertEquals(1, big.count("b"));
        assertEquals(java.util.Arrays.asList("a", "b"), new java.util.ArrayList<>(big.elementSet()));
        assertTrue(big instanceof com.google.common.collect.ImmutableSortedMultiset);

        // sorted order, empty document, zero count, Integer elements
        assertEquals(java.util.Arrays.asList("a", "b"), new java.util.ArrayList<>(t.valueOf("{\"b\": 1, \"a\": 2}").elementSet()));
        assertEquals(2, t.valueOf("{\"b\": 1, \"a\": 2}").count("a"));
        assertTrue(t.valueOf("{}").isEmpty());
        assertTrue(t.valueOf("{}") instanceof com.google.common.collect.ImmutableSortedMultiset);
        assertTrue(t.valueOf("{\"a\": 0}").isEmpty());

        final Type<com.google.common.collect.ImmutableSortedMultiset<Integer>> ti = TypeFactory
                .getType("com.google.common.collect.ImmutableSortedMultiset<Integer>");
        assertEquals(java.util.Arrays.asList(9, 10), new java.util.ArrayList<>(ti.valueOf("{\"10\": 1, \"9\": 2}").elementSet()));
        assertEquals(2, ti.valueOf("{\"10\": 1, \"9\": 2}").count(9));
    }

    // T8-01: the ordered targets (LinkedHashMultiset, ImmutableMultiset) were fed from an unordered HashMap intermediate.
    @Test
    public void reviewFixes20260906_orderedTargetsKeepDocumentOrder() {
        final String json = "{\"z\": 1, \"a\": 2, \"m\": 3, \"b\": 4, \"q\": 5}";
        final List<String> expected = java.util.Arrays.asList("z", "a", "m", "b", "q");

        final Type<LinkedHashMultiset<String>> linked = TypeFactory.getType("com.google.common.collect.LinkedHashMultiset<String>");
        assertEquals(expected, new java.util.ArrayList<>(linked.valueOf(json).elementSet()));
        assertEquals(json, linked.stringOf(linked.valueOf(json)));
        assertEquals(json, linked.stringOf(linked.valueOf(linked.stringOf(linked.valueOf(json)))));

        final Type<com.google.common.collect.ImmutableMultiset<String>> immutable = TypeFactory.getType("com.google.common.collect.ImmutableMultiset<String>");
        final com.google.common.collect.ImmutableMultiset<String> im = immutable.valueOf(json);
        assertEquals(expected, new java.util.ArrayList<>(im.elementSet()));
        assertEquals(5, im.count("q"));
        assertTrue(immutable.valueOf("{}").isEmpty());

        final String unicode = "{\"é\": 1, \"中\": 2, \"😀\": 3}";
        assertEquals(unicode, linked.stringOf(linked.valueOf(unicode)));

        // unordered / sorted targets keep their runtime classes
        assertEquals(HashMultiset.class, multisetType.valueOf(json).getClass());
        assertEquals(HashMultiset.class, TypeFactory.getType("com.google.common.collect.HashMultiset<String>").valueOf(json).getClass());
        assertEquals(TreeMultiset.class, TypeFactory.getType("com.google.common.collect.SortedMultiset<String>").valueOf(json).getClass());
        assertEquals(new java.util.HashSet<>(expected), multisetType.valueOf(json).elementSet());
    }

    // T8-05: {"a": null} threw an unboxing NullPointerException; now the element is simply not added (like a 0 count).
    @Test
    public void reviewFixes20260906_nullOrZeroCountAddsNothing() {
        for (final String typeName : new String[] { "com.google.common.collect.Multiset<String>", "com.google.common.collect.HashMultiset<String>",
                "com.google.common.collect.LinkedHashMultiset<String>", "com.google.common.collect.TreeMultiset<String>",
                "com.google.common.collect.ImmutableMultiset<String>", "com.google.common.collect.ImmutableSortedMultiset<String>" }) {
            final Type<Multiset<String>> t = TypeFactory.getType(typeName);

            final Multiset<String> ms = t.valueOf("{\"a\": null, \"b\": 1, \"c\": 0}");
            assertEquals(0, ms.count("a"), typeName);
            assertEquals(1, ms.count("b"), typeName);
            assertEquals(java.util.Collections.singleton("b"), ms.elementSet(), typeName);
            assertTrue(t.valueOf("{\"a\": null}").isEmpty(), typeName);

            org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> t.valueOf("{\"a\": -1}"), typeName);
            org.junit.jupiter.api.Assertions.assertThrows(NumberFormatException.class, () -> t.valueOf("{\"a\": \"x\"}"), typeName);
            org.junit.jupiter.api.Assertions.assertThrows(ArithmeticException.class, () -> t.valueOf("{\"a\": 2147483648}"), typeName);
            org.junit.jupiter.api.Assertions.assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> t.valueOf("{\"a\": 1"), typeName);
        }
    }
}
