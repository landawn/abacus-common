/*
 * Copyright (C) 2016 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

/**
 *
 * @author Haiyang Li
 * @since 0.8
 */
@SuppressWarnings("java:S1192")
public final class Splitter {

    public static final Pattern WHITE_SPACE_PATTERN = Pattern.compile("\\s+", Pattern.UNICODE_CHARACTER_CLASS);

    private static final SubStringFunc defaultSubStringFunc = (source, start, end) -> source.subSequence(start, end).toString();

    private static final SubStringFunc trimSubStringFunc = (source, start, end) -> {
        while (start < end && source.charAt(start) == ' ') {
            start++;
        }

        while (end > start && source.charAt(end - 1) == ' ') {
            end--;
        }

        return start >= end ? Strings.EMPTY_STRING : source.subSequence(start, end).toString();
    };

    private static final SubStringFunc stripSubStringFunc = (source, start, end) -> {
        while (start < end && Character.isWhitespace(source.charAt(start))) {
            start++;
        }

        while (end > start && Character.isWhitespace(source.charAt(end - 1))) {
            end--;
        }

        return start >= end ? Strings.EMPTY_STRING : source.subSequence(start, end).toString();
    };

    private final Strategy strategy;
    private boolean omitEmptyStrings = false;
    private boolean trimResults = false;
    private boolean stripResults = false;
    private int limit = Integer.MAX_VALUE;

    Splitter(Strategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Returns the Splitter with the default delimiter: <code>", "</code>.
     *
     * @return
     */
    public static Splitter defauLt() {
        return with(Joiner.DEFAULT_DELIMITER);
    }

    /**
     *
     * @param delimiter
     * @return
     */
    public static Splitter with(final char delimiter) {
        return new Splitter((source, omitEmptyStrings, trim, strip, limit) -> {
            if (source == null) {
                return ObjIterator.empty();
            }

            return new ObjIterator<>() {
                private final SubStringFunc subStringFunc = strip ? stripSubStringFunc : (trim ? trimSubStringFunc : defaultSubStringFunc);
                private final int sourceLen = source.length();
                private String next = null;
                private int start = 0;
                private int cursor = 0;
                private int cnt = 0;

                @Override
                public boolean hasNext() {
                    if (next == null && (cursor >= 0 && cursor <= sourceLen)) {
                        if (limit - cnt == 1) {
                            next = subStringFunc.subString(source, start, sourceLen);
                            start = (cursor = sourceLen + 1);

                            if (omitEmptyStrings && next.length() == 0) {
                                next = null;
                            }
                        } else {
                            while (cursor >= 0 && cursor <= sourceLen) {
                                if (cursor == sourceLen || source.charAt(cursor) == delimiter) {
                                    next = subStringFunc.subString(source, start, cursor);
                                    start = ++cursor;

                                    if (omitEmptyStrings && next.length() == 0) {
                                        next = null;
                                    }

                                    if (next != null) {
                                        break;
                                    }
                                } else {
                                    cursor++;
                                }
                            }
                        }
                    }

                    return next != null;
                }

                @Override
                public String next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final String result = next;
                    next = null;
                    cnt++;
                    return result;
                }
            };
        });
    }

