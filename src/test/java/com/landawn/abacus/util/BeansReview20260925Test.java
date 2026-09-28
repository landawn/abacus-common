package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.ParserUtil;
import com.landawn.abacus.parser.ParserUtil.BeanInfo;

/**
 * Regression tests for the 2026-09-25 fix pass over {@code Beans} and {@code ParserUtil.BeanInfo}
 * (review findings U14-02..U14-08 and U32-02..U32-04 on the 2026-09-24 ledger rows C-401/C-402/C-410/C-415/C-421/C-423/C-424).
 */
public class BeansReview20260925Test extends TestBase {

    // ---------------------------------------------------------------- fixtures

    /** A plain bean with a keyword-mapped property ({@code "class"} spells {@code clazz}). */
    public static class Plain {
        private String firstName;
        private String clazz;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(final String firstName) {
            this.firstName = firstName;
        }

        public String getClazz() {
            return clazz;
        }

        public void setClazz(final String clazz) {
            this.clazz = clazz;
        }
    }

    /** A nested type whose only property is a public field: no getter, no setter. */
    public static class Inner {
        public String name;
    }

    /** {@code outer.name} is both the class-qualified alias of {@code name} and a nested path through {@code outer}. */
    public static class Outer {
        private String name;
        private String zip;
        private Inner outer;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public String getZip() {
            return zip;
        }

        public void setZip(final String zip) {
            this.zip = zip;
        }

        public Inner getOuter() {
            return outer;
        }

        public void setOuter(final Inner outer) {
            this.outer = outer;
        }
    }

    /** A collection-typed head: the chain resolves {@code items.name} through the element type but cannot be walked. */
    public static class Bag {
        private List<Inner> items;

        public List<Inner> getItems() {
            return items;
        }

        public void setItems(final List<Inner> items) {
            this.items = items;
        }
    }

    public static class Kids {
        private List<String> children;
        private boolean hasChildren;

        public List<String> getChildren() {
            return children;
        }

        public void setChildren(final List<String> children) {
            this.children = children;
        }

        public boolean getHasChildren() {
            return hasChildren;
        }

        public void setHasChildren(final boolean hasChildren) {
            this.hasChildren = hasChildren;
        }
    }

    /** The getter returns an unmodifiable list (C-415 doc claim). */
    public static class Tagged {
        private final List<String> tags = List.of("a");

        public List<String> getTags() {
            return tags;
        }
    }

    public abstract static class AbsBean {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    /** An XML-binding base with a getter-only collection (C-410 / U14-04). */
    public static class XBase {
        private String id;
        private final List<String> items = new ArrayList<>();

        public String getId() {
            return id;
        }

        public void setId(final String id) {
            this.id = id;
        }

        public List<String> getItems() {
            return items;
        }
    }

    /** Governed by {@link XBase}'s registration through inheritance; its own construction fails. */
    public static class XSubNoCtor extends XBase {
        public XSubNoCtor() {
            throw new IllegalStateException("cannot be constructed");
        }
    }

    /** Initializers differ from the type defaults so a copied default is observable (C-424 / U14-05). */
    public static class HolderBean {
        private Holder<String> h = new Holder<>();
        private int n = 7;
        private boolean b = true;
        private String s = "init";

        public Holder<String> getH() {
            return h;
        }

        public void setH(final Holder<String> h) {
            this.h = h;
        }

        public int getN() {
            return n;
        }

        public void setN(final int n) {
            this.n = n;
        }

        public boolean isB() {
            return b;
        }

        public void setB(final boolean b) {
            this.b = b;
        }

        public String getS() {
            return s;
        }

        public void setS(final String s) {
            this.s = s;
        }
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> beansNamePool(final String fieldName) throws Exception {
        final Field f = Beans.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        return (Map<String, String>) f.get(null);
    }

    // ---------------------------------------------------------------- U14-02 / U32-02: the miss path writes nothing into the global name pools

