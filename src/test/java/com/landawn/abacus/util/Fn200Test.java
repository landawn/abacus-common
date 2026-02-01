package com.landawn.abacus.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.function.BiPredicate;
import com.landawn.abacus.util.function.Predicate;
import com.landawn.abacus.util.function.Supplier;

@Tag("new-test")
public class Fn200Test extends TestBase {

    @Test
    public void memoize() {
        Supplier<Integer> supplier = Fn.memoize(() -> 1);
        assertThat(supplier.get()).isEqualTo(1);
    }

    @Test
    public void memoizeWithExpiration() throws InterruptedException {
        Supplier<Integer> supplier = Fn.memoizeWithExpiration(() -> 1, 1, TimeUnit.SECONDS);
        assertThat(supplier.get()).isEqualTo(1);
        Thread.sleep(1100);
        assertThat(supplier.get()).isEqualTo(1);
    }

    @Test
    public void memoizeFunction() {
        Function<Integer, Integer> function = Fn.memoize(i -> i + 1);
        assertThat(function.apply(1)).isEqualTo(2);
        assertThat(function.apply(1)).isEqualTo(2);
    }

    @Test
    public void close() {
        AutoCloseable closeable = mock(AutoCloseable.class);
        Fn.close(closeable).run();
        try {
            verify(closeable, times(1)).close();
        } catch (Exception e) {
        }
    }

    @Test
    public void closeAll() {
        AutoCloseable closeable1 = mock(AutoCloseable.class);
        AutoCloseable closeable2 = mock(AutoCloseable.class);
        Fn.closeAll(closeable1, closeable2).run();
        try {
            verify(closeable1, times(1)).close();
            verify(closeable2, times(1)).close();
        } catch (Exception e) {
        }
    }

    @Test
    public void closeAllQuietly() {
        AutoCloseable closeable1 = mock(AutoCloseable.class);
        AutoCloseable closeable2 = mock(AutoCloseable.class);
        Fn.closeAllQuietly(closeable1, closeable2).run();
        try {
            verify(closeable1, times(1)).close();
            verify(closeable2, times(1)).close();
        } catch (Exception e) {
        }
    }

    @Test
    public void emptyAction() {
        Fn.emptyAction().run();
    }

    @Test
    public void doNothing() {
        Fn.emptyConsumer().accept(1);
    }

    @Test
    public void throwRuntimeException() {
        assertThatThrownBy(() -> Fn.throwRuntimeException("error").accept(1)).isInstanceOf(RuntimeException.class).hasMessage("error");
    }

    @Test
    public void throwException() {
        assertThatThrownBy(() -> Fn.throwException(() -> new RuntimeException("error")).accept(1)).isInstanceOf(RuntimeException.class).hasMessage("error");
    }

    @Test
    public void sleep() {
        Fn.sleep(10).accept(1);
    }

    @Test
    public void sleepUninterruptibly() {
        Fn.sleepUninterruptibly(10).accept(1);
    }

    @Test
    public void println() {
        Fn.println().accept("hello");
    }

    @Test
    public void printlnWithSeparator() {
        Fn.println("=").accept("key", "value");
    }

    @Test
    public void toStr() {
        assertThat(Fn.toStr().apply(1)).isEqualTo("1");
    }

    @Test
    public void toCamelCase() {
        assertThat(Fn.toCamelCase().apply("hello_world")).isEqualTo("helloWorld");
    }

    @Test
    public void toLowerCase() {
        assertThat(Fn.toLowerCase().apply("HELLO")).isEqualTo("hello");
    }

    @Test
    public void toSnakeCase() {
        assertThat(Fn.toSnakeCase().apply("helloWorld")).isEqualTo("hello_world");
    }

    @Test
    public void toUpperCase() {
        assertThat(Fn.toUpperCase().apply("hello")).isEqualTo("HELLO");
    }

    @Test
    public void toScreamingSnakeCase() {
        assertThat(Fn.toScreamingSnakeCase().apply("helloWorld")).isEqualTo("HELLO_WORLD");
    }

    @Test
    public void toJson() {
        assertThat(Fn.toJson().apply(new int[] { 1, 2 })).isEqualTo("[1, 2]");
    }

    @Test
    public void toXml() {
        assertThat(Fn.toXml().apply(new int[] { 1, 2 })).isNotEmpty();
    }

    @Test
    public void identity() {
        assertThat(Fn.identity().apply(1)).isEqualTo(1);
    }

    @Test
    public void keyed() {
        assertThat(Fn.keyed(i -> i).apply(1).val()).isEqualTo(1);
    }

