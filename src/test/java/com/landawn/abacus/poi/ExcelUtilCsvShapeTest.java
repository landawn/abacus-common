package com.landawn.abacus.poi;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.File;
import java.io.OutputStream;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;

import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.poi.ExcelUtil.ExcelFormat;

public class ExcelUtilCsvShapeTest extends TestBase {
    @TempDir
    Path tempDir;

    private File workbook(final ExcelFormat format, final Consumer<Sheet> populate) throws Exception {
        final Path path = tempDir.resolve("source." + format.name().toLowerCase(java.util.Locale.ROOT));
        try (Workbook workbook = format == ExcelFormat.XLS ? new HSSFWorkbook() : new XSSFWorkbook()) {
            populate.accept(workbook.createSheet("Data"));
            try (OutputStream output = Files.newOutputStream(path)) {
                workbook.write(output);
            }
        }
        return path.toFile();
    }

    private String export(final File file, final List<String> headers) {
        final StringWriter output = new StringWriter();
        ExcelUtil.exportSheetToCsv(file, 0, headers, output);
        return output.toString();
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void shortReplacementHeadersArePaddedWithoutLosingSparseData(final ExcelFormat format) throws Exception {
        final File file = workbook(format, sheet -> {
            sheet.createRow(3).createCell(0).setCellValue("old");
            sheet.createRow(5).createCell(2).setCellValue("\uD83D\uDE80,\"value\"\nnext");
            sheet.createRow(7);
        });
        final List<String> headers = List.of("\u540D\u79F0");
        assertEquals("\"\u540D\u79F0\",\"\",\"\"\n\"\",\"\",\"\uD83D\uDE80,\"\"value\"\"\nnext\"\n\"\",\"\",\"\"", export(file, headers));
        assertEquals(List.of("\u540D\u79F0"), headers);
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void widerReplacementHeadersPadDataAndKeepNullAndEmptyLabels(final ExcelFormat format) throws Exception {
        final File file = workbook(format, sheet -> {
            sheet.createRow(0).createCell(0).setCellValue("old");
            sheet.createRow(1).createCell(0).setCellValue("value");
        });
        assertEquals("\"name\",null,\"\"\n\"value\",\"\",\"\"", export(file, Arrays.asList("name", null, "")));
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void skippedHeaderWidthDoesNotPadReplacementWhenThereIsNoData(final ExcelFormat format) throws Exception {
        final File file = workbook(format, sheet -> sheet.createRow(8).createCell(7).setCellValue("discarded"));
        assertEquals("\"new\"", export(file, List.of("new")));
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void emptySheetMayStillHaveReplacementHeaders(final ExcelFormat format) throws Exception {
        final File file = workbook(format, sheet -> {
        });
        assertEquals("\"new\"", export(file, List.of("new")));
        assertEquals("", export(file, null));
        assertEquals("", export(file, List.of()));
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void nullAndEmptyHeaderListsPreserveOriginalRows(final ExcelFormat format) throws Exception {
        final File file = workbook(format, sheet -> {
            sheet.createRow(2).createCell(0).setCellValue("original");
            sheet.createRow(4).createCell(1).setCellValue("right");
        });
        final String expected = "\"original\",\"\"\n\"\",\"right\"";
        assertEquals(expected, export(file, null));
        assertEquals(expected, export(file, List.of()));
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    public void namedExportUsesTheSameRecordWidth(final ExcelFormat format) throws Exception {
        final File file = workbook(format, sheet -> {
            sheet.createRow(0).createCell(0).setCellValue("old");
            sheet.createRow(1).createCell(1).setCellValue("last");
        });
        final StringWriter output = new StringWriter();
        ExcelUtil.exportSheetToCsv(file, "Data", List.of("new"), output);
        assertEquals("\"new\",\"\"\n\"\",\"last\"", output.toString());
    }
}
