package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

@org.junit.jupiter.api.Tag("unit")
public class FactoryCacheLifetimeTest extends TestBase {
    @TempDir
    Path directory;

    @Test
    @Timeout(120)
    void automaticAndRegisteredCollectionAndMapFactoriesReleaseTheirTargetLoaders() throws Exception {
        final Path source = directory.resolve("Factories.java");
        Files.writeString(source, """
                package disposable;
                public class Factories {
                    public static class Values extends java.util.ArrayList<Object> {
                        public static int calls;
                        public Values() { calls++; }
                        public Values(int capacity) { super(capacity); calls++; }
                    }
                    public static class Entries extends java.util.HashMap<Object,Object> {
                        public static int calls;
                        public Entries() { calls++; }
                        public Entries(int capacity) { super(capacity); calls++; }
                    }
                }
                """);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", directory.toString(), source.toString()));
        final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        for (int mode = 0; mode < 8; mode++) {
            final Path log = directory.resolve("mode-" + mode + ".log");
            final Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-Xmx96m",
                    "-XX:ActiveProcessorCount=2", "-XX:+ClassUnloading", "-cp", classpath, LoaderProbe.class.getName(), Integer.toString(mode),
                    directory.toString()).redirectErrorStream(true).redirectOutput(log.toFile()).start();
            try {
                assertTrue(process.waitFor(12, TimeUnit.SECONDS), "factory mode " + mode);
                assertEquals(0, process.exitValue(), Files.readString(log));
            } finally {
                process.destroyForcibly();
            }
        }
    }

    public static class LoaderProbe {
        public static void main(String[] args) throws Exception {
            final WeakReference<?>[] references = exercise(Integer.parseInt(args[0]), Path.of(args[1]));
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
            throw new AssertionError("Factory cache retained its target class or loader");
        }

        private static Object construct(Class<?> type) {
            try {
                return type.getConstructor().newInstance();
            } catch (Exception e) {
                throw new AssertionError(e);
            }
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        private static WeakReference<?>[] exercise(int mode, Path classes) throws Exception {
            final boolean supplier = (mode & 1) != 0;
            final boolean map = (mode & 2) != 0;
            final boolean registered = (mode & 4) != 0;
            try (final var loader = new URLClassLoader(new URL[] { classes.toUri().toURL() }, LoaderProbe.class.getClassLoader())) {
                final Class type = loader.loadClass("disposable.Factories$" + (map ? "Entries" : "Values"));
                if (registered) {
                    final boolean accepted;
                    if (supplier) {
                        accepted = map ? Suppliers.registerForMap(type, () -> (Map) construct(type))
                                : Suppliers.registerForCollection(type, () -> (Collection) construct(type));
                    } else {
                        accepted = map ? IntFunctions.registerForMap(type, capacity -> (Map) construct(type))
                                : IntFunctions.registerForCollection(type, capacity -> (Collection) construct(type));
                    }
                    if (!accepted) {
                        throw new AssertionError("fresh registration rejected");
                    }
                }
                final Object factory = supplier ? (map ? Suppliers.ofMap(type) : Suppliers.ofCollection(type))
                        : (map ? IntFunctions.ofMap(type) : IntFunctions.ofCollection(type));
                final Object second = supplier ? (map ? Suppliers.ofMap(type) : Suppliers.ofCollection(type))
                        : (map ? IntFunctions.ofMap(type) : IntFunctions.ofCollection(type));
                if (factory != second || type.getField("calls").getInt(null) != 0) {
                    throw new AssertionError("lookup identity or eager construction");
                }
                final Object value = supplier ? ((java.util.function.Supplier) factory).get() : ((java.util.function.IntFunction) factory).apply(4);
                if (!type.isInstance(value)) {
                    throw new AssertionError("factory returned wrong type");
                }
                return new WeakReference<?>[] { new WeakReference<>(type), new WeakReference<>(loader) };
            }
        }
    }
}
