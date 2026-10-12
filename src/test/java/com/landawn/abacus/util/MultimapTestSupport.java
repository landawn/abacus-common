package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.AbstractTest;

public abstract class MultimapTestSupport extends AbstractTest {

    protected ListMultimap<String, Integer> listMultimap;

    protected Multimap<String, Integer, List<Integer>> multimap;

    @SuppressWarnings("rawtypes")
    protected SetMultimap setMultimap;

    @BeforeEach
    @SuppressWarnings("rawtypes")
    public void setUp() {
        listMultimap = CommonUtil.newListMultimap();
        multimap = CommonUtil.newListMultimap();
        setMultimap = CommonUtil.newSetMultimap();
    }

    protected Multimap<String, Integer, List<Integer>> getTestMultimap() {
        return CommonUtil.newListMultimap();
    }

    protected Multimap<String, Integer, Set<Integer>> getSetTestMultimap() {
        return CommonUtil.newSetMultimap();
    }

    protected static ListMultimap<String, Integer> listOf(final Object... keyValues) {
        final ListMultimap<String, Integer> mm = new ListMultimap<>(LinkedHashMap::new, ArrayList::new);

        for (int i = 0; i < keyValues.length; i += 2) {
            mm.put((String) keyValues[i], (Integer) keyValues[i + 1]);
        }

        return mm;
    }
}
