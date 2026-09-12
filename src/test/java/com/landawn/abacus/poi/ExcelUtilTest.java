package com.landawn.abacus.poi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.UncheckedException;
import com.landawn.abacus.poi.ExcelUtil.ExcelFormat;
import com.landawn.abacus.poi.ExcelUtil.FreezePane;
import com.landawn.abacus.poi.ExcelUtil.RowExtractors;
import com.landawn.abacus.poi.ExcelUtil.RowMappers;
import com.landawn.abacus.poi.ExcelUtil.SheetCreateOptions;
import com.landawn.abacus.util.CsvUtil;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.stream.Stream;

public class ExcelUtilTest extends TestBase {

    private static final File SAMPLE_XLSX = new File("./src/test/resources/test_excel_01.xlsx");
    private static final String NAME_31 = "abcdefghijklmnopqrstuvwxyz12345";
    private static final String NAME_32 = NAME_31 + "6";
    private static final List<String> HEADERS = Arrays.asList("h");
    private static final List<List<Object>> ROWS = Arrays.asList(Arrays.<Object> asList("v"));

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

    private File writeSheet(String sheetName, List<?> headers, List<? extends List<?>> rows) throws IOException {
        File file = createTempFile(".xlsx");
        ExcelUtil.writeRowsToSheet(sheetName, headers, rows, file);
        return file;
    }

