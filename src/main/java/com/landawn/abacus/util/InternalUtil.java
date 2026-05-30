/*
 * Copyright (c) 2019, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;

/**
 * Internal utility class for the abacus-common library.
 *
 * <p><strong>WARNING:</strong> This class is for internal use only. Do not use any methods
 * in this class directly. The API is subject to change without notice and may break
 * your code in future versions.</p>
 *
 * <p>This class contains various internal constants and helper methods used throughout
 * the abacus-common library for performance optimization and internal operations.</p>
 *
 * @see Internal
 */
@Internal
public final class InternalUtil {
    /**
     * Standard error message for NoSuchElementException when a target object/value does not exist.
     */
    public static final String ERROR_MSG_FOR_NO_SUCH_EX = "Target object/value does not exist or is not found";

    /**
     * Standard error message for NoSuchElementException when a target object/value does not exist or is {@code null}.
     */
    public static final String ERROR_MSG_FOR_NULL_ELEMENT_EX = "Target object/value does not exist or is not found, or its value is null";

    // To avoid SpotBugs: CD_CIRCULAR_DEPENDENCY, IC_INIT_CIRCULARITY
    /**
     * The number of CPU cores available to the JVM, obtained at class initialization time.
     * Used internally to tune parallel operation thresholds.
     */
    static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    /**
     * Internal pool size calculated based on available memory.
     *
     * <p>The pool size is dynamically calculated based on the JVM's maximum memory,
     * ranging from 1000 to 8192 elements.</p>
     *
     * @deprecated This field is for internal use only. Do not access it directly.
     */
    @Deprecated
    public static final int POOL_SIZE;

    static {
        final int multi = (int) (Runtime.getRuntime().maxMemory() / ((1024 * 1024) * 256));

        POOL_SIZE = Math.max(1000, Math.min(1000 * multi, 8192));
    }

    // ...
    static final Field listElementDataField;

    static volatile boolean isListElementDataFieldGettable = true;

    static {
        Field tmp = null;

        try {
            tmp = ArrayList.class.getDeclaredField("elementData");
        } catch (final Throwable e) {
            // ignore.
        }

        listElementDataField = tmp != null && tmp.getType().equals(Object[].class) ? tmp : null;

        if (listElementDataField != null) {
            try {
                listElementDataField.setAccessible(true); //NOSONAR
                isListElementDataFieldGettable = listElementDataField.canAccess(new ArrayList<>());
            } catch (final Throwable e) {
                // ignore.
            }
        }
    }

    /**
     * Attempts to get the internal array backing an ArrayList without copying.
     *
     * <p>This method uses reflection to access the internal {@code elementData} field of {@link ArrayList}
     * for performance optimization. It returns {@code null} if {@code c} is {@code null}, is not an
     * {@code ArrayList} instance (subclasses are not accepted), or if the reflective access fails.</p>
     *
     * @param c the collection to get the internal array from; may be {@code null}
     * @return the internal backing array if accessible, {@code null} otherwise
     * @deprecated DO NOT call the methods defined in this class. It's for internal use only.
     */
    @Deprecated
    @Beta
    static Object[] getInternalArray(final Collection<?> c) {
        if (c == null) {
            return null; // NOSONAR
        }

        if (isListElementDataFieldGettable && listElementDataField != null && c.getClass().equals(ArrayList.class)) {
            try {
                return (Object[]) listElementDataField.get(c);
            } catch (final Throwable e) {
                // ignore;
                isListElementDataFieldGettable = false;
            }

        }

        return null; // NOSONAR
    }

    /**
     * Creates a new, modifiable {@link java.util.ArrayList} containing the elements of the specified array.
     *
     * <p>The returned list is independent of the input array — changes to the array after
     * this call do not affect the returned list, and vice versa.
     * An empty {@code ArrayList} is returned if {@code a} is {@code null} or empty.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] array = {"a", "b", "c"};
     * List<String> list = InternalUtil.createList(array);
     * // list is independent of array
     * }</pre>
     *
     * @param <T> the element type
     * @param a the array whose elements are to be placed into the list; may be {@code null} or empty
     * @return a new {@code ArrayList} containing the elements of {@code a}, or an empty list if {@code a} is {@code null} or empty
     * @deprecated DO NOT call the methods defined in this class. It's for internal use only.
     */
    @Deprecated
    @Beta
    @SafeVarargs
    static <T> List<T> createList(final T... a) {
        if (N.isEmpty(a)) {
            return new ArrayList<>();
        }

        return N.toList(a);
    }

    /**
     * Returns a character array containing the characters of the given string.
     *
     * <p>This method is intended for internal read-only access to a string's characters.
     * The returned array is a copy produced by {@link String#toCharArray()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = InternalUtil.getCharsForReadOnly("Hello");
     * // Use chars for reading only
     * }</pre>
     *
     * @param str the string whose characters are to be returned; may be {@code null} or empty
     * @return a {@code char[]} containing the characters of {@code str};
     *         an empty array if {@code str} is {@code null} or empty
     * @deprecated DO NOT call the methods defined in this class. It's for internal use only.
     */
    @Deprecated
    @Beta
    public static char[] getCharsForReadOnly(final String str) {
        if (N.isEmpty(str)) {
            return N.EMPTY_CHAR_ARRAY;
        }

        return str.toCharArray();
    }

    /**
     * Private constructor to prevent instantiation of this utility class.
     */
    private InternalUtil() {
        // Utility class - prevent instantiation
    }
}
