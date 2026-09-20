package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

@org.junit.jupiter.api.Tag("unit")
public class ReviewActionsUtilTest extends TestBase {
    @Test
    public void optionalConversionsRejectMissingContainersAndPreserveEmptyAndPresentValues() {
        assertThrows(IllegalArgumentException.class, () -> u.Optional.from((java.util.Optional<String>) null));
        assertFalse(u.Optional.from(java.util.Optional.empty()).isPresent());
        assertEquals("value", u.Optional.from(java.util.Optional.of("value")).get());
        assertThrows(IllegalArgumentException.class, () -> u.OptionalInt.from(null));
        assertThrows(IllegalArgumentException.class, () -> u.OptionalLong.from(null));
        assertThrows(IllegalArgumentException.class, () -> u.OptionalDouble.from(null));
        assertThrows(IllegalArgumentException.class, () -> u.Nullable.from((java.util.Optional<String>) null));
        assertThrows(IllegalArgumentException.class, () -> u.Nullable.from((u.Optional<String>) null));
        assertTrue(u.Nullable.from(u.Optional.empty()).isEmpty());
        assertEquals("value", u.Nullable.from(u.Optional.of("value")).get());
    }

    @Test
    public void rowViewsDetectColumnChangesAndCanBeLoggedAfterInvalidation() {
        final List<Consumer<Dataset>> changes = List.of(d -> d.addColumn("c", List.of(5, 6)), d -> d.removeColumn("a"), d -> d.renameColumn("a", "c"),
                d -> d.swapColumns("a", "b"), d -> d.moveColumn("a", 1), d -> d.renameColumns(java.util.Map.of("a", "c", "b", "d")),
                d -> d.removeColumns(List.of("a")));
        for (final Consumer<Dataset> change : changes) {
            final Dataset data = Dataset.rows(List.of("a", "b"), new Object[][] { { 1, 2 }, { 3, 4 } });
            final Dataset.Row row = data.row(0);
            row.set("a", 9);
            assertEquals(9, row.getInt("a"));
            assertEquals("Row[0]=[9, 2]", row.toString());
            change.accept(data);
            assertThrows(ConcurrentModificationException.class, row::columnCount);
            assertThrows(ConcurrentModificationException.class, () -> row.get(0));
            assertThrows(ConcurrentModificationException.class, () -> row.set(0, 10));
            assertEquals("Row[0]=<invalidated>", row.toString());
            assertDoesNotThrow(() -> data.row(0).columnCount());
        }
        final Dataset parent = Dataset.rows(List.of("a"), new Object[][] { { 1 }, { 2 } });
        final Dataset.Row row = parent.slice(0, 1).row(0);
        parent.removeRow(0);
        assertEquals("Row[0]=<invalidated>", row.toString());
    }

    @Test
    public void optionalConvertersRejectMissingContainersAndPreserveEmptyAndPresentValues() {
        assertThrows(IllegalArgumentException.class, () -> u.Nullable.from((java.util.Optional<String>) null));
        assertThrows(IllegalArgumentException.class, () -> u.OptionalInt.from(null));
        assertThrows(IllegalArgumentException.class, () -> u.OptionalLong.from(null));
        assertThrows(IllegalArgumentException.class, () -> u.OptionalDouble.from(null));
        assertTrue(u.Nullable.from(java.util.Optional.empty()).isEmpty());
        assertTrue(u.OptionalInt.from(java.util.OptionalInt.empty()).isEmpty());
        assertTrue(u.OptionalLong.from(java.util.OptionalLong.empty()).isEmpty());
        assertTrue(u.OptionalDouble.from(java.util.OptionalDouble.empty()).isEmpty());
        assertEquals("v", u.Nullable.from(java.util.Optional.of("v")).get());
        assertEquals(3, u.OptionalInt.from(java.util.OptionalInt.of(3)).get());
        assertEquals(4L, u.OptionalLong.from(java.util.OptionalLong.of(4)).get());
        assertEquals(2.5, u.OptionalDouble.from(java.util.OptionalDouble.of(2.5)).get());
    }

