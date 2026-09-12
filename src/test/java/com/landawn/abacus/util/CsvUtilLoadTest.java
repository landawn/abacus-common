package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.function.TriConsumer;

public class CsvUtilLoadTest extends CsvUtilTestSupport {

    @Test
    public void testLoad() {
        Dataset fromFile = CsvUtil.load(testCsvFile);
        assertEquals(4, fromFile.columnCount());
        assertEquals(5, fromFile.size());
        assertEquals(List.of("id", "name", "age", "active"), fromFile.columnNames());
        assertEquals("1", fromFile.get(0, 0));
        assertEquals("John", fromFile.get(0, 1));
        assertEquals("25", fromFile.get(0, 2));

        Dataset fromReader = CsvUtil.load(new StringReader(testCsvContent));
        assertEquals(4, fromReader.columnCount());
        assertEquals(5, fromReader.size());
        assertEquals("1", fromReader.get(0, 0));
        assertEquals("John", fromReader.get(0, 1));
    }

    @Test
    public void testLoad_SelectColumns() {
        Dataset fileCols = CsvUtil.load(testCsvFile, List.of("name", "age"));
        assertEquals(2, fileCols.columnCount());
        assertEquals(5, fileCols.size());
        assertEquals(List.of("name", "age"), fileCols.columnNames());

        Dataset readerCols = CsvUtil.load(new StringReader(testCsvContent), List.of("name", "age"));
        assertEquals(2, readerCols.columnCount());
        assertEquals(5, readerCols.size());

        Dataset allSelected = CsvUtil.load(testCsvFile, List.of("id", "name", "age", "active"));
        assertEquals(4, allSelected.columnCount());
        Dataset single = CsvUtil.load(testCsvFile, List.of("name"));
        assertEquals(1, single.columnCount());
        assertEquals("John", single.get(0, 0));
        assertEquals(1, CsvUtil.load(new StringReader(testCsvContent), List.of("name")).columnCount());
        assertEquals(4, CsvUtil.load(testCsvFile, (List<String>) null).columnCount());
    }

    @Test
    public void testLoad_OffsetCountFilter() {
        Dataset offset = CsvUtil.load(testCsvFile, List.of("name", "age"), 1, 2);
        assertEquals(2, offset.size());
        assertEquals("Jane", offset.get(0, 0));
        assertEquals("Bob", offset.get(1, 0));

        Dataset readerOffset = CsvUtil.load(new StringReader(testCsvContent), null, 1, 2);
        assertEquals(4, readerOffset.columnCount());
        assertEquals(2, readerOffset.size());

        Dataset active = CsvUtil.load(testCsvFile, null, 0, Long.MAX_VALUE, row -> "true".equals(row[3]));
        assertEquals(3, active.size());
        Dataset older = CsvUtil.load(new StringReader(testCsvContent), null, 0, Long.MAX_VALUE, row -> Integer.parseInt(row[2]) > 30);
        assertEquals(2, older.size());
        Dataset fileFilter = CsvUtil.load(testCsvFile, null, 0, Long.MAX_VALUE, row -> Integer.parseInt(row[2]) > 30);
        assertTrue(fileFilter.size() >= 2);
        Dataset readerFilter = CsvUtil.load(new StringReader(testCsvContent), null, 0, Long.MAX_VALUE, row -> Integer.parseInt(row[2]) <= 30);
        assertTrue(readerFilter.size() >= 3);

        assertEquals(0, CsvUtil.load(testCsvFile, null, 0, 0).size());
        assertEquals(0, CsvUtil.load(testCsvFile, null, 100, Long.MAX_VALUE).size());
        assertEquals(0, CsvUtil.load(new StringReader(testCsvContent), null, 0, 0).size());
        assertEquals(0, CsvUtil.load(new StringReader(testCsvContent), null, 0, Long.MAX_VALUE, row -> Integer.parseInt(row[2]) > 100).size());
    }

