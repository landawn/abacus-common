package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;

import com.landawn.abacus.TestBase;

@Tag("unit")
class TypeFactoryRegistrationRaceReview20261003Test extends TestBase {
    interface Marker {
    }

    interface ClassStrict extends Marker {
    }

    interface NameStrict extends Marker {
    }

    interface ClassFunction extends Marker {
    }

    interface NameFunction extends Marker {
    }

    interface ClassBiFunction extends Marker {
    }

    interface NameBiFunction extends Marker {
    }

    interface LateFallback extends Marker {
    }

    interface LateExplicit extends Marker {
    }

    interface LateRuntime extends Marker {
    }

    interface LateError extends Marker {
    }

    interface NamedRuntime extends Marker {
    }

    interface NamedError extends Marker {
    }

    interface ExistingStrict extends Marker {
    }

    interface ExistingFunction extends Marker {
    }

    interface ExistingBiFunction extends Marker {
    }

    interface ExplicitFirstFunction extends Marker {
    }

    interface ExplicitFirstBiFunction extends Marker {
    }

    @ParameterizedTest
    @CsvSource({ "0,true", "0,false", "1,true", "1,false", "2,true", "2,false" })
    void successfulRegistrationSurvivesAnAlreadyRunningLookup(int mode, boolean lookupByClass) throws Exception {
        Class<?>[][] targets = { { ClassStrict.class, NameStrict.class }, { ClassFunction.class, NameFunction.class },
                { ClassBiFunction.class, NameBiFunction.class } };
        Class<?> target = targets[mode][lookupByClass ? 0 : 1];
        Gate gate = new Gate();
        Type<Marker> blocker = new AbstractType<>("RaceReviewBlocker" + target.getSimpleName()) {
            @Override
            public Class<Marker> javaType() {
                gate.pause();
                return Marker.class;
            }

            @Override
            public String stringOf(Marker value) {
                return "marker";
            }

            @Override
            public Marker valueOf(String value) {
                return null;
            }
        };
        TypeFactory.registerType(blocker);
        AtomicReference<Type<?>> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = worker(gate, failure, () -> result.set(lookupByClass ? TypeFactory.getType(target) : TypeFactory.getType(target.getCanonicalName())));
        Type<?> custom;
        try {
            gate.awaitEntered();
            custom = register(mode, target);
        } finally {
            gate.resume(worker);
        }
        assertNull(failure.get());
        assertSame(custom, result.get());
        assertSame(custom, TypeFactory.getType(target));
        assertSame(custom, TypeFactory.getType(target.getCanonicalName()));
        assertSame(custom, Type.ofList(target).elementType());
    }

    @Test
    void canonicalFallbackPublishedAfterTheInitialCheckIsReplaced() throws Exception {
        Gate gate = new Gate();
        Type<Object> custom = handler(LateFallback.class, gate, null);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = worker(gate, failure, () -> TypeFactory.registerType(cast(LateFallback.class), custom));
        try {
            gate.awaitEntered();
            Type<?> fallback = TypeFactory.getType(LateFallback.class.getCanonicalName());
            assertInstanceOf(ObjectType.class, fallback);
            assertNotSame(custom, fallback);
        } finally {
            gate.resume(worker);
        }
        assertNull(failure.get());
        assertSame(custom, TypeFactory.getType(LateFallback.class));
        assertSame(custom, TypeFactory.getType(LateFallback.class.getCanonicalName()));
    }