    @Test
    public void enumEmptyTokensRoundTripThroughValueOnlyAndCreatorCodecs() {
        assertSame(EmptyValue.EMPTY, Type.of(EmptyValue.class).valueOf(Type.of(EmptyValue.class).stringOf(EmptyValue.EMPTY)));
        assertSame(EmptyCreator.EMPTY, Type.of(EmptyCreator.class).valueOf(Type.of(EmptyCreator.class).stringOf(EmptyCreator.EMPTY)));
        assertNull(Type.of(TimeUnit.class).valueOf(""));
        assertNull(Type.of(EmptyCreator.class).valueOf((String) null));
    }

    public enum EmptyValue {
        EMPTY;

        @com.fasterxml.jackson.annotation.JsonValue
        public String token() {
            return "";
        }
    }

    public enum EmptyCreator {
        EMPTY;

        @com.fasterxml.jackson.annotation.JsonValue
        public String token() {
            return "";
        }

        @com.fasterxml.jackson.annotation.JsonCreator
        public static EmptyCreator from(String value) {
            if ("".equals(value))
                return EMPTY;
            throw new IllegalArgumentException(value);
        }
    }

    @Test
    public void walkRejectsNullAndKeepsMissingPathsEmpty() {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.walk(null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.walk(null, true, false));
        assertEquals(0, IOUtil.walk(new File("build/actions-batch-18/missing-" + java.util.UUID.randomUUID())).count());
    }

    @Test
    public void rowViewsFailAfterStructuralChangesAndRemainLiveForCellEdits() {
        List<Consumer<Dataset>> changes = List.of(d -> d.removeRow(0), d -> d.addRow(0, new Object[] { 9 }), d -> d.sortBy("id"), d -> d.swapRows(0, 1));
        for (Consumer<Dataset> change : changes) {
            Dataset dataset = Dataset.rows(List.of("id"), new Object[][] { { 2 }, { 1 }, { 3 } });
            Dataset.Row row = dataset.row(1);
            dataset.moveToRow(2);
            row.set(0, 4);
            assertEquals(4, row.getInt(0));
            assertEquals(2, dataset.currentRowIndex());
            change.accept(dataset);
            assertThrows(ConcurrentModificationException.class, row::rowIndex);
            assertThrows(ConcurrentModificationException.class, row::columnCount);
            assertThrows(ConcurrentModificationException.class, () -> row.columnIndex("id"));
            assertThrows(ConcurrentModificationException.class, () -> row.get(0));
            assertThrows(ConcurrentModificationException.class, () -> row.set(0, 99));
            assertEquals("Row[1]=<invalidated>", row.toString());
            assertDoesNotThrow(() -> dataset.row(0).get(0));
        }
        Dataset parent = Dataset.rows(List.of("id"), new Object[][] { { 1 }, { 2 } });
        Dataset.Row sliced = parent.slice(0, 1).row(0);
        parent.removeRow(0);
        assertThrows(ConcurrentModificationException.class, () -> sliced.get(0));
    }

