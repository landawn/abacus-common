/*
 * Copyright (C) 2017 HaiYang Li
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

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;

import com.landawn.abacus.annotation.Beta;

/**
 * A functional programming utility class for creating fluent conditional execution chains.
 * 
 * <p>This class provides a fluent API for conditional logic, offering an alternative to traditional
 * if-else statements and ternary operators. It's particularly useful in functional programming contexts
 * where method chaining is preferred.</p>
 * 
 * <p><strong>Note:</strong> While this class provides a functional approach, traditional if-else statements
 * or ternary operators are generally preferred for better readability and performance.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * // Basic condition checking
 * If.is(x > 0)
 *   .then(() -> System.out.println("Positive"))
 *   .orElse(() -> System.out.println("Non-positive"));
 * 
 * // Null checking with action
 * If.notNull(user)
 *   .then(u -> processUser(u))
 *   .orElse(() -> handleNullUser());
 * 
 * // Collection checking
 * If.notEmpty(list)
 *   .then(() -> processList(list))
 *   .orElseThrow(() -> new IllegalStateException("List is empty"));
 * }</pre>
 *
 * @see N#ifOrEmpty(boolean, Throwables.Supplier)
 * @see N#ifOrElse(boolean, Throwables.Runnable, Throwables.Runnable)
 * @see N#ifNotNull(Object, Throwables.Consumer)
 * @see N#ifNotEmpty(CharSequence, Throwables.Consumer)
 * @see N#ifNotEmpty(Collection, Throwables.Consumer)
 * @see N#ifNotEmpty(Map, Throwables.Consumer)
 */
@Beta
public final class If {

    private static final If TRUE = new If(true);

    private static final If FALSE = new If(false);

    private final boolean b;

    If(final boolean b) {
        this.b = b;
    }

    /**
     * Creates an If instance based on the given boolean condition.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.is(temperature > 30)
     *   .then(() -> System.out.println("It's hot!"));
     * }</pre>
     *
     * @param b the boolean condition to evaluate
     * @return an If instance representing the condition
     */
    public static If is(final boolean b) {
        return b ? TRUE : FALSE;
    }

    /**
     * Creates an If instance with the negation of the given boolean condition.
     * 
     * <p>This is equivalent to {@code is(!b)} but can be more readable in certain contexts.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.not(list.isEmpty())
     *   .then(() -> processList(list));
     * }</pre>
     *
     * @param b the boolean condition to negate
     * @return an If instance representing the negated condition
     */
    public static If not(final boolean b) {
        return b ? FALSE : TRUE;
    }

    /**
     * Creates an If instance that checks if an index is valid (non-negative).
     * 
     * <p>Returns {@code true} for {@code index >= 0}, {@code false} for {@code index < 0}.
     * This is commonly used for checking the result of indexOf operations.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.exists(list.indexOf(element))
     *   .then(() -> System.out.println("Element found"))
     *   .orElse(() -> System.out.println("Element not found"));
     * }</pre>
     *
     * @param index the index value to check
     * @return an If instance that is true if the index is non-negative
     */
    public static If exists(final int index) {
        return index >= 0 ? TRUE : FALSE;
    }

    /**
     * Creates an If instance that checks if the given object is null.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.isNull(result)
     *   .then(() -> handleNullResult())
     *   .orElse(() -> processResult(result));
     * }</pre>
     *
     * @param obj the object to check for null
     * @return an If instance that is true if the object is null
     */
    public static If isNull(final Object obj) {
        return is(obj == null);
    }

    /**
     * Creates an If instance that checks if the given CharSequence is null or empty.
     * 
     * <p>A CharSequence is considered empty if it is null or has zero length.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.isEmpty(username)
     *   .then(() -> System.out.println("Username is required"));
     * }</pre>
     *
     * @param s the CharSequence to check
     * @return an If instance that is true if the CharSequence is null or empty
     */
    public static If isEmpty(final CharSequence s) {
        return is(Strings.isEmpty(s));
    }

