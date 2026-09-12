package com.landawn.abacus.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class JavaVersionTest extends TestBase {

    @Test
    public void testAtLeast() {
        assertTrue(JavaVersion.JAVA_1_8.atLeast(JavaVersion.JAVA_1_8));

        assertTrue(JavaVersion.JAVA_11.atLeast(JavaVersion.JAVA_1_8));
        assertTrue(JavaVersion.JAVA_17.atLeast(JavaVersion.JAVA_11));
        assertTrue(JavaVersion.JAVA_21.atLeast(JavaVersion.JAVA_9));

        assertFalse(JavaVersion.JAVA_1_8.atLeast(JavaVersion.JAVA_11));
        assertFalse(JavaVersion.JAVA_9.atLeast(JavaVersion.JAVA_17));
        assertFalse(JavaVersion.JAVA_1_7.atLeast(JavaVersion.JAVA_1_8));

        assertTrue(JavaVersion.JAVA_RECENT.atLeast(JavaVersion.JAVA_1_1));
        assertFalse(JavaVersion.JAVA_ANDROID_0_9.atLeast(JavaVersion.JAVA_1_6));
        assertTrue(JavaVersion.JAVA_ANDROID_0_9.atLeast(JavaVersion.JAVA_ANDROID_0_9));

        assertTrue(JavaVersion.JAVA_10.atLeast(JavaVersion.JAVA_9));
        assertFalse(JavaVersion.JAVA_9.atLeast(JavaVersion.JAVA_10));
    }

    @Test
    public void testAtMost() {
        assertTrue(JavaVersion.JAVA_1_8.atMost(JavaVersion.JAVA_1_8));

        assertTrue(JavaVersion.JAVA_1_8.atMost(JavaVersion.JAVA_11));
        assertTrue(JavaVersion.JAVA_11.atMost(JavaVersion.JAVA_17));
        assertTrue(JavaVersion.JAVA_9.atMost(JavaVersion.JAVA_21));

        assertFalse(JavaVersion.JAVA_11.atMost(JavaVersion.JAVA_1_8));
        assertFalse(JavaVersion.JAVA_17.atMost(JavaVersion.JAVA_9));
        assertFalse(JavaVersion.JAVA_1_8.atMost(JavaVersion.JAVA_1_7));

        assertTrue(JavaVersion.JAVA_1_1.atMost(JavaVersion.JAVA_RECENT));
        assertTrue(JavaVersion.JAVA_ANDROID_0_9.atMost(JavaVersion.JAVA_1_5));
        assertTrue(JavaVersion.JAVA_11.atMost(JavaVersion.JAVA_RECENT));

        assertTrue(JavaVersion.JAVA_9.atMost(JavaVersion.JAVA_10));
        assertFalse(JavaVersion.JAVA_10.atMost(JavaVersion.JAVA_9));
    }

    @Test
    public void testVersionComparisons() {
        assertTrue(JavaVersion.JAVA_ANDROID_0_9.atMost(JavaVersion.JAVA_1_6));
        assertTrue(JavaVersion.JAVA_1_6.atLeast(JavaVersion.JAVA_ANDROID_0_9));

        assertTrue(JavaVersion.JAVA_ANDROID_0_9.atLeast(JavaVersion.JAVA_1_4));
        assertTrue(JavaVersion.JAVA_ANDROID_0_9.atMost(JavaVersion.JAVA_1_5));
        assertTrue(JavaVersion.JAVA_ANDROID_0_9.atMost(JavaVersion.JAVA_1_6));

        JavaVersion prev = JavaVersion.JAVA_1_1;
        for (JavaVersion v : JavaVersion.values()) {
            if (v != JavaVersion.JAVA_ANDROID_0_9 && v.atMost(JavaVersion.JAVA_RECENT)) {
                N.println(prev + ", " + v);
                assertTrue(v.atLeast(prev));
                assertTrue(prev.atMost(v));
                prev = v;
            }
        }
    }

    @Test
    public void testGetJavaVersion() {

        assertThrows(IllegalArgumentException.class, () -> JavaVersion.getJavaVersion(null));

        assertEquals(JavaVersion.JAVA_1_8, JavaVersion.getJavaVersion("1.8"));
        assertEquals(JavaVersion.JAVA_11, JavaVersion.getJavaVersion("11"));
        assertEquals(JavaVersion.JAVA_17, JavaVersion.getJavaVersion("17"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.getJavaVersion("50"));

        assertThrows(IllegalArgumentException.class, () -> JavaVersion.getJavaVersion("invalid"));
    }

    @Test
    public void testGet() {
        assertEquals(JavaVersion.JAVA_ANDROID_0_9, JavaVersion.get("0.9"));
        assertEquals(JavaVersion.JAVA_1_1, JavaVersion.get("1.1"));
        assertEquals(JavaVersion.JAVA_1_2, JavaVersion.get("1.2"));
        assertEquals(JavaVersion.JAVA_1_3, JavaVersion.get("1.3"));
        assertEquals(JavaVersion.JAVA_1_4, JavaVersion.get("1.4"));
        assertEquals(JavaVersion.JAVA_1_5, JavaVersion.get("1.5"));
        assertEquals(JavaVersion.JAVA_1_5, JavaVersion.get("5"));
        assertEquals(JavaVersion.JAVA_1_6, JavaVersion.get("1.6"));
        assertEquals(JavaVersion.JAVA_1_6, JavaVersion.get("6"));
        assertEquals(JavaVersion.JAVA_1_7, JavaVersion.get("1.7"));
        assertEquals(JavaVersion.JAVA_1_7, JavaVersion.get("7"));
        assertEquals(JavaVersion.JAVA_1_8, JavaVersion.get("1.8"));
        assertEquals(JavaVersion.JAVA_1_8, JavaVersion.get("8"));

        for (int i = 9; i <= 39; i++) {
            JavaVersion expected = JavaVersion.valueOf("JAVA_" + i);
            assertEquals(expected, JavaVersion.get(String.valueOf(i)));
        }

        assertEquals(JavaVersion.JAVA_1_8, JavaVersion.get("1.8.0_271"));
        assertEquals(JavaVersion.JAVA_ANDROID_0_9, JavaVersion.get("0.9.1"));
        assertEquals(JavaVersion.JAVA_11, JavaVersion.get("11.0.2"));
        assertEquals(JavaVersion.JAVA_17, JavaVersion.get("17.0.1.12"));

        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.get("40"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.get("50"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.get("100"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.get("999"));

        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.get("40.0.1"));
    }

    @Test
    public void testGetWithInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get(null));

        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get(""));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get("abc"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get("1.9"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get("2.0"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get("-1"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get("1.10"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get("3.0"));

        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get(".."));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get("1..8"));
    }

    @Test
    public void testOf() {
        assertEquals(JavaVersion.JAVA_1_1, JavaVersion.of("1.1"));
        assertEquals(JavaVersion.JAVA_1_2, JavaVersion.of("1.2"));
        assertEquals(JavaVersion.JAVA_1_3, JavaVersion.of("1.3"));
        assertEquals(JavaVersion.JAVA_1_4, JavaVersion.of("1.4"));
        assertEquals(JavaVersion.JAVA_1_5, JavaVersion.of("1.5"));
        assertEquals(JavaVersion.JAVA_1_6, JavaVersion.of("1.6"));
        assertEquals(JavaVersion.JAVA_1_7, JavaVersion.of("1.7"));
        assertEquals(JavaVersion.JAVA_1_8, JavaVersion.of("1.8"));

        assertEquals(JavaVersion.JAVA_ANDROID_0_9, JavaVersion.of("0.9"));

        assertEquals(JavaVersion.JAVA_9, JavaVersion.of("9"));
        assertEquals(JavaVersion.JAVA_10, JavaVersion.of("10"));
        assertEquals(JavaVersion.JAVA_11, JavaVersion.of("11"));
        assertEquals(JavaVersion.JAVA_17, JavaVersion.of("17"));
        assertEquals(JavaVersion.JAVA_21, JavaVersion.of("21"));
        assertEquals(JavaVersion.JAVA_25, JavaVersion.of("25"));
        assertEquals(JavaVersion.JAVA_39, JavaVersion.of("39"));

        assertEquals(JavaVersion.JAVA_1_8, JavaVersion.of("1.8.0_271"));
        assertEquals(JavaVersion.JAVA_11, JavaVersion.of("11.0.2"));
        assertEquals(JavaVersion.JAVA_17, JavaVersion.of("17.0.1"));
        assertEquals(JavaVersion.JAVA_21, JavaVersion.of("21.0.0"));

        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.of("40"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.of("50"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.of("100"));

        // JAVA_RECENT must compare as newer than the last named constant, even on older JVMs.
        assertTrue(JavaVersion.of("50").atLeast(JavaVersion.JAVA_39));
        assertTrue(JavaVersion.JAVA_39.atMost(JavaVersion.JAVA_RECENT));

        assertEquals(JavaVersion.JAVA_1_5, JavaVersion.of("5"));
        assertEquals(JavaVersion.JAVA_1_6, JavaVersion.of("6"));
        assertEquals(JavaVersion.JAVA_1_7, JavaVersion.of("7"));
        assertEquals(JavaVersion.JAVA_1_8, JavaVersion.of("8"));
    }

    @Test
    public void testOfWithInvalidInput() {
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of(null));

        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of(""));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("invalid"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("1.9"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("2.0"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("abc"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("-1"));
    }

    @Test
    public void testOf_shortLeadingDotPrefix_throwsIAE_notIOOBE() {
        // Regression: previously JavaVersion.of("1.") and JavaVersion.of("0.") threw
        // StringIndexOutOfBoundsException because substring(0,3) was called on a length-2 input.
        // After the fix these inputs should throw IllegalArgumentException (or its subclass).
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("1."));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("0."));
    }

    @Test
    public void testJavaRecent_MaxVersionFallback() throws Exception {
        final Method maxVersionMethod = JavaVersion.class.getDeclaredMethod("maxVersion");
        maxVersionMethod.setAccessible(true);

        final String previousVersion = System.getProperty("java.specification.version");

        try {
            System.setProperty("java.specification.version", "0");
            org.junit.jupiter.api.Assertions.assertEquals(99F, (float) maxVersionMethod.invoke(null), 0.0001F);

            System.setProperty("java.specification.version", "21");
            org.junit.jupiter.api.Assertions.assertEquals(21F, (float) maxVersionMethod.invoke(null), 0.0001F);
        } finally {
            if (previousVersion == null) {
                System.clearProperty("java.specification.version");
            } else {
                System.setProperty("java.specification.version", previousVersion);
            }
        }
    }

    @Test
    public void testToString() {
        assertEquals("0.9", JavaVersion.JAVA_ANDROID_0_9.toString());
        assertEquals("1.1", JavaVersion.JAVA_1_1.toString());
        assertEquals("1.2", JavaVersion.JAVA_1_2.toString());
        assertEquals("1.3", JavaVersion.JAVA_1_3.toString());
        assertEquals("1.4", JavaVersion.JAVA_1_4.toString());
        assertEquals("1.5", JavaVersion.JAVA_1_5.toString());
        assertEquals("1.6", JavaVersion.JAVA_1_6.toString());
        assertEquals("1.7", JavaVersion.JAVA_1_7.toString());
        assertEquals("1.8", JavaVersion.JAVA_1_8.toString());

        assertEquals("9", JavaVersion.JAVA_9.toString());
        assertEquals("10", JavaVersion.JAVA_10.toString());
        assertEquals("11", JavaVersion.JAVA_11.toString());
        assertEquals("12", JavaVersion.JAVA_12.toString());
        assertEquals("13", JavaVersion.JAVA_13.toString());
        assertEquals("14", JavaVersion.JAVA_14.toString());
        assertEquals("15", JavaVersion.JAVA_15.toString());
        assertEquals("16", JavaVersion.JAVA_16.toString());
        assertEquals("17", JavaVersion.JAVA_17.toString());
        assertEquals("18", JavaVersion.JAVA_18.toString());
        assertEquals("19", JavaVersion.JAVA_19.toString());
        assertEquals("20", JavaVersion.JAVA_20.toString());
        assertEquals("21", JavaVersion.JAVA_21.toString());
        assertEquals("22", JavaVersion.JAVA_22.toString());
        assertEquals("23", JavaVersion.JAVA_23.toString());
        assertEquals("24", JavaVersion.JAVA_24.toString());
        assertEquals("25", JavaVersion.JAVA_25.toString());
        assertEquals("26", JavaVersion.JAVA_26.toString());
        assertEquals("27", JavaVersion.JAVA_27.toString());
        assertEquals("28", JavaVersion.JAVA_28.toString());
        assertEquals("29", JavaVersion.JAVA_29.toString());
        assertEquals("30", JavaVersion.JAVA_30.toString());
        assertEquals("31", JavaVersion.JAVA_31.toString());
        assertEquals("32", JavaVersion.JAVA_32.toString());
        assertEquals("33", JavaVersion.JAVA_33.toString());
        assertEquals("34", JavaVersion.JAVA_34.toString());
        assertEquals("35", JavaVersion.JAVA_35.toString());
        assertEquals("36", JavaVersion.JAVA_36.toString());
        assertEquals("37", JavaVersion.JAVA_37.toString());
        assertEquals("38", JavaVersion.JAVA_38.toString());
        assertEquals("39", JavaVersion.JAVA_39.toString());

        assertNotNull(JavaVersion.JAVA_RECENT.toString());
        assertTrue(JavaVersion.JAVA_RECENT.toString().matches("\\d+(\\.\\d+)?"));
    }

    @Test
    public void testJavaRecentBehavior() {

        String recentStr = JavaVersion.JAVA_RECENT.toString();
        assertNotNull(recentStr);
        assertFalse(recentStr.isEmpty());
    }

    @Test
    public void testEnumValues() {
        JavaVersion[] values = JavaVersion.values();
        assertEquals(41, values.length);

        assertEquals(JavaVersion.JAVA_ANDROID_0_9, values[0]);
        assertEquals(JavaVersion.JAVA_39, values[39]);
        assertEquals(JavaVersion.JAVA_RECENT, values[40]);

        assertEquals(JavaVersion.JAVA_1_8, JavaVersion.valueOf("JAVA_1_8"));
        assertEquals(JavaVersion.JAVA_11, JavaVersion.valueOf("JAVA_11"));
        assertEquals(JavaVersion.JAVA_17, JavaVersion.valueOf("JAVA_17"));
        assertEquals(JavaVersion.JAVA_21, JavaVersion.valueOf("JAVA_21"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.valueOf("JAVA_RECENT"));
    }

    /**
     * {@code of}/{@code get} used to hand the remaining token straight to {@code Float.parseFloat}, whose grammar
     * is far wider than a version number: it trims surrounding whitespace and accepts {@code "Infinity"},
     * exponents, hex floats and {@code f}/{@code d} type suffixes. Every spelling below therefore cleared the
     * {@code > 39} test and resolved to {@link JavaVersion#JAVA_RECENT}, while the very same spellings at or below
     * 39 ({@code " 25 "}, {@code "25f"}) were rejected - a self-inconsistency, and a way for a vendor
     * {@code java.version} string to be silently accepted instead of falling through to the next property.
     */
    @Test
    public void testOfRejectsNonVersionStringsThatFloatParsingWouldAccept() {
        for (final String bogus : new String[] { "Infinity", "1e9", "4e1", "40f", "40F", "40d", " 40 ", "\t40\n", "0x1p10", "40e0" }) {
            assertThrows("of(\"" + bogus + "\") must be rejected", IllegalArgumentException.class, () -> JavaVersion.of(bogus));
            assertThrows("get(\"" + bogus + "\") must be rejected", IllegalArgumentException.class, () -> JavaVersion.get(bogus));
        }

        // The rejection reports the caller's own string.
        assertEquals("Invalid Java version:  40 ", assertThrows(IllegalArgumentException.class, () -> JavaVersion.of(" 40 ")).getMessage());

        // ... and every genuine numeric version above 39 still resolves.
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.of("40"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.of("50"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.of("99"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.of("999"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.of("40.0.1"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.get("40"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.get("999"));

        // ... as does everything below 39, including the suffixed JEP 223 spellings.
        assertEquals(JavaVersion.JAVA_1_8, JavaVersion.of("1.8.0_271"));
        assertEquals(JavaVersion.JAVA_25, JavaVersion.of("25-ea"));
        assertEquals(JavaVersion.JAVA_21, JavaVersion.of("21+35"));
        assertEquals(JavaVersion.JAVA_17, JavaVersion.of("17.0.9+9"));
    }

    /**
     * Pins what "a run of ASCII decimal digits" in {@code of}'s javadoc means, and the matching comment in
     * {@code get}: the guard is {@link Strings#isAsciiNumeric(CharSequence)}, NOT
     * {@link Strings#isNumeric(CharSequence)} ({@link Character#isDigit(int)}), so a decimal digit from another
     * script is stopped BY the guard.
     *
     * <p>It used to pass the guard and be rejected only incidentally, one step later: {@code Numbers.toFloat}
     * raises {@link NumberFormatException} for a token {@code Float.parseFloat} cannot read and
     * {@code toFloatVersion} rethrows that as {@link IllegalArgumentException}, so the {@code > 39} test named in
     * the old comment was never reached at all. The exception TYPE is the same either way, which is why the cause
     * is asserted here: the guard reports without one, a parse failure carries the {@code NumberFormatException}.
     */
    @Test
    public void testNonAsciiDecimalDigitsAreRejectedByTheNumericGuard() {
        // Arabic-Indic "40", Devanagari "20", fullwidth "40" - written as escapes to keep this file ASCII.
        for (final String digits : new String[] { "\u0664\u0660", "\u0968\u0966", "\uff14\uff10" }) {
            assertTrue("Strings.isNumeric accepts Unicode decimal digits: " + digits, Strings.isNumeric(digits));
            assertFalse("... but they are not ASCII digits, which is what the guard tests: " + digits, Strings.isAsciiNumeric(digits));

            final IllegalArgumentException fromOf = assertThrows("of(\"" + digits + "\") must be rejected", IllegalArgumentException.class,
                    () -> JavaVersion.of(digits));
            assertEquals("Invalid Java version: " + digits, fromOf.getMessage());
            assertNull("rejected by the guard, so there is no parse failure to report as a cause", fromOf.getCause());

            final IllegalArgumentException fromGet = assertThrows("get(\"" + digits + "\") must be rejected", IllegalArgumentException.class,
                    () -> JavaVersion.get(digits));
            assertEquals("Invalid Java version: " + digits, fromGet.getMessage());
            assertNull("rejected by the guard, so there is no parse failure to report as a cause", fromGet.getCause());
        }
    }

    /**
     * {@link JavaVersion#JAVA_RECENT}'s display name is the current {@code java.specification.version} rendered as
     * a {@code float} ({@code "25.0"} on a Java 25 JVM), not the raw property value - and it comes from the
     * <em>spec</em> version, not from the comparison value, which is what proves the two-argument constructor is
     * the one in use.
     */
    @Test
    public void testJavaRecentDisplayNameIsTheSpecVersionRenderedAsAFloat() {
        final String spec = System.getProperty("java.specification.version");
        assertNotNull(spec);

        final float specValue = Float.parseFloat(spec);

        assertEquals(Float.toString(specValue), JavaVersion.JAVA_RECENT.toString());
        assertTrue("a float rendering always carries a dot", JavaVersion.JAVA_RECENT.toString().indexOf('.') > 0);

        // Had JAVA_RECENT been built by the single-argument constructor its name would have been derived from the
        // COMPARISON value instead, which is floored at 40.
        if (specValue < 40.0f) {
            assertNotEquals(Float.toString(Math.max(specValue, 40.0f)), JavaVersion.JAVA_RECENT.toString());
        }

        // The rendered name is a version string in its own right, so it parses - but not back to JAVA_RECENT on a
        // JVM below 40.
        assertEquals(JavaVersion.of(spec), JavaVersion.of(JavaVersion.JAVA_RECENT.toString()));
    }

    /**
     * {@code JAVA_ANDROID_0_9} carries the value {@code 1.5f} but is declared first, so the enum's natural order
     * disagrees with {@code atLeast}/{@code atMost}, and it ties with {@code JAVA_1_5} so neither is a strict
     * predecessor of the other. Both facts are now called out in the class javadoc.
     */
    @Test
    public void testAndroidConstantBreaksTheNaturalEnumOrder() {
        assertTrue(JavaVersion.JAVA_ANDROID_0_9.compareTo(JavaVersion.JAVA_1_1) < 0);
        assertTrue(JavaVersion.JAVA_ANDROID_0_9.atLeast(JavaVersion.JAVA_1_1));

        assertTrue(JavaVersion.JAVA_ANDROID_0_9.atLeast(JavaVersion.JAVA_1_5));
        assertTrue(JavaVersion.JAVA_ANDROID_0_9.atMost(JavaVersion.JAVA_1_5));
        assertTrue(JavaVersion.JAVA_1_5.atLeast(JavaVersion.JAVA_ANDROID_0_9));
        assertTrue(JavaVersion.JAVA_1_5.atMost(JavaVersion.JAVA_ANDROID_0_9));

        int inversions = 0;
        final JavaVersion[] all = JavaVersion.values();

        for (int i = 0; i + 1 < all.length; i++) {
            if (!all[i + 1].atLeast(all[i])) {
                assertEquals(JavaVersion.JAVA_ANDROID_0_9, all[i]);
                inversions++;
            }
        }

        assertEquals("JAVA_ANDROID_0_9 is the only declaration-order inversion", 1, inversions);
    }

}
