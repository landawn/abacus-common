package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.avro.Schema;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericDatumWriter;
import org.apache.avro.generic.GenericRecord;
import org.apache.avro.util.Utf8;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.Strings;

public class ParserAvroCollectionTest extends TestBase {
    private final AvroParser parser = ParserFactory.createAvroParser();
    private static final Schema STRINGS = new Schema.Parser().parse("{\"type\":\"array\",\"items\":[\"null\",\"string\"]}");
    private static final Schema LABEL = new Schema.Parser()
            .parse("{\"type\":\"record\",\"name\":\"Label\",\"fields\":[{\"name\":\"label\",\"type\":\"string\"}]}");

    public static class LabelBean {
        private String label;

        public String getLabel() {
            return label;
        }

        public void setLabel(String label) {
            this.label = label;
        }
    }

    private static byte[] container(Schema schema, Object... datums) throws Exception {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (DataFileWriter<Object> writer = new DataFileWriter<>(new GenericDatumWriter<>(schema))) {
            writer.create(schema, output);
            for (Object datum : datums) {
                writer.append(datum);
            }
        }
        return output.toByteArray();
    }

    private <T> T read(byte[] bytes, Schema schema, Type<T> type) {
        return parser.deserialize(new ByteArrayInputStream(bytes), AvroDeserConfig.create().setSchema(schema), type);
    }

    private static GenericRecord label(String value) {
        GenericRecord record = new GenericData.Record(LABEL);
        record.put("label", value);
        return record;
    }

    @Test
    public void requestedContainersAndStringElementsAreReturned() {
        List<String> values = Arrays.asList("first", "first", null, "\u540d\ud83d\ude00", "last");
        String encoded = parser.serialize(values, AvroSerConfig.create().setSchema(STRINGS));
        AvroDeserConfig config = AvroDeserConfig.create().setSchema(STRINGS);
        List<String> list = parser.deserialize(encoded, config, Type.of("List<String>"));
        assertEquals("first", list.get(0));
        assertEquals(values, list);
        Set<String> set = parser.deserialize(encoded, config, Type.of("Set<String>"));
        assertEquals(new LinkedHashSet<>(values), set);
        LinkedHashSet<String> ordered = parser.deserialize(encoded, config, Type.of("LinkedHashSet<String>"));
        assertEquals(Arrays.asList("first", null, "\u540d\ud83d\ude00", "last"), new LinkedList<>(ordered));
        LinkedList<String> linked = parser.deserialize(encoded, config, Type.of("LinkedList<String>"));
        assertEquals(values, linked);
        assertEquals("last", linked.getLast());
    }

    @Test
    public void nestedArraysHonorEveryDeclaredContainerAndElementType() throws Exception {
        Schema schema = Schema.createArray(STRINGS);
        byte[] bytes = container(schema, List.of(Arrays.asList("a", "a", null), List.of("\u540d\ud83d\ude00")));
        LinkedList<LinkedList<String>> lists = read(bytes, schema, Type.of("LinkedList<LinkedList<String>>"));
        assertInstanceOf(LinkedList.class, lists.getFirst());
        assertEquals(Arrays.asList("a", "a", null), lists.getFirst());
        assertEquals("\u540d\ud83d\ude00", lists.getLast().getFirst());
        List<Set<String>> sets = read(bytes, schema, Type.of("List<Set<String>>"));
        assertEquals(new LinkedHashSet<>(Arrays.asList("a", null)), sets.get(0));
    }

    @Test
    public void nestedMapsConvertKeysValuesAndCollections() throws Exception {
        Schema schema = Schema.createArray(Schema.createMap(STRINGS));
        byte[] bytes = container(schema, List.of(Map.of("\u540d", Arrays.asList("x", "x", null))));
        List<Map<String, Set<String>>> values = read(bytes, schema, Type.of("List<Map<String,Set<String>>>"));
        String key = values.get(0).keySet().iterator().next();
        assertEquals("\u540d", key);
        assertEquals(new LinkedHashSet<>(Arrays.asList("x", null)), values.get(0).get(key));
    }

    @Test
    public void arrayRecordsAndRecordSequencesRemainDistinct() throws Exception {
        Schema array = Schema.createArray(LABEL);
        byte[] arrayData = container(array, List.of(label("a"), label("\u540d\ud83d\ude00")));
        byte[] recordData = container(LABEL, label("a"), label("\u540d\ud83d\ude00"));
        Type<LinkedList<LabelBean>> beanType = Type.of("LinkedList<" + LabelBean.class.getName() + ">");
        for (boolean arrayInput : List.of(false, true)) {
            Schema schema = arrayInput ? array : LABEL;
            byte[] data = arrayInput ? arrayData : recordData;
            LinkedList<LabelBean> beans = read(data, schema, beanType);
            assertEquals(2, beans.size());
            assertEquals("a", beans.getFirst().getLabel());
            assertEquals("\u540d\ud83d\ude00", beans.getLast().getLabel());
            List<Map<String, String>> maps = read(data, schema, Type.of("List<Map<String,String>>"));
            String value = maps.get(0).get("label");
            assertEquals("a", value);
            List<GenericRecord> records = read(data, schema, Type.of("List<org.apache.avro.generic.GenericRecord>"));
            assertEquals(2, records.size());
            assertInstanceOf(Utf8.class, records.get(0).get("label"));
        }
    }

