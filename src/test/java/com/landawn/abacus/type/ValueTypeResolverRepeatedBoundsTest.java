package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
public class ValueTypeResolverRepeatedBoundsTest extends TestBase {
    @Test
    public void everyConstructorVariableOccurrenceChecksItsBounds() {
        InvalidBox.constructorCalls = 0;

        assertThrows(IllegalArgumentException.class, () -> Type.of(InvalidBox.class));
        assertEquals(0, InvalidBox.constructorCalls);
    }

    @Test
    public void repeatedCompatibleBoundsStillDeserialize() {
        final Type<NumberBox> type = Type.of(NumberBox.class);

        assertInstanceOf(ObjectType.class, type);
        assertEquals(Map.of(1, 2), type.valueOf("{\"1\":2}").value());
    }

    @Test
    public void recursiveBoundsStillDeserializeAcrossSiblingArguments() {
        final Type<ComparableBox> type = Type.of(ComparableBox.class);

        assertInstanceOf(ObjectType.class, type);
        assertEquals(Map.of("key", "value"), type.valueOf("{\"key\":\"value\"}").value());
    }

    public static class InvalidBox {
        static int constructorCalls;
        private TreeMap<Integer, String> value;

        public <U extends Number> InvalidBox(final Map<U, U> source) {
            constructorCalls++;
            value = new TreeMap<>();
            source.forEach((key, element) -> value.put(key.intValue(), Integer.toString(element.intValue())));
        }

        public TreeMap<Integer, String> value() {
            return value;
        }
    }

    public static class NumberBox {
        private TreeMap<Integer, Integer> value;

        public <U extends Number> NumberBox(final Map<U, U> source) {
            value = new TreeMap<>();
            source.forEach((key, element) -> value.put(key.intValue(), element.intValue()));
        }

        public TreeMap<Integer, Integer> value() {
            return value;
        }
    }

    public static class ComparableBox {
        private TreeMap<String, String> value;

        public <U extends Comparable<U>> ComparableBox(final Map<U, U> source) {
            value = new TreeMap<>();
            source.forEach((key, element) -> value.put(key.toString(), element.toString()));
        }

        public TreeMap<String, String> value() {
            return value;
        }
    }
}
