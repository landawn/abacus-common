package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.parser.ParserUtil.BeanInfo;
import com.landawn.abacus.util.u.Nullable;

public class BeansGetTest extends BeansTestSupport {

    @Test
    public void testGetBeanInfo() {
        BeanInfo beanInfo = Beans.getBeanInfo(SimpleBean.class);
        assertNotNull(beanInfo);
        assertFalse(beanInfo.propInfoList.isEmpty());

        assertNotNull(Beans.getBeanInfo(EntityBean.class));
        assertFalse(Beans.getBeanInfo(EntityBean.class).propInfoList.isEmpty());
        assertNotNull(Beans.getBeanInfo(RecordBean.class));

        assertThrows(IllegalArgumentException.class, () -> Beans.getBeanInfo(NonBean.class));
    }

    @Test
    public void testGetBuilderInfo() {
        Beans.BuilderInfo builderInfo = Beans.getBuilderInfo(BeanWithBuilder.class);
        assertNotNull(builderInfo);
        assertNotNull(builderInfo.builderClass());

        Object builder = builderInfo.newBuilder();
        assertNotNull(builder);
        assertTrue(builder.getClass().getName().contains("Builder"));

        Object built = builderInfo.build(builder);
        assertNotNull(built);
        assertTrue(built instanceof BeanWithBuilder);

        assertNull(Beans.getBuilderInfo(SimpleBean.class));
        assertNull(Beans.getBuilderInfo(Address.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.getBuilderInfo(null));
    }

    @Test
    public void testGetPropNameByMethod() throws Exception {
        assertEquals("name", Beans.getPropNameByMethod(SimpleBean.class.getMethod("getName")));
        assertEquals("age", Beans.getPropNameByMethod(SimpleBean.class.getMethod("getAge")));
        assertEquals("name", Beans.getPropNameByMethod(SimpleBean.class.getMethod("setName", String.class)));
        assertEquals("age", Beans.getPropNameByMethod(SimpleBean.class.getMethod("setAge", int.class)));
        assertEquals("active", Beans.getPropNameByMethod(SimpleBean.class.getMethod("getActive")));
        assertEquals("active", Beans.getPropNameByMethod(SimpleBean.class.getMethod("setActive", Boolean.class)));
        assertEquals("active", Beans.getPropNameByMethod(HasPrefixBean.class.getMethod("hasActive")));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropNameByMethod(null));
    }

