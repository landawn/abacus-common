package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collector.Characteristics;
import java.util.stream.Collector;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.BiMap;
import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.stream.Collectors.MoreCollectors;
import com.landawn.abacus.util.u.Optional;

/** Collectors composition contracts. */
public class CollectorsCompositionTest extends CollectorsTestSupport {

    @Test
    public void testCollectingAndThen() {
        Integer result = stringList.stream().collect(Collectors.collectingAndThen(Collectors.toList(), List::size));
        assertEquals(5, result);
    }

    @Test
    public void testCollectingAndThenPreservesConcurrentCharacteristic() {
        final Collector<String, ?, Integer> collector = Collectors
                .collectingAndThen(Collectors.toConcurrentMap(Function.identity(), Function.identity(), (a, b) -> a, ConcurrentHashMap::new), Map::size);

        assertTrue(collector.characteristics().contains(Characteristics.CONCURRENT));
        assertTrue(collector.characteristics().contains(Characteristics.UNORDERED));
        assertFalse(collector.characteristics().contains(Characteristics.IDENTITY_FINISH));
        assertEquals(3, java.util.stream.Stream.of("a", "b", "c", "a").parallel().collect(collector));
    }

    @Test
    public void testCollectingOrDefaultIfEmpty() {
        List<String> defaultList = Arrays.asList("default");
        List<String> result = Stream.<String> empty().collect(Collectors.collectingOrDefaultIfEmpty(Collectors.toList(), defaultList));
        assertEquals(defaultList, result);
    }

    @Test
    public void testCollectingOrElseGetIfEmpty() {
        List<String> result = Stream.<String> empty().collect(Collectors.collectingOrElseGetIfEmpty(Collectors.toList(), () -> Arrays.asList("default")));
        assertEquals(Arrays.asList("default"), result);
    }

    // collectingOrElseThrowIfEmpty

    @Test
    public void testCollectingOrElseThrowIfEmpty() {
        assertThrows(NoSuchElementException.class, () -> Stream.<String> empty().collect(Collectors.collectingOrElseThrowIfEmpty(Collectors.toList())));
    }

    @Test
    public void testCollectingOrElseThrowIfEmpty_WithExceptionSupplier() {
        assertThrows(IllegalStateException.class,
                () -> Stream.<String> empty().collect(Collectors.collectingOrElseThrowIfEmpty(Collectors.toList(), IllegalStateException::new)));
    }

    @Test
    public void testCollectingOrElseThrowIfEmptyWithSupplier() {
        List<String> result = Stream.of("a", "b", "c")
                .collect(Collectors.collectingOrElseThrowIfEmpty(Collectors.toList(), () -> new IllegalStateException("Empty stream")));

        Assertions.assertEquals(Arrays.asList("a", "b", "c"), result);

        Assertions.assertThrows(IllegalStateException.class, () -> Stream.<String> empty()
                .collect(Collectors.collectingOrElseThrowIfEmpty(Collectors.toList(), () -> new IllegalStateException("Empty stream"))));
    }

    // collectingOrEmpty

    @Test
    public void testCollectingOrEmpty() {
        Optional<List<String>> result = stringList.stream().collect(Collectors.collectingOrEmpty(Collectors.toList()));
        assertTrue(result.isPresent());
        assertEquals(stringList, result.get());
    }

    @Test
    public void testCollectingOrEmpty_Empty() {
        Optional<List<String>> result = Stream.<String> empty().collect(Collectors.collectingOrEmpty(Collectors.toList()));
        assertFalse(result.isPresent());
    }

    @Test
    public void testCollectingOrEmptyNullDownstreamResultThrowsNPE() {
        assertThrows(NullPointerException.class, () -> Stream.of("a")
                .collect(Collectors.collectingOrEmpty(Collectors.collectingAndThen(Collectors.<String> toList(), list -> (List<String>) null))));
    }

