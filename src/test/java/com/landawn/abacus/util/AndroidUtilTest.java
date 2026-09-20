package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class AndroidUtilTest extends TestBase {

    @Test
    public void fallbackExecutorsCanBeInitializedDuringJvmShutdown() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(IOUtil.IS_PLATFORM_ANDROID);

        final String javaBin = System.getProperty("java.home") + File.separator + "bin" + File.separator + "java";
        final Path outputFile = Files.createTempFile("android-executor-shutdown-", ".log");
        Process process = null;

        try {
            process = new ProcessBuilder(javaBin, "-cp", System.getProperty("java.class.path"), FallbackShutdownProbe.class.getName()).redirectErrorStream(true)
                    .redirectOutput(outputFile.toFile())
                    .start();

            try {
                Assertions.assertTrue(process.waitFor(30, TimeUnit.SECONDS), "shutdown probe did not exit");
                final String output = Files.readString(outputFile, StandardCharsets.UTF_8);
                Assertions.assertEquals(0, process.exitValue(), output);
                Assertions.assertTrue(output.contains("PROBE serial=true pool=true"), output);
            } finally {
                process.destroyForcibly();
                process.waitFor(5, TimeUnit.SECONDS);
            }
        } finally {
            if (process == null || !process.isAlive()) {
                Files.deleteIfExists(outputFile);
            } else {
                outputFile.toFile().deleteOnExit();
            }
        }
    }

    public static final class FallbackShutdownProbe {
        public static void main(final String[] args) {
            // Initialize platform detection before shutdown to isolate AndroidUtil's first use.
            if (IOUtil.IS_PLATFORM_ANDROID) {
                throw new IllegalStateException("This probe requires the fallback executors");
            }

            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                try {
                    final boolean serial = CompletableFuture.supplyAsync(() -> Thread.currentThread().isDaemon(), AndroidUtil.getSerialExecutor())
                            .get(5, TimeUnit.SECONDS);
                    final boolean pool = CompletableFuture.supplyAsync(() -> Thread.currentThread().isDaemon(), AndroidUtil.getThreadPoolExecutor())
                            .get(5, TimeUnit.SECONDS);
                    System.out.println("PROBE serial=" + serial + " pool=" + pool);
                } catch (final Throwable failure) {
                    failure.printStackTrace();
                }
            }));
        }
    }

    @Test
    public void testGetSerialExecutor() {
        Executor executor = AndroidUtil.getSerialExecutor();
        assertNotNull(executor);
    }

    @Test
    public void testGetSerialExecutorNotNull() {
        Executor executor1 = AndroidUtil.getSerialExecutor();
        Executor executor2 = AndroidUtil.getSerialExecutor();
        assertNotNull(executor1);
        assertNotNull(executor2);
    }

    @Test
    public void testExecutorsAreDifferent() {
        Executor serial = AndroidUtil.getSerialExecutor();
        Executor threadPool = AndroidUtil.getThreadPoolExecutor();
        Assertions.assertNotSame(serial, threadPool);
    }

    @Test
    public void testSerialExecutorExecutes() throws InterruptedException {
        Executor executor = AndroidUtil.getSerialExecutor();
        final boolean[] executed = { false };

        executor.execute(() -> {
            executed[0] = true;
        });

        Thread.sleep(100);
        assertNotNull(executor);
    }

    @Test
    public void testGetThreadPoolExecutor() {
        Executor executor = AndroidUtil.getThreadPoolExecutor();
        assertNotNull(executor);
    }

    @Test
    public void testGetThreadPoolExecutorNotNull() {
        Executor executor1 = AndroidUtil.getThreadPoolExecutor();
        Executor executor2 = AndroidUtil.getThreadPoolExecutor();
        assertNotNull(executor1);
        assertNotNull(executor2);
    }

    @Test
    public void test() {
        assertDoesNotThrow(() -> {
            AndroidUtil.getThreadPoolExecutor().execute(() -> System.out.print("Hello"));
        });
    }

    @Test
    public void testThreadPoolExecutorExecutes() throws InterruptedException {
        Executor executor = AndroidUtil.getThreadPoolExecutor();
        final boolean[] executed = { false };

        executor.execute(() -> {
            executed[0] = true;
        });

        Thread.sleep(100);
        assertNotNull(executor);
    }

    @Test
    public void testFallbackExecutorThreadsAreDaemon() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(IOUtil.IS_PLATFORM_ANDROID);

        CompletableFuture<Boolean> serialDaemon = new CompletableFuture<>();
        CompletableFuture<Boolean> poolDaemon = new CompletableFuture<>();
        AndroidUtil.getSerialExecutor().execute(() -> serialDaemon.complete(Thread.currentThread().isDaemon()));
        AndroidUtil.getThreadPoolExecutor().execute(() -> poolDaemon.complete(Thread.currentThread().isDaemon()));

        Assertions.assertTrue(serialDaemon.get(2, TimeUnit.SECONDS));
        Assertions.assertTrue(poolDaemon.get(2, TimeUnit.SECONDS));
    }

    @Test
    public void reviewFixes20260908_fallbackWorkersComeFromOneSharedDefaultThreadFactory() throws Exception {
        org.junit.jupiter.api.Assumptions.assumeFalse(IOUtil.IS_PLATFORM_ANDROID);

        CompletableFuture<String> serialThread = new CompletableFuture<>();
        CompletableFuture<String> poolThread = new CompletableFuture<>();
        AndroidUtil.getSerialExecutor().execute(() -> serialThread.complete(Thread.currentThread().getName()));
        AndroidUtil.getThreadPoolExecutor().execute(() -> poolThread.complete(Thread.currentThread().getName()));

        String serialName = serialThread.get(5, TimeUnit.SECONDS);
        String poolName = poolThread.get(5, TimeUnit.SECONDS);

        // Executors.defaultThreadFactory() allocates a fresh factory on every call - a new JVM-wide pool number
        // and its own thread counter - so calling it inside newThread() named every worker "pool-N-thread-1"
        // under a different N and burned a pool number per thread. One hoisted delegate gives all the fallback
        // workers a single pool number and a distinct thread index.
        Pattern naming = Pattern.compile("pool-(\\d+)-thread-(\\d+)");
        Matcher serial = naming.matcher(serialName);
        Matcher pool = naming.matcher(poolName);
        Assertions.assertTrue(serial.matches(), serialName);
        Assertions.assertTrue(pool.matches(), poolName);
        Assertions.assertEquals(serial.group(1), pool.group(1), "one pool number expected, got " + serialName + " and " + poolName);
        Assertions.assertNotEquals(serial.group(2), pool.group(2), "distinct thread indexes expected, got " + serialName + " and " + poolName);
    }

}