    @ParameterizedTest
    @ValueSource(strings = { "row", "column", "slice" })
    public void everyRowAccessorRejectsInvalidationBeforeReadingOrWriting(final String change) {
        final Dataset parent = Dataset.rows(List.of("flag", "letter", "number"), new Object[][] { { true, 'a', 7 }, { false, 'b', 8 } });
        final Dataset data = change.equals("slice") ? parent.slice(0, 1) : parent;
        final Dataset.Row row = data.row(0);
        parent.moveToRow(1);
        parent.set(0, 2, 9);
        assertEquals(9, row.getInt("number"));
        assertEquals(1, parent.currentRowIndex());
        final List<Executable> accessors = List.of(row::rowIndex, row::columnCount, () -> row.columnIndex("number"), () -> row.get(2), () -> row.get("number"),
                () -> row.isNull(2), () -> row.isNull("number"), row::toArray, () -> row.getBoolean(0), () -> row.getBoolean("flag"), () -> row.getChar(1),
                () -> row.getChar("letter"), () -> row.getByte(2), () -> row.getByte("number"), () -> row.getShort(2), () -> row.getShort("number"),
                () -> row.getInt(2), () -> row.getInt("number"), () -> row.getLong(2), () -> row.getLong("number"), () -> row.getFloat(2),
                () -> row.getFloat("number"), () -> row.getDouble(2), () -> row.getDouble("number"));
        for (final Executable accessor : accessors) {
            assertDoesNotThrow(accessor);
        }
        for (final Executable setter : List.<Executable> of(() -> row.set(2, 9), () -> row.set("number", 9))) {
            if (change.equals("slice")) {
                // Slices are read-only but remain live for parent cell edits until structural invalidation.
                assertThrows(IllegalStateException.class, setter);
            } else {
                assertDoesNotThrow(setter);
            }
        }
        // Even a change outside this row's position invalidates it. Name-based access must fail before
        // a renamed column produces an unrelated missing-column error, and stale setters must not mutate data.
        if (change.equals("column")) {
            parent.renameColumn("number", "renamed");
        } else {
            parent.removeRow(1);
        }
        for (int i = 0; i < accessors.size(); i++) {
            assertThrows(ConcurrentModificationException.class, accessors.get(i), change + " accessor " + i);
        }
        assertThrows(ConcurrentModificationException.class, () -> row.set(2, 99));
        assertThrows(ConcurrentModificationException.class, () -> row.set("number", 99));
        assertEquals(Integer.valueOf(9), parent.<Integer> get(0, 2));
        assertEquals("Row[0]=<invalidated>", row.toString());
    }

    @Test
    public void throttleFirstCallbacksRunOutsideMonitorAndTerminalCannotOvertakeAcceptedItem() throws Exception {
        Observer<Integer> observer = Observer.of(List.<Integer> of()).throttleFirst(1, TimeUnit.NANOSECONDS);
        Observer.Dispatcher<Object> throttle = observer.dispatcher.downDispatcher;
        List<String> events = new ArrayList<>();
        CountDownLatch entered = new CountDownLatch(1), release = new CountDownLatch(1);
        observer.dispatcher.append(new Observer.Dispatcher<>() {
            @Override
            public void onNext(Object value) {
                assertFalse(Thread.holdsLock(throttle.holder));
                events.add("next");
                entered.countDown();
                try {
                    assertTrue(release.await(5, TimeUnit.SECONDS));
                } catch (InterruptedException e) {
                    throw new AssertionError(e);
                }
                events.add("returned");
            }

            @Override
            public void onComplete() {
                assertFalse(Thread.holdsLock(throttle.holder));
                events.add("complete");
            }
        });
        CompletableFuture<Void> delivery = CompletableFuture.runAsync(() -> observer.dispatcher.onNext(1));
        try {
            assertTrue(entered.await(5, TimeUnit.SECONDS));
            CompletableFuture.runAsync(observer.dispatcher::onComplete).get(2, TimeUnit.SECONDS);
            observer.dispatcher.onNext(2);
            assertEquals(List.of("next"), events);
        } finally {
            release.countDown();
        }
        delivery.get(5, TimeUnit.SECONDS);
        observer.dispatcher.onComplete();
        assertEquals(List.of("next", "returned", "complete"), events);
    }

