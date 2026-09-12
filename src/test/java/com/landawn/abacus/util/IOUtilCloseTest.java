package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.UncheckedIOException;

public class IOUtilCloseTest extends IOUtilTestSupport {
    @Test
    public void testCloseOperationsPreserveInterrupt() {
        org.junit.jupiter.api.Assertions.assertAll(() -> {
            try {
                IOUtil.closeQuietly(() -> {
                    throw new InterruptedException();
                });
                assertTrue(Thread.currentThread().isInterrupted());
            } finally {
                Thread.interrupted();
            }
        }, () -> {
            final IOException firstFailure = new IOException("first");
            final InterruptedException interruption = new InterruptedException("second");
            final AtomicInteger closed = new AtomicInteger();

            try {
                final UncheckedIOException failure = assertThrows(UncheckedIOException.class, () -> IOUtil.closeAll(Arrays.asList(() -> {
                    throw firstFailure;
                }, () -> {
                    throw interruption;
                }, () -> closed.incrementAndGet())));

                assertEquals(firstFailure, failure.getCause());
                assertArrayEquals(new Throwable[] { interruption }, firstFailure.getSuppressed());
                assertEquals(1, closed.get());
                assertTrue(Thread.currentThread().isInterrupted());
            } finally {
                Thread.interrupted();
            }
        });
    }

    @Test
    public void testClose_NullURLConnection() {
        assertDoesNotThrow(() -> {
            IOUtil.close((java.net.URLConnection) null);
        });
    }

    @Test
    public void testClose_NullAutoCloseable() {
        assertDoesNotThrow(() -> {
            IOUtil.close((AutoCloseable) null);
        });
    }

    @Test
    public void testClose_URLConnection() throws Exception {
        assertDoesNotThrow(() -> {
            File testFile = Files.createTempFile(tempFolder, "urltest", ".txt").toFile();
            Files.write(testFile.toPath(), TEST_CONTENT.getBytes(UTF_8));

            java.net.URL url = testFile.toURI().toURL();
            java.net.URLConnection conn = url.openConnection();
            conn.connect();

            try (InputStream in = conn.getInputStream()) {
                in.readAllBytes(); // simulate use
            }

            IOUtil.close(conn);
        });
    }

