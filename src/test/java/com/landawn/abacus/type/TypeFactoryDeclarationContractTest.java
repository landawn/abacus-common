package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.HBaseColumn;
import com.landawn.abacus.util.Holder;
import com.landawn.abacus.util.ImmutableList;
import com.landawn.abacus.util.ImmutableMap;
import com.landawn.abacus.util.ImmutableSet;
import com.landawn.abacus.util.Indexed;
import com.landawn.abacus.util.ListMultimap;
import com.landawn.abacus.util.Multimap;
import com.landawn.abacus.util.Multiset;
import com.landawn.abacus.util.Pair;
import com.landawn.abacus.util.Range;
import com.landawn.abacus.util.SetMultimap;
import com.landawn.abacus.util.Timed;
import com.landawn.abacus.util.Triple;
import com.landawn.abacus.util.Tuple;
import com.landawn.abacus.util.u;

class TypeFactoryDeclarationContractTest extends TestBase {
    static java.util.stream.Stream<Class<?>> noConstructorParameters() {
        return java.util.stream.Stream.of(java.util.Optional.class, u.Optional.class, u.Nullable.class, Holder.class, Multiset.class, ListMultimap.class,
                SetMultimap.class, Multimap.class, Range.class, HBaseColumn.class, ImmutableList.class, ImmutableSet.class, ImmutableMap.class, Pair.class,
                Triple.class, Tuple.Tuple1.class, Tuple.Tuple2.class, Tuple.Tuple3.class, Tuple.Tuple4.class, Tuple.Tuple5.class, Tuple.Tuple6.class,
                Tuple.Tuple7.class, Tuple.Tuple8.class, Tuple.Tuple9.class, Indexed.class, Timed.class, java.util.Map.Entry.class,
                java.util.AbstractMap.SimpleImmutableEntry.class, com.google.common.collect.Multiset.class, com.google.common.collect.Multimap.class);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("noConstructorParameters")
    void rejectsUnsupportedParametersWithoutPoisoningNormalResolution(Class<?> target) {
        String declaration = target.getName() + "(unexpected)";
        assertThrows(IllegalArgumentException.class, () -> Type.of(declaration));
        assertNotNull(Type.of(target));
        assertThrows(IllegalArgumentException.class, () -> Type.of(declaration));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Clazz<String,Integer>", "Type<String,Integer>", "JSON<String,Integer>", "XML<String,Integer>",
            "com.landawn.abacus.util.Holder<String,Integer>", "com.landawn.abacus.util.Pair<String>", "com.landawn.abacus.util.Triple<String,Integer>",
            "com.google.common.collect.Multiset<String,Integer>", "com.google.common.collect.Multimap<String>", "Clazz<String>(unexpected)",
            "Type<String>(unexpected)", "JSON<String>(unexpected)", "XML<String>(unexpected)" })
    void rejectsInvalidGenericArityAndSpecialTypeParameters(String declaration) {
        assertThrows(IllegalArgumentException.class, () -> Type.of(declaration));
    }

    @ParameterizedTest
    @ValueSource(strings = { "Boolean", "Character", "Byte", "Short", "Integer", "Long", "Float", "Double", "String", "java.lang.CharSequence",
            "java.util.Date", "java.util.Calendar", "java.time.LocalDate", "org.joda.time.DateTime", "byte[]", "List<String>", "Set<String>",
            "Map<String,Integer>", "com.landawn.abacus.util.MapEntity", "com.landawn.abacus.util.EntityId", "com.landawn.abacus.util.Dataset",
            "java.io.InputStream", "java.io.Reader", "java.nio.ByteBuffer" })
    void upperBoundedWildcardPreservesTheBoundsTypeCategories(String declaration) throws Exception {
        Type<?> bound = Type.of(declaration);
        Type<?> wildcard = Type.of("? extends " + declaration);
        assertEquals(bound.javaType(), wildcard.javaType());
        assertEquals(("? extends " + declaration).replace("<", "&lt;").replace(">", "&gt;"), wildcard.xmlName());
        assertEquals(bound.defaultValue(), wildcard.defaultValue());
        for (Method method : Type.class.getMethods()) {
            if (!Modifier.isStatic(method.getModifiers()) && method.getParameterCount() == 0 && method.getReturnType() == boolean.class
                    && method.getName().startsWith("is")) {
                assertEquals(method.invoke(bound), method.invoke(wildcard), method.getName() + ": " + declaration);
            }
        }
        assertEquals(wildcard, Type.of("? extends " + declaration));
        assertEquals(wildcard.name().hashCode(), wildcard.hashCode());
        assertEquals(wildcard.name(), wildcard.toString());
        assertNotEquals(wildcard, bound);
        assertNotEquals(wildcard, Type.of("? extends java.util.UUID"));
    }

    @Test
    void boundedIntegerUsesTheBoundsValueAndComparisonContracts() throws Exception {
        Type<Integer> bounded = Type.of("? extends Integer");
        assertEquals(7, bounded.valueOf((Object) "7"));
        assertEquals(0, bounded.compare(7, 7));
        assertEquals(-1, Integer.signum(bounded.compare(3, 7)));
        assertEquals(Type.of(Integer.class).isDefaultValue(null), bounded.isDefaultValue(null));
        assertEquals(Type.of(Integer.class).hashCode(7), bounded.hashCode(7));
        assertEquals(Type.of(Integer.class).deepHashCode(7), bounded.deepHashCode(7));
        assertEquals(true, bounded.equals(7, 7));
        assertEquals(false, bounded.deepEquals(7, 8));
        assertEquals("7", bounded.toString(7));
        assertEquals("7", bounded.deepToString(7));
        StringBuilder text = new StringBuilder();
        bounded.appendTo(text, 7);
        assertEquals("7", text.toString());
    }
}
