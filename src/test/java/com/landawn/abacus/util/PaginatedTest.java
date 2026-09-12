package com.landawn.abacus.util;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.Optional;
import com.landawn.abacus.util.stream.Stream;

/**
 * Behavioural tests for the {@link Paginated} interface against a minimal in-memory
 * implementation. Verifies boundary conditions: empty, single-page, exact-page-fit,
 * and trailing partial page.
 */
public class PaginatedTest extends TestBase {

    /** Splits a flat list of items into fixed-size pages. */
    private static final class ListPaginated<E> implements Paginated<List<E>> {
        private final List<E> data;
        private final int pageSize;

        ListPaginated(final List<E> data, final int pageSize) {
            if (pageSize <= 0) {
                throw new IllegalArgumentException("pageSize must be positive");
            }
            this.data = data;
            this.pageSize = pageSize;
        }

        @Override
        public Optional<List<E>> firstPage() {
            return totalPages() == 0 ? Optional.empty() : Optional.of(getPage(0));
        }

        @Override
        public Optional<List<E>> lastPage() {
            final int n = totalPages();
            return n == 0 ? Optional.empty() : Optional.of(getPage(n - 1));
        }

        @Override
        public List<E> getPage(final int pageNum) {
            if (pageNum < 0 || pageNum >= totalPages()) {
                throw new IllegalArgumentException("pageNum out of range: " + pageNum);
            }
            final int from = pageNum * pageSize;
            final int to = Math.min(from + pageSize, data.size());
            return new ArrayList<>(data.subList(from, to));
        }

        @Override
        public int pageSize() {
            return pageSize;
        }

        @Override
        @Deprecated
        public int pageCount() {
            return totalPages();
        }

        @Override
        public int totalPages() {
            if (data.isEmpty()) {
                return 0;
            }
            return (data.size() + pageSize - 1) / pageSize;
        }

        @Override
        public Stream<List<E>> stream() {
            final List<List<E>> pages = new ArrayList<>(totalPages());
            for (int i = 0; i < totalPages(); i++) {
                pages.add(getPage(i));
            }
            return Stream.of(pages);
        }

        @Override
        public Iterator<List<E>> iterator() {
            return new Iterator<>() {
                private int idx = 0;

                @Override
                public boolean hasNext() {
                    return idx < totalPages();
                }

                @Override
                public List<E> next() {
                    if (!hasNext()) {
                        throw new NoSuchElementException();
                    }
                    return getPage(idx++);
                }
            };
        }
    }

    @Test
    public void testEmpty_NoPages() {
        Paginated<List<Integer>> p = new ListPaginated<>(new ArrayList<>(), 10);
        Assertions.assertEquals(0, p.totalPages());
        Assertions.assertEquals(0, p.pageCount());
        Assertions.assertTrue(p.firstPage().isEmpty());
        Assertions.assertTrue(p.lastPage().isEmpty());
        Assertions.assertFalse(p.iterator().hasNext());
        Assertions.assertEquals(0, p.stream().count());
        Assertions.assertThrows(IllegalArgumentException.class, () -> p.getPage(0));
    }

    @Test
    public void testSinglePage_Partial() {
        // 3 items, page size 10 -> exactly 1 page of size 3
        Paginated<List<Integer>> p = new ListPaginated<>(java.util.Arrays.asList(1, 2, 3), 10);
        Assertions.assertEquals(1, p.totalPages());
        Assertions.assertEquals(java.util.Arrays.asList(1, 2, 3), p.getPage(0));
        Assertions.assertEquals(java.util.Arrays.asList(1, 2, 3), p.firstPage().get());
        Assertions.assertEquals(java.util.Arrays.asList(1, 2, 3), p.lastPage().get());
        Assertions.assertSame(p.firstPage().get().getClass(), p.lastPage().get().getClass());
    }

    @Test
    public void testExactPageFit() {
        // 6 items, page size 3 -> exactly 2 full pages, no partial trailing page
        Paginated<List<Integer>> p = new ListPaginated<>(java.util.Arrays.asList(1, 2, 3, 4, 5, 6), 3);
        Assertions.assertEquals(2, p.totalPages());
        Assertions.assertEquals(java.util.Arrays.asList(1, 2, 3), p.getPage(0));
        Assertions.assertEquals(java.util.Arrays.asList(4, 5, 6), p.getPage(1));
        Assertions.assertEquals(3, p.lastPage().get().size());
        Assertions.assertThrows(IllegalArgumentException.class, () -> p.getPage(2));
        Assertions.assertThrows(IllegalArgumentException.class, () -> p.getPage(-1));
    }

    @Test
    public void testTrailingPartialPage() {
        // 7 items, page size 3 -> 3 pages of [3,3,1]
        Paginated<List<Integer>> p = new ListPaginated<>(java.util.Arrays.asList(1, 2, 3, 4, 5, 6, 7), 3);
        Assertions.assertEquals(3, p.totalPages());
        Assertions.assertEquals(3, p.getPage(0).size());
        Assertions.assertEquals(3, p.getPage(1).size());
        Assertions.assertEquals(1, p.getPage(2).size());
        Assertions.assertEquals(java.util.Arrays.asList(7), p.lastPage().get());
    }

    @Test
    public void testIterator_TraversesAllPagesAndStopsAtEnd() {
        Paginated<List<Integer>> p = new ListPaginated<>(java.util.Arrays.asList(1, 2, 3, 4, 5), 2);
        Iterator<List<Integer>> it = p.iterator();
        Assertions.assertTrue(it.hasNext());
        Assertions.assertEquals(java.util.Arrays.asList(1, 2), it.next());
        Assertions.assertEquals(java.util.Arrays.asList(3, 4), it.next());
        Assertions.assertEquals(java.util.Arrays.asList(5), it.next());
        Assertions.assertFalse(it.hasNext());
        Assertions.assertThrows(NoSuchElementException.class, it::next);
    }

    @Test
    public void testStream_FlattensAcrossPages() {
        Paginated<List<Integer>> p = new ListPaginated<>(java.util.Arrays.asList(1, 2, 3, 4, 5), 2);
        long sum = p.stream().flattMap(List::stream).mapToLong(Integer::longValue).sum();
        Assertions.assertEquals(15L, sum);
    }

    @Test
    public void testFirstPage_EmptyVsPresent() {
        Paginated<List<Integer>> empty = new ListPaginated<>(new ArrayList<>(), 5);
        Paginated<List<Integer>> nonEmpty = new ListPaginated<>(java.util.Arrays.asList(42), 5);
        Assertions.assertTrue(empty.firstPage().isEmpty());
        Assertions.assertTrue(nonEmpty.firstPage().isPresent());
        Assertions.assertEquals(42, nonEmpty.firstPage().get().get(0));
    }

    @Test
    public void testPageSize_Reported() {
        Paginated<List<Integer>> p = new ListPaginated<>(java.util.Arrays.asList(1, 2, 3), 7);
        Assertions.assertEquals(7, p.pageSize());
    }

    @Test
    public void testPageCount_DeprecatedDelegatesToTotalPages() {
        Paginated<List<Integer>> p = new ListPaginated<>(java.util.Arrays.asList(1, 2, 3, 4), 2);
        Assertions.assertEquals(p.totalPages(), p.pageCount());
    }
}
