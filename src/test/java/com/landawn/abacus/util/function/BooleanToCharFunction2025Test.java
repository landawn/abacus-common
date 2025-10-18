package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class BooleanToCharFunction2025Test extends TestBase {

    @Test
    public void testApplyAsChar() {
        BooleanToCharFunction function = value -> value ? 'T' : 'F';

        assertEquals('T', function.applyAsChar(true));
        assertEquals('F', function.applyAsChar(false));
    }

    @Test
    public void testApplyAsCharWithLambda() {
        BooleanToCharFunction function = value -> value ? 'Y' : 'N';

        assertEquals('Y', function.applyAsChar(true));
        assertEquals('N', function.applyAsChar(false));
    }

    @Test
    public void testApplyAsCharWithNumbers() {
        BooleanToCharFunction function = value -> value ? '1' : '0';

        assertEquals('1', function.applyAsChar(true));
        assertEquals('0', function.applyAsChar(false));
    }

    @Test
    public void testApplyAsCharWithException() {
        BooleanToCharFunction function = value -> {
            throw new RuntimeException("Test exception");
        };

        assertThrows(RuntimeException.class, () -> function.applyAsChar(true));
    }

    @Test
    public void testAnonymousClass() {
        BooleanToCharFunction function = new BooleanToCharFunction() {
            @Override
            public char applyAsChar(boolean value) {
                return value ? 'A' : 'Z';
            }
        };

        assertEquals('A', function.applyAsChar(true));
        assertEquals('Z', function.applyAsChar(false));
    }

    @Test
    public void testConstantReturn() {
        BooleanToCharFunction function = value -> 'X';

        assertEquals('X', function.applyAsChar(true));
        assertEquals('X', function.applyAsChar(false));
    }
}
