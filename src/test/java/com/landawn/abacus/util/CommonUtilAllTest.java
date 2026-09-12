package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class CommonUtilAllTest extends CommonUtilTestSupport {

    @Test
    public void testAllNull() {
        assertTrue(CommonUtil.allNull(null, null));
        assertFalse(CommonUtil.allNull(null, "a"));
        assertFalse(CommonUtil.allNull("a", null));
        assertFalse(CommonUtil.allNull("a", "b"));

        assertTrue(CommonUtil.allNull(null, null, null));
        assertFalse(CommonUtil.allNull(null, null, "a"));
        assertFalse(CommonUtil.allNull("a", "b", "c"));

        assertTrue(CommonUtil.allNull(new Object[] { null, null, null }));
        assertFalse(CommonUtil.allNull(new Object[] { "a", null, "c" }));
        assertTrue(CommonUtil.allNull(new Object[0]));
        assertTrue(CommonUtil.allNull((Object[]) null));
        assertFalse(CommonUtil.allNull(new Object[] { "test1", "test2" }));

        assertTrue(CommonUtil.allNull((Iterable<?>) null));
        assertTrue(CommonUtil.allNull(Collections.emptyList()));
        assertFalse(CommonUtil.allNull(Arrays.asList("a", "b")));
        assertFalse(CommonUtil.allNull(Arrays.asList("a", null, "b")));
        assertTrue(CommonUtil.allNull(Arrays.asList(null, null)));
    }

    @Test
    public void testAllEmpty() {
        assertTrue(CommonUtil.allEmpty("", ""));
        assertTrue(CommonUtil.allEmpty(null, ""));
        assertFalse(CommonUtil.allEmpty("", "a"));
        assertFalse(CommonUtil.allEmpty("a", "b"));
        assertTrue(CommonUtil.allEmpty("", "", ""));
        assertFalse(CommonUtil.allEmpty("", "", "a"));

        assertTrue(CommonUtil.allEmpty((CharSequence[]) null));
        assertTrue(CommonUtil.allEmpty());
        assertTrue(CommonUtil.allEmpty((CharSequence) null));
        assertTrue(CommonUtil.allEmpty(""));
        assertTrue(CommonUtil.allEmpty(null, ""));
        assertFalse(CommonUtil.allEmpty(null, "a"));
        assertFalse(CommonUtil.allEmpty("", "nonEmpty", ""));
        assertFalse(CommonUtil.allEmpty(" ", "bar"));

        assertTrue(CommonUtil.allEmpty((Iterable<CharSequence>) null));
        assertTrue(CommonUtil.allEmpty(Collections.emptyList()));
        assertTrue(CommonUtil.allEmpty(Arrays.asList(null, "")));
        assertFalse(CommonUtil.allEmpty(Arrays.asList(null, "a")));

        assertTrue(CommonUtil.allEmpty((Object[]) null, (Object[]) null));
        assertTrue(CommonUtil.allEmpty(new Object[0], null));
        assertTrue(CommonUtil.allEmpty(null, new Object[0]));
        assertTrue(CommonUtil.allEmpty(new Object[0], new Object[0]));
        assertFalse(CommonUtil.allEmpty(new Object[] { "a" }, new Object[0]));
        assertTrue(CommonUtil.allEmpty((Object[]) null, (Object[]) null, (Object[]) null));
        assertTrue(CommonUtil.allEmpty(new Object[0], null, new Object[0]));
        assertFalse(CommonUtil.allEmpty(new Object[] { "a" }, new Object[0], null));

        assertTrue(CommonUtil.allEmpty((List) null, (List) null));
        assertTrue(CommonUtil.allEmpty(Collections.emptyList(), null));
        assertTrue(CommonUtil.allEmpty(null, Collections.emptyList()));
        assertTrue(CommonUtil.allEmpty(Collections.emptyList(), Collections.emptyList()));
        assertFalse(CommonUtil.allEmpty(Arrays.asList("a"), Collections.emptyList()));
        assertTrue(CommonUtil.allEmpty((List) null, (List) null, (List) null));
        assertTrue(CommonUtil.allEmpty(Collections.emptyList(), null, Collections.emptyList()));
        assertFalse(CommonUtil.allEmpty(Arrays.asList("a"), Collections.emptyList(), null));

        assertTrue(CommonUtil.allEmpty((Map) null, (Map) null));
        assertTrue(CommonUtil.allEmpty(Collections.emptyMap(), null));
        assertTrue(CommonUtil.allEmpty(null, Collections.emptyMap()));
        assertTrue(CommonUtil.allEmpty(Collections.emptyMap(), Collections.emptyMap()));
        Map<String, String> nonEmpty = new HashMap<>();
        nonEmpty.put("k", "v");
        assertFalse(CommonUtil.allEmpty(nonEmpty, Collections.emptyMap()));
        assertTrue(CommonUtil.allEmpty((Map) null, (Map) null, (Map) null));
        assertTrue(CommonUtil.allEmpty(Collections.emptyMap(), null, Collections.emptyMap()));
        assertFalse(CommonUtil.allEmpty(nonEmpty, Collections.emptyMap(), null));
    }

    @Test
    public void testAllBlank() {
        assertTrue(CommonUtil.allBlank("", ""));
        assertTrue(CommonUtil.allBlank(" ", "  "));
        assertTrue(CommonUtil.allBlank(null, ""));
        assertFalse(CommonUtil.allBlank("", "a"));
        assertTrue(CommonUtil.allBlank("", "", ""));
        assertTrue(CommonUtil.allBlank(" ", "  ", "\t"));
        assertFalse(CommonUtil.allBlank("", "", "a"));
        assertTrue(CommonUtil.allBlank(null, " "));
        assertTrue(CommonUtil.allBlank(null, null));
        assertFalse(CommonUtil.allBlank(null, " ", "a"));
        assertTrue(CommonUtil.allBlank(null, " ", null));

        assertTrue(CommonUtil.allBlank((CharSequence[]) null));
        assertTrue(CommonUtil.allBlank());
        assertTrue(CommonUtil.allBlank((CharSequence) null));
        assertTrue(CommonUtil.allBlank(" "));
        assertTrue(CommonUtil.allBlank(null, " ", "\t"));
        assertFalse(CommonUtil.allBlank(null, "a"));
        assertFalse(CommonUtil.allBlank("  ", "nonBlank", "  "));
        assertTrue(CommonUtil.allBlank(new CharSequence[0]));
        assertTrue(CommonUtil.allBlank("", " "));
        assertFalse(CommonUtil.allBlank("foo", "bar"));

        assertTrue(CommonUtil.allBlank((Iterable<CharSequence>) null));
        assertTrue(CommonUtil.allBlank(Collections.emptyList()));
        assertTrue(CommonUtil.allBlank(Arrays.asList(null, " ", "\t")));
        assertFalse(CommonUtil.allBlank(Arrays.asList(null, "a")));
    }
}
