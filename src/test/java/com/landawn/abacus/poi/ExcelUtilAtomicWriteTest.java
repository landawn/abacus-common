package com.landawn.abacus.poi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.N;

/**
 * B11 regression tests: every {@code File} sink used to open a {@code FileOutputStream} on the destination
 * <i>before</i> building the workbook, so a failure part-way through left the caller's existing file
 * truncated to zero bytes. Writes now go to a sibling temporary file that is moved into place only on
 * success.
 */
public class ExcelUtilAtomicWriteTest extends TestBase {

    @TempDir
    Path tempDir;

    private static final List<String> HEADERS = Arrays.asList("h1", "h2");

    /** A sheetSetter that throws, standing in for any failure raised while the workbook is being built. */
    private static void explode(final Sheet sheet) {
        throw new IllegalStateException("sheetSetter failed");
    }

    @Test
    public void testB11_writeRowsToSheetLeavesTheExistingFileIntactOnFailure() throws IOException {
        final File target = tempDir.resolve("rows.xlsx").toFile();
        Files.writeString(target.toPath(), "PREVIOUS CONTENT", StandardCharsets.UTF_8);
        final long sizeBefore = target.length();

        assertThrows(IllegalStateException.class,
                () -> ExcelUtil.writeRowsToSheet("S", HEADERS, List.of(List.of("a", "b")), ExcelUtilAtomicWriteTest::explode, target));

        assertEquals(sizeBefore, target.length(), "the destination must not be truncated by a failed write");
        assertEquals("PREVIOUS CONTENT", Files.readString(target.toPath(), StandardCharsets.UTF_8));
        assertNoTempFilesLeftBehind();
    }

    @Test
    public void testB11_writeDatasetToSheetLeavesTheExistingFileIntactOnFailure() throws IOException {
        final File target = tempDir.resolve("dataset.xlsx").toFile();
        Files.writeString(target.toPath(), "PREVIOUS CONTENT", StandardCharsets.UTF_8);

        final Dataset dataset = N.newDataset(HEADERS, List.of(List.of("a", "b")));

        assertThrows(IllegalStateException.class, () -> ExcelUtil.writeDatasetToSheet("S", dataset, ExcelUtilAtomicWriteTest::explode, target));

        assertEquals("PREVIOUS CONTENT", Files.readString(target.toPath(), StandardCharsets.UTF_8));
        assertNoTempFilesLeftBehind();
    }

    @Test
    public void testB11_aFailedWriteDoesNotCreateTheDestinationAtAll() {
        final File target = tempDir.resolve("never-created.xlsx").toFile();

        assertThrows(IllegalStateException.class,
                () -> ExcelUtil.writeRowsToSheet("S", HEADERS, List.of(List.of("a", "b")), ExcelUtilAtomicWriteTest::explode, target));

        assertTrue(!target.exists() || target.length() == 0);
        assertNoTempFilesLeftBehind();
    }

    @Test
    public void testB11_successfulWriteReplacesTheDestination() throws IOException {
        final File target = tempDir.resolve("replaced.xlsx").toFile();
        Files.writeString(target.toPath(), "PREVIOUS CONTENT", StandardCharsets.UTF_8);

        ExcelUtil.writeRowsToSheet("S", HEADERS, List.of(Arrays.asList("a", "b"), Arrays.asList("c", "d")), target);

        try (Workbook wb = WorkbookFactory.create(target)) {
            final Sheet sheet = wb.getSheetAt(0);
            assertEquals("h1", sheet.getRow(0).getCell(0).getStringCellValue());
            assertEquals("a", sheet.getRow(1).getCell(0).getStringCellValue());
            assertEquals("d", sheet.getRow(2).getCell(1).getStringCellValue());
        }

        assertNoTempFilesLeftBehind();
    }

