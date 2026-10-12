package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/** Performance and memory regression contracts for Reflection. */
@Tag("slow-test")
public class ReflectionPerformanceMemoryTest extends TestBase {

    // ---- perf review 2026-10-08 G055 end ----
    // ---- perf review 2026-10-08 G058 begin ----
    @Nested
    class G058 {

        /** Fast-path candidates and every reason ReflectASM declines a call; each method records what ran. */
        public static class Target {
            public int calls;
            public String selected;

            public String first() {
                mark("first");
                return "first";
            }

            public String one(final String s) {
                mark("one");
                return "one:" + s;
            }

            public String same(final String s) {
                mark("sameS");
                return "sameS:" + s;
            }

            public String same(final Integer i) {
                mark("sameI");
                return "sameI:" + i;
            }

            public String arity(final int a) {
                mark("arity1");
                return "arity1:" + a;
            }

            public String arity(final int a, final int b) {
                mark("arity2");
                return "arity2:" + (a + b);
            }

            public static String stat(final String s) {
                return "stat:" + s;
            }

            public String vararg(final String... values) {
                mark("vararg");
                return "vararg:" + values.length;
            }

            public String hidden(final Object o) {
                mark("hiddenO");
                return "hiddenO:" + o;
            }

            @SuppressWarnings("unused")
            private String hidden(final String s) {
                mark("hiddenS");
                return "hiddenS:" + s;
            }

            public long widen(final long v) {
                mark("widen");
                return v + 1;
            }

            public String boxed(final Integer v) {
                mark("boxed");
                return "boxed:" + v;
            }

            public int prim(final int v) {
                mark("prim");
                return v * 2;
            }

            public String fail(final String s) throws java.io.IOException {
                mark("fail");
                throw new java.io.IOException("io:" + s);
            }

            public void setter(final String s) {
                mark("setter:" + s);
            }

            public String last() {
                mark("last");
                return "last";
            }

            @Override
            public String toString() {
                return "Target";
            }

            void mark(final String s) {
                calls++;
                selected = s;
            }
        }

        /** Overrides first() (two table entries for the name) and inherits the rest. */
        public static class Child extends Target {
            @Override
            public String first() {
                mark("childFirst");
                return "childFirst";
            }

            public String childOnly(final String s) {
                mark("childOnly");
                return "childOnly:" + s;
            }
        }

        /** Many uniquely named methods, so the selected method's position in the table varies widely. */
        public static class Wide {
            public int calls;

            public String w00() {
                calls++;
                return "w00";
            }

            public String w01() {
                calls++;
                return "w01";
            }

            public String w02() {
                calls++;
                return "w02";
            }

            public String w03() {
                calls++;
                return "w03";
            }

            public String w04() {
                calls++;
                return "w04";
            }

            public String w05() {
                calls++;
                return "w05";
            }

            public String w06() {
                calls++;
                return "w06";
            }

            public String w07() {
                calls++;
                return "w07";
            }

            public String w08() {
                calls++;
                return "w08";
            }

            public String w09() {
                calls++;
                return "w09";
            }

            public String w10() {
                calls++;
                return "w10";
            }

            public String w11() {
                calls++;
                return "w11";
            }

            public String w12() {
                calls++;
                return "w12";
            }

            public String w13() {
                calls++;
                return "w13";
            }

            public String w14() {
                calls++;
                return "w14";
            }

            public String w15() {
                calls++;
                return "w15";
            }

            public String w16() {
                calls++;
                return "w16";
            }

            public String w17() {
                calls++;
                return "w17";
            }

            public String w18() {
                calls++;
                return "w18";
            }

            public String w19() {
                calls++;
                return "w19";
            }

            public String w20(final String s) {
                calls++;
                return "w20:" + s;
            }

            public String w21(final String s, final Integer i) {
                calls++;
                return "w21:" + s + i;
            }

            public String w22(final int i) {
                calls++;
                return "w22:" + i;
            }

            public String w23() {
                calls++;
                return "w23";
            }
        }

