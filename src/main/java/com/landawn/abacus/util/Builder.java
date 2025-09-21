/*
 * Copyright (C) 2016 HaiYang Li
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableObjArray;
import com.landawn.abacus.util.Tuple.Tuple2;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.function.TriFunction;
import com.landawn.abacus.util.stream.Stream;

/**
 * A fluent wrapper class that provides a builder pattern for manipulating objects of type {@code T}.
 * This class allows method chaining operations on the wrapped value, providing specialized builders
 * for collections, maps, and primitive list types.
 * 
 * <p>The Builder pattern enables fluent manipulation of mutable objects by returning the builder instance
 * after each operation, allowing for method chaining. Specialized builders are provided for different
 * data structures with type-specific operations.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * List<String> result = Builder.of(new ArrayList<String>())
 *     .add("Hello")
 *     .add("World")
 *     .val();
 * }</pre>
 *
 * @param <T> The type of the value this Builder holds
 *
 * @see ImmutableList#builder()
 * @see ImmutableSet#builder()
 */
@SuppressWarnings({ "java:S6539" })
public class Builder<T> {

    final T val;

    Builder(final T val) {
        N.checkArgNotNull(val);

        this.val = val;
    }

    /**
     * Creates a specialized BooleanListBuilder for the given BooleanList.
     *
     * @param val the BooleanList to wrap in a builder
     * @return a new BooleanListBuilder instance
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}
     */
    public static BooleanListBuilder of(final BooleanList val) throws IllegalArgumentException {
        return new BooleanListBuilder(val);
    }

    /**
     * Creates a specialized CharListBuilder for the given CharList.
     *
     * @param val the CharList to wrap in a builder
     * @return a new CharListBuilder instance
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}
     */
    public static CharListBuilder of(final CharList val) throws IllegalArgumentException {
        return new CharListBuilder(val);
    }

    /**
     * Creates a specialized ByteListBuilder for the given ByteList.
     *
     * @param val the ByteList to wrap in a builder
     * @return a new ByteListBuilder instance
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}
     */
    public static ByteListBuilder of(final ByteList val) throws IllegalArgumentException {
        return new ByteListBuilder(val);
    }

    /**
     * Creates a specialized ShortListBuilder for the given ShortList.
     *
     * @param val the ShortList to wrap in a builder
     * @return a new ShortListBuilder instance
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}
     */
    public static ShortListBuilder of(final ShortList val) throws IllegalArgumentException {
        return new ShortListBuilder(val);
    }

    /**
     * Creates a specialized IntListBuilder for the given IntList.
     *
     * @param val the IntList to wrap in a builder
     * @return a new IntListBuilder instance
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}
     */
    public static IntListBuilder of(final IntList val) throws IllegalArgumentException {
        return new IntListBuilder(val);
    }

    /**
     * Creates a specialized LongListBuilder for the given LongList.
     *
     * @param val the LongList to wrap in a builder
     * @return a new LongListBuilder instance
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}
     */
    public static LongListBuilder of(final LongList val) throws IllegalArgumentException {
        return new LongListBuilder(val);
    }

    /**
     * Creates a specialized FloatListBuilder for the given FloatList.
     *
     * @param val the FloatList to wrap in a builder
     * @return a new FloatListBuilder instance
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}
     */
    public static FloatListBuilder of(final FloatList val) throws IllegalArgumentException {
        return new FloatListBuilder(val);
    }

    /**
     * Creates a specialized DoubleListBuilder for the given DoubleList.
     *
     * @param val the DoubleList to wrap in a builder
     * @return a new DoubleListBuilder instance
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}
     */
    public static DoubleListBuilder of(final DoubleList val) throws IllegalArgumentException {
        return new DoubleListBuilder(val);
    }

    /**
     * Creates a specialized ListBuilder for the given List.
     *
     * @param <T> the element type of the list
     * @param <L> the list type
     * @param val the List to wrap in a builder
     * @return a new ListBuilder instance
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}
     */
    public static <T, L extends List<T>> ListBuilder<T, L> of(final L val) throws IllegalArgumentException {
        return new ListBuilder<>(val);
    }

    /**
     * Creates a specialized CollectionBuilder for the given Collection.
     *
     * @param <T> the element type of the collection
     * @param <C> the collection type
     * @param val the Collection to wrap in a builder
     * @return a new CollectionBuilder instance
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}
     */
    public static <T, C extends Collection<T>> CollectionBuilder<T, C> of(final C val) throws IllegalArgumentException {
        return new CollectionBuilder<>(val);
    }

    /**
     * Creates a specialized MapBuilder for the given Map.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M> the map type
     * @param val the Map to wrap in a builder
     * @return a new MapBuilder instance
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}
     */
    public static <K, V, M extends Map<K, V>> MapBuilder<K, V, M> of(final M val) throws IllegalArgumentException {
        return new MapBuilder<>(val);
    }

    /**
     * Creates a specialized MultisetBuilder for the given Multiset.
     *
     * @param <T> the element type of the multiset
     * @param val the Multiset to wrap in a builder
     * @return a new MultisetBuilder instance
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}
     */
    public static <T> MultisetBuilder<T> of(final Multiset<T> val) throws IllegalArgumentException {
        return new MultisetBuilder<>(val);
    }

    /**
     * Creates a specialized MultimapBuilder for the given Multimap.
     *
     * @param <K> the key type
     * @param <E> the element type of the value collection
     * @param <V> the value collection type
     * @param <M> the multimap type
     * @param val the Multimap to wrap in a builder
     * @return a new MultimapBuilder instance
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}
     */
    public static <K, E, V extends Collection<E>, M extends Multimap<K, E, V>> MultimapBuilder<K, E, V, M> of(final M val) throws IllegalArgumentException {
        return new MultimapBuilder<>(val);
    }

    /**
     * Creates a specialized DatasetBuilder for the given Dataset.
     *
     * @param val the Dataset to wrap in a builder
     * @return a new DatasetBuilder instance
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}
     */
    public static DatasetBuilder of(final Dataset val) throws IllegalArgumentException {
        return new DatasetBuilder(val);
    }

    @SuppressWarnings("rawtypes")
    private static final Map<Class<?>, Function<Object, Builder>> creatorMap = new HashMap<>();

    static {
        initCreatorMap();
    }

    @SuppressWarnings("rawtypes")
    private static void initCreatorMap() {
        creatorMap.put(BooleanList.class, val -> Builder.of((BooleanList) val));
        creatorMap.put(CharList.class, val -> Builder.of((CharList) val));
        creatorMap.put(ByteList.class, val -> Builder.of((ByteList) val));
        creatorMap.put(ShortList.class, val -> Builder.of((ShortList) val));
        creatorMap.put(IntList.class, val -> Builder.of((IntList) val));
        creatorMap.put(LongList.class, val -> Builder.of((LongList) val));
        creatorMap.put(FloatList.class, val -> Builder.of((FloatList) val));
        creatorMap.put(DoubleList.class, val -> Builder.of((DoubleList) val));

        creatorMap.put(List.class, val -> Builder.of((List) val));
        creatorMap.put(ArrayList.class, val -> Builder.of((List) val));
        creatorMap.put(LinkedList.class, val -> Builder.of((List) val));

        creatorMap.put(Set.class, val -> Builder.of((Collection) val));
        creatorMap.put(HashSet.class, val -> Builder.of((Collection) val));
        creatorMap.put(LinkedHashSet.class, val -> Builder.of((Collection) val));

        creatorMap.put(Map.class, val -> Builder.of((Map) val));
        creatorMap.put(HashMap.class, val -> Builder.of((Map) val));
        creatorMap.put(LinkedHashMap.class, val -> Builder.of((Map) val));
        creatorMap.put(TreeMap.class, val -> Builder.of((Map) val));

        creatorMap.put(Multiset.class, val -> Builder.of((Multiset) val));

        creatorMap.put(Multimap.class, val -> Builder.of((Multimap) val));
        creatorMap.put(ListMultimap.class, val -> Builder.of((Multimap) val));
        creatorMap.put(SetMultimap.class, val -> Builder.of((Multimap) val));

        creatorMap.put(Dataset.class, val -> Builder.of((Dataset) val));
        creatorMap.put(RowDataset.class, val -> Builder.of((Dataset) val));
    }

    /**
     * Creates a Builder for the given value. This method automatically selects the most appropriate
     * specialized builder based on the runtime type of the value. If the value is a known collection
     * type (List, Set, Map, etc.), a specialized builder is returned. Otherwise, a generic Builder
     * is returned.
     *
     * @param <T> the type of the value
     * @param val the value to wrap in a builder
     * @return an appropriate Builder instance for the given value
     * @throws IllegalArgumentException if the specified {@code val} is {@code null}
     */
    @SuppressWarnings("rawtypes")
    public static <T> Builder<T> of(final T val) throws IllegalArgumentException {
        N.checkArgNotNull(val);

        final Function<Object, Builder> func = creatorMap.get(val.getClass());

        if (func != null) {
            return func.apply(val);
        }

        Builder result = null;

        if (val instanceof List) {
            result = of((List) val);
        } else if (val instanceof Multiset) {
            result = of((Multiset) val);
        } else if (val instanceof Collection) {
            result = of((Collection) val);
        } else if (val instanceof Map) {
            result = of((Map) val);
        } else if (val instanceof Multimap) {
            result = of((Multimap) val);
        } else if (val instanceof Dataset) {
            result = of((Dataset) val);
        } else {
            result = new Builder<>(val);
        }

        return result;
    }

    //    public static <T> Builder<T> get(final Supplier<T> supplier) {
    //        return new Builder<>(supplier.get());
    //    }

    /**
     * Returns the wrapped value held by this builder.
     *
     * @return the wrapped value
     */
    public T val() {
        return val;
    }

    /**
     * Transforms the wrapped value using the provided mapping function and returns a new Builder
     * containing the transformed value.
     *
     * @param <R> the type of the result of the mapping function
     * @param mapper the function to transform the wrapped value
     * @return a new Builder containing the transformed value
     * @deprecated This method is deprecated and may be removed in future versions
     */
    @Deprecated
    public <R> Builder<R> map(final Function<? super T, ? extends R> mapper) {
        return of(mapper.apply(val));
    }

    /**
     * Tests the wrapped value with the given predicate and returns an Optional containing the value
     * if the predicate returns true, otherwise returns an empty Optional.
     *
     * @param predicate the predicate to test the wrapped value
     * @return {@code Optional} with the value if {@code predicate} returns {@code true},
     * otherwise, return an empty {@code Optional}
     * @deprecated This method is deprecated and may be removed in future versions
     */
    @Deprecated
    public Optional<T> filter(final Predicate<? super T> predicate) {
        return predicate.test(val) ? Optional.of(val) : Optional.empty();
    }

    /**
     * Performs the given action on the wrapped value and returns this builder for method chaining.
     *
     * @param consumer the action to perform on the wrapped value
     * @return this builder instance
     */
    public Builder<T> accept(final Consumer<? super T> consumer) {
        consumer.accept(val);

        return this;
    }

    /**
     * Applies the given function to the wrapped value and returns the result.
     *
     * @param <R> the type of the result of the function
     * @param func the function to apply to the wrapped value
     * @return the result of applying the function to the wrapped value
     */
    public <R> R apply(final Function<? super T, ? extends R> func) {
        return func.apply(val);
    }

    /**
     * Creates a Stream containing only the wrapped value.
     *
     * @return a Stream containing the wrapped value
     */
    public Stream<T> stream() {
        return Stream.of(val);
    }

    //    /**
    //    * Returns an empty <code>Nullable</code> if {@code val()} is {@code null} while {@code targetType} is primitive or can't be assigned to {@code targetType}.
    //    * Please be aware that {@code null} can be assigned to any {@code Object} type except primitive types: {@code boolean/char/byte/short/int/long/double}.
    //    *
    //    * @param val
    //    * @param targetType
    //    * @return
    //    */
    //    @SuppressWarnings("unchecked")
    //    public <TT> Nullable<TT> castIfAssignable(final Class<TT> targetType) {
    //        if (N.isPrimitive(targetType)) {
    //            return val != null && N.wrapperOf(targetType).isAssignableFrom(val.getClass()) ? Nullable.of((TT) val) : Nullable.<TT> empty();
    //        }
    //
    //        return val == null || targetType.isAssignableFrom(val.getClass()) ? Nullable.of((TT) val) : Nullable.<TT> empty();
    //    }
    //
    //    /**
    //     *
    //     * @param b
    //     * @param actionForTrue does nothing if it's {@code null} even {@code b} is true.
    //     * @param actionForFalse does nothing if it's {@code null} even {@code b} is false.
    //     * @throws E1
    //     * @throws E2
    //     */
    //    public <E1 extends Exception, E2 extends Exception> void ifOrElse(final boolean b, final Try.Consumer<? super T, E1> actionForTrue,
    //            final Try.Consumer<? super T, E2> actionForFalse) throws E1, E2 {
    //        if (b) {
    //            if (actionForTrue != null) {
    //                actionForTrue.accept(val);
    //            }
    //        } else {
    //            if (actionForFalse != null) {
    //                actionForFalse.accept(val);
    //            }
    //        }
    //    }
    //
    //    /**
    //     *
    //     * @param predicate
    //     * @param actionForTrue does nothing if it's {@code null} even {@code b} is true.
    //     * @param actionForFalse does nothing if it's {@code null} even {@code b} is false.
    //     * @throws E0
    //     * @throws E1
    //     * @throws E2
    //     */
    //    public <E0 extends Exception, E1 extends Exception, E2 extends Exception> void ifOrElse(final Try.Predicate<? super T, E0> predicate,
    //            final Try.Consumer<? super T, E1> actionForTrue, final Try.Consumer<? super T, E2> actionForFalse) throws E0, E1, E2 {
    //        if (predicate.test(val)) {
    //            if (actionForTrue != null) {
    //                actionForTrue.accept(val);
    //            }
    //        } else {
    //            if (actionForFalse != null) {
    //                actionForFalse.accept(val);
    //            }
    //        }
    //    }
    //
    //    /**
    //     * Returns an empty {@code Optional} if {@code cmd} is executed successfully, otherwise a {@code Optional} with the exception threw.
    //     *
    //     * @param cmd
    //     * @return
    //     */
    //    public Optional<Exception> run(final Try.Consumer<? super T, ? extends Exception> cmd) {
    //        try {
    //            cmd.accept(val);
    //            return Optional.empty();
    //        } catch (Exception e) {
    //            return Optional.of(e);
    //        }
    //    }
    //
    //    /**
    //     * Returns a {@code Pair} with {@code left=returnedValue, right=null} if {@code cmd} is executed successfully, otherwise a {@code Pair} with {@code left=null, right=exception}.
    //     *
    //     * @param cmd
    //     * @return
    //     */
    //    public <R> Pair<R, Exception> call(final Try.Function<? super T, R, ? extends Exception> cmd) {
    //        try {
    //            return Pair.of(cmd.apply(val), null);
    //        } catch (Exception e) {
    //            return Pair.of(null, e);
    //        }
    //    }
    //
    //    /**
    //     * Returns a {@code Nullable} with the value returned by {@code action} or an empty {@code Nullable} if exception happens.
    //     *
    //     * @param cmd
    //     * @return
    //     */
    //    public <R> Nullable<R> tryOrEmpty(final Try.Function<? super T, R, ? extends Exception> cmd) {
    //        try {
    //            return Nullable.of(cmd.apply(val));
    //        } catch (Exception e) {
    //            return Nullable.<R> empty();
    //        }
    //    }

    /**
     * Specialized builder for BooleanList that provides fluent methods for adding, removing,
     * and modifying boolean elements.
     */
    public static final class BooleanListBuilder extends Builder<BooleanList> {

