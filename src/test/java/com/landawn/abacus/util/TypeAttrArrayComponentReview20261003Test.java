package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;

@Tag("unit")
class TypeAttrArrayComponentReview20261003Test extends TestBase {

    @ParameterizedTest
    @ValueSource(strings = { "[] []()", "[]\t[]()", "[]\n[]<A>", "[]\r\n[]<A>(value)", "[]\u2003[]()", "[] [] []()", "List<[] []()>", "Map<String, [] []<A>>",
            "Owner<[] []()>.Member", "[] []<A>[]" })
    void rejectsWhitespaceSeparatedArrayDimensionsWithoutAComponent(final String declaration) {
        final IllegalArgumentException error = assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse(declaration));
        assertTrue(error.getMessage().startsWith("Malformed type attribute: missing class name in: "));
    }

    @ParameterizedTest
    @ValueSource(strings = { "[]()", "[][]()", "[]<A>", "[] []", "[]\t[]", "[]\u2003[]" })
    void preservesExistingComponentlessRejections(final String declaration) {
        assertThrows(IllegalArgumentException.class, () -> TypeAttrParser.parse(declaration));
    }

    @ParameterizedTest
    @ValueSource(strings = { " ", "\t", "\r\n", "\u2003" })
    void retainsNonemptyClassNamesAndArgumentMetadata(final String separator) {
        final String name = "Component[]" + separator + "[]";

        assertEquals("Component[][]", TypeAttrParser.parse(name).getClassName());
        assertEquals(name, TypeAttrParser.parse(name + "()").getClassName());

        final TypeAttrParser parsed = TypeAttrParser.parse(name + "<Element>(value)");
        assertEquals(name, parsed.getClassName());
        assertArrayEquals(new String[] { "Element" }, parsed.getTypeParameters());
        assertArrayEquals(new String[] { "value" }, parsed.getParameters());

        final TypeAttrParser nested = TypeAttrParser.parse("List<" + name + "<Element>>");
        assertEquals("List", nested.getClassName());
        assertArrayEquals(new String[] { name + "<Element>" }, nested.getTypeParameters());
    }
}