    @Test
    public void testClose_AutoCloseable() throws Exception {
        InputStream is = IOUtil.newBufferedInputStream(new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8)));
        IOUtil.close(is);

        assertThrows(IOException.class, () -> is.read());
    }

    @Test
    public void testClose_WithExceptionHandler() throws Exception {
        InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8));
        java.util.concurrent.atomic.AtomicBoolean handlerCalled = new java.util.concurrent.atomic.AtomicBoolean(false);

        IOUtil.close(is, ex -> handlerCalled.set(true));

        assertTrue(!handlerCalled.get());
    }

    @Test
    public void testClose_WithExceptionHandlerOnError() {
        AutoCloseable problematic = () -> {
            throw new IOException("Test exception");
        };

        java.util.concurrent.atomic.AtomicBoolean handlerCalled = new java.util.concurrent.atomic.AtomicBoolean(false);
        java.util.concurrent.atomic.AtomicReference<Exception> caughtException = new java.util.concurrent.atomic.AtomicReference<>();

        IOUtil.close(problematic, ex -> {
            handlerCalled.set(true);
            caughtException.set(ex);
        });

        assertTrue(handlerCalled.get());
        assertNotNull(caughtException.get());
        assertTrue(caughtException.get().getMessage().contains("Test exception"));
    }

    @Test
    public void testCloseAll_EmptyVarArgs() {
        assertDoesNotThrow(() -> {
            IOUtil.closeAll();
        });
    }

    @Test
    public void testCloseAll_EmptyIterable() {
        assertDoesNotThrow(() -> {
            java.util.List<AutoCloseable> closeables = new java.util.ArrayList<>();
            IOUtil.closeAll(closeables);
        });
    }

    @Test
    public void testCloseAll_VarArgs() throws Exception {
        InputStream is1 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data1".getBytes(UTF_8)));
        InputStream is2 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data2".getBytes(UTF_8)));
        InputStream is3 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data3".getBytes(UTF_8)));

        IOUtil.closeAll(is1, is2, is3);

        assertThrows(IOException.class, () -> is1.read());
        assertThrows(IOException.class, () -> is2.read());
        assertThrows(IOException.class, () -> is3.read());
    }

    @Test
    public void testCloseAll_WithNulls() throws Exception {
        InputStream is1 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data1".getBytes(UTF_8)));
        InputStream is2 = null;
        InputStream is3 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data3".getBytes(UTF_8)));

        IOUtil.closeAll(is1, is2, is3);

        assertThrows(IOException.class, () -> is1.read());
        assertThrows(IOException.class, () -> is3.read());
    }

    @Test
    public void testCloseAll_Iterable() throws Exception {
        java.util.List<AutoCloseable> closeables = new java.util.ArrayList<>();
        closeables.add(IOUtil.newBufferedInputStream(new ByteArrayInputStream("data1".getBytes(UTF_8))));
        closeables.add(IOUtil.newBufferedInputStream(new ByteArrayInputStream("data2".getBytes(UTF_8))));

        closeables.add(IOUtil.newBufferedInputStream(new ByteArrayInputStream("data3".getBytes(UTF_8))));

        IOUtil.closeAll(closeables);

        for (AutoCloseable c : closeables) {
            InputStream is = (InputStream) c;
            assertThrows(IOException.class, () -> is.read());
        }
    }

    @Test
    public void testCloseAll_IterableWithNulls() throws Exception {
        java.util.List<AutoCloseable> closeables = new java.util.ArrayList<>();
        closeables.add(IOUtil.newBufferedInputStream(new ByteArrayInputStream("data1".getBytes(UTF_8))));

        closeables.add(null);
        closeables.add(IOUtil.newBufferedInputStream(new ByteArrayInputStream("data3".getBytes(UTF_8))));

        IOUtil.closeAll(closeables);

        InputStream is1 = (InputStream) closeables.get(0);
        InputStream is3 = (InputStream) closeables.get(2);
        assertThrows(IOException.class, () -> is1.read());
        assertThrows(IOException.class, () -> is3.read());
    }

    @Test
    public void testCloseAll_ContinuesOnException() {
        AutoCloseable problematic = () -> {
            throw new IOException("Error in first closeable");
        };
        InputStream is = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data".getBytes(UTF_8)));

        assertThrows(UncheckedIOException.class, () -> IOUtil.closeAll(problematic, is));

        assertThrows(IOException.class, () -> is.read());
    }

    @Test
    public void testCloseQuietly_Null() {
        assertDoesNotThrow(() -> {
            IOUtil.closeQuietly((AutoCloseable) null);
        });
    }

    @Test
    public void testCloseQuietly_AutoCloseable() throws Exception {
        InputStream is = IOUtil.newBufferedInputStream(new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8)));
        IOUtil.closeQuietly(is);

        assertThrows(IOException.class, () -> is.read());
    }

    @Test
    public void testCloseQuietly_WithException() {
        assertDoesNotThrow(() -> {
            AutoCloseable problematic = () -> {
                throw new IOException("Test exception");
            };

            IOUtil.closeQuietly(problematic);
        });
    }

    @Test
    public void testCloseQuietly_MultipleResources() throws Exception {
        InputStream is1 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data1".getBytes(UTF_8)));
        InputStream is2 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data2".getBytes(UTF_8)));

        IOUtil.closeQuietly(is1);
        IOUtil.closeQuietly(is2);

        assertThrows(IOException.class, () -> is1.read());
        assertThrows(IOException.class, () -> is2.read());
    }

    @Test
    public void testCloseAllQuietly_EmptyVarArgs() {
        assertDoesNotThrow(() -> {
            IOUtil.closeAllQuietly();
        });
    }

    @Test
    public void testCloseAllQuietly_EmptyIterable() {
        assertDoesNotThrow(() -> {
            java.util.List<AutoCloseable> closeables = new java.util.ArrayList<>();
            IOUtil.closeAllQuietly(closeables);
        });
    }

    @Test
    public void testCloseAllQuietly_NullIterable() {
        assertDoesNotThrow(() -> {
            IOUtil.closeAllQuietly((Iterable<? extends AutoCloseable>) null);
        });
    }

    @Test
    public void testCloseAllQuietly_VarArgs() throws Exception {
        InputStream is1 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data1".getBytes(UTF_8)));
        InputStream is2 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data2".getBytes(UTF_8)));
        InputStream is3 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data3".getBytes(UTF_8)));

        IOUtil.closeAllQuietly(is1, is2, is3);

        assertThrows(IOException.class, () -> is1.read());
        assertThrows(IOException.class, () -> is2.read());
        assertThrows(IOException.class, () -> is3.read());
    }

    @Test
    public void testCloseAllQuietly_WithNulls() throws Exception {
        InputStream is1 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data1".getBytes(UTF_8)));
        InputStream is2 = null;
        InputStream is3 = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data3".getBytes(UTF_8)));

        IOUtil.closeAllQuietly(is1, is2, is3);

        assertThrows(IOException.class, () -> is1.read());
        assertThrows(IOException.class, () -> is3.read());
    }

    @Test
    public void testCloseAllQuietly_Iterable() throws Exception {
        java.util.List<AutoCloseable> closeables = new java.util.ArrayList<>();
        closeables.add(IOUtil.newBufferedReader(new ByteArrayInputStream("data1".getBytes(UTF_8))));
        closeables.add(IOUtil.newBufferedReader(new ByteArrayInputStream("data2".getBytes(UTF_8))));
        closeables.add(IOUtil.newBufferedReader(new ByteArrayInputStream("data3".getBytes(UTF_8))));

        IOUtil.closeAllQuietly(closeables);

        for (AutoCloseable c : closeables) {
            Reader is = (Reader) c;
            assertThrows(IOException.class, () -> is.read());
        }
    }

    @Test
    public void testCloseAllQuietly_IterableWithNulls() throws Exception {
        java.util.List<AutoCloseable> closeables = new java.util.ArrayList<>();
        closeables.add(IOUtil.newBufferedInputStream(new ByteArrayInputStream("data1".getBytes(UTF_8))));

        closeables.add(null);
        closeables.add(IOUtil.newBufferedInputStream(new ByteArrayInputStream("data3".getBytes(UTF_8))));

        IOUtil.closeAllQuietly(closeables);

        InputStream is1 = (InputStream) closeables.get(0);
        InputStream is3 = (InputStream) closeables.get(2);
        assertThrows(IOException.class, () -> is1.read());
        assertThrows(IOException.class, () -> is3.read());
    }

    @Test
    public void testCloseAllQuietly_WithExceptions() {
        AutoCloseable problematic1 = () -> {
            throw new IOException("Error 1");
        };
        AutoCloseable problematic2 = () -> {
            throw new IOException("Error 2");
        };
        InputStream is = IOUtil.newBufferedInputStream(new ByteArrayInputStream("data".getBytes(UTF_8)));

        IOUtil.closeAllQuietly(problematic1, problematic2, is);

        assertThrows(IOException.class, () -> is.read());
    }

    @Test
    public void testCloseAllQuietly_MixedTypes() throws Exception {
        FileInputStream fis = new FileInputStream(tempFile);
        java.io.BufferedReader br = IOUtil.newBufferedReader(tempFile);
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();

        IOUtil.closeAllQuietly(fis, br, baos);

        assertThrows(IOException.class, () -> fis.read());
        assertThrows(IOException.class, () -> br.read());
    }
}
