package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

public class TypeAttrParserTest extends TestBase {

    static class SingleArrayArg {
        final String[] values;

        SingleArrayArg(final String[] values) {
            this.values = values;
        }
    }

    static class PrefixedSingleArrayArg {
        final int prefix;
        final String[] values;

        PrefixedSingleArrayArg(final Integer prefix, final String[] values) {
            this.prefix = prefix;
            this.values = values;
        }
    }

    @Test
    public void testNewInstanceRejectsNullVarargsClearly() {
        final NullPointerException exception = Assertions.assertThrows(NullPointerException.class,
                () -> TypeAttrParser.newInstance(SingleArrayArg.class, "SingleArrayArg(alpha)", (Object[]) null));

        Assertions.assertEquals("args", exception.getMessage());
    }

    @Test
    public void testGetClassName() {
        TypeAttrParser parser = TypeAttrParser.parse("HashMap<String, Integer>(16)");
        Assertions.assertEquals("HashMap", parser.getClassName());

        TypeAttrParser simple = TypeAttrParser.parse("String");
        Assertions.assertEquals("String", simple.getClassName());

        TypeAttrParser withParams = TypeAttrParser.parse("ArrayList(10)");
        Assertions.assertEquals("ArrayList", withParams.getClassName());
    }

    @Test
    public void testGetTypeParameters() {
        TypeAttrParser parser = TypeAttrParser.parse("Map<String, List<Integer>>");
        String[] typeParams = parser.getTypeParameters();
        Assertions.assertEquals(2, typeParams.length);
        Assertions.assertEquals("String", typeParams[0]);
        Assertions.assertEquals("List<Integer>", typeParams[1]);

        TypeAttrParser noParams = TypeAttrParser.parse("String");
        Assertions.assertEquals(0, noParams.getTypeParameters().length);
    }

    @Test
    public void testGetParameters() {
        TypeAttrParser parser = TypeAttrParser.parse("HashMap(16, 0.75f)");
        String[] params = parser.getParameters();
        Assertions.assertEquals(2, params.length);
        Assertions.assertEquals("16", params[0]);
        Assertions.assertEquals("0.75f", params[1]);

        TypeAttrParser noParams = TypeAttrParser.parse("String");
        Assertions.assertEquals(0, noParams.getParameters().length);

        TypeAttrParser emptyParens = TypeAttrParser.parse("ArrayList()");
        Assertions.assertEquals(0, emptyParens.getParameters().length);
    }

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
    public void testParseConstructorParameters() {
        TypeAttrParser parser = TypeAttrParser.parse("StringBuilder(100)");

        Assertions.assertEquals("StringBuilder", parser.getClassName());
        Assertions.assertArrayEquals(new String[0], parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[] { "100" }, parser.getParameters());
    }

