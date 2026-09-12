package testfixtures;

import java.util.ArrayList;

public final class ConversionReviewFixtures {
    private ConversionReviewFixtures() {
    }

    public static final class Supplied {
        public final Object value;

        public Supplied(Object value) {
            this.value = value;
        }
    }

    public static final class Wrong {
    }

    public static final class SameList extends ArrayList<Object> {
    }

    public static final class AliasList<E> extends ArrayList<E> {
    }

    public static final class PrefilledList<E> extends ArrayList<E> {
    }

    public static final class ReusedList<E> extends ArrayList<E> {
    }
}