    @Test
    void concurrentExplicitCanonicalRegistrationWinsAndStrictLoserLeavesNoAliases() throws Exception {
        Gate gate = new Gate();
        Type<Object> rejected = handler(LateExplicit.class, gate, null);
        Type<Object> winner = plainHandler(LateExplicit.class, "RaceReviewExplicitWinner");
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = worker(gate, failure, () -> TypeFactory.registerType(cast(LateExplicit.class), rejected));
        try {
            gate.awaitEntered();
            TypeFactory.registerType(LateExplicit.class.getCanonicalName(), winner);
        } finally {
            gate.resume(worker);
        }
        assertInstanceOf(IllegalArgumentException.class, failure.get());
        assertNull(TypeFactory.getTypeIfPresent(rejected.name()));
        assertSame(winner, TypeFactory.getType(LateExplicit.class));
        assertSame(winner, TypeFactory.getType(LateExplicit.class.getCanonicalName()));
        assertSame(winner, TypeFactory.getType(winner.name()));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void classificationFailureAfterClassPublicationRollsBackOnlyTheRejectedMappings(boolean error) throws Exception {
        Class<?> target = error ? LateError.class : LateRuntime.class;
        Gate gate = new Gate();
        Type<Object> rejected = handler(target, gate, null);
        Throwable expected = error ? new AssertionError("classification") : new IllegalStateException("classification");
        AtomicReference<Thread> throwingThread = new AtomicReference<>();
        Type<Object> winner = handler(target, null, () -> {
            if (Thread.currentThread() == throwingThread.get()) {
                rethrow(expected);
            }
        });
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Thread worker = worker(gate, failure, () -> TypeFactory.registerType(cast(target), rejected));
        throwingThread.set(worker);
        try {
            gate.awaitEntered();
            TypeFactory.registerType(target.getCanonicalName(), winner);
        } finally {
            gate.resume(worker);
            throwingThread.set(null);
        }
        assertSame(expected, failure.get());
        assertNull(TypeFactory.getTypeIfPresent(rejected.name()));
        assertSame(winner, TypeFactory.getType(target));
        assertSame(winner, TypeFactory.getType(target.getCanonicalName()));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void namedRegistrationClassificationFailureLeavesNoPublishedAlias(boolean biFunction) {
        Class<?> target = biFunction ? NamedError.class : NamedRuntime.class;
        Throwable expected = biFunction ? new AssertionError("named classification") : new IllegalStateException("named classification");
        AtomicReference<Thread> throwingThread = new AtomicReference<>(Thread.currentThread());
        Type<Object> existing = handler(target, null, () -> {
            if (Thread.currentThread() == throwingThread.get()) {
                rethrow(expected);
            }
        });
        TypeFactory.registerType(target.getCanonicalName(), existing);
        try {
            Throwable actual = assertThrows(expected.getClass(), () -> register(biFunction ? 2 : 1, target));
            assertSame(expected, actual);
        } finally {
            throwingThread.set(null);
        }
        assertNull(TypeFactory.getTypeIfPresent(customName(target)));
        assertSame(existing, TypeFactory.getType(target));
        assertSame(existing, TypeFactory.getType(target.getCanonicalName()));
    }

    // ---- follow-up 2026-10-04: named overloads when an explicit canonical type owns the class ----

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void namedOverloadKeepsItsAliasButNotTheClassWhenAnExplicitCanonicalTypeExists(boolean biFunction) {
        Class<?> target = biFunction ? ExplicitFirstBiFunction.class : ExplicitFirstFunction.class;
        Type<Object> winner = plainHandler(target, "RaceReviewExplicitFirst" + target.getSimpleName());
        TypeFactory.registerType(target.getCanonicalName(), winner);

        Type<?> alias = register(biFunction ? 2 : 1, target); // must not throw
        assertNotSame(winner, alias);
        assertSame(target, alias.javaType());
        assertSame(alias, TypeFactory.getType(customName(target)));
        assertSame(winner, TypeFactory.getType(target));
        assertSame(winner, TypeFactory.getType(target.getCanonicalName()));
    }

    /**
     * The window between a named overload winning the class slot and publishing its canonical name cannot be paused
     * from outside (the overload builds its own handler), so race the two registrations repeatedly. Every
     * interleaving must leave the class and its canonical name answering with the same handler, the named alias
     * reachable, and the named overload free of exceptions.
     */
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void namedOverloadRacingAnExplicitCanonicalRegistrationKeepsClassAndNameConsistent(boolean biFunction) throws Exception {
        for (int i = 0; i < 200; i++) {
            Class<?> target = generatedMarker(biFunction, i);
            String canonicalName = target.getCanonicalName();
            String aliasName = "RaceGenAlias" + target.getSimpleName();
            Type<Object> winner = plainHandler(target, "RaceGenWinner" + target.getSimpleName());
            CyclicBarrier start = new CyclicBarrier(2);
            AtomicReference<Throwable> namedFailure = new AtomicReference<>();
            AtomicReference<Throwable> explicitFailure = new AtomicReference<>();
            Thread named = new Thread(() -> {
                try {
                    start.await(10, TimeUnit.SECONDS);
                    if (biFunction) {
                        TypeFactory.registerType(aliasName, cast(target), (value, parser) -> "custom", (value, parser) -> null);
                    } else {
                        TypeFactory.registerType(aliasName, cast(target), value -> "custom", value -> null);
                    }
                } catch (Throwable e) {
                    namedFailure.set(e);
                }
            });
            Thread explicit = new Thread(() -> {
                try {
                    start.await(10, TimeUnit.SECONDS);
                    TypeFactory.registerType(canonicalName, winner);
                } catch (Throwable e) {
                    explicitFailure.set(e);
                }
            });
            named.start();
            explicit.start();
            named.join(10000);
            explicit.join(10000);
            assertFalse(named.isAlive() || explicit.isAlive(), "registration threads must finish");

            assertNull(namedFailure.get(), "the named overload never throws for a canonical conflict");
            Type<?> alias = TypeFactory.getType(aliasName);
            assertSame(target, alias.javaType());
            if (explicitFailure.get() == null) {
                assertSame(winner, TypeFactory.getType(canonicalName));
                assertSame(winner, TypeFactory.getType(target), "the class must follow the explicit canonical winner");
            } else {
                // The named overload published the canonical name first, so the explicit name was taken.
                assertInstanceOf(IllegalArgumentException.class, explicitFailure.get());
                assertSame(alias, TypeFactory.getType(canonicalName));
                assertSame(alias, TypeFactory.getType(target));
            }
        }
    }

    private static final GeneratedClassLoader GENERATED = new GeneratedClassLoader();

    /** Defines a fresh, uniquely named marker interface: class registrations are permanent, so each race needs its own class. */
    private static Class<?> generatedMarker(boolean biFunction, int index) {
        String name = "com.landawn.abacus.type.racegen.RaceMarker" + (biFunction ? "Bi" : "Fn") + index + "x" + System.nanoTime();
        ClassWriter writer = new ClassWriter(0);
        writer.visit(Opcodes.V17, Opcodes.ACC_PUBLIC | Opcodes.ACC_INTERFACE | Opcodes.ACC_ABSTRACT, name.replace('.', '/'), null, "java/lang/Object", null);
        writer.visitEnd();
        return GENERATED.define(name, writer.toByteArray());
    }

    private static final class GeneratedClassLoader extends ClassLoader {
        GeneratedClassLoader() {
            super(TypeFactoryRegistrationRaceReview20261003Test.class.getClassLoader());
        }

        Class<?> define(String name, byte[] bytes) {
            return defineClass(name, bytes, 0, bytes.length);
        }
    }

    @Test
    void existingFallbacksRemainReplaceableAcrossAllRegistrationPaths() {
        Class<?>[] targets = { ExistingStrict.class, ExistingFunction.class, ExistingBiFunction.class };
        for (int mode = 0; mode < targets.length; mode++) {
            Class<?> target = targets[mode];
            Type<?> fallback = TypeFactory.getType(target.getCanonicalName());
            Type<?> registered = register(mode, target);
            assertNotSame(fallback, registered);
            assertSame(registered, TypeFactory.getType(target));
            assertSame(registered, TypeFactory.getType(target.getCanonicalName()));
            assertSame(registered, Type.ofList(target).elementType());
        }
    }

    @Test
    void builtInClassKeepsItsHandlerWhenAnAlternativeNameIsRegistered() {
        Type<?> original = TypeFactory.getType(String.class);
        String alias = "RaceReviewAlternativeString";
        TypeFactory.registerType(alias, String.class, value -> "alternative:" + value, value -> value);
        assertSame(original, TypeFactory.getType(String.class));
        assertSame(original, TypeFactory.getType(String.class.getCanonicalName()));
        assertNotSame(original, TypeFactory.getType(alias));
        assertThrows(IllegalArgumentException.class,
                () -> TypeFactory.registerType(cast(String.class), plainHandler(String.class, "RaceReviewRejectedString")));
        assertNull(TypeFactory.getTypeIfPresent("RaceReviewRejectedString"));
    }

    private static Type<?> register(int mode, Class<?> target) {
        String name = customName(target);
        if (mode == 0) {
            Type<Object> custom = plainHandler(target, name);
            TypeFactory.registerType(cast(target), custom);
            return custom;
        } else if (mode == 1) {
            TypeFactory.registerType(name, cast(target), value -> "custom", value -> null);
        } else {
            TypeFactory.registerType(name, cast(target), (value, parser) -> "custom", (value, parser) -> null);
        }
        return TypeFactory.getType(name);
    }

    private static String customName(Class<?> target) {
        return "RaceReviewCustom" + target.getSimpleName();
    }

    private static Type<Object> plainHandler(Class<?> target, String name) {
        return new AbstractType<>(name) {
            @Override
            public Class<Object> javaType() {
                return cast(target);
            }

            @Override
            public String stringOf(Object value) {
                return "custom";
            }

            @Override
            public Object valueOf(String value) {
                return null;
            }
        };
    }

    private static Type<Object> handler(Class<?> target, Gate nameGate, Runnable classCallback) {
        return new AbstractType<>(customName(target) + (nameGate == null ? "Existing" : "Rejected")) {
            @Override
            public String name() {
                if (nameGate != null) {
                    nameGate.pause();
                }
                return super.name();
            }

            @Override
            public Class<Object> javaType() {
                if (classCallback != null) {
                    classCallback.run();
                }
                return cast(target);
            }

            @Override
            public String stringOf(Object value) {
                return "custom";
            }

            @Override
            public Object valueOf(String value) {
                return null;
            }
        };
    }

    @SuppressWarnings("unchecked")
    private static Class<Object> cast(Class<?> target) {
        return (Class<Object>) target;
    }

    private static void rethrow(Throwable failure) {
        if (failure instanceof Error error) {
            throw error;
        }
        throw (RuntimeException) failure;
    }

    private static Thread worker(Gate gate, AtomicReference<Throwable> failure, Runnable action) {
        Thread thread = new Thread(() -> {
            try {
                action.run();
            } catch (Throwable e) {
                failure.set(e);
            }
        }, "type-factory-review");
        thread.setDaemon(true);
        gate.thread = thread;
        thread.start();
        return thread;
    }

    private static final class Gate {
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch released = new CountDownLatch(1);
        volatile Thread thread;

        void pause() {
            if (Thread.currentThread() != thread) {
                return;
            }
            entered.countDown();
            try {
                assertTrue(released.await(10, TimeUnit.SECONDS), "callback release timed out");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new AssertionError(e);
            }
        }

        void awaitEntered() throws InterruptedException {
            assertTrue(entered.await(10, TimeUnit.SECONDS), "callback not reached");
        }

        void resume(Thread worker) throws InterruptedException {
            released.countDown();
            worker.join(10000);
            assertFalse(worker.isAlive(), "worker did not complete");
        }
    }
}
