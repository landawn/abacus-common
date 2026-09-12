package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.function.Function;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlCreator;
import com.landawn.abacus.annotation.JsonXmlValue;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.Tuple.Tuple3;

public class SingleValueTypeTest extends TestBase {

    private TestSingleValueType singleValueType;

    public static class BoxedBooleanGetterValue {
        private boolean value;

        public BoxedBooleanGetterValue(final boolean value) {
            this.value = value;
        }

        public Boolean getValue() {
            return value;
        }

        public boolean isFalse() {
            return !value;
        }
    }

    private static class TestSingleValueType extends SingleValueType<TestValue> {
        public TestSingleValueType() {
            super(TestValue.class);
        }
    }

    public static class TestValue {
        public String value;

        public TestValue(String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return value;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            TestValue that = (TestValue) obj;
            return value != null ? value.equals(that.value) : that.value == null;
        }
    }

    public static class FactoryAnnotatedValue {
        private final String value;

        private FactoryAnnotatedValue(String value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static FactoryAnnotatedValue of(String value) {
            return new FactoryAnnotatedValue(value);
        }

        @JsonXmlValue
        public String getValue() {
            return value;
        }
    }

    public static class AnnotatedValue {
        private final String inner;

        private AnnotatedValue(final String inner) {
            this.inner = inner;
        }

        @JsonXmlValue
        public String getValue() {
            return inner;
        }

        @JsonXmlCreator
        public static AnnotatedValue of(final String s) {
            return new AnnotatedValue(s);
        }

        @Override
        public String toString() {
            return inner;
        }
    }

    private static class AnnotatedSingleValueType extends SingleValueType<AnnotatedValue> {
        public AnnotatedSingleValueType() {
            super(AnnotatedValue.class);
        }
    }

    public static class FieldAnnotatedValue {
        @JsonXmlValue
        public final String val;

        private FieldAnnotatedValue(final String val) {
            this.val = val;
        }

        @JsonXmlCreator
        public static FieldAnnotatedValue from(final String s) {
            return new FieldAnnotatedValue(s);
        }
    }

    private static class FieldAnnotatedSingleValueType extends SingleValueType<FieldAnnotatedValue> {
        public FieldAnnotatedSingleValueType() {
            super(FieldAnnotatedValue.class);
        }
    }

    public static class PublicFieldValue {
        public String value;

        public PublicFieldValue(final String value) {
            this.value = value;
        }
    }

    public static class BroaderConstructorValue {
        public String value;

        public BroaderConstructorValue(final CharSequence value) {
            this.value = value.toString();
        }
    }

    public static class BroaderFactoryValue {
        public String value;

        private BroaderFactoryValue(final String value) {
            this.value = value;
        }

        public static BroaderFactoryValue of(final CharSequence value) {
            return new BroaderFactoryValue(value.toString());
        }
    }

    public static class BoxedCreatorValue {
        private final int value;

        private BoxedCreatorValue(final int value) {
            this.value = value;
        }

        @JsonXmlValue
        public int value() {
            return value;
        }

        @JsonXmlCreator
        public static BoxedCreatorValue of(final Integer value) {
            return new BoxedCreatorValue(value);
        }
    }

    public static class InvalidValueMethod {
        @JsonXmlValue
        public String value(final String suffix) {
            return suffix;
        }

        @JsonXmlCreator
        public static InvalidValueMethod of(final String value) {
            return new InvalidValueMethod();
        }
    }

    public static class DuplicateValueMembers {
        @JsonXmlValue
        public String first;

        @JsonXmlValue
        public String second;

        @JsonXmlCreator
        public static DuplicateValueMembers of(final String value) {
            return new DuplicateValueMembers();
        }
    }

    public static class DuplicateCreatorMethods {
        @JsonXmlValue
        public String value() {
            return "value";
        }

        @JsonXmlCreator
        public static DuplicateCreatorMethods first(final String value) {
            return new DuplicateCreatorMethods();
        }

