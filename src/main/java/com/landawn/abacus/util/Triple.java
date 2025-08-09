/*
 * Copyright (c) 2015, Haiyang Li.
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

import com.landawn.abacus.annotation.Beta;
import com.landawn.abacus.annotation.SuppressFBWarnings;
import com.landawn.abacus.util.Tuple.Tuple3;
import com.landawn.abacus.util.u.Optional;

/**
 * A container class that holds three values of potentially different types.
 * This class is mutable and provides various utility methods for accessing and modifying
 * the three elements (left, middle, and right).
 * 
 * <p>Triple is commonly used to return multiple values from a method or to group
 * three related values together without creating a dedicated class.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * Triple<String, Integer, Boolean> triple = Triple.of("Hello", 42, true);
 * String left = triple.left();     // "Hello"
 * Integer middle = triple.middle(); // 42
 * Boolean right = triple.right();   // true
 * }</pre>
 *
 * @param <L> the type of the left element
 * @param <M> the type of the middle element
 * @param <R> the type of the right element
 * @see com.landawn.abacus.util.u.Optional
 * @see com.landawn.abacus.util.u.Nullable
 * @see com.landawn.abacus.util.Holder
 * @see com.landawn.abacus.util.Result
 * @see com.landawn.abacus.util.Pair
 * @see com.landawn.abacus.util.Tuple
 */
@SuppressFBWarnings("PA_PUBLIC_PRIMITIVE_ATTRIBUTE")
public final class Triple<L, M, R> implements Mutable {

    private L left; //NOSONAR

    private M middle; //NOSONAR

    private R right; //NOSONAR

    /**
     * Constructs an empty Triple with all elements set to null.
     * This constructor is typically used when the values will be set later
     * using the setter methods.
     */
    public Triple() {
    }

    /**
     * Constructs a Triple with the specified left, middle, and right values.
     * 
     * @param l the left element value
     * @param m the middle element value
     * @param r the right element value
     */
    Triple(final L l, final M m, final R r) {
        setLeft(l);
        setMiddle(m);
        setRight(r);
    }

    /**
     * Creates a new Triple instance with the specified left, middle, and right values.
     * This is the preferred way to create a Triple instance.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("text", 100, false);
     * }</pre>
     *
     * @param <L> the type of the left element
     * @param <M> the type of the middle element
     * @param <R> the type of the right element
     * @param l the left element value (can be null)
     * @param m the middle element value (can be null)
     * @param r the right element value (can be null)
     * @return a new Triple instance containing the specified values
     */
    public static <L, M, R> Triple<L, M, R> of(final L l, final M m, final R r) {
        return new Triple<>(l, m, r);
    }

    private static final Triple<?, ?, ?>[] EMPTY_ARRAY = new Triple[0];

    /**
     * Returns an empty array of Triple. This method is useful when you need to
     * return an empty array of Triple instances without creating a new array each time.
     * The returned array is immutable and shared across all calls.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean>[] emptyTriples = Triple.emptyArray();
     * // emptyTriples.length == 0
     * }</pre>
     *
     * @param <L> the type of the left element
     * @param <M> the type of the middle element
     * @param <R> the type of the right element
     * @return an empty, immutable array of Triple
     */
    @SuppressWarnings("unchecked")
    public static <L, M, R> Triple<L, M, R>[] emptyArray() {
        return (Triple<L, M, R>[]) EMPTY_ARRAY;
    }

    /**
     * Returns the left element of this Triple.
     * This is the preferred method for accessing the left element.
     *
     * @return the left element, which may be null
     */
    public L left() {
        return left;
    }

    /**
     * Returns the middle element of this Triple.
     * This is the preferred method for accessing the middle element.
     *
     * @return the middle element, which may be null
     */
    public M middle() {
        return middle;
    }

    /**
     * Returns the right element of this Triple.
     * This is the preferred method for accessing the right element.
     *
     * @return the right element, which may be null
     */
    public R right() {
        return right;
    }

    /**
     * Returns the left element of this Triple.
     * This method provides JavaBean-style access to the left element.
     * 
     * @return the left element, which may be null
     * @deprecated This method is deprecated in favor of the more concise {@link #left()} method.
     * @see #left()
     */
    @Deprecated
    public L getLeft() {
        return left;
    }

