package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.Writer;
import java.nio.MappedByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.exception.UncheckedIOException;

public class IOUtilTest extends IOUtilTestSupport {

    @Test
    public void testLineIterationProcessingWorkersReadSingleReader() throws Exception {
        Thread caller = Thread.currentThread();

        for (int processThreads : new int[] { 0, 2 }) {
            java.util.Set<Thread> readers = java.util.concurrent.ConcurrentHashMap.newKeySet();
            java.util.concurrent.atomic.AtomicInteger processed = new java.util.concurrent.atomic.AtomicInteger();
            Reader source = new java.io.StringReader("one\ntwo\nthree\nfour\n") {
                @Override
                public int read(char[] chars, int offset, int count) throws IOException {
                    readers.add(Thread.currentThread());
                    return super.read(chars, offset, count);
                }
            };

            IOUtil.forEachLine(source, IOUtil.LineIterationOptions.builder().readThreads(0).processThreads(processThreads).build(),
                    line -> processed.incrementAndGet());

            assertEquals(4, processed.get());
            assertFalse(readers.isEmpty());
            assertEquals(processThreads == 0, readers.contains(caller));
            if (processThreads == 0) {
                assertEquals(java.util.Set.of(caller), readers);
            }
        }
    }

    @Test
    public void testRepeatedCharsetWritesUseSeparateEncodingSessions() throws IOException {
        java.io.ByteArrayOutputStream encoded = new java.io.ByteArrayOutputStream();
        IOUtil.write(new char[] { 'A' }, StandardCharsets.UTF_16, encoded);
        IOUtil.write(new char[] { 'B' }, StandardCharsets.UTF_16, encoded);
        assertEquals("A\uFEFFB", encoded.toString(StandardCharsets.UTF_16));

        encoded.reset();
        try (Writer writer = IOUtil.newOutputStreamWriter(encoded, StandardCharsets.UTF_16)) {
            IOUtil.write(new char[] { 'A' }, writer);
            IOUtil.write(new char[] { 'B' }, writer);
        }
        assertEquals("AB", encoded.toString(StandardCharsets.UTF_16));

        if (Charset.isSupported("UTF-32")) {
            Charset utf32 = Charset.forName("UTF-32");
            encoded.reset();
            IOUtil.write(new char[] { 'A' }, utf32, encoded);
            IOUtil.write(new char[] { 'B' }, utf32, encoded);
            java.io.ByteArrayOutputStream expected = new java.io.ByteArrayOutputStream();
            expected.write("A".getBytes(utf32));
            expected.write("B".getBytes(utf32));
            assertArrayEquals(expected.toByteArray(), encoded.toByteArray());
        }
    }

    @Test
    public void testWriteLinesPreservesPrimaryFailureWhenDrainingFails() {
        for (boolean iterable : new boolean[] { false, true }) {
            for (boolean sameFailure : new boolean[] { false, true }) {
                RuntimeException primary = new IllegalStateException("iteration failure");
                RuntimeException secondary = sameFailure ? primary : new IllegalStateException("writer failure");
                java.util.concurrent.atomic.AtomicInteger writeAttempts = new java.util.concurrent.atomic.AtomicInteger();
                Iterator<String> lines = new Iterator<>() {
                    boolean emitted;

                    @Override
                    public boolean hasNext() {
                        if (emitted) {
                            throw primary;
                        }
                        return true;
                    }

                    @Override
                    public String next() {
                        emitted = true;
                        return "one";
                    }
                };
                Writer writer = new Writer() {
                    @Override
                    public void write(char[] chars, int offset, int count) {
                        writeAttempts.incrementAndGet();
                        throw secondary;
                    }

                    @Override
                    public void flush() {
                    }

                    @Override
                    public void close() {
                    }
                };

                RuntimeException actual = assertThrows(RuntimeException.class, () -> {
                    if (iterable) {
                        IOUtil.writeLines((Iterable<String>) () -> lines, writer, false);
                    } else {
                        IOUtil.writeLines(lines, writer, false);
                    }
                });
                org.junit.jupiter.api.Assertions.assertSame(primary, actual);
                assertEquals(1, writeAttempts.get());
                assertArrayEquals(sameFailure ? new Throwable[0] : new Throwable[] { secondary }, actual.getSuppressed());
            }
        }
    }

    @Test
    public void testSameFileGuardsRejectHardLinkAliases() throws IOException {
        final byte[] original = "same-file-content".getBytes(UTF_8);

        Path writeSource = tempFolder.resolve("write-source.txt");
        Path writeAlias = tempFolder.resolve("write-alias.txt");
        Files.write(writeSource, original);
        Files.createLink(writeAlias, writeSource);
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write(writeSource.toFile(), 0, original.length, writeAlias.toFile()));
        assertArrayEquals(original, Files.readAllBytes(writeSource));

