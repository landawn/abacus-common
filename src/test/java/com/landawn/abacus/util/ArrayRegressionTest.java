package com.landawn.abacus.util;

import java.util.Arrays;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;

import com.landawn.abacus.TestBase;

/**
 * Regression tests for the {@code Array} behaviour corrected in the 2026-08-31 review pass.
 *
 * <ul>
 *   <li>the 16 primitive 2-D/3-D {@code concat} overloads normalise a {@code null} row/layer to an empty
 *       array on <i>every</i> path - the empty-side shortcut used to hand the caller's {@code null} rows back
 *       unchanged, so whether a {@code null} survived depended on the other argument,</li>
 *   <li>the generic {@code concat2D}/{@code concat3D} keep propagating {@code null} rows, deliberately,</li>
 *   <li>{@code newInstance(componentType, 0)} no longer interns arbitrary component types,</li>
 *   <li>the {@code range}/{@code rangeClosed}/{@code repeat} size guards and the matrix-shape checks report
 *       what actually went wrong.</li>
 * </ul>
 */
public class ArrayRegressionTest extends TestBase {

    // ============================================================ concat: null rows are normalised everywhere

    @Test
    public void testConcat2D_nullRowIsNormalisedWhateverTheOtherArgumentIs() {
        final boolean[][] a = { { true }, null };

        // The row-by-row path always normalised; the shortcuts below did not.
        Assertions.assertArrayEquals(new boolean[][] { { true, false }, { true } }, Array.concat(a, new boolean[][] { { false }, { true } }));
        Assertions.assertArrayEquals(new boolean[][] { { true }, {} }, Array.concat(a, new boolean[0][]));
        Assertions.assertArrayEquals(new boolean[][] { { true }, {} }, Array.concat(a, (boolean[][]) null));
        Assertions.assertArrayEquals(new boolean[][] { { true }, {} }, Array.concat((boolean[][]) null, a));
        Assertions.assertArrayEquals(new boolean[][] { { true }, {} }, Array.concat(new boolean[0][], a));
    }

    @Test
    public void testConcat2D_neverReturnsANullRowForAnyPrimitiveType() {
        Assertions.assertArrayEquals(new boolean[][] { {} }, Array.concat(new boolean[][] { null }, new boolean[0][]));
        Assertions.assertArrayEquals(new char[][] { {} }, Array.concat(new char[][] { null }, new char[0][]));
        Assertions.assertArrayEquals(new byte[][] { {} }, Array.concat(new byte[][] { null }, new byte[0][]));
        Assertions.assertArrayEquals(new short[][] { {} }, Array.concat(new short[][] { null }, new short[0][]));
        Assertions.assertArrayEquals(new int[][] { {} }, Array.concat(new int[][] { null }, new int[0][]));
        Assertions.assertArrayEquals(new long[][] { {} }, Array.concat(new long[][] { null }, new long[0][]));
        Assertions.assertArrayEquals(new float[][] { {} }, Array.concat(new float[][] { null }, new float[0][]));
        Assertions.assertArrayEquals(new double[][] { {} }, Array.concat(new double[][] { null }, new double[0][]));
    }

    @Test
    public void testConcat3D_neverReturnsANullLayerForAnyPrimitiveType() {
        Assertions.assertArrayEquals(new boolean[][][] { {} }, Array.concat(new boolean[][][] { null }, new boolean[0][][]));
        Assertions.assertArrayEquals(new char[][][] { {} }, Array.concat(new char[][][] { null }, new char[0][][]));
        Assertions.assertArrayEquals(new byte[][][] { {} }, Array.concat(new byte[][][] { null }, new byte[0][][]));
        Assertions.assertArrayEquals(new short[][][] { {} }, Array.concat(new short[][][] { null }, new short[0][][]));
        Assertions.assertArrayEquals(new int[][][] { {} }, Array.concat(new int[][][] { null }, new int[0][][]));
        Assertions.assertArrayEquals(new long[][][] { {} }, Array.concat(new long[][][] { null }, new long[0][][]));
        Assertions.assertArrayEquals(new float[][][] { {} }, Array.concat(new float[][][] { null }, new float[0][][]));
        Assertions.assertArrayEquals(new double[][][] { {} }, Array.concat(new double[][][] { null }, new double[0][][]));
    }

