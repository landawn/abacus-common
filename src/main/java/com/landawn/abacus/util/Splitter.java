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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.function.Supplier;
import com.landawn.abacus.util.stream.EntryStream;
import com.landawn.abacus.util.stream.ObjIteratorEx;
import com.landawn.abacus.util.stream.Stream;

// TODO: Auto-generated Javadoc
/**
 * The Class Splitter.
 *
 * @author Haiyang Li
 * @since 0.8
 */
public final class Splitter {

    /** The Constant WHITE_SPACE_PATTERN. */
    public static final Pattern WHITE_SPACE_PATTERN = Pattern.compile("\\s+");

    /** The Constant defaultSubStringFunc. */
    private static final SubStringFunc defaultSubStringFunc = new SubStringFunc() {
        @Override
        public String subString(CharSequence source, int start, int end) {
            return source.subSequence(start, end).toString();
        }
    };

    /** The Constant trimSubStringFunc. */
    private static final SubStringFunc trimSubStringFunc = new SubStringFunc() {
        @Override
        public String subString(CharSequence source, int start, int end) {
            while (start < end && source.charAt(start) == ' ') {
                start++;
            }

            while (end > start && source.charAt(end - 1) == ' ') {
                end--;
            }

            return start >= end ? N.EMPTY_STRING : source.subSequence(start, end).toString();
        }
    };

    /** The Constant stripSubStringFunc. */
    private static final SubStringFunc stripSubStringFunc = new SubStringFunc() {
        @Override
        public String subString(CharSequence source, int start, int end) {
            while (start < end && Character.isWhitespace(source.charAt(start))) {
                start++;
            }

            while (end > start && Character.isWhitespace(source.charAt(end - 1))) {
                end--;
            }

            return start >= end ? N.EMPTY_STRING : source.subSequence(start, end).toString();
        }
    };

    /** The strategy. */
    private final Strategy strategy;

    /** The omit empty strings. */
    private boolean omitEmptyStrings = false;

    /** The trim. */
    private boolean trim = false;

    /** The strip. */
    private boolean strip = false;

    /** The limit. */
    private int limit = Integer.MAX_VALUE;

    /**
     * Instantiates a new splitter.
     *
     * @param strategy the strategy
     */
    Splitter(Strategy strategy) {
        this.strategy = strategy;
    }

    /**
     * Returns the Splitter with the default delimiter: <code>", "</code>.
     *
     * @return the splitter
     */
    public static Splitter defauLt() {
        return with(Joiner.DEFAULT_DELIMITER);
    }

