package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@org.junit.jupiter.api.Tag("unit")
public class CodegenTemporaryFileTest {
    @TempDir
    Path directory;

    public static class Entity {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(String value) {
            name = value;
        }
    }

    private static void write(final File file, final String source) throws Exception {
        final Method method = CodeGenerationUtil.class.getDeclaredMethod("writeSourceAtomically", String.class, File.class);
        method.setAccessible(true);
        method.invoke(null, source, file);
    }

    private void assertNoOwnedTemporaryFiles() throws Exception {
        try (final var files = Files.walk(directory)) {
            assertTrue(files.noneMatch(path -> path.getFileName().toString().startsWith(".codegen-")));
        }
    }

    @Test
    void publicGenerationPreservesAnExistingPredictableTemporaryName() throws Exception {
        final String name = ClassUtil.getSimpleClassName(Entity.class);
        final Path packageDirectory = directory.resolve(ClassUtil.getPackageName(Entity.class).replace('.', '/'));
        Files.createDirectories(packageDirectory);
        final Path source = packageDirectory.resolve(name + ".java");
        final Path sentinel = packageDirectory.resolve(name + ".java.codegen.tmp");
        Files.writeString(source, "public class " + name + " {\n}\n");
        Files.writeString(sentinel, "unrelated\uD83D\uDE00");
        CodeGenerationUtil.generatePropNameTableClass(Entity.class, "Props", directory.toString());
        assertTrue(Files.readString(source).contains("public interface Props"));
        assertEquals("unrelated\uD83D\uDE00", Files.readString(sentinel));
        assertNoOwnedTemporaryFiles();
    }

    @Test
    void concurrentWritesUseIndependentStagingAndFailuresCleanUp() throws Exception {
        final Path target = directory.resolve("target.java");
        final CountDownLatch staged = new CountDownLatch(2);
        final CountDownLatch releaseFirst = new CountDownLatch(1);
        final CountDownLatch releaseSecond = new CountDownLatch(1);
        try (final var executor = Executors.newFixedThreadPool(2)) {
            final var first = executor.submit(() -> {
                write(gatedTarget(target, staged, releaseFirst), "first\r\n甲🙂\r\n");
                return null;
            });
            final var second = executor.submit(() -> {
                write(gatedTarget(target, staged, releaseSecond), "second\n乙🙂\n");
                return null;
            });
            try {
                awaitStaging(staged, first, second);
                try (final var files = Files.list(directory)) {
                    assertEquals(2, files.filter(path -> path.getFileName().toString().startsWith(".codegen-")).count());
                }
                // Overlap staging, then publish separately: Windows need not support simultaneous replacement moves.
                releaseFirst.countDown();
                first.get(10, TimeUnit.SECONDS);
                assertEquals("first\r\n甲🙂\r\n", Files.readString(target));
                releaseSecond.countDown();
                second.get(10, TimeUnit.SECONDS);
            } finally {
                releaseFirst.countDown();
                releaseSecond.countDown();
            }
        }
        assertEquals("second\n乙🙂\n", Files.readString(target));
        final Path nonemptyDirectory = directory.resolve("occupied");
        Files.createDirectories(nonemptyDirectory);
        Files.writeString(nonemptyDirectory.resolve("keep"), "sentinel");
        assertThrows(InvocationTargetException.class, () -> write(nonemptyDirectory.toFile(), "replacement"));
        assertEquals("sentinel", Files.readString(nonemptyDirectory.resolve("keep")));
        assertNoOwnedTemporaryFiles();
    }

    private static void awaitStaging(final CountDownLatch staged, final Future<?> first, final Future<?> second) throws Exception {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!staged.await(20, TimeUnit.MILLISECONDS)) {
            // Surface reflection or worker failures promptly instead of hiding them behind a latch timeout.
            if (first.isDone()) first.get();
            if (second.isDone()) second.get();
            assertTrue(System.nanoTime() < deadline, "Writers did not reach the publication gate");
        }
    }

    private static File gatedTarget(final Path target, final CountDownLatch staged, final CountDownLatch release) {
        return new File(target.toString()) {
            private int pathLookups;

            @Override
            public Path toPath() {
                // The first lookup locates the staging directory; the second is the move destination,
                // after the complete String has been written and the staging file has been closed.
                if (++pathLookups == 2) {
                    staged.countDown();
                    try {
                        assertTrue(release.await(15, TimeUnit.SECONDS));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AssertionError(e);
                    }
                }
                return super.toPath();
            }
        };
    }
}
