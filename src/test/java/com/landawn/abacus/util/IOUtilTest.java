package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.guava.Files;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.stream.Stream;

@Tag("old-test")
public class IOUtilTest extends AbstractTest {

    @Test
    public void test_read_first_last_line() throws IOException {
        final File file = new File("./src/test/resources/empty_file.txt");
        assertNull(IOUtil.readFirstLine(file));
        assertNull(IOUtil.readLastLine(file));

        try (Reader reader = new FileReader(file)) {
            assertNull(IOUtil.readFirstLine(file));
        }
        try (Reader reader = new FileReader(file)) {
            assertNull(IOUtil.readLastLine(file));
        }
    }

    @Test
    public void test_append() throws IOException {
        final File file = new File("./tmp.txt");
        IOUtil.deleteIfExists(file);

        IOUtil.append("abc", file);
        IOUtil.append("123", file);

        assertEquals("abc123", IOUtil.readAllToString(file));
        IOUtil.deleteIfExists(file);
    }

    @Test
    public void test_freeDiskSpaceKb() {
        N.println(IOUtil.freeDiskSpaceKb());
    }

    @Test
    public void test_list_2() throws Exception {
        final File dir = new File("./src");
        Stream.listFiles(dir, true).filter(f -> f.getName().endsWith(".java")).forEach(Fn.println());
    }

    @Test
    public void test_File() throws Exception {
        final File file = new File("./src/test/resources/test.txt");

        if (file.exists()) {
            file.delete();
        }

        IOUtil.write("abc", file);

        assertEquals("abc", IOUtil.readAllToString(file));
        assertEquals("abc", IOUtil.readFirstLine(file));
    }

    @Test
    public void test_chars2Bytes() throws Exception {
        final char[] chars = "abc124黎".toCharArray();
        final byte[] bytes = IOUtil.chars2Bytes(chars);
        final char[] chars2 = IOUtil.bytes2Chars(bytes);
        assertTrue(CommonUtil.equals(chars, chars2));
    }

    @Test
    public void test_readLines() throws Exception {
        final File file = new File("./src/test/resources/test.txt");

        if (file.exists()) {
            file.delete();
        }

        List<String> lines = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            lines.add("Line#" + i);
        }

        IOUtil.writeLines(lines, file);

        lines = IOUtil.readLines(file, 0, 1);

        N.println(lines);

        lines = IOUtil.readLines(file, 1, 2);

        N.println(lines);

        lines = IOUtil.readLines(file, 98, 100);

        N.println(lines);

        lines = IOUtil.readLines(file, 0, 100);

        N.println(lines);

