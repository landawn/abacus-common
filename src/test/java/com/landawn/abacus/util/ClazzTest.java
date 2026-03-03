package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ClazzTest {

    @Test
    public void test_01() {
        String json = "[1, 2, 3]";
        List<String> list = N.fromJson(json, Clazz.of(List.class));
        assertNotNull(list);
        assertEquals(3, list.size());

        final String mapJson = "{\"a\":[1,2,3]}";
        final ListMultimap multimap = N.fromJson(mapJson, Clazz.of(ListMultimap.class));
        assertNotNull(multimap);
        assertEquals(3, ((List<?>) multimap.get("a")).size());
    }

}
