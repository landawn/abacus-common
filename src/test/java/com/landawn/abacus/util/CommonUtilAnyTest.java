package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class CommonUtilAnyTest extends CommonUtilTestSupport {

    @Test
    public void testAnyNull() {
        assertTrue(CommonUtil.anyNull(null, "a"));
        assertTrue(CommonUtil.anyNull("a", null));
        assertTrue(CommonUtil.anyNull(null, null));
        assertFalse(CommonUtil.anyNull("a", "b"));

        assertTrue(CommonUtil.anyNull(null, "a", "b"));
        assertTrue(CommonUtil.anyNull("a", null, "b"));
        assertTrue(CommonUtil.anyNull("a", "b", null));
        assertTrue(CommonUtil.anyNull(null, null, "b"));
        assertFalse(CommonUtil.anyNull("a", "b", "c"));

        assertTrue(CommonUtil.anyNull(new Object[] { "a", null, "c" }));
        assertFalse(CommonUtil.anyNull(new Object[] { "a", "b", "c" }));
        assertFalse(CommonUtil.anyNull(new Object[0]));
        assertFalse(CommonUtil.anyNull((Object[]) null));
        assertFalse(CommonUtil.anyNull());
        assertTrue(CommonUtil.anyNull("a", null, "c", "d"));

        assertFalse(CommonUtil.anyNull((Iterable<?>) null));
        assertFalse(CommonUtil.anyNull(Collections.emptyList()));
        assertFalse(CommonUtil.anyNull(Arrays.asList("a", "b")));
        assertTrue(CommonUtil.anyNull(Arrays.asList("a", null, "b")));
        List<String> listWithNull = new ArrayList<>();
        listWithNull.add(null);
        assertTrue(CommonUtil.anyNull(listWithNull));
    }

    @Test
    public void testAnyEmpty() {
        assertTrue(CommonUtil.anyEmpty("", "a"));
        assertTrue(CommonUtil.anyEmpty("a", ""));
        assertTrue(CommonUtil.anyEmpty(null, "a"));
        assertFalse(CommonUtil.anyEmpty("a", "b"));
        assertTrue(CommonUtil.anyEmpty("", "a", "b"));
        assertTrue(CommonUtil.anyEmpty("a", "", "b"));
        assertFalse(CommonUtil.anyEmpty("a", "b", "c"));

        assertTrue(CommonUtil.anyEmpty((String) null));
        assertFalse(CommonUtil.anyEmpty((String[]) null));
        assertFalse(CommonUtil.anyEmpty((CharSequence[]) null));
        assertFalse(CommonUtil.anyEmpty(new CharSequence[0]));
        assertTrue(CommonUtil.anyEmpty(null, "foo"));
        assertTrue(CommonUtil.anyEmpty("", "bar"));
        assertFalse(CommonUtil.anyEmpty("foo", "bar"));
        assertFalse(CommonUtil.anyEmpty(new String[] {}));
        assertTrue(CommonUtil.anyEmpty(new String[] { "" }));

        assertTrue(CommonUtil.anyEmpty(Arrays.asList(null, "a")));
        assertTrue(CommonUtil.anyEmpty(Arrays.asList("", "a")));
        assertFalse(CommonUtil.anyEmpty(Arrays.asList("a", "b")));
        assertFalse(CommonUtil.anyEmpty(Collections.<CharSequence> emptyList()));
        assertFalse(CommonUtil.anyEmpty((Iterable<? extends CharSequence>) null));

        assertTrue(CommonUtil.anyEmpty(null, new Object[] { "a" }));
        assertTrue(CommonUtil.anyEmpty(new Object[0], new Object[] { "a" }));
        assertTrue(CommonUtil.anyEmpty(new Object[] { "a" }, null));
        assertTrue(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[0]));
        assertFalse(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[] { "b" }));
        assertTrue(CommonUtil.anyEmpty(null, new Object[] { "a" }, new Object[] { "b" }));
        assertTrue(CommonUtil.anyEmpty(new Object[0], new Object[] { "a" }, new Object[] { "b" }));
        assertTrue(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[0], new Object[] { "b" }));
        assertFalse(CommonUtil.anyEmpty(new Object[] { "a" }, new Object[] { "b" }, new Object[] { "c" }));

        List<String> empty = new ArrayList<>();
        List<String> nonEmpty = Arrays.asList("a");
        assertTrue(CommonUtil.anyEmpty(empty, nonEmpty));
        assertTrue(CommonUtil.anyEmpty(nonEmpty, empty));
        assertFalse(CommonUtil.anyEmpty(nonEmpty, nonEmpty));
        assertTrue(CommonUtil.anyEmpty(null, Arrays.asList("a")));
        assertTrue(CommonUtil.anyEmpty(Collections.emptyList(), Arrays.asList("a")));
        assertTrue(CommonUtil.anyEmpty(null, Arrays.asList("a"), Arrays.asList("b")));
        assertTrue(CommonUtil.anyEmpty(Collections.emptyList(), Arrays.asList("a"), Arrays.asList("b")));
        assertTrue(CommonUtil.anyEmpty(Arrays.asList("a"), Collections.emptyList(), Arrays.asList("b")));
        assertFalse(CommonUtil.anyEmpty(Arrays.asList("a"), Arrays.asList("b"), Arrays.asList("c")));

        Map<String, String> nonEmptyMap = new HashMap<>();
        nonEmptyMap.put("k", "v");
        assertTrue(CommonUtil.anyEmpty(null, nonEmptyMap));
        assertTrue(CommonUtil.anyEmpty(Collections.emptyMap(), nonEmptyMap));
        assertFalse(CommonUtil.anyEmpty(nonEmptyMap, nonEmptyMap));
        assertTrue(CommonUtil.anyEmpty(null, nonEmptyMap, nonEmptyMap));
        assertTrue(CommonUtil.anyEmpty(Collections.emptyMap(), nonEmptyMap, nonEmptyMap));
        assertTrue(CommonUtil.anyEmpty(nonEmptyMap, Collections.emptyMap(), nonEmptyMap));
        assertFalse(CommonUtil.anyEmpty(nonEmptyMap, nonEmptyMap, nonEmptyMap));
    }

    @Test
    public void testAnyBlank() {
        assertTrue(CommonUtil.anyBlank("", "a"));
        assertTrue(CommonUtil.anyBlank(" ", "a"));
        assertTrue(CommonUtil.anyBlank("a", " "));
        assertFalse(CommonUtil.anyBlank("a", "b"));
        assertTrue(CommonUtil.anyBlank("", "a", "b"));
        assertTrue(CommonUtil.anyBlank("a", " ", "b"));
        assertFalse(CommonUtil.anyBlank("a", "b", "c"));
        assertTrue(CommonUtil.anyBlank(null, "a"));
        assertTrue(CommonUtil.anyBlank("a", null));
        assertTrue(CommonUtil.anyBlank(null, "a", "b"));

        assertFalse(CommonUtil.anyBlank((CharSequence[]) null));
        assertFalse(CommonUtil.anyBlank());
        assertTrue(CommonUtil.anyBlank((CharSequence) null));
        assertTrue(CommonUtil.anyBlank(" "));
        assertTrue(CommonUtil.anyBlank("a", " "));
        assertFalse(CommonUtil.anyBlank("a", "b"));
        assertTrue(CommonUtil.anyBlank(null, "foo"));
        assertTrue(CommonUtil.anyBlank("", "bar"));
        assertTrue(CommonUtil.anyBlank("bob", ""));
        assertTrue(CommonUtil.anyBlank("  bob  ", null));
        assertTrue(CommonUtil.anyBlank(" ", "bar"));
        assertFalse(CommonUtil.anyBlank("foo", "bar"));
        assertFalse(CommonUtil.anyBlank(new String[] {}));
        assertTrue(CommonUtil.anyBlank(new String[] { "" }));

        assertFalse(CommonUtil.anyBlank((Iterable<CharSequence>) null));
        assertFalse(CommonUtil.anyBlank(Collections.emptyList()));
        assertTrue(CommonUtil.anyBlank(Arrays.asList(null, "a")));
        assertTrue(CommonUtil.anyBlank(Arrays.asList(" ", "a")));
        assertFalse(CommonUtil.anyBlank(Arrays.asList("a", "b")));
        List<CharSequence> list = new ArrayList<>();
        list.add("test");
        assertFalse(CommonUtil.anyBlank(list));
        list.add(null);
        assertTrue(CommonUtil.anyBlank(list));
        list.clear();
        list.add("");
        assertTrue(CommonUtil.anyBlank(list));
        list.clear();
        list.add(" ");
        assertTrue(CommonUtil.anyBlank(list));
    }
}