        @JsonXmlCreator
        public static DuplicateCreatorMethods second(final String value) {
            return new DuplicateCreatorMethods();
        }
    }

    public static class StaticValueField {
        @JsonXmlValue
        public static String value;

        @JsonXmlCreator
        public static StaticValueField of(final String value) {
            return new StaticValueField();
        }
    }

    public static class InstanceCreatorMethod {
        @JsonXmlValue
        public String value() {
            return "value";
        }

        @JsonXmlCreator
        public InstanceCreatorMethod create(final String value) {
            return new InstanceCreatorMethod();
        }
    }

    public static class JacksonDisabledCreatorMethod {
        private final String value;

        private JacksonDisabledCreatorMethod(final String value) {
            this.value = value;
        }

        @com.fasterxml.jackson.annotation.JsonValue
        public String value() {
            return value;
        }

        @com.fasterxml.jackson.annotation.JsonCreator
        public static JacksonDisabledCreatorMethod of(final String value) {
            return new JacksonDisabledCreatorMethod(value);
        }

        @com.fasterxml.jackson.annotation.JsonCreator(mode = com.fasterxml.jackson.annotation.JsonCreator.Mode.DISABLED)
        public static JacksonDisabledCreatorMethod disabled(final String value) {
            throw new AssertionError("Disabled creator must not be selected");
        }
    }

    public enum TestEnum {
        A, B, C
    }

    private static class EnumSingleValueType extends SingleValueType<TestEnum> {
        public EnumSingleValueType() {
            super(TestEnum.class);
        }
    }

    public static class UnrelatedStaticMethodValue {
        public String label;

        public static Integer parseSomethingUnrelated(final String s) {
            return s.length();
        }
    }

    public static class ObjectContractMethodValue {
        private String value;

        public ObjectContractMethodValue(final String value) {
            this.value = value;
        }

        @Override
        public String toString() {
            return "ObjectContractMethodValue[value=" + value + "]";
        }
    }

    public static class HashCodeContractMethodValue {
        private int value;

        public HashCodeContractMethodValue(final int value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            return value + 31;
        }
    }

    @BeforeEach
    public void setUp() {
        singleValueType = new TestSingleValueType();
    }

    @Test
    public void testFlagsStringOfAndValueOf() {
        assertEquals(TestValue.class, singleValueType.javaType());
        assertFalse(singleValueType.isParameterizedType());
        assertFalse(singleValueType.isObject());
        assertFalse(new EnumSingleValueType().isObject());
        assertTrue(singleValueType.isSerializable());
        assertTrue(new AnnotatedSingleValueType().isSerializable());
        assertNotNull(singleValueType.parameterTypes());

        assertEquals("test", singleValueType.stringOf(new TestValue("test")));
        assertNull(singleValueType.stringOf(null));
        assertEquals(new TestValue("test"), singleValueType.valueOf("test"));
        assertNull(singleValueType.valueOf(null));

        AnnotatedSingleValueType annotated = new AnnotatedSingleValueType();
        assertEquals("hello", annotated.stringOf(AnnotatedValue.of("hello")));
        assertNull(annotated.stringOf(null));
        AnnotatedValue created = annotated.valueOf("world");
        assertEquals("world", created.getValue());
        assertNull(annotated.valueOf(null));

        FieldAnnotatedSingleValueType fieldType = new FieldAnnotatedSingleValueType();
        assertEquals("fieldval", fieldType.stringOf(FieldAnnotatedValue.from("fieldval")));
        assertEquals("created", fieldType.valueOf("created").val);

        assertEquals("A", new EnumSingleValueType().stringOf(TestEnum.A));

        SingleValueType<BoxedCreatorValue> boxed = new SingleValueType<>(BoxedCreatorValue.class) {
        };
        assertEquals("42", boxed.stringOf(BoxedCreatorValue.of(42)));
        assertEquals(42, boxed.valueOf("42").value());
    }

