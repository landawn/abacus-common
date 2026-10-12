package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.type.Type;

/**
 * Pins the array/component rules left open by {@code XmlReportedRemainderRegressionTest}:
 * <ul>
 *   <li>A declared array component is reified, so it beats global element configuration for JSON text and for
 *       {@code <e>} children, at the root and on a map value. This deliberately diverges from root collections,
 *       where the erased generic component yields to configuration
 *       ({@code XmlElementPrecedenceRegressionTest.rootElementConfigurationOverridesGenericDeclaration}): honoring
 *       configuration against a reified component could only fail, and a global element type must not make every
 *       typed array read in the application throw.</li>
 *   <li>An open slot (declared {@code Object} component) honors element configuration on every backend, including
 *       attribute-less map values and bean properties, whose DOM reader must agree with StAX.</li>
 *   <li>That configuration types the elements only. A declared {@code Object[]} stays an {@code Object[]} of the
 *       configured component at the root (including a blank array), on a map value, on a bean property, and inside
 *       {@code Optional} / {@code u.Optional} / {@code u.Nullable}. {@code Long[]} is an {@code Object[]}, so the
 *       component class is asserted explicitly.</li>
 * </ul>
 */
@Tag("unit")
public class GrokXmlParserTest extends TestBase {
    private static final Long[] LONGS = { 1L, 2L };

    static Stream<Arguments> backends() {
        return XmlWrappedObjectRegressionTest.backends();
    }

    private static XmlParser parser(final boolean abacus, final XmlParserType backend) {
        return abacus ? new AbacusXmlParserImpl(backend) : new XmlParserImpl(backend);
    }

    private static List<String> mapValues(final boolean abacus, final String elementName) {
        final List<String> bodies = new ArrayList<>();
        bodies.add("[\"1\",\"2\"]");
        if (abacus) {
            bodies.add("<" + elementName + ">[\"1\",\"2\"]</" + elementName + ">");
            bodies.add("<" + elementName + "><e>1</e><e>2</e></" + elementName + ">");
        } else {
            bodies.add("<e>1</e><e>2</e>");
        }
        final List<String> documents = new ArrayList<>();
        for (final String body : bodies) {
            documents.add(abacus ? "<map><entry><key>k</key><value>" + body + "</value></entry></map>" : "<map><k>" + body + "</k></map>");
        }
        return documents;
    }

    private static void assertOpenObjectArray(final Object value, final String xml) {
        final Object[] values = assertInstanceOf(Object[].class, value, xml);
        assertArrayEquals(new Object[] { 1L, 2L }, values, xml);
        assertEquals(Object.class, values.getClass().getComponentType(), xml);
    }

    private static void assertEmptyOpenObjectArray(final Object value, final String xml) {
        final Object[] values = assertInstanceOf(Object[].class, value, xml);
        assertEquals(0, values.length, xml);
        assertEquals(Object.class, values.getClass().getComponentType(), xml);
    }

    private static void check(final List<Executable> checks, final String xml, final Executable executable) {
        checks.add(() -> {
            try {
                executable.execute();
            } catch (final AssertionError error) {
                throw error;
            } catch (final Throwable thrown) {
                throw new AssertionError(xml + " -> " + thrown.getClass().getSimpleName() + ": " + thrown.getMessage(), thrown);
            }
        });
    }