    /**
     * B4 changed all 16 primitive 2-D/3-D overloads the same way, so check all 16 against every combination of
     * {@code null}/empty/populated arguments: no path may put a {@code null} into the result.
     */
    @Test
    public void testNoPrimitiveConcatOverloadEverReturnsANullRow() throws Exception {
        final Class<?>[] prims = { boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class };
        int checked = 0;

        for (final Class<?> prim : prims) {
            for (int dims = 2; dims <= 3; dims++) {
                final int[] shape = dims == 2 ? new int[] { 2, 1 } : new int[] { 2, 1, 1 };
                final Object withNullRow = java.lang.reflect.Array.newInstance(prim, shape);
                java.lang.reflect.Array.set(withNullRow, 1, null); // {row, null}
                final Class<?> arrType = withNullRow.getClass();
                final Object empty = java.lang.reflect.Array.newInstance(prim, dims == 2 ? new int[] { 0, 0 } : new int[] { 0, 0, 0 });
                final java.lang.reflect.Method concat = Array.class.getMethod("concat", arrType, arrType);

                for (final Object[] args : new Object[][] { { withNullRow, null }, { null, withNullRow }, { withNullRow, empty }, { empty, withNullRow },
                        { withNullRow, withNullRow } }) {
                    final Object result = concat.invoke(null, args[0], args[1]);
                    final String label = prim.getSimpleName() + " " + dims + "D " + (args[0] == null ? "null" : args[0] == empty ? "empty" : "withNullRow")
                            + "/" + (args[1] == null ? "null" : args[1] == empty ? "empty" : "withNullRow");

                    Assertions.assertNotNull(result, label);
                    Assertions.assertEquals(2, java.lang.reflect.Array.getLength(result), label);

                    for (int i = 0; i < 2; i++) {
                        Assertions.assertNotNull(java.lang.reflect.Array.get(result, i), label + " row " + i + " must not be null");
                    }

                    checked++;
                }
            }
        }

        Assertions.assertEquals(80, checked);
    }

    @Test
    public void testConcat3D_nullLayerAndNullRowInsideALayerAreBothNormalised() {
        final int[][][] a = { { { 1 }, null }, null };

        Assertions.assertArrayEquals(new int[][][] { { { 1 }, {} }, {} }, Array.concat(a, new int[0][][]));
        Assertions.assertArrayEquals(new int[][][] { { { 1, 2 }, {} }, {} }, Array.concat(a, new int[][][] { { { 2 } } }));
    }

    @Test
    public void testConcat_existingBehaviourIsUnchanged() {
        Assertions.assertArrayEquals(new int[][] { { 1, 2, 5 }, { 3, 6, 7 } }, Array.concat(new int[][] { { 1, 2 }, { 3 } }, new int[][] { { 5 }, { 6, 7 } }));
        Assertions.assertArrayEquals(new int[][] { { 1, 2, 5 }, { 3 } }, Array.concat(new int[][] { { 1, 2 }, { 3 } }, new int[][] { { 5 } }));
        Assertions.assertArrayEquals(new int[0][], Array.concat((int[][]) null, (int[][]) null));
        Assertions.assertArrayEquals(new int[0][][], Array.concat((int[][][]) null, (int[][][]) null));
        Assertions.assertArrayEquals(new int[][][] { { { 1, 2 } } }, Array.concat(new int[][][] { { { 1 } } }, new int[][][] { { { 2 } } }));
    }

    @Test
    public void testConcat_doesNotAliasEitherInput() {
        // Removing the N.clone shortcut must not start handing back the caller's own rows.
        final int[][] a = { { 1, 2 } };
        final int[][] result = Array.concat(a, new int[0][]);

        Assertions.assertNotSame(a, result);
        Assertions.assertNotSame(a[0], result[0]);
        result[0][0] = 99;
        Assertions.assertEquals(1, a[0][0]);

        final int[][] b = { { 3 } };
        final int[][] fromB = Array.concat((int[][]) null, b);

        Assertions.assertNotSame(b[0], fromB[0]);
        fromB[0][0] = 99;
        Assertions.assertEquals(3, b[0][0]);
    }

