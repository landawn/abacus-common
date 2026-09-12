package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.nio.file.Files;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.UncheckedIOException;

public class IOUtilNewTest extends IOUtilTestSupport {
    @Test
    public void testNewStringWriter_NullStringBuilder() {
        assertThrows(IllegalArgumentException.class, () -> IOUtil.newStringWriter((StringBuilder) null));
    }

    @Test
    public void testNewAppendableWriter_WithStringBuilder() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.newAppendableWriter(sb);

        writer.write("Hello");
        writer.write(" ");
        writer.write("World");
        writer.flush();

        assertEquals("Hello World", sb.toString());
    }

    @Test
    public void testNewAppendableWriter_WithStringBuffer() throws IOException {
        StringBuffer sb = new StringBuffer();
        Writer writer = IOUtil.newAppendableWriter(sb);

        writer.write("Test");
        writer.flush();

        assertEquals("Test", sb.toString());
    }

    @Test
    public void testNewAppendableWriter_MultipleWrites() throws IOException {
        StringBuilder sb = new StringBuilder();
        Writer writer = IOUtil.newAppendableWriter(sb);

        for (int i = 0; i < 5; i++) {
            writer.write("Line" + i + "\n");
        }
        writer.flush();

        assertTrue(sb.toString().contains("Line0"));
        assertTrue(sb.toString().contains("Line4"));
    }

    @Test
    public void testNewAppendableWriter_NullAppendable() {
        assertThrows(IllegalArgumentException.class, () -> {
            IOUtil.newAppendableWriter(null);
        });
    }

    @Test
    public void testNewStringWriter_Default() {
        StringWriter sw = IOUtil.newStringWriter();
        assertNotNull(sw);
        sw.write("test");
        assertEquals("test", sw.toString());
    }

    @Test
    public void testNewStringWriter_WithInitialSize() {
        StringWriter sw = IOUtil.newStringWriter(100);
        assertNotNull(sw);
        sw.write("test content");
        assertEquals("test content", sw.toString());
    }

    @Test
    public void testNewStringWriter_WithStringBuilder() {
        StringBuilder sb = new StringBuilder("initial");
        StringWriter sw = IOUtil.newStringWriter(sb);
        assertNotNull(sw);
        sw.write(" added");
        assertEquals("initial added", sw.toString());
    }

    @Test
    public void testNewStringWriter_MultipleWrites() {
        StringWriter sw = IOUtil.newStringWriter();
        sw.write("Hello");
        sw.write(" ");
        sw.write("World");
        assertEquals("Hello World", sw.toString());
    }

    @Test
    public void testNewByteArrayOutputStream_Default() {
        ByteArrayOutputStream baos = IOUtil.newByteArrayOutputStream();
        assertNotNull(baos);
        baos.write(65);
        assertEquals(1, baos.size());
    }

    @Test
    public void testNewByteArrayOutputStream_WithInitCapacity() {
        ByteArrayOutputStream baos = IOUtil.newByteArrayOutputStream(256);
        assertNotNull(baos);
        byte[] data = "test data".getBytes(UTF_8);
        baos.write(data, 0, data.length);
        assertEquals(9, baos.size());
    }

    @Test
    public void testNewByteArrayOutputStream_WriteBytes() throws Exception {
        ByteArrayOutputStream baos = IOUtil.newByteArrayOutputStream();
        byte[] testData = TEST_CONTENT.getBytes(UTF_8);
        baos.write(testData);
        assertArrayEquals(testData, baos.toByteArray());
    }

    @Test
    public void testNewFileInputStream_WithFile() throws Exception {
        try (FileInputStream fis = IOUtil.newFileInputStream(tempFile)) {
            assertNotNull(fis);
            byte[] buffer = new byte[1024];
            int bytesRead = fis.read(buffer);
            assertTrue(bytesRead > 0);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewFileInputStream_WithFileName() throws Exception {
        try (FileInputStream fis = IOUtil.newFileInputStream(tempFile.getAbsolutePath())) {
            assertNotNull(fis);
            byte[] buffer = new byte[1024];
            int bytesRead = fis.read(buffer);
            assertTrue(bytesRead > 0);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewFileInputStream_NonExistentFile() {
        File nonExistent = new File(tempFolder.toFile(), "nonexistent.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.newFileInputStream(nonExistent));
    }

    @Test
    public void testNewFileInputStream_NonExistentFileName() {
        assertThrows(UncheckedIOException.class, () -> IOUtil.newFileInputStream(tempFolder.resolve("nonexistent.txt").toString()));
    }

    @Test
    public void testNewFileOutputStream_WithFile() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = IOUtil.newFileOutputStream(outputFile)) {
            assertNotNull(fos);
            fos.write(TEST_CONTENT.getBytes(UTF_8));
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testNewFileOutputStream_WithFileAppendFalse() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "existing".getBytes(UTF_8));

        try (FileOutputStream fos = IOUtil.newFileOutputStream(outputFile, false)) {
            fos.write(TEST_CONTENT.getBytes(UTF_8));
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testNewFileOutputStream_WithFileAppendTrue() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "existing".getBytes(UTF_8));

        try (FileOutputStream fos = IOUtil.newFileOutputStream(outputFile, true)) {
            fos.write(TEST_CONTENT.getBytes(UTF_8));
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals("existing" + TEST_CONTENT, content);
    }

    @Test
    public void testNewFileOutputStream_WithFileName() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (FileOutputStream fos = IOUtil.newFileOutputStream(outputFile.getAbsolutePath())) {
            assertNotNull(fos);
            fos.write(TEST_CONTENT.getBytes(UTF_8));
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testNewFileOutputStream_InvalidDirectory() throws IOException {
        // Block the path: parent is a regular file, so mkdirs cannot succeed there.
        File blocker = Files.createTempFile(tempFolder, "blocker", "").toFile();
        File invalidFile = new File(blocker, "subdir/file.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.newFileOutputStream(invalidFile));
    }

    @Test
    public void testNewFileReader_WithFile() throws Exception {
        try (FileReader fr = IOUtil.newFileReader(tempFile)) {
            assertNotNull(fr);
            char[] buffer = new char[1024];
            int charsRead = fr.read(buffer);
            assertTrue(charsRead > 0);
            assertEquals(TEST_CONTENT, new String(buffer, 0, charsRead));
        }
    }

    @Test
    public void testNewFileReader_WithFileAndCharset() throws Exception {
        try (FileReader fr = IOUtil.newFileReader(tempFile, UTF_8)) {
            assertNotNull(fr);
            char[] buffer = new char[1024];
            int charsRead = fr.read(buffer);
            assertTrue(charsRead > 0);
            assertEquals(TEST_CONTENT, new String(buffer, 0, charsRead));
        }
    }

    @Test
    public void testNewFileReader_NonExistentFile() {
        File nonExistent = new File(tempFolder.toFile(), "nonexistent.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.newFileReader(nonExistent));
    }

    @Test
    public void testNewFileReader_WithDifferentCharset() throws Exception {
        File isoFile = Files.createTempFile(tempFolder, "iso", ".txt").toFile();
        Files.write(isoFile.toPath(), "ISO content".getBytes(ISO_8859_1));

        try (FileReader fr = IOUtil.newFileReader(isoFile, ISO_8859_1)) {
            assertNotNull(fr);
            char[] buffer = new char[1024];
            int charsRead = fr.read(buffer);
            assertEquals("ISO content", new String(buffer, 0, charsRead));
        }
    }

    @Test
    public void testNewFileWriter_WithFile() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (java.io.FileWriter fw = IOUtil.newFileWriter(outputFile)) {
            assertNotNull(fw);
            fw.write(TEST_CONTENT);
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testNewFileWriter_WithFileAndCharset() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (java.io.FileWriter fw = IOUtil.newFileWriter(outputFile, UTF_8)) {
            assertNotNull(fw);
            fw.write(UNICODE_CONTENT);
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(UNICODE_CONTENT, content);
    }

    @Test
    public void testNewFileWriter_WithFileCharsetAndAppend() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "existing".getBytes(UTF_8));

        try (java.io.FileWriter fw = IOUtil.newFileWriter(outputFile, UTF_8, true)) {
            fw.write(TEST_CONTENT);
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals("existing" + TEST_CONTENT, content);
    }

    @Test
    public void testNewFileWriter_WithFileCharsetNoAppend() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        Files.write(outputFile.toPath(), "existing".getBytes(UTF_8));

        try (java.io.FileWriter fw = IOUtil.newFileWriter(outputFile, UTF_8, false)) {
            fw.write(TEST_CONTENT);
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testNewFileWriter_InvalidDirectory() throws IOException {
        File blocker = Files.createTempFile(tempFolder, "blocker", "").toFile();
        File invalidFile = new File(blocker, "subdir/file.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.newFileWriter(invalidFile));
    }

    @Test
    public void testNewInputStreamReader_WithInputStream() throws Exception {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8));
             java.io.InputStreamReader isr = IOUtil.newInputStreamReader(is)) {
            assertNotNull(isr);
            char[] buffer = new char[1024];
            int charsRead = isr.read(buffer);
            assertEquals(TEST_CONTENT, new String(buffer, 0, charsRead));
        }
    }

    @Test
    public void testNewInputStreamReader_WithInputStreamAndCharset() throws Exception {
        try (InputStream is = new ByteArrayInputStream(UNICODE_CONTENT.getBytes(UTF_8));
             java.io.InputStreamReader isr = IOUtil.newInputStreamReader(is, UTF_8)) {
            assertNotNull(isr);
            char[] buffer = new char[1024];
            int charsRead = isr.read(buffer);
            assertEquals(UNICODE_CONTENT, new String(buffer, 0, charsRead));
        }
    }

    @Test
    public void testNewInputStreamReader_WithUTF16() throws Exception {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_16));
             java.io.InputStreamReader isr = IOUtil.newInputStreamReader(is, UTF_16)) {
            assertNotNull(isr);
            char[] buffer = new char[1024];
            int charsRead = isr.read(buffer);
            assertEquals(TEST_CONTENT, new String(buffer, 0, charsRead));
        }
    }

    @Test
    public void testNewOutputStreamWriter_WithOutputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.io.OutputStreamWriter osw = IOUtil.newOutputStreamWriter(baos)) {
            assertNotNull(osw);
            osw.write(TEST_CONTENT);
            osw.flush();
        }
        assertEquals(TEST_CONTENT, baos.toString(UTF_8.name()));
    }

    @Test
    public void testNewOutputStreamWriter_WithOutputStreamAndCharset() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.io.OutputStreamWriter osw = IOUtil.newOutputStreamWriter(baos, UTF_8)) {
            assertNotNull(osw);
            osw.write(UNICODE_CONTENT);
            osw.flush();
        }
        assertEquals(UNICODE_CONTENT, baos.toString(UTF_8.name()));
    }

    @Test
    public void testNewOutputStreamWriter_WithUTF16() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.io.OutputStreamWriter osw = IOUtil.newOutputStreamWriter(baos, UTF_16)) {
            assertNotNull(osw);
            osw.write(TEST_CONTENT);
            osw.flush();
        }
        assertEquals(TEST_CONTENT, baos.toString(UTF_16.name()));
    }

    @Test
    public void testNewBufferedInputStream_WithInputStream() throws Exception {
        try (java.io.BufferedInputStream bis = IOUtil.newBufferedInputStream(new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8)))) {
            assertNotNull(bis);
            byte[] buffer = new byte[1024];
            int bytesRead = bis.read(buffer);
            assertTrue(bytesRead > 0);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewBufferedInputStream_WithInputStream_AlreadyBuffered() throws Exception {
        java.io.BufferedInputStream original = new java.io.BufferedInputStream(new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8)));
        java.io.BufferedInputStream result = IOUtil.newBufferedInputStream(original);
        assertNotNull(result);
        result.close();
    }

    @Test
    public void testNewBufferedInputStream_WithFile() throws Exception {
        try (java.io.BufferedInputStream bis = IOUtil.newBufferedInputStream(tempFile)) {
            assertNotNull(bis);
            byte[] buffer = new byte[1024];
            int bytesRead = bis.read(buffer);
            assertTrue(bytesRead > 0);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewBufferedInputStream_WithFileAndSize() throws Exception {
        try (java.io.BufferedInputStream bis = IOUtil.newBufferedInputStream(tempFile, 4096)) {
            assertNotNull(bis);
            byte[] buffer = new byte[1024];
            int bytesRead = bis.read(buffer);
            assertTrue(bytesRead > 0);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewBufferedInputStream_NonExistentFile() {
        File nonExistent = new File(tempFolder.toFile(), "nonexistent.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.newBufferedInputStream(nonExistent));
    }

    @Test
    public void testNewBufferedInputStream_LargeBuffer() throws Exception {
        try (java.io.BufferedInputStream bis = IOUtil.newBufferedInputStream(largeFile, 16384)) {
            assertNotNull(bis);
            byte[] buffer = new byte[1024];
            int bytesRead = bis.read(buffer);
            assertTrue(bytesRead > 0);
        }
    }

    @Test
    public void testNewBufferedReader_WithReader() throws Exception {
        try (java.io.BufferedReader br = IOUtil.newBufferedReader(new StringReader(TEST_CONTENT))) {
            assertNotNull(br);
            String line = br.readLine();
            assertEquals(TEST_CONTENT, line);
        }
    }

    @Test
    public void testNewBufferedReader_WithReader_AlreadyBuffered() throws Exception {
        java.io.BufferedReader original = new java.io.BufferedReader(new StringReader(TEST_CONTENT));
        java.io.BufferedReader result = IOUtil.newBufferedReader(original);
        assertNotNull(result);
        result.close();
    }

    @Test
    public void testNewBufferedReader_WithFile() throws Exception {
        try (java.io.BufferedReader br = IOUtil.newBufferedReader(tempFile)) {
            assertNotNull(br);
            String line = br.readLine();
            assertEquals(TEST_CONTENT, line);
        }
    }

    @Test
    public void testNewBufferedReader_WithFileAndCharset() throws Exception {
        try (java.io.BufferedReader br = IOUtil.newBufferedReader(tempFile, UTF_8)) {
            assertNotNull(br);
            String line = br.readLine();
            assertEquals(TEST_CONTENT, line);
        }
    }

    @Test
    public void testNewBufferedReader_WithPath() throws Exception {
        try (java.io.BufferedReader br = IOUtil.newBufferedReader(tempFile.toPath())) {
            assertNotNull(br);
            String line = br.readLine();
            assertEquals(TEST_CONTENT, line);
        }
    }

    @Test
    public void testNewBufferedReader_WithPathAndCharset() throws Exception {
        try (java.io.BufferedReader br = IOUtil.newBufferedReader(tempFile.toPath(), UTF_8)) {
            assertNotNull(br);
            String line = br.readLine();
            assertEquals(TEST_CONTENT, line);
        }
    }

    @Test
    public void testNewBufferedReader_WithInputStream() throws Exception {
        try (InputStream is = new ByteArrayInputStream(TEST_CONTENT.getBytes(UTF_8));
             java.io.BufferedReader br = IOUtil.newBufferedReader(is)) {
            assertNotNull(br);
            String line = br.readLine();
            assertEquals(TEST_CONTENT, line);
        }
    }

    @Test
    public void testNewBufferedReader_WithInputStreamAndCharset() throws Exception {
        try (InputStream is = new ByteArrayInputStream(UNICODE_CONTENT.getBytes(UTF_8));
             java.io.BufferedReader br = IOUtil.newBufferedReader(is, UTF_8)) {
            assertNotNull(br);
            String line = br.readLine();
            assertEquals(UNICODE_CONTENT, line);
        }
    }

    @Test
    public void testNewBufferedReader_NonExistentFile() {
        File nonExistent = new File(tempFolder.toFile(), "nonexistent.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.newBufferedReader(nonExistent));
    }

    @Test
    public void testNewBufferedReader_MultipleLines() throws Exception {
        File multilineFile = Files.createTempFile(tempFolder, "multiline", ".txt").toFile();
        Files.write(multilineFile.toPath(), MULTILINE_CONTENT.getBytes(UTF_8));

        try (java.io.BufferedReader br = IOUtil.newBufferedReader(multilineFile)) {
            assertEquals("Line 1", br.readLine());
            assertEquals("Line 2", br.readLine());
            assertEquals("Line 3", br.readLine());
        }
    }

    @Test
    public void testNewBufferedOutputStream_WithOutputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.io.BufferedOutputStream bos = IOUtil.newBufferedOutputStream(baos)) {
            assertNotNull(bos);
            bos.write(TEST_CONTENT.getBytes(UTF_8));
        }
        assertEquals(TEST_CONTENT, baos.toString(UTF_8.name()));
    }

    @Test
    public void testNewBufferedOutputStream_WithOutputStream_AlreadyBuffered() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        java.io.BufferedOutputStream original = new java.io.BufferedOutputStream(baos);
        java.io.BufferedOutputStream result = IOUtil.newBufferedOutputStream(original);
        assertNotNull(result);
        result.close();
    }

    @Test
    public void testNewBufferedOutputStream_WithFile() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (java.io.BufferedOutputStream bos = IOUtil.newBufferedOutputStream(outputFile)) {
            assertNotNull(bos);
            bos.write(TEST_CONTENT.getBytes(UTF_8));
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testNewBufferedOutputStream_WithFileAndSize() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (java.io.BufferedOutputStream bos = IOUtil.newBufferedOutputStream(outputFile, 4096)) {
            assertNotNull(bos);
            bos.write(TEST_CONTENT.getBytes(UTF_8));
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testNewBufferedOutputStream_InvalidDirectory() throws IOException {
        File blocker = Files.createTempFile(tempFolder, "blocker", "").toFile();
        File invalidFile = new File(blocker, "subdir/file.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.newBufferedOutputStream(invalidFile));
    }

    @Test
    public void testNewBufferedOutputStream_LargeBuffer() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (java.io.BufferedOutputStream bos = IOUtil.newBufferedOutputStream(outputFile, 16384)) {
            assertNotNull(bos);
            byte[] largeData = new byte[8192];
            java.util.Arrays.fill(largeData, (byte) 'X');
            bos.write(largeData);
        }
        assertEquals(8192, Files.size(outputFile.toPath()));
    }

    @Test
    public void testNewBufferedWriter_WithWriter() throws Exception {
        java.io.StringWriter sw = new java.io.StringWriter();
        try (java.io.BufferedWriter bw = IOUtil.newBufferedWriter(sw)) {
            assertNotNull(bw);
            bw.write(TEST_CONTENT);
        }
        assertEquals(TEST_CONTENT, sw.toString());
    }

    @Test
    public void testNewBufferedWriter_WithWriter_AlreadyBuffered() throws Exception {
        java.io.StringWriter sw = new java.io.StringWriter();
        java.io.BufferedWriter original = new java.io.BufferedWriter(sw);
        java.io.BufferedWriter result = IOUtil.newBufferedWriter(original);
        assertNotNull(result);
        result.close();
    }

    @Test
    public void testNewBufferedWriter_WithFile() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (java.io.BufferedWriter bw = IOUtil.newBufferedWriter(outputFile)) {
            assertNotNull(bw);
            bw.write(TEST_CONTENT);
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(TEST_CONTENT, content);
    }

    @Test
    public void testNewBufferedWriter_WithFileAndCharset() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (java.io.BufferedWriter bw = IOUtil.newBufferedWriter(outputFile, UTF_8)) {
            assertNotNull(bw);
            bw.write(UNICODE_CONTENT);
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertEquals(UNICODE_CONTENT, content);
    }

    @Test
    public void testNewBufferedWriter_WithOutputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.io.BufferedWriter bw = IOUtil.newBufferedWriter(baos)) {
            assertNotNull(bw);
            bw.write(TEST_CONTENT);
            bw.flush();
        }
        assertEquals(TEST_CONTENT, baos.toString(UTF_8.name()));
    }

    @Test
    public void testNewBufferedWriter_WithOutputStreamAndCharset() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.io.BufferedWriter bw = IOUtil.newBufferedWriter(baos, UTF_8)) {
            assertNotNull(bw);
            bw.write(UNICODE_CONTENT);
            bw.flush();
        }
        assertEquals(UNICODE_CONTENT, baos.toString(UTF_8.name()));
    }

    @Test
    public void testNewBufferedWriter_InvalidDirectory() throws IOException {
        File blocker = Files.createTempFile(tempFolder, "blocker", "").toFile();
        File invalidFile = new File(blocker, "subdir/file.txt");
        assertThrows(UncheckedIOException.class, () -> IOUtil.newBufferedWriter(invalidFile));
    }

    @Test
    public void testNewBufferedWriter_MultipleWrites() throws Exception {
        File outputFile = Files.createTempFile(tempFolder, "output", ".txt").toFile();
        try (java.io.BufferedWriter bw = IOUtil.newBufferedWriter(outputFile)) {
            bw.write("Line 1");
            bw.newLine();
            bw.write("Line 2");
            bw.newLine();
            bw.write("Line 3");
        }
        String content = new String(Files.readAllBytes(outputFile.toPath()), UTF_8);
        assertTrue(content.contains("Line 1"));
        assertTrue(content.contains("Line 2"));
        assertTrue(content.contains("Line 3"));
    }

    @Test
    public void testNewLZ4BlockInputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (LZ4BlockOutputStream lz4Out = IOUtil.newLZ4BlockOutputStream(baos)) {
            lz4Out.write(TEST_CONTENT.getBytes(UTF_8));
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray());
             LZ4BlockInputStream lz4In = IOUtil.newLZ4BlockInputStream(is)) {
            assertNotNull(lz4In);
            byte[] buffer = new byte[1024];
            int bytesRead = lz4In.read(buffer);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewLZ4BlockOutputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (LZ4BlockOutputStream lz4Out = IOUtil.newLZ4BlockOutputStream(baos)) {
            assertNotNull(lz4Out);
            lz4Out.write(TEST_CONTENT.getBytes(UTF_8));
        }
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testNewLZ4BlockOutputStream_WithBlockSize() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (LZ4BlockOutputStream lz4Out = IOUtil.newLZ4BlockOutputStream(baos, 4096)) {
            assertNotNull(lz4Out);
            lz4Out.write(TEST_CONTENT.getBytes(UTF_8));
        }
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testNewSnappyInputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (SnappyOutputStream snappyOut = IOUtil.newSnappyOutputStream(baos)) {
            snappyOut.write(TEST_CONTENT.getBytes(UTF_8));
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray());
             SnappyInputStream snappyIn = IOUtil.newSnappyInputStream(is)) {
            assertNotNull(snappyIn);
            byte[] buffer = new byte[1024];
            int bytesRead = snappyIn.read(buffer);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewSnappyOutputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (SnappyOutputStream snappyOut = IOUtil.newSnappyOutputStream(baos)) {
            assertNotNull(snappyOut);
            snappyOut.write(TEST_CONTENT.getBytes(UTF_8));
        }
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testNewSnappyOutputStream_WithBufferSize() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (SnappyOutputStream snappyOut = IOUtil.newSnappyOutputStream(baos, 4096)) {
            assertNotNull(snappyOut);
            snappyOut.write(TEST_CONTENT.getBytes(UTF_8));
        }
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testNewGZIPInputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gzipOut = IOUtil.newGZIPOutputStream(baos)) {
            gzipOut.write(TEST_CONTENT.getBytes(UTF_8));
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray());
             java.util.zip.GZIPInputStream gzipIn = IOUtil.newGZIPInputStream(is)) {
            assertNotNull(gzipIn);
            byte[] buffer = new byte[1024];
            int bytesRead = gzipIn.read(buffer);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewGZIPInputStream_WithBufferSize() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gzipOut = IOUtil.newGZIPOutputStream(baos)) {
            gzipOut.write(TEST_CONTENT.getBytes(UTF_8));
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray());
             java.util.zip.GZIPInputStream gzipIn = IOUtil.newGZIPInputStream(is, 4096)) {
            assertNotNull(gzipIn);
            byte[] buffer = new byte[1024];
            int bytesRead = gzipIn.read(buffer);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewGZIPInputStream_InvalidData() {
        InputStream is = new ByteArrayInputStream("not gzip data".getBytes(UTF_8));
        assertThrows(UncheckedIOException.class, () -> IOUtil.newGZIPInputStream(is));
    }

    @Test
    public void testNewGZIPOutputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gzipOut = IOUtil.newGZIPOutputStream(baos)) {
            assertNotNull(gzipOut);
            gzipOut.write(TEST_CONTENT.getBytes(UTF_8));
        }
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testNewGZIPOutputStream_WithBufferSize() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.GZIPOutputStream gzipOut = IOUtil.newGZIPOutputStream(baos, 4096)) {
            assertNotNull(gzipOut);
            gzipOut.write(TEST_CONTENT.getBytes(UTF_8));
        }
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testNewZipInputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zipOut = IOUtil.newZipOutputStream(baos)) {
            java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry("test.txt");
            zipOut.putNextEntry(entry);
            zipOut.write(TEST_CONTENT.getBytes(UTF_8));
            zipOut.closeEntry();
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray());
             java.util.zip.ZipInputStream zipIn = IOUtil.newZipInputStream(is)) {
            assertNotNull(zipIn);
            java.util.zip.ZipEntry entry = zipIn.getNextEntry();
            assertNotNull(entry);
            assertEquals("test.txt", entry.getName());
            byte[] buffer = new byte[1024];
            int bytesRead = zipIn.read(buffer);
            assertEquals(TEST_CONTENT, new String(buffer, 0, bytesRead, UTF_8));
        }
    }

    @Test
    public void testNewZipInputStream_WithCharset() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zipOut = IOUtil.newZipOutputStream(baos, UTF_8)) {
            java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry("test.txt");
            zipOut.putNextEntry(entry);
            zipOut.write(TEST_CONTENT.getBytes(UTF_8));
            zipOut.closeEntry();
        }

        try (InputStream is = new ByteArrayInputStream(baos.toByteArray());
             java.util.zip.ZipInputStream zipIn = IOUtil.newZipInputStream(is, UTF_8)) {
            assertNotNull(zipIn);
            java.util.zip.ZipEntry entry = zipIn.getNextEntry();
            assertNotNull(entry);
        }
    }

    @Test
    public void testNewZipOutputStream() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zipOut = IOUtil.newZipOutputStream(baos)) {
            assertNotNull(zipOut);
            java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry("file.txt");
            zipOut.putNextEntry(entry);
            zipOut.write(TEST_CONTENT.getBytes(UTF_8));
            zipOut.closeEntry();
        }
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testNewZipOutputStream_WithCharset() throws Exception {
        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
        try (java.util.zip.ZipOutputStream zipOut = IOUtil.newZipOutputStream(baos, UTF_8)) {
            assertNotNull(zipOut);
            java.util.zip.ZipEntry entry = new java.util.zip.ZipEntry("unicode.txt");
            zipOut.putNextEntry(entry);
            zipOut.write(UNICODE_CONTENT.getBytes(UTF_8));
            zipOut.closeEntry();
        }
        assertTrue(baos.size() > 0);
    }

    @Test
    public void testNewBrotliInputStream() throws IOException {
        try (InputStream testInput = new ByteArrayInputStream(new byte[0]);
             BrotliInputStream brotliIn = IOUtil.newBrotliInputStream(testInput)) {
            assertNotNull(brotliIn);
        }
    }

    @Test
    public void testNewBrotliInputStream_NullInputStream() {
        assertThrows(Exception.class, () -> {
            IOUtil.newBrotliInputStream(null);
        });
    }

    @Test
    public void testNewFileOutputStreamByNameCanAppend() throws IOException {
        // newFileOutputStream(String) had no append counterpart, unlike the File pair.
        final File file = new File(tempFolder.toFile(), "by-name-append.bin");

        try (OutputStream os = IOUtil.newFileOutputStream(file.getAbsolutePath(), true)) {
            os.write('a');
        }

        try (OutputStream os = IOUtil.newFileOutputStream(file.getAbsolutePath(), true)) {
            os.write('b');
        }

        assertEquals("ab", IOUtil.readAllToString(file));

        // append=false truncates, like the File overload.
        try (OutputStream os = IOUtil.newFileOutputStream(file.getAbsolutePath(), false)) {
            os.write('c');
        }

        assertEquals("c", IOUtil.readAllToString(file));

        // Missing parent directories are created, as for every other file-opening factory here.
        final File nested = new File(tempFolder.toFile(), "by-name/deep/out.bin");

        try (OutputStream os = IOUtil.newFileOutputStream(nested.getAbsolutePath(), true)) {
            os.write('z');
        }

        assertEquals("z", IOUtil.readAllToString(nested));
    }

    @Test
    public void testNewByteArrayOutputStream_NegativeInitCapacity() {
        // The message comes from ByteArrayOutputStream's own constructor, not from N.checkArgNotNegative:
        // the two factories word their rejection differently by design.
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> IOUtil.newByteArrayOutputStream(-1));
        assertEquals("Negative initial size: -1", e.getMessage());
        assertNotNull(IOUtil.newByteArrayOutputStream(0));
    }
}
