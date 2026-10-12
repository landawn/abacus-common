package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.StringReader;
import java.lang.management.ManagementFactory;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Random;
import java.util.TreeMap;
import java.util.concurrent.atomic.AtomicInteger;

import javax.xml.parsers.DocumentBuilderFactory;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalInt;

@Tag("unit")
public class AlgorithmsPerformanceReview20261007Test extends TestBase {
    private static final Class<?>[] PRIMITIVES = { boolean.class, char.class, byte.class, short.class, int.class, long.class, float.class, double.class };

    private static Object primitiveArray(final Class<?> type, final int[] values) {
        final Object result = Array.newInstance(type, values.length);
        final float[] floats = { 0f, -0f, Float.NaN, Float.intBitsToFloat(0x7fa12345), Float.POSITIVE_INFINITY, Float.NEGATIVE_INFINITY, 1f, -2f };
        final double[] doubles = { 0d, -0d, Double.NaN, Double.longBitsToDouble(0x7ff0123412341234L), Double.POSITIVE_INFINITY, Double.NEGATIVE_INFINITY, 1d,
                -2d };
        for (int i = 0; i < values.length; i++) {
            final int value = values[i];
            if (type == boolean.class) {
                Array.setBoolean(result, i, (value & 1) != 0);
            } else if (type == char.class) {
                Array.setChar(result, i, (char) value);
            } else if (type == byte.class) {
                Array.setByte(result, i, (byte) value);
            } else if (type == short.class) {
                Array.setShort(result, i, (short) value);
            } else if (type == int.class) {
                Array.setInt(result, i, value);
            } else if (type == long.class) {
                Array.setLong(result, i, value);
            } else if (type == float.class) {
                Array.setFloat(result, i, floats[value & 7]);
            } else {
                Array.setDouble(result, i, doubles[value & 7]);
            }
        }
        return result;
    }

    private static int search(final Class<?> type, final Object source, final int bound, final Object pattern, final int offset, final int size,
            final boolean reverse) throws Exception {
        final Class<?> arrayType = Array.newInstance(type, 0).getClass();
        final Method method = Index.class.getMethod(reverse ? "lastOfSubArray" : "ofSubArray", arrayType, int.class, arrayType, int.class, int.class);
        try {
            return ((OptionalInt) method.invoke(null, source, bound, pattern, offset, size)).orElse(-1);
        } catch (final InvocationTargetException e) {
            if (e.getCause() instanceof Exception cause) {
                throw cause;
            }
            throw e;
        }
    }

    private static int referenceSearch(final Object source, final int bound, final Object pattern, final int offset, final int size, final boolean reverse) {
        if (source == null || pattern == null || reverse && bound < 0) {
            return -1;
        }
        final int length = Array.getLength(source);
        if (size == 0) {
            return reverse ? Math.min(bound, length) : Math.min(Math.max(bound, 0), length);
        }
        final int first = reverse ? Math.min(bound, length - size) : Math.max(bound, 0);
        for (int i = first; i >= 0 && i <= length - size; i += reverse ? -1 : 1) {
            boolean matches = true;
            for (int j = 0; j < size; j++) {
                // Wrapper equality has the same canonical-NaN and signed-zero rules as N.equals.
                if (!Objects.equals(Array.get(source, i + j), Array.get(pattern, offset + j))) {
                    matches = false;
                    break;
                }
            }
            if (matches) {
                return i;
            }
        }
        return -1;
    }

