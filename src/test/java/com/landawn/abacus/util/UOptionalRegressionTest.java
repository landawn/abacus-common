package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Locale;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.u.OptionalBoolean;
import com.landawn.abacus.util.u.OptionalByte;
import com.landawn.abacus.util.u.OptionalChar;
import com.landawn.abacus.util.u.OptionalDouble;
import com.landawn.abacus.util.u.OptionalFloat;
import com.landawn.abacus.util.u.OptionalInt;
import com.landawn.abacus.util.u.OptionalLong;
import com.landawn.abacus.util.u.OptionalShort;

/**
 * Cycle-1 regression tests for the {@code u.java} Optional/Nullable family
 * (ledger {@code scripts/cross_review/u_OptionalNullable_ledger_2026-09-02.md}).
 *
 * <ul>
 *   <li><b>C-010</b> — {@code Nullable.mapToNonNull}/{@code mapToNonNullIfNotNull} raised a
 *       message-less {@code NullPointerException} when the mapper returned {@code null}, because they
 *       went through {@code Optional.of(..)}'s bare {@code Objects.requireNonNull}.</li>
 *   <li><b>C-011</b> — ten {@code Nullable} methods invoke their callback with a present {@code null}
 *       and did not document it. The fix is javadoc; these tests pin the behaviour the new wording
 *       describes so the two cannot drift.</li>
 *   <li><b>C-012</b> — {@code orElseThrow(Supplier)} throws NPE when the supplier returns
 *       {@code null}; now documented on all eleven methods.</li>
 *   <li><b>C-013</b> — {@code toString()} moved off {@code String.format}; these tests prove the
 *       output is byte-identical to what {@code String.format} produced, including under a
 *       non-Latin default locale.</li>
 * </ul>
 */
public class UOptionalRegressionTest extends TestBase {

    private static final String MAPPER_MSG = "The mapping function must not return null";

    // ---------------------------------------------------------------- C-010

