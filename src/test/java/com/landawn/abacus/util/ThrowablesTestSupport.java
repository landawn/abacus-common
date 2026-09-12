package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicInteger;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Throwables.Iterator;

public abstract class ThrowablesTestSupport extends TestBase {

    public static class TestException extends Exception {
        public TestException(String message) {
            super(message);
        }
    }

    public static class TestRuntimeException extends RuntimeException {
        public TestRuntimeException(String message) {
            super(message);
        }
    }

    protected static Iterator<String, Exception> closeTrackingIterator(final AtomicInteger closeCount) {
        return new Iterator<>() {
            protected boolean consumed;

            @Override
            public boolean hasNext() {
                return !consumed;
            }

            @Override
            public String next() {
                if (consumed) {
                    throw new NoSuchElementException();
                }

                consumed = true;
                return "value";
            }

            @Override
            protected void closeResourceInternal() {
                closeCount.incrementAndGet();
            }
        };
    }

    protected static Iterator<String, Exception> closeFailingIterator(final AtomicInteger closeCount, final RuntimeException failure) {
        return new Iterator<>() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public String next() {
                throw new NoSuchElementException();
            }

            @Override
            protected void closeResourceInternal() {
                closeCount.incrementAndGet();
                throw failure;
            }
        };
    }

    //    @Test

    public void testIterator_Empty() throws Exception {
        Throwables.Iterator<String, Exception> iter = Throwables.Iterator.empty();
        assertFalse(iter.hasNext());
    }

    // ========== Tests for 52 untested methods ==========

    // 1. TriPredicate (generic)

    // 2. QuadPredicate

    // 3. TriFunction (generic)

    // 4. QuadFunction

    // 5. TriConsumer (generic)

    // 6. QuadConsumer

    // 7. ByteConsumer

    // 8. ShortConsumer

    // 9. FloatConsumer

    // 10. BytePredicate

    // 11. ShortPredicate

    // 12. FloatPredicate

    // 13. LongPredicate

    // 14. CharFunction

    // 15. ByteFunction

    // 16. ShortFunction

    // 17. FloatFunction

    // 18. LongFunction

    // 19. DoubleFunction

    // 20. IntToDoubleFunction

    // 21. LongToIntFunction

    // 22. LongToDoubleFunction

    // 23. FloatToIntFunction

    // 24. FloatToLongFunction

    // 25. FloatToDoubleFunction

    // 26. DoubleToLongFunction

    // 27. ToByteFunction

    // 28. ToShortFunction

    // 29. ToFloatFunction

    // 30. ToLongFunction

    // 31. ByteUnaryOperator

    // 32. ShortUnaryOperator

    // 33. LongUnaryOperator

    // 34. FloatUnaryOperator

    // 35. CharBinaryOperator

    // 36. ByteBinaryOperator

    // 37. ShortBinaryOperator

    // 38. LongBinaryOperator

    // 39. FloatBinaryOperator

    // 40. CharTernaryOperator

    // 41. ByteTernaryOperator

    // 42. ShortTernaryOperator

    // 43. LongTernaryOperator

    // 44. FloatTernaryOperator

    // 45. ByteBiPredicate

    // 46. ShortBiPredicate

    // 47. LongBiPredicate

    // 48. FloatBiPredicate

    // 49. ByteBiFunction

    // 50. ShortBiFunction

    // 51. LongBiFunction

    // 52. FloatBiFunction

    // === Tests for review fixes / additional coverage ===

    // FloatToIntFunction must produce an int (regression: it used to return double).

    // FloatToLongFunction must produce a long (regression: it used to return double).

    // Verify andThen semantics: this-then-after, not after-then-this.

    // Verify Predicate.negate handles checked-exception path correctly.

    // Verify Runnable.unchecked wraps checked exceptions in RuntimeException.

    // Verify Supplier.unchecked propagates value when no exception.

    // Verify Function.unchecked correctly wraps and applies.

    // Verify Consumer.unchecked side-effects work.

    // Iterator.advance with negative or zero is a no-op (should not consume).
}