    @Test
    public void primitiveRangesMatchIndependentOracle() throws Exception {
        final Random random = new Random(20261007);
        for (final Class<?> type : PRIMITIVES) {
            for (int trial = 0; trial < 350; trial++) {
                final int[] sourceValues = new int[random.nextInt(220)];
                final int[] patternValues = new int[random.nextInt(75)];
                for (int i = 0; i < sourceValues.length; i++) {
                    sourceValues[i] = random.nextInt(8);
                }
                for (int i = 0; i < patternValues.length; i++) {
                    patternValues[i] = random.nextInt(8);
                }
                final int offset = random.nextInt(patternValues.length + 1);
                final int size = random.nextInt(patternValues.length - offset + 1);
                if (size <= sourceValues.length && trial % 2 == 0) {
                    System.arraycopy(patternValues, offset, sourceValues, random.nextInt(sourceValues.length - size + 1), size);
                }
                final Object source = primitiveArray(type, sourceValues);
                final Object pattern = primitiveArray(type, patternValues);
                final int bound = trial % 13 == 0 ? Integer.MAX_VALUE : trial % 17 == 0 ? Integer.MIN_VALUE : random.nextInt(240) - 10;
                for (final boolean reverse : new boolean[] { false, true }) {
                    assertEquals(referenceSearch(source, bound, pattern, offset, size, reverse), search(type, source, bound, pattern, offset, size, reverse),
                            type + " trial=" + trial + " reverse=" + reverse);
                }
            }
        }
    }

    @Test
    public void repeatedPrefixesFallBackWithoutLosingOffsetsOrLastMatch() throws Exception {
        for (final Class<?> type : PRIMITIVES) {
            final int[] sourceValues = new int[4096];
            final int[] patternValues = new int[520];
            patternValues[514] = 1; // Slice [3, 515), not the complete pattern.
            sourceValues[2800] = 1;
            sourceValues[3800] = 1;
            final Object source = primitiveArray(type, sourceValues);
            final Object pattern = primitiveArray(type, patternValues);
            for (final int bound : new int[] { -1, 0, 100, 2500, 3288, 3289, 4096, Integer.MAX_VALUE }) {
                for (final boolean reverse : new boolean[] { false, true }) {
                    assertEquals(referenceSearch(source, bound, pattern, 3, 512, reverse), search(type, source, bound, pattern, 3, 512, reverse),
                            type + " bound=" + bound + " reverse=" + reverse);
                }
            }
            sourceValues[2800] = sourceValues[3800] = 0;
            final Object missing = primitiveArray(type, sourceValues);
            assertEquals(-1, search(type, missing, 0, pattern, 3, 512, false));
            assertEquals(-1, search(type, missing, Integer.MAX_VALUE, pattern, 3, 512, true));
        }
    }

    @Test
    public void floatingFallbackCanonicalizesNanAndDistinguishesSignedZero() throws Exception {
        final int[] sourceValues = new int[4096];
        final int[] patternValues = new int[520];
        for (int i = 0; i < sourceValues.length; i++) {
            sourceValues[i] = 2 + (i & 1);
        }
        for (int i = 0; i < patternValues.length; i++) {
            patternValues[i] = 3 - (i & 1);
        }
        sourceValues[1600] = 0; // Positive zero must not satisfy the negative-zero pattern tail.
        sourceValues[2800] = sourceValues[3800] = 1;
        patternValues[514] = 1;

        for (final Class<?> type : new Class<?>[] { float.class, double.class }) {
            final Object source = primitiveArray(type, sourceValues);
            final Object pattern = primitiveArray(type, patternValues);
            assertEquals(2289, search(type, source, 0, pattern, 3, 512, false));
            assertEquals(3289, search(type, source, Integer.MAX_VALUE, pattern, 3, 512, true));
            assertEquals(3289, search(type, source, 2300, pattern, 3, 512, false));
            assertEquals(-1, search(type, source, 2200, pattern, 3, 512, true));
        }
    }

    @Test
    public void primitiveNullEmptyAndValidationContractsRemain() throws Exception {
        for (final Class<?> type : PRIMITIVES) {
            final Object source = primitiveArray(type, new int[] { 1, 2, 3 });
            final Object empty = primitiveArray(type, new int[0]);
            assertEquals(3, search(type, source, Integer.MAX_VALUE, empty, 0, 0, false));
            assertEquals(3, search(type, source, Integer.MAX_VALUE, empty, 0, 0, true));
            assertEquals(0, search(type, source, -1, empty, 0, 0, false));
            assertEquals(-1, search(type, source, -1, empty, 0, 0, true));
            assertEquals(-1, search(type, null, 0, empty, 0, 0, false));
            assertEquals(-1, search(type, source, 0, null, 0, 0, false));
            assertThrows(IndexOutOfBoundsException.class, () -> search(type, null, 0, empty, 1, 0, false));
        }
    }