        /**
         * Instantiates a new boolean list builder.
         *
         * @param val
         */
        BooleanListBuilder(final BooleanList val) {
            super(val);
        }

        /**
         * Sets the element at the specified position in the list to the specified value.
         *
         * @param index the index of the element to replace
         * @param e the boolean value to be stored at the specified position
         * @return this builder instance
         */
        public BooleanListBuilder set(final int index, final boolean e) {
            val.set(index, e);

            return this;
        }

        /**
         * Appends the specified boolean value to the end of the list.
         *
         * @param e the boolean value to append
         * @return this builder instance
         */
        public BooleanListBuilder add(final boolean e) {
            val.add(e);

            return this;
        }

        /**
         * Inserts the specified boolean value at the specified position in the list.
         * Shifts the element currently at that position (if any) and any subsequent
         * elements to the right.
         *
         * @param index the index at which the specified element is to be inserted
         * @param e the boolean value to be inserted
         * @return this builder instance
         */
        public BooleanListBuilder add(final int index, final boolean e) {
            val.add(index, e);

            return this;
        }

        /**
         * Appends all of the elements in the specified BooleanList to the end of this list,
         * in the order that they are returned by the specified list's iterator.
         *
         * @param c the BooleanList containing elements to be added to this list
         * @return this builder instance
         */
        public BooleanListBuilder addAll(final BooleanList c) {
            val.addAll(c);

            return this;
        }

        /**
         * Inserts all of the elements in the specified BooleanList into this list,
         * starting at the specified position.
         *
         * @param index the index at which to insert the first element from the specified list
         * @param c the BooleanList containing elements to be added to this list
         * @return this builder instance
         */
        public BooleanListBuilder addAll(final int index, final BooleanList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         * Removes the first occurrence of the specified boolean value from this list, if it is present.
         *
         * @param e the boolean value to be removed from this list, if present
         * @return this builder instance
         */
        public BooleanListBuilder remove(final boolean e) {
            val.remove(e);

            return this;
        }

        //        public BooleanListBuilder removeAllOccurrences(double e) {
        //            value.removeAllOccurrences(e);
        //
        //            return this;
        //        }

        /**
         * Removes from this list all of its elements that are contained in the specified BooleanList.
         *
         * @param c the BooleanList containing elements to be removed from this list
         * @return this builder instance
         */
        public BooleanListBuilder removeAll(final BooleanList c) {
            val.removeAll(c);

            return this;
        }
    }

    /**
     * Specialized builder for CharList that provides fluent methods for adding, removing,
     * and modifying char elements.
     */
    public static final class CharListBuilder extends Builder<CharList> {

        /**
         * Instantiates a new char list builder.
         *
         * @param val
         */
        CharListBuilder(final CharList val) {
            super(val);
        }

        /**
         * Sets the element at the specified position in the list to the specified value.
         *
         * @param index the index of the element to replace
         * @param e the char value to be stored at the specified position
         * @return this builder instance
         */
        public CharListBuilder set(final int index, final char e) {
            val.set(index, e);

            return this;
        }

        /**
         * Appends the specified char value to the end of the list.
         *
         * @param e the char value to append
         * @return this builder instance
         */
        public CharListBuilder add(final char e) {
            val.add(e);

            return this;
        }

        /**
         * Inserts the specified char value at the specified position in the list.
         * Shifts the element currently at that position (if any) and any subsequent
         * elements to the right.
         *
         * @param index the index at which the specified element is to be inserted
         * @param e the char value to be inserted
         * @return this builder instance
         */
        public CharListBuilder add(final int index, final char e) {
            val.add(index, e);

            return this;
        }

        /**
         * Appends all of the elements in the specified CharList to the end of this list,
         * in the order that they are returned by the specified list's iterator.
         *
         * @param c the CharList containing elements to be added to this list
         * @return this builder instance
         */
        public CharListBuilder addAll(final CharList c) {
            val.addAll(c);

            return this;
        }

        /**
         * Inserts all of the elements in the specified CharList into this list,
         * starting at the specified position.
         *
         * @param index the index at which to insert the first element from the specified list
         * @param c the CharList containing elements to be added to this list
         * @return this builder instance
         */
        public CharListBuilder addAll(final int index, final CharList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         * Removes the first occurrence of the specified char value from this list, if it is present.
         *
         * @param e the char value to be removed from this list, if present
         * @return this builder instance
         */
        public CharListBuilder remove(final char e) {
            val.remove(e);

            return this;
        }

        //        public CharListBuilder removeAllOccurrences(double e) {
        //            value.removeAllOccurrences(e);
        //
        //            return this;
        //        }

        /**
         * Removes from this list all of its elements that are contained in the specified CharList.
         *
         * @param c the CharList containing elements to be removed from this list
         * @return this builder instance
         */
        public CharListBuilder removeAll(final CharList c) {
            val.removeAll(c);

            return this;
        }
    }

    /**
     * Specialized builder for ByteList that provides fluent methods for adding, removing,
     * and modifying byte elements.
     */
    public static final class ByteListBuilder extends Builder<ByteList> {

        /**
         * Instantiates a new byte list builder.
         *
         * @param val
         */
        ByteListBuilder(final ByteList val) {
            super(val);
        }

        /**
         * Sets the element at the specified position in the list to the specified value.
         *
         * @param index the index of the element to replace
         * @param e the byte value to be stored at the specified position
         * @return this builder instance
         */
        public ByteListBuilder set(final int index, final byte e) {
            val.set(index, e);

            return this;
        }

        /**
         * Appends the specified byte value to the end of the list.
         *
         * @param e the byte value to append
         * @return this builder instance
         */
        public ByteListBuilder add(final byte e) {
            val.add(e);

            return this;
        }

        /**
         * Inserts the specified byte value at the specified position in the list.
         * Shifts the element currently at that position (if any) and any subsequent
         * elements to the right.
         *
         * @param index the index at which the specified element is to be inserted
         * @param e the byte value to be inserted
         * @return this builder instance
         */
        public ByteListBuilder add(final int index, final byte e) {
            val.add(index, e);

            return this;
        }

        /**
         * Appends all of the elements in the specified ByteList to the end of this list,
         * in the order that they are returned by the specified list's iterator.
         *
         * @param c the ByteList containing elements to be added to this list
         * @return this builder instance
         */
        public ByteListBuilder addAll(final ByteList c) {
            val.addAll(c);

            return this;
        }

        /**
         * Inserts all of the elements in the specified ByteList into this list,
         * starting at the specified position.
         *
         * @param index the index at which to insert the first element from the specified list
         * @param c the ByteList containing elements to be added to this list
         * @return this builder instance
         */
        public ByteListBuilder addAll(final int index, final ByteList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         * Removes the first occurrence of the specified byte value from this list, if it is present.
         *
         * @param e the byte value to be removed from this list, if present
         * @return this builder instance
         */
        public ByteListBuilder remove(final byte e) {
            val.remove(e);

            return this;
        }

        //        public ByteListBuilder removeAllOccurrences(double e) {
        //            value.removeAllOccurrences(e);
        //
        //            return this;
        //        }

        /**
         * Removes from this list all of its elements that are contained in the specified ByteList.
         *
         * @param c the ByteList containing elements to be removed from this list
         * @return this builder instance
         */
        public ByteListBuilder removeAll(final ByteList c) {
            val.removeAll(c);

            return this;
        }
    }

    /**
     * Specialized builder for ShortList that provides fluent methods for adding, removing,
     * and modifying short elements.
     */
    public static final class ShortListBuilder extends Builder<ShortList> {

        /**
         * Instantiates a new short list builder.
         *
         * @param val
         */
        ShortListBuilder(final ShortList val) {
            super(val);
        }

        /**
         * Sets the element at the specified position in the list to the specified value.
         *
         * @param index the index of the element to replace
         * @param e the short value to be stored at the specified position
         * @return this builder instance
         */
        public ShortListBuilder set(final int index, final short e) {
            val.set(index, e);

            return this;
        }

        /**
         * Appends the specified short value to the end of the list.
         *
         * @param e the short value to append
         * @return this builder instance
         */
        public ShortListBuilder add(final short e) {
            val.add(e);

            return this;
        }

        /**
         * Inserts the specified short value at the specified position in the list.
         * Shifts the element currently at that position (if any) and any subsequent
         * elements to the right.
         *
         * @param index the index at which the specified element is to be inserted
         * @param e the short value to be inserted
         * @return this builder instance
         */
        public ShortListBuilder add(final int index, final short e) {
            val.add(index, e);

            return this;
        }

        /**
         * Appends all of the elements in the specified ShortList to the end of this list,
         * in the order that they are returned by the specified list's iterator.
         *
         * @param c the ShortList containing elements to be added to this list
         * @return this builder instance
         */
        public ShortListBuilder addAll(final ShortList c) {
            val.addAll(c);

            return this;
        }

        /**
         * Inserts all of the elements in the specified ShortList into this list,
         * starting at the specified position.
         *
         * @param index the index at which to insert the first element from the specified list
         * @param c the ShortList containing elements to be added to this list
         * @return this builder instance
         */
        public ShortListBuilder addAll(final int index, final ShortList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         * Removes the first occurrence of the specified short value from this list, if it is present.
         *
         * @param e the short value to be removed from this list, if present
         * @return this builder instance
         */
        public ShortListBuilder remove(final short e) {
            val.remove(e);

            return this;
        }

        //        public ShortListBuilder removeAllOccurrences(double e) {
        //            value.removeAllOccurrences(e);
        //
        //            return this;
        //        }

        /**
         * Removes from this list all of its elements that are contained in the specified ShortList.
         *
         * @param c the ShortList containing elements to be removed from this list
         * @return this builder instance
         */
        public ShortListBuilder removeAll(final ShortList c) {
            val.removeAll(c);

            return this;
        }
    }

    /**
     * Specialized builder for IntList that provides fluent methods for adding, removing,
     * and modifying int elements.
     */
    public static final class IntListBuilder extends Builder<IntList> {

        /**
         * Instantiates a new int list builder.
         *
         * @param val
         */
        IntListBuilder(final IntList val) {
            super(val);
        }

        /**
         * Sets the element at the specified position in the list to the specified value.
         *
         * @param index the index of the element to replace
         * @param e the int value to be stored at the specified position
         * @return this builder instance
         */
        public IntListBuilder set(final int index, final int e) {
            val.set(index, e);

            return this;
        }

        /**
         * Appends the specified int value to the end of the list.
         *
         * @param e the int value to append
         * @return this builder instance
         */
        public IntListBuilder add(final int e) {
            val.add(e);

            return this;
        }

        /**
         * Inserts the specified int value at the specified position in the list.
         * Shifts the element currently at that position (if any) and any subsequent
         * elements to the right.
         *
         * @param index the index at which the specified element is to be inserted
         * @param e the int value to be inserted
         * @return this builder instance
         */
        public IntListBuilder add(final int index, final int e) {
            val.add(index, e);

            return this;
        }

        /**
         * Appends all of the elements in the specified IntList to the end of this list,
         * in the order that they are returned by the specified list's iterator.
         *
         * @param c the IntList containing elements to be added to this list
         * @return this builder instance
         */
        public IntListBuilder addAll(final IntList c) {
            val.addAll(c);

            return this;
        }

        /**
         * Inserts all of the elements in the specified IntList into this list,
         * starting at the specified position.
         *
         * @param index the index at which to insert the first element from the specified list
         * @param c the IntList containing elements to be added to this list
         * @return this builder instance
         */
        public IntListBuilder addAll(final int index, final IntList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         * Removes the first occurrence of the specified int value from this list, if it is present.
         *
         * @param e the int value to be removed from this list, if present
         * @return this builder instance
         */
        public IntListBuilder remove(final int e) {
            val.remove(e);

            return this;
        }

        //        public IntListBuilder removeAllOccurrences(double e) {
        //            value.removeAllOccurrences(e);
        //
        //            return this;
        //        }

        /**
         * Removes from this list all of its elements that are contained in the specified IntList.
         *
         * @param c the IntList containing elements to be removed from this list
         * @return this builder instance
         */
        public IntListBuilder removeAll(final IntList c) {
            val.removeAll(c);

            return this;
        }
    }

    /**
     * Specialized builder for LongList that provides fluent methods for adding, removing,
     * and modifying long elements.
     */
    public static final class LongListBuilder extends Builder<LongList> {

        /**
         * Instantiates a new long list builder.
         *
         * @param val
         */
        LongListBuilder(final LongList val) {
            super(val);
        }

        /**
         * Sets the element at the specified position in the list to the specified value.
         *
         * @param index the index of the element to replace
         * @param e the long value to be stored at the specified position
         * @return this builder instance
         */
        public LongListBuilder set(final int index, final long e) {
            val.set(index, e);

            return this;
        }

        /**
         * Appends the specified long value to the end of the list.
         *
         * @param e the long value to append
         * @return this builder instance
         */
        public LongListBuilder add(final long e) {
            val.add(e);

            return this;
        }

        /**
         * Inserts the specified long value at the specified position in the list.
         * Shifts the element currently at that position (if any) and any subsequent
         * elements to the right.
         *
         * @param index the index at which the specified element is to be inserted
         * @param e the long value to be inserted
         * @return this builder instance
         */
        public LongListBuilder add(final int index, final long e) {
            val.add(index, e);

            return this;
        }

        /**
         * Appends all of the elements in the specified LongList to the end of this list,
         * in the order that they are returned by the specified list's iterator.
         *
         * @param c the LongList containing elements to be added to this list
         * @return this builder instance
         */
        public LongListBuilder addAll(final LongList c) {
            val.addAll(c);

            return this;
        }

        /**
         * Inserts all of the elements in the specified LongList into this list,
         * starting at the specified position.
         *
         * @param index the index at which to insert the first element from the specified list
         * @param c the LongList containing elements to be added to this list
         * @return this builder instance
         */
        public LongListBuilder addAll(final int index, final LongList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         * Removes the first occurrence of the specified long value from this list, if it is present.
         *
         * @param e the long value to be removed from this list, if present
         * @return this builder instance
         */
        public LongListBuilder remove(final long e) {
            val.remove(e);

            return this;
        }

        //        public LongListBuilder removeAllOccurrences(double e) {
        //            value.removeAllOccurrences(e);
        //
        //            return this;
        //        }

        /**
         * Removes from this list all of its elements that are contained in the specified LongList.
         *
         * @param c the LongList containing elements to be removed from this list
         * @return this builder instance
         */
        public LongListBuilder removeAll(final LongList c) {
            val.removeAll(c);

            return this;
        }
    }

    /**
     * Specialized builder for FloatList that provides fluent methods for adding, removing,
     * and modifying float elements.
     */
    public static final class FloatListBuilder extends Builder<FloatList> {

        /**
         * Instantiates a new float list builder.
         *
         * @param val
         */
        FloatListBuilder(final FloatList val) {
            super(val);
        }

        /**
         * Sets the element at the specified position in the list to the specified value.
         *
         * @param index the index of the element to replace
         * @param e the float value to be stored at the specified position
         * @return this builder instance
         */
        public FloatListBuilder set(final int index, final float e) {
            val.set(index, e);

            return this;
        }

        /**
         * Appends the specified float value to the end of the list.
         *
         * @param e the float value to append
         * @return this builder instance
         */
        public FloatListBuilder add(final float e) {
            val.add(e);

            return this;
        }

        /**
         * Inserts the specified float value at the specified position in the list.
         * Shifts the element currently at that position (if any) and any subsequent
         * elements to the right.
         *
         * @param index the index at which the specified element is to be inserted
         * @param e the float value to be inserted
         * @return this builder instance
         */
        public FloatListBuilder add(final int index, final float e) {
            val.add(index, e);

            return this;
        }

        /**
         * Appends all of the elements in the specified FloatList to the end of this list,
         * in the order that they are returned by the specified list's iterator.
         *
         * @param c the FloatList containing elements to be added to this list
         * @return this builder instance
         */
        public FloatListBuilder addAll(final FloatList c) {
            val.addAll(c);

            return this;
        }

