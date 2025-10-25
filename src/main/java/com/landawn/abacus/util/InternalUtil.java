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
 * Internal utility class for the Abacus library.
 * 
 * <p><strong>WARNING:</strong> This class is for internal use only. Do not use any methods
 * in this class directly. The API is subject to change without notice and may break
 * your code in future versions.</p>
 * 
 * <p>This class contains various internal constants and helper methods used throughout
 * the Abacus library for performance optimization and internal operations.</p>
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
     * Standard error message for NoSuchElementException when a target object/value does not exist or is null.
     */
    public static final String ERROR_MSG_FOR_NULL_ELEMENT_EX = "Target object/value does not exist or is not found, or its value is null";

    // To avoid SpotBugs: CD_CIRCULAR_DEPENDENCY, IC_INIT_CIRCULARITY
    static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    /**
     * Internal pool size calculated based on available memory.
     * 
     * <p>The pool size is dynamically calculated based on the JVM's maximum memory,
     * ranging from 1000 to 8192 elements.</p>
     *
     * @deprecated DO NOT call the methods defined in this class. it's for internal use only.
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
     * <p>This method uses reflection to access the internal elementData field of ArrayList
     * for performance optimization. If reflection fails or the collection is not an ArrayList,
     * returns null.</p>
     *
     * @param c the collection to get the internal array from
     * @return the internal array if accessible, null otherwise
     * @deprecated DO NOT call the methods defined in this class. it's for internal use only.
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
     * Creates an ArrayList by initializing its elements with the specified array.
     * 
     * <p>The returned list may share the same elements array with the input array.
     * Any modification to the list/array will affect the array/list.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String[] array = {"a", "b", "c"};
     * List<String> list = InternalUtil.createList(array);
     * // Modifications to list may affect array and vice versa
     * }</pre>
     *
     * @param <T> the element type
     * @param a the array to create the list from
     * @return a new ArrayList backed by the array
     * @deprecated DO NOT call the methods defined in this class. it's for internal use only.
     */
    @Deprecated
    @Beta
    @SafeVarargs
    static <T> List<T> createList(final T... a) {
        if (N.isEmpty(a)) {
            return new ArrayList<>();
        }

        return N.asList(a);
    }

    /**
     * Gets the character array from a string for read-only purposes.
     * 
     * <p>This method attempts to optimize string to char array conversion.
     * The returned array should not be modified as it may be shared with the original string.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = InternalUtil.getCharsForReadOnly("Hello");
     * // Use chars for reading only, do not modify
     * }</pre>
     *
     * @param str the string to get characters from
     * @return a character array (may be shared, do not modify)
     * @deprecated DO NOT call the methods defined in this class. it's for internal use only.
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
        // singleton for utility class
    }
}