    private static boolean bounded(final String source, final String token, final String delimiter, final int index) {
        final int end = index + token.length();
        return (index == 0 || index >= delimiter.length() && source.startsWith(delimiter, index - delimiter.length()))
                && (end == source.length() || source.startsWith(delimiter, end));
    }

    private static int tokenReference(final String source, final String token, final String delimiter, int bound, final boolean reverse) {
        if (source == null || token == null || reverse && bound < 0 || token.contains(delimiter)) {
            return -1;
        }
        if (!reverse) {
            bound = Math.max(0, bound);
            if (token.isEmpty()) {
                bound = Math.min(bound, source.length());
            }
            for (int i = bound; i <= source.length() - token.length(); i++) {
                if (source.startsWith(token, i) && bounded(source, token, delimiter, i)) {
                    return i;
                }
            }
        } else {
            for (int i = Math.min(bound, source.length() - token.length()); i >= 0; i--) {
                if (source.startsWith(token, i) && bounded(source, token, delimiter, i)) {
                    return i;
                }
            }
        }
        return -1;
    }

    private static void assertTokens(final String source, final String token, final String delimiter) {
        for (final int bound : new int[] { Integer.MIN_VALUE, -1, 0, 1, 3, source.length() / 2, source.length() - 1, source.length(), Integer.MAX_VALUE }) {
            assertEquals(tokenReference(source, token, delimiter, bound, false), Strings.StrUtil.indexOfToken(source, token, delimiter, bound),
                    "forward bound=" + bound + " source=" + source + " token=" + token + " delimiter=" + delimiter);
            assertEquals(tokenReference(source, token, delimiter, bound, true), Strings.StrUtil.lastIndexOfToken(source, token, delimiter, bound),
                    "reverse bound=" + bound + " source=" + source + " token=" + token + " delimiter=" + delimiter);
        }
    }

    @Test
    public void longTokenSearchPreservesOverlapsBoundsAndEmptyFields() {
        assertTokens("a".repeat(4096), "a".repeat(2048), ",");
        for (final String delimiter : new String[] { ",", "bab", "ab".repeat(20), "a".repeat(32), "\uD800x".repeat(12) }) {
            for (final String token : new String[] { "", "a", "ba", "z".repeat(40), "\uDC00\uD800".repeat(16), delimiter }) {
                assertTokens(delimiter + token + delimiter + token + delimiter, token, delimiter);
                assertTokens(token + delimiter + token, token, delimiter);
                assertTokens("bababab", token, delimiter);
                assertTokens(token, token, delimiter);
            }
        }
        final Random random = new Random(71062026);
        for (int trial = 0; trial < 250; trial++) {
            final String delimiter = randomText(random, 1 + random.nextInt(45));
            final String token = randomText(random, random.nextInt(100));
            final String source = randomText(random, 80) + delimiter + token + delimiter + randomText(random, 90);
            assertTokens(source, token, delimiter);
        }
        assertThrows(IllegalArgumentException.class, () -> Strings.StrUtil.indexOfToken(null, null, ""));
        assertThrows(IllegalArgumentException.class, () -> Strings.StrUtil.lastIndexOfToken(null, null, null));
        assertEquals(2, Strings.StrUtil.lastIndexOfToken("a,", "", ","));
    }

