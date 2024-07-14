/*
 * Copyright (C) 2019 HaiYang Li
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

import java.util.AbstractMap;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntFunction;

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SequentialOnly;
import com.landawn.abacus.annotation.Stateful;
import com.landawn.abacus.annotation.SuppressFBWarnings;

/**
 * One-off Object. No caching/saving in memory, No updating. To cache/save/update the Object, call {@code clone()/copy()}.
 *
 * @since 1.3
 *
 * @author Haiyang Li
 */
@Beta
@SequentialOnly
@Stateful
public interface NoCachingNoUpdating {

    /**
     * One-off Object. No caching/saving in memory, No updating. To cache/save/update the Object, call {@code clone()/copy()}.
     * <br />
     * Depends on context, it should be okay to cache/save the elements from the array, but never save or cache the array itself.
     *
     * @param <T>
     */
    @Beta
    @SequentialOnly
    @Stateful
    @SuppressFBWarnings("CN_IDIOM_NO_SUPER_CALL")
    public static class DisposableArray<T> implements NoCachingNoUpdating, Cloneable, Iterable<T> {

        /** The a. */
        private final T[] a;

        /**
         * Instantiates a new disposable array.
         *
         * @param a
         */
        protected DisposableArray(final T[] a) {
            N.checkArgNotNull(a, "a");
            this.a = a;
        }

        /**
         *
         *
         * @param <T>
         * @param componentType
         * @param len
         * @return
         */
        public static <T> DisposableArray<T> create(final Class<T> componentType, final int len) {
            return new DisposableArray<>(N.newArray(componentType, len));
        }

        /**
         *
         * @param <T>
         * @param a
         * @return
         */
        public static <T> DisposableArray<T> wrap(final T[] a) {
            return new DisposableArray<>(a);
        }

        /**
         *
         * @param index
         * @return
         */
        public T get(final int index) {
            return a[index];
        }

        /**
         *
         * @return
         */
        public int length() {
            return a.length;
        }

        /**
         *
         * @param <A>
         * @param target
         * @return
         */
        public <A> A[] toArray(A[] target) {
            if (target.length < length()) {
                target = N.newArray(target.getClass().getComponentType(), length());
            }

            N.copy(this.a, 0, target, 0, length());

            return target;
        }

        /**
         *
         * @return
         */
        @Override
        public T[] clone() { //NOSONAR
            return N.clone(a);
        }

        /**
         *
         * @return
         */
        public List<T> toList() {
            return N.toList(clone());
        }

        /**
         *
         * @return
         */
        public Set<T> toSet() {
            return N.toSet(a);
        }

        /**
         *
         * @param <C>
         * @param supplier
         * @return
         */
        public <C extends Collection<T>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());
            result.addAll(toList());
            return result;
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void forEach(final Throwables.Consumer<? super T, E> action) throws E {
            for (T e : a) {
                action.accept(e);
            }
        }

        /**
         *
         *
         * @param <R>
         * @param <E>
         * @param func
         * @return
         * @throws E
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super T[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super T[], E> action) throws E {
            action.accept(a);
        }

        /**
         *
         * @param delimiter
         * @return
         */
        public String join(String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         *
         * @param delimiter
         * @param prefix
         * @param suffix
         * @return
         */
        public String join(String delimiter, String prefix, String suffix) {
            return Strings.join(a, delimiter, prefix, suffix);
        }

        /**
         * 
         *
         * @return 
         */
        @Override
        public Iterator<T> iterator() {
            return ObjIterator.of(a);
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         *
         * @return
         */
        protected T[] values() {
            return a;
        }
    }

    /**
     * One-off Object. No caching/saving in memory, No updating. To cache/save/update the Object, call {@code clone()/copy()}.
     * <br />
     * Depends on context, it should be okay to cache/save the elements from the array, but never save or cache the array itself.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static class DisposableObjArray extends DisposableArray<Object> {

        /**
         * Instantiates a new disposable obj array.
         *
         * @param a
         */
        protected DisposableObjArray(Object[] a) {
            super(a);
        }

        /**
         *
         *
         * @param len
         * @return
         */
        public static DisposableObjArray create(final int len) {
            return new DisposableObjArray(new Object[len]);
        }

        /**
         *
         * @param <T>
         * @param componentType
         * @param len
         * @return
         * @throws UnsupportedOperationException
         * @deprecated throw UnsupportedOperation
         */
        @Deprecated
        public static <T> DisposableArray<T> create(final Class<T> componentType, final int len) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         *
         * @param a
         * @return
         */
        public static DisposableObjArray wrap(final Object[] a) {
            return new DisposableObjArray(a);
        }
    }

