package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;

import org.apache.avro.Schema;
import org.apache.avro.generic.GenericData;
import org.apache.avro.generic.GenericRecord;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

@Tag("unit")
class AvroNullPropertyRegressionTest extends TestBase {
    private static final Schema SCHEMA = new Schema.Parser().parse("""
            {"type":"record","name":"NullableDefaults","fields":[
              {"name":"name","type":["null","string"]},
              {"name":"count","type":["null","int"]},
              {"name":"primitiveCount","type":["null","int"]}
            ]}
            """);

    @Test
    void explicitNullFieldsReplaceConstructorDefaults() {
        final AvroParser parser = ParserFactory.createAvroParser();
        final GenericRecord record = new GenericData.Record(SCHEMA);
        final String encoded = parser.serialize(record, AvroSerConfig.create().setSchema(SCHEMA));
        final Defaults result = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(SCHEMA), Defaults.class);

        assertNull(result.getName());
        assertNull(result.getCount());
        assertEquals(0, result.getPrimitiveCount());
    }

    @Test
    void explicitNullFieldsArePreservedForTypedRecordCollections() {
        final AvroParser parser = ParserFactory.createAvroParser();
        final GenericRecord record = new GenericData.Record(SCHEMA);
        final String encoded = parser.serialize(List.of(record), AvroSerConfig.create().setSchema(SCHEMA));
        final List<Defaults> result = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(SCHEMA),
                Type.of("java.util.List<" + Defaults.class.getName() + ">"));

        assertEquals(1, result.size());
        assertNull(result.get(0).getName());
        assertNull(result.get(0).getCount());
        assertEquals(0, result.get(0).getPrimitiveCount());
    }

    public static class Defaults {
        private String name = "constructor name";
        private Integer count = 42;
        private int primitiveCount = 7;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public Integer getCount() {
            return count;
        }

        public void setCount(final Integer count) {
            this.count = count;
        }

        public int getPrimitiveCount() {
            return primitiveCount;
        }

        public void setPrimitiveCount(final int primitiveCount) {
            this.primitiveCount = primitiveCount;
        }
    }

    // ---- bug review 2026-10-02 final G127 begin ----
    private static final Schema G127_SCHEMA = new Schema.Parser().parse("""
            {"type":"record","name":"G127Holder","fields":[
              {"name":"inner","type":["null",{"type":"record","name":"G127Inner","fields":[{"name":"value","type":["null","string"]}]}]},
              {"name":"tags","type":["null",{"type":"array","items":"string"}]},
              {"name":"scores","type":["null",{"type":"map","values":"int"}]},
              {"name":"ratio","type":["null","double"]},
              {"name":"flag","type":["null","boolean"]}
            ]}
            """);

    private static final Schema G127_BUILDER_SCHEMA = new Schema.Parser().parse("""
            {"type":"record","name":"G127Built","fields":[
              {"name":"name","type":["null","string"]},
              {"name":"count","type":["null","int"]}
            ]}
            """);

    // G127: explicit nulls replace an initialized nested bean, list, map and primitive double/boolean fields, and a null
    // field inside a nested record replaces that nested bean's own initializer.
    @Test
    void explicitNullNestedRecordCollectionMapAndPrimitiveFieldsReplaceDefaults() {
        final AvroParser parser = ParserFactory.createAvroParser();
        final AvroSerConfig serConfig = AvroSerConfig.create().setSchema(G127_SCHEMA);
        final AvroDeserConfig deserConfig = AvroDeserConfig.create().setSchema(G127_SCHEMA);

        final G127Holder allNull = parser.deserialize(parser.serialize(new GenericData.Record(G127_SCHEMA), serConfig), deserConfig, G127Holder.class);
        assertNull(allNull.getInner());
        assertNull(allNull.getTags());
        assertNull(allNull.getScores());
        assertEquals(0.0d, allNull.getRatio());
        assertEquals(false, allNull.isFlag());

        final GenericRecord record = new GenericData.Record(G127_SCHEMA);
        record.put("inner", new GenericData.Record(G127_SCHEMA.getField("inner").schema().getTypes().get(1)));
        final G127Holder nestedNull = parser.deserialize(parser.serialize(record, serConfig), deserConfig, G127Holder.class);
        assertNull(nestedNull.getInner().getValue());
    }

    // G127: a builder-built bean gets explicit nulls too (the builder's own defaults are replaced; null into int gives 0).
    @Test
    void explicitNullFieldsReplaceBuilderDefaults() {
        final AvroParser parser = ParserFactory.createAvroParser();
        final String encoded = parser.serialize(new GenericData.Record(G127_BUILDER_SCHEMA), AvroSerConfig.create().setSchema(G127_BUILDER_SCHEMA));
        final G127Built result = parser.deserialize(encoded, AvroDeserConfig.create().setSchema(G127_BUILDER_SCHEMA), G127Built.class);

        assertNull(result.getName());
        assertEquals(0, result.getCount());
    }

    // G127 guard: non-null values (nested record, array, map, primitives) are still read exactly as written.
    @Test
    void nonNullFieldsAreReadAsWritten() {
        final AvroParser parser = ParserFactory.createAvroParser();
        final G127Holder source = new G127Holder();
        final G127Inner inner = new G127Inner();
        inner.setValue("written");
        source.setInner(inner);
        source.setTags(List.of("a", "b"));
        source.setScores(java.util.Map.of("k", 2));
        source.setRatio(2.5d);
        source.setFlag(false);

        final G127Holder result = parser.deserialize(parser.serialize(source, AvroSerConfig.create().setSchema(G127_SCHEMA)),
                AvroDeserConfig.create().setSchema(G127_SCHEMA), G127Holder.class);

        assertEquals("written", result.getInner().getValue());
        assertEquals(List.of("a", "b"), result.getTags());
        assertEquals(java.util.Map.of("k", 2), result.getScores());
        assertEquals(2.5d, result.getRatio());
        assertEquals(false, result.isFlag());
    }

    public static class G127Inner {
        private String value = "inner default";

        public String getValue() {
            return value;
        }

        public void setValue(final String value) {
            this.value = value;
        }
    }

    public static class G127Holder {
        private G127Inner inner = new G127Inner();
        private List<String> tags = new java.util.ArrayList<>(List.of("default"));
        private java.util.Map<String, Integer> scores = new java.util.HashMap<>(java.util.Map.of("default", 1));
        private double ratio = 1.5d;
        private boolean flag = true;

        public G127Inner getInner() {
            return inner;
        }

        public void setInner(final G127Inner inner) {
            this.inner = inner;
        }

        public List<String> getTags() {
            return tags;
        }

        public void setTags(final List<String> tags) {
            this.tags = tags;
        }

        public java.util.Map<String, Integer> getScores() {
            return scores;
        }

        public void setScores(final java.util.Map<String, Integer> scores) {
            this.scores = scores;
        }

        public double getRatio() {
            return ratio;
        }

        public void setRatio(final double ratio) {
            this.ratio = ratio;
        }

        public boolean isFlag() {
            return flag;
        }

        public void setFlag(final boolean flag) {
            this.flag = flag;
        }
    }

    public static final class G127Built {
        private final String name;
        private final int count;

        private G127Built(final String name, final int count) {
            this.name = name;
            this.count = count;
        }

        public static Builder builder() {
            return new Builder();
        }

        public String getName() {
            return name;
        }

        public int getCount() {
            return count;
        }

        public static final class Builder {
            private String name = "builder default";
            private int count = 5;

            public Builder name(final String name) {
                this.name = name;
                return this;
            }

            public Builder count(final int count) {
                this.count = count;
                return this;
            }

            public G127Built build() {
                return new G127Built(name, count);
            }
        }
    }
    // ---- bug review 2026-10-02 final G127 end ----
}
