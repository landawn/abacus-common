package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.temporal.Temporal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class AbstractTemporalType100Test extends TestBase {

    private Type<Temporal> temporalType;

    @BeforeEach
    public void setUp() {
        temporalType = createType("LocalDateTime");
    }

    @Test
    public void testIsNonQuotableCsvType() {
        assertTrue(temporalType.isNonQuotableCsvType());
    }
}
