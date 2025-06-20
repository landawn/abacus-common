/*
 * Copyright (C) 2019 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.stream.Stream;

/**
 * This is the Joiner class. It provides various methods to join elements into a string.
 * The class implements the Closeable interface, indicating that instances of this class can be closed to release resources.
 * The class provides methods to append different types of elements, including primitives, arrays, collections, and maps.
 * It also provides methods to control the joining behavior, such as whether to trim before appending, whether to skip nulls, and whether to use a cached buffer.
 * The class also includes methods for repeating a string, merging another Joiner, and converting the joined elements into a string, a map, or a stream.
 *
 * @see Splitter
 */
public final class Joiner implements Closeable {

    public static final String DEFAULT_DELIMITER = Strings.ELEMENT_SEPARATOR;

    public static final String DEFAULT_KEY_VALUE_DELIMITER = "=";

    private final String prefix;

    private final String separator;

    private final String keyValueSeparator;

    private final String suffix;

    private final boolean isEmptySeparator;

    private final boolean isEmptyKeyValueSeparator;

    private boolean trimBeforeAppend = false;

    private boolean skipNulls = false;

    private boolean useCachedBuffer = false;

    private String nullText = Strings.NULL;

    private StringBuilder buffer;

    private String emptyValue;

    private String latestToStringValue;

    private boolean isClosed;

    Joiner(final CharSequence separator) {
        this(separator, DEFAULT_KEY_VALUE_DELIMITER);
    }

    Joiner(final CharSequence separator, final CharSequence keyValueDelimiter) {
        this(separator, keyValueDelimiter, "", "");
    }

    Joiner(final CharSequence separator, final CharSequence prefix, final CharSequence suffix) {
        this(separator, DEFAULT_KEY_VALUE_DELIMITER, prefix, suffix);
    }

    Joiner(final CharSequence separator, final CharSequence keyValueSeparator, final CharSequence prefix, final CharSequence suffix) {
        N.checkArgNotNull(prefix, "The prefix must not be null");
        N.checkArgNotNull(separator, "The separator must not be null");
        N.checkArgNotNull(keyValueSeparator, "The keyValueSeparator must not be null");
        N.checkArgNotNull(suffix, "The suffix must not be null");

        // make defensive copies of arguments
        this.prefix = prefix.toString();
        this.separator = separator.toString();
        this.keyValueSeparator = keyValueSeparator.toString();
        this.suffix = suffix.toString();
        emptyValue = this.prefix + this.suffix;
        isEmptySeparator = Strings.isEmpty(separator);
        isEmptyKeyValueSeparator = Strings.isEmpty(keyValueSeparator);
    }

    /**
     *
     * @return
     * @see Splitter#defauLt()
     * @see Splitter.MapSplitter#defauLt()
     */
    @Beta
    public static Joiner defauLt() {
        return with(DEFAULT_DELIMITER, DEFAULT_KEY_VALUE_DELIMITER);
    }

    /**
     *
     * @param separator
     * @return
     */
    public static Joiner with(final CharSequence separator) {
        return new Joiner(separator);
    }

    /**
     *
     * @param separator
     * @param keyValueDelimiter
     * @return
     */
    public static Joiner with(final CharSequence separator, final CharSequence keyValueDelimiter) {
        return new Joiner(separator, keyValueDelimiter);
    }

    /**
     *
     * @param separator
     * @param prefix
     * @param suffix
     * @return
     */
    public static Joiner with(final CharSequence separator, final CharSequence prefix, final CharSequence suffix) {
        return new Joiner(separator, prefix, suffix);
    }

    /**
     *
     * @param separator
     * @param keyValueSeparator
     * @param prefix
     * @param suffix
     * @return
     */
    public static Joiner with(final CharSequence separator, final CharSequence keyValueSeparator, final CharSequence prefix, final CharSequence suffix) {
        return new Joiner(separator, keyValueSeparator, prefix, suffix);
    }

    /**
     * Sets the empty value.
     *
     * @param emptyValue
     * @return
     * @throws IllegalArgumentException
     */
    public Joiner setEmptyValue(final CharSequence emptyValue) throws IllegalArgumentException {
        this.emptyValue = N.checkArgNotNull(emptyValue, "The empty value must not be null").toString();

        return this;
    }

    //    /**
    //     *
    //     * @param trim
    //     * @return
    //     * @deprecated replaced by {@link #trimBeforeAppend()}
    //     */
    //    @Deprecated
    //    public Joiner trim(boolean trim) {
    //        this.trimBeforeAppend = trim;
    //
    //        return this;
    //    }

    public Joiner trimBeforeAppend() {
        trimBeforeAppend = true;

        return this;
    }

    //    /**
    //     * Ignore the {@code null} element/value for {@code key/value, Map, Bean} when the specified {@code element} or {@code value} is {@code null} if it's set to {@code true}.
    //     *
    //     * @param skipNull
    //     * @return
    //     * @deprecated replaced by {@link #skipNulls()}
    //     */
    //    @Deprecated
    //    public Joiner skipNull(boolean skipNull) {
    //        this.skipNulls = skipNull;
    //
    //        return this;
    //    }

