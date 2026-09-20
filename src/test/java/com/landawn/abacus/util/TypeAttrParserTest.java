package com.landawn.abacus.util;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.TypeFactory;

public class TypeAttrParserTest extends TestBase {

    public static class SingleArrayArg {
        final String[] values;

        SingleArrayArg(final String[] values) {
            this.values = values;
        }
    }

    public static class PrefixedSingleArrayArg {
        final int prefix;
        final String[] values;

        PrefixedSingleArrayArg(final Integer prefix, final String[] values) {
            this.prefix = prefix;
            this.values = values;
        }
    }

    @Test
    public void testNewInstanceRejectsNullVarargsClearly() {
        final IllegalArgumentException exception = Assertions.assertThrows(IllegalArgumentException.class,
                () -> TypeAttrParser.newInstance(SingleArrayArg.class, "SingleArrayArg(alpha)", (Object[]) null));

        Assertions.assertEquals("'args' cannot be null", exception.getMessage());
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
    public void testParameterizedOwnerMemberType() {
        TypeAttrParser parser = TypeAttrParser.parse("com.example.Owner<Integer>.Member<String>");
        Assertions.assertEquals("com.example.Owner.Member", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "String" }, parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[0], parser.getParameters());

        parser = TypeAttrParser.parse("Outer<Long>.Middle<Integer>.Inner<String>(value)");
        Assertions.assertEquals("Outer.Middle.Inner", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "String" }, parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[] { "value" }, parser.getParameters());
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
    public void testParseCommaParameterIsTheOnlySpecialCasedAllEmptyArgumentList() {
        // documents the exception to "the argument list is parsed as CSV": an argument list that is a lone comma
        // once trimmed is the delimiter itself, not two empty fields, so a comma delimiter needs no quoting.
        // Nothing else is special-cased.
        Assertions.assertArrayEquals(new String[] { "," }, TypeAttrParser.parse("StringJoiner(,)").getParameters());
        Assertions.assertArrayEquals(new String[] { "," }, TypeAttrParser.parse("StringJoiner( , )").getParameters());
        Assertions.assertArrayEquals(new String[] { "," }, TypeAttrParser.parse("StringJoiner(\",\")").getParameters());

        Assertions.assertArrayEquals(new String[] { "", "" }, new CsvParser(',', '"', '\\').parseLineToArray(","));
        Assertions.assertArrayEquals(new String[] { "", "", "" }, TypeAttrParser.parse("StringJoiner(,,)").getParameters());
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
    public void testParseParameterizedArrayTypes() {
        TypeAttrParser parser = TypeAttrParser.parse("List<String>[]");
        Assertions.assertEquals("List[]", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "String" }, parser.getTypeParameters());
        Assertions.assertArrayEquals(new String[0], parser.getParameters());

        parser = TypeAttrParser.parse("Map<String, List<Integer>[][]>");
        Assertions.assertEquals("Map", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "String", "List<Integer>[][]" }, parser.getTypeParameters());

        parser = TypeAttrParser.parse("  Owner<Long>.Member<String>[][]  ");
        Assertions.assertEquals("Owner.Member[][]", parser.getClassName());
        Assertions.assertArrayEquals(new String[] { "String" }, parser.getTypeParameters());
        Assertions.assertEquals("String[][]", TypeAttrParser.parse("String[][]").getClassName());