    @Test
    public void testGetPropNameList() {
        ImmutableList<String> propNames = Beans.getPropNameList(SimpleBean.class);
        assertTrue(propNames.contains("name"));
        assertTrue(propNames.contains("age"));
        assertTrue(propNames.contains("active"));

        assertTrue(Beans.getPropNameList(EntityBean.class).contains("id"));
        assertTrue(Beans.getPropNameList(EntityBean.class).contains("value"));
        assertEquals(Collections.singletonList("name"), Beans.getPropNameList(NoDefaultConstructorBean.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropNameList(null));
    }

    @Test
    public void testGetPropNameList_FieldBackedFirst() {
        assertEquals(CommonUtil.asList("a", "c", "baseComputed", "subComputed"), Beans.getPropNameList(BeansOrderSubFixture.class));
        assertEquals(CommonUtil.asList("a", "baseComputed"), Beans.getPropNameList(BeansOrderBaseFixture.class));
        assertEquals(CommonUtil.asList("a", "c"), new ArrayList<>(Beans.getPropFields(BeansOrderSubFixture.class).keySet()));
    }

    @Test
    public void testGetPropNameList_SkipsStaticAccessors() {
        List<String> propNames = Beans.getPropNameList(BeanWithStaticAccessors.class);
        assertTrue(propNames.contains("name"));
        assertFalse(propNames.contains("version"));
        assertNotNull(Beans.getPropGetter(BeanWithStaticAccessors.class, "name"));
        assertNull(Beans.getPropGetter(BeanWithStaticAccessors.class, "version"));
        assertNull(Beans.getPropSetter(BeanWithStaticAccessors.class, "version"));
    }

    @Test
    public void testGetPropNames() {
        List<String> props = Beans.getPropNames(SimpleBean.class, Collections.singleton("age"));
        assertTrue(props.contains("name"));
        assertTrue(props.contains("active"));
        assertFalse(props.contains("age"));

        props = Beans.getPropNames(SimpleBean.class, Arrays.asList("age", "active"));
        assertTrue(props.contains("name"));
        assertFalse(props.contains("age"));
        assertFalse(props.contains("active"));

        props = Beans.getPropNames(SimpleBean.class, new LinkedList<>(Arrays.asList("age")));
        assertTrue(props.contains("name"));
        assertFalse(props.contains("age"));

        Set<String> treeSet = new TreeSet<>();
        treeSet.add("age");
        props = Beans.getPropNames(SimpleBean.class, treeSet);
        assertTrue(props.contains("name"));
        assertFalse(props.contains("age"));

        props = Beans.getPropNames(SimpleBean.class, (Collection<String>) null);
        assertTrue(props.contains("name"));
        assertTrue(props.contains("age"));
        assertTrue(props.contains("active"));

        props = Beans.getPropNames(SimpleBean.class, (Set<String>) null);
        assertTrue(props.contains("name"));

        props = Beans.getPropNames(SimpleBean.class, Collections.emptyList());
        assertTrue(props.contains("name"));
        assertTrue(props.contains("age"));
        assertTrue(props.contains("active"));

        assertThrows(IllegalArgumentException.class, () -> Beans.getPropNames(null, Collections.emptySet()));
    }

    @Test
    public void testGetPropNames_ExclusionSetLargerThanPropList() {
        Set<String> oversized = new HashSet<>(Arrays.asList("name", "age", "active", "x1", "x2", "x3", "x4"));
        List<String> result = assertDoesNotThrow(() -> Beans.getPropNames(SimpleBean.class, oversized));
        assertTrue(result.isEmpty());
    }

    @Test
    public void testGetPropNames_FromBean() {
        simpleBean.setActive(null);
        List<String> props = Beans.getPropNames(simpleBean, true);
        assertTrue(props.contains("name"));
        assertTrue(props.contains("age"));
        assertFalse(props.contains("active"));

        props = Beans.getPropNames(simpleBean, false);
        assertTrue(props.contains("active"));

        SimpleBean allSet = new SimpleBean("Rex", 5);
        allSet.setActive(false);
        props = Beans.getPropNames(allSet, true);
        assertTrue(props.contains("name"));
        assertTrue(props.contains("active"));

        props = Beans.getPropNames(simpleBean, name -> name.startsWith("a"));
        assertTrue(props.contains("age"));
        assertTrue(props.contains("active"));
        assertFalse(props.contains("name"));
        assertTrue(Beans.getPropNames(simpleBean, name -> false).isEmpty());

        props = Beans.getPropNames(simpleBean, (name, value) -> value instanceof String);
        assertTrue(props.contains("name"));
        assertFalse(props.contains("age"));

        props = Beans.getPropNames(simpleBean, (name, value) -> value != null);
        assertFalse(props.contains("active"));
        assertTrue(props.contains("name"));
    }

    @Test
    public void testGetIgnoredPropNamesForDiff() {
        ImmutableSet<String> ignored = Beans.getIgnoredPropNamesForDiff(BeanWithDiffIgnore.class);
        assertEquals(2, ignored.size());
        assertTrue(ignored.contains("lastModified"));
        assertTrue(ignored.contains("internalFlag"));
        assertFalse(ignored.contains("name"));

        assertTrue(Beans.getIgnoredPropNamesForDiff(SimpleBean.class).isEmpty());
        assertEquals(CommonUtil.asSet("secret"), Beans.getIgnoredPropNamesForDiff(DiffProbeBean.class));
    }

    @Test
    public void testGetPropField() {
        Field field = Beans.getPropField(SimpleBean.class, "name");
        assertNotNull(field);
        assertEquals("name", field.getName());
        assertNotNull(Beans.getPropField(SimpleBean.class, "NAME"));
        assertNotNull(Beans.getPropField(SimpleBean.class, "NaMe"));
        assertNull(Beans.getPropField(SimpleBean.class, "nonExistent"));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropField(NonBean.class, "field"));
    }

    @Test
    public void testGetPropFields() {
        ImmutableMap<String, Field> fields = Beans.getPropFields(SimpleBean.class);
        assertNotNull(fields.get("name"));
        assertNotNull(fields.get("age"));
        assertNotNull(fields.get("active"));

        fields = Beans.getPropFields(EntityBean.class);
        assertNotNull(fields.get("id"));
        assertNotNull(fields.get("value"));
    }

