package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Comparator;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.BiFunction;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.UnaryOperator;

/**
 * Executes the {@code Fn}/{@code Fnn}/{@code Throwables} javadoc examples that were corrected on
 * 2026-09-02, so that both the <i>call syntax</i> and the <i>documented result</i> stay true.
 *
 * <p>Each example previously did not compile: a generic factory call used as the receiver of a
 * chained call is a standalone expression, so its type parameters were inferred in isolation. The
 * bodies below are copied verbatim from the corrected javadoc - if an example is edited without
 * being re-checked, this class stops compiling or stops passing.
 */
@Tag("unit")
public class FnFnnThrowablesJavadocExamplesTest extends TestBase {

    // ------------------------------------------------------------------------------------------
    // Fn.isNull / isEmpty / isBlank / notNull / notEmpty / notBlank (valueExtractor overloads)
    // ------------------------------------------------------------------------------------------

    @Test
    public void test_isNull_valueExtractor_example() {
        final Predicate<Map.Entry<String, String>> hasNullValue = Fn.isNull(Map.Entry::getValue);
        assertTrue(hasNullValue.test(CommonUtil.newEntry("a", null)));
        assertFalse(hasNullValue.test(Map.entry("a", "x")));
    }

    @Test
    public void test_isEmpty_valueExtractor_example() {
        final Predicate<Map.Entry<String, String>> hasEmptyValue = Fn.isEmpty(Map.Entry::getValue);
        assertTrue(hasEmptyValue.test(Map.entry("a", "")));
        assertTrue(hasEmptyValue.test(CommonUtil.newEntry("a", null)));
        assertFalse(hasEmptyValue.test(Map.entry("a", "x")));
    }

    @Test
    public void test_isBlank_valueExtractor_example() {
        final Predicate<Map.Entry<String, String>> hasBlankValue = Fn.isBlank(Map.Entry::getValue);
        assertTrue(hasBlankValue.test(Map.entry("a", "   ")));
        assertTrue(hasBlankValue.test(CommonUtil.newEntry("a", null)));
        assertFalse(hasBlankValue.test(Map.entry("a", "hello")));
        // Unicode boundary: NBSP (U+00A0) is NOT whitespace to Character.isWhitespace, so it is not
        // blank; the ideographic space (U+3000) is. Escapes, so the assertions survive re-encoding.
        assertFalse(hasBlankValue.test(Map.entry("a", "\u00A0")));
        assertTrue(hasBlankValue.test(Map.entry("a", "\u3000")));
    }

    @Test
    public void test_notNull_valueExtractor_example() {
        final Predicate<Map.Entry<String, String>> hasValue = Fn.notNull(Map.Entry::getValue);
        assertTrue(hasValue.test(Map.entry("a", "x")));
        assertFalse(hasValue.test(CommonUtil.newEntry("a", null)));
    }

    @Test
    public void test_notEmpty_valueExtractor_example() {
        final Predicate<Map.Entry<String, String>> hasNonEmptyValue = Fn.notEmpty(Map.Entry::getValue);
        assertTrue(hasNonEmptyValue.test(Map.entry("a", "hello")));
        assertFalse(hasNonEmptyValue.test(Map.entry("a", "")));
        assertFalse(hasNonEmptyValue.test(CommonUtil.newEntry("a", null)));
    }

    @Test
    public void test_notBlank_valueExtractor_example() {
        final Predicate<Map.Entry<String, String>> hasNonBlankValue = Fn.notBlank(Map.Entry::getValue);
        assertTrue(hasNonBlankValue.test(Map.entry("a", "hello")));
        assertFalse(hasNonBlankValue.test(Map.entry("a", "   ")));
        assertFalse(hasNonBlankValue.test(CommonUtil.newEntry("a", null)));
        // Unicode: a surrogate pair is non-blank content (U+1F600 GRINNING FACE)
        assertTrue(hasNonBlankValue.test(Map.entry("a", "\uD83D\uDE00")));
    }

    // ------------------------------------------------------------------------------------------
    // Fn entry helpers
    // ------------------------------------------------------------------------------------------

    @Test
    public void test_testByKey_and_testByValue_examples() {
        assertTrue(Fn.<Integer, String> testByKey(k -> k > 5).test(Map.entry(10, "v")));
        assertTrue(Fn.<String, String> testByKey(k -> k != null).test(Map.entry("k", "v")));

        assertTrue(Fn.<String, Integer> testByValue(v -> v > 50).test(Map.entry("k", 100)));
        assertTrue(Fn.<String, String> testByValue(String::isEmpty).test(Map.entry("k", "")));
    }

    @Test
    public void test_applyByKey_and_applyByValue_examples() {
        assertEquals(10, Fn.<Integer, String, Integer> applyByKey(k -> k * 2).apply(Map.entry(5, "v")));
        assertEquals(10, Fn.<String, Integer, Integer> applyByValue(v -> v * 2).apply(Map.entry("k", 5)));
    }

