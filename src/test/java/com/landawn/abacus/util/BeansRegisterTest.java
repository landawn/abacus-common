package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTimeoutPreemptively;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.time.Duration;

import org.junit.jupiter.api.Test;

public class BeansRegisterTest extends BeansTestSupport {

    @Test
    public void testRegisterNonBeanClass() {
        class TestNonBean {
            private String value;

            public String getValue() {
                return value;
            }

            public void setValue(String value) {
                this.value = value;
            }
        }

        assertTrue(Beans.isBeanClass(TestNonBean.class));
        Beans.registerNonBeanClass(TestNonBean.class);
        assertFalse(Beans.isBeanClass(TestNonBean.class));

        assertDoesNotThrow(() -> Beans.registerNonBeanClass(NonBean.class));
        assertDoesNotThrow(() -> Beans.registerNonBeanClass(NonBean.class));
    }

    @Test
    public void testRegisterNonPropertyAccessor() {
        assertDoesNotThrow(() -> Beans.registerNonPropertyAccessor(SimpleBean.class, "internal"));
        assertDoesNotThrow(() -> Beans.registerNonPropertyAccessor(SimpleBean.class, "toString"));
        assertDoesNotThrow(() -> Beans.registerNonPropertyAccessor(SimpleBean.class, "nonexistent"));

        assertTrue(Beans.getPropNameList(AccessorCacheBean.class).contains("hidden"));
        assertNotNull(Beans.getPropGetter(AccessorCacheBean.class, "hidden"));

        Beans.registerNonPropertyAccessor(AccessorCacheBean.class, "hidden");

        assertFalse(Beans.getPropNameList(AccessorCacheBean.class).contains("hidden"));
        assertNull(Beans.getPropGetter(AccessorCacheBean.class, "hidden"));
        assertNull(Beans.getPropSetter(AccessorCacheBean.class, "hidden"));
        assertTrue(Beans.getPropNameList(AccessorCacheBean.class).contains("visible"));
    }

    @Test
    public void testRegisterPropertyAccessor() throws Exception {
        Method getName = SimpleBean.class.getMethod("getName");
        Beans.registerPropertyAccessor("name", getName);
        assertDoesNotThrow(() -> Beans.registerPropertyAccessor("name", getName));

        Method setName = SimpleBean.class.getMethod("setName", String.class);
        Beans.registerPropertyAccessor("name", setName);

        Method setAge = SimpleBean.class.getMethod("setAge", int.class);
        assertDoesNotThrow(() -> Beans.registerPropertyAccessor("age", setAge));

        Method toStringMethod = Object.class.getMethod("toString");
        assertThrows(IllegalArgumentException.class, () -> Beans.registerPropertyAccessor("invalid", toStringMethod));
        assertThrows(IllegalArgumentException.class, () -> Beans.registerPropertyAccessor("name", toStringMethod));

        Method getActive = SimpleBean.class.getMethod("getActive");
        assertThrows(IllegalArgumentException.class, () -> Beans.registerPropertyAccessor("name", getActive));
    }

    @Test
    public void testRegisterPropertyAccessor_AliasAfterMiss() throws Exception {
        assertNull(Beans.getPropGetter(CustomAccessorBean.class, "alias"));

        Method getter = CustomAccessorBean.class.getMethod("getValue");
        Method setter = CustomAccessorBean.class.getMethod("setValue", String.class);
        Beans.registerPropertyAccessor("alias", getter);
        Beans.registerPropertyAccessor("alias", setter);

        assertEquals(getter, Beans.getPropGetter(CustomAccessorBean.class, "alias"));
        assertEquals(setter, Beans.getPropSetter(CustomAccessorBean.class, "alias"));
        assertTrue(Beans.getPropNameList(CustomAccessorBean.class).contains("alias"));
    }

