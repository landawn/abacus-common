package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

@org.junit.jupiter.api.Tag("unit")
public class SplitterEmptyDocsTest extends TestBase {
    @Test
    void emptyTokensFollowTheRequestedTargetTypeForBothOverloads() {
        final Splitter splitter = Splitter.with(",");
        assertEquals(List.of(""), splitter.split("", String.class));
        assertEquals(List.of(""), splitter.split("", Type.of(String.class)));
        assertEquals(Arrays.asList((Integer) null), splitter.split("", Integer.class));
        assertEquals(Arrays.asList((Integer) null), splitter.split("", Type.of(Integer.class)));
        assertEquals(List.of(0), splitter.split("", int.class));
        assertEquals(List.of(0), splitter.split("", Type.of(int.class)));
        assertEquals(List.of("\uD83D\uDE00", "", "x"), splitter.split("\uD83D\uDE00,,x", String.class));
        assertEquals(List.of(), splitter.split(null, String.class));
        assertEquals(List.of(), splitter.split(null, Type.of(String.class)));
        assertEquals(List.of(), Splitter.with(",").omitEmptyStrings().split("", String.class));
    }
}