    @Test
    public void testLoad_BeanClass() {
        Dataset typedFile = CsvUtil.load(testCsvFile, Person.class);
        assertEquals(5, typedFile.size());
        assertTrue(typedFile.get(0, 2) instanceof Integer);
        assertEquals(Integer.valueOf(25), typedFile.get(0, 2));

        Dataset typedReader = CsvUtil.load(new StringReader(testCsvContent), Person.class);
        assertTrue(typedReader.get(0, 2) instanceof Integer);

        Dataset selected = CsvUtil.load(testCsvFile, List.of("id", "name", "age"), Person.class);
        assertEquals(3, selected.columnCount());
        Dataset offset = CsvUtil.load(testCsvFile, null, 1, 2, Person.class);
        assertEquals(2, offset.size());
        Dataset filtered = CsvUtil.load(testCsvFile, null, 0, Long.MAX_VALUE, row -> "true".equals(row[3]), Person.class);
        assertEquals(3, filtered.size());

        Dataset readerSelected = CsvUtil.load(new StringReader(testCsvContent), List.of("id", "name"), Person.class);
        assertEquals(2, readerSelected.columnCount());
        Dataset readerOffset = CsvUtil.load(new StringReader(testCsvContent), null, 1, 3, Person.class);
        assertEquals(3, readerOffset.size());
        Dataset readerFilter = CsvUtil.load(new StringReader(testCsvContent), null, 0, Long.MAX_VALUE, row -> Integer.parseInt(row[2]) >= 30, Person.class);
        assertEquals(3, readerFilter.size());

        Dataset allParams = CsvUtil.load(testCsvFile, List.of("name", "age"), 0, 3, row -> Integer.parseInt(row[2]) > 25, Person.class);
        assertEquals(2, allParams.columnCount());
        assertTrue(allParams.size() <= 3);
        assertEquals(3, CsvUtil.load(new StringReader(testCsvContent), null, 0, Long.MAX_VALUE, row -> "true".equals(row[3]), Person.class).size());
        assertTrue(CsvUtil.load(new StringReader(testCsvContent), null, 1, 3, row -> !"false".equals(row[3]), Person.class).size() <= 3);
    }

