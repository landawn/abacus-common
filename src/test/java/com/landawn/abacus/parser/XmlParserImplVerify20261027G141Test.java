package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.annotation.JsonXmlField.Direction;
import com.landawn.abacus.exception.ParsingException;

/**
 * Verification of the StAX readers' handling of mixed content in a SERIALIZE_ONLY property (XmlParserImpl and
 * AbacusXmlParserImpl): the property is skipped whole, exactly as an ignored property is.
 */
public class XmlParserImplVerify20261027G141Test extends TestBase {

    // ---- deep review 2026-10-27 verify G141 begin ----

    public static class G141VKid {
        private String v;

        public String getV() {
            return v;
        }

        public void setV(final String v) {
            this.v = v;
        }

        @Override
        public String toString() {
            return "{v=" + v + "}";
        }
    }

    public static class G141VBean {
        private String name;
        private int num;
        @JsonXmlField(direction = Direction.SERIALIZE_ONLY)
        private String soStr;
        @JsonXmlField(direction = Direction.SERIALIZE_ONLY)
        private int soInt;
        @JsonXmlField(direction = Direction.SERIALIZE_ONLY)
        private List<String> soList;
        @JsonXmlField(direction = Direction.SERIALIZE_ONLY)
        private String[] soArr;
        @JsonXmlField(direction = Direction.SERIALIZE_ONLY)
        private Map<String, String> soMap;
        @JsonXmlField(direction = Direction.SERIALIZE_ONLY)
        private Optional<String> soOpt;
        @JsonXmlField(direction = Direction.SERIALIZE_ONLY)
        private Object soObj;
        @JsonXmlField(direction = Direction.SERIALIZE_ONLY)
        private G141VKid soBean;
        @JsonXmlField(direction = Direction.SERIALIZE_ONLY)
        private List<G141VKid> soKids;
        private List<String> afterList;
        private G141VKid afterBean;
        private String after;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getNum() {
            return num;
        }

        public void setNum(final int num) {
            this.num = num;
        }

        public String getSoStr() {
            return soStr;
        }

        public void setSoStr(final String soStr) {
            this.soStr = soStr;
        }

        public int getSoInt() {
            return soInt;
        }

        public void setSoInt(final int soInt) {
            this.soInt = soInt;
        }

        public List<String> getSoList() {
            return soList;
        }

        public void setSoList(final List<String> soList) {
            this.soList = soList;
        }

        public String[] getSoArr() {
            return soArr;
        }

        public void setSoArr(final String[] soArr) {
            this.soArr = soArr;
        }

        public Map<String, String> getSoMap() {
            return soMap;
        }

        public void setSoMap(final Map<String, String> soMap) {
            this.soMap = soMap;
        }

        public Optional<String> getSoOpt() {
            return soOpt;
        }

        public void setSoOpt(final Optional<String> soOpt) {
            this.soOpt = soOpt;
        }

        public Object getSoObj() {
            return soObj;
        }

        public void setSoObj(final Object soObj) {
            this.soObj = soObj;
        }

        public G141VKid getSoBean() {
            return soBean;
        }

        public void setSoBean(final G141VKid soBean) {
            this.soBean = soBean;
        }

        public List<G141VKid> getSoKids() {
            return soKids;
        }

        public void setSoKids(final List<G141VKid> soKids) {
            this.soKids = soKids;
        }

        public List<String> getAfterList() {
            return afterList;
        }

        public void setAfterList(final List<String> afterList) {
            this.afterList = afterList;
        }

        public G141VKid getAfterBean() {
            return afterBean;
        }

        public void setAfterBean(final G141VKid afterBean) {
            this.afterBean = afterBean;
        }

        public String getAfter() {
            return after;
        }

        public void setAfter(final String after) {
            this.after = after;
        }

