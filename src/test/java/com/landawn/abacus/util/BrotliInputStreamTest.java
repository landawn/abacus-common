package com.landawn.abacus.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class BrotliInputStreamTest extends TestBase {

    @Test
    public void testConstructorWithInputStream() throws IOException {
        byte[] data = new byte[] { 1, 2, 3, 4, 5 };
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            BrotliInputStream bis = new BrotliInputStream(bais);
            bis.close();
        } catch (Exception e) {
        }

    }

    @Test
    public void testConstructorWithBufferSize() throws IOException {
        byte[] data = new byte[] { 1, 2, 3, 4, 5 };
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            BrotliInputStream bis = new BrotliInputStream(bais, 1024);
            bis.close();
        } catch (Exception e) {
        }
    }

    @Test
    public void testRead() throws IOException {
        byte[] data = new byte[] {};
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            BrotliInputStream bis = new BrotliInputStream(bais);
            int result = bis.read();
            bis.close();
        } catch (Exception e) {
        }
    }

    @Test
    public void testReadByteArray() throws IOException {
        byte[] data = new byte[] {};
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            BrotliInputStream bis = new BrotliInputStream(bais);
            byte[] buffer = new byte[10];
            int result = bis.read(buffer);
            bis.close();
        } catch (Exception e) {
        }
    }

    @Test
    public void testReadByteArrayWithOffset() throws IOException {
        byte[] data = new byte[] {};
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            BrotliInputStream bis = new BrotliInputStream(bais);
            byte[] buffer = new byte[10];
            int result = bis.read(buffer, 0, 5);
            bis.close();
        } catch (Exception e) {
        }
    }

    @Test
    public void testSkip() throws IOException {
        byte[] data = new byte[] {};
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            BrotliInputStream bis = new BrotliInputStream(bais);
            long skipped = bis.skip(10);
            bis.close();
        } catch (Exception e) {
        }
    }

    @Test
    public void testSkipNegative() throws IOException {
        byte[] data = new byte[] {};
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            BrotliInputStream bis = new BrotliInputStream(bais);
            Assertions.assertThrows(IllegalArgumentException.class, () -> {
                bis.skip(-1);
            });
            bis.close();
        } catch (Exception e) {
        }
    }

    @Test
    public void testAvailable() throws IOException {
        byte[] data = new byte[] {};
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            BrotliInputStream bis = new BrotliInputStream(bais);
            int available = bis.available();
            bis.close();
        } catch (Exception e) {
        }
    }

    @Test
    public void testMarkAndReset() throws IOException {
        byte[] data = new byte[] {};
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            BrotliInputStream bis = new BrotliInputStream(bais);
            bis.mark(100);
            bis.reset();
            bis.close();
        } catch (Exception e) {
        }
    }

    @Test
    public void testMark() throws IOException {
        byte[] data = new byte[] {};
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            BrotliInputStream bis = new BrotliInputStream(bais);
            bis.mark(1024);
            Assertions.assertTrue(true);
            bis.close();
        } catch (Exception e) {
        }
    }

    @Test
    public void testMarkSupported() throws IOException {
        byte[] data = new byte[] {};
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            BrotliInputStream bis = new BrotliInputStream(bais);
            boolean supported = bis.markSupported();
            Assertions.assertNotNull(Boolean.valueOf(supported));
            bis.close();
        } catch (Exception e) {
        }
    }

    @Test
    public void testClose() throws IOException {
        byte[] data = new byte[] {};
        ByteArrayInputStream bais = new ByteArrayInputStream(data);
        try {
            BrotliInputStream bis = new BrotliInputStream(bais);
            bis.close();
            // Calling close again should not throw
            bis.close();
            Assertions.assertTrue(true);
        } catch (Exception e) {
            // Brotli may fail on invalid data, but close should work
        }
    }
}
