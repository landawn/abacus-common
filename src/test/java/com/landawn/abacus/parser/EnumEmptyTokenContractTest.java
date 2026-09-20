package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlCreator;
import com.landawn.abacus.annotation.JsonXmlValue;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.CsvUtil;
import com.landawn.abacus.util.Dataset;
import com.landawn.abacus.util.N;

/** Empty text is an enum codec token in every format, independently of an explicit null. */
public class EnumEmptyTokenContractTest extends TestBase {
    private static final List<String> REJECTING_PROPERTIES = List.of("valueOnly", "created", "xmlCreated");

    @Test
    public void jsonPropertiesDistinguishRejectedEmptyTokensAcceptedEmptyTokensAndNulls() {
        for (final String property : REJECTING_PROPERTIES) {
            assertThrows(IllegalArgumentException.class, () -> N.fromJson("{\"" + property + "\":\"\",\"name\":\"n\"}", EnumBean.class), property);
        }
        final EnumBean nulls = N.fromJson("{\"valueOnly\":null,\"created\":null,\"xmlCreated\":null,\"name\":\"n\"}", EnumBean.class);
        assertNull(nulls.getValueOnly());
        assertNull(nulls.getCreated());
        assertNull(nulls.getXmlCreated());
        assertEquals("n", nulls.getName());
        final EnumBean empty = N.fromJson("{\"emptyValue\":\"\",\"emptyCreated\":\"\",\"plain\":\"\",\"name\":\"n\"}", EnumBean.class);
        assertSame(EmptyValue.EMPTY, empty.getEmptyValue());
        assertSame(EmptyCreator.EMPTY, empty.getEmptyCreated());
        assertNull(empty.getPlain());
        assertEquals("n", empty.getName());
    }

    @Test
    public void xmlEmptyElementsReachAnnotatedCodecsOnEveryBackend() {
        for (final AbstractXmlParser parser : xmlParsers()) {
            for (final String property : REJECTING_PROPERTIES) {
                for (final String element : List.of("<" + property + "/>", "<" + property + "></" + property + ">")) {
                    final String xml = "<enumBean>" + element + "<name>n</name></enumBean>";
                    assertThrows(IllegalArgumentException.class, () -> parser.deserialize(xml, EnumBean.class), xml);
                }
            }
            final EnumBean empty = parser.deserialize("<enumBean><emptyValue/><emptyCreated></emptyCreated><plain/><name>n</name></enumBean>", EnumBean.class);
            assertSame(EmptyValue.EMPTY, empty.getEmptyValue());
            assertSame(EmptyCreator.EMPTY, empty.getEmptyCreated());
            assertNull(empty.getPlain());
            assertEquals("n", empty.getName());
        }
    }

    @Test
    public void xmlNullMarkersBypassEnumTokenConversionOnEveryBackend() {
        for (final AbstractXmlParser parser : xmlParsers()) {
            for (final String text : List.of("", "invalid")) {
                final StringBuilder xml = new StringBuilder("<enumBean>");
                for (final String property : List.of("valueOnly", "created", "xmlCreated", "emptyValue", "emptyCreated", "plain")) {
                    xml.append('<').append(property).append(" isNull='true'>").append(text).append("</").append(property).append('>');
                }
                final EnumBean nulls = parser.deserialize(xml.append("<name>n</name></enumBean>").toString(), EnumBean.class);
                assertNull(nulls.getValueOnly());
                assertNull(nulls.getCreated());
                assertNull(nulls.getXmlCreated());
                assertNull(nulls.getEmptyValue());
                assertNull(nulls.getEmptyCreated());
                assertNull(nulls.getPlain());
                assertEquals("n", nulls.getName());
            }
        }
    }

    @Test
    public void typedCsvFieldsUseTheSameEmptyTokenPolicy() {
        for (final String property : REJECTING_PROPERTIES) {
            assertThrows(IllegalArgumentException.class, () -> CsvUtil.load(new StringReader(property + ",name\n\"\",n\n"), EnumBean.class), property);
        }
        final Dataset values = CsvUtil.load(new StringReader("emptyValue,emptyCreated,plain,name\n\"\",\"\",\"\",n\n"), EnumBean.class);
        assertEquals(1, values.size());
        assertSame(EmptyValue.EMPTY, values.row(0).get("emptyValue"));
        assertSame(EmptyCreator.EMPTY, values.row(0).get("emptyCreated"));
        assertNull(values.row(0).get("plain"));
        assertEquals("n", values.row(0).get("name"));
    }