    /**
     * Sets the left element of this Triple to the specified value.
     * This method allows modification of the left element after the Triple has been created.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("old", 42, true);
     * triple.setLeft("new");
     * // triple.left() now returns "new"
     * }</pre>
     *
     * @param left the new value for the left element (can be null)
     */
    public void setLeft(final L left) {
        this.left = left;
    }

    /**
     * Returns the middle element of this Triple.
     * This method provides JavaBean-style access to the middle element.
     * 
     * @return the middle element, which may be null
     * @deprecated This method is deprecated in favor of the more concise {@link #middle()} method.
     * @see #middle()
     */
    @Deprecated
    public M getMiddle() {
        return middle;
    }

    /**
     * Sets the middle element of this Triple to the specified value.
     * This method allows modification of the middle element after the Triple has been created.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("text", 42, true);
     * triple.setMiddle(100);
     * // triple.middle() now returns 100
     * }</pre>
     *
     * @param middle the new value for the middle element (can be null)
     */
    public void setMiddle(final M middle) {
        this.middle = middle;
    }

    /**
     * Returns the right element of this Triple.
     * This method provides JavaBean-style access to the right element.
     * 
     * @return the right element, which may be null
     * @deprecated This method is deprecated in favor of the more concise {@link #right()} method.
     * @see #right()
     */
    @Deprecated
    public R getRight() {
        return right;
    }

    /**
     * Sets the right element of this Triple to the specified value.
     * This method allows modification of the right element after the Triple has been created.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("text", 42, true);
     * triple.setRight(false);
     * // triple.right() now returns false
     * }</pre>
     *
     * @param right the new value for the right element (can be null)
     */
    public void setRight(final R right) {
        this.right = right;
    }

    /**
     * Sets all three elements of this Triple to the specified values in a single operation.
     * This is more efficient than calling setLeft, setMiddle, and setRight separately.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("old", 1, true);
     * triple.set("new", 2, false);
     * // triple now contains ("new", 2, false)
     * }</pre>
     *
     * @param left the new value for the left element (can be null)
     * @param middle the new value for the middle element (can be null)
     * @param right the new value for the right element (can be null)
     */
    public void set(final L left, final M middle, final R right) {
        this.setLeft(left);
        this.setMiddle(middle);
        this.setRight(right);
    }

    /**
     * Returns the current value of the left element and then updates it with the specified new value.
     * This method is useful when you need to retrieve the old value while setting a new one
     * in an atomic-like operation.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("old", 42, true);
     * String oldValue = triple.getAndSetLeft("new");
     * // oldValue is "old", triple.left() is now "new"
     * }</pre>
     *
     * @param newLeft the new value to set for the left element (can be null)
     * @return the previous value of the left element
     */
    public L getAndSetLeft(final L newLeft) {
        final L res = left;
        setLeft(newLeft);
        return res;
    }

    /**
     * Sets the left element to the specified new value and then returns the new value.
     * This method is useful when you want to set a value and immediately use it
     * in a fluent style or chain of operations.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("old", 42, true);
     * String newValue = triple.setAndGetLeft("new");
     * // newValue is "new", triple.left() is also "new"
     * }</pre>
     *
     * @param newLeft the new value to set for the left element (can be null)
     * @return the new value of the left element (same as the parameter)
     */
    public L setAndGetLeft(final L newLeft) {
        setLeft(newLeft);
        return left;
    }

    /**
     * Returns the current value of the middle element and then updates it with the specified new value.
     * This method is useful when you need to retrieve the old value while setting a new one
     * in an atomic-like operation.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("text", 42, true);
     * Integer oldValue = triple.getAndSetMiddle(100);
     * // oldValue is 42, triple.middle() is now 100
     * }</pre>
     *
     * @param newMiddle the new value to set for the middle element (can be null)
     * @return the previous value of the middle element
     */
    public M getAndSetMiddle(final M newMiddle) {
        final M res = middle;
        setMiddle(newMiddle);
        return res;
    }

