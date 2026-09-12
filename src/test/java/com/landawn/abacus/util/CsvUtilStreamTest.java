package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.util.NoCachingNoUpdating.DisposableArray;
import com.landawn.abacus.util.stream.Stream;

public class CsvUtilStreamTest extends CsvUtilTestSupport {

    @Test
    public void testStream_BeanClass() {
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, Person.class)) {
            List<Person> persons = stream.toList();
            assertEquals(5, persons.size());
            assertEquals("John", persons.get(0).getName());
        }
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, List.of("id", "name", "age"), Person.class)) {
            assertEquals(5, stream.toList().size());
        }
        try (Stream<Person> stream = CsvUtil.stream(new StringReader(testCsvContent), Person.class, true)) {
            assertEquals(5, stream.toList().size());
        }
        try (Stream<Person> stream = CsvUtil.stream(new StringReader(testCsvContent), List.of("id", "name"), Person.class, true)) {
            assertEquals(5, stream.toList().size());
        }
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, null, 0, Long.MAX_VALUE, Fn.alwaysTrue(), Person.class)) {
            assertEquals(5, stream.toList().size());
        }
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, List.of("id", "name", "age"), 0, 2, Fn.alwaysTrue(), Person.class)) {
            assertEquals(2, stream.toList().size());
        }
        try (Stream<Person> stream = CsvUtil.stream(new StringReader(testCsvContent), List.of("id", "name"), 1, 2, Fn.alwaysTrue(), Person.class, true)) {
            assertEquals(2, stream.toList().size());
        }
    }

    @Test
    public void testStream_RowMapper() throws IOException {
        BiFunction<List<String>, DisposableArray<String>, Person> personMapper = (columns, row) -> new Person(row.get(0), row.get(1),
                Integer.parseInt(row.get(2)), Boolean.parseBoolean(row.get(3)));
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, personMapper)) {
            List<Person> persons = stream.toList();
            assertEquals(5, persons.size());
            assertEquals("John", persons.get(0).getName());
        }
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, List.of("id", "name", "age"), (columns, row) -> {
            Person p = new Person();
            p.setId(row.get(0));
            p.setName(row.get(1));
            p.setAge(Integer.parseInt(row.get(2)));
            return p;
        })) {
            assertEquals(5, stream.toList().size());
        }
        try (Stream<String> stream = CsvUtil.stream(new StringReader(testCsvContent), (columns, row) -> row.get(1), true)) {
            assertEquals(5, stream.toList().size());
        }
        try (Stream<String> stream = CsvUtil.stream(new StringReader(testCsvContent), List.of("name", "age"), (columns, row) -> row.get(0) + ":" + row.get(1),
                true)) {
            List<String> results = stream.toList();
            assertEquals(5, results.size());
            assertEquals("John:25", results.get(0));
        }
        try (Stream<String> stream = CsvUtil.stream(testCsvFile, (columns, row) -> row.get(1))) {
            assertEquals("John", stream.toList().get(0));
        }
        try (Stream<String> stream = CsvUtil.stream(testCsvFile, List.of("name"), (columns, row) -> row.get(0))) {
            assertEquals(5, stream.toList().size());
        }
        try (Stream<String> stream = CsvUtil.stream(testCsvFile, null, 0, Long.MAX_VALUE, Fn.alwaysTrue(), (columns, row) -> row.get(0) + ":" + row.get(1))) {
            assertEquals("1:John", stream.toList().get(0));
        }
        try (Stream<String> stream = CsvUtil.stream(testCsvFile, null, 1, 2, Fn.alwaysTrue(), (columns, row) -> row.get(1))) {
            assertEquals(2, stream.toList().size());
        }
        try (Reader reader = new StringReader(testCsvContent)) {
            List<String> result = CsvUtil.stream(reader, null, 0, Long.MAX_VALUE, row -> !"false".equals(row[3]), (columns, row) -> row.get(1), false).toList();
            assertEquals(3, result.size());
        }
        try (Stream<String> stream = CsvUtil.stream(new StringReader(testCsvContent), List.of("name", "age"), 0, Long.MAX_VALUE, Fn.alwaysTrue(),
                (columns, row) -> row.get(0), true)) {
            assertEquals(5, stream.toList().size());
        }
    }

    @Test
    public void testStream_TargetTypes() {
        try (Stream<Map> stream = CsvUtil.stream(testCsvFile, Map.class)) {
            List<Map> maps = stream.toList();
            assertEquals(5, maps.size());
            assertTrue(maps.get(0).containsKey("name"));
        }
        try (Stream<List> stream = CsvUtil.stream(testCsvFile, List.class)) {
            List<List> lists = stream.toList();
            assertEquals(5, lists.size());
            assertEquals(4, lists.get(0).size());
        }
        try (Stream<Object[]> stream = CsvUtil.stream(testCsvFile, Object[].class)) {
            List<Object[]> arrays = stream.toList();
            assertEquals(5, arrays.size());
            assertEquals(4, arrays.get(0).length);
        }
        try (Stream<Integer[]> stream = CsvUtil.stream(new StringReader("a,b\n1,2\n3,4\n"), Integer[].class, true)) {
            List<Integer[]> rows = stream.toList();
            assertEquals(2, rows.size());
            assertEquals(Integer.valueOf(1), rows.get(0)[0]);
            assertEquals(Integer.valueOf(4), rows.get(1)[1]);
        }
        try (Stream<String> stream = CsvUtil.stream(new StringReader("name\nJohn\nJane\n"), String.class, true)) {
            assertEquals(List.of("John", "Jane"), stream.toList());
        }
        try (Stream<Integer> stream = CsvUtil.stream(new StringReader("age\n25\n30\n"), Integer.class, true)) {
            assertEquals(List.of(25, 30), stream.toList());
        }
    }

    @Test
    public void testStream_EdgeCase() throws IOException {
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, null, 0, 0, Fn.alwaysTrue(), Person.class)) {
            assertEquals(0, stream.toList().size());
        }
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, null, 100, Long.MAX_VALUE, Fn.alwaysTrue(), Person.class)) {
            assertEquals(0, stream.toList().size());
        }
        try (Stream<Person> stream = CsvUtil.stream(new StringReader(""), Person.class, true)) {
            assertEquals(0, stream.toList().size());
        }
        try (Stream<String> stream = CsvUtil.stream(new StringReader(""), (columns, row) -> row.get(0), true)) {
            assertEquals(0, stream.toList().size());
        }
        File emptyFile = tempDir.resolve("empty.csv").toFile();
        Files.writeString(emptyFile.toPath(), "");
        try (Stream<Map> stream = CsvUtil.stream(emptyFile, Map.class)) {
            assertEquals(0, stream.toList().size());
        }
        File headerOnly = tempDir.resolve("header_only.csv").toFile();
        Files.writeString(headerOnly.toPath(), "id,name,age\n");
        try (Stream<Map> stream = CsvUtil.stream(headerOnly, Map.class)) {
            assertEquals(0, stream.toList().size());
        }

        assertThrows(IllegalArgumentException.class, () -> CsvUtil.stream(testCsvFile, null, -1, 10, Fn.alwaysTrue(), Person.class));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.stream(testCsvFile, null, 0, -1, Fn.alwaysTrue(), Person.class));
        assertThrows(IllegalArgumentException.class, () -> CsvUtil.stream(testCsvFile, null, -1, 10, Fn.alwaysTrue(),
                (BiFunction<List<String>, DisposableArray<String>, String>) (c, r) -> r.get(0)));
        assertThrows(IllegalArgumentException.class,
                () -> CsvUtil.stream(new StringReader("id\n1\n"), (BiFunction<List<String>, DisposableArray<String>, String>) null, false));

        try (Stream<Person> stream = CsvUtil.stream(new StringReader("id,name,unknownColumn\n1,John,val"), List.of("unknownColumn"), Person.class, false)) {
            assertThrows(IllegalArgumentException.class, stream::toList);
        }
        try (Stream<Person> stream = CsvUtil.stream(new StringReader("id,name,age\n1,John,25"), List.of("nonexistent"), Person.class, false)) {
            assertThrows(IllegalArgumentException.class, stream::toList);
        }
        try (Stream<String> stream = CsvUtil.stream(new StringReader("id,name\n1,John"), List.of("id", "name"), 0, Long.MAX_VALUE, Fn.alwaysTrue(),
                String.class, false)) {
            assertThrows(IllegalArgumentException.class, stream::toList);
        }
    }

    @Test
    public void testStream_ParserCaptureAndClose() {
        CsvUtil.setHeaderParser(CsvUtil.CSV_HEADER_PARSER_IN_JSON);
        CsvUtil.setLineParser(CsvUtil.CSV_LINE_PARSER_IN_JSON);
        Stream<String> stream = CsvUtil.stream(new StringReader("[\"id\",\"name\"]\n[\"1\",\"Ada\"]\n"), (columns, row) -> row.get(1), false);
        CsvUtil.resetHeaderParser();
        CsvUtil.resetLineParser();
        try (stream) {
            assertEquals(List.of("Ada"), stream.toList());
        }

        TrackingReader closed = new TrackingReader(new StringReader("id,name\n1,John\n2,Jane\n"));
        try (Stream<Object[]> s = CsvUtil.stream(closed, Object[].class, true)) {
            s.toList();
        }
        assertTrue(closed.closed);

        TrackingReader kept = new TrackingReader(new StringReader("id,name\n1,John\n"));
        try (Stream<Object[]> s = CsvUtil.stream(kept, Object[].class, false)) {
            s.toList();
        }
        assertFalse(kept.closed);

        TrackingReader failed = new TrackingReader(new StringReader("id,name\n1,John\n"));
        Stream<Object[]> failing = CsvUtil.stream(failed, List.of("missing"), Object[].class, true);
        try {
            assertThrows(IllegalArgumentException.class, failing::toList);
            assertTrue(failed.closed);
        } finally {
            failing.close();
        }
    }

    @Test
    public void testStream_EmptySourceRecyclesBorrowedBufferedReader() {
        List<java.io.BufferedReader> drainedReaders = new ArrayList<>();
        java.io.BufferedReader markerReader = null;
        java.io.BufferedReader nextReader = null;
        try {
            for (int i = 0; i < 64; i++) {
                drainedReaders.add(Objectory.createBufferedReader("drain-" + i));
            }
            markerReader = Objectory.createBufferedReader("marker");
            Objectory.recycle(markerReader);
            try (Stream<String> stream = CsvUtil.stream(new StringReader(""), null, 0, Long.MAX_VALUE, Fn.alwaysTrue(), (columns, row) -> row.get(0), false)) {
                assertTrue(stream.toList().isEmpty());
            }
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
    public void testStream_EmptySelectColumns() {
        try (Stream<Person> stream = CsvUtil.stream(testCsvFile, List.of(), Person.class)) {
            assertNotNull(stream.toList());
        }
        try (Stream<Person> stream = CsvUtil.stream(new StringReader(testCsvContent), List.of(), Person.class, true)) {
            assertNotNull(stream.toList());
        }
        try (Stream<String> stream = CsvUtil.stream(testCsvFile, List.of(), (columns, row) -> columns.toString())) {
            assertNotNull(stream.toList());
        }
    }

    @Test
    public void testStream_ScalarTargetTypeReportsSelectedColumnCount() {
        Stream<String> zeroColumns = CsvUtil.stream(new StringReader("a,b\n1,2\n"), List.of(), String.class, true);
        try {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, zeroColumns::toList);
            assertTrue(ex.getMessage().contains("only supported when exactly one column is selected"));
            assertTrue(ex.getMessage().contains("0 column(s) are selected"));
        } finally {
            zeroColumns.close();
        }

        Stream<String> twoColumns = CsvUtil.stream(new StringReader("a,b\n1,2\n"), null, String.class, true);
        try {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, twoColumns::toList);
            assertTrue(ex.getMessage().contains("java.lang.String"));
            assertTrue(ex.getMessage().contains("2 column(s) are selected"));
        } finally {
            twoColumns.close();
        }

        try (Stream<String> oneColumn = CsvUtil.stream(new StringReader("a,b\n1,2\n"), List.of("b"), String.class, true)) {
            assertEquals(List.of("2"), oneColumn.toList());
        }
    }
}
