package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BinaryOperator;

import org.junit.jupiter.api.Test;

public class BeansMergeTest extends BeansTestSupport {

    @Test
    public void testMergeInto() {
        SimpleBean source = new SimpleBean("Jane", 30);
        SimpleBean target = new SimpleBean("John", 25);
        Beans.mergeInto(source, target);
        assertEquals("Jane", target.getName());
        assertEquals(30, target.getAge());

        SimpleBean original = new SimpleBean("Bob", 40);
        Beans.mergeInto(null, original);
        assertEquals("Bob", original.getName());

        assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(source, null));
    }

    @Test
    public void testMergeInto_MergeFunc() {
        SimpleBean source = new SimpleBean("John", 10);
        SimpleBean target = new SimpleBean("Jane", 20);
        Beans.mergeInto(source, target, Fn.o((srcVal, tgtVal) -> {
            if (srcVal instanceof Integer && tgtVal instanceof Integer) {
                return ((Integer) srcVal) + ((Integer) tgtVal);
            }
            return srcVal;
        }));
        assertEquals("John", target.getName());
        assertEquals(30, target.getAge());

        Address unmatched = new Address();
        unmatched.setCity("NYC");
        Address result = Beans.mergeInto(simpleBean, unmatched, Fn.o((srcVal, tgtVal) -> srcVal));
        assertTrue(unmatched == result);
        assertEquals("NYC", unmatched.getCity());
    }

    @Test
    public void testMergeInto_ConverterAndMergeFunc() {
        SimpleBean source = new SimpleBean("Jane", 30);
        SimpleBean target = new SimpleBean("John", 25);
        Beans.mergeInto(source, target, name -> name, (srcVal, tgtVal) -> srcVal);
        assertEquals("Jane", target.getName());
        assertEquals(30, target.getAge());

        Address unmatched = new Address();
        unmatched.setCity("NYC");
        assertDoesNotThrow(() -> Beans.mergeInto(simpleBean, unmatched, name -> name, (srcVal, tgtVal) -> srcVal));
        assertEquals("NYC", unmatched.getCity());

        SimpleBean kept = new SimpleBean("John", 25);
        SimpleBean fromNull = Beans.mergeInto(null, kept, name -> name, (srcVal, tgtVal) -> srcVal);
        assertTrue(kept == fromNull);
        assertEquals("John", kept.getName());

        assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(simpleBean, (SimpleBean) null, name -> name, (srcVal, tgtVal) -> srcVal));
    }

    @Test
    public void testMergeInto_IgnoreUnmatched() {
        SimpleBean source = new SimpleBean("Jane", 30);
        EntityBean target = new EntityBean();
        target.setId(1L);
        target.setValue("original");
        Beans.mergeInto(source, target, true, null);
        assertEquals(1L, target.getId());
        assertEquals("original", target.getValue());

        Set<String> ignored = new HashSet<>();
        ignored.add("age");
        SimpleBean t2 = new SimpleBean("Target", 60);
        Beans.mergeInto(source, t2, true, ignored);
        assertEquals("Jane", t2.getName());
        assertEquals(60, t2.getAge());

        source.setAge(5);
        SimpleBean t3 = new SimpleBean("John", 10);
        Beans.mergeInto(source, t3, true, null, (srcVal, tgtVal) -> {
            if (srcVal instanceof Integer && tgtVal instanceof Integer) {
                return ((Integer) srcVal) + ((Integer) tgtVal);
            }
            return srcVal;
        });
        assertEquals(15, t3.getAge());

        Address throwsTarget = new Address();
        assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(simpleBean, throwsTarget, false, (Set<String>) null));
    }

    @Test
    public void testMergeInto_SelectPropNames() {
        SimpleBean source = new SimpleBean("New", 100);
        source.setActive(true);
        SimpleBean target = new SimpleBean("Old", 200);
        target.setActive(false);

        Beans.mergeInto(source, target, Arrays.asList("name"));
        assertEquals("New", target.getName());
        assertEquals(200, target.getAge());
        assertEquals(false, target.getActive());

        target = new SimpleBean("Old", 200);
        target.setActive(false);
        Beans.mergeInto(source, target, (Collection<String>) null);
        assertEquals("New", target.getName());
        assertEquals(100, target.getAge());
        assertEquals(true, target.getActive());

        target = new SimpleBean("Old", 200);
        target.setActive(false);
        Beans.mergeInto(source, target, Collections.emptyList());
        assertEquals("Old", target.getName());
        assertEquals(200, target.getAge());

        assertThrows(IllegalArgumentException.class, () -> Beans.mergeInto(simpleBean, new SimpleBean(), Arrays.asList("nonExistentProp")));
    }

    @Test
    public void testMergeInto_SelectPropNamesAndMergeFunc() {
        SimpleBean source = new SimpleBean("Alice", 5);
        SimpleBean target = new SimpleBean("Bob", 10);
        Beans.mergeInto(source, target, Arrays.asList("age"), (srcVal, tgtVal) -> {
            if (srcVal instanceof Integer && tgtVal instanceof Integer) {
                return ((Integer) srcVal) + ((Integer) tgtVal);
            }
            return srcVal;
        });
        assertEquals("Bob", target.getName());
        assertEquals(15, target.getAge());

        Beans.mergeInto(source, target, Collections.emptyList(), (srcVal, tgtVal) -> srcVal);
        assertEquals("Bob", target.getName());
    }

    @Test
    public void testMergeInto_SelectPropNamesConverterAndMergeFunc() {
        SimpleBean source = new SimpleBean("Jane", 30);
        SimpleBean target = new SimpleBean("John", 25);
        Beans.mergeInto(source, target, Arrays.asList("name"), name -> name, (a, b) -> a);
        assertEquals("Jane", target.getName());
        assertEquals(25, target.getAge());

        source = new SimpleBean("SrcName", 15);
        target = new SimpleBean("TgtName", 25);
        Beans.mergeInto(source, target, Arrays.asList("age"), name -> name, (srcVal, tgtVal) -> {
            if (srcVal instanceof Integer && tgtVal instanceof Integer) {
                return Math.max((Integer) srcVal, (Integer) tgtVal);
            }
            return srcVal;
        });
        assertEquals("TgtName", target.getName());
        assertEquals(25, target.getAge());
    }

    @Test
    public void testMergeIntoIf() {
        SimpleBean source = new SimpleBean("Jane", 30);
        source.setActive(true);
        SimpleBean target = new SimpleBean("John", 25);
        target.setActive(false);

        Beans.mergeIntoIf(source, target, Fn.p((name, value) -> value instanceof String));
        assertEquals("Jane", target.getName());
        assertEquals(25, target.getAge());
        assertEquals(false, target.getActive());

        source = new SimpleBean("FilteredMerge", 20);
        source.setActive(true);
        target = new SimpleBean("BaseTarget", 40);
        target.setActive(false);
        Beans.mergeIntoIf(source, target, Fn.p((name, value) -> value instanceof Integer));
        assertEquals("BaseTarget", target.getName());
        assertEquals(20, target.getAge());
        assertEquals(false, target.getActive());
    }

    @Test
    public void testMergeIntoIf_Converter() {
        SimpleBean source = new SimpleBean("PropFilter", 25);
        SimpleBean target = new SimpleBean("TargetBean", 75);
        Beans.mergeIntoIf(source, target, (name, value) -> true, name -> name);
        assertEquals("PropFilter", target.getName());
        assertEquals(25, target.getAge());

        BeanWithSnakeCase snakeSource = new BeanWithSnakeCase();
        snakeSource.setFirstName("NewFirst");
        SimpleBean convertTarget = new SimpleBean();
        Beans.mergeIntoIf(snakeSource, convertTarget, (name, value) -> true, name -> "name");
        assertEquals("NewFirst", convertTarget.getName());

        source = new SimpleBean("FromSrc", 50);
        source.setActive(true);
        target = new SimpleBean("OrigTarget", 100);
        target.setActive(false);
        Beans.mergeIntoIf(source, target, (name, value) -> name.equals("name"), name -> name);
        assertEquals("FromSrc", target.getName());
        assertEquals(100, target.getAge());
        assertEquals(false, target.getActive());
    }

    @Test
    public void testMergeIntoIf_ConverterAndMergeFunc() {
        SimpleBean source = new SimpleBean("Alice", 10);
        SimpleBean target = new SimpleBean("Bob", 20);
        Beans.mergeIntoIf(source, target, (name, value) -> value instanceof Integer, name -> name, (srcVal, tgtVal) -> ((Integer) srcVal) + ((Integer) tgtVal));
        assertEquals("Bob", target.getName());
        assertEquals(30, target.getAge());

        source = new SimpleBean("Src", 10);
        source.setActive(true);
        target = new SimpleBean("Tgt", 20);
        target.setActive(false);
        Beans.mergeIntoIf(source, target, (name, value) -> name.equals("age"), name -> name, (srcVal, tgtVal) -> ((Integer) srcVal) + ((Integer) tgtVal));
        assertEquals("Tgt", target.getName());
        assertEquals(30, target.getAge());
        assertEquals(false, target.getActive());

        assertThrows(IllegalArgumentException.class,
                () -> Beans.mergeIntoIf(simpleBean, new SimpleBean(), (propName, propValue) -> true, propName -> "__bad__", (srcVal, tgtVal) -> srcVal));
    }

    @Test
    public void testMergeInto_BuilderAndNullPreserving() {
        BeansMutableFixture src = new BeansMutableFixture();
        src.setA("x");
        src.setB(7);

        BeansMutableFixture target = new BeansMutableFixture();
        target.setA("target");
        Beans.mergeInto(src, target, CommonUtil.asList("a"), (BinaryOperator<Object>) (s, t) -> String.valueOf(s) + "+" + String.valueOf(t));
        assertEquals("x+target", target.getA());
        assertEquals(0, target.getB());

        BeansMutableFixture t2 = new BeansMutableFixture();
        t2.setA("keep");
        Beans.mergeInto(new BeansMutableFixture(), t2);
        assertEquals("keep", t2.getA());
    }
}
