package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

/**
 * Characterization tests for the documentation findings of the 2026-08-30c review of {@link Strings}.
 *
 * <ul>
 *   <li><b>C-001</b> the class uses three different whitespace predicates ({@code Character.isWhitespace},
 *       {@code isWhitespace || isSpaceChar}, and {@code char <= 32}) that are <i>not</i> interchangeable.
 *       The class javadoc previously claimed there were two and that they were equivalent.</li>
 *   <li><b>C-002</b> {@code reverse}, {@code rotate} and {@code shuffle} keep valid surrogate pairs together
 *       but do <i>not</i> keep extended grapheme clusters intact.</li>
 *   <li><b>D-1</b> {@code toCamelCase}/{@code toUpperCamelCase} are not idempotent, while
 *       {@code toSnakeCase}/{@code toKebabCase}/{@code toScreamingSnakeCase} are.</li>
 * </ul>
 *
 * These pin behaviour that is now documented, so a future change cannot silently invalidate the javadoc.
 */
public class StringsRegressionATest extends TestBase {

    private static final String CTRL = String.valueOf((char) 0x0001); // START OF HEADING (not Java whitespace)
    private static final String NEL = String.valueOf((char) 0x0085); // NEXT LINE (not Java whitespace)
    private static final String NBSP = String.valueOf((char) 0x00A0); // NO-BREAK SPACE (isSpaceChar only)
    private static final String OGHAM = String.valueOf((char) 0x1680); // OGHAM SPACE MARK (Java whitespace)
    private static final String EN_QUAD = String.valueOf((char) 0x2000); // EN QUAD (Java whitespace)
    private static final String FIGURE_SP = String.valueOf((char) 0x2007); // FIGURE SPACE (isSpaceChar only)
    private static final String NNBSP = String.valueOf((char) 0x202F); // NARROW NO-BREAK SPACE (isSpaceChar only)
    private static final String IDEO_SP = String.valueOf((char) 0x3000); // IDEOGRAPHIC SPACE (Java whitespace)

    /**
     * The three non-breaking Unicode space separators are exactly the code points where
     * {@code isSpaceChar} and {@code isWhitespace} disagree, so they are exactly the ones
     * {@code normalizeSpace} collapses and {@code strip} keeps.
     */
    @Test
    public void testExactlyThreeNonBreakingSpaceSeparatorsExist() {
        final StringBuilder divergent = new StringBuilder();
        for (int cp = 0; cp <= Character.MAX_CODE_POINT; cp++) {
            if (Character.isSpaceChar(cp) && !Character.isWhitespace(cp)) {
                divergent.append(String.format("U+%04X ", cp));
            }
        }
        assertEquals("U+00A0 U+2007 U+202F ", divergent.toString());
    }

    // ---------------------------------------------------------------- C-001

    @Test
    public void testWhitespacePredicates_trimIsCharLessThanOrEqual32() {
        // trim() uses String#trim's "char <= 32" rule: it removes low control characters ...
        assertEquals("x", Strings.trim(CTRL + "x" + CTRL));
        // ... but nothing above U+0020, not even characters that ARE Java whitespace.
        assertEquals(OGHAM + "x" + OGHAM, Strings.trim(OGHAM + "x" + OGHAM));
        assertEquals(EN_QUAD + "x" + EN_QUAD, Strings.trim(EN_QUAD + "x" + EN_QUAD));
        assertEquals(IDEO_SP + "x" + IDEO_SP, Strings.trim(IDEO_SP + "x" + IDEO_SP));
        assertEquals(NEL + "x" + NEL, Strings.trim(NEL + "x" + NEL));
        assertEquals(NBSP + "x" + NBSP, Strings.trim(NBSP + "x" + NBSP));
        assertEquals(FIGURE_SP + "x" + FIGURE_SP, Strings.trim(FIGURE_SP + "x" + FIGURE_SP));
    }

    @Test
    public void testWhitespacePredicates_stripIsCharacterIsWhitespace() {
        // strip() uses Character.isWhitespace: the mirror image of trim() at both ends of the range.
        assertEquals(CTRL + "x" + CTRL, Strings.strip(CTRL + "x" + CTRL));
        assertEquals("x", Strings.strip(OGHAM + "x" + OGHAM));
        assertEquals("x", Strings.strip(EN_QUAD + "x" + EN_QUAD));
        assertEquals("x", Strings.strip(IDEO_SP + "x" + IDEO_SP));
        // U+0085, U+00A0 and U+2007 are not Java whitespace, so strip keeps them too.
        assertEquals(NEL + "x" + NEL, Strings.strip(NEL + "x" + NEL));
        assertEquals(NBSP + "x" + NBSP, Strings.strip(NBSP + "x" + NBSP));
        assertEquals(FIGURE_SP + "x" + FIGURE_SP, Strings.strip(FIGURE_SP + "x" + FIGURE_SP));
        assertEquals(NNBSP + "x" + NNBSP, Strings.strip(NNBSP + "x" + NNBSP));
        // U+2007 is the one member of U+2000..U+200A that strip keeps, because it is non-breaking.
        for (int cp = 0x2000; cp <= 0x200A; cp++) {
            final String sp = String.valueOf((char) cp);
            assertEquals(cp == 0x2007 ? sp + "x" + sp : "x", Strings.strip(sp + "x" + sp), "U+" + Integer.toHexString(cp));
        }
    }

