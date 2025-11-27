package com.landawn.abacus.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Arrays;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.output.ByteArrayOutputStream;

import junit.framework.AssertionFailedError;
import junit.framework.TestCase;

public abstract class FileBasedTestCase extends TestCase {

    private static volatile File testDir;

    public static File getTestDirectory() {
        if (testDir == null) {
            testDir = new File("test/io/").getAbsoluteFile();
        }
        testDir.mkdirs();
        return testDir;
    }

    protected void createFile(File file, long size) throws IOException {
        if (!file.getParentFile().exists()) {
            throw new IOException("Cannot create file " + file + " as the parent directory does not exist");
        }
        BufferedOutputStream output = new BufferedOutputStream(new java.io.FileOutputStream(file));
        try {
            generateTestData(output, size);
        } finally {
            IOUtils.closeQuietly(output);
        }
    }

    protected byte[] generateTestData(long size) {
        try {
            ByteArrayOutputStream baout = new ByteArrayOutputStream();
            generateTestData(baout, size);
            return baout.toByteArray();
        } catch (IOException ioe) {
            throw new RuntimeException("This should never happen: " + ioe.getMessage());
        }
    }

    protected void generateTestData(OutputStream out, long size) throws IOException {
        for (int i = 0; i < size; i++) {

            out.write((byte) ((i % 127) + 1));
        }
    }

    protected void createLineBasedFile(File file, String[] data) throws IOException {
        if (file.getParentFile() != null && !file.getParentFile().exists()) {
            throw new IOException("Cannot create file " + file + " as the parent directory does not exist");
        }
        PrintWriter output = new PrintWriter(new OutputStreamWriter(new FileOutputStream(file), "UTF-8"));
        try {
            for (String element : data) {
                output.println(element);
            }
        } finally {
            IOUtils.closeQuietly(output);
        }
    }

    protected File newFile(String filename) throws IOException {
        File destination = new File(getTestDirectory(), filename);
        if (destination.exists()) {
            FileUtils.forceDelete(destination);
        }
        return destination;
    }

    protected void checkFile(File file, File referenceFile) throws Exception {
        assertTrue("Check existence of output file", file.exists());
        assertEqualContent(referenceFile, file);
    }

    private void assertEqualContent(File f0, File f1) throws IOException {
        InputStream is0 = new java.io.FileInputStream(f0);
        try (is0) {
            InputStream is1 = new java.io.FileInputStream(f1);
            try {
                byte[] buf0 = new byte[1024];
                byte[] buf1 = new byte[1024];
                int n0 = 0;
                int n1 = 0;

                while (-1 != n0) {
                    n0 = is0.read(buf0);
                    n1 = is1.read(buf1);
                    assertTrue("The files " + f0 + " and " + f1 + " have differing number of bytes available (" + n0 + " vs " + n1 + ")", (n0 == n1));

                    assertTrue("The files " + f0 + " and " + f1 + " have different content", Arrays.equals(buf0, buf1));
                }
            } finally {
                is1.close();
            }
        }
    }

    protected void assertEqualContent(byte[] b0, File file) throws IOException {
        InputStream is = new java.io.FileInputStream(file);
        int count = 0, numRead = 0;
        byte[] b1 = new byte[b0.length];
        try {
            while (count < b0.length && numRead >= 0) {
                numRead = is.read(b1, count, b0.length);
                count += numRead;
            }
            assertEquals("Different number of bytes: ", b0.length, count);
            for (int i = 0; i < count; i++) {
                assertEquals("byte " + i + " differs", b0[i], b1[i]);
            }
        } finally {
            is.close();
        }
    }

    protected void assertEqualContent(char[] c0, File file) throws IOException {
        Reader ir = new java.io.FileReader(file);
        int count = 0, numRead = 0;
        char[] c1 = new char[c0.length];
        try {
            while (count < c0.length && numRead >= 0) {
                numRead = ir.read(c1, count, c0.length);
                count += numRead;
            }
            assertEquals("Different number of chars: ", c0.length, count);
            for (int i = 0; i < count; i++) {
                assertEquals("char " + i + " differs", c0[i], c1[i]);
            }
        } finally {
            ir.close();
        }
    }

    protected void checkWrite(OutputStream output) throws Exception {
        try {
            new java.io.PrintStream(output).write(0);
        } catch (Throwable t) {
            throw new AssertionFailedError("The copy() method closed the stream " + "when it shouldn't have. " + t.getMessage());
        }
    }

    protected void checkWrite(Writer output) throws Exception {
        try {
            new java.io.PrintWriter(output).write('a');
        } catch (Throwable t) {
            throw new AssertionFailedError("The copy() method closed the stream " + "when it shouldn't have. " + t.getMessage());
        }
    }

    protected void deleteFile(File file) throws Exception {
        if (file.exists()) {
            assertTrue("Couldn't delete file: " + file, file.delete());
        }
    }

}