    @Test
    public void throttleFirstOrdersReentrantErrorAndDoesNotRepeatFailingTerminalCallbacks() {
        Observer<Integer> observer = Observer.of(List.<Integer> of()).throttleFirst(1, TimeUnit.NANOSECONDS);
        List<String> events = new ArrayList<>();
        RuntimeException error = new IllegalStateException("failure");
        observer.dispatcher.append(new Observer.Dispatcher<>() {
            @Override
            public void onNext(Object value) {
                events.add("next");
                observer.dispatcher.onError(error);
                events.add("returned");
            }

            @Override
            public void onError(Exception failure) {
                assertSame(error, failure);
                events.add("error");
                throw error;
            }
        });
        assertSame(error, assertThrows(RuntimeException.class, () -> observer.dispatcher.onNext(1)));
        observer.dispatcher.onError(error);
        observer.dispatcher.onNext(2);
        assertEquals(List.of("next", "returned", "error"), events);
    }

    @ParameterizedTest
    @ValueSource(strings = { "none", "complete", "error" })
    public void throttleFirstConvertsCallbackFailureToOneErrorAndDiscardsQueuedSignals(final String queuedTerminal) {
        final Observer<Integer> observer = Observer.of(List.<Integer> of()).throttleFirst(1, TimeUnit.NANOSECONDS);
        final Observer.Dispatcher<Object> throttle = observer.dispatcher.downDispatcher;
        final List<String> events = new ArrayList<>();
        final RuntimeException callbackFailure = new IllegalStateException("onNext failed");
        observer.dispatcher.append(new Observer.Dispatcher<>() {
            @Override
            public void onNext(final Object value) {
                assertFalse(Thread.holdsLock(throttle.holder));
                events.add("next:" + value);
                if (Integer.valueOf(1).equals(value)) {
                    queueThrottleSignals(observer, queuedTerminal);
                    assertEquals(List.of("next:1"), events);
                    throw callbackFailure;
                }
            }

            @Override
            public void onError(final Exception failure) {
                assertFalse(Thread.holdsLock(throttle.holder));
                assertSame(callbackFailure, failure);
                events.add("error");
            }

            @Override
            public void onComplete() {
                events.add("complete");
            }
        });

        // Drive the dispatcher directly so source-level error handling cannot mask the throttle's behavior.
        assertDoesNotThrow(() -> observer.dispatcher.onNext(1));
        observer.dispatcher.onNext(3);
        observer.dispatcher.onComplete();
        observer.dispatcher.onError(new IllegalStateException("late error"));
        assertEquals(List.of("next:1", "error"), events);
    }

    @ParameterizedTest
    @ValueSource(strings = { "none", "complete", "error" })
    public void throttleFirstPropagatesOriginalErrorAndDiscardsQueuedSignals(final String queuedTerminal) {
        final Observer<Integer> observer = Observer.of(List.<Integer> of()).throttleFirst(1, TimeUnit.NANOSECONDS);
        final List<String> events = new ArrayList<>();
        final AssertionError callbackFailure = new AssertionError("fatal onNext failure");
        observer.dispatcher.append(new Observer.Dispatcher<>() {
            @Override
            public void onNext(final Object value) {
                events.add("next:" + value);
                if (Integer.valueOf(1).equals(value)) {
                    queueThrottleSignals(observer, queuedTerminal);
                    assertEquals(List.of("next:1"), events);
                    throw callbackFailure;
                }
            }

            @Override
            public void onError(final Exception failure) {
                events.add("error");
            }

            @Override
            public void onComplete() {
                events.add("complete");
            }
        });

        assertSame(callbackFailure, assertThrows(AssertionError.class, () -> observer.dispatcher.onNext(1)));
        observer.dispatcher.onNext(3);
        observer.dispatcher.onComplete();
        observer.dispatcher.onError(new IllegalStateException("late error"));
        assertEquals(List.of("next:1"), events);
    }

