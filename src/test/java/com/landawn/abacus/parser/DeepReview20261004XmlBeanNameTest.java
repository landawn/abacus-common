package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;

@Tag("unit")
class DeepReview20261004XmlBeanNameTest extends TestBase {

    static Stream<Arguments> backends() {
        return Stream.of(Arguments.of(false, XmlParserType.StAX), Arguments.of(false, XmlParserType.DOM), Arguments.of(true, XmlParserType.StAX),
                Arguments.of(true, XmlParserType.DOM), Arguments.of(true, XmlParserType.SAX));
    }

    @ParameterizedTest
    @MethodSource("backends")
    void beanNamesAreValidatedOnlyWhenUsedAsElementNames(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = abacus ? new AbacusXmlParserImpl(backend, null, null, Set.of($Bean.class))
                : new XmlParserImpl(backend, null, null, Set.of($Bean.class));
        final $Bean bean = new $Bean();
        bean.setValue(7);

        for (final boolean typeInfo : new boolean[] { false, true }) {
            final XmlSerConfig named = XmlSerConfig.create().setTagByPropertyName(true).setWriteTypeInfo(typeInfo);
            assertThrows(ParsingException.class, () -> parser.serialize(bean, named));
            assertThrows(ParsingException.class, () -> parser.serialize(List.of(bean), named));

            final XmlSerConfig generic = XmlSerConfig.create().setTagByPropertyName(false).setWriteTypeInfo(typeInfo);
            final String xml = parser.serialize(bean, generic);
            assertEquals(7, parser.deserialize(xml, $Bean.class).getValue());
        }
    }

    public static class $Bean {
        private int value;

        public int getValue() {
            return value;
        }

        public void setValue(final int value) {
            this.value = value;
        }
    }
}