    @Test
    public void testJdbcGetSet() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn("test");
        when(rs.getString("column")).thenReturn("test");
        assertEquals(new TestValue("test"), singleValueType.get(rs, 1));
        assertEquals(new TestValue("test"), singleValueType.get(rs, "column"));

        AnnotatedSingleValueType annotated = new AnnotatedSingleValueType();
        when(rs.getString(1)).thenReturn("hello");
        when(rs.getString("col")).thenReturn("world");
        assertEquals("hello", annotated.get(rs, 1).getValue());
        assertEquals("world", annotated.get(rs, "col").getValue());

        when(rs.getString(1)).thenReturn(null);
        when(rs.getString("col")).thenReturn(null);
        assertNull(singleValueType.get(rs, 1));
        assertNull(singleValueType.get(rs, "col"));
        assertNull(annotated.get(rs, 1));
        assertNull(annotated.get(rs, "col"));

        SingleValueType<BoxedCreatorValue> primitive = new ObjectType<>(BoxedCreatorValue.class);
        ResultSet primitiveRs = mock(ResultSet.class);
        when(primitiveRs.getInt(1)).thenReturn(0);
        when(primitiveRs.getInt("col")).thenReturn(0);
        when(primitiveRs.wasNull()).thenReturn(true);
        assertNull(primitive.get(primitiveRs, 1));
        assertNull(primitive.get(primitiveRs, "col"));
        when(primitiveRs.wasNull()).thenReturn(false);
        assertEquals(0, primitive.get(primitiveRs, 1).value());
        assertEquals(0, primitive.get(primitiveRs, "col").value());
        when(primitiveRs.getInt(1)).thenReturn(42);
        when(primitiveRs.getInt("col")).thenReturn(42);
        assertEquals(42, primitive.get(primitiveRs, 1).value());
        assertEquals(42, primitive.get(primitiveRs, "col").value());

        PreparedStatement stmt = mock(PreparedStatement.class);
        CallableStatement callable = mock(CallableStatement.class);
        TestValue value = new TestValue("test");
        singleValueType.set(stmt, 1, value);
        singleValueType.set(callable, "param", value);
        singleValueType.set(stmt, 2, value, java.sql.Types.VARCHAR);
        singleValueType.set(callable, "param2", value, java.sql.Types.VARCHAR);
        verify(stmt).setString(1, "test");
        verify(callable).setString("param", "test");
        verify(stmt).setString(2, value.value);
        verify(callable).setString("param2", value.value);

        annotated.set(stmt, 1, null);
        annotated.set(callable, "param", null);
        annotated.set(stmt, 1, null, java.sql.Types.VARCHAR);
        annotated.set(callable, "param", null, java.sql.Types.VARCHAR);
        verify(stmt).setObject(1, null);
        verify(callable).setObject("param", null);
        verify(stmt).setObject(1, null, java.sql.Types.VARCHAR);
        verify(callable).setObject("param", null, java.sql.Types.VARCHAR);
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSerializeTo() throws IOException {
        CharacterWriter writer = createCharacterWriter();
        JsonXmlSerConfig<?> config = mock(JsonXmlSerConfig.class);
        TestValue value = new TestValue("test");
        singleValueType.serializeTo(writer, value, config);
        singleValueType.serializeTo(writer, null, config);
        when(config.getStringQuotation()).thenReturn('"');
        singleValueType.serializeTo(writer, value, config);

        AnnotatedSingleValueType annotated = new AnnotatedSingleValueType();
        annotated.serializeTo(writer, AnnotatedValue.of("hello"), config);
        annotated.serializeTo(writer, null, config);

        java.io.StringWriter output = new java.io.StringWriter();
        BufferedJsonWriter jsonWriter = Objectory.createBufferedJsonWriter(output);
        try {
            singleValueType.serializeTo(jsonWriter, new TestValue("can't"), JsonSerConfig.create().setStringQuotation('\''));
            jsonWriter.flush();
            assertEquals("'can\\'t'", output.toString());
        } finally {
            Objectory.recycle(jsonWriter);
        }
    }

