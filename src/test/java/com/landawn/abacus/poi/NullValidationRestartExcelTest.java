package com.landawn.abacus.poi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrowsExactly;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.apache.poi.ss.usermodel.Sheet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.poi.ExcelUtil.ExcelFormat;
import com.landawn.abacus.poi.ExcelUtil.RowExtractors;
import com.landawn.abacus.poi.ExcelUtil.RowMappers;
import com.landawn.abacus.poi.ExcelUtil.SheetCreateOptions;
import com.landawn.abacus.util.Dataset;

class NullValidationRestartExcelTest extends TestBase {
    @TempDir
    Path tempDir;

    @Test
    void allSourceOverloadsRejectNull() {
        assertArgument("excelFile", () -> ExcelUtil.readDatasetFromSheet((File) null),
                () -> ExcelUtil.readDatasetFromSheet((File) null, 0, RowExtractors.DEFAULT),
                () -> ExcelUtil.readDatasetFromSheet((File) null, "Data", RowExtractors.DEFAULT), () -> ExcelUtil.readRowsFromSheet((File) null),
                () -> ExcelUtil.readRowsFromSheet((File) null, 0, false, RowMappers.DEFAULT),
                () -> ExcelUtil.readRowsFromSheet((File) null, "Data", false, RowMappers.DEFAULT), () -> ExcelUtil.streamRowsFromSheet((File) null, 0, false),
                () -> ExcelUtil.streamRowsFromSheet((File) null, "Data", false));
        assertArgument("excelInputStream", () -> ExcelUtil.readDatasetFromSheet((InputStream) null, 0, RowExtractors.DEFAULT),
                () -> ExcelUtil.readDatasetFromSheet((InputStream) null, "Data", RowExtractors.DEFAULT),
                () -> ExcelUtil.readRowsFromSheet((InputStream) null, 0, false, RowMappers.DEFAULT),
                () -> ExcelUtil.readRowsFromSheet((InputStream) null, "Data", false, RowMappers.DEFAULT),
                () -> ExcelUtil.streamRowsFromSheet((InputStream) null, 0, false), () -> ExcelUtil.streamRowsFromSheet((InputStream) null, "Data", false));
        assertArgument("excelPath", () -> ExcelUtil.readDatasetFromSheet((Path) null, 0, RowExtractors.DEFAULT),
                () -> ExcelUtil.readDatasetFromSheet((Path) null, "Data", RowExtractors.DEFAULT),
                () -> ExcelUtil.readRowsFromSheet((Path) null, 0, false, RowMappers.DEFAULT),
                () -> ExcelUtil.readRowsFromSheet((Path) null, "Data", false, RowMappers.DEFAULT), () -> ExcelUtil.streamRowsFromSheet((Path) null, 0, false),
                () -> ExcelUtil.streamRowsFromSheet((Path) null, "Data", false));
    }

    @Test
    void rowWritersRejectNullBeforeCallbacksOrFileReplacement() throws Exception {
        final File destination = tempDir.resolve("rows.xlsx").toFile();
        Files.writeString(destination.toPath(), "keep");
        final Consumer<Sheet> setter = sheet -> {
            throw new AssertionError("Unexpected sheet callback");
        };
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertArgument("headers", () -> ExcelUtil.writeRowsToSheet("Data", null, List.of(), destination),
                () -> ExcelUtil.writeRowsToSheet("Data", null, List.of(), setter, destination),
                () -> ExcelUtil.writeRowsToSheet("Data", null, List.of(), setter, output, ExcelFormat.XLSX));
        assertArgument("rows", () -> ExcelUtil.writeRowsToSheet("Data", List.of(), null, destination),
                () -> ExcelUtil.writeRowsToSheet("Data", List.of(), null, setter, destination),
                () -> ExcelUtil.writeRowsToSheet("Data", List.of(), null, setter, output, ExcelFormat.XLSX));
        assertArgument("outputExcelFile", () -> ExcelUtil.writeRowsToSheet("Data", List.of(), List.of(), (File) null),
                () -> ExcelUtil.writeRowsToSheet("Data", List.of(), List.of(), setter, (File) null));
        assertArgument("outputExcelPath", () -> ExcelUtil.writeRowsToSheet("Data", List.of(), List.of(), setter, (Path) null));
        assertArgument("outputStream", () -> ExcelUtil.writeRowsToSheet("Data", List.of(), List.of(), setter, (OutputStream) null, ExcelFormat.XLSX));
        assertEquals(0, output.size());
        assertEquals("keep", Files.readString(destination.toPath()));
        try (java.util.stream.Stream<Path> files = Files.list(tempDir)) {
            assertEquals(1, files.count());
        }
    }

