package com.landawn.abacus.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.AccessFieldByMethod;
import com.landawn.abacus.annotation.Id;

@Tag("unit")
class DatasetCollectionAssemblyReview20261003Test extends TestBase {
    private static Dataset rows() {
        return Dataset.rows(List.of("id", "children.id"), new Object[][] { { 1, 11 }, { 1, 12 } });
    }

    private static List<Integer> ids(final List<Child> values) {
        return values.stream().map(Child::getId).toList();
    }

    @ParameterizedTest
    @ValueSource(strings = { "class", "range", "selection", "supplier", "prefix" })
    void nonmergedCopyingSetterReceivesItsCompletedRow(final String path) {
        final Dataset data = rows();
        final List<CopyingParent> parents = switch (path) {
            case "range" -> data.toList(1, 2, CopyingParent.class);
            case "selection" -> data.toList(List.of("children.id", "id"), CopyingParent.class);
            case "supplier" -> data.toList(ignored -> new CopyingParent());
            case "prefix" -> {
                data.renameColumn("children.id", "c.id");
                yield data.toEntities(Map.of("c", "children"), CopyingParent.class);
            }
            default -> data.toList(CopyingParent.class);
        };
        assertEquals(List.of(path.equals("range") ? 12 : 11), ids(parents.get(0).getChildren()));
        assertEquals(1, parents.get(0).assignments);
        if (!path.equals("range")) {
            assertEquals(List.of(12), ids(parents.get(1).getChildren()));
            assertNotSame(parents.get(0).getChildren(), parents.get(1).getChildren());
        }
    }

