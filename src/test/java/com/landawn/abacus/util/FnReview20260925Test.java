package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.UndeclaredThrowableException;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedInterruptedException;
import com.landawn.abacus.util.function.Predicate;

/**
 * Review fixes 2026-09-25 for {@link Fn}: U15-02 (class-doc interrupt-flag corner for an {@code InterruptedException}
 * reached by unwrapping an {@code ExecutionException}) and U15-03 (regression lock for the documented partial-match
 * contract of {@link Fn#matches(Pattern)}, ledger LST/C-006).
 */
public class FnReview20260925Test extends TestBase {

    // ---------------------------------------------------------------------------------------------------------
    // U15-02: the throwing adapters restore the interrupt flag for an InterruptedException - except for one reached
    // by unwrapping an ExecutionException, which was raised on the task's thread (ExceptionUtil.toRuntimeException)
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void u1502_interruptFlag_restoredForDirectAndSameThreadWrappers_notForAnExecutionExceptionHop() {
        Thread.interrupted(); // start from a clear flag

        try {
            final RuntimeException direct = assertThrows(RuntimeException.class, () -> Fn.ss(() -> {
                throw new InterruptedException("direct");
            }).get());
            assertInstanceOf(UncheckedInterruptedException.class, direct);
            assertTrue(Thread.interrupted(), "a direct InterruptedException restores the flag"); // and clears it again

            final RuntimeException viaUte = assertThrows(RuntimeException.class, () -> Fn.ss(() -> {
                throw new UndeclaredThrowableException(new InterruptedException("under UTE"));
            }).get());
            assertInstanceOf(UncheckedInterruptedException.class, viaUte);
            assertTrue(Thread.interrupted(), "an UndeclaredThrowableException hop stays on this thread: flag restored");

            final RuntimeException viaIte = assertThrows(RuntimeException.class, () -> Fn.ss(() -> {
                throw new InvocationTargetException(new InterruptedException("under ITE"));
            }).get());
            assertInstanceOf(UncheckedInterruptedException.class, viaIte);
            assertTrue(Thread.interrupted(), "an InvocationTargetException hop stays on this thread: flag restored");

            final RuntimeException viaEe = assertThrows(RuntimeException.class, () -> Fn.ss(() -> {
                throw new ExecutionException(new InterruptedException("under EE"));
            }).get());
            assertInstanceOf(UncheckedInterruptedException.class, viaEe, "converted the same way");
            assertFalse(Thread.currentThread().isInterrupted(),
                    "an InterruptedException under an ExecutionException belongs to the task's thread: flag left alone");

            // the same rule for a sibling adapter: all of them convert through ExceptionUtil.toRuntimeException(e, true)
            final RuntimeException viaEeFf = assertThrows(RuntimeException.class, () -> Fn.ff((String s) -> {
                throw new ExecutionException(new InterruptedException("under EE"));
            }).apply("x"));
            assertInstanceOf(UncheckedInterruptedException.class, viaEeFf);
            assertFalse(Thread.currentThread().isInterrupted());

            final RuntimeException viaEeRr = assertThrows(RuntimeException.class, () -> Fn.rr(() -> {
                throw new ExecutionException(new InterruptedException("under EE"));
            }).run());
            assertInstanceOf(UncheckedInterruptedException.class, viaEeRr);
            assertFalse(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted(); // never leave the flag set for the rest of the suite
        }
    }

    // ---------------------------------------------------------------------------------------------------------
    // U15-03 / LST/C-006: Fn.matches(Pattern) is a CONTAINS-a-match test (Matcher.find()), not a whole-string match
    // ---------------------------------------------------------------------------------------------------------

    @Test
    public void c006_matches_isAPartialMatch() {
        final Pattern digits = Pattern.compile("\\d+");
        final Predicate<CharSequence> containsDigits = Fn.matches(digits);

        assertTrue(containsDigits.test("123"));
        assertTrue(containsDigits.test("a1"), "contains a match: a partial match is enough");
        assertTrue(containsDigits.test("1a"));
        assertTrue(containsDigits.test(new StringBuilder("x9y")), "any CharSequence");
        assertFalse(containsDigits.test("abc"));
        assertFalse(containsDigits.test(""));
        assertFalse(containsDigits.test(null), "a null input tests false");

        // the whole-string alternatives the javadoc points to disagree on the partial input
        assertFalse(digits.asMatchPredicate().test("a1"));
        assertTrue(digits.asMatchPredicate().test("123"));
        assertTrue(digits.asPredicate().test("a1"), "Pattern.asPredicate() is the JDK twin of Fn.matches");

        assertThrows(IllegalArgumentException.class, () -> Fn.matches((Pattern) null));
    }
}
