package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.IdentityHashMap;
import java.util.LinkedHashMap;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class DeepReview20261004BiMapTest extends TestBase {

    @Test
    void toStringHandlesDirectSelfReferences() {
        final BiMap<Object, Object> map = new BiMap<>(IdentityHashMap::new, IdentityHashMap::new);

        map.put("self", map);
        assertEquals("{self=(this Map)}", map.toString());

        map.clear();
        map.put(map, "self");
        assertEquals("{(this Map)=self}", map.toString());

        map.clear();
        map.put(map, map);
        assertEquals("{(this Map)=(this Map)}", map.toString());
    }

    @Test
    void toStringPreservesEmptyAndEncounterOrderFormatting() {
        final BiMap<String, Integer> map = new BiMap<>(LinkedHashMap::new, LinkedHashMap::new);

        assertEquals("{}", map.toString());
        map.put("second", 2);
        map.put("first", 1);
        assertEquals("{second=2, first=1}", map.toString());
    }
}
