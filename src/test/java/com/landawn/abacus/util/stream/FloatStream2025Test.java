package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;
import com.landawn.abacus.util.FloatSummaryStatistics;
import com.landawn.abacus.util.IndexedFloat;
import com.landawn.abacus.util.MergeResult;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Percentage;
import com.landawn.abacus.util.Suppliers;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;

@Tag("2025")
public class FloatStream2025Test extends TestBase {

    protected FloatStream createFloatStream(float... a) {
        return FloatStream.of(a);
    }

    protected FloatStream createFloatStream(float[] a, int fromIndex, int toIndex) {
        return FloatStream.of(a, fromIndex, toIndex);
    }

    protected FloatStream createFloatStream(Float[] a) {
        return FloatStream.of(a);
    }

    protected FloatStream createFloatStream(Float[] a, int fromIndex, int toIndex) {
        return FloatStream.of(a, fromIndex, toIndex);
    }

    protected FloatStream createFloatStream(Collection<Float> coll) {
        return FloatStream.of(coll.toArray(new Float[coll.size()]));
    }

    protected FloatStream createFloatStream(FloatIterator iter) {
        return iter == null ? FloatStream.empty() : FloatStream.of(iter.toArray());
    }

    protected FloatStream createFloatStream(FloatBuffer buff) {
        return FloatStream.of(buff);
    }

    @Nested
    @DisplayName("Factory Methods - Static")
    public class FactoryMethods {

        @Test
        @DisplayName("empty() should return empty stream")
        public void testEmpty() {
            FloatStream stream = FloatStream.empty();
            assertEquals(0, stream.count());
        }

        @Test
        @DisplayName("of() with varargs should create stream")
        public void testOfVarargs() {
            FloatStream stream = FloatStream.of(1.0f, 2.0f, 3.0f);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
        }

        @Test
        @DisplayName("of() with primitive array should create stream")
        public void testOfArray() {
            float[] array = { 1.5f, 2.5f, 3.5f };
            FloatStream stream = FloatStream.of(array);
            assertArrayEquals(array, stream.toArray());
        }

        @Test
        @DisplayName("of() with array range should create substream")
        public void testOfArrayRange() {
            float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
            FloatStream stream = FloatStream.of(array, 1, 4);
            assertArrayEquals(new float[] { 2.0f, 3.0f, 4.0f }, stream.toArray());
        }

        @Test
        @DisplayName("of() with Float array should create stream")
        public void testOfFloatArray() {
            Float[] array = { 1.0f, 2.0f, 3.0f };
            FloatStream stream = FloatStream.of(array);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
        }

        @Test
        @DisplayName("of() with Float array range should create substream")
        public void testOfFloatArrayRange() {
            Float[] array = { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f };
            FloatStream stream = FloatStream.of(array, 1, 4);
            assertArrayEquals(new float[] { 2.0f, 3.0f, 4.0f }, stream.toArray());
        }

        @Test
        @DisplayName("of() with collection should create stream")
        public void testOfCollection() {
            List<Float> list = Arrays.asList(1.0f, 2.0f, 3.0f);
            FloatStream stream = FloatStream.of(list);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
        }

        @Test
        @DisplayName("of() with iterator should create stream")
        public void testOfIterator() {
            FloatIterator iterator = FloatIterator.of(1.0f, 2.0f, 3.0f);
            FloatStream stream = FloatStream.of(iterator);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
        }

        @Test
        @DisplayName("of() with FloatBuffer should create stream")
        public void testOfFloatBuffer() {
            FloatBuffer buffer = FloatBuffer.allocate(3);
            buffer.put(1.0f).put(2.0f).put(3.0f).flip();
            FloatStream stream = FloatStream.of(buffer);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
        }

        @Test
        @DisplayName("ofNullable() with null should return empty stream")
        public void testOfNullableNull() {
            FloatStream stream = FloatStream.ofNullable(null);
            assertEquals(0, stream.count());
        }

        @Test
        @DisplayName("ofNullable() with value should return single element stream")
        public void testOfNullableValue() {
            FloatStream stream = FloatStream.ofNullable(5.0f);
            assertArrayEquals(new float[] { 5.0f }, stream.toArray());
        }

        @Test
        @DisplayName("defer() should create deferred stream")
        public void testDefer() {
            FloatStream stream = FloatStream.defer(() -> FloatStream.of(1.0f, 2.0f, 3.0f));
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
        }

        @Test
        @DisplayName("repeat() should create stream with repeated element")
        public void testRepeat() {
            FloatStream stream = FloatStream.repeat(2.5f, 3);
            assertArrayEquals(new float[] { 2.5f, 2.5f, 2.5f }, stream.toArray());
        }

        @Test
        @DisplayName("repeat() with zero count should return empty stream")
        public void testRepeatZero() {
            FloatStream stream = FloatStream.repeat(1.0f, 0);
            assertEquals(0, stream.count());
        }

        @Test
        @DisplayName("iterate() with hasNext and next should create stream")
        public void testIterateWithHasNextNext() {
            AtomicInteger counter = new AtomicInteger(0);
            FloatStream stream = FloatStream.iterate(() -> counter.get() < 3, () -> counter.getAndIncrement() + 1.0f);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
        }

        @Test
        @DisplayName("iterate() with init, hasNext and function should create stream")
        public void testIterateWithInitHasNextFunction() {
            FloatStream stream = FloatStream.iterate(1.0f, () -> true, x -> x + 1.0f).limit(3);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
        }

        @Test
        @DisplayName("iterate() with init, predicate and function should create stream")
        public void testIterateWithPredicate() {
            FloatStream stream = FloatStream.iterate(1.0f, x -> x < 5.0f, x -> x + 1.0f);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
        }

        @Test
        @DisplayName("iterate() with init and function should create infinite stream")
        public void testIterateInfinite() {
            FloatStream stream = FloatStream.iterate(1.0f, x -> x + 1.0f).limit(3);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
        }

        @Test
        @DisplayName("generate() should create stream with supplier")
        public void testGenerate() {
            FloatStream stream = FloatStream.generate(() -> 5.0f).limit(3);
            assertArrayEquals(new float[] { 5.0f, 5.0f, 5.0f }, stream.toArray());
        }

        @Test
        @DisplayName("random() should generate random stream")
        public void testRandom() {
            FloatStream stream = FloatStream.random().limit(5);
            float[] result = stream.toArray();
            assertEquals(5, result.length);
            for (float f : result) {
                assertTrue(f >= 0.0f && f < 1.0f);
            }
        }

        @Test
        @DisplayName("flatten() should flatten two-dimensional array")
        public void testFlatten2D() {
            float[][] array = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            FloatStream stream = FloatStream.flatten(array);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
        }

        @Test
        @DisplayName("flatten() with vertically flag should flatten correctly")
        public void testFlatten2DVertically() {
            float[][] array = { { 1.0f, 2.0f }, { 3.0f, 4.0f } };
            FloatStream stream = FloatStream.flatten(array, false);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());

            stream = FloatStream.flatten(array, true);
            assertArrayEquals(new float[] { 1.0f, 3.0f, 2.0f, 4.0f }, stream.toArray());
        }