    /**
     * Sets the middle element to the specified new value and then returns the new value.
     * This method is useful when you want to set a value and immediately use it
     * in a fluent style or chain of operations.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("text", 42, true);
     * Integer newValue = triple.setAndGetMiddle(100);
     * // newValue is 100, triple.middle() is also 100
     * }</pre>
     *
     * @param newMiddle the new value to set for the middle element (can be null)
     * @return the new value of the middle element (same as the parameter)
     */
    public M setAndGetMiddle(final M newMiddle) {
        setMiddle(newMiddle);
        return middle;
    }

    /**
     * Returns the current value of the right element and then updates it with the specified new value.
     * This method is useful when you need to retrieve the old value while setting a new one
     * in an atomic-like operation.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("text", 42, true);
     * Boolean oldValue = triple.getAndSetRight(false);
     * // oldValue is true, triple.right() is now false
     * }</pre>
     *
     * @param newRight the new value to set for the right element (can be null)
     * @return the previous value of the right element
     */
    public R getAndSetRight(final R newRight) {
        final R res = right;
        setRight(newRight);
        return res;
    }

    /**
     * Sets the right element to the specified new value and then returns the new value.
     * This method is useful when you want to set a value and immediately use it
     * in a fluent style or chain of operations.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("text", 42, true);
     * Boolean newValue = triple.setAndGetRight(false);
     * // newValue is false, triple.right() is also false
     * }</pre>
     *
     * @param newRight the new value to set for the right element (can be null)
     * @return the new value of the right element (same as the parameter)
     */
    public R setAndGetRight(final R newRight) {
        setRight(newRight);
        return right;
    }

    /**
     * Conditionally sets the left element to the specified new value if the provided predicate returns true.
     * The predicate receives both the current Triple instance and the proposed new left value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("old", 42, true);
     * boolean wasSet = triple.setLeftIf("new", (t, newVal) -> t.left().length() < newVal.length());
     * // wasSet is true if "old".length() < "new".length()
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param newLeft the new value to potentially set for the left element
     * @param predicate a bi-predicate that takes the current Triple and the new left value;
     *                  returns true if the left element should be updated
     * @return true if the left element was updated, false otherwise
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> boolean setLeftIf(final L newLeft, final Throwables.BiPredicate<? super Triple<L, M, R>, ? super L, E> predicate) throws E {
        if (predicate.test(this, newLeft)) {
            setLeft(newLeft);
            return true;
        }

        return false;
    }

    /**
     * Conditionally sets the middle element to the specified new value if the provided predicate returns true.
     * The predicate receives both the current Triple instance and the proposed new middle value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("text", 42, true);
     * boolean wasSet = triple.setMiddleIf(100, (t, newVal) -> newVal > t.middle());
     * // wasSet is true if 100 > 42
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param newMiddle the new value to potentially set for the middle element
     * @param predicate a bi-predicate that takes the current Triple and the new middle value;
     *                  returns true if the middle element should be updated
     * @return true if the middle element was updated, false otherwise
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> boolean setMiddleIf(final M newMiddle, final Throwables.BiPredicate<? super Triple<L, M, R>, ? super M, E> predicate)
            throws E {
        if (predicate.test(this, newMiddle)) {
            setMiddle(newMiddle);
            return true;
        }

        return false;
    }

    /**
     * Conditionally sets the right element to the specified new value if the provided predicate returns true.
     * The predicate receives both the current Triple instance and the proposed new right value.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("text", 42, true);
     * boolean wasSet = triple.setRightIf(false, (t, newVal) -> t.right() != newVal);
     * // wasSet is true because true != false
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param newRight the new value to potentially set for the right element
     * @param predicate a bi-predicate that takes the current Triple and the new right value;
     *                  returns true if the right element should be updated
     * @return true if the right element was updated, false otherwise
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> boolean setRightIf(final R newRight, final Throwables.BiPredicate<? super Triple<L, M, R>, ? super R, E> predicate) throws E {
        if (predicate.test(this, newRight)) {
            setRight(newRight);
            return true;
        }

        return false;
    }

    /**
     * Conditionally sets all three elements to the specified new values if the provided predicate returns true.
     * The predicate receives the current Triple instance and all three proposed new values.
     * If the predicate returns true, all three elements are updated; otherwise, no changes are made.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("old", 42, true);
     * boolean wasSet = triple.setIf("new", 100, false, 
     *     (t, l, m, r) -> l.length() > t.left().length() && m > t.middle());
     * // Updates all values if "new".length() > "old".length() AND 100 > 42
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param newLeft the new value to potentially set for the left element
     * @param newMiddle the new value to potentially set for the middle element
     * @param newRight the new value to potentially set for the right element
     * @param predicate a quad-predicate that takes the current Triple and the three new values;
     *                  returns true if all elements should be updated
     * @return true if all elements were updated, false otherwise
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> boolean setIf(final L newLeft, final M newMiddle, final R newRight,
            final Throwables.QuadPredicate<? super Triple<L, M, R>, ? super L, ? super M, ? super R, E> predicate) throws E {
        if (predicate.test(this, newLeft, newMiddle, newRight)) {
            setLeft(newLeft);
            setMiddle(newMiddle);
            setRight(newRight);
            return true;
        }

        return false;
    }

    //    /**
    //     * Swaps the left and right values. They must be the same type.
    //     */
    //    public void reverse() {
    //        Object tmp = left;
    //        this.left = (L) right;
    //        this.right = (R) tmp;
    //    }