    @Test
    public void testParseGenericWithConstructorParameters() {
        TypeAttrParser parser = TypeAttrParser.parse("HashMap<String, Integer>(16, 0.75f)");

        Assertions.assertEquals("HashMap", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "String", "Integer" }, parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[] { "16", "0.75f" }, parser.getParameters());
    }

    @Test
    public void testParseWithSpaces() {
        TypeAttrParser parser = TypeAttrParser.parse("Map< String , Integer >( 16 , 0.75f )");

        Assertions.assertEquals("Map", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "String", "Integer" }, parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[] { "16", "0.75f" }, parser.getParameters());
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
    public void testComplexRealWorldExample() {
        TypeAttrParser parser = TypeAttrParser.parse("ConcurrentHashMap<String, List<Map<Integer, String>>>(32, 0.85f, 16)");

        Assertions.assertEquals("ConcurrentHashMap", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "String", "List<Map<Integer, String>>" }, parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[] { "32", "0.85f", "16" }, parser.getParameters());
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
            Assertions.assertEquals("String", parser.getTypeParameters()[0]);
        }

        if (params1.length > 0) {
            params1[0] = "Modified";
            Assertions.assertEquals("16", parser.getParameters()[0]);
        }
    }

    @Test
    public void testParseTrimsClassNameWithConstructorParameters() {
        TypeAttrParser parser = TypeAttrParser.parse("  java.util.ArrayList  (16)");

        Assertions.assertEquals("java.util.ArrayList", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "16" }, parser.getParameters());
    }

    @Test
    public void testParseTrimsBareClassName() {
        // regression: a bare type name (no generics, no constructor parens) fell through to the
        // untrimmed fallback assignment, unlike the generics/parens paths which both trim -
        // a stray space then silently broke Class.forName resolution downstream.
        Assertions.assertEquals("java.lang.String", TypeAttrParser.parse("  java.lang.String  ").getClassName());
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
    public void testParseRejectsUnclosedGenericSection() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Map<String"));
    }

    @Test
    public void testParseRejectsExtraClosingGenericBracket() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Map<String>>"));
    }

    @Test
    public void testParseRejectsUnmatchedOrOutOfOrderDelimiters() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Map>"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Map>ignored<String>"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("StringBuilder("));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("StringBuilder)"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("StringBuilder)ignored(value)"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Map(16)<String>"));
    }

    @Test
    public void testParseRejectsUnbalancedNestedConstructorParentheses() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Factory((value)"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Factory(value))"));

        TypeAttrParser balanced = TypeAttrParser.parse("Factory((value))");
        Assertions.assertArrayEquals(new String[] { "(value)" }, balanced.getParameters());

        TypeAttrParser quoted = TypeAttrParser.parse("Factory(\"value)\")");
        Assertions.assertArrayEquals(new String[] { "value)" }, quoted.getParameters());
    }

    @Test
    public void testParseRejectsMissingNamesAndEmptyGenericParameters() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse(""));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("<String>"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("List<>"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Map<String,>"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Map<,String>"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("List<Map<String,>>"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("List<Map<,String>>"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Map<String, List<>>"));
    }

    @Test
    public void testQuotedDelimiterScanningMatchesCsvEscapes() {
        final String backslashEscaped = "Factory(\"alpha\\\")>beta\")";
        TypeAttrParser parsed = TypeAttrParser.parse(backslashEscaped);
        Assertions.assertArrayEquals(new String[] { "alpha\")>beta" }, parsed.getParameters());

        final String doubledQuote = "Factory(\"alpha\"\")>beta\")";
        parsed = TypeAttrParser.parse(doubledQuote);
        Assertions.assertArrayEquals(new String[] { "alpha\")>beta" }, parsed.getParameters());

        final String nested = "Factory(\"alpha\\\")>beta\")";
        parsed = TypeAttrParser.parse("Holder<" + nested + ", String>");
        Assertions.assertArrayEquals(new String[] { nested, "String" }, parsed.getTypeParameters());
    }

    @Test
    public void testSingleQuotesDoNotShieldTypeDelimiters() {
        final TypeAttrParser apostrophe = TypeAttrParser.parse("Factory(O'Reilly)");
        Assertions.assertArrayEquals(new String[] { "O'Reilly" }, apostrophe.getParameters());

        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Factory('alpha)beta')"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Holder<Factory('alpha)>beta'), String>"));
    }

    @Test
    public void testNestedConstructorArgumentsDoNotSplitOuterGenericParameters() {
        TypeAttrParser parsed = TypeAttrParser.parse("Map<Factory(alpha,beta), List<String>>");
        Assertions.assertArrayEquals(new String[] { "Factory(alpha,beta)", "List<String>" }, parsed.getTypeParameters());

        parsed = TypeAttrParser.parse("Holder<Factory(\"alpha,beta\")>");
        Assertions.assertArrayEquals(new String[] { "Factory(\"alpha,beta\")" }, parsed.getTypeParameters());

        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Holder<Factory(alpha,beta>"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Holder<Factory(\"alpha,beta)>"));
    }

    @Test
    public void testAngleBracketsInConstructorArgumentsAreNotGenericDelimiters() {
        TypeAttrParser parsed = TypeAttrParser.parse("Holder<String>(\"alpha>beta\")");
        Assertions.assertArrayEquals(new String[] { "String" }, parsed.getTypeParameters());
        Assertions.assertArrayEquals(new String[] { "alpha>beta" }, parsed.getParameters());

        parsed = TypeAttrParser.parse("Holder<Factory(alpha>beta), String>");
        Assertions.assertArrayEquals(new String[] { "Factory(alpha>beta)", "String" }, parsed.getTypeParameters());

        parsed = TypeAttrParser.parse("String(\"alpha>beta\")");
        Assertions.assertArrayEquals(new String[0], parsed.getTypeParameters());
        Assertions.assertArrayEquals(new String[] { "alpha>beta" }, parsed.getParameters());

        parsed = TypeAttrParser.parse("String(\"alpha<beta>\")");
        Assertions.assertArrayEquals(new String[0], parsed.getTypeParameters());
        Assertions.assertArrayEquals(new String[] { "alpha<beta>" }, parsed.getParameters());
    }

    @Test
    public void testParseRejectsTrailingText() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Map<String>junk"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Map<String>(16)junk"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("String(1)junk"));
    }

    @Test
    public void testParseMultipleConstructorParameters() {
        TypeAttrParser parser = TypeAttrParser.parse("HashMap(16, 0.75f)");

        Assertions.assertEquals("HashMap", parser.getClassName());
        Assertions.assertArrayEquals(new String[0], parser.getTypeParameters());
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
    public void testParseEmptyConstructor() {
        TypeAttrParser parser = TypeAttrParser.parse("ArrayList()");

        Assertions.assertEquals("ArrayList", parser.getClassName());
        Assertions.assertArrayEquals(new String[0], parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[0], parser.getParameters());
    }

    @Test
    public void testEdgeCaseMultipleNestedBrackets() {
        TypeAttrParser parser = TypeAttrParser.parse("Map<Map<String, Integer>, Map<Long, Double>>");

        Assertions.assertEquals("Map", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "Map<String, Integer>", "Map<Long, Double>" }, parser.getTypeParameters());
    }

    @Test
    public void testNewInstanceSupportsSingleStringArrayConstructor() {
        SingleArrayArg result = TypeAttrParser.newInstance(SingleArrayArg.class, "SingleArrayArg(alpha)");

        Assertions.assertArrayEquals(new String[] { "alpha" }, result.values);
    }

    @Test
    public void testNewInstanceWithArgsSupportsSingleStringArrayConstructor() {
        PrefixedSingleArrayArg result = TypeAttrParser.newInstance(PrefixedSingleArrayArg.class, "PrefixedSingleArrayArg(beta)", Integer.class, 7);

        Assertions.assertEquals(7, result.prefix);
        Assertions.assertArrayEquals(new String[] { "beta" }, result.values);
    }

    @Test
    public void testToString() {
        TypeAttrParser parser = TypeAttrParser.parse("HashMap<String, Integer>(16, 0.75f)");
        String str = parser.toString();

        Assertions.assertTrue(str.contains("className=HashMap"));
        Assertions.assertTrue(str.contains("typeParameters=[String, Integer]"));
        Assertions.assertTrue(str.contains("parameters=[16, 0.75f]"));
    }
}