        /**
         * Inserts all of the elements in the specified FloatList into this list,
         * starting at the specified position.
         *
         * @param index the index at which to insert the first element from the specified list
         * @param c the FloatList containing elements to be added to this list
         * @return this builder instance
         */
        public FloatListBuilder addAll(final int index, final FloatList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         * Removes the first occurrence of the specified float value from this list, if it is present.
         *
         * @param e the float value to be removed from this list, if present
         * @return this builder instance
         */
        public FloatListBuilder remove(final float e) {
            val.remove(e);

            return this;
        }

        //        public FloatListBuilder removeAllOccurrences(double e) {
        //            value.removeAllOccurrences(e);
        //
        //            return this;
        //        }

        /**
         * Removes from this list all of its elements that are contained in the specified FloatList.
         *
         * @param c the FloatList containing elements to be removed from this list
         * @return this builder instance
         */
        public FloatListBuilder removeAll(final FloatList c) {
            val.removeAll(c);

            return this;
        }
    }

    /**
     * Specialized builder for DoubleList that provides fluent methods for adding, removing,
     * and modifying double elements.
     */
    public static final class DoubleListBuilder extends Builder<DoubleList> {

        /**
         * Instantiates a new double list builder.
         *
         * @param val
         */
        DoubleListBuilder(final DoubleList val) {
            super(val);
        }

        /**
         * Sets the element at the specified position in the list to the specified value.
         *
         * @param index the index of the element to replace
         * @param e the double value to be stored at the specified position
         * @return this builder instance
         */
        public DoubleListBuilder set(final int index, final double e) {
            val.set(index, e);

            return this;
        }

        /**
         * Appends the specified double value to the end of the list.
         *
         * @param e the double value to append
         * @return this builder instance
         */
        public DoubleListBuilder add(final double e) {
            val.add(e);

            return this;
        }

        /**
         * Inserts the specified double value at the specified position in the list.
         * Shifts the element currently at that position (if any) and any subsequent
         * elements to the right.
         *
         * @param index the index at which the specified element is to be inserted
         * @param e the double value to be inserted
         * @return this builder instance
         */
        public DoubleListBuilder add(final int index, final double e) {
            val.add(index, e);

            return this;
        }

        /**
         * Appends all of the elements in the specified DoubleList to the end of this list,
         * in the order that they are returned by the specified list's iterator.
         *
         * @param c the DoubleList containing elements to be added to this list
         * @return this builder instance
         */
        public DoubleListBuilder addAll(final DoubleList c) {
            val.addAll(c);

            return this;
        }

        /**
         * Inserts all of the elements in the specified DoubleList into this list,
         * starting at the specified position.
         *
         * @param index the index at which to insert the first element from the specified list
         * @param c the DoubleList containing elements to be added to this list
         * @return this builder instance
         */
        public DoubleListBuilder addAll(final int index, final DoubleList c) {
            val.addAll(index, c);

            return this;
        }

        /**
         * Removes the first occurrence of the specified double value from this list, if it is present.
         *
         * @param e the double value to be removed from this list, if present
         * @return this builder instance
         */
        public DoubleListBuilder remove(final double e) {
            val.remove(e);

            return this;
        }

        //        public DoubleListBuilder removeAllOccurrences(double e) {
        //            value.removeAllOccurrences(e);
        //
        //            return this;
        //        }

        /**
         * Removes from this list all of its elements that are contained in the specified DoubleList.
         *
         * @param c the DoubleList containing elements to be removed from this list
         * @return this builder instance
         */
        public DoubleListBuilder removeAll(final DoubleList c) {
            val.removeAll(c);

            return this;
        }
    }

    /**
     * Specialized builder for List that provides fluent methods for adding, removing,
     * and modifying elements. This builder extends CollectionBuilder with List-specific
     * operations such as indexed access.
     *
     * @param <T> the element type
     * @param <L> the list type
     */
    public static final class ListBuilder<T, L extends List<T>> extends CollectionBuilder<T, L> {

        /**
         * Instantiates a new list builder.
         *
         * @param c
         */
        ListBuilder(final L c) {
            super(c);
        }

        @Override
        public ListBuilder<T, L> add(final T e) {
            val.add(e);

            return this;
        }

        @Override
        public ListBuilder<T, L> addAll(final Collection<? extends T> c) {
            if (N.notEmpty(c)) {
                val.addAll(c);
            }

            return this;
        }

        @SafeVarargs
        @Override
        public final ListBuilder<T, L> addAll(final T... a) {
            if (N.notEmpty(a)) {
                val.addAll(Arrays.asList(a));
            }

            return this;
        }

        @Override
        public ListBuilder<T, L> remove(final Object e) {
            //noinspection SuspiciousMethodCalls
            val.remove(e);

            return this;
        }

        @Override
        public ListBuilder<T, L> removeAll(final Collection<?> c) {
            if (N.notEmpty(c)) {
                //noinspection SuspiciousMethodCalls
                val.removeAll(c);
            }

            return this;
        }

        @SafeVarargs
        @Override
        public final ListBuilder<T, L> removeAll(final T... a) {
            if (N.notEmpty(a)) {
                val.removeAll(Arrays.asList(a));
            }

            return this;
        }

        /**
         * Inserts the specified element at the specified position in this list.
         * Shifts the element currently at that position (if any) and any subsequent
         * elements to the right.
         *
         * @param index the index at which the specified element is to be inserted
         * @param e the element to be inserted
         * @return this builder instance
         */
        public ListBuilder<T, L> add(final int index, final T e) {
            val.add(index, e);

            return this;
        }

        /**
         * Inserts all of the elements in the specified collection into this list,
         * starting at the specified position. Shifts the element currently at that
         * position (if any) and any subsequent elements to the right.
         *
         * @param index the index at which to insert the first element from the specified collection
         * @param c the collection containing elements to be added to this list
         * @return this builder instance
         * @throws IndexOutOfBoundsException if the index is out of range
         */
        public ListBuilder<T, L> addAll(final int index, final Collection<? extends T> c) throws IndexOutOfBoundsException {
            N.checkElementIndex(index, val.size());

            if (N.notEmpty(c)) {
                val.addAll(index, c);
            }

            return this;
        }

        /**
         * Removes the element at the specified position in this list.
         * Shifts any subsequent elements to the left.
         *
         * @param index the index of the element to be removed
         * @return this builder instance
         */
        public ListBuilder<T, L> remove(final int index) {
            val.remove(index);

            return this;
        }
    }

    /**
     * Builder for Collection that provides fluent methods for adding and removing elements.
     * This is the base builder for collection types and is extended by ListBuilder.
     *
     * @param <T> the element type
     * @param <C> the collection type
     */
    public static sealed class CollectionBuilder<T, C extends Collection<T>> extends Builder<C> permits ListBuilder {

        /**
         * Instantiates a new collection builder.
         *
         * @param c
         */
        CollectionBuilder(final C c) {
            super(c);
        }

        /**
         * Adds the specified element to this collection.
         *
         * @param e the element to add
         * @return this builder instance
         */
        public CollectionBuilder<T, C> add(final T e) {
            val.add(e);

            return this;
        }

        /**
         * Adds all of the elements in the specified collection to this collection.
         * The behavior of this operation is undefined if the specified collection
         * is modified while the operation is in progress.
         *
         * @param c the collection containing elements to be added to this collection
         * @return this builder instance
         */
        public CollectionBuilder<T, C> addAll(final Collection<? extends T> c) {
            if (N.notEmpty(c)) {
                val.addAll(c);
            }

            return this;
        }

        /**
         * Adds all of the elements in the specified array to this collection.
         *
         * @param a the array containing elements to be added to this collection
         * @return this builder instance
         */
        public CollectionBuilder<T, C> addAll(final T... a) {
            if (N.notEmpty(a)) {
                val.addAll(Arrays.asList(a));
            }

            return this;
        }

        /**
         * Removes a single instance of the specified element from this collection,
         * if it is present.
         *
         * @param e the element to be removed from this collection, if present
         * @return this builder instance
         */
        public CollectionBuilder<T, C> remove(final Object e) {
            //noinspection SuspiciousMethodCalls
            val.remove(e);

            return this;
        }

        /**
         * Removes all of this collection's elements that are also contained in the
         * specified collection. After this call returns, this collection will contain
         * no elements in common with the specified collection.
         *
         * @param c the collection containing elements to be removed from this collection
         * @return this builder instance
         */
        public CollectionBuilder<T, C> removeAll(final Collection<?> c) {
            if (N.notEmpty(c)) {
                //noinspection SuspiciousMethodCalls
                val.removeAll(c);
            }

            return this;
        }

        /**
         * Removes all of this collection's elements that are also contained in the
         * specified array.
         *
         * @param a the array containing elements to be removed from this collection
         * @return this builder instance
         */
        public CollectionBuilder<T, C> removeAll(final T... a) {
            if (N.notEmpty(a)) {
                val.removeAll(Arrays.asList(a));
            }

            return this;
        }
    }

    /**
     * Specialized builder for Multiset that provides fluent methods for managing element occurrences.
     * A Multiset is a collection that allows duplicate elements and tracks the count of each element.
     *
     * @param <T> the element type
     * @see Multiset
     */
    public static final class MultisetBuilder<T> extends Builder<Multiset<T>> {

        /**
         * Instantiates a new multiset builder.
         *
         * @param c
         */
        MultisetBuilder(final Multiset<T> c) {
            super(c);
        }

        /**
         * Sets the count of the specified element to the specified non-negative value.
         * If the element is not already in the multiset and occurrences is positive,
         * it will be added. If occurrences is zero, the element will be removed entirely.
         *
         * @param e the element whose count is to be set
         * @param occurrences the desired count of the element; must be non-negative
         * @return this builder instance
         */
        public MultisetBuilder<T> setCount(final T e, final int occurrences) {
            val.setCount(e, occurrences);

            return this;
        }

        /**
         * Adds a single occurrence of the specified element to this multiset.
         *
         * @param e the element to add
         * @return this builder instance
         */
        public MultisetBuilder<T> add(final T e) {
            val.add(e);

            return this;
        }

        /**
         * Adds the specified number of occurrences of the specified element to this multiset.
         *
         * @param e the element to add
         * @param occurrencesToAdd the number of occurrences to add; may be negative to remove occurrences
         * @return this builder instance
         */
        public MultisetBuilder<T> add(final T e, final int occurrencesToAdd) {
            val.add(e, occurrencesToAdd);

            return this;
        }

        /**
         * Removes a single occurrence of the specified element from this multiset, if present.
         *
         * @param e the element to remove one occurrence of
         * @return this builder instance
         */
        public MultisetBuilder<T> remove(final Object e) {
            //noinspection SuspiciousMethodCalls
            val.remove(e);

            return this;
        }

        /**
         * Removes the specified number of occurrences of the specified element from this multiset.
         * If the multiset contains fewer than this number of occurrences, all occurrences
         * will be removed.
         *
         * @param e the element to remove occurrences of
         * @param occurrencesToAdd the number of occurrences to remove
         * @return this builder instance
         */
        public MultisetBuilder<T> remove(final Object e, final int occurrencesToAdd) {
            val.remove(e, occurrencesToAdd);

            return this;
        }

        /**
         * Removes all occurrences of all elements in the specified collection from this multiset.
         *
         * @param c the collection of elements to remove all occurrences of
         * @return this builder instance
         * @see #removeAllOccurrences(Collection)
         * @deprecated Use {@link #removeAllOccurrences(Collection<?>)} instead
         */
        @Deprecated
        public MultisetBuilder<T> removeAll(final Collection<?> c) {
            return removeAllOccurrences(c);
        }

        /**
         * Removes all occurrences of the specified element from this multiset.
         *
         * @param e the element to remove all occurrences of
         * @return this builder instance
         */
        public MultisetBuilder<T> removeAllOccurrences(final Object e) {
            val.removeAllOccurrences(e);

            return this;
        }

        /**
         * Removes all occurrences of all elements in the specified collection from this multiset.
         *
         * @param c the collection of elements to remove all occurrences of
         * @return this builder instance
         */
        public MultisetBuilder<T> removeAllOccurrences(final Collection<?> c) {
            val.removeAllOccurrences(c);

            return this;
        }
    }

    /**
     * Specialized builder for Map that provides fluent methods for adding and removing key-value pairs.
     *
     * @param <K> the key type
     * @param <V> the value type
     * @param <M> the map type
     */
    public static final class MapBuilder<K, V, M extends Map<K, V>> extends Builder<M> {

        /**
         * Instantiates a new map builder.
         *
         * @param m
         */
        MapBuilder(final M m) {
            super(m);
        }

        /**
         * Associates the specified value with the specified key in this map.
         * If the map previously contained a mapping for the key, the old value is replaced.
         *
         * @param k the key with which the specified value is to be associated
         * @param v the value to be associated with the specified key
         * @return this builder instance
         */
        public MapBuilder<K, V, M> put(final K k, final V v) {
            val.put(k, v);

            return this;
        }

        /**
         * Copies all of the mappings from the specified map to this map.
         * The effect of this call is equivalent to that of calling put(k, v)
         * on this map once for each mapping from key k to value v in the specified map.
         *
         * @param m the mappings to be stored in this map
         * @return this builder instance
         */
        public MapBuilder<K, V, M> putAll(final Map<? extends K, ? extends V> m) {
            if (N.notEmpty(m)) {
                val.putAll(m);
            }

            return this;
        }

        /**
         * Associates the specified value with the specified key in this map if the key
         * is not already associated with a value or is associated with null.
         *
         * <br />
         * Absent -> key is not found in the specified map or found with {@code null} value.
         *
         * @param key the key with which the specified value is to be associated
         * @param value the value to be associated with the specified key
         * @return this builder instance
         * @see Map#putIfAbsent(Object, Object)
         */
        public MapBuilder<K, V, M> putIfAbsent(final K key, final V value) {

            // if (v == null && val.containsKey(key) == false) {
            val.putIfAbsent(key, value);

            return this;
        }

        /**
         * Associates the value produced by the supplier with the specified key in this map
         * if the key is not already associated with a value or is associated with null.
         * The supplier is only invoked if the value needs to be added.
         *
         * <br />
         * Absent -> key is not found in the specified map or found with {@code null} value.
         *
         * @param key the key with which the specified value is to be associated
         * @param supplier the supplier that produces the value to be associated with the specified key
         * @return this builder instance
         * @see Map#putIfAbsent(Object, Object)
         */
        public MapBuilder<K, V, M> putIfAbsent(final K key, final Supplier<V> supplier) {
            final V v = val.get(key);

            // if (v == null && val.containsKey(key) == false) {
            if (v == null) {
                val.put(key, supplier.get());
            }

            return this;
        }

        /**
         * Removes the mapping for a key from this map if it is present.
         *
         * @param k the key whose mapping is to be removed from the map
         * @return this builder instance
         */
        public MapBuilder<K, V, M> remove(final Object k) {
            //noinspection SuspiciousMethodCalls
            val.remove(k);

            return this;
        }

        /**
         * Removes all mappings for the specified keys from this map if they are present.
         * <p>
         * This is equivalent to calling {@link Map#remove(Object)} for each key in the
         * provided collection, but with the added convenience of method chaining.
         * If the collection is empty or null, no changes are made to the map.
         *
         * @param keysToRemove the collection containing keys to be removed from the map
         * @return this builder instance for method chaining
         * @see #remove(Object)
         * @see Map#remove(Object) 
         */
        public MapBuilder<K, V, M> removeAll(final Collection<?> keysToRemove) {
            if (N.notEmpty(keysToRemove)) {
                for (final Object k : keysToRemove) {
                    //noinspection SuspiciousMethodCalls
                    val.remove(k);
                }
            }

            return this;
        }
    }