    @Test
    public void testWhitespacePredicates_normalizeSpaceAlsoUsesIsSpaceChar() {
        // normalizeSpace uses the strictly wider "isWhitespace || isSpaceChar" set, so it collapses the
        // non-breaking separators that strip/removeWhitespace/isBlank all leave alone.
        assertEquals("x", Strings.normalizeSpace(NBSP + "x" + NBSP));
        assertEquals("x", Strings.normalizeSpace(FIGURE_SP + "x" + FIGURE_SP));
        assertEquals("x", Strings.normalizeSpace(NNBSP + "x" + NNBSP));
        assertEquals("a b", Strings.normalizeSpace("a" + NBSP + FIGURE_SP + NNBSP + "b"));
        // ... and it still leaves the low control characters alone (they are neither).
        assertEquals(CTRL + "x" + CTRL, Strings.normalizeSpace(CTRL + "x" + CTRL));
        assertEquals(NEL + "x" + NEL, Strings.normalizeSpace(NEL + "x" + NEL));
    }

    @Test
    public void testWhitespacePredicates_isBlankAndRemoveWhitespaceKeepNonBreakingSeparators() {
        assertFalse(Strings.isBlank(NBSP));
        assertFalse(Strings.isBlank(FIGURE_SP));
        assertTrue(Strings.isBlank(OGHAM));
        assertTrue(Strings.isBlank(IDEO_SP));

        assertEquals(NBSP + "x" + NBSP, Strings.removeWhitespace(NBSP + "x" + NBSP));
        assertEquals(FIGURE_SP + "x", Strings.removeWhitespace(FIGURE_SP + "x" + IDEO_SP));

        // splitOnWhitespace does not treat U+00A0 as a boundary either.
        assertEquals(1, Strings.splitOnWhitespace("a" + NBSP + "b").length);
        assertEquals(2, Strings.splitOnWhitespace("a" + IDEO_SP + "b").length);
    }

    @Test
    public void testWhitespacePredicates_nullAndEmptyInputs() {
        assertEquals(null, Strings.trim(null));
        assertEquals(null, Strings.strip(null));
        assertEquals(null, Strings.normalizeSpace(null));
        assertEquals(null, Strings.removeWhitespace(null));
        assertEquals("", Strings.trim(""));
        assertEquals("", Strings.strip(""));
        assertEquals("", Strings.normalizeSpace(""));
        assertEquals("", Strings.removeWhitespace(""));
        assertEquals("", Strings.normalizeSpace(NBSP + FIGURE_SP));
    }

    // ---------------------------------------------------------------- C-002

    private static final char ZWJ = (char) 0x200D;
    private static final char COMBINING_ACUTE = (char) 0x0301;
    private static final char HIGH_EMOJI = (char) 0xD83D;
    private static final char LOW_MAN = (char) 0xDC68;
    private static final char LOW_WOMAN = (char) 0xDC69;
    private static final char LOW_GIRL = (char) 0xDC67;
    private static final char HIGH_FLAG = (char) 0xD83C;
    private static final char LOW_RI_U = (char) 0xDDFA;
    private static final char LOW_RI_S = (char) 0xDDF8;

    @Test
    public void testReverseKeepsSurrogatePairsButBreaksGraphemeClusters() {
        // Surrogate pairs survive (this part is, and stays, documented).
        assertEquals("" + HIGH_EMOJI + LOW_MAN + "a", Strings.reverse("a" + HIGH_EMOJI + LOW_MAN));

        // Combining sequence: the mark ends up in front of a different base (here, of nothing).
        assertEquals(COMBINING_ACUTE + "efac", Strings.reverse("cafe" + COMBINING_ACUTE));

        // Regional-indicator pair: reversing turns one flag into another.
        assertEquals("" + HIGH_FLAG + LOW_RI_S + HIGH_FLAG + LOW_RI_U, Strings.reverse("" + HIGH_FLAG + LOW_RI_U + HIGH_FLAG + LOW_RI_S));

        // ZWJ sequence: the joiners survive but the members are re-ordered.
        String family = "" + HIGH_EMOJI + LOW_MAN + ZWJ + HIGH_EMOJI + LOW_WOMAN + ZWJ + HIGH_EMOJI + LOW_GIRL;
        assertEquals("" + HIGH_EMOJI + LOW_GIRL + ZWJ + HIGH_EMOJI + LOW_WOMAN + ZWJ + HIGH_EMOJI + LOW_MAN, Strings.reverse(family));
    }

