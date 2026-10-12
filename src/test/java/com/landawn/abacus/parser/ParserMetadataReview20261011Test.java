package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.exception.ParsingException;

@Tag("unit")
class ParserMetadataReview20261011Test extends TestBase {
    @Test
    void genericBeanConfigurationsKeepDistinctEqualityAndMapKeys() throws Exception {
        final java.lang.reflect.Type textType = Types.class.getField("text").getGenericType();
        final java.lang.reflect.Type numberType = Types.class.getField("number").getGenericType();

        for (final DeserializationConfig<?> config : new DeserializationConfig<?>[] { new JsonDeserConfig(), new XmlDeserConfig(), new KryoDeserConfig(),
                new AvroDeserConfig() }) {
            final DeserializationConfig<?> text = config.copy().setValueTypesByBeanClass(textType);
            final DeserializationConfig<?> number = config.copy().setValueTypesByBeanClass(numberType);
            final DeserializationConfig<?> sameText = config.copy().setValueTypesByBeanClass(Types.class.getField("textAgain").getGenericType());

            assertNotEquals(text, number);
            assertNotEquals(number, text);
            assertEquals(text, sameText);
            assertEquals(text.hashCode(), sameText.hashCode());
            assertEquals(text, text.copy());
            assertEquals(2, new HashSet<>(java.util.List.of(text, number, sameText)).size());
            assertEquals(String.class, text.getValueType("value").javaType());
            assertEquals(Integer.class, number.getValueType("value").javaType());
            assertNotEquals(text, config);
            assertEquals(config, text.copy().setValueTypesByBeanClass(null));
        }

        final JsonParser parser = ParserFactory.createJsonParser();
        final Map<?, ?> text = parser.deserialize("{\"value\":12}", new JsonDeserConfig().setValueTypesByBeanClass(textType), Map.class);
        final Map<?, ?> number = parser.deserialize("{\"value\":12}", new JsonDeserConfig().setValueTypesByBeanClass(numberType), Map.class);
        assertEquals("12", text.get("value"));
        assertEquals(12, number.get("value"));
    }

    @Test
    @SuppressWarnings("deprecation")
    void ambiguousAliasesKeepTheirOriginalColdLookupResults() {
        for (final String name : new String[] { "ABCdEF", "getABCDEF", "isabcdef", "hasABCDEF" }) {
            final ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(AmbiguousAliasBean.class, false);

            assertEquals("abcDef", info.getPropInfo(name).name, name);
            assertEquals("abcDef", info.getPropInfo("abcDef").name);
            assertEquals("abcdef", info.getPropInfo("abcdef").name);
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    void ambiguousAliasesRemainStableAfterFuzzyCacheWarmup() {
        for (final String name : new String[] { "ABCdEF", "getABCDEF", "isabcdef", "hasABCDEF" }) {
            final ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(AmbiguousAliasBean.class, false);

            for (int i = 1; i <= 64; i++) {
                assertEquals("p0", info.getPropInfo(" ".repeat(i) + "p0").name);
            }

            assertEquals("abcDef", info.getPropInfo(name).name, name);
            assertSame(info.getPropInfo(name), info.readPropInfo(name.toCharArray(), 0, name.length()));
        }
    }

    @Test
    @SuppressWarnings("deprecation")
    void fuzzyLookupDoesNotExpandTheDeclaredPropertyVocabulary() {
        final ParserUtil.BeanInfo cold = ParserUtil.getBeanInfo(FieldBean.class, false);
        final ParserUtil.BeanInfo warm = ParserUtil.getBeanInfo(FieldBean.class, false);

        assertNull(cold.getPropInfo("getgetValue"));
        assertSame(warm.getPropInfo("value"), warm.getPropInfo("getValue"));
        assertNull(warm.getPropInfo("getgetValue"));
        assertNull(warm.getPropInfo("isgetValue"));
        assertNull(cold.getPropInfo("getgetValue"));
        assertSame(warm.getPropInfo("value"), warm.getPropInfo("VALUE"));
        assertSame(warm.getPropInfo("value"), warm.getPropInfo("va_lue"));
        assertSame(warm.getPropInfo("value"), warm.readPropInfo("getValue".toCharArray(), 0, 8));
        assertThrows(IllegalArgumentException.class, () -> warm.getPropInfo((String) null));
    }

    @Test
    @SuppressWarnings("deprecation")
    void fuzzyWarmupCannotMakeAnUnknownJsonKeyWritable() {
        ParserUtil.refreshBeanPropInfo(FieldBean.class);
        final JsonParser parser = ParserFactory.createJsonParser();
        final JsonDeserConfig strict = new JsonDeserConfig().setIgnoreUnmatchedProperty(false);

        assertEquals("warmup", parser.deserialize("{\"getValue\":\"warmup\"}", strict, FieldBean.class).value);
        assertThrows(ParsingException.class, () -> parser.deserialize("{\"getgetValue\":\"bad\"}", strict, FieldBean.class));
        assertThrows(ParsingException.class, () -> parser.deserialize(new StringReader("{\"isgetValue\":\"bad\"}"), strict, FieldBean.class));
        assertNull(parser.deserialize("{\"getgetValue\":\"ignored\"}", FieldBean.class).value);
    }

    @Test
    @SuppressWarnings("deprecation")
    void fuzzyMemoizationKeepsDeclaredScanSmallAndDoesNotRetainLongNames() throws Exception {
        final ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(FieldBean.class, false);
        final Field bindingsField = ParserUtil.BeanInfo.class.getDeclaredField("propInfoMap");
        bindingsField.setAccessible(true);
        final Map<?, ?> bindings = (Map<?, ?>) bindingsField.get(info);
        final Set<?> declaredNames = Set.copyOf(bindings.keySet());

        for (int i = 1; i < 270; i++) {
            assertSame(info.getPropInfo("value"), info.getPropInfo(" ".repeat(i) + "value"));
        }
        assertEquals(declaredNames, bindings.keySet());

        final Field fuzzyField = ParserUtil.BeanInfo.class.getDeclaredField("fuzzyPropInfoMap");
        fuzzyField.setAccessible(true);
        final Map<?, ?> fuzzy = (Map<?, ?>) fuzzyField.get(info);
        assertSame(info.getPropInfo("value"), info.getPropInfo("getValue"));
        assertTrue(fuzzy.containsKey("getValue"));
        assertFalse(fuzzy.containsKey(" ".repeat(257) + "value"));
    }

    // These padding fields expose a table-size boundary where HashMap and ConcurrentCacheMap
    // visit the equally ranked abcDef/abcdef aliases in a different order.
    public static class AmbiguousAliasBean {
        public String p0;
        public String p1;
        public String p2;
        public String p3;
        public String p4;
        public String p5;
        public String p6;
        public String p7;
        public String p8;
        public String p9;
        public String p10;
        public String p11;
        public String p12;
        public String p13;
        public String p14;
        public String p15;
        public String p16;
        public String p17;
        public String p18;
        public String p19;
        public String abcDef;
        public String abcdef;
    }

    public static class FieldBean {
        public String value;
    }

    public static class Box<T> {
        public T value;
    }

    public static class Types {
        public Box<String> text;
        public Box<String> textAgain;
        public Box<Integer> number;
    }
}
