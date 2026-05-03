package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.entity.extendDirty.basic.Account;
import com.landawn.abacus.type.TypeFactory;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.stream.Stream;

public abstract class AbstractJsonParserTest extends AbstractParserTest {

    @Test
    public void test_array() {
        Account[] accounts = N.asArray(createAccount(Account.class));

        String str = parser.serialize(accounts);

        N.println(str);

        Account[] accounts2 = parser.deserialize(str, Account[].class);

        assertTrue(N.equals(accounts, accounts2));

        String[][] twoArray = { { "abc", "efg" }, { "123", "345" } };

        str = parser.serialize(twoArray);

        String[][] twoArray2 = parser.deserialize(str, String[][].class);

        assertTrue(N.deepEquals(twoArray, twoArray2));

        twoArray = new String[][] { null, { "123", "345" } };

        str = parser.serialize(twoArray);

        twoArray2 = parser.deserialize(str, String[][].class);

        assertTrue(N.deepEquals(twoArray, twoArray2));

        if (parser instanceof JsonParserImpl) {
            str = "[, [\"123\", \"345\"]]";

            twoArray2 = parser.deserialize(str, String[][].class);

            assertTrue(N.deepEquals(new String[][] { null, { "123", "345" } }, twoArray2));
        }
    }

    @Test
    public void testParse() {
        JsonParser json = (JsonParser) parser;

        assertEquals(Integer.valueOf(1), json.parse("1", TypeFactory.getType(Integer.class)));
        assertEquals(Integer.valueOf(2), json.parse("2", Integer.class));
        assertEquals(Integer.valueOf(3), json.parse("3", JsonDeserConfig.create(), Integer.class));
    }

    @Test
    public void testParseArrayOutput() {
        JsonParser json = (JsonParser) parser;
        Object[] output = new Object[3];

        json.parse("[\"a\",\"b\",\"c\"]", output);

        assertEquals("a", output[0]);
        assertEquals("b", output[1]);
        assertEquals("c", output[2]);
    }

    @Test
    public void testParseArrayOutput_WithConfig() {
        JsonParser json = (JsonParser) parser;
        Object[] output = new Object[2];

        json.parse("[\"x\",\"y\"]", JsonDeserConfig.create(), output);

        assertEquals("x", output[0]);
        assertEquals("y", output[1]);
    }

    @Test
    public void testParseCollectionOutput() {
        JsonParser json = (JsonParser) parser;
        List<Object> output = new ArrayList<>();

        json.parse("[\"a\",\"b\"]", output);

        assertEquals(2, output.size());
        assertEquals("a", output.get(0));
        assertEquals("b", output.get(1));
    }

    @Test
    public void testParseCollectionOutput_WithConfig() {
        JsonParser json = (JsonParser) parser;
        List<Object> output = new ArrayList<>();

        json.parse("[\"x\",\"y\"]", JsonDeserConfig.create(), output);

        assertEquals(2, output.size());
        assertEquals("x", output.get(0));
        assertEquals("y", output.get(1));
    }

    @Test
    public void testParseMapOutput() {
        JsonParser json = (JsonParser) parser;
        Map<String, Object> output = new HashMap<>();

        json.parse("{\"name\":\"alpha\"}", output);

        assertEquals("alpha", output.get("name"));
    }

    @Test
    public void testParseMapOutput_WithConfig() {
        JsonParser json = (JsonParser) parser;
        Map<String, Object> output = new HashMap<>();

        json.parse("{\"name\":\"beta\"}", JsonDeserConfig.create(), output);

        assertEquals("beta", output.get("name"));
    }

    @Test
    public void testStream() {
        JsonParser json = (JsonParser) parser;

        try (Stream<Map> stream = json.stream("[{\"name\":\"a\"},{\"name\":\"b\"}]", TypeFactory.getType(Map.class))) {
            List<Map> rows = stream.toList();

            assertEquals(2, rows.size());
            assertEquals("a", rows.get(0).get("name"));
            assertEquals("b", rows.get(1).get("name"));
        }
    }

    @Test
    public void testStream_File() throws Exception {
        JsonParser json = (JsonParser) parser;
        File file = File.createTempFile("abacus-json-stream", ".json");

        try {
            try (FileWriter writer = new FileWriter(file)) {
                writer.write("[{\"name\":\"a\"},{\"name\":\"b\"}]");
            }

            try (Stream<Map> stream = json.stream(file, TypeFactory.getType(Map.class))) {
                List<Map> rows = stream.toList();

                assertEquals(2, rows.size());
                assertEquals("a", rows.get(0).get("name"));
                assertEquals("b", rows.get(1).get("name"));
            }
        } finally {
            assertTrue(file.delete() || !file.exists());
        }
    }

    @Test
    public void testStream_InputStream() {
        JsonParser json = (JsonParser) parser;
        ByteArrayInputStream input = new ByteArrayInputStream("[{\"name\":\"a\"},{\"name\":\"b\"}]".getBytes(StandardCharsets.UTF_8));

        try (Stream<Map> stream = json.stream(input, true, TypeFactory.getType(Map.class))) {
            List<Map> rows = stream.toList();

            assertEquals(2, rows.size());
            assertEquals("a", rows.get(0).get("name"));
            assertEquals("b", rows.get(1).get("name"));
        }
    }

    @Test
    public void testStream_Reader() {
        JsonParser json = (JsonParser) parser;

        try (Stream<Map> stream = json.stream(new StringReader("[{\"name\":\"a\"},{\"name\":\"b\"}]"), true, TypeFactory.getType(Map.class))) {
            List<Map> rows = stream.toList();

            assertEquals(2, rows.size());
            assertEquals("a", rows.get(0).get("name"));
            assertEquals("b", rows.get(1).get("name"));
        }
    }
}
