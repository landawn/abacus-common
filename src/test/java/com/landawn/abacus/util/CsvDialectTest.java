package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class CsvDialectTest extends TestBase {
    @Test
    void everyUtf16CodeUnitSurvivesMatchedWriterAndParserDialects() throws Exception {
        char[] chars = new char[Character.MAX_VALUE + 1];
        for (int i = 0; i < chars.length; i++) {
            chars[i] = (char) i;
        }
        String all = new String(chars);
        boolean oldMode = CsvUtil.isBackSlashEscapeCharForWrite();
        try {
            for (boolean backslash : new boolean[] { false, true, false, true }) {
                setMode(backslash);
                CsvParser parser = new CsvParser(',', '"', backslash ? '\\' : CsvParser.NULL_CHARACTER);
                BufferedCsvWriter writer = Objectory.createBufferedCsvWriter();
                try {
                    assertEquals(backslash, writer.isBackSlash());
                    writer.write('"');
                    writer.writeCharacter(chars, 0, chars.length);
                    writer.write('"');
                    assertEquals(List.of(all), parser.parseLine(writer.toString()));
                } finally {
                    Objectory.recycle(writer);
                }
            }
        } finally {
            setMode(oldMode);
        }
    }

    @Test
    void defaultsKeepLiteralEscapesDistinctFromActualControlCharacters() throws Exception {
        boolean oldMode = CsvUtil.isBackSlashEscapeCharForWrite();
        try {
            setMode(false);
            CsvParser parser = new CsvParser();
            assertEquals(CsvParser.NULL_CHARACTER, parser.getEscape());
            for (String value : List.of("", "\0", "\\u0000", "\u2028\u2029", "\\u2028\\u2029", "end\\", "two\\\\", "\\\"", "\ud83d\ude00")) {
                BufferedCsvWriter writer = new BufferedCsvWriter();
                writer.write('"');
                writer.writeCharacter(value);
                writer.write('"');
                assertEquals(List.of(value), parser.parseLine(writer.toString()));
            }
            assertTrue(parser.parseLine(null).isEmpty());
            assertEquals(List.of(""), parser.parseLine(""));
        } finally {
            setMode(oldMode);
        }
    }

    @Test
    void jsonCsvConversionsPreserveDefaultAndExplicitLegacyDialectData() throws Exception {
        var oldHeader = CsvUtil.getCurrentHeaderParser();
        var oldLine = CsvUtil.getCurrentLineParser();
        boolean oldMode = CsvUtil.isBackSlashEscapeCharForWrite();
        String header = "\u4e2d\\";
        String value = "\0\u007f\u2028\u2029\\u0000\\\\\"\ud83d\ude00";
        List<Map<String, String>> expected = List.of(Map.of(header, value));
        String json = N.toJson(expected);
        try {
            for (boolean backslash : new boolean[] { false, true }) {
                setMode(backslash);
                CsvParser parser = new CsvParser(',', '"', backslash ? '\\' : CsvParser.NULL_CHARACTER);
                CsvUtil.setHeaderParser(parser::parseLineToArray);
                CsvUtil.setLineParser(parser::parseLineInto);
                StringWriter csv = new StringWriter();
                assertEquals(1, CsvUtil.jsonToCsv(new StringReader(json), null, csv));
                Dataset dataset = CsvUtil.load(new StringReader(csv.toString()));
                assertEquals(List.of(header), dataset.columnNames());
                assertEquals(value, dataset.get(0, 0));
                StringWriter roundTrip = new StringWriter();
                assertEquals(1, CsvUtil.csvToJson(new StringReader(csv.toString()), null, roundTrip, null));
                assertEquals(expected, N.fromJson(roundTrip.toString(), new TypeReference<List<Map<String, String>>>() {
                }.type()));
            }
        } finally {
            setMode(oldMode);
            CsvUtil.setHeaderParser(oldHeader);
            CsvUtil.setLineParser(oldLine);
        }
    }

    @Test
    void typeAttributeArgumentsRetainTheirIndependentBackslashGrammar() {
        assertArrayEquals(new String[] { "alpha\")>beta" }, TypeAttrParser.parse("Factory(\"alpha\\\")>beta\")").getParameters());
        assertArrayEquals(new String[] { "alpha\")>beta" }, TypeAttrParser.parse("Factory(\"alpha\"\")>beta\")").getParameters());
        assertArrayEquals(new String[] { "a\\b", "\u4e2d,\ud83d\ude00" }, TypeAttrParser.parse("Factory(\"a\\\\b\",\"\u4e2d,\ud83d\ude00\")").getParameters());
    }

    private static void setMode(boolean backslash) {
        if (backslash) {
            CsvUtil.setEscapeCharToBackSlashForWrite();
        } else {
            CsvUtil.resetEscapeCharForWrite();
        }
    }
}
