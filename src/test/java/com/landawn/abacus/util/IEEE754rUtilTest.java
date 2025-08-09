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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;

public class IEEE754rUtilTest extends AbstractTest {

    @Test
    public void testLang381() {
        assertEquals(1.2, IEEE754rUtil.min(1.2, 2.5, Double.NaN), 0.01);
        assertEquals(2.5, IEEE754rUtil.max(1.2, 2.5, Double.NaN), 0.01);
        assertTrue(Double.isNaN(IEEE754rUtil.max(Double.NaN, Double.NaN, Double.NaN)));
        assertEquals(1.2f, IEEE754rUtil.min(1.2f, 2.5f, Float.NaN), 0.01);
        assertEquals(2.5f, IEEE754rUtil.max(1.2f, 2.5f, Float.NaN), 0.01);
        assertTrue(Float.isNaN(IEEE754rUtil.max(Float.NaN, Float.NaN, Float.NaN)));

        final double[] a = { 1.2, Double.NaN, 3.7, 27.0, 42.0, Double.NaN };
        assertEquals(42.0, IEEE754rUtil.max(a), 0.01);
        assertEquals(1.2, IEEE754rUtil.min(a), 0.01);

        final double[] b = { Double.NaN, 1.2, Double.NaN, 3.7, 27.0, 42.0, Double.NaN };
        assertEquals(42.0, IEEE754rUtil.max(b), 0.01);
        assertEquals(1.2, IEEE754rUtil.min(b), 0.01);

        final float[] aF = { 1.2f, Float.NaN, 3.7f, 27.0f, 42.0f, Float.NaN };
        assertEquals(1.2f, IEEE754rUtil.min(aF), 0.01);
        assertEquals(42.0f, IEEE754rUtil.max(aF), 0.01);

        final float[] bF = { Float.NaN, 1.2f, Float.NaN, 3.7f, 27.0f, 42.0f, Float.NaN };
        assertEquals(1.2f, IEEE754rUtil.min(bF), 0.01);
        assertEquals(42.0f, IEEE754rUtil.max(bF), 0.01);
    }

    @Test
    public void testEnforceExceptions() {
        try {
            IEEE754rUtil.min((float[]) null);
            fail("IllegalArgumentException expected for null input");
        } catch (final IllegalArgumentException iae) { /* expected */
        }

        try {
            IEEE754rUtil.min();
            fail("IllegalArgumentException expected for empty input");
        } catch (final IllegalArgumentException iae) { /* expected */
        }

        try {
            IEEE754rUtil.max((float[]) null);
            fail("IllegalArgumentException expected for null input");
        } catch (final IllegalArgumentException iae) { /* expected */
        }

        try {
            IEEE754rUtil.max();
            fail("IllegalArgumentException expected for empty input");
        } catch (final IllegalArgumentException iae) { /* expected */
        }

        try {
            IEEE754rUtil.min((double[]) null);
            fail("IllegalArgumentException expected for null input");
        } catch (final IllegalArgumentException iae) { /* expected */
        }

        try {
            IEEE754rUtil.min();
            fail("IllegalArgumentException expected for empty input");
        } catch (final IllegalArgumentException iae) { /* expected */
        }

        try {
            IEEE754rUtil.max((double[]) null);
            fail("IllegalArgumentException expected for null input");
        } catch (final IllegalArgumentException iae) { /* expected */
        }

        try {
            IEEE754rUtil.max();
            fail("IllegalArgumentException expected for empty input");
        } catch (final IllegalArgumentException iae) { /* expected */
        }

    }

}
