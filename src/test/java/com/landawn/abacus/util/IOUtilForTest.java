package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.UncheckedIOException;

public class IOUtilForTest extends IOUtilTestSupport {
    @Test
    public void testForEachLine_File_Basic() throws Exception {
        File file = Files.createTempFile(tempFolder, "for-lines", ".txt").toFile();
        Files.write(file.toPath(), "Line1\nLine2\nLine3\n".getBytes(UTF_8));

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forEachLine(file, line -> lines.add(line));

        assertEquals(3, lines.size());
        assertEquals("Line1", lines.get(0));
        assertEquals("Line2", lines.get(1));
        assertEquals("Line3", lines.get(2));
    }

    @Test
    public void testForEachLine_File_WithCharset() throws Exception {
        File file = Files.createTempFile(tempFolder, "for-lines-enc", ".txt").toFile();
        Files.write(file.toPath(), "Line1\nLine2\n".getBytes(UTF_8));

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forEachLine(file, line -> lines.add(line), () -> {
        });

        assertEquals(2, lines.size());
    }

    @Test
    public void testForEachLine_File_WithOffsetAndCount() throws Exception {
        File file = Files.createTempFile(tempFolder, "for-lines-offset", ".txt").toFile();
        Files.write(file.toPath(), "Line1\nLine2\nLine3\nLine4\nLine5\n".getBytes(UTF_8));

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forEachLine(file, 1, 3, line -> lines.add(line));

        assertEquals(3, lines.size());
        assertEquals("Line2", lines.get(0));
        assertEquals("Line3", lines.get(1));
        assertEquals("Line4", lines.get(2));
    }

    @Test
    public void testForEachLine_File_WithOffsetCountAndCallback() throws Exception {
        File file = Files.createTempFile(tempFolder, "for-lines-callback", ".txt").toFile();
        Files.write(file.toPath(), "Line1\nLine2\nLine3\n".getBytes(UTF_8));

        java.util.List<String> lines = new java.util.ArrayList<>();
        final boolean[] callbackInvoked = { false };

        IOUtil.forEachLine(file, 0, 2, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(2, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForEachLine_File_WithThreads() throws Exception {
        File file = Files.createTempFile(tempFolder, "for-lines-threads", ".txt").toFile();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Line").append(i).append("\n");
        }
        Files.write(file.toPath(), sb.toString().getBytes(UTF_8));

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        IOUtil.forEachLine(file, IOUtil.LineIterationOptions.builder().readThreads(0).processThreads(100).queueSize(2).build(), line -> lines.add(line));

        assertEquals(100, lines.size());
    }

    @Test
    public void testForEachLine_File_WithThreadsAndCallback() throws Exception {
        File file = Files.createTempFile(tempFolder, "for-lines-threads-cb", ".txt").toFile();
        Files.write(file.toPath(), "Line1\nLine2\nLine3\n".getBytes(UTF_8));

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final boolean[] callbackInvoked = { false };

        IOUtil.forEachLine(file, IOUtil.LineIterationOptions.builder().readThreads(0).processThreads(3).queueSize(2).build(), line -> lines.add(line),
                () -> callbackInvoked[0] = true);

        assertEquals(3, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForEachLine_Collection_Basic() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "coll1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "coll2", ".txt").toFile();
        Files.write(file1.toPath(), "File1Line1\nFile1Line2\n".getBytes(UTF_8));
        Files.write(file2.toPath(), "File2Line1\nFile2Line2\n".getBytes(UTF_8));

        java.util.List<File> files = java.util.Arrays.asList(file1, file2);
        java.util.List<String> lines = new java.util.ArrayList<>();

        IOUtil.forEachLine(files, line -> lines.add(line));

        assertEquals(4, lines.size());
    }

    @Test
    public void testForEachLine_Directory_LazyOpen() throws Exception {
        // A directory with many files: forEachLine now opens each file lazily (LazyFileLineIterator) rather than
        // holding every file descriptor open up front. Verify all lines from all files are still read.
        File dir = Files.createTempDirectory(tempFolder, "forlines-dir").toFile();
        final int fileCount = 20;
        final int linesPerFile = 5;

        for (int f = 0; f < fileCount; f++) {
            File file = new File(dir, "f" + f + ".txt");
            StringBuilder sb = new StringBuilder();
            for (int l = 0; l < linesPerFile; l++) {
                sb.append("f").append(f).append("L").append(l).append("\n");
            }
            Files.write(file.toPath(), sb.toString().getBytes(UTF_8));
        }

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forEachLine(dir, line -> lines.add(line));

        assertEquals(fileCount * linesPerFile, lines.size());
    }

    @Test
    public void testForEachLine_Collection_WithCallback() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "coll-cb1", ".txt").toFile();
        Files.write(file1.toPath(), "Line1\n".getBytes(UTF_8));

        java.util.List<File> files = java.util.Arrays.asList(file1);
        java.util.List<String> lines = new java.util.ArrayList<>();
        final boolean[] callbackInvoked = { false };

        IOUtil.forEachLine(files, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(1, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForEachLine_Collection_WithOffsetAndCount() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "coll-offset1", ".txt").toFile();
        Files.write(file1.toPath(), "Line1\nLine2\nLine3\nLine4\n".getBytes(UTF_8));

        java.util.List<File> files = java.util.Arrays.asList(file1);
        java.util.List<String> lines = new java.util.ArrayList<>();

        IOUtil.forEachLine(files, 1, 2, line -> lines.add(line));

        assertEquals(2, lines.size());
        assertEquals("Line2", lines.get(0));
    }

    @Test
    public void testForEachLine_Collection_WithThreads() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "coll-threads", ".txt").toFile();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("Line").append(i).append("\n");
        }
        Files.write(file1.toPath(), sb.toString().getBytes(UTF_8));