    @Test
    public void testGetPropGetter() {
        Method method = Beans.getPropGetter(SimpleBean.class, "name");
        assertNotNull(method);
        assertEquals("getName", method.getName());
        assertNotNull(Beans.getPropGetter(SimpleBean.class, "NAME"));
        assertNotNull(Beans.getPropGetter(SimpleBean.class, "NaMe"));
        assertNull(Beans.getPropGetter(SimpleBean.class, "nonExistent"));
        assertNotNull(Beans.getPropGetter(SimpleBean.class, "active"));
        assertDoesNotThrow(() -> Beans.getPropGetter(SimpleBean.class, "isActive"));
        assertDoesNotThrow(() -> Beans.getPropGetter(SimpleBean.class, "setName"));

        method = Beans.getPropGetter(EntityBean.class, "id");
        assertEquals("getId", method.getName());

        assertThrows(IllegalArgumentException.class, () -> Beans.getPropGetter(NonBean.class, "field"));
    }

    @Test
    public void testGetPropGetter_OverLongName() {
        String longName = "a".repeat(129);
        assertNull(Beans.getPropGetter(SimpleBean.class, longName));
        assertNull(Beans.getPropSetter(SimpleBean.class, longName));
        assertNull(Beans.getPropField(SimpleBean.class, longName));
    }

    @Test
    public void testGetPropGetters() {
        ImmutableMap<String, Method> methods = Beans.getPropGetters(SimpleBean.class);
        assertNotNull(methods.get("name"));
        assertNotNull(methods.get("age"));
        assertNotNull(methods.get("active"));

        methods = Beans.getPropGetters(EntityBean.class);
        assertNotNull(methods.get("id"));
        assertNotNull(methods.get("value"));
    }

    @Test
    public void testGetPropSetter() {
        Method method = Beans.getPropSetter(SimpleBean.class, "name");
        assertNotNull(method);
        assertEquals("setName", method.getName());
        assertNotNull(Beans.getPropSetter(SimpleBean.class, "NAME"));
        assertNotNull(Beans.getPropSetter(SimpleBean.class, "AgE"));
        assertNotNull(Beans.getPropSetter(SimpleBean.class, "active"));
        assertNull(Beans.getPropSetter(SimpleBean.class, "nonExistent"));

        method = Beans.getPropSetter(EntityBean.class, "value");
        assertEquals("setValue", method.getName());

        assertThrows(IllegalArgumentException.class, () -> Beans.getPropSetter(NonBean.class, "field"));
    }

    @Test
    public void testGetPropSetters() {
        ImmutableMap<String, Method> setters = Beans.getPropSetters(SimpleBean.class);
        assertEquals("setName", setters.get("name").getName());
        assertTrue(setters.containsKey("age"));
        assertTrue(setters.containsKey("active"));

        setters = Beans.getPropSetters(EntityBean.class);
        assertNotNull(setters.get("id"));
        assertNotNull(setters.get("value"));
    }

    @Test
    public void testGetPropValue() throws Exception {
        assertEquals("John", Beans.getPropValue(simpleBean, "name"));
        assertEquals(25, (int) Beans.getPropValue(simpleBean, "age"));
        assertEquals(true, Beans.getPropValue(simpleBean, "active"));

        assertEquals("John", Beans.getPropValue(simpleBean, SimpleBean.class.getMethod("getName")));
        assertEquals(25, (int) Beans.getPropValue(simpleBean, SimpleBean.class.getMethod("getAge")));

        assertEquals("New York", Beans.getPropValue(nestedBean, "address.city"));
        assertEquals("5th Avenue", Beans.getPropValue(nestedBean, "address.street"));
        assertEquals("10001", Beans.getPropValue(nestedBean, "address.zipCode"));
        assertEquals("John", Beans.getPropValue(nestedBean, "simpleBean.name"));
        assertEquals(25, (Integer) Beans.getPropValue(nestedBean, "simpleBean.age"));
    }