    @Test
    public void testRegisterPropertyAccessor_InheritedAlias() throws Exception {
        String alias = "inheritedAlias";
        assertNull(Beans.getPropGetter(CachedPropertyAccessorSubclass.class, alias));
        assertNull(Beans.getBeanInfo(CachedPropertyAccessorSubclass.class).getPropInfo(alias));

        Method getter = PropertyAccessorBaseBean.class.getMethod("getValue");
        Method setter = PropertyAccessorBaseBean.class.getMethod("setValue", String.class);
        Beans.registerPropertyAccessor(alias, getter);
        Beans.registerPropertyAccessor(alias, setter);

        assertEquals(getter, Beans.getPropGetter(CachedPropertyAccessorSubclass.class, alias));
        assertEquals(setter, Beans.getPropSetter(CachedPropertyAccessorSubclass.class, alias));
        assertEquals(getter, Beans.getPropGetter(LatePropertyAccessorSubclass.class, alias));
        assertNotNull(Beans.getBeanInfo(LatePropertyAccessorSubclass.class).getPropInfo(alias));

        LatePropertyAccessorSubclass bean = new LatePropertyAccessorSubclass();
        Beans.setPropValue(bean, alias, "registered-on-base");
        assertEquals("registered-on-base", Beans.getPropValue(bean, alias));
    }

    @Test
    public void testRegisterPropertyAccessor_SubclassWins() throws Exception {
        Method baseGetter = PropertyAccessorOverrideBaseBean.class.getMethod("getBaseValue");
        Method baseSetter = PropertyAccessorOverrideBaseBean.class.getMethod("setBaseValue", String.class);

        Method baseFirstGetter = BaseFirstPropertyAccessorSubclass.class.getMethod("getChildValue");
        Method baseFirstSetter = BaseFirstPropertyAccessorSubclass.class.getMethod("setChildValue", String.class);
        Beans.registerPropertyAccessor("baseFirstAlias", baseGetter);
        Beans.registerPropertyAccessor("baseFirstAlias", baseSetter);
        Beans.registerPropertyAccessor("baseFirstAlias", baseFirstGetter);
        Beans.registerPropertyAccessor("baseFirstAlias", baseFirstSetter);

        assertEquals(baseFirstGetter, Beans.getPropGetter(BaseFirstPropertyAccessorSubclass.class, "baseFirstAlias"));
        assertEquals(baseGetter, Beans.getPropGetter(PropertyAccessorOverrideBaseBean.class, "baseFirstAlias"));

        Method subclassFirstGetter = SubclassFirstPropertyAccessorSubclass.class.getMethod("getChildValue");
        Method subclassFirstSetter = SubclassFirstPropertyAccessorSubclass.class.getMethod("setChildValue", String.class);
        Beans.registerPropertyAccessor("subclassFirstAlias", subclassFirstGetter);
        Beans.registerPropertyAccessor("subclassFirstAlias", subclassFirstSetter);
        Beans.registerPropertyAccessor("subclassFirstAlias", baseGetter);
        Beans.registerPropertyAccessor("subclassFirstAlias", baseSetter);

        assertEquals(subclassFirstGetter, Beans.getPropGetter(SubclassFirstPropertyAccessorSubclass.class, "subclassFirstAlias"));
        assertEquals(baseGetter, Beans.getPropGetter(PropertyAccessorOverrideBaseBean.class, "subclassFirstAlias"));
    }

