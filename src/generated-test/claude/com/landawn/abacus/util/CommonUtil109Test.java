package com.landawn.abacus.util;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Deque;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.util.u.OptionalInt;

public class CommonUtil109Test extends TestBase {

    // Tests for binarySearch methods
    
    @Test
    public void testBinarySearch_shortArray() {
        short[] arr = {1, 3, 5, 7, 9};
        Assertions.assertEquals(2, N.binarySearch(arr, (short) 5));
        Assertions.assertEquals(-1, N.binarySearch(arr, (short) 0));
        Assertions.assertEquals(-6, N.binarySearch(arr, (short) 10));
        
        // Test empty array
        short[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, (short) 5));
        
        // Test null array
        Assertions.assertEquals(-1, N.binarySearch((short[]) null, (short) 5));
    }
    
    @Test
    public void testBinarySearch_shortArray_withRange() {
        short[] arr = {1, 3, 5, 7, 9, 11, 13};
        Assertions.assertEquals(3, N.binarySearch(arr, 1, 5, (short) 7));
        Assertions.assertEquals(-2, N.binarySearch(arr, 1, 5, (short) 2));
        
        // Test with invalid range
        Assertions.assertThrows(IndexOutOfBoundsException.class, 
                () -> N.binarySearch(arr, 5, 2, (short) 5));
        
        // Test empty array
        short[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, (short) 5));
    }
    
    @Test
    public void testBinarySearch_intArray() {
        int[] arr = {1, 3, 5, 7, 9};
        Assertions.assertEquals(2, N.binarySearch(arr, 5));
        Assertions.assertEquals(-1, N.binarySearch(arr, 0));
        Assertions.assertEquals(-6, N.binarySearch(arr, 10));
        
        // Test empty array
        int[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 5));
        
        // Test null array
        Assertions.assertEquals(-1, N.binarySearch((int[]) null, 5));
    }
    
    @Test
    public void testBinarySearch_intArray_withRange() {
        int[] arr = {1, 3, 5, 7, 9, 11, 13};
        Assertions.assertEquals(3, N.binarySearch(arr, 1, 5, 7));
        Assertions.assertEquals(-2, N.binarySearch(arr, 1, 5, 2));
        
        // Test empty array
        int[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, 5));
    }
    
    @Test
    public void testBinarySearch_longArray() {
        long[] arr = {1L, 3L, 5L, 7L, 9L};
        Assertions.assertEquals(2, N.binarySearch(arr, 5L));
        Assertions.assertEquals(-1, N.binarySearch(arr, 0L));
        Assertions.assertEquals(-6, N.binarySearch(arr, 10L));
        
        // Test empty array
        long[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 5L));
        
        // Test null array
        Assertions.assertEquals(-1, N.binarySearch((long[]) null, 5L));
    }
    
    @Test
    public void testBinarySearch_longArray_withRange() {
        long[] arr = {1L, 3L, 5L, 7L, 9L, 11L, 13L};
        Assertions.assertEquals(3, N.binarySearch(arr, 1, 5, 7L));
        Assertions.assertEquals(-2, N.binarySearch(arr, 1, 5, 2L));
        
        // Test empty array
        long[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, 5L));
    }
    
    @Test
    public void testBinarySearch_floatArray() {
        float[] arr = {1.0f, 3.0f, 5.0f, 7.0f, 9.0f};
        Assertions.assertEquals(2, N.binarySearch(arr, 5.0f));
        Assertions.assertEquals(-1, N.binarySearch(arr, 0.0f));
        Assertions.assertEquals(-6, N.binarySearch(arr, 10.0f));
        
        // Test empty array
        float[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 5.0f));
        
        // Test null array
        Assertions.assertEquals(-1, N.binarySearch((float[]) null, 5.0f));
    }
    
    @Test
    public void testBinarySearch_floatArray_withRange() {
        float[] arr = {1.0f, 3.0f, 5.0f, 7.0f, 9.0f, 11.0f, 13.0f};
        Assertions.assertEquals(3, N.binarySearch(arr, 1, 5, 7.0f));
        Assertions.assertEquals(-2, N.binarySearch(arr, 1, 5, 2.0f));
        
        // Test empty array
        float[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, 5.0f));
    }
    
    @Test
    public void testBinarySearch_doubleArray() {
        double[] arr = {1.0, 3.0, 5.0, 7.0, 9.0};
        Assertions.assertEquals(2, N.binarySearch(arr, 5.0));
        Assertions.assertEquals(-1, N.binarySearch(arr, 0.0));
        Assertions.assertEquals(-6, N.binarySearch(arr, 10.0));
        
        // Test empty array
        double[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 5.0));
        
        // Test null array
        Assertions.assertEquals(-1, N.binarySearch((double[]) null, 5.0));
    }
    
    @Test
    public void testBinarySearch_doubleArray_withRange() {
        double[] arr = {1.0, 3.0, 5.0, 7.0, 9.0, 11.0, 13.0};
        Assertions.assertEquals(3, N.binarySearch(arr, 1, 5, 7.0));
        Assertions.assertEquals(-2, N.binarySearch(arr, 1, 5, 2.0));
        
        // Test empty array
        double[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, 5.0));
    }
    
    @Test
    public void testBinarySearch_objectArray() {
        String[] arr = {"a", "c", "e", "g", "i"};
        Assertions.assertEquals(2, N.binarySearch(arr, "e"));
        Assertions.assertEquals(-1, N.binarySearch(arr, ""));
        Assertions.assertEquals(-6, N.binarySearch(arr, "z"));
        
        // Test empty array
        String[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, "e"));
        
        // Test null array
        Assertions.assertEquals(-1, N.binarySearch((String[]) null, "e"));
    }
    
    @Test
    public void testBinarySearch_objectArray_withRange() {
        String[] arr = {"a", "c", "e", "g", "i", "k", "m"};
        Assertions.assertEquals(3, N.binarySearch(arr, 1, 5, "g"));
        Assertions.assertEquals(-2, N.binarySearch(arr, 1, 5, "b"));
        
        // Test empty array
        String[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, "e"));
    }
    
    @Test
    public void testBinarySearch_genericArray_withComparator() {
        String[] arr = {"a", "c", "e", "g", "i"};
        Comparator<String> cmp = String::compareTo;
        Assertions.assertEquals(2, N.binarySearch(arr, "e", cmp));
        Assertions.assertEquals(-1, N.binarySearch(arr, "", cmp));
        
        // Test empty array
        String[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, "e", cmp));
        
        // Test null array
        Assertions.assertEquals(-1, N.binarySearch((String[]) null, "e", cmp));
    }
    
    @Test
    public void testBinarySearch_genericArray_withRangeAndComparator() {
        String[] arr = {"a", "c", "e", "g", "i", "k", "m"};
        Comparator<String> cmp = String::compareTo;
        Assertions.assertEquals(3, N.binarySearch(arr, 1, 5, "g", cmp));
        Assertions.assertEquals(-2, N.binarySearch(arr, 1, 5, "b", cmp));
        
        // Test empty array
        String[] empty = {};
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, "e", cmp));
    }
    
    @Test
    public void testBinarySearch_list() {
        List<Integer> list = Arrays.asList(1, 3, 5, 7, 9);
        Assertions.assertEquals(2, N.binarySearch(list, 5));
        Assertions.assertEquals(-1, N.binarySearch(list, 0));
        Assertions.assertEquals(-6, N.binarySearch(list, 10));
        
        // Test empty list
        List<Integer> empty = new ArrayList<>();
        Assertions.assertEquals(-1, N.binarySearch(empty, 5));
        
        // Test null list
        Assertions.assertEquals(-1, N.binarySearch((List<Integer>) null, 5));
    }
    
    @Test
    public void testBinarySearch_list_withRange() {
        List<Integer> list = Arrays.asList(1, 3, 5, 7, 9, 11, 13);
        Assertions.assertEquals(3, N.binarySearch(list, 1, 5, 7));
        Assertions.assertEquals(-2, N.binarySearch(list, 1, 5, 2));
        
        // Test empty list
        List<Integer> empty = new ArrayList<>();
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, 5));
    }
    
    @Test
    public void testBinarySearch_list_withComparator() {
        List<String> list = Arrays.asList("a", "c", "e", "g", "i");
        Comparator<String> cmp = String::compareTo;
        Assertions.assertEquals(2, N.binarySearch(list, "e", cmp));
        Assertions.assertEquals(-1, N.binarySearch(list, "", cmp));
        
        // Test empty list
        List<String> empty = new ArrayList<>();
        Assertions.assertEquals(-1, N.binarySearch(empty, "e", cmp));
        
        // Test null list
        Assertions.assertEquals(-1, N.binarySearch((List<String>) null, "e", cmp));
    }
    
    @Test
    public void testBinarySearch_list_withRangeAndComparator() {
        List<String> list = Arrays.asList("a", "c", "e", "g", "i", "k", "m");
        Comparator<String> cmp = String::compareTo;
        Assertions.assertEquals(3, N.binarySearch(list, 1, 5, "g", cmp));
        Assertions.assertEquals(-2, N.binarySearch(list, 1, 5, "b", cmp));
        
        // Test empty list
        List<String> empty = new ArrayList<>();
        Assertions.assertEquals(-1, N.binarySearch(empty, 0, 0, "e", cmp));
    }
    
    // Tests for indexOf methods
    
    @Test
    public void testIndexOf_booleanArray() {
        boolean[] arr = {true, false, true, false, true};
        Assertions.assertEquals(0, N.indexOf(arr, true));
        Assertions.assertEquals(1, N.indexOf(arr, false));
        
        // Test empty array
        boolean[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, true));
        
        // Test null array
        Assertions.assertEquals(-1, N.indexOf((boolean[]) null, true));
    }
    
    @Test
    public void testIndexOf_booleanArray_withFromIndex() {
        boolean[] arr = {true, false, true, false, true};
        Assertions.assertEquals(2, N.indexOf(arr, true, 1));
        Assertions.assertEquals(3, N.indexOf(arr, false, 2));
        Assertions.assertEquals(-1, N.indexOf(arr, true, 10));
        
        // Test negative fromIndex
        Assertions.assertEquals(0, N.indexOf(arr, true, -1));
    }
    
    @Test
    public void testIndexOf_charArray() {
        char[] arr = {'a', 'b', 'c', 'd', 'e'};
        Assertions.assertEquals(2, N.indexOf(arr, 'c'));
        Assertions.assertEquals(-1, N.indexOf(arr, 'z'));
        
        // Test empty array
        char[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, 'c'));
        
        // Test null array
        Assertions.assertEquals(-1, N.indexOf((char[]) null, 'c'));
    }
    
    @Test
    public void testIndexOf_charArray_withFromIndex() {
        char[] arr = {'a', 'b', 'c', 'd', 'c'};
        Assertions.assertEquals(4, N.indexOf(arr, 'c', 3));
        Assertions.assertEquals(-1, N.indexOf(arr, 'c', 10));
    }
    
    @Test
    public void testIndexOf_byteArray() {
        byte[] arr = {1, 2, 3, 4, 5};
        Assertions.assertEquals(2, N.indexOf(arr, (byte) 3));
        Assertions.assertEquals(-1, N.indexOf(arr, (byte) 10));
        
        // Test empty array
        byte[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, (byte) 3));
        
        // Test null array
        Assertions.assertEquals(-1, N.indexOf((byte[]) null, (byte) 3));
    }
    
    @Test
    public void testIndexOf_byteArray_withFromIndex() {
        byte[] arr = {1, 2, 3, 4, 3};
        Assertions.assertEquals(4, N.indexOf(arr, (byte) 3, 3));
        Assertions.assertEquals(-1, N.indexOf(arr, (byte) 3, 10));
    }
    
    @Test
    public void testIndexOf_shortArray() {
        short[] arr = {1, 2, 3, 4, 5};
        Assertions.assertEquals(2, N.indexOf(arr, (short) 3));
        Assertions.assertEquals(-1, N.indexOf(arr, (short) 10));
        
        // Test empty array
        short[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, (short) 3));
        
        // Test null array
        Assertions.assertEquals(-1, N.indexOf((short[]) null, (short) 3));
    }
    
    @Test
    public void testIndexOf_shortArray_withFromIndex() {
        short[] arr = {1, 2, 3, 4, 3};
        Assertions.assertEquals(4, N.indexOf(arr, (short) 3, 3));
        Assertions.assertEquals(-1, N.indexOf(arr, (short) 3, 10));
    }
    
    @Test
    public void testIndexOf_intArray() {
        int[] arr = {1, 2, 3, 4, 5};
        Assertions.assertEquals(2, N.indexOf(arr, 3));
        Assertions.assertEquals(-1, N.indexOf(arr, 10));
        
        // Test empty array
        int[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, 3));
        
        // Test null array
        Assertions.assertEquals(-1, N.indexOf((int[]) null, 3));
    }
    
    @Test
    public void testIndexOf_intArray_withFromIndex() {
        int[] arr = {1, 2, 3, 4, 3};
        Assertions.assertEquals(4, N.indexOf(arr, 3, 3));
        Assertions.assertEquals(-1, N.indexOf(arr, 3, 10));
    }
    
    @Test
    public void testIndexOf_longArray() {
        long[] arr = {1L, 2L, 3L, 4L, 5L};
        Assertions.assertEquals(2, N.indexOf(arr, 3L));
        Assertions.assertEquals(-1, N.indexOf(arr, 10L));
        
        // Test empty array
        long[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, 3L));
        
        // Test null array
        Assertions.assertEquals(-1, N.indexOf((long[]) null, 3L));
    }
    
    @Test
    public void testIndexOf_longArray_withFromIndex() {
        long[] arr = {1L, 2L, 3L, 4L, 3L};
        Assertions.assertEquals(4, N.indexOf(arr, 3L, 3));
        Assertions.assertEquals(-1, N.indexOf(arr, 3L, 10));
    }
    
    @Test
    public void testIndexOf_floatArray() {
        float[] arr = {1.0f, 2.0f, 3.0f, 4.0f, 5.0f};
        Assertions.assertEquals(2, N.indexOf(arr, 3.0f));
        Assertions.assertEquals(-1, N.indexOf(arr, 10.0f));
        
        // Test empty array
        float[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, 3.0f));
        
        // Test null array
        Assertions.assertEquals(-1, N.indexOf((float[]) null, 3.0f));
    }
    
    @Test
    public void testIndexOf_floatArray_withFromIndex() {
        float[] arr = {1.0f, 2.0f, 3.0f, 4.0f, 3.0f};
        Assertions.assertEquals(4, N.indexOf(arr, 3.0f, 3));
        Assertions.assertEquals(-1, N.indexOf(arr, 3.0f, 10));
    }
    
    @Test
    public void testIndexOf_doubleArray() {
        double[] arr = {1.0, 2.0, 3.0, 4.0, 5.0};
        Assertions.assertEquals(2, N.indexOf(arr, 3.0));
        Assertions.assertEquals(-1, N.indexOf(arr, 10.0));
        
        // Test empty array
        double[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, 3.0));
        
        // Test null array
        Assertions.assertEquals(-1, N.indexOf((double[]) null, 3.0));
    }
    
    @Test
    public void testIndexOf_doubleArray_withFromIndex() {
        double[] arr = {1.0, 2.0, 3.0, 4.0, 3.0};
        Assertions.assertEquals(4, N.indexOf(arr, 3.0, 3));
        Assertions.assertEquals(-1, N.indexOf(arr, 3.0, 10));
    }
    
    @Test
    public void testIndexOf_doubleArray_withTolerance() {
        double[] arr = {1.0, 2.0, 3.001, 4.0, 5.0};
        Assertions.assertEquals(2, N.indexOf(arr, 3.0, 0.01));
        Assertions.assertEquals(-1, N.indexOf(arr, 3.0, 0.0001));
        
        // Test empty array
        double[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, 3.0, 0.01));
        
        // Test null array
        Assertions.assertEquals(-1, N.indexOf((double[]) null, 3.0, 0.01));
    }
    
    @Test
    public void testIndexOf_doubleArray_withToleranceAndFromIndex() {
        double[] arr = {1.0, 2.0, 3.001, 4.0, 3.002};
        Assertions.assertEquals(4, N.indexOf(arr, 3.0, 0.01, 3));
        Assertions.assertEquals(-1, N.indexOf(arr, 3.0, 0.0001, 3));
    }
    
    @Test
    public void testIndexOf_objectArray() {
        String[] arr = {"a", "b", "c", "d", "e"};
        Assertions.assertEquals(2, N.indexOf(arr, "c"));
        Assertions.assertEquals(-1, N.indexOf(arr, "z"));
        
        // Test with null element
        String[] arrWithNull = {"a", null, "c"};
        Assertions.assertEquals(1, N.indexOf(arrWithNull, null));
        
        // Test empty array
        String[] empty = {};
        Assertions.assertEquals(-1, N.indexOf(empty, "c"));
        
        // Test null array
        Assertions.assertEquals(-1, N.indexOf((String[]) null, "c"));
    }
    
    @Test
    public void testIndexOf_objectArray_withFromIndex() {
        String[] arr = {"a", "b", "c", "d", "c"};
        Assertions.assertEquals(4, N.indexOf(arr, "c", 3));
        Assertions.assertEquals(-1, N.indexOf(arr, "c", 10));
    }
    
    @Test
    public void testIndexOf_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Assertions.assertEquals(2, N.indexOf(list, "c"));
        Assertions.assertEquals(-1, N.indexOf(list, "z"));
        
        // Test empty collection
        List<String> empty = new ArrayList<>();
        Assertions.assertEquals(-1, N.indexOf(empty, "c"));
        
        // Test null collection
        Assertions.assertEquals(-1, N.indexOf((Collection<?>) null, "c"));
    }
    
    @Test
    public void testIndexOf_collection_withFromIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        Assertions.assertEquals(4, N.indexOf(list, "c", 3));
        Assertions.assertEquals(-1, N.indexOf(list, "c", 10));
        
        // Test with non-RandomAccess collection
        LinkedList<String> linkedList = new LinkedList<>(list);
        Assertions.assertEquals(4, N.indexOf(linkedList, "c", 3));
    }
    
    @Test
    public void testIndexOf_iterator() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        Assertions.assertEquals(2, N.indexOf(list.iterator(), "c"));
        Assertions.assertEquals(-1, N.indexOf(list.iterator(), "z"));
        
        // Test null iterator
        Assertions.assertEquals(-1, N.indexOf((Iterator<?>) null, "c"));
    }
    
    @Test
    public void testIndexOf_iterator_withFromIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        Assertions.assertEquals(4, N.indexOf(list.iterator(), "c", 3));
        Assertions.assertEquals(-1, N.indexOf(list.iterator(), "c", 10));
    }
    
    @Test
    public void testIndexOfSubList() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "e");
        List<String> sub = Arrays.asList("c", "d");
        Assertions.assertEquals(2, N.indexOfSubList(source, sub));
        
        List<String> notFound = Arrays.asList("x", "y");
        Assertions.assertEquals(-1, N.indexOfSubList(source, notFound));
        
        // Test empty lists
        Assertions.assertEquals(-1, N.indexOfSubList(new ArrayList<>(), sub));
        Assertions.assertEquals(-1, N.indexOfSubList(source, new ArrayList<>()));
    }
    
    @Test
    public void testIndexOfSubList_withFromIndex() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        Assertions.assertEquals(4, N.indexOfSubList(source, sub, 3));
        Assertions.assertEquals(-1, N.indexOfSubList(source, sub, 10));
    }
    
    @Test
    public void testIndexOfIgnoreCase() {
        String[] arr = {"A", "B", "C", "D", "E"};
        Assertions.assertEquals(2, N.indexOfIgnoreCase(arr, "c"));
        Assertions.assertEquals(2, N.indexOfIgnoreCase(arr, "C"));
        Assertions.assertEquals(-1, N.indexOfIgnoreCase(arr, "z"));
        
        // Test empty array
        String[] empty = {};
        Assertions.assertEquals(-1, N.indexOfIgnoreCase(empty, "c"));
        
        // Test null array
        Assertions.assertEquals(-1, N.indexOfIgnoreCase((String[]) null, "c"));
    }
    
    @Test
    public void testIndexOfIgnoreCase_withFromIndex() {
        String[] arr = {"A", "B", "C", "D", "c"};
        Assertions.assertEquals(4, N.indexOfIgnoreCase(arr, "C", 3));
        Assertions.assertEquals(-1, N.indexOfIgnoreCase(arr, "C", 10));
    }
    
    // Tests for lastIndexOf methods
    
    @Test
    public void testLastIndexOf_booleanArray() {
        boolean[] arr = {true, false, true, false, true};
        Assertions.assertEquals(4, N.lastIndexOf(arr, true));
        Assertions.assertEquals(3, N.lastIndexOf(arr, false));
        
        // Test empty array
        boolean[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, true));
        
        // Test null array
        Assertions.assertEquals(-1, N.lastIndexOf((boolean[]) null, true));
    }
    
    @Test
    public void testLastIndexOf_booleanArray_withStartIndex() {
        boolean[] arr = {true, false, true, false, true};
        Assertions.assertEquals(2, N.lastIndexOf(arr, true, 3));
        Assertions.assertEquals(1, N.lastIndexOf(arr, false, 2));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, true, -1));
    }
    
    @Test
    public void testLastIndexOf_charArray() {
        char[] arr = {'a', 'b', 'c', 'd', 'c'};
        Assertions.assertEquals(4, N.lastIndexOf(arr, 'c'));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 'z'));
        
        // Test empty array
        char[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, 'c'));
        
        // Test null array
        Assertions.assertEquals(-1, N.lastIndexOf((char[]) null, 'c'));
    }
    
    @Test
    public void testLastIndexOf_charArray_withStartIndex() {
        char[] arr = {'a', 'b', 'c', 'd', 'c'};
        Assertions.assertEquals(2, N.lastIndexOf(arr, 'c', 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 'c', -1));
    }
    
    @Test
    public void testLastIndexOf_byteArray() {
        byte[] arr = {1, 2, 3, 4, 3};
        Assertions.assertEquals(4, N.lastIndexOf(arr, (byte) 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, (byte) 10));
        
        // Test empty array
        byte[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, (byte) 3));
        
        // Test null array
        Assertions.assertEquals(-1, N.lastIndexOf((byte[]) null, (byte) 3));
    }
    
    @Test
    public void testLastIndexOf_byteArray_withStartIndex() {
        byte[] arr = {1, 2, 3, 4, 3};
        Assertions.assertEquals(2, N.lastIndexOf(arr, (byte) 3, 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, (byte) 3, -1));
    }
    
    @Test
    public void testLastIndexOf_shortArray() {
        short[] arr = {1, 2, 3, 4, 3};
        Assertions.assertEquals(4, N.lastIndexOf(arr, (short) 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, (short) 10));
        
        // Test empty array
        short[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, (short) 3));
        
        // Test null array
        Assertions.assertEquals(-1, N.lastIndexOf((short[]) null, (short) 3));
    }
    
    @Test
    public void testLastIndexOf_shortArray_withStartIndex() {
        short[] arr = {1, 2, 3, 4, 3};
        Assertions.assertEquals(2, N.lastIndexOf(arr, (short) 3, 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, (short) 3, -1));
    }
    
    @Test
    public void testLastIndexOf_intArray() {
        int[] arr = {1, 2, 3, 4, 3};
        Assertions.assertEquals(4, N.lastIndexOf(arr, 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 10));
        
        // Test empty array
        int[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, 3));
        
        // Test null array
        Assertions.assertEquals(-1, N.lastIndexOf((int[]) null, 3));
    }
    
    @Test
    public void testLastIndexOf_intArray_withStartIndex() {
        int[] arr = {1, 2, 3, 4, 3};
        Assertions.assertEquals(2, N.lastIndexOf(arr, 3, 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 3, -1));
    }
    
    @Test
    public void testLastIndexOf_longArray() {
        long[] arr = {1L, 2L, 3L, 4L, 3L};
        Assertions.assertEquals(4, N.lastIndexOf(arr, 3L));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 10L));
        
        // Test empty array
        long[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, 3L));
        
        // Test null array
        Assertions.assertEquals(-1, N.lastIndexOf((long[]) null, 3L));
    }
    
    @Test
    public void testLastIndexOf_longArray_withStartIndex() {
        long[] arr = {1L, 2L, 3L, 4L, 3L};
        Assertions.assertEquals(2, N.lastIndexOf(arr, 3L, 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 3L, -1));
    }
    
    @Test
    public void testLastIndexOf_floatArray() {
        float[] arr = {1.0f, 2.0f, 3.0f, 4.0f, 3.0f};
        Assertions.assertEquals(4, N.lastIndexOf(arr, 3.0f));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 10.0f));
        
        // Test empty array
        float[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, 3.0f));
        
        // Test null array
        Assertions.assertEquals(-1, N.lastIndexOf((float[]) null, 3.0f));
    }
    
    @Test
    public void testLastIndexOf_floatArray_withStartIndex() {
        float[] arr = {1.0f, 2.0f, 3.0f, 4.0f, 3.0f};
        Assertions.assertEquals(2, N.lastIndexOf(arr, 3.0f, 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 3.0f, -1));
    }
    
    @Test
    public void testLastIndexOf_doubleArray() {
        double[] arr = {1.0, 2.0, 3.0, 4.0, 3.0};
        Assertions.assertEquals(4, N.lastIndexOf(arr, 3.0));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 10.0));
        
        // Test empty array
        double[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, 3.0));
        
        // Test null array
        Assertions.assertEquals(-1, N.lastIndexOf((double[]) null, 3.0));
    }
    
    @Test
    public void testLastIndexOf_doubleArray_withStartIndex() {
        double[] arr = {1.0, 2.0, 3.0, 4.0, 3.0};
        Assertions.assertEquals(2, N.lastIndexOf(arr, 3.0, 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 3.0, -1));
    }
    
    @Test
    public void testLastIndexOf_doubleArray_withTolerance() {
        double[] arr = {1.0, 2.0, 3.001, 4.0, 3.002};
        Assertions.assertEquals(4, N.lastIndexOf(arr, 3.0, 0.01));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 3.0, 0.0001));
        
        // Test empty array
        double[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, 3.0, 0.01));
        
        // Test null array
        Assertions.assertEquals(-1, N.lastIndexOf((double[]) null, 3.0, 0.01));
    }
    
    @Test
    public void testLastIndexOf_doubleArray_withToleranceAndStartIndex() {
        double[] arr = {1.0, 2.0, 3.001, 4.0, 3.002};
        Assertions.assertEquals(2, N.lastIndexOf(arr, 3.0, 0.01, 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, 3.0, 0.0001, 3));
    }
    
    @Test
    public void testLastIndexOf_objectArray() {
        String[] arr = {"a", "b", "c", "d", "c"};
        Assertions.assertEquals(4, N.lastIndexOf(arr, "c"));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, "z"));
        
        // Test with null element
        String[] arrWithNull = {"a", null, "c", null};
        Assertions.assertEquals(3, N.lastIndexOf(arrWithNull, null));
        
        // Test empty array
        String[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOf(empty, "c"));
        
        // Test null array
        Assertions.assertEquals(-1, N.lastIndexOf((String[]) null, "c"));
    }
    
    @Test
    public void testLastIndexOf_objectArray_withStartIndex() {
        String[] arr = {"a", "b", "c", "d", "c"};
        Assertions.assertEquals(2, N.lastIndexOf(arr, "c", 3));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, "c", -1));
    }
    
    @Test
    public void testLastIndexOf_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        Assertions.assertEquals(4, N.lastIndexOf(list, "c"));
        Assertions.assertEquals(-1, N.lastIndexOf(list, "z"));
        
        // Test empty collection
        List<String> empty = new ArrayList<>();
        Assertions.assertEquals(-1, N.lastIndexOf(empty, "c"));
        
        // Test null collection
        Assertions.assertEquals(-1, N.lastIndexOf((Collection<?>) null, "c"));
    }
    
    @Test
    public void testLastIndexOf_collection_withStartIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        Assertions.assertEquals(2, N.lastIndexOf(list, "c", 3));
        Assertions.assertEquals(-1, N.lastIndexOf(list, "c", -1));
        
        // Test with non-RandomAccess collection
        LinkedList<String> linkedList = new LinkedList<>(list);
        Assertions.assertEquals(2, N.lastIndexOf(linkedList, "c", 3));
    }
    
    @Test
    public void testLastIndexOfSubList() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        Assertions.assertEquals(4, N.lastIndexOfSubList(source, sub));
        
        List<String> notFound = Arrays.asList("x", "y");
        Assertions.assertEquals(-1, N.lastIndexOfSubList(source, notFound));
        
        // Test empty lists
        Assertions.assertEquals(-1, N.lastIndexOfSubList(new ArrayList<>(), sub));
        Assertions.assertEquals(-1, N.lastIndexOfSubList(source, new ArrayList<>()));
    }
    
    @Test
    public void testLastIndexOfSubList_withStartIndex() {
        List<String> source = Arrays.asList("a", "b", "c", "d", "c", "d");
        List<String> sub = Arrays.asList("c", "d");
        Assertions.assertEquals(2, N.lastIndexOfSubList(source, sub, 3));
        Assertions.assertEquals(-1, N.lastIndexOfSubList(source, sub, 1));
    }
    
    @Test
    public void testLastIndexOfIgnoreCase() {
        String[] arr = {"A", "B", "C", "D", "c"};
        Assertions.assertEquals(4, N.lastIndexOfIgnoreCase(arr, "C"));
        Assertions.assertEquals(4, N.lastIndexOfIgnoreCase(arr, "c"));
        Assertions.assertEquals(-1, N.lastIndexOfIgnoreCase(arr, "z"));
        
        // Test empty array
        String[] empty = {};
        Assertions.assertEquals(-1, N.lastIndexOfIgnoreCase(empty, "c"));
        
        // Test null array
        Assertions.assertEquals(-1, N.lastIndexOfIgnoreCase((String[]) null, "c"));
    }
    
    @Test
    public void testLastIndexOfIgnoreCase_withStartIndex() {
        String[] arr = {"A", "B", "C", "D", "c"};
        Assertions.assertEquals(2, N.lastIndexOfIgnoreCase(arr, "C", 3));
        Assertions.assertEquals(-1, N.lastIndexOfIgnoreCase(arr, "C", -1));
    }
    
    // Tests for findFirstIndex methods
    
    @Test
    public void testFindFirstIndex_array() {
        String[] arr = {"a", "b", "c", "d", "e"};
        OptionalInt result = N.findFirstIndex(arr, s -> s.equals("c"));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.getAsInt());
        
        OptionalInt notFound = N.findFirstIndex(arr, s -> s.equals("z"));
        Assertions.assertFalse(notFound.isPresent());
        
        // Test empty array
        String[] empty = {};
        OptionalInt emptyResult = N.findFirstIndex(empty, s -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }
    
    @Test
    public void testFindFirstIndex_array_withBiPredicate() {
        String[] arr = {"a", "b", "c", "d", "e"};
        String prefix = "c";
        OptionalInt result = N.findFirstIndex(arr, prefix, (s, p) -> s.startsWith(p));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.getAsInt());
        
        // Test empty array
        String[] empty = {};
        OptionalInt emptyResult = N.findFirstIndex(empty, prefix, (s, p) -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }
    
    @Test
    public void testFindFirstIndex_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        OptionalInt result = N.findFirstIndex(list, s -> s.equals("c"));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.getAsInt());
        
        OptionalInt notFound = N.findFirstIndex(list, s -> s.equals("z"));
        Assertions.assertFalse(notFound.isPresent());
        
        // Test empty collection
        List<String> empty = new ArrayList<>();
        OptionalInt emptyResult = N.findFirstIndex(empty, s -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }
    
    @Test
    public void testFindFirstIndex_collection_withBiPredicate() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "e");
        String prefix = "c";
        OptionalInt result = N.findFirstIndex(list, prefix, (s, p) -> s.startsWith(p));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(2, result.getAsInt());
        
        // Test empty collection
        List<String> empty = new ArrayList<>();
        OptionalInt emptyResult = N.findFirstIndex(empty, prefix, (s, p) -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }
    
    // Tests for findLastIndex methods
    
    @Test
    public void testFindLastIndex_array() {
        String[] arr = {"a", "b", "c", "d", "c"};
        OptionalInt result = N.findLastIndex(arr, s -> s.equals("c"));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.getAsInt());
        
        OptionalInt notFound = N.findLastIndex(arr, s -> s.equals("z"));
        Assertions.assertFalse(notFound.isPresent());
        
        // Test empty array
        String[] empty = {};
        OptionalInt emptyResult = N.findLastIndex(empty, s -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }
    
    @Test
    public void testFindLastIndex_array_withBiPredicate() {
        String[] arr = {"a", "b", "c", "d", "ca"};
        String prefix = "c";
        OptionalInt result = N.findLastIndex(arr, prefix, (s, p) -> s.startsWith(p));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.getAsInt());
        
        // Test empty array
        String[] empty = {};
        OptionalInt emptyResult = N.findLastIndex(empty, prefix, (s, p) -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }
    
    @Test
    public void testFindLastIndex_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "c");
        OptionalInt result = N.findLastIndex(list, s -> s.equals("c"));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.getAsInt());
        
        OptionalInt notFound = N.findLastIndex(list, s -> s.equals("z"));
        Assertions.assertFalse(notFound.isPresent());
        
        // Test empty collection
        List<String> empty = new ArrayList<>();
        OptionalInt emptyResult = N.findLastIndex(empty, s -> true);
        Assertions.assertFalse(emptyResult.isPresent());
        
        // Test non-RandomAccess collection
        LinkedList<String> linkedList = new LinkedList<>(list);
        OptionalInt linkedResult = N.findLastIndex(linkedList, s -> s.equals("c"));
        Assertions.assertTrue(linkedResult.isPresent());
        Assertions.assertEquals(4, linkedResult.getAsInt());
    }
    
    @Test
    public void testFindLastIndex_collection_withBiPredicate() {
        List<String> list = Arrays.asList("a", "b", "c", "d", "ca");
        String prefix = "c";
        OptionalInt result = N.findLastIndex(list, prefix, (s, p) -> s.startsWith(p));
        Assertions.assertTrue(result.isPresent());
        Assertions.assertEquals(4, result.getAsInt());
        
        // Test empty collection
        List<String> empty = new ArrayList<>();
        OptionalInt emptyResult = N.findLastIndex(empty, prefix, (s, p) -> true);
        Assertions.assertFalse(emptyResult.isPresent());
    }
    
    // Tests for indicesOfAllMin methods
    
    @Test
    public void testIndicesOfAllMin_array() {
        Integer[] arr = {3, 1, 4, 1, 5, 1};
        int[] indices = N.indicesOfAllMin(arr);
        Assertions.assertArrayEquals(new int[]{1, 3, 5}, indices);
        
        // Test single element
        Integer[] single = {5};
        int[] singleIndices = N.indicesOfAllMin(single);
        Assertions.assertArrayEquals(new int[]{0}, singleIndices);
        
        // Test empty array
        Integer[] empty = {};
        int[] emptyIndices = N.indicesOfAllMin(empty);
        Assertions.assertArrayEquals(new int[]{}, emptyIndices);
    }
    
    @Test
    public void testIndicesOfAllMin_array_withComparator() {
        String[] arr = {"cat", "a", "dog", "a", "bird", "a"};
        Comparator<String> cmp = Comparator.comparing(String::length);
        int[] indices = N.indicesOfAllMin(arr, cmp);
        Assertions.assertArrayEquals(new int[]{1, 3, 5}, indices);
        
        // Test with null comparator (should use null-safe comparator)
        String[] arrWithNull = {null, "a", "b", null};
        int[] nullIndices = N.indicesOfAllMin(arrWithNull, null);
        Assertions.assertArrayEquals(new int[]{1}, nullIndices);
    }
    
    @Test
    public void testIndicesOfAllMin_collection() {
        List<Integer> list = Arrays.asList(3, 1, 4, 1, 5, 1);
        int[] indices = N.indicesOfAllMin(list);
        Assertions.assertArrayEquals(new int[]{1, 3, 5}, indices);
        
        // Test empty collection
        List<Integer> empty = new ArrayList<>();
        int[] emptyIndices = N.indicesOfAllMin(empty);
        Assertions.assertArrayEquals(new int[]{}, emptyIndices);
    }
    
    @Test
    public void testIndicesOfAllMin_collection_withComparator() {
        List<String> list = Arrays.asList("cat", "a", "dog", "a", "bird", "a");
        Comparator<String> cmp = Comparator.comparing(String::length);
        int[] indices = N.indicesOfAllMin(list, cmp);
        Assertions.assertArrayEquals(new int[]{1, 3, 5}, indices);
    }
    
    // Tests for indicesOfAllMax methods
    
    @Test
    public void testIndicesOfAllMax_array() {
        Integer[] arr = {3, 5, 4, 5, 1, 5};
        int[] indices = N.indicesOfAllMax(arr);
        Assertions.assertArrayEquals(new int[]{1, 3, 5}, indices);
        
        // Test single element
        Integer[] single = {5};
        int[] singleIndices = N.indicesOfAllMax(single);
        Assertions.assertArrayEquals(new int[]{0}, singleIndices);
        
        // Test empty array
        Integer[] empty = {};
        int[] emptyIndices = N.indicesOfAllMax(empty);
        Assertions.assertArrayEquals(new int[]{}, emptyIndices);
    }
    
    @Test
    public void testIndicesOfAllMax_array_withComparator() {
        String[] arr = {"a", "cat", "b", "dog", "c", "dog"};
        Comparator<String> cmp = Comparator.comparing(String::length);
        int[] indices = N.indicesOfAllMax(arr, cmp);
        Assertions.assertArrayEquals(new int[]{1, 3, 5}, indices);
        
        // Test with null comparator (should use null-safe comparator)
        String[] arrWithNull = {"a", null, "b", null};
        int[] nullIndices = N.indicesOfAllMax(arrWithNull, null);
        Assertions.assertArrayEquals(new int[] { 2 }, nullIndices);
    }
    
    @Test
    public void testIndicesOfAllMax_collection() {
        List<Integer> list = Arrays.asList(3, 5, 4, 5, 1, 5);
        int[] indices = N.indicesOfAllMax(list);
        Assertions.assertArrayEquals(new int[]{1, 3, 5}, indices);
        
        // Test empty collection
        List<Integer> empty = new ArrayList<>();
        int[] emptyIndices = N.indicesOfAllMax(empty);
        Assertions.assertArrayEquals(new int[]{}, emptyIndices);
    }
    
    @Test
    public void testIndicesOfAllMax_collection_withComparator() {
        List<String> list = Arrays.asList("a", "cat", "b", "dog", "c", "dog");
        Comparator<String> cmp = Comparator.comparing(String::length);
        int[] indices = N.indicesOfAllMax(list, cmp);
        Assertions.assertArrayEquals(new int[]{1, 3, 5}, indices);
    }
    
    // Tests for indicesOfAll methods
    
    @Test
    public void testIndicesOfAll_objectArray() {
        String[] arr = {"a", "b", "c", "b", "d", "b"};
        int[] indices = N.indicesOfAll(arr, "b");
        Assertions.assertArrayEquals(new int[]{1, 3, 5}, indices);
        
        // Test not found
        int[] notFound = N.indicesOfAll(arr, "z");
        Assertions.assertArrayEquals(new int[]{}, notFound);
        
        // Test with null
        String[] arrWithNull = {"a", null, "b", null};
        int[] nullIndices = N.indicesOfAll(arrWithNull, (String) null);
        Assertions.assertArrayEquals(new int[]{1, 3}, nullIndices);
        
        // Test empty array
        String[] empty = {};
        int[] emptyIndices = N.indicesOfAll(empty, "a");
        Assertions.assertArrayEquals(new int[]{}, emptyIndices);
    }
    
    @Test
    public void testIndicesOfAll_objectArray_withStartIndex() {
        String[] arr = {"a", "b", "c", "b", "d", "b"};
        int[] indices = N.indicesOfAll(arr, "b", 2);
        Assertions.assertArrayEquals(new int[]{3, 5}, indices);
        
        // Test start index beyond array
        int[] beyondIndices = N.indicesOfAll(arr, "b", 10);
        Assertions.assertArrayEquals(new int[]{}, beyondIndices);
        
        // Test negative start index
        int[] negativeIndices = N.indicesOfAll(arr, "b", -1);
        Assertions.assertArrayEquals(new int[]{1, 3, 5}, negativeIndices);
    }
    
    @Test
    public void testIndicesOfAll_collection() {
        List<String> list = Arrays.asList("a", "b", "c", "b", "d", "b");
        int[] indices = N.indicesOfAll(list, "b");
        Assertions.assertArrayEquals(new int[]{1, 3, 5}, indices);
        
        // Test empty collection
        List<String> empty = new ArrayList<>();
        int[] emptyIndices = N.indicesOfAll(empty, "a");
        Assertions.assertArrayEquals(new int[]{}, emptyIndices);
        
        // Test non-RandomAccess collection
        LinkedList<String> linkedList = new LinkedList<>(list);
        int[] linkedIndices = N.indicesOfAll(linkedList, "b");
        Assertions.assertArrayEquals(new int[]{1, 3, 5}, linkedIndices);
    }
    
    @Test
    public void testIndicesOfAll_collection_withStartIndex() {
        List<String> list = Arrays.asList("a", "b", "c", "b", "d", "b");
        int[] indices = N.indicesOfAll(list, "b", 2);
        Assertions.assertArrayEquals(new int[]{3, 5}, indices);
        
        // Test start index beyond collection
        int[] beyondIndices = N.indicesOfAll(list, "b", 10);
        Assertions.assertArrayEquals(new int[]{}, beyondIndices);
    }
    
    @Test
    public void testIndicesOfAll_array_withPredicate() {
        String[] arr = {"apple", "banana", "apricot", "cherry", "avocado"};
        int[] indices = N.indicesOfAll(arr, s -> s.startsWith("a"));
        Assertions.assertArrayEquals(new int[]{0, 2, 4}, indices);
        
        // Test empty array
        String[] empty = {};
        int[] emptyIndices = N.indicesOfAll(empty, s -> true);
        Assertions.assertArrayEquals(new int[]{}, emptyIndices);
    }
    
    @Test
    public void testIndicesOfAll_array_withPredicateAndStartIndex() {
        String[] arr = {"apple", "banana", "apricot", "cherry", "avocado"};
        int[] indices = N.indicesOfAll(arr, s -> s.startsWith("a"), 1);
        Assertions.assertArrayEquals(new int[]{2, 4}, indices);
        
        // Test start index beyond array
        int[] beyondIndices = N.indicesOfAll(arr, s -> s.startsWith("a"), 10);
        Assertions.assertArrayEquals(new int[]{}, beyondIndices);
    }
    
    @Test
    public void testIndicesOfAll_collection_withPredicate() {
        List<String> list = Arrays.asList("apple", "banana", "apricot", "cherry", "avocado");
        int[] indices = N.indicesOfAll(list, s -> s.startsWith("a"));
        Assertions.assertArrayEquals(new int[]{0, 2, 4}, indices);
        
        // Test empty collection
        List<String> empty = new ArrayList<>();
        int[] emptyIndices = N.indicesOfAll(empty, s -> true);
        Assertions.assertArrayEquals(new int[]{}, emptyIndices);
        
        // Test non-RandomAccess collection
        LinkedList<String> linkedList = new LinkedList<>(list);
        int[] linkedIndices = N.indicesOfAll(linkedList, s -> s.startsWith("a"));
        Assertions.assertArrayEquals(new int[]{0, 2, 4}, linkedIndices);
    }
    
    @Test
    public void testIndicesOfAll_collection_withPredicateAndFromIndex() {
        List<String> list = Arrays.asList("apple", "banana", "apricot", "cherry", "avocado");
        int[] indices = N.indicesOfAll(list, s -> s.startsWith("a"), 1);
        Assertions.assertArrayEquals(new int[]{2, 4}, indices);
        
        // Test start index beyond collection
        int[] beyondIndices = N.indicesOfAll(list, s -> s.startsWith("a"), 10);
        Assertions.assertArrayEquals(new int[]{}, beyondIndices);
    }
    
    // Test for edge cases and special scenarios
    
    @Test
    public void testEdgeCases() {
        // Test with arrays/collections containing all identical elements
        Integer[] allSame = {5, 5, 5, 5, 5};
        int[] minIndices = N.indicesOfAllMin(allSame);
        Assertions.assertArrayEquals(new int[]{0, 1, 2, 3, 4}, minIndices);
        int[] maxIndices = N.indicesOfAllMax(allSame);
        Assertions.assertArrayEquals(new int[]{0, 1, 2, 3, 4}, maxIndices);
        
        // Test with very large start indices
        String[] arr = {"a", "b", "c"};
        Assertions.assertEquals(-1, N.indexOf(arr, "a", Integer.MAX_VALUE));
        Assertions.assertEquals(-1, N.lastIndexOf(arr, "a", Integer.MIN_VALUE));
    }
    
    @Test
    public void testWithSpecialFloatingPointValues() {
        // Test with NaN values
        double[] arrWithNaN = {1.0, Double.NaN, 3.0, Double.NaN};
        Assertions.assertEquals(1, N.indexOf(arrWithNaN, Double.NaN));
        Assertions.assertEquals(3, N.lastIndexOf(arrWithNaN, Double.NaN));
        
        // Test with infinity values
        double[] arrWithInf = {1.0, Double.POSITIVE_INFINITY, 3.0, Double.NEGATIVE_INFINITY};
        Assertions.assertEquals(1, N.indexOf(arrWithInf, Double.POSITIVE_INFINITY));
        Assertions.assertEquals(3, N.indexOf(arrWithInf, Double.NEGATIVE_INFINITY));
    }
    
    @Test
    public void testGetDescendingIteratorIfPossible() {
        // Test with Deque
        Deque<String> deque = new ArrayDeque<>(Arrays.asList("a", "b", "c"));
        Iterator<String> descIter = N.getDescendingIteratorIfPossible(deque);
        Assertions.assertNotNull(descIter);
        
        // Test with regular List (should return null)
        List<String> list = new ArrayList<>(Arrays.asList("a", "b", "c"));
        Iterator<String> listIter = N.getDescendingIteratorIfPossible(list);
        Assertions.assertNull(listIter);
    }
    
    @Test
    public void testCreateMask() {
        // Test createMask method
        Runnable mask = N.createMask(Runnable.class);
        Assertions.assertNotNull(mask);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> mask.run());
        
        // Test with another interface
        Comparator<?> compMask = N.createMask(Comparator.class);
        Assertions.assertNotNull(compMask);
        Assertions.assertThrows(UnsupportedOperationException.class, () -> compMask.compare(null, null));
    }
}