        file.delete();
    }

    @Test
    public void test_String() throws Exception {
        final String str = "abc";

        final StringBuilder sb = new StringBuilder();
        assertEquals(str, IOUtil.readAllToString(IOUtil.string2InputStream(str)));
        assertEquals(str, IOUtil.readAllToString(IOUtil.string2Reader(str)));
        IOUtil.write(str, IOUtil.stringBuilder2Writer(sb));
        assertEquals(str, sb.toString());
    }

    @Test
    public void test_splite() throws Exception {
        final File dir = new File("./tmpDir");
        IOUtil.deleteAllIfExists(dir);
        dir.mkdir();

        final File file = new File("./tmpDir/test.txt");

        if (file.exists()) {
            file.delete();
        }

        final Writer writer = new FileWriter(file);
        for (int i = 0; i < 1000; i++) {
            IOUtil.write(i + ": " + Strings.uuid() + IOUtil.LINE_SEPARATOR, writer);
        }

        writer.flush();
        IOUtil.close(writer);

        final int numOfParts = 10;

        IOUtil.splitByLine(file, numOfParts);

        final BiPredicate<File, File> filter = (parentDir, file1) -> file1.getName().indexOf('_') > 0;

        final List<File> subFiles = IOUtil.listFiles(dir, true, filter);
        assertEquals(numOfParts, subFiles.size());
        subFiles.forEach(it -> N.println(it.getName()));

        final File destFile = new File("./tmpDir/mregedTest.txt");
        IOUtil.merge(subFiles, destFile);

        assertEquals(IOUtil.readAllToString(file), IOUtil.readAllToString(destFile));

        IOUtil.deleteAllIfExists(dir);
    }

    @Test
    public void test_splite_2() throws Exception {
        final File dir = new File("./tmpDir");
        IOUtil.deleteAllIfExists(dir);
        dir.mkdir();

        final File file = new File("./tmpDir/test.txt");

        if (file.exists()) {
            file.delete();
        }

        final Writer writer = new FileWriter(file);
        for (int i = 0; i < 1000; i++) {
            IOUtil.write(i + ": " + Strings.uuid() + IOUtil.LINE_SEPARATOR, writer);
        }

        writer.flush();
        IOUtil.close(writer);

        IOUtil.split(file, 11);

        final BiPredicate<File, File> filter = (parentDir, file1) -> file1.getName().indexOf('_') > 0;

        final List<File> subFiles = IOUtil.listFiles(dir, true, filter);
        assertEquals(11, subFiles.size());

        final File destFile = new File("./tmpDir/mregedTest.txt");
        IOUtil.merge(subFiles, destFile);

        assertEquals(IOUtil.readAllToString(file), IOUtil.readAllToString(destFile));

        final File zipFile = new File("./tmpDir/test.zip");
        IOUtil.zip(file, zipFile);

        IOUtil.deleteAllIfExists(dir);
    }

    @Test
    public void test_read_write_2() throws Exception {
        int i = 0;

        final File file = new File("./src/test/resources/test.txt");

        if (file.exists()) {
            file.delete();
        }

        file.createNewFile();

        final StringBuilder sb = new StringBuilder();

        while (i <= Objectory.BUFFER_SIZE / 3) {
            final String str = Strings.uuid();
            sb.append(str);
            i += str.length();
        }

        final String str = sb.toString();
        IOUtil.write(str, file);

        String str2 = new String(IOUtil.readAllBytes(file));
        assertEquals(str, str2);

        final InputStream is = new FileInputStream(file);
        str2 = new String(IOUtil.readAllBytes(is));
        assertEquals(str, str2);
        IOUtil.close(is);

        str2 = String.valueOf((IOUtil.readAllChars(file)));
        assertEquals(str, str2);

        str2 = IOUtil.readAllToString(file);
        assertEquals(str, str2);

        IOUtil.deleteAllIfExists(file);
    }

    @Test
    public void test_read_write_3() throws Exception {
        final File file = new File("./src/test/resources/test.txt");

        if (!file.exists()) {
            file.createNewFile();
        }

        final InputStream is = new FileInputStream(file);
        final OutputStream os = new FileOutputStream(file);
        final Reader reader = new FileReader(file);
        final Writer writer = new FileWriter(file);

        IOUtil.close(is);
        IOUtil.close(os);
        IOUtil.close(reader);
        IOUtil.close(writer);

        IOUtil.deleteAllIfExists(file);

        try {
            IOUtil.readAllBytes(file);
            fail("should throw RuntimeException");
        } catch (final UncheckedIOException e) {
        }

        try {
            IOUtil.readAllChars(file);
            fail("should throw RuntimeException");
        } catch (final UncheckedIOException e) {
        }

        try {
            IOUtil.readAllToString(file);
            fail("should throw RuntimeException");
        } catch (final UncheckedIOException e) {
        }

        try {
            IOUtil.readAllLines(file);
            fail("should throw RuntimeException");
        } catch (final UncheckedIOException e) {
        }

        try {
            IOUtil.readAllBytes(is);
            fail("should throw RuntimeException");
        } catch (final UncheckedIOException e) {
        }

        try {
            IOUtil.readAllChars(is);
            fail("should throw RuntimeException");
        } catch (final UncheckedIOException e) {
        }

        try {
            IOUtil.readAllToString(is);
            fail("should throw RuntimeException");
        } catch (final UncheckedIOException e) {
        }

        try {
            IOUtil.readAllLines(is);
            fail("should throw RuntimeException");
        } catch (final UncheckedIOException e) {
        }

        try {
            IOUtil.readAllChars(reader);
            fail("should throw RuntimeException");
        } catch (final UncheckedIOException e) {
        }

        try {
            IOUtil.readAllToString(reader);
            fail("should throw RuntimeException");
        } catch (final RuntimeException e) {
        }

        try {
            IOUtil.readAllLines(reader);
            fail("should throw RuntimeException");
        } catch (final UncheckedIOException e) {
        }

        try {
            IOUtil.write("abc".getBytes(), os);
            fail("should throw RuntimeException");
        } catch (final IOException e) {
        }

        try {
            IOUtil.write("abc".toCharArray(), os);
            fail("should throw RuntimeException");
        } catch (final IOException e) {
        }

        try {
            IOUtil.write("abc", os);
            fail("should throw RuntimeException");
        } catch (final IOException e) {
        }

        try {
            IOUtil.write(is, os);
            fail("should throw RuntimeException");
        } catch (final IOException e) {
        }

        try {
            IOUtil.write(reader, new OutputStreamWriter(os));
            fail("should throw RuntimeException");
        } catch (final IOException e) {
        }

        try {
            IOUtil.write("abc".toCharArray(), writer);
            fail("should throw RuntimeException");
        } catch (final IOException e) {
        }

        try {
            IOUtil.write("abc", writer);
            fail("should throw RuntimeException");
        } catch (final IOException e) {
        }

        try {
            IOUtil.write(new InputStreamReader(is), writer);
            fail("should throw RuntimeException");
        } catch (final IOException e) {
        }

        try {
            IOUtil.write(reader, writer);
            fail("should throw RuntimeException");
        } catch (final IOException e) {
        }

        file.delete();
    }

    @Test
    public void testBufferWriter() throws IOException {
        final com.landawn.abacus.util.BufferedWriter bw = (com.landawn.abacus.util.BufferedWriter) Objectory.createBufferedWriter();

        bw.writeInt(Integer.MIN_VALUE + 1);
        bw.write(IOUtil.LINE_SEPARATOR);
        bw.writeInt(Integer.MAX_VALUE - 1);
        bw.write(IOUtil.LINE_SEPARATOR);

        bw.write(Byte.MAX_VALUE);
        bw.write(IOUtil.LINE_SEPARATOR);
        bw.write(Byte.MIN_VALUE);
        bw.write(IOUtil.LINE_SEPARATOR);

        bw.write(Short.MAX_VALUE);
        bw.write(IOUtil.LINE_SEPARATOR);
        bw.write(Short.MIN_VALUE);
        bw.write(IOUtil.LINE_SEPARATOR);

        bw.writeInt(Integer.MIN_VALUE);
        bw.write(IOUtil.LINE_SEPARATOR);
        bw.writeInt(Integer.MAX_VALUE);
        bw.write(IOUtil.LINE_SEPARATOR);

        bw.writeInt(Integer.MIN_VALUE + 1);
        bw.write(IOUtil.LINE_SEPARATOR);
        bw.writeInt(Integer.MAX_VALUE - 1);
        bw.write(IOUtil.LINE_SEPARATOR);

        bw.write(Long.MAX_VALUE);
        bw.write(IOUtil.LINE_SEPARATOR);
        bw.write(Long.MIN_VALUE);
        bw.write(IOUtil.LINE_SEPARATOR);

        bw.write(Long.MAX_VALUE - 1);
        bw.write(IOUtil.LINE_SEPARATOR);
        bw.write(Long.MIN_VALUE + 1);
        bw.write(IOUtil.LINE_SEPARATOR);

        bw.write(Float.MIN_VALUE);
        bw.write(IOUtil.LINE_SEPARATOR);
        bw.write(Float.MAX_VALUE);
        bw.write(IOUtil.LINE_SEPARATOR);
        bw.write(Double.MAX_VALUE);
        bw.write(IOUtil.LINE_SEPARATOR);
        bw.write(Double.MIN_VALUE);

        N.println(bw.toString());

        Objectory.recycle(bw);
    }

    @Test
    public void test_zipDir() throws IOException {
        final File file = new File("./src/test/resources/test.txt");
        IOUtil.deleteAllIfExists(file);
        IOUtil.write("abc", file);

        final File file2 = new File("./src/test/resources/test2.txt");
        IOUtil.deleteAllIfExists(file2);
        IOUtil.write("123", file2);

        final File targetFile = new File("./test.zip");
        IOUtil.zip(new File("./src/test/resources"), targetFile);

        final File targetDir = new File("./tmpDir");
        IOUtil.unzip(targetFile, targetDir);

        assertEquals(1, IOUtil.listFiles(targetDir).size());

        IOUtil.deleteAllIfExists(file);
        IOUtil.deleteAllIfExists(file2);
        IOUtil.deleteAllIfExists(targetFile);
        IOUtil.deleteAllIfExists(targetDir);
    }

    @Test
    public void test_writeString() throws Exception {
        final File file = new File("./temp.properties");
        final OutputStream os = new FileOutputStream(file);

        IOUtil.write("test", os);
        IOUtil.write("test", os);

        IOUtil.close(os);
        IOUtil.deleteAllIfExists(file);
    }

    @Test
    public void test_delete() throws Exception {
        final File file = new File("./temp.properties");
        IOUtil.write("test", file);

        assertTrue(IOUtil.deleteAllIfExists(file));
        assertFalse(IOUtil.deleteAllIfExists(file));
    }

    @Test
    public void test_read_write_file() throws Exception {
        final File file = new File("./src/test/resources/test.txt");

        if (file.exists()) {
            file.delete();
        }

        file.createNewFile();

        long startTime = System.currentTimeMillis();

        final OutputStream os = new BufferedOutputStream(new FileOutputStream(file), 80920);
        final byte[] bytes = (Strings.uuid() + "\n").getBytes();

        for (int i = 0; i < 10000000; i++) {
            os.write(bytes);
        }

        os.close();

        N.println("########### write: " + (System.currentTimeMillis() - startTime));
        startTime = System.currentTimeMillis();

        final Random rand = new Random(100000000);

        for (int i = 0; i < 10000; i++) {
            final RandomAccessFile raf = new RandomAccessFile(file, "r");
            int lineNum = rand.nextInt();
            lineNum = (lineNum < 0) ? (-lineNum) : lineNum;
            raf.seek(lineNum);
            raf.read(bytes);

            raf.close();
        }

        N.println("########### random read: " + (System.currentTimeMillis() - startTime));

        file.delete();
    }

    @Test
    public void test_renmae() throws IOException {
        final File srcFile = new File("./srcFile.txt");
        final String str = "dajfeoiwafldji";
        IOUtil.write(str, srcFile);

        IOUtil.renameTo(srcFile, "newSrcFile.txt");
        final File newSrcFile = new File("./newSrcFile.txt");

        assertEquals(str, IOUtil.readAllToString(newSrcFile));

        IOUtil.deleteAllIfExists(newSrcFile);
    }

    @Test
    public void test_copyFile() throws IOException {
        File srcFile = new File("./srcFile.txt");
        IOUtil.write("dajfeoiwafldji", srcFile);

        File destDir = new File("./dir");

        IOUtil.deleteAllIfExists(destDir);

        IOUtil.copyToDirectory(srcFile, destDir);

        final File newDestFile = new File("./dir2");

        IOUtil.deleteAllIfExists(newDestFile);

        IOUtil.move(destDir, newDestFile);

        IOUtil.deleteAllIfExists(newDestFile);

        destDir = new File(".");
        IOUtil.copyToDirectory(srcFile, destDir);
        destDir.delete();

        srcFile.delete();

        srcFile = new File("Copy of srcFile.txt");
        srcFile.delete();
    }

    @Test
    public void test_copyDir() throws IOException {
        final File srcFile = new File("./tempDir");
        IOUtil.deleteAllIfExists(srcFile);
        srcFile.mkdir();

        final File subDir = new File("./tempDir/subDir");
        subDir.mkdir();

        final File file1 = new File("./tempDir/file1.txt");
        IOUtil.write("file1", file1);

        final File file2 = new File("./tempDir/file2.txt");
        IOUtil.write("file2", file2);

        final File file3 = new File("./tempDir/subDir/file3.txt");
        IOUtil.write("file3", file3);

        final File destDir = new File("./tempDir2");
        IOUtil.deleteAllIfExists(destDir);
        destDir.mkdir();

        IOUtil.copyToDirectory(srcFile, destDir);

        assertEquals("file1", IOUtil.readAllToString(new File("./tempDir2/tempDir/file1.txt")));
        assertEquals("file2", IOUtil.readAllToString(new File("./tempDir2/tempDir/file2.txt")));
        assertEquals("file3", IOUtil.readAllToString(new File("./tempDir2/tempDir/subDir/file3.txt")));

        IOUtil.deleteAllIfExists(new File("./tempDir2/tempDir/subDir"));

        IOUtil.copyToDirectory(srcFile, destDir, true, (parentDir, file) -> file.getName().equals("subDir"));

        assertEquals("file3", IOUtil.readAllToString(new File("./tempDir2/tempDir/subDir/file3.txt")));

        IOUtil.deleteFilesFromDirectory(new File("./tempDir"), (parentDir, file) -> file.getName().equals("subDir"));

        IOUtil.copyToDirectory(srcFile, destDir, true, (parentDir, file) -> file.getName().equals("subDir"));

        try {
            IOUtil.copyToDirectory(srcFile, subDir);
            fail("Should throw IllegalArgumentException");
        } catch (final IllegalArgumentException e) {

        }

        IOUtil.copyToDirectory(subDir, srcFile);

        IOUtil.deleteAllIfExists(srcFile);
        IOUtil.deleteAllIfExists(destDir);
    }

    @Test
    public void test_readString_2() throws IOException {

        List<String> lines = CommonUtil.asList("abc", "1海洋23", "ef😀g😁");
        String allInString = Strings.join(lines, IOUtil.LINE_SEPARATOR) + IOUtil.LINE_SEPARATOR;

        File file = new File("./test.txt");

        IOUtil.writeLines(lines, file);

        assertEquals("abc", IOUtil.readFirstLine(file));
        assertEquals("ef😀g😁", IOUtil.readLastLine(file));
        assertEquals(lines, IOUtil.readAllLines(file));
        assertEquals(lines, Files.readAllLines(file));
        assertEquals(Files.readAllLines(file), IOUtil.readAllLines(file));

        assertEquals(Files.readString(file), IOUtil.readAllToString(file));
        assertArrayEquals(Files.readAllBytes(file), IOUtil.readAllBytes(file));
        assertEquals(allInString, Files.readString(file));
        assertEquals(allInString, IOUtil.readAllToString(file));

        IOUtil.deleteAllIfExists(file);
    }
}
