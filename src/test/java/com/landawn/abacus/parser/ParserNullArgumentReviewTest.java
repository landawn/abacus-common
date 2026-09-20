package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;

import org.apache.avro.Schema;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class ParserNullArgumentReviewTest extends TestBase {
    @Test
    void tokenReaderRequiresSourceAndBufferButAcceptsEmptyBuffer() {
        assertTrue(assertThrows(IllegalArgumentException.class, () -> JsonStringReader.parse(null, null)).getMessage().contains("str"));
        assertTrue(assertThrows(IllegalArgumentException.class, () -> JsonStringReader.parse("{}", null)).getMessage().contains("cbuf"));
        assertTrue(
                assertThrows(IllegalArgumentException.class, () -> new JsonStringReader((char[]) null, -1, -1, null, null)).getMessage().contains("strValue"));
        assertTrue(
                assertThrows(IllegalArgumentException.class, () -> new JsonStringReader(new char[1], -1, 0, null, null)).getMessage().contains("beginIndex"));
        final JsonReader reader = JsonStringReader.parse("\"x\\ny\"", new char[0]);
        assertEquals(JsonReader.START_DOUBLE_QUOTE, reader.nextToken());
        assertEquals(JsonReader.END_DOUBLE_QUOTE, reader.nextToken());
        assertEquals("x\ny", reader.getText());
        assertEquals(-1, JsonStringReader.parse(null, 0, 0, new char[0]).nextToken());
        assertTrue(assertThrows(IllegalArgumentException.class, () -> JsonStringReader.parse(null, 0, 1, new char[0])).getMessage().contains("beginIndex"));
    }

    @Test
    void concreteJsonParserKeepsItsNullSourceDefault() {
        final JsonParser parser = ParserFactory.createJsonParser();
        assertDoesNotThrow(() -> parser.deserialize((String) null, 0, 0, Object.class));
        assertDoesNotThrow(() -> parser.deserialize((String) null, 0, 0, new JsonDeserConfig(), Object.class));
    }

    @Test
    void avroRetainsNullObjectNoOpButRequiresUsedOutputs() {
        final AvroParser parser = new AvroParser();
        assertDoesNotThrow(() -> parser.serialize(null, null, (OutputStream) null));
        assertThrows(IllegalArgumentException.class, () -> parser.serialize(null, null, (File) null));
        final Schema schema = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"NullOutput\",\"fields\":[]}");
        final AvroSerConfig config = new AvroSerConfig().setSchema(schema);
        assertTrue(assertThrows(IllegalArgumentException.class, () -> parser.serialize(Map.of(), config, (OutputStream) null)).getMessage().contains("output"));
        assertThrows(IllegalArgumentException.class, () -> AbstractParser.createNewFileIfNotExists(null));
    }
}