        @Test
        @DisplayName("flatten() with alignment value should pad arrays")
        public void testFlatten2DWithAlignment() {
            float[][] array = { { 1.0f, 2.0f }, { 3.0f, 4.0f, 5.0f } };
            FloatStream stream = FloatStream.flatten(array, 0.0f, false);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 0.0f, 3.0f, 4.0f, 5.0f }, stream.toArray());

            stream = FloatStream.flatten(array, 0.0f, true);
            assertArrayEquals(new float[] { 1.0f, 3.0f, 2.0f, 4.0f, 0.0f, 5.0f }, stream.toArray());
        }

        @Test
        @DisplayName("flatten() should flatten three-dimensional array")
        public void testFlatten3D() {
            float[][][] array = { { { 1.0f, 2.0f }, { 3.0f, 4.0f } } };
            FloatStream stream = FloatStream.flatten(array);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
        }

        @Test
        @DisplayName("concat() with arrays should concatenate")
        public void testConcatArrays() {
            float[] a = { 1.0f, 2.0f };
            float[] b = { 3.0f, 4.0f };
            FloatStream stream = FloatStream.concat(a, b);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
        }

        @Test
        @DisplayName("concat() with iterators should concatenate")
        public void testConcatIterators() {
            FloatIterator a = FloatIterator.of(1.0f, 2.0f);
            FloatIterator b = FloatIterator.of(3.0f, 4.0f);
            FloatStream stream = FloatStream.concat(a, b);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
        }

        @Test
        @DisplayName("concat() with streams should concatenate")
        public void testConcatStreams() {
            FloatStream a = FloatStream.of(1.0f, 2.0f);
            FloatStream b = FloatStream.of(3.0f, 4.0f);
            FloatStream stream = FloatStream.concat(a, b);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
        }

        @Test
        @DisplayName("concat() with list of arrays should concatenate")
        public void testConcatListOfArrays() {
            List<float[]> list = Arrays.asList(new float[] { 1.0f, 2.0f }, new float[] { 3.0f, 4.0f });
            FloatStream stream = FloatStream.concat(list);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
        }

        @Test
        @DisplayName("concat() with collection of streams should concatenate")
        public void testConcatCollectionOfStreams() {
            List<FloatStream> list = Arrays.asList(FloatStream.of(1.0f, 2.0f), FloatStream.of(3.0f, 4.0f));
            FloatStream stream = FloatStream.concat(list);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
        }

        @Test
        @DisplayName("concatIterators() should concatenate iterators")
        public void testConcatIteratorsCollection() {
            List<FloatIterator> list = Arrays.asList(FloatIterator.of(1.0f, 2.0f), FloatIterator.of(3.0f, 4.0f));
            FloatStream stream = FloatStream.concatIterators(list);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
        }
    }

    @Nested
    @DisplayName("Zip Operations")
    public class ZipOperations {

        @Test
        @DisplayName("zip() two arrays with binary function")
        public void testZipTwoArrays() {
            float[] a = { 1.0f, 2.0f, 3.0f };
            float[] b = { 4.0f, 5.0f, 6.0f };
            FloatStream stream = FloatStream.zip(a, b, Float::sum);
            assertArrayEquals(new float[] { 5.0f, 7.0f, 9.0f }, stream.toArray());
        }

        @Test
        @DisplayName("zip() three arrays with ternary function")
        public void testZipThreeArrays() {
            float[] a = { 1.0f, 2.0f };
            float[] b = { 3.0f, 4.0f };
            float[] c = { 5.0f, 6.0f };
            FloatStream stream = FloatStream.zip(a, b, c, (x, y, z) -> x + y + z);
            assertArrayEquals(new float[] { 9.0f, 12.0f }, stream.toArray());
        }

        @Test
        @DisplayName("zip() two iterators with binary function")
        public void testZipTwoIterators() {
            FloatIterator a = FloatIterator.of(1.0f, 2.0f);
            FloatIterator b = FloatIterator.of(4.0f, 5.0f);
            FloatStream stream = FloatStream.zip(a, b, Float::sum);
            assertArrayEquals(new float[] { 5.0f, 7.0f }, stream.toArray());
        }

        @Test
        @DisplayName("zip() three iterators with ternary function")
        public void testZipThreeIterators() {
            FloatIterator a = FloatIterator.of(1.0f, 2.0f);
            FloatIterator b = FloatIterator.of(3.0f, 4.0f);
            FloatIterator c = FloatIterator.of(5.0f, 6.0f);
            FloatStream stream = FloatStream.zip(a, b, c, (x, y, z) -> x + y + z);
            assertArrayEquals(new float[] { 9.0f, 12.0f }, stream.toArray());
        }

        @Test
        @DisplayName("zip() two streams with binary function")
        public void testZipTwoStreams() {
            FloatStream a = FloatStream.of(1.0f, 2.0f);
            FloatStream b = FloatStream.of(4.0f, 5.0f);
            FloatStream stream = FloatStream.zip(a, b, Float::sum);
            assertArrayEquals(new float[] { 5.0f, 7.0f }, stream.toArray());
        }

        @Test
        @DisplayName("zip() three streams with ternary function")
        public void testZipThreeStreams() {
            FloatStream a = FloatStream.of(1.0f, 2.0f);
            FloatStream b = FloatStream.of(3.0f, 4.0f);
            FloatStream c = FloatStream.of(5.0f, 6.0f);
            FloatStream stream = FloatStream.zip(a, b, c, (x, y, z) -> x + y + z);
            assertArrayEquals(new float[] { 9.0f, 12.0f }, stream.toArray());
        }

        @Test
        @DisplayName("zip() collection of streams with N-function")
        public void testZipCollectionOfStreams() {
            List<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f), FloatStream.of(3.0f, 4.0f));
            FloatStream stream = FloatStream.zip(streams, floats -> floats[0] + floats[1]);
            assertArrayEquals(new float[] { 4.0f, 6.0f }, stream.toArray());
        }

        @Test
        @DisplayName("zip() with padding for two arrays")
        public void testZipTwoArraysWithPadding() {
            float[] a = { 1.0f, 2.0f };
            float[] b = { 4.0f, 5.0f, 6.0f };
            FloatStream stream = FloatStream.zip(a, b, 0.0f, 0.0f, Float::sum);
            assertArrayEquals(new float[] { 5.0f, 7.0f, 6.0f }, stream.toArray());
        }

        @Test
        @DisplayName("zip() with padding for three arrays")
        public void testZipThreeArraysWithPadding() {
            float[] a = { 1.0f };
            float[] b = { 4.0f, 5.0f };
            float[] c = { 7.0f, 8.0f, 9.0f };
            FloatStream stream = FloatStream.zip(a, b, c, 100.0f, 200.0f, 300.0f, (x, y, z) -> x + y + z);
            assertArrayEquals(new float[] { 12.0f, 113.0f, 309.0f }, stream.toArray());
        }

        @Test
        @DisplayName("zip() with padding for two iterators")
        public void testZipTwoIteratorsWithPadding() {
            FloatIterator a = FloatIterator.of(1.0f, 2.0f);
            FloatIterator b = FloatIterator.of(4.0f, 5.0f, 6.0f);
            FloatStream stream = FloatStream.zip(a, b, 0.0f, 0.0f, Float::sum);
            assertArrayEquals(new float[] { 5.0f, 7.0f, 6.0f }, stream.toArray());
        }

        @Test
        @DisplayName("zip() with padding for three iterators")
        public void testZipThreeIteratorsWithPadding() {
            FloatIterator a = FloatIterator.of(1.0f);
            FloatIterator b = FloatIterator.of(4.0f, 5.0f);
            FloatIterator c = FloatIterator.of(7.0f, 8.0f, 9.0f);
            FloatStream stream = FloatStream.zip(a, b, c, 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z);
            assertArrayEquals(new float[] { 12.0f, 13.0f, 9.0f }, stream.toArray());
        }

        @Test
        @DisplayName("zip() with padding for two streams")
        public void testZipTwoStreamsWithPadding() {
            FloatStream a = FloatStream.of(1.0f, 2.0f);
            FloatStream b = FloatStream.of(4.0f, 5.0f, 6.0f);
            FloatStream stream = FloatStream.zip(a, b, 0.0f, 0.0f, Float::sum);
            assertArrayEquals(new float[] { 5.0f, 7.0f, 6.0f }, stream.toArray());
        }

        @Test
        @DisplayName("zip() with padding for three streams")
        public void testZipThreeStreamsWithPadding() {
            FloatStream a = FloatStream.of(1.0f);
            FloatStream b = FloatStream.of(4.0f, 5.0f);
            FloatStream c = FloatStream.of(7.0f, 8.0f, 9.0f);
            FloatStream stream = FloatStream.zip(a, b, c, 0.0f, 0.0f, 0.0f, (x, y, z) -> x + y + z);
            assertArrayEquals(new float[] { 12.0f, 13.0f, 9.0f }, stream.toArray());
        }

        @Test
        @DisplayName("zip() with padding for collection of streams")
        public void testZipCollectionWithPadding() {
            List<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f), FloatStream.of(3.0f));
            float[] valuesForNone = { 0.0f, 100.0f };
            FloatStream stream = FloatStream.zip(streams, valuesForNone, floats -> floats[0] + floats[1]);
            assertArrayEquals(new float[] { 4.0f, 102.0f }, stream.toArray());
        }
    }

    @Nested
    @DisplayName("Merge Operations")
    public class MergeOperations {

        @Test
        @DisplayName("merge() two arrays")
        public void testMergeTwoArrays() {
            float[] a = { 1.0f, 3.0f, 5.0f };
            float[] b = { 2.0f, 4.0f, 6.0f };
            FloatStream stream = FloatStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
        }

        @Test
        @DisplayName("merge() three arrays")
        public void testMergeThreeArrays() {
            float[] a = { 1.0f, 4.0f };
            float[] b = { 2.0f, 5.0f };
            float[] c = { 3.0f, 6.0f };
            FloatStream stream = FloatStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
        }

        @Test
        @DisplayName("merge() two iterators")
        public void testMergeTwoIterators() {
            FloatIterator a = FloatIterator.of(1.0f, 3.0f, 5.0f);
            FloatIterator b = FloatIterator.of(2.0f, 4.0f, 6.0f);
            FloatStream stream = FloatStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
        }

        @Test
        @DisplayName("merge() three iterators")
        public void testMergeThreeIterators() {
            FloatIterator a = FloatIterator.of(1.0f, 4.0f);
            FloatIterator b = FloatIterator.of(2.0f, 5.0f);
            FloatIterator c = FloatIterator.of(3.0f, 6.0f);
            FloatStream stream = FloatStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
        }

        @Test
        @DisplayName("merge() two streams")
        public void testMergeTwoStreams() {
            FloatStream a = FloatStream.of(1.0f, 3.0f, 5.0f);
            FloatStream b = FloatStream.of(2.0f, 4.0f, 6.0f);
            FloatStream stream = FloatStream.merge(a, b, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
        }

        @Test
        @DisplayName("merge() three streams")
        public void testMergeThreeStreams() {
            FloatStream a = FloatStream.of(1.0f, 4.0f);
            FloatStream b = FloatStream.of(2.0f, 5.0f);
            FloatStream c = FloatStream.of(3.0f, 6.0f);
            FloatStream stream = FloatStream.merge(a, b, c, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f }, stream.toArray());
        }

        @Test
        @DisplayName("merge() collection of streams")
        public void testMergeCollectionOfStreams() {
            List<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 3.0f), FloatStream.of(2.0f, 4.0f));
            FloatStream stream = FloatStream.merge(streams, (x, y) -> x <= y ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.toArray());
        }
    }

    @Nested
    @DisplayName("Intermediate Operations - Filtering")
    public class FilteringOperations {

        @Test
        @DisplayName("filter() should filter elements")
        public void testFilter() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            assertArrayEquals(new float[] { 3.0f, 4.0f }, stream.filter(x -> x > 2.0f).toArray());
        }

        @Test
        @DisplayName("filter() with action on dropped items")
        public void testFilterWithAction() {
            List<Float> dropped = new ArrayList<>();
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            assertArrayEquals(new float[] { 3.0f, 4.0f }, stream.filter(x -> x > 2.0f, dropped::add).toArray());
            assertEquals(Arrays.asList(1.0f, 2.0f), dropped);
        }

        @Test
        @DisplayName("takeWhile() should take elements while predicate is true")
        public void testTakeWhile() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 2.0f, 1.0f);
            assertArrayEquals(new float[] { 1.0f, 2.0f }, stream.takeWhile(x -> x < 3.0f).toArray());
        }

        @Test
        @DisplayName("dropWhile() should drop elements while predicate is true")
        public void testDropWhile() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 2.0f);
            assertArrayEquals(new float[] { 3.0f, 4.0f, 2.0f }, stream.dropWhile(x -> x < 3.0f).toArray());
        }

        @Test
        @DisplayName("dropWhile() with action on dropped items")
        public void testDropWhileWithAction() {
            List<Float> dropped = new ArrayList<>();
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            assertArrayEquals(new float[] { 3.0f, 4.0f }, stream.dropWhile(x -> x < 3.0f, dropped::add).toArray());
            assertEquals(Arrays.asList(1.0f, 2.0f), dropped);
        }

        @Test
        @DisplayName("skipUntil() should skip elements until predicate is true")
        public void testSkipUntil() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            assertArrayEquals(new float[] { 3.0f, 4.0f }, stream.skipUntil(x -> x >= 3.0f).toArray());
        }

        @Test
        @DisplayName("distinct() should remove duplicates")
        public void testDistinct() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 2.0f, 3.0f, 1.0f);
            float[] result = stream.distinct().toArray();
            Arrays.sort(result);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
        }

        @Test
        @DisplayName("limit() should limit stream size")
        public void testLimit() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.limit(3).toArray());
        }

        @Test
        @DisplayName("skip() should skip elements")
        public void testSkip() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            assertArrayEquals(new float[] { 3.0f, 4.0f, 5.0f }, stream.skip(2).toArray());
        }

        @Test
        @DisplayName("skip() with action on skipped items")
        public void testSkipWithAction() {
            List<Float> skipped = new ArrayList<>();
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            assertArrayEquals(new float[] { 3.0f, 4.0f }, stream.skip(2, skipped::add).toArray());
            assertEquals(Arrays.asList(1.0f, 2.0f), skipped);
        }

        @Test
        @DisplayName("step() should take every nth element")
        public void testStep() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f);
            assertArrayEquals(new float[] { 1.0f, 3.0f, 5.0f }, stream.step(2).toArray());
        }
    }

    @Nested
    @DisplayName("Intermediate Operations - Transformation")
    public class TransformationOperations {

        @Test
        @DisplayName("map() should transform elements")
        public void testMap() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertArrayEquals(new float[] { 2.0f, 4.0f, 6.0f }, stream.map(x -> x * 2).toArray());
        }

        @Test
        @DisplayName("mapToInt() should convert to IntStream")
        public void testMapToInt() {
            FloatStream stream = createFloatStream(1.5f, 2.5f, 3.5f);
            assertArrayEquals(new int[] { 1, 2, 3 }, stream.mapToInt(x -> (int) x).toArray());
        }

        @Test
        @DisplayName("mapToLong() should convert to LongStream")
        public void testMapToLong() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertArrayEquals(new long[] { 1L, 2L, 3L }, stream.mapToLong(x -> (long) x).toArray());
        }

        @Test
        @DisplayName("mapToDouble() should convert to DoubleStream")
        public void testMapToDouble() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.mapToDouble(x -> x).toArray(), 0.001);
        }

        @Test
        @DisplayName("mapToObj() should convert to object stream")
        public void testMapToObj() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertArrayEquals(new String[] { "1.0", "2.0", "3.0" }, stream.mapToObj(String::valueOf).toArray(String[]::new));
        }

        @Test
        @DisplayName("flatMap() should flatten streams")
        public void testFlatMap() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 2.0f, 4.0f }, stream.flatMap(x -> FloatStream.of(x, x * 2)).toArray());
        }

        @Test
        @DisplayName("flatmap() should flatten arrays")
        public void testFlatmapArray() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 2.0f, 4.0f }, stream.flatmap(x -> new float[] { x, x * 2 }).toArray());
        }

        @Test
        @DisplayName("flatMapToInt() should flatten to IntStream")
        public void testFlatMapToInt() {
            FloatStream stream = createFloatStream(1.5f, 2.5f);
            assertArrayEquals(new int[] { 1, 3, 2, 5 }, stream.flatMapToInt(f -> IntStream.of((int) f, (int) (f * 2))).toArray());
        }

        @Test
        @DisplayName("flatMapToLong() should flatten to LongStream")
        public void testFlatMapToLong() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            assertArrayEquals(new long[] { 1L, 2L, 2L, 4L }, stream.flatMapToLong(f -> LongStream.of((long) f, (long) (f * 2))).toArray());
        }

        @Test
        @DisplayName("flatMapToDouble() should flatten to DoubleStream")
        public void testFlatMapToDouble() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            assertArrayEquals(new double[] { 1.0, 2.0, 2.0, 4.0 }, stream.flatMapToDouble(f -> DoubleStream.of(f, f * 2)).toArray(), 0.001);
        }

        @Test
        @DisplayName("flatMapToObj() should flatten to object stream")
        public void testFlatMapToObj() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            assertArrayEquals(new String[] { "1.0", "2.0", "2.0", "4.0" },
                    stream.flatMapToObj(f -> Stream.of(String.valueOf(f), String.valueOf(f * 2))).toArray(String[]::new));
        }

        @Test
        @DisplayName("flatmapToObj() should flatten collection to object stream")
        public void testFlatmapToObj() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            assertArrayEquals(new String[] { "1.0", "2.0", "2.0", "4.0" },
                    stream.flatmapToObj(f -> Arrays.asList(String.valueOf(f), String.valueOf(f * 2))).toArray(String[]::new));
        }

        @Test
        @DisplayName("flattmapToObj() should flatten array to object stream")
        public void testFlattmapToObj() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            assertArrayEquals(new String[] { "1.0", "2.0", "2.0", "4.0" },
                    stream.flattmapToObj(f -> new String[] { String.valueOf(f), String.valueOf(f * 2) }).toArray(String[]::new));
        }

        @Test
        @DisplayName("mapPartial() should filter and map partial results")
        public void testMapPartial() {
            FloatStream stream = createFloatStream(1.0f, -2.0f, 3.0f, -4.0f);
            assertArrayEquals(new float[] { 2.0f, 6.0f }, stream.mapPartial(x -> x > 0 ? OptionalFloat.of(x * 2) : OptionalFloat.empty()).toArray());
        }

        @Test
        @DisplayName("asDoubleStream() should convert to DoubleStream")
        public void testAsDoubleStream() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertArrayEquals(new double[] { 1.0, 2.0, 3.0 }, stream.asDoubleStream().toArray(), 0.001);
        }

        @Test
        @DisplayName("boxed() should convert to Stream<Float>")
        public void testBoxed() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertArrayEquals(new Float[] { 1.0f, 2.0f, 3.0f }, stream.boxed().toArray(Float[]::new));
        }
    }

    @Nested
    @DisplayName("Intermediate Operations - Advanced")
    public class AdvancedOperations {

        @Test
        @DisplayName("rangeMap() should map ranges")
        public void testRangeMap() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 6.0f, 7.0f);
            float[] result = stream.rangeMap((a, b) -> Math.abs(b - a) <= 1.5f, (start, end) -> end - start).toArray();
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("rangeMapToObj() should map ranges to objects")
        public void testRangeMapToObj() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 5.0f, 6.0f);
            String[] result = stream.rangeMapToObj((a, b) -> Math.abs(b - a) <= 1.5f, (start, end) -> String.format("%.1f-%.1f", start, end))
                    .toArray(String[]::new);
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("collapse() should group consecutive elements")
        public void testCollapse() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 5.0f, 6.0f, 7.0f);
            Stream<FloatList> result = stream.collapse((a, b) -> Math.abs(b - a) <= 1.5f);
            List<FloatList> groups = result.toList();
            assertTrue(groups.size() >= 2);
        }

        @Test
        @DisplayName("collapse() with merge function")
        public void testCollapseWithMerge() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 5.0f, 6.0f, 7.0f);
            float[] result = stream.collapse((a, b) -> Math.abs(b - a) <= 1.5f, Float::sum).toArray();
            assertTrue(result.length >= 2);
        }

        @Test
        @DisplayName("collapse() with tri-predicate and merge function")
        public void testCollapseWithTriPredicate() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 5.0f, 6.0f, 7.0f);
            float[] result = stream.collapse((first, last, next) -> Math.abs(next - first) <= 3.0f, Float::sum).toArray();
            assertTrue(result.length > 0);
        }

        @Test
        @DisplayName("scan() should return cumulative results")
        public void testScan() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertArrayEquals(new float[] { 1.0f, 3.0f, 6.0f }, stream.scan(Float::sum).toArray());
        }

        @Test
        @DisplayName("scan() with init should include initial value")
        public void testScanWithInit() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertArrayEquals(new float[] { 11.0f, 13.0f, 16.0f }, stream.scan(10.0f, Float::sum).toArray());
        }

        @Test
        @DisplayName("scan() with init and initIncluded flag")
        public void testScanWithInitIncluded() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            float[] result = stream.scan(10.0f, true, Float::sum).toArray();
            assertEquals(4, result.length);
            assertEquals(10.0f, result[0], 0.001);
        }

        @Test
        @DisplayName("prepend() should add elements to beginning")
        public void testPrepend() {
            FloatStream stream = createFloatStream(3.0f, 4.0f);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.prepend(1.0f, 2.0f).toArray());
        }

        @Test
        @DisplayName("append() should add elements to end")
        public void testAppend() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.append(3.0f, 4.0f).toArray());
        }

        @Test
        @DisplayName("appendIfEmpty() should append only if stream is empty")
        public void testAppendIfEmpty() {
            FloatStream stream1 = createFloatStream(1.0f, 2.0f);
            assertArrayEquals(new float[] { 1.0f, 2.0f }, stream1.appendIfEmpty(99.0f).toArray());

            FloatStream stream2 = FloatStream.empty();
            assertArrayEquals(new float[] { 99.0f }, stream2.appendIfEmpty(99.0f).toArray());
        }

        @Test
        @DisplayName("indexed() should pair elements with indices")
        public void testIndexed() {
            FloatStream stream = createFloatStream(10.0f, 20.0f, 30.0f);
            List<IndexedFloat> result = stream.indexed().toList();
            assertEquals(3, result.size());
            assertEquals(0, result.get(0).longIndex());
            assertEquals(10.0f, result.get(0).value(), 0.001);
        }

        @Test
        @DisplayName("cycled() should repeat stream indefinitely")
        public void testCycled() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 1.0f, 2.0f, 1.0f, 2.0f }, stream.cycled().limit(6).toArray());
        }

        @Test
        @DisplayName("cycled() with rounds should repeat specific times")
        public void testCycledWithRounds() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 1.0f, 2.0f, 1.0f, 2.0f }, stream.cycled(3).toArray());
        }
    }

    @Nested
    @DisplayName("Intermediate Operations - Sorting")
    public class SortingOperations {

        @Test
        @DisplayName("sorted() should sort elements")
        public void testSorted() {
            FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, stream.sorted().toArray());
        }

        @Test
        @DisplayName("reverseSorted() should sort in reverse")
        public void testReverseSorted() {
            FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f);
            assertArrayEquals(new float[] { 4.0f, 3.0f, 2.0f, 1.0f }, stream.reverseSorted().toArray());
        }

        @Test
        @DisplayName("reversed() should reverse order")
        public void testReversed() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            assertArrayEquals(new float[] { 4.0f, 3.0f, 2.0f, 1.0f }, stream.reversed().toArray());
        }

        @Test
        @DisplayName("shuffled() should shuffle elements")
        public void testShuffled() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            float[] result = stream.shuffled().toArray();
            assertEquals(4, result.length);
            Arrays.sort(result);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f }, result);
        }

        @Test
        @DisplayName("rotated() should rotate elements")
        public void testRotated() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            assertArrayEquals(new float[] { 3.0f, 4.0f, 1.0f, 2.0f }, stream.rotated(2).toArray());
        }

        @Test
        @DisplayName("top() should return top n elements")
        public void testTop() {
            FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f, 5.0f);
            float[] result = stream.top(3).toArray();
            Arrays.sort(result);
            assertArrayEquals(new float[] { 3.0f, 4.0f, 5.0f }, result);
        }

        @Test
        @DisplayName("top() with comparator should return top n elements")
        public void testTopWithComparator() {
            FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f, 5.0f);
            float[] result = stream.top(3, (a, b) -> Float.compare(b, a)).toArray();
            Arrays.sort(result);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, result);
        }
    }

    @Nested
    @DisplayName("Intermediate Operations - Set Operations")
    public class SetOperations {

        @Test
        @DisplayName("intersection() should return common elements")
        public void testIntersection() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            Collection<Float> other = Arrays.asList(2.0f, 3.0f, 5.0f);
            float[] result = stream.intersection(other).toArray();
            Arrays.sort(result);
            assertArrayEquals(new float[] { 2.0f, 3.0f }, result);
        }

        @Test
        @DisplayName("difference() should return elements not in other")
        public void testDifference() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            Collection<Float> other = Arrays.asList(2.0f, 3.0f, 5.0f);
            float[] result = stream.difference(other).toArray();
            Arrays.sort(result);
            assertArrayEquals(new float[] { 1.0f, 4.0f }, result);
        }

        @Test
        @DisplayName("symmetricDifference() should return elements in either but not both")
        public void testSymmetricDifference() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            Collection<Float> other = Arrays.asList(2.0f, 3.0f, 4.0f, 5.0f);
            float[] result = stream.symmetricDifference(other).toArray();
            Arrays.sort(result);
            assertArrayEquals(new float[] { 1.0f, 4.0f, 5.0f }, result);
        }
    }

    @Nested
    @DisplayName("Intermediate Operations - Instance Zip and Merge")
    public class InstanceZipMerge {

        @Test
        @DisplayName("mergeWith() should merge streams")
        public void testMergeWith() {
            FloatStream stream1 = createFloatStream(1.0f, 3.0f, 5.0f);
            FloatStream stream2 = createFloatStream(2.0f, 4.0f, 6.0f);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f },
                    stream1.mergeWith(stream2, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND).toArray());
        }

        @Test
        @DisplayName("zipWith() two streams with binary operator")
        public void testZipWith() {
            FloatStream stream1 = createFloatStream(1.0f, 2.0f, 3.0f);
            FloatStream stream2 = createFloatStream(4.0f, 5.0f, 6.0f);
            assertArrayEquals(new float[] { 5.0f, 7.0f, 9.0f }, stream1.zipWith(stream2, Float::sum).toArray());
        }

        @Test
        @DisplayName("zipWith() three streams with ternary operator")
        public void testZipWithThreeStreams() {
            FloatStream stream1 = createFloatStream(1.0f, 2.0f);
            FloatStream stream2 = createFloatStream(3.0f, 4.0f);
            FloatStream stream3 = createFloatStream(5.0f, 6.0f);
            assertArrayEquals(new float[] { 9.0f, 12.0f }, stream1.zipWith(stream2, stream3, (a, b, c) -> a + b + c).toArray());
        }

        @Test
        @DisplayName("zipWith() with padding for two streams")
        public void testZipWithPadding() {
            FloatStream stream1 = createFloatStream(1.0f, 2.0f, 5.0f);
            FloatStream stream2 = createFloatStream(3.0f, 4.0f);
            assertArrayEquals(new float[] { 4.0f, 6.0f, 5.0f }, stream1.zipWith(stream2, 0.0f, 0.0f, Float::sum).toArray());
        }

        @Test
        @DisplayName("zipWith() with padding for three streams")
        public void testZipWithPaddingThreeStreams() {
            FloatStream stream1 = createFloatStream(1.0f);
            FloatStream stream2 = createFloatStream(3.0f, 4.0f);
            FloatStream stream3 = createFloatStream(5.0f, 6.0f, 7.0f);
            assertArrayEquals(new float[] { 9.0f, 10.0f, 7.0f }, stream1.zipWith(stream2, stream3, 0.0f, 0.0f, 0.0f, (a, b, c) -> a + b + c).toArray());
        }
    }

    @Nested
    @DisplayName("Intermediate Operations - Side Effects")
    public class SideEffects {

        @Test
        @DisplayName("peek() should perform action on each element")
        public void testPeek() {
            List<Float> collected = new ArrayList<>();
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.peek(collected::add).toArray());
            assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), collected);
        }

        @Test
        @DisplayName("onEach() should perform action on each element")
        public void testOnEach() {
            List<Float> collected = new ArrayList<>();
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.onEach(collected::add).toArray());
            assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), collected);
        }
    }

    @Nested
    @DisplayName("Intermediate Operations - Conditional")
    public class ConditionalOperations {

        @Test
        @DisplayName("throwIfEmpty() should throw for empty stream")
        public void testThrowIfEmpty() {
            FloatStream stream1 = createFloatStream(1.0f);
            assertDoesNotThrow(() -> stream1.throwIfEmpty().toArray());

            FloatStream stream2 = FloatStream.empty();
            assertThrows(RuntimeException.class, () -> stream2.throwIfEmpty().toArray());
        }

        @Test
        @DisplayName("ifEmpty() should execute action for empty stream")
        public void testIfEmpty() {
            List<String> actions = new ArrayList<>();

            FloatStream stream1 = createFloatStream(1.0f);
            stream1.ifEmpty(() -> actions.add("empty1")).toArray();

            FloatStream stream2 = FloatStream.empty();
            stream2.ifEmpty(() -> actions.add("empty2")).toArray();

            assertEquals(Arrays.asList("empty2"), actions);
        }
    }

    @Nested
    @DisplayName("Terminal Operations - Aggregation")
    public class AggregationOperations {

        @Test
        @DisplayName("count() should return element count")
        public void testCount() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertEquals(3, stream.count());
        }

        @Test
        @DisplayName("sum() should return sum of elements")
        public void testSum() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertEquals(6.0, stream.sum(), 0.001);
        }

        @Test
        @DisplayName("average() should return average")
        public void testAverage() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            OptionalDouble avg = stream.average();
            assertTrue(avg.isPresent());
            assertEquals(2.0, avg.getAsDouble(), 0.001);
        }

        @Test
        @DisplayName("min() should return minimum")
        public void testMin() {
            FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f);
            OptionalFloat min = stream.min();
            assertTrue(min.isPresent());
            assertEquals(1.0f, min.get(), 0.001);
        }

        @Test
        @DisplayName("max() should return maximum")
        public void testMax() {
            FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f);
            OptionalFloat max = stream.max();
            assertTrue(max.isPresent());
            assertEquals(4.0f, max.get(), 0.001);
        }

        @Test
        @DisplayName("kthLargest() should return kth largest")
        public void testKthLargest() {
            FloatStream stream = createFloatStream(3.0f, 1.0f, 4.0f, 2.0f, 5.0f);
            OptionalFloat kth = stream.kthLargest(2);
            assertTrue(kth.isPresent());
            assertEquals(4.0f, kth.get(), 0.001);
        }

        @Test
        @DisplayName("summarize() should return summary statistics")
        public void testSummarize() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            FloatSummaryStatistics stats = stream.summarize();
            assertEquals(5, stats.getCount());
            assertEquals(1.0f, stats.getMin(), 0.001);
            assertEquals(5.0f, stats.getMax(), 0.001);
            assertEquals(15.0, stats.getSum(), 0.001);
            assertEquals(3.0, stats.getAverage(), 0.001);
        }

        @Test
        @DisplayName("percentiles() should return percentile map")
        public void testPercentiles() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            Optional<Map<Percentage, Float>> result = stream.percentiles();
            assertTrue(result.isPresent());
        }

        @Test
        @DisplayName("summarizeAndPercentiles() should return both")
        public void testSummarizeAndPercentiles() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f, 5.0f);
            Pair<FloatSummaryStatistics, Optional<Map<Percentage, Float>>> result = stream.summarizeAndPercentiles();
            assertEquals(5, result.left().getCount());
            assertTrue(result.right().isPresent());
        }
    }

    @Nested
    @DisplayName("Terminal Operations - Reduction")
    public class ReductionOperations {

        @Test
        @DisplayName("reduce() with identity should return value")
        public void testReduceWithIdentity() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertEquals(6.0f, stream.reduce(0.0f, Float::sum), 0.001);
        }

        @Test
        @DisplayName("reduce() without identity should return Optional")
        public void testReduceWithoutIdentity() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            OptionalFloat result = stream.reduce(Float::sum);
            assertTrue(result.isPresent());
            assertEquals(6.0f, result.get(), 0.001);
        }

        @Test
        @DisplayName("reduce() of empty stream should return empty")
        public void testReduceEmpty() {
            FloatStream stream = FloatStream.empty();
            OptionalFloat result = stream.reduce(Float::sum);
            assertFalse(result.isPresent());
        }
    }

    @Nested
    @DisplayName("Terminal Operations - Search")
    public class SearchOperations {

        @Test
        @DisplayName("first() should return first element")
        public void testFirst() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            OptionalFloat first = stream.first();
            assertTrue(first.isPresent());
            assertEquals(1.0f, first.get(), 0.001);
        }

        @Test
        @DisplayName("findFirst() with predicate should find first matching")
        public void testFindFirstWithPredicate() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            OptionalFloat result = stream.findFirst(x -> x > 2.0f);
            assertTrue(result.isPresent());
            assertEquals(3.0f, result.get(), 0.001);
        }

        @Test
        @DisplayName("last() should return last element")
        public void testLast() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            OptionalFloat last = stream.last();
            assertTrue(last.isPresent());
            assertEquals(3.0f, last.get(), 0.001);
        }

        @Test
        @DisplayName("findLast() with predicate should find last matching")
        public void testFindLastWithPredicate() {
            FloatStream stream = createFloatStream(1.0f, 3.0f, 2.0f, 4.0f);
            OptionalFloat result = stream.findLast(x -> x > 2.0f);
            assertTrue(result.isPresent());
            assertEquals(4.0f, result.get(), 0.001);
        }

        @Test
        @DisplayName("findAny() with predicate should find any matching")
        public void testFindAnyWithPredicate() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            OptionalFloat result = stream.findAny(x -> x > 2.0f);
            assertTrue(result.isPresent());
            assertTrue(result.get() > 2.0f);
        }

        @Test
        @DisplayName("onlyOne() should return single element")
        public void testOnlyOne() {
            FloatStream stream = createFloatStream(5.0f);
            OptionalFloat only = stream.onlyOne();
            assertTrue(only.isPresent());
            assertEquals(5.0f, only.get(), 0.001);
        }

        @Test
        @DisplayName("onlyOne() with multiple elements should throw")
        public void testOnlyOneMultiple() {
            FloatStream stream = createFloatStream(1.0f, 2.0f);
            assertThrows(Exception.class, stream::onlyOne);
        }

        @Test
        @DisplayName("elementAt() should return element at index")
        public void testElementAt() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f, 4.0f);
            OptionalFloat element = stream.elementAt(2);
            assertTrue(element.isPresent());
            assertEquals(3.0f, element.get(), 0.001);
        }
    }

    @Nested
    @DisplayName("Terminal Operations - Matching")
    public class MatchingOperations {

        @Test
        @DisplayName("anyMatch() should return true if any matches")
        public void testAnyMatch() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertTrue(stream.anyMatch(x -> x > 2.0f));
        }

        @Test
        @DisplayName("allMatch() should return true if all match")
        public void testAllMatch() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertTrue(stream.allMatch(x -> x > 0.0f));

            FloatStream stream2 = createFloatStream(1.0f, 2.0f, 3.0f);
            assertFalse(stream2.allMatch(x -> x > 2.0f));
        }

        @Test
        @DisplayName("noneMatch() should return true if none match")
        public void testNoneMatch() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertTrue(stream.noneMatch(x -> x > 5.0f));

            FloatStream stream2 = createFloatStream(1.0f, 2.0f, 3.0f);
            assertFalse(stream2.noneMatch(x -> x > 2.0f));
        }
    }

    @Nested
    @DisplayName("Terminal Operations - Collection")
    public class CollectionOperations {

        @Test
        @DisplayName("toArray() should return array")
        public void testToArray() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertArrayEquals(new float[] { 1.0f, 2.0f, 3.0f }, stream.toArray());
        }

        @Test
        @DisplayName("toFloatList() should return FloatList")
        public void testToFloatList() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            FloatList result = stream.toFloatList();
            assertEquals(3, result.size());
            assertEquals(1.0f, result.get(0), 0.001);
        }

        @Test
        @DisplayName("toList() should return List")
        public void testToList() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            List<Float> result = stream.toList();
            assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), result);
        }

        @Test
        @DisplayName("toSet() should return Set")
        public void testToSet() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 2.0f, 3.0f);
            Set<Float> result = stream.toSet();
            assertEquals(new HashSet<>(Arrays.asList(1.0f, 2.0f, 3.0f)), result);
        }

        @Test
        @DisplayName("toMap() should create map")
        public void testToMap() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            Map<String, Float> result = stream.toMap(f -> "key" + (int) f, f -> f * 2);
            assertEquals(3, result.size());
            assertEquals(2.0f, result.get("key1"), 0.001);
        }

        @Test
        @DisplayName("toMap() with merge function")
        public void testToMapWithMerge() {
            FloatStream stream = createFloatStream(1.1f, 1.9f, 2.1f);
            Map<Integer, Float> result = stream.toMap(f -> (int) f, f -> f, Float::sum);
            assertEquals(2, result.size());
            assertEquals(3.0f, result.get(1), 0.001);
        }

        @Test
        @DisplayName("toMap() with map factory")
        public void testToMapWithFactory() {
            FloatStream stream = createFloatStream(3.0f, 1.0f, 2.0f);
            Map<String, Float> result = stream.toMap(f -> String.valueOf((int) f), f -> f, Suppliers.ofLinkedHashMap());
            assertTrue(result instanceof LinkedHashMap);
        }

        @Test
        @DisplayName("groupTo() should group elements")
        public void testGroupTo() {
            FloatStream stream = createFloatStream(1.1f, 1.9f, 2.1f, 2.9f);
            Map<Integer, List<Float>> result = stream.groupTo(f -> (int) f, Collectors.toList());
            assertEquals(2, result.size());
            assertEquals(2, result.get(1).size());
        }

        @Test
        @DisplayName("toMultiset() should return multiset")
        public void testToMultiset() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 1.0f, 3.0f, 2.0f, 1.0f);
            Multiset<Float> result = stream.toMultiset();
            assertEquals(3, result.count(1.0f));
            assertEquals(2, result.count(2.0f));
            assertEquals(1, result.count(3.0f));
        }

        @Test
        @DisplayName("collect() with supplier, accumulator, combiner")
        public void testCollect() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            StringBuilder result = stream.collect(StringBuilder::new, (sb, f) -> sb.append(f).append(","), StringBuilder::append);
            assertEquals("1.0,2.0,3.0,", result.toString());
        }
    }

    @Nested
    @DisplayName("Terminal Operations - ForEach")
    public class ForEachOperations {

        @Test
        @DisplayName("forEach() should execute action")
        public void testForEach() {
            List<Float> collected = new ArrayList<>();
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            stream.forEach(collected::add);
            assertEquals(Arrays.asList(1.0f, 2.0f, 3.0f), collected);
        }

        @Test
        @DisplayName("forEachIndexed() should provide index")
        public void testForEachIndexed() {
            List<String> collected = new ArrayList<>();
            FloatStream stream = createFloatStream(10.0f, 20.0f, 30.0f);
            stream.forEachIndexed((index, value) -> collected.add(index + ":" + value));
            assertEquals(Arrays.asList("0:10.0", "1:20.0", "2:30.0"), collected);
        }
    }

    @Nested
    @DisplayName("Terminal Operations - Joining")
    public class JoiningOperations {

        @Test
        @DisplayName("join() with delimiter")
        public void testJoin() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertEquals("1.0, 2.0, 3.0", stream.join(", "));
        }

        @Test
        @DisplayName("join() with prefix and suffix")
        public void testJoinWithPrefixSuffix() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertEquals("[1.0, 2.0, 3.0]", stream.join(", ", "[", "]"));
        }
    }

    @Nested
    @DisplayName("BaseStream - Parallelism")
    public class ParallelismOperations {

        @Test
        @DisplayName("isParallel() should return false for sequential")
        public void testIsParallelFalse() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            assertFalse(stream.isParallel());
        }

        @Test
        @DisplayName("parallel() should return parallel stream")
        public void testParallel() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            FloatStream parallel = stream.parallel();
            assertTrue(parallel.isParallel());
        }

        @Test
        @DisplayName("parallel() with max thread number")
        public void testParallelWithMaxThreads() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            FloatStream parallel = stream.parallel(2);
            assertTrue(parallel.isParallel());
        }

        @Test
        @DisplayName("parallel() with executor")
        public void testParallelWithExecutor() {
            Executor executor = Executors.newFixedThreadPool(2);
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            FloatStream parallel = stream.parallel(executor);
            assertTrue(parallel.isParallel());
        }

        @Test
        @DisplayName("sequential() should return sequential stream")
        public void testSequential() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f).parallel();
            FloatStream sequential = stream.sequential();
            assertFalse(sequential.isParallel());
        }
    }

    @Nested
    @DisplayName("BaseStream - Lifecycle")
    public class LifecycleOperations {

        @Test
        @DisplayName("onClose() should register close handler")
        public void testOnClose() {
            List<String> closeActions = new ArrayList<>();
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f).onClose(() -> closeActions.add("closed"));
            stream.toArray();
            assertEquals(Arrays.asList("closed"), closeActions);
        }

        @Test
        @DisplayName("close() should close stream")
        public void testClose() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            stream.close();
        }

        @Test
        @DisplayName("closed stream should throw IllegalStateException")
        public void testClosedStream() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            stream.toArray();
            assertThrows(IllegalStateException.class, () -> stream.count());
        }
    }

    @Nested
    @DisplayName("BaseStream - Iterator")
    public class IteratorOperations {

        @Test
        @DisplayName("iterator() should return iterator")
        public void testIterator() {
            FloatStream stream = createFloatStream(1.0f, 2.0f, 3.0f);
            FloatIterator iter = stream.iterator();
            assertTrue(iter.hasNext());
            assertEquals(1.0f, iter.nextFloat(), 0.001);
        }
    }

    @Nested
    @DisplayName("Edge Cases")
    public class EdgeCases {

        @Test
        @DisplayName("empty stream operations")
        public void testEmptyStream() {
            FloatStream empty = FloatStream.empty();
            assertEquals(0, empty.count());

            assertFalse(FloatStream.empty().first().isPresent());
            assertFalse(FloatStream.empty().min().isPresent());
            assertFalse(FloatStream.empty().max().isPresent());
            assertEquals(0.0, FloatStream.empty().sum(), 0.001);
            assertFalse(FloatStream.empty().average().isPresent());
        }

        @Test
        @DisplayName("single element stream")
        public void testSingleElement() {
            FloatStream single = createFloatStream(5.0f);
            assertEquals(1, single.count());

            single = createFloatStream(5.0f);
            assertEquals(5.0f, single.first().get(), 0.001);
        }

        @Test
        @DisplayName("special float values")
        public void testSpecialValues() {
            FloatStream stream = createFloatStream(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 0.0f);
            float[] result = stream.toArray();
            assertEquals(4, result.length);
            assertTrue(Float.isNaN(result[0]));
            assertTrue(Float.isInfinite(result[1]));
            assertTrue(Float.isInfinite(result[2]));
        }

        @Test
        @DisplayName("negative arguments should throw")
        public void testNegativeArguments() {
            assertThrows(IllegalArgumentException.class, () -> FloatStream.repeat(1.0f, -1));
            assertThrows(IllegalArgumentException.class, () -> createFloatStream(1.0f, 2.0f).limit(-1));
            assertThrows(IllegalArgumentException.class, () -> createFloatStream(1.0f, 2.0f).skip(-1));
        }

        @Test
        @DisplayName("null arguments should throw")
        public void testNullArguments() {
            assertThrows(NullPointerException.class, () -> createFloatStream(1.0f, 2.0f).map(null).count());
            assertThrows(NullPointerException.class, () -> createFloatStream(1.0f, 2.0f).filter(null).count());
        }
    }

    // Additional coverage improvement tests - Collection-based factory methods

    @Test
    @DisplayName("zip() collection of streams with function")
    public void testZipCollectionStreamsAdditional() {
        Collection<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f), FloatStream.of(10.0f, 20.0f), FloatStream.of(100.0f, 200.0f));
        FloatStream result = FloatStream.zip(streams, floats -> {
            float sum = 0.0f;
            for (Float f : floats)
                sum += f;
            return sum;
        });
        assertArrayEquals(new float[] { 111.0f, 222.0f }, result.toArray());
    }

    @Test
    @DisplayName("zip() collection with defaults additional")
    public void testZipCollectionDefaultsAdditional() {
        Collection<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 2.0f, 3.0f), FloatStream.of(10.0f, 20.0f));
        float[] defaults = { 0.0f, 100.0f };
        FloatStream result = FloatStream.zip(streams, defaults, floats -> {
            float sum = 0.0f;
            for (Float f : floats)
                sum += f;
            return sum;
        });
        assertEquals(3, result.count());
    }

    @Test
    @DisplayName("merge() collection of streams additional")
    public void testMergeCollectionAdditional() {
        Collection<FloatStream> streams = Arrays.asList(FloatStream.of(1.0f, 7.0f), FloatStream.of(3.0f, 8.0f), FloatStream.of(5.0f, 9.0f));
        FloatStream result = FloatStream.merge(streams, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, result.count());
    }

    @Test
    @DisplayName("merge() three arrays additional")
    public void testMergeThreeArraysAdditional() {
        float[] a1 = { 1.0f, 10.0f };
        float[] a2 = { 4.0f, 11.0f };
        float[] a3 = { 7.0f, 12.0f };
        FloatStream stream = FloatStream.merge(a1, a2, a3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    @Test
    @DisplayName("merge() three streams additional")
    public void testMergeThreeStreamsAdditional() {
        FloatStream s1 = FloatStream.of(1.0f, 15.0f);
        FloatStream s2 = FloatStream.of(8.0f, 16.0f);
        FloatStream s3 = FloatStream.of(12.0f, 17.0f);
        FloatStream stream = FloatStream.merge(s1, s2, s3, (a, b) -> a <= b ? MergeResult.TAKE_FIRST : MergeResult.TAKE_SECOND);
        assertEquals(6, stream.count());
    }

    // Edge cases

    @Test
    @DisplayName("flatten() with alignment horizontally additional")
    public void testFlattenAlignmentAdditional() {
        float[][] array = { { 1.0f, 2.0f, 3.0f }, { 4.0f, 5.0f }, { 6.0f } };
        FloatStream stream = FloatStream.flatten(array, 0.0f, true);
        float[] result = stream.toArray();
        assertEquals(9, result.length);
        assertEquals(1.0f, result[0], 0.001);
        assertEquals(4.0f, result[1], 0.001);
        assertEquals(6.0f, result[2], 0.001);
    }

    // Parallel operations

    @Test
    @DisplayName("parallel stream operations additional")
    public void testParallelOperationsAdditional() {
        FloatStream stream = FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f).parallel();
        float[] result = stream.filter(f -> f > 3.0f).map(f -> f * 2).toArray();
        assertEquals(5, result.length);
    }

    @Test
    @DisplayName("iterate() with immediate false hasNext additional")
    public void testIterateFalseHasNextAdditional() {
        FloatStream stream = FloatStream.iterate(1.0f, () -> false, f -> f + 1.0f);
        assertEquals(0, stream.count());
    }

    @Test
    @DisplayName("collapse() with no collapsible elements additional")
    public void testCollapseNoCollapsibleAdditional() {
        FloatStream stream = FloatStream.of(1.0f, 10.0f, 20.0f, 30.0f);
        float[] result = stream.collapse((a, b) -> Math.abs(b - a) <= 2.0f, Float::sum).toArray();
        assertTrue(result.length > 0);
    }

    @Test
    @DisplayName("scan() on empty stream additional")
    public void testScanEmptyAdditional() {
        FloatStream emptyStream = FloatStream.empty();
        float[] result = emptyStream.scan(Float::sum).toArray();
        assertEquals(0, result.length);
    }

    @Test
    @DisplayName("zip() three arrays with defaults additional")
    public void testZipThreeArraysDefaultsAdditional() {
        float[] a1 = { 1.0f, 2.0f };
        float[] a2 = { 10.0f };
        float[] a3 = { 100.0f, 200.0f, 300.0f };
        FloatStream stream = FloatStream.zip(a1, a2, a3, 0.0f, 0.0f, 0.0f, (a, b, c) -> a + b + c);
        assertEquals(3, stream.count());
    }

    @Test
    @DisplayName("zip() three iterators with defaults additional")
    public void testZipThreeIteratorsDefaultsAdditional() {
        FloatIterator iter1 = FloatIterator.of(1.0f, 2.0f);
        FloatIterator iter2 = FloatIterator.of(10.0f);
        FloatIterator iter3 = FloatIterator.of(100.0f, 200.0f, 300.0f);
        FloatStream stream = FloatStream.zip(iter1, iter2, iter3, 0.0f, 0.0f, 0.0f, (a, b, c) -> a + b + c);
        assertEquals(3, stream.count());
    }

    @Test
    @DisplayName("zip() three streams with defaults additional")
    public void testZipThreeStreamsDefaultsAdditional() {
        FloatStream s1 = FloatStream.of(1.0f, 2.0f);
        FloatStream s2 = FloatStream.of(10.0f);
        FloatStream s3 = FloatStream.of(100.0f, 200.0f, 300.0f);
        FloatStream stream = FloatStream.zip(s1, s2, s3, 0.0f, 0.0f, 0.0f, (a, b, c) -> a + b + c);
        assertEquals(3, stream.count());
    }

    // ==================== debounce tests ====================

    @Test
    public void testDebounce_BasicFunctionality() {
        // Allow 3 elements per 1 second window
        float[] result = FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        // Only first 3 elements should pass through within the window
        assertEquals(3, result.length);
        assertEquals(1.0f, result[0], 0.001f);
        assertEquals(2.0f, result[1], 0.001f);
        assertEquals(3.0f, result[2], 0.001f);
    }

    @Test
    public void testDebounce_AllElementsPassWhenWithinLimit() {
        // Allow 10 elements per window, but only 5 elements in stream
        float[] result = FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
                .debounce(10, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        // All elements should pass
        assertEquals(5, result.length);
    }

    @Test
    public void testDebounce_EmptyStream() {
        float[] result = FloatStream.empty()
                .debounce(5, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals(0, result.length);
    }

    @Test
    public void testDebounce_SingleElement() {
        float[] result = FloatStream.of(42.5f)
                .debounce(1, com.landawn.abacus.util.Duration.ofMillis(100))
                .toArray();

        assertEquals(1, result.length);
        assertEquals(42.5f, result[0], 0.001f);
    }

    @Test
    public void testDebounce_MaxWindowSizeOne() {
        // Only 1 element allowed per window
        float[] result = FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f)
                .debounce(1, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals(1, result.length);
        assertEquals(1.0f, result[0], 0.001f);
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveMaxWindowSize() {
        assertThrows(IllegalArgumentException.class, () -> {
            FloatStream.of(1.0f, 2.0f, 3.0f).debounce(0, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            FloatStream.of(1.0f, 2.0f, 3.0f).debounce(-1, com.landawn.abacus.util.Duration.ofSeconds(1)).toArray();
        });
    }

    @Test
    public void testDebounce_ThrowsExceptionForNonPositiveDuration() {
        assertThrows(IllegalArgumentException.class, () -> {
            FloatStream.of(1.0f, 2.0f, 3.0f).debounce(5, com.landawn.abacus.util.Duration.ofMillis(0)).toArray();
        });

        assertThrows(IllegalArgumentException.class, () -> {
            FloatStream.of(1.0f, 2.0f, 3.0f).debounce(5, com.landawn.abacus.util.Duration.ofMillis(-100)).toArray();
        });
    }

    @Test
    public void testDebounce_WithLargeMaxWindowSize() {
        float[] input = new float[1000];
        for (int i = 0; i < 1000; i++) {
            input[i] = i;
        }

        float[] result = FloatStream.of(input)
                .debounce(500, com.landawn.abacus.util.Duration.ofSeconds(10))
                .toArray();

        assertEquals(500, result.length);
    }

    @Test
    public void testDebounce_PreservesOrder() {
        float[] result = FloatStream.of(10.0f, 20.0f, 30.0f, 40.0f, 50.0f)
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals(10.0f, result[0], 0.001f);
        assertEquals(20.0f, result[1], 0.001f);
        assertEquals(30.0f, result[2], 0.001f);
    }

    @Test
    public void testDebounce_ChainedWithOtherOperations() {
        float[] result = FloatStream.of(1.0f, 2.0f, 3.0f, 4.0f, 5.0f, 6.0f, 7.0f, 8.0f, 9.0f, 10.0f)
                .filter(n -> n % 2 == 0)  // 2, 4, 6, 8, 10
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))  // 2, 4, 6
                .map(n -> n * 10)  // 20, 40, 60
                .toArray();

        assertEquals(3, result.length);
        assertEquals(20.0f, result[0], 0.001f);
        assertEquals(40.0f, result[1], 0.001f);
        assertEquals(60.0f, result[2], 0.001f);
    }

    @Test
    public void testDebounce_WithSpecialValues() {
        float[] result = FloatStream.of(Float.NaN, Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1.0f, 2.0f)
                .debounce(3, com.landawn.abacus.util.Duration.ofSeconds(1))
                .toArray();

        assertEquals(3, result.length);
        assertTrue(Float.isNaN(result[0]));
        assertEquals(Float.POSITIVE_INFINITY, result[1], 0.001f);
        assertEquals(Float.NEGATIVE_INFINITY, result[2], 0.001f);
    }
}
