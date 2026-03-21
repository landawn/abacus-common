package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;

public class HolderTest extends TestBase {

    @Test
    public void testWithPrimitiveWrapper() {
        Holder<Integer> intHolder = Holder.of(42);
        Assertions.assertEquals(42, intHolder.value());

        Holder<Boolean> boolHolder = Holder.of(true);
        Assertions.assertTrue(boolHolder.value());

        Holder<Double> doubleHolder = Holder.of(3.14);
        Assertions.assertEquals(3.14, doubleHolder.value(), 0.001);
    }

    @Test
    public void test_of_WithValue() {
        Holder<String> holder = Holder.of("test");
        assertEquals("test", holder.value());
        assertFalse(holder.isNull());
    }

    @Test
    public void test_of_WithNull() {
        Holder<String> holder = Holder.of(null);
        assertNull(holder.value());
        assertTrue(holder.isNull());
    }

    @Test
    public void testOf() {
        Holder<String> holder = Holder.of("test");
        Assertions.assertEquals("test", holder.value());
        Assertions.assertFalse(holder.isNull());
        Assertions.assertTrue(holder.isNotNull());

        holder = Holder.of(null);
        Assertions.assertNull(holder.value());
        Assertions.assertTrue(holder.isNull());
    }

    @Test
    public void testOrDefaultIfNull() {
        Holder<String> holder = Holder.of("value");
        String result = holder.orElseIfNull("default");
        Assertions.assertEquals("value", result);

        holder.setValue(null);
        result = holder.orElseIfNull("default");
        Assertions.assertEquals("default", result);

        result = holder.orElseIfNull(null);
        Assertions.assertNull(result);
    }

    @Test
    public void testWithCustomObject() {
        class Person {
            String name;
            int age;

            Person(String name, int age) {
                this.name = name;
                this.age = age;
            }
        }

        Person person = new Person("John", 30);
        Holder<Person> personHolder = Holder.of(person);

        Assertions.assertSame(person, personHolder.value());

        String name = personHolder.map(p -> p.name);
        Assertions.assertEquals("John", name);

        personHolder.updateAndGet(p -> {
            p.age = 31;
            return p;
        });
        Assertions.assertEquals(31, person.age);
    }

    @Test
    public void test_chainedOperations() throws Exception {
        Holder<String> holder = Holder.of("hello");

        String upper = holder.map(String::toUpperCase);
        assertEquals("HELLO", upper);

        assertEquals("hello", holder.value());

        Nullable<Integer> length = holder.mapIfNotNull(String::toUpperCase).map(String::length);
        assertEquals(5, length.get());
    }

    @Test
    public void test_exceptionPropagation() {
        Holder<String> holder = Holder.of("test");

        assertThrows(RuntimeException.class, () -> {
            holder.map(s -> {
                throw new RuntimeException("Test exception");
            });
        });
    }

