package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;

public class CharacterArrayXmlRoundTripTest extends TestBase {
    static Stream<XmlParser> parsers() {
        return Stream.of(ParserFactory.createAbacusXmlStAXParser(), ParserFactory.createAbacusXmlDOMParser(), ParserFactory.createAbacusXmlSAXParser(),
                ParserFactory.createXmlStAXParser(), ParserFactory.createXmlDOMParser());
    }

    @ParameterizedTest
    @MethodSource("parsers")
    void characterArraysPreserveDelimitersControlsAndUnicode(XmlParser parser) {
        for (boolean circular : new boolean[] { false, true }) {
            for (boolean emptyBeans : new boolean[] { false, true }) {
                XmlSerConfig config = new XmlSerConfig().setCircularReferenceSupported(circular).setFailOnEmptyBean(!emptyBeans);
                for (char[] chars : new char[][] { null, new char[0], { '\r', '\t', '\n', '"', '\'', '\\', ' ', ',', '<', '>', '&', 0, '\u0001', '\u6C49',
                        '\uD83D', '\uDE00', '\uFFFE', Character.MAX_VALUE } }) {
                    CharacterArrayBean bean = new CharacterArrayBean();
                    bean.setPrimitive(chars);
                    if (chars != null) {
                        Character[] boxed = new Character[chars.length + 1];
                        for (int i = 0; i < chars.length; i++) {
                            boxed[i] = chars[i];
                        }
                        bean.setBoxed(boxed);
                        bean.setCharacters(Arrays.asList(boxed));
                    }
                    bean.setWords(List.of("\uD83D\uDE00", "literal \\uD800", "\uD800", "\uDC00", "\uFFFE\uFFFF"));
                    String xml = parser.serialize(bean, config);
                    assertBean(bean, parser.deserialize(xml, CharacterArrayBean.class));
                    assertBean(bean, parser.deserialize(new StringReader(xml), CharacterArrayBean.class));
                }
            }
        }
    }

    private static void assertBean(CharacterArrayBean expected, CharacterArrayBean actual) {
        assertArrayEquals(expected.getPrimitive(), actual.getPrimitive());
        assertArrayEquals(expected.getBoxed(), actual.getBoxed());
        assertEquals(expected.getCharacters(), actual.getCharacters());
        assertEquals(expected.getWords(), actual.getWords());
    }

    public static class CharacterArrayBean {
        private char[] primitive;
        private Character[] boxed;
        private List<Character> characters;
        private List<String> words;

        public char[] getPrimitive() {
            return primitive;
        }

        public void setPrimitive(char[] primitive) {
            this.primitive = primitive;
        }

        public Character[] getBoxed() {
            return boxed;
        }

        public void setBoxed(Character[] boxed) {
            this.boxed = boxed;
        }

        public List<Character> getCharacters() {
            return characters;
        }

        public void setCharacters(List<Character> characters) {
            this.characters = characters;
        }

        public List<String> getWords() {
            return words;
        }

        public void setWords(List<String> words) {
            this.words = words;
        }
    }
}
