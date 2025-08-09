/*
 * Copyright (c) 2015, Haiyang Li. All rights reserved.
 */

package com.landawn.abacus.util;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.logging.Logger;
import com.landawn.abacus.logging.LoggerFactory;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.N;

public class LogMessageTest extends AbstractTest {
    private static final Logger logger = LoggerFactory.getLogger(LogMessageTest.class);

    @Test
    public void test_1() {

        logger.error("Hi {}.", Array.of(false));
        logger.error("Hi {}.", Array.of('1', 'a'));
        logger.error("Hi {}.", Array.of((byte) 1, (byte) 2));
        logger.error("Hi {}.", Array.of((short) 1, (short) 2));
        logger.error("Hi {}.", Array.of(1, 2));
        logger.error("Hi {}.", Array.of(1L, 2L));
        logger.error("Hi {}.", Array.of(1f, 2f));
        logger.error("Hi {}.", Array.of(1d, 2d));
        logger.error("Hi {}.", "123", "abc");
        Object obj = N.asArray("123", "abc");
        logger.error("Hi {}.", obj);
    }
}
