/*
 * Copyright (C) 2025 HaiYang Li
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

package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class IntSupplierTest extends TestBase {

    @Test
    public void test_getAsInt_lambda() {
        IntSupplier supplier = () -> 42;

        assertEquals(42, supplier.getAsInt());
        assertEquals(42, supplier.getAsInt());
    }

    @Test
    public void test_getAsInt_anonymousClass() {
        IntSupplier supplier = new IntSupplier() {
            @Override
            public int getAsInt() {
                return 100;
            }
        };

        assertEquals(100, supplier.getAsInt());
        assertEquals(100, supplier.getAsInt());
    }

    @Test
    public void test_getAsInt_statefulSupplier() {
        final int[] counter = { 0 };

        IntSupplier incrementing = () -> counter[0]++;

        assertEquals(0, incrementing.getAsInt());
        assertEquals(1, incrementing.getAsInt());
        assertEquals(2, incrementing.getAsInt());
    }

    @Test
    public void test_getAsInt_varyingValues() {
        final int[] values = { 10, 20, 30 };
        final int[] index = { 0 };

        IntSupplier supplier = () -> values[index[0]++ % values.length];

        assertEquals(10, supplier.getAsInt());
        assertEquals(20, supplier.getAsInt());
        assertEquals(30, supplier.getAsInt());
        assertEquals(10, supplier.getAsInt()); // Wraps around
    }

    @Test
    public void test_ZERO() {
        assertEquals(0, IntSupplier.ZERO.getAsInt());
        assertEquals(0, IntSupplier.ZERO.getAsInt());
        assertEquals(0, IntSupplier.ZERO.getAsInt());
    }

    @Test
    public void test_getAsInt_negativeValue() {
        IntSupplier supplier = () -> -500;

        assertEquals(-500, supplier.getAsInt());
    }

    @Test
    public void test_getAsInt_maxValue() {
        IntSupplier supplier = () -> Integer.MAX_VALUE;

        assertEquals(Integer.MAX_VALUE, supplier.getAsInt());
    }

    @Test
    public void test_getAsInt_minValue() {
        IntSupplier supplier = () -> Integer.MIN_VALUE;

        assertEquals(Integer.MIN_VALUE, supplier.getAsInt());
    }

    @Test
    public void test_RANDOM() {
        assertDoesNotThrow(() -> {
            IntSupplier.RANDOM.getAsInt();
            IntSupplier.RANDOM.getAsInt();
            IntSupplier.RANDOM.getAsInt();
        });
    }
    // ---- perf review 2026-09-26 G115 begin ----
    // G115-01: the *Supplier.RANDOM constants call ThreadLocalRandom.current() per draw - safe and varying from several threads.
    @org.junit.jupiter.api.Test
    public void testRandom_allSuppliersConcurrentCallers() throws Exception {
        final int threadCount = 4;
        final int draws = 2000;
        final java.util.List<Throwable> errors = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final Thread[] threads = new Thread[threadCount];

        for (int t = 0; t < threadCount; t++) {
            threads[t] = new Thread(() -> {
                try {
                    int trueCount = 0;
                    final java.util.Set<Byte> bytes = new java.util.HashSet<>();
                    final java.util.Set<Character> chars = new java.util.HashSet<>();
                    final java.util.Set<Short> shorts = new java.util.HashSet<>();
                    final java.util.Set<Integer> ints = new java.util.HashSet<>();
                    final java.util.Set<Long> longs = new java.util.HashSet<>();
                    final java.util.Set<Float> floats = new java.util.HashSet<>();
                    final java.util.Set<Double> doubles = new java.util.HashSet<>();

                    for (int i = 0; i < draws; i++) {
                        if (BooleanSupplier.RANDOM.getAsBoolean()) {
                            trueCount++;
                        }

                        bytes.add(ByteSupplier.RANDOM.getAsByte());

                        final char ch = CharSupplier.RANDOM.getAsChar();
                        org.junit.jupiter.api.Assertions.assertTrue(Character.isDefined(ch));
                        chars.add(ch);

                        shorts.add(ShortSupplier.RANDOM.getAsShort());
                        ints.add(IntSupplier.RANDOM.getAsInt());
                        longs.add(LongSupplier.RANDOM.getAsLong());

                        final float f = FloatSupplier.RANDOM.getAsFloat();
                        org.junit.jupiter.api.Assertions.assertTrue(f >= 0f && f < 1f);
                        floats.add(f);

                        final double d = DoubleSupplier.RANDOM.getAsDouble();
                        org.junit.jupiter.api.Assertions.assertTrue(d >= 0d && d < 1d);
                        doubles.add(d);
                    }

                    org.junit.jupiter.api.Assertions.assertTrue(trueCount > 0 && trueCount < draws);
                    org.junit.jupiter.api.Assertions.assertTrue(bytes.size() > 100);
                    org.junit.jupiter.api.Assertions.assertTrue(chars.size() > draws / 2);
                    org.junit.jupiter.api.Assertions.assertTrue(shorts.size() > draws / 2);
                    org.junit.jupiter.api.Assertions.assertTrue(ints.size() > draws / 2);
                    org.junit.jupiter.api.Assertions.assertTrue(longs.size() > draws / 2);
                    org.junit.jupiter.api.Assertions.assertTrue(floats.size() > draws / 2);
                    org.junit.jupiter.api.Assertions.assertTrue(doubles.size() > draws / 2);
                } catch (final Throwable e) {
                    errors.add(e);
                }
            });
            threads[t].start();
        }

        for (final Thread thread : threads) {
            thread.join();
        }

        org.junit.jupiter.api.Assertions.assertTrue(errors.isEmpty(), errors::toString);
        // The constants are still singletons.
        org.junit.jupiter.api.Assertions.assertSame(IntSupplier.RANDOM, IntSupplier.RANDOM);
    }
    // ---- perf review 2026-09-26 G115 end ----
}