    @Test
    public void testLoad_BeanClass_EdgeCase() {
        assertEquals(0, CsvUtil.load(new StringReader(""), null, 0, Long.MAX_VALUE, Fn.alwaysTrue(), Person.class).size());
        Dataset headerOnly = CsvUtil.load(new StringReader("id,name,age\n"), null, 0, Long.MAX_VALUE, Fn.alwaysTrue(), Person.class);
        assertEquals(0, headerOnly.size());
        assertEquals(3, headerOnly.columnCount());

        Dataset unknown = CsvUtil.load(new StringReader("id,name,unknownColumn\n1,John,someRawValue"), null, 0, Long.MAX_VALUE, Fn.alwaysTrue(), Person.class);
        assertEquals(3, unknown.columnCount());
        assertEquals("someRawValue", unknown.get(0, 2));
        Dataset selectedUnknown = CsvUtil.load(new StringReader("id,name,unknownColumn\n1,John,rawValue"), List.of("id", "unknownColumn"), 0, Long.MAX_VALUE,
                Fn.alwaysTrue(), Person.class);
        assertEquals("rawValue", selectedUnknown.get(0, 1));

        assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.load(new StringReader("id,name,age\n1,John,25"), List.of("nonexistent"), 0, Long.MAX_VALUE, Fn.alwaysTrue(), Person.class));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(testCsvFile, List.of("nonexistent"), 0, Long.MAX_VALUE, Fn.alwaysTrue(), Person.class));
    }

    @Test
    public void testLoad_TypeMap() {
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("id", Type.of(String.class));
        typeMap.put("age", Type.of(Integer.class));
        Dataset fileTyped = CsvUtil.load(testCsvFile, CommonUtil.toList("id", "name", "age"), 0, Long.MAX_VALUE, typeMap);
        assertEquals(3, fileTyped.columnCount());
        assertEquals(5, fileTyped.size());

        Dataset offset = CsvUtil.load(testCsvFile, CommonUtil.toList("id", "name"), 1, 2, Map.of("id", Type.of(String.class), "name", Type.of(String.class)));
        assertEquals(2, offset.size());

        Dataset filtered = CsvUtil.load(testCsvFile, null, 0, Long.MAX_VALUE, row -> Integer.parseInt(row[0]) > 2,
                Map.of("id", Type.of(String.class), "name", Type.of(String.class)));
        assertEquals(3, filtered.size());

        Dataset readerTyped = CsvUtil.load(new StringReader(testCsvContent), Map.of("id", Type.of(String.class), "name", Type.of(String.class)));
        assertEquals(4, readerTyped.columnCount());
        Dataset readerOffset = CsvUtil.load(new StringReader(testCsvContent), CommonUtil.toList("id", "name", "age"), 0, 3,
                Map.of("id", Type.of(String.class), "name", Type.of(String.class)));
        assertEquals(3, readerOffset.size());
        Dataset readerFilter = CsvUtil.load(new StringReader(testCsvContent), null, 0, Long.MAX_VALUE, row -> Integer.parseInt(row[2]) < 35,
                Map.of("id", Type.of(String.class), "age", Type.of(Integer.class)));
        assertEquals(3, readerFilter.size());

        Map<String, Type<?>> ageActive = new HashMap<>();
        ageActive.put("age", Type.of(Integer.class));
        ageActive.put("active", Type.of(Boolean.class));
        Dataset fileTypeMap = CsvUtil.load(testCsvFile, ageActive);
        assertEquals(5, fileTypeMap.size());
        Dataset typedData = CsvUtil.load(new StringReader(testCsvContent), null, 0, Long.MAX_VALUE, Fn.alwaysTrue(), ageActive);
        assertTrue(typedData.get(0, 2) instanceof Integer);
        assertTrue(typedData.get(0, 3) instanceof Boolean);
        assertTrue(CsvUtil
                .load(testCsvFile, List.of("name", "age"), 0, Long.MAX_VALUE, row -> Integer.parseInt(row[2]) > 28, Map.of("age", Type.of(Integer.class)))
                .size() >= 2);
        assertEquals(2, CsvUtil.load(new StringReader(testCsvContent), List.of("name", "age"), 1, 2, Map.of("age", Type.of(Integer.class))).size());
        assertTrue(CsvUtil.load(testCsvFile, null, 1, 2, row -> Integer.parseInt(row[2]) > 25, Map.of("age", Type.of(Integer.class))).size() <= 2);
    }

    @Test
    public void testLoad_TypeMap_EdgeCase() {
        Map<String, Type<?>> nullType = new HashMap<>();
        nullType.put("id", null);
        Dataset ds = CsvUtil.load(new StringReader(testCsvContent), List.of("id", "name"), 0, Long.MAX_VALUE, nullType);
        assertEquals(2, ds.columnCount());
        assertEquals("1", ds.get(0, 0));
        assertEquals("John", ds.get(0, 1));
        Dataset allColumns = CsvUtil.load(new StringReader(testCsvContent), nullType);
        assertEquals(4, allColumns.columnCount());
        assertEquals("1", allColumns.get(0, 0));

        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(testCsvFile, new HashMap<>()));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(new StringReader(testCsvContent), new HashMap<>()));
        assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.load(new StringReader(testCsvContent), null, 0, Long.MAX_VALUE, Fn.alwaysTrue(), new HashMap<>()));
    }

    @Test
    public void testLoad_RowExtractor() {
        TriConsumer<List<String>, DisposableArray<String>, Object[]> copy = (columns, row, output) -> {
            for (int i = 0; i < row.length(); i++) {
                output[i] = row.get(i);
            }
        };
        TriConsumer<List<String>, DisposableArray<String>, Object[]> typed = (columns, row, output) -> {
            output[0] = row.get(0);
            output[1] = row.get(1).toUpperCase();
            output[2] = Integer.parseInt(row.get(2));
            output[3] = Boolean.parseBoolean(row.get(3));
        };

        Dataset fileExtracted = CsvUtil.load(testCsvFile, typed);
        assertEquals(5, fileExtracted.size());
        assertEquals("JOHN", fileExtracted.get(0, 1));
        assertEquals(Integer.valueOf(25), fileExtracted.get(0, 2));
        assertEquals(5, CsvUtil.load(new StringReader(testCsvContent), copy).size());

        Dataset selected = CsvUtil.load(testCsvFile, List.of("name", "age"), (columns, row, output) -> {
            output[0] = row.get(0);
            output[1] = Integer.parseInt(row.get(1));
        });
        assertEquals(2, selected.columnCount());
        assertEquals(2, CsvUtil.load(testCsvFile, 1, 2, copy).size());
        assertEquals(3, CsvUtil.load(testCsvFile, List.of("id", "name"), 0, Long.MAX_VALUE, row -> Integer.parseInt(row[0]) <= 3, (columns, row, output) -> {
            output[0] = row.get(0);
            output[1] = row.get(1);
        }).size());
        assertEquals(5, CsvUtil.load(new StringReader(testCsvContent), List.of("name", "age"), (columns, row, output) -> {
            output[0] = row.get(0);
            output[1] = row.get(1);
        }).size());
        assertEquals(3, CsvUtil.load(new StringReader(testCsvContent), 1, 3, copy).size());
        assertEquals(3, CsvUtil.load(new StringReader(testCsvContent), List.of("id", "age"), 0, Long.MAX_VALUE, row -> Integer.parseInt(row[2]) >= 30,
                (columns, row, output) -> {
                    output[0] = row.get(0);
                    output[1] = Integer.parseInt(row.get(1));
                }).size());
        assertEquals(2, CsvUtil.load(new StringReader(testCsvContent), 2, 2, copy).size());

        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(new StringReader(testCsvContent), null, 0, Long.MAX_VALUE, null,
                (TriConsumer<List<String>, DisposableArray<String>, Object[]>) null));
    }

    @Test
    public void testLoad_EdgeCase() throws IOException {
        ParsingException ragged = assertThrows(ParsingException.class, () -> {
            File raggedFile = tempDir.resolve("ragged.csv").toFile();
            Files.writeString(raggedFile.toPath(), "a,b,c\n1,2,3\n4,5,6,7\n");
            CsvUtil.load(raggedFile);
        });
        assertTrue(ragged.getMessage().contains("3 column"));

        File emptyFile = tempDir.resolve("empty.csv").toFile();
        Files.writeString(emptyFile.toPath(), "");
        assertEquals(0, CsvUtil.load(emptyFile).size());
        assertEquals(0, CsvUtil.load(new StringReader("")).size());

        File headerOnly = tempDir.resolve("header.csv").toFile();
        Files.writeString(headerOnly.toPath(), "id,name,age\n");
        Dataset headerDs = CsvUtil.load(headerOnly);
        assertEquals(3, headerDs.columnCount());
        assertEquals(0, headerDs.size());
        Dataset readerHeader = CsvUtil.load(new StringReader("id,name,age\n"));
        assertEquals(3, readerHeader.columnCount());
        assertEquals(0, readerHeader.size());

        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(testCsvFile, null, -1, 10));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(testCsvFile, null, 0, -1));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(testCsvFile, List.of("nonexistent")));
    }

    @Test
    public void testLoad_Rfc4180AndBom() throws IOException {
        File rfc = tempDir.resolve("rfc4180.csv").toFile();
        Files.writeString(rfc.toPath(), "name,note\n\"Doe, John\",hello\n\"He said \"\"hi\"\"\",end\n");
        Dataset ds = CsvUtil.load(rfc);
        assertEquals(2, ds.size());
        assertEquals("Doe, John", ds.get(0, 0));
        assertEquals("He said \"hi\"", ds.get(1, 0));

        File noEol = tempDir.resolve("noeol.csv").toFile();
        Files.writeString(noEol.toPath(), "a,b,c\n1,2,3");
        Dataset last = CsvUtil.load(noEol);
        assertEquals(1, last.size());
        assertEquals("3", last.get(0, 2));

        File dup = tempDir.resolve("dup.csv").toFile();
        Files.writeString(dup.toPath(), "a,a,b\n1,2,3\n");
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(dup));

        File bom = tempDir.resolve("bom.csv").toFile();
        byte[] bomBytes = new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF };
        byte[] body = "id,name\n1,John\n".getBytes(java.nio.charset.StandardCharsets.UTF_8);
        byte[] all = new byte[bomBytes.length + body.length];
        System.arraycopy(bomBytes, 0, all, 0, bomBytes.length);
        System.arraycopy(body, 0, all, bomBytes.length, body.length);
        Files.write(bom.toPath(), all);
        assertEquals("id", CsvUtil.load(bom).columnNames().get(0));
    }

    @Test
    public void testLoad_EmptySelectColumns() {
        assertEquals(0, CsvUtil.load(testCsvFile, List.of()).columnCount());
        assertEquals(0, CsvUtil.load(new StringReader(testCsvContent), List.of()).columnCount());
        assertEquals(0, CsvUtil.load(testCsvFile, List.of(), Person.class).columnCount());
        Map<String, Type<?>> typeMap = new HashMap<>();
        typeMap.put("age", Type.of(Integer.class));
        assertEquals(0, CsvUtil.load(new StringReader(testCsvContent), List.of(), 0, Long.MAX_VALUE, Fn.alwaysTrue(), typeMap).columnCount());
        assertEquals(0, CsvUtil.load(testCsvFile, List.of(), (columns, row, output) -> {
            for (int i = 0; i < output.length; i++) {
                output[i] = row.get(i);
            }
        }).columnCount());
    }

    @Test
    public void testLoad_NonBeanColumnTypeClassRecyclesBorrowedBufferedReader() {
        List<java.io.BufferedReader> drainedReaders = new ArrayList<>();
        java.io.BufferedReader markerReader = null;
        java.io.BufferedReader nextReader = null;
        try {
            for (int i = 0; i < 64; i++) {
                drainedReaders.add(Objectory.createBufferedReader("drain-" + i));
            }
            markerReader = Objectory.createBufferedReader("marker");
            Objectory.recycle(markerReader);
            assertThrows(IllegalArgumentException.class,
                    () -> CsvUtil.load(new StringReader("a,b\n1,2\n"), null, 0, Long.MAX_VALUE, Fn.alwaysTrue(), String.class));
            nextReader = Objectory.createBufferedReader("next");
            assertSame(markerReader, nextReader);
        } finally {
            if (nextReader != null && nextReader != markerReader) {
                Objectory.recycle(nextReader);
            }
            if (markerReader != null) {
                Objectory.recycle(markerReader);
            }
            for (java.io.BufferedReader reader : drainedReaders) {
                Objectory.recycle(reader);
            }
        }
    }

    @Test
    public void testLoad_RowFieldCountAgainstHeaderContract() {
        ParsingException tooManyFields = assertThrows(ParsingException.class, () -> CsvUtil.load(new StringReader("a,b\n1,2,3\n")));
        assertTrue(tooManyFields.getMessage().contains("more fields than the expected 2 column(s)"));

        Dataset padded = CsvUtil.load(new StringReader("a,b,c\n1,2\n"));
        assertEquals(1, padded.size());
        assertEquals(3, padded.columnCount());
        assertEquals("1", padded.get(0, 0));
        assertEquals("2", padded.get(0, 1));
        assertNull(padded.get(0, 2));
    }
}