    private byte[] writeXlsxToBytes(String sheetName, List<Object> headers, List<List<Object>> rows) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ExcelUtil.writeRowsToSheet(sheetName, headers, rows, sheet -> {
        }, baos, ExcelFormat.XLSX);
        return baos.toByteArray();
    }

    private static Dataset dataset() {
        return Dataset.rows(HEADERS, ROWS);
    }

    private static final class CloseTrackingInputStream extends ByteArrayInputStream {
        boolean closed = false;

        CloseTrackingInputStream(byte[] buf) {
            super(buf);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    private static final class CloseTrackingOutputStream extends ByteArrayOutputStream {
        boolean closed = false;
        boolean flushed = false;

        @Override
        public void flush() throws IOException {
            flushed = true;
            super.flush();
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }
    }

    @Test
    public void testReadDatasetFromSheet() {
        Dataset fromFile = ExcelUtil.readDatasetFromSheet(SAMPLE_XLSX);
        Dataset fromIndex = ExcelUtil.readDatasetFromSheet(SAMPLE_XLSX, 0, RowExtractors.DEFAULT);
        Dataset fromPath = ExcelUtil.readDatasetFromSheet(SAMPLE_XLSX.toPath(), 0, RowExtractors.DEFAULT);

        assertTrue(fromFile.columnCount() > 0);
        assertEquals(fromFile.columnCount(), fromIndex.columnCount());
        assertEquals(fromFile.columnCount(), fromPath.columnCount());
        assertEquals(fromFile.size(), fromPath.size());
    }

    @Test
    public void testReadDatasetFromSheet_ByName() throws Exception {
        File file = writeSheet("TestSheet", Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d")));

        Dataset fromFile = ExcelUtil.readDatasetFromSheet(file, "TestSheet", RowExtractors.DEFAULT);
        Dataset fromPath = ExcelUtil.readDatasetFromSheet(file.toPath(), "TestSheet", RowExtractors.DEFAULT);

        assertEquals(2, fromFile.columnCount());
        assertEquals(2, fromFile.size());
        assertEquals(fromFile.columnCount(), fromPath.columnCount());
        assertEquals(fromFile.size(), fromPath.size());
        assertThrows(IllegalArgumentException.class, () -> ExcelUtil.readDatasetFromSheet(file, "NonExistentSheet", RowExtractors.DEFAULT));
    }

    @Test
    public void testReadDatasetFromSheet_EmptySheet() throws Exception {
        File file = writeSheet("Empty", Arrays.asList("col1"), new ArrayList<>());
        Dataset dataset = ExcelUtil.readDatasetFromSheet(file);
        assertEquals(1, dataset.columnCount());
        assertEquals(0, dataset.size());
    }

    @Test
    public void testReadDatasetFromSheet_NonExistentFile() {
        File missing = new File("./nonexistent_file_12345.xlsx");
        assertThrows(UncheckedException.class, () -> ExcelUtil.readDatasetFromSheet(missing));
        assertThrows(UncheckedException.class, () -> ExcelUtil.readDatasetFromSheet(missing.toPath(), 0, RowExtractors.DEFAULT));
    }

    @Test
    public void testReadDatasetFromSheet_CustomRowExtractor() throws Exception {
        File file = writeSheet("Sheet1", Arrays.asList("A", "B"), Arrays.asList(Arrays.asList("x", "y")));
        TriConsumer<String[], Row, Object[]> extractor = (hdrs, row, output) -> {
            int idx = 0;
            for (Cell cell : row) {
                output[idx++] = "CUSTOM_" + ExcelUtil.CELL_TO_STRING.apply(cell);
            }
        };
        assertEquals("CUSTOM_x", ExcelUtil.readDatasetFromSheet(file, 0, extractor).get(0, 0));
    }

    @Test
    public void testReadDatasetFromSheet_BlankHeaderDoesNotCollideWithExplicitHeader() throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Sheet1");
            Row header = sheet.createRow(0);
            header.createCell(0).setBlank();
            header.createCell(1).setCellValue("Column_0");
            Row data = sheet.createRow(1);
            data.createCell(0).setCellValue("left");
            data.createCell(1).setCellValue("right");
            workbook.write(output);
        }

        Dataset dataset = ExcelUtil.readDatasetFromSheet(new ByteArrayInputStream(output.toByteArray()), 0, RowExtractors.DEFAULT);
        assertEquals(Arrays.asList("Column_0_1", "Column_0"), dataset.columnNames());
        assertEquals("left", dataset.get(0, 0));
        assertEquals("right", dataset.get(0, 1));
    }

    @Test
    public void testReadDatasetFromSheet_MissingCells() throws Exception {
        File file = writeSheet("Sheet1", Arrays.asList("A", "B"), Arrays.asList(Arrays.asList("left-only")));
        Dataset dataset = ExcelUtil.readDatasetFromSheet(file, 0, RowExtractors.DEFAULT);
        assertEquals(2, dataset.columnCount());
        assertEquals("left-only", dataset.get(0, 0));
        assertNull(dataset.get(0, 1));
    }

    @Test
    public void testReadDatasetFromSheet_HeaderWithBlankCell() throws Exception {
        File file = writeSheet("Sheet1", Arrays.asList("A", null, "C"), Arrays.asList(Arrays.asList("x", "y", "z")));
        Dataset dataset = ExcelUtil.readDatasetFromSheet(file);
        assertEquals(3, dataset.columnCount());
        assertEquals("A", dataset.getColumnName(0));
        assertEquals("C", dataset.getColumnName(2));
    }

    @Test
    public void testReadDatasetFromSheet_InputStream() throws IOException {
        byte[] bytes = writeXlsxToBytes("Named", Arrays.asList("col1", "col2"), Arrays.asList(Arrays.asList("a", "b"), Arrays.asList("c", "d")));

        try (InputStream is = new ByteArrayInputStream(bytes)) {
            Dataset dataset = ExcelUtil.readDatasetFromSheet(is, 0, RowExtractors.DEFAULT);
            assertEquals(2, dataset.columnCount());
            assertEquals(2, dataset.size());
            assertEquals("col1", dataset.getColumnName(0));
        }

        try (InputStream is = new ByteArrayInputStream(bytes)) {
            Dataset dataset = ExcelUtil.readDatasetFromSheet(is, "Named", RowExtractors.DEFAULT);
            assertEquals("col1", dataset.getColumnName(0));
        }

        assertThrows(IllegalArgumentException.class, () -> {
            try (InputStream is = new ByteArrayInputStream(bytes)) {
                ExcelUtil.readDatasetFromSheet(is, "Fake", RowExtractors.DEFAULT);
            }
        });
    }

    @Test
    public void testReadDatasetFromSheet_InputStream_NotClosed() throws IOException {
        byte[] bytes = writeXlsxToBytes("S", Arrays.asList("col"), Arrays.asList(Arrays.asList("value")));
        CloseTrackingInputStream is = new CloseTrackingInputStream(bytes);
        ExcelUtil.readDatasetFromSheet(is, 0, RowExtractors.DEFAULT);
        assertFalse(is.closed);
        is.close();
        assertTrue(is.closed);
    }

    @Test
    public void testReadDatasetFromSheet_FileAndInputStreamEquivalent() throws Exception {
        File file = writeSheet("S", Arrays.asList("a", "b"), Arrays.asList(Arrays.asList("x", "y")));
        Dataset fromFile = ExcelUtil.readDatasetFromSheet(file, 0, RowExtractors.DEFAULT);
        try (InputStream is = new FileInputStream(file)) {
            Dataset fromStream = ExcelUtil.readDatasetFromSheet(is, 0, RowExtractors.DEFAULT);
            assertEquals(fromFile.columnCount(), fromStream.columnCount());
            assertEquals(fromFile.size(), fromStream.size());
            assertEquals((Object) fromFile.get(0, 0), (Object) fromStream.get(0, 0));
        }
    }

    @Test
    public void testReadRowsFromSheet() throws Exception {
        File file = writeSheet("People", Arrays.asList("Name", "Age"), Arrays.asList(Arrays.asList("Alice", 30), Arrays.asList("Bob", 25)));

        List<List<Object>> allFromFile = ExcelUtil.readRowsFromSheet(file);
        List<List<Object>> skippedFromFile = ExcelUtil.readRowsFromSheet(file, 0, true, RowMappers.DEFAULT);
        List<List<Object>> skippedFromPath = ExcelUtil.readRowsFromSheet(file.toPath(), 0, true, RowMappers.DEFAULT);
        List<List<Object>> byNameFromFile = ExcelUtil.readRowsFromSheet(file, "People", true, RowMappers.DEFAULT);
        List<List<Object>> byNameFromPath = ExcelUtil.readRowsFromSheet(file.toPath(), "People", false, RowMappers.DEFAULT);

        assertEquals(3, allFromFile.size());
        assertEquals(2, skippedFromFile.size());
        assertEquals(skippedFromFile.size(), skippedFromPath.size());
        assertEquals(2, byNameFromFile.size());
        assertEquals(3, byNameFromPath.size());
        assertThrows(IllegalArgumentException.class, () -> ExcelUtil.readRowsFromSheet(file, "FakeSheet", true, RowMappers.DEFAULT));
    }

    @Test
    public void testReadRowsFromSheet_NonExistentFile() {
        File missing = new File("./nonexistent_file_12345.xlsx");
        assertThrows(UncheckedException.class, () -> ExcelUtil.readRowsFromSheet(missing));
    }

    @Test
    public void testReadRowsFromSheet_CustomRowMapper() throws Exception {
        File file = writeSheet("Sheet1", Arrays.asList("A", "B"), Arrays.asList(Arrays.asList(1, 2)));
        Function<Row, Integer> rowMapper = row -> {
            int sum = 0;
            for (Cell cell : row) {
                if (cell.getCellType() == CellType.NUMERIC) {
                    sum += (int) cell.getNumericCellValue();
                }
            }
            return sum;
        };
        List<Integer> sums = ExcelUtil.readRowsFromSheet(file, 0, true, rowMapper);
        assertEquals(1, sums.size());
        assertEquals(3, sums.get(0));
    }

    @Test
    public void testReadRowsFromSheet_InputStream() throws IOException {
        byte[] bytes = writeXlsxToBytes("Data", Arrays.asList("h"), Arrays.asList(Arrays.asList("a"), Arrays.asList("b")));
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            List<List<Object>> rows = ExcelUtil.readRowsFromSheet(is, 0, true, RowMappers.DEFAULT);
            assertEquals(2, rows.size());
            assertEquals("a", rows.get(0).get(0));
        }
        try (InputStream is = new ByteArrayInputStream(bytes)) {
            assertEquals(3, ExcelUtil.readRowsFromSheet(is, "Data", false, RowMappers.DEFAULT).size());
        }
    }

    @Test
    public void testStreamRowsFromSheet() throws Exception {
        File file = writeSheet("Items", Arrays.asList("Val"), Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList(3)));

        try (Stream<Row> stream = ExcelUtil.streamRowsFromSheet(file, 0, true)) {
            assertEquals(3, stream.count());
        }
        try (Stream<Row> stream = ExcelUtil.streamRowsFromSheet(file.toPath(), 0, true)) {
            assertEquals(3, stream.count());
        }
        try (Stream<Row> stream = ExcelUtil.streamRowsFromSheet(file, "Items", false)) {
            assertEquals(4, stream.count());
        }
        try (Stream<Row> stream = ExcelUtil.streamRowsFromSheet(file.toPath(), "Items", false)) {
            assertEquals(4, stream.count());
        }
        assertThrows(IllegalArgumentException.class, () -> ExcelUtil.streamRowsFromSheet(file, "FakeSheet", true));
    }

    @Test
    public void testStreamRowsFromSheet_NonExistentFile() {
        File missing = new File("./nonexistent_file_12345.xlsx");
        assertThrows(UncheckedException.class, () -> ExcelUtil.streamRowsFromSheet(missing, 0, false));
        assertThrows(UncheckedException.class, () -> ExcelUtil.streamRowsFromSheet(missing, "Sheet1", false));
    }

    @Test
    public void testStreamRowsFromSheet_FilterAndMap() throws Exception {
        File file = writeSheet("Numbers", Arrays.asList("Number"), Arrays.asList(Arrays.asList(10), Arrays.asList(20), Arrays.asList(5)));
        try (Stream<Row> stream = ExcelUtil.streamRowsFromSheet(file, 0, true)) {
            assertEquals(2, stream.filter(row -> row.getCell(0).getNumericCellValue() > 8).count());
        }
        try (Stream<Row> stream = ExcelUtil.streamRowsFromSheet(file, 0, true)) {
            List<Double> values = stream.map(row -> row.getCell(0).getNumericCellValue()).toList();
            assertEquals(Arrays.asList(10.0, 20.0, 5.0), values);
        }
    }

    @Test
    public void testStreamRowsFromSheet_InputStream() throws Exception {
        byte[] bytes = writeXlsxToBytes("Items", Arrays.asList("n"), Arrays.asList(Arrays.asList(1), Arrays.asList(2), Arrays.asList(3)));
        try (InputStream is = new ByteArrayInputStream(bytes);
             Stream<Row> stream = ExcelUtil.streamRowsFromSheet(is, 0, true)) {
            assertEquals(3, stream.count());
        }
        try (InputStream is = new ByteArrayInputStream(bytes);
             Stream<Row> stream = ExcelUtil.streamRowsFromSheet(is, "Items", true)) {
            assertEquals(3, stream.count());
        }

        CloseTrackingInputStream owned = new CloseTrackingInputStream(bytes);
        try (Stream<Row> stream = ExcelUtil.streamRowsFromSheet(owned, 0, false)) {
            assertEquals(4, stream.count());
        }
        assertFalse(owned.closed);
        owned.close();
        assertTrue(owned.closed);
    }

    @Test
    public void testCELL_GETTER() throws Exception {
        File file = writeSheet("Sheet1", Arrays.asList("S", "N", "B"), Arrays.asList(Arrays.asList("txt", 123.45, true)));
        List<List<Object>> rows = ExcelUtil.readRowsFromSheet(file, 0, true, RowMappers.DEFAULT);
        assertEquals("txt", rows.get(0).get(0));
        assertEquals(123.45, rows.get(0).get(1));
        assertEquals(true, rows.get(0).get(2));

        List<List<Object>> sample = ExcelUtil.readRowsFromSheet(SAMPLE_XLSX);
        assertFalse(sample.isEmpty());
    }

    @Test
    public void testCELL_TO_STRING() throws Exception {
        File file = writeSheet("Sheet1", Arrays.asList("S", "N", "B"), Arrays.asList(Arrays.asList("txt", 99.0, false)));
        List<List<String>> rows = ExcelUtil.readRowsFromSheet(file, 0, true, RowMappers.toList(ExcelUtil.CELL_TO_STRING));
        assertEquals(Arrays.asList("txt", "99.0", "false"), rows.get(0));
        assertTrue(ExcelUtil.readRowsFromSheet(file, 0, true, RowMappers.ROW2STRING).get(0).contains("txt"));
    }

    @Test
    public void testRowMappers() throws Exception {
        try (Workbook workbook = new XSSFWorkbook()) {
            Row sparse = workbook.createSheet("Sparse").createRow(0);
            sparse.createCell(0).setCellValue("first");
            sparse.createCell(2).setCellValue("third");
            assertEquals(Arrays.asList("first", null, "third"), RowMappers.DEFAULT.apply(sparse));
            assertEquals("first||third", RowMappers.toDelimitedString("|").apply(sparse));
        }

        File file = writeSheet("Sheet1", Arrays.asList("A", "B"), Arrays.asList(Arrays.asList("one", "two")));
        assertEquals("one|two", ExcelUtil.readRowsFromSheet(file, 0, true, RowMappers.toDelimitedString("|")).get(0));
        assertEquals("VAL,VAL", ExcelUtil.readRowsFromSheet(file, 0, true, RowMappers.toDelimitedString(",", cell -> "VAL")).get(0));
        assertEquals("onetwo", ExcelUtil.readRowsFromSheet(file, 0, true, RowMappers.toDelimitedString("")).get(0));
        assertEquals(Arrays.asList("one", "two"), ExcelUtil.readRowsFromSheet(file, 0, true, RowMappers.toList(ExcelUtil.CELL_TO_STRING)).get(0));
    }

    @Test
    public void testRowExtractors() throws Exception {
        File file = writeSheet("Sheet1", Arrays.asList("A", "B"), Arrays.asList(Arrays.asList("text", null)));
        assertEquals(1, ExcelUtil.readDatasetFromSheet(file, 0, RowExtractors.DEFAULT).size());
        Dataset custom = ExcelUtil.readDatasetFromSheet(file, 0, RowExtractors.create(cell -> "PREFIX_" + ExcelUtil.CELL_TO_STRING.apply(cell)));
        assertEquals("PREFIX_text", custom.get(0, 0));
    }

    @Test
    public void testWriteRowsToSheet() throws Exception {
        File file = createTempFile(".xlsx");
        File pathFile = createTempFile(".xlsx");
        List<Object> headers = Arrays.asList("Name", "Value");
        List<List<Object>> rows = Arrays.asList(Arrays.asList("Item1", 100), Arrays.asList("Item2", 200));

        ExcelUtil.writeRowsToSheet("Data", headers, rows, file);
        ExcelUtil.writeRowsToSheet("Data", headers, rows, sheet -> {
        }, pathFile.toPath());

        assertEquals(3, ExcelUtil.readRowsFromSheet(file, 0, false, RowMappers.DEFAULT).size());
        assertEquals(3, ExcelUtil.readRowsFromSheet(pathFile, 0, false, RowMappers.DEFAULT).size());
    }

    @Test
    public void testWriteRowsToSheet_EmptyRows() throws Exception {
        File file = writeSheet("EmptyData", Arrays.asList("Header"), new ArrayList<>());
        assertEquals(1, ExcelUtil.readRowsFromSheet(file).size());
    }

    @Test
    public void testWriteRowsToSheet_XlsFormat() throws Exception {
        File file = createTempFile(".xls");
        ExcelUtil.writeRowsToSheet("XlsSheet", Arrays.asList("Col1", "Col2"), Arrays.asList(Arrays.asList("A", "B")), file);
        Dataset loaded = ExcelUtil.readDatasetFromSheet(file);
        assertEquals(2, loaded.columnCount());
        assertEquals(1, loaded.size());
    }

    @Test
    public void testWriteRowsToSheet_SheetCreateOptions() throws Exception {
        File file = createTempFile(".xlsx");
        SheetCreateOptions options = SheetCreateOptions.builder()
                .autoSizeColumn(true)
                .freezePane(new FreezePane(1, 1))
                .freezeFirstRow(true)
                .autoFilter(new CellRangeAddress(0, 0, 0, 1))
                .autoFilterByFirstRow(true)
                .build();
        ExcelUtil.writeRowsToSheet("Formatted", Arrays.asList("Col1", "Col2"), Arrays.asList(Arrays.asList("A", "B")), options, file);
        assertEquals(2, ExcelUtil.readRowsFromSheet(file).size());

        ExcelUtil.writeRowsToSheet("Test", Arrays.asList("H"), Arrays.asList(Arrays.asList("V")), (SheetCreateOptions) null, createTempFile(".xlsx"));
        assertDoesNotThrow(() -> ExcelUtil.writeRowsToSheet("Empty", new ArrayList<>(), new ArrayList<>(),
                SheetCreateOptions.builder().autoFilterByFirstRow(true).build(), createTempFile(".xlsx")));
    }

    @Test
    public void testWriteRowsToSheet_SheetSetter() throws Exception {
        File file = createTempFile(".xlsx");
        ExcelUtil.writeRowsToSheet("Custom", Arrays.asList("X"), Arrays.asList(Arrays.asList("Y")), sheet -> sheet.setDefaultColumnWidth(20), file);
        assertEquals(2, ExcelUtil.readRowsFromSheet(file).size());
        assertThrows(IllegalArgumentException.class,
                () -> ExcelUtil.writeRowsToSheet("Test", Arrays.asList("H"), Arrays.asList(Arrays.asList("V")), (Consumer<Sheet>) null, file));
    }

    @Test
    public void testWriteRowsToSheet_CellTypes() throws Exception {
        File file = createTempFile(".xlsx");
        Calendar cal = Calendar.getInstance();
        Object custom = new Object() {
            @Override
            public String toString() {
                return "CustomToString";
            }
        };
        List<Object> headers = Arrays.asList("S", "I", "D", "B", "Date", "LD", "LDT", "Cal", "Long", "Null", "Obj", "Special");
        List<List<Object>> rows = Arrays.asList(
                Arrays.asList("text", 42, 3.14, true, new Date(), LocalDate.of(2025, 1, 15), LocalDateTime.of(2025, 6, 15, 10, 30), cal, 123456789L, null,
                        custom, "Hello, \"World\""),
                Arrays.asList("Line1\nLine2", null, null, false, null, null, null, null, null, "AlsoNotNull", new StringBuilder("builder-value"), "Tab\there"));

        ExcelUtil.writeRowsToSheet("Types", headers, rows, file);
        List<List<Object>> read = ExcelUtil.readRowsFromSheet(file, 0, true, RowMappers.DEFAULT);
        assertEquals(2, read.size());
        assertEquals("text", read.get(0).get(0));
        assertEquals(1.23456789E8, read.get(0).get(8));
        assertTrue(read.get(0).get(10).toString().contains("CustomToString"));
        assertEquals("builder-value", read.get(1).get(10));
        assertNotNull(read.get(0).get(5));
        assertNotNull(read.get(0).get(6));
        assertNotNull(read.get(0).get(7));
    }

    @Test
    public void testWriteDatasetToSheet() throws Exception {
        File file = createTempFile(".xlsx");
        File pathFile = createTempFile(".xlsx");
        Dataset dataset = N.newDataset(N.toList("column1", "column2"), N.toList(N.toList("ab", "cd"), N.toList("ef", "gh")));

        ExcelUtil.writeDatasetToSheet("DatasetSheet", dataset, file);
        ExcelUtil.writeDatasetToSheet("DatasetSheet", dataset, sheet -> {
        }, pathFile.toPath());

        Dataset fromFile = ExcelUtil.readDatasetFromSheet(file);
        Dataset fromPath = ExcelUtil.readDatasetFromSheet(pathFile);
        assertEquals(dataset.columnCount(), fromFile.columnCount());
        assertEquals(dataset.size(), fromFile.size());
        assertEquals(fromFile.columnCount(), fromPath.columnCount());
        assertEquals(fromFile.size(), fromPath.size());

        ExcelUtil.writeDatasetToSheet("DS", N.newDataset(N.toList("A"), N.toList(N.toList(1))), (SheetCreateOptions) null, createTempFile(".xlsx"));
        assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeDatasetToSheet("DS2", dataset, (Consumer<Sheet>) null, createTempFile(".xlsx")));
    }

    @Test
    public void testWriteDatasetToSheet_XlsFormat() throws Exception {
        File file = createTempFile(".xls");
        ExcelUtil.writeDatasetToSheet("XlsDS", N.newDataset(N.toList("A"), N.toList(N.toList("val"))), file);
        Dataset loaded = ExcelUtil.readDatasetFromSheet(file);
        assertEquals(1, loaded.columnCount());
        assertEquals(1, loaded.size());
    }

    @Test
    public void testWriteToSheet_OutputStream() throws Exception {
        ByteArrayOutputStream xlsx = new ByteArrayOutputStream();
        ExcelUtil.writeRowsToSheet("S", Arrays.asList("a", "b"), Arrays.asList(Arrays.asList("1", "2")), sheet -> {
        }, xlsx, ExcelFormat.XLSX);
        try (InputStream is = new ByteArrayInputStream(xlsx.toByteArray())) {
            Dataset loaded = ExcelUtil.readDatasetFromSheet(is, 0, RowExtractors.DEFAULT);
            assertEquals(2, loaded.columnCount());
            assertEquals(1, loaded.size());
        }

        ByteArrayOutputStream xls = new ByteArrayOutputStream();
        ExcelUtil.writeRowsToSheet("S", Arrays.asList("a"), Arrays.asList(Arrays.asList("v")), sheet -> {
        }, xls, ExcelFormat.XLS);
        byte[] bytes = xls.toByteArray();
        assertEquals((byte) 0xD0, bytes[0]);
        assertEquals((byte) 0xCF, bytes[1]);

        CloseTrackingOutputStream os = new CloseTrackingOutputStream();
        ExcelUtil.writeDatasetToSheet("DS", N.newDataset(N.toList("A"), N.toList(N.toList("v"))), sheet -> {
        }, os, ExcelFormat.XLSX);
        assertTrue(os.flushed);
        assertFalse(os.closed);
    }

    @Test
    public void testWriteToSheet_OutputStream_RejectsNullFormat() {
        Dataset dataset = N.newDataset(N.toList("A"), N.toList(N.toList(1)));
        assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeRowsToSheet("Rows", Arrays.asList("A"), Arrays.asList(Arrays.asList(1)), sheet -> {
        }, new ByteArrayOutputStream(), null));
        assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeDatasetToSheet("Dataset", dataset, sheet -> {
        }, new ByteArrayOutputStream(), null));
    }

    @Test
    public void testFormatOf_extensionInference() {
        assertEquals(ExcelFormat.XLS, ExcelUtil.formatOf(new File("a.xls")));
        assertEquals(ExcelFormat.XLS, ExcelUtil.formatOf(new File("a.XLS")));
        assertEquals(ExcelFormat.XLSX, ExcelUtil.formatOf(new File("a.xlsx")));
        assertEquals(ExcelFormat.XLSX, ExcelUtil.formatOf(new File("a.dat")));
        assertEquals(ExcelFormat.XLSX, ExcelUtil.formatOf(new File("noext")));
    }

    @Test
    public void testRoundTrip() throws Exception {
        Dataset original = N.newDataset(N.toList("name", "value", "flag"),
                N.toList(N.toList("A", 10, true), N.toList("B", 20, false), N.toList("C", 30, true)));
        File xlsx = createTempFile(".xlsx");
        ExcelUtil.writeDatasetToSheet("Data", original, xlsx);
        Dataset loaded = ExcelUtil.readDatasetFromSheet(xlsx);
        assertEquals(original.columnCount(), loaded.columnCount());
        assertEquals(original.size(), loaded.size());
        assertEquals(original.getColumnName(0), loaded.getColumnName(0));

        File csv = createTempFile(".csv");
        ExcelUtil.exportSheetToCsv(xlsx, 0, csv);
        Dataset fromCsv = CsvUtil.load(csv);
        assertEquals(3, fromCsv.columnCount());
        assertEquals(3, fromCsv.size());

        File xls = createTempFile(".xls");
        ExcelUtil.writeRowsToSheet("Scores", Arrays.asList("Name", "Score"), Arrays.asList(Arrays.asList("Alice", 95), Arrays.asList("Bob", 87)), xls);
        assertEquals(2, ExcelUtil.readDatasetFromSheet(xls).size());
    }

    @Test
    public void testMultipleSheets_ByName() throws Exception {
        File file = writeSheet("FirstSheet", Arrays.asList("Sheet1Col"), Arrays.asList(Arrays.asList("S1Data")));
        Dataset ds = ExcelUtil.readDatasetFromSheet(file, "FirstSheet", RowExtractors.DEFAULT);
        assertEquals(1, ds.columnCount());
        assertEquals("Sheet1Col", ds.getColumnName(0));
    }

    @Test
    public void testLargeDataset() throws Exception {
        List<List<Object>> rows = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            rows.add(Arrays.asList(i, "Name" + i, i * 10));
        }
        File file = writeSheet("Large", Arrays.asList("ID", "Name", "Value"), rows);
        assertEquals(100, ExcelUtil.readRowsFromSheet(file, 0, true, RowMappers.DEFAULT).size());
        try (Stream<Row> stream = ExcelUtil.streamRowsFromSheet(file, 0, true)) {
            assertEquals(100, stream.count());
        }
    }

    @Test
    public void testExportSheetToCsv() throws Exception {
        File excel = writeSheet("People", Arrays.asList("Name", "Age"), Arrays.asList(Arrays.asList("John", 30), Arrays.asList("Jane", 25)));
        File byIndex = createTempFile(".csv");
        File byName = createTempFile(".csv");

        ExcelUtil.exportSheetToCsv(excel, 0, byIndex);
        ExcelUtil.exportSheetToCsv(excel, "People", byName);

        String indexContent = IOUtil.readAllToString(byIndex);
        String nameContent = IOUtil.readAllToString(byName);
        assertTrue(indexContent.contains("Name"));
        assertTrue(indexContent.contains("John"));
        assertTrue(nameContent.contains("People") || nameContent.contains("Name"));
        assertTrue(nameContent.contains("John"));
        assertThrows(IllegalArgumentException.class, () -> ExcelUtil.exportSheetToCsv(excel, "FakeSheet", createTempFile(".csv")));
        assertThrows(UncheckedException.class, () -> ExcelUtil.exportSheetToCsv(new File("./nonexistent_file_12345.xlsx"), 0, createTempFile(".csv")));
    }

    @Test
    public void testExportSheetToCsv_Writer() throws Exception {
        File excel = writeSheet("Data", Arrays.asList("A", "B"),
                Arrays.asList(Arrays.asList("hello", 42.5), Arrays.asList(true, false), Arrays.asList("G", "H")));

        StringWriter custom = new StringWriter();
        ExcelUtil.exportSheetToCsv(excel, 0, Arrays.asList("Column1", "Column2"), custom);
        assertTrue(custom.toString().contains("Column1"));

        StringWriter named = new StringWriter();
        ExcelUtil.exportSheetToCsv(excel, "Data", Arrays.asList("Custom"), named);
        assertTrue(named.toString().contains("Custom"));

        StringWriter original = new StringWriter();
        ExcelUtil.exportSheetToCsv(excel, 0, null, original);
        String content = original.toString();
        assertTrue(content.contains("A"));
        assertTrue(content.contains("hello"));
        assertTrue(content.contains("42.5"));
        assertTrue(content.contains("true"));
        assertTrue(content.contains("G"));

        StringWriter empty = new StringWriter();
        ExcelUtil.exportSheetToCsv(writeSheet("Sheet1", Arrays.asList("H"), new ArrayList<>()), 0, null, empty);
        assertTrue(empty.toString().contains("H"));

        assertThrows(IllegalArgumentException.class, () -> ExcelUtil.exportSheetToCsv(excel, "FakeSheet", null, new StringWriter()));
    }

    @Test
    public void testSheetCreateOptions() {
        SheetCreateOptions defaults = new SheetCreateOptions();
        assertFalse(defaults.isAutoSizeColumn());
        assertFalse(defaults.isFreezeFirstRow());
        assertFalse(defaults.isAutoFilterByFirstRow());
        assertNull(defaults.getFreezePane());
        assertNull(defaults.getAutoFilter());
    }

    @Test
    public void testFreezePane() throws Exception {
        FreezePane fp1 = new FreezePane(1, 2);
        FreezePane fp2 = new FreezePane(1, 2);
        FreezePane zero = new FreezePane(0, 0);
        FreezePane colOnly = new FreezePane(2, 0);
        FreezePane rowOnly = new FreezePane(0, 2);

        assertEquals(fp1, fp2);
        assertEquals(fp1.hashCode(), fp2.hashCode());
        assertNotEquals(fp1, new FreezePane(2, 1));
        assertTrue(fp1.toString().contains("1"));
        assertTrue(fp1.toString().contains("2"));
        assertEquals(0, zero.colSplit());
        assertEquals(2, colOnly.colSplit());
        assertEquals(2, rowOnly.rowSplit());
        assertThrows(IllegalArgumentException.class, () -> new FreezePane(-1, 0));
        assertThrows(IllegalArgumentException.class, () -> new FreezePane(0, -1));

        File file = createTempFile(".xlsx");
        ExcelUtil.writeRowsToSheet("Frozen", Arrays.asList("A", "B"), Arrays.asList(Arrays.asList(1, 2)),
                SheetCreateOptions.builder().freezePane(fp1).freezeFirstRow(true).build(), file);
        assertEquals(2, ExcelUtil.readRowsFromSheet(file).size());
    }

    @Test
    public void testWriteToSheet_31CharName_roundTripsByName_xlsAndXlsx() throws Exception {
        for (String suffix : new String[] { ".xlsx", ".xls" }) {
            File rowsFile = createTempFile(suffix);
            ExcelUtil.writeRowsToSheet(NAME_31, HEADERS, ROWS, rowsFile);
            List<List<Object>> rows = ExcelUtil.readRowsFromSheet(rowsFile, NAME_31, true, RowMappers.DEFAULT);
            assertEquals(1, rows.size(), suffix);
            assertEquals("v", rows.get(0).get(0), suffix);

            File datasetFile = createTempFile(suffix);
            ExcelUtil.writeDatasetToSheet(NAME_31, dataset(), datasetFile);
            assertEquals("v", ExcelUtil.readDatasetFromSheet(datasetFile, NAME_31, RowExtractors.DEFAULT).get(0, 0), suffix);
        }
    }

    @Test
    public void testWriteToSheet_unicode31CodeUnitName_roundTripsByName_xlsAndXlsx() throws Exception {
        String name = "λ".repeat(29) + "😀";
        assertEquals(31, name.length());
        for (String suffix : new String[] { ".xlsx", ".xls" }) {
            File file = createTempFile(suffix);
            ExcelUtil.writeRowsToSheet(name, HEADERS, ROWS, file);
            assertEquals("v", ExcelUtil.readRowsFromSheet(file, name, true, RowMappers.DEFAULT).get(0).get(0), suffix);
        }
        assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeRowsToSheet(name + "x", HEADERS, ROWS, createTempFile(".xlsx")));
    }

    @Test
    public void testWriteToSheet_32CharName_IAE_everyOverload_noTempFileLeft() throws Exception {
        Path dir = Files.createTempDirectory("excel_sheetname_");
        try {
            File out = dir.resolve("out.xlsx").toFile();
            Path outPath = dir.resolve("out2.xls");
            Consumer<Sheet> setter = sheet -> {
            };

            assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeRowsToSheet(NAME_32, HEADERS, ROWS, out));
            assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeRowsToSheet(NAME_32, HEADERS, ROWS, (SheetCreateOptions) null, out));
            assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeRowsToSheet(NAME_32, HEADERS, ROWS, setter, out));
            assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeRowsToSheet(NAME_32, HEADERS, ROWS, setter, outPath));
            assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeDatasetToSheet(NAME_32, dataset(), out));
            assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeDatasetToSheet(NAME_32, dataset(), (SheetCreateOptions) null, out));
            assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeDatasetToSheet(NAME_32, dataset(), setter, out));
            assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeDatasetToSheet(NAME_32, dataset(), setter, outPath));

            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeRowsToSheet(NAME_32, HEADERS, ROWS, setter, bos, ExcelFormat.XLSX));
            assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeRowsToSheet(NAME_32, HEADERS, ROWS, setter, bos, ExcelFormat.XLS));
            assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeDatasetToSheet(NAME_32, dataset(), setter, bos, ExcelFormat.XLSX));
            assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeDatasetToSheet(NAME_32, dataset(), setter, bos, ExcelFormat.XLS));
            assertEquals(0, bos.size());
            assertFalse(out.exists());
            assertFalse(Files.exists(outPath));
            assertArrayEquals(new String[0], dir.toFile().list());
        } finally {
            IOUtil.deleteRecursivelyIfExists(dir.toFile());
        }
    }

    @Test
    public void testWriteToSheet_invalidNames_IAE_noTempFileLeft() throws Exception {
        Path dir = Files.createTempDirectory("excel_sheetname_");
        try {
            File out = dir.resolve("out.xlsx").toFile();
            Consumer<Sheet> setter = sheet -> {
            };
            for (String bad : new String[] { null, "", "a/b", "a\\b", "a?b", "a*b", "a[b]", "a]b", "a:b", "'quoted", "quoted'" }) {
                assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeRowsToSheet(bad, HEADERS, ROWS, out), String.valueOf(bad));
                assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeDatasetToSheet(bad, dataset(), out), String.valueOf(bad));
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeRowsToSheet(bad, HEADERS, ROWS, setter, bos, ExcelFormat.XLSX),
                        String.valueOf(bad));
                assertThrows(IllegalArgumentException.class, () -> ExcelUtil.writeDatasetToSheet(bad, dataset(), setter, bos, ExcelFormat.XLS),
                        String.valueOf(bad));
                assertEquals(0, bos.size(), String.valueOf(bad));
            }
            assertArrayEquals(new String[0], dir.toFile().list());
            ExcelUtil.writeRowsToSheet("It's Q1 Data", HEADERS, ROWS, out);
            assertEquals("v", ExcelUtil.readRowsFromSheet(out, "IT'S q1 data", true, RowMappers.DEFAULT).get(0).get(0));
        } finally {
            IOUtil.deleteRecursivelyIfExists(dir.toFile());
        }
    }

    @Test
    public void testReadDatasetFromSheet_whitespaceOnlyDuplicateHeaders_IAE() throws Exception {
        for (boolean xls : new boolean[] { false, true }) {
            File file = createTempFile(xls ? ".xls" : ".xlsx");
            try (Workbook wb = xls ? new HSSFWorkbook() : new XSSFWorkbook()) {
                Row header = wb.createSheet("S").createRow(0);
                header.createCell(0).setCellValue(" ");
                header.createCell(1).setCellValue(" ");
                try (OutputStream os = new FileOutputStream(file)) {
                    wb.write(os);
                }
            }
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> ExcelUtil.readDatasetFromSheet(file));
            assertTrue(e.getMessage().contains("Duplicate header name ' ' in columns 0 and 1"), e.getMessage());
        }
    }

    @Test
    public void testReadDatasetFromSheet_nonStringHeadersUseCellToStringRendering() throws Exception {
        for (boolean xls : new boolean[] { false, true }) {
            File file = createTempFile(xls ? ".xls" : ".xlsx");
            try (Workbook wb = xls ? new HSSFWorkbook() : new XSSFWorkbook()) {
                Sheet sheet = wb.createSheet("S");
                Row header = sheet.createRow(0);
                header.createCell(0).setCellValue(2024);
                header.createCell(1).setCellValue(true);
                header.createCell(2).setCellFormula("A2&B2");
                header.createCell(3).setCellValue(" ");
                header.createCell(4).setCellValue("");
                header.createCell(5).setCellValue("λ😀");
                Row data = sheet.createRow(1);
                for (int i = 0; i < 7; i++) {
                    data.createCell(i).setCellValue("d" + i);
                }
                try (OutputStream os = new FileOutputStream(file)) {
                    wb.write(os);
                }
            }
            Dataset ds = ExcelUtil.readDatasetFromSheet(file);
            assertEquals(Arrays.asList("2024.0", "true", "A2&B2", " ", "Column_4", "λ😀", "Column_6"), ds.columnNames(), xls ? "xls" : "xlsx");
            assertEquals("d3", ds.get(0, 3));
        }
    }
}
