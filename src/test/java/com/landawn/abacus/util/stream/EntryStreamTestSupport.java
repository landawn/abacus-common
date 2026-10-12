package com.landawn.abacus.util.stream;

import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.TestBase;

/** Shared fixtures and lifecycle for the EntryStream contract tests. */
public abstract class EntryStreamTestSupport extends TestBase {

    protected Map<String, Integer> testMap;

    protected Map<String, List<Integer>> multiValueMap;

    protected Map<Integer, String> numberMap;

    protected Map<String, Integer> emptyMap;

    @BeforeEach
    public void setUp() {
        testMap = new LinkedHashMap<>();
        testMap.put("one", 1);
        testMap.put("two", 2);
        testMap.put("three", 3);
        testMap.put("four", 4);
        testMap.put("five", 5);

        multiValueMap = new LinkedHashMap<>();
        multiValueMap.put("a", Arrays.asList(1, 2, 3));
        multiValueMap.put("b", Arrays.asList(4, 5));
        multiValueMap.put("c", Arrays.asList(6, 7, 8, 9));

        numberMap = new LinkedHashMap<>();
        numberMap.put(1, "one");
        numberMap.put(2, "two");
        numberMap.put(3, "three");

        emptyMap = new LinkedHashMap<>();
    }

    // ---- perf review 2026-09-26 G095 begin ----
    protected static <K, V> Map.Entry<K, V> g095Entry(final K key, final V value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }

    protected static final class G095RecordingEntry extends AbstractMap.SimpleImmutableEntry<String, Integer> {
        private static final long serialVersionUID = 1L;
        private final transient List<String> log;

        G095RecordingEntry(final String key, final Integer value, final List<String> log) {
            super(key, value);
            this.log = log;
        }

        @Override
        public String getKey() {
            log.add("getKey:" + super.getKey());
            return super.getKey();
        }

        @Override
        public Integer getValue() {
            log.add("getValue:" + super.getValue());
            return super.getValue();
        }
    }

    protected static <T> Collection<T> g095RecordingCollection(final List<T> list, final List<String> log) {
        return new java.util.AbstractCollection<>() {
            @Override
            public int size() {
                return list.size();
            }

            @Override
            public Iterator<T> iterator() {
                log.add("iterator");
                final Iterator<T> iter = list.iterator();

                return new Iterator<>() {
                    @Override
                    public boolean hasNext() {
                        return iter.hasNext();
                    }

                    @Override
                    public T next() {
                        final T next = iter.next();
                        log.add("next:" + next);
                        return next;
                    }
                };
            }
        };
    }
}
