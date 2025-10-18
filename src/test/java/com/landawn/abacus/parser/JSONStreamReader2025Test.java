package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.StringReader;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class JSONStreamReader2025Test extends TestBase {

    @Test
    public void test_parse() {
        String json = "{\"name\":\"John\"}";
        StringReader stringReader = new StringReader(json);
        JSONReader reader = JSONStreamReader.parse(stringReader, new char[1024], new char[256]);
        assertNotNull(reader);
        reader.close();
    }

    @Test
    public void test_nextToken() {
        String json = "{\"key\":\"value\"}";
        StringReader stringReader = new StringReader(json);
        JSONReader reader = JSONStreamReader.parse(stringReader, new char[1024], new char[256]);

        reader.nextToken();

        reader.close();
    }

    @Test
    public void test_hasText() {
        String json = "\"test\"";
        StringReader stringReader = new StringReader(json);
        JSONReader reader = JSONStreamReader.parse(stringReader, new char[1024], new char[256]);

        reader.nextToken();
        reader.nextToken();
        reader.hasText();

        reader.close();
    }

    @Test
    public void test_getText() {
        String json = "\"hello\"";
        StringReader stringReader = new StringReader(json);
        JSONReader reader = JSONStreamReader.parse(stringReader, new char[1024], new char[256]);

        reader.nextToken();
        reader.nextToken();
        reader.getText();

        reader.close();
    }
}