    /**
     * Creates an If instance that checks if the given boolean array is null or empty.
     *
     * @param a the boolean array to check
     * @return an If instance that is true if the array is null or has zero length
     */
    public static If isEmpty(final boolean[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given char array is null or empty.
     *
     * @param a the char array to check
     * @return an If instance that is true if the array is null or has zero length
     */
    public static If isEmpty(final char[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given byte array is null or empty.
     *
     * @param a the byte array to check
     * @return an If instance that is true if the array is null or has zero length
     */
    public static If isEmpty(final byte[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given short array is null or empty.
     *
     * @param a the short array to check
     * @return an If instance that is true if the array is null or has zero length
     */
    public static If isEmpty(final short[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given int array is null or empty.
     *
     * @param a the int array to check
     * @return an If instance that is true if the array is null or has zero length
     */
    public static If isEmpty(final int[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given long array is null or empty.
     *
     * @param a the long array to check
     * @return an If instance that is true if the array is null or has zero length
     */
    public static If isEmpty(final long[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given float array is null or empty.
     *
     * @param a the float array to check
     * @return an If instance that is true if the array is null or has zero length
     */
    public static If isEmpty(final float[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given double array is null or empty.
     *
     * @param a the double array to check
     * @return an If instance that is true if the array is null or has zero length
     */
    public static If isEmpty(final double[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given object array is null or empty.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.isEmpty(args)
     *   .then(() -> System.out.println("No arguments provided"));
     * }</pre>
     *
     * @param a the object array to check
     * @return an If instance that is true if the array is null or has zero length
     */
    public static If isEmpty(final Object[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given Collection is null or empty.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.isEmpty(resultList)
     *   .then(() -> System.out.println("No results found"))
     *   .orElse(() -> displayResults(resultList));
     * }</pre>
     *
     * @param c the Collection to check
     * @return an If instance that is true if the Collection is null or empty
     */
    public static If isEmpty(final Collection<?> c) {
        return is(N.isEmpty(c));
    }

    /**
     * Creates an If instance that checks if the given Map is null or empty.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.isEmpty(configMap)
     *   .then(() -> loadDefaultConfig());
     * }</pre>
     *
     * @param m the Map to check
     * @return an If instance that is true if the Map is null or empty
     */
    public static If isEmpty(final Map<?, ?> m) {
        return is(N.isEmpty(m));
    }

    /**
     * Creates an If instance that checks if the given PrimitiveList is null or empty.
     *
     * @param list the PrimitiveList to check
     * @return an If instance that is true if the PrimitiveList is null or empty
     */
    @SuppressWarnings("rawtypes")
    public static If isEmpty(final PrimitiveList list) {
        return is(N.isEmpty(list));
    }

    /**
     * Creates an If instance that checks if the given Multiset is null or empty.
     *
     * @param s the Multiset to check
     * @return an If instance that is true if the Multiset is null or empty
     */
    public static If isEmpty(final Multiset<?> s) {
        return is(N.isEmpty(s));
    }

    /**
     * Creates an If instance that checks if the given Multimap is null or empty.
     *
     * @param m the Multimap to check
     * @return an If instance that is true if the Multimap is null or empty
     */
    public static If isEmpty(final Multimap<?, ?, ?> m) {
        return is(N.isEmpty(m));
    }

    /**
     * Creates an If instance that checks if the given CharSequence is null, empty, or contains only whitespace.
     * 
     * <p>A CharSequence is considered blank if it is null, has zero length, or contains only
     * whitespace characters as defined by {@link Character#isWhitespace(char)}.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.isBlank(userInput)
     *   .then(() -> System.out.println("Please enter valid input"));
     * }</pre>
     *
     * @param s the CharSequence to check
     * @return an If instance that is true if the CharSequence is null, empty, or blank
     */
    // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu.
    public static If isBlank(final CharSequence s) {
        return is(Strings.isBlank(s));
    }

    /**
     * Creates an If instance that checks if the given object is not null.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.notNull(user)
     *   .then(u -> System.out.println("User: " + u.getName()))
     *   .orElse(() -> System.out.println("User not found"));
     * }</pre>
     *
     * @param obj the object to check
     * @return an If instance that is true if the object is not null
     */
    public static If notNull(final Object obj) {
        return is(obj != null);
    }

    /**
     * Creates an If instance that checks if the given CharSequence is not null and not empty.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.notEmpty(searchQuery)
     *   .then(() -> performSearch(searchQuery));
     * }</pre>
     *
     * @param s the CharSequence to check
     * @return an If instance that is true if the CharSequence is not null and has length > 0
     */
    public static If notEmpty(final CharSequence s) {
        return is(Strings.isNotEmpty(s));
    }

    /**
     * Creates an If instance that checks if the given boolean array is not null and not empty.
     *
     * @param a the boolean array to check
     * @return an If instance that is true if the array is not null and has length > 0
     */
    public static If notEmpty(final boolean[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given char array is not null and not empty.
     *
     * @param a the char array to check
     * @return an If instance that is true if the array is not null and has length > 0
     */
    public static If notEmpty(final char[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given byte array is not null and not empty.
     *
     * @param a the byte array to check
     * @return an If instance that is true if the array is not null and has length > 0
     */
    public static If notEmpty(final byte[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given short array is not null and not empty.
     *
     * @param a the short array to check
     * @return an If instance that is true if the array is not null and has length > 0
     */
    public static If notEmpty(final short[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given int array is not null and not empty.
     *
     * @param a the int array to check
     * @return an If instance that is true if the array is not null and has length > 0
     */
    public static If notEmpty(final int[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given long array is not null and not empty.
     *
     * @param a the long array to check
     * @return an If instance that is true if the array is not null and has length > 0
     */
    public static If notEmpty(final long[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given float array is not null and not empty.
     *
     * @param a the float array to check
     * @return an If instance that is true if the array is not null and has length > 0
     */
    public static If notEmpty(final float[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given double array is not null and not empty.
     *
     * @param a the double array to check
     * @return an If instance that is true if the array is not null and has length > 0
     */
    public static If notEmpty(final double[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given object array is not null and not empty.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.notEmpty(files)
     *   .then(() -> processFiles(files));
     * }</pre>
     *
     * @param a the object array to check
     * @return an If instance that is true if the array is not null and has length > 0
     */
    public static If notEmpty(final Object[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given Collection is not null and not empty.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.notEmpty(selectedItems)
     *   .then(() -> processSelection(selectedItems))
     *   .orElse(() -> showNoSelectionMessage());
     * }</pre>
     *
     * @param c the Collection to check
     * @return an If instance that is true if the Collection is not null and not empty
     */
    public static If notEmpty(final Collection<?> c) {
        return is(N.notEmpty(c));
    }

    /**
     * Creates an If instance that checks if the given Map is not null and not empty.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.notEmpty(properties)
     *   .then(() -> applyProperties(properties));
     * }</pre>
     *
     * @param m the Map to check
     * @return an If instance that is true if the Map is not null and not empty
     */
    public static If notEmpty(final Map<?, ?> m) {
        return is(N.notEmpty(m));
    }

    /**
     * Creates an If instance that checks if the given PrimitiveList is not null and not empty.
     *
     * @param list the PrimitiveList to check
     * @return an If instance that is true if the PrimitiveList is not null and not empty
     */
    @SuppressWarnings("rawtypes")
    public static If notEmpty(final PrimitiveList list) {
        return is(N.notEmpty(list));
    }

    /**
     * Creates an If instance that checks if the given Multiset is not null and not empty.
     *
     * @param s the Multiset to check
     * @return an If instance that is true if the Multiset is not null and not empty
     */
    public static If notEmpty(final Multiset<?> s) {
        return is(N.notEmpty(s));
    }

    /**
     * Creates an If instance that checks if the given Multimap is not null and not empty.
     *
     * @param m the Multimap to check
     * @return an If instance that is true if the Multimap is not null and not empty
     */
    public static If notEmpty(final Multimap<?, ?, ?> m) {
        return is(N.notEmpty(m));
    }

    /**
     * Creates an If instance that checks if the given CharSequence is not null, not empty, and not blank.
     * 
     * <p>A CharSequence is considered not blank if it is not null, has length > 0, and contains
     * at least one non-whitespace character.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.notBlank(username)
     *   .then(() -> loginUser(username))
     *   .orElse(() -> showError("Username cannot be blank"));
     * }</pre>
     *
     * @param s the CharSequence to check
     * @return an If instance that is true if the CharSequence is not null, not empty, and not blank
     */
    // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu.
    public static If notBlank(final CharSequence s) {
        return is(Strings.isNotBlank(s));
    }

    /**
     * Executes no action if the condition is true, but allows chaining to an orElse clause.
     * 
     * <p>This method is useful when you only want to execute an action in the false case.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.is(cache.contains(key))
     *   .thenDoNothing()
     *   .orElse(() -> cache.load(key));
     * }</pre>
     *
     * @return an OrElse instance for chaining the else clause
     */
    public OrElse thenDoNothing() {
        return OrElse.of(b);
    }

    /**
     * Executes the given runnable if the condition is true.
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.is(debugMode)
     *   .then(() -> logger.debug("Debug information"))
     *   .orElse(() -> logger.info("Normal operation"));
     * }</pre>
     *
     * @param <E> the type of exception that the runnable may throw
     * @param cmd the runnable to execute if the condition is true
     * @return an OrElse instance for optional chaining of an else clause
     * @throws IllegalArgumentException if cmd is null
     * @throws E if the runnable throws an exception
     */
    public <E extends Throwable> OrElse then(final Throwables.Runnable<E> cmd) throws IllegalArgumentException, E {
        N.checkArgNotNull(cmd);

        if (b) {
            cmd.run();
        }

        return OrElse.of(b);
    }

    /**
     * Executes the given consumer with the provided input if the condition is true.
     * 
     * <p>This method is useful for conditional processing of a value.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.notNull(user)
     *   .then(user, u -> saveUser(u))
     *   .orElse(() -> createNewUser());
     * }</pre>
     *
     * @param <T> the type of the input to the consumer
     * @param <E> the type of exception that the consumer may throw
     * @param init the input value to pass to the consumer
     * @param action the consumer to execute if the condition is true
     * @return an OrElse instance for optional chaining of an else clause
     * @throws IllegalArgumentException if action is null
     * @throws E if the consumer throws an exception
     */
    @Beta
    public <T, E extends Throwable> OrElse then(final T init, final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
        N.checkArgNotNull(action);

        if (b) {
            action.accept(init);
        }

        return OrElse.of(b);
    }

    /**
     * Throws the exception provided by the supplier if the condition is true.
     * 
     * <p>This method is useful for validation scenarios where an exception should be thrown
     * when a certain condition is met.</p>
     * 
     * <p>Example:</p>
     * <pre>{@code
     * If.isEmpty(requiredField)
     *   .thenThrow(() -> new ValidationException("Required field is empty"));
     * }</pre>
     *
     * @param <E> the type of exception to throw
     * @param exceptionSupplier the supplier that provides the exception to throw
     * @return an OrElse instance (though it will never be reached if exception is thrown)
     * @throws IllegalArgumentException if exceptionSupplier is null
     * @throws E if the condition is true
     */
    public <E extends Throwable> OrElse thenThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
        N.checkArgNotNull(exceptionSupplier);

        if (b) {
            throw exceptionSupplier.get();
        }

        //noinspection ConstantValue
        return OrElse.of(b);
    }

    /**
     * Represents the else clause in a conditional chain, allowing actions to be executed
     * when the initial condition is false.
     * 
     * <p>This class is returned by the then() methods of the If class and provides methods
     * to specify what should happen when the initial condition evaluates to false.</p>
     */
    public static final class OrElse {
        /**
         * For internal only
         */
        public static final OrElse TRUE = new OrElse(true);

        /**
         * For internal only
         */
        public static final OrElse FALSE = new OrElse(false);

        /** The b. */
        private final boolean isIfTrue;

        /**
         * Instantiates a new or.
         *
         * @param b
         */
        OrElse(final boolean b) {
            isIfTrue = b;
        }

        /**
         *
         * @param b
         * @return
         */
        static OrElse of(final boolean b) {
            return b ? TRUE : FALSE;
        }

        /**
         * Executes no action in the else case.
         * 
         * <p>This method completes the conditional chain without performing any action
         * when the initial condition is false. It's implicitly called when no orElse
         * method is chained.</p>
         */
        void orElseDoNothing() {
            // Do nothing.
        }

        /**
         * Executes the given runnable if the initial condition was false.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * If.is(hasPermission)
         *   .then(() -> performAction())
         *   .orElse(() -> showAccessDeniedMessage());
         * }</pre>
         *
         * @param <E> the type of exception that the runnable may throw
         * @param cmd the runnable to execute if the initial condition was false
         * @throws IllegalArgumentException if cmd is null
         * @throws E if the runnable throws an exception
         */
        public <E extends Throwable> void orElse(final Throwables.Runnable<E> cmd) throws IllegalArgumentException, E {
            N.checkArgNotNull(cmd);

            if (!isIfTrue) {
                cmd.run();
            }
        }

        /**
         * Executes the given consumer with the provided input if the initial condition was false.
         * 
         * <p>Example:</p>
         * <pre>{@code
         * If.isNull(cachedValue)
         *   .then(() -> value = loadFromCache())
         *   .orElse(key, k -> value = loadFromDatabase(k));
         * }</pre>
         *
         * @param <T> the type of the input to the consumer
         * @param <E> the type of exception that the consumer may throw
         * @param init the input value to pass to the consumer
         * @param action the consumer to execute if the initial condition was false
         * @throws IllegalArgumentException if action is null
         * @throws E if the consumer throws an exception
         */
        @Beta
        public <T, E extends Throwable> void orElse(final T init, final Throwables.Consumer<? super T, E> action) throws IllegalArgumentException, E {
            N.checkArgNotNull(action);

            if (!isIfTrue) {
                action.accept(init);
            }
        }

        /**
         * Throws the exception provided by the supplier if the initial condition was false.
         * 
         * <p>This method is useful for validation scenarios where an exception should be thrown
         * when a required condition is not met.</p>
         * 
         * <p>Example:</p>
         * <pre>{@code
         * If.notEmpty(results)
         *   .then(() -> processResults(results))
         *   .orElseThrow(() -> new NoResultsException("No results found"));
         * }</pre>
         *
         * @param <E> the type of exception to throw
         * @param exceptionSupplier the supplier that provides the exception to throw
         * @throws IllegalArgumentException if exceptionSupplier is null
         * @throws E if the initial condition was false
         */
        public <E extends Throwable> void orElseThrow(final Supplier<? extends E> exceptionSupplier) throws IllegalArgumentException, E {
            N.checkArgNotNull(exceptionSupplier);

            if (!isIfTrue) {
                throw exceptionSupplier.get();
            }
        }
    }
}