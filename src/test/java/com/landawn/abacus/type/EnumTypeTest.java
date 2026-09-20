package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.sql.CallableStatement;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.CharacterWriter;

public class EnumTypeTest extends TestBase {

    private enum TestEnum {
        VALUE1, VALUE2, VALUE3
    }

    private enum SingleQuotedNameEnum {
        @JsonXmlField(name = "can't")
        VALUE
    }

    @Test
    @SuppressWarnings("deprecation")
    public void testSerializeSingleQuotedEnumName() {
        final com.landawn.abacus.parser.JsonParser parser = com.landawn.abacus.parser.ParserFactory.createJsonParser();
        final com.landawn.abacus.parser.JsonSerConfig config = com.landawn.abacus.parser.JsonSerConfig.create().setStringQuotation('\'');
        final String encoded = parser.serialize(new SingleQuotedNameEnum[] { SingleQuotedNameEnum.VALUE }, config);
        assertEquals("['can\\'t']", encoded);
        assertEquals(SingleQuotedNameEnum.VALUE, parser.deserialize(encoded, SingleQuotedNameEnum[].class)[0]);
    }

    private enum JsonXmlNullNameEnum {
        @JsonXmlField(name = "null")
        VALUE
    }

    private enum DuplicateJsonXmlNameEnum {
        @JsonXmlField(name = "duplicate")
        FIRST, @JsonXmlField(name = "duplicate")
        SECOND
    }

    private enum JsonXmlNameCollidesWithConstantNameEnum {
        @JsonXmlField(name = "SECOND")
        FIRST, SECOND
    }

    public enum DuplicateCodeEnum {
        FIRST(7), SECOND(7);

        private final int code;

        DuplicateCodeEnum(final int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }
    }

    public enum IntValueEnum {
        A(10), B(20);

        private final int intValue;

        IntValueEnum(final int intValue) {
            this.intValue = intValue;
        }

        public int intValue() {
            return intValue;
        }
    }

    public interface DefaultCode {
        int codeValue();

        default int code() {
            return codeValue();
        }
    }

    public enum InheritedCodeEnum implements DefaultCode {
        LOW(31), HIGH(47);

        private final int code;

        InheritedCodeEnum(final int code) {
            this.code = code;
        }

        @Override
        public int codeValue() {
            return code;
        }
    }

    private EnumType<TestEnum> enumTypeByName;
    private EnumType<TestEnum> enumTypeByOrdinal;
    private CharacterWriter characterWriter;

    @Mock
    private ResultSet resultSet;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private CallableStatement callableStatement;

