package com.landawn.abacus.type;

import static org.junit.Assert.assertFalse;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.CharacterWriter;

@Tag("new-test")
public class AbstractAtomicTypeTest extends TestBase {
    private Type<AtomicInteger> type;
    private CharacterWriter characterWriter;

    @BeforeEach
    public void setUp() {
        type = createType(AtomicInteger.class);
        characterWriter = createCharacterWriter();
    }

    @Test
    public void test_isCsvQuoteRequired() {
        assertFalse(type.isCsvQuoteRequired());
    }

}
