package com.landawn.abacus.poi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.poifs.crypt.EncryptionInfo;
import org.apache.poi.poifs.crypt.EncryptionMode;
import org.apache.poi.poifs.crypt.Encryptor;
import org.apache.poi.poifs.filesystem.POIFSFileSystem;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedException;
import com.landawn.abacus.poi.ExcelUtil.ExcelFormat;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
@Tag("slow-test")
public class ExcelUtilPerformanceFixTest extends TestBase {
    @TempDir
    Path tempDir;

    private static final class TrackingWriter extends StringWriter {
        boolean closed;
        int flushes;

        @Override
        public void flush() {
            flushes++;
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    private File workbook(final ExcelFormat format) {
        final File file = tempDir.resolve("source." + format.name().toLowerCase(Locale.ROOT)).toFile();
        ExcelUtil.writeRowsToSheet("Sales", List.of("header"), List.of(List.of("value")), file);
        return file;
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void csvPreservesSourceBytesAndCallerWriterOwnership(final ExcelFormat format) throws Exception {
        final File file = workbook(format);
        final byte[] original = Files.readAllBytes(file.toPath());
        final TrackingWriter indexed = new TrackingWriter();
        final TrackingWriter named = new TrackingWriter();
        ExcelUtil.exportSheetToCsv(file, 0, null, indexed);
        ExcelUtil.exportSheetToCsv(file, "sALES", List.of("replacement"), named);
        assertEquals("\"header\"\n\"value\"", indexed.toString());
        assertEquals("\"replacement\"\n\"value\"", named.toString());
        assertFalse(indexed.closed);
        assertFalse(named.closed);
        assertTrue(indexed.flushes > 0);
        assertTrue(named.flushes > 0);
        assertArrayEquals(original, Files.readAllBytes(file.toPath()), "closing a CSV source must never save workbook changes");
        Files.move(file.toPath(), tempDir.resolve("moved-source"));
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void sameFileCsvExportClosesWorkbookBeforeReplacingDestination(final ExcelFormat format) throws Exception {
        final File file = workbook(format);
        ExcelUtil.exportSheetToCsv(file, 0, file);
        assertEquals("\"header\"\n\"value\"", Files.readString(file.toPath()));
        assertNoTemporaryFiles();
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void csvFailureClosesSourceButLeavesWriterAndDestinationOwnedByCaller(final ExcelFormat format) throws Exception {
        final File file = workbook(format);
        final byte[] original = Files.readAllBytes(file.toPath());
        final AtomicBoolean closed = new AtomicBoolean();
        final IOException failure = new IOException("expected output failure");
        final Writer failing = new Writer() {
            @Override
            public void write(final char[] chars, final int offset, final int length) throws IOException {
                throw failure;
            }

            @Override
            public void flush() throws IOException {
                throw failure;
            }

            @Override
            public void close() {
                closed.set(true);
            }
        };
        final UncheckedException thrown = assertThrows(UncheckedException.class, () -> ExcelUtil.exportSheetToCsv(file, 0, null, failing));
        assertInstanceOf(IOException.class, thrown.getCause());
        assertFalse(closed.get());
        assertArrayEquals(original, Files.readAllBytes(file.toPath()));

        final File destination = tempDir.resolve("existing.csv").toFile();
        Files.writeString(destination.toPath(), "keep me");
        assertThrows(IllegalArgumentException.class, () -> ExcelUtil.exportSheetToCsv(file, "missing", destination));
        assertThrows(IllegalArgumentException.class, () -> ExcelUtil.exportSheetToCsv(file, 99, destination));
        assertEquals("keep me", Files.readString(destination.toPath()));
        assertArrayEquals(original, Files.readAllBytes(file.toPath()));
        Files.delete(file.toPath());
        assertNoTemporaryFiles();
    }

    @Test
    public void missingEmptyAndInvalidInputsKeepTheirExceptionMapping() throws Exception {
        final File missing = tempDir.resolve("missing.xlsx").toFile();
        final File empty = tempDir.resolve("empty.xlsx").toFile();
        Files.write(empty.toPath(), new byte[0]);
        final File invalid = tempDir.resolve("invalid.xlsx").toFile();
        Files.writeString(invalid.toPath(), "this is not an Excel workbook");

        for (final File file : List.of(missing, empty, invalid)) {
            final RuntimeException expected = streamOpeningFailure(file);
            final TrackingWriter writer = new TrackingWriter();
            final RuntimeException indexed = assertThrows(RuntimeException.class, () -> ExcelUtil.exportSheetToCsv(file, 0, null, writer));
            final RuntimeException named = assertThrows(RuntimeException.class, () -> ExcelUtil.exportSheetToCsv(file, "Sales", null, writer));
            assertEquals(expected.getClass(), indexed.getClass());
            assertEquals(expected.getClass(), named.getClass());
            if (expected.getCause() != null) {
                assertEquals(expected.getCause().getClass(), indexed.getCause().getClass());
                assertEquals(expected.getCause().getClass(), named.getCause().getClass());
            }
            assertEquals("", writer.toString());
            assertFalse(writer.closed);
            Files.deleteIfExists(file.toPath());
        }
    }

    @Test
    public void encryptedInputStillFailsWithoutPasswordAndReleasesTheFile() throws Exception {
        final File plain = workbook(ExcelFormat.XLSX);
        final File encrypted = tempDir.resolve("encrypted.xlsx").toFile();
        try (POIFSFileSystem fs = new POIFSFileSystem()) {
            final EncryptionInfo info = new EncryptionInfo(EncryptionMode.agile);
            final Encryptor encryptor = info.getEncryptor();
            encryptor.confirmPassword("test password");
            try (OutputStream encryptedData = encryptor.getDataStream(fs)) {
                Files.copy(plain.toPath(), encryptedData);
            }
            try (OutputStream output = Files.newOutputStream(encrypted.toPath())) {
                fs.writeFilesystem(output);
            }
        }
        final byte[] original = Files.readAllBytes(encrypted.toPath());
        assertInstanceOf(EncryptedDocumentException.class, streamOpeningFailure(encrypted));
        final TrackingWriter writer = new TrackingWriter();
        assertThrows(EncryptedDocumentException.class, () -> ExcelUtil.exportSheetToCsv(encrypted, 0, null, writer));
        assertThrows(EncryptedDocumentException.class, () -> ExcelUtil.exportSheetToCsv(encrypted, "Sales", null, writer));
        assertFalse(writer.closed);
        assertEquals("", writer.toString());
        assertArrayEquals(original, Files.readAllBytes(encrypted.toPath()));
        Files.delete(encrypted.toPath());
    }

    private static final class CountingHeaders extends LinkedList<Object> {
        int indexedReads;

        @Override
        public Object get(final int index) {
            indexedReads++;
            return super.get(index);
        }
    }

    private static final class CountingRow extends ConcurrentLinkedQueue<Object> {
        int sizeReads;

        @Override
        public int size() {
            sizeReads++;
            return super.size();
        }
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void linkedInputsUseSequentialTraversalAndKeepRaggedRows(final ExcelFormat format) throws Exception {
        final CountingHeaders headers = new CountingHeaders();
        final CountingRow wideRow = new CountingRow();
        for (int i = 0; i < 128; i++) {
            headers.add(i == 0 ? null : "h" + i);
            wideRow.add("v" + i);
        }
        wideRow.add("beyond headers");
        final CountingRow emptyRow = new CountingRow();
        final AtomicBoolean callbackRan = new AtomicBoolean();
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExcelUtil.writeRowsToSheet("Rows", headers, List.of(wideRow, Arrays.asList("short", null), emptyRow), sheet -> {
            assertRows(sheet);
            callbackRan.set(true);
        }, output, format);
        assertTrue(callbackRan.get());
        assertEquals(0, headers.indexedReads, "linked headers must not repeatedly seek by index");
        assertTrue(wideRow.sizeReads <= 1, "a linear-time size must not be queried per cell");
        assertTrue(emptyRow.sizeReads <= 1);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray()))) {
            assertRows(workbook.getSheetAt(0));
        }
    }

    private static void assertRows(final Sheet sheet) {
        assertEquals("", sheet.getRow(0).getCell(0).getStringCellValue());
        assertEquals("h127", sheet.getRow(0).getCell(127).getStringCellValue());
        assertEquals("v0", sheet.getRow(1).getCell(0).getStringCellValue());
        assertEquals("v127", sheet.getRow(1).getCell(127).getStringCellValue());
        assertEquals("beyond headers", sheet.getRow(1).getCell(128).getStringCellValue());
        assertEquals("short", sheet.getRow(2).getCell(0).getStringCellValue());
        assertEquals(2, sheet.getRow(2).getLastCellNum());
        assertNull(sheet.getRow(2).getCell(2));
        assertEquals(-1, sheet.getRow(3).getLastCellNum());
    }

    private static RuntimeException streamOpeningFailure(final File file) {
        try (InputStream input = new FileInputStream(file);
             Workbook ignored = WorkbookFactory.create(input)) {
            throw new AssertionError("expected an unreadable workbook");
        } catch (final IOException e) {
            return new UncheckedException(e);
        } catch (final RuntimeException e) {
            return e;
        }
    }

    private void assertNoTemporaryFiles() throws IOException {
        try (var files = Files.list(tempDir)) {
            assertFalse(files.anyMatch(path -> path.getFileName().toString().endsWith(".tmp")));
        }
    }
}