    /**
     * One-off Object. No caching/saving in memory, No updating. To cache/save/update the Object, call {@code clone()/copy()}.
     * <br />
     * Depends on context, it should be okay to cache/save the elements from the array, but never save or cache the array itself.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static class DisposableBooleanArray implements NoCachingNoUpdating, Cloneable {

        /** The a. */
        private final boolean[] a;

        /**
         * Instantiates a new disposable boolean array.
         *
         * @param a
         */
        protected DisposableBooleanArray(final boolean[] a) {
            N.checkArgNotNull(a, "a");
            this.a = a;
        }

        /**
         *
         *
         * @param len
         * @return
         */
        public static DisposableBooleanArray create(final int len) {
            return new DisposableBooleanArray(new boolean[len]);
        }

        /**
         *
         * @param a
         * @return
         */
        public static DisposableBooleanArray wrap(final boolean[] a) {
            return new DisposableBooleanArray(a);
        }

        /**
         *
         * @param index
         * @return
         */
        public boolean get(final int index) { // NOSONAR
            return a[index];
        }

        /**
         *
         * @return
         */
        public int length() {
            return a.length;
        }

        /**
         *
         * @return
         */
        @Override
        public boolean[] clone() { //NOSONAR
            return N.clone(a);
        }

        /**
         *
         * @return
         */
        public Boolean[] box() {
            return Array.box(a);
        }

        /**
         *
         * @return
         */
        public BooleanList toList() {
            return BooleanList.of(clone());
        }

