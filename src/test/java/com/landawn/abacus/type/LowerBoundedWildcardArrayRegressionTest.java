package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class LowerBoundedWildcardArrayRegressionTest extends TestBase {

    public static final class Fields {
        public List<? super Integer[]> arrays;
        public List<? super List<String>[]> genericArrays;
        public List<? extends Integer[]> upperArrays;
    }

    @Test
    void reflectedLowerBoundPreservesHeterogeneousJsonValues() throws ReflectiveOperationException {
        final java.lang.reflect.Type reflected = Fields.class.getField("arrays").getGenericType();
        final WildcardType wildcard = (WildcardType) ((ParameterizedType) reflected).getActualTypeArguments()[0];
        final Type<List<?>> type = TypeFactory.getType(reflected);

        assertEquals(Object.class, wildcard.getUpperBounds()[0]);
        assertEquals(Object.class, type.elementType().javaType());
        assertFalse(type.elementType().isArray());
        assertEquals("? super Integer[]", type.elementType().name());
        assertEquals(List.of("scalar", 1, List.of(2, 3), Map.of("value", true)), type.valueOf("[\"scalar\", 1, [2, 3], {\"value\": true}]"));
    }

    @Test
    void genericArrayLowerBoundPreservesScalarAndNestedShapes() throws ReflectiveOperationException {
        final Type<List<?>> type = TypeFactory.getType(Fields.class.getField("genericArrays").getGenericType());

        assertEquals(Object.class, type.elementType().javaType());
        assertFalse(type.elementType().isArray());
        assertEquals("? super java.util.List<String>[]", type.elementType().name());
        assertEquals(List.of("scalar", List.of(List.of("nested"))), type.valueOf("[\"scalar\", [[\"nested\"]]]"));
    }

    @Test
    void upperBoundedArrayStillUsesItsArrayConstraint() throws ReflectiveOperationException {
        final Type<List<?>> type = TypeFactory.getType(Fields.class.getField("upperArrays").getGenericType());

        assertTrue(type.elementType().isArray());
        assertEquals(Integer[].class, type.elementType().javaType());
        assertArrayEquals(new Integer[] { 1, 2 }, (Integer[]) type.valueOf("[[1, 2]]").get(0));
    }

    @Test
    void explicitArrayOfUnboundedWildcardsRemainsAnArray() {
        final Type<Object[]> type = TypeFactory.getType("?[]");

        assertTrue(type.isArray());
        assertEquals(Object[].class, type.javaType());
        assertEquals("?", type.elementType().name());
        assertArrayEquals(new Object[] { "scalar", 1 }, type.valueOf("[\"scalar\", 1]"));
    }

    // ---- bug review 2026-10-02 final G128 begin ----

    public static class LowerBoundBeanG128 {
        private List<? super Integer[]> values;
        private Map<String, ? super String[]> byName;

        public List<? super Integer[]> getValues() {
            return values;
        }

        public void setValues(final List<? super Integer[]> values) {
            this.values = values;
        }

        public Map<String, ? super String[]> getByName() {
            return byName;
        }

        public void setByName(final Map<String, ? super String[]> byName) {
            this.byName = byName;
        }
    }

    // A bean written by the library reads back with its scalar members intact; an array-typed handler for "? super X[]"
    // read "s", 1 and "y" back as one-element Object[] arrays.
    @Test
    void beanRoundTripKeepsScalarMembersOfLowerBoundedArrayWildcards() {
        final LowerBoundBeanG128 bean = new LowerBoundBeanG128();
        bean.setValues(new java.util.ArrayList<>(List.of("s", 1)));
        bean.setByName(new java.util.LinkedHashMap<>(Map.of("b", "y")));

        final LowerBoundBeanG128 copy = com.landawn.abacus.util.N.fromJson(com.landawn.abacus.util.N.toJson(bean), LowerBoundBeanG128.class);

        assertEquals(List.of("s", 1), copy.getValues());
        assertEquals(Map.of("b", "y"), copy.getByName());
    }

    // ---- bug review 2026-10-02 final G128 end ----
}
