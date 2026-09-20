package com.landawn.abacus.type;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.landawn.abacus.TestBase;
import com.landawn.abacus.annotation.JsonXmlCreator;
import com.landawn.abacus.annotation.JsonXmlValue;
import com.landawn.abacus.parser.ParserFactory;

public class SingleValueGenericTypeTest extends TestBase {
    private static final List<BigDecimal> DECIMALS = Arrays.asList(new BigDecimal("1.2300"), new BigDecimal("12345678901234567890.123456789"),
            new BigDecimal("1E-1000"), new BigDecimal("-0.0000"), null);

    @Test
    public void annotatedFieldAndMethodPreserveExactElements() {
        Type<FieldBox> field = TypeFactory.getType(FieldBox.class);
        assertEquals(DECIMALS, field.valueOf(field.stringOf(FieldBox.of(DECIMALS))).value);
        assertEquals(List.of(), field.valueOf("[]").value);
        assertNull(field.valueOf((String) null));
        Type<MethodBox> method = TypeFactory.getType(MethodBox.class);
        assertEquals(DECIMALS, method.valueOf(method.stringOf(MethodBox.of(DECIMALS))).value());
        assertEquals(List.of(), method.valueOf("[]").value());
        assertNull(method.valueOf((String) null));
    }

    @Test
    public void autoDetectedValueUsesFieldTypeInsteadOfBroadConstructor() {
        Type<AutoBox> type = TypeFactory.getType(AutoBox.class);
        assertInstanceOf(ObjectType.class, type);
        assertEquals(DECIMALS, type.valueOf(type.stringOf(new AutoBox(DECIMALS))).value());
        assertEquals(List.of(), type.valueOf("[]").value());
        assertNull(type.valueOf((String) null));
    }

    @Test
    public void nestedClassAndFactoryVariablesResolveByIdentity() {
        Type<GenericBox<List<BigDecimal>>> type = TypeFactory.getType(GenericBox.class.getName() + "<List<BigDecimal>>");
        assertEquals(DECIMALS, type.valueOf(type.stringOf(GenericBox.of(DECIMALS))).value);
        Type<ListBox<BigDecimal>> listType = TypeFactory.getType(ListBox.class.getName() + "<BigDecimal>");
        assertEquals(DECIMALS, listType.valueOf(listType.stringOf(ListBox.of(DECIMALS))).value);
        assertNull(type.valueOf((String) null));
        assertEquals(List.of(), type.valueOf("[]").value);
    }

    @Test
    public void unicodeNestedMapsAndGenericArraysPreserveDeclaredValues() {
        Type<GenericBox<Map<String, List<BigDecimal>>>> mapType = TypeFactory.getType(GenericBox.class.getName() + "<Map<String,List<BigDecimal>>>");
        Map<String, List<BigDecimal>> expected = Map.of("\u0000\uD83D\uDE00\uD800x\uFFFF", DECIMALS);
        assertEquals(expected, mapType.valueOf(mapType.stringOf(GenericBox.of(expected))).value);
        Type<ArrayBox<BigDecimal>> arrayType = TypeFactory.getType(ArrayBox.class.getName() + "<BigDecimal>");
        BigDecimal[] array = DECIMALS.toArray(BigDecimal[]::new);
        assertArrayEquals(array, arrayType.valueOf(arrayType.stringOf(ArrayBox.of(array))).value);
        Type<GenericBox<BigDecimal[]>> wrappedArray = TypeFactory.getType(GenericBox.class.getName() + "<BigDecimal[]>");
        assertArrayEquals(array, wrappedArray.valueOf(wrappedArray.stringOf(GenericBox.of(array))).value);
        Type<GenericBox<List<BigDecimal>[]>> nestedArray = TypeFactory.getType(GenericBox.class.getName() + "<List<BigDecimal>[]>");
        List<BigDecimal>[] lists = new List[] { DECIMALS };
        assertArrayEquals(lists, nestedArray.valueOf(nestedArray.stringOf(GenericBox.of(lists))).value);
    }

