package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

public class BeansTest extends BeansTestSupport {

    @Test
    public void testBeanOperationsRejectThrowableTargets() {
        final ExceptionWithProperties exception = new ExceptionWithProperties();
        exception.setCode("unchanged");

        assertThrows(IllegalArgumentException.class, () -> Beans.beanToMap(exception));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(Map.of("code", "new"), ExceptionWithProperties.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(Map.of("code", "new"), false, ExceptionWithProperties.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.mapToBean(Map.of("code", "new"), List.of("code"), ExceptionWithProperties.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBean(ExceptionWithProperties.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBeanList(ExceptionWithProperties.class, 0));
        assertThrows(IllegalArgumentException.class, () -> Beans.randomize(exception));
        assertThrows(IllegalArgumentException.class, () -> Beans.clearAllProps(exception));
        assertThrows(IllegalArgumentException.class, () -> Beans.clearProps(exception, "code"));
        assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(exception, new ExceptionWithProperties()));
        assertEquals("unchanged", exception.getCode());
    }

    @Test
    public void testClearAllProps() {
        SimpleBean bean = new SimpleBean("John", 25);
        bean.setActive(true);
        Beans.clearAllProps(bean);
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertNull(bean.getActive());

        assertDoesNotThrow(() -> Beans.clearAllProps(null));
    }

    @Test
    public void testClearProps() {
        SimpleBean bean = new SimpleBean("John", 25);
        bean.setActive(true);

        Beans.clearProps(bean, "name", "age");
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());
        assertEquals(true, bean.getActive());

        bean = new SimpleBean("Jane", 30);
        bean.setActive(true);
        Beans.clearProps(bean, Arrays.asList("name"));
        assertNull(bean.getName());
        assertEquals(30, bean.getAge());
        assertEquals(true, bean.getActive());

        Beans.clearProps(bean, "age", "active");
        assertEquals(0, bean.getAge());
        assertNull(bean.getActive());
    }

    @Test
    public void testClearProps_EmptyOrNull() {
        SimpleBean bean = new SimpleBean("Bob", 35);
        Beans.clearProps(bean, new String[0]);
        assertEquals("Bob", bean.getName());

        Beans.clearProps(bean, Collections.emptyList());
        assertEquals("Bob", bean.getName());
        assertEquals(35, bean.getAge());

        assertDoesNotThrow(() -> Beans.clearProps(null, "name"));
        assertDoesNotThrow(() -> Beans.clearProps(null, Arrays.asList("name")));
    }

    @Test
    public void testComputedGetterDiscoveryForEntityAndRecord() {
        assertEquals(List.of("id"), Beans.getPropNameList(OrdinaryComputedGetter.class));
        assertTrue(Beans.getPropNameList(EntityComputedGetter.class).contains("computed"));
        assertEquals(List.of("id", "computed"), Beans.getPropNameList(RecordComputedGetter.class));
        assertEquals("derived", Beans.getPropValue(new EntityComputedGetter(), "computed"));
        assertEquals("derived", Beans.getPropValue(new RecordComputedGetter(7), "computed"));
        OrdinaryComputedGetter ordinary = new OrdinaryComputedGetter();
        ordinary.setId(7);
        assertEquals(7, Beans.<Integer> getPropValue(ordinary, "id"));
        assertFalse(Beans.getPropValueIfPresent(ordinary, "computed").isPresent());
    }

    // G020-03: the per-call cycle guard starts with a small table; pins deep nesting (past the initial capacity),
    // shared non-cyclic references, cycle detection, and that the guard is reset after a failure.
    @SuppressWarnings("unchecked")
    @Test
    public void testDeepBeanToMapAndFlatMap_cycleGuardDepthAndReset() {
        final int depth = 12;
        final G020Node root = new G020Node();
        root.setName("n0");
        G020Node current = root;

        for (int i = 1; i < depth; i++) {
            final G020Node next = new G020Node();
            next.setName("n" + i);
            current.setChild(next);
            current = next;
        }

        Map<String, Object> deep = Beans.deepBeanToMap(root);
        final StringBuilder flatKey = new StringBuilder();

        for (int i = 0; i < depth; i++) {
            assertEquals("n" + i, deep.get("name"));

            if (i < depth - 1) {
                deep = (Map<String, Object>) deep.get("child");
                flatKey.append("child.");
            }
        }

        final Map<String, Object> flat = Beans.beanToFlatMap(root);
        assertEquals("n" + (depth - 1), flat.get(flatKey + "name"));
        assertEquals(depth, flat.size());

        // the same instance twice, but not on one path: not a cycle
        final G020Node shared = new G020Node();
        shared.setName("shared");
        final G020Node diamond = new G020Node();
        diamond.setName("top");
        diamond.setChild(shared);
        diamond.setSibling(shared);

        final Map<String, Object> diamondMap = Beans.deepBeanToMap(diamond);
        assertEquals("shared", ((Map<String, Object>) diamondMap.get("child")).get("name"));
        assertEquals("shared", ((Map<String, Object>) diamondMap.get("sibling")).get("name"));
        assertEquals("shared", Beans.beanToFlatMap(diamond).get("sibling.name"));

        // a real cycle, deep in the graph
        current.setChild(root);
        assertThrows(IllegalArgumentException.class, () -> Beans.deepBeanToMap(root));
        assertThrows(IllegalArgumentException.class, () -> Beans.beanToFlatMap(root));

        // the guard was cleared by the failed calls: an acyclic graph converts again on this thread
        current.setChild(null);
        assertEquals("n0", Beans.deepBeanToMap(root).get("name"));
        assertEquals(depth, Beans.beanToFlatMap(root).size());
    }

    @Test
    public void testIsBeanClass() {
        assertTrue(Beans.isBeanClass(SimpleBean.class));
        assertTrue(Beans.isBeanClass(EntityBean.class));
        assertTrue(Beans.isBeanClass(RecordBean.class));
        assertFalse(Beans.isBeanClass(String.class));
        assertFalse(Beans.isBeanClass(Integer.class));
        assertFalse(Beans.isBeanClass(Map.Entry.class));
        assertFalse(Beans.isBeanClass(null));
    }

    @Test
    public void testIsBeanClass_NonBeanTypes() {
        assertFalse(Beans.isBeanClass(int.class));
        assertFalse(Beans.isBeanClass(void.class));
        assertFalse(Beans.isBeanClass(int[].class));
        assertFalse(Beans.isBeanClass(Object.class));
        assertFalse(Beans.isBeanClass(java.util.Date.class));
        assertFalse(Beans.isBeanClass(HashMap.class));
        assertFalse(Beans.isBeanClass(java.util.ArrayList.class));
    }

    @Test
    public void testIsRecordClass() {
        assertTrue(Beans.isRecordClass(RecordBean.class));
        assertFalse(Beans.isRecordClass(SimpleBean.class));
        assertFalse(Beans.isRecordClass(EntityBean.class));
        assertFalse(Beans.isRecordClass(String.class));
        assertFalse(Beans.isRecordClass(int.class));
        assertFalse(Beans.isRecordClass(null));
    }

    // G020-02: isRecordClass's mapping function no longer captures its argument; pins the answers and their caching.
    @Test
    public void testIsRecordClass_nonCapturingMapping() {
        for (int i = 0; i < 3; i++) {
            assertTrue(Beans.isRecordClass(RecordBean.class));
            assertFalse(Beans.isRecordClass(SimpleBean.class));
            assertFalse(Beans.isRecordClass(G020SetterShapes.class));
            assertFalse(Beans.isRecordClass(Object.class));
            assertFalse(Beans.isRecordClass(null));
        }
    }

    @Test
    public void testMapToBeanNestedPathUsesPreviouslyAssignedRecordValue() {
        final Map<String, Object> values = new java.util.LinkedHashMap<>();
        values.put("address", new NestedAddress());
        values.put("address.city.name", "Paris");
        assertEquals("Paris", Beans.mapToBean(values, false, NestedAddressRecord.class).address().getCity().getName());
    }

    @Test
    public void testMergeIntoDistinctBeansAppliesValuesInPropertyOrder() {
        final Address source = new Address();
        source.setStreet("source street");
        source.setCity("source city");
        source.setZipCode("12345");
        final Address target = new Address();
        final java.util.concurrent.atomic.AtomicInteger resolved = new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicInteger applied = new java.util.concurrent.atomic.AtomicInteger();

        // Distinct beans keep the original read/apply order; only self-merges need the source snapshot.
        Beans.mergeInto(source, target, (Collection<String>) null, name -> {
            assertEquals(resolved.getAndIncrement(), applied.get());
            return switch (name) {
                case "street" -> "city";
                case "city" -> "street";
                default -> name;
            };
        }, (sourceValue, targetValue) -> {
            applied.incrementAndGet();
            return sourceValue;
        });

        assertEquals(3, applied.get());
        assertEquals("source city", target.getStreet());
        assertEquals("source street", target.getCity());
        assertEquals("12345", target.getZipCode());
        assertEquals("source street", source.getStreet());
        assertEquals("source city", source.getCity());
    }

    @Test
    public void testMergeIntoSnapshotsSourceBeforeRenamingOnSameBean() {
        for (int variant = 0; variant < 2; variant++) {
            final Address bean = new Address();
            bean.setStreet("original street");
            bean.setCity("original city");
            bean.setZipCode("12345");
            final java.util.function.Function<String, String> swapNames = name -> switch (name) {
                case "street" -> "city";
                case "city" -> "street";
                default -> name;
            };

            final Address result = variant == 0 ? Beans.mergeInto(bean, bean, swapNames, Fn.selectFirst())
                    : Beans.mergeInto(bean, bean, (Collection<String>) null, swapNames, Fn.selectFirst());

            org.junit.jupiter.api.Assertions.assertSame(bean, result);
            assertEquals("original city", bean.getStreet());
            assertEquals("original street", bean.getCity());
            assertEquals("12345", bean.getZipCode());
        }
    }

    @Test
    public void testNewBean() {
        SimpleBean bean = Beans.newBean(SimpleBean.class);
        assertNotNull(bean);
        assertNull(bean.getName());
        assertEquals(0, bean.getAge());

        NestedBean nested = Beans.newBean(NestedBean.class);
        assertNotNull(nested);
        assertNull(nested.getId());

        EntityBean entity = Beans.newBean(EntityBean.class);
        assertNotNull(entity);
        assertNull(entity.getId());
        assertNull(entity.getValue());
    }

    // newRandomBean

    @Test
    public void testNewRandomBean() {
        SimpleBean filled = Beans.newRandomBean(SimpleBean.class);
        assertNotNull(filled);
        assertNotNull(filled.getName());
        assertTrue(filled.getAge() != 0);

        filled = Beans.newRandomBean(SimpleBean.class, Arrays.asList("name"));
        assertNotNull(filled.getName());
        assertEquals(0, filled.getAge());
        assertNull(filled.getActive());

        filled = Beans.newRandomBean(SimpleBean.class, Arrays.asList("age"));
        assertTrue(filled.getAge() != 0);
        assertNull(filled.getName());
    }

    // G020-01: an enum property whose enum declares a getter/setter pair (so isBeanClass accepts it) aborted the
    // random fill with UnsupportedOperationException after earlier properties were written; documented: default value
    @Test
    public void testNewRandomBean_enumWithAccessorsGetsDefault() {
        final G020EnumHolder bean = Beans.newRandomBean(G020EnumHolder.class);

        assertNotNull(bean.getName());
        assertNull(bean.getColor());

        final G020EnumHolder existing = new G020EnumHolder();
        existing.setColor(G020MutableEnum.GREEN);
        Beans.randomize(existing);

        assertNotNull(existing.getName());
        assertNull(existing.getColor());
    }

    // a Number type that can be created from a String but only in a narrow range (MutableByte, MutableShort, a byte-sized
    // user Number) gets a random value - it neither aborts the fill nor is left null - on every run
    @Test
    public void testNewRandomBean_narrowNumberTypesGetRandomValue() {
        for (int i = 0; i < 50; i++) {
            final G120NarrowNumbers bean = Beans.newRandomBean(G120NarrowNumbers.class);

            assertNotNull(bean.getMutableByte());
            assertNotNull(bean.getMutableShort());
            assertNotNull(bean.getByteSized());
            assertNotNull(bean.getBigInteger());
        }
    }

    // G020-01: a nested bean property whose type cannot be instantiated gets its default (like an abstract bean type) instead of
    // aborting newRandomBean/randomize with UnsupportedOperationException after the earlier properties were written.
    @Test
    public void testNewRandomBean_nestedUninstantiableBeanTypeGetsDefault() {
        assertTrue(Beans.isBeanClass(G020UnbuildableMoney.class));

        final G020MoneyHolder created = Beans.newRandomBean(G020MoneyHolder.class);
        assertNotNull(created.getName());
        assertNull(created.getMoney());
        assertNotNull(created.getTail());

        final List<G020MoneyHolder> list = Beans.newRandomBeanList(G020MoneyHolder.class, 2);
        assertEquals(2, list.size());
        assertNull(list.get(1).getMoney());
        assertNotNull(list.get(1).getTail());

        final G020MoneyHolder holder = new G020MoneyHolder();
        holder.setTail("origTail");
        holder.setMoney(new G020UnbuildableMoney(5, "USD"));
        Beans.randomize(holder);
        assertNotNull(holder.getName());
        assertNull(holder.getMoney());
        assertFalse("origTail".equals(holder.getTail()));

        // the class itself still cannot be created: the documented top-level UnsupportedOperationException is unchanged
        assertThrows(UnsupportedOperationException.class, () -> Beans.newRandomBean(G020UnbuildableMoney.class));
    }

    // G120 verify of G020-01: next to an uninstantiable nested type (left null), instantiable nested types - a record, a mutable
    // bean with its own nested bean, and a builder-based bean - are still created and filled in the same pass.
    @Test
    public void testNewRandomBean_nestedUninstantiableNextToInstantiableNestedTypes() {
        final G120VMixedHolder holder = Beans.newRandomBean(G120VMixedHolder.class);

        assertNull(holder.getMoney());
        assertNotNull(holder.getAddressRecord());
        assertNotNull(holder.getAddressRecord().name());
        assertNotNull(holder.getAddressRecord().address().getCity().getName());
        assertNotNull(holder.getAddress().getCity().getName());
        assertNotNull(holder.getBuiltCity());
        assertNotNull(holder.getBuiltCity().getName());
    }

    // G020-01: a concrete Number without a String factory (LongAdder, a user-defined Number) is an unsupported type and
    // must get its default value, not abort the whole random fill with UnsupportedOperationException.
    @Test
    public void testNewRandomBean_numberWithoutStringFactoryGetsDefault() {
        final G020BugUnsupportedTypes bean = Beans.newRandomBean(G020BugUnsupportedTypes.class);

        assertNotNull(bean.getName());
        assertNotNull(bean.getAmount());
        assertNull(bean.getAdder());
        assertNull(bean.getBugNumber());

        final G020BugUnsupportedTypes existing = new G020BugUnsupportedTypes();
        existing.setAdder(new java.util.concurrent.atomic.LongAdder());
        Beans.randomize(existing);
        assertNotNull(existing.getName());
        assertNotNull(existing.getAmount());
        assertNull(existing.getAdder());

        final G020BugUnsupportedTypes onlyNumber = new G020BugUnsupportedTypes();
        Beans.randomize(onlyNumber, Arrays.asList("bugNumber", "name"));
        assertNull(onlyNumber.getBugNumber());
        assertNotNull(onlyNumber.getName());

        assertEquals(2, Beans.newRandomBeanList(G020BugUnsupportedTypes.class, 2).size());
    }

    @Test
    public void testNewRandomBeanRequiresUsableConstructor() {
        assertTrue(Beans.isBeanClass(NoUsableConstructorBean.class));
        assertThrows(UnsupportedOperationException.class, () -> Beans.newRandomBean(NoUsableConstructorBean.class));
        assertThrows(UnsupportedOperationException.class, () -> Beans.newRandomBean(NoUsableConstructorBean.class, List.of("code")));
        assertThrows(UnsupportedOperationException.class, () -> Beans.newRandomBeanList(NoUsableConstructorBean.class, 1));
        assertThrows(UnsupportedOperationException.class, () -> Beans.newRandomBeanList(NoUsableConstructorBean.class, List.of("code"), 1));
        assertTrue(Beans.newRandomBeanList(NoUsableConstructorBean.class, List.of("code"), 0).isEmpty());
    }

    @Test
    public void testNewRandomBeanList() {
        List<SimpleBean> list = Beans.newRandomBeanList(SimpleBean.class, 5);
        assertEquals(5, list.size());
        for (SimpleBean bean : list) {
            assertNotNull(bean);
            assertNotNull(bean.getName());
        }

        list = Beans.newRandomBeanList(SimpleBean.class, Arrays.asList("name", "age"), 3);
        assertEquals(3, list.size());
        for (SimpleBean bean : list) {
            assertNotNull(bean.getName());
            assertTrue(bean.getAge() != 0);
            assertNull(bean.getActive());
        }

        list = Beans.newRandomBeanList(SimpleBean.class, Arrays.asList("name"), 3);
        assertEquals(3, list.size());
        for (SimpleBean bean : list) {
            assertNotNull(bean.getName());
            assertEquals(0, bean.getAge());
        }

        assertTrue(Beans.newRandomBeanList(SimpleBean.class, 0).isEmpty());
        assertTrue(Beans.newRandomBeanList(SimpleBean.class, Arrays.asList("name"), 0).isEmpty());
    }

    @Test
    public void testNormalizePropName() {
        assertEquals("userName", Beans.normalizePropName("user_name"));
        assertEquals("firstName", Beans.normalizePropName("first_name"));
        assertEquals("addressLine1", Beans.normalizePropName("address_line_1"));
        assertEquals("myPropertyName", Beans.normalizePropName("my_property_name"));
        assertEquals("userIdValue", Beans.normalizePropName("user_id_value"));
        assertEquals("id", Beans.normalizePropName("id"));
        assertEquals("simple", Beans.normalizePropName("simple"));
        assertEquals("alreadyCamel", Beans.normalizePropName("alreadyCamel"));
    }

    @Test
    public void testNormalizePropName_ClassKeyword() {
        assertEquals("clazz", Beans.normalizePropName("class"));
        assertEquals("clazz", Beans.normalizePropName("CLASS"));
        assertEquals("id", Beans.normalizePropName("ID"));
        assertEquals("url", Beans.normalizePropName("URL"));
    }

    @Test
    public void testPropNameAccessorsRejectNullPropNameEvenWhenIgnoringUnmatched() {
        final BeansTestSupport.Address bean = new BeansTestSupport.Address();

        assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(bean, (String) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropValue(bean, (String) null, true));
        assertThrows(IllegalArgumentException.class, () -> Beans.getPropValueIfPresent(bean, null));
        assertThrows(IllegalArgumentException.class, () -> Beans.setPropValue(bean, (String) null, "v"));
        assertThrows(IllegalArgumentException.class, () -> Beans.setPropValue(bean, (String) null, "v", true));

        // An unmatched but non-null name is still tolerated when unmatched properties are ignored.
        assertNull(Beans.getPropValue(bean, "noSuchProperty", true));
        assertFalse(Beans.getPropValueIfPresent(bean, "noSuchProperty").isPresent());
        assertFalse(Beans.setPropValue(bean, "noSuchProperty", "v", true));
    }

    // randomize

    @Test
    public void testRandomize() {
        SimpleBean bean = new SimpleBean();
        Beans.randomize(bean);
        assertNotNull(bean.getName());
        assertTrue(bean.getAge() != 0);
        assertNotNull(bean.getActive());

        bean = new SimpleBean();
        Beans.randomize(bean, Arrays.asList("name", "age"));
        assertNotNull(bean.getName());
        assertTrue(bean.getAge() != 0);
        assertNull(bean.getActive());

        bean = new SimpleBean();
        Beans.randomize(bean, Arrays.asList("name"));
        assertNotNull(bean.getName());
        assertEquals(0, bean.getAge());
    }

    /**
     * Pins the shape {@code randomize(Object)} documents for an e-mail-named property: the local part is the
     * first 12 characters of a canonical (hyphenated) UUID, so it is 8 hex digits, a hyphen and 3 hex digits -
     * never 12 hex characters.
     */
    @Test
    public void testRandomize_EmailPropertyLocalPartIsAUuidPrefix() {
        for (int i = 0; i < 5; i++) {
            final RandomizeEmailBean bean = new RandomizeEmailBean();
            Beans.randomize(bean);

            final String email = bean.getEmail();
            assertTrue(email.endsWith("@email.com"), email);

            final String local = email.substring(0, email.indexOf('@'));
            assertEquals(12, local.length(), local);
            assertEquals('-', local.charAt(8), local);
            assertTrue(local.matches("[0-9a-f]{8}-[0-9a-f]{3}"), local);
        }
    }

    // G020-01: an enum that isBeanClass accepts through a public mutable field, next to a plain enum; both get the default value.
    @Test
    public void testRandomize_enumWithPublicFieldAndPlainEnumGetDefault() {
        assertTrue(Beans.isBeanClass(G116FieldEnum.class));

        final G116EnumHolder bean = new G116EnumHolder();
        bean.setFieldEnum(G116FieldEnum.TWO);
        bean.setPlainEnum(G116PlainEnum.TWO);
        Beans.randomize(bean);

        assertNotNull(bean.getName());
        assertNull(bean.getFieldEnum());
        assertNull(bean.getPlainEnum());
    }

    @Test
    public void testRandomize_NullInput() {
        SimpleBean bean = new SimpleBean();
        assertThrows(IllegalArgumentException.class, () -> Beans.randomize((Object) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.randomize(bean, (Collection<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBean(SimpleBean.class, (Collection<String>) null));
        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBeanList(SimpleBean.class, (Collection<String>) null, 2));
        assertThrows(IllegalArgumentException.class, () -> Beans.newRandomBean((Class<?>) null));
    }

    @Test
    public void testRefreshBeanPropInfo() {
        Beans.refreshBeanPropInfo(SimpleBean.class);
        Beans.refreshBeanPropInfo(NestedBean.class);
        Beans.refreshBeanPropInfo(EntityBean.class);

        assertNotNull(Beans.getBeanInfo(SimpleBean.class));
        assertFalse(Beans.getBeanInfo(SimpleBean.class).propInfoList.isEmpty());
        assertNotNull(Beans.getBeanInfo(EntityBean.class));
    }

    // Nested property writes traverse the first collection element and create missing intermediate beans.
    @SuppressWarnings("deprecation")
    @Test
    public void testSetPropValueAndClearProps_pathsThroughCollections() {
        final AddressListHolder person = new AddressListHolder();
        person.setAddresses(new java.util.ArrayList<>(List.of(new NestedAddress())));
        Beans.setPropValue(person, "addresses.city.name", "Paris");
        assertEquals("Paris", person.getAddresses().get(0).getCity().getName());
        Beans.clearProps(person, "addresses.city.name");
        assertNull(person.getAddresses().get(0).getCity().getName());

        final AddressListHolder empty = new AddressListHolder();
        empty.setAddresses(new java.util.ArrayList<>());
        Beans.setPropValue(empty, "addresses.city.name", "Rome");
        assertEquals("Rome", empty.getAddresses().get(0).getCity().getName());
    }

    // G020-01: setPropValue(Object, Method, Object) no longer clones the parameter-type array; pins every branch that read it.
    @Test
    public void testSetPropValueByMethod_parameterCountBranches() throws Exception {
        final G020SetterShapes bean = new G020SetterShapes();
        final java.lang.reflect.Method setName = G020SetterShapes.class.getMethod("setName", String.class);
        final java.lang.reflect.Method setCount = G020SetterShapes.class.getMethod("setCount", int.class);
        final java.lang.reflect.Method reset = G020SetterShapes.class.getMethod("reset");
        final java.lang.reflect.Method setBoth = G020SetterShapes.class.getMethod("setBoth", String.class, int.class);

        // non-null value, direct invoke
        assertEquals("a", Beans.setPropValue(bean, setName, "a"));
        assertEquals("a", bean.name);
        assertEquals(5, Beans.setPropValue(bean, setCount, 5));
        assertEquals(5, bean.count);

        // null value: the parameter type's default is applied
        assertNull(Beans.setPropValue(bean, setName, null));
        assertNull(bean.name);
        assertEquals(0, Beans.setPropValue(bean, setCount, null));
        assertEquals(0, bean.count);

        // non-null value of the wrong type on a one-parameter setter: converted and retried
        assertEquals(7, Beans.setPropValue(bean, setCount, "7"));
        assertEquals(7, bean.count);

        // null value on a zero-parameter method: no default is applied, the reflective call rejects the argument
        assertThrows(IllegalArgumentException.class, () -> Beans.setPropValue(bean, reset, null));
        assertEquals(7, bean.count);

        // non-null value on a method that does not take exactly one parameter: no conversion retry, the failure propagates
        assertThrows(IllegalArgumentException.class, () -> Beans.setPropValue(bean, reset, "x"));
        assertThrows(IllegalArgumentException.class, () -> Beans.setPropValue(bean, setBoth, "x"));
        assertNull(bean.name);
    }

    @Test
    public void testStream() {
        List<Map.Entry<String, Object>> entries = Beans.stream(simpleBean).toList();
        Map<String, Object> asMap = new HashMap<>();
        for (Map.Entry<String, Object> entry : entries) {
            asMap.put(entry.getKey(), entry.getValue());
        }
        assertEquals("John", asMap.get("name"));
        assertEquals(25, asMap.get("age"));
        assertEquals(true, asMap.get("active"));

        List<Map.Entry<String, Object>> stringProps = Beans.stream(simpleBean, (name, value) -> value instanceof String).toList();
        assertEquals(1, stringProps.size());
        assertEquals("name", stringProps.get(0).getKey());
        assertEquals("John", stringProps.get(0).getValue());

        List<Map.Entry<String, Object>> nameFilter = Beans.stream(simpleBean, (name, value) -> name.startsWith("a")).toList();
        assertTrue(nameFilter.stream().allMatch(e -> e.getKey().startsWith("a")));
        assertTrue(nameFilter.stream().anyMatch(e -> "age".equals(e.getKey())));
        assertTrue(nameFilter.stream().anyMatch(e -> "active".equals(e.getKey())));

        assertTrue(Beans.stream(simpleBean, (name, value) -> false).toList().isEmpty());
        assertThrows(IllegalArgumentException.class, () -> Beans.stream(null));
    }

    @Test
    public void testStream_SkipNullValues() {
        SimpleBean bean = new SimpleBean("Owen", 40);
        List<Map.Entry<String, Object>> nonNull = Beans.stream(bean, (name, val) -> val != null).toList();
        assertFalse(nonNull.stream().anyMatch(e -> "active".equals(e.getKey())));
        assertTrue(nonNull.stream().anyMatch(e -> "name".equals(e.getKey()) && "Owen".equals(e.getValue())));
    }

    @Test
    public void testThrowableAccessorRegistrationCannotRestoreProperties() throws Exception {
        assertThrowablePropertyModelExcluded(ExceptionWithProperties.class);
        Beans.registerPropertyAccessor("registeredCode", ExceptionWithProperties.class.getMethod("getCode"));
        Beans.registerPropertyAccessor("registeredCode", ExceptionWithProperties.class.getMethod("setCode", String.class));

        // Rebuilding an existing model and discovering a subclass after registration must both remain empty.
        assertThrowablePropertyModelExcluded(ExceptionWithProperties.class);
        assertThrowablePropertyModelExcluded(RegisteredException.class);
    }

    // Throwable subclasses stay outside the bean model, even with properties or bean annotations.
    @Test
    public void testThrowableClassesAreNotBeans() {
        for (final Class<?> type : List.of(Throwable.class, Error.class, Exception.class, RuntimeException.class, java.io.IOException.class,
                ExceptionWithProperties.class, EntityAnnotatedException.class, RecordAnnotatedException.class, ErrorWithPublicField.class)) {
            assertFalse(Beans.isBeanClass(type), type.getName());
            final String expected = "Not a bean class: " + type.getCanonicalName() + " - Throwable subclasses are never treated as beans";
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> Beans.getBeanInfo(type)).getMessage());
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> CommonUtil.checkBeanClass(type)).getMessage());
            assertEquals(expected, assertThrows(IllegalArgumentException.class, () -> Beans.getPropGetter(type, "code")).getMessage());
            assertThrowablePropertyModelExcluded(type);
        }
    }

    public static class OrdinaryComputedGetter {
        private int id;

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }

        public String getComputed() {
            return "derived";
        }
    }

    @com.landawn.abacus.annotation.Entity
    public static class EntityComputedGetter {
        public String getComputed() {
            return "derived";
        }
    }

    public record RecordComputedGetter(int id) {
        public String getComputed() {
            return "derived";
        }
    }

    public static class RandomizeEmailBean {
        private String email;

        public String getEmail() {
            return email;
        }

        public void setEmail(final String email) {
            this.email = email;
        }
    }

    public static class G020SetterShapes {
        private String name;
        private int count;

        public void setName(final String name) {
            this.name = name;
        }

        public void setCount(final int count) {
            this.count = count;
        }

        public void reset() {
            this.name = "reset";
        }

        public void setBoth(final String name, final int count) {
            this.name = name;
            this.count = count;
        }
    }

    public static class G020Node {
        private String name;
        private G020Node child;
        private G020Node sibling;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public G020Node getChild() {
            return child;
        }

        public void setChild(final G020Node child) {
            this.child = child;
        }

        public G020Node getSibling() {
            return sibling;
        }

        public void setSibling(final G020Node sibling) {
            this.sibling = sibling;
        }
    }

    public static class G020BugNumber extends Number {
        private static final long serialVersionUID = 1L;

        @Override
        public int intValue() {
            return 0;
        }

        @Override
        public long longValue() {
            return 0;
        }

        @Override
        public float floatValue() {
            return 0;
        }

        @Override
        public double doubleValue() {
            return 0;
        }
    }

    public static class G020BugUnsupportedTypes {
        private String name;
        private java.util.concurrent.atomic.LongAdder adder;
        private G020BugNumber bugNumber;
        private java.math.BigDecimal amount;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public java.util.concurrent.atomic.LongAdder getAdder() {
            return adder;
        }

        public void setAdder(final java.util.concurrent.atomic.LongAdder adder) {
            this.adder = adder;
        }

        public G020BugNumber getBugNumber() {
            return bugNumber;
        }

        public void setBugNumber(final G020BugNumber bugNumber) {
            this.bugNumber = bugNumber;
        }

        public java.math.BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(final java.math.BigDecimal amount) {
            this.amount = amount;
        }
    }

    public static class G120ByteSizedNumber extends Number {
        private static final long serialVersionUID = 1L;
        private final byte value;

        public G120ByteSizedNumber(final byte value) {
            this.value = value;
        }

        @Override
        public int intValue() {
            return value;
        }

        @Override
        public long longValue() {
            return value;
        }

        @Override
        public float floatValue() {
            return value;
        }

        @Override
        public double doubleValue() {
            return value;
        }
    }

    public static class G120NarrowNumbers {
        private MutableByte mutableByte;
        private MutableShort mutableShort;
        private G120ByteSizedNumber byteSized;
        private java.math.BigInteger bigInteger;

        public MutableByte getMutableByte() {
            return mutableByte;
        }

        public void setMutableByte(final MutableByte mutableByte) {
            this.mutableByte = mutableByte;
        }

        public MutableShort getMutableShort() {
            return mutableShort;
        }

        public void setMutableShort(final MutableShort mutableShort) {
            this.mutableShort = mutableShort;
        }

        public G120ByteSizedNumber getByteSized() {
            return byteSized;
        }

        public void setByteSized(final G120ByteSizedNumber byteSized) {
            this.byteSized = byteSized;
        }

        public java.math.BigInteger getBigInteger() {
            return bigInteger;
        }

        public void setBigInteger(final java.math.BigInteger bigInteger) {
            this.bigInteger = bigInteger;
        }
    }

    public enum G020MutableEnum {
        RED, GREEN;

        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }
    }

    public static class G020EnumHolder {
        private String name;
        private G020MutableEnum color;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public G020MutableEnum getColor() {
            return color;
        }

        public void setColor(final G020MutableEnum color) {
            this.color = color;
        }
    }

    public enum G116FieldEnum {
        ONE, TWO;

        public int counter;
    }

    public enum G116PlainEnum {
        ONE, TWO
    }

    public static class G116EnumHolder {
        private String name;
        private G116FieldEnum fieldEnum;
        private G116PlainEnum plainEnum;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public G116FieldEnum getFieldEnum() {
            return fieldEnum;
        }

        public void setFieldEnum(final G116FieldEnum fieldEnum) {
            this.fieldEnum = fieldEnum;
        }

        public G116PlainEnum getPlainEnum() {
            return plainEnum;
        }

        public void setPlainEnum(final G116PlainEnum plainEnum) {
            this.plainEnum = plainEnum;
        }
    }

    public static class ExceptionWithProperties extends RuntimeException {
        private static final long serialVersionUID = 1L;
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }
    }

    @com.landawn.abacus.annotation.Entity
    public static class EntityAnnotatedException extends ExceptionWithProperties {
        private static final long serialVersionUID = 1L;
    }

    @com.landawn.abacus.annotation.Record
    public static class RecordAnnotatedException extends ExceptionWithProperties {
        private static final long serialVersionUID = 1L;
    }

    public static class ErrorWithPublicField extends Error {
        private static final long serialVersionUID = 1L;
        public String code;
    }

    public static class RegisteredException extends ExceptionWithProperties {
        private static final long serialVersionUID = 1L;
    }

    private static void assertThrowablePropertyModelExcluded(final Class<?> type) {
        assertTrue(Beans.getPropNameList(type).isEmpty(), type.getName());
        assertTrue(Beans.getPropFields(type).isEmpty(), type.getName());
        assertTrue(Beans.getPropGetters(type).isEmpty(), type.getName());
        assertTrue(Beans.getPropSetters(type).isEmpty(), type.getName());

        // Exact names previously escaped the non-bean check while aliases failed.
        for (final String name : List.of("code", "CODE", "cause", "stackTrace", "registeredCode")) {
            assertThrows(IllegalArgumentException.class, () -> Beans.getPropField(type, name), type.getName() + ": " + name);
            assertThrows(IllegalArgumentException.class, () -> Beans.getPropGetter(type, name), type.getName() + ": " + name);
            assertThrows(IllegalArgumentException.class, () -> Beans.getPropSetter(type, name), type.getName() + ": " + name);
        }
    }

    public static class NoUsableConstructorBean {
        private int code;

        public NoUsableConstructorBean(final String ignored) {
        }

        public int getCode() {
            return code;
        }

        public void setCode(final int code) {
            this.code = code;
        }
    }

    public static class NestedCity {
        private String name;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static class NestedAddress {
        private NestedCity city;

        public NestedCity getCity() {
            return city;
        }

        public void setCity(final NestedCity city) {
            this.city = city;
        }
    }

    public static class AddressListHolder {
        private List<NestedAddress> addresses;

        public List<NestedAddress> getAddresses() {
            return addresses;
        }

        public void setAddresses(final List<NestedAddress> addresses) {
            this.addresses = addresses;
        }
    }

    public record NestedAddressRecord(String name, NestedAddress address) {
    }

    // A concrete bean type with a getter/setter pair but no accessible no-arg constructor, builder or all-arguments constructor.
    public static class G020UnbuildableMoney {
        private long cents;

        public G020UnbuildableMoney(final long cents, final String currency) {
            this.cents = cents;
        }

        public long getCents() {
            return cents;
        }

        public void setCents(final long cents) {
            this.cents = cents;
        }
    }

    public static class G020MoneyHolder {
        private String name;
        private G020UnbuildableMoney money;
        private String tail;

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public G020UnbuildableMoney getMoney() {
            return money;
        }

        public void setMoney(final G020UnbuildableMoney money) {
            this.money = money;
        }

        public String getTail() {
            return tail;
        }

        public void setTail(final String tail) {
            this.tail = tail;
        }
    }

    public static final class G120VBuiltCity {
        private final String name;

        private G120VBuiltCity(final String name) {
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

            public G120VBuiltCity build() {
                return new G120VBuiltCity(name);
            }
        }
    }

    public static class G120VMixedHolder {
        private G020UnbuildableMoney money;
        private NestedAddressRecord addressRecord;
        private NestedAddress address;
        private G120VBuiltCity builtCity;

        public G020UnbuildableMoney getMoney() {
            return money;
        }

        public void setMoney(final G020UnbuildableMoney money) {
            this.money = money;
        }

        public NestedAddressRecord getAddressRecord() {
            return addressRecord;
        }

        public void setAddressRecord(final NestedAddressRecord addressRecord) {
            this.addressRecord = addressRecord;
        }

        public NestedAddress getAddress() {
            return address;
        }

        public void setAddress(final NestedAddress address) {
            this.address = address;
        }

        public G120VBuiltCity getBuiltCity() {
            return builtCity;
        }

        public void setBuiltCity(final G120VBuiltCity builtCity) {
            this.builtCity = builtCity;
        }
    }

    // ---- bug review 2026-10-02 verify G120 end ----

    // ---- deep review 2026-10-16 G023 begin ----

    /** A static String constant named like a property whose getter/setter work on a differently named field. */
    public static class G023StaticNamedBean {
        public static final String NAME = "NAME";
        private String fullName;

        public String getName() {
            return fullName;
        }

        public void setName(final String name) {
            this.fullName = name;
        }
    }

    // G023-01: a static field must never become a property's backing field
    @Test
    public void testGetPropNameList_staticFieldDoesNotBackProperty() {
        assertEquals(List.of("name"), Beans.getPropNameList(G023StaticNamedBean.class));
        assertTrue(Beans.getPropFields(G023StaticNamedBean.class).isEmpty());

        final G023StaticNamedBean bean = new G023StaticNamedBean();
        bean.setName("John");

        assertEquals(Map.of("name", "John"), Beans.beanToMap(bean));
        assertEquals("John", Beans.getPropValue(bean, "name"));

        final G023StaticNamedBean copy = Beans.mapToBean(Map.of("name", "Jane"), G023StaticNamedBean.class);
        assertEquals("Jane", copy.getName());
        assertEquals("NAME", G023StaticNamedBean.NAME);
    }

    /** An XML-binding bean whose no-arg constructor is not accessible to Beans (private, no nestmate access). */
    public static class G023PrivateCtorXmlBean {
        private final java.util.List<String> items = new java.util.ArrayList<>();
        private String name;

        private G023PrivateCtorXmlBean() {
        }

        public java.util.List<String> getItems() {
            return items;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    // G023-02: the JAXB getter probe must construct the bean the way BeanInfo does (accessible constructor),
    // not demote the registration and drop the getter-only collection
    @Test
    public void testRegisterXmlBindingClass_inaccessibleConstructorKeepsGetterOnlyCollection() {
        Beans.registerXmlBindingClass(G023PrivateCtorXmlBean.class);

        assertEquals(List.of("items", "name"), Beans.getPropNameList(G023PrivateCtorXmlBean.class));
        assertTrue(Beans.isRegisteredXmlBindingClass(G023PrivateCtorXmlBean.class));

        final Map<String, Object> map = new HashMap<>();
        map.put("items", List.of("a", "b"));
        map.put("name", "n");

        final G023PrivateCtorXmlBean bean = Beans.mapToBean(map, false, G023PrivateCtorXmlBean.class);
        assertEquals(List.of("a", "b"), bean.getItems());
        assertEquals("n", bean.getName());
    }

    // ---- deep review 2026-10-16 G023 end ----

    // ---- deep review 2026-10-16 verify G119 begin ----

    /** The static field is the only field matching the accessors: the getter and setter work on it. */
    public static class G119StaticOnlyCandidateBean {
        private static String region = "init";
        private int id;

        public String getRegion() {
            return region;
        }

        public void setRegion(final String region) {
            G119StaticOnlyCandidateBean.region = region;
        }

        public int getId() {
            return id;
        }

        public void setId(final int id) {
            this.id = id;
        }
    }

    // G023-01: a static field that is the only candidate for a getter/setter pair does not back the property;
    // the property is read and written through its accessors (it used to crash every read)
    @Test
    public void testGetPropNameList_staticFieldOnlyCandidateIsAccessorProperty() {
        final String saved = G119StaticOnlyCandidateBean.region;

        try {
            assertEquals(List.of("id", "region"), Beans.getPropNameList(G119StaticOnlyCandidateBean.class));
            assertEquals(List.of("id"), List.copyOf(Beans.getPropFields(G119StaticOnlyCandidateBean.class).keySet()));
            assertNull(Beans.getPropField(G119StaticOnlyCandidateBean.class, "region"));

            final G119StaticOnlyCandidateBean bean = new G119StaticOnlyCandidateBean();
            bean.setId(3);
            G119StaticOnlyCandidateBean.region = "eu";

            assertEquals("eu", Beans.getPropValue(bean, "region"));
            assertEquals(Map.of("id", 3, "region", "eu"), Beans.beanToMap(bean));

            Beans.setPropValue(bean, "region", "us");
            assertEquals("us", G119StaticOnlyCandidateBean.region);
        } finally {
            G119StaticOnlyCandidateBean.region = saved;
        }
    }

    /** A static constant collides by name with a getter of an @AccessFieldByMethod bean. */
    @com.landawn.abacus.annotation.AccessFieldByMethod
    public static class G119AccessorModeStaticCollisionBean {
        public static final String CODE = "c";
        private String internalCode;
        private String name;

        public String getCode() {
            return internalCode;
        }

        public void setCode(final String code) {
            this.internalCode = code;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    // G023-01: in accessor mode too, the property is named after its getter, not after the colliding static constant
    @Test
    public void testGetPropNameList_staticConstantCollidingWithAccessorModeGetter() {
        assertEquals(List.of("name", "code"), Beans.getPropNameList(G119AccessorModeStaticCollisionBean.class));
        assertEquals(List.of("name"), List.copyOf(Beans.getPropFields(G119AccessorModeStaticCollisionBean.class).keySet()));

        final G119AccessorModeStaticCollisionBean bean = new G119AccessorModeStaticCollisionBean();
        bean.setCode("x");
        bean.setName("n");

        assertEquals(Map.of("name", "n", "code", "x"), Beans.beanToMap(bean));
        assertEquals("c", G119AccessorModeStaticCollisionBean.CODE);
    }

    /** Immutable bean (no no-arg constructor) with a static constant matching a getter by name. */
    public static class G119ImmutableStaticCollisionBean {
        public static final String TYPE = "T";
        private final int amount;

        public G119ImmutableStaticCollisionBean(final int amount) {
            this.amount = amount;
        }

        public int getAmount() {
            return amount;
        }

        public String getType() {
            return TYPE;
        }
    }

    // G023-01: the static field no longer enters the immutable-constructor signature, so the class is an immutable bean
    @Test
    public void testIsBeanClass_immutableBeanWithStaticConstantCollidingWithGetter() {
        assertTrue(Beans.isBeanClass(G119ImmutableStaticCollisionBean.class));
        assertEquals(List.of("amount"), Beans.getPropNameList(G119ImmutableStaticCollisionBean.class));
        assertEquals(Map.of("amount", 4), Beans.beanToMap(new G119ImmutableStaticCollisionBean(4)));
    }

    /** A static field names the getter exactly; the instance field matches it only case-insensitively. */
    public static class G119StaticExactOwnerBean {
        private static String userName = "STATIC";
        private String username;

        public String getUserName() {
            return username;
        }

        public void setUserName(final String userName) {
            this.username = userName;
        }
    }

    // G023-01: a static field does not claim a getter in the exact-owner pass, so the instance field keeps it
    @Test
    public void testGetPropNameList_staticFieldDoesNotOwnGetterExactly() {
        assertEquals(List.of("username"), Beans.getPropNameList(G119StaticExactOwnerBean.class));
        assertEquals(List.of("username"), List.copyOf(Beans.getPropFields(G119StaticExactOwnerBean.class).keySet()));

        final G119StaticExactOwnerBean bean = new G119StaticExactOwnerBean();
        bean.setUserName("bob");
        assertEquals(Map.of("username", "bob"), Beans.beanToMap(bean));

        final G119StaticExactOwnerBean copy = Beans.mapToBean(Map.of("username", "al"), G119StaticExactOwnerBean.class);
        assertEquals("al", copy.getUserName());
        assertEquals("STATIC", G119StaticExactOwnerBean.userName);
    }

    /** Static members that never collided with an accessor: logger-style constants, a same-name-ignoring-case static. */
    public static class G119UnrelatedStaticsBean implements java.io.Serializable {
        private static final long serialVersionUID = 1L;
        private static final Object LOG = new Object();
        public static final String FIRST_NAME = "firstName";
        public static String shared = "s";
        private static String URL = "u";
        private String firstName;
        private String url;
        public String plain;

        public String getFirstName() {
            return firstName;
        }

        public void setFirstName(final String firstName) {
            this.firstName = firstName;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(final String url) {
            this.url = url;
        }
    }

    // G023-01 neighbour: beans whose static fields never matched a getter keep the same field-backed properties
    @Test
    public void testGetPropNameList_unrelatedStaticFieldsUnchanged() {
        assertEquals(List.of("firstName", "url", "plain"), Beans.getPropNameList(G119UnrelatedStaticsBean.class));
        assertEquals(List.of("firstName", "url", "plain"), List.copyOf(Beans.getPropFields(G119UnrelatedStaticsBean.class).keySet()));
        assertNull(Beans.getPropField(G119UnrelatedStaticsBean.class, "shared"));
        assertNotNull(G119UnrelatedStaticsBean.LOG);

        final G119UnrelatedStaticsBean bean = Beans.mapToBean(Map.of("firstName", "f", "url", "h", "plain", "p", "shared", "z"),
                G119UnrelatedStaticsBean.class);
        assertEquals("f", bean.getFirstName());
        assertEquals("h", bean.getUrl());
        assertEquals("p", bean.plain);
        assertEquals("s", G119UnrelatedStaticsBean.shared);
        assertEquals("u", G119UnrelatedStaticsBean.URL);
    }

    // G023-02: a registered XML-binding class that is not public, in another package (public no-arg constructor,
    // public getter-only collection): both the construction and the getter probe need setAccessible
    @Test
    public void testRegisterXmlBindingClass_nonPublicClassInOtherPackageKeepsGetterOnlyCollection(
            @org.junit.jupiter.api.io.TempDir final java.nio.file.Path tempDir) throws Exception {
        final java.nio.file.Path source = tempDir.resolve("HiddenXmlBean.java");
        java.nio.file.Files.writeString(source, String.join("\n", //
                "package g119hidden;", //
                "class HiddenXmlBean {", //
                "    private final java.util.List<String> items = new java.util.ArrayList<>();", //
                "    private String name;", //
                "    public HiddenXmlBean() {}", //
                "    public java.util.List<String> getItems() { return items; }", //
                "    public String getName() { return name; }", //
                "    public void setName(String name) { this.name = name; }", //
                "}", ""), java.nio.charset.StandardCharsets.UTF_8);
        assertEquals(0, javax.tools.ToolProvider.getSystemJavaCompiler().run(null, null, null, "-d", tempDir.toString(), source.toString()));

        try (java.net.URLClassLoader loader = new java.net.URLClassLoader(new java.net.URL[] { tempDir.toUri().toURL() }, BeansTest.class.getClassLoader())) {
            final Class<?> cls = Class.forName("g119hidden.HiddenXmlBean", false, loader);
            Beans.registerXmlBindingClass(cls);

            assertEquals(List.of("items", "name"), Beans.getPropNameList(cls));
            assertTrue(Beans.isRegisteredXmlBindingClass(cls));

            final Map<String, Object> map = new HashMap<>();
            map.put("items", List.of("a"));
            map.put("name", "n");

            final Object bean = Beans.mapToBean(map, false, cls);
            assertEquals(List.of("a"), Beans.getPropValue(bean, "items"));
            assertEquals("n", Beans.getPropValue(bean, "name"));
        }
    }

    /** Not registered, no XML annotations: its getter-only list must not trigger the construction probe. */
    public static class G119PlainPrivateCtorBean {
        static int constructed = 0;
        private final java.util.List<String> items = new java.util.ArrayList<>();
        private String name;

        private G119PlainPrivateCtorBean() {
            constructed++;
        }

        public java.util.List<String> getItems() {
            return items;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    // G023-02 neighbour: the now-accessible probe constructor runs only for XML-binding candidates
    @Test
    public void testGetPropNameList_privateCtorNonXmlBeanIsNotConstructed() {
        assertEquals(List.of("name"), Beans.getPropNameList(G119PlainPrivateCtorBean.class));
        assertEquals(0, G119PlainPrivateCtorBean.constructed);
        assertFalse(Beans.isRegisteredXmlBindingClass(G119PlainPrivateCtorBean.class));
    }

    // ---- deep review 2026-10-16 verify G119 end ----

    // ---- deep review 2026-10-16 G154 begin ----

    /** Superclass fixture: an ordinary field-backed property. */
    public static class G154CodeBase {
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(final String code) {
            this.code = code;
        }
    }

    /** Subclass fixture: its own static constant matches the INHERITED getCode() case-insensitively. */
    public static class G154StaticOverInheritedBean extends G154CodeBase {
        public static final String CODE = "STATIC";
        private int size;

        public int getSize() {
            return size;
        }

        public void setSize(final int size) {
            this.size = size;
        }
    }

    /** Record fixture with a static constant named like its component. */
    public record G154StaticRecord(String name, int size) {
        public static final String NAME = "STATIC";
    }

    // G154 (verifies G023-01): a subclass's static constant no longer pairs with an inherited getter - on r89 it became a
    // second, static-field-backed property "CODE" beside the inherited "code" (beanToMap then failed reading it).
    @Test
    public void testGetPropNameList_subclassStaticDoesNotDuplicateInheritedProperty() {
        assertEquals(List.of("code", "size"), Beans.getPropNameList(G154StaticOverInheritedBean.class));
        assertEquals(List.of("code", "size"), List.copyOf(Beans.getPropFields(G154StaticOverInheritedBean.class).keySet()));

        final G154StaticOverInheritedBean bean = new G154StaticOverInheritedBean();
        bean.setCode("c");
        bean.setSize(2);
        assertEquals(Map.of("code", "c", "size", 2), Beans.beanToMap(bean));

        final G154StaticOverInheritedBean copy = Beans.mapToBean(Map.of("code", "x", "size", 3), G154StaticOverInheritedBean.class);
        assertEquals("x", copy.getCode());
        assertEquals(3, copy.getSize());
        assertEquals("STATIC", G154StaticOverInheritedBean.CODE);
    }

    // G154 neighbour (unchanged): a record's static constant never competes with its components.
    @Test
    public void testGetPropNameList_recordStaticConstantUnchanged() {
        assertEquals(List.of("name", "size"), Beans.getPropNameList(G154StaticRecord.class));
        assertEquals(Map.of("name", "n", "size", 1), Beans.beanToMap(new G154StaticRecord("n", 1)));
        assertEquals(new G154StaticRecord("m", 2), Beans.mapToBean(Map.of("name", "m", "size", 2), G154StaticRecord.class));
        assertEquals("STATIC", G154StaticRecord.NAME);
    }

    // ---- deep review 2026-10-16 G154 end ----

    // ---- deep review 2026-10-26 G023 begin ----

    /** Builder-based bean for G023-01 (a registration against the builder class itself). */
    public static class G023RegBuilderBean {
        private final String name;
        private final int age;

        private G023RegBuilderBean(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String name;
            private int age;

            public Builder name(final String name) {
                this.name = name;
                return this;
            }

            public Builder age(final int age) {
                this.age = age;
                return this;
            }

            public G023RegBuilderBean build() {
                return new G023RegBuilderBean(name, age);
            }
        }
    }

    /** Builder-based bean for G023-01 (a registration against the bean, then the builder scanned first). */
    public static class G023ScanBuilderBean {
        private final String name;
        private final int age;

        private G023ScanBuilderBean(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String name;
            private int age;

            public Builder name(final String name) {
                this.name = name;
                return this;
            }

            public Builder age(final int age) {
                this.age = age;
                return this;
            }

            public G023ScanBuilderBean build() {
                return new G023ScanBuilderBean(name, age);
            }
        }
    }

    // G023-01: a registration against a builder class dropped the setter model its bean had published, and the bean
    // (still introspected) never republished it - getPropSetters answered {} and mapToBean silently wrote nothing.
    @Test
    public void testGetPropSetters_registrationOnBuilderClassKeepsItsSetters() {
        assertEquals(List.of("name", "age"), Beans.getPropNameList(G023RegBuilderBean.class));
        assertEquals(java.util.Set.of("name", "age"), Beans.getPropSetters(G023RegBuilderBean.Builder.class).keySet());

        Beans.registerNonPropertyAccessor(G023RegBuilderBean.Builder.class, "zzzNotAProperty");

        assertEquals(java.util.Set.of("name", "age"), Beans.getPropSetters(G023RegBuilderBean.Builder.class).keySet());
        assertEquals("name", Beans.getPropSetter(G023RegBuilderBean.Builder.class, "name").getName());

        // The bean's BeanInfo is first built here, after the registration, and drives the builder by these setters.
        final G023RegBuilderBean bean = Beans.mapToBean(Map.of("name", "x", "age", 3), false, G023RegBuilderBean.class);
        assertEquals("x", bean.getName());
        assertEquals(3, bean.getAge());
    }

    // G023-01: after a registration against the bean, a lookup that scanned the builder class before the bean was
    // re-introspected (isBeanClass, getPropNameList, getPropGetters ...) published the builder's empty standalone model.
    @Test
    public void testGetPropSetters_builderScannedBeforeItsBeanAfterRegistrationKeepsItsSetters() {
        assertEquals(List.of("name", "age"), Beans.getPropNameList(G023ScanBuilderBean.class));
        assertEquals(java.util.Set.of("name", "age"), Beans.getPropSetters(G023ScanBuilderBean.Builder.class).keySet());

        Beans.registerNonPropertyAccessor(G023ScanBuilderBean.class, "zzzNotAProperty");
        assertFalse(Beans.isBeanClass(G023ScanBuilderBean.Builder.class));

        assertEquals(java.util.Set.of("name", "age"), Beans.getPropSetters(G023ScanBuilderBean.Builder.class).keySet());
        assertEquals("age", Beans.getPropSetter(G023ScanBuilderBean.Builder.class, "age").getName());

        final G023ScanBuilderBean bean = Beans.mapToBean(Map.of("name", "y", "age", 4), G023ScanBuilderBean.class);
        assertEquals("y", bean.getName());
        assertEquals(4, bean.getAge());
    }

    // ---- deep review 2026-10-26 G023 end ----

    // ---- deep review 2026-10-26 verify G122 begin ----

    /** Record with a builder, for the G023-01 record path (a registration against the builder class). */
    public record G122BuilderRecord(String name, int age) {
        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String name;
            private int age;

            public Builder name(final String name) {
                this.name = name;
                return this;
            }

            public Builder age(final int age) {
                this.age = age;
                return this;
            }

            public G122BuilderRecord build() {
                return new G122BuilderRecord(name, age);
            }
        }
    }

    /** Bean whose builder is a bean itself for "name" (getName/setName) and fluent for "age", for G023-01. */
    public static class G122BeanLikeBuilderBean {
        private final String name;
        private final int age;

        private G122BeanLikeBuilderBean(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String name;
            private int age;

            public String getName() {
                return name;
            }

            public void setName(final String name) {
                this.name = name;
            }

            public Builder age(final int age) {
                this.age = age;
                return this;
            }

            public G122BeanLikeBuilderBean build() {
                return new G122BeanLikeBuilderBean(name, age);
            }
        }
    }

    // G023-01 (record): a registration against a record's builder class must keep the builder setters, so mapToBean and
    // N.fromJson still populate the record (they produced G122BuilderRecord[name=null, age=0]).
    @Test
    public void testGetPropSetters_registrationOnRecordBuilderClassKeepsItsSetters() {
        assertEquals(List.of("name", "age"), Beans.getPropNameList(G122BuilderRecord.class));
        assertEquals(java.util.Set.of("name", "age"), Beans.getPropSetters(G122BuilderRecord.Builder.class).keySet());

        Beans.registerNonPropertyAccessor(G122BuilderRecord.Builder.class, "zzzNotAProperty");

        assertEquals(java.util.Set.of("name", "age"), Beans.getPropSetters(G122BuilderRecord.Builder.class).keySet());
        assertEquals(new G122BuilderRecord("r", 2), Beans.mapToBean(Map.of("name", "r", "age", 2), false, G122BuilderRecord.class));
        assertEquals(new G122BuilderRecord("j", 6), N.fromJson("{\"name\":\"j\",\"age\":6}", G122BuilderRecord.class));
    }

    // G023-01 (builder that is a bean itself): after a registration against the builder class its republished model kept
    // only its own setter (name -> setName) and lost the fluent age(int); the scan's own entry still wins a shared name.
    @Test
    public void testGetPropSetters_registrationOnBeanLikeBuilderClassKeepsFluentSetters() {
        assertEquals(List.of("name", "age"), Beans.getPropNameList(G122BeanLikeBuilderBean.class));

        Beans.registerNonPropertyAccessor(G122BeanLikeBuilderBean.Builder.class, "zzzNotAProperty");

        final Map<String, java.lang.reflect.Method> setters = Beans.getPropSetters(G122BeanLikeBuilderBean.Builder.class);
        assertEquals(java.util.Set.of("name", "age"), setters.keySet());
        assertEquals("setName", setters.get("name").getName());
        assertEquals("age", setters.get("age").getName());

        final G122BeanLikeBuilderBean bean = Beans.mapToBean(Map.of("name", "e", "age", 1), G122BeanLikeBuilderBean.class);
        assertEquals("e", bean.getName());
        assertEquals(1, bean.getAge());
    }

    // ---- deep review 2026-10-26 verify G122 end ----

    // ---- deep review 2026-10-26 G127 begin ----

    /** Builder-based bean for G127-01 (exclusion registered against the builder after the bean was introspected). */
    public static class G127ExclAfterBean {
        private final String name;
        private final int age;

        private G127ExclAfterBean(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String name;
            private int age;

            public Builder name(final String name) {
                this.name = name;
                return this;
            }

            public Builder age(final int age) {
                this.age = age;
                return this;
            }

            public G127ExclAfterBean build() {
                return new G127ExclAfterBean(name, age);
            }
        }
    }

    /** Builder-based bean for G127-01 (exclusion registered against the builder before the bean is introspected). */
    public static class G127ExclBeforeBean {
        private final String name;
        private final int age;

        private G127ExclBeforeBean(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String name;
            private int age;

            public Builder name(final String name) {
                this.name = name;
                return this;
            }

            public Builder age(final int age) {
                this.age = age;
                return this;
            }

            public G127ExclBeforeBean build() {
                return new G127ExclBeforeBean(name, age);
            }
        }
    }

    // G127-01: a real property excluded with registerNonPropertyAccessor against the BUILDER class after the bean was
    // introspected. r107 lost every builder setter (age too; mapToBean built an empty bean); the first G023-01 fix re-derived
    // all of them, the excluded one included. Only the excluded setter may be gone.
    @Test
    public void testGetPropSetters_builderExclusionRegisteredAfterBeanIntrospection() {
        assertEquals(List.of("name", "age"), Beans.getPropNameList(G127ExclAfterBean.class));
        assertEquals(java.util.Set.of("name", "age"), Beans.getPropSetters(G127ExclAfterBean.Builder.class).keySet());

        Beans.registerNonPropertyAccessor(G127ExclAfterBean.Builder.class, "name");

        assertNull(Beans.getPropSetter(G127ExclAfterBean.Builder.class, "name"));
        assertEquals("age", Beans.getPropSetter(G127ExclAfterBean.Builder.class, "age").getName());
        assertEquals(java.util.Set.of("age"), Beans.getPropSetters(G127ExclAfterBean.Builder.class).keySet());

        final G127ExclAfterBean bean = Beans.mapToBean(Map.of("name", "x", "age", 3), true, G127ExclAfterBean.class);
        assertNull(bean.getName());
        assertEquals(3, bean.getAge());
    }

    // G127-01: the same registration made BEFORE the bean is introspected. The builder setters published by the bean's
    // introspection ignored it (r107 too), so the two orders gave different models; both now exclude the name.
    @Test
    public void testGetPropSetters_builderExclusionRegisteredBeforeBeanIntrospection() {
        Beans.registerNonPropertyAccessor(G127ExclBeforeBean.Builder.class, "NAME");

        assertEquals(List.of("name", "age"), Beans.getPropNameList(G127ExclBeforeBean.class));
        assertNull(Beans.getPropSetter(G127ExclBeforeBean.Builder.class, "name"));
        assertEquals("age", Beans.getPropSetter(G127ExclBeforeBean.Builder.class, "age").getName());
        assertEquals(java.util.Set.of("age"), Beans.getPropSetters(G127ExclBeforeBean.Builder.class).keySet());

        final G127ExclBeforeBean bean = Beans.mapToBean(Map.of("name", "y", "age", 4), true, G127ExclBeforeBean.class);
        assertNull(bean.getName());
        assertEquals(4, bean.getAge());
    }

    /** Builder-based bean for G127-03 (a conversion runs before the builder exclusion is registered). */
    public static class G127ConvertFirstBean {
        private final String name;
        private final int age;

        private G127ConvertFirstBean(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String name;
            private int age;

            public Builder name(final String name) {
                this.name = name;
                return this;
            }

            public Builder age(final int age) {
                this.age = age;
                return this;
            }

            public G127ConvertFirstBean build() {
                return new G127ConvertFirstBean(name, age);
            }
        }
    }

    /** Supertype of the builder of {@link G127SuperBuilderBean}; G127-03 registers the exclusion against it. */
    public interface G127BuilderBase {
    }

    /** Builder-based bean for G127-03 whose builder implements {@link G127BuilderBase}. */
    public static class G127SuperBuilderBean {
        private final String name;
        private final int age;

        private G127SuperBuilderBean(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder implements G127BuilderBase {
            private String name;
            private int age;

            public Builder name(final String name) {
                this.name = name;
                return this;
            }

            public Builder age(final int age) {
                this.age = age;
                return this;
            }

            public G127SuperBuilderBean build() {
                return new G127SuperBuilderBean(name, age);
            }
        }
    }

    // G127-03: a conversion before the registration cached the bean's parser model (BeanInfo), which captured the builder's
    // setters; a registration against the builder class refreshed only the builder, so mapToBean and N.fromJson kept
    // populating the excluded property (r107 too) while getPropSetter already answered null.
    @Test
    public void testMapToBeanAndFromJson_builderExclusionRegisteredAfterAConversion() {
        final G127ConvertFirstBean before = Beans.mapToBean(Map.of("name", "a", "age", 1), true, G127ConvertFirstBean.class);
        assertEquals("a", before.getName());
        assertEquals("b", N.fromJson("{\"name\":\"b\",\"age\":2}", G127ConvertFirstBean.class).getName());

        Beans.registerNonPropertyAccessor(G127ConvertFirstBean.Builder.class, "name");

        assertNull(Beans.getPropSetter(G127ConvertFirstBean.Builder.class, "name"));

        final G127ConvertFirstBean viaMap = Beans.mapToBean(Map.of("name", "c", "age", 3), true, G127ConvertFirstBean.class);
        assertNull(viaMap.getName());
        assertEquals(3, viaMap.getAge());

        final G127ConvertFirstBean viaJson = N.fromJson("{\"name\":\"d\",\"age\":4}", G127ConvertFirstBean.class);
        assertNull(viaJson.getName());
        assertEquals(4, viaJson.getAge());
    }

    // G127-03: the same through a registration against a supertype of the builder class. A builder published only through
    // its bean is not in the introspected-class index, so neither its setter pools nor the bean's parser model were dropped.
    @Test
    public void testMapToBeanAndFromJson_exclusionRegisteredAgainstBuilderSupertypeAfterAConversion() {
        assertEquals("a", Beans.mapToBean(Map.of("name", "a", "age", 1), true, G127SuperBuilderBean.class).getName());
        assertEquals("b", N.fromJson("{\"name\":\"b\",\"age\":2}", G127SuperBuilderBean.class).getName());
        assertEquals(java.util.Set.of("name", "age"), Beans.getPropSetters(G127SuperBuilderBean.Builder.class).keySet());

        Beans.registerNonPropertyAccessor(G127BuilderBase.class, "name");

        assertNull(Beans.getPropSetter(G127SuperBuilderBean.Builder.class, "name"));
        assertEquals(java.util.Set.of("age"), Beans.getPropSetters(G127SuperBuilderBean.Builder.class).keySet());

        final G127SuperBuilderBean viaMap = Beans.mapToBean(Map.of("name", "c", "age", 3), true, G127SuperBuilderBean.class);
        assertNull(viaMap.getName());
        assertEquals(3, viaMap.getAge());

        final G127SuperBuilderBean viaJson = N.fromJson("{\"name\":\"d\",\"age\":4}", G127SuperBuilderBean.class);
        assertNull(viaJson.getName());
        assertEquals(4, viaJson.getAge());
    }

    // ---- deep review 2026-10-26 G127 end ----

    // ---- deep review 2026-10-27 G133 begin ----

    // G023-03: an abstract class (or interface) registered with registerXmlBindingClass can never be constructed, so the
    // JAXB getter probe used to demote its registration the first time it was introspected. Subclasses introspected
    // afterwards then lost the inherited getter-only collection and mapToBean / fromJson / fromXml dropped its data,
    // while subclasses introspected before kept it. Every test uses its own fixtures: the registration is static state.

    // xjc shape: protected list, lazily created by the getter, no setter
    public abstract static class G133BaseFirstBase {
        protected List<String> items;
        private String name;

        public List<String> getItems() {
            if (items == null) {
                items = new java.util.ArrayList<>();
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

    public static class G133BaseFirstSub extends G133BaseFirstBase {
        private int size;

        public int getSize() {
            return size;
        }

        public void setSize(final int size) {
            this.size = size;
        }
    }

    // final list: a write has to go through the getter, which consults the registration of the declaring (abstract) class
    public abstract static class G133SubFirstBase {
        private final List<String> items = new java.util.ArrayList<>();
        private String name;

        public List<String> getItems() {
            return items;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }
    }

    public static class G133SubFirstSub extends G133SubFirstBase {
        private int size;

        public int getSize() {
            return size;
        }

        public void setSize(final int size) {
            this.size = size;
        }
    }

    public static class G133SubFirstSibling extends G133SubFirstBase {
        private String tag;

        public String getTag() {
            return tag;
        }

        public void setTag(final String tag) {
            this.tag = tag;
        }
    }

    // a getter-only map without a backing field of the same name (found by the method scan, not the field scan)
    public abstract static class G133FieldlessBase {
        private final Map<String, String> store = new java.util.LinkedHashMap<>();
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

    public static class G133FieldlessSub extends G133FieldlessBase {
    }

    public interface G133XmlIface {
        List<String> getNotes();
    }

    // concrete and explicitly registered, but cannot be constructed: still demoted
    public static class G133ConcreteNoCtorBean {
        private final List<String> items = new java.util.ArrayList<>();

        public G133ConcreteNoCtorBean() {
            throw new IllegalStateException("not constructible");
        }

        public List<String> getItems() {
            return items;
        }
    }

    private static Map<String, Object> g133Map(final Object... keyValues) {
        final Map<String, Object> map = new HashMap<>();

        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }

        return map;
    }

    @Test
    public void testRegisterXmlBindingClass_abstractBaseIntrospectedBeforeSubclassIsNotDemoted() {
        Beans.registerXmlBindingClass(G133BaseFirstBase.class);

        // the abstract base itself cannot be probed, so its own model has no getter-only collection ...
        assertEquals(List.of("name"), Beans.getPropNameList(G133BaseFirstBase.class));

        // ... but a subclass introspected afterwards still exposes the inherited one, and mapToBean fills it
        final G133BaseFirstSub bean = Beans.mapToBean(g133Map("items", List.of("a", "b"), "name", "n", "size", 3), G133BaseFirstSub.class);
        assertEquals(List.of("a", "b"), bean.getItems());
        assertEquals("n", bean.getName());
        assertEquals(3, bean.getSize());
        assertEquals(List.of("items", "name", "size"), Beans.getPropNameList(G133BaseFirstSub.class));
        assertEquals(List.of("a", "b"), Beans.beanToMap(bean).get("items"));

        // because the base's registration survives the failed construction and governs the subclass
        assertTrue(Beans.isRegisteredXmlBindingClass(G133BaseFirstBase.class));
        assertTrue(Beans.isRegisteredXmlBindingClass(G133BaseFirstSub.class));

        assertEquals(List.of("x"), N.fromJson("{\"items\":[\"x\"],\"name\":\"j\"}", G133BaseFirstSub.class).getItems());

        final String xml = N.toXml(bean);
        assertTrue(xml.contains("<items>"), xml);
        final G133BaseFirstSub fromXml = N.fromXml(xml, G133BaseFirstSub.class);
        assertEquals(List.of("a", "b"), fromXml.getItems());
        assertEquals("n", fromXml.getName());
    }

    @Test
    public void testRegisterXmlBindingClass_abstractBaseIntrospectedAfterSubclassIsNotDemoted() {
        Beans.registerXmlBindingClass(G133SubFirstBase.class);

        assertEquals(List.of("items", "name", "size"), Beans.getPropNameList(G133SubFirstSub.class));

        // the abstract base is introspected afterwards ...
        assertEquals(List.of("name"), Beans.getPropNameList(G133SubFirstBase.class));

        // ... and a sibling introspected only now sees the same inherited collection as the earlier subclass
        final G133SubFirstSibling sibling = Beans.mapToBean(g133Map("items", List.of("c"), "name", "m", "tag", "t"), G133SubFirstSibling.class);
        assertEquals(List.of("c"), sibling.getItems());
        assertEquals("t", sibling.getTag());
        assertEquals(List.of("items", "name", "tag"), Beans.getPropNameList(G133SubFirstSibling.class));

        final G133SubFirstSibling fromXml = N.fromXml(N.toXml(sibling), G133SubFirstSibling.class);
        assertEquals(List.of("c"), fromXml.getItems());
        assertEquals("t", fromXml.getTag());

        // the earlier subclass keeps it as well
        assertEquals(List.of("items", "name", "size"), Beans.getPropNameList(G133SubFirstSub.class));

        final G133SubFirstSub sub = Beans.mapToBean(g133Map("items", List.of("a", "b"), "name", "n", "size", 3), G133SubFirstSub.class);
        assertEquals(List.of("a", "b"), sub.getItems());
        assertEquals(3, sub.getSize());
        assertEquals(List.of("x"), N.fromJson("{\"items\":[\"x\"],\"size\":4}", G133SubFirstSub.class).getItems());

        // introspecting the base did not withdraw the registration that governs both subclasses
        assertTrue(Beans.isRegisteredXmlBindingClass(G133SubFirstBase.class));
        assertTrue(Beans.isRegisteredXmlBindingClass(G133SubFirstSub.class));
        assertTrue(Beans.isRegisteredXmlBindingClass(G133SubFirstSibling.class));
    }

    @Test
    public void testRegisterXmlBindingClass_abstractBaseWithFieldlessGetterOnlyMapIsNotDemoted() {
        Beans.registerXmlBindingClass(G133FieldlessBase.class);

        assertEquals(List.of("name"), Beans.getPropNameList(G133FieldlessBase.class));

        final G133FieldlessSub bean = Beans.mapToBean(g133Map("attrs", Map.of("k", "v"), "name", "n"), G133FieldlessSub.class);
        assertEquals(Map.of("k", "v"), bean.getAttrs());
        assertEquals("n", bean.getName());
        assertEquals(List.of("name", "attrs"), Beans.getPropNameList(G133FieldlessSub.class));

        assertTrue(Beans.isRegisteredXmlBindingClass(G133FieldlessBase.class));
    }

    @Test
    public void testRegisterXmlBindingClass_interfaceIsNotDemotedByIntrospection() {
        Beans.registerXmlBindingClass(G133XmlIface.class);

        assertEquals(List.of(), Beans.getPropNameList(G133XmlIface.class));
        assertTrue(Beans.isRegisteredXmlBindingClass(G133XmlIface.class));
    }

    // unchanged: a concrete class with an explicit registration whose construction fails is still demoted
    @Test
    public void testRegisterXmlBindingClass_concreteUnconstructibleClassIsStillDemoted() {
        Beans.registerXmlBindingClass(G133ConcreteNoCtorBean.class);
        assertTrue(Beans.isRegisteredXmlBindingClass(G133ConcreteNoCtorBean.class));

        assertFalse(Beans.getPropNameList(G133ConcreteNoCtorBean.class).contains("items"));
        assertFalse(Beans.isRegisteredXmlBindingClass(G133ConcreteNoCtorBean.class));
    }

    // A registered abstract class with a detected builder, used directly as the conversion target, is built through
    // that builder: an abstract class can never take the "registered XML-binding class -> no-arg constructor" path.

    // with a getter-only collection without a backing field: r108 demoted it, which happened to select the builder
    public abstract static class G133BuiltBase {
        private final String name;

        protected G133BuiltBase(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public List<String> getAliases() {
            return new java.util.ArrayList<>(List.of("alias"));
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

            public G133BuiltBase build() {
                return new G133BuiltImpl(name);
            }
        }
    }

    public static class G133BuiltImpl extends G133BuiltBase {
        G133BuiltImpl(final String name) {
            super(name);
        }
    }

    // without such a getter: never demoted, so r108 tried the no-arg constructor of the abstract class and failed
    public abstract static class G133BuiltBase2 {
        private final String name;
        private final List<String> tags = new java.util.ArrayList<>();

        protected G133BuiltBase2(final String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public List<String> getTags() {
            return tags;
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

            public G133BuiltBase2 build() {
                return new G133BuiltImpl2(name);
            }
        }
    }

    public static class G133BuiltImpl2 extends G133BuiltBase2 {
        G133BuiltImpl2(final String name) {
            super(name);
        }
    }

    // passes on r108 as well: keeping the registration of the abstract class must not take its builder path away
    @Test
    public void testRegisterXmlBindingClass_abstractClassWithBuilderAndGetterOnlyCollectionIsBuiltThroughBuilder() {
        Beans.registerXmlBindingClass(G133BuiltBase.class);

        assertEquals(List.of("name"), Beans.getPropNameList(G133BuiltBase.class));

        final G133BuiltBase viaJson = N.fromJson("{\"name\":\"j\"}", G133BuiltBase.class);
        assertEquals(G133BuiltImpl.class, viaJson.getClass());
        assertEquals("j", viaJson.getName());

        final G133BuiltBase viaXml = N.fromXml("<g133BuiltBase><name>x</name></g133BuiltBase>", G133BuiltBase.class);
        assertEquals(G133BuiltImpl.class, viaXml.getClass());
        assertEquals("x", viaXml.getName());

        final G133BuiltBase viaMap = Beans.mapToBean(Map.of("name", "m"), G133BuiltBase.class);
        assertEquals(G133BuiltImpl.class, viaMap.getClass());
        assertEquals("m", viaMap.getName());
    }

    @Test
    public void testRegisterXmlBindingClass_abstractClassWithBuilderIsBuiltThroughBuilder() {
        Beans.registerXmlBindingClass(G133BuiltBase2.class);

        assertEquals(List.of("name", "tags"), Beans.getPropNameList(G133BuiltBase2.class));
        assertTrue(Beans.isRegisteredXmlBindingClass(G133BuiltBase2.class));

        final G133BuiltBase2 viaJson = N.fromJson("{\"name\":\"j\"}", G133BuiltBase2.class);
        assertEquals(G133BuiltImpl2.class, viaJson.getClass());
        assertEquals("j", viaJson.getName());

        final G133BuiltBase2 viaXml = N.fromXml("<g133BuiltBase2><name>x</name></g133BuiltBase2>", G133BuiltBase2.class);
        assertEquals(G133BuiltImpl2.class, viaXml.getClass());
        assertEquals("x", viaXml.getName());

        final G133BuiltBase2 viaMap = Beans.mapToBean(Map.of("name", "m"), G133BuiltBase2.class);
        assertEquals(G133BuiltImpl2.class, viaMap.getClass());
        assertEquals("m", viaMap.getName());
    }

    // ---- deep review 2026-10-27 G133 end ----

}