    @Test
    public void testConcat3D_doesNotAliasEitherInput() {
        final int[][][] a = { { { 1, 2 } } };

        for (final int[][][] result : Arrays.asList(Array.concat(a, new int[0][][]), Array.concat(a, (int[][][]) null), Array.concat((int[][][]) null, a),
                Array.concat(a, new int[][][] { { { 9 } } }))) {
            Assertions.assertNotSame(a, result);
            Assertions.assertNotSame(a[0], result[0]);
            Assertions.assertNotSame(a[0][0], result[0][0]);
        }

        final int[][][] copy = Array.concat(a, new int[0][][]);
        copy[0][0][0] = 99;
        Assertions.assertEquals(1, a[0][0][0]);
    }

    @Test
    public void testConcat2DGeneric_stillPropagatesNullRowsDeliberately() {
        // The generic overloads propagate a null row on every path, so a result row is null exactly when both
        // input rows are - the row component type is in fact available at such a row (see the concat2D
        // javadoc). Locked in so the divergence from the primitive overloads stays a decision, not drift.
        Assertions.assertArrayEquals(new String[][] { { "a" }, null }, Array.concat2D(new String[][] { { "a" }, null }, new String[0][]));
        Assertions.assertArrayEquals(new String[][] { { "a", "b" }, null }, Array.concat2D(new String[][] { { "a" }, null }, new String[][] { { "b" }, null }));
        Assertions.assertNull(Array.concat2D((String[][]) null, (String[][]) null));
        Assertions.assertNull(Array.concat3D((String[][][]) null, (String[][][]) null));
    }

    @Test
    public void testConcat2DGeneric_nonNullEmptyFirstArgumentWithNullSecond() {
        // The reachable half of the branch whose dead null check was removed.
        final String[][] result = Array.concat2D(new String[0][], null);

        Assertions.assertNotNull(result);
        Assertions.assertEquals(0, result.length);
        Assertions.assertEquals(String[].class, result.getClass().getComponentType());

        final String[][][] result3D = Array.concat3D(new String[0][][], null);

        Assertions.assertNotNull(result3D);
        Assertions.assertEquals(0, result3D.length);
    }

    // ============================================================ newInstance: no unbounded Class caching

    private static final class LocalComponentType {
        // a type that cannot already be in the pre-seeded table
    }

    @Test
    public void testNewInstance_doesNotInternArbitraryComponentTypes() {
        final Object first = Array.newInstance(LocalComponentType.class, 0);
        final Object second = Array.newInstance(LocalComponentType.class, 0);

        Assertions.assertEquals(0, java.lang.reflect.Array.getLength(first));
        Assertions.assertEquals(LocalComponentType[].class, first.getClass());
        Assertions.assertNotSame(first, second, "an arbitrary Class must not be retained in a static cache");
    }

    @Test
    public void testNewInstance_stillSharesThePreSeededEmptyArrays() {
        Assertions.assertSame(Array.newInstance(int.class, 0), Array.newInstance(int.class, 0));
        Assertions.assertSame(Array.newInstance(String.class, 0), Array.newInstance(String.class, 0));
        Assertions.assertSame(Array.newInstance(Object.class, 0), Array.newInstance(Object.class, 0));
    }

    @Test
    public void testNewInstance_nonZeroLengthIsAlwaysFresh() {
        final int[] a = Array.newInstance(int.class, 3);
        final int[] b = Array.newInstance(int.class, 3);

        Assertions.assertNotSame(a, b);
        Assertions.assertEquals(3, a.length);
    }

    // ============================================================ error messages name the actual problem

