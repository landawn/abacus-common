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

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Stack;

import com.landawn.abacus.annotation.MayReturnNull;

/**
 * General filename and filepath manipulation utilities.
 * 
 * <p>This class helps avoid problems when moving between Windows and Unix-based systems
 * by providing platform-independent filename operations. It's copied from Apache Commons IO
 * developed at The Apache Software Foundation under the Apache License 2.0.</p>
 *
 * <p>This class defines six components within a filename (example C:\dev\project\file.txt):</p>
 * <ul>
 * <li>the prefix - C:\</li>
 * <li>the path - dev\project\</li>
 * <li>the full path - C:\dev\project\</li>
 * <li>the name - file.txt</li>
 * <li>the base name - file</li>
 * <li>the extension - txt</li>
 * </ul>
 *
 * <p>Most methods work with both Unix and Windows separators and prefixes.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Get file extension
 * String ext = FilenameUtil.getExtension("report.pdf");   // Returns "pdf"
 * 
 * // Get base name without extension
 * String base = FilenameUtil.getBaseName("/docs/report.pdf");   // Returns "report"
 * 
 * // Normalize path
 * String normalized = FilenameUtil.normalize("/foo/../bar/./file.txt");   // Returns "/bar/file.txt"
 * 
 * // Check if file matches wildcard pattern
 * boolean matches = FilenameUtil.wildcardMatch("test.java", "*.java");   // Returns true
 * }</pre>
 *
 * <p>Origin of code: Excalibur, Alexandria, Tomcat, Commons-Utils.</p>
 */
public final class FilenameUtil {

    private static final int NOT_FOUND = -1;

    /**
     * The extension separator character.
     * This is always a period/dot character: '.'
     */
    public static final char EXTENSION_SEPARATOR = '.';

    /**
     * The extension separator String.
     * This is always a period/dot character: "."
     */
    public static final String EXTENSION_SEPARATOR_STR = Character.toString(EXTENSION_SEPARATOR);

    /**
     * The Unix separator character.
     */
    private static final char UNIX_SEPARATOR = '/';

    /**
     * The Windows separator character.
     */
    private static final char WINDOWS_SEPARATOR = '\\';

    /**
     * The system separator character.
     */
    private static final char SYSTEM_SEPARATOR = File.separatorChar;

    /**
     * The separator character that is the opposite of the system separator.
     */
    private static final char OTHER_SEPARATOR;

    static {
        if (IOUtil.IS_OS_WINDOWS) {
            OTHER_SEPARATOR = UNIX_SEPARATOR;
        } else {
            OTHER_SEPARATOR = WINDOWS_SEPARATOR;
        }
    }

    private FilenameUtil() {
        // singleton for utility class.
    }

    //-----------------------------------------------------------------------

    private static boolean isSeparator(final char ch) {
        return ch == UNIX_SEPARATOR || ch == WINDOWS_SEPARATOR;
    }

    //-----------------------------------------------------------------------

    /**
     * Normalizes a path, removing double and single dot path steps.
     *
     * <p>This method normalizes a path to a standard format.
     * The input may contain separators in either Unix or Windows format.
     * The output will contain separators in the format of the system.</p>
     *
     * <p>A trailing slash will be retained.
     * A double slash will be merged to a single slash (but UNC names are handled).
     * A single dot path segment will be removed.
     * A double dot will cause that path segment and the one before to be removed.
     * If the double dot has no parent path segment, {@code null} is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.normalize("/foo//");               // Returns "/foo/"
     * FilenameUtil.normalize("/foo/./");              // Returns "/foo/"
     * FilenameUtil.normalize("/foo/../bar");          // Returns "/bar"
     * FilenameUtil.normalize("/foo/../bar/");         // Returns "/bar/"
     * FilenameUtil.normalize("/foo/../bar/../baz");   // Returns "/baz"
     * FilenameUtil.normalize("/../");                 // Returns null
     * FilenameUtil.normalize("C:\\foo\\..\\bar");     // Returns "C:\\bar" on Windows
     * }</pre>
     *
     * @param filename the filename to normalize, {@code null} returns {@code null}
     * @return the normalized filename, or {@code null} if invalid. Null bytes inside string will be removed
     * @see IOUtil#simplifyPath(String)
     */
    @MayReturnNull
    public static String normalize(final String filename) {
        return doNormalize(filename, SYSTEM_SEPARATOR, true);
    }

    /**
     * Normalizes a path with control over the separator character.
     *
     * <p>This method normalizes a path to a standard format.
     * The input may contain separators in either Unix or Windows format.
     * The output will contain separators in the format specified.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Force Unix separators
     * FilenameUtil.normalize("C:\\foo\\bar", true);   // Returns "C:/foo/bar"
     *
     * // Force Windows separators
     * FilenameUtil.normalize("/foo/bar", false);   // Returns "\\foo\\bar"
     * }</pre>
     *
     * @param filename the filename to normalize, {@code null} returns {@code null}
     * @param unixSeparator {@code true} if a unix separator should be used,
     *                      {@code false} if a windows separator should be used
     * @return the normalized filename, or {@code null} if invalid. Null bytes inside string will be removed
     * @see IOUtil#simplifyPath(String)
     */
    @MayReturnNull
    public static String normalize(final String filename, final boolean unixSeparator) {
        final char separator = unixSeparator ? UNIX_SEPARATOR : WINDOWS_SEPARATOR;
        return doNormalize(filename, separator, true);
    }

    //-----------------------------------------------------------------------

    /**
     * Normalizes a path, removing double and single dot path steps,
     * and removing any final directory separator.
     *
     * <p>This method is similar to {@link #normalize(String)} but removes
     * the trailing slash if present.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.normalizeNoEndSeparator("/foo//");        // Returns "/foo"
     * FilenameUtil.normalizeNoEndSeparator("/foo/./");       // Returns "/foo"
     * FilenameUtil.normalizeNoEndSeparator("/foo/../bar");   // Returns "/bar"
     * FilenameUtil.normalizeNoEndSeparator("/foo/bar/");     // Returns "/foo/bar"
     * }</pre>
     *
     * @param filename the filename to normalize, {@code null} returns {@code null}
     * @return the normalized filename without trailing separator, or {@code null} if invalid.
     *         Null bytes inside string will be removed
     */
    @MayReturnNull
    public static String normalizeNoEndSeparator(final String filename) {
        return doNormalize(filename, SYSTEM_SEPARATOR, false);
    }