        java.util.List<File> files = java.util.Arrays.asList(file1);
        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        IOUtil.forEachLine(files, IOUtil.LineIterationOptions.builder().readThreads(0).processThreads(50).queueSize(2).build(), line -> lines.add(line));

        assertEquals(50, lines.size());
    }

    @Test
    public void testForEachLine_File_ReadAndProcessThreads() throws Exception {
        File file = Files.createTempFile(tempFolder, "read-process", ".txt").toFile();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Line").append(i).append("\n");
        }
        Files.write(file.toPath(), sb.toString().getBytes(UTF_8));

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        IOUtil.forEachLine(file, IOUtil.LineIterationOptions.builder().readThreads(1).processThreads(2).queueSize(100).build(), line -> lines.add(line));

        assertEquals(100, lines.size());
    }

    @Test
    public void testForEachLine_File_ReadProcessThreadsWithCallback() throws Exception {
        File file = Files.createTempFile(tempFolder, "read-process-cb", ".txt").toFile();
        Files.write(file.toPath(), "Line1\nLine2\nLine3\n".getBytes(UTF_8));

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final boolean[] callbackInvoked = { false };

        IOUtil.forEachLine(file, IOUtil.LineIterationOptions.builder().readThreads(1).processThreads(2).queueSize(100).build(), line -> lines.add(line),
                () -> callbackInvoked[0] = true);

        assertEquals(3, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForEachLine_File_WithOffsetCountReadProcessThreads() throws Exception {
        File file = Files.createTempFile(tempFolder, "offset-read-process", ".txt").toFile();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Line").append(i).append("\n");
        }
        Files.write(file.toPath(), sb.toString().getBytes(UTF_8));

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        IOUtil.forEachLine(file, IOUtil.LineIterationOptions.builder().offset(10).count(50).readThreads(1).processThreads(2).queueSize(100).build(),
                line -> lines.add(line));

        assertEquals(50, lines.size());
    }

    @Test
    public void testForEachLine_File_WithOffsetCountReadProcessThreadsCallback() throws Exception {
        File file = Files.createTempFile(tempFolder, "offset-read-process-cb", ".txt").toFile();
        Files.write(file.toPath(), "Line1\nLine2\nLine3\nLine4\nLine5\n".getBytes(UTF_8));

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final boolean[] callbackInvoked = { false };

        IOUtil.forEachLine(file, IOUtil.LineIterationOptions.builder().offset(1).count(3).readThreads(1).processThreads(2).queueSize(50).build(),
                line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(3, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForEachLine_Collection_ReadProcessThreads() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "coll-rp1", ".txt").toFile();
        File file2 = Files.createTempFile(tempFolder, "coll-rp2", ".txt").toFile();

        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 50; i++) {
            sb.append("Line").append(i).append("\n");
        }
        Files.write(file1.toPath(), sb.toString().getBytes(UTF_8));
        Files.write(file2.toPath(), sb.toString().getBytes(UTF_8));

        java.util.List<File> files = java.util.Arrays.asList(file1, file2);
        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        IOUtil.forEachLine(files, IOUtil.LineIterationOptions.builder().offset(0).count(100).processThreads(2).queueSize(1024).build(),
                line -> lines.add(line));

        assertEquals(100, lines.size());
    }

    @Test
    public void testForEachLine_Collection_ReadProcessThreadsCallback() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "coll-rp-cb", ".txt").toFile();
        Files.write(file1.toPath(), "Line1\nLine2\n".getBytes(UTF_8));

        java.util.List<File> files = java.util.Arrays.asList(file1);
        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final boolean[] callbackInvoked = { false };

        IOUtil.forEachLine(files, IOUtil.LineIterationOptions.builder().readThreads(1).processThreads(2).queueSize(50).build(), line -> lines.add(line),
                () -> callbackInvoked[0] = true);

        assertEquals(2, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForEachLine_Collection_WithOffsetCountReadProcessThreads() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "coll-offset-rp", ".txt").toFile();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Line").append(i).append("\n");
        }
        Files.write(file1.toPath(), sb.toString().getBytes(UTF_8));

        java.util.List<File> files = java.util.Arrays.asList(file1);
        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());

        IOUtil.forEachLine(files, IOUtil.LineIterationOptions.builder().offset(10).count(30).readThreads(1).processThreads(2).queueSize(100).build(),
                line -> lines.add(line));

        assertEquals(30, lines.size());
    }

    @Test
    public void testForEachLine_Collection_WithOffsetCountReadProcessThreadsCallback() throws Exception {
        File file1 = Files.createTempFile(tempFolder, "coll-offset-rp-cb", ".txt").toFile();
        Files.write(file1.toPath(), "Line1\nLine2\nLine3\nLine4\nLine5\n".getBytes(UTF_8));

        java.util.List<File> files = java.util.Arrays.asList(file1);
        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final boolean[] callbackInvoked = { false };

        IOUtil.forEachLine(files, IOUtil.LineIterationOptions.builder().offset(1).count(3).readThreads(1).processThreads(2).queueSize(50).build(),
                line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(3, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForEachLine_InputStream_Basic() throws Exception {
        String content = "Line1\nLine2\nLine3\n";
        InputStream is = new ByteArrayInputStream(content.getBytes(UTF_8));

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forEachLine(is, line -> lines.add(line));

        assertEquals(3, lines.size());
        assertEquals("Line1", lines.get(0));
    }

    @Test
    public void testForEachLine_InputStream_WithCallback() throws Exception {
        String content = "Line1\nLine2\n";
        InputStream is = new ByteArrayInputStream(content.getBytes(UTF_8));

        java.util.List<String> lines = new java.util.ArrayList<>();
        final boolean[] callbackInvoked = { false };

        IOUtil.forEachLine(is, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(2, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForEachLine_InputStream_WithOffsetAndCount() throws Exception {
        String content = "Line1\nLine2\nLine3\nLine4\nLine5\n";
        InputStream is = new ByteArrayInputStream(content.getBytes(UTF_8));

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forEachLine(is, 1, 3, line -> lines.add(line));

        assertEquals(3, lines.size());
        assertEquals("Line2", lines.get(0));
    }

    @Test
    public void testForEachLine_InputStream_WithOffsetCountCallback() throws Exception {
        String content = "Line1\nLine2\nLine3\n";
        InputStream is = new ByteArrayInputStream(content.getBytes(UTF_8));

        java.util.List<String> lines = new java.util.ArrayList<>();
        final boolean[] callbackInvoked = { false };

        IOUtil.forEachLine(is, 0, 2, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(2, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForEachLine_InputStream_WithProcessThreads() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Line").append(i).append("\n");
        }
        InputStream is = new ByteArrayInputStream(sb.toString().getBytes(UTF_8));

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        IOUtil.forEachLine(is, IOUtil.LineIterationOptions.builder().offset(0L).count(100L).processThreads(2).queueSize(16).build(),
                Fnn.c(line -> lines.add(line)));

        assertEquals(100, lines.size());
    }

    @Test
    public void testForEachLine_InputStream_WithProcessThreadsCallback() throws Exception {
        String content = "Line1\nLine2\nLine3\n";
        InputStream is = new ByteArrayInputStream(content.getBytes(UTF_8));

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final boolean[] callbackInvoked = { false };

        IOUtil.forEachLine(is, IOUtil.LineIterationOptions.builder().offset(0L).count(3L).processThreads(2).queueSize(16).build(),
                Fnn.c(line -> lines.add(line)), () -> callbackInvoked[0] = true);

        assertEquals(3, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForEachLine_Reader_Basic() throws Exception {
        Reader reader = new StringReader("Line1\nLine2\nLine3\n");

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forEachLine(reader, line -> lines.add(line));

        assertEquals(3, lines.size());
        assertEquals("Line1", lines.get(0));
    }

    @Test
    public void testForEachLine_Reader_WithCallback() throws Exception {
        Reader reader = new StringReader("Line1\nLine2\n");

        java.util.List<String> lines = new java.util.ArrayList<>();
        final boolean[] callbackInvoked = { false };

        IOUtil.forEachLine(reader, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(2, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForEachLine_Reader_WithOffsetAndCount() throws Exception {
        Reader reader = new StringReader("Line1\nLine2\nLine3\nLine4\nLine5\n");

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forEachLine(reader, 1, 3, line -> lines.add(line));

        assertEquals(3, lines.size());
        assertEquals("Line2", lines.get(0));
    }

    @Test
    public void testForEachLine_Reader_WithOffsetCountCallback() throws Exception {
        Reader reader = new StringReader("Line1\nLine2\nLine3\n");

        java.util.List<String> lines = new java.util.ArrayList<>();
        final boolean[] callbackInvoked = { false };

        IOUtil.forEachLine(reader, 0, 2, line -> lines.add(line), () -> callbackInvoked[0] = true);

        assertEquals(2, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForEachLine_Reader_WithProcessThreads() throws Exception {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            sb.append("Line").append(i).append("\n");
        }
        Reader reader = new StringReader(sb.toString());

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        IOUtil.forEachLine(reader, IOUtil.LineIterationOptions.builder().offset(0L).count(100L).processThreads(2).queueSize(16).build(),
                line -> lines.add(line));

        assertEquals(100, lines.size());
    }

    @Test
    public void testForEachLine_Reader_WithProcessThreadsCallback() throws Exception {
        Reader reader = new StringReader("Line1\nLine2\nLine3\n");

        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        final boolean[] callbackInvoked = { false };

        IOUtil.forEachLine(reader, IOUtil.LineIterationOptions.builder().offset(0L).count(3L).processThreads(2).queueSize(16).build(), line -> lines.add(line),
                () -> callbackInvoked[0] = true);

        assertEquals(3, lines.size());
        assertTrue(callbackInvoked[0]);
    }

    @Test
    public void testForEachLine_EmptyFile() throws Exception {
        File file = Files.createTempFile(tempFolder, "empty-for-lines", ".txt").toFile();

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forEachLine(file, line -> lines.add(line));

        assertEquals(0, lines.size());
    }

    @Test
    public void testForEachLine_EmptyInputStream() throws Exception {
        InputStream is = new ByteArrayInputStream(new byte[0]);

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forEachLine(is, line -> lines.add(line));

        assertEquals(0, lines.size());
    }

    @Test
    public void testForEachLine_EmptyReader() throws Exception {
        Reader reader = new StringReader("");

        java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forEachLine(reader, line -> lines.add(line));

        assertEquals(0, lines.size());
    }

    @Test
    public void testForEachLine_LargeFile() throws Exception {
        File largeFile = Files.createTempFile(tempFolder, "large-for-lines", ".txt").toFile();
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 10000; i++) {
            sb.append("Line ").append(i).append("\n");
        }
        Files.write(largeFile.toPath(), sb.toString().getBytes(UTF_8));

        java.util.concurrent.atomic.AtomicInteger count = new java.util.concurrent.atomic.AtomicInteger(0);
        IOUtil.forEachLine(largeFile, line -> count.incrementAndGet());

        assertEquals(10000, count.get());
    }

    @Test
    public void testForEachLine_WithException() throws Exception {
        File file = Files.createTempFile(tempFolder, "for-lines-exception", ".txt").toFile();
        Files.write(file.toPath(), "Line1\nLine2\nLine3\n".getBytes(UTF_8));

        assertThrows(Exception.class, () -> {
            IOUtil.forEachLine(file, line -> {
                if (line.equals("Line2")) {
                    throw new RuntimeException("Test exception");
                }
            });
        });
    }

    @Test
    public void testForEachLine_WithProcessThreads() throws Exception {
        File file = Files.createTempFile(tempFolder, "forlines-thread", ".txt").toFile();
        IOUtil.writeLines(java.util.Arrays.asList("line1", "line2", "line3", "line4", "line5"), file);

        java.util.List<String> lines = new java.util.concurrent.CopyOnWriteArrayList<>();
        IOUtil.forEachLine(file, IOUtil.LineIterationOptions.builder().offset(0L).count(Long.MAX_VALUE).processThreads(2).queueSize(4).build(),
                line -> lines.add(line));
        assertEquals(5, lines.size());
    }

    // ===== forEachLine with Collection<File> and processThreadNum =====

    @Test
    public void testForEachLine_Collection_WithOffsetCountAndProcessThreads() throws Exception {
        java.util.List<String> lines = java.util.Collections.synchronizedList(new java.util.ArrayList<>());
        java.util.List<File> files = java.util.Arrays.asList(largeFile);
        IOUtil.forEachLine(files, IOUtil.LineIterationOptions.builder().offset(0L).count(5L).processThreads(1).queueSize(1).build(),
                (String line) -> lines.add(line), () -> {
                });
        assertEquals(5, lines.size());
    }

    @Test
    public void testForEachLine_Collection_EmptyList_WithProcessThreads() throws Exception {
        java.util.List<String> lines = new java.util.ArrayList<>();
        java.util.List<File> emptyFiles = java.util.Collections.emptyList();
        IOUtil.forEachLine(emptyFiles, IOUtil.LineIterationOptions.builder().offset(0L).count(5L).processThreads(1).queueSize(1).build(),
                (String line) -> lines.add(line), () -> {
                });
        assertEquals(0, lines.size());
    }

    @Test
    public void testForEachLine_Collection_WithOffsetCount_NoProcessThreads() throws Exception {
        java.util.List<String> lines = new java.util.ArrayList<>();
        java.util.List<File> files = java.util.Arrays.asList(largeFile);
        IOUtil.forEachLine(files, IOUtil.LineIterationOptions.builder().offset(2L).count(3L).processThreads(0).queueSize(0).build(),
                (String line) -> lines.add(line), () -> {
                });
        assertEquals(3, lines.size());
    }

    @Test
    public void testForEachLineValidatesEmptyInputsAndMissingFiles() {
        assertThrows(IllegalArgumentException.class,
                () -> IOUtil.forEachLine(java.util.Collections.emptyList(), IOUtil.LineIterationOptions.builder().offset(-1).count(0).build(), line -> {
                }));
        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class, () -> IOUtil.forEachLine(java.util.Collections.emptyList(),
                IOUtil.LineIterationOptions.builder().count(0).build(), (Throwables.Consumer<String, RuntimeException>) null));
        assertThrows(UncheckedIOException.class, () -> IOUtil.forEachLine(tempFolder.resolve("missing-lines.txt").toFile(), line -> {
        }));
    }

    @Test
    public void testForEachLineReadsANonUtf8Source() throws Exception {
        // Before LineIterationOptions existed, forEachLine could only decode UTF-8, so a Latin-1 file came back with
        // U+FFFD and no error at all.
        final byte[] latin1 = "café\nnaïve\n".getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);

        final File file = new File(tempFolder.toFile(), "latin1.txt");
        Files.write(file.toPath(), latin1);

        final List<String> asUtf8 = new ArrayList<>();
        IOUtil.forEachLine(file, asUtf8::add);
        assertEquals(CommonUtil.asList("caf�", "na�ve"), asUtf8, "the default is still UTF-8");

        final IOUtil.LineIterationOptions options = IOUtil.LineIterationOptions.builder().charset(java.nio.charset.StandardCharsets.ISO_8859_1).build();

        final List<String> fromFile = new ArrayList<>();
        IOUtil.forEachLine(file, options, fromFile::add);
        assertEquals(CommonUtil.asList("café", "naïve"), fromFile);

        // ...and the same charset reaches the Collection<File> and InputStream forms.
        final List<String> fromCollection = new ArrayList<>();
        IOUtil.forEachLine(CommonUtil.asList(file), options, fromCollection::add);
        assertEquals(CommonUtil.asList("café", "naïve"), fromCollection);

        final List<String> fromStream = new ArrayList<>();

        try (InputStream is = new java.io.ByteArrayInputStream(latin1)) {
            IOUtil.forEachLine(is, options, fromStream::add);
        }

        assertEquals(CommonUtil.asList("café", "naïve"), fromStream);

        // The charset combines with the slicing and concurrency fields rather than replacing them.
        final List<String> sliced = new ArrayList<>();
        IOUtil.forEachLine(file, IOUtil.LineIterationOptions.builder().charset(java.nio.charset.StandardCharsets.ISO_8859_1).offset(1).count(1).build(),
                sliced::add);
        assertEquals(CommonUtil.asList("naïve"), sliced);

        // A Reader is already decoded, so the charset field is ignored there - not misapplied.
        final List<String> fromReader = new ArrayList<>();

        try (Reader reader = new java.io.InputStreamReader(new java.io.ByteArrayInputStream(latin1), java.nio.charset.StandardCharsets.ISO_8859_1)) {
            IOUtil.forEachLine(reader, IOUtil.LineIterationOptions.builder().charset(java.nio.charset.StandardCharsets.UTF_16).build(), fromReader::add);
        }

        assertEquals(CommonUtil.asList("café", "naïve"), fromReader);

        // A null charset means the default, not a NullPointerException.
        final List<String> nullCharset = new ArrayList<>();
        IOUtil.forEachLine(file, IOUtil.LineIterationOptions.builder().charset(null).build(), nullCharset::add);
        assertEquals(asUtf8, nullCharset);
    }

    @Test
    public void testForEachLine_DecompressesGzipLikeReadAll() throws Exception {
        // forEachLine is the streaming form of readAllLines, so it answers the same file the same way: a .gz
        // name is decompressed. It used to read the file literally, which handed the caller one "line" of
        // U+FFFD replacement characters - with no error - on exactly the file kind forEachLine exists for.
        final File gzip = tempFolder.resolve("for-lines.gz").toFile();
        try (java.util.zip.GZIPOutputStream output = new java.util.zip.GZIPOutputStream(new FileOutputStream(gzip))) {
            output.write("hello".getBytes(UTF_8));
        }

        assertEquals("hello", IOUtil.readAllToString(gzip));

        final java.util.List<String> lines = new java.util.ArrayList<>();
        IOUtil.forEachLine(gzip, lines::add);

        assertEquals("hello", String.join("", lines));
    }

    @Test
    public void testForEachLine_leadingNumbersAlwaysMeanOffsetAndCount() throws Exception {
        final File f = new File(tempFolder.toFile(), "forlines-overloads.txt");
        Files.write(f.toPath(), "l0\nl1\nl2\nl3\nl4\nl5\n".getBytes(UTF_8));

        // Only one positional shape survives, and its two numbers can only mean offset/count. No overload
        // reads leading numbers as thread counts any more, so adding an argument cannot silently
        // re-interpret the earlier ones - it simply does not compile.
        final List<String> sliced = new ArrayList<>();
        IOUtil.forEachLine(f, 1, 2, sliced::add);
        assertEquals(Arrays.asList("l1", "l2"), sliced);

        // The builder reaches exactly the same slice.
        final List<String> viaBuilder = new ArrayList<>();
        IOUtil.forEachLine(f, IOUtil.LineIterationOptions.builder().offset(1).count(2).build(), viaBuilder::add);
        assertEquals(sliced, viaBuilder);
    }

    @Test
    public void testForEachLine_iterateOptionsNamesEveryValue() throws Exception {
        final File f = new File(tempFolder.toFile(), "forlines-options.txt");
        Files.write(f.toPath(), "l0\nl1\nl2\nl3\nl4\nl5\n".getBytes(UTF_8));

        final List<String> lines = new ArrayList<>();
        IOUtil.forEachLine(f, IOUtil.LineIterationOptions.builder().offset(1).count(2).build(), lines::add);
        assertEquals(Arrays.asList("l1", "l2"), lines);

        // Threads are a separate, explicitly named knob - they no longer disturb the slice.
        final List<String> threaded = Collections.synchronizedList(new ArrayList<String>());
        IOUtil.forEachLine(f, IOUtil.LineIterationOptions.builder().offset(1).count(2).readThreads(1).processThreads(2).queueSize(8).build(), threaded::add);
        assertEquals(2, threaded.size());

        // null options == defaults: everything, on the calling thread.
        final List<String> all = new ArrayList<>();
        IOUtil.forEachLine(f, (IOUtil.LineIterationOptions) null, all::add);
        assertEquals(6, all.size());
    }

    @Test
    public void testForEachLine_iterateOptionsOnCollectionStreamAndReader() throws Exception {
        final File a = new File(tempFolder.toFile(), "opts-a.txt");
        final File b = new File(tempFolder.toFile(), "opts-b.txt");
        Files.write(a.toPath(), "a0\na1\n".getBytes(UTF_8));
        Files.write(b.toPath(), "b0\nb1\n".getBytes(UTF_8));

        final List<String> fromFiles = new ArrayList<>();
        IOUtil.forEachLine(Arrays.asList(a, b), IOUtil.LineIterationOptions.builder().offset(1).count(2).build(), fromFiles::add);
        assertEquals(Arrays.asList("a1", "b0"), fromFiles);

        final List<String> fromStream = new ArrayList<>();
        try (InputStream is = new ByteArrayInputStream("s0\ns1\ns2\n".getBytes(UTF_8))) {
            IOUtil.forEachLine(is, IOUtil.LineIterationOptions.builder().offset(1).build(), fromStream::add);
        }
        assertEquals(Arrays.asList("s1", "s2"), fromStream);

        final List<String> fromReader = new ArrayList<>();
        try (Reader r = new StringReader("r0\nr1\nr2\n")) {
            IOUtil.forEachLine(r, IOUtil.LineIterationOptions.builder().count(2).build(), fromReader::add);
        }
        assertEquals(Arrays.asList("r0", "r1"), fromReader);
    }

    @Test
    public void testForEachLine_iterateOptionsRunsOnCompleteExactlyOnce() throws Exception {
        final File f = new File(tempFolder.toFile(), "opts-oncomplete.txt");
        Files.write(f.toPath(), "x\ny\n".getBytes(UTF_8));

        final AtomicInteger completed = new AtomicInteger();
        final List<String> lines = new ArrayList<>();
        IOUtil.forEachLine(f, IOUtil.LineIterationOptions.builder().build(), lines::add, completed::incrementAndGet);

        assertEquals(Arrays.asList("x", "y"), lines);
        assertEquals(1, completed.get());
    }

    @Test
    public void testForEachLineDoesNotLogSpuriousStreamCloseWarning() throws Exception {
        // Regression: every forEachLine(..) call used to log
        //   WARNING: Remember to close IteratorStream after iteration because it has close handlers
        // because Iterators.forEach built a Stream even for sequential reading and then took
        // stream.skip(..).limit(..).iterator(); every derived stream carries a parent-link close handler, so
        // the check fired even though forEach closes everything it opens. Sequential reading now
        // concatenates and slices the iterators directly, with no Stream involved.
        final File file = new File(tempFolder.toFile(), "no-warn.txt");
        Files.write(file.toPath(), "a\nb\nc\n".getBytes(UTF_8));

        final StringBuilder captured = new StringBuilder();
        final java.util.logging.Logger root = java.util.logging.Logger.getLogger("");
        final java.util.logging.Handler probe = new java.util.logging.Handler() {
            @Override
            public void publish(final java.util.logging.LogRecord r) {
                captured.append(r.getMessage()).append(System.lineSeparator());
            }

            @Override
            public void flush() {
            }

            @Override
            public void close() {
            }
        };

        root.addHandler(probe);

        try {
            final List<String> read = new ArrayList<>();
            IOUtil.forEachLine(file, read::add);
            IOUtil.forEachLine(file, 1, 2, read::add);
            IOUtil.forEachLine(file, IOUtil.LineIterationOptions.builder().offset(1).build(), read::add);
            assertEquals(7, read.size());
        } finally {
            root.removeHandler(probe);
        }

        assertFalse(captured.toString().contains("Remember to close"), "forEachLine must not log a stream close-handler warning; captured: " + captured);
    }
}
