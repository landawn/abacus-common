package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.SetMultimap;

public class SetMultimapType100Test extends TestBase {

    private SetMultimapType<String, Integer> setMultimapType;

    @BeforeEach
    public void setUp() {
        setMultimapType = (SetMultimapType<String, Integer>) createType("SetMultimap<String, Integer>");
    }

    @Test
    public void testStringOf() {
        // Test with multimap
        SetMultimap<String, Integer> multimap = N.newLinkedSetMultimap();
        multimap.put("key1", 1);
        multimap.put("key1", 2);
        multimap.put("key2", 3);

        String result = setMultimapType.stringOf(multimap);
        assertNotNull(result);

        // Test with null
        assertNull(setMultimapType.stringOf(null));
    }

    @Test
    public void testValueOf() {
        // Test with valid JSON string
        String json = "{\"key1\":[1,2],\"key2\":[3]}";
        SetMultimap<String, Integer> result = setMultimapType.valueOf(json);
        assertNotNull(result);

        // Test with null/empty string
        assertNull(setMultimapType.valueOf(null));
        assertNull(setMultimapType.valueOf(""));
        assertNull(setMultimapType.valueOf(" "));
    }
}