    @Test
    public void wildcardsAndUnboundVariablesUseUpperBounds() {
        Type<WildcardBox> wildcard = TypeFactory.getType(WildcardBox.class);
        assertEquals(DECIMALS, wildcard.valueOf(wildcard.stringOf(WildcardBox.of(DECIMALS))).value);
        Type<BoundedBox> bounded = TypeFactory.getType(BoundedBox.class);
        assertEquals(new BigDecimal("1.2300"), bounded.valueOf("1.2300").value);
        Type<GenericBox<? extends BigDecimal>> argument = TypeFactory.getType(GenericBox.class.getName() + "<? extends java.math.BigDecimal>");
        assertEquals(new BigDecimal("1.2300"), argument.valueOf("1.2300").value);
        for (String parameter : new String[] { "?", "? super java.math.BigDecimal", "? extends Number" }) {
            Type<BoundedBox<?>> wildcardBound = TypeFactory.getType(BoundedBox.class.getName() + "<" + parameter + ">");
            assertEquals(new BigDecimal("1.2300"), wildcardBound.valueOf("1.2300").value);
        }
    }

    @Test
    public void wrapperValuesInsideCollectionsKeepTheirGenericArguments() {
        Type<List<GenericBox<List<BigDecimal>>>> type = TypeFactory.getType("List<" + GenericBox.class.getName() + "<List<BigDecimal>>>");
        List<GenericBox<List<BigDecimal>>> values = Arrays.asList(GenericBox.of(DECIMALS), null, GenericBox.of(List.of()));
        for (boolean stream : new boolean[] { false, true }) {
            List<GenericBox<List<BigDecimal>>> parsed = parse(type.stringOf(values), type, stream);
            assertEquals(DECIMALS, parsed.get(0).value);
            assertNull(parsed.get(1));
            assertEquals(List.of(), parsed.get(2).value);
        }
    }

    @Test
    public void scalarWrappersUseOriginalTokensAndPreserveQuotedNull() {
        Type<List<GenericBox<BigDecimal>>> numbers = TypeFactory.getType("List<" + GenericBox.class.getName() + "<BigDecimal>>");
        Type<List<GenericBox<String>>> strings = TypeFactory.getType("List<" + GenericBox.class.getName() + "<String>>");
        for (boolean stream : new boolean[] { false, true }) {
            List<GenericBox<BigDecimal>> parsed = parse("[1.2300,null,-0.0000,1e-1000]", numbers, stream);
            assertEquals(new BigDecimal("1.2300"), parsed.get(0).value);
            assertNull(parsed.get(1));
            assertEquals(new BigDecimal("-0.0000"), parsed.get(2).value);
            assertEquals(new BigDecimal("1e-1000"), parsed.get(3).value);
            List<GenericBox<String>> text = parse("[\"null\",null,\"\",\"a\\\"b\\\\c\\uD800\"]", strings, stream);
            assertEquals("null", text.get(0).value);
            assertNull(text.get(1));
            assertEquals("", text.get(2).value);
            assertEquals("a\"b\\c\uD800", text.get(3).value);
        }
    }

    @Test
    public void capturedStructuresPreserveTokensAndRejectInvalidDelimiters() {
        Type<List<GenericBox<Map<String, List<BigDecimal>>>>> maps = TypeFactory
                .getType("List<" + GenericBox.class.getName() + "<Map<String,List<BigDecimal>>>>");
        Type<List<GenericBox<List<String>>>> strings = TypeFactory.getType("List<" + GenericBox.class.getName() + "<List<String>>>");
        for (boolean stream : new boolean[] { false, true }) {
            List<GenericBox<Map<String, List<BigDecimal>>>> result = parse("[{'key':[1.2300,null]},{}]", maps, stream);
            assertEquals(Arrays.asList(new BigDecimal("1.2300"), null), result.get(0).value.get("key"));
            assertEquals(Map.of(), result.get(1).value);
            List<GenericBox<List<String>>> text = parse("[['',null,'null','a\\\'b','\\u0000\\uD800x\\uFFFF'],[]]", strings, stream);
            assertEquals(Arrays.asList("", null, "null", "a'b", "\u0000\uD800x\uFFFF"), text.get(0).value);
            assertEquals(List.of(), text.get(1).value);
            // The existing list parser accepts a trailing empty slot; the wrapper follows that grammar.
            assertEquals(TypeFactory.getType("List<String>").valueOf("[1,]"), parse("[[1,]]", strings, stream).get(0).value);
            for (String invalid : new String[] { "[[1}]", "[[1]", "[[\"unterminated]]", "[[1 2]]", "[[1][2]]" }) {
                assertThrows(RuntimeException.class, () -> parse(invalid, strings, stream), invalid);
            }
            String deep = "[".repeat(1100) + "0" + "]".repeat(1100);
            assertThrows(RuntimeException.class, () -> parse(deep, strings, stream));
            assertEquals(List.of(), parse("[[]]", strings, stream).get(0).value);
        }
    }

