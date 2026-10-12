package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.function.BiConsumer;

// Extends TestBase and is tagged "slow-test": performance/memory-tuning regressions stay out of
// the regular suite (AbacusCoreTestSuite excludes "slow-test"). Run this class directly when changing that code.
@Tag("slow-test")
class ParserConfigurationSnapshotPerformanceTest extends TestBase {
    @Test
    void copiesAndTransitiveCopiesKeepIndependentOptions() {
        final BiConsumer<Collection<Object>, Object> firstHandler = Collection::add;
        final BiConsumer<Collection<Object>, Object> secondHandler = (collection, value) -> collection.clear();
        final JsonDeserConfig original = new JsonDeserConfig().setValueType("value", String.class).setPropHandler("items", firstHandler);
        final JsonDeserConfig copy = original.copy();
        final JsonDeserConfig grandchild = copy.copy();

        assertEquals(original, copy);
        assertEquals(original.hashCode(), copy.hashCode());
        original.setValueType("value", Integer.class).setPropHandler("items", secondHandler);
        copy.setValueType("extra", Long.class).setPropHandler("extra", secondHandler);
        grandchild.setValueType("value", Double.class);

        assertEquals(Integer.class, original.getValueType("value").javaType());
        assertEquals(String.class, copy.getValueType("value").javaType());
        assertEquals(Double.class, grandchild.getValueType("value").javaType());
        assertNull(original.getValueType("extra"));
        assertNull(grandchild.getValueType("extra"));
        assertSame(secondHandler, original.getPropHandler("items"));
        assertSame(firstHandler, copy.getPropHandler("items"));
        assertSame(firstHandler, grandchild.getPropHandler("items"));
        assertNull(grandchild.getPropHandler("extra"));
    }

    @Test
    void callerOwnedMapsStayLiveOnlyOnTheOriginalConfiguration() {
        final Map<String, Type<?>> supplied = new HashMap<>();
        supplied.put("value", Type.of(String.class));
        final JsonDeserConfig original = new JsonDeserConfig().setValueTypes(supplied);
        final JsonDeserConfig copy = original.copy();
        final JsonDeserConfig grandchild = copy.copy();

        supplied.put("value", Type.of(Integer.class));
        original.setValueType("extra", Long.class);
        copy.setValueType("value", Double.class);

        assertEquals(Integer.class, original.getValueType("value").javaType());
        assertEquals(Long.class, supplied.get("extra").javaType());
        assertEquals(Double.class, copy.getValueType("value").javaType());
        assertEquals(String.class, grandchild.getValueType("value").javaType());
        assertNull(copy.getValueType("extra"));
        assertNull(grandchild.getValueType("extra"));
    }

    @Test
    void nestedParsingStillInvokesTheConfigurationCopyOverride() {
        final CountingConfig config = new CountingConfig();
        for (int i = 0; i < 1000; i++) {
            config.setValueType("unused" + i, String.class);
        }
        final List<List<Integer>> values = new JsonParserImpl().deserialize("[[1],[2],[3]]", config, Type.of("List<List<Integer>>"));

        assertEquals(List.of(List.of(1), List.of(2), List.of(3)), values);
        assertTrue(config.copies.get() >= 3, "nested configuration copies must still reach subclass hooks");
        assertEquals(1000, config.valueTypeMap.size());
    }

    static final class CountingConfig extends JsonDeserConfig {
        final AtomicInteger copies = new AtomicInteger();

        @Override
        public JsonDeserConfig copy() {
            copies.incrementAndGet();
            return super.copy();
        }
    }
}
