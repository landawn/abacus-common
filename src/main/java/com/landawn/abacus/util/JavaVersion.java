/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

/**
 * An enumeration representing all versions of the Java specification.
 * <p>
 * This enum provides a comprehensive list of Java versions from Java 0.9 (Android) through Java 25,
 * plus a special JAVA_RECENT constant that represents the most recent Java version detected at runtime.
 * The enum mirrors values available from the {@code java.specification.version} system property.
 * </p>
 * 
 * <p>
 * Each enum constant contains both a float value for numerical comparisons and a string name
 * that matches the official version designation. The class provides utility methods for version
 * comparison and parsing version strings.
 * </p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if current Java version is at least Java 8
 * JavaVersion current = JavaVersion.of(System.getProperty("java.specification.version"));
 * if (current.atLeast(JavaVersion.JAVA_1_8)) {
 *     // Use Java 8 features
 * }
 * 
 * // Parse a version string
 * JavaVersion version = JavaVersion.of("11");
 * System.out.println(version); // Prints: 11
 * }</pre>
 * 
 * <p>
 * Note: This class includes codes copied from Apache Commons Lang, under the Apache License 2.0.
 * </p>
 *
 */
public enum JavaVersion {

    /**
     * The Java version reported by Android (0.9).
     * <p>
     * This is not an official Java version number but is included for Android compatibility.
     * Maps to float value 1.5 for comparison purposes.
     * </p>
     */
    JAVA_ANDROID_0_9(1.5f, "0.9"),

    /**
     * Java 1.1.
     * <p>
     * Released in February 1997, introduced inner classes, JavaBeans, JDBC, and RMI.
     * </p>
     */
    JAVA_1_1(1.1f, "1.1"),

    /**
     * Java 1.2.
     * <p>
     * Released in December 1998, introduced the Collections framework, Swing GUI, and strictfp.
     * </p>
     */
    JAVA_1_2(1.2f, "1.2"),

    /**
     * Java 1.3.
     * <p>
     * Released in May 2000, introduced HotSpot JVM, JNDI, and Java Sound.
     * </p>
     */
    JAVA_1_3(1.3f, "1.3"),

    /**
     * Java 1.4.
     * <p>
     * Released in February 2002, introduced assert keyword, regular expressions, and NIO.
     * </p>
     */
    JAVA_1_4(1.4f, "1.4"),

    /**
     * Java 1.5 (also known as Java 5).
     * <p>
     * Released in September 2004, introduced generics, annotations, autoboxing, and enums.
     * </p>
     */
    JAVA_1_5(1.5f, "1.5"),

    /**
     * Java 1.6 (also known as Java 6).
     * <p>
     * Released in December 2006, introduced scripting support, web services, and JDBC 4.0.
     * </p>
     */
    JAVA_1_6(1.6f, "1.6"),

    /**
     * Java 1.7 (also known as Java 7).
     * <p>
     * Released in July 2011, introduced try-with-resources, diamond operator, and switch on strings.
     * </p>
     */
    JAVA_1_7(1.7f, "1.7"),

    /**
     * Java 1.8 (also known as Java 8).
     * <p>
     * Released in March 2014, introduced lambda expressions, streams API, and default methods.
     * </p>
     */
    JAVA_1_8(1.8f, "1.8"),

    /**
     * Java 9.
     * <p>
     * Released in September 2017, introduced the module system (Project Jigsaw) and JShell.
     * </p>
     */
    JAVA_9(9.0f, "9"),

    /**
     * Java 10.
     * <p>
     * Released in March 2018, introduced local variable type inference (var keyword).
     * </p>
     */
    JAVA_10(10.0f, "10"),

    /**
     * Java 11 (LTS - Long Term Support).
     * <p>
     * Released in September 2018, introduced HTTP client API and single-file source execution.
     * </p>
     */
    JAVA_11(11.0f, "11"),

    /**
     * Java 12.
     * <p>
     * Released in March 2019, introduced switch expressions (preview) and compact number formatting.
     * </p>
     */
    JAVA_12(12.0f, "12"),

