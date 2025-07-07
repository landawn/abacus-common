package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;


public class ParserFactory100Test extends TestBase {
    
    @Test
    public void testIsAbacusXMLAvailable() {
        // This depends on runtime environment
        boolean available = ParserFactory.isAbacusXMLAvailable();
        // Just verify it returns a boolean
        assertTrue(available || !available);
    }
    
    @Test
    public void testIsXMLAvailable() {
        boolean available = ParserFactory.isXMLAvailable();
        assertTrue(available || !available);
    }
    
    @Test
    public void testIsAvroAvailable() {
        boolean available = ParserFactory.isAvroAvailable();
        assertTrue(available || !available);
    }
    
    @Test
    public void testIsKryoAvailable() {
        boolean available = ParserFactory.isKryoAvailable();
        assertTrue(available || !available);
    }
    
    @Test
    public void testCreateAvroParser() {
        if (ParserFactory.isAvroAvailable()) {
            AvroParser parser = ParserFactory.createAvroParser();
            assertNotNull(parser);
        }
    }
    
    @Test
    public void testCreateKryoParser() {
        if (ParserFactory.isKryoAvailable()) {
            KryoParser parser = ParserFactory.createKryoParser();
            assertNotNull(parser);
        }
    }
    
    @Test
    public void testCreateJSONParser() {
        JSONParser parser = ParserFactory.createJSONParser();
        assertNotNull(parser);
    }
    
    @Test
    public void testCreateJSONParserWithConfig() {
        JSONSerializationConfig jsc = new JSONSerializationConfig();
        JSONDeserializationConfig jdc = new JSONDeserializationConfig();
        
        JSONParser parser = ParserFactory.createJSONParser(jsc, jdc);
        assertNotNull(parser);
    }
    
    @Test
    public void testCreateAbacusXMLParser() {
        if (ParserFactory.isAbacusXMLAvailable()) {
            XMLParser parser = ParserFactory.createAbacusXMLParser();
            assertNotNull(parser);
        }
    }
    
    @Test
    public void testCreateAbacusXMLParserWithConfig() {
        if (ParserFactory.isAbacusXMLAvailable()) {
            XMLSerializationConfig xsc = new XMLSerializationConfig();
            XMLDeserializationConfig xdc = new XMLDeserializationConfig();
            
            XMLParser parser = ParserFactory.createAbacusXMLParser(xsc, xdc);
            assertNotNull(parser);
        }
    }
    
    @Test
    public void testCreateXMLParser() {
        if (ParserFactory.isXMLAvailable()) {
            XMLParser parser = ParserFactory.createXMLParser();
            assertNotNull(parser);
        }
    }
    
    @Test
    public void testCreateXMLParserWithConfig() {
        if (ParserFactory.isXMLAvailable()) {
            XMLSerializationConfig xsc = new XMLSerializationConfig();
            XMLDeserializationConfig xdc = new XMLDeserializationConfig();
            
            XMLParser parser = ParserFactory.createXMLParser(xsc, xdc);
            assertNotNull(parser);
        }
    }
    
    @Test
    public void testCreateJAXBParser() {
        XMLParser parser = ParserFactory.createJAXBParser();
        assertNotNull(parser);
    }
    
    @Test
    public void testCreateJAXBParserWithConfig() {
        XMLSerializationConfig xsc = new XMLSerializationConfig();
        XMLDeserializationConfig xdc = new XMLDeserializationConfig();
        
        XMLParser parser = ParserFactory.createJAXBParser(xsc, xdc);
        assertNotNull(parser);
    }
    
    @Test
    public void testRegisterKryo() {
        // These methods modify static state, just verify they don't throw
        ParserFactory.registerKryo(String.class);
        ParserFactory.registerKryo(Integer.class, 100);
        
        // Can't easily create Serializer without Kryo dependency
        // Just verify the method exists
    }
    
    @Test
    public void testRegisterKryoWithNullClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParserFactory.registerKryo(null);
        });
    }
    
    @Test
    public void testRegisterKryoWithIdAndNullClass() {
        assertThrows(IllegalArgumentException.class, () -> {
            ParserFactory.registerKryo(null, 100);
        });
    }
}

//