    @Test
    public void tokenSearchesKeepTheirResultsWhenRejectedCandidatesSwitchToKmp() {
        // A run of overlapping rejected candidates of a long token or delimiter exhausts the scan's linear budget part-way
        // through the range; the KMP continuation must start exactly after the last rejected candidate, so delimited
        // matches before, at and after the switch point are all found in the same order as by the plain scan.
        final String token = "a".repeat(40);
        final String middle = "a".repeat(3000) + "," + token + "," + "a".repeat(3000);
        assertTokens(middle, token, ",");
        assertTokens(token + "," + "a".repeat(3000) + "," + token, token, ",");
        assertTokens("a".repeat(3000) + "," + token, token, ",");
        assertTokens(token + "," + "a".repeat(3000), token, ",");
        final String delimiter = "ab".repeat(12);
        assertTokens(delimiter.repeat(200) + "x" + delimiter + "b" + delimiter.repeat(200), "b", delimiter);
        assertTokens("b" + delimiter + "ab".repeat(2000), "b", delimiter);

        final Random random = new Random(20261008);
        for (int trial = 0; trial < 400; trial++) {
            final boolean longToken = random.nextBoolean();
            final String trialDelimiter = longToken ? randomText(random, 1 + random.nextInt(3))
                    : "ab".repeat(8 + random.nextInt(8)) + randomText(random, random.nextInt(3));
            final String trialToken = longToken ? "a".repeat(16 + random.nextInt(48)) : randomText(random, random.nextInt(4));
            final StringBuilder source = new StringBuilder();
            for (int part = 0, parts = 1 + random.nextInt(6); part < parts; part++) {
                source.append(longToken ? "a".repeat(trialToken.length() + random.nextInt(400))
                        : trialDelimiter.substring(0, trialDelimiter.length() - 1).repeat(1 + random.nextInt(30)));
                if (random.nextInt(3) == 0) {
                    source.append(trialDelimiter).append(trialToken).append(trialDelimiter);
                } else {
                    source.append(randomText(random, random.nextInt(5)));
                }
            }
            assertTokens(source.toString(), trialToken, trialDelimiter);
        }
    }

    @Test
    public void ordinaryLongTokenSearchesStayOnTheAllocationFreeScan() {
        assumeTrue(ManagementFactory.getThreadMXBean() instanceof com.sun.management.ThreadMXBean bean && bean.isThreadAllocatedMemorySupported()
                && bean.isThreadAllocatedMemoryEnabled());
        final com.sun.management.ThreadMXBean mx = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
        final StringBuilder csv = new StringBuilder();
        for (int i = 0; i < 30; i++) {
            csv.append(i == 0 ? "" : ",").append(String.format("id-%021d", i));
        }
        final String line = csv.toString();
        final String hit = String.format("id-%021d", 15);
        final String miss = String.format("id-%021d", 99);
        final String delimiter = " ||| separator ||| ";
        final StringBuilder fields = new StringBuilder();
        for (int i = 0; i < 40; i++) {
            fields.append(i == 0 ? "" : delimiter).append('v').append(i);
        }
        final String delimited = fields.toString();
        final String uuidLine = "x,123e4567-e89b-12d3-a456-426614174000,1234567890,abcdefghijklmnopqrstuvwxyz,0123456789,the-end-of-the-line-and-more";
        // A 300-character token checked for the 19-character delimiter: the containment test used to build a KMP table too.
        final String longToken = "y".repeat(300);
        final java.util.function.IntSupplier searches = () -> Strings.StrUtil.indexOfToken(line, hit, ",", 0) + Strings.StrUtil.indexOfToken(line, miss, ",", 0)
                + Strings.StrUtil.lastIndexOfToken(line, hit, ",", Integer.MAX_VALUE) + Strings.StrUtil.indexOfToken(delimited, "v20", delimiter, 0)
                + Strings.StrUtil.lastIndexOfToken(delimited, "v20", delimiter, Integer.MAX_VALUE)
                + Strings.StrUtil.indexOfToken(uuidLine, "123e4567-e89b-12d3-a456-426614174000", ",", 0)
                + Strings.StrUtil.indexOfToken(delimited, longToken, delimiter, 0) + Strings.StrUtil.lastIndexOfToken(delimited, longToken, delimiter, 99);
        final int expected = tokenReference(line, hit, ",", 0, false) + tokenReference(line, miss, ",", 0, false)
                + tokenReference(line, hit, ",", Integer.MAX_VALUE, true) + tokenReference(delimited, "v20", delimiter, 0, false)
                + tokenReference(delimited, "v20", delimiter, Integer.MAX_VALUE, true)
                + tokenReference(uuidLine, "123e4567-e89b-12d3-a456-426614174000", ",", 0, false) + tokenReference(delimited, longToken, delimiter, 0, false)
                + tokenReference(delimited, longToken, delimiter, 99, true);
        for (int i = 0; i < 2000; i++) {
            assertEquals(expected, searches.getAsInt());
        }
        final long before = mx.getCurrentThreadAllocatedBytes();
        int sum = 0;
        for (int i = 0; i < 2000; i++) {
            sum += searches.getAsInt();
        }
        final long bytes = mx.getCurrentThreadAllocatedBytes() - before;
        assertEquals(2000L * expected, sum);
        // An up-front KMP copied the delimited pattern and built a failure table for each of these calls (240-376 bytes).
        assertTrue(bytes < 2000 * 16, bytes + " bytes for 2000 rounds of ordinary searches");
    }

