package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.MutableBoolean;
import com.landawn.abacus.util.MutableByte;
import com.landawn.abacus.util.MutableDouble;
import com.landawn.abacus.util.MutableFloat;
import com.landawn.abacus.util.MutableInt;
import com.landawn.abacus.util.MutableLong;
import com.landawn.abacus.util.MutableShort;
import com.landawn.abacus.util.Objectory;

/**
 * Mutable and Atomic Type.serializeTo must honor the same JSON/XML null-substitution and
 * writeLongAsString config flags that their Abstract*Type counterparts honor (previously ignored).
 */
public class MutableAtomicTypeSerializeToConfigTest extends TestBase {

    private <T> String ser(final Class<T> cls, final T value, final JsonSerConfig config) throws IOException {
        final Type<T> type = createType(cls);
        final BufferedJsonWriter writer = Objectory.createBufferedJsonWriter();

        try {
            type.serializeTo(writer, value, config);
            return writer.toString();
        } finally {
            Objectory.recycle(writer);
        }
    }

    @Test
    public void testWriteNullNumberAsZero() throws IOException {
        final JsonSerConfig cfg = JsonSerConfig.create().setWriteNullNumberAsZero(true);
        assertEquals("0", ser(MutableByte.class, null, cfg));
        assertEquals("0", ser(MutableShort.class, null, cfg));
        assertEquals("0", ser(MutableInt.class, null, cfg));
        assertEquals("0", ser(MutableLong.class, null, cfg));
        assertEquals("0.0", ser(MutableFloat.class, null, cfg));
        assertEquals("0.0", ser(MutableDouble.class, null, cfg));
        assertEquals("0", ser(AtomicInteger.class, null, cfg));
        assertEquals("0", ser(AtomicLong.class, null, cfg));
    }

    @Test
    public void testWriteNullBooleanAsFalse() throws IOException {
        final JsonSerConfig cfg = JsonSerConfig.create().setWriteNullBooleanAsFalse(true);
        assertEquals("false", ser(MutableBoolean.class, null, cfg));
        assertEquals("false", ser(AtomicBoolean.class, null, cfg));
    }

    @Test
    public void testWriteLongAsString() throws IOException {
        final JsonSerConfig cfg = JsonSerConfig.create().setWriteLongAsString(true);
        assertEquals("\"9007199254740993\"", ser(MutableLong.class, MutableLong.of(9007199254740993L), cfg));
        assertEquals("\"9007199254740993\"", ser(AtomicLong.class, new AtomicLong(9007199254740993L), cfg));
    }

    @Test
    public void testNoFlagStillWritesNull() throws IOException {
        // Without any flag, a null wrapper must still serialize to null (no regression).
        final JsonSerConfig cfg = JsonSerConfig.create();
        assertEquals("null", ser(MutableLong.class, null, cfg));
        assertEquals("null", ser(MutableInt.class, null, cfg));
        assertEquals("null", ser(MutableBoolean.class, null, cfg));
        assertEquals("null", ser(AtomicInteger.class, null, cfg));
        assertEquals("null", ser(AtomicLong.class, null, cfg));
        assertEquals("null", ser(AtomicBoolean.class, null, cfg));
    }

