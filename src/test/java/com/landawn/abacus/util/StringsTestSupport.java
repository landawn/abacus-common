package com.landawn.abacus.util;

import static com.landawn.abacus.util.Strings.join;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import com.landawn.abacus.AbstractTest;

/** Reference algorithms and fixtures shared by multiple Strings test topics. */
public abstract class StringsTestSupport extends AbstractTest {

    // ---------------------------------------------------------------- C-002

    protected static final char ZWJ = (char) 0x200D;

    protected static final char HIGH_EMOJI = (char) 0xD83D;

    protected static final char LOW_MAN = (char) 0xDC68;

    protected static String decode(int n, final int len, final char[] alphabet) {
        final StringBuilder sb = new StringBuilder(len);

        for (int i = 0; i < len; i++) {
            sb.append(alphabet[n % alphabet.length]);
            n /= alphabet.length;
        }

        return sb.toString();
    }

    protected static final String EMOJI = "😀";

    protected static <T> T onSmallStack(final java.util.concurrent.Callable<T> task) throws Exception {
        final Object[] result = new Object[1];
        final Throwable[] failure = new Throwable[1];
        final Thread thread = new Thread(null, () -> {
            try {
                result[0] = task.call();
            } catch (final Throwable e) {
                failure[0] = e;
            }
        }, "small-stack", 256 * 1024);
        thread.start();
        thread.join();
        if (failure[0] != null) {
            throw new AssertionError(failure[0]);
        }
        @SuppressWarnings("unchecked")
        final T t = (T) result[0];
        return t;
    }

    // U+1F600 GRINNING FACE, U+1F601 GRINNING FACE WITH SMILING EYES (same high surrogate U+D83D)
    protected static final String SMILE = "\uD83D\uDE00";

    protected static final String GRIN = "\uD83D\uDE01";

    protected static String[] nullToEmpty(final String[] array) {
        return array == null ? new String[0] : array;
    }

    protected static String escape(final String str) {
        final StringBuilder sb = new StringBuilder();

        for (final char c : str.toCharArray()) {
            sb.append(String.format("\\u%04X", (int) c));
        }

        return sb.toString();
    }

    protected static String cp(final int codePoint) {
        return new String(Character.toChars(codePoint));
    }

    @SafeVarargs
    protected final <T> List<T> list(final T... elements) {
        return Arrays.asList(elements);
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

    protected static String randomString(final Random random, final char[] alphabet, final int length) {
        final StringBuilder result = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            result.append(alphabet[random.nextInt(alphabet.length)]);
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