    public Joiner skipNulls() {
        skipNulls = true;

        return this;
    }

    /**
     * Use for {@code null}.
     *
     * @param nullText
     * @return
     */
    public Joiner useForNull(final String nullText) {
        this.nullText = nullText == null ? Strings.NULL : nullText;

        return this;
    }

    //    /**
    //     * Get the {@code StringBuilder} from object factory to improve performance if it's set to true, and must remember to call {@code toString()/map()/mapIfNotEmpty()/stream()/streamIfNotEmpty()} or {@code close()} to recycle the {@code StringBuilder}.
    //     *
    //     * @param reuseStringBuilder
    //     * @return
    //     * @deprecated replaced by {@code #reuseCachedBuffer(boolean)}
    //     */
    //    @Deprecated
    //    public Joiner reuseStringBuilder(boolean reuseStringBuilder) {
    //        if (buffer != null) {
    //            throw new IllegalStateException("Can't reset because the StringBuilder has been created");
    //        }
    //
    //        this.useCachedBuffer = reuseStringBuilder;
    //
    //        return this;
    //    }

    //    /**
    //     * Improving performance by set {@code useCachedBuffer=true}, and must remember to call {@code toString()/map()/mapIfNotEmpty()/stream()/streamIfNotEmpty()} or {@code close()} to recycle the cached buffer.
    //     *
    //     * @param useCachedBuffer
    //     * @return
    //     * @deprecated replaced by {@link #reuseCachedBuffer()}
    //     */
    //    @Deprecated
    //    public Joiner reuseCachedBuffer(boolean useCachedBuffer) {
    //        if (buffer != null) {
    //            throw new IllegalStateException("Can't reset because the buffer has been created");
    //        }
    //
    //        this.useCachedBuffer = useCachedBuffer;
    //
    //        return this;
    //    }

    /**
     * Improving performance by set {@code useCachedBuffer=true},
     * and must remember to call {@code toString()/appendTo()/map()/mapIfNotEmpty()/stream()/streamIfNotEmpty()} or {@code close()} to recycle the cached buffer.
     *
     * @return
     */
    @Beta
    public Joiner reuseCachedBuffer() {
        if (buffer != null) {
            throw new IllegalStateException("Can't reset because the buffer has been created");
        }

        useCachedBuffer = true;

        return this;
    }

    /**
     *
     * @param element
     * @return
     */
    public Joiner append(final boolean element) {
        prepareBuilder().append(element);
        return this;
    }

    /**
     *
     * @param element
     * @return
     */
    public Joiner append(final char element) {
        prepareBuilder().append(element);
        return this;
    }

    /**
     *
     * @param element
     * @return
     */
    public Joiner append(final int element) {
        prepareBuilder().append(element);
        return this;
    }

    /**
     *
     * @param element
     * @return
     */
    public Joiner append(final long element) {
        prepareBuilder().append(element);
        return this;
    }

    /**
     *
     * @param element
     * @return
     */
    public Joiner append(final float element) {
        prepareBuilder().append(element);
        return this;
    }

    /**
     *
     * @param element
     * @return
     */
    public Joiner append(final double element) {
        prepareBuilder().append(element);
        return this;
    }

    /**
     *
     * @param element
     * @return
     */
    public Joiner append(final String element) {
        if (element != null || !skipNulls) {
            prepareBuilder().append(element == null ? nullText : (trimBeforeAppend ? element.trim() : element));
        }

        return this;
    }

    //    /**
    //     *
    //     * @param element
    //     * @return
    //     */
    // This is wrong. is it to join a char array, like int[], Object[] or String[]?
    //    public Joiner append(final char[] element) {
    //        if (element != null || !skipNulls) {
    //            if (element == null) {
    //                prepareBuilder().append(nullText);
    //            } else {
    //                prepareBuilder().append(element);
    //            }
    //        }
    //
    //        return this;
    //    }
    //
    //    /**
    //     *
    //     * @param element
    //     * @param offset
    //     * @param len
    //     * @return
    //     * @see StringBuilder#append(char[], int, int)
    //     */
    // This is wrong. is it to join a char array, like int[], Object[] or String[]?
    //    public Joiner append(final char[] element, final int offset, final int len) {
    //        if (element != null || !skipNulls) {
    //            if (element == null) {
    //                prepareBuilder().append(nullText);
    //            } else {
    //                prepareBuilder().append(element, offset, len);
    //            }
    //        }
    //
    //        return this;
    //    }

    /**
     *
     * @param element
     * @return
     */
    public Joiner append(final CharSequence element) {
        if (element != null || !skipNulls) {
            prepareBuilder().append(element == null ? nullText : (trimBeforeAppend ? element.toString().trim() : element));
        }

        return this;
    }

