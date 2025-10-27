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
 * Enumeration for controlling case sensitivity in I/O operations.
 * 
 * <p>This enum provides a consistent way to handle case sensitivity across different
 * file systems. Windows is typically case-insensitive while Unix/Linux is case-sensitive.
 * This class provides methods to perform case-aware string comparisons.</p>
 * 
 * <p>Note: This class is adapted from Apache Commons IO, licensed under Apache License 2.0.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Check if two filenames are equal considering case sensitivity
 * boolean isEqual = IOCase.SYSTEM.checkEquals("File.txt", "file.txt");
 * // On Windows: true, on Unix: false
 * 
 * // Always case-sensitive comparison
 * boolean isSensitive = IOCase.SENSITIVE.checkEquals("File.txt", "file.txt"); // false
 * }</pre>
 *
 * @version $Id: IOCase.java 1483915 2013-05-17 17:02:35Z sebb $
 */
public enum IOCase {

    /**
     * Case-sensitive comparison regardless of operating system.
     * This constant forces case-sensitive string comparisons on all platforms.
     */
    SENSITIVE("Sensitive", true),

    /**
     * Case-insensitive comparison regardless of operating system.
     * This constant forces case-insensitive string comparisons on all platforms.
     */
    INSENSITIVE("Insensitive", false),

    /**
     * Case sensitivity determined by the current operating system.
     * Windows is treated as case-insensitive, Unix/Linux as case-sensitive.
     * 
     * <p><strong>Note:</strong> This only handles Windows and Unix. Other operating
     * systems (e.g., OSX and OpenVMS) are treated as case-sensitive if they use the
     * Unix file separator and case-insensitive if they use the Windows file separator
     * (see {@link java.io.File#separatorChar}).</p>
     * 
     * <p>If you serialize this constant on Windows and deserialize on Unix (or vice
     * versa), the case-sensitivity flag will change to match the target system.</p>
     */
    SYSTEM("System", !IOUtil.IS_OS_WINDOWS);

    /** Serialization version. */
    private static final long serialVersionUID = -6343169151696340687L;

    private static final String ERROR_MSG_01 = "The strings must not be null";

    private final String name;

    private final transient boolean sensitive;

    //-----------------------------------------------------------------------

    /**
     * Factory method to create an IOCase instance from its name.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * IOCase ioCase = IOCase.forName("Sensitive"); // returns IOCase.SENSITIVE
     * }</pre>
     *
     * @param name the name of the IOCase constant ("Sensitive", "Insensitive", or "System")
     * @return the corresponding IOCase enum constant
     * @throws IllegalArgumentException if the name is not valid
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

    IOCase(final String name, final boolean sensitive) {
        this.name = name;
        this.sensitive = sensitive;
    }

    /**
     * Replaces the deserialized enum with the proper singleton instance.
     * This ensures that the SYSTEM constant has the correct case sensitivity
     * for the current operating system after deserialization.
     *
     * @return the resolved enum constant
     */
    private Object readResolve() {
        return forName(name);
    }

    //-----------------------------------------------------------------------

    /**
     * Gets the name of this IOCase constant.
     *
     * @return the name of the constant ("Sensitive", "Insensitive", or "System")
     */
    public String getName() {
        return name;
    }

    /**
     * Checks whether this IOCase represents case-sensitive comparison.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * if (IOCase.SENSITIVE.isCaseSensitive()) {
     *     // Perform case-sensitive operations
     * }
     * }</pre>
     *
     * @return {@code true} if case-sensitive, {@code false} if case-insensitive
     */
    public boolean isCaseSensitive() {
        return sensitive;
    }

    //-----------------------------------------------------------------------

    /**
     * Compares two strings using the case-sensitivity rule of this IOCase.
     * 
     * <p>This method mimics {@link String#compareTo} but takes case-sensitivity
     * into account based on this IOCase setting.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int result = IOCase.INSENSITIVE.checkCompareTo("File.txt", "file.txt"); // returns 0
     * int result2 = IOCase.SENSITIVE.checkCompareTo("File.txt", "file.txt"); // returns negative
     * }</pre>
     *
     * @param str1 the first string to compare, not null
     * @param str2 the second string to compare, not null
     * @return negative if str1 &lt; str2, zero if str1 equals str2, positive if str1 &gt; str2
     * @throws IllegalArgumentException if either string is null
     */
    public int checkCompareTo(final String str1, final String str2) {
        if (str1 == null || str2 == null) {
            throw new IllegalArgumentException(ERROR_MSG_01);
        }
        return sensitive ? str1.compareTo(str2) : str1.compareToIgnoreCase(str2);
    }

