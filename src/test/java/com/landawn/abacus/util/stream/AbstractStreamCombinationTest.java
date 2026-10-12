package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.Fn;
import com.landawn.abacus.util.Joiner;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.u.Optional;

/** AbstractStream combination contracts. */
public class AbstractStreamCombinationTest extends AbstractStreamTestSupport {

    // append

    @Test
    public void testAppend() {
        List<Integer> input1 = Arrays.asList(1, 2, 3);
        List<Integer> input2 = Arrays.asList(4, 5);
        Stream<Integer> stream1 = createStream(input1);
        Stream<Integer> stream2 = createStream(input2);

        List<Integer> result = stream1.append(stream2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void test_append_collection() {
        List<Integer> result = Stream.of(1, 2).append(List.of(3, 4)).toList();
        assertEquals(List.of(1, 2, 3, 4), result);
    }

    @Test
    public void test_append_optional() {
        Stream<Integer> s1 = Stream.of(1, 2);
        Optional<Integer> op = Optional.of(3);
        List<Integer> result = s1.append(op).toList();
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    public void test_append_stream() {
        List<Integer> result = Stream.of(1, 2).append(Stream.of(3, 4)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testAppendCollection() {
        stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.append(Arrays.asList(4, 5));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.toList());
    }

    @Test
    public void testAppendOptional() {
        stream = createStream(1, 2, 3);
        Stream<Integer> result = stream.append(Optional.of(4));
        assertEquals(Arrays.asList(1, 2, 3, 4), result.toList());

        stream = createStream(1, 2, 3);
        result = stream.append(Optional.empty());
        assertEquals(Arrays.asList(1, 2, 3), result.toList());
    }

    @Test
    public void testAppendStream() {
        stream = createStream(1, 2, 3);
        Stream<Integer> other = createStream(4, 5);
        Stream<Integer> result = stream.append(other);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.toList());
    }

    @Test
    public void test_cartesianProduct() {
        List<List<Integer>> result = Stream.of(1, 2).cartesianProduct(List.of(List.of(3, 4))).toList();
        assertEquals(List.of(List.of(1, 3), List.of(1, 4), List.of(2, 3), List.of(2, 4)), result);
    }

    @Test
    public void testCartesianProduct() {
        stream = createStream(1, 2);
        Stream<List<Integer>> result = stream.cartesianProduct(Arrays.asList(Arrays.asList(3, 4), Arrays.asList(5, 6)));
        List<List<Integer>> lists = result.toList();
        assertEquals(8, lists.size());
    }

    // combinations

    @Test
    public void test_combinations() {
        List<List<Integer>> result = Stream.of(1, 2, 3).combinations(2).toList();
        assertEquals(List.of(List.of(1, 2), List.of(1, 3), List.of(2, 3)), result);
    }

    @Test
    public void testCombinations() {
        stream = createStream(1, 2, 3);
        Stream<List<Integer>> result = stream.combinations();
        List<List<Integer>> lists = result.toList();
        assertEquals(8, lists.size());
    }

    @Test
    public void testCombinations_withLength() {
        stream = createStream(1, 2, 3);
        List<List<Integer>> result = stream.combinations(2).toList();
        assertEquals(3, result.size());
    }

    @Test
    public void testCombinations_withLengthAndRepeat() {
        stream = createStream(1, 2);
        List<List<Integer>> result = stream.combinations(2, true).toList();
        assertTrue(result.size() > 0);
    }

    @Test
    public void testCombinationsWithLength() {
        stream = createStream(1, 2, 3, 4);
        Stream<List<Integer>> result = stream.combinations(2);
        List<List<Integer>> lists = result.toList();
        assertEquals(6, lists.size());
        assertTrue(lists.contains(Arrays.asList(1, 2)));
        assertTrue(lists.contains(Arrays.asList(3, 4)));
    }

    @Test
    public void testCombinationsWithRepeat() {
        stream = createStream(1, 2);
        Stream<List<Integer>> result = stream.combinations(2, true);
        List<List<Integer>> lists = result.toList();
        assertEquals(4, lists.size());
    }

    @Test
    public void testCombinationsWithRepeatRejectsNegativeLengthEagerly() {
        stream = createStream(1, 2);

        assertThrows(IllegalArgumentException.class, () -> stream.combinations(-1, true));
    }

    // crossJoin

    @Test
    public void test_crossJoin() {
        List<Pair<Integer, String>> result = Stream.of(1, 2).crossJoin(List.of("a", "b")).toList();
        assertEquals(List.of(Pair.of(1, "a"), Pair.of(1, "b"), Pair.of(2, "a"), Pair.of(2, "b")), result);
    }

    @Test
    public void testCrossJoin() {
        stream = createStream(1, 2);
        Stream<Pair<Integer, String>> result = stream.crossJoin(Arrays.asList("a", "b"));
        List<Pair<Integer, String>> list = result.toList();
        assertEquals(4, list.size());
        assertTrue(list.contains(Pair.of(1, "a")));
        assertTrue(list.contains(Pair.of(1, "b")));
        assertTrue(list.contains(Pair.of(2, "a")));
        assertTrue(list.contains(Pair.of(2, "b")));
    }



    /**
     * {@code crossJoin(Collection)} silently returned an empty stream for a null right-hand side, while its
     * own javadoc says "must not be null" and every sibling join family - and {@code crossJoin(Stream, func)}
     * - throws {@code IllegalArgumentException}.
     */
    @Test
    public void testCrossJoin_nullCollectionIsRejectedLikeEveryOtherJoin() {
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2).crossJoin((Collection<Integer>) null).toList());
        assertThrows(IllegalArgumentException.class, () -> Stream.of(1, 2).crossJoin((Collection<Integer>) null, (a, b) -> a).toList());

        // the ordinary path is unaffected
        assertEquals(2, Stream.of(1, 2).crossJoin(Arrays.asList("a")).count());
    }

    @Test
    public void testCrossJoinWithFunction() {
        stream = createStream(1, 2);
        Stream<String> result = stream.crossJoin(Arrays.asList("a", "b"), (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "1b", "2a", "2b"), result.toList());
    }

    @Test
    public void testCrossJoinWithStream() {
        stream = createStream(1, 2);
        Stream<String> other = createStream("a", "b");
        Stream<String> result = stream.crossJoin(other, (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "1b", "2a", "2b"), result.toList());
    }

    // fullJoin

    @Test
    public void test_fullJoin() {
        List<Pair<Integer, String>> result = Stream.of(Pair.of(1, "A"), Pair.of(2, "B"))
                .fullJoin(List.of(Pair.of(1, "X"), Pair.of(3, "Y")), p -> p.left(), p -> p.left(), (p1, p2) -> {
                    if (p1 != null && p2 != null) {
                        return Pair.of(p1.left(), p1.right() + p2.right());
                    }
                    if (p1 != null) {
                        return Pair.of(p1.left(), p1.right());
                    }
                    return Pair.of(p2.left(), p2.right());
                })
                .sorted(Comparator.comparing(p -> p.left()))
                .toList();

        assertEquals(List.of(Pair.of(1, "AX"), Pair.of(2, "B"), Pair.of(3, "Y")), result);
    }

    @Test
    public void testFullJoin() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, String>> result = stream.fullJoin(Arrays.asList("2b", "3c", "4d"), Fn.identity(), s -> Integer.parseInt(s.substring(0, 1)));
        List<Pair<Integer, String>> list = result.toList();
        assertEquals(4, list.size());
        assertTrue(list.contains(Pair.of(1, null)));
        assertTrue(list.contains(Pair.of(null, "4d")));
    }