    /**
     * Normalizes a path with control over the separator and trailing slash.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Unix separators, no trailing slash
     * FilenameUtil.normalizeNoEndSeparator("C:\\foo\\bar\\", true);   // Returns "C:/foo/bar"
     * }</pre>
     *
     * @param filename the filename to normalize, {@code null} returns {@code null}
     * @param unixSeparator {@code true} if a unix separator should be used,
     *                      {@code false} if a windows separator should be used
     * @return the normalized filename without trailing separator, or {@code null} if invalid.
     *         Null bytes inside string will be removed
     */
    @MayReturnNull
    public static String normalizeNoEndSeparator(final String filename, final boolean unixSeparator) {
        final char separator = unixSeparator ? UNIX_SEPARATOR : WINDOWS_SEPARATOR;
        return doNormalize(filename, separator, false);
    }

    private static String doNormalize(final String filename, final char separator, final boolean keepSeparator) {
        if (filename == null) {
            return null;
        }

        failIfNullBytePresent(filename);

        int size = filename.length();
        if (size == 0) {
            return filename;
        }
        final int prefix = getPrefixLength(filename);
        if (prefix < 0) {
            return null;
        }

        final char[] array = new char[size + 2]; // +1 for possible extra slash, +2 for arraycopy
        filename.getChars(0, filename.length(), array, 0);

        // fix separators throughout
        final char otherSeparator = separator == SYSTEM_SEPARATOR ? OTHER_SEPARATOR : SYSTEM_SEPARATOR;
        for (int i = 0; i < size; i++) {
            if (array[i] == otherSeparator) {
                array[i] = separator;
            }
        }

        // add extra separator on the end to simplify code below
        boolean lastIsDirectory = true;
        if (array[size - 1] != separator) {
            array[size++] = separator;
            lastIsDirectory = false;
        }

        // adjoining slashes
        for (int i = prefix + 1; i < size; i++) {
            if (array[i] == separator && array[i - 1] == separator) {
                System.arraycopy(array, i, array, i - 1, size - i);
                size--;
                i--;
            }
        }

        // dot slash
        for (int i = prefix + 1; i < size; i++) {
            if (array[i] == separator && array[i - 1] == '.' && (i == prefix + 1 || array[i - 2] == separator)) {
                if (i == size - 1) {
                    lastIsDirectory = true;
                }
                System.arraycopy(array, i + 1, array, i - 1, size - i);
                size -= 2;
                i--;
            }
        }

        // double dot slash
        outer: for (int i = prefix + 2; i < size; i++) { //NOSONAR
            if (array[i] == separator && array[i - 1] == '.' && array[i - 2] == '.' && (i == prefix + 2 || array[i - 3] == separator)) {
                if (i == prefix + 2) {
                    return null;
                }
                if (i == size - 1) {
                    lastIsDirectory = true;
                }
                int j;
                for (j = i - 4; j >= prefix; j--) {
                    if (array[j] == separator) {
                        // remove b/../ from a/b/../c
                        System.arraycopy(array, i + 1, array, j + 1, size - i);
                        size -= i - j;
                        i = j + 1;
                        continue outer;
                    }
                }
                // remove a/../ from a/../c
                System.arraycopy(array, i + 1, array, prefix, size - i);
                size -= i + 1 - prefix;
                i = prefix + 1;
            }
        }

        if (size <= 0) { // should never be less than 0
            return "";
        }
        if ((size <= prefix) || (lastIsDirectory && keepSeparator)) {
            return new String(array, 0, size); // keep trailing separator
        }
        return new String(array, 0, size - 1); // lose trailing separator
    }

    //-----------------------------------------------------------------------

    /**
     * Concatenates a filename to a base path using normal command line style rules.
     *
     * <p>The effect is equivalent to changing directory to the first argument,
     * followed by changing directory to the second argument.</p>
     *
     * <p>If {@code fullFilenameToAdd} is absolute, it will be normalized and returned.
     * Otherwise, the paths will be joined, normalized and returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.concat("/foo/", "bar");         // Returns "/foo/bar"
     * FilenameUtil.concat("/foo", "bar");          // Returns "/foo/bar"
     * FilenameUtil.concat("/foo", "/bar");         // Returns "/bar"
     * FilenameUtil.concat("/foo", "C:/bar");       // Returns "C:/bar"
     * FilenameUtil.concat("/foo/a/", "../bar");    // Returns "/foo/bar"
     * FilenameUtil.concat("/foo/", "../../bar");   // Returns null
     * }</pre>
     *
     * @param basePath the base path to attach to, always treated as a path, {@code null} returns {@code null}
     * @param fullFilenameToAdd the filename (or path) to attach to the base, {@code null} returns {@code null}
     * @return the concatenated path, or {@code null} if invalid. Null bytes inside string will be removed
     */
    @MayReturnNull
    public static String concat(final String basePath, final String fullFilenameToAdd) {
        final int prefix = getPrefixLength(fullFilenameToAdd);
        if (prefix < 0) {
            return null;
        }
        if (prefix > 0) {
            return normalize(fullFilenameToAdd);
        }
        if (basePath == null) {
            return null;
        }
        final int len = basePath.length();
        if (len == 0) {
            return normalize(fullFilenameToAdd);
        }
        final char ch = basePath.charAt(len - 1);
        if (isSeparator(ch)) {
            return normalize(basePath + fullFilenameToAdd);
        } else {
            return normalize(basePath + '/' + fullFilenameToAdd);
        }
    }