    /**
     *
     * @param delimiter
     * @return
     * @throws IllegalArgumentException if the specified {@code delimiter} is null or empty.
     */
    public static Splitter with(final CharSequence delimiter) throws IllegalArgumentException {
        N.checkArgNotEmpty(delimiter, "delimiter");

        if (Strings.isEmpty(delimiter)) {
            return with(WHITE_SPACE_PATTERN);
        } else if (delimiter.length() == 1) {
            return with(delimiter.charAt(0));
        } else {
            return new Splitter((source, omitEmptyStrings, trim, strip, limit) -> {
                if (source == null) {
                    return ObjIterator.empty();
                }

                return new ObjIterator<>() {
                    private final SubStringFunc subStringFunc = strip ? stripSubStringFunc : (trim ? trimSubStringFunc : defaultSubStringFunc);
                    @SuppressWarnings("deprecation")
                    private final char[] delimiterChars = InternalUtil.getCharsForReadOnly(delimiter.toString());
                    private final int sourceLen = source.length();
                    private final int delimiterLen = delimiterChars.length;
                    private String next = null;
                    private int start = 0;
                    private int cursor = 0;
                    private int cnt = 0;

                    @Override
                    public boolean hasNext() {
                        if (next == null && (cursor >= 0 && cursor <= sourceLen)) {
                            if (limit - cnt == 1) {
                                next = subStringFunc.subString(source, start, sourceLen);
                                start = (cursor = sourceLen + 1);

                                if (omitEmptyStrings && next.length() == 0) {
                                    next = null;
                                }
                            } else {
                                while (cursor >= 0 && cursor <= sourceLen) {
                                    if (cursor > sourceLen - delimiterLen || (source.charAt(cursor) == delimiterChars[0] && match(cursor))) {
                                        if (cursor > sourceLen - delimiterLen) {
                                            next = subStringFunc.subString(source, start, sourceLen);
                                            start = (cursor = sourceLen + 1);
                                        } else {
                                            next = subStringFunc.subString(source, start, cursor);
                                            start = (cursor += delimiter.length());
                                        }

                                        if (omitEmptyStrings && next.length() == 0) {
                                            next = null;
                                        }

                                        if (next != null) {
                                            break;
                                        }
                                    } else {
                                        cursor++;
                                    }
                                }
                            }
                        }

                        return next != null;
                    }

                    @Override
                    public String next() {
                        if (!hasNext()) {
                            throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                        }

                        final String result = next;
                        next = null;
                        cnt++;
                        return result;
                    }

                    private boolean match(int cursor) {
                        for (int i = 1; i < delimiterLen; i++) {
                            if (source.charAt(cursor + i) != delimiterChars[i]) {
                                return false;
                            }
                        }

                        return true;
                    }
                };
            });
        }
    }

