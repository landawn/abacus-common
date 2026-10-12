package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.AccessFieldByMethod;
import com.landawn.abacus.annotation.JsonXmlField;

@Tag("unit")
class BeansNestedGenericReview20261003Test extends TestBase {
    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void nestedMapsPreserveDeclaredGenericArguments(boolean selected) {
        final Map<String, Object> source = Map.of("box", Map.of("value", "123"));
        final Root bean = selected ? Beans.mapToBean(source, List.of("box"), Root.class) : Beans.mapToBean(source, Root.class);
        assertEquals(Integer.valueOf(123), bean.box.value);
        assertEquals(Integer.class, ((Box<?>) bean.box).value.getClass());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void inheritedArgumentsRemainResolvedAcrossMultipleLevels(boolean selected) {
        final Map<String, Object> source = Map.of("box", Map.of("value", Map.of("value", "456")));
        final Inherited bean = selected ? Beans.mapToBean(source, List.of("box"), Inherited.class) : Beans.mapToBean(source, Inherited.class);
        assertEquals(Integer.valueOf(456), bean.box.value.value);
        assertEquals(Integer.class, ((Box<?>) bean.box.value).value.getClass());
    }

    @Test
    void nestedStrictnessAndSelectionLeniencyRemainDistinct() {
        final Map<String, Object> source = Map.of("box", Map.of("value", "123", "unknown", true));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(source, false, Root.class));
        assertEquals(Integer.valueOf(123), Beans.mapToBean(source, true, Root.class).box.value);
        assertEquals(Integer.valueOf(123), Beans.mapToBean(source, List.of("box"), Root.class).box.value);
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(source, List.of("unknown"), Root.class));
    }

    @Test
    void nullAndRawRootContractsRemainUnchanged() {
        assertNull(Beans.mapToBean(null, Root.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(null, String.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(null, (Class<?>) null));
        final Box<?> raw = Beans.mapToBean(Map.of("value", "raw"), Box.class);
        assertEquals("raw", raw.value);
        final Root empty = Beans.mapToBean(Map.of("box", Map.of("value", "123")), List.of(), Root.class);
        assertNull(empty.box);
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void erasedSetterReceivesConvertedValueOnce(boolean selected) {
        final Map<String, Object> source = Map.of("box", Map.of("value", "789"));
        final SetterRoot bean = selected ? Beans.mapToBean(source, List.of("box"), SetterRoot.class) : Beans.mapToBean(source, SetterRoot.class);
        assertEquals(Integer.valueOf(789), bean.box.getValue());
        assertEquals(1, bean.box.calls());
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void dottedGenericScalarAssignmentUsesResolvedLeaf(boolean selected) {
        final Map<String, Object> source = Map.of("box.value", "321");
        final Root field = selected ? Beans.mapToBean(source, List.of("box.value"), Root.class) : Beans.mapToBean(source, Root.class);
        assertEquals(Integer.valueOf(321), field.box.value);
        final SetterRoot setter = selected ? Beans.mapToBean(source, List.of("box.value"), SetterRoot.class) : Beans.mapToBean(source, SetterRoot.class);
        assertEquals(Integer.valueOf(321), setter.box.getValue());
        assertEquals(1, setter.box.calls());
    }

    @Test
    void compatibleIdentityNullAndRawJsonNormalizationArePreserved() {
        final String text = new String("identity");
        final StringRoot root = Beans.mapToBean(Map.of("box", Map.of("value", text)), StringRoot.class);
        assertSame(text, root.box.value);
        final Root nullValue = Beans.mapToBean(Map.of("box", java.util.Collections.singletonMap("value", null)), Root.class);
        assertNull(nullValue.box.value);
        final List<String> rawValue = List.of("quoted\"value");
        final RawRoot raw = Beans.mapToBean(Map.of("box", Map.of("value", rawValue)), RawRoot.class);
        assertEquals(N.toJson(rawValue), raw.box.value);
    }

    public static class Box<T> {
        public T value;
    }

    public static class Root {
        public Box<Integer> box;
    }

    public static class Base<T> {
        public Box<Box<T>> box;
    }

    public static class Inherited extends Base<Integer> {
    }

    public static class StringRoot {
        public Box<String> box;
    }

    public static class SetterRoot {
        public SetterBox<Integer> box;
    }

    @AccessFieldByMethod
    public static class SetterBox<T> {
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

    public static class RawBox<T> {
        @JsonXmlField(isJsonRawValue = true)
        public T value;
    }

    public static class RawRoot {
        public RawBox<String> box;
    }
}