        for (final String malformed : new String[] { "List<String>[]junk", "List<String>[3]", "List<String>[", "List<String>[]>", "List<>[]",
                "List<Map<String,>[]>", "[]", "Factory(value)[]" }) {
            Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse(malformed), malformed);
        }
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
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Owner<Integer>.Member<String>junk"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Owner<Integer>.Member<String>."));
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

    @Test
    public void testParseReportsMalformedQuotedArgumentAsIllegalArgument() {
        // The delimiter scanner opens a quoted region on ANY '"', while the CSV dialect treats a '"' inside an
        // unquoted field as data, so a substring the scanner accepted can still be malformed CSV. That used to
        // escape as com.landawn.abacus.exception.ParsingException, which parse() does not document.
        for (final String malformed : new String[] { "Foo(a\",\")", "(a\",\")", "Foo(x,a\",\")", "Foo<String>(a\",\")", "Map<String, Foo(a\",\")>" }) {
            final IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse(malformed), malformed);
            Assertions.assertTrue(e.getMessage().startsWith("Malformed type attribute: malformed quoted constructor argument in: "), e.getMessage());
        }

        // the same escape reached the IAE-documented public entry points
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType("List(a\",\")"));

        // the accept/reject split itself is deliberately unchanged: an even number of embedded quotes parses,
        // an odd number does not.
        Assertions.assertArrayEquals(new String[] { "bc\"d\"ef" }, TypeAttrParser.parse("Foo(bc\"d\"ef)").getParameters());
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Foo(a\"b)"));
    }

    @Test
    public void testParseRejectsArrayBracketsWithoutComponentType() {
        // "[]" was rejected only when the brackets ended the string; once generics or constructor parentheses
        // followed, the brackets were absorbed into the class name and parse() returned className "[]".
        for (final String malformed : new String[] { "[]", "[][]", "[]()", "[] ()", "[](1)", "[]<A>", "[]<A>(1)", " []()", "[][]()", "[]<A>[]" }) {
            final IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse(malformed), malformed);
            Assertions.assertTrue(e.getMessage().startsWith("Malformed type attribute: missing class name in: "), malformed + " -> " + e.getMessage());
        }

        Assertions.assertEquals("Foo[]", TypeAttrParser.parse("Foo[]").getClassName());
        Assertions.assertEquals("Foo[]", TypeAttrParser.parse("Foo[]()").getClassName());
        Assertions.assertEquals("Foo[]", TypeAttrParser.parse("Foo[]<A>").getClassName());
        Assertions.assertEquals("String[][]", TypeAttrParser.parse("String[][]").getClassName());
        Assertions.assertEquals("List[]", TypeAttrParser.parse("List<String>[]").getClassName());
    }

    @Test
    public void testParseRejectsPathologicallyDeepNestingInsteadOfOverflowingTheStack() {
        // the cap counts parse() re-entries, so a pure generic ladder costs one level each: 64 passes, 65 fails
        Assertions.assertEquals("A", TypeAttrParser.parse("A<".repeat(64) + "B" + ">".repeat(64)).getClassName());
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("A<".repeat(65) + "B" + ">".repeat(65)));

        final String tooDeep = "A<".repeat(200) + "B" + ">".repeat(200);
        final IllegalArgumentException e = Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse(tooDeep));
        // the message must name the declaration the caller passed: the fragment the recursion had reached when the
        // guard tripped is a leaf ("B" here, "String" for the Map ladder below) and is plainly not 64 levels deep
        Assertions.assertEquals("Malformed type attribute: nesting deeper than 64 levels in: " + tooDeep, e.getMessage());

        final String tooDeepMap = "Map<String, ".repeat(65) + "Integer" + ">".repeat(65);
        final IllegalArgumentException mapFailure = Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse(tooDeepMap));
        Assertions.assertEquals("Malformed type attribute: nesting deeper than 64 levels in: " + tooDeepMap, mapFailure.getMessage());

        // an array-of-generic suffix and a parameterized qualified-member segment each cost a second level, so
        // those two shapes reach the same cap at half the textual depth: 32 passes, 33 fails
        String arrayOfGeneric = "B";
        String memberChain = "M";

        for (int i = 0; i < 32; i++) {
            arrayOfGeneric = "A<" + arrayOfGeneric + ">[]";
            memberChain = "O<" + memberChain + ">.M";
        }

        Assertions.assertEquals("A[]", TypeAttrParser.parse(arrayOfGeneric).getClassName());
        Assertions.assertEquals("O.M", TypeAttrParser.parse(memberChain).getClassName());

        final String arrayOfGeneric33 = "A<" + arrayOfGeneric + ">[]";
        final String memberChain33 = "O<" + memberChain + ">.M";

        Assertions.assertEquals("Malformed type attribute: nesting deeper than 64 levels in: " + arrayOfGeneric33,
                Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse(arrayOfGeneric33)).getMessage());
        Assertions.assertEquals("Malformed type attribute: nesting deeper than 64 levels in: " + memberChain33,
                Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse(memberChain33)).getMessage());

        // array dimensions never recursed per dimension and must keep working at any count
        Assertions.assertEquals("int" + "[]".repeat(500), TypeAttrParser.parse("int" + "[]".repeat(500)).getClassName());
    }

    @Test
    public void testWhitespaceSeparatedArrayDimensionsDoNotConsumeNestingDepth() {
        final String dimensions = "[]".repeat(255);
        Assertions.assertEquals("int" + dimensions, TypeAttrParser.parse("int" + "[] ".repeat(255)).getClassName());
        Assertions.assertEquals("int" + dimensions, TypeAttrParser.parse("int" + "[]\u3000".repeat(255)).getClassName());

        final TypeAttrParser generic = TypeAttrParser.parse("Owner<Long>.Member<String>" + "[] \t".repeat(255));
        Assertions.assertEquals("Owner.Member" + dimensions, generic.getClassName());
        Assertions.assertArrayEquals(new String[] { "String" }, generic.getTypeParameters());

        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("Factory(value)" + "[] ".repeat(255)));
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("[] ".repeat(255)));
    }

    @Test
    public void reviewFixes20260911_arraySuffixScanStripsUnicodeWhitespace() {
        // The array-suffix scan uses Character.isWhitespace, so a trailing U+3000 (whitespace, but > 0x20 and
        // therefore NOT removed by String#trim) is padding and the array token still resolves. If the scan used
        // the String#trim predicate instead, the character would survive into the class name and
        // TypeFactory.getType would reject the token as misplaced brackets.
        Assertions.assertEquals("int[]", TypeAttrParser.parse("int[]\u3000").getClassName());
        Assertions.assertEquals("int[][]", TypeAttrParser.parse("int[]\u3000[]").getClassName());
        Assertions.assertEquals("int[]", TypeAttrParser.parse("int[] \u3000").getClassName());
        Assertions.assertEquals("List[]", TypeAttrParser.parse("List<String>[]\u3000").getClassName());
        Assertions.assertEquals("int[]", TypeFactory.getType("int[]\u3000").name());
        Assertions.assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse("List<String>\u3000"));

        // a bare (non-array) name still keeps it, because the class-name trims use String#trim semantics
        Assertions.assertEquals("Foo\u3000", TypeAttrParser.parse("Foo\u3000").getClassName());

        // ASCII padding is still stripped on every path
        Assertions.assertEquals("int[]", TypeAttrParser.parse("  int[]  ").getClassName());
        Assertions.assertEquals("Foo", TypeAttrParser.parse("  Foo  ").getClassName());
        Assertions.assertEquals("List[]", TypeAttrParser.parse("  List<String>[]  ").getClassName());
    }

    @Test
    public void testNewInstanceFailureNamesBothCandidateSignatures() {
        // the String[] fallback used to overwrite the primary signature, so the message named only the fallback
        final IllegalArgumentException primaryAndFallback = Assertions.assertThrows(IllegalArgumentException.class,
                () -> TypeAttrParser.newInstance(StringBuilder.class, "StringBuilder(a, b, c, d, e)"));
        Assertions.assertEquals(
                "No constructor found with parameters: [class java.lang.String, class java.lang.String, class java.lang.String,"
                        + " class java.lang.String, class java.lang.String] or [class [Ljava.lang.String;]. in class: java.lang.StringBuilder",
                primaryAndFallback.getMessage());

        final IllegalArgumentException withPairs = Assertions.assertThrows(IllegalArgumentException.class,
                () -> TypeAttrParser.newInstance(StringBuilder.class, "StringBuilder(x)", Integer.class, 1));
        Assertions.assertEquals("No constructor found with parameters: [class java.lang.Integer, class java.lang.String]"
                + " or [class java.lang.Integer, class [Ljava.lang.String;]. in class: java.lang.StringBuilder", withPairs.getMessage());

        // no constructor parameters means no fallback was tried, so only one signature is reported
        final IllegalArgumentException noFallback = Assertions.assertThrows(IllegalArgumentException.class,
                () -> TypeAttrParser.newInstance(java.util.ArrayList.class, "ArrayList<X>"));
        Assertions.assertEquals("No constructor found with parameters: [class java.lang.String]. in class: java.util.ArrayList", noFallback.getMessage());
    }
}
