package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;

/**
 * Regression tests for the {@code Maps} / {@code Beans} / {@code Array} review of 2026-08-30..31.
 * See {@code scripts/cross_review/Maps_Beans_Array_ledger_2026-08-30.md} for the findings these cover.
 */
public class MapsBeansArrayTest extends TestBase {

    // ------------------------------------------------------------------------------------------------
    // Fixtures
    // ------------------------------------------------------------------------------------------------

    /** Only ever passed to Array.newInstance/N.nullToEmpty by this test, so the pool assertions are stable. */
    public static class B1Uncached {
    }

    public record RecBean(String a, int b) {
    }

    public static final class ImmutableBean {
        private final String x;
        private final int y;

        public ImmutableBean(final String x, final int y) {
            this.x = x;
            this.y = y;
        }

        public String getX() {
            return x;
        }

        public int getY() {
            return y;
        }
    }

    public static class B6Base {
        private String a;

        public String getA() {
            return a;
        }
    }

    /** Immutable, but its constructor also takes the superclass's field - the shape B6 used to miss. */
    public static final class B6Sub extends B6Base {
        private final String b;

        public B6Sub(final String a, final String b) {
            this.b = b;
        }

        public String getB() {
            return b;
        }
    }

    /** Mutable, but declares a same-arity constructor - must NOT be mistaken for immutable. */
    public static class B6NotImmutable {
        private String p;
        private String q;

        public B6NotImmutable() {
        }

        public B6NotImmutable(final Integer somethingElse, final Long alsoNotTheFields) {
        }

        public String getP() {
            return p;
        }

        public void setP(final String p) {
            this.p = p;
        }

        public String getQ() {
            return q;
        }

        public void setQ(final String q) {
            this.q = q;
        }
    }

    public static class MutableBean {
        private String a;
        private int b;

        public String getA() {
            return a;
        }

        public void setA(final String a) {
            this.a = a;
        }

        public int getB() {
            return b;
        }

        public void setB(final int b) {
            this.b = b;
        }
    }

    public static class SelfRef {
        private String name;
        private SelfRef parent;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public SelfRef getParent() {
            return parent;
        }

