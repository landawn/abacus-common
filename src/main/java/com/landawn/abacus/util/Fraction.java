/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.util;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.io.Serializable;
import java.math.BigInteger;

/**
 * An immutable representation of rational numbers stored as integer fractions. Arithmetic remains
 * exact while all intermediate and final numerator and denominator values fit in {@code int}; operations
 * that exceed that range throw {@link ArithmeticException}. Conversions to or from floating-point values
 * are necessarily approximate, so decimal-based financial calculations may be better represented by
 * {@link java.math.BigDecimal}.
 *
 * <p>Unlike floating-point numbers ({@code float}, {@code double}) which use binary approximations
 * that can introduce rounding errors, {@code Fraction} stores the numerator and denominator as
 * separate integers. Subject to the integer range limitation, this ensures that operations like
 * 1/3 + 1/3 + 1/3 = 1 are computed exactly, without accumulating floating-point errors.</p>
 *
 * <p><b>Note: this class has a natural ordering that is inconsistent with {@code equals}.</b>
 * {@link #compareTo(Fraction)} compares numeric <em>values</em>, while {@link #equals(Object)}
 * compares the stored numerator and denominator, so {@code 1/2} and {@code 2/4} compare equal but are
 * not {@code equals}. Sorted and hashed collections therefore disagree about them:</p>
 * <pre>{@code
 * Set<Fraction> sorted = new TreeSet<>(List.of(Fraction.of(1, 2), Fraction.of(2, 4)));
 * Set<Fraction> hashed = new HashSet<>(List.of(Fraction.of(1, 2), Fraction.of(2, 4)));
 * sorted.size();   // returns 1 - TreeSet uses compareTo
 * hashed.size();   // returns 2 - HashSet uses equals
 * }</pre>
 * <p>Reduce with {@link #reduce()} before using fractions as set elements or map keys if a single
 * canonical representative per value is wanted. All arithmetic operations return reduced terms,
 * including unary operations and identity operations such as adding zero or raising to power one.
 * For example, {@code Fraction.of(1, 2).add(Fraction.of(2, 4))} returns {@code 1/1}.
 * Factories without reduction and parsing still preserve unreduced terms. Code that previously
 * depended on unreduced arithmetic results must account for changes to {@code equals}, hash codes,
 * text, and serialized terms; preserve the original operands when their written terms matter.</p>
 *
 * <p><b>Key Features:</b>
 * <ul>
 *   <li><b>Exact Rational Arithmetic:</b> No rounding in fraction-to-fraction arithmetic while terms fit in {@code int}</li>
 *   <li><b>Immutable Design:</b> All instances are immutable, ensuring thread safety and preventing accidental modification</li>
 *   <li><b>Integer-Based Storage:</b> Uses {@code int} numerator and denominator for optimal performance</li>
 *   <li><b>Optional Reduction:</b> Factory methods document whether they reduce the fraction; {@link #of(int, int)} preserves the supplied terms</li>
 *   <li><b>Number Integration:</b> Extends {@code Number} for seamless integration with Java's numeric hierarchy</li>
 *   <li><b>Comprehensive Arithmetic:</b> Full support for addition, subtraction, multiplication, and division</li>
 *   <li><b>Multiple Representations:</b> Support for proper fractions, improper fractions, and mixed numbers</li>
 *   <li><b>String Parsing:</b> Flexible parsing of various fraction string formats</li>
 * </ul>
 *
 * <p><b>IMPORTANT - Immutable Design:</b>
 * <ul>
 *   <li>This class implements {@link Immutable}, guaranteeing that instances cannot be modified after creation</li>
 *   <li>Both {@code numerator} and {@code denominator} fields are final and set only during construction</li>
 *   <li>Arithmetic operations return reduced results without modifying their operands; an existing instance may be reused</li>
 *   <li>Thread-safe by design due to immutability and lack of mutable state</li>
 * </ul>
 *
 * <p><b>IMPORTANT - Integer Range Limitations:</b>
 * <ul>
 *   <li>This implementation uses {@code int} primitives, limiting values to approximately ±2 billion</li>
 *   <li>Arithmetic operations may cause integer overflow for very large numerators or denominators</li>
 *   <li>For unlimited precision, consider using Apache Commons Math's {@code BigFraction} class</li>
 *   <li>Overflow detection is built into arithmetic operations and will throw {@code ArithmeticException}</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Precision Over Performance:</b> Exact fractional representation prioritized over floating-point speed</li>
 *   <li><b>Simplicity Over Complexity:</b> Integer-based implementation for optimal performance in common cases</li>
 *   <li><b>Immutability Over Mutability:</b> Ensures predictable behavior and thread safety</li>
 *   <li><b>Explicit Form:</b> Reduced and unreduced fractions are both supported; use {@link #reduce()} when canonical terms are required</li>
 *   <li><b>Interoperability:</b> Seamless integration with existing Java numeric APIs</li>
 * </ul>
 *
 * <p><b>Internal Representation:</b>
 * <ul>
 *   <li><b>Numerator:</b> {@code int} representing the fraction's numerator (top number)</li>
 *   <li><b>Denominator:</b> {@code int} representing the fraction's denominator (bottom number), always positive</li>
 *   <li><b>Reduction:</b> Fractions may be stored in reduced or unreduced form depending on the factory method or operation used</li>
 *   <li><b>Sign Convention:</b> Negative fractions have negative numerator, positive denominator</li>
 * </ul>
 *
 * <p><b>Common Fraction Constants:</b>
 * <ul>
 *   <li><b>{@link #ZERO}:</b> 0/1 - Represents zero</li>
 *   <li><b>{@link #ONE}:</b> 1/1 - Represents unity</li>
 *   <li><b>{@link #ONE_HALF}:</b> 1/2 - One half</li>
 *   <li><b>{@link #ONE_THIRD}, {@link #TWO_THIRDS}:</b> Common thirds</li>
 *   <li><b>{@link #ONE_QUARTER}, {@link #THREE_QUARTERS}:</b> Common quarters</li>
 *   <li><b>{@link #ONE_FIFTH} through {@link #FOUR_FIFTHS}:</b> Common fifths</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Creating fractions using static factory methods
 * Fraction half = Fraction.of(1, 2);             // returns 1/2
 * Fraction twoThirds = Fraction.of(2, 3);        // returns 2/3
 * Fraction mixedNumber = Fraction.ofMixed(2, 1, 4);   // returns 2 1/4 = 9/4
 *
 * // Creating from decimal values
 * Fraction fromDecimal = Fraction.of(0.75);          // returns 3/4
 * Fraction fromString = Fraction.of("3/4");          // returns 3/4
 * Fraction mixedFromString = Fraction.of("2 1/4");   // returns 9/4
 *
 * // Accessing fraction components
 * int num = half.numerator();                      // returns 1
 * int denom = half.denominator();                  // returns 2
 * int whole = mixedNumber.properWhole();           // returns 2
 * int properNum = mixedNumber.properNumerator();   // returns 1
 *
 * // Arithmetic operations
 * Fraction sum = half.add(twoThirds);                // returns 7/6
 * Fraction difference = twoThirds.subtract(half);    // returns 1/6
 * Fraction product = half.multipliedBy(twoThirds);   // returns 1/3
 * Fraction quotient = twoThirds.dividedBy(half);     // returns 4/3
 * }</pre>
 *
 * <p><b>Advanced Usage Examples:</b></p>
 * <pre>{@code
 * Fraction half = Fraction.of(1, 2);
 * Fraction twoThirds = Fraction.of(2, 3);
 * Fraction mixedNumber = Fraction.ofMixed(2, 1, 4);
 *
 * // Complex fraction arithmetic
 * Fraction recipe = Fraction.of(2, 3);                          // 2/3 cup flour
 * Fraction scalingFactor = Fraction.of(3, 2);                   // 1.5x scaling factor
 * Fraction scaledAmount = recipe.multipliedBy(scalingFactor);   // 1 cup flour
 *
 * // Financial calculations (avoiding floating-point errors)
 * Fraction interestRate = Fraction.of(3, 100);                // 3% as exact fraction
 * Fraction principal = Fraction.of(1000, 1);                  // $1000
 * Fraction interest = principal.multipliedBy(interestRate);   // 30/1 (exactly $30)
 *
 * // Mathematical operations
 * Fraction negative = half.negate();                      // returns -1/2
 * Fraction reciprocal = twoThirds.invert();               // returns 3/2
 * Fraction absolute = negative.abs();                     // returns 1/2
 * Fraction reduced = Fraction.of(6, 8, false).reduce();   // returns 3/4
 * Fraction squared = half.pow(2);                         // returns 1/4
 *
 * // Comparisons and ordering
 * int comparison = half.compareTo(twoThirds);   // returns negative (1/2 < 2/3)
 * List<Fraction> fractions = Arrays.asList(half, twoThirds, Fraction.ONE_QUARTER);
 * Collections.sort(fractions);   // fractions is now [1/4, 1/2, 2/3]
 *
 * // String representations
 * String simple = half.toString();                // returns "1/2"
 * String proper = mixedNumber.toProperString();   // returns "2 1/4"
 * }</pre>
 *
 * <p><b>Fraction Creation Methods:</b>
 * <ul>
 *   <li><b>{@code of(int, int)}:</b> Creates fraction from numerator and denominator without reducing (use {@link #reduce()} or {@link #of(int, int, boolean)} for the reduced form)</li>
 *   <li><b>{@code of(int, int, boolean)}:</b> Creates fraction with optional reduction control</li>
 *   <li><b>{@code ofMixed(int, int, int)}:</b> Creates fraction from whole number, numerator, and denominator</li>
 *   <li><b>{@code of(double)}:</b> Converts decimal value to closest fraction representation</li>
 *   <li><b>{@code of(String)}:</b> Parses fraction from string in various formats</li>
 * </ul>
 *
 * <p><b>Supported String Formats:</b>
 * <ul>
 *   <li><b>Simple Fractions:</b> {@code "3/4"}, {@code "-2/5"}, {@code "7/8"}</li>
 *   <li><b>Whole Numbers:</b> {@code "5"}, {@code "-3"}, {@code "0"}</li>
 *   <li><b>Mixed Numbers:</b> {@code "2 1/4"}, {@code "-1 2/3"}, {@code "5 7/8"}</li>
 *   <li><b>Decimal Conversion:</b> Automatic conversion via {@code of(double)} internally</li>
 * </ul>
 *
 * <p><b>Arithmetic Operations and Overflow Protection:</b>
 * <ul>
 *   <li><b>Addition/Subtraction:</b> Uses common denominator approach with overflow checking</li>
 *   <li><b>Multiplication:</b> Direct numerator/denominator multiplication with reduction</li>
 *   <li><b>Division:</b> Multiplication by reciprocal with zero-denominator protection</li>
 *   <li><b>Overflow Detection:</b> Built-in checks throw {@code ArithmeticException} on overflow</li>
 * </ul>
 *
 * <p><b>Performance Characteristics:</b>
 * <ul>
 *   <li><b>Creation Cost:</b> O(1) for the non-reducing factories; O(log min(n,d)) when reduction is requested</li>
 *   <li><b>{@code of(double)} Cost:</b> a continued-fraction search over the exact binary value of the
 *       input, bounded by the 10,000 denominator and the {@code int} numerator limit; it uses
 *       {@link BigInteger} internally and is substantially more expensive than the {@code int}-term
 *       factories. {@link #of(String)} pays the same cost for a plain decimal token such as
 *       {@code "0.333"}.</li>
 *   <li><b>Arithmetic Cost:</b> O(log min(n,d)) for operations requiring reduction</li>
 *   <li><b>Comparison Cost:</b> O(1) - Cross multiplication for ordering</li>
 *   <li><b>Memory Overhead:</b> Two {@code int} fields plus object header, cached string representations</li>
 *   <li><b>String Caching:</b> {@code toString()} and {@code toProperString()} results are cached</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Immutable Fields:</b> Numerator and denominator are final and never modified</li>
 *   <li><b>Concurrent Access:</b> Safe for concurrent read access from multiple threads</li>
 *   <li><b>Cached Values:</b> String representations are cached lazily; concurrent callers may
 *       harmlessly compute the same value more than once</li>
 *   <li><b>No Synchronization:</b> No locks are needed because the represented value is immutable</li>
 * </ul>
 *
 * <p><b>Number Interface Implementation:</b>
 * <ul>
 *   <li><b>{@code intValue()}:</b> Returns truncated integer value (numerator / denominator)</li>
 *   <li><b>{@code longValue()}:</b> Returns the same truncated whole-number part as {@link #intValue()},
 *       widened to {@code long}; because the denominator is always positive the quotient of two
 *       {@code int} terms always fits in an {@code int}, so the wider type never yields a different value</li>
 *   <li><b>{@code floatValue()}:</b> Returns floating-point approximation</li>
 *   <li><b>{@code doubleValue()}:</b> Returns double-precision approximation</li>
 * </ul>
 *
 * <p><b>Representation:</b>
 * <ul>
 *   <li><b>Reduction:</b> {@link #of(int, int)} preserves supplied terms; {@link #of(int, int, boolean)} and {@link #reduce()} can be used when reduced terms are required</li>
 *   <li><b>Positive Denominator:</b> Negative sign is always carried by the numerator</li>
 *   <li><b>Equality Representation:</b> {@link #equals(Object)} compares stored numerator and denominator, so unreduced equivalent fractions are not equal</li>
 *   <li><b>Efficient Comparison:</b> {@link #compareTo(Fraction)} compares numeric values without requiring both fractions to be reduced</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li><b>ArithmeticException:</b> Thrown for division by zero or integer overflow</li>
 *   <li><b>NumberFormatException:</b> Thrown for invalid string formats in parsing</li>
 *   <li><b>IllegalArgumentException:</b> Thrown for invalid construction parameters</li>
 *   <li><b>Null Safety:</b> {@code null} arguments are rejected with {@code IllegalArgumentException}
 *       (only {@link #compareTo(Fraction)} surfaces a {@code NullPointerException})</li>
 * </ul>
 *
 * <p><b>Best Practices:</b>
 * <ul>
 *   <li>Use static factory methods ({@code of()}) rather than constructors for fraction creation</li>
 *   <li>Prefer {@code Fraction} over {@code double} for exact decimal arithmetic</li>
 *   <li>Use predefined constants ({@code ONE_HALF}, {@code ONE_THIRD}) for common fractions</li>
 *   <li>Cache frequently used fraction instances to reduce object allocation</li>
 *   <li>Use {@code reduce()} explicitly only when working with unreduced fractions</li>
 *   <li>Consider overflow potential when working with large numerators or denominators</li>
 *   <li>Use {@code compareTo()} for ordering rather than converting to decimal values</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Converting to {@code double} for arithmetic and back to {@code Fraction} (loses precision)</li>
 *   <li>Attempting to use the {@code new Fraction(...)} constructor directly instead of the static
 *   {@code of(...)} factory methods (the constructor is private)</li>
 *   <li>Ignoring potential overflow in arithmetic operations with large values</li>
 *   <li>Creating multiple fraction instances for the same logical value</li>
 *   <li>Using {@code Fraction} for very large numbers without considering integer limits</li>
 *   <li>Assuming all arithmetic operations will succeed without handling {@code ArithmeticException}</li>
 * </ul>
 *
 * <p><b>Comparison with Alternative Approaches:</b>
 * <ul>
 *   <li><b>vs. double/float:</b> Fraction provides exact precision vs. floating-point approximations</li>
 *   <li><b>vs. BigDecimal:</b> Fraction represents true fractions vs. decimal approximations</li>
 *   <li><b>vs. BigFraction:</b> This class is faster but limited to int range vs. unlimited precision</li>
 *   <li><b>vs. Rational Libraries:</b> Optimized for common use cases vs. comprehensive mathematical features</li>
 * </ul>
 *
 * <p><b>Integration with Java Ecosystem:</b>
 * <ul>
 *   <li><b>{@link Number}:</b> Direct integration with Java's numeric hierarchy</li>
 *   <li><b>{@link Comparable}:</b> Natural ordering support for sorting and searching</li>
 *   <li><b>{@link Serializable}:</b> Support for serialization and persistence</li>
 *   <li><b>Collections Framework:</b> Can be used in collections with proper ordering</li>
 * </ul>
 *
 * <p><b>Use Cases and Applications:</b>
 * <ul>
 *   <li><b>Financial Calculations:</b> Exact monetary computations without rounding errors</li>
 *   <li><b>Recipe Scaling:</b> Precise scaling of ingredient proportions</li>
 *   <li><b>Mathematical Education:</b> Teaching exact fractional arithmetic</li>
 *   <li><b>Engineering Calculations:</b> Precise ratios and proportional calculations</li>
 *   <li><b>Music Theory:</b> Representing musical intervals and frequency ratios</li>
 *   <li><b>Scientific Computing:</b> Exact rational number arithmetic in algorithms</li>
 * </ul>
 *
 * <p><b>Usage Examples: Financial Interest Calculation</b>
 * <pre>{@code
 * public class FinancialCalculator {
 *     public static Fraction calculateCompoundInterest(
 *             Fraction principal, Fraction rate, int periods) {
 *         Fraction onePlusRate = Fraction.ONE.add(rate);
 *         Fraction compoundFactor = onePlusRate.pow(periods);
 *         return principal.multipliedBy(compoundFactor);
 *     }
 *
 *     public static void main(String[] args) {
 *         Fraction principal = Fraction.of(1000, 1);   // $1000
 *         Fraction rate = Fraction.of(5, 100);   // 5% as exact fraction
 *         int years = 3;
 *
 *         Fraction finalAmount = calculateCompoundInterest(principal, rate, years);
 *         System.out.println("Final amount: $" + finalAmount.doubleValue());
 *         // Result is mathematically exact, no floating-point errors
 *     }
 * }
 * }</pre>
 *
 * <p><b>Attribution:</b>
 * This class includes code adapted from Apache Commons Lang, Google Guava, and other
 * open source projects under the Apache License 2.0. Methods from these libraries may have been
 * modified for consistency, performance optimization, and null-safety enhancement.
 *
 * @see Number
 * @see Comparable
 * @see Immutable
 * @see BigInteger
 * @see java.math.BigDecimal
 * @see java.math.MathContext
 */