    @Test
    public void testGetCreatorAndValueExtractor() {
        var boxed = SingleValueType.getCreatorAndValueExtractor(BoxedBooleanGetterValue.class);
        for (boolean value : new boolean[] { true, false }) {
            assertEquals(value, boxed._3.apply(new BoxedBooleanGetterValue(value)));
            assertEquals(value, boxed._3.apply(boxed._2.apply(Boolean.toString(value))));
        }

        for (boolean value : new boolean[] { true, false }) {
            org.apache.commons.lang3.mutable.MutableBoolean mutable = new org.apache.commons.lang3.mutable.MutableBoolean(value);
            String json = N.toJson(mutable);
            assertEquals(Boolean.toString(value), json);
            assertEquals(mutable, N.fromJson(json, org.apache.commons.lang3.mutable.MutableBoolean.class));
            assertEquals(value, mutable.booleanValue());
        }

        Tuple3<Type<Object>, Function<String, FactoryAnnotatedValue>, Function<FactoryAnnotatedValue, Object>> factory = SingleValueType
                .getCreatorAndValueExtractor(FactoryAnnotatedValue.class);
        assertNull(factory._1);
        assertNull(factory._2);
        assertNull(factory._3);

        Tuple3<Type<Object>, Function<String, PublicFieldValue>, Function<PublicFieldValue, Object>> publicField = SingleValueType
                .getCreatorAndValueExtractor(PublicFieldValue.class);
        assertEquals(String.class, publicField._1.javaType());
        assertEquals("field-value", publicField._3.apply(publicField._2.apply("field-value")));

        Tuple3<Type<Object>, Function<String, BroaderConstructorValue>, Function<BroaderConstructorValue, Object>> ctor = SingleValueType
                .getCreatorAndValueExtractor(BroaderConstructorValue.class);
        assertEquals(String.class, ctor._1.javaType());
        assertEquals("ctor-value", ctor._3.apply(ctor._2.apply("ctor-value")));

        Tuple3<Type<Object>, Function<String, BroaderFactoryValue>, Function<BroaderFactoryValue, Object>> factoryValue = SingleValueType
                .getCreatorAndValueExtractor(BroaderFactoryValue.class);
        assertEquals(String.class, factoryValue._1.javaType());
        assertEquals("factory-value", factoryValue._3.apply(factoryValue._2.apply("factory-value")));

        Tuple3<Type<Object>, Function<String, UnrelatedStaticMethodValue>, Function<UnrelatedStaticMethodValue, Object>> unrelated = SingleValueType
                .getCreatorAndValueExtractor(UnrelatedStaticMethodValue.class);
        assertNull(unrelated._1);
        assertNull(unrelated._2);
        assertNull(unrelated._3);

        Tuple3<Type<Object>, Function<String, ObjectContractMethodValue>, Function<ObjectContractMethodValue, Object>> objectContract = SingleValueType
                .getCreatorAndValueExtractor(ObjectContractMethodValue.class);
        assertNull(objectContract._1);
        assertNull(objectContract._2);
        assertNull(objectContract._3);

        Tuple3<Type<Object>, Function<String, HashCodeContractMethodValue>, Function<HashCodeContractMethodValue, Object>> hashCode = SingleValueType
                .getCreatorAndValueExtractor(HashCodeContractMethodValue.class);
        assertNull(hashCode._1);
        assertNull(hashCode._2);
        assertNull(hashCode._3);
    }

    @Test
    public void testInvalidAnnotationsRejected() {
        assertThrows(IllegalArgumentException.class, () -> new SingleValueType<>(InvalidValueMethod.class) {
        });
        assertThrows(IllegalArgumentException.class, () -> new SingleValueType<>(DuplicateValueMembers.class) {
        });
        assertThrows(IllegalArgumentException.class, () -> new SingleValueType<>(DuplicateCreatorMethods.class) {
        });
        assertThrows(IllegalArgumentException.class, () -> new SingleValueType<>(StaticValueField.class) {
        });
        assertThrows(IllegalArgumentException.class, () -> new SingleValueType<>(InstanceCreatorMethod.class) {
        });

        SingleValueType<JacksonDisabledCreatorMethod> type = new SingleValueType<>(JacksonDisabledCreatorMethod.class) {
        };
        assertEquals("value", type.valueOf("value").value());
    }

