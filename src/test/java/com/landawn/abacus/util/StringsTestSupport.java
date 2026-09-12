package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import com.landawn.abacus.AbstractTest;
import com.landawn.abacus.util.Strings.DelimiterMatchMode;

public abstract class StringsTestSupport extends AbstractTest {

    public int findLength2(final char[] a, final char[] b) {
        if (CommonUtil.isEmpty(a) || CommonUtil.isEmpty(b)) {
            return 0;
        }

        int end = 0;

        final int lenA = a.length;
        final int lenB = b.length;

        final int[] dp = new int[lenB + 1];
        int maxLen = 0;

        for (int i = 1; i <= lenA; i++) {
            for (int j = lenB; j > 0; j--) {
                if (a[i - 1] == b[j - 1]) {
                    dp[j] = 1 + dp[j - 1];

                    maxLen = Math.max(maxLen, dp[j]);

                    if (dp[j] > maxLen) {
                        maxLen = dp[j];
                    }

                    end = i - 1;
                } else {
                    dp[j] = 0;
                }
            }
        }

        return maxLen;
    }

    @SafeVarargs
    protected final <T> List<T> list(final T... elements) {
        return Arrays.asList(elements);
    }

    protected List<String> substringsBetween_StackBased_(final String str, final char fromDelimiter, final char toDelimiter) {
        return Strings.substringsBetween(str, fromDelimiter, toDelimiter, DelimiterMatchMode.ALL_LEVELS);
    }

    protected List<String> substringsBetween_IgnoreNested_(final String str, final char fromDelimiter, final char toDelimiter) {
        return Strings.substringsBetween(str, fromDelimiter, toDelimiter, DelimiterMatchMode.OUTERMOST_ONLY);
    }

    protected static int indexOfIgnoreCaseReference(final String str, final String target, int fromIndex) {
        fromIndex = Math.max(0, fromIndex);

        if (target.isEmpty()) {
            return Math.min(fromIndex, str.length());
        }

        for (int i = fromIndex, end = str.length() - target.length(); i <= end; i++) {
            if (str.regionMatches(true, i, target, 0, target.length())) {
                return i;
            }
        }

        return -1;
    }

    protected static int lastIndexOfIgnoreCaseReference(final String str, final String target, final int startIndexFromBack) {
        if (startIndexFromBack < 0 || target.length() > str.length()) {
            return -1;
        }

        if (target.isEmpty()) {
            return Math.min(startIndexFromBack, str.length());
        }

        for (int i = Math.min(startIndexFromBack, str.length() - target.length()); i >= 0; i--) {
            if (str.regionMatches(true, i, target, 0, target.length())) {
                return i;
            }
        }

        return -1;
    }

    protected static String longestCommonSubstringReference(final CharSequence a, final CharSequence b) {
        final int[] codePointsA = a.toString().codePoints().toArray();
        final int[] codePointsB = b.toString().codePoints().toArray();
        final int[] matches = new int[codePointsB.length + 1];
        int earliestEndInA = 0;
        int maxLength = 0;

        for (int i = 1; i <= codePointsA.length; i++) {
            for (int j = codePointsB.length; j > 0; j--) {
                if (codePointsA[i - 1] == codePointsB[j - 1]) {
                    final int matchLength = matches[j] = matches[j - 1] + 1;

                    if (matchLength > maxLength || matchLength == maxLength && i < earliestEndInA) {
                        maxLength = matchLength;
                        earliestEndInA = i;
                    }
                } else {
                    matches[j] = 0;
                }
            }
        }

        return maxLength == 0 ? "" : new String(codePointsA, earliestEndInA - maxLength, maxLength);
    }

    protected void assertCodePointRangesAreSortedAndDisjoint(final String fieldName) throws ReflectiveOperationException {
        final Field field = Unicode17Data.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        final int[] ranges = (int[]) field.get(null);

        assertEquals(0, ranges.length % 2);
        int previousEnd = -1;

        for (int i = 0; i < ranges.length; i += 2) {
            final int start = ranges[i];
            final int end = ranges[i + 1];
            assertTrue(Character.isValidCodePoint(start));
            assertTrue(Character.isValidCodePoint(end));
            assertTrue(start <= end);
            assertTrue(start > previousEnd);
            previousEnd = end;
        }
    }

