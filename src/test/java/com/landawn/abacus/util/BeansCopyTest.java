package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.junit.jupiter.api.Test;

public class BeansCopyTest extends BeansTestSupport {

    @Test
    public void testCopy() {
        SimpleBean copied = Beans.copy(simpleBean);
        assertNotNull(copied);
        assertNotSame(simpleBean, copied);
        assertEquals("John", copied.getName());
        assertEquals(25, copied.getAge());
        assertEquals(true, copied.getActive());

        assertNull(Beans.copy((SimpleBean) null));
    }

    @Test
    public void testCopy_SelectPropNames() {
        SimpleBean copied = Beans.copy(simpleBean, Arrays.asList("name"));
        assertEquals("John", copied.getName());
        assertEquals(0, copied.getAge());
        assertNull(copied.getActive());

        copied = Beans.copy(simpleBean, (Collection<String>) null);
        assertEquals("John", copied.getName());
        assertEquals(25, copied.getAge());
        assertEquals(true, copied.getActive());

        copied = Beans.copy(simpleBean, Collections.emptyList());
        assertNull(copied.getName());
        assertEquals(0, copied.getAge());
        assertNull(copied.getActive());

        assertThrows(IllegalArgumentException.class, () -> Beans.copy(simpleBean, Arrays.asList("nonExistentProp")));
    }

    @Test
    public void testCopy_PropFilter() {
        SimpleBean copied = Beans.copy(simpleBean, (name, value) -> value instanceof String);
        assertEquals("John", copied.getName());
        assertEquals(0, copied.getAge());
        assertNull(copied.getActive());

        copied = Beans.copy(simpleBean, (name, value) -> !name.equals("age"));
        assertEquals("John", copied.getName());
        assertEquals(0, copied.getAge());
    }

    @Test
    public void testCopy_BuilderBean() {
        BeansBuilderFixture built = BeansBuilderFixture.builder().setA("built").setB(3).build();
        assertEquals("built", Beans.copy(built).getA());
        assertEquals(3, Beans.copy(built).getB());
    }

    @Test
    public void testCopy_NonPropertyFieldShortcut() {
        BeansNonPropFixture src = new BeansNonPropFixture();
        src.setName("n");
        src.pokeSecret("SECRET");

        assertFalse(Beans.getPropNameList(BeansNonPropFixture.class).contains("secret"));

        BeansNonPropFixture selected = Beans.copy(src, CommonUtil.asList("name"));
        assertEquals("n", selected.getName());
        assertEquals("init", selected.peekSecret());

        assertEquals("n", Beans.copy(src).getName());
        assertEquals("n", Beans.copy(src, (Collection<String>) null).getName());
        assertEquals("n", Beans.copy(src, BiPredicates.alwaysTrue()).getName());

        if (Utils.kryoParser != null) {
            assertEquals("SECRET", Beans.copy(src).peekSecret());
            assertEquals("SECRET", Beans.copy(src, (Collection<String>) null).peekSecret());
            assertEquals("SECRET", Beans.copy(src, BiPredicates.alwaysTrue()).peekSecret());
        }
    }

    @Test
    public void testCopyAs() {
        SimpleBean source = new SimpleBean("Transfer", 50);
        source.setActive(true);

        SimpleBean target = Beans.copyAs(source, SimpleBean.class);
        assertEquals("Transfer", target.getName());
        assertEquals(50, target.getAge());

        EntityBean entity = new EntityBean();
        entity.setId(100L);
        assertEquals(100L, Beans.copyAs(entity, EntityBean.class).getId());

        SimpleBean fromNull = Beans.copyAs(null, SimpleBean.class);
        assertNotNull(fromNull);
        assertNull(fromNull.getName());

        assertThrows(IllegalArgumentException.class, () -> Beans.copyAs(simpleBean, (Class<SimpleBean>) null));
    }

    @Test
    public void testCopyAs_SelectPropNames() {
        SimpleBean copied = Beans.copyAs(simpleBean, Arrays.asList("name"), SimpleBean.class);
        assertEquals("John", copied.getName());
        assertEquals(0, copied.getAge());

        copied = Beans.copyAs(simpleBean, Arrays.asList("age"), SimpleBean.class);
        assertNull(copied.getName());
        assertEquals(25, copied.getAge());

        copied = Beans.copyAs(simpleBean, Collections.emptyList(), SimpleBean.class);
        assertNull(copied.getName());
        assertEquals(0, copied.getAge());

        assertThrows(IllegalArgumentException.class, () -> Beans.copyAs(simpleBean, Arrays.asList("name"), Address.class));
        assertThrows(IllegalArgumentException.class, () -> Beans.copyAs(simpleBean, Arrays.asList("nonExistentProp"), SimpleBean.class));
    }

    @Test
    public void testCopyAs_SelectPropNamesAndConverter() {
        SimpleBean copied = Beans.copyAs(simpleBean, Arrays.asList("name", "age"), name -> name, SimpleBean.class);
        assertEquals("John", copied.getName());
        assertEquals(25, copied.getAge());

        BeanWithSnakeCase snakeBean = new BeanWithSnakeCase();
        snakeBean.setFirstName("John");
        SimpleBean renamed = Beans.copyAs(snakeBean, Arrays.asList("firstName"), name -> "firstName".equals(name) ? "name" : name, SimpleBean.class);
        assertEquals("John", renamed.getName());

        copied = Beans.copyAs(simpleBean, (Collection<String>) null, name -> name, SimpleBean.class);
        assertEquals("John", copied.getName());

        assertThrows(IllegalArgumentException.class, () -> Beans.copyAs(simpleBean, Arrays.asList("name"), null, SimpleBean.class));
    }

