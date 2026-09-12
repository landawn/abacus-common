package com.landawn.abacus.util;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.Builder.DatasetBuilder;

public abstract class BuilderTestSupport extends TestBase {

    protected Dataset dataset;
    protected DatasetBuilder builder;
    protected Dataset testDataset;
    protected Set<String> testSet;

    @BeforeEach
    public void setUp() {
        dataset = Dataset.rows(Arrays.asList("name", "age", "salary", "department"),
                Arrays.asList(Arrays.asList("John", 30, 50000.0, "IT"), Arrays.asList("Jane", 25, 45000.0, "HR"), Arrays.asList("Bob", 35, 60000.0, "Sales")));
        builder = Builder.of(dataset);

        testDataset = Dataset.rows(Arrays.asList("name", "age", "city"),
                Arrays.asList(Arrays.asList("John", 30, "New York"), Arrays.asList("Jane", 25, "Los Angeles"), Arrays.asList("Bob", 35, "Chicago")));
        testSet = new LinkedHashSet<>(Arrays.asList("x", "y"));
    }

    // --- Tests for Builder.of static factory methods (specialized detection) ---

    // --- Test for Builder accept/apply chaining ---

    // --- BooleanListBuilder set returns same builder ---

    // --- BooleanListBuilder add returns same builder ---

    // --- BooleanListBuilder addAtIndex returns same builder ---

    // --- BooleanListBuilder addAll returns same builder ---

    // --- BooleanListBuilder addAllAtIndex returns same builder ---

    // --- BooleanListBuilder remove returns same builder ---

    // --- BooleanListBuilder removeAll returns same builder ---

    // --- CharListBuilder returns same builder for all operations ---

    // --- ByteListBuilder returns same builder ---

    // --- ShortListBuilder returns same builder ---

    // --- IntListBuilder returns same builder ---

    // --- LongListBuilder returns same builder ---

    // --- FloatListBuilder returns same builder ---

    // --- DoubleListBuilder returns same builder ---

    // --- ListBuilder returns same builder ---

    // --- CollectionBuilder returns same builder ---

    // --- MapBuilder returns same builder ---

    // --- MultisetBuilder returns same builder ---

    // --- MultimapBuilder returns same builder ---

    // --- DatasetBuilder returns same builder for all methods ---

    //
    //
    //

    // --- Tests for DatasetBuilder combineColumns with class parameter ---

    // --- Tests for MapBuilder putIfAbsent with existing key ---

    // --- Tests for MultimapBuilder additional operations ---

    // --- Builder.of with LinkedHashMap ---

    // --- Builder.of with LinkedHashSet ---

    // --- Tests for ListBuilder.remove(int index) vs remove(Object) ---

    // --- Tests for CollectionBuilder removeAll(T... a) varargs ---

    // --- CollectionBuilder addAll/removeAll with null or empty ---

    // --- MapBuilder putAll with null ---

    // --- MapBuilder removeAll with null ---

    // --- MapBuilder removeAll with empty ---

    // --- Builder.of generic with unknown type ---

    // --- ListBuilder addAll varargs with null ---

    // --- ListBuilder removeAll varargs with null ---

    // --- Builder.of(Object) generic dispatch tests ---

    // --- CollectionBuilder addAll varargs with null ---

    // --- CollectionBuilder removeAll varargs with null ---

    // ========================================================================
    // Additional tests for untested methods and edge cases
    // ========================================================================

    // --- Builder.of(BooleanList) null argument ---

    // --- Builder.of(CharList) null argument ---

    // --- Builder.of(ByteList) null argument ---

    // --- Builder.of(ShortList) null argument ---

    // --- Builder.of(IntList) null argument ---

    // --- Builder.of(LongList) null argument ---

    // --- Builder.of(FloatList) null argument ---

    // --- Builder.of(DoubleList) null argument ---

    // --- Builder.of(Collection) null argument ---

    // --- Builder.of(Map) null argument ---

    // --- Builder.of(Multiset) null argument ---

    // --- Builder.of(Dataset) null argument ---

    // --- Builder of with SetMultimap null ---

    // --- Builder val returns same reference ---

    // --- Builder accept returns same builder ---

    // --- Builder accept multiple chained ---

    // --- Builder apply with null function result ---

    // --- Builder stream content ---

    // --- Tests for static compare factory methods with more primitive types ---

    // --- ComparisonBuilder instance compare with Comparable ---

    // --- ComparisonBuilder returns 0 when all equal ---

    // --- CompareFalseLess true, false ---

    // --- CompareTrueLess false, true ---

    // --- Compare char equal ---

    // --- Compare byte equal ---

    // --- Compare short equal ---

    // --- Compare int equal ---

    // --- Compare long equal ---

    // --- Compare float equal ---

    // --- Compare double equal ---

    // --- Compare float with tolerance not equal ---

    // --- Compare double with tolerance not equal ---

    // --- Tests for ComparisonBuilder instance methods ---

    // --- ComparisonBuilder compare rejects a null Comparator ---

    // --- CompareNullLess with non-null values ---

    // --- CompareNullLess right is null ---

    // --- CompareNullBigger with non-null values ---

    // --- CompareNullBigger right is null ---

    // --- Compare static methods returning positive ---

    // --- ComparisonBuilder instance compareNullLess when already decided ---

    // --- ComparisonBuilder instance compareNullBigger when already decided ---

    // --- ComparisonBuilder instance compareFalseLess when already decided ---

    // --- ComparisonBuilder instance compareTrueLess when already decided ---

    // --- Tests for EquivalenceBuilder instance methods ---

    // --- Tests for static equals factory methods with more primitive types ---

    // --- Equals float not equal ---

    // --- Equals double not equal ---

    // --- Equals char not equal ---

    // --- Equals byte not equal ---

    // --- Equals short not equal ---

    // --- Equals int not equal ---

    // --- Equals long not equal ---

    // --- Equals float with tolerance not equal ---

    // --- Equals double with tolerance not equal ---

    // --- EquivalenceBuilder instance equals skipped when already false ---

    // --- EquivalenceBuilder instance equals with predicate skipped when already false ---

    // --- EquivalenceBuilder equals with null/non-null ---

    // --- EquivalenceBuilder isEquals vs result ---

    // --- Tests for HashCodeBuilder instance methods ---

    // --- Tests for static hash factory methods with more types ---

    // --- HashCodeBuilder hash(T, func) ---

    // --- Hash boolean false ---

    // --- Hash boolean true ---

    // --- HashCodeBuilder instance hash chain correctness ---

    // --- HashCodeBuilder hash(Object) with non-null ---

    // -------- 2026-09-06 review fixes --------

    protected static Dataset reviewFixes20260906Dataset() {
        return Dataset.rows(Arrays.asList("name", "age"), new Object[][] { { "Alice", 25 }, { "Bob", 30 } });
    }
}