        private static void assertCanInvoke(final boolean expected, final Object target, final String name, final Object... arguments) {
            assertEquals(expected, ReflectASM.on(target).canInvoke(name, arguments), () -> target.getClass().getSimpleName() + "." + name);
        }

        // G058-01: canInvoke answers from the per-class name index exactly as the full table scan did
        @Test
        void testCanInvoke_nameIndexMatchesTableScanRules() {
            final Target t = new Target();
            assertCanInvoke(true, t, "first");
            assertCanInvoke(true, t, "last");
            assertCanInvoke(true, t, "one", "x");
            assertCanInvoke(true, t, "one", (Object) null);
            assertCanInvoke(false, t, "one");
            assertCanInvoke(false, t, "one", 5);
            assertCanInvoke(false, t, "one", "x", "y");
            assertCanInvoke(false, t, "same", "x");
            assertCanInvoke(false, t, "same", 1);
            assertCanInvoke(true, t, "arity", 1);
            assertCanInvoke(true, t, "arity", 1, 2);
            assertCanInvoke(false, t, "arity", (Object) null);
            assertCanInvoke(false, t, "arity", "1");
            assertCanInvoke(true, t, "stat", "x");
            assertCanInvoke(true, t, "vararg", (Object) new String[] { "a" });
            assertCanInvoke(false, t, "vararg", "a");
            assertCanInvoke(false, t, "hidden", "x");
            assertCanInvoke(false, t, "hidden", new Object());
            assertCanInvoke(false, t, "widen", 5);
            assertCanInvoke(true, t, "widen", 5L);
            assertCanInvoke(true, t, "boxed", 5);
            assertCanInvoke(true, t, "prim", 3);
            assertCanInvoke(false, t, "prim", (Object) null);
            assertCanInvoke(true, t, "toString");
            assertCanInvoke(false, t, "hashCode");
            assertCanInvoke(false, t, "getClass");
            assertCanInvoke(false, t, "noSuchMethod");
            assertCanInvoke(false, t, "");
            assertTrue(ReflectASM.on(t).canInvoke("first", (Object[]) null));
            assertFalse(ReflectASM.on(t).canInvoke("one", (Object[]) null));

            final Child c = new Child();
            assertCanInvoke(false, c, "first");
            assertCanInvoke(true, c, "childOnly", "x");
            assertCanInvoke(true, c, "one", "x");
            assertCanInvoke(true, c, "last");

            assertCanInvoke(true, "hello", "isEmpty");
            assertCanInvoke(true, "hello", "length");
            assertCanInvoke(false, new ArrayList<>(), "size");
            assertFalse(ReflectASM.on(java.util.Comparator.class).canInvoke("naturalOrder"));

            // repeated lookups (served from the cached index) give the same answers
            for (int i = 0; i < 3; i++) {
                assertCanInvoke(true, t, "arity", 1, 2);
                assertCanInvoke(false, t, "same", "x");
                assertCanInvoke(false, t, "noSuchMethod" + i);
            }
        }

        // G058-01: the fast path runs exactly the method the name lookup selected, once, wherever it sits in the table
        @Test
        void testInvoke_fastPathSelectsSameMethodAtEveryTablePosition() {
            final Wide wide = new Wide();
            int expectedCalls = 0;

            for (int round = 0; round < 2; round++) {
                for (int i = 0; i < 24; i++) {
                    final String name = (i < 10 ? "w0" : "w") + i;
                    final Object[] arguments = i == 20 ? new Object[] { "s" }
                            : i == 21 ? new Object[] { "s", 7 } : i == 22 ? new Object[] { 9 } : new Object[0];
                    final String expected = i == 20 ? "w20:s" : i == 21 ? "w21:s7" : i == 22 ? "w22:9" : name;

                    assertTrue(ReflectASM.on(wide).canInvoke(name, arguments), name);
                    assertEquals(expected, Reflection.on(wide).invoke(name, arguments));
                    assertEquals(++expectedCalls, wide.calls, name);

                    final Reflection<Wide> reflection = Reflection.on(wide);
                    assertSame(reflection, reflection.call(name, arguments));
                    assertEquals(++expectedCalls, wide.calls, name);
                }
            }

            for (final String name : new String[] { "isEmpty", "length", "isBlank", "toUpperCase", "trim", "strip", "hashCode", "intern" }) {
                final Object viaReflection = Reflection.on((Object) "Hello").invoke(name);
                assertEquals(directStringCall("Hello", name), viaReflection, name);
            }
        }