    protected void assertPropertyRangesAreSortedAndDisjoint(final String fieldName) throws ReflectiveOperationException {
        final Field field = Unicode17Data.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        final int[] ranges = (int[]) field.get(null);

        assertEquals(0, ranges.length % 3);
        int previousEnd = -1;

        for (int i = 0; i < ranges.length; i += 3) {
            final int start = ranges[i];
            final int end = ranges[i + 1];
            assertTrue(Character.isValidCodePoint(start));
            assertTrue(Character.isValidCodePoint(end));
            assertTrue(start <= end);
            assertTrue(start > previousEnd);
            assertTrue(ranges[i + 2] > 0);
            previousEnd = end;
        }
    }

    @SuppressWarnings("deprecation")
    protected static void assertCaseFormatConvertersRejectSurrogate(final char splitChar) {
        final String value = "a\uD83D\uDE00b";
        final String expectedMessage = "splitChar must not be a UTF-16 surrogate code unit: U+" + Integer.toHexString(splitChar).toUpperCase(Locale.ROOT);

        assertEquals(expectedMessage, assertThrows(IllegalArgumentException.class, () -> Strings.toCamelCase(value, splitChar)).getMessage());
        assertThrows(IllegalArgumentException.class, () -> Strings.toUpperCamelCase(value, splitChar));
        assertThrows(IllegalArgumentException.class, () -> Strings.toPascalCase(value, splitChar));
        assertThrows(IllegalArgumentException.class, () -> Strings.toSnakeCase(value, splitChar));
        assertThrows(IllegalArgumentException.class, () -> Strings.toScreamingSnakeCase(value, splitChar));
        assertThrows(IllegalArgumentException.class, () -> Strings.toKebabCase(value, splitChar));
        assertThrows(IllegalArgumentException.class, () -> Strings.toCamelCase(null, splitChar));
        assertThrows(IllegalArgumentException.class, () -> Strings.toKebabCase("", splitChar));
    }

    protected static int fullySegmentedDisplayWidth(final String str, final Strings.DisplayWidthPolicy policy, final Method nextBoundary,
            final Method clusterWidth) throws ReflectiveOperationException {
        long result = 0;

        for (int clusterStart = 0; clusterStart < str.length();) {
            final int clusterEnd = (Integer) nextBoundary.invoke(null, str, clusterStart);
            result += (Integer) clusterWidth.invoke(null, str, clusterStart, clusterEnd, policy);

            if (result >= Integer.MAX_VALUE) {
                return Integer.MAX_VALUE;
            }

            clusterStart = clusterEnd;
        }

        return (int) result;
    }

    protected static String padStartReference(final String str, final int minimumLength, final String padString) {
        final String value = str == null ? "" : str;
        if (value.length() >= minimumLength) {
            return value;
        }

        return paddingReference(padString, minimumLength - value.length()) + value;
    }

    protected static String padEndReference(final String str, final int minimumLength, final String padString) {
        final String value = str == null ? "" : str;
        if (value.length() >= minimumLength) {
            return value;
        }

        return value + paddingReference(padString, minimumLength - value.length());
    }

    protected static String paddingReference(final String padString, final int length) {
        final String actualPadString = padString == null || padString.isEmpty() ? " " : padString;
        final StringBuilder result = new StringBuilder(length + 1);
        int remaining = length;

        while (remaining >= actualPadString.length()) {
            result.append(actualPadString);
            remaining -= actualPadString.length();
        }

        if (remaining > 0) {
            int endIndex = remaining;

            if (Character.isHighSurrogate(actualPadString.charAt(remaining - 1)) && remaining < actualPadString.length()
                    && Character.isLowSurrogate(actualPadString.charAt(remaining))) {
                endIndex++;
            }

            result.append(actualPadString, 0, endIndex);
        }

        return result.toString();
    }

    protected static String repeatReference(String str, final int repeatCount, String delimiter, String prefix, String suffix) {
        str = str == null ? "" : str;
        delimiter = delimiter == null ? "" : delimiter;
        prefix = prefix == null ? "" : prefix;
        suffix = suffix == null ? "" : suffix;

        final StringBuilder result = new StringBuilder(prefix);

        for (int i = 0; i < repeatCount; i++) {
            if (i > 0) {
                result.append(delimiter);
            }

            result.append(str);
        }

        return result.append(suffix).toString();
    }

    protected static int indexOfAnyReference(final String str, int fromIndex, final char[] values) {
        if (str == null || str.isEmpty() || values == null || values.length == 0) {
            return -1;
        }

        fromIndex = Math.max(0, fromIndex);

        for (final char value : values) {
            final int index = str.indexOf(value, fromIndex);

            if (index >= 0) {
                return index;
            }
        }

        return -1;
    }

