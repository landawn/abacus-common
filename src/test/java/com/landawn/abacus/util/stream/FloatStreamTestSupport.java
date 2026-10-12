package com.landawn.abacus.util.stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.FloatBuffer;
import java.util.Arrays;
import java.util.Collection;
import java.util.NoSuchElementException;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.FloatIterator;
import com.landawn.abacus.util.FloatList;

/** Shared fixtures and lifecycle for the FloatStream contract tests. */
public abstract class FloatStreamTestSupport extends TestBase {

    protected static final float DELTA = 0.0001f;

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
    public class FactoryMethods extends TestBase {
    }

    @Nested
    @DisplayName("Zip Operations")
    public class ZipOperations extends TestBase {
    }

    @Nested
    @DisplayName("Merge Operations")
    public class MergeOperations extends TestBase {
    }

    @Nested
    @DisplayName("Intermediate Operations - Filtering")
    public class FilteringOperations extends TestBase {
    }

    @Nested
    @DisplayName("Intermediate Operations - Transformation")
    public class TransformationOperations extends TestBase {
    }

    @Nested
    @DisplayName("Intermediate Operations - Advanced")
    public class AdvancedOperations extends TestBase {
    }

    @Nested
    @DisplayName("Intermediate Operations - Sorting")
    public class SortingOperations extends TestBase {
    }

    @Nested
    @DisplayName("Intermediate Operations - Set Operations")
    public class SetOperations extends TestBase {
    }

    @Nested
    @DisplayName("Intermediate Operations - Instance Zip and Merge")
    public class InstanceZipMerge extends TestBase {
    }

    @Nested
    @DisplayName("Intermediate Operations - Side Effects")
    public class SideEffects extends TestBase {
    }

    @Nested
    @DisplayName("Intermediate Operations - Conditional")
    public class ConditionalOperations extends TestBase {
    }

    @Nested
    @DisplayName("Terminal Operations - Aggregation")
    public class AggregationOperations extends TestBase {
    }

    @Nested
    @DisplayName("Terminal Operations - Reduction")
    public class ReductionOperations extends TestBase {
    }

    @Nested
    @DisplayName("Terminal Operations - Search")
    public class SearchOperations extends TestBase {
    }

    @Nested
    @DisplayName("Terminal Operations - Matching")
    public class MatchingOperations extends TestBase {
    }

    @Nested
    @DisplayName("Terminal Operations - Collection")
    public class CollectionOperations extends TestBase {
    }

    @Nested
    @DisplayName("Terminal Operations - ForEach")
    public class ForEachOperations extends TestBase {
    }

    @Nested
    @DisplayName("Terminal Operations - Joining")
    public class JoiningOperations extends TestBase {
    }

    @Nested
    @DisplayName("BaseStream - Parallelism")
    public class ParallelismOperations extends TestBase {
    }

    @Nested
    @DisplayName("BaseStream - Lifecycle")
    public class LifecycleOperations extends TestBase {
    }

    @Nested
    @DisplayName("BaseStream - Iterator")
    public class IteratorOperations extends TestBase {
    }

    @Nested
    @DisplayName("Edge Cases")
    public class EdgeCases extends TestBase {
    }

    // ---- perf review 2026-09-26 G096 begin ----
    protected static float[] flattenVerticallyReference(final float[][] a) {
        final FloatList ret = new FloatList();
        int maxLen = 0;

        for (final float[] row : a) {
            maxLen = Math.max(maxLen, row == null ? 0 : row.length);
        }

        for (int col = 0; col < maxLen; col++) {
            for (final float[] row : a) {
                if (row != null && col < row.length) {
                    ret.add(row[col]);
                }
            }
        }

        return ret.toArray();
    }

    // ---- perf review 2026-09-26 G112 end ----
    // ---- perf review 2026-09-26 G114 begin ----
    protected static float[] flattenVerticallyReferenceG114(final float[][] a) {
        final FloatList ret = new FloatList();
        int maxLen = 0;

        for (final float[] row : a) {
            maxLen = Math.max(maxLen, row == null ? 0 : row.length);
        }

        for (int col = 0; col < maxLen; col++) {
            for (final float[] row : a) {
                if (row != null && col < row.length) {
                    ret.add(row[col]);
                }
            }
        }

        return ret.toArray();
    }

    protected static void assertFlattenVerticallyMatchesReferenceG114(final float[][] a, final int skip) {
        final float[] expected = flattenVerticallyReferenceG114(a);
        final String message = Arrays.deepToString(a);
        assertArrayEquals(expected, FloatStream.flatten(a, true).toArray(), message);
        assertEquals(expected.length, FloatStream.flatten(a, true).count(), message);
        assertArrayEquals(Arrays.copyOfRange(expected, Math.min(skip, expected.length), expected.length), FloatStream.flatten(a, true).skip(skip).toArray(),
                message);

        final FloatIterator iter = FloatStream.flatten(a, true).iterator();

        for (final float element : expected) {
            assertTrue(iter.hasNext());
            assertEquals(element, iter.nextFloat());
        }

        assertFalse(iter.hasNext());
        assertThrows(NoSuchElementException.class, iter::nextFloat);
        assertFalse(iter.hasNext());
    }
}
