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
 * <p>
 * Note: This class includes codes copied from Apache Commons Lang, under the Apache License 2.0.
 * </p>
 *
 * <p>An enum representing all the versions of the Java specification.
 * This is intended to mirror available values from the
 * <em>java.specification.version</em> System property. </p>
 *
 */
public enum JavaVersion {

    /**
     * The Java version reported by Android. This is not an official Java version number.
     */
    JAVA_0_9(1.5f, "0.9"),

    /**
     * Java 1.1.
     */
    JAVA_1_1(1.1f, "1.1"),

    /**
     * Java 1.2.
     */
    JAVA_1_2(1.2f, "1.2"),

    /**
     * Java 1.3.
     */
    JAVA_1_3(1.3f, "1.3"),

    /**
     * Java 1.4.
     */
    JAVA_1_4(1.4f, "1.4"),

    /**
     * Java 1.5.
     */
    JAVA_1_5(1.5f, "1.5"),

    /**
     * Java 1.6.
     */
    JAVA_1_6(1.6f, "1.6"),

    /**
     * Java 1.7.
     */
    JAVA_1_7(1.7f, "1.7"),

    /**
     * Java 1.8.
     */
    JAVA_1_8(1.8f, "1.8"),

    JAVA_9(9.0f, "9"),

    JAVA_10(10.0f, "10"),

    JAVA_11(11.0f, "11"),

    JAVA_12(12.0f, "12"),

    JAVA_13(13.0f, "13"),

    JAVA_14(14.0f, "14"),

    JAVA_15(15.0f, "15"),

    JAVA_16(16.0f, "16"),

    JAVA_17(17.0f, "17"),

    JAVA_18(18.0f, "18"),

    /**
     * Java 19.
     */
    JAVA_19(19.0f, "19"),

    /**
     * Java 20.
     */
    JAVA_20(20.0f, "20"),

    /**
     * Java 21.
     */
    JAVA_21(21.0f, "21"),

    /**
     * The most recent java version. Mainly introduced to avoid breaking when a new version of Java is used.
     */
    JAVA_RECENT(maxVersion(), Float.toString(maxVersion()));

    /**
     * The float value.
     */
    private final float value;

    /**
     * The standard name.
     */
    private final String name;

    /**
     *
     * @param value the float value
     * @param name the standard name, not null
     */
    JavaVersion(final float value, final String name) {
        this.value = value;
        this.name = name;
    }

    //-----------------------------------------------------------------------

    /**
     * <p>Whether this version of Java is at least the version of Java passed in.</p>
     *
     * <p>For example:<br>
     *  {@code myVersion.atLeast(JavaVersion.JAVA_1_4)}<p>
     *
     * @param requiredVersion the version to check against, not null
     * @return {@code true} if this version is equal to or greater than the specified version
     */
    public boolean atLeast(final JavaVersion requiredVersion) {
        return value >= requiredVersion.value;
    }

    //-----------------------------------------------------------------------

    /**
     * <p>Whether this version of Java is at most the version of Java passed in.</p>
     *
     * <p>For example:<br>
     *  {@code myVersion.atMost(JavaVersion.JAVA_1_4)}<p>
     *
     * @param requiredVersion the version to check against, not null
     * @return {@code true} if this version is equal to or greater than the specified version
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
     * Transforms the given string with a Java version number to the
     * corresponding constant of this enumeration class. This method is used
     * internally.
     *
     * @param versionStr the Java version as string
     * @return the corresponding enumeration constant or <b>null</b> if the
     * version is unknown
     */
    static JavaVersion get(final String versionStr) {
        if (versionStr == null) {
            return null;
        }
        switch (versionStr) {
            case "0.9":
                return JAVA_0_9;
            case "1.1":
                return JAVA_1_1;
            case "1.2":
                return JAVA_1_2;
            case "1.3":
                return JAVA_1_3;
            case "1.4":
                return JAVA_1_4;
            case "1.5":
                return JAVA_1_5;
            case "1.6":
                return JAVA_1_6;
            case "1.7":
                return JAVA_1_7;
            case "1.8":
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
            default:
                final float v = toFloatVersion(versionStr);
                if ((v - 1.) < 1.) { // then we need to check decimals > .9
                    final int firstComma = Math.max(versionStr.indexOf('.'), versionStr.indexOf(','));
                    final int end = Math.max(versionStr.length(), versionStr.indexOf(',', firstComma));
                    if (Float.parseFloat(versionStr.substring(firstComma + 1, end)) > .9f) {
                        return JAVA_RECENT;
                    }
                } else if (v > 10) {
                    return JAVA_RECENT;
                }
                return null;
        }
    }

    /**
     *
     * @param versionStr
     * @return
     */
    public static JavaVersion of(final String versionStr) {
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
     * <p>The string value is overridden to return the standard name.</p>
     *
     * <p>For example, {@code "1.5"}.</p>
     *
     * @return the name, not null
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
     * Parses a float value from a String.
     *
     * @param value the String to parse.
     * @return the float value represented by the string or -1 if the given String cannot be parsed.
     */
    private static float toFloatVersion(final String value) {
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
    }
}
