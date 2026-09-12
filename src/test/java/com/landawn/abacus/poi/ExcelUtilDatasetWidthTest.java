package com.landawn.abacus.poi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.poi.ExcelUtil.ExcelFormat;
import com.landawn.abacus.poi.ExcelUtil.RowExtractors;
import com.landawn.abacus.util.Dataset;

public class ExcelUtilDatasetWidthTest extends TestBase {
    private byte[] workbook(final ExcelFormat format, final Consumer<Sheet> populate) throws Exception {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Workbook workbook = format == ExcelFormat.XLS ? new HSSFWorkbook() : new XSSFWorkbook()) {
            populate.accept(workbook.createSheet("Data"));
            workbook.write(output);
        }
        return output.toByteArray();
    }

    private Dataset read(final byte[] bytes) {
        return ExcelUtil.readDatasetFromSheet(new ByteArrayInputStream(bytes), 0, RowExtractors.DEFAULT);
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void laterWiderRowsRetainValuesAndGenerateUniqueHeaders(final ExcelFormat format) throws Exception {
        final byte[] bytes = workbook(format, sheet -> {
            final Row header = sheet.createRow(2);
            header.createCell(0).setCellValue("Column_2");
            header.createCell(1).setCellValue("Column_2_1");
            sheet.createRow(4).createCell(0).setCellValue("first");
            sheet.createRow(7).createCell(2).setCellValue("\u540D\u79F0\uD83D\uDE80");
        });
        final Dataset dataset = read(bytes);
        assertEquals(List.of("Column_2", "Column_2_1", "Column_2_2"), dataset.columnNames());
        assertEquals(2, dataset.size());
        assertEquals(Arrays.asList("first", null, null), dataset.getRow(0));
        assertEquals(Arrays.asList(null, null, "\u540D\u79F0\uD83D\uDE80"), dataset.getRow(1));
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void emptyPhysicalHeaderStillRetainsFollowingData(final ExcelFormat format) throws Exception {
        final byte[] bytes = workbook(format, sheet -> {
            sheet.createRow(3);
            sheet.createRow(5).createCell(1).setCellValue("value");
            sheet.createRow(6);
        });
        final Dataset dataset = ExcelUtil.readDatasetFromSheet(new ByteArrayInputStream(bytes), "Data", RowExtractors.DEFAULT);
        assertEquals(List.of("Column_0", "Column_1"), dataset.columnNames());
        assertEquals(2, dataset.size());
        assertEquals(Arrays.asList(null, "value"), dataset.getRow(0));
        assertEquals(Arrays.asList(null, null), dataset.getRow(1));
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void extractorReceivesFullWidthAndAClearedBuffer(final ExcelFormat format) throws Exception {
        final byte[] bytes = workbook(format, sheet -> {
            sheet.createRow(0).createCell(0).setCellValue("\u540D");
            sheet.createRow(1).createCell(2).setCellValue("tail");
            sheet.createRow(2).createCell(0).setCellValue("next");
        });
        final Dataset dataset = ExcelUtil.readDatasetFromSheet(new ByteArrayInputStream(bytes), 0, (headers, row, output) -> {
            assertEquals(3, headers.length);
            assertEquals(3, output.length);
            for (final Object value : output) {
                assertNull(value);
            }
            final int column = row.getRowNum() == 1 ? 2 : 0;
            output[column] = row.getCell(column).getStringCellValue();
        });
        assertEquals(List.of("\u540D", "Column_1", "Column_2"), dataset.columnNames());
        assertEquals(Arrays.asList(null, null, "tail"), dataset.getRow(0));
        assertEquals(Arrays.asList("next", null, null), dataset.getRow(1));
        assertThrows(IllegalArgumentException.class, () -> ExcelUtil.readDatasetFromSheet(new ByteArrayInputStream(bytes), 0, null));
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void lastSupportedColumnIsRetained(final ExcelFormat format) throws Exception {
        final int lastColumn = format == ExcelFormat.XLS ? 255 : 16383;
        final Dataset dataset = read(workbook(format, sheet -> {
            sheet.createRow(0).createCell(0).setCellValue("first");
            sheet.createRow(1).createCell(lastColumn).setCellValue("last");
        }));
        assertEquals(lastColumn + 1, dataset.columnCount());
        assertEquals("Column_" + lastColumn, dataset.getColumnName(lastColumn));
        assertEquals("last", dataset.get(0, lastColumn));
        assertNull(dataset.get(0, lastColumn - 1));
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void headerOnlyAndCellFreeSheetsKeepTheirExistingShapes(final ExcelFormat format) throws Exception {
        assertEquals(0, read(workbook(format, sheet -> {
        })).columnCount());
        final Dataset cellFree = read(workbook(format, sheet -> {
            sheet.createRow(0);
            sheet.createRow(1);
        }));
        assertEquals(0, cellFree.columnCount());
        assertEquals(0, cellFree.size());
        final Dataset headers = read(workbook(format, sheet -> sheet.createRow(2).createCell(2).setCellValue("\u540D")));
        assertEquals(List.of("Column_0", "Column_1", "\u540D"), headers.columnNames());
        assertEquals(0, headers.size());
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void duplicateExplicitHeadersStillFailWithWiderData(final ExcelFormat format) throws Exception {
        final byte[] bytes = workbook(format, sheet -> {
            final Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("duplicate");
            header.createCell(1).setCellValue("duplicate");
            sheet.createRow(1).createCell(2).setCellValue("tail");
        });
        final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> read(bytes));
        assertEquals("Duplicate header name 'duplicate' in columns 0 and 1 of sheet: Data", failure.getMessage());
    }
}