    /**
     * Builder for {@link Multimap} that provides fluent methods for adding, removing, and manipulating
     * key-value mappings in a multimap structure.
     *
     * @param <K> the key type
     * @param <E> the element type stored in the collection values
     * @param <V> the collection type that holds the values (e.g., List, Set)
     * @param <M> the specific Multimap implementation type
     * @see Multimap
     * @see ListMultimap
     * @see SetMultimap
     */
    public static final class MultimapBuilder<K, E, V extends Collection<E>, M extends Multimap<K, E, V>> extends Builder<M> {

        /**
         * Instantiates a new multimap builder.
         *
         * @param m the multimap instance to wrap
         */
        MultimapBuilder(final M m) {
            super(m);
        }

        /**
         * Adds a single key-value mapping to the multimap.
         * If the key already exists, the value is added to the collection associated with that key.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * multimapBuilder.put("fruits", "apple")
         *                .put("fruits", "orange");
         * }</pre>
         *
         * @param key the key to map
         * @param e the value to add to the collection for this key
         * @return this builder instance for method chaining
         */
        public MultimapBuilder<K, E, V, M> put(final K key, final E e) {
            val.put(key, e);

            return this;
        }

        /**
         * Adds all key-value mappings from the specified map to the multimap.
         * Each entry in the map results in a single value being added to the multimap.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, String> map = Map.of("color", "red", "size", "large");
         * multimapBuilder.put(map);
         * }</pre>
         *
         * @param m the map containing key-value pairs to add
         * @return this builder instance for method chaining
         */
        public MultimapBuilder<K, E, V, M> put(final Map<? extends K, ? extends E> m) {
            val.put(m);

            return this;
        }

        /**
         * Adds multiple values for a single key to the multimap.
         * All values in the collection are associated with the specified key.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * multimapBuilder.putMany("colors", Arrays.asList("red", "green", "blue"));
         * }</pre>
         *
         * @param k the key to map
         * @param c the collection of values to add for this key
         * @return this builder instance for method chaining
         */
        public MultimapBuilder<K, E, V, M> putMany(final K k, final Collection<? extends E> c) {
            val.putMany(k, c);

            return this;
        }

        /**
         * Adds multiple key-collection mappings from the specified map to the multimap.
         * Each key is mapped to all values in its corresponding collection.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, List<String>> map = Map.of(
         *     "fruits", Arrays.asList("apple", "orange"),
         *     "colors", Arrays.asList("red", "blue")
         * );
         * multimapBuilder.putMany(map);
         * }</pre>
         *
         * @param m the map containing key-collection pairs to add
         * @return this builder instance for method chaining
         */
        public MultimapBuilder<K, E, V, M> putMany(final Map<? extends K, ? extends Collection<? extends E>> m) {
            val.putMany(m);

            return this;
        }

        /**
         * Adds all key-value mappings from another multimap to this multimap.
         * All associations from the source multimap are copied.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Multimap<String, String> source = // ... another multimap
         * multimapBuilder.putMany(source);
         * }</pre>
         *
         * @param m the multimap whose mappings should be added
         * @return this builder instance for method chaining
         */
        public MultimapBuilder<K, E, V, M> putMany(final Multimap<? extends K, ? extends E, ? extends V> m) {
            val.putMany(m);

            return this;
        }

        /**
         * Removes a single occurrence of the specified key-value mapping from the multimap.
         * If the key-value pair exists multiple times, only one occurrence is removed.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * multimapBuilder.removeOne("fruits", "apple");
         * }</pre>
         *
         * @param k the key of the mapping to remove
         * @param e the value to remove from the key's collection
         * @return this builder instance for method chaining
         */
        public MultimapBuilder<K, E, V, M> removeOne(final Object k, final Object e) {
            val.removeOne(k, e);

            return this;
        }

        /**
         * Removes single occurrences of all key-value mappings specified in the map.
         * For each entry, one occurrence of the key-value pair is removed if it exists.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, String> toRemove = Map.of("fruits", "apple", "colors", "red");
         * multimapBuilder.removeOne(toRemove);
         * }</pre>
         *
         * @param m the map containing key-value pairs to remove
         * @return this builder instance for method chaining
         */
        public MultimapBuilder<K, E, V, M> removeOne(final Map<? extends K, ? extends E> m) {
            val.removeOne(m);

            return this;
        }

        /**
         * Removes all values associated with the specified key from the multimap.
         * After this operation, the key will no longer be present in the multimap.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * multimapBuilder.removeAll("fruits");
         * }</pre>
         *
         * @param k the key whose mappings should be removed
         * @return this builder instance for method chaining
         */
        public MultimapBuilder<K, E, V, M> removeAll(final Object k) {
            val.removeAll(k);

            return this;
        }

        /**
         * Removes multiple specific values associated with a key from the multimap.
         * Only the specified values are removed; other values for the key remain.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * multimapBuilder.removeMany("fruits", Arrays.asList("apple", "orange"));
         * }</pre>
         *
         * @param k the key whose values should be partially removed
         * @param valuesToRemove the collection of values to remove for this key
         * @return this builder instance for method chaining
         */
        public MultimapBuilder<K, E, V, M> removeMany(final Object k, final Collection<?> valuesToRemove) {
            val.removeMany(k, valuesToRemove);

            return this;
        }

        /**
         * Removes multiple values for multiple keys as specified in the map.
         * For each key in the map, the associated collection of values is removed.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, List<String>> toRemove = Map.of(
         *     "fruits", Arrays.asList("apple", "orange"),
         *     "colors", Arrays.asList("red")
         * );
         * multimapBuilder.removeMany(toRemove);
         * }</pre>
         *
         * @param m the map containing key-collection pairs to remove
         * @return this builder instance for method chaining
         */
        public MultimapBuilder<K, E, V, M> removeMany(final Map<?, ? extends Collection<?>> m) {
            val.removeMany(m);

            return this;
        }

        /**
         * Removes all key-value mappings that exist in the specified multimap from this multimap.
         * Each key-value pair in the source multimap is removed from this multimap if present.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Multimap<String, String> toRemove = // ... another multimap
         * multimapBuilder.removeMany(toRemove);
         * }</pre>
         *
         * @param m the multimap containing mappings to remove
         * @return this builder instance for method chaining
         */
        public MultimapBuilder<K, E, V, M> removeMany(final Multimap<?, ?, ?> m) {
            val.removeMany(m);

            return this;
        }
    }

    /**
     * Builder for {@link Dataset} that provides fluent methods for data manipulation operations
     * such as renaming columns, adding/removing columns, transforming data, and combining datasets.
     * 
     * @see Dataset
     */
    public static final class DatasetBuilder extends Builder<Dataset> {

        /**
         * Instantiates a new data set builder.
         *
         * @param ds the Dataset instance to wrap
         */
        DatasetBuilder(final Dataset ds) {
            super(ds);
        }

        /**
         * Renames a single column in the dataset.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.renameColumn("old_name", "new_name");
         * }</pre>
         *
         * @param columnName the current name of the column
         * @param newColumnName the new name for the column
         * @return this builder instance for method chaining
         */
        public DatasetBuilder renameColumn(final String columnName, final String newColumnName) {
            val.renameColumn(columnName, newColumnName);

            return this;
        }

        /**
         * Renames multiple columns in the dataset using a map of old names to new names.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, String> renames = Map.of(
         *     "old_col1", "new_col1",
         *     "old_col2", "new_col2"
         * );
         * datasetBuilder.renameColumns(renames);
         * }</pre>
         *
         * @param oldNewNames a map where keys are current column names and values are new names
         * @return this builder instance for method chaining
         */
        public DatasetBuilder renameColumns(final Map<String, String> oldNewNames) {
            val.renameColumns(oldNewNames);

            return this;
        }

        //        /**
        //         *
        //         * @param columnName
        //         * @param func
        //         * @return
        //         */
        //        public DatasetBuilder renameColumn(String columnName, Function<? super String, String> func) {
        //            val.renameColumn(columnName, func);
        //
        //            return this;
        //        }

        /**
         * Renames specified columns using a function that transforms column names.
         * The function is applied to each column name in the collection.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.renameColumns(Arrays.asList("col1", "col2"), 
         *                             name -> name.toUpperCase());
         * }</pre>
         *
         * @param columnNames the collection of column names to rename
         * @param func the function that transforms old names to new names
         * @return this builder instance for method chaining
         */
        public DatasetBuilder renameColumns(final Collection<String> columnNames, final Function<? super String, String> func) {
            val.renameColumns(columnNames, func);

            return this;
        }

        /**
         * Renames all columns in the dataset using a function that transforms column names.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.renameColumns(name -> "prefix_" + name);
         * }</pre>
         *
         * @param func the function that transforms old names to new names
         * @return this builder instance for method chaining
         */
        public DatasetBuilder renameColumns(final Function<? super String, String> func) {
            val.renameColumns(func);

            return this;
        }

        /**
         * Adds a new column to the dataset at the end with the specified name and values.
         * The size of the column list must match the number of rows in the dataset.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.addColumn("age", Arrays.asList(25, 30, 35));
         * }</pre>
         *
         * @param columnName the name of the new column
         * @param column the list of values for the new column
         * @return this builder instance for method chaining
         */
        public DatasetBuilder addColumn(final String columnName, final List<?> column) {
            val.addColumn(columnName, column);

            return this;
        }

        /**
         * Adds a new column to the dataset at the specified position with the given name and values.
         * Existing columns at or after the index are shifted to the right.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.addColumn(1, "age", Arrays.asList(25, 30, 35));
         * }</pre>
         *
         * @param columnIndex the position where the column should be inserted (0-based)
         * @param columnName the name of the new column
         * @param column the list of values for the new column
         * @return this builder instance for method chaining
         */
        public DatasetBuilder addColumn(final int columnIndex, final String columnName, final List<?> column) {
            val.addColumn(columnIndex, columnName, column);

            return this;
        }

        /**
         * Adds a new column by applying a function to values from an existing column.
         * The function transforms each value in the source column to produce the new column.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.addColumn("age_squared", "age", 
         *                         (Integer age) -> age * age);
         * }</pre>
         *
         * @param newColumnName the name of the new column
         * @param fromColumnName the name of the source column
         * @param func the function to transform values from the source column
         * @return this builder instance for method chaining
         */
        public DatasetBuilder addColumn(final String newColumnName, final String fromColumnName, final Function<?, ?> func) {
            val.addColumn(newColumnName, fromColumnName, func);

            return this;
        }

        /**
         * Adds a new column at a specific position by applying a function to values from an existing column.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.addColumn(2, "age_squared", "age", 
         *                         (Integer age) -> age * age);
         * }</pre>
         *
         * @param columnIndex the position where the column should be inserted (0-based)
         * @param newColumnName the name of the new column
         * @param fromColumnName the name of the source column
         * @param func the function to transform values from the source column
         * @return this builder instance for method chaining
         */
        public DatasetBuilder addColumn(final int columnIndex, final String newColumnName, final String fromColumnName, final Function<?, ?> func) {
            val.addColumn(columnIndex, newColumnName, fromColumnName, func);

            return this;
        }

