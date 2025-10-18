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

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enumeration representing common percentage values with their string representations and decimal equivalents.
 * This enum provides a convenient way to work with percentages in calculations and formatting.
 * 
 * <p>The enum includes values from very small percentages (0.0001%) to very high percentages (99.9999%),
 * covering most common use cases in statistics, probability, and general calculations.</p>
 * 
 * <p>Example usage:</p>
 * <pre>{@code
 * Percentage p = Percentage._95;
 * double value = p.doubleValue(); // Returns 0.95
 * String str = p.toString(); // Returns "95%"
 * 
 * // Get a range of percentages
 * ImmutableSet<Percentage> highPercentages = Percentage.range(Percentage._90, Percentage._99);
 * }</pre>
 */
public enum Percentage {

    _0_0001("0.0001%", 0.000001),
    _0_001("0.001%", 0.00001),
    _0_01("0.01%", 0.0001),
    _0_1("0.1%", 0.001),
    _1("1%", 0.01),
    _2("2%", 0.02),
    _3("3%", 0.03),
    _4("4%", 0.04),
    _5("5%", 0.05),
    _6("6%", 0.06),
    _7("7%", 0.07),
    _8("8%", 0.08),
    _9("9%", 0.09),
    _10("10%", 0.10),
    _15("15%", 0.15),
    _20("20%", 0.20),
    _25("25%", 0.25),
    _30("30%", 0.30),
    _35("35%", 0.35),
    _40("40%", 0.40),
    _45("45%", 0.45),
    _50("50%", 0.50),
    _55("55%", 0.55),
    _60("60%", 0.60),
    _65("65%", 0.65),
    _70("70%", 0.70),
    _75("75%", 0.75),
    _80("80%", 0.80),
    _85("85%", 0.85),
    _90("90%", 0.90),
    _91("91%", 0.91),
    _92("92%", 0.92),
    _93("93%", 0.93),
    _94("94%", 0.94),
    _95("95%", 0.95),
    _96("96%", 0.96),
    _97("97%", 0.97),
    _98("98%", 0.98),
    _99("99%", 0.99),
    _99_9("99.9%", 0.999),
    _99_99("99.99%", 0.9999),
    _99_999("99.999%", 0.99999),
    _99_9999("99.9999%", 0.999999);

    private static final Map<String, ImmutableSet<Percentage>> rangePool = new ConcurrentHashMap<>();

    private final String str;

    private final double val;

    Percentage(final String str, final double val) {
        this.str = str;
        this.val = val;
    }

    /**
     * Returns an immutable set of Percentage values within the specified range.
     * The range is inclusive of the start value and exclusive of the end value.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * ImmutableSet<Percentage> mediumRange = Percentage.range(Percentage._40, Percentage._60);
     * // Returns set containing: _40, _45, _50, _55
     * }</pre>
     *
     * @param startInclusive the starting percentage (inclusive)
     * @param endExclusive the ending percentage (exclusive)
     * @return an immutable set containing all Percentage values in the specified range
     */
    public static ImmutableSet<Percentage> range(final Percentage startInclusive, final Percentage endExclusive) {
        final String key = "(" + startInclusive.str + ", " + endExclusive.str + ")";
        ImmutableSet<Percentage> result = rangePool.get(key);

        if (result == null) {
            final Set<Percentage> set = N.newLinkedHashSet();

            for (final Percentage e : Percentage.values()) {
                if (N.compare(e.val, startInclusive.val) >= 0 && N.compare(e.val, endExclusive.val) < 0) {
                    set.add(e);
                }
            }

            result = ImmutableSet.wrap(set);
            rangePool.put(key, result);
        }

        return result;
    }

    /**
     * Returns an immutable set of Percentage values within the specified range with a step increment.
     * The range is inclusive of the start value and exclusive of the end value.
     * Only percentages that are at the specified step intervals from the start are included.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * ImmutableSet<Percentage> everyTenth = Percentage.range(Percentage._10, Percentage._50, Percentage._10);
     * // Returns set containing: _10, _20, _30, _40
     * }</pre>
     *
     * @param startInclusive the starting percentage (inclusive)
     * @param endExclusive the ending percentage (exclusive)
     * @param by the step increment between percentages
     * @return an immutable set containing Percentage values at the specified intervals
     */
    public static ImmutableSet<Percentage> range(final Percentage startInclusive, final Percentage endExclusive, final Percentage by) {
        final String key = "(" + startInclusive.str + ", " + endExclusive.str + ", " + by.str + ")";
        ImmutableSet<Percentage> result = rangePool.get(key);

        if (result == null) {
            final Set<Percentage> set = N.newLinkedHashSet();
            final int startVal = intValue(startInclusive);
            final int endVal = intValue(endExclusive);
            final int byVal = intValue(by);

            for (final Percentage p : Percentage.values()) {
                final int val = intValue(p);

                if (val >= startVal && val < endVal && (val - startVal) % byVal == 0) {
                    set.add(p);
                }
            }

            result = ImmutableSet.wrap(set);
            rangePool.put(key, result);
        }

        return result;
    }