    @Test
    public void test_mapKey_and_mapValue_examples() {
        assertEquals(CommonUtil.newEntry("KEY", "val"), Fn.<String, String, String> mapKey(k -> k.toUpperCase()).apply(Map.entry("key", "val")));
        assertEquals(CommonUtil.newEntry("k", 10), Fn.<String, Integer, Integer> mapValue(v -> v * 2).apply(Map.entry("k", 5)));
    }

    @Test
    public void test_minBy_maxBy_key_value_examples() {
        assertEquals(CommonUtil.newEntry("a", 1), Fn.<String, Integer> minByKey().apply(Map.entry("a", 1), Map.entry("b", 2)));
        assertEquals(CommonUtil.newEntry("b", 2), Fn.<String, Integer> minByValue().apply(Map.entry("a", 5), Map.entry("b", 2)));
        assertEquals(CommonUtil.newEntry("b", 2), Fn.<String, Integer> maxByKey().apply(Map.entry("a", 1), Map.entry("b", 2)));
        assertEquals(CommonUtil.newEntry("a", 5), Fn.<String, Integer> maxByValue().apply(Map.entry("a", 5), Map.entry("b", 2)));
    }

    @Test
    public void test_compare_example() {
        final BiFunction<String, String, Integer> cmpStr = Fn.compare();
        assertTrue(cmpStr.apply("apple", "banana") < 0, "expected a negative value");

        final BiFunction<Integer, Integer, Integer> cmpInt = Fn.compare();
        assertEquals(0, cmpInt.apply(5, 5));
    }

    @Test
    public void test_o_example_usesAnExplicitLambda() {
        // Fn.o(String::toUpperCase) does NOT compile: an inexact method reference makes the
        // UnaryOperator/BinaryOperator overload pair ambiguous. The javadoc shows this form instead.
        final UnaryOperator<String> upper = Fn.o((String s) -> s.toUpperCase());
        assertEquals("HELLO", upper.apply("hello"));
    }

    // ------------------------------------------------------------------------------------------
    // Fnn.min / Fnn.max with a Comparator
    // ------------------------------------------------------------------------------------------

    @Test
    public void test_fnn_min_max_comparator_examples() throws Exception {
        assertEquals(3, Fnn.<Integer, Exception> min(Comparator.<Integer> naturalOrder()).apply(3, 5));
        assertEquals(5, Fnn.<Integer, Exception> min(Comparator.<Integer> reverseOrder()).apply(3, 5));
        assertEquals(5, Fnn.<Integer, Exception> max(Comparator.<Integer> naturalOrder()).apply(3, 5));
        assertEquals(3, Fnn.<Integer, Exception> max(Comparator.<Integer> reverseOrder()).apply(3, 5));
    }

    // ------------------------------------------------------------------------------------------
    // Throwables.Iterator - the examples now qualify the nested type
    // ------------------------------------------------------------------------------------------

    @Test
    public void test_throwablesIterator_examples_qualifyTheNestedType() throws Exception {
        final Throwables.Iterator<String, IOException> empty = Throwables.Iterator.empty();
        assertFalse(empty.hasNext());

        final Throwables.Iterator<String, IOException> single = Throwables.Iterator.just("hello");
        assertEquals("hello", single.next());
        assertFalse(single.hasNext());

        final Throwables.Iterator<String, RuntimeException> iter = Throwables.Iterator.of("a", "b", "c");
        assertEquals("a", iter.next());

        final String[] data = { "a", "b", "c", "d", "e" };
        final Throwables.Iterator<String, RuntimeException> ranged = Throwables.Iterator.of(data, 1, 4);
        assertEquals(CommonUtil.asList("b", "c", "d"), ranged.toList());

        final Throwables.Iterator<String, RuntimeException> combined = Throwables.Iterator.concat(Throwables.Iterator.<String, RuntimeException> of("a", "b"),
                Throwables.Iterator.<String, RuntimeException> of("c", "d"));
        assertEquals(CommonUtil.asList("a", "b", "c", "d"), combined.toList());
    }

    // ------------------------------------------------------------------------------------------
    // The "instead of / you can use" blocks: the two declarations must not collide
    // ------------------------------------------------------------------------------------------

    @Test
    public void test_inferenceHelper_examples_declareDistinctVariables() {
        final com.landawn.abacus.util.function.Supplier<String> explicitSupplier = () -> "value";
        final var inferredSupplier = Fn.s(() -> "value");
        assertEquals(explicitSupplier.get(), inferredSupplier.get());

        final Predicate<String> explicitPredicate = s -> s.length() > 5;
        final var inferredPredicate = Fn.p((String s) -> s.length() > 5);
        assertEquals(explicitPredicate.test("abcdef"), inferredPredicate.test("abcdef"));

        final com.landawn.abacus.util.function.Consumer<String> explicitLogger = str -> {
        };
        final var inferredLogger = Fn.c((String str) -> {
        });
        explicitLogger.accept("x");
        inferredLogger.accept("x");
    }
}
