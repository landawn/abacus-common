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
 * Note: it's copied from Apache Commons IO developed at <a href="http://www.apache.org/">The Apache Software Foundation</a>, or under the Apache License 2.0.
 *
 * Enumeration of IO case sensitivity.
 * <p>
 * Different filing systems have different rules for case-sensitivity.
 * Windows is case-insensitive, Unix is case-sensitive.
 * <p>
 * This class captures that difference, providing an enumeration to
 * control how filename comparisons should be performed. It also provides
 * methods that use the enumeration to perform comparisons.
 * <p>
 * Wherever possible, you should use the {@code check} methods in this
 * class to compare filenames.
 *
 * @version $Id: IOCase.java 1483915 2013-05-17 17:02:35Z sebb $
 */
public enum IOCase {

    /**
     * The constant for case-sensitive regardless of operating system.
     */
    SENSITIVE("Sensitive", true),

    /**
     * The constant for case-insensitive regardless of operating system.
     */
    INSENSITIVE("Insensitive", false),

    /**
     * The constant for case sensitivity determined by the current operating system.
     * Windows is case-insensitive, when comparing filenames, Unix is case-sensitive.
     * <p>
     * <strong>Note:</strong> This only caters for Windows and Unix. Other operating
     * systems (e.g., OSX and OpenVMS) are treated as case-sensitive if they use the
     * Unix file separator and case-insensitive if they use the Windows file separator
     * (see {@link java.io.File#separatorChar}).
     * <p>
     * If you serialize this constant of Windows, and deserialize on Unix, or vice
     * versa, then the value of the case-sensitivity flag will change.
     */
    SYSTEM("System", !IOUtil.IS_OS_WINDOWS);

    /** Serialization version. */
    private static final long serialVersionUID = -6343169151696340687L;

    private static final String ERROR_MSG_01 = "The strings must not be null";

    private final String name;

    private final transient boolean sensitive;

    //-----------------------------------------------------------------------

    /**
     * Factory method to create an IOCase from a name.
     *
     * @param name the name to find
     * @return
     * @throws IllegalArgumentException if the name is invalid
     */
    public static IOCase forName(final String name) {
        for (final IOCase ioCase : IOCase.values()) {
            if (ioCase.getName().equals(name)) {
                return ioCase;
            }
        }
        throw new IllegalArgumentException("Invalid IOCase name: " + name);
    }

    //-----------------------------------------------------------------------

    /**
     *
     * @param name
     * @param sensitive the sensitivity
     */
    IOCase(final String name, final boolean sensitive) {
        this.name = name;
        this.sensitive = sensitive;
    }

    /**
     * Replaces the enumeration from the stream with a real one.
     * This ensures that the correct flag is set for SYSTEM.
     *
     * @return
     */
    private Object readResolve() {
        return forName(name);
    }

    //-----------------------------------------------------------------------

    /**
     * Gets the name of the constant.
     *
     * @return
     */
    public String getName() {
        return name;
    }

    /**
     * Does the object represent case-sensitive comparison.
     *
     * @return {@code true} if case-sensitive
     */
    public boolean isCaseSensitive() {
        return sensitive;
    }

    //-----------------------------------------------------------------------

    /**
     * Compares two strings using the case-sensitivity rule.
     * <p>
     * This method mimics {@link String#compareTo} but takes case-sensitivity
     * into account.
     *
     * @param str1 the first string to compare, not null
     * @param str2 the second string to compare, not null
     * @return {@code true} if equal using the case rules
     * @throws IllegalArgumentException if either string is null
     */
    public int checkCompareTo(final String str1, final String str2) {
        if (str1 == null || str2 == null) {
            throw new IllegalArgumentException(ERROR_MSG_01);
        }
        return sensitive ? str1.compareTo(str2) : str1.compareToIgnoreCase(str2);
    }

    /**
     * Compares two strings using the case-sensitivity rule.
     * <p>
     * This method mimics {@link String#equals} but takes case-sensitivity
     * into account.
     *
     * @param str1 the first string to compare, not null
     * @param str2 the second string to compare, not null
     * @return {@code true} if equal using the case rules
     * @throws IllegalArgumentException if either string is null
     */
    public boolean checkEquals(final String str1, final String str2) {
        if (str1 == null || str2 == null) {
            throw new IllegalArgumentException(ERROR_MSG_01);
        }
        return sensitive ? str1.equals(str2) : str1.equalsIgnoreCase(str2);
    }

    /**
     * Checks if one string starts with another using the case-sensitivity rule.
     * <p>
     * This method mimics {@link String#startsWith(String)} but takes case-sensitivity
     * into account.
     *
     * @param str the string to check, not null
     * @param start the start to compare against, not null
     * @return {@code true} if equal using the case rules
     * @throws IllegalArgumentException if either string is null
     */
    public boolean checkStartsWith(final String str, final String start) {
        if (str == null || start == null) {
            throw new IllegalArgumentException(ERROR_MSG_01);
        }

        return str.regionMatches(!sensitive, 0, start, 0, start.length());
    }

    /**
     * Checks if one string ends with another using the case-sensitivity rule.
     * <p>
     * This method mimics {@link String#endsWith} but takes case-sensitivity
     * into account.
     *
     * @param str the string to check, not null
     * @param end the end to compare against, not null
     * @return {@code true} if equal using the case rules
     * @throws IllegalArgumentException if either string is null
     */
    public boolean checkEndsWith(final String str, final String end) {
        if (str == null || end == null) {
            throw new IllegalArgumentException(ERROR_MSG_01);
        }

        final int endLen = end.length();
        return str.regionMatches(!sensitive, str.length() - endLen, end, 0, endLen);
    }

    /**
     * Checks if one string contains another starting at a specific index using the
     * case-sensitivity rule.
     * <p>
     * This method mimics parts of {@link String#indexOf(String, int)}
     * but takes case-sensitivity into account.
     *
     * @param str the string to check, not null
     * @param strStartIndex the index to start at in str
     * @param search the start to search for, not null
     * @return
     *  -1 if no match or {@code null} string input
     * @throws IllegalArgumentException if either string is null
     */
    public int checkIndexOf(final String str, final int strStartIndex, final String search) {
        if (str == null || search == null) {
            throw new IllegalArgumentException(ERROR_MSG_01);
        }

        final int endIndex = str.length() - search.length();
        if (endIndex >= strStartIndex) {
            for (int i = strStartIndex; i <= endIndex; i++) {
                if (checkRegionMatches(str, i, search)) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * Checks if one string contains another at a specific index using the case-sensitivity rule.
     * <p>
     * This method mimics parts of {@link String#regionMatches(boolean, int, String, int, int)}
     * but takes case-sensitivity into account.
     *
     * @param str the string to check, not null
     * @param strStartIndex the index to start at in str
     * @param search the start to search for, not null
     * @return {@code true} if equal using the case rules
     * @throws IllegalArgumentException if either string is null
     */
    public boolean checkRegionMatches(final String str, final int strStartIndex, final String search) {
        if (str == null || search == null) {
            throw new IllegalArgumentException(ERROR_MSG_01);
        }

        return str.regionMatches(!sensitive, strStartIndex, search, 0, search.length());
    }

    //-----------------------------------------------------------------------

    /**
     * Gets a string describing the sensitivity.
     *
     * @return a string describing the sensitivity
     */
    @Override
    public String toString() {
        return name;
    }

}
