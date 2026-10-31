package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Beans;
import com.landawn.abacus.util.N;

/**
 * An abstract class registered with {@code Beans.registerXmlBindingClass} is no longer demoted when it is introspected,
 * so it keeps governing its subclasses in every introspection order. These tests cover what that changes in
 * {@code ParserUtil.BeanInfo} (the "registered XML-binding class -> mutable" shortcut) and the neighbours that must not
 * move. The registration is static state, so every test uses fixtures of its own.
 */
@Tag("unit")
public class ParserUtilVerify20261027G144Test extends TestBase {

    // ---- deep review 2026-10-27 verify G144 begin ----

    // xjc shape: a protected list created lazily by its getter, no setter
    public abstract static class BuilderBaseFirstBase {
        protected List<String> items;
        private String name;

        public List<String> getItems() {
            if (items == null) {
                items = new ArrayList<>();
            }

            return items;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    // an immutable subclass that is created through its builder only
    public static class BuilderBaseFirstSub extends BuilderBaseFirstBase {
        private final int size;

        private BuilderBaseFirstSub(final int size) {
            this.size = size;
        }

        public int getSize() {
            return size;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private int size;

            public Builder size(final int size) {
                this.size = size;
                return this;
            }

            public BuilderBaseFirstSub build() {
                return new BuilderBaseFirstSub(size);
            }
        }
    }

    public abstract static class BuilderSubFirstBase {
        protected List<String> items;

        public List<String> getItems() {
            if (items == null) {
                items = new ArrayList<>();
            }

            return items;
        }
    }

    public static class BuilderSubFirstSub extends BuilderSubFirstBase {
        private final int size;

        private BuilderSubFirstSub(final int size) {
            this.size = size;
        }

        public int getSize() {
            return size;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private int size;

            public Builder size(final int size) {
                this.size = size;
                return this;
            }

            public BuilderSubFirstSub build() {
                return new BuilderSubFirstSub(size);
            }
        }
    }

    public abstract static class AllArgsBase {
        protected final List<String> items;

        protected AllArgsBase(final List<String> items) {
            this.items = items;
        }

        public List<String> getItems() {
            return items;
        }
    }

    // an immutable subclass that is created through its all-arguments constructor only
    public static class AllArgsSub extends AllArgsBase {
        private final int size;

        public AllArgsSub(final List<String> items, final int size) {
            super(items);
            this.size = size;
        }

        public int getSize() {
            return size;
        }
    }

    // concrete, registered directly, no no-arg constructor, created through its builder
    public static class DirectBuilderBean {
        private final String name;

        private DirectBuilderBean(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static class Builder {
            private String name;

            public Builder name(final String name) {
                this.name = name;
                return this;
            }

            public DirectBuilderBean build() {
                return new DirectBuilderBean(name);
            }
        }
    }

    // concrete JAXB shape with a no-arg constructor but no setter at all: the shortcut keeps it mutable
    public static class FinalListJaxbBean {
        private final List<String> items = new ArrayList<>();

        public List<String> getItems() {
            return items;
        }
    }

    // abstract, no builder, with a getter-only list (r108 demoted it on introspection)
    public abstract static class NoBuilderAbstractBase {
        protected List<String> items;
        private String name;

        public List<String> getItems() {
            if (items == null) {
                items = new ArrayList<>();
            }

            return items;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    // abstract -> abstract -> concrete, each abstract level adding a getter-only collection
    public abstract static class LevelA {
        protected List<String> items;
        private String name;

        public List<String> getItems() {
            if (items == null) {
                items = new ArrayList<>();
            }

            return items;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public abstract static class LevelB extends LevelA {
        private final Map<String, String> store = new LinkedHashMap<>();

        public Map<String, String> getAttrs() {
            return store;
        }
    }

    public static class LevelC extends LevelB {
        private int size;

        public int getSize() {
            return size;
        }

        public void setSize(final int size) {
            this.size = size;
        }
    }

    public abstract static class LateA {
        private final Map<String, String> store = new LinkedHashMap<>();
        private String name;

        public Map<String, String> getAttrs() {
            return store;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static class LateC extends LateA {
        private int size;

        public int getSize() {
            return size;
        }

        public void setSize(final int size) {
            this.size = size;
        }
    }

    // Immutable subclass of a registered abstract base, base introspected first: it must stay created through its
    // builder (the shortcut would pick the missing no-arg constructor now that the base keeps its registration).
    @Test
    public void testRegisteredAbstractBase_builderSubclass_baseIntrospectedFirst() {
        Beans.registerXmlBindingClass(BuilderBaseFirstBase.class);
        assertEquals(List.of("name"), Beans.getPropNameList(BuilderBaseFirstBase.class));
        assertTrue(Beans.isRegisteredXmlBindingClass(BuilderBaseFirstSub.class));

        assertTrue(ParserUtil.getBeanInfo(BuilderBaseFirstSub.class).isImmutable);
        assertEquals(3, N.fromJson("{\"size\":3}", BuilderBaseFirstSub.class).getSize());
        assertEquals(4, Beans.mapToBean(Map.of("size", 4), BuilderBaseFirstSub.class).getSize());
        assertEquals(5, N.fromXml("<x><size>5</size></x>", BuilderBaseFirstSub.class).getSize());
    }

    // Same, subclass introspected before the base (the order r108 already failed in): also built through the builder.
    @Test
    public void testRegisteredAbstractBase_builderSubclass_subclassIntrospectedFirst() {
        Beans.registerXmlBindingClass(BuilderSubFirstBase.class);

        assertEquals(3, N.fromJson("{\"size\":3}", BuilderSubFirstSub.class).getSize());
        assertEquals(4, Beans.mapToBean(Map.of("size", 4), BuilderSubFirstSub.class).getSize());
        assertTrue(ParserUtil.getBeanInfo(BuilderSubFirstSub.class).isImmutable);
        assertTrue(Beans.isRegisteredXmlBindingClass(BuilderSubFirstBase.class));
    }

    // Immutable subclass with an all-arguments constructor only: created through that constructor, collection included.
    @Test
    public void testRegisteredAbstractBase_allArgsSubclass() {
        Beans.registerXmlBindingClass(AllArgsBase.class);
        Beans.getPropNameList(AllArgsBase.class);

        assertTrue(ParserUtil.getBeanInfo(AllArgsSub.class).isImmutable);

        final AllArgsSub viaJson = N.fromJson("{\"items\":[\"a\"],\"size\":3}", AllArgsSub.class);
        assertEquals(List.of("a"), viaJson.getItems());
        assertEquals(3, viaJson.getSize());

        final AllArgsSub viaMap = Beans.mapToBean(Map.of("items", List.of("b"), "size", 4), AllArgsSub.class);
        assertEquals(List.of("b"), viaMap.getItems());
        assertEquals(4, viaMap.getSize());
    }

    // A registered concrete class without a no-arg constructor is created through its builder, as without the registration.
    @Test
    public void testRegisteredConcreteClassWithoutNoArgConstructor_isBuiltThroughBuilder() {
        Beans.registerXmlBindingClass(DirectBuilderBean.class);
        assertTrue(Beans.isRegisteredXmlBindingClass(DirectBuilderBean.class));

        assertTrue(ParserUtil.getBeanInfo(DirectBuilderBean.class).isImmutable);
        assertEquals("j", N.fromJson("{\"name\":\"j\"}", DirectBuilderBean.class).getName());
        assertEquals("x", N.fromXml("<x><name>x</name></x>", DirectBuilderBean.class).getName());
        assertEquals("m", Beans.mapToBean(Map.of("name", "m"), DirectBuilderBean.class).getName());
    }

    // Unchanged: a registered concrete class WITH a no-arg constructor keeps the shortcut and fills its getter-only list.
    @Test
    public void testRegisteredConcreteClassWithNoArgConstructor_staysMutable() {
        Beans.registerXmlBindingClass(FinalListJaxbBean.class);

        assertFalse(ParserUtil.getBeanInfo(FinalListJaxbBean.class).isImmutable);
        assertEquals(List.of("a"), N.fromJson("{\"items\":[\"a\"]}", FinalListJaxbBean.class).getItems());
        assertEquals(List.of("b"), Beans.mapToBean(Map.of("items", List.of("b")), FinalListJaxbBean.class).getItems());
    }

    // A registered abstract class without a builder, introspected and used as the target itself: still not creatable,
    // now with the IllegalArgumentException every registered abstract class gets.
    @Test
    public void testRegisteredAbstractClassWithoutBuilder_asTarget_throwsIAE() {
        Beans.registerXmlBindingClass(NoBuilderAbstractBase.class);
        assertEquals(List.of("name"), Beans.getPropNameList(NoBuilderAbstractBase.class));
        assertTrue(Beans.isRegisteredXmlBindingClass(NoBuilderAbstractBase.class));

        assertFalse(ParserUtil.getBeanInfo(NoBuilderAbstractBase.class).isImmutable);
        assertThrows(IllegalArgumentException.class, () -> N.fromJson("{\"name\":\"n\"}", NoBuilderAbstractBase.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(Map.of("name", "n"), NoBuilderAbstractBase.class));
    }

    // abstract -> abstract -> concrete: introspecting both abstract levels first must not strip either collection.
    @Test
    public void testRegisteredAbstractBase_twoAbstractLevels_baseLevelsIntrospectedFirst() {
        Beans.registerXmlBindingClass(LevelA.class);
        assertEquals(List.of("name"), Beans.getPropNameList(LevelA.class));
        assertEquals(List.of("name"), Beans.getPropNameList(LevelB.class));

        final LevelC bean = Beans.mapToBean(Map.of("items", List.of("a"), "attrs", Map.of("k", "v"), "name", "n", "size", 2), LevelC.class);
        assertEquals(List.of("a"), bean.getItems());
        assertEquals(Map.of("k", "v"), bean.getAttrs());
        assertEquals(2, bean.getSize());
        assertEquals(List.of("items", "name", "size", "attrs"), Beans.getPropNameList(LevelC.class));

        final LevelC fromXml = N.fromXml(N.toXml(bean), LevelC.class);
        assertEquals(List.of("a"), fromXml.getItems());
        assertEquals(Map.of("k", "v"), fromXml.getAttrs());

        assertTrue(Beans.isRegisteredXmlBindingClass(LevelA.class));
        assertTrue(Beans.isRegisteredXmlBindingClass(LevelB.class));
        assertTrue(Beans.isRegisteredXmlBindingClass(LevelC.class));
    }

    // Registration made after the base and subclass were introspected: the refreshed models keep the collection even
    // when the base is introspected again afterwards.
    @Test
    public void testRegisteredAbstractBase_registeredAfterIntrospection() {
        assertEquals(List.of("name"), Beans.getPropNameList(LateA.class));
        assertEquals(List.of("name", "size"), Beans.getPropNameList(LateC.class));
        assertTrue(Beans.mapToBean(Map.of("attrs", Map.of("k", "v")), LateC.class).getAttrs().isEmpty());

        Beans.registerXmlBindingClass(LateA.class);
        assertEquals(List.of("name", "size", "attrs"), Beans.getPropNameList(LateC.class));
        assertEquals(List.of("name"), Beans.getPropNameList(LateA.class));

        assertEquals(Map.of("k", "v"), Beans.mapToBean(Map.of("attrs", Map.of("k", "v"), "size", 1), LateC.class).getAttrs());
        assertEquals(Map.of("k", "j"), N.fromJson("{\"attrs\":{\"k\":\"j\"}}", LateC.class).getAttrs());
        assertTrue(Beans.isRegisteredXmlBindingClass(LateA.class));
        assertTrue(Beans.isRegisteredXmlBindingClass(LateC.class));
    }

    // ---- deep review 2026-10-27 verify G144 end ----

}
