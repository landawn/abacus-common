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

@Internal
public final class InternalUtil {
    public static final String ERROR_MSG_FOR_NO_SUCH_EX = "Target object/value does not exist or is not found";
    public static final String ERROR_MSG_FOR_NULL_ELEMENT_EX = "Target object/value does not exist or is not found, or its value is null";

    // To avoid SpotBugs: CD_CIRCULAR_DEPENDENCY, IC_INIT_CIRCULARITY
    static final int CPU_CORES = Runtime.getRuntime().availableProcessors();

    /**
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
     *
     * @param c
     * @return
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
     * Create an array list by initializing its elements with the specified array {@code a}.
     * The returned list may share the same elements with the specified array {@code a}.
     * That's to say, any change on the List/Array will affect the Array/List.
     *
     * @param <T>
     * @param a
     * @return
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

    //    static volatile boolean isStringCharsGettable = JavaVersion.of(System.getProperty("java.version")).atMost(JavaVersion.JAVA_1_8);
    //    static volatile boolean isStringCharsCreatable = JavaVersion.of(System.getProperty("java.version")).atMost(JavaVersion.JAVA_1_8);
    //
    //    static final Field strValueField;
    //    static final Constructor<String> sharedStringConstructor;
    //
    //    static {
    //        Field tmp = null;
    //
    //        try {
    //            tmp = String.class.getDeclaredField("value");
    //        } catch (Throwable e) {
    //            // ignore.
    //        }
    //
    //        strValueField = ((tmp != null) && tmp.getName().equals("value") && tmp.getType().equals(char[].class)) ? tmp : null;
    //
    //        if (strValueField != null) {
    //            ClassUtil.setAccessibleQuietly(strValueField, true);
    //        }
    //
    //        Constructor<String> tmpConstructor = null;
    //
    //        try {
    //            tmpConstructor = String.class.getDeclaredConstructor(char[].class, boolean.class);
    //            ClassUtil.setAccessibleQuietly(tmpConstructor, true);
    //        } catch (Throwable e) {
    //            // ignore.
    //        }
    //
    //        sharedStringConstructor = tmpConstructor;
    //    }

    /**
     * Gets the chars for read-only.
     *
     * @param str
     * @return
     * @deprecated DO NOT call the methods defined in this class. it's for internal use only.
     */
    @Deprecated
    @Beta
    public static char[] getCharsForReadOnly(final String str) {
        //    if (isStringCharsGettable && strValueField != null && str.length() > 3) {
        //        try {
        //            final char[] chars = (char[]) strValueField.get(str);
        //
        //            if (chars.length == str.length()) {
        //                return chars;
        //            } else {
        //                isStringCharsGettable = false;
        //            }
        //
        //        } catch (Throwable e) {
        //            // ignore.
        //            isStringCharsGettable = false;
        //        }
        //    }

        if (N.isEmpty(str)) {
            return N.EMPTY_CHAR_ARRAY;
        }

        return str.toCharArray();
    }

    //    /**
    //     *
    //     * @param a the array should not be modified after it's used to
    //     *            create the new String.
    //     * @param share the same array will be shared with the new created ArrayList
    //     *            if it's true.
    //     * @return
    //     * @deprecated DO NOT call the methods defined in this class. it's for internal use only.
    //     */
    //    @Deprecated
    //    @Beta
    //    static String newString(final char[] a, @SuppressWarnings("unused") final boolean share) {
    //        //    if (isStringCharsCreatable && share && sharedStringConstructor != null) {
    //        //        try {
    //        //            return sharedStringConstructor.newInstance(a, true);
    //        //        } catch (Throwable e) {
    //        //            // ignore
    //        //            isStringCharsCreatable = false;
    //        //        }
    //        //    }
    //
    //        return String.valueOf(a);
    //    }

    //    /**
    //     * Checks if it's not null or default. {@code null} is default value for all reference types, {@code false} is default value for primitive boolean, {@code 0} is the default value for primitive number type.
    //     *
    //     *
    //     * @param s
    //     * @return true, if it's not null or default
    //     * @deprecated internal only
    //     */
    //    @Deprecated
    //    @Internal
    //    @Beta
    //    static boolean notNullOrDefault(final Object value) {
    //        return (value != null) && !equals(value, defaultValueOf(value.getClass()));
    //    }

    //    /**
    //     * Checks if is null or default.
    //     * {@code null} is default value for all reference types,
    //     * {@code false} is default value for primitive boolean, {@code 0} is the default value for a primitive number type.
    //     *
    //     * @param s
    //     * @return true, if it is null or default
    //     * @deprecated DO NOT call the methods defined in this class.
    //     It's for internal use only.
    //     */
    //    @Deprecated
    //    static boolean isNullOrDefault(final Object value) {
    //        return (value == null) || N.equals(value, N.defaultValueOf(value.getClass()));
    //    }

    //    /**
    //     * Checks if it's not null or default.
    //     * {@code null} is default value for all reference types,
    //     * {@code false} is default value for primitive boolean, {@code 0} is the default value for a primitive number type.
    //     *
    //     *
    //     * @param s
    //     * @return true, if it's not null or default
    //     * @deprecated DO NOT call the methods defined in this class.
    //     It's for internal use only.
    //     */
    //    @Deprecated
    //    static boolean notNullOrDefault(final Object value) {
    //        return (value != null) && !N.equals(value, N.defaultValueOf(value.getClass()));
    //    }

    private InternalUtil() {
        // singleton for utility class
    }
}