    /**
     * Java 13.
     * <p>
     * Released in September 2019, introduced text blocks (preview) and dynamic CDS archives.
     * </p>
     */
    JAVA_13(13.0f, "13"),

    /**
     * Java 14.
     * <p>
     * Released in March 2020, introduced records (preview) and pattern matching for instanceof (preview).
     * </p>
     */
    JAVA_14(14.0f, "14"),

    /**
     * Java 15.
     * <p>
     * Released in September 2020, introduced sealed classes (preview) and hidden classes.
     * </p>
     */
    JAVA_15(15.0f, "15"),

    /**
     * Java 16.
     * <p>
     * Released in March 2021, made records a standard feature and introduced Unix domain socket channels.
     * </p>
     */
    JAVA_16(16.0f, "16"),

    /**
     * Java 17 (LTS - Long Term Support).
     * <p>
     * Released in September 2021, made sealed classes standard and introduced pattern matching for switch (preview).
     * </p>
     */
    JAVA_17(17.0f, "17"),

    /**
     * Java 18.
     * <p>
     * Released in March 2022, introduced UTF-8 by default and simple web server.
     * </p>
     */
    JAVA_18(18.0f, "18"),

    /**
     * Java 19.
     * <p>
     * Released in September 2022, introduced virtual threads (preview) and structured concurrency (preview).
     * </p>
     */
    JAVA_19(19.0f, "19"),

    /**
     * Java 20.
     * <p>
     * Released in March 2023, continued evolution of virtual threads and pattern matching.
     * </p>
     */
    JAVA_20(20.0f, "20"),

    /**
     * Java 21 (LTS - Long Term Support).
     * <p>
     * Released in September 2023, made virtual threads a standard feature and introduced sequenced collections.
     * </p>
     */
    JAVA_21(21.0f, "21"),

    /**
     * Java 22.
     * <p>
     * Released in March 2024, introduced unnamed variables and patterns, statements before super(),
     * and string templates (preview).
     * </p>
     */
    JAVA_22(22.0f, "22"),

    /**
     * Java 23.
     * <p>
     * Released in September 2024, introduced primitive types in patterns, switch expressions,
     * and instanceof (preview), and module import declarations (preview).
     * </p>
     */
    JAVA_23(23.0f, "23"),

    /**
     * Java 24.
     * <p>
     * Expected release in March 2025. Features are subject to change until official release.
     * </p>
     */
    JAVA_24(24.0f, "24"),

    /**
     * Java 25.
     * <p>
     * Expected release in September 2025. Features are subject to change until official release.
     * </p>
     */
    JAVA_25(25.0f, "25"),

    /**
     * Java 26.
     * <p>
     * Expected release in March 2026. Features are subject to change until official release.
     * </p>
     */
    JAVA_26(26.0f, "26"),

    /**
     * Java 27.
     * <p>
     * Expected release in September 2026. Features are subject to change until official release.
     * </p>
     */
    JAVA_27(27.0f, "27"),

    /**
     * Java 28.
     * <p>
     * Expected release in March 2027. Features are subject to change until official release.
     * </p>
     */
    JAVA_28(28.0f, "28"),

    /**
     * Java 29.
     * <p>
     * Expected release in September 2027. Features are subject to change until official release.
     * </p>
     */
    JAVA_29(29.0f, "29"),

    /**
     * Java 30.
     * <p>
     * Expected release in March 2028. Features are subject to change until official release.
     * </p>
     */
    JAVA_30(30.0f, "30"),

    /**
     * Java 31.
     * <p>
     * Expected release in September 2028. Features are subject to change until official release.
     * </p>
     */
    JAVA_31(31.0f, "31"),

    /**
     * Java 32.
     * <p>
     * Expected release in March 2029. Features are subject to change until official release.
     * </p>
     */
    JAVA_32(32.0f, "32"),