        @Override
        public String toString() {
            return "{name=" + name + ", num=" + num + ", so=[" + soStr + ", " + soInt + ", " + soList + ", " + (soArr == null ? null : Arrays.toString(soArr))
                    + ", " + soMap + ", " + soOpt + ", " + soObj + ", " + soBean + ", " + soKids + "], afterList=" + afterList + ", afterBean=" + afterBean
                    + ", after=" + after + "}";
        }
    }

    public static class G141VOuter {
        private G141VBean inner;
        private List<G141VBean> inners;
        private String tail;

        public G141VBean getInner() {
            return inner;
        }

        public void setInner(final G141VBean inner) {
            this.inner = inner;
        }

        public List<G141VBean> getInners() {
            return inners;
        }

        public void setInners(final List<G141VBean> inners) {
            this.inners = inners;
        }

        public String getTail() {
            return tail;
        }

        public void setTail(final String tail) {
            this.tail = tail;
        }
    }

    private static final String NO_SO = "so=[null, 0, null, null, null, null, null, null, null]";

    private static final String[] SERIALIZE_ONLY_SLOTS = { "soStr", "soInt", "soList", "soArr", "soMap", "soOpt", "soObj", "soBean", "soKids" };

    // Non-blank text followed by a child element; SLOT is replaced by the property's own name.
    private static final String[] MIXED_CONTENTS = { "ab<zqx/>", "ab<i>b</i>c", "<![CDATA[ab]]><zqx><q>1</q></zqx>c", "ab<SLOT><SLOT>x</SLOT></SLOT>",
            "ab<e>1</e><e>2</e>", "ab<!-- c --><zqx/>", "ab<after>evil</after>" };

    private static XmlParser[] staxParsers() {
        return new XmlParser[] { new XmlParserImpl(XmlParserType.StAX), new AbacusXmlParserImpl(XmlParserType.StAX) };
    }

    // Mixed content in a SERIALIZE_ONLY property of every kind (scalar, primitive, collection, array, map, Optional, Object,
    // bean, bean list), in the element form, the name-attribute form, as the last property and twice in a row, is skipped
    // exactly as the same property in ignoredPropNames is, and every following property (a list, a primitive, a bean) is read.
    // r108 rejected all of these with "Mixed scalar and nested XML values are not supported".
    @Test
    public void testStAXSkipsSerializeOnlyMixedContentOfEveryKindLikeAnIgnoredProperty() {
        for (final XmlParser parser : staxParsers()) {
            for (final String slot : SERIALIZE_ONLY_SLOTS) {
                final XmlDeserConfig ignored = new XmlDeserConfig().setIgnoredPropNames(G141VBean.class, Set.of(slot));

                for (final String content : MIXED_CONTENTS) {
                    final String c = content.replace("SLOT", slot);
                    final String element = "<" + slot + ">" + c + "</" + slot + ">";
                    final String[] documents = {
                            "<g141VBean>" + element + "<afterList>[\"p\",\"q\"]</afterList><num>5</num><afterBean><g141VKid><v>w</v></g141VKid></afterBean>"
                                    + "<after>z</after></g141VBean>",
                            "<g141VBean><num>5</num><after>z</after>" + element + "</g141VBean>",
                            "<g141VBean>" + element + "<soStr>ab<i>q</i></soStr><num>5</num><after>z</after></g141VBean>",
                            "<bean name=\"g141VBean\"><property name=\"" + slot + "\">" + c + "</property><property name=\"num\">5</property>"
                                    + "<property name=\"afterList\">[\"p\",\"q\"]</property><property name=\"after\">z</property></bean>" };

                    for (final String xml : documents) {
                        final String label = parser.getClass().getSimpleName() + ": " + xml;
                        final G141VBean bean = parser.deserialize(xml, null, G141VBean.class);

                        assertEquals(parser.deserialize(xml, ignored, G141VBean.class).toString(), bean.toString(), label);
                        assertEquals("z", bean.getAfter(), label);
                        assertEquals(5, bean.getNum(), label);
                        assertNull(bean.getName(), label);
                        assertTrue(bean.toString().contains(NO_SO), label);

                        if (xml.contains("afterList")) {
                            assertEquals(List.of("p", "q"), bean.getAfterList(), label);
                        }

                        if (xml.contains("afterBean")) {
                            assertEquals("w", bean.getAfterBean().getV(), label);
                        }
                    }
                }
            }
        }
    }