        private Object directStringCall(final String s, final String name) {
            switch (name) {
                case "isEmpty":
                    return s.isEmpty();
                case "length":
                    return s.length();
                case "isBlank":
                    return s.isBlank();
                case "toUpperCase":
                    return s.toUpperCase();
                case "trim":
                    return s.trim();
                case "strip":
                    return s.strip();
                case "hashCode":
                    return s.hashCode();
                case "intern":
                    return s.intern();
                default:
                    throw new IllegalArgumentException(name);
            }
        }

        // G058-01: results, selected overload and call counts of invoke/call on both paths (fast path and fallback)
        @Test
        void testInvokeAndCall_resultsAndCallbacksOnBothPaths() {
            final String[][] cases = {
                    // name, expected result, expected selected
                    { "first", "first", "first" }, { "last", "last", "last" } };

            for (final String[] row : cases) {
                final Target t = new Target();
                assertEquals(row[1], Reflection.on(t).invoke(row[0]));
                assertEquals(row[2], t.selected);
                assertEquals(1, t.calls);
                final Reflection<Target> r = Reflection.on(t);
                assertSame(r, r.call(row[0]));
                assertEquals(2, t.calls);
            }

            Target t = new Target();
            assertEquals("one:x", Reflection.on(t).invoke("one", "x"));
            assertEquals("one:null", Reflection.on(t).invoke("one", (Object) null));
            assertEquals("sameS:x", Reflection.on(t).invoke("same", "x"));
            assertEquals("sameI:4", Reflection.on(t).invoke("same", 4));
            assertEquals("arity1:3", Reflection.on(t).invoke("arity", 3));
            assertEquals("arity2:7", Reflection.on(t).invoke("arity", 3, 4));
            assertEquals("stat:x", Reflection.on(t).invoke("stat", "x"));
            assertEquals("vararg:2", Reflection.on(t).invoke("vararg", (Object) new String[] { "a", "b" }));
            assertEquals("hiddenS:x", Reflection.on(t).invoke("hidden", "x"));
            assertEquals("hiddenO:5", Reflection.on(t).invoke("hidden", 5));
            assertEquals(Long.valueOf(6L), Reflection.on(t).invoke("widen", 5));
            assertEquals(Long.valueOf(6L), Reflection.on(t).invoke("widen", 5L));
            assertEquals("boxed:5", Reflection.on(t).invoke("boxed", 5));
            assertEquals(Integer.valueOf(6), Reflection.on(t).invoke("prim", 3));
            assertEquals(Integer.valueOf(130), Reflection.on(t).invoke("prim", 'A'));
            assertEquals("Target", Reflection.on(t).invoke("toString"));
            assertEquals(14, t.calls);
            assertSame(Target.class, Reflection.on(t).invoke("getClass"));

            final Reflection<Target> setter = Reflection.on(t);
            assertSame(setter, setter.call("setter", "v"));
            assertEquals("setter:v", t.selected);
            assertEquals(15, t.calls);

            final Child c = new Child();
            assertEquals("childFirst", Reflection.on(c).invoke("first"));
            assertEquals("childOnly:y", Reflection.on(c).invoke("childOnly", "y"));
            assertEquals("one:z", Reflection.on(c).invoke("one", "z"));
            assertEquals(3, c.calls);

            assertEquals(Integer.valueOf(3), Reflection.on(new ArrayList<>(Arrays.asList(1, 2, 3))).invoke("size"));
            assertEquals(Integer.valueOf(5), Reflection.on((Object) "hello").invoke("length"));
            assertEquals("stat:q", Reflection.on(Target.class).invoke("stat", "q"));
        }

