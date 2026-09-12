package com.landawn.abacus.poi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Date;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

/**
 * B2 regression tests: {@code exportSheetToCsv} used to emit the raw {@code double} behind every NUMERIC
 * cell, so integers came out as {@code 1001.0} and date cells as their serial number
 * ({@code 45244.59259259259}). Numeric cells are now rendered through POI's {@code DataFormatter}.
 */
public class ExcelUtilCsvExportFixTest extends TestBase {

    @TempDir
    Path tempDir;

    /** 2023-11-14T12:33:20Z - an instant with a non-zero time component, so a date-only format must truncate it. */
    private static final long FIXED_MILLIS = 1_700_000_000_000L;

    private File writeWorkbook(final String name) throws Exception {
        final File f = tempDir.resolve(name).toFile();

        try (Workbook wb = new XSSFWorkbook()) {
            final Sheet sh = wb.createSheet("S1");

            final Row header = sh.createRow(0);
            header.createCell(0).setCellValue("id");
            header.createCell(1).setCellValue("qty");
            header.createCell(2).setCellValue("when");
            header.createCell(3).setCellValue("flag");
            header.createCell(4).setCellValue("ratio");

            final CellStyle dateStyle = wb.createCellStyle();
            dateStyle.setDataFormat(wb.createDataFormat().getFormat("yyyy-mm-dd"));

            final Row r = sh.createRow(1);
            r.createCell(0).setCellValue(1001);
            r.createCell(1).setCellValue(7);

            final Cell d = r.createCell(2);
            d.setCellValue(new Date(FIXED_MILLIS));
            d.setCellStyle(dateStyle);

            r.createCell(3).setCellValue(true);
            r.createCell(4).setCellValue(3.5);

            try (OutputStream os = new FileOutputStream(f)) {
                wb.write(os);
            }
        }

        return f;
    }

    @Test
    public void b2_integerCellsAreNotExportedWithASpuriousDecimal() throws Exception {
        final File f = writeWorkbook("ints.xlsx");
        final StringWriter out = new StringWriter();

        ExcelUtil.exportSheetToCsv(f, 0, Arrays.asList("id", "qty", "when", "flag", "ratio"), out);

        final String csv = out.toString();
        final String dataRow = csv.split("\n")[1];

        assertTrue(dataRow.contains("\"1001\""), "an integer cell must not become 1001.0, was: " + dataRow);
        assertTrue(dataRow.contains("\"7\""), "an integer cell must not become 7.0, was: " + dataRow);
        assertFalse(dataRow.contains("1001.0"), "was: " + dataRow);
        assertFalse(dataRow.contains("7.0"), "was: " + dataRow);
    }

    @Test
    public void b2_dateCellsAreExportedAsDatesNotSerialNumbers() throws Exception {
        final File f = writeWorkbook("dates.xlsx");
        final StringWriter out = new StringWriter();

        ExcelUtil.exportSheetToCsv(f, 0, Arrays.asList("id", "qty", "when", "flag", "ratio"), out);

        final String dataRow = out.toString().split("\n")[1];

        assertTrue(dataRow.contains("2023-11-14"), "a date-formatted cell must render as a date, was: " + dataRow);
        assertFalse(dataRow.contains("45244"), "the serial number must not leak into the CSV, was: " + dataRow);
    }

    @Test
    public void b2_nonIntegralNumbersAndBooleansAreUnchanged() throws Exception {
        final File f = writeWorkbook("mixed.xlsx");
        final StringWriter out = new StringWriter();

        ExcelUtil.exportSheetToCsv(f, 0, Arrays.asList("id", "qty", "when", "flag", "ratio"), out);

        final String dataRow = out.toString().split("\n")[1];

        // A genuinely fractional value keeps its fraction ...
        assertTrue(dataRow.contains("3.5"), "was: " + dataRow);
        // ... and booleans keep the lower-case rendering this class has always produced (DataFormatter
        // would have produced "TRUE"), so only NUMERIC cells changed.
        assertTrue(dataRow.contains("true"), "was: " + dataRow);
        assertFalse(dataRow.contains("TRUE"), "was: " + dataRow);
    }

    @Test
    public void b2_headerRowAndFieldCountAreUnchanged() throws Exception {
        final File f = writeWorkbook("shape.xlsx");
        final StringWriter out = new StringWriter();

        ExcelUtil.exportSheetToCsv(f, 0, Arrays.asList("id", "qty", "when", "flag", "ratio"), out);

        final String[] lines = out.toString().split("\n");

        assertEquals(2, lines.length, "one header record and one data record");
        assertEquals("\"id\",\"qty\",\"when\",\"flag\",\"ratio\"", lines[0]);
        assertEquals(5, lines[1].split(",", -1).length, "every record must keep the same field count");
    }

    @Test
    public void b2_exportedCsvRoundTripsThroughCsvUtil() throws Exception {
        final File f = writeWorkbook("roundtrip.xlsx");
        final StringWriter out = new StringWriter();

        ExcelUtil.exportSheetToCsv(f, 0, Arrays.asList("id", "qty", "when", "flag", "ratio"), out);

        // The whole point of quoting the formatted number: the CSV must still parse.
        final com.landawn.abacus.util.Dataset ds = com.landawn.abacus.util.CsvUtil.load(new java.io.StringReader(out.toString()));

        assertEquals(Arrays.asList("id", "qty", "when", "flag", "ratio"), ds.columnNames());
        assertEquals(1, ds.size());
        assertEquals("1001", ds.getRow(0).get(0));
        assertEquals("2023-11-14", ds.getRow(0).get(2));
    }

    @Test
    public void b2_readersStillExposeTheUnderlyingDoubleValue() throws Exception {
        final File f = writeWorkbook("readers.xlsx");

        // The reader families are deliberately unchanged: they expose the cell's underlying value.
        final com.landawn.abacus.util.Dataset ds = ExcelUtil.readDatasetFromSheet(f);

        assertEquals(Double.valueOf(1001d), ds.getRow(0).get(0));
        assertTrue(ds.getRow(0).get(2) instanceof Double, "date cells still read back as the serial number");
    }
}
