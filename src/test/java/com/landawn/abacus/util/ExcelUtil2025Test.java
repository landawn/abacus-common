package com.landawn.abacus.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedException;
import com.landawn.abacus.poi.ExcelUtil;
import com.landawn.abacus.poi.ExcelUtil.FreezePane;
import com.landawn.abacus.poi.ExcelUtil.RowExtractors;
import com.landawn.abacus.poi.ExcelUtil.RowMappers;
import com.landawn.abacus.poi.ExcelUtil.SheetCreateOptions;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.stream.Stream;

@Tag("2025")
public class ExcelUtil2025Test extends TestBase {

    private final List<File> tempFiles = new ArrayList<>();

    @AfterEach
    public void cleanup() {
        for (File file : tempFiles) {
            IOUtil.deleteQuietly(file);
        }
        tempFiles.clear();
    }

    private File createTempFile(String suffix) throws IOException {
        File file = File.createTempFile("excel_test_", suffix);
        tempFiles.add(file);
        return file;
    }

    @Test
    public void test_CELL_GETTER_String() {
        File file = new File("./src/test/resources/test_excel_01.xlsx");
        List<List<Object>> rows = ExcelUtil.readSheet(file);
        Assertions.assertNotNull(rows);
        Assertions.assertFalse(rows.isEmpty());
    }