    @Test
    public void testB11_datasetWriteAndReadBackRoundTrips() throws IOException {
        final File target = tempDir.resolve("roundtrip.xlsx").toFile();
        final Dataset dataset = N.newDataset(HEADERS, List.of(Arrays.asList("a", "b"), Arrays.asList("c", "d")));

        ExcelUtil.writeDatasetToSheet("S", dataset, target);

        final Dataset loaded = ExcelUtil.readDatasetFromSheet(target);
        assertEquals(HEADERS, loaded.columnNames());
        assertEquals(2, loaded.size());
        assertNoTempFilesLeftBehind();
    }

    @Test
    public void testB11_exportSheetToCsvFileSinkIsAtomicToo() throws IOException {
        final File xlsx = tempDir.resolve("source.xlsx").toFile();
        ExcelUtil.writeRowsToSheet("S", HEADERS, List.of(Arrays.asList("a", "b")), xlsx);

        final File csv = tempDir.resolve("out.csv").toFile();
        Files.writeString(csv.toPath(), "PREVIOUS CONTENT", StandardCharsets.UTF_8);

        // A missing sheet name fails before anything is written.
        assertThrows(IllegalArgumentException.class, () -> ExcelUtil.exportSheetToCsv(xlsx, "NoSuchSheet", csv));
        assertEquals("PREVIOUS CONTENT", Files.readString(csv.toPath(), StandardCharsets.UTF_8));

        // A successful export replaces it.
        ExcelUtil.exportSheetToCsv(xlsx, 0, csv);
        assertEquals("\"h1\",\"h2\"\n\"a\",\"b\"", Files.readString(csv.toPath(), StandardCharsets.UTF_8));
        assertNoTempFilesLeftBehind();
    }

    @Test
    public void testB11_aMissingParentDirectoryStillFailsWithAnIoError() {
        final File target = tempDir.resolve("no-such-dir").resolve("out.xlsx").toFile();

        // The temp file is created beside the destination, so an unusable destination directory fails the
        // same way opening the destination itself used to.
        assertThrows(com.landawn.abacus.exception.UncheckedException.class, () -> ExcelUtil.writeRowsToSheet("S", HEADERS, List.of(List.of("a", "b")), target));
        assertTrue(!target.exists());
    }

    @Test
    public void testB11_shortDestinationNameIsAccepted() throws IOException {
        // File.createTempFile rejects a prefix shorter than three characters, which a destination named "a"
        // would otherwise produce.
        final File target = tempDir.resolve("a").toFile();

        ExcelUtil.writeRowsToSheet("S", HEADERS, List.of(Arrays.asList("a", "b")), target);

        assertTrue(target.length() > 0);
        try (Workbook wb = WorkbookFactory.create(target)) {
            assertEquals("h1", wb.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
        }
        assertNoTempFilesLeftBehind();
    }

    @Test
    public void testB11_csvExportUsesTheLibraryDefaultCharset() throws IOException {
        final File xlsx = tempDir.resolve("utf8.xlsx").toFile();
        ExcelUtil.writeRowsToSheet("S", Arrays.asList("h"), List.of(Arrays.asList("\u4e2d\u6587")), xlsx);

        final File csv = tempDir.resolve("utf8.csv").toFile();
        ExcelUtil.exportSheetToCsv(xlsx, 0, csv);

        // The File sink went from IOUtil.newFileWriter(File) to IOUtil.newOutputStreamWriter(OutputStream);
        // both use the library default (UTF-8), so non-ASCII content must survive unchanged.
        assertEquals("\"h\"\n\"\u4e2d\u6587\"", Files.readString(csv.toPath(), StandardCharsets.UTF_8));
        assertNoTempFilesLeftBehind();
    }

    private void assertNoTempFilesLeftBehind() {
        final String[] leftovers = tempDir.toFile().list((dir, name) -> name.endsWith(".tmp"));
        assertEquals(0, leftovers == null ? 0 : leftovers.length, "temporary files must be cleaned up: " + Arrays.toString(leftovers));
    }
}