    /**
     * Java 33.
     * <p>
     * Expected release in September 2029. Features are subject to change until official release.
     * </p>
     */
    JAVA_33(33.0f, "33"),

    /**
     * Java 34.
     * <p>
     * Expected release in March 2030. Features are subject to change until official release.
     * </p>
     */
    JAVA_34(34.0f, "34"),

    /**
     * Java 35.
     * <p>
     * Expected release in September 2030. Features are subject to change until official release.
     * </p>
     */
    JAVA_35(35.0f, "35"),

    /**
     * Java 36.
     * <p>
     * Expected release in March 2031. Features are subject to change until official release.
     * </p>
     */
    JAVA_36(36.0f, "36"),

    /**
     * Java 37.
     * <p>
     * Expected release in September 2031. Features are subject to change until official release.
     * </p>
     */
    JAVA_37(37.0f, "37"),

    /**
     * Java 38.
     * <p>
     * Expected release in March 2032. Features are subject to change until official release.
     * </p>
     */
    JAVA_38(38.0f, "38"),

    /**
     * Java 39.
     * <p>
     * Expected release in September 2032. Features are subject to change until official release.
     * </p>
     */
    JAVA_39(39.0f, "39"),

    /**
     * The most recent Java version detected at runtime.
     * <p>
     * This constant dynamically represents the highest Java version available on the system,
     * as determined by the {@code java.specification.version} system property. It's primarily
     * used to avoid breaking when newer versions of Java are released after this enum was defined.
     * </p>
     * <p>
     * If the system property cannot be read, defaults to version 99.0.
     * </p>
     */
    JAVA_RECENT(maxVersion(), Float.toString(maxVersion()));

    /**
     * The float value used for numerical version comparisons.
     */
    private final float value;

    /**
     * The standard version name as a string (e.g., "1.8", "11", "17").
     */
    private final String name;

    /**
     * Constructs a JavaVersion enum constant with the specified float value and name.
     *
     * @param value the float value for numerical comparisons
     * @param name the standard version name, not null
     */
    JavaVersion(final float value, final String name) {
        this.value = value;
        this.name = name;
    }

    //-----------------------------------------------------------------------

    /**
     * Checks whether this Java version is at least as recent as the specified version.
     * <p>
     * This method performs a numerical comparison of version values to determine if this
     * version is equal to or greater than the required version.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JavaVersion current = JavaVersion.JAVA_11;
     * if (current.atLeast(JavaVersion.JAVA_1_8)) {
     *     System.out.println("Java 8+ features available");
     * }
     * }</pre>
     *
     * @param requiredVersion the minimum version to check against, not null
     * @return {@code true} if this version is equal to or greater than the specified version,
     *         {@code false} otherwise
     */
    public boolean atLeast(final JavaVersion requiredVersion) {
        return value >= requiredVersion.value;
    }

    //-----------------------------------------------------------------------

    /**
     * Checks whether this Java version is at most as recent as the specified version.
     * <p>
     * This method performs a numerical comparison of version values to determine if this
     * version is equal to or less than the specified version.
     * </p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JavaVersion current = JavaVersion.JAVA_1_7;
     * if (current.atMost(JavaVersion.JAVA_1_8)) {
     *     System.out.println("Not using Java 9+ features");
     * }
     * }</pre>
     *
     * @param requiredVersion the maximum version to check against, not null
     * @return {@code true} if this version is equal to or less than the specified version,
     *         {@code false} otherwise
     */
    public boolean atMost(final JavaVersion requiredVersion) {
        return value <= requiredVersion.value;
    }

    /**
     * Transforms the given string with a Java version number to the
     * corresponding constant of this enumeration class. This method is used
     * internally.
     *
     * @param nom the Java version as string
     * @return the corresponding enumeration constant or <b>null</b> if the
     * version is unknown
     */
    // helper for static importing
    static JavaVersion getJavaVersion(final String nom) {
        return get(nom);
    }