    @Test
    public void testRotateAndSortBreakGraphemeClusters() {
        String family = "" + HIGH_EMOJI + LOW_MAN + ZWJ + HIGH_EMOJI + LOW_WOMAN + ZWJ + HIGH_EMOJI + LOW_GIRL;

        // rotate moves one code point, leaving a dangling ZWJ at the end.
        assertEquals("" + HIGH_EMOJI + LOW_GIRL + HIGH_EMOJI + LOW_MAN + ZWJ + HIGH_EMOJI + LOW_WOMAN + ZWJ, Strings.rotate(family, 1));

        // sort orders by code point, so the two joiners migrate to the front.
        assertEquals("" + ZWJ + ZWJ + HIGH_EMOJI + LOW_GIRL + HIGH_EMOJI + LOW_MAN + HIGH_EMOJI + LOW_WOMAN, Strings.sort(family));

        // ... while still never splitting a surrogate pair.
        assertEquals(COMBINING_ACUTE + "cafe", Strings.rotate("cafe" + COMBINING_ACUTE, 1));
    }

    @Test
    public void testShuffleIsAPermutationOfCodePointsOnly() {
        String family = "" + HIGH_EMOJI + LOW_MAN + ZWJ + HIGH_EMOJI + LOW_WOMAN + ZWJ + HIGH_EMOJI + LOW_GIRL;
        String shuffled = Strings.shuffle(family, new java.util.Random(7));

        int[] before = family.codePoints().sorted().toArray();
        int[] after = shuffled.codePoints().sorted().toArray();
        assertTrue(java.util.Arrays.equals(before, after), "shuffle must permute the same code points");
        assertEquals(family.length(), shuffled.length());
    }

    @Test
    public void testClusterMethodsNullAndEmpty() {
        assertEquals(null, Strings.reverse(null));
        assertEquals(null, Strings.rotate(null, 1));
        assertEquals(null, Strings.sort(null));
        assertEquals(null, Strings.shuffle(null));
        assertEquals("", Strings.reverse(""));
        assertEquals("", Strings.rotate("", 1));
        assertEquals("", Strings.sort(""));
        assertEquals("", Strings.shuffle(""));
        // a single supplementary character is one code point, so all four are no-ops
        String grin = "" + HIGH_EMOJI + (char) 0xDE00;
        assertEquals(grin, Strings.reverse(grin));
        assertEquals(grin, Strings.rotate(grin, 1));
        assertEquals(grin, Strings.sort(grin));
        assertEquals(grin, Strings.shuffle(grin));
    }

    // ------------------------------------------------------------------ D-1

    @Test
    public void testCamelCaseConvertersAreNotIdempotent() {
        // Adjacent single-letter words become an acronym-shaped run that a second pass re-splits.
        assertEquals("aCA", Strings.toCamelCase("a-c a"));
        assertEquals("aCa", Strings.toCamelCase("aCA"));
        assertNotEquals(Strings.toCamelCase("a-c a"), Strings.toCamelCase(Strings.toCamelCase("a-c a")));

        assertEquals("CD", Strings.toUpperCamelCase("c-d "));
        assertEquals("Cd", Strings.toUpperCamelCase("CD"));
        assertNotEquals(Strings.toUpperCamelCase("c-d "), Strings.toUpperCamelCase(Strings.toUpperCamelCase("c-d ")));

        assertEquals("ABBaAaA", Strings.toUpperCamelCase("aBBaAaA"));
        assertEquals("AbBaAaA", Strings.toUpperCamelCase("ABBaAaA"));

        // A third application is stable: only the first re-split changes anything.
        assertEquals("aCa", Strings.toCamelCase("aCa"));
        assertEquals("Cd", Strings.toUpperCamelCase("Cd"));
    }

    @Test
    public void testDelimitedCaseConvertersAreIdempotent() {
        for (final String input : new String[] { "a-c a", "c-d ", "aBBaAaA", "helloWorldAPI", "  x__y  ", "a", "", "" + HIGH_EMOJI + LOW_MAN + "aB" }) {
            assertEquals(Strings.toSnakeCase(input), Strings.toSnakeCase(Strings.toSnakeCase(input)), "toSnakeCase: " + input);
            assertEquals(Strings.toKebabCase(input), Strings.toKebabCase(Strings.toKebabCase(input)), "toKebabCase: " + input);
            assertEquals(Strings.toScreamingSnakeCase(input), Strings.toScreamingSnakeCase(Strings.toScreamingSnakeCase(input)),
                    "toScreamingSnakeCase: " + input);
        }
    }