    protected static int lastIndexOfAnyReference(final String str, int startIndexFromBack, final char[] values) {
        if (str == null || str.isEmpty() || values == null || values.length == 0) {
            return -1;
        }

        startIndexFromBack = Math.min(startIndexFromBack, str.length() - 1);

        for (final char value : values) {
            final int index = str.lastIndexOf(value, startIndexFromBack);

            if (index >= 0) {
                return index;
            }
        }

        return -1;
    }

    protected static int minIndexOfAllReference(final String str, int fromIndex, final char[] values) {
        if (str == null || fromIndex > str.length() || values == null || values.length == 0) {
            return -1;
        }

        fromIndex = Math.max(0, fromIndex);
        int result = -1;

        for (final char value : values) {
            final int index = str.indexOf(value, fromIndex);

            if (index >= 0 && (result < 0 || index < result)) {
                result = index;
            }
        }

        return result;
    }

    protected static int maxLastIndexOfAllReference(final String str, int startIndexFromBack, final char[] values) {
        if (str == null || str.isEmpty() || startIndexFromBack < 0 || values == null || values.length == 0) {
            return -1;
        }

        startIndexFromBack = Math.min(startIndexFromBack, str.length() - 1);
        int result = -1;

        for (final char value : values) {
            result = Math.max(result, str.lastIndexOf(value, startIndexFromBack));
        }

        return result;
    }

    protected static String camelCaseReference(final String str, final boolean upperCamelCase) {
        final StringBuilder result = new StringBuilder(str.length());
        final StringBuilder word = new StringBuilder();
        boolean capitalizeWord = upperCamelCase;

        for (int i = 0, len = str.length(); i < len;) {
            final int codePoint = str.codePointAt(i);
            final int charCount = Character.charCount(codePoint);

            if (codePoint == '_' || codePoint == '-' || Character.isWhitespace(codePoint)) {
                if (!word.isEmpty()) {
                    appendCamelCaseWordReference(result, word, capitalizeWord);
                    word.setLength(0);
                }

                if (!result.isEmpty()) {
                    capitalizeWord = true;
                }
            } else {
                final boolean atCaseBoundary = !word.isEmpty() && (Character.isUpperCase(codePoint) || Character.isTitleCase(codePoint))
                        && (Character.isLowerCase(str.codePointBefore(i)) || (i + charCount < len && Character.isLowerCase(str.codePointAt(i + charCount))));

                if (atCaseBoundary) {
                    appendCamelCaseWordReference(result, word, capitalizeWord);
                    word.setLength(0);
                    capitalizeWord = true;
                }

                word.appendCodePoint(codePoint);
            }

            i += charCount;
        }

        if (!word.isEmpty()) {
            appendCamelCaseWordReference(result, word, capitalizeWord);
        }

        return result.toString();
    }

    protected static void appendCamelCaseWordReference(final StringBuilder output, final CharSequence word, final boolean capitalize) {
        final String lowercaseWord = word.toString().toLowerCase(Locale.ROOT);

        if (!capitalize) {
            output.append(lowercaseWord);
            return;
        }

        for (int i = 0, len = lowercaseWord.length(); i < len;) {
            final int codePoint = lowercaseWord.codePointAt(i);
            final int charCount = Character.charCount(codePoint);

            if (Character.isLetter(codePoint)) {
                output.append(lowercaseWord, 0, i);
                output.append(lowercaseWord.substring(i, i + charCount).toUpperCase(Locale.ROOT));
                output.append(lowercaseWord, i + charCount, len);
                return;
            }

            i += charCount;
        }

        output.append(lowercaseWord);
    }

    protected static String delimitedCaseReference(final String str, final char separator, final boolean uppercase) {
        final StringBuilder result = new StringBuilder(str.length() + 16);

        for (int i = 0, len = str.length(); i < len;) {
            final int codePoint = str.codePointAt(i);
            final int charCount = Character.charCount(codePoint);

            if (codePoint == '_' || codePoint == '-' || Character.isWhitespace(codePoint)) {
                final boolean atStart = result.isEmpty();

                while (i < len) {
                    final int separatorCodePoint = str.codePointAt(i);

                    if (separatorCodePoint != '_' && separatorCodePoint != '-' && !Character.isWhitespace(separatorCodePoint)) {
                        break;
                    }

                    i += Character.charCount(separatorCodePoint);
                }

                if (!atStart && i < len) {
                    result.append(separator);
                }

                continue;
            }

            if ((Character.isUpperCase(codePoint) || Character.isTitleCase(codePoint)) && i > 0
                    && (Character.isLowerCase(str.codePointBefore(i)) || (i + charCount < len && Character.isLowerCase(str.codePointAt(i + charCount))))
                    && !result.isEmpty() && result.charAt(result.length() - 1) != separator) {
                result.append(separator);
            }

            result.appendCodePoint(codePoint);
            i += charCount;
        }

        return uppercase ? result.toString().toUpperCase(Locale.ROOT) : result.toString().toLowerCase(Locale.ROOT);
    }

