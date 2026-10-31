package com.landawn.abacus.poi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedException;
import com.landawn.abacus.poi.ExcelUtil.ExcelFormat;
import com.landawn.abacus.poi.ExcelUtil.RowExtractors;
import com.landawn.abacus.poi.ExcelUtil.RowMappers;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.stream.Stream;

public class ExcelUtilContractTest extends TestBase {
    @TempDir
    Path tempDir;

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void namedReadersUseCaseInsensitiveLookup(final ExcelFormat format) throws Exception {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExcelUtil.writeRowsToSheet("Sales\u540D", List.of("header"), List.of(List.of("\uD83D\uDE80")), sheet -> {
        }, output, format);
        final byte[] bytes = output.toByteArray();
        final Path path = tempDir.resolve("source." + format.name().toLowerCase(java.util.Locale.ROOT));
        Files.write(path, bytes);
        final File file = path.toFile();
        final String name = "sALES\u540D";
        assertEquals("\uD83D\uDE80", ExcelUtil.readDatasetFromSheet(file, name, RowExtractors.DEFAULT).get(0, 0));
        assertEquals(1, ExcelUtil.readDatasetFromSheet(path, name, RowExtractors.DEFAULT).size());
        assertEquals(1, ExcelUtil.readDatasetFromSheet(new ByteArrayInputStream(bytes), name, RowExtractors.DEFAULT).size());
        assertEquals(List.of(List.of("\uD83D\uDE80")), ExcelUtil.readRowsFromSheet(file, name, true, RowMappers.DEFAULT));
        assertEquals(1, ExcelUtil.readRowsFromSheet(path, name, true, RowMappers.DEFAULT).size());
        assertEquals(1, ExcelUtil.readRowsFromSheet(new ByteArrayInputStream(bytes), name, true, RowMappers.DEFAULT).size());
        try (Stream<?> rows = ExcelUtil.streamRowsFromSheet(file, name, true)) {
            assertEquals(1, rows.count());
        }
        try (Stream<?> rows = ExcelUtil.streamRowsFromSheet(path, name, true)) {
            assertEquals(1, rows.count());
        }
        try (Stream<?> rows = ExcelUtil.streamRowsFromSheet(new ByteArrayInputStream(bytes), name, true)) {
            assertEquals(1, rows.count());
        }
        final StringWriter csv = new StringWriter();
        ExcelUtil.exportSheetToCsv(file, name, null, csv);
        assertEquals("\"header\"\n\"\uD83D\uDE80\"", csv.toString());
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void emptyHeadersCreateAPhysicalRowAndCallbacksSeeCompletedData(final ExcelFormat format) throws Exception {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        ExcelUtil.writeRowsToSheet("Data", List.of(), List.of(List.of("value")), sheet -> {
            assertNotNull(sheet.getRow(0));
            assertEquals(-1, sheet.getRow(0).getLastCellNum());
            assertEquals("value", sheet.getRow(1).getCell(0).getStringCellValue());
        }, output, format);
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(output.toByteArray()))) {
            assertNotNull(workbook.getSheetAt(0).getRow(0));
            assertEquals("value", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
        }
        assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeRowsToSheet("Data", null, List.of(), sheet -> {
        }, new ByteArrayOutputStream(), format));
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void datasetCallbackSeesCompletedRows(final ExcelFormat format) {
        ExcelUtil.writeDatasetToSheet("Data", N.newDataset(List.of("header"), List.of(List.of("value"))), sheet -> {
            assertEquals("header", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("value", sheet.getRow(1).getCell(0).getStringCellValue());
        }, new ByteArrayOutputStream(), format);
    }

    // ---- deep review 2026-10-27 G139 begin ----
    /** The 24 public overloads that open a workbook, each reading {@code source} (as File, Path or InputStream). */
    private static List<org.junit.jupiter.api.function.Executable> workbookOpeningCalls(final File source, final File csvFile) throws Exception {
        final byte[] bytes = Files.readAllBytes(source.toPath());
        final Path path = source.toPath();

        return List.of(() -> ExcelUtil.readDatasetFromSheet(source), //
                () -> ExcelUtil.readDatasetFromSheet(source, 0, RowExtractors.DEFAULT), //
                () -> ExcelUtil.readDatasetFromSheet(new ByteArrayInputStream(bytes), 0, RowExtractors.DEFAULT), //
                () -> ExcelUtil.readDatasetFromSheet(path, 0, RowExtractors.DEFAULT), //
                () -> ExcelUtil.readDatasetFromSheet(source, "Data", RowExtractors.DEFAULT), //
                () -> ExcelUtil.readDatasetFromSheet(new ByteArrayInputStream(bytes), "Data", RowExtractors.DEFAULT), //
                () -> ExcelUtil.readDatasetFromSheet(path, "Data", RowExtractors.DEFAULT), //
                () -> ExcelUtil.readRowsFromSheet(source), //
                () -> ExcelUtil.readRowsFromSheet(source, 0, false, RowMappers.DEFAULT), //
                () -> ExcelUtil.readRowsFromSheet(new ByteArrayInputStream(bytes), 0, false, RowMappers.DEFAULT), //
                () -> ExcelUtil.readRowsFromSheet(path, 0, false, RowMappers.DEFAULT), //
                () -> ExcelUtil.readRowsFromSheet(source, "Data", false, RowMappers.DEFAULT), //
                () -> ExcelUtil.readRowsFromSheet(new ByteArrayInputStream(bytes), "Data", false, RowMappers.DEFAULT), //
                () -> ExcelUtil.readRowsFromSheet(path, "Data", false, RowMappers.DEFAULT), //
                () -> ExcelUtil.streamRowsFromSheet(source, 0, false).close(), //
                () -> ExcelUtil.streamRowsFromSheet(new ByteArrayInputStream(bytes), 0, false).close(), //
                () -> ExcelUtil.streamRowsFromSheet(path, 0, false).close(), //
                () -> ExcelUtil.streamRowsFromSheet(source, "Data", false).close(), //
                () -> ExcelUtil.streamRowsFromSheet(new ByteArrayInputStream(bytes), "Data", false).close(), //
                () -> ExcelUtil.streamRowsFromSheet(path, "Data", false).close(), //
                () -> ExcelUtil.exportSheetToCsv(source, 0, csvFile), //
                () -> ExcelUtil.exportSheetToCsv(source, "Data", csvFile), //
                () -> ExcelUtil.exportSheetToCsv(source, 0, null, new StringWriter()), //
                () -> ExcelUtil.exportSheetToCsv(source, "Data", null, new StringWriter()));
    }

    // A zero-byte source is rejected by POI's EmptyFileException, an IllegalArgumentException that every
    // workbook-opening overload passes through unchanged (it is not wrapped as UncheckedException), for File, Path and
    // InputStream sources alike; the CSV file sinks leave no output behind.
    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void zeroByteSourceFailsWithPoiEmptyFileExceptionInEveryOverload(final ExcelFormat format) throws Exception {
        final Path source = tempDir.resolve("empty." + format.name().toLowerCase(java.util.Locale.ROOT));
        Files.write(source, new byte[0]);
        final File csvFile = tempDir.resolve("out.csv").toFile();
        final List<org.junit.jupiter.api.function.Executable> calls = workbookOpeningCalls(source.toFile(), csvFile);
        assertEquals(24, calls.size());

        for (int i = 0; i < calls.size(); i++) {
            final IllegalArgumentException e = assertThrows(org.apache.poi.EmptyFileException.class, calls.get(i), "overload #" + i);
            assertNull(e.getCause(), "overload #" + i);
        }

        assertFalse(csvFile.exists());
    }

    // The documented boundary between the two failure kinds: content of an unknown type (plain text) fails with POI's
    // IOException rethrown as UncheckedException, while ZIP data without readable entries is POI's
    // NotOfficeXmlFileException (an IllegalArgumentException) - in every overload.
    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void textSourceIsUncheckedExceptionWhileZipStubIsIllegalArgumentException(final ExcelFormat format) throws Exception {
        final String extension = "." + format.name().toLowerCase(java.util.Locale.ROOT);
        final Path text = tempDir.resolve("text" + extension);
        Files.write(text, "a,b\n1,2\n".getBytes(java.nio.charset.StandardCharsets.US_ASCII));
        final Path zipStub = tempDir.resolve("zipstub" + extension);
        Files.write(zipStub, new byte[] { 'P', 'K', 3, 4 });
        final File csvFile = tempDir.resolve("out.csv").toFile();

        for (final org.junit.jupiter.api.function.Executable call : workbookOpeningCalls(text.toFile(), csvFile)) {
            final UncheckedException e = assertThrows(UncheckedException.class, call);
            assertEquals(java.io.IOException.class, e.getCause().getClass());
        }

        for (final org.junit.jupiter.api.function.Executable call : workbookOpeningCalls(zipStub.toFile(), csvFile)) {
            assertThrows(org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException.class, call);
        }

        assertFalse(csvFile.exists());
    }
    // ---- deep review 2026-10-27 G139 end ----

    // ---- deep review 2026-10-27 verify G148 begin ----
    /** An OLE2 file holding one zero-filled stream per name (no workbook unless a name says so). */
    private static byte[] ole2(final String... entryNames) throws Exception {
        try (org.apache.poi.poifs.filesystem.POIFSFileSystem fs = new org.apache.poi.poifs.filesystem.POIFSFileSystem();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            for (final String entryName : entryNames) {
                fs.createDocument(new ByteArrayInputStream(new byte[600]), entryName);
            }

            fs.writeFilesystem(output);
            return output.toByteArray();
        }
    }

    private void assertEveryOverloadThrows(final byte[] content, final Class<? extends RuntimeException> expected, final String label) throws Exception {
        final Path source = tempDir.resolve("source.bin");
        Files.write(source, content);
        final File csvFile = tempDir.resolve("out.csv").toFile();
        final List<org.junit.jupiter.api.function.Executable> calls = workbookOpeningCalls(source.toFile(), csvFile);
        assertEquals(24, calls.size());

        for (int i = 0; i < calls.size(); i++) {
            final RuntimeException e = assertThrows(RuntimeException.class, calls.get(i), label + " overload #" + i);
            assertEquals(expected, e.getClass(), label + " overload #" + i);
        }

        assertFalse(csvFile.exists(), label);
    }

    // Class-level note: an OLE2 document that is not a workbook (a Word file, an empty OLE2 file) is POI's plain
    // IllegalArgumentException and an Excel 5.0/95 workbook its OldExcelFormatException, both unwrapped, in every overload.
    @org.junit.jupiter.api.Test
    public void ole2SourcesWithoutBiff8WorkbookFailWithIllegalArgumentException() throws Exception {
        assertEveryOverloadThrows(ole2("WordDocument"), IllegalArgumentException.class, "Word file");
        assertEveryOverloadThrows(ole2(), IllegalArgumentException.class, "empty OLE2 file");
        assertEveryOverloadThrows(ole2("Book"), org.apache.poi.hssf.OldExcelFormatException.class, "Excel 5.0/95 workbook");
    }

    // Class-level note: a password-protected workbook - an encrypted XLSX (an OLE2 file too) or XLS - fails with POI's
    // EncryptedDocumentException, an IllegalStateException, unwrapped in every overload (not the IAE of a non-workbook).
    @org.junit.jupiter.api.Test
    public void passwordProtectedWorkbooksFailWithEncryptedDocumentException() throws Exception {
        final ByteArrayOutputStream xlsx = new ByteArrayOutputStream();
        ExcelUtil.writeRowsToSheet("Data", List.of("header"), List.of(List.of("value")), sheet -> {
        }, xlsx, ExcelFormat.XLSX);
        final ByteArrayOutputStream encryptedXlsx = new ByteArrayOutputStream();

        try (org.apache.poi.poifs.filesystem.POIFSFileSystem fs = new org.apache.poi.poifs.filesystem.POIFSFileSystem()) {
            final org.apache.poi.poifs.crypt.Encryptor encryptor = new org.apache.poi.poifs.crypt.EncryptionInfo(
                    org.apache.poi.poifs.crypt.EncryptionMode.standard).getEncryptor();
            encryptor.confirmPassword("secret");

            try (java.io.OutputStream output = encryptor.getDataStream(fs)) {
                output.write(xlsx.toByteArray());
            }

            fs.writeFilesystem(encryptedXlsx);
        }

        assertEveryOverloadThrows(encryptedXlsx.toByteArray(), org.apache.poi.EncryptedDocumentException.class, "encrypted XLSX");

        final ByteArrayOutputStream encryptedXls = new ByteArrayOutputStream();
        org.apache.poi.hssf.record.crypto.Biff8EncryptionKey.setCurrentUserPassword("secret");

        try (org.apache.poi.hssf.usermodel.HSSFWorkbook workbook = new org.apache.poi.hssf.usermodel.HSSFWorkbook()) {
            workbook.createSheet("Data").createRow(0).createCell(0).setCellValue("value");
            workbook.write(encryptedXls);
        } finally {
            org.apache.poi.hssf.record.crypto.Biff8EncryptionKey.setCurrentUserPassword(null);
        }

        assertEveryOverloadThrows(encryptedXls.toByteArray(), org.apache.poi.EncryptedDocumentException.class, "encrypted XLS");
    }

    // Class-level note: data that starts like a ZIP entry but holds no readable one is NotOfficeXmlFileException (IAE),
    // while an empty ZIP archive (no entry at all) is content of an unknown type: UncheckedException(IOException).
    @org.junit.jupiter.api.Test
    public void zipEntryWithoutReadableContentVersusEmptyZipArchive() throws Exception {
        final byte[] entryThenGarbage = new byte[2000];
        new java.util.Random(148).nextBytes(entryThenGarbage);
        entryThenGarbage[0] = 'P';
        entryThenGarbage[1] = 'K';
        entryThenGarbage[2] = 3;
        entryThenGarbage[3] = 4;
        assertEveryOverloadThrows(entryThenGarbage, org.apache.poi.openxml4j.exceptions.NotOfficeXmlFileException.class, "ZIP entry signature + garbage");

        final ByteArrayOutputStream emptyZip = new ByteArrayOutputStream();
        new java.util.zip.ZipOutputStream(emptyZip).close();
        assertEveryOverloadThrows(emptyZip.toByteArray(), UncheckedException.class, "empty ZIP archive");

        final Path source = tempDir.resolve("source.bin");
        assertEquals(java.io.IOException.class,
                assertThrows(UncheckedException.class, () -> ExcelUtil.readRowsFromSheet(source.toFile())).getCause().getClass());
        assertEquals(java.io.IOException.class,
                assertThrows(UncheckedException.class,
                        () -> ExcelUtil.readRowsFromSheet(new ByteArrayInputStream(emptyZip.toByteArray()), 0, false, RowMappers.DEFAULT)).getCause()
                                .getClass());
    }
    // ---- deep review 2026-10-27 verify G148 end ----
}
