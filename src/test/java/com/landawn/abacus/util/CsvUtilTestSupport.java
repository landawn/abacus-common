package com.landawn.abacus.util;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.io.TempDir;

import com.landawn.abacus.TestBase;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

public abstract class CsvUtilTestSupport extends TestBase {

    @TempDir
    Path tempDir;

    protected File testCsvFile;
    protected String testCsvContent;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Person {
        protected String id;
        protected String name;
        protected Integer age;
        protected Boolean active;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Product {
        protected String id;
        protected String name;
        protected Double price;
    }

    @BeforeEach
    public void setUp() throws IOException {
        testCsvContent = "id,name,age,active\n" + "1,John,25,true\n" + "2,Jane,30,true\n" + "3,Bob,35,false\n" + "4,Alice,28,true\n" + "5,Charlie,40,false\n";

        testCsvFile = tempDir.resolve("test.csv").toFile();
        Files.writeString(testCsvFile.toPath(), testCsvContent);
    }

    @AfterEach
    public void tearDown() {
        CsvUtil.resetHeaderParser();
        CsvUtil.resetLineParser();
        CsvUtil.resetEscapeCharForWrite();
    }

    // CsvLoader.load() with reader, no beanClass, no columnTypeMap -> L3403

    // --- Additional missing tests ---

    // load(Reader, Collection, long, long, Predicate, Class) with empty reader -> L1001

    // load(Reader, ..., beanClass) where a column has no matching bean property (noSelect path) -> L1059-1060

    // load(Reader, ..., beanClass) with selected columns where selected column has no bean property -> L1068-1069

    // load(Reader, Collection, long, long, Predicate, Class) with header-only reader (data line null) -> different from empty

    // load(Reader, Collection, long, long, Predicate, Class) with invalid selectColumnNames -> L1030

    // CsvLoader.load() with invalid selectColumnNames from reader -> exercises L1340

    // stream(Reader, ..., Class) with selectColumnNames having property not in bean -> L2012

    // stream(Reader, ..., Class) with invalid selectColumnNames -> L2021

    // stream(Reader, ..., Class) with unsupported target type (not array/collection/map/bean), multiple columns -> L2100

    // csvToJson with invalid selectColumnNames -> L2775

    // CsvConverter.csvToJson with invalid selectColumns -> exercises builder path

    // CSVCommon.apply() with escapeCharToBackSlashForWrite=true, previous state=false -> L3294-3295 (finally restores to non-backslash)

    // -------------------------------------------------------------------------
    // Backslash round-trip tests (regression for REPLACEMENT_CHARS['\\'] = null bug)
    // -------------------------------------------------------------------------

    // --- Helpers ---

    /**
     * Reader wrapper that records whether close() was invoked.
     */
    protected static final class TrackingReader extends Reader {
        final Reader delegate;
        boolean closed = false;

        TrackingReader(Reader delegate) {
            this.delegate = delegate;
        }

        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            return delegate.read(cbuf, off, len);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            delegate.close();
        }
    }

    /**
     * Writer wrapper that records whether close() was invoked.
     */
    protected static final class TrackingWriter extends java.io.Writer {
        final StringWriter delegate;
        boolean closed = false;

        TrackingWriter(StringWriter delegate) {
            this.delegate = delegate;
        }

        @Override
        public void write(char[] cbuf, int off, int len) {
            delegate.write(cbuf, off, len);
        }

        @Override
        public void flush() {
            delegate.flush();
        }

        @Override
        public void close() {
            closed = true;
        }
    }

    // --- regression tests for 2026-06-11 deep-review fixes ---

    //
    // ============================ review fixes 2026-09-06 ============================
    //
}
