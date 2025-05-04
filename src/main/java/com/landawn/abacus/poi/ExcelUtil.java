/*
 * Copyright (c) 2015, Haiyang Li.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.landawn.abacus.poi;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.landawn.abacus.exception.UncheckedException;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.DataSet;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.RowDataSet;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.stream.Stream;

import lombok.Builder;
import lombok.Data;

public final class ExcelUtil {

    public static final Function<Cell, String> CELL2STRING = cell -> switch (cell.getCellType()) {
        case STRING -> cell.getStringCellValue();
        case NUMERIC -> String.valueOf(cell.getNumericCellValue());
        case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
        case FORMULA -> cell.getCellFormula();
        case BLANK -> "";
        default -> throw new RuntimeException("Unsupported cell type: " + cell.getCellType());
    };

    private ExcelUtil() {
        // prevent instantiation
    }

    /**
     * Read the first sheet of the Excel file and return the rows as a list of strings.
     *
     * @param excelFile the Excel file
     * @return a list of strings representing the rows in the first sheet
     */
    public static List<String> readSheet(final File excelFile) {
        return readSheet(excelFile, 0, RowMappers.ROW2STRING);
    }

    /**
     * Reads the specified sheet from the given Excel file and maps each row to an object of type T using the provided row mapper.
     *
     * @param <T> the type of the objects to be returned
     * @param excelFile the Excel file to read
     * @param sheetIndex the index of the sheet to read, starting from 0.
     * @param rowMapper a function to map each row to an object of type T
     * @return a list of objects of type T representing the rows in the specified sheet
     * @throws UncheckedException if an I/O error occurs while reading the file
     */
    public static <T> List<T> readSheet(final File excelFile, final int sheetIndex, final Function<? super Row, ? extends T> rowMapper) {
        try (InputStream is = new FileInputStream(excelFile); //
                Workbook workbook = new XSSFWorkbook(is)) {
            return readSheet(workbook.getSheetAt(sheetIndex), rowMapper);

        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Reads the specified sheet from the given Excel file and maps each row to an object of type T using the provided row mapper.
     *
     * @param <T> the type of the objects to be returned
     * @param excelFile the Excel file to read
     * @param sheetName the name of the sheet to read
     * @param rowMapper a function to map each row to an object of type T
     * @return a list of objects of type T representing the rows in the specified sheet
     * @throws UncheckedException if an I/O error occurs while reading the file
     */
    public static <T> List<T> readSheet(final File excelFile, final String sheetName, final Function<? super Row, ? extends T> rowMapper) {
        try (InputStream is = new FileInputStream(excelFile); //
                Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheet(sheetName);
            return readSheet(sheet, rowMapper);

        } catch (IOException e) {
            throw new UncheckedException(e);
        }

    }

    private static <T> List<T> readSheet(final Sheet sheet, final Function<? super Row, ? extends T> rowMapper) {
        final List<T> rowList = new ArrayList<>();

        for (Row row : sheet) {
            rowList.add(rowMapper.apply(row));
        }

        return rowList;
    }

    /**
     * Reads the specified sheet from the given Excel file and converts each row in the specified sheet to a row in the returned DataSet by using the provided row extractor.
     * The first row of the sheet is used as column names for the returned DataSet.
     *
     * @param excelFile the Excel file
     * @return a list of strings representing the rows in the first sheet
     */
    public static DataSet loadSheet(final File excelFile) {
        return loadSheet(excelFile, 0, RowExtractors.ROW2STRING_ARRAY);
    }

    /**
     * Reads the specified sheet from the given Excel file and converts each row in the specified sheet to a row in the returned DataSet by using the provided row extractor.
     * The first row of the sheet is used as column names for the returned DataSet.
     *
     * @param excelFile the Excel file to read
     * @param sheetIndex the index of the sheet to read, starting from 0.
     * @param rowExtractor converts each row in the specified sheet to a row in the returned DataSet. 
     *        The first parameter is the column names, the second parameter is the row, and the third parameter is an array to store the extracted values.
     * @return a list of objects of type T representing the rows in the specified sheet
     * @throws UncheckedException if an I/O error occurs while reading the file
     */
    public static DataSet loadSheet(final File excelFile, final int sheetIndex,
            final TriConsumer<? super String[], ? super Row, ? super Object[]> rowExtractor) {
        try (InputStream is = new FileInputStream(excelFile); //
                Workbook workbook = new XSSFWorkbook(is)) {
            return loadSheet(workbook.getSheetAt(sheetIndex), rowExtractor);

        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Reads the specified sheet from the given Excel file and converts each row in the specified sheet to a row in the returned DataSet by using the provided row extractor.
     * The first row of the sheet is used as column names for the returned DataSet.
     *
     * @param excelFile the Excel file to read
     * @param sheetName the name of the sheet to read
     * @param rowExtractor converts each row in the specified sheet to a row in the returned DataSet. 
     *        The first parameter is the column names, the second parameter is the row, and the third parameter is an array to store the extracted values.
     * @return a list of objects of type T representing the rows in the specified sheet
     * @throws UncheckedException if an I/O error occurs while reading the file
     */
    public static DataSet loadSheet(final File excelFile, final String sheetName,
            final TriConsumer<? super String[], ? super Row, ? super Object[]> rowExtractor) {
        try (InputStream is = new FileInputStream(excelFile); //
                Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheet(sheetName);
            return loadSheet(sheet, rowExtractor);

        } catch (IOException e) {
            throw new UncheckedException(e);
        }

    }

    private static DataSet loadSheet(final Sheet sheet, final TriConsumer<? super String[], ? super Row, ? super Object[]> rowExtractor) {
        final Iterator<Row> rowIter = sheet.rowIterator();

        if (!rowIter.hasNext()) {
            return DataSet.empty();
        }

        final Row headerRow = rowIter.next();
        final int columnCount = headerRow.getPhysicalNumberOfCells();
        final String[] headers = new String[columnCount];

        for (int i = 0; i < columnCount; i++) {
            headers[i] = CELL2STRING.apply(headerRow.getCell(i));
        }

        final List<List<Object>> columnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            columnList.add(new ArrayList<>());
        }

        final Object[] output = new Object[columnCount];

        while (rowIter.hasNext()) {
            rowExtractor.accept(headers, rowIter.next(), output);

            for (int i = 0; i < columnCount; i++) {
                columnList.get(i).add(output[i]);
            }
        }

        final List<String> columnNameList = new ArrayList<>(List.of(headers));

        return new RowDataSet(columnNameList, columnList);
    }

    /**
     * Returns a stream of rows from the specified sheet in the given Excel file.
     *
     * @param excelFile the Excel file to read
     * @param sheetIndex the index of the sheet to read
     * @return a stream of rows from the specified sheet
     * @throws UncheckedException if an I/O error occurs while reading the file
     */
    public static Stream<Row> streamSheet(final File excelFile, final int sheetIndex) {
        try (InputStream is = new FileInputStream(excelFile); //
                Workbook workbook = new XSSFWorkbook(is)) {
            final Sheet sheet = workbook.getSheetAt(sheetIndex);

            return Stream.of(sheet.rowIterator());
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Returns a stream of rows from the specified sheet in the given Excel file.
     *
     * @param excelFile the Excel file to read
     * @param sheetName the name of the sheet to read
     * @return a stream of rows from the specified sheet
     * @throws UncheckedException if an I/O error occurs while reading the file
     */
    public static Stream<Row> streamSheet(final File excelFile, final String sheetName) {
        try (InputStream is = new FileInputStream(excelFile); //
                Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheet(sheetName);

            return Stream.of(sheet.rowIterator());
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Writes the specified data to an Excel file with the given sheet name and headers.
     * @param sheetName the name of the sheet to create
     * @param headers the headers for the columns
     * @param rows the data to write to the sheet
     * @param outputExcelFile the Excel file to write to
     */
    public static void writeSheet(final String sheetName, final List<String> headers, final List<? extends Collection<?>> rows, final File outputExcelFile) {
        writeSheet(sheetName, headers, rows, (SheetCreateOptions) null, outputExcelFile);
    }

    /**
     * Writes the specified data to an Excel file with the given sheet name, headers, and options.
     * @param sheetName the name of the sheet to create
     * @param headers the headers for the columns
     * @param rows the data to write to the sheet
     * @param sheetCreateOptions options for creating the sheet
     * @param sheetCreateOptions
     * @param outputExcelFile the Excel file to write to
     */
    public static void writeSheet(final String sheetName, final List<String> headers, final List<? extends Collection<?>> rows,
            final SheetCreateOptions sheetCreateOptions, final File outputExcelFile) {
        final int columnColumn = headers.size();

        final Consumer<Sheet> sheetSetter = createSheetSetter(sheetCreateOptions, columnColumn);

        writeSheet(sheetName, headers, rows, sheetSetter, outputExcelFile);
    }

    static Consumer<Sheet> createSheetSetter(final SheetCreateOptions sheetCreateOptions, final int columnColumn) {
        return sheetCreateOptions == null ? null : sheet -> {
            if (sheetCreateOptions.isAutoSizeColumn()) {
                // Resize columns to fit content
                for (int i = 0; i < columnColumn; i++) {
                    sheet.autoSizeColumn(i);
                }
            }

            final FreezePane freezePane = sheetCreateOptions.getFreezePane();

            if (freezePane != null) {
                sheet.createFreezePane(freezePane.colSplit(), freezePane.rowSplit());
            } else if (sheetCreateOptions.isFreezeFirstRow()) {
                sheet.createFreezePane(0, 1);
            }

            if (sheetCreateOptions.getAutoFilter() != null) {
                sheet.setAutoFilter(sheetCreateOptions.getAutoFilter());
            } else if (sheetCreateOptions.isAutoFilterByFirstRow()) {
                sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, columnColumn));
            }
        };
    }

    /**
     * Writes the specified data to an Excel file with the given sheet name, headers, and options.
     * @param sheetName the name of the sheet to create
     * @param headers the headers for the columns
     * @param rows the data to write to the sheet
     * @param sheetSetter a consumer to set additional properties on the sheet
     * @param outputExcelFile the Excel file to write to
     */
    public static void writeSheet(final String sheetName, final List<String> headers, final List<? extends Collection<?>> rows,
            final Consumer<? super Sheet> sheetSetter, final File outputExcelFile) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);

        final int columnColumn = headers.size();

        final Row headerRow = sheet.createRow(0);

        for (int i = 0; i < columnColumn; i++) {
            headerRow.createCell(i).setCellValue(headers.get(i));
        }

        int rowNum = 1;

        for (Collection<?> rowData : rows) {
            final Row row = sheet.createRow(rowNum++);
            final Iterator<?> iter = rowData.iterator();

            for (int i = 0; i < rowData.size(); i++) {
                setCellValue(row.createCell(i), iter.next());
            }
        }

        if (sheetSetter != null) {
            sheetSetter.accept(sheet);
        }

        // Write to file
        try (OutputStream os = new FileOutputStream(outputExcelFile)) {
            workbook.write(os);
            workbook.close();
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Writes the specified data to an Excel file with the given sheet name and headers.
     * @param sheetName the name of the sheet to create 
     * @param dataset the data to write to the sheet
     * @param outputExcelFile the Excel file to write to
     */
    public static void writeSheet(final String sheetName, final DataSet dataset, final File outputExcelFile) {
        writeSheet(sheetName, dataset, (SheetCreateOptions) null, outputExcelFile);
    }

    /**
     * Writes the specified data to an Excel file with the given sheet name, headers, and options.
     * @param sheetName the name of the sheet to create
     * @param dataset the data to write to the sheet
     * @param sheetCreateOptions options for creating the sheet
     * @param sheetCreateOptions
     * @param outputExcelFile the Excel file to write to
     */
    public static void writeSheet(final String sheetName, final DataSet dataset, final SheetCreateOptions sheetCreateOptions, final File outputExcelFile) {
        final int columnColumn = dataset.columnCount();

        final Consumer<Sheet> sheetSetter = createSheetSetter(sheetCreateOptions, columnColumn);

        writeSheet(sheetName, dataset, sheetSetter, outputExcelFile);
    }

    /**
     * Writes the specified data to an Excel file with the given sheet name, headers, and options.
     * @param sheetName the name of the sheet to create
     * @param dataset the data to write to the sheet
     * @param sheetSetter a consumer to set additional properties on the sheet
     * @param outputExcelFile the Excel file to write to
     */
    public static void writeSheet(final String sheetName, final DataSet dataset, final Consumer<? super Sheet> sheetSetter, final File outputExcelFile) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);

        final int columnColumn = dataset.columnCount();

        final Row headerRow = sheet.createRow(0);

        for (int i = 0; i < columnColumn; i++) {
            headerRow.createCell(i).setCellValue(dataset.getColumnName(i));
        }

        final MutableInt rowNum = MutableInt.of(1);

        dataset.forEach(rowData -> {
            final Row row = sheet.createRow(rowNum.getAndIncrement());

            for (int i = 0; i < rowData.length(); i++) {
                setCellValue(row.createCell(i), rowData.get(i));
            }
        });

        if (sheetSetter != null) {
            sheetSetter.accept(sheet);
        }

        // Write to file
        try (OutputStream os = new FileOutputStream(outputExcelFile)) {
            workbook.write(os);
            workbook.close();
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    static void setCellValue(Cell cell, Object cellValue) {
        if (cellValue instanceof String val) {
            cell.setCellValue(val);
        } else if (cellValue instanceof Boolean val) {
            cell.setCellValue(val);
        } else if (cellValue instanceof java.util.Date val) {
            cell.setCellValue(val);
        } else if (cellValue instanceof java.util.Calendar val) {
            cell.setCellValue(val);
        } else if (cellValue instanceof Integer val) {
            cell.setCellValue(val);
        } else if (cellValue instanceof Double val) {
            cell.setCellValue(val);
        } else {
            cell.setCellValue(String.valueOf(cellValue));
        }
    }

    /**
     * Saves the specified sheet from the given Excel file as a CSV file.
     *
     * @param excelFile the Excel file to read
     * @param sheetIndex the index of the sheet to save as CSV, starting from 0.
     * @param outputCsvFile the output CSV file
     * @throws UncheckedException if an I/O error occurs while reading or writing the file
     */
    public static void saveSheetAsCsv(final File excelFile, final int sheetIndex, File outputCsvFile) {
        saveSheetAsCsv(excelFile, sheetIndex, null, false, outputCsvFile, Charsets.DEFAULT);
    }

    /**
     * Saves the specified sheet from the given Excel file as a CSV file.
     *
     * @param excelFile the Excel file to read
     * @param sheetName the name of the sheet to save as CSV
     * @param outputCsvFile the output CSV file
     * @throws UncheckedException if an I/O error occurs while reading or writing the file
     */
    public static void saveSheetAsCsv(final File excelFile, final String sheetName, File outputCsvFile) {
        saveSheetAsCsv(excelFile, sheetName, null, false, outputCsvFile, Charsets.DEFAULT);
    }

    /**
     * Saves the specified sheet from the given Excel file as a CSV file.
     *
     * @param excelFile the Excel file to read
     * @param sheetIndex the index of the sheet to save as CSV, starting from 0
     * @param csvHeader a list of strings representing the header row for the CSV file; can be null
     * @param quoted a boolean indicating whether the cell values should be quoted in the CSV file
     * @param outputCsvFile the output CSV file to write to
     * @param charset the character set to use for writing the CSV file
     * @throws UncheckedException if an I/O error occurs while reading or writing the file
     */
    public static void saveSheetAsCsv(final File excelFile, final int sheetIndex, List<String> csvHeader, final boolean quoted, File outputCsvFile,
            Charset charset) {
        final Function<Cell, String> cellMapper = quoted ? cell -> Strings.concat("\"", CELL2STRING.apply(cell), "\"") : CELL2STRING;
        final List<String> rowList = readSheet(excelFile, sheetIndex, RowMappers.toString(Strings.ELEMENT_SEPARATOR, cellMapper));

        try (Writer writer = IOUtil.newFileWriter(outputCsvFile, charset)) {
            if (N.notEmpty(csvHeader)) {
                IOUtil.writeLine(Strings.join(csvHeader, Strings.ELEMENT_SEPARATOR), writer);
            }

            IOUtil.writeLines(rowList, writer);
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Saves the specified sheet from the given Excel file as a CSV file with the specified character set.
     *
     * @param excelFile the Excel file to read
     * @param sheetName the name of the sheet to save as CSV
     * @param csvHeader a list of strings representing the header row for the CSV file; can be null
     * @param quoted a boolean indicating whether the cell values should be quoted in the CSV file
     * @param outputCsvFile the output CSV file to write to
     * @param charset the character set to use for writing the CSV file
     * @throws UncheckedException if an I/O error occurs while reading or writing the file
     */
    public static void saveSheetAsCsv(final File excelFile, final String sheetName, List<String> csvHeader, final boolean quoted, File outputCsvFile,
            Charset charset) {
        final Function<Cell, String> cellMapper = quoted ? cell -> Strings.concat(WD.QUOTATION_D, CELL2STRING.apply(cell), WD.QUOTATION_D) : CELL2STRING;
        final List<String> rowList = readSheet(excelFile, sheetName, RowMappers.toString(Strings.ELEMENT_SEPARATOR, cellMapper));

        try (Writer writer = IOUtil.newFileWriter(outputCsvFile, charset)) {
            if (N.notEmpty(csvHeader)) {
                IOUtil.writeLine(Strings.join(csvHeader, Strings.ELEMENT_SEPARATOR), writer);
            }

            IOUtil.writeLines(rowList, writer);
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    public static final class RowMappers {
        private RowMappers() {
            // prevent instantiation
        }

        public static final Function<Row, String> ROW2STRING = toString(Strings.ELEMENT_SEPARATOR);

        public static Function<Row, String> toString(final String cellSeparator) {
            return toString(cellSeparator, CELL2STRING);
        }

        public static Function<Row, String> toString(final String cellSeparator, final Function<Cell, String> cellMapper) {
            return row -> {
                final StringBuilder sb = Objectory.createStringBuilder();
                boolean first = true;

                try {
                    for (Cell cell : row) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(cellSeparator);
                        }

                        sb.append(cellMapper.apply(cell));
                    }

                    return sb.toString();
                } finally {
                    Objectory.recycle(sb);
                }
            };
        }

        public static Function<Row, List<String>> toList() {
            return toList(CELL2STRING);
        }

        public static <T> Function<Row, List<T>> toList(final Function<Cell, T> cellMapper) {
            return row -> {
                final List<T> list = new ArrayList<>(row.getPhysicalNumberOfCells());

                for (Cell cell : row) {
                    list.add(cellMapper.apply(cell));
                }

                return list;
            };
        }
    }

    public static final class RowExtractors {

        private RowExtractors() {
            // prevent instantiation
        }

        public static final TriConsumer<String[], Row, Object[]> ROW2STRING_ARRAY = (header, row, output) -> {
            int idx = 0;

            for (Cell cell : row) {
                output[idx++] = CELL2STRING.apply(cell);
            }
        };

        public static TriConsumer<String[], Row, Object[]> create(final Function<Cell, ?> cellMapper) {
            return (header, row, output) -> {
                int idx = 0;

                for (Cell cell : row) {
                    output[idx++] = cellMapper.apply(cell);
                }
            };
        }
    }

    @Data
    @Builder
    public static class SheetCreateOptions {
        private boolean autoSizeColumn;
        private FreezePane freezePane;
        private boolean freezeFirstRow;
        private CellRangeAddress autoFilter;
        private boolean autoFilterByFirstRow;
    }

    public record FreezePane(int colSplit, int rowSplit) {
    }
}