    /**
     * Creates and returns a new Triple with the left and right elements swapped,
     * while keeping the middle element in the same position.
     * The original Triple remains unchanged.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> original = Triple.of("left", 42, true);
     * Triple<Boolean, Integer, String> reversed = original.reverse();
     * // reversed contains (true, 42, "left")
     * // original still contains ("left", 42, true)
     * }</pre>
     *
     * @return a new Triple instance with type Triple&lt;R, M, L&gt; where the left and right
     *         elements are swapped
     */
    @Beta
    public Triple<R, M, L> reverse() {
        return new Triple<>(right, middle, left);
    }

    /**
     * Creates and returns a shallow copy of this Triple.
     * The new Triple contains the same element references as the original
     * (the elements themselves are not cloned).
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> original = Triple.of("text", 42, true);
     * Triple<String, Integer, Boolean> copy = original.copy();
     * // copy contains the same values as original
     * // Modifying copy does not affect original
     * copy.setLeft("modified");
     * // original.left() still returns "text"
     * }</pre>
     *
     * @return a new Triple instance containing the same elements as this Triple
     */
    public Triple<L, M, R> copy() {
        return new Triple<>(left, middle, right);
    }

    /**
     * Converts this Triple into an array containing the three elements in order: left, middle, right.
     * The returned array is of type Object[] and has a length of 3.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("text", 42, true);
     * Object[] array = triple.toArray();
     * // array[0] is "text", array[1] is 42, array[2] is true
     * }</pre>
     *
     * @return a new Object array of length 3 containing the left, middle, and right elements
     */
    public Object[] toArray() {
        return new Object[] { left, middle, right };
    }

    /**
     * Converts this Triple into an array of the specified type, storing the three elements
     * in order: left, middle, right. If the provided array has a length of at least 3,
     * the elements are stored in it; otherwise, a new array of the same type with length 3
     * is created and returned.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, String, String> triple = Triple.of("one", "two", "three");
     * String[] array = triple.toArray(new String[3]);
     * // array[0] is "one", array[1] is "two", array[2] is "three"
     * }</pre>
     *
     * @param <A> the component type of the array
     * @param a the array into which the elements are to be stored, if it has length >= 3;
     *          otherwise, a new array of the same runtime type is allocated
     * @return an array containing the three elements of this Triple
     * @throws ArrayStoreException if the runtime type of the specified array is not a
     *         supertype of the runtime type of the elements in this Triple
     */
    public <A> A[] toArray(A[] a) {
        if (a.length < 3) {
            a = N.copyOf(a, 3);
        }

        a[0] = (A) left;
        a[1] = (A) middle;
        a[2] = (A) right;

        return a;
    }

    /**
     * Applies the given consumer function to each element of this Triple in order:
     * left, middle, then right. This method is useful for performing the same operation
     * on all three elements.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, String, String> triple = Triple.of("one", "two", "three");
     * triple.forEach(System.out::println);
     * // Prints:
     * // one
     * // two
     * // three
     * }</pre>
     *
     * @param <E> the type of exception that the consumer may throw
     * @param consumer the consumer function to apply to each element; must accept
     *                 a common supertype of L, M, and R
     * @throws E if the consumer throws an exception
     */
    public <E extends Exception> void forEach(final Throwables.Consumer<?, E> consumer) throws E {
        final Throwables.Consumer<Object, E> objConsumer = (Throwables.Consumer<Object, E>) consumer;

        objConsumer.accept(left);
        objConsumer.accept(middle);
        objConsumer.accept(right);
    }