    /**
     *
     * @param delimiter
     * @return
     * @throws IllegalArgumentException if the specified {@code delimiter} is null, or empty string may be matched by it.
     */
    public static Splitter with(final Pattern delimiter) throws IllegalArgumentException {
        N.checkArgNotNull(delimiter, "delimiter");
        N.checkArgument(!delimiter.matcher("").matches(), "Empty string may be matched by pattern: %s", delimiter);

        return new Splitter((source, omitEmptyStrings, trim, strip, limit) -> {
            if (source == null) {
                return ObjIterator.empty();
            }

            return new ObjIterator<>() {
                private final SubStringFunc subStringFunc = strip ? stripSubStringFunc : (trim ? trimSubStringFunc : defaultSubStringFunc);
                private final int sourceLen = source.length();
                private final Matcher matcher = delimiter.matcher(source);
                private String next = null;
                private int start = 0;
                private int cursor = 0;
                private int cnt = 0;
                private boolean matches = false;

                @Override
                public boolean hasNext() {
                    if (next == null && (cursor >= 0 && cursor <= sourceLen)) {
                        if (limit - cnt == 1) {
                            next = subStringFunc.subString(source, start, sourceLen);
                            start = (cursor = sourceLen + 1);

                            if (omitEmptyStrings && next.length() == 0) {
                                next = null;
                            }
                        } else {
                            while (cursor >= 0 && cursor <= sourceLen) {
                                if (cursor == sourceLen || (matches = matcher.find(start))) {
                                    if (matches) {
                                        next = subStringFunc.subString(source, start, matcher.start());
                                        start = (cursor = matcher.end());
                                        matches = false;
                                    } else {
                                        next = subStringFunc.subString(source, start, sourceLen);
                                        start = (cursor = sourceLen + 1);
                                    }

                                    if (omitEmptyStrings && next.length() == 0) {
                                        next = null;
                                    }

                                    if (next != null) {
                                        break;
                                    }
                                } else {
                                    cursor++;
                                }
                            }
                        }
                    }

                    return next != null;
                }

                @Override
                public String next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final String result = next;
                    next = null;
                    cnt++;
                    return result;
                }
            };
        });
    }

    /**
     *
     * @param delimiterRegex
     * @return
     * @throws IllegalArgumentException if the specified {@code delimiter} is null or empty, or empty string may be matched by it.
     */
    public static Splitter pattern(CharSequence delimiterRegex) throws IllegalArgumentException {
        N.checkArgNotEmpty(delimiterRegex, "delimiterRegex");

        return with(Pattern.compile(delimiterRegex.toString()));
    }

    /**
     * Omit empty strings.
     *
     * @param omitEmptyStrings
     * @return
     * @deprecated replaced by {@link #omitEmptyStrings()}
     */
    @Deprecated
    public Splitter omitEmptyStrings(boolean omitEmptyStrings) {
        this.omitEmptyStrings = omitEmptyStrings;

        return this;
    }

    /**
     *
     *
     * @return
     */
    public Splitter omitEmptyStrings() {
        this.omitEmptyStrings = true;

        return this;
    }

    /**
     *
     * @param trim
     * @return
     * @deprecated replaced by {@link #trimResults()}
     */
    @Deprecated
    public Splitter trim(boolean trim) {
        this.trimResults = trim;

        return this;
    }

    /**
     *
     *
     * @return
     */
    public Splitter trimResults() {
        this.trimResults = true;

        return this;
    }

    /**
     * Removes the starting and ending white space characters if {@code strip} is true.
     *
     * @param strip
     * @return
     * @see Character#isWhitespace(char)
     * @deprecated replaced by {@link #stripResults()}
     */
    @Deprecated
    public Splitter strip(boolean strip) {
        this.stripResults = strip;

        return this;
    }

    /**
     *
     * @return
     */
    public Splitter stripResults() {
        this.stripResults = true;

        return this;
    }

    /**
     *
     * @param limit
     * @return
     */
    public Splitter limit(int limit) {
        N.checkArgPositive(limit, "limit");

        this.limit = limit;

        return this;
    }

    /**
     *
     * @param source
     * @return
     */
    public List<String> split(final CharSequence source) {
        final List<String> result = new ArrayList<>();

        split(source, result);

        return result;
    }

    /**
     *
     * @param <C>
     * @param source
     * @param supplier
     * @return
     */
    public <C extends Collection<String>> C split(final CharSequence source, final Supplier<? extends C> supplier) {
        final C result = supplier.get();

        split(source, result);

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param <E>
     * @param source
     * @param mapper
     * @return
     * @throws E
     */
    public <T, E extends Exception> List<T> split(final CharSequence source, final Throwables.Function<? super String, ? extends T, E> mapper) throws E {
        final List<String> tmp = new ArrayList<>();
        split(source, tmp);

        @SuppressWarnings("rawtypes")
        final List<T> result = (List) tmp;

        for (int i = 0, size = tmp.size(); i < size; i++) {
            result.set(i, mapper.apply(tmp.get(i)));
        }

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param source
     * @param targetType
     * @return
     */
    public <T> List<T> split(final CharSequence source, final Class<? extends T> targetType) {
        N.checkArgNotNull(targetType, "targetType");

        final Type<T> type = N.typeOf(targetType);

        return split(source, type);
    }

    /**
     *
     *
     * @param <T>
     * @param <C>
     * @param source
     * @param targetType
     * @param supplier
     * @return
     */
    public <T, C extends Collection<T>> C split(final CharSequence source, final Class<? extends T> targetType, final Supplier<? extends C> supplier) {
        final C result = supplier.get();

        split(source, targetType, result);

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param source
     * @param targetType
     * @return
     */
    public <T> List<T> split(final CharSequence source, final Type<? extends T> targetType) {
        N.checkArgNotNull(targetType, "targetType");

        final List<T> result = new ArrayList<>();

        split(source, targetType, result);

        return result;
    }

    /**
     *
     *
     * @param <T>
     * @param <C>
     * @param source
     * @param targetType
     * @param supplier
     * @return
     */
    public <T, C extends Collection<T>> C split(final CharSequence source, final Type<? extends T> targetType, final Supplier<? extends C> supplier) {
        final C result = supplier.get();

        split(source, targetType, result);

        return result;
    }

    /**
     * 
     *
     * @param <C> 
     * @param source 
     * @param output 
     */
    public <C extends Collection<String>> void split(final CharSequence source, final C output) {
        N.checkArgNotNull(output, "output");

        final ObjIterator<String> iter = iterate(source);

        while (iter.hasNext()) {
            output.add(iter.next());
        }
    }

    /**
     * 
     *
     * @param <T> 
     * @param <C> 
     * @param source 
     * @param targetType 
     * @param output 
     * @return the specified output parameter: {@code output}
     */
    public <T, C extends Collection<T>> void split(final CharSequence source, final Class<? extends T> targetType, final C output) {
        N.checkArgNotNull(targetType, "targetType");
        N.checkArgNotNull(output, "output");

        final Type<T> type = N.typeOf(targetType);

        split(source, type, output);
    }

    /**
     * 
     *
     * @param <T> 
     * @param <C> 
     * @param source 
     * @param targetType 
     * @param output 
     */
    public <T, C extends Collection<T>> void split(final CharSequence source, final Type<? extends T> targetType, final C output) {
        N.checkArgNotNull(targetType, "targetType");
        N.checkArgNotNull(output, "output");

        final ObjIterator<String> iter = iterate(source);

        while (iter.hasNext()) {
            output.add(targetType.valueOf(iter.next()));
        }
    }

    /**
     * Split to array.
     *
     * @param source
     * @return
     */
    public ImmutableList<String> splitToImmutableList(final CharSequence source) {
        return ImmutableList.wrap(split(source));
    }

    /**
     * Split to array.
     *
     * @param <T>
     * @param source
     * @param targetType
     * @return
     */
    public <T> ImmutableList<T> splitToImmutableList(final CharSequence source, final Class<? extends T> targetType) {
        return ImmutableList.wrap(split(source, targetType));
    }

    /**
     * Split to array.
     *
     * @param source
     * @return
     */
    public String[] splitToArray(final CharSequence source) {
        final List<String> substrs = split(source);

        return substrs.toArray(new String[substrs.size()]);
    }

    /**
     * Split to array.
     *
     * @param <E>
     * @param source
     * @param mapper
     * @return
     * @throws E
     */
    public <E extends Exception> String[] splitToArray(final CharSequence source, final Throwables.Function<? super String, String, E> mapper) throws E {
        final List<String> substrs = split(source, mapper);

        return substrs.toArray(new String[substrs.size()]);
    }

    /**
     * Split to array.
     *
     * @param <T>
     * @param source
     * @param arrayType
     * @return
     */
    public <T> T splitToArray(final CharSequence source, final Class<T> arrayType) {
        N.checkArgNotNull(arrayType, "arrayType");

        final Class<?> eleCls = arrayType.getComponentType();

        final List<String> substrs = split(source);

        if (eleCls.equals(String.class) || eleCls.equals(Object.class)) {
            return (T) substrs.toArray((Object[]) N.newArray(eleCls, substrs.size()));
        } else {
            final Type<?> eleType = N.typeOf(eleCls);
            final Object a = N.newArray(eleCls, substrs.size());

            if (ClassUtil.isPrimitiveType(eleCls)) {
                for (int i = 0, len = substrs.size(); i < len; i++) {
                    Array.set(a, i, eleType.valueOf(substrs.get(i)));
                }
            } else {
                final Object[] objArray = (Object[]) a;

                for (int i = 0, len = substrs.size(); i < len; i++) {
                    objArray[i] = eleType.valueOf(substrs.get(i));
                }
            }

            return (T) a;
        }
    }

    /**
     * Split to array.
     * @param source
     * @param output
     *
     */
    public void splitToArray(final CharSequence source, final String[] output) {
        N.checkArgNotEmpty(output, "output");

        final ObjIterator<String> iter = iterate(source);

        for (int i = 0, len = output.length; i < len && iter.hasNext(); i++) {
            output[i] = iter.next();
        }
    }

    /**
     * Split to stream.
     *
     * @param source
     * @return
     */
    public Stream<String> splitToStream(final CharSequence source) {
        return Stream.of(iterate(source));
    }

    /**
     *
     * @param <R>
     * @param <E>
     * @param source
     * @param converter
     * @return
     * @throws E the e
     */
    public <R, E extends Exception> R splitThenApply(final CharSequence source, final Throwables.Function<? super List<String>, R, E> converter) throws E {
        return converter.apply(split(source));
    }

    /**
     *
     *
     * @param <E>
     * @param source
     * @param consumer
     * @throws E the e
     */
    public <E extends Exception> void splitThenAccept(final CharSequence source, final Throwables.Consumer<? super List<String>, E> consumer) throws E {
        consumer.accept(split(source));
    }

    /**
     *
     * @param source
     * @return
     */
    ObjIterator<String> iterate(final CharSequence source) {
        return strategy.split(source, omitEmptyStrings, trimResults, stripResults, limit);
    }

    /**
     * The Class MapSplitter.
     */
    public static final class MapSplitter {

        /** The entry splitter. */
        private final Splitter entrySplitter;

        /** The key value splitter. */
        private final Splitter keyValueSplitter;

        /**
         * Instantiates a new map splitter.
         *
         * @param entrySplitter
         * @param keyValueSplitter
         */
        MapSplitter(Splitter entrySplitter, Splitter keyValueSplitter) {
            this.entrySplitter = entrySplitter;
            this.keyValueSplitter = keyValueSplitter;
        }

        /**
         * Returns the Map Splitter with the default entry and key/value delimiter: <code>", "</code> and <code>"="</code>.
         *
         * @return
         */
        public static MapSplitter defauLt() {
            return with(Joiner.DEFAULT_DELIMITER, Joiner.DEFAULT_KEY_VALUE_DELIMITER);
        }

        /**
         *
         * @param entryDelimiter
         * @param keyValueDelimiter
         * @return
         * @throws IllegalArgumentException if the specified {@code entryDelimiter/keyValueDelimiter} is null or empty.
         * @see Splitter#with(CharSequence)
         */
        public static MapSplitter with(final CharSequence entryDelimiter, final CharSequence keyValueDelimiter) throws IllegalArgumentException {
            return new MapSplitter(Splitter.with(entryDelimiter), Splitter.with(keyValueDelimiter));
        }

        /**
         *
         * @param entryDelimiter
         * @param keyValueDelimiter
         * @return
         * @throws IllegalArgumentException if the specified {@code entryDelimiter/keyValueDelimiter} is null, or empty string may be matched by one of them.
         * @see Splitter#with(Pattern)
         */
        public static MapSplitter with(final Pattern entryDelimiter, final Pattern keyValueDelimiter) throws IllegalArgumentException {
            return new MapSplitter(Splitter.with(entryDelimiter), Splitter.with(keyValueDelimiter));
        }

        /**
         *
         * @param entryDelimiterRegex
         * @param keyValueDelimiterRegex
         * @return
         * @throws IllegalArgumentException if the specified {@code entryDelimiterRegex/keyValueDelimiterRegex} is null or empty, or empty string may be matched by one of them.
         * @see Splitter#pattern(CharSequence)
         */
        public static MapSplitter pattern(CharSequence entryDelimiterRegex, CharSequence keyValueDelimiterRegex) throws IllegalArgumentException {
            return new MapSplitter(Splitter.pattern(entryDelimiterRegex), Splitter.pattern(keyValueDelimiterRegex));
        }

        /**
         * Omit empty strings.
         *
         * @param omitEmptyStrings
         * @return
         * @deprecated replaced by {@link #omitEmptyStrings()}
         */
        @Deprecated
        public MapSplitter omitEmptyStrings(boolean omitEmptyStrings) {
            keyValueSplitter.omitEmptyStrings(omitEmptyStrings);

            return this;
        }

        /**
         *
         *
         * @return
         */
        public MapSplitter omitEmptyStrings() {
            keyValueSplitter.omitEmptyStrings();

            return this;
        }

        /**
         *
         * @param trim
         * @return
         * @deprecated replaced by {@link #trimResults()}
         */
        @Deprecated
        public MapSplitter trim(boolean trim) {
            entrySplitter.trim(trim);
            keyValueSplitter.trim(trim);

            return this;
        }

        /**
         *
         *
         * @return
         */
        public MapSplitter trimResults() {
            entrySplitter.trimResults();
            keyValueSplitter.trimResults();

            return this;
        }

        /**
         * Removes the starting and ending white space characters if {@code strip} is true.
         *
         * @param strip
         * @return
         * @see Character#isWhitespace(char)
         * @deprecated replaced by {@link #stripResults()}
         */
        @Deprecated
        public MapSplitter strip(boolean strip) {
            entrySplitter.strip(strip);
            keyValueSplitter.strip(strip);

            return this;
        }

        /**
         *
         *
         * @return
         */
        public MapSplitter stripResults() {
            entrySplitter.stripResults();
            keyValueSplitter.stripResults();

            return this;
        }

        /**
         *
         * @param limit
         * @return
         */
        public MapSplitter limit(int limit) {
            N.checkArgPositive(limit, "limit");

            entrySplitter.limit(limit);

            return this;
        }

        /**
         *
         * @param source
         * @return
         */
        public Map<String, String> split(final CharSequence source) {
            final LinkedHashMap<String, String> result = new LinkedHashMap<>();

            split(source, result);

            return result;
        }

        /**
         *
         * @param <M>
         * @param source
         * @param supplier
         * @return
         */
        public <M extends Map<String, String>> M split(final CharSequence source, final Supplier<? extends M> supplier) {
            final M result = supplier.get();

            split(source, result);

            return result;
        }

        /**
         *
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param source
         * @param keyType
         * @param valueType
         * @return
         */
        public <K, V> Map<K, V> split(final CharSequence source, final Class<K> keyType, final Class<V> valueType) {
            N.checkArgNotNull(keyType, "keyType");
            N.checkArgNotNull(valueType, "valueType");

            final Type<K> typeOfKey = N.typeOf(keyType);
            final Type<V> typeOfValue = N.typeOf(valueType);

            return split(source, typeOfKey, typeOfValue);
        }

        /**
         *
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param source
         * @param keyType
         * @param valueType
         * @return
         */
        public <K, V> Map<K, V> split(final CharSequence source, final Type<K> keyType, final Type<V> valueType) {
            N.checkArgNotNull(keyType, "keyType");
            N.checkArgNotNull(valueType, "valueType");

            final LinkedHashMap<K, V> result = new LinkedHashMap<>();

            split(source, keyType, valueType, result);

            return result;
        }

        /**
         *
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @param source
         * @param keyType
         * @param valueType
         * @param supplier
         * @return
         */
        public <K, V, M extends Map<K, V>> M split(final CharSequence source, final Class<K> keyType, final Class<V> valueType,
                final Supplier<? extends M> supplier) {
            final M result = supplier.get();

            split(source, keyType, valueType, result);

            return result;
        }

        /**
         *
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M>
         * @param source
         * @param keyType
         * @param valueType
         * @param supplier
         * @return
         */
        public <K, V, M extends Map<K, V>> M split(final CharSequence source, final Type<K> keyType, final Type<V> valueType,
                final Supplier<? extends M> supplier) {
            final M result = supplier.get();

            split(source, keyType, valueType, result);

            return result;
        }

        /**
         * 
         *
         * @param <M> 
         * @param source 
         * @param output 
         */
        public <M extends Map<String, String>> void split(final CharSequence source, final M output) {
            N.checkArgNotNull(output, "output");

            entrySplitter.omitEmptyStrings();
            keyValueSplitter.limit(2);

            final ObjIterator<String> iter = entrySplitter.iterate(source);
            ObjIterator<String> keyValueIter = null;
            String entryString = null;
            String key = null;
            String value = null;

            while (iter.hasNext()) {
                entryString = iter.next();
                keyValueIter = keyValueSplitter.iterate(entryString);

                if (keyValueIter.hasNext()) {
                    key = keyValueIter.next();

                    if (keyValueIter.hasNext()) {
                        value = keyValueIter.next();
                    } else {
                        throw new IllegalArgumentException("Invalid map entry String: " + entryString);
                    }

                    if (keyValueIter.hasNext()) {
                        throw new IllegalArgumentException("Invalid map entry String: " + entryString);
                    } else {
                        output.put(key, value);
                    }
                }
            }
        }

        /**
         * 
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> 
         * @param source 
         * @param keyType 
         * @param valueType 
         * @param output 
         */
        public <K, V, M extends Map<K, V>> void split(final CharSequence source, final Class<K> keyType, final Class<V> valueType, final M output) {
            N.checkArgNotNull(keyType, "keyType");
            N.checkArgNotNull(valueType, "valueType");
            N.checkArgNotNull(output, "output");

            final Type<K> typeOfKey = N.typeOf(keyType);
            final Type<V> typeOfValue = N.typeOf(valueType);

            split(source, typeOfKey, typeOfValue, output);
        }

        /**
         * 
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> 
         * @param source 
         * @param keyType 
         * @param valueType 
         * @param output 
         */
        public <K, V, M extends Map<K, V>> void split(final CharSequence source, final Type<K> keyType, final Type<V> valueType, final M output) {
            N.checkArgNotNull(keyType, "keyType");
            N.checkArgNotNull(valueType, "valueType");
            N.checkArgNotNull(output, "output");

            entrySplitter.omitEmptyStrings();
            keyValueSplitter.limit(2);

            final ObjIterator<String> iter = entrySplitter.iterate(source);
            ObjIterator<String> keyValueIter = null;
            String entryString = null;
            String key = null;
            String value = null;

            while (iter.hasNext()) {
                entryString = iter.next();
                keyValueIter = keyValueSplitter.iterate(entryString);

                if (keyValueIter.hasNext()) {
                    key = keyValueIter.next();

                    if (keyValueIter.hasNext()) {
                        value = keyValueIter.next();
                    } else {
                        throw new IllegalArgumentException("Invalid map entry String: " + entryString);
                    }

                    if (keyValueIter.hasNext()) {
                        throw new IllegalArgumentException("Invalid map entry String: " + entryString);
                    } else {
                        output.put(keyType.valueOf(key), valueType.valueOf(value));
                    }
                }
            }
        }

        /**
         *
         * @param source
         * @return
         */
        public ImmutableMap<String, String> splitToImmutableMap(final CharSequence source) {
            return ImmutableMap.wrap(split(source));
        }

        /**
         *
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param source
         * @param keyType
         * @param valueType
         * @return
         */
        public <K, V> ImmutableMap<K, V> splitToImmutableMap(final CharSequence source, final Class<K> keyType, final Class<V> valueType) {
            return ImmutableMap.wrap(split(source, keyType, valueType));
        }

        /**
         * Split to stream.
         *
         * @param source
         * @return
         */
        public Stream<Map.Entry<String, String>> splitToStream(final CharSequence source) {
            entrySplitter.omitEmptyStrings();
            keyValueSplitter.limit(2);

            return Stream.of(new ObjIteratorEx<Map.Entry<String, String>>() {
                private final ObjIterator<String> iter = entrySplitter.iterate(source);
                private ObjIterator<String> keyValueIter = null;
                private String entryString = null;
                private String key = null;
                private String value = null;
                private Map.Entry<String, String> next;

                @Override
                public boolean hasNext() {
                    if (next == null) {
                        while (iter.hasNext()) {
                            entryString = iter.next();
                            keyValueIter = keyValueSplitter.iterate(entryString);

                            if (keyValueIter.hasNext()) {
                                key = keyValueIter.next();

                                if (keyValueIter.hasNext()) {
                                    value = keyValueIter.next();
                                } else {
                                    throw new IllegalArgumentException("Invalid map entry String: " + entryString);
                                }

                                if (keyValueIter.hasNext()) {
                                    throw new IllegalArgumentException("Invalid map entry String: " + entryString);
                                } else {
                                    next = new ImmutableEntry<>(key, value);
                                    break;
                                }
                            }
                        }
                    }

                    return next != null;
                }

                @Override
                public Map.Entry<String, String> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException(InternalUtil.ERROR_MSG_FOR_NO_SUCH_EX);
                    }

                    final Map.Entry<String, String> result = next;
                    next = null;
                    return result;
                }
            });
        }

        /**
         * Split to entry stream.
         *
         * @param source
         * @return
         */
        public EntryStream<String, String> splitToEntryStream(final CharSequence source) {
            return splitToStream(source).mapToEntry(Fn.<Map.Entry<String, String>> identity());
        }

        /**
         *
         * @param <T>
         * @param <E>
         * @param source
         * @param converter
         * @return
         * @throws E the e
         */
        public <T, E extends Exception> T splitThenApply(final CharSequence source, final Throwables.Function<? super Map<String, String>, T, E> converter)
                throws E {
            return converter.apply(split(source));
        }

        /**
         *
         *
         * @param <E>
         * @param source
         * @param consumer
         * @throws E the e
         */
        public <E extends Exception> void splitThenAccept(final CharSequence source, final Throwables.Consumer<? super Map<String, String>, E> consumer)
                throws E {
            consumer.accept(split(source));
        }
    }

    /**
     * The Interface Strategy.
     */
    interface Strategy {

        /**
         *
         * @param toSplit
         * @param omitEmptyStrings
         * @param trim
         * @param strip
         * @param limit
         * @return
         */
        ObjIterator<String> split(CharSequence toSplit, boolean omitEmptyStrings, boolean trim, final boolean strip, int limit);
    }

    /**
     * The Interface SubStringFunc.
     */
    interface SubStringFunc {

        /**
         *
         * @param source
         * @param start
         * @param end
         * @return
         */
        String subString(CharSequence source, int start, int end);
    }
}