    /**
     * Gets a JavaVersion enum constant by parsing the provided version string.
     * <p>
     * This method handles various Java version string formats including:
     * </p>
     * <ul>
     *   <li>Legacy format: "1.1" through "1.8"</li>
     *   <li>Modern format: "9" through "39"</li>
     *   <li>Alternative format for Java 5-8: "5", "6", "7", "8"</li>
     *   <li>Version strings with dots: extracts major version number</li>
     * </ul>
     *
     * <p>For versions above 39, returns {@code JAVA_RECENT}. For complex version
     * strings with multiple dots, recursively parses the major version portion.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JavaVersion.get("1.8");     // Returns JAVA_1_8
     * JavaVersion.get("8");       // Returns JAVA_1_8
     * JavaVersion.get("11");      // Returns JAVA_11
     * JavaVersion.get("17.0.1");  // Returns JAVA_17
     * JavaVersion.get("50");      // Returns JAVA_RECENT (for versions > 39)
     * }</pre>
     *
     * @param versionStr the version string to parse (e.g., "1.8", "11", "17.0.1")
     * @return the corresponding JavaVersion enum constant, or JAVA_RECENT for versions &gt; 39
     * @throws IllegalArgumentException if the version string is {@code null}, invalid, or unrecognized
     */
    static JavaVersion get(final String versionStr) {
        if (Strings.isEmpty(versionStr)) {
            throw new IllegalArgumentException("Invalid Java version: " + versionStr);
        }

        switch (versionStr) {
            case "0.9":
                return JAVA_ANDROID_0_9;
            case "1.1":
                return JAVA_1_1;
            case "1.2":
                return JAVA_1_2;
            case "1.3":
                return JAVA_1_3;
            case "1.4":
                return JAVA_1_4;
            case "1.5":
            case "5":
                return JAVA_1_5;
            case "1.6":
            case "6":
                return JAVA_1_6;
            case "1.7":
            case "7":
                return JAVA_1_7;
            case "1.8":
            case "8":
                return JAVA_1_8;
            case "9":
                return JAVA_9;
            case "10":
                return JAVA_10;
            case "11":
                return JAVA_11;
            case "12":
                return JAVA_12;
            case "13":
                return JAVA_13;
            case "14":
                return JAVA_14;
            case "15":
                return JAVA_15;
            case "16":
                return JAVA_16;
            case "17":
                return JAVA_17;
            case "18":
                return JAVA_18;
            case "19":
                return JAVA_19;
            case "20":
                return JAVA_20;
            case "21":
                return JAVA_21;
            case "22":
                return JAVA_22;
            case "23":
                return JAVA_23;
            case "24":
                return JAVA_24;
            case "25":
                return JAVA_25;
            case "26":
                return JAVA_26;
            case "27":
                return JAVA_27;
            case "28":
                return JAVA_28;
            case "29":
                return JAVA_29;
            case "30":
                return JAVA_30;
            case "31":
                return JAVA_31;
            case "32":
                return JAVA_32;
            case "33":
                return JAVA_33;
            case "34":
                return JAVA_34;
            case "35":
                return JAVA_35;
            case "36":
                return JAVA_36;
            case "37":
                return JAVA_37;
            case "38":
                return JAVA_38;
            case "39":
                return JAVA_39;
            default:
                if ((versionStr.startsWith("1.") || versionStr.startsWith("0.")) && versionStr.indexOf('.', 2) > 0) {
                    return get(versionStr.substring(0, versionStr.indexOf('.', 2)));
                } else if (versionStr.indexOf('.') > 0) {
                    return get(versionStr.substring(0, versionStr.indexOf('.')));
                }

                float v = toFloatVersion(versionStr);

                if (v > 39) {
                    return JAVA_RECENT;
                }

                throw new IllegalArgumentException("Invalid Java version: " + versionStr);
        }
    }