    @Test
    @DisplayName("C-010: mapToNonNull/mapToNonNullIfNotNull name what was null and stay NPE, not IAE")
    public void c010_mapToNonNullNullResult_hasMessage() {
        // The javadoc has always promised NPE here; only the message was missing. Guard the type so a
        // future "make it consistent with flatMap's IAE" refactor cannot silently change the contract.
        assertEquals(MAPPER_MSG, assertThrows(NullPointerException.class, () -> Nullable.of("x").mapToNonNull(v -> null)).getMessage());
        assertEquals(MAPPER_MSG, assertThrows(NullPointerException.class, () -> Nullable.of("x").mapToNonNullIfNotNull(v -> null)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> Nullable.of("x").flatMap(v -> null));
    }

    @Test
    @DisplayName("C-010: a non-null mapper result is unaffected, and the callback is skipped when it must be")
    public void c010_happyPathUnchanged() {
        assertEquals(Optional.of("X"), Nullable.of("x").mapToNonNull(String::toUpperCase));
        assertEquals(Optional.of("X"), Nullable.of("x").mapToNonNullIfNotNull(String::toUpperCase));
        assertTrue(Nullable.<String> empty().mapToNonNull(v -> null).isEmpty());
        assertTrue(Nullable.of((String) null).mapToNonNullIfNotNull(v -> null).isEmpty());
    }

    // ---------------------------------------------------------------- C-011

    @Test
    @DisplayName("C-011: all ten documented methods really do invoke the callback with a present null")
    public void c011_presentNullReachesTheCallback() {
        final Nullable<Integer> n = Nullable.of((Integer) null);

        // the 8 primitive mapToXxx — a natural unboxing lambda throws, exactly as the javadoc now warns
        assertThrows(NullPointerException.class, () -> n.mapToBoolean(v -> v > 0));
        assertThrows(NullPointerException.class, () -> n.mapToChar(v -> (char) (int) v));
        assertThrows(NullPointerException.class, () -> n.mapToByte(Integer::byteValue));
        assertThrows(NullPointerException.class, () -> n.mapToShort(Integer::shortValue));
        assertThrows(NullPointerException.class, () -> n.mapToInt(v -> v));
        assertThrows(NullPointerException.class, () -> n.mapToLong(v -> v));
        assertThrows(NullPointerException.class, () -> n.mapToFloat(v -> v));
        assertThrows(NullPointerException.class, () -> n.mapToDouble(v -> v));
        // ... and the two object-valued ones
        assertThrows(NullPointerException.class, () -> n.mapToNonNull(Object::toString));
        assertThrows(NullPointerException.class, () -> n.flatMap(v -> Nullable.of(v.toString())));

        // a null-tolerant callback is invoked and works
        assertEquals(OptionalInt.of(-1), n.mapToInt(v -> v == null ? -1 : v));
    }

    @Test
    @DisplayName("C-011: every *IfNotNull twin skips the callback for a present null")
    public void c011_ifNotNullTwinsSkipTheCallback() {
        final Nullable<Integer> n = Nullable.of((Integer) null);
        final int[] calls = { 0 };

        assertTrue(n.mapToBooleanIfNotNull(v -> {
            calls[0]++;
            return v > 0;
        }).isEmpty());
        assertTrue(n.mapToCharIfNotNull(v -> {
            calls[0]++;
            return (char) (int) v;
        }).isEmpty());
        assertTrue(n.mapToByteIfNotNull(v -> {
            calls[0]++;
            return v.byteValue();
        }).isEmpty());
        assertTrue(n.mapToShortIfNotNull(v -> {
            calls[0]++;
            return v.shortValue();
        }).isEmpty());
        assertTrue(n.mapToIntIfNotNull(v -> {
            calls[0]++;
            return v;
        }).isEmpty());
        assertTrue(n.mapToLongIfNotNull(v -> {
            calls[0]++;
            return v;
        }).isEmpty());
        assertTrue(n.mapToFloatIfNotNull(v -> {
            calls[0]++;
            return v;
        }).isEmpty());
        assertTrue(n.mapToDoubleIfNotNull(v -> {
            calls[0]++;
            return v;
        }).isEmpty());
        assertTrue(n.mapToNonNullIfNotNull(v -> {
            calls[0]++;
            return v.toString();
        }).isEmpty());
        assertTrue(n.flatMapIfNotNull(v -> {
            calls[0]++;
            return Nullable.of(v.toString());
        }).isNotPresent());

        assertEquals(0, calls[0], "no *IfNotNull callback should have run for a present null");
    }

    // ---------------------------------------------------------------- C-012

    @Test
    @DisplayName("C-012: orElseThrow(supplier) throws NPE when the supplier returns null, on all ten classes")
    public void c012_orElseThrowNullSupplierResult() {
        assertThrows(NullPointerException.class, () -> OptionalBoolean.empty().orElseThrow(() -> null));
        assertThrows(NullPointerException.class, () -> OptionalChar.empty().orElseThrow(() -> null));
        assertThrows(NullPointerException.class, () -> OptionalByte.empty().orElseThrow(() -> null));
        assertThrows(NullPointerException.class, () -> OptionalShort.empty().orElseThrow(() -> null));
        assertThrows(NullPointerException.class, () -> OptionalInt.empty().orElseThrow(() -> null));
        assertThrows(NullPointerException.class, () -> OptionalLong.empty().orElseThrow(() -> null));
        assertThrows(NullPointerException.class, () -> OptionalFloat.empty().orElseThrow(() -> null));
        assertThrows(NullPointerException.class, () -> OptionalDouble.empty().orElseThrow(() -> null));
        assertThrows(NullPointerException.class, () -> Optional.empty().orElseThrow(() -> null));
        assertThrows(NullPointerException.class, () -> Nullable.empty().orElseThrow(() -> null));
        // the IfNull variant too
        assertThrows(NullPointerException.class, () -> Nullable.of((String) null).orElseThrowIfNull(() -> null));

        // a present value never consults the supplier, so a null-returning one is harmless
        assertEquals(1, OptionalInt.of(1).orElseThrow(() -> null));
        assertEquals("x", Optional.of("x").orElseThrow(() -> null));
    }

    // ---------------------------------------------------------------- C-013

    /** Exactly what {@code toString()} used to build, kept here as the differential oracle. */
    private static String viaFormat(String cls, Object value) {
        return String.format(cls + "[%s]", value);
    }

    @Test
    @DisplayName("C-013: toString() is byte-identical to the String.format it replaced")
    public void c013_toStringMatchesFormat() {
        for (final boolean b : new boolean[] { true, false }) {
            assertEquals(viaFormat("OptionalBoolean", b), OptionalBoolean.of(b).toString());
        }
        for (final char c : new char[] { 0, 'A', 'z', '\n', '\t', ' ', 128, 255, 'é', '中', '\uD83D', '\uDE00', Character.MAX_VALUE }) {
            assertEquals(viaFormat("OptionalChar", c), OptionalChar.of(c).toString());
        }
        for (final byte v : new byte[] { Byte.MIN_VALUE, -1, 0, 1, Byte.MAX_VALUE }) {
            assertEquals(viaFormat("OptionalByte", v), OptionalByte.of(v).toString());
        }
        for (final short v : new short[] { Short.MIN_VALUE, -1, 0, 1, Short.MAX_VALUE }) {
            assertEquals(viaFormat("OptionalShort", v), OptionalShort.of(v).toString());
        }
        for (final int v : new int[] { Integer.MIN_VALUE, -1000000, -1, 0, 1, 1000000, Integer.MAX_VALUE }) {
            assertEquals(viaFormat("OptionalInt", v), OptionalInt.of(v).toString());
        }
        for (final long v : new long[] { Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE }) {
            assertEquals(viaFormat("OptionalLong", v), OptionalLong.of(v).toString());
        }
        for (final float v : new float[] { Float.NEGATIVE_INFINITY, -Float.MAX_VALUE, -1.5f, -0.0f, 0.0f, Float.MIN_VALUE, 1.0e-7f, 1.5f, Float.MAX_VALUE,
                Float.POSITIVE_INFINITY, Float.NaN }) {
            assertEquals(viaFormat("OptionalFloat", v), OptionalFloat.of(v).toString());
        }
        for (final double v : new double[] { Double.NEGATIVE_INFINITY, -Double.MAX_VALUE, -1.5d, -0.0d, 0.0d, Double.MIN_VALUE, 1.0e-7d, 1.5d, Double.MAX_VALUE,
                Double.POSITIVE_INFINITY, Double.NaN }) {
            assertEquals(viaFormat("OptionalDouble", v), OptionalDouble.of(v).toString());
        }
        for (final String s : new String[] { "", "x", "null", "a b", "é中", "😀", "%s", "%d%%" }) {
            assertEquals(viaFormat("Optional", CommonUtil.toString(s)), Optional.of(s).toString());
            assertEquals(viaFormat("Nullable", CommonUtil.toString(s)), Nullable.of(s).toString());
        }
    }

    @Test
    @DisplayName("C-013: empty and present-null renderings are unchanged")
    public void c013_toStringEdgeStates() {
        assertEquals("OptionalBoolean.empty", OptionalBoolean.empty().toString());
        assertEquals("OptionalChar.empty", OptionalChar.empty().toString());
        assertEquals("OptionalByte.empty", OptionalByte.empty().toString());
        assertEquals("OptionalShort.empty", OptionalShort.empty().toString());
        assertEquals("OptionalInt.empty", OptionalInt.empty().toString());
        assertEquals("OptionalLong.empty", OptionalLong.empty().toString());
        assertEquals("OptionalFloat.empty", OptionalFloat.empty().toString());
        assertEquals("OptionalDouble.empty", OptionalDouble.empty().toString());
        assertEquals("Optional.empty", Optional.empty().toString());
        assertEquals("Nullable.empty", Nullable.empty().toString());
        assertEquals("Nullable[null]", Nullable.of((String) null).toString());
        assertEquals("OptionalInt[42]", OptionalInt.of(42).toString());
        assertEquals("Nullable[x]", Nullable.of("x").toString());
    }

    @Test
    @DisplayName("C-013: a '%' in the value is no longer a format specifier")
    public void c013_percentInValueIsLiteral() {
        // The old code passed the value as a format ARGUMENT, so this always worked; the point of the
        // test is that concatenation cannot regress it into a format STRING.
        assertEquals("Optional[%s]", Optional.of("%s").toString());
        assertEquals("Nullable[100%]", Nullable.of("100%").toString());
        assertEquals("Optional[%d %n %%]", Optional.of("%d %n %%").toString());
    }

    @Test
    @DisplayName("C-013: rendering does not depend on the default locale")
    public void c013_localeIndependent() {
        final Locale original = Locale.getDefault();
        try {
            for (final Locale l : new Locale[] { Locale.US, Locale.GERMANY, Locale.forLanguageTag("ar-EG"), Locale.forLanguageTag("hi-IN-u-nu-deva"),
                    Locale.forLanguageTag("th-TH-u-nu-thai") }) {
                Locale.setDefault(l);
                assertEquals("OptionalInt[1234567]", OptionalInt.of(1234567).toString(), "locale " + l);
                assertEquals("OptionalDouble[1234.5]", OptionalDouble.of(1234.5d).toString(), "locale " + l);
                assertEquals("OptionalFloat[-0.0]", OptionalFloat.of(-0.0f).toString(), "locale " + l);
                assertEquals("OptionalLong[-9223372036854775808]", OptionalLong.of(Long.MIN_VALUE).toString(), "locale " + l);
                assertFalse(OptionalDouble.of(Double.NaN).toString().isEmpty(), "locale " + l);
            }
        } finally {
            Locale.setDefault(original);
        }
    }
}