    /**
     * Applies the given tri-consumer action to the three elements of this Triple.
     * The action receives the left, middle, and right elements as separate parameters.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("text", 42, true);
     * triple.accept((l, m, r) -> {
     *     System.out.println("Left: " + l);
     *     System.out.println("Middle: " + m);
     *     System.out.println("Right: " + r);
     * });
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the tri-consumer action to apply to the three elements
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void accept(final Throwables.TriConsumer<? super L, ? super M, ? super R, E> action) throws E {
        action.accept(left, middle, right);
    }

    /**
     * Applies the given consumer action to this Triple instance as a whole.
     * The action receives the entire Triple object rather than individual elements.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("text", 42, true);
     * triple.accept(t -> {
     *     System.out.println("Triple: " + t);
     *     // Can access elements via t.left(), t.middle(), t.right()
     * });
     * }</pre>
     *
     * @param <E> the type of exception that the action may throw
     * @param action the consumer action to apply to this Triple
     * @throws E if the action throws an exception
     */
    public <E extends Exception> void accept(final Throwables.Consumer<? super Triple<L, M, R>, E> action) throws E {
        action.accept(this);
    }

    /**
     * Applies the given tri-function to the three elements of this Triple and returns the result.
     * The function receives the left, middle, and right elements as separate parameters.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("Hello", 5, true);
     * String result = triple.map((l, m, r) -> l + " has " + m + " letters: " + r);
     * // result is "Hello has 5 letters: true"
     * }</pre>
     *
     * @param <U> the type of the result
     * @param <E> the type of exception that the mapper may throw
     * @param mapper the tri-function to apply to the three elements
     * @return the result of applying the mapper function
     * @throws E if the mapper throws an exception
     */
    public <U, E extends Exception> U map(final Throwables.TriFunction<? super L, ? super M, ? super R, ? extends U, E> mapper) throws E {
        return mapper.apply(left, middle, right);
    }

    /**
     * Applies the given function to this Triple instance as a whole and returns the result.
     * The function receives the entire Triple object rather than individual elements.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("text", 42, true);
     * String result = triple.map(t -> 
     *     String.format("(%s, %d, %b)", t.left(), t.middle(), t.right())
     * );
     * // result is "(text, 42, true)"
     * }</pre>
     *
     * @param <U> the type of the result
     * @param <E> the type of exception that the mapper may throw
     * @param mapper the function to apply to this Triple
     * @return the result of applying the mapper function
     * @throws E if the mapper throws an exception
     */
    public <U, E extends Exception> U map(final Throwables.Function<? super Triple<L, M, R>, ? extends U, E> mapper) throws E {
        return mapper.apply(this);
    }

    /**
     * Returns an Optional containing this Triple if the given tri-predicate returns true
     * when applied to the three elements, otherwise returns an empty Optional.
     * The predicate receives the left, middle, and right elements as separate parameters.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("text", 42, true);
     * Optional<Triple<String, Integer, Boolean>> filtered = 
     *     triple.filter((l, m, r) -> l.length() == 4 && m > 40 && r);
     * // filtered contains the triple because all conditions are met
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the tri-predicate to test the three elements
     * @return an Optional containing this Triple if the predicate returns true,
     *         otherwise an empty Optional
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> Optional<Triple<L, M, R>> filter(final Throwables.TriPredicate<? super L, ? super M, ? super R, E> predicate) throws E {
        return predicate.test(left, middle, right) ? Optional.of(this) : Optional.empty();
    }

    /**
     * Returns an Optional containing this Triple if the given predicate returns true
     * when applied to this Triple instance, otherwise returns an empty Optional.
     * The predicate receives the entire Triple object rather than individual elements.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("text", 42, true);
     * Optional<Triple<String, Integer, Boolean>> filtered = 
     *     triple.filter(t -> t.left().startsWith("t") && t.middle() % 2 == 0);
     * // filtered contains the triple because both conditions are met
     * }</pre>
     *
     * @param <E> the type of exception that the predicate may throw
     * @param predicate the predicate to test this Triple
     * @return an Optional containing this Triple if the predicate returns true,
     *         otherwise an empty Optional
     * @throws E if the predicate throws an exception
     */
    public <E extends Exception> Optional<Triple<L, M, R>> filter(final Throwables.Predicate<? super Triple<L, M, R>, E> predicate) throws E {
        return predicate.test(this) ? Optional.of(this) : Optional.empty();
    }