    @ParameterizedTest
    @MethodSource("backends")
    void declaredRootLongArrayBeatsElementConfiguration(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Integer.class);
        final List<Executable> checks = new ArrayList<>();
        for (final String body : List.of("[\"1\",\"2\"]", "<e>1</e><e>2</e>")) {
            final String xml = "<array>" + body + "</array>";
            check(checks, xml, () -> assertArrayEquals(LONGS, parser.deserialize(xml, config, Type.of("Long[]")), xml));
        }
        assertAll(checks);
    }

    @ParameterizedTest
    @MethodSource("backends")
    void declaredMapLongArrayBeatsElementConfiguration(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Integer.class);
        final List<Executable> checks = new ArrayList<>();
        for (final String xml : mapValues(abacus, "array")) {
            check(checks, xml, () -> {
                final Map<?, ?> actual = parser.deserialize(xml, config, Type.of("Map<String,Long[]>"));
                assertInstanceOf(Long[].class, actual.get("k"), xml);
                assertArrayEquals(LONGS, (Long[]) actual.get("k"), xml);
            });
        }
        assertAll(checks);
    }

    @ParameterizedTest
    @MethodSource("backends")
    void openMapListWithoutTypeAttributeHonorsElementConfiguration(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class);
        final List<Executable> checks = new ArrayList<>();
        for (final String xml : mapValues(abacus, "list")) {
            check(checks, xml, () -> {
                final Map<?, ?> actual = parser.deserialize(xml, config, Type.of("Map<String,List<Object>>"));
                final List<?> values = assertInstanceOf(List.class, actual.get("k"), xml);
                assertEquals(Long.class, values.getFirst().getClass(), xml);
                assertEquals(List.of(1L, 2L), values, xml);
            });
        }
        assertAll(checks);
    }

    @ParameterizedTest
    @MethodSource("backends")
    void openMapObjectArrayWithoutTypeAttributeHonorsElementConfiguration(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class);
        final List<Executable> checks = new ArrayList<>();
        for (final String xml : mapValues(abacus, "array")) {
            check(checks, xml, () -> {
                final Map<?, ?> actual = parser.deserialize(xml, config, Type.of("Map<String,Object[]>"));
                assertOpenObjectArray(actual.get("k"), xml);
            });
        }
        assertAll(checks);
    }

    @ParameterizedTest
    @MethodSource("backends")
    void openRootObjectArrayKeepsObjectComponent(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class);
        final List<Executable> checks = new ArrayList<>();
        for (final String body : List.of("[\"1\",\"2\"]", "<e>1</e><e>2</e>")) {
            final String xml = "<array>" + body + "</array>";
            check(checks, xml, () -> assertOpenObjectArray(parser.deserialize(xml, config, Type.of("Object[]")), xml));
        }
        assertAll(checks);
    }

    public static class ObjectListBean {
        public List<Object> values;
    }

    /**
     * The DOM bean-property reader resolves property types through the same attribute-less path as map values;
     * without the fix it silently ignores element configuration where StAX honors it.
     */
    @ParameterizedTest
    @MethodSource("backends")
    void openBeanListPropertyWithoutTypeAttributeHonorsElementConfiguration(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class);
        // Abacus wraps collection properties in a child element; it rejects bare <e> children on the property element.
        final List<String> bodies = abacus ? List.of("[\"1\",\"2\"]", "<list>[\"1\",\"2\"]</list>", "<list><e>1</e><e>2</e></list>")
                : List.of("[\"1\",\"2\"]", "<e>1</e><e>2</e>");
        final List<Executable> checks = new ArrayList<>();
        for (final String body : bodies) {
            final String xml = "<objectListBean><values>" + body + "</values></objectListBean>";
            check(checks, xml, () -> {
                final ObjectListBean bean = parser.deserialize(xml, config, Type.of(ObjectListBean.class.getName()));
                assertEquals(List.of(1L, 2L), bean.values, xml);
                assertEquals(Long.class, bean.values.getFirst().getClass(), xml);
            });
        }
        assertAll(checks);
    }

    public static class ObjectArrayBean {
        public Object[] values;
    }

    /**
     * Element configuration types the elements of a declared {@code Object[]} property. It must not reify that
     * property as {@code Long[]}.
     */
    @ParameterizedTest
    @MethodSource("backends")
    void openBeanObjectArrayPropertyKeepsObjectComponent(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class);
        final List<String> bodies = abacus ? List.of("[\"1\",\"2\"]", "<array>[\"1\",\"2\"]</array>", "<array><e>1</e><e>2</e></array>")
                : List.of("[\"1\",\"2\"]", "<e>1</e><e>2</e>");
        final List<Executable> checks = new ArrayList<>();
        for (final String body : bodies) {
            final String xml = "<objectArrayBean><values>" + body + "</values></objectArrayBean>";
            check(checks, xml, () -> {
                final ObjectArrayBean bean = parser.deserialize(xml, config, Type.of(ObjectArrayBean.class.getName()));
                assertOpenObjectArray(bean.values, xml);
            });
        }
        assertAll(checks);
    }

    /**
     * A wrapped open array uses the same slot-component rule as the array itself. The wrapper stays, and the held
     * array stays {@code Object[]}.
     */
    @ParameterizedTest
    @MethodSource("backends")
    void openWrappedObjectArrayKeepsObjectComponent(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class);
        final List<Executable> checks = new ArrayList<>();
        for (final XmlWrappedObjectRegressionTest.Wrapper wrapper : XmlWrappedObjectRegressionTest.Wrapper.values()) {
            final String targetName = "Map<String, " + wrapper.typeName + "<Object[]>>";
            for (final String xml : mapValues(abacus, "array")) {
                check(checks, wrapper + " " + xml, () -> {
                    final Map<?, ?> actual = parser.deserialize(xml, config, Type.<Map<?, ?>> of(targetName));
                    assertOpenObjectArray(wrapper.unwrap(actual.get("k")), wrapper + " " + xml);
                });
            }
        }
        assertAll(checks);
    }

    /**
     * A blank {@code <array>} is an empty array of the declared component. Element configuration must not turn it
     * into {@code Long[0]}.
     */
    @ParameterizedTest
    @MethodSource("backends")
    void openRootObjectArrayBlankStaysObjectComponent(final boolean abacus, final XmlParserType backend) {
        final XmlParser parser = parser(abacus, backend);
        final XmlDeserConfig config = XmlDeserConfig.create().setElementType(Long.class);
        final List<Executable> checks = new ArrayList<>();
        for (final String xml : List.of("<array></array>", "<array> </array>", "<array>\n</array>")) {
            check(checks, xml, () -> assertEmptyOpenObjectArray(parser.deserialize(xml, config, Type.of("Object[]")), xml));
        }
        assertAll(checks);
    }
}
