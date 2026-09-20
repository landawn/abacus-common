package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.WeakReference;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

import javax.tools.ToolProvider;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.io.TempDir;

import testfixtures.UtilExceptionSources.Cause;

import com.landawn.abacus.TestBase;

@org.junit.jupiter.api.Tag("unit")
public class ExceptionCacheLifetimeTest extends TestBase {
    @TempDir
    Path directory;

    @Test
    @Timeout(120)
    void derivedAndRememberedWrapperEntriesDoNotRetainDisposableLoaders() throws Exception {
        final Path source = directory.resolve("Failures.java");
        Files.writeString(source, """
                package disposable;
                public class Failures {
                    public static class Checked extends Exception { }
                    public static class IO extends java.io.IOException { }
                    public static class Runtime extends RuntimeException {
                        public Runtime() { }
                        public Runtime(Throwable cause) { super(cause); }
                    }
                }
                """);
        assertEquals(0, ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", directory.toString(), source.toString()));
        final String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
        for (final String mode : new String[] { "Checked", "IO", "Runtime", "wrapper" }) {
            final Path log = directory.resolve(mode + ".log");
            final Process process = new ProcessBuilder(Path.of(System.getProperty("java.home"), "bin", "java").toString(), "-Xmx96m",
                    "-XX:ActiveProcessorCount=2", "-XX:+ClassUnloading", "-cp", classpath, LoaderProbe.class.getName(), mode, directory.toString())
                            .redirectErrorStream(true)
                            .redirectOutput(log.toFile())
                            .start();
            try {
                assertTrue(process.waitFor(20, TimeUnit.SECONDS), mode);
                assertEquals(0, process.exitValue(), Files.readString(log));
            } finally {
                process.destroyForcibly();
            }
        }
    }

    public static class LoaderProbe {
        public static void main(String[] args) throws Exception {
            final WeakReference<?>[] references = exercise(args[0], Path.of(args[1]));
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
            throw new AssertionError("Exception conversion retained a disposable class or loader");
        }

        private static WeakReference<?>[] exercise(final String mode, final Path classes) throws Exception {
            try (final var loader = new URLClassLoader(new URL[] { classes.toUri().toURL() }, LoaderProbe.class.getClassLoader())) {
                final Class<?> type = loader.loadClass("disposable.Failures$" + (mode.equals("wrapper") ? "Runtime" : mode));
                if (mode.equals("wrapper")) {
                    final WeakReference<Class<?>> weak = new WeakReference<>(type);
                    ExceptionUtil.registerRuntimeExceptionMapper(Cause.class, cause -> {
                        try {
                            return (RuntimeException) weak.get().getConstructor(Throwable.class).newInstance(cause);
                        } catch (Exception e) {
                            throw new AssertionError(e);
                        }
                    });
                    final Cause cause = new Cause();
                    final var wrapper = (RuntimeException) type.getConstructor(Throwable.class).newInstance(cause);
                    if (ExceptionUtil.tryToGetOriginalCheckedException(wrapper) != cause) {
                        throw new AssertionError("wrapper mismatch");
                    }
                } else {
                    final Throwable original = (Throwable) type.getConstructor().newInstance();
                    final RuntimeException converted = ExceptionUtil.toRuntimeException(original);
                    if (original instanceof RuntimeException ? converted != original : converted.getCause() != original) {
                        throw new AssertionError("conversion mismatch");
                    }
                }
                return new WeakReference<?>[] { new WeakReference<>(type), new WeakReference<>(loader) };
            }
        }
    }
}
