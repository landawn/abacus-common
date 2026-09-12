package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;
import com.landawn.abacus.util.MapEntity;

public class ParserWhitespaceTest extends TestBase {
    public static class TextBean {
        private String text;
        private TextBean child;
        private List<TextBean> children;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public TextBean getChild() {
            return child;
        }

        public void setChild(TextBean child) {
            this.child = child;
        }

        public List<TextBean> getChildren() {
            return children;
        }

        public void setChildren(List<TextBean> children) {
            this.children = children;
        }
    }

    private List<XmlParser> streamParsers() {
        return List.of(ParserFactory.createXmlParser(), ParserFactory.createAbacusXmlParser());
    }

    @Test
    public void scalarWhitespaceAndCdataMatchAllSupportedBackends() {
        for (XmlParser parser : List.of(ParserFactory.createXmlParser(), ParserFactory.createAbacusXmlParser(), ParserFactory.createXmlDOMParser(),
                ParserFactory.createAbacusXmlDOMParser(), ParserFactory.createAbacusXmlSAXParser())) {
            for (String text : List.of(" ", " \t\n ", "\u2003\u00a0", "\u540d\ud83d\ude00 ", "a b", " leading ")) {
                String xml = "<TextBean><text>" + text + "</text></TextBean>";
                assertEquals(text, parser.deserialize(xml, TextBean.class).getText(), parser.getClass().getName());
                assertEquals(text, parser.deserialize(new StringReader(xml), TextBean.class).getText());
                assertEquals(text, parser.deserialize(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), TextBean.class).getText());
            }
            String xml = "<TextBean><text>a<![CDATA[ ]]><!-- split --><![CDATA[\t]]>\u540d\ud83d\ude00<![CDATA[ ]]></text></TextBean>";
            assertEquals("a \t\u540d\ud83d\ude00 ", parser.deserialize(xml, TextBean.class).getText());
        }
    }

    @Test
    public void prettyNestedBeansKeepWhitespaceAndFollowingSiblings() {
        String xml = "<TextBean>\n <child>\n <TextBean><text> </text></TextBean>\n </child>\n <text>after</text>\n</TextBean>";
        for (XmlParser parser : streamParsers()) {
            TextBean result = parser.deserialize(xml, TextBean.class);
            assertEquals(" ", result.getChild().getText());
            assertEquals("after", result.getText());
            result = parser.deserialize(new StringReader(xml), TextBean.class);
            assertEquals(" ", result.getChild().getText());
            assertEquals("after", result.getText());
        }
        XmlParser parser = ParserFactory.createAbacusXmlParser();
        TextBean parent = new TextBean();
        TextBean child = new TextBean();
        child.setText(" ");
        parent.setChild(child);
        parent.setText("after");
        String attributes = parser.serialize(parent, XmlSerConfig.create().setPrettyFormat(true).setTagByPropertyName(false));
        TextBean result = parser.deserialize(attributes, TextBean.class);
        assertEquals(" ", result.getChild().getText());
        assertEquals("after", result.getText());
    }

    @Test
    public void compactAndPrettyCollectionRoundTripsKeepEveryItem() {
        for (XmlParser parser : streamParsers()) {
            for (boolean pretty : List.of(false, true)) {
                XmlSerConfig config = XmlSerConfig.create().setPrettyFormat(pretty);
                List<String> words = Arrays.asList(" ", "", null, "\u540d\ud83d\ude00 ", "a b");
                assertEquals(words, parser.deserialize(parser.serialize(words, config), Type.of("List<String>")));
                assertArrayEquals(words.toArray(new String[0]), parser.deserialize(parser.serialize(words.toArray(new String[0]), config), String[].class));
                assertEquals(List.of(1, 2, 3), parser.deserialize(parser.serialize(List.of(1, 2, 3), config), Type.of("List<Integer>")));
                TextBean first = new TextBean();
                first.setText(" ");
                TextBean last = new TextBean();
                last.setText("tail");
                List<TextBean> beans = Arrays.asList(first, null, last);
                String xml = parser.serialize(beans, config);
                List<TextBean> result = parser.deserialize(xml, Type.of("List<" + TextBean.class.getName() + ">"));
                assertEquals(3, result.size(), xml);
                assertEquals(" ", result.get(0).getText());
                assertNull(result.get(1));
                assertEquals("tail", result.get(2).getText());
                TextBean[] array = parser.deserialize(parser.serialize(beans.toArray(new TextBean[0]), config), TextBean[].class);
                assertEquals(3, array.length);
                assertEquals(" ", array[0].getText());
                assertNull(array[1]);
                assertEquals("tail", array[2].getText());
                TextBean parent = new TextBean();
                parent.setChildren(beans);
                parent.setText("after");
                TextBean restored = parser.deserialize(parser.serialize(parent, config), TextBean.class);
                assertEquals(3, restored.getChildren().size());
                assertEquals(" ", restored.getChildren().get(0).getText());
                assertEquals("tail", restored.getChildren().get(2).getText());
                assertEquals("after", restored.getText());
            }
        }
    }

    @Test
    public void mapValuesPreserveScalarWhitespace() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("first", " ");
        values.put("unicode", "\u540d\ud83d\ude00 ");
        values.put("last", "tail");
        for (XmlParser parser : streamParsers()) {
            for (boolean pretty : List.of(false, true)) {
                String xml = parser.serialize(values, XmlSerConfig.create().setPrettyFormat(pretty));
                assertEquals(values, parser.deserialize(xml, Type.of("Map<String,String>")), xml);
                assertEquals(values, parser.deserialize(new StringReader(xml), Type.of("Map<String,String>")), xml);
            }
        }
    }

    @Test
    public void abacusIndentedWrappersDistinguishScalarTextFromNestedValues() {
        XmlParser parser = ParserFactory.createAbacusXmlParser();
        String xml = "<map>\n <entry>\n <key> </key>\n <value> <![CDATA[ ]]></value>\n </entry>\n"
                + "<entry><key>nested</key><value>\n <list>\n <e> </e>\n <e>tail</e>\n </list>\n </value></entry>\n</map>";
        Map<String, Object> expected = Map.of(" ", "  ", "nested", List.of(" ", "tail"));
        assertEquals(expected, parser.deserialize(xml, Type.of("Map<String,Object>")));
        assertEquals(expected, parser.deserialize(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)), Type.of("Map<String,Object>")));
        String array = "<array>\n <e> </e>\n <e></e>\n <e isNull=\"true\"/>\n <e>tail</e>\n </array>";
        assertArrayEquals(new String[] { " ", "", null, "tail" }, parser.deserialize(array, String[].class));
        TextBean key = new TextBean();
        key.setText(" ");
        Type<Map<TextBean, List<String>>> mapType = Type.of("Map<" + TextBean.class.getName() + ",List<String>>");
        String nestedKey = parser.serialize(Map.of(key, List.of(" ", "tail")), XmlSerConfig.create().setPrettyFormat(true));
        Map<TextBean, List<String>> result = parser.deserialize(nestedKey, mapType);
        assertEquals(1, result.size());
        assertEquals(" ", result.keySet().iterator().next().getText());
        assertEquals(List.of(" ", "tail"), result.values().iterator().next());
    }

    @Test
    public void ignoredIndentedValuesDoNotConsumeFollowingProperties() {
        String xml = "<TextBean>\n <child>\n <TextBean><text>ignored</text></TextBean>\n </child>\n <text> </text>\n</TextBean>";
        for (XmlParser parser : streamParsers()) {
            TextBean result = parser.deserialize(xml, XmlDeserConfig.create().setIgnoredPropNames(Set.of("child")), TextBean.class);
            assertNull(result.getChild());
            assertEquals(" ", result.getText());
        }
        String map = "<map>\n <entry>\n <key>skip</key>\n <value>\n <list><e>x</e></list>\n </value>\n </entry>\n"
                + "<entry>\n <key>tail</key>\n <value> </value>\n </entry>\n</map>";
        assertEquals(Map.of("tail", " "), ParserFactory.createAbacusXmlParser()
                .deserialize(map, XmlDeserConfig.create().setIgnoredPropNames(Set.of("skip")), Type.of("Map<String,String>")));
    }

    @Test
    public void textSurvivesReaderAndByteBufferBoundaries() {
        String text = "\u540d\ud83d\ude00 ".repeat(3000);
        String xml = "<TextBean><text>" + text + "<![CDATA[ ]]></text></TextBean>";
        for (XmlParser parser : streamParsers()) {
            StringReader reader = new StringReader(xml) {
                @Override
                public int read(char[] buffer, int offset, int length) throws java.io.IOException {
                    return super.read(buffer, offset, Math.min(3, length));
                }
            };
            ByteArrayInputStream input = new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)) {
                @Override
                public synchronized int read(byte[] buffer, int offset, int length) {
                    return super.read(buffer, offset, Math.min(3, length));
                }
            };
            assertEquals(text + " ", parser.deserialize(reader, TextBean.class).getText());
            assertEquals(text + " ", parser.deserialize(input, TextBean.class).getText());
        }
    }

    @Test
    public void standardMapAndMapEntityNestedCollectionsKeepEverySibling() {
        XmlParser parser = ParserFactory.createXmlParser();
        String xml = "<map>\n <children>\n <TextBean><text> </text></TextBean>\n"
                + "<TextBean><text>tail</text></TextBean>\n </children>\n <text>after</text>\n </map>";
        XmlDeserConfig config = XmlDeserConfig.create().setValueType("children", Type.of("List<" + TextBean.class.getName() + ">"));
        Map<?, ?> map = parser.deserialize(xml, config, Map.class);
        List<?> children = (List<?>) map.get("children");
        assertEquals(2, children.size());
        assertEquals(" ", ((TextBean) children.get(0)).getText());
        assertEquals("tail", ((TextBean) children.get(1)).getText());
        assertEquals("after", map.get("text"));
        MapEntity entity = parser.deserialize(xml, config, MapEntity.class);
        List<TextBean> entityChildren = entity.get("children");
        assertEquals(2, entityChildren.size());
        assertEquals(" ", entityChildren.get(0).getText());
        assertEquals("tail", entityChildren.get(1).getText());
        assertEquals("after", entity.get("text"));
    }

    @Test
    public void existingEmptyAndNullConventionsRemainStable() {
        for (XmlParser parser : streamParsers()) {
            String empty = parser.deserialize("<TextBean><text/></TextBean>", TextBean.class).getText();
            if (parser instanceof XmlParserImpl) {
                assertNull(empty);
            } else {
                assertEquals("", empty);
            }
            assertNull(parser.deserialize("<TextBean><text isNull=\"true\"/></TextBean>", TextBean.class).getText());
            assertNull(parser.deserialize("<TextBean/>", TextBean.class).getText());
        }
        XmlParser parser = ParserFactory.createXmlParser();
        String items = "<TextBean isNull=\"true\"> \n </TextBean>\n <TextBean><text>tail</text></TextBean>";
        Type<List<TextBean>> listType = Type.of("List<" + TextBean.class.getName() + ">");
        List<TextBean> list = parser.deserialize("<list>" + items + "</list>", listType);
        assertEquals(2, list.size());
        assertNull(list.get(0));
        assertEquals("tail", list.get(1).getText());
        TextBean[] array = parser.deserialize("<array>" + items + "</array>", TextBean[].class);
        assertEquals(2, array.length);
        assertNull(array[0]);
        assertEquals("tail", array[1].getText());
        TextBean bean = parser.deserialize("<TextBean><children>" + items + "</children></TextBean>", TextBean.class);
        assertEquals(2, bean.getChildren().size());
        assertNull(bean.getChildren().get(0));
        assertEquals("tail", bean.getChildren().get(1).getText());
    }
}
