package com.landawn.abacus.poi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
}