    @Test
    public void testGetPropValue_IgnoreUnmatched() {
        assertNull(Beans.getPropValue(simpleBean, "nonExistent", true));
        assertNull(Beans.getPropValue(simpleBean, "nonexistent.field", true));

        NestedBean bean = new NestedBean();
        bean.setId("123");
        assertNull(Beans.getPropValue(bean, "simpleBean.name", true));

        assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(simpleBean, "nonExistent", false));
    }

    @Test
    public void testGetPropValueIfPresent() {
        Nullable<String> name = Beans.getPropValueIfPresent(simpleBean, "name");
        assertTrue(name.isPresent());
        assertEquals("John", name.get());

        SimpleBean bean = new SimpleBean("John", 25);
        Nullable<Boolean> active = Beans.getPropValueIfPresent(bean, "active");
        assertTrue(active.isPresent());
        assertNull(active.orElseNull());

        assertTrue(Beans.getPropValueIfPresent(bean, "unknown").isEmpty());

        Nullable<String> city = Beans.getPropValueIfPresent(nestedBean, "address.city");
        assertTrue(city.isPresent());
        assertEquals("New York", city.get());

        NestedBean nb = new NestedBean();
        nb.setAddress(new Address("NYC"));
        Nullable<String> street = Beans.getPropValueIfPresent(nb, "address.street");
        assertTrue(street.isPresent());
        assertNull(street.orElseNull());

        NestedBean nb2 = new NestedBean();
        assertTrue(Beans.getPropValueIfPresent(nb2, "address.city").isEmpty());
        assertTrue(Beans.getPropValueIfPresent(nb2, "address.nope").isEmpty());
    }

    /**
     * A builder-based bean with a property its builder does not expose. {@code scanPropAccessors} promotes a
     * field-backed getter to a property without requiring the builder to be able to set it, so this is a legal
     * shape; {@code final} fields make it an immutable bean, which is the shape {@code ParserUtil} cannot fall
     * back to field access for.
     */
    public static class BuilderOmitsPropBean {
        private final String value;
        private final String extra;

        private BuilderOmitsPropBean(final String value, final String extra) {
            this.value = value;
            this.extra = extra;
        }

        public String getValue() {
            return value;
        }

        public String getExtra() {
            return extra;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String value;

            public Builder value(final String value) {
                this.value = value;
                return this;
            }

            public BuilderOmitsPropBean build() {
                return new BuilderOmitsPropBean(value, null);
            }
        }
    }

    @Test
    public void testGetPropSetter_BuilderClassOfABeanWhoseBuilderOmitsAProperty() {
        assertEquals(Arrays.asList("value", "extra"), new ArrayList<>(Beans.getPropNameList(BuilderOmitsPropBean.class)));

        // The builder's setter model is published while the bean is introspected, so the builder class resolves
        // its own setters - tolerantly - and answers null, not "not a bean class", for a property it omits.
        assertEquals("value", Beans.getPropSetter(BuilderOmitsPropBean.Builder.class, "value").getName());
        assertNull(Beans.getPropSetter(BuilderOmitsPropBean.Builder.class, "extra"));
        assertEquals("value", Beans.getPropSetter(BuilderOmitsPropBean.Builder.class, "VALUE").getName());
        assertFalse(Beans.isBeanClass(BuilderOmitsPropBean.Builder.class));

        // A class that really has no setter model of its own is still rejected.
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropSetter(NonBean.class, "field"));
    }

    /** Its own bean, referenced by no other test, so the lookups below really do see the cold path. */
    public static class ColdBuilderBean {
        private final String name;

        private ColdBuilderBean(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String name;

            public Builder name(final String name) {
                this.name = name;
                return this;
            }

            public ColdBuilderBean build() {
                return new ColdBuilderBean(name);
            }
        }
    }

    /**
     * Pins the precondition both builder paragraphs state: a builder class is resolvable here only because the
     * bean it builds was introspected. Looked up on its own first, it has no setter model at all - an empty map,
     * and "not a bean class" from {@code getPropSetter} - because a standalone scan of a canonical builder
     * derives no properties.
     */
    @Test
    public void testGetPropSetters_BuilderClassLookedUpBeforeItsBeanHasNoModel() {
        assertTrue(Beans.getPropSetters(ColdBuilderBean.Builder.class).isEmpty());
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropSetter(ColdBuilderBean.Builder.class, "name"));

        // Introspecting the bean publishes the builder's model, and from then on both answer.
        assertEquals(Arrays.asList("name"), new ArrayList<>(Beans.getPropNameList(ColdBuilderBean.class)));
        assertEquals(Set.of("name"), Beans.getPropSetters(ColdBuilderBean.Builder.class).keySet());
        assertEquals("name", Beans.getPropSetter(ColdBuilderBean.Builder.class, "name").getName());
    }
}
