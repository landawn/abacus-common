/*
 * Copyright (C) 2015 HaiYang Li
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.landawn.abacus.type;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Collection;
import java.util.List;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.Array;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;
import com.landawn.abacus.util.cs;

/**
 * Type handler for object arrays, providing serialization, deserialization,
 * and collection conversion capabilities for arrays of any object type.
 * This handler supports JSON serialization and handles nested array elements
 * by delegating to their respective type handlers.
 *
 * <p>ObjectArrayType instances are created by TypeFactory and manage the conversion
 * between object arrays and their string representations (typically JSON format).
 * The handler properly handles the element type for correct serialization.</p>
 *
 * <p><b>Usage Examples:</b></p>
 * <pre>{@code
 * // Get ObjectArrayType for String[]
 * Type<String[]> stringArrayType = TypeFactory.getType(String[].class);
 *
 * // Serialize array to string
 * String[] names = {"Alice", "Bob", "Charlie"};
 * String json = stringArrayType.stringOf(names);
 * // Result: ["Alice", "Bob", "Charlie"]
 *
 * // Deserialize string to array
 * String jsonInput = "[\"David\",\"Eve\",\"Frank\"]";
 * String[] parsedNames = stringArrayType.valueOf(jsonInput);
 *
 * // Works with complex element types
 * Type<Integer[]> intArrayType = TypeFactory.getType(Integer[].class);
 * Integer[] numbers = {1, 2, 3, 4, 5};
 * String numbersJson = intArrayType.stringOf(numbers);
 * }</pre>
 *
 * @param <T> the component type of the array
 */
public class ObjectArrayType<T> extends AbstractArrayType<T[]> { //NOSONAR

    /** The array class handled by this type (e.g., {@code String[].class}). */
    protected final Class<T[]> typeClass;

    /** The type handler for the array's component type. */
    protected final Type<T> elementType;

    /** Immutable single-element list holding {@code elementType}, returned by {@link #parameterTypes()}. */
    protected final List<Type<?>> parameterTypes;

    /** JSON deserialization configuration preconfigured with {@code elementType} as the element type. */
    protected final JsonDeserConfig jdc;

    /**
     * Constructs an ObjectArrayType for the specified array class.
     * This constructor initializes the type handler by extracting the element type from the array class.
     *
     * @param arrayClass the array class to create a type handler for (e.g., String[].class)
     * @throws IllegalArgumentException if {@code arrayClass} is {@code null} or is not an array class, so it has no component type.
     */
    ObjectArrayType(final Class<T[]> arrayClass) throws IllegalArgumentException {
        super(ClassUtil.getCanonicalClassName(arrayClass));

        typeClass = arrayClass;
        elementType = TypeFactory.getType(arrayClass.getComponentType());
        this.parameterTypes = List.of(elementType);

        jdc = JsonDeserConfig.create().setElementType(elementType);
    }

    /**
     * Constructs an ObjectArrayType for the specified element type.
     * This constructor initializes the type handler by creating an array type from the element type.
     *
     * @param elementType the Type handler for the array's element type
     * @throws IllegalArgumentException if {@code elementType} is {@code null} or represents {@code void}, which cannot be an array component.
     */
    ObjectArrayType(final Type<T> elementType) throws IllegalArgumentException {
        super(N.checkArgNotNull(elementType, cs.elementType).name() + "[]");

        typeClass = (Class<T[]>) N.newArray(elementType.javaType(), 0).getClass();
        this.elementType = elementType;
        this.parameterTypes = List.of(elementType);

        jdc = JsonDeserConfig.create().setElementType(elementType);
    }

    /**
     * Returns the Java class type that this type handler manages.
     *
     * @return the array class object
     */
    @Override
    public Class<T[]> javaType() {
        return typeClass;
    }

    /**
     * Gets the type handler for the array's element type.
     *
     * @return the Type handler for array elements
     */
    @Override
    public Type<T> elementType() {
        return elementType;
    }

    /**
     * Gets the immutable list of parameter types for this generic array type.
     * For object arrays, this returns a single-element list containing the element type.
     *
     * @return an immutable list containing the element type
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Indicates whether this type represents an object array.
     *
     * @return {@code true}, as this is an object array type
     */
    @Override
    public boolean isObjectArray() {
        return true;
    }

    /**
     * Indicates whether arrays of this type can be serialized.
     * Serialization capability depends on whether the element type is serializable.
     *
     * @return {@code true} if the element type is serializable, {@code false} otherwise
     */
    @Override
    public boolean isSerializable() {
        return elementType.isSerializable();
    }

