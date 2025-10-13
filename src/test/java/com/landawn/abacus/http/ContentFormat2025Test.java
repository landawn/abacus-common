package com.landawn.abacus.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("2025")
public class ContentFormat2025Test extends TestBase {

    @Test
    public void testContentTypeNONE() {
        assertEquals("", ContentFormat.NONE.contentType());
    }

    @Test
    public void testContentEncodingNONE() {
        assertEquals("", ContentFormat.NONE.contentEncoding());
    }

    @Test
    public void testContentTypeJSON() {
        assertEquals("application/json", ContentFormat.JSON.contentType());
    }

    @Test
    public void testContentEncodingJSON() {
        assertEquals("", ContentFormat.JSON.contentEncoding());
    }

    @Test
    public void testContentTypeJSON_LZ4() {
        assertEquals("application/json", ContentFormat.JSON_LZ4.contentType());
    }

    @Test
    public void testContentEncodingJSON_LZ4() {
        assertEquals("lz4", ContentFormat.JSON_LZ4.contentEncoding());
    }

    @Test
    public void testContentTypeJSON_SNAPPY() {
        assertEquals("application/json", ContentFormat.JSON_SNAPPY.contentType());
    }

    @Test
    public void testContentEncodingJSON_SNAPPY() {
        assertEquals("snappy", ContentFormat.JSON_SNAPPY.contentEncoding());
    }

    @Test
    public void testContentTypeJSON_GZIP() {
        assertEquals("application/json", ContentFormat.JSON_GZIP.contentType());
    }

    @Test
    public void testContentEncodingJSON_GZIP() {
        assertEquals("gzip", ContentFormat.JSON_GZIP.contentEncoding());
    }

    @Test
    public void testContentTypeJSON_BR() {
        assertEquals("application/json", ContentFormat.JSON_BR.contentType());
    }

    @Test
    public void testContentEncodingJSON_BR() {
        assertEquals("br", ContentFormat.JSON_BR.contentEncoding());
    }

    @Test
    public void testContentTypeXML() {
        assertEquals("application/xml", ContentFormat.XML.contentType());
    }

    @Test
    public void testContentEncodingXML() {
        assertEquals("", ContentFormat.XML.contentEncoding());
    }

    @Test
    public void testContentTypeXML_LZ4() {
        assertEquals("application/xml", ContentFormat.XML_LZ4.contentType());
    }

    @Test
    public void testContentEncodingXML_LZ4() {
        assertEquals("lz4", ContentFormat.XML_LZ4.contentEncoding());
    }

    @Test
    public void testContentTypeXML_SNAPPY() {
        assertEquals("application/xml", ContentFormat.XML_SNAPPY.contentType());
    }

    @Test
    public void testContentEncodingXML_SNAPPY() {
        assertEquals("snappy", ContentFormat.XML_SNAPPY.contentEncoding());
    }

    @Test
    public void testContentTypeXML_GZIP() {
        assertEquals("application/xml", ContentFormat.XML_GZIP.contentType());
    }

    @Test
    public void testContentEncodingXML_GZIP() {
        assertEquals("gzip", ContentFormat.XML_GZIP.contentEncoding());
    }

    @Test
    public void testContentTypeXML_BR() {
        assertEquals("application/xml", ContentFormat.XML_BR.contentType());
    }

    @Test
    public void testContentEncodingXML_BR() {
        assertEquals("br", ContentFormat.XML_BR.contentEncoding());
    }

    @Test
    public void testContentTypeFormUrlEncoded() {
        assertEquals("application/x-www-form-urlencoded", ContentFormat.FormUrlEncoded.contentType());
    }

    @Test
    public void testContentEncodingFormUrlEncoded() {
        assertEquals("", ContentFormat.FormUrlEncoded.contentEncoding());
    }

    @Test
    public void testContentTypeKRYO() {
        assertEquals("", ContentFormat.KRYO.contentType());
    }

    @Test
    public void testContentEncodingKRYO() {
        assertEquals("kryo", ContentFormat.KRYO.contentEncoding());
    }

    @Test
    public void testContentTypeLZ4() {
        assertEquals("", ContentFormat.LZ4.contentType());
    }

    @Test
    public void testContentEncodingLZ4() {
        assertEquals("lz4", ContentFormat.LZ4.contentEncoding());
    }

    @Test
    public void testContentTypeSNAPPY() {
        assertEquals("", ContentFormat.SNAPPY.contentType());
    }

    @Test
    public void testContentEncodingSNAPPY() {
        assertEquals("snappy", ContentFormat.SNAPPY.contentEncoding());
    }

    @Test
    public void testContentTypeGZIP() {
        assertEquals("", ContentFormat.GZIP.contentType());
    }

    @Test
    public void testContentEncodingGZIP() {
        assertEquals("gzip", ContentFormat.GZIP.contentEncoding());
    }

    @Test
    public void testContentTypeBR() {
        assertEquals("", ContentFormat.BR.contentType());
    }

    @Test
    public void testContentEncodingBR() {
        assertEquals("br", ContentFormat.BR.contentEncoding());
    }

    @Test
    public void testEnumValues() {
        ContentFormat[] values = ContentFormat.values();
        assertNotNull(values);
        assertEquals(17, values.length);
    }

    @Test
    public void testEnumValueOf() {
        assertEquals(ContentFormat.JSON, ContentFormat.valueOf("JSON"));
        assertEquals(ContentFormat.XML, ContentFormat.valueOf("XML"));
        assertEquals(ContentFormat.JSON_GZIP, ContentFormat.valueOf("JSON_GZIP"));
        assertEquals(ContentFormat.NONE, ContentFormat.valueOf("NONE"));
    }

    @Test
    public void testEnumName() {
        assertEquals("JSON", ContentFormat.JSON.name());
        assertEquals("XML", ContentFormat.XML.name());
        assertEquals("JSON_GZIP", ContentFormat.JSON_GZIP.name());
        assertEquals("NONE", ContentFormat.NONE.name());
    }

    @Test
    public void testEnumToString() {
        assertEquals("JSON", ContentFormat.JSON.toString());
        assertEquals("XML", ContentFormat.XML.toString());
        assertEquals("JSON_GZIP", ContentFormat.JSON_GZIP.toString());
    }

    @Test
    public void testEnumOrdinal() {
        assertEquals(0, ContentFormat.NONE.ordinal());
        assertEquals(1, ContentFormat.JSON.ordinal());
    }
}