@com.landawn.abacus.annotation.Immutable
public final class Fraction extends Number implements Comparable<Fraction>, Immutable {

    @Serial
    private static final long serialVersionUID = 65382027393090L;

    /**
     * {@code Fraction} representation of 0.
     */
    public static final Fraction ZERO = new Fraction(0, 1);
    /**
     * {@code Fraction} representation of 1.
     */
    public static final Fraction ONE = new Fraction(1, 1);
    /**
     * {@code Fraction} representation of 1/2.
     */
    public static final Fraction ONE_HALF = new Fraction(1, 2);
    /**
     * {@code Fraction} representation of 1/3.
     */
    public static final Fraction ONE_THIRD = new Fraction(1, 3);
    /**
     * {@code Fraction} representation of 2/3.
     */
    public static final Fraction TWO_THIRDS = new Fraction(2, 3);
    /**
     * {@code Fraction} representation of 1/4.
     */
    public static final Fraction ONE_QUARTER = new Fraction(1, 4);
    /**
     * {@code Fraction} representation of 2/4.
     */
    public static final Fraction TWO_QUARTERS = new Fraction(2, 4);
    /**
     * {@code Fraction} representation of 3/4.
     */
    public static final Fraction THREE_QUARTERS = new Fraction(3, 4);
    /**
     * {@code Fraction} representation of 1/5.
     */
    public static final Fraction ONE_FIFTH = new Fraction(1, 5);
    /**
     * {@code Fraction} representation of 2/5.
     */
    public static final Fraction TWO_FIFTHS = new Fraction(2, 5);
    /**
     * {@code Fraction} representation of 3/5.
     */
    public static final Fraction THREE_FIFTHS = new Fraction(3, 5);
    /**
     * {@code Fraction} representation of 4/5.
     */
    public static final Fraction FOUR_FIFTHS = new Fraction(4, 5);