    //    /**
    //     *
    //     *
    //     * @return
    //     * @deprecated {@code Optional} is misused. It's marked to be removed.
    //     */
    //    @Deprecated
    //    @Beta
    //    public Stream<Triple<L, M, R>> stream() {
    //        return Stream.of(this);
    //    }
    //
    //    /**
    //     *
    //     *
    //     * @param <T>
    //     * @param <E>
    //     * @param func
    //     * @return
    //     * @throws E
    //     * @deprecated {@code Optional} is misused. It's marked to be removed.
    //     */
    //    @Deprecated
    //    @Beta
    //    public <T, E extends Exception> Stream<T> stream(final Throwables.Function<? super Triple<L, M, R>, Stream<T>, E> func) throws E {
    //        return func.apply(this);
    //    }
    //
    //    /**
    //     *
    //     *
    //     * @return
    //     * @deprecated {@code Optional} is misused. It's marked to be removed.
    //     */
    //    @Deprecated
    //    public Optional<Triple<L, M, R>> toOptional() {
    //        return Optional.of(this);
    //    }

    /**
     * Converts this Triple to a Tuple3 with the same elements.
     * Tuple3 is another three-element container type that may have different
     * characteristics or API compared to Triple.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("text", 42, true);
     * Tuple3<String, Integer, Boolean> tuple = triple.toTuple();
     * // tuple contains the same elements as triple
     * }</pre>
     *
     * @return a new Tuple3 instance containing the same elements as this Triple
     */
    public Tuple3<L, M, R> toTuple() {
        return Tuple.of(left, middle, right);
    }

    /**
     * Returns a hash code value for this Triple.
     * The hash code is calculated based on the hash codes of the three elements.
     * Two Triple objects with equal elements (as determined by their equals methods)
     * will have the same hash code.
     *
     * @return a hash code value for this Triple
     */
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + N.hashCode(left);
        result = prime * result + N.hashCode(middle);
        return prime * result + N.hashCode(right);
    }

    /**
     * Compares this Triple with the specified object for equality.
     * Returns true if and only if the specified object is also a Triple
     * and both Triples have equal left, middle, and right elements.
     * Element equality is determined using the N.equals utility method,
     * which handles null values correctly.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> t1 = Triple.of("text", 42, true);
     * Triple<String, Integer, Boolean> t2 = Triple.of("text", 42, true);
     * Triple<String, Integer, Boolean> t3 = Triple.of("other", 42, true);
     * 
     * t1.equals(t2); // returns true
     * t1.equals(t3); // returns false
     * t1.equals(null); // returns false
     * }</pre>
     *
     * @param obj the object to compare with this Triple for equality
     * @return true if the specified object is a Triple with equal elements,
     *         false otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }

        if (obj instanceof Triple) {
            final Triple<L, M, R> other = (Triple<L, M, R>) obj;

            return N.equals(left, other.left) && N.equals(middle, other.middle) && N.equals(right, other.right);
        }

        return false;
    }

    /**
     * Returns a string representation of this Triple in the format "(left, middle, right)".
     * The string representations of the individual elements are obtained using
     * the N.toString utility method, which handles null values by converting them
     * to the string "null".
     * 
     * <p>Example:</p>
     * <pre>{@code
     * Triple<String, Integer, Boolean> triple = Triple.of("text", 42, true);
     * System.out.println(triple);  // prints: (text, 42, true)
     * 
     * Triple<String, Integer, Boolean> nullTriple = Triple.of(null, null, null);
     * System.out.println(nullTriple);  // prints: (null, null, null)
     * }</pre>
     *
     * @return a string representation of this Triple
     */
    @Override
    public String toString() {
        return "(" + N.toString(left) + ", " + N.toString(middle) + ", " + N.toString(right) + ")"; // To align with Pair.toString() in Apache Commons Lang
    }
}