    /**
     * Determines whether the parent directory contains the child element (a file or directory).
     *
     * <p>The file names are expected to be normalized.</p>
     *
     * <p>Edge cases:</p>
     * <ul>
     * <li>A directory must not be null: if {@code null}, throws IllegalArgumentException</li>
     * <li>A directory does not contain itself: returns false</li>
     * <li>A {@code null} child file is not contained in any parent: returns false</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.directoryContains("/Users/john", "/Users/john/documents");   // Returns true
     * FilenameUtil.directoryContains("/Users/john", "/Users/jane/documents");   // Returns false
     * FilenameUtil.directoryContains("/Users/john", "/Users/john");             // Returns false
     * }</pre>
     *
     * @param canonicalParent the file to consider as the parent, must not be {@code null}
     * @param canonicalChild the file to consider as the child, {@code null} returns {@code false}
     * @return {@code true} if the child is under the parent directory, {@code false} otherwise
     * @throws IllegalArgumentException if canonicalParent is {@code null}
     */
    public static boolean directoryContains(final String canonicalParent, final String canonicalChild) {

        // Fail fast against NullPointerException
        if (canonicalParent == null) {
            throw new IllegalArgumentException("Directory must not be null");
        }

        if ((canonicalChild == null) || IOCase.SYSTEM.checkEquals(canonicalParent, canonicalChild)) {
            return false;
        }

        if (!IOCase.SYSTEM.checkStartsWith(canonicalChild, canonicalParent)) {
            return false;
        }

        if (canonicalParent.length() == 0) {
            return true;
        }

        final char lastParentChar = canonicalParent.charAt(canonicalParent.length() - 1);
        if (isSeparator(lastParentChar)) {
            return true;
        }

        return canonicalChild.length() > canonicalParent.length() && isSeparator(canonicalChild.charAt(canonicalParent.length()));
    }

    //-----------------------------------------------------------------------

    /**
     * Converts all separators to the Unix separator of forward slash.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.separatorsToUnix("C:\\docs\\file.txt");   // Returns "C:/docs/file.txt"
     * FilenameUtil.separatorsToUnix("/already/unix");        // Returns "/already/unix"
     * }</pre>
     *
     * @param path the path to be changed, {@code null} returns {@code null}
     * @return the updated path with Unix separators, or the same reference if no changes were made
     */
    public static String separatorsToUnix(final String path) {
        if (path == null || path.indexOf(WINDOWS_SEPARATOR) == NOT_FOUND) {
            return path;
        }
        return path.replace(WINDOWS_SEPARATOR, UNIX_SEPARATOR);
    }

    /**
     * Converts all separators to the Windows separator of backslash.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.separatorsToWindows("/docs/file.txt");         // Returns "\\docs\\file.txt"
     * FilenameUtil.separatorsToWindows("C:\\already\\windows");   // Returns "C:\\already\\windows"
     * }</pre>
     *
     * @param path the path to be changed, {@code null} returns {@code null}
     * @return the updated path with Windows separators, or the same reference if no changes were made
     */
    public static String separatorsToWindows(final String path) {
        if (path == null || path.indexOf(UNIX_SEPARATOR) == NOT_FOUND) {
            return path;
        }
        return path.replace(UNIX_SEPARATOR, WINDOWS_SEPARATOR);
    }

    /**
     * Converts all separators to the system separator.
     * Calls either {@link #separatorsToUnix(String)} or {@link #separatorsToWindows(String)}
     * based on the current operating system.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // On Windows:
     * FilenameUtil.separatorsToSystem("/docs/file.txt");   // Returns "\\docs\\file.txt"
     *
     * // On Unix/Linux/Mac:
     * FilenameUtil.separatorsToSystem("C:\\docs\\file.txt");   // Returns "C:/docs/file.txt"
     * }</pre>
     *
     * @param path the path to be changed, {@code null} returns {@code null}
     * @return the updated path with system separators, or the same reference if no changes were made
     */
    @MayReturnNull
    public static String separatorsToSystem(final String path) {
        if (path == null) {
            return null;
        }
        if (IOUtil.IS_OS_WINDOWS) {
            return separatorsToWindows(path);
        } else {
            return separatorsToUnix(path);
        }
    }

    //-----------------------------------------------------------------------

    /**
     * Returns the length of the filename prefix, such as {@code C:/} or {@code ~/}.
     *
     * <p>This method handles files in either Unix or Windows format.
     * The prefix length includes the first slash in the full filename if applicable.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Windows:
     * a\b\c.txt           → 0     (relative)
     * \a\b\c.txt          → 1     (current drive absolute)
     * C:a\b\c.txt         → 2     (drive relative)
     * C:\a\b\c.txt        → 3     (absolute)
     * \\server\a\b\c.txt  → 9     (UNC)
     *
     * Unix:
     * a/b/c.txt           → 0     (relative)
     * /a/b/c.txt          → 1     (absolute)
     * ~/a/b/c.txt         → 2     (current user)
     * ~user/a/b/c.txt     → 6     (named user)
     * }</pre>
     *
     * @param filename the filename to find the prefix in, {@code null} returns -1
     * @return the length of the prefix, -1 if invalid or {@code null}
     */
    public static int getPrefixLength(final String filename) {
        if (filename == null) {
            return NOT_FOUND;
        }
        final int len = filename.length();
        if (len == 0) {
            return 0;
        }
        char ch0 = filename.charAt(0);
        if (ch0 == ':') {
            return NOT_FOUND;
        }
        if (len == 1) {
            if (ch0 == '~') {
                return 2; // return a length greater than the input
            }
            return isSeparator(ch0) ? 1 : 0;
        } else {
            if (ch0 == '~') {
                int posUnix = filename.indexOf(UNIX_SEPARATOR, 1);
                int posWin = filename.indexOf(WINDOWS_SEPARATOR, 1);
                if (posUnix == NOT_FOUND && posWin == NOT_FOUND) {
                    return len + 1; // return a length greater than the input
                }
                posUnix = posUnix == NOT_FOUND ? posWin : posUnix;
                posWin = posWin == NOT_FOUND ? posUnix : posWin;
                return Math.min(posUnix, posWin) + 1;
            }
            final char ch1 = filename.charAt(1);
            if (ch1 == ':') {
                ch0 = Character.toUpperCase(ch0);
                if (ch0 >= 'A' && ch0 <= 'Z') {
                    if (len == 2 || !isSeparator(filename.charAt(2))) {
                        return 2;
                    }
                    return 3;
                } else if (ch0 == UNIX_SEPARATOR) {
                    return 1;
                }
                return NOT_FOUND;

            } else if (isSeparator(ch0) && isSeparator(ch1)) {
                int posUnix = filename.indexOf(UNIX_SEPARATOR, 2);
                int posWin = filename.indexOf(WINDOWS_SEPARATOR, 2);
                if ((posUnix == NOT_FOUND && posWin == NOT_FOUND) || posUnix == 2 || posWin == 2) {
                    return NOT_FOUND;
                }
                posUnix = posUnix == NOT_FOUND ? posWin : posUnix;
                posWin = posWin == NOT_FOUND ? posUnix : posWin;
                return Math.min(posUnix, posWin) + 1;
            } else {
                return isSeparator(ch0) ? 1 : 0;
            }
        }
    }

