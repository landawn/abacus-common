package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.AccessFieldByMethod;
import com.landawn.abacus.annotation.Entity;
import com.landawn.abacus.annotation.JsonXmlConfig;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.JsonXmlField.Direction;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.N;

@Tag("unit")
@SuppressWarnings("deprecation")
public class PropInfoAccessContractReview20261010Test extends TestBase {
    private static final List<Class<? extends MutableBase>> READ_ONLY_TYPES = List.of(PublicReadOnly.class, PrivateReadOnly.class, TypeReadOnly.class);

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void annotationsOnFieldsGettersSettersAndTypesRequireAccessors(final boolean asm) {
        final MemberAnnotations bean = new MemberAnnotations();
        final ParserUtil.BeanInfo metadata = info(MemberAnnotations.class, asm);

        for (final String name : List.of("fieldValue", "getterValue", "setterValue")) {
            final ParserUtil.PropInfo property = metadata.getPropInfo(name);
            assertEquals("getter:initial", property.getPropValue(bean));
            property.setPropValue(bean, "changed");
            assertEquals("getter:setter:changed", property.getPropValue(bean));
        }
        assertEquals(3, bean.writes);

        final TypeAnnotations typed = new TypeAnnotations();
        final ParserUtil.PropInfo value = info(TypeAnnotations.class, asm).getPropInfo("value");
        assertEquals("getter:initial", value.getPropValue(typed));
        value.setPropValue(typed, "changed");
        assertEquals("setter:changed", typed.value);
        assertEquals("getter:setter:changed", value.getPropValue(typed));
        assertEquals(1, typed.writes);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void rejectedSetterRunsOnceAndNeverFallsBackToTheField(final boolean asm) {
        final TypeAnnotations bean = new TypeAnnotations();
        final ParserUtil.PropInfo value = info(TypeAnnotations.class, asm).getPropInfo("value");
        assertSame(bean.failure, assertThrows(IllegalArgumentException.class, () -> value.setPropValue(bean, "reject")));
        assertEquals(1, bean.writes);
        assertEquals("initial", bean.value);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void missingSetterDoesNotPermitAnnotatedBackingFieldWrites(final boolean asm) throws Exception {
        for (final Class<? extends MutableBase> type : READ_ONLY_TYPES) {
            final ParserUtil.BeanInfo metadata = info(type, asm);
            final ParserUtil.PropInfo value = metadata.getPropInfo("value");
            assertNotNull(value, type.getSimpleName());
            final MutableBase bean = type.getConstructor().newInstance();
            assertTrue(value.isReadOnlyProperty, type.getSimpleName());
            assertEquals(Direction.SERIALIZE_ONLY, value.jsonXmlExpose);
            assertThrows(UnsupportedOperationException.class, () -> value.setPropValue(bean, "changed"));
            assertThrows(UnsupportedOperationException.class, () -> value.setPropValue(bean, null));
            assertEquals("getter:initial", value.getPropValue(bean));
            assertFalse(metadata.setPropValue(bean, "value", "changed", true));
            assertThrows(IllegalArgumentException.class, () -> metadata.setPropValue(bean, "value", "changed", false));
            assertEquals("initial", backingValue(bean));
        }
    }

    @Test
    void jsonSkipsReadOnlyInputWhileKeepingItSerializable() throws Exception {
        for (final Class<? extends MutableBase> type : READ_ONLY_TYPES) {
            for (final boolean ignoreUnknown : new boolean[] { false, true }) {
                final JsonDeserConfig config = JsonDeserConfig.create().setIgnoreUnmatchedProperty(ignoreUnknown);
                // Skipping a matched read-only property also skips values incompatible with its declared String type.
                final MutableBase bean = N.fromJson("{\"value\":{\"ignored\":[1]},\"mutable\":7}", config, type);
                assertEquals("initial", backingValue(bean));
                assertEquals(7, bean.getMutable());
                assertTrue(N.toJson(bean).contains("\"value\": \"getter:initial\""));
            }
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void explicitDirectionsKeepTheirMeaningForReadOnlyProperties(final boolean asm) throws Exception {
        assumeTrue(!asm || ASMUtil.isASMAvailable());

        // BOTH - the @JsonXmlField default, typically left alone when the annotation only renames or formats the field -
        // cannot be honoured for input by a read-only property, so it is narrowed to SERIALIZE_ONLY: the value is
        // skipped on input instead of failing the whole document.
        for (final Class<? extends MutableBase> type : List.of(ExplicitBoth.class, RenamedOnly.class)) {
            final String key = type == RenamedOnly.class ? "state" : "value";
            final ParserUtil.PropInfo value = info(type, asm).getPropInfo("value");
            assertNotNull(value);
            assertTrue(value.isReadOnlyProperty);
            assertEquals(Direction.SERIALIZE_ONLY, value.jsonXmlExpose, type.getSimpleName());
            final MutableBase bean = type.getConstructor().newInstance();
            assertThrows(UnsupportedOperationException.class, () -> value.setPropValue(bean, "changed"));
            assertEquals("initial", backingValue(bean));
            final MutableBase read = N.fromJson("{\"" + key + "\":\"changed\",\"mutable\":7}", type);
            assertEquals("initial", backingValue(read), type.getSimpleName());
            assertEquals(7, read.getMutable());
            assertTrue(N.toJson(bean).contains("\"" + key + "\": \"initial\""), N.toJson(bean));
        }

        // An explicit DESERIALIZE_ONLY asks for input the property cannot accept, so deserialization rejects it.
        final ParserUtil.PropInfo input = info(ExplicitInput.class, asm).getPropInfo("value");
        assertTrue(input.isReadOnlyProperty);
        assertEquals(Direction.DESERIALIZE_ONLY, input.jsonXmlExpose);
        final ExplicitInput inputBean = new ExplicitInput();
        assertThrows(UnsupportedOperationException.class, () -> input.setPropValue(inputBean, "changed"));
        assertThrows(UnsupportedOperationException.class, () -> N.fromJson("{\"value\":\"changed\"}", ExplicitInput.class));
        assertEquals("initial", backingValue(inputBean));

        for (final Class<?> type : List.of(ExplicitTransient.class, ExplicitIgnored.class)) {
            assertThrows(IllegalArgumentException.class, () -> info(type, asm), type.getSimpleName());
        }
        final ParserUtil.PropInfo allowed = info(ExplicitOutput.class, asm).getPropInfo("value");
        assertTrue(allowed.isReadOnlyProperty);
        assertEquals(Direction.SERIALIZE_ONLY, allowed.jsonXmlExpose);
        assertEquals("initial", N.fromJson("{\"value\":\"changed\"}", ExplicitOutput.class).value);
    }

    @Test
    void beanCopyAndFillRespectAnnotatedReadOnlyTargets() {
        final PrivateReadOnly source = new PrivateReadOnly();
        source.setMutable(7);
        final PrivateReadOnly copied = Beans.copy(source);
        assertEquals(7, copied.getMutable());
        assertEquals("initial", copied.value);

        final Map<String, Object> values = Map.of("value", "changed", "mutable", 9);
        final PrivateReadOnly mapped = Beans.mapToBean(values, true, PrivateReadOnly.class);
        assertEquals(9, mapped.getMutable());
        assertEquals("initial", mapped.value);
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(values, false, PrivateReadOnly.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.copy(source, List.of("mutable", "value")));

        assertThrows(IllegalArgumentException.class, () -> Beans.clearProps(source, "mutable", "value"));
        assertEquals(7, source.getMutable());
        assertThrows(IllegalArgumentException.class, () -> Beans.randomize(source, List.of("mutable", "value")));
        assertEquals(7, source.getMutable());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void getterCollectionAndImmutableConstructionRemainWritable(final boolean asm) {
        Beans.registerXmlBindingClass(GetterCollection.class);
        final ParserUtil.PropInfo collection = info(GetterCollection.class, asm).getPropInfo("value");
        final GetterCollection bean = new GetterCollection();
        final List<String> original = bean.value;
        assertFalse(collection.isReadOnlyProperty);
        collection.setPropValue(bean, List.of("changed"));
        assertSame(original, bean.value);
        assertEquals(List.of("changed"), bean.value);
        assertEquals(1, bean.reads);

        final ParserUtil.BeanInfo recordInfo = info(MethodRecord.class, asm);
        assertFalse(recordInfo.getPropInfo("value").isReadOnlyProperty);
        final Object arguments = recordInfo.createBeanResult();
        recordInfo.getPropInfo("value").setPropValue(arguments, "record");
        assertEquals(new MethodRecord("record"), recordInfo.finishBeanResult(arguments));

        final ParserUtil.BeanInfo builderInfo = info(MethodBuilderBean.class, asm);
        assertFalse(builderInfo.getPropInfo("value").isReadOnlyProperty);
        assertFalse(builderInfo.getPropInfo("derived").isReadOnlyProperty);
        final Object builder = builderInfo.createBeanResult();
        builderInfo.getPropInfo("value").setPropValue(builder, "builder");
        builderInfo.getPropInfo("derived").setPropValue(builder, "ignored");
        final MethodBuilderBean built = builderInfo.finishBeanResult(builder);
        assertEquals("builder", built.getValue());
        assertEquals("derived:builder", built.getDerived());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void volatilePrimitiveAndReferenceFieldAccessKeepsValuesAndBoxing(final boolean asm) {
        // These checks cover dispatch and boxing of the volatile access path, not thread ordering. A plain-read
        // publication race need not reproduce on a given JVM; timing-based assertions would be unreliable.
        final VolatileBean bean = new VolatileBean();
        final ParserUtil.BeanInfo metadata = info(VolatileBean.class, asm);
        final String text = new String("identity");
        metadata.getPropInfo("ready").setPropValue(bean, true);
        metadata.getPropInfo("sequence").setPropValue(bean, 9_000_000_000L);
        metadata.getPropInfo("value").setPropValue(bean, text);
        assertSame(Boolean.TRUE, metadata.getPropInfo("ready").getPropValue(bean));
        final Object sequence = metadata.getPropInfo("sequence").getPropValue(bean);
        assertEquals(Long.valueOf(9_000_000_000L), sequence);
        assertSame(text, metadata.getPropInfo("value").getPropValue(bean));
        assertTrue(bean.ready);
        assertEquals(9_000_000_000L, bean.sequence);
        assertSame(text, bean.value);

        // Select the converted VarHandle write branch directly, without manufacturing conversion failures.
        // This covers volatile-write dispatch and boxing only; it does not establish ordering between threads.
        final ParserUtil.PropInfo sequenceProperty = metadata.getPropInfo("sequence");
        sequenceProperty.failureCountForSetProp = 101;
        sequenceProperty.setPropValue(bean, "9000000001");
        assertEquals(9_000_000_001L, bean.sequence);
        final Object updatedSequence = sequenceProperty.getPropValue(bean);
        assertEquals(Long.valueOf(9_000_000_001L), updatedSequence);
    }

    private static ParserUtil.BeanInfo info(final Class<?> type, final boolean asm) {
        assumeTrue(!asm || ASMUtil.isASMAvailable());
        final ParserUtil.BeanInfo metadata = ParserUtil.getBeanInfo(type, asm);
        for (final ParserUtil.PropInfo property : metadata.propInfoList) {
            assertEquals(asm, property instanceof ParserUtil.ASMPropInfo, type.getSimpleName() + "." + property.name);
        }
        return metadata;
    }

    private static Object backingValue(final Object bean) throws Exception {
        final java.lang.reflect.Field field = bean.getClass().getDeclaredField("value");
        field.setAccessible(true);
        return field.get(bean);
    }

    public static class MutableBase {
        private int mutable = 1;

        public int getMutable() {
            return mutable;
        }

        public void setMutable(final int mutable) {
            this.mutable = mutable;
        }
    }

    @Entity
    public static class PrivateReadOnly extends MutableBase {
        @AccessFieldByMethod
        private String value = "initial";

        public String getValue() {
            return "getter:" + value;
        }
    }

    public static class PublicReadOnly extends MutableBase {
        public String value = "initial";

        @AccessFieldByMethod
        public String getValue() {
            return "getter:" + value;
        }
    }

    @AccessFieldByMethod
    @Entity
    public static class TypeReadOnly extends MutableBase {
        private String value = "initial";

        public String getValue() {
            return "getter:" + value;
        }
    }

    public static class MemberAnnotations {
        @AccessFieldByMethod
        private String fieldValue = "initial";
        private String getterValue = "initial";
        private String setterValue = "initial";
        private int writes;

        public String getFieldValue() {
            return "getter:" + fieldValue;
        }

        public void setFieldValue(final String value) {
            writes++;
            fieldValue = "setter:" + value;
        }

        @AccessFieldByMethod
        public String getGetterValue() {
            return "getter:" + getterValue;
        }

        public void setGetterValue(final String value) {
            writes++;
            getterValue = "setter:" + value;
        }

        public String getSetterValue() {
            return "getter:" + setterValue;
        }

        @AccessFieldByMethod
        public void setSetterValue(final String value) {
            writes++;
            setterValue = "setter:" + value;
        }
    }

    @AccessFieldByMethod
    public static class TypeAnnotations {
        private String value = "initial";
        private int writes;
        private final IllegalArgumentException failure = new IllegalArgumentException("rejected");

        public String getValue() {
            return "getter:" + value;
        }

        public void setValue(final String value) {
            writes++;
            if ("reject".equals(value)) {
                throw failure;
            }
            this.value = "setter:" + value;
        }
    }

    public static class ExplicitBoth extends MutableBase {
        @AccessFieldByMethod
        @JsonXmlField
        public String value = "initial";

        public String getValue() {
            return value;
        }
    }

    public static class RenamedOnly extends MutableBase {
        @AccessFieldByMethod
        @JsonXmlField(name = "state")
        public String value = "initial";

        public String getValue() {
            return value;
        }
    }

    public static class ExplicitInput extends MutableBase {
        @AccessFieldByMethod
        @JsonXmlField(direction = Direction.DESERIALIZE_ONLY)
        public String value = "initial";

        public String getValue() {
            return value;
        }
    }

    public static class ExplicitOutput extends MutableBase {
        @AccessFieldByMethod
        @JsonXmlField(direction = Direction.SERIALIZE_ONLY)
        public String value = "initial";

        public String getValue() {
            return value;
        }
    }

    public static class ExplicitTransient extends MutableBase {
        @AccessFieldByMethod
        @JsonXmlField(direction = Direction.SERIALIZE_ONLY)
        public transient String value = "initial";

        public String getValue() {
            return value;
        }
    }

    @JsonXmlConfig(ignoredFields = "value")
    public static class ExplicitIgnored extends MutableBase {
        @AccessFieldByMethod
        @JsonXmlField(direction = Direction.SERIALIZE_ONLY)
        public String value = "initial";

        public String getValue() {
            return value;
        }
    }

    @AccessFieldByMethod
    public static class GetterCollection {
        @JsonXmlField(direction = Direction.BOTH)
        private final List<String> value = new ArrayList<>();
        private int reads;

        public List<String> getValue() {
            reads++;
            return value;
        }
    }

    @AccessFieldByMethod
    public record MethodRecord(@JsonXmlField(direction = Direction.BOTH) String value) {
    }

    @AccessFieldByMethod
    public static class MethodBuilderBean {
        private final String value;
        private final String derived;

        private MethodBuilderBean(final String value) {
            this.value = value;
            derived = "derived:" + value;
        }

        public String getValue() {
            return value;
        }

        public String getDerived() {
            return derived;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String value;

            public Builder value(final String value) {
                this.value = value;
                return this;
            }

            public MethodBuilderBean build() {
                return new MethodBuilderBean(value);
            }
        }
    }

    public static class VolatileBean {
        private volatile boolean ready;
        private volatile long sequence;
        private volatile String value;

        public boolean isReady() {
            throw new AssertionError("field access expected");
        }

        public void setReady(final boolean ready) {
            throw new AssertionError("field access expected");
        }

        public long getSequence() {
            throw new AssertionError("field access expected");
        }

        public void setSequence(final long sequence) {
            throw new AssertionError("field access expected");
        }

        public String getValue() {
            throw new AssertionError("field access expected");
        }

        public void setValue(final String value) {
            throw new AssertionError("field access expected");
        }
    }
}