    @Test
    public void testfullJoin_2() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(2, "Order2"), new TestOrder(3, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.fullJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Pair::of).toList();

        assertEquals(3, result.size());
        assertEquals(Pair.of(new TestPerson(1, "John"), null), result.get(0));
        assertEquals(Pair.of(new TestPerson(2, "Jane"), new TestOrder(2, "Order2")), result.get(1));
        assertEquals(Pair.of(null, new TestOrder(3, "Order3")), result.get(2));
    }

    @Test
    public void testFullJoinSameType() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, Integer>> result = stream.fullJoin(Arrays.asList(2, 3, 4), Fn.identity());
        List<Pair<Integer, Integer>> list = result.toList();
        assertEquals(4, list.size());
    }

    @Test
    public void testFullJoinWithPredicate() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, Integer>> result = stream.fullJoin(Arrays.asList(2, 3, 4), (a, b) -> a.equals(b));
        List<Pair<Integer, Integer>> list = result.toList();
        assertTrue(list.contains(Pair.of(1, null)));
        assertTrue(list.contains(Pair.of(null, 4)));
    }

    // innerJoin

    @Test
    public void test_innerJoin() {
        List<Pair<Integer, String>> result = Stream.of(Pair.of(1, "A"), Pair.of(2, "B"))
                .innerJoin(List.of(Pair.of(1, "X"), Pair.of(3, "Y")), p -> p.left(), p -> p.left(), (p1, p2) -> Pair.of(p1.left(), p2.right()))
                .toList();
        assertEquals(List.of(Pair.of(1, "X")), result);
    }

    @Test
    public void testInnerJoin() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, String>> result = stream.innerJoin(Arrays.asList("1a", "2b", "3c"), Fn.identity(), s -> Integer.parseInt(s.substring(0, 1)));
        assertEquals(3, result.toList().size());
    }

    @Test
    public void testInnerJoin_2() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"), new TestPerson(3, "Bob"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(1, "Order1"), new TestOrder(2, "Order2"), new TestOrder(1, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.innerJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Pair::of).toList();

        assertEquals(3, result.size());
    }

    @Test
    public void testInnerJoinSameType() {
        stream = createStream(1, 2, 3, 4);
        Stream<Pair<Integer, Integer>> result = stream.innerJoin(Arrays.asList(2, 3, 4, 5), i -> i % 2);
        List<Pair<Integer, Integer>> list = result.toList();
        assertTrue(list.size() > 0);
    }

    @Test
    public void testInnerJoinWithFunction() {
        stream = createStream(1, 2, 3);
        Stream<String> result = stream.innerJoin(Arrays.asList("1a", "2b", "3c"), Fn.identity(), s -> Integer.parseInt(s.substring(0, 1)),
                (i, s) -> i + "-" + s);
        assertTrue(result.toList().contains("1-1a"));
    }

    @Test
    public void testInnerJoinWithPredicate() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, String>> result = stream.innerJoin(Arrays.asList("a", "b", "c"), (i, s) -> true);
        assertEquals(9, result.toList().size());
    }

    @Test
    public void test_join() {
        String result = Stream.of("a", "b", "c").join(", ", "[", "]");
        assertEquals("[a, b, c]", result);
    }

    @Test
    public void testJoin() {
        stringStream = createStream("a", "b", "c");
        String result = stringStream.join(",", "[", "]");
        assertEquals("[a,b,c]", result);
    }

    // joinByRange

    @Test
    public void test_joinByRange() {
        Stream<Integer> streamA = Stream.of(1, 2, 5, 6);
        Stream<Integer> streamB = Stream.of(1, 2, 3, 4);
        List<Pair<Integer, List<Integer>>> result = streamA.joinByRange(streamB.iterator(), (a, b) -> b <= a).toList();

        assertEquals(4, result.size());
        assertEquals(Pair.of(1, List.of(1)), result.get(0));
        assertEquals(Pair.of(2, List.of(2)), result.get(1));
        assertEquals(Pair.of(5, List.of(3, 4)), result.get(2));
        assertEquals(Pair.of(6, List.of()), result.get(3));
    }

    @Test
    public void testJoinByRange() {
        stream = createStream(1, 5, 10);
        Iterator<Integer> iter = Arrays.asList(2, 3, 6, 7, 8, 11).iterator();
        Stream<Pair<Integer, List<Integer>>> result = stream.joinByRange(iter, (a, b) -> b > a && b < a + 5);
        List<Pair<Integer, List<Integer>>> list = result.toList();
        assertEquals(3, list.size());
        assertEquals(Arrays.asList(2, 3), list.get(0).getRight());
        assertEquals(Arrays.asList(6, 7, 8), list.get(1).getRight());
    }

    @Test
    public void testJoinByRange_2() {
        List<Integer> left = Arrays.asList(1, 5, 10, 15);
        List<Integer> right = Arrays.asList(2, 3, 6, 7, 11, 12);
        Stream<Integer> stream = createStream(left);

        List<Pair<Integer, List<Integer>>> result = stream.joinByRange(right.iterator(), (l, r) -> r > l && r < l + 5).toList();

        assertEquals(4, result.size());
        assertEquals(Arrays.asList(2, 3), result.get(0).right());
        assertEquals(Arrays.asList(6, 7), result.get(1).right());
        assertEquals(Arrays.asList(11, 12), result.get(2).right());
        assertEquals(Collections.emptyList(), result.get(3).right());
    }

    @Test
    public void testJoinByRange_Stream_Parallel() {
        Stream<Integer> streamA = Stream.of(1, 2, 3).parallel();
        Stream<Integer> streamB = Stream.of(4, 5, 6).parallel();
        List<Pair<Integer, List<Integer>>> result = streamA.joinByRange(streamB, (a, b) -> b > a).toList();
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    @Test
    public void test_joinByRange_withCollector() {
        Stream<String> streamA = Stream.of("a", "b", "c");
        Stream<String> streamB = Stream.of("a1", "a2", "b1", "c1", "c2");

        List<Pair<String, Long>> result = streamA.joinByRange(streamB.iterator(), (a, b) -> b.startsWith(a), Collectors.counting()).toList();

        assertEquals(3, result.size());
        assertEquals(Pair.of("a", 2L), result.get(0));
        assertEquals(Pair.of("b", 1L), result.get(1));
        assertEquals(Pair.of("c", 2L), result.get(2));
    }

    @Test
    public void testJoinByRange_WithCollector_Parallel() {
        Stream<Integer> leftStream = Stream.of(1, 5, 10);
        Iterator<Integer> rightIter = Arrays.asList(1, 2, 3, 5, 6, 7, 10, 11, 12).iterator();
        List<Pair<Integer, Long>> result = leftStream
                .joinByRange(rightIter, (a, b) -> b >= a && b < a + 3, com.landawn.abacus.util.stream.Collectors.counting())
                .toList();
        assertEquals(3, result.size());
        assertEquals(Long.valueOf(3), result.get(0).right());
    }

    @Test
    public void testJoinByRangeWithCollector() {
        stream = createStream(1, 5, 10);
        Iterator<Integer> iter = Arrays.asList(2, 3, 6, 7, 8, 11).iterator();
        Stream<Pair<Integer, Long>> result = stream.joinByRange(iter, (a, b) -> b > a && b < a + 5, Collectors.counting());
        Map<Integer, Long> map = result.toMap(Pair::getLeft, Pair::getRight);
        assertEquals(2L, (long) map.get(1));
        assertEquals(3L, (long) map.get(5));
    }

    @Test
    public void testJoinByRangeWithMapperForUnjoined() {
        stream = createStream(1, 5);
        Iterator<Integer> iter = Arrays.asList(2, 6, 10, 11).iterator();
        Stream<String> result = stream.joinByRange(iter, (a, b) -> b > a && b < a + 4, Collectors.counting(), (key, count) -> key + ":" + count,
                unjoined -> Stream.of(unjoined).map(i -> "unjoined:" + i));
        List<String> list = result.toList();
        assertTrue(list.contains("1:1"));
        assertTrue(list.contains("5:1"));
        assertTrue(list.contains("unjoined:10"));
        assertTrue(list.contains("unjoined:11"));
    }

    @Test
    public void testJoinByRangeWithStream() {
        List<Integer> left = Arrays.asList(1, 5, 10);
        List<Integer> right = Arrays.asList(2, 3, 6, 7, 11);
        Stream<Integer> leftStream = createStream(left);
        Stream<Integer> rightStream = createStream(right);

        List<Pair<Integer, List<Integer>>> result = leftStream.joinByRange(rightStream, (l, r) -> r > l && r < l + 5).toList();

        assertEquals(3, result.size());
    }

    @Test
    public void testJoinByRangeWithUnjoined() {
        List<Integer> left = Arrays.asList(1, 5);
        List<Integer> right = Arrays.asList(2, 3, 10, 11);
        Stream<Integer> stream = createStream(left);

        List<Integer> result = stream
                .joinByRange(right.iterator(), (l, r) -> r > l && r < l + 2, Collectors.toList(), (a, b) -> a, iter -> Stream.of(iter).map(x -> -x))
                .toList();

        assertTrue(result.contains(1));
        assertTrue(result.contains(5));
        assertTrue(result.contains(-10));
        assertTrue(result.contains(-11));
    }

    @Test
    public void testJoinByRangeWithUnjoined_Parallel() {
        Stream<Integer> left = Stream.of(1, 2, 3).parallel();
        List<Integer> rightList = Arrays.asList(4, 5, 6);
        List<Integer> result = left.joinByRange(rightList.iterator(), (l, r) -> false, Collectors.toList(), (a, b) -> a, iter -> Stream.of(iter).map(x -> -x))
                .toList();
        assertEquals(Arrays.asList(1, 2, 3, -4, -5, -6), result);
    }

    @Test
    public void test_joinTo() {
        com.landawn.abacus.util.Joiner joiner = com.landawn.abacus.util.Joiner.with(", ");
        Stream.of(1, 2, 3).joinTo(joiner);
        assertEquals("1, 2, 3", joiner.toString());
    }

    @Test
    public void testJoinTo() {
        stringStream = createStream("a", "b", "c");
        Joiner joiner = Joiner.with(",");
        Joiner result = stringStream.joinTo(joiner);
        assertEquals("a,b,c", result.toString());
    }

    // leftJoin

    @Test
    public void test_leftJoin() {
        List<Pair<Integer, String>> result = Stream.of(Pair.of(1, "A"), Pair.of(2, "B"))
                .leftJoin(List.of(Pair.of(1, "X"), Pair.of(3, "Y")), p -> p.left(), p -> p.left(),
                        (p1, p2) -> Pair.of(p1.left(), p2 == null ? null : p2.right()))
                .toList();
        assertEquals(List.of(Pair.of(1, "X"), Pair.of(2, null)), result);
    }

    @Test
    public void testLeftJoin() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, String>> result = stream.leftJoin(Arrays.asList("2b", "3c"), Fn.identity(), s -> Integer.parseInt(s.substring(0, 1)));
        List<Pair<Integer, String>> list = result.toList();
        assertEquals(3, list.size());
        assertTrue(list.contains(Pair.of(1, null)));
    }

    @Test
    public void testLeftJoin_2() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(2, "Order2"), new TestOrder(3, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.leftJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Pair::of).toList();

        assertEquals(2, result.size());
        assertEquals(Pair.of(new TestPerson(1, "John"), null), result.get(0));
        assertEquals(Pair.of(new TestPerson(2, "Jane"), new TestOrder(2, "Order2")), result.get(1));
    }

    @Test
    public void testLeftJoinSameType() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, Integer>> result = stream.leftJoin(Arrays.asList(2, 3, 4), Fn.identity());
        List<Pair<Integer, Integer>> list = result.toList();
        assertEquals(3, list.size());
    }

    @Test
    public void testLeftJoinWithPredicate() {
        stream = createStream(1, 2, 3);
        Stream<Pair<Integer, Integer>> result = stream.leftJoin(Arrays.asList(2, 3, 4), (a, b) -> a.equals(b));
        List<Pair<Integer, Integer>> list = result.toList();
        assertEquals(3, list.size());
        assertTrue(list.contains(Pair.of(1, null)));
    }

    // mergeWith

    @Test
    public void test_mergeWith() {
        Stream<Integer> s1 = Stream.of(1, 3, 5);
        Stream<Integer> s2 = Stream.of(2, 4, 6);
        List<Integer> result = s1
                .mergeWith(s2, (a, b) -> a < b ? com.landawn.abacus.util.MergeResult.TAKE_FIRST : com.landawn.abacus.util.MergeResult.TAKE_SECOND)
                .toList();
        assertEquals(List.of(1, 2, 3, 4, 5, 6), result);
    }

    @Test
    public void testMergeWith() {
        stream = createStream(1, 3, 5);
        Stream<Integer> result = stream.mergeWith(Arrays.asList(2, 4, 6), (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result.toList());
    }

    @Test
    public void testMergeWithStream() {
        stream = createStream(1, 3, 5);
        Stream<Integer> other = createStream(2, 4, 6);
        Stream<Integer> result = stream.mergeWith(other, (a, b) -> a < b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), result.toList());
    }

    @Test
    public void test_permutations() {
        long count = Stream.of(1, 2, 3).permutations().count();
        assertEquals(6, count);
    }

    @Test
    public void testPermutations() {
        stream = createStream(1, 2, 3);
        Stream<List<Integer>> result = stream.permutations();
        List<List<Integer>> lists = result.toList();
        assertEquals(6, lists.size());
    }

    // prepend

    @Test
    public void testPrepend() {
        List<Integer> input1 = Arrays.asList(3, 4, 5);
        List<Integer> input2 = Arrays.asList(1, 2);
        Stream<Integer> stream1 = createStream(input1);
        Stream<Integer> stream2 = createStream(input2);

        List<Integer> result = stream1.prepend(stream2).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result);
    }

    @Test
    public void test_prepend_collection() {
        List<Integer> result = Stream.of(3, 4).prepend(List.of(1, 2)).toList();
        assertEquals(List.of(1, 2, 3, 4), result);
    }

    @Test
    public void test_prepend_optional() {
        Stream<Integer> s1 = Stream.of(2, 3);
        Optional<Integer> op = Optional.of(1);
        List<Integer> result = s1.prepend(op).toList();
        assertEquals(List.of(1, 2, 3), result);
    }

    @Test
    public void test_prepend_stream() {
        List<Integer> result = Stream.of(3, 4).prepend(Stream.of(1, 2)).toList();
        assertEquals(Arrays.asList(1, 2, 3, 4), result);
    }

    @Test
    public void testPrependCollection() {
        stream = createStream(3, 4, 5);
        Stream<Integer> result = stream.prepend(Arrays.asList(1, 2));
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.toList());
    }

    @Test
    public void testPrependOptional() {
        stream = createStream(2, 3, 4);
        Stream<Integer> result = stream.prepend(Optional.of(1));
        assertEquals(Arrays.asList(1, 2, 3, 4), result.toList());

        stream = createStream(1, 2, 3);
        result = stream.prepend(Optional.empty());
        assertEquals(Arrays.asList(1, 2, 3), result.toList());
    }

    @Test
    public void testPrependStream() {
        stream = createStream(3, 4, 5);
        Stream<Integer> other = createStream(1, 2);
        Stream<Integer> result = stream.prepend(other);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5), result.toList());
    }

    // rightJoin

    @Test
    public void test_rightJoin() {
        List<Pair<Integer, String>> result = Stream.of(Pair.of(1, "A"))
                .rightJoin(List.of(Pair.of(1, "X"), Pair.of(2, "Y")), p -> p.left(), p -> p.left(),
                        (p1, p2) -> Pair.of(p2.left(), p1 == null ? null : p1.right()))
                .toList();
        assertEquals(List.of(Pair.of(1, "A"), Pair.of(2, null)), result);
    }

    @Test
    public void testRightJoin() {
        stream = createStream(1, 2);
        Stream<Pair<Integer, String>> result = stream.rightJoin(Arrays.asList("1a", "2b", "3c"), Fn.identity(), s -> Integer.parseInt(s.substring(0, 1)));
        List<Pair<Integer, String>> list = result.toList();
        assertEquals(3, list.size());
        assertTrue(list.contains(Pair.of(null, "3c")));
    }

    @Test
    public void testRightJoin_2() {
        List<TestPerson> persons = Arrays.asList(new TestPerson(1, "John"), new TestPerson(2, "Jane"));
        List<TestOrder> orders = Arrays.asList(new TestOrder(2, "Order2"), new TestOrder(3, "Order3"));

        Stream<TestPerson> stream = createStream(persons);
        List<Pair<TestPerson, TestOrder>> result = stream.rightJoin(Stream.of(orders), TestPerson::getId, TestOrder::getPersonId, Pair::of).toList();

        assertEquals(2, result.size());
        assertEquals(Pair.of(new TestPerson(2, "Jane"), new TestOrder(2, "Order2")), result.get(0));
        assertEquals(Pair.of(null, new TestOrder(3, "Order3")), result.get(1));
    }

    @Test
    public void testRightJoinSameType() {
        stream = createStream(1, 2);
        Stream<Pair<Integer, Integer>> result = stream.rightJoin(Arrays.asList(1, 2, 3), Fn.identity());
        List<Pair<Integer, Integer>> list = result.toList();
        assertEquals(3, list.size());
    }

    @Test
    public void testRightJoinWithPredicate() {
        stream = createStream(1, 2);
        Stream<Pair<Integer, Integer>> result = stream.rightJoin(Arrays.asList(1, 2, 3), (a, b) -> a.equals(b));
        List<Pair<Integer, Integer>> list = result.toList();
        assertEquals(3, list.size());
        assertTrue(list.contains(Pair.of(null, 3)));
    }

    // zipWith

    @Test
    public void testZipWith() {
        stream = createStream(1, 2, 3);
        Stream<String> result = stream.zipWith(Arrays.asList("a", "b", "c"), (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b", "3c"), result.toList());
    }

    @Test
    public void testZipWith3Streams() {
        List<String> stream1Data = Arrays.asList("A", "B", "C");
        List<Integer> stream2Data = Arrays.asList(1, 2, 3);
        List<Boolean> stream3Data = Arrays.asList(true, false, true);

        Stream<String> stream = createStream(stream1Data);

        List<String> result = stream.zipWith(stream2Data, stream3Data, (s, i, b) -> s + i + (b ? "!" : "")).toList();

        assertEquals(Arrays.asList("A1!", "B2", "C3!"), result);
    }

    @Test
    public void test_zipWith_collection() {
        Stream<String> s1 = Stream.of("a", "b", "c");
        List<Integer> c2 = List.of(1, 2, 3);
        List<String> result = s1.zipWith(c2, (s, i) -> s + i).toList();
        assertEquals(List.of("a1", "b2", "c3"), result);
    }

    @Test
    public void test_zipWith_stream() {
        Stream<String> s1 = Stream.of("a", "b", "c");
        Stream<Integer> s2 = Stream.of(1, 2, 3);
        List<String> result = s1.zipWith(s2, (s, i) -> s + i).toList();
        assertEquals(List.of("a1", "b2", "c3"), result);
    }

    @Test
    public void testZipWithDefaults() {
        List<String> stream1Data = Arrays.asList("A", "B", "C", "D");
        List<Integer> stream2Data = Arrays.asList(1, 2);

        Stream<String> stream = createStream(stream1Data);

        List<String> result = stream.zipWith(stream2Data, "X", 0, (s, i) -> s + i).toList();

        assertEquals(Arrays.asList("A1", "B2", "C0", "D0"), result);
    }

    @Test
    public void testZipWithDefaultValues() {
        stream = createStream(1, 2, 3);
        Stream<String> result = stream.zipWith(Arrays.asList("a", "b"), 0, "z", (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b", "3z"), result.toList());
    }

    @Test
    public void testZipWithStream() {
        stream = createStream(1, 2, 3);
        Stream<String> other = createStream("a", "b", "c");
        Stream<String> result = stream.zipWith(other, (i, s) -> i + s);
        assertEquals(Arrays.asList("1a", "2b", "3c"), result.toList());
    }

    @Test
    public void testZipWithThreeCollections() {
        stream = createStream(1, 2, 3);
        Stream<String> result = stream.zipWith(Arrays.asList("a", "b", "c"), Arrays.asList(10.0, 20.0, 30.0), (i, s, d) -> i + s + d);
        assertEquals(Arrays.asList("1a10.0", "2b20.0", "3c30.0"), result.toList());
    }

    @Test
    public void testZipWithThreeCollectionsWithDefaultValues() {
        stream = createStream(1, 2, 3, 4);
        List<String> result = stream.zipWith(Arrays.asList("a", "b"), Arrays.asList(10, 20, 30), 0, "z", 0, (i, s, d) -> i + s + d).toList();
        assertEquals(4, result.size());
        assertEquals("1a10", result.get(0));
        assertEquals("2b20", result.get(1));
        assertEquals("3z30", result.get(2));
        assertEquals("4z0", result.get(3));
    }

}
