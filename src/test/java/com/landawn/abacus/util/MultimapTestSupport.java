package com.landawn.abacus.util;

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

    //
    //
    //
    //
    //
    //

    //
    //
    //

    //
    //

    //

    // --- Additional missing coverage tests ---

    //

    //
    //
    //
    //

    //
    //

    //

    //

    //
    //
    //
    //
    //
    //

    //

    //
    //
    //

    // --- regression tests for 2026-06-10 deep-review fixes ---
}
