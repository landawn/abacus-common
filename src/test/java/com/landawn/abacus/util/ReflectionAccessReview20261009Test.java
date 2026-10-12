package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class ReflectionAccessReview20261009Test extends TestBase {

    @Test
    void invokesNonPublicTargetsWithoutModuleOpens() throws Exception {
        String java = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        Process process = new ProcessBuilder(java, "-cp", System.getProperty("java.class.path"), AccessProbe.class.getName()).redirectErrorStream(true).start();
        try {
            assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Reflection access probe timed out");
            String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
            assertEquals(0, process.exitValue(), output);
            assertTrue(output.contains("reflection-access-ok"), output);
        } finally {
            process.destroyForcibly();
        }
    }

    @Test
    void doesNotRetryAnAccessErrorThrownByTheTarget() {
        PublicTarget target = new PublicTarget();
        assertSame(target.failure, assertThrows(IllegalAccessError.class, () -> Reflection.on(target).invoke("fail")));
        assertEquals(1, target.calls);
    }

    @Test
    void fieldsTheGeneratedAccessorCannotReachUseReflectionDirectly() {
        final HiddenFieldHolder hidden = new HiddenFieldHolder();
        assertEquals("v", Reflection.on(hidden).get("value"));
        Reflection.on(hidden).set("value", "w");
        assertEquals("w", hidden.value);

        final PublicFieldHolder target = new PublicFieldHolder();
        Reflection.on(target).set("packageValue", "p");
        assertEquals("p", Reflection.on(target).get("packageValue"));
        Reflection.on(target).set("finalValue", "f");
        assertEquals("f", Reflection.on(target).get("finalValue"));
        final HiddenArgument argument = new HiddenArgument();
        Reflection.on(target).set("hiddenTyped", argument);
        assertSame(argument, target.hiddenTyped);

        // None of these reached the generated accessor, so none was recorded as a linkage failure (which would also
        // log the once-per-JVM "failed to link" warning meant for a broken ReflectASM deployment).
        assertFalse(Reflection.asmUnreachableFields.get(HiddenFieldHolder.class).contains("value"));
        assertFalse(Reflection.asmUnreachableFields.get(PublicFieldHolder.class).contains("packageValue"));
        assertFalse(Reflection.asmUnreachableFields.get(PublicFieldHolder.class).contains("finalValue"));
        assertFalse(Reflection.asmUnreachableFields.get(PublicFieldHolder.class).contains("hiddenTyped"));
    }

    public static class AccessProbe {
        public static void main(String[] args) {
            HiddenTarget hidden = new HiddenTarget();
            assertEquals("hidden", Reflection.on(hidden).invoke("value"));
            Reflection.on(hidden).call("value");
            assertEquals(2, hidden.calls);

            PublicTarget target = new PublicTarget();
            assertEquals("package", Reflection.on(target).invoke("packageValue"));
            assertEquals("protected", Reflection.on(target).invoke("protectedValue"));
            Reflection.on(target).call("packageValue");
            Reflection.on(target).call("protectedValue");
            assertEquals(4, target.calls);

            // Public methods whose parameter types are not public: the generated accessor's argument cast would fail.
            assertEquals("arg", Reflection.on(target).invoke("hiddenParameter", new HiddenArgument()));
            assertEquals(2, (int) Reflection.on(target).invoke("hiddenArrayParameter", (Object) new HiddenArgument[2]));
            Reflection.on(target).call("hiddenParameter", new HiddenArgument());
            assertEquals(7, target.calls);
            System.out.println("reflection-access-ok");
        }
    }

    static class HiddenTarget {
        int calls;

        public String value() {
            calls++;
            return "hidden";
        }
    }

    static class HiddenFieldHolder {
        public String value = "v";
    }

    public static class PublicFieldHolder {
        String packageValue;
        public final String finalValue = new String("initial");
        public HiddenArgument hiddenTyped;
    }

    static class HiddenArgument {
        final String value = "arg";
    }

    public static class PublicTarget {
        int calls;
        final IllegalAccessError failure = new IllegalAccessError("target failure");

        public String hiddenParameter(final HiddenArgument argument) {
            calls++;
            return argument.value;
        }

        public int hiddenArrayParameter(final HiddenArgument[] arguments) {
            calls++;
            return arguments.length;
        }

        String packageValue() {
            calls++;
            return "package";
        }

        protected String protectedValue() {
            calls++;
            return "protected";
        }

        public void fail() {
            calls++;
            throw failure;
        }
    }
}
