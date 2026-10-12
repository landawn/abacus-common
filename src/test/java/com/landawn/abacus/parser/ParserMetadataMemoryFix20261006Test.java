package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.AccessFieldByMethod;
import com.landawn.abacus.annotation.JsonXmlField;
import com.landawn.abacus.exception.ParsingException;

@Tag("unit")
class ParserMetadataMemoryFix20261006Test extends TestBase {
    private static final String LONG_NAME = "long_" + "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
            + "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz"
            + "abcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyzabcdefghijklmnopqrstuvwxyz";

    @Test
    void ignoredLongJsonKeysAreNotRetainedForStringOrReaderInput() throws Exception {
        final ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(NameBean.class);
        final JsonParser parser = new JsonParserImpl();
        final JsonDeserConfig config = JsonDeserConfig.create().setIgnoreUnmatchedProperty(true);
        final String key = "unknown" + "x".repeat(32_768);
        final String json = "{\"" + key + "\":1,\"name\":\"kept\"}";

        assertEquals("kept", parser.deserialize(json, config, NameBean.class).name);
        assertEquals("kept", parser.deserialize(new StringReader(json), config, NameBean.class).name);
        assertNull(info.getPropInfo(key));
        assertFalse(misses(info).contains(key));
        assertFalse(bindings(info).containsKey(key));

        assertNull(info.getPropInfo("ordinaryMiss"));
        assertTrue(misses(info).contains("ordinaryMiss"), "ordinary repeated misses should still benefit from memoization");
    }

