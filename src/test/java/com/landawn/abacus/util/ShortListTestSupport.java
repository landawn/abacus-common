package com.landawn.abacus.util;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.TestBase;

public abstract class ShortListTestSupport extends TestBase {

    protected ShortList list;

    @BeforeEach
    public void setUp() {
        list = new ShortList();
    }

    // replaceRange with empty replacement removes range

    // addAll(index, ShortList) with empty list returns false

    // addAll(index, short[]) with empty array returns false

    // removeIf where no element matches returns false (batchRemove returns 0)

    // removeDuplicates on list with fewer than 2 elements returns false

    // removeAll(ShortList) with empty list returns false

    // removeAll(short[]) with empty array returns false

    // retainAll(ShortList) with empty list clears and returns true if non-empty

    // retainAll(short[]) with empty array clears and returns true if non-empty

    // ---- newly added tests for uncovered methods ----

    // removeAllAt(int...) with no indices is a no-op

    // --- Missing dedicated tests ---

    //
    // ============================ review fixes 2026-09-06 ============================
    //
}