    @Test
    public void valOfKeyed() {
        assertThat(Fn.val().apply(Fn.keyed(i -> i).apply(1))).isEqualTo(1);
    }

    @Test
    public void wrap() {
        assertThat(Fn.wrap().apply(1).value()).isEqualTo(1);
    }

    @Test
    public void unwrap() {
        assertThat(Fn.unwrap().apply(Fn.wrap().apply(1))).isEqualTo(1);
    }

    @Test
    public void key() {
        assertThat(Fn.key().apply(Fn.entry().apply("a", 1))).isEqualTo("a");
    }

    @Test
    public void value() {
        assertThat(Fn.value().apply(Fn.entry().apply("a", 1))).isEqualTo(1);
    }

    @Test
    public void invert() {
        assertThat(Fn.invert().apply(Fn.entry().apply("a", 1)).getKey()).isEqualTo(1);
    }

    @Test
    public void entry() {
        assertThat(Fn.entry().apply("a", 1).getKey()).isEqualTo("a");
    }

    @Test
    public void entryWithKey() {
        assertThat(Fn.entryWithKey("a").apply(1).getKey()).isEqualTo("a");
    }

    @Test
    public void entryByKeyMapper() {
        assertThat(Fn.entryByKeyMapper(Object::toString).apply(1).getKey()).isEqualTo("1");
    }

    @Test
    public void entryWithValue() {
        assertThat(Fn.entryWithValue(1).apply("a").getValue()).isEqualTo(1);
    }

    @Test
    public void entryByValueMapper() {
        assertThat(Fn.<String, Integer> entryByValueMapper(Integer::parseInt).apply("1").getValue()).isEqualTo(1);
    }

    @Test
    public void pair() {
        assertThat(Fn.pair().apply("a", 1).left()).isEqualTo("a");
    }

    @Test
    public void triple() {
        assertThat(Fn.triple().apply("a", 1, true).left()).isEqualTo("a");
    }

    @Test
    public void tuple1() {
        assertThat(Fn.tuple1().apply(1)._1).isEqualTo(1);
    }

    @Test
    public void tuple2() {
        assertThat(Fn.tuple2().apply(1, "a")._1).isEqualTo(1);
    }

    @Test
    public void tuple3() {
        assertThat(Fn.tuple3().apply(1, "a", true)._1).isEqualTo(1);
    }

    @Test
    public void tuple4() {
        assertThat(Fn.tuple4().apply(1, "a", true, 2.0)._1).isEqualTo(1);
    }

    @Test
    public void trim() {
        assertThat(Fn.trim().apply(" a ")).isEqualTo("a");
    }

    @Test
    public void trimToEmpty() {
        assertThat(Fn.trimToEmpty().apply(" a ")).isEqualTo("a");
    }

    @Test
    public void trimToNull() {
        assertThat(Fn.trimToNull().apply(" ")).isNull();
    }

    @Test
    public void strip() {
        assertThat(Fn.strip().apply(" a ")).isEqualTo("a");
    }

    @Test
    public void stripToEmpty() {
        assertThat(Fn.stripToEmpty().apply(" a ")).isEqualTo("a");
    }

    @Test
    public void stripToNull() {
        assertThat(Fn.stripToNull().apply(" ")).isNull();
    }

    @Test
    public void nullToEmpty() {
        assertThat(Fn.nullToEmpty().apply(null)).isEmpty();
    }

    @Test
    public void nullToEmptyList() {
        assertThat(Fn.nullToEmptyList().apply(null)).isNotNull();
    }

    @Test
    public void nullToEmptySet() {
        assertThat(Fn.nullToEmptySet().apply(null)).isNotNull();
    }

    @Test
    public void nullToEmptyMap() {
        assertThat(Fn.nullToEmptyMap().apply(null)).isNotNull();
    }

    @Test
    public void len() {
        assertThat(Fn.len().apply(new Integer[] { 1, 2 })).isEqualTo(2);
    }

    @Test
    public void length() {
        assertThat(Fn.length().apply("abc")).isEqualTo(3);
    }

    @Test
    public void size() {
        assertThat(Fn.size().apply(java.util.Arrays.asList(1, 2))).isEqualTo(2);
    }

    @Test
    public void sizeM() {
        assertThat(Fn.sizeM().apply(java.util.Map.of("a", 1))).isEqualTo(1);
    }

    @Test
    public void cast() {
        assertThat(Fn.cast(String.class).apply("a")).isInstanceOf(String.class);
    }

    @Test
    public void alwaysTrue() {
        assertThat(Fn.alwaysTrue().test(1)).isTrue();
    }

    @Test
    public void alwaysFalse() {
        assertThat(Fn.alwaysFalse().test(1)).isFalse();
    }

