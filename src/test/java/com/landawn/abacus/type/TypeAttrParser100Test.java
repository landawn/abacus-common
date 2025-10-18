package com.landawn.abacus.type;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("new-test")
public class TypeAttrParser100Test extends TestBase {

    @Test
    public void testParseSimpleClassName() {
        TypeAttrParser parser = TypeAttrParser.parse("String");

        Assertions.assertEquals("String", parser.getClassName());
        Assertions.assertArrayEquals(new String[0], parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[0], parser.getParameters());
    }

    @Test
    public void testParseGenericType() {
        TypeAttrParser parser = TypeAttrParser.parse("List<String>");

        Assertions.assertEquals("List", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "String" }, parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[0], parser.getParameters());
    }

    @Test
    public void testParseMultipleTypeParameters() {
        TypeAttrParser parser = TypeAttrParser.parse("Map<String, Integer>");

        Assertions.assertEquals("Map", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "String", "Integer" }, parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[0], parser.getParameters());
    }

    @Test
    public void testParseNestedGenerics() {
        TypeAttrParser parser = TypeAttrParser.parse("Map<String, List<Integer>>");

        Assertions.assertEquals("Map", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "String", "List<Integer>" }, parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[0], parser.getParameters());
    }

    @Test
    public void testParseConstructorParameters() {
        TypeAttrParser parser = TypeAttrParser.parse("StringBuilder(100)");

        Assertions.assertEquals("StringBuilder", parser.getClassName());
        Assertions.assertArrayEquals(new String[0], parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[] { "100" }, parser.getParameters());
    }

    @Test
    public void testParseMultipleConstructorParameters() {
        TypeAttrParser parser = TypeAttrParser.parse("HashMap(16, 0.75f)");

        Assertions.assertEquals("HashMap", parser.getClassName());
        Assertions.assertArrayEquals(new String[0], parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[] { "16", "0.75f" }, parser.getParameters());
    }

    @Test
    public void testParseGenericWithConstructorParameters() {
        TypeAttrParser parser = TypeAttrParser.parse("HashMap<String, Integer>(16, 0.75f)");

        Assertions.assertEquals("HashMap", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "String", "Integer" }, parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[] { "16", "0.75f" }, parser.getParameters());
    }

    @Test
    public void testParseComplexNestedGenerics() {
        TypeAttrParser parser = TypeAttrParser.parse("Map<String, Map<Integer, List<String>>>");

        Assertions.assertEquals("Map", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "String", "Map<Integer, List<String>>" }, parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[0], parser.getParameters());
    }

    @Test
    public void testParseWithSpaces() {
        TypeAttrParser parser = TypeAttrParser.parse("Map< String , Integer >( 16 , 0.75f )");

        Assertions.assertEquals("Map", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "String", "Integer" }, parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[] { "16", "0.75f" }, parser.getParameters());
    }

    @Test
    public void testParseEmptyConstructor() {
        TypeAttrParser parser = TypeAttrParser.parse("ArrayList()");

        Assertions.assertEquals("ArrayList", parser.getClassName());
        Assertions.assertArrayEquals(new String[0], parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[0], parser.getParameters());
    }

    @Test
    public void testParseCommaParameter() {
        TypeAttrParser parser = TypeAttrParser.parse("StringJoiner(,)");

        Assertions.assertEquals("StringJoiner", parser.getClassName());
        Assertions.assertArrayEquals(new String[0], parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[] { "," }, parser.getParameters());
    }

    @Test
    public void testParseQuotedParameters() {
        TypeAttrParser parser = TypeAttrParser.parse("StringBuilder(\"Hello, World\")");

        Assertions.assertEquals("StringBuilder", parser.getClassName());
        Assertions.assertArrayEquals(new String[0], parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[] { "Hello, World" }, parser.getParameters());
    }

    @Test
    public void testToString() {
        TypeAttrParser parser = TypeAttrParser.parse("HashMap<String, Integer>(16, 0.75f)");
        String str = parser.toString();

        Assertions.assertTrue(str.contains("className=HashMap"));
        Assertions.assertTrue(str.contains("typeParameters=[String, Integer]"));
        Assertions.assertTrue(str.contains("parameters=[16, 0.75f]"));
    }

    @Test
    public void testComplexRealWorldExample() {
        TypeAttrParser parser = TypeAttrParser.parse("ConcurrentHashMap<String, List<Map<Integer, String>>>(32, 0.85f, 16)");

        Assertions.assertEquals("ConcurrentHashMap", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "String", "List<Map<Integer, String>>" }, parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[] { "32", "0.85f", "16" }, parser.getParameters());
    }

    @Test
    public void testEdgeCaseMultipleNestedBrackets() {
        TypeAttrParser parser = TypeAttrParser.parse("Map<Map<String, Integer>, Map<Long, Double>>");

        Assertions.assertEquals("Map", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "Map<String, Integer>", "Map<Long, Double>" }, parser.getTypeParameters());
    }

    @Test
    public void testParseWithArrayType() {
        TypeAttrParser parser = TypeAttrParser.parse("List<String[]>");

        Assertions.assertEquals("List", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "String[]" }, parser.getTypeParameters());
    }

    @Test
    public void testParseWildcardTypes() {
        TypeAttrParser parser = TypeAttrParser.parse("List<? extends Number>");

        Assertions.assertEquals("List", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "? extends Number" }, parser.getTypeParameters());
    }

    @Test
    public void testGettersReturnArrayCopies() {
        TypeAttrParser parser = TypeAttrParser.parse("Map<String, Integer>(16)");

        String[] typeParams1 = parser.getTypeParameters();
        String[] typeParams2 = parser.getTypeParameters();
        String[] params1 = parser.getParameters();
        String[] params2 = parser.getParameters();

        Assertions.assertArrayEquals(typeParams1, typeParams2);
        Assertions.assertArrayEquals(params1, params2);

        if (typeParams1.length > 0) {
            typeParams1[0] = "Modified";
            Assertions.assertEquals("Modified", parser.getTypeParameters()[0]);
        }
    }
}
