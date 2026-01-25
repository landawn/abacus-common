package com.landawn.abacus.util;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;

@Tag("new-test")
public class Holder100Test extends TestBase {

    @Test
    public void testDefaultConstructor() {
        Holder<String> holder = new Holder<>();
        Assertions.assertNull(holder.value());
        Assertions.assertTrue(holder.isNull());
        Assertions.assertFalse(holder.isNotNull());
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
    public void testSetValue() {
        Holder<String> holder = new Holder<>();
        holder.setValue("new value");
        Assertions.assertEquals("new value", holder.value());

        holder.setValue(null);
        Assertions.assertNull(holder.value());
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
    public void testSetIf() throws Exception {
        Holder<Integer> holder = Holder.of(10);

        boolean updated = holder.setIf(20, v -> v < 15);
        Assertions.assertTrue(updated);
        Assertions.assertEquals(20, holder.value());

        updated = holder.setIf(30, v -> v < 15);
        Assertions.assertFalse(updated);
        Assertions.assertEquals(20, holder.value());

        holder.setValue(null);
        updated = holder.setIf(5, v -> v == null);
        Assertions.assertTrue(updated);
        Assertions.assertEquals(5, holder.value());
    }

    @Test
    public void testSetIfWithBiPredicate() throws Exception {
        Holder<String> holder = Holder.of("old");

        boolean updated = holder.setIf("newVal", (current, newVal) -> current.length() < newVal.length());
        Assertions.assertTrue(updated);
        Assertions.assertEquals("newVal", holder.value());

        updated = holder.setIf("no", (current, newVal) -> current.length() < newVal.length());
        Assertions.assertFalse(updated);
        Assertions.assertEquals("newVal", holder.value());
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
    public void testIsNotNull() {
        Holder<String> holder = new Holder<>();
        Assertions.assertFalse(holder.isNotNull());

        holder.setValue("value");
        Assertions.assertTrue(holder.isNotNull());

        holder.setValue(null);
        Assertions.assertFalse(holder.isNotNull());
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
    public void testMap() throws Exception {
        Holder<String> holder = Holder.of("test");

        Integer length = holder.map(String::length);
        Assertions.assertEquals(4, length);

        holder.setValue(null);
        Object result = holder.map(v -> v == null ? "null" : v);
        Assertions.assertEquals("null", result);
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
    public void testHashCode() {
        Holder<String> holder1 = Holder.of("test");
        Holder<String> holder2 = Holder.of("test");
        Holder<String> holder3 = Holder.of("different");
        Holder<String> holderNull = Holder.of(null);

        Assertions.assertEquals(holder1.hashCode(), holder2.hashCode());

        Assertions.assertEquals(0, holderNull.hashCode());
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
    public void testWithPrimitiveWrapper() {
        Holder<Integer> intHolder = Holder.of(42);
        Assertions.assertEquals(42, intHolder.value());

        Holder<Boolean> boolHolder = Holder.of(true);
        Assertions.assertTrue(boolHolder.value());

        Holder<Double> doubleHolder = Holder.of(3.14);
        Assertions.assertEquals(3.14, doubleHolder.value(), 0.001);
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
    public void testComplexChaining() {
        Holder<String> holder = Holder.of("test");

        Optional<Integer> result = holder.mapIfNotNull(String::toUpperCase).mapIfNotNull(s -> s + "123").mapToNonNullIfNotNull(String::length);

        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(7, result.get());

        holder.setValue(null);
        result = holder.mapIfNotNull(String::toUpperCase).mapIfNotNull(s -> s + "123").mapToNonNullIfNotNull(String::length);

        Assertions.assertFalse(result.isPresent());
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
}