    @Mock
    private JsonXmlSerConfig<?> config;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        enumTypeByName = (EnumType<TestEnum>) createType(TestEnum.class.getName());
        enumTypeByOrdinal = (EnumType<TestEnum>) createType(TestEnum.class.getName() + "(ORDINAL)");
        characterWriter = createCharacterWriter();
    }

    @Test
    public void testEnumerated() {
        assertEquals(com.landawn.abacus.util.EnumType.NAME, enumTypeByName.enumerated());
        assertEquals(com.landawn.abacus.util.EnumType.ORDINAL, enumTypeByOrdinal.enumerated());
    }

    @Test
    public void testCodeRepresentationRejectsDuplicateCodes() {
        assertThrows(IllegalArgumentException.class, () -> createType(DuplicateCodeEnum.class.getName() + "(CODE)"));
    }

    @Test
    public void testDuplicateJsonXmlNamesAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> createType(DuplicateJsonXmlNameEnum.class.getName()));
        assertThrows(IllegalArgumentException.class, () -> createType(JsonXmlNameCollidesWithConstantNameEnum.class.getName()));
    }

    @Test
    public void testCodeRepresentationFallsBackToIntValue() {
        // Enums that expose their numeric code via intValue() rather than code() (all the numeric enums in
        // com.landawn.abacus.util: Color/Gender/MediaType/OperationType/LockMode/Month/
        // CalendarField/YesNo, plus this IntValueEnum) must still work with CODE representation. Before the
        // intValue() fallback, constructing any of them with "(CODE)" threw at construction time.
        final EnumType<IntValueEnum> type = (EnumType<IntValueEnum>) createType(IntValueEnum.class.getName() + "(CODE)");
        assertEquals(com.landawn.abacus.util.EnumType.CODE, type.enumerated());
        assertEquals(IntValueEnum.A, type.valueOf(10));
        assertEquals(IntValueEnum.B, type.valueOf(20));

        // A real numeric enum from the util package (Color.intValue()); previously threw at construction.
        final EnumType<com.landawn.abacus.util.Color> colorType = (EnumType<com.landawn.abacus.util.Color>) createType(
                com.landawn.abacus.util.Color.class.getName() + "(CODE)");
        assertEquals(com.landawn.abacus.util.EnumType.CODE, colorType.enumerated());
        assertEquals(com.landawn.abacus.util.Color.RED, colorType.valueOf(2));
        assertEquals(com.landawn.abacus.util.Color.PURPLE, colorType.valueOf(8));
    }

    @Test
    public void testCodeRepresentationFindsInheritedInterfaceDefaultCode() {
        // code() is inherited from DefaultCode, so getDeclaredMethod("code") would not find it.
        assertThrows(NoSuchMethodException.class, () -> InheritedCodeEnum.class.getDeclaredMethod("code"));

        final EnumType<InheritedCodeEnum> type = (EnumType<InheritedCodeEnum>) createType(InheritedCodeEnum.class.getName() + "(CODE)");
        assertEquals(com.landawn.abacus.util.EnumType.CODE, type.enumerated());
        assertEquals(InheritedCodeEnum.LOW, type.valueOf(31));
        assertEquals(InheritedCodeEnum.HIGH, type.valueOf(47));
    }

    @Test
    public void testIsSerializable() {
        assertTrue(enumTypeByName.isSerializable());
        assertTrue(enumTypeByOrdinal.isSerializable());
    }

    @Test
    public void testIsImmutable() {
        assertTrue(enumTypeByName.isImmutable());
        assertTrue(enumTypeByOrdinal.isImmutable());
    }

    @Test
    public void testStringOf() {
        assertEquals("VALUE1", enumTypeByName.stringOf(TestEnum.VALUE1));
        assertEquals("VALUE2", enumTypeByName.stringOf(TestEnum.VALUE2));
        assertNull(enumTypeByName.stringOf(null));
    }

    @Test
    public void testJsonXmlNameLiteralNullRoundTrips() {
        EnumType<JsonXmlNullNameEnum> type = (EnumType<JsonXmlNullNameEnum>) createType(JsonXmlNullNameEnum.class.getName());
        assertEquals(JsonXmlNullNameEnum.VALUE, type.valueOf("null"));
    }

    @Test
    public void testValueOfString() {
        assertEquals(TestEnum.VALUE1, enumTypeByName.valueOf("VALUE1"));
        assertEquals(TestEnum.VALUE2, enumTypeByName.valueOf("VALUE2"));

        assertNull(enumTypeByName.valueOf((String) null));
        assertNull(enumTypeByName.valueOf(""));

        assertEquals(TestEnum.VALUE1, enumTypeByName.valueOf("0"));
        assertEquals(TestEnum.VALUE2, enumTypeByName.valueOf("1"));

        assertThrows(IllegalArgumentException.class, () -> enumTypeByName.valueOf("INVALID"));
    }

    @Test
    public void testValueOfInt() {
        assertEquals(TestEnum.VALUE1, enumTypeByName.valueOf(0));
        assertEquals(TestEnum.VALUE2, enumTypeByName.valueOf(1));
        assertEquals(TestEnum.VALUE3, enumTypeByName.valueOf(2));

        assertThrows(IllegalArgumentException.class, () -> enumTypeByName.valueOf(99));

    }

    @Test
    public void testGetByColumnIndexWithName() throws SQLException {
        when(resultSet.getString(1)).thenReturn("VALUE1");
        assertEquals(TestEnum.VALUE1, enumTypeByName.get(resultSet, 1));
        verify(resultSet).getString(1);
    }

    @Test
    public void testGetByColumnIndexWithOrdinal() throws SQLException {
        when(resultSet.getObject(1)).thenReturn(1);
        assertEquals(TestEnum.VALUE2, enumTypeByOrdinal.get(resultSet, 1));
        verify(resultSet).getObject(1);
    }

    @Test
    public void testGetByColumnLabelWithName() throws SQLException {
        when(resultSet.getString("enumColumn")).thenReturn("VALUE2");
        assertEquals(TestEnum.VALUE2, enumTypeByName.get(resultSet, "enumColumn"));
        verify(resultSet).getString("enumColumn");
    }

    @Test
    public void testGetByColumnLabelWithOrdinal() throws SQLException {
        when(resultSet.getObject("enumColumn")).thenReturn("2");
        assertEquals(TestEnum.VALUE3, enumTypeByOrdinal.get(resultSet, "enumColumn"));
        verify(resultSet).getObject("enumColumn");
    }

    @Test
    public void testGetByColumnLabelWithOrdinalNull() throws SQLException {
        when(resultSet.getObject("enumColumn")).thenReturn(null);
        assertNull(enumTypeByOrdinal.get(resultSet, "enumColumn"));
        verify(resultSet).getObject("enumColumn");
    }

    @Test
    public void testSetPreparedStatementWithName() throws SQLException {
        enumTypeByName.set(preparedStatement, 1, TestEnum.VALUE1);
        verify(preparedStatement).setString(1, "VALUE1");

        enumTypeByName.set(preparedStatement, 2, null);
        verify(preparedStatement).setString(2, null);
    }

    @Test
    public void testSetPreparedStatementWithOrdinal() throws SQLException {
        enumTypeByOrdinal.set(preparedStatement, 1, TestEnum.VALUE2);
        verify(preparedStatement).setInt(1, 1);

        enumTypeByOrdinal.set(preparedStatement, 2, null);
        verify(preparedStatement).setNull(2, Types.INTEGER);
    }

    @Test
    public void testSetCallableStatementWithName() throws SQLException {
        enumTypeByName.set(callableStatement, "enumParam", TestEnum.VALUE3);
        verify(callableStatement).setString("enumParam", "VALUE3");

        enumTypeByName.set(callableStatement, "nullParam", null);
        verify(callableStatement).setString("nullParam", null);
    }

    @Test
    public void testSetCallableStatementWithOrdinal() throws SQLException {
        enumTypeByOrdinal.set(callableStatement, "enumParam", TestEnum.VALUE1);
        verify(callableStatement).setInt("enumParam", 0);

        enumTypeByOrdinal.set(callableStatement, "nullParam", null);
        verify(callableStatement).setNull("nullParam", Types.INTEGER);
    }

    @Test
    public void testSerializeTo() throws IOException {
        assertDoesNotThrow(() -> {
            enumTypeByName.serializeTo(characterWriter, null, config);

            enumTypeByName.serializeTo(characterWriter, TestEnum.VALUE1, config);

            enumTypeByOrdinal.serializeTo(characterWriter, TestEnum.VALUE2, config);

            when(config.getStringQuotation()).thenReturn('"');
            enumTypeByName.serializeTo(characterWriter, TestEnum.VALUE3, config);
        });
    }

    // ---- review fixes 2026-09-06, T2-03: an enum with only a Jackson @JsonValue gets a Type ----

    public enum JacksonValueOnly {
        A("a1"), B("b2");

        private final String code;

        JacksonValueOnly(final String code) {
            this.code = code;
        }

        @com.fasterxml.jackson.annotation.JsonValue
        public String getCode() {
            return code;
        }
    }

    public enum JacksonIntValueOnly {
        X(1), Y(2);

        private final int code;

        JacksonIntValueOnly(final int code) {
            this.code = code;
        }

        @com.fasterxml.jackson.annotation.JsonValue
        public int getCode() {
            return code;
        }
    }

    public enum JacksonDuplicateValues {
        A("same"), B("same");

        private final String code;

        JacksonDuplicateValues(final String code) {
            this.code = code;
        }

        @com.fasterxml.jackson.annotation.JsonValue
        public String getCode() {
            return code;
        }
    }

    public enum JacksonNullValued {
        NONE("null"), SOME("s");

        private final String code;

        JacksonNullValued(final String code) {
            this.code = code;
        }

        @com.fasterxml.jackson.annotation.JsonValue
        public String getCode() {
            return code;
        }
    }

    public enum JacksonBoth {
        A("a1"), B("b2");

        private final String code;

        JacksonBoth(final String code) {
            this.code = code;
        }

        @com.fasterxml.jackson.annotation.JsonValue
        public String getCode() {
            return code;
        }

        @com.fasterxml.jackson.annotation.JsonCreator
        public static JacksonBoth from(final String code) {
            for (final JacksonBoth e : values()) {
                if (e.code.equals(code)) {
                    return e;
                }
            }

            throw new IllegalArgumentException("unknown code: " + code);
        }
    }

    public enum JacksonBothNullValued {
        NONE("null"), SOME("s");

        private final String code;

        JacksonBothNullValued(final String code) {
            this.code = code;
        }

        @com.fasterxml.jackson.annotation.JsonValue
        public String getCode() {
            return code;
        }

        @com.fasterxml.jackson.annotation.JsonCreator
        public static JacksonBothNullValued from(final String code) {
            for (final JacksonBothNullValued e : values()) {
                if (e.code.equals(code)) {
                    return e;
                }
            }

            throw new IllegalArgumentException("unknown code: " + code);
        }
    }

    public static class JacksonEnumBean {
        private JacksonValueOnly e;
        private JacksonIntValueOnly i;

        public JacksonValueOnly getE() {
            return e;
        }

        public void setE(final JacksonValueOnly e) {
            this.e = e;
        }

        public JacksonIntValueOnly getI() {
            return i;
        }

        public void setI(final JacksonIntValueOnly i) {
            this.i = i;
        }
    }

    public enum WithBodies {
        X {
            @Override
            String hi() {
                return "x";
            }
        },
        Y {
            @Override
            String hi() {
                return "y";
            }
        };

        abstract String hi();
    }

    private static String serialize(final Type<?> type, final Object value, final JsonXmlSerConfig<?> cfg) throws IOException {
        final java.io.StringWriter output = new java.io.StringWriter();
        final com.landawn.abacus.util.BufferedJsonWriter writer = com.landawn.abacus.util.Objectory.createBufferedJsonWriter(output);

        try {
            ((Type<Object>) type).serializeTo(writer, value, cfg);
            writer.flush();
            return output.toString();
        } finally {
            com.landawn.abacus.util.Objectory.recycle(writer);
        }
    }

    @Test
    public void reviewFixes20260906_loneJsonValueEnumGetsATypeAndWritesTheValue() throws IOException {
        // Before the fix Type.of(...) threw "must be declared as a pair".
        final Type<JacksonValueOnly> type = Type.of(JacksonValueOnly.class);
        final Type<JacksonIntValueOnly> intType = Type.of(JacksonIntValueOnly.class);

        assertEquals("b2", type.stringOf(JacksonValueOnly.B));
        assertNull(type.stringOf(null));
        assertEquals("\"b2\"", serialize(type, JacksonValueOnly.B, com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("2", serialize(intType, JacksonIntValueOnly.Y, com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("null", serialize(type, null, com.landawn.abacus.parser.JsonSerConfig.create()));
        assertEquals("[\"a1\", \"b2\"]", com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asList(JacksonValueOnly.A, JacksonValueOnly.B)));
    }

    @Test
    public void reviewFixes20260906_loneJsonValueEnumReadsThroughTheReverseMap() {
        final Type<JacksonValueOnly> type = Type.of(JacksonValueOnly.class);

        assertEquals(JacksonValueOnly.B, type.valueOf("b2"));
        // Constant name is accepted as a fallback (documented; Jackson itself rejects it).
        assertEquals(JacksonValueOnly.B, type.valueOf("B"));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf(""));
        assertNull(type.valueOf((String) null));
        assertNull(type.valueOf("null"));
        assertEquals(JacksonValueOnly.A, type.valueOf((Object) JacksonValueOnly.A));

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> type.valueOf("zzz"));
        assertTrue(e.getMessage().contains("zzz"));

        final Type<JacksonIntValueOnly> intType = Type.of(JacksonIntValueOnly.class);
        assertEquals(JacksonIntValueOnly.Y, intType.valueOf("2"));
        assertEquals(JacksonIntValueOnly.Y, com.landawn.abacus.util.N.fromJson("[1, 2]", JacksonIntValueOnly[].class)[1]);
    }

    @Test
    public void reviewFixes20260906_loneJsonValueEnumReadsFromResultSet() throws SQLException {
        final EnumType<JacksonValueOnly> type = (EnumType<JacksonValueOnly>) (Type) Type.of(JacksonValueOnly.class);
        final EnumType<JacksonIntValueOnly> intType = (EnumType<JacksonIntValueOnly>) (Type) Type.of(JacksonIntValueOnly.class);

        when(resultSet.getString(1)).thenReturn("a1");
        when(resultSet.getString("col")).thenReturn(null);
        when(resultSet.getString(2)).thenReturn("2");

        // Before the fix (had the pair check been bypassed) this NPE'd on the missing creator.
        assertEquals(JacksonValueOnly.A, type.get(resultSet, 1));
        assertNull(type.get(resultSet, "col"));
        assertEquals(JacksonIntValueOnly.Y, intType.get(resultSet, 2));
    }

    @Test
    public void reviewFixes20260906_loneJsonValueEnumBeanRoundTrip() {
        final JacksonEnumBean bean = new JacksonEnumBean();
        bean.setE(JacksonValueOnly.B);
        bean.setI(JacksonIntValueOnly.X);

        final String json = com.landawn.abacus.util.N.toJson(bean);
        assertEquals("{\"e\": \"b2\", \"i\": 1}", json);

        final JacksonEnumBean back = com.landawn.abacus.util.N.fromJson(json, JacksonEnumBean.class);
        assertEquals(JacksonValueOnly.B, back.getE());
        assertEquals(JacksonIntValueOnly.X, back.getI());

        // A bean with the enum fields left null must serialize too (BeanInfo resolves the field types).
        assertEquals("{}", com.landawn.abacus.util.N.toJson(new JacksonEnumBean()));
    }

    @Test
    public void reviewFixes20260907_loneJsonValueConstantClaimingNullWinsOverTheLiteralNullRule() {
        // R12: the name-based branch lets a constant claiming "null" win (testJsonXmlNameLiteralNullRoundTrips);
        // the creator-less annotated branch checked the literal-null rule FIRST and hid such a constant.
        final Type<JacksonNullValued> type = Type.of(JacksonNullValued.class);

        assertEquals("null", type.stringOf(JacksonNullValued.NONE));
        assertEquals(JacksonNullValued.NONE, type.valueOf("null"));
        assertEquals(JacksonNullValued.SOME, type.valueOf("s"));
        // An enum whose constants do not claim it keeps the literal-null rule.
        assertNull(Type.of(JacksonValueOnly.class).valueOf("null"));
    }

    @Test
    public void reviewFixes20260906_loneJsonValueEnumRejectsDuplicateValues() {
        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> Type.of(JacksonDuplicateValues.class));
        // R12: assert the REASON, not just the type - the pre-fix pair check threw the same exception class here.
        assertTrue(e.getMessage().contains("Duplicate 'JsonValue' value 'same'"), e.getMessage());
        assertTrue(e.getMessage().contains("A") && e.getMessage().contains("B"), e.getMessage());
    }

    @Test
    public void reviewFixes20260906_bothAnnotationsKeepTheCreatorPath() {
        final Type<JacksonBoth> type = Type.of(JacksonBoth.class);

        assertEquals("a1", type.stringOf(JacksonBoth.A));
        assertEquals(JacksonBoth.B, type.valueOf("b2"));
        assertEquals("[\"a1\", \"b2\"]", com.landawn.abacus.util.N.toJson(com.landawn.abacus.util.N.asList(JacksonBoth.A, JacksonBoth.B)));
        // The creator, not the name fallback, decides: "B" is not a code.
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("B"));
    }

    // ---- T2-04: creator exceptions unwrapped, including rejected empty tokens ----

    @Test
    public void reviewFixes20260906_creatorEnumEmptyTokenReachesCreatorAndExceptionIsUnwrapped() {
        final Type<JacksonBoth> type = Type.of(JacksonBoth.class);

        // Before the fix: RuntimeException(InvocationTargetException) for all three.
        assertThrows(IllegalArgumentException.class, () -> type.valueOf(""));
        assertNull(type.valueOf("null"));

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> type.valueOf("zzz"));
        assertEquals("unknown code: zzz", e.getMessage());
    }

    // ---- T2-05: out-of-int-range numeric text is "no such constant", not ArithmeticException ----

    @Test
    public void reviewFixes20260906_outOfIntRangeNumericStringIsIllegalArgument() {
        final Type<java.util.concurrent.TimeUnit> type = Type.of(java.util.concurrent.TimeUnit.class);

        final IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> type.valueOf("99999999999"));
        assertTrue(e.getMessage().contains("99999999999"));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("-99999999999"));
        // In-range signed forms still parse as ordinals.
        assertEquals(java.util.concurrent.TimeUnit.MICROSECONDS, type.valueOf("+1"));
        assertEquals(java.util.concurrent.TimeUnit.NANOSECONDS, type.valueOf("-0"));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("-1"));
    }

    // ---- T2-08: a constant body class resolves to a handler named after (and equal to) the enum ----

    @Test
    public void reviewFixes20260906_constantBodyClassYieldsTheEnumsOwnHandler() {
        final Type<?> fromBody = Type.of(WithBodies.X.getClass());
        final Type<WithBodies> fromEnum = Type.of(WithBodies.class);

        // Before the fix the name was "...EnumTypeTest$WithBodies$1(NAME)" and equals() was false.
        assertEquals(fromEnum.name(), fromBody.name());
        assertFalse(fromBody.name().contains("$1"));
        assertEquals(fromEnum, fromBody);
        assertEquals(WithBodies.class, fromBody.javaType());
        assertEquals("Y", ((Type<Object>) fromBody).stringOf(WithBodies.Y));
        assertEquals(WithBodies.Y, fromBody.valueOf("Y"));
        assertEquals(fromEnum.name(), Type.of(WithBodies.Y.getClass().getName() + "(NAME)").name());
        assertTrue(Type.of(WithBodies.Y.getClass().getName() + "(ORDINAL)").name().endsWith("WithBodies(ORDINAL)"));
    }

    // ---- F118 review fix 2026-09-08: the pair branch's literal-null rule now has an escape hatch ----

    @Test
    public void reviewFixes20260908_pairEnumConstantClaimingNullReachesTheCreator() {
        // The pair branch short-circuited "null" on !hasNull, and hasNull asks for a constant NAMED "null" -
        // impossible for a Java-compiled enum - so the guard could never fire and a constant whose annotated
        // value is "null" was unreachable through valueOf(String), unlike the lone-@JsonValue branch.
        final Type<JacksonBothNullValued> type = Type.of(JacksonBothNullValued.class);

        assertEquals("null", type.stringOf(JacksonBothNullValued.NONE));
        assertEquals(JacksonBothNullValued.NONE, type.valueOf("null"));
        assertEquals(JacksonBothNullValued.SOME, type.valueOf("s"));
        assertThrows(IllegalArgumentException.class, () -> type.valueOf("zzz"));

        // Every other read route funnels through valueOf(String) - valueOf(Object), the char[] overload and the
        // ResultSet reads - so all of them were hiding the constant too, and all of them must agree now.
        assertEquals(JacksonBothNullValued.NONE, type.valueOf((Object) "null"));
        assertEquals(JacksonBothNullValued.NONE, type.valueOf("null".toCharArray(), 0, 4));
        assertEquals("null", type.stringOf(type.valueOf(type.stringOf(JacksonBothNullValued.NONE))));

        // An empty token reaches the creator; a null container value stays null.
        assertThrows(IllegalArgumentException.class, () -> type.valueOf(""));
        assertNull(type.valueOf((String) null));

        // A pair enum whose constants do not claim it keeps the literal-null rule (the creator never sees it).
        assertNull(Type.of(JacksonBoth.class).valueOf("null"));
        // ... and so does a plain, unannotated enum.
        assertNull(Type.of(java.util.concurrent.TimeUnit.class).valueOf("null"));
    }
}