        // G058-01: failures on the fast path are unchanged - target exceptions, missing instance, unresolvable calls
        @Test
        void testInvoke_failuresUnchanged() {
            final Target t = new Target();
            final RuntimeException wrapped = assertThrows(RuntimeException.class, () -> Reflection.on(t).invoke("fail", "x"));
            assertTrue(wrapped.getCause() instanceof java.io.IOException);
            assertEquals("io:x", wrapped.getCause().getMessage());
            assertEquals(1, t.calls);

            final RuntimeException wrappedByCall = assertThrows(RuntimeException.class, () -> Reflection.on(t).call("fail", "y"));
            assertEquals("io:y", wrappedByCall.getCause().getMessage());
            assertEquals(2, t.calls);

            assertThrows(NullPointerException.class, () -> Reflection.on(Target.class).invoke("first"));
            assertThrows(NullPointerException.class, () -> Reflection.on(Target.class).call("one", "x"));

            final RuntimeException missing = assertThrows(RuntimeException.class, () -> Reflection.on(t).invoke("noSuchMethod"));
            assertEquals("No method found by name: noSuchMethod with parameter types: []", missing.getMessage());

            final RuntimeException wrongArity = assertThrows(RuntimeException.class, () -> Reflection.on(t).invoke("one"));
            assertEquals("No method found by name: one with parameter types: []", wrongArity.getMessage());

            final RuntimeException nullPrimitive = assertThrows(RuntimeException.class, () -> Reflection.on(t).invoke("prim", (Object) null));
            assertEquals("No method found by name: prim with parameter types: [null]", nullPrimitive.getMessage());

            final RuntimeException spread = assertThrows(RuntimeException.class, () -> Reflection.on(t).invoke("vararg", "a", "b"));
            assertTrue(spread.getMessage().startsWith("No method found by name: vararg"), spread.getMessage());

            assertThrows(IllegalArgumentException.class, () -> Reflection.on(t).invoke(null));
            assertThrows(IllegalArgumentException.class, () -> Reflection.on(t).call(null));
            assertEquals(2, t.calls);
        }

        // G058-01: the name index is built from the class's own method table and does not grow with looked-up names
        @Test
        @SuppressWarnings("unchecked")
        void testInvoke_nameIndexBoundedByMethodTable() throws Exception {
            final Target t = new Target();

            for (int i = 0; i < 200; i++) {
                final String name = "noSuchMethod" + i;
                assertThrows(RuntimeException.class, () -> Reflection.on(t).invoke(name));
            }

            java.lang.reflect.Field indexField = null;

            try {
                indexField = ReflectASM.class.getDeclaredField("METHOD_INDEXES_BY_NAME");
            } catch (final NoSuchFieldException e) {
                // pristine build: no name index, nothing to check
            }

            if (indexField != null) {
                indexField.setAccessible(true);
                final Map<String, int[]> index = ((ClassValue<Map<String, int[]>>) indexField.get(null)).get(Target.class);
                final java.lang.reflect.Field accessField = ReflectASM.class.getDeclaredField("METHOD_ACCESS_CACHE");
                accessField.setAccessible(true);
                final String[] table = ((ClassValue<com.esotericsoftware.reflectasm.MethodAccess>) accessField.get(null)).get(Target.class).getMethodNames();
                assertEquals(new HashSet<>(Arrays.asList(table)), index.keySet());

                int entries = 0;

                for (final Map.Entry<String, int[]> entry : index.entrySet()) {
                    int previous = -1;

                    for (final int position : entry.getValue()) {
                        assertTrue(position > previous);
                        assertEquals(entry.getKey(), table[position]);
                        previous = position;
                        entries++;
                    }
                }

                assertEquals(table.length, entries);
            }
        }
    }
}