    @Test
    void getRowAndLazyStreamAlreadySupplyCompletedCollections() {
        final Dataset data = rows();
        assertEquals(List.of(11), ids(data.getRow(0, CopyingParent.class).getChildren()));
        assertEquals(List.of(12), ids(data.getRow(1, ignored -> new CopyingParent()).getChildren()));
        assertEquals(List.of(11), ids(data.stream(CopyingParent.class).toList().get(0).getChildren()));
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void mergedFreshCollectionIsAssignedOnceAfterAllChildren(final boolean immutableCopy) {
        final Dataset data = Dataset.rows(List.of("id", "children.id"),
                new Object[][] { { 1, 11 }, { 1, 11 }, { 2, 11 }, { 1, 12 }, { 3, null }, { null, 99 } });
        final List<? extends CopyingParent> parents = immutableCopy ? data.toMergedEntities("id", ImmutableCopyParent.class)
                : data.toMergedEntities("id", CopyingParent.class);
        assertEquals(3, parents.size());
        assertEquals(List.of(11, 12), ids(parents.get(0).getChildren()));
        assertEquals(List.of(11), ids(parents.get(1).getChildren()));
        assertNotSame(parents.get(0).getChildren().get(0), parents.get(1).getChildren().get(0));
        assertEquals(1, parents.get(0).assignments);
        assertEquals(1, parents.get(1).assignments);
        assertNull(parents.get(2).getChildren());
        assertEquals(0, parents.get(2).assignments);
    }

    @Test
    void nonmergedImmutableCopyContainsTheChild() {
        assertEquals(List.of(11), ids(rows().toList(ImmutableCopyParent.class).get(0).getChildren()));
    }

    @Test
    void ordinaryConversionReplacesPreinitializedValues() {
        final SeededParent parent = rows().toList(SeededParent.class).get(0);
        assertEquals(List.of(11), ids(parent.getChildren()));
        assertNotSame(parent.initial, parent.getChildren());
        assertEquals(2, parent.assignments);
    }

    @Test
    void mergingPreservesExistingCollectionIdentityAndDefaultEntries() {
        final SeededParent parent = rows().toMergedEntities("id", SeededParent.class).get(0);
        assertEquals(List.of(-1, -1, 11, 12), ids(parent.getChildren()));
        assertSame(parent.initial, parent.getChildren());
        assertSame(parent.getChildren().get(0), parent.getChildren().get(1));
        assertEquals(1, parent.assignments);
    }

    @Test
    void existingReadOnlyPropertyRemainsAppendableWhenMerging() {
        Beans.registerXmlBindingClass(ReadOnlyParent.class);
        final ReadOnlyParent parent = rows().toMergedEntities(ReadOnlyParent.class).get(0);
        assertEquals(List.of(-1, 11, 12), ids(parent.getChildren()));
        assertSame(parent.initial, parent.getChildren());
    }

    @Test
    void copyingBuilderReceivesCompleteNonmergedCollection() {
        final List<CopyingBuilderParent> parents = rows().toList(CopyingBuilderParent.class);
        assertEquals(List.of(11), ids(parents.get(0).getChildren()));
        assertEquals(List.of(12), ids(parents.get(1).getChildren()));
    }

    @Test
    void mergingCopyingBuilderAndRecordControls() {
        final Dataset data = rows();
        assertEquals(List.of(11, 12), ids(data.toMergedEntities(CopyingBuilderParent.class).get(0).getChildren()));
        assertEquals(List.of(11, 12), ids(data.toMergedEntities(RecordParent.class).get(0).children()));
        assertEquals(List.of(11), ids(data.toList(RecordParent.class).get(0).children()));
    }

    @Test
    void collectionSetterFailurePropagates() {
        final IllegalStateException error = assertThrows(IllegalStateException.class, () -> rows().toList(RejectingParent.class));
        assertEquals("rejected children", error.getMessage());
    }

    public static class Child {
        @Id
        private Integer id;

        public Child() {
        }

        Child(final int id) {
            this.id = id;
        }

        public Integer getId() {
            return id;
        }

        public void setId(final Integer value) {
            id = value;
        }
    }

    public static class CopyingParent {
        @Id
        private Integer id;
        // Accessor-based on purpose: this fixture observes getter/setter calls, and properties are read and written
        // through their fields unless @AccessFieldByMethod says otherwise (in both the reflective and the ASM mode).
        @AccessFieldByMethod
        protected List<Child> children;
        int assignments;

        public Integer getId() {
            return id;
        }

        public void setId(final Integer value) {
            id = value;
        }

        public List<Child> getChildren() {
            return children;
        }

        public void setChildren(final List<Child> value) {
            assignments++;
            children = new ArrayList<>(value);
        }
    }

    public static class ImmutableCopyParent extends CopyingParent {
        @Override
        public void setChildren(final List<Child> value) {
            assignments++;
            children = List.copyOf(value);
        }
    }

    public static class SeededParent extends CopyingParent {
        final List<Child> initial;

        public SeededParent() {
            final Child seed = new Child(-1);
            setChildren(List.of(seed, seed));
            initial = getChildren();
        }
    }

    public static class ReadOnlyParent {
        @Id
        private Integer id;
        private final List<Child> children = new ArrayList<>(List.of(new Child(-1)));
        final List<Child> initial = children;

        public Integer getId() {
            return id;
        }

        public void setId(final Integer value) {
            id = value;
        }

        public List<Child> getChildren() {
            return children;
        }
    }

    public static class RejectingParent extends CopyingParent {
        @Override
        public void setChildren(final List<Child> value) {
            throw new IllegalStateException("rejected children");
        }
    }

    public record RecordParent(@Id Integer id, List<Child> children) {
    }

    public static final class CopyingBuilderParent {
        @Id
        private final Integer id;
        private final List<Child> children;

        private CopyingBuilderParent(final Integer id, final List<Child> children) {
            this.id = id;
            this.children = children;
        }

        public Integer getId() {
            return id;
        }

        public List<Child> getChildren() {
            return children;
        }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private Integer id;
            private List<Child> children;

            public Builder id(final Integer value) {
                id = value;
                return this;
            }

            public Builder children(final List<Child> value) {
                children = new ArrayList<>(value);
                return this;
            }

            public CopyingBuilderParent build() {
                return new CopyingBuilderParent(id, children);
            }
        }
    }
}