        public void setParent(final SelfRef parent) {
            this.parent = parent;
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B1 - Array.newInstance(cls, 0) / N.nullToEmpty(a, cls) must not retain the caller's Class
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class B1_EmptyArrayCacheDoesNotRetainClasses {

        @Test
        public void newInstance_zeroLength_doesNotCacheAnUncachedComponentType() {
            final int before = CommonUtil.CLASS_EMPTY_ARRAY.size();

            final B1Uncached[] first = Array.newInstance(B1Uncached.class, 0);
            final B1Uncached[] second = Array.newInstance(B1Uncached.class, 0);

            assertEquals(0, first.length);
            assertEquals(B1Uncached.class, first.getClass().getComponentType());
            // A fresh array each time - which is the whole point: nothing was stored, so nothing is retained.
            assertNotSame(first, second);
            assertFalse(CommonUtil.CLASS_EMPTY_ARRAY.containsKey(B1Uncached.class),
                    "Array.newInstance(cls, 0) must not add the caller's class to the static empty-array pool");
            assertEquals(before, CommonUtil.CLASS_EMPTY_ARRAY.size());
        }

        @Test
        public void newInstance_zeroLength_stillSharesThePreSeededBuiltInTypes() {
            assertSame(CommonUtil.EMPTY_STRING_ARRAY, Array.newInstance(String.class, 0));
            assertSame(CommonUtil.EMPTY_INT_ARRAY, Array.newInstance(int.class, 0));
            assertSame(CommonUtil.EMPTY_OBJECT_ARRAY, Array.newInstance(Object.class, 0));
        }

        @Test
        public void newInstance_nonZeroLength_isUnaffected() {
            final B1Uncached[] a = Array.newInstance(B1Uncached.class, 3);
            assertEquals(3, a.length);
            assertNull(a[0]);
            assertThrows(NegativeArraySizeException.class, () -> Array.newInstance(B1Uncached.class, -1));
            assertThrows(IllegalArgumentException.class, () -> Array.newInstance(null, 0));
        }

        @Test
        public void nullToEmpty_doesNotCacheAnUncachedArrayType() {
            final int before = CommonUtil.CLASS_EMPTY_ARRAY.size();

            final B1Uncached[] first = CommonUtil.nullToEmpty(null, B1Uncached[].class);
            final B1Uncached[] second = CommonUtil.nullToEmpty(null, B1Uncached[].class);

            assertEquals(0, first.length);
            assertEquals(B1Uncached.class, first.getClass().getComponentType());
            assertNotSame(first, second);
            assertFalse(CommonUtil.CLASS_EMPTY_ARRAY.containsKey(B1Uncached.class));
            assertEquals(before, CommonUtil.CLASS_EMPTY_ARRAY.size());
        }

        @Test
        public void nullToEmpty_stillSharesThePreSeededTypesAndPassesNonNullThrough() {
            assertSame(CommonUtil.EMPTY_STRING_ARRAY, CommonUtil.nullToEmpty(null, String[].class));

            final String[] original = { "a" };
            assertSame(original, CommonUtil.nullToEmpty(original, String[].class));

            assertThrows(IllegalArgumentException.class, () -> CommonUtil.nullToEmpty(original, null));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B2 - the Beans mutation family must reject a record/immutable target with a clear exception
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class B2_ImmutableBeansRejectInPlaceMutation {

        private final RecBean rec = new RecBean("hi", 3);
        private final ImmutableBean immutable = new ImmutableBean("hi", 3);

        private void assertRejected(final Runnable r) {
            final UnsupportedOperationException e = assertThrows(UnsupportedOperationException.class, r::run);
            assertTrue(e.getMessage().contains("Cannot set properties on"), e.getMessage());
            assertTrue(e.getMessage().contains("immutable bean"), e.getMessage());
            // The message must point at a way forward, not just refuse - and at one that actually works for
            // every shape it rejects (copyAs does not: it fails on builder-based beans).
            assertTrue(e.getMessage().contains("Beans.mapToBean"), e.getMessage());
        }

        @Test
        public void record_isStillAFullyReadableBean() {
            assertTrue(Beans.isBeanClass(RecBean.class));
            assertEquals(CommonUtil.asList("a", "b"), new ArrayList<>(Beans.getPropNameList(RecBean.class)));
        }

        @Test
        public void clearProps_and_clearAllProps_areRejected() {
            assertRejected(() -> Beans.clearProps(rec, "a"));
            assertRejected(() -> Beans.clearProps(rec, CommonUtil.asList("a")));
            assertRejected(() -> Beans.clearAllProps(rec));
            assertRejected(() -> Beans.clearProps(immutable, "x"));
            assertRejected(() -> Beans.clearAllProps(immutable));
        }

        @Test
        public void setPropValue_isRejected() {
            assertRejected(() -> Beans.setPropValue(rec, "a", "z"));
            assertRejected(() -> Beans.setPropValue(immutable, "x", "z"));
        }

        @Test
        public void randomize_isRejected() {
            assertRejected(() -> Beans.randomize(rec));
            assertRejected(() -> Beans.randomize(rec, CommonUtil.asList("a")));
            assertRejected(() -> Beans.randomize(immutable));
        }

        @Test
        public void everyPublicMergeIntoOverloadIsRejected() {
            final RecBean src = new RecBean("q", 9);
            final BinaryOperator<Object> mf = (a, b) -> a == null ? b : a;
            final java.util.function.Function<String, String> id = Fn.identity();
            final java.util.function.BiPredicate<String, Object> all = (k, v) -> true;
            final Collection<String> sel = CommonUtil.asList("a");
            final java.util.Set<String> ignored = CommonUtil.newHashSet();

            assertRejected(() -> Beans.mergeInto(src, rec));
            assertRejected(() -> Beans.mergeInto(src, rec, mf));
            assertRejected(() -> Beans.mergeInto(src, rec, true, ignored));
            assertRejected(() -> Beans.mergeInto(src, rec, true, ignored, mf));
            assertRejected(() -> Beans.mergeInto(src, rec, id, mf));
            assertRejected(() -> Beans.mergeInto(src, rec, sel));
            assertRejected(() -> Beans.mergeInto(src, rec, sel, mf));
            assertRejected(() -> Beans.mergeInto(src, rec, sel, id));
            assertRejected(() -> Beans.mergeInto(src, rec, sel, id, mf));
            assertRejected(() -> Beans.mergeIntoIf(src, rec, all));
            assertRejected(() -> Beans.mergeIntoIf(src, rec, all, mf));
            assertRejected(() -> Beans.mergeIntoIf(src, rec, all, id));
            assertRejected(() -> Beans.mergeIntoIf(src, rec, all, id, mf));
        }

        @Test
        public void mergeInto_withNullSource_remainsANoOpRatherThanAnError() {
            // "nothing to do" wins over the guard: no write is attempted, so there is nothing to reject.
            assertSame(rec, Beans.mergeInto(null, rec));
        }

        @Test
        public void theReadAndCopyFamiliesStillWorkOnTheSameBean() {
            assertEquals(rec, Beans.copy(rec));
            assertEquals(rec, Beans.copyAs(rec, RecBean.class));
            assertEquals(rec, Beans.mapToBean(Beans.beanToMap(rec), RecBean.class));
            assertEquals("hi", Beans.<String> getPropValue(Beans.copyAs(rec, MutableBean.class), "a"));
            assertEquals(rec, Beans.copyAs(Beans.copyAs(rec, MutableBean.class), RecBean.class));
            assertEquals(CommonUtil.asMap("a", "hi", "b", 3), Beans.beanToMap(rec));
            assertEquals(rec, Beans.mapToBean(Beans.beanToMap(rec), RecBean.class));
            assertEquals(rec, Beans.mapToBean(Beans.beanToMap(rec), CommonUtil.asList("a", "b"), RecBean.class));
            assertNotNull(Beans.newRandomBean(RecBean.class));
            assertEquals(2, Beans.newRandomBeanList(RecBean.class, 2).size());
            assertEquals(2, Beans.stream(rec).count());
        }

        /**
         * Has a real setter, but no no-arg constructor - which is what {@code BeanInfo} probes with, so it is
         * classified immutable anyway. Before the guard this threw a raw {@code ClassCastException}; the point
         * of this test is that the outcome is now an explanatory {@code UnsupportedOperationException}, and
         * that the copy-based alternatives the message recommends genuinely work for this shape.
         */
        public static class NoNoArgConstructorButHasSetter {
            private String a;

            public NoNoArgConstructorButHasSetter(final String a) {
                this.a = a;
            }

            public String getA() {
                return a;
            }

            public void setA(final String a) {
                this.a = a;
            }
        }

        @Test
        public void aSetterBearingClassWithNoNoArgConstructorIsRejectedClearlyRatherThanWithACCE() {
            final NoNoArgConstructorButHasSetter bean = new NoNoArgConstructorButHasSetter("orig");

            assertRejected(() -> Beans.setPropValue(bean, "a", "changed"));
            assertRejected(() -> Beans.clearAllProps(bean));
            assertEquals("orig", bean.getA(), "the failed write must leave the bean untouched");

            // ... and the alternatives the message names really do work for this shape.
            assertEquals("orig", Beans.<NoNoArgConstructorButHasSetter> copyAs(bean, NoNoArgConstructorButHasSetter.class).getA());
            assertEquals("orig", Beans.<NoNoArgConstructorButHasSetter> mapToBean(Beans.beanToMap(bean), NoNoArgConstructorButHasSetter.class).getA());
        }

        /** Builder-based: {@code BeanInfo.isImmutable} is true for these too, and the message names them. */
        public static final class BuilderBean {
            private final String a;

            private BuilderBean(final String a) {
                this.a = a;
            }

            public static Builder builder() {
                return new Builder();
            }

            public String getA() {
                return a;
            }

            public static final class Builder {
                private String a;

                public Builder setA(final String a) {
                    this.a = a;
                    return this;
                }

                public BuilderBean build() {
                    return new BuilderBean(a);
                }
            }
        }

        @Test
        public void aBuilderBasedBeanIsRejectedForInPlaceWritesButStillReadsAndCopies() {
            final BuilderBean bean = BuilderBean.builder().setA("built").build();

            assertTrue(Beans.isBeanClass(BuilderBean.class));
            assertEquals("built", Beans.<String> getPropValue(bean, "a"));
            assertEquals(CommonUtil.asMap("a", "built"), Beans.beanToMap(bean));

            assertRejected(() -> Beans.setPropValue(bean, "a", "changed"));
            assertRejected(() -> Beans.clearAllProps(bean));
            assertRejected(() -> Beans.mergeInto(BuilderBean.builder().setA("src").build(), bean));
            assertEquals("built", bean.getA(), "the rejected writes must leave the bean untouched");

            // mapToBean builds through the builder correctly and is what the exception message recommends.
            assertEquals("built", Beans.<BuilderBean> mapToBean(Beans.beanToMap(bean), BuilderBean.class).getA());
            assertNotNull(Beans.newRandomBean(BuilderBean.class).getA(), "the builder path must still construct");

            // NOTE: Beans.copyAs/copy are BROKEN for builder-based beans - they read the target's current
            // value off the Builder as if it were the finished bean and throw ClassCastException. That is a
            // pre-existing defect in the copy machinery (verified identical before this review's changes),
            // reported separately and deliberately NOT pinned here, so fixing it will not fail this test.
        }

        @Test
        public void mutableBeansAreUnaffected() {
            final MutableBean m = new MutableBean();

            Beans.mergeInto(new RecBean("q", 9), m);
            assertEquals("q", m.getA());
            assertEquals(9, m.getB());

            Beans.setPropValue(m, "a", "z");
            assertEquals("z", m.getA());

            Beans.clearProps(m, "a");
            assertNull(m.getA());
            assertEquals(9, m.getB());

            Beans.randomize(m);
            assertNotNull(m.getA());

            Beans.clearAllProps(m);
            assertNull(m.getA());
            assertEquals(0, m.getB());
        }

        @Test
        public void clearProps_varargsOverloadStillValidatesUnknownNamesBeforeWriting() {
            final MutableBean m = new MutableBean();
            m.setA("keep");
            m.setB(7);

            assertThrows(IllegalArgumentException.class, () -> Beans.clearProps(m, "a", "nope"));
            // all-or-nothing: the valid name must not have been cleared
            assertEquals("keep", m.getA());
            assertEquals(7, m.getB());
        }

        @Test
        public void clearProps_withNullOrEmptyNamesIsANoOpEvenForARecord() {
            assertEquals(rec, applyNoOp(() -> Beans.clearProps(rec, (String[]) null)));
            assertEquals(rec, applyNoOp(() -> Beans.clearProps(rec, new String[0])));
            assertEquals(rec, applyNoOp(() -> Beans.clearProps(rec, new ArrayList<>())));
            assertEquals(rec, applyNoOp(() -> Beans.clearProps(null, "a")));
        }

        private RecBean applyNoOp(final Runnable r) {
            r.run();
            return rec;
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B6 - immutable-bean detection must span the whole hierarchy, without false positives
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class B6_ImmutableDetectionSpansTheHierarchy {

        @Test
        public void immutableSubclass_exposesBothItsOwnAndItsInheritedProperties() {
            assertEquals(CommonUtil.asList("a", "b"), new ArrayList<>(Beans.getPropNameList(B6Sub.class)));
            assertTrue(Beans.isBeanClass(B6Sub.class));
        }

        @Test
        public void immutableSubclass_isReadableEndToEnd() {
            final B6Sub bean = new B6Sub("A", "B");
            assertEquals("B", Beans.<String> getPropValue(bean, "b"));
            assertEquals(CommonUtil.asMap("b", "B"), Beans.beanToMap(bean)); // "a" is null and null props are omitted
        }

        @Test
        public void flatImmutableClassStillWorks() {
            assertEquals(CommonUtil.asList("x", "y"), new ArrayList<>(Beans.getPropNameList(ImmutableBean.class)));
        }

        /** Inherited mutable property + own final one: matched only the own-fields signature, historically. */
        public static class HybridBase {
            private String a;

            public String getA() {
                return a;
            }

            public void setA(final String a) {
                this.a = a;
            }
        }

        public static final class HybridSub extends HybridBase {
            private final String b;

            public HybridSub(final String b) {
                this.b = b;
            }

            public String getB() {
                return b;
            }
        }

        @Test
        public void theLegacyOwnFieldsOnlySignatureStillClassifiesAHybridAsImmutable() {
            // HybridSub(String) matches its own fields but not [a, b]; widening the signature must not have
            // taken this shape away, or "b" would silently stop being a property.
            assertEquals(CommonUtil.asList("a", "b"), new ArrayList<>(Beans.getPropNameList(HybridSub.class)));
            assertEquals("B", Beans.<String> getPropValue(new HybridSub("B"), "b"));
        }

        @Test
        public void aMutableClassWithASameArityConstructorIsNotMistakenForImmutable() {
            // B6NotImmutable has a 2-arg constructor, but of the wrong types - matching on arity alone would
            // classify it as immutable and change how its properties are discovered and written.
            assertEquals(CommonUtil.asList("p", "q"), new ArrayList<>(Beans.getPropNameList(B6NotImmutable.class)));

            final B6NotImmutable bean = new B6NotImmutable();
            Beans.setPropValue(bean, "p", "written"); // must not throw: this bean IS writable in place
            assertEquals("written", bean.getP());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B7 - a null property name is an argument error, not an internal NPE
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class B7_NullPropNameIsRejected {

        @Test
        public void getPropGetterFieldAndSetterAllRejectANullName() {
            for (final Runnable r : CommonUtil.asList(//
                    (Runnable) () -> Beans.getPropGetter(MutableBean.class, null), //
                    () -> Beans.getPropSetter(MutableBean.class, null), //
                    () -> Beans.getPropField(MutableBean.class, null))) {
                final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, r::run);
                assertTrue(e.getMessage().contains("propName"), e.getMessage());
            }
        }

        @Test
        public void anUnknownButNonNullNameStillAnswersNullForABeanClass() {
            assertNull(Beans.getPropGetter(MutableBean.class, "nope"));
            assertNull(Beans.getPropSetter(MutableBean.class, "nope"));
            assertNull(Beans.getPropField(MutableBean.class, "nope"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B8 - every random-bean entry point uses the same cycle-breaking scope
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class B8_RandomBeanCycleDepthIsConsistent {

        @Test
        public void newRandomBean_stopsAtTheSelfReference() {
            assertNull(Beans.newRandomBean(SelfRef.class).getParent());
        }

        @Test
        public void newRandomBeanList_stopsAtTheSameDepthAsNewRandomBean() {
            for (final SelfRef bean : Beans.newRandomBeanList(SelfRef.class, 3)) {
                assertNotNull(bean.getName());
                assertNull(bean.getParent(), "newRandomBeanList must break the cycle where newRandomBean does");
            }
        }

        @Test
        public void randomize_stopsAtTheSameDepthToo() {
            final SelfRef bean = new SelfRef();
            Beans.randomize(bean);
            assertNotNull(bean.getName());
            assertNull(bean.getParent());
        }

        @Test
        public void theVisitedScopeIsClearedAfterEachCall() {
            // If the scope leaked, the second call would see its own class as "in progress" and skip the
            // property entirely - so a repeated call must keep producing the same shape.
            Beans.newRandomBean(SelfRef.class);
            assertNotNull(Beans.newRandomBean(SelfRef.class).getName());
            Beans.newRandomBeanList(SelfRef.class, 2);
            assertNotNull(Beans.newRandomBean(SelfRef.class).getName());
        }

        @Test
        public void newRandomBeanList_validatesUpFrontEvenForZeroCount() {
            assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBeanList(MutableBean.class, CommonUtil.asList("nope"), 0));
            assertEquals(0, Beans.newRandomBeanList(MutableBean.class, 0).size());
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B3 - the float/double accessors saturate; they do not range-check
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class B3_FloatAndDoubleSaturateRatherThanThrow {

        private Map<String, Object> map() {
            final Map<String, Object> m = new HashMap<>();
            m.put("bigDecimal", new BigDecimal("1E400"));
            m.put("negBigDecimal", new BigDecimal("-1E400"));
            m.put("bigDouble", 1e300d);
            m.put("bigText", "1e400");
            m.put("nan", Double.NaN);
            return m;
        }

        @Test
        public void getAsDouble_saturatesToInfinity() {
            final Map<String, Object> m = map();
            assertEquals(Double.POSITIVE_INFINITY, Maps.getAsDouble(m, "bigDecimal").orElseThrow());
            assertEquals(Double.NEGATIVE_INFINITY, Maps.getAsDouble(m, "negBigDecimal").orElseThrow());
            assertEquals(Double.POSITIVE_INFINITY, Maps.getAsDouble(m, "bigText").orElseThrow());
            assertTrue(Double.isNaN(Maps.getAsDouble(m, "nan").orElseThrow()));
        }

        @Test
        public void getAsDoubleOrDefaultIfAbsent_saturatesToo() {
            final Map<String, Object> m = map();
            assertEquals(Double.POSITIVE_INFINITY, Maps.getAsDoubleOrDefaultIfAbsent(m, "bigDecimal", -1d));
            assertEquals(-1d, Maps.getAsDoubleOrDefaultIfAbsent(m, "missing", -1d));
        }

        @Test
        public void getAsFloat_saturatesToInfinity() {
            final Map<String, Object> m = map();
            assertEquals(Float.POSITIVE_INFINITY, Maps.getAsFloat(m, "bigDouble").orElseThrow());
            assertEquals(Float.POSITIVE_INFINITY, Maps.getAsFloat(m, "bigDecimal").orElseThrow());
            assertEquals(Float.NEGATIVE_INFINITY, Maps.getAsFloat(m, "negBigDecimal").orElseThrow());
            assertTrue(Float.isNaN(Maps.getAsFloat(m, "nan").orElseThrow()));
        }

        @Test
        public void getAsFloatOrDefaultIfAbsent_saturatesToo() {
            final Map<String, Object> m = map();
            assertEquals(Float.POSITIVE_INFINITY, Maps.getAsFloatOrDefaultIfAbsent(m, "bigDouble", -1f));
            assertEquals(-1f, Maps.getAsFloatOrDefaultIfAbsent(m, "missing", -1f));
        }

        @Test
        public void theIntegralAccessorsStillDoRangeCheck() {
            final Map<String, Object> m = new HashMap<>();
            m.put("tooBig", 300);
            assertThrows(ArithmeticException.class, () -> Maps.getAsByte(m, "tooBig"));
            assertThrows(ArithmeticException.class, () -> Maps.getAsByteOrDefaultIfAbsent(m, "tooBig", (byte) 0));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B5 - getAsChar shares one grammar and one out-of-range exception with the other accessors
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class B5_GetAsCharParsing {

        private Map<String, Object> m;

        private Map<String, Object> map() {
            if (m == null) {
                m = new HashMap<>();
                m.put("hex", "0x41");
                m.put("dec", "65");
                m.put("single", "A");
                m.put("empty", "");
                m.put("blank", " ");
                m.put("negative", "-1");
                m.put("tooBig", "65536");
                m.put("max", "65535");
                m.put("bad", "abc");
                m.put("fraction", "65.9");
                m.put("huge", "99999999999999999999999");
                m.put("numTooBig", 3000000000L);
                m.put("numFraction", 65.9d);
                m.put("numChar", 'Z');
            }
            return m;
        }

        @Test
        public void multiCharStringsUseTheSameGrammarAsTheOtherAccessors() {
            assertEquals('A', Maps.getAsChar(map(), "hex").orElseThrow());
            assertEquals(65, Numbers.toInt("0x41"), "sanity: the shared grammar accepts hex");
            assertEquals('A', Maps.getAsChar(map(), "dec").orElseThrow());
            assertEquals((char) 65535, Maps.getAsChar(map(), "max").orElseThrow());
        }

        @Test
        public void singleCharAndEmptyStringRulesAreUnchanged() {
            assertEquals('A', Maps.getAsChar(map(), "single").orElseThrow());
            assertEquals('\0', Maps.getAsChar(map(), "empty").orElseThrow());
            assertEquals(' ', Maps.getAsChar(map(), "blank").orElseThrow());
        }

        @Test
        public void outOfRangeReportsTheSameExceptionWhetherItArrivesAsTextOrAsANumber() {
            final IllegalArgumentException fromText = assertThrows(IllegalArgumentException.class, () -> Maps.getAsChar(map(), "tooBig"));
            final IllegalArgumentException fromNumber = assertThrows(IllegalArgumentException.class, () -> Maps.getAsChar(map(), "numTooBig"));
            assertTrue(fromText.getMessage().contains("out of char range"), fromText.getMessage());
            assertTrue(fromNumber.getMessage().contains("out of char range"), fromNumber.getMessage());

            assertThrows(IllegalArgumentException.class, () -> Maps.getAsChar(map(), "negative"));
        }

        @Test
        public void unparseableTextStillThrowsNumberFormatException() {
            assertThrows(NumberFormatException.class, () -> Maps.getAsChar(map(), "bad"));
            // A fractional spelling is not an integer - same rule as getAsInt("65.9").
            assertThrows(NumberFormatException.class, () -> Maps.getAsChar(map(), "fraction"));
            assertThrows(NumberFormatException.class, () -> Maps.getAsInt(map(), "fraction"));
        }

        @Test
        public void aMagnitudeBeyondLongIsAnArithmeticException() {
            assertThrows(ArithmeticException.class, () -> Maps.getAsChar(map(), "huge"));
        }

        @Test
        public void theGetByPathSiblingsShareTheSameFractionalStringSplit() {
            final Map<String, Object> user = new HashMap<>();
            user.put("strFraction", "25.9");
            user.put("numFraction", 25.9d);
            final Map<String, Object> root = new HashMap<>();
            root.put("user", user);

            assertThrows(NumberFormatException.class, () -> Maps.getByPathAsInt(root, "user.strFraction"));
            assertThrows(NumberFormatException.class, () -> Maps.getByPathAsIntOrDefaultIfAbsent(root, "user.strFraction", 0));
            assertEquals(25, Maps.getByPathAsInt(root, "user.numFraction").orElseThrow());
            assertEquals(25, Maps.getByPathAsIntOrDefaultIfAbsent(root, "user.numFraction", 0));
        }

        @Test
        public void numbersAndCharactersAreUnchanged() {
            assertEquals('Z', Maps.getAsChar(map(), "numChar").orElseThrow());
            assertEquals('A', Maps.getAsChar(map(), "numFraction").orElseThrow(), "a fractional Number truncates toward zero");
            assertEquals('A', Maps.getAsCharOrDefaultIfAbsent(map(), "hex", 'x'));
            assertEquals('x', Maps.getAsCharOrDefaultIfAbsent(map(), "missing", 'x'));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B10 - only "[...]" is reserved path syntax
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class B10_PathSegmentsEndingInABracket {

        @Test
        public void anOrdinaryKeyEndingInACloseBracketIsResolvable() {
            final Map<String, Object> map = new HashMap<>();
            map.put("a]", 7);
            map.put("b[c]d", 8);

            assertEquals(Integer.valueOf(7), Maps.getByPath(map, "a]"));
            assertEquals(Nullable.of(7), Maps.getByPathIfExists(map, "a]"));
        }

        @Test
        public void nestedUnderAKeyEndingInACloseBracket() {
            final Map<String, Object> inner = new HashMap<>();
            inner.put("x", 1);
            final Map<String, Object> map = new HashMap<>();
            map.put("a]", inner);

            assertEquals(Integer.valueOf(1), Maps.getByPath(map, "a].x"));
        }

        @Test
        public void realIndexSyntaxStillWorks() {
            final Map<String, Object> map = new HashMap<>();
            map.put("list", CommonUtil.asList("a", "b", "c"));

            assertEquals("b", Maps.getByPath(map, "list[1]"));
            assertNull(Maps.getByPath(map, "list[9]"));
            assertNull(Maps.getByPath(map, "list[]"), "a blank index stays malformed");
            assertNull(Maps.getByPath(map, "list[ ]"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B9 / E1 / P3 - difference sizing, replaceKeys message and single-traversal snapshot
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class B9_E1_P3_MapsHousekeeping {

        @Test
        public void difference_againstAnEmptyOrNullMapReturnsEveryEntry() {
            final Map<String, Integer> m = new LinkedHashMap<>();
            m.put("a", 1);
            m.put("b", 2);
            m.put("c", 3);

            for (final Map<String, Integer> other : Arrays.<Map<String, Integer>> asList(new HashMap<>(), null)) {
                final Map<String, Pair<Integer, Nullable<Integer>>> diff = Maps.difference(m, other);
                assertEquals(3, diff.size());
                assertEquals(Pair.of(1, Nullable.<Integer> empty()), diff.get("a"));
                assertEquals(Pair.of(3, Nullable.<Integer> empty()), diff.get("c"));
            }
        }

        @Test
        public void replaceKeys_duplicateMessageNamesBothSourceKeys() {
            final Map<String, Integer> m = new LinkedHashMap<>();
            m.put("aa", 1);
            m.put("ab", 2);

            final IllegalStateException e = assertThrows(IllegalStateException.class, () -> Maps.replaceKeys(m, k -> k.substring(0, 1)));
            assertTrue(e.getMessage().contains("'aa'"), e.getMessage());
            assertTrue(e.getMessage().contains("'ab'"), e.getMessage());
            // all-or-nothing: the map is untouched
            assertEquals(CommonUtil.asList("aa", "ab"), new ArrayList<>(m.keySet()));
            assertEquals(Integer.valueOf(1), m.get("aa"));
            assertEquals(Integer.valueOf(2), m.get("ab"));
        }

        @Test
        public void replaceKeys_stillRekeysCorrectlyIncludingNullValuesAndOrder() {
            final Map<String, Integer> m = new LinkedHashMap<>();
            m.put("one", 1);
            m.put("two", null);
            m.put("three", 3);

            Maps.replaceKeys(m, String::toUpperCase);

            assertEquals(CommonUtil.asList("ONE", "TWO", "THREE"), new ArrayList<>(m.keySet()));
            assertNull(m.get("TWO"));
            assertEquals(Integer.valueOf(3), m.get("THREE"));
        }

        @Test
        public void replaceKeys_withMergeFunctionStillMergesInEncounterOrder() {
            final Map<String, Integer> m = new LinkedHashMap<>();
            m.put("a1", 10);
            m.put("a2", 20);
            m.put("b1", 30);

            Maps.replaceKeys(m, k -> k.substring(0, 1), Integer::sum);

            assertEquals(Integer.valueOf(30), m.get("a"));
            assertEquals(Integer.valueOf(30), m.get("b"));
            assertEquals(2, m.size());
        }

        @Test
        public void replaceKeys_honoursTheTargetMapsOwnKeySemantics() {
            final java.util.TreeMap<String, Integer> m = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
            m.put("a", 1);
            m.put("B", 2);

            Maps.replaceKeys(m, String::toUpperCase);

            assertEquals(2, m.size());
            assertEquals(Integer.valueOf(1), m.get("a"));
            assertEquals(Integer.valueOf(2), m.get("b"));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // B4 - documented (not changed) divergence between flatten and the path grammar
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class B4_EmptyPathSegmentsAreNotAddressable {

        @Test
        public void flattenKeepsAnEmptySegmentThatGetByPathDrops() {
            final Map<String, Object> inner = new LinkedHashMap<>();
            inner.put("a", 1);
            final Map<String, Object> outer = new LinkedHashMap<>();
            outer.put("", inner);
            outer.put("a", 99);

            // flatten keeps the empty segment ...
            assertEquals(Integer.valueOf(1), Maps.flatten(outer).get(".a"));
            // ... while the path grammar drops it, so ".a" means the top-level "a". Documented, and pinned
            // here so the behaviour cannot drift silently in either direction.
            assertEquals(Integer.valueOf(99), Maps.getByPath(outer, ".a"));
        }

        @Test
        public void leadingTrailingAndRepeatedSeparatorsRemainEquivalent() {
            final Map<String, Object> user = new HashMap<>();
            user.put("name", "John");
            final Map<String, Object> map = new HashMap<>();
            map.put("user", user);

            assertEquals("John", Maps.getByPath(map, "user.name"));
            assertEquals("John", Maps.getByPath(map, ".user.name"));
            assertEquals("John", Maps.getByPath(map, "user..name"));
            assertEquals("John", Maps.getByPath(map, "user.name."));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // D5 - Kryo fallback state is per class AND per copy depth, and is not poisoned by an Error
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class D5_KryoFallback {

        @Test
        public void deepCopyAndShallowCopyStillRoundTripAnOrdinaryBean() {
            final MutableBean bean = new MutableBean();
            bean.setA("hello");
            bean.setB(42);

            final MutableBean deep = Beans.deepCopy(bean);
            assertNotSame(bean, deep);
            assertEquals("hello", deep.getA());
            assertEquals(42, deep.getB());

            final MutableBean shallow = Beans.copy(bean);
            assertNotSame(bean, shallow);
            assertEquals("hello", shallow.getA());
            assertEquals(42, shallow.getB());
        }

        @Test
        public void deepCopyOfNullAndDeepCopyAsOfNullKeepTheirContracts() {
            assertNull(Beans.deepCopy(null));
            assertNotNull(Beans.deepCopyAs(null, MutableBean.class));
            assertThrows(IllegalArgumentException.class, () -> Beans.deepCopyAs(new MutableBean(), null));
        }

        @Test
        public void theFallbackStateIsPerClassAndSplitByCopyDepth() throws Exception {
            // Structural, on purpose: forcing a genuine Kryo failure is version-dependent and would make a
            // flaky test, but the two properties the fix turns on are checkable directly.
            // (1) No shared Set<Class<?>> survives - that set was the classloader leak.
            for (final java.lang.reflect.Field f : Beans.class.getDeclaredFields()) {
                assertFalse("notKryoCompatible".equals(f.getName()), "the leaking Set<Class<?>> of Kryo-incompatible classes must not come back");
            }

            // (2) The replacement is per-class state that dies with the class, and deep/shallow are separate
            // flags - one path's verdict must not disable the other.
            final java.lang.reflect.Field field = Beans.class.getDeclaredField("kryoSupport");
            field.setAccessible(true);
            assertTrue(ClassValue.class.isAssignableFrom(field.getType()), "per-class Kryo state must be a ClassValue so it is collected with the class");

            final ClassValue<?> state = (ClassValue<?>) field.get(null);
            final Object forThisClass = state.get(MutableBean.class);
            assertNotNull(forThisClass);
            assertSame(forThisClass, state.get(MutableBean.class), "state must be stable per class");
            assertNotSame(forThisClass, state.get(RecBean.class), "state must not be shared across classes");

            final List<String> flags = new ArrayList<>();
            for (final java.lang.reflect.Field f : forThisClass.getClass().getDeclaredFields()) {
                if (!f.isSynthetic()) {
                    flags.add(f.getName());
                }
            }
            assertEquals(CommonUtil.asList("deepCopyUnsupported", "shallowCopyUnsupported"), flags, "deep and shallow must be tracked separately");
        }

        @Test
        public void repeatedCopiesOfTheSameClassRemainCorrect() {
            // Exercises the per-class fallback state: whatever it records, the result must not change.
            for (int i = 0; i < 3; i++) {
                final MutableBean bean = new MutableBean();
                bean.setA("v" + i);
                assertEquals("v" + i, Beans.<String> getPropValue(Beans.copy(bean), "a"));
                assertEquals("v" + i, Beans.<String> getPropValue(Beans.deepCopy(bean), "a"));
            }
        }
    }

    // ------------------------------------------------------------------------------------------------
    // J5 - the repeat(<array>, n) family answers an EMPTY array for a null input, now documented
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class J5_RepeatFamilyNullInput {

        @Test
        public void aNullArrayYieldsAnEmptyArrayRatherThanNull() {
            assertEquals(0, Array.repeat((int[]) null, 3).length);
            assertEquals(0, Array.repeat((boolean[]) null, 3).length);
            assertEquals(0, Array.repeat((char[]) null, 3).length);
            assertEquals(0, Array.repeat((byte[]) null, 3).length);
            assertEquals(0, Array.repeat((short[]) null, 3).length);
            assertEquals(0, Array.repeat((long[]) null, 3).length);
            assertEquals(0, Array.repeat((float[]) null, 3).length);
            assertEquals(0, Array.repeat((double[]) null, 3).length);
            assertEquals(0, Array.repeat((String[]) null, 3).length);
            assertEquals(0, Array.repeat((Integer[]) null, 3, Integer.class).length);
        }

        @Test
        public void anEmptyArrayAndANegativeCountKeepTheirContracts() {
            assertEquals(0, Array.repeat(new int[0], 10).length);
            assertThrows(IllegalArgumentException.class, () -> Array.repeat(new int[] { 1 }, -1));
            assertArrayEquals(new int[] { 1, 2, 1, 2 }, Array.repeat(new int[] { 1, 2 }, 2));
        }
    }

    // ------------------------------------------------------------------------------------------------
    // P1 - a getter must still be claimed by exactly one property name
    // ------------------------------------------------------------------------------------------------

    @Nested
    public class P1_GetterClaimedOnce {

        public static class Base {
            private String shared;

            public String getShared() {
                return shared;
            }

            public void setShared(final String shared) {
                this.shared = shared;
            }
        }

        public static class Derived extends Base {
            private int own;

            public int getOwn() {
                return own;
            }

            public void setOwn(final int own) {
                this.own = own;
            }
        }

        @Test
        public void anInheritedGetterAppearsUnderOnePropertyNameOnly() {
            final List<String> names = new ArrayList<>(Beans.getPropNameList(Derived.class));
            assertEquals(CommonUtil.asList("shared", "own"), names);
            assertEquals(1, names.stream().filter("shared"::equals).count());
            assertSame(Beans.getPropGetter(Derived.class, "shared"), Beans.getPropGetters(Derived.class).get("shared"));
        }

        @Test
        public void inheritedPropertiesStillReadAndWrite() {
            final Derived d = new Derived();
            Beans.setPropValue(d, "shared", "s");
            Beans.setPropValue(d, "own", 5);
            assertEquals("s", d.getShared());
            assertEquals(5, d.getOwn());
            assertEquals(CommonUtil.asMap("shared", "s", "own", 5), Beans.beanToMap(d));
        }
    }
}