    @Test
    public void testU1402_tolerantMissLeavesTheGlobalNamePoolsUntouched() throws Exception {
        // Warm up every path with a KNOWN spelling first, so the fixture's own introspection (which legitimately
        // normalizes its canonical names) is not measured below.
        final JsonDeserConfig tolerant = JsonDeserConfig.create().setIgnoreUnmatchedProperty(true);
        assertEquals("getFirstName", Beans.getPropGetter(Plain.class, "first_name").getName());
        assertEquals("setFirstName", Beans.getPropSetter(Plain.class, "FIRST_NAME").getName());
        assertEquals("firstName", Beans.getPropField(Plain.class, "First_Name").getName());
        final BeanInfo beanInfo = ParserUtil.getBeanInfo(Plain.class);
        assertEquals("firstName", beanInfo.getPropInfo("first_name").name);
        assertEquals("w", Beans.mapToBean(N.asMap("first_name", "w"), true, Plain.class).getFirstName());
        assertNull(Beans.getPropValue(new Plain(), "first_name", true));
        assertEquals("j", N.fromJson("{\"first_name\":\"j\"}", tolerant, Plain.class).getFirstName());

        final Map<String, String> formalized = beansNamePool("formalizedPropNamePool");
        final Map<String, String> camelCase = beansNamePool("camelCasePropNamePool");
        final int formalizedSize = formalized.size();
        final int camelCaseSize = camelCase.size();

        // a handful of unknown spellings - never thousands: the pools are process-global
        final List<String> unknown = Arrays.asList("u1402_junk_alpha", "u1402_junk_beta", "u1402junkgamma", "U1402_JUNK_DELTA");

        for (final String name : unknown) {
            assertFalse(NameUtil.isCachedName(name), name);
            assertFalse(NameUtil.isCachedName(Strings.toCamelCase(name)), name);
        }

        for (final String name : unknown) {
            assertNull(Beans.getPropGetter(Plain.class, name), name);
            assertNull(Beans.getPropSetter(Plain.class, name), name);
            assertNull(Beans.getPropField(Plain.class, name), name);
            assertNull(beanInfo.getPropInfo(name), name);
            assertNull(Beans.getPropValue(new Plain(), name, true), name);
            assertNull(Beans.mapToBean(N.asMap(name, 1), true, Plain.class).getFirstName(), name);
            assertNull(N.fromJson("{\"" + name + "\":1}", tolerant, Plain.class).getFirstName(), name);
        }

        for (final String name : unknown) {
            final String camel = Strings.toCamelCase(name);
            // used to flip to true through Beans.normalizePropName -> toCamelCase -> NameUtil.getCachedName
            assertFalse(NameUtil.isCachedName(name), name);
            assertFalse(NameUtil.isCachedName(camel), camel);
            assertFalse(formalized.containsKey(name), name);
            assertFalse(camelCase.containsKey(name), name);
            assertFalse(formalized.containsKey(camel), camel);
            assertFalse(camelCase.containsKey(camel), camel);
        }

        assertEquals(formalizedSize, formalized.size());
        assertEquals(camelCaseSize, camelCase.size());

        // the normalized retry still resolves a HIT, including the single keyword remapping, on both sides
        assertEquals("getClazz", Beans.getPropGetter(Plain.class, "class").getName());
        assertEquals("setClazz", Beans.getPropSetter(Plain.class, "CLASS").getName());
        assertEquals("clazz", Beans.getPropField(Plain.class, "Class").getName());
        assertEquals("clazz", beanInfo.getPropInfo("class").name);
        assertEquals("c", N.fromJson("{\"class\":\"c\"}", tolerant, Plain.class).getClazz());
    }

    @Test
    public void testU1402_normalizePropNameUncachedMatchesNormalizePropNameWithoutMemoizing() throws Exception {
        final Map<String, String> formalized = beansNamePool("formalizedPropNamePool");
        final Map<String, String> camelCase = beansNamePool("camelCasePropNamePool");

        assertNull(Beans.normalizePropNameUncached(null));
        assertEquals("", Beans.normalizePropNameUncached(""));
        assertEquals("clazz", Beans.normalizePropNameUncached("class"));
        assertEquals("clazz", Beans.normalizePropNameUncached("CLASS"));
        assertEquals("userName", Beans.normalizePropNameUncached("user_name"));
        assertEquals("addressLine1", Beans.normalizePropNameUncached("address_line_1"));
        assertEquals("id", Beans.normalizePropNameUncached("ID"));

        assertEquals("u1402NeverSeen", Beans.normalizePropNameUncached("u1402_never_seen"));
        assertFalse(formalized.containsKey("u1402_never_seen"));
        assertFalse(camelCase.containsKey("u1402_never_seen"));
        assertFalse(NameUtil.isCachedName("u1402NeverSeen"));

        // same answers as the memoizing method for real names (which it may memoize - these are canonical shapes)
        for (final String s : Arrays.asList("class", "user_name", "ID", "address_line_1", "firstName")) {
            assertEquals(Beans.normalizePropName(s), Beans.normalizePropNameUncached(s), s);
        }
    }

    // ---------------------------------------------------------------- U14-03 / U32-03: a field-only nested tail is a nested path