    // ---- review fixes 2026-09-06, T2-02: transient/synthetic fields and unrelated getters are not the value ----

    /** Mirrors java.util.Locale: the only mutable field is a transient cache, plus an unrelated String getter. */
    public static class TransientCacheOnly {
        private transient String cached;

        public TransientCacheOnly(final String s) {
            cached = s;
        }

        public String getLanguage() {
            return "lang";
        }
    }

    /** A real value field next to a transient cache of the SAME type: the cache must not make the class ambiguous. */
    public static class ValueWithTransientCache {
        private int count;
        private transient int cachedDoubled;

        public ValueWithTransientCache(final int count) {
            this.count = count;
            cachedDoubled = count * 2;
        }

        public int getCount() {
            return count;
        }

        public int getCachedDoubled() {
            return cachedDoubled;
        }
    }

    /** Field 'count', but the only String/int getter is unrelated: must be object mode. */
    public static class UnrelatedGetterValue {
        private int count;

        public UnrelatedGetterValue(final int count) {
            this.count = count;
        }

        public int size() {
            return count * 2;
        }
    }

    public static class IsGetterValue {
        private boolean enabled;

        public IsGetterValue(final boolean enabled) {
            this.enabled = enabled;
        }

        public boolean isEnabled() {
            return enabled;
        }
    }

    public static class XxxValueGetterValue {
        private long amount;

        public XxxValueGetterValue(final long amount) {
            this.amount = amount;
        }

        public long amountValue() {
            return amount;
        }
    }

    @Test
    public void reviewFixes20260906_transientOnlyFieldIsNotASingleValue() {
        final var tuple = SingleValueType.getCreatorAndValueExtractor(TransientCacheOnly.class);

        assertNull(tuple._1);
        assertNull(tuple._2);
        assertNull(tuple._3);
    }

    @Test
    public void reviewFixes20260906_transientCacheDoesNotHideTheRealValueField() {
        final var tuple = SingleValueType.getCreatorAndValueExtractor(ValueWithTransientCache.class);

        // Before the fix both int fields matched the int constructor -> two candidates -> object mode (null tuple);
        // now the transient cache is skipped and 'count' with its derived getter is the value.
        assertNotNull(tuple._1);
        assertEquals(int.class, tuple._1.javaType());
        assertEquals(7, tuple._3.apply(new ValueWithTransientCache(7)));
        assertEquals(7, tuple._3.apply(tuple._2.apply("7")));
    }

    @Test
    public void reviewFixes20260906_unrelatedGetterIsNotTheValueAccessor() {
        // size() returns an int, but it is not named after 'count': object mode.
        final var tuple = SingleValueType.getCreatorAndValueExtractor(UnrelatedGetterValue.class);

        assertNull(tuple._1);
        assertNull(tuple._2);
        assertNull(tuple._3);
    }

    @Test
    public void reviewFixes20260906_fieldDerivedGetterNamesAreAccepted() {
        final var isTuple = SingleValueType.getCreatorAndValueExtractor(IsGetterValue.class);
        assertNotNull(isTuple._3);
        assertEquals(true, isTuple._3.apply(new IsGetterValue(true)));
        assertEquals(false, isTuple._3.apply(isTuple._2.apply("false")));

        final var xxxValueTuple = SingleValueType.getCreatorAndValueExtractor(XxxValueGetterValue.class);
        assertNotNull(xxxValueTuple._3);
        assertEquals(42L, xxxValueTuple._3.apply(new XxxValueGetterValue(42L)));
        assertEquals(42L, xxxValueTuple._3.apply(xxxValueTuple._2.apply("42")));
    }

