package com.landawn.abacus.type;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.time.temporal.Temporal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class AbstractTemporalTypeTest extends TestBase {

    private Type<Temporal> temporalType;

    @BeforeEach
    public void setUp() {
        temporalType = createType("LocalDateTime");
    }

    @Test
    public void test_isCsvQuoteRequired() {
        assertFalse(temporalType.isCsvQuoteRequired());
    }

    @Test
    public void test_isTemporal() {
        assertTrue(temporalType.isTemporal());
    }
}