    @Test
    public void testRegisterPropertyAccessor_AmbiguousInterfaces() throws Exception {
        Beans.registerPropertyAccessor("ambiguousAlias", LeftAmbiguousPropertyAccessor.class.getMethod("getLeftValue"));
        Beans.registerPropertyAccessor("ambiguousAlias", RightAmbiguousPropertyAccessor.class.getMethod("getRightValue"));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropGetter(AmbiguousPropertyAccessorBean.class, "ambiguousAlias"));
    }

    @Test
    public void testRegisterPropertyAccessor_DoesNotDependOnPriorLookup() throws Exception {
        Beans.registerPropertyAccessor("VALUE", BeansAccessorAFixture.class.getMethod("getOther"));
        assertEquals("getValue", Beans.getPropGetter(BeansAccessorBFixture.class, "VALUE").getName());

        Beans.registerPropertyAccessor("VALUE", BeansAccessorBFixture.class.getMethod("getOther"));
        assertEquals("getOther", Beans.getPropGetter(BeansAccessorAFixture.class, "VALUE").getName());
        assertEquals("getOther", Beans.getPropGetter(BeansAccessorBFixture.class, "VALUE").getName());

        assertThrows(IllegalArgumentException.class, () -> Beans.registerPropertyAccessor("value", BeansAccessorAFixture.class.getMethod("getOther")));
        Beans.registerPropertyAccessor("value", BeansAccessorAFixture.class.getMethod("getValue"));
    }

    @Test
    public void testRegisterXmlBindingClass() {
        Beans.registerXmlBindingClass(SimpleBean.class);
        assertTrue(Beans.isRegisteredXmlBindingClass(SimpleBean.class));
        Beans.registerXmlBindingClass(SimpleBean.class);
        assertTrue(Beans.isRegisteredXmlBindingClass(SimpleBean.class));

        class XMLBean {
            private String data;

            public String getData() {
                return data;
            }

            public void setData(String data) {
                this.data = data;
            }
        }

        assertFalse(Beans.isRegisteredXmlBindingClass(XMLBean.class));
        Beans.registerXmlBindingClass(XMLBean.class);
        assertTrue(Beans.isRegisteredXmlBindingClass(XMLBean.class));
        assertFalse(Beans.isRegisteredXmlBindingClass(CollectionBean.class));
    }

    @Test
    public void testIsRegisteredXmlBindingClass_NonBeanEntry() {
        assertTrue(Beans.isBeanClass(XmlRegistryProbeBean.class));
        Beans.registerNonBeanClass(XmlRegistryProbeBean.class);

        assertFalse(Beans.isRegisteredXmlBindingClass(XmlRegistryProbeBean.class));
        assertFalse(Beans.isBeanClass(XmlRegistryProbeBean.class));

        Beans.registerXmlBindingClass(XmlRegistryProbeBean.class);
        assertTrue(Beans.isRegisteredXmlBindingClass(XmlRegistryProbeBean.class));
    }

    /**
     * A builder-based bean used only by the builder-invalidation tests below, so that the registrations they
     * perform (which are permanent and process-wide) cannot leak into any other test's expectations.
     */
    public static class BuilderInvalidationBean {
        private String name;

        private BuilderInvalidationBean(final String name) {
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

            public BuilderInvalidationBean build() {
                return new BuilderInvalidationBean(name);
            }
        }
    }

    /**
     * A registration against the bean must leave {@code getPropSetters(builderClass)} able to answer.
     *
     * <p>Dropping the builder's setter pools is deliberate and is pinned by
     * {@code BeansRegressionBTest.testD6_registrationInvalidatesTheBuilderClassPoolsToo} - the point of this test
     * is only that the drop is <i>symmetric</i>. The builder's getter-pool entry used to survive while its setter
     * entry did not, and {@code loadPropGetSetMethodList} decides "already introspected?" from the getter pool
     * alone, so the re-derivation never happened. What it asserts is therefore termination, not content.</p>
     */
    @Test
    public void testGetPropSetters_BuilderRederivableAfterBeanRegistration() {
        final BuilderInvalidationBean bean = BuilderInvalidationBean.builder().name("n").build();

        assertEquals("{name=n}", Beans.beanToMap(bean).toString());
        assertTrue(Beans.getPropSetters(BuilderInvalidationBean.Builder.class).containsKey("name"));

        Beans.registerNonPropertyAccessor(BuilderInvalidationBean.class, "zzz1");

        assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> assertNotNull(Beans.getPropSetters(BuilderInvalidationBean.Builder.class)));
    }

    /**
     * A second registration against the bean must not make {@code getPropSetters(builderClass)} spin forever.
     *
     * <p>This is the state the asymmetric drop actually reached: the first {@code getPropSetters(builderClass)}
     * after a registration re-derives the builder <i>as a bean in its own right</i> and so publishes its getter
     * pool; the next registration then removed only the setter pools, leaving a pair that
     * {@code loadPropGetSetMethodList} would not republish and {@code getPropSetters} could not stop waiting for.</p>
     *
     * <p>Guarded by a preemptive timeout on purpose: the defect was an unbounded loop, so without the timeout a
     * regression would hang the whole suite instead of failing this one test.</p>
     */
    @Test
    public void testGetPropSetters_BuilderDoesNotSpinAfterRepeatedRegistration() {
        final BuilderInvalidationBean2 bean = BuilderInvalidationBean2.builder().name("n").build();

        Beans.beanToMap(bean);
        Beans.getPropSetters(BuilderInvalidationBean2.Builder.class);

        Beans.registerNonPropertyAccessor(BuilderInvalidationBean2.class, "zzz1");
        Beans.getPropSetters(BuilderInvalidationBean2.Builder.class);
        Beans.registerNonPropertyAccessor(BuilderInvalidationBean2.class, "zzz2");

        assertTimeoutPreemptively(Duration.ofSeconds(10),
                () -> assertNotNull(Beans.getPropSetters(BuilderInvalidationBean2.Builder.class)));
    }

    /** Separate class from {@link BuilderInvalidationBean} so the two tests cannot influence each other. */
    public static class BuilderInvalidationBean2 {
        private String name;

        private BuilderInvalidationBean2(final String name) {
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

            public BuilderInvalidationBean2 build() {
                return new BuilderInvalidationBean2(name);
            }
        }
    }

    /** Separate class again, so no other test's introspection order can reach this one. */
    public static class BuilderContentBean {
        private String name;

        private BuilderContentBean(final String name) {
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

            public BuilderContentBean build() {
                return new BuilderContentBean(name);
            }
        }
    }

    /**
     * A registration against the bean must not change <i>what</i> {@code getPropSetters(builderClass)} answers -
     * the two sibling tests above pin termination only.
     *
     * <p>The builder's setter model is a by-product of introspecting the bean, so the lookup has to re-derive it
     * through the bean. A standalone scan of a canonical builder (private fields, fluent one-argument methods)
     * finds no properties at all, and used to publish that empty model in its place: the same call then answered
     * {@code [name]}, then {@code []}, then {@code [name]} again over the life of one JVM, with no error.</p>
     */
    @Test
    public void testGetPropSetters_BuilderKeepsItsContentAfterBeanRegistration() {
        final BuilderContentBean bean = BuilderContentBean.builder().name("n").build();

        assertEquals("{name=n}", Beans.beanToMap(bean).toString());
        assertEquals(1, Beans.getPropSetters(BuilderContentBean.Builder.class).size());

        Beans.registerNonPropertyAccessor(BuilderContentBean.class, "zzzNotAProperty");

        assertEquals(1, Beans.getPropSetters(BuilderContentBean.Builder.class).size());
        assertEquals("name", Beans.getPropSetter(BuilderContentBean.Builder.class, "name").getName());
    }

    /**
     * Gives {@link BuilderSymmetryBean.Builder} two properties of its own: one a registration takes away, and one
     * that keeps the builder a bean class in its own right afterwards.
     */
    public static class BuilderSymmetryBase {
        private String note;
        private String keptNote;

        public String getNote() {
            return note;
        }

        public void setNote(final String note) {
            this.note = note;
        }

        public String getKeptNote() {
            return keptNote;
        }

        public void setKeptNote(final String keptNote) {
            this.keptNote = keptNote;
        }
    }

    /** Separate class again, so no other test's introspection order can reach this one. */
    public static class BuilderSymmetryBean {
        private String name;

        private BuilderSymmetryBean(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder extends BuilderSymmetryBase {
            private String name;

            public Builder name(final String name) {
                this.name = name;
                return this;
            }

            public BuilderSymmetryBean build() {
                return new BuilderSymmetryBean(name);
            }
        }
    }

    /**
     * Invalidating a bean drops its builder class's metadata too, and that drop has to cover the same pools as
     * the bean's own: a partial drop leaves the builder's <i>lookup</i> getter pool answering from the model the
     * registration just invalidated, and because {@code getPropGetter} reads that pool first, the re-derivation
     * the drop exists to force never runs.
     */
    @Test
    public void testGetPropGetter_BuilderMetadataIsDroppedSymmetrically() {
        final BuilderSymmetryBean bean = BuilderSymmetryBean.builder().name("n").build();

        // The builder is introspected in its own right, so its own getter pools are populated too.
        assertTrue(Beans.getPropGetters(BuilderSymmetryBean.Builder.class).containsKey("note"));
        assertEquals("getNote", Beans.getPropGetter(BuilderSymmetryBean.Builder.class, "note").getName());

        // Introspecting the bean publishes the builder's setter pools as a side effect.
        assertEquals("{name=n}", Beans.beanToMap(bean).toString());

        // Invalidates the bean, and with it the builder class's metadata.
        Beans.registerNonPropertyAccessor(BuilderSymmetryBean.class, "zzzNotAProperty");

        // "note" is no longer a property of the builder. The builder class itself is not in the introspected
        // index any more, so this registration does not re-publish anything for it.
        Beans.registerNonPropertyAccessor(BuilderSymmetryBase.class, "note");

        // Asserted before getPropGetters below, which would re-publish both pools and hide the stale entry.
        assertNull(Beans.getPropGetter(BuilderSymmetryBean.Builder.class, "note"));
        assertFalse(Beans.getPropGetters(BuilderSymmetryBean.Builder.class).containsKey("note"));
        assertEquals("getKeptNote", Beans.getPropGetter(BuilderSymmetryBean.Builder.class, "keptNote").getName());
    }
}
