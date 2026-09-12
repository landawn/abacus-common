package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

public class ClassMetadataCacheTest extends TestBase {
    @TempDir
    Path temp;

    @Test
    void namesPackagesAndClassKindsMatchJdkIncludingAbsentMetadata() {
        class Local {
        }
        Object anonymous = new Object() {
        };
        for (Class<?> type : List.of(String.class, int.class, void.class, String[].class, int[].class, Local.class, anonymous.getClass(), Fixture.class)) {
            assertEquals(type.getName(), ClassUtil.getClassName(type));
            assertEquals(type.getSimpleName(), ClassUtil.getSimpleClassName(type));
            assertEquals(type.getCanonicalName() == null ? type.getName() : type.getCanonicalName(), ClassUtil.getCanonicalClassName(type));
            assertSame(type.getPackage(), ClassUtil.getPackage(type));
            assertEquals(type.getPackage() == null ? "" : type.getPackage().getName(), ClassUtil.getPackageName(type));
            assertSame(type.getEnclosingClass(), ClassUtil.getEnclosingClass(type));
            assertEquals(type.isAnonymousClass(), ClassUtil.isAnonymousClass(type));
            assertEquals(type.isMemberClass(), ClassUtil.isMemberClass(type));
            assertEquals(type.isAnonymousClass() || type.isMemberClass(), ClassUtil.isAnonymousOrMemberClass(type));
        }
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.getPackage(null));
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.getPackageName(null));
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.getSimpleClassName(null));
        assertThrows(IllegalArgumentException.class, () -> ClassUtil.getDeclaredConstructor(null));
    }

    @Test
    void reflectionCachesCopySignaturesPreserveAccessAndSupportConcurrentLookups() throws Exception {
        Constructor<Fixture> noArgs = ClassUtil.getDeclaredConstructor(Fixture.class, (Class<?>[]) null);
        assertSame(noArgs, ClassUtil.getDeclaredConstructor(Fixture.class));
        assertFalse(noArgs.isAccessible());
        Class<?>[] constructorTypes = { String.class };
        Constructor<Fixture> constructor = ClassUtil.getDeclaredConstructor(Fixture.class, constructorTypes);
        constructorTypes[0] = Integer.class;
        assertSame(constructor, ClassUtil.getDeclaredConstructor(Fixture.class, String.class));
        assertNull(ClassUtil.getDeclaredConstructor(Fixture.class, constructorTypes));
        Class<?>[] methodTypes = { String.class };
        Method method = ClassUtil.getDeclaredMethod(Fixture.class, "\u65b9\u6cd5", methodTypes);
        methodTypes[0] = Integer.class;
        assertSame(method, ClassUtil.getDeclaredMethod(Fixture.class, "\u65b9\u6cd5", String.class));
        assertNull(ClassUtil.getDeclaredMethod(Fixture.class, "\u65b9\u6cd5", methodTypes));
        assertFalse(method.isAccessible());
        assertEquals(Fixture.class.getDeclaredMethod("hello"), ClassUtil.getDeclaredMethod(Fixture.class, "HeLLo", (Class<?>[]) null));
        assertNull(ClassUtil.getDeclaredMethod(Fixture.class, "missing"));
        try (var executor = Executors.newFixedThreadPool(4)) {
            List<Callable<Method>> tasks = new ArrayList<>();
            for (int i = 0; i < 32; i++) {
                tasks.add(() -> ClassUtil.getDeclaredMethod(Fixture.class, "\u65b9\u6cd5", String.class));
            }
            for (var result : executor.invokeAll(tasks)) {
                assertSame(method, result.get());
            }
        }
    }

    @Test
    @Timeout(60)
    void disposableClassAndFailedSignaturesOnParentClassesDoNotPinLoader() throws Exception {
        Path source = temp.resolve("Disposable.java");
        Files.writeString(source, """
                package reviewloader;
                public class Disposable {
                    public Disposable() {}
                    public Disposable(Disposable value) {}
                    public void echo(Disposable value) {}
                    public static class Nested {}
                }
                """, StandardCharsets.UTF_8);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", temp.toString(), source.toString()));
        String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        Path output = temp.resolve("loader-probe.log");
        Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-Xmx96m", "-XX:+ClassUnloading", "-cp",
                classpath, LoaderProbe.class.getName(), temp.toString()).redirectErrorStream(true).redirectOutput(output.toFile()).start();
        try {
            assertTrue(process.waitFor(45, TimeUnit.SECONDS), "loader probe timed out");
            assertEquals(0, process.exitValue(), Files.readString(output));
        } finally {
            process.destroyForcibly();
        }
    }

    private static class Fixture {
        private Fixture() {
        }

        private Fixture(String value) {
        }

        private void hello() {
        }

        private void \u65b9\u6cd5(String value) {
        }
    }

    public static class LoaderProbe {
        public static void main(String[] args) throws Exception {
            WeakReference<?>[] references = exercise(Path.of(args[0]));
            for (int round = 0; round < 80; round++) {
                System.gc();
                if (references[0].get() == null && references[1].get() == null) {
                    System.out.println("class and loader collected");
                    return;
                }
                byte[][] pressure = new byte[8][];
                for (int i = 0; i < pressure.length; i++) {
                    pressure[i] = new byte[512 * 1024];
                }
                Thread.sleep(10);
            }
            throw new AssertionError("ClassUtil metadata retained disposable class or loader");
        }

        private static WeakReference<?>[] exercise(Path classes) throws Exception {
            try (URLClassLoader loader = new URLClassLoader(new URL[] { classes.toUri().toURL() }, ClassLoader.getPlatformClassLoader())) {
                Class<?> type = loader.loadClass("reviewloader.Disposable");
                Class<?> nested = loader.loadClass("reviewloader.Disposable$Nested");
                ClassUtil.getClassName(type);
                ClassUtil.getCanonicalClassName(type);
                ClassUtil.getSimpleClassName(type);
                ClassUtil.getPackage(type);
                ClassUtil.getPackageName(type);
                ClassUtil.getEnclosingClass(nested);
                ClassUtil.isAnonymousClass(type);
                ClassUtil.isMemberClass(nested);
                ClassUtil.isAnonymousOrMemberClass(nested);
                if (ClassUtil.getDeclaredConstructor(type) == null || ClassUtil.getDeclaredConstructor(type, type) == null
                        || ClassUtil.getDeclaredMethod(type, "echo", type) == null) {
                    throw new AssertionError("fixture reflection failed");
                }
                // A failed child-parameter signature must not be stored in a parent class's ClassValue.
                if (ClassUtil.getDeclaredConstructor(String.class, type) != null || ClassUtil.getDeclaredMethod(String.class, "substring", type) != null) {
                    throw new AssertionError("unexpected parent signature");
                }
                return new WeakReference<?>[] { new WeakReference<>(type), new WeakReference<>(loader) };
            }
        }
    }
}