    /**
     * Returns the index of the last directory separator character.
     *
     * <p>This method handles files in either Unix or Windows format.
     * The position of the last forward or backslash is returned.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.indexOfLastSeparator("a/b/c.txt");     // Returns 3
     * FilenameUtil.indexOfLastSeparator("a\\b\\c.txt");   // Returns 3
     * FilenameUtil.indexOfLastSeparator("file.txt");      // Returns -1
     * }</pre>
     *
     * @param filename the filename to find the last path separator in, {@code null} returns -1
     * @return the index of the last separator character, or -1 if there is no such character or if {@code null}
     */
    public static int indexOfLastSeparator(final String filename) {
        if (filename == null) {
            return NOT_FOUND;
        }
        final int lastUnixPos = filename.lastIndexOf(UNIX_SEPARATOR);
        final int lastWindowsPos = filename.lastIndexOf(WINDOWS_SEPARATOR);
        return Math.max(lastUnixPos, lastWindowsPos);
    }

    /**
     * Returns the index of the last extension separator character (dot).
     *
     * <p>This method checks that there is no directory separator after the last dot.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.indexOfExtension("file.txt");       // Returns 4
     * FilenameUtil.indexOfExtension("a/b/file.txt");   // Returns 8
     * FilenameUtil.indexOfExtension("a.txt/b");        // Returns -1 (no extension)
     * FilenameUtil.indexOfExtension("a/b/c");          // Returns -1
     * }</pre>
     *
     * @param filename the filename to find the last extension separator in, {@code null} returns -1
     * @return the index of the last extension separator character, or -1 if there is no such character or if {@code null}
     */
    public static int indexOfExtension(final String filename) {
        if (filename == null) {
            return NOT_FOUND;
        }
        final int extensionPos = filename.lastIndexOf(EXTENSION_SEPARATOR);
        final int lastSeparator = indexOfLastSeparator(filename);
        return lastSeparator > extensionPos ? NOT_FOUND : extensionPos;
    }

    //-----------------------------------------------------------------------

    /**
     * Gets the prefix from a full filename, such as {@code C:/} or {@code ~/}.
     * 
     * <p>This method includes the first slash in the full filename where applicable.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.getPrefix("C:\\a\\b\\c.txt");   // Returns "C:\\"
     * FilenameUtil.getPrefix("/a/b/c.txt");        // Returns "/"
     * FilenameUtil.getPrefix("~/a/b/c.txt");       // Returns "~/"
     * FilenameUtil.getPrefix("a/b/c.txt");         // Returns ""
     * }</pre>
     *
     * @param filename the filename to query, {@code null} returns null
     * @return the prefix of the file, {@code null} if invalid. Null bytes inside string will be removed
     */
    @MayReturnNull
    public static String getPrefix(final String filename) {
        if (filename == null) {
            return null;
        }
        final int len = getPrefixLength(filename);
        if (len < 0) {
            return null;
        }
        if (len > filename.length()) {
            failIfNullBytePresent(filename + UNIX_SEPARATOR);
            return filename + UNIX_SEPARATOR;
        }
        final String path = filename.substring(0, len);
        failIfNullBytePresent(path);
        return path;
    }

    /**
     * Gets the path from a full filename, which excludes the prefix.
     *
     * <p>This method returns the text before and including the last forward or backslash.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.getPath("C:\\a\\b\\c.txt");   // Returns "a\\b\\"
     * FilenameUtil.getPath("~/a/b/c.txt");       // Returns "a/b/"
     * FilenameUtil.getPath("a.txt");             // Returns ""
     * FilenameUtil.getPath("a/b/c");             // Returns "a/b/"
     * }</pre>
     *
     * @param filename the filename to query, {@code null} returns {@code null}
     * @return the path of the file, an empty string if none exists, {@code null} if invalid.
     *         Null bytes inside string will be removed
     * @see #getFullPath(String)
     */
    @MayReturnNull
    public static String getPath(final String filename) {
        return doGetPath(filename, 1);
    }

    /**
     * Gets the path from a full filename, excluding the prefix and final directory separator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.getPathNoEndSeparator("C:\\a\\b\\c.txt");   // Returns "a\\b"
     * FilenameUtil.getPathNoEndSeparator("~/a/b/c.txt");       // Returns "a/b"
     * FilenameUtil.getPathNoEndSeparator("a.txt");             // Returns ""
     * FilenameUtil.getPathNoEndSeparator("a/b/c/");            // Returns "a/b/c"
     * }</pre>
     *
     * @param filename the filename to query, {@code null} returns {@code null}
     * @return the path of the file without trailing separator, an empty string if none exists,
     *         {@code null} if invalid. Null bytes inside string will be removed
     * @see #getFullPathNoEndSeparator(String)
     */
    @MayReturnNull
    public static String getPathNoEndSeparator(final String filename) {
        return doGetPath(filename, 0);
    }

