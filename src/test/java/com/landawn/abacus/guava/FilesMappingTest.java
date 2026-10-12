package com.landawn.abacus.guava;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel.MapMode;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import org.junit.jupiter.api.Test;

/** Files mapping contracts. */
@SuppressWarnings("resource")
public class FilesMappingTest extends FilesTestSupport {

    @Test
    public void testMap() throws IOException {
        File file = file("map.dat");
        byte[] data = "0123456789".getBytes(StandardCharsets.US_ASCII);
        Files.write(data, file);

        MappedByteBuffer readOnly = Files.map(file);
        byte[] read = new byte[data.length];
        readOnly.get(read);
        assertArrayEquals(data, read);
        unmap(readOnly);

        MappedByteBuffer readWrite = Files.map(file, MapMode.READ_WRITE);
        readWrite.put(0, (byte) 'X');
        readWrite.force();
        unmap(readWrite);
        assertEquals((byte) 'X', java.nio.file.Files.readAllBytes(file.toPath())[0]);

        File sized = file("map-sized.dat");
        MappedByteBuffer sizedBuffer = Files.map(sized, MapMode.READ_WRITE, 20);
        assertEquals(20, sized.length());
        assertEquals(20, sizedBuffer.capacity());
        unmap(sizedBuffer);

        assertThrows(FileNotFoundException.class, () -> Files.map(file("missing-map.dat")));
        assertThrows(IllegalArgumentException.class, () -> Files.map(file, MapMode.READ_WRITE, -1));
        assertThrows(IllegalArgumentException.class, () -> Files.map(file, MapMode.READ_ONLY, Integer.MAX_VALUE + 1L));
        assertThrows(NullPointerException.class, () -> Files.map(file, null));
    }

    @Test
    public void testMap_MissingFile() throws IOException {
        File missingRw = file("missing-rw.bin");
        MappedByteBuffer rw = Files.map(missingRw, MapMode.READ_WRITE);
        assertEquals(0, rw.capacity());
        assertTrue(missingRw.exists());

        File missingPrivate = file("missing-private.bin");
        MappedByteBuffer priv = Files.map(missingPrivate, MapMode.PRIVATE);
        assertEquals(0, priv.capacity());
        assertTrue(missingPrivate.exists());

        File missingRo = file("missing-ro.bin");
        assertThrows(FileNotFoundException.class, () -> Files.map(missingRo, MapMode.READ_ONLY));
        assertThrows(FileNotFoundException.class, () -> Files.map(missingRo, MapMode.READ_ONLY, 10));
        assertFalse(missingRo.exists());
    }

    @Test
    public void testMap_SizeBeyondLength() throws IOException {
        File file = file("extend.bin");
        Files.write(new byte[] { 1, 2, 3 }, file);
        long len = file.length();
        IOException ex = assertThrows(IOException.class, () -> Files.map(file, MapMode.READ_ONLY, len + 1));
        assertFalse(ex instanceof FileNotFoundException);
        assertEquals(len, file.length());

        MappedByteBuffer buffer = Files.map(file, MapMode.READ_WRITE, 10);
        assertEquals(10, buffer.capacity());
        assertEquals(10L, file.length());
        unmap(buffer);
    }

    // ---------------------------------------------------------------- C-362

    @Test
    public void testC362_mapOversizeRejectedBeforeFileIsCreated() {
        for (final MapMode mode : Arrays.asList(MapMode.READ_WRITE, MapMode.PRIVATE, MapMode.READ_ONLY)) {
            for (final long size : new long[] { Integer.MAX_VALUE + 1L, Long.MAX_VALUE }) {
                final File missing = file("m_big_" + mode + "_" + size + ".dat");

                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Files.map(missing, mode, size));
                assertTrue(e.getMessage().contains(String.valueOf(size)), e.getMessage());
                assertFalse(missing.exists(), mode + " created " + missing);
            }
        }
    }

    @Test
    public void testC362_mapNegativeRejectedBeforeFileIsCreated() {
        for (final long size : new long[] { -1, Long.MIN_VALUE }) {
            final File missing = file("m_neg.dat");

            final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Files.map(missing, MapMode.READ_WRITE, size));
            assertEquals("size (" + size + ") may not be negative", e.getMessage());
            assertFalse(missing.exists());
        }
    }

    @Test
    public void testC362_mapBoundarySizesStillDelegate() throws IOException {
        // size 0 on a missing file in READ_WRITE mode still creates an empty file
        final File zero = file("m_zero.dat");
        final MappedByteBuffer buffer = Files.map(zero, MapMode.READ_WRITE, 0);
        assertEquals(0, buffer.capacity());
        assertTrue(zero.exists());

        // READ_ONLY on a missing file with a valid size still reports the missing file
        assertThrows(FileNotFoundException.class, () -> Files.map(file("m_ro_missing.dat"), MapMode.READ_ONLY, 10));

        // null file / mode with a valid size still NPE
        assertThrows(NullPointerException.class, () -> Files.map(null, MapMode.READ_WRITE, 10));
        assertThrows(NullPointerException.class, () -> Files.map(zero, null, 10));
    }

    // ---------------------------------------------------------------- U24-03 (C-362 map: argument-check order)

    @Test
    public void testU24_03_mapArgumentChecksKeepGuavaOrder() {
        final File missing = file("m_order.dat");

        // a negative size is Guava's own first check (checkArgument before checkNotNull): reported before a null argument
        assertEquals("size (-1) may not be negative", assertThrows(IllegalArgumentException.class, () -> Files.map(null, MapMode.READ_WRITE, -1)).getMessage());
        assertEquals("size (-1) may not be negative", assertThrows(IllegalArgumentException.class, () -> Files.map(missing, null, -1)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> Files.map(null, null, Long.MIN_VALUE));

        // an oversize was detected by FileChannel.map AFTER Guava's checkNotNull calls at r9620: the hoisted check must
        // not overtake the NullPointerException
        assertThrows(NullPointerException.class, () -> Files.map(null, MapMode.READ_WRITE, Integer.MAX_VALUE + 1L));
        assertThrows(NullPointerException.class, () -> Files.map(missing, null, Integer.MAX_VALUE + 1L));
        assertThrows(NullPointerException.class, () -> Files.map(null, MapMode.READ_ONLY, Long.MAX_VALUE));
        assertThrows(NullPointerException.class, () -> Files.map(null, null, Long.MAX_VALUE));
        assertFalse(missing.exists());

        // a single bad argument: unchanged
        assertThrows(NullPointerException.class, () -> Files.map(null, MapMode.READ_WRITE, 10));
        assertThrows(NullPointerException.class, () -> Files.map(missing, null, 10));
        assertThrows(NullPointerException.class, () -> Files.map(missing, null, 0));
        assertThrows(IllegalArgumentException.class, () -> Files.map(missing, MapMode.READ_WRITE, Integer.MAX_VALUE + 1L));
        assertThrows(IllegalArgumentException.class, () -> Files.map(missing, MapMode.PRIVATE, -1));
        assertFalse(missing.exists(), "no size or null rejection may create the file");
        assertThrows(FileNotFoundException.class, () -> Files.map(missing, MapMode.READ_ONLY, 10));
        assertFalse(missing.exists());
    }
}
