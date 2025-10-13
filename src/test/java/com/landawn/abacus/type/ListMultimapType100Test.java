package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ListMultimapType100Test extends TestBase {

    private ListMultimapType<String, String> stringListMultimapType;
    private ListMultimapType<Integer, Integer> integerListMultimapType;

    @BeforeEach
    public void setUp() {
        stringListMultimapType = (ListMultimapType<String, String>) createType("ListMultimap<String,String>");
        integerListMultimapType = (ListMultimapType<Integer, Integer>) createType("ListMultimap<Integer,Integer>");
    }

    @Test
    public void testStringOf_Null() {
        assertNull(stringListMultimapType.stringOf(null));
    }

    @Test
    public void testValueOf_Null() {
        assertNull(stringListMultimapType.valueOf(null));
    }

    @Test
    public void testValueOf_EmptyString() {
        assertNull(stringListMultimapType.valueOf(""));
    }
}