    @Test
    public void testCamelCaseConvertersNullEmptyAndUnicode() {
        assertEquals(null, Strings.toCamelCase(null));
        assertEquals(null, Strings.toUpperCamelCase(null));
        assertEquals("", Strings.toCamelCase(""));
        assertEquals("", Strings.toUpperCamelCase(""));
        // a supplementary code point is not a letter here, so it is carried through unchanged
        String grin = "" + HIGH_EMOJI + (char) 0xDE00;
        assertEquals(grin, Strings.toCamelCase(grin));
        assertEquals(grin, Strings.toUpperCamelCase(grin));
    }
    /**
     * The class-level whitespace-predicate index lists {@code containsWhitespace} in the code-unit half and the
     * case-conversion family, {@code capitalizeWords}/{@code capitalizeWordsFully} and {@code mapWords} in the
     * code-point half. Both halves resolve to Java whitespace, so a non-breaking separator is neither a
     * {@code containsWhitespace} hit nor a word separator, while U+3000 is both.
     */
    @Test
    public void testWhitespacePredicates_containsWhitespaceAndCaseConversionFamily() {
        assertFalse(Strings.containsWhitespace("a" + NBSP + "b"));
        assertFalse(Strings.containsWhitespace("a" + FIGURE_SP + "b"));
        assertFalse(Strings.containsWhitespace("a" + NNBSP + "b"));
        assertFalse(Strings.containsWhitespace("a" + NEL + "b"));
        assertFalse(Strings.containsWhitespace("a" + CTRL + "b"));
        assertTrue(Strings.containsWhitespace("a" + OGHAM + "b"));
        assertTrue(Strings.containsWhitespace("a" + IDEO_SP + "b"));

        // the case-conversion family splits on a Java-whitespace run ...
        assertEquals("a_b", Strings.toSnakeCase("a" + IDEO_SP + "b"));
        assertEquals("a-b", Strings.toKebabCase("a" + IDEO_SP + "b"));
        assertEquals("A_B", Strings.toScreamingSnakeCase("a" + IDEO_SP + "b"));
        assertEquals("aB", Strings.toCamelCase("a" + IDEO_SP + "b"));
        assertEquals("AB", Strings.toUpperCamelCase("a" + IDEO_SP + "b"));
        assertEquals("AB", Strings.toPascalCase("a" + IDEO_SP + "b"));
        assertEquals("a_b", Strings.toSnakeCase("a" + OGHAM + "b"));

        // ... and not on a non-breaking separator, which is not Java whitespace
        assertEquals("a" + NBSP + "b", Strings.toSnakeCase("a" + NBSP + "b"));
        assertEquals("a" + NBSP + "b", Strings.toKebabCase("a" + NBSP + "b"));
        assertEquals("A" + NBSP + "B", Strings.toScreamingSnakeCase("a" + NBSP + "b"));
        assertEquals("a" + NBSP + "b", Strings.toCamelCase("a" + NBSP + "b"));
        assertEquals("A" + NBSP + "b", Strings.toUpperCamelCase("a" + NBSP + "b"));
        assertEquals("a" + FIGURE_SP + "b", Strings.toSnakeCase("a" + FIGURE_SP + "b"));

        // capitalizeWords, capitalizeWordsFully and mapWords use the same predicate
        assertEquals("Ab" + IDEO_SP + "Cd", Strings.capitalizeWords("ab" + IDEO_SP + "cd"));
        assertEquals("Ab" + NBSP + "cd", Strings.capitalizeWords("ab" + NBSP + "cd"));
        assertEquals("Ab" + IDEO_SP + "Cd", Strings.capitalizeWordsFully("AB" + IDEO_SP + "CD"));
        assertEquals("Ab" + NBSP + "cd", Strings.capitalizeWordsFully("AB" + NBSP + "CD"));
        assertEquals("<ab>" + IDEO_SP + "<cd>", Strings.mapWords("ab" + IDEO_SP + "cd", w -> "<" + w + ">"));
        assertEquals("<ab" + NBSP + "cd>", Strings.mapWords("ab" + NBSP + "cd", w -> "<" + w + ">"));

        // the code-unit half of the index
        final String[] tokens = Strings.splitOnWhitespacePreserveAllTokens("a" + IDEO_SP + IDEO_SP + "b");
        assertEquals(3, tokens.length);
        assertEquals("a", tokens[0]);
        assertEquals("", tokens[1]);
        assertEquals("b", tokens[2]);
        assertEquals(1, Strings.splitOnWhitespacePreserveAllTokens("a" + NBSP + NBSP + "b").length);
        assertEquals("a", Strings.stripStart(IDEO_SP + "a", null));
        assertEquals("a", Strings.stripEnd("a" + IDEO_SP, null));
        assertEquals(null, Strings.stripToNull(IDEO_SP));
        assertEquals(NBSP, Strings.stripToEmpty(NBSP));
    }
}