    // T5-01 / T6-03 / R-T01 (2026-09-06): every optional handler in one bean honours writeNullNumberAsZero / writeNullBooleanAsFalse.
    @Test
    public void reviewFixes20260906_optionalFamilyHonoursNullSubstitutionFlagsInBean() {
        final JsonSerConfig zeroAndFalse = JsonSerConfig.create().setWriteNullNumberAsZero(true).setWriteNullBooleanAsFalse(true);
        final ReviewFixesOptionalBean bean = new ReviewFixesOptionalBean();

        assertEquals("{\"ob\": 0, \"os\": 0, \"oi\": 0, \"ol\": 0, \"of\": 0.0, \"od\": 0.0, \"obool\": false, \"joi\": 0, \"jol\": 0, \"jod\": 0.0, "
                + "\"og\": 0, \"obg\": false, \"ostr\": null, \"ni\": 0, \"jog\": 0}", com.landawn.abacus.util.N.toJson(bean, zeroAndFalse));
        assertEquals("{\"ob\": null, \"os\": null, \"oi\": null, \"ol\": null, \"of\": null, \"od\": null, \"obool\": null, \"joi\": null, \"jol\": null, "
                + "\"jod\": null, \"og\": null, \"obg\": null, \"ostr\": null, \"ni\": null, \"jog\": null}", com.landawn.abacus.util.N.toJson(bean));

        // writeLongAsString quotes the substituted zero of the long handlers only
        final String las = com.landawn.abacus.util.N.toJson(bean, JsonSerConfig.create().setWriteNullNumberAsZero(true).setWriteLongAsString(true));
        org.junit.jupiter.api.Assertions.assertTrue(las.contains("\"ol\": \"0\""), las);
        org.junit.jupiter.api.Assertions.assertTrue(las.contains("\"jol\": \"0\""), las);
        org.junit.jupiter.api.Assertions.assertTrue(las.contains("\"oi\": 0,"), las);

        // the substituted values read back as PRESENT values - the documented consequence of the flags
        final ReviewFixesOptionalBean back = com.landawn.abacus.util.N.fromJson(com.landawn.abacus.util.N.toJson(bean, zeroAndFalse), ReviewFixesOptionalBean.class);
        assertEquals(0, back.oi.get());
        assertEquals(0L, back.ol.get());
        assertEquals(0.0d, back.od.get());
        assertEquals(0, back.joi.getAsInt());
        assertEquals(Integer.valueOf(0), back.og.get());
        assertEquals(Boolean.FALSE, back.obg.get());
        org.junit.jupiter.api.Assertions.assertFalse(back.obool.get());
        org.junit.jupiter.api.Assertions.assertTrue(back.ostr.isEmpty());
        assertEquals(Integer.valueOf(0), back.ni.get());
        assertEquals(Integer.valueOf(0), back.jog.get());
    }

    public static class ReviewFixesOptionalBean {
        public com.landawn.abacus.util.u.OptionalByte ob = com.landawn.abacus.util.u.OptionalByte.empty();
        public com.landawn.abacus.util.u.OptionalShort os = com.landawn.abacus.util.u.OptionalShort.empty();
        public com.landawn.abacus.util.u.OptionalInt oi = com.landawn.abacus.util.u.OptionalInt.empty();
        public com.landawn.abacus.util.u.OptionalLong ol = com.landawn.abacus.util.u.OptionalLong.empty();
        public com.landawn.abacus.util.u.OptionalFloat of = com.landawn.abacus.util.u.OptionalFloat.empty();
        public com.landawn.abacus.util.u.OptionalDouble od = com.landawn.abacus.util.u.OptionalDouble.empty();
        public com.landawn.abacus.util.u.OptionalBoolean obool = com.landawn.abacus.util.u.OptionalBoolean.empty();
        public java.util.OptionalInt joi = java.util.OptionalInt.empty();
        public java.util.OptionalLong jol = java.util.OptionalLong.empty();
        public java.util.OptionalDouble jod = java.util.OptionalDouble.empty();
        public com.landawn.abacus.util.u.Optional<Integer> og = com.landawn.abacus.util.u.Optional.empty();
        public com.landawn.abacus.util.u.Optional<Boolean> obg = com.landawn.abacus.util.u.Optional.empty();
        public com.landawn.abacus.util.u.Optional<String> ostr = com.landawn.abacus.util.u.Optional.empty();
        public com.landawn.abacus.util.u.Nullable<Integer> ni = com.landawn.abacus.util.u.Nullable.of((Integer) null);
        public java.util.Optional<Integer> jog = java.util.Optional.empty();
    }
}
