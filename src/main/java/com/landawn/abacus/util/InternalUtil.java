/*
 * Copyright (c) 2019, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.landawn.abacus.util;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.Internal;

/**  
 * 
 * @deprecated DO NOT call the methods defined in this class. it's for internal use only.
 */
@Deprecated
@Internal
@Beta
public final class InternalUtil {
    private InternalUtil() {
        // singleton for utility class
    }

    /** The Constant listElementDataField. */
    // ...
    static final Field listElementDataField;

    /** The Constant listSizeField. */
    static final Field listSizeField;

    /** The is list element data field gettable. */
    static volatile boolean isListElementDataFieldGettable = true;

    /** The is list element data field settable. */
    static volatile boolean isListElementDataFieldSettable = true;

    static {
        Field tmp = null;

        try {
            tmp = ArrayList.class.getDeclaredField("elementData");
        } catch (Throwable e) {
            // ignore.
        }

        listElementDataField = tmp != null && tmp.getType().equals(Object[].class) ? tmp : null;

        if (listElementDataField != null) {
            ClassUtil.setAccessibleQuietly(listElementDataField, true);
        }

        tmp = null;

        try {
            tmp = ArrayList.class.getDeclaredField("size");
        } catch (Throwable e) {
            // ignore.
        }

        listSizeField = tmp != null && tmp.getType().equals(int.class) ? tmp : null;

        if (listSizeField != null) {
            ClassUtil.setAccessibleQuietly(listSizeField, true);
        }
    }

    /**
     * 
     * @param c
     * @return
     * @deprecated internal use only
     */
    @Deprecated
    static Object[] getInternalArray(final Collection<?> c) {
        if (c == null) {
            return null;
        }

        if (isListElementDataFieldGettable && listElementDataField != null && c.getClass().equals(ArrayList.class)) {
            try {
                return (Object[]) listElementDataField.get(c);
            } catch (Throwable e) {
                // ignore;
                isListElementDataFieldGettable = false;
            }

        }

        return null;
    }

    /**
     * Create an array list by initializing its elements data with the specified array <code>a</code>.
     * The returned list may share the same elements with the specified array <code>a</code>.
     * That's to say any change on the List/Array will affect the Array/List.
     *
     * @param <T>
     * @param a
     * @return
     */
    @SafeVarargs
    static <T> List<T> createList(final T... a) {
        if (CommonUtil.isNullOrEmpty(a)) {
            return new ArrayList<>();
        }

        if (isListElementDataFieldSettable && listElementDataField != null && listSizeField != null) {
            final List<T> list = new ArrayList<>();

            try {
                listElementDataField.set(list, a);
                listSizeField.set(list, a.length);

                return list;
            } catch (Throwable e) {
                // ignore;
                isListElementDataFieldSettable = false;
            }
        }

        return CommonUtil.asList(a);
    }

    /** The Constant strValueField. */
    static final Field strValueField;

    /** The is string chars gettable. */
    static volatile boolean isStringCharsGettable = true;

    /** The Constant sharedStringConstructor. */
    static final Constructor<String> sharedStringConstructor;

    static {
        Field tmp = null;

        strValueField = ((tmp != null) && tmp.getName().equals("value") && tmp.getType().equals(char[].class)) ? tmp : null;

        if (strValueField != null) {
            ClassUtil.setAccessibleQuietly(strValueField, true);
        }

        Constructor<String> tmpConstructor = null;

        try {
            tmpConstructor = String.class.getDeclaredConstructor(char[].class, boolean.class);
            ClassUtil.setAccessibleQuietly(tmpConstructor, true);
        } catch (Exception e) {
            // ignore.
        }

        sharedStringConstructor = tmpConstructor;
    }

    /**
     * Gets the chars for read only.
     *
     * @param str
     * @return
     */
    public static char[] getCharsForReadOnly(final String str) {
        if (isStringCharsGettable && strValueField != null && str.length() > 3) {
            try {
                final char[] chars = (char[]) strValueField.get(str);

                if (chars.length == str.length()) {
                    return chars;
                } else {
                    isStringCharsGettable = false;
                }

            } catch (Exception e) {
                // ignore.
                isStringCharsGettable = false;
            }
        }

        return str.toCharArray();
    }

    /**
     *
     * @param a the specified array should not be modified after it's used to
     *            create the new String.
     * @param share the same array will be shared with the new created ArrayList
     *            if it's true.
     * @return
     */
    static String newString(final char[] a, final boolean share) {
        if (share && sharedStringConstructor != null) {
            try {
                return sharedStringConstructor.newInstance(a, true);
            } catch (Exception e) {
                throw N.toRuntimeException(e);
            }
        } else {
            return String.valueOf(a);
        }
    }
}
