package com.landawn.abacus.util;

import com.landawn.abacus.TestBase;

public abstract class ArrayTestSupport extends TestBase {

    /** A class used only as a cache key probe; must never end up pinned in {@code N.CLASS_EMPTY_ARRAY}. */
    protected static final class EmptyArrayCacheProbe {
    }
}
