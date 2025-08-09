package com.landawn.abacus.util;

import java.util.List;

import org.junit.jupiter.api.Test;

public class ClazzTest {

    @Test
    public void test_01() {
        String json = "[1, 2, 3]";
        List<String> list = N.fromJson(json, Clazz.of(List.class));
        N.println(list);

        N.fromJson(json, Clazz.of(ListMultimap.class));
        N.println(list);
    }

}
