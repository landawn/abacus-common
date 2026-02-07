package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;
import java.util.function.Supplier;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Nullable;
import com.landawn.abacus.util.u.Optional;

@Tag("2025")
public class Holder2025Test extends TestBase {

    @Test
    public void test_Constructor_Default() {
        Holder<String> holder = new Holder<>();
        assertNull(holder.value());
        assertTrue(holder.isNull());
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
    public void test_setValue_Replace() {
        Holder<Integer> holder = Holder.of(10);
        holder.setValue(20);
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

    //    @Test
    //    public void test_setIf_BiPredicateTrue() throws Exception {
    //        Holder<Integer> holder = Holder.of(5);
    //        boolean result = holder.setIf((oldVal, newVal) -> oldVal < newVal, 10);
    //        assertTrue(result);
    //        assertEquals(10, holder.value());
    //    }
    //
    //    @Test
    //    public void test_setIf_BiPredicateFalse() throws Exception {
    //        Holder<Integer> holder = Holder.of(15);
    //        boolean result = holder.setIf((oldVal, newVal) -> oldVal < newVal, 10);
    //        assertFalse(result);
    //        assertEquals(15, holder.value());
    //    }

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
    public void test_orElseGetIfNull_NullSupplier() {
        Holder<String> holder = Holder.of(null);
        assertThrows(IllegalArgumentException.class, () -> holder.orElseGetIfNull(null));
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
    public void test_hashCode_WithValue() {
        Holder<String> holder = Holder.of("test");
        assertEquals("test".hashCode(), holder.hashCode());
    }

    @Test
    public void test_hashCode_WithNull() {
        Holder<String> holder = Holder.of(null);
        assertEquals(0, holder.hashCode());
    }

    @Test
    public void test_hashCode_Consistency() {
        Holder<Integer> holder = Holder.of(42);
        int hash1 = holder.hashCode();
        int hash2 = holder.hashCode();
        assertEquals(hash1, hash2);
    }

    @Test
    public void test_equals_SameInstance() {
        Holder<String> holder = Holder.of("test");
        assertEquals(holder, holder);
    }

    @Test
    public void test_equals_EqualValues() {
        Holder<String> h1 = Holder.of("test");
        Holder<String> h2 = Holder.of("test");
        assertEquals(h1, h2);
    }

    @Test
    public void test_equals_BothNull() {
        Holder<String> h1 = Holder.of(null);
        Holder<String> h2 = Holder.of(null);
        assertEquals(h1, h2);
    }

    @Test
    public void test_equals_DifferentValues() {
        Holder<String> h1 = Holder.of("test1");
        Holder<String> h2 = Holder.of("test2");
        assertNotEquals(h1, h2);
    }

    @Test
    public void test_equals_OneNull() {
        Holder<String> h1 = Holder.of("test");
        Holder<String> h2 = Holder.of(null);
        assertNotEquals(h1, h2);
    }

    @Test
    public void test_equals_DifferentType() {
        Holder<String> holder = Holder.of("test");
        assertNotEquals(holder, "test");
    }

    @Test
    public void test_equals_Null() {
        Holder<String> holder = Holder.of("test");
        assertNotEquals(holder, null);
    }

    @Test
    public void test_toString_WithValue() {
        Holder<String> holder = Holder.of("test");
        assertEquals("Holder[test]", holder.toString());
    }

    @Test
    public void test_toString_WithNull() {
        Holder<String> holder = Holder.of(null);
        assertEquals("Holder[null]", holder.toString());
    }

    @Test
    public void test_toString_WithInteger() {
        Holder<Integer> holder = Holder.of(42);
        assertEquals("Holder[42]", holder.toString());
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
}