    private static <T> T parse(String text, Type<T> type, boolean stream) {
        return stream ? ParserFactory.createJsonParser().deserialize(new TinyReader(text), type) : type.valueOf(text);
    }

    private static class TinyReader extends StringReader {
        TinyReader(String text) {
            super(text);
        }

        @Override
        public int read(char[] buffer, int offset, int length) throws IOException {
            return super.read(buffer, offset, Math.min(1, length));
        }
    }

    @Test
    public void contradictoryCreatorArgumentsFailBeforeInvocation() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType(WrongBox.class));
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType(WrongBoundBox.class));
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType(WrongArrayBox.class));
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType(BoundedBox.class.getName() + "<? super String>"));
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType(WrongReturnBox.class.getName() + "<BigDecimal>"));
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType(NestedBoundBox.class.getName() + "<? extends List<String>>"));
        for (String argument : new String[] { "?", "? extends List<?>", "? extends List<BigDecimal>" }) {
            Type<NestedBoundBox<?>> valid = TypeFactory.getType(NestedBoundBox.class.getName() + "<" + argument + ">");
            assertEquals(List.of(new BigDecimal("1.2300")), valid.valueOf("[1.2300]").value);
        }
    }

    @Test
    public void creatorSupertypesRetainTheirGenericArguments() {
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType(WrongCollectionBox.class));
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType(WrongInheritedCollectionBox.class));
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType(WrongNestedCollectionBox.class));
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType(WrongInheritedArrayCollectionBox.class));
        assertThrows(IllegalArgumentException.class, () -> TypeFactory.getType(WrongInheritedWildcardCollectionBox.class));

        final Type<CollectionBox> collectionType = TypeFactory.getType(CollectionBox.class);
        final CollectionBox value = CollectionBox.of(DECIMALS);
        assertEquals(DECIMALS, collectionType.valueOf(collectionType.stringOf(value)).value);

        final Type<GenericCollectionBox<BigDecimal>> genericType = TypeFactory.getType(GenericCollectionBox.class.getName() + "<BigDecimal>");
        assertEquals(DECIMALS, genericType.valueOf(genericType.stringOf(GenericCollectionBox.of(DECIMALS))).value);
    }

    public static class WrongCollectionBox {
        @JsonXmlValue
        public final List<BigDecimal> value = List.of();

        @JsonXmlCreator
        public static WrongCollectionBox of(Collection<String> value) {
            throw new AssertionError("Must reject before invocation");
        }
    }

    public static class GenericList<T> extends ArrayList<T> {
        private static final long serialVersionUID = 1L;
    }

    public static class DecimalList extends GenericList<BigDecimal> {
        private static final long serialVersionUID = 1L;
    }

    public static class GenericArrayList<T> extends ArrayList<T[]> {
        private static final long serialVersionUID = 1L;
    }

    public static class DecimalArrayList extends GenericArrayList<BigDecimal> {
        private static final long serialVersionUID = 1L;
    }

    public static class WrongInheritedArrayCollectionBox {
        @JsonXmlValue
        public final DecimalArrayList value = new DecimalArrayList();

        @JsonXmlCreator
        public static WrongInheritedArrayCollectionBox of(Collection<String[]> value) {
            throw new AssertionError("Must reject before invocation");
        }
    }

    public static class GenericWildcardList<T> extends ArrayList<List<? extends T>> {
        private static final long serialVersionUID = 1L;
    }

    public static class DecimalWildcardList extends GenericWildcardList<BigDecimal> {
        private static final long serialVersionUID = 1L;
    }

    public static class WrongInheritedWildcardCollectionBox {
        @JsonXmlValue
        public final DecimalWildcardList value = new DecimalWildcardList();

        @JsonXmlCreator
        public static WrongInheritedWildcardCollectionBox of(Collection<List<? extends String>> value) {
            throw new AssertionError("Must reject before invocation");
        }
    }

    public static class WrongInheritedCollectionBox {
        @JsonXmlValue
        public final DecimalList value = new DecimalList();

        @JsonXmlCreator
        public static WrongInheritedCollectionBox of(Collection<String> value) {
            throw new AssertionError("Must reject before invocation");
        }
    }

    public static class WrongNestedCollectionBox {
        @JsonXmlValue
        public final List<List<BigDecimal>> value = List.of();

        @JsonXmlCreator
        public static WrongNestedCollectionBox of(Collection<List<String>> value) {
            throw new AssertionError("Must reject before invocation");
        }
    }

    public static class CollectionBox {
        @JsonXmlValue
        public final List<BigDecimal> value;

        private CollectionBox(Collection<? extends BigDecimal> value) {
            this.value = new ArrayList<>(value);
        }

        @JsonXmlCreator
        public static CollectionBox of(Collection<? extends BigDecimal> value) {
            return new CollectionBox(value);
        }
    }

    public static class GenericCollectionBox<T extends BigDecimal> {
        @JsonXmlValue
        public final List<T> value;

        private GenericCollectionBox(Collection<T> value) {
            this.value = new ArrayList<>(value);
        }

        @JsonXmlCreator
        public static <U extends BigDecimal> GenericCollectionBox<U> of(Collection<U> value) {
            return new GenericCollectionBox<>(value);
        }
    }

    public static class FieldBox {
        @JsonXmlValue
        public final List<BigDecimal> value;

        private FieldBox(List<BigDecimal> value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static FieldBox of(List<BigDecimal> value) {
            return new FieldBox(value);
        }
    }

    public static class MethodBox {
        private final List<BigDecimal> value;

        private MethodBox(List<BigDecimal> value) {
            this.value = value;
        }

        @JsonXmlValue
        public List<BigDecimal> value() {
            return value;
        }

        @JsonXmlCreator
        public static MethodBox of(List<BigDecimal> value) {
            return new MethodBox(value);
        }
    }

    public static class AutoBox {
        private List<BigDecimal> value;

        public AutoBox(Object value) {
            this.value = (List<BigDecimal>) value;
        }

        public List<BigDecimal> value() {
            return value;
        }
    }

    public static class GenericBox<T> {
        @JsonXmlValue
        public final T value;

        private GenericBox(T value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static <U> GenericBox<U> of(U value) {
            return new GenericBox<>(value);
        }
    }

    public static class ListBox<T> {
        @JsonXmlValue
        public final List<T> value;

        private ListBox(List<T> value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static <U> ListBox<U> of(List<U> value) {
            return new ListBox<>(value);
        }
    }

    public static class ArrayBox<T> {
        @JsonXmlValue
        public final T[] value;

        private ArrayBox(T[] value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static <U> ArrayBox<U> of(U[] value) {
            return new ArrayBox<>(value);
        }
    }

    public static class WildcardBox {
        @JsonXmlValue
        public final List<? extends BigDecimal> value;

        private WildcardBox(List<? extends BigDecimal> value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static WildcardBox of(List<? extends BigDecimal> value) {
            return new WildcardBox(value);
        }
    }

    public static class BoundedBox<T extends BigDecimal> {
        @JsonXmlValue
        public final T value;

        private BoundedBox(T value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static <U extends BigDecimal> BoundedBox<U> of(U value) {
            return new BoundedBox<>(value);
        }
    }

    public static class WrongBox {
        @JsonXmlValue
        public final List<BigDecimal> value = List.of();

        @JsonXmlCreator
        public static WrongBox of(List<String> value) {
            throw new AssertionError("Must reject before invocation");
        }
    }

    public static class WrongBoundBox {
        @JsonXmlValue
        public final String value = "text";

        @JsonXmlCreator
        public static <U extends Number> WrongBoundBox of(U value) {
            throw new AssertionError("Must reject before invocation");
        }
    }

    public static class WrongArrayBox {
        @JsonXmlValue
        public final List<BigDecimal>[] value = new List[0];

        @JsonXmlCreator
        public static WrongArrayBox of(List<String>[] value) {
            throw new AssertionError("Must reject before invocation");
        }
    }

    public static class WrongReturnBox<T> {
        @JsonXmlValue
        public final T value = null;

        @JsonXmlCreator
        public static WrongReturnBox<String> of(Object value) {
            throw new AssertionError("Must reject before invocation");
        }
    }

    public static class NestedBoundBox<T extends List<BigDecimal>> {
        @JsonXmlValue
        public final T value;

        private NestedBoundBox(T value) {
            this.value = value;
        }

        @JsonXmlCreator
        public static <U extends List<BigDecimal>> NestedBoundBox<U> of(U value) {
            return new NestedBoundBox<>(value);
        }
    }
}