    /**
     *
     * @param element
     * @param start
     * @param end
     * @return
     * @see Appendable#append(CharSequence, int, int)
     */
    public Joiner append(final CharSequence element, final int start, final int end) {
        if (element != null || !skipNulls) {
            if (element == null) {
                prepareBuilder().append(nullText);
            } else if (trimBeforeAppend) {
                prepareBuilder().append(element.subSequence(start, end).toString().trim());
            } else {
                prepareBuilder().append(element, start, end);
            }
        }

        return this;
    }

    /**
     *
     * @param element
     * @return
     */
    public Joiner append(final StringBuilder element) {
        if (element != null || !skipNulls) {
            if (element == null) {
                prepareBuilder().append(nullText);
            } else {
                prepareBuilder().append(element);
            }
        }

        return this;
    }

    /**
     *
     * @param element
     * @return
     */
    public Joiner append(final Object element) { // Note: DO NOT remove/update this method because it also protects append(boolean/char/byte/.../double) from NullPointerException.
        if (element != null || !skipNulls) {
            prepareBuilder().append(toString(element));
        }

        return this;
    }

    /**
     * Append if not {@code null}.
     *
     * @param element
     * @return
     */
    public Joiner appendIfNotNull(final Object element) {
        if (element != null) {
            prepareBuilder().append(toString(element));
        }

        return this;
    }

    /**
     *
     * @param b
     * @param supplier
     * @return
     */
    public Joiner appendIf(final boolean b, final Supplier<?> supplier) {
        if (b) {
            //noinspection resource
            append(supplier.get());
        }

        return this;
    }