    /**
     * Converts an object array to its JSON string representation.
     * If the element type is serializable, performs custom JSON serialization.
     * Otherwise, delegates to the JSON parser.
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}; Non-null values of this type generally round-trip; {@code null}/empty handling is type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the array to convert
     * @return JSON string representation, {@code null} if input is {@code null}, or {@code "[]"} for empty arrays
     * @throws UncheckedIOException if an element type handler throws an IOException while producing the string representation.
     * @throws RuntimeException if a value or bean property cannot be serialized by its selected type handler.
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @MayReturnNull
    @Override
    public String stringOf(final T[] x) throws UncheckedIOException, RuntimeException {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        if (this.isSerializable()) {
            final BufferedJsonWriter bw = Objectory.createBufferedJsonWriter();
            Throwable failure = null;

            try {
                bw.write(SK._BRACKET_L);

                for (int i = 0, len = x.length; i < len; i++) {
                    if (i > 0) {
                        bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    if (x[i] == null) {
                        bw.write(NULL_CHAR_ARRAY);
                    } else {
                        elementType.serializeTo(bw, x[i], Utils.jsc);
                    }
                }

                bw.write(SK._BRACKET_R);

                return bw.toString();
            } catch (final IOException e) {
                final UncheckedIOException uncheckedException = new UncheckedIOException(e);
                failure = uncheckedException;
                throw uncheckedException;
            } catch (final RuntimeException | Error e) {
                failure = e;
                throw e;
            } finally {
                Utils.recycle(bw, failure);
            }
        } else {
            return Utils.jsonParser.serialize(x, Utils.jsc);
        }

    }

    /**
     * Converts a JSON string representation to an object array.
     * Returns {@code null} if the input is {@code null}, empty, or blank, and an empty array
     * for the special "[]" representation; otherwise parses the string as JSON.
     * Structural whitespace surrounding the array (any character for which
     * {@link Character#isWhitespace(char)} is {@code true}, for example {@code '\t'}, {@code '\n'} or
     * the ideographic space U+3000) is ignored, as it is for the primitive and boxed array types.
     *
     * <p>The returned array's component type is always the declared element type. A nested array or object in a slot
     * whose element type is {@code String} is stored as the raw text of that value ({@code "[[1]]"} yields
     * {@code {"[1]"}}); an element type that accepts structured values ({@code Object}, {@code Serializable}) keeps the
     * parsed {@code List}/{@code Map}; for any other element type the element type's own parse failure is propagated
     * (for example {@code NumberFormatException} for {@code Integer[]}).</p>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form produced by
     * {@code stringOf} back into a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the type's default). Strings produced by {@link Object#toString()} are not
     * guaranteed to be parseable in this way.</p>
     *
     * @param str the JSON string to parse
     * @return the parsed array, {@code null} if input is {@code null}, empty, or blank, or an empty array for the "[]" representation
     * @throws ParsingException if {@code str} is not a valid JSON array text
     * @throws RuntimeException if a selected type handler cannot convert a parsed value, or constructing the target value fails.
     * @see #valueOf(Object)
     * @see #stringOf(Object[])
     */
    @MayReturnNull
    @Override
    public T[] valueOf(final String str) throws ParsingException, RuntimeException {
        if (Strings.isBlank(str)) {
            return null; // NOSONAR
        }

        // isBlank accepted the input with Character.isWhitespace; the JSON parser only skips ASCII whitespace
        // around the root value, so strip with the same predicate (strip() returns this when nothing changes).
        final String trimmed = str.strip();

        if (STR_FOR_EMPTY_ARRAY.equals(trimmed)) {
            return Array.newInstance(elementType.javaType(), 0);
        } else {
            return Utils.jsonParser.deserialize(trimmed, jdc, this);
        }
    }

