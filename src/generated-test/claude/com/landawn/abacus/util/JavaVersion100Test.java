package com.landawn.abacus.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class JavaVersion100Test extends TestBase {

    @Test
    public void testAtLeast() {
        // Test with same version
        assertTrue(JavaVersion.JAVA_1_8.atLeast(JavaVersion.JAVA_1_8));

        // Test with older version
        assertTrue(JavaVersion.JAVA_11.atLeast(JavaVersion.JAVA_1_8));
        assertTrue(JavaVersion.JAVA_17.atLeast(JavaVersion.JAVA_11));
        assertTrue(JavaVersion.JAVA_21.atLeast(JavaVersion.JAVA_9));

        // Test with newer version
        assertFalse(JavaVersion.JAVA_1_8.atLeast(JavaVersion.JAVA_11));
        assertFalse(JavaVersion.JAVA_9.atLeast(JavaVersion.JAVA_17));
        assertFalse(JavaVersion.JAVA_1_7.atLeast(JavaVersion.JAVA_1_8));

        // Test edge cases
        assertTrue(JavaVersion.JAVA_RECENT.atLeast(JavaVersion.JAVA_1_1));
        assertFalse(JavaVersion.JAVA_ANDROID_0_9.atLeast(JavaVersion.JAVA_1_6));
        assertTrue(JavaVersion.JAVA_ANDROID_0_9.atLeast(JavaVersion.JAVA_ANDROID_0_9));

        // Test with sequential versions
        assertTrue(JavaVersion.JAVA_10.atLeast(JavaVersion.JAVA_9));
        assertFalse(JavaVersion.JAVA_9.atLeast(JavaVersion.JAVA_10));
    }

    @Test
    public void testAtMost() {
        // Test with same version
        assertTrue(JavaVersion.JAVA_1_8.atMost(JavaVersion.JAVA_1_8));

        // Test with newer version
        assertTrue(JavaVersion.JAVA_1_8.atMost(JavaVersion.JAVA_11));
        assertTrue(JavaVersion.JAVA_11.atMost(JavaVersion.JAVA_17));
        assertTrue(JavaVersion.JAVA_9.atMost(JavaVersion.JAVA_21));

        // Test with older version
        assertFalse(JavaVersion.JAVA_11.atMost(JavaVersion.JAVA_1_8));
        assertFalse(JavaVersion.JAVA_17.atMost(JavaVersion.JAVA_9));
        assertFalse(JavaVersion.JAVA_1_8.atMost(JavaVersion.JAVA_1_7));

        // Test edge cases
        assertTrue(JavaVersion.JAVA_1_1.atMost(JavaVersion.JAVA_RECENT));
        assertTrue(JavaVersion.JAVA_ANDROID_0_9.atMost(JavaVersion.JAVA_1_5));
        assertTrue(JavaVersion.JAVA_11.atMost(JavaVersion.JAVA_RECENT));

        // Test with sequential versions
        assertTrue(JavaVersion.JAVA_9.atMost(JavaVersion.JAVA_10));
        assertFalse(JavaVersion.JAVA_10.atMost(JavaVersion.JAVA_9));
    }

    @Test
    public void testOf() {
        // Test legacy format (1.x)
        assertEquals(JavaVersion.JAVA_1_1, JavaVersion.of("1.1"));
        assertEquals(JavaVersion.JAVA_1_2, JavaVersion.of("1.2"));
        assertEquals(JavaVersion.JAVA_1_3, JavaVersion.of("1.3"));
        assertEquals(JavaVersion.JAVA_1_4, JavaVersion.of("1.4"));
        assertEquals(JavaVersion.JAVA_1_5, JavaVersion.of("1.5"));
        assertEquals(JavaVersion.JAVA_1_6, JavaVersion.of("1.6"));
        assertEquals(JavaVersion.JAVA_1_7, JavaVersion.of("1.7"));
        assertEquals(JavaVersion.JAVA_1_8, JavaVersion.of("1.8"));

        // Test Android version
        assertEquals(JavaVersion.JAVA_ANDROID_0_9, JavaVersion.of("0.9"));

        // Test modern format (9+)
        assertEquals(JavaVersion.JAVA_9, JavaVersion.of("9"));
        assertEquals(JavaVersion.JAVA_10, JavaVersion.of("10"));
        assertEquals(JavaVersion.JAVA_11, JavaVersion.of("11"));
        assertEquals(JavaVersion.JAVA_17, JavaVersion.of("17"));
        assertEquals(JavaVersion.JAVA_21, JavaVersion.of("21"));
        assertEquals(JavaVersion.JAVA_25, JavaVersion.of("25"));
        assertEquals(JavaVersion.JAVA_39, JavaVersion.of("39"));

        // Test with minor versions (should ignore minor part)
        assertEquals(JavaVersion.JAVA_1_8, JavaVersion.of("1.8.0_271"));
        assertEquals(JavaVersion.JAVA_11, JavaVersion.of("11.0.2"));
        assertEquals(JavaVersion.JAVA_17, JavaVersion.of("17.0.1"));
        assertEquals(JavaVersion.JAVA_21, JavaVersion.of("21.0.0"));

        // Test JAVA_RECENT for versions > 39
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.of("40"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.of("50"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.of("100"));

        // Test alternative format for Java 5-8
        assertEquals(JavaVersion.JAVA_1_5, JavaVersion.of("5"));
        assertEquals(JavaVersion.JAVA_1_6, JavaVersion.of("6"));
        assertEquals(JavaVersion.JAVA_1_7, JavaVersion.of("7"));
        assertEquals(JavaVersion.JAVA_1_8, JavaVersion.of("8"));
    }

    @Test
    public void testOfWithInvalidInput() {
        // Test null input
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of(null));

        // Test invalid version strings
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of(""));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("invalid"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("1.9"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("2.0"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("abc"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.of("-1"));
    }

    @Test
    public void testToString() {
        // Test legacy versions
        assertEquals("0.9", JavaVersion.JAVA_ANDROID_0_9.toString());
        assertEquals("1.1", JavaVersion.JAVA_1_1.toString());
        assertEquals("1.2", JavaVersion.JAVA_1_2.toString());
        assertEquals("1.3", JavaVersion.JAVA_1_3.toString());
        assertEquals("1.4", JavaVersion.JAVA_1_4.toString());
        assertEquals("1.5", JavaVersion.JAVA_1_5.toString());
        assertEquals("1.6", JavaVersion.JAVA_1_6.toString());
        assertEquals("1.7", JavaVersion.JAVA_1_7.toString());
        assertEquals("1.8", JavaVersion.JAVA_1_8.toString());

        // Test modern versions
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

        // Test JAVA_RECENT (should return the detected version as string)
        assertNotNull(JavaVersion.JAVA_RECENT.toString());
        assertTrue(JavaVersion.JAVA_RECENT.toString().matches("\\d+(\\.\\d+)?"));
    }

    @Test
    public void testGetJavaVersion() {
        // This is a static method that delegates to get(), so we test similar cases

        // Test null input
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.getJavaVersion(null));

        // Test valid versions
        assertEquals(JavaVersion.JAVA_1_8, JavaVersion.getJavaVersion("1.8"));
        assertEquals(JavaVersion.JAVA_11, JavaVersion.getJavaVersion("11"));
        assertEquals(JavaVersion.JAVA_17, JavaVersion.getJavaVersion("17"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.getJavaVersion("50"));

        // Test invalid versions
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.getJavaVersion("invalid"));
    }

    @Test
    public void testGet() {
        // Test all supported version strings
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

        // Test Java 9 through 39
        for (int i = 9; i <= 39; i++) {
            JavaVersion expected = JavaVersion.valueOf("JAVA_" + i);
            assertEquals(expected, JavaVersion.get(String.valueOf(i)));
        }

        // Test complex version strings with dots (recursive parsing)
        assertEquals(JavaVersion.JAVA_1_8, JavaVersion.get("1.8.0_271"));
        assertEquals(JavaVersion.JAVA_ANDROID_0_9, JavaVersion.get("0.9.1"));
        assertEquals(JavaVersion.JAVA_11, JavaVersion.get("11.0.2"));
        assertEquals(JavaVersion.JAVA_17, JavaVersion.get("17.0.1.12"));

        // Test versions > 39 return JAVA_RECENT
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.get("40"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.get("50"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.get("100"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.get("999"));

        // Test edge case with complex dotted version > 39
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.get("40.0.1"));
    }

    @Test
    public void testGetWithInvalidInput() {
        // Test null
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get(null));

        // Test invalid strings
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get(""));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get("abc"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get("1.9"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get("2.0"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get("-1"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get("1.10"));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get("3.0"));

        // Test malformed version strings
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get(".."));
        assertThrows(IllegalArgumentException.class, () -> JavaVersion.get("1..8"));
    }

    @Test
    public void testEnumValues() {
        // Test that all enum values are accessible
        JavaVersion[] values = JavaVersion.values();
        assertEquals(41, values.length); // 40 versions + JAVA_RECENT

        // Verify first and last regular versions
        assertEquals(JavaVersion.JAVA_ANDROID_0_9, values[0]);
        assertEquals(JavaVersion.JAVA_39, values[39]);
        assertEquals(JavaVersion.JAVA_RECENT, values[40]);

        // Test valueOf
        assertEquals(JavaVersion.JAVA_1_8, JavaVersion.valueOf("JAVA_1_8"));
        assertEquals(JavaVersion.JAVA_11, JavaVersion.valueOf("JAVA_11"));
        assertEquals(JavaVersion.JAVA_17, JavaVersion.valueOf("JAVA_17"));
        assertEquals(JavaVersion.JAVA_21, JavaVersion.valueOf("JAVA_21"));
        assertEquals(JavaVersion.JAVA_RECENT, JavaVersion.valueOf("JAVA_RECENT"));
    }

    @Test
    public void testVersionComparisons() {
        // Test version ordering
        assertTrue(JavaVersion.JAVA_ANDROID_0_9.atMost(JavaVersion.JAVA_1_6));
        assertTrue(JavaVersion.JAVA_1_6.atLeast(JavaVersion.JAVA_ANDROID_0_9));

        // Test that JAVA_0_9 has value 1.5 (same as JAVA_1_5)
        assertTrue(JavaVersion.JAVA_ANDROID_0_9.atLeast(JavaVersion.JAVA_1_4));
        assertTrue(JavaVersion.JAVA_ANDROID_0_9.atMost(JavaVersion.JAVA_1_5));
        assertTrue(JavaVersion.JAVA_ANDROID_0_9.atMost(JavaVersion.JAVA_1_6));

        // Test sequential version ordering
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
    public void testJavaRecentBehavior() {
        //    // JAVA_RECENT should be at least as recent as any other version
        //    for (JavaVersion v : JavaVersion.values()) {
        //        assertTrue(JavaVersion.JAVA_RECENT.atLeast(v));
        //        assertTrue(v.atMost(JavaVersion.JAVA_RECENT));
        //    }

        // Test that JAVA_RECENT has a valid string representation
        String recentStr = JavaVersion.JAVA_RECENT.toString();
        assertNotNull(recentStr);
        assertFalse(recentStr.isEmpty());
    }
}