    /**
     * Compares two strings for equality using the case-sensitivity rule of this IOCase.
     * 
     * <p>This method mimics {@link String#equals} but takes case-sensitivity
     * into account based on this IOCase setting.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean equal = IOCase.INSENSITIVE.checkEquals("File.txt", "file.txt"); // true
     * boolean equal2 = IOCase.SENSITIVE.checkEquals("File.txt", "file.txt"); // false
     * }</pre>
     *
     * @param str1 the first string to compare, not null
     * @param str2 the second string to compare, not null
     * @return {@code true} if the strings are equal according to the case rule
     * @throws IllegalArgumentException if either string is null
     */
    public boolean checkEquals(final String str1, final String str2) {
        if (str1 == null || str2 == null) {
            throw new IllegalArgumentException(ERROR_MSG_01);
        }
        return sensitive ? str1.equals(str2) : str1.equalsIgnoreCase(str2);
    }

    /**
     * Checks if one string starts with another using the case-sensitivity rule of this IOCase.
     * 
     * <p>This method mimics {@link String#startsWith(String)} but takes case-sensitivity
     * into account based on this IOCase setting.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean starts = IOCase.INSENSITIVE.checkStartsWith("File.txt", "FILE"); // true
     * boolean starts2 = IOCase.SENSITIVE.checkStartsWith("File.txt", "FILE"); // false
     * }</pre>
     *
     * @param str the string to check, not null
     * @param start the prefix to look for, not null
     * @return {@code true} if str starts with the prefix according to the case rule
     * @throws IllegalArgumentException if either string is null
     */
    public boolean checkStartsWith(final String str, final String start) {
        if (str == null || start == null) {
            throw new IllegalArgumentException(ERROR_MSG_01);
        }

        return str.regionMatches(!sensitive, 0, start, 0, start.length());
    }

    /**
     * Checks if one string ends with another using the case-sensitivity rule of this IOCase.
     * 
     * <p>This method mimics {@link String#endsWith} but takes case-sensitivity
     * into account based on this IOCase setting.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean ends = IOCase.INSENSITIVE.checkEndsWith("File.txt", ".TXT"); // true
     * boolean ends2 = IOCase.SENSITIVE.checkEndsWith("File.txt", ".TXT"); // false
     * }</pre>
     *
     * @param str the string to check, not null
     * @param end the suffix to look for, not null
     * @return {@code true} if str ends with the suffix according to the case rule
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
     * Finds the index of a search string within another string starting at a specific index,
     * using the case-sensitivity rule of this IOCase.
     * 
     * <p>This method mimics parts of {@link String#indexOf(String, int)}
     * but takes case-sensitivity into account based on this IOCase setting.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int index = IOCase.INSENSITIVE.checkIndexOf("Find a FILE here", 0, "file"); // returns 7
     * int index2 = IOCase.SENSITIVE.checkIndexOf("Find a FILE here", 0, "file"); // returns -1
     * }</pre>
     *
     * @param str the string to search in, not null
     * @param strStartIndex the index to start searching from
     * @param search the string to search for, not null
     * @return the index of the first occurrence of search in str, or -1 if not found
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
     * Checks if a region of one string matches another string at a specific index,
     * using the case-sensitivity rule of this IOCase.
     * 
     * <p>This method mimics parts of {@link String#regionMatches(boolean, int, String, int, int)}
     * but takes case-sensitivity into account based on this IOCase setting.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean matches = IOCase.INSENSITIVE.checkRegionMatches("File.txt", 0, "FILE"); // true
     * boolean matches2 = IOCase.SENSITIVE.checkRegionMatches("File.txt", 0, "FILE"); // false
     * }</pre>
     *
     * @param str the string to check, not null
     * @param strStartIndex the index in str to start matching from
     * @param search the string to match against, not null
     * @return {@code true} if the region matches according to the case rule
     * @throws IllegalArgumentException if either string is null
     */
    public boolean checkRegionMatches(final String str, final int strStartIndex, final String search) {
        if (str == null || search == null) {
            throw new IllegalArgumentException(ERROR_MSG_01);
        }

        return str.regionMatches(!sensitive, strStartIndex, search, 0, search.length());
    }

    //-----------------------------------------------------------------------

    @Override
    public String toString() {
        return name;
    }

}