    @Test
    public void testThreadSafety() throws InterruptedException {
        Holder<Integer> holder = Holder.of(0);

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                holder.updateAndGet(v -> v + 1);
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < 1000; i++) {
                holder.updateAndGet(v -> v + 1);
            }
        });

        t1.start();
        t2.start();
        t1.join();
        t2.join();

        Assertions.assertTrue(holder.value() > 0);
        Assertions.assertTrue(holder.value() <= 2000);
    }

    @Test
    public void testExceptionHandling() {
        Holder<String> holder = Holder.of("test");

        Assertions.assertThrows(RuntimeException.class, () -> holder.updateAndGet(v -> {
            throw new RuntimeException("Test exception");
        }));

        Assertions.assertEquals("test", holder.value());

        Assertions.assertThrows(RuntimeException.class, () -> holder.map(v -> {
            throw new RuntimeException("Test exception");
        }));
    }

    @Test
    public void test_Constructor_Default() {
        Holder<String> holder = new Holder<>();
        assertNull(holder.value());
        assertTrue(holder.isNull());
    }

    @Test
    public void test_value_NonNull() {
        Holder<Integer> holder = Holder.of(42);
        assertEquals(42, holder.value());
    }

    @Test
    public void test_value_Null() {
        Holder<String> holder = Holder.of(null);
        assertNull(holder.value());
    }

    @Test
    public void testDefaultConstructor() {
        Holder<String> holder = new Holder<>();
        Assertions.assertNull(holder.value());
        Assertions.assertTrue(holder.isNull());
        Assertions.assertFalse(holder.isNotNull());
    }

    @Test
    public void testValue() {
        Holder<Integer> holder = Holder.of(42);
        Assertions.assertEquals(42, holder.value());

        holder = Holder.of(null);
        Assertions.assertNull(holder.value());
    }

    @Test
    public void testGetValue() {
        Holder<String> holder = Holder.of("deprecated");
        Assertions.assertEquals("deprecated", holder.getValue());
    }

    @Test
    public void test_getValue_NonNull() {
        Holder<String> holder = Holder.of("test");
        assertEquals("test", holder.getValue());
    }

    @Test
    public void test_getValue_Null() {
        Holder<String> holder = Holder.of(null);
        assertNull(holder.getValue());
    }

    @Test
    public void test_setValue_Replace() {
        Holder<Integer> holder = Holder.of(10);
        holder.setValue(20);
        assertEquals(20, holder.value());
    }

    @Test
    public void test_setValue_NonNull() {
        Holder<String> holder = new Holder<>();
        holder.setValue("value");
        assertEquals("value", holder.value());
    }

    @Test
    public void test_setValue_Null() {
        Holder<String> holder = Holder.of("test");
        holder.setValue(null);
        assertNull(holder.value());
    }

    @Test
    public void testSetValue() {
        Holder<String> holder = new Holder<>();
        holder.setValue("new value");
        Assertions.assertEquals("new value", holder.value());

        holder.setValue(null);
        Assertions.assertNull(holder.value());
    }

    @Test
    public void test_multipleOperations() throws Exception {
        Holder<Integer> holder = new Holder<>();
        holder.setValue(5);
        assertEquals(5, holder.value());

        Integer old = holder.getAndSet(10);
        assertEquals(5, old);
        assertEquals(10, holder.value());

        Integer updated = holder.updateAndGet(n -> n * 2);
        assertEquals(20, updated);
        assertEquals(20, holder.value());
    }

    @Test
    public void test_getAndSet_NonNull() {
        Holder<String> holder = Holder.of("old");
        String previous = holder.getAndSet("new");
        assertEquals("old", previous);
        assertEquals("new", holder.value());
    }

    @Test
    public void test_getAndSet_Null() {
        Holder<String> holder = Holder.of(null);
        String previous = holder.getAndSet("new");
        assertNull(previous);
        assertEquals("new", holder.value());
    }

    @Test
    public void test_getAndSet_ToNull() {
        Holder<String> holder = Holder.of("value");
        String previous = holder.getAndSet(null);
        assertEquals("value", previous);
        assertNull(holder.value());
    }

    @Test
    public void testGetAndSet() {
        Holder<String> holder = Holder.of("old");
        String old = holder.getAndSet("new");

        Assertions.assertEquals("old", old);
        Assertions.assertEquals("new", holder.value());

        old = holder.getAndSet(null);
        Assertions.assertEquals("new", old);
        Assertions.assertNull(holder.value());
    }

    @Test
    public void test_setAndGet_NonNull() {
        Holder<Integer> holder = Holder.of(10);
        Integer newValue = holder.setAndGet(20);
        assertEquals(20, newValue);
        assertEquals(20, holder.value());
    }

    @Test
    public void test_setAndGet_Null() {
        Holder<String> holder = Holder.of("test");
        String newValue = holder.setAndGet(null);
        assertNull(newValue);
        assertNull(holder.value());
    }

    @Test
    public void testSetAndGet() {
        Holder<String> holder = new Holder<>();
        String result = holder.setAndGet("value");

        Assertions.assertEquals("value", result);
        Assertions.assertEquals("value", holder.value());

        result = holder.setAndGet(null);
        Assertions.assertNull(result);
        Assertions.assertNull(holder.value());
    }

    @Test
    public void test_getAndUpdate_NonNull() throws Exception {
        Holder<Integer> holder = Holder.of(5);
        Integer previous = holder.getAndUpdate(n -> n * 2);
        assertEquals(5, previous);
        assertEquals(10, holder.value());
    }

    @Test
    public void test_getAndUpdate_Null() throws Exception {
        Holder<Integer> holder = Holder.of(null);
        Integer previous = holder.getAndUpdate(n -> n == null ? 0 : n * 2);
        assertNull(previous);
        assertEquals(0, holder.value());
    }

    @Test
    public void test_getAndUpdate_ToNull() throws Exception {
        Holder<String> holder = Holder.of("test");
        String previous = holder.getAndUpdate(s -> null);
        assertEquals("test", previous);
        assertNull(holder.value());
    }

    @Test
    public void testGetAndUpdate() throws Exception {
        Holder<Integer> holder = Holder.of(10);
        Integer old = holder.getAndUpdate(v -> v + 5);

        Assertions.assertEquals(10, old);
        Assertions.assertEquals(15, holder.value());

        holder.setValue(null);
        old = holder.getAndUpdate(v -> v == null ? 0 : v + 1);
        Assertions.assertNull(old);
        Assertions.assertEquals(0, holder.value());
    }

    @Test
    public void testGetAndUpdate_ChainedTransformations() throws Exception {
        Holder<Integer> holder = Holder.of(1);
        int old1 = holder.getAndUpdate(v -> v + 10);
        assertEquals(1, old1);
        assertEquals(11, holder.value().intValue());

        int old2 = holder.getAndUpdate(v -> v * 2);
        assertEquals(11, old2);
        assertEquals(22, holder.value().intValue());
    }

    @Test
    public void test_updateAndGet_NonNull() throws Exception {
        Holder<Integer> holder = Holder.of(5);
        Integer updated = holder.updateAndGet(n -> n * 2);
        assertEquals(10, updated);
        assertEquals(10, holder.value());
    }

    @Test
    public void test_updateAndGet_Null() throws Exception {
        Holder<Integer> holder = Holder.of(null);
        Integer updated = holder.updateAndGet(n -> n == null ? 10 : n * 2);
        assertEquals(10, updated);
        assertEquals(10, holder.value());
    }

    @Test
    public void test_updateAndGet_ToNull() throws Exception {
        Holder<String> holder = Holder.of("test");
        String updated = holder.updateAndGet(s -> null);
        assertNull(updated);
        assertNull(holder.value());
    }

    @Test
    public void testUpdateAndGet() throws Exception {
        Holder<Integer> holder = Holder.of(10);
        Integer result = holder.updateAndGet(v -> v * 2);

        Assertions.assertEquals(20, result);
        Assertions.assertEquals(20, holder.value());

        holder.setValue(null);
        result = holder.updateAndGet(v -> v == null ? 5 : v * 2);
        Assertions.assertEquals(5, result);
        Assertions.assertEquals(5, holder.value());
    }

    @Test
    public void testUpdateAndGet_ChainedTransformations() throws Exception {
        Holder<Integer> holder = Holder.of(5);
        int newVal1 = holder.updateAndGet(v -> v + 5);
        assertEquals(10, newVal1);

        int newVal2 = holder.updateAndGet(v -> v * 3);
        assertEquals(30, newVal2);
    }

    @Test
    public void test_setIf_PredicateTrue() throws Exception {
        Holder<Integer> holder = Holder.of(5);
        boolean result = holder.setIf(n -> n != null && n < 10, 10);
        assertTrue(result);
        assertEquals(10, holder.value());
    }

    @Test
    public void test_setIf_PredicateFalse() throws Exception {
        Holder<Integer> holder = Holder.of(15);
        boolean result = holder.setIf(n -> n != null && n < 10, 10);
        assertFalse(result);
        assertEquals(15, holder.value());
    }

    @Test
    public void test_setIf_NullValue() throws Exception {
        Holder<String> holder = Holder.of(null);
        boolean result = holder.setIf(n -> n == null, "new");
        assertTrue(result);
        assertEquals("new", holder.value());
    }

    @Test
    public void testSetIf() throws Exception {
        Holder<Integer> holder = Holder.of(10);

        boolean updated = holder.setIf(v -> v < 15, 20);
        Assertions.assertTrue(updated);
        Assertions.assertEquals(20, holder.value());

        updated = holder.setIf(v -> v < 15, 30);
        Assertions.assertFalse(updated);
        Assertions.assertEquals(20, holder.value());

        holder.setValue(null);
        updated = holder.setIf(v -> v == null, 5);
        Assertions.assertTrue(updated);
        Assertions.assertEquals(5, holder.value());
    }

    @Test
    public void testSetIf_WithNullOldValue() throws Exception {
        Holder<String> holder = Holder.of(null);
        boolean updated = holder.setIf(v -> v == null, "set");
        assertTrue(updated);
        assertEquals("set", holder.value());
    }

    //
    //    @Test
    //    public void test_setIf_BiPredicateFalse() throws Exception {
    //        Holder<Integer> holder = Holder.of(15);
    //        boolean result = holder.setIf((oldVal, newVal) -> oldVal < newVal, 10);
    //        assertFalse(result);
    //        assertEquals(15, holder.value());
    //    }

    //
    //

    @Test
    public void test_isNull_True() {
        Holder<String> holder = Holder.of(null);
        assertTrue(holder.isNull());
    }

    @Test
    public void test_isNull_False() {
        Holder<String> holder = Holder.of("test");
        assertFalse(holder.isNull());
    }

    @Test
    public void testIsNull() {
        Holder<String> holder = new Holder<>();
        Assertions.assertTrue(holder.isNull());

        holder.setValue("value");
        Assertions.assertFalse(holder.isNull());

        holder.setValue(null);
        Assertions.assertTrue(holder.isNull());
    }

    @Test
    public void test_isNotNull_True() {
        Holder<String> holder = Holder.of("test");
        assertTrue(holder.isNotNull());
    }

    @Test
    public void test_isNotNull_False() {
        Holder<String> holder = Holder.of(null);
        assertFalse(holder.isNotNull());
    }

    @Test
    public void testIsNotNull() {
        Holder<String> holder = new Holder<>();
        Assertions.assertFalse(holder.isNotNull());

        holder.setValue("value");
        Assertions.assertTrue(holder.isNotNull());

        holder.setValue(null);
        Assertions.assertFalse(holder.isNotNull());
    }

    @Test
    public void test_ifNotNull_WithValue() throws Exception {
        Holder<Integer> holder = Holder.of(10);
        Holder<Integer> result = new Holder<>();
        holder.ifNotNull(value -> result.setValue(value * 2));
        assertEquals(20, result.value());
    }

    @Test
    public void test_ifNotNull_WithNull() throws Exception {
        Holder<Integer> holder = Holder.of(null);
        Holder<Integer> result = new Holder<>();
        holder.ifNotNull(value -> result.setValue(value * 2));
        assertNull(result.value());
    }

    @Test
    public void test_ifNotNull_NullAction() {
        Holder<String> holder = Holder.of("test");
        assertThrows(IllegalArgumentException.class, () -> holder.ifNotNull(null));
    }

    @Test
    public void testIfNotNull() throws Exception {
        Holder<String> holder = Holder.of("test");

        boolean[] called = { false };
        holder.ifNotNull(value -> {
            called[0] = true;
            Assertions.assertEquals("test", value);
        });
        Assertions.assertTrue(called[0]);

        holder.setValue(null);
        called[0] = false;
        holder.ifNotNull(value -> called[0] = true);
        Assertions.assertFalse(called[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> holder.ifNotNull(null));
    }

    @Test
    public void test_ifNotNullOrElse_WithValue() throws Exception {
        Holder<Integer> holder = Holder.of(10);
        Holder<String> result = new Holder<>();
        holder.ifNotNullOrElse(value -> result.setValue("value: " + value), () -> result.setValue("empty"));
        assertEquals("value: 10", result.value());
    }

    @Test
    public void test_ifNotNullOrElse_WithNull() throws Exception {
        Holder<Integer> holder = Holder.of(null);
        Holder<String> result = new Holder<>();
        holder.ifNotNullOrElse(value -> result.setValue("value: " + value), () -> result.setValue("empty"));
        assertEquals("empty", result.value());
    }

    @Test
    public void test_ifNotNullOrElse_NullAction() {
        Holder<String> holder = Holder.of("test");
        assertThrows(IllegalArgumentException.class, () -> holder.ifNotNullOrElse(null, () -> {
        }));
    }

    @Test
    public void test_ifNotNullOrElse_NullEmptyAction() {
        Holder<String> holder = Holder.of("test");
        assertThrows(IllegalArgumentException.class, () -> holder.ifNotNullOrElse(v -> {
        }, null));
    }

    @Test
    public void testIfNotNullOrElse() throws Exception {
        Holder<String> holder = Holder.of("value");

        boolean[] actionCalled = { false };
        boolean[] emptyActionCalled = { false };

        holder.ifNotNullOrElse(value -> {
            actionCalled[0] = true;
            Assertions.assertEquals("value", value);
        }, () -> emptyActionCalled[0] = true);

        Assertions.assertTrue(actionCalled[0]);
        Assertions.assertFalse(emptyActionCalled[0]);

        holder.setValue(null);
        actionCalled[0] = false;
        emptyActionCalled[0] = false;

        holder.ifNotNullOrElse(value -> actionCalled[0] = true, () -> emptyActionCalled[0] = true);

        Assertions.assertFalse(actionCalled[0]);
        Assertions.assertTrue(emptyActionCalled[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> holder.ifNotNullOrElse(null, () -> {
        }));
        Assertions.assertThrows(IllegalArgumentException.class, () -> holder.ifNotNullOrElse(v -> {
        }, null));
    }

    @Test
    public void test_accept_WithValue() throws Exception {
        Holder<Integer> holder = Holder.of(10);
        Holder<Integer> result = new Holder<>();
        holder.accept(value -> result.setValue(value));
        assertEquals(10, result.value());
    }

    @Test
    public void test_accept_WithNull() throws Exception {
        Holder<Integer> holder = Holder.of(null);
        Holder<Integer> result = new Holder<>();
        holder.accept(value -> result.setValue(value));
        assertNull(result.value());
    }

    @Test
    public void testAccept() throws Exception {
        Holder<String> holder = Holder.of("test");

        String[] result = { null };
        holder.accept(value -> result[0] = value);
        Assertions.assertEquals("test", result[0]);

        holder.setValue(null);
        result[0] = "unchanged";
        holder.accept(value -> result[0] = value);
        Assertions.assertNull(result[0]);
    }

    @Test
    public void test_acceptIfNotNull_WithValue() throws Exception {
        Holder<Integer> holder = Holder.of(10);
        Holder<Integer> result = new Holder<>();
        holder.acceptIfNotNull(value -> result.setValue(value * 2));
        assertEquals(20, result.value());
    }

    @Test
    public void test_acceptIfNotNull_WithNull() throws Exception {
        Holder<Integer> holder = Holder.of(null);
        Holder<Integer> result = new Holder<>();
        holder.acceptIfNotNull(value -> result.setValue(value * 2));
        assertNull(result.value());
    }

    @Test
    public void testAcceptIfNotNull() throws Exception {
        Holder<String> holder = Holder.of("test");

        boolean[] called = { false };
        holder.acceptIfNotNull(value -> {
            called[0] = true;
            Assertions.assertEquals("test", value);
        });
        Assertions.assertTrue(called[0]);

        holder.setValue(null);
        called[0] = false;
        holder.acceptIfNotNull(value -> called[0] = true);
        Assertions.assertFalse(called[0]);

        Assertions.assertThrows(IllegalArgumentException.class, () -> holder.acceptIfNotNull(null));
    }

    @Test
    public void test_map_NonNull() throws Exception {
        Holder<String> holder = Holder.of("hello");
        Integer length = holder.map(String::length);
        assertEquals(5, length);
    }

    @Test
    public void test_map_Null() throws Exception {
        Holder<String> holder = Holder.of(null);
        Integer length = holder.map(s -> s == null ? 0 : s.length());
        assertEquals(0, length);
    }

    @Test
    public void test_map_ToNull() throws Exception {
        Holder<String> holder = Holder.of("test");
        String result = holder.map(s -> null);
        assertNull(result);
    }

    @Test
    public void testMap() throws Exception {
        Holder<String> holder = Holder.of("test");

        Integer length = holder.map(String::length);
        Assertions.assertEquals(4, length);

        holder.setValue(null);
        Object result = holder.map(v -> v == null ? "null" : v);
        Assertions.assertEquals("null", result);
    }

    @Test
    public void test_mapIfNotNull_WithValue() throws Exception {
        Holder<String> holder = Holder.of("hello");
        Nullable<Integer> result = holder.mapIfNotNull(String::length);
        assertTrue(result.isPresent());
        assertEquals(5, result.get());
    }

    @Test
    public void test_mapIfNotNull_WithNull() throws Exception {
        Holder<String> holder = Holder.of(null);
        Nullable<Integer> result = holder.mapIfNotNull(String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_mapIfNotNull_NullMapper() {
        Holder<String> holder = Holder.of("test");
        assertThrows(IllegalArgumentException.class, () -> holder.mapIfNotNull(null));
    }

    @Test
    public void testMapIfNotNull() throws Exception {
        Holder<String> holder = Holder.of("test");

        Nullable<Integer> length = holder.mapIfNotNull(String::length);
        Assertions.assertTrue(length.isPresent());
        Assertions.assertEquals(4, length.get());

        holder.setValue(null);
        Nullable<String> result = holder.mapIfNotNull(String::toUpperCase);
        Assertions.assertFalse(result.isPresent());

        Assertions.assertThrows(IllegalArgumentException.class, () -> holder.mapIfNotNull(null));
    }

    @Test
    public void testMapIfNotNull_ReturnsNull() throws Exception {
        Holder<String> holder = Holder.of("test");
        com.landawn.abacus.util.u.Nullable<String> result = holder.mapIfNotNull(v -> null);
        assertTrue(result.isPresent());
        assertNull(result.get());
    }

    @Test
    public void test_mapToNonNullIfNotNull_WithValue() throws Exception {
        Holder<String> holder = Holder.of("test");
        Optional<Integer> result = holder.mapToNonNullIfNotNull(String::length);
        assertTrue(result.isPresent());
        assertEquals(4, result.get());
    }

    @Test
    public void test_mapToNonNullIfNotNull_WithNull() throws Exception {
        Holder<String> holder = Holder.of(null);
        Optional<Integer> result = holder.mapToNonNullIfNotNull(String::length);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_mapToNonNullIfNotNull_NullMapper() {
        Holder<String> holder = Holder.of("test");
        assertThrows(IllegalArgumentException.class, () -> holder.mapToNonNullIfNotNull(null));
    }

    @Test
    public void testMapToNonNullIfNotNull() throws Exception {
        Holder<String> holder = Holder.of("test");

        Optional<Integer> length = holder.mapToNonNullIfNotNull(String::length);
        Assertions.assertTrue(length.isPresent());
        Assertions.assertEquals(4, length.get());

        holder.setValue(null);
        Optional<String> result = holder.mapToNonNullIfNotNull(String::toUpperCase);
        Assertions.assertFalse(result.isPresent());

        Assertions.assertThrows(IllegalArgumentException.class, () -> holder.mapToNonNullIfNotNull(null));
    }

    @Test
    public void test_filter_PredicateTrue() throws Exception {
        Holder<Integer> holder = Holder.of(10);
        Nullable<Integer> result = holder.filter(n -> n != null && n > 5);
        assertTrue(result.isPresent());
        assertEquals(10, result.get());
    }

    @Test
    public void test_filter_PredicateFalse() throws Exception {
        Holder<Integer> holder = Holder.of(3);
        Nullable<Integer> result = holder.filter(n -> n != null && n > 5);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_filter_Null() throws Exception {
        Holder<Integer> holder = Holder.of(null);
        Nullable<Integer> result = holder.filter(n -> n == null);
        assertTrue(result.isPresent());
        assertNull(result.get());
    }

    @Test
    public void testFilter() throws Exception {
        Holder<Integer> holder = Holder.of(10);

        Nullable<Integer> result = holder.filter(v -> v > 5);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(10, result.get());

        result = holder.filter(v -> v > 20);
        Assertions.assertFalse(result.isPresent());

        holder.setValue(null);
        result = holder.filter(v -> v == null);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertNull(result.orElseNull());
    }

    @Test
    public void testFilter_NullValueMatchesPredicate() throws Exception {
        Holder<String> holder = Holder.of(null);
        com.landawn.abacus.util.u.Nullable<String> result = holder.filter(v -> v == null);
        assertTrue(result.isPresent());
        assertNull(result.get());
    }

    @Test
    public void testFilter_NullValueDoesNotMatch() throws Exception {
        Holder<String> holder = Holder.of(null);
        com.landawn.abacus.util.u.Nullable<String> result = holder.filter(v -> v != null);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_filterIfNotNull_PredicateTrue() throws Exception {
        Holder<Integer> holder = Holder.of(10);
        Optional<Integer> result = holder.filterIfNotNull(n -> n > 5);
        assertTrue(result.isPresent());
        assertEquals(10, result.get());
    }

    @Test
    public void test_filterIfNotNull_PredicateFalse() throws Exception {
        Holder<Integer> holder = Holder.of(3);
        Optional<Integer> result = holder.filterIfNotNull(n -> n > 5);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_filterIfNotNull_WithNull() throws Exception {
        Holder<Integer> holder = Holder.of(null);
        Optional<Integer> result = holder.filterIfNotNull(n -> n > 5);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_filterIfNotNull_NullPredicate() {
        Holder<Integer> holder = Holder.of(10);
        assertThrows(IllegalArgumentException.class, () -> holder.filterIfNotNull(null));
    }

    @Test
    public void testFilterIfNotNull() throws Exception {
        Holder<Integer> holder = Holder.of(10);

        Optional<Integer> result = holder.filterIfNotNull(v -> v > 5);
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(10, result.get());

        result = holder.filterIfNotNull(v -> v > 20);
        Assertions.assertFalse(result.isPresent());

        holder.setValue(null);
        result = holder.filterIfNotNull(v -> true);
        Assertions.assertFalse(result.isPresent());

        Assertions.assertThrows(IllegalArgumentException.class, () -> holder.filterIfNotNull(null));
    }

    @Test
    public void testFilterIfNotNull_NullValue() throws Exception {
        Holder<String> holder = Holder.of(null);
        com.landawn.abacus.util.u.Optional<String> result = holder.filterIfNotNull(v -> true);
        assertFalse(result.isPresent());
    }

    @Test
    public void test_orElseIfNull_WithValue() {
        Holder<String> holder = Holder.of("test");
        String result = holder.orElseIfNull("default");
        assertEquals("test", result);
    }

    @Test
    public void test_orElseIfNull_WithNull() {
        Holder<String> holder = Holder.of(null);
        String result = holder.orElseIfNull("default");
        assertEquals("default", result);
    }

    @Test
    public void test_orElseIfNull_NullDefault() {
        Holder<String> holder = Holder.of(null);
        String result = holder.orElseIfNull(null);
        assertNull(result);
    }

    @Test
    public void testOrElseIfNull() {
        Holder<String> holder = Holder.of("hello");
        Assertions.assertEquals("hello", holder.orElseIfNull("fallback"));

        Holder<String> nullHolder = Holder.of(null);
        Assertions.assertEquals("fallback", nullHolder.orElseIfNull("fallback"));
        Assertions.assertNull(nullHolder.orElseIfNull(null));
    }

    @Test
    public void test_orElseGetIfNull_WithValue() {
        Holder<String> holder = Holder.of("test");
        String result = holder.orElseGetIfNull(() -> "default");
        assertEquals("test", result);
    }

    @Test
    public void test_orElseGetIfNull_WithNull() {
        Holder<String> holder = Holder.of(null);
        String result = holder.orElseGetIfNull(() -> "default");
        assertEquals("default", result);
    }

    @Test
    public void testOrElseGetIfNull_SupplierReturnsNull() {
        Holder<String> holder = Holder.of(null);
        String result = holder.orElseGetIfNull(() -> null);
        assertNull(result);
    }

    @Test
    public void testOrElseGetIfNull_ValuePresent() {
        Holder<String> holder = Holder.of("exists");
        String result = holder.orElseGetIfNull(() -> "default");
        assertEquals("exists", result);
    }

    @Test
    public void test_orElseGetIfNull_NullSupplier() {
        Holder<String> holder = Holder.of(null);
        assertThrows(IllegalArgumentException.class, () -> holder.orElseGetIfNull(null));
    }

    @Test
    public void testOrElseGetIfNull() {
        Holder<String> holder = Holder.of("value");
        String result = holder.orElseGetIfNull(() -> "default");
        Assertions.assertEquals("value", result);

        holder.setValue(null);
        result = holder.orElseGetIfNull(() -> "default");
        Assertions.assertEquals("default", result);

        Assertions.assertThrows(IllegalArgumentException.class, () -> holder.orElseGetIfNull(null));
    }

    @Test
    public void test_orElseThrowIfNull_WithValue() {
        Holder<String> holder = Holder.of("test");
        String result = holder.orElseThrowIfNull();
        assertEquals("test", result);
    }

    @Test
    public void test_orElseThrowIfNull_WithNull() {
        Holder<String> holder = Holder.of(null);
        assertThrows(NoSuchElementException.class, () -> holder.orElseThrowIfNull());
    }

    @Test
    public void test_orElseThrowIfNull_String_WithValue() {
        Holder<String> holder = Holder.of("test");
        String result = holder.orElseThrowIfNull("Custom error");
        assertEquals("test", result);
    }

    @Test
    public void test_orElseThrowIfNull_String_WithNull() {
        Holder<String> holder = Holder.of(null);
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> holder.orElseThrowIfNull("Custom error"));
        assertEquals("Custom error", ex.getMessage());
    }

    @Test
    public void test_orElseThrowIfNull_OneParam_WithValue() {
        Holder<String> holder = Holder.of("test");
        String result = holder.orElseThrowIfNull("Error: %s", "param1");
        assertEquals("test", result);
    }

    @Test
    public void test_orElseThrowIfNull_OneParam_WithNull() {
        Holder<String> holder = Holder.of(null);
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> holder.orElseThrowIfNull("Error: %s", "param1"));
        assertTrue(ex.getMessage().contains("param1"));
    }

    @Test
    public void test_orElseThrowIfNull_TwoParams_WithValue() {
        Holder<String> holder = Holder.of("test");
        String result = holder.orElseThrowIfNull("Error: %s %s", "p1", "p2");
        assertEquals("test", result);
    }

    @Test
    public void test_orElseThrowIfNull_TwoParams_WithNull() {
        Holder<String> holder = Holder.of(null);
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> holder.orElseThrowIfNull("Error: %s %s", "p1", "p2"));
        assertTrue(ex.getMessage().contains("p1"));
        assertTrue(ex.getMessage().contains("p2"));
    }

    @Test
    public void test_orElseThrowIfNull_ThreeParams_WithValue() {
        Holder<String> holder = Holder.of("test");
        String result = holder.orElseThrowIfNull("Error: %s %s %s", "p1", "p2", "p3");
        assertEquals("test", result);
    }

    @Test
    public void test_orElseThrowIfNull_ThreeParams_WithNull() {
        Holder<String> holder = Holder.of(null);
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> holder.orElseThrowIfNull("Error: %s %s %s", "p1", "p2", "p3"));
        assertTrue(ex.getMessage().contains("p1"));
        assertTrue(ex.getMessage().contains("p2"));
        assertTrue(ex.getMessage().contains("p3"));
    }

    @Test
    public void test_orElseThrowIfNull_Varargs_WithValue() {
        Holder<String> holder = Holder.of("test");
        String result = holder.orElseThrowIfNull("Error: %s %s %s %s", "p1", "p2", "p3", "p4");
        assertEquals("test", result);
    }

    @Test
    public void test_orElseThrowIfNull_Varargs_WithNull() {
        Holder<String> holder = Holder.of(null);
        NoSuchElementException ex = assertThrows(NoSuchElementException.class, () -> holder.orElseThrowIfNull("Error: %s %s %s %s", "p1", "p2", "p3", "p4"));
        assertTrue(ex.getMessage().contains("p1"));
        assertTrue(ex.getMessage().contains("p4"));
    }

    @Test
    public void test_orElseThrowIfNull_Supplier_WithValue() throws Exception {
        Holder<String> holder = Holder.of("test");
        String result = holder.orElseThrowIfNull(() -> new IllegalStateException("Error"));
        assertEquals("test", result);
    }

    @Test
    public void test_orElseThrowIfNull_Supplier_WithNull() {
        Holder<String> holder = Holder.of(null);
        assertThrows(IllegalStateException.class, () -> holder.orElseThrowIfNull(() -> new IllegalStateException("Error")));
    }

    @Test
    public void test_orElseThrowIfNull_Supplier_NullSupplier() {
        Holder<String> holder = Holder.of(null);
        assertThrows(IllegalArgumentException.class, () -> holder.orElseThrowIfNull((Supplier<Exception>) null));
    }

    @Test
    public void testOrElseThrowIfNull() {
        Holder<String> holder = Holder.of("value");
        String result = holder.orElseThrowIfNull();
        Assertions.assertEquals("value", result);

        holder.setValue(null);
        Assertions.assertThrows(NoSuchElementException.class, () -> holder.orElseThrowIfNull());
    }

    @Test
    public void testOrElseThrowIfNullWithMessage() {
        Holder<String> holder = Holder.of("value");
        String result = holder.orElseThrowIfNull("Custom error");
        Assertions.assertEquals("value", result);

        holder.setValue(null);
        NoSuchElementException ex = Assertions.assertThrows(NoSuchElementException.class, () -> holder.orElseThrowIfNull("Custom error"));
        Assertions.assertEquals("Custom error", ex.getMessage());
    }

    @Test
    public void testOrElseThrowIfNullWithMessageAndParam() {
        Holder<String> holder = Holder.of("value");
        String result = holder.orElseThrowIfNull("Error: %s", "param1");
        Assertions.assertEquals("value", result);

        holder.setValue(null);
        NoSuchElementException ex = Assertions.assertThrows(NoSuchElementException.class, () -> holder.orElseThrowIfNull("Error: %s", "param1"));
        Assertions.assertTrue(ex.getMessage().contains("param1"));
    }

    @Test
    public void testOrElseThrowIfNullWithMessageAndTwoParams() {
        Holder<String> holder = Holder.of("value");
        String result = holder.orElseThrowIfNull("Error: %s %s", "param1", "param2");
        Assertions.assertEquals("value", result);

        holder.setValue(null);
        NoSuchElementException ex = Assertions.assertThrows(NoSuchElementException.class, () -> holder.orElseThrowIfNull("Error: %s %s", "param1", "param2"));
        Assertions.assertTrue(ex.getMessage().contains("param1"));
        Assertions.assertTrue(ex.getMessage().contains("param2"));
    }

    @Test
    public void testOrElseThrowIfNullWithMessageAndThreeParams() {
        Holder<String> holder = Holder.of("value");
        String result = holder.orElseThrowIfNull("Error: %s %s %s", "p1", "p2", "p3");
        Assertions.assertEquals("value", result);

        holder.setValue(null);
        NoSuchElementException ex = Assertions.assertThrows(NoSuchElementException.class, () -> holder.orElseThrowIfNull("Error: %s %s %s", "p1", "p2", "p3"));
        Assertions.assertTrue(ex.getMessage().contains("p1"));
        Assertions.assertTrue(ex.getMessage().contains("p2"));
        Assertions.assertTrue(ex.getMessage().contains("p3"));
    }

    @Test
    public void testOrElseThrowIfNullWithMessageAndVarargs() {
        Holder<String> holder = Holder.of("value");
        String result = holder.orElseThrowIfNull("Error: %s %s %s %s", "p1", "p2", "p3", "p4");
        Assertions.assertEquals("value", result);

        holder.setValue(null);
        NoSuchElementException ex = Assertions.assertThrows(NoSuchElementException.class,
                () -> holder.orElseThrowIfNull("Error: %s %s %s %s", "p1", "p2", "p3", "p4"));
        Assertions.assertTrue(ex.getMessage().contains("p1"));
        Assertions.assertTrue(ex.getMessage().contains("p4"));
    }

    @Test
    public void testOrElseThrowIfNullWithSupplier() {
        Holder<String> holder = Holder.of("value");
        String result = holder.orElseThrowIfNull(() -> new IllegalStateException("Custom exception"));
        Assertions.assertEquals("value", result);

        holder.setValue(null);
        Assertions.assertThrows(IllegalStateException.class, () -> holder.orElseThrowIfNull(() -> new IllegalStateException("Custom exception")));

        holder.setValue(null);
        Assertions.assertThrows(IllegalArgumentException.class, () -> holder.orElseThrowIfNull((Supplier<? extends Throwable>) null));
    }

    @Test
    public void testOrElseThrowIfNull_SupplierCustomException() {
        Holder<String> holder = Holder.of(null);
        assertThrows(IllegalStateException.class, () -> holder.orElseThrowIfNull(() -> new IllegalStateException("custom")));
    }

    @Test
    public void testOrElseThrowIfNull_SupplierWithValue() throws Throwable {
        Holder<String> holder = Holder.of("value");
        String result = holder.orElseThrowIfNull(() -> new RuntimeException("should not throw"));
        assertEquals("value", result);
    }

    @Test
    public void test_hashCode_WithValue() {
        Holder<String> holder = Holder.of("test");
        assertEquals("test".hashCode(), holder.hashCode());
    }

    @Test
    public void test_hashCode_Consistency() {
        Holder<Integer> holder = Holder.of(42);
        int hash1 = holder.hashCode();
        int hash2 = holder.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void test_hashCode_WithNull() {
        Holder<String> holder = Holder.of(null);
        assertEquals(0, holder.hashCode());
    }

    @Test
    public void testHashCode() {
        Holder<String> holder1 = Holder.of("test");
        Holder<String> holder2 = Holder.of("test");
        Holder<String> holder3 = Holder.of("different");
        Holder<String> holderNull = Holder.of(null);

        Assertions.assertEquals(holder1.hashCode(), holder2.hashCode());

        Assertions.assertEquals(0, holderNull.hashCode());
    }

    @Test
    public void testHashCode_NullValue() {
        Holder<String> holder = Holder.of(null);
        assertEquals(0, holder.hashCode());
    }

    @Test
    public void test_equals_EqualValues() {
        Holder<String> h1 = Holder.of("test");
        Holder<String> h2 = Holder.of("test");
        assertEquals(h1, h2);
    }

    @Test
    public void test_equals_DifferentValues() {
        Holder<String> h1 = Holder.of("test1");
        Holder<String> h2 = Holder.of("test2");
        assertNotEquals(h1, h2);
    }

    @Test
    public void test_equals_DifferentType() {
        Holder<String> holder = Holder.of("test");
        assertNotEquals(holder, "test");
    }

    @Test
    public void test_equals_SameInstance() {
        Holder<String> holder = Holder.of("test");
        assertEquals(holder, holder);
    }

    @Test
    public void test_equals_BothNull() {
        Holder<String> h1 = Holder.of(null);
        Holder<String> h2 = Holder.of(null);
        assertEquals(h1, h2);
    }

    @Test
    public void test_equals_OneNull() {
        Holder<String> h1 = Holder.of("test");
        Holder<String> h2 = Holder.of(null);
        assertNotEquals(h1, h2);
    }

    @Test
    public void test_equals_Null() {
        Holder<String> holder = Holder.of("test");
        assertNotEquals(holder, null);
    }

    @Test
    public void testEquals() {
        Holder<String> holder1 = Holder.of("test");
        Holder<String> holder2 = Holder.of("test");
        Holder<String> holder3 = Holder.of("different");
        Holder<String> holderNull1 = Holder.of(null);
        Holder<String> holderNull2 = Holder.of(null);

        Assertions.assertEquals(holder1, holder1);
        Assertions.assertEquals(holder1, holder2);
        Assertions.assertNotEquals(holder1, holder3);
        Assertions.assertEquals(holderNull1, holderNull2);
        Assertions.assertNotEquals(holder1, holderNull1);

        Assertions.assertNotEquals(holder1, "not a holder");
        Assertions.assertNotEquals(holder1, null);

        Holder<Integer> intHolder = Holder.of(123);
        Assertions.assertNotEquals(holder1, intHolder);
    }

    @Test
    public void testEquals_BothHoldNull() {
        Holder<String> h1 = Holder.of(null);
        Holder<String> h2 = Holder.of(null);
        assertEquals(h1, h2);
    }

    @Test
    public void test_toString_WithValue() {
        Holder<String> holder = Holder.of("test");
        assertEquals("Holder[test]", holder.toString());
    }

    @Test
    public void test_toString_WithInteger() {
        Holder<Integer> holder = Holder.of(42);
        assertEquals("Holder[42]", holder.toString());
    }

    @Test
    public void test_toString_WithNull() {
        Holder<String> holder = Holder.of(null);
        assertEquals("Holder[null]", holder.toString());
    }

    @Test
    public void testToString() {
        Holder<String> holder = Holder.of("test value");
        String str = holder.toString();
        Assertions.assertTrue(str.contains("Holder"));
        Assertions.assertTrue(str.contains("test value"));

        holder.setValue(null);
        str = holder.toString();
        Assertions.assertEquals("Holder[null]", str);

        Holder<Object> objHolder = Holder.of(new Object() {
            @Override
            public String toString() {
                return "CustomObject";
            }
        });
        str = objHolder.toString();
        Assertions.assertTrue(str.contains("CustomObject"));
    }

    @Test
    public void testComplexChaining() {
        Holder<String> holder = Holder.of("test");

        Optional<Integer> result = holder.mapIfNotNull(String::toUpperCase).mapIfNotNull(s -> s + "123").mapToNonNullIfNotNull(String::length);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(7, result.get());

        holder.setValue(null);
        result = holder.mapIfNotNull(String::toUpperCase).mapIfNotNull(s -> s + "123").mapToNonNullIfNotNull(String::length);

        Assertions.assertFalse(result.isPresent());
    }

}