    private static String doGetPath(final String filename, final int separatorAdd) {
        if (filename == null) {
            return null;
        }
        final int prefix = getPrefixLength(filename);
        if (prefix < 0) {
            return null;
        }
        final int index = indexOfLastSeparator(filename);
        final int endIndex = index + separatorAdd;
        if (prefix >= filename.length() || index < 0 || prefix >= endIndex) {
            return "";
        }
        final String path = filename.substring(prefix, endIndex);
        failIfNullBytePresent(path);
        return path;
    }

    /**
     * Gets the full path from a full filename, which is the prefix + path.
     *
     * <p>This method returns the text before and including the last forward or backslash.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.getFullPath("C:\\a\\b\\c.txt");   // Returns "C:\\a\\b\\"
     * FilenameUtil.getFullPath("~/a/b/c.txt");       // Returns "~/a/b/"
     * FilenameUtil.getFullPath("a.txt");             // Returns ""
     * FilenameUtil.getFullPath("C:");                // Returns "C:"
     * }</pre>
     *
     * @param filename the filename to query, {@code null} returns {@code null}
     * @return the path of the file, an empty string if none exists, {@code null} if invalid.
     *         Null bytes inside string will be removed
     */
    @MayReturnNull
    public static String getFullPath(final String filename) {
        return doGetFullPath(filename, true);
    }

    /**
     * Gets the full path from a full filename, excluding the final directory separator.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.getFullPathNoEndSeparator("C:\\a\\b\\c.txt");   // Returns "C:\\a\\b"
     * FilenameUtil.getFullPathNoEndSeparator("~/a/b/c.txt");       // Returns "~/a/b"
     * FilenameUtil.getFullPathNoEndSeparator("a.txt");             // Returns ""
     * FilenameUtil.getFullPathNoEndSeparator("a/b/c/");            // Returns "a/b/c"
     * FilenameUtil.getFullPathNoEndSeparator("C:\\");              // Returns "C:\"
     * }</pre>
     *
     * @param filename the filename to query, {@code null} returns {@code null}
     * @return the path of the file without trailing separator, an empty string if none exists,
     *         {@code null} if invalid. Null bytes inside string will be removed
     */
    @MayReturnNull
    public static String getFullPathNoEndSeparator(final String filename) {
        return doGetFullPath(filename, false);
    }

    private static String doGetFullPath(final String filename, final boolean includeSeparator) {
        if (filename == null) {
            return null;
        }
        final int prefix = getPrefixLength(filename);
        if (prefix < 0) {
            return null;
        }
        if (prefix >= filename.length()) {
            if (includeSeparator) {
                return getPrefix(filename); // add end slash if necessary
            } else {
                return filename;
            }
        }
        final int index = indexOfLastSeparator(filename);
        if (index < 0) {
            return filename.substring(0, prefix);
        }
        int end = index + (includeSeparator ? 1 : 0);
        if (end == 0) {
            end++;
        }
        return filename.substring(0, end);
    }

    /**
     * Gets the name minus the path from a full filename.
     *
     * <p>This method returns the text after the last forward or backslash.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.getName("a/b/c.txt");   // Returns "c.txt"
     * FilenameUtil.getName("a.txt");       // Returns "a.txt"
     * FilenameUtil.getName("a/b/c");       // Returns "c"
     * FilenameUtil.getName("a/b/c/");      // Returns ""
     * }</pre>
     *
     * @param filename the filename to query, {@code null} returns {@code null}
     * @return the name of the file without the path, or an empty string if none exists,
     *         or {@code null} if the filename is {@code null}. Null bytes inside string will be removed
     * @throws IllegalArgumentException if the supplied filename contains {@code null} bytes
     */
    @MayReturnNull
    public static String getName(final String filename) {
        if (filename == null) {
            return null;
        }
        failIfNullBytePresent(filename);
        final int index = indexOfLastSeparator(filename);
        return filename.substring(index + 1);
    }

    /**
     * Check the input for {@code null} bytes, a sign of unsanitized data being passed to file level functions.
     *
     * This may be used for poison byte attacks.
     * @param path the path to check
     */
    private static void failIfNullBytePresent(final String path) {
        final int len = path.length();
        for (int i = 0; i < len; i++) {
            if (path.charAt(i) == 0) {
                throw new IllegalArgumentException("Null byte present in file/path name. There are no "
                        + "known legitimate use cases for such data, but several injection attacks may use it");
            }
        }
    }

    /**
     * Gets the base name minus the full path and extension from a full filename.
     *
     * <p>This method returns the text after the last separator and before the last dot.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.getBaseName("a/b/c.txt");   // Returns "c"
     * FilenameUtil.getBaseName("a.txt");       // Returns "a"
     * FilenameUtil.getBaseName("a/b/c");       // Returns "c"
     * FilenameUtil.getBaseName("a/b/c/");      // Returns ""
     * }</pre>
     *
     * @param filename the filename to query, {@code null} returns {@code null}
     * @return the name of the file without path or extension, or {@code null} if the filename is {@code null}.
     *         Null bytes inside string will be removed
     * @see #getName(String)
     * @see #getExtension(String)
     * @see #removeExtension(String)
     */
    @MayReturnNull
    public static String getBaseName(final String filename) {
        return removeExtension(getName(filename));
    }

    /**
     * Gets the extension of a filename.
     *
     * <p>This method returns the text after the last dot. There must be no directory separator after the dot.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.getExtension("foo.txt");     // Returns "txt"
     * FilenameUtil.getExtension("a/b/c.jpg");   // Returns "jpg"
     * FilenameUtil.getExtension("a/b.txt/c");   // Returns ""
     * FilenameUtil.getExtension("a/b/c");       // Returns ""
     * }</pre>
     *
     * @param filename the filename to retrieve the extension of, {@code null} returns {@code null}
     * @return the extension of the file or an empty string if none exists,
     *         or {@code null} if the filename is {@code null}
     * @see #getBaseName(String)
     * @see #removeExtension(String)
     */
    @MayReturnNull
    public static String getExtension(final String filename) {
        if (filename == null) {
            return null;
        }
        final int index = indexOfExtension(filename);
        if (index == NOT_FOUND) {
            return "";
        } else {
            return filename.substring(index + 1);
        }
    }