        /**
         * Adds a new column by applying a function to values from multiple existing columns.
         * The function receives an array of values from the specified columns for each row.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.addColumn("full_name", Arrays.asList("first", "last"),
         *     arr -> arr.get(0) + " " + arr.get(1));
         * }</pre>
         *
         * @param newColumnName the name of the new column
         * @param fromColumnNames the names of the source columns
         * @param func the function that combines values from source columns
         * @return this builder instance for method chaining
         */
        public DatasetBuilder addColumn(final String newColumnName, final Collection<String> fromColumnNames,
                final Function<? super DisposableObjArray, ?> func) {
            val.addColumn(newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Adds a new column at a specific position by applying a function to values from multiple columns.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.addColumn(1, "full_name", Arrays.asList("first", "last"),
         *     arr -> arr.get(0) + " " + arr.get(1));
         * }</pre>
         *
         * @param columnIndex the position where the column should be inserted (0-based)
         * @param newColumnName the name of the new column
         * @param fromColumnNames the names of the source columns
         * @param func the function that combines values from source columns
         * @return this builder instance for method chaining
         */
        public DatasetBuilder addColumn(final int columnIndex, final String newColumnName, final Collection<String> fromColumnNames,
                final Function<? super DisposableObjArray, ?> func) {
            val.addColumn(columnIndex, newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Adds a new column by applying a binary function to values from two existing columns.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.addColumn("sum", Tuple.of("col1", "col2"),
         *     (Integer a, Integer b) -> a + b);
         * }</pre>
         *
         * @param newColumnName the name of the new column
         * @param fromColumnNames a tuple containing the names of two source columns
         * @param func the binary function to combine values from the two columns
         * @return this builder instance for method chaining
         */
        public DatasetBuilder addColumn(final String newColumnName, final Tuple2<String, String> fromColumnNames, final BiFunction<?, ?, ?> func) {
            val.addColumn(newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Adds a new column at a specific position by applying a binary function to values from two columns.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.addColumn(3, "sum", Tuple.of("col1", "col2"),
         *     (Integer a, Integer b) -> a + b);
         * }</pre>
         *
         * @param columnIndex the position where the column should be inserted (0-based)
         * @param newColumnName the name of the new column
         * @param fromColumnNames a tuple containing the names of two source columns
         * @param func the binary function to combine values from the two columns
         * @return this builder instance for method chaining
         */
        public DatasetBuilder addColumn(final int columnIndex, final String newColumnName, final Tuple2<String, String> fromColumnNames,
                final BiFunction<?, ?, ?> func) {
            val.addColumn(columnIndex, newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Adds a new column by applying a ternary function to values from three existing columns.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.addColumn("result", Tuple.of("a", "b", "c"),
         *     (Integer a, Integer b, Integer c) -> a + b + c);
         * }</pre>
         *
         * @param newColumnName the name of the new column
         * @param fromColumnNames a tuple containing the names of three source columns
         * @param func the ternary function to combine values from the three columns
         * @return this builder instance for method chaining
         */
        public DatasetBuilder addColumn(final String newColumnName, final Tuple3<String, String, String> fromColumnNames, final TriFunction<?, ?, ?, ?> func) {
            val.addColumn(newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Adds a new column at a specific position by applying a ternary function to values from three columns.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.addColumn(2, "result", Tuple.of("a", "b", "c"),
         *     (Integer a, Integer b, Integer c) -> a + b + c);
         * }</pre>
         *
         * @param columnIndex the position where the column should be inserted (0-based)
         * @param newColumnName the name of the new column
         * @param fromColumnNames a tuple containing the names of three source columns
         * @param func the ternary function to combine values from the three columns
         * @return this builder instance for method chaining
         */
        public DatasetBuilder addColumn(final int columnIndex, final String newColumnName, final Tuple3<String, String, String> fromColumnNames,
                final TriFunction<?, ?, ?, ?> func) {
            val.addColumn(columnIndex, newColumnName, fromColumnNames, func);

            return this;
        }

        /**
         * Removes a column from the dataset by name.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.removeColumn("unnecessary_column");
         * }</pre>
         *
         * @param columnName the name of the column to remove
         * @return this builder instance for method chaining
         */
        public DatasetBuilder removeColumn(final String columnName) {
            val.removeColumn(columnName);

            return this;
        }

        /**
         * Removes multiple columns from the dataset by their names.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.removeColumns(Arrays.asList("col1", "col2", "col3"));
         * }</pre>
         *
         * @param columnNames the collection of column names to remove
         * @return this builder instance for method chaining
         */
        public DatasetBuilder removeColumns(final Collection<String> columnNames) {
            val.removeColumns(columnNames);

            return this;
        }

        /**
         * Removes all columns that match the specified predicate condition.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.removeColumns(name -> name.startsWith("temp_"));
         * }</pre>
         *
         * @param filter the predicate to test column names; columns returning true are removed
         * @return this builder instance for method chaining
         */
        public DatasetBuilder removeColumns(final Predicate<? super String> filter) {
            val.removeColumns(filter);

            return this;
        }

        //        /**
        //         *
        //         * @param filter
        //         * @return
        //         * @deprecated replaced by {@code removeColumns}.
        //         */
        //        @Deprecated
        //        public DatasetBuilder removeColumnsIf(Predicate<? super String> filter) {
        //            val.removeColumnsIf(filter);
        //
        //            return this;
        //        }

        /**
         * Updates all values in a column by applying a transformation function.
         * Each value in the column is replaced with the result of the function.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.updateColumn("price", (Double p) -> p * 1.1);
         * }</pre>
         *
         * @param columnName the name of the column to update
         * @param func the function to transform each value in the column
         * @return this builder instance for method chaining
         */
        public DatasetBuilder updateColumn(final String columnName, final Function<?, ?> func) {
            val.updateColumn(columnName, func);

            return this;
        }

        /**
         * Updates all values in multiple columns by applying the same transformation function.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.updateColumns(Arrays.asList("price", "cost"), 
         *                             (Double v) -> v * 1.1);
         * }</pre>
         *
         * @param columnNames the names of columns to update
         * @param func the function to transform values in the specified columns
         * @return this builder instance for method chaining
         */
        public DatasetBuilder updateColumns(final Collection<String> columnNames, final Function<?, ?> func) {
            val.updateColumns(columnNames, func);

            return this;
        }

        /**
         * Converts a column's data type to the specified target type.
         * Values are converted using appropriate type conversion rules.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.convertColumn("age", Integer.class);
         * }</pre>
         *
         * @param columnName the name of the column to convert
         * @param targetType the target class type for the column values
         * @return this builder instance for method chaining
         */
        public DatasetBuilder convertColumn(final String columnName, final Class<?> targetType) {
            val.convertColumn(columnName, targetType);

            return this;
        }

        /**
         * Converts multiple columns to their specified target types.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Map<String, Class<?>> conversions = Map.of(
         *     "age", Integer.class,
         *     "salary", Double.class
         * );
         * datasetBuilder.convertColumns(conversions);
         * }</pre>
         *
         * @param columnTargetTypes a map of column names to their target types
         * @return this builder instance for method chaining
         */
        public DatasetBuilder convertColumns(final Map<String, Class<?>> columnTargetTypes) {
            val.convertColumns(columnTargetTypes);

            return this;
        }

        /**
         * Combines multiple columns into a single new column of the specified type.
         * The values from the source columns are merged based on the target type's requirements.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.combineColumns(Arrays.asList("year", "month", "day"), 
         *                              "date", LocalDate.class);
         * }</pre>
         *
         * @param columnNames the names of columns to combine
         * @param newColumnName the name of the resulting combined column
         * @param newColumnClass the class type of the new column
         * @return this builder instance for method chaining
         */
        public DatasetBuilder combineColumns(final Collection<String> columnNames, final String newColumnName, final Class<?> newColumnClass) {
            val.combineColumns(columnNames, newColumnName, newColumnClass);

            return this;
        }

        /**
         * Combines multiple columns into a single new column using a custom combine function.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.combineColumns(Arrays.asList("first", "last"), "fullName",
         *     arr -> arr.get(0) + " " + arr.get(1));
         * }</pre>
         *
         * @param columnNames the names of columns to combine
         * @param newColumnName the name of the resulting combined column
         * @param combineFunc the function that combines values from the source columns
         * @return this builder instance for method chaining
         */
        public DatasetBuilder combineColumns(final Collection<String> columnNames, final String newColumnName,
                final Function<? super DisposableObjArray, ?> combineFunc) {
            val.combineColumns(columnNames, newColumnName, combineFunc);

            return this;
        }

        /**
         * Combines two columns into a single new column using a binary function.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.combineColumns(Tuple.of("width", "height"), "area",
         *     (Integer w, Integer h) -> w * h);
         * }</pre>
         *
         * @param columnNames a tuple containing the names of two columns to combine
         * @param newColumnName the name of the resulting combined column
         * @param combineFunc the binary function to combine values from the two columns
         * @return this builder instance for method chaining
         */
        public DatasetBuilder combineColumns(final Tuple2<String, String> columnNames, final String newColumnName, final BiFunction<?, ?, ?> combineFunc) {
            val.combineColumns(columnNames, newColumnName, combineFunc);

            return this;
        }

        /**
         * Combines three columns into a single new column using a ternary function.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.combineColumns(Tuple.of("red", "green", "blue"), "rgb",
         *     (Integer r, Integer g, Integer b) -> String.format("#%02X%02X%02X", r, g, b));
         * }</pre>
         *
         * @param columnNames a tuple containing the names of three columns to combine
         * @param newColumnName the name of the resulting combined column
         * @param combineFunc the ternary function to combine values from the three columns
         * @return this builder instance for method chaining
         */
        public DatasetBuilder combineColumns(final Tuple3<String, String, String> columnNames, final String newColumnName,
                final TriFunction<?, ?, ?, ?> combineFunc) {
            val.combineColumns(columnNames, newColumnName, combineFunc);

            return this;
        }

        /**
         * Divides a single column into multiple new columns using a function that returns a list.
         * Each value in the source column is split into multiple values for the new columns.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.divideColumn("full_name", Arrays.asList("first", "last"),
         *     (String name) -> Arrays.asList(name.split(" ")));
         * }</pre>
         *
         * @param columnName the name of the column to divide
         * @param newColumnNames the names of the new columns to create
         * @param divideFunc the function that splits a value into multiple values
         * @return this builder instance for method chaining
         */
        public DatasetBuilder divideColumn(final String columnName, final Collection<String> newColumnNames, final Function<?, ? extends List<?>> divideFunc) {
            val.divideColumn(columnName, newColumnNames, divideFunc);

            return this;
        }

        /**
         * Divides a single column into multiple new columns using a consumer that populates an array.
         * The consumer receives the source value and an output array to fill.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.divideColumn("coordinates", Arrays.asList("x", "y", "z"),
         *     (String coords, Object[] output) -> {
         *         String[] parts = coords.split(",");
         *         output[0] = Double.parseDouble(parts[0]);
         *         output[1] = Double.parseDouble(parts[1]);
         *         output[2] = Double.parseDouble(parts[2]);
         *     });
         * }</pre>
         *
         * @param columnName the name of the column to divide
         * @param newColumnNames the names of the new columns to create
         * @param output the consumer that populates the output array with divided values
         * @return this builder instance for method chaining
         */
        public DatasetBuilder divideColumn(final String columnName, final Collection<String> newColumnNames, final BiConsumer<?, Object[]> output) {
            val.divideColumn(columnName, newColumnNames, output);

            return this;
        }

        /**
         * Divides a single column into two new columns using a consumer that populates a Pair.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.divideColumn("key_value", Tuple.of("key", "value"),
         *     (String kv, Pair<Object, Object> output) -> {
         *         String[] parts = kv.split(":");
         *         output.setLeft(parts[0]);
         *         output.setRight(parts[1]);
         *     });
         * }</pre>
         *
         * @param columnName the name of the column to divide
         * @param newColumnNames a tuple containing the names of two new columns
         * @param output the consumer that populates the output pair
         * @return this builder instance for method chaining
         */
        public DatasetBuilder divideColumn(final String columnName, final Tuple2<String, String> newColumnNames,
                final BiConsumer<?, Pair<Object, Object>> output) {
            val.divideColumn(columnName, newColumnNames, output);

            return this;
        }

        /**
         * Divides a single column into three new columns using a consumer that populates a Triple.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.divideColumn("rgb", Tuple.of("red", "green", "blue"),
         *     (String color, Triple<Object, Object, Object> output) -> {
         *         // Parse RGB values and populate triple
         *         output.setLeft(red);
         *         output.setMiddle(green);
         *         output.setRight(blue);
         *     });
         * }</pre>
         *
         * @param columnName the name of the column to divide
         * @param newColumnNames a tuple containing the names of three new columns
         * @param output the consumer that populates the output triple
         * @return this builder instance for method chaining
         */
        public DatasetBuilder divideColumn(final String columnName, final Tuple3<String, String, String> newColumnNames,
                final BiConsumer<?, Triple<Object, Object, Object>> output) {
            val.divideColumn(columnName, newColumnNames, output);

            return this;
        }

        /**
         * Updates all values in the entire dataset by applying a transformation function.
         * The function is applied to every value in every column.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.updateAll(value -> value == null ? "" : value);
         * }</pre>
         *
         * @param func the function to transform all values in the dataset
         * @return this builder instance for method chaining
         */
        public DatasetBuilder updateAll(final Function<?, ?> func) {
            val.updateAll(func);

            return this;
        }

        /**
         * Replaces all values in the dataset that match a predicate with a new value.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * datasetBuilder.replaceIf(value -> value == null || "".equals(value), "N/A");
         * }</pre>
         *
         * @param predicate the condition to test each value
         * @param newValue the replacement value for matching elements
         * @return this builder instance for method chaining
         */
        public DatasetBuilder replaceIf(final Predicate<?> predicate, final Object newValue) {
            val.replaceIf(predicate, newValue);

            return this;
        }

        /**
         * Prepends the rows from another Dataset to the beginning of this Dataset.
         * The columns of both Datasets must be identical.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Dataset header = // ... dataset with header rows
         * datasetBuilder.prepend(header);
         * }</pre>
         *
         * @param other the Dataset whose rows should be added at the beginning
         * @return this builder instance for method chaining
         * @see Dataset#prepend(Dataset)
         */
        public DatasetBuilder prepend(final Dataset other) {
            val.prepend(other);

            return this;
        }

        /**
         * Appends the rows from another Dataset to the end of this Dataset.
         * The columns of both Datasets must be identical.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * Dataset moreData = // ... dataset with additional rows
         * datasetBuilder.append(moreData);
         * }</pre>
         *
         * @param other the Dataset whose rows should be added at the end
         * @return this builder instance for method chaining
         * @see Dataset#append(Dataset)
         */
        public DatasetBuilder append(final Dataset other) {
            val.append(other);

            return this;
        }

        //    /**
        //     *
        //     * @param columnName
        //     * @return
        //     */
        //    public DatasetBuilder sortBy(final String columnName) {
        //        val.sortBy(columnName);
        //
        //        return this;
        //    }
        //
        //    /**
        //     *
        //     * @param <T>
        //     * @param columnName
        //     * @param cmp
        //     * @return
        //     */
        //    public DatasetBuilder sortBy(final String columnName, final Comparator<?> cmp) {
        //        val.sortBy(columnName, cmp);
        //
        //        return this;
        //    }
        //
        //    /**
        //     *
        //     * @param columnNames
        //     * @return
        //     */
        //    public DatasetBuilder sortBy(final Collection<String> columnNames) {
        //        val.sortBy(columnNames);
        //
        //        return this;
        //    }
        //
        //    /**
        //     *
        //     * @param columnNames
        //     * @param cmp
        //     * @return
        //     */
        //    public DatasetBuilder sortBy(final Collection<String> columnNames, final Comparator<? super Object[]> cmp) {
        //        val.sortBy(columnNames, cmp);
        //
        //        return this;
        //    }
        //
        //    /**
        //     *
        //     * @param columnNames
        //     * @param keyExtractor
        //     * @return
        //     */
        //    @SuppressWarnings("rawtypes")
        //    public DatasetBuilder sortBy(final Collection<String> columnNames, final Function<? super DisposableObjArray, ? extends Comparable> keyExtractor) {
        //        val.sortBy(columnNames, keyExtractor);
        //
        //        return this;
        //    }
        //
        //    /**
        //     * Parallel sort by.
        //     *
        //     * @param columnName
        //     * @return
        //     */
        //    public DatasetBuilder parallelSortBy(final String columnName) {
        //        val.parallelSortBy(columnName);
        //
        //        return this;
        //    }
        //
        //    /**
        //     * Parallel sort by.
        //     *
        //     * @param <T>
        //     * @param columnName
        //     * @param cmp
        //     * @return
        //     */
        //    public DatasetBuilder parallelSortBy(final String columnName, final Comparator<?> cmp) {
        //        val.parallelSortBy(columnName, cmp);
        //
        //        return this;
        //    }
        //
        //    /**
        //     * Parallel sort by.
        //     *
        //     * @param columnNames
        //     * @return
        //     */
        //    public DatasetBuilder parallelSortBy(final Collection<String> columnNames) {
        //        val.parallelSortBy(columnNames);
        //
        //        return this;
        //    }
        //
        //    /**
        //     * Parallel sort by.
        //     *
        //     * @param columnNames
        //     * @param cmp
        //     * @return
        //     */
        //    public DatasetBuilder parallelSortBy(final Collection<String> columnNames, final Comparator<? super Object[]> cmp) {
        //        val.parallelSortBy(columnNames, cmp);
        //
        //        return this;
        //    }
        //
        //    /**
        //     * Parallel sort by.
        //     *
        //     * @param columnNames
        //     * @param keyExtractor
        //     * @return
        //     */
        //    @SuppressWarnings("rawtypes")
        //    public DatasetBuilder parallelSortBy(final Collection<String> columnNames,
        //            final Function<? super DisposableObjArray, ? extends Comparable> keyExtractor) {
        //        val.parallelSortBy(columnNames, keyExtractor);
        //
        //        return this;
        //    }
    }

    /**
     * Creates a new ComparisonBuilder and compares two comparable objects using their natural ordering.
     * This is the starting point for building a comparison chain.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int result = Builder.compare(obj1.getName(), obj2.getName())
     *                     .compare(obj1.getAge(), obj2.getAge())
     *                     .result();
     * }</pre>
     *
     * @param <T> the type of objects being compared (must be Comparable)
     * @param left the first object to compare
     * @param right the second object to compare
     * @return a new ComparisonBuilder for method chaining
     */
    public static <T extends Comparable<? super T>> ComparisonBuilder compare(final T left, final T right) {
        return new ComparisonBuilder().compare(left, right);
    }

    /**
     * Creates a new ComparisonBuilder and compares two objects using a specified comparator.
     * This is the starting point for building a comparison chain with custom comparison logic.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int result = Builder.compare(str1, str2, String.CASE_INSENSITIVE_ORDER)
     *                     .compare(obj1.getDate(), obj2.getDate())
     *                     .result();
     * }</pre>
     *
     * @param <T> the type of objects being compared
     * @param left the first object to compare
     * @param right the second object to compare
     * @param comparator the comparator to use for comparison
     * @return a new ComparisonBuilder for method chaining
     */
    public static <T> ComparisonBuilder compare(final T left, final T right, final Comparator<T> comparator) {
        return new ComparisonBuilder().compare(left, right, comparator);
    }

    //    /**
    //     *
    //     * @param <T>
    //     * @param left
    //     * @param right
    //     * @param func
    //     * @return
    //     */
    //    public static <T> ComparisonBuilder compare(T left, T right, BiFunction<? super T, ? super T, Integer> func) {
    //        return new ComparisonBuilder().compare(left, right, func);
    //    }

    /**
     * Creates a new ComparisonBuilder and compares two comparable objects, treating null as smaller.
     * Null values are considered less than non-null values in the comparison.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int result = Builder.compareNullLess(obj1.getName(), obj2.getName())
     *                     .compareNullLess(obj1.getDescription(), obj2.getDescription())
     *                     .result();
     * }</pre>
     *
     * @param <T> the type of objects being compared (must be Comparable)
     * @param left the first object to compare
     * @param right the second object to compare
     * @return a new ComparisonBuilder for method chaining
     */
    public static <T extends Comparable<? super T>> ComparisonBuilder compareNullLess(final T left, final T right) {
        return new ComparisonBuilder().compareNullLess(left, right);
    }

    /**
     * Creates a new ComparisonBuilder and compares two comparable objects, treating null as bigger.
     * Null values are considered greater than non-null values in the comparison.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int result = Builder.compareNullBigger(obj1.getOptionalField(), obj2.getOptionalField())
     *                     .compare(obj1.getId(), obj2.getId())
     *                     .result();
     * }</pre>
     *
     * @param <T> the type of objects being compared (must be Comparable)
     * @param left the first object to compare
     * @param right the second object to compare
     * @return a new ComparisonBuilder for method chaining
     */
    public static <T extends Comparable<? super T>> ComparisonBuilder compareNullBigger(final T left, final T right) {
        return new ComparisonBuilder().compareNullBigger(left, right);
    }

    /**
     * Creates a new ComparisonBuilder and compares two boolean values, treating false as less than true.
     * This is useful for sorting where false values should come before true values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int result = Builder.compareFalseLess(obj1.isActive(), obj2.isActive())
     *                     .compare(obj1.getName(), obj2.getName())
     *                     .result();
     * }</pre>
     *
     * @param left the first boolean to compare
     * @param right the second boolean to compare
     * @return a new ComparisonBuilder for method chaining
     */
    public static ComparisonBuilder compareFalseLess(final boolean left, final boolean right) {
        return new ComparisonBuilder().compareFalseLess(left, right);
    }

    /**
     * Creates a new ComparisonBuilder and compares two boolean values, treating true as less than false.
     * This is useful for sorting where true values should come before false values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int result = Builder.compareTrueLess(obj1.isPriority(), obj2.isPriority())
     *                     .compare(obj1.getTimestamp(), obj2.getTimestamp())
     *                     .result();
     * }</pre>
     *
     * @param left the first boolean to compare
     * @param right the second boolean to compare
     * @return a new ComparisonBuilder for method chaining
     */
    public static ComparisonBuilder compareTrueLess(final boolean left, final boolean right) {
        return new ComparisonBuilder().compareTrueLess(left, right);
    }

    /**
     * Creates a new ComparisonBuilder and compares two char values.
     * Characters are compared by their numeric values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int result = Builder.compare(obj1.getGrade(), obj2.getGrade())
     *                     .compare(obj1.getScore(), obj2.getScore())
     *                     .result();
     * }</pre>
     *
     * @param left the first char to compare
     * @param right the second char to compare
     * @return a new ComparisonBuilder for method chaining
     */
    public static ComparisonBuilder compare(final char left, final char right) {
        return new ComparisonBuilder().compare(left, right);
    }

    /**
     * Creates a new ComparisonBuilder and compares two byte values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int result = Builder.compare(obj1.getPriority(), obj2.getPriority())
     *                     .compare(obj1.getLevel(), obj2.getLevel())
     *                     .result();
     * }</pre>
     *
     * @param left the first byte to compare
     * @param right the second byte to compare
     * @return a new ComparisonBuilder for method chaining
     */
    public static ComparisonBuilder compare(final byte left, final byte right) {
        return new ComparisonBuilder().compare(left, right);
    }

    /**
     * Creates a new ComparisonBuilder and compares two short values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int result = Builder.compare(obj1.getRank(), obj2.getRank())
     *                     .compare(obj1.getYear(), obj2.getYear())
     *                     .result();
     * }</pre>
     *
     * @param left the first short to compare
     * @param right the second short to compare
     * @return a new ComparisonBuilder for method chaining
     */
    public static ComparisonBuilder compare(final short left, final short right) {
        return new ComparisonBuilder().compare(left, right);
    }

    /**
     * Creates a new ComparisonBuilder and compares two int values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int result = Builder.compare(obj1.getAge(), obj2.getAge())
     *                     .compare(obj1.getId(), obj2.getId())
     *                     .result();
     * }</pre>
     *
     * @param left the first int to compare
     * @param right the second int to compare
     * @return a new ComparisonBuilder for method chaining
     */
    public static ComparisonBuilder compare(final int left, final int right) {
        return new ComparisonBuilder().compare(left, right);
    }

    /**
     * Creates a new ComparisonBuilder and compares two long values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int result = Builder.compare(obj1.getTimestamp(), obj2.getTimestamp())
     *                     .compare(obj1.getSize(), obj2.getSize())
     *                     .result();
     * }</pre>
     *
     * @param left the first long to compare
     * @param right the second long to compare
     * @return a new ComparisonBuilder for method chaining
     */
    public static ComparisonBuilder compare(final long left, final long right) {
        return new ComparisonBuilder().compare(left, right);
    }

    /**
     * Creates a new ComparisonBuilder and compares two float values.
     * Uses Float.compare() for proper handling of NaN and special values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int result = Builder.compare(obj1.getWeight(), obj2.getWeight())
     *                     .compare(obj1.getHeight(), obj2.getHeight())
     *                     .result();
     * }</pre>
     *
     * @param left the first float to compare
     * @param right the second float to compare
     * @return a new ComparisonBuilder for method chaining
     */
    public static ComparisonBuilder compare(final float left, final float right) {
        return new ComparisonBuilder().compare(left, right);
    }

    /**
     * Creates a new ComparisonBuilder and compares two float values with a specified tolerance.
     * This is useful for comparing floating-point numbers where exact equality is not required.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int result = Builder.compare(obj1.getWeight(), obj2.getWeight(), 0.01f)
     *                     .compare(obj1.getHeight(), obj2.getHeight(), 0.01f)
     *                     .result();
     * }</pre>
     *
     * @param left the first float to compare
     * @param right the second float to compare
     * @param tolerance the acceptable difference between the two floats
     * @return a new ComparisonBuilder for method chaining
     */
    public static ComparisonBuilder compare(final float left, final float right, final float tolerance) {
        return new ComparisonBuilder().compare(left, right, tolerance);
    }

    /**
     * Creates a new ComparisonBuilder and compares two double values.
     * Uses Double.compare() for proper handling of NaN and special values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int result = Builder.compare(obj1.getPrice(), obj2.getPrice())
     *                     .compare(obj1.getDiscount(), obj2.getDiscount())
     *                     .result();
     * }</pre>
     *
     * @param left the first double to compare
     * @param right the second double to compare
     * @return a new ComparisonBuilder for method chaining
     */
    public static ComparisonBuilder compare(final double left, final double right) {
        return new ComparisonBuilder().compare(left, right);
    }

    /**
     * Creates a new ComparisonBuilder and compares two double values with a specified tolerance.
     * This is useful for comparing floating-point numbers where exact equality is not required.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int result = Builder.compare(obj1.getPrice(), obj2.getPrice(), 0.01)
     *                     .compare(obj1.getDiscount(), obj2.getDiscount(), 0.01)
     *                     .result();
     * }</pre>
     *
     * @param left the first double to compare
     * @param right the second double to compare
     * @param tolerance the acceptable difference between the two doubles
     * @return a new ComparisonBuilder for method chaining
     */
    public static ComparisonBuilder compare(final double left, final double right, final double tolerance) {
        return new ComparisonBuilder().compare(left, right, tolerance);
    }

    /**
     * Creates a new EquivalenceBuilder and checks equality of two objects.
     * Uses N.equals() for null-safe comparison.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * boolean equal = Builder.equals(obj1.getName(), obj2.getName())
     *                        .equals(obj1.getAge(), obj2.getAge())
     *                        .result();
     * }</pre>
     *
     * @param left the first object to compare
     * @param right the second object to compare
     * @return a new EquivalenceBuilder for method chaining
     */
    public static EquivalenceBuilder equals(final Object left, final Object right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Creates a new EquivalenceBuilder and checks equality using a custom function.
     * The function determines whether the two objects should be considered equal.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * boolean equal = Builder.equals(obj1, obj2, (a, b) -> a.getId() == b.getId())
     *                        .equals(obj1.getName(), obj2.getName())
     *                        .result();
     * }</pre>
     *
     * @param <T> the type of objects being compared
     * @param left the first object to compare
     * @param right the second object to compare
     * @param predicate the predicate to check equality, must not be null
     * @return a new EquivalenceBuilder for method chaining
     */
    public static <T> EquivalenceBuilder equals(final T left, final T right, final BiPredicate<? super T, ? super T> predicate) {
        return new EquivalenceBuilder().equals(left, right, predicate);
    }

    /**
     * Creates a new EquivalenceBuilder and checks equality of two boolean values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * boolean equal = Builder.equals(obj1.isActive(), obj2.isActive())
     *                        .equals(obj1.isEnabled(), obj2.isEnabled())
     *                        .result();
     * }</pre>
     *
     * @param left the first boolean to compare
     * @param right the second boolean to compare
     * @return a new EquivalenceBuilder for method chaining
     */
    public static EquivalenceBuilder equals(final boolean left, final boolean right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Creates a new EquivalenceBuilder and checks equality of two char values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * boolean equal = Builder.equals(obj1.getGrade(), obj2.getGrade())
     *                        .equals(obj1.getCategory(), obj2.getCategory())
     *                        .result();
     * }</pre>
     *
     * @param left the first char to compare
     * @param right the second char to compare
     * @return a new EquivalenceBuilder for method chaining
     */
    public static EquivalenceBuilder equals(final char left, final char right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Creates a new EquivalenceBuilder and checks equality of two byte values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * boolean equal = Builder.equals(obj1.getLevel(), obj2.getLevel())
     *                        .equals(obj1.getRank(), obj2.getRank())
     *                        .result();
     * }</pre>
     *
     * @param left the first byte to compare
     * @param right the second byte to compare
     * @return a new EquivalenceBuilder for method chaining
     */
    public static EquivalenceBuilder equals(final byte left, final byte right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Creates a new EquivalenceBuilder and checks equality of two short values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * boolean equal = Builder.equals(obj1.getYear(), obj2.getYear())
     *                        .equals(obj1.getMonth(), obj2.getMonth())
     *                        .result();
     * }</pre>
     *
     * @param left the first short to compare
     * @param right the second short to compare
     * @return a new EquivalenceBuilder for method chaining
     */
    public static EquivalenceBuilder equals(final short left, final short right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Creates a new EquivalenceBuilder and checks equality of two int values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * boolean equal = Builder.equals(obj1.getId(), obj2.getId())
     *                        .equals(obj1.getAge(), obj2.getAge())
     *                        .result();
     * }</pre>
     *
     * @param left the first int to compare
     * @param right the second int to compare
     * @return a new EquivalenceBuilder for method chaining
     */
    public static EquivalenceBuilder equals(final int left, final int right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Creates a new EquivalenceBuilder and checks equality of two long values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * boolean equal = Builder.equals(obj1.getTimestamp(), obj2.getTimestamp())
     *                        .equals(obj1.getSize(), obj2.getSize())
     *                        .result();
     * }</pre>
     *
     * @param left the first long to compare
     * @param right the second long to compare
     * @return a new EquivalenceBuilder for method chaining
     */
    public static EquivalenceBuilder equals(final long left, final long right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Creates a new EquivalenceBuilder and checks equality of two float values.
     * Uses Float.compare() for proper handling of NaN and special values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * boolean equal = Builder.equals(obj1.getWeight(), obj2.getWeight())
     *                        .equals(obj1.getHeight(), obj2.getHeight())
     *                        .result();
     * }</pre>
     *
     * @param left the first float to compare
     * @param right the second float to compare
     * @return a new EquivalenceBuilder for method chaining
     */
    public static EquivalenceBuilder equals(final float left, final float right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Creates a new EquivalenceBuilder and checks equality of two float values with a specified tolerance.
     * This is useful for comparing floating-point numbers where exact equality is not required.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * boolean equal = Builder.equals(obj1.getWeight(), obj2.getWeight(), 0.01f)
     *                        .equals(obj1.getHeight(), obj2.getHeight(), 0.01f)
     *                        .result();
     * }</pre>
     *
     * @param left the first float to compare
     * @param right the second float to compare
     * @param tolerance the acceptable difference between the two floats
     * @return a new EquivalenceBuilder for method chaining
     */
    public static EquivalenceBuilder equals(final float left, final float right, final float tolerance) {
        return new EquivalenceBuilder().equals(left, right, tolerance);
    }

    /**
     * Creates a new EquivalenceBuilder and checks equality of two double values.
     * Uses Double.compare() for proper handling of NaN and special values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * boolean equal = Builder.equals(obj1.getPrice(), obj2.getPrice())
     *                        .equals(obj1.getDiscount(), obj2.getDiscount())
     *                        .result();
     * }</pre>
     *
     * @param left the first double to compare
     * @param right the second double to compare
     * @return a new EquivalenceBuilder for method chaining
     */
    public static EquivalenceBuilder equals(final double left, final double right) {
        return new EquivalenceBuilder().equals(left, right);
    }

    /**
     * Creates a new EquivalenceBuilder and checks equality of two double values with a specified tolerance.
     * This is useful for comparing floating-point numbers where exact equality is not required.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * boolean equal = Builder.equals(obj1.getPrice(), obj2.getPrice(), 0.01)
     *                        .equals(obj1.getDiscount(), obj2.getDiscount(), 0.01)
     *                        .result();
     * }</pre>
     *
     * @param left the first double to compare
     * @param right the second double to compare
     * @param tolerance the acceptable difference between the two doubles
     * @return a new EquivalenceBuilder for method chaining
     */
    public static EquivalenceBuilder equals(final double left, final double right, final double tolerance) {
        return new EquivalenceBuilder().equals(left, right, tolerance);
    }

    /**
     * Creates a new HashCodeBuilder and adds the hash code of the specified object.
     * This is the starting point for building a hash code from multiple values.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int hashCode = Builder.hash(obj.getName())
     *                       .hash(obj.getAge())
     *                       .hash(obj.isActive())
     *                       .result();
     * }</pre>
     *
     * @param value the object whose hash code should be added
     * @return a new HashCodeBuilder for method chaining
     */
    public static HashCodeBuilder hash(final Object value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Creates a new HashCodeBuilder and adds a hash code computed by a custom function.
     * This allows custom hash code calculation for complex objects.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int hashCode = Builder.hash(person, p -> p.getId())
     *                       .hash(person.getName())
     *                       .result();
     * }</pre>
     *
     * @param <T> the type of the value
     * @param value the object to hash
     * @param func the function that computes the hash code for the value
     * @return a new HashCodeBuilder for method chaining
     */
    public static <T> HashCodeBuilder hash(final T value, final ToIntFunction<? super T> func) {
        return new HashCodeBuilder().hash(value, func);
    }

    /**
     * Creates a new HashCodeBuilder and adds the hash code of a boolean value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int hashCode = Builder.hash(obj.isActive())
     *                       .hash(obj.isEnabled())
     *                       .result();
     * }</pre>
     *
     * @param value the boolean value to hash
     * @return a new HashCodeBuilder for method chaining
     */
    public static HashCodeBuilder hash(final boolean value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Creates a new HashCodeBuilder and adds the hash code of a char value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int hashCode = Builder.hash(obj.getGrade())
     *                       .hash(obj.getCategory())
     *                       .result();
     * }</pre>
     *
     * @param value the char value to hash
     * @return a new HashCodeBuilder for method chaining
     */
    public static HashCodeBuilder hash(final char value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Creates a new HashCodeBuilder and adds the hash code of a byte value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int hashCode = Builder.hash(obj.getLevel())
     *                       .hash(obj.getRank())
     *                       .result();
     * }</pre>
     *
     * @param value the byte value to hash
     * @return a new HashCodeBuilder for method chaining
     */
    public static HashCodeBuilder hash(final byte value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Creates a new HashCodeBuilder and adds the hash code of a short value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int hashCode = Builder.hash(obj.getYear())
     *                       .hash(obj.getMonth())
     *                       .result();
     * }</pre>
     *
     * @param value the short value to hash
     * @return a new HashCodeBuilder for method chaining
     */
    public static HashCodeBuilder hash(final short value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Creates a new HashCodeBuilder and adds the hash code of an int value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int hashCode = Builder.hash(obj.getId())
     *                       .hash(obj.getAge())
     *                       .result();
     * }</pre>
     *
     * @param value the int value to hash
     * @return a new HashCodeBuilder for method chaining
     */
    public static HashCodeBuilder hash(final int value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Creates a new HashCodeBuilder and adds the hash code of a long value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int hashCode = Builder.hash(obj.getTimestamp())
     *                       .hash(obj.getSize())
     *                       .result();
     * }</pre>
     *
     * @param value the long value to hash
     * @return a new HashCodeBuilder for method chaining
     */
    public static HashCodeBuilder hash(final long value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Creates a new HashCodeBuilder and adds the hash code of a float value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int hashCode = Builder.hash(obj.getWeight())
     *                       .hash(obj.getHeight())
     *                       .result();
     * }</pre>
     *
     * @param value the float value to hash
     * @return a new HashCodeBuilder for method chaining
     */
    public static HashCodeBuilder hash(final float value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * Creates a new HashCodeBuilder and adds the hash code of a double value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * int hashCode = Builder.hash(obj.getPrice())
     *                       .hash(obj.getDiscount())
     *                       .result();
     * }</pre>
     *
     * @param value the double value to hash
     * @return a new HashCodeBuilder for method chaining
     */
    public static HashCodeBuilder hash(final double value) {
        return new HashCodeBuilder().hash(value);
    }

    /**
     * A builder class for performing chained comparisons between objects.
     * This class follows the builder pattern to allow multiple comparisons to be
     * chained together, with the result being determined by the first non-zero
     * comparison result.
     * 
     * <p>The comparison chain short-circuits on the first non-equal comparison,
     * making it efficient for comparing objects with multiple fields. Once a 
     * non-zero result is found, subsequent comparisons are skipped.</p>
     * 
     * <p><strong>Thread Safety:</strong> This class is not thread-safe and should
     * not be shared between threads.</p>
     * 
     * <p><strong>Example usage:</strong></p>
     * <pre>{@code
     * int result = Builder
     *     .compare(person1.getLastName(), person2.getLastName())
     *     .compare(person1.getFirstName(), person2.getFirstName())
     *     .compare(person1.getAge(), person2.getAge())
     *     .result();
     * }</pre>
     * 
     * @author HaiYang Li
     * @since 0.8
     */
    public static final class ComparisonBuilder {

        /** The result. */
        private int result = 0;

        /**
         * Instantiates a new comparison chain.
         */
        private ComparisonBuilder() {
            // singleton.
        }

        /**
         * Compares two comparable objects using their natural ordering.
         * If the result of this comparison chain has not already been determined
         * (i.e., all previous comparisons returned 0), this method compares the
         * two objects using {@link Comparable#compareTo}.
         * 
         * <p>This method handles null values gracefully, considering them equal
         * to each other but less than any non-null value.</p>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * ComparisonBuilder.create()
         *     .compare("apple", "banana")
         *     .result(); // returns negative value
         * }</pre>
         *
         * @param <T> the type of objects being compared, must implement Comparable
         * @param left the first object to compare, may be null
         * @param right the second object to compare, may be null
         * @return this ComparisonBuilder instance for method chaining
         */
        public <T extends Comparable<? super T>> ComparisonBuilder compare(final T left, final T right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two objects using a specified comparator.
         * If the result of this comparison chain has not already been determined,
         * this method uses the provided comparator to compare the objects.
         * 
         * <p>If the comparator is null, the natural ordering is used (objects must
         * implement Comparable). This allows for flexible comparison strategies.</p>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * ComparisonBuilder.create()
         *     .compare(person1, person2, Comparator.comparing(Person::getAge))
         *     .result();
         * }</pre>
         *
         * @param <T> the type of objects being compared
         * @param left the first object to compare
         * @param right the second object to compare
         * @param comparator the comparator to use, or null for natural ordering
         * @return this ComparisonBuilder instance for method chaining
         * @throws IllegalArgumentException if comparator is null and objects don't implement Comparable
         */
        public <T> ComparisonBuilder compare(final T left, final T right, final Comparator<T> comparator) throws IllegalArgumentException {
            if (result == 0) {
                if (comparator == null) {
                    result = Comparators.NATURAL_ORDER.compare(left, right);
                } else {
                    result = comparator.compare(left, right);
                }
            }

            return this;
        }

        /**
         * Compares two comparable objects with null values considered less than non-null values.
         * This method provides explicit null-handling behavior where null is treated as the
         * smallest possible value.
         * 
         * <p>Comparison behavior:</p>
         * <ul>
         *   <li>null == null returns 0</li>
         *   <li>null &lt; non-null returns -1</li>
         *   <li>non-null &gt; null returns 1</li>
         *   <li>non-null values are compared using compareTo</li>
         * </ul>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * ComparisonBuilder.create()
         *     .compareNullLess(null, "value")
         *     .result(); // returns -1
         * }</pre>
         *
         * @param <T> the type of objects being compared, must implement Comparable
         * @param left the first object to compare, may be null
         * @param right the second object to compare, may be null
         * @return this ComparisonBuilder instance for method chaining
         */
        public <T extends Comparable<? super T>> ComparisonBuilder compareNullLess(final T left, final T right) {
            if (result == 0) {
                result = left == null ? (right == null ? 0 : -1) : (right == null ? 1 : left.compareTo(right));
            }

            return this;
        }

        /**
         * Compares two comparable objects with null values considered greater than non-null values.
         * This method provides explicit null-handling behavior where null is treated as the
         * largest possible value.
         * 
         * <p>Comparison behavior:</p>
         * <ul>
         *   <li>null == null returns 0</li>
         *   <li>null &gt; non-null returns 1</li>
         *   <li>non-null &lt; null returns -1</li>
         *   <li>non-null values are compared using compareTo</li>
         * </ul>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * ComparisonBuilder.create()
         *     .compareNullBigger("value", null)
         *     .result(); // returns -1
         * }</pre>
         *
         * @param <T> the type of objects being compared, must implement Comparable
         * @param left the first object to compare, may be null
         * @param right the second object to compare, may be null
         * @return this ComparisonBuilder instance for method chaining
         */
        public <T extends Comparable<? super T>> ComparisonBuilder compareNullBigger(final T left, final T right) {
            if (result == 0) {
                result = left == null ? (right == null ? 0 : 1) : (right == null ? -1 : left.compareTo(right));
            }

            return this;
        }

        /**
         * Compares two boolean values with false considered less than true.
         * If the result of this comparison chain has not already been determined,
         * this method compares the boolean values.
         * 
         * <p>Comparison behavior:</p>
         * <ul>
         *   <li>false &lt; true returns -1</li>
         *   <li>true &gt; false returns 1</li>
         *   <li>equal values return 0</li>
         * </ul>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * ComparisonBuilder.create()
         *     .compareFalseLess(false, true)
         *     .result(); // returns -1
         * }</pre>
         *
         * @param left the first boolean value
         * @param right the second boolean value
         * @return this ComparisonBuilder instance for method chaining
         */
        public ComparisonBuilder compareFalseLess(final boolean left, final boolean right) {
            if (result == 0) {
                result = left == right ? 0 : (left ? 1 : -1);
            }

            return this;
        }

        /**
         * Compares two boolean values with true considered less than false.
         * If the result of this comparison chain has not already been determined,
         * this method compares the boolean values.
         * 
         * <p>Comparison behavior:</p>
         * <ul>
         *   <li>true &lt; false returns -1</li>
         *   <li>false &gt; true returns 1</li>
         *   <li>equal values return 0</li>
         * </ul>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * ComparisonBuilder.create()
         *     .compareTrueLess(true, false)
         *     .result(); // returns -1
         * }</pre>
         *
         * @param left the first boolean value
         * @param right the second boolean value
         * @return this ComparisonBuilder instance for method chaining
         */
        public ComparisonBuilder compareTrueLess(final boolean left, final boolean right) {
            if (result == 0) {
                result = left == right ? 0 : (left ? -1 : 1);
            }

            return this;
        }

        /**
         * Compares two char values numerically.
         * If the result of this comparison chain has not already been determined,
         * this method compares the char values based on their numeric values.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * ComparisonBuilder.create()
         *     .compare('a', 'b')
         *     .result(); // returns -1
         * }</pre>
         *
         * @param left the first char value
         * @param right the second char value
         * @return this ComparisonBuilder instance for method chaining
         */
        public ComparisonBuilder compare(final char left, final char right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two byte values numerically.
         * If the result of this comparison chain has not already been determined,
         * this method compares the byte values.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * ComparisonBuilder.create()
         *     .compare((byte)10, (byte)20)
         *     .result(); // returns -1
         * }</pre>
         *
         * @param left the first byte value
         * @param right the second byte value
         * @return this ComparisonBuilder instance for method chaining
         */
        public ComparisonBuilder compare(final byte left, final byte right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two short values numerically.
         * If the result of this comparison chain has not already been determined,
         * this method compares the short values.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * ComparisonBuilder.create()
         *     .compare((short)100, (short)200)
         *     .result(); // returns -1
         * }</pre>
         *
         * @param left the first short value
         * @param right the second short value
         * @return this ComparisonBuilder instance for method chaining
         */
        public ComparisonBuilder compare(final short left, final short right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two int values numerically.
         * If the result of this comparison chain has not already been determined,
         * this method compares the int values.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * ComparisonBuilder.create()
         *     .compare(42, 100)
         *     .result(); // returns -1
         * }</pre>
         *
         * @param left the first int value
         * @param right the second int value
         * @return this ComparisonBuilder instance for method chaining
         */
        public ComparisonBuilder compare(final int left, final int right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two long values numerically.
         * If the result of this comparison chain has not already been determined,
         * this method compares the long values.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * ComparisonBuilder.create()
         *     .compare(1000L, 2000L)
         *     .result(); // returns -1
         * }</pre>
         *
         * @param left the first long value
         * @param right the second long value
         * @return this ComparisonBuilder instance for method chaining
         */
        public ComparisonBuilder compare(final long left, final long right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two float values as specified by {@link Float#compare}.
         * If the result of this comparison chain has not already been determined,
         * this method compares the float values handling NaN and infinity correctly.
         * 
         * <p>This method handles special float values:</p>
         * <ul>
         *   <li>NaN is considered greater than any other value</li>
         *   <li>0.0f is considered greater than -0.0f</li>
         * </ul>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * ComparisonBuilder.create()
         *     .compare(1.5f, 2.5f)
         *     .result(); // returns -1
         * }</pre>
         *
         * @param left the first float value
         * @param right the second float value
         * @return this ComparisonBuilder instance for method chaining
         */
        public ComparisonBuilder compare(final float left, final float right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**        
         * Compares two float values using a fuzzy comparison with the given tolerance.
         * This method is useful for comparing floating-point numbers that may have
         * small differences due to rounding errors or precision limitations.
         * 
         * <p>Values are considered equal if their absolute difference is less than
         * or equal to the specified tolerance.</p>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * ComparisonBuilder.create()
         *     .compare(1.0001f, 1.0002f, 0.001f)
         *     .result(); // returns 0 (considered equal)
         * }</pre>
         *
         * @param left the first float to compare
         * @param right the second float to compare
         * @param tolerance the maximum difference allowed for the values to be considered equal
         * @return this ComparisonBuilder instance for method chaining
         */
        public ComparisonBuilder compare(final float left, final float right, final float tolerance) {
            if (result == 0) {
                result = Numbers.fuzzyCompare(left, right, tolerance);
            }

            return this;
        }

        /**
         * Compares two double values as specified by {@link Double#compare}.
         * If the result of this comparison chain has not already been determined,
         * this method compares the double values handling NaN and infinity correctly.
         * 
         * <p>This method handles special double values:</p>
         * <ul>
         *   <li>NaN is considered greater than any other value</li>
         *   <li>0.0d is considered greater than -0.0d</li>
         * </ul>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * ComparisonBuilder.create()
         *     .compare(1.5, 2.5)
         *     .result(); // returns -1
         * }</pre>
         *
         * @param left the first double value
         * @param right the second double value
         * @return this ComparisonBuilder instance for method chaining
         */
        public ComparisonBuilder compare(final double left, final double right) {
            if (result == 0) {
                result = N.compare(left, right);
            }

            return this;
        }

        /**
         * Compares two double values using a fuzzy comparison with the given tolerance.
         * This method is useful for comparing floating-point numbers that may have
         * small differences due to rounding errors or precision limitations.
         * 
         * <p>Values are considered equal if their absolute difference is less than
         * or equal to the specified tolerance.</p>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * ComparisonBuilder.create()
         *     .compare(1.00001, 1.00002, 0.0001)
         *     .result(); // returns 0 (considered equal)
         * }</pre>
         *
         * @param left the first double to compare
         * @param right the second double to compare
         * @param tolerance the maximum difference allowed for the values to be considered equal
         * @return this ComparisonBuilder instance for method chaining
         */
        public ComparisonBuilder compare(final double left, final double right, final double tolerance) {
            if (result == 0) {
                result = Numbers.fuzzyCompare(left, right, tolerance);
            }

            return this;
        }

        /**
         * Returns the result of the comparison chain.
         * 
         * <p>The result follows standard comparison conventions:</p>
         * <ul>
         *   <li>Returns 0 if all compared values are equal</li>
         *   <li>Returns a negative value if the first value is less than the second</li>
         *   <li>Returns a positive value if the first value is greater than the second</li>
         * </ul>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * int comparison = ComparisonBuilder.create()
         *     .compare(obj1.getName(), obj2.getName())
         *     .compare(obj1.getAge(), obj2.getAge())
         *     .result();
         *     
         * if (comparison < 0) {
         *     // obj1 is less than obj2
         * } else if (comparison > 0) {
         *     // obj1 is greater than obj2
         * } else {
         *     // obj1 equals obj2
         * }
         * }</pre>
         *
         * @return 0 if all values are equal; a negative value if the first 
         *         differing value is less; a positive value if the first 
         *         differing value is greater
         */
        public int result() {
            return result;
        }
    }

    /**
     * A builder class for performing chained equality comparisons between objects.
     * This class follows the builder pattern to allow multiple equality checks to be
     * chained together, with the result being true only if all comparisons are equal.
     * 
     * <p>The equivalence chain short-circuits on the first non-equal comparison,
     * making it efficient for comparing objects with multiple fields. Once a 
     * false result is found, subsequent comparisons are skipped.</p>
     * 
     * <p><strong>Thread Safety:</strong> This class is not thread-safe and should
     * not be shared between threads.</p>
     * 
     * <p><strong>Example usage:</strong></p>
     * <pre>{@code
     * boolean equal = Builder
     *     .equals(person1.getFirstName(), person2.getFirstName())
     *     .equals(person1.getLastName(), person2.getLastName())
     *     .equals(person1.getAge(), person2.getAge())
     *     .result();
     * }</pre>
     * 
     * @author HaiYang Li
     * @since 0.8
     */
    public static final class EquivalenceBuilder {

        /** The result. */
        private boolean result = true;

        /**
         * Instantiates a new equivalence chain.
         */
        private EquivalenceBuilder() {
            // singleton.
        }

        /**
         * Compares two objects for equality using {@link N#equals(Object, Object)}.
         * If the result of this equivalence chain has not already been determined
         * to be false, this method checks if the two objects are equal.
         * 
         * <p>This method handles null values gracefully, considering two null
         * values as equal.</p>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * EquivalenceBuilder.create()
         *     .equals("hello", "hello")
         *     .equals(null, null)
         *     .result(); // returns true
         * }</pre>
         *
         * @param left the first object to compare, may be null
         * @param right the second object to compare, may be null
         * @return this EquivalenceBuilder instance for method chaining
         */
        public EquivalenceBuilder equals(final Object left, final Object right) {
            if (result) {
                result = N.equals(left, right);
            }

            return this;
        }

        /**
         * Compares two objects for equality using a custom function.
         * If the result of this equivalence chain has not already been determined
         * to be false, this method uses the provided function to check equality.
         * 
         * <p>This allows for custom equality logic beyond standard equals() method.</p>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * EquivalenceBuilder.create()
         *     .equals(str1, str2, (a, b) -> a.equalsIgnoreCase(b))
         *     .result();
         * }</pre>
         *
         * @param <T> the type of objects being compared
         * @param left the first object to compare
         * @param right the second object to compare
         * @param predicate the predicate to check equality, must not be null
         * @return this EquivalenceBuilder instance for method chaining
         * @throws IllegalArgumentException if predicate is null
         */
        public <T> EquivalenceBuilder equals(final T left, final T right, final BiPredicate<? super T, ? super T> predicate) throws IllegalArgumentException {
            N.checkArgNotNull(predicate, cs.predicate);

            if (result) {
                result = predicate.test(left, right);
            }

            return this;
        }

        /**
         * Compares two boolean values for equality.
         * If the result of this equivalence chain has not already been determined
         * to be false, this method checks if the two boolean values are equal.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * EquivalenceBuilder.create()
         *     .equals(true, true)
         *     .equals(false, false)
         *     .result(); // returns true
         * }</pre>
         *
         * @param left the first boolean value
         * @param right the second boolean value
         * @return this EquivalenceBuilder instance for method chaining
         */
        public EquivalenceBuilder equals(final boolean left, final boolean right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two char values for equality.
         * If the result of this equivalence chain has not already been determined
         * to be false, this method checks if the two char values are equal.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * EquivalenceBuilder.create()
         *     .equals('a', 'a')
         *     .equals('X', 'X')
         *     .result(); // returns true
         * }</pre>
         *
         * @param left the first char value
         * @param right the second char value
         * @return this EquivalenceBuilder instance for method chaining
         */
        public EquivalenceBuilder equals(final char left, final char right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two byte values for equality.
         * If the result of this equivalence chain has not already been determined
         * to be false, this method checks if the two byte values are equal.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * EquivalenceBuilder.create()
         *     .equals((byte)10, (byte)10)
         *     .result(); // returns true
         * }</pre>
         *
         * @param left the first byte value
         * @param right the second byte value
         * @return this EquivalenceBuilder instance for method chaining
         */
        public EquivalenceBuilder equals(final byte left, final byte right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two short values for equality.
         * If the result of this equivalence chain has not already been determined
         * to be false, this method checks if the two short values are equal.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * EquivalenceBuilder.create()
         *     .equals((short)100, (short)100)
         *     .result(); // returns true
         * }</pre>
         *
         * @param left the first short value
         * @param right the second short value
         * @return this EquivalenceBuilder instance for method chaining
         */
        public EquivalenceBuilder equals(final short left, final short right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two int values for equality.
         * If the result of this equivalence chain has not already been determined
         * to be false, this method checks if the two int values are equal.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * EquivalenceBuilder.create()
         *     .equals(42, 42)
         *     .equals(100, 100)
         *     .result(); // returns true
         * }</pre>
         *
         * @param left the first int value
         * @param right the second int value
         * @return this EquivalenceBuilder instance for method chaining
         */
        public EquivalenceBuilder equals(final int left, final int right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two long values for equality.
         * If the result of this equivalence chain has not already been determined
         * to be false, this method checks if the two long values are equal.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * EquivalenceBuilder.create()
         *     .equals(1000L, 1000L)
         *     .result(); // returns true
         * }</pre>
         *
         * @param left the first long value
         * @param right the second long value
         * @return this EquivalenceBuilder instance for method chaining
         */
        public EquivalenceBuilder equals(final long left, final long right) {
            if (result) {
                result = left == right;
            }

            return this;
        }

        /**
         * Compares two float values for equality using {@link Float#compare}.
         * If the result of this equivalence chain has not already been determined
         * to be false, this method checks if the two float values are equal.
         * 
         * <p>This method correctly handles special float values including NaN,
         * positive and negative infinity, and distinguishes between 0.0f and -0.0f.</p>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * EquivalenceBuilder.create()
         *     .equals(1.5f, 1.5f)
         *     .equals(Float.NaN, Float.NaN)
         *     .result(); // returns true
         * }</pre>
         *
         * @param left the first float value
         * @param right the second float value
         * @return this EquivalenceBuilder instance for method chaining
         */
        public EquivalenceBuilder equals(final float left, final float right) {
            if (result) {
                result = Float.compare(left, right) == 0;
            }

            return this;
        }

        /**
         * Compares two float values for equality using fuzzy comparison with the given tolerance.
         * This method is useful for comparing floating-point numbers that may have
         * small differences due to rounding errors or precision limitations.
         * 
         * <p>Values are considered equal if their absolute difference is less than
         * or equal to the specified tolerance.</p>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * EquivalenceBuilder.create()
         *     .equals(1.0001f, 1.0002f, 0.001f)
         *     .result(); // returns true
         * }</pre>
         *
         * @param left the first float to compare
         * @param right the second float to compare
         * @param tolerance the maximum difference allowed for the values to be considered equal
         * @return this EquivalenceBuilder instance for method chaining
         */
        public EquivalenceBuilder equals(final float left, final float right, final float tolerance) {
            if (result) {
                result = Numbers.fuzzyEquals(left, right, tolerance);
            }

            return this;
        }

        /**
         * Compares two double values for equality using {@link Double#compare}.
         * If the result of this equivalence chain has not already been determined
         * to be false, this method checks if the two double values are equal.
         * 
         * <p>This method correctly handles special double values including NaN,
         * positive and negative infinity, and distinguishes between 0.0d and -0.0d.</p>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * EquivalenceBuilder.create()
         *     .equals(1.5, 1.5)
         *     .equals(Double.NaN, Double.NaN)
         *     .result(); // returns true
         * }</pre>
         *
         * @param left the first double value
         * @param right the second double value
         * @return this EquivalenceBuilder instance for method chaining
         */
        public EquivalenceBuilder equals(final double left, final double right) {
            if (result) {
                result = Double.compare(left, right) == 0;
            }

            return this;
        }

        /**
         * Compares two double values for equality using fuzzy comparison with the given tolerance.
         * This method is useful for comparing floating-point numbers that may have
         * small differences due to rounding errors or precision limitations.
         * 
         * <p>Values are considered equal if their absolute difference is less than
         * or equal to the specified tolerance.</p>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * EquivalenceBuilder.create()
         *     .equals(1.00001, 1.00002, 0.0001)
         *     .result(); // returns true
         * }</pre>
         *
         * @param left the first double to compare
         * @param right the second double to compare
         * @param tolerance the maximum difference allowed for the values to be considered equal
         * @return this EquivalenceBuilder instance for method chaining
         */
        public EquivalenceBuilder equals(final double left, final double right, final double tolerance) {
            if (result) {
                result = Numbers.fuzzyEquals(left, right, tolerance);
            }

            return this;
        }

        /**
         * Returns the result of the equivalence chain.
         * 
         * <p>Returns true if and only if all comparisons in the chain returned true.
         * If any comparison returned false, this method returns false.</p>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * boolean areEqual = EquivalenceBuilder.create()
         *     .equals(obj1.getField1(), obj2.getField1())
         *     .equals(obj1.getField2(), obj2.getField2())
         *     .result();
         *     
         * if (areEqual) {
         *     // All fields are equal
         * }
         * }</pre>
         * 
         * @return {@code true} if all values compared are equal, {@code false} otherwise
         */
        public boolean result() {
            return result;
        }

        /**
         * Returns the result of the equivalence chain.
         * This method is an alias for {@link #result()} and provides a more descriptive
         * name for checking equality results.
         *
         * <p>Returns true if and only if all comparisons in the chain returned true.
         * If any comparison returned false, this method returns false.</p>
         *
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * boolean areEqual = Builder.equals(obj1.getName(), obj2.getName())
         *                           .equals(obj1.getAge(), obj2.getAge())
         *                           .isEquals();
         *
         * if (areEqual) {
         *     // Objects are considered equal based on compared fields
         * }
         * }</pre>
         *
         * @return {@code true} if all values compared are equal, {@code false} otherwise
         * @see #result()
         */
        public boolean isEquals() {
            return result;
        }
    }

    /**
     * A builder class for computing hash codes using the standard Java hash code algorithm.
     * This class follows the builder pattern to allow multiple values to be
     * incorporated into a single hash code calculation.
     * 
     * <p>The hash code is computed using the standard Java convention where each
     * value's hash code is multiplied by 31 and added to the running total. This
     * provides good distribution of hash values for use in hash-based collections.</p>
     * 
     * <p><strong>Thread Safety:</strong> This class is not thread-safe and should
     * not be shared between threads.</p>
     * 
     * <p><strong>Example usage:</strong></p>
     * <pre>{@code
     * int hashCode = Builder
     *     .hash(firstName)
     *     .hash(lastName)
     *     .hash(age)
     *     .result();
     * }</pre>
     * 
     * @author HaiYang Li
     * @since 0.8
     */
    public static final class HashCodeBuilder {

        /** The result. */
        private int result = 0;

        /**
         * Instantiates a new hash code chain.
         */
        private HashCodeBuilder() {
            // singleton.
        }

        /**
         * Adds the hash code of the specified object to the running hash code.
         * The object's hash code is computed using {@link N#hashCode(Object)},
         * which handles null values gracefully (returning 0 for null).
         * 
         * <p>The hash code is updated using the formula: 
         * {@code result = result * 31 + hashCode(value)}</p>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * HashCodeBuilder.create()
         *     .hash("Hello")
         *     .hash(null)
         *     .hash(42)
         *     .result();
         * }</pre>
         *
         * @param value the object whose hash code should be added, may be null
         * @return this HashCodeBuilder instance for method chaining
         */
        public HashCodeBuilder hash(final Object value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Adds a custom hash code for the specified value using the provided function.
         * This allows for custom hash code calculations beyond the standard
         * Object.hashCode() method.
         * 
         * <p>The hash code is updated using the formula: 
         * {@code result = result * 31 + func.applyAsInt(value)}</p>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * HashCodeBuilder.create()
         *     .hash(person, p -> p.getId())
         *     .hash(name, String::length)
         *     .result();
         * }</pre>
         *
         * @param <T> the type of the value
         * @param value the value to hash
         * @param func the function to compute the hash code, must not be null
         * @return this HashCodeBuilder instance for method chaining
         * @throws IllegalArgumentException if func is null
         */
        public <T> HashCodeBuilder hash(final T value, final ToIntFunction<? super T> func) throws IllegalArgumentException {
            N.checkArgNotNull(func, cs.func);

            result = result * 31 + func.applyAsInt(value);

            return this;
        }

        /**
         * Adds the hash code of a boolean value to the running hash code.
         * True is mapped to 1231 and false to 1237, following Java conventions.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * HashCodeBuilder.create()
         *     .hash(true)
         *     .hash(false)
         *     .result();
         * }</pre>
         *
         * @param value the boolean value to hash
         * @return this HashCodeBuilder instance for method chaining
         */
        public HashCodeBuilder hash(final boolean value) {
            result = result * 31 + (value ? 1231 : 1237);

            return this;
        }

        /**
         * Adds the hash code of a char value to the running hash code.
         * The char value is used directly as its hash code.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * HashCodeBuilder.create()
         *     .hash('A')
         *     .hash('Z')
         *     .result();
         * }</pre>
         *
         * @param value the char value to hash
         * @return this HashCodeBuilder instance for method chaining
         */
        public HashCodeBuilder hash(final char value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Adds the hash code of a byte value to the running hash code.
         * The byte value is used directly as its hash code.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * HashCodeBuilder.create()
         *     .hash((byte)10)
         *     .hash((byte)20)
         *     .result();
         * }</pre>
         *
         * @param value the byte value to hash
         * @return this HashCodeBuilder instance for method chaining
         */
        public HashCodeBuilder hash(final byte value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Adds the hash code of a short value to the running hash code.
         * The short value is used directly as its hash code.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * HashCodeBuilder.create()
         *     .hash((short)100)
         *     .hash((short)200)
         *     .result();
         * }</pre>
         *
         * @param value the short value to hash
         * @return this HashCodeBuilder instance for method chaining
         */
        public HashCodeBuilder hash(final short value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Adds the hash code of an int value to the running hash code.
         * The int value is used directly as its hash code.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * HashCodeBuilder.create()
         *     .hash(42)
         *     .hash(100)
         *     .result();
         * }</pre>
         *
         * @param value the int value to hash
         * @return this HashCodeBuilder instance for method chaining
         */
        public HashCodeBuilder hash(final int value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Adds the hash code of a long value to the running hash code.
         * The hash code is computed as {@code (int)(value ^ (value >>> 32))}.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * HashCodeBuilder.create()
         *     .hash(1000L)
         *     .hash(2000L)
         *     .result();
         * }</pre>
         *
         * @param value the long value to hash
         * @return this HashCodeBuilder instance for method chaining
         */
        public HashCodeBuilder hash(final long value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Adds the hash code of a float value to the running hash code.
         * The hash code is computed using {@link Float#floatToIntBits(float)}.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * HashCodeBuilder.create()
         *     .hash(1.5f)
         *     .hash(2.5f)
         *     .result();
         * }</pre>
         *
         * @param value the float value to hash
         * @return this HashCodeBuilder instance for method chaining
         */
        public HashCodeBuilder hash(final float value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Adds the hash code of a double value to the running hash code.
         * The hash code is computed using {@link Double#doubleToLongBits(double)}
         * and then converting to int.
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * HashCodeBuilder.create()
         *     .hash(1.5)
         *     .hash(2.5)
         *     .result();
         * }</pre>
         *
         * @param value the double value to hash
         * @return this HashCodeBuilder instance for method chaining
         */
        public HashCodeBuilder hash(final double value) {
            result = result * 31 + N.hashCode(value);

            return this;
        }

        /**
         * Returns the computed hash code.
         * 
         * <p>The hash code is computed using the standard Java algorithm where
         * each value's contribution is multiplied by 31 and added to the total.
         * This provides good distribution for use in hash-based collections.</p>
         * 
         * <p><strong>Example:</strong></p>
         * <pre>{@code
         * @Override
         * public int hashCode() {
         *     return HashCodeBuilder.create()
         *         .hash(field1)
         *         .hash(field2)
         *         .hash(field3)
         *         .result();
         * }
         * }</pre>
         *
         * @return the computed hash code
         */
        public int result() {
            return result;
        }
    }

    //    /**
    //     * An experimental extension of the Builder class marked with the {@code @Beta} annotation.
    //     * This class provides the same functionality as Builder but indicates that it may be
    //     * subject to change in future versions.
    //     * 
    //     * <p><strong>WARNING:</strong> This class is marked as {@code @Beta} and should be used
    //     * with caution in production code as its API may change without notice.</p>
    //     * 
    //     * <p><strong>Example usage:</strong></p>
    //     * <pre>{@code
    //     * List<String> result = X.of(new ArrayList<String>())
    //     *     .add("Hello")
    //     *     .add("World")
    //     *     .val();
    //     * }</pre>
    //     *
    //     * @param <T> The type of the value this X builder holds
    //     * @author HaiYang Li
    //     * @since 0.8
    //     */
    //    @Beta
    //    public static final class X<T> extends Builder<T> {
    //
    //        /**
    //         * Instantiates a new X builder with the specified value.
    //         * This constructor is private to enforce the use of factory methods.
    //         *
    //         * @param val the initial value to wrap in this builder
    //         */
    //        private X(final T val) { //NOSONAR
    //            super(val);
    //        }
    //    }
}
