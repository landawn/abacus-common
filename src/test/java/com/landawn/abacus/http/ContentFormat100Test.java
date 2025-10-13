package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class ContentFormat100Test extends TestBase {

    @Test
    public void testContentType() {
        assertEquals("", ContentFormat.NONE.contentType());
        assertEquals("application/json", ContentFormat.JSON.contentType());
        assertEquals("application/json", ContentFormat.JSON_LZ4.contentType());
        assertEquals("application/json", ContentFormat.JSON_SNAPPY.contentType());
        assertEquals("application/json", ContentFormat.JSON_GZIP.contentType());
        assertEquals("application/json", ContentFormat.JSON_BR.contentType());
        assertEquals("application/xml", ContentFormat.XML.contentType());
        assertEquals("application/xml", ContentFormat.XML_LZ4.contentType());
        assertEquals("application/xml", ContentFormat.XML_SNAPPY.contentType());
        assertEquals("application/xml", ContentFormat.XML_GZIP.contentType());
        assertEquals("application/xml", ContentFormat.XML_BR.contentType());
        assertEquals("application/x-www-form-urlencoded", ContentFormat.FormUrlEncoded.contentType());
        assertEquals("", ContentFormat.KRYO.contentType());
        assertEquals("", ContentFormat.LZ4.contentType());
        assertEquals("", ContentFormat.SNAPPY.contentType());
        assertEquals("", ContentFormat.GZIP.contentType());
        assertEquals("", ContentFormat.BR.contentType());
    }

    @Test
    public void testContentEncoding() {
        assertEquals("", ContentFormat.NONE.contentEncoding());
        assertEquals("", ContentFormat.JSON.contentEncoding());
        assertEquals("lz4", ContentFormat.JSON_LZ4.contentEncoding());
        assertEquals("snappy", ContentFormat.JSON_SNAPPY.contentEncoding());
        assertEquals("gzip", ContentFormat.JSON_GZIP.contentEncoding());
        assertEquals("br", ContentFormat.JSON_BR.contentEncoding());
        assertEquals("", ContentFormat.XML.contentEncoding());
        assertEquals("lz4", ContentFormat.XML_LZ4.contentEncoding());
        assertEquals("snappy", ContentFormat.XML_SNAPPY.contentEncoding());
        assertEquals("gzip", ContentFormat.XML_GZIP.contentEncoding());
        assertEquals("br", ContentFormat.XML_BR.contentEncoding());
        assertEquals("", ContentFormat.FormUrlEncoded.contentEncoding());
        assertEquals("kryo", ContentFormat.KRYO.contentEncoding());
        assertEquals("lz4", ContentFormat.LZ4.contentEncoding());
        assertEquals("snappy", ContentFormat.SNAPPY.contentEncoding());
        assertEquals("gzip", ContentFormat.GZIP.contentEncoding());
        assertEquals("br", ContentFormat.BR.contentEncoding());
    }

    @Test
    public void testEnumValues() {
        ContentFormat[] values = ContentFormat.values();
        assertEquals(17, values.length);

        assertEquals(ContentFormat.JSON, ContentFormat.valueOf("JSON"));
        assertEquals(ContentFormat.XML_GZIP, ContentFormat.valueOf("XML_GZIP"));
        assertEquals(ContentFormat.FormUrlEncoded, ContentFormat.valueOf("FormUrlEncoded"));
    }

    @Test
    public void testEnumValueOfThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> ContentFormat.valueOf("INVALID"));
    }
}
