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
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.BufferedCSVWriter;
import com.landawn.abacus.util.CSVUtil;
import com.landawn.abacus.util.Charsets;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.RowDataset;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Utility class for reading and writing Excel files using Apache POI.
 * Provides methods to read Excel sheets into various data structures and write data to Excel files.
 * Supports conversion between Excel and CSV formats.
 * 
 * <p>This class is thread-safe as all methods are stateless.</p>
 * 
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Read Excel file into Dataset
 * Dataset data = ExcelUtil.loadSheet(new File("data.xlsx"));
 * 
 * // Write Dataset to Excel
 * ExcelUtil.writeSheet("Sheet1", data, new File("output.xlsx"));
 * }</pre>
 * 
 * @since 0.8
 */
public final class ExcelUtil {

    /**
     * Default cell getter function that extracts cell values based on their type.
     * Returns appropriate Java objects: String for STRING cells, Double for NUMERIC cells,
     * Boolean for BOOLEAN cells, String for FORMULA cells, and empty string for BLANK cells.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Object value = CELL_GETTER.apply(cell);
     * }</pre>
     */
    public static final Function<Cell, Object> CELL_GETTER = cell -> switch (cell.getCellType()) {
        case STRING -> cell.getStringCellValue();
        case NUMERIC -> cell.getNumericCellValue();
        case BOOLEAN -> cell.getBooleanCellValue();
        case FORMULA -> cell.getCellFormula();
        case BLANK -> "";
        default -> throw new RuntimeException("Unsupported cell type: " + cell.getCellType());
    };

    /**
     * Cell to String converter function that converts any cell value to its string representation.
     * Handles all cell types including STRING, NUMERIC, BOOLEAN, FORMULA, and BLANK.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * String stringValue = CELL2STRING.apply(cell);
     * }</pre>
     */
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
     * Loads the first sheet from the specified Excel file into a Dataset.
     * This is a convenience method that uses the first row as column headers and applies
     * the default row extractor (CELL_GETTER) for data extraction. Cell types are preserved
     * as appropriate Java objects (String, Double, Boolean, etc.).
     *
     * <p>The method automatically handles Excel file format detection and resource cleanup.
     * The resulting Dataset provides column-based access to the data with type preservation.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dataset dataset = ExcelUtil.loadSheet(new File("sales_data.xlsx"));
     * // Access data by column name
     * List<String> productNames = dataset.getColumn("Product");
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @return a Dataset containing the sheet data with the first row as column names, empty Dataset if sheet is empty
     * @throws UncheckedException if an I/O error occurs while reading the file, or if the file is not a valid Excel file
     */
    public static Dataset loadSheet(final File excelFile) {
        return loadSheet(excelFile, 0, RowExtractors.DEFAULT);
    }

