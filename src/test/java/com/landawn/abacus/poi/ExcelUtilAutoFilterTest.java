package com.landawn.abacus.poi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.poi.ExcelUtil.ExcelFormat;
import com.landawn.abacus.poi.ExcelUtil.SheetCreateOptions;
import com.landawn.abacus.util.N;

public class ExcelUtilAutoFilterTest extends TestBase {
    private final SheetCreateOptions automatic = SheetCreateOptions.builder().autoFilterByFirstRow(true).build();

    private static void assertPersistedRange(final byte[] bytes, final String range) throws Exception {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            final var filters = workbook.getAllNames().stream().filter(name -> name.getNameName().endsWith("_FilterDatabase")).toList();
            if (range == null) {
                assertTrue(filters.isEmpty());
            } else {
                assertEquals(1, filters.size());
                // HSSF normalizes a complete column to A:A instead of spelling out its row bounds.
                final var actual = new AreaReference(filters.get(0).getRefersToFormula(), workbook.getSpreadsheetVersion());
                final var expected = new AreaReference(range, workbook.getSpreadsheetVersion());
                assertEquals(expected.getFirstCell().getRow(), Math.max(0, actual.getFirstCell().getRow()));
                assertEquals(expected.getFirstCell().getCol(), actual.getFirstCell().getCol());
                assertEquals(expected.getLastCell().getRow(),
                        actual.getLastCell().getRow() < 0 ? workbook.getSpreadsheetVersion().getLastRowIndex() : actual.getLastCell().getRow());
                assertEquals(expected.getLastCell().getCol(), actual.getLastCell().getCol());
            }
        }
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void rowsAndDatasetIncludeEveryDataRow(final ExcelFormat format) throws Exception {
        final var output = new ByteArrayOutputStream();
        ExcelUtil.writeRowsToSheet("\u540D\uD83D\uDE80", List.of("Name", "Value"), List.of(List.of("a", 1), List.of("b", 2)),
                ExcelUtil.createSheetSetter(automatic, 2), output, format);
        assertPersistedRange(output.toByteArray(), "$A$1:$B$3");
        output.reset();
        ExcelUtil.writeDatasetToSheet("Data", N.newDataset(List.of("Name", "Value"), List.of(List.of("a", "b"), List.of(1, 2))),
                ExcelUtil.createSheetSetter(automatic, 2), output, format);
        assertPersistedRange(output.toByteArray(), "$A$1:$B$3");
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void headerOnlyAndZeroColumnsAreSupported(final ExcelFormat format) throws Exception {
        final var output = new ByteArrayOutputStream();
        ExcelUtil.writeRowsToSheet("Data", List.of("A"), List.of(), ExcelUtil.createSheetSetter(automatic, 1), output, format);
        assertPersistedRange(output.toByteArray(), "$A$1:$A$1");
        output.reset();
        ExcelUtil.writeRowsToSheet("Data", List.of(), List.of(), ExcelUtil.createSheetSetter(automatic, 0), output, format);
        assertPersistedRange(output.toByteArray(), null);
        output.reset();
        ExcelUtil.writeRowsToSheet("Data", List.of("A"), List.of(List.of("x")), ExcelUtil.createSheetSetter(null, 1), output, format);
        assertPersistedRange(output.toByteArray(), null);
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void explicitRangeTakesPrecedence(final ExcelFormat format) throws Exception {
        final var output = new ByteArrayOutputStream();
        final var options = SheetCreateOptions.builder().autoFilterByFirstRow(true).autoFilter(new CellRangeAddress(0, 1, 0, 0)).build();
        ExcelUtil.writeRowsToSheet("Data", List.of("A", "B"), List.of(List.of(1, 2), List.of(3, 4)), ExcelUtil.createSheetSetter(options, 2), output, format);
        assertPersistedRange(output.toByteArray(), "$A$1:$A$2");
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void sparseLastSupportedRowIsIncluded(final ExcelFormat format) throws Exception {
        try (Workbook workbook = format == ExcelFormat.XLS ? new HSSFWorkbook() : new XSSFWorkbook()) {
            final Sheet sheet = workbook.createSheet("Data");
            sheet.createRow(0).createCell(0).setCellValue("Header");
            final int lastRow = workbook.getSpreadsheetVersion().getLastRowIndex();
            sheet.createRow(lastRow).createCell(0).setCellValue("\u540D");
            ExcelUtil.createSheetSetter(automatic, 1).accept(sheet);
            final var output = new ByteArrayOutputStream();
            workbook.write(output);
            assertPersistedRange(output.toByteArray(), "$A$1:$A$" + (lastRow + 1));
        }
    }
}