    @Test
    void datasetWritersRejectNullBeforeCallbacksOrFileReplacement() throws Exception {
        final File destination = tempDir.resolve("dataset.xlsx").toFile();
        Files.writeString(destination.toPath(), "keep");
        final Consumer<Sheet> setter = sheet -> {
            throw new AssertionError("Unexpected sheet callback");
        };
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertArgument("dataset", () -> ExcelUtil.writeDatasetToSheet("Data", null, destination),
                () -> ExcelUtil.writeDatasetToSheet("Data", null, setter, destination),
                () -> ExcelUtil.writeDatasetToSheet("Data", null, setter, output, ExcelFormat.XLSX));
        assertArgument("outputExcelFile", () -> ExcelUtil.writeDatasetToSheet("Data", Dataset.empty(), (File) null),
                () -> ExcelUtil.writeDatasetToSheet("Data", Dataset.empty(), setter, (File) null));
        assertArgument("outputExcelPath", () -> ExcelUtil.writeDatasetToSheet("Data", Dataset.empty(), setter, (Path) null));
        assertArgument("outputStream", () -> ExcelUtil.writeDatasetToSheet("Data", Dataset.empty(), setter, (OutputStream) null, ExcelFormat.XLSX));
        assertEquals(0, output.size());
        assertEquals("keep", Files.readString(destination.toPath()));
    }

    @Test
    void csvExportRejectsNullBeforeReadingOrReplacingFiles() throws Exception {
        final File destination = tempDir.resolve("output.csv").toFile();
        Files.writeString(destination.toPath(), "keep");
        final File missingSource = tempDir.resolve("missing.xlsx").toFile();
        final StringWriter writer = new StringWriter();
        assertArgument("excelFile", () -> ExcelUtil.exportSheetToCsv(null, 0, destination), () -> ExcelUtil.exportSheetToCsv(null, "Data", destination),
                () -> ExcelUtil.exportSheetToCsv(null, 0, null, writer), () -> ExcelUtil.exportSheetToCsv(null, "Data", null, writer));
        assertArgument("outputCsvFile", () -> ExcelUtil.exportSheetToCsv(missingSource, 0, (File) null),
                () -> ExcelUtil.exportSheetToCsv(missingSource, "Data", (File) null));
        assertArgument("outputWriter", () -> ExcelUtil.exportSheetToCsv(missingSource, 0, null, (Writer) null),
                () -> ExcelUtil.exportSheetToCsv(missingSource, "Data", null, (Writer) null));
        assertEquals("", writer.toString());
        assertEquals("keep", Files.readString(destination.toPath()));
    }

    @Test
    void nullableOptionsHeadersAndCellValuesKeepTheirMeaning() throws Exception {
        final File destination = tempDir.resolve("nullable.xlsx").toFile();
        ExcelUtil.writeRowsToSheet("Data", List.of("name", "value"), List.of(Arrays.asList("entry", null)), (SheetCreateOptions) null, destination);
        final StringWriter csv = new StringWriter();
        ExcelUtil.exportSheetToCsv(destination, "Data", null, csv);
        assertEquals("\"name\",\"value\"\n\"entry\",\"\"", csv.toString());
        assertEquals(List.of("entry", ""), ExcelUtil.readRowsFromSheet(destination, 0, true, RowMappers.DEFAULT).get(0));
    }

    private static void assertArgument(final String name, final Executable... operations) {
        for (Executable operation : operations) {
            final IllegalArgumentException exception = assertThrowsExactly(IllegalArgumentException.class, operation);
            assertTrue(exception.getMessage().contains(name), exception::getMessage);
        }
    }
}