    /**
     * Parses a version string and returns the corresponding JavaVersion enum constant.
     * <p>
     * This method intelligently handles various version string formats:
     * </p>
     * <ul>
     *   <li>Old format with "1." prefix: "1.5", "1.8" → JAVA_1_5, JAVA_1_8</li>
     *   <li>Android format: "0.9" → JAVA_0_9</li>
     *   <li>Modern format: "9", "11", "17" → JAVA_9, JAVA_11, JAVA_17</li>
     *   <li>With minor versions: "11.0.2" → JAVA_11 (ignores minor/patch versions)</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * JavaVersion v1 = JavaVersion.of("1.8");     // Returns JAVA_1_8
     * JavaVersion v2 = JavaVersion.of("11");      // Returns JAVA_11
     * JavaVersion v3 = JavaVersion.of("17.0.1");  // Returns JAVA_17
     * 
     * // Get current Java version
     * JavaVersion current = JavaVersion.of(System.getProperty("java.specification.version"));
     * }</pre>
     *
     * @param versionStr the version string to parse (e.g., "1.8", "11", "17.0.1")
     * @return the corresponding JavaVersion enum constant
     * @throws IllegalArgumentException if the version string cannot be parsed or doesn't match any known Java version
     */
    public static JavaVersion of(final String versionStr) {
        if (Strings.isEmpty(versionStr)) {
            throw new IllegalArgumentException("Invalid Java version: " + versionStr);
        }

        JavaVersion result = null;

        if (versionStr.startsWith("1.") || versionStr.startsWith("0.")) {
            result = get(versionStr.substring(0, 3));
        } else {
            final int idx = versionStr.indexOf('.');
            result = get(idx > 0 ? versionStr.substring(0, idx) : versionStr);
        }

        if (result == null) {
            throw new IllegalArgumentException("No java version found by: " + versionStr);
        }

        return result;
    }

    //-----------------------------------------------------------------------

    /**
     * Returns the standard name of this Java version.
     * <p>
     * The returned string matches the official version designation:
     * </p>
     * <ul>
     *   <li>For Java 8 and earlier: "1.1", "1.2", ..., "1.8"</li>
     *   <li>For Java 9 and later: "9", "10", "11", ..., "25"</li>
     *   <li>For Android: "0.9"</li>
     *   <li>For JAVA_RECENT: the detected version number as a string</li>
     * </ul>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * System.out.println(JavaVersion.JAVA_1_8);  // Prints: 1.8
     * System.out.println(JavaVersion.JAVA_11);   // Prints: 11
     * }</pre>
     *
     * @return the standard version name, never null
     */
    @Override
    public String toString() {
        return name;
    }

    /**
     * Gets the Java Version from the system or 99.0 if the {@code java.specification.version} system property is not set.
     *
     * @return the value of {@code java.specification.version} system property or 99.0 if it is not set.
     */
    private static float maxVersion() {
        final float v = toFloatVersion(System.getProperty("java.specification.version", "99.0"));
        if (v > 0) {
            return v;
        }
        return 99f;
    }

    /**
     * Converts a Java version string to its float representation.
     * <p>
     * Handles version strings in different formats:
     * </p>
     * <ul>
     *   <li>Dotted format: "1.8" → 1.8, "11.0.2" → 11.0</li>
     *   <li>Simple format: "17" → 17.0</li>
     * </ul>
     * 
     * <p>For dotted versions, only the first two components are used.
     * Returns -1 if the version string cannot be parsed.</p>
     *
     * @param value the version string to convert
     * @return the float representation of the version, or -1 if parsing fails
     * @throws IllegalArgumentException if the version string format is completely invalid
     */
    private static float toFloatVersion(final String value) {
        try {
            final int defaultReturnValue = -1;
            if (value.contains(".")) {
                final String[] toParse = value.split("\\.");
                if (toParse.length >= 2) {
                    return Numbers.toFloat(toParse[0] + '.' + toParse[1], defaultReturnValue);
                }
            } else {
                return Numbers.toFloat(value, defaultReturnValue);
            }
            return defaultReturnValue;
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Invalid Java version: " + value, e);
        }
    }
}
