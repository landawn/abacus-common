package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.lang.reflect.Proxy;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Tag("unit")
public class ReflectionArgumentLifetimeTest {
    @TempDir
    Path directory;

    @Test
    void exactCompatibleNullAndPrimitiveLookupsKeepTheirBehavior() {
        for (int repeat = 0; repeat < 2; repeat++) {
            final var target = Reflection.on(Target.class).newInstance("\uD83D\uDE00");
            assertEquals(1, target.<Integer> invoke("accept", "text"));
            assertEquals(1, target.<Integer> invoke("accept", new Object()));
            assertEquals(1, target.<Integer> invoke("accept", (Object) null));
            assertEquals(7L, target.<Long> invoke("widen", 7).longValue());
            assertNotNull(Reflection.on(Target.class).newInstance((Object) null).instance());
            assertNotNull(Reflection.on(Target.class).newInstance(new Object()).instance());
            assertThrows(RuntimeException.class, () -> target.invoke("missing", ""));
        }
    }

    @Test
    void boxedArgumentsForPrimitiveParametersAreCachedAfterTheFirstResolution() {
        final var methodPool = Reflection.clsMethodPool.get(CountTarget.class);
        final var constructorPool = Reflection.clsConstructorPool.get(CountTarget.class);
        assertTrue(methodPool.isEmpty(), "the pool of a class only this test uses must start empty");
        assertTrue(constructorPool.isEmpty());

        // Integer -> int can never equal the declared signature, but it retains no loader the target class
        // does not already keep alive, so the resolution must be cached instead of repeated on every call.
        assertEquals(3, Reflection.on(new CountTarget(0)).<Integer> invoke("setCount", 3).intValue());
        assertEquals(1, methodPool.get("setCount").size());

        assertEquals(7, Reflection.on(CountTarget.class).newInstance(7).instance().count);
        assertEquals(1, constructorPool.size());
    }

    @Test
    void argumentTypesFromAnUnrelatedLoaderAreStillNotCached() throws Exception {
        try (final URLClassLoader loader = new URLClassLoader(new URL[0], ClassLoader.getPlatformClassLoader())) {
            final Object argument = Proxy.newProxyInstance(loader, new Class<?>[] { Runnable.class }, (proxy, method, arguments) -> null);

            assertEquals("described", Reflection.on(new DescribeTarget()).invoke("describe", argument));
            assertTrue(Reflection.clsMethodPool.get(DescribeTarget.class).get("describe").isEmpty(),
                    "an argument type from a loader unrelated to the target class must not be pinned by its cache");
        }
    }

    @Test
    @Timeout(60)
    void compatibleArgumentTypesDoNotPinTheirLoaderUnderTheLongLivedTarget() throws Exception {
        final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        final Path output = directory.resolve("argument-loader.log");
        final Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-Xmx96m", "-XX:ActiveProcessorCount=2",
                "-XX:+ClassUnloading", "-cp", classpath, LoaderProbe.class.getName()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(45, TimeUnit.SECONDS), "argument loader probe timed out");
            assertEquals(0, process.exitValue(), Files.readString(output));
        } finally {
            process.destroyForcibly();
        }
    }

    /**
     * Used by the primitive-parameter caching test only, so its metadata pools start out empty. The method is
     * private so the lookup cannot be answered by ReflectASM, which never reaches the metadata caches.
     */
    public static class CountTarget {
        public int count;

        public CountTarget(int count) {
            this.count = count;
        }

        private int setCount(int count) {
            this.count = count;
            return count;
        }
    }

    /** Used by the foreign-loader caching test only, so its metadata pools start out empty. */
    public static class DescribeTarget {
        private String describe(Object ignored) {
            return "described";
        }
    }

    public static class Target {
        public Target(Object ignored) {
        }

        private int accept(Object ignored) {
            return 1;
        }

        private long widen(long value) {
            return value;
        }
    }

    public static class LoaderProbe {
        public static void main(String[] args) throws Exception {
            final WeakReference<?>[] references = exercise();
            for (int round = 0; round < 80; round++) {
                System.gc();
                if (references[0].get() == null && references[1].get() == null) {
                    return;
                }
                final byte[][] pressure = new byte[8][];
                for (int i = 0; i < pressure.length; i++) {
                    pressure[i] = new byte[512 * 1024];
                }
                Thread.sleep(10);
            }
            throw new AssertionError("Reflection retained an argument class or loader");
        }

        private static WeakReference<?>[] exercise() throws Exception {
            try (final URLClassLoader loader = new URLClassLoader(new URL[0], ClassLoader.getPlatformClassLoader())) {
                final Object argument = Proxy.newProxyInstance(loader, new Class<?>[] { Runnable.class }, (proxy, method, arguments) -> null);
                final var reflected = Reflection.on(Target.class).newInstance(argument);
                if (reflected.<Integer> invoke("accept", argument) != 1) {
                    throw new AssertionError("compatible invocation failed");
                }
                return new WeakReference<?>[] { new WeakReference<>(argument.getClass()), new WeakReference<>(loader) };
            }
        }
    }
}