        Path appendSource = tempFolder.resolve("append-source.txt");
        Path appendAlias = tempFolder.resolve("append-alias.txt");
        Files.write(appendSource, original);
        Files.createLink(appendAlias, appendSource);
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append(appendSource.toFile(), 0, 1, appendAlias.toFile()));
        assertArrayEquals(original, Files.readAllBytes(appendSource));

        Path mergeSource = tempFolder.resolve("merge-source.txt");
        Path mergeAlias = tempFolder.resolve("merge-alias.txt");
        Files.write(mergeSource, original);
        Files.createLink(mergeAlias, mergeSource);
        assertThrows(IllegalArgumentException.class, () -> IOUtil.merge(java.util.Collections.singletonList(mergeSource.toFile()), null, mergeAlias.toFile()));
        assertArrayEquals(original, Files.readAllBytes(mergeSource));
    }

    @Test
    public void testZipRejectsSameFileBeforeOpeningTarget() throws IOException {
        final byte[] original = "zip-source-content".getBytes(UTF_8);
        Path source = tempFolder.resolve("zip-source.txt");
        Files.write(source, original);

        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(source.toFile(), source.toFile()));
        assertArrayEquals(original, Files.readAllBytes(source));

        Path aliasSource = tempFolder.resolve("zip-alias-source.txt");
        Path alias = tempFolder.resolve("zip-alias-target.txt");
        Files.write(aliasSource, original);
        Files.createLink(alias, aliasSource);
        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(aliasSource.toFile(), alias.toFile()));
        assertArrayEquals(original, Files.readAllBytes(aliasSource));
    }

    @Test
    public void testMissingFileIsNeverNewer() throws IOException {
        File missing = tempFolder.resolve("missing-file.txt").toFile();
        File existingReference = Files.createFile(tempFolder.resolve("before-epoch-reference.txt")).toFile();
        File beforeEpochReference = new File(existingReference.getPath()) {
            private static final long serialVersionUID = 1L;

            @Override
            public long lastModified() {
                return -1;
            }
        };

        // A file that does not exist is neither newer nor older, even against a pre-epoch reference time.
        assertFalse(IOUtil.isFileNewer(missing, new java.util.Date(-1)));
        assertFalse(IOUtil.isFileOlder(missing, new java.util.Date(-1)));
        assertFalse(IOUtil.isFileNewer(missing, beforeEpochReference));
        assertFalse(IOUtil.isFileOlder(missing, beforeEpochReference));
    }

    @Test
    public void testMissingReferenceIsRejected() throws IOException {
        File existing = Files.createFile(tempFolder.resolve("reference-subject.txt")).toFile();
        File missingReference = tempFolder.resolve("no-such-reference.txt").toFile();

        // File.lastModified() reports 0 for an absent file, so without a guard every existing file would silently
        // test as "newer than" a reference that was never created. Rejected as a bad argument instead, matching
        // Apache Commons-IO.
        assertThrows(IllegalArgumentException.class, () -> IOUtil.isFileNewer(existing, missingReference));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.isFileOlder(existing, missingReference));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.isFileNewer(existing, (File) null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.isFileOlder(existing, (File) null));

        // A Date reference has no existence to check, so it is unaffected.
        assertFalse(IOUtil.isFileNewer(existing, new java.util.Date(Long.MAX_VALUE)));
    }

    @Test
    public void testEmptyFileCollectionsInvokeCompletion() throws Exception {
        java.util.concurrent.atomic.AtomicInteger completions = new java.util.concurrent.atomic.AtomicInteger();

        IOUtil.forEachLine(java.util.Collections.emptyList(),
                IOUtil.LineIterationOptions.builder().offset(0).count(Long.MAX_VALUE).processThreads(0).queueSize(1).build(), line -> {
                }, completions::incrementAndGet);
        IOUtil.forEachLine(java.util.Collections.emptyList(),
                IOUtil.LineIterationOptions.builder().offset(0).count(Long.MAX_VALUE).readThreads(1).processThreads(0).queueSize(1).build(), line -> {
                }, completions::incrementAndGet);

        assertEquals(2, completions.get());
    }

    @Test
    public void testFreeDiskSpaceKb_Default() {
        long freeSpace = IOUtil.freeDiskSpaceInKB();

        assertTrue(freeSpace > 0);
    }

    @Test
    public void testFreeDiskSpaceKb_WithTimeout() {
        long freeSpace = IOUtil.freeDiskSpaceInKB(5000);

        assertTrue(freeSpace > 0);
    }

    @Test
    public void testRoundTrip_CharsToBytes() {
        char[] original = UNICODE_CONTENT.toCharArray();
        byte[] bytes = IOUtil.charsToBytes(original, UTF_8);
        char[] result = IOUtil.bytesToChars(bytes, UTF_8);
        assertArrayEquals(original, result);
    }

    // ===== charsToBytes with offset/count =====

    @Test
    public void testCharsToBytes_WithOffsetAndCount() {
        char[] chars = "Hello World".toCharArray();
        byte[] result = IOUtil.charsToBytes(chars, 6, 5, UTF_8);
        assertEquals("World", new String(result, UTF_8));
    }

    @Test
    public void testCharsToBytes_WithDifferentCharset() {
        char[] chars = "Hello".toCharArray();
        byte[] utf8Result = IOUtil.charsToBytes(chars, 0, 5, UTF_8);
        byte[] isoResult = IOUtil.charsToBytes(chars, 0, 5, ISO_8859_1);
        // Both should produce 5 bytes for ASCII chars
        assertEquals(5, utf8Result.length);
        assertEquals(5, isoResult.length);
    }

    @Test
    public void testChars2Bytes_Default() {
        char[] chars = TEST_CONTENT.toCharArray();
        byte[] bytes = IOUtil.charsToBytes(chars);
        assertNotNull(bytes);
        assertEquals(TEST_CONTENT, new String(bytes, UTF_8));
    }

    @Test
    public void testChars2Bytes_WithCharset() {
        char[] chars = TEST_CONTENT.toCharArray();
        byte[] bytes = IOUtil.charsToBytes(chars, UTF_8);
        assertNotNull(bytes);
        assertEquals(TEST_CONTENT, new String(bytes, UTF_8));
    }

    @Test
    public void testChars2Bytes_WithCharsetUTF16() {
        char[] chars = TEST_CONTENT.toCharArray();
        byte[] bytes = IOUtil.charsToBytes(chars, UTF_16);
        assertNotNull(bytes);
        assertEquals(TEST_CONTENT, new String(bytes, UTF_16));
    }

    @Test
    public void testChars2Bytes_WithOffsetAndLength() {
        char[] chars = "0123456789".toCharArray();
        byte[] bytes = IOUtil.charsToBytes(chars, 2, 5, UTF_8);
        assertNotNull(bytes);
        assertEquals("23456", new String(bytes, UTF_8));
    }

    @Test
    public void testChars2Bytes_EmptyArray() {
        char[] chars = new char[0];
        byte[] bytes = IOUtil.charsToBytes(chars);
        assertNotNull(bytes);
        assertEquals(0, bytes.length);
    }

    @Test
    public void testChars2Bytes_UnicodeChars() {
        char[] chars = UNICODE_CONTENT.toCharArray();
        byte[] bytes = IOUtil.charsToBytes(chars, UTF_8);
        assertNotNull(bytes);
        assertEquals(UNICODE_CONTENT, new String(bytes, UTF_8));
    }

    @Test
    public void testChars2Bytes_ZeroLength() {
        char[] chars = "0123456789".toCharArray();
        byte[] bytes = IOUtil.charsToBytes(chars, 5, 0, UTF_8);
        assertNotNull(bytes);
        assertEquals(0, bytes.length);
    }

    @Test
    public void testChars2Bytes_NullArray() {
        assertArrayEquals(new byte[] {}, IOUtil.charsToBytes(null));
    }

    @Test
    public void testCharsToBytes_WithZeroCount() {
        char[] chars = "Hello".toCharArray();
        byte[] result = IOUtil.charsToBytes(chars, 0, 0, UTF_8);
        assertEquals(0, result.length);
    }

    // ===== bytesToChars with offset/count =====

    @Test
    public void testBytesToChars_WithOffsetAndCount() {
        byte[] bytes = "Hello World".getBytes(UTF_8);
        char[] result = IOUtil.bytesToChars(bytes, 6, 5, UTF_8);
        assertEquals("World", new String(result));
    }

    @Test
    public void testBytes2Chars_Default() {
        byte[] bytes = TEST_CONTENT.getBytes(UTF_8);
        char[] chars = IOUtil.bytesToChars(bytes);
        assertNotNull(chars);
        assertEquals(TEST_CONTENT, new String(chars));
    }

    @Test
    public void testBytes2Chars_WithCharset() {
        byte[] bytes = TEST_CONTENT.getBytes(UTF_8);
        char[] chars = IOUtil.bytesToChars(bytes, UTF_8);
        assertNotNull(chars);
        assertEquals(TEST_CONTENT, new String(chars));
    }

    @Test
    public void testBytes2Chars_WithCharsetUTF16() {
        byte[] bytes = TEST_CONTENT.getBytes(UTF_16);
        char[] chars = IOUtil.bytesToChars(bytes, UTF_16);
        assertNotNull(chars);
        assertEquals(TEST_CONTENT, new String(chars));
    }

    @Test
    public void testBytes2Chars_WithOffsetAndLength() {
        byte[] bytes = "0123456789".getBytes(UTF_8);
        char[] chars = IOUtil.bytesToChars(bytes, 2, 5, UTF_8);
        assertNotNull(chars);
        assertEquals("23456", new String(chars));
    }

    @Test
    public void testBytes2Chars_EmptyArray() {
        byte[] bytes = new byte[0];
        char[] chars = IOUtil.bytesToChars(bytes);
        assertNotNull(chars);
        assertEquals(0, chars.length);
    }

    @Test
    public void testBytes2Chars_UnicodeBytes() {
        byte[] bytes = UNICODE_CONTENT.getBytes(UTF_8);
        char[] chars = IOUtil.bytesToChars(bytes, UTF_8);
        assertNotNull(chars);
        assertEquals(UNICODE_CONTENT, new String(chars));
    }

    @Test
    public void testBytes2Chars_ZeroLength() {
        byte[] bytes = "0123456789".getBytes(UTF_8);
        char[] chars = IOUtil.bytesToChars(bytes, 5, 0, UTF_8);
        assertNotNull(chars);
        assertEquals(0, chars.length);
    }

    @Test
    public void testBytes2Chars_NullArray() {
        assertArrayEquals(new char[] {}, IOUtil.bytesToChars(null));
    }

    @Test
    public void testBytesToChars_WithZeroCount() {
        byte[] bytes = "Hello".getBytes(UTF_8);
        char[] result = IOUtil.bytesToChars(bytes, 0, 0, UTF_8);
        assertEquals(0, result.length);
    }

    @Test
    public void testBytesToChars_NullInput() {
        char[] result = IOUtil.bytesToChars(null);
        assertEquals(0, result.length);
    }

    @Test
    public void testBytesToChars_EmptyInput() {
        char[] result = IOUtil.bytesToChars(new byte[0]);
        assertEquals(0, result.length);
    }

    @Test
    public void testString2InputStream_Default() throws IOException {
        InputStream is = IOUtil.stringToInputStream(TEST_CONTENT);
        assertNotNull(is);
        byte[] bytes = is.readAllBytes();
        assertEquals(TEST_CONTENT, new String(bytes, UTF_8));
        is.close();
    }

    @Test
    public void testString2InputStream_WithCharset() throws IOException {
        InputStream is = IOUtil.stringToInputStream(TEST_CONTENT, UTF_16);
        assertNotNull(is);
        byte[] bytes = is.readAllBytes();
        assertEquals(TEST_CONTENT, new String(bytes, UTF_16));
        is.close();
    }

    @Test
    public void testString2InputStream_EmptyString() throws IOException {
        InputStream is = IOUtil.stringToInputStream("");
        assertNotNull(is);
        byte[] bytes = is.readAllBytes();
        assertEquals(0, bytes.length);
        is.close();
    }

    @Test
    public void testString2InputStream_UnicodeContent() throws IOException {
        InputStream is = IOUtil.stringToInputStream(UNICODE_CONTENT, UTF_8);
        assertNotNull(is);
        byte[] bytes = is.readAllBytes();
        assertEquals(UNICODE_CONTENT, new String(bytes, UTF_8));
        is.close();
    }

    @Test
    public void testString2InputStream_MultilineContent() throws IOException {
        InputStream is = IOUtil.stringToInputStream(MULTILINE_CONTENT);
        assertNotNull(is);
        byte[] bytes = is.readAllBytes();
        assertEquals(MULTILINE_CONTENT, new String(bytes, UTF_8));
        is.close();
    }

    @Test
    public void testString2InputStream_NullString() {
        try (InputStream is = IOUtil.stringToInputStream(null)) {
            byte[] bytes = is.readAllBytes();
            assertEquals(0, bytes.length);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }

    }

    @Test
    public void testRoundTrip_StringToInputStreamToString() throws IOException {
        InputStream is = IOUtil.stringToInputStream(UNICODE_CONTENT, UTF_8);
        String result = IOUtil.readAllToString(is, UTF_8);
        assertEquals(UNICODE_CONTENT, result);
    }

    @Test
    public void testString2Reader() throws IOException {
        Reader reader = IOUtil.stringToReader(TEST_CONTENT);
        assertNotNull(reader);
        char[] buffer = new char[TEST_CONTENT.length()];
        int read = reader.read(buffer);
        assertEquals(TEST_CONTENT.length(), read);
        assertEquals(TEST_CONTENT, new String(buffer));
        reader.close();
    }

    @Test
    public void testString2Reader_EmptyString() throws IOException {
        Reader reader = IOUtil.stringToReader("");
        assertNotNull(reader);
        char[] buffer = new char[10];
        int read = reader.read(buffer);
        assertEquals(-1, read);
        reader.close();
    }

    @Test
    public void testString2Reader_UnicodeContent() throws IOException {
        Reader reader = IOUtil.stringToReader(UNICODE_CONTENT);
        assertNotNull(reader);
        char[] buffer = new char[UNICODE_CONTENT.length()];
        int read = reader.read(buffer);
        assertEquals(UNICODE_CONTENT.length(), read);
        assertEquals(UNICODE_CONTENT, new String(buffer));
        reader.close();
    }

    @Test
    public void testString2Reader_MultilineContent() throws IOException {
        Reader reader = IOUtil.stringToReader(MULTILINE_CONTENT);
        assertNotNull(reader);
        char[] buffer = new char[MULTILINE_CONTENT.length()];
        int read = reader.read(buffer);
        assertEquals(MULTILINE_CONTENT.length(), read);
        assertEquals(MULTILINE_CONTENT, new String(buffer));
        reader.close();
    }

    @Test
    public void testString2Reader_NullString() {
        try (Reader reader = IOUtil.stringToReader(null)) {
            char[] buffer = new char[10];
            int read = reader.read(buffer);
            assertEquals(-1, read);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Test
    public void testRoundTrip_StringToReaderToString() throws IOException {
        Reader reader = IOUtil.stringToReader(UNICODE_CONTENT);
        String result = IOUtil.readAllToString(reader);
        assertEquals(UNICODE_CONTENT, result);
    }

    @Test
    public void testStringBuilder2Writer() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.newStringWriter(sb);
        assertNotNull(writer);
        writer.write(TEST_CONTENT);
        writer.flush();
        assertEquals(TEST_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testStringBuilder2Writer_MultipleWrites() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.newStringWriter(sb);
        writer.write("Hello");
        writer.write(" ");
        writer.write("World");
        writer.flush();
        assertEquals("Hello World", sb.toString());
        writer.close();
    }

    @Test
    public void testStringBuilder2Writer_EmptyWrite() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.newStringWriter(sb);
        writer.write("");
        writer.flush();
        assertEquals("", sb.toString());
        writer.close();
    }

    @Test
    public void testStringBuilder2Writer_UnicodeContent() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.newStringWriter(sb);
        writer.write(UNICODE_CONTENT);
        writer.flush();
        assertEquals(UNICODE_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testStringBuilder2Writer_MultilineContent() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.newStringWriter(sb);
        writer.write(MULTILINE_CONTENT);
        writer.flush();
        assertEquals(MULTILINE_CONTENT, sb.toString());
        writer.close();
    }

    @Test
    public void testBufferRead_PartialContent() throws IOException {
        byte[] byteBuffer = new byte[5];
        int bytesRead = IOUtil.read(tempFile, byteBuffer);
        assertEquals(5, bytesRead);

        char[] charBuffer = new char[5];
        int charsRead = IOUtil.read(tempFile, charBuffer);
        assertEquals(5, charsRead);

        assertEquals(new String(byteBuffer, UTF_8), new String(charBuffer));
    }

    @Test
    public void testMerge_destAmongSources_isRejectedWithoutDataLoss() throws IOException {
        // Regression: merge(...) truncates destFile up front (newFileOutputStream), so a source
        // that is the same file as the destination was silently wiped (or read back the freshly
        // merged bytes) instead of being merged. It must be rejected before the destination is opened.
        final File other = Files.createTempFile(tempFolder, "mergeSrc", ".txt").toFile();
        IOUtil.write("other-content", other);
        final long originalLen = tempFile.length();
        assertTrue(originalLen > 0);
        assertThrows(IllegalArgumentException.class, () -> IOUtil.merge(CommonUtil.asList(tempFile), new byte[0], tempFile));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.merge(CommonUtil.asList(other, tempFile), new byte[0], tempFile));
        assertEquals(originalLen, tempFile.length());
        assertEquals(TEST_CONTENT, IOUtil.readAllToString(tempFile));
    }

    @Test
    public void testTransfer_Channels() throws IOException {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".bin").toFile();
        File targetFile = Files.createTempFile(tempFolder, "target", ".bin").toFile();

        byte[] testData = "Transfer test data".getBytes(UTF_8);
        Files.write(sourceFile.toPath(), testData);

        try (FileInputStream fis = new FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(targetFile)) {

            long transferred = IOUtil.transfer(fis.getChannel(), fos.getChannel());

            assertEquals(testData.length, transferred);
            assertArrayEquals(testData, Files.readAllBytes(targetFile.toPath()));
        }
    }

    @Test
    public void testTransfer_EmptyChannel() throws IOException {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".bin").toFile();
        File targetFile = Files.createTempFile(tempFolder, "target", ".bin").toFile();

        try (FileInputStream fis = new FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(targetFile)) {

            long transferred = IOUtil.transfer(fis.getChannel(), fos.getChannel());

            assertEquals(0, transferred);
        }
    }

    @Test
    public void testTransfer_LargeData() throws IOException {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".bin").toFile();
        File targetFile = Files.createTempFile(tempFolder, "target", ".bin").toFile();

        byte[] largeData = new byte[100000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        Files.write(sourceFile.toPath(), largeData);

        try (FileInputStream fis = new FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(targetFile)) {

            long transferred = IOUtil.transfer(fis.getChannel(), fos.getChannel());

            assertEquals(largeData.length, transferred);
            assertArrayEquals(largeData, Files.readAllBytes(targetFile.toPath()));
        }
    }

    @Test
    public void testMap_DefaultMode() throws IOException {
        File mapFile = Files.createTempFile(tempFolder, "map", ".bin").toFile();
        byte[] data = "Memory mapped file".getBytes(UTF_8);
        Files.write(mapFile.toPath(), data);

        java.nio.MappedByteBuffer buffer = IOUtil.map(mapFile);

        assertNotNull(buffer);
        assertEquals(data.length, buffer.remaining());

        byte[] read = new byte[data.length];
        buffer.get(read);
        assertArrayEquals(data, read);

        unmap(buffer);
    }

    @Test
    public void testMap_ReadOnlyMode() throws IOException {
        File mapFile = Files.createTempFile(tempFolder, "map", ".bin").toFile();
        byte[] data = "Read only mapping".getBytes(UTF_8);
        Files.write(mapFile.toPath(), data);

        java.nio.MappedByteBuffer buffer = IOUtil.map(mapFile, java.nio.channels.FileChannel.MapMode.READ_ONLY);

        assertNotNull(buffer);
        assertTrue(buffer.isReadOnly());
        assertEquals(data.length, buffer.remaining());
        unmap(buffer);
    }

    @Test
    public void testMap_ReadWriteMode() throws IOException {
        File mapFile = Files.createTempFile(tempFolder, "map", ".bin").toFile();
        byte[] data = "Read write mapping".getBytes(UTF_8);
        Files.write(mapFile.toPath(), data);

        java.nio.MappedByteBuffer buffer = IOUtil.map(mapFile, java.nio.channels.FileChannel.MapMode.READ_WRITE);

        assertNotNull(buffer);
        assertEquals(data.length, buffer.remaining());

        buffer.put(0, (byte) 'X');
        buffer.force();

        unmap(buffer);
    }

    @Test
    public void testMap_WithOffsetAndCount() throws IOException {
        File mapFile = Files.createTempFile(tempFolder, "map", ".bin").toFile();
        byte[] data = "0123456789ABCDEFGHIJ".getBytes(UTF_8);
        Files.write(mapFile.toPath(), data);

        java.nio.MappedByteBuffer buffer = IOUtil.map(mapFile, java.nio.channels.FileChannel.MapMode.READ_ONLY, 5, 10);

        assertNotNull(buffer);
        assertEquals(10, buffer.remaining());

        byte[] read = new byte[10];
        buffer.get(read);
        assertEquals("56789ABCDE", new String(read, UTF_8));

        unmap(buffer);
    }

    @Test
    public void testMap_EmptyFile() throws IOException {
        File mapFile = Files.createTempFile(tempFolder, "map", ".bin").toFile();

        java.nio.MappedByteBuffer buffer = IOUtil.map(mapFile, java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, 0);

        assertNotNull(buffer);
        assertEquals(0, buffer.remaining());
    }

    @Test
    public void testMap_NonexistentFile() {
        File nonexistent = new File(tempFolder.toFile(), "nonexistent.bin");

        // A missing source is a FileNotFoundException wrapped in UncheckedIOException, per the class contract -
        // and the whole map(..) family agrees on that, whichever overload is called.
        assertThrows(UncheckedIOException.class, () -> IOUtil.map(nonexistent));
        assertThrows(UncheckedIOException.class, () -> IOUtil.map(nonexistent, java.nio.channels.FileChannel.MapMode.READ_ONLY));
        assertThrows(UncheckedIOException.class, () -> IOUtil.map(nonexistent, java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, 4));

        // The whole-file overloads never create the file, not even for READ_WRITE: there would be nothing to map.
        assertThrows(UncheckedIOException.class, () -> IOUtil.map(nonexistent, java.nio.channels.FileChannel.MapMode.READ_WRITE));
        assertFalse(nonexistent.exists());

        // Only the explicitly-sized overload creates and extends a missing file, as its javadoc promises.
        File created = new File(tempFolder.toFile(), "created-by-map.bin");
        unmap(IOUtil.map(created, java.nio.channels.FileChannel.MapMode.READ_WRITE, 0, 8));
        assertTrue(created.exists());
        assertEquals(8, created.length());

        // A null argument is still a bad argument, not an I/O failure.
        assertThrows(IllegalArgumentException.class, () -> IOUtil.map(null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.map(nonexistent, null));
    }

    @Test
    public void testMap_LargeFile() throws IOException {
        File mapFile = Files.createTempFile(tempFolder, "map", ".bin").toFile();
        byte[] largeData = new byte[10000];
        for (int i = 0; i < largeData.length; i++) {
            largeData[i] = (byte) (i % 256);
        }
        Files.write(mapFile.toPath(), largeData);

        java.nio.MappedByteBuffer buffer = IOUtil.map(mapFile);

        assertNotNull(buffer);
        assertEquals(largeData.length, buffer.remaining());

        unmap(buffer);
    }

    @ParameterizedTest(name = "simplifyPath({0}) -> {1}")
    @MethodSource("simplifyPathCases")
    public void testSimplifyPath(final String input, final String expected) {
        assertEquals(expected, IOUtil.simplifyPath(input));
    }

    @Test
    public void testLZ4BlockCompressDecompress() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (LZ4BlockOutputStream lz4Out = IOUtil.newLZ4BlockOutputStream(baos, 8192)) {
            lz4Out.write(MULTILINE_CONTENT.getBytes(UTF_8));
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray());
             LZ4BlockInputStream lz4In = IOUtil.newLZ4BlockInputStream(is)) {
            byte[] buffer = new byte[1024];
            int bytesRead = lz4In.read(buffer);
            assertEquals(MULTILINE_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testSnappyCompressDecompress() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (SnappyOutputStream snappyOut = IOUtil.newSnappyOutputStream(baos, 8192)) {
            snappyOut.write(MULTILINE_CONTENT.getBytes(UTF_8));
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray());
             SnappyInputStream snappyIn = IOUtil.newSnappyInputStream(is)) {
            byte[] buffer = new byte[1024];
            int bytesRead = snappyIn.read(buffer);
            assertEquals(MULTILINE_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testGZIPCompressDecompress() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gzipOut = IOUtil.newGZIPOutputStream(baos, 8192)) {
            gzipOut.write(MULTILINE_CONTENT.getBytes(UTF_8));
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray());
             java.util.zip.GZIPInputStream gzipIn = IOUtil.newGZIPInputStream(is, 8192)) {
            byte[] buffer = new byte[1024];
            int bytesRead = gzipIn.read(buffer);
            assertEquals(MULTILINE_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testMove_FileToDirectory() throws Exception {
        File srcFile = Files.createTempFile(tempFolder, "move_src", ".txt").toFile();
        Files.write(srcFile.toPath(), "Move content".getBytes());

        File destDir = Files.createTempDirectory(tempFolder, "move_dest").toFile();

        IOUtil.moveToDirectory(srcFile, destDir);

        File movedFile = new File(destDir, srcFile.getName());
        assertTrue(movedFile.exists());
        assertTrue(!srcFile.exists());
        assertEquals("Move content", IOUtil.readAllToString(movedFile));
    }

    @Test
    public void testMove_FileToDirectoryWithOptions() throws Exception {
        File srcFile = Files.createTempFile(tempFolder, "move_src", ".txt").toFile();
        Files.write(srcFile.toPath(), "Move content with options".getBytes());

        File destDir = Files.createTempDirectory(tempFolder, "move_dest").toFile();

        IOUtil.moveToDirectory(srcFile, destDir, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        File movedFile = new File(destDir, srcFile.getName());
        assertTrue(movedFile.exists());
        assertTrue(!srcFile.exists());
    }

    @Test
    public void testMove_PathToPath() throws Exception {
        File srcFile = Files.createTempFile(tempFolder, "move_src", ".txt").toFile();
        Files.write(srcFile.toPath(), "Path move content".getBytes());

        Path target = tempFolder.resolve("moved_file.txt");

        Path result = IOUtil.move(srcFile.toPath(), target, java.nio.file.StandardCopyOption.REPLACE_EXISTING);

        assertNotNull(result);
        assertTrue(Files.exists(target));
        assertTrue(!Files.exists(srcFile.toPath()));
    }

    @Test
    public void testRenameTo_NullFile() {
        boolean result = IOUtil.renameTo(null, "newname.txt");
        assertFalse(result);
    }

    @Test
    public void testRenameTo_Success() throws Exception {
        File srcFile = Files.createTempFile(tempFolder, "rename_src", ".txt").toFile();
        Files.write(srcFile.toPath(), "Rename content".getBytes());

        boolean result = IOUtil.renameTo(srcFile, "renamed_file.txt");

        assertTrue(result);
        File renamedFile = new File(srcFile.getParent(), "renamed_file.txt");
        assertTrue(renamedFile.exists());
        assertTrue(!srcFile.exists());
    }

    @Test
    public void testRenameTo_DifferentName() throws Exception {
        File srcFile = Files.createTempFile(tempFolder, "original", ".txt").toFile();
        String newName = "new_name.txt";

        boolean result = IOUtil.renameTo(srcFile, newName);

        assertTrue(result);
        File renamedFile = new File(srcFile.getParent(), newName);
        assertTrue(renamedFile.exists());
    }

    @Test
    public void testRenameTo_NonExistentFile() throws Exception {
        File nonExistent = new File(tempFolder.toFile(), "nonexistent.txt");
        boolean result = IOUtil.renameTo(nonExistent, "newname.txt");
        assertFalse(result);
    }

    // ===== createNewFileIfNotExists =====

    @Test
    public void testCreateFileIfNotExists_InNewSubDirectory() throws Exception {
        File newDir = new File(tempFolder.toFile(), "subdir_for_test");
        File newFile = new File(newDir, "new_file.txt");
        assertFalse(newFile.exists());
        boolean created = IOUtil.createFileIfNotExists(newFile);
        assertTrue(created);
        assertTrue(newFile.exists());
    }

    @Test
    public void testCreateFileIfNotExists_NewFile() throws Exception {
        File file = new File(tempFolder.toFile(), "new_file.txt");
        assertTrue(!file.exists());

        boolean result = IOUtil.createFileIfNotExists(file);

        assertTrue(result);
        assertTrue(file.exists());
    }

    @Test
    public void testCreateFileIfNotExists_ExistingFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "existing", ".txt").toFile();
        assertTrue(file.exists());

        boolean result = IOUtil.createFileIfNotExists(file);

        assertTrue(!result);
        assertTrue(file.exists());
    }

    @Test
    public void testCreateFileIfNotExists_WithNonExistingParent() throws Exception {
        File parentDir = new File(tempFolder.toFile(), "new_parent");
        File file = new File(parentDir, "new_file.txt");
        assertTrue(!file.exists());

        boolean result = IOUtil.createFileIfNotExists(file);

        assertTrue(result);
        assertTrue(file.exists());
        assertTrue(parentDir.exists());
    }

    @Test
    public void testMkdirIfNotExists_NewDirectory() throws Exception {
        File dir = new File(tempFolder.toFile(), "new_dir");
        assertTrue(!dir.exists());

        boolean result = IOUtil.mkdirIfNotExists(dir);

        assertTrue(result);
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
    }

    @Test
    public void testMkdirIfNotExists_ExistingDirectory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "existing_dir").toFile();
        assertTrue(dir.exists());

        boolean result = IOUtil.mkdirIfNotExists(dir);

        assertTrue(!result);
        assertTrue(dir.exists());
    }

    @Test
    public void testMkdirIfNotExists_ExistingFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "file", ".txt").toFile();
        assertTrue(file.exists());
        assertTrue(!file.isDirectory());

        boolean result = IOUtil.mkdirIfNotExists(file);

        assertTrue(!result);
    }

    @Test
    public void testMkdirsIfNotExists_NewDirectoryHierarchy() throws Exception {
        File dir = new File(tempFolder.toFile(), "parent/child/grandchild");
        assertTrue(!dir.exists());

        boolean result = IOUtil.mkdirsIfNotExists(dir);

        assertTrue(result);
        assertTrue(dir.exists());
        assertTrue(dir.isDirectory());
    }

    @Test
    public void testMkdirsIfNotExists_ExistingDirectory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "existing_dirs").toFile();
        assertTrue(dir.exists());

        boolean result = IOUtil.mkdirsIfNotExists(dir);

        assertTrue(!result);
        assertTrue(dir.exists());
    }

    @Test
    public void testMkdirsIfNotExists_SingleDirectory() throws Exception {
        File dir = new File(tempFolder.toFile(), "single_dir");
        assertTrue(!dir.exists());

        boolean result = IOUtil.mkdirsIfNotExists(dir);

        assertTrue(result);
        assertTrue(dir.exists());
    }

    // ===== checkFileExists (tested indirectly via copyFile) =====

    @Test
    public void testCheckFileExists_NonExistingFile_ViaRead() throws Exception {
        File nonExistent = new File(tempFolder.toFile(), "does_not_exist.txt");
        assertFalse(nonExistent.exists());
        // readAllLines calls checkFileExists internally
        assertThrows(Exception.class, () -> IOUtil.readAllLines(nonExistent));
    }

    // ===== checkDestDirectory =====

    @Test
    public void testCheckDestDirectory_NonExistingDirectory() throws Exception {
        File newDir = new File(tempFolder.toFile(), "newSubDir");
        assertFalse(newDir.exists());
        // copyToDirectory will call checkDestDirectory internally
        File srcFile = tempFile;
        IOUtil.copyToDirectory(srcFile, newDir);
        assertTrue(newDir.exists());
    }

    @Test
    public void testCheckDestDirectory_ExistingFile_ThrowsException() throws Exception {
        // copyToDirectory should throw when destDir is a file
        assertThrows(Exception.class, () -> IOUtil.copyToDirectory(tempFile, tempFile));
    }

    @Test
    public void testZipMultipleEntries() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zipOut = IOUtil.newZipOutputStream(baos)) {
            java.util.zip.ZipEntry entry1 = new java.util.zip.ZipEntry("file1.txt");
            zipOut.putNextEntry(entry1);
            zipOut.write("Content 1".getBytes(UTF_8));
            zipOut.closeEntry();

            java.util.zip.ZipEntry entry2 = new java.util.zip.ZipEntry("file2.txt");
            zipOut.putNextEntry(entry2);
            zipOut.write("Content 2".getBytes(UTF_8));
            zipOut.closeEntry();
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray());
             java.util.zip.ZipInputStream zipIn = IOUtil.newZipInputStream(is)) {
            java.util.zip.ZipEntry entry1 = zipIn.getNextEntry();
            assertEquals("file1.txt", entry1.getName());

            java.util.zip.ZipEntry entry2 = zipIn.getNextEntry();
            assertEquals("file2.txt", entry2.getName());
        }
    }

    @Test
    public void testZip_SingleFile() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".txt").toFile();
        File zipFile = Files.createTempFile(tempFolder, "archive", ".zip").toFile();
        Files.write(sourceFile.toPath(), "Test content for zipping".getBytes(UTF_8));

        IOUtil.zip(sourceFile, zipFile);

        assertTrue(zipFile.exists());
        assertTrue(zipFile.length() > 0);
    }

    @Test
    public void testZip_EmptyFile() throws Exception {
        File emptySource = Files.createTempFile(tempFolder, "empty", ".txt").toFile();
        File zipFile = Files.createTempFile(tempFolder, "empty-archive", ".zip").toFile();

        IOUtil.zip(emptySource, zipFile);

        assertTrue(zipFile.exists());
        assertTrue(zipFile.length() > 0);
    }

    @Test
    public void testZip_Directory() throws Exception {
        File sourceDir = Files.createTempDirectory(tempFolder, "source-dir").toFile();
        File file1 = new File(sourceDir, "file1.txt");
        File file2 = new File(sourceDir, "file2.txt");
        Files.write(file1.toPath(), "Content 1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Content 2".getBytes(UTF_8));

        File zipFile = Files.createTempFile(tempFolder, "dir-archive", ".zip").toFile();
        IOUtil.zip(sourceDir, zipFile);

        assertTrue(zipFile.exists());
        assertTrue(zipFile.length() > 0);
    }

    @Test
    public void testZip_CollectionOfFiles() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "file1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "file2", ".txt").toFile();
        File file3 = Files.createTempFile(tempFolder, "file3", ".txt").toFile();

        Files.write(file1.toPath(), "Content 1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Content 2".getBytes(UTF_8));
        Files.write(file3.toPath(), "Content 3".getBytes(UTF_8));

        java.util.List<File> sourceFiles = java.util.Arrays.asList(file1, file2, file3);
        File zipFile = Files.createTempFile(tempFolder, "multi-archive", ".zip").toFile();

        IOUtil.zip(sourceFiles, zipFile);

        assertTrue(zipFile.exists());
        assertTrue(zipFile.length() > 0);
    }

    @Test
    public void testZip_EmptyCollection() throws Exception {
        java.util.List<File> emptyList = new java.util.ArrayList<>();
        File zipFile = Files.createTempFile(tempFolder, "empty-collection", ".zip").toFile();

        IOUtil.zip(emptyList, zipFile);

        assertTrue(zipFile.exists());
    }

    @Test
    public void testZip_LargeFile() throws Exception {
        File largeSource = Files.createTempFile(tempFolder, "large-source", ".txt").toFile();
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            largeContent.append("This is line ").append(i).append(" with some content to make it larger.\n");
        }
        Files.write(largeSource.toPath(), largeContent.toString().getBytes(UTF_8));

        File zipFile = Files.createTempFile(tempFolder, "large-zip", ".zip").toFile();
        IOUtil.zip(largeSource, zipFile);

        assertTrue(zipFile.exists());
        assertTrue(zipFile.length() > 0);
        assertTrue(zipFile.length() < largeSource.length());
    }

    @Test
    public void testZip_NonExistentSource_DoesNotTruncateExistingTarget() throws Exception {
        File missingSource = new File(tempFolder.toFile(), "missing-zip-source.txt");
        File zipFile = Files.createTempFile(tempFolder, "existing-archive", ".zip").toFile();
        Files.write(zipFile.toPath(), "precious existing content".getBytes(UTF_8));

        // zip is checked now, like its unzip counterpart.
        assertThrows(java.io.FileNotFoundException.class, () -> IOUtil.zip(missingSource, zipFile));

        // The existing target file must not be truncated when the source is invalid.
        assertEquals("precious existing content", new String(Files.readAllBytes(zipFile.toPath()), UTF_8));
    }

    @Test
    public void testZip_Collection_NonExistentSource_DoesNotTruncateExistingTarget() throws Exception {
        File okSource = Files.createTempFile(tempFolder, "ok-source", ".txt").toFile();
        Files.write(okSource.toPath(), "ok".getBytes(UTF_8));
        File missingSource = new File(tempFolder.toFile(), "missing-zip-source2.txt");

        File zipFile = Files.createTempFile(tempFolder, "existing-archive2", ".zip").toFile();
        Files.write(zipFile.toPath(), "precious existing content".getBytes(UTF_8));

        assertThrows(java.io.FileNotFoundException.class, () -> IOUtil.zip(java.util.Arrays.asList(okSource, missingSource), zipFile));

        // The existing target file must not be truncated when any source is invalid.
        assertEquals("precious existing content", new String(Files.readAllBytes(zipFile.toPath()), UTF_8));
    }

    @Test
    public void testUnzip_Basic() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "source", ".txt").toFile();
        Files.write(sourceFile.toPath(), "Content to unzip".getBytes(UTF_8));

        File zipFile = Files.createTempFile(tempFolder, "archive", ".zip").toFile();
        IOUtil.zip(sourceFile, zipFile);

        File targetDir = Files.createTempDirectory(tempFolder, "unzip-target").toFile();
        IOUtil.unzip(zipFile, targetDir);

        assertTrue(targetDir.exists());
        assertTrue(targetDir.isDirectory());
        File[] unzippedFiles = targetDir.listFiles();
        assertNotNull(unzippedFiles);
        assertTrue(unzippedFiles.length > 0);
    }

    @Test
    public void testUnzip_DirectoryStructure() throws Exception {
        File sourceDir = Files.createTempDirectory(tempFolder, "nested").toFile();
        File subDir = new File(sourceDir, "subdir");
        subDir.mkdir();

        File file1 = new File(sourceDir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        Files.write(file1.toPath(), "Root content".getBytes(UTF_8));
        Files.write(file2.toPath(), "Nested content".getBytes(UTF_8));

        File zipFile = Files.createTempFile(tempFolder, "nested-archive", ".zip").toFile();
        IOUtil.zip(sourceDir, zipFile);

        File targetDir = Files.createTempDirectory(tempFolder, "unzip-nested").toFile();
        IOUtil.unzip(zipFile, targetDir);

        assertTrue(targetDir.exists());
        File[] unzippedFiles = targetDir.listFiles();
        assertNotNull(unzippedFiles);
        assertTrue(unzippedFiles.length > 0);
    }

    @Test
    public void testUnzip_NonexistentZipFile() throws IOException {
        File nonexistentZip = new File(tempFolder.toFile(), "nonexistent.zip");
        File targetDir = Files.createTempDirectory(tempFolder, "unzip-fail").toFile();

        assertThrows(Exception.class, () -> IOUtil.unzip(nonexistentZip, targetDir));
    }

    @Test
    public void testMerge_FileArray() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "merge1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "merge2", ".txt").toFile();
        File file3 = Files.createTempFile(tempFolder, "merge3", ".txt").toFile();

        Files.write(file1.toPath(), "Part1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Part2".getBytes(UTF_8));
        Files.write(file3.toPath(), "Part3".getBytes(UTF_8));

        File[] sourceFiles = { file1, file2, file3 };
        File destFile = Files.createTempFile(tempFolder, "merged", ".txt").toFile();

        long totalBytes = IOUtil.merge(sourceFiles, destFile);

        assertTrue(totalBytes > 0);
        String merged = IOUtil.readAllToString(destFile);
        assertEquals("Part1Part2Part3", merged);
    }

    @Test
    public void testMerge_Collection() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "merge-c1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "merge-c2", ".txt").toFile();

        Files.write(file1.toPath(), "First".getBytes(UTF_8));
        Files.write(file2.toPath(), "Second".getBytes(UTF_8));

        java.util.List<File> sourceFiles = java.util.Arrays.asList(file1, file2);
        File destFile = Files.createTempFile(tempFolder, "merged-collection", ".txt").toFile();

        long totalBytes = IOUtil.merge(sourceFiles, destFile);

        assertTrue(totalBytes > 0);
        String merged = IOUtil.readAllToString(destFile);
        assertEquals("FirstSecond", merged);
    }

    @Test
    public void testMerge_WithDelimiter() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "delim1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "delim2", ".txt").toFile();

        Files.write(file1.toPath(), "Line1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Line2".getBytes(UTF_8));

        java.util.List<File> sourceFiles = java.util.Arrays.asList(file1, file2);
        File destFile = Files.createTempFile(tempFolder, "merged-delim", ".txt").toFile();

        byte[] delimiter = "\n".getBytes(UTF_8);
        long totalBytes = IOUtil.merge(sourceFiles, delimiter, destFile);

        assertTrue(totalBytes > 0);
        String merged = IOUtil.readAllToString(destFile);
        assertEquals("Line1\nLine2", merged);
    }

    @Test
    public void testMerge_EmptyFiles() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "empty1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "empty2", ".txt").toFile();

        File[] sourceFiles = { file1, file2 };
        File destFile = Files.createTempFile(tempFolder, "merged-empty", ".txt").toFile();

        long totalBytes = IOUtil.merge(sourceFiles, destFile);

        assertEquals(0, totalBytes);
    }

    @Test
    public void testMerge_SingleFile() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "single", ".txt").toFile();
        Files.write(file1.toPath(), "OnlyOne".getBytes(UTF_8));

        File[] sourceFiles = { file1 };
        File destFile = Files.createTempFile(tempFolder, "merged-single", ".txt").toFile();

        long totalBytes = IOUtil.merge(sourceFiles, destFile);

        assertTrue(totalBytes > 0);
        assertEquals("OnlyOne", IOUtil.readAllToString(destFile));
    }

    @Test
    public void testMerge_LargeFiles() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "large-merge1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "large-merge2", ".txt").toFile();

        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 1000; i++) {
            content.append("Line ").append(i).append("\n");
        }

        Files.write(file1.toPath(), content.toString().getBytes(UTF_8));
        Files.write(file2.toPath(), content.toString().getBytes(UTF_8));

        File[] sourceFiles = { file1, file2 };
        File destFile = Files.createTempFile(tempFolder, "large-merged", ".txt").toFile();

        long totalBytes = IOUtil.merge(sourceFiles, destFile);

        assertTrue(totalBytes > 0);
        assertEquals(file1.length() + file2.length(), totalBytes);
    }

    @Test
    public void testMerge_NonExistentSource_DoesNotTruncateExistingDest() throws Exception {
        File okSource = Files.createTempFile(tempFolder, "merge-ok", ".txt").toFile();
        Files.write(okSource.toPath(), "ok".getBytes(UTF_8));
        File missingSource = new File(tempFolder.toFile(), "missing-merge-source.txt");

        File destFile = Files.createTempFile(tempFolder, "merged-existing", ".txt").toFile();
        Files.write(destFile.toPath(), "precious existing content".getBytes(UTF_8));

        // merge is checked now, like its split/splitBySize counterparts.
        assertThrows(java.io.FileNotFoundException.class, () -> IOUtil.merge(java.util.Arrays.asList(okSource, missingSource), destFile));

        // The existing destination file must not be truncated when any source is invalid.
        assertEquals("precious existing content", new String(Files.readAllBytes(destFile.toPath()), UTF_8));
    }

    @Test
    public void testListFiles_NonRecursive() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "list-files").toFile();
        File file1 = new File(dir, "file1.txt");
        File file2 = new File(dir, "file2.txt");
        Files.write(file1.toPath(), "Content1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Content2".getBytes(UTF_8));

        java.util.List<File> files = IOUtil.listFiles(dir);

        assertNotNull(files);
        assertEquals(2, files.size());
    }

    @Test
    public void testListFiles_Recursive() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "list-recursive").toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();

        File file1 = new File(dir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        Files.write(file1.toPath(), "Content1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Content2".getBytes(UTF_8));

        java.util.List<File> files = IOUtil.listFiles(dir, true, false);

        assertNotNull(files);
        assertTrue(files.size() >= 2);
    }

    @Test
    public void testListFiles_ExcludeDirectories() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "exclude-dirs").toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();

        File file1 = new File(dir, "file1.txt");
        Files.write(file1.toPath(), "Content1".getBytes(UTF_8));

        java.util.List<File> files = IOUtil.listFiles(dir, true, true);

        assertNotNull(files);
        for (File f : files) {
            assertTrue(f.isFile());
        }
    }

    @Test
    public void testListFiles_WithFilter() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "filter-files").toFile();
        File txtFile = new File(dir, "file1.txt");
        File csvFile = new File(dir, "file2.csv");
        Files.write(txtFile.toPath(), "Text content".getBytes(UTF_8));
        Files.write(csvFile.toPath(), "CSV content".getBytes(UTF_8));

        java.util.List<File> files = IOUtil.listFiles(dir, false, (parent, f) -> f.getName().endsWith(".txt"));

        assertNotNull(files);
        assertEquals(1, files.size());
        assertTrue(files.get(0).getName().endsWith(".txt"));
    }

    @Test
    public void testListFiles_EmptyDirectory() throws Exception {
        File emptyDir = Files.createTempDirectory(tempFolder, "empty-dir").toFile();

        java.util.List<File> files = IOUtil.listFiles(emptyDir);

        assertNotNull(files);
        assertEquals(0, files.size());
    }

    @Test
    public void testListFiles_DeepNesting() throws Exception {
        File root = Files.createTempDirectory(tempFolder, "deep").toFile();
        File current = root;

        for (int i = 0; i < 5; i++) {
            current = new File(current, "level" + i);
            current.mkdir();
            File file = new File(current, "file" + i + ".txt");
            Files.write(file.toPath(), ("Content " + i).getBytes(UTF_8));
        }

        java.util.List<File> files = IOUtil.listFiles(root, true, true);

        assertNotNull(files);
        assertTrue(files.size() >= 5);
    }

    @Test
    public void testListFiles_NullPath() throws Exception {
        java.util.List<File> files = IOUtil.listFiles(null);
        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    public void testListFiles_NonExistingDirectory() throws Exception {
        File nonExistent = new File(tempFolder.toFile(), "nonexistent_list_dir");
        java.util.List<File> files = IOUtil.listFiles(nonExistent);
        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    public void testListFiles_RecursiveWithFilter() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "recursive-filter-test").toFile();
        File subDir = new File(dir, "sub");
        subDir.mkdir();
        File txtFile = new File(dir, "top.txt");
        File csvFile = new File(dir, "top.csv");
        File subTxtFile = new File(subDir, "nested.txt");
        Files.write(txtFile.toPath(), "txt".getBytes(UTF_8));
        Files.write(csvFile.toPath(), "csv".getBytes(UTF_8));
        Files.write(subTxtFile.toPath(), "nested".getBytes(UTF_8));

        java.util.List<File> files = IOUtil.listFiles(dir, true, (parent, f) -> f.getName().endsWith(".txt"));
        assertNotNull(files);
        assertEquals(2, files.size());
    }

    @Test
    public void testListDirectories_NonRecursive() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "list-dirs").toFile();
        File subDir1 = new File(dir, "subdir1");
        File subDir2 = new File(dir, "subdir2");
        subDir1.mkdir();
        subDir2.mkdir();

        File file1 = new File(dir, "file1.txt");
        Files.write(file1.toPath(), "Content".getBytes(UTF_8));

        java.util.List<File> dirs = IOUtil.listDirectories(dir);

        assertNotNull(dirs);
        assertEquals(2, dirs.size());
        for (File d : dirs) {
            assertTrue(d.isDirectory());
        }
    }

    @Test
    public void testListDirectories_Recursive() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "list-dirs-rec").toFile();
        File subDir1 = new File(dir, "level1");
        subDir1.mkdir();
        File subDir2 = new File(subDir1, "level2");
        subDir2.mkdir();

        java.util.List<File> dirs = IOUtil.listDirectories(dir, true);

        assertNotNull(dirs);
        assertTrue(dirs.size() >= 2);
        for (File d : dirs) {
            assertTrue(d.isDirectory());
        }
    }

    @Test
    public void testListDirectories_EmptyDirectory() throws Exception {
        File emptyDir = Files.createTempDirectory(tempFolder, "empty-for-dirs").toFile();

        java.util.List<File> dirs = IOUtil.listDirectories(emptyDir);

        assertNotNull(dirs);
        assertEquals(0, dirs.size());
    }

    @Test
    public void testListDirectories_NullPath() throws Exception {
        java.util.List<File> dirs = IOUtil.listDirectories(null);
        assertNotNull(dirs);
        assertTrue(dirs.isEmpty());
    }

    @Test
    public void testListDirectories_NonExistingDirectory() throws Exception {
        File nonExistent = new File(tempFolder.toFile(), "nonexistent_listdir");
        java.util.List<File> dirs = IOUtil.listDirectories(nonExistent);
        assertNotNull(dirs);
        assertTrue(dirs.isEmpty());
    }

    @Test
    public void testWalk_Basic() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "walk-basic").toFile();
        File file1 = new File(dir, "file1.txt");
        Files.write(file1.toPath(), "Content".getBytes(UTF_8));

        com.landawn.abacus.util.stream.Stream<File> stream = IOUtil.walk(dir);

        assertNotNull(stream);
        long count = stream.count();
        assertTrue(count > 0);
    }

    @Test
    public void testWalk_Recursive() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "walk-rec").toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();

        File file1 = new File(dir, "file1.txt");
        File file2 = new File(subDir, "file2.txt");
        Files.write(file1.toPath(), "Content1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Content2".getBytes(UTF_8));

        com.landawn.abacus.util.stream.Stream<File> stream = IOUtil.walk(dir, true, false);

        assertNotNull(stream);
        long count = stream.count();
        assertTrue(count >= 2);
    }

    @Test
    public void testWalk_ExcludeDirectories() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "walk-exclude").toFile();
        File subDir = new File(dir, "subdir");
        subDir.mkdir();

        File file1 = new File(dir, "file1.txt");
        Files.write(file1.toPath(), "Content".getBytes(UTF_8));

        com.landawn.abacus.util.stream.Stream<File> stream = IOUtil.walk(dir, true, true);

        assertNotNull(stream);
        java.util.List<File> files = stream.toList();
        for (File f : files) {
            assertTrue(f.isFile());
        }
    }

    @Test
    public void testWalk_EmptyDirectory() throws Exception {
        File emptyDir = Files.createTempDirectory(tempFolder, "walk-empty").toFile();

        com.landawn.abacus.util.stream.Stream<File> stream = IOUtil.walk(emptyDir);

        assertNotNull(stream);
        long count = stream.count();
        assertEquals(0, count);
    }

    @Test
    public void testWalk_WithFileFilter() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "walk-filter").toFile();
        File txtFile = new File(dir, "file.txt");
        File csvFile = new File(dir, "file.csv");
        Files.write(txtFile.toPath(), "Text".getBytes(UTF_8));
        Files.write(csvFile.toPath(), "CSV".getBytes(UTF_8));

        com.landawn.abacus.util.stream.Stream<File> stream = IOUtil.walk(dir, false, false);
        long txtCount = stream.filter(f -> f.getName().endsWith(".txt")).count();

        assertEquals(1, txtCount);
    }

    @Test
    public void testFreeDiskSpaceKb_WithPath() throws Exception {
        String path = tempFolder.toFile().getAbsolutePath();
        long freeSpace = IOUtil.freeDiskSpaceInKB(path);

        assertTrue(freeSpace > 0);
    }

    @Test
    public void testFreeDiskSpaceKb_WithPathAndTimeout() throws Exception {
        String path = tempFolder.toFile().getAbsolutePath();
        long freeSpace = IOUtil.freeDiskSpaceInKB(path, 5000);

        assertTrue(freeSpace > 0);
    }

    @Test
    public void testRoundTrip_FileWriteRead() throws IOException {
        File testFile = Files.createTempFile(tempFolder, "roundtrip", ".txt").toFile();
        Files.write(testFile.toPath(), UNICODE_CONTENT.getBytes(UTF_8));

        String result = IOUtil.readAllToString(testFile, UTF_8);
        assertEquals(UNICODE_CONTENT, result);
    }

    @Test
    public void testLineRoundTrip_WriteAndRead() throws IOException {
        File outputFile = Files.createTempFile(tempFolder, "roundtrip", ".txt").toFile();
        java.util.List<String> originalLines = java.util.Arrays.asList("First", "Second", "Third");

        IOUtil.writeLines(originalLines, outputFile);
        java.util.List<String> readLines = IOUtil.readAllLines(outputFile);

        assertEquals(originalLines.size(), readLines.size());
        assertEquals("First", readLines.get(0));
        assertEquals("Second", readLines.get(1));
        assertEquals("Third", readLines.get(2));
    }

    @Test
    public void testLineOperations_FirstLastAndIndex() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), "First\nSecond\nThird\nFourth\nFifth".getBytes(UTF_8));

        String first = IOUtil.readFirstLine(multilineFile);
        assertEquals("First", first);

        String last = IOUtil.readLastLine(multilineFile);
        assertEquals("Fifth", last);

        String third = IOUtil.readLine(multilineFile, 2);
        assertEquals("Third", third);
    }

    @Test
    public void testLineOperations_PartialRead() throws IOException {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), "Line0\nLine1\nLine2\nLine3\nLine4\nLine5".getBytes(UTF_8));

        java.util.List<String> lines = IOUtil.readLines(multilineFile, 2, 3);
        assertEquals(3, lines.size());
        assertEquals("Line2", lines.get(0));
        assertEquals("Line3", lines.get(1));
        assertEquals("Line4", lines.get(2));
    }

    @Test
    public void testTouch_ExistingFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "touch-existing", ".txt").toFile();
        Files.write(file.toPath(), "keep me".getBytes(UTF_8));
        long originalModified = file.lastModified();

        Thread.sleep(100);

        IOUtil.touch(file);

        assertTrue(file.lastModified() >= originalModified);
        // touch only stamps the time; it must not truncate an existing file.
        assertEquals("keep me", new String(Files.readAllBytes(file.toPath()), UTF_8));
    }

    @Test
    public void testTouch_NonexistentFile_IsCreated() throws Exception {
        File newFile = new File(tempFolder.toFile(), "new-touch-file.txt");
        assertFalse(newFile.exists());

        IOUtil.touch(newFile);

        assertTrue(newFile.exists());
        assertEquals(0, newFile.length());
    }

    @Test
    public void testTouch_NonexistentFile_CreatesMissingParentDirectories() throws Exception {
        File newFile = new File(tempFolder.toFile(), "touch-parent/nested/created.txt");
        assertFalse(newFile.exists());

        IOUtil.touch(newFile);

        assertTrue(newFile.exists());
        assertTrue(newFile.getParentFile().isDirectory());
    }

    @Test
    public void testTouch_Directory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "touch-dir").toFile();

        IOUtil.touch(dir);

        assertTrue(dir.isDirectory());
    }

    @Test
    public void testTouch_NullFile() throws Exception {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.touch(null));
    }

    // ===== updateLastModified: the non-creating counterpart of touch =====

    @Test
    public void testUpdateLastModified_ExistingFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "update-existing", ".txt").toFile();
        long originalModified = file.lastModified();

        Thread.sleep(100);

        assertTrue(IOUtil.updateLastModified(file));
        assertTrue(file.lastModified() >= originalModified);
    }

    @Test
    public void testUpdateLastModified_NonexistentFile_DoesNotCreate() throws Exception {
        File newFile = new File(tempFolder.toFile(), "never-created.txt");

        assertFalse(IOUtil.updateLastModified(newFile));
        assertFalse(newFile.exists());
    }

    @Test
    public void testUpdateLastModified_Directory() throws Exception {
        File dir = Files.createTempDirectory(tempFolder, "update-dir").toFile();

        assertTrue(IOUtil.updateLastModified(dir));
    }

    @Test
    public void testUpdateLastModified_NullFile() throws Exception {
        assertFalse(IOUtil.updateLastModified(null));
    }

    @Test
    public void testLazyFileLineIteratorNextAfterCloseThrowsNoSuchElementException() throws Exception {
        final Class<?> iteratorClass = java.util.Arrays.stream(IOUtil.class.getDeclaredClasses())
                .filter(it -> it.getSimpleName().equals("LazyFileLineIterator"))
                .findFirst()
                .orElseThrow();
        final java.lang.reflect.Constructor<?> constructor = iteratorClass.getDeclaredConstructor(File.class, java.nio.charset.Charset.class);
        constructor.setAccessible(true);
        final Object iterator = constructor.newInstance(emptyFile, UTF_8);

        ((AutoCloseable) iterator).close();

        assertThrows(NoSuchElementException.class, () -> ((Iterator<?>) iterator).next());
    }

    @Test
    public void testOpenFile_ZipAndGzipViaReflection() throws Exception {
        java.lang.reflect.Method method = IOUtil.class.getDeclaredMethod("openFile", File.class, Holder.class);
        method.setAccessible(true);

        File gzipFile = Files.createTempFile(tempFolder, "io-util-open", ".gz").toFile();
        try (java.util.zip.GZIPOutputStream out = new java.util.zip.GZIPOutputStream(Files.newOutputStream(gzipFile.toPath()))) {
            out.write("gzip-content".getBytes(UTF_8));
        }

        Holder<java.util.zip.ZipFile> gzipHolder = Holder.of(null);
        try (java.io.InputStream in = (java.io.InputStream) method.invoke(null, gzipFile, gzipHolder)) {
            assertEquals("gzip-content", new String(IOUtil.readAllBytes(in), UTF_8));
            assertNull(gzipHolder.value());
        }

        File zipFile = Files.createTempFile(tempFolder, "io-util-open", ".zip").toFile();
        try (java.util.zip.ZipOutputStream out = new java.util.zip.ZipOutputStream(Files.newOutputStream(zipFile.toPath()))) {
            out.putNextEntry(new java.util.zip.ZipEntry("entry.txt"));
            out.write("zip-content".getBytes(UTF_8));
            out.closeEntry();
        }

        Holder<java.util.zip.ZipFile> zipHolder = Holder.of(null);
        try (java.io.InputStream in = (java.io.InputStream) method.invoke(null, zipFile, zipHolder)) {
            assertEquals("zip-content", new String(IOUtil.readAllBytes(in), UTF_8));
            assertNotNull(zipHolder.value());
        } finally {
            IOUtil.close(zipHolder.value());
        }
    }

    @Test
    public void testListFiles_recursive_doesNotFollowSymlinkCycle() throws Exception {
        File root = Files.createTempDirectory(tempFolder, "symlink-cycle").toFile();
        File sub = new File(root, "sub");
        assertTrue(sub.mkdir());
        Files.write(new File(sub, "leaf.txt").toPath(), "leaf".getBytes(UTF_8));

        // sub/loop -> root  (cycle: root/sub/loop/sub/loop/sub/...)
        Path loop = sub.toPath().resolve("loop");
        assumeTrue(trySymlink(loop, root.toPath()), SYMLINK_UNSUPPORTED);

        // Must complete (no StackOverflowError / no hang).
        java.util.List<File> files = IOUtil.listFiles(root, true, false);
        assertNotNull(files);
        // The symlink itself is reported, but its contents are NOT recursed into.
        // Therefore "leaf.txt" should appear at most twice (real file, possibly via the link).
        long leafCount = files.stream().filter(f -> "leaf.txt".equals(f.getName())).count();
        assertTrue(leafCount <= 2, "leaf.txt appeared " + leafCount + " times — symlink was followed recursively");
    }

    @Test
    public void testListFiles_nullParentReturnsEmptyNotNpe() {
        java.util.List<File> files = IOUtil.listFiles(null, true, false);
        assertNotNull(files);
        assertTrue(files.isEmpty());
    }

    @Test
    public void testZipSingleSegmentRelativeDirectory() throws Exception {
        final File srcDir = new File("io_util_relative_zip_dir_" + System.nanoTime());
        final File child = new File(srcDir, "child.txt");
        final File zip = tempFolder.resolve("relative-dir.zip").toFile();

        try {
            assertTrue(srcDir.mkdir());
            IOUtil.write("child", child);

            assertDoesNotThrow(() -> IOUtil.zip(srcDir, zip));

            try (java.util.zip.ZipFile zf = new java.util.zip.ZipFile(zip)) {
                assertNotNull(zf.getEntry(srcDir.getName() + "/"));
                assertNotNull(zf.getEntry(srcDir.getName() + "/child.txt"));
            }
        } finally {
            IOUtil.deleteRecursivelyIfExists(srcDir);
        }
    }

    @Test
    public void testSimplifyPathDoesNotTrimComponents() {
        // regression: trimResults() stripped spaces from path components, which could fabricate
        // parent-directory traversal (" .. " -> "..") and merge distinct names ("b " -> "b")
        assertEquals("a/b /c", IOUtil.simplifyPath("a/b /c"));
        assertEquals("a/ .. /b", IOUtil.simplifyPath("a/ .. /b"));

        // documented behavior unchanged
        assertEquals("b", IOUtil.simplifyPath("a/../b"));
        assertEquals(".", IOUtil.simplifyPath(""));
        assertEquals("a/b", IOUtil.simplifyPath("a//b/"));
    }

    @Test
    public void testNullStreamAndReaderSourcesDoNotTouchTargets() throws Exception {
        final File byteTarget = tempFolder.resolve("null_stream_target.txt").toFile();
        IOUtil.write("KEEP", byteTarget);
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write((InputStream) null, byteTarget));
        assertEquals("KEEP", IOUtil.readAllToString(byteTarget));

        final File charTarget = tempFolder.resolve("null_reader_target.txt").toFile();
        IOUtil.write("KEEP", charTarget);
        assertThrows(IllegalArgumentException.class, () -> IOUtil.write((Reader) null, charTarget));
        assertEquals("KEEP", IOUtil.readAllToString(charTarget));

        final File appendByteTarget = tempFolder.resolve("null_stream_append.txt").toFile();
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append((InputStream) null, appendByteTarget));
        assertFalse(appendByteTarget.exists());

        final File appendCharTarget = tempFolder.resolve("null_reader_append.txt").toFile();
        assertThrows(IllegalArgumentException.class, () -> IOUtil.append((Reader) null, appendCharTarget));
        assertFalse(appendCharTarget.exists());
    }

    @Test
    public void testFileFactoriesValidateBeforeOpeningAndUseDefaultForNullCharset() throws Exception {
        final File output = tempFolder.resolve("buffer_size_target.txt").toFile();
        IOUtil.write("KEEP", output);

        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedOutputStream(output, 0));
        assertEquals("KEEP", IOUtil.readAllToString(output));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedInputStream(output, 0));

        try (FileReader reader = IOUtil.newFileReader(output, null)) {
            assertEquals("KEEP", IOUtil.readAllToString(reader));
        }

        try (Writer writer = IOUtil.newFileWriter(output, null)) {
            writer.write("UTF-8");
        }
        assertEquals("UTF-8", IOUtil.readAllToString(output));

        try (java.io.BufferedReader reader = IOUtil.newBufferedReader(output.toPath(), null)) {
            assertEquals("UTF-8", reader.readLine());
        }
    }

    @Test
    public void testCreateParentDirectoriesAcceptsParentlessRelativeFile() throws Exception {
        final java.lang.reflect.Method method = IOUtil.class.getDeclaredMethod("createParentDirectories", File.class);
        method.setAccessible(true);

        assertEquals(Boolean.TRUE, method.invoke(null, new File("parentless-file.txt")));
    }

    @Test
    public void testPrimitiveWriteToWriter_charIsTextEveryOtherPrimitiveIsDecimal() throws Exception {
        // The family rule now documented on all nine overloads: the value's natural *text* form.
        assertEquals("A", writeToString(w -> IOUtil.write((char) 65, w)));
        assertEquals("65", writeToString(w -> IOUtil.write((byte) 65, w)));
        assertEquals("65", writeToString(w -> IOUtil.write((short) 65, w)));
        assertEquals("65", writeToString(w -> IOUtil.write(65, w)));
        assertEquals("65", writeToString(w -> IOUtil.write(65L, w)));
        assertEquals("true", writeToString(w -> IOUtil.write(true, w)));

        // Boxed values fall through to write(Object, Writer) and are rendered by toString().
        assertEquals("A", writeToString(w -> IOUtil.write(Character.valueOf('A'), w)));
        assertEquals("65", writeToString(w -> IOUtil.write(Byte.valueOf((byte) 65), w)));
        assertEquals("null", writeToString(w -> IOUtil.write((Object) null, w)));

        // Non-finite and signed-zero floats keep their Float/Double.toString rendering.
        assertEquals("NaN", writeToString(w -> IOUtil.write(Float.NaN, w)));
        assertEquals("Infinity", writeToString(w -> IOUtil.write(Float.POSITIVE_INFINITY, w)));
        assertEquals("-0.0", writeToString(w -> IOUtil.write(-0.0f, w)));
        assertEquals("NaN", writeToString(w -> IOUtil.write(Double.NaN, w)));
        assertEquals("-0.0", writeToString(w -> IOUtil.write(-0.0d, w)));
    }

    @Test
    public void testSimplifyPathKeepsWindowsDriveRoot() {
        assertEquals("C:/file.txt", IOUtil.simplifyPath("C:/../file.txt"));
        assertEquals("C:/", IOUtil.simplifyPath("C:/folder/../.."));
    }

    @Test
    public void testZipSkipsHardLinkAliasesOfArchiveInsideSourceDirectory() throws Exception {
        final Path sourceDir = Files.createDirectory(tempFolder.resolve("zip-source-with-alias"));
        Files.write(sourceDir.resolve("data.txt"), "data".getBytes(UTF_8));
        final Path archive = sourceDir.resolve("archive.zip");
        Files.write(archive, new byte[] { 1 });
        final Path alias = sourceDir.resolve("archive-alias.zip");
        Files.createLink(alias, archive);

        IOUtil.zip(sourceDir.toFile(), archive.toFile());

        try (java.util.zip.ZipFile zip = new java.util.zip.ZipFile(archive.toFile())) {
            final String root = sourceDir.getFileName() + "/";
            assertNotNull(zip.getEntry(root + "data.txt"));
            assertNull(zip.getEntry(root + "archive.zip"));
            assertNull(zip.getEntry(root + "archive-alias.zip"));
        }
    }

    @Test
    public void testUnzipRejectsAbsoluteEntriesAndSourceArchiveOverwrite() throws Exception {
        final File rootedArchive = tempFolder.resolve("rooted-entry.zip").toFile();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(new FileOutputStream(rootedArchive))) {
            zip.putNextEntry(new java.util.zip.ZipEntry("/outside/"));
            zip.closeEntry();
        }

        assertThrows(IOException.class, () -> IOUtil.unzip(rootedArchive, tempFolder.resolve("rooted-target").toFile()));

        final File backslashTraversalArchive = tempFolder.resolve("backslash-traversal.zip").toFile();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(new FileOutputStream(backslashTraversalArchive))) {
            zip.putNextEntry(new java.util.zip.ZipEntry("..\\outside.txt"));
            zip.write(1);
            zip.closeEntry();
        }

        assertThrows(IOException.class, () -> IOUtil.unzip(backslashTraversalArchive, tempFolder.resolve("backslash-traversal-target").toFile()));

        final Path targetDir = Files.createDirectory(tempFolder.resolve("archive-self-target"));
        final File selfArchive = targetDir.resolve("self.zip").toFile();
        try (java.util.zip.ZipOutputStream zip = new java.util.zip.ZipOutputStream(new FileOutputStream(selfArchive))) {
            zip.putNextEntry(new java.util.zip.ZipEntry("self.zip"));
            zip.write("replacement".getBytes(UTF_8));
            zip.closeEntry();
        }

        final byte[] originalArchive = Files.readAllBytes(selfArchive.toPath());
        assertThrows(IOException.class, () -> IOUtil.unzip(selfArchive, targetDir.toFile()));
        assertArrayEquals(originalArchive, Files.readAllBytes(selfArchive.toPath()));
    }

    @Test
    public void testCompressedFileExtensionIsCaseInsensitive() throws Exception {
        final File gzip = tempFolder.resolve("text.GZ").toFile();
        try (java.util.zip.GZIPOutputStream output = new java.util.zip.GZIPOutputStream(new FileOutputStream(gzip))) {
            output.write("hello".getBytes(UTF_8));
        }

        assertEquals("hello", IOUtil.readAllToString(gzip));
    }

    @Test
    public void testMutatingOperationsStayChecked() throws Exception {
        // Inverse pairs must agree, so a round trip needs one catch rather than two shapes.
        final File missing = new File(tempFolder.toFile(), "no-such-source.bin");
        final File target = new File(tempFolder.toFile(), "round-trip-target.zip");

        assertThrows(IOException.class, () -> IOUtil.zip(missing, target));
        assertThrows(IOException.class, () -> IOUtil.unzip(missing, tempFolder.toFile()));
        assertThrows(IOException.class, () -> IOUtil.merge(new File[] { missing }, target));
        assertThrows(IOException.class, () -> IOUtil.split(missing, 2, tempFolder.toFile()));
        assertThrows(IOException.class, () -> IOUtil.splitBySize(missing, 2, tempFolder.toFile()));
    }

    @Test
    public void testLineWritersAndAppendersHonourAnExplicitCharset() throws IOException {
        // writeLine/writeLines gained the Charset overloads that appendLine/appendLines already had, so the two
        // families are now mirror images. Latin-1 makes the charset observable: "é" is one byte there and two in
        // UTF-8.
        final java.nio.charset.Charset latin1 = java.nio.charset.StandardCharsets.ISO_8859_1;
        final byte[] cafeLatin1 = { 'c', 'a', 'f', (byte) 0xE9, '\n' };

        final File file = new File(tempFolder.toFile(), "charset-writers.txt");

        IOUtil.writeLine("café", latin1, file);
        assertArrayEquals(cafeLatin1, IOUtil.readAllBytes(file));

        // Each write REPLACES, so the file holds only the latest content - never appended to.
        IOUtil.writeLines(CommonUtil.asList("café"), latin1, file);
        assertArrayEquals(cafeLatin1, IOUtil.readAllBytes(file));

        IOUtil.writeLines(CommonUtil.asList("café").iterator(), latin1, file);
        assertArrayEquals(cafeLatin1, IOUtil.readAllBytes(file));

        // ...and an empty source still truncates, matching every other write to a File.
        IOUtil.writeLines(CommonUtil.<String> emptyList(), latin1, file);
        assertEquals(0, file.length());

        // appendLines(Iterator, ..) is the new mirror of writeLines(Iterator, ..): it never truncates.
        final File log = new File(tempFolder.toFile(), "charset-appends.txt");
        IOUtil.appendLines(CommonUtil.asList("a", "b").iterator(), log);
        IOUtil.appendLines(CommonUtil.asList("café").iterator(), latin1, log);

        final byte[] expected = { 'a', '\n', 'b', '\n', 'c', 'a', 'f', (byte) 0xE9, '\n' };
        assertArrayEquals(expected, IOUtil.readAllBytes(log));

        // An empty or null iterator appends nothing but still creates a missing file - and never truncates.
        // The cast is required: appendLines(null, ..) is ambiguous now that both an Iterable and an Iterator
        // overload exist at this arity, exactly as writeLines(null, ..) already was.
        IOUtil.appendLines(CommonUtil.<String> emptyList().iterator(), log);
        IOUtil.appendLines((Iterator<String>) null, latin1, log);
        assertArrayEquals(expected, IOUtil.readAllBytes(log));

        final File created = new File(tempFolder.toFile(), "created-by-append/nested.txt");
        IOUtil.appendLines(CommonUtil.<String> emptyList().iterator(), created);
        assertTrue(created.exists());
        assertEquals(0, created.length());

        // A null charset means the default, not a NullPointerException.
        IOUtil.writeLine("hi", null, file);
        assertEquals("hi\n", IOUtil.readAllToString(file));
    }

    @Test
    public void testMapRejectsADirectoryAsABadArgument() throws IOException {
        // The class contract splits the two failures: a path that does not exist is FileNotFoundException
        // (wrapped, since map is unchecked), while a path that exists but is the WRONG KIND is
        // IllegalArgumentException. Left to RandomAccessFile a directory surfaced as an UncheckedIOException
        // whose text is platform-dependent - "Access is denied" on Windows, "Is a directory" on Unix.
        final File directory = Files.createDirectory(tempFolder.resolve("map-a-directory")).toFile();

        assertThrows(IllegalArgumentException.class, () -> IOUtil.map(directory));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.map(directory, java.nio.channels.FileChannel.MapMode.READ_ONLY));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.map(directory, java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, 4));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.map(directory, java.nio.channels.FileChannel.MapMode.READ_WRITE, 0, 4));

        // The not-found case is unchanged, and a real file still maps.
        assertThrows(UncheckedIOException.class, () -> IOUtil.map(new File(tempFolder.toFile(), "no-such-file.bin")));

        final File file = new File(tempFolder.toFile(), "mappable.bin");
        IOUtil.write("abcd", file);

        final java.nio.MappedByteBuffer buffer = IOUtil.map(file);
        assertEquals(4, buffer.remaining());
        // A live mapping keeps a Windows file lock, which would break @TempDir cleanup.
        unmap(buffer);

        // MapMode has THREE values, not two. PRIVATE is copy-on-write and still needs write access, so it opens
        // the file exactly as READ_WRITE does - which means the whole-file overloads must refuse to create a
        // missing file for it, while the explicitly-sized one creates it.
        final File missingPrivate = new File(tempFolder.toFile(), "private-missing.bin");
        assertThrows(UncheckedIOException.class, () -> IOUtil.map(missingPrivate, java.nio.channels.FileChannel.MapMode.PRIVATE));
        assertFalse(missingPrivate.exists(), "the whole-file overloads never create the file");
        assertThrows(IllegalArgumentException.class, () -> IOUtil.map(directory, java.nio.channels.FileChannel.MapMode.PRIVATE));

        final File createdByPrivate = new File(tempFolder.toFile(), "private-created.bin");
        unmap(IOUtil.map(createdByPrivate, java.nio.channels.FileChannel.MapMode.PRIVATE, 0, 8));
        assertTrue(createdByPrivate.exists());
        assertEquals(8, createdByPrivate.length());
    }

    @Test
    public void testMkdirIfNotExistsRejectsNull() throws IOException {
        // A null path is a programming error and is reported as IllegalArgumentException, like every other
        // null-path argument in this class - not as a bare NullPointerException from File.isDirectory().
        assertThrows(IllegalArgumentException.class, () -> IOUtil.mkdirIfNotExists(null));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.mkdirsIfNotExists(null));

        final File dir = new File(tempFolder.toFile(), "made-once");
        assertTrue(IOUtil.mkdirIfNotExists(dir));
        assertTrue(dir.isDirectory());
        assertFalse(IOUtil.mkdirIfNotExists(dir), "already existing is reported as false, not as an error");

        final File nested = new File(tempFolder.toFile(), "made/deeply/nested");
        assertTrue(IOUtil.mkdirsIfNotExists(nested));
        assertTrue(nested.isDirectory());
    }

    @Test
    public void testTransferHonoursChannelPositionsAndFallsBackForNonFileSources() throws IOException {
        // transfer() gained a FileChannel-to-FileChannel fast path built on transferFrom(..). These are the
        // scenarios that path can get wrong and the existing whole-file tests cannot see.
        final File source = new File(tempFolder.toFile(), "transfer-src.bin");
        Files.write(source.toPath(), "ABCDEFGH".getBytes(UTF_8));

        // 1. A source positioned mid-stream must transfer only the remainder, not the whole file.
        final File midDest = new File(tempFolder.toFile(), "transfer-mid.bin");

        try (FileInputStream fis = new FileInputStream(source);
             FileOutputStream fos = new FileOutputStream(midDest)) {
            fis.getChannel().position(3);
            assertEquals(5, IOUtil.transfer(fis.getChannel(), fos.getChannel()));
        }

        assertEquals("DEFGH", IOUtil.readAllToString(midDest));

        // 2. A destination positioned mid-stream must write AT that position and leave it after the last byte.
        final File posDest = new File(tempFolder.toFile(), "transfer-pos.bin");
        Files.write(posDest.toPath(), "0123456789".getBytes(UTF_8));

        try (FileInputStream fis = new FileInputStream(source);
             java.nio.channels.FileChannel out = java.nio.channels.FileChannel.open(posDest.toPath(), java.nio.file.StandardOpenOption.WRITE)) {
            out.position(1);
            assertEquals(8, IOUtil.transfer(fis.getChannel(), out));
            assertEquals(9, out.position());
        }

        assertEquals("0ABCDEFGH9", IOUtil.readAllToString(posDest));

        // 3. An APPEND-mode destination must still append rather than overwrite - transferFrom takes an absolute
        //    position, so getting this wrong would silently clobber the existing content.
        final File appendDest = new File(tempFolder.toFile(), "transfer-append.bin");
        Files.write(appendDest.toPath(), "EXISTING:".getBytes(UTF_8));

        try (FileInputStream fis = new FileInputStream(source);
             java.nio.channels.FileChannel out = java.nio.channels.FileChannel.open(appendDest.toPath(), java.nio.file.StandardOpenOption.WRITE,
                     java.nio.file.StandardOpenOption.APPEND)) {
            assertEquals(8, IOUtil.transfer(fis.getChannel(), out));
        }

        assertEquals("EXISTING:ABCDEFGH", IOUtil.readAllToString(appendDest));

        // 4. A non-file source takes the buffered fallback: a zero-byte transferFrom cannot be told apart from
        //    end-of-input there, so the fast path is deliberately not used.
        final File fallbackDest = new File(tempFolder.toFile(), "transfer-fallback.bin");

        try (InputStream in = new ByteArrayInputStream("from-a-pipe".getBytes(UTF_8));
             FileOutputStream fos = new FileOutputStream(fallbackDest)) {
            assertEquals(11, IOUtil.transfer(java.nio.channels.Channels.newChannel(in), fos.getChannel()));
        }

        assertEquals("from-a-pipe", IOUtil.readAllToString(fallbackDest));
    }

    @Test
    public void testNullSourceCollectionNeverTouchesTheDestination() throws IOException {
        // Array.asList(null) yields an EMPTY list, so merge((File[]) null, dest) used to mean "merge zero files"
        // - which truncates dest. A null must be a bad argument, and must be rejected before anything opens the
        // destination.
        final File dest = new File(tempFolder.toFile(), "null-source-dest.txt");
        Files.write(dest.toPath(), "PRE-EXISTING".getBytes(UTF_8));

        assertThrows(IllegalArgumentException.class, () -> IOUtil.merge((File[]) null, dest));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.merge((java.util.Collection<File>) null, dest));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.merge(null, new byte[] { ',' }, dest));
        assertEquals("PRE-EXISTING", IOUtil.readAllToString(dest));

        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip((java.util.Collection<File>) null, dest));
        assertEquals("PRE-EXISTING", IOUtil.readAllToString(dest));

        // An EMPTY collection is still a real request to replace the destination with nothing.
        assertEquals(0, IOUtil.merge(java.util.Collections.<File> emptyList(), dest));
        assertTrue(dest.exists());
        assertEquals(0, dest.length());
    }

    @Test
    public void testZipMergeRoundTripNeedsOneCatch() throws IOException {
        // Compiles only because zip/unzip/split/merge all declare the same checked IOException.
        final File src = new File(tempFolder.toFile(), "round-trip.txt");
        Files.write(src.toPath(), "round-trip-content".getBytes(UTF_8));

        final File archive = new File(tempFolder.toFile(), "round-trip.zip");
        final File extractDir = new File(tempFolder.toFile(), "round-trip-out");

        IOUtil.zip(src, archive);
        IOUtil.unzip(archive, extractDir);

        assertEquals("round-trip-content", IOUtil.readAllToString(new File(extractDir, "round-trip.txt")));

        final File partsDir = new File(tempFolder.toFile(), "round-trip-parts");
        IOUtil.split(src, 3, partsDir);
        final File[] parts = partsDir.listFiles();
        assertNotNull(parts);
        Arrays.sort(parts);

        final File merged = new File(tempFolder.toFile(), "round-trip-merged.txt");
        IOUtil.merge(parts, merged);
        assertEquals("round-trip-content", IOUtil.readAllToString(merged));
    }

    @Test
    public void testRenameTo_RejectsPathElements() throws Exception {
        File srcFile = Files.createTempFile(tempFolder, "rename_path", ".txt").toFile();

        assertThrows(IllegalArgumentException.class, () -> IOUtil.renameTo(srcFile, "subdir/name.txt"));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.renameTo(srcFile, "subdir\\name.txt"));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.renameTo(srcFile, ".."));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.renameTo(srcFile, "."));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.renameTo(srcFile, ""));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.renameTo(srcFile, null));
        assertTrue(srcFile.exists());
    }

    @Test
    public void testZip_DuplicateBasenameRejectedBeforeTruncate() throws Exception {
        File dir1 = tempFolder.resolve("zip-dup-1").toFile();
        File dir2 = tempFolder.resolve("zip-dup-2").toFile();
        assertTrue(dir1.mkdirs());
        assertTrue(dir2.mkdirs());
        File a1 = new File(dir1, "same.txt");
        File a2 = new File(dir2, "same.txt");
        Files.write(a1.toPath(), "one".getBytes(UTF_8));
        Files.write(a2.toPath(), "two".getBytes(UTF_8));

        File zipFile = Files.createTempFile(tempFolder, "dup-archive", ".zip").toFile();
        Files.write(zipFile.toPath(), "precious existing content".getBytes(UTF_8));

        assertThrows(IllegalArgumentException.class, () -> IOUtil.zip(java.util.Arrays.asList(a1, a2), zipFile));
        assertEquals("precious existing content", new String(Files.readAllBytes(zipFile.toPath()), UTF_8));
    }

    @Test
    public void testZip_DirectorySourceToAbsentTarget() throws Exception {
        final File sourceDir = tempFolder.resolve("zip-absent-target-src").toFile();
        assertTrue(new File(sourceDir, "nested").mkdirs());
        Files.write(new File(sourceDir, "a.txt").toPath(), "A".getBytes(UTF_8));
        Files.write(new File(sourceDir, "nested/b.txt").toPath(), "B".getBytes(UTF_8));

        final File zipFile = tempFolder.resolve("zip-absent-target.zip").toFile();
        assertFalse(zipFile.exists());

        IOUtil.zip(Arrays.asList(sourceDir), zipFile);

        assertTrue(zipFile.exists());

        final File extracted = tempFolder.resolve("zip-absent-target-out").toFile();
        IOUtil.unzip(zipFile, extracted);

        final File root = new File(extracted, sourceDir.getName());
        assertEquals("A", IOUtil.readAllToString(new File(root, "a.txt")));
        assertEquals("B", IOUtil.readAllToString(new File(root, "nested/b.txt")));
    }

    @Test
    public void testZip_TargetInsideSourceDirectoryIsNotArchived() throws Exception {
        final File sourceDir = tempFolder.resolve("zip-target-inside").toFile();
        assertTrue(sourceDir.mkdirs());
        Files.write(new File(sourceDir, "a.txt").toPath(), "A".getBytes(UTF_8));

        final File zipFile = new File(sourceDir, "inside.zip");
        IOUtil.zip(Arrays.asList(sourceDir), zipFile);

        final File extracted = tempFolder.resolve("zip-target-inside-out").toFile();
        IOUtil.unzip(zipFile, extracted);

        final File root = new File(extracted, sourceDir.getName());
        assertTrue(new File(root, "a.txt").exists());
        assertFalse(new File(root, "inside.zip").exists(), "the archive must not contain itself");
    }

    @Test
    public void testMap_ReturnsUsableBufferAfterFileHandleClosed() throws IOException {
        // Deliberately NOT under @TempDir: on Windows a MappedByteBuffer keeps the file locked until the
        // mapping is released by the garbage collector, which would break the temp-directory cleanup.
        final File file = File.createTempFile("map-src", ".bin");
        file.deleteOnExit();

        try {
            Files.write(file.toPath(), "mapped-content".getBytes(UTF_8));

            final MappedByteBuffer buffer = IOUtil.map(file);
            final byte[] read = new byte[(int) file.length()];
            buffer.get(read);

            assertEquals("mapped-content", new String(read, UTF_8));
        } finally {
            file.delete(); // best effort: fails while the mapping is still held, hence deleteOnExit above
        }
    }

    @Test
    public void testOpenFile_CorruptGzipReleasesTheFileHandle() throws IOException {
        // The gzip branch of openFile opens the FileInputStream and only then constructs the GZIPInputStream.
        // When that construction fails the stream must still be closed, or the handle leaks.
        final File corrupt = tempFolder.resolve("corrupt.gz").toFile();
        Files.write(corrupt.toPath(), "not actually gzip".getBytes(UTF_8));

        assertThrows(UncheckedIOException.class, () -> IOUtil.readAllToString(corrupt));

        // On Windows an open handle prevents deletion, so this asserts the handle was released.
        assertTrue(corrupt.delete(), "the file handle must be released when the gzip stream cannot be built");
    }

    @Test
    public void testCharsToBytes_outOfRangeThrowsIndexOutOfBounds() {
        final char[] chars = new char[] { 'a', 'b', 'c' };
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.charsToBytes(chars, 5, 1, UTF_8));
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.charsToBytes(chars, 2, 5, UTF_8));
    }

    @Test
    public void testBytesToChars_outOfRangeThrowsIndexOutOfBounds() {
        final byte[] bytes = new byte[] { 1, 2, 3 };
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.bytesToChars(bytes, 5, 1, UTF_8));
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.bytesToChars(bytes, 1, 10, UTF_8));
    }

    @Test
    public void testMaxMemoryInMb_isPositive() {
        assertTrue(IOUtil.MAX_MEMORY_IN_MB > 0, "MAX_MEMORY_IN_MB must never be negative: " + IOUtil.MAX_MEMORY_IN_MB);
    }

    @Test
    public void testSimplifyPath_preservesUncPrefix() {
        assertEquals("//host/share/x", IOUtil.simplifyPath("//host/share/./x"));
        assertEquals("//host/share/x", IOUtil.simplifyPath("//host/share/y/../x"));
        assertEquals("//host/share", IOUtil.simplifyPath("//host/share/"));
        assertEquals("//host/share", IOUtil.simplifyPath("\\\\host\\share"));
        // A UNC root cannot be ascended above.
        assertEquals("//host/share", IOUtil.simplifyPath("//host/share/../.."));
    }

    @Test
    public void testSimplifyPath_threeOrMoreLeadingSlashesAreNotUnc() {
        assertEquals("/a", IOUtil.simplifyPath("///a"));
        assertEquals("/a", IOUtil.simplifyPath("////a"));
        // A bare "//" names no host, so it is not a UNC root and collapses like any other run of separators.
        assertEquals("/", IOUtil.simplifyPath("//"));
        assertEquals("/", IOUtil.simplifyPath("/"));
        // ...but two slashes followed by a host are kept.
        assertEquals("//a", IOUtil.simplifyPath("//a"));
    }

    @Test
    public void testSimplifyPath_absoluteAndWindowsRootsAreStable() {
        assertEquals("/a/b/c", IOUtil.simplifyPath("/a/./b/./c/"));
        assertEquals("/", IOUtil.simplifyPath("/.."));
        assertEquals("/a", IOUtil.simplifyPath("/../a"));
        assertEquals("C:/b", IOUtil.simplifyPath("C:/a/../../b"));
        assertEquals("C:/", IOUtil.simplifyPath("C:/"));
        assertEquals("../a", IOUtil.simplifyPath("../a"));
        assertEquals(".", IOUtil.simplifyPath(""));
    }

    @Test
    public void testErrorMessagesUseAbsolutePaths() throws Exception {
        final File relative = new File("no-such-relative-file.txt");

        // checkFileExists / checkDirectoryExists / checkDestDirectory all report the absolute path, so a
        // failure on a relative File still says which file on disk was meant.
        final Exception missingFile = assertThrows(Exception.class, () -> IOUtil.sizeOf(relative));
        assertTrue(missingFile.getMessage().contains(relative.getAbsolutePath()),
                "message should identify the file by absolute path but was: " + missingFile.getMessage());

        final Exception missingDir = assertThrows(Exception.class, () -> IOUtil.sizeOfDirectory(relative));
        assertTrue(missingDir.getMessage().contains(relative.getAbsolutePath()),
                "message should identify the directory by absolute path but was: " + missingDir.getMessage());

        final File existingFile = Files.createTempFile(tempFolder, "not-a-dir", ".txt").toFile();
        final Exception notADir = assertThrows(IllegalArgumentException.class, () -> IOUtil.copyToDirectory(existingFile, existingFile));
        assertTrue(notADir.getMessage().contains(existingFile.getAbsolutePath()),
                "message should identify the destination by absolute path but was: " + notADir.getMessage());

        // A null File is rendered as "null" rather than dereferenced.
        final Exception nullFile = assertThrows(Exception.class, () -> IOUtil.sizeOf(null));
        assertTrue(nullFile.getMessage().contains("null"), "message should render a null file as \"null\" but was: " + nullFile.getMessage());
    }

    /**
     * C-002: {@code File.listFiles()} answers {@code null} on an I/O error, which is not an empty directory.
     * {@code copyDirectory} used to route the listing through {@code listFiles(File)}, whose walk semantics fold
     * that into "no entries", so it reported a successful copy of a source it had not read - the failure mode a
     * caller of the documented copy-then-delete idiom loses data to. {@code doCopyDirectory} always guarded it.
     */
    @Test
    public void testCopyDirectory_RejectsAnUnlistableSourceInsteadOfCopyingNothing() throws Exception {
        final Path realSource = tempFolder.resolve("c002-src");
        Files.createDirectories(realSource);
        Files.writeString(realSource.resolve("a.txt"), "payload");

        final File unlistable = new File(realSource.toFile().getAbsolutePath()) {
            private static final long serialVersionUID = 1L;

            @Override
            public File[] listFiles() {
                return null; // what the JDK returns when the directory cannot be read
            }
        };

        final File destination = tempFolder.resolve("c002-dest").toFile();

        final IOException e = assertThrows(IOException.class, () -> IOUtil.copyDirectory(unlistable, destination));
        assertTrue(e.getMessage().contains("Failed to list contents of"), "unexpected message: " + e.getMessage());

        // and the ordinary path is untouched
        final File destination2 = tempFolder.resolve("c002-dest2").toFile();
        IOUtil.copyDirectory(realSource.toFile(), destination2);
        assertEquals("payload", IOUtil.readAllToString(new File(destination2, "a.txt")));
    }

    /**
     * C-005: a {@code null} {@code InputStream} used to reach {@code newInputStreamReader}, which reports the
     * internal parameter name {@code 'is'}, and only after the offset/count checks - while the byte readers and
     * every {@code Reader} overload report {@code 'source'} first. All three families now agree.
     */
    @Test
    public void testRead_NullInputStreamIsReportedAsSource() {
        for (final org.junit.jupiter.api.function.Executable call : new org.junit.jupiter.api.function.Executable[] {
                () -> IOUtil.readAllChars((InputStream) null), //
                () -> IOUtil.readAllLines((InputStream) null), //
                () -> IOUtil.readChars((InputStream) null, 0, 8), //
                () -> IOUtil.readToString((InputStream) null, 0, 8), //
                () -> IOUtil.readLines((InputStream) null, 0, 8) }) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
            assertTrue(e.getMessage().contains("source"), "should name the caller's argument, was: " + e.getMessage());
        }

        // the stream is checked BEFORE offset/count, exactly as readBytes(InputStream, long, int) does
        for (final org.junit.jupiter.api.function.Executable call : new org.junit.jupiter.api.function.Executable[] {
                () -> IOUtil.readChars((InputStream) null, -1, 8), //
                () -> IOUtil.readToString((InputStream) null, -1, 8), //
                () -> IOUtil.readLines((InputStream) null, -1, 8) }) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
            assertTrue(e.getMessage().contains("source"), "the source must be reported first, was: " + e.getMessage());
        }

        // a negative offset is still reported when the stream itself is fine
        final IllegalArgumentException offsetFailure = assertThrows(IllegalArgumentException.class,
                () -> IOUtil.readChars(new ByteArrayInputStream(new byte[0]), -1, 8));
        assertTrue(offsetFailure.getMessage().contains("offset"), offsetFailure.getMessage());
    }

    /**
     * C-001: the deprecated {@code move(File, File, CopyOption...)} javadoc claimed {@code IllegalArgumentException}
     * for a missing source; the code has always thrown {@code FileNotFoundException}, like its two-argument twin
     * and like {@code moveToDirectory}. The javadoc was corrected, and this pins the behaviour it now describes.
     */
    @Test
    public void testMove_MissingSourceIsFileNotFound() throws Exception {
        final File missing = tempFolder.resolve("c001-missing.txt").toFile();
        final File destination = tempFolder.resolve("c001-dest").toFile();

        assertThrows(java.io.FileNotFoundException.class, () -> IOUtil.move(missing, destination));
        assertThrows(java.io.FileNotFoundException.class, () -> IOUtil.move(missing, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING));
    }

    /**
     * C-004: {@code appendLine}/{@code appendLines} accept a {@code null} charset and resolve it to UTF-8, which
     * their javadoc did not say (every {@code writeLine}/{@code writeLines} sibling does). Pins the behaviour the
     * corrected javadoc now documents.
     */
    @Test
    public void testAppend_NullCharsetResolvesToUtf8() throws Exception {
        final File target = tempFolder.resolve("c004-append.txt").toFile();

        IOUtil.appendLine("premier", (Charset) null, target);
        IOUtil.appendLines(CommonUtil.asList("deuxieme", "troisieme"), (Charset) null, target);

        assertEquals(CommonUtil.asList("premier", "deuxieme", "troisieme"), IOUtil.readAllLines(target, StandardCharsets.UTF_8));
    }

    /**
     * C-031: {@code close(AutoCloseable, Consumer)} was the only member of the close family that let an
     * {@code InterruptedException} pass to the handler without restoring the thread's interrupt status.
     */
    @Test
    public void testClose_WithHandlerRestoresTheInterruptStatus() {
        Thread.interrupted(); // clear

        // Each assertion below leaves an interrupt pending on the way in and clears it as a side effect of
        // Thread.interrupted(). That is only true when they all pass: a failure part-way through would leave
        // the flag set on a thread surefire reuses, so one real failure would be followed by unrelated ones.
        try {
            final java.util.concurrent.atomic.AtomicReference<Exception> seen = new java.util.concurrent.atomic.AtomicReference<>();
            IOUtil.close(() -> {
                throw new InterruptedException();
            }, seen::set);

            assertTrue(Thread.interrupted(), "close(closeable, handler) must not swallow the interrupt");
            assertNotNull(seen.get());
            assertTrue(seen.get() instanceof InterruptedException);

            // the siblings it now matches
            Thread.interrupted();
            IOUtil.closeQuietly(() -> {
                throw new InterruptedException();
            });
            assertTrue(Thread.interrupted());

            Thread.interrupted();
            assertThrows(RuntimeException.class, () -> IOUtil.closeAll(CommonUtil.asList((AutoCloseable) () -> {
                throw new InterruptedException();
            })));
            assertTrue(Thread.interrupted());
        } finally {
            Thread.interrupted();
        }
    }

    /**
     * C-032: {@code write(char[], offset, count, [Charset,] File)} judged the slice before looking at the
     * destination, so a call that was wrong twice over answered {@code IndexOutOfBoundsException} where its
     * {@code byte[]} twin and its {@code append} mirror answered {@code IllegalArgumentException}.
     */
    @Test
    public void testWrite_CharArrayValidatesTheDestinationFirst() {
        for (final org.junit.jupiter.api.function.Executable call : new org.junit.jupiter.api.function.Executable[] {
                () -> IOUtil.write(new char[] { 'a' }, 0, 5, (File) null), //
                () -> IOUtil.write(new char[] { 'a' }, 0, 5, StandardCharsets.UTF_8, (File) null), //
                () -> IOUtil.write(new char[] { 'a' }, -1, 1, (File) null), //
                () -> IOUtil.write(new byte[] { 1 }, 0, 5, (File) null), //
                () -> IOUtil.append(new char[] { 'a' }, 0, 5, (File) null) }) {
            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, call);
            assertTrue(e.getMessage().contains("output") || e.getMessage().contains("targetFile"), e.getMessage());
        }

        // a bad slice with a valid destination is still an IndexOutOfBoundsException
        assertThrows(IndexOutOfBoundsException.class, () -> IOUtil.write(new char[] { 'a' }, 0, 5, tempFolder.resolve("c032.txt").toFile()));
    }

    /**
     * C-033: the {@code String}-taking and size-taking {@code newXxx} factories reject a directory with
     * {@code IllegalArgumentException} exactly as their {@code File} twins do; only the twins said so.
     */
    @Test
    public void testNewFileInputStream_RejectsADirectory() {
        final File dir = tempFolder.toFile();

        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileInputStream(dir.getAbsolutePath()));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileOutputStream(dir.getAbsolutePath()));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newFileOutputStream(dir.getAbsolutePath(), true));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedInputStream(dir, 1024));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newBufferedOutputStream(dir, 1024));
    }

    /**
     * C-003: when neither move can replace the destination (the documented Windows case - a reader holds it
     * without {@code FILE_SHARE_DELETE}), {@code replaceWith} writes the downloaded content over the
     * destination in place. That write succeeding means the destination holds the whole download, so the
     * operation succeeded; the trailing removal of the {@code .part} sibling used to be unguarded, and a
     * failure there propagated out as an {@code IOException} describing a transfer that had in fact worked.
     * It is now logged instead.
     *
     * <p>The scenario is reproduced with a live memory mapping, which on Windows blocks both {@code rename}
     * and {@code delete} while allowing reads - so both moves fail, the in-place write succeeds, and the
     * cleanup fails. The method is reached by reflection because it is private and the public
     * {@code copyURLToFile} cannot be steered onto this branch: the {@code .part} name it generates is a
     * fresh random hex string that a test cannot lock in advance.</p>
     */
    @Test
    public void testCopyURLToFile_ReplaceWithReportsSuccessWhenOnlyTempCleanupFails() throws Exception {
        // Deliberately NOT under @TempDir: on Windows a live MappedByteBuffer keeps the file locked, which
        // would break the temp-directory cleanup. The mapping is released explicitly in the finally below -
        // deleteOnExit could not do it, because its shutdown hook runs while this JVM still holds the mapping.
        final File part = File.createTempFile("c003-download", ".part");
        final File target = File.createTempFile("c003-download", ".txt");
        MappedByteBuffer mapping = null;

        try {
            IOUtil.write("NEW CONTENT", part);
            IOUtil.write("OLD CONTENT", target);

            mapping = IOUtil.map(part);
            assertNotNull(mapping);

            // Only a platform where a live mapping locks the file can reach the in-place fallback at all;
            // where it does not, both moves succeed and there is nothing to test.
            assumeTrue(aLiveMappingLocksFiles(), "a live memory mapping does not lock files on this platform");

            final java.lang.reflect.Method replaceWith = IOUtil.class.getDeclaredMethod("replaceWith", File.class, File.class);
            replaceWith.setAccessible(true);

            // must NOT throw: the destination already holds the whole download
            replaceWith.invoke(null, part, target);

            assertEquals("NEW CONTENT", IOUtil.readAllToString(target));
            assertTrue(part.exists(), "the temporary sibling survives - its removal failed and was only logged");
        } finally {
            // Unmapping is what releases the lock, and the call also keeps `mapping` reachable through the
            // scenario above: a buffer whose last use is assertNotNull is collectable from that point on, and
            // a cleaner running early would unlock the file and defeat the whole test.
            unmap(mapping);
            part.delete();
            target.delete();
        }
    }
}
