package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class PrimitiveBooleanArrayTypeCoverageTest extends TestBase {

    @Test
    public void testArrayToCollection() {
        Type<boolean[]> type = TypeFactory.getType(boolean[].class);
        List<Object> out = new ArrayList<>();
        type.arrayToCollection(new boolean[] { true, false, true }, out);
        assertEquals(List.of(true, false, true), out);

        List<Object> empty = new ArrayList<>();
        type.arrayToCollection(new boolean[0], empty);
        assertTrue(empty.isEmpty());
        type.arrayToCollection(null, empty);
        assertTrue(empty.isEmpty());
    }
}
