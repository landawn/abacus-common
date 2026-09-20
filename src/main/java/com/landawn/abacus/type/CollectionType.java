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
import java.util.Queue;
import java.util.Set;

import com.landawn.abacus.annotation.MayReturnNull;
import com.landawn.abacus.exception.ParsingException;
import com.landawn.abacus.exception.UncheckedIOException;
import com.landawn.abacus.parser.JsonDeserConfig;
import com.landawn.abacus.parser.JsonSerConfig;
import com.landawn.abacus.parser.JsonXmlSerConfig;
import com.landawn.abacus.util.BufferedJsonWriter;
import com.landawn.abacus.util.CharacterWriter;
import com.landawn.abacus.util.ClassUtil;
import com.landawn.abacus.util.IOUtil;
import com.landawn.abacus.util.N;
import com.landawn.abacus.util.Objectory;
import com.landawn.abacus.util.SK;
import com.landawn.abacus.util.Strings;

/**
 * Type handler for {@link Collection} implementations, including {@link List}, {@link Set},
 * {@link Queue}, and their concrete subtypes.
 *
 * <p>Instances are created by the {@code TypeFactory} for parameterized collection type names such as
 * {@code "List<String>"} or {@code "Set<Integer>"}. The handler converts between collection objects and
 * their JSON array string representations, preserving generic element-type information for proper
 * serialization and deserialization.</p>
 *
 * <p>Example:</p>
 * <pre>{@code
 * // Obtain a typed handler
 * Type<List<String>> listType = TypeFactory.getType("List<String>");
 *
 * // Serialize
 * List<String> names = N.asList("Alice", "Bob", "Charlie");
 * String json = listType.stringOf(names);  // ["Alice", "Bob", "Charlie"]
 *
 * // Deserialize
 * List<String> parsed = listType.valueOf("[\"Alice\", \"Bob\", \"Charlie\"]");
 * }</pre>
 *
 * @param <E> the element type of the collection
 * @param <T> the concrete collection type (must extend {@link Collection}{@code <E>})
 */
@SuppressWarnings("java:S2160")
public class CollectionType<E, T extends Collection<E>> extends AbstractType<T> {

    private final String declaringName;

    private final Class<T> typeClass;

    private final List<Type<?>> parameterTypes;

    private final Type<E> elementType;

    private final boolean isList;

    private final boolean isSet;

    private final JsonDeserConfig jdc;

    /**
     * Package-private constructor for {@code CollectionType}.
     * Instances are created by the {@code TypeFactory} using a collection class and its element type name.
     * The declaring name is resolved to the most specific collection interface implemented by
     * {@code typeClass} (e.g., {@code List}, {@code Set}, or {@code Queue}).
     *
     * @param typeClass         the concrete or interface collection class to handle
     * @param parameterTypeName the name of the element type (e.g., {@code "String"} or {@code "java.lang.Integer"})
     * @throws IllegalArgumentException if {@code typeClass} is {@code null}, or a supplied type name is {@code null}, blank, or structurally invalid.
     */
    CollectionType(final Class<T> typeClass, final String parameterTypeName) throws IllegalArgumentException {
        super(getTypeName(typeClass, parameterTypeName, false));

        String declaringNameValue;

        if (typeClass.isInterface()) {
            declaringNameValue = getTypeName(typeClass, parameterTypeName, true);
        } else {
            if (List.class.isAssignableFrom(typeClass)) {
                declaringNameValue = getTypeName(List.class, parameterTypeName, true);
            } else if (Set.class.isAssignableFrom(typeClass)) {
                declaringNameValue = getTypeName(Set.class, parameterTypeName, true);
            } else if (Queue.class.isAssignableFrom(typeClass)) {
                declaringNameValue = getTypeName(Queue.class, parameterTypeName, true);
            } else {
                declaringNameValue = getTypeName(Collection.class, parameterTypeName, true);

                final Class<?>[] interfaceClasses = typeClass.getInterfaces();

                for (final Class<?> interfaceClass : interfaceClasses) {
                    if (Collection.class.isAssignableFrom(interfaceClass) && !interfaceClass.equals(Collection.class)) {
                        declaringNameValue = getTypeName(interfaceClass, parameterTypeName, true);

                        break;
                    }
                }
            }
        }

        declaringName = declaringNameValue;

        this.typeClass = typeClass;
        elementType = TypeFactory.getType(parameterTypeName);
        parameterTypes = List.of(elementType);

        jdc = JsonDeserConfig.create().setElementType(elementType);

        isList = List.class.isAssignableFrom(this.typeClass);
        isSet = Set.class.isAssignableFrom(this.typeClass);
    }