    protected static String capitalizeWhitespaceWordsReference(final String str, final boolean lowercaseRemainder) {
        final StringBuilder result = new StringBuilder(str.length());

        for (int i = 0, len = str.length(); i < len;) {
            final int start = i;
            final boolean whitespace = Character.isWhitespace(str.codePointAt(i));

            do {
                i += Character.charCount(str.codePointAt(i));
            } while (i < len && Character.isWhitespace(str.codePointAt(i)) == whitespace);

            if (whitespace) {
                result.append(str, start, i);
            } else {
                final String word = str.substring(start, i);
                result.append(Strings.capitalize(lowercaseRemainder ? word.toLowerCase(Locale.ROOT) : word));
            }
        }

        return result.toString();
    }

    protected static String swapAsciiCaseReference(final String str) {
        final char[] result = str.toCharArray();

        for (int i = 0; i < result.length; i++) {
            if (result[i] >= 'A' && result[i] <= 'Z') {
                result[i] += 'a' - 'A';
            } else if (result[i] >= 'a' && result[i] <= 'z') {
                result[i] -= 'a' - 'A';
            }
        }

        return new String(result);
    }

    protected static String normalizeSpaceReference(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        final StringBuilder result = new StringBuilder(str.length());
        boolean lastWasWhitespace = true;
        boolean sawNonWhitespace = false;

        for (int i = 0, len = str.length(); i < len;) {
            final int codePoint = str.codePointAt(i);

            if (Character.isWhitespace(codePoint) || Character.isSpaceChar(codePoint)) {
                if (!lastWasWhitespace) {
                    result.append(' ');
                    lastWasWhitespace = true;
                }
            } else {
                result.appendCodePoint(codePoint);
                lastWasWhitespace = false;
                sawNonWhitespace = true;
            }

            i += Character.charCount(codePoint);
        }

        if (!sawNonWhitespace) {
            return "";
        }

        if (lastWasWhitespace) {
            result.setLength(result.length() - 1);
        }

        return result.toString();
    }

    protected static String removeWhitespaceReference(final String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        final StringBuilder result = new StringBuilder(str.length());

        for (int i = 0; i < str.length(); i++) {
            if (!Character.isWhitespace(str.charAt(i))) {
                result.append(str.charAt(i));
            }
        }

        return result.toString();
    }

    protected static String escapeQuotesReference(final String str, final char quoteChar, final boolean escapeBothQuotes) {
        if (str == null || str.isEmpty()) {
            return str;
        }

        final StringBuilder result = new StringBuilder(str.length() + 16);

        if (!escapeBothQuotes && quoteChar == '\\') {
            for (int i = 0, len = str.length(); i < len;) {
                if (str.charAt(i) != '\\') {
                    result.append(str.charAt(i++));
                    continue;
                }

                final int runStart = i;
                while (i < len && str.charAt(i) == '\\') {
                    i++;
                }

                result.append(str, runStart, i);

                if (((i - runStart) & 1) != 0) {
                    result.append('\\');
                }
            }

            return result.toString();
        }

        for (int i = 0, len = str.length(); i < len; i++) {
            final char ch = str.charAt(i);

            if (ch == '\\' && i < len - 1) {
                result.append(ch).append(str.charAt(++i));
            } else {
                if (escapeBothQuotes ? ch == '\'' || ch == '"' : ch == quoteChar) {
                    result.append('\\');
                }

                result.append(ch);
            }
        }

        return result.toString();
    }