    @Test
    public void reviewFixes20260906_localeAndInetAddressAreObjectModeNotSingleValue() {
        // java.util.Locale's only mutable field is the transient languageTag cache and getLanguage() is unrelated.
        // Before the fix Locale.FRANCE serialized as "fr" (country silently dropped).
        final var tuple = SingleValueType.getCreatorAndValueExtractor(java.util.Locale.class);
        assertNull(tuple._1);
        assertNull(tuple._2);
        assertNull(tuple._3);

        assertTrue(Type.of(java.util.Locale.class).isObject());
        assertEquals("fr_FR", Type.of(java.util.Locale.class).stringOf(java.util.Locale.FRANCE));
        assertTrue(Type.of(java.net.InetAddress.class).isObject());
    }

    public static class LocaleBean {
        private java.util.Locale loc;

        public java.util.Locale getLoc() {
            return loc;
        }

        public void setLoc(final java.util.Locale loc) {
            this.loc = loc;
        }
    }

    @Test
    public void reviewFixes20260906_localeBeanFieldFailsHardInsteadOfLosingTheCountry() {
        // Documented consequence of the fix (SingleValueType class javadoc): an object-mode JDK class in a bean
        // slot is rejected by the JSON serializer instead of being written as a lossy "fr". Replace this pin when a
        // dedicated Locale handler is registered in TypeFactory.
        final LocaleBean bean = new LocaleBean();
        bean.setLoc(java.util.Locale.FRANCE);

        assertThrows(com.landawn.abacus.exception.ParsingException.class, () -> com.landawn.abacus.util.N.toJson(bean));
    }

    // ---- review fixes 2026-09-06, T2-03 / T2-04: annotation pair rules and unwrapped creator exceptions ----

    public static class ThrowingCreatorValue {
        private final String value;

        private ThrowingCreatorValue(final String value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static ThrowingCreatorValue of(final String value) {
            if (value.isEmpty()) {
                throw new IllegalArgumentException("empty not allowed");
            }

            return new ThrowingCreatorValue(value);
        }

        @JsonXmlValue
        public String value() {
            if ("boom".equals(value)) {
                throw new IllegalStateException("accessor failed");
            }

            return value;
        }
    }

    public static class LoneJacksonValueClass {
        private final String value = "v";

        @com.fasterxml.jackson.annotation.JsonValue
        public String value() {
            return value;
        }
    }

    public static class LoneCreatorClass {
        private final String value;

        private LoneCreatorClass(final String value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static LoneCreatorClass of(final String value) {
            return new LoneCreatorClass(value);
        }
    }

    public enum LoneJsonXmlValueEnum {
        A(1);

        @JsonXmlValue
        private final int code;

        LoneJsonXmlValueEnum(final int code) {
            this.code = code;
        }
    }

    @Test
    public void reviewFixes20260906_creatorExceptionSurfacesUnwrapped() {
        final SingleValueType<ThrowingCreatorValue> type = new SingleValueType<>(ThrowingCreatorValue.class) {
        };

        // Before the fix: RuntimeException(InvocationTargetException) with a null message.
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> type.valueOf(""));
        assertEquals("empty not allowed", e.getMessage());

        final IllegalStateException e2 = assertThrows(IllegalStateException.class, () -> type.stringOf(ThrowingCreatorValue.of("boom")));
        assertEquals("accessor failed", e2.getMessage());
    }

    @Test
    public void reviewFixes20260906_lonePairMembersOnNonEnumsStillRejected() {
        // A lone Jackson @JsonValue is tolerated on an ENUM only; a class without a creator cannot be read back.
        assertThrows(IllegalArgumentException.class, () -> new SingleValueType<>(LoneJacksonValueClass.class) {
        });
        // A lone creator is always rejected.
        assertThrows(IllegalArgumentException.class, () -> new SingleValueType<>(LoneCreatorClass.class) {
        });
        // The framework's @JsonXmlValue documents the pair as mandatory, even on an enum.
        assertThrows(IllegalArgumentException.class, () -> Type.of(LoneJsonXmlValueEnum.class));
    }
}