    @Test
    public void jdbcEmptyStringsRemainDistinctFromSqlNull() throws Exception {
        final ResultSet result = mock(ResultSet.class);
        final List<Type<?>> rejecting = List.of(Type.of(ValueToken.class), Type.of(CreatorToken.class), Type.of(XmlCreatorToken.class));
        when(result.getString(1)).thenReturn("");
        when(result.getString("token")).thenReturn("");
        for (final Type<?> type : rejecting) {
            assertThrows(IllegalArgumentException.class, () -> type.get(result, 1));
            assertThrows(IllegalArgumentException.class, () -> type.get(result, "token"));
        }
        assertSame(EmptyValue.EMPTY, Type.of(EmptyValue.class).get(result, 1));
        assertSame(EmptyCreator.EMPTY, Type.of(EmptyCreator.class).get(result, "token"));
        assertNull(Type.of(PlainToken.class).get(result, 1));

        when(result.getString(1)).thenReturn(null);
        when(result.getString("token")).thenReturn(null);
        for (final Type<?> type : List.of(Type.of(ValueToken.class), Type.of(CreatorToken.class), Type.of(XmlCreatorToken.class), Type.of(EmptyValue.class),
                Type.of(EmptyCreator.class), Type.of(PlainToken.class))) {
            assertNull(type.get(result, 1));
            assertNull(type.get(result, "token"));
        }
    }

    private static List<AbstractXmlParser> xmlParsers() {
        final Set<Class<?>> approved = Set.of(EnumBean.class, ValueToken.class, CreatorToken.class, XmlCreatorToken.class, EmptyValue.class, EmptyCreator.class,
                PlainToken.class);
        final List<AbstractXmlParser> parsers = new ArrayList<>();
        for (final XmlParserType backend : XmlParserType.values()) {
            parsers.add(new AbacusXmlParserImpl(backend, null, null, approved));
            if (backend != XmlParserType.SAX) {
                parsers.add(new XmlParserImpl(backend, null, null, approved));
            }
        }
        return parsers;
    }

    public enum ValueToken {
        VALUE;

        @JsonValue
        public String token() {
            return "value";
        }
    }

    public enum CreatorToken {
        VALUE;

        @JsonValue
        public String token() {
            return "value";
        }

        @JsonCreator
        public static CreatorToken from(final String token) {
            if ("value".equals(token))
                return VALUE;
            throw new IllegalArgumentException("Unsupported token: " + token);
        }
    }

    public enum XmlCreatorToken {
        VALUE;

        @JsonXmlValue
        public String token() {
            return "value";
        }

        @JsonXmlCreator
        public static XmlCreatorToken from(final String token) {
            if ("value".equals(token))
                return VALUE;
            throw new IllegalArgumentException("Unsupported token: " + token);
        }
    }

    public enum EmptyValue {
        EMPTY;

        @JsonValue
        public String token() {
            return "";
        }
    }

    public enum EmptyCreator {
        EMPTY;

        @JsonValue
        public String token() {
            return "";
        }

        @JsonCreator
        public static EmptyCreator from(final String token) {
            if ("".equals(token))
                return EMPTY;
            throw new IllegalArgumentException("Unsupported token: " + token);
        }
    }

    public enum PlainToken {
        VALUE
    }

    public static class EnumBean {
        private ValueToken valueOnly = ValueToken.VALUE;
        private CreatorToken created = CreatorToken.VALUE;
        private XmlCreatorToken xmlCreated = XmlCreatorToken.VALUE;
        private EmptyValue emptyValue = EmptyValue.EMPTY;
        private EmptyCreator emptyCreated = EmptyCreator.EMPTY;
        private PlainToken plain = PlainToken.VALUE;
        private String name;

        public ValueToken getValueOnly() {
            return valueOnly;
        }

        public void setValueOnly(final ValueToken value) {
            valueOnly = value;
        }

        public CreatorToken getCreated() {
            return created;
        }

        public void setCreated(final CreatorToken value) {
            created = value;
        }

        public XmlCreatorToken getXmlCreated() {
            return xmlCreated;
        }

        public void setXmlCreated(final XmlCreatorToken value) {
            xmlCreated = value;
        }

        public EmptyValue getEmptyValue() {
            return emptyValue;
        }

        public void setEmptyValue(final EmptyValue value) {
            emptyValue = value;
        }

        public EmptyCreator getEmptyCreated() {
            return emptyCreated;
        }

        public void setEmptyCreated(final EmptyCreator value) {
            emptyCreated = value;
        }

        public PlainToken getPlain() {
            return plain;
        }

        public void setPlain(final PlainToken value) {
            plain = value;
        }

        public String getName() {
            return name;
        }

        public void setName(final String value) {
            name = value;
        }
    }
}
