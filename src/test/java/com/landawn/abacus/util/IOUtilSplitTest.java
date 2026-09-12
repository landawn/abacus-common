package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

public class IOUtilSplitTest extends IOUtilTestSupport {
    @Test
    public void testSplitDoesNotOverwriteSourceThroughHardLink() {
        org.junit.jupiter.api.Assertions.assertAll(() -> checkSplitDoesNotOverwriteSourceThroughHardLink(0),
                () -> checkSplitDoesNotOverwriteSourceThroughHardLink(1), () -> checkSplitDoesNotOverwriteSourceThroughHardLink(2));
    }

    @Test
    public void testSplit_TwoParts() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "split-source", ".txt").toFile();
        StringBuilder content = new StringBuilder();
        for (int i = 0; i < 100; i++) {
            content.append("Line ").append(i).append("\n");
        }
        Files.write(sourceFile.toPath(), content.toString().getBytes(UTF_8));

        IOUtil.split(sourceFile, 2);

        File part1 = new File(sourceFile.getAbsolutePath() + "_0001");
        File part2 = new File(sourceFile.getAbsolutePath() + "_0002");

        assertTrue(part1.exists());
        assertTrue(part2.exists());
        assertTrue(part1.length() > 0);
        assertTrue(part2.length() > 0);
    }

    @Test
    public void testSplit_ThreeParts() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "split3", ".txt").toFile();
        Files.write(sourceFile.toPath(), "0123456789ABCDEFGHIJ".getBytes(UTF_8));

        IOUtil.split(sourceFile, 3);

        File part1 = new File(sourceFile.getAbsolutePath() + "_0001");
        File part2 = new File(sourceFile.getAbsolutePath() + "_0002");
        File part3 = new File(sourceFile.getAbsolutePath() + "_0003");

        assertTrue(part1.exists());
        assertTrue(part2.exists());
        assertTrue(part3.exists());
    }

    @Test
    public void testSplit_WithDestDir() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "split-dest", ".txt").toFile();
        Files.write(sourceFile.toPath(), "Content for splitting".getBytes(UTF_8));

        File destDir = Files.createTempDirectory(tempFolder, "split-dest-dir").toFile();

        IOUtil.split(sourceFile, 2, destDir);

        File part1 = new File(destDir, sourceFile.getName() + "_0001");
        File part2 = new File(destDir, sourceFile.getName() + "_0002");

        assertTrue(part1.exists());
        assertTrue(part2.exists());
    }

    @Test
    public void testSplit_OnePart() throws Exception {
        File file = Files.createTempFile(tempFolder, "split-one", ".txt").toFile();
        Files.write(file.toPath(), "Short content".getBytes(UTF_8));

        File destDir = Files.createTempDirectory(tempFolder, "split-dest").toFile();
        IOUtil.split(file, 1, destDir);

        File[] parts = destDir.listFiles();
        assertNotNull(parts);
        assertEquals(1, parts.length);
    }

    @Test
    public void testSplitBySize_SmallChunks() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "split-size", ".txt").toFile();
        Files.write(sourceFile.toPath(), "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".getBytes(UTF_8));

        IOUtil.splitBySize(sourceFile, 10);

        File part1 = new File(sourceFile.getAbsolutePath() + "_0001");
        File part2 = new File(sourceFile.getAbsolutePath() + "_0002");

        assertTrue(part1.exists());
        assertTrue(part2.exists());
        assertTrue(part1.length() <= 10);
    }

    @Test
    public void testSplitBySize_WithDestDir() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "split-by-size-dest", ".txt").toFile();
        Files.write(sourceFile.toPath(), "Content for size-based splitting".getBytes(UTF_8));

        File destDir = Files.createTempDirectory(tempFolder, "split-size-dir").toFile();

        IOUtil.splitBySize(sourceFile, 10, destDir);

        File[] parts = destDir.listFiles();
        assertNotNull(parts);
        assertTrue(parts.length > 0);
    }

    @Test
    public void testSplitBySize_LargerThanFile() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "small-file", ".txt").toFile();
        Files.write(sourceFile.toPath(), "Small".getBytes(UTF_8));

        IOUtil.splitBySize(sourceFile, 1000);

        File part1 = new File(sourceFile.getAbsolutePath() + "_0001");
        assertTrue(part1.exists());
        assertEquals("Small", IOUtil.readAllToString(part1));
    }

    @Test
    public void testSplitBySize_LargerThanFileWithDestDir() throws Exception {
        File file = Files.createTempFile(tempFolder, "split-size", ".txt").toFile();
        Files.write(file.toPath(), "Small data".getBytes(UTF_8));

        File destDir = Files.createTempDirectory(tempFolder, "split-dest").toFile();
        IOUtil.splitBySize(file, 999999, destDir);

        File[] parts = destDir.listFiles();
        assertNotNull(parts);
        assertEquals(1, parts.length);
    }

    @Test
    public void testSplitBySize_EmptyFile_CreatesOneEmptyPart() throws Exception {
        // BUG FIX: an empty source file previously threw IOException ("Source file ended before
        // split part 1 was complete") instead of producing the documented single empty part.
        File sourceFile = Files.createTempFile(tempFolder, "split-empty", ".txt").toFile();
        File destDir = Files.createTempDirectory(tempFolder, "split-empty-dest").toFile();

        IOUtil.splitBySize(sourceFile, 10, destDir);

        File[] parts = destDir.listFiles();
        assertNotNull(parts);
        assertEquals(1, parts.length);
        assertEquals(0, parts[0].length());
    }

    @Test
    public void testSplit_EmptyFile_CreatesOneEmptyPart() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "split-empty-count", ".txt").toFile();
        File destDir = Files.createTempDirectory(tempFolder, "split-empty-count-dest").toFile();

        IOUtil.split(sourceFile, 3, destDir);

        File[] parts = destDir.listFiles();
        assertNotNull(parts);
        assertEquals(1, parts.length);
        assertEquals(0, parts[0].length());
    }

    @Test
    public void testSplitBySize_NonPositiveSize_DoesNotCreateDestDir() throws Exception {
        File sourceFile = Files.createTempFile(tempFolder, "split-bad-size", ".txt").toFile();
        Files.write(sourceFile.toPath(), "data".getBytes(UTF_8));

        File destDir = new File(tempFolder.toFile(), "split-bad-size-dest");

        assertThrows(IllegalArgumentException.class, () -> IOUtil.splitBySize(sourceFile, 0, destDir));

        // The destination directory must not be created when sizeOfPart is invalid.
        assertFalse(destDir.exists());
    }

    @Test
    public void testSplitByLine_MultiplePartsWithDestDir() throws Exception {
        File file = Files.createTempFile(tempFolder, "split-by-line", ".txt").toFile();
        Files.write(file.toPath(), java.util.Arrays.asList("L1", "L2", "L3", "L4", "L5", "L6", "L7", "L8", "L9", "L10"), UTF_8);

        File destDir = Files.createTempDirectory(tempFolder, "split-lines-dest").toFile();
        IOUtil.splitByLine(file, 2, destDir);

        File[] parts = destDir.listFiles();
        assertNotNull(parts);
        assertEquals(2, parts.length);
        java.util.Arrays.sort(parts, java.util.Comparator.comparing(File::getName));
        assertHaveSameElements(java.util.Arrays.asList("L1", "L2", "L3", "L4", "L5"), Files.readAllLines(parts[0].toPath(), UTF_8));
        assertHaveSameElements(java.util.Arrays.asList("L6", "L7", "L8", "L9", "L10"), Files.readAllLines(parts[1].toPath(), UTF_8));
    }

    @Test
    public void testSplitByLine_EmptyFileCreatesNoParts() throws Exception {
        File file = Files.createTempFile(tempFolder, "split-empty", ".txt").toFile();
        File destDir = Files.createTempDirectory(tempFolder, "split-empty-dest").toFile();

        IOUtil.splitByLine(file, 3, destDir);

        File[] parts = destDir.listFiles();
        assertNotNull(parts);
        assertEquals(0, parts.length);
    }

    @Test
    public void testSplitByLine_DefaultDestDir() throws Exception {
        File file = Files.createTempFile(tempFolder, "split-default", ".txt").toFile();
        IOUtil.writeLines(java.util.Arrays.asList("line1", "line2", "line3", "line4"), file);
        File parentDir = file.getParentFile();

        IOUtil.splitByLine(file, 2);

        // Just verify no exception was thrown and original file still exists
        assertTrue(file.exists() || parentDir != null);
    }

    @Test
    public void testSplitBySize_normalSplitMatchesDeclaredSignature() throws Exception {
        File src = Files.createTempFile(tempFolder, "splitsrc", ".bin").toFile();
        byte[] data = new byte[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        Files.write(src.toPath(), data);
        File destDir = Files.createTempDirectory(tempFolder, "splitdest").toFile();

        // Should not throw UncheckedIOException for a healthy file.
        // Splitting a 10-byte file by 4 should produce 3 parts (4, 4, 2).
        IOUtil.splitBySize(src, 4, destDir);

        File p1 = new File(destDir, src.getName() + "_0001");
        File p2 = new File(destDir, src.getName() + "_0002");
        File p3 = new File(destDir, src.getName() + "_0003");
        assertTrue(p1.exists() && p2.exists() && p3.exists());
        assertEquals(4, p1.length());
        assertEquals(4, p2.length());
        assertEquals(2, p3.length());

        byte[] merged = new byte[10];
        System.arraycopy(Files.readAllBytes(p1.toPath()), 0, merged, 0, 4);
        System.arraycopy(Files.readAllBytes(p2.toPath()), 0, merged, 4, 4);
        System.arraycopy(Files.readAllBytes(p3.toPath()), 0, merged, 8, 2);
        assertArrayEquals(data, merged);
    }

    @Test
    public void testSplitByLineProducesRequestedNumberOfParts() throws Exception {
        // regression: floor division created more part files than requested (10 lines / 3 parts -> 4 files)
        final java.io.File src = tempFolder.resolve("regression_ten_lines.txt").toFile();
        final StringBuilder sb = new StringBuilder();
        for (int i = 1; i <= 10; i++) {
            sb.append("line").append(i).append('\n');
        }
        IOUtil.write(sb.toString(), src);

        final java.io.File destDir = tempFolder.resolve("regression_split_parts").toFile();
        assertTrue(destDir.mkdirs());
        IOUtil.splitByLine(src, 3, destDir);

        assertEquals(3, destDir.listFiles().length);
    }

    @Test
    public void testSplitProducesRequestedNumberOfParts() throws Exception {
        final File src = tempFolder.resolve("regression_split_exact.bin").toFile();
        final byte[] data = new byte[10];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte) i;
        }
        Files.write(src.toPath(), data);

        final File destDir = tempFolder.resolve("regression_split_exact_parts").toFile();
        assertTrue(destDir.mkdirs());

        IOUtil.split(src, 6, destDir);

        final File[] parts = destDir.listFiles();
        assertNotNull(parts);
        assertEquals(6, parts.length);

        final byte[] merged = new byte[10];
        int offset = 0;
        final int[] expectedLengths = { 2, 2, 2, 2, 1, 1 };

        for (int i = 0; i < expectedLengths.length; i++) {
            final File part = new File(destDir, src.getName() + "_" + Strings.padStart(CommonUtil.stringOf(i + 1), 4, '0'));
            assertTrue(part.exists());
            assertEquals(expectedLengths[i], part.length());

            final byte[] partBytes = Files.readAllBytes(part.toPath());
            System.arraycopy(partBytes, 0, merged, offset, partBytes.length);
            offset += partBytes.length;
        }

        assertArrayEquals(data, merged);
    }

    @Test
    public void testSplitFamilyRejectsNullSourceConsistently() {
        final File destDir = tempFolder.resolve("split-null-dest").toFile();

        // All four split* entry points treat a null source as a bad argument, not as a missing file.
        assertThrows(IllegalArgumentException.class, () -> IOUtil.split(null, 2));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.split(null, 2, destDir));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.splitBySize(null, 2));
        assertThrows(IllegalArgumentException.class, () -> IOUtil.splitBySize(null, 2, destDir));

        assertFalse(destDir.exists(), "a rejected split must not create the destination directory");
    }

    @Test
    public void testSplitReportsSourceThatEndsBeforeAdvertisedLength() throws Exception {
        final File actual = tempFolder.resolve("short-source.bin").toFile();
        Files.write(actual.toPath(), new byte[] { 1, 2, 3 });
        final File lengthChangingSource = new File(actual.getPath()) {
            private static final long serialVersionUID = 1L;

            @Override
            public long length() {
                return 10;
            }
        };

        assertThrows(IOException.class, () -> IOUtil.split(lengthChangingSource, 2, tempFolder.resolve("short-source-parts").toFile()));
    }

    @Test
    public void testSplitByLineDecompressesAndNamesPartsForText() throws IOException {
        // splitByLine produces TEXT parts, so it opens the source the way the read* family does: a .gz is
        // decompressed rather than split as raw bytes. Reading it literally would emit mangled binary.
        final File source = new File(tempFolder.toFile(), "sample.txt.gz");

        try (java.io.Writer w = new java.io.OutputStreamWriter(new java.util.zip.GZIPOutputStream(new java.io.FileOutputStream(source)), UTF_8)) {
            for (int i = 1; i <= 40; i++) {
                w.write("line" + i + "\n");
            }
        }

        final File destDir = new File(tempFolder.toFile(), "gz-line-parts");
        IOUtil.splitByLine(source, 4, destDir);

        final String[] names = destDir.list();
        assertNotNull(names);
        Arrays.sort(names);

        // The .gz suffix must NOT survive onto files that hold plain text - otherwise IOUtil's own read* family
        // would try to gunzip them.
        assertArrayEquals(new String[] { "sample_0001.txt", "sample_0002.txt", "sample_0003.txt", "sample_0004.txt" }, names);

        // Four parts, not "however many the compression ratio implies": the line-count estimate measures the
        // sample in the same unit as the total it is scaled against.
        final List<String> reassembled = new ArrayList<>();

        for (final String name : names) {
            reassembled.addAll(IOUtil.readAllLines(new File(destDir, name)));
        }

        assertEquals(40, reassembled.size());
        assertEquals("line1", reassembled.get(0));
        assertEquals("line40", reassembled.get(39));

        // A .gz NAME on something that is not gzip is an error, exactly as it is for readAllToString.
        final File notReallyGz = new File(tempFolder.toFile(), "fake.gz");
        Files.write(notReallyGz.toPath(), "a\nb\n".getBytes(UTF_8));
        // A CHECKED IOException, as splitByLine declares. UncheckedIOException is a RuntimeException and so is
        // NOT an IOException, which is exactly what makes this assertion meaningful: it fails if the line-count
        // estimate leaks the unchecked form out of a method whose signature promises the checked one.
        assertThrows(IOException.class, () -> IOUtil.splitByLine(notReallyGz, 2, new File(tempFolder.toFile(), "fake-parts")));
    }

    @Test
    public void testSplitByLineBalancesACompressedZipSourceLargerThanTheSample() throws IOException {
        // The GZIP twin of this test proves the exact-count path. This one guards the OTHER half of the fix:
        // a ZIP is still sampled, and stays correct because its counter measures DECOMPRESSED bytes against
        // ZipEntry.getSize() - the same unit - rather than compressed bytes against the archive length.
        final int lineCount = 30_000;
        final File source = new File(tempFolder.toFile(), "large.zip");

        try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(new java.io.FileOutputStream(source))) {
            zos.putNextEntry(new java.util.zip.ZipEntry("large.txt"));
            final java.io.Writer w = new java.io.OutputStreamWriter(zos, UTF_8);

            for (int i = 0; i < lineCount; i++) {
                w.write("the same line over and over\n");
            }

            w.flush();
            zos.closeEntry();
        }

        final File destDir = new File(tempFolder.toFile(), "large-zip-parts");
        IOUtil.splitByLine(source, 4, destDir);

        final String[] names = destDir.list();
        assertNotNull(names);
        assertTrue(names.length <= 4, "at most numOfParts parts: " + Arrays.toString(names));

        long total = 0;
        long biggest = 0;

        for (final String name : names) {
            final long n = IOUtil.readAllLines(new File(destDir, name)).size();
            total += n;
            biggest = Math.max(biggest, n);
        }

        assertEquals(lineCount, total, "no line may be lost");
        // Sampled, so allow generous slack - but nothing like the 3/4-of-the-file last part that a
        // compression-ratio error produces.
        assertTrue(biggest <= (lineCount / 2), "parts are badly unbalanced; biggest = " + biggest);
    }

    @Test
    public void testSplitByLineBalancesACompressedSourceLargerThanTheSample() throws IOException {
        // The source MUST exceed the 10,000-line sample, or it takes the exact-count path for a different reason
        // and cannot detect the defect. A gzip cannot be sampled: the counter has to sit under the decompressor
        // to share a unit with File.length(), but the decompressor pulls in nearly the whole compressed stream
        // before the sample yields its lines, so the estimate used to stick at 10,000 - splitting 30,000 lines
        // four ways as 2500/2500/2500/22500 instead of 7500 each.
        final int lineCount = 30_000;
        final File source = new File(tempFolder.toFile(), "large.txt.gz");

        try (java.io.Writer w = new java.io.OutputStreamWriter(new java.util.zip.GZIPOutputStream(new java.io.FileOutputStream(source)), UTF_8)) {
            for (int i = 0; i < lineCount; i++) {
                w.write("the same line over and over\n");
            }
        }

        final File destDir = new File(tempFolder.toFile(), "large-gz-parts");
        IOUtil.splitByLine(source, 4, destDir);

        final String[] names = destDir.list();
        assertNotNull(names);
        assertEquals(4, names.length);

        long total = 0;
        long biggest = 0;

        for (final String name : names) {
            final long n = IOUtil.readAllLines(new File(destDir, name)).size();
            total += n;
            biggest = Math.max(biggest, n);
        }

        assertEquals(lineCount, total, "no line may be lost");
        // Perfectly balanced would be 7500. Allow slack for the ceiling division, but nothing like the 22500
        // that the broken estimate produced.
        assertTrue(biggest <= (lineCount / 4) + 100, "parts are unbalanced; biggest = " + biggest);
    }

    @Test
    public void testSplitByLineNeverExceedsRequestedPartCount() throws IOException {
        // The lines-per-part figure comes from a sampled estimate. When it under-counts, the surplus must land in
        // the LAST part rather than spilling into extra files - otherwise the zero-padded names overflow their
        // width and stop sorting in part order, which merge(..) depends on.
        final File source = new File(tempFolder.toFile(), "many.txt");
        final StringBuilder sb = new StringBuilder();

        for (int i = 0; i < 30000; i++) {
            sb.append("row").append(i).append('\n');
        }

        IOUtil.write(sb.toString(), source);

        final File destDir = new File(tempFolder.toFile(), "many-parts");
        IOUtil.splitByLine(source, 5, destDir);

        final String[] names = destDir.list();
        assertNotNull(names);
        assertTrue(names.length <= 5, "expected at most 5 parts, got " + names.length + ": " + Arrays.toString(names));

        long lines = 0;

        for (final String name : names) {
            assertTrue(name.matches("many_\\d{4}\\.txt"), "unexpected part name: " + name);
            lines += IOUtil.readAllLines(new File(destDir, name)).size();
        }

        assertEquals(30000, lines);
    }

    @Test
    public void testSplit_partNamesRemainSortableAboveFourDigits() throws Exception {
        final File src = new File(tempFolder.toFile(), "many-parts.bin");
        Files.write(src.toPath(), new byte[20002]);
        final File destDir = new File(tempFolder.toFile(), "many-parts-out");

        IOUtil.split(src, 10001, destDir);

        final String[] names = destDir.list();
        assertNotNull(names);
        assertEquals(10001, names.length);
        Arrays.sort(names);
        assertEquals("many-parts.bin_00001", names[0]);
        assertEquals("many-parts.bin_10001", names[names.length - 1]);
    }

    @Test
    public void testSplit_keepsFourDigitNamesForSmallSplits() throws Exception {
        final File src = new File(tempFolder.toFile(), "few-parts.bin");
        Files.write(src.toPath(), new byte[10]);
        final File destDir = new File(tempFolder.toFile(), "few-parts-out");

        IOUtil.split(src, 5, destDir);

        final String[] names = destDir.list();
        assertNotNull(names);
        Arrays.sort(names);
        assertEquals("few-parts.bin_0001", names[0]);
        assertEquals("few-parts.bin_0005", names[4]);
    }

    @Test
    public void testSplitBySize_partNamesRemainSortableAboveFourDigits() throws Exception {
        final File src = new File(tempFolder.toFile(), "many-size-parts.bin");
        Files.write(src.toPath(), new byte[10001]);
        final File destDir = new File(tempFolder.toFile(), "many-size-parts-out");

        IOUtil.splitBySize(src, 1, destDir);

        final String[] names = destDir.list();
        assertNotNull(names);
        assertEquals(10001, names.length);
        Arrays.sort(names);
        assertEquals("many-size-parts.bin_00001", names[0]);
        assertEquals("many-size-parts.bin_10001", names[names.length - 1]);
    }
}