    //-----------------------------------------------------------------------

    /**
     * Removes the file extension from a filename.
     *
     * <p>This method removes the extension (the suffix starting from the last dot '.')
     * from the given filename. If no extension is found, the original filename is returned
     * unchanged.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.removeExtension("foo.txt");       // Returns "foo"
     * FilenameUtil.removeExtension("a\\b\\c.jpg");   // Returns "a\\b\\c"
     * FilenameUtil.removeExtension("a\\b\\c");       // Returns "a\\b\\c"
     * FilenameUtil.removeExtension("a.b\\c");        // Returns "a.b\\c"
     * FilenameUtil.removeExtension(null);            // Returns null
     * }</pre>
     *
     * @param filename the filename to query, {@code null} returns {@code null}
     * @return the filename minus the extension, or {@code null} if the filename is {@code null}
     * @throws IllegalArgumentException if the supplied filename contains {@code null} bytes
     * @see #getBaseName(String)
     * @see #getExtension(String)
     */
    @MayReturnNull
    public static String removeExtension(final String filename) {
        if (filename == null) {
            return null;
        }
        failIfNullBytePresent(filename);

        final int index = indexOfExtension(filename);
        if (index == NOT_FOUND) {
            return filename;
        } else {
            return filename.substring(0, index);
        }
    }

    //-----------------------------------------------------------------------

    /**
     * Checks whether two filenames are equal exactly.
     *
     * <p>No processing is performed on the filenames other than comparison.
     * This is a null-safe case-sensitive equals.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.equals("file.txt", "file.txt");   // Returns true
     * FilenameUtil.equals("file.txt", "FILE.TXT");   // Returns false
     * FilenameUtil.equals(null, null);               // Returns true
     * }</pre>
     *
     * @param filename1 the first filename to query, {@code null} is allowed
     * @param filename2 the second filename to query, {@code null} is allowed
     * @return {@code true} if the filenames are equal, {@code null} equals {@code null}
     * @see IOCase#SENSITIVE
     */
    public static boolean equals(final String filename1, final String filename2) {
        return equals(filename1, filename2, false, IOCase.SENSITIVE);
    }

    /**
     * Checks whether two filenames are equal using the case rules of the system.
     *
     * <p>The check is case-sensitive on Unix and case-insensitive on Windows.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // On Windows:
     * FilenameUtil.equalsOnSystem("file.txt", "FILE.TXT");   // Returns true
     *
     * // On Unix/Linux:
     * FilenameUtil.equalsOnSystem("file.txt", "FILE.TXT");   // Returns false
     * }</pre>
     *
     * @param filename1 the first filename to query, {@code null} is allowed
     * @param filename2 the second filename to query, {@code null} is allowed
     * @return {@code true} if the filenames are equal, {@code null} equals {@code null}
     * @see IOCase#SYSTEM
     */
    public static boolean equalsOnSystem(final String filename1, final String filename2) {
        return equals(filename1, filename2, false, IOCase.SYSTEM);
    }

    //-----------------------------------------------------------------------

    /**
     * Checks whether two filenames are equal after both have been normalized.
     *
     * <p>Both filenames are first passed to {@link #normalize(String)}.
     * The check is then performed in a case-sensitive manner.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.equalsNormalized("/foo/bar", "/foo/./bar");        // Returns true
     * FilenameUtil.equalsNormalized("/foo/bar", "/foo/../foo/bar");   // Returns true
     * }</pre>
     *
     * @param filename1 the first filename to query, {@code null} is allowed
     * @param filename2 the second filename to query, {@code null} is allowed
     * @return {@code true} if the filenames are equal, {@code null} equals {@code null}
     * @see IOCase#SENSITIVE
     */
    public static boolean equalsNormalized(final String filename1, final String filename2) {
        return equals(filename1, filename2, true, IOCase.SENSITIVE);
    }

    /**
     * Checks whether two filenames are equal after normalization and using system case rules.
     *
     * <p>Both filenames are first normalized, then compared using system case sensitivity.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // On Windows:
     * FilenameUtil.equalsNormalizedOnSystem("/foo/bar", "/FOO/./BAR");   // Returns true
     *
     * // On Unix:
     * FilenameUtil.equalsNormalizedOnSystem("/foo/bar", "/FOO/./BAR");   // Returns false
     * }</pre>
     *
     * @param filename1 the first filename to query, {@code null} is allowed
     * @param filename2 the second filename to query, {@code null} is allowed
     * @return {@code true} if the filenames are equal, {@code null} equals {@code null}
     * @see IOCase#SYSTEM
     */
    public static boolean equalsNormalizedOnSystem(final String filename1, final String filename2) {
        return equals(filename1, filename2, true, IOCase.SYSTEM);
    }

    /**
     * Checks whether two filenames are equal with full control over processing and case sensitivity.
     *
     * <p>This method provides comprehensive control over filename comparison by allowing
     * normalization and custom case sensitivity rules. When normalization is enabled,
     * both filenames are normalized before comparison using {@link #normalize(String)}.</p>
     *
     * <p>The comparison can be case-sensitive or case-insensitive based on the provided
     * {@code IOCase} parameter. If {@code null} is provided, defaults to case-sensitive.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.equals("/foo/bar", "/foo/./bar", true, IOCase.SENSITIVE);    // Returns true
     * FilenameUtil.equals("file.txt", "FILE.TXT", false, IOCase.INSENSITIVE);   // Returns true
     * }</pre>
     *
     * @param filename1 the first filename to query, {@code null} is allowed
     * @param filename2 the second filename to query, {@code null} is allowed
     * @param normalized whether to normalize the filenames before comparison
     * @param caseSensitivity what case sensitivity rule to use, {@code null} means case-sensitive
     * @return {@code true} if the filenames are equal, {@code null} equals {@code null}
     * @throws IllegalArgumentException if normalizing fails (when {@code normalized} is {@code true})
     */
    public static boolean equals(String filename1, String filename2, final boolean normalized, IOCase caseSensitivity) {
        if (filename1 == null || filename2 == null) {
            return filename1 == null && filename2 == null;
        }
        if (normalized) {
            filename1 = normalize(filename1);
            filename2 = normalize(filename2);
            if (filename1 == null || filename2 == null) {
                throw new IllegalArgumentException("Error normalizing one or both of the file names");
            }
        }
        if (caseSensitivity == null) {
            caseSensitivity = IOCase.SENSITIVE;
        }
        return caseSensitivity.checkEquals(filename1, filename2);
    }