    protected static String stripAccentsReference(final String str) {
        if (str == null) {
            return null;
        }

        StringBuilder result = null;

        for (int fromIndex = 0, len = str.length(); fromIndex < len;) {
            final int codePoint = str.codePointAt(fromIndex);
            final int toIndex = fromIndex + Character.charCount(codePoint);

            if (codePoint < 0x80) {
                if (result != null) {
                    result.append((char) codePoint);
                }

                fromIndex = toIndex;
                continue;
            }

            final String originalCodePoint = str.substring(fromIndex, toIndex);
            final String codePointNfd = java.text.Normalizer.normalize(originalCodePoint, java.text.Normalizer.Form.NFD);
            final StringBuilder decomposed = new StringBuilder(codePointNfd);

            for (int i = 0; i < decomposed.length(); i++) {
                if (decomposed.charAt(i) == '\u0141') {
                    decomposed.setCharAt(i, 'L');
                } else if (decomposed.charAt(i) == '\u0142') {
                    decomposed.setCharAt(i, 'l');
                }
            }

            final String stripped = removeAccentMarksReference(decomposed);

            if (stripped.equals(codePointNfd)) {
                if (result != null) {
                    result.append(originalCodePoint);
                }
            } else {
                if (result == null) {
                    result = new StringBuilder(str.length());
                    result.append(str, 0, fromIndex);
                }

                result.append(java.text.Normalizer.normalize(stripped, java.text.Normalizer.Form.NFC));
            }

            fromIndex = toIndex;
        }

        return result == null ? str : result.toString();
    }

    protected static String removeAccentMarksReference(final CharSequence decomposed) {
        StringBuilder result = null;

        for (int fromIndex = 0, len = decomposed.length(); fromIndex < len;) {
            final int codePoint = Character.codePointAt(decomposed, fromIndex);
            final int toIndex = fromIndex + Character.charCount(codePoint);

            if (isRemovableAccentMarkReference(codePoint)) {
                if (result == null) {
                    result = new StringBuilder(len);
                    result.append(decomposed, 0, fromIndex);
                }
            } else if (result != null) {
                result.append(decomposed, fromIndex, toIndex);
            }

            fromIndex = toIndex;
        }

        return result == null ? decomposed.toString() : result.toString();
    }

    protected static boolean isRemovableAccentMarkReference(final int codePoint) {
        if (!Unicode17Data.isMark(codePoint) || codePoint == 0x034F) {
            return false;
        }

        return codePoint >= 0x0300 && codePoint <= 0x036F || codePoint >= 0x1AB0 && codePoint <= 0x1AFF || codePoint >= 0x1DC0 && codePoint <= 0x1DFF
                || codePoint >= 0xFE20 && codePoint <= 0xFE2F;
    }

    protected static int indexOfAnyButReference(final String str, int fromIndex, final char[] valuesToExclude) {
        if (str == null || str.isEmpty()) {
            return CommonUtil.INDEX_NOT_FOUND;
        }

        fromIndex = Math.max(0, fromIndex);

        if (valuesToExclude == null || valuesToExclude.length == 0) {
            return fromIndex < str.length() ? fromIndex : CommonUtil.INDEX_NOT_FOUND;
        }

        outer: for (int i = fromIndex; i < str.length(); i++) {
            for (final char excluded : valuesToExclude) {
                if (str.charAt(i) == excluded) {
                    continue outer;
                }
            }

            return i;
        }

        return CommonUtil.INDEX_NOT_FOUND;
    }

    protected static boolean containsAllCharsReference(final String str, final char[] values) {
        if (values == null || values.length == 0) {
            return true;
        }

        if (str == null || str.isEmpty()) {
            return false;
        }

        for (final char value : values) {
            if (str.indexOf(value) < 0) {
                return false;
            }
        }

        return true;
    }

    protected static boolean containsNoneCharsReference(final String str, final char[] values) {
        if (str == null || str.isEmpty() || values == null || values.length == 0) {
            return true;
        }

        for (int i = 0; i < str.length(); i++) {
            for (final char value : values) {
                if (str.charAt(i) == value) {
                    return false;
                }
            }
        }

        return true;
    }

    protected static List<String> shortStrings(final char[] alphabet, final int maximumLength) {
        final List<String> result = new ArrayList<>();
        result.add("");
        int valueCount = 1;

        for (int length = 1; length <= maximumLength; length++) {
            valueCount *= alphabet.length;

            for (int encoded = 0; encoded < valueCount; encoded++) {
                final char[] value = new char[length];
                int remaining = encoded;

                for (int i = length - 1; i >= 0; i--) {
                    value[i] = alphabet[remaining % alphabet.length];
                    remaining /= alphabet.length;
                }

                result.add(new String(value));
            }
        }

        return result;
    }