    @Test
    void longResolvedSpellingsAndChainsKeepTheirLookupSemantics() throws Exception {
        final ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(NameBean.class);
        final String fuzzy = " ".repeat(300) + "name";
        assertSame(info.getPropInfo("name"), info.getPropInfo(fuzzy));
        assertSame(info.getPropInfo("name"), info.getPropInfo(fuzzy));
        assertFalse(bindings(info).containsKey(fuzzy));

        final ParserUtil.BeanInfo child = ParserUtil.getBeanInfo(LongNameBean.class);
        assertSame(child.getPropInfo("value"), child.getPropInfo(LONG_NAME));
        assertTrue(bindings(child).containsKey(LONG_NAME), "declared names are installed regardless of their length");
        assertEquals("v", new JsonParserImpl().deserialize("{\"" + LONG_NAME + "\":\"v\"}", LongNameBean.class).value);

        final ParserUtil.BeanInfo outer = ParserUtil.getBeanInfo(OuterBean.class);
        final String path = "child." + LONG_NAME;
        final List<ParserUtil.PropInfo> chain = outer.getPropInfoChain(path);
        assertEquals(List.of(outer.getPropInfo("child"), child.getPropInfo("value")), chain);
        assertEquals(chain, outer.getPropInfoChain(path));
        assertFalse(chains(outer).containsKey(path));
        assertEquals(2, outer.getPropInfoChain("child.value").size());
        assertTrue(chains(outer).containsKey("child.value"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void asmAccessorsAreSharedByDeclaringClassWithinOneBeanOnly() {
        assertTrue(ASMUtil.isASMAvailable(), "this test requires the project's reflectasm dependency and generated availability-probe accessors");
        final ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(AccessorBean.class, true);
        final ParserUtil.ASMPropInfo first = assertInstanceOf(ParserUtil.ASMPropInfo.class, info.getPropInfo("first"));
        final ParserUtil.ASMPropInfo second = assertInstanceOf(ParserUtil.ASMPropInfo.class, info.getPropInfo("second"));
        final ParserUtil.ASMPropInfo inherited = assertInstanceOf(ParserUtil.ASMPropInfo.class, info.getPropInfo("inherited"));

        assertSame(first.getMethodAccess, first.setMethodAccess);
        assertSame(first.getMethodAccess, second.getMethodAccess);
        assertSame(first.getMethodAccess, second.setMethodAccess);
        assertNotSame(first.getMethodAccess, inherited.getMethodAccess);
        // Private backing fields are reached through the getters/setters, so no field accessor is generated for them
        // (2026-10-10; it used to be generated and never used). Field accessor sharing is pinned with public fields below.
        assertNull(first.fieldAccess);
        assertNull(second.fieldAccess);
        assertNull(inherited.fieldAccess);

        final ParserUtil.BeanInfo fieldInfo = ParserUtil.getBeanInfo(PublicFieldAccessorBean.class, true);
        final ParserUtil.ASMPropInfo a = assertInstanceOf(ParserUtil.ASMPropInfo.class, fieldInfo.getPropInfo("a"));
        final ParserUtil.ASMPropInfo b = assertInstanceOf(ParserUtil.ASMPropInfo.class, fieldInfo.getPropInfo("b"));
        final ParserUtil.ASMPropInfo inheritedField = assertInstanceOf(ParserUtil.ASMPropInfo.class, fieldInfo.getPropInfo("inheritedField"));
        assertNotNull(a.fieldAccess);
        assertSame(a.fieldAccess, b.fieldAccess);
        assertNotSame(a.fieldAccess, inheritedField.fieldAccess);

        final AccessorBean bean = new AccessorBean();
        first.setPropValue(bean, "a");
        second.setPropValue(bean, "b");
        inherited.setPropValue(bean, "c");
        assertEquals("a", first.getPropValue(bean));
        assertEquals("b", second.getPropValue(bean));
        assertEquals("c", inherited.getPropValue(bean));

        final ParserUtil.ASMPropInfo other = (ParserUtil.ASMPropInfo) ParserUtil.getBeanInfo(AccessorBean.class, true).getPropInfo("first");
        assertNotSame(first.getMethodAccess, other.getMethodAccess, "the sharing map must not become a new global class cache");
        final ParserUtil.PropInfo reflective = ParserUtil.getBeanInfo(AccessorBean.class, false).getPropInfo("first");
        assertFalse(reflective instanceof ParserUtil.ASMPropInfo);
        reflective.setPropValue(bean, "d");
        assertEquals("d", reflective.getPropValue(bean));
    }

    @Test
    void sharedAsmAccessorsPreserveImmutableRecordConstruction() {
        final Pair pair = new JsonParserImpl().deserialize("{\"first\":\"a\",\"second\":\"b\"}", Pair.class);
        assertEquals(new Pair("a", "b"), pair);
        assertEquals("a", ParserUtil.getBeanInfo(Pair.class).getPropInfo("first").getPropValue(pair));
    }

    @Test
    void xmlTagEscapingAndDeferredValidationKeepBothNameModes() {
        final ParserUtil.XmlNameTag plain = new ParserUtil.XmlNameTag("plain", "String", false);
        assertEquals("<property name=\"plain\" type=\"String\">", String.valueOf(plain.epStartWithType));
        plain.checkXmlName(true, "Property name");
        plain.checkXmlName(false, "Property name");
        plain.checkXmlName(true, "Property name");

        final ParserUtil.XmlNameTag escaped = new ParserUtil.XmlNameTag("a&<>'\"\t\n\r", "Map<String,Integer>", false);
        assertEquals("<property name=\"a&amp;&lt;&gt;&apos;&quot;&#x9;&#xa;&#xd;\" type=\"Map&lt;String,Integer&gt;\">",
                String.valueOf(escaped.epStartWithType));
        escaped.checkXmlName(false, "Property name");
        assertThrows(ParsingException.class, () -> escaped.checkXmlName(true, "Property name"));
        escaped.checkXmlName(false, "Property name");

        final ParserUtil.XmlNameTag invalid = new ParserUtil.XmlNameTag("a\u0001b", "String", false);
        assertThrows(ParsingException.class, () -> invalid.checkXmlName(false, "First label"));
        final ParsingException secondFailure = assertThrows(ParsingException.class, () -> invalid.checkXmlName(false, "Second label"));
        assertTrue(secondFailure.getMessage().contains("Second label"));
    }

    @Test
    void xmlNameValidationStillRunsAfterExcludingTheProperty() {
        final InvalidXmlNameBean bean = new InvalidXmlNameBean();
        bean.value = "v";
        final XmlParser parser = new AbacusXmlParserImpl(XmlParserType.StAX);
        assertThrows(ParsingException.class, () -> parser.serialize(bean, XmlSerConfig.create().setTagByPropertyName(true)));
        final String output = parser.serialize(bean, XmlSerConfig.create().setTagByPropertyName(true).setIgnoredPropNames(Set.of("value")));
        assertFalse(output.contains("v</"));
    }

    @SuppressWarnings("unchecked")
    private static Set<String> misses(final ParserUtil.BeanInfo info) throws Exception {
        return (Set<String>) field(info, "missedPropNames");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> bindings(final ParserUtil.BeanInfo info) throws Exception {
        return (Map<String, ?>) field(info, "propInfoMap");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, ?> chains(final ParserUtil.BeanInfo info) throws Exception {
        return (Map<String, ?>) field(info, "propInfoQueueMap");
    }

    private static Object field(final Object target, final String name) throws Exception {
        final Field field = target.getClass().getDeclaredField(name);
        field.setAccessible(true);
        return field.get(target);
    }

    public static class NameBean {
        public String name;
    }

    public static class LongNameBean {
        @JsonXmlField(name = LONG_NAME)
        public String value;
    }

    public static class OuterBean {
        public LongNameBean child;
    }

    public static class PublicFieldAccessorBase {
        public String inheritedField;
    }

    public static class PublicFieldAccessorBean extends PublicFieldAccessorBase {
        public String a;
        public String b;
    }

    // Accessor-based on purpose: a property is read and written through its field by default (in both modes), so the
    // generated method accessors - whose sharing this fixture pins - are used only under @AccessFieldByMethod.
    @AccessFieldByMethod
    public static class AccessorBase {
        private String inherited;

        public String getInherited() {
            return inherited;
        }

        public void setInherited(final String value) {
            inherited = value;
        }
    }

    @AccessFieldByMethod
    public static class AccessorBean extends AccessorBase {
        private String first;
        private String second;

        public String getFirst() {
            return first;
        }

        public void setFirst(final String value) {
            first = value;
        }

        public String getSecond() {
            return second;
        }

        public void setSecond(final String value) {
            second = value;
        }
    }

    public record Pair(String first, String second) {
    }

    public static class InvalidXmlNameBean {
        @JsonXmlField(name = "a&b")
        public String value;
    }
}
