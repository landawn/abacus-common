package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.function.IntBiFunction;
import com.landawn.abacus.util.stream.Stream;

@Tag("old-test")
public class SplitterTest extends AbstractTest {

    @Test
    public void test_62026064() {
        final String key = "mykey";
        final String value = "=2>@C=b";
        final String combined = key + "=" + value;

        N.println(Splitter.with('=').limit(2).omitEmptyStrings(true).split(combined));
    }

    @Test
    public void test_splitToCount() {
        final int[] a = Array.rangeClosed(1, 7);

        final IntBiFunction<int[]> func = (fromIndex, toIndex) -> CommonUtil.copyOfRange(a, fromIndex, toIndex);

        N.splitByChunkCount(7, 5, true, func).forEach(Fn.println());
        N.splitByChunkCount(7, 5, false, func).forEach(Fn.println());

        N.println("================================================");

        N.splitByChunkCount(7, 5, true, func).forEach(Fn.println());
        N.splitByChunkCount(7, 5, false, func).forEach(Fn.println());

        N.println("================================================");

        Stream.splitByChunkCount(7, 5, true, func).skip(1).forEach(Fn.println());
        Stream.splitByChunkCount(7, 5, false, func).skip(1).forEach(Fn.println());

        N.println("================================================");

        Stream.splitByChunkCount(7, 5, true, func).skip(2).forEach(Fn.println());
        Stream.splitByChunkCount(7, 5, false, func).skip(2).forEach(Fn.println());

        N.println("================================================");

        Stream.splitByChunkCount(7, 5, true, func).skip(5).forEach(Fn.println());
        Stream.splitByChunkCount(7, 5, false, func).skip(5).forEach(Fn.println());

        N.println("================================================");

        Stream.splitByChunkCount(7, 5, true, func).skip(6).forEach(Fn.println());
        Stream.splitByChunkCount(7, 5, false, func).skip(6).forEach(Fn.println());

        N.println("================================================");

        N.splitByChunkCount(7, 8, true, func).forEach(Fn.println());
        N.splitByChunkCount(7, 8, false, func).forEach(Fn.println());

        N.println("================================================");

        N.splitByChunkCount(0, 5, true, func).forEach(Fn.println());
        N.splitByChunkCount(0, 5, false, func).forEach(Fn.println());
    }

    @Test
    public void test_03() {
        final String str = "3341     Wed. Apr 10, 2019          4          16          22          31          42          4     ";
        final String[] strs = Splitter.with("    ").trim(true).splitToArray(str);
        N.println(strs);
    }

    @Test
    public void test_01() {
        String source = "aaaaa";
        N.println(Splitter.with("aa").split(source));
        assertTrue(CommonUtil.equals(Array.of("", "", "", "", "", ""), Splitter.with("a").splitToArray(source)));
        assertTrue(CommonUtil.equals(CommonUtil.asList("", "", "", "", "", ""), com.google.common.base.Splitter.on("a").splitToList(source)));

        assertTrue(CommonUtil.equals(Array.of("", "", "a"), Splitter.with("aa").splitToArray(source)));
        assertTrue(CommonUtil.equals(CommonUtil.asList("", "", "a"), com.google.common.base.Splitter.on("aa").splitToList(source)));

        assertTrue(CommonUtil.equals(Array.of("", "", "", "", "", ""), Splitter.pattern("a").splitToArray(source)));
        assertTrue(CommonUtil.equals(CommonUtil.asList("", "", "", "", "", ""), com.google.common.base.Splitter.onPattern("a").splitToList(source)));

        assertTrue(CommonUtil.equals(Array.of("", "", "a"), Splitter.pattern("aa").splitToArray(source)));
        assertTrue(CommonUtil.equals(CommonUtil.asList("", "", "a"), com.google.common.base.Splitter.onPattern("aa").splitToList(source)));

        source = "a   b \t \n c \n \t \r d " + '\u0009' + '\u000B' + '\u000C' + " \re";
        N.println(source);
        N.println(Splitter.with(Splitter.WHITE_SPACE_PATTERN).split(source));
    }

    @Test
    public void test_02() {
        try {
            N.println(Splitter.with(Pattern.compile("a*")).split("aaa"));
            fail("IllegalArgumentException should be threw");
        } catch (final IllegalArgumentException e) {

        }

    }

}
