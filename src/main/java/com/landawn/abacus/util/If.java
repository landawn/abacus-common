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
 * A functional programming utility class for creating fluent conditional execution chains that provides
 * an alternative to traditional if-else statements through method chaining. This class enables elegant
 * conditional logic in functional programming contexts while maintaining readability and supporting
 * various condition types including null checks, emptiness validation, and custom boolean conditions.
 *
 * <p>The {@code If} class follows a fluent API pattern where conditions are evaluated once and subsequent
 * actions are executed based on that evaluation. It provides a more expressive way to handle conditional
 * logic, especially in scenarios involving method chaining, functional transformations, or when building
 * complex conditional workflows.</p>
 *
 * <p><b>⚠️ IMPORTANT - Performance Consideration:</b>
 * While this class provides a functional approach to conditionals, traditional if-else statements
 * or ternary operators are generally preferred for better readability and performance in most cases.
 * Use this class when the fluent API significantly improves code expressiveness or when integrating
 * with functional programming patterns.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Fluent API Design:</b> Method chaining for readable conditional logic</li>
 *   <li><b>Comprehensive Condition Types:</b> Boolean, null, empty, blank, and existence checks</li>
 *   <li><b>Exception Safety:</b> Built-in exception handling with custom exception suppliers</li>
 *   <li><b>Type Safety:</b> Generic support for various data types and collections</li>
 *   <li><b>Functional Integration:</b> Seamless integration with lambda expressions and method references</li>
 *   <li><b>Lazy Evaluation:</b> Actions are only executed when conditions are met</li>
 *   <li><b>Immutable Design:</b> Thread-safe and side-effect free condition evaluation</li>
 *   <li><b>Beta Features:</b> Experimental enhancements for advanced use cases</li>
 * </ul>
 *
 * <p><b>⚠️ IMPORTANT - Final Class Design:</b>
 * <ul>
 *   <li>This is a <b>final utility class</b> that cannot be extended</li>
 *   <li>Uses static factory methods for creating {@code If} instances</li>
 *   <li>Immutable condition state ensures predictable behavior</li>
 *   <li>Thread-safe design with no mutable static state</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Expressiveness:</b> Makes conditional logic more readable through natural language-like syntax</li>
 *   <li><b>Functional Style:</b> Embraces functional programming paradigms with lambda support</li>
 *   <li><b>Error Prevention:</b> Reduces common mistakes through type-safe APIs</li>
 *   <li><b>Composability:</b> Enables building complex conditional workflows</li>
 *   <li><b>Consistency:</b> Uniform API across different condition types</li>
 * </ul>
 *
 * <p><b>Core API Pattern:</b>
 * <ul>
 *   <li><b>Condition Creation:</b> Static factory methods ({@code is()}, {@code notNull()}, {@code notEmpty()}, etc.)</li>
 *   <li><b>Action Execution:</b> {@code then()} methods for conditional execution</li>
 *   <li><b>Alternative Handling:</b> {@code orElse()} methods for fallback logic</li>
 *   <li><b>Exception Throwing:</b> {@code thenThrow()} and {@code orElseThrow()} for error scenarios</li>
 * </ul>
 *
 * <p><b>Supported Condition Types:</b>
 * <ul>
 *   <li><b>Boolean Conditions:</b> {@code is(boolean)}, {@code not(boolean)}</li>
 *   <li><b>Null Checks:</b> {@code isNull(Object)}, {@code notNull(Object)}</li>
 *   <li><b>Empty Checks:</b> Arrays, Collections, Maps, Strings, and specialized containers</li>
 *   <li><b>Blank Checks:</b> {@code isBlank(CharSequence)}, {@code notBlank(CharSequence)}</li>
 *   <li><b>Existence Checks:</b> {@code exists(int)} for index validation</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Basic boolean condition with action
 * If.is(user.isActive())
 *   .then(() -> sendWelcomeEmail(user))
 *   .orElse(() -> sendReactivationEmail(user));
 *
 * // Null safety with conditional processing
 * If.notNull(user.getProfile())
 *   .then(profile -> updateProfile(profile))
 *   .orElse(() -> createDefaultProfile(user));
 *
 * // Collection emptiness checking
 * If.notEmpty(orders)
 *   .then(() -> processOrders(orders))
 *   .orElseThrow(() -> new IllegalStateException("No orders to process"));
 *
 * // String validation
 * If.notBlank(email)
 *   .then(() -> sendNotification(email))
 *   .orElse(() -> logMissingEmailWarning());
 *
 * // Complex conditional logic
 * If.is(user.hasPermission() && resource.isAvailable())
 *   .then(() -> grantAccess(user, resource))
 *   .orElseThrow(() -> new AccessDeniedException("Insufficient permissions"));
 * }</pre>
 *
 * <p><b>Advanced Usage Examples:</b></p>
 * <pre>{@code
 * // Beta feature: action with initialization parameter
 * If.notEmpty(dataList)
 *   .then(new ProcessingContext(), (context, list) -> {
 *       context.initialize();
 *       processDataWithContext(context, list);
 *   })
 *   .orElse(context -> handleEmptyData(context));
 *
 * // Chaining multiple conditions
 * public void processUser(User user) {
 *     If.notNull(user)
 *       .then(() -> validateUser(user))
 *       .orElseThrow(() -> new IllegalArgumentException("User cannot be null"));
 *
 *     If.notBlank(user.getEmail())
 *       .then(() -> sendEmail(user.getEmail()))
 *       .orElse(() -> logEmailMissing(user.getId()));
 *
 *     If.notEmpty(user.getPreferences())
 *       .then(() -> applyPreferences(user.getPreferences()))
 *       .orElse(() -> setDefaultPreferences(user));
 * }
 *
 * // Exception handling patterns
 * If.is(criticalCondition)
 *   .thenThrow(() -> new CriticalException("System failure detected"))
 *   .orElse(() -> continueNormalOperation());
 *
 * // Working with arrays
 * If.notEmpty(dataArray)
 *   .then(() -> Arrays.stream(dataArray).forEach(this::processItem))
 *   .orElse(() -> handleEmptyArray());
 * }</pre>
 *
 * <p><b>OrElse Nested Class:</b>
 * <ul>
 *   <li><b>Fluent Continuation:</b> Returned by {@code then()} methods to enable {@code orElse()} chaining</li>
 *   <li><b>Lazy Evaluation:</b> {@code orElse()} actions only execute if the original condition was false</li>
 *   <li><b>Multiple Options:</b> Supports actions, exception throwing, and no-operation alternatives</li>
 *   <li><b>Type Safety:</b> Maintains generic type information through the chain</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Condition Evaluation:</b> O(1) for boolean conditions, O(1) for null checks</li>
 *   <li><b>Collection Checks:</b> O(1) for size-based emptiness checks</li>
 *   <li><b>String Operations:</b> O(n) for blank checking (whitespace scanning)</li>
 *   <li><b>Memory Overhead:</b> Minimal object creation, cached instances for common cases</li>
 *   <li><b>Method Calls:</b> Additional method call overhead compared to direct if-else</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Immutable State:</b> {@code If} instances are immutable after creation</li>
 *   <li><b>Concurrent Access:</b> Safe for concurrent access from multiple threads</li>
 *   <li><b>Static Methods:</b> All factory methods are thread-safe</li>
 *   <li><b>Action Execution:</b> Thread safety depends on the provided lambda expressions</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li><b>IllegalArgumentException:</b> Thrown for null action parameters where not allowed</li>
 *   <li><b>Custom Exceptions:</b> Support for throwing custom exceptions via suppliers</li>
 *   <li><b>Exception Propagation:</b> Exceptions from lambda expressions are propagated correctly</li>
 *   <li><b>Type Safety:</b> Compile-time checking prevents many runtime errors</li>
 * </ul>
 *
 * <p><b>Memory Management:</b>
 * <ul>
 *   <li><b>Instance Caching:</b> {@code TRUE} and {@code FALSE} instances are cached for reuse</li>
 *   <li><b>Minimal Allocation:</b> Reduces object creation for common boolean conditions</li>
 *   <li><b>Lambda Efficiency:</b> Lambda expressions are compiled efficiently by the JVM</li>
 *   <li><b>No Leaks:</b> No references held beyond the execution chain</li>
 * </ul>
 *
 * <p><b>Integration with Functional Programming:</b>
 * <ul>
 *   <li><b>Lambda Expressions:</b> Full support for lambda expressions and method references</li>
 *   <li><b>Stream API:</b> Can be used within stream operations for conditional processing</li>
 *   <li><b>Optional Integration:</b> Complements {@code Optional} for different conditional scenarios</li>
 *   <li><b>Function Composition:</b> Enables building complex conditional workflows</li>
 * </ul>
 *
 * <p><b>Beta Features:</b>
 * <ul>
 *   <li><b>Parameter Passing:</b> {@code then(T init, Consumer<T>)} allows passing initialization parameters</li>
 *   <li><b>Experimental API:</b> Subject to change in future versions</li>
 *   <li><b>Enhanced Expressiveness:</b> Provides additional ways to structure conditional logic</li>
 *   <li><b>Feedback Requested:</b> Beta features are for evaluation and feedback</li>
 * </ul>
 *
 * <p><b>Comparison with Alternatives:</b>
 * <ul>
 *   <li><b>vs. if-else:</b> More expressive but with slight performance overhead</li>
 *   <li><b>vs. ternary operator:</b> Better for complex conditions and multiple actions</li>
 *   <li><b>vs. Optional:</b> Different use case - conditional execution vs. value presence</li>
 *   <li><b>vs. Guards:</b> More fluent than guard clauses but less explicit</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use for complex conditional logic where readability is improved</li>
 *   <li>Prefer traditional if-else for simple boolean conditions</li>
 *   <li>Chain multiple conditions for related validation logic</li>
 *   <li>Use meaningful lambda expressions and avoid complex inline logic</li>
 *   <li>Consider performance implications for hot code paths</li>
 *   <li>Leverage type safety features to prevent runtime errors</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Overusing fluent API where simple if-else would be clearer</li>
 *   <li>Creating overly complex lambda expressions inline</li>
 *   <li>Ignoring the performance overhead in performance-critical code</li>
 *   <li>Using for simple boolean conditions that don't benefit from fluency</li>
 *   <li>Nesting {@code If} chains unnecessarily</li>
 * </ul>
 *
 * <p><b>Related Utility Methods:</b>
 * <ul>
 *   <li><b>{@link N#ifOrEmpty}:</b> Alternative conditional execution utilities</li>
 *   <li><b>{@link N#ifOrElse}:</b> Simple if-else utility methods</li>
 *   <li><b>{@link N#ifNotNull}:</b> Null-safe conditional execution</li>
 *   <li><b>{@link N#ifNotEmpty}:</b> Emptiness-aware conditional execution</li>
 * </ul>
 *
 * <p><b>Example: User Validation Workflow</b>
 * <pre>{@code
 * public class UserValidator {
 *     public void validateAndProcess(User user, UserContext context) {
 *         // Comprehensive user validation using If chains
 *         If.notNull(user)
 *           .then(() -> validateUserStructure(user))
 *           .orElseThrow(() -> new IllegalArgumentException("User cannot be null"));
 *
 *         If.notBlank(user.getUsername())
 *           .then(() -> checkUsernameAvailability(user.getUsername()))
 *           .orElseThrow(() -> new ValidationException("Username is required"));
 *
 *         If.notBlank(user.getEmail())
 *           .then(() -> validateEmailFormat(user.getEmail()))
 *           .orElse(() -> user.setEmail(generateTemporaryEmail()));
 *
 *         If.notEmpty(user.getRoles())
 *           .then(() -> validateRoles(user.getRoles()))
 *           .orElse(() -> assignDefaultRole(user));
 *
 *         If.is(user.isActive() && context.isRegistrationOpen())
 *           .then(() -> processActiveUser(user, context))
 *           .orElse(() -> queueForLaterProcessing(user));
 *
 *         If.notEmpty(user.getPreferences())
 *           .then(context, (ctx, prefs) -> {
 *               ctx.setProcessingMode(CUSTOM);
 *               applyUserPreferences(prefs, ctx);
 *           })
 *           .orElse(ctx -> ctx.setProcessingMode(DEFAULT));
 *     }
 * }
 * }</pre>
 *
 * @see N#ifOrEmpty(boolean, Throwables.Supplier)
 * @see N#ifOrElse(boolean, Throwables.Runnable, Throwables.Runnable)
 * @see N#ifNotNull(Object, Throwables.Consumer)
 * @see N#ifNotEmpty(CharSequence, Throwables.Consumer)
 * @see N#ifNotEmpty(Collection, Throwables.Consumer)
 * @see N#ifNotEmpty(Map, Throwables.Consumer)
 * @see u.Optional
 * @see Supplier
 * @see Throwables.Runnable
 * @see Throwables.Consumer
 * @see Collection
 * @see Map
 * @see CharSequence
 */
@Beta
public final class If {

    private static final If TRUE = new If(true);

    private static final If FALSE = new If(false);

    final boolean b; // change to package-private for testing purposes

    If(final boolean b) {
        this.b = b;
    }

    /**
     * Creates an If instance based on the given boolean condition.
     * 
     * <p><b>Usage Examples:</b></p>
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
     * <p><b>Usage Examples:</b></p>
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
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.exists(list.indexOf(element))
     *   .then(() -> System.out.println("Element found"))
     *   .orElse(() -> System.out.println("Element not found"));
     * }</pre>
     *
     * @param index the index value to check
     * @return an If instance that is {@code true} if the index is non-negative
     */
    public static If exists(final int index) {
        return index >= 0 ? TRUE : FALSE;
    }

    /**
     * Creates an If instance that checks if the given object is {@code null}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.isNull(result)
     *   .then(() -> handleNullResult())
     *   .orElse(() -> processResult(result));
     * }</pre>
     *
     * @param obj the object to check for null
     * @return an If instance that is {@code true} if the object is null
     */
    public static If isNull(final Object obj) {
        return is(obj == null);
    }

    /**
     * Creates an If instance that checks if the given CharSequence is {@code null} or empty.
     * 
     * <p>A CharSequence is considered empty if it is {@code null} or has zero length.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.isEmpty(username)
     *   .then(() -> System.out.println("Username is required"));
     * }</pre>
     *
     * @param s the CharSequence to check
     * @return an If instance that is {@code true} if the CharSequence is {@code null} or empty
     */
    public static If isEmpty(final CharSequence s) {
        return is(Strings.isEmpty(s));
    }

    /**
     * Creates an If instance that checks if the given boolean array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] flags = {true, false};
     * If.isEmpty(flags).thenRun(() -> System.out.println("Empty"));  // does nothing
     * If.isEmpty(null).thenRun(() -> System.out.println("Empty"));   // prints "Empty"
     * }</pre>
     *
     * @param a the boolean array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final boolean[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given char array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'a', 'b', 'c'};
     * If.isEmpty(chars).thenRun(() -> System.out.println("Empty"));  // does nothing
     * If.isEmpty(null).thenRun(() -> System.out.println("Empty"));   // prints "Empty"
     * }</pre>
     *
     * @param a the char array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final char[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given byte array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = {1, 2, 3};
     * If.isEmpty(data).thenRun(() -> System.out.println("Empty"));  // does nothing
     * If.isEmpty(null).thenRun(() -> System.out.println("Empty"));  // prints "Empty"
     * }</pre>
     *
     * @param a the byte array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final byte[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given short array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] values = {10, 20, 30};
     * If.isEmpty(values).thenRun(() -> System.out.println("Empty"));  // does nothing
     * If.isEmpty(null).thenRun(() -> System.out.println("Empty"));    // prints "Empty"
     * }</pre>
     *
     * @param a the short array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final short[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given int array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] numbers = {1, 2, 3};
     * If.isEmpty(numbers).thenRun(() -> System.out.println("Empty"));  // does nothing
     * If.isEmpty(null).thenRun(() -> System.out.println("Empty"));     // prints "Empty"
     * }</pre>
     *
     * @param a the int array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final int[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given long array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] ids = {100L, 200L, 300L};
     * If.isEmpty(ids).thenRun(() -> System.out.println("Empty"));  // does nothing
     * If.isEmpty(null).thenRun(() -> System.out.println("Empty")); // prints "Empty"
     * }</pre>
     *
     * @param a the long array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final long[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given float array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] prices = {1.5f, 2.5f, 3.5f};
     * If.isEmpty(prices).thenRun(() -> System.out.println("Empty"));  // does nothing
     * If.isEmpty(null).thenRun(() -> System.out.println("Empty"));    // prints "Empty"
     * }</pre>
     *
     * @param a the float array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final float[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given double array is {@code null} or empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] scores = {98.5, 87.3, 92.1};
     * If.isEmpty(scores).thenRun(() -> System.out.println("Empty"));  // does nothing
     * If.isEmpty(null).thenRun(() -> System.out.println("Empty"));    // prints "Empty"
     * }</pre>
     *
     * @param a the double array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final double[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given object array is {@code null} or empty.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.isEmpty(args)
     *   .then(() -> System.out.println("No arguments provided"));
     * }</pre>
     *
     * @param a the object array to check
     * @return an If instance that is {@code true} if the array is {@code null} or has zero length
     */
    public static If isEmpty(final Object[] a) {
        return is(N.isEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given Collection is {@code null} or empty.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.isEmpty(resultList)
     *   .then(() -> System.out.println("No results found"))
     *   .orElse(() -> displayResults(resultList));
     * }</pre>
     *
     * @param c the Collection to check
     * @return an If instance that is {@code true} if the Collection is {@code null} or empty
     */
    public static If isEmpty(final Collection<?> c) {
        return is(N.isEmpty(c));
    }

    /**
     * Creates an If instance that checks if the given Map is {@code null} or empty.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.isEmpty(configMap)
     *   .then(() -> loadDefaultConfig());
     * }</pre>
     *
     * @param m the Map to check
     * @return an If instance that is {@code true} if the Map is {@code null} or empty
     */
    public static If isEmpty(final Map<?, ?> m) {
        return is(N.isEmpty(m));
    }

    /**
     * Creates an If instance that checks if the given PrimitiveList is {@code null} or empty.
     *
     * @param list the PrimitiveList to check
     * @return an If instance that is {@code true} if the PrimitiveList is {@code null} or empty
     */
    @SuppressWarnings("rawtypes")
    public static If isEmpty(final PrimitiveList list) {
        return is(N.isEmpty(list));
    }

    /**
     * Creates an If instance that checks if the given Multiset is {@code null} or empty.
     *
     * @param s the Multiset to check
     * @return an If instance that is {@code true} if the Multiset is {@code null} or empty
     */
    public static If isEmpty(final Multiset<?> s) {
        return is(N.isEmpty(s));
    }

    /**
     * Creates an If instance that checks if the given Multimap is {@code null} or empty.
     *
     * @param m the Multimap to check
     * @return an If instance that is {@code true} if the Multimap is {@code null} or empty
     */
    public static If isEmpty(final Multimap<?, ?, ?> m) {
        return is(N.isEmpty(m));
    }

    /**
     * Creates an If instance that checks if the given CharSequence is {@code null}, empty, or contains only whitespace.
     * 
     * <p>A CharSequence is considered blank if it is {@code null}, has zero length, or contains only
     * whitespace characters as defined by {@link Character#isWhitespace(char)}.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.isBlank(userInput)
     *   .then(() -> System.out.println("Please enter valid input"));
     * }</pre>
     *
     * @param s the CharSequence to check
     * @return an If instance that is {@code true} if the CharSequence is {@code null}, empty, or blank
     */
    // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu.
    public static If isBlank(final CharSequence s) {
        return is(Strings.isBlank(s));
    }

    /**
     * Creates an If instance that checks if the given object is not {@code null}.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.notNull(user)
     *   .then(u -> System.out.println("User: " + u.getName()))
     *   .orElse(() -> System.out.println("User not found"));
     * }</pre>
     *
     * @param obj the object to check
     * @return an If instance that is {@code true} if the object is not null
     */
    public static If notNull(final Object obj) {
        return is(obj != null);
    }

    /**
     * Creates an If instance that checks if the given CharSequence is not {@code null} and not empty.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.notEmpty(searchQuery)
     *   .then(() -> performSearch(searchQuery));
     * }</pre>
     *
     * @param s the CharSequence to check
     * @return an If instance that is {@code true} if the CharSequence is not {@code null} and has length > 0
     */
    public static If notEmpty(final CharSequence s) {
        return is(Strings.isNotEmpty(s));
    }

    /**
     * Creates an If instance that checks if the given boolean array is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * boolean[] flags = {true, false};
     * If.notEmpty(flags).thenRun(() -> System.out.println("Has data"));  // prints "Has data"
     * If.notEmpty(null).thenRun(() -> System.out.println("Has data"));   // does nothing
     * }</pre>
     *
     * @param a the boolean array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final boolean[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given char array is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * char[] chars = {'a', 'b', 'c'};
     * If.notEmpty(chars).thenRun(() -> System.out.println("Has data"));  // prints "Has data"
     * If.notEmpty(null).thenRun(() -> System.out.println("Has data"));   // does nothing
     * }</pre>
     *
     * @param a the char array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final char[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given byte array is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * byte[] data = {1, 2, 3};
     * If.notEmpty(data).thenRun(() -> System.out.println("Has data"));  // prints "Has data"
     * If.notEmpty(null).thenRun(() -> System.out.println("Has data"));  // does nothing
     * }</pre>
     *
     * @param a the byte array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final byte[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given short array is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * short[] values = {10, 20, 30};
     * If.notEmpty(values).thenRun(() -> System.out.println("Has data"));  // prints "Has data"
     * If.notEmpty(null).thenRun(() -> System.out.println("Has data"));    // does nothing
     * }</pre>
     *
     * @param a the short array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final short[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given int array is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * int[] numbers = {1, 2, 3};
     * If.notEmpty(numbers).thenRun(() -> System.out.println("Has data"));  // prints "Has data"
     * If.notEmpty(null).thenRun(() -> System.out.println("Has data"));     // does nothing
     * }</pre>
     *
     * @param a the int array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final int[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given long array is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * long[] ids = {100L, 200L, 300L};
     * If.notEmpty(ids).thenRun(() -> System.out.println("Has data"));  // prints "Has data"
     * If.notEmpty(null).thenRun(() -> System.out.println("Has data")); // does nothing
     * }</pre>
     *
     * @param a the long array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final long[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given float array is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * float[] prices = {1.5f, 2.5f, 3.5f};
     * If.notEmpty(prices).thenRun(() -> System.out.println("Has data"));  // prints "Has data"
     * If.notEmpty(null).thenRun(() -> System.out.println("Has data"));    // does nothing
     * }</pre>
     *
     * @param a the float array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final float[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given double array is not {@code null} and not empty.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * double[] scores = {98.5, 87.3, 92.1};
     * If.notEmpty(scores).thenRun(() -> System.out.println("Has data"));  // prints "Has data"
     * If.notEmpty(null).thenRun(() -> System.out.println("Has data"));    // does nothing
     * }</pre>
     *
     * @param a the double array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final double[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given object array is not {@code null} and not empty.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.notEmpty(files)
     *   .then(() -> processFiles(files));
     * }</pre>
     *
     * @param a the object array to check
     * @return an If instance that is {@code true} if the array is not {@code null} and has length > 0
     */
    public static If notEmpty(final Object[] a) {
        return is(N.notEmpty(a));
    }

    /**
     * Creates an If instance that checks if the given Collection is not {@code null} and not empty.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.notEmpty(selectedItems)
     *   .then(() -> processSelection(selectedItems))
     *   .orElse(() -> showNoSelectionMessage());
     * }</pre>
     *
     * @param c the Collection to check
     * @return an If instance that is {@code true} if the Collection is not {@code null} and not empty
     */
    public static If notEmpty(final Collection<?> c) {
        return is(N.notEmpty(c));
    }

    /**
     * Creates an If instance that checks if the given Map is not {@code null} and not empty.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.notEmpty(properties)
     *   .then(() -> applyProperties(properties));
     * }</pre>
     *
     * @param m the Map to check
     * @return an If instance that is {@code true} if the Map is not {@code null} and not empty
     */
    public static If notEmpty(final Map<?, ?> m) {
        return is(N.notEmpty(m));
    }

    /**
     * Creates an If instance that checks if the given PrimitiveList is not {@code null} and not empty.
     *
     * @param list the PrimitiveList to check
     * @return an If instance that is {@code true} if the PrimitiveList is not {@code null} and not empty
     */
    @SuppressWarnings("rawtypes")
    public static If notEmpty(final PrimitiveList list) {
        return is(N.notEmpty(list));
    }

    /**
     * Creates an If instance that checks if the given Multiset is not {@code null} and not empty.
     *
     * @param s the Multiset to check
     * @return an If instance that is {@code true} if the Multiset is not {@code null} and not empty
     */
    public static If notEmpty(final Multiset<?> s) {
        return is(N.notEmpty(s));
    }

    /**
     * Creates an If instance that checks if the given Multimap is not {@code null} and not empty.
     *
     * @param m the Multimap to check
     * @return an If instance that is {@code true} if the Multimap is not {@code null} and not empty
     */
    public static If notEmpty(final Multimap<?, ?, ?> m) {
        return is(N.notEmpty(m));
    }

    /**
     * Creates an If instance that checks if the given CharSequence is not {@code null}, not empty, and not blank.
     * 
     * <p>A CharSequence is considered not blank if it is not {@code null}, has length > 0, and contains
     * at least one non-whitespace character.</p>
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * If.notBlank(username)
     *   .then(() -> loginUser(username))
     *   .orElse(() -> showError("Username cannot be blank"));
     * }</pre>
     *
     * @param s the CharSequence to check
     * @return an If instance that is {@code true} if the CharSequence is not {@code null}, not empty, and not blank
     */
    // DON'T change 'OrEmptyOrBlank' to 'OrBlank' because of the occurring order in the auto-completed context menu.
    public static If notBlank(final CharSequence s) {
        return is(Strings.isNotBlank(s));
    }

    /**
     * Executes no action if the condition is {@code true}, but allows chaining to an orElse clause.
     * 
     * <p>This method is useful when you only want to execute an action in the {@code false} case.</p>
     * 
     * <p><b>Usage Examples:</b></p>
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
     * Executes the given runnable if the condition is {@code true}.
     * 
     * <p><b>Usage Examples:</b></p>
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
     * Executes the given consumer with the provided input if the condition is {@code true}.
     * 
     * <p>This method is useful for conditional processing of a value.</p>
     * 
     * <p><b>Usage Examples:</b></p>
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
     * Throws the exception provided by the supplier if the condition is {@code true}.
     * 
     * <p>This method is useful for validation scenarios where an exception should be thrown
     * when a certain condition is met.</p>
     * 
     * <p><b>Usage Examples:</b></p>
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
     * when the initial condition is {@code false}.
     * 
     * <p>This class is returned by the then() methods of the If class and provides methods
     * to specify what should happen when the initial condition evaluates to {@code false}.</p>
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

        /**
         * The boolean state indicating whether the initial If condition was {@code true}.
         * Used to determine whether to execute the then clause or the orElse clause.
         */
        private final boolean isIfTrue;

        /**
         * Constructs a new OrElse instance with the given boolean state.
         *
         * <p>This constructor is package-private and used internally by the If class
         * to create OrElse instances representing the state of the conditional chain.</p>
         *
         * @param b the boolean state indicating whether the initial If condition was true
         */
        OrElse(final boolean b) {
            isIfTrue = b;
        }

        /**
         * Factory method to create an OrElse instance based on the given boolean value.
         *
         * <p>This method uses cached instances (TRUE or FALSE) for performance optimization,
         * avoiding object creation for repeated conditional evaluations.</p>
         *
         * @param b the boolean state indicating whether the initial If condition was true
         * @return an OrElse instance representing the given state (cached instance)
         */
        static OrElse of(final boolean b) {
            return b ? TRUE : FALSE;
        }

        /**
         * Executes no action in the else case.
         * 
         * <p>This method completes the conditional chain without performing any action
         * when the initial condition is {@code false}. It's implicitly called when no orElse
         * method is chained.</p>
         */
        void orElseDoNothing() {
            // Do nothing.
        }

        /**
         * Executes the given runnable if the initial condition was {@code false}.
         * 
         * <p><b>Usage Examples:</b></p>
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
         * Executes the given consumer with the provided input if the initial condition was {@code false}.
         * 
         * <p><b>Usage Examples:</b></p>
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
         * Throws the exception provided by the supplier if the initial condition was {@code false}.
         * 
         * <p>This method is useful for validation scenarios where an exception should be thrown
         * when a required condition is not met.</p>
         * 
         * <p><b>Usage Examples:</b></p>
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