    @Test
    public void throttleFirstPropagatesErrorHandlerFailureWithoutRepeatingNotification() {
        final Observer<Integer> observer = Observer.of(List.<Integer> of()).throttleFirst(1, TimeUnit.NANOSECONDS);
        final List<String> events = new ArrayList<>();
        final RuntimeException callbackFailure = new IllegalStateException("onNext failed");
        final RuntimeException errorHandlerFailure = new IllegalArgumentException("onError failed");
        observer.dispatcher.append(new Observer.Dispatcher<>() {
            @Override
            public void onNext(final Object value) {
                events.add("next:" + value);
                if (Integer.valueOf(1).equals(value)) {
                    queueThrottleSignals(observer, "complete");
                    throw callbackFailure;
                }
            }

            @Override
            public void onError(final Exception failure) {
                assertSame(callbackFailure, failure);
                events.add("error");
                throw errorHandlerFailure;
            }

            @Override
            public void onComplete() {
                events.add("complete");
            }
        });

        assertSame(errorHandlerFailure, assertThrows(RuntimeException.class, () -> observer.dispatcher.onNext(1)));
        observer.dispatcher.onNext(3);
        observer.dispatcher.onComplete();
        observer.dispatcher.onError(callbackFailure);
        assertEquals(List.of("next:1", "error"), events);
    }

    @ParameterizedTest
    @ValueSource(strings = { "complete-runtime", "complete-error", "error-runtime", "error-error" })
    public void throttleFirstTerminalFailuresPropagateAfterQueuedItemsWithoutAnotherNotification(final String scenario) {
        final boolean completes = scenario.startsWith("complete");
        final boolean fatal = scenario.endsWith("-error");
        final Throwable callbackFailure = fatal ? new AssertionError("terminal callback failed") : new IllegalStateException("terminal callback failed");
        final RuntimeException sourceFailure = new IllegalArgumentException("source failed");
        final Observer<Integer> observer = Observer.of(List.<Integer> of()).throttleFirst(1, TimeUnit.NANOSECONDS);
        final Observer.Dispatcher<Object> throttle = observer.dispatcher.downDispatcher;
        final List<String> events = new ArrayList<>();
        final Runnable failTerminal = () -> {
            assertFalse(Thread.holdsLock(throttle.holder));
            if (callbackFailure instanceof Error error) {
                throw error;
            }
            throw (RuntimeException) callbackFailure;
        };
        observer.dispatcher.append(new Observer.Dispatcher<>() {
            @Override
            public void onNext(final Object value) {
                events.add("next:" + value);
                if (Integer.valueOf(1).equals(value)) {
                    queueThrottleSignals(observer, "none");
                    if (completes) {
                        observer.dispatcher.onComplete();
                    } else {
                        observer.dispatcher.onError(sourceFailure);
                    }
                }
            }

            @Override
            public void onError(final Exception failure) {
                assertSame(sourceFailure, failure);
                events.add("error");
                failTerminal.run();
            }

            @Override
            public void onComplete() {
                events.add("complete");
                failTerminal.run();
            }
        });
        // The terminal callback runs only after the reserved second item. Its own failure is not a new signal.
        final Class<? extends Throwable> failureType = fatal ? AssertionError.class : RuntimeException.class;
        assertSame(callbackFailure, assertThrows(failureType, () -> observer.dispatcher.onNext(1)));
        observer.dispatcher.onNext(3);
        observer.dispatcher.onComplete();
        observer.dispatcher.onError(sourceFailure);
        assertEquals(List.of("next:1", "next:2", completes ? "complete" : "error"), events);
    }

    private static void queueThrottleSignals(final Observer<Integer> observer, final String terminal) {
        // Let the one-nanosecond suppression window expire without sleeps, even on a coarse clock.
        // The reentrant item is then accepted while the original onNext callback still owns the drain.
        final long started = System.nanoTime();
        while (System.nanoTime() == started) {
            Thread.onSpinWait();
        }
        observer.dispatcher.onNext(2);
        switch (terminal) {
            case "none":
                break;
            case "complete":
                observer.dispatcher.onComplete();
                break;
            case "error":
                observer.dispatcher.onError(new IllegalStateException("queued error"));
                break;
            default:
                throw new IllegalArgumentException("Unexpected terminal signal: " + terminal);
        }
    }
}
