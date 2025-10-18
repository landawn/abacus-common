package com.landawn.abacus.parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ASMUtil100Test extends TestBase {

    @Test
    public void testIsASMAvailable() {
        boolean result = ASMUtil.isASMAvailable();
        Assertions.assertEquals(result, ASMUtil.isASMAvailable());
        Assertions.assertEquals(result, ASMUtil.isASMAvailable());
    }

    @Test
    public void testIsASMAvailableConsistency() {
        boolean firstCall = ASMUtil.isASMAvailable();
        boolean secondCall = ASMUtil.isASMAvailable();
        boolean thirdCall = ASMUtil.isASMAvailable();

        Assertions.assertEquals(firstCall, secondCall);
        Assertions.assertEquals(secondCall, thirdCall);
    }
}