    /**
     * Returns the declaring name of this collection type.
     * The declaring name represents the type in a simplified format suitable for type declarations,
     * using simple class names rather than fully qualified names.
     *
     * @return the declaring name of this type (e.g., {@code "List<String>"} instead of {@code "java.util.List<java.lang.String>"})
     */
    @Override
    public String declaringName() {
        return declaringName;
    }

    /**
     * Returns the Class object representing the collection type handled by this type handler.
     *
     * @return the Class object for the collection type
     */
    @Override
    public Class<T> javaType() {
        return typeClass;
    }

    /**
     * Returns the type handler for the elements contained in this collection.
     *
     * @return the Type instance representing the element type of this collection
     */
    @Override
    public Type<E> elementType() {
        return elementType;
    }

    /**
     * Returns an immutable list containing the parameter types of this generic collection type.
     * For collection types, this list contains a single element representing the element type.
     *
     * @return an immutable list containing the element type as the only parameter type
     */
    @Override
    public List<Type<?>> parameterTypes() {
        return parameterTypes;
    }

    /**
     * Checks whether this collection type represents a List or its subtype.
     *
     * @return {@code true} if this type represents a List, {@code false} otherwise
     */
    @Override
    public boolean isList() {
        return isList;
    }

    /**
     * Checks whether this collection type represents a Set or its subtype.
     *
     * @return {@code true} if this type represents a Set, {@code false} otherwise
     */
    @Override
    public boolean isSet() {
        return isSet;
    }

    /**
     * Always returns {@code true} as this type handler specifically handles Collection types.
     *
     * @return {@code true} always
     */
    @Override
    public boolean isCollection() {
        return true;
    }

    /**
     * Always returns {@code true} as collection types are parameterized with an element type.
     *
     * @return {@code true} always
     */
    @Override
    public boolean isParameterizedType() {
        return true;
    }

    /**
     * Checks whether the element type of this collection is directly serializable by the type system.
     * If the element type is serializable, the collection as a whole can be written without delegating
     * to a full JSON serializer.
     *
     * @return {@code true} if the element type is serializable, {@code false} otherwise
     */
    @Override
    public boolean isSerializable() {
        return elementType.isSerializable();
    }

    /**
     * Returns the serialization type category for this collection.
     *
     * @return {@link SerializationType#SERIALIZABLE} if the element type is directly serializable,
     *         or {@link SerializationType#COLLECTION} otherwise
     */
    @Override
    public SerializationType serializationType() {
        return isSerializable() ? SerializationType.SERIALIZABLE : SerializationType.COLLECTION;
    }