    @Test
    public void test_CELL_GETTER_Numeric() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Number");
        List<List<Object>> rows = Arrays.asList(Arrays.asList(123.45));
        ExcelUtil.writeSheet("Sheet1", headers, rows, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile);
        Assertions.assertEquals(2, readRows.size());
        Assertions.assertEquals(123.45, readRows.get(1).get(0));
    }

    @Test
    public void test_CELL_GETTER_Boolean() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Flag");
        List<List<Object>> rows = Arrays.asList(Arrays.asList(true), Arrays.asList(false));
        ExcelUtil.writeSheet("Sheet1", headers, rows, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile);
        Assertions.assertEquals(3, readRows.size());
        Assertions.assertEquals(true, readRows.get(1).get(0));
        Assertions.assertEquals(false, readRows.get(2).get(0));
    }

    @Test
    public void test_CELL2STRING() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Text", "Number", "Bool");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("Hello", 42.5, true));
        ExcelUtil.writeSheet("Sheet1", headers, rows, tempFile);

        List<String> stringRow = ExcelUtil.readSheet(tempFile, 0, true, RowMappers.ROW2STRING);
        Assertions.assertEquals(1, stringRow.size());
        Assertions.assertTrue(stringRow.get(0).contains("Hello"));
    }

    @Test
    public void test_loadSheet_File() {
        File file = new File("./src/test/resources/test_excel_01.xlsx");
        Dataset dataset = ExcelUtil.loadSheet(file);
        Assertions.assertNotNull(dataset);
        Assertions.assertTrue(dataset.columnCount() > 0);
    }

    @Test
    public void test_loadSheet_File_SheetIndex_RowExtractor() {
        File file = new File("./src/test/resources/test_excel_01.xlsx");
        Dataset dataset = ExcelUtil.loadSheet(file, 0, RowExtractors.DEFAULT);
        Assertions.assertNotNull(dataset);
        Assertions.assertTrue(dataset.columnCount() > 0);
    }

    @Test
    public void test_loadSheet_File_SheetName_RowExtractor() throws Exception {
        File tempFile = createTempFile(".xlsx");
        Dataset dataset = CommonUtil.newDataset(CommonUtil.asList("col1", "col2"), CommonUtil.asList(CommonUtil.asList("a", "b"), CommonUtil.asList("c", "d")));
        ExcelUtil.writeSheet("TestSheet", dataset, tempFile);

        Dataset loaded = ExcelUtil.loadSheet(tempFile, "TestSheet", RowExtractors.DEFAULT);
        Assertions.assertNotNull(loaded);
        Assertions.assertEquals(2, loaded.columnCount());
        Assertions.assertEquals(2, loaded.size());
    }

    @Test
    public void test_loadSheet_EmptySheet() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("col1");
        List<List<Object>> rows = new ArrayList<>();
        ExcelUtil.writeSheet("Empty", headers, rows, tempFile);

        Dataset dataset = ExcelUtil.loadSheet(tempFile);
        Assertions.assertNotNull(dataset);
        Assertions.assertEquals(1, dataset.columnCount());
        Assertions.assertEquals(0, dataset.size());
    }

    @Test
    public void test_loadSheet_CustomRowExtractor() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("A", "B");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("x", "y"));
        ExcelUtil.writeSheet("Sheet1", headers, rows, tempFile);

        TriConsumer<String[], Row, Object[]> customExtractor = (hdrs, row, output) -> {
            int idx = 0;
            for (Cell cell : row) {
                output[idx++] = "CUSTOM_" + ExcelUtil.CELL2STRING.apply(cell);
            }
        };

        Dataset dataset = ExcelUtil.loadSheet(tempFile, 0, customExtractor);
        Assertions.assertEquals("CUSTOM_x", dataset.get(0, 0));
    }

    @Test
    public void test_readSheet_File() {
        File file = new File("./src/test/resources/test_excel_01.xlsx");
        List<List<Object>> rows = ExcelUtil.readSheet(file);
        Assertions.assertNotNull(rows);
        Assertions.assertFalse(rows.isEmpty());
    }

    @Test
    public void test_readSheet_File_SheetIndex_SkipFirstRow_RowMapper() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Name", "Age");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("Alice", 30), Arrays.asList("Bob", 25));
        ExcelUtil.writeSheet("People", headers, rows, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile, 0, true, RowMappers.DEFAULT);
        Assertions.assertEquals(2, readRows.size());
    }

    @Test
    public void test_readSheet_File_SheetIndex_NoSkip() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Col1");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("Data"));
        ExcelUtil.writeSheet("Sheet1", headers, rows, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile, 0, false, RowMappers.DEFAULT);
        Assertions.assertEquals(2, readRows.size());
    }

    @Test
    public void test_readSheet_File_SheetName_SkipFirstRow_RowMapper() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Product");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("Widget"));
        ExcelUtil.writeSheet("Products", headers, rows, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile, "Products", true, RowMappers.DEFAULT);
        Assertions.assertEquals(1, readRows.size());
    }

    @Test
    public void test_readSheet_CustomRowMapper() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("A", "B");
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1, 2));
        ExcelUtil.writeSheet("Sheet1", headers, rows, tempFile);

        Function<Row, Integer> rowMapper = row -> {
            int sum = 0;
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.NUMERIC) {
                    sum += (int) cell.getNumericCellValue();
                }
            }
            return sum;
        };

        List<Integer> sums = ExcelUtil.readSheet(tempFile, 0, true, rowMapper);
        Assertions.assertEquals(1, sums.size());
        Assertions.assertEquals(3, sums.get(0));
    }

    @Test
    public void test_streamSheet_File_SheetIndex_SkipFirstRow() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Val");
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList(3));
        ExcelUtil.writeSheet("Sheet1", headers, rows, tempFile);

        try (Stream<Row> stream = ExcelUtil.streamSheet(tempFile, 0, true)) {
            long count = stream.count();
            Assertions.assertEquals(3, count);
        }
    }

    @Test
    public void test_streamSheet_File_SheetIndex_NoSkip() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Val");
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1));
        ExcelUtil.writeSheet("Sheet1", headers, rows, tempFile);

        try (Stream<Row> stream = ExcelUtil.streamSheet(tempFile, 0, false)) {
            long count = stream.count();
            Assertions.assertEquals(2, count);
        }
    }

    @Test
    public void test_streamSheet_File_SheetName_SkipFirstRow() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Item");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("A"), Arrays.asList("B"));
        ExcelUtil.writeSheet("Items", headers, rows, tempFile);

        try (Stream<Row> stream = ExcelUtil.streamSheet(tempFile, "Items", true)) {
            long count = stream.count();
            Assertions.assertEquals(2, count);
        }
    }

    @Test
    public void test_streamSheet_FilterAndMap() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Number");
        List<List<Object>> rows = Arrays.asList(Arrays.asList(10), Arrays.asList(20), Arrays.asList(5));
        ExcelUtil.writeSheet("Numbers", headers, rows, tempFile);

        try (Stream<Row> stream = ExcelUtil.streamSheet(tempFile, 0, true)) {
            long count = stream.filter(row -> row.getCell(0).getNumericCellValue() > 8).count();
            Assertions.assertEquals(2, count);
        }
    }

    @Test
    public void test_writeSheet_SheetName_Headers_Rows_File() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Name", "Value");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("Item1", 100), Arrays.asList("Item2", 200));

        ExcelUtil.writeSheet("Data", headers, rows, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile, 0, false, RowMappers.DEFAULT);
        Assertions.assertEquals(3, readRows.size());
    }

    @Test
    public void test_writeSheet_SheetName_Headers_Rows_SheetCreateOptions_File() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Col1", "Col2");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("A", "B"));

        SheetCreateOptions options = SheetCreateOptions.builder().autoSizeColumn(true).freezeFirstRow(true).autoFilterByFirstRow(true).build();

        ExcelUtil.writeSheet("Formatted", headers, rows, options, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile);
        Assertions.assertEquals(2, readRows.size());
    }

    @Test
    public void test_writeSheet_SheetName_Headers_Rows_SheetSetter_File() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("X");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("Y"));

        Consumer<Sheet> sheetSetter = sheet -> {
            sheet.setDefaultColumnWidth(20);
        };

        ExcelUtil.writeSheet("Custom", headers, rows, sheetSetter, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile);
        Assertions.assertEquals(2, readRows.size());
    }

    @Test
    public void test_writeSheet_WithDataset() throws Exception {
        File tempFile = createTempFile(".xlsx");
        Dataset dataset = CommonUtil.newDataset(CommonUtil.asList("column1", "column2"), CommonUtil.asList(CommonUtil.asList("ab", "cd"), CommonUtil.asList("ef", "gh")));

        ExcelUtil.writeSheet("DatasetSheet", dataset, tempFile);

        Dataset loaded = ExcelUtil.loadSheet(tempFile);
        Assertions.assertEquals(dataset.columnCount(), loaded.columnCount());
        Assertions.assertEquals(dataset.size(), loaded.size());
    }

    @Test
    public void test_writeSheet_Dataset_SheetCreateOptions() throws Exception {
        File tempFile = createTempFile(".xlsx");
        Dataset dataset = CommonUtil.newDataset(CommonUtil.asList("A", "B"), CommonUtil.asList(CommonUtil.asList(1, 2)));

        SheetCreateOptions options = SheetCreateOptions.builder().autoSizeColumn(true).build();

        ExcelUtil.writeSheet("DS", dataset, options, tempFile);

        Dataset loaded = ExcelUtil.loadSheet(tempFile);
        Assertions.assertEquals(1, loaded.size());
    }

    @Test
    public void test_writeSheet_Dataset_SheetSetter() throws Exception {
        File tempFile = createTempFile(".xlsx");
        Dataset dataset = CommonUtil.newDataset(CommonUtil.asList("C"), CommonUtil.asList(CommonUtil.asList(3)));

        Consumer<Sheet> setter = sheet -> sheet.setDefaultRowHeight((short) 400);

        ExcelUtil.writeSheet("DS2", dataset, setter, tempFile);

        Dataset loaded = ExcelUtil.loadSheet(tempFile);
        Assertions.assertEquals(1, loaded.size());
    }

    @Test
    public void test_writeSheet_WithFreezePane() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("A", "B", "C");
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1, 2, 3));

        SheetCreateOptions options = SheetCreateOptions.builder().freezePane(new FreezePane(1, 1)).build();

        ExcelUtil.writeSheet("Frozen", headers, rows, options, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile);
        Assertions.assertEquals(2, readRows.size());
    }

    @Test
    public void test_writeSheet_WithAutoFilter() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("H1", "H2");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("v1", "v2"));

        SheetCreateOptions options = SheetCreateOptions.builder().autoFilter(new CellRangeAddress(0, 0, 0, 1)).build();

        ExcelUtil.writeSheet("Filtered", headers, rows, options, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile);
        Assertions.assertEquals(2, readRows.size());
    }

    @Test
    public void test_writeSheet_EmptyRows() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Header");
        List<List<Object>> rows = new ArrayList<>();

        ExcelUtil.writeSheet("EmptyData", headers, rows, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile);
        Assertions.assertEquals(1, readRows.size());
    }

    @Test
    public void test_writeSheet_VariousDataTypes() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("String", "Integer", "Double", "Boolean", "Date");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("text", 42, 3.14, true, new java.util.Date()));

        ExcelUtil.writeSheet("Types", headers, rows, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile, 0, true, RowMappers.DEFAULT);
        Assertions.assertEquals(1, readRows.size());
        Assertions.assertNotNull(readRows.get(0).get(0));
    }

    @Test
    public void test_saveSheetAsCsv_File_SheetIndex_CsvFile() throws Exception {
        File excelFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Name", "Age");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("John", 30));
        ExcelUtil.writeSheet("People", headers, rows, excelFile);

        File csvFile = createTempFile(".csv");
        ExcelUtil.saveSheetAsCsv(excelFile, 0, csvFile);

        Assertions.assertTrue(csvFile.exists());
        String content = IOUtil.readAllToString(csvFile);
        Assertions.assertTrue(content.contains("Name"));
        Assertions.assertTrue(content.contains("John"));
    }

    @Test
    public void test_saveSheetAsCsv_File_SheetName_CsvFile() throws Exception {
        File excelFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Product");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("Widget"));
        ExcelUtil.writeSheet("Products", headers, rows, excelFile);

        File csvFile = createTempFile(".csv");
        ExcelUtil.saveSheetAsCsv(excelFile, "Products", csvFile);

        Assertions.assertTrue(csvFile.exists());
        String content = IOUtil.readAllToString(csvFile);
        Assertions.assertTrue(content.contains("Product"));
    }

    @Test
    public void test_saveSheetAsCsv_File_SheetIndex_CustomHeaders_CsvFile_Charset() throws Exception {
        File excelFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("A", "B");
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1, 2));
        ExcelUtil.writeSheet("Data", headers, rows, excelFile);

        File csvFile = createTempFile(".csv");
        List<String> customHeaders = Arrays.asList("Column1", "Column2");
        ExcelUtil.saveSheetAsCsv(excelFile, 0, customHeaders, csvFile, StandardCharsets.UTF_8);

        String content = IOUtil.readAllToString(csvFile);
        Assertions.assertTrue(content.contains("Column1"));
        Assertions.assertTrue(content.contains("Column2"));
    }

    @Test
    public void test_saveSheetAsCsv_File_SheetName_CustomHeaders_CsvFile_Charset() throws Exception {
        File excelFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("X");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("Y"));
        ExcelUtil.writeSheet("TestSheet", headers, rows, excelFile);

        File csvFile = createTempFile(".csv");
        List<String> customHeaders = Arrays.asList("Custom");
        ExcelUtil.saveSheetAsCsv(excelFile, "TestSheet", customHeaders, csvFile, Charsets.UTF_8);

        String content = IOUtil.readAllToString(csvFile);
        Assertions.assertTrue(content.contains("Custom"));
    }

    @Test
    public void test_saveSheetAsCsv_MultipleRows() throws Exception {
        File excelFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Col1", "Col2", "Col3");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("A", "B", "C"), Arrays.asList("D", "E", "F"), Arrays.asList("G", "H", "I"));
        ExcelUtil.writeSheet("Sheet1", headers, rows, excelFile);

        File csvFile = createTempFile(".csv");
        ExcelUtil.saveSheetAsCsv(excelFile, 0, csvFile);

        String content = IOUtil.readAllToString(csvFile);
        Assertions.assertTrue(content.contains("Col1"));
        Assertions.assertTrue(content.contains("A"));
        Assertions.assertTrue(content.contains("G"));
    }

    @Test
    public void test_RowMappers_DEFAULT() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("A", "B");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("x", 1));
        ExcelUtil.writeSheet("Sheet1", headers, rows, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile, 0, true, RowMappers.DEFAULT);
        Assertions.assertEquals(1, readRows.size());
        Assertions.assertEquals("x", readRows.get(0).get(0));
    }

    @Test
    public void test_RowMappers_ROW2STRING() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("A");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("test"));
        ExcelUtil.writeSheet("Sheet1", headers, rows, tempFile);

        List<String> stringRows = ExcelUtil.readSheet(tempFile, 0, true, RowMappers.ROW2STRING);
        Assertions.assertEquals(1, stringRows.size());
        Assertions.assertTrue(stringRows.get(0).contains("test"));
    }

    @Test
    public void test_RowMappers_toString_CustomSeparator() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("A", "B");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("one", "two"));
        ExcelUtil.writeSheet("Sheet1", headers, rows, tempFile);

        Function<Row, String> mapper = RowMappers.toString("|");
        List<String> stringRows = ExcelUtil.readSheet(tempFile, 0, true, mapper);
        Assertions.assertEquals(1, stringRows.size());
        Assertions.assertTrue(stringRows.get(0).contains("|"));
    }

    @Test
    public void test_RowMappers_toString_CustomSeparator_CustomCellMapper() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("A", "B");
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1, 2));
        ExcelUtil.writeSheet("Sheet1", headers, rows, tempFile);

        Function<Cell, String> cellMapper = cell -> "VAL";
        Function<Row, String> mapper = RowMappers.toString(",", cellMapper);
        List<String> stringRows = ExcelUtil.readSheet(tempFile, 0, true, mapper);
        Assertions.assertEquals(1, stringRows.size());
        Assertions.assertEquals("VAL,VAL", stringRows.get(0));
    }

    @Test
    public void test_RowMappers_toList() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Num");
        List<List<Object>> rows = Arrays.asList(Arrays.asList(42));
        ExcelUtil.writeSheet("Sheet1", headers, rows, tempFile);

        Function<Cell, Integer> cellMapper = cell -> {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
            return 0;
        };
        Function<Row, List<Integer>> mapper = RowMappers.toList(cellMapper);
        List<List<Integer>> intRows = ExcelUtil.readSheet(tempFile, 0, true, mapper);
        Assertions.assertEquals(1, intRows.size());
        Assertions.assertEquals(42, intRows.get(0).get(0));
    }

    @Test
    public void test_RowExtractors_DEFAULT() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Col");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("val"));
        ExcelUtil.writeSheet("Sheet1", headers, rows, tempFile);

        Dataset dataset = ExcelUtil.loadSheet(tempFile, 0, RowExtractors.DEFAULT);
        Assertions.assertEquals(1, dataset.size());
    }

    @Test
    public void test_RowExtractors_create() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("A");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("text"));
        ExcelUtil.writeSheet("Sheet1", headers, rows, tempFile);

        Function<Cell, String> cellMapper = cell -> "PREFIX_" + ExcelUtil.CELL2STRING.apply(cell);
        TriConsumer<String[], Row, Object[]> extractor = RowExtractors.create(cellMapper);
        Dataset dataset = ExcelUtil.loadSheet(tempFile, 0, extractor);
        Assertions.assertEquals("PREFIX_text", dataset.get(0, 0));
    }

    @Test
    public void test_SheetCreateOptions_AllFields() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("A", "B");
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1, 2));

        SheetCreateOptions options = SheetCreateOptions.builder()
                .autoSizeColumn(true)
                .freezePane(new FreezePane(1, 1))
                .freezeFirstRow(false)
                .autoFilter(new CellRangeAddress(0, 0, 0, 1))
                .autoFilterByFirstRow(false)
                .build();

        ExcelUtil.writeSheet("Options", headers, rows, options, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile);
        Assertions.assertEquals(2, readRows.size());
    }

    @Test
    public void test_SheetCreateOptions_OnlyAutoSizeColumn() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Column");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("Value"));

        SheetCreateOptions options = SheetCreateOptions.builder().autoSizeColumn(true).build();

        ExcelUtil.writeSheet("Auto", headers, rows, options, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile);
        Assertions.assertEquals(2, readRows.size());
    }

    @Test
    public void test_SheetCreateOptions_OnlyFreezeFirstRow() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("H");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("V"));

        SheetCreateOptions options = SheetCreateOptions.builder().freezeFirstRow(true).build();

        ExcelUtil.writeSheet("Freeze", headers, rows, options, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile);
        Assertions.assertEquals(2, readRows.size());
    }

    @Test
    public void test_SheetCreateOptions_OnlyAutoFilterByFirstRow() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Filter");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("Data"));

        SheetCreateOptions options = SheetCreateOptions.builder().autoFilterByFirstRow(true).build();

        ExcelUtil.writeSheet("FilterSheet", headers, rows, options, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile);
        Assertions.assertEquals(2, readRows.size());
    }

    @Test
    public void test_FreezePane_BothZero() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("A");
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1));

        FreezePane freezePane = new FreezePane(0, 0);
        SheetCreateOptions options = SheetCreateOptions.builder().freezePane(freezePane).build();

        ExcelUtil.writeSheet("NFreeze", headers, rows, options, tempFile);

        Assertions.assertEquals(0, freezePane.colSplit());
        Assertions.assertEquals(0, freezePane.rowSplit());
    }

    @Test
    public void test_FreezePane_ColumnOnly() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("A", "B", "C");
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1, 2, 3));

        FreezePane freezePane = new FreezePane(2, 0);
        SheetCreateOptions options = SheetCreateOptions.builder().freezePane(freezePane).build();

        ExcelUtil.writeSheet("ColFreeze", headers, rows, options, tempFile);

        Assertions.assertEquals(2, freezePane.colSplit());
        Assertions.assertEquals(0, freezePane.rowSplit());
    }

    @Test
    public void test_FreezePane_RowOnly() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("A");
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1), Arrays.asList(2));

        FreezePane freezePane = new FreezePane(0, 2);
        SheetCreateOptions options = SheetCreateOptions.builder().freezePane(freezePane).build();

        ExcelUtil.writeSheet("RowFreeze", headers, rows, options, tempFile);

        Assertions.assertEquals(0, freezePane.colSplit());
        Assertions.assertEquals(2, freezePane.rowSplit());
    }

    @Test
    public void test_loadSheet_NonExistentFile() {
        File nonExistent = new File("./nonexistent_file_12345.xlsx");
        Assertions.assertThrows(UncheckedException.class, () -> {
            ExcelUtil.loadSheet(nonExistent);
        });
    }

    @Test
    public void test_readSheet_NonExistentFile() {
        File nonExistent = new File("./nonexistent_file_12345.xlsx");
        Assertions.assertThrows(UncheckedException.class, () -> {
            ExcelUtil.readSheet(nonExistent);
        });
    }

    @Test
    public void test_streamSheet_NonExistentFile() {
        File nonExistent = new File("./nonexistent_file_12345.xlsx");
        Assertions.assertThrows(UncheckedException.class, () -> {
            ExcelUtil.streamSheet(nonExistent, 0, false);
        });
    }

    @Test
    public void test_saveSheetAsCsv_NonExistentFile() {
        File nonExistent = new File("./nonexistent_file_12345.xlsx");
        File csvFile = new File("./temp.csv");
        Assertions.assertThrows(UncheckedException.class, () -> {
            ExcelUtil.saveSheetAsCsv(nonExistent, 0, csvFile);
        });
    }

    @Test
    public void test_RoundTrip_WriteAndLoad() throws Exception {
        File tempFile = createTempFile(".xlsx");
        Dataset original = CommonUtil.newDataset(CommonUtil.asList("name", "value", "flag"),
                CommonUtil.asList(CommonUtil.asList("A", 10, true), CommonUtil.asList("B", 20, false), CommonUtil.asList("C", 30, true)));

        ExcelUtil.writeSheet("Data", original, tempFile);
        Dataset loaded = ExcelUtil.loadSheet(tempFile);

        Assertions.assertEquals(original.columnCount(), loaded.columnCount());
        Assertions.assertEquals(original.size(), loaded.size());
        Assertions.assertEquals(original.getColumnName(0), loaded.getColumnName(0));
    }

    @Test
    public void test_RoundTrip_ExcelToCsvToDataset() throws Exception {
        File excelFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("X", "Y");
        List<List<Object>> rows = Arrays.asList(Arrays.asList(1, 2), Arrays.asList(3, 4));
        ExcelUtil.writeSheet("Numbers", headers, rows, excelFile);

        File csvFile = createTempFile(".csv");
        ExcelUtil.saveSheetAsCsv(excelFile, 0, csvFile);

        Dataset dataset = CSVUtil.loadCSV(csvFile);
        Assertions.assertEquals(2, dataset.columnCount());
        Assertions.assertEquals(2, dataset.size());
    }

    @Test
    public void test_MultipleSheets_ByName() throws Exception {
        File tempFile = createTempFile(".xlsx");

        List<Object> headers1 = Arrays.asList("Sheet1Col");
        List<List<Object>> rows1 = Arrays.asList(Arrays.asList("S1Data"));
        ExcelUtil.writeSheet("FirstSheet", headers1, rows1, tempFile);

        Dataset ds1 = ExcelUtil.loadSheet(tempFile, "FirstSheet", RowExtractors.DEFAULT);
        Assertions.assertEquals(1, ds1.columnCount());
        Assertions.assertEquals("Sheet1Col", ds1.getColumnName(0));
    }

    @Test
    public void test_LargeDataset() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("ID", "Name", "Value");
        List<List<Object>> rows = new ArrayList<>();

        for (int i = 0; i < 100; i++) {
            rows.add(Arrays.asList(i, "Name" + i, i * 10));
        }

        ExcelUtil.writeSheet("Large", headers, rows, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile, 0, true, RowMappers.DEFAULT);
        Assertions.assertEquals(100, readRows.size());
    }

    @Test
    public void test_StreamProcessing_LargeFile() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Num");
        List<List<Object>> rows = new ArrayList<>();

        for (int i = 1; i <= 50; i++) {
            rows.add(Arrays.asList(i));
        }

        ExcelUtil.writeSheet("Stream", headers, rows, tempFile);

        try (Stream<Row> stream = ExcelUtil.streamSheet(tempFile, 0, true)) {
            long sum = stream.mapToLong(row -> (long) row.getCell(0).getNumericCellValue()).sum();
            Assertions.assertEquals(1275, sum);
        }
    }

    @Test
    public void test_NullValues() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Col1", "Col2");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("NotNull", null), Arrays.asList(null, "AlsoNotNull"));

        ExcelUtil.writeSheet("Nulls", headers, rows, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile, 0, true, RowMappers.DEFAULT);
        Assertions.assertEquals(2, readRows.size());
    }

    @Test
    public void test_SpecialCharacters() throws Exception {
        File tempFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Special");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("Hello, \"World\""), Arrays.asList("Line1\nLine2"), Arrays.asList("Tab\there"));

        ExcelUtil.writeSheet("Special", headers, rows, tempFile);

        List<List<Object>> readRows = ExcelUtil.readSheet(tempFile, 0, true, RowMappers.DEFAULT);
        Assertions.assertEquals(3, readRows.size());
    }
}