    /**
     * The largest denominator {@link #of(double)} may produce; it bounds both the convergent search
     * and the semiconvergent that closes it out.
     */
    private static final int MAX_DENOMINATOR = 10_000;

    /**
     * The numerator number part of the fraction (the three in three sevenths).
     */
    private final int numerator;
    /**
     * The denominator number part of the fraction (the seven in three sevenths).
     */
    private final int denominator;

    /**
     * Cached output hashCode (class is immutable).
     */
    private transient int hashCode = 0;
    /**
     * Cached output toString (class is immutable).
     */
    private transient String toString = null;
    /**
     * Cached output toProperString (class is immutable).
     */
    private transient String toProperString = null;

    /**
     * <p>
     * Constructs a {@code Fraction} instance with the 2 parts of a fraction Y/Z.
     * </p>
     *
     * @param numerator the numerator, for example, the three in 'three sevenths'
     * @param denominator the denominator, for example, the seven in 'three sevenths'
     */
    private Fraction(final int numerator, final int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    /**
     * Creates a {@code Fraction} instance with the specified numerator and denominator,
     * without reducing the fraction. Any negative sign on the denominator is moved to
     * the numerator. For example, {@code Fraction.of(2, 4)} creates a fraction
     * representing 2/4 (not automatically reduced to 1/2), and {@code Fraction.of(3, -4)}
     * creates -3/4.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(3, 4);     // returns 3/4
     * Fraction f2 = Fraction.of(-5, 8);    // returns -5/8
     * Fraction f3 = Fraction.of(2, 4);     // returns 2/4 (not reduced to 1/2)
     * Fraction f4 = Fraction.of(0, 3);     // returns 0/3
     * Fraction.of(1, 0);                   // throws ArithmeticException
     * Fraction.of(1, Integer.MIN_VALUE);   // throws ArithmeticException
     * }</pre>
     *
     * <p><b>Note on {@code Integer.MIN_VALUE} denominators:</b> the sign is always moved to the
     * numerator, and {@code -Integer.MIN_VALUE} is not representable as an {@code int}, so this
     * non-reducing factory rejects {@code Fraction.of(n, Integer.MIN_VALUE)} outright.
     * {@link #of(int, int, boolean) of(n, Integer.MIN_VALUE, true)} accepts an <em>even</em> numerator,
     * because halving both terms first ({@code 2/-2^31} to {@code 1/-2^30}) makes the negation fit.
     * That rescue is only available when reduction is requested, since it changes the stored terms.</p>
     *
     * @param numerator the numerator of the fraction
     * @param denominator the denominator of the fraction, must not be zero
     * @return a new fraction instance
     * @throws ArithmeticException if the denominator is zero, or if the denominator is
     *         negative and either the numerator or the denominator equals
     *         {@code Integer.MIN_VALUE} (in which case negation would overflow)
     * @see #of(int, int, boolean)
     */
    public static Fraction of(final int numerator, final int denominator) throws ArithmeticException {
        return of(numerator, denominator, false);
    }

    /**
     * Creates a {@code Fraction} instance with the specified numerator and denominator,
     * with an option to reduce the fraction to its simplest form.
     *
     * <p>Any negative signs are resolved to be on the numerator. For example,
     * {@code Fraction.of(3, -4, false)} creates -3/4.</p>
     *
     * <p>When {@code reduce} is {@code true}, the fraction is simplified to its lowest terms
     * by dividing both numerator and denominator by their greatest common divisor.</p>
     *
     * <p>Reduction happens before sign normalization and range checking, so
     * {@code of(2, Integer.MIN_VALUE, true)} returns {@code -1/1073741824} whereas
     * {@link #of(int, int) of(2, Integer.MIN_VALUE)} throws.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(2, 4, false);    // returns 2/4
     * Fraction f2 = Fraction.of(2, 4, true);     // returns 1/2 (reduced)
     * Fraction f3 = Fraction.of(-6, -9, true);   // returns 2/3 (reduced and signs resolved)
     * Fraction f4 = Fraction.of(0, 5, true);     // returns ZERO (0/1)
     * Fraction.of(1, 0, true);                   // throws ArithmeticException
     * Fraction.of(1, Integer.MIN_VALUE, true);   // throws ArithmeticException
     * }</pre>
     *
     * @param numerator the numerator of the fraction
     * @param denominator the denominator of the fraction, must not be zero
     * @param reduce if {@code true}, reduces the fraction to its simplest form
     * @return a new fraction instance
     * @throws ArithmeticException if the denominator is zero, or if the final terms after
     *         optional reduction and sign normalization cannot be stored as an {@code int}
     */
    public static Fraction of(final int numerator, final int denominator, final boolean reduce) throws ArithmeticException {
        return fromTerms(numerator, denominator, reduce);
    }

    /**
     * @throws ArithmeticException if the denominator is zero, or the normalized terms after optional reduction cannot be represented with an int numerator and positive int denominator
     */
    private static Fraction fromTerms(long numerator, long denominator, final boolean reduce) throws ArithmeticException {
        if (denominator == 0) {
            throw new ArithmeticException("The denominator must not be zero");
        }

        // Reduce before resolving signs or narrowing: even a non-representable intermediate
        // numerator/denominator can describe a fraction whose final stored terms fit in int.
        if (reduce) {
            if (numerator == 0) {
                return ZERO;
            }
            final long gcd = greatestCommonDivisor(numerator, denominator);
            numerator /= gcd;
            denominator /= gcd;
        }

        if (denominator < 0) {
            numerator = -numerator;
            denominator = -denominator;
        }

        if (numerator < Integer.MIN_VALUE || numerator > Integer.MAX_VALUE || denominator > Integer.MAX_VALUE) {
            throw new ArithmeticException("Fraction terms exceed the int range: " + numerator + "/" + denominator);
        }
        return new Fraction((int) numerator, (int) denominator);
    }

    /**
     * Creates a {@code Fraction} instance representing a mixed fraction (whole and fractional parts).
     * The fraction is not reduced. For example, {@code Fraction.ofMixed(1, 2, 4)} creates the
     * fraction equivalent to 1 + 2/4 = 6/4.
     *
     * <p><b>Values between {@code -1} and {@code 0} cannot be expressed.</b> The sign is carried
     * solely by {@code whole}, and an {@code int} cannot distinguish {@code 0} from {@code -0}, so
     * {@code ofMixed(0, 1, 2)} is {@code +1/2} and there is no argument triple that yields
     * {@code -1/2}. Use {@link #of(int, int) of(-1, 2)} for those values, or
     * {@link #of(String) of("-0 1/2")}, where the written sign survives.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.ofMixed(1, 3, 4);    // returns 7/4
     * Fraction f2 = Fraction.ofMixed(-2, 1, 3);   // returns -7/3
     * Fraction f3 = Fraction.ofMixed(0, 1, 2);    // returns 1/2 - a zero whole part cannot be negative
     * Fraction.ofMixed(0, 1, 0);                  // throws ArithmeticException
     * Fraction.ofMixed(1, -6, -10);               // throws ArithmeticException
     * }</pre>
     *
     * @param whole the whole number part (use a negative value for a negative mixed fraction; a zero
     *              whole part always yields a non-negative fraction)
     * @param numerator the numerator of the fractional part, must be non-negative
     * @param denominator the denominator of the fractional part, must be positive
     * @return a new fraction instance
     * @throws ArithmeticException if the denominator is zero, if the denominator is negative,
     *         if the numerator is negative, or if the resulting numerator would overflow the range of an {@code int}
     * @see #ofMixed(int, int, int, boolean)
     * @see #of(int, int)
     */
    public static Fraction ofMixed(final int whole, final int numerator, final int denominator) throws ArithmeticException {
        return ofMixed(whole, numerator, denominator, false);
    }

    /**
     * Creates a {@code Fraction} instance representing a mixed fraction (whole and fractional parts).
     *
     * @param whole the whole number part (use negative value for a negative mixed fraction)
     * @param numerator the numerator of the fractional part, must be non-negative
     * @param denominator the denominator of the fractional part, must be positive
     * @return a new fraction instance
     * @throws ArithmeticException if the denominator is zero, if the denominator is negative,
     *         if the numerator is negative, or if the resulting numerator would overflow the range of an {@code int}
     * @deprecated renamed to {@link #ofMixed(int, int, int)}. Under the old name, {@code Fraction.of(1, 2, 3)}
     *             (whole, numerator, denominator = 5/3) and {@code Fraction.of(1, 2, true)}
     *             (numerator, denominator, reduce = 1/2) gave the first two arguments different meanings
     *             depending on the third argument's type.
     */
    @Deprecated
    public static Fraction of(final int whole, final int numerator, final int denominator) throws ArithmeticException {
        return ofMixed(whole, numerator, denominator, false);
    }

    /**
     * Creates a {@code Fraction} instance representing a mixed fraction with optional reduction.
     * The negative sign must be on the whole number part if the fraction is negative.
     *
     * <p>The calculation is performed as follows:</p>
     * <ul>
     * <li>If whole is positive: result = whole × denominator + numerator</li>
     * <li>If whole is negative: result = whole × denominator - numerator</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.ofMixed(1, 2, 4, false);   // returns 6/4
     * Fraction f2 = Fraction.ofMixed(1, 2, 4, true);    // returns 3/2 (reduced)
     * Fraction f3 = Fraction.ofMixed(-1, 1, 2, true);   // returns -3/2
     * Fraction.ofMixed(0, 0, 0, true);                  // throws ArithmeticException
     * Fraction.ofMixed(0, -1, 2, true);                 // throws ArithmeticException
     * }</pre>
     *
     * @param whole the whole number part (negative sign goes here for negative fractions)
     * @param numerator the numerator of the fractional part (must be non-negative)
     * @param denominator the denominator of the fractional part (must be positive)
     * @param reduce if {@code true}, reduces the resulting fraction to its simplest form
     * @return a new fraction instance
     * @throws ArithmeticException if the denominator is zero, the denominator is negative,
     *         the numerator is negative, or if the resulting numerator after optional reduction would overflow
     * @see #ofMixed(int, int, int)
     * @see #of(int, int, boolean)
     */
    public static Fraction ofMixed(final int whole, final int numerator, final int denominator, final boolean reduce) throws ArithmeticException {
        if (denominator == 0) {
            throw new ArithmeticException("The denominator must not be zero");
        }
        if (denominator < 0) {
            throw new ArithmeticException("The denominator must not be negative");
        }
        if (numerator < 0) {
            throw new ArithmeticException("The numerator must not be negative");
        }
        long numeratorValue;
        if (whole < 0) {
            numeratorValue = whole * (long) denominator - numerator;
        } else {
            numeratorValue = whole * (long) denominator + numerator;
        }
        return fromTerms(numeratorValue, denominator, reduce);
    }

    /**
     * Creates a {@code Fraction} instance representing a mixed fraction with optional reduction.
     *
     * @param whole the whole number part (negative sign goes here for negative fractions)
     * @param numerator the numerator of the fractional part (must be non-negative)
     * @param denominator the denominator of the fractional part (must be positive)
     * @param reduce if {@code true}, reduces the resulting fraction to its simplest form
     * @return a new fraction instance
     * @throws ArithmeticException if the denominator is zero or negative, the numerator is negative, or the resulting numerator after optional reduction exceeds the int range.
     * @deprecated renamed to {@link #ofMixed(int, int, int, boolean)}; see {@link #of(int, int, int)}.
     */
    @Deprecated
    public static Fraction of(final int whole, final int numerator, final int denominator, final boolean reduce) throws ArithmeticException {
        return ofMixed(whole, numerator, denominator, reduce);
    }

    /**
     * Creates a {@code Fraction} instance from a {@code double} value using the continued
     * fraction algorithm.
     *
     * <p><b>Approximation contract:</b> the result is the fraction <em>closest to {@code value} by
     * absolute difference</em> among all fractions whose denominator is at most 10,000 and whose
     * numerator fits in an {@code int}. When two such fractions are equally close, the one with the
     * smaller denominator is returned, then the numerator closest to zero. The comparison uses the
     * exact binary value of the double: {@code of(0.00005)} returns {@code 1/10000}, since that double
     * is slightly above the exact midpoint. The result is always reduced to its simplest form.</p>
     *
     * <p>Both bounds apply to the entire fraction. For large values the numerator bound can require
     * a coarser approximation and can change the whole-number part. For example,
     * {@code of(1000000000.25)} returns {@code 1000000000/1}: the equally close fraction with
     * denominator two loses the denominator tie-break. Every finite value in the supported range
     * has an integer candidate, so approximation itself does not fail.</p>
     *
     * <p><b>The denominator bound is a real limit, not a formality.</b> A value that is not itself a
     * fraction with a small denominator can only be approximated: {@code of(Math.PI)} returns
     * {@code 355/113}, and values whose magnitude is below the exact rational {@code 1/20000}
     * return {@code 0/1}. The additional numerator bound can increase the error up to one half.
     * Use {@link java.math.BigDecimal} when the exact
     * decimal value must be preserved.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(0.5);          // returns 1/2
     * Fraction f2 = Fraction.of(0.333);        // returns 333/1000
     * Fraction f3 = Fraction.of(3.14159);      // returns 9563/3044 (an approximation of pi)
     * Fraction f4 = Fraction.of(0.99991);      // returns 9999/10000, not 1/1
     * Fraction f5 = Fraction.of(0.00001);      // returns 0/1 - no closer fraction fits the bound
     * Fraction.of(Double.NaN);                 // throws ArithmeticException
     * Fraction.of(Double.POSITIVE_INFINITY);   // throws ArithmeticException
     * }</pre>
     *
     * @param value the double value to convert to a fraction
     * @return the closest fraction to {@code value} whose denominator is at most 10,000 and whose
     *         numerator is representable as an {@code int}, in reduced form
     * @throws ArithmeticException if the value is outside the inclusive range
     *         [{@link Integer#MIN_VALUE}, {@link Integer#MAX_VALUE}], is {@code NaN}, or is infinite
     */
    public static Fraction of(final double value) throws ArithmeticException {
        if (!Double.isFinite(value) || value < Integer.MIN_VALUE || value > Integer.MAX_VALUE) {
            throw new ArithmeticException("The value must be finite and between Integer.MIN_VALUE and Integer.MAX_VALUE");
        }
        if (value == 0) {
            return of(0, 1);
        }
        // Work with the exact binary rational, so rounding near a midpoint cannot choose the wrong neighbor.
        final int sign = value < 0 ? -1 : 1;
        final long numeratorLimit = sign < 0 ? -(long) Integer.MIN_VALUE : Integer.MAX_VALUE;
        final long bits = Double.doubleToLongBits(Math.abs(value));
        final int encodedExponent = (int) ((bits >>> 52) & 0x7ff);
        final long significand = (bits & ((1L << 52) - 1)) | (encodedExponent == 0 ? 0 : 1L << 52);
        final int exponent = encodedExponent == 0 ? -1074 : encodedExponent - 1075;
        final BigInteger originalNumerator = exponent >= 0 ? BigInteger.valueOf(significand).shiftLeft(exponent) : BigInteger.valueOf(significand);
        final BigInteger originalDenominator = exponent >= 0 ? BigInteger.ONE : BigInteger.ONE.shiftLeft(-exponent);
        BigInteger numerator = originalNumerator;
        BigInteger denominator = originalDenominator;
        long p0 = 0, q0 = 1, p1 = 1, q1 = 0;
        while (true) {
            final BigInteger[] step = numerator.divideAndRemainder(denominator);
            long limit = Long.MAX_VALUE;
            if (p1 != 0) {
                limit = Math.min(limit, (numeratorLimit - p0) / p1);
            }
            if (q1 != 0) {
                limit = Math.min(limit, (MAX_DENOMINATOR - q0) / q1);
            }
            if (step[0].compareTo(BigInteger.valueOf(limit)) > 0) {
                // At either bound, the previous convergent and the last admissible semiconvergent
                // bracket all better candidates. Compare their errors exactly, without division.
                final long p2 = p0 + limit * p1;
                final long q2 = q0 + limit * q1;
                final BigInteger error1 = originalNumerator.multiply(BigInteger.valueOf(q1))
                        .subtract(originalDenominator.multiply(BigInteger.valueOf(p1)))
                        .abs();
                final BigInteger error2 = originalNumerator.multiply(BigInteger.valueOf(q2))
                        .subtract(originalDenominator.multiply(BigInteger.valueOf(p2)))
                        .abs();
                final int comparison = error2.multiply(BigInteger.valueOf(q1)).compareTo(error1.multiply(BigInteger.valueOf(q2)));
                if (comparison < 0 || (comparison == 0 && (q2 < q1 || (q2 == q1 && p2 < p1)))) {
                    return of((int) (sign * p2), (int) q2, true);
                }
                return of((int) (sign * p1), (int) q1, true);
            }
            final long coefficient = step[0].longValueExact();
            final long p2 = p0 + coefficient * p1;
            final long q2 = q0 + coefficient * q1;
            if (step[1].signum() == 0) {
                return of((int) (sign * p2), (int) q2, true);
            }
            p0 = p1;
            q0 = q1;
            p1 = p2;
            q1 = q2;
            numerator = denominator;
            denominator = step[1];
        }
    }

    /**
     * Creates a {@code Fraction} from a string representation. Multiple formats are supported
     * to accommodate different fraction notations.
     *
     * <p>Accepted formats:</p>
     * <ul>
     * <li>Decimal format: "0.5", "-2.75" (a token containing a decimal point but no {@code '/'}; parsed
     *     with {@link Double#parseDouble} and then approximated by {@link #of(double)})</li>
     * <li>Mixed fraction: "1 3/4", "-2 1/3" (whole number followed by space and fraction)</li>
     * <li>Simple fraction: "3/4", "-7/8" (numerator/denominator)</li>
     * <li>Whole number: "5", "-12" (treated as a fraction with denominator 1)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of("3/4");     // returns 3/4
     * Fraction f2 = Fraction.of("1 2/3");   // returns 5/3
     * Fraction f3 = Fraction.of("0.25");    // returns 1/4
     * Fraction f4 = Fraction.of("-5");      // returns -5/1
     * Fraction.of(" 3");                   // throws NumberFormatException (whitespace is significant)
     * Fraction.of(null);                    // throws IllegalArgumentException
     * Fraction.of("invalid");               // throws NumberFormatException
     * Fraction.of("1e-3");                  // throws NumberFormatException (no decimal point)
     * Fraction.of("1.0/2.0");               // throws NumberFormatException ("1.0" is not an integer)
     * }</pre>
     *
     * <p>A token containing {@code '/'} is always read as a fraction, never as a decimal, so the
     * components of {@code "1.0/2.0"} are reported individually rather than the whole token being
     * rejected as a malformed decimal number.</p>
     *
     * <p>In the mixed {@code "X Y/Z"} form the sign belongs to the whole number and negates the whole
     * value, so {@code "-2 1/3"} is {@code -7/3} and not {@code -6/3 + 1/3}. A written minus sign is
     * honoured even on a zero whole part: {@code "-0 1/2"} parses as {@code -1/2}. That case is
     * carried by the string alone &mdash; {@link #ofMixed(int, int, int)} takes an {@code int} whole
     * part, which cannot distinguish {@code 0} from {@code -0} and therefore cannot express any value
     * between {@code -1} and {@code 0}.</p>
     *
     * @param str the string to parse, must not be {@code null}. Whitespace is significant: a space
     *            separates the whole number of the {@code "X Y/Z"} form, so padded input such as
     *            {@code " 3"} or {@code "2 "} is rejected
     * @return a new fraction instance
     * @throws IllegalArgumentException if {@code str} is {@code null}.
     * @throws NumberFormatException if the string is not in a recognized format, or if
     *         an integer component is out of range, or if the decimal form cannot be parsed
     * @throws ArithmeticException if the parsed fraction is invalid (e.g. a zero denominator or a
     *         negative numerator in the mixed {@code "X Y/Z"} form), or if the decimal form is
     *         outside the representable range
     */
    public static Fraction of(final String str) throws IllegalArgumentException, NumberFormatException, ArithmeticException {
        if (str == null) {
            throw new IllegalArgumentException("The string must not be null");
        }

        // Note: whitespace is significant. ' ' separates the whole number of the "X Y/Z" form, so a
        // padded token such as " 3" or "2 " is a malformed mixed number, not an integer to be trimmed.

        // A '/' means the caller wrote a fraction, so route to the fraction parsers even when a '.' is
        // also present. Dispatching on '.' first sent "1.0/2.0" to the decimal parser, which reported it
        // as a malformed decimal rather than naming the non-integer component that is actually wrong.
        // (No exception type changes: every component of a slash-bearing token is parsed with
        // Integer.parseInt, and a token containing '.' can never satisfy Double.parseDouble with a '/'
        // in it either, so both routes raise NumberFormatException.)
        final int slashPos = str.indexOf('/');

        // parse double format
        if (slashPos < 0 && str.indexOf('.') >= 0) {
            return of(parseDoublePart(str));
        }

        // parse X Y/Z format
        int pos = str.indexOf(' ');
        if (pos > 0) {
            final String wholePart = str.substring(0, pos);
            final int whole = parseIntPart(wholePart, str);
            final String fractionPart = str.substring(pos + 1);
            pos = fractionPart.indexOf('/');
            if (pos < 0) {
                throw new NumberFormatException("The fraction \"" + str + "\" could not be parsed as the format X Y/Z");
            }

            final int numer = parseIntPart(fractionPart.substring(0, pos), str);
            final int denom = parseIntPart(fractionPart.substring(pos + 1), str);
            final Fraction result = ofMixed(whole, numer, denom);

            // "-0 1/2" means -1/2, but Integer.parseInt("-0") is 0, so by the time the whole part is an
            // int the sign is gone and ofMixed - whose only sign carrier is that int - can no longer
            // express it. The written '-' is the sole remaining record, so apply it here. Only the
            // zero whole part needs this: any other negative whole already carries the sign, and the
            // numerator is non-negative by ofMixed's contract. Apply the sign without invoking
            // arithmetic normalization: parsing preserves the caller's unreduced terms.
            return whole == 0 && wholePart.charAt(0) == '-' ? fromTerms(-(long) result.numerator, result.denominator, false) : result;
        }

        // parse Y/Z format
        if (slashPos < 0) {
            // simple whole number
            return of(parseIntPart(str, str), 1);
        }

        final int numer = parseIntPart(str.substring(0, slashPos), str);
        final int denom = parseIntPart(str.substring(slashPos + 1), str);
        return of(numer, denom);
    }

    /**
     * Parses one integer component of a fraction string, reporting the whole input on failure.
     *
     * @param part the component to parse
     * @param source the complete string being parsed, used for the error message
     * @return the parsed value
     * @throws NumberFormatException if {@code part} is not a valid {@code int}
     */
    private static int parseIntPart(final String part, final String source) throws NumberFormatException {
        try {
            return Integer.parseInt(part);
        } catch (final NumberFormatException e) {
            throw new NumberFormatException("The fraction \"" + source + "\" could not be parsed: \"" + part + "\" is not an integer");
        }
    }

    /**
     * Parses the decimal form of a fraction string, naming the input in the failure message.
     *
     * @param source the decimal token to parse, which is also the complete string being parsed
     * @return the parsed value
     * @throws NumberFormatException if {@code source} is not a valid {@code double}
     */
    private static double parseDoublePart(final String source) throws NumberFormatException {
        try {
            return Double.parseDouble(source);
        } catch (final NumberFormatException e) {
            throw new NumberFormatException("The fraction \"" + source + "\" could not be parsed as a decimal number");
        }
    }

    // Accessors
    //-------------------------------------------------------------------

    /**
     * Gets the numerator part of the fraction.
     * This method may return a value greater than the denominator, representing an improper
     * fraction such as 7 in the fraction 7/4.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f = Fraction.of(7, 4);
     * int n = f.getNumerator();   // returns 7
     * Fraction f2 = Fraction.of(-3, 5);
     * int n2 = f2.getNumerator(); // returns -3
     * }</pre>
     *
     * @return the numerator of the fraction
     * @deprecated replaced by {@link #numerator()}
     */
    @Deprecated
    public int getNumerator() {
        return numerator;
    }

    /**
     * Gets the numerator part of the fraction.
     * This method may return a value greater than the denominator, representing an improper
     * fraction such as 7 in the fraction 7/4.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f = Fraction.of(7, 4);
     * int n = f.numerator();   // returns 7
     * Fraction f2 = Fraction.of(-3, 5);
     * int n2 = f2.numerator(); // returns -3
     * Fraction f3 = Fraction.of(0, 8);
     * int n3 = f3.numerator(); // returns 0
     * }</pre>
     *
     * @return the numerator of the fraction
     */
    public int numerator() {
        return numerator;
    }

    /**
     * Gets the denominator part of the fraction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f = Fraction.of(3, 8);
     * int d = f.getDenominator();   // returns 8
     * Fraction f2 = Fraction.of(5, 12);
     * int d2 = f2.getDenominator(); // returns 12
     * }</pre>
     *
     * @return the denominator of the fraction
     * @deprecated replaced by {@link #denominator()}
     */
    @Deprecated
    public int getDenominator() {
        return denominator;
    }

    /**
     * Gets the denominator part of the fraction.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f = Fraction.of(3, 8);
     * int d = f.denominator();   // returns 8
     * Fraction f2 = Fraction.of(5, 12);
     * int d2 = f2.denominator(); // returns 12
     * Fraction f3 = Fraction.of(0, 1);
     * int d3 = f3.denominator(); // returns 1
     * }</pre>
     *
     * @return the denominator of the fraction
     */
    public int denominator() {
        return denominator;
    }

    /**
     * Gets the non-negative proper numerator of the fraction.
     * An improper fraction like 7/4 can be expressed as the mixed number 1 3/4.
     * This method returns the numerator of the fractional part (3 in this example).
     *
     * <p>For negative fractions like -7/4 (which equals -1 3/4), this method still
     * returns the positive proper numerator 3.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(7, 4);     // returns 7/4 = 1 3/4
     * int pn1 = f1.getProperNumerator();   // returns 3
     *
     * Fraction f2 = Fraction.of(-7, 4);    // returns -7/4 = -1 3/4
     * int pn2 = f2.getProperNumerator();   // returns 3
     *
     * Fraction f3 = Fraction.of(8, 4);     // returns 8/4 = 2
     * int pn3 = f3.getProperNumerator();   // returns 0
     * }</pre>
     *
     * @return the non-negative proper numerator
     * @deprecated replaced by {@link #properNumerator()}
     */
    @Deprecated
    public int getProperNumerator() {
        return Math.abs(numerator % denominator);
    }

    /**
     * Gets the non-negative proper numerator of the fraction.
     * An improper fraction like 7/4 can be expressed as the mixed number 1 3/4.
     * This method returns the numerator of the fractional part (3 in this example).
     *
     * <p>For negative fractions like -7/4 (which equals -1 3/4), this method still
     * returns the positive proper numerator 3.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(7, 4);    // returns 7/4 = 1 3/4
     * int pn1 = f1.properNumerator();     // returns 3
     *
     * Fraction f2 = Fraction.of(-7, 4);   // returns -7/4 = -1 3/4
     * int pn2 = f2.properNumerator();     // returns 3
     *
     * Fraction f3 = Fraction.of(8, 4);    // returns 8/4 = 2
     * int pn3 = f3.properNumerator();     // returns 0
     * }</pre>
     *
     * @return the non-negative proper numerator
     */
    public int properNumerator() {
        return Math.abs(numerator % denominator);
    }

    /**
     * Gets the whole number part of the fraction.
     * An improper fraction like 7/4 can be expressed as the mixed number 1 3/4.
     * This method returns the whole number part (1 in this example).
     *
     * <p>For negative fractions like -7/4 (which equals -1 3/4), this method
     * returns -1.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(7, 4);    // returns 7/4 = 1 3/4
     * int w1 = f1.getProperWhole();       // returns 1
     *
     * Fraction f2 = Fraction.of(-7, 4);   // returns -7/4 = -1 3/4
     * int w2 = f2.getProperWhole();       // returns -1
     *
     * Fraction f3 = Fraction.of(3, 4);    // returns 3/4
     * int w3 = f3.getProperWhole();       // returns 0
     * }</pre>
     *
     * @return the whole number part of the fraction
     * @deprecated replaced by {@link #properWhole()}
     */
    @Deprecated
    public int getProperWhole() {
        return numerator / denominator;
    }

    /**
     * Gets the whole number part of the fraction.
     * An improper fraction like 7/4 can be expressed as the mixed number 1 3/4.
     * This method returns the whole number part (1 in this example).
     *
     * <p>For negative fractions like -7/4 (which equals -1 3/4), this method
     * returns -1.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(7, 4);    // returns 7/4 = 1 3/4
     * int w1 = f1.properWhole();          // returns 1
     *
     * Fraction f2 = Fraction.of(-7, 4);   // returns -7/4 = -1 3/4
     * int w2 = f2.properWhole();          // returns -1
     *
     * Fraction f3 = Fraction.of(3, 4);    // returns 3/4
     * int w3 = f3.properWhole();          // returns 0
     * }</pre>
     *
     * @return the whole number part of the fraction
     */
    public int properWhole() {
        return numerator / denominator;
    }

    // Number methods
    //-------------------------------------------------------------------

    /**
     * Gets the fraction as an {@code int} value by performing integer division.
     * This returns only the whole number part of the fraction, discarding any remainder.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(7, 4);     // returns 7/4 = 1.75
     * int i1 = f1.intValue();              // returns 1
     *
     * Fraction f2 = Fraction.of(-10, 3);   // returns -10/3 = -3.333...
     * int i2 = f2.intValue();              // returns -3
     *
     * Fraction f3 = Fraction.of(5, 1);     // returns 5/1
     * int i3 = f3.intValue();              // returns 5
     *
     * Fraction f4 = Fraction.of(3, 4);     // returns 3/4
     * int i4 = f4.intValue();              // returns 0
     * }</pre>
     *
     * @return the whole number part of the fraction as an int
     */
    @Override
    public int intValue() {
        return numerator / denominator;
    }

    /**
     * Gets the fraction as a {@code long} value by performing integer division.
     * This returns only the whole number part of the fraction, discarding any remainder.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(7, 4);     // returns 7/4 = 1.75
     * long l1 = f1.longValue();            // returns 1L
     *
     * Fraction f2 = Fraction.of(-10, 3);   // returns -10/3 = -3.333...
     * long l2 = f2.longValue();            // returns -3L
     *
     * Fraction f3 = Fraction.of(5, 1);     // returns 5/1
     * long l3 = f3.longValue();            // returns 5L
     *
     * Fraction f4 = Fraction.of(3, 4);     // returns 3/4
     * long l4 = f4.longValue();            // returns 0L
     * }</pre>
     *
     * @return the whole number part of the fraction as a long
     */
    @Override
    public long longValue() {
        return (long) numerator / denominator;
    }

    /**
     * Gets the fraction as a {@code float} value by performing floating-point division.
     * This calculates the decimal representation of the fraction.
     *
     * <p><b>Accuracy:</b> the quotient is computed as a {@code double} and narrowed once, which is
     * {@link #doubleValue()} rounded to {@code float}. Both {@code int} terms are exact as
     * {@code double}s and the {@code double} quotient is correctly rounded, so this is the nearest
     * {@code float} to {@code doubleValue()}. It is <em>not</em> guaranteed to be the nearest
     * {@code float} to the exact rational: the two successive roundings can land one ULP away when the
     * exact value sits extremely close to a {@code float} midpoint (for example
     * {@code Fraction.of(2125113837, 2125114027)}). Such cases are rare &mdash; none occurred in
     * 200,000 uniformly random {@code int}/{@code int} pairs &mdash; but if exact rounding of the
     * rational is required, use {@link #doubleValue()} or divide the terms with
     * {@link java.math.BigDecimal}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(1, 3);    // returns 1/3
     * float v1 = f1.floatValue();         // returns 0.33333334f
     *
     * Fraction f2 = Fraction.of(3, 4);    // returns 3/4
     * float v2 = f2.floatValue();         // returns 0.75f
     *
     * Fraction f3 = Fraction.of(0, 1);    // returns 0/1
     * float v3 = f3.floatValue();         // returns 0.0f
     *
     * Fraction f4 = Fraction.of(-1, 2);   // returns -1/2
     * float v4 = f4.floatValue();         // returns -0.5f
     *
     * // Large terms: rounding each term to float first would lose two ULPs here
     * Fraction f5 = Fraction.of(16777217, 16777219);
     * float v5 = f5.floatValue();         // returns 0.9999999f
     * }</pre>
     *
     * @return the fraction as a float value
     * @see #doubleValue()
     */
    @Override
    public float floatValue() {
        // Divide in double and narrow once, rather than dividing two floats. Narrowing the terms first
        // rounds each to 24-bit precision before the division, which loses up to ~2 ULPs and differed
        // from the correctly rounded result for ~34% of uniformly random int/int pairs.
        return (float) doubleValue();
    }

    /**
     * Gets the fraction as a {@code double} value by performing floating-point division.
     * This calculates the decimal representation of the fraction with higher precision
     * than {@link #floatValue()}.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(1, 3);    // returns 1/3
     * double v1 = f1.doubleValue();       // returns 0.3333333333333333
     *
     * Fraction f2 = Fraction.of(22, 7);   // returns 22/7 (approximation of pi)
     * double v2 = f2.doubleValue();       // returns 3.142857142857143
     *
     * Fraction f3 = Fraction.of(0, 1);    // returns 0/1
     * double v3 = f3.doubleValue();       // returns 0.0
     *
     * Fraction f4 = Fraction.of(-3, 4);   // returns -3/4
     * double v4 = f4.doubleValue();       // returns -0.75
     * }</pre>
     *
     * @return the fraction as a double value
     */
    @Override
    public double doubleValue() {
        return (double) numerator / (double) denominator;
    }

    // Calculations
    //-------------------------------------------------------------------

    /**
     * Reduces this fraction to its simplest form by dividing both numerator and denominator
     * by their greatest common divisor (GCD). Returns a new fraction instance if reduction
     * is possible, otherwise returns this instance.
     *
     * <p>For example, 6/8 reduces to 3/4, and 15/25 reduces to 3/5.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(6, 8);
     * Fraction r1 = f1.reduce();   // returns 3/4
     *
     * Fraction f2 = Fraction.of(7, 13);
     * Fraction r2 = f2.reduce();   // returns 7/13 (already in simplest form)
     *
     * Fraction f3 = Fraction.of(0, 5);
     * Fraction r3 = f3.reduce();   // returns ZERO (0/1)
     *
     * Fraction f4 = Fraction.of(15, 25);
     * Fraction r4 = f4.reduce();   // returns 3/5
     * }</pre>
     *
     * @return this instance if it is already in lowest terms, {@link #ZERO} if the numerator is zero,
     *         otherwise a new fraction holding the reduced terms
     */
    public Fraction reduce() {
        if (numerator == 0) {
            return equals(ZERO) ? this : ZERO;
        }
        final long gcd = greatestCommonDivisor(numerator, denominator);
        if (gcd == 1) {
            return this;
        }
        return new Fraction((int) (numerator / gcd), (int) (denominator / gcd));
    }

    /**
     * Returns the multiplicative inverse (reciprocal) of this fraction.
     * For a fraction a/b, the inverse is b/a. The returned fraction is reduced.
     *
     * <p>Special handling for negative fractions: the negative sign is moved to the
     * numerator in the result.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(3, 4);
     * Fraction i1 = f1.invert();   // returns 4/3
     *
     * Fraction f2 = Fraction.of(-2, 5);
     * Fraction i2 = f2.invert();   // returns -5/2
     *
     * Fraction f3 = Fraction.of(1, 7);
     * Fraction i3 = f3.invert();    // returns 7/1
     *
     * Fraction.of(0, 1).invert();   // throws ArithmeticException
     * }</pre>
     *
     * @return a new fraction that is the inverse of this fraction
     * @throws ArithmeticException if this fraction is zero, or the reduced reciprocal cannot be represented with an int numerator and positive int denominator
     */
    public Fraction invert() throws ArithmeticException {
        return fromTerms(denominator, numerator, true);
    }

    /**
     * Returns the additive inverse (negative) of this fraction.
     * For a fraction a/b, the negative is -a/b. The returned fraction is reduced.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(3, 4);
     * Fraction n1 = f1.negate();   // returns -3/4
     *
     * Fraction f2 = Fraction.of(-2, 5);
     * Fraction n2 = f2.negate();   // returns 2/5
     *
     * Fraction f3 = Fraction.of(0, 1);
     * Fraction n3 = f3.negate();                    // returns 0/1
     *
     * Fraction.of(Integer.MIN_VALUE, 1).negate();   // throws ArithmeticException
     * }</pre>
     *
     * @return the negated fraction in reduced form; the shared {@link #ZERO} constant when this fraction
     *         is zero
     * @throws ArithmeticException if the reduced result cannot be represented with int terms
     */
    public Fraction negate() throws ArithmeticException {
        return fromTerms(-(long) numerator, denominator, true);
    }

    /**
     * Returns the absolute value of this fraction.
     * Returns this instance when it is already non-negative and reduced.
     * The returned fraction is always reduced.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(-3, 4);
     * Fraction a1 = f1.abs();   // returns 3/4
     *
     * Fraction f2 = Fraction.of(2, 5);
     * Fraction a2 = f2.abs();   // returns 2/5 (same instance)
     *
     * Fraction f3 = Fraction.of(0, 1);
     * Fraction a3 = f3.abs();                    // returns 0/1 (same instance)
     *
     * Fraction.of(Integer.MIN_VALUE, 1).abs();   // throws ArithmeticException
     * }</pre>
     *
     * @return the reduced, non-negative fraction
     * @throws ArithmeticException if the reduced result cannot be represented with int terms
     */
    public Fraction abs() throws ArithmeticException {
        if (numerator >= 0) {
            return reduce();
        }
        return negate();
    }

    /**
     * Raises this fraction to the specified integer power, returning a reduced result.
     *
     * <p>Special cases:</p>
     * <ul>
     * <li>Any fraction to the power of 0 equals 1 (even 0/1)</li>
     * <li>Any fraction to the power of 1 equals itself in value; the result is reduced, so
     * {@code Fraction.of(2, 4).pow(1)} returns {@code 1/2} and is not {@link #equals(Object) equal}
     * to the receiver</li>
     * <li>Negative powers: (a/b)^(-n) = (b/a)^n</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f = Fraction.of(2, 3);
     * Fraction p1 = f.pow(2);      // returns 4/9
     * Fraction p2 = f.pow(-1);     // returns 3/2
     * Fraction p3 = f.pow(0);      // returns 1/1
     * Fraction.of(0, 1).pow(5);    // returns 0/1
     * Fraction.of(0, 1).pow(-1);   // throws ArithmeticException
     * }</pre>
     *
     * @param power the power to raise the fraction to
     * @return the fraction raised to the given power
     * @throws ArithmeticException if the power is negative and the fraction is zero,
     *         or if the calculation results in integer overflow
     */
    public Fraction pow(final int power) throws ArithmeticException {
        if (power == 1) {
            return reduce();
        } else if (power == 0) {
            return ONE;
        }

        long exponent = power < 0 ? -(long) power : power;
        Fraction base = power < 0 ? invert() : reduce();
        Fraction result = ONE;

        while (exponent != 0) {
            if ((exponent & 1) != 0) {
                result = result.multipliedBy(base);
            }
            exponent >>>= 1;
            // An unused final square may overflow even when the requested power fits, e.g. (-2)^31.
            if (exponent != 0) {
                base = base.multipliedBy(base);
            }
        }

        return result;
    }

    // All callers supply int terms, products of int terms, or sums of two cross-products
    // with positive int denominators. Their absolute values fit in long, including gcd 2^31.
    private static long greatestCommonDivisor(long u, long v) {
        u = Math.abs(u);
        v = Math.abs(v);
        while (v != 0) {
            final long remainder = u % v;
            u = v;
            v = remainder;
        }
        return u;
    }

    /**
     * Adds this fraction to another fraction and returns the result in reduced form.
     *
     * <p>The addition is performed using the standard formula: a/b + c/d = (ad + bc) / bd,
     * with reduction before checking whether the final terms fit in an {@code int}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(1, 2);   // returns 1/2
     * Fraction f2 = Fraction.of(1, 3);   // returns 1/3
     * Fraction sum = f1.add(f2);         // returns 5/6
     *
     * Fraction f3 = Fraction.of(5, 8);
     * Fraction f4 = Fraction.of(3, 8);
     * Fraction sum2 = f3.add(f4);            // returns 1/1
     *
     * Fraction f5 = Fraction.of(0, 1);
     * Fraction sum3 = f5.add(Fraction.of(3, 4));   // returns 3/4
     *
     * Fraction.of(1, 2).add(null);                 // throws IllegalArgumentException
     * }</pre>
     *
     * @param fraction the fraction to add to this fraction (must not be {@code null})
     * @return the sum in reduced form
     * @throws IllegalArgumentException if the fraction parameter is {@code null}.
     * @throws ArithmeticException if the reduced result cannot be represented with an int numerator and positive int denominator
     */
    public Fraction add(final Fraction fraction) throws IllegalArgumentException, ArithmeticException {
        return addSub(fraction, true /* add */);
    }

    /**
     * Subtracts another fraction from this fraction and returns the result in reduced form.
     *
     * <p>The subtraction is performed using the standard formula: a/b - c/d = (ad - bc) / bd,
     * with reduction before checking whether the final terms fit in an {@code int}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(3, 4);   // returns 3/4
     * Fraction f2 = Fraction.of(1, 2);   // returns 1/2
     * Fraction diff = f1.subtract(f2);   // returns 1/4
     *
     * Fraction f3 = Fraction.of(1, 3);
     * Fraction f4 = Fraction.of(2, 3);
     * Fraction diff2 = f3.subtract(f4);      // returns -1/3
     *
     * Fraction f5 = Fraction.of(5, 8);
     * Fraction diff3 = f5.subtract(Fraction.of(0, 1));   // returns 5/8
     *
     * Fraction.of(3, 4).subtract(null);                  // throws IllegalArgumentException
     * }</pre>
     *
     * @param fraction the fraction to subtract from this fraction (must not be {@code null})
     * @return the difference in reduced form
     * @throws IllegalArgumentException if the fraction parameter is {@code null}.
     * @throws ArithmeticException if the reduced result cannot be represented with an int numerator and positive int denominator
     */
    public Fraction subtract(final Fraction fraction) throws IllegalArgumentException, ArithmeticException {
        return addSub(fraction, false /* subtract */);
    }

    /**
     * Implements addition and subtraction with exact intermediate terms.
     *
     * @param fraction the fraction to add or subtract, must not be {@code null}
     * @param isAdd {@code true} to add, {@code false} to subtract
     * @return a {@code Fraction} instance with the resulting values
     * @throws IllegalArgumentException if {@code fraction} is {@code null}.
     * @throws ArithmeticException if the reduced result cannot be represented with an int numerator and positive int denominator
     */
    private Fraction addSub(final Fraction fraction, final boolean isAdd) throws IllegalArgumentException, ArithmeticException {
        if (fraction == null) {
            throw new IllegalArgumentException("The fraction must not be null"); //NOSONAR
        }
        if (numerator == 0) {
            return isAdd ? fraction.reduce() : fraction.negate();
        }
        if (fraction.numerator == 0) {
            return reduce();
        }
        // Positive int denominators bound the sum of both cross-products within long.
        final long left = (long) numerator * fraction.denominator;
        final long right = (long) fraction.numerator * denominator;
        return fromTerms(isAdd ? left + right : left - right, (long) denominator * fraction.denominator, true);
    }

    /**
     * Multiplies this fraction by another fraction and returns the result in reduced form.
     *
     * <p>The multiplication is performed using the standard formula: a/b × c/d = ac/bd,
     * with reduction before checking whether the final terms fit in an {@code int}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(2, 3);       // returns 2/3
     * Fraction f2 = Fraction.of(3, 4);       // returns 3/4
     * Fraction prod = f1.multipliedBy(f2);   // returns 1/2 (reduced from 6/12)
     *
     * Fraction f3 = Fraction.of(5, 6);
     * Fraction f4 = Fraction.of(7, 8);
     * Fraction prod2 = f3.multipliedBy(f4);    // returns 35/48
     *
     * Fraction f5 = Fraction.of(0, 1);
     * Fraction prod3 = f5.multipliedBy(Fraction.of(5, 3));   // returns ZERO
     *
     * Fraction.of(2, 3).multipliedBy(null);                  // throws IllegalArgumentException
     * }</pre>
     *
     * @param fraction the fraction to multiply by (must not be {@code null})
     * @return the product, in reduced form; the shared {@link #ZERO} constant when either operand is zero
     * @throws IllegalArgumentException if the fraction parameter is {@code null}.
     * @throws ArithmeticException if the reduced result cannot be represented with an int numerator and positive int denominator
     */
    public Fraction multipliedBy(final Fraction fraction) throws IllegalArgumentException, ArithmeticException {
        if (fraction == null) {
            throw new IllegalArgumentException("The fraction must not be null");
        }
        return fromTerms((long) numerator * fraction.numerator, (long) denominator * fraction.denominator, true);
    }

    /**
     * Divides this fraction by another fraction and returns the result.
     * Division is performed by multiplying by the reciprocal of the divisor:
     * a/b ÷ c/d = a/b × d/c = ad/bc
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(3, 4);    // returns 3/4
     * Fraction f2 = Fraction.of(1, 2);    // returns 1/2
     * Fraction quot = f1.dividedBy(f2);   // returns 3/2 (3/4 ÷ 1/2 = 3/4 × 2/1)
     *
     * Fraction f3 = Fraction.of(5, 6);
     * Fraction f4 = Fraction.of(2, 3);
     * Fraction quot2 = f3.dividedBy(f4);                // returns 5/4
     *
     * Fraction.of(3, 4).dividedBy(Fraction.of(0, 1));   // throws ArithmeticException
     * Fraction.of(3, 4).dividedBy(null);                // throws IllegalArgumentException
     * }</pre>
     *
     * @param fraction the fraction to divide by (must not be {@code null} or zero)
     * @return the quotient, in reduced form; the shared {@link #ZERO} constant when this fraction is zero
     * @throws IllegalArgumentException if the fraction parameter is {@code null}.
     * @throws ArithmeticException if the divisor fraction is zero or if the
     *         calculation results in integer overflow
     */
    public Fraction dividedBy(final Fraction fraction) throws IllegalArgumentException, ArithmeticException {
        if (fraction == null) {
            throw new IllegalArgumentException("The fraction must not be null");
        }
        if (fraction.numerator == 0) {
            throw new ArithmeticException("The fraction to divide by must not be zero");
        }
        return fromTerms((long) numerator * fraction.denominator, (long) denominator * fraction.numerator, true);
    }

    // Basics
    //-------------------------------------------------------------------

    /**
     * Compares this fraction to another fraction based on their numeric values.
     *
     * <p>Note: This class has a natural ordering that is inconsistent with equals.
     * The compareTo method treats fractions as equal if they have the same numeric value
     * (e.g., 1/2 and 2/4 are considered equal), while the equals method requires both
     * numerator and denominator to be identical.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(1, 2);
     * Fraction f2 = Fraction.of(2, 4);
     * Fraction f3 = Fraction.of(3, 4);
     *
     * f1.compareTo(f2);                                   // returns 0 (numerically equal)
     * f1.compareTo(f3);                                   // returns negative (f1 < f3)
     * f3.compareTo(f1);                                   // returns positive (f3 > f1)
     *
     * Fraction.of(-1, 2).compareTo(Fraction.of(-1, 3));   // returns negative
     * f1.compareTo(null);                                 // throws NullPointerException
     * }</pre>
     *
     * @param other the fraction to compare to, must not be {@code null}
     * @return a negative integer if this fraction is less than the other, zero if equal
     *         in value, or a positive integer if this fraction is greater than the other
     * @throws NullPointerException if {@code other} is {@code null}
     */
    @Override
    public int compareTo(final Fraction other) throws NullPointerException {
        if (this.equals(other)) {
            return 0;
        }

        // otherwise see which is less
        final long first = (long) numerator * (long) other.denominator;
        final long second = (long) other.numerator * (long) denominator;
        return Long.compare(first, second);
    }

    /**
     * Formats this fraction as a proper fraction string in the format "X Y/Z".
     *
     * <p>The format rules are:</p>
     * <ul>
     * <li>If the numerator is zero, returns "0"</li>
     * <li>If the fraction equals 1, returns "1"</li>
     * <li>If the fraction equals -1, returns "-1"</li>
     * <li>If the fraction is proper (the absolute value of the numerator is less than the denominator), returns "numerator/denominator"</li>
     * <li>If the fraction is improper, returns "whole numerator/denominator" format</li>
     * <li>If the fraction has no fractional part, returns just the whole number</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction.of(0, 1).toProperString();    // returns "0"
     * Fraction.of(3, 4).toProperString();    // returns "3/4"
     * Fraction.of(7, 4).toProperString();    // returns "1 3/4"
     * Fraction.of(8, 4).toProperString();    // returns "2"
     * Fraction.of(-7, 4).toProperString();   // returns "-1 3/4"
     * }</pre>
     *
     * @return a string representation in proper fraction format
     */
    public String toProperString() {
        if (toProperString == null) {
            if (numerator == 0) {
                toProperString = "0";
            } else if (numerator == denominator) {
                toProperString = "1";
            } else if (numerator < 0 && numerator == -denominator) {
                // denominator is always > 0 (every factory moves the sign onto the numerator), so
                // -denominator cannot overflow here.
                toProperString = "-1";
            } else if ((numerator > 0 ? -numerator : numerator) < -denominator) {
                // note that we do the magnitude comparison test above with
                // NEGATIVE (not positive) numbers, since negative numbers
                // have a larger range.  otherwise, numerator==Integer.MIN_VALUE
                // is handled incorrectly.
                final int properNumerator = properNumerator();
                if (properNumerator == 0) {
                    toProperString = Integer.toString(properWhole());
                } else {
                    toProperString = String.valueOf(properWhole()) + ' ' + properNumerator + '/' + denominator;
                }
            } else {
                toProperString = String.valueOf(numerator) + '/' + denominator;
            }
        }
        return toProperString;
    }

    /**
     * Returns whether this fraction represents zero.
     * Because the denominator is never zero, this holds for every unreduced form of zero as well.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction.of(0, 5).isZero();    // returns true
     * Fraction.ZERO.isZero();        // returns true
     * Fraction.of(-1, 2).isZero();   // returns false
     * }</pre>
     *
     * @return {@code true} if the numerator is zero; otherwise {@code false}
     * @see #isPositive()
     * @see #isNegative()
     */
    public boolean isZero() {
        return numerator == 0;
    }

    /**
     * Returns whether this fraction represents a positive value.
     * Zero is neither positive nor negative, so both {@code isPositive()} and {@link #isNegative()}
     * return {@code false} for it.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction.of(3, 4).isPositive();    // returns true
     * Fraction.of(-3, 4).isPositive();   // returns false
     * Fraction.ZERO.isPositive();        // returns false
     * }</pre>
     *
     * @return {@code true} if the numerator is positive; otherwise {@code false}
     * @see #isNegative()
     * @see #isZero()
     */
    public boolean isPositive() {
        return numerator > 0;
    }

    /**
     * Returns whether this fraction represents a negative value.
     * The sign is always carried by the numerator, so a negative denominator never occurs.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction.of(-3, 4).isNegative();   // returns true
     * Fraction.of(3, -4).isNegative();   // returns true (stored as -3/4)
     * Fraction.ZERO.isNegative();        // returns false
     * }</pre>
     *
     * @return {@code true} if the numerator is negative; otherwise {@code false}
     * @see #isPositive()
     * @see #isZero()
     */
    public boolean isNegative() {
        return numerator < 0;
    }

    /**
     * Returns whether this fraction represents an integer value, that is, whether it has no
     * fractional part. This does not require the fraction to be stored in reduced form.
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction.of(8, 4).isInteger();    // returns true (equals 2)
     * Fraction.of(0, 5).isInteger();    // returns true
     * Fraction.of(7, 4).isInteger();    // returns false
     * }</pre>
     *
     * @return {@code true} if the numerator is evenly divisible by the denominator; otherwise {@code false}
     * @see #properWhole()
     * @see #properNumerator()
     */
    public boolean isInteger() {
        return numerator % denominator == 0;
    }

    /**
     * Tests whether this fraction is equal to another object.
     * Two fractions are considered equal if and only if they have the same numerator
     * and the same denominator. This means that 1/2 and 2/4 are NOT considered equal
     * by this method, even though they represent the same numeric value.
     *
     * <p>To compare fractions by their numeric value, use {@link #compareTo(Fraction)}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(1, 2);
     * Fraction f2 = Fraction.of(1, 2);
     * Fraction f3 = Fraction.of(2, 4);
     *
     * f1.equals(f2);     // returns true (same numerator and denominator)
     * f1.equals(f3);     // returns false (different numerator and denominator)
     * f1.equals(null);   // returns false
     * f1.equals("x");    // returns false
     * }</pre>
     *
     * @param obj the object to compare with
     * @return {@code true} if the objects are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof Fraction other)) {
            return false;
        }
        return numerator == other.numerator && denominator == other.denominator;
    }

    /**
     * Returns a hash code for this fraction.
     * The hash code is calculated using both the numerator and denominator to ensure
     * that equal fractions (as defined by {@link #equals(Object)}) have equal hash codes.
     *
     * <p>The hash code is computed lazily and cached since this class is immutable. Concurrent callers may compute it more than once.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction f1 = Fraction.of(3, 4);
     * Fraction f2 = Fraction.of(3, 4);
     * boolean sameHash = f1.hashCode() == f2.hashCode();    // true (same state)
     *
     * Fraction f3 = Fraction.of(5, 8);
     * boolean differentHash = f1.hashCode() != f3.hashCode();    // true for these values
     *
     * Fraction f4 = Fraction.of(0, 1);
     * boolean nonZeroHash = f4.hashCode() != 0;    // true (the cached sentinel value is never returned)
     * }</pre>
     *
     * @return a hash code value for this fraction
     */
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            // hashcode update should be atomic.
            final int h = 37 * (37 * 17 + numerator) + denominator;
            hashCode = h == 0 ? 1 : h;
        }
        return hashCode;
    }

    /**
     * Returns a string representation of this fraction in the format "numerator/denominator".
     * This format is always used regardless of whether the fraction could be simplified
     * or expressed as a whole number.
     *
     * <p>For a more readable format that handles whole numbers and mixed fractions,
     * use {@link #toProperString()}.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Fraction.of(3, 4).toString();    // returns "3/4"
     * Fraction.of(8, 4).toString();    // returns "8/4" (not simplified)
     * Fraction.of(-1, 2).toString();   // returns "-1/2"
     * Fraction.of(0, 5).toString();    // returns "0/5"
     * }</pre>
     *
     * @return a string in the format "numerator/denominator"
     */
    @Override
    public String toString() {
        if (toString == null) {
            toString = String.valueOf(numerator) + '/' + denominator;
        }
        return toString;
    }

    /**
     * Restores a {@code Fraction} from a stream, rejecting any state no factory method could have
     * produced.
     *
     * <p>Every invariant of this class is enforced by the {@code of(...)} factories rather than by the
     * private constructor, and deserialization runs neither: a hand-crafted or corrupted stream can
     * otherwise yield a {@code Fraction} whose denominator is zero (making {@link #intValue()} throw
     * and {@link #doubleValue()} return {@code NaN}) or negative (putting the sign on the wrong term,
     * which silently breaks {@link #compareTo(Fraction)}, whose cross-multiplication assumes a
     * positive denominator).</p>
     *
     * @param in the stream to read this fraction from
     * @throws IOException if restoring the serialized fraction fields fails
     * @throws ClassNotFoundException if the class of a serialized object cannot be found
     * @throws InvalidObjectException if the stream holds a zero or negative denominator
     */
    @Serial
    private void readObject(final ObjectInputStream in) throws IOException, ClassNotFoundException, InvalidObjectException {
        in.defaultReadObject();

        if (denominator == 0) {
            throw new InvalidObjectException("The denominator must not be zero");
        }

        if (denominator < 0) {
            throw new InvalidObjectException("The denominator must be positive (" + denominator + "); the sign belongs on the numerator");
        }
    }
}