    /**
     * Appends the {@code toString()}-style string representation of an object array to an Appendable.
     * Optimizes performance by using buffered writers when appropriate.
     * <p>
     * Each element is appended by the declared element type's handler. When that declared type is {@code Object} the
     * handler of the element's runtime class is used instead, exactly as {@code AbstractTupleType.appendElement} -
     * the slot writer the Pair/Triple/Tuple, {@code Map.Entry} and optional handlers use - resolves a slot:
     * {@code ObjectType} has no {@code appendTo} of its own, so it would otherwise fall back to {@code stringOf}, i.e.
     * the JSON form. A map, collection or bean element therefore keeps the {@code toString()}-style form
     * ({@code [{k:1}]}, not {@code [{"k": 1}]}), matching what the same value appends as when it is not in a container.
     * A {@code null} element is appended as the literal {@code null}.
     * <p>
     * <b>appendTo vs. serializeTo:</b> {@code appendTo} produces a plain, {@code toString()}-style rendering with no
     * JSON/XML quoting or escaping (for general text output), whereas {@code serializeTo} produces the JSON/XML
     * serialized form (applying string quotation and character escaping per the serialization config) and is used by the
     * JSON/XML serializers.
     *
     * @param appendable the Appendable to write to
     * @param x the array to append
     * @throws NullPointerException if {@code appendable} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     * @throws RuntimeException if a contained value is incompatible with its declared type or its selected type handler fails while writing it.
     * @implNote
     * This method appends a string representation of {@code x} to {@code appendable} (the literal {@code "null"} for a
     * {@code null} value). Conceptually this is the human-readable form produced by {@code toString()}, <i>not</i> the
     * value returned by {@code stringOf}, which is a formatted, serializable representation (typically a JSON string)
     * that {@link #valueOf(String)} can convert back into an equivalent value. For values whose nested structure makes
     * the two forms differ (collections, maps, arrays), {@code appendTo} emits the unquoted, {@code toString()}-style
     * form; it is therefore not, in the general contract, a plain
     * {@code appendable.append(x == null ? NULL_STRING : stringOf(x))}. (For value types whose human-readable and
     * serialized forms coincide, the appended text is naturally identical to {@code stringOf(x)}.)
     */
    @Override
    public void appendTo(final Appendable appendable, final T[] x) throws NullPointerException, IOException, RuntimeException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
                final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer); //NOSONAR
                Throwable failure = null;

                try {
                    bw.write(SK._BRACKET_L);

                    for (int i = 0, len = x.length; i < len; i++) {
                        if (i > 0) {
                            bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                        }

                        if (x[i] == null) {
                            bw.write(NULL_CHAR_ARRAY);
                        } else {
                            AbstractTupleType.appendElement(bw, elementType, x[i]);
                        }
                    }

                    bw.write(SK._BRACKET_R);

                    if (!isBufferedWriter) {
                        bw.flush();
                    }
                } catch (final IOException | RuntimeException | Error e) {
                    failure = e;
                    throw e;
                } finally {
                    if (!isBufferedWriter) {
                        Utils.recycle((BufferedWriter) bw, failure);
                    }
                }
            } else {
                appendable.append(SK._BRACKET_L);

                int i = 0;
                for (final T element : x) {
                    if (i++ > 0) {
                        appendable.append(ELEMENT_SEPARATOR);
                    }

                    if (element == null) {
                        appendable.append(NULL_STRING);
                    } else {
                        AbstractTupleType.appendElement(appendable, elementType, element);
                    }
                }

                appendable.append(SK._BRACKET_R);
            }
        }
    }

    /**
     * Writes the JSON character representation of an object array to a CharacterWriter.
     * This method is typically used for JSON/XML serialization and handles {@code null} elements.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     * <p>
     * When the element type is serializable (for example {@code String[]}, {@code Integer[]}, enum arrays) each element
     * is written by its own type handler. When it is not (for example {@code Object[]}, {@code Bean[]},
     * {@code Map[]}, {@code Object[][]}) the array is rendered the same way {@link #stringOf(Object[])} renders it: under a
     * {@link com.landawn.abacus.parser.JsonSerConfig} the JSON parser writes real JSON (a number stays a number, a
     * bean becomes an object, so {@code new Object[] {1, "a", null, bean}} becomes
     * {@code [1, "a", null, {"name": "z", "age": 3}]}), and under an XML configuration the JSON text of the array is
     * written as escaped character content. A {@code null} config writes the elements element by element without
     * quotation, as before. That embedded JSON is always written compactly: {@code prettyFormat} is not propagated to
     * it, because this handler is not told the caller's current indentation and a pretty embedded array would restart
     * at the left margin.
     *
     * @param writer the CharacterWriter to write to
     * @param x the array to write
     * @param config the serialization configuration
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     * @throws RuntimeException if a contained value is incompatible with its declared type or its selected type handler fails while writing it.
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final T[] x, final JsonXmlSerConfig<?> config)
            throws NullPointerException, IOException, RuntimeException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else if (x.length > 0 && config != null && !isSerializable()) {
            // A non-serializable element type (Object, bean, map, nested array) would otherwise go element by element
            // through SingleValueType.serializeTo, which quotes stringOf(element): 1 -> "1", a bean -> a JSON string.
            // Build the JSON the same way stringOf does. The writer is handed to the parser only when it is already a
            // JSON writer; any other writer (XML, CSV) receives the text through writeCharacter so it is escaped for
            // that format instead of receiving raw JSON.
            if (config instanceof JsonSerConfig jsc) {
                // Pretty format is deliberately not propagated to the embedded write: this handler is not told the
                // caller's current indentation, so a pretty embedded array would restart at the left margin and
                // mis-align every one of its lines. Same rule as AbstractTupleType.serializeSlot.
                final JsonSerConfig embeddedConfig = jsc.isPrettyFormat() ? jsc.copy().setPrettyFormat(false) : jsc;

                if (writer instanceof BufferedJsonWriter) {
                    Utils.jsonParser.serialize(x, embeddedConfig, writer);
                } else {
                    writer.writeCharacter(Utils.jsonParser.serialize(x, embeddedConfig));
                }
            } else {
                writer.writeCharacter(stringOf(x));
            }
        } else {
            writer.write(SK._BRACKET_L);

            for (int i = 0, len = x.length; i < len; i++) {
                if (i > 0) {
                    writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                }

                elementType.serializeTo(writer, x[i], config);
            }

            writer.write(SK._BRACKET_R);
        }
    }

    /**
     * Converts a Collection to an array of the appropriate type.
     * Creates a new array with the same size as the collection and copies all elements.
     *
     * @param c the collection to convert
     * @return an array containing all elements from the collection, or {@code null} if the collection is null
     * @throws ArrayStoreException if any element in the collection is not assignable to the array's component type
     * @throws ArrayIndexOutOfBoundsException if the collection supplies more elements during iteration than the size used to allocate the array.
     */
    @MayReturnNull
    @Override
    public T[] collectionToArray(final Collection<?> c) throws ArrayStoreException, ArrayIndexOutOfBoundsException {
        if (c == null) {
            return null; // NOSONAR
        }

        final Object[] array = N.newArray(typeClass.getComponentType(), c.size());

        int i = 0;

        for (final Object element : c) {
            array[i++] = element;
        }

        return (T[]) array;
    }

    /**
     * Converts an array to a Collection by adding all array elements to the provided collection.
     * Does nothing if the input array is {@code null} or empty.
     *
     * @param x the array to convert
     * @param output the collection to add elements to
     * @throws NullPointerException if the input array is nonempty and {@code output} is {@code null}, or the output collection rejects a null array element.
     * @throws UnsupportedOperationException if the input array is nonempty and the output collection does not support adding elements.
     * @throws ClassCastException if the output collection cannot accept the array's elements
     * @throws IllegalArgumentException if the output collection rejects an element for a restriction other than its type or nullness.
     */
    @Override
    public void arrayToCollection(final T[] x, final Collection<?> output)
            throws NullPointerException, UnsupportedOperationException, ClassCastException, IllegalArgumentException {
        if (N.notEmpty(x)) {
            final Collection<Object> c = (Collection<Object>) output;

            c.addAll(Array.asList(x));
        }
    }

    /**
     * Computes a hash code for the given array.
     * This method delegates to {@link N#hashCode(Object[])}, which hashes elements with
     * {@link java.util.Objects#hashCode(Object)} (shallow), consistent with {@link #equals(Object[], Object[])}.
     *
     * @param x the array to hash
     * @return the computed hash code
     */
    @Override
    public int hashCode(final Object[] x) {
        return N.hashCode(x);
    }

    /**
     * Computes a deep hash code for the given array.
     * This method recursively computes hash codes for nested arrays and objects.
     *
     * @param x the array to hash
     * @return the computed deep hash code
     */
    @Override
    public int deepHashCode(final Object[] x) {
        return N.deepHashCode(x);
    }

    /**
     * Compares two arrays for equality.
     * This method delegates to {@link N#equals(Object[], Object[])}, which compares elements with
     * {@link java.util.Objects#equals(Object, Object)} (shallow).
     *
     * @param x the first array
     * @param y the second array
     * @return {@code true} if the arrays are equal, {@code false} otherwise
     */
    @Override
    public boolean equals(final Object[] x, final Object[] y) {
        return N.equals(x, y);
    }

    /**
     * Performs a deep comparison of two arrays for equality.
     * This method recursively compares nested arrays and objects.
     *
     * @param x the first array
     * @param y the second array
     * @return {@code true} if the arrays are deeply equal, {@code false} otherwise
     */
    @Override
    public boolean deepEquals(final Object[] x, final Object[] y) {
        return N.deepEquals(x, y);
    }

    /**
     * Creates a string representation of the array.
     * This method produces a shallow string representation using toString() on elements.
     *
     * @param x the array to convert to string
     * @return string representation of the array, {@code null} if input is {@code null}, or {@code "[]"} for empty arrays
     */
    @MayReturnNull
    @Override
    public String toString(final Object[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return N.toString(x);
    }

    /**
     * Creates a deep string representation of the array.
     * This method recursively converts nested arrays and objects to strings.
     *
     * @param x the array to convert to string
     * @return deep string representation of the array, {@code null} if input is {@code null}, or {@code "[]"} for empty arrays
     */
    @MayReturnNull
    @Override
    public String deepToString(final Object[] x) {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.length == 0) {
            return STR_FOR_EMPTY_ARRAY;
        }

        return N.deepToString(x);
    }
}