    /**
     *
     * @param a
     * @return
     */
    public Joiner appendAll(final boolean[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final boolean[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (sb == null) {
                sb = prepareBuilder().append(a[i]);
            } else {
                if (isEmptySeparator) {
                    sb.append(a[i]);
                } else {
                    sb.append(separator).append(a[i]);
                }
            }
        }

        return this;
    }

    /**
     *
     * @param a
     * @return
     */
    public Joiner appendAll(final char[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final char[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (sb == null) {
                sb = prepareBuilder().append(a[i]);
            } else {
                if (isEmptySeparator) {
                    sb.append(a[i]);
                } else {
                    sb.append(separator).append(a[i]);
                }
            }
        }

        return this;
    }

    /**
     *
     * @param a
     * @return
     */
    public Joiner appendAll(final byte[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final byte[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (sb == null) {
                sb = prepareBuilder().append(a[i]);
            } else {
                if (isEmptySeparator) {
                    sb.append(a[i]);
                } else {
                    sb.append(separator).append(a[i]);
                }
            }
        }

        return this;
    }

    /**
     *
     * @param a
     * @return
     */
    public Joiner appendAll(final short[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final short[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (sb == null) {
                sb = prepareBuilder().append(a[i]);
            } else {
                if (isEmptySeparator) {
                    sb.append(a[i]);
                } else {
                    sb.append(separator).append(a[i]);
                }
            }
        }

        return this;
    }

    /**
     *
     * @param a
     * @return
     */
    public Joiner appendAll(final int[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final int[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (sb == null) {
                sb = prepareBuilder().append(a[i]);
            } else {
                if (isEmptySeparator) {
                    sb.append(a[i]);
                } else {
                    sb.append(separator).append(a[i]);
                }
            }
        }

        return this;
    }

    /**
     *
     * @param a
     * @return
     */
    public Joiner appendAll(final long[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final long[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (sb == null) {
                sb = prepareBuilder().append(a[i]);
            } else {
                if (isEmptySeparator) {
                    sb.append(a[i]);
                } else {
                    sb.append(separator).append(a[i]);
                }
            }
        }

        return this;
    }

    /**
     *
     * @param a
     * @return
     */
    public Joiner appendAll(final float[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final float[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (sb == null) {
                sb = prepareBuilder().append(a[i]);
            } else {
                if (isEmptySeparator) {
                    sb.append(a[i]);
                } else {
                    sb.append(separator).append(a[i]);
                }
            }
        }

        return this;
    }

    /**
     *
     * @param a
     * @return
     */
    public Joiner appendAll(final double[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final double[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (sb == null) {
                sb = prepareBuilder().append(a[i]);
            } else {
                if (isEmptySeparator) {
                    sb.append(a[i]);
                } else {
                    sb.append(separator).append(a[i]);
                }
            }
        }

        return this;
    }

    /**
     *
     * @param a
     * @return
     */
    public Joiner appendAll(final Object[] a) {
        if (N.notEmpty(a)) {
            return appendAll(a, 0, a.length);
        }

        return this;
    }

    /**
     *
     * @param a
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final Object[] a, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, a == null ? 0 : a.length);

        if (N.isEmpty(a) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        for (int i = fromIndex; i < toIndex; i++) {
            if (a[i] != null || !skipNulls) {
                if (sb == null) {
                    sb = prepareBuilder().append(toString(a[i]));
                } else {
                    if (isEmptySeparator) {
                        sb.append(toString(a[i]));
                    } else {
                        sb.append(separator).append(toString(a[i]));
                    }
                }
            }
        }

        return this;
    }

    /**
     *
     * @param c
     * @return
     */
    public Joiner appendAll(final BooleanList c) {
        if (N.notEmpty(c)) {
            return appendAll(c.array(), 0, c.size());
        }

        return this;
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final BooleanList c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        return appendAll(c.array(), fromIndex, toIndex);
    }

    /**
     *
     * @param c
     * @return
     */
    public Joiner appendAll(final CharList c) {
        if (N.notEmpty(c)) {
            return appendAll(c.array(), 0, c.size());
        }

        return this;
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final CharList c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        return appendAll(c.array(), fromIndex, toIndex);
    }

    /**
     *
     * @param c
     * @return
     */
    public Joiner appendAll(final ByteList c) {
        if (N.notEmpty(c)) {
            return appendAll(c.array(), 0, c.size());
        }

        return this;
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final ByteList c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        return appendAll(c.array(), fromIndex, toIndex);
    }

    /**
     *
     * @param c
     * @return
     */
    public Joiner appendAll(final ShortList c) {
        if (N.notEmpty(c)) {
            return appendAll(c.array(), 0, c.size());
        }

        return this;
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final ShortList c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        return appendAll(c.array(), fromIndex, toIndex);
    }

    /**
     *
     * @param c
     * @return
     */
    public Joiner appendAll(final IntList c) {
        if (N.notEmpty(c)) {
            return appendAll(c.array(), 0, c.size());
        }

        return this;
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final IntList c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        return appendAll(c.array(), fromIndex, toIndex);
    }

    /**
     *
     * @param c
     * @return
     */
    public Joiner appendAll(final LongList c) {
        if (N.notEmpty(c)) {
            return appendAll(c.array(), 0, c.size());
        }

        return this;
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final LongList c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        return appendAll(c.array(), fromIndex, toIndex);
    }

    /**
     *
     * @param c
     * @return
     */
    public Joiner appendAll(final FloatList c) {
        if (N.notEmpty(c)) {
            return appendAll(c.array(), 0, c.size());
        }

        return this;
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final FloatList c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        return appendAll(c.array(), fromIndex, toIndex);
    }

    /**
     *
     * @param c
     * @return
     */
    public Joiner appendAll(final DoubleList c) {
        if (N.notEmpty(c)) {
            return appendAll(c.array(), 0, c.size());
        }

        return this;
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final DoubleList c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        return appendAll(c.array(), fromIndex, toIndex);
    }

    /**
     *
     * @param c
     * @return
     */
    public Joiner appendAll(final Collection<?> c) {
        if (N.notEmpty(c)) {
            return appendAll(c, 0, c.size());
        }

        return this;
    }

    /**
     *
     * @param c
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendAll(final Collection<?> c, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, c == null ? 0 : c.size());

        if (N.isEmpty(c) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        int i = 0;
        for (final Object e : c) {
            if (i++ < fromIndex) {
                continue;
            }

            if (e != null || !skipNulls) {
                if (sb == null) {
                    sb = prepareBuilder().append(toString(e));
                } else {
                    if (isEmptySeparator) {
                        sb.append(toString(e));
                    } else {
                        sb.append(separator).append(toString(e));
                    }
                }
            }

            if (i >= toIndex) {
                break;
            }
        }

        return this;
    }

    /**
     *
     * @param c
     * @return
     */
    public Joiner appendAll(final Iterable<?> c) {
        if (c != null) {
            StringBuilder sb = null;

            for (final Object e : c) {
                if (e != null || !skipNulls) {
                    if (sb == null) {
                        sb = prepareBuilder().append(toString(e));
                    } else {
                        if (isEmptySeparator) {
                            sb.append(toString(e));
                        } else {
                            sb.append(separator).append(toString(e));
                        }
                    }
                }
            }
        }

        return this;
    }

    /**
     *
     * @param <T>
     * @param c
     * @param filter will be the only condition to decide if append an element from the specified {@code Iterable} or not. {@code skipNulls()} won't be used here.
     * @return
     * @throws IllegalArgumentException
     */
    public <T> Joiner appendAll(final Iterable<? extends T> c, final Predicate<? super T> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter); //NOSONAR

        if (c != null) {
            StringBuilder sb = null;

            for (final T e : c) {
                if (!filter.test(e)) {
                    continue;
                }

                if (sb == null) {
                    sb = prepareBuilder().append(toString(e));
                } else {
                    if (isEmptySeparator) {
                        sb.append(toString(e));
                    } else {
                        sb.append(separator).append(toString(e));
                    }
                }
            }
        }

        return this;
    }

    /**
     *
     * @param iter
     * @return
     */
    public Joiner appendAll(final Iterator<?> iter) {
        if (iter != null) {
            StringBuilder sb = null;
            Object e = null;

            while (iter.hasNext()) {
                e = iter.next();

                if (e != null || !skipNulls) {
                    if (sb == null) {
                        sb = prepareBuilder().append(toString(e));
                    } else {
                        if (isEmptySeparator) {
                            sb.append(toString(e));
                        } else {
                            sb.append(separator).append(toString(e));
                        }
                    }
                }
            }
        }

        return this;
    }

    /**
     *
     * @param <T>
     * @param iter
     * @param filter will be the only condition to decide if append an element from the specified {@code Iterable} or not. {@code skipNulls()} won't be used here.
     * @return
     * @throws IllegalArgumentException
     */
    public <T> Joiner appendAll(final Iterator<? extends T> iter, final Predicate<? super T> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter);

        if (iter != null) {
            StringBuilder sb = null;
            T e = null;

            while (iter.hasNext()) {
                e = iter.next();

                if (!filter.test(e)) {
                    continue;
                }

                if (sb == null) {
                    sb = prepareBuilder().append(toString(e));
                } else {
                    if (isEmptySeparator) {
                        sb.append(toString(e));
                    } else {
                        sb.append(separator).append(toString(e));
                    }
                }
            }
        }

        return this;
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public Joiner appendEntry(final String key, final boolean value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(value);
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(value);
        }

        return this;
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public Joiner appendEntry(final String key, final char value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(value);
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(value);
        }

        return this;
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public Joiner appendEntry(final String key, final int value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(value);
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(value);
        }

        return this;
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public Joiner appendEntry(final String key, final long value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(value);
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(value);
        }

        return this;
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public Joiner appendEntry(final String key, final float value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(value);
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(value);
        }

        return this;
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public Joiner appendEntry(final String key, final double value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(value);
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(value);
        }

        return this;
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public Joiner appendEntry(final String key, final String value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(format(value));
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(format(value));
        }

        return this;
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public Joiner appendEntry(final String key, final CharSequence value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(format(value));
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(format(value));
        }

        return this;
    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public Joiner appendEntry(final String key, final StringBuilder value) {
        if (value == null) {
            if (isEmptyKeyValueSeparator) {
                prepareBuilder().append(format(key)).append(nullText);
            } else {
                prepareBuilder().append(format(key)).append(keyValueSeparator).append(nullText);
            }
        } else {
            if (isEmptyKeyValueSeparator) {
                if (trimBeforeAppend) {
                    prepareBuilder().append(format(key)).append(value.toString().trim());
                } else {
                    prepareBuilder().append(format(key)).append(value);
                }
            } else {
                if (trimBeforeAppend) {
                    prepareBuilder().append(format(key)).append(keyValueSeparator).append(value.toString().trim());
                } else {
                    prepareBuilder().append(format(key)).append(keyValueSeparator).append(value);
                }
            }
        }

        return this;
    }

    //    /**
    //     *
    //     * @param key
    //     * @param value
    //     * @return
    //     */
    //    // This is wrong. is it to join a char array, like int[], Object[] or String[]?
    //    public Joiner appendEntry(final String key, final char[] value) {
    //        if (value == null) {
    //            if (isEmptyKeyValueSeparator) {
    //                prepareBuilder().append(format(key)).append(nullText);
    //            } else {
    //                prepareBuilder().append(format(key)).append(keyValueSeparator).append(nullText);
    //            }
    //        } else {
    //            if (isEmptyKeyValueSeparator) {
    //                prepareBuilder().append(format(key)).append(value);
    //            } else {
    //                prepareBuilder().append(format(key)).append(keyValueSeparator).append(value);
    //            }
    //        }
    //
    //        return this;
    //    }

    /**
     *
     * @param key
     * @param value
     * @return
     */
    public Joiner appendEntry(final String key, final Object value) {
        if (isEmptyKeyValueSeparator) {
            prepareBuilder().append(format(key)).append(toString(value));
        } else {
            prepareBuilder().append(format(key)).append(keyValueSeparator).append(toString(value));
        }

        return this;
    }

    /**
     *
     * @param entry
     * @return
     */
    public Joiner appendEntry(final Map.Entry<?, ?> entry) {
        if (entry == null) {
            //noinspection resource
            append(nullText);
        } else {
            //noinspection resource
            appendEntry(toString(entry.getKey()), toString(entry.getValue()));
        }

        return this;
    }

    //    public Joiner appendEntryIf(boolean b, String key, Object value) {
    //        if (b) {
    //            appendEntry(key, value);
    //        }
    //
    //        return this;
    //    }
    //
    //    public Joiner appendEntryIf(boolean b, Map.Entry<?, ?> entry) {
    //        if (b) {
    //            appendEntry(entry);
    //        }
    //
    //        return this;
    //    }

    /**
     * Appends all entries from a given map to the Joiner.
     * Each entry is appended as a key-value pair.
     *
     * @param m The map containing the entries to be appended.
     * @return The Joiner instance with the appended entries.
     */
    public Joiner appendEntries(final Map<?, ?> m) {
        if (N.notEmpty(m)) {
            return appendEntries(m, 0, m.size());
        }

        return this;
    }

    /**
     *
     * @param m
     * @param fromIndex
     * @param toIndex
     * @return
     * @throws IndexOutOfBoundsException
     */
    public Joiner appendEntries(final Map<?, ?> m, final int fromIndex, final int toIndex) throws IndexOutOfBoundsException {
        N.checkFromToIndex(fromIndex, toIndex, m == null ? 0 : m.size());

        if (N.isEmpty(m) || fromIndex == toIndex) {
            return this;
        }

        StringBuilder sb = null;

        int i = 0;
        for (final Map.Entry<?, ?> entry : m.entrySet()) {
            if (i++ < fromIndex) {
                continue;
            }

            if (sb == null) {
                sb = prepareBuilder().append(toString(entry.getKey())).append(keyValueSeparator).append(toString(entry.getValue()));
            } else {
                if (isEmptySeparator) {
                    sb.append(toString(entry.getKey()));
                } else {
                    sb.append(separator).append(toString(entry.getKey()));
                }

                if (isEmptyKeyValueSeparator) {
                    sb.append(toString(entry.getValue()));
                } else {
                    sb.append(keyValueSeparator).append(toString(entry.getValue()));
                }
            }

            if (i >= toIndex) {
                break;
            }
        }

        return this;
    }

    /**
     *
     * @param <K>
     * @param <V>
     * @param m
     * @param filter
     * @return
     * @throws IllegalArgumentException
     */
    public <K, V> Joiner appendEntries(final Map<K, V> m, final Predicate<? super Map.Entry<K, V>> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter);

        StringBuilder sb = null;

        for (final Map.Entry<K, V> entry : m.entrySet()) {
            if (!filter.test(entry)) {
                continue;
            }

            if (sb == null) {
                sb = prepareBuilder().append(toString(entry.getKey())).append(keyValueSeparator).append(toString(entry.getValue()));
            } else {
                if (isEmptySeparator) {
                    sb.append(toString(entry.getKey()));
                } else {
                    sb.append(separator).append(toString(entry.getKey()));
                }

                if (isEmptyKeyValueSeparator) {
                    sb.append(toString(entry.getValue()));
                } else {
                    sb.append(keyValueSeparator).append(toString(entry.getValue()));
                }
            }
        }

        return this;
    }

    /**
     *
     * @param <K>
     * @param <V>
     * @param m
     * @param filter
     * @return
     * @throws IllegalArgumentException
     */
    public <K, V> Joiner appendEntries(final Map<K, V> m, final BiPredicate<? super K, ? super V> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter);

        StringBuilder sb = null;

        for (final Map.Entry<K, V> entry : m.entrySet()) {
            if (!filter.test(entry.getKey(), entry.getValue())) {
                continue;
            }

            if (sb == null) {
                sb = prepareBuilder().append(toString(entry.getKey())).append(keyValueSeparator).append(toString(entry.getValue()));
            } else {
                if (isEmptySeparator) {
                    sb.append(toString(entry.getKey()));
                } else {
                    sb.append(separator).append(toString(entry.getKey()));
                }

                if (isEmptyKeyValueSeparator) {
                    sb.append(toString(entry.getValue()));
                } else {
                    sb.append(keyValueSeparator).append(toString(entry.getValue()));
                }
            }
        }

        return this;
    }

    /**
     *
     * @param <K>
     * @param <V>
     * @param m
     * @param keyExtractor
     * @param valueExtractor
     * @return
     * @throws IllegalArgumentException
     */
    public <K, V> Joiner appendEntries(final Map<K, V> m, final Function<? super K, ?> keyExtractor, final Function<? super V, ?> valueExtractor)
            throws IllegalArgumentException {
        N.checkArgNotNull(keyExtractor, cs.keyExtractor);
        N.checkArgNotNull(valueExtractor, cs.valueExtractor);

        StringBuilder sb = null;

        for (final Map.Entry<K, V> entry : m.entrySet()) {
            if (sb == null) {
                sb = prepareBuilder().append(toString(keyExtractor.apply(entry.getKey())))
                        .append(keyValueSeparator)
                        .append(toString(valueExtractor.apply(entry.getValue())));
            } else {
                if (isEmptySeparator) {
                    sb.append(toString(keyExtractor.apply(entry.getKey())));
                } else {
                    sb.append(separator).append(toString(keyExtractor.apply(entry.getKey())));
                }

                if (isEmptyKeyValueSeparator) {
                    sb.append(toString(toString(valueExtractor.apply(entry.getValue()))));
                } else {
                    sb.append(keyValueSeparator).append(toString(valueExtractor.apply(entry.getValue())));
                }
            }
        }

        return this;
    }

    /**
     *
     * @param bean
     * @return
     */
    public Joiner appendBean(final Object bean) {
        return appendBean(bean, (Collection<String>) null);
    }

    /**
     *
     * @param bean
     * @param selectPropNames
     * @return
     * @throws IllegalArgumentException
     */
    public Joiner appendBean(final Object bean, final Collection<String> selectPropNames) throws IllegalArgumentException {
        if (bean == null) {
            return this;
        }

        if (N.isEmpty(selectPropNames)) {
            return appendBean(bean, true, null);
        }

        final Class<?> cls = bean.getClass();

        N.checkArgument(ClassUtil.isBeanClass(cls), "'bean' must be bean class with getter/setter methods"); //NOSONAR

        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);
        StringBuilder sb = null;
        Object propValue = null;

        for (final String propName : selectPropNames) {
            propValue = beanInfo.getPropValue(bean, propName);

            if (sb == null) {
                sb = prepareBuilder().append(propName).append(keyValueSeparator).append(toString(propValue));
            } else {
                if (isEmptySeparator) {
                    sb.append(propName);
                } else {
                    sb.append(separator).append(propName);
                }

                if (isEmptyKeyValueSeparator) {
                    sb.append(toString(propValue));
                } else {
                    sb.append(keyValueSeparator).append(toString(propValue));
                }
            }
        }

        return this;
    }

    /**
     *
     * @param bean
     * @param ignoreNullProperty
     * @param ignoredPropNames
     * @return
     * @throws IllegalArgumentException
     */
    public Joiner appendBean(final Object bean, final boolean ignoreNullProperty, final Set<String> ignoredPropNames) throws IllegalArgumentException {
        if (bean == null) {
            return this;
        }

        final Class<?> cls = bean.getClass();

        N.checkArgument(ClassUtil.isBeanClass(cls), "'bean' must be bean class with getter/setter methods");

        final boolean hasIgnoredPropNames = N.notEmpty(ignoredPropNames);
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);
        StringBuilder sb = null;
        Object propValue = null;

        for (final String propName : ClassUtil.getPropNameList(cls)) {
            if (hasIgnoredPropNames && ignoredPropNames.contains(propName)) {
                continue;
            }

            propValue = beanInfo.getPropValue(bean, propName);

            if (propValue != null || !ignoreNullProperty) {
                if (sb == null) {
                    sb = prepareBuilder().append(propName).append(keyValueSeparator).append(toString(propValue));
                } else {
                    if (isEmptySeparator) {
                        sb.append(propName);
                    } else {
                        sb.append(separator).append(propName);
                    }

                    if (isEmptyKeyValueSeparator) {
                        sb.append(toString(propValue));
                    } else {
                        sb.append(keyValueSeparator).append(toString(propValue));
                    }
                }
            }
        }

        return this;
    }

    /**
     *
     * @param bean
     * @param filter
     * @return
     * @throws IllegalArgumentException
     */
    public Joiner appendBean(final Object bean, final BiPredicate<? super String, ?> filter) throws IllegalArgumentException {
        N.checkArgNotNull(filter, cs.filter);

        if (bean == null) {
            return this;
        }

        final Class<?> cls = bean.getClass();

        N.checkArgument(ClassUtil.isBeanClass(cls), "'bean' must be bean class with getter/setter methods");

        final BiPredicate<? super String, Object> filterToUse = (BiPredicate<? super String, Object>) filter;
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(cls);
        StringBuilder sb = null;
        Object propValue = null;

        for (final String propName : ClassUtil.getPropNameList(cls)) {
            propValue = beanInfo.getPropValue(bean, propName);

            if (!filterToUse.test(propName, propValue)) {
                continue;
            }

            if (sb == null) {
                sb = prepareBuilder().append(propName).append(keyValueSeparator).append(toString(propValue));
            } else {
                if (isEmptySeparator) {
                    sb.append(propName);
                } else {
                    sb.append(separator).append(propName);
                }

                if (isEmptyKeyValueSeparator) {
                    sb.append(toString(propValue));
                } else {
                    sb.append(keyValueSeparator).append(toString(propValue));
                }
            }
        }

        return this;
    }

    /**
     *
     * @param str
     * @param n
     * @return
     * @throws IllegalArgumentException
     */
    public Joiner repeat(final String str, final int n) throws IllegalArgumentException {
        N.checkArgNotNegative(n, cs.n);

        final String newString = toString(str);

        if (n < 10) {
            for (int i = 0; i < n; i++) {
                //noinspection resource
                append(newString);
            }
        } else {
            //noinspection resource
            append(Strings.repeat(newString, n, separator));
        }

        return this;
    }

    /**
     *
     * @param obj
     * @param n
     * @return
     */
    public Joiner repeat(final Object obj, final int n) {
        return repeat(toString(obj), n);
    }

    /**
     * Adds the contents from the specified Joiner {@code other} without prefix and suffix as the next element if it is non-empty.
     * If the specified {@code Joiner} is empty, the call has no effect.
     *
     * <p>Remember to close {@code other} Joiner if {@code reuseCachedBuffer} is set to {@code} {@code true}.
     *
     * @param other
     * @return
     * @throws IllegalArgumentException the specified Joiner {@code other} is {@code null}.
     */
    public Joiner merge(final Joiner other) throws IllegalArgumentException {
        N.checkArgNotNull(other);

        if (other.buffer != null) {
            final int length = other.buffer.length();
            final StringBuilder builder = prepareBuilder();
            builder.append(other.buffer, other.prefix.length(), length);
        }

        return this;
    }

    public int length() {
        // Remember that we never actually append the suffix unless we return
        // the full (present) value or some substring or length of it, so that
        // we can add on more if we need to.
        return (buffer != null ? buffer.length() + suffix.length() : emptyValue.length());
    }

    /**
     * Returns the current value, consisting of the {@code prefix}, the values
     * added so far separated by the {@code delimiter}, and the {@code suffix},
     * unless no elements have been added in which case, the
     * {@code prefix + suffix} or the {@code emptyValue} characters are returned
     *
     * <pre>
     * The underline {@code StringBuilder} will be recycled after this method is called if {@code reuseStringBuilder} is set to {@code true},
     * and should not continue to this instance.
     * </pre>
     *
     * @return
     */
    @Override
    public String toString() {
        if (buffer == null) {
            return emptyValue;
        } else {
            try {
                String result = null;

                if (suffix.isEmpty()) {
                    result = buffer.toString();
                } else {
                    final int initialLength = buffer.length();

                    result = buffer.append(suffix).toString();

                    // reset value to pre-append initialLength
                    buffer.setLength(initialLength);
                }

                latestToStringValue = result;

                return result;
            } finally {
                recycleBuffer();
            }
        }
    }

    /**
     *
     * @param <A>
     * @param appendable
     * @return
     * @throws IOException
     */
    public <A extends Appendable> A appendTo(final A appendable) throws IOException {
        if (buffer == null) {
            return appendable;
        }

        appendable.append(toString());

        return appendable;
    }

    /**
     * <pre>
     * The underline {@code StringBuilder} will be recycled after this method is called if {@code reuseStringBuilder} is set to {@code true},
     * and should not continue to this instance.
     * </pre>
     *
     * @param <T>
     * @param mapper
     * @return
     */
    public <T> T map(final Function<? super String, T> mapper) {
        return mapper.apply(toString());
    }

    /**
     * Call {@code mapper} only if at least one element/object/entry is appended.
     *
     * <pre>
     * The underline {@code StringBuilder} will be recycled after this method is called if {@code reuseStringBuilder} is set to {@code true},
     * and should not continue to this instance.
     * </pre>
     *
     * @param <T>
     * @param mapper
     * @return
     * @throws IllegalArgumentException
     */
    public <T> Nullable<T> mapIfNotEmpty(final Function<? super String, T> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper);

        return buffer == null ? Nullable.empty() : Nullable.of(mapper.apply(toString()));
    }

    /**
     * Call {@code mapper} only if at least one element/object/entry is appended.
     *
     * <pre>
     * The underline {@code StringBuilder} will be recycled after this method is called if {@code reuseStringBuilder} is set to {@code true},
     * and should not continue to this instance.
     * </pre>
     *
     * @param <T>
     * @param mapper
     * @return
     * @throws IllegalArgumentException
     */
    public <T> u.Optional<T> mapToNonNullIfNotEmpty(final Function<? super String, T> mapper) throws IllegalArgumentException {
        N.checkArgNotNull(mapper);

        return buffer == null ? u.Optional.empty() : u.Optional.of(mapper.apply(toString()));
    }

    /**
     * <pre>
     * The underline {@code StringBuilder} will be recycled after this method is called if {@code reuseStringBuilder} is set to {@code true},
     * and should not continue to this instance.
     * </pre>
     *
     * @return
     */
    public Stream<String> stream() {
        return Stream.of(toString());
    }

    /**
     * Returns a stream with the String value generated by {@code toString()} if at least one element/object/entry is appended, otherwise an empty {@code Stream} is returned.
     *
     * <pre>
     * The underline {@code StringBuilder} will be recycled after this method is called if {@code reuseStringBuilder} is set to {@code true},
     * and should not continue to this instance.
     * </pre>
     *
     * @return
     */
    public Stream<String> streamIfNotEmpty() {
        return buffer == null ? Stream.empty() : Stream.of(toString());
    }

    /**
     * Close.
     */
    @Override
    public synchronized void close() {
        if (isClosed) {
            return;
        }

        isClosed = true;

        recycleBuffer();
    }

    private String format(final String text) {
        return text == null ? nullText : (trimBeforeAppend ? text.trim() : text);
    }

    private String format(final CharSequence text) {
        return text == null ? nullText : (trimBeforeAppend ? text.toString().trim() : text.toString());
    }

    private String toString(final Object obj) {
        return obj == null ? nullText : (trimBeforeAppend ? N.toString(obj).trim() : N.toString(obj));
    }

    private StringBuilder prepareBuilder() {
        assertNotClosed();

        if (buffer != null) {
            if (!isEmptySeparator) {
                buffer.append(separator);
            }
        } else {
            buffer = (useCachedBuffer ? Objectory.createStringBuilder() : new StringBuilder())
                    .append(latestToStringValue == null ? prefix : latestToStringValue);

            if (!isEmptySeparator && latestToStringValue != null) {
                buffer.append(separator);
            }
        }

        return buffer;
    }

    private void recycleBuffer() {
        if (useCachedBuffer) {
            Objectory.recycle(buffer);
            buffer = null;

            // reset useCachedBuffer.
            useCachedBuffer = false;
        }
    }

    private void assertNotClosed() {
        if (isClosed) {
            throw new IllegalStateException();
        }
    }
}
