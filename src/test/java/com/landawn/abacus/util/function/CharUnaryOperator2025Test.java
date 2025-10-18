package com.landawn.abacus.util.function;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class CharUnaryOperator2025Test extends TestBase {

    @Test
    public void testApplyAsChar() {
        CharUnaryOperator toUpper = Character::toUpperCase;

        assertEquals('A', toUpper.applyAsChar('a'));
        assertEquals('Z', toUpper.applyAsChar('z'));
    }

    @Test
    public void testApplyAsCharWithLambda() {
        CharUnaryOperator toLower = Character::toLowerCase;

        assertEquals('a', toLower.applyAsChar('A'));
        assertEquals('z', toLower.applyAsChar('Z'));
    }

    @Test
    public void testApplyAsCharWithAnonymousClass() {
        CharUnaryOperator increment = new CharUnaryOperator() {
            @Override
            public char applyAsChar(char operand) {
                return (char) (operand + 1);
            }
        };

        assertEquals('b', increment.applyAsChar('a'));
        assertEquals('B', increment.applyAsChar('A'));
    }

    @Test
    public void testCompose() {
        CharUnaryOperator toUpper = Character::toUpperCase;
        CharUnaryOperator increment = c -> (char) (c + 1);

        CharUnaryOperator composed = toUpper.compose(increment);

        assertEquals('B', composed.applyAsChar('a'));
    }

    @Test
    public void testAndThen() {
        CharUnaryOperator toUpper = Character::toUpperCase;
        CharUnaryOperator increment = c -> (char) (c + 1);

        CharUnaryOperator composed = toUpper.andThen(increment);

        assertEquals('B', composed.applyAsChar('a'));
    }

    @Test
    public void testComposeAndAndThen() {
        CharUnaryOperator toUpper = Character::toUpperCase;
        CharUnaryOperator increment = c -> (char) (c + 1);
        CharUnaryOperator decrement = c -> (char) (c - 1);

        CharUnaryOperator composed = toUpper.compose(increment).andThen(decrement);

        assertEquals('A', composed.applyAsChar('a'));
    }

    @Test
    public void testIdentity() {
        CharUnaryOperator identity = CharUnaryOperator.identity();

        assertEquals('a', identity.applyAsChar('a'));
        assertEquals('Z', identity.applyAsChar('Z'));
        assertEquals('\0', identity.applyAsChar('\0'));
    }

    @Test
    public void testWithBoundaryValues() {
        CharUnaryOperator identity = CharUnaryOperator.identity();

        assertEquals(Character.MAX_VALUE, identity.applyAsChar(Character.MAX_VALUE));
        assertEquals(Character.MIN_VALUE, identity.applyAsChar(Character.MIN_VALUE));
    }

    @Test
    public void testMethodReference() {
        CharUnaryOperator toUpper = Character::toUpperCase;

        assertEquals('A', toUpper.applyAsChar('a'));
    }

    @Test
    public void testFunctionalInterface() {
        assertNotNull(CharUnaryOperator.class.getAnnotation(FunctionalInterface.class));
    }
}
