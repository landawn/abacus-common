package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.apache.avro.AvroRuntimeException;
import org.apache.avro.Schema;
import org.apache.avro.file.DataFileStream;
import org.apache.avro.file.DataFileWriter.AppendWriteException;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumReader;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.entity.User;

public class ParserAvroSchemaWriteTest extends TestBase {
    private final AvroParser parser = new AvroParser();

    private static Schema recordSchema(String name) {
        return new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"" + name + "\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}");
    }

    private byte[] write(Object value, Schema schema) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        parser.serialize(value, AvroSerConfig.create().setSchema(schema), output);
        return output.toByteArray();
    }

    private FileContents read(byte[] bytes) throws Exception {
        try (DataFileStream<Object> input = new DataFileStream<>(new ByteArrayInputStream(bytes), new GenericDatumReader<>())) {
            List<Object> values = new ArrayList<>();
            while (input.hasNext()) {
                values.add(input.next());
            }
            return new FileContents(input.getSchema(), values);
        }
    }

    @Test
    public void recordSchemaWritesZeroOrManyDatums() throws Exception {
        Schema schema = recordSchema("Rows");
        FileContents empty = read(write(List.of(), schema));
        assertEquals(schema, empty.schema());
        assertEquals(List.of(), empty.values());
        FileContents rows = read(write(List.of(Map.of("name", ""), Map.of("name", "\u96ea\ud83d\ude00")), schema));
        assertEquals(2, rows.values().size());
        assertEquals("", ((GenericRecord) rows.values().get(0)).get("name").toString());
        assertEquals("\u96ea\ud83d\ude00", ((GenericRecord) rows.values().get(1)).get("name").toString());
    }

    @Test
    public void recordArrayWritesOneDatumForEmptyMapsBeansAndRecords() throws Exception {
        Schema record = recordSchema("Items");
        Schema schema = Schema.createArray(record);
        FileContents empty = read(write(List.of(), schema));
        assertEquals(schema, empty.schema());
        assertEquals(List.of(List.of()), empty.values());
        Named bean = new Named();
        bean.name = "bean";
        GenericRecord generic = new GenericData.Record(record);
        generic.put("name", "generic");
        FileContents file = read(write(List.of(Map.of("name", "map"), bean, generic), schema));
        assertEquals(schema, file.schema());
        assertEquals(1, file.values().size());
        List<?> values = (List<?>) file.values().get(0);
        assertEquals(List.of("map", "bean", "generic"), values.stream().map(x -> ((GenericRecord) x).get("name").toString()).toList());
    }

    @Test
    public void nullableArrayItemsUseTheirElementSchema() throws Exception {
        Schema record = recordSchema("NullableItem");
        Schema schema = Schema.createArray(Schema.createUnion(Schema.create(Schema.Type.NULL), record));
        FileContents file = read(write(Arrays.asList(null, Map.of("name", "tail"), null), schema));
        assertEquals(1, file.values().size());
        List<?> values = (List<?>) file.values().get(0);
        assertNull(values.get(0));
        assertEquals("tail", ((GenericRecord) values.get(1)).get("name").toString());
        assertNull(values.get(2));
        assertThrows(AppendWriteException.class, () -> write(Arrays.asList((Object) null), Schema.createArray(record)));
    }

    @Test
    public void nestedRecordFieldsArraysAndMapValuesAreConvertedRecursively() throws Exception {
        Schema schema = new Schema.Parser().parse("{\"type\":\"record\",\"name\":\"OuterWrite\",\"fields\":["
                + "{\"name\":\"child\",\"type\":[\"null\",{\"type\":\"record\",\"name\":\"ChildWrite\",\"fields\":[{\"name\":\"name\",\"type\":\"string\"}]}]},"
                + "{\"name\":\"rows\",\"type\":{\"type\":\"array\",\"items\":\"ChildWrite\"}},"
                + "{\"name\":\"byName\",\"type\":{\"type\":\"map\",\"values\":[\"null\",\"ChildWrite\"]}}]}");
        Named child = new Named();
        child.name = "\u96ea";
        Map<String, Object> byName = new LinkedHashMap<>();
        byName.put("\u96ea", Map.of("name", "mapped"));
        byName.put("missing", null);
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("child", child);
        source.put("rows", List.of(Map.of("name", "row")));
        source.put("byName", byName);
        GenericRecord result = (GenericRecord) read(write(source, schema)).values().get(0);
        assertEquals("\u96ea", ((GenericRecord) result.get("child")).get("name").toString());
        assertEquals("row", ((GenericRecord) ((List<?>) result.get("rows")).get(0)).get("name").toString());
        Map<?, ?> map = (Map<?, ?>) result.get("byName");
        assertEquals(2, map.size());
        assertTrue(map.values().contains(null));
        assertTrue(map.values().stream().anyMatch(x -> x instanceof GenericRecord r && r.get("name").toString().equals("mapped")));
        source.put("child", null);
        assertNull(((GenericRecord) read(write(source, schema)).values().get(0)).get("child"));
    }

    @Test
    public void mapSchemasRemainMapsAtRootAndInsideArrays() throws Exception {
        Schema mapSchema = Schema.createMap(Schema.createArray(Schema.create(Schema.Type.STRING)));
        Map<String, Object> source = Map.of("\u96ea", List.of("", "\ud83d\ude00"));
        FileContents root = read(write(source, mapSchema));
        assertEquals(mapSchema, root.schema());
        assertEquals(1, ((Map<?, ?>) root.values().get(0)).size());
        Schema arrays = Schema.createArray(mapSchema);
        List<?> values = (List<?>) read(write(List.of(source, Map.of()), arrays)).values().get(0);
        assertEquals(2, values.size());
        assertTrue(((Map<?, ?>) values.get(1)).isEmpty());
        Collection<?> strings = (Collection<?>) ((Map<?, ?>) values.get(0)).values().iterator().next();
        assertEquals(List.of("", "\ud83d\ude00"), strings.stream().map(Object::toString).toList());
    }

    @Test
    public void specificRecordsKeepInferenceAndHonorExplicitArrays() throws Exception {
        User user = new User("\u96ea", 7, "blue");
        byte[] inferred = Base64.getDecoder().decode(parser.serialize(List.of(user), (AvroSerConfig) null));
        FileContents sequence = read(inferred);
        assertEquals(user.getSchema(), sequence.schema());
        assertEquals(1, sequence.values().size());
        Schema array = Schema.createArray(user.getSchema());
        FileContents file = read(write(List.of(user), array));
        assertEquals(array, file.schema());
        assertEquals(1, file.values().size());
        assertEquals("\u96ea", ((GenericRecord) ((List<?>) file.values().get(0)).get(0)).get("name").toString());
        assertEquals(List.of(List.of()), read(write(List.of(), array)).values());
        Schema nullable = Schema.createArray(Schema.createUnion(Schema.create(Schema.Type.NULL), user.getSchema()));
        assertEquals(2, ((List<?>) read(write(Arrays.asList(user, null), nullable)).values().get(0)).size());
        assertThrows(IllegalArgumentException.class, () -> parser.serialize(Arrays.asList(user, null), (AvroSerConfig) null));
    }

    @Test
    public void unionResolutionDoesNotGuessAmongRecordBranches() throws Exception {
        Schema scalars = Schema
                .createArray(Schema.createUnion(Schema.create(Schema.Type.NULL), Schema.create(Schema.Type.INT), Schema.create(Schema.Type.STRING)));
        List<?> values = (List<?>) read(write(Arrays.asList(null, 1, "\u96ea"), scalars)).values().get(0);
        assertNull(values.get(0));
        assertEquals(1, values.get(1));
        assertEquals("\u96ea", values.get(2).toString());
        Schema ambiguous = Schema.createArray(Schema.createUnion(recordSchema("ChoiceA"), recordSchema("ChoiceB")));
        assertThrows(AvroRuntimeException.class, () -> write(List.of(Map.of("name", "x")), ambiguous));
        GenericRecord selected = new GenericData.Record(ambiguous.getElementType().getTypes().get(1));
        selected.put("name", "selected");
        GenericRecord decoded = (GenericRecord) ((List<?>) read(write(List.of(selected), ambiguous)).values().get(0)).get(0);
        assertEquals("ChoiceB", decoded.getSchema().getName());
    }

    @Test
    public void scalarArraysKeepBoundariesAndNestedEmptyArrays() throws Exception {
        Schema longs = Schema.createArray(Schema.create(Schema.Type.LONG));
        List<Long> values = List.of(Long.MIN_VALUE, -1L, 0L, 1L, Long.MAX_VALUE);
        assertEquals(List.of(values), read(write(values, longs)).values());
        Schema nested = Schema.createArray(longs);
        assertEquals(List.of(List.of(List.of(), values)), read(write(List.of(List.of(), values), nested)).values());
    }

    @Test
    public void stringFileAndStreamsPreserveOwnershipAndEarlyValidation() throws Exception {
        Schema schema = Schema.createArray(recordSchema("OutputItem"));
        List<?> value = List.of(Map.of("name", "\u96ea"));
        assertEquals(schema, read(Base64.getDecoder().decode(parser.serialize(value, AvroSerConfig.create().setSchema(schema)))).schema());
        class TrackedOutput extends ByteArrayOutputStream {
            boolean closed;

            @Override
            public void close() {
                closed = true;
            }
        }
        TrackedOutput output = new TrackedOutput();
        parser.serialize(value, AvroSerConfig.create().setSchema(schema), output);
        assertFalse(output.closed);
        assertEquals(schema, read(output.toByteArray()).schema());
        TrackedOutput invalid = new TrackedOutput();
        assertThrows(AppendWriteException.class, () -> parser.serialize(Arrays.asList((Object) null), AvroSerConfig.create().setSchema(schema), invalid));
        assertFalse(invalid.closed);
        TrackedOutput none = new TrackedOutput();
        parser.serialize(null, null, none);
        assertEquals(0, none.size());
        assertFalse(none.closed);
        Path file = Files.createTempFile("parser-avro-schema-", ".avro");
        try {
            parser.serialize(value, AvroSerConfig.create().setSchema(schema), file.toFile());
            assertEquals(schema, read(Files.readAllBytes(file)).schema());
            Files.writeString(file, "preserve", StandardCharsets.UTF_8);
            assertThrows(IllegalArgumentException.class, () -> parser.serialize(value, null, file.toFile()));
            assertEquals("preserve", Files.readString(file));
            assertThrows(IllegalArgumentException.class, () -> parser.serialize(Arrays.asList(new User("x", 1, "b"), null), null, file.toFile()));
            assertEquals("preserve", Files.readString(file));
        } finally {
            Files.deleteIfExists(file);
        }
    }

    private record FileContents(Schema schema, List<Object> values) {
    }

    public static class Named {
        public String name;
    }
}