    protected static int capacityReference(final int elementCount, final long estimatedElementLength, final long delimiterLength, final int prefixLength,
            final int suffixLength) {
        final long maximumCapacity = 1_048_576;
        final int actualElementCount = Math.max(0, elementCount);
        final int delimiterCount = Math.max(0, actualElementCount - 1);
        long result = (long) Math.max(0, prefixLength) + Math.max(0, suffixLength);

        if (result >= maximumCapacity) {
            return (int) maximumCapacity;
        }

        final long actualElementLength = Math.max(0, estimatedElementLength);

        if (actualElementCount > 0 && actualElementLength > (maximumCapacity - result) / actualElementCount) {
            return (int) maximumCapacity;
        }

        result += actualElementCount * actualElementLength;
        final long actualDelimiterLength = Math.max(0, delimiterLength);

        if (delimiterCount > 0 && actualDelimiterLength > (maximumCapacity - result) / delimiterCount) {
            return (int) maximumCapacity;
        }

        return (int) (result + delimiterCount * actualDelimiterLength);
    }

    protected static String randomString(final Random random, final char[] alphabet, final int length) {
        final StringBuilder result = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            result.append(alphabet[random.nextInt(alphabet.length)]);
        }

        return result.toString();
    }

    protected static String caseVariant(final String str) {
        final char[] result = str.toCharArray();

        for (int i = 0; i < result.length; i++) {
            if (Character.isUpperCase(result[i]) || Character.isTitleCase(result[i])) {
                result[i] = Character.toLowerCase(result[i]);
            } else if (Character.isLowerCase(result[i])) {
                result[i] = Character.toUpperCase(result[i]);
            }
        }

        return new String(result);
    }

    protected static int countMatchesIgnoreCaseReference(final String str, final String target) {
        if (str == null || str.isEmpty() || target == null || target.isEmpty()) {
            return 0;
        }

        return indicesOfIgnoreCaseReference(str, target, 0).length;
    }

    protected static int[] indicesOfIgnoreCaseReference(final String str, final String target, final int fromIndex) {
        if (str == null || target == null || target.length() > str.length()) {
            return new int[0];
        }

        final List<Integer> indices = new ArrayList<>();
        final int targetLength = target.length();
        int nextIndex = Math.max(0, fromIndex);

        if (targetLength == 0) {
            for (int i = nextIndex; i < str.length(); i++) {
                indices.add(i);
            }
        } else {
            while (nextIndex <= str.length() - targetLength) {
                final int index = indexOfIgnoreCaseReference(str, target, nextIndex);

                if (index < 0) {
                    break;
                }

                indices.add(index);
                nextIndex = index + targetLength;
            }
        }

        final int[] result = new int[indices.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = indices.get(i);
        }
        return result;
    }

    protected static String replaceIgnoreCaseReference(final String str, int fromIndex, final String target, String replacement, int max) {
        if (str == null || str.isEmpty() || target == null || target.isEmpty() || max == 0 || fromIndex >= str.length()) {
            return str;
        }

        fromIndex = Math.max(0, fromIndex);
        replacement = replacement == null ? "" : replacement;
        int end = indexOfIgnoreCaseReference(str, target, fromIndex);

        if (end < 0) {
            return str;
        }

        final StringBuilder result = new StringBuilder(str.length());
        int start = fromIndex;
        result.append(str, 0, fromIndex);

        while (end >= 0) {
            result.append(str, start, end).append(replacement);
            start = end + target.length();

            if (--max == 0) {
                break;
            }

            end = indexOfIgnoreCaseReference(str, target, start);
        }

        return result.append(str, start, str.length()).toString();
    }

    protected static String randomCodePointString(final Random random, final int length) {
        final int[] alphabet = { 'a', 'b', 'c', 0x0301, 0x034F, 0x03A3, 0x03C2, 0x10400, 0x10428, 0x1F600, 0x1F601, 0xD800, 0xDC00 };
        final StringBuilder result = new StringBuilder(length * 2);

        for (int i = 0; i < length; i++) {
            result.appendCodePoint(alphabet[random.nextInt(alphabet.length)]);
        }

        return result.toString();
    }

    protected static String reverseDelimitedReference(final String str, final char delimiter) {
        final String[] parts = Strings.splitPreserveAllTokens(str, delimiter);
        final StringBuilder result = new StringBuilder(str.length());

        for (int i = parts.length - 1; i >= 0; i--) {
            if (i < parts.length - 1) {
                result.append(delimiter);
            }

            result.append(parts[i]);
        }

        return result.toString();
    }

    protected static String concatNullToEmptyReference(final String[] values) {
        final StringBuilder result = new StringBuilder();

        for (final String value : values) {
            if (value != null) {
                result.append(value);
            }
        }

        return result.toString();
    }
}