    @Test
    public void testRangeOverflowMessagesNameTheSize() {
        Assertions.assertTrue(Assertions.assertThrows(IllegalArgumentException.class, () -> Array.range(Integer.MIN_VALUE, Integer.MAX_VALUE))
                .getMessage()
                .contains("4294967295"));
        Assertions.assertTrue(
                Assertions.assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0, Integer.MAX_VALUE)).getMessage().contains("2147483648"));
        Assertions.assertTrue(Assertions.assertThrows(IllegalArgumentException.class, () -> Array.repeat(new int[] { 1, 2 }, Integer.MAX_VALUE))
                .getMessage()
                .contains("4294967294"));
        Assertions.assertTrue(
                Assertions.assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0, Integer.MAX_VALUE, 1)).getMessage().contains("2147483648"));

        // The long endpoints cannot be subtracted into a usable size (the subtraction is what overflowed),
        // so those two name the range instead.
        Assertions.assertEquals("Overflow. Array size is too large to allocate: the range [0, 3000000000) exceeds Integer.MAX_VALUE",
                Assertions.assertThrows(IllegalArgumentException.class, () -> Array.range(0L, 3_000_000_000L)).getMessage());
        Assertions.assertEquals("Overflow. Array size is too large to allocate: the range [0, 3000000000] exceeds Integer.MAX_VALUE",
                Assertions.assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(0L, 3_000_000_000L)).getMessage());
        Assertions.assertTrue(Assertions.assertThrows(IllegalArgumentException.class, () -> Array.range(Long.MIN_VALUE, Long.MAX_VALUE))
                .getMessage()
                .contains(String.valueOf(Long.MIN_VALUE)));
        Assertions.assertTrue(Assertions.assertThrows(IllegalArgumentException.class, () -> Array.rangeClosed(Long.MIN_VALUE, Long.MAX_VALUE))
                .getMessage()
                .contains(String.valueOf(Long.MAX_VALUE)));

        // Every message must actually say something; none may be the bare "overflow" it used to be.
        for (final Executable call : new Executable[] { () -> Array.range(Integer.MIN_VALUE, Integer.MAX_VALUE), () -> Array.rangeClosed(0, Integer.MAX_VALUE),
                () -> Array.range(0L, 3_000_000_000L), () -> Array.rangeClosed(0L, 3_000_000_000L), () -> Array.repeat(new int[] { 1, 2 }, Integer.MAX_VALUE),
                () -> Array.repeat(new String[] { "a", "b" }, Integer.MAX_VALUE), () -> Array.range(0L, Long.MAX_VALUE, 1L) }) {
            Assertions.assertTrue(Assertions.assertThrows(IllegalArgumentException.class, call).getMessage().startsWith("Overflow. Array size is too large"));
        }
    }

    @Test
    public void testRangeStillProducesTheRightValuesAfterTheGuardRewrite() {
        Assertions.assertArrayEquals(new int[] { 0, 1, 2, 3, 4 }, Array.range(0, 5));
        Assertions.assertArrayEquals(new int[] {}, Array.range(5, 5));
        Assertions.assertArrayEquals(new int[] {}, Array.range(5, 0));
        Assertions.assertArrayEquals(new int[] { -2, -1, 0, 1 }, Array.range(-2, 2));
        Assertions.assertArrayEquals(new int[] { 0, 1, 2, 3, 4, 5 }, Array.rangeClosed(0, 5));
        Assertions.assertArrayEquals(new int[] { 7 }, Array.rangeClosed(7, 7));
        Assertions.assertArrayEquals(new int[] {}, Array.rangeClosed(5, 0));
        Assertions.assertArrayEquals(new int[] { -2, -1, 0, 1, 2 }, Array.rangeClosed(-2, 2));
        // The largest size the guard still admits, checked without allocating it: one below is fine, and
        // Integer.MAX_VALUE + 1 elements is what the assertions above reject.
        Assertions.assertEquals(1_000_000, Array.range(0, 1_000_000).length);
        Assertions.assertEquals(1_000_000, Array.rangeClosed(1, 1_000_000).length);
    }

    @Test
    public void testMatrixShapeMessagesNameTheOffendingRow() {
        final IllegalArgumentException nullRow = Assertions.assertThrows(IllegalArgumentException.class, () -> Array.transpose(new int[][] { { 1 }, null }));
        Assertions.assertTrue(nullRow.getMessage().contains("index 1"), nullRow.getMessage());

        final IllegalArgumentException firstRowNull = Assertions.assertThrows(IllegalArgumentException.class,
                () -> Array.transpose(new int[][] { null, { 1 } }));
        Assertions.assertTrue(firstRowNull.getMessage().contains("index 0"), firstRowNull.getMessage());

        final IllegalArgumentException ragged = Assertions.assertThrows(IllegalArgumentException.class, () -> Array.transpose(new int[][] { { 1, 2 }, { 3 } }));
        Assertions.assertTrue(ragged.getMessage().contains("index 1"), ragged.getMessage());
        Assertions.assertTrue(ragged.getMessage().contains("length 1"), ragged.getMessage());
        Assertions.assertTrue(ragged.getMessage().contains("length 2"), ragged.getMessage());
    }

    @Test
    public void testTransposeStillWorks() {
        Assertions.assertArrayEquals(new int[][] { { 1, 4 }, { 2, 5 }, { 3, 6 } }, Array.transpose(new int[][] { { 1, 2, 3 }, { 4, 5, 6 } }));
        Assertions.assertArrayEquals(new int[0][], Array.transpose(new int[0][]));
        Assertions.assertNull(Array.transpose((int[][]) null));
        Assertions.assertArrayEquals(new String[][] { { "A", "D" }, { "B", "E" } }, Array.transpose(new String[][] { { "A", "B" }, { "D", "E" } }));
    }

    // ============================================================ box/unbox: null rows are propagated

    /**
     * The {@code @return} clause added by this pass claims null-row propagation on all 48 multi-dimensional
     * {@code box}/{@code unbox} overloads, so check all 48 rather than a hand-picked pair.
     */
    @Test
    public void testEveryMultiDimensionalBoxUnboxOverloadPropagatesNullRows() throws Exception {
        final Class<?>[] prims = { boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class };
        final Class<?>[] wraps = { Boolean.class, Character.class, Byte.class, Short.class, Integer.class, Long.class, Float.class, Double.class };
        int checked = 0;

        for (int t = 0; t < prims.length; t++) {
            for (int dims = 2; dims <= 3; dims++) {
                final int[] shape = dims == 2 ? new int[] { 2, 1 } : new int[] { 2, 1, 1 };

                final Object prim = java.lang.reflect.Array.newInstance(prims[t], shape);
                java.lang.reflect.Array.set(prim, 1, null);
                assertNullRowPreserved("box " + prims[t].getSimpleName() + " " + dims + "D", Array.class.getMethod("box", prim.getClass()).invoke(null, prim));

                final Object wrap = java.lang.reflect.Array.newInstance(wraps[t], shape);
                java.lang.reflect.Array.set(wrap, 1, null);
                assertNullRowPreserved("unbox " + wraps[t].getSimpleName() + " " + dims + "D",
                        Array.class.getMethod("unbox", wrap.getClass()).invoke(null, wrap));

                final Object defaultValue = java.lang.reflect.Array.get(java.lang.reflect.Array.newInstance(prims[t], 1), 0);
                assertNullRowPreserved("unbox+default " + wraps[t].getSimpleName() + " " + dims + "D",
                        Array.class.getMethod("unbox", wrap.getClass(), prims[t]).invoke(null, wrap, defaultValue));

                checked += 3;
            }
        }

        Assertions.assertEquals(48, checked);
    }

    private static void assertNullRowPreserved(final String label, final Object result) {
        Assertions.assertNotNull(result, label);
        Assertions.assertEquals(2, java.lang.reflect.Array.getLength(result), label);
        Assertions.assertNotNull(java.lang.reflect.Array.get(result, 0), label + ": a non-null row must survive");
        Assertions.assertNull(java.lang.reflect.Array.get(result, 1), label + ": a null row must stay null");
    }

    @Test
    public void testBoxAndUnboxPropagateNullRowsAsDocumented() {
        Assertions.assertArrayEquals(new Boolean[][] { { Boolean.TRUE }, null }, Array.box(new boolean[][] { { true }, null }));
        Assertions.assertArrayEquals(new boolean[][] { { true }, null }, Array.unbox(new Boolean[][] { { Boolean.TRUE }, null }));
        Assertions.assertArrayEquals(new int[][] { { 1, -1 }, null }, Array.unbox(new Integer[][] { { 1, null }, null }, -1));
        Assertions.assertArrayEquals(new Integer[][][] { { { 1 }, null }, null }, Array.box(new int[][][] { { { 1 }, null }, null }));
        Assertions.assertArrayEquals(new int[][][] { { { 1 }, null }, null }, Array.unbox(new Integer[][][] { { { 1 }, null }, null }));
        Assertions.assertEquals(Arrays.deepToString(new Object[] { null }), Arrays.deepToString(Array.box(new int[][] { null })));
    }
}