    @Test
    public void tokenSearchesStayLinearWhenALongTokenPrefixMatchesEverywhere() {
        // The token's 10,000-character prefix matches at every position of the text and the token never occurs, so
        // String.indexOf/lastIndexOf alone would do text * token work without returning a candidate to charge (seconds
        // here); a search that large must start with KMP.
        final String text = "a".repeat(1_000_000);
        final String missing = "a".repeat(10_000) + "b";
        final String missingBackward = "b" + "a".repeat(10_000);
        final String found = text + "," + missing;
        assertTimeoutPreemptively(java.time.Duration.ofSeconds(2), () -> {
            assertEquals(-1, Strings.StrUtil.indexOfToken(text, missing, ",", 0));
            assertEquals(-1, Strings.StrUtil.lastIndexOfToken(text, missingBackward, ",", Integer.MAX_VALUE));
            assertEquals(text.length() + 1, Strings.StrUtil.indexOfToken(found, missing, ",", 0));
            assertEquals(text.length() + 1, Strings.StrUtil.lastIndexOfToken(found, missing, ",", Integer.MAX_VALUE));
        });
    }

    @Test
    public void tokenDelimiterContainmentAgreesOnBothSidesOfItsKmpThreshold() {
        // The "token contains the delimiter" check uses String.contains below 2^20 token * delimiter characters and KMP from
        // there on; a 16-character delimiter puts that switch between 65,535- and 65,536-character tokens.
        final String delimiter = "<sep-0123456789>";
        for (final int tokenLength : new int[] { (1 << 20) / 16 - 1, (1 << 20) / 16 }) {
            final String token = "0123456789abcdef".repeat(tokenLength / 16 + 1).substring(0, tokenLength);
            final String split = token.substring(0, tokenLength / 2) + delimiter + token.substring(tokenLength / 2 + delimiter.length());
            assertEquals(tokenLength, split.length());
            final String source = "x" + delimiter + token + delimiter + "y" + delimiter + split;
            final int at = 1 + delimiter.length();
            assertEquals(at, Strings.StrUtil.indexOfToken(source, token, delimiter, 0), "token length " + tokenLength);
            assertEquals(at, Strings.StrUtil.lastIndexOfToken(source, token, delimiter, Integer.MAX_VALUE), "token length " + tokenLength);
            // A token that contains the delimiter is never a single field, not even where it ends the source.
            assertEquals(-1, Strings.StrUtil.indexOfToken(source, split, delimiter, 0), "token length " + tokenLength);
            assertEquals(-1, Strings.StrUtil.lastIndexOfToken(source, split, delimiter, Integer.MAX_VALUE), "token length " + tokenLength);
        }
    }