    @Test
    public void testCollectingOrEmptyTracksPerContainerNotPerCollector() {
        final java.util.stream.Collector<Integer, ?, Optional<List<Integer>>> c = Collectors.collectingOrEmpty(Collectors.toList());

        // regression: after one non-empty use, an empty stream returned Optional[[]] instead of empty
        assertEquals(Optional.of(N.asList(1)), Stream.of(1).collect(c));
        assertFalse(Stream.<Integer> empty().collect(c).isPresent());

        // regression: in groupingBy, a group whose elements were all filtered out got Optional[[]]
        final Map<String, Optional<List<Integer>>> grouped = Stream.of(1, 2, 11)
                .collect(Collectors.groupingBy(i -> i < 10 ? "small" : "big",
                        Collectors.filtering(i -> i % 2 == 0, Collectors.collectingOrEmpty(Collectors.toList()))));

        assertEquals(Optional.of(N.asList(2)), grouped.get("small"));
        assertFalse(grouped.get("big").isPresent());
    }

    @Test
    public void test_combine_three() {
        Tuple3<Long, Integer, Optional<Integer>> result = Stream.of(1, 2, 3, 4, 5)
                .collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(i -> i), Collectors.maxBy(Fn.identity())));
        assertEquals(5L, result._1);
        assertEquals(15, result._2);
        assertEquals(Optional.of(5), result._3);
    }

    @Test
    public void test_combine_two() {
        Tuple2<Long, Integer> result = Stream.of(1, 2, 3, 4, 5).collect(MoreCollectors.combine(Collectors.counting(), Collectors.summingInt(i -> i)));
        assertEquals(5L, result._1);
        assertEquals(15, result._2);
    }

    // commonPrefix

    @Test
    public void testCommonPrefix() {
        List<String> words = Arrays.asList("apple", "application", "apply");
        String result = words.stream().collect(Collectors.commonPrefix());
        assertEquals("appl", result);
    }

    @Test
    public void testCommonPrefix_Empty() {
        String result = Stream.<String> empty().collect(Collectors.commonPrefix());
        assertEquals("", result);
    }

    @Test
    public void testCommonPrefix_NoCommon() {
        String result = stringList.stream().collect(Collectors.commonPrefix());
        assertEquals("", result);
    }

    @Test
    public void testCommonPrefix_NoCommonPrefix() {
        String result = Stream.of("abc", "xyz").collect(Collectors.commonPrefix());
        assertEquals("", result);
    }

    @Test
    public void testCommonPrefix_Parallel_CombinerUsed() {
        // Exercises commonPrefix combiner in parallel
        String result = Stream.of("test_alpha", "test_beta", "test_gamma", "test_delta", "test_epsilon", "test_zeta")
                .parallel()
                .collect(Collectors.commonPrefix());
        assertEquals("test_", result);
    }

    @Test
    public void testCommonPrefix_WithValues() {
        String result = Stream.of("abcdef", "abcxyz", "abcpqr").collect(Collectors.commonPrefix());
        assertEquals("abc", result);
    }

    // G112-06: commonPrefix()/commonSuffix() compare at most (current length + 1) chars per element - randomized differential test
    // against the previous full-scan accumulator, incl. surrogate pairs and lone surrogates at the boundary, null/empty elements,
    // long shared runs, StringBuilder elements and the combiner
    @Test
    public void testCommonPrefixSuffix_boundedScanMatchesFullScan_G112() {
        final char[] alphabet = { 'a', 'b', (char) 0xD83D, (char) 0xDE00, (char) 0xDE01 };
        final String pair0 = new String(new char[] { (char) 0xD83D, (char) 0xDE00 });
        final String pair1 = new String(new char[] { (char) 0xD83D, (char) 0xDE01 });
        final String pair2 = new String(new char[] { (char) 0xD83E, (char) 0xDE00 });
        final java.util.Random random = new java.util.Random(20260926L);

        for (int round = 0; round < 20000; round++) {
            final String shared = g112RandomString(random, alphabet, random.nextInt(4) == 0 ? 12 : 3);
            final int count = 1 + random.nextInt(6);
            final List<CharSequence> elements = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                final int kind = random.nextInt(20);

                if (kind == 0) {
                    elements.add(null);
                } else if (kind == 1) {
                    elements.add("");
                } else {
                    final String tail = g112RandomString(random, alphabet, 4);
                    final String head = random.nextBoolean() ? shared.substring(0, random.nextInt(shared.length() + 1)) : shared;
                    final String s = random.nextBoolean() ? head + tail : tail + head;
                    elements.add(kind == 2 ? new StringBuilder(s) : s);
                }
            }

            for (final boolean prefix : new boolean[] { true, false }) {
                final String expected = g112CommonAffixReference(elements, prefix);
                assertEquals(expected, prefix ? elements.stream().collect(Collectors.commonPrefix()) : elements.stream().collect(Collectors.commonSuffix()),
                        elements + " prefix=" + prefix);

                final int split = random.nextInt(elements.size() + 1);
                final List<CharSequence> first = elements.subList(0, split);
                final List<CharSequence> second = elements.subList(split, elements.size());
                final String expectedSplit;

                if (first.isEmpty()) {
                    expectedSplit = g112CommonAffixReference(second, prefix);
                } else if (second.isEmpty()) {
                    expectedSplit = g112CommonAffixReference(first, prefix);
                } else {
                    final List<CharSequence> merged = new ArrayList<>(first);
                    merged.add(g112CommonAffixReference(second, prefix));
                    expectedSplit = g112CommonAffixReference(merged, prefix);
                }

                assertEquals(expectedSplit, g112CollectSplit(prefix ? Collectors.commonPrefix() : Collectors.commonSuffix(), first, second),
                        elements + " split=" + split + " prefix=" + prefix);
            }
        }

        // long shared runs beyond the current common prefix / suffix
        final String run = com.landawn.abacus.util.Strings.repeat("z", 500);
        assertEquals("a", Stream.of("aa" + run, "ab" + run, "aa" + run, "aa" + run).collect(Collectors.commonPrefix()));
        assertEquals("a", Stream.of(run + "aa", run + "ba", run + "aa", run + "aa").collect(Collectors.commonSuffix()));
        assertEquals("aa" + run, Stream.of("aa" + run, "aa" + run + "x", "aa" + run + "y").collect(Collectors.commonPrefix()));
        assertEquals("x" + pair0, Stream.of("x" + pair0 + "a", "x" + pair0 + "b", "x" + pair0 + "a").collect(Collectors.commonPrefix()));
        assertEquals("x", Stream.of("x" + pair0, "x" + pair1, "x" + pair0).collect(Collectors.commonPrefix()));
        assertEquals("", Stream.of(pair0, pair1).collect(Collectors.commonPrefix()));
        assertEquals("", Stream.of(pair0, pair2).collect(Collectors.commonSuffix()));
        assertEquals("b", Stream.of(pair0 + "b", pair2 + "b", pair0 + "b").collect(Collectors.commonSuffix()));
    }

    // commonSuffix

    @Test
    public void testCommonSuffix() {
        List<String> words = Arrays.asList("running", "jumping", "walking");
        String result = words.stream().collect(Collectors.commonSuffix());
        assertEquals("ing", result);
    }

    @Test
    public void testCommonSuffix_Empty() {
        String result = Stream.<String> empty().collect(Collectors.commonSuffix());
        assertEquals("", result);
    }

    @Test
    public void testCommonSuffix_NoCommon() {
        String result = stringList.stream().collect(Collectors.commonSuffix());
        assertEquals("", result);
    }

    @Test
    public void testCommonSuffix_Parallel_CombinerUsed() {
        // Exercises commonSuffix combiner in parallel
        String result = Stream.of("alpha_suffix", "beta_suffix", "gamma_suffix", "delta_suffix", "epsilon_suffix", "zeta_suffix")
                .parallel()
                .collect(Collectors.commonSuffix());
        assertEquals("_suffix", result);
    }

    @Test
    public void testCommonSuffixCombinerNullTolerant() {
        // regression: the commonSuffix combiner dereferenced b.left() before the right == 0 check,
        // throwing NPE in parallel collection when a partition started with a null element
        assertEquals("", java.util.stream.Stream.of("abc", null, "xbc", "bc").parallel().collect(Collectors.commonSuffix()));
        assertEquals("_sfx", java.util.stream.Stream.of("test_sfx", "demo_sfx").parallel().collect(Collectors.commonSuffix()));
    }

    @Test
    public void testDistinctByPreservesFirstOccurrenceOrder() {
        // regression: distinctByToList/distinctByToCollection accumulate into a LinkedHashMap and
        // document first-occurrence encounter order, but advertised UNORDERED, which permits
        // order-discarding optimizations in JDK parallel pipelines
        assertFalse(Collectors.distinctByToList(Function.identity()).characteristics().contains(Characteristics.UNORDERED));

        final List<Integer> expected = new ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            expected.add(i);
        }

        // first occurrence of key (i % 1000) is i itself for i in [0, 1000)
        final List<Integer> result = java.util.stream.IntStream.range(0, 2000).boxed().parallel().collect(Collectors.distinctByToList(i -> i % 1000));
        assertEquals(expected, result);

        // the counting variant is order-insensitive and stays UNORDERED
        assertTrue(Collectors.distinctByToCounting(Function.identity()).characteristics().contains(Characteristics.UNORDERED));
    }

    @Test
    public void testDistinctByToCollection() {
        Set<Person> result = personList.stream().collect(Collectors.distinctByToCollection(Person::getDepartment, HashSet::new));
        assertEquals(3, result.size());
    }

    @Test
    public void testDistinctByToCollection_Parallel_CombinerUsed() {
        // Triggers lambda$202 (distinctByToCollection combiner) in parallel
        List<String> result = Stream.of("apple", "apricot", "banana", "blueberry", "cherry", "cranberry")
                .parallel()
                .collect(Collectors.distinctByToCollection(s -> s.charAt(0), ArrayList::new));
        assertEquals(3, result.size());
    }

    @Test
    public void testDistinctByToCounting() {
        Integer result = personList.stream().collect(Collectors.distinctByToCounting(Person::getDepartment));
        assertEquals(3, result);
    }

    @Test
    public void testDistinctByToList() {
        List<Person> result = personList.stream().collect(Collectors.distinctByToList(Person::getDepartment));
        assertEquals(3, result.size());
    }

    @Test
    public void testFiltering() {
        List<String> result = stringList.stream().collect(Collectors.filtering(s -> s.startsWith("a"), Collectors.toList()));
        assertEquals(Arrays.asList("apple"), result);
    }

    @Test
    public void testFilteringToList() {
        List<String> result = stringList.stream().collect(Collectors.filteringToList(s -> s.startsWith("b")));
        assertEquals(Arrays.asList("banana"), result);
    }

    @Test
    public void testFlatMapping() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4))
                .collect(Collectors.flatMapping(list -> Stream.of(list), Collectors.toList()));

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testFlatMappingClosesMappedStreams() {
        final java.util.concurrent.atomic.AtomicBoolean closed = new java.util.concurrent.atomic.AtomicBoolean();

        final List<Integer> result = Stream.of(1)
                .collect(Collectors.flatMapping(value -> Stream.of(value, value + 1).onClose(() -> closed.set(true)), Collectors.toList()));

        assertEquals(Arrays.asList(1, 2), result);
        assertTrue(closed.get());
    }

    // flatmapping

    @Test
    public void testFlatmapping() {
        List<String> words = Arrays.asList("hello world", "foo bar");
        List<String> result = words.stream().collect(Collectors.flatmapping(s -> Arrays.asList(s.split(" ")), Collectors.toList()));
        assertEquals(Arrays.asList("hello", "world", "foo", "bar"), result);
    }

    @Test
    public void testFlatMappingCollection() {
        List<List<String>> nested = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"));
        List<String> result = nested.stream().collect(Collectors.flatmapping(Function.identity(), Collectors.toList()));
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testFlatmappingWithCollection() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4)).collect(Collectors.flatmapping(e -> e, Collectors.toList()));

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    // flatMappingKeyToMultimap

    @Test
    public void testFlatMappingKeyToMultimap() {
        ListMultimap<Integer, String> multimap = Stream.of("a", "bb")
                .collect(Collectors.flatMappingKeyToMultimap(s -> Stream.of(1, 2, 3).limit(s.length()), e -> e));

        Assertions.assertEquals(Arrays.asList("a", "bb"), multimap.get(1));
        Assertions.assertEquals(Arrays.asList("bb"), multimap.get(2));
    }

    @Test
    public void testFlatMappingKeyToMultimapNullStreamIsEmpty() {
        final ListMultimap<Integer, String> multimap = Stream.of("a").collect(Collectors.flatMappingKeyToMultimap(s -> (Stream<Integer>) null, Fn.identity()));

        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testFlatMappingKeyToMultimapWithMapFactory() {
        ListMultimap<Integer, String> multimap = Stream.of("a", "bb")
                .collect(Collectors.flatMappingKeyToMultimap(s -> Stream.of(1, 2, 3).limit(s.length()), e -> e, Suppliers.ofListMultimap()));

        Assertions.assertEquals(Arrays.asList("a", "bb"), multimap.get(1));
        Assertions.assertEquals(Arrays.asList("bb"), multimap.get(2));
    }

    // flatmappingKeyToMultimap

    @Test
    public void testFlatmappingKeyToMultimap() {
        List<Pair<List<String>, Integer>> data = Arrays.asList(Pair.of(Arrays.asList("a", "b"), 1));
        ListMultimap<String, Integer> result = data.stream().collect(Collectors.flatmappingKeyToMultimap(Pair::left, Pair::right));
        assertEquals(1, result.get("a").get(0));
    }

    @Test
    public void testFlatmappingKeyToMultimapCollection() {
        List<Employee> employees = Arrays.asList(new Employee("John", Arrays.asList("Java", "Python")),
                new Employee("Jane", Arrays.asList("Java", "JavaScript")));

        ListMultimap<String, String> result = employees.stream().collect(Collectors.flatmappingKeyToMultimap(Employee::getSkills, Employee::getName));

        assertEquals(2, result.get("Java").size());
    }

    @Test
    public void testFlatmappingKeyToMultimapWithMapFactory() {
        ListMultimap<Integer, String> multimap = Stream.of("a", "bb")
                .collect(Collectors.flatmappingKeyToMultimap(s -> Arrays.asList(1, 2, 3).subList(0, s.length()), e -> e, Suppliers.ofListMultimap()));

        Assertions.assertEquals(Arrays.asList("a", "bb"), multimap.get(1));
        Assertions.assertEquals(Arrays.asList("bb"), multimap.get(2));
    }

    @Test
    public void testFlatMappingToList() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4)).collect(Collectors.flatMappingToList(list -> Stream.of(list)));

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    // flatmappingToList

    @Test
    public void testFlatmappingToList() {
        List<String> words = Arrays.asList("hello world", "foo bar");
        List<String> result = words.stream().collect(Collectors.flatmappingToList(s -> Arrays.asList(s.split(" "))));
        assertEquals(Arrays.asList("hello", "world", "foo", "bar"), result);
    }

    @Test
    public void testFlatMappingToListCollection() {
        List<List<String>> nested = Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d"));
        List<String> result = nested.stream().collect(Collectors.flatmappingToList(Function.identity()));
        assertEquals(Arrays.asList("a", "b", "c", "d"), result);
    }

    @Test
    public void testFlatmappingToListWithCollection() {
        List<Integer> result = Stream.of(Arrays.asList(1, 2), Arrays.asList(3, 4)).collect(Collectors.flatmappingToList(e -> e));

        Assertions.assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    // flatMappingValueToMultimap

    @Test
    public void testFlatMappingValueToMultimap() {
        ListMultimap<String, Integer> multimap = Stream.of("a", "bb")
                .collect(Collectors.flatMappingValueToMultimap(e -> e, s -> Stream.of(1, 2, 3).limit(s.length())));

        Assertions.assertEquals(Arrays.asList(1), multimap.get("a"));
        Assertions.assertEquals(Arrays.asList(1, 2), multimap.get("bb"));
    }

    @Test
    public void testFlatMappingValueToMultimapNullStreamIsEmpty() {
        final ListMultimap<String, Integer> multimap = Stream.of("a")
                .collect(Collectors.flatMappingValueToMultimap(Fn.identity(), s -> (Stream<Integer>) null));

        assertTrue(multimap.isEmpty());
    }

    @Test
    public void testFlatMappingValueToMultimapWithMapFactory() {
        ListMultimap<String, Integer> multimap = Stream.of("a", "bb")
                .collect(Collectors.flatMappingValueToMultimap(e -> e, s -> Stream.of(1, 2, 3).limit(s.length()), Suppliers.ofListMultimap()));

        Assertions.assertEquals(Arrays.asList(1), multimap.get("a"));
        Assertions.assertEquals(Arrays.asList(1, 2), multimap.get("bb"));
    }

    // flatmappingValueToMultimap

    @Test
    public void testFlatmappingValueToMultimap() {
        List<Pair<String, List<Integer>>> data = Arrays.asList(Pair.of("a", Arrays.asList(1, 2, 3)));
        ListMultimap<String, Integer> result = data.stream().collect(Collectors.flatmappingValueToMultimap(Pair::left, Pair::right));
        assertEquals(3, result.get("a").size());
    }

    @Test
    public void testFlatmappingValueToMultimapCollection() {
        List<Department> departments = Arrays.asList(new Department("Engineering", Arrays.asList("Java", "Python")),
                new Department("Sales", Arrays.asList("Communication", "Negotiation")));

        ListMultimap<String, String> result = departments.stream().collect(Collectors.flatmappingValueToMultimap(Department::getName, Department::getSkills));

        assertEquals(2, result.get("Engineering").size());
        assertEquals(2, result.get("Sales").size());
    }

    @Test
    public void testFlatmappingValueToMultimapWithMapFactory() {
        ListMultimap<String, Integer> multimap = Stream.of("a", "bb")
                .collect(Collectors.flatmappingValueToMultimap(e -> e, s -> Arrays.asList(1, 2, 3).subList(0, s.length()), Suppliers.ofListMultimap()));

        Assertions.assertEquals(Arrays.asList(1), multimap.get("a"));
        Assertions.assertEquals(Arrays.asList(1, 2), multimap.get("bb"));
    }

    // joining

    @Test
    public void testJoining() {
        String result = stringList.stream().collect(Collectors.joining());
        assertEquals("applebananacherrydateelderberry", result);
    }

    @Test
    public void test_joining() {
        assertEquals("abc", Stream.of("a", "b", "c").collect(Collectors.joining()));
        assertEquals("a,b,c", Stream.of("a", "b", "c").collect(Collectors.joining(",")));
        assertEquals("[a,b,c]", Stream.of("a", "b", "c").collect(Collectors.joining(",", "[", "]")));
    }

    @Test
    public void testJoining_Parallel_CombinerUsed() {
        // Triggers Joiner_Combiner (lambda$96) in parallel
        String result = Stream.of("a", "b", "c", "d", "e", "f").parallel().collect(Collectors.joining(", "));
        assertNotNull(result);
        assertTrue(result.contains("a"));
        assertTrue(result.contains("b"));
    }

    @Test
    public void testJoining_WithDelimiter() {
        String result = stringList.stream().collect(Collectors.joining(", "));
        assertEquals("apple, banana, cherry, date, elderberry", result);
    }

    @Test
    public void testJoining_WithDelimiterPrefixSuffix() {
        String result = stringList.stream().collect(Collectors.joining(", ", "[", "]"));
        assertEquals("[apple, banana, cherry, date, elderberry]", result);
    }

    @Test
    public void testJoiningSnapshotsMutableFormattingArguments() {
        final StringBuilder delimiter = new StringBuilder(",");
        final StringBuilder prefix = new StringBuilder("[");
        final StringBuilder suffix = new StringBuilder("]");
        final Collector<Object, ?, String> collector = Collectors.joining(delimiter, prefix, suffix);

        delimiter.append(';');
        prefix.append('<');
        suffix.append('>');

        assertEquals("[a,b]", Stream.of("a", "b").collect(collector));
        assertThrows(IllegalArgumentException.class, () -> Collectors.joining((CharSequence) null));
        assertThrows(IllegalArgumentException.class, () -> Collectors.joining(",", null, "]"));
    }

    @Test
    public void testJoiningWithDelimiter() {
        String result = Stream.of("a", "b", "c").collect(Collectors.joining(", "));

        Assertions.assertEquals("a, b, c", result);
    }

    @Test
    public void testJoiningWithDelimiterPrefixSuffix() {
        String result = Stream.of("a", "b", "c").collect(Collectors.joining(", ", "[", "]"));

        Assertions.assertEquals("[a, b, c]", result);
    }

    @Test
    public void testMapping() {
        List<Integer> result = stringList.stream().collect(Collectors.mapping(String::length, Collectors.toList()));
        assertEquals(Arrays.asList(5, 6, 6, 4, 10), result);
    }

    @Test
    public void testMappingToList() {
        List<Integer> result = stringList.stream().collect(Collectors.mappingToList(String::length));
        assertEquals(Arrays.asList(5, 6, 6, 4, 10), result);
    }

    @Test
    public void testMerge_BiMap_NewKey() {
        // Triggers Collectors.merge(BiMap, key, value, function) with duplicate keys
        // "a" and "bb" have different lengths, "bb" and "cc" have same length key=2
        BiMap<Integer, String> result = Stream.of("a", "bb", "cc", "ddd").collect(Collectors.toBiMap(String::length, s -> s, (v1, v2) -> v1));
        assertNotNull(result);
        // Keys: 1->"a", 2->"bb" (first wins), 3->"ddd"
        assertEquals(3, result.size());
        assertEquals("a", result.get(1));
        assertEquals("bb", result.get(2)); // first one wins
    }

    @Test
    public void testMerge_Map_ExistingKey() {
        // Triggers Collectors.merge(Map, key, value, function) with existing key
        Map<Integer, String> result = Stream.of("apple", "avocado", "banana", "blueberry", "cherry")
                .parallel()
                .collect(Collectors.toMap(s -> s.charAt(0) - 'a', s -> s, (v1, v2) -> v1 + "," + v2));
        assertNotNull(result);
        // 'a' keys: "apple" and "avocado" should be merged
        assertTrue(result.get(0).contains("apple") || result.get(0).contains("avocado"));
    }

    @Test
    public void testFlatMappingToMultimap_Parallel_CombinerUsed() {
        // Triggers lambda$294 (flatMappingToMultimap accumulator/combiner) in parallel
        ListMultimap<String, Integer> result = Stream.of("a", "bb", "ccc", "dd", "e", "ff")
                .parallel()
                .collect(Collectors.flatMappingValueToMultimap(s -> s, s -> Stream.of(1, 2).limit(s.length())));
        assertNotNull(result);
    }

    @Test
    public void testTeeing() {
        Pair<Long, Integer> result = stringList.stream().collect(Collectors.teeing(Collectors.counting(), Collectors.summingInt(String::length), Pair::of));
        assertEquals(5L, result.left());
        assertEquals(31, result.right());
    }

}