    /**
     * Loads the specified sheet from the Excel file into a Dataset using a custom row extractor.
     * This method provides fine-grained control over how row data is extracted and converted,
     * allowing for custom parsing logic, data validation, or type conversion. The first row
     * is always treated as column headers.
     *
     * <p>The row extractor receives the header names, current row, and an output array to populate.
     * This allows for conditional extraction, computed values, or format-specific parsing.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * TriConsumer<String[], Row, Object[]> customExtractor = (headers, row, output) -> {
     *     for (int i = 0; i < headers.length; i++) {
     *         Cell cell = row.getCell(i);
     *         output[i] = cell == null ? null : cell.getStringCellValue();
     *     }
     * };
     * Dataset dataset = ExcelUtil.loadSheet(new File("data.xlsx"), 0, customExtractor);
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @param sheetIndex the zero-based index of the sheet to read (0 for first sheet)
     * @param rowExtractor custom function to extract row data. Receives three parameters:
     *                     column headers array, current row, and output array to populate with extracted values
     * @return a Dataset containing the extracted sheet data with the first row as column names
     * @throws UncheckedException if an I/O error occurs while reading the file, if the file is not a valid Excel file,
     *                            or if the sheet index is out of bounds
     */
    public static Dataset loadSheet(final File excelFile, final int sheetIndex,
            final TriConsumer<? super String[], ? super Row, ? super Object[]> rowExtractor) {
        try (InputStream is = new FileInputStream(excelFile); //
                Workbook workbook = new XSSFWorkbook(is)) {
            return loadSheet(workbook.getSheetAt(sheetIndex), rowExtractor);

        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Loads the sheet with the specified name from the Excel file into a Dataset.
     * This method allows loading a specific sheet by name rather than index, which is useful
     * when working with workbooks that have meaningful sheet names. Uses a custom row extractor
     * to process each row after the header row.
     *
     * <p>The row extractor provides full control over data extraction and transformation,
     * enabling custom parsing, validation, or type conversion logic per cell or row.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dataset dataset = ExcelUtil.loadSheet(
     *     new File("workbook.xlsx"),
     *     "Sales2024",
     *     RowExtractors.DEFAULT
     * );
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @param sheetName the name of the sheet to read, case-sensitive
     * @param rowExtractor custom function to extract row data. Receives three parameters:
     *                     column headers array, current row, and output array to populate with extracted values
     * @return a Dataset containing the extracted sheet data with the first row as column names
     * @throws UncheckedException if an I/O error occurs, if the file is not a valid Excel file,
     *                            or if the sheet name is not found in the workbook
     */
    public static Dataset loadSheet(final File excelFile, final String sheetName,
            final TriConsumer<? super String[], ? super Row, ? super Object[]> rowExtractor) {
        try (InputStream is = new FileInputStream(excelFile); //
                Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheet(sheetName);
            return loadSheet(sheet, rowExtractor);

        } catch (IOException e) {
            throw new UncheckedException(e);
        }

    }

    private static Dataset loadSheet(final Sheet sheet, final TriConsumer<? super String[], ? super Row, ? super Object[]> rowExtractor) {
        final Iterator<Row> rowIter = sheet.rowIterator();

        if (!rowIter.hasNext()) {
            return Dataset.empty();
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

        return new RowDataset(columnNameList, columnList);
    }

    /**
     * Reads the first sheet of the Excel file and returns rows as a list of lists.
     * This is a convenience method that processes all rows (including headers) and returns
     * each row as a list of cell values. Cell types are preserved as appropriate Java objects
     * (String, Double, Boolean, etc.) using the default row mapper.
     *
     * <p>Unlike loadSheet methods, this does not create a Dataset structure and instead
     * provides direct access to row data. Useful for simple row-by-row processing without
     * column-based access requirements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<List<Object>> rows = ExcelUtil.readSheet(new File("data.xlsx"));
     * for (List<Object> row : rows) {
     *     System.out.println("Row: " + row);
     * }
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @return a list of rows, where each row is a list of cell values with preserved types
     * @throws UncheckedException if an I/O error occurs while reading the file or if the file is not a valid Excel file
     */
    public static List<List<Object>> readSheet(final File excelFile) {
        return readSheet(excelFile, 0, false, RowMappers.DEFAULT);
    }

    /**
     * Reads the specified sheet and maps each row to a custom object type using the provided mapper.
     * This method provides flexible row-to-object transformation, allowing conversion of Excel rows
     * to domain objects, DTOs, or any custom type. Optionally skips the first row if it contains headers.
     *
     * <p>The row mapper receives the complete Row object from Apache POI, providing access to all
     * cells, formatting information, and row metadata. This enables sophisticated mapping logic
     * including conditional processing based on cell types or values.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Function<Row, String> rowToString = row -> {
     *     StringBuilder sb = new StringBuilder();
     *     row.forEach(cell -> sb.append(cell).append(","));
     *     return sb.toString();
     * };
     * List<String> rows = ExcelUtil.readSheet(new File("data.xlsx"), 0, true, rowToString);
     * }</pre>
     *
     * @param <T> the type of objects to map rows to
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @param sheetIndex the zero-based index of the sheet to read (0 for first sheet)
     * @param skipFirstRow {@code true} to skip the first row (typically headers), {@code false} to process all rows
     * @param rowMapper function to convert each Row to an object of type T
     * @return a list of mapped objects, one per row (excluding skipped rows)
     * @throws UncheckedException if an I/O error occurs while reading the file, if the file is not a valid Excel file,
     *                            or if the sheet index is out of bounds
     */
    public static <T> List<T> readSheet(final File excelFile, final int sheetIndex, final boolean skipFirstRow,
            final Function<? super Row, ? extends T> rowMapper) {
        try (InputStream is = new FileInputStream(excelFile); //
                Workbook workbook = new XSSFWorkbook(is)) {
            return readSheet(workbook.getSheetAt(sheetIndex), skipFirstRow, rowMapper);

        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Reads the sheet with the specified name and maps each row to a custom object type.
     * This method combines named sheet access with flexible row mapping, making it ideal
     * for processing specific worksheets in multi-sheet workbooks. Allows skipping the first
     * row and applying a custom row mapping function to transform rows into domain objects.
     *
     * <p>The row mapper provides full control over the transformation process, enabling
     * validation, default value handling, and complex object construction from row data.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Function<Row, Product> rowToProduct = row -> new Product(
     *     row.getCell(0).getStringCellValue(),  // name
     *     row.getCell(1).getNumericCellValue()  // price
     * );
     * List<Product> products = ExcelUtil.readSheet(
     *     new File("products.xlsx"),
     *     "ProductList",
     *     true,
     *     rowToProduct
     * );
     * }</pre>
     *
     * @param <T> the type of objects to map rows to
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @param sheetName the name of the sheet to read, case-sensitive
     * @param skipFirstRow {@code true} to skip the first row (typically headers), {@code false} to process all rows
     * @param rowMapper function to convert each Row to an object of type T
     * @return a list of mapped objects, one per row (excluding skipped rows)
     * @throws UncheckedException if an I/O error occurs, if the file is not a valid Excel file,
     *                            or if the sheet name is not found in the workbook
     */
    public static <T> List<T> readSheet(final File excelFile, final String sheetName, final boolean skipFirstRow,
            final Function<? super Row, ? extends T> rowMapper) {
        try (InputStream is = new FileInputStream(excelFile); //
                Workbook workbook = new XSSFWorkbook(is)) {
            Sheet sheet = workbook.getSheet(sheetName);
            return readSheet(sheet, skipFirstRow, rowMapper);

        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    private static <T> List<T> readSheet(final Sheet sheet, final boolean skipFirstRow, final Function<? super Row, ? extends T> rowMapper) {
        final List<T> rowList = new ArrayList<>();
        final Iterator<Row> rowIter = sheet.rowIterator();

        if (skipFirstRow && rowIter.hasNext()) {
            rowIter.next(); // skip the first row
        }

        while (rowIter.hasNext()) {
            rowList.add(rowMapper.apply(rowIter.next()));
        }

        return rowList;
    }

    /**
     * Creates a stream of Row objects from the specified sheet in the Excel file.
     * This method enables lazy, memory-efficient processing of large Excel files by streaming
     * rows one at a time instead of loading the entire sheet into memory. Ideal for processing
     * files that are too large to fit comfortably in memory or when early termination is possible.
     *
     * <p>The stream leverages Java's Stream API, allowing functional-style operations like
     * filtering, mapping, and reduction. This enables powerful data processing pipelines while
     * maintaining low memory footprint.</p>
     *
     * <p><strong>Important:</strong> The Stream must be closed after use to release file handles
     * and workbook resources. Always use try-with-resources or explicitly call {@code close()}
     * on the stream when done.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Stream<Row> stream = ExcelUtil.streamSheet(new File("large_data.xlsx"), 0, true)) {
     *     stream.filter(row -> row.getCell(0).getNumericCellValue() > 1000)
     *           .forEach(row -> processRow(row));
     * }
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @param sheetIndex the zero-based index of the sheet to stream (0 for first sheet)
     * @param skipFirstRow {@code true} to skip the first row (typically headers), {@code false} to process all rows
     * @return a Stream of Row objects from the specified sheet that must be closed after use
     * @throws UncheckedException if an I/O error occurs while reading the file or if the file is not a valid Excel file
     */
    public static Stream<Row> streamSheet(final File excelFile, final int sheetIndex, final boolean skipFirstRow) {
        try {
            final InputStream is = new FileInputStream(excelFile);
            final Workbook workbook = new XSSFWorkbook(is);
            final Sheet sheet = workbook.getSheetAt(sheetIndex);

            return Stream.of(sheet.rowIterator()).skip(skipFirstRow ? 1 : 0).onClose(() -> {
                try {
                    workbook.close();
                    is.close();
                } catch (IOException e) {
                    throw new UncheckedException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Creates a stream of Row objects from the sheet with the specified name.
     * This method combines named sheet access with memory-efficient streaming, enabling
     * lazy processing of specific worksheets in large Excel files. Particularly useful for
     * multi-sheet workbooks where only certain sheets need to be processed.
     *
     * <p>Streaming allows for early termination, filtering, and transformation operations
     * without loading the entire sheet into memory. This is essential for handling large
     * datasets or when processing can be short-circuited based on conditions.</p>
     *
     * <p><strong>Important:</strong> The Stream must be closed after use to release file handles
     * and workbook resources. Always use try-with-resources or explicitly call {@code close()}
     * on the stream when done.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Stream<Row> stream = ExcelUtil.streamSheet(new File("data.xlsx"), "Sales", true)) {
     *     long count = stream.map(row -> row.getCell(2).getNumericCellValue())
     *                        .filter(value -> value > 100)
     *                        .count();
     * }
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @param sheetName the name of the sheet to stream, case-sensitive
     * @param skipFirstRow {@code true} to skip the first row (typically headers), {@code false} to process all rows
     * @return a Stream of Row objects from the specified sheet that must be closed after use
     * @throws UncheckedException if an I/O error occurs, if the file is not a valid Excel file,
     *                            or if the sheet name is not found in the workbook
     */
    public static Stream<Row> streamSheet(final File excelFile, final String sheetName, final boolean skipFirstRow) {
        try {
            final InputStream is = new FileInputStream(excelFile);
            final Workbook workbook = new XSSFWorkbook(is);
            final Sheet sheet = workbook.getSheet(sheetName);

            return Stream.of(sheet.rowIterator()).skip(skipFirstRow ? 1 : 0).onClose(() -> {
                try {
                    workbook.close();
                    is.close();
                } catch (IOException e) {
                    throw new UncheckedException(e);
                }
            });
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Writes data to a new Excel file with the specified sheet name and headers.
     * This is a convenience method that creates a basic Excel file without any special
     * formatting, auto-sizing, or freeze panes. The resulting file contains a single sheet
     * with the provided headers and data rows.
     *
     * <p>Cell values are automatically converted based on their Java types: String, Boolean,
     * Date, Calendar, Integer, and Double are set using type-specific cell methods, while
     * other types are converted to strings.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Object> headers = Arrays.asList("Name", "Age", "City");
     * List<List<String>> rows = Arrays.asList(
     *     Arrays.asList("John", "30", "New York"),
     *     Arrays.asList("Jane", "25", "London")
     * );
     * ExcelUtil.writeSheet("Users", headers, rows, new File("users.xlsx"));
     * }</pre>
     *
     * @param sheetName the name of the sheet to create in the workbook
     * @param headers the column headers as a list of objects (will be converted to strings)
     * @param rows the data rows, where each row is a collection of cell values
     * @param outputExcelFile the file to write the Excel data to (will be created or overwritten)
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created
     */
    public static void writeSheet(final String sheetName, final List<Object> headers, final List<? extends Collection<?>> rows, final File outputExcelFile) {
        writeSheet(sheetName, headers, rows, (SheetCreateOptions) null, outputExcelFile);
    }

    /**
     * Writes data to a new Excel file with additional formatting options.
     * This method extends the basic write functionality by applying professional formatting
     * options such as auto-sized columns, frozen panes, and auto-filters. These options
     * improve readability and usability of the generated Excel files.
     *
     * <p>The SheetCreateOptions allows configuring multiple formatting aspects:
     * auto-sizing columns to fit content, freezing rows/columns for easier navigation,
     * and adding auto-filters for data filtering capabilities.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SheetCreateOptions options = SheetCreateOptions.builder()
     *     .autoSizeColumn(true)
     *     .freezeFirstRow(true)
     *     .autoFilterByFirstRow(true)
     *     .build();
     *
     * ExcelUtil.writeSheet("Report", headers, data, options, new File("report.xlsx"));
     * }</pre>
     *
     * @param sheetName the name of the sheet to create in the workbook
     * @param headers the column headers as a list of objects (will be converted to strings)
     * @param rows the data rows, where each row is a collection of cell values
     * @param sheetCreateOptions configuration options for sheet formatting (null to apply no formatting)
     * @param outputExcelFile the file to write the Excel data to (will be created or overwritten)
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created
     */
    public static void writeSheet(final String sheetName, final List<Object> headers, final List<? extends Collection<?>> rows,
            final SheetCreateOptions sheetCreateOptions, final File outputExcelFile) {
        final int columnCount = headers.size();

        final Consumer<Sheet> sheetSetter = createSheetSetter(sheetCreateOptions, columnCount);

        writeSheet(sheetName, headers, rows, sheetSetter, outputExcelFile);
    }

    static Consumer<Sheet> createSheetSetter(final SheetCreateOptions sheetCreateOptions, final int columnCount) {
        return sheetCreateOptions == null ? null : sheet -> {
            if (sheetCreateOptions.isAutoSizeColumn()) {
                // Resize columns to fit content
                for (int i = 0; i < columnCount; i++) {
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
                sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, columnCount - 1));
            }
        };
    }

    /**
     * Writes data to a new Excel file with a custom sheet configuration function.
     * This method provides maximum flexibility by allowing direct manipulation of the Sheet
     * object through a custom consumer. Use this when you need formatting options beyond what
     * SheetCreateOptions provides, such as custom column widths, conditional formatting,
     * cell styles, or data validation rules.
     *
     * <p>The sheetSetter consumer is invoked after all data has been written, allowing you
     * to apply formatting based on the actual content. This is useful for advanced scenarios
     * like conditional formatting, merged cells, or complex styling requirements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Consumer<Sheet> customFormatter = sheet -> {
     *     sheet.setDefaultColumnWidth(15);
     *     sheet.createFreezePane(0, 1);
     *     sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, 3));
     * };
     *
     * ExcelUtil.writeSheet("CustomSheet", headers, data, customFormatter, new File("custom.xlsx"));
     * }</pre>
     *
     * @param sheetName the name of the sheet to create in the workbook
     * @param headers the column headers as a list of objects (will be converted to strings)
     * @param rows the data rows, where each row is a collection of cell values
     * @param sheetSetter a consumer to apply custom formatting to the sheet after data is written (null to skip)
     * @param outputExcelFile the file to write the Excel data to (will be created or overwritten)
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created
     */
    public static void writeSheet(final String sheetName, final List<Object> headers, final List<? extends Collection<?>> rows,
            final Consumer<? super Sheet> sheetSetter, final File outputExcelFile) {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet(sheetName);

        final int columnCount = headers.size();

        final Row headerRow = sheet.createRow(0);

        for (int i = 0; i < columnCount; i++) {
            headerRow.createCell(i).setCellValue(N.stringOf(headers.get(i)));
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
     * Writes a Dataset to a new Excel file with the specified sheet name.
     * This convenience method automatically extracts column names from the Dataset to use
     * as headers and writes all data rows to the Excel file. The Dataset's structure and
     * column ordering are preserved in the output file.
     *
     * <p>This is the simplest way to export a Dataset to Excel format, creating a basic
     * Excel file without any special formatting. For formatted output, use the overloaded
     * methods that accept SheetCreateOptions or a custom sheetSetter.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dataset dataset = Dataset.fromCSV(new File("data.csv"));
     * ExcelUtil.writeSheet("ImportedData", dataset, new File("output.xlsx"));
     * }</pre>
     *
     * @param sheetName the name of the sheet to create in the workbook
     * @param dataset the Dataset containing the data to write, must not be null
     * @param outputExcelFile the file to write the Excel data to (will be created or overwritten)
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created
     */
    public static void writeSheet(final String sheetName, final Dataset dataset, final File outputExcelFile) {
        writeSheet(sheetName, dataset, (SheetCreateOptions) null, outputExcelFile);
    }

    /**
     * Writes a Dataset to a new Excel file with additional formatting options.
     * This method combines the convenience of Dataset export with professional formatting
     * capabilities, enabling creation of polished Excel reports directly from Dataset objects.
     * Automatically uses the Dataset's column names as headers.
     *
     * <p>The SheetCreateOptions allows applying various formatting enhancements such as
     * auto-sizing columns to fit content, freezing panes for better navigation of large
     * datasets, and adding auto-filters for interactive data exploration.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SheetCreateOptions options = SheetCreateOptions.builder()
     *     .autoSizeColumn(true)
     *     .freezePane(new FreezePane(2, 1))  // Freeze first 2 columns and first row
     *     .build();
     *
     * ExcelUtil.writeSheet("Analysis", dataset, options, new File("analysis.xlsx"));
     * }</pre>
     *
     * @param sheetName the name of the sheet to create in the workbook
     * @param dataset the Dataset containing the data to write, must not be null
     * @param sheetCreateOptions configuration options for sheet formatting (null to apply no formatting)
     * @param outputExcelFile the file to write the Excel data to (will be created or overwritten)
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created
     */
    public static void writeSheet(final String sheetName, final Dataset dataset, final SheetCreateOptions sheetCreateOptions, final File outputExcelFile) {
        final int columnColumn = dataset.columnCount();

        final Consumer<Sheet> sheetSetter = createSheetSetter(sheetCreateOptions, columnColumn);

        writeSheet(sheetName, dataset, sheetSetter, outputExcelFile);
    }

    /**
     * Writes a Dataset to a new Excel file with custom sheet formatting.
     * This method provides maximum control over the output Excel file by allowing direct
     * manipulation of the Sheet object after data is written. Use this for advanced scenarios
     * requiring conditional formatting, cell styles, data validation, merged cells, or any
     * other Apache POI sheet operations.
     *
     * <p>The sheetSetter consumer receives the Sheet object after all data has been written,
     * enabling context-aware formatting decisions based on the actual content and structure.
     * This is the most flexible write method for creating sophisticated Excel reports.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Consumer<Sheet> formatter = sheet -> {
     *     // Apply conditional formatting
     *     SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();
     *     // ... configure conditional formatting
     * };
     *
     * ExcelUtil.writeSheet("FormattedData", dataset, formatter, new File("formatted.xlsx"));
     * }</pre>
     *
     * @param sheetName the name of the sheet to create in the workbook
     * @param dataset the Dataset containing the data to write, must not be null
     * @param sheetSetter a consumer to apply custom formatting to the sheet after data is written (null to skip)
     * @param outputExcelFile the file to write the Excel data to (will be created or overwritten)
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created
     */
    public static void writeSheet(final String sheetName, final Dataset dataset, final Consumer<? super Sheet> sheetSetter, final File outputExcelFile) {
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
            cell.setCellValue(cellValue == null ? Strings.NULL : N.stringOf(cellValue));
        }
    }

    /**
     * Converts the specified sheet from an Excel file to a CSV file using default charset.
     * This convenience method exports an Excel sheet to comma-separated values format,
     * preserving all data including the header row. Uses the platform's default charset
     * for text encoding.
     *
     * <p>Cell values are converted to their string representations: numeric cells are
     * formatted as numbers, formulas are exported as formula text, and blank cells
     * become empty strings. The CSV format uses comma as delimiter and proper quoting
     * for values containing special characters.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExcelUtil.saveSheetAsCsv(
     *     new File("data.xlsx"),
     *     0,
     *     new File("output.csv")
     * );
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @param sheetIndex the zero-based index of the sheet to convert (0 for first sheet)
     * @param outputCsvFile the CSV file to write to (will be created or overwritten)
     * @throws UncheckedException if an I/O error occurs during conversion, if the file is not a valid Excel file,
     *                            or if the sheet index is out of bounds
     */
    public static void saveSheetAsCsv(final File excelFile, final int sheetIndex, File outputCsvFile) {
        saveSheetAsCsv(excelFile, sheetIndex, null, outputCsvFile, Charsets.DEFAULT);
    }

    /**
     * Converts the sheet with the specified name from an Excel file to a CSV file.
     * This method allows exporting a specific worksheet by name rather than index, which is
     * more robust when working with workbooks where sheet positions might change. Uses the
     * platform's default charset for text encoding and preserves all original data including headers.
     *
     * <p>The conversion process maintains data fidelity by converting each cell type appropriately:
     * strings remain as text, numbers are formatted, and formulas are exported as their text
     * representation. The output CSV uses standard comma delimiting with proper escaping.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExcelUtil.saveSheetAsCsv(
     *     new File("workbook.xlsx"),
     *     "SalesData",
     *     new File("sales.csv")
     * );
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @param sheetName the name of the sheet to convert, case-sensitive
     * @param outputCsvFile the CSV file to write to (will be created or overwritten)
     * @throws UncheckedException if an I/O error occurs, if the file is not a valid Excel file,
     *                            or if the sheet name is not found in the workbook
     */
    public static void saveSheetAsCsv(final File excelFile, final String sheetName, File outputCsvFile) {
        saveSheetAsCsv(excelFile, sheetName, null, outputCsvFile, Charsets.DEFAULT);
    }

    /**
     * Converts the specified sheet from an Excel file to a CSV file with custom options.
     * This method provides full control over the CSV export process, allowing you to replace
     * the original Excel headers with custom headers and specify the character encoding for
     * the output file. When custom headers are provided, the first row of the Excel sheet
     * is skipped and replaced with the custom headers.
     *
     * <p>The charset parameter is particularly important for international data or when
     * the CSV will be consumed by systems with specific encoding requirements. Common
     * choices include UTF-8 for universal compatibility or ISO-8859-1 for legacy systems.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> customHeaders = Arrays.asList("Product ID", "Product Name", "Price");
     * ExcelUtil.saveSheetAsCsv(
     *     new File("products.xlsx"),
     *     0,
     *     customHeaders,
     *     new File("products.csv"),
     *     StandardCharsets.UTF_8
     * );
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @param sheetIndex the zero-based index of the sheet to convert (0 for first sheet)
     * @param csvHeaders custom headers for the CSV file (null to use original Excel headers from first row)
     * @param outputCsvFile the CSV file to write to (will be created or overwritten)
     * @param charset the character encoding to use for the CSV file (e.g., UTF-8, ISO-8859-1)
     * @throws UncheckedException if an I/O error occurs during conversion, if the file is not a valid Excel file,
     *                            or if the sheet index is out of bounds
     */
    public static void saveSheetAsCsv(final File excelFile, final int sheetIndex, List<String> csvHeaders, File outputCsvFile, Charset charset) {
        try (InputStream is = new FileInputStream(excelFile); //
                Workbook workbook = new XSSFWorkbook(is)) {
            final Sheet sheet = workbook.getSheetAt(sheetIndex);

            saveSheetAsCsv(sheet, csvHeaders, outputCsvFile, charset);
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Converts the sheet with the specified name to a CSV file with full control over headers and encoding.
     * This is the most flexible CSV export method, combining named sheet access with custom
     * header replacement and character encoding specification. Ideal for generating CSV files
     * that need to conform to specific system requirements or international character sets.
     *
     * <p>When custom headers are provided, they replace the first row of the Excel sheet in
     * the CSV output. This is useful for normalizing column names, translating headers, or
     * adapting to target system naming conventions. The charset parameter ensures proper
     * handling of special characters and international text.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Export with custom headers in ISO-8859-1 encoding
     * List<String> headers = Arrays.asList("Código", "Nombre", "Precio");
     * ExcelUtil.saveSheetAsCsv(
     *     new File("productos.xlsx"),
     *     "Inventario",
     *     headers,
     *     new File("productos.csv"),
     *     Charset.forName("ISO-8859-1")
     * );
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @param sheetName the name of the sheet to convert, case-sensitive
     * @param csvHeaders custom headers for the CSV file (null to use original Excel headers from first row)
     * @param outputCsvFile the CSV file to write to (will be created or overwritten)
     * @param charset the character encoding to use for the CSV file (e.g., UTF-8, ISO-8859-1)
     * @throws UncheckedException if an I/O error occurs, if the file is not a valid Excel file,
     *                            or if the sheet name is not found in the workbook
     */
    public static void saveSheetAsCsv(final File excelFile, final String sheetName, List<String> csvHeaders, File outputCsvFile, Charset charset) {
        try (InputStream is = new FileInputStream(excelFile); //
                Workbook workbook = new XSSFWorkbook(is)) {
            final Sheet sheet = workbook.getSheet(sheetName);

            saveSheetAsCsv(sheet, csvHeaders, outputCsvFile, charset);
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    private static void saveSheetAsCsv(final Sheet sheet, List<String> csvHeaders, File outputCsvFile, Charset charset) {
        final Type<Object> strType = N.typeOf(String.class);
        final char separator = WD._COMMA;

        try (Writer writer = IOUtil.newFileWriter(outputCsvFile, charset)) {
            final BufferedCSVWriter bw = Objectory.createBufferedCSVWriter(writer);

            try {
                if (N.notEmpty(csvHeaders)) {
                    int idx = 0;

                    for (String csvHeader : csvHeaders) {
                        if (idx++ > 0) {
                            bw.write(separator);
                        }

                        CSVUtil.writeField(bw, strType, csvHeader);
                    }

                }

                int rowIndex = N.notEmpty(csvHeaders) ? 1 : 0;

                for (Row row : sheet) {
                    if (rowIndex++ > 0) {
                        bw.write(IOUtil.LINE_SEPARATOR);
                    }

                    int idx = 0;

                    for (Cell cell : row) {
                        if (idx++ > 0) {
                            bw.write(separator);
                        }

                        switch (cell.getCellType()) {
                            case STRING -> CSVUtil.writeField(bw, strType, cell.getStringCellValue());
                            case NUMERIC -> CSVUtil.writeField(bw, null, cell.getNumericCellValue());
                            case BOOLEAN -> CSVUtil.writeField(bw, null, cell.getBooleanCellValue());
                            case FORMULA -> CSVUtil.writeField(bw, strType, cell.getCellFormula());
                            case BLANK -> CSVUtil.writeField(bw, strType, "");
                            default -> throw new RuntimeException("Unsupported cell type: " + cell.getCellType());
                        }
                    }
                }

            } finally {
                Objectory.recycle(bw);
            }
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Collection of predefined row mapping functions for common use cases.
     * Provides ready-to-use mappers for converting Excel rows to various formats.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use default mapper to get list of objects
     * List<List<Object>> rows = ExcelUtil.readSheet(
     *     new File("data.xlsx"), 
     *     0, 
     *     false, 
     *     RowMappers.DEFAULT
     * );
     * 
     * // Convert rows to strings
     * List<String> rowStrings = ExcelUtil.readSheet(
     *     new File("data.xlsx"), 
     *     0, 
     *     false, 
     *     RowMappers.ROW2STRING
     * );
     * }</pre>
     */
    public static final class RowMappers {
        private RowMappers() {
            // prevent instantiation
        }

        /**
         * Default row mapper that converts each row to a List of Objects.
         * Uses CELL_GETTER to extract appropriate Java objects from cells.
         */
        public static final Function<Row, List<Object>> DEFAULT = toList(CELL_GETTER);

        /**
         * Row to String mapper that converts entire rows to delimited strings.
         * Uses the default element separator from Strings.ELEMENT_SEPARATOR.
         */
        public static final Function<Row, String> ROW2STRING = toString(Strings.ELEMENT_SEPARATOR);

        /**
         * Creates a row mapper that converts rows to delimited strings with a custom separator.
         * Each cell is converted to its string representation using the default CELL2STRING mapper
         * and the resulting strings are joined together with the specified separator. Useful for
         * creating custom text representations of Excel rows.
         *
         * <p>This method uses CELL2STRING for cell conversion, which handles all cell types
         * (STRING, NUMERIC, BOOLEAN, FORMULA, BLANK) and converts them to appropriate string
         * representations.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Function<Row, String> pipeMapper = RowMappers.toString("|");
         * List<String> rows = ExcelUtil.readSheet(file, 0, true, pipeMapper);
         * // Results in rows like: "John|30|New York"
         * }</pre>
         *
         * @param cellSeparator the string to use as separator between cell values
         * @return a Function that converts Row to delimited String
         */
        public static Function<Row, String> toString(final String cellSeparator) {
            return toString(cellSeparator, CELL2STRING);
        }

        /**
         * Creates a row mapper that converts rows to delimited strings with custom cell processing.
         * This method provides maximum flexibility by allowing you to specify both the separator
         * and a custom cell-to-string conversion function. Use this when you need special formatting,
         * value transformations, or custom handling of specific cell types.
         *
         * <p>The cellMapper function is applied to each cell individually, giving you complete
         * control over formatting numbers, dates, handling {@code null} cells, or applying business logic
         * during the conversion process.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Function<Cell, String> customCellMapper = cell ->
         *     cell.getCellType() == CellType.NUMERIC
         *         ? String.format("%.2f", cell.getNumericCellValue())
         *         : CELL2STRING.apply(cell);
         *
         * Function<Row, String> mapper = RowMappers.toString(",", customCellMapper);
         * }</pre>
         *
         * @param cellSeparator the string to use as separator between cell values
         * @param cellMapper custom function to convert each cell to a string
         * @return a Function that converts Row to delimited String
         */
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

        /**
         * Creates a row mapper that converts rows to Lists with custom cell processing.
         * This method enables type-safe extraction of data from Excel rows by applying a custom
         * cell mapper to each cell. Particularly useful when you need to extract specific data
         * types, perform validation, or apply transformations during the read process.
         *
         * <p>The cellMapper function is applied to each cell in the row, and the results are
         * collected into a List. This allows for strongly-typed data extraction and ensures
         * consistent processing of all cells in each row.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Extract only numeric values as Double
         * Function<Cell, Double> numericMapper = cell ->
         *     cell.getCellType() == CellType.NUMERIC
         *         ? cell.getNumericCellValue()
         *         : 0.0;
         *
         * Function<Row, List<Double>> mapper = RowMappers.toList(numericMapper);
         * List<List<Double>> numericData = ExcelUtil.readSheet(file, 0, true, mapper);
         * }</pre>
         *
         * @param <T> the type of objects to extract from cells
         * @param cellMapper function to convert each cell to type T
         * @return a Function that converts Row to List&lt;T&gt;
         */
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

    /**
     * Collection of row extractors for use with loadSheet methods.
     * Extractors process rows and populate output arrays for Dataset creation.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use default extractor
     * Dataset ds = ExcelUtil.loadSheet(file, 0, RowExtractors.DEFAULT);
     * 
     * // Create custom extractor for specific processing
     * TriConsumer<String[], Row, Object[]> customExtractor = 
     *     RowExtractors.create(cell -> cell.getStringCellValue().toUpperCase());
     * Dataset ds = ExcelUtil.loadSheet(file, 0, customExtractor);
     * }</pre>
     */
    public static final class RowExtractors {

        private RowExtractors() {
            // prevent instantiation
        }

        /**
         * Default row extractor that uses CELL_GETTER to extract cell values.
         * Suitable for general-purpose data extraction maintaining original data types.
         */
        public static final TriConsumer<String[], Row, Object[]> DEFAULT = create(CELL_GETTER);

        /**
         * Creates a custom row extractor with the specified cell mapping function.
         * The extractor processes each cell in a row and populates the output array with
         * transformed values. This is used with loadSheet methods to control how Excel
         * cell data is extracted and converted for Dataset creation.
         *
         * <p>The cell mapper function receives each Cell from the row and returns the
         * value to store in the output array. This enables custom type conversions,
         * default value handling, data validation, or any other per-cell transformation logic.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Extract all values as strings, with empty string for null cells
         * TriConsumer<String[], Row, Object[]> stringExtractor =
         *     RowExtractors.create(cell ->
         *         cell == null ? "" : CELL2STRING.apply(cell)
         *     );
         *
         * Dataset ds = ExcelUtil.loadSheet(file, "Sheet1", stringExtractor);
         * }</pre>
         *
         * @param cellMapper function to convert each cell to the desired output type
         * @return a TriConsumer that extracts row data using the cell mapper
         */
        public static TriConsumer<String[], Row, Object[]> create(final Function<Cell, ?> cellMapper) {
            return (header, row, output) -> {
                int idx = 0;

                for (Cell cell : row) {
                    output[idx++] = cellMapper.apply(cell);
                }
            };
        }
    }

    /**
     * Configuration options for creating Excel sheets with specific formatting.
     * Uses the builder pattern to provide a fluent API for configuration.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * SheetCreateOptions options = SheetCreateOptions.builder()
     *     .autoSizeColumn(true)
     *     .freezePane(new FreezePane(1, 1))
     *     .autoFilterByFirstRow(true)
     *     .build();
     * 
     * ExcelUtil.writeSheet("Report", data, options, new File("report.xlsx"));
     * }</pre>
     *
     * <p>Configuration options for creating Excel sheets with specific formatting and behavior.
     * This class provides various settings to control sheet creation including auto-sizing,
     * freeze panes, filtering, and cell styling.</p>
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class SheetCreateOptions {

        /**
         * Creates a new instance of SheetCreateOptions with default values.
         * All boolean flags are initialized to false and object references to null.
         */
        public SheetCreateOptions() {
        }

        /**
         * Whether to automatically size columns to fit their content.
         * When {@code true}, columns will be resized after all data is written.
         */
        private boolean autoSizeColumn;

        /**
         * Freeze pane configuration specifying which rows/columns to freeze.
         * Takes precedence over freezeFirstRow if both are set.
         */
        private FreezePane freezePane;

        /**
         * Whether to freeze the first row (typically headers).
         * Ignored if freezePane is specified.
         */
        private boolean freezeFirstRow;

        /**
         * Cell range for applying auto-filter.
         * Takes precedence over autoFilterByFirstRow if both are set.
         */
        private CellRangeAddress autoFilter;

        /**
         * Whether to apply auto-filter to the first row.
         * Creates filter dropdowns for all columns in the header row.
         */
        private boolean autoFilterByFirstRow;
    }

    /**
     * Represents freeze pane configuration for Excel sheets.
     * Specifies the number of columns and rows to freeze from the top-left corner.
     * 
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Freeze first column and first row
     * FreezePane freezeHeaders = new FreezePane(1, 1);
     * 
     * // Freeze first two columns only
     * FreezePane freezeColumns = new FreezePane(2, 0);
     * }</pre>
     * 
     * @param colSplit number of columns to freeze (0 for none)
     * @param rowSplit number of rows to freeze (0 for none)
     */
    public record FreezePane(int colSplit, int rowSplit) {
    }
}
