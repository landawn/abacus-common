package com.landawn.abacus.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;

@Tag("unit")
class CollectionPropertyPathRegressionTest extends TestBase {
    @Test
    void readsFirstCollectionElementThroughNestedPaths() {
        final Family family = new Family();
        family.setChildren(new ArrayList<>(List.of(new Child("first", 7), new Child("second", 9))));
        family.setMembers(new LinkedHashSet<>(family.getChildren()));
        family.setWaiting(new ArrayDeque<>(family.getChildren()));
        final ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(Family.class);

        assertEquals("first", info.getPropValue(family, "children.name"));
        assertEquals(Integer.valueOf(7), info.getPropValue(family, "members.age"));
        assertEquals("first", info.getPropValue(family, "waiting.name"));
        info.setPropValue(family, "children.name", "updated");
        assertEquals("updated", info.getPropValue(family, "children.name"));
        assertSame(family.getChildren(), info.getPropValue(family, "children"));
    }

    @Test
    void emptyOrNullCollectionPathsReturnFinalPropertyDefault() {
        final Family family = new Family();
        final ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(Family.class);

        assertNull(info.getPropValue(family, "children.name"));
        assertEquals(Integer.valueOf(0), info.getPropValue(family, "children.age"));
        family.setChildren(new ArrayList<>());
        assertNull(info.getPropValue(family, "children.name"));
        assertEquals(Integer.valueOf(0), info.getPropValue(family, "children.age"));
        family.getChildren().add(null);
        assertNull(info.getPropValue(family, "children.name"));
        assertEquals(Integer.valueOf(0), info.getPropValue(family, "children.age"));
    }

    @Test
    void terminalNestedCollectionIsReturnedWithoutUnwrapping() {
        final Family family = new Family();
        final Child child = new Child("parent", 4);
        child.setFriends(new ArrayList<>(List.of(new Child("friend", 5))));
        family.setChildren(new ArrayList<>(List.of(child)));
        final ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(Family.class);

        assertSame(child.getFriends(), info.getPropValue(family, "children.friends"));
        assertEquals("friend", info.getPropValue(family, "children.friends.name"));
    }

    public static class Family {
        private List<Child> children;
        private Set<Child> members;
        private Queue<Child> waiting;

        public List<Child> getChildren() {
            return children;
        }

        public void setChildren(final List<Child> children) {
            this.children = children;
        }

        public Set<Child> getMembers() {
            return members;
        }

        public void setMembers(final Set<Child> members) {
            this.members = members;
        }

        public Queue<Child> getWaiting() {
            return waiting;
        }

        public void setWaiting(final Queue<Child> waiting) {
            this.waiting = waiting;
        }
    }

    public static class Child {
        private String name;
        private int age;
        private Collection<Child> friends;

        public Child() {
        }

        Child(final String name, final int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public void setName(final String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(final int age) {
            this.age = age;
        }

        public Collection<Child> getFriends() {
            return friends;
        }

        public void setFriends(final Collection<Child> friends) {
            this.friends = friends;
        }
    }

    // ---- bug review 2026-10-02 final G127 begin ----
    // G127: a write through a Set-typed intermediate goes to its first element and reads back the same way; the public
    // Beans.getPropValue keeps rejecting a path through a collection (its restriction is unchanged by the BeanInfo fix).
    @Test
    void setThroughSetIntermediateReadsBackAndBeansStillRejectsCollectionPath() {
        final Family family = new Family();
        final Child first = new Child("first", 7);
        family.setMembers(new LinkedHashSet<>(List.of(first, new Child("second", 9))));
        family.setChildren(new ArrayList<>(List.of(first)));
        final ParserUtil.BeanInfo info = ParserUtil.getBeanInfo(Family.class);

        info.setPropValue(family, "members.age", 11);
        assertEquals(11, first.getAge());
        assertEquals(Integer.valueOf(11), info.getPropValue(family, "members.age"));

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
                () -> com.landawn.abacus.util.Beans.getPropValue(family, "children.name"));
    }
    // ---- bug review 2026-10-02 final G127 end ----
}