    @Test
    public void untypedElementsRetainAvroValuesAndConfigOverridesElementType() throws Exception {
        byte[] data = container(STRINGS, List.of("\u540d\ud83d\ude00"));
        List<Object> raw = read(data, STRINGS, Type.of("List<Object>"));
        assertInstanceOf(Utf8.class, raw.get(0));
        AvroDeserConfig config = AvroDeserConfig.create().setSchema(STRINGS).setElementType(String.class);
        List<CharSequence> strings = parser.deserialize(new ByteArrayInputStream(data), config, Type.of("List<CharSequence>"));
        assertInstanceOf(String.class, strings.get(0));
        assertEquals("\u540d\ud83d\ude00", strings.get(0));
        List<Object> records = read(container(Schema.createArray(LABEL), List.of(label("x"))), Schema.createArray(LABEL), Type.of("List<Object>"));
        assertInstanceOf(GenericRecord.class, records.get(0));
    }

    @Test
    public void emptyArraysZeroDatumFilesAndFirstArrayBehaviorRemainStable() throws Exception {
        assertEquals(Set.of(), read(container(STRINGS, List.of()), STRINGS, Type.of("Set<String>")));
        assertNull(read(container(STRINGS), STRINGS, Type.of("List<String>")));
        assertEquals(List.of(), read(container(LABEL), LABEL, Type.of("List<" + LabelBean.class.getName() + ">")));
        assertEquals(List.of("first"), read(container(STRINGS, List.of("first"), List.of("second")), STRINGS, Type.of("List<String>")));
    }

    @Test
    public void nullableNumericBoundariesConvertWithoutLosingRange() throws Exception {
        Schema schema = new Schema.Parser().parse("{\"type\":\"array\",\"items\":[\"null\",\"long\"]}");
        List<Long> values = Arrays.asList(Long.MIN_VALUE, 0L, Long.MAX_VALUE, null);
        assertEquals(values, read(container(schema, values), schema, Type.of("LinkedList<Long>")));
        assertEquals(Arrays.asList((short) -32768, (short) 32767, null),
                read(container(schema, Arrays.asList(-32768L, 32767L, null)), schema, Type.of("List<Short>")));
        assertThrows(ArithmeticException.class, () -> read(container(schema, List.of(32768L)), schema, Type.of("List<Short>")));
    }

    @Test
    public void sourcesAndCallerStreamOwnershipRemainCorrect() throws Exception {
        byte[] data = container(STRINGS, List.of("\u540d\ud83d\ude00"));
        AvroDeserConfig config = AvroDeserConfig.create().setSchema(STRINGS);
        Type<LinkedList<String>> type = Type.of("LinkedList<String>");
        assertEquals(List.of("\u540d\ud83d\ude00"), parser.deserialize(Strings.base64Encode(data), config, type));
        Path file = Files.createTempFile("parser-avro-collection-", ".avro");
        try {
            Files.write(file, data);
            LinkedList<String> result = parser.deserialize(file.toFile(), config, type);
            assertEquals("\u540d\ud83d\ude00", result.getFirst());
        } finally {
            Files.deleteIfExists(file);
        }
        class TrackedInput extends ByteArrayInputStream {
            boolean closed;

            TrackedInput() {
                super(data);
            }

            @Override
            public void close() {
                closed = true;
            }
        }
        TrackedInput input = new TrackedInput();
        assertEquals(List.of("\u540d\ud83d\ude00"), parser.deserialize(input, config, type));
        assertFalse(input.closed);
        assertThrows(IllegalArgumentException.class, () -> parser.deserialize(new ByteArrayInputStream(data), null, type));
    }

    public static class Box<T> {
        private List<T> items;

        public List<T> getItems() {
            return items;
        }

        public void setItems(List<T> items) {
            this.items = items;
        }
    }

    @Test
    public void recordPropertiesAndRootMapsUseTheirFullDeclaredTypes() throws Exception {
        Schema schema = new Schema.Parser()
                .parse("{\"type\":\"record\",\"name\":\"BoxRecord\",\"fields\":[{\"name\":\"items\",\"type\":{\"type\":\"array\",\"items\":\"string\"}}]}");
        GenericRecord record = new GenericData.Record(schema);
        record.put("items", List.of("\u540d\ud83d\ude00"));
        byte[] data = container(schema, record);
        Box<String> bean = read(data, schema, Type.of(Box.class.getName() + "<String>"));
        String item = bean.getItems().get(0);
        assertEquals("\u540d\ud83d\ude00", item);
        Map<String, List<String>> map = read(data, schema, Type.of("Map<String,List<String>>"));
        assertEquals("\u540d\ud83d\ude00", map.get("items").get(0));
    }

    @Test
    public void specificRecordArraysUseArrayShapeAndRequestedContainer() throws Exception {
        com.landawn.abacus.parser.entity.User user = new com.landawn.abacus.parser.entity.User("\u540d\ud83d\ude00", 7, "blue");
        Schema schema = Schema.createArray(user.getSchema());
        byte[] data = container(schema, List.of(user));
        Type<LinkedList<com.landawn.abacus.parser.entity.User>> type = Type.of("LinkedList<com.landawn.abacus.parser.entity.User>");
        LinkedList<com.landawn.abacus.parser.entity.User> users = parser.deserialize(new ByteArrayInputStream(data), null, type);
        assertEquals("\u540d\ud83d\ude00", users.getFirst().getName().toString());
        assertEquals(Integer.valueOf(7), users.getFirst().getFavoriteNumber());
    }
}
