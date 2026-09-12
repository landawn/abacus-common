package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.mock;

import java.sql.CallableStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class ListMultimapTypeTest extends TestBase {

    private final ListMultimapType type = new ListMultimapType(com.landawn.abacus.util.ListMultimap.class, "String", "String");

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

    // T8-01: valueOf lost the JSON document order (HashMap intermediate) although the javadoc promises it.
    @Test
    public void reviewFixes20260906_valueOfKeepsDocumentKeyOrderAndRoundTrips() {
        final Type<com.landawn.abacus.util.ListMultimap<String, Integer>> t = TypeFactory.getType("ListMultimap<String, Integer>");
        final String json = "{\"z\": [1], \"a\": [2], \"m\": [3], \"b\": [4], \"q\": [5]}";

        final com.landawn.abacus.util.ListMultimap<String, Integer> lm = t.valueOf(json);

        org.junit.jupiter.api.Assertions.assertEquals(java.util.Arrays.asList("z", "a", "m", "b", "q"), new java.util.ArrayList<>(lm.keySet()));
        org.junit.jupiter.api.Assertions.assertEquals(json, t.stringOf(lm));
        org.junit.jupiter.api.Assertions.assertEquals(json, t.stringOf(t.valueOf(t.stringOf(lm))));

        // Unicode keys keep their order too
        final String unicode = "{\"é\": [\"中\"], \"à\": [\"x\"], \"😀\": [\"y\"]}";
        final Type<com.landawn.abacus.util.ListMultimap<String, String>> ts = TypeFactory.getType("ListMultimap<String, String>");
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Arrays.asList("é", "à", "😀"), new java.util.ArrayList<>(ts.valueOf(unicode).keySet()));
        org.junit.jupiter.api.Assertions.assertEquals(unicode, ts.stringOf(ts.valueOf(unicode)));
    }

    @Test
    public void reviewFixes20260906_valueOfValueOrderAndEdgeCases() {
        final Type<com.landawn.abacus.util.ListMultimap<String, String>> t = TypeFactory.getType("ListMultimap<String, String>");

        // value order of one key (duplicates kept)
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Arrays.asList("z", "a", "m", "b", "q", "z"),
                t.valueOf("{\"k\": [\"z\", \"a\", \"m\", \"b\", \"q\", \"z\"]}").get("k"));

        // empty document and a single key
        org.junit.jupiter.api.Assertions.assertTrue(t.valueOf("{}").isEmpty());
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Arrays.asList("only"), new java.util.ArrayList<>(t.valueOf("{\"only\": [\"v\"]}").keySet()));

        // duplicate key: position of the first occurrence, values of the last one
        final com.landawn.abacus.util.ListMultimap<String, String> dup = t.valueOf("{\"a\": [\"1\"], \"b\": [\"2\"], \"a\": [\"3\"]}");
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Arrays.asList("a", "b"), new java.util.ArrayList<>(dup.keySet()));
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Arrays.asList("3"), dup.get("a"));

        // T8-05: a null value or an empty array drops the key
        final com.landawn.abacus.util.ListMultimap<String, String> nullValue = t.valueOf("{\"a\": null, \"b\": [\"1\"], \"c\": []}");
        org.junit.jupiter.api.Assertions.assertEquals(java.util.Arrays.asList("b"), new java.util.ArrayList<>(nullValue.keySet()));

        // T8-12: malformed text
        org.junit.jupiter.api.Assertions.assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> t.valueOf("{\"a\": [\"1\"]"));
    }

}