    /**
     * With.
     *
     * @param delimiter the delimiter
     * @return the splitter
     */
    public static Splitter with(final char delimiter) {
        return new Splitter(new Strategy() {
            @Override
            public ObjIterator<String> split(final CharSequence source, final boolean omitEmptyStrings, final boolean trim, final boolean strip,
                    final int limit) {
                if (source == null) {
                    return ObjIterator.empty();
                }

                return new ObjIterator<String>() {
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
                        if (hasNext() == false) {
                            throw new NoSuchElementException();
                        }

                        final String result = next;
                        next = null;
                        cnt++;
                        return result;
                    }
                };
            }
        });
    }

    /**
     * With.
     *
     * @param delimiter the delimiter
     * @return the splitter
     * @throws IllegalArgumentException if the specified {@code delimiter} is null or empty.
     */
    public static Splitter with(final CharSequence delimiter) throws IllegalArgumentException {
        N.checkArgNotNullOrEmpty(delimiter, "delimiter");

        if (N.isNullOrEmpty(delimiter)) {
            return with(WHITE_SPACE_PATTERN);
        } else if (delimiter.length() == 1) {
            return with(delimiter.charAt(0));
        } else {
            return new Splitter(new Strategy() {
                @Override
                public ObjIterator<String> split(final CharSequence source, final boolean omitEmptyStrings, final boolean trim, final boolean strip,
                        final int limit) {
                    if (source == null) {
                        return ObjIterator.empty();
                    }

                    return new ObjIterator<String>() {
                        private final SubStringFunc subStringFunc = strip ? stripSubStringFunc : (trim ? trimSubStringFunc : defaultSubStringFunc);
                        @SuppressWarnings("deprecation")
                        private final char[] sourceChars = StringUtil.getCharsForReadOnly(source.toString());
                        @SuppressWarnings("deprecation")
                        private final char[] delimiterChars = StringUtil.getCharsForReadOnly(delimiter.toString());
                        private final int sourceLen = sourceChars.length;
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
                                        if (cursor > sourceLen - delimiterLen || (sourceChars[cursor] == delimiterChars[0] && match(cursor))) {
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
                            if (hasNext() == false) {
                                throw new NoSuchElementException();
                            }

                            final String result = next;
                            next = null;
                            cnt++;
                            return result;
                        }

                        private boolean match(int cursor) {
                            for (int i = 1; i < delimiterLen; i++) {
                                if (sourceChars[cursor + i] != delimiterChars[i]) {
                                    return false;
                                }
                            }

                            return true;
                        }
                    };
                }
            });
        }
    }

    /**
     * With.
     *
     * @param delimiter the delimiter
     * @return the splitter
     * @throws IllegalArgumentException if the specified {@code delimiter} is null, or empty string may be matched by it.
     */
    public static Splitter with(final Pattern delimiter) throws IllegalArgumentException {
        N.checkArgNotNull(delimiter, "delimiter");
        N.checkArgument(!delimiter.matcher("").matches(), "Empty string may be matched by pattern: %s", delimiter);

        return new Splitter(new Strategy() {
            @Override
            public ObjIterator<String> split(final CharSequence source, final boolean omitEmptyStrings, final boolean trim, final boolean strip,
                    final int limit) {
                if (source == null) {
                    return ObjIterator.empty();
                }

                return new ObjIterator<String>() {
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
                        if (hasNext() == false) {
                            throw new NoSuchElementException();
                        }

                        final String result = next;
                        next = null;
                        cnt++;
                        return result;
                    }
                };
            }
        });
    }

    /**
     * Pattern.
     *
     * @param delimiterRegex the delimiter regex
     * @return the splitter
     * @throws IllegalArgumentException if the specified {@code delimiter} is null or empty, or empty string may be matched by it.
     */
    public static Splitter pattern(CharSequence delimiterRegex) throws IllegalArgumentException {
        N.checkArgNotNullOrEmpty(delimiterRegex, "delimiterRegex");

        return with(Pattern.compile(delimiterRegex.toString()));
    }

    /**
     * Omit empty strings.
     *
     * @param omitEmptyStrings the omit empty strings
     * @return the splitter
     */
    public Splitter omitEmptyStrings(boolean omitEmptyStrings) {
        this.omitEmptyStrings = omitEmptyStrings;

        return this;
    }

    /**
     * Trim.
     *
     * @param trim the trim
     * @return the splitter
     */
    public Splitter trim(boolean trim) {
        this.trim = trim;

        return this;
    }

    /**
     * Removes the starting and ending white space characters if {@code strip} is true.
     *
     * @param strip the strip
     * @return the splitter
     * @see Character#isWhitespace(char)
     */
    public Splitter strip(boolean strip) {
        this.strip = strip;

        return this;
    }

    /**
     * Limit.
     *
     * @param limit the limit
     * @return the splitter
     */
    public Splitter limit(int limit) {
        N.checkArgPositive(limit, "limit");

        this.limit = limit;

        return this;
    }

    /**
     * Split.
     *
     * @param source the source
     * @return the list
     */
    public List<String> split(final CharSequence source) {
        final List<String> result = new ArrayList<>();
        split(result, source);
        return result;
    }

    /**
     * Split.
     *
     * @param <T> the generic type
     * @param targetType the target type
     * @param source the source
     * @return the list
     */
    public <T> List<T> split(Class<T> targetType, final CharSequence source) {
        N.checkArgNotNull(targetType, "targetType");

        final Type<T> type = N.typeOf(targetType);

        return split(type, source);
    }

    /**
     * Split.
     *
     * @param <T> the generic type
     * @param targetType the target type
     * @param source the source
     * @return the list
     */
    public <T> List<T> split(Type<T> targetType, final CharSequence source) {
        N.checkArgNotNull(targetType, "targetType");

        final List<T> result = new ArrayList<>();
        split(result, targetType, source);
        return result;
    }

    /**
     * Split.
     *
     * @param <C> the generic type
     * @param output the output
     * @param source the source
     * @return the c
     */
    public <C extends Collection<String>> C split(final C output, final CharSequence source) {
        N.checkArgNotNull(output, "output");

        final ObjIterator<String> iter = iterate(source);

        while (iter.hasNext()) {
            output.add(iter.next());
        }

        return output;
    }

    /**
     * Split.
     *
     * @param <T> the generic type
     * @param <C> the generic type
     * @param output the output
     * @param targetType the target type
     * @param source the source
     * @return the c
     */
    public <T, C extends Collection<T>> C split(final C output, Class<T> targetType, final CharSequence source) {
        N.checkArgNotNull(output, "output");
        N.checkArgNotNull(targetType, "targetType");

        final Type<T> type = N.typeOf(targetType);

        return split(output, type, source);
    }

    /**
     * Split.
     *
     * @param <T> the generic type
     * @param <C> the generic type
     * @param output the output
     * @param targetType the target type
     * @param source the source
     * @return the c
     */
    public <T, C extends Collection<T>> C split(final C output, Type<T> targetType, final CharSequence source) {
        N.checkArgNotNull(output, "output");
        N.checkArgNotNull(targetType, "targetType");

        final ObjIterator<String> iter = iterate(source);

        while (iter.hasNext()) {
            output.add(targetType.valueOf(iter.next()));
        }

        return output;
    }

    /**
     * Split.
     *
     * @param <C> the generic type
     * @param source the source
     * @param supplier the supplier
     * @return the c
     */
    public <C extends Collection<String>> C split(final CharSequence source, final Supplier<? extends C> supplier) {
        return split(supplier.get(), source);
    }

    /**
     * Split.
     *
     * @param <T> the generic type
     * @param <C> the generic type
     * @param targetType the target type
     * @param source the source
     * @param supplier the supplier
     * @return the c
     */
    public <T, C extends Collection<T>> C split(Class<T> targetType, final CharSequence source, final Supplier<? extends C> supplier) {
        return split(supplier.get(), targetType, source);
    }

    /**
     * Split.
     *
     * @param <T> the generic type
     * @param <C> the generic type
     * @param targetType the target type
     * @param source the source
     * @param supplier the supplier
     * @return the c
     */
    public <T, C extends Collection<T>> C split(Type<T> targetType, final CharSequence source, final Supplier<? extends C> supplier) {
        return split(supplier.get(), targetType, source);
    }

    /**
     * Split to array.
     *
     * @param source the source
     * @return the string[]
     */
    public String[] splitToArray(final CharSequence source) {
        final List<String> substrs = split(source);

        return substrs.toArray(new String[substrs.size()]);
    }

    /**
     * Split to array.
     *
     * @param <T> the generic type
     * @param arrayType the array type
     * @param source the source
     * @return the t
     */
    public <T> T splitToArray(Class<T> arrayType, final CharSequence source) {
        N.checkArgNotNull(arrayType, "arrayType");

        final Class<?> eleCls = arrayType.getComponentType();

        final List<String> substrs = split(source);

        if (eleCls.equals(String.class) || eleCls.equals(Object.class)) {
            return (T) substrs.toArray((Object[]) N.newArray(eleCls, substrs.size()));
        } else {
            final Type<?> eleType = N.typeOf(eleCls);
            final Object a = N.newArray(eleCls, substrs.size());

            if (Primitives.isPrimitiveType(eleCls)) {
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
     * Split to stream.
     *
     * @param source the source
     * @return the stream
     */
    public Stream<String> splitToStream(final CharSequence source) {
        return Stream.of(iterate(source));
    }

    /**
     * Split and then.
     *
     * @param <T> the generic type
     * @param <E> the element type
     * @param source the source
     * @param converter the converter
     * @return the t
     * @throws E the e
     */
    public <T, E extends Exception> T splitAndThen(final CharSequence source, Try.Function<? super List<String>, T, E> converter) throws E {
        N.checkArgNotNull(converter, "converter");

        return converter.apply(split(source));
    }

    /**
     * Iterate.
     *
     * @param source the source
     * @return the obj iterator
     */
    ObjIterator<String> iterate(final CharSequence source) {
        return strategy.split(source, omitEmptyStrings, trim, strip, limit);
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
         * @param entrySplitter the entry splitter
         * @param keyValueSplitter the key value splitter
         */
        MapSplitter(Splitter entrySplitter, Splitter keyValueSplitter) {
            this.entrySplitter = entrySplitter;
            this.keyValueSplitter = keyValueSplitter;
        }

        /**
         * Returns the Map Splitter with the default entry and key/value delimiter: <code>", "</code> and <code>"="</code>.
         *
         * @return the map splitter
         */
        public static MapSplitter defauLt() {
            return with(Joiner.DEFAULT_DELIMITER, Joiner.DEFAULT_KEY_VALUE_DELIMITER);
        }

        /**
         * With.
         *
         * @param entryDelimiter the entry delimiter
         * @param keyValueDelimiter the key value delimiter
         * @return the map splitter
         * @throws IllegalArgumentException if the specified {@code entryDelimiter/keyValueDelimiter} is null or empty.
         * @see Splitter#with(CharSequence)
         */
        public static MapSplitter with(final CharSequence entryDelimiter, final CharSequence keyValueDelimiter) throws IllegalArgumentException {
            return new MapSplitter(Splitter.with(entryDelimiter), Splitter.with(keyValueDelimiter));
        }

        /**
         * With.
         *
         * @param entryDelimiter the entry delimiter
         * @param keyValueDelimiter the key value delimiter
         * @return the map splitter
         * @throws IllegalArgumentException if the specified {@code entryDelimiter/keyValueDelimiter} is null, or empty string may be matched by one of them.
         * @see Splitter#with(Pattern)
         */
        public static MapSplitter with(final Pattern entryDelimiter, final Pattern keyValueDelimiter) throws IllegalArgumentException {
            return new MapSplitter(Splitter.with(entryDelimiter), Splitter.with(keyValueDelimiter));
        }

        /**
         * Pattern.
         *
         * @param entryDelimiterRegex the entry delimiter regex
         * @param keyValueDelimiterRegex the key value delimiter regex
         * @return the map splitter
         * @throws IllegalArgumentException if the specified {@code entryDelimiterRegex/keyValueDelimiterRegex} is null or empty, or empty string may be matched by one of them.
         * @see Splitter#pattern(CharSequence)
         */
        public static MapSplitter pattern(CharSequence entryDelimiterRegex, CharSequence keyValueDelimiterRegex) throws IllegalArgumentException {
            return new MapSplitter(Splitter.pattern(entryDelimiterRegex), Splitter.pattern(keyValueDelimiterRegex));
        }

        /**
         * Omit empty strings.
         *
         * @param omitEmptyStrings the omit empty strings
         * @return the map splitter
         */
        public MapSplitter omitEmptyStrings(boolean omitEmptyStrings) {
            keyValueSplitter.omitEmptyStrings(omitEmptyStrings);

            return this;
        }

        /**
         * Trim.
         *
         * @param trim the trim
         * @return the map splitter
         */
        public MapSplitter trim(boolean trim) {
            entrySplitter.trim(trim);
            keyValueSplitter.trim(trim);

            return this;
        }

        /**
         * Removes the starting and ending white space characters if {@code strip} is true.
         *
         * @param strip the strip
         * @return the map splitter
         * @see Character#isWhitespace(char)
         */
        public MapSplitter strip(boolean strip) {
            entrySplitter.strip(strip);
            keyValueSplitter.strip(strip);

            return this;
        }

        /**
         * Limit.
         *
         * @param limit the limit
         * @return the map splitter
         */
        public MapSplitter limit(int limit) {
            N.checkArgPositive(limit, "limit");

            entrySplitter.limit(limit);

            return this;
        }

        /**
         * Split.
         *
         * @param source the source
         * @return the map
         */
        public Map<String, String> split(final CharSequence source) {
            return split(new LinkedHashMap<String, String>(), source);
        }

        /**
         * Split.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param keyType the key type
         * @param valueType the value type
         * @param source the source
         * @return the map
         */
        public <K, V> Map<K, V> split(Class<K> keyType, Class<V> valueType, final CharSequence source) {
            N.checkArgNotNull(keyType, "keyType");
            N.checkArgNotNull(valueType, "valueType");

            final Type<K> typeOfKey = N.typeOf(keyType);
            final Type<V> typeOfValue = N.typeOf(valueType);

            return split(typeOfKey, typeOfValue, source);
        }

        /**
         * Split.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param keyType the key type
         * @param valueType the value type
         * @param source the source
         * @return the map
         */
        public <K, V> Map<K, V> split(Type<K> keyType, Type<V> valueType, final CharSequence source) {
            N.checkArgNotNull(keyType, "keyType");
            N.checkArgNotNull(valueType, "valueType");

            return split(new LinkedHashMap<K, V>(), keyType, valueType, source);
        }

        /**
         * Split.
         *
         * @param <M> the generic type
         * @param output the output
         * @param source the source
         * @return the m
         */
        public <M extends Map<String, String>> M split(final M output, final CharSequence source) {
            N.checkArgNotNull(output, "output");

            entrySplitter.omitEmptyStrings(true);
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

            return output;
        }

        /**
         * Split.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> the generic type
         * @param output the output
         * @param keyType the key type
         * @param valueType the value type
         * @param source the source
         * @return the m
         */
        public <K, V, M extends Map<K, V>> M split(final M output, Class<K> keyType, Class<V> valueType, final CharSequence source) {
            N.checkArgNotNull(output, "output");
            N.checkArgNotNull(keyType, "keyType");
            N.checkArgNotNull(valueType, "valueType");

            final Type<K> typeOfKey = N.typeOf(keyType);
            final Type<V> typeOfValue = N.typeOf(valueType);

            return split(output, typeOfKey, typeOfValue, source);
        }

        /**
         * Split.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> the generic type
         * @param output the output
         * @param keyType the key type
         * @param valueType the value type
         * @param source the source
         * @return the m
         */
        public <K, V, M extends Map<K, V>> M split(final M output, Type<K> keyType, Type<V> valueType, final CharSequence source) {
            N.checkArgNotNull(output, "output");
            N.checkArgNotNull(keyType, "keyType");
            N.checkArgNotNull(valueType, "valueType");

            entrySplitter.omitEmptyStrings(true);
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

            return output;
        }

        /**
         * Split.
         *
         * @param <M> the generic type
         * @param source the source
         * @param supplier the supplier
         * @return the m
         */
        public <M extends Map<String, String>> M split(final CharSequence source, final Supplier<? extends M> supplier) {
            return split(supplier.get(), source);
        }

        /**
         * Split.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> the generic type
         * @param keyType the key type
         * @param valueType the value type
         * @param source the source
         * @param supplier the supplier
         * @return the m
         */
        public <K, V, M extends Map<K, V>> M split(final Class<K> keyType, final Class<V> valueType, final CharSequence source,
                final Supplier<? extends M> supplier) {
            return split(supplier.get(), keyType, valueType, source);
        }

        /**
         * Split.
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param <M> the generic type
         * @param keyType the key type
         * @param valueType the value type
         * @param source the source
         * @param supplier the supplier
         * @return the m
         */
        public <K, V, M extends Map<K, V>> M split(final Type<K> keyType, final Type<V> valueType, final CharSequence source,
                final Supplier<? extends M> supplier) {
            return split(supplier.get(), keyType, valueType, source);
        }

        /**
         * Split to stream.
         *
         * @param source the source
         * @return the stream
         */
        public Stream<Map.Entry<String, String>> splitToStream(final CharSequence source) {
            entrySplitter.omitEmptyStrings(true);
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
                    if (hasNext() == false) {
                        throw new NoSuchElementException();
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
         * @param source the source
         * @return the entry stream
         */
        public EntryStream<String, String> splitToEntryStream(final CharSequence source) {
            return splitToStream(source).mapToEntry(Fn.<Map.Entry<String, String>> identity());
        }

        /**
         * Split and then.
         *
         * @param <T> the generic type
         * @param <E> the element type
         * @param source the source
         * @param converter the converter
         * @return the t
         * @throws E the e
         */
        public <T, E extends Exception> T splitAndThen(final CharSequence source, Try.Function<? super Map<String, String>, T, E> converter) throws E {
            return converter.apply(split(source));
        }
    }

    /**
     * The Interface Strategy.
     */
    static interface Strategy {

        /**
         * Split.
         *
         * @param toSplit the to split
         * @param omitEmptyStrings the omit empty strings
         * @param trim the trim
         * @param strip the strip
         * @param limit the limit
         * @return the obj iterator
         */
        ObjIterator<String> split(CharSequence toSplit, boolean omitEmptyStrings, boolean trim, final boolean strip, int limit);
    }

    /**
     * The Interface SubStringFunc.
     */
    static interface SubStringFunc {

        /**
         * Sub string.
         *
         * @param source the source
         * @param start the start
         * @param end the end
         * @return the string
         */
        String subString(CharSequence source, int start, int end);
    }
}