    @Test
    public void testU1403_fieldOnlyNestedTailIsANestedPathInBothResolvers() {
        final Outer o = new Outer();
        o.setName("OUTER");
        o.setZip("Z1");
        final Inner in = new Inner();
        in.name = "INNER";
        o.setOuter(in);

        final BeanInfo beanInfo = Beans.getBeanInfo(Outer.class);

        for (final String path : Arrays.asList("outer.name", "Outer.name", "OUTER.NAME")) {
            // Beans required a GETTER on the tail and kept the class-qualified alias (getName); ParserUtil deferred
            // to the chain but its Beans.getPropGetter fallback re-resolved the alias - so both read/wrote the OUTER name.
            assertNull(Beans.getPropGetter(Outer.class, path), path);
            assertNull(beanInfo.getPropInfo(path), path);
            assertEquals(2, beanInfo.getPropInfoChain(path).size(), path);
            assertEquals("INNER", Beans.getPropValue(o, path), path);
            assertEquals("INNER", Beans.getPropValue(o, path, true), path);
            assertEquals("INNER", Beans.<String> getPropValueIfPresent(o, path).orElseThrow(), path);
            assertEquals("INNER", beanInfo.getPropValue(o, path), path);
        }

        // a tail that does not resolve on the nested type still keeps the class-qualified alias
        assertEquals("Z1", Beans.getPropValue(o, "outer.zip"));
        assertEquals("getZip", Beans.getPropGetter(Outer.class, "Outer.zip").getName());
        assertEquals("zip", beanInfo.getPropInfo("outer.zip").name);

        // unreachable (outer is null) -> the leaf type's default / absent; leaf null -> null / present
        final Outer none = new Outer();
        assertNull(Beans.getPropValue(none, "outer.name"));
        assertFalse(Beans.getPropValueIfPresent(none, "outer.name").isPresent());
        final Outer leafNull = new Outer();
        leafNull.setOuter(new Inner());
        assertNull(Beans.getPropValue(leafNull, "outer.name"));
        assertTrue(Beans.getPropValueIfPresent(leafNull, "outer.name").isPresent());
        assertNull(Beans.getPropValueIfPresent(leafNull, "outer.name").orElseNull());

        // mapToBean writes the nested field, tolerant and strict
        final Map<String, Object> m = new HashMap<>();
        m.put("outer.name", "X");
        final Outer c = Beans.mapToBean(m, Outer.class);
        assertNull(c.getName());
        assertEquals("X", c.getOuter().name);
        final Outer c2 = Beans.mapToBean(m, false, Outer.class);
        assertNull(c2.getName());
        assertEquals("X", c2.getOuter().name);

        // flat map round trip is exact
        final Map<String, Object> flat = Beans.beanToFlatMap(o);
        assertEquals("OUTER", flat.get("name"));
        assertEquals("INNER", flat.get("outer.name"));
        final Outer back = Beans.mapToBean(flat, Outer.class);
        assertEquals("OUTER", back.getName());
        assertEquals("Z1", back.getZip());
        assertEquals("INNER", back.getOuter().name);
        assertEquals(flat, Beans.beanToFlatMap(back));
    }

    @Test
    public void testU1403_collectionTypedHeadIsStillNotAReadablePath() {
        final Inner in = new Inner();
        in.name = "INNER";
        final Bag bag = new Bag();
        bag.setItems(Arrays.asList(in));

        // the chain resolves the tail through the element type, but a List has no `name` to read
        assertEquals(2, Beans.getBeanInfo(Bag.class).getPropInfoChain("items.name").size());
        assertNull(Beans.getBeanInfo(Bag.class).getPropInfo("items.name"));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(bag, "items.name"));
        assertNull(Beans.getPropValue(bag, "items.name", true));
        assertFalse(Beans.getPropValueIfPresent(bag, "items.name").isPresent());

        // and an unknown flat / dotted name is still unmatched
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(bag, "nope.name"));
        assertNull(Beans.getPropValue(bag, "nope.name", true));
        assertNull(Beans.getPropValue(bag, "nope", true));
        assertFalse(Beans.getPropValueIfPresent(bag, "nope").isPresent());
    }

    // ---------------------------------------------------------------- U14-04: inherited registration is not demoted

