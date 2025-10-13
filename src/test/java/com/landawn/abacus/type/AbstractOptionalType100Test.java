package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class AbstractOptionalType100Test extends TestBase {

    private Type<Optional<?>> optionalType;

    @BeforeEach
    public void setUp() {
        optionalType = createType("Optional");
    }

    @Test
    public void testIsOptionalOrNullable() {
        assertTrue(optionalType.isOptionalOrNullable());
    }
}
