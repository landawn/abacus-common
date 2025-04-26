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
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.stream.Stream;

import lombok.Builder;
import lombok.Data;

public final class ExcelUtil {

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
    public static <T> List<T> readSheet(final File excelFile, final int sheetIndex, final Function<Row, T> rowMapper) {
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
    public static <T> List<T> readSheet(final File excelFile, final String sheetName, final Function<Row, T> rowMapper) {
        try (InputStream is = new FileInputStream(excelFile); //
                Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheet(sheetName);
            return readSheet(sheet, rowMapper);

        } catch (IOException e) {
            throw new UncheckedException(e);
        }

    }

    private static <T> List<T> readSheet(final Sheet sheet, final Function<Row, T> rowMapper) {
        final List<T> rowList = new ArrayList<>();

        for (Row row : sheet) {
            rowList.add(rowMapper.apply(row));
        }

        return rowList;
    }

    /**
     * Returns a stream of rows from the specified sheet in the given Excel file.
     *
     * @param excelFile the Excel file to read
     * @param sheetIndex the index of the sheet to read
     * @return a stream of rows from the specified sheet
     * @throws UncheckedException if an I/O error occurs while reading the file
     */
    public static Stream<Row> rows(final File excelFile, final int sheetIndex) {
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
    public static Stream<Row> rows(final File excelFile, final String sheetName) {
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
     *
     * @param excelFile the Excel file to write to
     * @param sheetName the name of the sheet to create
     * @param headers the headers for the columns
     * @param rows the data to write to the sheet
     */
    public static void writeSheet(final File excelFile, final String sheetName, final List<String> headers, final List<? extends Collection<?>> rows) {
        writeSheet(excelFile, sheetName, headers, rows, (SheetCreateOptions) null);
    }

    /**
     * Writes the specified data to an Excel file with the given sheet name, headers, and options.
     *
     * @param excelFile the Excel file to write to
     * @param sheetName the name of the sheet to create
     * @param headers the headers for the columns
     * @param rows the data to write to the sheet
     * @param sheetCreateOptions options for creating the sheet
     * @param sheetCreateOptions
     */
    public static void writeSheet(final File excelFile, final String sheetName, final List<String> headers, final List<? extends Collection<?>> rows,
            final SheetCreateOptions sheetCreateOptions) {
        final int columnColumn = headers.size();

        final Consumer<Sheet> sheetSetter = sheetCreateOptions == null ? null : sheet -> {
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

        writeSheet(excelFile, sheetName, headers, rows, sheetSetter);
    }

    /**
     * Writes the specified data to an Excel file with the given sheet name, headers, and options.
     *
     * @param excelFile the Excel file to write to
     * @param sheetName the name of the sheet to create
     * @param headers the headers for the columns
     * @param rows the data to write to the sheet
     * @param sheetSetter a consumer to set additional properties on the sheet
     * @param sheetCreateOptions
     */
    public static void writeSheet(final File excelFile, final String sheetName, final List<String> headers, final List<? extends Collection<?>> rows,
            final Consumer<? super Sheet> sheetSetter) {
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
            Object cellValue = null;

            for (int i = 0; i < rowData.size(); i++) {
                cellValue = iter.next();
                Cell cell = row.createCell(i);

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
        }

        if (sheetSetter != null) {
            sheetSetter.accept(sheet);
        }

        // Write to file
        try (OutputStream os = new FileOutputStream(excelFile)) {
            workbook.write(os);
            workbook.close();
        } catch (IOException e) {
            throw new UncheckedException(e);
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
        final Function<Cell, String> cellMapper = quoted ? cell -> Strings.concat("\"", RowMappers.CELL2STRING.apply(cell), "\"") : RowMappers.CELL2STRING;
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
     * @param sheetIndex the index of the sheet to save as CSV, starting from 0
     * @param csvHeader a list of strings representing the header row for the CSV file; can be null
     * @param quoted a boolean indicating whether the cell values should be quoted in the CSV file
     * @param outputCsvFile the output CSV file to write to
     * @param charset the character set to use for writing the CSV file
     * @throws UncheckedException if an I/O error occurs while reading or writing the file
     */
    public static void saveSheetAsCsv(final File excelFile, final String sheetName, List<String> csvHeader, final boolean quoted, File outputCsvFile,
            Charset charset) {
        final Function<Cell, String> cellMapper = quoted ? cell -> Strings.concat(WD.QUOTATION_D, RowMappers.CELL2STRING.apply(cell), WD.QUOTATION_D)
                : RowMappers.CELL2STRING;
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

        public static final Function<Cell, String> CELL2STRING = cell -> {
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue();
                case NUMERIC -> String.valueOf(cell.getNumericCellValue());
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                case FORMULA -> cell.getCellFormula();
                case BLANK -> "";
                default -> throw new RuntimeException("Unsupported cell type: " + cell.getCellType());
            };
        };

        private static final Function<Row, String> ROW2STRING = toString(Strings.ELEMENT_SEPARATOR);

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

    @Data
    @Builder
    public static class SheetCreateOptions {
        private boolean autoSizeColumn;
        private FreezePane freezePane;
        private boolean freezeFirstRow;
        private CellRangeAddress autoFilter;
        private boolean autoFilterByFirstRow;
    }

    public static record FreezePane(int colSplit, int rowSplit) {
    }
}