    @Test
    public void testCopyAs_PropFilter() {
        SimpleBean copied = Beans.copyAs(simpleBean, (name, value) -> true, SimpleBean.class);
        assertEquals("John", copied.getName());
        assertEquals(25, copied.getAge());

        copied = Beans.copyAs(simpleBean, (name, value) -> value instanceof String, SimpleBean.class);
        assertEquals("John", copied.getName());
        assertEquals(0, copied.getAge());

        copied = Beans.copyAs(simpleBean, (BiPredicate<String, Object>) (name, val) -> !name.equals("age"), SimpleBean.class);
        assertEquals("John", copied.getName());
        assertEquals(0, copied.getAge());

        copied = Beans.copyAs(null, (BiPredicate<String, Object>) (name, val) -> true, SimpleBean.class);
        assertNotNull(copied);
        assertNull(copied.getName());

        assertThrows(IllegalArgumentException.class,
                () -> Beans.copyAs(simpleBean, (BiPredicate<String, Object>) (propName, propValue) -> propName.equals("name"), Address.class));
    }

    @Test
    public void testCopyAs_PropFilterAndConverter() {
        SimpleBean copied = Beans.copyAs(simpleBean, (name, value) -> true, Function.identity(), SimpleBean.class);
        assertEquals("John", copied.getName());
        assertEquals(25, copied.getAge());

        copied = Beans.copyAs(simpleBean, (propName, val) -> !propName.equals("age"), Function.identity(), SimpleBean.class);
        assertEquals("John", copied.getName());
        assertEquals(0, copied.getAge());

        copied = Beans.copyAs(null, (name, val) -> true, Function.identity(), SimpleBean.class);
        assertNotNull(copied);
        assertNull(copied.getName());
    }

    @Test
    public void testCopyAs_IgnoreUnmatched() {
        SimpleBean source = new SimpleBean("Jack", 55);
        source.setActive(true);

        Set<String> ignored = new HashSet<>(Collections.singletonList("age"));
        SimpleBean copy = Beans.copyAs(source, true, ignored, SimpleBean.class);
        assertEquals("Jack", copy.getName());
        assertEquals(0, copy.getAge());
        assertEquals(true, copy.getActive());

        copy = Beans.copyAs(source, true, null, SimpleBean.class);
        assertEquals("Jack", copy.getName());
        assertEquals(55, copy.getAge());

        ignored = new HashSet<>();
        ignored.add("active");
        copy = Beans.copyAs(source, true, ignored, SimpleBean.class);
        assertEquals("Jack", copy.getName());
        assertNull(copy.getActive());

        EntityBean unmatched = Beans.copyAs(source, true, null, EntityBean.class);
        assertNotNull(unmatched);

        copy = Beans.copyAs(null, true, null, SimpleBean.class);
        assertNotNull(copy);
        assertNull(copy.getName());

        assertThrows(IllegalArgumentException.class, () -> Beans.copyAs(source, false, null, null));
    }

    @Test
    public void testCopyAs_IgnoreUnmatchedFalse_SkipsNullSourceValues() {
        SimpleBean source = new SimpleBean();
        Set<String> ignored = new HashSet<>(Collections.singletonList("age"));
        Address copy = Beans.copyAs(source, false, ignored, Address.class);
        assertNotNull(copy);
        assertNull(copy.getCity());

        source.setName("Pia");
        assertThrows(IllegalArgumentException.class, () -> Beans.copyAs(source, false, ignored, Address.class));
    }

    @Test
    public void testCopyAs_BuilderBean() {
        BeansMutableFixture src = new BeansMutableFixture();
        src.setA("x");
        src.setB(7);

        BeansBuilderFixture copied = Beans.copyAs(src, BeansBuilderFixture.class);
        assertEquals("x", copied.getA());
        assertEquals(7, copied.getB());

        assertEquals("x", Beans.copyAs(src, CommonUtil.asList("a"), BeansBuilderFixture.class).getA());

        src.setA(null);
        BeansBuilderFixture filtered = Beans.copyAs(src, (name, value) -> value != null, BeansBuilderFixture.class);
        assertNull(filtered.getA());
        assertEquals(7, filtered.getB());

        BeansBuilderFixture built = BeansBuilderFixture.builder().setA("built").setB(3).build();
        assertEquals("built", Beans.copyAs(built, BeansMutableFixture.class).getA());
    }

    @Test
    public void testCopyAs_PropNameConverterForSameType() {
        ConverterProbeBean src = new ConverterProbeBean();
        src.setName("John");
        src.setNickName("Johnny");

        Function<String, String> swap = p -> p.equals("name") ? "nickName" : (p.equals("nickName") ? "name" : p);
        ConverterProbeBean copy = Beans.copyAs(src, (Collection<String>) null, swap, ConverterProbeBean.class);

        assertEquals("Johnny", copy.getName());
        assertEquals("John", copy.getNickName());
    }
}
