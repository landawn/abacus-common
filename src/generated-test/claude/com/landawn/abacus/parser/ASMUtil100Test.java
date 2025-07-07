package com.landawn.abacus.parser;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;


public class ASMUtil100Test extends TestBase {

    @Test
    public void testIsASMAvailable() {
        // Test that the method returns a boolean value
        boolean result = ASMUtil.isASMAvailable();
        // The result should be consistent across multiple calls
        Assertions.assertEquals(result, ASMUtil.isASMAvailable());
        Assertions.assertEquals(result, ASMUtil.isASMAvailable());
    }

    @Test
    public void testIsASMAvailableConsistency() {
        // Test that multiple calls return the same result
        boolean firstCall = ASMUtil.isASMAvailable();
        boolean secondCall = ASMUtil.isASMAvailable();
        boolean thirdCall = ASMUtil.isASMAvailable();
        
        Assertions.assertEquals(firstCall, secondCall);
        Assertions.assertEquals(secondCall, thirdCall);
    }
}
