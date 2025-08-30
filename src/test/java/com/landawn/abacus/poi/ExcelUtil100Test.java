package com.landawn.abacus.poi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.poi.ExcelUtil.FreezePane;
import com.landawn.abacus.poi.ExcelUtil.RowExtractors;
import com.landawn.abacus.poi.ExcelUtil.RowMappers;
import com.landawn.abacus.poi.ExcelUtil.SheetCreateOptions;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.RowDataset;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.stream.Stream;

import com.landawn.abacus.TestBase;


public class ExcelUtil100Test extends TestBase {

    @TempDir
    File tempDir;

    private File testExcelFile;
    private File outputExcelFile;
    private File outputCsvFile;

    @BeforeEach
    public void setUp() throws IOException {
        testExcelFile = new File(tempDir, "test.xlsx");
        outputExcelFile = new File(tempDir, "output.xlsx");
        outputCsvFile = new File(tempDir, "output.csv");
        createTestExcelFile();
    }

    private void createTestExcelFile() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet1 = workbook.createSheet("Sheet1");
            createTestSheet(sheet1);
            
            Sheet sheet2 = workbook.createSheet("TestSheet");
            createTestSheet(sheet2);
            
            try (var fos = new java.io.FileOutputStream(testExcelFile)) {
                workbook.write(fos);
            }
        }
    }

    private void createTestSheet(Sheet sheet) {
        Row headerRow = sheet.createRow(0);
        headerRow.createCell(0).setCellValue("Name");
        headerRow.createCell(1).setCellValue("Age");
        headerRow.createCell(2).setCellValue("Active");
        headerRow.createCell(3).setCellValue("Score");

        Row dataRow1 = sheet.createRow(1);
        dataRow1.createCell(0).setCellValue("John");
        dataRow1.createCell(1).setCellValue(30);
        dataRow1.createCell(2).setCellValue(true);
        dataRow1.createCell(3).setCellValue(95.5);

        Row dataRow2 = sheet.createRow(2);
        dataRow2.createCell(0).setCellValue("Jane");
        dataRow2.createCell(1).setCellValue(25);
        dataRow2.createCell(2).setCellValue(false);
        dataRow2.createCell(3).setCellValue(87.3);
    }

    @Test
    public void testCELL_GETTER() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            
            Cell stringCell = row.createCell(0);
            stringCell.setCellValue("test");
            assertEquals("test", ExcelUtil.CELL_GETTER.apply(stringCell));
            
            Cell numericCell = row.createCell(1);
            numericCell.setCellValue(42.5);
            assertEquals(42.5, ExcelUtil.CELL_GETTER.apply(numericCell));
            
            Cell booleanCell = row.createCell(2);
            booleanCell.setCellValue(true);
            assertEquals(true, ExcelUtil.CELL_GETTER.apply(booleanCell));
            
            Cell formulaCell = row.createCell(3);
            formulaCell.setCellFormula("A1");
            assertEquals("A1", ExcelUtil.CELL_GETTER.apply(formulaCell));
            
            Cell blankCell = row.createCell(4);
            assertEquals("", ExcelUtil.CELL_GETTER.apply(blankCell));
        }
    }

    @Test
    public void testCELL2STRING() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            
            Cell stringCell = row.createCell(0);
            stringCell.setCellValue("test");
            assertEquals("test", ExcelUtil.CELL2STRING.apply(stringCell));
            
            Cell numericCell = row.createCell(1);
            numericCell.setCellValue(42.5);
            assertEquals("42.5", ExcelUtil.CELL2STRING.apply(numericCell));
            
            Cell booleanCell = row.createCell(2);
            booleanCell.setCellValue(true);
            assertEquals("true", ExcelUtil.CELL2STRING.apply(booleanCell));
            
            Cell formulaCell = row.createCell(3);
            formulaCell.setCellFormula("A1");
            assertEquals("A1", ExcelUtil.CELL2STRING.apply(formulaCell));
            
            Cell blankCell = row.createCell(4);
            assertEquals("", ExcelUtil.CELL2STRING.apply(blankCell));
        }
    }

    @Test
    public void testLoadSheet_File() {
        Dataset dataset = ExcelUtil.loadSheet(testExcelFile);
        
        assertNotNull(dataset);
        assertEquals(4, dataset.columnCount());
        assertEquals(2, dataset.size());
        
        List<String> columnNames = dataset.columnNameList();
        assertEquals(Arrays.asList("Name", "Age", "Active", "Score"), columnNames);
        
        assertEquals("John", dataset.get(0, 0));
        assertEquals(30.0, dataset.get(0, 1));
        assertEquals(true, dataset.get(0, 2));
        assertEquals(95.5, dataset.get(0, 3));
    }

    @Test
    public void testLoadSheet_FileIndexExtractor() {
        TriConsumer<String[], Row, Object[]> customExtractor = (headers, row, output) -> {
            for (int i = 0; i < headers.length; i++) {
                Cell cell = row.getCell(i);
                output[i] = cell == null ? null : ExcelUtil.CELL2STRING.apply(cell);
            }
        };
        
        Dataset dataset = ExcelUtil.loadSheet(testExcelFile, 0, customExtractor);
        
        assertNotNull(dataset);
        assertEquals(4, dataset.columnCount());
        assertEquals(2, dataset.size());
        
        assertEquals("John", dataset.get(0, 0));
        assertEquals("30.0", dataset.get(0, 1));
        assertEquals("true", dataset.get(0, 2));
        assertEquals("95.5", dataset.get(0, 3));
    }

    @Test
    public void testLoadSheet_FileNameExtractor() {
        Dataset dataset = ExcelUtil.loadSheet(testExcelFile, "TestSheet", RowExtractors.DEFAULT);
        
        assertNotNull(dataset);
        assertEquals(4, dataset.columnCount());
        assertEquals(2, dataset.size());
        
        assertEquals("John", dataset.get(0, 0));
        assertEquals(30.0, dataset.get(0, 1));
    }

    @Test
    public void testReadSheet_File() {
        List<List<Object>> rows = ExcelUtil.readSheet(testExcelFile);
        
        assertNotNull(rows);
        assertEquals(3, rows.size()); // Including header
        
        List<Object> headerRow = rows.get(0);
        assertEquals(Arrays.asList("Name", "Age", "Active", "Score"), headerRow);
        
        List<Object> dataRow1 = rows.get(1);
        assertEquals("John", dataRow1.get(0));
        assertEquals(30.0, dataRow1.get(1));
    }

    @Test
    public void testReadSheet_FileIndexSkipMapper() {
        Function<Row, String> rowToString = row -> {
            StringBuilder sb = new StringBuilder();
            row.forEach(cell -> sb.append(ExcelUtil.CELL2STRING.apply(cell)).append(","));
            return sb.toString();
        };
        
        List<String> rows = ExcelUtil.readSheet(testExcelFile, 0, true, rowToString);
        
        assertNotNull(rows);
        assertEquals(2, rows.size()); // Skipped header
        
        assertTrue(rows.get(0).startsWith("John,30"));
        assertTrue(rows.get(1).startsWith("Jane,25"));
    }

    @Test
    public void testReadSheet_FileNameSkipMapper() {
        List<List<Object>> rows = ExcelUtil.readSheet(testExcelFile, "TestSheet", false, RowMappers.DEFAULT);
        
        assertNotNull(rows);
        assertEquals(3, rows.size());
        
        List<Object> headerRow = rows.get(0);
        assertEquals("Name", headerRow.get(0));
    }

    @Test
    public void testStreamSheet_FileIndex() {
        Stream<Row> stream = ExcelUtil.streamSheet(testExcelFile, 0, false);
        
        List<Row> rows = stream.toList();
        assertEquals(3, rows.size());
        
        Row firstRow = rows.get(0);
        assertEquals("Name", firstRow.getCell(0).getStringCellValue());
    }

    @Test
    public void testStreamSheet_FileName() {
        Stream<Row> stream = ExcelUtil.streamSheet(testExcelFile, "TestSheet", true);
        
        List<Row> rows = stream.toList();
        assertEquals(2, rows.size()); // Skipped header
        
        Row firstDataRow = rows.get(0);
        assertEquals("John", firstDataRow.getCell(0).getStringCellValue());
    }

    @Test
    public void testWriteSheet_SimpleHeaders() {
        List<Object> headers = Arrays.asList("Col1", "Col2", "Col3");
        List<List<String>> rows = Arrays.asList(
            Arrays.asList("A1", "B1", "C1"),
            Arrays.asList("A2", "B2", "C2")
        );
        
        ExcelUtil.writeSheet("TestSheet", headers, rows, outputExcelFile);
        
        assertTrue(outputExcelFile.exists());
        
        // Verify written data
        Dataset dataset = ExcelUtil.loadSheet(outputExcelFile);
        assertEquals(3, dataset.columnCount());
        assertEquals(2, dataset.size());
        assertEquals("A1", dataset.get(0, 0));
    }

    @Test
    public void testWriteSheet_WithOptions() {
        SheetCreateOptions options = SheetCreateOptions.builder()
            .autoSizeColumn(true)
            .freezeFirstRow(true)
            .autoFilterByFirstRow(true)
            .build();
        
        List<Object> headers = Arrays.asList("Name", "Value");
        List<List<String>> rows = Arrays.asList(
            Arrays.asList("Item1", "100"),
            Arrays.asList("Item2", "200")
        );
        
        ExcelUtil.writeSheet("OptionsSheet", headers, rows, options, outputExcelFile);
        
        assertTrue(outputExcelFile.exists());
        
        Dataset dataset = ExcelUtil.loadSheet(outputExcelFile);
        assertEquals(2, dataset.columnCount());
        assertEquals(2, dataset.size());
    }

    @Test
    public void testWriteSheet_WithConsumer() {
        Consumer<Sheet> sheetSetter = sheet -> {
            sheet.setDefaultColumnWidth(20);
        };
        
        List<Object> headers = Arrays.asList("Header1", "Header2");
        List<List<Object>> rows = Arrays.asList(
            Arrays.asList("Data1", 123),
            Arrays.asList("Data2", 456)
        );
        
        ExcelUtil.writeSheet("ConsumerSheet", headers, rows, sheetSetter, outputExcelFile);
        
        assertTrue(outputExcelFile.exists());
    }

    @Test
    public void testWriteSheet_Dataset() {
        List<String> columnNames = Arrays.asList("ID", "Name", "Score");
        List<List<Object>> columnData = Arrays.asList(
            Arrays.asList(1, 2, 3),
            Arrays.asList("Alice", "Bob", "Charlie"),
            Arrays.asList(90.5, 85.0, 92.3)
        );
        
        Dataset dataset = new RowDataset(columnNames, columnData);
        
        ExcelUtil.writeSheet("DatasetSheet", dataset, outputExcelFile);
        
        assertTrue(outputExcelFile.exists());
        
        Dataset loaded = ExcelUtil.loadSheet(outputExcelFile);
        assertEquals(3, loaded.columnCount());
        assertEquals(3, loaded.size());
    }

    @Test
    public void testWriteSheet_DatasetWithOptions() {
        SheetCreateOptions options = SheetCreateOptions.builder()
            .freezePane(new FreezePane(1, 1))
            .autoSizeColumn(true)
            .build();
        
        List<String> columnNames = Arrays.asList("A", "B");
        List<List<Object>> columnData = Arrays.asList(
            Arrays.asList(1, 2),
            Arrays.asList("X", "Y")
        );
        
        Dataset dataset = new RowDataset(columnNames, columnData);
        
        ExcelUtil.writeSheet("OptionsDataset", dataset, options, outputExcelFile);
        
        assertTrue(outputExcelFile.exists());
    }

    @Test
    public void testWriteSheet_DatasetWithConsumer() {
        Consumer<Sheet> customizer = sheet -> {
            sheet.createFreezePane(0, 1);
        };
        
        List<String> columnNames = Arrays.asList("Col");
        List<List<Object>> columnData = Arrays.asList(
            Arrays.asList("Value1", "Value2")
        );
        
        Dataset dataset = new RowDataset(columnNames, columnData);
        
        ExcelUtil.writeSheet("CustomizedSheet", dataset, customizer, outputExcelFile);
        
        assertTrue(outputExcelFile.exists());
    }

    @Test
    public void testSaveSheetAsCsv_Index() {
        ExcelUtil.saveSheetAsCsv(testExcelFile, 0, outputCsvFile);
        
        assertTrue(outputCsvFile.exists());
        assertTrue(outputCsvFile.length() > 0);
    }

    @Test
    public void testSaveSheetAsCsv_Name() {
        ExcelUtil.saveSheetAsCsv(testExcelFile, "TestSheet", outputCsvFile);
        
        assertTrue(outputCsvFile.exists());
        assertTrue(outputCsvFile.length() > 0);
    }

    @Test
    public void testSaveSheetAsCsv_IndexWithHeaders() {
        List<String> customHeaders = Arrays.asList("CustomName", "CustomAge", "CustomActive", "CustomScore");
        
        ExcelUtil.saveSheetAsCsv(testExcelFile, 0, customHeaders, outputCsvFile, StandardCharsets.UTF_8);
        
        assertTrue(outputCsvFile.exists());
        assertTrue(outputCsvFile.length() > 0);
    }

    @Test
    public void testSaveSheetAsCsv_NameWithHeaders() {
        List<String> customHeaders = Arrays.asList("H1", "H2", "H3", "H4");
        
        ExcelUtil.saveSheetAsCsv(testExcelFile, "TestSheet", customHeaders, outputCsvFile, StandardCharsets.ISO_8859_1);
        
        assertTrue(outputCsvFile.exists());
        assertTrue(outputCsvFile.length() > 0);
    }

    @Test
    public void testRowMappers_DEFAULT() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("text");
            row.createCell(1).setCellValue(42);
            row.createCell(2).setCellValue(true);
            
            List<Object> result = RowMappers.DEFAULT.apply(row);
            
            assertEquals(3, result.size());
            assertEquals("text", result.get(0));
            assertEquals(42.0, result.get(1));
            assertEquals(true, result.get(2));
        }
    }

    @Test
    public void testRowMappers_ROW2STRING() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("A");
            row.createCell(1).setCellValue(10);
            row.createCell(2).setCellValue(false);
            
            String result = RowMappers.ROW2STRING.apply(row);
            
            assertNotNull(result);
            assertTrue(result.contains("A"));
            assertTrue(result.contains("10"));
            assertTrue(result.contains("false"));
        }
    }

    @Test
    public void testRowMappers_toString() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("X");
            row.createCell(1).setCellValue(99);
            
            Function<Row, String> mapper = RowMappers.toString("|");
            String result = mapper.apply(row);
            
            assertEquals("X|99.0", result);
        }
    }

    @Test
    public void testRowMappers_toStringWithCellMapper() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("test");
            row.createCell(1).setCellValue(123.456);
            
            Function<Cell, String> cellMapper = cell -> {
                if (cell.getCellType() == CellType.NUMERIC) {
                    return String.format("%.1f", cell.getNumericCellValue());
                }
                return ExcelUtil.CELL2STRING.apply(cell);
            };
            
            Function<Row, String> mapper = RowMappers.toString(",", cellMapper);
            String result = mapper.apply(row);
            
            assertEquals("test,123.5", result);
        }
    }

    @Test
    public void testRowMappers_toList() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("A");
            row.createCell(1).setCellValue(1.5);
            row.createCell(2).setCellValue(true);
            
            Function<Cell, String> cellToString = ExcelUtil.CELL2STRING;
            Function<Row, List<String>> mapper = RowMappers.toList(cellToString);
            
            List<String> result = mapper.apply(row);
            
            assertEquals(3, result.size());
            assertEquals("A", result.get(0));
            assertEquals("1.5", result.get(1));
            assertEquals("true", result.get(2));
        }
    }

    @Test
    public void testRowExtractors_DEFAULT() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("Value1");
            row.createCell(1).setCellValue(100);
            row.createCell(2).setCellValue(false);
            
            String[] headers = {"Col1", "Col2", "Col3"};
            Object[] output = new Object[3];
            
            RowExtractors.DEFAULT.accept(headers, row, output);
            
            assertEquals("Value1", output[0]);
            assertEquals(100.0, output[1]);
            assertEquals(false, output[2]);
        }
    }

    @Test
    public void testRowExtractors_create() throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("test");
            row.createCell(1).setCellValue(42);
            
            Function<Cell, String> upperCaseMapper = cell -> 
                ExcelUtil.CELL2STRING.apply(cell).toUpperCase();
            
            TriConsumer<String[], Row, Object[]> extractor = RowExtractors.create(upperCaseMapper);
            
            String[] headers = {"H1", "H2"};
            Object[] output = new Object[2];
            
            extractor.accept(headers, row, output);
            
            assertEquals("TEST", output[0]);
            assertEquals("42.0", output[1]);
        }
    }

    @Test
    public void testSheetCreateOptions() {
        SheetCreateOptions options = SheetCreateOptions.builder()
            .autoSizeColumn(true)
            .freezePane(new FreezePane(2, 3))
            .freezeFirstRow(false)
            .autoFilter(new CellRangeAddress(0, 10, 0, 5))
            .autoFilterByFirstRow(false)
            .build();
        
        assertTrue(options.isAutoSizeColumn());
        assertNotNull(options.getFreezePane());
        assertEquals(2, options.getFreezePane().colSplit());
        assertEquals(3, options.getFreezePane().rowSplit());
        assertFalse(options.isFreezeFirstRow());
        assertNotNull(options.getAutoFilter());
        assertFalse(options.isAutoFilterByFirstRow());
    }

    @Test
    public void testFreezePane() {
        FreezePane freezePane = new FreezePane(3, 2);
        
        assertEquals(3, freezePane.colSplit());
        assertEquals(2, freezePane.rowSplit());
    }

    @Test
    public void testCreateSheetSetter() {
        SheetCreateOptions options = SheetCreateOptions.builder()
            .autoSizeColumn(true)
            .freezeFirstRow(true)
            .autoFilterByFirstRow(true)
            .build();
        
        Consumer<Sheet> sheetSetter = ExcelUtil.createSheetSetter(options, 4);
        
        assertNotNull(sheetSetter);
        
        // Test with null options
        Consumer<Sheet> nullSetter = ExcelUtil.createSheetSetter(null, 4);
        assertNull(nullSetter);
    }

    @Test
    public void testSetCellValue() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            Row row = sheet.createRow(0);
            
            // Test String
            Cell cell1 = row.createCell(0);
            ExcelUtil.setCellValue(cell1, "test");
            assertEquals("test", cell1.getStringCellValue());
            
            // Test Boolean
            Cell cell2 = row.createCell(1);
            ExcelUtil.setCellValue(cell2, true);
            assertTrue(cell2.getBooleanCellValue());
            
            // Test Date
            Cell cell3 = row.createCell(2);
            java.util.Date date = new java.util.Date();
            ExcelUtil.setCellValue(cell3, date);
            assertEquals(date, cell3.getDateCellValue());
            
            // Test Calendar
            Cell cell4 = row.createCell(3);
            java.util.Calendar cal = java.util.Calendar.getInstance();
            ExcelUtil.setCellValue(cell4, cal);
            assertNotNull(cell4.getDateCellValue());
            
            // Test Integer
            Cell cell5 = row.createCell(4);
            ExcelUtil.setCellValue(cell5, 42);
            assertEquals(42.0, cell5.getNumericCellValue());
            
            // Test Double
            Cell cell6 = row.createCell(5);
            ExcelUtil.setCellValue(cell6, 3.14);
            assertEquals(3.14, cell6.getNumericCellValue());
            
            // Test null
            Cell cell7 = row.createCell(6);
            ExcelUtil.setCellValue(cell7, null);
            assertEquals("null", cell7.getStringCellValue());
            
            // Test other object
            Cell cell8 = row.createCell(7);
            ExcelUtil.setCellValue(cell8, new Object());
            assertTrue(cell8.getStringCellValue().startsWith("java.lang.Object@"));
        }
    }
}
