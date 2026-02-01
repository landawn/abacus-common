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
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.WD;
import com.landawn.abacus.util.function.TriConsumer;
import com.landawn.abacus.util.stream.Stream;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * A comprehensive, enterprise-grade utility class providing advanced Excel file processing capabilities
 * including high-performance reading, writing, and conversion operations using Apache POI. This class
 * serves as a powerful toolkit for Excel-based ETL (Extract, Transform, Load) operations, data import/export
 * scenarios, and seamless integration with Dataset objects for efficient spreadsheet data manipulation
 * and analysis in enterprise environments requiring robust Excel processing capabilities.
 *
 * <p>The {@code ExcelUtil} class addresses critical challenges in enterprise Excel data processing by
 * providing optimized, scalable solutions for handling complex Excel workbooks efficiently while
 * maintaining data integrity, type safety, and memory efficiency. It supports various Excel formats
 * (XLS, XLSX), custom cell processing, flexible worksheet operations, and configurable options for
 * memory management, custom transformations, and filtering operations suitable for production
 * environments with strict performance and reliability requirements.</p>
 *
 * <p><b>⚠️ IMPORTANT - Memory Management:</b>
 * Excel files, especially those with large numbers of rows or complex formatting, can consume
 * significant memory. For large Excel files (>10MB or >50,000 rows), use streaming methods
 * where available and ensure proper resource cleanup with try-with-resources blocks. Configure
 * appropriate JVM heap settings for production environments processing large Excel files.</p>
 *
 * <p><b>Key Features and Capabilities:</b>
 * <ul>
 *   <li><b>Complete Excel Format Support:</b> Full support for XLS (Excel 97-2003) and XLSX (Excel 2007+) formats</li>
 *   <li><b>High-Performance Processing:</b> Optimized reading and writing operations using Apache POI</li>
 *   <li><b>Type-Safe Data Extraction:</b> Automatic type conversion from Excel cells to Java objects</li>
 *   <li><b>Dataset Integration:</b> Seamless conversion between Excel data and Dataset objects for analysis</li>
 *   <li><b>Multi-Sheet Operations:</b> Comprehensive support for multi-worksheet Excel files</li>
 *   <li><b>CSV Conversion:</b> Bidirectional conversion between Excel and CSV formats</li>
 *   <li><b>Custom Cell Processing:</b> Pluggable cell processors for custom data transformations</li>
 *   <li><b>Flexible Column Selection:</b> Support for column filtering, mapping, and selective processing</li>
 * </ul>
 *
 * <p><b>Design Philosophy:</b>
 * <ul>
 *   <li><b>Simplicity First:</b> Clean, intuitive API for common Excel processing tasks</li>
 *   <li><b>Performance Optimized:</b> Efficient memory usage and processing speed for large Excel files</li>
 *   <li><b>Type Safety Priority:</b> Strong typing and automatic type conversion for data integrity</li>
 *   <li><b>Integration Ready:</b> Seamless integration with existing data processing frameworks</li>
 *   <li><b>Production Ready:</b> Robust error handling and resource management for enterprise use</li>
 * </ul>
 *
 * <p><b>Core Operation Categories:</b>
 * <table border="1" style="border-collapse: collapse;">
 *   <caption><b>Excel Operation Types and Methods</b></caption>
 *   <tr style="background-color: #f2f2f2;">
 *     <th>Operation Type</th>
 *     <th>Primary Methods</th>
 *     <th>Memory Usage</th>
 *     <th>Use Cases</th>
 *   </tr>
 *   <tr>
 *     <td>Data Loading</td>
 *     <td>loadSheet()</td>
 *     <td>Medium to High</td>
 *     <td>Reading Excel data into Dataset</td>
 *   </tr>
 *   <tr>
 *     <td>Row Reading</td>
 *     <td>readSheet()</td>
 *     <td>Medium</td>
 *     <td>Reading Excel rows with custom mapping</td>
 *   </tr>
 *   <tr>
 *     <td>Data Writing</td>
 *     <td>writeSheet()</td>
 *     <td>Medium</td>
 *     <td>Creating Excel files from data</td>
 *   </tr>
 *   <tr>
 *     <td>Format Conversion</td>
 *     <td>saveSheetAsCsv()</td>
 *     <td>Low to Medium</td>
 *     <td>Excel to CSV conversion</td>
 *   </tr>
 *   <tr>
 *     <td>Stream Processing</td>
 *     <td>streamSheet()</td>
 *     <td>Low</td>
 *     <td>Memory-efficient processing of large Excel files</td>
 *   </tr>
 * </table>
 *
 * <p><b>Excel Format Support:</b>
 * <ul>
 *   <li><b>XLSX Format:</b> Excel 2007+ format with full feature support (recommended for new files)</li>
 *   <li><b>XLS Format:</b> Legacy Excel 97-2003 format support for backward compatibility</li>
 *   <li><b>Cell Types:</b> String, Numeric, Boolean, Date, Formula, and Blank cell handling</li>
 *   <li><b>Formatting:</b> Basic cell formatting preservation and custom format application</li>
 *   <li><b>Worksheets:</b> Multi-sheet workbooks with named sheet access and creation</li>
 * </ul>
 *
 * <p><b>Common Usage Patterns:</b>
 * <pre>{@code
 * // Load Excel file into Dataset for analysis
 * Dataset dataset = ExcelUtil.loadSheet(new File("sales_data.xlsx"));
 *
 * // Load specific worksheet by name
 * Dataset dataset = ExcelUtil.loadSheet(
 *     new File("report.xlsx"), "Q1_Sales", RowExtractors.DEFAULT);
 *
 * // Write Dataset to Excel file
 * ExcelUtil.writeSheet("Sales Report", dataset, new File("output.xlsx"));
 *
 * // Convert Excel to CSV
 * ExcelUtil.saveSheetAsCsv(new File("data.xlsx"), 0, new File("data.csv"));
 *
 * // Process Excel data with custom row extractor
 * TriConsumer<String[], Row, Object[]> customExtractor = (headers, row, output) -> {
 *     for (int i = 0; i < headers.length; i++) {
 *         Cell cell = row.getCell(i);
 *         if (cell != null && cell.getCellType() == CellType.FORMULA) {
 *             output[i] = cell.getCachedFormulaResultType() == CellType.NUMERIC ?
 *                 cell.getNumericCellValue() : cell.getStringCellValue();
 *         } else {
 *             output[i] = cell != null ? ExcelUtil.CELL_GETTER.apply(cell) : null;
 *         }
 *     }
 * };
 * Dataset dataset = ExcelUtil.loadSheet(new File("data.xlsx"), 0, customExtractor);
 *
 * // Row-based data extraction with custom mapping
 * Function<Row, Employee> employeeMapper = row -> new Employee(
 *     row.getCell(0).getStringCellValue(),   // name
 *     (int) row.getCell(1).getNumericCellValue(),  // age
 *     row.getCell(2).getStringCellValue()    // department
 * );
 * List<Employee> employees = ExcelUtil.readSheet(
 *     new File("employees.xlsx"), 0, true, employeeMapper
 * );
 * }</pre>
 *
 * <p><b>Advanced Processing Patterns:</b>
 * <pre>{@code
 * public class ExcelProcessingService {
 *
 *     // Batch processing of multiple Excel files
 *     public ProcessingResult processBatchFiles(List<File> excelFiles) {
 *         ProcessingResult result = new ProcessingResult();
 *
 *         for (File file : excelFiles) {
 *             try {
 *                 Dataset dataset = ExcelUtil.loadSheet(file);
 *                 dataset = validateAndTransform(dataset);
 *                 saveToDatabase(dataset);
 *                 result.addSuccess(file.getName());
 *             } catch (Exception e) {
 *                 logger.error("Failed to process file: " + file.getName(), e);
 *                 result.addError(file.getName(), e.getMessage());
 *             }
 *         }
 *
 *         return result;
 *     }
 *
 *     // Multi-sheet workbook processing
 *     public Map<String, Dataset> processWorkbook(File excelFile) {
 *         Map<String, Dataset> sheets = new HashMap<>();
 *
 *         try (InputStream is = new FileInputStream(excelFile);
 *              Workbook workbook = WorkbookFactory.create(is)) {
 *             for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
 *                 Sheet sheet = workbook.getSheetAt(i);
 *                 String sheetName = sheet.getSheetName();
 *
 *                 if (!isSkippableSheet(sheetName)) {
 *                     Dataset dataset = ExcelUtil.loadSheet(excelFile, i, RowExtractors.DEFAULT);
 *                     sheets.put(sheetName, dataset);
 *                 }
 *             }
 *         } catch (IOException e) {
 *             throw new RuntimeException("Failed to process workbook: " + excelFile.getName(), e);
 *         }
 *
 *         return sheets;
 *     }
 *
 *     // Dynamic report generation to Excel (single sheet per file)
 *     public void generateExcelReport(ReportCriteria criteria, File outputFile) {
 *         Dataset summaryData = generateSummaryData(criteria);
 *
 *         SheetCreateOptions options = SheetCreateOptions.builder()
 *             .autoSizeColumn(true)
 *             .freezeFirstRow(true)
 *             .autoFilterByFirstRow(true)
 *             .build();
 *
 *         ExcelUtil.writeSheet("Summary", summaryData, options, outputFile);
 *     }
 * }
 * }</pre>
 *
 * <p><b>Type Safety and Cell Processing:</b>
 * <ul>
 *   <li><b>Automatic Type Preservation:</b> Cell values are extracted preserving their Excel types (String, Double, Boolean)</li>
 *   <li><b>Custom Cell Processors:</b> Support for custom cell value conversion via {@code CELL_GETTER} and {@code CELL_TO_STRING}</li>
 *   <li><b>Row Extractors:</b> Pluggable row extraction functions via {@link RowExtractors}</li>
 *   <li><b>Row Mappers:</b> Flexible row-to-object mapping via {@link RowMappers}</li>
 *   <li><b>Null Handling:</b> Configurable empty cell handling through custom extractors</li>
 *   <li><b>Formula Support:</b> Access to formula text from formula cells</li>
 * </ul>
 *
 * <p><b>Performance Considerations:</b>
 * <ul>
 *   <li><b>Streaming Support:</b> Use {@code streamSheet()} for memory-efficient row-by-row processing</li>
 *   <li><b>Resource Cleanup:</b> Stream methods include proper resource cleanup via {@code onClose()}</li>
 *   <li><b>Direct Loading:</b> Use {@code loadSheet()} for smaller files that fit in memory</li>
 *   <li><b>Workbook Format:</b> XLSX files generally perform better than XLS for large datasets</li>
 *   <li><b>Auto-sizing Impact:</b> Disable {@code autoSizeColumn} for large sheets to improve write performance</li>
 * </ul>
 *
 * <p><b>Error Handling:</b>
 * <ul>
 *   <li><b>File Format Detection:</b> Apache POI automatically detects XLS vs XLSX format</li>
 *   <li><b>Unchecked Exceptions:</b> I/O errors wrapped in {@link UncheckedException} for cleaner code</li>
 *   <li><b>Sheet Validation:</b> {@code IllegalArgumentException} thrown for missing sheet names</li>
 *   <li><b>Cell Type Errors:</b> {@code RuntimeException} thrown for unsupported cell types</li>
 *   <li><b>Resource Safety:</b> Proper resource cleanup even when exceptions occur</li>
 * </ul>
 *
 * <p><b>Integration with Abacus Framework:</b>
 * <ul>
 *   <li><b>Dataset Integration:</b> Direct loading into Dataset objects via {@code loadSheet()}</li>
 *   <li><b>CSV Interoperability:</b> Export Excel sheets to CSV format via {@code saveSheetAsCsv()}</li>
 *   <li><b>Stream API:</b> Memory-efficient streaming via {@code streamSheet()} with proper resource cleanup</li>
 *   <li><b>CsvUtil Compatibility:</b> Works alongside {@link CsvUtil} for comprehensive data format support</li>
 * </ul>
 *
 * <p><b>Excel Cell Type Support:</b>
 * <ul>
 *   <li><b>STRING:</b> Text values extracted as Java String</li>
 *   <li><b>NUMERIC:</b> Numbers extracted as Java Double</li>
 *   <li><b>BOOLEAN:</b> Boolean values extracted as Java Boolean</li>
 *   <li><b>FORMULA:</b> Formula text extracted (not evaluated result)</li>
 *   <li><b>BLANK:</b> Empty cells extracted as empty string</li>
 * </ul>
 *
 * <p><b>CSV Export Capabilities:</b>
 * <ul>
 *   <li><b>Excel to CSV:</b> Convert Excel sheets to CSV format via {@code saveSheetAsCsv()}</li>
 *   <li><b>Sheet Selection:</b> Export specific sheets by index or name</li>
 *   <li><b>Custom Headers:</b> Replace Excel headers with custom CSV headers</li>
 *   <li><b>Encoding Support:</b> Proper character encoding handling via Writer parameter</li>
 *   <li><b>Standard Format:</b> Uses comma delimiter with proper CSV escaping</li>
 * </ul>
 *
 * <p><b>Advanced Configuration Options:</b>
 * <pre>{@code
 * // Custom cell processor for complex data extraction
 * Function<Cell, Object> customProcessor = cell -> {
 *     switch (cell.getCellType()) {
 *         case STRING:
 *             return cell.getStringCellValue().trim().toUpperCase();
 *         case NUMERIC:
 *             if (DateUtil.isCellDateFormatted(cell)) {
 *                 return cell.getDateCellValue();
 *             } else {
 *                 return cell.getNumericCellValue();
 *             }
 *         case BOOLEAN:
 *             return cell.getBooleanCellValue();
 *         case FORMULA:
 *             return cell.getCellFormula();
 *         default:
 *             return null;
 *     }
 * };
 *
 * // Load with custom row extractor using the custom processor
 * TriConsumer<String[], Row, Object[]> customExtractor = RowExtractors.create(customProcessor);
 * Dataset dataset = ExcelUtil.loadSheet(new File("data.xlsx"), 0, customExtractor);
 *
 * // Advanced Excel writing with formatting options
 * SheetCreateOptions options = SheetCreateOptions.builder()
 *     .autoSizeColumn(true)
 *     .freezeFirstRow(true)
 *     .autoFilterByFirstRow(true)
 *     .build();
 *
 * ExcelUtil.writeSheet("Employee Report", dataset, options, new File("report.xlsx"));
 * }</pre>
 *
 * <p><b>Resource Management:</b>
 * <ul>
 *   <li><b>Automatic Cleanup:</b> {@code loadSheet()}, {@code readSheet()}, and {@code writeSheet()} methods handle resource cleanup internally</li>
 *   <li><b>Stream Cleanup:</b> {@code streamSheet()} returns streams with {@code onClose()} handlers - always use try-with-resources</li>
 *   <li><b>File Handles:</b> All file operations properly close input/output streams after completion</li>
 *   <li><b>Workbook Resources:</b> Apache POI Workbook objects are closed automatically after processing</li>
 * </ul>
 *
 * <p><b>Best Practices and Recommendations:</b>
 * <ul>
 *   <li>Use try-with-resources for automatic resource cleanup when working with Workbook objects</li>
 *   <li>Prefer XLSX format over XLS for better performance and larger file support</li>
 *   <li>Implement proper error handling for corrupted or malformed Excel files</li>
 *   <li>Configure appropriate JVM heap settings for processing large Excel files</li>
 *   <li>Use streaming methods for memory-constrained environments</li>
 *   <li>Validate Excel file structure before processing in production environments</li>
 *   <li>Consider using custom cell processors for complex data transformation requirements</li>
 *   <li>Monitor memory usage when processing multiple large Excel files concurrently</li>
 * </ul>
 *
 * <p><b>Common Anti-Patterns to Avoid:</b>
 * <ul>
 *   <li>Loading multiple large Excel files simultaneously without memory management</li>
 *   <li>Not handling Excel formula evaluation errors properly</li>
 *   <li>Ignoring cell type validation during data extraction</li>
 *   <li>Not disposing of Workbook resources in long-running applications</li>
 *   <li>Using inappropriate data types for Excel cell value conversion</li>
 *   <li>Processing Excel files without considering file format compatibility</li>
 *   <li>Not implementing proper exception handling for I/O operations</li>
 * </ul>
 *
 * <p><b>Security Considerations:</b>
 * <ul>
 *   <li><b>File Validation:</b> Validate Excel file format and structure before processing</li>
 *   <li><b>Input Sanitization:</b> Sanitize Excel data to prevent injection attacks</li>
 *   <li><b>Resource Limits:</b> Implement limits on file size and processing time</li>
 *   <li><b>Access Control:</b> Ensure proper file access permissions and security</li>
 *   <li><b>Data Privacy:</b> Handle sensitive data in Excel files according to privacy requirements</li>
 * </ul>
 *
 * <p><b>Thread Safety:</b>
 * <ul>
 *   <li><b>Stateless Operations:</b> All utility methods are stateless and thread-safe</li>
 *   <li><b>Concurrent Access:</b> Multiple threads can safely use ExcelUtil methods simultaneously</li>
 *   <li><b>Resource Isolation:</b> Each operation uses isolated POI resources</li>
 *   <li><b>No Shared State:</b> No static mutable state that could cause thread safety issues</li>
 * </ul>
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
     * formula text), and empty string for BLANK cells.
     *
     * <p>This extractor is commonly used as the default cell processor for reading Excel data
     * when you want to preserve the original cell types without conversion. It's ideal for
     * generic data loading scenarios where type preservation is important.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use directly on a cell
     * Cell cell = row.getCell(0);
     * Object value = CELL_GETTER.apply(cell);
     *
     * // Use with row mapper
     * Function<Row, List<Object>> mapper = RowMappers.toList(CELL_GETTER);
     * List<List<Object>> rows = ExcelUtil.readSheet(file, 0, true, mapper);
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
     * This function normalizes all cell types to {@code String}, making it useful for text-based
     * processing, CSV conversion, or when uniform string representation is required. Handles all
     * standard cell types including STRING, NUMERIC, BOOLEAN, FORMULA, and BLANK cells.
     *
     * <p>Numeric values are converted using {@code String.valueOf()}, boolean values are converted
     * to "true" or "false", formula cells return the formula text (not the evaluated result), and
     * blank cells return an empty string. This converter is ideal for export operations and text-based
     * data processing where type information is not critical.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use directly on a cell
     * Cell cell = row.getCell(0);
     * String stringValue = CELL_TO_STRING.apply(cell);
     *
     * // Use with row mapper to get all values as strings
     * Function<Row, List<String>> stringMapper = RowMappers.toList(CELL_TO_STRING);
     * List<List<String>> rows = ExcelUtil.readSheet(file, 0, true, stringMapper);
     * }</pre>
     */
    public static final Function<Cell, String> CELL_TO_STRING = cell -> switch (cell.getCellType()) {
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
     * @param excelFile the Excel file to read, must exist and be a valid Excel file.
     * @return a Dataset containing the sheet data with the first row as column names, empty Dataset if sheet is empty.
     * @throws UncheckedException if an I/O error occurs while reading the file, or if the file is not a valid Excel file.
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
     * @param excelFile the Excel file to read, must exist and be a valid Excel file.
     * @param sheetIndex the zero-based index of the sheet to read (0 for first sheet).
     * @param rowExtractor custom function to extract row data. Receives three parameters:
     *                     column headers array, current row, and output array to populate with extracted values.
     * @return a Dataset containing the extracted sheet data with the first row as column names.
     * @throws UncheckedException if an I/O error occurs while reading the file, if the file is not a valid Excel file,
     *                            or if the sheet index is out of bounds.
     */
    public static Dataset loadSheet(final File excelFile, final int sheetIndex,
            final TriConsumer<? super String[], ? super Row, ? super Object[]> rowExtractor) {
        try (InputStream is = new FileInputStream(excelFile); //
             Workbook workbook = WorkbookFactory.create(is)) {
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
     * @param excelFile the Excel file to read, must exist and be a valid Excel file.
     * @param sheetName the name of the sheet to read, case-sensitive.
     * @param rowExtractor custom function to extract row data. Receives three parameters:
     *                     column headers array, current row, and output array to populate with extracted values.
     * @return a Dataset containing the extracted sheet data with the first row as column names.
     * @throws UncheckedException if an I/O error occurs, if the file is not a valid Excel file,
     *                            or if the sheet name is not found in the workbook.
     */
    public static Dataset loadSheet(final File excelFile, final String sheetName,
            final TriConsumer<? super String[], ? super Row, ? super Object[]> rowExtractor) {
        try (InputStream is = new FileInputStream(excelFile); //
             Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = getRequiredSheet(workbook, sheetName);
            return loadSheet(sheet, rowExtractor);

        } catch (IOException e) {
            throw new UncheckedException(e);
        }

    }

    private static Sheet getRequiredSheet(final Workbook workbook, final String sheetName) {
        final Sheet sheet = workbook.getSheet(sheetName);

        if (sheet == null) {
            throw new IllegalArgumentException("Sheet not found: " + sheetName);
        }

        return sheet;
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
            headers[i] = CELL_TO_STRING.apply(headerRow.getCell(i));
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
     * @param excelFile the Excel file to read, must exist and be a valid Excel file.
     * @return a list of rows, where each row is a list of cell values with preserved types.
     * @throws UncheckedException if an I/O error occurs while reading the file or if the file is not a valid Excel file.
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
     * @param <T> the type of objects to map rows to.
     * @param excelFile the Excel file to read, must exist and be a valid Excel file.
     * @param sheetIndex the zero-based index of the sheet to read (0 for first sheet).
     * @param skipFirstRow {@code true} to skip the first row (typically headers), {@code false} to process all rows.
     * @param rowMapper function to convert each Row to an object of type T.
     * @return a list of mapped objects, one per row (excluding skipped rows).
     * @throws UncheckedException if an I/O error occurs while reading the file, if the file is not a valid Excel file,
     *                            or if the sheet index is out of bounds.
     */
    public static <T> List<T> readSheet(final File excelFile, final int sheetIndex, final boolean skipFirstRow,
            final Function<? super Row, ? extends T> rowMapper) {
        try (InputStream is = new FileInputStream(excelFile); //
             Workbook workbook = WorkbookFactory.create(is)) {
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
     * @param <T> the type of objects to map rows to.
     * @param excelFile the Excel file to read, must exist and be a valid Excel file.
     * @param sheetName the name of the sheet to read, case-sensitive.
     * @param skipFirstRow {@code true} to skip the first row (typically headers), {@code false} to process all rows.
     * @param rowMapper function to convert each Row to an object of type T.
     * @return a list of mapped objects, one per row (excluding skipped rows).
     * @throws UncheckedException if an I/O error occurs, if the file is not a valid Excel file,
     *                            or if the sheet name is not found in the workbook.
     */
    public static <T> List<T> readSheet(final File excelFile, final String sheetName, final boolean skipFirstRow,
            final Function<? super Row, ? extends T> rowMapper) {
        try (InputStream is = new FileInputStream(excelFile); //
             Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = getRequiredSheet(workbook, sheetName);
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
        InputStream is = null;
        Workbook workbook = null;
        Stream<Row> result = null;

        try {
            is = new FileInputStream(excelFile);
            workbook = WorkbookFactory.create(is);
            final Sheet sheet = workbook.getSheetAt(sheetIndex);
            final Workbook workbookToClose = workbook;
            final InputStream inputStreamToClose = is;

            result = Stream.of(sheet.rowIterator()).skip(skipFirstRow ? 1 : 0).onClose(() -> {
                try {
                    workbookToClose.close();
                } catch (IOException e) {
                    throw new UncheckedException(e);
                } finally {
                    IOUtil.closeQuietly(inputStreamToClose);
                }
            });

            return result;
        } catch (IOException e) {
            throw new UncheckedException(e);
        } finally {
            if (result == null) {
                IOUtil.closeQuietly(workbook);
                IOUtil.closeQuietly(is);
            }
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
        InputStream is = null;
        Workbook workbook = null;
        Stream<Row> result = null;

        try {
            is = new FileInputStream(excelFile);
            workbook = WorkbookFactory.create(is);
            final Sheet sheet = getRequiredSheet(workbook, sheetName);
            final Workbook workbookToClose = workbook;
            final InputStream inputStreamToClose = is;

            result = Stream.of(sheet.rowIterator()).skip(skipFirstRow ? 1 : 0).onClose(() -> {
                try {
                    workbookToClose.close();
                } catch (IOException e) {
                    throw new UncheckedException(e);
                } finally {
                    IOUtil.closeQuietly(inputStreamToClose);
                }
            });

            return result;
        } catch (IOException e) {
            throw new UncheckedException(e);
        } finally {
            if (result == null) {
                IOUtil.closeQuietly(workbook);
                IOUtil.closeQuietly(is);
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
     * Date, Calendar, Integer, and Double are set using type-specific cell methods, while
     * other types are converted to strings.</p>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * List<Object> headers = List.of("Name", "Age", "City");
     * List<List<Object>> rows = List.of(
     *     List.of("John", 30, "New York"),
     *     List.of("Jane", 25, "London")
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
    public static void writeSheet(final String sheetName, final List<?> headers, final List<? extends Collection<?>> rows, final File outputExcelFile) {
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
     * List<Object> headers = List.of("ID", "Name", "Value");
     * List<List<Object>> data = List.of(
     *     List.of(1, "Item1", 100.0),
     *     List.of(2, "Item2", 200.0)
     * );
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
    public static void writeSheet(final String sheetName, final List<?> headers, final List<? extends Collection<?>> rows,
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
     * @param sheetName the name of the sheet to create in the workbook.
     * @param headers the column headers as a list of objects (will be converted to strings).
     * @param rows the data rows, where each row is a collection of cell values.
     * @param sheetSetter a consumer to apply custom formatting to the sheet after data is written (null to skip).
     * @param outputExcelFile the file to write the Excel data to (will be created or overwritten).
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created.
     */
    public static void writeSheet(final String sheetName, final List<?> headers, final List<? extends Collection<?>> rows,
            final Consumer<? super Sheet> sheetSetter, final File outputExcelFile) {
        try (Workbook workbook = newWorkbookForOutput(outputExcelFile)) {
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
            }
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
     * Dataset dataset = CsvUtil.load(new File("data.csv"));
     * ExcelUtil.writeSheet("ImportedData", dataset, new File("output.xlsx"));
     * }</pre>
     *
     * @param sheetName the name of the sheet to create in the workbook.
     * @param dataset the Dataset containing the data to write, must not be null.
     * @param outputExcelFile the file to write the Excel data to (will be created or overwritten).
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created.
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
     * @param sheetName the name of the sheet to create in the workbook.
     * @param dataset the Dataset containing the data to write, must not be null.
     * @param sheetCreateOptions configuration options for sheet formatting (null to apply no formatting).
     * @param outputExcelFile the file to write the Excel data to (will be created or overwritten).
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created.
     */
    public static void writeSheet(final String sheetName, final Dataset dataset, final SheetCreateOptions sheetCreateOptions, final File outputExcelFile) {
        final int columnCount = dataset.columnCount();

        final Consumer<Sheet> sheetSetter = createSheetSetter(sheetCreateOptions, columnCount);

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
     * @param sheetName the name of the sheet to create in the workbook.
     * @param dataset the Dataset containing the data to write, must not be null.
     * @param sheetSetter a consumer to apply custom formatting to the sheet after data is written (null to skip).
     * @param outputExcelFile the file to write the Excel data to (will be created or overwritten).
     * @throws UncheckedException if an I/O error occurs while writing the file or if the file cannot be created.
     */
    public static void writeSheet(final String sheetName, final Dataset dataset, final Consumer<? super Sheet> sheetSetter, final File outputExcelFile) {
        try (Workbook workbook = newWorkbookForOutput(outputExcelFile)) {
            Sheet sheet = workbook.createSheet(sheetName);

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

            // Write to file
            try (OutputStream os = new FileOutputStream(outputExcelFile)) {
                workbook.write(os);
            }
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
        } else if (cellValue instanceof LocalDate val) {
            cell.setCellValue(val);
        } else if (cellValue instanceof LocalDateTime val) {
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

    private static Workbook newWorkbookForOutput(final File outputExcelFile) {
        final String name = outputExcelFile == null ? "" : outputExcelFile.getName();
        final int dot = name.lastIndexOf('.');
        final String extension = dot >= 0 ? name.substring(dot + 1).toLowerCase(Locale.ROOT) : "";

        if ("xls".equals(extension)) {
            return new HSSFWorkbook();
        }

        return new XSSFWorkbook();
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
    public static void saveSheetAsCsv(final File excelFile, final int sheetIndex, final File outputCsvFile) {
        try (Writer writer = IOUtil.newFileWriter(outputCsvFile)) {
            saveSheetAsCsv(excelFile, sheetIndex, null, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
    public static void saveSheetAsCsv(final File excelFile, final String sheetName, final File outputCsvFile) {
        try (Writer writer = IOUtil.newFileWriter(outputCsvFile)) {
            saveSheetAsCsv(excelFile, sheetName, null, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
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
     * List<String> customHeaders = List.of("Product ID", "Product Name", "Price");
     * ExcelUtil.saveSheetAsCsv(
     *     new File("products.xlsx"),
     *     0,
     *     customHeaders,
     *     IOUtil.newFileWriter(new File("products.csv"))
     * );
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @param sheetIndex the zero-based index of the sheet to convert (0 for first sheet)
     * @param csvHeaders custom headers for the CSV file (null to use original Excel headers from first row)
     * @param outputWriter the CSV file to write to (will be created or overwritten) 
     * @throws UncheckedException if an I/O error occurs during conversion, if the file is not a valid Excel file,
     *                            or if the sheet index is out of bounds
     */
    public static void saveSheetAsCsv(final File excelFile, final int sheetIndex, final List<String> csvHeaders, final Writer outputWriter) {
        try (InputStream is = new FileInputStream(excelFile); //
             Workbook workbook = WorkbookFactory.create(is)) {
            final Sheet sheet = workbook.getSheetAt(sheetIndex);

            saveSheetAsCsv(sheet, csvHeaders, outputWriter);
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
     * List<String> headers = List.of("Código", "Nombre", "Precio");
     * ExcelUtil.saveSheetAsCsv(
     *     new File("productos.xlsx"),
     *     "Inventario",
     *     headers,
     *     IOUtil.newFileWriter(new File("productos.csv"))
     * );
     * }</pre>
     *
     * @param excelFile the Excel file to read, must exist and be a valid Excel file
     * @param sheetName the name of the sheet to convert, case-sensitive
     * @param csvHeaders custom headers for the CSV file (null to use original Excel headers from first row)
     * @param outputWriter the CSV writer to write to (will be created or overwritten) 
     * @throws UncheckedException if an I/O error occurs, if the file is not a valid Excel file,
     *                            or if the sheet name is not found in the workbook
     */
    public static void saveSheetAsCsv(final File excelFile, final String sheetName, List<String> csvHeaders, final Writer outputWriter) {
        try (InputStream is = new FileInputStream(excelFile); //
             Workbook workbook = WorkbookFactory.create(is)) {
            final Sheet sheet = getRequiredSheet(workbook, sheetName);

            saveSheetAsCsv(sheet, csvHeaders, outputWriter);
        } catch (IOException e) {
            throw new UncheckedException(e);
        }
    }

    private static void saveSheetAsCsv(final Sheet sheet, List<String> csvHeaders, final Writer output) throws IOException {
        final Type<Object> strType = Type.of(String.class);
        final char separator = WD._COMMA;

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
            int rowIndex = skipFirstRow ? 1 : 0;

            for (Row row : sheet) {
                if (skipFirstRow) {
                    skipFirstRow = false;
                    continue;
                }

                if (rowIndex++ > 0) {
                    bw.write(IOUtil.LINE_SEPARATOR_UNIX);
                }

                int idx = 0;

                for (Cell cell : row) {
                    if (idx++ > 0) {
                        bw.write(separator);
                    }

                    switch (cell.getCellType()) {
                        case STRING -> CsvUtil.writeField(bw, strType, cell.getStringCellValue());
                        case NUMERIC -> CsvUtil.writeField(bw, null, cell.getNumericCellValue());
                        case BOOLEAN -> CsvUtil.writeField(bw, null, cell.getBooleanCellValue());
                        case FORMULA -> CsvUtil.writeField(bw, strType, cell.getCellFormula());
                        case BLANK -> CsvUtil.writeField(bw, strType, "");
                        default -> throw new RuntimeException("Unsupported cell type: " + cell.getCellType());
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
     * representations. These mappers are designed for use with {@code readSheet} methods to control
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
     * // Use default mapper to get list of objects with type preservation
     * List<List<Object>> rows = ExcelUtil.readSheet(
     *     new File("data.xlsx"),
     *     0,
     *     true,  // Skip header row
     *     RowMappers.DEFAULT
     * );
     *
     * // Convert rows to comma-separated strings
     * List<String> rowStrings = ExcelUtil.readSheet(
     *     new File("data.xlsx"),
     *     0,
     *     false,
     *     RowMappers.ROW2STRING
     * );
     *
     * // Create custom mapper with pipe separator
     * Function<Row, String> pipeMapper = RowMappers.toString("|");
     * List<String> pipeDelimited = ExcelUtil.readSheet(file, 0, true, pipeMapper);
     *
     * // Create strongly-typed mapper for numeric data
     * Function<Cell, Double> numericExtractor = cell ->
     *     cell.getCellType() == CellType.NUMERIC ? cell.getNumericCellValue() : 0.0;
     * Function<Row, List<Double>> numericMapper = RowMappers.toList(numericExtractor);
     * List<List<Double>> numericData = ExcelUtil.readSheet(file, 0, true, numericMapper);
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
         * List<List<Object>> rows = ExcelUtil.readSheet(
         *     new File("data.xlsx"),
         *     0,
         *     true,  // Skip first row (headers)
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
         * List<String> rowStrings = ExcelUtil.readSheet(
         *     new File("data.xlsx"),
         *     "Sheet1",
         *     false,
         *     RowMappers.ROW2STRING
         * );
         *
         * // Print or log row data
         * rowStrings.forEach(System.out::println);
         * }</pre>
         */
        public static final Function<Row, String> ROW2STRING = toDelimitedString(Strings.ELEMENT_SEPARATOR);

        /**
         * Creates a row mapper that converts rows to delimited strings with a custom separator.
         * Each cell is converted to its string representation using the default CELL_TO_STRING mapper
         * and the resulting strings are joined together with the specified separator. Useful for
         * creating custom text representations of Excel rows.
         *
         * <p>This method uses CELL_TO_STRING for cell conversion, which handles all cell types
         * (STRING, NUMERIC, BOOLEAN, FORMULA, BLANK) and converts them to appropriate string
         * representations.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Function<Row, String> pipeMapper = RowMappers.toDelimitedString("|");
         * List<String> rows = ExcelUtil.readSheet(file, 0, true, pipeMapper);
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
         * <p>The cellMapper function is applied to each cell individually, giving you complete
         * control over formatting numbers, dates, handling {@code null} cells, or applying business logic
         * during the conversion process.</p>
         *
         * <p><b>Usage Examples:</b></p>
         * <pre>{@code
         * Function<Cell, String> customCellMapper = cell ->
         *     cell.getCellType() == CellType.NUMERIC
         *         ? String.format("%.2f", cell.getNumericCellValue())
         *         : CELL_TO_STRING.apply(cell);
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
     * Collection of row extraction functions specifically designed for use with {@code loadSheet} methods.
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
     *   <li><b>Use Case:</b> Extractors with {@code loadSheet()}; mappers with {@code readSheet()}</li>
     * </ul>
     *
     * <p><b>Usage Examples:</b></p>
     * <pre>{@code
     * // Use default extractor for standard Dataset loading
     * Dataset ds = ExcelUtil.loadSheet(
     *     new File("data.xlsx"),
     *     0,
     *     RowExtractors.DEFAULT
     * );
     *
     * // Create custom extractor for specific processing (e.g., uppercase conversion)
     * TriConsumer<String[], Row, Object[]> uppercaseExtractor =
     *     RowExtractors.create(cell ->
     *         cell.getCellType() == CellType.STRING
     *             ? cell.getStringCellValue().toUpperCase()
     *             : ExcelUtil.CELL_GETTER.apply(cell)
     *     );
     * Dataset ds = ExcelUtil.loadSheet(file, 0, uppercaseExtractor);
     *
     * // Create extractor with null handling
     * TriConsumer<String[], Row, Object[]> safeExtractor =
     *     RowExtractors.create(cell ->
     *         cell == null ? "" : ExcelUtil.CELL_GETTER.apply(cell)
     *     );
     * Dataset ds = ExcelUtil.loadSheet(file, "Sheet1", safeExtractor);
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
         * Dataset dataset = ExcelUtil.loadSheet(
         *     new File("sales.xlsx"),
         *     0,  // First sheet
         *     RowExtractors.DEFAULT
         * );
         *
         * // Access data with original types preserved
         * List<Object> productCol = dataset.getColumn("Product");   // Strings
         * List<Object> priceCol = dataset.getColumn("Price");       // Doubles
         * }</pre>
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
         *         cell == null ? "" : CELL_TO_STRING.apply(cell)
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
     * // Basic formatted report with auto-sizing and frozen header
     * SheetCreateOptions basicOptions = SheetCreateOptions.builder()
     *     .autoSizeColumn(true)
     *     .freezeFirstRow(true)
     *     .autoFilterByFirstRow(true)
     *     .build();
     *
     * ExcelUtil.writeSheet("Sales Report", data, basicOptions, new File("report.xlsx"));
     *
     * // Advanced formatting with custom freeze pane
     * SheetCreateOptions advancedOptions = SheetCreateOptions.builder()
     *     .autoSizeColumn(true)
     *     .freezePane(new FreezePane(2, 1))  // Freeze first 2 columns and first row
     *     .autoFilter(new CellRangeAddress(0, 0, 0, 5))  // Filter first 6 columns
     *     .build();
     *
     * ExcelUtil.writeSheet("Detailed Analysis", dataset, advancedOptions, new File("analysis.xlsx"));
     *
     * // Default (no formatting) - fastest option
     * SheetCreateOptions defaultOptions = new SheetCreateOptions();
     * ExcelUtil.writeSheet("Raw Data", data, defaultOptions, new File("raw.xlsx"));
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
         * <p><b>Example:</b>
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
     * // Freeze first column and first row (most common pattern)
     * FreezePane headerAndId = new FreezePane(1, 1);
     * SheetCreateOptions options = SheetCreateOptions.builder()
     *     .freezePane(headerAndId)
     *     .build();
     *
     * // Freeze only the header row for wide datasets
     * FreezePane headerOnly = new FreezePane(0, 1);
     * ExcelUtil.writeSheet("Data", dataset, SheetCreateOptions.builder()
     *     .freezePane(headerOnly)
     *     .autoFilterByFirstRow(true)
     *     .build(), new File("output.xlsx"));
     *
     * // Freeze first two columns for comparison
     * FreezePane twoColumns = new FreezePane(2, 0);
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
