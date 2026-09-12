package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class SetMultimapTypeTest extends TestBase {

    private final SetMultimapType type = new SetMultimapType(com.landawn.abacus.util.SetMultimap.class, "String", "String");

    @Test
    public void test_get_ResultSet_byLabel() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Basic get test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.get(rs, "col"));
    }

    @Test
    public void test_set_CallableStatement() throws SQLException {
        CallableStatement stmt = mock(CallableStatement.class);
        // Basic set test - actual implementation will vary by type
        assertDoesNotThrow(() -> type.set(stmt, "param", null));
    }

    // T8-01 / T8-08: valueOf lost the key order (HashMap intermediate) and the value order (HashSet intermediate).
    @Test
    public void reviewFixes20260906_valueOfKeepsDocumentKeyOrderAndRoundTrips() {
        final Type<com.landawn.abacus.util.SetMultimap<String, Integer>> t = TypeFactory.getType("SetMultimap<String, Integer>");
        final String json = "{\"z\": [1], \"a\": [2], \"m\": [3], \"b\": [4], \"q\": [5]}";

        final com.landawn.abacus.util.SetMultimap<String, Integer> sm = t.valueOf(json);

        org.junit.jupiter.api.Assertions.assertEquals(java.util.Arrays.asList("z", "a", "m", "b", "q"), new java.util.ArrayList<>(sm.keySet()));
        org.junit.jupiter.api.Assertions.assertEquals(json, t.stringOf(sm));
        org.junit.jupiter.api.Assertions.assertEquals(json, t.stringOf(t.valueOf(t.stringOf(sm))));

        final String unicode = "{\"é\": [\"中\"], \"à\": [\"x\"], \"😀\": [\"y\"]}";
        final Type<com.landawn.abacus.util.SetMultimap<String, String>> ts = TypeFactory.getType("SetMultimap<String, String>");
        org.junit.jupiter.api.Assertions.assertEquals(unicode, ts.stringOf(ts.valueOf(unicode)));
    }

    @Test
    public void reviewFixes20260906_valueOfValuesAreInsertionOrderedLinkedHashSet() {
        final Type<com.landawn.abacus.util.SetMultimap<String, String>> t = TypeFactory.getType("SetMultimap<String, String>");

        final com.landawn.abacus.util.SetMultimap<String, String> sm = t.valueOf("{\"k\": [\"z\", \"a\", \"m\", \"b\", \"q\", \"z\"]}");

        org.junit.jupiter.api.Assertions.assertTrue(sm.get("k") instanceof java.util.LinkedHashSet, "values class: " + sm.get("k").getClass());
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Arrays.asList("z", "a", "m", "b", "q"), new java.util.ArrayList<>(sm.get("k")));

        org.junit.jupiter.api.Assertions.assertTrue(t.valueOf("{}").isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Arrays.asList("only"), new java.util.ArrayList<>(t.valueOf("{\"only\": [\"v\"]}").keySet()));

        final com.landawn.abacus.util.SetMultimap<String, String> dup = t.valueOf("{\"a\": [\"1\"], \"b\": [\"2\"], \"a\": [\"3\"]}");
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Arrays.asList("a", "b"), new java.util.ArrayList<>(dup.keySet()));
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Collections.singleton("3"), dup.get("a"));

        // T8-05: a null value or an empty array drops the key
        final com.landawn.abacus.util.SetMultimap<String, String> nullValue = t.valueOf("{\"a\": null, \"b\": [\"1\"], \"c\": []}");
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Arrays.asList("b"), new java.util.ArrayList<>(nullValue.keySet()));

        org.junit.jupiter.api.Assertions.assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> t.valueOf("{\"a\": [\"1\"]"));
    }

}