    @Test
    public void testU1404_inheritedRegistrationIsNotDemotedForAnUnconstructibleSubclass() {
        Beans.registerXmlBindingClass(XBase.class);

        assertTrue(Beans.isRegisteredXmlBindingClass(XBase.class));
        assertTrue(Beans.getPropNameList(XBase.class).contains("items"));

        // governed through inheritance: registered, but its own construction fails
        assertTrue(Beans.isRegisteredXmlBindingClass(XSubNoCtor.class));
        assertFalse(Beans.getPropNameList(XSubNoCtor.class).contains("items"));
        assertTrue(Beans.getPropNameList(XSubNoCtor.class).contains("id"));

        // not demoted: it has no explicit entry of its own, and the base's registration is untouched
        assertTrue(Beans.isRegisteredXmlBindingClass(XSubNoCtor.class));
        assertTrue(Beans.isRegisteredXmlBindingClass(XBase.class));
        assertTrue(Beans.getPropNameList(XBase.class).contains("items"));
    }

    // ---------------------------------------------------------------- U14-05: copyAs skips values equal to their runtime type's default

    @Test
    public void testU1405_copyAsSkipsValuesEqualToTheirRuntimeTypeDefault() {
        final HolderBean src = new HolderBean();
        src.setN(0);
        src.setB(false);
        src.setS("");
        // src.h: an empty Holder - the runtime type's default value

        final HolderBean t = Beans.copyAs(src, true, null, HolderBean.class);
        assertEquals(0, t.getN());
        assertFalse(t.isB());
        assertEquals("", t.getS());
        assertNotSame(src.getH(), t.getH());
        assertNull(t.getH().value());

        src.getH().setValue("x");
        final HolderBean t2 = Beans.copyAs(src, true, null, HolderBean.class);
        assertSame(src.getH(), t2.getH());
        assertEquals("x", t2.getH().value());

        src.setS(null);
        assertEquals("init", Beans.copyAs(src, true, null, HolderBean.class).getS());
    }

    // ---------------------------------------------------------------- U14-06 / U32-04: the hoisted rank-2 strip and the rank-3 length pre-check change no answer

    @Test
    public void testU1406_underscoreAndQualifiedSpellingsResolveTheSameThroughBothResolvers() {
        final BeanInfo beanInfo = Beans.getBeanInfo(Kids.class);

        for (final String s : Arrays.asList("has_children", "HAS__CHILDREN", "_has_children_", "h_a_s_c_h_i_l_d_r_e_n", "Kids.hasChildren",
                "KIDS.HAS_CHILDREN".toLowerCase().replace("_", ""), "gethas_children")) {
            assertEquals("getHasChildren", Beans.getPropGetter(Kids.class, s).getName(), s);
            assertEquals("setHasChildren", Beans.getPropSetter(Kids.class, s).getName(), s);
            assertEquals("hasChildren", Beans.getPropField(Kids.class, s).getName(), s);
            assertEquals("hasChildren", beanInfo.getPropInfo(s).name, s);
        }

        for (final String s : Arrays.asList("children_", "_children", "Kids.children", "get_children")) {
            assertEquals("getChildren", Beans.getPropGetter(Kids.class, s).getName(), s);
            assertEquals("children", beanInfo.getPropInfo(s).name, s);
        }

        // the class-qualified form needs the exact class name: same length, different qualifier, no match on either side
        for (final String s : Arrays.asList("Kidz.hasChildren", "Kid.hasChildren", "kids.nope", "Kids.")) {
            assertNull(Beans.getPropGetter(Kids.class, s), s);
            assertNull(beanInfo.getPropInfo(s), s);
        }
    }

    // ---------------------------------------------------------------- U14-08: the C-415 / C-423 doc claims

    @Test
    public void testU1408_setPropValueByGetterOnAnUnmodifiableCollectionThrowsBeforeAnythingChanges() throws Exception {
        final Tagged tagged = new Tagged();
        final Method getTags = Tagged.class.getMethod("getTags");

        assertThrows(UnsupportedOperationException.class, () -> Beans.setPropValueByGetter(tagged, getTags, Arrays.asList("x", "y")));
        assertEquals(Arrays.asList("a"), tagged.getTags());

        // a non-Collection value is rejected before clear() as well
        assertThrows(IllegalArgumentException.class, () -> Beans.setPropValueByGetter(tagged, getTags, "not a collection"));
        assertEquals(Arrays.asList("a"), tagged.getTags());

        // null does nothing
        Beans.setPropValueByGetter(tagged, getTags, null);
        assertEquals(Arrays.asList("a"), tagged.getTags());
    }

    @Test
    public void testU1408_newBeanFollowsNewInstanceRules() {
        assertTrue(Beans.newBean(List.class) instanceof ArrayList);
        assertTrue(Beans.newBean(Map.class) instanceof HashMap);
        assertEquals("", Beans.newBean(String.class));
        assertNull(Beans.newBean(Plain.class).getFirstName());
        assertThrows(IllegalArgumentException.class, () -> Beans.newBean(AbsBean.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.newBean(null));
    }
}