    @Test
    public void isNull() {
        assertThat(Fn.isNull().test(null)).isTrue();
    }

    @Test
    public void isEmpty() {
        assertThat(Fn.isEmpty().test("")).isTrue();
    }

    @Test
    public void isBlank() {
        assertThat(Fn.isBlank().test(" ")).isTrue();
    }

    @Test
    public void notNull() {
        assertThat(Fn.notNull().test(1)).isTrue();
    }

    @Test
    public void notEmpty() {
        assertThat(Fn.notEmpty().test("a")).isTrue();
    }

    @Test
    public void notBlank() {
        assertThat(Fn.notBlank().test("a")).isTrue();
    }

    @Test
    public void isFile() {
        java.io.File file = mock(java.io.File.class);
        when(file.isFile()).thenReturn(true);
        assertThat(Fn.isFile().test(file)).isTrue();
    }

    @Test
    public void isDirectory() {
        java.io.File file = mock(java.io.File.class);
        when(file.isDirectory()).thenReturn(true);
        assertThat(Fn.isDirectory().test(file)).isTrue();
    }

    @Test
    public void equal() {
        assertThat(Fn.equal(1).test(1)).isTrue();
    }

    @Test
    public void notEqual() {
        assertThat(Fn.notEqual(1).test(2)).isTrue();
    }

    @Test
    public void greaterThan() {
        assertThat(Fn.greaterThan(1).test(2)).isTrue();
    }

    @Test
    public void greaterEqual() {
        assertThat(Fn.greaterEqual(1).test(1)).isTrue();
    }

    @Test
    public void lessThan() {
        assertThat(Fn.lessThan(2).test(1)).isTrue();
    }

    @Test
    public void lessEqual() {
        assertThat(Fn.lessEqual(1).test(1)).isTrue();
    }

    @Test
    public void between() {
        assertThat(Fn.between(1, 3).test(2)).isTrue();
    }

    @Test
    public void in() {
        assertThat(Fn.in(java.util.Arrays.asList(1, 2, 3)).test(2)).isTrue();
    }

    @Test
    public void notIn() {
        assertThat(Fn.notIn(java.util.Arrays.asList(1, 2, 3)).test(4)).isTrue();
    }

    @Test
    public void instanceOf() {
        assertThat(Fn.instanceOf(String.class).test("a")).isTrue();
    }

    @Test
    public void subtypeOf() {
        assertThat(Fn.subtypeOf(Object.class).test(String.class)).isTrue();
    }

    @Test
    public void startsWith() {
        assertThat(Fn.startsWith("a").test("abc")).isTrue();
    }

    @Test
    public void endsWith() {
        assertThat(Fn.endsWith("c").test("abc")).isTrue();
    }

    @Test
    public void contains() {
        assertThat(Fn.contains("b").test("abc")).isTrue();
    }

    @Test
    public void notStartsWith() {
        assertThat(Fn.notStartsWith("b").test("abc")).isTrue();
    }

    @Test
    public void notEndsWith() {
        assertThat(Fn.notEndsWith("b").test("abc")).isTrue();
    }

    @Test
    public void notContains() {
        assertThat(Fn.notContains("d").test("abc")).isTrue();
    }

    @Test
    public void matches() {
        assertThat(Fn.matches(java.util.regex.Pattern.compile("a.c")).test("abc")).isTrue();
    }

    @Test
    public void equalBiPredicate() {
        assertThat(Fn.equal().test(1, 1)).isTrue();
    }

    @Test
    public void notEqualBiPredicate() {
        assertThat(Fn.notEqual().test(1, 2)).isTrue();
    }

    @Test
    public void greaterThanBiPredicate() {
        assertThat(Fn.<Integer> greaterThan().test(2, 1)).isTrue();
    }

    @Test
    public void greaterEqualBiPredicate() {
        assertThat(Fn.<Integer> greaterEqual().test(1, 1)).isTrue();
    }

    @Test
    public void lessThanBiPredicate() {
        assertThat(Fn.<Integer> lessThan().test(1, 2)).isTrue();
    }

    @Test
    public void lessEqualBiPredicate() {
        assertThat(Fn.<Integer> lessEqual().test(1, 1)).isTrue();
    }

    @Test
    public void notPredicate() {
        Predicate<Integer> p = i -> i > 1;
        assertThat(Fn.not(p).test(1)).isTrue();
    }

    @Test
    public void notBiPredicate() {
        BiPredicate<Integer, Integer> p = (i, j) -> i > j;
        assertThat(Fn.not(p).test(1, 2)).isTrue();
    }
}