    /**
     * Converts a collection to its JSON array string representation.
     * <ul>
     *   <li>{@code null} input returns {@code null}.</li>
     *   <li>An empty collection returns {@code "[]"}.</li>
     *   <li>Otherwise each element is serialized according to its element type and the results are
     *       joined in a JSON array.</li>
     * </ul>
     *
     * <p>The returned string is a serializable representation designed to be parsed back into an equivalent value
     * via {@link #valueOf(String)}. Non-null values of this type generally round-trip; {@code null}/empty handling is
     * type-specific (often yielding the type's default) and is not always identity-preserving for {@code null}. This
     * is the key distinction from {@link Object#toString()}, whose result is not guaranteed to be convertible back
     * into the original value.</p>
     *
     * @param x the collection to serialize; may be {@code null}
     * @return the JSON array string, or {@code null} if {@code x} is {@code null}
     * @throws ClassCastException if an element is not compatible with the declared element type (each element is written by the
     *         declared element type's writer, e.g. a {@code String} or a {@code Map} element inside a {@code List<Integer>};
     *         a {@code Long} or {@code Double} inside a {@code List<Integer>} is narrowed instead)
     * @throws UncheckedIOException if the declared element serializer throws an I/O exception while producing the string.
     * @throws RuntimeException if a value or bean property cannot be serialized by its selected type handler.
     * @see #valueOf(String)
     * @see #valueOf(Object)
     */
    @MayReturnNull
    @Override
    public String stringOf(final T x) throws ClassCastException, UncheckedIOException, RuntimeException {
        if (x == null) {
            return null; // NOSONAR
        } else if (x.isEmpty()) {
            return STR_FOR_EMPTY_ARRAY;
        }

        if (this.isSerializable()) {
            final BufferedJsonWriter bw = Objectory.createBufferedJsonWriter();
            Throwable failure = null;

            try {
                bw.write(SK._BRACKET_L);

                int i = 0;
                for (final E element : x) {
                    if (i++ > 0) {
                        bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                    }

                    if (element == null) {
                        bw.write(NULL_CHAR_ARRAY);
                    } else {
                        elementType.serializeTo(bw, element, Utils.jsc);
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
     * Parses a JSON array string back into a collection instance.
     * <ul>
     *   <li>{@code null}, blank, or empty string returns {@code null}.</li>
     *   <li>{@code "[]"} returns an empty collection of the appropriate type.</li>
     *   <li>Otherwise the string is deserialized by the JSON parser with the configured element type.</li>
     * </ul>
     *
     * <p>This method is intended as the inverse of {@code stringOf}: it parses the type-defined string form back into
     * a value of this type. Exact round-trip behavior is type-specific ({@code null}/empty inputs typically yield the
     * type's default). Strings produced by {@link Object#toString()} are not guaranteed to be parseable in this way.</p>
     *
     * @param str the JSON array string to parse; may be {@code null}
     * @return a new collection containing the parsed elements, or {@code null} if {@code str} is {@code null} or blank
     * @throws ParsingException if {@code str} is not a well-formed JSON array text
     * @throws IllegalArgumentException if the collection class cannot be instantiated (an interface without a known implementation, an abstract
     *         class, or a class without an accessible no-arg constructor)
     * @throws RuntimeException if a selected type handler cannot convert a parsed value, or constructing the target value fails.
     * @see #valueOf(Object)
     * @see #stringOf(Collection)
     */
    @MayReturnNull
    @Override
    public T valueOf(final String str) throws ParsingException, IllegalArgumentException, RuntimeException {
        if (Strings.isBlank(str)) {
            return null; // NOSONAR
        } else if (STR_FOR_EMPTY_ARRAY.equals(str)) {
            return (T) N.newCollection(typeClass);
        } else {
            return Utils.jsonParser.deserialize(str, jdc, typeClass);
        }
    }

    /**
     * Appends the {@code toString()}-style string representation of a collection to an {@link Appendable}.
     * When the {@link Appendable} is a {@link java.io.Writer}, a buffered wrapper is used for
     * better I/O performance. If {@code x} is {@code null}, the literal {@code null} is appended.
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
     * @param appendable the {@link Appendable} to write to
     * @param x          the collection to append; may be {@code null}
     * @throws NullPointerException if {@code appendable} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     * @throws ClassCastException if an element is not compatible with the declared element type (each element is written by the
     *         declared element type's writer; a declared {@code Object} element type dispatches on the element's runtime class
     *         instead, which by construction matches)
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
    public void appendTo(final Appendable appendable, final T x) throws NullPointerException, IOException, ClassCastException, RuntimeException {
        if (x == null) {
            appendable.append(NULL_STRING);
        } else {
            if (appendable instanceof Writer writer) {
                final boolean isBufferedWriter = IOUtil.isBufferedWriter(writer);
                final Writer bw = isBufferedWriter ? writer : Objectory.createBufferedWriter(writer); //NOSONAR
                Throwable failure = null;

                try {
                    bw.write(SK._BRACKET_L);

                    int i = 0;
                    for (final E element : x) {
                        if (i++ > 0) {
                            bw.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                        }

                        if (element == null) {
                            bw.write(NULL_CHAR_ARRAY);
                        } else {
                            AbstractTupleType.appendElement(bw, elementType, element);
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
                for (final E element : x) {
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
     * Writes the JSON array representation of a collection to a {@link CharacterWriter}.
     * Each element &mdash; including a {@code null} element &mdash; is written using the declared element type's
     * {@code serializeTo} method, so element-level quotation and escaping are applied correctly.
     * If {@code x} itself is {@code null}, the literal {@code null} is written.
     * <p>
     * When the declared element type is not serializable ({@code Object}, a bean, a nested container...) and
     * {@code config} is not {@code null}, the collection is rendered the way {@link #stringOf(Collection)} renders it,
     * so each element is emitted in its structural JSON form ({@code [1, "a", null, 2.5, {"name": "x"}]}) rather than
     * as a quoted {@code stringOf} string per element ({@code ["1", "a", ...]}). Under a {@link JsonSerConfig} the JSON
     * parser writes that form, handed the {@code writer} directly only when it is a JSON writer; on any other writer
     * (XML, CSV) the JSON text is written as escaped character content instead of raw JSON. Under any other
     * configuration &mdash; an XML configuration in practice &mdash; the {@code stringOf} text of the whole collection
     * is written as escaped character content, which is what {@code Object[]} already does and the only form
     * {@link #valueOf(String)} can read back. That embedded JSON is always written compactly: {@code prettyFormat} is
     * not propagated to it, because this handler is not told the caller's current indentation and a pretty embedded
     * collection would restart at the left margin. With no config at all the elements are written element by element
     * without quotation, as before.
     * <p>
     * This method is specifically designed for JSON/XML serialization: it writes the serialized form of {@code x} to the
     * {@code CharacterWriter}, applying string quotation and character escaping according to the supplied serialization
     * config (a {@code null} config means no surrounding quotation). It is the streaming counterpart of {@code stringOf}
     * and is invoked by the JSON/XML serializers.
     * <p>
     * <b>serializeTo vs. appendTo:</b> {@code serializeTo} produces machine-readable JSON/XML (quoted and escaped),
     * whereas {@code appendTo} produces a plain, human-readable {@code toString()}-style rendering without JSON/XML
     * quoting or escaping.
     *
     * @param writer the {@link CharacterWriter} to write to
     * @param x      the collection to write; may be {@code null}
     * @param config serialization configuration forwarded to each element's writer; may be {@code null}
     * @throws NullPointerException if {@code writer} is {@code null}.
     * @throws IOException if writing the representation to the destination fails.
     * @throws ClassCastException if an element is not compatible with the declared element type (each element is written by the
     *         declared element type's writer)
     * @throws RuntimeException if a contained value is incompatible with its declared type or its selected type handler fails while writing it.
     */
    @Override
    public void serializeTo(final CharacterWriter writer, final T x, final JsonXmlSerConfig<?> config)
            throws NullPointerException, IOException, ClassCastException, RuntimeException {
        if (x == null) {
            writer.write(NULL_CHAR_ARRAY);
        } else if (!isSerializable() && config != null) {
            // A non-serializable element type (Object, beans, ...) has no typed writer: elementType.serializeTo would
            // quote stringOf(element) and emit ["1", "a", ...] (read back as Strings), and under an XmlSerConfig it
            // would drop the quoting altogether. Write the whole structure the way stringOf() does instead, as
            // ObjectArrayType does for the same shape.
            if (config instanceof JsonSerConfig jsc) {
                // Pretty format is deliberately not propagated to the embedded write: this handler is not told the
                // caller's current indentation, so a pretty embedded collection would restart at the left margin and
                // mis-align every one of its lines. Same rule as AbstractTupleType.serializeSlot.
                final JsonSerConfig embeddedConfig = jsc.isPrettyFormat() ? jsc.copy().setPrettyFormat(false) : jsc;

                if (writer instanceof BufferedJsonWriter) {
                    Utils.jsonParser.serialize(x, embeddedConfig, writer);
                } else {
                    // A JSON config can still arrive on an XML or CSV writer (Type.serializeTo is public API): the
                    // parser would write raw JSON - unescaped ", < and & - into that format. Emit the text through
                    // writeCharacter so it is escaped for the target format, as ObjectArrayType does.
                    writer.writeCharacter(Utils.jsonParser.serialize(x, embeddedConfig));
                }
            } else {
                writer.writeCharacter(stringOf(x));
            }
        } else {
            writer.write(SK._BRACKET_L);

            int i = 0;
            for (final E element : x) {
                if (i++ > 0) {
                    writer.write(ELEMENT_SEPARATOR_CHAR_ARRAY);
                }

                elementType.serializeTo(writer, element, config);
            }

            writer.write(SK._BRACKET_R);
        }
    }

    /**
     * Generates the type name string for a collection type parameterized with the given element type.
     *
     * @param typeClass         the collection class (e.g., {@code List.class})
     * @param parameterTypeName the element type name (e.g., {@code "String"})
     * @param isDeclaringName   {@code true} to produce a simple (declaring) name using
     *                          {@link com.landawn.abacus.util.ClassUtil#getSimpleClassName(Class)}
     *                          (e.g., {@code "List<String>"}); {@code false} for the fully qualified
     *                          form (e.g., {@code "java.util.List<java.lang.String>"})
     * @return the formatted type name
     * @throws IllegalArgumentException if {@code typeClass} is {@code null}, or a supplied type name is {@code null}, blank, or structurally invalid.
     */
    protected static String getTypeName(final Class<?> typeClass, final String parameterTypeName, final boolean isDeclaringName)
            throws IllegalArgumentException {
        if (isDeclaringName) {
            return ClassUtil.getSimpleClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).declaringName() + SK.GREATER_THAN;
        } else {
            return ClassUtil.getCanonicalClassName(typeClass) + SK.LESS_THAN + TypeFactory.getType(parameterTypeName).name() + SK.GREATER_THAN;
        }
    }
}
