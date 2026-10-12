package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class ReflectionHiddenOverloadReview20261003Test extends TestBase {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void privateOverloadParticipatesInSelection(final boolean call) {
        check(new PrivateChoice(), "choose", "value", "private", call);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void inheritedPrivateOverloadParticipatesInSelection(final boolean call) {
        check(new InheritedChoice(), "choose", "value", "inherited private", call);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void interfaceDefaultOverloadParticipatesInSelection(final boolean call) {
        check(new DefaultChoice(), "choose", "value", "default", call);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void nullSelectsTheMoreSpecificPrivateOverload(final boolean call) {
        check(new PrivateChoice(), "choose", null, "private", call);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void nullAmbiguityIsRejectedBeforeEitherOverloadRuns(final boolean call) {
        final AmbiguousChoice target = new AmbiguousChoice();
        final Reflection<AmbiguousChoice> reflection = Reflection.on(target);
        final RuntimeException failure = assertThrows(RuntimeException.class, () -> {
            if (call) {
                reflection.call("choose", (Object) null);
            } else {
                reflection.invoke("choose", (Object) null);
            }
        });
        assertTrue(failure.getMessage().contains("Ambiguous"));
        assertEquals(0, target.calls);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void boxingAndWideningStillSelectCompatibleMethods(final boolean call) {
        check(new PrimitiveChoice(), "widen", Character.valueOf('A'), "65", call);
        check(new PrimitiveChoice(), "boxed", Integer.valueOf(7), "number", call);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void uncomplicatedFastPathPropagatesTargetFailureOnlyOnce(final boolean call) {
        final ThrowingChoice target = new ThrowingChoice();
        assertTrue(ReflectASM.on(target).canInvoke("run", "value"));
        final Reflection<ThrowingChoice> reflection = Reflection.on(target);
        final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> {
            if (call) {
                reflection.call("run", "value");
            } else {
                reflection.invoke("run", "value");
            }
        });
        assertSame(target.failure, failure);
        assertEquals(1, target.calls);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void objectMethodsAndUnrelatedPrivateOverloadsRemainUsable(final boolean call) {
        check(new AmbiguousChoice(), "choose", "value", "string", call);
        final Object target = new Object();
        final Reflection<Object> reflection = Reflection.on(target);
        if (call) {
            assertSame(reflection, reflection.call("toString"));
        } else {
            assertEquals(target.toString(), reflection.invoke("toString"));
        }
    }

    // ---- follow-up 2026-10-04: overriding an Object method keeps the ReflectASM fast path ----

    @Test
    void overriddenObjectMethodsKeepTheFastPath() {
        for (final ObjectOverrides target : new ObjectOverrides[] { new ObjectOverrides(), new InheritedObjectOverrides() }) {
            final ReflectASM<ObjectOverrides> asm = ReflectASM.on(target);
            assertTrue(asm.canInvoke("toString"), target.getClass().getSimpleName());
            assertTrue(asm.canInvoke("hashCode"), target.getClass().getSimpleName());
            assertTrue(asm.canInvoke("equals", target), target.getClass().getSimpleName());

            final Reflection<ObjectOverrides> reflection = Reflection.on(target);
            assertEquals("overridden", reflection.invoke("toString"));
            assertEquals(42, (Integer) reflection.invoke("hashCode"));
            assertEquals(Boolean.TRUE, reflection.invoke("equals", "anything"));
        }
    }

    @Test
    void notOverriddenOrFinalObjectMethodsStillUseStandardReflection() {
        assertFalse(ReflectASM.on(new Object()).canInvoke("toString"));
        assertFalse(ReflectASM.on(new ObjectOverrides()).canInvoke("getClass"));

        // equals(EqualsOverload) does not override equals(Object): the hidden Object method still competes.
        final EqualsOverload target = new EqualsOverload();
        assertFalse(ReflectASM.on(target).canInvoke("equals", target));
        final Reflection<EqualsOverload> reflection = Reflection.on(target);
        assertEquals(Boolean.TRUE, reflection.invoke("equals", target));
        assertEquals("overload", target.selected);
        assertEquals(Boolean.FALSE, reflection.invoke("equals", "other"));
    }

    private static void check(final Counter target, final String method, final Object argument, final String expected, final boolean call) {
        final Reflection<Counter> reflection = Reflection.on(target);
        // Repeat to exercise cached metadata without changing which method is selected.
        for (int count = 1; count <= 2; count++) {
            if (call) {
                assertSame(reflection, reflection.call(method, argument));
            } else {
                assertEquals(expected, reflection.invoke(method, argument));
            }
            assertEquals(expected, target.selected);
            assertEquals(count, target.calls);
        }
    }

    public static class Counter {
        int calls;
        String selected;

        String mark(final String value) {
            calls++;
            selected = value;
            return value;
        }
    }

    public static class PrivateChoice extends Counter {
        public String choose(final Object value) {
            return mark("object");
        }

        private String choose(final String value) {
            return mark("private");
        }
    }

    public static class PrivateBase extends Counter {
        private String choose(final String value) {
            return mark("inherited private");
        }
    }

    public static class InheritedChoice extends PrivateBase {
        public String choose(final Object value) {
            return mark("object");
        }
    }

    public interface DefaultMethod {
        default String choose(final String value) {
            return ((Counter) this).mark("default");
        }
    }

    public static class DefaultChoice extends Counter implements DefaultMethod {
        public String choose(final Object value) {
            return mark("object");
        }
    }

    public static class AmbiguousChoice extends Counter {
        public String choose(final String value) {
            return mark("string");
        }

        private String choose(final Integer value) {
            return mark("integer");
        }
    }

    public static class PrimitiveChoice extends Counter {
        public String widen(final long value) {
            return mark(Long.toString(value));
        }

        private String widen(final String value) {
            return mark("string");
        }

        public String boxed(final int value) {
            return mark("primitive");
        }

        private String boxed(final Number value) {
            return mark("number");
        }
    }

    public static class ThrowingChoice extends Counter {
        final IllegalArgumentException failure = new IllegalArgumentException("target failure");

        public String run(final String value) {
            calls++;
            throw failure;
        }
    }

    public static class ObjectOverrides {
        @Override
        public String toString() {
            return "overridden";
        }

        @Override
        public int hashCode() {
            return 42;
        }

        @Override
        public boolean equals(final Object other) {
            return true;
        }
    }

    public static class InheritedObjectOverrides extends ObjectOverrides {
    }

    public static class EqualsOverload extends Counter {
        public boolean equals(final EqualsOverload other) {
            mark("overload");
            return true;
        }
    }
}
