package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.u;

public class XmlOptionalScalarContractTest extends TestBase {
    static java.util.stream.Stream<Arguments> wrappers() {
        return java.util.stream.Stream.of(Arguments.of(u.Optional.of("a<&b"), u.Optional.empty(), "a<&b", Type.of("Optional<String>")),
                Arguments.of(u.Nullable.of("a<&b"), u.Nullable.empty(), "a<&b", Type.of("Nullable<String>")),
                Arguments.of(java.util.Optional.of("a<&b"), java.util.Optional.empty(), "a<&b", Type.of("java.util.Optional<String>")),
                Arguments.of(u.OptionalBoolean.of(true), u.OptionalBoolean.empty(), true, Type.of(u.OptionalBoolean.class)),
                Arguments.of(u.OptionalChar.of('&'), u.OptionalChar.empty(), '&', Type.of(u.OptionalChar.class)),
                Arguments.of(u.OptionalByte.of((byte) 7), u.OptionalByte.empty(), (byte) 7, Type.of(u.OptionalByte.class)),
                Arguments.of(u.OptionalShort.of((short) 7), u.OptionalShort.empty(), (short) 7, Type.of(u.OptionalShort.class)),
                Arguments.of(u.OptionalInt.of(7), u.OptionalInt.empty(), 7, Type.of(u.OptionalInt.class)),
                Arguments.of(u.OptionalLong.of(7L), u.OptionalLong.empty(), 7L, Type.of(u.OptionalLong.class)),
                Arguments.of(u.OptionalFloat.of(1.25F), u.OptionalFloat.empty(), 1.25F, Type.of(u.OptionalFloat.class)),
                Arguments.of(u.OptionalDouble.of(1.25D), u.OptionalDouble.empty(), 1.25D, Type.of(u.OptionalDouble.class)),
                Arguments.of(java.util.OptionalInt.of(7), java.util.OptionalInt.empty(), 7, Type.of(java.util.OptionalInt.class)),
                Arguments.of(java.util.OptionalLong.of(7L), java.util.OptionalLong.empty(), 7L, Type.of(java.util.OptionalLong.class)),
                Arguments.of(java.util.OptionalDouble.of(1.25D), java.util.OptionalDouble.empty(), 1.25D, Type.of(java.util.OptionalDouble.class)));
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("wrappers")
    void unwrappingPreservesScalarTypeAndAbsence(Object present, Object empty, Object scalar, Type<?> type) {
        assertEquals(scalar, AbstractXmlParser.unwrapOptional(present));
        assertNull(AbstractXmlParser.unwrapOptional(empty));
    }

    @ParameterizedTest(name = "{3}")
    @MethodSource("wrappers")
    void presentScalarWrappersRoundTripAcrossXmlReaders(Object present, Object empty, Object scalar, Type<?> type) {
        for (XmlParserType backend : XmlParserType.values()) {
            List<XmlParser> parsers = backend == XmlParserType.SAX ? List.of(new AbacusXmlParserImpl(backend))
                    : List.of(new XmlParserImpl(backend), new AbacusXmlParserImpl(backend));
            for (XmlParser parser : parsers) {
                for (boolean typeInfo : new boolean[] { false, true }) {
                    Map<String, Object> values = Map.of("value", present);
                    String xml = parser.serialize(values, XmlSerConfig.create().setWriteTypeInfo(typeInfo));
                    assertEquals(values, parser.deserialize(xml, Type.of("Map<String, " + type.name() + ">")), backend + ": " + xml);
                }
            }
        }
    }

    @Test
    void presentNullIsRejectedWhileUnwrappedValuesKeepTheirIdentity() {
        assertThrows(ParsingException.class, () -> AbstractXmlParser.unwrapOptional(u.Nullable.of(null)));
        Object scalar = new Object();
        assertSame(scalar, AbstractXmlParser.unwrapOptional(scalar));
        assertNull(AbstractXmlParser.unwrapOptional(null));
    }
}
