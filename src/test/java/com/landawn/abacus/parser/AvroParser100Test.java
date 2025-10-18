package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.AvroDeserializationConfig.ADC;
import com.landawn.abacus.parser.AvroSerializationConfig.ASC;

@Tag("new-test")
public class AvroParser100Test extends TestBase {

    private AvroParser parser;
    private Schema schema;
    private String schemaJson = "{\"type\":\"record\",\"name\":\"TestRecord\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"},{\"name\":\"age\",\"type\":\"int\"}]}";

    @BeforeEach
    public void setUp() {
        parser = new AvroParser();
        schema = new Schema.Parser().parse(schemaJson);
    }

    @Test
    public void testSerializeToString() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "John");
        data.put("age", 30);

        AvroSerializationConfig config = ASC.create().setSchema(schema);
        String result = parser.serialize(data, config);

        Assertions.assertNotNull(result);
        Assertions.assertFalse(result.isEmpty());
    }

    @Test
    public void testSerializeToStringWithNull() {
        assertThrows(NullPointerException.class, () -> parser.serialize(null, (AvroSerializationConfig) null));
    }

    @Test
    public void testSerializeToOutputStream() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Jane");
        data.put("age", 25);

        AvroSerializationConfig config = ASC.create().setSchema(schema);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        parser.serialize(data, config, os);

        byte[] bytes = os.toByteArray();
        Assertions.assertTrue(bytes.length > 0);
    }

    @Test
    public void testSerializeToOutputStreamWithoutSchema() {
        Map<String, Object> data = new HashMap<>();
        data.put("test", "value");

        ByteArrayOutputStream os = new ByteArrayOutputStream();

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            parser.serialize(data, null, os);
        });
    }

    @Test
    public void testSerializeToWriter() {
        Writer writer = new StringWriter();

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            parser.serialize("test", null, writer);
        });
    }

    @Test
    public void testDeserializeFromString() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Test");
        data.put("age", 35);

        AvroSerializationConfig serConfig = ASC.create().setSchema(schema);
        String base64 = parser.serialize(data, serConfig);

        AvroDeserializationConfig desConfig = ADC.create().setSchema(schema);
        GenericRecord result = parser.deserialize(base64, desConfig, GenericRecord.class);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("Test", result.get("name").toString());
        Assertions.assertEquals(35, result.get("age"));
    }

    @Test
    public void testDeserializeFromInputStream() {
        Map<String, Object> data = new HashMap<>();
        data.put("name", "Stream Test");
        data.put("age", 40);

        AvroSerializationConfig serConfig = ASC.create().setSchema(schema);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        parser.serialize(data, serConfig, os);

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        AvroDeserializationConfig desConfig = ADC.create().setSchema(schema);
        GenericRecord result = parser.deserialize(is, desConfig, GenericRecord.class);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("Stream Test", result.get("name").toString());
        Assertions.assertEquals(40, result.get("age"));
    }

    @Test
    public void testDeserializeFromInputStreamWithoutSchema() {
        ByteArrayInputStream is = new ByteArrayInputStream(new byte[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> {
            parser.deserialize(is, null, Map.class);
        });
    }

    @Test
    public void testDeserializeFromReader() {
        Reader reader = new StringReader("test");

        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            parser.deserialize(reader, null, Object.class);
        });
    }

    @Test
    public void testSerializeCollection() {
        List<Map<String, Object>> list = new ArrayList<>();
        Map<String, Object> item1 = new HashMap<>();
        item1.put("name", "Item1");
        item1.put("age", 10);
        Map<String, Object> item2 = new HashMap<>();
        item2.put("name", "Item2");
        item2.put("age", 20);
        list.add(item1);
        list.add(item2);

        AvroSerializationConfig config = ASC.create().setSchema(schema);
        ByteArrayOutputStream os = new ByteArrayOutputStream();

        parser.serialize(list, config, os);

        byte[] bytes = os.toByteArray();
        Assertions.assertTrue(bytes.length > 0);
    }

    @Test
    public void testDeserializeToMap() {
        GenericRecord record = new GenericData.Record(schema);
        record.put("name", "MapTest");
        record.put("age", 50);

        AvroSerializationConfig serConfig = ASC.create().setSchema(schema);
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        parser.serialize(record, serConfig, os);

        ByteArrayInputStream is = new ByteArrayInputStream(os.toByteArray());
        AvroDeserializationConfig desConfig = ADC.create().setSchema(schema);
        Map<String, Object> result = parser.deserialize(is, desConfig, Map.class);

        Assertions.assertNotNull(result);
        Assertions.assertEquals("MapTest", result.get("name").toString());
        Assertions.assertEquals(50, result.get("age"));
    }
}
