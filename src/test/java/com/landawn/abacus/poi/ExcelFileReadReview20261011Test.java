package com.landawn.abacus.poi;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

import org.apache.poi.openxml4j.opc.PackageAccess;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.poi.ExcelUtil.ExcelFormat;
import com.landawn.abacus.util.stream.Stream;

@Tag("unit")
class ExcelFileReadReview20261011Test extends TestBase {
    @TempDir
    Path directory;

    private File workbook(final ExcelFormat format) {
        final File file = directory.resolve("source." + format.name().toLowerCase(Locale.ROOT)).toFile();
        ExcelUtil.writeRowsToSheet("Data", List.of("header"), List.of(List.of("first"), List.of("second")), file);
        return file;
    }

    @Test
    void xlsxFileReadersUseReadOnlyPackages() {
        final File file = workbook(ExcelFormat.XLSX);
        for (boolean named : List.of(false, true)) {
            final var mapper = (java.util.function.Function<Row, String>) row -> {
                assertReadOnly(row);
                return row.getCell(0).getStringCellValue();
            };
            assertEquals(List.of("first", "second"),
                    named ? ExcelUtil.readRowsFromSheet(file, "data", true, mapper) : ExcelUtil.readRowsFromSheet(file, 0, true, mapper));
            final var extractor = (com.landawn.abacus.util.function.TriConsumer<String[], Row, Object[]>) (headers, row, output) -> {
                assertReadOnly(row);
                output[0] = row.getCell(0).getStringCellValue();
            };
            assertEquals("second",
                    (named ? ExcelUtil.readDatasetFromSheet(file, "data", extractor) : ExcelUtil.readDatasetFromSheet(file, 0, extractor)).get(1, 0));
            try (Stream<Row> rows = named ? ExcelUtil.streamRowsFromSheet(file, "data", true) : ExcelUtil.streamRowsFromSheet(file, 0, true)) {
                rows.forEach(ExcelFileReadReview20261011Test::assertReadOnly);
            }
        }
    }

    private static void assertReadOnly(final Row row) {
        final XSSFWorkbook workbook = assertInstanceOf(XSSFWorkbook.class, row.getSheet().getWorkbook());
        assertEquals(PackageAccess.READ, workbook.getPackage().getPackageAccess(),
                "File readers must not buffer an editable copy of the XLSX package or save changes on close");
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    void callbacksAndEarlyStreamCloseLeaveSourceUnchangedAndReleaseIt(final ExcelFormat format) throws Exception {
        final File file = workbook(format);
        final byte[] original = Files.readAllBytes(file.toPath());
        ExcelUtil.readRowsFromSheet(file, 0, true, row -> {
            row.getCell(0).setCellValue("changed in memory");
            return row.getRowNum();
        });
        ExcelUtil.readDatasetFromSheet(file, "data", (headers, row, output) -> {
            row.getCell(0).setCellValue("also changed in memory");
            output[0] = row.getRowNum();
        });
        try (Stream<Row> rows = ExcelUtil.streamRowsFromSheet(file, "data", true)) {
            rows.limit(1).forEach(row -> row.getCell(0).setCellValue("early close"));
        }
        assertArrayEquals(original, Files.readAllBytes(file.toPath()));
        Files.move(file.toPath(), directory.resolve("moved"));
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    void failedCallbacksAndMissingSheetsReleaseTheFile(final ExcelFormat format) throws Exception {
        final File file = workbook(format);
        final byte[] original = Files.readAllBytes(file.toPath());
        final IllegalStateException expected = new IllegalStateException("callback failed");
        assertSame(expected, assertThrows(IllegalStateException.class, () -> ExcelUtil.readRowsFromSheet(file, "data", true, row -> {
            throw expected;
        })));
        assertSame(expected, assertThrows(IllegalStateException.class, () -> ExcelUtil.readDatasetFromSheet(file, 0, (headers, row, output) -> {
            throw expected;
        })));
        assertThrows(IllegalArgumentException.class, () -> ExcelUtil.streamRowsFromSheet(file, "missing", false));
        assertThrows(IllegalArgumentException.class, () -> ExcelUtil.streamRowsFromSheet(file, 99, false));
        assertArrayEquals(original, Files.readAllBytes(file.toPath()));
        Files.delete(file.toPath());
    }

    @ParameterizedTest
    @EnumSource(ExcelFormat.class)
    void callerInputStreamRemainsOpenAfterStreaming(final ExcelFormat format) throws Exception {
        final byte[] bytes = Files.readAllBytes(workbook(format).toPath());
        final class CallerStream extends ByteArrayInputStream {
            boolean closed;

            CallerStream() {
                super(bytes);
            }

            @Override
            public void close() {
                closed = true;
            }
        }
        final CallerStream input = new CallerStream();
        try (Stream<Row> rows = ExcelUtil.streamRowsFromSheet(input, 0, true)) {
            assertEquals(2, rows.count());
        }
        assertEquals(false, input.closed);
    }
}
