package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class BuilderTest {

    @Test
    public void test_001() {
        Map<String, Integer> m = Builder.of(new HashMap<String, Integer>()).put("ab", 1).put("abc", 1).val();
        N.println(m);

        List<Long> list = Builder.of(new ArrayList<Long>()).val();
        N.println(list);

        Multiset<String> m2 = Builder.of(new Multiset<String>()).add("abc").add("123").val();
        N.println(m2);

        Multimap<String, Integer, List<Integer>> m3 = Builder.of(new Multimap<String, Integer, List<Integer>>()).put("abc", 123).val();
        N.println(m3);

        IntList intList = Builder.of(IntList.of(1, 2, 3)).add(1).remove(2).add(5).removeAll(IntList.of(1)).val();
        N.println(intList);
    }

}