    /**
     * Returns an immutable set of Percentage values within the specified closed range.
     * Both the start and end values are inclusive.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * ImmutableSet<Percentage> highRange = Percentage.rangeClosed(Percentage._90, Percentage._99);
     * // Returns set containing: _90, _91, _92, _93, _94, _95, _96, _97, _98, _99
     * }</pre>
     *
     * @param startInclusive the starting percentage (inclusive)
     * @param endInclusive the ending percentage (inclusive)
     * @return an immutable set containing all Percentage values in the specified closed range
     */
    public static ImmutableSet<Percentage> rangeClosed(final Percentage startInclusive, final Percentage endInclusive) {
        final String key = "(" + startInclusive.str + ", " + endInclusive.str + "]";
        ImmutableSet<Percentage> result = rangePool.get(key);

        if (result == null) {
            final Set<Percentage> set = N.newLinkedHashSet();

            for (final Percentage e : Percentage.values()) {
                if (N.compare(e.val, startInclusive.val) >= 0 && N.compare(e.val, endInclusive.val) <= 0) {
                    set.add(e);
                }
            }

            result = ImmutableSet.wrap(set);
            rangePool.put(key, result);
        }

        return result;
    }

    /**
     * Returns an immutable set of Percentage values within the specified closed range with a step increment.
     * Both the start and end values are inclusive.
     * Only percentages that are at the specified step intervals from the start are included.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * ImmutableSet<Percentage> everyFifth = Percentage.rangeClosed(Percentage._5, Percentage._20, Percentage._5);
     * // Returns set containing: _5, _10, _20
     * }</pre>
     *
     * @param startInclusive the starting percentage (inclusive)
     * @param endInclusive the ending percentage (inclusive)  
     * @param by the step increment between percentages
     * @return an immutable set containing Percentage values at the specified intervals
     */
    public static ImmutableSet<Percentage> rangeClosed(final Percentage startInclusive, final Percentage endInclusive, final Percentage by) {
        final String key = "(" + startInclusive.str + ", " + endInclusive.str + ", " + by.str + "]";
        ImmutableSet<Percentage> result = rangePool.get(key);

        if (result == null) {
            final Set<Percentage> set = N.newLinkedHashSet();
            final int startVal = intValue(startInclusive);
            final int endVal = intValue(endInclusive);
            final int byVal = intValue(by);

            for (final Percentage p : Percentage.values()) {
                final int val = intValue(p);

                if (val >= startVal && val <= endVal && (val - startVal) % byVal == 0) {
                    set.add(p);
                }
            }

            result = ImmutableSet.wrap(set);
            rangePool.put(key, result);
        }

        return result;
    }

    /**
     * Converts a Percentage enum value to its integer representation.
     * The integer value is calculated by multiplying the decimal value by 1,000,000.
     * This internal method is used for precise comparisons and calculations.
     *
     * @param p the percentage to convert
     * @return the integer representation of the percentage
     */
    private static int intValue(final Percentage p) {
        return (int) (p.val * 1_000_000);
    }

    /**
     * Returns the decimal representation of this percentage.
     * For example, 95% returns 0.95, 0.1% returns 0.001.
     * 
     * <p>Example usage:</p>
     * <pre>{@code
     * double value = Percentage._75.doubleValue(); // Returns 0.75
     * double calculation = 1000 * Percentage._10.doubleValue(); // Returns 100.0
     * }</pre>
     *
     * @return the decimal value of this percentage
     */
    public double doubleValue() {
        return val;
    }

    /**
     * Returns the string representation of this percentage.
     * The format includes the percentage sign, e.g., "95%" or "0.01%".
     *
     * @return the string representation of this percentage
     */
    @Override
    public String toString() {
        return str;
    }

}