        /**
         *
         * @param <C>
         * @param supplier
         * @return
         */
        public <C extends Collection<Boolean>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());

            for (boolean e : a) {
                result.add(e);
            }

            return result;
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void forEach(final Throwables.BooleanConsumer<E> action) throws E {
            for (boolean e : a) {
                action.accept(e);
            }
        }

        /**
         *
         *
         * @param <R>
         * @param <E>
         * @param func
         * @return
         * @throws E
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super boolean[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super boolean[], E> action) throws E {
            action.accept(a);
        }

        /**
         *
         * @param delimiter
         * @return
         */
        public String join(String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         *
         * @param delimiter
         * @param prefix
         * @param suffix
         * @return
         */
        public String join(String delimiter, String prefix, String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         *
         * @return
         */
        protected boolean[] values() {
            return a;
        }
    }

    /**
     * One-off Object. No caching/saving in memory, No updating. To cache/save/update the Object, call {@code clone()/copy()}.
     * <br />
     * Depends on context, it should be okay to cache/save the elements from the array, but never save or cache the array itself.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static class DisposableCharArray implements NoCachingNoUpdating, Cloneable {

        /** The a. */
        private final char[] a;

        /**
         * Instantiates a new disposable char array.
         *
         * @param a
         */
        protected DisposableCharArray(final char[] a) {
            N.checkArgNotNull(a, "a");
            this.a = a;
        }

        /**
         *
         *
         * @param len
         * @return
         */
        public static DisposableCharArray create(final int len) {
            return new DisposableCharArray(new char[len]);
        }

        /**
         *
         * @param a
         * @return
         */
        public static DisposableCharArray wrap(final char[] a) {
            return new DisposableCharArray(a);
        }

        /**
         *
         * @param index
         * @return
         */
        public char get(final int index) {
            return a[index];
        }

        /**
         *
         * @return
         */
        public int length() {
            return a.length;
        }

        /**
         *
         * @return
         */
        @Override
        public char[] clone() { //NOSONAR
            return N.clone(a);
        }

        /**
         *
         * @return
         */
        public Character[] box() {
            return Array.box(a);
        }

        /**
         *
         * @return
         */
        public CharList toList() {
            return CharList.of(clone());
        }

        /**
         *
         * @param <C>
         * @param supplier
         * @return
         */
        public <C extends Collection<Character>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());

            for (char e : a) {
                result.add(e);
            }

            return result;
        }

        /**
         *
         * @return
         */
        public int sum() {
            return N.sum(a);
        }

        /**
         *
         * @return
         */
        public double average() {
            return N.average(a);
        }

        /**
         *
         * @return
         */
        public char min() {
            return N.min(a);
        }

        /**
         *
         * @return
         */
        public char max() {
            return N.max(a);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void forEach(final Throwables.CharConsumer<E> action) throws E {
            for (char e : a) {
                action.accept(e);
            }
        }

        /**
         *
         *
         * @param <R>
         * @param <E>
         * @param func
         * @return
         * @throws E
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super char[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super char[], E> action) throws E {
            action.accept(a);
        }

        /**
         *
         * @param delimiter
         * @return
         */
        public String join(String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         *
         * @param delimiter
         * @param prefix
         * @param suffix
         * @return
         */
        public String join(String delimiter, String prefix, String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         *
         * @return
         */
        protected char[] values() {
            return a;
        }
    }

    /**
     * One-off Object. No caching/saving in memory, No updating. To cache/save/update the Object, call {@code clone()/copy()}.
     * <br />
     * Depends on context, it should be okay to cache/save the elements from the array, but never save or cache the array itself.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static class DisposableByteArray implements NoCachingNoUpdating, Cloneable {

        /** The a. */
        private final byte[] a;

        /**
         * Instantiates a new disposable byte array.
         *
         * @param a
         */
        protected DisposableByteArray(final byte[] a) {
            N.checkArgNotNull(a, "a");
            this.a = a;
        }

        /**
         *
         *
         * @param len
         * @return
         */
        public static DisposableByteArray create(final int len) {
            return new DisposableByteArray(new byte[len]);
        }

        /**
         *
         * @param a
         * @return
         */
        public static DisposableByteArray wrap(final byte[] a) {
            return new DisposableByteArray(a);
        }

        /**
         *
         * @param index
         * @return
         */
        public byte get(final int index) {
            return a[index];
        }

        /**
         *
         * @return
         */
        public int length() {
            return a.length;
        }

        /**
         *
         * @return
         */
        @Override
        public byte[] clone() { //NOSONAR
            return N.clone(a);
        }

        /**
         *
         * @return
         */
        public Byte[] box() {
            return Array.box(a);
        }

        /**
         *
         * @return
         */
        public ByteList toList() {
            return ByteList.of(clone());
        }

        /**
         *
         * @param <C>
         * @param supplier
         * @return
         */
        public <C extends Collection<Byte>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());

            for (byte e : a) {
                result.add(e);
            }

            return result;
        }

        /**
         *
         * @return
         */
        public int sum() {
            return N.sum(a);
        }

        /**
         *
         * @return
         */
        public double average() {
            return N.average(a);
        }

        /**
         *
         * @return
         */
        public byte min() {
            return N.min(a);
        }

        /**
         *
         * @return
         */
        public byte max() {
            return N.max(a);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void forEach(final Throwables.ByteConsumer<E> action) throws E {
            for (byte e : a) {
                action.accept(e);
            }
        }

        /**
         *
         *
         * @param <R>
         * @param <E>
         * @param func
         * @return
         * @throws E
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super byte[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super byte[], E> action) throws E {
            action.accept(a);
        }

        /**
         *
         * @param delimiter
         * @return
         */
        public String join(String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         *
         * @param delimiter
         * @param prefix
         * @param suffix
         * @return
         */
        public String join(String delimiter, String prefix, String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         *
         * @return
         */
        protected byte[] values() {
            return a;
        }
    }

    /**
     * One-off Object. No caching/saving in memory, No updating. To cache/save/update the Object, call {@code clone()/copy()}.
     * <br />
     * Depends on context, it should be okay to cache/save the elements from the array, but never save or cache the array itself.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static class DisposableShortArray implements NoCachingNoUpdating, Cloneable {

        /** The a. */
        private final short[] a;

        /**
         * Instantiates a new disposable short array.
         *
         * @param a
         */
        protected DisposableShortArray(final short[] a) {
            N.checkArgNotNull(a, "a");
            this.a = a;
        }

        /**
         *
         *
         * @param len
         * @return
         */
        public static DisposableShortArray create(final int len) {
            return new DisposableShortArray(new short[len]);
        }

        /**
         *
         * @param a
         * @return
         */
        public static DisposableShortArray wrap(final short[] a) {
            return new DisposableShortArray(a);
        }

        /**
         *
         * @param index
         * @return
         */
        public short get(final int index) {
            return a[index];
        }

        /**
         *
         * @return
         */
        public int length() {
            return a.length;
        }

        /**
         *
         * @return
         */
        @Override
        public short[] clone() { //NOSONAR
            return N.clone(a);
        }

        /**
         *
         * @return
         */
        public Short[] box() {
            return Array.box(a);
        }

        /**
         *
         * @return
         */
        public ShortList toList() {
            return ShortList.of(clone());
        }

        /**
         *
         * @param <C>
         * @param supplier
         * @return
         */
        public <C extends Collection<Short>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());

            for (short e : a) {
                result.add(e);
            }

            return result;
        }

        /**
         *
         * @return
         */
        public int sum() {
            return N.sum(a);
        }

        /**
         *
         * @return
         */
        public double average() {
            return N.average(a);
        }

        /**
         *
         * @return
         */
        public short min() {
            return N.min(a);
        }

        /**
         *
         * @return
         */
        public short max() {
            return N.max(a);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void forEach(final Throwables.ShortConsumer<E> action) throws E {
            for (short e : a) {
                action.accept(e);
            }
        }

        /**
         *
         *
         * @param <R>
         * @param <E>
         * @param func
         * @return
         * @throws E
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super short[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super short[], E> action) throws E {
            action.accept(a);
        }

        /**
         *
         * @param delimiter
         * @return
         */
        public String join(String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         *
         * @param delimiter
         * @param prefix
         * @param suffix
         * @return
         */
        public String join(String delimiter, String prefix, String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         *
         * @return
         */
        protected short[] values() {
            return a;
        }
    }

    /**
     * One-off Object. No caching/saving in memory, No updating. To cache/save/update the Object, call {@code clone()/copy()}.
     * <br />
     * Depends on context, it should be okay to cache/save the elements from the array, but never save or cache the array itself.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static class DisposableIntArray implements NoCachingNoUpdating, Cloneable {

        /** The a. */
        private final int[] a;

        /**
         * Instantiates a new disposable int array.
         *
         * @param a
         */
        protected DisposableIntArray(final int[] a) {
            N.checkArgNotNull(a, "a");
            this.a = a;
        }

        /**
         *
         *
         * @param len
         * @return
         */
        public static DisposableIntArray create(final int len) {
            return new DisposableIntArray(new int[len]);
        }

        /**
         *
         * @param a
         * @return
         */
        public static DisposableIntArray wrap(final int[] a) {
            return new DisposableIntArray(a);
        }

        /**
         *
         * @param index
         * @return
         */
        public int get(final int index) {
            return a[index];
        }

        /**
         *
         * @return
         */
        public int length() {
            return a.length;
        }

        /**
         *
         * @return
         */
        @Override
        public int[] clone() { //NOSONAR
            return N.clone(a);
        }

        /**
         *
         * @return
         */
        public Integer[] box() {
            return Array.box(a);
        }

        /**
         *
         * @return
         */
        public IntList toList() {
            return IntList.of(clone());
        }

        /**
         *
         * @param <C>
         * @param supplier
         * @return
         */
        public <C extends Collection<Integer>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());

            for (int e : a) {
                result.add(e);
            }

            return result;
        }

        /**
         *
         * @return
         */
        public int sum() {
            return N.sum(a);
        }

        /**
         *
         * @return
         */
        public double average() {
            return N.average(a);
        }

        /**
         *
         * @return
         */
        public int min() {
            return N.min(a);
        }

        /**
         *
         * @return
         */
        public int max() {
            return N.max(a);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void forEach(final Throwables.IntConsumer<E> action) throws E {
            for (int e : a) {
                action.accept(e);
            }
        }

        /**
         *
         *
         * @param <R>
         * @param <E>
         * @param func
         * @return
         * @throws E
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super int[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super int[], E> action) throws E {
            action.accept(a);
        }

        /**
         *
         * @param delimiter
         * @return
         */
        public String join(String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         *
         * @param delimiter
         * @param prefix
         * @param suffix
         * @return
         */
        public String join(String delimiter, String prefix, String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         *
         * @return
         */
        protected int[] values() {
            return a;
        }
    }

    /**
     * One-off Object. No caching/saving in memory, No updating. To cache/save/update the Object, call {@code clone()/copy()}.
     * <br />
     * Depends on context, it should be okay to cache/save the elements from the array, but never save or cache the array itself.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static class DisposableLongArray implements NoCachingNoUpdating, Cloneable {

        /** The a. */
        private final long[] a;

        /**
         * Instantiates a new disposable long array.
         *
         * @param a
         */
        protected DisposableLongArray(final long[] a) {
            N.checkArgNotNull(a, "a");
            this.a = a;
        }

        /**
         *
         *
         * @param len
         * @return
         */
        public static DisposableLongArray create(final int len) {
            return new DisposableLongArray(new long[len]);
        }

        /**
         *
         * @param a
         * @return
         */
        public static DisposableLongArray wrap(final long[] a) {
            return new DisposableLongArray(a);
        }

        /**
         *
         * @param index
         * @return
         */
        public long get(final int index) {
            return a[index];
        }

        /**
         *
         * @return
         */
        public int length() {
            return a.length;
        }

        /**
         *
         * @return
         */
        @Override
        public long[] clone() { //NOSONAR
            return N.clone(a);
        }

        /**
         *
         * @return
         */
        public Long[] box() {
            return Array.box(a);
        }

        /**
         *
         * @return
         */
        public LongList toList() {
            return LongList.of(clone());
        }

        /**
         *
         * @param <C>
         * @param supplier
         * @return
         */
        public <C extends Collection<Long>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());

            for (long e : a) {
                result.add(e);
            }

            return result;
        }

        /**
         *
         * @return
         */
        public long sum() {
            return N.sum(a);
        }

        /**
         *
         * @return
         */
        public double average() {
            return N.average(a);
        }

        /**
         *
         * @return
         */
        public long min() {
            return N.min(a);
        }

        /**
         *
         * @return
         */
        public long max() {
            return N.max(a);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void forEach(final Throwables.LongConsumer<E> action) throws E {
            for (long e : a) {
                action.accept(e);
            }
        }

        /**
         *
         *
         * @param <R>
         * @param <E>
         * @param func
         * @return
         * @throws E
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super long[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super long[], E> action) throws E {
            action.accept(a);
        }

        /**
         *
         * @param delimiter
         * @return
         */
        public String join(String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         *
         * @param delimiter
         * @param prefix
         * @param suffix
         * @return
         */
        public String join(String delimiter, String prefix, String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         *
         * @return
         */
        protected long[] values() {
            return a;
        }
    }

    /**
     * One-off Object. No caching/saving in memory, No updating. To cache/save/update the Object, call {@code clone()/copy()}.
     * <br />
     * Depends on context, it should be okay to cache/save the elements from the array, but never save or cache the array itself.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static class DisposableFloatArray implements NoCachingNoUpdating, Cloneable {

        /** The a. */
        private final float[] a;

        /**
         * Instantiates a new disposable float array.
         *
         * @param a
         */
        protected DisposableFloatArray(final float[] a) {
            N.checkArgNotNull(a, "a");
            this.a = a;
        }

        /**
         *
         *
         * @param len
         * @return
         */
        public static DisposableFloatArray create(final int len) {
            return new DisposableFloatArray(new float[len]);
        }

        /**
         *
         * @param a
         * @return
         */
        public static DisposableFloatArray wrap(final float[] a) {
            return new DisposableFloatArray(a);
        }

        /**
         *
         * @param index
         * @return
         */
        public float get(final int index) {
            return a[index];
        }

        /**
         *
         * @return
         */
        public int length() {
            return a.length;
        }

        /**
         *
         * @return
         */
        @Override
        public float[] clone() { //NOSONAR
            return N.clone(a);
        }

        /**
         *
         * @return
         */
        public Float[] box() {
            return Array.box(a);
        }

        /**
         *
         * @return
         */
        public FloatList toList() {
            return FloatList.of(clone());
        }

        /**
         *
         * @param <C>
         * @param supplier
         * @return
         */
        public <C extends Collection<Float>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());

            for (float e : a) {
                result.add(e);
            }

            return result;
        }

        /**
         *
         * @return
         */
        public float sum() {
            return N.sum(a);
        }

        /**
         *
         * @return
         */
        public double average() {
            return N.average(a);
        }

        /**
         *
         * @return
         */
        public float min() {
            return N.min(a);
        }

        /**
         *
         * @return
         */
        public float max() {
            return N.max(a);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void forEach(final Throwables.FloatConsumer<E> action) throws E {
            for (float e : a) {
                action.accept(e);
            }
        }

        /**
         *
         *
         * @param <R>
         * @param <E>
         * @param func
         * @return
         * @throws E
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super float[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super float[], E> action) throws E {
            action.accept(a);
        }

        /**
         *
         * @param delimiter
         * @return
         */
        public String join(String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         *
         * @param delimiter
         * @param prefix
         * @param suffix
         * @return
         */
        public String join(String delimiter, String prefix, String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         *
         * @return
         */
        protected float[] values() {
            return a;
        }
    }

    /**
     * One-off Object. No caching/saving in memory, No updating. To cache/save/update the Object, call {@code clone()/copy()}.
     * <br />
     * Depends on context, it should be okay to cache/save the elements from the array, but never save or cache the array itself.
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static class DisposableDoubleArray implements NoCachingNoUpdating, Cloneable {

        /** The a. */
        private final double[] a;

        /**
         * Instantiates a new disposable double array.
         *
         * @param a
         */
        protected DisposableDoubleArray(final double[] a) {
            N.checkArgNotNull(a, "a");
            this.a = a;
        }

        /**
         *
         *
         * @param len
         * @return
         */
        public static DisposableDoubleArray create(final int len) {
            return new DisposableDoubleArray(new double[len]);
        }

        /**
         *
         * @param a
         * @return
         */
        public static DisposableDoubleArray wrap(final double[] a) {
            return new DisposableDoubleArray(a);
        }

        /**
         *
         * @param index
         * @return
         */
        public double get(final int index) {
            return a[index];
        }

        /**
         *
         * @return
         */
        public int length() {
            return a.length;
        }

        /**
         *
         * @return
         */
        @Override
        public double[] clone() { //NOSONAR
            return N.clone(a);
        }

        /**
         *
         * @return
         */
        public Double[] box() {
            return Array.box(a);
        }

        /**
         *
         * @return
         */
        public DoubleList toList() {
            return DoubleList.of(clone());
        }

        /**
         *
         * @param <C>
         * @param supplier
         * @return
         */
        public <C extends Collection<Double>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(length());

            for (double e : a) {
                result.add(e);
            }

            return result;
        }

        /**
         *
         * @return
         */
        public double sum() {
            return N.sum(a);
        }

        /**
         *
         * @return
         */
        public double average() {
            return N.average(a);
        }

        /**
         *
         * @return
         */
        public double min() {
            return N.min(a);
        }

        /**
         *
         * @return
         */
        public double max() {
            return N.max(a);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void forEach(final Throwables.DoubleConsumer<E> action) throws E {
            for (double e : a) {
                action.accept(e);
            }
        }

        /**
         *
         *
         * @param <R>
         * @param <E>
         * @param func
         * @return
         * @throws E
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super double[], ? extends R, E> func) throws E {
            return func.apply(a);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super double[], E> action) throws E {
            action.accept(a);
        }

        /**
         *
         * @param delimiter
         * @return
         */
        public String join(String delimiter) {
            return Strings.join(a, delimiter);
        }

        /**
         *
         * @param delimiter
         * @param prefix
         * @param suffix
         * @return
         */
        public String join(String delimiter, String prefix, String suffix) {
            return Strings.join(a, 0, length(), delimiter, prefix, suffix);
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return N.toString(a);
        }

        /**
         *
         * @return
         */
        protected double[] values() {
            return a;
        }
    }

    /**
     * One-off Object. No caching/saving in memory, No updating. To cache/save/update the Object, call {@code clone()/copy()}.
     * <br />
     * Depends on context, it should be okay to cache/save the elements from the array, but never save or cache the {@code Queue} itself.
     *
     * @param <T>
     */
    @Beta
    @SequentialOnly
    @Stateful
    public static class DisposableDeque<T> implements NoCachingNoUpdating {

        /** The deque. */
        private final Deque<T> deque;

        /**
         * Instantiates a new disposable deque.
         *
         * @param deque
         */
        protected DisposableDeque(final Deque<T> deque) {
            N.checkArgNotNull(deque, "deque");
            this.deque = deque;
        }

        /**
         *
         *
         * @param <T>
         * @param len
         * @return
         */
        public static <T> DisposableDeque<T> create(final int len) {
            return new DisposableDeque<>(new ArrayDeque<>(len));
        }

        /**
         *
         * @param <T>
         * @param deque
         * @return
         */
        public static <T> DisposableDeque<T> wrap(final Deque<T> deque) {
            return new DisposableDeque<>(deque);
        }

        /**
         *
         * @return
         */
        public int size() {
            return deque.size();
        }

        /**
         * Gets the first.
         *
         * @return
         */
        public T getFirst() {
            return deque.getFirst();
        }

        /**
         * Gets the last.
         *
         * @return
         */
        public T getLast() {
            return deque.getLast();
        }

        /**
         *
         * @param <A>
         * @param a
         * @return
         */
        public <A> A[] toArray(A[] a) {
            return deque.toArray(a);
        }

        /**
         *
         * @return
         */
        public List<T> toList() {
            return new ArrayList<>(deque);
        }

        /**
         *
         * @return
         */
        public Set<T> toSet() {
            return new HashSet<>(deque);
        }

        /**
         *
         * @param <C>
         * @param supplier
         * @return
         */
        public <C extends Collection<T>> C toCollection(final IntFunction<? extends C> supplier) {
            final C result = supplier.apply(size());
            result.addAll(deque);
            return result;
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void forEach(final Throwables.Consumer<? super T, E> action) throws E {
            for (T e : deque) {
                action.accept(e);
            }
        }

        /**
         *
         *
         * @param <R>
         * @param <E>
         * @param func
         * @return
         * @throws E
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super Deque<T>, ? extends R, E> func) throws E {
            return func.apply(deque);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super Deque<T>, E> action) throws E {
            action.accept(deque);
        }

        /**
         *
         * @param delimiter
         * @return
         */
        public String join(String delimiter) {
            return Strings.join(deque, delimiter);
        }

        /**
         *
         * @param delimiter
         * @param prefix
         * @param suffix
         * @return
         */
        public String join(String delimiter, String prefix, String suffix) {
            return Strings.join(deque, delimiter, prefix, suffix);
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return N.toString(deque);
        }
    }

    /**
     * One-off Object. No caching/saving in memory, No updating. To cache/save/update the Object, call {@code clone()/copy()}.
     * <br />
     * Depends on context, it should be okay to cache/save the elements from the array, but never save or cache the {@code Entry} itself.
     *
     * @param <K> the key type
     * @param <V> the value type
     */
    @Beta
    @SequentialOnly
    @Stateful
    public abstract static class DisposableEntry<K, V> implements Map.Entry<K, V>, NoCachingNoUpdating {

        /**
         * 
         *
         * @param <K> the key type
         * @param <V> the value type
         * @param entry 
         * @return 
         * @throws IllegalArgumentException 
         */
        public static <K, V> DisposableEntry<K, V> wrap(final Map.Entry<K, V> entry) throws IllegalArgumentException {
            N.checkArgNotNull(entry, "entry");

            return new DisposableEntry<>() {
                private final Map.Entry<K, V> e = entry;

                @Override
                public K getKey() {
                    return e.getKey();
                }

                @Override
                public V getValue() {
                    return e.getValue();
                }
            };
        }

        /**
         * Sets the value.
         *
         * @param value
         * @return
         * @throws UnsupportedOperationException the unsupported operation exception
         * @deprecated UnsupportedOperationException
         */
        @Deprecated
        @Override
        public V setValue(V value) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         *
         *
         * @return
         */
        public Map.Entry<K, V> copy() {
            return new AbstractMap.SimpleEntry<>(getKey(), getValue());
        }

        /**
         *
         *
         * @param <R>
         * @param <E>
         * @param func
         * @return
         * @throws E
         */
        public <R, E extends Exception> R apply(final Throwables.Function<? super DisposableEntry<K, V>, ? extends R, E> func) throws E {
            return func.apply(this);
        }

        /**
         *
         *
         * @param <R>
         * @param <E>
         * @param func
         * @return
         * @throws E
         */
        public <R, E extends Exception> R apply(final Throwables.BiFunction<? super K, ? super V, ? extends R, E> func) throws E {
            return func.apply(this.getKey(), this.getValue());
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void accept(final Throwables.Consumer<? super DisposableEntry<K, V>, E> action) throws E {
            action.accept(this);
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void accept(final Throwables.BiConsumer<? super K, ? super V, E> action) throws E {
            action.accept(this.getKey(), this.getValue());
        }

        /**
         *
         * @return
         */
        @Override
        public String toString() {
            return getKey() + "=" + getValue();
        }
    }

    /**
     * One-off Object. No caching/saving in memory, No updating. To cache/save/update the Object, call {@code clone()/copy()}.
     * <br />
     * Depends on context, it should be okay to cache/save the elements from the array, but never save or cache the {@code Pair} itself.
     *
     * @param <L>
     * @param <R>
     */
    @Beta
    @SequentialOnly
    @Stateful
    public abstract static class DisposablePair<L, R> implements NoCachingNoUpdating {

        /**
         * 
         *
         * @param <L> 
         * @param <R> 
         * @param p 
         * @return 
         * @throws IllegalArgumentException 
         */
        public static <L, R> DisposablePair<L, R> wrap(final Pair<L, R> p) throws IllegalArgumentException {
            N.checkArgNotNull(p, "pair");

            return new DisposablePair<>() {
                private final Pair<L, R> pair = p;

                @Override
                public L left() {
                    return pair.left;
                }

                @Override
                public R right() {
                    return pair.right;
                }
            };
        }

        /**
         *
         *
         * @return
         */
        public abstract L left();

        /**
         *
         *
         * @return
         */
        public abstract R right();

        /**
         *
         *
         * @return
         */
        public Pair<L, R> copy() {
            return Pair.of(left(), right());
        }

        /**
         *
         *
         * @param <U>
         * @param <E>
         * @param func
         * @return
         * @throws E
         */
        public <U, E extends Exception> U apply(final Throwables.BiFunction<? super L, ? super R, ? extends U, E> func) throws E {
            return func.apply(left(), right());
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void accept(final Throwables.BiConsumer<? super L, ? super R, E> action) throws E {
            action.accept(left(), right());
        }

        /**
         *
         *
         * @return
         */
        @Override
        public String toString() {
            return "[" + N.toString(left()) + ", " + N.toString(right()) + "]";
            // return N.toString(left()) + "=" + N.toString(right());
        }
    }

    /**
     * One-off Object. No caching/saving in memory, No updating. To cache/save/update the Object, call {@code clone()/copy()}.
     * <br />
     * Depends on context, it should be okay to cache/save the elements from the array, but never save or cache the {@code Tripe} itself.
     *
     * @param <L>
     * @param <M>
     * @param <R>
     */
    @Beta
    @SequentialOnly
    @Stateful
    public abstract static class DisposableTriple<L, M, R> implements NoCachingNoUpdating {

        /**
         * 
         *
         * @param <L> 
         * @param <M> 
         * @param <R> 
         * @param p 
         * @return 
         * @throws IllegalArgumentException 
         */
        public static <L, M, R> DisposableTriple<L, M, R> wrap(final Triple<L, M, R> p) throws IllegalArgumentException {
            N.checkArgNotNull(p, "triple");

            return new DisposableTriple<>() {
                private final Triple<L, M, R> triple = p;

                @Override
                public L left() {
                    return triple.left;
                }

                @Override
                public M middle() {
                    return triple.middle;
                }

                @Override
                public R right() {
                    return triple.right;
                }
            };
        }

        /**
         *
         *
         * @return
         */
        public abstract L left();

        /**
         *
         *
         * @return
         */
        public abstract M middle();

        /**
         *
         *
         * @return
         */
        public abstract R right();

        /**
         *
         *
         * @return
         */
        public Triple<L, M, R> copy() {
            return Triple.of(left(), middle(), right());
        }

        /**
         *
         *
         * @param <U>
         * @param <E>
         * @param func
         * @return
         * @throws E
         */
        public <U, E extends Exception> U apply(final Throwables.TriFunction<? super L, ? super M, ? super R, ? extends U, E> func) throws E {
            return func.apply(left(), middle(), right());
        }

        /**
         *
         *
         * @param <E>
         * @param action
         * @throws E
         */
        public <E extends Exception> void accept(final Throwables.TriConsumer<? super L, ? super M, ? super R, E> action) throws E {
            action.accept(left(), middle(), right());
        }

        /**
         *
         *
         * @return
         */
        @Override
        public String toString() {
            return "[" + N.toString(left()) + ", " + N.toString(middle()) + ", " + N.toString(right()) + "]";
        }
    }

    public static class Timed<T> implements NoCachingNoUpdating {

        protected T value;
        protected long timeInMillis;

        protected Timed(T value, long timeInMillis) {
            this.value = value;
            this.timeInMillis = timeInMillis;
        }

        /**
         *
         * @param <T>
         * @param value
         * @param timeInMillis
         * @return
         */
        public static <T> Timed<T> of(T value, long timeInMillis) {
            return new Timed<>(value, timeInMillis);
        }

        protected void set(T value, long timeInMillis) {
            this.value = value;
            this.timeInMillis = timeInMillis;
        }

        /**
         *
         *
         * @return
         */
        public T value() {
            return value;
        }

        /**
         *
         * @return time in milliseconds.
         */
        public long timestamp() {
            return timeInMillis;
        }

        /**
         *
         *
         * @return
         */
        @Override
        public int hashCode() {
            return (int) (timeInMillis * 31 + (value == null ? 0 : value.hashCode()));
        }

        /**
         *
         * @param obj
         * @return
         */
        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }

            if (obj instanceof Timed) {
                final Timed<?> other = (Timed<?>) obj;

                return this.timeInMillis == other.timeInMillis && N.equals(this.value, other.value);
            }

            return false;
        }

        /**
         *
         *
         * @return
         */
        @Override
        public String toString() {
            return timeInMillis + ": " + N.toString(value);
        }
    }
}