    // Inside a nested bean (as its first or last property) and in a list of beans, the skip ends at the SERIALIZE_ONLY
    // property's own end: the nested bean, the following beans and the outer bean's remaining properties are read.
    @Test
    public void testStAXSkipsSerializeOnlyMixedContentInNestedBeans() {
        for (final XmlParser parser : staxParsers()) {
            final String label = parser.getClass().getSimpleName();

            G141VOuter outer = parser.deserialize(
                    "<g141VOuter><inner><g141VBean><soList>ab<i>b</i>c</soList><after>in</after></g141VBean></inner><tail>t</tail></g141VOuter>", null,
                    G141VOuter.class);
            assertEquals("in", outer.getInner().getAfter(), label);
            assertTrue(outer.getInner().toString().contains(NO_SO), label);
            assertEquals("t", outer.getTail(), label);

            outer = parser.deserialize(
                    "<g141VOuter><inner><g141VBean><after>in</after><soBean>ab<g141VKid><v>x</v></g141VKid></soBean></g141VBean></inner><tail>t</tail></g141VOuter>",
                    null, G141VOuter.class);
            assertEquals("in", outer.getInner().getAfter(), label);
            assertNull(outer.getInner().getSoBean(), label);
            assertEquals("t", outer.getTail(), label);
        }

        // The Abacus StAX reader reads a bean list property in a different form; the standard reader reads this one.
        final G141VOuter outer = new XmlParserImpl(XmlParserType.StAX).deserialize(
                "<g141VOuter><inners><g141VBean><soMap>ab<zqx/></soMap><after>a</after></g141VBean><g141VBean><after>b</after></g141VBean></inners>"
                        + "<tail>t</tail></g141VOuter>",
                null, G141VOuter.class);
        assertEquals(2, outer.getInners().size());
        assertEquals("a", outer.getInners().get(0).getAfter());
        assertNull(outer.getInners().get(0).getSoMap());
        assertEquals("b", outer.getInners().get(1).getAfter());
        assertEquals("t", outer.getTail());
    }

    // Neighbours that must stay as they were: an isNull marker still consumes the whole SERIALIZE_ONLY element, a structured
    // SERIALIZE_ONLY value without text is still read and dropped, and mixed content in an ordinary property is still rejected
    // in the name-attribute form as well.
    @Test
    public void testStAXSerializeOnlyNeighboursUnchanged() {
        for (final XmlParser parser : staxParsers()) {
            final String label = parser.getClass().getSimpleName();

            G141VBean bean = parser.deserialize("<g141VBean><soStr isNull=\"true\">ab<i>b</i></soStr><after>z</after></g141VBean>", null, G141VBean.class);
            assertEquals("z", bean.getAfter(), label);
            assertNull(bean.getSoStr(), label);

            bean = parser.deserialize("<g141VBean><soKids><g141VKid><v>x</v></g141VKid></soKids><after>z</after></g141VBean>", null, G141VBean.class);
            assertEquals("z", bean.getAfter(), label);
            assertNull(bean.getSoKids(), label);

            final ParsingException e = assertThrows(ParsingException.class,
                    () -> parser.deserialize("<bean name=\"g141VBean\"><property name=\"name\">ab<zqx/></property><property name=\"after\">z</property></bean>",
                            null, G141VBean.class),
                    label);
            assertEquals("Mixed scalar and nested XML values are not supported", e.getMessage(), label);
        }
    }

    // ---- deep review 2026-10-27 verify G141 end ----
}
