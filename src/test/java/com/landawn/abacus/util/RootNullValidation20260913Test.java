package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.Charset;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

class RootNullValidation20260913Test extends TestBase {
    @Test
    void statisticsRejectNullWithoutChangingTheirState() {
        final ShortSummaryStatistics statistics = new ShortSummaryStatistics();
        statistics.accept((short) 7);
        final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> statistics.combine(null));
        assertTrue(failure.getMessage().contains("other"));
        assertEquals(1, statistics.getCount());
        assertEquals(7, statistics.getSum());
        statistics.combine(statistics);
        assertEquals(2, statistics.getCount());
        assertEquals(14, statistics.getSum());
    }

    @Test
    void compactionValidatesRequiredBuffersAndPreservesNoOpPublicCalls() {
        final int[] values = { 10, 20, 30 };
        assertThrows(IllegalArgumentException.class, () -> PrimitiveList.compactAfterRemovingIndices(null, 0, new int[0]));
        assertThrows(IllegalArgumentException.class, () -> PrimitiveList.compactAfterRemovingIndices(values, values.length, null));
        assertArrayEquals(new int[] { 10, 20, 30 }, values);
        final ShortList list = ShortList.of((short) 1, (short) 2);
        list.removeAllAt((int[]) null);
        assertArrayEquals(new short[] { 1, 2 }, list.toArray());
    }

    @SuppressWarnings("deprecation")
    @Test
    void xmlDecoderGuardPrecedesRequiredInputValidation() {
        if (Boolean.getBoolean("abacus.xml.allowXmlEncoderDecoder")) {
            final IllegalArgumentException failure = assertThrows(IllegalArgumentException.class, () -> XmlUtil.xmlDecode(null));
            assertTrue(failure.getMessage().contains("xml"));
        } else {
            assertThrows(UnsupportedOperationException.class, () -> XmlUtil.xmlDecode(null));
        }
    }

    @Test
    void jdkAdaptersAndOptionalStringPathsRetainTheirNullPolicy() {
        assertEquals(null, Strings.getBytes(null, (Charset) null));
        assertEquals("", Strings.base64EncodeString("", null));
        assertEquals("", Strings.toLowerCase("", (Locale) null));
        assertThrows(NullPointerException.class, () -> Strings.getBytes("text", (Charset) null));
        assertThrows(NullPointerException.class, () -> ObjIterator.empty().toArray((Object[]) null));
        assertThrows(NullPointerException.class, () -> u.Optional.of((Object) null));
        assertThrows(NullPointerException.class, () -> u.OptionalInt.empty().compareTo(null));
    }

    @Test
    void numericReplacementValidatesOnlyForNonemptyInput() {
        assertEquals(null, Strings.replaceFirstInteger(null, null));
        assertEquals("", Strings.replaceFirstInteger("", null));
        assertEquals(null, Strings.replaceFirstDouble(null, null));
        assertEquals("", Strings.replaceFirstDouble("", null));
        assertEquals(null, Strings.replaceFirstDouble(null, null, true));
        assertEquals("", Strings.replaceFirstDouble("", null, true));
        assertThrows(IllegalArgumentException.class, () -> Strings.replaceFirstInteger("no number", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.replaceFirstDouble("1.5", null));
        assertThrows(IllegalArgumentException.class, () -> Strings.replaceFirstDouble("1e5", null, true));
        assertEquals("value=$1", Strings.replaceFirstInteger("value=12", "$1"));
        assertEquals("value=$1", Strings.replaceFirstDouble("value=1.5", "$1"));
        assertEquals("value=$1", Strings.replaceFirstDouble("value=1e5", "$1", true));
    }
}
