package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

public class IOUtilContentTest extends IOUtilTestSupport {
    @Test
    public void testContentEqualsRejectsIdenticalDirectory() throws IOException {
        File directory = Files.createDirectory(tempFolder.resolve("content-directory")).toFile();

        assertThrows(IllegalArgumentException.class, () -> IOUtil.contentEquals(directory, directory));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.contentEqualsIgnoreEOL(directory, directory, "UTF-8"));
    }

    @Test
    public void testContentEquals_Files_Identical() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "equal1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "equal2", ".txt").toFile();

        String content = "Identical content";
        Files.write(file1.toPath(), content.getBytes(UTF_8));
        Files.write(file2.toPath(), content.getBytes(UTF_8));

        boolean result = IOUtil.contentEquals(file1, file2);

        assertTrue(result);
    }

    @Test
    public void testContentEquals_Files_Different() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "diff1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "diff2", ".txt").toFile();

        Files.write(file1.toPath(), "Content 1".getBytes(UTF_8));
        Files.write(file2.toPath(), "Content 2".getBytes(UTF_8));

        boolean result = IOUtil.contentEquals(file1, file2);

        assertTrue(!result);
    }

    @Test
    public void testContentEquals_Files_BothEmpty() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "empty-eq1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "empty-eq2", ".txt").toFile();

        boolean result = IOUtil.contentEquals(file1, file2);

        assertTrue(result);
    }

    @Test
    public void testContentEquals_Files_SameFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "same", ".txt").toFile();
        Files.write(file.toPath(), "Content".getBytes(UTF_8));

        boolean result = IOUtil.contentEquals(file, file);

        assertTrue(result);
    }

    @Test
    public void testContentEquals_InputStreams_Identical() throws Exception {
        String content = "Stream content";
        InputStream is1 = new ByteArrayInputStream(content.getBytes(UTF_8));
        InputStream is2 = new ByteArrayInputStream(content.getBytes(UTF_8));

        boolean result = IOUtil.contentEquals(is1, is2);

        assertTrue(result);
    }

    @Test
    public void testContentEquals_InputStreams_Different() throws Exception {
        InputStream is1 = new ByteArrayInputStream("Content 1".getBytes(UTF_8));
        InputStream is2 = new ByteArrayInputStream("Content 2".getBytes(UTF_8));

        boolean result = IOUtil.contentEquals(is1, is2);

        assertTrue(!result);
    }

    @Test
    public void testContentEquals_InputStreams_Empty() throws Exception {
        InputStream is1 = new ByteArrayInputStream(new byte[0]);
        InputStream is2 = new ByteArrayInputStream(new byte[0]);

        boolean result = IOUtil.contentEquals(is1, is2);

        assertTrue(result);
    }

    @Test
    public void testContentEquals_Readers_Identical() throws Exception {
        String content = "Reader content";
        Reader r1 = new StringReader(content);
        Reader r2 = new StringReader(content);

        boolean result = IOUtil.contentEquals(r1, r2);

        assertTrue(result);
    }

    @Test
    public void testContentEquals_Readers_Different() throws Exception {
        Reader r1 = new StringReader("Content 1");
        Reader r2 = new StringReader("Content 2");

        boolean result = IOUtil.contentEquals(r1, r2);

        assertTrue(!result);
    }

    @Test
    public void testContentEquals_Readers_Empty() throws Exception {
        Reader r1 = new StringReader("");
        Reader r2 = new StringReader("");

        boolean result = IOUtil.contentEquals(r1, r2);

        assertTrue(result);
    }

    @Test
    public void testContentEquals_LargeFiles() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "large-eq1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "large-eq2", ".txt").toFile();

        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            content.append("Line ").append(i).append("\n");
        }

        Files.write(file1.toPath(), content.toString().getBytes(UTF_8));
        Files.write(file2.toPath(), content.toString().getBytes(UTF_8));

        boolean result = IOUtil.contentEquals(file1, file2);

        assertTrue(result);
    }

    @Test
    public void testContentEquals_Null() throws Exception {
        assertTrue(IOUtil.contentEquals((File) null, (File) null));
        assertTrue(IOUtil.contentEquals((InputStream) null, (InputStream) null));
        assertTrue(IOUtil.contentEquals((Reader) null, (Reader) null));
    }

    @Test
    public void testContentEquals_OneNull() throws Exception {
        assertFalse(IOUtil.contentEquals(tempFile, null));
        assertFalse(IOUtil.contentEquals(null, tempFile));

        try (ByteArrayInputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            assertFalse(IOUtil.contentEquals(is, null));
            assertFalse(IOUtil.contentEquals(null, is));
        }
        assertFalse(IOUtil.contentEquals(null, new ByteArrayInputStream(new byte[0])));

        try (Reader reader = new StringReader(TEST_CONTENT)) {
            assertFalse(IOUtil.contentEquals(reader, null));
            assertFalse(IOUtil.contentEquals(null, reader));
        }
        assertFalse(IOUtil.contentEquals(null, new StringReader("")));
    }

    @Test
    public void testContentEquals_SameReference() throws IOException {
        try (ByteArrayInputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8))) {
            assertTrue(IOUtil.contentEquals(is, is));
        }
        try (Reader reader = new StringReader(TEST_CONTENT)) {
            assertTrue(IOUtil.contentEquals(reader, reader));
        }
    }

    @Test
    public void testContentEqualsIgnoreEOL_Files_Identical() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "eol1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "eol2", ".txt").toFile();

        Files.write(file1.toPath(), "Line1\nLine2\n".getBytes(UTF_8));
        Files.write(file2.toPath(), "Line1\nLine2\n".getBytes(UTF_8));

        boolean result = IOUtil.contentEqualsIgnoreEOL(file1, file2, UTF_8.name());

        assertTrue(result);
    }

    @Test
    public void testContentEqualsIgnoreEOL_Files_DifferentEOL() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "eol-unix", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "eol-win", ".txt").toFile();

        Files.write(file1.toPath(), "Line1\nLine2\n".getBytes(UTF_8));
        Files.write(file2.toPath(), "Line1\nLine2\n".getBytes(UTF_8));

        boolean result = IOUtil.contentEqualsIgnoreEOL(file1, file2, UTF_8.name());

        assertTrue(result);
    }

    @Test
    public void testContentEqualsIgnoreEOL_Files_DifferentContent() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "eol-diff1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "eol-diff2", ".txt").toFile();

        Files.write(file1.toPath(), "Line1\nLine2\n".getBytes(UTF_8));
        Files.write(file2.toPath(), "Line1\nLine3\n".getBytes(UTF_8));

        boolean result = IOUtil.contentEqualsIgnoreEOL(file1, file2, UTF_8.name());

        assertTrue(!result);
    }

    @Test
    public void testContentEqualsIgnoreEOL_Readers_DifferentEOL() throws Exception {
        Reader r1 = new StringReader("Line1\nLine2");
        Reader r2 = new StringReader("Line1\nLine2");

        boolean result = IOUtil.contentEqualsIgnoreEOL(r1, r2);

        assertTrue(result);
    }

    @Test
    public void testContentEqualsIgnoreEOL_Readers_DifferentContent() throws Exception {
        Reader r1 = new StringReader("Line1\nLine2");
        Reader r2 = new StringReader("Line1\nLine3");

        boolean result = IOUtil.contentEqualsIgnoreEOL(r1, r2);

        assertTrue(!result);
    }

    @Test
    public void testContentEqualsIgnoreEOL_Readers_Empty() throws Exception {
        Reader r1 = new StringReader("");
        Reader r2 = new StringReader("");

        boolean result = IOUtil.contentEqualsIgnoreEOL(r1, r2);

        assertTrue(result);
    }

    @Test
    public void testContentEqualsIgnoreEOL_SameFile() throws Exception {
        assertTrue(IOUtil.contentEqualsIgnoreEOL(tempFile, tempFile, "UTF-8"));
    }

    @Test
    public void testContentEqualsIgnoreEOL_NullReaders() throws Exception {
        assertTrue(IOUtil.contentEqualsIgnoreEOL((Reader) null, (Reader) null));
    }

    @Test
    public void testContentEqualsIgnoreEOL_OneNullReader() throws Exception {
        Reader reader = new StringReader(TEST_CONTENT);
        assertFalse(IOUtil.contentEqualsIgnoreEOL(reader, null));
    }

    @Test
    public void testContentEqualsIgnoreEOL_OneNullOneEmpty_Reader() throws Exception {
        assertFalse(IOUtil.contentEqualsIgnoreEOL(null, new StringReader("")));
    }

    @Test
    public void testContentEqualsMakesProgressWhenBulkReadReturnsZero() throws Exception {
        final byte[] bytes = { 0, 1, 2, 3 };
        assertTrue(IOUtil.contentEquals(zeroBulkInputStream(bytes), new ByteArrayInputStream(bytes)));

        final char[] chars = { 0, 'a', 'b' };
        assertTrue(IOUtil.contentEquals(zeroBulkReader(chars), new StringReader(new String(chars))));
    }

    @Test
    public void testContentEqualsConsumesPastTheComparedPrefix() throws Exception {
        final InputStream shorter = dripInputStream(new byte[] { 1, 2, 3 });
        final InputStream longer = dripInputStream(new byte[] { 1, 2, 3, 4, 5 });

        assertFalse(IOUtil.contentEquals(shorter, longer));

        // byte 4 was pulled out of the longer stream by the end-of-input probe and discarded, so the
        // position afterwards is unspecified and the stream is not meant to be continued from
        assertEquals(5, longer.read());
        assertEquals(-1, longer.read());
    }

    /** Delivers at most one byte per bulk read, which is what makes the equal-prefix branch reachable. */
    private static InputStream dripInputStream(final byte[] bytes) {
        return new InputStream() {
            private int position;

            @Override
            public int read(final byte[] buffer, final int offset, final int length) {
                if (length == 0) {
                    return 0;
                }

                if (position >= bytes.length) {
                    return -1;
                }

                buffer[offset] = bytes[position++];

                return 1;
            }

            @Override
            public int read() {
                return position < bytes.length ? bytes[position++] & 0xff : -1;
            }
        };
    }
}