    private static String randomText(final Random random, final int length) {
        final StringBuilder text = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            text.append((char) ('a' + random.nextInt(3)));
        }
        return text.toString();
    }

    @Test
    public void flattenPreservesAliasesEmptySegmentsOrderingAndSupplierSemantics() {
        final Map<String, Object> child = new LinkedHashMap<>();
        child.put("", 1);
        child.put("b", null);
        final Map<String, Object> root = new LinkedHashMap<>();
        root.put("", child);
        root.put("again", child);
        root.put("empty", Map.of());
        root.put("last", 2);
        final AtomicInteger calls = new AtomicInteger();
        final Map<String, Object> result = Maps.flatten(root, "::", size -> {
            assertEquals(root.size(), size);
            calls.incrementAndGet();
            return new LinkedHashMap<>();
        });
        assertEquals(1, calls.get());
        assertEquals(List.of("::", "::b", "again::", "again::b", "last"), List.copyOf(result.keySet()));
        assertEquals(1, result.get("again::"));
        assertTrue(result.containsKey("::b"));
        assertNull(result.get("::b"));
        assertSame(child, root.get("again"));

        final String key = new String("identity");
        final Map<String, Object> identity = Maps.flatten(Map.of(key, 3), IdentityHashMap::new);
        assertTrue(identity.containsKey(key));
        assertFalse(identity.containsKey(new String("identity")));
        assertThrows(IllegalArgumentException.class,
                () -> Maps.flatten(Map.of("a", Map.of("x", 1), "A", Map.of("x", 2)), ignored -> new TreeMap<>(String.CASE_INSENSITIVE_ORDER)));
        assertThrows(IllegalArgumentException.class, () -> Maps.flatten(root, ignored -> root));
        assertThrows(NullPointerException.class, () -> Maps.flatten(root, ignored -> null));

        child.put("cycle", root);
        assertThrows(IllegalArgumentException.class, () -> Maps.flatten(root));
        child.remove("cycle");
        child.put(null, 3);
        assertThrows(IllegalArgumentException.class, () -> Maps.flatten(root));
        assertThrows(IllegalArgumentException.class, () -> Maps.flatten(Map.of("a.b", 1, "a", Map.of("b", 2))));
    }

    @Test
    public void deepMapAndDomPathsUseExplicitTraversal() throws Exception {
        final int depth = 6000;
        final Map<String, Object> root = new LinkedHashMap<>();
        Map<String, Object> current = root;
        final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        final Element element = document.createElement("r");
        document.appendChild(element);
        Element currentElement = element;
        for (int i = 0; i < depth; i++) {
            final Map<String, Object> next = new LinkedHashMap<>();
            current.put("k", next);
            current = next;
            final Element child = document.createElement("k");
            currentElement.appendChild(child);
            currentElement = child;
        }
        current.put("v", 7);
        currentElement.setTextContent(" value ");
        assertEquals(Map.of("k.".repeat(depth) + "v", 7), Maps.flatten(root));
        assertEquals(Map.of("r" + ".k".repeat(depth), "value"), XmlUtil.readElement(element));
    }

    @Test
    public void domFlattenPreservesAttributesPreorderAndRepeatedPathOverwrite() throws Exception {
        final String xml = "<r id='root'><x old='kept' name='attribute'><name> first </name></x>"
                + "<x name='replaced'><name> second </name><z> a<![CDATA[b]]>c </z></x><empty flag='yes'/></r>";
        final Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new InputSource(new StringReader(xml)));
        final Map<String, String> result = XmlUtil.readElement(document.getDocumentElement());
        final List<String> expectedKeys = new ArrayList<>(List.of("id"));
        final var firstAttributes = document.getDocumentElement().getFirstChild().getAttributes();
        for (int i = 0; i < firstAttributes.getLength(); i++) {
            expectedKeys.add("r.x." + firstAttributes.item(i).getNodeName());
        }
        expectedKeys.addAll(List.of("r.x.z", "r.empty.flag", "r.empty"));
        assertEquals(expectedKeys, List.copyOf(result.keySet()));
        assertEquals("root", result.get("id"));
        assertEquals("kept", result.get("r.x.old"));
        assertEquals("second", result.get("r.x.name"));
        assertEquals("abc", result.get("r.x.z"));
        assertEquals("", result.get("r.empty"));
    }
}
