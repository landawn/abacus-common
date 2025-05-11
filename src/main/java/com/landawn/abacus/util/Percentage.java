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
 * The Enum Percentage.
 *
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
    _20("20%", 0.20),
    _30("30%", 0.30),
    _40("40%", 0.40),
    _50("50%", 0.50),
    _60("60%", 0.60),
    _70("70%", 0.70),
    _80("80%", 0.80),
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

    //    private static final Map<Integer, Percentage> valuePool = new HashMap<>();
    //
    //    static {
    //        for (final Percentage p : Percentage.values()) {
    //            valuePool.put(intValue(p), p);
    //        }
    //    }

    private static final Map<String, ImmutableSet<Percentage>> rangePool = new ConcurrentHashMap<>();

    private final String str;

    private final double val;

    Percentage(final String str, final double val) {
        this.str = str;
        this.val = val;
    }

    /**
     *
     * @param startInclusive
     * @param endExclusive
     * @return
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
     *
     * @param startInclusive
     * @param endExclusive
     * @param by
     * @return
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
     *
     * @param startInclusive
     * @param endInclusive
     * @return
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
     *
     * @param startInclusive
     * @param endInclusive
     * @param by
     * @return
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
     *
     * @param p
     * @return
     */
    private static int intValue(final Percentage p) {
        return (int) (p.val * 1_000_000);
    }

    public double doubleValue() {
        return val;
    }

    @Override
    public String toString() {
        return str;
    }

}
