package com.landawn.abacus.testfixture;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Fixture for {@code CommonUtil.getDescendingIteratorIfPossible}'s reflective path (U06-02, 2026-09-25): a public
 * {@code descendingIterator()} declared by a NON-public class in a package other than {@code com.landawn.abacus.util}.
 * {@code Method.invoke} on such a method fails with {@code IllegalAccessException} (the lookup never calls
 * {@code setAccessible}), which is the only production path that sets the negative-cache flag. Used by
 * {@code com.landawn.abacus.util.CommonUtilReview20260925Test}.
 */
public final class CommonUtilReviewFixtures {

    private CommonUtilReviewFixtures() {
    }

    /** A fresh package-private {@code Iterable} whose public {@code descendingIterator()} is reflectively inaccessible. */
    public static Iterable<String> hiddenDescending(final List<String> data) {
        return new CommonUtilReviewHiddenDescending(data);
    }

    /** The concrete (package-private) class of {@link #hiddenDescending(List)}, for cache inspection. */
    public static Class<?> hiddenDescendingClass() {
        return CommonUtilReviewHiddenDescending.class;
    }

    /** How many times {@code descendingIterator()} has actually run on any instance. */
    public static int descendingCalls() {
        return CommonUtilReviewHiddenDescending.DESCENDING_CALLS.get();
    }
}

/** Package-private on purpose: see {@link CommonUtilReviewFixtures}. */
final class CommonUtilReviewHiddenDescending implements Iterable<String> {
    static final AtomicInteger DESCENDING_CALLS = new AtomicInteger();

    private final List<String> data;

    CommonUtilReviewHiddenDescending(final List<String> data) {
        this.data = data;
    }

    @Override
    public Iterator<String> iterator() {
        return data.iterator();
    }

    @SuppressWarnings("unused")
    public Iterator<String> descendingIterator() {
        DESCENDING_CALLS.incrementAndGet();
        final List<String> copy = new ArrayList<>(data);
        Collections.reverse(copy);
        return copy.iterator();
    }
}
