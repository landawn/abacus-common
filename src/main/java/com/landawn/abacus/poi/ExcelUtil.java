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
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Function;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import com.landawn.abacus.exception.UncheckedException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.BufferedCsvWriter;
import com.landawn.abacus.util.CsvUtil;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.RowDataset;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Utility class for reading, writing, and converting Excel files using Apache POI.
 * Supports both XLS (Excel 97-2003) and XLSX (Excel 2007+) formats.
 *
 * <p>The primary operations are:
 * <ul>
 *   <li>{@link #loadDatasetFromSheet(File)} reads a sheet into a {@link Dataset} for column-oriented access</li>
 *   <li>{@link #readRowsFromSheet(File)} reads a sheet as row lists, or maps rows to caller-defined objects</li>
 *   <li>{@link #streamRowsFromSheet(File, int, boolean)} iterates over rows through a closeable {@link Stream}</li>
 *   <li>{@link #writeRowsToSheet(String, List, List, File)} writes explicit headers and row collections</li>
 *   <li>{@link #writeDatasetToSheet(String, Dataset, File)} writes a {@link Dataset} with its column names as headers</li>
 *   <li>{@link #exportSheetToCsv(File, int, File)} exports a sheet to comma-separated values</li>
 * </ul>
 *
 * <p>Cell values are extracted using the bundled {@link #CELL_GETTER} (type-preserving) or
 * {@link #CELL_TO_STRING} (string-normalizing) functions, or via custom functions supplied
 * through {@link RowExtractors} and {@link RowMappers}.
 *
 * <p>Cell type mapping used by the type-preserving {@link #CELL_GETTER} (and the {@code DEFAULT}
 * extractor/mapper based on it):
 * <ul>
 *   <li>STRING  → {@code String}</li>
 *   <li>NUMERIC → {@code Double}</li>
 *   <li>BOOLEAN → {@code Boolean}</li>
 *   <li>FORMULA → formula text as {@code String} (not the evaluated result)</li>
 *   <li>BLANK / ERROR → empty {@code String}</li>
 * </ul>
 * <p>{@link #CELL_TO_STRING} uses the same mapping except that NUMERIC and BOOLEAN cells are
 * normalized to their {@code String} representation.
 *
 * <p><b>Sources and sinks:</b> the {@code loadDatasetFromSheet}, {@code readRowsFromSheet}, and
 * {@code streamRowsFromSheet} readers accept an Excel source as a {@link File}, an
 * {@link InputStream}, or a {@link Path}; writers accept a {@link File}, an {@link OutputStream}
 * (with an explicit {@link ExcelFormat}), or a {@link Path}. {@code exportSheetToCsv} reads its
 * Excel source from a {@link File} only (no {@code InputStream}/{@code Path} overloads), writing the
 * resulting CSV to a {@link File} or a {@link Writer}. {@code File}/{@code Path} writers infer the workbook
 * format (XLS vs XLSX) from the filename extension (anything other than {@code .xls} produces an
 * XLSX/XSSF workbook), whereas the {@code OutputStream} writers require the format to be passed
 * explicitly. {@code InputStream}/{@code OutputStream} overloads do not close the supplied stream;
 * the caller owns it. {@code File}/{@code Path} overloads manage their own file resources internally.
 *
 * <p><b>Header-handling divergence between read families:</b> the {@code loadDatasetFromSheet}
 * family always consumes row 0 as the header row and uses those cell values as the {@link Dataset}
 * column names (synthesizing {@code "Column_i"} for blank/empty header cells), and therefore has no
 * {@code skipFirstRow} parameter. In contrast, the {@code readRowsFromSheet} and
 * {@code streamRowsFromSheet} families never interpret row 0 as headers; they expose a raw
 * {@code skipFirstRow} boolean and simply discard the first row when it is {@code true}. Choose
 * {@code loadDatasetFromSheet} when the first row holds column names, and the row/stream families
 * for header-agnostic row processing.
 *
 * <p>Streams returned by {@code streamRowsFromSheet} carry an {@code onClose()} handler that closes
 * the underlying workbook (and, for {@code File}/{@code Path} sources, the underlying input stream);
 * always use them in a try-with-resources block. CSV overloads that accept a {@link Writer} flush
 * the writer but leave closing it to the caller.
 *
 * <p><b>Limitations:</b> the write methods build the entire workbook in memory before persisting it
 * (no SXSSF streaming-write variant), so very large exports may exhaust memory.
 * {@code writeRowsToSheet} writes each data row using that row's own size, so rows wider or narrower
 * than {@code headers} are emitted ragged (no padding or truncation to the header column count); the
 * {@code Dataset} overloads are rectangular by construction. The {@code exportSheetToCsv} File-sink
 * overloads use whatever charset {@link IOUtil#newFileWriter(File)} selects; supply a {@link Writer}
 * (e.g. an {@link java.io.OutputStreamWriter} over a chosen {@link java.nio.charset.Charset}) for
 * explicit encoding control.
 *
 * <p>I/O errors are rethrown as {@link UncheckedException} or
 * {@link com.landawn.abacus.exception.UncheckedIOException}. A missing sheet name causes
 * {@code IllegalArgumentException}. All methods are stateless and thread-safe.
 *
 * <p><b>Usage Examples:</b>
 * <pre>{@code
 * // Load the first sheet into a Dataset
 * Dataset firstSheet = ExcelUtil.loadDatasetFromSheet(new File("sales_data.xlsx"));
 *
 * // Load a named sheet with a custom row extractor
 * Dataset salesSheet = ExcelUtil.loadDatasetFromSheet(
 *     new File("report.xlsx"), "Q1_Sales", ExcelUtil.RowExtractors.DEFAULT);
 *
 * // Write a Dataset to Excel with formatting
 * ExcelUtil.SheetCreateOptions options = ExcelUtil.SheetCreateOptions.builder()
 *     .autoSizeColumn(true)
 *     .freezeFirstRow(true)
 *     .autoFilterByFirstRow(true)
 *     .build();
 * ExcelUtil.writeDatasetToSheet("Sales Report", salesSheet, options, new File("output.xlsx"));
 *
 * // Convert the first sheet to CSV
 * ExcelUtil.exportSheetToCsv(new File("data.xlsx"), 0, new File("data.csv"));
 *
 * // Stream rows for functional processing
 * try (Stream<Row> stream = ExcelUtil.streamRowsFromSheet(new File("large.xlsx"), 0, true)) {
 *     stream.filter(row -> row.getCell(0).getNumericCellValue() > 1000)
 *           .forEach(row -> processRow(row));
 * }
 * }</pre>
 *
 * @see Dataset
 * @see CsvUtil
 * @see RowMappers
 * @see RowExtractors
 * @see SheetCreateOptions
 * @see FreezePane
 * @see org.apache.poi.ss.usermodel.Workbook
 * @see org.apache.poi.ss.usermodel.Sheet
 * @see org.apache.poi.ss.usermodel.Row
 * @see org.apache.poi.ss.usermodel.Cell
 * @see <a href="https://poi.apache.org/components/spreadsheet/">Apache POI Spreadsheet API</a>
 */
public final class ExcelUtil {

    /**
     * Default cell value extractor function that retrieves cell values based on their type.
     * This function provides automatic type-preserving extraction, returning appropriate Java
     * objects based on the cell's actual type: {@code String} for STRING cells, {@code Double} for
     * NUMERIC cells, {@code Boolean} for BOOLEAN cells, {@code String} for FORMULA cells (returns
     * formula text), and an empty {@code String} for BLANK and ERROR cells.
     *
     * <p>This extractor is commonly used as the default cell processor for reading Excel data
     * when you want to preserve the original cell types without conversion. It's ideal for
     * generic data loading scenarios where type preservation is important.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use directly on a cell
     * Cell cell = row.getCell(0);
     * Object value = ExcelUtil.CELL_GETTER.apply(cell);
     *
     * // Use with row mapper
     * Function<Row, List<Object>> mapper = ExcelUtil.RowMappers.toList(ExcelUtil.CELL_GETTER);
     * List<List<Object>> rows = ExcelUtil.readRowsFromSheet(file, 0, true, mapper);
     * }</pre>
     *
     */
    public static final Function<Cell, Object> CELL_GETTER = cell -> switch (cell.getCellType()) {
        case STRING -> cell.getStringCellValue();
        case NUMERIC -> cell.getNumericCellValue();
        case BOOLEAN -> cell.getBooleanCellValue();
        case FORMULA -> cell.getCellFormula();
        case BLANK -> "";
        case ERROR -> "";
        default -> throw new RuntimeException("Unsupported cell type: " + cell.getCellType());
    };

    /**
     * Cell to String converter function that converts any cell value to its string representation.
     * This function normalizes all cell types to {@code String}, making it useful for text-based
     * processing, CSV conversion, or when uniform string representation is required. Handles all
     * standard cell types including STRING, NUMERIC, BOOLEAN, FORMULA, BLANK, and ERROR cells.
     *
     * <p>Numeric values are converted using {@code String.valueOf()}, boolean values are converted
     * to "true" or "false", formula cells return the formula text (not the evaluated result), and
     * blank and error cells return an empty string. This converter is ideal for export operations and
     * text-based data processing where type information is not critical.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use directly on a cell
     * Cell cell = row.getCell(0);
     * String stringValue = ExcelUtil.CELL_TO_STRING.apply(cell);
     *
     * // Use with row mapper to get all values as strings
     * Function<Row, List<String>> stringMapper = ExcelUtil.RowMappers.toList(ExcelUtil.CELL_TO_STRING);
     * List<List<String>> rows = ExcelUtil.readRowsFromSheet(file, 0, true, stringMapper);
     * }</pre>
     *
     */
    public static final Function<Cell, String> CELL_TO_STRING = cell -> switch (cell.getCellType()) {
        case STRING -> cell.getStringCellValue();
        case NUMERIC -> String.valueOf(cell.getNumericCellValue());
        case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
        case FORMULA -> cell.getCellFormula();
        case BLANK -> "";
        case ERROR -> "";
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
     * <p><b>Header handling:</b> the {@code loadDatasetFromSheet} family always consumes row 0 as the
     * header row and uses those cell values as column names (synthesizing {@code "Column_i"} for
     * blank/empty header cells); it has no {@code skipFirstRow} parameter. This differs from the
     * {@link #readRowsFromSheet(File, int, boolean, Function)} and
     * {@link #streamRowsFromSheet(File, int, boolean)} families, which never interpret row 0 as
     * headers and instead expose a raw {@code skipFirstRow} boolean. See the class-level
     * documentation for details.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dataset dataset = ExcelUtil.loadDatasetFromSheet(new File("sales_data.xlsx"));
     * // Access data by column name
     * List<String> productNames = dataset.getColumn("Product");
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file.
     * @return a Dataset containing the sheet data with the first row as column names, or an empty Dataset if the sheet is empty.
     * @throws UncheckedException if an I/O error occurs while reading the file, or if the file is not a valid Excel file.
     */
    public static Dataset loadDatasetFromSheet(final File excelFile) {
        return loadDatasetFromSheet(excelFile, 0, RowExtractors.DEFAULT);
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
     * Dataset dataset = ExcelUtil.loadDatasetFromSheet(new File("data.xlsx"), 0, customExtractor);
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file.
     * @param sheetIndex the zero-based index of the sheet to read (0 for first sheet).
     * @param rowExtractor custom function to extract row data. Receives three parameters:
     *                     column headers array, current row, and output array to populate with extracted values.
     * @return a Dataset containing the extracted sheet data with the first row as column names.
     * @throws UncheckedException if an I/O error occurs while reading the file, or if the file is not a valid Excel file.
     * @throws IllegalArgumentException if the sheet index is out of bounds.
     */
    public static Dataset loadDatasetFromSheet(final File excelFile, final int sheetIndex,
            final TriConsumer<? super String[], ? super Row, ? super Object[]> rowExtractor) {
        try (InputStream is = new FileInputStream(excelFile)) {
            return loadDatasetFromSheet(is, sheetIndex, rowExtractor);
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Loads the specified sheet from the Excel input stream into a Dataset using a custom row extractor.
     * This overload reads from an arbitrary {@link InputStream} (e.g. a classpath resource, an uploaded
     * file, or an in-memory {@code byte[]}) instead of a {@link File}. The stream is read fully but
     * <b>not closed</b> by this method; the caller retains ownership of it. The first row is always
     * treated as column headers.
     *
     * @param excelInputStream the input stream of the Excel content, must be a valid Excel stream. It is not closed by this method.
     * @param sheetIndex the zero-based index of the sheet to read (0 for first sheet).
     * @param rowExtractor custom function to extract row data. Receives three parameters:
     *                     column headers array, current row, and output array to populate with extracted values.
     * @return a Dataset containing the extracted sheet data with the first row as column names.
     * @throws UncheckedException if an I/O error occurs while reading the stream, or if the content is not a valid Excel stream.
     * @throws IllegalArgumentException if the sheet index is out of bounds.
     */
    public static Dataset loadDatasetFromSheet(final InputStream excelInputStream, final int sheetIndex,
            final TriConsumer<? super String[], ? super Row, ? super Object[]> rowExtractor) {
        try (Workbook workbook = WorkbookFactory.create(excelInputStream)) {
            return loadDatasetFromSheet(workbook.getSheetAt(sheetIndex), rowExtractor);
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Loads the specified sheet from the Excel file located at the given path into a Dataset using a custom row extractor.
     * This is a thin {@link Path}-based delegate to {@link #loadDatasetFromSheet(File, int, TriConsumer)}.
     * The first row is always treated as column headers.
     *
     * @param excelPath the path of the Excel file to read, must exist and be a valid Excel file.
     * @param sheetIndex the zero-based index of the sheet to read (0 for first sheet).
     * @param rowExtractor custom function to extract row data. Receives three parameters:
     *                     column headers array, current row, and output array to populate with extracted values.
     * @return a Dataset containing the extracted sheet data with the first row as column names.
     * @throws UncheckedException if an I/O error occurs while reading the file, or if the file is not a valid Excel file.
     * @throws IllegalArgumentException if the sheet index is out of bounds.
     */
    public static Dataset loadDatasetFromSheet(final Path excelPath, final int sheetIndex,
            final TriConsumer<? super String[], ? super Row, ? super Object[]> rowExtractor) {
        return loadDatasetFromSheet(excelPath.toFile(), sheetIndex, rowExtractor);
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
     * Dataset dataset = ExcelUtil.loadDatasetFromSheet(
     *     new File("workbook.xlsx"),
     *     "Sales2024",
     *     ExcelUtil.RowExtractors.DEFAULT
     * );
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file.
     * @param sheetName the name of the sheet to read, case-sensitive.
     * @param rowExtractor custom function to extract row data. Receives three parameters:
     *                     column headers array, current row, and output array to populate with extracted values.
     * @return a Dataset containing the extracted sheet data with the first row as column names.
     * @throws UncheckedException if an I/O error occurs or if the file is not a valid Excel file.
     * @throws IllegalArgumentException if the sheet name is not found in the workbook.
     */
    public static Dataset loadDatasetFromSheet(final File excelFile, final String sheetName,
            final TriConsumer<? super String[], ? super Row, ? super Object[]> rowExtractor) {
        try (InputStream is = new FileInputStream(excelFile)) {
            return loadDatasetFromSheet(is, sheetName, rowExtractor);
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Loads the sheet with the specified name from the Excel input stream into a Dataset.
     * This overload reads from an arbitrary {@link InputStream} (e.g. a classpath resource, an uploaded
     * file, or an in-memory {@code byte[]}) instead of a {@link File}. The stream is read fully but
     * <b>not closed</b> by this method; the caller retains ownership of it. Uses a custom row extractor
     * to process each row after the header row.
     *
     * @param excelInputStream the input stream of the Excel content, must be a valid Excel stream. It is not closed by this method.
     * @param sheetName the name of the sheet to read, case-sensitive.
     * @param rowExtractor custom function to extract row data. Receives three parameters:
     *                     column headers array, current row, and output array to populate with extracted values.
     * @return a Dataset containing the extracted sheet data with the first row as column names.
     * @throws UncheckedException if an I/O error occurs or if the content is not a valid Excel stream.
     * @throws IllegalArgumentException if the sheet name is not found in the workbook.
     */
    public static Dataset loadDatasetFromSheet(final InputStream excelInputStream, final String sheetName,
            final TriConsumer<? super String[], ? super Row, ? super Object[]> rowExtractor) {
        try (Workbook workbook = WorkbookFactory.create(excelInputStream)) {
            final Sheet sheet = getRequiredSheet(workbook, sheetName);
            return loadDatasetFromSheet(sheet, rowExtractor);
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Loads the sheet with the specified name from the Excel file located at the given path into a Dataset.
     * This is a thin {@link Path}-based delegate to {@link #loadDatasetFromSheet(File, String, TriConsumer)}.
     *
     * @param excelPath the path of the Excel file to read, must exist and be a valid Excel file.
     * @param sheetName the name of the sheet to read, case-sensitive.
     * @param rowExtractor custom function to extract row data. Receives three parameters:
     *                     column headers array, current row, and output array to populate with extracted values.
     * @return a Dataset containing the extracted sheet data with the first row as column names.
     * @throws UncheckedException if an I/O error occurs or if the file is not a valid Excel file.
     * @throws IllegalArgumentException if the sheet name is not found in the workbook.
     */
    public static Dataset loadDatasetFromSheet(final Path excelPath, final String sheetName,
            final TriConsumer<? super String[], ? super Row, ? super Object[]> rowExtractor) {
        return loadDatasetFromSheet(excelPath.toFile(), sheetName, rowExtractor);
    }

    private static Sheet getRequiredSheet(final Workbook workbook, final String sheetName) {
        final Sheet sheet = workbook.getSheet(sheetName);

        if (sheet == null) {
            throw new IllegalArgumentException("Sheet not found: " + sheetName);
        }

        return sheet;
    }

    @SuppressWarnings("deprecation")
    private static Dataset loadDatasetFromSheet(final Sheet sheet, final TriConsumer<? super String[], ? super Row, ? super Object[]> rowExtractor) {
        final Iterator<Row> rowIter = sheet.rowIterator();

        if (!rowIter.hasNext()) {
            return Dataset.empty();
        }

        final Row headerRow = rowIter.next();
        final int columnCount = Math.max(headerRow.getLastCellNum(), 0);
        final String[] headers = new String[columnCount];

        for (int i = 0; i < columnCount; i++) {
            final Cell cell = headerRow.getCell(i);
            final String name = cell == null ? "" : CELL_TO_STRING.apply(cell);
            headers[i] = name.isEmpty() ? "Column_" + i : name;
        }

        final List<List<Object>> columnList = new ArrayList<>(columnCount);

        for (int i = 0; i < columnCount; i++) {
            columnList.add(new ArrayList<>());
        }

        final Object[] output = new Object[columnCount];

        while (rowIter.hasNext()) {
            Arrays.fill(output, null);
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
     * <p>Unlike loadDatasetFromSheet methods, this does not create a Dataset structure and instead
     * provides direct access to row data. Useful for simple row-by-row processing without
     * column-based access requirements.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<List<Object>> rows = ExcelUtil.readRowsFromSheet(new File("data.xlsx"));
     * for (List<Object> row : rows) {
     *     System.out.println("Row: " + row);
     * }
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file.
     * @return a list of rows, where each row is a list of cell values with preserved types.
     * @throws UncheckedException if an I/O error occurs while reading the file or if the file is not a valid Excel file.
     */
    public static List<List<Object>> readRowsFromSheet(final File excelFile) {
        return readRowsFromSheet(excelFile, 0, false, RowMappers.DEFAULT);
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
     * <p><b>Header handling:</b> unlike the {@link #loadDatasetFromSheet(File, int, TriConsumer)}
     * family (which always treats row 0 as headers and derives column names from it), this family
     * never interprets row 0 as headers. The {@code skipFirstRow} flag merely discards the first row
     * when {@code true}; its cell values are not used as names. See the class-level documentation for
     * details.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Function<Row, String> rowToString = row -> {
     *     StringBuilder sb = new StringBuilder();
     *     row.forEach(cell -> sb.append(cell).append(","));
     *     return sb.toString();
     * };
     * List<String> rows = ExcelUtil.readRowsFromSheet(new File("data.xlsx"), 0, true, rowToString);
     * }</pre>
     *
     * @param <T> the type of objects to map rows to.
     * @param excelFile the Excel file to read, must exist and be a valid Excel file.
     * @param sheetIndex the zero-based index of the sheet to read (0 for first sheet).
     * @param skipFirstRow {@code true} to skip the first row (typically headers), {@code false} to process all rows.
     * @param rowMapper function to convert each Row to an object of type T.
     * @return a list of mapped objects, one per row (excluding skipped rows).
     * @throws UncheckedException if an I/O error occurs while reading the file, or if the file is not a valid Excel file.
     * @throws IllegalArgumentException if the sheet index is out of bounds.
     */
    public static <T> List<T> readRowsFromSheet(final File excelFile, final int sheetIndex, final boolean skipFirstRow,
            final Function<? super Row, ? extends T> rowMapper) {
        try (InputStream is = new FileInputStream(excelFile)) {
            return readRowsFromSheet(is, sheetIndex, skipFirstRow, rowMapper);
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Reads the specified sheet from the Excel input stream and maps each row to a custom object type.
     * This overload reads from an arbitrary {@link InputStream} (e.g. a classpath resource, an uploaded
     * file, or an in-memory {@code byte[]}) instead of a {@link File}. The stream is read fully but
     * <b>not closed</b> by this method; the caller retains ownership of it.
     *
     * <p>Unlike the {@code loadDatasetFromSheet} family, row 0 is never interpreted as a header row;
     * the {@code skipFirstRow} flag simply discards the first row when {@code true}.</p>
     *
     * @param <T> the type of objects to map rows to.
     * @param excelInputStream the input stream of the Excel content, must be a valid Excel stream. It is not closed by this method.
     * @param sheetIndex the zero-based index of the sheet to read (0 for first sheet).
     * @param skipFirstRow {@code true} to skip the first row (typically headers), {@code false} to process all rows.
     * @param rowMapper function to convert each Row to an object of type T.
     * @return a list of mapped objects, one per row (excluding skipped rows).
     * @throws UncheckedException if an I/O error occurs while reading the stream, or if the content is not a valid Excel stream.
     * @throws IllegalArgumentException if the sheet index is out of bounds.
     */
    public static <T> List<T> readRowsFromSheet(final InputStream excelInputStream, final int sheetIndex, final boolean skipFirstRow,
            final Function<? super Row, ? extends T> rowMapper) {
        try (Workbook workbook = WorkbookFactory.create(excelInputStream)) {
            return readRowsFromSheet(workbook.getSheetAt(sheetIndex), skipFirstRow, rowMapper);
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Reads the specified sheet from the Excel file located at the given path and maps each row to a custom object type.
     * This is a thin {@link Path}-based delegate to {@link #readRowsFromSheet(File, int, boolean, Function)}.
     *
     * @param <T> the type of objects to map rows to.
     * @param excelPath the path of the Excel file to read, must exist and be a valid Excel file.
     * @param sheetIndex the zero-based index of the sheet to read (0 for first sheet).
     * @param skipFirstRow {@code true} to skip the first row (typically headers), {@code false} to process all rows.
     * @param rowMapper function to convert each Row to an object of type T.
     * @return a list of mapped objects, one per row (excluding skipped rows).
     * @throws UncheckedException if an I/O error occurs while reading the file, or if the file is not a valid Excel file.
     * @throws IllegalArgumentException if the sheet index is out of bounds.
     */
    public static <T> List<T> readRowsFromSheet(final Path excelPath, final int sheetIndex, final boolean skipFirstRow,
            final Function<? super Row, ? extends T> rowMapper) {
        return readRowsFromSheet(excelPath.toFile(), sheetIndex, skipFirstRow, rowMapper);
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
     * List<Product> products = ExcelUtil.readRowsFromSheet(
     *     new File("products.xlsx"),
     *     "ProductList",
     *     true,
     *     rowToProduct
     * );
     * }</pre>
     *
     * @param <T> the type of objects to map rows to.
     * @param excelFile the Excel file to read, must exist and be a valid Excel file.
     * @param sheetName the name of the sheet to read, case-sensitive.
     * @param skipFirstRow {@code true} to skip the first row (typically headers), {@code false} to process all rows.
     * @param rowMapper function to convert each Row to an object of type T.
     * @return a list of mapped objects, one per row (excluding skipped rows).
     * @throws UncheckedException if an I/O error occurs or if the file is not a valid Excel file.
     * @throws IllegalArgumentException if the sheet name is not found in the workbook.
     */
    public static <T> List<T> readRowsFromSheet(final File excelFile, final String sheetName, final boolean skipFirstRow,
            final Function<? super Row, ? extends T> rowMapper) {
        try (InputStream is = new FileInputStream(excelFile)) {
            return readRowsFromSheet(is, sheetName, skipFirstRow, rowMapper);
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Reads the sheet with the specified name from the Excel input stream and maps each row to a custom object type.
     * This overload reads from an arbitrary {@link InputStream} (e.g. a classpath resource, an uploaded
     * file, or an in-memory {@code byte[]}) instead of a {@link File}. The stream is read fully but
     * <b>not closed</b> by this method; the caller retains ownership of it.
     *
     * <p>Unlike the {@code loadDatasetFromSheet} family, row 0 is never interpreted as a header row;
     * the {@code skipFirstRow} flag simply discards the first row when {@code true}.</p>
     *
     * @param <T> the type of objects to map rows to.
     * @param excelInputStream the input stream of the Excel content, must be a valid Excel stream. It is not closed by this method.
     * @param sheetName the name of the sheet to read, case-sensitive.
     * @param skipFirstRow {@code true} to skip the first row (typically headers), {@code false} to process all rows.
     * @param rowMapper function to convert each Row to an object of type T.
     * @return a list of mapped objects, one per row (excluding skipped rows).
     * @throws UncheckedException if an I/O error occurs or if the content is not a valid Excel stream.
     * @throws IllegalArgumentException if the sheet name is not found in the workbook.
     */
    public static <T> List<T> readRowsFromSheet(final InputStream excelInputStream, final String sheetName, final boolean skipFirstRow,
            final Function<? super Row, ? extends T> rowMapper) {
        try (Workbook workbook = WorkbookFactory.create(excelInputStream)) {
            final Sheet sheet = getRequiredSheet(workbook, sheetName);
            return readRowsFromSheet(sheet, skipFirstRow, rowMapper);
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Reads the sheet with the specified name from the Excel file located at the given path and maps each row to a custom object type.
     * This is a thin {@link Path}-based delegate to {@link #readRowsFromSheet(File, String, boolean, Function)}.
     *
     * @param <T> the type of objects to map rows to.
     * @param excelPath the path of the Excel file to read, must exist and be a valid Excel file.
     * @param sheetName the name of the sheet to read, case-sensitive.
     * @param skipFirstRow {@code true} to skip the first row (typically headers), {@code false} to process all rows.
     * @param rowMapper function to convert each Row to an object of type T.
     * @return a list of mapped objects, one per row (excluding skipped rows).
     * @throws UncheckedException if an I/O error occurs or if the file is not a valid Excel file.
     * @throws IllegalArgumentException if the sheet name is not found in the workbook.
     */
    public static <T> List<T> readRowsFromSheet(final Path excelPath, final String sheetName, final boolean skipFirstRow,
            final Function<? super Row, ? extends T> rowMapper) {
        return readRowsFromSheet(excelPath.toFile(), sheetName, skipFirstRow, rowMapper);
    }

    private static <T> List<T> readRowsFromSheet(final Sheet sheet, final boolean skipFirstRow, final Function<? super Row, ? extends T> rowMapper) {
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
     * This method provides a convenient Stream-based API for processing rows, supporting
     * functional-style operations like filtering, mapping, and reduction.
     *
     * <p><b>Note:</b> The entire workbook is still loaded into memory via {@code WorkbookFactory.create()}.
     * The "streaming" refers to iterating over the already-loaded rows via Stream API, not to
     * memory-efficient incremental parsing. For truly memory-efficient streaming of very large files,
     * consider using Apache POI's SAX-based or SXSSF-based APIs directly.</p>
     *
     * <p><strong>Important:</strong> The Stream must be closed after use to release file handles
     * and workbook resources. Always use try-with-resources or explicitly call {@code close()}
     * on the stream when done.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Stream<Row> stream = ExcelUtil.streamRowsFromSheet(new File("large_data.xlsx"), 0, true)) {
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
     * @throws IllegalArgumentException if the sheet index is out of bounds
     */
    public static Stream<Row> streamRowsFromSheet(final File excelFile, final int sheetIndex, final boolean skipFirstRow) {
        InputStream is = null;
        Stream<Row> result = null;

        try {
            is = new FileInputStream(excelFile);
            // streamFromSource takes ownership of the FileInputStream and closes it via onClose.
            result = streamFromSource(is, true, sheetIndex, null, skipFirstRow);
            return result;
        } catch (IOException e) {
            throw new UncheckedException(e);
        } finally {
            if (result == null) {
                IOUtil.closeQuietly(is);
            }
        }
    }

    /**
     * Creates a stream of Row objects from the specified sheet in the Excel input stream.
     * This overload reads from an arbitrary {@link InputStream} (e.g. a classpath resource, an uploaded
     * file, or an in-memory {@code byte[]}) instead of a {@link File}.
     *
     * <p><b>Note:</b> The entire workbook is still loaded into memory via {@code WorkbookFactory.create()}.
     * The "streaming" refers to iterating over the already-loaded rows via Stream API, not to
     * memory-efficient incremental parsing.</p>
     *
     * <p><strong>Important:</strong> The returned Stream must be closed after use. Closing the Stream
     * closes the underlying workbook but <b>does not</b> close the supplied {@code excelInputStream};
     * the caller retains ownership of the stream and must close it (typically after the Stream).
     * Unlike the {@code loadDatasetFromSheet} family, row 0 is never interpreted as a header row; the
     * {@code skipFirstRow} flag simply discards the first row when {@code true}.</p>
     *
     * @param excelInputStream the input stream of the Excel content, must be a valid Excel stream. It is not closed by the returned Stream.
     * @param sheetIndex the zero-based index of the sheet to stream (0 for first sheet)
     * @param skipFirstRow {@code true} to skip the first row (typically headers), {@code false} to process all rows
     * @return a Stream of Row objects from the specified sheet that must be closed after use
     * @throws UncheckedException if an I/O error occurs while reading the stream or if the content is not a valid Excel stream
     * @throws IllegalArgumentException if the sheet index is out of bounds
     */
    public static Stream<Row> streamRowsFromSheet(final InputStream excelInputStream, final int sheetIndex, final boolean skipFirstRow) {
        return streamFromSource(excelInputStream, false, sheetIndex, null, skipFirstRow);
    }

    /**
     * Creates a stream of Row objects from the specified sheet in the Excel file located at the given path.
     * This is a thin {@link Path}-based delegate to {@link #streamRowsFromSheet(File, int, boolean)}.
     * The returned Stream must be closed after use.
     *
     * @param excelPath the path of the Excel file to read, must exist and be a valid Excel file
     * @param sheetIndex the zero-based index of the sheet to stream (0 for first sheet)
     * @param skipFirstRow {@code true} to skip the first row (typically headers), {@code false} to process all rows
     * @return a Stream of Row objects from the specified sheet that must be closed after use
     * @throws UncheckedException if an I/O error occurs while reading the file or if the file is not a valid Excel file
     * @throws IllegalArgumentException if the sheet index is out of bounds
     */
    public static Stream<Row> streamRowsFromSheet(final Path excelPath, final int sheetIndex, final boolean skipFirstRow) {
        return streamRowsFromSheet(excelPath.toFile(), sheetIndex, skipFirstRow);
    }

    /**
     * Creates a stream of Row objects from the sheet with the specified name.
     * This method combines named sheet access with Stream-based row iteration, enabling
     * functional-style processing of specific worksheets. Particularly useful for
     * multi-sheet workbooks where only certain sheets need to be processed.
     *
     * <p><b>Note:</b> The entire workbook is still loaded into memory via {@code WorkbookFactory.create()}.
     * The Stream API provides convenient iteration and early termination support, but does not
     * reduce memory usage compared to {@code loadDatasetFromSheet()}.</p>
     *
     * <p><strong>Important:</strong> The Stream must be closed after use to release file handles
     * and workbook resources. Always use try-with-resources or explicitly call {@code close()}
     * on the stream when done.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (Stream<Row> stream = ExcelUtil.streamRowsFromSheet(new File("data.xlsx"), "Sales", true)) {
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
     * @throws UncheckedException if an I/O error occurs or if the file is not a valid Excel file
     * @throws IllegalArgumentException if the sheet name is not found in the workbook
     */
    public static Stream<Row> streamRowsFromSheet(final File excelFile, final String sheetName, final boolean skipFirstRow) {
        InputStream is = null;
        Stream<Row> result = null;

        try {
            is = new FileInputStream(excelFile);
            // streamFromSource takes ownership of the FileInputStream and closes it via onClose.
            result = streamFromSource(is, true, -1, sheetName, skipFirstRow);
            return result;
        } catch (IOException e) {
            throw new UncheckedException(e);
        } finally {
            if (result == null) {
                IOUtil.closeQuietly(is);
            }
        }
    }

    /**
     * Creates a stream of Row objects from the sheet with the specified name in the Excel input stream.
     * This overload reads from an arbitrary {@link InputStream} (e.g. a classpath resource, an uploaded
     * file, or an in-memory {@code byte[]}) instead of a {@link File}.
     *
     * <p><strong>Important:</strong> The returned Stream must be closed after use. Closing the Stream
     * closes the underlying workbook but <b>does not</b> close the supplied {@code excelInputStream};
     * the caller retains ownership of the stream and must close it (typically after the Stream).
     * Unlike the {@code loadDatasetFromSheet} family, row 0 is never interpreted as a header row; the
     * {@code skipFirstRow} flag simply discards the first row when {@code true}.</p>
     *
     * @param excelInputStream the input stream of the Excel content, must be a valid Excel stream. It is not closed by the returned Stream.
     * @param sheetName the name of the sheet to stream, case-sensitive
     * @param skipFirstRow {@code true} to skip the first row (typically headers), {@code false} to process all rows
     * @return a Stream of Row objects from the specified sheet that must be closed after use
     * @throws UncheckedException if an I/O error occurs or if the content is not a valid Excel stream
     * @throws IllegalArgumentException if the sheet name is not found in the workbook
     */
    public static Stream<Row> streamRowsFromSheet(final InputStream excelInputStream, final String sheetName, final boolean skipFirstRow) {
        return streamFromSource(excelInputStream, false, -1, sheetName, skipFirstRow);
    }

    /**
     * Creates a stream of Row objects from the sheet with the specified name in the Excel file located at the given path.
     * This is a thin {@link Path}-based delegate to {@link #streamRowsFromSheet(File, String, boolean)}.
     * The returned Stream must be closed after use.
     *
     * @param excelPath the path of the Excel file to read, must exist and be a valid Excel file
     * @param sheetName the name of the sheet to stream, case-sensitive
     * @param skipFirstRow {@code true} to skip the first row (typically headers), {@code false} to process all rows
     * @return a Stream of Row objects from the specified sheet that must be closed after use
     * @throws UncheckedException if an I/O error occurs or if the file is not a valid Excel file
     * @throws IllegalArgumentException if the sheet name is not found in the workbook
     */
    public static Stream<Row> streamRowsFromSheet(final Path excelPath, final String sheetName, final boolean skipFirstRow) {
        return streamRowsFromSheet(excelPath.toFile(), sheetName, skipFirstRow);
    }

    /**
     * Shared worker that opens a workbook from the given input stream and returns a row Stream whose
     * {@code onClose} handler closes the workbook (and, when {@code closeInputStream} is {@code true},
     * the supplied input stream as well). Exactly one of {@code sheetIndex >= 0} or {@code sheetName != null}
     * selects the sheet. If wiring fails, the workbook (and, when owned, the stream) is closed before
     * rethrowing so nothing leaks.
     */
    private static Stream<Row> streamFromSource(final InputStream is, final boolean closeInputStream, final int sheetIndex, final String sheetName,
            final boolean skipFirstRow) {
        Workbook workbook = null;
        Stream<Row> result = null;

        try {
            workbook = WorkbookFactory.create(is);
            final Sheet sheet = sheetName != null ? getRequiredSheet(workbook, sheetName) : workbook.getSheetAt(sheetIndex);
            final Workbook workbookToClose = workbook;

            result = Stream.of(sheet.rowIterator()).skip(skipFirstRow ? 1 : 0).onClose(() -> {
                try {
                    workbookToClose.close();
                } catch (IOException e) {
                    throw new UncheckedException(e);
                } finally {
                    if (closeInputStream) {
                        IOUtil.closeQuietly(is);
                    }
                }
            });

            return result;
        } catch (IOException e) {
            throw new UncheckedException(e);
        } finally {
            if (result == null) {
                IOUtil.closeQuietly(workbook);
                // Note: the caller's finally closes the input stream when it owns it.
            }
        }
    }

    /**
     * Writes data to a new Excel file with the specified sheet name and headers.
     * This is a convenience method that creates a basic Excel file without any special
     * formatting, auto-sizing, or freeze panes. The resulting file contains a single sheet
     * with the provided headers and data rows.
     *
     * <p>Cell values are automatically converted based on their Java types: String, Boolean,
     * Date, LocalDate, LocalDateTime, Calendar, Integer, and other Number types (set as double)
     * are set using type-specific cell methods, while other types are converted to strings.
     * {@code null} values result in blank cells.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Object> headers = List.of("Name", "Age", "City");
     * List<List<Object>> rows = List.of(
     *     List.of("John", 30, "New York"),
     *     List.of("Jane", 25, "London")
     * );
     * ExcelUtil.writeRowsToSheet("Users", headers, rows, new File("users.xlsx"));
     * }</pre>
     *
     * @param sheetName the name of the sheet to create in the workbook
     * @param headers the column headers as a list of objects (will be converted to strings)
     * @param rows the data rows, where each row is a collection of cell values
     * @param outputExcelFile the file to write the Excel data to (will be created or overwritten)
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created
     */
    public static void writeRowsToSheet(final String sheetName, final List<?> headers, final List<? extends Collection<?>> rows, final File outputExcelFile) {
        writeRowsToSheet(sheetName, headers, rows, (SheetCreateOptions) null, outputExcelFile);
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
     * List<Object> headers = List.of("ID", "Name", "Value");
     * List<List<Object>> data = List.of(
     *     List.of(1, "Item1", 100.0),
     *     List.of(2, "Item2", 200.0)
     * );
     * ExcelUtil.SheetCreateOptions options = ExcelUtil.SheetCreateOptions.builder()
     *     .autoSizeColumn(true)
     *     .freezeFirstRow(true)
     *     .autoFilterByFirstRow(true)
     *     .build();
     *
     * ExcelUtil.writeRowsToSheet("Report", headers, data, options, new File("report.xlsx"));
     * }</pre>
     *
     * @param sheetName the name of the sheet to create in the workbook
     * @param headers the column headers as a list of objects (will be converted to strings)
     * @param rows the data rows, where each row is a collection of cell values
     * @param sheetCreateOptions configuration options for sheet formatting (null to apply no formatting)
     * @param outputExcelFile the file to write the Excel data to (will be created or overwritten)
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created
     */
    public static void writeRowsToSheet(final String sheetName, final List<?> headers, final List<? extends Collection<?>> rows,
            final SheetCreateOptions sheetCreateOptions, final File outputExcelFile) {
        final int columnCount = headers.size();

        final Consumer<Sheet> sheetSetter = createSheetSetter(sheetCreateOptions, columnCount);

        writeRowsToSheet(sheetName, headers, rows, sheetSetter, outputExcelFile);
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
            } else if (sheetCreateOptions.isAutoFilterByFirstRow() && columnCount > 0) {
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
     * List<Object> headers = List.of("ID", "Name", "Amount", "Status");
     * List<List<Object>> rows = List.of(
     *     List.of(1, "North", 1250.0, "Open"),
     *     List.of(2, "South", 980.0, "Closed")
     * );
     * ExcelUtil.writeRowsToSheet("CustomSheet", headers, rows, customFormatter, new File("custom.xlsx"));
     * }</pre>
     *
     * @param sheetName the name of the sheet to create in the workbook.
     * @param headers the column headers as a list of objects (will be converted to strings).
     * @param rows the data rows, where each row is a collection of cell values.
     * @param sheetSetter a consumer to apply custom formatting to the sheet after data is written (null to skip).
     * @param outputExcelFile the file to write the Excel data to (will be created or overwritten).
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created.
     */
    public static void writeRowsToSheet(final String sheetName, final List<?> headers, final List<? extends Collection<?>> rows,
            final Consumer<? super Sheet> sheetSetter, final File outputExcelFile) {
        try (OutputStream os = new FileOutputStream(outputExcelFile)) {
            writeRowsToSheet(sheetName, headers, rows, sheetSetter, os, formatOf(outputExcelFile));
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Writes data to the given output stream as an Excel workbook of the specified {@link ExcelFormat}.
     * This overload lets callers write to an arbitrary {@link OutputStream} (e.g. an
     * {@code HttpServletResponse} output stream or an in-memory {@code ByteArrayOutputStream}) and
     * choose the workbook format explicitly instead of relying on filename-extension inference.
     *
     * <p>The supplied {@code outputStream} is flushed (via {@code workbook.write}) but <b>not closed</b>
     * by this method; the caller retains ownership of it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = response.getOutputStream()) {
     *     ExcelUtil.writeRowsToSheet("Users", headers, rows, (Consumer<Sheet>) null, os, ExcelUtil.ExcelFormat.XLSX);
     * }
     * }</pre>
     *
     * @param sheetName the name of the sheet to create in the workbook.
     * @param headers the column headers as a list of objects (will be converted to strings).
     * @param rows the data rows, where each row is a collection of cell values.
     * @param sheetSetter a consumer to apply custom formatting to the sheet after data is written (null to skip).
     * @param outputStream the stream to write the Excel data to; it is not closed by this method.
     * @param format the workbook format to produce ({@link ExcelFormat#XLS} or {@link ExcelFormat#XLSX}), must not be null.
     * @throws UncheckedException if an I/O error occurs while writing.
     */
    public static void writeRowsToSheet(final String sheetName, final List<?> headers, final List<? extends Collection<?>> rows,
            final Consumer<? super Sheet> sheetSetter, final OutputStream outputStream, final ExcelFormat format) {
        try (Workbook workbook = newWorkbookForOutput(format)) {
            final Sheet sheet = workbook.createSheet(sheetName);

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

            workbook.write(outputStream);
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Writes data to the Excel file located at the given path with a custom sheet configuration function.
     * This is a thin {@link Path}-based delegate to {@link #writeRowsToSheet(String, List, List, Consumer, File)};
     * the workbook format is inferred from the path's filename extension.
     *
     * @param sheetName the name of the sheet to create in the workbook.
     * @param headers the column headers as a list of objects (will be converted to strings).
     * @param rows the data rows, where each row is a collection of cell values.
     * @param sheetSetter a consumer to apply custom formatting to the sheet after data is written (null to skip).
     * @param outputExcelPath the path to write the Excel data to (will be created or overwritten).
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created.
     */
    public static void writeRowsToSheet(final String sheetName, final List<?> headers, final List<? extends Collection<?>> rows,
            final Consumer<? super Sheet> sheetSetter, final Path outputExcelPath) {
        writeRowsToSheet(sheetName, headers, rows, sheetSetter, outputExcelPath.toFile());
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
     * Dataset dataset = CsvUtil.load(new File("data.csv"));
     * ExcelUtil.writeDatasetToSheet("ImportedData", dataset, new File("output.xlsx"));
     * }</pre>
     *
     * @param sheetName the name of the sheet to create in the workbook.
     * @param dataset the Dataset containing the data to write, must not be null.
     * @param outputExcelFile the file to write the Excel data to (will be created or overwritten).
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created.
     */
    public static void writeDatasetToSheet(final String sheetName, final Dataset dataset, final File outputExcelFile) {
        writeDatasetToSheet(sheetName, dataset, (SheetCreateOptions) null, outputExcelFile);
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
     * Dataset dataset = CsvUtil.load(new File("analysis.csv"));
     * ExcelUtil.SheetCreateOptions options = ExcelUtil.SheetCreateOptions.builder()
     *     .autoSizeColumn(true)
     *     .freezePane(new ExcelUtil.FreezePane(2, 1))  // freeze first 2 columns and first row
     *     .build();
     *
     * ExcelUtil.writeDatasetToSheet("Analysis", dataset, options, new File("analysis.xlsx"));
     * }</pre>
     *
     * @param sheetName the name of the sheet to create in the workbook.
     * @param dataset the Dataset containing the data to write, must not be null.
     * @param sheetCreateOptions configuration options for sheet formatting (null to apply no formatting).
     * @param outputExcelFile the file to write the Excel data to (will be created or overwritten).
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created.
     */
    public static void writeDatasetToSheet(final String sheetName, final Dataset dataset, final SheetCreateOptions sheetCreateOptions,
            final File outputExcelFile) {
        final int columnCount = dataset.columnCount();

        final Consumer<Sheet> sheetSetter = createSheetSetter(sheetCreateOptions, columnCount);

        writeDatasetToSheet(sheetName, dataset, sheetSetter, outputExcelFile);
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
     * Dataset dataset = CsvUtil.load(new File("data.csv"));
     * Consumer<Sheet> formatter = sheet -> {
     *     // Apply conditional formatting
     *     SheetConditionalFormatting sheetCF = sheet.getSheetConditionalFormatting();
     *     // ... configure conditional formatting
     * };
     *
     * ExcelUtil.writeDatasetToSheet("FormattedData", dataset, formatter, new File("formatted.xlsx"));
     * }</pre>
     *
     * @param sheetName the name of the sheet to create in the workbook.
     * @param dataset the Dataset containing the data to write, must not be null.
     * @param sheetSetter a consumer to apply custom formatting to the sheet after data is written (null to skip).
     * @param outputExcelFile the file to write the Excel data to (will be created or overwritten).
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created.
     */
    public static void writeDatasetToSheet(final String sheetName, final Dataset dataset, final Consumer<? super Sheet> sheetSetter,
            final File outputExcelFile) {
        try (OutputStream os = new FileOutputStream(outputExcelFile)) {
            writeDatasetToSheet(sheetName, dataset, sheetSetter, os, formatOf(outputExcelFile));
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Writes a Dataset to the given output stream as an Excel workbook of the specified {@link ExcelFormat}.
     * This overload lets callers write to an arbitrary {@link OutputStream} (e.g. an
     * {@code HttpServletResponse} output stream or an in-memory {@code ByteArrayOutputStream}) and
     * choose the workbook format explicitly instead of relying on filename-extension inference.
     * The Dataset's column names are used as the header row.
     *
     * <p>The supplied {@code outputStream} is flushed (via {@code workbook.write}) but <b>not closed</b>
     * by this method; the caller retains ownership of it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * try (OutputStream os = response.getOutputStream()) {
     *     ExcelUtil.writeDatasetToSheet("Report", dataset, (Consumer<Sheet>) null, os, ExcelUtil.ExcelFormat.XLSX);
     * }
     * }</pre>
     *
     * @param sheetName the name of the sheet to create in the workbook.
     * @param dataset the Dataset containing the data to write, must not be null.
     * @param sheetSetter a consumer to apply custom formatting to the sheet after data is written (null to skip).
     * @param outputStream the stream to write the Excel data to; it is not closed by this method.
     * @param format the workbook format to produce ({@link ExcelFormat#XLS} or {@link ExcelFormat#XLSX}), must not be null.
     * @throws UncheckedException if an I/O error occurs while writing.
     */
    public static void writeDatasetToSheet(final String sheetName, final Dataset dataset, final Consumer<? super Sheet> sheetSetter,
            final OutputStream outputStream, final ExcelFormat format) {
        try (Workbook workbook = newWorkbookForOutput(format)) {
            final Sheet sheet = workbook.createSheet(sheetName);

            final int columnCount = dataset.columnCount();

            final Row headerRow = sheet.createRow(0);

            for (int i = 0; i < columnCount; i++) {
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

            workbook.write(outputStream);
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Writes a Dataset to the Excel file located at the given path with custom sheet formatting.
     * This is a thin {@link Path}-based delegate to {@link #writeDatasetToSheet(String, Dataset, Consumer, File)};
     * the workbook format is inferred from the path's filename extension.
     *
     * @param sheetName the name of the sheet to create in the workbook.
     * @param dataset the Dataset containing the data to write, must not be null.
     * @param sheetSetter a consumer to apply custom formatting to the sheet after data is written (null to skip).
     * @param outputExcelPath the path to write the Excel data to (will be created or overwritten).
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created.
     */
    public static void writeDatasetToSheet(final String sheetName, final Dataset dataset, final Consumer<? super Sheet> sheetSetter,
            final Path outputExcelPath) {
        writeDatasetToSheet(sheetName, dataset, sheetSetter, outputExcelPath.toFile());
    }

    static void setCellValue(final Cell cell, final Object cellValue) {
        if (cellValue == null) {
            cell.setBlank();
        } else if (cellValue instanceof String val) {
            cell.setCellValue(val);
        } else if (cellValue instanceof Boolean val) {
            cell.setCellValue(val);
        } else if (cellValue instanceof java.util.Date val) {
            cell.setCellValue(val);
        } else if (cellValue instanceof LocalDate val) {
            cell.setCellValue(val);
        } else if (cellValue instanceof LocalDateTime val) {
            cell.setCellValue(val);
        } else if (cellValue instanceof java.util.Calendar val) {
            cell.setCellValue(val);
        } else if (cellValue instanceof Integer val) {
            cell.setCellValue(val);
        } else if (cellValue instanceof Number val) {
            cell.setCellValue(val.doubleValue());
        } else {
            cell.setCellValue(N.stringOf(cellValue));
        }
    }

    private static Workbook newWorkbookForOutput(final ExcelFormat format) {
        return format == ExcelFormat.XLS ? new HSSFWorkbook() : new XSSFWorkbook();
    }

    /**
     * Infers the {@link ExcelFormat} from the file's name: a {@code .xls} extension (case-insensitive)
     * maps to {@link ExcelFormat#XLS}; anything else (including no extension) maps to
     * {@link ExcelFormat#XLSX}. This mirrors the historical extension-inference behavior of the
     * {@code File}/{@code Path} write overloads.
     *
     * <p><b>Note:</b> only {@code .xls} and {@code .xlsx} are first-class here. Because only the literal
     * {@code .xls} extension maps to {@link ExcelFormat#XLS}, names such as {@code .xlsm} (macro-enabled)
     * or {@code .xlsb} (binary) infer {@link ExcelFormat#XLSX} and therefore produce an XSSF (OOXML)
     * workbook written under that name — a file whose extension does not match its actual content. To
     * control the format independently of the filename, use an {@code OutputStream} write overload and
     * pass the desired {@link ExcelFormat} explicitly.
     */
    static ExcelFormat formatOf(final File outputExcelFile) {
        final String name = outputExcelFile == null ? "" : outputExcelFile.getName();
        final int dot = name.lastIndexOf('.');
        final String extension = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";

        return "xls".equals(extension) ? ExcelFormat.XLS : ExcelFormat.XLSX;
    }

    /**
     * The physical Excel workbook format produced by the {@code OutputStream} write overloads.
     * The {@link File}/{@link Path} write overloads infer this from the filename extension
     * ({@code .xls} maps to {@link #XLS}, anything else to {@link #XLSX}); the {@code OutputStream}
     * overloads require it to be chosen explicitly.
     *
     * <ul>
     *   <li>{@link #XLS} — the legacy Excel 97-2003 binary format (POI {@code HSSFWorkbook}).</li>
     *   <li>{@link #XLSX} — the Excel 2007+ Office Open XML format (POI {@code XSSFWorkbook}).</li>
     * </ul>
     */
    public enum ExcelFormat {
        /** Excel 97-2003 binary workbook (.xls), backed by POI {@code HSSFWorkbook}. */
        XLS,

        /** Excel 2007+ Office Open XML workbook (.xlsx), backed by POI {@code XSSFWorkbook}. */
        XLSX
    }

    /**
     * Converts the specified sheet from an Excel file to a CSV file.
     * This convenience method exports an Excel sheet to comma-separated values format,
     * preserving all data including the header row. The charset depends on the
     * underlying {@code IOUtil.newFileWriter} implementation.
     *
     * <p>Cell values are converted to their string representations: numeric cells are
     * formatted as numbers, formulas are exported as formula text, and blank cells
     * become empty strings. The CSV format uses comma as delimiter and proper quoting
     * for values containing special characters.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExcelUtil.exportSheetToCsv(
     *     new File("data.xlsx"),
     *     0,
     *     new File("output.csv")
     * );
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @param sheetIndex the zero-based index of the sheet to convert (0 for first sheet)
     * @param outputCsvFile the CSV file to write to (will be created or overwritten)
     * @throws UncheckedException if an I/O error occurs while reading the Excel file or if the file is not a valid Excel file
     * @throws UncheckedIOException if an I/O error occurs while opening or closing the CSV output file (write-phase errors surface as {@code UncheckedException})
     * @throws IllegalArgumentException if the sheet index is out of bounds
     */
    public static void exportSheetToCsv(final File excelFile, final int sheetIndex, final File outputCsvFile) {
        try (Writer writer = IOUtil.newFileWriter(outputCsvFile)) {
            exportSheetToCsv(excelFile, sheetIndex, null, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Converts the sheet with the specified name from an Excel file to a CSV file.
     * This method allows exporting a specific worksheet by name rather than index, which is
     * more robust when working with workbooks where sheet positions might change. The charset
     * depends on the underlying {@code IOUtil.newFileWriter} implementation and all original
     * data, including headers, is preserved.
     *
     * <p>The conversion process maintains data fidelity by converting each cell type appropriately:
     * strings remain as text, numbers are formatted, and formulas are exported as their text
     * representation. The output CSV uses standard comma delimiting with proper escaping.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * ExcelUtil.exportSheetToCsv(
     *     new File("workbook.xlsx"),
     *     "SalesData",
     *     new File("sales.csv")
     * );
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @param sheetName the name of the sheet to convert, case-sensitive
     * @param outputCsvFile the CSV file to write to (will be created or overwritten)
     * @throws UncheckedException if an I/O error occurs while reading the Excel file or if the file is not a valid Excel file
     * @throws UncheckedIOException if an I/O error occurs while opening or closing the CSV output file (write-phase errors surface as {@code UncheckedException})
     * @throws IllegalArgumentException if the sheet name is not found in the workbook
     */
    public static void exportSheetToCsv(final File excelFile, final String sheetName, final File outputCsvFile) {
        try (Writer writer = IOUtil.newFileWriter(outputCsvFile)) {
            exportSheetToCsv(excelFile, sheetName, null, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Converts the specified sheet from an Excel file to a CSV file with custom options.
     * This method provides full control over the CSV export process, allowing you to replace
     * the original Excel headers with custom headers. When custom headers are provided, the
     * first row of the Excel sheet is skipped and replaced with the custom headers.
     *
     * <p>The character encoding is determined by the {@code Writer} provided by the caller.
     * For specific encoding requirements, wrap a {@code FileOutputStream} with an
     * {@code OutputStreamWriter} specifying the desired charset. This method flushes the writer
     * but does not close it.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> customHeaders = List.of("Product ID", "Product Name", "Price");
     * try (Writer writer = IOUtil.newFileWriter(new File("products.csv"))) {
     *     ExcelUtil.exportSheetToCsv(
     *         new File("products.xlsx"),
     *         0,
     *         customHeaders,
     *         writer
     *     );
     * }
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @param sheetIndex the zero-based index of the sheet to convert (0 for first sheet)
     * @param csvHeaders custom headers for the CSV file; {@code null} or empty preserves the original Excel rows.
     * @param outputWriter the Writer to write the CSV content to; it is flushed but not closed.
     * @throws UncheckedException if an I/O error occurs during conversion or if the file is not a valid Excel file
     * @throws IllegalArgumentException if the sheet index is out of bounds
     */
    public static void exportSheetToCsv(final File excelFile, final int sheetIndex, final List<String> csvHeaders, final Writer outputWriter) {
        try (InputStream is = new FileInputStream(excelFile); //
             Workbook workbook = WorkbookFactory.create(is)) {
            final Sheet sheet = workbook.getSheetAt(sheetIndex);

            exportSheetToCsv(sheet, csvHeaders, outputWriter);
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    /**
     * Converts the sheet with the specified name to CSV output, writing to the given Writer.
     * This method combines named sheet access with optional custom header replacement.
     * When custom headers are provided, the first row of the Excel sheet is skipped and replaced
     * with the custom headers in the CSV output. The character encoding of the output is determined
     * by the {@code Writer} supplied by the caller, and the writer is flushed but not closed.
     *
     * <p>When custom headers are provided, they replace the first row of the Excel sheet in
     * the CSV output. This is useful for normalizing column names, translating headers, or
     * adapting to target system naming conventions.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<String> headers = List.of("Código", "Nombre", "Precio");
     * try (Writer writer = IOUtil.newFileWriter(new File("productos.csv"))) {
     *     ExcelUtil.exportSheetToCsv(
     *         new File("productos.xlsx"),
     *         "Inventario",
     *         headers,
     *         writer
     *     );
     * }
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @param sheetName the name of the sheet to convert, case-sensitive
     * @param csvHeaders custom headers for the CSV file; {@code null} or empty preserves the original Excel rows.
     * @param outputWriter the Writer to write the CSV content to; it is flushed but not closed.
     * @throws UncheckedException if an I/O error occurs or if the file is not a valid Excel file
     * @throws IllegalArgumentException if the sheet name is not found in the workbook
     */
    public static void exportSheetToCsv(final File excelFile, final String sheetName, final List<String> csvHeaders, final Writer outputWriter) {
        try (InputStream is = new FileInputStream(excelFile); //
             Workbook workbook = WorkbookFactory.create(is)) {
            final Sheet sheet = getRequiredSheet(workbook, sheetName);

            exportSheetToCsv(sheet, csvHeaders, outputWriter);
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    private static void exportSheetToCsv(final Sheet sheet, final List<String> csvHeaders, final Writer output) throws IOException {
        final Type<Object> strType = Type.of(String.class);
        final char separator = SK._COMMA;

        final BufferedCsvWriter bw = Objectory.createBufferedCsvWriter(output);

        try {
            if (N.notEmpty(csvHeaders)) {
                int idx = 0;

                for (String csvHeader : csvHeaders) {
                    if (idx++ > 0) {
                        bw.write(separator);
                    }

                    CsvUtil.writeField(bw, strType, csvHeader);
                }

            }

            boolean skipFirstRow = N.notEmpty(csvHeaders);
            int linesWritten = skipFirstRow ? 1 : 0;

            for (Row row : sheet) {
                if (skipFirstRow) {
                    skipFirstRow = false;
                    continue;
                }

                if (linesWritten++ > 0) {
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                }

                final int cellCount = Math.max(row.getLastCellNum(), 0);

                for (int i = 0; i < cellCount; i++) {
                    if (i > 0) {
                        bw.write(separator);
                    }

                    final Cell cell = row.getCell(i);

                    if (cell == null || cell.getCellType() == CellType.BLANK) {
                        CsvUtil.writeField(bw, strType, "");
                    } else {
                        switch (cell.getCellType()) {
                            case STRING -> CsvUtil.writeField(bw, strType, cell.getStringCellValue());
                            case NUMERIC -> CsvUtil.writeField(bw, null, cell.getNumericCellValue());
                            case BOOLEAN -> CsvUtil.writeField(bw, null, cell.getBooleanCellValue());
                            case FORMULA -> CsvUtil.writeField(bw, strType, cell.getCellFormula());
                            case ERROR -> CsvUtil.writeField(bw, strType, "");
                            default -> throw new RuntimeException("Unsupported cell type: " + cell.getCellType());
                        }
                    }
                }
            }

            bw.flush();

        } finally {
            Objectory.recycle(bw);
        }
    }

    /**
     * Collection of predefined row mapping functions for common Excel data transformation use cases.
     * This utility class provides ready-to-use mapper functions that convert Excel {@code Row} objects
     * to various Java types including {@code List<Object>}, {@code List<String>}, or delimited string
     * representations. These mappers are designed for use with {@code readRowsFromSheet} methods to control
     * how row data is extracted and transformed during the read process.
     *
     * <p>The class offers both predefined constants for common scenarios ({@code DEFAULT}, {@code ROW2STRING})
     * and factory methods for creating custom mappers with specific cell processing logic. This flexibility
     * allows for tailored data extraction while maintaining clean, reusable code patterns.</p>
     *
     * <p><b>Key Features:</b>
     * <ul>
     *   <li><b>DEFAULT Mapper:</b> Converts rows to {@code List<Object>} with type preservation</li>
     *   <li><b>ROW2STRING Mapper:</b> Converts rows to delimited strings using default separator</li>
     *   <li><b>Custom Separators:</b> Create string mappers with custom delimiters</li>
     *   <li><b>Custom Cell Processing:</b> Define custom cell transformation logic</li>
     *   <li><b>Type-Safe Extraction:</b> Create strongly-typed list mappers for specific data types</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * File file = new File("data.xlsx");
     *
     * // Use default mapper to get list of objects with type preservation
     * List<List<Object>> rows = ExcelUtil.readRowsFromSheet(
     *     file,
     *     0,
     *     true,  // skip header row
     *     ExcelUtil.RowMappers.DEFAULT
     * );
     *
     * // Convert rows to comma-separated strings
     * List<String> rowStrings = ExcelUtil.readRowsFromSheet(
     *     file,
     *     0,
     *     false,
     *     ExcelUtil.RowMappers.ROW2STRING
     * );
     *
     * // Create custom mapper with pipe separator
     * Function<Row, String> pipeMapper = ExcelUtil.RowMappers.toDelimitedString("|");
     * List<String> pipeDelimited = ExcelUtil.readRowsFromSheet(file, 0, true, pipeMapper);
     *
     * // Create strongly-typed mapper for numeric data
     * Function<Cell, Double> numericExtractor = cell ->
     *     cell.getCellType() == CellType.NUMERIC ? cell.getNumericCellValue() : 0.0;
     * Function<Row, List<Double>> numericMapper = ExcelUtil.RowMappers.toList(numericExtractor);
     * List<List<Double>> numericData = ExcelUtil.readRowsFromSheet(file, 0, true, numericMapper);
     * }</pre>
     *
     * @see #DEFAULT
     * @see #ROW2STRING
     * @see #toDelimitedString(String)
     * @see #toDelimitedString(String, Function)
     * @see #toList(Function)
     */
    public static final class RowMappers {
        private RowMappers() {
            // prevent instantiation
        }

        /**
         * Default row mapper that converts each Excel row to a {@code List<Object>} with type preservation.
         * This mapper uses {@link #CELL_GETTER} to extract cell values while maintaining their original
         * data types: {@code String} for text cells, {@code Double} for numeric cells, {@code Boolean}
         * for boolean cells, etc. This is the recommended mapper for general-purpose data extraction
         * when you need to preserve Excel cell types in your Java objects.
         *
         * <p>The resulting list contains one element per cell in the row, with each element typed
         * according to the cell's content. This allows for flexible downstream processing while
         * maintaining data type integrity from the source Excel file.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Read all rows with type preservation
         * List<List<Object>> rows = ExcelUtil.readRowsFromSheet(
         *     new File("data.xlsx"),
         *     0,
         *     true,  // skip first row (headers)
         *     RowMappers.DEFAULT
         * );
         *
         * // Process typed data
         * for (List<Object> row : rows) {
         *     String name = (String) row.get(0);
         *     Double value = (Double) row.get(1);
         *     Boolean flag = (Boolean) row.get(2);
         * }
         * }</pre>
         *
         */
        public static final Function<Row, List<Object>> DEFAULT = toList(CELL_GETTER);

        /**
         * Row to String mapper that converts entire Excel rows to delimited string representations.
         * This mapper converts each cell to its string representation and joins them using the
         * default element separator defined by {@code Strings.ELEMENT_SEPARATOR}. Useful for quick
         * text conversion of Excel data or debugging purposes where a simple string representation
         * of row data is sufficient.
         *
         * <p>All cell types are converted to strings: numbers are formatted as decimal strings,
         * booleans as "true"/"false", and formulas as their formula text. The resulting string
         * provides a readable representation of the complete row data.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Convert all rows to strings with default separator
         * List<String> rowStrings = ExcelUtil.readRowsFromSheet(
         *     new File("data.xlsx"),
         *     "Sheet1",
         *     false,
         *     RowMappers.ROW2STRING
         * );
         *
         * // Print or log row data
         * rowStrings.forEach(System.out::println);
         * }</pre>
         *
         */
        public static final Function<Row, String> ROW2STRING = toDelimitedString(Strings.ELEMENT_SEPARATOR);

        /**
         * Creates a row mapper that converts rows to delimited strings with a custom separator.
         * Each cell is converted to its string representation using the default {@link #CELL_TO_STRING}
         * mapper and the resulting strings are joined together with the specified separator. Useful for
         * creating custom text representations of Excel rows.
         *
         * <p>This method uses {@link #CELL_TO_STRING} for cell conversion, which handles all cell types
         * (STRING, NUMERIC, BOOLEAN, FORMULA, BLANK, ERROR) and converts them to appropriate string
         * representations.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Function<Row, String> pipeMapper = RowMappers.toDelimitedString("|");
         * List<String> rows = ExcelUtil.readRowsFromSheet(file, 0, true, pipeMapper);
         * // Results in rows like: "John|30|New York"
         * }</pre>
         *
         * @param cellSeparator the string to use as separator between cell values
         * @return a Function that converts Row to delimited String
         */
        public static Function<Row, String> toDelimitedString(final String cellSeparator) {
            return toDelimitedString(cellSeparator, CELL_TO_STRING);
        }

        /**
         * Creates a row mapper that converts rows to delimited strings with custom cell processing.
         * This method provides maximum flexibility by allowing you to specify both the separator
         * and a custom cell-to-string conversion function. Use this when you need special formatting,
         * value transformations, or custom handling of specific cell types.
         *
         * <p>The cellMapper function is applied to each non-null cell, giving you control over
         * formatting numbers, dates, or applying business logic during the conversion process.
         * Missing cells are emitted as empty fields without invoking the mapper.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Function<Cell, String> customCellMapper = cell ->
         *     cell.getCellType() == CellType.NUMERIC
         *         ? String.format("%.2f", cell.getNumericCellValue())
         *         : ExcelUtil.CELL_TO_STRING.apply(cell);
         *
         * Function<Row, String> mapper = RowMappers.toDelimitedString(",", customCellMapper);
         * }</pre>
         *
         * @param cellSeparator the string to use as separator between cell values
         * @param cellMapper custom function to convert each cell to a string
         * @return a Function that converts Row to delimited String
         */
        public static Function<Row, String> toDelimitedString(final String cellSeparator, final Function<Cell, String> cellMapper) {
            return row -> {
                final StringBuilder sb = Objectory.createStringBuilder();
                final int cellCount = Math.max(row.getLastCellNum(), 0);

                try {
                    for (int i = 0; i < cellCount; i++) {
                        if (i > 0) {
                            sb.append(cellSeparator);
                        }

                        final Cell cell = row.getCell(i);
                        sb.append(cell == null ? "" : cellMapper.apply(cell));
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
         * <p>The cellMapper function is applied to each non-null cell in the row, and the results are
         * collected into a List. Missing cells are represented as {@code null}, preserving sparse row
         * structure while still allowing strongly typed extraction.</p>
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
         * List<List<Double>> numericData = ExcelUtil.readRowsFromSheet(file, 0, true, mapper);
         * }</pre>
         *
         * @param <T> the type of objects to extract from cells
         * @param cellMapper function to convert each cell to type T
         * @return a Function that converts Row to List&lt;T&gt;
         */
        public static <T> Function<Row, List<T>> toList(final Function<Cell, T> cellMapper) {
            return row -> {
                final int cellCount = Math.max(row.getLastCellNum(), 0);
                final List<T> list = new ArrayList<>(cellCount);

                for (int i = 0; i < cellCount; i++) {
                    final Cell cell = row.getCell(i);
                    list.add(cell == null ? null : cellMapper.apply(cell));
                }

                return list;
            };
        }
    }

    /**
     * Collection of row extraction functions specifically designed for use with {@code loadDatasetFromSheet} methods.
     * Row extractors are specialized consumers that process Excel {@code Row} objects and populate output
     * arrays with extracted data, which is then used to construct {@code Dataset} objects. Unlike
     * {@link RowMappers} which transform rows to various types, extractors focus on efficient data
     * extraction for Dataset creation with column-oriented data structures.
     *
     * <p>Extractors receive three parameters: the header names array, the current row being processed,
     * and an output array to populate. This design enables efficient, array-based data transfer optimized
     * for Dataset construction while allowing custom cell processing logic.</p>
     *
     * <p><b>Key Differences from RowMappers:</b>
     * <ul>
     *   <li><b>Purpose:</b> Extractors are for Dataset creation; mappers are for general row transformation</li>
     *   <li><b>Output:</b> Extractors populate pre-allocated arrays; mappers return new objects</li>
     *   <li><b>Performance:</b> Extractors are optimized for bulk data loading with minimal object allocation</li>
     *   <li><b>Use Case:</b> Extractors with {@code loadDatasetFromSheet()}; mappers with {@code readRowsFromSheet()}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use default extractor for standard Dataset loading
     * File file = new File("data.xlsx");
     * Dataset defaultDataset = ExcelUtil.loadDatasetFromSheet(
     *     file,
     *     0,
     *     ExcelUtil.RowExtractors.DEFAULT
     * );
     *
     * // Create custom extractor for specific processing (e.g., uppercase conversion)
     * TriConsumer<String[], Row, Object[]> uppercaseExtractor =
     *     ExcelUtil.RowExtractors.create(cell ->
     *         cell.getCellType() == CellType.STRING
     *             ? cell.getStringCellValue().toUpperCase()
     *             : ExcelUtil.CELL_GETTER.apply(cell)
     *     );
     * Dataset uppercaseDataset = ExcelUtil.loadDatasetFromSheet(file, 0, uppercaseExtractor);
     *
     * // The extractor short-circuits null cells (writing null to the output) — the mapper
     * // itself is only invoked for non-null cells.
     * TriConsumer<String[], Row, Object[]> safeExtractor =
     *     ExcelUtil.RowExtractors.create(ExcelUtil.CELL_GETTER);
     * Dataset safeDataset = ExcelUtil.loadDatasetFromSheet(file, "Sheet1", safeExtractor);
     * }</pre>
     *
     * @see #DEFAULT
     * @see #create(Function)
     * @see RowMappers
     */
    public static final class RowExtractors {

        private RowExtractors() {
            // prevent instantiation
        }

        /**
         * Default row extractor that extracts cell values while preserving their original data types.
         * This extractor uses {@link #CELL_GETTER} to extract values from each cell, maintaining
         * the Excel cell's native type: {@code String} for text, {@code Double} for numbers,
         * {@code Boolean} for boolean values, etc. This is the recommended extractor for standard
         * Dataset creation when type preservation is desired.
         *
         * <p>The extractor iterates through all cells in a row and populates the output array with
         * appropriately typed values, which are then used to construct column-oriented Dataset
         * structures. This provides the most natural and type-safe representation of Excel data
         * in Dataset form.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Load Excel sheet into Dataset with type preservation
         * Dataset dataset = ExcelUtil.loadDatasetFromSheet(
         *     new File("sales.xlsx"),
         *     0,  // first sheet
         *     ExcelUtil.RowExtractors.DEFAULT
         * );
         *
         * // Access data with original types preserved
         * List<Object> productCol = dataset.getColumn("Product");   // values are Strings
         * List<Object> priceCol = dataset.getColumn("Price");       // values are Doubles
         * }</pre>
         *
         */
        public static final TriConsumer<String[], Row, Object[]> DEFAULT = create(CELL_GETTER);

        /**
         * Creates a custom row extractor with the specified cell mapping function.
         * The extractor processes each cell in a row and populates the output array with
         * transformed values. This is used with loadDatasetFromSheet methods to control how Excel
         * cell data is extracted and converted for Dataset creation.
         *
         * <p>The cell mapper function receives each non-null {@link Cell} from the row and returns
         * the value to store in the output array. The extractor itself short-circuits for
         * {@code null} cells (writing {@code null} to the output array) — the mapper is therefore
         * only invoked for non-null cells and does not need to handle {@code null} input.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * // Extract every cell as its String form (null cells become null entries in the row)
         * TriConsumer<String[], Row, Object[]> stringExtractor =
         *     ExcelUtil.RowExtractors.create(ExcelUtil.CELL_TO_STRING);
         *
         * File file = new File("data.xlsx");
         * Dataset stringDataset = ExcelUtil.loadDatasetFromSheet(file, "Sheet1", stringExtractor);
         * }</pre>
         *
         * @param cellMapper function to convert each cell to the desired output type
         * @return a TriConsumer that extracts row data using the cell mapper
         */
        public static TriConsumer<String[], Row, Object[]> create(final Function<Cell, ?> cellMapper) {
            return (header, row, output) -> {
                for (int i = 0, len = header.length; i < len; i++) {
                    final Cell cell = row.getCell(i);
                    output[i] = cell == null ? null : cellMapper.apply(cell);
                }
            };
        }
    }

    /**
     * Configuration options for controlling Excel sheet creation with professional formatting and features.
     * This class provides a comprehensive set of options to enhance the appearance and usability of
     * generated Excel files, including automatic column sizing, freeze panes for easier navigation,
     * and auto-filters for data exploration. Uses the builder pattern to enable fluent, readable
     * configuration with optional settings.
     *
     * <p>These options are applied after all sheet data has been written, ensuring that formatting
     * decisions can be based on the actual content. Auto-sizing, for example, adjusts column widths
     * based on the actual data written to each column, and auto-filters are positioned based on the
     * header row structure.</p>
     *
     * <p><b>Configuration Precedence Rules:</b>
     * <ul>
     *   <li>If {@code freezePane} is set, it takes precedence over {@code freezeFirstRow}</li>
     *   <li>If {@code autoFilter} is set, it takes precedence over {@code autoFilterByFirstRow}</li>
     *   <li>All other options are applied independently and can be combined</li>
     * </ul>
     *
     * <p><b>Common Configuration Patterns:</b>
     * <ul>
     *   <li><b>Basic Report:</b> Enable {@code autoSizeColumn} and {@code freezeFirstRow}</li>
     *   <li><b>Interactive Data:</b> Add {@code autoFilterByFirstRow} for filtering capabilities</li>
     *   <li><b>Large Dataset:</b> Use {@code freezePane} to freeze multiple columns/rows</li>
     *   <li><b>Simple Export:</b> Leave all options disabled (default values)</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dataset dataset = CsvUtil.load(new File("sales.csv"));
     *
     * // Basic formatted report with auto-sizing and frozen header
     * ExcelUtil.SheetCreateOptions basicOptions = ExcelUtil.SheetCreateOptions.builder()
     *     .autoSizeColumn(true)
     *     .freezeFirstRow(true)
     *     .autoFilterByFirstRow(true)
     *     .build();
     *
     * ExcelUtil.writeDatasetToSheet("Sales Report", dataset, basicOptions, new File("report.xlsx"));
     *
     * // Advanced formatting with custom freeze pane
     * ExcelUtil.SheetCreateOptions advancedOptions = ExcelUtil.SheetCreateOptions.builder()
     *     .autoSizeColumn(true)
     *     .freezePane(new ExcelUtil.FreezePane(2, 1))    // freeze first 2 columns and first row
     *     .autoFilter(new CellRangeAddress(0, 0, 0, 5))  // auto-filter on first row, columns A-F
     *     .build();
     *
     * ExcelUtil.writeDatasetToSheet("Detailed Analysis", dataset, advancedOptions, new File("analysis.xlsx"));
     *
     * // Default (no formatting) - fastest option
     * ExcelUtil.SheetCreateOptions defaultOptions = new ExcelUtil.SheetCreateOptions();
     * ExcelUtil.writeDatasetToSheet("Raw Data", dataset, defaultOptions, new File("raw.xlsx"));
     * }</pre>
     *
     * @see FreezePane
     * @see CellRangeAddress
     */
    @Data
    @Builder
    @AllArgsConstructor
    public static class SheetCreateOptions {

        /**
         * Creates a new instance of SheetCreateOptions with default values.
         * All boolean flags are initialized to {@code false}, and object references are {@code null},
         * resulting in no special formatting being applied to the generated sheet. This is the
         * fastest option as it skips all post-processing formatting operations.
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * ExcelUtil.SheetCreateOptions options = new ExcelUtil.SheetCreateOptions();
         * options.isAutoSizeColumn();       // returns false (boolean flags default to false)
         * options.isFreezeFirstRow();       // returns false
         * options.getFreezePane();          // returns null (object references default to null)
         * options.getAutoFilter();          // returns null
         *
         * options.setAutoSizeColumn(true);  // @Data generates setters
         * options.isAutoSizeColumn();       // returns true
         * }</pre>
         *
         */
        public SheetCreateOptions() {
        }

        /**
         * Whether to automatically resize columns to fit their content width.
         * When set to {@code true}, after all data is written to the sheet, each column will be
         * auto-sized based on the width of its content. This ensures all data is visible without
         * manual column width adjustments, improving readability of the generated Excel file.
         *
         * <p><b>Performance Note:</b> Auto-sizing can be slow for sheets with many columns or rows,
         * as it requires Apache POI to measure the width of content in each cell. For large datasets
         * (>10,000 rows or >50 columns), consider disabling this option or using fixed column widths.</p>
         *
         * <p><b>Default:</b> {@code false} (no auto-sizing)</p>
         */
        private boolean autoSizeColumn;

        /**
         * Custom freeze pane configuration specifying the number of columns and rows to freeze.
         * When set, creates a freeze pane at the specified position, allowing users to scroll
         * through data while keeping certain columns and rows visible. This is particularly useful
         * for large datasets where you want to keep headers and/or ID columns always visible.
         *
         * <p>This option takes precedence over {@link #freezeFirstRow}. If both are set, only
         * {@code freezePane} will be applied. Set to {@code null} to use {@code freezeFirstRow}
         * instead or to disable freeze panes entirely.</p>
         *
         * <p><b>Common Use Cases:</b>
         * <ul>
         *   <li>{@code new FreezePane(0, 1)} - Freeze only the first row (header row)</li>
         *   <li>{@code new FreezePane(1, 1)} - Freeze first column and first row</li>
         *   <li>{@code new FreezePane(2, 1)} - Freeze first two columns and first row</li>
         * </ul>
         *
         * <p><b>Default:</b> {@code null} (no custom freeze pane)</p>
         *
         * @see FreezePane
         */
        private FreezePane freezePane;

        /**
         * Whether to freeze the first row of the sheet (typically the header row).
         * When set to {@code true}, the first row remains visible while scrolling through data,
         * making it easy to keep track of column meanings in large datasets. This is a convenience
         * option equivalent to setting {@code freezePane} to {@code new FreezePane(0, 1)}.
         *
         * <p>This option is ignored if {@link #freezePane} is also set. Use {@code freezePane}
         * for more control over which rows and columns to freeze.</p>
         *
         * <p><b>Default:</b> {@code false} (no freeze)</p>
         */
        private boolean freezeFirstRow;

        /**
         * Custom cell range for applying Excel's auto-filter feature.
         * When set, creates filter dropdown buttons for the specified range of cells, allowing users
         * to interactively filter and sort data within Excel. This provides maximum control over
         * which columns and rows should have filtering enabled.
         *
         * <p>This option takes precedence over {@link #autoFilterByFirstRow}. If both are set,
         * only {@code autoFilter} will be applied. Set to {@code null} to use {@code autoFilterByFirstRow}
         * instead or to disable auto-filters entirely.</p>
         *
         * <p><b>Usage Examples:</b>
         * {@code new CellRangeAddress(0, 0, 0, 5)} creates auto-filter for the first row
         * across columns A through F (indices 0-5).</p>
         *
         * <p><b>Default:</b> {@code null} (no custom auto-filter)</p>
         *
         * @see CellRangeAddress
         */
        private CellRangeAddress autoFilter;

        /**
         * Whether to apply auto-filter to all columns in the first row.
         * When set to {@code true}, creates filter dropdown buttons for all columns in the header row,
         * enabling users to filter and sort data interactively. This is a convenience option that
         * automatically determines the correct column range based on the data written.
         *
         * <p>This option is ignored if {@link #autoFilter} is also set. Use {@code autoFilter}
         * for more precise control over the filter range.</p>
         *
         * <p><b>Default:</b> {@code false} (no auto-filter)</p>
         */
        private boolean autoFilterByFirstRow;
    }

    /**
     * Represents a freeze pane configuration for Excel sheets, specifying which columns and rows
     * should remain visible when scrolling through the worksheet. Freeze panes are essential for
     * improving the usability of large Excel datasets by keeping headers and key columns always
     * visible, making it easier for users to understand the data context while scrolling.
     *
     * <p>The freeze pane divides the sheet into panes at the specified position. Columns to the
     * left of {@code colSplit} and rows above {@code rowSplit} will remain fixed in place when
     * scrolling. The split occurs <b>after</b> the specified column/row number, meaning
     * {@code new FreezePane(1, 1)} freezes the first column (index 0) and first row (index 0).</p>
     *
     * <p><b>Common Freeze Pane Patterns:</b>
     * <ul>
     *   <li><b>Header Row Only:</b> {@code new FreezePane(0, 1)} - Freezes just the top row</li>
     *   <li><b>Header and ID Column:</b> {@code new FreezePane(1, 1)} - Freezes first column and first row</li>
     *   <li><b>Multiple Columns:</b> {@code new FreezePane(2, 1)} - Freezes first two columns and top row</li>
     *   <li><b>Column Only:</b> {@code new FreezePane(1, 0)} - Freezes just the first column</li>
     *   <li><b>Multiple Rows:</b> {@code new FreezePane(0, 2)} - Freezes the first two rows</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * Dataset dataset = CsvUtil.load(new File("data.csv"));
     *
     * // Freeze first column and first row (most common pattern)
     * ExcelUtil.FreezePane headerAndId = new ExcelUtil.FreezePane(1, 1);
     * ExcelUtil.SheetCreateOptions options = ExcelUtil.SheetCreateOptions.builder()
     *     .freezePane(headerAndId)
     *     .build();
     *
     * // Freeze only the header row for wide datasets
     * ExcelUtil.FreezePane headerOnly = new ExcelUtil.FreezePane(0, 1);
     * ExcelUtil.writeDatasetToSheet("Data", dataset, ExcelUtil.SheetCreateOptions.builder()
     *     .freezePane(headerOnly)
     *     .autoFilterByFirstRow(true)
     *     .build(), new File("output.xlsx"));
     *
     * // Freeze first two columns for comparison
     * ExcelUtil.FreezePane twoColumns = new ExcelUtil.FreezePane(2, 0);
     * }</pre>
     *
     * @param colSplit the number of columns to freeze from the left (0 for none). The freeze occurs
     *                 after this many columns, so {@code colSplit=1} freezes column A (index 0).
     * @param rowSplit the number of rows to freeze from the top (0 for none). The freeze occurs
     *                 after this many rows, so {@code rowSplit=1} freezes row 1 (index 0).
     */
    public record FreezePane(int colSplit, int rowSplit) {
    }
}
