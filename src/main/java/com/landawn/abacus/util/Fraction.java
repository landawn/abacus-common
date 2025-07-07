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

import java.io.Serial;
import java.math.BigInteger;

/**
 * <p>
 * Note: it's copied from Apache Commons Lang developed at <a href="http://www.apache.org/">The Apache Software Foundation</a>, or
 * under the Apache License 2.0. The methods copied from other products/frameworks may be modified in this class.
 * </p>
 *
 * <p>
 * {@code Fraction} is a {@code Number} implementation that stores fractions accurately.
 * </p>
 *
 * <p>
 * This class is immutable, and interoperable with most methods that accept a {@code Number}.
 * </p>
 *
 * <p>
 * Note that this class is intended for common use cases, it is <i>int</i> based and thus suffers from various overflow
 * issues. For a BigInteger based equivalent, please see the Commons Math BigFraction class.
 * </p>
 *
 * @version $Id: Fraction.java 1583482 2014-03-31 22:54:57Z niallp $
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
     * @param numerator
     *            the numerator, for example, the three in 'three sevenths'
     * @param denominator
     *            the denominator, for example, the seven in 'three sevenths'
     */
    private Fraction(final int numerator, final int denominator) {
        this.numerator = numerator;
        this.denominator = denominator;
    }

    /**
     * Creates a {@code Fraction} instance with the specified numerator and denominator.
     * The fraction is not reduced to its simplest form. For example, {@code Fraction.of(2, 4)}
     * creates a fraction representing 2/4, not 1/2.
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(3, 4);    // Creates 3/4
     * Fraction f2 = Fraction.of(-5, 8);   // Creates -5/8
     * </pre>
     *
     * @param numerator the numerator of the fraction
     * @param denominator the denominator of the fraction
     * @return a new fraction instance
     * @throws ArithmeticException if the denominator is zero
     * @see #of(int, int, boolean)
     */
    public static Fraction of(final int numerator, final int denominator) {
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
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(2, 4, false);  // Creates 2/4
     * Fraction f2 = Fraction.of(2, 4, true);   // Creates 1/2 (reduced)
     * Fraction f3 = Fraction.of(-6, -9, true); // Creates 2/3 (reduced and signs resolved)
     * </pre>
     *
     * @param numerator the numerator of the fraction
     * @param denominator the denominator of the fraction
     * @param reduce if {@code true}, reduces the fraction to its simplest form
     * @return a new fraction instance
     * @throws ArithmeticException if the denominator is zero, or if the denominator is 
     *         {@code Integer.MIN_VALUE} and the numerator is {@code Integer.MIN_VALUE}
     */
    public static Fraction of(int numerator, int denominator, final boolean reduce) {
        if (denominator == 0) {
            throw new ArithmeticException("The denominator must not be zero");
        }

        // allow 2^k/-2^31 as a valid fraction (where k>0)
        if (reduce && (denominator == Integer.MIN_VALUE && (numerator & 1) == 0)) {
            numerator /= 2;
            denominator /= 2;
        }

        if (denominator < 0) {
            if (numerator == Integer.MIN_VALUE || denominator == Integer.MIN_VALUE) {
                throw new ArithmeticException("overflow: can't negate");
            }
            numerator = -numerator;
            denominator = -denominator;
        }

        if (reduce) {
            if (numerator == 0) {
                return ZERO; // normalize zero.
            }
            // simplify fraction.
            final int gcd = greatestCommandDivisor(numerator, denominator);
            numerator /= gcd;
            denominator /= gcd;
        }
        return new Fraction(numerator, denominator);
    }

    /**
     * Creates a {@code Fraction} instance representing a mixed fraction (whole and fractional parts).
     * The fraction is not reduced. For example, {@code Fraction.of(1, 2, 4)} creates the
     * fraction equivalent to 1 + 2/4 = 6/4.
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(1, 3, 4);   // Creates 1 3/4 = 7/4
     * Fraction f2 = Fraction.of(-2, 1, 3);  // Creates -2 1/3 = -7/3
     * </pre>
     *
     * @param whole the whole number part
     * @param numerator the numerator of the fractional part
     * @param denominator the denominator of the fractional part
     * @return a new fraction instance
     * @throws ArithmeticException if the denominator is zero or if the resulting numerator
     *         exceeds {@code Integer.MAX_VALUE}
     * @see #of(int, int, int, boolean)
     */
    public static Fraction of(final int whole, final int numerator, final int denominator) {
        return of(whole, numerator, denominator, false);
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
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(1, 2, 4, false); // Creates 6/4
     * Fraction f2 = Fraction.of(1, 2, 4, true);  // Creates 3/2 (reduced)
     * Fraction f3 = Fraction.of(-1, 1, 2, true); // Creates -3/2
     * </pre>
     *
     * @param whole the whole number part (negative sign goes here for negative fractions)
     * @param numerator the numerator of the fractional part (must be non-negative)
     * @param denominator the denominator of the fractional part (must be positive)
     * @param reduce if {@code true}, reduces the resulting fraction to its simplest form
     * @return a new fraction instance
     * @throws ArithmeticException if the denominator is zero, the denominator is negative,
     *         the numerator is negative, or if the resulting numerator would overflow
     */
    public static Fraction of(final int whole, final int numerator, final int denominator, final boolean reduce) {
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
        if (numeratorValue < Integer.MIN_VALUE || numeratorValue > Integer.MAX_VALUE) {
            throw new ArithmeticException("Numerator too large to represent as an Integer.");
        }

        return of((int) numeratorValue, denominator, reduce);
    }

    /**
     * Creates a {@code Fraction} instance from a {@code double} value using the continued
     * fraction algorithm. This method finds a fraction that closely approximates the given
     * double value.
     * 
     * <p>The algorithm computes a maximum of 25 convergents and bounds the denominator by 10,000
     * to ensure reasonable fraction sizes. The resulting fraction is automatically reduced to
     * its simplest form.</p>
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(0.5);     // Creates 1/2
     * Fraction f2 = Fraction.of(0.333);   // Creates approximately 333/1000 or similar
     * Fraction f3 = Fraction.of(3.14159); // Creates an approximation of pi
     * </pre>
     *
     * @param value the double value to convert to a fraction
     * @return a new fraction instance that approximates the given value
     * @throws ArithmeticException if the value is greater than {@code Integer.MAX_VALUE},
     *         is {@code NaN}, or if the algorithm fails to converge within 25 iterations
     */
    public static Fraction of(double value) {
        final int sign = value < 0 ? -1 : 1;
        value = Math.abs(value);
        if (value > Integer.MAX_VALUE || Double.isNaN(value)) {
            throw new ArithmeticException("The value must not be greater than Integer.MAX_VALUE or NaN");
        }
        final int wholeNumber = (int) value;
        value -= wholeNumber;

        int numer0 = 0; // the pre-previous
        int denom0 = 1; // the pre-previous
        int numer1 = 1; // the previous
        int denom1 = 0; // the previous
        int numer2 = 0; // the current, setup in calculation
        int denom2 = 0; // the current, setup in calculation
        int a1 = (int) value;
        int a2 = 0;
        double x1 = 1;
        double x2 = 0;
        double y1 = value - a1;
        double y2 = 0;
        double delta1, delta2 = Double.MAX_VALUE;
        double fraction;
        int i = 1;
        //        System.out.println("---");
        do {
            delta1 = delta2;
            a2 = (int) (x1 / y1);
            x2 = y1;
            y2 = x1 - a2 * y1;
            numer2 = a1 * numer1 + numer0;
            denom2 = a1 * denom1 + denom0;
            fraction = (double) numer2 / (double) denom2;
            delta2 = Math.abs(value - fraction);
            //            System.out.println(numer2 + " " + denom2 + " " + fraction + " " + delta2 + " " + y1);
            a1 = a2;
            x1 = x2;
            y1 = y2;
            numer0 = numer1;
            denom0 = denom1;
            numer1 = numer2;
            denom1 = denom2;
            i++;
            //            System.out.println(">>" + delta1 +" "+ delta2+" "+(delta1 > delta2)+" "+i+" "+denom2);
        } while (delta1 > delta2 && denom2 <= 10000 && denom2 > 0 && i < 25);
        if (i == 25) {
            throw new ArithmeticException("Unable to convert double to fraction");
        }
        return of((numer0 + wholeNumber * denom0) * sign, denom0, true);
    }

    /**
     * Creates a {@code Fraction} from a string representation. Multiple formats are supported
     * to accommodate different fraction notations.
     * 
     * <p>Accepted formats:</p>
     * <ul>
     * <li>Decimal format: "0.5", "-2.75" (contains a decimal point)</li>
     * <li>Mixed fraction: "1 3/4", "-2 1/3" (whole number followed by space and fraction)</li>
     * <li>Simple fraction: "3/4", "-7/8" (numerator/denominator)</li>
     * <li>Whole number: "5", "-12" (treated as a fraction with denominator 1)</li>
     * </ul>
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of("3/4");     // Creates 3/4
     * Fraction f2 = Fraction.of("1 2/3");   // Creates 5/3
     * Fraction f3 = Fraction.of("0.25");    // Creates 1/4
     * Fraction f4 = Fraction.of("-5");      // Creates -5/1
     * </pre>
     *
     * @param str the string to parse
     * @return a new fraction instance
     * @throws IllegalArgumentException if the string is {@code null}
     * @throws NumberFormatException if the string format is invalid or cannot be parsed
     */
    public static Fraction of(String str) {
        if (str == null) {
            throw new IllegalArgumentException("The string must not be null");
        }
        // parse double format
        int pos = str.indexOf('.');
        if (pos >= 0) {
            return of(Double.parseDouble(str));
        }

        // parse X Y/Z format
        pos = str.indexOf(' ');
        if (pos > 0) {
            final int whole = Integer.parseInt(str.substring(0, pos));
            str = str.substring(pos + 1);
            pos = str.indexOf('/');
            if (pos < 0) {
                throw new NumberFormatException("The fraction could not be parsed as the format X Y/Z");
            } else {
                final int numer = Integer.parseInt(str.substring(0, pos));
                final int denom = Integer.parseInt(str.substring(pos + 1));
                return of(whole, numer, denom);
            }
        }

        // parse Y/Z format
        pos = str.indexOf('/');
        if (pos < 0) {
            // simple whole number
            return of(Integer.parseInt(str), 1);
        } else {
            final int numer = Integer.parseInt(str.substring(0, pos));
            final int denom = Integer.parseInt(str.substring(pos + 1));
            return of(numer, denom);
        }
    }

    // Accessors
    //-------------------------------------------------------------------

    /**
     * Gets the numerator part of the fraction.
     * This method may return a value greater than the denominator, representing an improper
     * fraction such as 7 in the fraction 7/4.
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f = Fraction.of(7, 4);
     * int n = f.getNumerator();  // Returns 7
     * </pre>
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
     * <p>Example usage:</p>
     * <pre>
     * Fraction f = Fraction.of(7, 4);
     * int n = f.numerator();  // Returns 7
     * </pre>
     *
     * @return the numerator of the fraction
     */
    public int numerator() {
        return numerator;
    }

    /**
     * Gets the denominator part of the fraction.
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f = Fraction.of(3, 8);
     * int d = f.getDenominator();  // Returns 8
     * </pre>
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
     * <p>Example usage:</p>
     * <pre>
     * Fraction f = Fraction.of(3, 8);
     * int d = f.denominator();  // Returns 8
     * </pre>
     *
     * @return the denominator of the fraction
     */
    public int denominator() {
        return denominator;
    }

    /**
     * Gets the proper numerator of the fraction, always positive.
     * An improper fraction like 7/4 can be expressed as the mixed number 1 3/4.
     * This method returns the numerator of the fractional part (3 in this example).
     * 
     * <p>For negative fractions like -7/4 (which equals -1 3/4), this method still
     * returns the positive proper numerator 3.</p>
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(7, 4);   // 7/4 = 1 3/4
     * int pn1 = f1.getProperNumerator(); // Returns 3
     * 
     * Fraction f2 = Fraction.of(-7, 4);  // -7/4 = -1 3/4
     * int pn2 = f2.getProperNumerator(); // Returns 3
     * </pre>
     *
     * @return the positive proper numerator
     * @deprecated replaced by {@link #properNumerator()}
     */
    @Deprecated
    public int getProperNumerator() {
        return Math.abs(numerator % denominator);
    }

    /**
     * Gets the proper numerator of the fraction, always positive.
     * An improper fraction like 7/4 can be expressed as the mixed number 1 3/4.
     * This method returns the numerator of the fractional part (3 in this example).
     * 
     * <p>For negative fractions like -7/4 (which equals -1 3/4), this method still
     * returns the positive proper numerator 3.</p>
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(7, 4);   // 7/4 = 1 3/4
     * int pn1 = f1.properNumerator();    // Returns 3
     * 
     * Fraction f2 = Fraction.of(-7, 4);  // -7/4 = -1 3/4
     * int pn2 = f2.properNumerator();    // Returns 3
     * </pre>
     *
     * @return the positive proper numerator
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
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(7, 4);   // 7/4 = 1 3/4
     * int w1 = f1.getProperWhole();      // Returns 1
     * 
     * Fraction f2 = Fraction.of(-7, 4);  // -7/4 = -1 3/4
     * int w2 = f2.getProperWhole();      // Returns -1
     * </pre>
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
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(7, 4);   // 7/4 = 1 3/4
     * int w1 = f1.properWhole();         // Returns 1
     * 
     * Fraction f2 = Fraction.of(-7, 4);  // -7/4 = -1 3/4
     * int w2 = f2.properWhole();         // Returns -1
     * </pre>
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
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(7, 4);   // 7/4 = 1.75
     * int i1 = f1.intValue();            // Returns 1
     * 
     * Fraction f2 = Fraction.of(-10, 3); // -10/3 = -3.333...
     * int i2 = f2.intValue();            // Returns -3
     * </pre>
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
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(7, 4);   // 7/4 = 1.75
     * long l1 = f1.longValue();          // Returns 1L
     * 
     * Fraction f2 = Fraction.of(-10, 3); // -10/3 = -3.333...
     * long l2 = f2.longValue();          // Returns -3L
     * </pre>
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
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(1, 3);   // 1/3
     * float v1 = f1.floatValue();        // Returns 0.33333334f
     * 
     * Fraction f2 = Fraction.of(3, 4);   // 3/4
     * float v2 = f2.floatValue();        // Returns 0.75f
     * </pre>
     *
     * @return the fraction as a float value
     */
    @Override
    public float floatValue() {
        return (float) numerator / (float) denominator; // NOSONAR
    }

    /**
     * Gets the fraction as a {@code double} value by performing floating-point division.
     * This calculates the decimal representation of the fraction with higher precision
     * than {@link #floatValue()}.
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(1, 3);   // 1/3
     * double v1 = f1.doubleValue();      // Returns 0.3333333333333333
     * 
     * Fraction f2 = Fraction.of(22, 7);  // 22/7 (approximation of pi)
     * double v2 = f2.doubleValue();      // Returns 3.142857142857143
     * </pre>
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
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(6, 8);
     * Fraction r1 = f1.reduce();         // Returns 3/4
     * 
     * Fraction f2 = Fraction.of(7, 13);
     * Fraction r2 = f2.reduce();         // Returns 7/13 (already in simplest form)
     * </pre>
     *
     * @return a new reduced fraction instance, or this instance if already in simplest form
     */
    public Fraction reduce() {
        if (numerator == 0) {
            return equals(ZERO) ? this : ZERO;
        }
        final int gcd = greatestCommandDivisor(Math.abs(numerator), denominator);
        if (gcd == 1) {
            return this;
        }
        return Fraction.of(numerator / gcd, denominator / gcd);
    }

    /**
     * Returns the multiplicative inverse (reciprocal) of this fraction.
     * For a fraction a/b, the inverse is b/a. The returned fraction is not reduced.
     * 
     * <p>Special handling for negative fractions: the negative sign is moved to the
     * numerator in the result.</p>
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(3, 4);
     * Fraction i1 = f1.invert();         // Returns 4/3
     * 
     * Fraction f2 = Fraction.of(-2, 5);
     * Fraction i2 = f2.invert();         // Returns -5/2
     * </pre>
     *
     * @return a new fraction that is the inverse of this fraction
     * @throws ArithmeticException if this fraction equals zero (cannot invert zero)
     *         or if the numerator is {@code Integer.MIN_VALUE}
     */
    public Fraction invert() {
        if (numerator == 0) {
            throw new ArithmeticException("Unable to invert zero.");
        }
        if (numerator == Integer.MIN_VALUE) {
            throw new ArithmeticException("overflow: can't negate numerator");
        }
        if (numerator < 0) {
            return new Fraction(-denominator, -numerator);
        } else {
            return new Fraction(denominator, numerator);
        }
    }

    /**
     * Returns the additive inverse (negative) of this fraction.
     * For a fraction a/b, the negative is -a/b. The returned fraction is not reduced.
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(3, 4);
     * Fraction n1 = f1.negate();         // Returns -3/4
     * 
     * Fraction f2 = Fraction.of(-2, 5);
     * Fraction n2 = f2.negate();         // Returns 2/5
     * </pre>
     *
     * @return a new fraction with the opposite sign
     * @throws ArithmeticException if the numerator is {@code Integer.MIN_VALUE}
     *         (cannot be negated due to integer overflow)
     */
    public Fraction negate() {
        // the positive range is one smaller than the negative range of an int.
        if (numerator == Integer.MIN_VALUE) {
            throw new ArithmeticException("overflow: too large to negate");
        }
        return new Fraction(-numerator, denominator);
    }

    /**
     * Returns the absolute value of this fraction.
     * If the fraction is already positive or zero, returns this instance.
     * If negative, returns a new fraction with the positive value.
     * The returned fraction is not reduced.
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(-3, 4);
     * Fraction a1 = f1.abs();            // Returns 3/4
     * 
     * Fraction f2 = Fraction.of(2, 5);
     * Fraction a2 = f2.abs();            // Returns 2/5 (same instance)
     * </pre>
     *
     * @return this instance if positive or zero, otherwise a new positive fraction
     */
    public Fraction abs() {
        if (numerator >= 0) {
            return this;
        }
        return negate();
    }

    /**
     * Raises this fraction to the specified integer power.
     * The result is automatically reduced to its simplest form.
     * 
     * <p>Special cases:</p>
     * <ul>
     * <li>Any fraction to the power of 0 equals 1 (even 0/1)</li>
     * <li>Any fraction to the power of 1 equals itself</li>
     * <li>Negative powers: (a/b)^(-n) = (b/a)^n</li>
     * </ul>
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f = Fraction.of(2, 3);
     * Fraction p1 = f.pow(2);            // Returns 4/9
     * Fraction p2 = f.pow(-1);           // Returns 3/2
     * Fraction p3 = f.pow(0);            // Returns 1/1
     * </pre>
     *
     * @param power the power to raise the fraction to
     * @return the fraction raised to the given power
     * @throws ArithmeticException if the power is negative and the fraction is zero,
     *         or if the calculation results in integer overflow
     */
    public Fraction pow(final int power) {
        if (power == 1) {
            return this;
        } else if (power == 0) {
            return ONE;
        } else if (power < 0) {
            if (power == Integer.MIN_VALUE) { // MIN_VALUE can't be negated.
                return invert().pow(2).pow(-(power / 2));
            }
            return invert().pow(-power);
        } else {
            final Fraction f = multipliedBy(this);
            if (power % 2 == 0) { // if even...
                return f.pow(power / 2);
            } else { // if odd...
                return f.pow(power / 2).multipliedBy(this);
            }
        }
    }

    /**
     * <p>
     * Gets the greatest common divisor of the absolute value of two numbers, using the "binary gcd" method which avoids
     * division and modulo operations. See Knuth 4.5.2 algorithm B. This algorithm is due to Josef Stein (1961).
     * </p>
     *
     * @param u
     *            a non-zero number
     * @param v
     *            a non-zero number
     * @return
     */
    private static int greatestCommandDivisor(int u, int v) {
        // From Commons Math:
        if (u == 0 || v == 0) {
            if (u == Integer.MIN_VALUE || v == Integer.MIN_VALUE) {
                throw new ArithmeticException("overflow: gcd is 2^31");
            }
            return Math.abs(u) + Math.abs(v);
        }
        //if either operand is abs 1, return 1:
        if (Math.abs(u) == 1 || Math.abs(v) == 1) {
            return 1;
        }
        // keep u and v negative, as negative integers range down to
        // -2^31, while positive numbers can only be as large as 2^31-1
        // (i.e., we can't necessarily negate a negative number without
        // overflow)
        if (u > 0) {
            u = -u;
        } // make u negative
        if (v > 0) {
            v = -v;
        } // make v negative
          // B1. [Find power of 2]
        int k = 0;
        while ((u & 1) == 0 && (v & 1) == 0 && k < 31) { // while u and v are both even...
            u /= 2;
            v /= 2;
            k++; // cast out twos.
        }
        if (k == 31) {
            throw new ArithmeticException("overflow: gcd is 2^31");
        }
        // B2. Initialize: u and v have been divided by 2^k and at least
        //     one is odd.
        int t = (u & 1) == 1 ? v : -(u / 2)/* B3 */;
        // t negative: u was odd, v may be even (t replaces v)
        // t positive: u was even, v is odd (t replaces u)
        do {
            /* assert u<0 && v<0; */
            // B4/B3: cast out twos from t.
            while ((t & 1) == 0) { // while t is even.
                t /= 2; // cast out twos
            }
            // B5 [reset max(u,v)]
            if (t > 0) {
                u = -t;
            } else {
                v = t;
            }
            // B6/B3. at this point both u and v should be odd.
            t = (v - u) / 2;
            // |u| larger: t positive (replace u)
            // |v| larger: t negative (replace v)
        } while (t != 0);
        return -u * (1 << k); // gcd is u*2^k
    }

    // Arithmetic
    //-------------------------------------------------------------------

    /**
     * Multiply two integers, checking for overflow.
     *
     * @param x
     *            a factor
     * @param y
     *            a factor
     * @return
     * @throws ArithmeticException
     *             if the result cannot be represented as an int
     */
    private static int mulAndCheck(final int x, final int y) {
        final long m = (long) x * (long) y;
        if (m < Integer.MIN_VALUE || m > Integer.MAX_VALUE) {
            throw new ArithmeticException("overflow: mul");
        }
        return (int) m;
    }

    /**
     * Multiply two non-negative integers, checking for overflow.
     *
     * @param x
     *            a non-negative factor
     * @param y
     *            a non-negative factor
     * @return
     * @throws ArithmeticException
     *             if the result cannot be represented as an int
     */
    private static int mulPosAndCheck(final int x, final int y) {
        /* assert x>=0 && y>=0; */
        final long m = (long) x * (long) y;
        if (m > Integer.MAX_VALUE) {
            throw new ArithmeticException("overflow: mulPos");
        }
        return (int) m;
    }

    /**
     * Add two integers, checking for overflow.
     *
     * @param x
     *            an addend
     * @param y
     *            an addend
     * @return
     * @throws ArithmeticException
     *             if the result cannot be represented as an int
     */
    private static int addAndCheck(final int x, final int y) {
        final long s = (long) x + (long) y;
        if (s < Integer.MIN_VALUE || s > Integer.MAX_VALUE) {
            throw new ArithmeticException("overflow: add");
        }
        return (int) s;
    }

    /**
     * Subtract two integers, checking for overflow.
     *
     * @param x
     *            the minuend
     * @param y
     *            the subtrahend
     * @return
     * @throws ArithmeticException
     *             if the result cannot be represented as an int
     */
    private static int subAndCheck(final int x, final int y) {
        final long s = (long) x - (long) y;
        if (s < Integer.MIN_VALUE || s > Integer.MAX_VALUE) {
            throw new ArithmeticException("overflow: add");
        }
        return (int) s;
    }

    /**
     * Adds this fraction to another fraction and returns the result in reduced form.
     * The algorithm follows Knuth 4.5.1 for efficient computation.
     * 
     * <p>The addition is performed using the standard formula: a/b + c/d = (ad + bc) / bd,
     * but optimized to prevent unnecessary overflow and to maintain the result in
     * reduced form.</p>
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(1, 2);   // 1/2
     * Fraction f2 = Fraction.of(1, 3);   // 1/3
     * Fraction sum = f1.add(f2);         // Returns 5/6
     * </pre>
     *
     * @param fraction the fraction to add to this fraction (must not be null)
     * @return a new {@code Fraction} instance with the sum in reduced form
     * @throws IllegalArgumentException if the fraction parameter is null
     * @throws ArithmeticException if the calculation results in integer overflow
     */
    public Fraction add(final Fraction fraction) {
        return addSub(fraction, true /* add */);
    }

    /**
     * Subtracts another fraction from this fraction and returns the result in reduced form.
     * 
     * <p>The subtraction is performed using the standard formula: a/b - c/d = (ad - bc) / bd,
     * but optimized to prevent unnecessary overflow and to maintain the result in
     * reduced form.</p>
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(3, 4);   // 3/4
     * Fraction f2 = Fraction.of(1, 2);   // 1/2
     * Fraction diff = f1.subtract(f2);  // Returns 1/4
     * </pre>
     *
     * @param fraction the fraction to subtract from this fraction (must not be null)
     * @return a new {@code Fraction} instance with the difference in reduced form
     * @throws IllegalArgumentException if the fraction parameter is null
     * @throws ArithmeticException if the calculation results in integer overflow
     */
    public Fraction subtract(final Fraction fraction) {
        return addSub(fraction, false /* subtract */);
    }

    /**
     * Implement add and subtract using algorithm described in Knuth 4.5.1.
     *
     * @param fraction
     *            the fraction to subtract, must not be {@code null}
     * @param isAdd
     *            {@code true} to add, {@code false} to subtract
     * @return a {@code Fraction} instance with the resulting values
     * @throws IllegalArgumentException
     *             if the fraction is {@code null}
     * @throws ArithmeticException
     *             if the resulting numerator or denominator cannot be represented in an {@code int}.
     */
    private Fraction addSub(final Fraction fraction, final boolean isAdd) {
        if (fraction == null) {
            throw new IllegalArgumentException("The fraction must not be null"); //NOSONAR
        }
        // zero is identity for addition.
        if (numerator == 0) {
            return isAdd ? fraction : fraction.negate();
        }
        if (fraction.numerator == 0) {
            return this;
        }
        // if denominators are randomly distributed, d1 will be 1 about 61%
        // of the time.
        final int d1 = greatestCommandDivisor(denominator, fraction.denominator);
        if (d1 == 1) {
            // result is ( (u*v' +/- u'v) / u'v')
            final int uvp = mulAndCheck(numerator, fraction.denominator);
            final int upv = mulAndCheck(fraction.numerator, denominator);
            return new Fraction(isAdd ? addAndCheck(uvp, upv) : subAndCheck(uvp, upv), mulPosAndCheck(denominator, fraction.denominator));
        }
        // the quantity 't' requires 65 bits of precision; see knuth 4.5.1
        // exercise 7.  we're going to use a BigInteger.
        // t = u(v'/d1) +/- v(u'/d1)
        final BigInteger uvp = BigInteger.valueOf(numerator).multiply(BigInteger.valueOf(fraction.denominator / d1));
        final BigInteger upv = BigInteger.valueOf(fraction.numerator).multiply(BigInteger.valueOf(denominator / d1));
        final BigInteger t = isAdd ? uvp.add(upv) : uvp.subtract(upv);
        // but d2 doesn't need extra precision because
        // d2 = gcd(t,d1) = gcd(t mod d1, d1)
        final int tmodd1 = t.mod(BigInteger.valueOf(d1)).intValue();
        final int d2 = tmodd1 == 0 ? d1 : greatestCommandDivisor(tmodd1, d1);

        // result is (t/d2) / (u'/d1)(v'/d2)
        final BigInteger w = t.divide(BigInteger.valueOf(d2));
        if (w.bitLength() > 31) {
            throw new ArithmeticException("overflow: numerator too large after multiply");
        }
        return new Fraction(w.intValue(), mulPosAndCheck(denominator / d1, fraction.denominator / d2));
    }

    /**
     * Multiplies this fraction by another fraction and returns the result in reduced form.
     * 
     * <p>The multiplication is performed using the standard formula: a/b × c/d = ac/bd,
     * but optimized using Knuth's algorithm 4.5.1 to prevent overflow by reducing
     * common factors before multiplication.</p>
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(2, 3);   // 2/3
     * Fraction f2 = Fraction.of(3, 4);   // 3/4
     * Fraction prod = f1.multipliedBy(f2); // Returns 1/2 (reduced from 6/12)
     * </pre>
     *
     * @param fraction the fraction to multiply by (must not be null)
     * @return a new {@code Fraction} instance with the product in reduced form
     * @throws IllegalArgumentException if the fraction parameter is null
     * @throws ArithmeticException if the calculation results in integer overflow
     */
    public Fraction multipliedBy(final Fraction fraction) {
        if (fraction == null) {
            throw new IllegalArgumentException("The fraction must not be null");
        }
        if (numerator == 0 || fraction.numerator == 0) {
            return ZERO;
        }
        // knuth 4.5.1
        // make sure we don't overflow unless the result *must* overflow.
        final int d1 = greatestCommandDivisor(numerator, fraction.denominator);
        final int d2 = greatestCommandDivisor(fraction.numerator, denominator);
        return of(mulAndCheck(numerator / d1, fraction.numerator / d2), mulPosAndCheck(denominator / d2, fraction.denominator / d1), true);
    }

    /**
     * Divides this fraction by another fraction and returns the result.
     * Division is performed by multiplying by the reciprocal of the divisor:
     * a/b ÷ c/d = a/b × d/c = ad/bc
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(3, 4);   // 3/4
     * Fraction f2 = Fraction.of(1, 2);   // 1/2
     * Fraction quot = f1.dividedBy(f2);  // Returns 3/2 (3/4 ÷ 1/2 = 3/4 × 2/1)
     * </pre>
     *
     * @param fraction the fraction to divide by (must not be null or zero)
     * @return a new {@code Fraction} instance with the quotient
     * @throws IllegalArgumentException if the fraction parameter is null
     * @throws ArithmeticException if the divisor fraction is zero or if the
     *         calculation results in integer overflow
     */
    public Fraction dividedBy(final Fraction fraction) {
        if (fraction == null) {
            throw new IllegalArgumentException("The fraction must not be null");
        }
        if (fraction.numerator == 0) {
            throw new ArithmeticException("The fraction to divide by must not be zero");
        }
        return multipliedBy(fraction.invert());
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
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(1, 2);
     * Fraction f2 = Fraction.of(2, 4);
     * Fraction f3 = Fraction.of(3, 4);
     * 
     * f1.compareTo(f2);  // Returns 0 (numerically equal)
     * f1.compareTo(f3);  // Returns -1 (f1 < f3)
     * f3.compareTo(f1);  // Returns 1 (f3 > f1)
     * </pre>
     *
     * @param other the fraction to compare to
     * @return -1 if this fraction is less than the other, 0 if equal in value,
     *         +1 if this fraction is greater than the other
     * @throws ClassCastException if the object is not a {@code Fraction}
     */
    @Override
    public int compareTo(final Fraction other) {
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
     * <li>If the fraction is proper (numerator < denominator), returns "numerator/denominator"</li>
     * <li>If the fraction is improper, returns "whole numerator/denominator" format</li>
     * <li>If the fraction has no fractional part, returns just the whole number</li>
     * </ul>
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction.of(0, 1).toProperString();    // Returns "0"
     * Fraction.of(3, 4).toProperString();    // Returns "3/4"
     * Fraction.of(7, 4).toProperString();    // Returns "1 3/4"
     * Fraction.of(8, 4).toProperString();    // Returns "2"
     * Fraction.of(-7, 4).toProperString();   // Returns "-1 3/4"
     * </pre>
     *
     * @return a string representation in proper fraction format
     */
    public String toProperString() {
        if (toProperString == null) {
            if (numerator == 0) {
                toProperString = "0";
            } else if (numerator == denominator) {
                toProperString = "1";
            } else if (numerator == -1 * denominator) {
                toProperString = "-1";
            } else if ((numerator > 0 ? -numerator : numerator) < -denominator) {
                // note that we do the magnitude comparison test above with
                // NEGATIVE (not positive) numbers, since negative numbers
                // have a larger range.  otherwise, numerator==Integer.MIN_VALUE
                // is handled incorrectly.
                final int properNumerator = getProperNumerator();
                if (properNumerator == 0) {
                    toProperString = Integer.toString(getProperWhole());
                } else {
                    toProperString = String.valueOf(getProperWhole()) + ' ' + properNumerator + '/' + getDenominator();
                }
            } else {
                toProperString = String.valueOf(getNumerator()) + '/' + getDenominator();
            }
        }
        return toProperString;
    }

    /**
     * Tests whether this fraction is equal to another object.
     * Two fractions are considered equal if and only if they have the same numerator
     * and the same denominator. This means that 1/2 and 2/4 are NOT considered equal
     * by this method, even though they represent the same numeric value.
     * 
     * <p>To compare fractions by their numeric value, use {@link #compareTo(Fraction)}.</p>
     * 
     * <p>Example usage:</p>
     * <pre>
     * Fraction f1 = Fraction.of(1, 2);
     * Fraction f2 = Fraction.of(1, 2);
     * Fraction f3 = Fraction.of(2, 4);
     * 
     * f1.equals(f2);  // Returns true (same numerator and denominator)
     * f1.equals(f3);  // Returns false (different numerator and denominator)
     * </pre>
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
        return getNumerator() == other.getNumerator() && getDenominator() == other.getDenominator();
    }

    /**
     * Returns a hash code for this fraction.
     * The hash code is calculated using both the numerator and denominator to ensure
     * that equal fractions (as defined by {@link #equals(Object)}) have equal hash codes.
     * 
     * <p>The hash code is computed once and cached since this class is immutable.</p>
     *
     * @return a hash code value for this fraction
     */
    @Override
    public int hashCode() {
        if (hashCode == 0) {
            // hashcode update should be atomic.
            hashCode = 37 * (37 * 17 + getNumerator()) + getDenominator();
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
     * <p>Example usage:</p>
     * <pre>
     * Fraction.of(3, 4).toString();    // Returns "3/4"
     * Fraction.of(8, 4).toString();    // Returns "8/4" (not simplified)
     * Fraction.of(-1, 2).toString();   // Returns "-1/2"
     * </pre>
     *
     * @return a string in the format "numerator/denominator"
     */
    @Override
    public String toString() {
        if (toString == null) {
            toString = String.valueOf(getNumerator()) + '/' + getDenominator();
        }
        return toString;
    }
}