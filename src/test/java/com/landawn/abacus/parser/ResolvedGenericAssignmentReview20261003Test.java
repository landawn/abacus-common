package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.AccessFieldByMethod;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.N;

@Tag("unit")
class ResolvedGenericAssignmentReview20261003Test extends TestBase {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void directMetadataConvertsErasedFieldsAndSetters(boolean asm) {
        final ParserUtil.BeanInfo fieldInfo = info(IntegerField.class, asm);
        final IntegerField field = new IntegerField();
        fieldInfo.getPropInfo("value").setPropValue(field, "42");
        assertEquals(Integer.valueOf(42), field.value);
        assertThrows(RuntimeException.class, () -> fieldInfo.getPropInfo("value").setPropValue(field, "invalid"));

        final ParserUtil.BeanInfo setterInfo = info(IntegerSetter.class, asm);
        final IntegerSetter setter = new IntegerSetter();
        setterInfo.getPropInfo("value").setPropValue(setter, "43");
        assertEquals(Integer.valueOf(43), setter.getValue());
        assertEquals(1, setter.calls());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void nullIdentityAndOrdinaryObjectMetadataAreUnchanged(boolean asm) {
        final ParserUtil.BeanInfo typed = info(StringField.class, asm);
        final StringField field = new StringField();
        final String value = new String("identity");
        typed.getPropInfo("value").setPropValue(field, value);
        assertSame(value, field.value);
        typed.getPropInfo("value").setPropValue(field, null);
        assertNull(field.value);

        final Object payload = new Object();
        final PlainObject plain = new PlainObject();
        info(PlainObject.class, asm).getPropInfo("value").setPropValue(plain, payload);
        assertSame(payload, plain.value);
        final AnnotatedObject annotated = new AnnotatedObject();
        info(AnnotatedObject.class, asm).getPropInfo("value").setPropValue(annotated, payload);
        assertSame(payload, annotated.value);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void allSupportedRawJsonAnnotationsNormalizeBeforeGenericConversion(boolean asm) {
        final List<String> value = List.of("quote\"value");
        final RawStrings bean = new RawStrings();
        final ParserUtil.BeanInfo metadata = info(RawStrings.class, asm);
        metadata.getPropInfo("value").setPropValue(bean, value);
        metadata.getPropInfo("jackson").setPropValue(bean, value);
        metadata.getPropInfo("fastjson").setPropValue(bean, value);
        assertEquals(N.toJson(value), bean.value);
        assertEquals(N.toJson(value), bean.jackson);
        assertEquals(N.toJson(value), bean.fastjson);
    }

    @Test
    void copyingUsesTheSameResolvedScalarContract() {
        assertEquals(Integer.valueOf(42), Beans.copyAs(new TextSource(), IntegerField.class).value);
        final IntegerSetter setter = Beans.copyAs(new TextSource(), IntegerSetter.class);
        assertEquals(Integer.valueOf(42), setter.getValue());
        assertEquals(1, setter.calls());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void omittedBuilderPropertyRemainsIgnoredWithoutConversion(boolean asm) {
        final ParserUtil.BeanInfo metadata = info(Omitted.class, asm);
        final Object builder = metadata.createBeanResult();
        assertEquals(Integer.class, metadata.getPropInfo("derived").clazz);
        assertNull(metadata.getPropInfo("derived").setMethod);
        metadata.getPropInfo("derived").setPropValue(builder, "not-a-number");
        metadata.getPropInfo("value").setPropValue(builder, "kept");
        final Omitted result = metadata.finishBeanResult(builder);
        assertEquals(Integer.valueOf(17), result.getDerived());
        assertEquals("kept", result.getValue());
    }

    private static ParserUtil.BeanInfo info(Class<?> type, boolean asm) {
        final ParserUtil.BeanInfo result = ParserUtil.getBeanInfo(type, asm);
        assertEquals(asm ? ParserUtil.ASMPropInfo.class : ParserUtil.PropInfo.class, result.getPropInfo("value").getClass());
        return result;
    }

    public static class GenericField<T> {
        public T value;
    }

    public static class IntegerField extends GenericField<Integer> {
    }

    public static class StringField extends GenericField<String> {
    }

    public static class PlainObject {
        public Object value;
    }

    public static class AnnotatedObject {
        @JsonXmlField(type = "String")
        public Object value;
    }

    public static class TextSource {
        public String value = "42";
    }

    @AccessFieldByMethod
    public static class GenericSetter<T> {
        private T value;
        private int calls;

        public T getValue() {
            return value;
        }

        public void setValue(T value) {
            this.value = value;
            calls++;
        }

        public int calls() {
            return calls;
        }
    }

    public static class IntegerSetter extends GenericSetter<Integer> {
    }

    public static class RawFields<T> {
        @JsonXmlField(isJsonRawValue = true)
        public T value;
        @com.fasterxml.jackson.annotation.JsonRawValue
        public T jackson;
        @com.alibaba.fastjson2.annotation.JSONField(jsonDirect = true)
        public T fastjson;
    }

    public static class RawStrings extends RawFields<String> {
    }

    public static class Derived<T> {
        private final T derived;

        protected Derived(T derived) {
            this.derived = derived;
        }

        public T getDerived() {
            return derived;
        }
    }

    public static final class Omitted extends Derived<Integer> {
        private final String value;

        private Omitted(String value) {
            super(17);
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String value;

            public Builder value(String value) {
                this.value = value;
                return this;
            }

            public Omitted build() {
                return new Omitted(value);
            }
        }
    }
}