    //-----------------------------------------------------------------------

    /**
     * Checks whether the extension of the filename is that specified.
     *
     * <p>This method obtains the extension as the text after the last dot.
     * The extension check is case-sensitive on all platforms.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.isExtension("file.txt", "txt");   // Returns true
     * FilenameUtil.isExtension("file.txt", "TXT");   // Returns false
     * FilenameUtil.isExtension("file", "");          // Returns true
     * FilenameUtil.isExtension("file.txt", null);    // Returns false
     * }</pre>
     *
     * @param filename the filename to query, {@code null} returns {@code false}
     * @param extension the extension to check for, {@code null} or empty checks for no extension
     * @return {@code true} if the filename has the specified extension
     * @throws IllegalArgumentException if the supplied filename contains {@code null} bytes
     */
    public static boolean isExtension(final String filename, final String extension) {
        if (filename == null) {
            return false;
        }
        failIfNullBytePresent(filename);

        if (extension == null || extension.isEmpty()) {
            return indexOfExtension(filename) == NOT_FOUND;
        }
        final String fileExt = getExtension(filename);
        return fileExt.equals(extension);
    }

    /**
     * Checks whether the extension of the filename is one of those specified.
     *
     * <p>The extension check is case-sensitive on all platforms.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] exts = {"txt", "xml", "json"};
     * FilenameUtil.isExtension("file.txt", exts);   // Returns true
     * FilenameUtil.isExtension("file.doc", exts);   // Returns false
     * }</pre>
     *
     * @param filename the filename to query, {@code null} returns {@code false}
     * @param extensions the extensions to check for, {@code null} or empty array checks for no extension
     * @return {@code true} if the filename is one of the extensions
     * @throws IllegalArgumentException if the supplied filename contains {@code null} bytes
     */
    public static boolean isExtension(final String filename, final String[] extensions) {
        if (filename == null) {
            return false;
        }
        failIfNullBytePresent(filename);

        if (extensions == null || extensions.length == 0) {
            return indexOfExtension(filename) == NOT_FOUND;
        }
        final String fileExt = getExtension(filename);
        for (final String extension : extensions) {
            if (fileExt.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks whether the extension of the filename is one of those specified.
     *
     * <p>The extension check is case-sensitive on all platforms.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Collection<String> exts = Arrays.asList("txt", "xml", "json");
     * FilenameUtil.isExtension("file.txt", exts);   // Returns true
     * FilenameUtil.isExtension("file.doc", exts);   // Returns false
     * }</pre>
     *
     * @param filename the filename to query, {@code null} returns {@code false}
     * @param extensions the extensions to check for, {@code null} or empty collection checks for no extension
     * @return {@code true} if the filename is one of the extensions
     * @throws IllegalArgumentException if the supplied filename contains {@code null} bytes
     */
    public static boolean isExtension(final String filename, final Collection<String> extensions) {
        if (filename == null) {
            return false;
        }
        failIfNullBytePresent(filename);

        if (extensions == null || extensions.isEmpty()) {
            return indexOfExtension(filename) == NOT_FOUND;
        }
        final String fileExt = getExtension(filename);
        for (final String extension : extensions) {
            if (fileExt.equals(extension)) {
                return true;
            }
        }
        return false;
    }

    //-----------------------------------------------------------------------

    /**
     * Checks a filename to see if it matches the specified wildcard matcher.
     *
     * <p>The wildcard matcher uses '?' and '*' to represent single or multiple wildcard characters.
     * The check is case-sensitive always.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.wildcardMatch("c.txt", "*.txt");       // Returns true
     * FilenameUtil.wildcardMatch("c.txt", "*.jpg");       // Returns false
     * FilenameUtil.wildcardMatch("a/b/c.txt", "a/b/*");   // Returns true
     * FilenameUtil.wildcardMatch("c.txt", "*.???");       // Returns true
     * FilenameUtil.wildcardMatch("c.txt", "*.????");      // Returns false
     * }</pre>
     *
     * @param filename the filename to match on, {@code null} returns {@code true} only if wildcardMatcher is also {@code null}
     * @param wildcardMatcher the wildcard string to match against, {@code null} returns {@code true} only if filename is also {@code null}
     * @return {@code true} if the filename matches the wildcard string
     * @see IOCase#SENSITIVE
     */
    public static boolean wildcardMatch(final String filename, final String wildcardMatcher) {
        return wildcardMatch(filename, wildcardMatcher, IOCase.SENSITIVE);
    }

    /**
     * Checks a filename against a wildcard matcher using system case rules.
     *
     * <p>The check is case-sensitive on Unix and case-insensitive on Windows.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // On Windows:
     * FilenameUtil.wildcardMatchOnSystem("file.TXT", "*.txt");   // Returns true
     *
     * // On Unix:
     * FilenameUtil.wildcardMatchOnSystem("file.TXT", "*.txt");   // Returns false
     * }</pre>
     *
     * @param filename the filename to match on, {@code null} returns {@code true} only if wildcardMatcher is also {@code null}
     * @param wildcardMatcher the wildcard string to match against, {@code null} returns {@code true} only if filename is also {@code null}
     * @return {@code true} if the filename matches the wildcard string
     * @see IOCase#SYSTEM
     */
    public static boolean wildcardMatchOnSystem(final String filename, final String wildcardMatcher) {
        return wildcardMatch(filename, wildcardMatcher, IOCase.SYSTEM);
    }

    /**
     * Checks a filename against a wildcard matcher with full control over case-sensitivity.
     *
     * <p>The wildcard matcher uses the characters '?' and '*' to represent a
     * single or multiple (zero or more) wildcard characters. This method provides
     * complete control over case sensitivity rules during matching.</p>
     *
     * <p>Wildcard semantics:</p>
     * <ul>
     * <li>'?' matches exactly one character</li>
     * <li>'*' matches zero or more characters</li>
     * <li>All other characters match themselves</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * FilenameUtil.wildcardMatch("file.txt", "*.txt", IOCase.SENSITIVE);     // Returns true
     * FilenameUtil.wildcardMatch("FILE.TXT", "*.txt", IOCase.INSENSITIVE);   // Returns true
     * FilenameUtil.wildcardMatch("FILE.TXT", "*.txt", IOCase.SENSITIVE);     // Returns false
     * }</pre>
     *
     * @param filename the filename to match on, {@code null} returns {@code true} only if wildcardMatcher is also {@code null}
     * @param wildcardMatcher the wildcard string to match against, {@code null} returns {@code true} only if filename is also {@code null}
     * @param caseSensitivity what case sensitivity rule to use, {@code null} means case-sensitive
     * @return {@code true} if the filename matches the wildcard string, both {@code null} returns {@code true}
     */
    public static boolean wildcardMatch(final String filename, final String wildcardMatcher, IOCase caseSensitivity) {
        if (filename == null && wildcardMatcher == null) {
            return true;
        }
        if (filename == null || wildcardMatcher == null) {
            return false;
        }
        if (caseSensitivity == null) {
            caseSensitivity = IOCase.SENSITIVE;
        }
        final String[] wcs = splitOnTokens(wildcardMatcher);
        boolean anyChars = false;
        int textIdx = 0;
        int wcsIdx = 0;
        final Stack<int[]> backtrack = new Stack<>(); //NOSONAR

        // loop around a backtrack stack, to handle complex * matching
        do {
            if (backtrack.size() > 0) {
                final int[] array = backtrack.pop();
                wcsIdx = array[0];
                textIdx = array[1];
                anyChars = true;
            }

            // loop whilst tokens and text left to process
            while (wcsIdx < wcs.length) {

                if (wcs[wcsIdx].equals("?")) {
                    // ? so move to next text char
                    textIdx++;
                    if (textIdx > filename.length()) {
                        break;
                    }
                    anyChars = false;

                } else if (wcs[wcsIdx].equals("*")) {
                    // set any chars status
                    anyChars = true;
                    if (wcsIdx == wcs.length - 1) {
                        textIdx = filename.length();
                    }

                } else {
                    // matching text token
                    if (anyChars) {
                        // any chars then try to locate text token
                        textIdx = caseSensitivity.checkIndexOf(filename, textIdx, wcs[wcsIdx]);
                        if (textIdx == NOT_FOUND) {
                            // token not found
                            break;
                        }
                        final int repeat = caseSensitivity.checkIndexOf(filename, textIdx + 1, wcs[wcsIdx]);
                        if (repeat >= 0) {
                            backtrack.push(new int[] { wcsIdx, repeat });
                        }
                    } else {
                        // matching from current position
                        if (!caseSensitivity.checkRegionMatches(filename, textIdx, wcs[wcsIdx])) {
                            // couldn't match token
                            break;
                        }
                    }

                    // matched text token, move text index to the end of matched token
                    textIdx += wcs[wcsIdx].length();
                    anyChars = false;
                }

                wcsIdx++;
            }

            // full match
            if (wcsIdx == wcs.length && textIdx == filename.length()) {
                return true;
            }

        } while (backtrack.size() > 0);

        return false;
    }

    /**
     * Splits a wildcard pattern string into a sequence of tokens for matching.
     *
     * <p>This method tokenizes wildcard patterns by splitting on the special
     * characters '?' and '*'. The resulting tokens are used internally for
     * efficient wildcard matching operations.</p>
     *
     * <p>Tokenization rules:</p>
     * <ul>
     * <li>Text between wildcards becomes a literal token</li>
     * <li>Each '?' becomes a separate "?" token</li>
     * <li>Each '*' becomes a separate "*" token</li>
     * <li>Multiple consecutive '*' characters are collapsed into a single "*" token</li>
     * <li>If no wildcards exist, returns the original text as a single-element array</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * splitOnTokens("*.txt");       // Returns ["*", ".txt"]
     * splitOnTokens("file?.txt");   // Returns ["file", "?", ".txt"]
     * splitOnTokens("a**b");        // Returns ["a", "*", "b"]
     * splitOnTokens("plain");       // Returns ["plain"]
     * }</pre>
     *
     * @param text the wildcard pattern text to split, must not be {@code null}
     * @return the array of tokens representing the pattern, never {@code null}
     */
    static String[] splitOnTokens(final String text) {
        // used by wildcardMatch
        // package level so a unit test may run on this

        if (text.indexOf('?') == NOT_FOUND && text.indexOf('*') == NOT_FOUND) {
            return new String[] { text };
        }

        final char[] array = text.toCharArray();
        final ArrayList<String> list = new ArrayList<>();
        final StringBuilder buffer = new StringBuilder();
        char prevChar = 0;
        for (final char ch : array) {
            if (ch == '?' || ch == '*') {
                if (!buffer.isEmpty()) {
                    list.add(buffer.toString());
                    buffer.setLength(0);
                }
                if (ch == '?') {
                    list.add("?");
                } else if (prevChar != '*') {// ch == '*' here; check if previous char was '*'
                    list.add("*");
                }
            } else {
                buffer.append(ch);
            }
            prevChar = ch;
        }
        if (!buffer.isEmpty()) {
            list.add(buffer.toString());
        }

        return list.toArray(new String[0]);
    }
}
